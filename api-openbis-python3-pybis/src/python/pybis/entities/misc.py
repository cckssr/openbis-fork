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
"""Tags and deletions: client API."""

from __future__ import annotations

from collections.abc import Sequence
from typing import TYPE_CHECKING, Any, cast

from ..api.rpc import parse_jackson, type_for_id
from ..api.search import explicit_id_criterion
from ..definitions import get_fetchoption_for_entity
from ..exceptions import NotFoundError
from ..tag import Tag
from ..types.results import SearchResult
from ..utils import extract_deletion, format_timestamp
from ._mixin import ClientApiMixin

if TYPE_CHECKING:
    import pandas as pd


def _tags_df(items: Sequence[Any]) -> "pd.DataFrame":
    """Build the notebook DataFrame view for a page of tags."""
    from pandas import DataFrame

    attrs = ["permId", "code", "description", "owner", "private", "registrationDate"]
    if not items:
        return DataFrame(columns=attrs)
    df = DataFrame([item.data for item in items])
    for column, mapper in [
        ("permId", lambda v: (v or {}).get("permId", "") if isinstance(v, dict) else v),
        ("owner", lambda v: (v or {}).get("userId", "") if isinstance(v, dict) else v),
        ("registrationDate", format_timestamp),
    ]:
        if column in df:
            df[column] = df[column].map(mapper)
    return cast("pd.DataFrame", df[df.columns.intersection(attrs)])


def _deletions_df(items: Sequence[Any]) -> "pd.DataFrame":
    """Build the notebook DataFrame view for a page of deletions."""
    from pandas import DataFrame

    if not items:
        return DataFrame()
    return DataFrame(items)


class _MiscApi(ClientApiMixin):
    """Tag and deletion methods of the Openbis client."""

    # --- tags ---------------------------------------------------------------

    def get_tag(self, perm_id: str) -> Tag | None:
        """Get a single Tag by perm_id or code, or None if it does not exist."""
        cached = self._object_cache(entity="tag", code=perm_id)
        if cached is not None:
            return cached  # type: ignore[no-any-return]  # reason: heterogeneous legacy cache

        fetchopts = get_fetchoption_for_entity("tag")
        fetchopts["owner"] = get_fetchoption_for_entity("owner")
        request = {
            "method": "getTags",
            "params": [self.token, [type_for_id(perm_id, "tag")], fetchopts],
        }
        resp = self._post_request(self.as_v3, request)
        parse_jackson(resp)
        for ident in resp:
            tag = Tag(self, data=resp[ident])
            self._object_cache(entity="tag", code=perm_id, value=tag)
            return tag
        return None

    def get_tag_or_raise(self, perm_id: str) -> Tag:
        """Get a single Tag; raise if it does not exist.

        Raises:
            NotFoundError: No tag matches the perm_id or code.
        """
        tag = self.get_tag(perm_id)
        if tag is None:
            raise NotFoundError("tag", perm_id)
        return tag

    def search_tags(
        self,
        *,
        code: str | None = None,
        count: int = 25,
        start_with: int = 0,
    ) -> SearchResult[Tag]:
        """Search for Tags.

        Args:
            code: Filter by tag code (exact match).
            count: Maximum number of results to return (default: 25).
            start_with: Pagination offset (default: 0).
        """
        criteria: dict[str, Any] = {
            "@type": "as.dto.tag.search.TagSearchCriteria",
            "operator": "AND",
            "criteria": [],
        }
        if code is not None:
            criteria["criteria"].append(explicit_id_criterion("code", code))
        fetchopts = get_fetchoption_for_entity("tag")
        fetchopts["from"] = start_with
        fetchopts["count"] = count
        fetchopts["owner"] = get_fetchoption_for_entity("owner")
        request = {
            "method": "searchTags",
            "params": [self.token, criteria, fetchopts],
        }
        resp = self._post_request(self.as_v3, request)
        parse_jackson(resp)
        items = [Tag(self, data=data) for data in resp["objects"]]
        return SearchResult(items, int(resp.get("totalCount", len(items))), _tags_df)

    def new_tag(self, code: str, *, description: str | None = None) -> Tag:
        """Construct an unsaved Tag (owned by the current user).

        Args:
            code: Code of the new tag.
            description: Free-text description.
        """
        return Tag(self, code=code, description=description)

    # --- deletions -----------------------------------------------------------

    def search_deletions(
        self,
        *,
        count: int = 25,
        start_with: int = 0,
    ) -> SearchResult[dict[str, Any]]:
        """Search for performed deletions.

        Args:
            count: Maximum number of deleted-object records (default: 25).
            start_with: Pagination offset (default: 0).

        Returns:
            A SearchResult of deleted-object records (plain dicts with
            deletion id, entity kind, and identifying fields).
        """
        criteria = {"@type": "as.dto.deletion.search.DeletionSearchCriteria"}
        fetchopts = get_fetchoption_for_entity("deletion")
        deleted_objects = get_fetchoption_for_entity("deletedObjects")
        deleted_objects["from"] = start_with
        deleted_objects["count"] = count
        fetchopts["deletedObjects"] = deleted_objects
        request = {
            "method": "searchDeletions",
            "params": [self.token, criteria, fetchopts],
        }
        resp = self._post_request(self.as_v3, request)
        objects = resp["objects"]
        parse_jackson(objects)
        items: list[dict[str, Any]] = []
        for value in objects:
            items.extend(extract_deletion(value))
        return SearchResult(
            items, int(resp.get("totalCount", len(items))), _deletions_df
        )


__all__ = ["Tag", "_MiscApi"]
