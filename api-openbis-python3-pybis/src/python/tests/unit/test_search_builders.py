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
"""Exact-payload tests for the typed search-criteria builders."""

import pytest

from pybis.api import filters as f
from pybis.api.search import (
    attribute_date_criterion,
    coerce_filter,
    explicit_id_criterion,
    hierarchy_property_criterion,
    id_criterion,
    is_finished_criterion,
    modifier_criterion,
    property_criterion,
    registrator_criterion,
    status_criterion,
    tags_criterion,
    type_criterion,
)
from pybis.exceptions import ValidationError

# --- PropertyFilter.to_rpc: one test per operator ----------------------------

OPERATOR_RPC_TABLE = [
    (f.eq("H2O"), {"@type": "as.dto.common.search.StringEqualToValue", "value": "H2O"}),
    (
        f.contains("H2O"),
        {"@type": "as.dto.common.search.StringContainsValue", "value": "H2O"},
    ),
    (
        f.starts_with("H2"),
        {"@type": "as.dto.common.search.StringStartsWithValue", "value": "H2"},
    ),
    (
        f.ends_with("2O"),
        {"@type": "as.dto.common.search.StringEndsWithValue", "value": "2O"},
    ),
    (
        f.wildcard("H2*"),
        {
            "@type": "as.dto.common.search.StringEqualToValue",
            "value": "H2*",
            "useWildcards": True,
        },
    ),
    (f.gt(5), {"@type": "as.dto.common.search.NumberGreaterThanValue", "value": 5}),
    (
        f.gte(5.5),
        {
            "@type": "as.dto.common.search.NumberGreaterThanOrEqualToValue",
            "value": 5.5,
        },
    ),
    (f.lt(5), {"@type": "as.dto.common.search.NumberLessThanValue", "value": 5}),
    (
        f.lte(5),
        {"@type": "as.dto.common.search.NumberLessThanOrEqualToValue", "value": 5},
    ),
    (f.number_eq(42), {"@type": "as.dto.common.search.NumberEqualToValue", "value": 42}),
    (
        f.date_eq("2024-01-01"),
        {"@type": "as.dto.common.search.DateEqualToValue", "value": "2024-01-01"},
    ),
    (
        f.date_after("2024-01-01"),
        {
            "@type": "as.dto.common.search.DateLaterThanOrEqualToValue",
            "value": "2024-01-01",
        },
    ),
    (
        f.date_before("2024-01-01"),
        {
            "@type": "as.dto.common.search.DateEarlierThanOrEqualToValue",
            "value": "2024-01-01",
        },
    ),
    (f.any_value(), {"@type": "as.dto.common.search.AnyStringValue"}),
]


@pytest.mark.parametrize("flt,expected", OPERATOR_RPC_TABLE)
def test_filter_to_rpc(flt, expected):
    assert flt.to_rpc() == expected


def test_date_factory_coerces_datetime():
    from datetime import datetime

    flt = f.date_after(datetime(2024, 1, 2, 3, 4, 5))
    assert flt.value == "2024-01-02 03:04:05"


# --- coerce_filter ------------------------------------------------------------


def test_coerce_plain_string_is_exact_match():
    assert coerce_filter("H2O") == f.eq("H2O")


def test_coerce_star_string_enables_wildcards():
    assert coerce_filter("H2*") == f.wildcard("H2*")


def test_coerce_passes_filters_through():
    flt = f.gt(5)
    assert coerce_filter(flt) is flt


# --- property_criterion -------------------------------------------------------


def test_property_criterion_string():
    assert property_criterion("formula", "H2O") == {
        "@type": "as.dto.common.search.StringPropertySearchCriteria",
        "fieldName": "FORMULA",
        "fieldType": "PROPERTY",
        "fieldValue": {
            "@type": "as.dto.common.search.StringEqualToValue",
            "value": "H2O",
        },
        "useWildcards": False,
    }


def test_property_criterion_number_family():
    crit = property_criterion("ATOMS", f.gte(3))
    assert crit["@type"] == "as.dto.common.search.NumberPropertySearchCriteria"
    assert crit["fieldValue"] == {
        "@type": "as.dto.common.search.NumberGreaterThanOrEqualToValue",
        "value": 3,
    }


def test_property_criterion_date_family():
    crit = property_criterion("MEASURED", f.date_before("2024-06-01"))
    assert crit["@type"] == "as.dto.common.search.DatePropertySearchCriteria"


def test_property_criterion_wildcard_string():
    crit = property_criterion("FORMULA", "H2*")
    assert crit["useWildcards"] is True
    assert crit["fieldValue"]["value"] == "H2*"


def test_property_criterion_dollar_prefix_normalization():
    """1.x translated a leading underscore into the $ prefix."""
    assert property_criterion("_name", "x")["fieldName"] == "$NAME"


def test_property_criterion_keeps_explicit_dollar():
    assert property_criterion("$NAME", "x")["fieldName"] == "$NAME"


# --- attribute date criteria ---------------------------------------------------


def test_registration_date_criterion():
    assert attribute_date_criterion("registration_date", "2024-01-01") == {
        "@type": "as.dto.common.search.RegistrationDateSearchCriteria",
        "fieldName": "REGISTRATIONDATE",
        "fieldType": "ATTRIBUTE",
        "fieldValue": {
            "@type": "as.dto.common.search.DateEqualToValue",
            "value": "2024-01-01",
        },
    }


def test_modification_date_criterion_with_filter():
    crit = attribute_date_criterion("modification_date", f.date_after("2024-01-01"))
    assert crit["@type"] == "as.dto.common.search.ModificationDateSearchCriteria"
    assert (
        crit["fieldValue"]["@type"]
        == "as.dto.common.search.DateLaterThanOrEqualToValue"
    )


def test_attribute_date_criterion_rejects_non_date_filter():
    with pytest.raises(ValidationError, match="date filters"):
        attribute_date_criterion("registration_date", f.gt(5))


# --- hierarchy property criteria -----------------------------------------------


def test_parent_property_criterion_for_objects():
    crit = hierarchy_property_criterion(
        "sample", f.parent_prop("BATCH_ID", f.eq("BATCH-001"))
    )
    assert crit["@type"] == "as.dto.sample.search.SampleParentsSearchCriteria"
    inner = crit["criteria"][0]
    assert inner["fieldName"] == "BATCH_ID"
    assert inner["fieldValue"]["value"] == "BATCH-001"


def test_child_property_criterion_for_datasets():
    crit = hierarchy_property_criterion(
        "dataset", f.child_prop("STATUS", f.eq("OK"))
    )
    assert crit["@type"] == "as.dto.dataset.search.DataSetChildrenSearchCriteria"


def test_container_property_criterion():
    crit = hierarchy_property_criterion(
        "sample", f.container_prop("BOX", f.eq("B1"))
    )
    assert crit["@type"] == "as.dto.sample.search.SampleContainerSearchCriteria"


def test_hierarchy_criterion_rejects_unsupported_entity():
    with pytest.raises(ValidationError):
        hierarchy_property_criterion(
            "experiment",  # type: ignore[arg-type] — invalid on purpose
            f.parent_prop("X", f.eq("y")),
        )


# --- id criteria ----------------------------------------------------------------


def test_id_criterion_perm_id():
    assert id_criterion("20240101000000000-1") == {
        "@type": "as.dto.common.search.PermIdSearchCriteria",
        "fieldName": "perm_id",
        "fieldType": "ATTRIBUTE",
        "fieldValue": {
            "value": "20240101000000000-1",
            "@type": "as.dto.common.search.StringEqualToValue",
        },
    }


def test_id_criterion_identifier():
    crit = id_criterion("/SPACE/PROJ/CODE")
    assert crit["@type"] == "as.dto.common.search.IdentifierSearchCriteria"
    assert crit["fieldName"] == "identifier"
    assert crit["fieldValue"]["value"] == "/SPACE/PROJ/CODE"


def test_id_criterion_identifier_normalizes_slash():
    assert id_criterion("SPACE/CODE")["fieldValue"]["value"] == "/SPACE/CODE"


def test_id_criterion_code_uppercased():
    crit = id_criterion("my-object")
    assert crit["@type"] == "as.dto.common.search.CodeSearchCriteria"
    assert crit["fieldValue"]["value"] == "MY-OBJECT"


@pytest.mark.parametrize(
    "kind,expected_type",
    [
        ("perm_id", "as.dto.common.search.PermIdSearchCriteria"),
        ("identifier", "as.dto.common.search.IdentifierSearchCriteria"),
        ("code", "as.dto.common.search.CodeSearchCriteria"),
    ],
)
def test_explicit_id_criterion_kinds(kind, expected_type):
    assert explicit_id_criterion(kind, "X")["@type"] == expected_type


# --- other criteria --------------------------------------------------------------


def test_tags_criterion():
    crit = tags_criterion(["TAG1", "TAG2"])
    assert crit["@type"] == "as.dto.tag.search.TagSearchCriteria"
    assert crit["operator"] == "AND"
    assert [c["fieldValue"]["value"] for c in crit["criteria"]] == ["TAG1", "TAG2"]


def test_type_criterion():
    crit = type_criterion("Sample", "molecule")
    assert crit["@type"] == "as.dto.sample.search.SampleTypeSearchCriteria"
    assert crit["criteria"][0]["fieldValue"]["value"] == "MOLECULE"


def test_type_criterion_dataset_camelcase():
    crit = type_criterion("DataSet", "RAW_DATA")
    assert crit["@type"] == "as.dto.dataset.search.DataSetTypeSearchCriteria"


def test_status_criterion():
    crit = status_criterion("archived")
    assert crit["@type"] == "as.dto.dataset.search.PhysicalDataSearchCriteria"
    assert crit["criteria"][0]["fieldValue"] == "ARCHIVED"


def test_status_criterion_rejects_invalid():
    with pytest.raises(ValidationError, match="status must be one of"):
        status_criterion("NOT_A_STATUS")


def test_registrator_criterion():
    crit = registrator_criterion("alice")
    assert crit["@type"] == "as.dto.person.search.RegistratorSearchCriteria"
    inner = crit["criteria"][0]
    assert inner["@type"] == "as.dto.person.search.UserIdSearchCriteria"
    assert inner["fieldValue"]["value"] == "alice"


def test_modifier_criterion():
    crit = modifier_criterion("bob")
    assert crit["@type"] == "as.dto.person.search.ModifierSearchCriteria"


@pytest.mark.parametrize("flag,expected", [(True, "true"), (False, "false")])
def test_is_finished_criterion(flag, expected):
    crit = is_finished_criterion(flag)
    assert crit["fieldName"] == "FINISHED_FLAG"
    assert crit["fieldValue"]["value"] == expected
