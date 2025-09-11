#!/bin/bash
set -euo pipefail

orig="${1:?usage: $0 <original-file> <downloaded-file>}"
copy="${2:?usage: $0 <original-file> <downloaded-file>}"

# quick existence + size check
[[ -f "$orig" && -f "$copy" ]] || { echo "File missing."; exit 2; }
size1=$(stat -c%s "$orig" 2>/dev/null || stat -f%z "$orig")
size2=$(stat -c%s "$copy" 2>/dev/null || stat -f%z "$copy")
if [[ "$size1" != "$size2" ]]; then
  echo "DIFFER (size mismatch: $size1 vs $size2)"; exit 1
fi

# fastest exact compare
if cmp -s "$orig" "$copy"; then
  echo "MATCH"; exit 0
fi

# fallback: SHA-256 (useful if cmp unavailable across environments)
hash() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print $1}'
  elif command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$1" | awk '{print $1}'
  elif command -v openssl >/dev/null 2>&1; then
    openssl dgst -sha256 -r "$1" | awk '{print $1}'
  else
    echo "no-hash-tool"
  fi
}
h1=$(hash "$orig"); h2=$(hash "$copy")
if [[ "$h1" != "no-hash-tool" && "$h1" == "$h2" ]]; then
  echo "MATCH (by SHA-256)"; exit 0
else
  echo "DIFFER"; exit 1
fi

