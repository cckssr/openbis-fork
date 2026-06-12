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
"""Unit tests for search_collections / get_collection / new_collection."""

import pytest

from factories import make_collection_response, make_search_response
from pybis.api import filters
from pybis.exceptions import NotFoundError


def make_collection_type_response(code="UNKNOWN"):
    return {
        code: {
            "@type": "as.dto.experiment.ExperimentType",
            "permId": {
                "permId": code,
                "@type": "as.dto.entitytype.id.EntityTypePermId",
                "entityKind": "EXPERIMENT",
            },
            "code": code,
            "propertyAssignments": [],
        }
    }


@pytest.fixture
def search_client(client, mock_rpc):
    def post(resource, request):
        method = request["method"]
        if method == "searchExperiments":
            return post.search_response
        if method == "getExperimentTypes":
            return make_collection_type_response(request["params"][1][0]["permId"])
        if method == "getExperiments":
            return post.get_response
        if method == "getProjects":  # new_collection validates the project
            return {
                "/SPACE/PROJ": {
                    "permId": {
                        "permId": "20240101000000000-100",
                        "@type": "as.dto.project.id.ProjectPermId",
                    },
                    "identifier": {
                        "identifier": "/SPACE/PROJ",
                        "@type": "as.dto.project.id.ProjectIdentifier",
                    },
                    "code": "PROJ",
                    "space": None,
                    "description": None,
                    "registrator": None,
                    "modifier": None,
                    "leader": None,
                    "registrationDate": None,
                    "modificationDate": None,
                    "attachments": None,
                }
            }
        raise AssertionError(f"unexpected RPC {method}")

    post.search_response = make_search_response([])
    post.get_response = {}
    mock_rpc.return_value.post.side_effect = post
    client._test_post = post
    return client


def sent_requests(mock_rpc):
    return [c[0][1] for c in mock_rpc.return_value.post.call_args_list]


def first_criterion(mock_rpc):
    for request in sent_requests(mock_rpc):
        if request["method"] == "searchExperiments":
            return request["params"][1]["criteria"][0]
    raise AssertionError("no searchExperiments request sent")


def test_search_collections_no_filters(search_client):
    result = search_client.search_collections()
    assert len(result) == 0


def test_search_collections_by_type(search_client, mock_rpc):
    search_client.search_collections(type="DEFAULT_EXPERIMENT")
    crit = first_criterion(mock_rpc)
    assert crit["@type"] == "as.dto.experiment.search.ExperimentTypeSearchCriteria"


def test_search_collections_by_space_nests_in_project(search_client, mock_rpc):
    search_client.search_collections(space="MY_SPACE")
    crit = first_criterion(mock_rpc)
    assert crit["@type"] == "as.dto.project.search.ProjectSearchCriteria"
    assert crit["criteria"][0]["@type"] == "as.dto.space.search.SpaceSearchCriteria"


def test_search_collections_by_project(search_client, mock_rpc):
    search_client.search_collections(project="/SPACE/PROJ")
    crit = first_criterion(mock_rpc)
    assert crit["@type"] == "as.dto.project.search.ProjectSearchCriteria"


def test_search_collections_by_code(search_client, mock_rpc):
    search_client.search_collections(code="coll-1")
    crit = first_criterion(mock_rpc)
    assert crit["fieldValue"]["value"] == "COLL-1"


def test_search_collections_by_perm_id(search_client, mock_rpc):
    search_client.search_collections(perm_id="20240101000000000-50")
    crit = first_criterion(mock_rpc)
    assert crit["@type"] == "as.dto.common.search.PermIdSearchCriteria"


def test_search_collections_by_tags(search_client, mock_rpc):
    search_client.search_collections(tags=["TAG1"])
    assert first_criterion(mock_rpc)["@type"] == "as.dto.tag.search.TagSearchCriteria"


def test_search_collections_by_properties(search_client, mock_rpc):
    search_client.search_collections(properties={"GOAL": filters.contains("x")})
    crit = first_criterion(mock_rpc)
    assert crit["fieldValue"]["@type"] == "as.dto.common.search.StringContainsValue"


@pytest.mark.parametrize("flag,expected", [(True, "true"), (False, "false")])
def test_search_collections_by_is_finished(search_client, mock_rpc, flag, expected):
    search_client.search_collections(is_finished=flag)
    crit = first_criterion(mock_rpc)
    assert crit["fieldName"] == "FINISHED_FLAG"
    assert crit["fieldValue"]["value"] == expected


def test_search_collections_returns_entities(search_client):
    search_client._test_post.search_response = make_search_response(
        [make_collection_response(code="E1")]
    )
    result = search_client.search_collections()
    assert result[0].code == "E1"


def test_get_collection_by_identifier(search_client, mock_rpc):
    search_client._test_post.get_response = {
        "/SPACE/PROJ/COLL-1": make_collection_response()
    }
    coll = search_client.get_collection("/SPACE/PROJ/COLL-1")
    assert coll is not None
    assert coll.code == "COLL-1"
    request = sent_requests(mock_rpc)[0]
    assert request["method"] == "getExperiments"


def test_get_collection_returns_none_when_not_found(search_client):
    assert search_client.get_collection("/SPACE/PROJ/NOPE") is None


def test_get_collection_or_raise(search_client):
    with pytest.raises(NotFoundError):
        search_client.get_collection_or_raise("/SPACE/PROJ/NOPE")


def test_new_collection_resolves_type(search_client):
    coll = search_client.new_collection("UNKNOWN", project="/SPACE/PROJ", code="E-NEW")
    assert coll.is_new
    assert coll.code == "E-NEW"


def test_legacy_get_experiments_shim(search_client, mock_rpc):
    import warnings

    with warnings.catch_warnings():
        warnings.simplefilter("ignore")
        result = search_client.get_experiments(space="MY_SPACE", props="*")
    assert len(result) == 0
    crit = first_criterion(mock_rpc)
    assert crit["@type"] == "as.dto.project.search.ProjectSearchCriteria"


def test_search_collection_types(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response([])
    result = client.search_collection_types()
    assert len(result) == 0
    request = mock_rpc.return_value.post.call_args[0][1]
    assert request["method"] == "searchExperimentTypes"
