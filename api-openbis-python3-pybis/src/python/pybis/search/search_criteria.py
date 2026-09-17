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
"""
Proposed `pybis/search_criteria.py`
===================================

Replacement for the `where=` / `**properties` handling in
`Openbis.get_samples()` (and the identical code in get_experiments /
get_datasets).

Design constraints
------------------
* Purely client-side: emits the same JSON-RPC search-criteria DTOs the
  V3 API already understands. No server change needed.
* Backward compatible: every ``where`` value that works today produces
  byte-identical JSON.
* Composition is expressed by *nesting* an entity search criteria with
  its own ``operator``, which is the pattern pybis already uses in
  ``_subcriteria_for_code_new(..., operator="OR")``.

Integration points in pybis.py
------------------------------
1. ``_subcriteria_for_properties`` -> ``build_property_criteria`` (below),
   which returns a *list* of criteria dicts instead of a single dict.
2. In ``get_samples`` / ``get_experiments`` / ``get_datasets``::

       for prop, value in properties.items():
           sub_criteria += build_property_criteria(prop, value, entity="sample")

3. ``where`` gains a second accepted shape: a *list of (prop, value) pairs*,
   so the same property can legally appear more than once even without the
   Q-object sugar.
"""

from __future__ import annotations

import datetime
import numbers
import re

# ---------------------------------------------------------------------------
# operator tables
#
# The strict date operators are the fix for a real bug: pybis 1.37.5 maps
# both ">" and ">=" to DateLaterThanOrEqualToValue, and both "<" and "<=" to
# DateEarlierThanOrEqualToValue, so `"> 2021-01-01"` silently includes
# 2021-01-01. DateLaterThanValue / DateEarlierThanValue exist in the V3 DTOs.
# ---------------------------------------------------------------------------

_PREFIX = "as.dto.common.search."

DATE_OPS = {
    "==": _PREFIX + "DateEqualToValue",
    "=": _PREFIX + "DateEqualToValue",
    ">": _PREFIX + "DateLaterThanValue",
    ">=": _PREFIX + "DateLaterThanOrEqualToValue",
    "<": _PREFIX + "DateEarlierThanValue",
    "<=": _PREFIX + "DateEarlierThanOrEqualToValue",
}

NUMBER_OPS = {
    "==": _PREFIX + "NumberEqualToValue",
    "=": _PREFIX + "NumberEqualToValue",
    ">": _PREFIX + "NumberGreaterThanValue",
    ">=": _PREFIX + "NumberGreaterThanOrEqualToValue",
    "<": _PREFIX + "NumberLessThanValue",
    "<=": _PREFIX + "NumberLessThanOrEqualToValue",
}

STRING_OPS = {
    "==": _PREFIX + "StringEqualToValue",
    "=": _PREFIX + "StringEqualToValue",
    ">": _PREFIX + "StringGreaterThanValue",
    ">=": _PREFIX + "StringGreaterThanOrEqualToValue",
    "<": _PREFIX + "StringLessThanValue",
    "<=": _PREFIX + "StringLessThanOrEqualToValue",
    "contains": _PREFIX + "StringContainsValue",
    "startswith": _PREFIX + "StringStartsWithValue",
    "endswith": _PREFIX + "StringEndsWithValue",
}

ENTITY_SEARCH_TYPE = {
    "sample": "as.dto.sample.search.SampleSearchCriteria",
    "dataset": "as.dto.dataset.search.DataSetSearchCriteria",
    "experiment": "as.dto.experiment.search.ExperimentSearchCriteria",
}

RELATION_SEARCH_TYPE = {
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

_COMPARATOR_RE = re.compile(r"^\s*(>=|<=|==|!=|>|<|=)\s*(.*)$", re.S)
_ISO_DATE_RE = re.compile(r"^\d{4}-\d{2}-\d{2}([ T]\d{2}:\d{2}(:\d{2})?)?$")
_NUMBER_RE = re.compile(r"^[+-]?(\d+)(\.\d+)?$")


# ---------------------------------------------------------------------------
# public value wrappers ("Q objects")
#
# These are the only new user-facing vocabulary. Everything else is inferred.
# ---------------------------------------------------------------------------


class Predicate:
    """Base class. Subclasses render to a list of criteria dicts."""

    def render(self, prop, entity, ctx):  # pragma: no cover - interface
        raise NotImplementedError


class Cmp(Predicate):
    """A single comparison, e.g. ``Cmp(">=", "2021-01-01")``."""

    def __init__(self, op, value):
        if op not in ("!=",) and op not in STRING_OPS and op not in DATE_OPS:
            raise ValueError(f"unsupported operator: {op!r}")
        self.op = op
        self.value = value

    def render(self, prop, entity, ctx):
        if self.op == "!=":
            return Not(Cmp("==", self.value)).render(prop, entity, ctx)
        return [_leaf(prop, self.op, self.value, entity, ctx)]




class All(Predicate):
    """AND over several predicates on the *same* property."""

    def __init__(self, *predicates):
        self.predicates = [_coerce(p) for p in predicates]

    def render(self, prop, entity, ctx):
        child = dict(ctx, operator="AND")
        criteria = []
        for p in self.predicates:
            criteria += p.render(prop, entity, child)
        # An AND branch can be spliced straight into an enclosing AND list;
        # inside an OR it must be wrapped or the grouping is lost.
        # Exception: relation criteria (parent_/child_/container_) must be
        # merged into ONE wrapper, otherwise two constraints become
        # "a parent matching A" AND "a parent matching B" -- possibly two
        # different parents -- instead of "one parent matching A and B".
        if (
            ctx.get("operator", "AND") == "AND"
            and not ctx.get("wrap_all")
            and not _all_same_relation(criteria, entity)
        ):
            return criteria
        return [_composite(criteria, "AND", entity, ctx)]


class Any(Predicate):
    """OR over several predicates on the same property. ``In`` is sugar for this."""

    def __init__(self, *predicates):
        self.predicates = [_coerce(p) for p in predicates]

    def render(self, prop, entity, ctx):
        child = dict(ctx, operator="OR")
        criteria = []
        for p in self.predicates:
            criteria += p.render(prop, entity, child)
        if len(criteria) == 1:
            return criteria
        return [_composite(criteria, "OR", entity, ctx)]


class Not(Predicate):
    """
    Negation.

    The ``"negated": true`` flag does NOT cascade: openBIS applies it only to
    the criteria that directly holds the field criteria being negated, and
    ignores it on any composite above that. So the flag goes on the INNER
    composite, and that composite is then wrapped in a plain (non-negated)
    one:

        SampleSearchCriteria (AND)              <- outer wrapper, no flag
          SampleSearchCriteria (AND, negated)   <- the flag belongs here
            NumberPropertySearchCriteria >= 15
            NumberPropertySearchCriteria <= 25

    Putting the flag on the outer wrapper instead is accepted by the server
    and silently returns the UN-negated result set. Do not "simplify" this by
    dropping a level or moving the flag up -- test_negation_flag_sits_on_the_
    inner_composite pins the exact shape.
    """

    def __init__(self, predicate):
        self.predicate = _coerce(predicate)

    def render(self, prop, entity, ctx):
        rendered = self.predicate.render(
            prop, entity, dict(ctx, operator="AND", wrap_all=True)
        )
        if len(rendered) == 1 and rendered[0].get("@type") == ENTITY_SEARCH_TYPE[entity]:
            negated = dict(rendered[0])  # already the composite holding the leaves
        else:
            negated = _composite(rendered, "AND", entity, ctx)
        negated["negated"] = True
        return [_composite([negated], "AND", entity, ctx)]


class IsSet(Predicate):
    """
    The property has a value.

    There is no "is not null" criteria in the V3 DTOs, so this is a wildcard
    match on any value: ``StringEqualToValue("*")`` with ``useWildcards``.
    Deliberately NOT routed through _classify() -- the criteria must be a
    StringPropertySearchCriteria whatever the property's declared data type,
    because "*" is not a number or a date.

    Two things this cannot settle from the client side:

    * whether a property explicitly set to the empty string counts as set
    * whether a String criteria matches against a non-VARCHAR property
      (INTEGER, REAL, TIMESTAMP) on your server

    TestPropertyPresence covers both against a live instance.
    """

    def render(self, prop, entity, ctx):
        relation, prop_name = _split_relation(prop)
        leaf = {
            "@type": _PREFIX + "StringPropertySearchCriteria",
            "fieldName": _field_name(prop_name),
            "fieldType": "PROPERTY",
            "fieldValue": {"@type": "as.dto.common.search.AnyStringValue", "value": ""},
        }
        if relation:
            return [
                {
                    "@type": RELATION_SEARCH_TYPE[entity][relation],
                    "criteria": [leaf],
                }
            ]
        return [leaf]


def NotSet():
    """The property has no value. Shorthand for ``Not(IsSet())``."""
    return Not(IsSet())


def In(values):
    """``In(['a', 'b', 'c'])`` -> prop == a OR prop == b OR prop == c."""
    return Any(*[Cmp("==", v) for v in values])


def Between(low, high, inclusive=(True, True)):
    """Closed by default: ``Between('2021-01-01', '2021-02-01')``."""
    lo_op = ">=" if inclusive[0] else ">"
    hi_op = "<=" if inclusive[1] else "<"
    return All(Cmp(lo_op, low), Cmp(hi_op, high))


def Gt(v):
    return Cmp(">", v)


def Ge(v):
    return Cmp(">=", v)


def Lt(v):
    return Cmp("<", v)


def Le(v):
    return Cmp("<=", v)


def Eq(v):
    return Cmp("==", v)


def Ne(v):
    """Shorthand for Not(Eq(v))."""
    return Not(Cmp("==", v))


def Contains(v):
    return Cmp("contains", v)


# ---------------------------------------------------------------------------
# coercion: turn plain Python values into predicates
# ---------------------------------------------------------------------------


_SCALAR_TYPES = (str, numbers.Number, datetime.date, datetime.datetime)


def _reject(prop, value):
    """
    Raise on a value we cannot interpret, instead of falling back to str().

    The old fallback stringified anything, so a mis-imported helper travelled
    all the way to the server as
    ``StringEqualToValue("<...Cmp object at 0x...>")`` and came back as
    "String does not represent a number". Failing here instead names the cause.
    """
    where = f"`where` value for {prop!r}" if prop else "`where` value"

    if callable(getattr(value, "render", None)) and type(value).__name__ in (
        "Predicate",
        "Cmp",
        "All",
        "Any",
        "Not",
    ):
        raise TypeError(
            f"{where} is a {type(value).__name__} from "
            f"{type(value).__module__!r}, which is a DIFFERENT module object "
            f"than {Predicate.__module__!r}. Two copies of search_criteria are "
            "loaded, so isinstance() fails and the predicate is not "
            "recognised. Import the helpers from the same place pybis does, "
            f"e.g. `from {Predicate.__module__} import ...`."
        )

    if value is None:
        raise TypeError(
            f"{where} is None. openBIS has no null comparison; omit the key "
            "instead, or search for the empty string explicitly."
        )

    raise TypeError(
        f"{where} has unsupported type {type(value).__name__}. Expected a "
        "string, number, date/datetime, list, operator dict, or one of the "
        "search helpers (Between, In, Any, All, Not, ...)."
    )


def _coerce(value, prop=None):
    """
    Map a raw ``where`` value onto a Predicate.

    scalar str/num/date  -> Cmp, honouring an embedded comparator prefix
                            (">= 2021-01-01") for backward compatibility
    list / tuple / set   -> In(...)          (currently silently broken)
    dict {op: value}     -> All(Cmp(op, v))  (range on one property)
    Predicate            -> itself
    """
    if isinstance(value, Predicate):
        return value
    if isinstance(value, (list, tuple, set, frozenset)):
        if not value:
            raise ValueError(
                "empty value list in `where`: openBIS treats a composite "
                "criteria with no sub-criteria as unconstrained, so this "
                "would return EVERYTHING rather than nothing. Decide "
                "explicitly in the caller, e.g. "
                "`samples = o.get_samples(where={prop: values}) if values else []`."
            )
        return In(list(value))
    if isinstance(value, dict):
        if not value:
            raise ValueError(
                "empty operator dict in `where`: this would place no "
                "constraint on the property at all. Omit the key instead."
            )
        return All(*[Cmp(op, v) for op, v in value.items()])
    if isinstance(value, str) and value.lstrip()[:1] in ("<", ">", "="):
        # same trigger condition as pybis 1.37.5, so existing calls are unchanged
        match = _COMPARATOR_RE.match(value)
        if match:
            op, rest = match.groups()
            return Cmp(op, rest.strip())
    if not isinstance(value, _SCALAR_TYPES):
        _reject(prop, value)
    return Cmp("==", value)


# ---------------------------------------------------------------------------
# leaf + composite construction
# ---------------------------------------------------------------------------


def _classify(prop, value, ctx):
    """
    Decide DATE / NUMBER / STRING for one leaf.

    Improvements over 1.37.5:
      * a real datetime/date object is always a date, whatever the prop is named
      * an ISO timestamp value is treated as a date even if the property is not
        called "*date*" (the current `"date" in prop.lower()` heuristic misses
        e.g. HARVESTED_ON)
      * the caller can force it via ctx["type"], populated from the sample-type
        property assignments when they are already cached.
    """
    forced = ctx.get("type")
    if forced:
        return forced
    if isinstance(value, (datetime.date, datetime.datetime)):
        return "DATE"
    text = str(value)
    if _ISO_DATE_RE.match(text):
        return "DATE"
    if _NUMBER_RE.match(text):
        return "NUMBER"
    return "STRING"


def _leaf(prop, op, value, entity, ctx):
    """Build one field-level search criteria dict."""
    relation, prop_name = _split_relation(prop)
    kind = _classify(prop_name, value, ctx)

    if isinstance(value, (datetime.date, datetime.datetime)):
        value = (
            value.isoformat(sep=" ")[:19]
            if isinstance(value, datetime.datetime)
            else value.isoformat()
        )
    elif kind == "NUMBER" and isinstance(value, (int, float)):
        pass  # AbstractNumberValue takes a JSON number; don't stringify it
    else:
        value = str(value)

    is_attribute = prop_name.lower() in ("registrationdate", "modificationdate")

    if kind == "DATE":
        eq_type = DATE_OPS[op]
        if prop_name.lower() == "registrationdate":
            crit_type = _PREFIX + "RegistrationDateSearchCriteria"
        elif prop_name.lower() == "modificationdate":
            crit_type = _PREFIX + "ModificationDateSearchCriteria"
        else:
            crit_type = _PREFIX + "DatePropertySearchCriteria"
    elif kind == "NUMBER":
        eq_type = NUMBER_OPS[op]
        crit_type = _PREFIX + "NumberPropertySearchCriteria"
    else:
        eq_type = STRING_OPS[op]
        crit_type = _PREFIX + "StringPropertySearchCriteria"

    leaf = {
        "@type": crit_type,
        "fieldName": _field_name(prop_name),
        "fieldType": "ATTRIBUTE" if is_attribute else "PROPERTY",
        "fieldValue": {"@type": eq_type, "value": value},
    }
    # 1.37.5 emits useWildcards on every leaf, including date and number
    # criteria where it is meaningless. Kept for byte-identical legacy output;
    # removing it would be a separate, independently testable change.
    leaf["useWildcards"] = kind == "STRING" and "*" in str(value) and op == "=="

    if relation:
        wrapper = {
            "@type": RELATION_SEARCH_TYPE[entity][relation],
            "criteria": [leaf],
        }
        return wrapper
    return leaf


def _all_same_relation(criteria, entity):
    types = {c.get("@type") for c in criteria}
    return (
        len(types) == 1
        and next(iter(types)) in RELATION_SEARCH_TYPE.get(entity, {}).values()
    )


def _field_name(prop_name):
    """Property codes starting with '_' denote openBIS internal '$' properties."""
    if prop_name.startswith("_"):
        prop_name = "$" + prop_name[1:]
    return prop_name.upper()


def _split_relation(prop):
    match = re.match(r"^(parent|child|container)_(.+)$", prop, re.I)
    if match:
        return match.group(1).lower(), match.group(2)
    return None, prop


def _composite(criteria, operator, entity, ctx):
    """
    Wrap a list of criteria in a nested entity search criteria carrying its
    own operator. This is exactly what _subcriteria_for_code_new already does
    for `code=[...]`, so the shape is known to round-trip through the V3 API.
    """
    if not criteria:
        raise ValueError(
            "refusing to emit a composite search criteria with an empty "
            "`criteria` list: openBIS reads it as 'no constraint' and returns "
            "every entity, which is never what the caller meant."
        )
    relation_types = {c.get("@type") for c in criteria}
    if _all_same_relation(criteria, entity):
        # merge parent_X in [...] into ONE parents-criteria with OR inside,
        # otherwise "OR of two parent criteria" would mean two distinct parents
        merged = []
        for c in criteria:
            merged += c["criteria"]
        wrapper = {"@type": next(iter(relation_types)), "criteria": merged}
        if operator != "AND":  # AND is the SearchOperator default
            wrapper["operator"] = operator
        return wrapper
    return {
        "@type": ENTITY_SEARCH_TYPE[entity],
        "operator": operator,
        "criteria": criteria,
    }


# ---------------------------------------------------------------------------
# entry point used by get_samples / get_experiments / get_datasets
# ---------------------------------------------------------------------------


def build_property_criteria(prop, value, entity="sample", property_type=None):
    """
    Returns a LIST of criteria dicts to be spliced into the top-level
    (AND) criteria list. Replaces _subcriteria_for_properties.
    """
    ctx = {"operator": "AND", "type": property_type}
    return _coerce(value, prop).render(prop, entity, ctx)


def normalize_where(where, kwargs):
    """
    Accept both shapes:

        where={"A": 1, "B": 2}                            (dict, as today)
        where=[("modificationDate", Ge("2021-01-01")),
               ("modificationDate", Lt("2021-02-01"))]    (pairs; repeats OK)

    Precedence rules:

    * dict form -- keys are deduplicated against **properties with the kwarg
      winning, i.e. exactly ``{**where, **properties}`` as in 1.37.5.
      Concatenating instead would turn a documented override into an
      unsatisfiable AND of two equalities and silently return nothing.
    * pair-list form -- taken verbatim, because repetition is the whole point
      of that shape. A key present in BOTH the pair list and **properties is
      ambiguous (override or add?), so it is rejected rather than guessed.
    """
    if where is None:
        return list(kwargs.items())

    if isinstance(where, dict):
        return list({**where, **kwargs}.items())

    pairs = [tuple(item) for item in where]
    clash = {prop for prop, _ in pairs} & set(kwargs)
    if clash:
        raise ValueError(
            f"{sorted(clash)} given both in the `where` pair list and as a "
            "keyword argument. Use one form or the other -- with a pair list "
            "there is no way to tell an override from an extra constraint."
        )
    return pairs + list(kwargs.items())
