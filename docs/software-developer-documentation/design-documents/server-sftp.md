# AFS SFTP Server Specification

# Current Status - Original Data Store FTP/FTPS/SFTP
- FTP <- Original Unencrypted protocol
- FTPS <- FTP Secure, it tries to do a handshake to provide encryption, if the handshake fails defaults to FTP
  These 2 protocols are today deprecated and disabled even if the source code has not been removed.
- SFTP <- SSH File transfer protocol, always encrypted
  All protocols on the original data store are `READ ONLY`

# Development proposal - AFS SFTP (server-sftp)
## Features
Only one protocol SFTP
Will support both upload and download files under data
- LOGIN
- UPLOAD data
- DOWNLOAD data
- LIST entities and files under data
    - When listing an entity, it will always show the NAME property as the path name, in absence of the NAME property the CODE is shown instead.
- CREATE On a second version we can allow  to also create entities in addition to files/folders
- RENAME On a second version we can allow  to also rename entities in addition to files/folders
    - To RENAME the entity needs to have a NAME property, when trying to rename a code of an entity without a NAME property an error is raised.
- DELETE On a second version we can allow  to also delete entities in addition to files/folders

- All operations should be done over files/folders after the first development iteration.
- Leaves operations over entities as a follow up.

### Providing an SFTP endpoint
The endpoint provides the typical SFTP commands and the server translates them using `api-openbis-java` to `server-application-server` commands to list entities and `server-data-store` to upload and download files.

### SFTP endpoint plugin system
The plugin receives a path and operation and returns the result from that operation and path, this is to allow to represent the entity and data tree in custom ways. The default implementation is an implementation of this plugin system.

**Example 1:**
LIST /spaces/MY_SPACE/

/spaces/MY_SPACE/projects
/spaces/MY_SPACE/samples

**Example 2:**
CREATE /spaces/MY_SPACE/samples/MY_SAMPLE (Sample Type)

/spaces/MY_SPACE/samples/MY_SAMPLE

## Technology decisions / libraries for the project
What library we choose to provide an SFTP endpoint?
Historically Java SFTP libraries have been particularly slow compared with native options.

We are moving new openBIS projects to use Netty as the library to implement protocols since is a newer for efficient library.

Apache Mina with the sshd-netty core seems a fit.

## Project configuration and general software patterns
- It should follow the steps used at `server-data-store`
- Same way of configuring service.properties
    - SFTP port
    - server-application-server URL
    - server-data-store URL
- Same way of configuring logging
- Same pattern to startup the server
- etc...

Important: This new project uses ONLY the AFS API to do operations with files. The new AFS API already can read the older Datasets. This project NEVER uses the older Datastore server API for anything.

## Other design choices
## Login
SFTP login with user and password that is the same as their openBIS user and password.
Login requests are handed over to the AS V3 Login API, if successful the SFTP returns a successful login and returns an SFTP session.
NOTE: Historically the users should have needed to login previously to the Web UI before login to the SFTP session, don't know if this is still true.
Under any case we should manage the login similarly to the current SFTP server.

## openBIS graph/folder structure
We currently agree on implementing option 2

### Option 1: Entity Kind Postfix
/MY_SPACE (Space)/MY_PROJECT (Project)/MY_EXPERIMENT (Experiment)

**Example creation:**
/MY_SPACE (Space)/MY_PROJECT (Project)/MY_EXPERIMENT (Experiment) (Experiment Type Code) -> /MY_SPACE (Space)/MY_PROJECT (Project)/MY_EXPERIMENT (Experiment)
/MY_SPACE (Space)/MY_PROJECT (Project)/MY_SAMPLE (Sample) (Sample Type Code) -> /MY_SPACE (Space)/MY_PROJECT (Project)/MY_SAMPLE (Sample)

### Option 2: Entity Kind Directory
/spaces/MY_SPACE/projects/MY_PROJECT/experiments/MY_EXPERIMENT

**Example creation:**
/spaces/MY_SPACE/projects/MY_PROJECT/experiments/MY_EXPERIMENT (Experiment Type Code) -> /spaces/MY_SPACE/projects/MY_PROJECT/experiments/MY_EXPERIMENT

- (PRO) This convention is a lot easier to use for creating new entities, because the entity kind don't need to be indicated, since is already the directory name.
- (PRO) Listing and Sorting goes one to one with the openBIS API.

## openBIS graph unfolding challenges
We currently agree on implementing option 2

### Option 1: Try to show entities that could appear in different places in ONE of them
ELN-LIMS try to follow the next rule:
- Children Samples and DataSets ALWAYS are shown under their children folder if they belong to the same space, if not, they are shown on the space they belong to.

**Example:**

B child A, C child B

If a sample has a parent, is not represented directly connected to the project

/spaces/MY_SPACE_A/projects/MY_PROJECT/samples/
/spaces/MY_SPACE_A/projects/MY_PROJECT/samples/MY_SAMPLE_A
/spaces/MY_SPACE_A/projects/MY_PROJECT/samples/MY_SAMPLE_A/children/MY_SAMPLE_B/children/MY_SAMPLE_C

Ideally Samples and DataSets SHOULD only appear on the tree ONCE but in practice the ideal doesn't happen

**Example - Converging paths between spaces:**
B child of A (In another space)

/spaces/MY_SPACE_A/projects/MY_PROJECT/samples/MY_SAMPLE_A/children/MY_SAMPLE_B

/spaces/MY_SPACE_B/projects/MY_PROJECT/samples/MY_SAMPLE_B/

On the example Above MY_SAMPLE_B could be shown under the Space it belongs to to avoid showing it in two places.

But then it doesn't appear under the children.

**Example - Converging paths same space:**
C child of A and B (same space)

/spaces/MY_SPACE_A/projects/MY_PROJECT/samples/MY_SAMPLE_A/children/MY_SAMPLE_C
/spaces/MY_SPACE_A/projects/MY_PROJECT/samples/MY_SAMPLE_B/children/MY_SAMPLE_C

/spaces/MY_SPACE_A/projects/MY_PROJECT/samples/MY_SAMPLE_C

On the example Above MY_SAMPLE_C is child of both MY_SAMPLE_A and MY_SAMPLE_B. Where should it be shown? Because it has parents it should not be shown under

/spaces/MY_SPACE_A/projects/MY_PROJECT/samples/MY_SAMPLE_C

But you have 2 options after to show it under

/spaces/MY_SPACE_A/projects/MY_PROJECT/samples/MY_SAMPLE_A/children/MY_SAMPLE_C
/spaces/MY_SPACE_A/projects/MY_PROJECT/samples/MY_SAMPLE_B/children/MY_SAMPLE_C

Now if you show it in both of them, and you download MY_PROJECT you will be duplicating the data you download from here on.

### Option 2: Try to show entities that could appear in different places in all of them
Showing all entities under all possible places

/spaces/MY_SPACE/projects/MY_PROJECT/samples/
/spaces/MY_SPACE/projects/MY_PROJECT/samples/MY_SAMPLE_A
/spaces/MY_SPACE/projects/MY_PROJECT/samples/MY_SAMPLE_B
/spaces/MY_SPACE/projects/MY_PROJECT/samples/MY_SAMPLE_C
/spaces/MY_SPACE/projects/MY_PROJECT/samples/MY_SAMPLE_A/children/MY_SAMPLE_B/children/MY_SAMPLE_C

## Future Developments - Separate folder structure composition from parents/children
1. In theory is possible that entities like files and folders in a file system are only owned by one folder.
2. Have parents and children modeled separately than the owner.

This would solve situations where an entity can be shown on different parts of the openBIS navigation tree.