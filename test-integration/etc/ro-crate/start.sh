#!/bin/bash

BASE=$(dirname "$0")

SERVER_RO_CRATE_FOLDER=$BASE/../../../server-ro-crate
TEST_INTEGRATION_FOLDER=$BASE/../../
INSTALLATION_FOLDER=$TEST_INTEGRATION_FOLDER/targets/ro-crate

# stop running server
if [[ -f $INSTALLATION_FOLDER/server-ro-crate/bin/ro-crate.sh ]]; then
  $INSTALLATION_FOLDER/server-ro-crate/bin/ro-crate.sh stop
fi

# prepare zip
cd $SERVER_RO_CRATE_FOLDER
./gradlew RoCrateServerZip

# install server
rm -r $INSTALLATION_FOLDER
mkdir -p $INSTALLATION_FOLDER
unzip $SERVER_RO_CRATE_FOLDER/build/distributions/server-ro-crate.zip -d $INSTALLATION_FOLDER

# copy configuration
cp $1 $INSTALLATION_FOLDER/server-ro-crate/etc/service.properties

# start server
$INSTALLATION_FOLDER/server-ro-crate/bin/ro-crate.sh start
