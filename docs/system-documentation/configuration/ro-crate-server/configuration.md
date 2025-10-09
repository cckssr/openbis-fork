Server Configuration
====================

### Folder Structure

| File          | Description                                                                                             |
|---------------|---------------------------------------------------------------------------------------------------------|
| `bin`         | Contains one file, `ro-crate.sh` that can be used for start, stop, restart or get status of the server. |
| `quarkus-app` | Compiled server libraries                                                                               |
| `log`         | Contains log, by default one log file `ro_crate.log`.                                                   |
| `etc`         | config files                                                                                            |

### etc config files

There are the following config files

| File                     | Description                                                                                                                                                                                           |
|--------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `application.properties` | Configuration used directly by quarkus. This can be [standard quarkus configuration](https://quarkus.io/guides/all-config). May also hold configuration related to class loading and quarkus logging. |
| `logging.properties`     | Logging configuration, the possible switches are documented on the file.                                                                                                                              |
| `service.properties`     | General configuration for openBIS-related logic                                                                                                                                                       |

#### application.properties

#### logging.properties

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
.global.handlerAliases=myFileHandler, resolvedPropertiesHandler
# Custom File Handler using your custom class
myFileHandler.class=ch.ethz.sis.shared.log.standard.handlers.DailyRollingFileHandler
myFileHandler.logFileName=log/afs_server.log
myFileHandler.maxLogFileSize=10485760
myFileHandler.append=true
myFileHandler.level=INFO
myFileHandler.messagePattern=%d{yyy-MM-dd HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n
# Resolved Properties Handler
resolvedPropertiesHandler.class=ch.ethz.sis.shared.log.standard.handlers.SingleFileHandler
resolvedPropertiesHandler.level=INFO
resolvedPropertiesHandler.append=false
resolvedPropertiesHandler.maxLogFileSize=10048576
resolvedPropertiesHandler.filter=OPERATION.ExposablePropertyPlaceholderConfigurer
resolvedPropertiesHandler.logFileName=log/startup_properties.log
resolvedPropertiesHandler.messagePattern=%d %-5p [%t] %c - %m%n
```

#### service.properties (Mandatory)

The service.properties contains around two dozen properties that SHOULD NOT be modified on standard
production environments.

Here are ONLY discussed the ones that SHOULD BE manually configured.

| Property               | Description                                                                                              |
|------------------------|----------------------------------------------------------------------------------------------------------|
| `httpServerPort`       | Port used by the quarkus instance                                                                        |
| `httpServerTimeout`    | Timeout for this server, e.g. `30s`                                                                      |
| `sessionWorkSpace`     | This path is used to write temporary files used by the server. The subdirectories are based on sessions. |
| `httpMaxContentLength` | For AFS                                                                                                  |
| `maxReadSizeInBytes`   | For AFS                                                                                                  |
| `openBISUrl`           | URL for an openBIS application server instance, e.g. http://localhost:8080                               |
| `openBISTimeout`       | Timeout for openBIS calls                                                                                |

#### application.properties (Optional)

This allows fine-grained control over quarkus settings. Notably,

```
quarkus.class-loading.parent-first-artifacts=stax:stax-api
```

is used to prevent class loading issues with libraries. 
