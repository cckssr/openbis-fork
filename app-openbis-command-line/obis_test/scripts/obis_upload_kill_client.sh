#!/usr/bin/env bash
set -euo pipefail
set +e
#export PS4='+ $(date "+%F %T") [${BASH_SOURCE##*/}:${LINENO}] '

# Config you can tweak
DATA_SET_TYPE="${DATA_SET_TYPE:-RAW_DATA}"
FILE_NAME="${FILE_NAME:-test_file.bin}"
FILE_PATH="${FILE_PATH:-test_data/$FILE_NAME}"
KILL_AFTER_SECS="${KILL_AFTER_SECS:-5}"   # kill after this many seconds (first run)
RESTARTS="${RESTARTS:-7}"                 # how many kill/restart cycles
OBIS_BIN="${OBIS_BIN:-obis}"

# Paths
"scripts/common/obis-installed.sh" || true

$OBIS_BIN config -g set openbis_token="$OBIS_TOKEN"

start_upload() {
  echo "Starting upload: sample=$OBIS_SAMPLE_ID type=$DATA_SET_TYPE file=$FILE_PATH"

  "$OBIS_BIN" upload "$OBIS_SAMPLE_ID" "$DATA_SET_TYPE" -f "$FILE_PATH" &
  #expect -c 'spawn -- $env(OBIS_BIN) upload $env(OBIS_SAMPLE_ID) $env(DATA_SET_TYPE) -f $env(FILE_PATH); expect -re "(?i)password"; send -- "changeit\r"; expect eof' &

  up_pid=$!
  set -e
  echo "Upload PID: $up_pid"
}

kill_client() {
  local pid="$1"
  if kill -0 "$pid" 2>/dev/null; then
    echo "Attempting to terminate obis upload PID $pid..."
    kill "$pid" || true
    for i in {1..10}; do
      sleep 0.3
      if ! kill -0 "$pid" 2>/dev/null; then
        echo "PID $pid exited gracefully after $((i * 300))ms."
        return 0
      fi
    done
    echo "PID $pid still alive after grace period. Force killing..."
    kill -9 "$pid" || true
    sleep 0.3
    if kill -0 "$pid" 2>/dev/null; then
      echo "Failed to kill PID $pid (still running)."
      return 1
    else
      echo "PID $pid was force killed with -9."
      return 0
    fi
  else
    echo "Upload process $pid already exited (probably finished before kill)."
  fi
}

# MAIN
restart_count=0

start_upload
sleep "$KILL_AFTER_SECS" || true
if [[ "$RESTARTS" -gt 0 ]]; then
  kill_client "$up_pid" || true
  restart_count=$((restart_count + 1))
fi

while [[ "$restart_count" -lt "$RESTARTS" ]]; do
  start_upload
  sleep "$KILL_AFTER_SECS" || true
  kill_client "$up_pid" || true
  restart_count=$((restart_count + 1))
done

# Final run (without killing) to complete the upload
start_upload
wait "$up_pid"
status=$?

if [[ $status -eq 0 ]]; then
  echo "Upload completed successfully."
else
  echo "Upload failed with exit code $status."
  exit "$status"
fi
