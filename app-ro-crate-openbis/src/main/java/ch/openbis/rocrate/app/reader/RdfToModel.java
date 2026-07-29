package ch.openbis.rocrate.app.reader;

import ch.eth.sis.rocrate.SchemaFacade;
import ch.eth.sis.rocrate.facade.*;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.entity.AbstractEntityPropertyHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.id.ObjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IEntityType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.ExperimentType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
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
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.SemanticAnnotation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import ch.ethz.sis.openbis.generic.excel.v3.model.IFileInfo;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.openbis.rocrate.app.Constants;
import ch.openbis.rocrate.app.reader.helper.*;
import ch.openbis.rocrate.app.writer.mapping.images.ImageExtractor;
import edu.kit.datamanager.ro_crate.entities.AbstractEntity;
import edu.kit.datamanager.ro_crate.entities.data.DataEntity;
import jakarta.annotation.Nullable;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ch.ethz.sis.rdf.main.mappers.openBis.ValueMapper.CANONICAL_DATE_FORMAT_PATTERN;
import static ch.openbis.rocrate.app.Constants.*;

public class RdfToModel
{

    public record ConversionResult(OpenBisModel openBisModel,
                                   Map<String, List<FileProblem>> identfiersOfMissingFiles,
                                   List<MissingReferenceValue> missingReferenceValues)
    {
    }

    public record MissingReferenceValue(String oldIdentifier, Sample sample, String key)
    {
    }


    public record FileProblem(String type, String path)
    {

    }

    public static ConversionResult convert(List<IType> rdfsTypes,
            List<IPropertyType> rdfsTypeProperties,
            List<IMetadataEntry> metadataEntries, String fallbackSpaceCode,
            String fallbackProjectCode,
            SchemaFacade schemaFacade, Map<AbstractEntity, Path> identifiersToExternalFiles)
            throws IOException
    {
        List<IType> types = rdfsTypes.stream().sorted(Comparator.comparing(IType::getId)).toList();
        List<IMetadataEntry> entries =
                metadataEntries.stream().sorted(Comparator.comparing(IMetadataEntry::getId))
                        .toList();
        List<IPropertyType> typeProperties =
                rdfsTypeProperties.stream().sorted(Comparator.comparing(IPropertyType::getId))
                        .toList();



        Set<SampleType> openBisDerivedTypes = new LinkedHashSet<>();


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

        Map<ObjectIdentifier, List<IFileInfo>> objectIdentifiersTOImageFiles =
                new LinkedHashMap<>();

        Map<ExperimentIdentifier, Experiment> idsToCollections = new LinkedHashMap<>();
        Map<ObjectIdentifier, List<IFileInfo>> objectIdentifiersToFiles =
                new LinkedHashMap<>();

        Map<EntityTypePermId, IEntityType> schema = new LinkedHashMap<>();
        handleTypes(types, IdsToTypes, typeToInheritanceChain, entityTypeToRdfIdentifier,
                codeToSampleType,
                schema, identifierToCollectionType, openBisDerivedTypes);

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
        Map<String, Sample> externalIdentifierToSample = new LinkedHashMap<>();

        Map<ObjectIdentifier, AbstractEntityPropertyHolder> metadata = new LinkedHashMap<>();
        Map<String, IMetadataEntry> idToEntities =
                entries.stream().collect(Collectors.toMap(x -> x.getId(), x -> x, (x, y) -> y));
        EntityProcessingresult entityProcessingResult =
                processEntities(schema, entityTypeToRdfIdentifier, entries, fallbackSpaceCode,
                        fallbackProjectCode,
                        typeToInheritanceChain,
                        codeToSampleType,
                        externalIdentifierToSample, baseCodeToPossibleDataTypes, idToEntities,
                        roCrateIdsToObjects,
                        samplesWithSpaceAndProjectCodes, spaces, projects, objectIdentifiersToFiles,
                        objectIdentifiersTOImageFiles,
                        identifiersToExternalFiles);
        List<AbstractEntityPropertyHolder> abstractEntityPropertyHolders =
                entityProcessingResult.abstractEntityPropertyHolders();

        mapSpaces(fallbackSpaceCode, fallbackProjectCode, spaces, projects);
        mapProjects(projects, spaces);

        mapCollections(schema, entityTypeToRdfIdentifier, entries, typeToInheritanceChain,
                identifierToCollectionType,
                idsToCollections);

        resolveSpaceProjectAndCollections(samplesWithSpaceAndProjectCodes, spaces, projects,
                idsToCollections, fallbackProjectCode, fallbackSpaceCode);

        List<MissingReferenceValue> missingReferenceValues =
                resolveOpenBisStructure(schema, entityTypeToRdfIdentifier, entries,
                        fallbackSpaceCode,
                        fallbackProjectCode,
                        typeToInheritanceChain,
                        roCrateIdsToObjects, spaces, projects,
                        entityProcessingResult.objectReferenceToResolves);

        resolveSamples(samplesWithSpaceAndProjectCodes, externalIdentifierToSample);

        metadata =
                abstractEntityPropertyHolders.stream().collect(Collectors.toMap(x ->
                        {
                            if (x instanceof Sample)
                            {
                                return (((Sample) x).getIdentifier());

                            }
                            if (x instanceof Experiment)
                            {
                                return (((Experiment) x).getIdentifier());

                            }
                            throw new RuntimeException();
                        }

                        , x -> x, (x, y) -> y, LinkedHashMap::new));

        Map<String, List<FileProblem>> identifierToMissingFile = new LinkedHashMap<>();
        for (IMetadataEntry entry : entries)
        {
            Sample sample = externalIdentifierToSample.get(entry.getId());
            if (sample == null)
            {
                continue;
            }

            List<FileProblem> missingFileIdentifiers =
                    handleFiles(entry, objectIdentifiersToFiles, objectIdentifiersTOImageFiles,
                            sample,
                            identifiersToExternalFiles, schemaFacade);
            identifierToMissingFile.put(entry.getId(), missingFileIdentifiers);
        }

        Map<String, String> collect = externalIdentifierToSample.entrySet().stream()
                .collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue().getCode()));

        OpenBisModel openBisModel =
                new OpenBisModel(Map.of(), schema, spaces, projects, metadata, Map.of(), Map.of(),
                        collect,
                        objectIdentifiersToFiles, objectIdentifiersTOImageFiles);
        PropertyTypePruning.prune(openBisModel.getEntities());

        return new ConversionResult(openBisModel, identifierToMissingFile, missingReferenceValues);
    }

    private static void handleTypes(List<IType> types, Map<String, IType> IdsToTypes,
            Map<String, List<String>> typeToInheritanceChain,
            Map<String, EntityTypePermId> entityTypeToRdfIdentifier,
            Map<String, SampleType> codeToSampleType, Map<EntityTypePermId, IEntityType> schema,
            Map<String, ExperimentType> identifierToCollectionType,
            Set<SampleType> openBisDerivedTypes)
    {
        for (IType type : types)
        {
            if (isProject(type) || isSpace(type))
            {
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

                if (inheritanceChain.stream().anyMatch(x -> x.getId().equals(GRAPH_ID_OBJECT)))
                {
                    openBisDerivedTypes.add(sampleType);
                }

                if (!isCollection(type) && (isOpenBisDerivedType(type) || isSample(type)))
                {
                    schema.put(sampleType.getPermId(), sampleType);
                }
                type.getOntologicalAnnotations().forEach(x -> {
                    SemanticAnnotation semanticAnnotation = new SemanticAnnotation();
                    semanticAnnotation.setPredicateAccessionId(x);
                    semanticAnnotation.setPredicateOntologyVersion(x);
                    if (x.contains("https://schema.org"))
                    {
                        semanticAnnotation.setPredicateOntologyId("https://schema.org");
                    }

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
            if (PropertyTypeSpecialHandling.requiresFileHandling(a))
            {
                continue;
            }

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
                propertyType.setLabel(a.getId());

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
            List<SemanticAnnotation> annotations = new ArrayList<>();

            List<PropertyAssignment> assignments = new ArrayList<>();
            List<SemanticAnnotation> semanticAnnotations = new ArrayList<>();

            for (String type : intersectionType)
            {

                IEntityType entityType = tryFind(schema, entityTypeToRdfIdentifier, type);
                if (entityType == null)
                {
                    continue;
                }
                SampleType sampleType1 =
                        (SampleType) entityType;
                if (sampleType1.getSemanticAnnotations() != null)
                {
                    sampleType1.getSemanticAnnotations().stream().forEach(annotations::add);
                }

                for (PropertyAssignment propertyAssignment : sampleType1.getPropertyAssignments())
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
            List<SemanticAnnotation> deduplicatedAnnotations = deduplicateAnnotations(annotations);
            if (deduplicatedAnnotations.size() == 1)
            {
                // Only adding 1 annotation to guard against transitive equivalences for people who use owl reasoners
                sampleType.setSemanticAnnotations(deduplicatedAnnotations);
            }
        }
    }

    /**
     * Deduplication logic, the ontology field be null, therefore standard deduplication does not
     * work.
     *
     * @param annotations
     * @return
     */
    private static List<SemanticAnnotation> deduplicateAnnotations(
            List<SemanticAnnotation> annotations)
    {
        Set<String> accessions = new LinkedHashSet<>();
        List<SemanticAnnotation> res = new ArrayList<>();
        for (SemanticAnnotation semanticAnnotation : annotations)
        {
            if (!accessions.contains(semanticAnnotation.getPredicateAccessionId()))
            {
                accessions.add(semanticAnnotation.getPredicateAccessionId());
                res.add(semanticAnnotation);
            }
        }
        return res;
    }

    ;

    private static IEntityType tryFind(Map<EntityTypePermId, IEntityType> schema,
            Map<String, EntityTypePermId> entityTypeToRdfIdentifier, String type)
    {
        IEntityType entityType = schema.get(entityTypeToRdfIdentifier.get(type));
        if (entityType != null)
        {
            return entityType;
        }
        if (!type.contains(":"))
        {
            return schema.get(entityTypeToRdfIdentifier.get("schema:" + type));

        }
        return null;

    }

    private static String tryFindRdfIdentifier(Map<EntityTypePermId, IEntityType> schema,
            Map<String, EntityTypePermId> entityTypeToRdfIdentifier, String type)
    {
        if (type.contains(":"))
        {
            return type;
        }
        return "schema:" + type;

    }

    record ObjectReferenceToResolve(Sample sample, String key, String value)
    {
    }

    record EntityProcessingresult(List<AbstractEntityPropertyHolder> abstractEntityPropertyHolders,
                                  List<ObjectReferenceToResolve> objectReferenceToResolves)
    {
    }

    private static EntityProcessingresult processEntities(
            Map<EntityTypePermId, IEntityType> schema,
            Map<String, EntityTypePermId> entityTypeToRdfIdentifier, List<IMetadataEntry> entries,
            String fallbackSpaceCode,
            String fallbackProjectCode, Map<String, List<String>> typeToInheritanceChain,
            Map<String, SampleType> codeToSampleType,
            Map<String, Sample> externalIdentifierToSample,
            Map<String, Set<DataType>> baseCodeToPossibleDataTypes,
            Map<String, IMetadataEntry> idToEntities, Map<String, Sample> roCrateIdsToObjects,
            List<Pair<Sample, ReferencesToResolve>> samplesWithSpaceAndProjectCodes,
            Map<SpacePermId, Space> spaces, Map<ProjectIdentifier, Project> projects,
            Map<ObjectIdentifier, List<IFileInfo>> objectIdentifiersToFiles,
            Map<ObjectIdentifier, List<IFileInfo>> images,
            Map<AbstractEntity, Path> identifiersToExternalFiles)
            throws IOException
    {
        List<AbstractEntityPropertyHolder> res = new ArrayList<>();
        List<ObjectReferenceToResolve> objectReferencesToResolve = new ArrayList<>();

        for (IMetadataEntry entry : entries)
        {
            AbstractEntityPropertyHolder entity;
            ObjectIdentifier objectIdentifier;

            Optional<EntityKind> entityKind =
                    matchEntityKind(schema, entityTypeToRdfIdentifier, entry,
                            typeToInheritanceChain);

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
                    fetchOptions.withParents();
                    fetchOptions.withChildren();
                    sample.setFetchOptions(fetchOptions);
                }
                String typeCode = entry.getTypes().size() == 1 ?
                        entry.getTypes().stream().findFirst().orElseThrow() :
                        getIntersectionTypeIdentifier(entry.getTypes());

                SampleType type =
                        Optional.ofNullable(codeToSampleType.get(openBisifyCode(typeCode)))
                                .orElse(codeToSampleType.get(openBisifyCode("schema:" + typeCode)));
                sample.setType(type);

                String code = SampleCodeHelper.createSampleCode(type, entry.getId());
                sample.setCode(code);
                externalIdentifierToSample.put(entry.getId(), sample);

                externalIdentifierToSample.get(entry.getId());
                objectIdentifier = sample.getIdentifier();
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

                        Serializable valueToPut =
                                handlePossibleMultiValues(property.getValue(), dataType);


                        properties.put(DataTypeMatcher.suffixTypeCode(key, dataType),
                                valueToPut);

                    } else
                    {
                        DataType dataType =

                                Optional.ofNullable(baseCodeToPossibleDataTypes.get(key))
                                        .map(x -> x.stream().findFirst().orElseThrow())
                                        .orElse(DataType.MULTILINE_VARCHAR);
                        Serializable valueToPut =
                                handlePossibleMultiValues(property.getValue(), dataType);

                        properties.put(key,
                                valueToPut);
                    }
                }

                for (Map.Entry<String, List<String>> property : entry.getReferences().entrySet())
                {
                    for (String value : property.getValue())
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
                                    DataTypeMatcher.findDataType(value, dataTypes,
                                            idToEntities);

                            Serializable valueToPut =
                                    handlePossibleMultiValues(value, dataType);
                            String suffixedTypeCode = DataTypeMatcher.suffixTypeCode(key, dataType);

                            if (dataType == DataType.SAMPLE)
                            {
                                objectReferencesToResolve.add(
                                        new ObjectReferenceToResolve(sample, suffixedTypeCode,
                                                value));

                            } else
                            {

                                properties.put(suffixedTypeCode,
                                        valueToPut);
                            }
                        } else
                        {
                            DataType dataType =

                                    Optional.ofNullable(baseCodeToPossibleDataTypes.get(key))
                                            .map(x -> x.stream().findFirst().orElseThrow())
                                            .orElse(DataType.MULTILINE_VARCHAR);

                            if (dataType == DataType.SAMPLE)
                            {
                                objectReferencesToResolve.add(
                                        new ObjectReferenceToResolve(sample, key, value));

                            } else
                            {
                                Serializable valueToPut =
                                        handlePossibleMultiValues(value, dataType);

                                properties.put(key,
                                        valueToPut);
                            }

                        }


                    }
                }

                roCrateIdsToObjects.put(entry.getId(), sample);

                if (!properties.containsKey("NAME"))
                {
                    properties.put("NAME",
                            sample.getCode()); // We need a name to construct certain paths inside the zip
                }
                sample.setProperties(properties);

                properties.get("SPACE");
                ReferencesToResolve referencesToResolve =
                        buildEntryWithSpaceAndProjectToResolve(entry);
                samplesWithSpaceAndProjectCodes.add(
                        new ImmutablePair<>(sample, referencesToResolve));
                res.add(entity);

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

            } else if (entry.getTypes().contains(GRAPH_ID_Collection))
            {
                ExperimentIdentifier identifier = new ExperimentIdentifier(entry.getId());
                objectIdentifier = identifier;
                handleFilesExperiment(entry, objectIdentifier, objectIdentifiersToFiles);
                Experiment experiment = new Experiment();
                {
                    ExperimentFetchOptions experimentFetchOptions = new ExperimentFetchOptions();
                    experimentFetchOptions.withProject();
                    experiment.setFetchOptions(experimentFetchOptions);
                }

                experiment.setIdentifier(identifier);

                res.add(experiment);

            }

        }
        return new EntityProcessingresult(res, objectReferencesToResolve);
    }

    private static List<FileProblem> handleFiles(IMetadataEntry metadataEntry,
            Map<ObjectIdentifier, List<IFileInfo>> res,
            Map<ObjectIdentifier, List<IFileInfo>> richTextImageFiles, Sample sample,
            Map<AbstractEntity, Path> identifiersToExternalFiles, SchemaFacade schemaFacade)
            throws IOException
    {

        List<FileProblem> identifiersWithMissingFiles = new ArrayList<>();
        List<OpenBisModel.FileInfoPath> myRes = new ArrayList<>();

        List<OpenBisModel.FileInfoPath> finalMyRes = myRes;
        metadataEntry.getFileOrDirectory().ifPresent(x -> {
            OpenBisModel.FileInfoPath fileInfo =
                    new OpenBisModel.FileInfoPath(sample.getIdentifier().toString(), x.toString(),
                            x, metadataEntry.getId());
                finalMyRes.add(fileInfo);

        });

        DirectoryTraversal directoryTraversal =
                new DirectoryTraversal();
        DirectoryTraversal.TraversalResult traversalResult =
                directoryTraversal.findAllFiles(metadataEntry.getId(),
                        schemaFacade.getCrate(), sample);
        List<AbstractEntity> allFiles =
                traversalResult.files();
        identifiersWithMissingFiles.addAll(traversalResult.missingEntitites());

        for (AbstractEntity a : allFiles)
        {
            Path downloadedPath = identifiersToExternalFiles.get(a);

            if (a instanceof DataEntity && ((DataEntity) a).getPath() != null)
            {
                DataEntity dataEntity = (DataEntity) a;
                OpenBisModel.FileInfoPath fileInfo =
                        new OpenBisModel.FileInfoPath(sample.getIdentifier().toString(),
                                dataEntity.getPath().toString(), dataEntity.getPath(), a.getId());
                myRes.add(fileInfo);
            } else if (downloadedPath != null)
            {
                OpenBisModel.FileInfoPath fileInfoPath =
                        new OpenBisModel.FileInfoPath(sample.getIdentifier().toString(), a.getId(),
                                downloadedPath, a.getId());
                myRes.add(fileInfoPath);
            } else
            {
                identifiersWithMissingFiles.add(new FileProblem("File", a.getId()));
            }

        }
        Set<String> multiLineVarcharProperties =
                sample.getType().getPropertyAssignments().stream().map(x -> x.getPropertyType())
                        .filter(x -> x.getDataType() == DataType.MULTILINE_VARCHAR)
                        .map(x -> x.getCode())
                        .collect(Collectors.toSet());

        myRes.stream().collect(Collectors.toMap(x -> x.filePath(), x -> x));
        Map<String, String> images = new LinkedHashMap<>();

        for (Map.Entry<String, Serializable> entry : sample.getProperties().entrySet())
            {
                if (!multiLineVarcharProperties.contains(entry.getKey()))
                {
                    continue;
                }
                Serializable[] vals;
                if (entry.getValue() instanceof Serializable[])
                {
                    vals = (Serializable[]) entry.getValue();
                } else
                {
                    vals = new Serializable[] { entry.getValue() };
                }

                Serializable writeVal = vals[0];
                for (Serializable value : vals)
                {
                    ImageExtractor.ValueAndImages imageRes =
                            ImageExtractor.findImageAndUpdatePaths(value);
                    Map<String, String> collect = Stream.concat(images.entrySet().stream(),
                            imageRes.images().entrySet().stream()).collect(
                            Collectors.toMap(x -> x.getKey(), x -> x.getValue()));
                    images.putAll(collect);
                    writeVal = imageRes.value();
                }
                sample.getProperties().put(entry.getKey(), writeVal);



        }
        myRes.addAll(finalMyRes);
        HashMap<String, String> compareMap = new HashMap<>(images);
        List<IFileInfo> fileRes =
                myRes.stream().distinct().filter(x -> !isImageMatch(x, compareMap))
                .collect(Collectors.toList());
        List<IFileInfo> imageRes =
                myRes.stream().distinct().filter(x -> isImageMatch(x, compareMap))
                        .map(x -> new OpenBisModel.FileInfoPath(x.objectIdentifier(), x.filePath(),
                                x.readPath(), compareMap.get(x.originalPath())))
                .collect(Collectors.toList());
        ;

        res.put(sample.getIdentifier(), fileRes);

        richTextImageFiles.put(sample.getIdentifier(), imageRes);
        return identifiersWithMissingFiles;
    }

    private static void handleFilesExperiment(IMetadataEntry metadataEntry,
            ObjectIdentifier objectIdentifier,
            Map<ObjectIdentifier, List<IFileInfo>> res)
            throws IOException
    {

        List<IFileInfo> myRes = new ArrayList<>();

        metadataEntry.getFileOrDirectory().ifPresent(x -> {

            IFileInfo fileInfo =
                    new OpenBisModel.FileInfoPath(objectIdentifier.getIdentifier(), x.toString(), x,
                            metadataEntry.getId());
            myRes.add(fileInfo);



        });
        for (DataEntity a : metadataEntry.getDataEntitiesReferenced())
        {
            if (a.getPath() != null)
            {
                IFileInfo fileInfo =
                        new OpenBisModel.FileInfoPath(objectIdentifier.getIdentifier(),
                                a.getPath().toString(), a.getPath(), a.getId());
                myRes.add(fileInfo);
            }

        }
        LinkedHashMap<Object, Object> compareMap = new LinkedHashMap<>();
        List<IFileInfo> fileRes = myRes;
        ;

        res.put(objectIdentifier, fileRes);

    }

    private static boolean isImageMatch(IFileInfo x, Map<String, String> images)
    {
        return images.keySet().stream()
                .anyMatch(y -> x.filePath().endsWith(y.replace("file-service/eln-lims", "")));
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

    private static void mapCollections(Map<EntityTypePermId, IEntityType> schema,
            Map<String, EntityTypePermId> entityTypeToRdfIdentifier, List<IMetadataEntry> entries,
            Map<String, List<String>> typeToInheritanceChain,
            Map<String, ExperimentType> identifierToCollectionType,
            Map<ExperimentIdentifier, Experiment> idsToCollections)
    {
        for (IMetadataEntry entry : entries)
        {
            Optional<EntityKind> entityKind =
                    matchEntityKind(schema, entityTypeToRdfIdentifier, entry,
                            typeToInheritanceChain);


            if (entityKind.filter(x -> x == EntityKind.EXPERIMENT).isPresent())
            {
                Experiment experiment = new Experiment();
                ExperimentType experimentType = identifierToCollectionType.get(
                        entry.getTypes().stream().findFirst().orElseThrow());
                experiment.setType(experimentType);
                ExperimentIdentifier identifier = new ExperimentIdentifier(entry.getId());
                experiment.setIdentifier(identifier);
                experiment.setCode(entry.getId().split("/")[3]);
                idsToCollections.put(identifier, experiment);

            }

        }
    }

    private static void resolveSpaceProjectAndCollections(
            List<Pair<Sample, ReferencesToResolve>> samplesWithSpaceAndProjectCodes,
            Map<SpacePermId, Space> spaces, Map<ProjectIdentifier, Project> projects,
            Map<ExperimentIdentifier, Experiment> idsToCollections,

            String fallbacbProjectCode, String fallbackSpaceCode)
    {
        for (Pair<Sample, ReferencesToResolve> sampleToResolve : samplesWithSpaceAndProjectCodes)
        {

            OpenBisStructureHelper.Structure structure =
                    OpenBisStructureHelper.findStructure(spaces, projects, idsToCollections,
                            sampleToResolve, fallbacbProjectCode, fallbackSpaceCode);

            if (structure.space() != null)
            {
                sampleToResolve.getLeft().setSpace(structure.space());
            }

            if (structure.project() != null)
            {
                sampleToResolve.getLeft().setProject(structure.project());

            }
            if (structure.experiment() != null)
            {
                sampleToResolve.getLeft().setExperiment(structure.experiment());

            }
            sampleToResolve.getLeft().setIdentifier(structure.sampleIdentifier());



        }
    }

    private static void resolveSamples(
            List<Pair<Sample, ReferencesToResolve>> samplesWithSpaceAndProjectCodes,
            Map<String, Sample> externalIdentifierToSample)
    {
        for (Pair<Sample, ReferencesToResolve> sampleToResolve : samplesWithSpaceAndProjectCodes)
        {
            Map<String, List<String>> sampleIdentifiers =
                    sampleToResolve.getRight().sampleIdentifiers;
            Sample sample = sampleToResolve.getLeft();

            {
            List<Sample> parents = sampleToResolve.getRight().hierarchyEntries.stream()
                    .filter(x -> x.type() == HierarchyToResolve.Type.PARENT)
                    .map(x -> externalIdentifierToSample.get(x.objectIdentifer))
                    .collect(Collectors.toList());
                sample.setParents(parents);
                if (!parents.isEmpty())
                {
                    sample.getProperties().remove("parents");
                }
                for (Sample parent : parents)
                {
                    List<Sample> children =
                            Optional.ofNullable(parent.getChildren()).orElse(new ArrayList<>());
                    children.add(sampleToResolve.getLeft());
                    parent.setChildren(children);

                }

            }

            List<Sample> children =
                    Optional.ofNullable(sample.getChildren()).orElse(new ArrayList<>());
            sample.setChildren(children); // prevent NPEs for empty lists



        }
    }

    private static List<MissingReferenceValue> resolveOpenBisStructure(
            Map<EntityTypePermId, IEntityType> schema,
            Map<String, EntityTypePermId> entityTypeToRdfIdentifier, List<IMetadataEntry> entries,
            String fallbackSpaceCode,
            String fallbackProjectCode, Map<String, List<String>> typeToInheritanceChain,
            Map<String, Sample> roCrateIdsToObjects, Map<SpacePermId, Space> spaces,
            Map<ProjectIdentifier, Project> projects,
            List<ObjectReferenceToResolve> objectReferencesToResolve)
    {
        Map<Sample, List<ObjectReferenceToResolve>> grouped =
                objectReferencesToResolve.stream().collect(
                        Collectors.groupingBy(x -> x.sample())
                );
        List<MissingReferenceValue> missingReferenceValues = new ArrayList<>();

        for (Map.Entry<Sample, List<ObjectReferenceToResolve>> sampleWithReferenes : grouped.entrySet())
        {
            Sample sample = sampleWithReferenes.getKey();
            Map<String, List<String>> keyVals = new LinkedHashMap<>();
            for (ObjectReferenceToResolve reference : sampleWithReferenes.getValue())
            {
                List<String> orDefault = keyVals.getOrDefault(reference.key, new ArrayList<>());
                Sample sample1 = roCrateIdsToObjects.get(reference.value);
                if (sample1 == null)
                {
                    missingReferenceValues.add(
                            new MissingReferenceValue(reference.value, sample, reference.key));
                    continue;
                }

                String identifierToPut =
                        mapIdentifier(fallbackSpaceCode, fallbackProjectCode, spaces, projects,
                                sample1);
                orDefault.add(identifierToPut);
                keyVals.put(reference.key, orDefault);
            }
            keyVals.forEach((key, value) -> sample.getProperties()
                    .put(key, value.toArray(new String[] {})));

        }
        return missingReferenceValues;


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
            IMetadataEntry entry)
    {

        Map<String, List<String>> properties = entry.getReferences();
        String[] parts = entry.getId().split("/");

        String identifierSpaceCode = parts[0];



        String mySpace = Optional.ofNullable(properties.get(PROPERTY_SPACE)).map(x -> x.get(0))
                .orElse(null);
        String myProject =
                Optional.ofNullable(properties.get(PROPERTY_PROJECT)).map(x -> x.get(0))
                        .orElse(null);


        String myExperiment =
                Optional.ofNullable(properties.get(Constants.PROPERTY_COLLECTION)).map(
                                Object::toString)
                        .map(x -> x.split("/"))
                        .filter(x -> x.length >= 4)
                        .map(x -> x[3])
                        .map(x -> x.replaceAll("]$", ""))
                        .orElse(null);

        myProject =
                Optional.ofNullable(properties.get(PROPERTY_PROJECT)).map(
                                Object::toString)
                        .map(x -> x.split("/"))
                        .filter(x -> x.length >= 3)
                        .map(x -> x[2])
                        .map(x -> x.replaceAll("]$", ""))
                        .orElse(myProject);
        if (myProject == null)
        {
            String s = entry.getValues().entrySet().stream()
                    .filter(x -> x.getKey().equals(PROPERTY_PROJECT))
                    .findFirst()
                    .map(x -> x.getValue())
                    .map(Object::toString)
                    .orElse(null);
            myProject = s;

        }
        Set<String> filterSet =
                Set.of(PROPERTY_SPACE, PROPERTY_PROJECT, PROPERTY_COLLECTION,
                        PROPERTY_ID_PARENTS);
        Map<String, List<String>> samplesToResolve =
                properties.entrySet().stream().filter(x -> !filterSet.contains(x.getKey()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        List<HierarchyToResolve> hierarchyInfo = new ArrayList<>();

        properties.getOrDefault(PROPERTY_ID_PARENTS, List.of())
                .stream().map(x -> new HierarchyToResolve(HierarchyToResolve.Type.PARENT, x))
                .forEach(hierarchyInfo::add);

        return new ReferencesToResolve(mySpace, myProject, myExperiment, samplesToResolve,
                hierarchyInfo);
    }

    private static void resolveFile()
    {

    }

    private static Optional<EntityKind> matchEntityKind(Map<EntityTypePermId, IEntityType> schema,
            Map<String, EntityTypePermId> entityTypeToRdfIdentifier, IMetadataEntry metadataEntry,
            Map<String, List<String>> typeToInheritanceChain)
    {



        List<String> a =
                metadataEntry.getTypes().stream()
                        .map(x -> tryFindRdfIdentifier(schema, entityTypeToRdfIdentifier, x))
                        .map(typeToInheritanceChain::get)
                        .filter(Objects::nonNull).flatMap(Collection::stream)
                        .distinct()
                        .collect(Collectors.toList());
        for (String typeId : metadataEntry.getTypes())
        {
            IEntityType iEntityType = tryFind(schema, entityTypeToRdfIdentifier, typeId);
            if (iEntityType != null)
            {
                return Optional.of(EntityKind.SAMPLE);
            }

        }


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
            return DataType.MULTILINE_VARCHAR;
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
            return DataType.TIMESTAMP;
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
        if (typeProperty.getId().equals(PROPERTY_ID_PARENTS))
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

    record HierarchyToResolve(HierarchyToResolve.Type type, String objectIdentifer)
    {
        enum Type
        {
            PARENT,
            CHILD
        }

    }

    public static class ReferencesToResolve
    {
        String spaceCode;

        String projectCode;

        String collectionCode;

        Map<String, List<String>> sampleIdentifiers;

        List<HierarchyToResolve> hierarchyEntries;

        public ReferencesToResolve(@Nullable String spaceCode, @Nullable String projectCode,
                @Nullable String collectionCode,
                Map<String, List<String>> sampleIdentifiers,
                List<HierarchyToResolve> hierarchyEntries)
        {
            this.spaceCode = spaceCode;
            this.projectCode = projectCode;
            this.collectionCode = collectionCode;
            this.sampleIdentifiers = sampleIdentifiers;
            this.hierarchyEntries = hierarchyEntries;
        }

        public String getSpaceCode()
        {
            return spaceCode;
        }

        public String getProjectCode()
        {
            return projectCode;
        }

        public String getCollectionCode()
        {
            return collectionCode;
        }

        public Map<String, List<String>> getSampleIdentifiers()
        {
            return sampleIdentifiers;
        }

        public List<HierarchyToResolve> getHierarchyEntries()
        {
            return hierarchyEntries;
        }
    }

    private static String getIntersectionTypeIdentifier(Set<String> types)
    {
        return String.join("_", types);

    }

    private static String suffixCodeType(String code, DataType dataType)
    {
        return DataTypeMatcher.suffixTypeCode(code, dataType);
    }

    private static String openBisifyCode(String code)
    {
        return code.replaceAll(":", "_");
    }

    private static String mapIdentifier(String fallbackSpace, String fallBackProject,
            Map<SpacePermId, Space> spaces, Map<ProjectIdentifier, Project> projects, Sample sample)
    {

        String spaceCode =
                Optional.ofNullable(sample.getSpace()).map(Space::getCode).orElse(fallbackSpace);
        String projectCode = Optional.ofNullable(sample.getProject()).map(Project::getCode)
                .orElse(fallBackProject);

        return "/" + spaceCode + "/" + projectCode + "/" + sample.getCode();


    }

    private static String getCollectionTypeCode(IType type)
    {
        String[] split = type.getId().split(":");
        return split[split.length - 1].toUpperCase();

    }

    private static Serializable handleLiteralValues(Serializable a, DataType dataType)
    {
        if (dataType == DataType.TIMESTAMP || dataType == DataType.DATE)
        {
            TemporalAccessor ta = DateTimeFormatter.ISO_INSTANT.parse(
                    a.toString().toString().replaceAll("\"", ""));
            Instant i = Instant.from(ta);
            Date d = Date.from(i);
            DateFormat dateFormat = new SimpleDateFormat(
                    CANONICAL_DATE_FORMAT_PATTERN); // ch.systemsx.cisd.openbis.generic.shared.util.SupportedDateTimePattern.ISO_CANONICAL_DATE_PATTERN
            return dateFormat.format(d);

        }
        return a;

    }

    private static Serializable handlePossibleMultiValues(Serializable a, DataType dataType)
    {
        if (a instanceof Serializable[])
        {
            Serializable[] b = (Serializable[]) a;
            Arrays.stream(b).map(x -> handleLiteralValues(b, dataType)).map(x -> x.toString())
                    .collect(Collectors.joining(","));
        }
        return handleLiteralValues(a, dataType);

    }



    private static boolean isOpenBisDerived(Set<SampleType> openBisDerivedTypes,
            SampleType sampleType)
    {
        return openBisDerivedTypes.contains(sampleType);

    }

    private static Map<ObjectIdentifier, List<IFileInfo>> findRichTextImages()
    {
        return null;

    }

}
