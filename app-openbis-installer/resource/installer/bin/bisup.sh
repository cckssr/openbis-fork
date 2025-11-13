#!/bin/bash
# Starts up openBIS server

#
# Return the age of a file in seconds.
#
# parameter $1: a file name
#

STARTING_MESSAGE="STARTING SERVER"
STARTED_MESSAGE="SERVER STARTED"
ERROR_MESSAGE="ERROR"


if [ -n "$(readlink $0)" ]; then
   # handle symbolic links
   scriptName=$(readlink $0)
   if [[ "$scriptName" != /* ]]; then
      scriptName=$(dirname $0)/$scriptName
   fi
else
    scriptName=$0
fi

BASE=`dirname "$scriptName"`
if [ ${BASE#/} == ${BASE} ]; then
    BASE="`pwd`/${BASE}"
fi


JETTY_HOME=$BASE/../servers/openBIS-server/jetty

echo Starting openBIS...
rm -f $STARTED_MARKER

$JETTY_HOME/bin/startup.sh


