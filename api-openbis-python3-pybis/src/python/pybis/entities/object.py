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
"""The Object entity (openBIS sample) and its client API."""

from __future__ import annotations

from collections.abc import Iterator, Sequence
from typing import TYPE_CHECKING, Any, cast

from ..api.filters import HierarchyPropertyFilter, PropertyFilter
from ..api.rpc import parse_jackson, type_for_id
from ..api.search import (
    attribute_date_criterion,
    explicit_id_criterion,
    hierarchy_property_criterion,
    id_criterion,
    modifier_criterion,
    property_criterion,
    registrator_criterion,
    tags_criterion,
    type_criterion,
)
from ..definitions import get_fetchoption_for_entity
from ..exceptions import NotFoundError
from ..sample import Sample as Object
from ..types.results import SearchResult
from ..types.values import PropertyValue
from ..utils import (
    extract_identifier,
    extract_identifiers,
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

_FETCH_EXTRAS = [
    "tags",
    "properties",
    "registrator",
    "modifier",
    "space",
    "experiment",
]

_GET_FETCH_EXTRAS = _FETCH_EXTRAS + [
    "attachments",
    "container",
    "components",
    "parents",
    "children",
    "dataSets",
]


def _objects_df(items: Sequence[Any]) -> "pd.DataFrame":
    """Build the notebook DataFrame view for a page of objects."""
    from pandas import DataFrame

    attrs = [
        "identifier",
        "permId",
        "type",
        "space",
        "experiment",
        "registrator",
        "registrationDate",
        "modifier",
        "modificationDate",
        "parents",
        "children",
    ]
    if not items:
        return DataFrame(columns=attrs)
    df = DataFrame([item.data for item in items])
    for column, mapper in [
        ("identifier", extract_identifier),
        ("permId", extract_permid),
        ("type", extract_nested_permid),
        ("space", lambda v: (v or {}).get("code", "") if isinstance(v, dict) else v),
        ("experiment", extract_nested_identifier),
        ("registrator", extract_person),
        ("modifier", extract_person),
        ("registrationDate", format_timestamp),
        ("modificationDate", format_timestamp),
        ("parents", extract_identifiers),
        ("children", extract_identifiers),
    ]:
        if column in df:
            df[column] = df[column].map(mapper)
    # expand properties into columns (1.x props="*" behavior)
    for i, item in enumerate(items):
        for prop_name, value in (item.data.get("properties") or {}).items():
            column = prop_name.upper()
            df.loc[i, column] = str(value)
            if column not in attrs:
                attrs.append(column)
    return cast("pd.DataFrame", df[df.columns.intersection(attrs)])


def _related_criterion(relation_type: str, things: str | list[str]) -> dict[str, Any]:
    """Wrap id criteria of related entities (parents/children filters)."""
    if not isinstance(things, list):
        things = [things]
    return {
        "@type": relation_type,
        "operator": "OR",
        "criteria": [id_criterion(thing) for thing in things],
    }


class _ObjectApi(_EntityTypeApi):
    """Object (sample) methods of the Openbis client."""

    def _object_from_data(
        self, data: dict[str, Any], type_cache: dict[str, Any]
    ) -> Object:
        """Construct an Object with its (cached) full type."""
        type_code = (data.get("type") or {}).get("code")
        type_obj = None
        if type_code is not None:
            if type_code not in type_cache:
                type_cache[type_code] = self.get_object_type(type_code)
            type_obj = type_cache[type_code]
        return Object(self, type=type_obj, data=data)

    def get_object(self, identifier: str) -> Object | None:
        """Get a single Object by perm_id, identifier, or code.

        Args:
            identifier: ``"20240101000000000-1"``, ``"/SPACE/PROJ/CODE"``, or
                a bare code (auto-classified). ELN-LIMS 4-part identifiers
                are normalized.

        Returns:
            The Object, or None if it does not exist.
        """
        fetchopts = get_fetchoption_for_entity("sample")
        for option in _GET_FETCH_EXTRAS:
            fetchopts[option] = get_fetchoption_for_entity(option)
        request = {
            "method": "getSamples",
            "params": [
                self.token,
                [type_for_id(identifier, "sample")],
                fetchopts,
            ],
        }
        resp = self._post_request(self.as_v3, request)
        parse_jackson(resp)
        for key in resp:
            return self._object_from_data(resp[key], {})
        return None

    def get_object_or_raise(self, identifier: str) -> Object:
        """Get a single Object; raise if it does not exist.

        Raises:
            NotFoundError: No object matches the identifier.
        """
        obj = self.get_object(identifier)
        if obj is None:
            raise NotFoundError("object", identifier)
        return obj

    def search_objects(
        self,
        *,
        type: str | None = None,
        space: str | None = None,
        project: str | None = None,
        collection: str | None = None,
        id: str | None = None,
        perm_id: str | None = None,
        identifier: str | None = None,
        code: str | None = None,
        tags: list[str] | None = None,
        properties: dict[str, str | PropertyFilter] | None = None,
        hierarchy_properties: list[HierarchyPropertyFilter] | None = None,
        parents: str | list[str] | None = None,
        children: str | list[str] | None = None,
        with_parents: bool = False,
        with_children: bool = False,
        registration_date: str | PropertyFilter | None = None,
        modification_date: str | PropertyFilter | None = None,
        registrator: str | None = None,
        modifier: str | None = None,
        count: int = 25,
        start_with: int = 0,
    ) -> SearchResult[Object]:
        """Search for Objects matching the given criteria (AND logic).

        Args:
            type: Filter by ObjectType code, e.g. ``"MOLECULE"``.
            space: Filter by space code.
            project: Filter by project identifier or code.
            collection: Filter by collection perm_id or identifier.
            id: Identifying string, auto-classified as perm_id, identifier,
                or code.
            perm_id: Explicit perm_id filter.
            identifier: Explicit identifier filter.
            code: Explicit code filter.
            tags: Objects must carry all the given tags.
            properties: Property conditions; plain ``str`` means exact match,
                use :mod:`pybis.api.filters` for operators.
                Example: ``{"FORMULA": "H2O", "ATOMS": filters.gte(3)}``.
            hierarchy_properties: Conditions on properties of parents,
                children, or containers (:func:`~pybis.api.filters.parent_prop`
                and friends).
            parents: Only objects having (one of) the given parents.
            children: Only objects having (one of) the given children.
            with_parents: Eagerly include parent relationships in results.
            with_children: Eagerly include child relationships in results.
            registration_date: Registration-date condition (ISO string or
                date filter).
            modification_date: Modification-date condition.
            registrator: Exact user_id of the registering user.
            modifier: Exact user_id of the last modifying user.
            count: Maximum number of results to return (default: 25).
            start_with: Pagination offset (default: 0).

        Returns:
            A SearchResult of Objects; ``total_count`` may exceed
            ``len(result)`` when paginated.
        """
        criteria: dict[str, Any] = {
            "@type": "as.dto.sample.search.SampleSearchCriteria",
            "operator": "AND",
            "criteria": [],
        }
        crit: list[dict[str, Any]] = criteria["criteria"]
        if type is not None:
            crit.append(type_criterion("Sample", type))
        if space is not None:
            crit.append(
                {
                    "@type": "as.dto.space.search.SpaceSearchCriteria",
                    "operator": "AND",
                    "criteria": [id_criterion(space)],
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
        if collection is not None:
            crit.append(
                {
                    "@type": "as.dto.experiment.search.ExperimentSearchCriteria",
                    "operator": "AND",
                    "criteria": [id_criterion(collection)],
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
        if parents is not None:
            crit.append(
                _related_criterion(
                    "as.dto.sample.search.SampleParentsSearchCriteria", parents
                )
            )
        if children is not None:
            crit.append(
                _related_criterion(
                    "as.dto.sample.search.SampleChildrenSearchCriteria", children
                )
            )
        for prop_code, condition in (properties or {}).items():
            crit.append(property_criterion(prop_code, condition))
        for hierarchy in hierarchy_properties or []:
            crit.append(hierarchy_property_criterion("sample", hierarchy))
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

        fetchopts = get_fetchoption_for_entity("sample")
        fetchopts["from"] = start_with
        fetchopts["count"] = count
        for option in _FETCH_EXTRAS:
            fetchopts[option] = get_fetchoption_for_entity(option)
        if with_parents:
            fetchopts["parents"] = {
                "@type": "as.dto.sample.fetchoptions.SampleFetchOptions"
            }
        if with_children:
            fetchopts["children"] = {
                "@type": "as.dto.sample.fetchoptions.SampleFetchOptions"
            }

        request = {
            "method": "searchSamples",
            "params": [self.token, criteria, fetchopts],
        }
        resp = self._post_request(self.as_v3, request)
        parse_jackson(resp)
        type_cache: dict[str, Any] = {}
        items = [self._object_from_data(data, type_cache) for data in resp["objects"]]
        return SearchResult(items, int(resp.get("totalCount", len(items))), _objects_df)

    def iter_objects(self, *, page_size: int = 100, **filters: Any) -> Iterator[Object]:
        """Iterate over all matching Objects, paginating automatically.

        Args:
            page_size: Entities fetched per request.
            **filters: Same filters as :meth:`search_objects` (except
                pagination).

        Yields:
            Every matching Object.
        """
        return paginate(
            lambda *, count, start_with: self.search_objects(
                count=count, start_with=start_with, **filters
            ),
            page_size=page_size,
        )

    def new_object(
        self,
        type: str | Any,
        *,
        space: str | Any | None = None,
        project: str | Any | None = None,
        collection: str | Any | None = None,
        code: str | None = None,
        parents: list[Any] | None = None,
        children: list[Any] | None = None,
        tags: list[str] | None = None,
        properties: dict[str, PropertyValue] | None = None,
    ) -> Object:
        """Construct an unsaved Object; call ``.save()`` to persist it.

        Args:
            type: ObjectType code or ObjectType instance.
            space: Space code or Space the object lives in.
            project: Project (requires project-level objects on the server).
            collection: Owning collection identifier or Collection.
            code: Object code; omit for auto-generated codes.
            parents: Parent objects or their identifiers.
            children: Child objects or their identifiers.
            tags: Tags to assign.
            properties: Initial property values.

        Returns:
            The unsaved Object.

        Raises:
            NotFoundError: The object type does not exist.
        """
        object_type = (
            self.get_object_type_or_raise(type) if isinstance(type, str) else type
        )
        kwargs: dict[str, Any] = {}
        if space is not None:
            kwargs["space"] = space
        if collection is not None:
            kwargs["collection"] = collection
        if code is not None:
            kwargs["code"] = code
        if parents is not None:
            kwargs["parents"] = parents
        if children is not None:
            kwargs["children"] = children
        if tags is not None:
            kwargs["tags"] = tags
        return Object(
            self, type=object_type, project=project, props=properties, **kwargs
        )


__all__ = ["Object", "_ObjectApi"]
