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
"""Unit tests for search_projects / get_project / iter_projects / new_project."""

import pytest

from factories import make_project_response, make_search_response
from pybis.exceptions import NotFoundError


def sent_request(mock_rpc):
    return mock_rpc.return_value.post.call_args[0][1]


def criteria_of(mock_rpc):
    return sent_request(mock_rpc)["params"][1]


# --- search_projects -------------------------------------------------------------


def test_search_projects_no_filters(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response([])
    result = client.search_projects()
    assert len(result) == 0
    assert sent_request(mock_rpc)["method"] == "searchProjects"


def test_search_projects_by_code(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response(
        [make_project_response(code="PROJ")]
    )
    result = client.search_projects(code="proj")
    crit = criteria_of(mock_rpc)["criteria"][0]
    assert crit["@type"] == "as.dto.common.search.CodeSearchCriteria"
    assert crit["fieldValue"]["value"] == "PROJ"
    assert result[0].code == "PROJ"


def test_search_projects_by_space(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response([])
    client.search_projects(space="MY_SPACE")
    crit = criteria_of(mock_rpc)["criteria"][0]
    assert crit["@type"] == "as.dto.space.search.SpaceSearchCriteria"
    assert crit["criteria"][0]["fieldValue"]["value"] == "MY_SPACE"


def test_search_projects_by_id_identifier(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response([])
    client.search_projects(id="/SPACE/PROJ")
    crit = criteria_of(mock_rpc)["criteria"][0]
    assert crit["@type"] == "as.dto.common.search.IdentifierSearchCriteria"
    assert crit["fieldValue"]["value"] == "/SPACE/PROJ"


def test_search_projects_by_id_perm_id(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response([])
    client.search_projects(id="20240101000000000-100")
    crit = criteria_of(mock_rpc)["criteria"][0]
    assert crit["@type"] == "as.dto.common.search.PermIdSearchCriteria"


def test_search_projects_combined_filters_use_and(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response([])
    client.search_projects(code="PROJ", space="MY_SPACE")
    crit = criteria_of(mock_rpc)
    assert crit["operator"] == "AND"
    assert len(crit["criteria"]) == 2


def test_search_projects_pagination(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response([])
    client.search_projects(count=3, start_with=6)
    fo = sent_request(mock_rpc)["params"][2]
    assert fo["count"] == 3
    assert fo["from"] == 6


# --- get_project -----------------------------------------------------------------


def test_get_project_by_identifier(client, mock_rpc):
    mock_rpc.return_value.post.return_value = {"/SPACE/PROJ": make_project_response()}
    project = client.get_project("/space/proj")
    assert project is not None
    assert project.code == "PROJ"
    request = sent_request(mock_rpc)
    assert request["method"] == "getProjects"
    assert request["params"][1] == [
        {"identifier": "/SPACE/PROJ", "@type": "as.dto.project.id.ProjectIdentifier"}
    ]


def test_get_project_by_perm_id(client, mock_rpc):
    mock_rpc.return_value.post.return_value = {
        "20240101000000000-100": make_project_response()
    }
    project = client.get_project("20240101000000000-100")
    assert project is not None
    assert sent_request(mock_rpc)["params"][1] == [
        {
            "permId": "20240101000000000-100",
            "@type": "as.dto.project.id.ProjectPermId",
        }
    ]


def test_get_project_by_bare_code_uses_search(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response(
        [make_project_response(code="PROJ")]
    )
    project = client.get_project("PROJ")
    assert project is not None
    assert sent_request(mock_rpc)["method"] == "searchProjects"


def test_get_project_returns_none_when_not_found(client, mock_rpc):
    mock_rpc.return_value.post.return_value = {}
    assert client.get_project("/SPACE/NOPE") is None


def test_get_project_bare_code_ambiguous_returns_none(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response(
        [make_project_response(), make_project_response()]
    )
    assert client.get_project("PROJ") is None


def test_get_project_or_raise(client, mock_rpc):
    mock_rpc.return_value.post.return_value = {}
    with pytest.raises(NotFoundError):
        client.get_project_or_raise("/SPACE/NOPE")


# --- iter / new -------------------------------------------------------------------


def test_iter_projects_paginates(client, mock_rpc):
    page1 = make_search_response([make_project_response(code="A")], total_count=2)
    page2 = make_search_response([make_project_response(code="B")], total_count=2)
    mock_rpc.return_value.post.side_effect = [page1, page2]
    assert len(list(client.iter_projects(page_size=1))) == 2


def test_new_project_returns_unsaved_entity(client, mock_rpc):
    mock_rpc.return_value.post.return_value = {
        "MY_SPACE": {
            "permId": {"permId": "MY_SPACE", "@type": "as.dto.space.id.SpacePermId"},
            "code": "MY_SPACE",
        }
    }
    project = client.new_project("MY_SPACE", "NEW_PROJ", description="d")
    assert project.code == "NEW_PROJ"
    assert project.is_new
