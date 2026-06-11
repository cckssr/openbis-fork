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
"""The Project entity and its client API (get/search/iter/new)."""

from __future__ import annotations

from collections.abc import Iterator, Sequence
from typing import TYPE_CHECKING, Any, cast

from ..api.identifiers import classify
from ..api.rpc import parse_jackson
from ..api.search import explicit_id_criterion, id_criterion
from ..definitions import get_fetchoption_for_entity
from ..exceptions import NotFoundError
from ..project import Project
from ..types.results import SearchResult
from ..utils import (
    extract_identifier,
    extract_permid,
    extract_person,
    format_timestamp,
)
from ._mixin import ClientApiMixin, paginate

if TYPE_CHECKING:
    import pandas as pd

_GET_OPTIONS = ["space", "registrator", "modifier", "attachments"]


def _projects_df(projects: Sequence[Any]) -> "pd.DataFrame":
    """Build the notebook DataFrame view for a page of projects."""
    from pandas import DataFrame

    attrs = [
        "code",
        "identifier",
        "permId",
        "description",
        "leader",
        "registrator",
        "registrationDate",
        "modifier",
        "modificationDate",
        "frozen",
        "frozenForExperiments",
        "frozenForSamples",
    ]
    if not projects:
        return DataFrame(columns=attrs)
    df = DataFrame([project.data for project in projects])
    for column, mapper in [
        ("registrationDate", format_timestamp),
        ("modificationDate", format_timestamp),
        ("leader", extract_person),
        ("registrator", extract_person),
        ("modifier", extract_person),
        ("permId", extract_permid),
        ("identifier", extract_identifier),
    ]:
        if column in df:
            df[column] = df[column].map(mapper)
    return cast("pd.DataFrame", df[df.columns.intersection(attrs)])


class _ProjectApi(ClientApiMixin):
    """Project methods of the Openbis client."""

    def get_project(self, identifier: str) -> Project | None:
        """Get a single Project by identifier, perm_id, or code.

        Args:
            identifier: ``"/SPACE/PROJECT"``, a perm_id, or a bare project
                code (auto-classified).

        Returns:
            The Project, or None if it does not exist (or a bare code
            matches more than one project).
        """
        cached = self._object_cache(entity="project", code=identifier)
        if cached is not None:
            return cached  # type: ignore[no-any-return]  # reason: heterogeneous legacy cache

        cid = classify(identifier)
        fetchopts: dict[str, Any] = {
            "@type": "as.dto.project.fetchoptions.ProjectFetchOptions"
        }
        for option in _GET_OPTIONS:
            fetchopts[option] = get_fetchoption_for_entity(option)

        if cid.kind in ("identifier", "perm_id"):
            id_payload = (
                {
                    "identifier": cid.value.upper(),
                    "@type": "as.dto.project.id.ProjectIdentifier",
                }
                if cid.kind == "identifier"
                else {
                    "permId": cid.value,
                    "@type": "as.dto.project.id.ProjectPermId",
                }
            )
            request = {
                "method": "getProjects",
                "params": [self.token, [id_payload], fetchopts],
            }
            resp = self._post_request(self.as_v3, request)
            for key in resp:
                project = Project(self, type=None, data=resp[key])
                self._object_cache(entity="project", code=identifier, value=project)
                return project
            return None

        criteria: dict[str, Any] = {
            "@type": "as.dto.project.search.ProjectSearchCriteria",
            "operator": "AND",
            "criteria": [explicit_id_criterion("code", cid.value)],
        }
        request = {
            "method": "searchProjects",
            "params": [self.token, criteria, fetchopts],
        }
        resp = self._post_request(self.as_v3, request)
        objects = resp["objects"]
        if len(objects) != 1:
            # missing — or ambiguous bare code; the caller must use the
            # full identifier to disambiguate
            return None
        parse_jackson(objects)
        project = Project(self, type=None, data=objects[0])
        self._object_cache(entity="project", code=identifier, value=project)
        return project

    def get_project_or_raise(self, identifier: str) -> Project:
        """Get a single Project; raise if it does not exist.

        Args:
            identifier: ``"/SPACE/PROJECT"``, a perm_id, or a bare code.

        Returns:
            The Project.

        Raises:
            NotFoundError: No (single) project matches.
        """
        project = self.get_project(identifier)
        if project is None:
            raise NotFoundError("project", identifier)
        return project

    def search_projects(
        self,
        *,
        id: str | None = None,
        code: str | None = None,
        space: str | None = None,
        count: int = 25,
        start_with: int = 0,
    ) -> SearchResult[Project]:
        """Search for Projects matching the given criteria.

        Args:
            id: Identifying string, auto-classified as perm_id, identifier,
                or code.
            code: Filter by project code (exact match).
            space: Filter by space code.
            count: Maximum number of results to return (default: 25).
            start_with: Pagination offset (default: 0).

        Returns:
            A SearchResult of Projects with the server-side total count.
        """
        criteria: dict[str, Any] = {
            "@type": "as.dto.project.search.ProjectSearchCriteria",
            "operator": "AND",
            "criteria": [],
        }
        if id is not None:
            criteria["criteria"].append(id_criterion(id))
        if code is not None:
            criteria["criteria"].append(explicit_id_criterion("code", code))
        if space is not None:
            criteria["criteria"].append(
                {
                    "@type": "as.dto.space.search.SpaceSearchCriteria",
                    "operator": "AND",
                    "criteria": [explicit_id_criterion("code", space)],
                }
            )

        fetchopts: dict[str, Any] = {
            "@type": "as.dto.project.fetchoptions.ProjectFetchOptions",
            "from": start_with,
            "count": count,
        }
        for option in ["registrator", "modifier", "leader"]:
            fetchopts[option] = get_fetchoption_for_entity(option)

        request = {
            "method": "searchProjects",
            "params": [self.token, criteria, fetchopts],
        }
        resp = self._post_request(self.as_v3, request)
        parse_jackson(resp)
        projects = [Project(self, type=None, data=obj) for obj in resp["objects"]]
        return SearchResult(
            projects, int(resp.get("totalCount", len(projects))), _projects_df
        )

    def iter_projects(
        self,
        *,
        id: str | None = None,
        code: str | None = None,
        space: str | None = None,
        page_size: int = 100,
    ) -> Iterator[Project]:
        """Iterate over all matching Projects, paginating automatically.

        Args:
            id: Identifying string, auto-classified.
            code: Filter by project code.
            space: Filter by space code.
            page_size: Entities fetched per request.

        Yields:
            Every matching Project.
        """
        return paginate(
            lambda *, count, start_with: self.search_projects(
                id=id, code=code, space=space, count=count, start_with=start_with
            ),
            page_size=page_size,
        )

    def new_project(
        self,
        space: Any,
        code: str,
        *,
        description: str | None = None,
        attachments: str | list[str] | None = None,
    ) -> Project:
        """Construct an unsaved Project; call ``.save()`` to persist it.

        Args:
            space: Space code or Space object the project belongs to.
            code: Code of the new project.
            description: Optional free-text description.
            attachments: Optional file path(s) to attach.

        Returns:
            The unsaved Project.
        """
        if attachments is not None:
            return Project(
                self,
                None,
                space=space,
                code=code,
                description=description,
                attachments=attachments,
            )
        return Project(self, None, space=space, code=code, description=description)


__all__ = ["Project", "_ProjectApi"]
