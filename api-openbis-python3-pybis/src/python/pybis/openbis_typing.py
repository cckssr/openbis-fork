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
"""Type aliases, NewTypes, and TypedDicts shared across the pybis package.

These definitions provide a vocabulary for the most common openBIS
identifiers and enumerations so that type-checkers and IDEs can surface
mistakes early.  Import from this module rather than repeating literals
inline.

Example:
    >>> from pybis.openbis_typing import AuthorizationRoles, DataSetKind

    >>> def grant(role: AuthorizationRoles, kind: DataSetKind) -> None: ...
"""

from __future__ import annotations

from typing import Literal, NewType, TypeAlias, TypedDict, TYPE_CHECKING

if TYPE_CHECKING:
    from .person import Person
    from .group import Group

# ---------------------------------------------------------------------------
# Scalar identity types
# ---------------------------------------------------------------------------

PermId = NewType("PermId", str)
"""Permanent identifier assigned by openBIS, e.g. ``"20251218172409814-1"``."""

SessionToken = NewType("SessionToken", str)
"""Session token returned after login, e.g. ``"user.name-datex128-bit-hex"``."""

Identifier = NewType("Identifier", str)
"""Human-readable path-style identifier, e.g. ``"/SPACE/PROJECT/COLLECTION"``."""

# ---------------------------------------------------------------------------
# Entity / role enumerations
# ---------------------------------------------------------------------------

EntityKindCode: TypeAlias = Literal["DATASET", "OBJECT", "COLLECTION"]
"""The three high-level entity kinds used in openBIS search criteria."""

AuthorizationRoles: TypeAlias = Literal["ADMIN", "POWER_USER", "USER", "OBSERVER"]
"""Role names that can be granted to users or groups."""

AuthorizationRoleLevels: TypeAlias = Literal["INSTANCE", "SPACE", "PROJECT"]
"""Scope at which a role assignment applies."""

DataSetKind: TypeAlias = Literal["PHYSICAL", "CONTAINER", "LINK"]
"""Physical storage kind of a :class:`~pybis.dataset.DataSet`.

- ``"PHYSICAL"`` — stores actual files in the data store.
- ``"CONTAINER"`` — groups other data sets; holds no files itself.
- ``"LINK"`` — references files in an external data management system.
"""

# ---------------------------------------------------------------------------
# Property data types
# ---------------------------------------------------------------------------

PropertyDataType: TypeAlias = Literal[
    "INTEGER",
    "VARCHAR",
    "MULTILINE_VARCHAR",
    "REAL",
    "TIMESTAMP",
    "BOOLEAN",
    "HYPERLINK",
    "XML",
    "CONTROLLEDVOCABULARY",
    "MATERIAL",  # Deprecated
]
"""Scalar data types supported by openBIS property types."""

PropertyDataArrayTypes: TypeAlias = Literal[
    "ARRAY_INTEGER", "ARRAY_REAL", "ARRAY_STRING", "ARRAY_TIMESTAMP"
]
"""Array-valued data types for multi-value properties."""


# ---------------------------------------------------------------------------
# Structured search helpers
# ---------------------------------------------------------------------------
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
