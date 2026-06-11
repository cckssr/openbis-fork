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
"""Table-driven tests for identifier classification.

The expectations encode pybis 1.x behavior (``is_identifier`` /
``is_permid``) so that the new classifier cannot silently drift.
"""

import pytest

from pybis.api.identifiers import ClassifiedId, classify, classify_id

# (value, expected kind) — derived from 1.x is_identifier/is_permid semantics
CLASSIFICATION_TABLE = [
    # perm_ids
    ("20240101000000000-1", "perm_id"),
    ("20160817175233002-331", "perm_id"),
    ("1-1", "perm_id"),  # 1.x: any digits-dash-digits
    # identifiers
    ("/SPACE/PROJECT/CODE", "identifier"),
    ("/SPACE/CODE", "identifier"),
    ("/SPACE/PROJECT/EXPERIMENT/SAMPLE", "identifier"),  # ELN-LIMS 4-part
    ("SPACE/CODE", "identifier"),  # 1.x tolerated a missing leading slash
    ("/ELN_SETTINGS/STORAGES/BENCH", "identifier"),
    # codes
    ("MY-OBJECT", "code"),
    ("SAMPLE_123", "code"),
    ("20240101", "code"),  # digits only, no dash-digits suffix
    ("ABC-1", "code"),  # letters before the dash
    ("MOLECULE", "code"),
]


@pytest.mark.parametrize("value,expected", CLASSIFICATION_TABLE)
def test_classify_id(value, expected):
    assert classify_id(value) == expected


def test_classify_id_strips_whitespace():
    assert classify_id("  20240101000000000-1  ") == "perm_id"


def test_classify_normalizes_missing_slash():
    assert classify("SPACE/CODE") == ClassifiedId("/SPACE/CODE", "identifier")


def test_classify_keeps_existing_slash():
    assert classify("/SPACE/CODE") == ClassifiedId("/SPACE/CODE", "identifier")


def test_classify_strips_and_keeps_value_for_codes():
    assert classify(" MY-OBJECT ") == ClassifiedId("MY-OBJECT", "code")


def test_classify_permid_passthrough():
    cid = classify("20240101000000000-1")
    assert cid.kind == "perm_id"
    assert cid.value == "20240101000000000-1"
