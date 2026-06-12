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
"""MIGRATION_GUIDE.md must cover every entry of the compat tables."""

from pathlib import Path

import pytest

from pybis._compat import (
    _METHOD_RENAMES,
    _PARAM_RENAMES,
    _PARAM_SHIMMED,
    _REMOVED_PARAMS,
)

GUIDE = (Path(__file__).parents[2] / "MIGRATION_GUIDE.md").read_text()


@pytest.mark.parametrize("old,new", sorted(_METHOD_RENAMES.items()))
def test_method_rename_documented(old, new):
    assert old in GUIDE, f"old method {old} missing from MIGRATION_GUIDE.md"
    assert new in GUIDE, f"new method {new} missing from MIGRATION_GUIDE.md"


@pytest.mark.parametrize("old,new", sorted(_PARAM_RENAMES.items()))
def test_param_rename_documented(old, new):
    assert old in GUIDE, f"old parameter {old} missing from MIGRATION_GUIDE.md"
    assert new in GUIDE, f"new parameter {new} missing from MIGRATION_GUIDE.md"


@pytest.mark.parametrize("removed", sorted(_REMOVED_PARAMS))
def test_removed_param_documented(removed):
    assert removed in GUIDE, f"removed parameter {removed} missing from guide"


@pytest.mark.parametrize("name", sorted(_PARAM_SHIMMED))
def test_param_shimmed_method_documented(name):
    assert name in GUIDE, f"param-shimmed method {name} missing from guide"
