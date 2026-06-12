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
"""Plugins (dynamic property, managed property, entity validation scripts)."""

from __future__ import annotations

from collections.abc import Sequence
from typing import TYPE_CHECKING, Any, cast

from ..api.rpc import parse_jackson, type_for_id
from ..definitions import get_fetchoption_for_entity, get_type_for_entity
from ..exceptions import NotFoundError
from ..openbis_object import OpenBisObject
from ..types.results import SearchResult
from ..utils import extract_permid, extract_person, format_timestamp
from ._mixin import ClientApiMixin

if TYPE_CHECKING:
    import pandas as pd


class Plugin(OpenBisObject, entity="plugin", single_item_method_name="get_plugin"):
    """A server-side script: dynamic property, managed property, or validation."""


def _plugins_df(items: Sequence[Any]) -> "pd.DataFrame":
    """Build the notebook DataFrame view for a page of plugins."""
    from pandas import DataFrame

    attrs = [
        "name",
        "description",
        "pluginType",
        "pluginKind",
        "entityKinds",
        "registrator",
        "registrationDate",
        "permId",
    ]
    if not items:
        return DataFrame(columns=attrs)
    df = DataFrame([item.data for item in items])
    for column, mapper in [
        ("permId", extract_permid),
        ("registrator", extract_person),
        ("registrationDate", format_timestamp),
        ("description", lambda x: "" if x is None else x),
        ("entityKinds", lambda x: "" if x is None else x),
    ]:
        if column in df:
            df[column] = df[column].map(mapper)
    return cast("pd.DataFrame", df[df.columns.intersection(attrs)])


class _PluginApi(ClientApiMixin):
    """Plugin methods of the Openbis client."""

    def get_plugin(self, name: str, *, with_script: bool = True) -> Plugin | None:
        """Get a single plugin by name, or None if it does not exist.

        Args:
            name: The plugin name (its permId).
            with_script: Also fetch the script source (default: True).
        """
        fetchopts = get_fetchoption_for_entity("plugin")
        options = ["registrator"]
        if with_script:
            options.append("script")
        for option in options:
            fetchopts[option] = get_fetchoption_for_entity(option)
        request = {
            "method": "getPlugins",
            "params": [self.token, [type_for_id(name, "plugin")], fetchopts],
        }
        resp = self._post_request(self.as_v3, request)
        parse_jackson(resp)
        for ident in resp:
            return Plugin(self, data=resp[ident])
        return None

    def get_plugin_or_raise(self, name: str, *, with_script: bool = True) -> Plugin:
        """Get a single plugin by name; raise if it does not exist.

        Raises:
            NotFoundError: No plugin exists with this name.
        """
        plugin = self.get_plugin(name, with_script=with_script)
        if plugin is None:
            raise NotFoundError("plugin", name)
        return plugin

    def search_plugins(
        self,
        *,
        count: int = 25,
        start_with: int = 0,
    ) -> SearchResult[Plugin]:
        """Search for plugins.

        Args:
            count: Maximum number of results to return (default: 25).
            start_with: Pagination offset (default: 0).
        """
        criteria = get_type_for_entity("plugin", "search")
        criteria["operator"] = "AND"
        criteria["criteria"] = []
        fetchopts = get_fetchoption_for_entity("plugin")
        fetchopts["from"] = start_with
        fetchopts["count"] = count
        fetchopts["registrator"] = get_fetchoption_for_entity("registrator")
        request = {
            "method": "searchPlugins",
            "params": [self.token, criteria, fetchopts],
        }
        resp = self._post_request(self.as_v3, request)
        parse_jackson(resp)
        items = [Plugin(self, data=data) for data in resp["objects"]]
        return SearchResult(items, int(resp.get("totalCount", len(items))), _plugins_df)

    def new_plugin(
        self,
        name: str,
        plugin_type: str,
        *,
        description: str | None = None,
        entity_kind: str | None = None,
        script: str | None = None,
        available: bool = True,
    ) -> Plugin:
        """Construct an unsaved Plugin; call ``.save()`` to persist it.

        Args:
            name: Name of the plugin.
            plugin_type: One of ``DYNAMIC_PROPERTY``, ``MANAGED_PROPERTY``,
                ``ENTITY_VALIDATION``.
            description: Free-text description.
            entity_kind: Restrict to one of ``MATERIAL``, ``EXPERIMENT``,
                ``SAMPLE``, ``DATA_SET`` (default: all).
            script: Source of the script itself.
            available: Whether the plugin is available (default: True).
        """
        return Plugin(
            self,
            name=name,
            pluginType=plugin_type,
            description=description,
            entityKind=entity_kind,
            script=script,
            available=available,
        )


__all__ = ["Plugin", "_PluginApi"]
