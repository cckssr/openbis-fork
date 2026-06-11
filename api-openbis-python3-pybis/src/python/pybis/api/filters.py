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
"""Typed property filters for ``search_*`` methods.

Filters replace the magic string operators of pybis 1.x (``">= 5"``,
``"*foo*"``).  A plain ``str`` value in a ``properties`` dict still means
exact match; everything else is expressed with the factory functions below.

Example:
    >>> from pybis.api.filters import contains, gte, parent_prop, eq

    >>> client.search_objects(
    ...     properties={"FORMULA": contains("H2O"), "ATOMS": gte(3)},
    ...     hierarchy_properties=[parent_prop("BATCH_ID", eq("BATCH-001"))],
    ... )
"""

from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime
from typing import Literal, TypeAlias

FilterOperator: TypeAlias = Literal[
    "eq",
    "contains",
    "starts_with",
    "ends_with",
    "gt",
    "gte",
    "lt",
    "lte",
    "number_eq",
    "date_eq",
    "date_after",
    "date_before",
    "any",
]
"""Operators supported by :class:`PropertyFilter`."""

# Internal mapping to openBIS @type strings.
_OP_TO_RPC: dict[str, str] = {
    "eq": "as.dto.common.search.StringEqualToValue",
    "contains": "as.dto.common.search.StringContainsValue",
    "starts_with": "as.dto.common.search.StringStartsWithValue",
    "ends_with": "as.dto.common.search.StringEndsWithValue",
    "gt": "as.dto.common.search.NumberGreaterThanValue",
    "gte": "as.dto.common.search.NumberGreaterThanOrEqualToValue",
    "lt": "as.dto.common.search.NumberLessThanValue",
    "lte": "as.dto.common.search.NumberLessThanOrEqualToValue",
    "number_eq": "as.dto.common.search.NumberEqualToValue",
    "date_eq": "as.dto.common.search.DateEqualToValue",
    "date_after": "as.dto.common.search.DateLaterThanOrEqualToValue",
    "date_before": "as.dto.common.search.DateEarlierThanOrEqualToValue",
    "any": "as.dto.common.search.AnyStringValue",
}


@dataclass(frozen=True)
class PropertyFilter:
    """Typed filter for a single property value.

    Do not instantiate directly; use the factory functions of this module
    (:func:`eq`, :func:`contains`, :func:`gt`, :func:`date_after`, ...).

    Attributes:
        operator (FilterOperator): The comparison operator.
        value (str | int | float | None): The comparison value; ``None`` for
            operators without a value (``"any"``).
        use_wildcards (bool): Interpret ``*`` and ``?`` in ``value`` as
            wildcards (string operators only).
    """

    operator: FilterOperator
    value: str | int | float | None = None
    use_wildcards: bool = False

    def to_rpc(self) -> dict[str, str | int | float | bool]:
        """Serialize to an openBIS JSON-RPC search value dict.

        Returns:
            The ``@type``-tagged dict embedded into a property search
            criterion.
        """
        base: dict[str, str | int | float | bool] = {
            "@type": _OP_TO_RPC[self.operator]
        }
        if self.value is not None:
            base["value"] = self.value
        if self.use_wildcards:
            base["useWildcards"] = True
        return base


@dataclass(frozen=True)
class HierarchyPropertyFilter:
    """Filter on a property of a parent, child, or container entity.

    Build instances with :func:`parent_prop`, :func:`child_prop`, or
    :func:`container_prop`.

    Attributes:
        relation (Literal["parent", "child", "container"]): Which related
            entity the property belongs to.
        property_code (str): Code of the property on the related entity.
        filter (PropertyFilter): The condition the property must satisfy.
    """

    relation: Literal["parent", "child", "container"]
    property_code: str
    filter: PropertyFilter


# --- Public factory functions -------------------------------------------------


def eq(value: str) -> PropertyFilter:
    """Exact string match."""
    return PropertyFilter("eq", value)


def contains(value: str) -> PropertyFilter:
    """Property value contains the given substring."""
    return PropertyFilter("contains", value)


def starts_with(value: str) -> PropertyFilter:
    """Property value starts with the given prefix."""
    return PropertyFilter("starts_with", value)


def ends_with(value: str) -> PropertyFilter:
    """Property value ends with the given suffix."""
    return PropertyFilter("ends_with", value)


def wildcard(pattern: str) -> PropertyFilter:
    """Wildcard pattern using ``*`` (any sequence) and ``?`` (single char)."""
    return PropertyFilter("eq", pattern, use_wildcards=True)


def gt(value: int | float) -> PropertyFilter:
    """Numeric greater-than."""
    return PropertyFilter("gt", value)


def gte(value: int | float) -> PropertyFilter:
    """Numeric greater-than-or-equal."""
    return PropertyFilter("gte", value)


def lt(value: int | float) -> PropertyFilter:
    """Numeric less-than."""
    return PropertyFilter("lt", value)


def lte(value: int | float) -> PropertyFilter:
    """Numeric less-than-or-equal."""
    return PropertyFilter("lte", value)


def number_eq(value: int | float) -> PropertyFilter:
    """Exact numeric match."""
    return PropertyFilter("number_eq", value)


def date_eq(value: datetime | str) -> PropertyFilter:
    """Exact date match. Accepts a datetime or an ISO date string."""
    return PropertyFilter("date_eq", _coerce_date(value))


def date_after(value: datetime | str) -> PropertyFilter:
    """Date on or after the given value."""
    return PropertyFilter("date_after", _coerce_date(value))


def date_before(value: datetime | str) -> PropertyFilter:
    """Date on or before the given value."""
    return PropertyFilter("date_before", _coerce_date(value))


def any_value() -> PropertyFilter:
    """Matches any non-null property value (property-exists check)."""
    return PropertyFilter("any")


def parent_prop(code: str, f: PropertyFilter) -> HierarchyPropertyFilter:
    """Filter on a property of a parent entity."""
    return HierarchyPropertyFilter("parent", code, f)


def child_prop(code: str, f: PropertyFilter) -> HierarchyPropertyFilter:
    """Filter on a property of a child entity."""
    return HierarchyPropertyFilter("child", code, f)


def container_prop(code: str, f: PropertyFilter) -> HierarchyPropertyFilter:
    """Filter on a property of a container entity."""
    return HierarchyPropertyFilter("container", code, f)


def _coerce_date(value: datetime | str) -> str:
    """Normalize a datetime to the openBIS timestamp string format."""
    if isinstance(value, datetime):
        return value.strftime("%Y-%m-%d %H:%M:%S")
    return value


__all__ = [
    "FilterOperator",
    "HierarchyPropertyFilter",
    "PropertyFilter",
    "any_value",
    "child_prop",
    "container_prop",
    "contains",
    "date_after",
    "date_before",
    "date_eq",
    "ends_with",
    "eq",
    "gt",
    "gte",
    "lt",
    "lte",
    "number_eq",
    "parent_prop",
    "starts_with",
    "wildcard",
]
