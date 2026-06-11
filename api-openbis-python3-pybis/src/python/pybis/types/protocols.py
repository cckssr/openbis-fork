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
"""Structural protocols implemented by pybis entities and the client.

These exist so that helper code (and user code) can depend on capabilities
— "can be saved", "has properties" — instead of concrete entity classes.
``ClientProtocol`` also breaks the import cycle between entity modules and
:mod:`pybis.client`: entities annotate their client as ``ClientProtocol``
and never import the client module.
"""

from __future__ import annotations

from collections.abc import MutableMapping
from typing import Protocol, runtime_checkable

from .values import PropertyValue


@runtime_checkable
class Saveable(Protocol):
    """An entity that can be persisted to openBIS."""

    def save(self) -> None:
        """Persist pending changes to openBIS."""
        ...


@runtime_checkable
class Deletable(Protocol):
    """An entity that can be deleted from openBIS."""

    def delete(self, reason: str) -> None:
        """Delete the entity.

        Args:
            reason: Human-readable justification recorded on the server.
        """
        ...


@runtime_checkable
class HasProperties(Protocol):
    """An entity that carries user-defined properties."""

    @property
    def props(self) -> MutableMapping[str, PropertyValue]:
        """Dict-like access to the entity's properties."""
        ...


@runtime_checkable
class ClientProtocol(Protocol):
    """The minimal client surface that entity code is allowed to depend on.

    Grows alongside the refactor; entity modules must annotate their
    ``openbis`` reference with this protocol instead of importing
    :class:`pybis.client.Openbis`.
    """

    @property
    def url(self) -> str:
        """Base URL of the openBIS application server."""
        ...

    @property
    def token(self) -> str | None:
        """Current session or personal access token, if authenticated."""
        ...


__all__ = [
    "ClientProtocol",
    "Deletable",
    "HasProperties",
    "Saveable",
]
