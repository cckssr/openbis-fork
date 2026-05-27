https://ethsis.atlassian.net/browse/BIS-2328

# Why

At the moment, it’s not possible to retrieve the objects by their original ID in the RO-Crate graph.
This could be useful for interoperability and sine we have the information during import, we might
as well use it.

# Possible Semantic Annotations

No common ones found.

# Approaches considered

## New property

Add a new property with code `RO_CRATE_ID`, type VARCHAR.

### Pros

- Easy to find in ELN
- Could be used for export functionality even

### Cons

- Change to schema might be unexpected for users
- More work to implement, matching imported types to our types comes to mind
- Property pollution and chance of collisions with

## Metadata

Add the identifier to the metadata under a key, e.g. `RO_CRATE_ID`.

### Pros

- Way easier to implement.
- Does not pollute regular view of properties, schema etc.
- No chance of collision with other property codes

### Cons

- Harder to index and to query

# Decision

I lean towards a new property because searching is important. 
