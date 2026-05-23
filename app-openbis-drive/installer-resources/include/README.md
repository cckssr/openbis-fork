# OpenBIS Drive

A synchronization tool for openBIS (https://openbis.ch)

Distribution package containing the installation executable file.

## Installation

In many places, this document refers to the system-dependent installation directory and subdirectories in this way:
- [installation directory](#installation-directory)
- [launcher and command-line directory](#launcher-and-command-line-directory)
- [configuration directory](#state-and-configuration-directory)

See [appendix](#appendix) at the end of the document.

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

### Command-line

The command-line can be invoked by entering into the [launcher and command-line directory](#launcher-and-command-line-directory)
(or by adding this to the PATH environment variable) and typing:

- **Linux** and **MAC OS** : `./openbis-drive help`
  (`openbis-drive-cmd-line.sh help` without initial dot if the [launcher and command-line directory](#launcher-and-command-line-directory) is added to the PATH)
- **Windows** : `openbis-drive-cmd.exe help`

### Graphical user-interface

The most direct way to start the graphical user interface is through the platform-specific menu short-cut.

The graphical user-interface can also be started by entering into the [launcher and command-line directory](#launcher-and-command-line-directory)
(or by adding this to the PATH environment variable) and typing:

- **Linux** and **MAC OS** : `./openbis-drive`
  (`openbis-drive` without initial dot if the [launcher and command-line directory](#launcher-and-command-line-directory) is added to the PATH)
- **Windows** : `openbis-drive.exe`

### Skipping TLS certificate checks for OpenBIS servers

If working with OpenBIS servers covered by self-signed TLS certificates (or anyway not validatable by public certificate authorities, or expired, ...),
one can modify (create if necessary) a file openbis-drive.properties under

for Linux: `$HOME/.local/state/openbis-drive/`

for Windows: `%USERPROFILE%\AppData\Local\openbis-drive\`

for MAC-OS: `$HOME/Library/"Application Support"/openbis-drive/`

adding (or adapting) the following key-value line: `ch.ethz.sis.afs.client.client.noTLSCertCheck=true`

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

