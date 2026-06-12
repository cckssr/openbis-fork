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
"""Tests for the migrate.py codemod (transformation rules + CLI)."""

import difflib
import subprocess
import sys
from pathlib import Path

import pytest

SRC_ROOT = Path(__file__).parents[2]
FIXTURES = Path(__file__).parent / "fixtures"

sys.path.insert(0, str(SRC_ROOT))

from migrate import migrate_source  # noqa: E402


@pytest.mark.parametrize(
    "fixture", sorted((FIXTURES / "before").glob("*.py")), ids=lambda p: p.name
)
def test_migration_fixture(fixture):
    before = fixture.read_text()
    expected = (FIXTURES / "after" / fixture.name).read_text()
    actual, _changes = migrate_source(before)
    assert actual == expected, "\n" + "\n".join(
        difflib.unified_diff(
            expected.splitlines(), actual.splitlines(), lineterm=""
        )
    )


def test_transformed_fixtures_are_valid_python():
    for fixture in (FIXTURES / "after").glob("*.py"):
        compile(fixture.read_text(), str(fixture), "exec")


def test_unrelated_code_is_untouched():
    source = "import json\n\nresult = api.get_items(permId='x')\n"
    actual, changes = migrate_source(source)
    assert actual == source  # get_items is not a pybis method
    assert changes == {}


def test_method_rename_counts_reported():
    _, changes = migrate_source("o.get_samples(space='X')\n")
    assert changes == {"method get_samples -> search_objects": 1}


def test_todo_comment_inserted_for_ambiguous_case():
    out, _ = migrate_source("x = o.get_samples(withParents=flag)\n")
    assert "TODO[pybis-migrate]" in out
    assert "parents=flag" in out


def test_magic_string_in_properties_flagged():
    out, _ = migrate_source("o.get_samples(where={'AGE': '>= 5'})\n")
    assert "TODO[pybis-migrate]" in out
    assert "filters" in out


# --- CLI ---------------------------------------------------------------------


def run_migrate_cli(args, cwd):
    return subprocess.run(
        [sys.executable, str(SRC_ROOT / "migrate.py"), *args],
        capture_output=True,
        text=True,
        cwd=cwd,
    )


def test_dry_run_does_not_write(tmp_path):
    f = tmp_path / "test.py"
    f.write_text("o.get_samples()\n")
    result = run_migrate_cli(["--dry-run", str(tmp_path)], cwd=SRC_ROOT)
    assert result.returncode == 0
    assert f.read_text() == "o.get_samples()\n"  # unchanged
    assert "search_objects" in result.stdout  # diff shown


def test_write_modifies_file(tmp_path):
    f = tmp_path / "test.py"
    f.write_text("o.get_samples()\n")
    result = run_migrate_cli(["--write", str(tmp_path)], cwd=SRC_ROOT)
    assert result.returncode == 0
    assert f.read_text() == "o.search_objects()\n"


def test_report_mode_summarizes_without_writing(tmp_path):
    f = tmp_path / "test.py"
    f.write_text("o.get_samples(permId='x')\n")
    result = run_migrate_cli(["--report", str(tmp_path)], cwd=SRC_ROOT)
    assert result.returncode == 0
    assert f.read_text() == "o.get_samples(permId='x')\n"
    assert "method get_samples -> search_objects" in result.stdout
    assert "param permId= -> perm_id=" in result.stdout


def test_broken_file_is_skipped_not_fatal(tmp_path):
    good = tmp_path / "good.py"
    good.write_text("o.get_samples()\n")
    bad = tmp_path / "bad.py"
    bad.write_text("def broken(:\n")
    result = run_migrate_cli(["--write", str(tmp_path)], cwd=SRC_ROOT)
    assert result.returncode == 0
    assert "SKIP" in result.stderr
    assert good.read_text() == "o.search_objects()\n"
