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
"""Unit tests for search_spaces / get_space / iter_spaces / new_space."""

import pandas as pd

from factories import make_search_response, make_space_response
from pybis.types import SearchResult


def sent_request(mock_rpc):
    return mock_rpc.return_value.post.call_args[0][1]


def criteria_of(mock_rpc):
    return sent_request(mock_rpc)["params"][1]


def fetchopts_of(mock_rpc):
    return sent_request(mock_rpc)["params"][2]


# --- search_spaces ------------------------------------------------------------


def test_search_spaces_no_filters(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response([])
    result = client.search_spaces()
    assert isinstance(result, SearchResult)
    assert len(result) == 0
    assert result.total_count == 0
    assert sent_request(mock_rpc)["method"] == "searchSpaces"
    assert criteria_of(mock_rpc)["criteria"] == []


def test_search_spaces_by_code(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response(
        [make_space_response(code="MY_SPACE")]
    )
    result = client.search_spaces(code="MY_SPACE")
    crit = criteria_of(mock_rpc)["criteria"]
    assert crit == [
        {
            "@type": "as.dto.common.search.CodeSearchCriteria",
            "fieldName": "code",
            "fieldType": "ATTRIBUTE",
            "fieldValue": {
                "value": "MY_SPACE",
                "@type": "as.dto.common.search.StringEqualToValue",
            },
        }
    ]
    assert result[0].code == "MY_SPACE"


def test_search_spaces_pagination_defaults(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response([])
    client.search_spaces()
    fo = fetchopts_of(mock_rpc)
    assert fo["from"] == 0
    assert fo["count"] == 25


def test_search_spaces_pagination_explicit(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response([])
    client.search_spaces(count=7, start_with=14)
    fo = fetchopts_of(mock_rpc)
    assert fo["from"] == 14
    assert fo["count"] == 7


def test_search_spaces_total_count_exceeds_page(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response(
        [make_space_response()], total_count=1000
    )
    result = client.search_spaces(count=1)
    assert len(result) == 1
    assert result.total_count == 1000


def test_search_spaces_df(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response(
        [make_space_response(code="A"), make_space_response(code="B")]
    )
    df = client.search_spaces().df
    assert isinstance(df, pd.DataFrame)
    assert list(df["code"]) == ["A", "B"]


# --- get_space -------------------------------------------------------------------


def test_get_space_returns_none_when_not_found(client, mock_rpc):
    mock_rpc.return_value.post.return_value = {}
    assert client.get_space("NO_SUCH_SPACE") is None


def test_get_space_returns_entity(client, mock_rpc):
    mock_rpc.return_value.post.return_value = {
        "MY_SPACE": make_space_response(code="MY_SPACE")
    }
    space = client.get_space("my_space")  # case-insensitive
    assert space is not None
    assert space.code == "MY_SPACE"
    request = sent_request(mock_rpc)
    assert request["method"] == "getSpaces"
    assert request["params"][1] == [
        {"permId": "MY_SPACE", "@type": "as.dto.space.id.SpacePermId"}
    ]


def test_get_space_uses_cache_on_second_call(client, mock_rpc):
    mock_rpc.return_value.post.return_value = {
        "MY_SPACE": make_space_response(code="MY_SPACE")
    }
    first = client.get_space("MY_SPACE")
    second = client.get_space("MY_SPACE")
    assert first is second
    assert mock_rpc.return_value.post.call_count == 1


def test_get_space_or_raise_raises_not_found(client, mock_rpc):
    import pytest

    from pybis.exceptions import NotFoundError

    mock_rpc.return_value.post.return_value = {}
    with pytest.raises(NotFoundError, match="NO_SUCH_SPACE"):
        client.get_space_or_raise("NO_SUCH_SPACE")


def test_get_space_or_raise_is_value_error_for_legacy_callers(client, mock_rpc):
    import pytest

    mock_rpc.return_value.post.return_value = {}
    with pytest.raises(ValueError):
        client.get_space_or_raise("NO_SUCH_SPACE")


# --- iter_spaces ----------------------------------------------------------------


def test_iter_spaces_fetches_multiple_pages(client, mock_rpc):
    page1 = make_search_response([make_space_response(code="A")], total_count=2)
    page2 = make_search_response([make_space_response(code="B")], total_count=2)
    mock_rpc.return_value.post.side_effect = [page1, page2]
    results = list(client.iter_spaces(page_size=1))
    assert [s.code for s in results] == ["A", "B"]
    assert mock_rpc.return_value.post.call_count == 2


def test_iter_spaces_empty(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response([])
    assert list(client.iter_spaces()) == []


def test_iter_spaces_single_page(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response(
        [make_space_response(code="A")], total_count=1
    )
    assert len(list(client.iter_spaces())) == 1
    assert mock_rpc.return_value.post.call_count == 1


# --- new_space --------------------------------------------------------------------


def test_new_space_returns_unsaved_entity(client, mock_rpc):
    space = client.new_space(code="NEW_SPACE", description="d")
    assert space.code == "NEW_SPACE"
    assert space.description == "d"
    assert space.is_new
    mock_rpc.return_value.post.assert_not_called()


def test_new_space_save_posts_creation(client, mock_rpc):
    space = client.new_space(code="NEW_SPACE")
    mock_rpc.return_value.post.side_effect = [
        {"api-version": "3.6", "openbis-version": "20.10.7"},  # server info
        [{"permId": "NEW_SPACE", "@type": "as.dto.space.id.SpacePermId"}],
        {"NEW_SPACE": make_space_response(code="NEW_SPACE")},  # refetch
    ]
    space.save()
    methods = [c[0][1]["method"] for c in mock_rpc.return_value.post.call_args_list]
    assert methods == ["getServerInformation", "createSpaces", "getSpaces"]
    create_request = mock_rpc.return_value.post.call_args_list[1][0][1]
    assert create_request["params"][1][0]["code"] == "NEW_SPACE"
    assert not space.is_new
