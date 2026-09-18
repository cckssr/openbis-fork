#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

"$SCRIPT_DIR/eln.bundle.js.sh"
"$SCRIPT_DIR/eln.bundle.lib.sh"