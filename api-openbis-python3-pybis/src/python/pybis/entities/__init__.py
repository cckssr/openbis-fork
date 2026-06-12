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
"""Typed openBIS entity classes (Object, Collection, DataSet, Space, ...)."""

from ._properties import PropertyBag
from .base import EntityBehavior
from .collection import Collection
from .dataset import DataSet, DataSetType
from .entity_type import CollectionType, MaterialType, ObjectType, PropertyType
from .external_dms import ExternalDMS
from .object import Object
from .pat import PersonalAccessToken
from .plugin import Plugin
from .project import Project
from .semantic_annotation import SemanticAnnotation
from .server import ServerInformation, ServerVersion, requires_version
from .space import Space

__all__ = [
    "Collection",
    "CollectionType",
    "DataSet",
    "DataSetType",
    "EntityBehavior",
    "ExternalDMS",
    "MaterialType",
    "Object",
    "ObjectType",
    "PersonalAccessToken",
    "Plugin",
    "Project",
    "PropertyBag",
    "PropertyType",
    "SemanticAnnotation",
    "ServerInformation",
    "ServerVersion",
    "Space",
    "requires_version",
]
