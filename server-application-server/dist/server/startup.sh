#!/usr/bin/env bash

set -o pipefail

source "$(dirname "$0")/setup-env"

checkNotRoot
"$(dirname "$0")/autosymlink.sh" "$JETTY_BIN_DIR/../webapps/openbis/WEB-INF/lib"

JETTY_HOME="$(cd "$JETTY_BIN_DIR/.." && pwd -P)"
STARTED_MARKER="$JETTY_HOME/SERVER_STARTED"
PID_FILE="${PID_FILE:-$JETTY_HOME/openbis.pid}"
TIMEOUT=120

rm -f "$STARTED_MARKER" "$PID_FILE"

# Start Java in background, output to stdout
stdbuf -oL -eL "$JVM" -DSTOP.PORT="$JETTY_STOP_PORT" \
       -DSTOP.KEY="$JETTY_STOP_KEY" \
       $JAVA_OPTS $JAVA_MEM_OPTS $OPENBIS_OPTS \
       -Dpython.path="$JETTY_LIB_PATH" \
       -Dnative.libpath="$JETTY_BIN_DIR/../webapps/openbis/WEB-INF/lib/native" \
       -jar ../jetty-dist/start.jar --lib=lib/logging/*.jar:webapps/openbis/WEB-INF/lib/*.jar \
       etc/jetty-started.xml "$@" 2>&1 &

echo $! > "$PID_FILE"
JAVA_PID=$!

# Wait for marker file
elapsed=0
while [ $elapsed -lt $TIMEOUT ]; do
    sleep 1
    elapsed=$((elapsed + 1))

    if [ -f "$STARTED_MARKER" ]; then
        echo "Server started (marker found in ${elapsed}s)"
        exit 0
    fi

    if ! isPIDRunning "$JAVA_PID"; then
        echo "Server process died"
        exit 1
    fi
done

echo "Timeout after ${TIMEOUT}s - marker file not found: $STARTED_MARKER"
exit 1