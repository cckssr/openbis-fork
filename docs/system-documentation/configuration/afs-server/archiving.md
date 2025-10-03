Archiving
=========

**IMPORTANT:** AFS and DSS only consider their own data for the archiving/unarchiving (DSS does NOT archive/unarchive AFS data sets and vice versa). 

**IMPORTANT:** AFS and DSS can share the same path info and multi dataset archiving databases. AFS and AS must share the same messages database.

**IMPORTANT:** Only immutable data can be archived.

AFS provides the same archiving functionality as DSS i.e. both servers share the same plugins and tasks that can be configured to perform the archiving and unarchiving operations. The configuration of these plugins and tasks is almost exactly the same at both servers. The only differences between the two are:

* database configuration : AFS uses the same properties for the database configuration as AS (which are different from the properties that DSS uses)
* queuing mechanisms : AFS uses a new queuing mechanism where all asynchronous tasks are stored in the newly created "messages" database (in DSS such tasks were stored in dedicated files that represented the queues). Consumption of the messages at AFS is configurable. Messages of chosen types can be configured to form a single queue and be consumed one after another (e.g. see AFS service.properties example below with "archivingMessagesConsumerTask" consumer that handles both archiving and unarchiving requests, while "finalizeArchivingMessagesConsumerTask" consumer independently handles archiving finalization requests). Conceptually this is similar to the DSS command queue which also offers such configuration options.

Archiving can be only done on immutable data. All DSS data sets are immutable from the moment they are created (i.e. files and folders inside a data set can never be created, changed or deleted once a DSS dataset is created). The situation with AFS data sets is different. They are created mutable (i.e. files and folders inside an AFS data set can be created, changed and deleted after a data set is created). Making the AFS data immutable is an explicit operation that can be triggered by a user from ELN (samples_all.immutable_data_timestamp or experiments_all.immutable_data_timestamp is set in the database). Once an AFS data set is made immutable it can be archived, but it cannot be made mutable again.

An example of a simple AFS archiving configuration is presented below. The example consists of: AFS service.properties snippet, AS service.properties snippet and share.properties file which has to be put in the root folder of the unarchiving share. This constitutes a simple multi dataset archiver configuration which provides the following flows:

Archiving flow:
* a user in ELN requests archiving for chosen data sets (external_data.archiving_requested flags are set to true in openbis database)
* archiving by request task at AS (ch.systemsx.cisd.openbis.generic.server.task.ArchivingByRequestTask) finds and groups the data sets requested for the archiving and creates archiving messages in the messages database
* AFS consumes the archiving messages and executes the archiver plugin (ch.systemsx.cisd.openbis.dss.generic.server.plugins.standard.archiver.MultiDataSetArchiver)

Unarchiving flow:
* a user in ELN requests unarchiving of a chosen data set (an unarchiving message in the messages database is created)
* AFS consumes the unarchiving message and executes the unarchive logic of the archiver plugin (ch.systemsx.cisd.openbis.dss.generic.server.plugins.standard.archiver.MultiDataSetArchiver)

AFS service.properties:

```properties
#########################
# AFS service.properties
#########################

# NOTE: properties not directly related with the archiving have been skipped for brevity

# path info database (this can be the same database that DSS uses i.e. AFS and DSS can share the path info database)
pathInfoDB.name = pathinfo
pathInfoDB.kind = test
pathInfoDB.engine = postgresql
pathInfoDB.version-holder-class = ch.systemsx.cisd.openbis.dss.generic.shared.PathInfoDatabaseVersionHolder
pathInfoDB.script-folder = afs-server/sql/pathinfo

# messages database (this MUST be the same database that AS uses, otherwise AFS and AS won't be able to communicate via messages)
messagesDB.name = messages
messagesDB.kind = test
messagesDB.engine = postgresql
messagesDB.version-holder-class = ch.ethz.sis.messages.db.MessagesDatabaseVersionHolder
messagesDB.script-folder = afs-server/sql/messages

# archiver database (this can be the same database that DSS uses i.e. AFS and DSS can share the archiver database)
archiverDB.name = multi_dataset_archive
archiverDB.kind = test
archiverDB.engine = postgresql
archiverDB.version-holder-class = ch.systemsx.cisd.openbis.dss.generic.server.plugins.standard.archiver.dataaccess.MultiDataSetArchiverDBVersionHolder
archiverDB.script-folder = afs-server/sql/multi-dataset-archive

# maintenance tasks
maintenance-plugins = pathInfoFeedingTask, archivingMessagesConsumerTask, finalizeArchivingMessagesConsumerTask, commonMessagesConsumerTask

# task that populates the path info database
pathInfoFeedingTask.class = ch.systemsx.cisd.etlserver.path.PathInfoDatabaseFeedingTask
pathInfoFeedingTask.interval = 60 sec
pathInfoFeedingTask.compute-checksum = true

# task that handles archiving and unarchiving messages
archivingMessagesConsumerTask.class = ch.ethz.sis.afsserver.server.messages.MessagesConsumerMaintenanceTask
archivingMessagesConsumerTask.consumerId = Archiving messages
archivingMessagesConsumerTask.handlers = ch.ethz.sis.afsserver.server.archiving.messages.ArchiveDataSetMessageHandler, ch.ethz.sis.afsserver.server.archiving.messages.UnarchiveDataSetMessageHandler
archivingMessagesConsumerTask.interval = 60 sec

# task that handles archiving finalization messages
finalizeArchivingMessagesConsumerTask.class = ch.ethz.sis.afsserver.server.messages.MessagesConsumerMaintenanceTask
finalizeArchivingMessagesConsumerTask.consumerId = Finalize archiving messages
finalizeArchivingMessagesConsumerTask.handlers = ch.ethz.sis.afsserver.server.archiving.messages.FinalizeDataSetArchivingMessageHandler
finalizeArchivingMessagesConsumerTask.interval = 60 sec

# task that handles other messages
commonMessagesConsumerTask.class = ch.ethz.sis.afsserver.server.messages.MessagesConsumerMaintenanceTask
commonMessagesConsumerTask.consumerId = Common messages
commonMessagesConsumerTask.handlers = ch.ethz.sis.afsserver.server.messages.DeleteFileMessageHandler, ch.ethz.sis.afsserver.server.messages.DeleteDataSetFromStoreMessageHandler , ch.ethz.sis.afsserver.server.archiving.messages.UpdateDataSetArchivingStatusMessageHandler
commonMessagesConsumerTask.interval = 60 sec

# multi dataset archiving (exactly the same configuration properties as for DSS)
archiver.class = ch.systemsx.cisd.openbis.dss.generic.server.plugins.standard.archiver.MultiDataSetArchiver
archiver.final-destination = ./test-archive/final-destination
archiver.staging-destination = ./test-archive/staging-destination
archiver.replicated-destination = ./test-archive/replicated-destination
archiver.minimum-container-size-in-bytes = 10
archiver.maximum-container-size-in-bytes = 1000000
archiver.batch-size-in-bytes = 100000
archiver.hdf5-files-in-data-set = false
archiver.with-sharding = false
archiver.wait-for-sanity-check = true
archiver.sanity-check-verify-checksums = true
archiver.check-consistency-between-store-and-pathinfo-db = true
archiver.unarchiving-prepare-command-template =
archiver.unarchiving-wait-for-t-flag = true
archiver.unarchiving-max-waiting-time = 5 min
archiver.unarchiving-polling-time = 10 sec
archiver.finalizer-wait-for-t-flag = true
archiver.finalizer-sanity-check = true
archiver.finalizer-polling-time = 10 sec
archiver.finalizer-max-waiting-time = 5 min
archiver.delay-unarchiving = false
archiver.pause-file = pause-archiving
archiver.pause-file-polling-time = 10 sec
archiver.cleaner.file-path-prefixes-for-async-deletion = ./test-archive/final-destination, ./test-archive/replicated-destination
archiver.cleaner.email-address = afs_cleaner@localhost
archiver.cleaner.email-from-address = afs_cleaner_from@localhost
archiver.cleaner.email-subject = Deletion failure
archiver.cleaner.email-template = The following files could not be deleted:\n${file-list}
archiver.timeout = 10800

# email notification
mail.from = afs_server@localhost
mail.smtp.host = file://afs-server/test-email
mail.smtp.user =
mail.smtp.password =
```

AS service.properties:

```properties
########################
# AS service.properties
########################

# NOTE: properties not directly related with the archiving have been skipped for brevity

maintenance-plugins = archiveByRequest

archiveByRequest.class = ch.systemsx.cisd.openbis.generic.server.task.ArchivingByRequestTask
archiveByRequest.interval = 60 sec
archiveByRequest.minimum-container-size-in-bytes = 10
```

share.properties:

```properties
###################
# share.properties
###################

# NOTE: this file should be put in the main folder of the unarchiving share

unarchiving-scratch-share = true
```