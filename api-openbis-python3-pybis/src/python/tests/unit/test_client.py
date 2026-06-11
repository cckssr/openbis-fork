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
"""Unit tests for Openbis client construction and session lifecycle."""

import json

import pytest

from pybis import Openbis


@pytest.fixture(autouse=True)
def _no_token_store(monkeypatch):
    """Unit tests must never touch the real ~/.pybis token store."""
    monkeypatch.setattr(Openbis, "_get_saved_token", lambda self: None)
    monkeypatch.setattr(Openbis, "_save_token_to_disk", lambda self, *a, **k: None)


# --- construction -----------------------------------------------------------


def test_init_requires_url(monkeypatch):
    monkeypatch.delenv("OPENBIS_URL", raising=False)
    monkeypatch.delenv("OPENBIS_HOST", raising=False)
    with pytest.raises(ValueError, match="provide a URL"):
        Openbis()


def test_init_url_from_environment(mock_rpc, monkeypatch):
    monkeypatch.setenv("OPENBIS_URL", "https://env.openbis.example.com")
    o = Openbis()
    assert o.url == "https://env.openbis.example.com"


def test_init_prepends_https(mock_rpc):
    o = Openbis("openbis.example.com")
    assert o.url == "https://openbis.example.com"


def test_init_rejects_plain_http(mock_rpc):
    with pytest.raises(ValueError, match="always use https"):
        Openbis("http://openbis.example.com")


def test_init_allows_http_with_explicit_opt_in(mock_rpc):
    o = Openbis(
        "http://localhost:8888",
        allow_http_but_do_not_use_this_in_production_and_only_within_safe_networks=True,
    )
    assert o.url == "http://localhost:8888"


def test_init_sets_hostname_and_rpc(mock_rpc):
    o = Openbis("https://openbis.example.com:8443")
    assert o.hostname == "openbis.example.com"
    assert o.port == 8443
    mock_rpc.assert_called_once_with(
        "https://openbis.example.com:8443", verify_certificates=True
    )


def test_init_passes_verify_certificates_to_rpc(mock_rpc):
    Openbis("https://openbis.example.com", verify_certificates=False)
    assert mock_rpc.call_args.kwargs["verify_certificates"] is False


# --- login / logout ---------------------------------------------------------


def test_login_sets_token(mock_rpc):
    mock_rpc.return_value.post.return_value = "user-240101-abc"
    o = Openbis("https://mock.openbis.example.com")
    token = o.login("user", "password")
    assert token == "user-240101-abc"
    assert o.token == "user-240101-abc"


def test_login_sends_credentials(mock_rpc):
    mock_rpc.return_value.post.return_value = "user-240101-abc"
    o = Openbis("https://mock.openbis.example.com")
    o.login("user", "password")
    first_call = mock_rpc.return_value.post.call_args_list[0]
    request = first_call[0][1]
    assert request["method"] == "login"
    assert request["params"] == ["user", "password"]


def test_login_failure_raises(mock_rpc):
    mock_rpc.return_value.post.return_value = None
    o = Openbis("https://mock.openbis.example.com")
    with pytest.raises(ValueError, match="login to openBIS failed"):
        o.login("user", "wrong-password")


def test_logout_clears_token(client, mock_rpc):
    mock_rpc.return_value.post.return_value = None
    client.logout()
    assert client.token is None


def test_logout_without_session_is_noop(mock_rpc):
    o = Openbis("https://mock.openbis.example.com")
    assert o.logout() is None
    mock_rpc.return_value.post.assert_not_called()


def test_is_token_valid_false_without_token(mock_rpc):
    o = Openbis("https://mock.openbis.example.com")
    assert o.is_token_valid() is False


# --- context manager --------------------------------------------------------


def test_context_manager_returns_client(mock_rpc):
    with Openbis("https://mock.openbis.example.com") as o:
        assert isinstance(o, Openbis)


def test_context_manager_logs_out(mock_rpc):
    mock_rpc.return_value.post.return_value = "user-240101-abc"
    with Openbis("https://mock.openbis.example.com") as o:
        o.login("user", "password")
        mock_rpc.return_value.post.reset_mock()
        mock_rpc.return_value.post.return_value = None
    sent = mock_rpc.return_value.post.call_args[0][1]
    assert sent["method"] == "logout"
    assert o.token is None


def test_context_manager_swallows_logout_failure(mock_rpc):
    mock_rpc.return_value.post.return_value = "user-240101-abc"
    with Openbis("https://mock.openbis.example.com") as o:
        o.login("user", "password")
        mock_rpc.return_value.post.side_effect = RuntimeError("session gone")
    # leaving the context despite the logout failure is the assertion


# --- client fixture sanity ---------------------------------------------------


def test_client_fixture_is_authenticated(client):
    assert client.token == "mock-token-123"
