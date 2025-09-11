#!/usr/bin/env bash
# Compare the downloaded file against the source file
set -euo pipefail
export PS4='+ $(date "+%F %T") [${BASH_SOURCE##*/}:${LINENO}] '
set -x

# Inputs (env with sensible defaults)
FILE_NAME="${FILE_NAME:-test_file.bin}"
FILE_NAME_DOWNLOADED="${FILE_NAME_DOWNLOADED:-test_file.bin}"
FILE_PATH="${FILE_PATH:-test_data/$FILE_NAME}"

# Paths
ROOT_DIR="$(cd -- "$(dirname -- "$0")/.." && pwd)"
COMPARE_SCRIPT="scripts/common/compare-files.sh"


# Allow override via arg1 (dataset id), else use env var
dataSetId="${1:-${OBIS_LAST_DATASET_ID:-}}"

if [[ -z "${dataSetId:-}" ]]; then
  echo "Error: No dataset id provided and OBIS_LAST_DATASET_ID not set."
  echo "Usage: scripts/compare_downloaded_file.sh <dataSetId>"
  exit 1
fi

# Expected downloaded path layout
DOWNLOADED_PATH="$dataSetId/original/$FILE_NAME_DOWNLOADED"

echo "$COMPARE_SCRIPT $DOWNLOADED_PATH $FILE_PATH"
# Compare (your script should return non-zero on mismatch)
"$COMPARE_SCRIPT" "$DOWNLOADED_PATH" "$FILE_PATH"
echo "Files match."
