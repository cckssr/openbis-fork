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
"""Typed exception hierarchy raised by pybis.

Every error raised by pybis derives from :class:`PybisError`, so callers can
catch one base class.  More specific subclasses distinguish authentication
problems, missing entities, validation failures, transport problems, and
server-side errors.

Example:
    >>> from pybis.exceptions import NotFoundError, PybisError

    >>> try:
    ...     obj = client.get_object_or_raise("20240101-1")
    ... except NotFoundError as err:
    ...     print(err.entity_type, err.identifier)
    ... except PybisError:
    ...     raise
"""

from __future__ import annotations


class PybisError(Exception):
    """Base class of every exception raised by pybis."""


# Several subclasses additionally inherit ValueError: pybis 1.x raised bare
# ValueError for these conditions, and existing user code (and the legacy
# test suite) catches it. The extra base keeps `except ValueError` working
# during the v1 -> v2 migration window.


class AuthenticationError(PybisError, ValueError):
    """Login failed, or the session/token is missing, expired, or invalid."""


class NotFoundError(PybisError, ValueError):
    """A ``get_*_or_raise`` lookup did not match any entity.

    Attributes:
        entity_type (str): Kind of entity that was looked up, e.g. ``"object"``.
        identifier (str): The perm_id, identifier, or code that did not match.
    """

    def __init__(self, entity_type: str, identifier: str) -> None:
        """Build the error message from the entity kind and identifier.

        Args:
            entity_type: Kind of entity that was looked up, e.g. ``"object"``.
            identifier: The perm_id, identifier, or code that did not match.
        """
        self.entity_type = entity_type
        self.identifier = identifier
        super().__init__(f"No {entity_type} found for {identifier!r}")


class ValidationError(PybisError, ValueError):
    """A value does not satisfy entity or property constraints."""


class ConnectionError(PybisError):  # noqa: A001 — mirrors the spec'd public name
    """The openBIS server could not be reached (network, TLS, timeouts)."""


class PermissionError(PybisError, ValueError):  # noqa: A001 — spec'd public name
    """The authenticated user lacks the rights for the attempted operation."""


class ServerError(PybisError, ValueError):
    """openBIS reported an error while executing the request.

    Attributes:
        code (int | None): Numeric JSON-RPC error code, when provided.
    """

    def __init__(self, message: str, code: int | None = None) -> None:
        """Store the server message and optional JSON-RPC error code.

        Args:
            message: Error message reported by the server.
            code: Numeric JSON-RPC error code, when provided.
        """
        self.code = code
        super().__init__(message)


class FeatureNotAvailableError(PybisError):
    """The connected openBIS server is too old or lacks a required feature."""


__all__ = [
    "AuthenticationError",
    "ConnectionError",
    "FeatureNotAvailableError",
    "NotFoundError",
    "PermissionError",
    "PybisError",
    "ServerError",
    "ValidationError",
]
