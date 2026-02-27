# App openBIS-Drive

This application can keep local directories synchronized with AFS-server directories.
It consists of a background-process, which performs the synchronization tasks,
and of a graphical interface and a command-line interface to start, stop, configure and monitor it.

## Start in development environment

See Gradle tasks:

- `openBISDevelopmentEnvironmentDriveServerStart`
- `openBISDevelopmentEnvironmentDriveGUIStart`
- `openBISDevelopmentEnvironmentDriveCommandLine`

## Manual installation

After building the Gradle task: `manualRelease`

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

## Skipping TLS certificate checks for OpenBIS servers

If working with OpenBIS servers covered by self-signed TLS certificates (or anyway not validatable by public certificate authorities, or expired, ...),
one can modify (create if necessary) a file openbis-drive.properties under

for Linux: `$HOME/.local/state/openbis-drive/`

for Windows: `%USERPROFILE%\AppData\Local\openbis-drive\`

for MAC-OS: `$HOME/Library/"Application Support"/openbis-drive/`

adding (or adapting) the following key-value line: `ch.ethz.sis.afs.client.client.noTLSCertCheck=true`

## Working with local AS and AFS servers in development environment

In normal installations, AS and AFS servers are reachable at the same URL (http(s)://hostname:port) through different paths.
In development environments, AS and AFS servers are often started on separate ports on localhost:
yet, only one URL can be configured on a synchronization task and this is used both
- to retrieve openBIS entities from AS in the graphical interface

and
- to communicate with AFS and perform the actual synchronization in the background-service

In order to deal with these scenarios, an environment variable can be used when starting the graphical interface
(for example, in Gradle task: `openBISDevelopmentEnvironmentDriveGUIStart`)

`OPENBIS_DRIVE_LOCAL_DEVELOPMENT_AS_AND_AFS_URL=true`

This enables a development-feature-toggle that allows to work with:
- AFS listening on `localhost:8085`
- AS listening on `localhost:8888`
- synchronization tasks configured with openBIS-URL `http://localhost:8085`

(Different ports can easily be handled by adapting [OpenBISQueryUtil.java](src/main/java/ch/openbis/drive/util/OpenBISQueryUtil.java))