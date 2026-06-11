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
"""Unit tests for search_datasets / get_dataset / new_dataset — one per filter."""

import pytest

from factories import make_dataset_response, make_search_response
from pybis.api import filters
from pybis.exceptions import NotFoundError, ValidationError


def make_dataset_type_response(code="RAW_DATA"):
    return {
        code: {
            "@type": "as.dto.dataset.DataSetType",
            "permId": {
                "permId": code,
                "@type": "as.dto.entitytype.id.EntityTypePermId",
                "entityKind": "DATA_SET",
            },
            "code": code,
            "propertyAssignments": [],
        }
    }


@pytest.fixture
def search_client(client, mock_rpc):
    def post(resource, request):
        method = request["method"]
        if method == "searchDataSets":
            return post.search_response
        if method == "getDataSetTypes":
            return make_dataset_type_response(request["params"][1][0]["permId"])
        if method == "getDataSets":
            return post.get_response
        if method == "getSamples":  # new_dataset validates the owning object
            from factories import make_object_response

            return {"/SPACE/PROJ/OBJ-1": make_object_response()}
        if method == "getSampleTypes":
            return {
                "UNKNOWN": {
                    "@type": "as.dto.sample.SampleType",
                    "permId": {
                        "permId": "UNKNOWN",
                        "@type": "as.dto.entitytype.id.EntityTypePermId",
                        "entityKind": "SAMPLE",
                    },
                    "code": "UNKNOWN",
                    "propertyAssignments": [],
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
        if request["method"] == "searchDataSets":
            return request["params"][1]["criteria"][0]
    raise AssertionError("no searchDataSets request sent")


def test_search_datasets_no_filters(search_client):
    result = search_client.search_datasets()
    assert len(result) == 0


def test_search_datasets_by_type(search_client, mock_rpc):
    search_client.search_datasets(type="RAW_DATA")
    crit = first_criterion(mock_rpc)
    assert crit["@type"] == "as.dto.dataset.search.DataSetTypeSearchCriteria"


def test_search_datasets_by_kind_raises(search_client):
    with pytest.raises(ValidationError, match="kind"):
        search_client.search_datasets(kind="PHYSICAL")


def test_search_datasets_by_status(search_client, mock_rpc):
    search_client.search_datasets(status="ARCHIVED")
    crit = first_criterion(mock_rpc)
    assert crit["@type"] == "as.dto.dataset.search.PhysicalDataSearchCriteria"


def test_search_datasets_by_object(search_client, mock_rpc):
    search_client.search_datasets(object="/SPACE/PROJ/OBJ-1")
    crit = first_criterion(mock_rpc)
    assert crit["@type"] == "as.dto.sample.search.SampleSearchCriteria"


def test_search_datasets_by_collection(search_client, mock_rpc):
    search_client.search_datasets(collection="/SPACE/PROJ/COLL")
    crit = first_criterion(mock_rpc)
    assert crit["@type"] == "as.dto.experiment.search.ExperimentSearchCriteria"


def test_search_datasets_by_project_nests_in_collection(search_client, mock_rpc):
    search_client.search_datasets(project="/SPACE/PROJ")
    crit = first_criterion(mock_rpc)
    assert crit["@type"] == "as.dto.experiment.search.ExperimentSearchCriteria"
    assert crit["criteria"][0]["@type"] == "as.dto.project.search.ProjectSearchCriteria"


def test_search_datasets_by_space_nests_deeply(search_client, mock_rpc):
    search_client.search_datasets(space="MY_SPACE")
    crit = first_criterion(mock_rpc)
    inner = crit["criteria"][0]["criteria"][0]
    assert inner["@type"] == "as.dto.space.search.SpaceSearchCriteria"


def test_search_datasets_by_perm_id(search_client, mock_rpc):
    search_client.search_datasets(perm_id="20240101000000000-3")
    crit = first_criterion(mock_rpc)
    assert crit["@type"] == "as.dto.common.search.PermIdSearchCriteria"


def test_search_datasets_by_tags(search_client, mock_rpc):
    search_client.search_datasets(tags=["T1"])
    assert first_criterion(mock_rpc)["@type"] == "as.dto.tag.search.TagSearchCriteria"


def test_search_datasets_by_properties(search_client, mock_rpc):
    search_client.search_datasets(properties={"NOTES": filters.contains("x")})
    crit = first_criterion(mock_rpc)
    assert crit["fieldValue"]["@type"] == "as.dto.common.search.StringContainsValue"


def test_search_datasets_by_parents_filter(search_client, mock_rpc):
    search_client.search_datasets(parents="20240101000000000-1")
    crit = first_criterion(mock_rpc)
    assert crit["@type"] == "as.dto.dataset.search.DataSetParentsSearchCriteria"


def test_search_datasets_with_children_fetches(search_client, mock_rpc):
    search_client.search_datasets(with_children=True)
    fo = sent_requests(mock_rpc)[0]["params"][2]
    assert "children" in fo


def test_search_datasets_pagination(search_client, mock_rpc):
    search_client.search_datasets(count=2, start_with=4)
    fo = sent_requests(mock_rpc)[0]["params"][2]
    assert fo["count"] == 2
    assert fo["from"] == 4


def test_search_datasets_builds_entities_with_cached_types(search_client, mock_rpc):
    search_client._test_post.search_response = make_search_response(
        [make_dataset_response(perm_id=f"2024010100000000{i}-3") for i in range(3)]
    )
    result = search_client.search_datasets()
    assert len(result) == 3
    type_lookups = [
        r for r in sent_requests(mock_rpc) if r["method"] == "getDataSetTypes"
    ]
    assert len(type_lookups) == 1


def test_search_datasets_df_includes_physical_columns(search_client):
    search_client._test_post.search_response = make_search_response(
        [
            make_dataset_response(
                physicalData={
                    "location": "/store/1",
                    "status": "AVAILABLE",
                    "presentInArchive": False,
                    "size": 123,
                    "shareId": "1",
                }
            )
        ]
    )
    df = search_client.search_datasets().df
    assert df["location"][0] == "/store/1"
    assert df["status"][0] == "AVAILABLE"


# --- get_dataset -------------------------------------------------------------------


def test_get_dataset_returns_none_when_not_found(search_client):
    assert search_client.get_dataset("20991231000000000-9") is None


def test_get_dataset_by_perm_id(search_client, mock_rpc):
    search_client._test_post.get_response = {
        "20240101000000000-3": make_dataset_response()
    }
    ds = search_client.get_dataset("20240101000000000-3")
    assert ds is not None
    request = sent_requests(mock_rpc)[0]
    assert request["method"] == "getDataSets"


def test_get_dataset_or_raise(search_client):
    with pytest.raises(NotFoundError):
        search_client.get_dataset_or_raise("20991231000000000-9")


def test_legacy_get_dataset_list_dispatch(search_client):
    import warnings

    search_client._test_post.get_response = {
        "20240101000000000-3": make_dataset_response()
    }
    with warnings.catch_warnings():
        warnings.simplefilter("ignore")
        result = search_client.get_dataset(
            ["20240101000000000-3", "20240101000000000-3"]
        )
    assert isinstance(result, list)
    assert len(result) == 2


# --- new_dataset --------------------------------------------------------------------


def test_new_dataset_resolves_type(search_client, tmp_path):
    data_file = tmp_path / "data.txt"
    data_file.write_text("x")
    ds = search_client.new_dataset(
        "RAW_DATA", object="/SPACE/PROJ/OBJ-1", files=[str(data_file)]
    )
    assert ds.is_new
    assert ds.type.code == "RAW_DATA"


def test_new_dataset_container_kind_needs_no_files(search_client):
    ds = search_client.new_dataset("RAW_DATA", kind="CONTAINER")
    assert ds.kind == "CONTAINER"


def test_legacy_new_dataset_sample_param_translated(search_client, tmp_path):
    import warnings

    with warnings.catch_warnings(record=True) as caught:
        warnings.simplefilter("always")
        data_file = tmp_path / "data.txt"
        data_file.write_text("x")
        ds = search_client.new_dataset(
            type="RAW_DATA",
            sample="/SPACE/PROJ/OBJ-1",
            files=[str(data_file)],
            props=None,
        )
    assert ds is not None
    assert any(issubclass(w.category, DeprecationWarning) for w in caught)


# --- dataset types ------------------------------------------------------------------


def test_search_dataset_types(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response([])
    client.search_dataset_types(code="RAW_DATA")
    request = mock_rpc.return_value.post.call_args[0][1]
    assert request["method"] == "searchDataSetTypes"


def test_new_dataset_type_snake_case_params(client, mock_rpc):
    dst = client.new_dataset_type(
        "NEW_TYPE",
        description="d",
        main_dataset_pattern=".*\\.csv",
        disallow_deletion=True,
    )
    assert dst.code == "NEW_TYPE"
    assert dst.is_new
