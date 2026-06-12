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
"""Dict-like property access for openBIS entities.

``PropertyBag`` is the public face of entity properties (``entity.props``).
Validation and value coercion stay in the proven 1.x engine
(``PropertyHolder`` + ``PropertyReformatter``) that the bag wraps; the bag
adds the standard :class:`~collections.abc.MutableMapping` protocol and
dirty-marking on write.
"""

from __future__ import annotations

from collections.abc import Iterator, MutableMapping
from typing import Any, Protocol

from ..types.values import PropertyValue


class _LegacyEntity(Protocol):
    """The slice of OpenBisObject that PropertyBag relies on.

    ``Any`` is justified: ``p`` is the untyped 1.x PropertyHolder engine.
    """

    @property
    def p(self) -> Any:
        """The legacy PropertyHolder carrying validation and storage."""
        ...


class PropertyBag(MutableMapping[str, PropertyValue]):
    """Dict-like access to openBIS entity properties; marks entity dirty on write.

    Keys are property codes (case-insensitive, like openBIS itself).  Writes
    are validated against the entity type's property assignments — wrong data
    types or unknown controlled-vocabulary terms raise immediately, not at
    ``save()`` time.

    Example:
        >>> obj.props["FORMULA"]
        'H2O'

        >>> obj.props["FORMULA"] = "D2O"  # entity is now dirty; call save()

        >>> "FORMULA" in obj.props
        True

        >>> obj.props.to_dict()
        {'formula': 'D2O'}
    """

    def __init__(self, entity: _LegacyEntity) -> None:
        """Wrap the property store of an entity.

        Args:
            entity: The entity whose properties this bag exposes.
        """
        self._entity = entity

    # --- internals ----------------------------------------------------------

    @property
    def _holder(self) -> Any:
        """The wrapped legacy PropertyHolder (untyped 1.x engine)."""
        return self._entity.p

    def _set_keys(self) -> list[str]:
        """Codes of all properties that currently hold a value."""
        return [
            key
            for key, value in self._holder.__dict__.items()
            if not key.startswith("_") and value is not None
        ]

    def _is_known(self, key: str) -> bool:
        """True if the property is defined by the type or currently set."""
        lower = key.lower()
        return lower in self._holder._property_names or lower in self._holder.__dict__

    # --- MutableMapping interface ---------------------------------------------

    def __getitem__(self, key: str) -> PropertyValue:
        """Return the value of a property.

        Raises:
            KeyError: The property is not defined for this entity type and
                holds no value.
        """
        if not self._is_known(key):
            raise KeyError(key)
        value: PropertyValue = self._holder.__dict__.get(key.lower())
        return value

    def __setitem__(self, key: str, value: PropertyValue) -> None:
        """Set a property value (validated) and mark the entity dirty.

        Raises:
            ValueError: The value does not match the property's data type,
                or the property is not assigned to the entity type.
        """
        setattr(self._holder, key.lower(), value)
        self._entity.__dict__["_dirty"] = True

    def __delitem__(self, key: str) -> None:
        """Clear a property (the value is removed on the next ``save()``).

        Raises:
            KeyError: The property holds no value.
        """
        if key.lower() not in self._holder.__dict__:
            raise KeyError(key)
        self._holder.__dict__[key.lower()] = None
        self._entity.__dict__["_dirty"] = True

    def __iter__(self) -> Iterator[str]:
        """Iterate over the codes of all set properties."""
        return iter(self._set_keys())

    def __len__(self) -> int:
        """Number of properties that currently hold a value."""
        return len(self._set_keys())

    def __contains__(self, key: object) -> bool:
        """True if the property currently holds a value."""
        if not isinstance(key, str):
            return False
        return self._holder.__dict__.get(key.lower()) is not None

    # --- conveniences -----------------------------------------------------------

    def to_dict(self) -> dict[str, PropertyValue]:
        """Return a plain-dict copy of all set properties."""
        return {key: self._holder.__dict__[key] for key in self._set_keys()}

    def __repr__(self) -> str:
        """Show the set properties like a dict."""
        return f"PropertyBag({self.to_dict()!r})"

    # --- legacy attribute-style compatibility -------------------------------------
    #
    # pybis 1.x exposed properties attribute-style (sample.props.name = "x").
    # Reads and writes of non-underscore attributes are routed through the
    # mapping interface so legacy code keeps working (and keeps dirty-marking).

    def __getattr__(self, name: str) -> Any:
        """Delegate unknown attribute reads to the wrapped holder (1.x style)."""
        if name.startswith("_"):
            raise AttributeError(name)
        return getattr(self._holder, name)

    def __setattr__(self, name: str, value: Any) -> None:
        """Route attribute-style property writes through the mapping interface."""
        if name.startswith("_"):
            object.__setattr__(self, name, value)
        else:
            self[name] = value


__all__ = ["PropertyBag"]
