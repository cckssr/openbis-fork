package ch.openbis.rocrate.app.reader;

import ch.eth.sis.rocrate.facade.*;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.entity.AbstractEntityPropertyHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.id.ObjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IEntityType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.ExperimentType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.fetchoptions.ProjectFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.ProjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.DataType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.fetchoptions.PropertyAssignmentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.fetchoptions.PropertyTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.id.PropertyAssignmentPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.id.PropertyTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.SemanticAnnotation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.openbis.rocrate.app.Constants;
import ch.openbis.rocrate.app.reader.helper.DataTypeMatcher;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ch.openbis.rocrate.app.Constants.*;

public class RdfToModel
{

    public static OpenBisModel convert(List<IType> types, List<IPropertyType> typeProperties,
            List<IMetadataEntry> entries, String fallbackSpaceCode, String fallbackProjectCode)
    {

        Map<String, IType> IdsToTypes =
                types.stream().collect(Collectors.toMap(IType::getId, Function.identity()));

        Map<String, EntityTypePermId> entityTypeToRdfIdentifier = new LinkedHashMap<>();

        Map<String, List<String>> typeToInheritanceChain = new LinkedHashMap<>();

        Map<SpacePermId, Space> spaces = new LinkedHashMap<>();

        Map<ProjectIdentifier, Project> projects = new LinkedHashMap<>();

        Map<String, SampleType> codeToSampleType = new LinkedHashMap<>();

        List<Pair<Sample, ReferencesToResolve>> samplesWithSpaceAndProjectCodes = new ArrayList<>();

        Map<String, Sample> roCrateIdsToObjects = new LinkedHashMap<>();

        Map<String, ExperimentType> identifierToCollectionType = new LinkedHashMap<>();

        Map<ExperimentIdentifier, Experiment> idsToCollections = new LinkedHashMap<>();


        Map<EntityTypePermId, IEntityType> schema = new LinkedHashMap<>();
        handleTypes(types, IdsToTypes, typeToInheritanceChain, entityTypeToRdfIdentifier,
                codeToSampleType,
                schema, identifierToCollectionType);

        Map<IType, List<String>> typesToProperties = new LinkedHashMap<>();
        for (IPropertyType typeProperty : typeProperties)
        {
            for (IType domain : typeProperty.getDomain())
            {
                List<String> typeToDomain =
                        typesToProperties.getOrDefault(domain, new ArrayList<>());
                typeToDomain.add(typeProperty.getId());
                typesToProperties.put(domain, typeToDomain);
            }
        }
        Map<Pair<String, DataType>, PropertyTypeMapping> propertyTypeMappings =
                new LinkedHashMap<>();
        Map<String, Set<DataType>> baseCodeToPossibleDataTypes = new LinkedHashMap<>();

        handlePropertyTypes(typeProperties, baseCodeToPossibleDataTypes, propertyTypeMappings,
                codeToSampleType);
        handleIntersectionTypes(entries, schema, entityTypeToRdfIdentifier, codeToSampleType);
        Map<String, String> identifierToOpenBisCode = new LinkedHashMap<>();

        Map<ObjectIdentifier, AbstractEntityPropertyHolder> metadata = new LinkedHashMap<>();
        Map<String, IMetadataEntry> idToEntities =
                entries.stream().collect(Collectors.toMap(x -> x.getId(), x -> x, (x, y) -> y));
        processEntities(entries, fallbackSpaceCode, fallbackProjectCode, typeToInheritanceChain,
                codeToSampleType,
                identifierToOpenBisCode, baseCodeToPossibleDataTypes, idToEntities,
                roCrateIdsToObjects,
                samplesWithSpaceAndProjectCodes, spaces, projects);

        mapSpaces(fallbackSpaceCode, fallbackProjectCode, spaces, projects);
        mapProjects(projects, spaces);

        mapCollections(entries, typeToInheritanceChain, identifierToCollectionType,
                idsToCollections);

        resolveSpaceProjectAndCollections(samplesWithSpaceAndProjectCodes, spaces, projects,
                idsToCollections, metadata);

        resolveOpenBisStructure(entries, fallbackSpaceCode, fallbackProjectCode,
                typeToInheritanceChain,
                roCrateIdsToObjects, spaces, projects);

        return new OpenBisModel(Map.of(), schema, spaces, projects, metadata, Map.of(), Map.of(),
                identifierToOpenBisCode);
    }

    private static void handleTypes(List<IType> types, Map<String, IType> IdsToTypes,
            Map<String, List<String>> typeToInheritanceChain,
            Map<String, EntityTypePermId> entityTypeToRdfIdentifier,
            Map<String, SampleType> codeToSampleType, Map<EntityTypePermId, IEntityType> schema,
            Map<String, ExperimentType> identifierToCollectionType)
    {
        for (IType type : types)
        {
            if (isProject(type) || isSpace(type))
            {
                System.out.println("Type " + type.getId() + " is space or project, skipping");
                continue;
            }

            List<IType> inheritanceChain = getInheritanceChain(type, IdsToTypes);
            typeToInheritanceChain.put(type.getId(),
                    inheritanceChain.stream().map(IType::getId).collect(
                            Collectors.toList()));

            EntityKind kind = matchKind(inheritanceChain);
            if (kind == EntityKind.SAMPLE)
            {

                SampleTypeFetchOptions
                        sampleTypeFetchOptions = getSampleTypeFetchOptions();

                SampleType sampleType = new SampleType();
                sampleType.setCode(openBisifyCode(removePrefix(type.getId())));
                sampleType.setFetchOptions(sampleTypeFetchOptions);
                sampleType.setPermId(new EntityTypePermId(sampleType.getCode(), EntityKind.SAMPLE));
                entityTypeToRdfIdentifier.put(type.getId(), sampleType.getPermId());

                sampleType.setPropertyAssignments(new ArrayList<>());

                codeToSampleType.put(sampleType.getCode(), sampleType);

                if (!isCollection(type) && (isOpenBisDerivedType(type) || isSample(type)))
                {
                    schema.put(sampleType.getPermId(), sampleType);
                }
                type.getOntologicalAnnotations().forEach(x -> {
                    SemanticAnnotation semanticAnnotation = new SemanticAnnotation();
                    semanticAnnotation.setPredicateAccessionId(x);
                    semanticAnnotation.setPredicateAccessionId(x);
                    semanticAnnotation.setPredicateOntologyVersion(x);

                    List<SemanticAnnotation> existingAnnotations =
                            sampleType.getSemanticAnnotations();
                    List<SemanticAnnotation> newAnnotations = new ArrayList<>();
                    if (existingAnnotations != null)
                    {
                        newAnnotations.addAll(existingAnnotations);
                    }
                    newAnnotations.add(semanticAnnotation);
                    sampleType.setSemanticAnnotations(newAnnotations);

                });


            }
            if (kind == EntityKind.EXPERIMENT)
            {
                ExperimentTypeFetchOptions fetchOptions = new ExperimentTypeFetchOptions();
                fetchOptions.withPropertyAssignments();

                ExperimentType experimentType = new ExperimentType();
                experimentType.setCode(getCollectionTypeCode(type));
                experimentType.setPermId(new EntityTypePermId(type.getId(), kind));

                if (isOpenBisDerivedType(type))
                {
                    schema.put(experimentType.getPermId(), experimentType);
                }
                identifierToCollectionType.put(type.getId(), experimentType);
            }

        }
    }

    private static void handlePropertyTypes(List<IPropertyType> typeProperties,
            Map<String, Set<DataType>> baseCodeToPossibleDataTypes,
            Map<Pair<String, DataType>, PropertyTypeMapping> propertyTypeMappings,
            Map<String, SampleType> codeToSampleType)
    {
        for (IPropertyType a : typeProperties)
        {
            Set<DataType> dataTypes = matchDataTypes(a);
            boolean addSuffixes = dataTypes.size() > 1;
            String baseCode = openBisifyCode(deRdfIdentifier(a.getId()));
            baseCodeToPossibleDataTypes.put(baseCode, dataTypes);

            for (DataType dataType : dataTypes)
            {
                String code = baseCode;
                PropertyType propertyType = new PropertyType();
                {
                    PropertyTypeFetchOptions fetchOptions = new PropertyTypeFetchOptions();
                    fetchOptions.withSemanticAnnotations();
                    fetchOptions.withVocabulary();
                    fetchOptions.withSampleType();

                    propertyType.setFetchOptions(fetchOptions);
                }
                propertyType.setSemanticAnnotations(new ArrayList<>());
                propertyType.setMultiValue(false);
                if (addSuffixes)
                {
                    String newCode = suffixCodeType(baseCode, dataType);
                    propertyTypeMappings.put(new ImmutablePair<>(baseCode, dataType),
                            new PropertyTypeMapping(baseCode, newCode, dataType));
                    code = newCode;
                }

                propertyType.setCode(code);
                propertyType.setDescription(propertyType.getCode());
                propertyType.setLabel(propertyType.getCode());

                propertyType.setPermId(new PropertyTypePermId(baseCode));
                propertyType.setDataType(dataType);

                for (IType domain : a.getDomain())
                {
                    if (requiresSpecialHandling(a))
                    {
                        continue;
                    }

                    SampleType sampleType = codeToSampleType.get(openBisifyCode(domain.getId()));
                    if (sampleType != null)
                    {
                        List<PropertyAssignment> assignments = sampleType.getPropertyAssignments();
                        List<PropertyAssignment> newAssignments = new ArrayList<>();
                        Optional<IRestriction> maybeRestriction = domain.getResstrictions().stream()
                                .filter(x -> x.getPropertyType().equals(a))
                                .findFirst();
                        PropertyAssignment curProperty =
                                getPropertyAssignment(propertyType, sampleType,
                                        maybeRestriction.filter(x -> x.getMinCardinality() == 1)
                                                .isPresent(),
                                        maybeRestriction.filter(x -> x.getMaxCardinality() == 0)
                                                .isPresent());

                        newAssignments.add(curProperty);
                        if (assignments != null)
                        {
                            newAssignments.addAll(assignments);
                        }
                        sampleType.setPropertyAssignments(newAssignments);
                        a.getOntologicalAnnotations().forEach(x -> {
                            SemanticAnnotation annotation = new SemanticAnnotation();
                            annotation.setPredicateAccessionId(x);
                            annotation.setEntityType(sampleType);
                            annotation.setPropertyType(propertyType);
                            annotation.setPredicateAccessionId(x);
                            annotation.setPredicateOntologyId(x);
                            annotation.setPredicateOntologyVersion(x);

                            List<SemanticAnnotation> existingAnnotations =
                                    propertyType.getSemanticAnnotations();
                            List<SemanticAnnotation> annotations = new ArrayList<>();
                            if (existingAnnotations != null)
                            {
                                annotations.addAll(existingAnnotations);
                            }
                            annotations.add(annotation);
                            propertyType.setSemanticAnnotations(annotations);
                            curProperty.setSemanticAnnotations(annotations);
                        });
                    }

                }

            }
        }
    }

    private static void handleIntersectionTypes(List<IMetadataEntry> entries,
            Map<EntityTypePermId, IEntityType> schema,
            Map<String, EntityTypePermId> entityTypeToRdfIdentifier,
            Map<String, SampleType> codeToSampleType)
    {
        Set<Set<String>> intersectionTypes = new LinkedHashSet<>();
        for (IMetadataEntry entry : entries)
        {
            if (entry.getTypes().size() > 1)
            {
                intersectionTypes.add(entry.getTypes());
            }
        }
        for (Set<String> intersectionType : intersectionTypes)
        {
            SampleType sampleType = new SampleType();
            sampleType.setFetchOptions(getSampleTypeFetchOptions());
            String artificialTypeIdentifier =
                    openBisifyCode(getIntersectionTypeIdentifier(intersectionType));
            sampleType.setCode(artificialTypeIdentifier);

            List<PropertyAssignment> assignments = new ArrayList<>();
            List<SemanticAnnotation> semanticAnnotations = new ArrayList<>();

            for (String type : intersectionType)
            {

                IEntityType entityType = schema.get(entityTypeToRdfIdentifier.get(type));
                if (entityType == null)
                {
                    continue;
                }
                SampleType sampleType1 =
                        (SampleType) entityType;

                for (var propertyAssignment : sampleType1.getPropertyAssignments())
                {
                    PropertyAssignment newAssignment = new PropertyAssignment();
                    newAssignment.setMandatory(propertyAssignment.isMandatory());
                    newAssignment.setFetchOptions(propertyAssignment.getFetchOptions());
                    newAssignment.setPropertyType(propertyAssignment.getPropertyType());
                    newAssignment.setSemanticAnnotations(
                            propertyAssignment.getSemanticAnnotations());
                    newAssignment.setUnique(propertyAssignment.isUnique());
                    newAssignment.setEntityType(sampleType1);
                    if (assignments.stream().noneMatch(
                            x -> x.getPropertyType().equals(newAssignment.getPropertyType())))
                    {
                        assignments.add(newAssignment);
                    }
                }

            }
            sampleType.setPropertyAssignments(assignments);
            sampleType.setSemanticAnnotations(semanticAnnotations);
            sampleType.setCode(artificialTypeIdentifier);
            sampleType.setPermId(new EntityTypePermId(sampleType.getCode(), EntityKind.SAMPLE));
            schema.put(sampleType.getPermId(), sampleType);
            codeToSampleType.put(sampleType.getCode(), sampleType);

        }
    }

    private static void processEntities(List<IMetadataEntry> entries, String fallbackSpaceCode,
            String fallbackProjectCode, Map<String, List<String>> typeToInheritanceChain,
            Map<String, SampleType> codeToSampleType, Map<String, String> identifierToOpenBisCode,
            Map<String, Set<DataType>> baseCodeToPossibleDataTypes,
            Map<String, IMetadataEntry> idToEntities, Map<String, Sample> roCrateIdsToObjects,
            List<Pair<Sample, ReferencesToResolve>> samplesWithSpaceAndProjectCodes,
            Map<SpacePermId, Space> spaces, Map<ProjectIdentifier, Project> projects)
    {
        for (IMetadataEntry entry : entries)
        {
            AbstractEntityPropertyHolder entity;
            ObjectIdentifier objectIdentifier;

            Optional<EntityKind> entityKind =
                    matchEntityKind(entry, typeToInheritanceChain);

            if (entityKind.filter(x -> x == EntityKind.SAMPLE).isPresent())
            {
                Sample sample = new Sample();
                {
                    SampleFetchOptions fetchOptions = new SampleFetchOptions();
                    fetchOptions.withType();
                    fetchOptions.withProject();
                    fetchOptions.withSpace();
                    fetchOptions.withProperties();
                    fetchOptions.withExperiment();
                    sample.setFetchOptions(fetchOptions);
                }
                String typeCode = entry.getTypes().size() == 1 ?
                        entry.getTypes().stream().findFirst().orElseThrow() :
                        getIntersectionTypeIdentifier(entry.getTypes());

                SampleType type = codeToSampleType.get(openBisifyCode(typeCode));
                sample.setType(type);

                String code = createSampleCode(type, entry.getId());
                sample.setCode(code);
                identifierToOpenBisCode.put(entry.getId(), code);

                objectIdentifier = new SampleIdentifier(entry.getId());
                entity = sample;
                Map<String, Serializable> properties = new LinkedHashMap<>();
                for (Map.Entry<String, Serializable> property : entry.getValues().entrySet())
                {
                    if (requiresSpecialHandling(property.getKey()))
                    {
                        continue;
                    }

                    String key = openBisifyCode(deRdfIdentifier(property.getKey()));
                    if (baseCodeToPossibleDataTypes.containsKey(
                            key) && baseCodeToPossibleDataTypes.get(key).size() > 1)
                    {
                        Set<DataType> dataTypes = baseCodeToPossibleDataTypes.get(key);
                        DataType dataType =
                                DataTypeMatcher.findDataType(property.getValue(), dataTypes,
                                        idToEntities);
                        properties.put(key, property.getValue());

                    } else
                    {

                        properties.put(key,
                                property.getValue());
                    }
                }

                roCrateIdsToObjects.put(entry.getId(), sample);



                sample.setProperties(properties);
                properties.get("SPACE");
                ReferencesToResolve referencesToResolve =
                        buildEntryWithSpaceAndProjectToResolve(entry.getReferences(),
                                fallbackSpaceCode,
                                fallbackProjectCode, sample.getType().getCode() + "_COLLECTION");
                samplesWithSpaceAndProjectCodes.add(
                        new ImmutablePair<>(sample, referencesToResolve));

            } else if (entry.getTypes().stream().anyMatch(x -> x.equals(GRAPH_ID_SPACE)))
            {
                Space space = new Space();
                space.setCode(entry.getId());
                spaces.put(new SpacePermId(space.getCode()), space);
            } else if (entry.getTypes().contains(GRAPH_ID_PROJECT))
            {
                Project project = new Project();
                {
                    ProjectFetchOptions fetchOptions = new ProjectFetchOptions();
                    fetchOptions.withSpace();

                    project.setFetchOptions(fetchOptions);

                }

                String projectCode = entry.getId().split("/")[2];
                String spaceCode = entry.getId().split("/")[1];
                project.setCode(projectCode);
                project.setSpace(spaces.get(new SpacePermId(entry.getId().split("/")[1])));
                ProjectIdentifier identifier = new ProjectIdentifier(spaceCode, projectCode);
                project.setIdentifier(identifier);

                projects.put(identifier, project);

            }

        }
    }

    private static void mapSpaces(String fallbackSpaceCode, String fallbackProjectCode,
            Map<SpacePermId, Space> spaces, Map<ProjectIdentifier, Project> projects)
    {
        SpacePermId spacePermId = new SpacePermId(fallbackSpaceCode);

        if (!spaces.containsKey(new SpacePermId(fallbackSpaceCode)))
        {
            Space space = new Space();
            space.setPermId(spacePermId);
            space.setCode(spacePermId.getPermId());
            spaces.put(spacePermId, space);
        }
        ProjectIdentifier projectIdentifier =
                new ProjectIdentifier(fallbackSpaceCode, fallbackProjectCode);
        if (!projects.containsKey(projectIdentifier))
        {
            Project project = new Project();

            ProjectFetchOptions fetchOptions = new ProjectFetchOptions();
            fetchOptions.withSpace();
            project.setFetchOptions(fetchOptions);

            project.setSpace(spaces.get(new SpacePermId(fallbackSpaceCode)));
            project.setIdentifier(projectIdentifier);
            project.setCode(fallbackProjectCode);
            projects.put(projectIdentifier, project);

        }
    }

    private static void mapProjects(Map<ProjectIdentifier, Project> projects,
            Map<SpacePermId, Space> spaces)
    {
        for (Project project : projects.values())
        {
            SpacePermId identifier =
                    new SpacePermId(project.getIdentifier().getIdentifier().split("/")[1]);
            project.setSpace(spaces.get(identifier));
        }
    }

    private static void mapCollections(List<IMetadataEntry> entries,
            Map<String, List<String>> typeToInheritanceChain,
            Map<String, ExperimentType> identifierToCollectionType,
            Map<ExperimentIdentifier, Experiment> idsToCollections)
    {
        for (IMetadataEntry entry : entries)
        {
            Optional<EntityKind> entityKind =
                    matchEntityKind(entry, typeToInheritanceChain);


            if (entityKind.filter(x -> x == EntityKind.EXPERIMENT).isPresent())
            {
                Experiment experiment = new Experiment();
                ExperimentType experimentType = identifierToCollectionType.get(
                        entry.getTypes().stream().findFirst().orElseThrow());
                experiment.setType(experimentType);
                ExperimentIdentifier identifier = new ExperimentIdentifier(entry.getId());
                experiment.setIdentifier(identifier);
                experimentType.setCode(entry.getId().split("/")[3]);
                idsToCollections.put(identifier, experiment);

            }

        }
    }

    private static void resolveSpaceProjectAndCollections(
            List<Pair<Sample, ReferencesToResolve>> samplesWithSpaceAndProjectCodes,
            Map<SpacePermId, Space> spaces, Map<ProjectIdentifier, Project> projects,
            Map<ExperimentIdentifier, Experiment> idsToCollections,
            Map<ObjectIdentifier, AbstractEntityPropertyHolder> metadata)
    {
        for (Pair<Sample, ReferencesToResolve> sampleToResolve : samplesWithSpaceAndProjectCodes)
        {
            Space space = spaces.get(new SpacePermId(sampleToResolve.getRight().getSpaceCode()));
            Project project = projects.get(
                    new ProjectIdentifier(space.getCode(),
                            sampleToResolve.getRight().getProjectCode()));
            sampleToResolve.getLeft().setSpace(space);
            sampleToResolve.getLeft().setProject(project);
            sampleToResolve.getLeft().setExperiment(idsToCollections.get(new ExperimentIdentifier(
                    "/" + space.getCode() + "/" + project.getCode() + "/" + sampleToResolve.getRight().collectionCode)));
            ObjectIdentifier objectIdentifier = new SampleIdentifier(
                    "/" + sampleToResolve.getLeft().getSpace()
                            .getCode() + "/" + sampleToResolve.getLeft().getProject()
                            .getCode() + "/" + sampleToResolve.getLeft().getCode());
            metadata.put(objectIdentifier, sampleToResolve.getLeft());


        }
    }

    private static void resolveOpenBisStructure(List<IMetadataEntry> entries,
            String fallbackSpaceCode,
            String fallbackProjectCode, Map<String, List<String>> typeToInheritanceChain,
            Map<String, Sample> roCrateIdsToObjects, Map<SpacePermId, Space> spaces,
            Map<ProjectIdentifier, Project> projects)
    {
        for (IMetadataEntry entry : entries)
        {
            Optional<EntityKind> entityKind =
                    matchEntityKind(entry, typeToInheritanceChain);

            if (entityKind.filter(x -> x == EntityKind.SAMPLE).isPresent())
            {
                Sample sample = roCrateIdsToObjects.get(entry.getId());

                // resolving object references needs another pass after creating all objects
                for (Map.Entry<String, List<String>> reference : entry.getReferences().entrySet())
                {
                    if (requiresSpecialHandling(reference.getKey()))
                    {
                        continue;
                    }

                    sample.getProperties().put(openBisifyCode(deRdfIdentifier(reference.getKey())),
                            String.join(",",
                                    reference.getValue().stream()
                                            .map(x -> roCrateIdsToObjects.get(x))
                                            .filter(Objects::nonNull)
                                            .map(x -> mapIdentifier(fallbackSpaceCode,
                                                    fallbackProjectCode, spaces, projects,
                                                    x))
                                            .collect(
                                                    Collectors.toList())));
                }
            }
        }
    }

    private static SampleTypeFetchOptions getSampleTypeFetchOptions()
    {
        SampleTypeFetchOptions sampleTypeFetchOptions = new SampleTypeFetchOptions();
        sampleTypeFetchOptions.withSemanticAnnotations();
        sampleTypeFetchOptions.withPropertyAssignments();
        return sampleTypeFetchOptions;
    }

    private static PropertyAssignment getPropertyAssignment(PropertyType propertyType,
            SampleType sampleType, boolean mandatory, boolean multiValued)
    {
        PropertyAssignment curProperty = new PropertyAssignment();
        curProperty.setPropertyType(propertyType);

        {
            PropertyAssignmentFetchOptions fetchOptions1 =
                    new PropertyAssignmentFetchOptions();
            fetchOptions1.withPropertyType();
            fetchOptions1.withEntityType();
            fetchOptions1.withSemanticAnnotations();

            curProperty.setFetchOptions(fetchOptions1);

        }
        curProperty.setSemanticAnnotations(new ArrayList<>());

        curProperty.setMandatory(mandatory);
        propertyType.setMultiValue(multiValued);

        curProperty.setPermId(new PropertyAssignmentPermId(sampleType.getPermId(),
                propertyType.getPermId()));
        return curProperty;
    }

    private static ReferencesToResolve buildEntryWithSpaceAndProjectToResolve(
            Map<String, List<String>> properties, String spaceCode, String projectCode,
            String defaultExperimentCode)
    {

        String mySpace = Optional.ofNullable(properties.get(PROPERTY_SPACE)).map(x -> x.get(0))
                .orElse(spaceCode);
        String myProject =
                Optional.ofNullable(properties.get(PROPERTY_PROJECT)).map(x -> x.get(0))
                        .orElse(spaceCode);
        String myExperiment =
                Optional.ofNullable(properties.get(Constants.PROPERTY_COLLECTION)).map(
                                Object::toString)
                        .map(x -> x.split("/"))
                        .map(x -> x[3])
                        .map(x -> x.replaceAll("]$", ""))
                        .orElse(defaultExperimentCode);

        myProject =
                Optional.ofNullable(properties.get(Constants.PROPERTY_COLLECTION)).map(
                                Object::toString)
                        .map(x -> x.split("/"))
                        .map(x -> x[2])
                        .map(x -> x.replaceAll("]$", ""))
                        .orElse(myProject);

        return new ReferencesToResolve(mySpace, myProject, myExperiment);
    }



    private static Optional<EntityKind> matchEntityKind(IMetadataEntry metfadataEntry,
            Map<String, List<String>> typeToInheritanceChain)
    {
        List<String> a =
                metfadataEntry.getTypes().stream().map(x -> typeToInheritanceChain.get(x))
                        .filter(Objects::nonNull).flatMap(Collection::stream)
                        .collect(Collectors.toList());

        if (a.isEmpty())
        {
            return Optional.empty();
        }

        if (a.stream().anyMatch(GRAPH_ID_Collection::equals))
        {
            return Optional.of(EntityKind.EXPERIMENT);
        }
        return Optional.of(EntityKind.SAMPLE);
    }

    private static String deRdfIdentifier(String a)
    {
        Pattern patternBool = Pattern.compile("^is");
        Pattern patternRest = Pattern.compile("^has");

        Pattern prefix = Pattern.compile("");

        a = a.replaceFirst("^[a-zA-Z0-9]*:", "");

        if (a.startsWith("_"))
        {
            a = a.replaceFirst("_", "");
        }
        if (a.startsWith(":"))
        {
            a = a.replaceFirst(":", "");
        }


        if (patternBool.matcher(a).find())
        {
            return a.replaceAll("^is", "");
        }
        if (patternRest.matcher(a).find())
        {
            return a.replaceAll("^has", "");
        }

        return a;

    }

    private static Set<DataType> matchDataTypes(IPropertyType propertyType)
    {
        return propertyType.getRange().stream().map(RdfToModel::matchDataType)
                .collect(Collectors.toSet());

    }

    private static DataType matchDataType(String rangeId)
    {

        if (rangeId.equals(LiteralType.STRING.getTypeName()))
        {
            return DataType.VARCHAR;
        }
        if (rangeId.equals(LiteralType.BOOLEAN.getTypeName()))
        {
            return DataType.BOOLEAN;
        }
        if (rangeId.equals(LiteralType.DECIMAL.getTypeName()))
        {
            return DataType.REAL;
        }
        if (rangeId.equals(LiteralType.INTEGER.getTypeName()))
        {
            return DataType.INTEGER;
        }
        if (rangeId.equals(LiteralType.DATETIME.getTypeName()))
        {
            return DataType.DATE;
        }
        if (rangeId.equals(LiteralType.ANY_URI.getTypeName()))
        {
            return DataType.HYPERLINK;
        }
        if (rangeId.equals(LiteralType.XML_LITERAL))
        {
            return DataType.XML;
        }

        return DataType.SAMPLE;

    }

    private static List<IType> getInheritanceChain(IType type, Map<String, IType> idsToType)
    {
        IType cur = type;
        Set<String> closed = new HashSet<>();
        Queue<String> candidates = new ArrayDeque<>();

        List<String> idChain = new ArrayList<>();
        while (cur != null && cur.getSubClassOf() != null)
        {
            idChain.add(cur.getId());
            closed.add(cur.getId());
            for (String next : cur.getSubClassOf())
            {
                if (!closed.contains(next))
                {
                    candidates.add(next);
                }
            }
            cur = idsToType.get(candidates.poll());
        }
        return idChain.stream().map(idsToType::get).collect(Collectors.toList());
    }

    private static EntityKind matchKind(List<IType> inheritanceChain)
    {
        if (inheritanceChain.stream()
                .anyMatch(x -> x.getId().equals(Constants.GRAPH_ID_Collection)))
        {
            return EntityKind.EXPERIMENT;
        }
        return EntityKind.SAMPLE;

    }

    private static boolean isSpace(IType type)
    {
        return type.getId().equals(GRAPH_ID_SPACE);
    }

    private static boolean isCollection(IType type)
    {
        return type.getSubClassOf().contains(GRAPH_ID_Collection);
    }


    private static boolean isProject(IType type)
    {
        return type.getId().equals(GRAPH_ID_PROJECT);
    }

    private static boolean isSample(IType type)
    {
        return type.getSubClassOf().stream().anyMatch(GRAPH_ID_OBJECT::equals);
    }

    private static boolean isOpenBisDerivedType(IType type)
    {
        if (isSpace(type))
        {
            return false;
        }
        if (isProject(type))
        {
            return false;
        }
        if (type.getId().equals(GRAPH_ID_DATASET))
        {
            return false;
        }
        if (type.getId().equals(GRAPH_ID_Collection))
        {
            return false;
        }
        if (type.getId().equals(GRAPH_ID_OBJECT))
        {
            return false;
        }
        if (type.getId().equals(GRAPH_ID_VOCABULARY))
        {
            return false;
        }
        return true;

    }

    private static boolean requiresSpecialHandling(IPropertyType typeProperty)
    {
        if (deRdfIdentifier(typeProperty.getId()).toUpperCase(Locale.ROOT).equals("NAME"))
        {
            return true;
        }
        if (deRdfIdentifier(typeProperty.getId()).toUpperCase(Locale.ROOT).equals("SPACE"))
        {
            return true;
        }
        if (deRdfIdentifier(typeProperty.getId()).toUpperCase(Locale.ROOT).equals("PROJECT"))
        {
            return true;
        }
        if (deRdfIdentifier(typeProperty.getId()).toUpperCase(Locale.ROOT).equals("COLLECTION"))
        {
            return true;
        }
        return false;

    }

    private static boolean requiresSpecialHandling(String identifier)
    {
        if (deRdfIdentifier(identifier).toUpperCase(Locale.ROOT).equals("SPACE"))
        {
            return true;
        }
        if (deRdfIdentifier(identifier).toUpperCase(Locale.ROOT).equals("PROJECT"))
        {
            return true;
        }
        if (deRdfIdentifier(identifier).toUpperCase(Locale.ROOT).equals("COLLECTION"))
        {
            return true;
        }
        return false;

    }


    private static String removePrefix(String a)
    {

        return a.replaceFirst("^:", "").replaceFirst("^_:", "");
    }

    private static class PropertyTypeMapping
    {
        String oldIdentifier;

        String newIdentifier;

        DataType dataType;

        public PropertyTypeMapping(String oldIdentifier, String newIdentifier, DataType dataType)
        {
            this.oldIdentifier = oldIdentifier;
            this.newIdentifier = newIdentifier;
            this.dataType = dataType;
        }
    }

    private static class ReferencesToResolve
    {
        String spaceCode;

        String projectCode;

        String collectionCode;

        public ReferencesToResolve(String spaceCode, String projectCode, String collectionCode)
        {
            this.spaceCode = spaceCode;
            this.projectCode = projectCode;
            this.collectionCode = collectionCode;
        }

        public String getSpaceCode()
        {
            return spaceCode;
        }

        public String getProjectCode()
        {
            return projectCode;
        }

    }

    private static String getIntersectionTypeIdentifier(Set<String> types)
    {
        return String.join("_", types);

    }

    private static String suffixCodeType(String code, DataType dataType)
    {
        return code + dataType.name().toUpperCase();
    }

    private static String openBisifyCode(String code)
    {
        return code.replaceAll(":", "_");
    }

    private static String createSampleCode(SampleType sampleType, String identifier)
    {
        boolean isUrl = false;
        try
        {
            URL url = new URL(identifier);
            isUrl = true;

        } catch (MalformedURLException ignored)
        {
        }
        if (isUrl)
        {
            String[] parts = identifier.split("/");
            return OpenBisModel.makeOpenBisCodeCompliant(
                    sampleType.getCode() + "_" + parts[parts.length - 1]);
        }
        if (DataTypeMatcher.matches(identifier, DataType.SAMPLE))
        {
            String[] parts = identifier.split("/");
            return parts[parts.length - 1];
        }


        return OpenBisModel.makeOpenBisCodeCompliant(identifier);

    }

    private static String mapIdentifier(String fallbackSpace, String fallBackProject,
            Map<SpacePermId, Space> spaces, Map<ProjectIdentifier, Project> projects, Sample sample)
    {

        String spaceCode =
                Optional.ofNullable(sample.getSpace()).map(Space::getCode).orElse(fallbackSpace);
        String projectCode = Optional.ofNullable(sample.getProject()).map(Project::getCode)
                .orElse(fallbackSpace);

        return "/" + spaceCode + "/" + projectCode + "/" + sample.getCode();


    }

    private static String getCollectionTypeCode(IType type)
    {
        String[] split = type.getId().split(":");
        return split[split.length - 1].toUpperCase();

    }

}
