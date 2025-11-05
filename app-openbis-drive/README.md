# App openBIS-Drive

This application can keep local directories synchronized with AFS-server directories.

## Installation

After building the Gradle task: "release"

extract the content of build/distributions/app-openbis-drive-$VERSION.tar.gz and copy it all under:
(equivalently, copy the content of build/release under:)

for Linux: `$HOME/.local/state/openbis-drive/`

for Windows: `%USERPROFILE%\AppData\Local\openbis-drive\`

for MAC-OS: `$HOME/Library/"Application Support"/openbis-drive/`

create the directory hierarchy if necessary.
At the end, you should find the following subdirectory:

`.../openbis-drive/launch-scripts`

## Configuration and state files

Configuration and state files will be stored by the running application under:

for Linux: `$HOME/.local/state/openbis-drive/state`

for Windows: `%USERPROFILE%\AppData\Local\openbis-drive\state`

for MAC-OS: `$HOME/Library/"Application Support"/openbis-drive/state`

## Application launch and stop (Linux and MAC OS)

The background-process can be started by entering into the launch-scripts directory 
(or by adding this to the PATH environment variable) and:
- by using the command-line-application start command: `./openbis-drive-cmd-line.sh start`
  (`openbis-drive-cmd-line.sh start` without initial dot if the launch-scripts directory is added to the PATH)
- directly with the start-script: `./openbis-drive-service-start.sh` 
  (`openbis-drive-service-start.sh start` without initial dot if the launch-scripts directory is added to the PATH)

Check running status with: `openbis-drive-cmd-line.sh status`

Stop:
- by using the command-line-application stop command: `./openbis-drive-cmd-line.sh stop`
  (`openbis-drive-cmd-line.sh stop` without initial dot if the launch-scripts directory is added to the PATH)
- directly with the stop-script: `./openbis-drive-service-stop.sh`
  (`openbis-drive-service-stop.sh start` without initial dot if the launch-scripts directory is added to the PATH)

## Application launch and stop (Windows)

The background-process can be started by entering into the launch-scripts directory
(or by adding this to the PATH environment variable) and:
- by using the command-line-application start command: `openbis-drive-cmd-line.bat start`
- directly with the start-script: `openbis-drive-service-start.bat`

Check running status with: `openbis-drive-cmd-line.bat status`

Stop:
- by using the command-line-application stop command: `openbis-drive-cmd-line.bat stop`
- directly with the stop-script: `openbis-drive-service-stop.bat`

## Command-line (Linux and MAC OS)

The command-line can be invoked by entering into the launch-scripts directory
(or by adding this to the PATH environment variable) and typing:
- `./openbis-drive-cmd-line.sh help`
  (`openbis-drive-cmd-line.sh help` without initial dot if the launch-scripts directory is added to the PATH)

## Command-line (Windows)

The command-line can be invoked by entering into the launch-scripts directory
(or by adding this to the PATH environment variable) and typing:
- `openbis-drive-cmd-line.bat help`

## Graphical user-interface (Linux and MAC OS)

The graphical user-interface can be started by entering into the launch-scripts directory
(or by adding this to the PATH environment variable) and typing:
- `./openbis-drive-gui.sh`
  (`openbis-drive-gui.sh` without initial dot if the launch-scripts directory is added to the PATH)

## Graphical user-interface (Windows)

The graphical user-interface can be started by entering into the launch-scripts directory
(or by adding this to the PATH environment variable) and typing:
- `openbis-drive-gui.bat`

