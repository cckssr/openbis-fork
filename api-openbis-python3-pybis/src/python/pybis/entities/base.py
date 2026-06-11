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
"""Shared behavior of all openBIS entities: identity, equality, properties.

``EntityBehavior`` is mixed into the legacy ``OpenBisObject`` base so every
entity — old-style or new — gains perm_id-based equality/hashing, the
``props`` PropertyBag, and dirty tracking.  The typed per-entity classes of
the v2 API build on top of this.
"""

from __future__ import annotations

from typing import Any, cast

from ._properties import PropertyBag, _LegacyEntity


class EntityBehavior:
    """Mixin adding the v2 entity contract to entity classes.

    - ``props``: dict-like, validated property access (:class:`PropertyBag`)
    - ``__eq__`` / ``__hash__``: based on ``perm_id``, so saved entities can
      live in sets and dict keys; unsaved entities fall back to identity
    - ``is_dirty``: True when properties changed since the last ``save()``
    """

    def _perm_id_value(self) -> str | None:
        """The perm_id string, or None for unsaved entities."""
        # AttrHolder stores the raw dict; its __getattr__ extracts the string.
        perm_id: Any = getattr(self, "permId", None)
        if isinstance(perm_id, dict):
            perm_id = perm_id.get("permId")
        if not perm_id:  # legacy code reports "" for unsaved entities
            return None
        return str(perm_id)

    @property
    def perm_id(self) -> str | None:
        """The server-assigned permanent id, or None before the first save."""
        return self._perm_id_value()

    @property
    def props(self) -> PropertyBag:
        """Dict-like access to the entity's properties."""
        # The concrete entity classes provide the legacy `p` holder at
        # runtime; this mixin cannot declare it without owning construction.
        return PropertyBag(cast(_LegacyEntity, self))

    @property
    def is_dirty(self) -> bool:
        """True when the entity has unsaved property changes."""
        return bool(self.__dict__.get("_dirty", False))

    def _mark_clean(self) -> None:
        """Reset the dirty flag (called after a successful save)."""
        self.__dict__["_dirty"] = False

    def __eq__(self, other: object) -> bool:
        """Entities are equal when they have the same perm_id.

        Unsaved entities (no perm_id yet) are only equal to themselves.
        """
        if not isinstance(other, EntityBehavior):
            return NotImplemented
        if type(self) is not type(other):
            return NotImplemented
        mine, theirs = self._perm_id_value(), other._perm_id_value()
        if mine is None or theirs is None:
            return self is other
        return mine == theirs

    def __hash__(self) -> int:
        """Hash by perm_id (or identity for unsaved entities)."""
        perm_id = self._perm_id_value()
        if perm_id is None:
            return object.__hash__(self)
        return hash((type(self).__name__, perm_id))


__all__ = ["EntityBehavior"]
