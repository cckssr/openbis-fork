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
"""Administration entities: persons (users), groups, and role assignments."""

from __future__ import annotations

from collections.abc import Sequence
from typing import TYPE_CHECKING, Any, cast

from ..api.rpc import parse_jackson
from ..api.search import explicit_id_criterion
from ..definitions import get_fetchoption_for_entity, get_type_for_entity
from ..exceptions import NotFoundError
from ..group import Group
from ..person import Person
from ..role_assignment import RoleAssignment
from ..types.results import SearchResult
from ..utils import (
    extract_code,
    extract_id,
    extract_nested_identifier,
    extract_nested_permid,
    extract_permid,
    extract_userId,
    format_timestamp,
)
from ._mixin import ClientApiMixin

if TYPE_CHECKING:
    import pandas as pd


def _user_id_criterion(user_id: str) -> dict[str, Any]:
    return {
        "@type": "as.dto.person.search.PersonSearchCriteria",
        "operator": "AND",
        "criteria": [
            {
                "@type": "as.dto.person.search.UserIdSearchCriteria",
                "fieldName": "userId",
                "fieldType": "ATTRIBUTE",
                "fieldValue": {
                    "value": user_id,
                    "@type": "as.dto.common.search.StringEqualToValue",
                },
            }
        ],
    }


def _persons_df(items: Sequence[Any]) -> "pd.DataFrame":
    """Build the notebook DataFrame view for a page of persons."""
    from pandas import DataFrame

    attrs = [
        "permId",
        "userId",
        "firstName",
        "lastName",
        "email",
        "space",
        "registrationDate",
        "active",
    ]
    if not items:
        return DataFrame(columns=attrs)
    df = DataFrame([item.data for item in items])
    for column, mapper in [
        ("permId", extract_permid),
        ("registrationDate", format_timestamp),
        ("space", extract_nested_permid),
    ]:
        if column in df:
            df[column] = df[column].map(mapper)
    return cast("pd.DataFrame", df[df.columns.intersection(attrs)])


def _user_ids_of(users: Any) -> list[str]:
    """Map a group's user list to the bare user ids."""
    if not isinstance(users, list):
        return []
    return [user.get("userId", "") for user in users]


def _groups_df(items: Sequence[Any]) -> "pd.DataFrame":
    """Build the notebook DataFrame view for a page of groups."""
    from pandas import DataFrame

    attrs = [
        "permId",
        "code",
        "description",
        "users",
        "registrator",
        "registrationDate",
        "modificationDate",
    ]
    if not items:
        return DataFrame(columns=attrs)
    df = DataFrame([item.data for item in items])
    for column, mapper in [
        ("permId", extract_permid),
        ("registrator", extract_userId),
        ("registrationDate", format_timestamp),
        ("modificationDate", format_timestamp),
        ("users", _user_ids_of),
    ]:
        if column in df:
            df[column] = df[column].map(mapper)
    return cast("pd.DataFrame", df[df.columns.intersection(attrs)])


def _roles_df(items: Sequence[Any]) -> "pd.DataFrame":
    """Build the notebook DataFrame view for a page of role assignments."""
    from pandas import DataFrame

    attrs = ["techId", "role", "roleLevel", "user", "group", "space", "project"]
    if not items:
        return DataFrame(columns=attrs)
    df = DataFrame([item.data for item in items])
    if "id" in df:
        df["techId"] = df["id"].map(extract_id)
    for column, mapper in [
        ("user", extract_userId),
        ("space", extract_code),
        ("project", extract_nested_identifier),
    ]:
        if column in df:
            df[column] = df[column].map(mapper)
    if "authorizationGroup" in df:
        df["group"] = df["authorizationGroup"].map(extract_code)
    return cast("pd.DataFrame", df[df.columns.intersection(attrs)])


class _AdminApi(ClientApiMixin):
    """Person, group, and role-assignment methods of the Openbis client."""

    # --- persons -------------------------------------------------------------

    def get_person(self, user_id: str) -> Person | None:
        """Get a single user by user_id, or None if it does not exist."""
        fetchopts: dict[str, Any] = {
            "@type": "as.dto.person.fetchoptions.PersonFetchOptions"
        }
        for option in ["roleAssignments", "space"]:
            fetchopts[option] = get_fetchoption_for_entity(option)
        request = {
            "method": "getPersons",
            "params": [
                self.token,
                [{"@type": "as.dto.person.id.PersonPermId", "permId": user_id}],
                fetchopts,
            ],
        }
        resp = self._post_request(self.as_v3, request)
        parse_jackson(resp)
        for perm_id in resp:
            return Person(self, data=resp[perm_id])
        return None

    def get_person_or_raise(self, user_id: str) -> Person:
        """Get a single user; raise if it does not exist.

        Raises:
            NotFoundError: No user exists with this user_id.
        """
        person = self.get_person(user_id)
        if person is None:
            raise NotFoundError("person", user_id)
        return person

    def search_persons(
        self,
        *,
        user_id: str | None = None,
        count: int = 25,
        start_with: int = 0,
    ) -> SearchResult[Person]:
        """Search for users.

        Args:
            user_id: Filter by exact user_id.
            count: Maximum number of results to return (default: 25).
            start_with: Pagination offset (default: 0).
        """
        criteria: dict[str, Any] = {
            "@type": "as.dto.person.search.PersonSearchCriteria",
            "operator": "AND",
            "criteria": [],
        }
        if user_id is not None:
            criteria["criteria"].append(
                {
                    "@type": "as.dto.person.search.UserIdSearchCriteria",
                    "fieldName": "userId",
                    "fieldType": "ATTRIBUTE",
                    "fieldValue": {
                        "value": user_id,
                        "@type": "as.dto.common.search.StringEqualToValue",
                    },
                }
            )
        fetchopts = get_fetchoption_for_entity("person")
        fetchopts["from"] = start_with
        fetchopts["count"] = count
        fetchopts["space"] = get_fetchoption_for_entity("space")
        request = {
            "method": "searchPersons",
            "params": [self.token, criteria, fetchopts],
        }
        resp = self._post_request(self.as_v3, request)
        parse_jackson(resp)
        items = [Person(self, data=data) for data in resp["objects"]]
        return SearchResult(items, int(resp.get("totalCount", len(items))), _persons_df)

    def new_person(self, user_id: str, *, space: Any | None = None) -> Person:
        """Construct an unsaved Person; call ``.save()`` to persist it.

        Args:
            user_id: The login of the new user.
            space: Optional home space (code or Space).
        """
        return Person(self, userId=user_id, space=space)

    # --- groups ---------------------------------------------------------------

    def get_group(self, code: str) -> Group | None:
        """Get a single authorization group by code, or None if missing."""
        fetchopts: dict[str, Any] = {
            "@type": (
                "as.dto.authorizationgroup.fetchoptions"
                ".AuthorizationGroupFetchOptions"
            )
        }
        for option in ["roleAssignments", "users", "registrator"]:
            fetchopts[option] = get_fetchoption_for_entity(option)
        fetchopts["users"]["space"] = get_fetchoption_for_entity("space")
        request = {
            "method": "getAuthorizationGroups",
            "params": [
                self.token,
                [
                    {
                        "@type": (
                            "as.dto.authorizationgroup.id.AuthorizationGroupPermId"
                        ),
                        "permId": code,
                    }
                ],
                fetchopts,
            ],
        }
        resp = self._post_request(self.as_v3, request)
        parse_jackson(resp)
        for perm_id in resp:
            return Group(self, data=resp[perm_id])
        return None

    def get_group_or_raise(self, code: str) -> Group:
        """Get a single authorization group; raise if it does not exist.

        Raises:
            NotFoundError: No group exists with this code.
        """
        group = self.get_group(code)
        if group is None:
            raise NotFoundError("group", code)
        return group

    def search_groups(
        self,
        *,
        code: str | None = None,
        user_id: str | None = None,
        count: int = 25,
        start_with: int = 0,
    ) -> SearchResult[Group]:
        """Search for authorization groups.

        Args:
            code: Filter by group code (exact match).
            user_id: Only groups containing this user.
            count: Maximum number of results to return (default: 25).
            start_with: Pagination offset (default: 0).
        """
        criteria = get_type_for_entity("authorizationGroup", "search")
        criteria["operator"] = "AND"
        criteria["criteria"] = []
        if code is not None:
            criteria["criteria"].append(explicit_id_criterion("code", code))
        if user_id is not None:
            criteria["criteria"].append(_user_id_criterion(user_id))

        fetchopts = get_fetchoption_for_entity("authorizationGroup")
        fetchopts["from"] = start_with
        fetchopts["count"] = count
        for option in ["roleAssignments", "registrator", "users"]:
            fetchopts[option] = get_fetchoption_for_entity(option)
        for option in ["space", "project", "user", "authorizationGroup", "registrator"]:
            fetchopts["roleAssignments"][option] = get_fetchoption_for_entity(option)
        request = {
            "method": "searchAuthorizationGroups",
            "params": [self.token, criteria, fetchopts],
        }
        resp = self._post_request(self.as_v3, request)
        parse_jackson(resp)
        items = [Group(self, data=data) for data in resp["objects"]]
        return SearchResult(items, int(resp.get("totalCount", len(items))), _groups_df)

    def new_group(
        self,
        code: str,
        *,
        description: str | None = None,
        user_ids: list[str] | None = None,
    ) -> Group:
        """Construct an unsaved authorization Group; ``.save()`` persists it.

        Args:
            code: Code of the new group.
            description: Free-text description.
            user_ids: Initial members (user_ids).
        """
        kwargs: dict[str, Any] = {}
        if user_ids is not None:
            kwargs["userIds"] = user_ids
        return Group(self, code=code, description=description, **kwargs)

    # --- role assignments -----------------------------------------------------

    def get_role_assignment(self, tech_id: int | str) -> RoleAssignment | None:
        """Get a single role assignment by its technical id, or None."""
        fetchopts = get_fetchoption_for_entity("roleAssignment")
        for option in ["space", "project", "user", "authorizationGroup", "registrator"]:
            fetchopts[option] = get_fetchoption_for_entity(option)
        request = {
            "method": "getRoleAssignments",
            "params": [
                self.token,
                [
                    {
                        "techId": str(tech_id),
                        "@type": "as.dto.roleassignment.id.RoleAssignmentTechId",
                    }
                ],
                fetchopts,
            ],
        }
        resp = self._post_request(self.as_v3, request)
        parse_jackson(resp)
        for perm_id in resp:
            return RoleAssignment(self, data=resp[perm_id])
        return None

    def get_role_assignment_or_raise(self, tech_id: int | str) -> RoleAssignment:
        """Get a single role assignment; raise if it does not exist.

        Raises:
            NotFoundError: No role assignment exists with this tech_id.
        """
        assignment = self.get_role_assignment(tech_id)
        if assignment is None:
            raise NotFoundError("role assignment", str(tech_id))
        return assignment

    def search_role_assignments(
        self,
        *,
        person: str | None = None,
        group: str | None = None,
        space: str | None = None,
        count: int = 25,
        start_with: int = 0,
    ) -> SearchResult[RoleAssignment]:
        """Search for role assignments.

        Args:
            person: Filter by user_id.
            group: Filter by authorization-group code.
            space: Filter by space code.
            count: Maximum number of results to return (default: 25).
            start_with: Pagination offset (default: 0).
        """
        criteria = get_type_for_entity("roleAssignment", "search")
        criteria["criteria"] = []
        if person is not None:
            criteria["criteria"].append(_user_id_criterion(person))
        if group is not None:
            criteria["criteria"].append(
                {
                    "@type": (
                        "as.dto.authorizationgroup.search"
                        ".AuthorizationGroupSearchCriteria"
                    ),
                    "operator": "AND",
                    "criteria": [explicit_id_criterion("perm_id", group)],
                }
            )
        if space is not None:
            criteria["criteria"].append(
                {
                    "@type": "as.dto.space.search.SpaceSearchCriteria",
                    "operator": "AND",
                    "criteria": [explicit_id_criterion("code", space)],
                }
            )

        fetchopts = get_fetchoption_for_entity("roleAssignment")
        fetchopts["from"] = start_with
        fetchopts["count"] = count
        for option in ["space", "project", "user", "authorizationGroup", "registrator"]:
            fetchopts[option] = get_fetchoption_for_entity(option)
        request = {
            "method": "searchRoleAssignments",
            "params": [self.token, criteria, fetchopts],
        }
        resp = self._post_request(self.as_v3, request)
        parse_jackson(resp)
        items = [RoleAssignment(self, data=data) for data in resp["objects"]]
        return SearchResult(items, int(resp.get("totalCount", len(items))), _roles_df)


__all__ = ["Group", "Person", "RoleAssignment", "_AdminApi"]
