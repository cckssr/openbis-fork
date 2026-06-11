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
"""Python-side value types for openBIS entity properties.

A property value travels in two directions: Python objects are coerced to
the JSON-RPC wire format on write, and wire values are deserialized back to
the types below on read (see ``pybis.entities`` ``PropertyBag``).  The unions
here describe the Python side of that round trip.
"""

from __future__ import annotations

from datetime import date, datetime
from typing import Any, TypeAlias

ScalarPropertyValue: TypeAlias = str | int | float | bool | datetime | date | None
"""Value of a scalar property (``VARCHAR``, ``INTEGER``, ``TIMESTAMP``, ...)."""

ArrayPropertyValue: TypeAlias = (
    list[int]  # ARRAY_INTEGER
    | list[float]  # ARRAY_REAL
    | list[str]  # ARRAY_STRING
    | list[datetime]  # ARRAY_TIMESTAMP
)
"""Value of an array property (``ARRAY_*`` data types)."""

# Any is unavoidable here: the JSON data type carries arbitrary user-defined
# JSON documents whose shape openBIS does not constrain.
JsonPropertyValue: TypeAlias = dict[str, Any] | list[Any]
"""Value of a ``JSON`` property — any JSON-serializable document."""

PropertyValue: TypeAlias = ScalarPropertyValue | ArrayPropertyValue | JsonPropertyValue
"""Union of all possible Python-side property values."""

__all__ = [
    "ArrayPropertyValue",
    "JsonPropertyValue",
    "PropertyValue",
    "ScalarPropertyValue",
]
