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
"""The DataSet entity and its client API (get/search/iter/new + types)."""

from __future__ import annotations

from collections.abc import Iterator, Sequence
from pathlib import Path
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
    status_criterion,
    tags_criterion,
    type_criterion,
)
from ..dataset import DataSet
from ..definitions import get_fetchoption_for_entity, get_fetchoptions
from ..entity_type import DataSetType
from ..exceptions import NotFoundError, ValidationError
from ..types.results import SearchResult
from ..types.values import PropertyValue
from ..utils import (
    extract_nested_identifier,
    extract_nested_permid,
    extract_permid,
    format_timestamp,
)
from ._mixin import paginate
from .entity_type import _EntityTypeApi

if TYPE_CHECKING:
    import pandas as pd

_FETCH_EXTRAS = [
    "tags",
    "properties",
    "dataStore",
    "physicalData",
    "linkedData",
    "experiment",
    "sample",
    "registrator",
    "modifier",
]


def _datasets_df(items: Sequence[Any]) -> "pd.DataFrame":
    """Build the notebook DataFrame view for a page of datasets."""
    from pandas import DataFrame

    attrs = [
        "permId",
        "type",
        "experiment",
        "sample",
        "registrationDate",
        "modificationDate",
        "location",
        "status",
        "presentInArchive",
        "size",
        "parents",
        "children",
    ]
    if not items:
        return DataFrame(columns=attrs)
    rows = []
    for item in items:
        data = dict(item.data)
        physical = data.get("physicalData") or {}
        data.setdefault("location", physical.get("location", ""))
        data.setdefault("status", physical.get("status", ""))
        data.setdefault("presentInArchive", physical.get("presentInArchive", ""))
        data.setdefault("size", physical.get("size", ""))
        rows.append(data)
    df = DataFrame(rows)
    for column, mapper in [
        ("permId", extract_permid),
        ("type", extract_nested_permid),
        ("experiment", extract_nested_identifier),
        ("sample", extract_nested_identifier),
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


def _related_criterion(relation_type: str, things: str | list[str]) -> dict[str, Any]:
    """Wrap id criteria of related datasets (parents/children filters)."""
    if not isinstance(things, list):
        things = [things]
    return {
        "@type": relation_type,
        "operator": "OR",
        "criteria": [id_criterion(thing) for thing in things],
    }


class _DataSetApi(_EntityTypeApi):
    """DataSet methods of the Openbis client."""

    def _dataset_from_data(
        self, data: dict[str, Any], type_cache: dict[str, Any]
    ) -> DataSet:
        """Construct a DataSet with its (cached) full type."""
        type_code = (data.get("type") or {}).get("code")
        type_obj = None
        if type_code is not None:
            if type_code not in type_cache:
                type_cache[type_code] = self.get_dataset_type(type_code)
            type_obj = type_cache[type_code]
        return DataSet(self, type=type_obj, data=data)

    def get_dataset(self, perm_id: str) -> DataSet | None:
        """Get a single DataSet by perm_id.

        Args:
            perm_id: The dataset perm_id, e.g. ``"20240101000000000-1"``.

        Returns:
            The DataSet, or None if it does not exist.
        """
        fetchopts = get_fetchoption_for_entity("dataSet")
        for option in _FETCH_EXTRAS:
            fetchopts[option] = get_fetchoption_for_entity(option)
        request = {
            "method": "getDataSets",
            "params": [
                self.token,
                [type_for_id(perm_id, "dataset")],
                fetchopts,
            ],
        }
        resp = self._post_request(self.as_v3, request)
        parse_jackson(resp)
        for key in resp:
            return self._dataset_from_data(resp[key], {})
        return None

    def get_dataset_or_raise(self, perm_id: str) -> DataSet:
        """Get a single DataSet; raise if it does not exist.

        Raises:
            NotFoundError: No dataset matches the perm_id.
        """
        dataset = self.get_dataset(perm_id)
        if dataset is None:
            raise NotFoundError("dataset", perm_id)
        return dataset

    def search_datasets(
        self,
        *,
        type: str | None = None,
        kind: str | None = None,
        status: str | None = None,
        object: str | None = None,
        collection: str | None = None,
        project: str | None = None,
        space: str | None = None,
        id: str | None = None,
        perm_id: str | None = None,
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
    ) -> SearchResult[DataSet]:
        """Search for DataSets matching the given criteria (AND logic).

        Args:
            type: Filter by DataSetType code.
            kind: Not searchable in the openBIS v3 API; raises.
            status: Archiving status (:data:`~pybis.types.DataSetStatus`).
            object: Owning Object perm_id or identifier (1.x: ``sample=``).
            collection: Owning Collection perm_id or identifier
                (1.x: ``experiment=``).
            project: Filter by project (via the owning collection).
            space: Filter by space (via the owning collection's project).
            id: Identifying string, auto-classified.
            perm_id: Explicit perm_id filter.
            code: Explicit code filter (synonym of perm_id for datasets).
            tags: Datasets must carry all the given tags.
            properties: Property conditions; plain ``str`` means exact match.
            hierarchy_properties: Conditions on properties of parent/child/
                container datasets.
            parents: Only datasets having (one of) the given parents.
            children: Only datasets having (one of) the given children.
            with_parents: Eagerly include parent relationships in results.
            with_children: Eagerly include child relationships in results.
            registration_date: Registration-date condition.
            modification_date: Modification-date condition.
            registrator: Exact user_id of the registering user.
            modifier: Exact user_id of the last modifying user.
            count: Maximum number of results to return (default: 25).
            start_with: Pagination offset (default: 0).

        Returns:
            A SearchResult of DataSets with the server-side total count.

        Raises:
            ValidationError: ``kind`` filtering was requested (the openBIS
                v3 API cannot search by dataset kind) or ``status`` is not
                a valid archiving status.
        """
        if kind is not None:
            raise ValidationError(
                "the openBIS v3 API cannot search by dataset kind; filter"
                " the result instead"
            )
        criteria: dict[str, Any] = {
            "@type": "as.dto.dataset.search.DataSetSearchCriteria",
            "operator": "AND",
            "criteria": [],
        }
        crit: list[dict[str, Any]] = criteria["criteria"]
        if type is not None:
            crit.append(type_criterion("DataSet", type))
        if status is not None:
            crit.append(status_criterion(status))
        if object is not None:
            crit.append(
                {
                    "@type": "as.dto.sample.search.SampleSearchCriteria",
                    "operator": "AND",
                    "criteria": [id_criterion(object)],
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
        if project is not None:
            crit.append(
                {
                    "@type": "as.dto.experiment.search.ExperimentSearchCriteria",
                    "operator": "AND",
                    "criteria": [
                        {
                            "@type": "as.dto.project.search.ProjectSearchCriteria",
                            "operator": "AND",
                            "criteria": [id_criterion(project)],
                        }
                    ],
                }
            )
        if space is not None:
            crit.append(
                {
                    "@type": "as.dto.experiment.search.ExperimentSearchCriteria",
                    "operator": "AND",
                    "criteria": [
                        {
                            "@type": "as.dto.project.search.ProjectSearchCriteria",
                            "operator": "AND",
                            "criteria": [
                                {
                                    "@type": (
                                        "as.dto.space.search.SpaceSearchCriteria"
                                    ),
                                    "operator": "AND",
                                    "criteria": [id_criterion(space)],
                                }
                            ],
                        }
                    ],
                }
            )
        if id is not None:
            crit.append(id_criterion(id))
        if perm_id is not None:
            crit.append(explicit_id_criterion("perm_id", perm_id))
        if code is not None:
            crit.append(explicit_id_criterion("code", code))
        if tags:
            crit.append(tags_criterion(tags))
        if parents is not None:
            crit.append(
                _related_criterion(
                    "as.dto.dataset.search.DataSetParentsSearchCriteria", parents
                )
            )
        if children is not None:
            crit.append(
                _related_criterion(
                    "as.dto.dataset.search.DataSetChildrenSearchCriteria", children
                )
            )
        for prop_code, condition in (properties or {}).items():
            crit.append(property_criterion(prop_code, condition))
        for hierarchy in hierarchy_properties or []:
            crit.append(hierarchy_property_criterion("dataset", hierarchy))
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

        fetchopts = get_fetchoptions("dataSet", including=["type"])
        fetchopts["from"] = start_with
        fetchopts["count"] = count
        for option in _FETCH_EXTRAS:
            fetchopts[option] = get_fetchoption_for_entity(option)
        fetchopts["experiment"]["project"] = get_fetchoption_for_entity("project")
        if with_parents:
            fetchopts["parents"] = get_fetchoption_for_entity("dataSet")
        if with_children:
            fetchopts["children"] = get_fetchoption_for_entity("dataSet")

        request = {
            "method": "searchDataSets",
            "params": [self.token, criteria, fetchopts],
        }
        resp = self._post_request(self.as_v3, request)
        parse_jackson(resp)
        type_cache: dict[str, Any] = {}
        items = [
            self._dataset_from_data(data, type_cache) for data in resp["objects"]
        ]
        return SearchResult(
            items, int(resp.get("totalCount", len(items))), _datasets_df
        )

    def iter_datasets(
        self, *, page_size: int = 100, **filters: Any
    ) -> Iterator[DataSet]:
        """Iterate over all matching DataSets, paginating automatically.

        Args:
            page_size: Entities fetched per request.
            **filters: Same filters as :meth:`search_datasets` (except
                pagination).

        Yields:
            Every matching DataSet.
        """
        return paginate(
            lambda *, count, start_with: self.search_datasets(
                count=count, start_with=start_with, **filters
            ),
            page_size=page_size,
        )

    def new_dataset(
        self,
        type: str | Any,
        *,
        kind: str = "PHYSICAL",
        object: str | Any | None = None,
        collection: str | Any | None = None,
        files: list[str | Path] | None = None,
        folder: str | Path | None = None,
        zip_file: str | Path | None = None,
        parents: list[Any] | None = None,
        properties: dict[str, PropertyValue] | None = None,
        code: str | None = None,
    ) -> DataSet:
        """Construct an unsaved DataSet; call ``.save()`` to upload/persist.

        Args:
            type: DataSetType code or DataSetType instance.
            kind: A :data:`~pybis.types.DataSetKind` value
                (default: ``"PHYSICAL"``). CONTAINER datasets carry no files.
            object: Owning object identifier or Object (1.x: ``sample=``).
            collection: Owning collection identifier or Collection
                (1.x: ``experiment=``).
            files: Files (or directories) to upload; structure is kept.
            folder: Folder to upload recursively.
            zip_file: A single zip whose contents are extracted in openBIS.
            parents: Parent datasets or their perm_ids.
            properties: Initial property values.
            code: Explicit dataset code (rarely needed).

        Returns:
            The unsaved DataSet.

        Raises:
            NotFoundError: The dataset type does not exist.
        """
        if isinstance(type, str):
            type_obj = self.get_dataset_type_or_raise(type.upper())
        else:
            type_obj = type
        kwargs: dict[str, Any] = {}
        if object is not None:
            kwargs["sample"] = object
        if collection is not None:
            kwargs["experiment"] = collection
        if zip_file is not None:
            kwargs["zipfile"] = zip_file
        if parents is not None:
            kwargs["parents"] = parents
        if code is not None:
            kwargs["code"] = code
        return DataSet(
            self,
            type=type_obj,
            kind=kind,
            files=files,
            folder=folder,
            props=properties,
            **kwargs,
        )

    # --- dataset types ----------------------------------------------------------

    def get_dataset_type(self, code: str) -> DataSetType | None:
        """Get a single DataSetType by code, or None if it does not exist."""
        return cast(
            "DataSetType | None",
            self._get_entity_type_v2("dataSetType", DataSetType, code),
        )

    def get_dataset_type_or_raise(self, code: str) -> DataSetType:
        """Get a single DataSetType by code; raise if it does not exist.

        Raises:
            NotFoundError: No dataset type exists with this code.
        """
        dataset_type = self.get_dataset_type(code)
        if dataset_type is None:
            raise NotFoundError("dataset type", code)
        return dataset_type

    def search_dataset_types(
        self,
        *,
        code: str | None = None,
        count: int = 25,
        start_with: int = 0,
    ) -> SearchResult[DataSetType]:
        """Search for DataSetTypes.

        Args:
            code: Filter by type code (exact match).
            count: Maximum number of results (default: 25).
            start_with: Pagination offset (default: 0).
        """
        return self._search_entity_types_v2(
            "dataSetType", DataSetType, code=code, count=count, start_with=start_with
        )

    def new_dataset_type(
        self,
        code: str,
        *,
        description: str | None = None,
        main_dataset_pattern: str | None = None,
        main_dataset_path: str | None = None,
        disallow_deletion: bool = False,
        validation_plugin: str | None = None,
    ) -> DataSetType:
        """Construct an unsaved DataSetType; call ``.save()`` to persist it.

        Args:
            code: Code of the new type.
            description: Free-text description.
            main_dataset_pattern: Regex marking the main dataset file.
            main_dataset_path: Path of the main dataset folder.
            disallow_deletion: Forbid deleting datasets of this type.
            validation_plugin: Name of the validation plugin.
        """
        return DataSetType(
            self,
            code=code,
            description=description,
            mainDataSetPattern=main_dataset_pattern,
            mainDataSetPath=main_dataset_path,
            disallowDeletion=disallow_deletion,
            validationPlugin=validation_plugin,
        )


__all__ = ["DataSet", "DataSetType", "_DataSetApi"]
