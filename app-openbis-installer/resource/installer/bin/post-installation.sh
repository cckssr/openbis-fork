#!/bin/bash
BASE=`dirname "$0"`
if [ ${BASE#/} == ${BASE} ]; then
    BASE="`pwd`/${BASE}"
fi

source $BASE/common-functions.sh

executeScriptHooks "Executing post installation script " "$BASE/post-install/*.sh"

# Trying to delete all leftovers from the installer
rm -rf "$BASE/post-install"
rm "$BASE/chmodx-all-scripts.sh"
rm "$BASE/common-functions.sh"
rm "$BASE/finish-installation.sh"
rm "$BASE/InstallerVariableAccess.class"
rm "$BASE/postgres_bin_path.txt"
rm "$BASE/post-installation.sh"