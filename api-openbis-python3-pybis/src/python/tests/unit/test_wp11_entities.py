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
"""Unit tests for wave-5 entities: plugins, property/material types,
semantic annotations, external DMS, personal access tokens."""

import warnings

import pytest

from factories import make_search_response
from pybis.exceptions import NotFoundError


def request_of(mock_rpc, method):
    for call in mock_rpc.return_value.post.call_args_list:
        if call[0][1]["method"] == method:
            return call[0][1]
    raise AssertionError(f"no {method} request sent")


# --- plugins --------------------------------------------------------------------


def test_search_plugins_paginates(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response([])
    client.search_plugins(count=7, start_with=3)
    request = request_of(mock_rpc, "searchPlugins")
    fetchopts = request["params"][2]
    assert fetchopts["count"] == 7
    assert fetchopts["from"] == 3


def test_get_plugin_returns_none_when_missing(client, mock_rpc):
    mock_rpc.return_value.post.return_value = {}
    assert client.get_plugin("no_such_script") is None


def test_get_plugin_or_raise(client, mock_rpc):
    mock_rpc.return_value.post.return_value = {}
    with pytest.raises(NotFoundError):
        client.get_plugin_or_raise("no_such_script")


def test_get_plugin_without_script_skips_fetchopt(client, mock_rpc):
    mock_rpc.return_value.post.return_value = {}
    client.get_plugin("p", with_script=False)
    request = request_of(mock_rpc, "getPlugins")
    assert "script" not in request["params"][2]


def test_legacy_get_plugins_alias_warns_and_delegates(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response([])
    with warnings.catch_warnings(record=True) as caught:
        warnings.simplefilter("always")
        result = client.get_plugins()
    assert len(result) == 0
    assert any("search_plugins" in str(w.message) for w in caught)


def test_new_plugin_legacy_camel_case_params(client, mock_rpc):
    with warnings.catch_warnings():
        warnings.simplefilter("ignore")
        plugin = client.new_plugin("my_plugin", pluginType="ENTITY_VALIDATION")
    assert plugin.name == "my_plugin"
    assert plugin.pluginType == "ENTITY_VALIDATION"


# --- property types ---------------------------------------------------------------


def test_search_property_types_by_code(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response([])
    client.search_property_types(code="DESCRIPTION")
    request = request_of(mock_rpc, "searchPropertyTypes")
    crit = request["params"][1]["criteria"][0]
    assert crit["fieldValue"]["value"] == "DESCRIPTION"


def test_get_property_type_returns_none_when_missing(client, mock_rpc):
    mock_rpc.return_value.post.return_value = {}
    assert client.get_property_type("NO_SUCH_PROP") is None


def test_get_property_type_uppercases_and_caches(client, mock_rpc):
    mock_rpc.return_value.post.return_value = {
        "DESCRIPTION": {"code": "DESCRIPTION", "dataType": "VARCHAR"}
    }
    first = client.get_property_type("description")
    second = client.get_property_type("DESCRIPTION")
    assert first is second
    assert mock_rpc.return_value.post.call_count == 1
    request = request_of(mock_rpc, "getPropertyTypes")
    assert request["params"][1][0]["permId"] == "DESCRIPTION"


def test_legacy_get_property_types_alias(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response([])
    with warnings.catch_warnings(record=True) as caught:
        warnings.simplefilter("always")
        client.get_property_types()
    assert any("search_property_types" in str(w.message) for w in caught)


def test_new_property_type_legacy_params_translated(client, mock_rpc):
    with warnings.catch_warnings():
        warnings.simplefilter("ignore")
        pt = client.new_property_type(
            code="MY_PROP",
            label="My prop",
            description="d",
            dataType="VARCHAR",
            managedInternally=False,
        )
    assert pt.code == "MY_PROP"
    assert pt.dataType == "VARCHAR"


def test_new_property_type_rejects_only_data(client, mock_rpc):
    with pytest.raises(TypeError, match="only_data"):
        with warnings.catch_warnings():
            warnings.simplefilter("ignore")
            client.get_property_type("X", only_data=True)


# --- material types (deprecated) ----------------------------------------------------


def test_search_material_types_warns_deprecated(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response([])
    with warnings.catch_warnings(record=True) as caught:
        warnings.simplefilter("always")
        client.search_material_types()
    assert any("Material is deprecated" in str(w.message) for w in caught)
    request_of(mock_rpc, "searchMaterialTypes")


def test_legacy_get_material_types_type_param(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response([])
    with warnings.catch_warnings():
        warnings.simplefilter("ignore")
        client.get_material_types(type="MY_TYPE")
    request = request_of(mock_rpc, "searchMaterialTypes")
    assert request["params"][1]["criteria"][0]["fieldValue"]["value"] == "MY_TYPE"


def test_get_material_type_returns_none_when_missing(client, mock_rpc):
    mock_rpc.return_value.post.return_value = {}
    with warnings.catch_warnings():
        warnings.simplefilter("ignore")
        assert client.get_material_type("NO_TYPE") is None


# --- semantic annotations -------------------------------------------------------------


def test_search_semantic_annotations_by_entity_type(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response([])
    client.search_semantic_annotations(entity_type="EXPERIMENTAL_STEP")
    request = request_of(mock_rpc, "searchSemanticAnnotations")
    crit = request["params"][1]["criteria"][0]
    assert crit["@type"] == "as.dto.entitytype.search.EntityTypeSearchCriteria"


def test_search_semantic_annotations_assignment_criteria(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response([])
    client.search_semantic_annotations(entity_type="ET", property_type="PT")
    request = request_of(mock_rpc, "searchSemanticAnnotations")
    crit = request["params"][1]["criteria"][0]
    assert crit["@type"] == "as.dto.property.search.PropertyAssignmentSearchCriteria"
    assert len(crit["criteria"]) == 2


def test_search_semantic_annotations_flattens_items(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response(
        [
            {
                "permId": {"permId": "20240101-1"},
                "entityType": {"code": "ET"},
                "creationDate": 1700000000000,
                "predicateOntologyId": "ro",
            }
        ]
    )
    result = client.search_semantic_annotations()
    annotation = result[0]
    assert annotation.permId == "20240101-1"
    assert annotation.entityType == "ET"
    assert annotation.predicateOntologyId == "ro"


def test_legacy_search_semantic_annotations_camel_params(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response([])
    with warnings.catch_warnings(record=True) as caught:
        warnings.simplefilter("always")
        client.search_semantic_annotations(entityType="ET")
    assert any("parameters changed" in str(w.message) for w in caught)
    request = request_of(mock_rpc, "searchSemanticAnnotations")
    assert request["params"][1]["criteria"]


def test_legacy_get_semantic_annotations_alias(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response([])
    with warnings.catch_warnings(record=True) as caught:
        warnings.simplefilter("always")
        client.get_semantic_annotations()
    assert any("search_semantic_annotations" in str(w.message) for w in caught)


def test_get_semantic_annotation_returns_none_when_missing(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response([])
    assert client.get_semantic_annotation("missing-id") is None


def test_new_semantic_annotation_snake_case(client, mock_rpc):
    annotation = client.new_semantic_annotation(
        entity_type="ET",
        predicate_ontology_id="ro",
        predicate_accession_id="RO:1",
    )
    assert annotation.entityType == "ET"
    assert annotation.predicateOntologyId == "ro"


def test_new_semantic_annotation_legacy_camel_case(client, mock_rpc):
    with warnings.catch_warnings():
        warnings.simplefilter("ignore")
        annotation = client.new_semantic_annotation(
            entityType="ET", predicateOntologyId="ro"
        )
    assert annotation.entityType == "ET"
    assert annotation.predicateOntologyId == "ro"


# --- external DMS ---------------------------------------------------------------------


def test_search_external_dms(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response([])
    result = client.search_external_data_management_systems()
    assert len(result) == 0
    request_of(mock_rpc, "searchExternalDataManagementSystems")


def test_get_external_dms_returns_none_when_missing(client, mock_rpc):
    mock_rpc.return_value.post.return_value = {}
    assert client.get_external_data_management_system("NO_DMS") is None


def test_legacy_get_externalDms_alias(client, mock_rpc):
    mock_rpc.return_value.post.return_value = {
        "DMS1": {"code": "DMS1", "permId": {"permId": "DMS1"}}
    }
    with warnings.catch_warnings(record=True) as caught:
        warnings.simplefilter("always")
        dms = client.get_externalDms("DMS1")
    assert dms.code == "DMS1"
    assert any("get_external_data_management_system" in str(w.message) for w in caught)


def test_create_external_dms_legacy_positional(client, mock_rpc):
    mock_rpc.return_value.post.side_effect = [
        [{"permId": "DMS1"}],
        {"DMS1": {"code": "DMS1"}},
    ]
    with warnings.catch_warnings():
        warnings.simplefilter("ignore")
        dms = client.create_external_data_management_system(
            "DMS1", "label", "/addr", "FILE_SYSTEM"
        )
    assert dms.code == "DMS1"
    request = request_of(mock_rpc, "createExternalDataManagementSystems")
    assert request["params"][1][0]["addressType"] == "FILE_SYSTEM"


# --- personal access tokens --------------------------------------------------------------


def test_search_pats_by_session_name(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response([])
    client.search_personal_access_tokens(session_name="jupyter")
    request = request_of(mock_rpc, "searchPersonalAccessTokens")
    crit = request["params"][1]["criteria"][0]
    assert crit["fieldValue"]["value"] == "jupyter"


def test_get_pat_returns_none_when_missing(client, mock_rpc):
    mock_rpc.return_value.post.return_value = {}
    assert client.get_personal_access_token("no-pat") is None


def test_legacy_get_personal_access_tokens_alias(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response([])
    with warnings.catch_warnings(record=True) as caught:
        warnings.simplefilter("always")
        client.get_personal_access_tokens(sessionName="jupyter")
    assert any("search_personal_access_tokens" in str(w.message) for w in caught)
    request = request_of(mock_rpc, "searchPersonalAccessTokens")
    assert request["params"][1]["criteria"][0]["fieldValue"]["value"] == "jupyter"
