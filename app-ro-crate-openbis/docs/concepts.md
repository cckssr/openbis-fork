# Overview

| openBIS concept        | RO-Crate concept                                                                                       |
|------------------------|--------------------------------------------------------------------------------------------------------|
| Object Type            | `rdfs:Type` inheriting from `openBIS` object                                                           |
| Property Type          | `rdf:Property`, linking types (domain) to their possible values (range)                                |
| Multivalued/Mandatory  | Cardinality constraint _on_ type _referencing_ property type                                           |
| Object/Collection File | RO-Crate file, referenced by `schema:hasPart`                                                          |
| Space                  | Entity of type `openBIS:Space`                                                                         |
| Project                | Entity of type `openBIS:Project`                                                                       |
| Collection             | Entity of type `openBIS:Collection`                                                                    |
| Object                 | Entity that is defined as object type                                                                  |
| Dataset                | N/A                                                                                                    |
| Rich Text Image        | File that is referenced in one of the rich text properties, this is separate from the data directories |
| parent/child           | [TBD](https://en.wikipedia.org/wiki/Douglas_TBD_Devastator)                                            |
| File                   | `File` entities, referenced via `schema:hasPart`                                                       |


# Space

These are nothing special in the crate.

# Project

These are also straightforward, nothing huge in the crate.

# Collection

Collections may have files, see Files.

# OpenBIS Dataset

Currently not represented, please attach files to Collections or Objects


# Object

This consists of two types:
- the base `openBIS:object`
- the concrete type, e.g. `openBIS:Publication`
  The concrete type inherits from the base type. This allows us to mention that a property can have any openBIS objects as value, e.g. for files.

Objects may have files, see Files.

# Files

The files themselves are linked using the standard RO-Crate `File` type.
For export from openBIS to RO-Crate, the paths remain the same, e.g. `/SPACE/PROJECT/SAMPLE1/data/foo.csv`.  
Files are added using the  `schema:hasPart` property, this is also included in the system schema.


# Rich Text and Images

While the discussion on rich text has not been resolved: https://ethsis.atlassian.net/browse/BIS-2470, we guess whether something's rich text.

Images are extracted, kept separate in the openBisModel and the paths are updated to a format usable by openBIS.


# openBIS -> RO-Crate


This is a pretty straightforward mapping of our concepts.


# RO-Crate -> openBIS

This is the more complicated one.
Before the actual conversion, the RO-Crate is searched for entities that require downloads.
After that, the conversion starts.
An initial step is converting
1. converting object types
2. converting property types
3. finding intersection types, e.g. things that are `SciCat_PublishedData` and `Collection` at the same time. This creates new types again.

Afterward, the various types of references are resolved. All object references need to be mapped
from RO-Crate identifiers to openBIS ones. The really complicated stuff is mapping back the openBIS
constructs like projects, spaces, collections. 
