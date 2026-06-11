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
"""Generic result container returned by all ``search_*`` methods."""

from __future__ import annotations

from collections.abc import Callable, Iterator, Sequence
from dataclasses import dataclass, field
from functools import cached_property
from typing import TYPE_CHECKING, Generic, TypeVar

if TYPE_CHECKING:
    import pandas as pd

T = TypeVar("T")


@dataclass
class SearchResult(Generic[T]):
    """Iterable search-result page with total-count pagination awareness.

    ``total_count`` reflects how many entities matched on the server, which
    may exceed ``len(result)`` when the query was paginated (``count=`` /
    ``start_with=``).  Iterate the result directly, or use :attr:`df` for a
    pandas view in notebooks.

    Example:
        >>> result = client.search_objects(type="MOLECULE", count=10)

        >>> len(result), result.total_count
        (10, 4213)

        >>> [obj.code for obj in result]
        ['MOL-1', 'MOL-2', ...]

    Attributes:
        total_count (int): Number of matches on the server, across all pages.
    """

    _items: list[T]
    total_count: int
    _df_builder: Callable[[Sequence[T]], "pd.DataFrame"] | None = field(
        default=None, repr=False, compare=False
    )

    def __iter__(self) -> Iterator[T]:
        """Iterate over the entities of this page."""
        return iter(self._items)

    def __len__(self) -> int:
        """Number of entities in this page (not the server-side total)."""
        return len(self._items)

    def __getitem__(self, index: int) -> T:
        """Return the entity at ``index`` within this page."""
        return self._items[index]

    def __bool__(self) -> bool:
        """True if this page contains at least one entity."""
        return bool(self._items)

    def __repr__(self) -> str:
        """Summarize page size and server-side total."""
        return (
            f"{type(self).__name__}({len(self._items)} items,"
            f" total_count={self.total_count})"
        )

    @cached_property
    def df(self) -> "pd.DataFrame":
        """The page as a pandas DataFrame (lazily built, then cached)."""
        import pandas as pd

        if self._df_builder is not None:
            return self._df_builder(self._items)
        return pd.DataFrame(self._items)

    def _repr_html_(self) -> str:
        """Render as an HTML table in Jupyter notebooks."""
        html = self.df.to_html(notebook=True)
        return (
            f"{html}\n<p>{len(self._items)} of {self.total_count}"
            f" total matches</p>"
        )


__all__ = ["SearchResult"]
