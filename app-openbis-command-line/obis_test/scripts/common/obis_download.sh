#!/usr/bin/env bash
# Download the most recent dataset for the prepared sample
set -euo pipefail

# Config / paths
OBIS_BIN="${OBIS_BIN:-obis}"
LATEST_PY="scripts/common/download_sample.py"

# --- Redirect all normal output to stderr; keep original stdout on fd 3 ---
exec 3>&1 
exec 1>&2 

# Load env (provides OBIS_SAMPLE_ID and OBIS_TOKEN from prepare step)
# shellcheck disable=SC1090

if [[ -z "${OBIS_SAMPLE_ID:-}" ]]; then
  echo "Error: OBIS_SAMPLE_ID not set. Run scripts/prepare_sample.sh first."
  exit 1
fi

# Resolve latest dataset permId for this sample
dataSetId="$( OBIS_SAMPLE_ID="$OBIS_SAMPLE_ID" python3 "$LATEST_PY" )" \
  || { echo "Failed to get latest dataset permId"; exit 1; }

#echo "Latest dataset permId: $dataSetId"

# Download the dataset
"$OBIS_BIN" download "$dataSetId" || {
  echo "obis download failed; inspecting dataset metadata..."
  "$OBIS_BIN" dataset get "$dataSetId" --json | jq '{permId, kind, status, physicalData}' || true
  exit 1
}

printf '%s\n' "$dataSetId" >&3
