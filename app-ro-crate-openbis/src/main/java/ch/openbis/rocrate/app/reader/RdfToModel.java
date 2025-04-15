package ch.openbis.rocrate.app.reader;

import ch.eth.sis.rocrate.facade.IMetadataEntry;
import ch.eth.sis.rocrate.facade.IPropertyType;
import ch.eth.sis.rocrate.facade.IType;
import ch.eth.sis.rocrate.facade.LiteralType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.entity.AbstractEntityPropertyHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.id.ObjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IEntityType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.ExperimentType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentTypeFetchOptions;
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
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.Serializable;
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

        Map<String, List<String>> typeToInheritanceChain = new LinkedHashMap<>();

        Map<SpacePermId, Space> spaces = new LinkedHashMap<>();

        Map<ProjectIdentifier, Project> projects = new LinkedHashMap<>();

        Map<String, SampleType> codeToSampleType = new LinkedHashMap<>();

        List<Pair<Sample, ReferencesToResolve>> samplesWithSpaceAndProjectCodes = new ArrayList<>();

        Map<EntityTypePermId, IEntityType> schema = new LinkedHashMap<>();
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
                sampleType.setCode(removePrefix(type.getId()));
                sampleType.setFetchOptions(sampleTypeFetchOptions);
                sampleType.setPermId(new EntityTypePermId(type.getId(), EntityKind.SAMPLE));
                sampleType.setPropertyAssignments(new ArrayList<>());

                codeToSampleType.put(sampleType.getCode(), sampleType);

                if (isOpenBisDerivedType(type))
                {
                    schema.put(sampleType.getPermId(), sampleType);
                }
                type.getOntologicalAnnotations().forEach(x -> {
                    SemanticAnnotation semanticAnnotation = new SemanticAnnotation();
                    semanticAnnotation.setDescriptorAccessionId(x);
                    semanticAnnotation.setDescriptorOntologyId(x);
                    semanticAnnotation.setDescriptorOntologyVersion(x);

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
                experimentType.setCode(type.getId());
                experimentType.setPermId(new EntityTypePermId(type.getId(), kind));

                if (isOpenBisDerivedType(type))
                {
                    schema.put(experimentType.getPermId(), experimentType);
                }
            }

        }

        Map<String, List<String>> typesToProperties = new LinkedHashMap<>();
        for (IPropertyType typeProperty : typeProperties)
        {
            for (String domain : typeProperty.getDomain())
            {
                List<String> typeToDomain =
                        typesToProperties.getOrDefault(domain, new ArrayList<>());
                typeToDomain.add(typeProperty.getId());
                typesToProperties.put(domain, typeToDomain);
            }
        }

        for (IPropertyType a : typeProperties)
        {
            PropertyType propertyType = new PropertyType();
            {
                PropertyTypeFetchOptions fetchOptions = new PropertyTypeFetchOptions();
                fetchOptions.withSemanticAnnotations();
                fetchOptions.withVocabulary();

                propertyType.setFetchOptions(fetchOptions);
            }
            propertyType.setSemanticAnnotations(new ArrayList<>());
            propertyType.setMultiValue(false);
            propertyType.setCode(deRdfIdentifier(a.getId()));
            propertyType.setDescription(propertyType.getCode());
            propertyType.setLabel(propertyType.getCode());


            String code = deRdfIdentifier(a.getId());
            propertyType.setPermId(new PropertyTypePermId(code));
            propertyType.setCode(code);
            DataType dataType = matchDataType(a);
            propertyType.setDataType(dataType);

            for (String domain : a.getDomain())
            {
                if (requiresSpecialHandling(a))
                {
                    continue;
                }

                SampleType sampleType = codeToSampleType.get(domain);
                if (sampleType != null)
                {
                    List<PropertyAssignment> assignments = sampleType.getPropertyAssignments();
                    List<PropertyAssignment> newAssignments = new ArrayList<>();
                    PropertyAssignment curProperty =
                            getPropertyAssignment(propertyType, sampleType);

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
                String artificialTypeIdentifier = getIntersectionTypeIdentifier(intersectionType);
                sampleType.setCode(artificialTypeIdentifier);

                List<PropertyAssignment> assignments = new ArrayList<>();
                List<SemanticAnnotation> semanticAnnotations = new ArrayList<>();

                for (String type : intersectionType)
                {

                    IEntityType entityType =
                            schema.get(new EntityTypePermId(type, EntityKind.SAMPLE));
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
                sampleType.setCode(getIntersectionTypeIdentifier(intersectionType));
                sampleType.setPermId(new EntityTypePermId(sampleType.getCode(), EntityKind.SAMPLE));
                schema.put(sampleType.getPermId(), sampleType);
                codeToSampleType.put(sampleType.getCode(), sampleType);

            }

        }


        Map<ObjectIdentifier, AbstractEntityPropertyHolder> metadata = new LinkedHashMap<>();
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
                    sample.setFetchOptions(fetchOptions);
                }
                sample.setCode(entry.getId());


                objectIdentifier = new SampleIdentifier(entry.getId());
                String typeCode = entry.getTypes().size() == 1 ?
                        entry.getTypes().stream().findFirst().orElseThrow() :
                        getIntersectionTypeIdentifier(entry.getTypes());

                sample.setType(codeToSampleType.get(typeCode));
                entity = sample;
                Map<String, Serializable> properties = new LinkedHashMap<>(entry.getValues());
                for (Map.Entry<String, Serializable> property : entry.getValues().entrySet())
                {
                    properties.put(deRdfIdentifier(property.getKey()), property.getValue());

                }

                for (Map.Entry<String, List<String>> reference : entry.getReferences().entrySet())
                {
                    properties.put(deRdfIdentifier(reference.getKey()),
                            String.join("\n", reference.getValue()));
                }
                metadata.put(objectIdentifier, entity);
                sample.setProperties(properties);
                properties.get("SPACE");
                ReferencesToResolve referencesToResolve =
                        buildEntryWithSpaceAndProjectToResolve(properties, fallbackSpaceCode,
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

            } else if (entry.getTypes().stream().anyMatch(
                    x -> typeToInheritanceChain.get(x) != null && typeToInheritanceChain.get(x)
                            .stream()
                    .filter(Objects::nonNull)
                            .anyMatch(y -> y.equals(Constants.GRAPH_ID_Collection))))
            {
                ExperimentType experiment = new ExperimentType();
                experiment.setCode(entry.getId());
                experiment.setPermId(new EntityTypePermId(entry.getId(), EntityKind.EXPERIMENT));

                schema.put(experiment.getPermId(), experiment);

            }

        }
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
        //resolve stuff all references and stuff. This should help. Or something.
        for (Project project : projects.values())
        {
            SpacePermId identifier =
                    new SpacePermId(project.getIdentifier().getIdentifier().split("/")[1]);
            project.setSpace(spaces.get(identifier));
        }
        for (Pair<Sample, ReferencesToResolve> sampleToResolve : samplesWithSpaceAndProjectCodes)
        {
            Space space = spaces.get(new SpacePermId(sampleToResolve.getRight().getSpaceCode()));
            Project project = projects.get(
                    new ProjectIdentifier(space.getCode(),
                            sampleToResolve.getRight().getProjectCode()));
            sampleToResolve.getLeft().setSpace(space);
            sampleToResolve.getLeft().setProject(project);

        }


        return new OpenBisModel(Map.of(), schema, spaces, projects, metadata, Map.of(), Map.of());
    }

    private static SampleTypeFetchOptions getSampleTypeFetchOptions()
    {
        SampleTypeFetchOptions sampleTypeFetchOptions = new SampleTypeFetchOptions();
        sampleTypeFetchOptions.withSemanticAnnotations();
        sampleTypeFetchOptions.withPropertyAssignments();
        return sampleTypeFetchOptions;
    }

    private static PropertyAssignment getPropertyAssignment(PropertyType propertyType,
            SampleType sampleType)
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

        curProperty.setMandatory(false);

        curProperty.setPermId(new PropertyAssignmentPermId(sampleType.getPermId(),
                propertyType.getPermId()));
        return curProperty;
    }

    private static ReferencesToResolve buildEntryWithSpaceAndProjectToResolve(
            Map<String, Serializable> properties, String spaceCode, String projectCode,
            String defaultExperimentCode)
    {
        String mySpace = properties.getOrDefault(Constants.PROPERTY_SPACE, spaceCode).toString();
        String myProject =
                properties.getOrDefault(Constants.PROPERTY_PROJECT, projectCode).toString();
        String myExperiment =
                Optional.ofNullable(properties.get(Constants.PROPERTY_COLLECTION)).map(
                                Object::toString)
                        .map(x -> x.split("/"))
                        .map(x -> x[3])
                        .orElse(defaultExperimentCode);

        myProject = Optional.ofNullable(properties.get(Constants.PROPERTY_COLLECTION))
                .map(Object::toString)
                .map(x -> x.split("/"))
                .map(x -> x[2])
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

    private static DataType matchDataType(IPropertyType propertyType)
    {

        if (propertyType.getRange().stream()
                .anyMatch(x -> x.equals(LiteralType.STRING.getTypeName())))
        {
            return DataType.VARCHAR;
        }
        if (propertyType.getRange().stream()
                .anyMatch(x -> x.equals(LiteralType.BOOLEAN.getTypeName())))
        {
            return DataType.BOOLEAN;
        }
        if (propertyType.getRange().stream()
                .anyMatch(x -> x.equals(LiteralType.DECIMAL.getTypeName())))
        {
            return DataType.REAL;
        }
        if (propertyType.getRange().stream()
                .anyMatch(x -> x.equals(LiteralType.INTEGER.getTypeName())))
        {
            return DataType.INTEGER;
        }
        if (propertyType.getRange().stream()
                .anyMatch(x -> x.equals(LiteralType.DATETIME.getTypeName())))
        {
            return DataType.DATE;
        }
        if (propertyType.getRange().stream().anyMatch(x -> x.equals(LiteralType.XML_LITERAL)))
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

    private static boolean isProject(IType type)
    {
        return type.getId().equals(GRAPH_ID_PROJECT);
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
        if (type.getId().equals(":Dataset"))
        {
            return false;
        }
        if (type.getId().equals(GRAPH_ID_Collection))
        {
            return false;
        }
        if (type.getId().equals(":Object"))
        {
            return false;
        }
        if (type.getId().equals(":Vocabulary"))
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

    private static String removePrefix(String a)
    {
        return a.replaceFirst("^_:", "");
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


}
