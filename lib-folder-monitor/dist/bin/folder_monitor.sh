#!/bin/bash
#
# Control script for Folder Monitor on Unix / Linux systems
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
    echo "Folder Monitor cannot run as user 'root'." > /dev/stderr
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
      echo "Folder Monitor is running (pid $PID)"
      return 0
    else
      echo "Folder Monitor is dead (stale pid $PID)"
      return 1
    fi
  else
    echo "Folder Monitor is not running."
    return 2
  fi
}

#
# definitions
#

PID_FILE=./folder_monitor.pid
LIB_FOLDER=./lib
CONF_FILE=./etc/folder_monitor.conf
SERVICE_PROPERTIES_FILE=./etc/service.properties
LOG_FOLDER=./log
LOG_FILE=$LOG_FOLDER/folder_monitor.log
STARTUP_LOG=$LOG_FOLDER/folder_monitor.log
SUCCESS_MSG="=== STARTED ==="
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
JAVA_OPTIONS="${JAVA_OPTS} ${JAVA_MEM_OPTS} --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED -classpath $JAVA_CLASS_PATH ch.ethz.sis.foldermonitor.FolderMonitor $SERVICE_PROPERTIES_FILE"

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
      echo "Cannot start Folder Monitor: already running."
      exit 100
    fi

    echo -n "Starting Folder Monitor "

    shift 1
    $JAVA_BIN $JAVA_OPTIONS "$@" > $STARTUP_LOG 2>&1 & echo $! > $PID_FILE
    if [ $? -eq 0 ]; then
      # wait for initial self-test to finish
      n=0
      while [ $n -lt $MAX_LOOPS ]; do
        sleep 1
        if [ ! -f $PID_FILE ]; then
          break
        fi
        if [ -s $STARTUP_LOG ]; then
          PID=`cat $PID_FILE 2> /dev/null`
          isPIDRunning $PID
          if [ $? -ne 0 ]; then
            break
          fi
        fi
        grep "$SUCCESS_MSG" $LOG_FILE > /dev/null 2>&1
        if [ $? -eq 0 ]; then
          break
        fi
        n=$(($n+1))
      done
      PID=`cat $PID_FILE 2> /dev/null`
      isPIDRunning $PID
      if [ $? -eq 0 ]; then
        grep "$SUCCESS_MSG" $LOG_FILE > /dev/null 2>&1
        if [ $? -ne 0 ]; then
          echo "(pid $PID - WARNING: SelfTest not yet finished)"
        else
          echo "(pid $PID)"
        fi
      else
        echo "FAILED"
        if [ -s $STARTUP_LOG ]; then
          echo "startup log says:"
          cat $STARTUP_LOG
        else
          echo "log file says:"
          tail $LOG_FILE
        fi
      fi
    else
      echo "FAILED"
    fi
    ;;
  stop)
    echo -n "Stopping Folder Monitor "
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
