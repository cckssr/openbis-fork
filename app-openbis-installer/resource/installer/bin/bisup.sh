#!/bin/bash
# Starts up openBIS server

#
# Return the age of a file in seconds.
#
# parameter $1: a file name
#
#!/bin/bash
# Starts up AFS

BASE=`dirname "$scriptName"`
if [ ${BASE#/} == ${BASE} ]; then
    BASE="`pwd`/${BASE}"
fi

JETTY_HOME=$BASE/../servers/openBIS-server/jetty
OPENBIS_LOG=$JETTY_HOME/logs/openbis.log

if [ -n "$(readlink $0)" ]; then
   # handle symbolic links
   scriptName=$(readlink $0)
   if [[ "$scriptName" != /* ]]; then
      scriptName=$(dirname $0)/$scriptName
   fi
else
    scriptName=$0
fi

"$JETTY_HOME/bin/startup.sh"
