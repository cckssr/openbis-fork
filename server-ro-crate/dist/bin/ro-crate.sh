#!/bin/bash
#
#  Copyright ETH 2025 Zürich, Scientific IT Services
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#
#

BASE=$(dirname "$0")/..
PIDFILE=$BASE/ro_crate_server.pid
JAR=$BASE/lib/server-ro-crate.jar
SERVICE_PROPERTIES_FILE=$BASE/etc/service.properties
LOG_FOLDER=$BASE/log
LOG_FILE=$LOG_FOLDER/ro_crate.log
SUCCESS_MSG="Server started"

start(){
  if [ -f "$PIDFILE" ] && kill -0 $(cat "$PIDFILE") 2>/dev/null; then
    echo "Already running."
    exit 1
  fi
  mkdir -p $LOG_FOLDER
  CLASSPATH="$BASE/lib/*"
  MAIN_CLASS="ch.ethz.sis.rocrateserver.startup.StartupMain"
  java -cp "$CLASSPATH" $MAIN_CLASS $SERVICE_PROPERTIES_FILE "$@" > $LOG_FILE 2>&1 &
  echo $! >"$PIDFILE"
  echo "Started, pid: $(cat "$PIDFILE")"

  # Now tail the log in the foreground
  echo "Tailing log ..."
  tail -n0 -F "$LOG_FILE" | while IFS= read -r line; do
    echo "$line"
    # check for your success marker
    if [[ "$line" == *"$SUCCESS_MSG"* ]]; then
      echo "Server ready detected – stopping log tail."
      break
    fi
    # check for any ERROR or Exception
    if echo "$line" | grep -q -E 'ERROR|Exception'; then
      echo "Error detected – stopping log tail."
      break
    fi
  done
}

stop(){
  if [ -f "$PIDFILE" ]; then
    kill $(cat "$PIDFILE")
    rm "$PIDFILE"
    echo "Stopped."
  else
    echo "Not running."
  fi
}

case "$1" in
  start) start "${@:2}" ;;
  stop)  stop ;;
  status)
    if [ -f "$PIDFILE" ] && kill -0 $(cat "$PIDFILE") 2>/dev/null; then
      echo "Running (pid $(cat $PIDFILE))"
    else
      echo "Not running."
    fi ;;
  *)
    echo "Usage: $0 {start|stop|status}"
    exit 1 ;;
esac
