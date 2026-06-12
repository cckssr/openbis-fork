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
"""Unit tests for the transparent object cache."""

from factories import make_search_response


def test_get_property_type_is_cached(client, mock_rpc):
    mock_rpc.return_value.post.return_value = {
        "DESCRIPTION": {"code": "DESCRIPTION", "dataType": "VARCHAR"}
    }
    first = client.get_property_type("DESCRIPTION")
    second = client.get_property_type("DESCRIPTION")
    assert first is second
    assert mock_rpc.return_value.post.call_count == 1


def test_clear_cache_invalidates_property_type(client, mock_rpc):
    mock_rpc.return_value.post.return_value = {
        "DESCRIPTION": {"code": "DESCRIPTION", "dataType": "VARCHAR"}
    }
    first = client.get_property_type("DESCRIPTION")
    client.clear_cache("propertyType")
    second = client.get_property_type("DESCRIPTION")
    assert first is not second
    assert mock_rpc.return_value.post.call_count == 2


def test_use_cache_false_disables_caching(client, mock_rpc):
    client.use_cache = False
    mock_rpc.return_value.post.return_value = {
        "DESCRIPTION": {"code": "DESCRIPTION", "dataType": "VARCHAR"}
    }
    first = client.get_property_type("DESCRIPTION")
    second = client.get_property_type("DESCRIPTION")
    assert first is not second
    assert mock_rpc.return_value.post.call_count == 2


def test_clear_vocabulary_cache_drops_term_lists(client, mock_rpc):
    mock_rpc.return_value.post.return_value = make_search_response([])
    first = client.search_terms(vocabulary="MY_VOC")
    client.clear_cache("vocabulary")
    second = client.search_terms(vocabulary="MY_VOC")
    assert first is not second
    assert mock_rpc.return_value.post.call_count == 2


def test_clear_cache_without_entity_clears_everything(client, mock_rpc):
    mock_rpc.return_value.post.return_value = {
        "DESCRIPTION": {"code": "DESCRIPTION", "dataType": "VARCHAR"}
    }
    client.get_property_type("DESCRIPTION")
    client.clear_cache()
    assert client.cache == {}
