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
"""Unit tests for EntityBehavior: perm_id, equality, hashing, dirty flag."""

from payload_scenarios import (
    FakeOpenbis,
    make_experiment_type,
    make_sample_type,
    make_space,
    experiment_data,
    sample_data,
)
from pybis.experiment import Experiment
from pybis.sample import Sample


def loaded_sample(o, st, perm_id="20240101000000000-1", identifier="/SPACE/PROJ/S1"):
    return Sample(o, type=st, data=sample_data(st, perm_id, identifier))


def test_perm_id_of_loaded_entity():
    o = FakeOpenbis()
    st = make_sample_type(o)
    assert loaded_sample(o, st).perm_id == "20240101000000000-1"


def test_perm_id_of_new_entity_is_none():
    o = FakeOpenbis()
    st = make_sample_type(o)
    assert Sample(o, type=st, code="NEW").perm_id is None


def test_equality_by_perm_id():
    o = FakeOpenbis()
    st = make_sample_type(o)
    a = loaded_sample(o, st)
    b = loaded_sample(o, st)
    assert a is not b
    assert a == b


def test_inequality_for_different_perm_ids():
    o = FakeOpenbis()
    st = make_sample_type(o)
    a = loaded_sample(o, st)
    b = loaded_sample(o, st, perm_id="20240101000000000-2", identifier="/SPACE/PROJ/S2")
    assert a != b


def test_unsaved_entities_equal_only_to_themselves():
    o = FakeOpenbis()
    st = make_sample_type(o)
    a = Sample(o, type=st, code="NEW1")
    b = Sample(o, type=st, code="NEW1")
    assert a == a
    assert a != b


def test_different_entity_types_never_equal():
    o = FakeOpenbis()
    st = make_sample_type(o)
    et = make_experiment_type(o)
    sample = loaded_sample(o, st)
    data = experiment_data(et)
    data["permId"]["permId"] = sample.perm_id
    collection = Experiment(o, type=et, data=data)
    assert sample != collection


def test_entities_usable_in_sets():
    o = FakeOpenbis()
    st = make_sample_type(o)
    a = loaded_sample(o, st)
    b = loaded_sample(o, st)  # same perm_id
    c = loaded_sample(o, st, perm_id="20240101000000000-2", identifier="/SPACE/PROJ/S2")
    assert len({a, b, c}) == 2


def test_entities_usable_as_dict_keys():
    o = FakeOpenbis()
    st = make_sample_type(o)
    a = loaded_sample(o, st)
    b = loaded_sample(o, st)
    d = {a: "value"}
    assert d[b] == "value"


def test_space_entities_share_the_behavior():
    o = FakeOpenbis()
    assert make_space(o) == make_space(o)
    assert make_space(o) != make_space(o, code="OTHER")


def test_is_dirty_starts_false_and_mark_clean_resets():
    o = FakeOpenbis()
    st = make_sample_type(o)
    s = loaded_sample(o, st)
    assert s.is_dirty is False
    s.__dict__["_dirty"] = True
    assert s.is_dirty is True
    s._mark_clean()
    assert s.is_dirty is False
