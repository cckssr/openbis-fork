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
"""Distinct string types for the different kinds of openBIS identifiers.

openBIS addresses entities through several string formats that are easy to
mix up.  These :func:`typing.NewType` wrappers let type-checkers tell them
apart while remaining plain ``str`` at runtime.

Example:
    >>> from pybis.types import PermId, Identifier

    >>> def link(perm_id: PermId, ident: Identifier) -> None: ...
"""

from __future__ import annotations

from typing import NewType

PermId = NewType("PermId", str)
"""Permanent identifier assigned by openBIS, e.g. ``"20251218172409814-1"``."""

Identifier = NewType("Identifier", str)
"""Human-readable path-style identifier, e.g. ``"/SPACE/PROJECT/COLLECTION"``."""

TypeCode = NewType("TypeCode", str)
"""Code of an entity type, e.g. ``"MOLECULE"`` or ``"RAW_DATA"``."""

SessionToken = NewType("SessionToken", str)
"""Session token returned after login, e.g. ``"username-241218..."``."""

SpaceCode = NewType("SpaceCode", str)
"""Code of a space, e.g. ``"MY_SPACE"``."""

ProjectCode = NewType("ProjectCode", str)
"""Code of a project (without the space prefix), e.g. ``"MY_PROJECT"``."""

__all__ = [
    "Identifier",
    "PermId",
    "ProjectCode",
    "SessionToken",
    "SpaceCode",
    "TypeCode",
]
