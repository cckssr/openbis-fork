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
"""Golden tests freezing the create/update JSON-RPC payloads.

Regenerate fixtures intentionally (after reviewing the diff!) with::

    PYBIS_REGEN_GOLDENS=1 pytest tests/unit/test_payload_snapshots.py
"""

import json
import os
from pathlib import Path

import pytest

from payload_scenarios import build_scenarios

GOLDEN_DIR = Path(__file__).parent / "fixtures" / "payloads"

SCENARIOS = build_scenarios()


@pytest.mark.parametrize("name", sorted(SCENARIOS))
def test_payload_matches_golden(name):
    payload = json.loads(json.dumps(SCENARIOS[name], sort_keys=True))
    golden_file = GOLDEN_DIR / f"{name}.json"

    if os.environ.get("PYBIS_REGEN_GOLDENS"):
        GOLDEN_DIR.mkdir(parents=True, exist_ok=True)
        golden_file.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n")

    assert golden_file.exists(), (
        f"missing golden fixture {golden_file.name}; run with"
        f" PYBIS_REGEN_GOLDENS=1 and review the generated payload"
    )
    expected = json.loads(golden_file.read_text())
    assert payload == expected, (
        f"payload for {name!r} drifted from the golden fixture —"
        f" if intentional, regenerate with PYBIS_REGEN_GOLDENS=1 and review"
    )
