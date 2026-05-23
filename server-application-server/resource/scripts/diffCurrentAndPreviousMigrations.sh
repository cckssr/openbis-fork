#!/usr/bin/env bash
set -e

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"/../..

SQL_BASE="$BASE_DIR/source/sql/openbis/postgresql"
GENERIC_BASE="$BASE_DIR/source/sql/openbis/generic"
TEST_BASE="$BASE_DIR/sourceTest/sql/openbis/postgresql"

missing_found=0
diff_found=0

if [ ! -d "$SQL_BASE" ]; then
  echo "Missing directory: $SQL_BASE" >&2
  exit 1
fi

versions="$(find "$SQL_BASE" -maxdepth 1 -mindepth 1 -type d -printf '%f\n' | grep -E '^[0-9]+$' || true)"
if [ -z "$versions" ]; then
  echo "No version directories found under $SQL_BASE" >&2
  exit 1
fi

curr="$(printf "%s\n" $versions | sort -n | tail -n 1)"
prev="$((10#$curr - 1))"

curr_fmt="$(printf "%03d" "$curr")"
prev_fmt="$(printf "%03d" "$prev")"

echo "Will check the diff between versions $prev_fmt and $curr_fmt"

copy_dir() {
  local base_dir="$1"

  shopt -s nullglob
  for f in "$base_dir/$prev_fmt"/*; do
    filename="$(basename "$f")"

    # Replace version in filename: -207.sql or -207.sql → -208.sql
    new_filename="$(echo "$filename" | sed -E "s/-$prev_fmt\.sql$/-$curr_fmt.sql/")"


    new_f="$base_dir/$curr_fmt/$new_filename"

    if [[ -f "$new_f" ]]; then
      if ! git diff --quiet --no-index "$f" "$new_f"; then
        git diff --no-index "$f" "$new_f"
        diff_found=1
      fi
    else
      echo "Missing file: $new_f"
      missing_found=1
    fi
  done
}



copy_dir "$SQL_BASE"
copy_dir "$GENERIC_BASE"
copy_dir "$TEST_BASE"

if [[ $missing_found -eq 0 && $diff_found -eq 0 ]]; then
  echo "No differences found and no missing files."
fi
