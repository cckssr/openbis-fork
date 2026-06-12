#
#   Copyright ETH 2018 - 2026 Zürich, Scientific IT Services
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
"""Semantic annotation support for openBIS entity and property types."""

from __future__ import annotations

from typing import Any, Optional, TYPE_CHECKING

from tabulate import tabulate

from .utils import VERBOSE

if TYPE_CHECKING:
    from .pybis import Openbis


class SemanticAnnotation:
    """An ontology-based annotation attached to an entity.

    Semantic annotations link openBIS entities to external ontology terms,
    enabling machine-readable, interoperable metadata.  Each annotation
    carries two optional term references:

    - **Predicate** — the relationship between the entity and an ontology term.
    - **Descriptor** — the ontology term that *describes* the entity.

    Annotations are typically created via
    :meth:`~pybis.entity_type.SampleType.add_semantic_annotation` or
    retrieved via ``openbis.search_semantic_annotations()``.

    Attributes:
        permId (Optional[str]): Server-assigned permanent identifier.
        entityType (Optional[str]): Code of the sample type this annotation
            belongs to (e.g. ``"EXPERIMENTAL_STEP"``).
        propertyType (Optional[str]): Code of the property type this
            annotation belongs to.
        predicateOntologyId (Optional[str]): Identifier of the predicate
            ontology (e.g. ``"ro"``).
        predicateOntologyVersion (Optional[str]): Version of the predicate
            ontology.
        predicateAccessionId (Optional[str]): Accession ID of the predicate
            term (e.g. ``"RO:0000056"``).
        descriptorOntologyId (Optional[str]): Identifier of the descriptor
            ontology.
        descriptorOntologyVersion (Optional[str]): Version of the descriptor
            ontology.
        descriptorAccessionId (Optional[str]): Accession ID of the descriptor
            term.
        creationDate (Optional[str]): ISO-8601 timestamp of when the
            annotation was created.

    Example:
        >>> sample_type = openbis.get_sample_type("EXPERIMENTAL_STEP")
        >>> sample_type.add_semantic_annotation(
        ...     predicateOntologyId="ro",
        ...     predicateAccessionId="RO:0000056",
        ...     descriptorOntologyId="bao",
        ...     descriptorAccessionId="BAO:0000015",
        ... )
    """

    permId: Optional[str]
    entityType: Optional[str]
    propertyType: Optional[str]
    predicateOntologyId: Optional[str]
    predicateOntologyVersion: Optional[str]
    predicateAccessionId: Optional[str]
    descriptorOntologyId: Optional[str]
    descriptorOntologyVersion: Optional[str]
    descriptorAccessionId: Optional[str]
    creationDate: Optional[str]

    def __init__(self, openbis_obj: Openbis, isNew: bool = True, **kwargs: Any) -> None:
        """Create a SemanticAnnotation instance.

        Args:
            openbis_obj: The :class:`~pybis.Openbis` connection instance.
            isNew: ``True`` if this annotation has not yet been persisted
                to openBIS.  ``False`` when loaded from the server.
            **kwargs: Annotation field values.  Recognised keys:
                ``permId``, ``entityType``, ``propertyType``,
                ``predicateOntologyId``, ``predicateOntologyVersion``,
                ``predicateAccessionId``, ``descriptorOntologyId``,
                ``descriptorOntologyVersion``, ``descriptorAccessionId``,
                ``creationDate``.
        """
        self._openbis = openbis_obj
        self._isNew = isNew

        self.permId = kwargs.get("permId")
        self.entityType = kwargs.get("entityType")
        self.propertyType = kwargs.get("propertyType")
        self.predicateOntologyId = kwargs.get("predicateOntologyId")
        self.predicateOntologyVersion = kwargs.get("predicateOntologyVersion")
        self.predicateAccessionId = kwargs.get("predicateAccessionId")
        self.descriptorOntologyId = kwargs.get("descriptorOntologyId")
        self.descriptorOntologyVersion = kwargs.get("descriptorOntologyVersion")
        self.descriptorAccessionId = kwargs.get("descriptorAccessionId")
        self.creationDate = kwargs.get("creationDate")

    def __dir__(self) -> list[str]:
        """Return public attributes and methods for tab-completion.

        Returns:
            A list of attribute and method names.
        """
        return [
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
            "save()",
            "delete()",
        ]

    def save(self) -> None:
        """Persist this annotation to openBIS (create or update).

        Calls :meth:`_create` for new annotations, or :meth:`_update` for
        existing ones.  After a successful create the ``permId`` attribute
        is populated with the server-assigned identifier.

        Example:
            >>> ann = SemanticAnnotation(
            ...     openbis, isNew=True, entityType="MY_TYPE", predicateOntologyId="ro"
            ... )
            >>> ann.save()
            >>> print(ann.permId)
            202502010123456789-1
        """
        if self._isNew:
            self._create()
        else:
            self._update()

    def _create(self) -> None:
        """Send a ``createSemanticAnnotations`` request to the V3 API.

        Constructs the creation DTO based on whether an ``entityType``,
        ``propertyType``, or both are set, then populates all ontology
        term fields.
        """
        creation: dict[str, Any] = {
            "@type": "as.dto.semanticannotation.create.SemanticAnnotationCreation"
        }

        if self.entityType is not None and self.propertyType is not None:
            creation["propertyAssignmentId"] = {
                "@type": "as.dto.property.id.PropertyAssignmentPermId",
                "entityTypeId": {
                    "@type": "as.dto.entitytype.id.EntityTypePermId",
                    "permId": self.entityType,
                    "entityKind": "SAMPLE",
                },
                "propertyTypeId": {
                    "@type": "as.dto.property.id.PropertyTypePermId",
                    "permId": self.propertyType,
                },
            }
        elif self.entityType is not None:
            creation["entityTypeId"] = {
                "@type": "as.dto.entitytype.id.EntityTypePermId",
                "permId": self.entityType,
                "entityKind": "SAMPLE",
            }
        elif self.propertyType is not None:
            creation["propertyTypeId"] = {
                "@type": "as.dto.property.id.PropertyTypePermId",
                "permId": self.propertyType,
            }

        for attr in [
            "predicateOntologyId",
            "predicateOntologyVersion",
            "predicateAccessionId",
            "descriptorOntologyId",
            "descriptorOntologyVersion",
            "descriptorAccessionId",
        ]:
            creation[attr] = getattr(self, attr)

        request = {
            "method": "createSemanticAnnotations",
            "params": [self._openbis.token, [creation]],
        }

        response = self._openbis._post_request(self._openbis.as_v3, request)
        self._isNew = False
        self.permId = response[0]["permId"]

        if VERBOSE:
            print("Semantic annotation successfully created.")

    def _update(self) -> None:
        """Send an ``updateSemanticAnnotations`` request to the V3 API.

        All six ontology term fields (predicate and descriptor) are
        included in the update payload, regardless of whether they
        changed.
        """
        update = {
            "@type": "as.dto.semanticannotation.update.SemanticAnnotationUpdate",
            "semanticAnnotationId": {
                "@type": "as.dto.semanticannotation.id.SemanticAnnotationPermId",
                "permId": self.permId,
            },
        }

        for attr in [
            "predicateOntologyId",
            "predicateOntologyVersion",
            "predicateAccessionId",
            "descriptorOntologyId",
            "descriptorOntologyVersion",
            "descriptorAccessionId",
        ]:
            update[attr] = {
                "@type": "as.dto.common.update.FieldUpdateValue",
                "isModified": True,
                "value": getattr(self, attr),
            }

        request = {
            "method": "updateSemanticAnnotations",
            "params": [self._openbis.token, [update]],
        }

        self._openbis._post_request(self._openbis.as_v3, request)
        if VERBOSE:
            print("Semantic annotation successfully updated.")

    def delete(self, reason: str) -> None:
        """Delete this semantic annotation from openBIS.

        Args:
            reason: Human-readable reason recorded with the deletion.

        Example:
            >>> ann.delete("Ontology term retired")
        """
        self._openbis.delete_entity(  # type: ignore[no-untyped-call]  # reason: legacy client module
            entity="SemanticAnnotation", id=self.permId, reason=reason
        )
        if VERBOSE:
            print("Semantic annotation successfully deleted.")

    def _repr_html_(self) -> str:
        attrs = [
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

        html = """
            <table border="1" class="dataframe">
            <thead>
                <tr style="text-align: right;">
                <th>attribute</th>
                <th>value</th>
                </tr>
            </thead>
            <tbody>
        """

        for attr in attrs:
            html += f"<tr> <td>{attr}</td> <td>{getattr(self, attr, '')}</td> </tr>"

        html += """
            </tbody>
            </table>
        """
        return html

    def __repr__(self) -> str:
        """Return a table of all annotation fields."""
        headers = ["attribute", "value"]
        lines = []
        lines.append(["permId", self.permId])
        lines.append(["entityType", self.entityType])
        lines.append(["propertyType", self.propertyType])
        lines.append(["predicateOntologyId", self.predicateOntologyId])
        lines.append(["predicateOntologyVersion", self.predicateOntologyVersion])
        lines.append(["predicateAccessionId", self.predicateAccessionId])
        lines.append(["descriptorOntologyId", self.descriptorOntologyId])
        lines.append(["descriptorOntologyVersion", self.descriptorOntologyVersion])
        lines.append(["descriptorAccessionId", self.descriptorAccessionId])
        lines.append(["creationDate", self.creationDate])
        return tabulate(lines, headers=headers)
