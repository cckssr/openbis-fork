#!/bin/sh

deleteConfigurationDirectory() {
    read -r -p "Do you want to remove openBIS Drive configuration from $HOME/Library/\"Application Support\"/openbis-drive/state? (y/[N]) " response
    case "$response" in
        [yY][eE][sS]|[yY])
            rm -r $HOME/Library/"Application Support"/openbis-drive/state
            if [ -d $HOME/Library/"Application Support"/openbis-drive/state ] ; then
                echo "ERROR deleting $HOME/Library/\"Application Support\"/openbis-drive/state"
            fi
            ;;
        *)
            ;;
    esac
}

read -r -p "Are you sure you want to remove openBIS Drive application from $HOME/Library/\"Application Support\"/openbis-drive/? (y/[N]) " response
case "$response" in
    [yY][eE][sS]|[yY])
        pkill -SIGKILL -f 'java -cp app-openbis-drive-full\.jar ch\.openbis\.drive\.DriveAPIService'
        rm -r $HOME/Library/"Application Support"/openbis-drive/launch-scripts
        if [ -d $HOME/Library/"Application Support"/openbis-drive/launch-scripts ] ; then
            echo "ERROR deleting $HOME/Library/\"Application Support\"/openbis-drive/launch-scripts"
        fi
        ;;
    *)
        ;;
esac

deleteConfigurationDirectory

read -r -p "Finish" exit
