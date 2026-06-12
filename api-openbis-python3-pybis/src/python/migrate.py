#!/usr/bin/env python3
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
"""Rewrite pybis 1.x call sites to the pybis 7 API.

Usage::

    python migrate.py --dry-run path/to/project/   # unified diffs to stdout
    python migrate.py --write   path/to/project/   # rewrite files in place
    python migrate.py --report  path/to/project/   # summary only

Transformations:

- method renames (``get_samples(`` → ``search_objects(`` and friends),
- keyword renames (``permId=`` → ``perm_id=``, ``sample=`` → ``object=``,
  ...) inside recognized pybis calls,
- ``withParents=``/``withChildren=``: literal booleans become the
  ``with_parents``/``with_children`` fetch flags, anything else becomes the
  ``parents``/``children`` relationship filter,
- removal of ``only_data``/``raw_response``/``use_cache``/``attrs``
  arguments (flagged for review — the call's return shape changes),
- import rewrites (``from pybis.sample import Sample`` →
  ``from pybis.entities import Object as Sample``).

Ambiguous spots receive a ``# TODO[pybis-migrate]: ...`` comment instead of
a possibly wrong rewrite. The rename tables come straight from
``pybis._compat`` so the codemod can never drift from the shims.
"""

from __future__ import annotations

import argparse
import difflib
import sys
from pathlib import Path

import libcst as cst

from pybis._compat import (
    _LEGACY_POSITIONAL,
    _METHOD_RENAMES,
    _PARAM_RENAMES,
    _PARAM_SHIMMED,
    _PER_METHOD_PARAM_RENAMES,
    _REMOVED_PARAMS,
    _VALUE_DEPENDENT,
)

#: Imports that moved; usages keep working via an alias.
_IMPORT_RENAMES: dict[tuple[str, str], tuple[str, str]] = {
    ("pybis.pybis", "Openbis"): ("pybis", "Openbis"),
    ("pybis.sample", "Sample"): ("pybis.entities", "Object"),
    ("pybis.experiment", "Experiment"): ("pybis.entities", "Collection"),
    ("pybis.entity_type", "SampleType"): ("pybis.entities", "ObjectType"),
    ("pybis.entity_type", "ExperimentType"): ("pybis.entities", "CollectionType"),
}

#: Calls whose keyword arguments we are allowed to touch.
_KNOWN_METHODS = (
    set(_METHOD_RENAMES)
    | set(_METHOD_RENAMES.values())
    | set(_PARAM_SHIMMED)
    | {
        "new_object",
        "new_collection",
        "assign_role",
        "get_object",
        "get_collection",
        "get_space",
        "get_project",
        "get_person",
        "get_group",
        "get_role_assignment",
        "get_term",
        "get_vocabulary",
        "get_tag",
        "get_plugin",
        "get_property_type",
        "new_project",
        "new_space",
        "search_files",
        "delete_content_copy",
        "get_or_create_personal_access_token",
    }
)

#: Parameters the 7.x search/get methods understand; anything else that we
#: did not translate is probably a 1.x free-form property filter.
_NEW_PARAM_WHITELIST = frozenset(
    """type space project collection object id perm_id identifier code tags
    properties hierarchy_properties parents children with_parents
    with_children registration_date modification_date registrator modifier
    count start_with page_size is_finished kind status user_id person group
    vocabulary description files folder zip_file dss_code label session_name
    valid_from valid_to force save_to_disk with_vocabulary with_script
    user_ids attachments reason permanently""".split()
)

_MAGIC_PREFIXES = (">", "<", "=")

_TODO_PREFIX = "# TODO[pybis-migrate]:"


def _is_bool_literal(node: cst.BaseExpression) -> bool:
    return isinstance(node, cst.Name) and node.value in ("True", "False")


class PybisMigrator(cst.CSTTransformer):
    """Apply the pybis 1.x → 7 rename rules to one module."""

    def __init__(self) -> None:
        self.changes: dict[str, int] = {}
        self._pending_todos: list[str] = []

    def _count(self, rule: str) -> None:
        self.changes[rule] = self.changes.get(rule, 0) + 1

    def _todo(self, message: str) -> None:
        self._pending_todos.append(message)

    # --- calls -----------------------------------------------------------------

    def leave_Call(self, original: cst.Call, updated: cst.Call) -> cst.Call:
        func = updated.func
        if not isinstance(func, cst.Attribute):
            return updated
        method_name = func.attr.value

        new_name = _METHOD_RENAMES.get(method_name)
        if new_name is not None:
            updated = updated.with_changes(
                func=func.with_changes(attr=cst.Name(new_name))
            )
            self._count(f"method {method_name} -> {new_name}")
            effective = method_name  # per-method tables key on the old name
        elif method_name in _KNOWN_METHODS:
            effective = method_name
        else:
            return updated

        per_method = _PER_METHOD_PARAM_RENAMES.get(effective, {})

        # 1.x positional call styles: the 7.x methods are keyword-only after
        # the first argument, so positionals beyond the first must become
        # keywords (the legacy positional order is known).
        positional_names = _LEGACY_POSITIONAL.get(effective, [])
        positional_seen = sum(1 for a in updated.args if a.keyword is None)
        if positional_seen > 1 and positional_names:
            converted = []
            index = 0
            for arg in updated.args:
                if arg.keyword is None and index < len(positional_names):
                    if index > 0:  # first positional stays positional
                        arg = arg.with_changes(
                            keyword=cst.Name(positional_names[index]),
                            equal=cst.AssignEqual(
                                whitespace_before=cst.SimpleWhitespace(""),
                                whitespace_after=cst.SimpleWhitespace(""),
                            ),
                        )
                    index += 1
                converted.append(arg)
            updated = updated.with_changes(args=converted)
            self._count("positional arguments converted to keywords")

        new_args: list[cst.Arg] = []
        for arg in updated.args:
            if arg.keyword is None:
                new_args.append(arg)
                continue
            key = arg.keyword.value

            if key in _REMOVED_PARAMS:
                self._count(f"removed {key}=")
                self._todo(
                    f"removed argument {key}= dropped from {effective}();"
                    f" the call now returns entities — review the usage"
                )
                continue

            if key in per_method:
                target = per_method[key]
                if target is None:
                    self._count(f"absorbed {key}=")
                    continue
                new_args.append(arg.with_changes(keyword=cst.Name(target)))
                self._count(f"param {key}= -> {target}=")
                continue

            if key in _VALUE_DEPENDENT:
                fetch_flag, relation_filter = _VALUE_DEPENDENT[key]
                if _is_bool_literal(arg.value):
                    target = fetch_flag
                else:
                    target = relation_filter
                    if not isinstance(
                        arg.value, (cst.SimpleString, cst.List, cst.Tuple)
                    ):
                        self._todo(
                            f"{key}= became {target}= (relationship filter);"
                            f" pass with_{key[4:].lower()}=True if a fetch"
                            f" flag was intended"
                        )
                new_args.append(arg.with_changes(keyword=cst.Name(target)))
                self._count(f"param {key}= -> {target}=")
                continue

            if key == "where":
                self._warn_on_magic_values(arg.value)
                new_args.append(arg.with_changes(keyword=cst.Name("properties")))
                self._count("param where= -> properties=")
                continue

            if key == "properties":
                self._warn_on_magic_values(arg.value)
                new_args.append(arg)
                continue

            if key in _PARAM_RENAMES:
                target = _PARAM_RENAMES[key]
                new_args.append(arg.with_changes(keyword=cst.Name(target)))
                self._count(f"param {key}= -> {target}=")
                continue

            if (
                new_name is not None
                and new_name.startswith("search_")
                and key not in _NEW_PARAM_WHITELIST
            ):
                self._todo(
                    f"keyword {key}= looks like a 1.x free-form property"
                    f" filter; move it into properties={{...}}"
                )
            new_args.append(arg)

        # drop a trailing comma left behind by removed arguments
        if new_args and updated.args and len(new_args) != len(updated.args):
            new_args[-1] = new_args[-1].with_changes(comma=cst.MaybeSentinel.DEFAULT)
        return updated.with_changes(args=new_args)

    def _warn_on_magic_values(self, node: cst.BaseExpression) -> None:
        """Flag 1.x magic operator strings inside a properties/where dict."""
        if not isinstance(node, cst.Dict):
            return
        for element in node.elements:
            if not isinstance(element, cst.DictElement):
                continue
            value = element.value
            if isinstance(value, cst.SimpleString) and value.value[1:].lstrip(
                "'\""
            ).startswith(_MAGIC_PREFIXES):
                self._todo(
                    "magic operator string in properties — use"
                    " pybis.api.filters (e.g. filters.gte(3)) instead"
                )

    # --- imports ----------------------------------------------------------------

    def leave_ImportFrom(
        self, original: cst.ImportFrom, updated: cst.ImportFrom
    ) -> cst.ImportFrom:
        if updated.module is None or isinstance(updated.names, cst.ImportStar):
            return updated
        module = cst.Module(body=[]).code_for_node(updated.module)
        new_names = []
        new_module = None
        changed = False
        for alias in updated.names:
            name = alias.name.value if isinstance(alias.name, cst.Name) else None
            mapping = _IMPORT_RENAMES.get((module, name)) if name else None
            if mapping is None:
                new_names.append(alias)
                continue
            target_module, target_name = mapping
            changed = True
            self._count(f"import {module}.{name} -> {target_module}.{target_name}")
            keep_as = alias.asname
            if keep_as is None and target_name != name:
                # preserve the old local name so usages keep working
                keep_as = cst.AsName(name=cst.Name(name))
                self._todo(
                    f"'{name}' is now '{target_name}'; the alias keeps old"
                    f" code working — consider renaming usages"
                )
            new_names.append(
                cst.ImportAlias(name=cst.Name(target_name), asname=keep_as)
            )
            new_module = target_module
        if not changed:
            return updated
        if new_module is not None:
            parts = new_module.split(".")
            node: cst.BaseExpression = cst.Name(parts[0])
            for part in parts[1:]:
                node = cst.Attribute(value=node, attr=cst.Name(part))
            updated = updated.with_changes(module=node)
        return updated.with_changes(names=new_names)

    # --- TODO comments -------------------------------------------------------------

    def leave_SimpleStatementLine(
        self,
        original: cst.SimpleStatementLine,
        updated: cst.SimpleStatementLine,
    ) -> cst.SimpleStatementLine:
        if not self._pending_todos:
            return updated
        todos = self._pending_todos
        self._pending_todos = []
        comment_lines = tuple(
            cst.EmptyLine(comment=cst.Comment(f"{_TODO_PREFIX} {message}"))
            for message in todos
        )
        return updated.with_changes(
            leading_lines=tuple(updated.leading_lines) + comment_lines
        )


def migrate_source(source: str) -> tuple[str, dict[str, int]]:
    """Transform one module's source; returns (new_source, change counts)."""
    module = cst.parse_module(source)
    migrator = PybisMigrator()
    new_module = module.visit(migrator)
    return new_module.code, migrator.changes


def _iter_python_files(target: Path) -> list[Path]:
    if target.is_file():
        return [target]
    return sorted(target.rglob("*.py"))


def main(argv: list[str] | None = None) -> int:
    """CLI entry point; returns the exit code."""
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--dry-run", action="store_true", help="print diffs only")
    mode.add_argument("--write", action="store_true", help="rewrite files in place")
    mode.add_argument("--report", action="store_true", help="print a summary only")
    parser.add_argument("target", type=Path, help="file or directory to migrate")
    args = parser.parse_args(argv)

    total_changes = 0
    changed_files = 0
    for path in _iter_python_files(args.target):
        source = path.read_text()
        try:
            new_source, changes = migrate_source(source)
        except cst.ParserSyntaxError as error:
            print(f"SKIP {path}: {error}", file=sys.stderr)
            continue
        if not changes:
            continue
        changed_files += 1
        total_changes += sum(changes.values())
        if args.report:
            print(f"{path}:")
            for rule, count in sorted(changes.items()):
                print(f"  {count:4d}  {rule}")
        elif args.dry_run:
            diff = difflib.unified_diff(
                source.splitlines(keepends=True),
                new_source.splitlines(keepends=True),
                fromfile=str(path),
                tofile=f"{path} (migrated)",
            )
            sys.stdout.writelines(diff)
        elif args.write:
            path.write_text(new_source)
            print(f"rewrote {path} ({sum(changes.values())} changes)")

    print(
        f"\n{changed_files} file(s) with {total_changes} change(s)"
        + ("" if args.write else " (nothing written)"),
        file=sys.stderr,
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
