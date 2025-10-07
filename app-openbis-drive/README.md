# App openBIS-Drive

This application can keep local directories synchronized with AFS-server directories.

## Installation

After building the following Gradle tasks:

app-openbis-drive-service-jar
app-openbis-drive-cmd-line-jar

copy the content of build/launch-scripts under:

for Linux: `$HOME/.local/state/openbis-drive/launch-scripts`

for Windows: `%USERPROFILE%\AppData\Local\openbis-drive\launch-scripts`

for MAC-OS: `$HOME/Library/"Application Support"/openbis-drive/launch-scripts`

create the directory hierarchy if necessary.

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