#!/usr/bin/env sh
# SHUTDOWN_OPENBIS.SH — stop openBIS services
set -eu

# Hardcoded path to your extracted openBIS installation
OPENBIS_PATH="openbis/servers/openbis"

if [ ! -x "$OPENBIS_PATH/bin/alldown.sh" ]; then
  echo "Error: $OPENBIS_PATH/bin/alldown.sh not found or not executable"
  exit 1
fi

echo "Shutting down openBIS at $OPENBIS_PATH"
sh "$OPENBIS_PATH/bin/alldown.sh"
