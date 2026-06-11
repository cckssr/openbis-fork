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
"""Automatic classification of openBIS identifying strings.

Every ``get_*`` single-entity method accepts one string and auto-detects
whether it is a perm_id (``"20240101000000000-1"``), a path-style identifier
(``"/SPACE/PROJECT/CODE"``), or a bare code (``"MY-OBJECT"``) — callers never
have to pick the right keyword themselves.

The rules deliberately match the behavior of pybis 1.x
(``is_identifier`` / ``is_permid``):

- a ``/`` anywhere makes it an identifier (the leading slash may be omitted,
  which 1.x tolerated and normalized),
- ``<digits>-<digits>`` is a perm_id,
- anything else is a code.
"""

from __future__ import annotations

import re
from typing import Literal, NamedTuple, TypeAlias

IdentifierKind: TypeAlias = Literal["perm_id", "identifier", "code"]
"""The three kinds of identifying strings accepted by openBIS."""

# pybis 1.x compatible: any digits-dash-digits string is a perm_id (server
# perm_ids are timestamp-based, e.g. 20240101000000000-1).
_PERM_ID_RE = re.compile(r"^\d+-\d+$")


def classify_id(value: str) -> IdentifierKind:
    """Classify a string as a perm_id, full identifier, or bare code.

    Rules (in order):

    - contains ``/``                  → ``"identifier"`` (``/SPACE/PROJECT/CODE``)
    - matches ``<digits>-<digits>``   → ``"perm_id"`` (``20240101000000000-1``)
    - anything else                   → ``"code"`` (``MY-OBJECT``)

    Args:
        value: The identifying string; surrounding whitespace is ignored.

    Returns:
        The detected identifier kind.
    """
    s = value.strip()
    if "/" in s:
        return "identifier"
    if _PERM_ID_RE.match(s):
        return "perm_id"
    return "code"


class ClassifiedId(NamedTuple):
    """An identifying string together with its detected kind.

    Attributes:
        value (str): The stripped (and, for identifiers, slash-normalized)
            identifying string.
        kind (IdentifierKind): The detected kind.
    """

    value: str
    kind: IdentifierKind


def classify(value: str) -> ClassifiedId:
    """Classify and normalize an identifying string.

    Identifiers get the leading ``/`` restored when omitted (1.x tolerated
    ``"SPACE/CODE"``).

    Args:
        value: The identifying string.

    Returns:
        The normalized value and its kind.
    """
    s = value.strip()
    kind = classify_id(s)
    if kind == "identifier" and not s.startswith("/"):
        s = "/" + s
    return ClassifiedId(value=s, kind=kind)


__all__ = ["ClassifiedId", "IdentifierKind", "classify", "classify_id"]
