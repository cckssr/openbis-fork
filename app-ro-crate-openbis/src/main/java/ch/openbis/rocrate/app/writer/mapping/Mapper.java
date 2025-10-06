package ch.openbis.rocrate.app.writer.mapping;

import ch.eth.sis.rocrate.facade.*;
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
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.openbis.rocrate.app.Constants;
import ch.openbis.rocrate.app.writer.mapping.types.MapResult;
import ch.openbis.rocrate.app.writer.mapping.types.RdfsSchema;
import ch.openbis.rocrate.app.writer.mappinginfo.MappingInfo;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.Serializable;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.stream.Collectors;

public class Mapper
{

    public static final String CANONICAL_OPENBIS_DATE_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss Z";



    public MapResult transform(OpenBisModel openBisModel)
    {
        Map<String, List<Pair<PropertyAssignment, IType>>> classesUsingProperty =
                new HashMap<>();

        Map<String, String> openBisPropertiesToRdfsProperties = new HashMap<>();

        Map<IEntityType, String> typeToRdfsName = new HashMap<>();

        Map<String, IType> classes = new LinkedHashMap<>();
        List<PropertyType> properties = new ArrayList<PropertyType>();
        Map<String, List<IEntityType>> reverseMapping = new HashMap<String, List<IEntityType>>();
        Map<String, List<IEntityType>> rdfsPropertiesUsedIn =
                new HashMap<String, List<IEntityType>>();
        List<IType> typesWithSpace = new ArrayList<>();
        List<IType> typesWithProject = new ArrayList<>();
        List<IType> typesWithCollection
                = new ArrayList<>();

        {
            IType type = getSpaceType();
            classes.put(type.getId(), getSpaceType());
        }

        {
            IType type = getCollectionType();
            classes.put(type.getId(), getCollectionType());
        }

        {
            IType type = getProjectType();
            classes.put(type.getId(), getProjectType());
        }



        for (Map.Entry<EntityTypePermId, IEntityType> schemaEntry : openBisModel.getEntityTypes()
                .entrySet())
        {
            Type myClass = new Type();
            IEntityType value = schemaEntry.getValue();
            String rdfsID = value.getCode();
            myClass.setId(rdfsID);
            List<IEntityType> types =
                    reverseMapping.getOrDefault(rdfsID, new ArrayList<IEntityType>());
            types.add(value);
            reverseMapping.put(rdfsID, types);
            typeToRdfsName.put(value, rdfsID);
            typesWithCollection
                    .add(myClass);
            typesWithProject.add(myClass);
            typesWithSpace.add(myClass);

            if (value instanceof SampleType)
            {
                SampleType sampleType = (SampleType) value;
                List<String> ontologicalAnnotations =
                        ((SampleType) value).getSemanticAnnotations().stream()
                                .map(x -> x.getPredicateAccessionId()).collect(
                                        Collectors.toList());
                myClass.setOntologicalAnnotations(ontologicalAnnotations);

                myClass.setSubClassOf(List.of(Constants.GRAPH_ID_OBJECT));
            }
            if (value instanceof DataSetType)
            {
                myClass.setSubClassOf(List.of(Constants.GRAPH_ID_DATASET));
            }
            if (value instanceof ExperimentType)
            {
                myClass.setSubClassOf(List.of(Constants.GRAPH_ID_Collection));
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

                List<Pair<PropertyAssignment, IType>> classes1 =
                        classesUsingProperty.getOrDefault(propertyName,
                                new ArrayList<Pair<PropertyAssignment, IType>>());
                classes1.add(
                        new ImmutablePair<PropertyAssignment, IType>(propertyAssignment, (IType)
                                myClass));
                classesUsingProperty.put(propertyName, classes1);
            }

            classes.put(myClass.getId(), myClass);

        }

        for (Map.Entry<String, List<Pair<PropertyAssignment, IType>>> a : classesUsingProperty
                .entrySet())
        {
            PropertyType rdfsProperty = new PropertyType();
            rdfsProperty.setId(a.getKey());

            List<IType> domainIncludes =
                    a.getValue().stream().map(x -> x.getRight()).toList();
            rdfsProperty.setDomainIncludes(domainIncludes);

            rdfsProperty.setLabel(a.getValue().stream().map(Pair::getLeft)
                    .map(PropertyAssignment::getPropertyType)
                    .findFirst().map(x -> x.getLabel()).orElse(null)
            );

            rdfsProperty.setComment(a.getValue().stream().map(Pair::getLeft)
                    .map(PropertyAssignment::getPropertyType)
                    .findFirst().map(x -> x.getDescription()).orElse(null));

            int minCardinality = a.getValue().stream().map(x -> x.getLeft())
                    .findFirst()
                    .map(x -> x.isMandatory())
                    .orElse(false) ? 1 : 0;

            int maxCardinality = a.getValue().stream().map(x -> x.getLeft())
                    .findFirst()
                    .map(x -> x.getPropertyType().isMultiValue())
                    .orElse(false) ? 0 : 1;

            if (minCardinality != 0 || maxCardinality != 0)
            {
                for (IType type : domainIncludes)
                {
                    Restriction restriction =
                            new Restriction(UUID.randomUUID().toString(), rdfsProperty,
                                    minCardinality, maxCardinality);
                    Type type1 = (Type) type;
                    type1.addRestriction(restriction);
                }

            }


            a.getValue().stream().map(x -> x.getLeft().getPropertyType().getDataType())
                    .filter(x -> x != DataType.SAMPLE)
                    .map(Enum::name)
                    .distinct()
                    .map(this::mapOpenBisToXsdDataTypes)
                    .forEach(rdfsProperty::addDataType);
            a.getValue().stream()
                    .filter(x -> x.getLeft().getPropertyType().getDataType() == DataType.SAMPLE)
                    .filter(x -> x.getLeft().getPropertyType().getSampleType() != null)
                    .map(x -> classes.get(x.getLeft().getPropertyType().getSampleType().getCode()))
                    .forEach(rdfsProperty::addType);


            List<String> semanticAnnotations = a.getValue().stream().map(x -> x.getLeft())
                    .map(x -> x.getPropertyType())
                    .filter(x -> x.getSemanticAnnotations() != null)
                    .map(x -> x.getSemanticAnnotations())
                    .flatMap(Collection::stream)
                    .map(x -> x.getPredicateAccessionId())
                    .collect(Collectors.toList());
            rdfsProperty.setOntologicalAnnotations(semanticAnnotations);

            properties.add(rdfsProperty);
        }

        List<MetadataEntry> metaDataEntries = new ArrayList<MetadataEntry>();
        for (Map.Entry<SpacePermId, Space> space : openBisModel.getSpaces().entrySet())
        {
            metaDataEntries.add(
                    new MetadataEntry(space.getKey().getPermId(), Set.of(Constants.GRAPH_ID_SPACE),
                            Map.of(), Map.of()))
            ;
        }
        openBisModel.getProjects().entrySet().forEach(project -> {
            Map<String, Serializable> props = new HashMap<>();
            if (project.getValue().getDescription() != null)
            {
                props.put("description", project.getValue().getDescription());
            }
            metaDataEntries.add(
                    new MetadataEntry(project.getKey().toString(),
                            Set.of(Constants.GRAPH_ID_PROJECT),
                            props,
                            Map.of()));
        });

        for (var metaData : openBisModel.getEntities().entrySet())
        {
            var val = metaData.getValue();
            Map<String, Serializable> props = new LinkedHashMap<>();
            Map<String, List<String>> references = new LinkedHashMap<>();

            if (val instanceof Sample sample)
            {
                references.put(Constants.PROPERTY_SPACE, List.of(sample.getSpace().getCode()));
                String projectIdentifier =
                        Optional.ofNullable(sample.getProject()).map(x -> x.getIdentifier())
                                .map(x -> x.getIdentifier()).orElse("/DEFAULT/DEFAULT");

                references.put(Constants.PROPERTY_PROJECT,
                        List.of(projectIdentifier));

                String experimentIdentifier = Optional.ofNullable(sample.getExperiment())
                        .map(x -> x.getIdentifier())
                        .map(x -> x.getIdentifier())
                        .orElse(sample.getType().getCode() + "_Collection");

                references.put(Constants.PROPERTY_COLLECTION,
                        List.of(experimentIdentifier));

                Set<String> referenceTypeNames = openBisModel.getEntityTypes().values().stream()
                        .map(x -> x.getPropertyAssignments())
                        .flatMap(Collection::stream).map(x -> x.getPropertyType())
                        .filter(x -> x.getDataType().name().startsWith("SAMPLE"))
                        .map(x -> x.getCode())
                        .collect(Collectors.toSet());

                Map<String, DataType> codeToDataType = sample.getType().getPropertyAssignments()
                        .stream()
                        .map(x -> x.getPropertyType())
                        .collect(Collectors.toMap(x -> x.getCode(), x -> x.getDataType()));

                String type = typeToRdfsName.get(sample.getType());
                for (Map.Entry<String, Serializable> a : sample.getProperties().entrySet())
                {
                    if (a.getValue() == null)
                    {
                        continue;
                    }
                    String propName = openBisPropertiesToRdfsProperties.get(a.getKey());
                    DataType dataType = codeToDataType.get(a.getKey());
                    if (!referenceTypeNames.contains(a.getKey()))
                    {
                        String[] vals = extractSerializableList(a.getValue()).stream()
                                .map(x -> mapValue(x, dataType)).map(x -> x.toString())
                                .toArray(String[]::new);

                        props.put(propName, vals);
                    } else
                    {

                        references.put(propName, extractSerializableList(a.getValue()).stream()
                                .map(x -> mapValue(x, dataType)).collect(
                                        Collectors.toList()));

                    }

                }

                references.put("children", ((Sample) val).getChildren().stream()
                        .map(x -> x.getIdentifier().getIdentifier()).collect(Collectors.toList()));
                references.put("parents", ((Sample) val).getChildren().stream()
                        .map(x -> x.getIdentifier().getIdentifier()).collect(Collectors.toList()));
                MetadataEntry
                        entry =
                        new MetadataEntry(sample.getIdentifier().toString(), Set.of(type), props,
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
                        new MetadataEntry(dataSet.getCode(), Set.of(type), props, Map.of()));

            }
            if (val instanceof Experiment experiment)
            {
                String type = Constants.GRAPH_ID_Collection;
                for (Map.Entry<String, Serializable> a : experiment.getProperties().entrySet())
                {
                    String propName = openBisPropertiesToRdfsProperties.get(a.getKey());
                    props.put(propName, a.getValue());
                }
                metaDataEntries.add(
                        new MetadataEntry(experiment.getIdentifier().toString(), Set.of(type),
                                props,
                                Map.of()));

            }

        }
        {
            PropertyType propertyType = new PropertyType();
            propertyType.setId(Constants.PROPERTY_SPACE);
            propertyType.setDomainIncludes(typesWithSpace);
            propertyType.addType(getSpaceType()); //Constants.GRAPH_ID_SPACE
            properties.add(propertyType);
        }
        {
            PropertyType propertyType = new PropertyType();
            propertyType.setId(Constants.PROPERTY_PROJECT);
            propertyType.setDomainIncludes(typesWithProject);
            propertyType.addType(getProjectType());
        }
        {
            PropertyType propertyType = new PropertyType();
            propertyType.setId(Constants.PROPERTY_COLLECTION);
            propertyType.setDomainIncludes(typesWithCollection
            );
            propertyType.addType(getCollectionType());
            properties.add(propertyType);

        }


        return new MapResult(
                new RdfsSchema(classes.values().stream().collect(Collectors.toList()), properties),
                new MappingInfo(reverseMapping, rdfsPropertiesUsedIn), metaDataEntries);
    }

    private String mapValue(String val, DataType dataType)
    {
        if (dataType == DataType.DATE || dataType == DataType.TIMESTAMP)
        {

            DateTimeFormatter dateTimeFormatter =
                    DateTimeFormatter.ofPattern(CANONICAL_OPENBIS_DATE_FORMAT_PATTERN);
            TemporalAccessor parsed = dateTimeFormatter.parse(val);
            Instant i = Instant.from(parsed);
            Date d = Date.from(i);
            String format = DateTimeFormatter.ISO_DATE_TIME.format(parsed);
            return format;

        }

        return val;

    }



    private List<String> extractSerializableList(Serializable a)
    {
        if (a instanceof Serializable[])
        {
            return Arrays.stream(((Serializable[]) a)).map(x -> x.toString()).toList();
        }
        return List.of(a.toString());

    }

    IType mapOpenBisToRdfType()
    {
        return null;
    }

    IDataType mapOpenBisToXsdDataTypes(String openBisType)
    {
        DataType type = DataType.valueOf(openBisType);

        switch (type)
        {
            case VARCHAR, MULTILINE_VARCHAR, XML ->
            {
                return LiteralType.STRING;
            }
            case BOOLEAN ->
            {
                return LiteralType.STRING;
            }
            case CONTROLLEDVOCABULARY ->
            {
                return LiteralType.STRING;
            }
            case INTEGER ->
            {
                return LiteralType.INTEGER;
            }
            case DATE ->
            {
                return LiteralType.DATETIME;

            }
            case JSON ->
            {
                return LiteralType.STRING;
            }
            case REAL ->
            {
                return LiteralType.DOUBLE;
            }
            case TIMESTAMP ->
            {
                return LiteralType.DATETIME;
            }
            case HYPERLINK ->
            {
                return LiteralType.STRING;
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
            return "openBIS:is" + label;
        }
        return "openBIS:has" + label;

    }

    private String deRdfsName(String label)
    {
        if (label.startsWith("is"))
        {
            return label.replaceFirst("is", "");
        }
        return label.replaceFirst("has", "");
    }

    IType getSpaceType()
    {
        Type type = new Type();
        type.setId(Constants.GRAPH_ID_SPACE);
        type.setType("rdfs:class");
        return type;

    }

    IType getCollectionType()
    {
        Type type = new Type();
        type.setId(Constants.GRAPH_ID_Collection);
        type.setType("rdfs:class");
        return type;

    }

    IType getProjectType()
    {
        Type type = new Type();
        type.setId(Constants.GRAPH_ID_PROJECT);
        type.setType("rdfs:class");
        return type;

    }

}