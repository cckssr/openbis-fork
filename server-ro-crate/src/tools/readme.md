# Async Client Tools for the openBIS RO-Crate APi

The specification for the API is found
under: https://github.com/paulscherrerinstitute/rocrate-api/blob/main/openapi.yaml

There are a number of openBIS parameters added on top.

## Authentication

Use openBIS session tokens or personal access tokens to authenticate.
Set them using the environment variable `OPENBIS_KEY`.

## Export

This is an opinionated export for openBIS to RO-Crate. Most of the flags are set to include a lot of
data and metadata.
Use the following arguments as required:

```
-u, --url URL of the RO-Crate API, e.g. https://openbis-sis-ci-sprint-public.ethz.ch/ro-crate-server
-o, --output Path to write result to
-a, --afs_data Include AFS data in crate
-i, --identifier One or more identifiers for openBIS: these can be openBIS permIds, openBIS identifiers or properties annotated with `https://schema.org/identifier`
-m, --max-calls Optional maximum calls for timing out
```

## Import

This imports an RO-Crate into openBIS.
Use the following arugments as required:

```
-u, --url URL of the RO-Crate API, e.g. https://openbis-sis-ci-sprint-public.ethz.ch/ro-crate-server
-p, --path Path to RO-Crate for importing, this can be a zipped crate ending in .zip or .eln or a manifest in JSON-LD
-m, --max-calls Optional maximum calls for timing out
```