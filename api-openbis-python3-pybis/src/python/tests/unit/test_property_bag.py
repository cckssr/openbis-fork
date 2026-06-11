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
"""Unit tests for PropertyBag (dict-like, validated property access)."""

import pytest

from payload_scenarios import FakeOpenbis, sample_data
from pybis.entities._properties import PropertyBag
from pybis.entity_type import SampleType
from pybis.sample import Sample


def prop_assignment(code, data_type, mandatory=False):
    return {
        "propertyType": {"code": code, "dataType": data_type},
        "mandatory": mandatory,
        "showInEditView": True,
    }


@pytest.fixture
def entity():
    o = FakeOpenbis()
    st = SampleType(
        o,
        data={
            "@type": "as.dto.sample.SampleType",
            "permId": {
                "permId": "MOLECULE",
                "@type": "as.dto.entitytype.id.EntityTypePermId",
                "entityKind": "SAMPLE",
            },
            "code": "MOLECULE",
            "propertyAssignments": [
                prop_assignment("FORMULA", "VARCHAR"),
                prop_assignment("ATOMS", "INTEGER"),
                prop_assignment("$NAME", "VARCHAR"),
            ],
        },
    )
    data = sample_data(st)
    data["properties"] = {"FORMULA": "H2O"}
    return Sample(o, type=st, data=data)


@pytest.fixture
def bag(entity):
    return entity.props


def test_entity_props_is_a_property_bag(bag):
    assert isinstance(bag, PropertyBag)


def test_getitem(bag):
    assert bag["FORMULA"] == "H2O"


def test_getitem_is_case_insensitive(bag):
    assert bag["formula"] == "H2O"


def test_missing_key_raises_keyerror(bag):
    with pytest.raises(KeyError):
        bag["NO_SUCH_PROPERTY"]


def test_defined_but_unset_property_returns_none(bag):
    assert bag["ATOMS"] is None


def test_setitem_and_readback(entity):
    entity.props["FORMULA"] = "D2O"
    assert entity.props["FORMULA"] == "D2O"


def test_setitem_marks_dirty(entity):
    assert entity.is_dirty is False
    entity.props["FORMULA"] = "D2O"
    assert entity.is_dirty is True


def test_setitem_validates_data_type(entity):
    with pytest.raises(ValueError):
        entity.props["ATOMS"] = "not-a-number"


def test_setitem_unknown_property_raises(entity):
    with pytest.raises(KeyError):
        entity.props["NO_SUCH_PROPERTY"] = "x"


def test_contains(bag):
    assert "FORMULA" in bag
    assert "formula" in bag
    assert "ATOMS" not in bag  # defined but unset
    assert "NO_SUCH_PROPERTY" not in bag


def test_iter_yields_all_set_keys(bag):
    assert list(bag) == ["formula"]


def test_len(entity):
    assert len(entity.props) == 1
    entity.props["ATOMS"] = 3
    assert len(entity.props) == 2


def test_del_key(entity):
    del entity.props["FORMULA"]
    assert "FORMULA" not in entity.props
    assert entity.is_dirty is True


def test_del_missing_key_raises(entity):
    with pytest.raises(KeyError):
        del entity.props["ATOMS"]


def test_to_dict_returns_copy(entity):
    snapshot = entity.props.to_dict()
    assert snapshot == {"formula": "H2O"}
    snapshot["formula"] = "changed"
    assert entity.props["FORMULA"] == "H2O"


def test_repr_shows_values(bag):
    assert "formula" in repr(bag)
    assert "H2O" in repr(bag)


def test_mapping_get_default(bag):
    assert bag.get("ATOMS") is None
    assert bag.get("FORMULA") == "H2O"


def test_legacy_attribute_style_read(bag):
    """1.x style: sample.props.formula keeps working through delegation."""
    assert bag.formula == "H2O"


def test_legacy_attribute_style_write_marks_dirty(entity):
    entity.props.formula = "D2O"
    assert entity.props["FORMULA"] == "D2O"
    assert entity.is_dirty is True


def test_legacy_p_namespace_still_works(entity):
    """The 1.x .p namespace stays untouched underneath."""
    assert entity.p.formula == "H2O"
