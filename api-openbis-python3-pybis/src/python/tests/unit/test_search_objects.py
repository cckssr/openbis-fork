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
"""Unit tests for search_objects / get_object / new_object — one per filter."""

import pytest

from factories import make_object_response, make_search_response
from pybis.api import filters
from pybis.exceptions import NotFoundError


def make_object_type_response(code="UNKNOWN", property_codes=()):
    return {
        code: {
            "@type": "as.dto.sample.SampleType",
            "permId": {
                "permId": code,
                "@type": "as.dto.entitytype.id.EntityTypePermId",
                "entityKind": "SAMPLE",
            },
            "code": code,
            "propertyAssignments": [
                {
                    "propertyType": {"code": prop, "dataType": "VARCHAR"},
                    "mandatory": False,
                    "showInEditView": True,
                }
                for prop in property_codes
            ],
        }
    }


@pytest.fixture
def search_client(client, mock_rpc):
    """Client whose RPC answers searchSamples and the type lookups."""

    def post(resource, request):
        method = request["method"]
        if method == "searchSamples":
            return post.search_response
        if method == "getSampleTypes":
            return make_object_type_response(
                request["params"][1][0]["permId"],
                property_codes=post.type_property_codes,
            )
        if method == "getSamples":
            return post.get_response
        raise AssertionError(f"unexpected RPC {method}")

    post.search_response = make_search_response([])
    post.get_response = {}
    post.type_property_codes = ()
    mock_rpc.return_value.post.side_effect = post
    client._test_post = post
    return client


def sent_requests(mock_rpc):
    return [c[0][1] for c in mock_rpc.return_value.post.call_args_list]


def search_criteria(mock_rpc):
    for request in sent_requests(mock_rpc):
        if request["method"] == "searchSamples":
            return request["params"][1]
    raise AssertionError("no searchSamples request sent")


def first_criterion(mock_rpc):
    return search_criteria(mock_rpc)["criteria"][0]


# --- one test per filter parameter ---------------------------------------------


def test_search_objects_no_filters(search_client, mock_rpc):
    result = search_client.search_objects()
    assert len(result) == 0
    assert result.total_count == 0
    assert search_criteria(mock_rpc)["criteria"] == []


def test_search_objects_by_type(search_client, mock_rpc):
    search_client._test_post.search_response = make_search_response(
        [make_object_response(type="MOLECULE")]
    )
    result = search_client.search_objects(type="MOLECULE")
    crit = first_criterion(mock_rpc)
    assert crit["@type"] == "as.dto.sample.search.SampleTypeSearchCriteria"
    assert crit["criteria"][0]["fieldValue"]["value"] == "MOLECULE"
    assert result[0].type.code == "MOLECULE"


def test_search_objects_by_space(search_client, mock_rpc):
    search_client.search_objects(space="MY_SPACE")
    crit = first_criterion(mock_rpc)
    assert crit["@type"] == "as.dto.space.search.SpaceSearchCriteria"
    assert crit["criteria"][0]["fieldValue"]["value"] == "MY_SPACE"


def test_search_objects_by_project(search_client, mock_rpc):
    search_client.search_objects(project="/SPACE/PROJ")
    crit = first_criterion(mock_rpc)
    assert crit["@type"] == "as.dto.project.search.ProjectSearchCriteria"
    assert (
        crit["criteria"][0]["@type"] == "as.dto.common.search.IdentifierSearchCriteria"
    )


def test_search_objects_by_collection(search_client, mock_rpc):
    search_client.search_objects(collection="/SPACE/PROJ/COLL")
    crit = first_criterion(mock_rpc)
    assert crit["@type"] == "as.dto.experiment.search.ExperimentSearchCriteria"


def test_search_objects_by_id_auto_classified(search_client, mock_rpc):
    search_client.search_objects(id="20240101000000000-1")
    assert (
        first_criterion(mock_rpc)["@type"]
        == "as.dto.common.search.PermIdSearchCriteria"
    )


def test_search_objects_by_perm_id(search_client, mock_rpc):
    search_client.search_objects(perm_id="20240101000000000-1")
    crit = first_criterion(mock_rpc)
    assert crit["@type"] == "as.dto.common.search.PermIdSearchCriteria"


def test_search_objects_by_identifier(search_client, mock_rpc):
    search_client.search_objects(identifier="/SPACE/PROJ/X")
    assert (
        first_criterion(mock_rpc)["@type"]
        == "as.dto.common.search.IdentifierSearchCriteria"
    )


def test_search_objects_by_code(search_client, mock_rpc):
    search_client.search_objects(code="x-1")
    crit = first_criterion(mock_rpc)
    assert crit["@type"] == "as.dto.common.search.CodeSearchCriteria"
    assert crit["fieldValue"]["value"] == "X-1"


def test_search_objects_by_tags(search_client, mock_rpc):
    search_client.search_objects(tags=["TAG1"])
    crit = first_criterion(mock_rpc)
    assert crit["@type"] == "as.dto.tag.search.TagSearchCriteria"


def test_search_objects_by_properties_exact(search_client, mock_rpc):
    search_client.search_objects(properties={"FORMULA": "H2O"})
    crit = first_criterion(mock_rpc)
    assert crit["@type"] == "as.dto.common.search.StringPropertySearchCriteria"
    assert crit["fieldName"] == "FORMULA"
    assert crit["fieldValue"]["value"] == "H2O"


def test_search_objects_by_properties_typed_filter(search_client, mock_rpc):
    search_client.search_objects(properties={"ATOMS": filters.gte(3)})
    crit = first_criterion(mock_rpc)
    assert crit["@type"] == "as.dto.common.search.NumberPropertySearchCriteria"
    assert (
        crit["fieldValue"]["@type"]
        == "as.dto.common.search.NumberGreaterThanOrEqualToValue"
    )


def test_search_objects_by_hierarchy_properties(search_client, mock_rpc):
    search_client.search_objects(
        hierarchy_properties=[filters.parent_prop("BATCH", filters.eq("B1"))]
    )
    crit = first_criterion(mock_rpc)
    assert crit["@type"] == "as.dto.sample.search.SampleParentsSearchCriteria"


def test_search_objects_by_parents_filter(search_client, mock_rpc):
    search_client.search_objects(parents="/SPACE/PROJ/PARENT")
    crit = first_criterion(mock_rpc)
    assert crit["@type"] == "as.dto.sample.search.SampleParentsSearchCriteria"
    assert (
        crit["criteria"][0]["@type"] == "as.dto.common.search.IdentifierSearchCriteria"
    )


def test_search_objects_by_children_filter(search_client, mock_rpc):
    search_client.search_objects(children=["20240101000000000-9"])
    crit = first_criterion(mock_rpc)
    assert crit["@type"] == "as.dto.sample.search.SampleChildrenSearchCriteria"


def test_search_objects_with_parents_fetches_them(search_client, mock_rpc):
    search_client.search_objects(with_parents=True)
    fo = sent_requests(mock_rpc)[0]["params"][2]
    assert fo["parents"]["@type"] == "as.dto.sample.fetchoptions.SampleFetchOptions"


def test_search_objects_with_children_fetches_them(search_client, mock_rpc):
    search_client.search_objects(with_children=True)
    fo = sent_requests(mock_rpc)[0]["params"][2]
    assert fo["children"]["@type"] == "as.dto.sample.fetchoptions.SampleFetchOptions"


def test_search_objects_by_registration_date(search_client, mock_rpc):
    search_client.search_objects(registration_date="2024-01-01")
    crit = first_criterion(mock_rpc)
    assert crit["@type"] == "as.dto.common.search.RegistrationDateSearchCriteria"


def test_search_objects_by_modification_date_filter(search_client, mock_rpc):
    search_client.search_objects(modification_date=filters.date_after("2024-01-01"))
    crit = first_criterion(mock_rpc)
    assert crit["@type"] == "as.dto.common.search.ModificationDateSearchCriteria"


def test_search_objects_by_registrator(search_client, mock_rpc):
    search_client.search_objects(registrator="alice")
    crit = first_criterion(mock_rpc)
    assert crit["@type"] == "as.dto.person.search.RegistratorSearchCriteria"


def test_search_objects_by_modifier(search_client, mock_rpc):
    search_client.search_objects(modifier="bob")
    crit = first_criterion(mock_rpc)
    assert crit["@type"] == "as.dto.person.search.ModifierSearchCriteria"


def test_search_objects_pagination(search_client, mock_rpc):
    search_client.search_objects(count=5, start_with=10)
    fo = sent_requests(mock_rpc)[0]["params"][2]
    assert fo["count"] == 5
    assert fo["from"] == 10


def test_search_objects_total_count_exceeds_page(search_client, mock_rpc):
    search_client._test_post.search_response = make_search_response(
        [make_object_response()], total_count=1000
    )
    result = search_client.search_objects(count=1)
    assert len(result) == 1
    assert result.total_count == 1000


def test_search_objects_df_includes_properties(search_client, mock_rpc):
    search_client._test_post.type_property_codes = ("FORMULA",)
    search_client._test_post.search_response = make_search_response(
        [make_object_response(properties={"FORMULA": "H2O"})]
    )
    df = search_client.search_objects().df
    assert "FORMULA" in df.columns
    assert df["FORMULA"][0] == "H2O"


def test_object_types_fetched_once_per_distinct_type(search_client, mock_rpc):
    search_client._test_post.search_response = make_search_response(
        [make_object_response(code=f"OBJ-{i}") for i in range(3)]
    )
    search_client.search_objects()
    type_lookups = [
        r for r in sent_requests(mock_rpc) if r["method"] == "getSampleTypes"
    ]
    assert len(type_lookups) == 1  # all three share type UNKNOWN


# --- get_object ------------------------------------------------------------------


def test_get_object_returns_none_when_not_found(search_client):
    assert search_client.get_object("20991231000000000-999") is None


def test_get_object_by_perm_id(search_client, mock_rpc):
    search_client._test_post.get_response = {
        "20240101000000000-1": make_object_response()
    }
    obj = search_client.get_object("20240101000000000-1")
    assert obj is not None
    assert obj.permId == "20240101000000000-1"
    request = sent_requests(mock_rpc)[0]
    assert request["params"][1] == [
        {
            "permId": "20240101000000000-1",
            "@type": "as.dto.sample.id.SamplePermId",
        }
    ]


def test_get_object_by_identifier(search_client, mock_rpc):
    search_client._test_post.get_response = {
        "/SPACE/PROJ/OBJ-1": make_object_response()
    }
    obj = search_client.get_object("/space/proj/obj-1")
    assert obj is not None
    request = sent_requests(mock_rpc)[0]
    assert request["params"][1][0]["identifier"] == "/SPACE/PROJ/OBJ-1"


def test_get_object_eln_four_part_identifier(search_client, mock_rpc):
    search_client._test_post.get_response = {}
    search_client.get_object("/SPACE/PROJ/EXP/SAMP")
    request = sent_requests(mock_rpc)[0]
    assert request["params"][1][0]["identifier"] == "/SPACE/PROJ/SAMP"


def test_get_object_or_raise(search_client):
    with pytest.raises(NotFoundError, match="20991231000000000-999"):
        search_client.get_object_or_raise("20991231000000000-999")


# --- new_object -------------------------------------------------------------------


def test_new_object_resolves_type_and_is_unsaved(search_client, mock_rpc):
    obj = search_client.new_object("UNKNOWN", code="OBJ-NEW")
    assert obj.is_new
    assert obj.code == "OBJ-NEW"
    assert obj.type.code == "UNKNOWN"


def test_new_object_unknown_type_raises(search_client, mock_rpc):
    def post(resource, request):
        if request["method"] == "getSampleTypes":
            return {}
        raise AssertionError("unexpected")

    mock_rpc.return_value.post.side_effect = post
    with pytest.raises(NotFoundError, match="NO_SUCH_TYPE"):
        search_client.new_object("NO_SUCH_TYPE")


def test_new_object_sets_properties(search_client):
    obj = search_client.new_object("UNKNOWN", properties=None, code="X")
    assert obj.props.to_dict() == {}


# --- legacy shims ------------------------------------------------------------------


def test_legacy_get_samples_props_param_absorbed(search_client, recwarn):
    import warnings

    with warnings.catch_warnings():
        warnings.simplefilter("ignore")
        result = search_client.get_samples(props="*")
    assert len(result) == 0


def test_legacy_with_parents_string_becomes_filter(search_client, mock_rpc):
    import warnings

    with warnings.catch_warnings():
        warnings.simplefilter("ignore")
        search_client.get_samples(withParents="/SPACE/PROJ/P1")
    crit = first_criterion(mock_rpc)
    assert crit["@type"] == "as.dto.sample.search.SampleParentsSearchCriteria"


def test_legacy_extra_kwargs_become_property_filters(search_client, mock_rpc):
    import warnings

    with warnings.catch_warnings():
        warnings.simplefilter("ignore")
        search_client.get_samples(FORMULA="H2O")
    crit = first_criterion(mock_rpc)
    assert crit["fieldName"] == "FORMULA"
    assert crit["fieldValue"]["value"] == "H2O"


def test_legacy_magic_string_operator_translated(search_client, mock_rpc):
    import warnings

    with warnings.catch_warnings():
        warnings.simplefilter("ignore")
        search_client.get_samples(where={"ATOMS": ">= 3"})
    crit = first_criterion(mock_rpc)
    assert crit["@type"] == "as.dto.common.search.NumberPropertySearchCriteria"
    assert (
        crit["fieldValue"]["@type"]
        == "as.dto.common.search.NumberGreaterThanOrEqualToValue"
    )
    assert crit["fieldValue"]["value"] == 3


def test_legacy_entity_object_arguments_coerced(search_client, mock_rpc):
    import warnings

    class FakeEntity:
        identifier = "/SPACE/PROJ/P1"
        permId = "20240101000000000-7"

    with warnings.catch_warnings():
        warnings.simplefilter("ignore")
        search_client.get_samples(identifier=FakeEntity())
    crit = first_criterion(mock_rpc)
    assert crit["fieldValue"]["value"] == "/SPACE/PROJ/P1"
