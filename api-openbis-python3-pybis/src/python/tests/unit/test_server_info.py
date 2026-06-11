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
"""Unit tests for ServerInformation, ServerVersion, and version gating."""

import pytest

from pybis.entities.server import (
    ServerInformation,
    ServerVersion,
    require_server_flag,
    requires_version,
)
from pybis.exceptions import FeatureNotAvailableError

RAW_INFO = {
    "api-version": "3.6",
    "openbis-version": "20.10.7",
    "project-samples-enabled": "true",
    "archiving-configured": "false",
    "personal-access-tokens-enabled": "true",
    "personal-access-tokens-max-validity-period": "2592000",
    "enabled-technologies": "eln-lims, flow",
    "openbis.support.email": "support@example.com",
}


def make_info(**overrides):
    raw = dict(RAW_INFO)
    raw.update(overrides)
    return ServerInformation.from_rpc(raw)


# --- ServerVersion ------------------------------------------------------------


def test_version_ordering():
    assert ServerVersion(3, 6) > ServerVersion(3, 5)
    assert ServerVersion(4, 0) > ServerVersion(3, 9)
    assert ServerVersion(3, 5) >= ServerVersion(3, 5)
    assert ServerVersion(3, 4) < ServerVersion(3, 5)


def test_version_str():
    assert str(ServerVersion(3, 6)) == "3.6"


# --- ServerInformation ----------------------------------------------------------


def test_version_parsed():
    assert make_info().version == ServerVersion(3, 6)


def test_is_version_gte():
    info = make_info()
    assert info.is_version_gte(3, 6)
    assert info.is_version_gte(3, 5)
    assert not info.is_version_gte(3, 7)
    assert not info.is_version_gte(4, 0)


def test_legacy_is_version_greater_than_is_strict():
    info = make_info()
    assert info.is_version_greater_than(3, 5)
    assert not info.is_version_greater_than(3, 6)


def test_boolean_flags_coerced():
    info = make_info()
    assert info.project_samples_enabled is True
    assert info.archiving_configured is False
    assert info.personal_access_tokens_enabled is True


def test_flags_default_false_when_missing():
    info = ServerInformation({"api-version": "3.6"})
    assert info.project_samples_enabled is False


def test_enabled_technologies_split():
    assert make_info().enabled_technologies == ["eln-lims", "flow"]


def test_int_fields_coerced():
    assert make_info().personal_access_tokens_max_validity_period == 2592000


def test_support_email_remapped():
    assert make_info().openbis_support_email == "support@example.com"


def test_dash_attribute_passthrough():
    assert make_info().openbis_version == "20.10.7"


def test_legacy_major_minor_accessors():
    info = make_info()
    assert info.get_major_version() == 3
    assert info.get_minor_version() == 6


def test_repr_mentions_version():
    assert "3.6" in repr(make_info())


# --- requires_version decorator ---------------------------------------------------


class FakeClient:
    def __init__(self, api_version):
        self._info = ServerInformation({"api-version": api_version})

    def get_server_information(self):
        return self._info

    @requires_version(3, 5, "JSON property type")
    def json_feature(self):
        return "ran"

    @requires_version(3, 1)
    def array_feature(self):
        return "ran"


def test_requires_version_passes_on_new_server():
    assert FakeClient("3.6").json_feature() == "ran"


def test_requires_version_passes_on_exact_version():
    assert FakeClient("3.5").json_feature() == "ran"


def test_requires_version_raises_on_old_server():
    with pytest.raises(FeatureNotAvailableError) as excinfo:
        FakeClient("3.4").json_feature()
    msg = str(excinfo.value)
    assert "JSON property type" in msg
    assert "3.5" in msg
    assert "3.4" in msg


def test_requires_version_defaults_feature_to_method_name():
    with pytest.raises(FeatureNotAvailableError, match="array_feature"):
        FakeClient("3.0").array_feature()


# --- require_server_flag ------------------------------------------------------------


def test_require_server_flag_passes_when_enabled():
    require_server_flag(make_info(), "project_samples_enabled", "Project objects")


def test_require_server_flag_raises_when_disabled():
    info = make_info(**{"project-samples-enabled": "false"})
    with pytest.raises(FeatureNotAvailableError, match="project_samples_enabled"):
        require_server_flag(info, "project_samples_enabled", "Project objects")
