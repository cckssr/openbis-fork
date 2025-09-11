#!/usr/bin/env sh
# download_run_openbis.sh — download & run the latest openBIS installer from Jenkins
set -eu

JOB_BASE="$JENKINS_SERVER_OPENBIS_DOWNLOAD/view/Master/job/app-openbis-installer"
OPENBIS_INSTALLER_DIST_DIR="$JOB_BASE/lastSuccessfulBuild/artifact/app-openbis-installer/targets/gradle/distributions/"

DEST_DIR="openbis/install"
OPENBIS_DIR="../../servers/openbis"
CONFIG_DIR="configs"   # folder where your console.properties lives
OPENBIS_INSTALLER_NO_DOWNLOAD=0

# Check for flag
if [ "${1:-}" = "--no-download" ]; then
  OPENBIS_INSTALLER_NO_DOWNLOAD=1
  shift
fi

mkdir -p "$DEST_DIR"

if [ "$OPENBIS_INSTALLER_NO_DOWNLOAD" -eq 0 ]; then
  echo "Finding latest artifact from directory listing…"

  FILE_NAME="$(
    curl -fsSL "$OPENBIS_INSTALLER_DIST_DIR" \
    | grep -oE 'openBIS-installation-standard-technologies-SNAPSHOT-[A-Za-z0-9._-]+\.tar\.gz' \
    | sort -u \
    | head -n 1
  )" || true

  if [ -z "${FILE_NAME:-}" ]; then
    echo "Error: Could not find the installer tarball."
    exit 1
  fi

  DL_URL="$OPENBIS_INSTALLER_DIST_DIR$FILE_NAME"
  OUT_NAME="$FILE_NAME"

  echo "Downloading: $DL_URL"
  curl -fL --retry 3 --retry-delay 2 -o "$DEST_DIR/$OUT_NAME" "$DL_URL"

  echo "Extracting to: $DEST_DIR"
  tar -xzf "$DEST_DIR/$OUT_NAME" -C "$DEST_DIR"
else
  echo "Skipping download and extract (manual mode)"
fi




# find the extracted top-level directory
EXTRACTED_DIR="$(find "$DEST_DIR" -mindepth 1 -maxdepth 1 -type d -printf '%f\n' | head -n1)"
export EXTRACTED_PATH="$DEST_DIR/$EXTRACTED_DIR"

echo "Using extracted directory: $EXTRACTED_PATH"

echo "Create console.properties in $EXTRACTED_PATH"
#cp "$CONFIG_DIR/console.properties" "$EXTRACTED_PATH/"
./scripts/common/create_console_properties.sh

echo "Starting openBIS with run-console.sh"
cd "$EXTRACTED_PATH"
printf "changeit\nchangeit\n" | ./run-console.sh



echo "Starting openBIS with bin/allup.sh"
cd "$OPENBIS_DIR"
sh bin/allup.sh
