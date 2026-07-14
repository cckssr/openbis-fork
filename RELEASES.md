# 7.0.0 - Early Access Release

```
IMPORTANT

We recommend this version for new openBIS users.

Established openBIS users are strongly encouraged to follow the migration guide below.

A General Availability release will be made available later this year.
```

## Server

Installing Prerequisites:
- JDK 21
- Postgres 17

[openbis-installer-7.0.0.tar.gz](https://polybox.ethz.ch/index.php/s/eyKK8nBaALwEYEc)

## Development Libraries
- [openbis-java-api-7.0.0-dependencies-included.tar.gz](https://polybox.ethz.ch/index.php/s/S2EbaDp38pZ2Me2)
- [openbis-java-api-7.0.0.tar.gz](https://polybox.ethz.ch/index.php/s/NmmmkApA3iZtf9T)
- [openbis-javascript-api-7.0.0-esm.tar.gz](https://polybox.ethz.ch/index.php/s/EK2iyJEH7m5WrRe)
- [openbis-python3-api-7.0.0.tar.gz](https://polybox.ethz.ch/index.php/s/caEMneT3EZmaisX)

## Clients
- [openbis-command-line-tool-7.0.0.tar.gz](https://polybox.ethz.ch/index.php/s/7rPWzJCFgPtojtd)
- [openbis-drive-7.0.0.tar.gz](https://polybox.ethz.ch/index.php/s/MHLsm8wboyWmnn2)
- [openbis-drive-7.0.0-win.tar.gz](https://polybox.ethz.ch/index.php/s/PeS2yfGwxNDTcW2)
- [openbis-drive-7.0.0-mac.tar.gz](https://polybox.ethz.ch/index.php/s/m4giC78QQc9HGJH)
- [openbis-drive-7.0.0-linux.tar.gz](https://polybox.ethz.ch/index.php/s/mYTKyYXyYPifrgg)

## Source Code
- [Source code (tar.gz)](https://sissource.ethz.ch/sispub/openbis/-/archive/7.0.0/openbis-7.0.0.tar.gz?ref_type=tags)
- [Source code (tar)](https://sissource.ethz.ch/sispub/openbis/-/archive/7.0.0/openbis-7.0.0.tar?ref_type=tags)

## Deliverables
- [Docker image](https://hub.docker.com/layers/openbis/openbis-app/7.0.0/)
- [VirtualBox appliance](https://polybox.ethz.ch/index.php/s/Wnr236YSLo4P468)

## Useful Links
- [Documentation](https://openbis.readthedocs.io/en/7.x/)
- [Change Log](https://sissource.ethz.ch/sispub/openbis/-/blob/7.x/CHANGELOG.md?ref_type=heads)

## 20.10.x Migration Guide

### Important Upgrade Notice

**DO NOT UPGRADE DIRECTLY BETWEEN MAJOR VERSIONS ON YOUR PRODUCTION SYSTEM**

* Upgrade to the latest available **20.10.x** release before upgrading to **7.x**.
* Test the upgrade on a staging server using a copy of your production database before deploying it to production.

To upgrade to **7.x** is a prerequisite to remove Material entities and File entity types. Otherwise, the openBIS 7.x installation will fail to upgrade the database and terminate with an error.

To prepare for the upgrade:

1. Upgrade to the latest available **20.10.x** release.
2. Open the Core UI (available only in **20.10.x**), navigate to **File Types**, and delete all file entity types.
3. Run the **[MaterialsMigration](https://openbis.readthedocs.io/en/20.10.12-plus/system-documentation/configuration/maintenance-tasks.html#materialsmigration)** maintenance task to convert Materials into Objects.

Do **not** reuse configuration files (such as `service.properties`) from **20.10.x**. openBIS **7.x** introduces new configuration files and additional required configuration sections in existing files. Reusing the old configuration will result in a broken installation.

Instead, perform a fresh **7.x** installation:

1. Configure the Application Server (`service.properties`) to connect to the existing **20.10.x** database.
2. Configure the Data Store Server (`service.properties`) to use the existing data store.
3. Reapply any custom ELN-LIMS and maintenance task configuration by editing or adding the required settings in the new configuration files.
