#!/usr/bin/env bash
# Startup script for CISD openBIS Application Server on Unix / Linux systems
# Streams console to stdout and detects readiness via message or marker file.

awkBin()
{
  # We need a awk that accepts variable assignments with '-v'
  case `uname -s` in
    "SunOS")
      echo "nawk"
      return
      ;;
  esac
  # default
  echo "awk"
}

set -o pipefail

# --- env/bootstrap ----------------------------------------------------------
source "$(dirname "$0")/setup-env"

checkNotRoot
"$(dirname "$0")/autosymlink.sh" "$JETTY_BIN_DIR/../webapps/openbis/WEB-INF/lib"

JETTY_HOME="$(cd "$JETTY_BIN_DIR/.." && pwd -P)"
STARTED_MARKER="$JETTY_HOME/SERVER_STARTED"
MAX_LOOPS=${MAX_LOOPS:-120}

STARTED_MESSAGE="${STARTED_MESSAGE:-SERVER STARTED}"
ERROR_MESSAGE="${ERROR_MESSAGE:-ERROR}"

READY_FLAG="$(mktemp -t openbis_ready.XXXXXX)"
ERROR_FLAG="$(mktemp -t openbis_error.XXXXXX)"
trap 'rm -f "$READY_FLAG" "$ERROR_FLAG"' EXIT

# how long to keep the console phase alive while waiting (seconds)
CONSOLE_WAIT=${CONSOLE_WAIT:-60}



AWK_BIN=$(awkBin)   # or just 'awk'

# Launch server in a subshell so we can capture the Java PID and keep the pipe open
(
  # Start Java (line-buffered) in background
  stdbuf -oL -eL "$JVM" -DSTOP.PORT="$JETTY_STOP_PORT" \
         -DSTOP.KEY="$JETTY_STOP_KEY" \
         $JAVA_OPTS $JAVA_MEM_OPTS \
         $OPENBIS_OPTS \
         -Dpython.path="$JETTY_LIB_PATH" \
         -Dnative.libpath="$JETTY_BIN_DIR/../webapps/openbis/WEB-INF/lib/native" \
         -jar ../jetty-dist/start.jar --lib=lib/logging/*.jar:webapps/openbis/WEB-INF/lib/*.jar \
         etc/jetty-started.xml \
     "$@" 2>&1 &

  CHILD=$!
  #echo "$CHILD" > "$PID_FILE"
  # Keep the pipe open until Java exits (important so tee/awk keep receiving)
  wait "$CHILD"
) | tee >(
      "$AWK_BIN" -v s="$SUCCESS_MSG" -v r="$READY_FLAG" -v e="$ERROR_FLAG" -v err="ERROR" '
        # mark error immediately if you want fail-fast
        index($0, err) { system("touch " e); fflush(); exit }
        # mark ready on success line
        index($0, s)   { system("touch " r); fflush(); exit }
        { fflush() }
      ' >/dev/null
    ) &
PIPE_PID=$!

# Poll for initial state (ready/error/child exit) up to MAX_LOOPS seconds
n=0
while [ $n -lt "$MAX_LOOPS" ]; do
  sleep 1

  # Success?
  if [ -f "$READY_FLAG" ]; then
    echo "(pid $(cat "$PID_FILE" 2>/dev/null))"
    exit 0
  fi

  # Error?
  if [ -f "$ERROR_FLAG" ]; then
    echo "FAILED"
    exit 1
  fi

  # Child died? (pid file vanished or process not running)
  if [ ! -f "$PID_FILE" ]; then
    echo "FAILED"
    exit 1
  fi
  PID="$(cat "$PID_FILE" 2>/dev/null)"
  if ! isPIDRunning "$PID"; then
    echo "FAILED"
    exit 1
  fi

  n=$((n+1))
done

# Timed out waiting for ready line
echo "(pid $(cat "$PID_FILE" 2>/dev/null) - WARNING: SelfTest not yet finished)"
exit 0