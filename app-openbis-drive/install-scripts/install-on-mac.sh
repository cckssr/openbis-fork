#!/bin/sh

createDesktopLink() {
    read -r -p "

You can copy openbis-drive.app to your desktop to use it as a launch-icon for the graphical interface

"
}

read -r -p "Are you sure you want to install openBIS Drive application under $HOME/Library/\"Application Support\"/openbis-drive/? (y/[N]) " response
case "$response" in
    [yY][eE][sS]|[yY])
        mkdir -p $HOME/Library/"Application Support"/openbis-drive/
        cp -r launch-scripts $HOME/Library/"Application Support"/openbis-drive/
        echo "Application files copied to $HOME/Library/\"Application Support\"/openbis-drive"
        createDesktopLink
        ;;
    *)
        ;;
esac

read -r -p "Finish" exit
