AFS Server Configuration
========================

### Folder Structure
bin
lib
log
etc

### bin
Contains one file, `afs_server.sh` that can be used for start, stop, restart or get status of the server.

### lib : compiled server libraries

### log : server logs
Contains one log file `afs_server.log`, this file contains all relevant log information.

### etc : config files

| File                 | Description                                                                     |
|----------------------|---------------------------------------------------------------------------------|
| `afs_server.conf`    | Java configuration used to start the serve, particularly the amount of RAM used |
| `logging.properties` | Logging configuration, the possible switches are documented on the file         |
| `service.properties` | General configuration                                                           |

### afs_server.conf

By default, the RAM usage is 512MB, makes assumptions for a small instance using only the web UI.

A good number is: httpMaxContentLength * 3 * Web UI uploads + ttpMaxContentLength * 12 * openBIS Drive uploads

For 10 concurrent users doing uploads using the defaults that means: 15728640 * 3 * 10 = 470~ MB If only using the web UI

### logging.properties

```properties
#############################################################
# JUL Log Levels (must use these exact names) and Log4j mapping
#
# OFF     = no logging                      | Log4j: OFF
# SEVERE  = serious failures, system errors | Log4j: ERROR
# WARNING = potential problems, recoverable | Log4j: WARN
# INFO    = general informational messages   | Log4j: INFO
# CONFIG  = static configuration messages    | Log4j: DEBUG
# FINE    = general debugging/tracing        | Log4j: DEBUG
# FINER   = more detailed tracing            | Log4j: TRACE
# FINEST  = most detailed, very verbose      | Log4j: TRACE
# ALL     = enable all logging               | Log4j: ALL
#############################################################

# Global logging level
.global.level=INFO

# List of handlers to configure
.global.handlerAliases = myFileHandler, resolvedPropertiesHandler

# Custom File Handler using your custom class
myFileHandler.class = ch.ethz.sis.shared.log.standard.handlers.DailyRollingFileHandler
myFileHandler.logFileName = log/afs_server.log
myFileHandler.maxLogFileSize = 10485760
myFileHandler.append = true
myFileHandler.level = INFO
myFileHandler.messagePattern = %d{yyy-MM-dd HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n

# Resolved Properties Handler
resolvedPropertiesHandler.class = ch.ethz.sis.shared.log.standard.handlers.SingleFileHandler
resolvedPropertiesHandler.level = INFO
resolvedPropertiesHandler.append = false
resolvedPropertiesHandler.maxLogFileSize = 10048576
resolvedPropertiesHandler.filter = OPERATION.ExposablePropertyPlaceholderConfigurer
resolvedPropertiesHandler.logFileName = log/startup_properties.log
resolvedPropertiesHandler.messagePattern = %d %-5p [%t] %c - %m%n
```

### service.properties (Mandatory)

The service.properties contains around two dozen properties that SHOULD NOT be modified on standard production environments.

Here are ONLY discussed the ones that SHOULD BE manually configured.

| Property                                   | Description                                                                                                                                                                            |
|--------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `writeAheadLogRoot`                        | Defaults to `$DSS_ROOT_DIR/transactions`, this is the write ahead log of the transactional filesystem, SHOULD be on the same filesystem as the final storage for optional performance. |
| `storageRoot`                              | Same used by the DSS. Defaults to `$DSS_ROOT_DIR/store`                                                                                                                                |
| `storageUuid`                              | Same used by the DSS.                                                                                                                                                                  |
| `poolSize`                                 | Defaults to 50, a good number is: Concurrent Uploads/Downloads Web UI + 4 * Concurrent Uploads/Downloads openBIS Drive                                                                 |
| `apiServerInteractiveSessionKey`           | Secret password, to be shared only to those allowed to call BEGIN and PREPARE and COMMIT manually. Used for 2 Phase Transactions                                                       |
| `apiServerTransactionManagerKey`           | Secret password, to be shared only with the transaction manager to run recovery workflows.                                                                                             |
| `openBISUrl`                               | Ideally points directly to the AS in localhost http://localhost:8080                                                                                                                   |
| `openBISTimeout`                           | Defaults to 30000 millis, 30 sec                                                                                                                                                       |
| `openBISUser`                              | Used by the integration plugin, should be an INSTANCE_ADMIN                                                                                                                            |
| `openBISPassword`                          | Password for the openBIS user                                                                                                                                                          |
| `openBISLastSeenDeletionFile`              | Defaults to ./last-seen-deletion                                                                                                                                                       |
| `openBISLastSeenDeletionBatchSize`         | Defaults to 1000                                                                                                                                                                       |
| `openBISLastSeenDeletionIntervalInSeconds` | Defaults to 900 seconds, 15 minutes                                                                                                                                                    |

### service.properties (Optional)

Additionally, these are MANDATORY for ```Archiving``` but RECOMMENDED for EVERYONE, provides a cache for the immutable dataset metadata.

Values are already filled in the template, just uncomment them.

| Property                          | Description                                                                                 |
|-----------------------------------|---------------------------------------------------------------------------------------------|
| `maintenance-plugins`             | Value to be used: pathInfoFeedingTask                                                       |
| `pathInfoFeedingTask.class`       | Value to be used: ch.systemsx.cisd.etlserver.path.PathInfoDatabaseFeedingTask               |
| `pathInfoFeedingTask.interval`    | Value to be used: 3600                                                                      |
| `pathInfoDB.name`                 | Value to be used: pathinfo                                                                  |
| `pathInfoDB.kind`                 | Value to be used: prod                                                                      |
| `pathInfoDB.engine`               | Value to be used: postgresql                                                                |
| `pathInfoDB.version-holder-class` | Value to be used: ch.systemsx.cisd.openbis.dss.generic.shared.PathInfoDatabaseVersionHolder |
| `pathInfoDB.script-folder`        | Value to be used: afs-server/sql/pathinfo                                                   |

Additionally, these are MANDATORY for setups with multiple share ids, this is for backwards compatibility with the way DSS shuffles data.

| Property                       | Description                                                                      |
|--------------------------------|----------------------------------------------------------------------------------|
| `maintenance-plugins`          | Value to be used: pathInfoFeedingTask, shufflingTask                             |
| `shuffling-task.class`         | Value to be used: ch.systemsx.cisd.etlserver.plugins.SegmentedStoreShufflingTask |
| `shuffling-task.interval`      | Value to be used: 3600                                                           |
| `shuffling.class`              | Value to be used: ch.systemsx.cisd.etlserver.plugins.SimpleShuffling             |
| `shuffling.share-finder.class` | Value to be used: ch.systemsx.cisd.openbis.dss.generic.shared.SimpleShareFinder  |
| `shuffling.verify-checksum `   | Value to be used: true                                                           |