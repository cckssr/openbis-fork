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
"""Role assignment entity for openBIS access control."""

from __future__ import annotations

from typing import Any, Optional, TYPE_CHECKING

from .attribute import AttrHolder
from .openbis_object import OpenBisObject
from .utils import VERBOSE

if TYPE_CHECKING:
    from .pybis import Openbis


class RoleAssignment(OpenBisObject):
    """A single role assignment in the openBIS access-control system.

    A role assignment binds a role (e.g. ``"ADMIN"``, ``"USER"``) at a
    particular scope (instance, space, or project) to either a
    :class:`~pybis.person.Person` or an
    :class:`~pybis.group.Group`.  Role assignments are created via
    :meth:`~pybis.person.Person.assign_role` or
    :meth:`~pybis.group.Group.assign_role` and can be deleted via
    :meth:`delete`.

    Attributes:
        id (int): Internal technical identifier (``techId``) of this assignment.
        role (str): Role name, e.g. ``"ADMIN"``, ``"POWER_USER"``,
            ``"USER"``, or ``"OBSERVER"``.
        roleLevel (str): Scope of the role — ``"INSTANCE"``, ``"SPACE"``,
            or ``"PROJECT"``.
        space (Optional[str]): Space code when ``roleLevel`` is ``"SPACE"``.
        project (Optional[str]): Project identifier when ``roleLevel`` is
            ``"PROJECT"``.
        group (Optional[str]): Authorization group code if this assignment
            targets a group rather than an individual person.

    Example:
        >>> ra = openbis.get_role_assignment(42)
        >>> print(ra.role, ra.roleLevel)
        ADMIN INSTANCE
        >>> ra.delete("No longer needed")
    """

    def __init__(
        self, openbis_obj: Any, data: Optional[dict[str, Any]] = None, **kwargs: Any
    ) -> None:
        """Initialise a RoleAssignment from raw V3 API data.

        Args:
            openbis_obj: The :class:`~pybis.Openbis` connection instance.
            data: Raw role-assignment dict as returned by the V3 API.
                When provided the attributes are populated via the internal
                :class:`~pybis.attribute.AttrHolder`.
            **kwargs: Additional key/value pairs set as attributes on
                construction.
        """
        self.__dict__["openbis"] = openbis_obj
        self.__dict__["a"] = AttrHolder(openbis_obj, "roleAssignment")  # type: ignore[no-untyped-call]  # reason: legacy attribute module

        if data is not None:
            self.a(data)
            self.__dict__["data"] = data

        if kwargs is not None:
            for key in kwargs:
                setattr(self, key, kwargs[key])

    def __dir__(self) -> list[str]:
        """Return the list of public attributes for tab-completion.

        Returns:
            A list of attribute names exposed by this entity.
        """
        return ["id", "role", "roleLevel", "space", "project", "group"]

    def __str__(self) -> str:
        """Return the role name."""
        return f"{self.get('role')}"

    def delete(self, reason: str = "no reason specified") -> None:  # type: ignore[override]  # reason: role assignments are deleted directly, no trash/confirm step
        """Delete this role assignment from openBIS.

        Calls ``deleteRoleAssignments`` via the V3 API.  After deletion the
        object becomes stale — do not use it further.

        Args:
            reason: Human-readable reason recorded with the deletion.
                Defaults to ``"no reason specified"``.

        Example:
            >>> ra = openbis.get_role_assignment(42)
            >>> ra.delete("Revoked during offboarding")
        """
        self.openbis.delete_openbis_entity(
            entity="roleAssignment", objectId=self._id, reason=reason
        )
        if VERBOSE:
            print(
                f"RoleAssignment role={self.role}, roleLevel={self.roleLevel} successfully deleted."
            )
