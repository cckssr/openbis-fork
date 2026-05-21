#!/bin/bash

BASE=$(dirname "$0")
BASE=$(cd "$BASE"; pwd)

SERVER_AFS_SFTP_FOLDER=$BASE/../../../../server-sftp
TEST_INTEGRATION_FOLDER=$BASE/../../..
INSTALLATION_FOLDER=$TEST_INTEGRATION_FOLDER/targets/server-sftp

export AFS_SFTP_HOME="$INSTALLATION_FOLDER"/server-sftp

# stop running server
if [[ -f $INSTALLATION_FOLDER/server-sftp/bin/afs_sftp.sh ]]; then
  $INSTALLATION_FOLDER/server-sftp/bin/afs_sftp.sh stop
fi

# prepare zip
cd $SERVER_AFS_SFTP_FOLDER
./gradlew AfsSftpServerZip

# install server
rm -r $INSTALLATION_FOLDER
mkdir -p $INSTALLATION_FOLDER
unzip $SERVER_AFS_SFTP_FOLDER/build/distributions/server-sftp.zip -d $INSTALLATION_FOLDER

# copy configuration
cp $1 $INSTALLATION_FOLDER/server-sftp/etc/service.properties

# start server

$INSTALLATION_FOLDER/server-sftp/bin/afs_sftp.sh start
