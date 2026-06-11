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
"""Unit tests for the JSON-RPC transport: Jackson graphs and error mapping."""

import copy
import json
from pathlib import Path
from unittest.mock import MagicMock, patch

import pytest
import requests

from pybis.api.rpc import (
    RpcClient,
    assign_jackson_ids,
    parse_jackson,
    type_for_id,
)
from pybis.exceptions import AuthenticationError, ConnectionError, ServerError

FIXTURES = Path(__file__).parent / "fixtures"


# --- parse_jackson ----------------------------------------------------------


def test_parse_jackson_dereferences_int_reference():
    person = {"@id": 1, "userId": "alice", "@type": "as.dto.person.Person"}
    graph = {
        "objects": [
            {"registrator": person, "code": "A"},
            {"registrator": 1, "code": "B"},
        ]
    }
    parse_jackson(graph)
    assert graph["objects"][1]["registrator"] is person


def test_parse_jackson_dereferences_lists():
    tag = {"@id": 7, "code": "TAG1", "@type": "as.dto.tag.Tag"}
    graph = {
        "objects": [
            {"tags": [tag], "code": "A"},
            {"tags": [7], "code": "B"},
        ]
    }
    parse_jackson(graph)
    assert graph["objects"][1]["tags"][0] is tag


def test_parse_jackson_resolves_back_references_into_cycles():
    """Wire graphs are trees with int back-references; dereferencing them
    produces the in-memory cycle (parent <-> child)."""
    child = {
        "@id": 2,
        "@type": "as.dto.sample.Sample",
        "code": "S2",
        "parents": [1],
    }
    parent = {
        "@id": 1,
        "@type": "as.dto.sample.Sample",
        "code": "S1",
        "children": [child],
    }
    graph = {"objects": [{"sample": parent}]}
    parse_jackson(graph)
    assert parent["children"][0]["parents"][0] is parent


def test_parse_jackson_top_level_objects_are_not_cached():
    """1.x behavior preserved: entities reachable only through non-'interesting'
    keys (like the top-level 'objects' list) are not registered, so integer
    references to them stay unresolved."""
    sample = {"@id": 1, "@type": "as.dto.sample.Sample", "code": "S1"}
    child = {
        "@id": 2,
        "@type": "as.dto.sample.Sample",
        "code": "S2",
        "parents": [1],
    }
    sample["children"] = [child]
    graph = {"objects": [sample]}
    parse_jackson(graph)
    assert graph["objects"][0]["children"][0]["parents"][0] == 1


def test_parse_jackson_fixture_roundtrip():
    """Golden test: a wire-format graph dereferences to the expected shape."""
    wire = json.loads((FIXTURES / "jackson_graph.json").read_text())
    parse_jackson(wire)
    objects = wire["objects"]
    # Both objects must share the identical registrator dict after deref.
    assert objects[0]["registrator"] is objects[1]["registrator"]
    assert objects[1]["registrator"]["userId"] == "admin"
    # The shared sample type must be dereferenced into object 2 as well.
    assert objects[1]["type"]["code"] == "MOLECULE"


# --- assign_jackson_ids -----------------------------------------------------


def test_assign_ids_to_typed_objects():
    payload = {"@type": "as.dto.sample.create.SampleCreation", "code": "X"}
    assign_jackson_ids(payload)
    assert payload["@id"] == 1


def test_assign_ids_reuses_reference_for_same_object():
    space = {"@type": "as.dto.space.id.SpacePermId", "permId": "SP"}
    payload = {
        "@type": "as.dto.sample.create.SampleCreation",
        "spaceId": space,
        "other": space,
    }
    result = assign_jackson_ids(payload)
    values = [result["spaceId"], result["other"]]
    # one stays the dict (with @id), the other becomes the integer reference
    assert any(isinstance(v, dict) for v in values)
    assert any(isinstance(v, int) for v in values)


def test_assign_ids_untyped_dicts_untouched():
    payload = {"code": "X", "nested": {"a": 1}}
    before = copy.deepcopy(payload)
    assign_jackson_ids(payload)
    assert payload == before


# --- type_for_id ------------------------------------------------------------


def test_type_for_id_permid():
    assert type_for_id("20240101000000000-1", "sample") == {
        "permId": "20240101000000000-1",
        "@type": "as.dto.sample.id.SamplePermId",
    }


def test_type_for_id_identifier():
    assert type_for_id("/SPACE/PROJ/CODE", "sample") == {
        "identifier": "/SPACE/PROJ/CODE",
        "@type": "as.dto.sample.id.SampleIdentifier",
    }


def test_type_for_id_adds_missing_slash_prefix():
    assert type_for_id("SPACE/CODE", "sample")["identifier"] == "/SPACE/CODE"


def test_type_for_id_strips_eln_experiment_part():
    """ELN-LIMS 4-part sample identifiers lose the experiment component."""
    result = type_for_id("/SPACE/PROJ/EXP/SAMP", "sample")
    assert result["identifier"] == "/SPACE/PROJ/SAMP"


def test_type_for_id_uppercases_identifier():
    assert type_for_id("/space/proj/code", "sample")["identifier"] == "/SPACE/PROJ/CODE"


def test_type_for_id_dataset_camelcase_type():
    assert (
        type_for_id("20240101000000000-1", "dataset")["@type"]
        == "as.dto.dataset.id.DataSetPermId"
    )


def test_type_for_id_tag_code_and_permid():
    assert type_for_id("MY_TAG", "tag") == {
        "code": "MY_TAG",
        "@type": "as.dto.tag.id.TagCode",
    }
    assert type_for_id("/admin/MY_TAG", "tag") == {
        "permId": "/admin/MY_TAG",
        "@type": "as.dto.tag.id.TagPermId",
    }


# --- RpcClient error mapping -------------------------------------------------


def _response(ok=True, payload=None, status_code=200, reason="OK"):
    resp = MagicMock()
    resp.ok = ok
    resp.status_code = status_code
    resp.reason = reason
    resp.json.return_value = payload if payload is not None else {}
    return resp


def _rpc():
    return RpcClient("https://example.invalid:8443", verify_certificates=False)


def test_post_returns_result():
    rpc = _rpc()
    with patch.object(
        rpc._session, "post", return_value=_response(payload={"result": {"ok": 1}})
    ):
        assert rpc.post("/res", {"method": "m", "params": ["tok"]}) == {"ok": 1}


def test_post_fills_jsonrpc_defaults():
    rpc = _rpc()
    with patch.object(
        rpc._session, "post", return_value=_response(payload={"result": None})
    ) as post:
        rpc.post("/res", {"method": "m", "params": ["tok"]})
        sent = json.loads(post.call_args[0][1])
        assert sent["id"] == "2"
        assert sent["jsonrpc"] == "2.0"


def test_post_without_token_raises_authentication_error():
    with pytest.raises(AuthenticationError):
        _rpc().post("/res", {"method": "m", "params": [None]})


def test_server_error_payload_raises_server_error():
    rpc = _rpc()
    payload = {"error": {"message": "entity does not exist"}}
    with patch.object(rpc._session, "post", return_value=_response(payload=payload)):
        with pytest.raises(ServerError, match="entity does not exist"):
            rpc.post("/res", {"method": "m", "params": ["tok"]})


def test_server_error_is_value_error_for_legacy_callers():
    rpc = _rpc()
    payload = {"error": {"message": "boom"}}
    with patch.object(rpc._session, "post", return_value=_response(payload=payload)):
        with pytest.raises(ValueError):
            rpc.post("/res", {"method": "m", "params": ["tok"]})


def test_http_error_status_raises_server_error_with_code():
    rpc = _rpc()
    resp = _response(ok=False, status_code=502, reason="Bad Gateway")
    with patch.object(rpc._session, "post", return_value=resp):
        with pytest.raises(ServerError) as excinfo:
            rpc.post("/res", {"method": "m", "params": ["tok"]})
    assert excinfo.value.code == 502


def test_missing_result_and_error_raises_server_error():
    rpc = _rpc()
    with patch.object(rpc._session, "post", return_value=_response(payload={})):
        with pytest.raises(ServerError, match="did not return"):
            rpc.post("/res", {"method": "m", "params": ["tok"]})


def test_connection_error_is_mapped():
    rpc = _rpc()
    with patch.object(
        rpc._session, "post", side_effect=requests.ConnectionError("nope")
    ):
        with pytest.raises(ConnectionError, match="Could not connect"):
            rpc.post("/res", {"method": "m", "params": ["tok"]})


def test_ssl_error_is_mapped_with_hint():
    rpc = _rpc()
    with pytest.raises(ConnectionError, match="verify_certificates=False"):
        with patch.object(
            rpc._session, "post", side_effect=requests.exceptions.SSLError("bad cert")
        ):
            rpc.post("/res", {"method": "m", "params": ["tok"]})
