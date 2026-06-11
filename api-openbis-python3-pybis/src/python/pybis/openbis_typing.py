#
#   Copyright ETH 2018 - 2026 Zürich, Scientific IT Services
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
"""Transitional shim over :mod:`pybis.types` for legacy modules.

Deprecated: import from :mod:`pybis.types` instead.  This module disappears
once the last legacy import is migrated.
"""

from __future__ import annotations

from typing import TYPE_CHECKING, Literal, TypeAlias, TypedDict

from .types import (
    DataSetKind,
    Identifier,
    PermId,
    RoleCode as AuthorizationRoles,
    RoleLevel as AuthorizationRoleLevels,
    SessionToken,
)
from .types.literals import DataType as PropertyDataType

if TYPE_CHECKING:
    from .group import Group
    from .person import Person

EntityKindCode: TypeAlias = Literal["DATASET", "OBJECT", "COLLECTION"]
"""The three high-level entity kinds used in openBIS search criteria."""

PropertyDataArrayTypes: TypeAlias = Literal[
    "ARRAY_INTEGER", "ARRAY_REAL", "ARRAY_STRING", "ARRAY_TIMESTAMP"
]
"""Array-valued data types for multi-value properties."""


class RoleAssignmentSearch(TypedDict, total=False):
    """Keyword filters accepted by ``Openbis.get_role_assignments()``.

    All keys are optional.  Combine them to narrow the result set.

    Attributes:
        role (Optional[str]): Restrict to a specific role name, e.g. ``"ADMIN"``.
        roleLevel (Optional[str]): Restrict by scope — ``"INSTANCE"``, ``"SPACE"``, or
            ``"PROJECT"``.
        person (Optional[Any]): A ``userId`` string or :class:`~pybis.person.Person` object.
        group (Optional[Any]): A group code string or :class:`~pybis.group.Group` object.
        space (Optional[str]): A space code string, e.g. ``"MY_SPACE"``.
    """

    role: AuthorizationRoles
    roleLevel: AuthorizationRoleLevels
    person: str | Person
    group: str | Group
    space: str


__all__ = [
    "AuthorizationRoleLevels",
    "AuthorizationRoles",
    "DataSetKind",
    "EntityKindCode",
    "Identifier",
    "PermId",
    "PropertyDataArrayTypes",
    "PropertyDataType",
    "RoleAssignmentSearch",
    "SessionToken",
]
