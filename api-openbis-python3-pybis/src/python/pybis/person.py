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
"""Person (user) entity for openBIS."""

from __future__ import annotations

from itertools import chain
from typing import Any, Optional, cast, TYPE_CHECKING

from pandas import DataFrame

from .attribute import AttrHolder
from .openbis_object import OpenBisObject
from .openbis_typing import AuthorizationRoles, AuthorizationRoleLevels
from .things import Things
from .utils import (
    VERBOSE,
    extract_code,
    extract_id,
    extract_nested_identifier,
    extract_userId,
    parse_jackson,
)

if TYPE_CHECKING:
    from .pybis import Openbis


class Person(OpenBisObject):
    """An openBIS user account (person).

    Person objects represent individual user accounts registered in
    openBIS.  They can be fetched via ``openbis.get_person()`` or
    ``openbis.get_persons()``.

    Use :meth:`get_roles` to see all roles assigned directly to the user
    or inherited through group membership.  Use :meth:`assign_role` and
    :meth:`revoke_role` to manage access control.

    Attributes:
        permId (str): Permanent identifier (usually the ``userId``).
        userId (str): Login name of the user.
        firstName (Optional[str]): First name.
        lastName (Optional[str]): Last name.
        email (Optional[str]): E-mail address.
        registrationDate (Optional[str]): ISO-8601 timestamp of account creation.
        space (Optional[str]): Home space code, if any.

    Example:
        >>> person = openbis.get_person("john.doe")
        >>> person.assign_role("USER", space="MY_SPACE")
        >>> person.get_roles().df
    """

    def __init__(
        self, openbis_obj: Any, data: Optional[dict[str, Any]] = None, **kwargs: Any
    ) -> None:
        """Initialise a Person from raw V3 API data.

        Args:
            openbis_obj: The :class:`~pybis.Openbis` connection instance.
            data: Raw person dict as returned by the V3 API.  When provided
                the attributes are populated via :class:`~pybis.attribute.AttrHolder`.
            **kwargs: Additional key/value pairs set as attributes.
        """
        self.__dict__["openbis"] = openbis_obj
        self.__dict__["a"] = AttrHolder(openbis_obj, "person")

        if data is not None:
            self.a(data)
            self.__dict__["data"] = data

        if kwargs is not None:
            for key in kwargs:
                setattr(self, key, kwargs[key])

    def __dir__(self) -> list[str]:
        """Return public attributes and methods for tab-completion.

        Returns:
            A list of attribute and method names.
        """
        return [
            "permId",
            "userId",
            "firstName",
            "lastName",
            "email",
            "registrator",
            "registrationDate",
            "space",
            "get_roles()",
            "assign_role(role, space)",
            "revoke_role(role)",
        ]

    def get_roles(self, **search_args: Any) -> Things:
        """Return all roles assigned to this person, including group roles.

        Merges roles assigned directly to the user with roles inherited
        through group membership.  Additional keyword arguments are passed
        to ``openbis.get_role_assignments()`` for filtering.

        Args:
            **search_args: Optional search filters, e.g. ``space="MY_SPACE"``.

        Returns:
            A :class:`~pybis.things.Things` container whose ``.df`` gives a
            :class:`~pandas.DataFrame` with columns ``techId``, ``role``,
            ``roleLevel``, ``user``, ``group``, ``space``, ``project``.

        Example:
            >>> person.get_roles().df
            >>> person.get_roles(space="TEST_SPACE").df
        """
        roles = self.openbis.get_role_assignments(person=self, **search_args)
        groups = self.openbis.get_groups(userId=self.userId, **search_args)

        group_roles = chain.from_iterable(
            map(lambda x: x["roleAssignments"], groups.response["objects"])
        )
        count = len(roles) + groups.response["totalCount"]
        response_combined = roles.response["objects"] + list(group_roles)

        return Things(
            openbis_obj=self.openbis,
            entity="role_assignment",
            identifier_name="techId",
            start_with=0,
            count=0,
            totalCount=count,
            response=response_combined,
            df_initializer=self._create_role_assigment_data_frame,
        )

    def _create_role_assigment_data_frame(
        self, attrs: Any, props: Any, response: "list[Any]"
    ) -> DataFrame:
        """Build the role-assignment DataFrame from raw V3 API objects.

        Args:
            attrs: Unused — kept for interface compatibility.
            props: Unused — kept for interface compatibility.
            response: List of raw role-assignment dicts.

        Returns:
            A :class:`~pandas.DataFrame` with columns ``techId``, ``role``,
            ``roleLevel``, ``user``, ``group``, ``space``, ``project``.
        """
        attrs = ["techId", "role", "roleLevel", "user", "group", "space", "project"]
        if len(response) == 0:
            roles = DataFrame(columns=attrs)
        else:
            objects = response
            parse_jackson(objects)
            roles = DataFrame(objects)
            roles["techId"] = roles["id"].map(extract_id)
            roles["user"] = roles["user"].map(extract_userId)
            roles["group"] = roles["authorizationGroup"].map(extract_code)
            spaces_s = roles["space"].map(extract_code)
            spaces_p = roles["project"].map(
                lambda x: x["space"]["code"] if x is not None else ""
            )
            roles["space"] = spaces_s + spaces_p
            roles["project"] = roles["project"].map(extract_nested_identifier)
        return cast(DataFrame, roles[roles.columns.intersection(attrs)])

    def assign_role(self, role: AuthorizationRoles, **kwargs: Any) -> None:
        """Assign a role to this person.

        The scope is determined by optional keyword arguments:

        - No extra args → ``roleLevel`` is ``"INSTANCE"``.
        - ``space=...`` → ``roleLevel`` is ``"SPACE"``.
        - ``project=...`` → ``roleLevel`` is ``"PROJECT"``.

        If the role is already assigned at the requested scope the call
        is silently ignored.

        Args:
            role: Role name, e.g. ``"ADMIN"``, ``"USER"``, etc.
            **kwargs: Optional scope arguments: ``space`` or ``project``.

        Raises:
            ValueError: If the server returns an error unrelated to a
                duplicate assignment.

        Example:
            >>> person.assign_role("USER", space="MY_SPACE")
            >>> person.assign_role("ADMIN")
        """
        try:
            self.openbis.assign_role(role=role, person=self, **kwargs)
            if VERBOSE:
                print(f"Role {role} successfully assigned to person {self.userId}")
        except ValueError as e:
            if "exists" in str(e):
                if VERBOSE:
                    print(f"Role {role} already assigned to person {self.userId}")
            else:
                raise ValueError(str(e))

    def revoke_role(
        self,
        role: AuthorizationRoles | int,
        space: Optional[str] = None,
        project: Optional[str] = None,
        reason: str = "no reason specified",
    ) -> None:
        """Revoke a role from this person.

        The role to remove can be identified by its numeric ``techId`` (int)
        or by role name combined with optional ``space``/``project`` scope
        filters.  When a name is given, the matching assignment is looked up
        via :meth:`get_roles` and resolved to a ``techId`` before deletion.

        If no matching role is found (e.g. already revoked), the call
        returns silently.

        Args:
            role: Either the integer ``techId`` of the role assignment, or a
                role name string (e.g. ``"ADMIN"``).
            space: Restrict lookup to a specific space code.  Uppercased
                automatically.  ``None`` matches instance-level roles.
            project: Restrict lookup to a specific project code.  Uppercased
                automatically.  ``None`` matches non-project roles.
            reason: Human-readable reason recorded with the deletion.
                Defaults to ``"no reason specified"``.

        Example:
            >>> person.revoke_role("USER", space="MY_SPACE")
            >>> person.revoke_role(42)  # by techId
        """
        techId = None
        if isinstance(role, int):
            techId = role
        else:
            query: dict[str, str] = {"role": role}
            if space is None:
                query["space"] = ""
            else:
                if isinstance(space, str):
                    query["space"] = space.upper()
                else:
                    query["space"] = space.code.upper()

            if project is None:
                query["project"] = ""
            else:
                if isinstance(project, str):
                    query["project"] = project.upper()
                else:
                    query["project"] = project.code.upper()

            # build a query string for dataframe
            querystr = " & ".join(f'{key} == "{value}"' for key, value in query.items())
            roles = self.get_roles().df
            if len(roles) == 0:
                if VERBOSE:
                    print(
                        f"Role {role} has already been revoked from person {self.code}"
                    )
                return
            techId = roles.query(querystr)["techId"].values[0]

        # finally delete the role assignment
        ra = self.openbis.get_role_assignment(techId)
        ra.delete(reason)
        if VERBOSE:
            print(f"Role {role} successfully revoked from person {self.code}")
        return

    def __str__(self) -> str:
        """Return first and last name."""
        return f"{self.get('firstName')} {self.get('lastName')}"

    def delete(self, reason: str) -> None:  # type: ignore[override]  # reason: persons cannot be deleted at all
        """Persons cannot be deleted via the openBIS V3 API.

        Args:
            reason: Unused.

        Raises:
            ValueError: Always — person deletion is not supported.
        """
        raise ValueError("Persons cannot be deleted")

    def save(self) -> Optional["Person"]:  # type: ignore[override]  # reason: 1.x returns None after updates
        """Persist this person to openBIS (create or update).

        Returns:
            This :class:`Person` instance, updated with server-assigned
            fields.

        Example:
            >>> person = openbis.new_person(userId="jane.doe", space="MY_SPACE")
            >>> person.save()
        """
        if self.is_new:
            request = self._new_attrs()
            resp = self.openbis._post_request(self.openbis.as_v3, request)
            if VERBOSE:
                print("Person successfully created.")
            new_person_data = self.openbis.get_person_or_raise(resp[0]["permId"]).data
            self._set_data(new_person_data)
            return self

        else:
            request = self._up_attrs()
            self.openbis._post_request(self.openbis.as_v3, request)
            if VERBOSE:
                print("Person successfully updated.")
            new_person_data = self.openbis.get_person_or_raise(self.permId).data
            self._set_data(new_person_data)
            return None
