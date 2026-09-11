#!/bin/bash

BASE=$(dirname "$0")
BASE=$(cd "$BASE"; pwd)

INSTALLATION_FOLDER=$BASE/../../../targets/server-sftp

export SFTP_HOME="$INSTALLATION_FOLDER"/server-sftp

# stop server
if [[ -f $INSTALLATION_FOLDER/server-sftp/bin/sftp.sh ]]; then
  $INSTALLATION_FOLDER/server-sftp/bin/sftp.sh stop
fi
