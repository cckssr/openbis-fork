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
"""Shared base for the per-entity client API mixins.

Each ``entities/<entity>.py`` module contributes a ``_<Entity>Api`` mixin to
:class:`pybis.Openbis` (``get_* / get_*_or_raise / search_* / iter_* /
new_*``).  This base declares the client members the mixins rely on, so they
type-check standalone; the real implementations live on ``Openbis``.
"""

from __future__ import annotations

from collections.abc import Iterator
from typing import TYPE_CHECKING, Any, Protocol, TypeVar

from ..api.rpc import JsonPayload
from ..types.results import SearchResult

if TYPE_CHECKING:
    from .server import ServerInformation

T = TypeVar("T")


class SearchPage(Protocol[T]):
    """A search closure over fixed filters, parameterized by pagination."""

    def __call__(self, *, count: int, start_with: int) -> SearchResult[T]:
        """Fetch one page."""
        ...


def paginate(search_page: SearchPage[T], page_size: int = 100) -> Iterator[T]:
    """Yield all entities of a paginated search, fetching page after page.

    Args:
        search_page: Callable fetching one page (e.g. a ``search_*`` method
            with the non-pagination filters already bound).
        page_size: Entities fetched per request.

    Yields:
        Every matching entity, across all pages.
    """
    start = 0
    while True:
        result = search_page(count=page_size, start_with=start)
        yield from result
        start += len(result)
        if len(result) == 0 or start >= result.total_count:
            return


class ClientApiMixin:
    """Declares the Openbis members that entity API mixins may use."""

    as_v3: str
    url: str
    hostname: str | None

    @property
    def token(self) -> str | None:
        """Current session or personal access token."""
        raise NotImplementedError

    def is_token_valid(self, token: str | None = None) -> bool:
        """Check a token against the server (implemented by Openbis)."""
        raise NotImplementedError

    def _get_username(self) -> str:
        """User id behind the current token (implemented by Openbis)."""
        raise NotImplementedError

    def get_server_information(self) -> "ServerInformation":
        """Cached server capability record (implemented by Openbis)."""
        raise NotImplementedError

    def _post_request(self, resource: str, request: JsonPayload) -> JsonPayload:
        """Send a JSON-RPC request (implemented by Openbis)."""
        raise NotImplementedError

    def _object_cache(
        self,
        entity: str | None = None,
        code: str | None = None,
        value: Any | None = None,
    ) -> Any:
        """Read/write the transparent object cache (implemented by Openbis).

        ``Any`` is justified: the cache stores heterogeneous legacy entities.
        """
        raise NotImplementedError


__all__ = ["ClientApiMixin"]
