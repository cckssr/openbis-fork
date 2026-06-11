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
"""Factories building minimal openBIS JSON-RPC response payloads for tests."""

from __future__ import annotations

from typing import Any


def make_object_response(**kwargs: Any) -> dict[str, Any]:
    """Build a minimal JSON-RPC sample/object response dict."""
    perm_id = kwargs.pop("perm_id", "20240101000000000-1")
    identifier = kwargs.pop("identifier", "/SPACE/PROJ/OBJ-1")
    defaults: dict[str, Any] = {
        "@type": "as.dto.sample.Sample",
        "permId": {"permId": perm_id, "@type": "as.dto.sample.id.SamplePermId"},
        "identifier": {
            "identifier": identifier,
            "@type": "as.dto.sample.id.SampleIdentifier",
        },
        "code": kwargs.pop("code", "OBJ-1"),
        "type": {
            "@type": "as.dto.sample.SampleType",
            "code": kwargs.pop("type", "UNKNOWN"),
            "permId": {
                "permId": "UNKNOWN",
                "@type": "as.dto.entitytype.id.EntityTypePermId",
            },
        },
        "properties": kwargs.pop("properties", {}),
        # relationship keys are always present on the wire (fetch options
        # request them); AttrHolder indexes them unconditionally
        "parents": kwargs.pop("parents", []),
        "children": kwargs.pop("children", []),
        "components": kwargs.pop("components", []),
        "tags": kwargs.pop("tags", []),
        "container": kwargs.pop("container", None),
        "space": kwargs.pop("space", None),
        "experiment": kwargs.pop("experiment", None),
        "project": kwargs.pop("project", None),
        "registrator": None,
        "modifier": None,
        "registrationDate": None,
        "modificationDate": None,
        "attachments": None,
    }
    defaults.update(kwargs)
    return defaults


def make_collection_response(**kwargs: Any) -> dict[str, Any]:
    """Build a minimal JSON-RPC experiment/collection response dict."""
    perm_id = kwargs.pop("perm_id", "20240101000000000-2")
    identifier = kwargs.pop("identifier", "/SPACE/PROJ/COLL-1")
    defaults: dict[str, Any] = {
        "@type": "as.dto.experiment.Experiment",
        "permId": {
            "permId": perm_id,
            "@type": "as.dto.experiment.id.ExperimentPermId",
        },
        "identifier": {
            "identifier": identifier,
            "@type": "as.dto.experiment.id.ExperimentIdentifier",
        },
        "code": kwargs.pop("code", "COLL-1"),
        "type": {
            "@type": "as.dto.experiment.ExperimentType",
            "code": kwargs.pop("type", "UNKNOWN"),
        },
        "properties": kwargs.pop("properties", {}),
        "tags": kwargs.pop("tags", []),
        "project": kwargs.pop("project", None),
        "registrator": None,
        "modifier": None,
        "registrationDate": None,
        "modificationDate": None,
        "attachments": None,
    }
    defaults.update(kwargs)
    return defaults


def make_dataset_response(**kwargs: Any) -> dict[str, Any]:
    """Build a minimal JSON-RPC dataset response dict."""
    perm_id = kwargs.pop("perm_id", "20240101000000000-3")
    defaults: dict[str, Any] = {
        "@type": "as.dto.dataset.DataSet",
        "permId": {"permId": perm_id, "@type": "as.dto.dataset.id.DataSetPermId"},
        "code": kwargs.pop("code", perm_id),
        "type": {
            "@type": "as.dto.dataset.DataSetType",
            "code": kwargs.pop("type", "RAW_DATA"),
        },
        "kind": kwargs.pop("kind", "PHYSICAL"),
        "properties": kwargs.pop("properties", {}),
    }
    defaults.update(kwargs)
    return defaults


def make_space_response(**kwargs: Any) -> dict[str, Any]:
    """Build a minimal JSON-RPC space response dict."""
    code = kwargs.pop("code", "MY_SPACE")
    defaults: dict[str, Any] = {
        "@type": "as.dto.space.Space",
        "permId": {"permId": code, "@type": "as.dto.space.id.SpacePermId"},
        "code": code,
        "description": kwargs.pop("description", None),
        "registrationDate": kwargs.pop("registration_date", 1704067200000),
    }
    defaults.update(kwargs)
    return defaults


def make_project_response(**kwargs: Any) -> dict[str, Any]:
    """Build a minimal JSON-RPC project response dict."""
    perm_id = kwargs.pop("perm_id", "20240101000000000-4")
    identifier = kwargs.pop("identifier", "/SPACE/PROJ")
    defaults: dict[str, Any] = {
        "@type": "as.dto.project.Project",
        "permId": {"permId": perm_id, "@type": "as.dto.project.id.ProjectPermId"},
        "identifier": {
            "identifier": identifier,
            "@type": "as.dto.project.id.ProjectIdentifier",
        },
        "code": kwargs.pop("code", "PROJ"),
        "description": kwargs.pop("description", None),
    }
    defaults.update(kwargs)
    return defaults


def make_search_response(
    objects: list[dict[str, Any]], total_count: int | None = None
) -> dict[str, Any]:
    """Wrap entity dicts into a JSON-RPC search-result envelope."""
    return {
        "@type": "as.dto.common.search.SearchResult",
        "objects": objects,
        "totalCount": total_count if total_count is not None else len(objects),
    }
