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
"""Unit tests for wave-4 entities: persons, groups, roles, vocab, tags."""

import warnings

import pytest

from factories import make_search_response
from pybis.exceptions import NotFoundError


def request_of(mock_rpc, method):
    for call in mock_rpc.return_value.post.call_args_list:
        if call[0][1]["method"] == method:
            return call[0][1]
    raise AssertionError(f"no {method} request sent")


# --- persons --------------------------------------------------------------------


def test_search_persons_by_user_id(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response([])
    client.search_persons(user_id="alice")
    request = request_of(mock_rpc, "searchPersons")
    crit = request["params"][1]["criteria"][0]
    assert crit["@type"] == "as.dto.person.search.UserIdSearchCriteria"
    assert crit["fieldValue"]["value"] == "alice"


def test_get_person_returns_none_when_missing(client, mock_rpc):
    mock_rpc.return_value.post.return_value = {}
    assert client.get_person("ghost") is None


def test_get_person_or_raise(client, mock_rpc):
    mock_rpc.return_value.post.return_value = {}
    with pytest.raises(NotFoundError):
        client.get_person_or_raise("ghost")


def test_legacy_get_users_alias_warns_and_delegates(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response([])
    with warnings.catch_warnings(record=True) as caught:
        warnings.simplefilter("always")
        result = client.get_users()
    assert len(result) == 0
    assert any("search_persons" in str(w.message) for w in caught)


# --- groups ----------------------------------------------------------------------


def test_search_groups_by_code(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response([])
    client.search_groups(code="MY_GROUP")
    request = request_of(mock_rpc, "searchAuthorizationGroups")
    crit = request["params"][1]["criteria"][0]
    assert crit["fieldValue"]["value"] == "MY_GROUP"


def test_get_group_returns_none_when_missing(client, mock_rpc):
    mock_rpc.return_value.post.return_value = {}
    assert client.get_group("NO_GROUP") is None


def test_new_group_legacy_user_ids_param(client, mock_rpc):
    with warnings.catch_warnings():
        warnings.simplefilter("ignore")
        group = client.new_group("G1", description="d", userIds=["alice"])
    assert group.code == "G1"


# --- role assignments ---------------------------------------------------------------


def test_search_role_assignments_by_person(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response([])
    client.search_role_assignments(person="alice")
    request = request_of(mock_rpc, "searchRoleAssignments")
    crit = request["params"][1]["criteria"][0]
    assert crit["@type"] == "as.dto.person.search.PersonSearchCriteria"


def test_legacy_get_role_assignments_user_param(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response([])
    with warnings.catch_warnings():
        warnings.simplefilter("ignore")
        client.get_role_assignments(user="alice")
    request = request_of(mock_rpc, "searchRoleAssignments")
    assert request["params"][1]["criteria"]


# --- vocabularies / terms --------------------------------------------------------------


def test_search_vocabularies_by_code(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response([])
    client.search_vocabularies(code="MY_VOC")
    request = request_of(mock_rpc, "searchVocabularies")
    assert request["params"][1]["criteria"][0]["fieldValue"]["value"] == "MY_VOC"


def test_search_terms_by_vocabulary(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response([])
    client.search_terms(vocabulary="MY_VOC")
    request = request_of(mock_rpc, "searchVocabularyTerms")
    crit = request["params"][1]["criteria"][0]
    assert crit["@type"] == "as.dto.vocabulary.search.VocabularySearchCriteria"


def test_search_terms_caches_per_vocabulary(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response([])
    first = client.search_terms(vocabulary="MY_VOC")
    second = client.search_terms(vocabulary="MY_VOC")
    assert first is second
    assert mock_rpc.return_value.post.call_count == 1


def test_get_term_returns_none_when_missing(client, mock_rpc):
    mock_rpc.return_value.post.return_value = {}
    assert client.get_term("T1", "MY_VOC") is None


def test_legacy_get_terms_shim(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response([])
    with warnings.catch_warnings():
        warnings.simplefilter("ignore")
        result = client.get_terms("MY_VOC")
    assert len(result) == 0


def test_new_term_legacy_vocabulary_code(client, mock_rpc):
    with warnings.catch_warnings():
        warnings.simplefilter("ignore")
        term = client.new_term("T1", vocabularyCode="my_voc", label="L")
    assert term.code == "T1"


# --- tags / deletions ---------------------------------------------------------------


def test_search_tags_by_code(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response([])
    client.search_tags(code="TAG1")
    request = request_of(mock_rpc, "searchTags")
    assert request["params"][1]["criteria"][0]["fieldValue"]["value"] == "TAG1"


def test_get_tag_returns_none_when_missing(client, mock_rpc):
    mock_rpc.return_value.post.return_value = {}
    assert client.get_tag("NO_TAG") is None


def test_search_deletions_empty(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response([])
    result = client.search_deletions()
    assert len(result) == 0


def test_legacy_get_deletions_shim(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response([])
    with warnings.catch_warnings(record=True) as caught:
        warnings.simplefilter("always")
        client.get_deletions()
    assert any("search_deletions" in str(w.message) for w in caught)
