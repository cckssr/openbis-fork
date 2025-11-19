#!/bin/bash

BASE=$(dirname "$0")

INSTALLATION_FOLDER=$BASE/../../targets/ro-crate

# stop server
if [[ -f $INSTALLATION_FOLDER/server-ro-crate/bin/ro-crate.sh ]]; then
  $INSTALLATION_FOLDER/server-ro-crate/bin/ro-crate.sh stop
fi
