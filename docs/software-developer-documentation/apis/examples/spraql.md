# SPARQL

SPARQL (SPARQL Protocol and RDF Query Language) is a semantic query language for databases capable of storing and querying data in RDF (Resource Description Framework) format.
It is the standard way to query linked data and knowledge graphs, enabling powerful cross-dataset queries based on relationships between entities.

## SPARQL and openBIS

openBIS does not expose a SPARQL endpoint and does not support SPARQL queries directly against its internal database. 
openBIS uses a relational (PostgreSQL) backend with its own query API (the V3 API), which is not RDF-based.

## Querying openBIS Data with SPARQL via RO-Crate

Although SPARQL queries cannot be run directly against openBIS, it is possible to export openBIS entities in **RO-Crate** format,
which is an RDF-compatible packaging standard based on Schema.org and JSON-LD. 
The exported RO-Crate can then be loaded into any RDF triple store (e.g. Apache Jena Fuseki, Blazegraph, GraphDB) and queried with SPARQL.

### Workflows

#### ELN-LIMS UI
1. **Export entities from openBIS as RO-Crate**

   Use the openBIS ELN-LIMS export feature or the RO-Crate API to export the desired entities (experiments, samples, datasets, etc.). The result is a ZIP archive containing a `ro-crate-metadata.json` file encoded as JSON-LD.

2. **Extract the metadata file**

   Unzip the archive and locate `ro-crate-metadata.json`. This file contains all entity metadata as a JSON-LD graph, which is directly importable into RDF tools.

3. **Load into an RDF triple store**

   Import the JSON-LD file into an RDF database. 

4. **Run SPARQL queries**

   Once the data is in the triple store, query it with standard SPARQL. Example — list all samples with their identifiers:

   ```sparql
   PREFIX schema: <https://schema.org/>

   SELECT ?entity ?name ?identifier
   WHERE {
     ?entity a schema:Thing ;
             schema:name ?name ;
             schema:identifier ?identifier .
   }
   ORDER BY ?name
   ```
   
#### PyBIS

PyBIS 7.0.0+ supports basic RO-Crate server communication. With it, it is possible to export and download entities in RO-Crate format.

Example:
*In this example script will download information about OpenBIS space (together with projects, collections and objects) with code 'RDF' in ro-crate format*

```python

from pybis import Openbis
from pybis.ro_crate import RoCrateClient

url = "https://your-openbis-instance"
ro_crate_url = "https://your-openbis-instance:8086/openbis"

# Create OpenBIS client
openbis_instance = Openbis(url=url)
# Create RO-Crate server client 
ro_crate_client = RoCrateClient(ro_crate_url, openbis_instance.token)

# Create export task to export space 'RDF' with everything below it.
jobId = ro_crate_client.export([{"kind": "SPACE", "permId": "RDF"}], zipExport=False, withLevelsBelow=True)

# Export task may take time, depending on amount of entities to process
for i in range(10):
   time.sleep(1)
   # check status of task
   status = ro_crate_client.check_status(jobId)
   if not status.status in ['RUNNING', 'COMPLETED']:
      raise ValueError(status)
   if status.status == 'COMPLETED':
      break

# Download exported entities 
file_path = ro_crate_client.download(jobId, '/path/where/to/store/download', zip=False)

```

Once ro-crate-metadata.json is downloaded, its contents can be imported into a RDF database.
[RDFlib](https://rdflib.readthedocs.io/en/stable/) is a popular library that can handle parsing and quering RDF data. 

Here are examples how to use it to query data exported from OpenBIS.
[This file](ro-crate-metadata.json) has been exported from OpenBIS and used in this example. 
```python

from rdflib import Graph, URIRef

g = Graph()
g.parse(file_path, format="json-ld")

print("All RDF triplets")
results = g.query("SELECT ?s ?p ?o WHERE { ?s ?p ?o }")
for row in results:
   print(f"Subject:   {row.s}")
   print(f"Predicate: {row.p}")
   print(f"Object:    {row.o}")
   print("---")


print("Samples that have experiments with start date after '2026-07'")

results = g.query("""
    SELECT ?sampleName ?experiment ?date
    WHERE {
        ?sample <openBIS:hasCOLLECTION> ?experiment ;
                <openBIS:hasNAME>       ?sampleName .
        ?experiment <openBIS:hasSTART_DATE> ?date .
        FILTER(STR(?date) > "2026-07")
    }
    ORDER BY ?date
""")

for row in results:
   print(f"Sample:     {row.sampleName}")
   print(f"Experiment: {row.experiment}")
   print(f"Start date: {row.date}")
   print("---")


print("Samples from PROJECT1 that have experiments with start date after '2026-07'")

results = g.query("""
    SELECT ?sampleName ?experiment ?date ?project ?space
    WHERE {
        ?sample <openBIS:hasCOLLECTION> ?experiment ;
                <openBIS:hasNAME>       ?sampleName ;
                <openBIS:hasPROJECT>    ?project ;
                <openBIS:hasSPACE>      ?space .
        ?experiment <openBIS:hasSTART_DATE> ?date .
        FILTER(STR(?date) > "2026-07")
        FILTER(?project = ?projectParam)
    }
    ORDER BY ?date
""", initBindings={"projectParam": URIRef("file:///RDF/PROJECT1")})

for row in results:
   print(f"Space:      {row.space}")
   print(f"Project:    {row.project}")
   print(f"Sample:     {row.sampleName}")
   print(f"Experiment: {row.experiment}")
   print(f"Start date: {row.date}")
   print("---")


print("Samples from project having TEST in project description that have experiments with start date after '2026-07'")

results = g.query("""
  SELECT ?sampleName ?experiment ?date ?project ?projectDesc ?space
  WHERE {
      ?sample <openBIS:hasCOLLECTION> ?experiment ;
              <openBIS:hasNAME>       ?sampleName ;
              <openBIS:hasPROJECT>    ?project ;
              <openBIS:hasSPACE>      ?space .
      ?project <http://schema.org/description> ?projectDesc .
      ?experiment <openBIS:hasSTART_DATE> ?date .
      FILTER(STR(?date) > "2026-07")
      FILTER(CONTAINS(STR(?projectDesc), "TEST"))
  }
  ORDER BY ?date
""")

for row in results:
   print(f"Space:       {row.space}")
   print(f"Project:     {row.project}")
   print(f"Description: {row.projectDesc}")
   print(f"Sample:      {row.sampleName}")
   print(f"Experiment:  {row.experiment}")
   print(f"Start date:  {row.date}")
   print("---")

```



### Notes

- The RO-Crate JSON-LD context maps openBIS entity properties to Schema.org terms. Consult the `ro-crate-metadata.json` `@context` block to understand which predicates are in use.
- Relationships between entities (e.g. sample → experiment, dataset → sample) are preserved as RDF links, making them queryable with graph traversal patterns in SPARQL.
- For large exports, prefer a persistent triple store over in-memory solutions to avoid memory constraints.

## Further Reading

- [RO-Crate specification](https://www.researchobject.org/ro-crate/)
- [SPARQL 1.1 Query Language (W3C)](https://www.w3.org/TR/sparql11-query/)
- openBIS RO-Crate export documentation