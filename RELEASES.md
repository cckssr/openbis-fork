# Production release 20.10.12

## Installer

[openbis-installer-20.10.12.tar.gz](https://polybox.ethz.ch/index.php/s/jWL7QJrcdZPosdj/download)

## Release notes

**Pre-Upgrade Checklist**

We’ve resolved an issue that, in rare cases, could allow cycles to be created. To ensure a smooth upgrade, we recommend running the tool below beforehand.
- If the tool doesn’t report anything, you’re all set.
- If it does find something, please address it before upgrading.

This step is important because openBIS 20.10.12 may not start properly if cycles remain in the database after the upgrade.

Run the tool from the command line:
```
java -jar app-openbis-cycle-finder.jar
```

Download it here: [app-openbis-cycle-finder.tar.xz](https://sissource.ethz.ch/-/project/118/uploads/b88f0326c2baf661944a7ad9c7837627/app-openbis-cycle-finder.tar.xz)

## Source Code
- [Source code (tar.gz)](https://sissource.ethz.ch/sispub/openbis/-/archive/20.10.12/openbis-20.10.12.tar.gz)
- [Source code (tar)](https://sissource.ethz.ch/sispub/openbis/-/archive/20.10.12/openbis-20.10.12.tar)

## Development Libraries
- [openbis-java-api-20.10.12-dependencies-included.tar.gz]()
- [openbis-java-api-20.10.12.tar.gz]()
- [openbis-python3-api-20.10.12.tar.gz]()
- [openbis-javascript-api-20.10.12-esm.tar.gz]()

## Clients
- [openbis-command-line-tool-20.10.12.tar.gz]()
- [openbis-drive-20.10.12.tar.gz]()

## Useful Links
- [Documentation](https://openbis.readthedocs.io/en/20.10.12-plus/)
- [Change Log](https://sissource.ethz.ch/sispub/openbis/-/blob/20.10.x/CHANGELOG.md?ref_type=heads)