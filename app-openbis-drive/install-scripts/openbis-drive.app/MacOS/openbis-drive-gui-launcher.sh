#!/bin/sh
if [ -z "$OPENBIS_DRIVE_DIR" ]
then
    cd "$HOME" ; cd Library ; cd "Application Support" ; cd openbis-drive ; cd launch-scripts
else
    cd "$OPENBIS_DRIVE_DIR" ; cd launch-scripts
fi
nohup /bin/sh openbis-drive-gui.sh >/dev/null 2>&1 &