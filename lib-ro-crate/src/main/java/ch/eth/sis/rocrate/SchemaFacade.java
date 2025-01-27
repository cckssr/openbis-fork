package ch.eth.sis.rocrate;

import ch.eth.sis.rocrate.facade.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.entities.data.DataEntity;

import java.io.Serializable;
import java.util.*;

public class SchemaFacade implements ISchemaFacade
{
    private final static String RDFS_CLASS = "rdfs:Class";

    private final static String RDFS_PROPERTY = "rdfs:Property";

    private Map<String, IRdfsClass> rdfsClasses;

    private Map<String, IRdfsProperty> rdfsProperties;

    private Map<String, IMetadataEntry> metadataEntries;

    private final RoCrate crate;

    public SchemaFacade(RoCrate crate)
    {
        this.crate = crate;
        this.rdfsClasses = new LinkedHashMap<>();
        this.rdfsProperties = new LinkedHashMap<>();
        this.metadataEntries = new LinkedHashMap<>();
    }

    public static SchemaFacade of(RoCrate crate) throws JsonProcessingException
    {
        SchemaFacade schemaFacade = new SchemaFacade(crate);
        schemaFacade.parseEntities();
        return schemaFacade;

    }

    @Override
    public void addRdfsClass(IRdfsClass rdfsClass)
    {

        DataEntity.DataEntityBuilder builder = new DataEntity.DataEntityBuilder();
        builder.addProperty("@id", rdfsClass.getId());
        builder.addProperty("@type", RDFS_CLASS);
        rdfsClass.getSuperClasses().forEach(x -> builder.addIdProperty("rdfs:subClassOf", x));
        this.rdfsClasses.put(rdfsClass.getId(), rdfsClass);
        DataEntity entity = builder.build();
        entity.addIdListProperties("owl:equivalentConcept", rdfsClass.getOntologicalAnnotations());
        crate.addDataEntity(entity);

    }

    @Override
    public List<IRdfsClass> getRdfsClasses()
    {
        return this.rdfsClasses.values().stream().toList();
    }

    @Override
    public IRdfsClass getRdfsClass(String id)
    {
        return this.rdfsClasses.get(id);
    }

    @Override
    public void addRfsProperty(IRdfsProperty rdfsProperty)
    {
        DataEntity.DataEntityBuilder builder = new DataEntity.DataEntityBuilder();

        builder.setId(rdfsProperty.getId());
        builder.addProperty("@type", RDFS_PROPERTY);

        var stuff = builder.build();
        stuff.addIdListProperties("schema:rangeIncludes",
                rdfsProperty.getRange());
        stuff.addIdListProperties("schema:domainIncludes",
                rdfsProperty.getDomain());
        stuff.addIdListProperties("owl:equivalentConcept",
                rdfsProperty.getOntologicalAnnotations());
        crate.addDataEntity(stuff);
        rdfsProperties.put(rdfsProperty.getId(), rdfsProperty);

    }

    @Override
    public List<IRdfsProperty> getRdfsProperties()
    {
        return rdfsProperties.values().stream().toList();
    }

    @Override
    public IRdfsProperty getRdfsProperty(String id)
    {
        return rdfsProperties.get(id);
    }

    @Override
    public void addEntry(IMetadataEntry metaDataEntry)
    {
        DataEntity.DataEntityBuilder builder = new DataEntity.DataEntityBuilder();
        builder.setId(metaDataEntry.getId());
        builder.addProperty("@type", metaDataEntry.getClassId());
        ObjectMapper objectMapper = new ObjectMapper();

        metaDataEntry.getValues().forEach((s, o) -> {
            if (o instanceof Double)
            {
                builder.addProperty(s, (Double) o);
            } else if (o instanceof Integer)
            {
                builder.addProperty(s, (Integer) o);
            } else if (o instanceof Boolean)
            {
                builder.addProperty(s, (Boolean) o);
            } else if (o instanceof String)
            {
                builder.addProperty(s, o.toString());
            } else if (o == null)
            {
                builder.addProperty(s, objectMapper.nullNode());
            }
        });
        DataEntity dataEntity = builder.build();
        metaDataEntry.getReferences().forEach(dataEntity::addIdListProperties);

        crate.addDataEntity(dataEntity);

    }

    @Override
    public IMetadataEntry getEntry(String id)
    {
        return metadataEntries.get(id);
    }

    @Override
    public List<IMetadataEntry> getEntries(String rdfsClassId)
    {
        return metadataEntries.values().stream().toList();
    }

    private void parseEntities() throws JsonProcessingException
    {
        Map<String, IRdfsProperty> properties = new LinkedHashMap<>();
        Map<String, IRdfsClass> classes = new LinkedHashMap<>();
        Map<String, IMetadataEntry> entries = new LinkedHashMap<>();

        for (DataEntity entity : crate.getAllDataEntities())
        {
            String type = entity
                    .getProperty("@type").asText();
            String id =
                    entity.getProperty("@id")
                            .asText();

            switch (type)
            {
                case "rdfs:Class" ->
                {
                    RdfsClass rdfsClass = new RdfsClass();
                    rdfsClass.setSubClassOf(parseMultiValued(entity, "rdfs:subClassOf"));
                    rdfsClass.setOntologicalAnnotations(
                            parseMultiValued(entity, "owl:equivalentConcept"));
                    rdfsClass.setId(id);
                    classes.put(id, rdfsClass);

                }
                case "rdfs:Property" ->
                {
                    RdfsProperty rdfsProperty = new RdfsProperty();
                    rdfsProperty.setId(id);
                    rdfsProperty.setOntologicalAnnotations(
                            parseMultiValued(entity, "owl:equivalentConcept"));
                    rdfsProperty.setRangeIncludes(
                            parseMultiValued(entity, "schema:rangeIncludes"));
                    rdfsProperty.setDomainIncludes(
                            parseMultiValued(entity, "schema:domainIncludes"));
                    properties.put(id, rdfsProperty);

                }

            }

        }

        for (var entity : crate.getAllDataEntities())
        {
            String type = entity
                    .getProperty("@type").asText();
            String id =
                    entity.getProperty("@id")
                            .asText();
            if (!classes.containsKey(type))
            {
                continue;
            }

            Map<String, Serializable> entryProperties = new LinkedHashMap<>();
            MetadataEntry entry = new MetadataEntry();
            entry.setId(id);
            entry.setType(type);
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Serializable> keyVals =
                    objectMapper.readValue(entity.getProperties().toString(), HashMap.class);
            for (Map.Entry<String, Serializable> a : keyVals.entrySet())
            {
                if (properties.containsKey(a.getKey()))
                {
                    IRdfsProperty property = properties.get(a.getKey());
                    if (property.getRange().stream().anyMatch(x -> x.equals("xsd:string")))
                    {
                        entryProperties.put(a.getKey(), a.getValue().toString());
                    }
                }
            }
            entry.setProps(entryProperties);
            entries.put(id, entry);
        }
        System.out.println("Done");
        this.rdfsClasses = classes;
        this.rdfsProperties = properties;
        this.metadataEntries = entries;

    }

    private List<String> parseMultiValued(DataEntity dataEntity, String key)
    {
        JsonNode node = dataEntity.getProperty(key);
        if (node instanceof ObjectNode)
        {
            return List.of(node.get("@id").textValue());
        }
        if (node instanceof ArrayNode arrayNode)
        {
            List<String> accumulator = new ArrayList<>();
            arrayNode.elements().forEachRemaining(
                    x -> accumulator.add(x.get("@id").textValue())
            );
            return accumulator;
        }
        return List.of();

    }
}
