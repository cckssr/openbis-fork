#!/usr/bin/env bash
set -euo pipefail

#-------------#
# Config      #
#-------------#
DATA_SET_TYPE="${DATA_SET_TYPE:-RAW_DATA}"
FILE_NAME="${FILE_NAME:-test_file.bin}"
FILE_PATH="${FILE_PATH:-test_data/$FILE_NAME}"
WORKERS="${WORKERS:-4}"            # number of parallel uploads
STAGGER_SECS="${STAGGER_SECS:-0}"  # delay between starting workers (e.g., 0.2)
OBIS_BIN="${OBIS_BIN:-obis}"       # path to obis CLI

#-------------#
# Setup       #
#-------------#
# Optional helper if you have it:
"scripts/common/obis-installed.sh" || true

# Configure token if provided
if [[ "${OBIS_TOKEN:-}" != "" ]]; then
  "$OBIS_BIN" config -g set openbis_token="$OBIS_TOKEN"
fi

# Basic checks
: "${OBIS_SAMPLE_ID:?Set OBIS_SAMPLE_ID in env}"
[[ -f "$FILE_PATH" ]] || { echo "File not found: $FILE_PATH" >&2; exit 1; }

#-------------#
# Run         #
#-------------#
pids=()

echo "Starting $WORKERS parallel uploads of '$FILE_PATH' to sample '$OBIS_SAMPLE_ID' (type=$DATA_SET_TYPE)"
for ((w=1; w<=WORKERS; w++)); do
  echo "  -> Worker $w starting..."
  "$OBIS_BIN" upload "$OBIS_SAMPLE_ID" "$DATA_SET_TYPE" -f "$FILE_PATH" &
  pids+=("$!")
  if (( w < WORKERS )) && (( $(awk "BEGIN {print ($STAGGER_SECS > 0)}") )); then
    sleep "$STAGGER_SECS"
  fi
done

echo "Waiting for workers to finish..."
status=0
for i in "${!pids[@]}"; do
  pid="${pids[$i]}"
  if wait "$pid"; then
    echo "  -> Worker $((i+1)) completed."
  else
    rc=$?
    echo "  -> Worker $((i+1)) failed with exit code $rc"
    status=1
  fi
done

if [[ $status -eq 0 ]]; then
  echo "All uploads completed successfully."
else
  echo "One or more uploads failed."
fi
exit "$status"
