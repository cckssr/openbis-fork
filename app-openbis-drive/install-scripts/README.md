# OpenBIS Drive

A synchronization tool for openBIS (https://openbis.ch)

Distribution package containing installation and executable files for:
- background synchronization process
- a command-line program to interact with it
- a graphical user-interface

## Installation

In many places, this document refers to the system-dependent installation directory and subdirectories in this way:
- [installation directory](#installation-directory)
- [launch-scripts directory](#launch-scripts-directory)
- [configuration directory](#state-and-configuration-directory)

See [appendix](#appendix) at the and of the document.

### Automatic installation through scripts

According to your operating system, run as program the appropriate script in this same directory:

- `install-on-linux.sh`
- `install-on-mac.sh`
- `install-on-windows.bat`

You will be prompted for confirmation, to copy the entire launch-scripts directory to the [launch-scripts directory](#launch-scripts-directory).

A message will explain how to copy a desktop-link to your desktop, as a launcher for the graphical interface.

### Manual installation

Simply copy the entire directory launch-scripts from this distribution package to the [launch-scripts directory](#launch-scripts-directory).

## Deinstallation

### Automatic deinstallation through scripts

According to your operating system, run as program the appropriate script in this same directory:

- `uninstall-on-linux.sh`
- `uninstall-on-mac.sh`
- `uninstall-on-windows.bat`

You will be prompted for confirmation, to delete the [launch-scripts directory](#launch-scripts-directory)

You will then be prompted for confirmation, to delete the [configuration directory](#state-and-configuration-directory)
(keeping the configuration can be useful not to lose your current data, 
if you plan to reinstall a compatible openBIS Drive version in the future).

### Manual deinstallation

Simply remove the entire [launch-scripts directory](#launch-scripts-directory).

If you want to remove configuration files too, remove the [configuration directory](#state-and-configuration-directory).

## Running the application

### Application launch and stop (Linux and MAC OS)

The background-process can be started by entering into the [launch-scripts directory](#launch-scripts-directory)
(or by adding this to the PATH environment variable) and:
- by using the command-line-application start command: `./openbis-drive-cmd-line.sh start`
  (`openbis-drive-cmd-line.sh start` without initial dot if the [launch-scripts directory](#launch-scripts-directory) is added to the PATH)
- directly with the start-script: `./openbis-drive-service-start.sh`
  (`openbis-drive-service-start.sh start` without initial dot if the [launch-scripts directory](#launch-scripts-directory) is added to the PATH)
- through the graphical interface, which will always prompt for starting the background-process, if that is not running (see "Graphical user-interface" section below)

Check running status with: `openbis-drive-cmd-line.sh status`

Stop:
- by using the command-line-application stop command: `./openbis-drive-cmd-line.sh stop`
  (`openbis-drive-cmd-line.sh stop` without initial dot if the [launch-scripts directory](#launch-scripts-directory) is added to the PATH)
- directly with the stop-script: `./openbis-drive-service-stop.sh`
  (`openbis-drive-service-stop.sh start` without initial dot if the [launch-scripts directory](#launch-scripts-directory) is added to the PATH)

### Application launch and stop (Windows)

The background-process can be started by entering into the [launch-scripts directory](#launch-scripts-directory)
(or by adding this to the PATH environment variable) and:
- by using the command-line-application start command: `openbis-drive-cmd-line.bat start`
- directly with the start-script: `openbis-drive-service-start.bat`
- through the graphical interface, which will always prompt for starting the background-process, if that is not running (see "Graphical user-interface" section below)

Check running status with: `openbis-drive-cmd-line.bat status`

Stop:
- by using the command-line-application stop command: `openbis-drive-cmd-line.bat stop`
- directly with the stop-script: `openbis-drive-service-stop.bat`

### Command-line (Linux and MAC OS)

The command-line can be invoked by entering into the [launch-scripts directory](#launch-scripts-directory)
(or by adding this to the PATH environment variable) and typing:
- `./openbis-drive-cmd-line.sh help`
  (`openbis-drive-cmd-line.sh help` without initial dot if the [launch-scripts directory](#launch-scripts-directory) is added to the PATH)

### Command-line (Windows)

The command-line can be invoked by entering into the [launch-scripts directory](#launch-scripts-directory)
(or by adding this to the PATH environment variable) and typing:
- `openbis-drive-cmd-line.bat help`

### Graphical user-interface (Linux and MAC OS)

The graphical user-interface can be started by entering into the [launch-scripts directory](#launch-scripts-directory)
(or by adding this to the PATH environment variable) and typing:
- `./openbis-drive-gui.sh`
  (`openbis-drive-gui.sh` without initial dot if the [launch-scripts directory](#launch-scripts-directory) is added to the PATH)

Alternatively and more directly, through the desktop-link, if it was added to the desktop.

### Graphical user-interface (Windows)

The graphical user-interface can be started by entering into the [launch-scripts directory](#launch-scripts-directory)
(or by adding this to the PATH environment variable) and typing:
- `openbis-drive-gui.bat`

Alternatively and more directly, through the desktop-link, if it was added to the desktop.

### Appendix:
#### Installation directory
- for Linux: `$HOME/.local/state/openbis-drive/`
- for Windows: `%USERPROFILE%\AppData\Local\openbis-drive\`
- for MAC-OS: `$HOME/Library/"Application Support"/openbis-drive/`

With subdirectories: 

#### Launch-scripts directory

With executable files:

- for Linux: `$HOME/.local/state/openbis-drive/launch-scripts`
- for Windows: `%USERPROFILE%\AppData\Local\openbis-drive\launch-scripts`
- for MAC-OS: `$HOME/Library/"Application Support"/openbis-drive/launch-scripts`

#### State and configuration directory
- for Linux: `$HOME/.local/state/openbis-drive/state`
- for Windows: `%USERPROFILE%\AppData\Local\openbis-drive\state`
- for MAC-OS: `$HOME/Library/"Application Support"/openbis-drive/state`

