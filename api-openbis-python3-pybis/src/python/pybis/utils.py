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
"""Small helpers: value checks and ``extract_*`` response-flattening functions."""

import math
import re
from datetime import datetime
from typing import Any, Callable, Optional

# moved to the transport layer; re-exported here for legacy importers
from .api.rpc import assign_jackson_ids as assign_jackson_ids  # noqa: F401
from .api.rpc import parse_jackson as parse_jackson  # noqa: F401

# display messages when in a interactive context (IPython or Jupyter)
try:
    get_ipython()  # type: ignore[name-defined]  # reason: defined only inside IPython
except Exception:
    VERBOSE = False
else:
    VERBOSE = True


def check_datatype(
    type_name: Optional[str], value: Any, is_multi_value: bool = False
) -> bool:
    """Check whether ``value`` matches an openBIS property data type.

    Args:
        type_name: The openBIS data type, e.g. ``"INTEGER"`` or ``"VARCHAR"``.
        value: The value (or list of values) to check.
        is_multi_value: Treat ``value`` as a list of values.

    Returns:
        True when the value is acceptable for the data type (unknown types
        are accepted).
    """
    if is_multi_value:
        if type_name == "INTEGER":
            return all([isinstance(x, int) and not math.isnan(x) for x in value])
        if type_name == "REAL":
            return all(
                [isinstance(x, (int, float)) and not math.isnan(x) for x in value]
            )
        if type_name == "BOOLEAN":
            return all([isinstance(x, bool) for x in value])
        if type_name == "VARCHAR":
            return all([isinstance(x, str) for x in value])
    else:
        if type_name == "INTEGER":
            return isinstance(value, int) and not math.isnan(value)
        if type_name == "REAL":
            return isinstance(value, (int, float)) and not math.isnan(value)
        if type_name == "BOOLEAN":
            return isinstance(value, bool)
        if type_name == "VARCHAR":
            return isinstance(value, str)
        if type_name is not None and type_name.startswith("ARRAY"):
            return isinstance(value, list)
    return True


def split_identifier(ident: str) -> "dict[str, str]":
    """Split a path-style identifier into space, experiment, and code parts.

    Args:
        ident: An identifier such as ``"/SPACE/EXPERIMENT/CODE"``.

    Returns:
        A dict with the recognized parts (``space``, ``experiment``,
        ``code``); missing parts are simply absent.
    """
    parts = ident.upper().split("/")
    results: dict[str, str] = {}
    try:
        if parts[0] == "":
            parts.pop(0)
        if parts[-1] == "":
            parts.pop(-1)
        results["space"] = parts.pop(0)
        results["code"] = parts.pop(-1)
        results["experiment"] = parts.pop(0)
    except Exception:
        pass

    return results


def format_timestamp(ts: "float | None") -> str:
    """Format a millisecond epoch timestamp as ``YYYY-MM-DD HH:MM:SS``."""
    if ts is None:
        return ""
    if ts != ts:  # test for NaN
        return ""
    return datetime.fromtimestamp(round(ts / 1000)).strftime("%Y-%m-%d %H:%M:%S")


def is_identifier(ident: str) -> bool:
    """Check whether the string looks like a path-style identifier."""
    # assume we got a sample identifier e.g. /TEST/TEST-SAMPLE
    return bool(re.search("/", ident))


def is_permid(ident: str) -> bool:
    """Check whether the string looks like a permId (``digits-digits``)."""
    return bool(re.match(r"^\d+\-\d+$", ident))


def nvl(val: Any, string: Any = "") -> Any:
    """Return ``val``, or ``string`` when ``val`` is None."""
    if val is None:
        return string
    return val


def extract_permid(permid: object) -> str:
    """Extract the permId string from a permId dict (or stringify)."""
    if not isinstance(permid, dict):
        return str(permid)
    return str(permid["permId"])


def extract_data_type(obj: Any) -> Any:
    """Extract the ``dataType`` field (empty string for None)."""
    if not isinstance(obj, dict):
        return "" if obj is None else str(obj)
    return "" if obj["dataType"] is None else obj["dataType"]


def extract(obj: Any, property_name: str) -> Any:
    """Extract an arbitrary field from a dict (empty string for None)."""
    if not isinstance(obj, dict):
        return "" if obj is None else str(obj)
    return "" if obj[property_name] is None else obj[property_name]


def extract_code(obj: object) -> str:
    """Extract the ``code`` field (empty string for None)."""
    if not isinstance(obj, dict):
        return "" if obj is None else str(obj)
    return "" if obj["code"] is None else str(obj["code"])


def extract_downloadUrl(obj: Any) -> Any:
    """Extract the ``downloadUrl`` field (empty string for None)."""
    if not isinstance(obj, dict):
        return "" if obj is None else str(obj)
    return "" if obj["downloadUrl"] is None else obj["downloadUrl"]


def extract_name(obj: Any) -> Any:
    """Extract the ``name`` field (empty string for None)."""
    if not isinstance(obj, dict):
        return "" if obj is None else str(obj)
    return "" if obj["name"] is None else obj["name"]


def extract_deletion(obj: Any) -> "list[dict[str, Any]]":
    """Flatten one deletion record into per-deleted-object dicts."""
    del_objs = []
    for deleted_object in obj["deletedObjects"]:
        del_objs.append(
            {
                "reason": obj["reason"],
                "permId": deleted_object["id"]["permId"],
                "type": deleted_object["id"]["@type"],
                "deletionId": obj["id"]["id"],
            }
        )
    return del_objs


def extract_attr(attr: str) -> "Callable[[Any], Any]":
    """Build an extractor for an arbitrary attribute key."""

    def attr_func(obj: Any) -> Any:
        if isinstance(obj, dict):
            return obj.get(attr, "")
        else:
            return str(obj)

    return attr_func


def extract_identifier(ident: object) -> object:
    """Extract the ``identifier`` field (empty string for None)."""
    if not isinstance(ident, dict):
        return "" if ident is None else str(ident)
    return "" if ident["identifier"] is None else ident["identifier"]


def extract_identifiers(items: Any) -> "list[Any]":
    """Extract identifier (or permId) strings from a list of entity dicts."""
    if not items:
        return []
    try:
        return [
            data["identifier"]["identifier"]
            if "identifier" in data
            else data["permId"]["permId"]
            for data in items
        ]
    except TypeError:
        return []


def extract_nested_identifier(ident: object) -> object:
    """Extract a nested ``identifier.identifier`` field."""
    if not isinstance(ident, dict):
        return "" if ident is None else str(ident)
    return (
        ""
        if ident["identifier"]["identifier"] is None
        else ident["identifier"]["identifier"]
    )


def extract_nested_permid(permid: object) -> object:
    """Extract a nested ``permId.permId`` field."""
    if not isinstance(permid, dict):
        return "" if permid is None else str(permid)
    return "" if permid["permId"]["permId"] is None else permid["permId"]["permId"]


def extract_nested_permids(items: Any) -> "list[Any]":
    """Extract nested ``permId.permId`` fields from a list."""
    if not isinstance(items, list):
        return []

    return list(item["permId"]["permId"] for item in items)


def extract_property_assignments(pas: Any) -> "list[Any]":
    """Extract the property-type labels of property assignments."""
    pa_strings = []
    for pa in pas:
        if not isinstance(pa["propertyType"], dict):
            pa_strings.append(pa["propertyType"])
        else:
            pa_strings.append(pa["propertyType"]["label"])
    return pa_strings


def extract_role_assignments(ras: Any) -> "list[dict[str, Any]]":
    """Extract role, level, and space of role assignments."""
    ra_strings = []
    for ra in ras:
        ra_strings.append(
            {
                "role": ra["role"],
                "roleLevel": ra["roleLevel"],
                "space": ra["space"]["code"] if ra["space"] else None,
            }
        )
    return ra_strings


def extract_person(person: object) -> str:
    """Extract the ``userId`` of a person dict (or stringify)."""
    if not isinstance(person, dict):
        if person is None:
            return ""
        else:
            return str(person)
    return str(person["userId"])


def extract_person_details(person: Any) -> str:
    """Format a person dict as ``First Last <email>``."""
    if not isinstance(person, dict):
        return str(person)
    return "{firstName} {lastName} <{email}>".format(**person)


def extract_id(id: Any) -> Any:
    """Extract the ``techId`` field (or stringify)."""
    if not isinstance(id, dict):
        return str(id)
    else:
        return id["techId"]


def extract_userId(user: object) -> str:
    """Extract user id(s) from a person dict, list of dicts, or string."""
    if isinstance(user, list):
        return ", ".join([u["userId"] for u in user])
    elif isinstance(user, dict):
        return "" if user["userId"] is None else str(user["userId"])
    else:
        return "" if user is None else str(user)


def is_number(value: str) -> "Optional[re.Match[str]]":
    """Detect whether a given value is an integer or floating point number.

    Matches ``1``, ``2``, ``1.0``, ``.5``, etc.
    """
    number_regex = re.compile(r"^(?=.)([+-]?([0-9]*)(\.([0-9]+))?)$")
    return number_regex.search(value)


def extract_username_from_token(token: str) -> "Optional[str]":
    """Extract the user name from a session token or PAT, or None."""
    if token.startswith("$pat"):
        match = re.search(r"^\$pat-(?P<username>.*?)-.*", token)
    else:
        match = re.search(r"(?P<username>.*?)-.*", token)
    if match:
        return match.groupdict().get("username")
    return None
