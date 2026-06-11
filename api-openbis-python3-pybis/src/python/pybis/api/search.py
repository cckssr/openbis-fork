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
"""Builders translating typed search parameters into JSON-RPC criteria.

These functions produce exactly the criterion dicts the openBIS v3 API
expects.  They replace the magic-string parser of pybis 1.x
(``_subcriteria_for_properties``): operators arrive as
:class:`~pybis.api.filters.PropertyFilter` objects, never embedded in
values.

Entity names used here are the *internal* wire vocabulary (``sample``,
``experiment``, ``dataset``); the public Object/Collection naming is mapped
by the client methods.
"""

from __future__ import annotations

from typing import Literal

from ..exceptions import ValidationError
from .filters import HierarchyPropertyFilter, PropertyFilter, eq, wildcard
from .identifiers import classify
from .rpc import JsonPayload

# Criterion families: which PropertySearchCriteria class carries which
# operator family on the wire.
_STRING_OPS = frozenset(["eq", "contains", "starts_with", "ends_with", "any"])
_NUMBER_OPS = frozenset(["gt", "gte", "lt", "lte", "number_eq"])
_DATE_OPS = frozenset(["date_eq", "date_after", "date_before"])

_RELATION_CRITERIA: dict[str, dict[str, str]] = {
    "sample": {
        "parent": "as.dto.sample.search.SampleParentsSearchCriteria",
        "child": "as.dto.sample.search.SampleChildrenSearchCriteria",
        "container": "as.dto.sample.search.SampleContainerSearchCriteria",
    },
    "dataset": {
        "parent": "as.dto.dataset.search.DataSetParentsSearchCriteria",
        "child": "as.dto.dataset.search.DataSetChildrenSearchCriteria",
        "container": "as.dto.dataset.search.DataSetContainerSearchCriteria",
    },
}

_ID_CRITERION_FIELDS: dict[str, tuple[str, str]] = {
    "perm_id": ("as.dto.common.search.PermIdSearchCriteria", "perm_id"),
    "identifier": ("as.dto.common.search.IdentifierSearchCriteria", "identifier"),
    "code": ("as.dto.common.search.CodeSearchCriteria", "code"),
}


def coerce_filter(condition: str | PropertyFilter) -> PropertyFilter:
    """Promote a plain string to a filter (exact match; ``*`` → wildcards)."""
    if isinstance(condition, PropertyFilter):
        return condition
    if "*" in condition:
        return wildcard(condition)
    return eq(condition)


def _field_name(code: str) -> str:
    """Normalize a property code: 1.x mapped a leading ``_`` to ``$``."""
    if code.startswith("_"):
        code = "$" + code[1:]
    return code.upper()


def _criterion_type(f: PropertyFilter) -> str:
    """Pick the PropertySearchCriteria class for the filter's operator."""
    if f.operator in _NUMBER_OPS:
        return "as.dto.common.search.NumberPropertySearchCriteria"
    if f.operator in _DATE_OPS:
        return "as.dto.common.search.DatePropertySearchCriteria"
    return "as.dto.common.search.StringPropertySearchCriteria"


def property_criterion(code: str, condition: str | PropertyFilter) -> JsonPayload:
    """Build the search criterion for one property condition.

    Args:
        code: Property code, e.g. ``"FORMULA"`` (a leading ``_`` is
            translated to ``$`` as in pybis 1.x).
        condition: A :class:`PropertyFilter`, or a plain string meaning
            exact match (``*`` switches on wildcard matching).

    Returns:
        The ``*PropertySearchCriteria`` dict for the JSON-RPC request.
    """
    f = coerce_filter(condition)
    return {
        "@type": _criterion_type(f),
        "fieldName": _field_name(code),
        "fieldType": "PROPERTY",
        "fieldValue": f.to_rpc(),
        "useWildcards": f.use_wildcards,
    }


def attribute_date_criterion(
    attribute: Literal["registration_date", "modification_date"],
    condition: str | PropertyFilter,
) -> JsonPayload:
    """Build the criterion for the registration or modification date.

    Args:
        attribute: Which timestamp attribute to filter on.
        condition: A date filter (:func:`~pybis.api.filters.date_eq`,
            :func:`~pybis.api.filters.date_after`,
            :func:`~pybis.api.filters.date_before`) or a plain ISO date
            string meaning exact match.

    Returns:
        The ``RegistrationDateSearchCriteria`` /
        ``ModificationDateSearchCriteria`` dict.

    Raises:
        ValidationError: The filter is not a date filter.
    """
    f = condition
    if isinstance(f, str):
        f = PropertyFilter("date_eq", f)
    if f.operator not in _DATE_OPS:
        raise ValidationError(
            f"{attribute} accepts date filters (date_eq/date_after/date_before),"
            f" got operator {f.operator!r}"
        )
    criteria_type = (
        "as.dto.common.search.RegistrationDateSearchCriteria"
        if attribute == "registration_date"
        else "as.dto.common.search.ModificationDateSearchCriteria"
    )
    return {
        "@type": criteria_type,
        "fieldName": attribute.replace("_", "").upper(),
        "fieldType": "ATTRIBUTE",
        "fieldValue": f.to_rpc(),
    }


def hierarchy_property_criterion(
    entity: Literal["sample", "dataset"], h: HierarchyPropertyFilter
) -> JsonPayload:
    """Build the criterion for a property of a parent/child/container.

    Args:
        entity: Wire-level entity the search runs on.
        h: The relation, property code, and condition.

    Returns:
        The relation sub-criteria dict (e.g. ``SampleParentsSearchCriteria``
        wrapping a property criterion).

    Raises:
        ValidationError: The entity does not support the relation.
    """
    try:
        relation_type = _RELATION_CRITERIA[entity][h.relation]
    except KeyError:
        raise ValidationError(
            f"{entity!r} does not support {h.relation!r} property filters"
        ) from None
    return {
        "@type": relation_type,
        "criteria": [property_criterion(h.property_code, h.filter)],
    }


def id_criterion(value: str) -> JsonPayload:
    """Build the criterion matching one identifying string, auto-classified.

    Args:
        value: A perm_id, path-style identifier, or bare code.

    Returns:
        The ``PermIdSearchCriteria`` / ``IdentifierSearchCriteria`` /
        ``CodeSearchCriteria`` dict.
    """
    cid = classify(value)
    criteria_type, field_name = _ID_CRITERION_FIELDS[cid.kind]
    value_out = cid.value.upper() if cid.kind == "code" else cid.value
    return {
        "@type": criteria_type,
        "fieldName": field_name,
        "fieldType": "ATTRIBUTE",
        "fieldValue": {
            "value": value_out,
            "@type": "as.dto.common.search.StringEqualToValue",
        },
    }


def explicit_id_criterion(kind: str, value: str) -> JsonPayload:
    """Build an id criterion without auto-classification.

    Args:
        kind: One of ``"perm_id"``, ``"identifier"``, ``"code"``.
        value: The identifying string, used as given (codes uppercased).

    Returns:
        The corresponding criterion dict.
    """
    criteria_type, field_name = _ID_CRITERION_FIELDS[kind]
    value_out = value.upper() if kind == "code" else value
    return {
        "@type": criteria_type,
        "fieldName": field_name,
        "fieldType": "ATTRIBUTE",
        "fieldValue": {
            "value": value_out,
            "@type": "as.dto.common.search.StringEqualToValue",
        },
    }


def tags_criterion(tags: list[str]) -> JsonPayload:
    """Build the sub-criteria requiring all given tag codes.

    Args:
        tags: Tag codes the entity must carry.

    Returns:
        The ``TagSearchCriteria`` dict (AND over the codes).
    """
    return {
        "@type": "as.dto.tag.search.TagSearchCriteria",
        "operator": "AND",
        "criteria": [
            {
                "@type": "as.dto.common.search.CodeSearchCriteria",
                "fieldName": "code",
                "fieldType": "ATTRIBUTE",
                "fieldValue": {
                    "value": tag,
                    "@type": "as.dto.common.search.StringEqualToValue",
                },
            }
            for tag in tags
        ],
    }


def type_criterion(entity_class: str, code: str) -> JsonPayload:
    """Build the sub-criteria matching an entity-type code.

    Args:
        entity_class: Wire-level entity class name in camel case, e.g.
            ``"Sample"``, ``"Experiment"``, ``"DataSet"``.
        code: The type code, e.g. ``"MOLECULE"``.

    Returns:
        The ``<Entity>TypeSearchCriteria`` dict.
    """
    return {
        "@type": (
            f"as.dto.{entity_class.lower()}.search.{entity_class}TypeSearchCriteria"
        ),
        "criteria": [
            {
                "@type": "as.dto.common.search.CodeSearchCriteria",
                "fieldValue": {
                    "value": code.upper(),
                    "@type": "as.dto.common.search.StringEqualToValue",
                },
            }
        ],
    }


def status_criterion(status: str) -> JsonPayload:
    """Build the sub-criteria matching a dataset archiving status.

    Args:
        status: A :data:`~pybis.types.DataSetStatus` value (case-insensitive).

    Returns:
        The ``PhysicalDataSearchCriteria`` dict wrapping the status.

    Raises:
        ValidationError: The status value is not valid.
    """
    status = status.upper()
    valid = (
        "AVAILABLE LOCKED ARCHIVED UNARCHIVE_PENDING ARCHIVE_PENDING BACKUP_PENDING"
    ).split()
    if status not in valid:
        raise ValidationError(
            "status must be one of the following: " + ", ".join(valid)
        )
    return {
        "@type": "as.dto.dataset.search.PhysicalDataSearchCriteria",
        "operator": "AND",
        "criteria": [
            {
                "@type": "as.dto.dataset.search.StatusSearchCriteria",
                "fieldName": "status",
                "fieldType": "ATTRIBUTE",
                "fieldValue": status,
            }
        ],
    }


def _person_criterion(wrapper_type: str, user_id: str) -> JsonPayload:
    return {
        "@type": wrapper_type,
        "criteria": [
            {
                "@type": "as.dto.person.search.UserIdSearchCriteria",
                "fieldName": "userId",
                "fieldType": "ATTRIBUTE",
                "fieldValue": {
                    "value": user_id,
                    "@type": "as.dto.common.search.StringEqualToValue",
                },
            }
        ],
    }


def registrator_criterion(user_id: str) -> JsonPayload:
    """Build the sub-criteria matching the registering user (exact user_id)."""
    return _person_criterion("as.dto.person.search.RegistratorSearchCriteria", user_id)


def modifier_criterion(user_id: str) -> JsonPayload:
    """Build the sub-criteria matching the last modifying user (exact user_id)."""
    return _person_criterion("as.dto.person.search.ModifierSearchCriteria", user_id)


def is_finished_criterion(is_finished: bool) -> JsonPayload:
    """Build the collection ``FINISHED_FLAG`` property criterion."""
    return {
        "@type": "as.dto.common.search.StringPropertySearchCriteria",
        "fieldName": "FINISHED_FLAG",
        "fieldType": "PROPERTY",
        "fieldValue": {
            "value": "true" if is_finished else "false",
            "@type": "as.dto.common.search.StringEqualToValue",
        },
    }


__all__ = [
    "attribute_date_criterion",
    "coerce_filter",
    "explicit_id_criterion",
    "hierarchy_property_criterion",
    "id_criterion",
    "is_finished_criterion",
    "modifier_criterion",
    "property_criterion",
    "registrator_criterion",
    "status_criterion",
    "tags_criterion",
    "type_criterion",
]
