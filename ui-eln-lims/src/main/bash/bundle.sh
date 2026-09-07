#!/bin/bash

# Check if at least two arguments are provided (output + at least one input)
if [ "$#" -lt 2 ]; then
    echo "Usage: $0 <output_file> <input_file1> [input_file2 ...]"
    exit 1
fi

# 1. First parameter is the output file
OUTPUT_FILE="$1"

# Shift removes the first argument ($1), so $@ now contains ONLY the input files
shift

# 2. Prepare the output directory and remove any old bundle
mkdir -p "$(dirname "$OUTPUT_FILE")"
rm -f "$OUTPUT_FILE"

echo "Bundling files into $OUTPUT_FILE..."

# 3. Loop through all remaining arguments ($@)
for FILE in "$@"; do
    if [ -f "$FILE" ]; then
        # Append the file contents
        cat "$FILE" >> "$OUTPUT_FILE"

        # Append a newline and a semicolon to prevent syntax collisions
        echo -e "\n;" >> "$OUTPUT_FILE"

        echo "Added $FILE"
    else
        echo "Error: $FILE not found."
        exit 1
    fi
done

echo "Bundling complete for $OUTPUT_FILE!"