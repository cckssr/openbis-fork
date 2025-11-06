#!/bin/sh

BASEDIR=`dirname $0`

if [ ! -d "$BASEDIR"/launch-scripts ] ; then
    read -r -p "ERROR: Installation files not found. Launch this script as: '/bin/sh this-script-file' from the release-package directory" ko
    exit 1
fi

createDesktopLink() {
    echo "[Desktop Entry]
Name=openBIS Drive
Exec=$HOME/.local/state/openbis-drive/launch-scripts/openbis-drive-gui.sh
Icon=$HOME/.local/state/openbis-drive/launch-scripts/openbis-drive-icon-small.png
Type=Application
Comment=openBIS synchronization tool
Categories=Utility" > $BASEDIR/openbis-drive.desktop

    echo "You can copy openbis-drive.desktop to your desktop to use it as a launch-icon for the graphical interface"
    echo "(Right click -> Allow launching : might be necessary)"
}

read -r -p "Are you sure you want to install openBIS Drive application under $HOME/.local/state/openbis-drive/? (y/[N]) " response
case "$response" in
    [yY][eE][sS]|[yY])
        mkdir -p $HOME/.local/state/openbis-drive/
        cp -r $BASEDIR/launch-scripts $HOME/.local/state/openbis-drive/
        echo "Application files copied to $HOME/.local/state/openbis-drive"
        createDesktopLink
        ;;
    *)
        ;;
esac

read -r -p "Finish" exit
