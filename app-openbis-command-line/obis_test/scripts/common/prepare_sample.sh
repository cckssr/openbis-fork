#!/usr/bin/env bash
set -euo pipefail
export PS4='+ $(date "+%F %T") [${BASH_SOURCE##*/}:${LINENO}] '

# Paths
PY_SCRIPT="scripts/common/openbis_prepare_sample.py"

# 1) Create/ensure sample and get token (pure JSON on stdout)
info="$(python3 "$PY_SCRIPT")"

# 2) Parse JSON
permId="$(echo "$info" | jq -r '.permId')"
identifier="$(echo "$info" | jq -r '.identifier')"
token="$(echo "$info" | jq -r '.token')"

if [[ -z "$permId" || -z "$token" || "$permId" == "null" || "$token" == "null" ]]; then
  echo "Error: permId or token missing from JSON."
  echo "$info"
  exit 1
fi

# Emit pure JSON to stdout for the caller to parse
printf '%s\n' "$info"

