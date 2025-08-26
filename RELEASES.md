# Mockup release 7.0

## Installer

Installing Prerequisites:
- JDK 21
- Postgres 17

[openbis-installer-7.0.tar.gz]()

## Release notes

**Pre-Upgrade Checklist (Example from 20.10.12)**

We’ve resolved an issue that, in rare cases, could allow cycles to be created. To ensure a smooth upgrade, we recommend running the tool below beforehand.
- If the tool doesn’t report anything, you’re all set.
- If it does find something, please address it before upgrading.

This step is important because openBIS 20.10.12 may not start properly if cycles remain in the database after the upgrade.

Run the tool from the command line:
```
java -jar app-openbis-cycle-finder.jar
```

Download it here: [app-openbis-cycle-finder.tar.xz]()

## Source Code
- [Source code (tar.gz)]()
- [Source code (tar)]()

## Development Libraries
- [openbis-java-api-7.0-dependencies-included.tar.gz]()
- [openbis-java-api-7.0.tar.gz]()
- [openbis-javascript-api-7.0-esm.tar.gz]()
- [openbis-python3-api-7.0.tar.gz]()

## Clients
- [openbis-command-line-tool-7.0.tar.gz]()
- [openbis-drive-7.0.tar.gz]()

## Useful Links
- [Documentation]()
- [Change Log]()