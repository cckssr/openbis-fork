package ch.eth.sis.rocrate.writer.mapping;

import ch.eth.sis.rocrate.parser.results.ParseResult;
import ch.eth.sis.rocrate.writer.Writer;
import ch.eth.sis.rocrate.writer.mapping.types.*;
import ch.eth.sis.rocrate.writer.mappinginfo.MappingInfo;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IEntityType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSetType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.ExperimentType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.DataType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Mapper
{

    public MapResult transform(ParseResult parseResult)
    {
        Map<String, List<Pair<PropertyAssignment, RdfsClass>>> classesUsingProperty =
                new HashMap<>();

        Map<String, String> openBisPropertiesToRdfsProperties = new HashMap<>();

        Map<IEntityType, String> typeToRdfsName = new HashMap<>();

        List<RdfsClass> classes = new ArrayList<RdfsClass>();
        List<RdfsProperty> properties = new ArrayList<RdfsProperty>();
        Map<String, List<IEntityType>> reverseMapping = new HashMap<String, List<IEntityType>>();
        Map<String, List<IEntityType>> rdfsPropertiesUsedIn =
                new HashMap<String, List<IEntityType>>();
        for (Map.Entry<EntityTypePermId, IEntityType> schemaEntry : parseResult.getSchema()
                .entrySet())
        {
            RdfsClass myClass = new RdfsClass();
            IEntityType value = schemaEntry.getValue();
            String rdfsID = Writer.PREFIX_SCHEMA + Writer.NAMESPACE_SEPARATOR + value.getCode();
            myClass.setId(rdfsID);
            List<IEntityType> types =
                    reverseMapping.getOrDefault(rdfsID, new ArrayList<IEntityType>());
            types.add(value);
            reverseMapping.put(rdfsID, types);
            typeToRdfsName.put(value, rdfsID);

            if (value instanceof SampleType)
            {
                myClass.setSubClassOf(new JsonLdId(Writer.SYSTEM_OBJECT));
            }
            if (value instanceof DataSetType)
            {
                myClass.setSubClassOf(new JsonLdId(Writer.SYSTEM_DATASET));
            }
            if (value instanceof ExperimentType)
            {
                myClass.setSubClassOf(new JsonLdId(Writer.SYSTEM_COLLECTION));
            }
            for (PropertyAssignment propertyAssignment : value.getPropertyAssignments())
            {
                String label = propertyAssignment.getPropertyType().getCode();
                DataType dataType = propertyAssignment.getPropertyType().getDataType();
                String propertyName = this.getRdfsName(label, dataType);

                openBisPropertiesToRdfsProperties.put(label, propertyName);
                List<IEntityType> typesProperty =
                        rdfsPropertiesUsedIn.getOrDefault(propertyName,
                                new ArrayList<IEntityType>());
                typesProperty.add(value);
                rdfsPropertiesUsedIn.put(propertyName, typesProperty);

                List<Pair<PropertyAssignment, RdfsClass>> classes1 =
                        classesUsingProperty.getOrDefault(propertyName,
                                new ArrayList<Pair<PropertyAssignment, RdfsClass>>());
                classes1.add(new ImmutablePair<PropertyAssignment, RdfsClass>(propertyAssignment,
                        myClass));
                classesUsingProperty.put(propertyName, classes1);
            }

            classes.add(myClass);

        }

        for (Map.Entry<String, List<Pair<PropertyAssignment, RdfsClass>>> a : classesUsingProperty
                .entrySet())
        {
            RdfsProperty rdfsProperty = new RdfsProperty();
            rdfsProperty.setId(Writer.PREFIX_SCHEMA + Writer.NAMESPACE_SEPARATOR + a.getKey());

            List<JsonLdId> domainIncludes =
                    a.getValue().stream().map(x -> new JsonLdId(x.getRight().getId())).toList();
            rdfsProperty.setDomainIncludes(domainIncludes);
            List<JsonLdId> range =
                    a.getValue().stream().map(x -> x.getLeft().getPropertyType().getDataType())
                            .map(Enum::name)
                            .map(this::mapOpenBisToXsdDataTypes)
                            .map(JsonLdId::new).toList();
            rdfsProperty.setRangeIncludes(range);
            properties.add(rdfsProperty);
        }

        List<MetaDataEntry> metaDataEntries = new ArrayList<MetaDataEntry>();
        for (var space : parseResult.getSpaceResult().entrySet())
        {
            metaDataEntries.add(new MetaDataEntry(space.getKey(), Writer.SYSTEM_SPACE, Map.of()))
            ;
        }
        for (var project : parseResult.getProjects().entrySet())
        {
            Map<String, Object> props = new HashMap<>();
            if (project.getValue().getDescription() != null)
            {
                props.put("description", project.getValue().getDescription());
            }
            metaDataEntries.add(
                    new MetaDataEntry(project.getKey().toString(), Writer.SYSTEM_SPACE, props));
        }

        for (var metaData : parseResult.getMetadata().entrySet())
        {
            var val = metaData.getValue();
            Map<String, Object> props = new HashMap<String, Object>();
            if (val instanceof Sample sample)
            {
                String type = typeToRdfsName.get(sample.getType());
                for (Map.Entry<String, Serializable> a : sample.getProperties().entrySet())
                {
                    String propName = openBisPropertiesToRdfsProperties.get(a.getKey());
                    props.put(propName, a.getValue());
                }
                metaDataEntries.add(
                        new MetaDataEntry(sample.getIdentifier().toString(), type, props));

            }
            if (val instanceof DataSet dataSet)
            {
                String type = typeToRdfsName.get(dataSet.getType());
                for (Map.Entry<String, Serializable> a : dataSet.getProperties().entrySet())
                {
                    String propName = openBisPropertiesToRdfsProperties.get(a.getKey());
                    props.put(propName, a.getValue());
                }
                metaDataEntries.add(new MetaDataEntry(dataSet.getCode(), type, props));

            }
            if (val instanceof Experiment experiment)
            {
                String type = typeToRdfsName.get(experiment.getType());
                for (Map.Entry<String, Serializable> a : experiment.getProperties().entrySet())
                {
                    String propName = openBisPropertiesToRdfsProperties.get(a.getKey());
                    props.put(propName, a.getValue());
                }
                metaDataEntries.add(
                        new MetaDataEntry(experiment.getIdentifier().toString(), type, props));

            }

        }

        return new MapResult(
                new RdfsSchema(classes, properties),
                new MappingInfo(reverseMapping, rdfsPropertiesUsedIn), metaDataEntries);
    }

    String mapOpenBisToXsdDataTypes(String openBisType)
    {
        DataType type = DataType.valueOf(openBisType);

        switch (type)
        {
            case VARCHAR, MULTILINE_VARCHAR, XML ->
            {
                return "xsd:string";
            }
            case BOOLEAN ->
            {
                return "xsd:boolean";
            }
            case CONTROLLEDVOCABULARY ->
            {
                return "xsd:string";
            }
            case INTEGER ->
            {
                return "xsd:integer";
            }
            case DATE ->
            {
                return "xsd:dateTime";

            }
            case JSON ->
            {
                return "xsd:string";
            }
            case REAL ->
            {
                return "xsd:double";
            }
            case TIMESTAMP ->
            {
                return "xsd:dateTime";
            }
            default ->
            {
                throw new RuntimeException("Unknown type: " + openBisType);
            }
        }

    }

    public String getRdfsName(String label, DataType dataType)
    {
        if (dataType.equals(DataType.BOOLEAN))
        {
            return "is" + label;
        }
        return "has" + label;

    }
}