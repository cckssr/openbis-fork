Developer Notes
====================

### General

This server allows import ad exporting metadata objects with schemas and ontological annotations
based on a specific RO-Crate profile.

It is built on this
profile: https://github.com/researchobjectschema/ro-crate-interoperability-profile/blob/main/0.2.x/spec.md

There is a openAPI specification for documentation
purposes: https://github.com/paulscherrerinstitute/rocrate-api/blob/main/openapi.yaml
Please note that the contents of the manifest are not deserialized as shown in the openAPI
specification.

### Endpoints

Currently there are three endpoints

#### Export

`<openbis-url>/ro-crate-server/export`

In the header, the following arguments are available

| Header                                           | Values                                                                  | Required | Description                 |
|--------------------------------------------------|-------------------------------------------------------------------------|----------|-----------------------------|
| `Accept`                                         | `application/ld+json` for the manifest, `application/zip` for the crate | Yes      | HTTP Accept header          |
| `Content-Type`                                   | `application/json`                                                      | Yes      | HTTP Content type header    |
| `api-key`                                        | openBIS session token _or_ openBIS personal access token                | Yes      | Token for accessing openBIS |
| `openbis.identifier-annotations`                 | Semantic annotation for identifiers used in                             | No       | Used                        |
| `openbis.with-Levels-below`                      | Standard openBIS config option for export                               | No       | openBIS setting for export  |
| `openbis.with-objects-and-dataSets-parents`      | Standard openBIS config option for export                               | No       | openBIS setting for export  |
| `openbis.with-objects-and-dataSets-other-spaces` | Standard openBIS config option for export                               | No       | openBIS setting for export  |

The body of the request is an array of strings. These strings are identifiers. These can be

- openBIS identifiers, e.g. `/SPACE/PROJECT/ENTRY1`
- openBIS permIDs, e.g. `20250730131201280-261`
- an annotation to specify, defaults to . Properties annotated with this property are queried.
  All of these are searched and the results are written to the RO-Crate

#### Validate

`<openbis-url>/ro-crate-server/validate`

This performs internal validation of the RO-Crate.
This means:

- Checking that mandatory properties are present
- Checking that the values of properties have the correct data types
- Check that referenced objects are present

| Header         | Values                                                                  | Required | Description                 |
|----------------|-------------------------------------------------------------------------|----------|-----------------------------|
| `Accept`       | `application/json`                                                      | Yes      | HTTP Accept header          |
| `Content-Type` | `application/ld+json` for the manifest, `application/zip` for the crate | Yes      | HTTP Content type header    |
| `api-key`      | openBIS session token _or_ openBIS personal access token                | Yes      | Token for accessing openBIS |

Feedback is provided by entities and properties with problems. It
follows `ch.ethz.sis.rocrateserver.openapi.v1.service.response.Validation.ValidationReport`.
Returns a validation report. It includes all issues found as well as all entities found.

#### Import

`<openbis-url>/ro-crate-server/import`

First, it performs the same checks as validate.

| Header                | Values                                                                                                           | Required | Description                 |
|-----------------------|------------------------------------------------------------------------------------------------------------------|----------|-----------------------------|
| `Accept`              | `application/json`                                                                                               | Yes      | HTTP Accept header          |
| `Content-Type`        | `application/ld+json` for the manifest, `application/zip` for the crate                                          | Yes      | HTTP Content type header    |
| `api-key`             | openBIS session token _or_ openBIS personal access token                                                         | Yes      | Token for accessing openBIS |
| `openbis.import-mode` | Based on `ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.options.ImportMode`, defaults to  `UPDATE_IF_EXISTS` | No       |                             |

Afterward, the import is converted into an openBIS excel. This is sent to the application server for
the actual import.
Feedback is a map of identifiers in the crate to identifiers of imported openBIS
objects. `ch.ethz.sis.rocrateserver.openapi.v1.service.delegates.ImportDelegate.OpenBisImportResult`
is the response type. 