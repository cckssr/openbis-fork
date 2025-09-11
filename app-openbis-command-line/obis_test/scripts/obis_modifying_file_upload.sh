#!/usr/bin/env bash
set -euo pipefail

#-------------#
# Config      #
#-------------#
DATA_SET_TYPE="${DATA_SET_TYPE:-RAW_DATA}"
FILE_NAME="${FILE_NAME:-test_file.bin}"
FILE_PATH="${FILE_PATH:-test_data/$FILE_NAME}"
OBIS_BIN="${OBIS_BIN:-obis}"       # path to obis CLI

# Modification timing/size
MODIFY_DELAY_SECS="${MODIFY_DELAY_SECS:-5}"  # wait before modifying (seconds)
APPEND_BYTES="${APPEND_BYTES:-655361}"          # bytes to append at the end (default 64 KiB)
EDIT_COPY_SUFFIX="${EDIT_COPY_SUFFIX:-.editing}"

#-------------#
# Setup       #
#-------------#
# Optional helper if you have it:
"scripts/common/obis-installed.sh" || true
#
#OBIS_SAMPLE_ID="20250909123821708-108"
#OBIS_TOKEN='$pat-admin-250909123820990x02AF1F588CB815BE6FD24E5484B7325F'

# Configure token if provided
if [[ "${OBIS_TOKEN:-}" != "" ]]; then
  "$OBIS_BIN" config -g set openbis_token="$OBIS_TOKEN"
fi

# Basic checks
: "${OBIS_SAMPLE_ID:?Set OBIS_SAMPLE_ID in env}"
[[ -f "$FILE_PATH" ]] || { echo "File not found: $FILE_PATH" >&2; exit 1; }

# Prepare editable copy
EDIT_FILE_PATH="${FILE_PATH}${EDIT_COPY_SUFFIX}"
cp "$FILE_PATH" "$EDIT_FILE_PATH"

#-------------#
# Run         #
#-------------#
# Kick off a background modifier that appends bytes to the end mid-upload.

MODIFY_RUNS="${MODIFY_RUNS:-3}"  # default 10

(
  for i in $(seq 1 "$MODIFY_RUNS"); do
    echo "-> [$i/$MODIFY_RUNS] Wait ${MODIFY_DELAY_SECS}s before modifying '$EDIT_FILE_PATH'..."
    sleep "$MODIFY_DELAY_SECS"

    echo "-> [$i/$MODIFY_RUNS] Appending $APPEND_BYTES bytes to '$EDIT_FILE_PATH'..."
    # append bytes; '>>' keeps existing content
    dd if=/dev/zero bs="$APPEND_BYTES" count=1 status=none >> "$EDIT_FILE_PATH"

    # ensure data hits disk (best-effort)
    sync || true
    echo "-> [$i/$MODIFY_RUNS] Modification done."
  done
  echo "-> All $MODIFY_RUNS modifications completed."
) &
modifier_pid=$!




echo "Starting upload of '$EDIT_FILE_PATH' to sample '$OBIS_SAMPLE_ID' (type=$DATA_SET_TYPE)"
set +e
"$OBIS_BIN" upload "$OBIS_SAMPLE_ID" "$DATA_SET_TYPE" -f "$EDIT_FILE_PATH"
rc=$?
set -e

# Ensure modifier finished
if kill -0 "$modifier_pid" 2>/dev/null; then
  wait "$modifier_pid" || true
fi

