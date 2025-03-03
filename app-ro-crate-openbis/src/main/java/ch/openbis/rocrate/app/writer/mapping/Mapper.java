package ch.openbis.rocrate.app.writer.mapping;

import ch.eth.sis.rocrate.facade.MetadataEntry;
import ch.eth.sis.rocrate.facade.RdfsClass;
import ch.eth.sis.rocrate.facade.TypeProperty;
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
import ch.openbis.rocrate.app.parser.results.ParseResult;
import ch.openbis.rocrate.app.writer.Writer;
import ch.openbis.rocrate.app.writer.mapping.types.MapResult;
import ch.openbis.rocrate.app.writer.mapping.types.RdfsSchema;
import ch.openbis.rocrate.app.writer.mappinginfo.MappingInfo;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class Mapper
{

    public MapResult transform(ParseResult parseResult)
    {
        Map<String, List<Pair<PropertyAssignment, RdfsClass>>> classesUsingProperty =
                new HashMap<>();

        Map<String, String> openBisPropertiesToRdfsProperties = new HashMap<>();

        Map<IEntityType, String> typeToRdfsName = new HashMap<>();

        List<RdfsClass> classes = new ArrayList<RdfsClass>();
        List<TypeProperty> properties = new ArrayList<TypeProperty>();
        Map<String, List<IEntityType>> reverseMapping = new HashMap<String, List<IEntityType>>();
        Map<String, List<IEntityType>> rdfsPropertiesUsedIn =
                new HashMap<String, List<IEntityType>>();
        for (Map.Entry<EntityTypePermId, IEntityType> schemaEntry : parseResult.getSchema()
                .entrySet())
        {
            RdfsClass myClass = new RdfsClass();
            IEntityType value = schemaEntry.getValue();
            String rdfsID = value.getCode();
            myClass.setId(rdfsID);
            List<IEntityType> types =
                    reverseMapping.getOrDefault(rdfsID, new ArrayList<IEntityType>());
            types.add(value);
            reverseMapping.put(rdfsID, types);
            typeToRdfsName.put(value, rdfsID);

            if (value instanceof SampleType)
            {
                myClass.setSubClassOf(List.of(Writer.SYSTEM_OBJECT));
            }
            if (value instanceof DataSetType)
            {
                myClass.setSubClassOf(List.of(Writer.SYSTEM_DATASET));
            }
            if (value instanceof ExperimentType)
            {
                myClass.setSubClassOf(List.of(Writer.SYSTEM_COLLECTION));
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
                classes1.add(
                        new ImmutablePair<PropertyAssignment, RdfsClass>(propertyAssignment,
                                myClass));
                classesUsingProperty.put(propertyName, classes1);
            }

            List<String> semanticAnnotations = Optional.ofNullable(
                            parseResult.getSemanticAnnotationByKind().getEntityTypeAnnotations()
                                    .get(rdfsID))
                    .map(x -> x.stream().map(y -> y.getDescriptorAccessionId())
                            .distinct()
                            .toList())
                    .orElse(List.of());

            myClass.setOntologicalAnnotations(
                    semanticAnnotations);

            classes.add(myClass);

        }

        for (Map.Entry<String, List<Pair<PropertyAssignment, RdfsClass>>> a : classesUsingProperty
                .entrySet())
        {
            TypeProperty rdfsProperty = new TypeProperty();
            rdfsProperty.setId(a.getKey());

            List<String> domainIncludes =
                    a.getValue().stream().map(x -> x.getRight().getId()).toList();
            rdfsProperty.setDomainIncludes(domainIncludes);
            List<String> range =
                    a.getValue().stream().map(x -> x.getLeft().getPropertyType().getDataType())
                            .map(Enum::name)
                            .distinct()
                            .map(this::mapOpenBisToXsdDataTypes)
                            .toList();
            rdfsProperty.setRangeIncludes(range);

            List<String> semanticAnnotations = Optional.ofNullable(
                            parseResult.getSemanticAnnotationByKind().getEntityPropertyTypeAnnotations()
                                    .get(deRdfsName(a.getKey())))
                    .map(x -> x.stream().map(y -> y.getDescriptorAccessionId()).distinct().toList())
                    .orElse(List.of());

            rdfsProperty.setOntologicalAnnotations(semanticAnnotations);

            properties.add(rdfsProperty);
        }

        List<MetadataEntry> metaDataEntries = new ArrayList<MetadataEntry>();
        for (var space : parseResult.getSpaceResult().entrySet())
        {
            metaDataEntries.add(
                    new MetadataEntry(space.getKey(), Writer.SYSTEM_SPACE, Map.of(), Map.of()))
            ;
        }
        for (var project : parseResult.getProjects().entrySet())
        {
            Map<String, Serializable> props = new HashMap<>();
            if (project.getValue().getDescription() != null)
            {
                props.put("description", project.getValue().getDescription());
            }
            metaDataEntries.add(
                    new MetadataEntry(project.getKey().toString(), Writer.SYSTEM_SPACE, props,
                            Map.of()));
        }

        for (var metaData : parseResult.getMetadata().entrySet())
        {
            var val = metaData.getValue();
            Map<String, Serializable> props = new HashMap<String, Serializable>();
            if (val instanceof Sample sample)
            {

                Map<String, List<String>> references = new LinkedHashMap<>();

                Set<String> referenceTypeNames = parseResult.getSchema().values().stream()
                        .map(x -> x.getPropertyAssignments())
                        .flatMap(Collection::stream).map(x -> x.getPropertyType())
                        .filter(x -> x.getDataType().name().startsWith("SAMPLE"))
                        .map(x -> x.getCode())
                        .collect(Collectors.toSet());

                String type = typeToRdfsName.get(sample.getType());
                for (Map.Entry<String, Serializable> a : sample.getProperties().entrySet())
                {
                    String propName = openBisPropertiesToRdfsProperties.get(a.getKey());
                    if (!referenceTypeNames.contains(a.getKey()))
                    {
                        props.put(propName, a.getValue());
                    } else
                    {
                        references.put(propName, List.of(a.getValue().toString()));

                    }

                }

                references.put("children", ((Sample) val).getChildren().stream()
                        .map(x -> x.getIdentifier().getIdentifier()).toList());
                references.put("parents", ((Sample) val).getChildren().stream()
                        .map(x -> x.getIdentifier().getIdentifier()).toList());
                MetadataEntry
                        entry =
                        new MetadataEntry(sample.getIdentifier().toString(), type, props,
                                references);

                metaDataEntries.add(entry
                );

            }
            if (val instanceof DataSet dataSet)
            {
                String type = typeToRdfsName.get(dataSet.getType());
                for (Map.Entry<String, Serializable> a : dataSet.getProperties().entrySet())
                {
                    String propName = openBisPropertiesToRdfsProperties.get(a.getKey());
                    props.put(propName, a.getValue());
                }
                metaDataEntries.add(
                        new MetadataEntry(dataSet.getCode(), type, props, Map.of()));

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
                        new MetadataEntry(experiment.getIdentifier().toString(), type, props,
                                Map.of()));

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
            case SAMPLE ->
            {
                return ":Object";
            }
            case HYPERLINK ->
            {
                return "xsd:string";
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

    private String deRdfsName(String label)
    {
        if (label.startsWith("is"))
        {
            return label.replaceFirst("is", "");
        }
        return label.replaceFirst("has", "");
    }

}