#!/usr/bin/env bash
set -e

# Name of the Python package
PKG="obis"

# Function to log messages
log() {
  echo "$(date '+%Y-%m-%d %H:%M:%S') [$1] $2"
}

# Check if pip is available
if ! command -v pip3 &>/dev/null; then
  log "ERROR" "pip3 is not installed. Please install Python3 and pip."
  exit 1
fi

# Check if pybis is installed
if ! pip3 show "$PKG" &>/dev/null; then
  log "INFO" "Package $PKG not found. Installing..."
  pip3 install --upgrade "$PKG"
  log "INFO" "$PKG installed successfully."
else
  log "INFO" "$PKG is already installed."
fi

# Run your Python script (adjust path if needed)
# SCRIPT="script.py"
# if [[ -f "$SCRIPT" ]]; then
#   log "INFO" "Running $SCRIPT..."
#   python3 "$SCRIPT" "$@"
# else
#   log "ERROR" "$SCRIPT not found in current directory."
#   exit 1
# fi
