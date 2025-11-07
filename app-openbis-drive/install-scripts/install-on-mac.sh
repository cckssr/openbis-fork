#!/bin/sh

BASEDIR=`dirname $0`

if [ ! -d "$BASEDIR"/launch-scripts ] ; then
    read -r -p "ERROR: Installation files not found. Launch this script as: '/bin/sh this-script-file' from the release-package directory" ko
    exit 1
fi

createDesktopLink() {
    chmod +x "$BASEDIR"/openbis-drive.app/Contents/MacOS/openbis-drive-gui-launcher.sh

    read -r -p "

You can copy openbis-drive.app to your desktop to use it as a launch-icon for the graphical interface

"
}

read -r -p "Are you sure you want to install openBIS Drive application under $HOME/Library/\"Application Support\"/openbis-drive/? (y/[N]) " response
case "$response" in
    [yY][eE][sS]|[yY])
        mkdir -p $HOME/Library/"Application Support"/openbis-drive/
        cp -r $BASEDIR/launch-scripts $HOME/Library/"Application Support"/openbis-drive/
        echo "Application files copied to $HOME/Library/\"Application Support\"/openbis-drive"
        createDesktopLink
        ;;
    *)
        ;;
esac

read -r -p "Finish" exit
