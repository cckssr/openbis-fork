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
"""``Literal`` types for the fixed vocabularies of the openBIS v3 API.

These are deliberately :data:`typing.Literal` unions rather than enums:
callers pass plain strings, and type-checkers verify them against the
server-side vocabulary.

Example:
    >>> from pybis.types import DataSetKind

    >>> def make(kind: DataSetKind) -> None: ...

    >>> make("PHYSICAL")
"""

from __future__ import annotations

from typing import Literal, TypeAlias

DataSetKind: TypeAlias = Literal["PHYSICAL", "CONTAINER", "LINK"]
"""Storage kind of a dataset.

- ``"PHYSICAL"`` — stores actual files in the data store.
- ``"CONTAINER"`` — groups other datasets; holds no files itself.
- ``"LINK"`` — references files in an external data management system.
"""

EntityKind: TypeAlias = Literal["OBJECT", "COLLECTION", "DATASET", "MATERIAL"]
"""High-level entity kinds, e.g. in semantic annotations and search criteria."""

DataType: TypeAlias = Literal[
    # Scalar
    "INTEGER",
    "REAL",
    "BOOLEAN",
    "VARCHAR",
    "MULTILINE_VARCHAR",
    "HYPERLINK",
    "XML",
    "TIMESTAMP",
    "DATE",
    "CONTROLLEDVOCABULARY",
    "SAMPLE",  # object-link property
    "MATERIAL",  # legacy
    # Array (openBIS >= 20.10)
    "ARRAY_INTEGER",
    "ARRAY_REAL",
    "ARRAY_STRING",  # the actual API name; not ARRAY_VARCHAR
    "ARRAY_TIMESTAMP",
    # Structured
    "JSON",
]
"""Data types supported by openBIS property types.

Note that the array variant of ``VARCHAR`` is named ``"ARRAY_STRING"`` on the
server, and that ``"ARRAY_*"`` and ``"JSON"`` types require a sufficiently
recent openBIS version (see :func:`pybis.client.requires_version`).
"""

StorageFormat: TypeAlias = Literal["PROPRIETARY", "BDS_DIRECTORY"]
"""Physical storage format of a dataset."""

DataSetStatus: TypeAlias = Literal[
    "AVAILABLE",
    "LOCKED",
    "ARCHIVED",
    "UNARCHIVE_PENDING",
    "ARCHIVE_PENDING",
    "BACKUP_PENDING",
]
"""Archiving status of a physical dataset."""

Role: TypeAlias = Literal[
    "OBSERVER",
    "USER",
    "POWER_USER",
    "SPACE_ETL_SERVER",
    "PROJECT_ETL_SERVER",
    "INSTANCE_ETL_SERVER",
    "PROJECT_ADMIN",
    "SPACE_ADMIN",
    "INSTANCE_ADMIN",
]
"""Combined role names as used by the v3 role-assignment API."""

RoleCode: TypeAlias = Literal["ADMIN", "POWER_USER", "USER", "OBSERVER", "ETL_SERVER"]
"""Bare role names used together with a :data:`RoleLevel`, e.g. in ``assign_role``."""

RoleLevel: TypeAlias = Literal["INSTANCE", "SPACE", "PROJECT"]
"""Scope at which a role assignment applies."""

PluginType: TypeAlias = Literal["JYTHON", "PREDEPLOYED"]
"""Implementation type of a server-side plugin."""

__all__ = [
    "DataSetKind",
    "DataSetStatus",
    "DataType",
    "EntityKind",
    "PluginType",
    "Role",
    "RoleCode",
    "RoleLevel",
    "StorageFormat",
]
