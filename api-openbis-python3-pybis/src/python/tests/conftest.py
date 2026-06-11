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
"""Shared pytest configuration: integration gating and the mocked client.

Unit tests (``tests/unit``) run fully offline against a mocked JSON-RPC
transport.  Integration tests (``tests/integration*``) require a live
openBIS instance and only run with ``--integration``::

    pytest --integration \
        --openbis-url https://localhost:8443 \
        --openbis-user admin --openbis-pass admin
"""

from unittest.mock import patch

import pytest

from pybis import Openbis


def pytest_addoption(parser):
    parser.addoption(
        "--integration",
        action="store_true",
        default=False,
        help="Run integration tests against a live openBIS instance",
    )
    parser.addoption("--openbis-url", default=None)
    parser.addoption("--openbis-user", default=None)
    parser.addoption("--openbis-pass", default=None)


def pytest_configure(config):
    config.addinivalue_line(
        "markers",
        "integration: requires a live openBIS instance (skip without --integration)",
    )


def pytest_collection_modifyitems(config, items):
    run_integration = config.getoption("--integration")
    skip = pytest.mark.skip(reason="pass --integration to run")
    for item in items:
        # Everything under tests/integration* talks to a live server.
        if any(part.startswith("integration") for part in item.path.parts):
            item.add_marker(pytest.mark.integration)
        if "integration" in item.keywords and not run_integration:
            item.add_marker(skip)


@pytest.fixture
def mock_rpc():
    """Patch the JSON-RPC transport class used by Openbis; yields the mock class.

    ``mock_rpc.return_value`` is the RpcClient instance the client will use;
    configure ``mock_rpc.return_value.post.return_value`` (or
    ``side_effect``) to script server responses.
    """
    with patch("pybis.client.RpcClient", autospec=True) as mock:
        yield mock


@pytest.fixture
def client(mock_rpc, monkeypatch):
    """An authenticated Openbis client with a mocked transport."""
    # Never read or write the real ~/.pybis token store in unit tests.
    monkeypatch.setattr(Openbis, "_get_saved_token", lambda self: None)
    monkeypatch.setattr(Openbis, "_save_token_to_disk", lambda self, *a, **k: None)

    mock_rpc.return_value.post.return_value = "mock-token-123"
    o = Openbis("https://mock.openbis.example.com")
    o.login("user", "password")
    # Leave response scripting to the individual test.
    mock_rpc.return_value.post.reset_mock()
    return o
