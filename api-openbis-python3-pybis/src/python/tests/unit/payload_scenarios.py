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
"""Offline scenario builders for create/update payload golden tests.

Each scenario drives the AttrHolder payload engine exactly the way
``save()`` does and returns the JSON-RPC request it would send.  The golden
fixtures freeze pybis 1.x wire behavior (FieldUpdateValue, ListUpdateAction
add/remove/set, freeze flags, identifier shapes) so the engine can never
silently drift during the v2 refactor.
"""

from __future__ import annotations

from pybis.entity_type import ExperimentType, SampleType
from pybis.experiment import Experiment
from pybis.project import Project
from pybis.sample import Sample
from pybis.space import Space


class FakeOpenbis:
    """Minimal stand-in for the client: payload building only needs a token."""

    token = "golden-token"


def make_sample_type(o):
    return SampleType(
        o,
        data={
            "@type": "as.dto.sample.SampleType",
            "permId": {
                "permId": "MOLECULE",
                "@type": "as.dto.entitytype.id.EntityTypePermId",
                "entityKind": "SAMPLE",
            },
            "code": "MOLECULE",
            "propertyAssignments": [],
        },
    )


def make_experiment_type(o):
    return ExperimentType(
        o,
        data={
            "@type": "as.dto.experiment.ExperimentType",
            "permId": {
                "permId": "DEFAULT_EXPERIMENT",
                "@type": "as.dto.entitytype.id.EntityTypePermId",
                "entityKind": "EXPERIMENT",
            },
            "code": "DEFAULT_EXPERIMENT",
            "propertyAssignments": [],
        },
    )


def make_space(o, code="MY_SPACE"):
    return Space(
        o,
        data={
            "permId": {"permId": code, "@type": "as.dto.space.id.SpacePermId"},
            "code": code,
            "description": None,
            "registrator": None,
            "modifier": None,
            "registrationDate": None,
            "modificationDate": None,
            "frozen": False,
            "frozenForProjects": False,
            "frozenForSamples": False,
        },
    )


def make_project(o, ident="/MY_SPACE/MY_PROJECT"):
    return Project(
        o,
        data={
            "permId": {
                "permId": "20240101000000000-100",
                "@type": "as.dto.project.id.ProjectPermId",
            },
            "identifier": {
                "identifier": ident,
                "@type": "as.dto.project.id.ProjectIdentifier",
            },
            "code": ident.rsplit("/", 1)[-1],
            "description": None,
            "space": {
                "permId": {
                    "permId": ident.split("/")[1],
                    "@type": "as.dto.space.id.SpacePermId",
                },
                "code": ident.split("/")[1],
            },
            "registrator": None,
            "modifier": None,
            "leader": None,
            "registrationDate": None,
            "modificationDate": None,
            "attachments": None,
            "frozen": False,
            "frozenForExperiments": False,
            "frozenForSamples": False,
        },
    )


def sample_data(
    st,
    perm_id="20240101000000000-1",
    identifier="/SPACE/PROJ/S1",
    parents=None,
    children=None,
    tags=None,
):
    return {
        "@type": "as.dto.sample.Sample",
        "permId": {"permId": perm_id, "@type": "as.dto.sample.id.SamplePermId"},
        "identifier": {
            "identifier": identifier,
            "@type": "as.dto.sample.id.SampleIdentifier",
        },
        "code": identifier.rsplit("/", 1)[-1],
        "type": st.data,
        "parents": parents or [],
        "children": children or [],
        "components": [],
        "tags": tags or [],
        "properties": {},
        "project": None,
        "space": {
            "permId": {"permId": "SPACE", "@type": "as.dto.space.id.SpacePermId"},
            "code": "SPACE",
        },
        "experiment": None,
        "registrator": None,
        "modifier": None,
        "registrationDate": None,
        "modificationDate": None,
        "attachments": None,
        "container": None,
        "frozen": False,
        "frozenForComponents": False,
        "frozenForChildren": False,
        "frozenForParents": False,
        "frozenForDataSets": False,
        "metaData": None,
        "immutableData": False,
    }


def experiment_data(et, identifier="/SPACE/PROJ/E1"):
    return {
        "@type": "as.dto.experiment.Experiment",
        "permId": {
            "permId": "20240101000000000-50",
            "@type": "as.dto.experiment.id.ExperimentPermId",
        },
        "identifier": {
            "identifier": identifier,
            "@type": "as.dto.experiment.id.ExperimentIdentifier",
        },
        "code": identifier.rsplit("/", 1)[-1],
        "type": et.data,
        "project": {
            "identifier": {
                "identifier": identifier.rsplit("/", 1)[0],
                "@type": "as.dto.project.id.ProjectIdentifier",
            },
            "code": identifier.split("/")[2],
        },
        "tags": [],
        "properties": {},
        "registrator": None,
        "modifier": None,
        "registrationDate": None,
        "modificationDate": None,
        "attachments": None,
        "frozen": False,
        "frozenForDataSets": False,
        "frozenForSamples": False,
        "metaData": None,
        "immutableData": False,
    }


TAG_DICTS = [
    {"code": "TAG1", "permId": {"permId": "/admin/TAG1"}},
    {"code": "TAG2", "permId": {"permId": "/admin/TAG2"}},
]


def build_scenarios():
    """Build all golden scenarios; returns {name: jsonable request dict}."""
    o = FakeOpenbis()
    st = make_sample_type(o)
    et = make_experiment_type(o)
    scenarios = {}

    # --- creations -----------------------------------------------------------

    space_new = Space(o, code="MY_SPACE", description="test space")
    scenarios["space_new"] = space_new.a._new_attrs()

    sample_new = Sample(o, type=st, code="S-NEW")
    sample_new.space = make_space(o)
    sample_new.parents = [
        Sample(o, type=st, data=sample_data(st, "20240101000000000-9", "/SPACE/PROJ/PARENT1"))
    ]
    sample_new.tags = TAG_DICTS
    sample_new.metaData = {"custom": "x"}
    scenarios["sample_new"] = sample_new.a._new_attrs()

    experiment_new = Experiment(o, type=et, code="E-NEW")
    experiment_new.project = make_project(o)
    scenarios["experiment_new"] = experiment_new.a._new_attrs()

    # --- updates --------------------------------------------------------------

    space_up = make_space(o)
    space_up.description = "updated description"
    scenarios["space_update_description"] = space_up.a._up_attrs()

    sample_up = Sample(o, type=st, data=sample_data(st))
    sample_up.space = make_space(o, "OTHER_SPACE")
    scenarios["sample_update_space"] = sample_up.a._up_attrs()

    parent_a = {"identifier": "/SPACE/PROJ/PARENT_A", "@type": "as.dto.sample.id.SampleIdentifier"}
    parent_b = Sample(o, type=st, data=sample_data(st, "20240101000000000-8", "/SPACE/PROJ/PARENT_B"))
    sample_rel = Sample(
        o,
        type=st,
        data=sample_data(st, parents=[{"identifier": dict(parent_a), "permId": None}]),
    )
    sample_rel.add_parents([parent_b])
    sample_rel.del_parents(
        [Sample(o, type=st, data=sample_data(st, "20240101000000000-7", "/SPACE/PROJ/PARENT_A"))]
    )
    scenarios["sample_update_parents_add_remove"] = sample_rel.a._up_attrs()

    sample_freeze = Sample(o, type=st, data=sample_data(st))
    sample_freeze.freeze = True
    sample_freeze.freezeForChildren = True
    scenarios["sample_update_freeze"] = sample_freeze.a._up_attrs()

    sample_tags = Sample(o, type=st, data=sample_data(st))
    sample_tags.tags = TAG_DICTS
    scenarios["sample_update_tags"] = sample_tags.a._up_attrs()

    experiment_up = Experiment(o, type=et, data=experiment_data(et))
    experiment_up.project = make_project(o, "/MY_SPACE/OTHER_PROJECT")
    scenarios["experiment_update_project"] = experiment_up.a._up_attrs()

    return scenarios
