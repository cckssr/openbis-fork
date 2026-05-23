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

In many places, this document refers to the system-dependent installation directory and subdirectories in this way:
- [installation directory](#installation-directory)
- [launcher and command-line directory](#launcher-and-command-line-directory)
- [configuration directory](#state-and-configuration-directory)

See [appendix](#appendix) at the end of the document.

### System requirements

- for Linux and MAC-OS: check that `pkill` utility is available in terminal
  (it should be preinstalled and already available in all recent distributions)

### Installation

- **Linux** (Debian based, AMD-64 architecture) : double-click on the .deb installation package (or from command line: `sudo dpkg -i openbis-drive-***.deb`) to start the installation process
- **Windows** : double-click (or launch from console) the installer `openbis-drive-***.exe` to start the installation process (recommended: confirm the creation of a menu shortcut)
- **MAC OS** : open the `openbis-drive-***.dmg` package and drag openBIS Drive to the `/Applications` folder

### Deinstallation

Before uninstalling the application, make sure the background-process (see [Running the application](#running-the-application)) is stopped: otherwise it will keep running until you log out from the system.

- **Linux** (Debian based, AMD-64 architecture) : from console: `sudo apt-get purge openbis-drive` (or `sudo apt purge openbis-drive`)
- **Windows** : from main menu, look for "Installed Apps" administration tool and remove openBIS Drive from there
- **MAC OS** : open the `/Applications` folder in "Finder" (file explorer) and drag openBIS Drive from there to the "recycle bin"

You can keep files under the [configuration directory](#state-and-configuration-directory) , if you wish to find your last openBIS Drive data,
in case you install a compatible openBIS Drive version again in the future; otherwise, you can safely remove that folder.

## Running the application

### Application launch and stop

The background-process can be started:
- through the graphical interface, which will always prompt for starting the background-process, if that is not running (see [Graphical user-interface](#graphical-user-interface) section below)
- by using the command-line-application start command:
    - `openbis-drive start` for Linux and MAC OS
    - `openbis-drive-cmd.exe start` for Windows

Check running status with the command-line:
- `openbis-drive status` for Linux and MAC OS
- `openbis-drive-cmd.exe status` for Windows

Stop with the command-line:
- `openbis-drive stop` for Linux and MAC OS
- `openbis-drive-cmd.exe stop` for Windows

See [Command-line](#command-line) section below.


### Application data directory

According to the platform, application installation and executable files, configuration and data will be stored in:
- [installation directory](#installation-directory)
- [configuration directory](#state-and-configuration-directory)

When uninstalling, the possibility is given to maintain the [configuration directory](#state-and-configuration-directory),
if, for example, one wishes to maintain the current configuration and data for future compatible installations.

## Command-line

The command-line can be invoked in a terminal by entering into the [launcher and command-line directory](#launcher-and-command-line-directory)
(or by adding this to the PATH environment variable) and typing:

- **Linux** and **MAC OS** : `./openbis-drive help`
  (`openbis-drive-cmd-line.sh help` without initial dot if the [launcher and command-line directory](#launcher-and-command-line-directory) is added to the PATH)
- **Windows** : `openbis-drive-cmd.exe help`


Use it as in the following examples:
- on Linux and MAC-OS
```
./openbis-drive help
./openbis-drive status
./openbis-drive ...other-command...
```
- on Windows
```
openbis-drive-cmd.exe help
openbis-drive-cmd.exe status
openbis-drive-cmd.exe ...other-command...
```

(To avoid the step of moving to the [launcher and command-line directory](#launcher-and-command-line-directory), you can add its specific path to
the `PATH` environment variable for your operating system: this is done differently on different
platforms. Please, check the documentation of your operating system.
At that point, also on MAC and Linux you will skip the initial dot when launching the command-line:
`openbis-drive` instead of `./openbis-drive` )

### Basic commands

(Note: from here on, `./openbis-drive` will be used.
Replace this with `openbis-drive-cmd.exe` on Windows and
with `openbis-drive` on Linux and MAC-OS with adapted `PATH` variable)

#### help

```shell
./openbis-drive help
```

Returns a short helping guide for the command-line tool itself:

```
Supported commands:
    start   -> starts the background service
    stop    -> stops the background service
    status  -> prints the status of the background service

    config  -> prints the configuration with which the background service is running
    config -startAtLogin=true|false -language=en|fr|de|it|es -syncInterval=120   -> sets configuration parameters: two-letter ISO-code for language, synchronization-interval in seconds (defaults: false, 'en', 120 seconds = 2 minutes)
    config ignored-path-patterns -> shows the active global default list of ignored file patterns (glob syntax)
    config ignored-path-patterns -showFactoryDefault -> shows the showFactoryDefault (not necessarily active) global default list of ignored file patterns (glob syntax)
    config ignored-path-patterns -reset -> resets the active global default list of ignored file patterns to factory-default values
    config ignored-path-patterns -set -> sets new ignored path-patterns from console-input
    config ignored-path-patterns -setFromFile -> sets new ignored path-patterns
        from UTF-8 multiline text-file with a glob expression on each line

    jobs    -> prints the currently registered synchronization-jobs
    jobs add -title='Description...' -type='Bidirectional|Upload|Download' -dir='./dir-a/dir-b' -openBISurl='https://...' -entityPermId='123-abc-...' -personalAccessToken='098abc...' -remDir='/remote/dir/absolute-path/' -enabled=true|false  ( optional: -ignoreFiles=GlobalDefault|SpecificList|None with default-value: GlobalDefault )
    jobs remove -dir='./dir-a/dir-b'
    jobs start -dir='./dir-a/dir-b'
    jobs stop -dir='./dir-a/dir-b'

    jobs ignored-path-patterns -dir='./dir-a/dir-b' (shows the ignored path-patterns for the job related to this local directory)
    jobs ignored-path-patterns -dir='./dir-a/dir-b' -set -> sets new ignored path-patterns from console-input (glob syntax)
    jobs ignored-path-patterns -dir='./dir-a/dir-b' -setFromFile=./documents/new-patterns.txt -> sets new ignored path-patterns
        from UTF-8 multiline text-file with a glob expression on each line

    notifications -limit=100   (default: 100)  -> prints the last limit-number of notifications
    events -limit=100   (default: 100)  -> prints the last limit-number of events

```

#### status

```shell
./openbis-drive status
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
Title: Descriptive title...
Type: Bidirectional
Local directory: /home/myuser/openbis_sync_test
openBIS url: http://localhost:8085
Entity-perm-id: a7bc2fbd-49af-4e2d-86cc-ea316028b793
Remote directory: /remotedir
Personal access token: a13fe879-1753-41dd-8c3e-eb5a97e1c7be
Enabled: false
Ignored files: GlobalDefault
```

If the background-service is not running:

```
OpenBIS Drive Service is not running.
```

#### start

```shell
./openbis-drive start
```

Starts the background process.

#### stop

```shell
./openbis-drive stop
```

Stops the background process.

### Config commands

#### config

```shell
./openbis-drive config
```

Prints the current configuration of the background-process:

```
Start-at-login: false
Language: en
Sync-interval: 60 seconds
Synchronization-jobs: 
----------
Title: Descriptive title...
Type: Bidirectional
Local directory: /home/myuser/openbis_sync_test
openBIS url: http://localhost:8085
Entity-perm-id: a7bc2fbd-49af-4e2d-86cc-ea316028b793
Remote directory: /remotedir
Personal access token: a13fe879-1753-41dd-8c3e-eb5a97e1c7be
Enabled: false
Ignored files: GlobalDefault
```

- Start-at-login: indicates if the background-process has to be started at system-login
- Language: one of en (English), de (German), fr (French), it (Italian), es (Spanish):
it affects only the graphical interface
- Synchronization-interval: every which number of seconds the synchronization checks have to run
- At the end: a list of the registered synchronization-tasks

Use the `config` command with options to modify the configuration 
(not necessarily all options have to be specified):
```shell
./openbis-drive -startAtLogin=true -language=fr -syncInterval=60
```
```
Start-at-login: true
Language: fr
Sync-interval: 60 seconds
Synchronization-jobs: 
----------
Title: Descriptive title...
Type: Bidirectional
Local directory: /home/myuser/openbis_sync_test
openBIS url: http://localhost:8085
Entity-perm-id: a7bc2fbd-49af-4e2d-86cc-ea316028b793
Remote directory: /remotedir
Personal access token: a13fe879-1753-41dd-8c3e-eb5a97e1c7be
Enabled: false
Ignored files: GlobalDefault
```

#### config ignored-path-patterns

Use the `config` command with `ignored-path-patterns` subcommand, to inspect and possibly to modify the global-default list of ignored path-patterns.

That is a list of glob expressions used only by synchronization-tasks which work in `GlobalDefault` ignore-files mode (see: [jobs ignored-path-patterns](#jobs-ignored-path-patterns) ).
For such tasks, the background-process ignores local files whose path (relative to the synchronization-task local-root-directory) matches at least one of the glob expressions.

```shell
./openbis-drive config ignored-path-patterns
./openbis-drive config ignored-path-patterns -showFactoryDefault
./openbis-drive config ignored-path-patterns -reset
./openbis-drive config ignored-path-patterns -set
./openbis-drive config ignored-path-patterns -setFromFile=./dir/new-patterns.txt
```

Without options: it shows the currently saved list of glob expressions which are considered global-default.

With `-showFactoryDefault` option: it shows the factory-default list of glob-expressions for ignored paths.

With `-reset` option: it resets the global-default list of glob expressions to the factory-default list.

With `-set` option: it sets the global-default list of glob expressions to new values, given one after the other from console input.

With `-setFromFile` option: it sets the global-default list of glob expressions to new values, given from a UTF-8 multiline text file.

### Jobs commands

#### jobs

```shell
./openbis-drive jobs
```

Prints the synchronization-tasks registered in the background-service:

```
Synchronization-jobs: 
----------
Title: Descriptive title...
Type: Bidirectional
Local directory: /home/myuser/openbis_sync_test
openBIS url: http://localhost:8085
Entity-perm-id: a7bc2fbd-49af-4e2d-86cc-ea316028b793
Remote directory: /remotedir
Personal access token: a13fe879-1753-41dd-8c3e-eb5a97e1c7be
Enabled: false
Ignored files: GlobalDefault
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
- a mode of ignoring some local paths (defaults to "GlobalDefault": that is, local paths which match certain global-default glob expressions are skipped)
- a list of glob expressions which determine which local paths have to be ignored (if the mode of ignoring files is `SpecificList` ) (see [jobs ignored-path-patterns](#jobs-ignored-path-patterns))

#### jobs add

When adding a new synchronization-task, all of its properties must be specified:

```
jobs add -type='Bidirectional|Upload|Download' -dir='./my-local-dir' -openBISurl='https://...' -entityPermId='123-abc-...' -personalAccessToken='098abc...' -remDir='/remote/dir/absolute-path/' -enabled=true|false
```

Option that represent properties are:
```
-title   : descriptive title
-type   :   one of Bidirectional, Upload, Download
-dir   :   local directory
-openBISurl   :   openBIS server URL (example: 'http://localhost:8080')
-entityPermId   :   entity ID on openBIS server
-remDir   :   absolute path of the remote directory on openBIS server within the entity ID
-personalAccessToken   :   personal access token
-enabled   :   true or false
-ignoreFiles   :   GlobalDefault|SpecificList|None ( optional: with default-value: GlobalDefault )
```

For example: 

```shell
./openbis-drive jobs add -title='Description' -type='Bidirectional' -dir='/home/myuser/openbis_sync_test' -openBISurl='http://localhost:8085' -entityPermId='a7bc2fbd-49af-4e2d-86cc-ea316028b793' -personalAccessToken='a13fe879-1753-41dd-8c3e-eb5a97e1c7be' -remDir='/remote/dir/absolute-path/' -enabled=true
```

#### jobs remove

When removing a synchronization-task, only the local directory needs to be specified:

```shell
./openbis-drive jobs remove -dir='./my-local-dir'
```

Removing a synchronization-task means completely and definitively deleting it from the registered configuration.

#### jobs start

When starting or restarting a synchronization-task, only the local directory needs to be specified:

```shell
./openbis-drive jobs start -dir='./my-local-dir'
```

Starting a synchronization-task merely means changing its state attribute from false to true (if not already true):
so the background-service actually executes it at each synchronization interval, starting from immediately.

#### jobs stop

When stopping a synchronization-task, only the local directory needs to be specified:

```shell
./openbis-drive jobs stop -dir='./my-local-dir'
```

Stopping a synchronization-task merely means changing its state attribute from true to false (if not already false):
so the background-service will immediately interrupt it (if running) and will not execute it anymore at each synchronization interval.
Stopped synchronization-tasks are not deleted from configuration: they can be restarted at any time
with the `jobs start` command.

#### jobs ignored-path-patterns

Each synchronization-task can have a different ignore-files mode:

- `GlobalDefault` : the default option for ignore-files mode: it means that the background-process will ignore local files matching the global-default list of ignored-patterns (see: [config ignored-path-patterns](#config-ignored-path-patterns))
- `SpecificList` : it means that the background-process will ignore local files matching the task-specific list of ignored-patterns (see below)
- `None` : it means, no files are ignored by the background-process

This subcommand allows to inspect and modify the ignored path-patterns specific to a synchronization-task with different options.
Note: the synchronization-task-specific list of ignored-patterns is taken into account by the background-process if and only if the ignore-files mode is `SpecificList`.

```shell
./openbis-drive jobs ignored-path-patterns -dir='./my-local-dir'
./openbis-drive jobs ignored-path-patterns -dir='./my-local-dir' -set
./openbis-drive jobs ignored-path-patterns -dir='./my-local-dir' -setFromFile=./documents/new-patterns.txt
```

With `-dir` option: for the synchronization-task with local directory indicated by `-dir`,
it shows the specific-list of glob expressions, which are matched against local paths (relative to the local task-directory) to determine which of them should be ignored.

With `-dir` and `-reset` option: for the synchronization-task with local directory indicated by `-dir`,
it resets the list of ignored path-patterns to the global-default list: see [config ignored-path-patterns](#config-ignored-path-patterns)

With `-dir` and `-set` option: for the synchronization-task with local directory indicated by `-dir`,
it sets the list of ignored path-patterns to a new list, given one line after the other from console with glob-syntax.

With `-dir` and `-setFromFile` option: for the synchronization-task with local directory indicated by `-dir`,
it sets the list of ignored path-patterns to a new list, given from a UTF-8 multiline text-file (as per `-setFromFile`)
with a glob expression on each line.

### Event and notification commands

#### events

Use this command to read the log of synchronization events (default-maximum-number of retrieved entries is 100):
```shell
./openbis-drive events
```
(
or with `limit` option, to use a different default-number for the maximum number of retrieved events:
```shell
./openbis-drive events -limit=10
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
./openbis-drive notifications
```
(
or with `limit` option, to use a different default-number for the maximum number of retrieved notifications:
```shell
./openbis-drive notifications -limit=10
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

## Graphical user-interface

The most direct way to start the graphical user interface is through the platform-specific menu short-cut.

The graphical user-interface can also be started by entering into the [launcher and command-line directory](#launcher-and-command-line-directory)
(or by adding this to the PATH environment variable) and typing:

- **Linux** and **MAC OS** : `./openbis-drive`
  (`openbis-drive` without initial dot if the [launcher and command-line directory](#launcher-and-command-line-directory) is added to the PATH)
- **Windows** : `openbis-drive.exe`

(To avoid the step of moving to the [launcher and command-line directory](#launcher-and-command-line-directory), you can add its specific path to
the `PATH` environment variable for your operating system: this is done differently on different
platforms. Please, check the documentation of your operating system.
At that point, also on MAC and Linux you will skip the initial dot when launching the command-line:
`openbis-drive` instead of `./openbis-drive` )

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
After checking both contents and adapting the local one if necessary,
delete the suffixed `.openbis-conflict` file to mark the resolution of the conflict:
at that point, the background process will keep the local version as the valid one.

## Appendix:
#### Installation directory
- for Linux: `/opt/openbis-drive`
- for Windows: `C:\Program Files\openbis-drive`
- for MAC OS: `/Applications/openbis-drive`

#### Launcher and command-line directory
- for Linux: `/opt/openbis-drive/bin`
- for Windows: `C:\Program Files\openbis-drive`
- for MAC OS: `/Applications/openbis-drive.app/Contents/MacOS`

In particular, the command-line executable is:
- for Linux: `/opt/openbis-drive/bin/openbis-drive`
- for Windows: `C:\Program Files\openbis-drive\openbis-drive-cmd.exe`
- for MAC OS: `/Applications/openbis-drive.app/Contents/MacOS/openbis-drive`

#### State and configuration directory
- for Linux: `$HOME/.local/state/openbis-drive/state`
- for Windows: `%USERPROFILE%\AppData\Local\openbis-drive\state`
- for MAC OS: `$HOME/Library/"Application Support"/openbis-drive/state`
