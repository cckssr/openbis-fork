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
Tests for the extended ``where=`` grammar of get_samples()
==========================================================

Two independent layers:

* ``TestLegacyJsonEquivalence`` -- offline. Asserts that for every value shape
  pybis 1.37.5 already accepts, the new builder emits *identical* JSON to
  ``_subcriteria_for_properties``. No server needed; run this in CI on every
  commit.

* everything else -- integration. Needs a live openBIS. Configure with::

      export OPENBIS_URL=https://localhost:8443
      export OPENBIS_USER=admin
      export OPENBIS_PASSWORD=changeit

  The whole module is skipped when those are unset.

The integration fixture builds an isolated space + sample type so the
assertions can be exact set comparisons rather than ">= n results".
"""

import datetime
import uuid

import pytest

from pybis.pybis import _subcriteria_for_properties  # legacy builder
from pybis.search import (
    Any,
    Cmp,
    Between,
    Contains,
    Eq,
    Ge,
    Gt,
    In,
    IsSet,
    Le,
    Lt,
    Ne,
    Not,
    NotSet,
)

from pybis.search.search_criteria import (normalize_where, build_property_criteria)

# ---------------------------------------------------------------------------
# offline: legacy syntax must round-trip unchanged
# ---------------------------------------------------------------------------


class TestLegacyJsonEquivalence:
    """
    Guards the promise that the new builder is a superset of the old one.

    build_property_criteria() returns a list; for all legacy inputs it must be
    a list of exactly one element equal to what the old function returned.
    """

    @pytest.mark.parametrize(
        "prop,value",
        [
            ("MY_PROP", "abc"),
            ("MY_PROP", "abc*"),
            ("MY_PROP", "*abc*"),
            ("MY_PROP", "= abc"),
            ("CONC", ">= 1.5"),
            ("CONC", "<= 9"),
            ("CONC", "> 1.5"),  # numeric strict: same in both
            ("CONC", "< 9"),
            ("NAME", ">= abc"),  # string comparison
            ("modificationDate", ">= 2021-01-01"),
            ("modificationDate", "<= 2021-02-01"),
            ("registrationDate", ">= 2021-01-01"),
            ("registrationDate", "2021-01-01"),
            ("_NAME", "abc"),  # $-prefixed internal property
            ("parent_STRAIN", "K12"),
            ("child_STRAIN", "K12"),
        ],
    )
    def test_same_json_as_legacy_builder(self, prop, value):
        legacy = _subcriteria_for_properties(prop, value, entity="sample")
        new = build_property_criteria(prop, value, entity="sample")
        assert isinstance(new, list)
        assert len(new) == 1
        assert new[0] == legacy, (
            f"{prop}={value!r} changed shape\n legacy: {legacy}\n new:    {new[0]}"
        )

    @pytest.mark.parametrize("prop", ["modificationDate", "registrationDate"])
    @pytest.mark.parametrize(
        "op,legacy_type,fixed_type",
        [
            (">", "DateLaterThanOrEqualToValue", "DateLaterThanValue"),
            ("<", "DateEarlierThanOrEqualToValue", "DateEarlierThanValue"),
        ],
    )
    def test_strict_date_operators_are_deliberately_different(
        self, prop, op, legacy_type, fixed_type
    ):
        """
        The one intentional behaviour change: 1.37.5 maps both '>' and '>=' to
        DateLaterThanOrEqualToValue, so '> 2021-01-01' wrongly matched
        2021-01-01. This test pins the fix and documents the divergence.
        """
        value = f"{op} 2021-01-01"
        legacy = _subcriteria_for_properties(prop, value, entity="sample")
        new = build_property_criteria(prop, value, entity="sample")[0]

        assert legacy["fieldValue"]["@type"].endswith(legacy_type)
        assert new["fieldValue"]["@type"].endswith(fixed_type)
        # everything except the comparison operator is unchanged
        assert new["@type"] == legacy["@type"]
        assert new["fieldType"] == legacy["fieldType"]
        assert new["fieldValue"]["value"] == legacy["fieldValue"]["value"]

    def test_list_value_produces_an_or_group(self):
        new = build_property_criteria("MY_PROP", ["a", "b"], entity="sample")
        assert len(new) == 1
        assert new[0]["operator"] == "OR"
        assert len(new[0]["criteria"]) == 2
        for leaf in new[0]["criteria"]:
            assert isinstance(leaf["fieldValue"]["value"], str)

    def test_range_emits_two_anded_leaves(self):
        crit = build_property_criteria(
            "modificationDate", {">=": "2021-01-01", "<": "2021-02-01"}, entity="sample"
        )
        assert len(crit) == 2
        types = [c["fieldValue"]["@type"].rsplit(".", 1)[-1] for c in crit]
        assert types == ["DateLaterThanOrEqualToValue", "DateEarlierThanValue"]

    def test_relation_range_is_merged_into_one_wrapper(self):
        """
        Regression: two separate SampleParentsSearchCriteria would mean
        "a parent later than X" AND "a parent earlier than Y" -- possibly two
        different parents. Both bounds must land in ONE wrapper.
        """
        crit = build_property_criteria(
            "parent_HARVEST_DATE",
            Between("2020-06-01", "2020-06-30"),
            entity="sample",
        )
        assert len(crit) == 1
        assert crit[0]["@type"].endswith("SampleParentsSearchCriteria")
        # AND is the SearchOperator default and is left implicit, as 1.37.5 does
        assert crit[0].get("operator", "AND") == "AND"
        assert len(crit[0]["criteria"]) == 2

    def test_dict_where_keeps_legacy_kwargs_precedence(self):
        """
        1.37.5 does {**where, **properties}: a key in both is overridden, not
        added. Concatenating the pairs instead turned an override into an
        unsatisfiable AND of two equalities and returned nothing.
        """
        pairs = normalize_where({"STATUS": "DONE"}, {"STATUS": "ABORTED"})
        assert pairs == [("STATUS", "ABORTED")]

    def test_dict_where_merges_disjoint_keys(self):
        pairs = dict(normalize_where({"A": 1}, {"B": 2}))
        assert pairs == {"A": 1, "B": 2}

    def test_pair_list_where_preserves_repeats(self):
        pairs = normalize_where([("CONC", Ge(1.5)), ("CONC", Lt(9))], {})
        assert [prop for prop, _ in pairs] == ["CONC", "CONC"]

    def test_pair_list_clashing_with_kwargs_is_rejected(self):
        with pytest.raises(ValueError, match="CONC"):
            normalize_where([("CONC", Ge(1.5))], {"CONC": "> 1"})

    @pytest.mark.parametrize("empty", [[], (), set(), {}])
    def test_empty_collection_is_rejected(self, empty):
        """
        openBIS reads a composite criteria with an empty `criteria` list as
        "no constraint" and returns EVERYTHING. Silently widening a query is
        far worse than failing, so this must raise.
        """
        with pytest.raises(ValueError):
            build_property_criteria("STATUS", empty, entity="sample")

    @pytest.mark.parametrize("junk", [object(), None, {1, 2}.__iter__()])
    def test_uninterpretable_values_raise_instead_of_being_stringified(self, junk):
        """
        The old fallback ran str() on anything, so a bad value reached the
        server as StringEqualToValue("<... object at 0x...>") and came back as
        "String does not represent a number". Fail at the call site instead.
        """
        with pytest.raises(TypeError):
            build_property_criteria("INT", junk, entity="sample")

    def test_negation_flag_sits_on_the_inner_composite(self):
        """
        The 'negated' flag does not cascade: openBIS honours it only on the
        composite that directly holds the field criteria, and silently ignores
        it one level up -- returning the UN-negated result set. This pins the
        exact shape; do not flatten it.
        """
        crit = build_property_criteria(
            "INT", Not(Between("15", "25")), entity="sample"
        )
        assert len(crit) == 1

        outer = crit[0]
        assert outer["@type"].endswith("SampleSearchCriteria")
        assert "negated" not in outer, "flag on the outer wrapper is a no-op"
        assert len(outer["criteria"]) == 1

        inner = outer["criteria"][0]
        assert inner["@type"].endswith("SampleSearchCriteria")
        assert inner["negated"] is True
        assert inner["operator"] == "AND"
        # the negated composite holds the LEAVES directly, nothing in between
        assert [c["@type"].rsplit(".", 1)[-1] for c in inner["criteria"]] == [
            "NumberPropertySearchCriteria",
            "NumberPropertySearchCriteria",
        ]

    def test_isset_is_a_string_wildcard_whatever_the_data_type(self):
        """
        "*" is not a number, so IsSet() must emit a String criteria even for
        an INTEGER or TIMESTAMP property -- it must not go through _classify.
        """
        for prop in ("STATUS", "BATCH", "HARVEST_DATE"):
            leaf = build_property_criteria(prop, IsSet(), entity="sample")[0]
            assert leaf["@type"].endswith("StringPropertySearchCriteria"), prop
            assert leaf["fieldValue"]["@type"] == "as.dto.common.search.AnyStringValue"
            assert leaf["fieldValue"]["value"] == ""

    def test_notset_uses_the_working_negation_shape(self):
        outer = build_property_criteria("STATUS", NotSet(), entity="sample")[0]
        assert "negated" not in outer
        inner = outer["criteria"][0]
        assert inner["negated"] is True

    def test_isset_on_a_parent_property(self):
        crit = build_property_criteria("parent_STATUS", IsSet(), entity="sample")[0]
        assert crit["@type"].endswith("SampleParentsSearchCriteria")
        sub_crit = crit["criteria"][0]
        assert sub_crit["@type"].endswith("StringPropertySearchCriteria")
        assert sub_crit["fieldName"] == "STATUS"
        assert sub_crit["fieldType"] == "PROPERTY"
        assert sub_crit["fieldValue"]["@type"] == "as.dto.common.search.AnyStringValue"
        assert sub_crit["fieldValue"]["value"] == ""

    def test_ne_wraps_a_single_leaf_the_same_way(self):
        outer = build_property_criteria("STATUS", Ne("ABORTED"), entity="sample")[0]
        assert "negated" not in outer
        inner = outer["criteria"][0]
        assert inner["negated"] is True
        assert len(inner["criteria"]) == 1
        assert inner["criteria"][0]["fieldValue"]["value"] == "ABORTED"

    def test_negation_composes_inside_an_or(self):
        crit = build_property_criteria(
            "STATUS", Any(Ne("ABORTED"), Eq("DONE")), entity="sample"
        )
        assert len(crit) == 1 and crit[0]["operator"] == "OR"
        negated_branch = crit[0]["criteria"][0]
        assert "negated" not in negated_branch
        assert negated_branch["criteria"][0]["negated"] is True

    def test_disjoint_ranges_keep_their_grouping(self):
        crit = build_property_criteria(
            "CONC", Any(Between(0, 1), Between(10, 11)), entity="sample"
        )
        assert len(crit) == 1
        assert crit[0]["operator"] == "OR"
        assert len(crit[0]["criteria"]) == 2
        for branch in crit[0]["criteria"]:
            assert branch["operator"] == "AND"
            assert len(branch["criteria"]) == 2


# ---------------------------------------------------------------------------
# integration fixture
# ---------------------------------------------------------------------------


class Fixture:
    """Handles for the isolated test data set."""

    def __init__(self, o, space_code, type_code, props):
        self.o = o
        self.space_code = space_code
        self.type_code = type_code
        self.props = props  # logical name -> generated property code

    def search(self, **kwargs):
        """get_samples scoped to this fixture; returns the set of sample codes."""
        kwargs.setdefault("space", self.space_code)
        kwargs.setdefault("type", self.type_code)
        things = self.o.get_samples(**kwargs)
        if len(things) == 0:
            return set()
        return set(things.df["identifier"].str.rsplit("/", n=1).str[-1])


# Reference data. Timestamps sit at midday so a server/client timezone offset
# cannot push a value across a date boundary and flip an assertion.
#
#  code   STATUS    CONC   BATCH  HARVEST_DATE          parents
SAMPLES = [
    ("P01", "DONE", 0.1, 100, "2020-06-05 12:00:00", []),
    ("P02", "FAILED", 0.2, 101, "2020-06-25 12:00:00", []),
    ("P03", "DONE", 0.3, 102, "2020-05-01 12:00:00", []),
    ("P04", "FAILED", 0.4, 103, "2020-08-01 12:00:00", []),
    ("S01", "DONE", 0.5, 1, "2021-01-05 12:00:00", ["P01"]),
    ("S02", "DONE", 1.5, 2, "2021-01-15 12:00:00", ["P02"]),
    ("S03", "FAILED", 5.0, 3, "2021-01-31 12:00:00", []),
    ("S04", "FAILED", 9.0, 4, "2021-02-01 12:00:00", []),
    ("S05", "ABORTED", 10.0, 5, "2021-02-15 12:00:00", []),
    ("S06", "ABORTED", 10.5, 6, "2021-03-01 12:00:00", []),
    # S07 straddles June without any single parent being inside it:
    # the un-merged relation criteria would wrongly match it.
    ("S07", "PENDING", 20.0, 7, "2020-12-31 12:00:00", ["P03", "P04"]),
    ("S08", "PENDING", 0.0, 8, "2021-01-01 12:00:00", []),
    # S09 has no STATUS at all; S10 has STATUS set to the empty string.
    # Together they separate "unset" from "set but empty".
    ("S09", None, 30.0, 9, "2021-04-01 12:00:00", []),
    ("S10", "", 31.0, 10, "2021-04-02 12:00:00", []),
]

#: flip if the server includes rows whose property is unset in a negated search
INCLUDES_UNSET_PROPERTIES = True
#: flip if IsSet() matches a property explicitly set to the empty string
EMPTY_STRING_COUNTS_AS_SET = False

PARENTS = {"P01", "P02", "P03", "P04"}
CHILDREN = {code for code, *_ in SAMPLES} - PARENTS


@pytest.fixture(scope="module")
def fixture(openbis_instance):
    o = openbis_instance

    run = uuid.uuid4().hex[:8].upper()
    space_code = f"TEST_SEARCH_{run}"
    type_code = f"TEST_SEARCH_TYPE_{run}"
    props = {
        "STATUS": f"TEST_STATUS_{run}",  # VARCHAR
        "CONC": f"TEST_CONC_{run}",  # REAL
        "BATCH": f"TEST_BATCH_{run}",  # INTEGER
        "HARVEST_DATE": f"TEST_HARVEST_DATE_{run}",  # TIMESTAMP
    }

    created = []
    try:
        # --- 1. property types -------------------------------------------
        for logical, data_type in (
            ("STATUS", "VARCHAR"),
            ("CONC", "REAL"),
            ("BATCH", "INTEGER"),
            ("HARVEST_DATE", "TIMESTAMP"),
        ):
            pt = o.new_property_type(
                code=props[logical],
                label=logical.title(),
                description=f"{data_type} property for search criteria tests",
                dataType=data_type,
            )
            pt.save()

        # --- 2. sample type ----------------------------------------------
        sample_type = o.new_sample_type(
            code=type_code,
            generatedCodePrefix="TS",
            autoGeneratedCode=False,
            listable=True,
            description="sample type for search criteria tests",
        )
        sample_type.save()
        for ordinal, logical in enumerate(
            ["STATUS", "CONC", "BATCH", "HARVEST_DATE"], start=1
        ):
            sample_type.assign_property(
                prop=props[logical], section="test", ordinal=ordinal, mandatory=False
            )

        # --- 3. space -----------------------------------------------------
        space = o.new_space(code=space_code, description="search criteria tests")
        space.save()

        # --- 4. samples ---------------------------------------------------
        by_code = {}
        for code, status, conc, batch, harvest, parents in SAMPLES:
            sample = o.new_sample(
                code=code,
                type=type_code,
                space=space_code,
                parents=[by_code[p] for p in parents] or None,
                props={
                    **({} if status is None else {props["STATUS"]: status}),
                    props["CONC"]: conc,
                    props["BATCH"]: batch,
                    props["HARVEST_DATE"]: harvest,
                },
            )
            sample.save()
            by_code[code] = sample
            created.append(sample)

        yield Fixture(o, space_code, type_code, props)

    finally:
        # best-effort teardown; leave a readable reason in the trash
        reason = "search criteria test cleanup"
        for sample in reversed(created):
            try:
                sample.delete(reason)
            except Exception:  # pragma: no cover - teardown is advisory
                pass
        for deleter in (
            lambda: o.get_space(space_code).delete(reason),
            lambda: o.get_sample_type(type_code).delete(reason),
        ):
            try:
                deleter()
            except Exception:  # pragma: no cover
                pass
        for logical in props:
            try:
                o.get_property_type(props[logical]).delete(reason)
            except Exception:  # pragma: no cover
                pass


# ---------------------------------------------------------------------------
# integration: legacy syntax still returns the same rows
# ---------------------------------------------------------------------------


class TestLegacySyntaxAgainstServer:
    def test_plain_equality(self, fixture):
        assert fixture.search(where={fixture.props["STATUS"]: "DONE"}) == {
            "P01",
            "P03",
            "S01",
            "S02",
        }

    def test_wildcard(self, fixture):
        assert fixture.search(where={fixture.props["STATUS"]: "ABO*"}) == {"S05", "S06"}

    def test_numeric_comparator_string(self, fixture):
        assert fixture.search(where={fixture.props["CONC"]: ">= 10"}) == {
            "S05",
            "S06",
            "S07",
            "S09",
            "S10",
        }

    def test_property_kwargs_still_work(self, fixture):
        """The **properties path, not the where= path."""
        assert fixture.search(**{fixture.props["STATUS"]: "PENDING"}) == {"S07", "S08"}

    def test_kwargs_override_where_on_conflict(self, fixture):
        """
        Documented precedence in 1.37.5: {**where, **properties}. The kwarg
        replaces the where entry; it must not be ANDed with it, which would
        make the query unsatisfiable.
        """
        result = fixture.search(
            where={fixture.props["STATUS"]: "DONE"},
            **{fixture.props["STATUS"]: "ABORTED"},
        )
        assert result == {"S05", "S06"}

    def test_parent_property_equality(self, fixture):
        assert fixture.search(where={f"parent_{fixture.props['STATUS']}": "DONE"}) == {
            "S01",
            "S07",
        }

    def test_modification_date_lower_bound(self, fixture):
        """Everything in the fixture was just written, so nothing is excluded."""
        today = datetime.date.today().isoformat()
        assert fixture.search(where={"modificationDate": f">= {today}"}) == (
            PARENTS | CHILDREN
        )


# ---------------------------------------------------------------------------
# integration: the new grammar
# ---------------------------------------------------------------------------


class TestValueLists:
    def test_list_shorthand(self, fixture):
        assert fixture.search(
            where={fixture.props["STATUS"]: ["DONE", "ABORTED"]}
        ) == {"P01", "P03", "S01", "S02", "S05", "S06"}

    def test_explicit_In_is_the_same(self, fixture):
        shorthand = fixture.search(where={fixture.props["STATUS"]: ["FAILED"]})
        explicit = fixture.search(where={fixture.props["STATUS"]: In(["FAILED"])})
        assert shorthand == explicit == {"P02", "P04", "S03", "S04"}

    def test_numeric_list(self, fixture):
        assert fixture.search(where={fixture.props["BATCH"]: [1, 4, 8]}) == {
            "S01",
            "S04",
            "S08",
        }

    def test_empty_list_is_rejected_before_reaching_the_server(self, fixture):
        """
        The server returned all 12 samples for an empty OR criteria, so the
        builder refuses to emit one. Verified end-to-end here as well as
        offline, because the failure mode is a silently unfiltered query.
        """
        with pytest.raises(ValueError):
            fixture.search(where={fixture.props["STATUS"]: []})

    def test_list_on_parent_property(self, fixture):
        assert fixture.search(
            where={f"parent_{fixture.props['STATUS']}": ["DONE", "FAILED"]}
        ) == {"S01", "S02", "S07"}


class TestRanges:
    def test_numeric_range_dict(self, fixture):
        assert fixture.search(where={fixture.props["CONC"]: {">=": 1.5, "<=": 9.0}}) == {
            "S02",
            "S03",
            "S04",
        }

    def test_numeric_range_Between(self, fixture):
        assert fixture.search(where={fixture.props["CONC"]: Between(1.5, 9.0)}) == {
            "S02",
            "S03",
            "S04",
        }

    def test_half_open_interval(self, fixture):
        assert fixture.search(
            where={fixture.props["CONC"]: Between(1.5, 9.0, inclusive=(True, False))}
        ) == {"S02", "S03"}

    def test_strict_lower_bound_excludes_the_boundary(self, fixture):
        inclusive = fixture.search(where={fixture.props["CONC"]: Ge(1.5)})
        strict = fixture.search(where={fixture.props["CONC"]: Gt(1.5)})
        assert "S02" in inclusive  # CONC == 1.5
        assert "S02" not in strict
        assert inclusive - strict == {"S02"}

    def test_repeated_key_via_pair_list(self, fixture):
        """The dict cannot hold the same key twice; the pair list can."""
        conc = fixture.props["CONC"]
        assert fixture.search(where=[(conc, Ge(1.5)), (conc, Lt(9.0))]) == {
            "S02",
            "S03",
        }

    def test_disjoint_ranges(self, fixture):
        assert fixture.search(
            where={fixture.props["CONC"]: Any(Between(0.0, 0.5), Between(10.0, 10.5))}
        ) == {"P01", "P02", "P03", "P04", "S01", "S05", "S06", "S08"}

    def test_integer_property_range(self, fixture):
        assert fixture.search(where={fixture.props["BATCH"]: Between(2, 5)}) == {
            "S02",
            "S03",
            "S04",
            "S05",
        }


class TestTimestamps:
    def test_timestamp_property_range(self, fixture):
        assert fixture.search(
            where={
                fixture.props["HARVEST_DATE"]: Between(
                    "2021-01-01 00:00:00", "2021-01-31 23:59:59"
                )
            }
        ) == {"S01", "S02", "S03", "S08"}

    def test_timestamp_range_with_datetime_objects(self, fixture):
        assert fixture.search(
            where={
                fixture.props["HARVEST_DATE"]: Between(
                    datetime.datetime(2021, 2, 1), datetime.datetime(2021, 2, 28)
                )
            }
        ) == {"S04", "S05"}

    def test_open_ended_upper_bound(self, fixture):
        assert fixture.search(
            where={fixture.props["HARVEST_DATE"]: Lt("2021-01-01 00:00:00")}
        ) == PARENTS | {"S07"}

    def test_modification_date_range(self, fixture):
        """The originally reported use case, on an ATTRIBUTE rather than a property."""
        today = datetime.date.today()
        tomorrow = today + datetime.timedelta(days=1)
        assert (
            fixture.search(
                where={
                    "modificationDate": Between(
                        today.isoformat(), tomorrow.isoformat()
                    )
                }
            )
            == PARENTS | CHILDREN
        )

    def test_date_only_bounds_are_documented(self, fixture):
        """
        openBIS interprets a date-only bound at day granularity. Pin whatever
        the server actually does so a change in that behaviour is visible here
        rather than surfacing as a mysterious off-by-one in user code.
        """
        result = fixture.search(
            where={fixture.props["HARVEST_DATE"]: Le("2021-01-31")}
        )
        assert {"S01", "S02", "S08"} <= result
        assert "S04" not in result  # 2021-02-01, unambiguously outside


class TestRelationCriteria:
    def test_parent_property_range_does_not_straddle_two_parents(self, fixture):
        """
        S07's parents are dated 2020-05-01 and 2020-08-01. Neither is inside
        June, so S07 must not match -- the un-merged version of the builder
        matched it, because one parent satisfied each bound separately.
        """
        result = fixture.search(
            where={
                f"parent_{fixture.props['HARVEST_DATE']}": Between(
                    "2020-06-01 00:00:00", "2020-06-30 23:59:59"
                )
            }
        )
        assert result == {"S01", "S02"}
        assert "S07" not in result

    def test_parent_range_combined_with_own_property(self, fixture):
        assert fixture.search(
            where={
                f"parent_{fixture.props['HARVEST_DATE']}": Between(
                    "2020-06-01 00:00:00", "2020-06-30 23:59:59"
                ),
                fixture.props["STATUS"]: "DONE",
            }
        ) == {"S01", "S02"}


class TestStringPredicates:
    def test_contains(self, fixture):
        assert fixture.search(where={fixture.props["STATUS"]: Contains("ORT")}) == {
            "S05",
            "S06",
        }


class TestCombinations:
    def test_range_and_list_in_one_call(self, fixture):
        assert fixture.search(
            where={
                fixture.props["HARVEST_DATE"]: Between(
                    "2021-01-01 00:00:00", "2021-02-28 23:59:59"
                ),
                fixture.props["STATUS"]: ["DONE", "FAILED"],
            }
        ) == {"S01", "S02", "S03", "S04"}

    def test_new_and_legacy_syntax_mixed(self, fixture):
        """A legacy comparator string and a new range in the same where dict."""
        assert fixture.search(
            where={
                fixture.props["CONC"]: ">= 1.5",
                fixture.props["BATCH"]: Between(2, 4),
            }
        ) == {"S02", "S03", "S04"}

    def test_other_get_samples_filters_still_apply(self, fixture):
        """where= must compose with space/type/attrs, not replace them."""
        things = fixture.o.get_samples(
            space=fixture.space_code,
            type=fixture.type_code,
            where={fixture.props["STATUS"]: ["DONE"]},
            props=[fixture.props["CONC"]],
            attrs=["parents"],
        )
        assert len(things) == 4
        assert fixture.props["CONC"].lower() in [c.lower() for c in things.df.columns]


# ---------------------------------------------------------------------------
# unverified: negation
# ---------------------------------------------------------------------------


class TestNegation:
    def test_ne_excludes_a_value(self, fixture):
        result = fixture.search(where={fixture.props["STATUS"]: Ne("ABORTED")})
        assert "S05" not in result and "S06" not in result
        assert {"S01", "S02", "S03", "S04"} <= result

    def test_not_a_range_returns_the_complement(self, fixture):
        plain = fixture.search(where={fixture.props["CONC"]: Between(1.5, 9.0)})
        negated = fixture.search(where={fixture.props["CONC"]: Not(Between(1.5, 9.0))})
        assert plain == {"S02", "S03", "S04"}
        assert plain & negated == set(), "negation returned overlapping rows"
        assert plain | negated == PARENTS | CHILDREN

    def test_not_agrees_with_the_directly_expressed_complement(self, fixture):
        """Any(Lt, Gt) and Not(Between) must select the same samples."""
        assert fixture.search(
            where={fixture.props["CONC"]: Not(Between(1.5, 9.0))}
        ) == fixture.search(where={fixture.props["CONC"]: Any(Lt(1.5), Gt(9.0))})

    def test_not_a_list(self, fixture):
        assert fixture.search(
            where={fixture.props["STATUS"]: Not(In(["DONE", "FAILED"]))}
        ) == {"S05", "S06", "S07", "S08", "S09", "S10"}

    def test_negation_combined_with_another_constraint(self, fixture):
        assert fixture.search(
            where={
                fixture.props["STATUS"]: Ne("ABORTED"),
                fixture.props["CONC"]: Between(0.5, 20.0),
            }
        ) == {"S01", "S02", "S03", "S04", "S07"}

    def test_unset_property_behaviour_is_pinned(self, fixture):
        """
        S09 has no STATUS at all. Whether negation returns it is a server
        decision; assert whichever way it goes so a change is visible.
        """
        result = fixture.search(where={fixture.props["STATUS"]: Ne("ABORTED")})
        assert ("S09" in result) == INCLUDES_UNSET_PROPERTIES


class TestPropertyPresence:
    """
    IsSet()/NotSet() rest on a wildcard match, so their exact semantics are a
    server question. These tests answer it rather than assuming.
    """

    def test_isset_excludes_the_sample_without_the_property(self, fixture):
        result = fixture.search(where={fixture.props["STATUS"]: IsSet()})
        assert "S09" not in result
        assert {"S01", "S05", "S08"} <= result

    def test_notset_returns_the_sample_without_the_property(self, fixture):
        assert "S09" in fixture.search(where={fixture.props["STATUS"]: NotSet()})

    def test_isset_and_notset_partition_the_samples(self, fixture):
        is_set = fixture.search(where={fixture.props["STATUS"]: IsSet()})
        not_set = fixture.search(where={fixture.props["STATUS"]: NotSet()})
        assert is_set & not_set == set()
        assert is_set | not_set == PARENTS | CHILDREN

    def test_empty_string_classification_is_pinned(self, fixture):
        """S10 has STATUS = ''. Does a wildcard consider that a value?"""
        is_set = fixture.search(where={fixture.props["STATUS"]: IsSet()})
        assert ("S10" in is_set) == EMPTY_STRING_COUNTS_AS_SET

    def test_isset_works_on_a_non_string_property(self, fixture):
        """
        Probes whether a String criteria matches an INTEGER property. Every
        sample has BATCH, so anything short of the full set means the server
        will not apply a String criteria to a non-VARCHAR property -- in which
        case IsSet() is only usable on text properties.
        """
        assert fixture.search(where={fixture.props["BATCH"]: IsSet()}) == (
            PARENTS | CHILDREN
        )

    def test_notset_combined_with_another_constraint(self, fixture):
        assert fixture.search(
            where={
                fixture.props["STATUS"]: NotSet(),
                fixture.props["CONC"]: Gt(25.0),
            }
        ) == {"S09", "S10"}


# ---------------------------------------------------------------------------
# offline: the get_* call sites themselves
# ---------------------------------------------------------------------------


class TestCallSitesAreMigrated:
    """
    build_property_criteria() returns a LIST, so every call site must splice it
    (``sub_criteria += ...``). Swapping only the function name into the old
    ``sub_criteria.extend(list(map(...)))`` yields ``criteria: [[{...}]]`` --
    a list nested inside the criteria list -- which the server answers with a
    bare 500. These tests drive the real get_* methods with a stubbed
    transport, so the whole path is checked without a server.
    """

    @pytest.fixture
    def captured(self, monkeypatch):
        from pybis.pybis import Openbis as OpenbisClass

        seen = {}

        def fake_post(self, resource, request, *args, **kwargs):
            seen["request"] = request
            return {"objects": [], "totalCount": 0}

        from pybis import Openbis
        monkeypatch.setattr(OpenbisClass, "_post_request", fake_post)
        o = Openbis("https://example.invalid", verify_certificates=False)
        o.token = "offline-stub"
        seen["openbis"] = o
        return seen

    @staticmethod
    def _criteria(captured):
        return captured["request"]["params"][1]["criteria"]

    @pytest.mark.parametrize(
        "method", ["get_samples", "get_datasets", "get_experiments"]
    )
    @pytest.mark.parametrize(
        "value",
        [
            "<2021-01-01",                              # legacy comparator string
            Between("2021-01-01", "2021-02-01"),        # two leaves
            ["2021-01-01", "2021-02-01"],               # an OR group
        ],
    )
    def test_criteria_list_contains_only_dicts(self, captured, method, value):
        getattr(captured["openbis"], method)(where={"modificationDate": value})
        criteria = self._criteria(captured)
        assert criteria, "no criteria emitted at all"
        for entry in criteria:
            assert isinstance(entry, dict), (
                f"{method} spliced a {type(entry).__name__} into criteria -- the "
                "call site still uses extend(list(map(...))) instead of +="
            )
            assert "@type" in entry

    @pytest.mark.parametrize(
        "method", ["get_samples", "get_datasets", "get_experiments"]
    )
    def test_range_produces_two_sibling_criteria(self, captured, method):
        """A range must flatten into the AND list, not nest one level down."""
        getattr(captured["openbis"], method)(
            where={"modificationDate": Between("2021-01-01", "2021-02-01")}
        )
        dates = [
            c
            for c in self._criteria(captured)
            if c["@type"].endswith("ModificationDateSearchCriteria")
        ]
        assert len(dates) == 2