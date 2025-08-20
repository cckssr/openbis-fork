# Pending 20.10 Configuration Changes

## Version 20.10.12

**Pre-Upgrade Checklist**

We’ve resolved an issue that, in rare cases, could allow cycles to be created. To ensure a smooth upgrade, we recommend running the tool below beforehand.
- If the tool doesn’t report anything, you’re all set.
- If it does find something, please address it before upgrading.

This step is important because openBIS 20.10.12 may not start properly if cycles remain in the database after the upgrade.

Run the tool from the command line:
```
java -jar app-openbis-cycle-finder.jar
```

Download it here: [app-openbis-cycle-finder.tar.xz](/uploads/b88f0326c2baf661944a7ad9c7837627/app-openbis-cycle-finder.tar.xz)

## Version 20.10.10

#### 1. Changes to Datastore logs configuration

Datastore server will no longer create a separate log file everytime it starts up.

In order to revert this change, uncomment `rotateLogFiles` call from `<INSTALLATION_DIR>/servers/datastore_server/datastore_server.sh` (line 184)

```shell
echo -n "Starting Data Store Server "
# rotateLogFiles $LOGFILE $MAXLOGS  # <- uncomment this line to bring back log rotation
```

## Version 20.10.9

#### 1. Changes to ELN LIMS Dropbox, new configuration keys for DSS service.properties.
The ELN-LIMS dropbox validation has been overhauled so that it can happen in two widely configurable steps. 
- First, files are allowed to be removed; this is aimed at removing files that are knowingly considered unnecessary. 
- Secondly, the remains were validated for acceptability. This should allow administrators to choose the level of strictness in every particular environment. 

The current default configuration matches previous behavior.

##### Configuration:

- `eln-lims-dropbox-discard-files-patterns`

Allows to specify comma-separated regular expressions of filenames that will be discarded by dropbox script during dataset creation.  

Default setting: `eln-lims-dropbox-discard-files-patterns=` (no files are discarded)


- `eln-lims-dropbox-illegal-files-patterns`

Allows to specify comma-separated regular expressions of filenames that will be considered as illegal by the dropbox script. Their presence in the dropbox directory will automatically abort the dataset creation.

Default setting: `eln-lims-dropbox-illegal-files-patterns=desktop\\.ini, IconCache\\.db, thumbs\\.db, \\..*, .*'.*, .*~.*, .*\\$.*, .*%.*`

**Note: regular expressions in this configuration are [Java patterns](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html) and special characters need to be escaped**

#### 2. Configuration of download-url for Application Server service.properties
The ELN-LIMS "Print PDF" functionality has been reworked and to produce proper PDFs a `download-url` parameter (which is a base URL for Web client access to the application server) in AS service.properties needs to be set. 
When the machine is behind a reverse proxy, it needs to match the actual domain of the server

`download-url` - Base URL. Contains protocol, domain, and port. (e.g https://localhost:8443)

## Version 20.10.6
#### 1. Changes on ELN LIMS Dropbox, new configuration key for DSS service.properties. This change is OPTIONAL.

`mail.addresses.dropbox-errors`

Allows you to set a list of mails to get notified of registration errors.



#### 2. Changes to User Management Task, new configuration key for the configuration file. This change is OPTIONAL.

`List<String> instanceAdmins`

Allows you to set instance admins on the global configuration and they will be created.



#### 3. Technology Upgrade: Postgres 15. This change is OPTIONAL.

openBIS can now run using Postgres 15. Upgrades of Postgres should be taken carefully and with proper backup procedures in place.

It is required to upgrade the database directory format manually. Please check the [official documentation](https://www.postgresql.org/docs/15/upgrading.html)

## Version 20.10.3
After migrating to 20.10.3 please make sure you don't have in ELN settings properties that have BOTH custom widget set to "Word Processor" and at the same time they are listed in "Forced Disable RTF" section. If your ELN instance happens to have such properties please remove them from both "Custom Widgets" as well as "Forced Disable RTF" sections. This way they will behave as normal text fields. In case you would like to allow styling/formatting of your property values (rich text), please make sure the custom widget is set to "Word Processor" and the property is not listed in "Force Disable RTF" section. 
 
In openBIS version 20.10.3 a mechanism for displaying property values in ELN has been changed. Starting from that version any HTML tags contained in the property values won't be interpreted by a web browser anymore. Instead, such HTML tags will be displayed as normal text. The only exception to that rule are properties that have custom widget set to "Word Processor" in the ELN settings. HTML tags in such properties will be still recognized, interpreted and rendered by a web browser. For instance, `value = "<b>text</b>"` will be rendered as "text" in bold font. 

In some cases, due to this change in the ELN property rendering mechanism, after the upgrade to 20.10.3 properties may contain unwanted HTML tags when displayed in ELN. What was rendered as "value" in older versions of ELN, may after the upgrade to 20.10.3 become "<?xml version="1.0" encoding="UTF-8"?><html><head></head><body>value</body></html>". If that is the case with your installation, then you can clean such problematic property values using a Java tool created for that purpose.

The tool can be downloaded here. The tools requires Java 11 or newer. It connects with a chosen openBIS server and fetches/updates the data using openBIS V3 API.

**IMPORTANT:** Please note that it is normal for rich text property values (i.e. properties with custom widget set to "Word Processor") to contain HTML tags. HTML is used for styling/formatting of such property values. The tool should not normally be used for such properties.

Tool usage:

1) Show statistics about property values that contain HTML tags (by default it searches for property values that start with: "<?xml version="1.0" encoding="UTF-8"?>, contain: "<html><head></head><body>" and end with: "</body></html>")

`java -jar openbis-html-properties-cleaner.jar stats --openbis-url=https://openbis-xxx.com`

2) List property values that contain HTML tags for a given entity kind and property type

`java -jar openbis-html-properties-cleaner.jar list --openbis-url=https://openbis-xxx.com --entity-kind=SAMPLE --property-type=AUTHORS`

3) Fix property values that contain HTML tags for a given entity kind and property type

`java -jar openbis-html-properties-cleaner.jar fix --openbis-url=https://openbis-xxx.com--entity-kind=SAMPLE --property-type=AUTHORS`

## Version 20.10.2 GA (General Availability)
The default value of "project-samples-enabled" paramenter in service.properties of the openbis project has been changed to "true". This property controls whether it is possible to have samples related to a project directly.
## Version 20.10.1 EA (Early Access)
- **WARNING:** Downgrade from 20.10.1 EA to 20.10.0 RC is NOT possible (20.10.1 EA version includes database schema changes)  
- The default value of `entity-history.enabled` property has been changed to 'true'. The property controls gathering information about deleted entities in 'events' table (for more details please check: Installation and Administrator Guide of the openBIS Server [here](../configuration/optional-application-server-configuration.md#deleted-entity-history) ).  
- Property collect-statistics is added with "true" as the default value. When the value is "true" (and not overridden by the environment variable) openBIS sends usage statistics to a dedicated server on startup as well as on the 1st day of the month.
- In the `service.properties` file the property collect-statistics is added with "true" as the default value. When the value is "true" (and not overridden by the environment variable) openBIS sends usage statistics to a dedicated server on startup as well as on the 1st day of the month. Usage statistics include only openBIS version, country and number of users.
- `DISABLE_OPENBIS_STATISTICS` environment variable, when set to "true" the statistics is not collected regardless of the value of the property collect-statistics.
## Release 20.10.0 RC
This release includes migration to a new version of Postgres, as well as other technology upgrades that reduce the amount of flags and configuration keys needed (configuration files included on new installations don't have them).

Before you upgrade, you need to take care of:



### Technology Upgrade: Postgres 11

If the installer was used: It is required to upgrade the database directory format manually. Please check the [official documentation](https://www.postgresql.org/docs/11/upgrading.html)

If the docker image was used:

1. Log in interactively into your 19.06.5 Docker container and make binary database dumps with the same name of the databases openbis_prod and pathinfo_prod into the ROOT of your mounted state directory.
2. Stop your 19.06.5 Docker container.
3. Rename postgresql_data into the state directory to postgresql_data_19_6 since it is not compatible with Postgres 11. It is not recommended to delete it until you are sure you are not going to need it.
4. Start your 20.10.0 Docker container, it should detect the presence of openbis_prod and  pathinfo_prod, automatically populating the database from them.
5. When the process is finish test that the system looks like you expect.
6. Feel free to Delete or move somewhere else the openbis_prod and  pathinfo_prod backups so the upgrade procedure is not repeated.


### Technology Upgrade: Java 11

Upgrading to Java 11 forces to remove some old flags on the next files, Java 11 will fail to start with them:

- openbis.conf : remove -d64
- datastore_server.conf : remove -d64


### Technology Upgrade: Search Engine

Now configuration keys related with the old one are ignored, the next configuration keys will be ignored and can be removed from AS service.properties:

```
hibernate.search.index-base
hibernate.search.index-mode
hibernate.search.batch-size
hibernate.search.maxResults
hibernate.search.worker.execution
hibernate.batch.sessionCache.maxEntities
```
