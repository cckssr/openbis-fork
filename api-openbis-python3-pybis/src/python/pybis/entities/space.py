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
"""The Space entity and its client API (get/search/iter/new)."""

from __future__ import annotations

from collections.abc import Iterator, Sequence
from typing import TYPE_CHECKING, Any, cast

from ..api.rpc import parse_jackson
from ..api.search import id_criterion
from ..definitions import get_fetchoption_for_entity
from ..exceptions import NotFoundError
from ..space import Space
from ..types.results import SearchResult
from ..utils import extract_userId, format_timestamp
from ._mixin import ClientApiMixin, paginate

if TYPE_CHECKING:
    import pandas as pd


def _spaces_df(spaces: Sequence[Any]) -> "pd.DataFrame":
    """Build the notebook DataFrame view for a page of spaces."""
    from pandas import DataFrame

    attrs = [
        "code",
        "description",
        "registrationDate",
        "registrator",
        "modificationDate",
        "frozen",
        "frozenForProjects",
        "frozenForSamples",
    ]
    if not spaces:
        return DataFrame(columns=attrs)
    df = DataFrame([space.data for space in spaces])
    for column, mapper in [
        ("registrationDate", format_timestamp),
        ("modificationDate", format_timestamp),
        ("registrator", extract_userId),
    ]:
        if column in df:
            df[column] = df[column].map(mapper)
    return cast("pd.DataFrame", df[df.columns.intersection(attrs)])


class _SpaceApi(ClientApiMixin):
    """Space methods of the Openbis client."""

    def get_space(self, code: str) -> Space | None:
        """Get a single Space by its code.

        Args:
            code: The space code, e.g. ``"MY_SPACE"`` (case-insensitive).

        Returns:
            The Space, or None if it does not exist.
        """
        code = str(code).upper()
        cached = self._object_cache(entity="space", code=code)
        if cached is not None:
            return cached  # type: ignore[no-any-return]  # reason: heterogeneous legacy cache

        fetchopts: dict[str, Any] = {
            "@type": "as.dto.space.fetchoptions.SpaceFetchOptions"
        }
        fetchopts["registrator"] = get_fetchoption_for_entity("registrator")
        request = {
            "method": "getSpaces",
            "params": [
                self.token,
                [{"permId": code, "@type": "as.dto.space.id.SpacePermId"}],
                fetchopts,
            ],
        }
        resp = self._post_request(self.as_v3, request)
        for perm_id in resp:
            space = Space(self, data=resp[perm_id])
            self._object_cache(entity="space", code=code, value=space)
            return space
        return None

    def get_space_or_raise(self, code: str) -> Space:
        """Get a single Space by code; raise if it does not exist.

        Args:
            code: The space code.

        Returns:
            The Space.

        Raises:
            NotFoundError: No space exists with this code.
        """
        space = self.get_space(code)
        if space is None:
            raise NotFoundError("space", code)
        return space

    def search_spaces(
        self,
        *,
        code: str | None = None,
        count: int = 25,
        start_with: int = 0,
    ) -> SearchResult[Space]:
        """Search for Spaces matching the given criteria.

        Args:
            code: Filter by space code (exact match).
            count: Maximum number of results to return (default: 25).
            start_with: Pagination offset (default: 0).

        Returns:
            A SearchResult of Spaces with the server-side total count.
        """
        criteria: dict[str, Any] = {
            "@type": "as.dto.space.search.SpaceSearchCriteria",
            "operator": "AND",
            "criteria": [],
        }
        if code is not None:
            criteria["criteria"].append(id_criterion(code))

        fetchopts = get_fetchoption_for_entity("space")
        fetchopts["from"] = start_with
        fetchopts["count"] = count
        fetchopts["registrator"] = get_fetchoption_for_entity("registrator")

        request = {
            "method": "searchSpaces",
            "params": [self.token, criteria, fetchopts],
        }
        resp = self._post_request(self.as_v3, request)
        parse_jackson(resp)
        spaces = [Space(self, data=obj) for obj in resp["objects"]]
        return SearchResult(
            spaces, int(resp.get("totalCount", len(spaces))), _spaces_df
        )

    def iter_spaces(
        self, *, code: str | None = None, page_size: int = 100
    ) -> Iterator[Space]:
        """Iterate over all matching Spaces, paginating automatically.

        Args:
            code: Filter by space code (exact match).
            page_size: Entities fetched per request.

        Yields:
            Every matching Space.
        """
        return paginate(
            lambda *, count, start_with: self.search_spaces(
                code=code, count=count, start_with=start_with
            ),
            page_size=page_size,
        )

    def new_space(self, *, code: str, description: str | None = None) -> Space:
        """Construct an unsaved Space; call ``.save()`` to persist it.

        Args:
            code: Code of the new space.
            description: Optional free-text description.

        Returns:
            The unsaved Space.
        """
        return Space(self, code=code, description=description)


__all__ = ["Space", "_SpaceApi"]
