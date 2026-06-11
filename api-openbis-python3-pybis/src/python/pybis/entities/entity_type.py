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
"""Entity types (ObjectType, CollectionType) and their client API."""

from __future__ import annotations

from collections.abc import Sequence
from typing import TYPE_CHECKING, Any, cast

from ..api.rpc import parse_jackson
from ..api.search import explicit_id_criterion
from ..definitions import (
    get_definition_for_entity,
    get_fetchoption_for_entity,
    get_method_for_entity,
    get_type_for_entity,
)
from ..entity_type import ExperimentType as CollectionType
from ..entity_type import SampleType as ObjectType
from ..exceptions import NotFoundError
from ..types.results import SearchResult
from ..utils import extract_nested_permid, extract_permid, format_timestamp

if TYPE_CHECKING:
    import pandas as pd

from ._mixin import ClientApiMixin


def _types_df_builder(entity: str) -> Any:
    """Build the DataFrame builder for an entity-type search result."""

    def build(types: Sequence[Any]) -> "pd.DataFrame":
        from pandas import DataFrame

        attrs = get_definition_for_entity(entity)["attrs"]
        if not types:
            return DataFrame(columns=attrs)
        df = DataFrame([t.data for t in types])
        for column, mapper in [
            ("permId", extract_permid),
            ("modificationDate", format_timestamp),
            ("validationPlugin", extract_nested_permid),
        ]:
            if column in df:
                df[column] = df[column].map(mapper)
        return cast("pd.DataFrame", df[df.columns.intersection(attrs)])

    return build


class _EntityTypeApi(ClientApiMixin):
    """Generic entity-type lookup/search shared by all type kinds."""

    def _get_entity_type_v2(
        self,
        entity: str,
        cls: Any,
        code: str,
        *,
        with_vocabulary: bool = False,
    ) -> Any:
        """Fetch one entity type by code; None when missing (cached)."""
        cached = self._object_cache(entity=entity, code=code)
        if cached is not None:
            return cached

        fetch_options = get_fetchoption_for_entity(entity)
        if with_vocabulary:
            fetch_options["propertyAssignments"]["propertyType"]["vocabulary"] = {
                "@type": "as.dto.vocabulary.fetchoptions.VocabularyFetchOptions",
                "terms": {
                    "@type": "as.dto.vocabulary.fetchoptions.VocabularyTermFetchOptions"
                },
            }
        request = {
            "method": get_method_for_entity(entity, "get"),
            "params": [
                self.token,
                [{"permId": code, "@type": "as.dto.entitytype.id.EntityTypePermId"}],
                fetch_options,
            ],
        }
        resp = self._post_request(self.as_v3, request)
        parse_jackson(resp)
        for ident in resp:
            obj = cls(openbis_obj=self, data=resp[ident])
            self._object_cache(entity=entity, code=ident, value=obj)
            return obj
        return None

    def _search_entity_types_v2(
        self,
        entity: str,
        cls: Any,
        *,
        code: str | None = None,
        count: int = 25,
        start_with: int = 0,
    ) -> SearchResult[Any]:
        """Search entity types of one kind."""
        if code is not None:
            criteria = get_type_for_entity(entity, "search")
            criteria["criteria"] = [explicit_id_criterion("code", code)]
            criteria["operator"] = "AND"
        else:
            criteria = get_type_for_entity(entity, "search")

        fetch_options = get_fetchoption_for_entity(entity)
        fetch_options["from"] = start_with
        fetch_options["count"] = count

        request = {
            "method": get_method_for_entity(entity, "search"),
            "params": [self.token, criteria, fetch_options],
        }
        resp = self._post_request(self.as_v3, request)
        parse_jackson(resp)
        items = [cls(openbis_obj=self, data=obj) for obj in resp["objects"]]
        return SearchResult(
            items, int(resp.get("totalCount", len(items))), _types_df_builder(entity)
        )

    # --- object types ---------------------------------------------------------

    def get_object_type(
        self, code: str, *, with_vocabulary: bool = False
    ) -> ObjectType | None:
        """Get a single ObjectType by code, or None if it does not exist.

        Args:
            code: The type code, e.g. ``"MOLECULE"``.
            with_vocabulary: Eagerly load controlled-vocabulary terms of the
                type's property assignments.
        """
        return cast(
            "ObjectType | None",
            self._get_entity_type_v2(
                "sampleType", ObjectType, code, with_vocabulary=with_vocabulary
            ),
        )

    def get_object_type_or_raise(
        self, code: str, *, with_vocabulary: bool = False
    ) -> ObjectType:
        """Get a single ObjectType by code; raise if it does not exist.

        Raises:
            NotFoundError: No object type exists with this code.
        """
        entity_type = self.get_object_type(code, with_vocabulary=with_vocabulary)
        if entity_type is None:
            raise NotFoundError("object type", code)
        return entity_type

    def search_object_types(
        self,
        *,
        code: str | None = None,
        count: int = 25,
        start_with: int = 0,
    ) -> SearchResult[ObjectType]:
        """Search for ObjectTypes.

        Args:
            code: Filter by type code (exact match).
            count: Maximum number of results (default: 25).
            start_with: Pagination offset (default: 0).
        """
        return self._search_entity_types_v2(
            "sampleType", ObjectType, code=code, count=count, start_with=start_with
        )

    # --- collection types -------------------------------------------------------

    def get_collection_type(self, code: str) -> CollectionType | None:
        """Get a single CollectionType by code, or None if it does not exist."""
        return cast(
            "CollectionType | None",
            self._get_entity_type_v2("experimentType", CollectionType, code),
        )

    def get_collection_type_or_raise(self, code: str) -> CollectionType:
        """Get a single CollectionType by code; raise if it does not exist.

        Raises:
            NotFoundError: No collection type exists with this code.
        """
        entity_type = self.get_collection_type(code)
        if entity_type is None:
            raise NotFoundError("collection type", code)
        return entity_type

    def search_collection_types(
        self,
        *,
        code: str | None = None,
        count: int = 25,
        start_with: int = 0,
    ) -> SearchResult[CollectionType]:
        """Search for CollectionTypes.

        Args:
            code: Filter by type code (exact match).
            count: Maximum number of results (default: 25).
            start_with: Pagination offset (default: 0).
        """
        return self._search_entity_types_v2(
            "experimentType",
            CollectionType,
            code=code,
            count=count,
            start_with=start_with,
        )

    # --- creation ------------------------------------------------------------------

    def new_object_type(
        self,
        code: str,
        *,
        description: str | None = None,
        generated_code_prefix: str | None = None,
        subcode_unique: bool = False,
        auto_generated_code: bool = False,
        listable: bool = True,
        show_container: bool = False,
        show_parents: bool = True,
        show_parent_metadata: bool = False,
        validation_plugin: str | None = None,
    ) -> ObjectType:
        """Construct an unsaved ObjectType; call ``.save()`` to persist it.

        Args:
            code: Code of the new type, e.g. ``"MOLECULE"``.
            description: Free-text description.
            generated_code_prefix: Prefix for auto-generated object codes.
            subcode_unique: Subcodes must be unique within the type.
            auto_generated_code: Generate object codes automatically.
            listable: Show the type in listings.
            show_container: Show the container field in forms.
            show_parents: Show the parents field in forms.
            show_parent_metadata: Show parent metadata in forms.
            validation_plugin: Name of the validation plugin.
        """
        return ObjectType(
            self,
            code=code,
            description=description,
            generatedCodePrefix=generated_code_prefix,
            subcodeUnique=subcode_unique,
            autoGeneratedCode=auto_generated_code,
            listable=listable,
            showContainer=show_container,
            showParents=show_parents,
            showParentMetadata=show_parent_metadata,
            validationPlugin=validation_plugin,
        )

    def new_collection_type(
        self,
        code: str,
        *,
        description: str | None = None,
        validation_plugin: str | None = None,
    ) -> CollectionType:
        """Construct an unsaved CollectionType; call ``.save()`` to persist it.

        Args:
            code: Code of the new type.
            description: Free-text description.
            validation_plugin: Name of the validation plugin.
        """
        return CollectionType(
            self,
            code=code,
            description=description,
            validationPlugin=validation_plugin,
        )


__all__ = ["CollectionType", "ObjectType", "_EntityTypeApi"]
