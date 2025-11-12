# openBIS Drive

## Introduction

This application is a synchronization tool which can keep synchronization 
between local directories and openBIS AFS-directories:
either unidirectionally (upload or download only) or bidirectionally.
It consists of:
- a background-process, which runs on the local system and:
  - performs synchronization-tasks according to configuration
  - keeps track of synchronization (download, upload, deletion...) events
  - raises and stores notifications for specific attention-worthy situations
- a command-line client, which can talk on a local TCP connection with the background-process and:
  - start it or stop it
  - configure it for: interval between synchronization checks, language, start-at-login...
  - configure the registered synchronization-tasks: that is to say, 
which local directories have to be kept up-to-date with which remote directories 
in which mode (upload, download, bidirectional)
  - read the log of synchronization events
  - read notifications
- a graphical (window-based) interface, which essentially serves the same purpose of the command-line client
(note: this graphical interface will always ask you for confirmation to start the background-process, 
if that is not running)

Note: none of the aforementioned processes require root-user permissions.

## Installation

### System requirements

- Java 17 (or more recent)
- for Linux and MAC-OS: check that `pkill` utility is available in terminal 
(it should be preinstalled and already available in all recent distributions)

### Preparation

Extract the installation-package `app-openbis-drive-{VERSION}.tar.gz` 
(at the moment of writing this documentation: `app-openbis-drive-0.0.1.tar.gz`)
to a suitable working-space local directory: 
it can be the desktop or the home directory for example.
In the uncompressed directory, you will find:
- a README.txt (which will give the same information as here, possibly beside some more specific details)
- a `launch-scripts` folder: this is the actual content which will be installed (simply copied) to a different
position according to the operating-system (this position will always be within those 
under the permission-scope of the current user)
- a series of `install-on-***` scripts, one for each supported platform, 
which will guide you through the installation process
- a series of `uninstall-on-***` scripts, to be kept for possible deinstallation need
- some other files, which can be useful for getting a launch-icon for the graphical interface
(which one and their specific use depends on the platform)

### Automatic installation (through script)

To install the application, choose the correct script for your platform:

#### Linux 
```
install-on-linux.sh
```
Either right-click and launch this as program or open a terminal in the containing directory and
```shell
sh install-on-linux.sh
```
You will be prompted for confirmation to copy the entire `launch-scripts` directory onto:
```
$HOME/.local/state/openbis-drive/launch-scripts
```
The prompt should then indicate a file (`openbis-drive.desktop`) which you can copy
from the uncompressed distribution directory to the desktop as a launch-icon
for the graphical interface.

#### Windows
```
install-on-windows.bat
```
Either right-click and launch this as program or open a CMD terminal in the containing folder and
```shell
install-on-windows.bat
```
You will be prompted for confirmation to copy the entire `launch-scripts` folder onto:
```
%USERPROFILE%\AppData\Local\openbis-drive\launch-scripts
```
The prompt should then indicate a file (`openbis-drive.lnk`) which you can copy
from the uncompressed distribution folder to the desktop as a launch-icon
for the graphical interface.

#### Mac
```
install-on-mac.sh
```
Open a terminal in the containing directory and
```shell
sh install-on-mac.sh
```
You will be prompted for confirmation to copy the entire `launch-scripts` directory onto:
```
$HOME/Library/"Application Support"/openbis-drive/launch-scripts
```
The prompt should then indicate a file (`openbis-drive.app`) which you can copy
from the uncompressed distribution directory to the desktop as a launch-icon
for the graphical interface.

### Manual installation

If for any reason the installation by script does not work, you can simply copy the `launch-scripts` directory
to the appropriate location according to platform: see the points above.
(Always check the README in the uncompressed distribution directory to see if other actions are needed)

### Application data directory

According to the platform, application executable files, configuration and data will be stored in:
- Linux
```
$HOME/.local/state/openbis-drive/launch-scripts   :   executable files
$HOME/.local/state/openbis-drive/state            :   configuration and data
```
- Windows
```
%USERPROFILE%\AppData\Local\openbis-drive\launch-scripts   :   executable files
%USERPROFILE%\AppData\Local\openbis-drive\state            :   configuration and data
```
- Mac
```
$HOME/Library/"Application Support"/openbis-drive/launch-scripts   :   executable files
$HOME/Library/"Application Support"/openbis-drive/state            :   configuration and data
```

When installing a new version, normally only the `launch-scripts` will be overwritten.
When deinstalling, the possibility is given to remove both directories `launch-scripts` and `state`
or only one of them: if, for example, one wishes to maintain the current configuration for
future installations.

## Command line client

To use the command-line tool, open a terminal and move the working directory (`cd`) to the appropriate `launch-scripts` location:
- Linux
```shell
cd $HOME/.local/state/openbis-drive/launch-scripts
```
- Windows CMD
```shell
cd %USERPROFILE%\AppData\Local\openbis-drive\launch-scripts
```
- Mac
```shell
cd $HOME/Library/"Application Support"/openbis-drive/launch-scripts
```

Then use it as in the following examples:
- on Linux and MAC-OS
```
./openbis-drive-cmd-line.sh help
./openbis-drive-cmd-line.sh status
./openbis-drive-cmd-line.sh ...other-command...
```
- on Windows
```
openbis-drive-cmd-line.bat help
openbis-drive-cmd-line.bat status
openbis-drive-cmd-line.bat ...other-command...
```

(To avoid the step of moving to the `launch-scripts` directory, you can add its specific path to
the `PATH` environment variable for your operating system: this is done differently on different
platforms. Please, check the documentation of your operating system.
At that point, also on MAC and Linux you will skip the initial dot when launching the command-line:
`openbis-drive-cmd-line.sh` instead of `./openbis-drive-cmd-line.sh` )

### Basic commands

(Note: from here on, `./openbis-drive-cmd-line.sh` will be used.
Replace this with `openbis-drive-cmd-line.bat` on Windows and 
with `openbis-drive-cmd-line.sh` on Linux and MAC-OS with adapted `PATH` variable)

#### help

```shell
./openbis-drive-cmd-line.sh help
```

Returns a short helping guide for the command-line tool itself:

```
Use 'help' command to print this message.
Supported commands:
    start   -> starts the background service
    stop    -> stops the background service
    status  -> prints the status of the background service

    config  -> prints the configuration with which the background service is running
    config -startAtLogin=true|false -language=en|fr|de|it|es -syncInterval=120   -> sets configuration parameters: two-letter ISO-code for language, synchronization-interval in seconds (defaults: false, 'en', 120 seconds = 2 minutes)

    jobs    -> prints the currently registered synchronization-jobs
    jobs add -type='Bidirectional|Upload|Download' -dir='./dir-a/dir-b' -openBISurl='https://...' -entityPermId='123-abc-...' -personalAccessToken='098abc...' -remDir='/remote/dir/absolute-path/' -enabled=true|false
    jobs remove -dir='./dir-a/dir-b'
    jobs start -dir='./dir-a/dir-b'
    jobs stop -dir='./dir-a/dir-b'

    notifications -limit=100   (default: 100)  -> prints the last limit-number of notifications
    events -limit=100   (default: 100)  -> prints the last limit-number of events
```

#### status

```shell
./openbis-drive-cmd-line.sh help
```

Reports if the background-service is running and, if so, 
a summary of its configuration and the registered synchronization-tasks:

```
OpenBIS Drive Service is running with this configuration:
Start-at-login: false
Language: en
Sync-interval: 60 seconds
Synchronization-jobs: 
----------
Type: Bidirectional
Local directory: /home/myuser/openbis_sync_test
openBIS url: http://localhost:8085/afs-server
Entity-perm-id: a7bc2fbd-49af-4e2d-86cc-ea316028b793
Remote directory: /remotedir
Personal access token: a13fe879-1753-41dd-8c3e-eb5a97e1c7be
Enabled: false
```

If the background-service is not running:

```
OpenBIS Drive Service is not running.
```

#### start

```shell
./openbis-drive-cmd-line.sh start
```

Starts the background process.

#### stop

```shell
./openbis-drive-cmd-line.sh stop
```

Stops the background process.

### Config commands

#### config

```shell
./openbis-drive-cmd-line.sh config
```

Prints the current configuration of the background-process:

```
Start-at-login: false
Language: en
Sync-interval: 60 seconds
Synchronization-jobs: 
----------
Type: Bidirectional
Local directory: /home/myuser/openbis_sync_test
openBIS url: http://localhost:8085/afs-server
Entity-perm-id: a7bc2fbd-49af-4e2d-86cc-ea316028b793
Remote directory: /remotedir
Personal access token: a13fe879-1753-41dd-8c3e-eb5a97e1c7be
Enabled: false
```

- Start-at-login: indicates if the background-process has to be started at system-login
- Language: one of en (English), de (German), fr (French), it (Italian), es (Spanish):
it affects only the graphical interface
- Synchronization-interval: every which number of seconds the synchronization checks have to run
- At the end: a list of the registered synchronization-tasks

Use the `config` command with options to modify the configuration 
(not necessarily all options have to be specified):
```shell
./openbis-drive-cmd-config -startAtLogin=true -language=fr -syncInterval=60
```
```
Start-at-login: true
Language: fr
Sync-interval: 60 seconds
Synchronization-jobs: 
----------
Type: Bidirectional
Local directory: /home/myuser/openbis_sync_test
openBIS url: http://localhost:8085/afs-server
Entity-perm-id: a7bc2fbd-49af-4e2d-86cc-ea316028b793
Remote directory: /remotedir
Personal access token: a13fe879-1753-41dd-8c3e-eb5a97e1c7be
Enabled: false
```

### Jobs commands

#### jobs

```shell
./openbis-drive-cmd-line.sh jobs
```

Prints the synchronization-tasks registered in the background-service:

```
Synchronization-jobs: 
----------
Type: Bidirectional
Local directory: /home/myuser/openbis_sync_test
openBIS url: http://localhost:8085/afs-server
Entity-perm-id: a7bc2fbd-49af-4e2d-86cc-ea316028b793
Remote directory: /remotedir
Personal access token: a13fe879-1753-41dd-8c3e-eb5a97e1c7be
Enabled: false
```

Each synchronization-task consists of:
- type: upload, download or bidirectional
- local directory: the local directory to be kept in synchronization
  (NOTE: local directories of different synchronization-tasks can never overlap)
- openBIS url: http or https URL of the openBIS server
- entity ID: interested entity ID on the openBIS server
- remote directory: remote directory withing the entity ID on the specified openBIS server 
to be kept in synchronization
- personal access token: user credential to get access to the openBIS server
- indication of enabled or disabled state

#### jobs add

When adding a new synchronization-task, all of its properties must be specified:

```
jobs add -type='Bidirectional|Upload|Download' -dir='./my-local-dir' -openBISurl='https://...' -entityPermId='123-abc-...' -personalAccessToken='098abc...' -remDir='/remote/dir/absolute-path/' -enabled=true|false
```

Option that represent properties are:
```
-type   :   one of Bidirectional, Upload, Download
-dir   :   local directory
-openBISurl   :   openBIS server URL (example: 'http://localhost:8080')
-entityPermId   :   entity ID on openBIS server
-remDir   :   absolute path of the remote directory on openBIS server within the entity ID
-personalAccessToken   :   personal access token
-enabled   :   true or false
```

For example: 

```shell
./openbis-drive-cmd-line.sh jobs add -type='Bidirectional' -dir='/home/myuser/openbis_sync_test' -openBISurl='http://localhost:8085/afs-server' -entityPermId='a7bc2fbd-49af-4e2d-86cc-ea316028b793' -personalAccessToken='a13fe879-1753-41dd-8c3e-eb5a97e1c7be' -remDir='/remote/dir/absolute-path/' -enabled=true
```

#### jobs remove

When removing a synchronization-task, only the local directory needs to be specified:

```shell
./openbis-drive-cmd-line.sh jobs remove -dir='./my-local-dir'
```

Removing a synchronization-task means completely and definitively deleting it from the registered configuration.

#### jobs start

When starting or restarting a synchronization-task, only the local directory needs to be specified:

```shell
./openbis-drive-cmd-line.sh jobs start -dir='./my-local-dir'
```

Starting a synchronization-task merely means changing its state attribute from false to true (if not already true):
so the background-service actually executes it at each synchronization interval, starting from immediately.

#### jobs stop

When stopping a synchronization-task, only the local directory needs to be specified:

```shell
./openbis-drive-cmd-line.sh jobs stop -dir='./my-local-dir'
```

Stopping a synchronization-task merely means changing its state attribute from true to false (if not already false):
so the background-service will immediately interrupt it (if running) and will not execute it anymore at each synchronization interval.
Stopped synchronization-tasks are not deleted from configuration: they can be restarted at any time
with the `jobs start` command.

### Event and notification commands

#### events

Use this command to read the log of synchronization events (default-maximum-number of retrieved entries is 100):
```shell
./openbis-drive-cmd-line.sh events
```
(
or with `limit` option, to use a different default-number for the maximum number of retrieved events:
```shell
./openbis-drive-cmd-line.sh events -limit=10
```
)

Example response:
```
Events: 
----------
Synchronization-direction: UP
Local directory: /home/myuser/openbis_sync_test
Local file: /home/myuser/openbis_sync_test/images/3.png
Remote file: /remotedir/images/3.png
Directory: false
Source file deleted: true
Timestamp: 2025-11-04T13:51:14.382Z
----------
Synchronization-direction: DOWN
Local directory: /home/myuser/openbis_sync_test
Local file: /home/myuser/openbis_sync_test/images/4.png
Remote file: /remotedir/images/4.png
Directory: false
Source file deleted: false
Timestamp: 2025-11-04T13:49:01.828Z
```

#### notifications

Use this command to read the notifications raised and stored by the background-service (default-maximum-number of retrieved entries is 100):
```shell
./openbis-drive-cmd-line.sh notifications
```
(
or with `limit` option, to use a different default-number for the maximum number of retrieved notifications:
```shell
./openbis-drive-cmd-line.sh notifications -limit=10
```
)

Example response:
```
Notifications: 
----------
Type: JobException
Local directory: /home/myuser/openbis_sync_test
Local file: 
Remote file: 
Message: java.net.ConnectException exception with message: null
Timestamp: 2025-11-10T13:42:14.053Z
----------
Type: JobException
Local directory: /home/myuser/openbis_sync_test
Local file: 
Remote file: 
Message: java.net.ConnectException exception with message: null
Timestamp: 2025-11-05T15:03:54.017Z
```

At the moment of writing this documentation, possible types of notifications are:
- JobStopped : a synchronization-task was interrupted in the middle of execution
- JobException : an unexpected error occurred during the execution of a synchronization-task
- Conflict : a version conflict was detected between a local and the corresponding remote file, 
in that both were concurrently modified (see [File version conflict](#file-version-conflict))

## Graphical interface

To use the graphical interface, open a terminal and move the working directory (`cd`) to the appropriate `launch-scripts` location:
- Linux
```shell
cd $HOME/.local/state/openbis-drive/launch-scripts
```
- Windows CMD
```shell
cd %USERPROFILE%\AppData\Local\openbis-drive\launch-scripts
```
- Mac
```shell
cd $HOME/Library/"Application Support"/openbis-drive/launch-scripts
```

Then:
- on Linux and MAC-OS
```
./openbis-drive-gui.sh
```
- on Windows
```
openbis-drive-gui.bat
```

(To avoid the step of moving to the `launch-scripts` directory, you can add its specific path to
the `PATH` environment variable for your operating system: this is done differently on different
platforms. Please, check the documentation of your operating system.
At that point, also on MAC and Linux you will skip the initial dot when launching the command-line:
`openbis-drive-gui.sh` instead of `./openbis-drive-gui.sh` )

(If, during the installation process, a launch-icon was added to the desktop, that can be used too)

When started, the graphical interface will always check if the background-service is running:
if not, it will ask for confirmation to start it.

Functionalities offered by the graphical interface are essentially equivalent to those of the command-line,
with the increased usability and handiness of a window-application.

## File version conflict

If, in the context of a synchronization-task, a local and the corresponding remote file were concurrently modified,
the resulting case is not handled automatically by the background-service,
but rather presented as a "conflict notification".
In this situation, the remote version of the file is copied to the local file-system alongside the local one,
but suffixed with `.openbis-conflict`.
After checking both contents and adapted the local one if necessary,
delete the suffixed `.openbis-conflict` file to mark the resolution of the conflict:
at that point, the background process will keep the local version as the valid one.