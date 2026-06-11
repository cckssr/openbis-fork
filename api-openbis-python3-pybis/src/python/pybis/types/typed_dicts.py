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
"""``TypedDict`` definitions for search parameters, fetch options, and creation.

These serve two purposes: they are the ``Unpack`` targets for the keyword
arguments of ``search_*`` / ``iter_*`` / ``new_*`` methods, and they can be
built as standalone dicts for programmatic query construction.

Example:
    >>> from pybis.types import ObjectSearchParams

    >>> params: ObjectSearchParams = {"type": "MOLECULE", "space": "MY_SPACE"}

    >>> client.search_objects(**params)
"""

from __future__ import annotations

from pathlib import Path
from typing import TypedDict

from ..api.filters import HierarchyPropertyFilter, PropertyFilter
from .values import PropertyValue

# ---------------------------------------------------------------------------
# Search parameters
# ---------------------------------------------------------------------------


class BasePaginatedParams(TypedDict, total=False):
    """Pagination knobs shared by every search method.

    Attributes:
        count (int): Maximum number of results to return (default: 25).
        start_with (int): Pagination offset (default: 0).
    """

    count: int
    start_with: int


class BaseEntitySearchParams(BasePaginatedParams, total=False):
    """Filters shared by entities that carry properties, tags, and history.

    Attributes:
        id (str): Identifying string, auto-classified as perm_id, identifier,
            or code (see ``pybis.api.identifiers.classify_id``).
        perm_id (str): Explicit perm_id (overrides ``id`` auto-detection).
        identifier (str): Explicit path-style identifier.
        code (str): Explicit bare code.
        tags (list[str]): Entity must carry all the given tags.
        properties (dict[str, str | PropertyFilter]): Property conditions,
            combined with AND logic; a plain ``str`` means exact match.
        hierarchy_properties (list[HierarchyPropertyFilter]): Conditions on
            properties of parent/child/container entities.
        registration_date (str | PropertyFilter): Registration-date condition;
            a plain ``str`` means exact date.
        modification_date (str | PropertyFilter): Modification-date condition.
        registrator (str): Exact user_id of the registering user.
        modifier (str): Exact user_id of the last modifying user.
    """

    id: str
    perm_id: str
    identifier: str
    code: str
    tags: list[str]
    properties: dict[str, str | PropertyFilter]
    hierarchy_properties: list[HierarchyPropertyFilter]
    registration_date: str | PropertyFilter
    modification_date: str | PropertyFilter
    registrator: str
    modifier: str


class ObjectSearchParams(BaseEntitySearchParams, total=False):
    """Keyword filters accepted by ``search_objects`` / ``iter_objects``.

    Attributes:
        type (str): ObjectType code, e.g. ``"MOLECULE"``.
        space (str): Space code.
        project (str): Project identifier or code.
        collection (str): Collection perm_id or identifier.
        with_parents (bool): Include parent relationships in the results.
        with_children (bool): Include child relationships in the results.
    """

    type: str
    space: str
    project: str
    collection: str
    with_parents: bool
    with_children: bool


class CollectionSearchParams(BaseEntitySearchParams, total=False):
    """Keyword filters accepted by ``search_collections`` / ``iter_collections``.

    Attributes:
        type (str): CollectionType code.
        space (str): Space code.
        project (str): Project identifier or code.
        is_finished (bool): Filter on the finished flag.
    """

    type: str
    space: str
    project: str
    is_finished: bool


class DataSetSearchParams(BaseEntitySearchParams, total=False):
    """Keyword filters accepted by ``search_datasets`` / ``iter_datasets``.

    Attributes:
        type (str): DataSetType code.
        kind (str): A :data:`~pybis.types.DataSetKind` value.
        status (str): A :data:`~pybis.types.DataSetStatus` value.
        object (str): Owning Object perm_id or identifier.
        collection (str): Owning Collection perm_id or identifier.
        project (str): Project identifier or code.
        space (str): Space code.
        with_parents (bool): Include parent relationships in the results.
        with_children (bool): Include child relationships in the results.
    """

    type: str
    kind: str
    status: str
    object: str
    collection: str
    project: str
    space: str
    with_parents: bool
    with_children: bool


class BaseTypeSearchParams(BasePaginatedParams, total=False):
    """Filters shared by entity-type searches (``search_object_types``, ...).

    Attributes:
        code (str): Type code, e.g. ``"MOLECULE"``.
        with_vocabulary (bool): Eagerly load controlled-vocabulary terms of
            the type's property assignments.
    """

    code: str
    with_vocabulary: bool


class ObjectTypeSearchParams(BaseTypeSearchParams, total=False):
    """Keyword filters accepted by ``search_object_types``."""


class CollectionTypeSearchParams(BaseTypeSearchParams, total=False):
    """Keyword filters accepted by ``search_collection_types``."""


class DataSetTypeSearchParams(BaseTypeSearchParams, total=False):
    """Keyword filters accepted by ``search_dataset_types``."""


class SpaceSearchParams(BasePaginatedParams, total=False):
    """Keyword filters accepted by ``search_spaces``.

    Attributes:
        code (str): Space code.
    """

    code: str


class ProjectSearchParams(BasePaginatedParams, total=False):
    """Keyword filters accepted by ``search_projects``.

    Attributes:
        id (str): Identifying string, auto-classified (perm_id, identifier,
            or code).
        code (str): Project code.
        space (str): Space code.
    """

    id: str
    code: str
    space: str


# ---------------------------------------------------------------------------
# Fetch options
# ---------------------------------------------------------------------------


class BaseFetchOptions(TypedDict, total=False):
    """Related data that every entity can eagerly load.

    Attributes:
        tags (bool): Load assigned tags.
        history (bool): Load the modification history.
    """

    tags: bool
    history: bool


class HierarchicalFetchOptions(BaseFetchOptions, total=False):
    """Fetch options for entities with parent/child relationships.

    Attributes:
        parents (bool): Load parent entities.
        children (bool): Load child entities.
    """

    parents: bool
    children: bool


class ObjectFetchOptions(HierarchicalFetchOptions, total=False):
    """Eager-loading switches for Object searches.

    Attributes:
        properties (bool): Load property values.
        datasets (bool): Load attached datasets.
        collections (bool): Load the owning collection.
        object_type (bool): Load the full ObjectType.
        attachments (bool): Load attachments.
        registrator (bool): Load the registering user.
        modifier (bool): Load the last modifying user.
    """

    properties: bool
    datasets: bool
    collections: bool
    object_type: bool
    attachments: bool
    registrator: bool
    modifier: bool


class CollectionFetchOptions(BaseFetchOptions, total=False):
    """Eager-loading switches for Collection searches.

    Attributes:
        properties (bool): Load property values.
        datasets (bool): Load contained datasets.
        objects (bool): Load contained objects.
        collection_type (bool): Load the full CollectionType.
        attachments (bool): Load attachments.
        project (bool): Load the owning project.
    """

    properties: bool
    datasets: bool
    objects: bool
    collection_type: bool
    attachments: bool
    project: bool


class DataSetFetchOptions(HierarchicalFetchOptions, total=False):
    """Eager-loading switches for DataSet searches.

    Attributes:
        properties (bool): Load property values.
        object (bool): Load the owning object.
        collection (bool): Load the owning collection.
        dataset_type (bool): Load the full DataSetType.
        physical_data (bool): Load physical-data details (location, size, ...).
        linked_data (bool): Load external-DMS link details.
    """

    properties: bool
    object: bool
    collection: bool
    dataset_type: bool
    physical_data: bool
    linked_data: bool


# ---------------------------------------------------------------------------
# Creation parameters
# ---------------------------------------------------------------------------


class NewObjectParams(TypedDict, total=False):
    """Keyword arguments accepted by ``new_object`` (besides ``type``).

    Attributes:
        space (str): Space code the object lives in.
        project (str): Project identifier (requires project-level objects).
        collection (str): Owning collection perm_id or identifier.
        code (str): Object code; omit for auto-generated codes.
        parents (list[str]): Identifiers of parent objects.
        children (list[str]): Identifiers of child objects.
        tags (list[str]): Tags to assign.
        properties (dict[str, PropertyValue]): Initial property values.
    """

    space: str
    project: str
    collection: str
    code: str
    parents: list[str]
    children: list[str]
    tags: list[str]
    properties: dict[str, PropertyValue]


class NewCollectionParams(TypedDict, total=False):
    """Keyword arguments accepted by ``new_collection`` (besides ``type``).

    Attributes:
        space (str): Space code (used to resolve a bare project code).
        project (str): Project identifier the collection belongs to.
        code (str): Collection code.
        tags (list[str]): Tags to assign.
        properties (dict[str, PropertyValue]): Initial property values.
    """

    space: str
    project: str
    code: str
    tags: list[str]
    properties: dict[str, PropertyValue]


class NewDataSetParams(TypedDict, total=False):
    """Keyword arguments accepted by ``new_dataset`` (besides ``type``).

    Attributes:
        kind (str): A :data:`~pybis.types.DataSetKind` value
            (default: ``"PHYSICAL"``).
        object (str): Owning object perm_id or identifier.
        collection (str): Owning collection perm_id or identifier.
        files (list[str | Path]): Files to upload.
        folder (str | Path): Folder to upload recursively.
        parents (list[str]): Perm_ids of parent datasets.
        properties (dict[str, PropertyValue]): Initial property values.
        dss_code (str): Target data store server code.
    """

    kind: str
    object: str
    collection: str
    files: list[str | Path]
    folder: str | Path
    parents: list[str]
    properties: dict[str, PropertyValue]
    dss_code: str


__all__ = [
    "BaseEntitySearchParams",
    "BaseFetchOptions",
    "BasePaginatedParams",
    "BaseTypeSearchParams",
    "CollectionFetchOptions",
    "CollectionSearchParams",
    "CollectionTypeSearchParams",
    "DataSetFetchOptions",
    "DataSetSearchParams",
    "DataSetTypeSearchParams",
    "HierarchicalFetchOptions",
    "NewCollectionParams",
    "NewDataSetParams",
    "NewObjectParams",
    "ObjectFetchOptions",
    "ObjectSearchParams",
    "ObjectTypeSearchParams",
    "ProjectSearchParams",
    "SpaceSearchParams",
]
