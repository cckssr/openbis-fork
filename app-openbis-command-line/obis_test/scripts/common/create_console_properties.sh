#!/bin/bash

PROP_FILE="$EXTRACTED_PATH/console.properties"

# Absolute paths based on where the script is executed
OPENBIS_DIR="$(pwd)/openbis/servers/openbis"
DSS_DIR="$(pwd)/openbis/servers/openbisdss"

cat > "$PROP_FILE" <<EOF
INSTALL_PATH=$OPENBIS_DIR
DSS_ROOT_DIR=$DSS_DIR
INSTALLATION_TYPE=local
KEY_STORE_PASSWORD=changeit
KEY_PASSWORD=changeit
ELN-LIMS=true
ELN-LIMS-TEMPLATE-TYPES=true
EOF

echo "Wrote $PROP_FILE with:"
echo "  INSTALL_PATH=$OPENBIS_DIR"
echo "  DSS_ROOT_DIR=$DSS_DIR"
