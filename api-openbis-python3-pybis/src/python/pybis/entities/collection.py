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
"""The Collection entity (openBIS experiment) and its client API."""

from __future__ import annotations

from collections.abc import Iterator, Sequence
from typing import TYPE_CHECKING, Any, cast

from ..api.filters import PropertyFilter
from ..api.rpc import parse_jackson, type_for_id
from ..api.search import (
    attribute_date_criterion,
    explicit_id_criterion,
    id_criterion,
    is_finished_criterion,
    modifier_criterion,
    property_criterion,
    registrator_criterion,
    tags_criterion,
    type_criterion,
)
from ..definitions import get_fetchoption_for_entity
from ..exceptions import NotFoundError
from ..experiment import Experiment as Collection
from ..types.results import SearchResult
from ..types.values import PropertyValue
from ..utils import (
    extract_identifier,
    extract_nested_identifier,
    extract_nested_permid,
    extract_permid,
    extract_person,
    format_timestamp,
)
from ._mixin import paginate
from .entity_type import _EntityTypeApi

if TYPE_CHECKING:
    import pandas as pd

_FETCH_EXTRAS = ["tags", "properties", "registrator", "modifier", "project"]


def _collections_df(items: Sequence[Any]) -> "pd.DataFrame":
    """Build the notebook DataFrame view for a page of collections."""
    from pandas import DataFrame

    attrs = [
        "identifier",
        "permId",
        "type",
        "project",
        "registrator",
        "registrationDate",
        "modifier",
        "modificationDate",
    ]
    if not items:
        return DataFrame(columns=attrs)
    df = DataFrame([item.data for item in items])
    for column, mapper in [
        ("identifier", extract_identifier),
        ("permId", extract_permid),
        ("type", extract_nested_permid),
        ("project", extract_nested_identifier),
        ("registrator", extract_person),
        ("modifier", extract_person),
        ("registrationDate", format_timestamp),
        ("modificationDate", format_timestamp),
    ]:
        if column in df:
            df[column] = df[column].map(mapper)
    for i, item in enumerate(items):
        for prop_name, value in (item.data.get("properties") or {}).items():
            column = prop_name.upper()
            df.loc[i, column] = str(value)
            if column not in attrs:
                attrs.append(column)
    return cast("pd.DataFrame", df[df.columns.intersection(attrs)])


class _CollectionApi(_EntityTypeApi):
    """Collection (experiment) methods of the Openbis client."""

    def _collection_from_data(
        self, data: dict[str, Any], type_cache: dict[str, Any]
    ) -> Collection:
        """Construct a Collection with its (cached) full type."""
        type_code = (data.get("type") or {}).get("code")
        type_obj = None
        if type_code is not None:
            if type_code not in type_cache:
                type_cache[type_code] = self.get_collection_type(type_code)
            type_obj = type_cache[type_code]
        return Collection(self, type=type_obj, data=data)

    def get_collection(self, identifier: str) -> Collection | None:
        """Get a single Collection by perm_id or identifier.

        Args:
            identifier: ``"20240101000000000-1"`` or ``"/SPACE/PROJ/COLL"``.

        Returns:
            The Collection, or None if it does not exist.
        """
        fetchopts = get_fetchoption_for_entity("experiment")
        for option in _FETCH_EXTRAS + ["attachments"]:
            fetchopts[option] = get_fetchoption_for_entity(option)
        request = {
            "method": "getExperiments",
            "params": [
                self.token,
                [type_for_id(identifier, "experiment")],
                fetchopts,
            ],
        }
        resp = self._post_request(self.as_v3, request)
        parse_jackson(resp)
        for key in resp:
            return self._collection_from_data(resp[key], {})
        return None

    def get_collection_or_raise(self, identifier: str) -> Collection:
        """Get a single Collection; raise if it does not exist.

        Raises:
            NotFoundError: No collection matches the identifier.
        """
        collection = self.get_collection(identifier)
        if collection is None:
            raise NotFoundError("collection", identifier)
        return collection

    def search_collections(
        self,
        *,
        type: str | None = None,
        space: str | None = None,
        project: str | None = None,
        id: str | None = None,
        perm_id: str | None = None,
        identifier: str | None = None,
        code: str | None = None,
        tags: list[str] | None = None,
        properties: dict[str, str | PropertyFilter] | None = None,
        is_finished: bool | None = None,
        registration_date: str | PropertyFilter | None = None,
        modification_date: str | PropertyFilter | None = None,
        registrator: str | None = None,
        modifier: str | None = None,
        count: int = 25,
        start_with: int = 0,
    ) -> SearchResult[Collection]:
        """Search for Collections matching the given criteria (AND logic).

        Args:
            type: Filter by CollectionType code.
            space: Filter by space code (via the owning project).
            project: Filter by project identifier or code.
            id: Identifying string, auto-classified.
            perm_id: Explicit perm_id filter.
            identifier: Explicit identifier filter.
            code: Explicit code filter.
            tags: Collections must carry all the given tags.
            properties: Property conditions; plain ``str`` means exact match.
            is_finished: Filter on the ELN finished flag.
            registration_date: Registration-date condition.
            modification_date: Modification-date condition.
            registrator: Exact user_id of the registering user.
            modifier: Exact user_id of the last modifying user.
            count: Maximum number of results to return (default: 25).
            start_with: Pagination offset (default: 0).

        Returns:
            A SearchResult of Collections with the server-side total count.
        """
        criteria: dict[str, Any] = {
            "@type": "as.dto.experiment.search.ExperimentSearchCriteria",
            "operator": "AND",
            "criteria": [],
        }
        crit: list[dict[str, Any]] = criteria["criteria"]
        if type is not None:
            crit.append(type_criterion("Experiment", type))
        if space is not None:
            crit.append(
                {
                    "@type": "as.dto.project.search.ProjectSearchCriteria",
                    "operator": "AND",
                    "criteria": [
                        {
                            "@type": "as.dto.space.search.SpaceSearchCriteria",
                            "operator": "AND",
                            "criteria": [id_criterion(space)],
                        }
                    ],
                }
            )
        if project is not None:
            crit.append(
                {
                    "@type": "as.dto.project.search.ProjectSearchCriteria",
                    "operator": "AND",
                    "criteria": [id_criterion(project)],
                }
            )
        if id is not None:
            crit.append(id_criterion(id))
        if perm_id is not None:
            crit.append(explicit_id_criterion("perm_id", perm_id))
        if identifier is not None:
            crit.append(explicit_id_criterion("identifier", identifier))
        if code is not None:
            crit.append(explicit_id_criterion("code", code))
        if tags:
            crit.append(tags_criterion(tags))
        for prop_code, condition in (properties or {}).items():
            crit.append(property_criterion(prop_code, condition))
        if is_finished is not None:
            crit.append(is_finished_criterion(is_finished))
        if registration_date is not None:
            crit.append(
                attribute_date_criterion("registration_date", registration_date)
            )
        if modification_date is not None:
            crit.append(
                attribute_date_criterion("modification_date", modification_date)
            )
        if registrator is not None:
            crit.append(registrator_criterion(registrator))
        if modifier is not None:
            crit.append(modifier_criterion(modifier))

        fetchopts = get_fetchoption_for_entity("experiment")
        fetchopts["from"] = start_with
        fetchopts["count"] = count
        for option in _FETCH_EXTRAS:
            fetchopts[option] = get_fetchoption_for_entity(option)

        request = {
            "method": "searchExperiments",
            "params": [self.token, criteria, fetchopts],
        }
        resp = self._post_request(self.as_v3, request)
        parse_jackson(resp)
        type_cache: dict[str, Any] = {}
        items = [
            self._collection_from_data(data, type_cache) for data in resp["objects"]
        ]
        return SearchResult(
            items, int(resp.get("totalCount", len(items))), _collections_df
        )

    def iter_collections(
        self, *, page_size: int = 100, **filters: Any
    ) -> Iterator[Collection]:
        """Iterate over all matching Collections, paginating automatically.

        Args:
            page_size: Entities fetched per request.
            **filters: Same filters as :meth:`search_collections` (except
                pagination).

        Yields:
            Every matching Collection.
        """
        return paginate(
            lambda *, count, start_with: self.search_collections(
                count=count, start_with=start_with, **filters
            ),
            page_size=page_size,
        )

    def new_collection(
        self,
        type: str | Any,
        *,
        project: str | Any,
        code: str | None = None,
        tags: list[str] | None = None,
        properties: dict[str, PropertyValue] | None = None,
    ) -> Collection:
        """Construct an unsaved Collection; call ``.save()`` to persist it.

        Args:
            type: CollectionType code or CollectionType instance.
            project: Project identifier or Project the collection belongs to.
            code: Collection code.
            tags: Tags to assign.
            properties: Initial property values.

        Returns:
            The unsaved Collection.

        Raises:
            NotFoundError: The collection type does not exist.
        """
        collection_type = (
            self.get_collection_type_or_raise(type) if isinstance(type, str) else type
        )
        kwargs: dict[str, Any] = {"project": project}
        if code is not None:
            kwargs["code"] = code
        if tags is not None:
            kwargs["tags"] = tags
        return Collection(self, type=collection_type, props=properties, **kwargs)


__all__ = ["Collection", "_CollectionApi"]
