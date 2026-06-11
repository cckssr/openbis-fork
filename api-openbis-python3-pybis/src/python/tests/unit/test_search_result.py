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
"""Unit tests for pybis.types.results.SearchResult."""

import pandas as pd
import pytest

from pybis.types import SearchResult


def test_iter():
    assert list(SearchResult([1, 2, 3], 100)) == [1, 2, 3]


def test_len():
    assert len(SearchResult([1, 2, 3], 100)) == 3


def test_getitem():
    assert SearchResult([1, 2, 3], 100)[1] == 2


def test_getitem_out_of_range_raises():
    with pytest.raises(IndexError):
        SearchResult([1], 1)[5]


def test_bool_nonempty():
    assert SearchResult([1], 1)


def test_bool_empty():
    assert not SearchResult([], 0)


def test_total_count():
    assert SearchResult([1], 1000).total_count == 1000


def test_total_count_can_exceed_page_length():
    result = SearchResult(["only-item"], 1000)
    assert len(result) == 1
    assert result.total_count == 1000


def test_repr_shows_counts():
    r = repr(SearchResult([1, 2], 50))
    assert "2 items" in r
    assert "total_count=50" in r


def test_df_default_builder():
    df = SearchResult([{"code": "A"}, {"code": "B"}], 2).df
    assert isinstance(df, pd.DataFrame)
    assert list(df["code"]) == ["A", "B"]


def test_df_custom_builder_and_caching():
    calls = []

    def builder(items):
        calls.append(items)
        return pd.DataFrame({"n": list(items)})

    result = SearchResult([1, 2, 3], 3, builder)
    assert list(result.df["n"]) == [1, 2, 3]
    assert result.df is result.df  # cached
    assert len(calls) == 1


def test_repr_html_mentions_total():
    html = SearchResult([{"code": "A"}], 42)._repr_html_()
    assert "42" in html
    assert "<table" in html


def test_equality_is_value_based():
    assert SearchResult([1, 2], 10) == SearchResult([1, 2], 10)
    assert SearchResult([1, 2], 10) != SearchResult([1, 2], 11)
