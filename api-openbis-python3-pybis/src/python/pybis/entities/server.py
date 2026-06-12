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
"""Server metadata: API version, feature flags, and version gating.

Version-gated client methods are decorated with :func:`requires_version` and
raise :class:`~pybis.exceptions.FeatureNotAvailableError` against servers
that are too old — failing fast with a clear message instead of a cryptic
server error.
"""

from __future__ import annotations

from dataclasses import dataclass
from functools import total_ordering, wraps
from typing import Any, Callable, ParamSpec, Protocol, TypeVar

from ..api.rpc import JsonPayload
from ..exceptions import FeatureNotAvailableError

P = ParamSpec("P")
R = TypeVar("R")


@total_ordering
@dataclass(frozen=True)
class ServerVersion:
    """An openBIS API version (major.minor).

    Attributes:
        major (int): Major API version, e.g. ``3``.
        minor (int): Minor API version, e.g. ``6``.
    """

    major: int
    minor: int

    def __lt__(self, other: ServerVersion) -> bool:
        """Order versions by (major, minor)."""
        return (self.major, self.minor) < (other.major, other.minor)

    def __str__(self) -> str:
        """Render as ``"major.minor"``."""
        return f"{self.major}.{self.minor}"


class ServerInformation:
    """Typed view of the openBIS ``getServerInformation`` response.

    Provides the API version, server feature flags, and the legacy
    dash-to-underscore attribute access (``info.project_samples_enabled``
    reads the ``project-samples-enabled`` field).

    Example:
        >>> info = client.get_server_information()

        >>> info.version
        ServerVersion(major=3, minor=6)

        >>> info.project_samples_enabled
        True
    """

    def __init__(self, info: JsonPayload) -> None:
        """Wrap a raw server-information payload.

        Args:
            info: The raw (string-valued) response of
                ``getServerInformation``.
        """
        self._info = self._reformat_info(dict(info))
        self.attrs = [
            "api_version",
            "archiving_configured",
            "authentication_service",
            "authentication_service.switch_aai.label",
            "authentication_service.switch_aai.link",
            "create_continuous_sample_codes",
            "enabled_technologies",
            "openbis_version",
            "openbis_support_email",
            "personal_access_tokens_enabled",
            "personal_access_tokens_max_validity_period",
            "personal_access_tokens_validity_warning_period",
            "project_samples_enabled",
            "server-public-information.afs-server.url",
            "server-public-information.afs-server.timeout",
        ]

    @classmethod
    def from_rpc(cls, payload: JsonPayload) -> ServerInformation:
        """Build from a raw JSON-RPC ``getServerInformation`` result."""
        return cls(payload)

    @staticmethod
    def _reformat_info(info: JsonPayload) -> JsonPayload:
        """Coerce the string-valued payload (bools, ints, csv lists)."""
        for bool_field in [
            "archiving-configured",
            "project-samples-enabled",
            "personal-access-tokens-enabled",
        ]:
            if bool_field in info:
                info[bool_field] = info[bool_field] == "true"
        for csv_field in ["enabled-technologies"]:
            if csv_field in info:
                info[csv_field] = [item.strip() for item in info[csv_field].split(",")]
        for int_field in [
            "personal-access-tokens-max-validity-period",
            "personal-access-tokens-validity-warning-period",
        ]:
            if int_field in info:
                info[int_field] = int(info[int_field])
        info["openbis-support-email"] = info.get("openbis.support.email", "")
        info.pop("openbis.support.email", "")
        return info

    # --- typed surface -----------------------------------------------------

    @property
    def version(self) -> ServerVersion:
        """The API version reported by the server."""
        parts = str(self._info["api-version"]).split(".")
        return ServerVersion(int(parts[0]), int(parts[1]))

    @property
    def version_str(self) -> str:
        """The API version as a ``"major.minor"`` string."""
        return str(self.version)

    @property
    def project_samples_enabled(self) -> bool:
        """True if the server allows objects directly under projects."""
        return bool(self._info.get("project-samples-enabled", False))

    @property
    def archiving_configured(self) -> bool:
        """True if dataset archiving is configured on the server."""
        return bool(self._info.get("archiving-configured", False))

    @property
    def personal_access_tokens_enabled(self) -> bool:
        """True if the server issues personal access tokens."""
        return bool(self._info.get("personal-access-tokens-enabled", False))

    def is_version_gte(self, major: int, minor: int) -> bool:
        """True if the server API version is at least ``major.minor``."""
        return self.version >= ServerVersion(major, minor)

    # --- legacy surface (kept for 1.x compatibility) -------------------------

    def get_major_version(self) -> int:
        """Major API version (legacy accessor)."""
        return self.version.major

    def get_minor_version(self) -> int:
        """Minor API version (legacy accessor)."""
        return self.version.minor

    def is_openbis_1605(self) -> bool:
        """True for openBIS 16.05 (API 3.minor<=2) servers (legacy)."""
        return (self.version.major == 3) and (self.version.minor <= 2)

    def is_openbis_1806(self) -> bool:
        """True for openBIS >= 18.06 (API 3.minor>=5) servers (legacy)."""
        return (self.version.major == 3) and (self.version.minor >= 5)

    def is_version_greater_than(self, major: int, minor: int) -> bool:
        """True if the server API version is strictly greater (legacy)."""
        return self.version > ServerVersion(major, minor)

    def get_service_props(self) -> dict[str, str]:
        """Parse the ``as-service-properties`` blob into a dict (legacy)."""
        result: dict[str, str] = {}
        if "as-service-properties" in self._info:
            props = str(self._info["as-service-properties"]).split("\n")[1:]
            result = {"_resolution_date": props[0]}
            for prop in props[1:]:
                split = prop.split("=")
                if len(split) > 1:
                    result[split[0]] = "=".join(split[1:])
        return result

    def __getattr__(self, name: str) -> Any:
        """Map ``foo_bar`` attribute access to the ``foo-bar`` server field.

        ``Any`` is justified here: this is the legacy passthrough for
        arbitrary, server-defined information fields.
        """
        return self._info.get(name.replace("_", "-"))

    def __dir__(self) -> list[str]:
        """Expose the known attributes for tab completion."""
        return self.attrs

    def __repr__(self) -> str:
        """Summarize the server version and key flags."""
        return (
            f"ServerInformation(version={self.version_str!r},"
            f" openbis_version={self._info.get('openbis-version')!r})"
        )

    def _repr_html_(self) -> str:
        """Render the known attributes as an HTML table for Jupyter."""
        html = """
            <table border="1" class="dataframe">
            <thead>
                <tr style="text-align: right;">
                <th>attribute</th>
                <th>value</th>
                </tr>
            </thead>
            <tbody>
        """

        for attr in self.attrs:
            html += f"<tr> <td>{attr}</td> <td>{getattr(self, attr, '')}</td> </tr>"

        html += """
            </tbody>
            </table>
        """
        return html


class _HasServerInformation(Protocol):
    """Anything that can report its server information (the client)."""

    def get_server_information(self) -> ServerInformation:
        """Return (possibly cached) server information."""
        ...


def requires_version(
    major: int, minor: int, feature: str = ""
) -> Callable[[Callable[..., R]], Callable[..., R]]:
    """Gate a client method on a minimum openBIS API version.

    Args:
        major: Required major API version.
        minor: Required minor API version.
        feature: Human-readable feature name for the error message
            (defaults to the method name).

    Returns:
        A decorator raising :class:`FeatureNotAvailableError` when the
        connected server is older than ``major.minor``.

    Example:
        >>> class Openbis:
        ...     @requires_version(3, 5, "JSON property type")
        ...     def some_json_feature(self): ...
    """

    def decorator(fn: Callable[..., R]) -> Callable[..., R]:
        @wraps(fn)
        def wrapper(self: _HasServerInformation, *args: object, **kwargs: object) -> R:
            info = self.get_server_information()
            if not info.is_version_gte(major, minor):
                name = feature or fn.__name__
                raise FeatureNotAvailableError(
                    f"{name!r} requires openBIS API >= {major}.{minor},"
                    f" server is {info.version_str}"
                )
            return fn(self, *args, **kwargs)

        return wrapper

    return decorator


def require_server_flag(info: ServerInformation, flag: str, feature: str) -> None:
    """Raise unless a boolean server flag is enabled.

    Args:
        info: The server information to check.
        flag: Attribute name of the flag, e.g. ``"project_samples_enabled"``.
        feature: Human-readable feature name for the error message.

    Raises:
        FeatureNotAvailableError: The flag is off on the connected server.
    """
    if not getattr(info, flag, False):
        raise FeatureNotAvailableError(
            f"{feature} requires the server setting {flag!r} to be enabled"
        )


__all__ = [
    "ServerInformation",
    "ServerVersion",
    "require_server_flag",
    "requires_version",
]
