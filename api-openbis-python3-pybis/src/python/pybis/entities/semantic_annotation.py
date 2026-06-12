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
"""Semantic annotations: ontology terms attached to types and properties."""

from __future__ import annotations

from collections.abc import Sequence
from typing import TYPE_CHECKING, Any, cast

from ..api.rpc import parse_jackson
from ..api.search import explicit_id_criterion
from ..exceptions import NotFoundError
from ..semantic_annotation import SemanticAnnotation
from ..types.results import SearchResult
from ..utils import format_timestamp
from ._mixin import ClientApiMixin

if TYPE_CHECKING:
    import pandas as pd

_ANNOTATION_ATTRS = [
    "permId",
    "entityType",
    "propertyType",
    "predicateOntologyId",
    "predicateOntologyVersion",
    "predicateAccessionId",
    "descriptorOntologyId",
    "descriptorOntologyVersion",
    "descriptorAccessionId",
    "creationDate",
]

_FETCH_OPTIONS = {
    "@type": "as.dto.semanticannotation.fetchoptions.SemanticAnnotationFetchOptions",
    "entityType": {"@type": "as.dto.entitytype.fetchoptions.EntityTypeFetchOptions"},
    "propertyType": {"@type": "as.dto.property.fetchoptions.PropertyTypeFetchOptions"},
    "propertyAssignment": {
        "@type": "as.dto.property.fetchoptions.PropertyAssignmentFetchOptions",
        "entityType": {
            "@type": "as.dto.entitytype.fetchoptions.EntityTypeFetchOptions"
        },
        "propertyType": {
            "@type": "as.dto.property.fetchoptions.PropertyTypeFetchOptions"
        },
    },
}


def _annotations_df(items: Sequence[Any]) -> "pd.DataFrame":
    """Build the notebook DataFrame view for a page of annotations."""
    from pandas import DataFrame

    if not items:
        return DataFrame(columns=_ANNOTATION_ATTRS)
    df = DataFrame(
        [
            {attr: getattr(item, attr, None) for attr in _ANNOTATION_ATTRS}
            for item in items
        ]
    )
    return cast("pd.DataFrame", df[df.columns.intersection(_ANNOTATION_ATTRS)])


def _flatten(obj: dict[str, Any]) -> dict[str, Any]:
    """Flatten the nested type references of one server-side annotation."""
    obj["permId"] = obj["permId"]["permId"]
    if obj.get("entityType") is not None:
        obj["entityType"] = obj["entityType"]["code"]
    elif obj.get("propertyType") is not None:
        obj["propertyType"] = obj["propertyType"]["code"]
    elif obj.get("propertyAssignment") is not None:
        obj["entityType"] = obj["propertyAssignment"]["entityType"]["code"]
        obj["propertyType"] = obj["propertyAssignment"]["propertyType"]["code"]
    obj["creationDate"] = format_timestamp(obj["creationDate"])
    return {key: obj.get(key) for key in _ANNOTATION_ATTRS}


class _SemanticAnnotationApi(ClientApiMixin):
    """Semantic-annotation methods of the Openbis client."""

    def search_semantic_annotations(
        self,
        *,
        perm_id: str | None = None,
        entity_type: str | None = None,
        property_type: str | None = None,
        count: int = 25,
        start_with: int = 0,
    ) -> SearchResult[SemanticAnnotation]:
        """Search for semantic annotations.

        Args:
            perm_id: Filter by the annotation's permId.
            entity_type: Filter by annotated entity (object) type code.
            property_type: Filter by annotated property type code. Combined
                with entity_type, matches the property *assignment*.
            count: Maximum number of results to return (default: 25).
            start_with: Pagination offset (default: 0).
        """
        criteria: list[dict[str, Any]] = []
        type_criteria: list[dict[str, Any]] = []
        if perm_id is not None:
            criteria.append(
                {
                    "@type": "as.dto.common.search.PermIdSearchCriteria",
                    "fieldValue": {
                        "@type": "as.dto.common.search.StringEqualToValue",
                        "value": perm_id,
                    },
                }
            )
        if entity_type is not None:
            type_criteria.append(
                {
                    "@type": "as.dto.entitytype.search.EntityTypeSearchCriteria",
                    "criteria": [explicit_id_criterion("code", entity_type)],
                }
            )
        if property_type is not None:
            type_criteria.append(
                {
                    "@type": "as.dto.property.search.PropertyTypeSearchCriteria",
                    "criteria": [explicit_id_criterion("code", property_type)],
                }
            )
        if entity_type is not None and property_type is not None:
            criteria.append(
                {
                    "@type": "as.dto.property.search.PropertyAssignmentSearchCriteria",
                    "criteria": type_criteria,
                }
            )
        else:
            criteria += type_criteria

        fetchopts: dict[str, Any] = dict(_FETCH_OPTIONS)
        fetchopts["from"] = start_with
        fetchopts["count"] = count
        request = {
            "method": "searchSemanticAnnotations",
            "params": [
                self.token,
                {
                    "@type": (
                        "as.dto.semanticannotation.search"
                        ".SemanticAnnotationSearchCriteria"
                    ),
                    "criteria": criteria,
                },
                fetchopts,
            ],
        }
        resp = self._post_request(self.as_v3, request)
        objects = resp["objects"]
        parse_jackson(objects)
        items = [
            SemanticAnnotation(cast("Any", self), isNew=False, **_flatten(obj))
            for obj in objects
        ]
        return SearchResult(
            items, int(resp.get("totalCount", len(items))), _annotations_df
        )

    def get_semantic_annotation(self, perm_id: str) -> SemanticAnnotation | None:
        """Get a single semantic annotation by permId, or None if missing."""
        result = self.search_semantic_annotations(perm_id=perm_id, count=1)
        for annotation in result:
            return annotation
        return None

    def get_semantic_annotation_or_raise(self, perm_id: str) -> SemanticAnnotation:
        """Get a single semantic annotation by permId; raise if missing.

        Raises:
            NotFoundError: No semantic annotation exists with this permId.
        """
        annotation = self.get_semantic_annotation(perm_id)
        if annotation is None:
            raise NotFoundError("semantic annotation", perm_id)
        return annotation

    def new_semantic_annotation(
        self,
        *,
        entity_type: str | None = None,
        property_type: str | None = None,
        predicate_ontology_id: str | None = None,
        predicate_ontology_version: str | None = None,
        predicate_accession_id: str | None = None,
        descriptor_ontology_id: str | None = None,
        descriptor_ontology_version: str | None = None,
        descriptor_accession_id: str | None = None,
    ) -> SemanticAnnotation:
        """Construct an unsaved SemanticAnnotation; ``.save()`` persists it.

        Args:
            entity_type: Code of the annotated entity (object) type.
            property_type: Code of the annotated property type.
            predicate_ontology_id: Identifier of the predicate ontology.
            predicate_ontology_version: Version of the predicate ontology.
            predicate_accession_id: Accession ID of the predicate term.
            descriptor_ontology_id: Identifier of the descriptor ontology.
            descriptor_ontology_version: Version of the descriptor ontology.
            descriptor_accession_id: Accession ID of the descriptor term.
        """
        return SemanticAnnotation(
            cast("Any", self),
            isNew=True,
            entityType=entity_type,
            propertyType=property_type,
            predicateOntologyId=predicate_ontology_id,
            predicateOntologyVersion=predicate_ontology_version,
            predicateAccessionId=predicate_accession_id,
            descriptorOntologyId=descriptor_ontology_id,
            descriptorOntologyVersion=descriptor_ontology_version,
            descriptorAccessionId=descriptor_accession_id,
        )


__all__ = ["SemanticAnnotation", "_SemanticAnnotationApi"]
