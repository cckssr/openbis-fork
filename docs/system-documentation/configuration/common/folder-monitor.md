# Folder Monitor

Folder monitor is a mechanism similar to the DSS dropboxes which monitors a chosen folder for incoming files or folders and executes for them a
configurable task. As opposed to the DSS dropboxes the folder monitor can run anywhere (not only at DSS). The folder monitor together with V3 API and
Two-Phase-Commit protocol can be used to import both metadata and the data into openBIS in a single transaction. Because of that it can be thought of
as a successor of DSS dropbox concept but for the AFS managed data.

### Folder Monitor vs DSS Dropbox

| Feature                                              | Folder Monitor                                         | DSS dropbox                                                |
|------------------------------------------------------|--------------------------------------------------------|------------------------------------------------------------|
| Where can it run?                                    | anywhere                                               | at DSS only                                                |
| Can monitor a folder for incoming files and folders? | YES                                                    | YES                                                        |
| Can read/write both metadata and data?               | YES                                                    | YES                                                        |
| Keeps track of failed executions?                    | YES (via .faulty_paths)                                | YES (via .faulty_paths)                                    |
| Where can it read/write data from/to?                | AFS only                                               | DSS only                                                   |
| Supported modes                                      | MARKER_FILE and QUIET_PERIOD                           | MARKER_FILE and QUIET_PERIOD                               |
| Supports Jython tasks                                | NO                                                     | YES                                                        |
| Supports Java tasks                                  | YES                                                    | YES                                                        |
| Supports third party Java libraries                  | YES                                                    | YES                                                        |
| Supports debugging                                   | YES (it can be run and debugged locally within an IDE) | NO (it only runs at DSS)                                   |
| Supports transactions                                | YES (Two-Phase-Transactions protocol)                  | YES (proprietary mechanism)                                |
| Supports V3 API in transactions?                     | YES (all V3 API method calls are transactional)        | NO (only V1 API and V2 API method calls are transactional) |

### Folder Monitor Distribution

Folder monitor is distributed as a zip file which contains all necessary jars to connect to an openBIS instance and use V3 API and transactions.

### Starting and stopping Folder Monitor

To start the folder monitor, run the following command from within the folder monitor folder: 
```
    ./bin/folder_monitor.sh start
```

To stop the folder monitor, run the following command from within the folder monitor folder:
```
    ./bin/folder_monitor.sh stop
```

### Folder Monitor Configuration

A folder monitor can be configured via `etc/service.properties` file. An example of such file:

```properties
# Folder to monitor for incoming files/folders (if relative path is used, then it is resolved against the main folder of the folder monitor)
folder=test-path/test-folder

# Supported modes:
# - "MARKER_FILE" : incoming file/folder is recognized only after a marker file with name ".MARKER_is_finished_<INCOMING_NAME>" is created next to the incoming file/folder 
# - "QUIET_PERIOD" : incoming file/folder is recognized only after a quiet period with no changes to the incoming file/folder elapses 
mode=MARKER_FILE

# How often the monitored folder is checked for changes
checking-interval=1s

# Quiet period (only needed when mode = QUIET_PERIOD)
# quiet-period=1min

# Full Java class name of a task that should be used for processing of the incoming files/folders
task.class=ch.ethz.test.TestFolderMonitorTask
# Additional configuration properties of the task
task.my-property-1=ABC
task.my-property-2=123
```
### Folder Monitor Task

A folder monitor task has to implement `ch.ethz.sis.foldermonitor.FolderMonitorTask` interface:

```java
public interface FolderMonitorTask
{
    // Invoked only once at the start of the folder monitor. Properties assigned to the task in `service.properties` are passed here as a parameter.
    void configure(Properties properties) throws Exception;

    // Invoked for every incoming file/folder. Path of the incoming file/folder is passed here as a parameter.
    void process(Path incoming) throws Exception;
}
```
### ELN Folder Monitor Task

If you are looking for an example on how to migrate your own DSS Jython dropboxes to the new folder monitor based solution, please check
the openBIS ELN dropbox code. The dropbox was originally written in Jython to run on DSS. Now, a newer Java based version is also available. 
It offers similar functionality but implements it using the new folder monitor mechanism and new V3 API Two-Phase-Commit transactions.

Old ELN dropbox:
https://sissource.ethz.ch/sispub/openbis/-/blob/master/ui-eln-lims/src/core-plugins/eln-lims/1/dss/drop-boxes/eln-lims-dropbox/eln-lims-dropbox.py

New ELN dropbox:
https://sissource.ethz.ch/sispub/openbis/-/blob/master/ui-eln-lims/src/main/java/ch/ethz/sis/elnlims/dropbox/ElnDropbox.java

### Folder Monitor Task Development

Folder monitor tasks are far easier to develop than the old DSS dropboxes. The tasks can be developed and tested in your local environment. For instance:

```java
public class FolderMonitorTaskExample implements FolderMonitorTask
{

    @Override public void configure(final Properties properties)
    {
        // your code goes here
    }

    @Override public void process(final Path incoming) throws Exception
    {
        // your code goes here
    }

    public static void main(String[] args) throws Exception
    {
        // An easy way to run your folder monitor task in a development environment by manually creating the task and
        // calling "configure" and "process" methods exactly how the folder monitor mechanism would.
        Properties properties = new Properties();
        FolderMonitorTaskExample exercise = new FolderMonitorTaskExample();
        exercise.configure(properties);
        exercise.process(Path.of("test-file-or-folder-1"));
        exercise.process(Path.of("test-file-or-folder-2"));
    }
}

```
