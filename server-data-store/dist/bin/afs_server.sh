#!/bin/bash
#
# Control script for openBIS AFS Server on Unix / Linux systems
# -------------------------------------------------------------------------

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
    echo "openBIS AFS Server cannot run as user 'root'." > /dev/stderr
    exit 1
  fi
}

getStatus()
{
  if [ -f $PID_FILE ]; then
    PID=`cat $PID_FILE`
    isPIDRunning $PID
    if [ $? -eq 0 ]; then
      return 0
    else
      return 1
    fi
  else
    return 2
  fi
}

printStatus()
{
  if [ -f $PID_FILE ]; then
    PID=`cat $PID_FILE`
    isPIDRunning $PID
    if [ $? -eq 0 ]; then
      echo "AFS Server is running (pid $PID)"
      return 0
    else
      echo "AFS Server is dead (stale pid $PID)"
      return 1
    fi
  else
    echo "AFS Server is not running."
    return 2
  fi
}

#
# definitions
#


PID_FILE=./afs_server.pid
LIB_FOLDER=./lib
CONF_FILE=./etc/afs_server.conf
SERVICE_PROPERTIES_FILE=./etc/service.properties
LOG_FOLDER=./log
LOG_FILE=$LOG_FOLDER/afs.log
SUCCESS_MSG="=== Server ready ==="
MAX_LOOPS=20

#
# change to installation directory
#

bin=$0
if [ -L $bin ]; then
  bin=`dirname $bin`/`readlink $bin`
fi
WD=`dirname $bin`/../
cd $WD
SCRIPT=./bin/`basename $0`

#
# create log directory
#

mkdir -p $LOG_FOLDER

#
# source configuration script
#

test -f $CONF_FILE && source $CONF_FILE

if [ "$JAVA_HOME" != "" ]; then
  JAVA_BIN="$JAVA_HOME/bin/java"
else
  JAVA_BIN="java"
fi

JAVA_CLASS_PATH=`echo $LIB_FOLDER/*.jar | sed 's/ /:/g'`
JAVA_OPTIONS="${JAVA_OPTS} ${JAVA_MEM_OPTS} -Dio.netty.tryReflectionSetAccessible=true -Djava.awt.headless=true --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED -classpath $JAVA_CLASS_PATH ch.ethz.sis.afsserver.startup.Main $SERVICE_PROPERTIES_FILE"

#
# ensure that we ignore a possible prefix "--" for any command
#

command=$1
command="${command#--*}"

case "$command" in
  start)
    checkNotRoot
    getStatus
    EXIT_STATUS=$?
    if [ $EXIT_STATUS -eq 0 ]; then
      echo "Cannot start AFS Server: already running."
      exit 100
    fi

    echo -n "Starting AFS Server "

    shift 1

    # Flag file toggled when SUCCESS_MSG appears on stdout
    READY_FLAG=$(mktemp -t afs_ready.XXXXXX)
    trap 'rm -f "$READY_FLAG"' EXIT

    AWK_BIN=$(awkBin)

    # Launch server in a subshell so we can capture the actual Java PID and keep the pipe open
    (
      # Start Java (line-buffered) in background
      stdbuf -oL -eL $JAVA_BIN $JAVA_OPTIONS "$@" 2>&1 &
      CHILD=$!
      echo $CHILD > "$PID_FILE"
      # Wait for the Java process so the pipe stays open for tee
      wait $CHILD
    ) | tee >( "$AWK_BIN" -v s="$SUCCESS_MSG" -v ready="$READY_FLAG" '
          index($0, s) { system("touch " ready); exit }
        ' > /dev/null ) &
    PIPE_PID=$!

    # wait for initial self-test to finish (by success message or process exit)
    n=0
    while [ $n -lt $MAX_LOOPS ]; do
      sleep 1

      # If pid file vanished, assume process terminated
      if [ ! -f "$PID_FILE" ]; then
        break
      fi

      PID=`cat "$PID_FILE" 2> /dev/null`
      isPIDRunning "$PID"
      if [ $? -ne 0 ]; then
        break
      fi

      # Success line seen?
      if [ -f "$READY_FLAG" ]; then
        break
      fi

      n=$((n+1))
    done

    # Final status check
    if [ -f "$PID_FILE" ]; then
      PID=`cat "$PID_FILE" 2> /dev/null`
    else
      PID=""
    fi

    isPIDRunning "$PID"
    if [ $? -eq 0 ]; then
      if [ -f "$READY_FLAG" ]; then
        echo "(pid $PID)"
      else
        echo "(pid $PID - WARNING: SelfTest not yet finished)"
      fi
    else
      echo "FAILED"

      # Only consult the regular log file if it exists
      if [ -f "$LOG_FILE" ]; then
        echo
        echo "log file ($LOG_FILE) says (recent):"
        tail -n 400 "$LOG_FILE"
      fi
    fi
    ;;
  stop)
    echo -n "Stopping AFS Server "
    if [ -f $PID_FILE ]; then
      PID=`cat $PID_FILE 2> /dev/null`
      isPIDRunning $PID
      if [ $? -eq 0 ]; then
        kill $PID
        n=0
        while [ $n -lt $MAX_LOOPS ]; do
          isPIDRunning $PID
          if [ $? -ne 0 ]; then
            break
          fi
          sleep 1
          n=$(($n+1))
        done
        isPIDRunning $PID
        if [ $? -ne 0 ]; then
          echo "(pid $PID)"
          test -f $PID_FILE && rm $PID_FILE 2> /dev/null
        else
          echo "FAILED"
        fi
      else
        if [ -f $PID_FILE ]; then
          rm $PID_FILE 2> /dev/null
          echo "(was dead - cleaned up pid file)"
        fi
      fi
    else
      echo "(not running - nothing to do)"
    fi
    ;;
  restart)
    $SCRIPT stop
    $SCRIPT start
    ;;
  status)
    printStatus
    EXIT_STATUS=$?
    exit $EXIT_STATUS
    ;;
  *)
    echo "Usage: $0 {start|stop|restart|status}"
    exit 200
    ;;
esac
exit 0
