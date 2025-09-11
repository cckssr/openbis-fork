#!/usr/bin/env sh
# make_file.sh — always create a real file with random content
set -eu

SIZE="${1:-3G}"          # default size = 1G
OUTPUT_FILE="${2:-test_data/test_file.bin}"

echo "Creating random file of size $SIZE: $OUTPUT_FILE"

# Convert size (e.g. 1G, 500M) into MB count
BYTES=$(numfmt --from=iec "$SIZE")
COUNT=$(( BYTES / 1048576 ))

dd if=/dev/urandom of="$OUTPUT_FILE" bs=1M count="$COUNT" status=progress

echo "Done. File created:"
ls -lh "$OUTPUT_FILE"
