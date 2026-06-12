#
#   Copyright ETH 2026 Zürich, Scientific IT Services
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#        http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.
#
"""JSON-RPC transport to openBIS servers.

This is the only module in pybis where ``Any`` may appear: JSON payloads are
inherently untyped, and the Jackson ``@id`` graph helpers walk arbitrary
response structures.

Warning:
    ``parse_jackson`` and ``assign_jackson_ids`` encode subtle wire-format
    behavior (circular reference graphs, in-place mutation order).  They were
    moved here verbatim from ``pybis.utils`` and must not be "cleaned up"
    without golden tests against captured server responses.
"""

from __future__ import annotations

import json
import logging
from typing import Any, TypeAlias, cast
from urllib.parse import urljoin

import requests

from ..exceptions import AuthenticationError, ConnectionError, ServerError

JsonPayload: TypeAlias = dict[str, Any]
"""A JSON-RPC request or response fragment."""

logger = logging.getLogger("pybis")


def parse_jackson(input_json: Any) -> None:
    """Dereference Jackson «@id» references in a JSON-RPC response, in place.

    openBIS uses a library called «jackson» to automatically generate the
    JSON RPC output.  Objects that are found the first time are added an
    attribute «@id»; any further findings only carry this reference id.
    This function is used to dereference the output.
    """
    interesting = [
        "tags",
        "registrator",
        "modifier",
        "owner",
        "type",
        "parents",
        "children",
        "containers",  # 'container',
        "properties",
        "experiment",
        "sample",
        "project",
        "space",
        "propertyType",
        "entityType",
        "propertyType",
        "propertyAssignment",
        "externalDms",
        "roleAssignments",
        "user",
        "person",
        "creatorPerson",
        "users",
        "authorizationGroup",
        "vocabulary",
        "validationPlugin",
        "dataSetPermId",
        "dataStore",
        "sampleType",
    ]
    found: dict[int, Any] = {}

    def build_cache(graph: Any) -> None:
        if isinstance(graph, list):
            for item in graph:
                build_cache(item)
        elif isinstance(graph, dict) and len(graph) > 0:
            for key, value in graph.items():
                if key in interesting:
                    if isinstance(value, dict):
                        if "@id" in value:
                            found[value["@id"]] = value
                        build_cache(value)
                    elif isinstance(value, list):
                        for item in value:
                            if isinstance(item, dict):
                                if "@id" in item:
                                    found[item["@id"]] = item
                                build_cache(item)
                elif isinstance(value, dict):
                    build_cache(value)
                elif isinstance(value, list):
                    build_cache(value)

    def deref_graph(graph: Any) -> None:
        if isinstance(graph, list):
            for i, list_item in enumerate(graph):
                if isinstance(list_item, int):
                    if list_item in found:
                        graph[i] = found.get(list_item)
                else:
                    deref_graph(list_item)
        elif isinstance(graph, dict) and len(graph) > 0:
            for key, value in graph.items():
                if key in interesting:
                    if isinstance(value, dict):
                        deref_graph(value)
                    elif isinstance(value, int):
                        graph[key] = found.get(value)
                    elif isinstance(value, list):
                        for i, list_item in enumerate(value):
                            if isinstance(list_item, int):
                                if list_item in found:
                                    value[i] = found[list_item]
                                else:
                                    value[i] = list_item
                elif isinstance(value, dict):
                    deref_graph(value)
                elif isinstance(value, list):
                    deref_graph(value)

    build_cache(input_json)
    deref_graph(found)
    deref_graph(input_json)


def assign_jackson_ids(input_json: Any) -> Any:
    """Ensure all objects with an @type have unique @id values and reuse ids via references."""
    counter = 1
    seen: dict[int, int] = {}

    def visit(graph: Any) -> Any:
        nonlocal counter
        if isinstance(graph, dict):
            if "@type" in graph:
                obj_key = id(graph)
                existing = seen.get(obj_key)
                if existing is not None:
                    return existing
                obj_id = counter
                counter += 1
                seen[obj_key] = obj_id
                graph["@id"] = obj_id
            for key, value in list(graph.items()):
                graph[key] = visit(value)
            return graph
        if isinstance(graph, list):
            return [visit(item) for item in graph]
        return graph

    return visit(input_json)


def type_for_id(ident: str, entity: str) -> JsonPayload:
    """Build the ``@type``-tagged id payload for an identifier or permId.

    Examples of produced payloads::

        {
            "identifier": "/DEFAULT/SAMPLE_NAME",
            "@type": "as.dto.sample.id.SampleIdentifier",
        }
        {"permId": "20160817175233002-331", "@type": "as.dto.sample.id.SamplePermId"}

    Args:
        ident: A path-style identifier or a permId.
        entity: Lowercase entity name, e.g. ``"sample"`` or ``"dataset"``.

    Returns:
        The id payload to embed into a JSON-RPC request.
    """
    # Tags have strange permIds...
    ident = ident.strip()
    if entity.lower() == "tag":
        if "/" in ident:
            if not ident.startswith("/"):
                ident = "/" + ident
            return {"permId": ident, "@type": "as.dto.tag.id.TagPermId"}
        else:
            return {"code": ident, "@type": "as.dto.tag.id.TagCode"}
    if entity == "personalAccessToken":
        return {"permId": ident, "@type": "as.dto.pat.id.PersonalAccessTokenPermId"}

    entities = {
        "sample": "Sample",
        "dataset": "DataSet",
        "experiment": "Experiment",
        "plugin": "Plugin",
        "space": "Space",
        "project": "Project",
        "semanticannotation": "SemanticAnnotation",
    }
    if entity.lower() in entities:
        entity_capitalize = entities[entity.lower()]
    else:
        entity_capitalize = entity.capitalize()

    if "/" in ident:
        # people tend to omit the / prefix of an identifier...
        if not ident.startswith("/"):
            ident = "/" + ident
        # ELN-LIMS style contains also experiment in sample identifer,
        # i.e. /space/project/experiment/sample_code — drop the experiment code
        if ident.count("/") == 4:
            codes = ident.split("/")
            ident = "/".join([codes[0], codes[1], codes[2], codes[4]])

        return {
            "identifier": ident.upper(),
            "@type": f"as.dto.{entity.lower()}.id.{entity_capitalize}Identifier",
        }
    return {
        "permId": ident,
        "@type": f"as.dto.{entity.lower()}.id.{entity_capitalize}PermId",
    }


class RpcClient:
    """HTTP transport for the openBIS JSON-RPC v3 API.

    One instance per :class:`pybis.Openbis` client; owns the ``requests``
    session and maps transport/server failures to the typed exceptions of
    :mod:`pybis.exceptions`.

    Attributes:
        url (str): Base URL of the openBIS server.
        verify_certificates (bool): Whether TLS certificates are validated.
    """

    def __init__(self, url: str, *, verify_certificates: bool = True) -> None:
        """Create a transport bound to a server URL.

        Args:
            url: Base URL, e.g. ``"https://openbis.example.com:8443"``.
            verify_certificates: Set False for self-signed certificates.
        """
        self.url = url
        self.verify_certificates = verify_certificates
        self._session = requests.Session()
        self._session.verify = verify_certificates

    def post(self, resource: str, request: JsonPayload) -> JsonPayload:
        """Send a JSON-RPC request to a resource path on the server.

        Args:
            resource: Resource path, e.g.
                ``"/openbis/openbis/rmi-application-server-v3.json"``.
            request: The JSON-RPC request dict (``method`` and ``params``).

        Returns:
            The deserialized ``result`` part of the response.
        """
        return self.post_full_url(urljoin(self.url, resource), request)

    def post_full_url(self, full_url: str, request: JsonPayload) -> JsonPayload:
        """Send a JSON-RPC request to a fully qualified URL.

        Args:
            full_url: Full URL including the resource path (used for DSS
                endpoints living on a different host).
            request: The JSON-RPC request dict.

        Returns:
            The deserialized ``result`` part of the response.

        Raises:
            AuthenticationError: The session token in the request is missing.
            ConnectionError: The server is unreachable or TLS validation
                failed.
            ServerError: openBIS reported an error or returned an unexpected
                response.
        """
        if "id" not in request:
            request["id"] = "2"
        if "jsonrpc" not in request:
            request["jsonrpc"] = "2.0"
        if request["params"][0] is None:
            raise AuthenticationError("Your session expired, please log in again")

        for param in request["params"]:
            assign_jackson_ids(param)

        if logger.isEnabledFor(logging.DEBUG):
            logger.debug("request: %s", json.dumps(request))
        try:
            resp = self._session.post(full_url, json.dumps(request))
        except requests.exceptions.SSLError as exc:
            raise ConnectionError(
                "Certificate validation failed. Use o=Openbis(url,"
                " verify_certificates=False) if you are using self-signed"
                " certificates."
            ) from exc
        except requests.ConnectionError as exc:
            raise ConnectionError(
                "Could not connect to the openBIS server. Please check your"
                " internet connection, the specified hostname and port."
            ) from exc
        if resp.ok:
            data = resp.json()
            if "error" in data:
                logger.debug("failed request: %s", json.dumps(request))
                raise ServerError(data["error"]["message"])
            elif "result" in data:
                return cast(JsonPayload, data["result"])
            else:
                raise ServerError("request did not return either result nor error")
        else:
            raise ServerError(
                f"general error while performing post request."
                f" {resp.status_code}:{resp.reason}",
                code=resp.status_code,
            )


__all__ = [
    "JsonPayload",
    "RpcClient",
    "assign_jackson_ids",
    "parse_jackson",
    "type_for_id",
]
