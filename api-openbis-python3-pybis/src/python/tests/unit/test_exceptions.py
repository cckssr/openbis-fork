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
"""Unit tests for the pybis exception hierarchy."""

import pytest

from pybis.exceptions import (
    AuthenticationError,
    ConnectionError,
    FeatureNotAvailableError,
    NotFoundError,
    PermissionError,
    PybisError,
    ServerError,
    ValidationError,
)

ALL_ERRORS = [
    AuthenticationError,
    ConnectionError,
    FeatureNotAvailableError,
    NotFoundError,
    PermissionError,
    ServerError,
    ValidationError,
]


@pytest.mark.parametrize("exc_type", ALL_ERRORS)
def test_all_errors_derive_from_pybis_error(exc_type):
    assert issubclass(exc_type, PybisError)
    assert issubclass(exc_type, Exception)


@pytest.mark.parametrize(
    "exc_type",
    [AuthenticationError, NotFoundError, PermissionError, ServerError, ValidationError],
)
def test_legacy_value_error_compatibility(exc_type):
    """pybis 1.x raised ValueError; `except ValueError` must keep working."""
    assert issubclass(exc_type, ValueError)


def test_not_found_error_attributes():
    err = NotFoundError("object", "20240101-1")
    assert err.entity_type == "object"
    assert err.identifier == "20240101-1"
    assert "object" in str(err)
    assert "20240101-1" in str(err)


def test_server_error_carries_code():
    err = ServerError("boom", code=500)
    assert err.code == 500
    assert "boom" in str(err)


def test_server_error_code_defaults_to_none():
    assert ServerError("boom").code is None


def test_catching_base_class_catches_all():
    for exc_type in ALL_ERRORS:
        with pytest.raises(PybisError):
            if exc_type is NotFoundError:
                raise exc_type("object", "x")
            raise exc_type("x")
