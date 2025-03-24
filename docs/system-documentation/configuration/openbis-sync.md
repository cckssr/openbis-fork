openBIS Sync
============

## Introduction

Sync is a service of openBIS and comes with every instance.
Sync allows to synchronize two openBIS instances using the OAI-PMH protocol.

This protocol has two participants:
- One instance (called `Data Source`) provides the data (types, meta-data and data sets).
- Another instance (called `Harvester`) grabs these data and makes them available.

In regular time intervals, the `Harvester` instance will synchronize its data with the data on the `Data Source` instance.
Synchronization will add and/or delete data to the `Harvester` instance.

An openBIS instance can be one or both `Data Source` and `Harvester` since these are separate services.
- An openBIS instance only needs one `Data Source` service. Even with many participants since it decides what to share depending on the user requesting the information.
- An openBIS instance only needs one `Harvester` service. Even with many participants since it goes through a list of `Data Source`.

## Data Source Service Configuration

The `Data Source` instance provides a service based on the ResourceSync Framework Specification (see <http://www.openarchives.org/rs/1.1/resourcesync>). 

This service is configured by default in all new installations as a core plugin as [core plugin](../../software-developer-documentation/server-side-extensions/core-plugins.md#core-plugins) module `openbis-sync`.

So in theory all openBIS instances are by default a `Data Source`. This should not worry admins, since users cannot access any data though the `Data Source` endpoint that they could not already with the UI or standard API.

As a `Data Source`  is key to learn to configure the module `openbis-sync`, this module has two config files:

```bash
# Main Configuration File
./core-plugins/openbis-sync/2/dss/servlet-services/resource-sync/plugin.properties 
# Configuration file providing an AS datasource to the DSS
./core-plugins/openbis-sync/2/dss/data-sources/openbis-db/plugin.properties
```

Is strongly encouraged that any overrides to the values of any configuration file should be done forwarding 
such properties to AS service.properties keys.

The main configuration file defines the URL where the `Data Source` will be available.
When using the default settings for the openBIS installation is not necessary to modify it.

Special attention to ```request-handler.file-service-repository-path``` that should point to the `file-server`.

**plugin.properties**
```properties
# ./core-plugins/openbis-sync/2/dss/servlet-services/resource-sync/plugin.properties 
class = ch.systemsx.cisd.openbis.dss.generic.server.oaipmh.OaipmhServlet
path = ${openbis-sync.servlet-services.resource-sync.path:/datastore_server/re-sync/*}
request-handler = ${openbis-sync.servlet-services.resource-sync.request-handler:ch.ethz.sis.openbis.generic.server.dss.plugins.sync.datasource.DataSourceRequestHandler}
request-handler.server-url = ${server-url}/openbis 
request-handler.download-url = ${download-url}
request-handler.file-service-repository-path = ${openbis-sync.servlet-services.resource-sync.request-handler.file-service-repository-path:../../data/file-server}
authentication-handler = ${openbis-sync.servlet-services.resource-sync.authentication-handler:ch.systemsx.cisd.openbis.dss.generic.server.oaipmh.BasicHttpAuthenticationHandler}
```

The second configuration file is just there to create an AS database data source. 

Special attention to ```databaseKind```, if is not the default `prod`.

**plugin.properties**
```properties
# ./core-plugins/openbis-sync/2/dss/data-sources/openbis-db/plugin.properties
#
# Data source used to determine which entities have been deleted
#
databaseEngineCode = ${openbis-sync.data-sources.openbis-db.databaseEngineCode:postgresql}
basicDatabaseName = ${openbis-sync.data-sources.openbis-db.basicDatabaseName:openbis}
# This needs to match the databaseKind in the openBIS service.properties
databaseKind = ${database.kind:prod}
```

In practice by default the service should be available at `<DSS base URL>/datastore_server/re-sync`.
Please test this is available in your instance by opening such URL in your browser, using one of the 3 verbs available: 
- `https://openbis-sis-ci-sprint.ethz.ch/datastore_server/re-sync/?verb=about.xml`
- `https://openbis-sis-ci-sprint.ethz.ch/datastore_server/re-sync/?verb=capabilitylist.xml`
- `https://openbis-sis-ci-sprint.ethz.ch:443/datastore_server/re-sync/?verb=resourcelist.xml`

You will be asked with the openBIS user and password you want to access the service and the service will return an XML file.

## Use case: One Datasource - One or more Harvester

The key is the fact that the XML file contains only the information visible for the selected user.

It is recommended to create a ```user``` on the `Data Source` that can only see the set of data needed for a `Harvester`.

This way is possible to shared different sets of data, using different ```users```, for example:
- sync-datasource-user-inventory: This user is OBSERVER of all inventory SPACE.
- sync-datasource-user-project-a: This user is OBSERVER of project A.

Is recommended that technical users are created on the file authentication system of the instance and their 
rights configured on  the admin UI.

## Data Source Service Document

By default, the URL of the service is `<DSS base URL>/datastore_server/re-sync` and supports the verbs mentioned previously. 

For example for `https://openbis-sis-ci-sprint.ethz.ch/datastore_server/re-sync/?verb=about.xml` we get:

```xml
<urlset xsi:schemaLocation="https://sis.id.ethz.ch/software/#openbis/xdterms/ ./xml/xdterms.xsd https://sis.id.ethz.ch/software/#openbis/xmdterms/">
<rs:ln href="https://openbis-sis-ci-sprint.ethz.ch:443/datastore_server/re-sync/?verb=about.xml" rel="describedby"/>
<rs:md capability="description"/>
<url>
<loc>
https://openbis-sis-ci-sprint.ethz.ch:443/datastore_server/re-sync/?verb=capabilitylist.xml
</loc>
<rs:md capability="capabilitylist"/>
</url>
</urlset>
```

From capabilities described in the ResourceSync Framework Specification only `resourcelist` is supported. 
The resourcelist returns an XML with all metadata of the data source openBIS instance. 
This includes master data, meta data including file meta data.

Two optional URL parameters filter the data by spaces:

-   `black_list`: comma-separated list of regular expressions. All
    entities which belong to a space which matches one of the regular
    expressions of this list will be suppressed.
-   `white_list`: comma-separated list of regular expressions. If
    defined only entities which belong to a space which matches one of
    the regular expressions of this list will be delivered (if not
    suppressed by the black list).

Remarks:

-   Basic HTTP authentication is used for authentication.
-   The resourcelist capability returns only data visible for the user
    which did the authentication.

## Harvester Service Configuration

This service needs to be configured in a case by case basis.
A `Harvester` can sync with one or more `Data Source` openBIS instances.
For that a `Harvester` [maintenance task](./maintenance-tasks.md#maintenance-tasks) has to be configured 
on the `Harvester` openBIS instance. 

The `Harvester` is a DSS maintenance task. An example config file follows:

**plugin.properties**

```properties
class = ch.ethz.sis.openbis.generic.server.dss.plugins.sync.harvester.HarvesterMaintenanceTask
interval = 1 d
harvester-config-file = ../../data/harvester-config.txt
```

The only specific property of `HarvesterMaintenanceTask` is
`harvester-config-file` which is absolute or relative path to the actual
configuration file. This separation in two configuration files has been
done because `plugin.properties` is only read once (at start up of DSS).
Thus changes in Harvester configuration would be possible without
restarting DSS.

Here is an example of a typical configuration:


**harvester-config.txt**

```properties
[LAB1]

resource-list-url = https://<data source host>:<DSS port>/datastore_server/re-sync

data-source-openbis-url = https://<data source host>:<AS port>/openbis/openbis
data-source-dss-url = https://<data source host>:<DSS port>/datastore_server
data-source-auth-realm = OAI-PMH
data-source-auth-user = <data source user id>
data-source-auth-pass = <data source password>
space-black-list = SYSTEM
space-white-list = LAB1.*

harvester-user = <harvester user id>
harvester-pass = <harvester user password>

keep-original-timestamps-and-users = false
harvester-tmp-dir = temp
last-sync-timestamp-file = ../../data/last-sync-timestamp-file_HRVSTR.txt
log-file = log/synchronization.log

email-addresses = <e-mail 1>, <e-mail 2>, ...

translate-using-data-source-alias = true
verbose = true
#dry-run = true
```

-   The configuration file can have one or many section for each openBIS
    instance. Each section start with an arbitrary name in square
    brackets. This name becomes the alias of the instance. 
    Warning: This alias SHOULD only contain letters and numbers. 
    Other characters MIGHT look like the work but can lead to errors when parsing the prefix.
-   `<data source host>`, `<DSS port>` and `<AS port>` have to be host
    name and ports of the Data Source openBIS instance as seen by the
    Harvester instance.
-   `<data source user id>` and `<data source password>` are the
    credential to access the Data Source openBIS instance. Only data
    seen by this user is harvested.
-   `space-black-list` and `space-white-list` have the same meaning
    as `black_list` and `white_list` as specified above in the Data
    Source section.
-   `<harvester user id>` and `<harvester user password>` are the
    credential to access the Harvester openBIS instance. It has to be a
    user with instance admin rights.
-   `Temporary `files created during harvesting are stored
    in` harvester-tmp-dir` which is a path relative to the root of the
    data store. The root store is specified by `storeroot-dir` in
    DSS `service.properties`. The default value is `temp`.
-   By default the original timestamps (registration timestamps and
    modification timestamps) and users (registrator and modifier) are
    synchronized. If necessary users will be created. With the
    configuration property  `keep-original-timestamps-and-users = false`
    no timestamps and users will be synchronized. 
-   The `last-sync-timestamp-file` is a relative or absolute path to the
    file which store the last timestamp of synchronization.
-   The `log-file` is a relative or absolute path to the file where
    synchronization information is logged. This information does not
    appear in the standard DSS log file.
-   In case of an error an e-mail is sent to the specified e-mail
    addresses.
-   `translate-using-data-source-alias` is a flag which controls whether
    the code of spaces, types and materials should have a prefix or not.
    If true the prefix will be the name in the square bracket followed
    by an underscore. The default value of this flag is false.
-   `verbose` flag adds to the synchronization log added, updated and
    deleted items. Default: `false` or `true` if `dry-run` flag is set.
-   `dry-run` flag allows to run without changing Harvester openBIS
    instance. This allows to check config errors or errors with the Data
    Source openBIS instance. A dry run will be performed even if this
    flag is set. Default: `false`
-   `master-data-update-allowed` flag allows to update master data as
    plugins, property types, entity types and entity assignments. Note,
    that master data can still be added if this flag is `false`.
    Default: `false`
-   `property-unassignment-allowed` flag allows to unassign property
    assignments, that is, removing property types from entity types.
    Default: `false`
-   `deletion-allowed` flag allows deletion of entities on the Harvester
    openBIS instance. Default: `false`
-   `keep-original-timestamps-and-users` flag yields that time stamps
    and users are copied from the Data Source to the Harvester.
    Otherwise the entities will have harvester user and the actual
    registration time stamp. Default: `true`
-   `keep-original-frozen-flags` flag yields that the frozen flags are
    copied from the Data Source to the Harvester. Otherwise entities
    which are frozen on the Data Source are not frozen on the Harvester.
    Default: `true`

This DSS service access the main openBIS database directly in order to
synchronize timestamps and users. If the name of this database isn't
{{openbis\_prod}} the property `database.kind` in DSS service.properties
should be defined with the same value as the same property in AS service.properties.

### What HarvesterMaintenanceTask does

In the first step it reads the configuration file from the file path
specified by `harvester-config-file` in `plugins.properties`. Next, the
following steps will be performed in DRY RUN mode. That is, all data are
read, parsed and checked but nothing is changed on the Harvester. If no
error occured and the `dry-run` flag isn't set the same steps are
performed but this time the data is changed (i.e. synced) on the
Harvester.

1.  Read meta data from the Data Source.
2.  Delete entities from the Harvester which are no longer on the Data
    Source (if `deletion-allowed` flag is set).
3.  Register/update master data.
4.  Register/update spaces, projects, experiments, samples and
    materials.
5.  Register/update attachments.
6.  Synchronize files from the file service.
7.  Register/update data sets.
8.  Update timestamps and users (if `keep-original-timestamps-and-users`
    flag is set).
9.  Update frozen flags (if `keep-original-frozen-flags` flag is set).

-   Data are registered if they do not exists on the Harvester.
    Otherwise they are updated if the Data Source version has a
    modification timestamp which is after the last time the
    HarvesterMaintenanceTask has been performed
-   If `translate-using-data-source-alias` flag is set a prefix is added
    to spaces, types and materials when created. 
-   To find out if an entity already exist on the Harvester the perm ID
    is used.

### Master Data Synchronization Rules

Normally all master data are registered/updated if they do not exists or
they are older. But for internal vocabularies and property types
different rules apply. Internal means that the entity (i.e. a vocabulary
or a property type) is managed internally (visible by the prefix '$' in
its code) and has been registered by the system user.

1.  Internal vocabularies and property types will not be created or
    updated on the Harvester.
2.  An internal vocabulary or property type of the Data Source which
    doesn't exist on the Harvester leads to an error.
3.  An internal property type which exists on the Data Source and the
    Harvester but have different data type leads to an error.
4.  Terms of an internal vocabulary are added if they do not exists on
    the Harvester.
