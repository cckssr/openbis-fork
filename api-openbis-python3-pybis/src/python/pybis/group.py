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
"""Authorization group entity for openBIS."""

from __future__ import annotations

from typing import Any, Optional, cast

from pandas import DataFrame

from .openbis_object import OpenBisObject
from .things import Things
from .utils import VERBOSE, extract_permid, extract_nested_permid, format_timestamp

from .openbis_typing import AuthorizationRoles


class Group(
    OpenBisObject, entity="authorizationGroup", single_item_method_name="get_group"
):
    """An openBIS authorization group controlling access via role assignments.

    A group collects :class:`~pybis.person.Person` objects under a shared
    identity so that roles can be granted at the group level instead of
    per individual user.  Roles may be scoped to the entire instance, a
    specific :class:`~pybis.space.Space`, or a specific
    :class:`~pybis.project.Project`.

    Fetch groups via ``openbis.get_group()`` / ``openbis.get_groups()`` and
    create new ones with ``openbis.new_group()``.

    Attributes:
        code (str): Unique group code (read-only after creation).
        description (str): Human-readable description of the group.
        users (list): Raw list of user dicts as returned by the V3 API.
        roleAssignments (list): Raw list of role-assignment dicts.

    Example:
        >>> group = openbis.new_group(
        ...     code="DATA_MANAGERS", description="Data management team"
        ... )
        >>> group.save()
        >>> group.assign_role("ADMIN", space="MY_SPACE")
        >>> group.get_users().df
    """

    def __dir__(self) -> list[str]:
        """Return the public interface of a :class:`Group` for tab-completion.

        Returns:
            A list of attribute and method names for this entity.
        """
        return [
            "code",
            "description",
            "users",
            "roleAssignments",
            "get_users()",
            "set_users()",
            "add_users()",
            "del_users()",
            "get_roles()",
            "assign_role()",
            "revoke_role(techId)",
        ]

    def get_persons(self) -> Things:
        """Return all persons (users) that belong to this group.

        Builds a :class:`~pybis.things.Things` container whose ``.df``
        property yields a :class:`~pandas.DataFrame` with columns
        ``permId``, ``userId``, ``firstName``, ``lastName``, ``email``,
        ``space``, ``registrationDate``, ``active``.

        Returns:
            :class:`~pybis.things.Things`: Container wrapping the group's
            current member list.

        Example:
            >>> group.get_persons().df
        """

        def create_data_frame(
            attrs: Any, props: Any, response: "list[Any]"
        ) -> DataFrame:
            columns = [
                "permId",
                "userId",
                "firstName",
                "lastName",
                "email",
                "space",
                "registrationDate",
                "active",
            ]
            persons = DataFrame(response)
            if len(persons) == 0:
                persons = DataFrame(columns=columns)
            persons["permId"] = persons["permId"].map(extract_permid)
            persons["registrationDate"] = persons["registrationDate"].map(
                format_timestamp
            )
            persons["space"] = persons["space"].map(extract_nested_permid)

            return cast("DataFrame", persons[columns])

        p = Things(
            self.openbis,
            entity="person",
            identifier_name="permId",
            response=self._users,
            df_initializer=create_data_frame,
        )
        return p

    get_users = get_persons
    get_members = get_persons

    def get_roles(self, **search_args: Any) -> Things:
        """Return all role assignments associated with this group.

        Delegates to ``openbis.get_role_assignments`` with ``group=self``.
        Additional keyword arguments are forwarded as search filters.

        Args:
            **search_args: Optional filters accepted by
                ``openbis.get_role_assignments()``, e.g.
                ``space="MY_SPACE"``.

        Returns:
            :class:`~pybis.things.Things`: Container holding the matching
            role assignments.

        Example:
            >>> group.get_roles().df
            >>> group.get_roles(space="TEST_SPACE").df
        """
        return cast(
            "Things", self.openbis.get_role_assignments(group=self, **search_args)
        )

    def assign_role(self, role: AuthorizationRoles, **kwargs: Any) -> None:
        """Assign a role to this group.

        The scope of the role is determined by the keyword arguments:

        - No extra args → ``roleLevel`` is ``"INSTANCE"``.
        - ``space=...`` → ``roleLevel`` is ``"SPACE"``.
        - ``project=...`` → ``roleLevel`` is ``"PROJECT"``.

        If the role is already assigned at the requested scope the call is
        silently ignored — no error is raised.

        Args:
            role: Role name, e.g. ``"ADMIN"``, ``"USER"``, ``"OBSERVER"``.
            **kwargs: Optional scope arguments (``space``, ``project``)
                forwarded to ``openbis.assign_role()``.

        Raises:
            ValueError: If the server returns an error unrelated to a
                duplicate assignment.

        Example:
            >>> group.assign_role("ADMIN")
            >>> group.assign_role("USER", space="MY_SPACE")
            >>> group.assign_role("OBSERVER", project="/MY_SPACE/MY_PROJECT")
        """
        try:
            self.openbis.assign_role(role=role, group=self, **kwargs)
            if VERBOSE:
                print(f"Role {role} successfully assigned to group {self.code}")
        except ValueError as e:
            if "exists" in str(e):
                if VERBOSE:
                    print(f"Role {role} already assigned to group {self.code}")
            else:
                raise ValueError(str(e))

    def revoke_role(
        self,
        role: AuthorizationRoles | int,
        space: Optional[str] = None,
        project: Optional[str] = None,
        reason: str = "no reason specified",
    ) -> None:
        """Revoke a role from this group.

        The role to remove can be identified either by its numeric ``techId``
        (int) or by role name combined with optional ``space``/``project``
        scope filters.  When a name is given, the matching role assignment is
        looked up via :meth:`get_roles` and resolved to a ``techId`` before
        deletion.

        If no matching role is found (already revoked), the call returns
        silently.

        Args:
            role: Either the integer ``techId`` of the role assignment, or a
                role name string (e.g. ``"ADMIN"``).
            space: Restrict the lookup to a specific space code.  Uppercased
                automatically.  ``None`` matches instance-level roles.
            project: Restrict the lookup to a specific project code.
                Uppercased automatically.  ``None`` matches non-project roles.
            reason: Human-readable reason recorded with the deletion.
                Defaults to ``"no reason specified"``.

        Example:
            >>> group.revoke_role("ADMIN")
            >>> group.revoke_role("USER", space="MY_SPACE")
            >>> group.revoke_role(42)  # by techId
        """
        techId = None
        if isinstance(role, int):
            techId = role
        else:
            query: dict[str, str] = {"role": role}
            if space is None:
                query["space"] = ""
            else:
                query["space"] = space.upper()

            if project is None:
                query["project"] = ""
            else:
                query["project"] = project.upper()

            querystr = " & ".join(f'{key} == "{value}"' for key, value in query.items())
            roles = self.get_roles().df
            if roles is None or len(roles) == 0:
                if VERBOSE:
                    print(
                        f"Role {role} has already been revoked from group {self.code}"
                    )
                return
            techId = roles.query(querystr)["techId"].values[0]

        ra = self.openbis.get_role_assignment(techId)
        ra.delete(reason)
        if VERBOSE:
            print(f"Role {role} successfully revoked from group {self.code}")
        return

    def save(self) -> Group:
        """Persist the group to openBIS, either creating or updating it.

        Behaviour depends on whether the object is new or already exists:

        - **New group** (``is_new=True``): calls ``createAuthorizationGroups``
          via the V3 API, then re-fetches the created group so
          server-assigned fields (``permId``, ``registrationDate``, …) are
          populated on ``self``.
        - **Existing group**: calls ``updateAuthorizationGroups`` using the
          current ``permId``, then re-fetches to reflect any server-side
          changes.

        Returns:
            This :class:`Group` instance, updated in-place with the server
            response.  Allows chaining: ``group.save().permId``.

        Raises:
            Exception: Propagated from ``openbis._post_request`` on API or
                network failure.

        Example:
            >>> group = openbis.new_group(code="ANALYSTS")
            >>> group.description = "Analyst team"
            >>> group.save()
            >>> group.assign_role("USER", space="MY_SPACE")
        """
        if self.is_new:
            request = self._new_attrs()
            resp = self.openbis._post_request(self.openbis.as_v3, request)
            if VERBOSE:
                print("Group successfully created.")
            new_data = self.openbis.get_group_or_raise(resp[0]["permId"]).data
            self._set_data(new_data)
            return self

        else:
            request = self._up_attrs()
            self.openbis._post_request(self.openbis.as_v3, request)
            if VERBOSE:
                print("Group successfully updated.")
            new_data = self.openbis.get_group_or_raise(self.permId).data
            self._set_data(new_data)
            return self
