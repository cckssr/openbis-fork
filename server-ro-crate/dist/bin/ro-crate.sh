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

isPIDRunning()
{
  if [ "$1" = "" ]; then
    return 1
  fi
  if [ "$1" = "fake" ]; then # for unit tests
    return 0
  fi
  # This will have a return value of 0 on BSDish systems
  isBSD="`ps aux > /dev/null 2>&1; echo $?`"
  AWK=`awkBin`
  if [ "$isBSD" = "0" ]; then
    if [ "`ps aux | $AWK -v PID=$1 '{if ($2==PID) {print "FOUND"}}'`" = "FOUND" ]; then
      return 0
    else
      return 1
    fi
  else
    if [ "`ps -ef | $AWK -v PID=$1 '{if ($2==PID) {print "FOUND"}}'`" = "FOUND" ]; then
      return 0
    else
      return 1
    fi
  fi
}

checkNotRoot()
{
  if [ $UID -eq 0 ]; then
    echo "openBIS RO-CRATE Server cannot run as user 'root'." > /dev/stderr
    exit 1
  fi
}


BASE=$(dirname "$0")/..
PIDFILE=$BASE/ro_crate_server.pid
QUARKUS_RUN_JAR=$BASE/quarkus-app/quarkus-run.jar
SERVICE_PROPERTIES_FILE=$BASE/etc/service.properties
#LOG_FOLDER=$BASE/log
#LOG_FILE=$LOG_FOLDER/ro_crate.log
SUCCESS_MSG="Quarkus app running"
STARTED_MARKER="$RO_CRATE_HOME/SERVER_STARTED"
TIMEOUT=120

start(){
  checkNotRoot
  if [ -f "$PIDFILE" ] && kill -0 $(cat "$PIDFILE") 2>/dev/null; then
    echo "Already running."
    exit 1
  fi

  #mkdir -p $LOG_FOLDER

  # cd to working directory
  cd "$RO_CRATE_HOME"

  stdbuf -oL -eL java -jar "$QUARKUS_RUN_JAR" "$SERVICE_PROPERTIES_FILE" "$@" 2>&1 &
  echo $! >"$PIDFILE"
  JAVA_PID=$!
  echo "Starting RO-CRATE server (pid $(cat "$PIDFILE"))"

  echo "STARTED_MARKER: $STARTED_MARKER"

  # Poll for marker file
  for i in $(seq 1 $TIMEOUT); do
      sleep 1

      [ -f "$STARTED_MARKER" ] && { echo "Started RO-CRATE server."; exit 0; }

      isPIDRunning "$JAVA_PID" || { echo "Startup of RO-CRATE server failed."; exit 1; }
  done
}

stop(){
  if [ -f "$PIDFILE" ]; then
    kill $(cat "$PIDFILE")
    rm "$PIDFILE"
    echo "Stopped RO-CRATE server."
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
