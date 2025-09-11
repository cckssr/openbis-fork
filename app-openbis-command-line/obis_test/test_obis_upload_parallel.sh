#!/usr/bin/env bash

set -eu
export PS4='+ $(date "+%F %T") [${BASH_SOURCE##*/}:${LINENO}] '
set -x

export OBIS_URL="https://localhost:8443"
export JENKINS_SERVER_OPENBIS_DOWNLOAD="http://openbis-sis-ci-master-jdk17.ethz.ch:8080"

BASE_DIR="$(dirname "$0")"
COMMON_DIR="$BASE_DIR/scripts/common"

DOWNLOAD_OPENBIS_SCRIPT="$COMMON_DIR/download_run_openbis.sh"
CREATE_FILE_SCRIPT="$COMMON_DIR/create_file.sh"
PREPARE_SAMPLE_SCRIPT="$COMMON_DIR/prepare_sample.sh"
PARALLEL_UPLOAD_SCRIPT="$BASE_DIR/scripts/obis_parallel_upload.sh"
DOWNLOAD_DATASET_SCRIPT="$COMMON_DIR/obis_download.sh"
SHUTDOWN_SCRIPT="$COMMON_DIR/shutdown_openbis.sh"
COMPARE_SCRIPT="$COMMON_DIR/compare_downloaded_files.sh"


# Track what actually started, so cleanup knows what to undo
OPENBIS_STARTED=0
TEMP_FILES_CREATED=0
DATASET_DOWNLOADED=0
DELETE_OPENBIS_FOLDER=1

# Always run this (success or failure)
cleanup() {
  local exit_code=$?
  set +e  # never let cleanup fail the cleanup

  echo "=== CLEANUP (exit_code=$exit_code) ==="

  # Kill any leftover background jobs from this script
  jobs -pr | xargs -r kill 2>/dev/null
  sleep 0.2
  jobs -pr | xargs -r kill -9 2>/dev/null

  # If we started openBIS, try shutting it down
  if [[ $OPENBIS_STARTED -eq 1 ]]; then
    echo "Shutdown openBIS…"
    chmod +x "$SHUTDOWN_SCRIPT" 2>/dev/null || true
    ./"$SHUTDOWN_SCRIPT" || true
  fi

  # Optional: remove temp files you created here
  if [[ $TEMP_FILES_CREATED -eq 1 ]]; then
    rm -f test_data/test_file.bin || true
  fi

  # Optional: clean downloaded dataset dir if you want
  if [[ $DATASET_DOWNLOADED -eq 1 ]]; then
    rm -rf "$OBIS_LAST_DATASET_ID" || true
  fi

  if [[ $DATASET_DOWNLOADED -eq 1 ]]; then
    rm -rf "$OBIS_LAST_DATASET_ID" || true
  fi

  if [[ $DELETE_OPENBIS_FOLDER -eq 1 ]]; then
    rm -rf openbis || true
  fi

  echo "=== CLEANUP DONE ==="
  exit "$exit_code"
}

# Run cleanup on: script exit, any error, or signals
trap cleanup EXIT
trap 'exit 2' INT TERM


echo "=== STEP 1: Download & start openBIS ==="
chmod +x "$DOWNLOAD_OPENBIS_SCRIPT"
./"$DOWNLOAD_OPENBIS_SCRIPT"

echo "=== STEP 2: Create File to upload ==="
chmod +x "$CREATE_FILE_SCRIPT"
./"$CREATE_FILE_SCRIPT" 1G test_data/test_file.bin
TEMP_FILES_CREATED=1

echo "=== STEP 3: Prepare the Sample ==="
info="$(bash "$PREPARE_SAMPLE_SCRIPT")"

permId=$(jq -r '.permId' <<<"$info")
token=$(jq -r '.token' <<<"$info")
identifier=$(jq -r '.identifier' <<<"$info")

export OBIS_SAMPLE_ID="$permId" OBIS_TOKEN="$token" OBIS_SAMPLE_IDENTIFIER="$identifier"

# Configure obis token (global)
obis config -g set openbis_token="$token"

echo "=== STEP 4: Run parallel uploads==="
chmod +x "$PARALLEL_UPLOAD_SCRIPT"
./"$PARALLEL_UPLOAD_SCRIPT"

echo "=== STEP 5: Download all datasets ==="
chmod +x "$DOWNLOAD_DATASET_SCRIPT"
dataSetId="$(bash "$DOWNLOAD_DATASET_SCRIPT")"
export OBIS_LAST_DATASET_ID="$dataSetId"
DATASET_DOWNLOADED=1

echo "=== STEP 6: Shutdown openBIS ==="
chmod +x "$SHUTDOWN_SCRIPT"
./"$SHUTDOWN_SCRIPT"

echo "=== STEP 7: Compare Files ==="
chmod +x "$COMPARE_SCRIPT"
./"$COMPARE_SCRIPT"

echo "Wow! Cycle completed successfully. obis still works!"
