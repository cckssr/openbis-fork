package ch.openbis.rocrate.app.reader;

import ch.eth.sis.rocrate.facade.IMetadataEntry;
import ch.eth.sis.rocrate.facade.IPropertyType;
import ch.eth.sis.rocrate.facade.IType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.entity.AbstractEntityCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.entity.AbstractEntityPropertyHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.id.ObjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IEntityType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.ExperimentType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.ProjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.DataType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.fetchoptions.PropertyTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.id.PropertyAssignmentPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.id.PropertyTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.SemanticAnnotation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RdfToModel
{
    public static OpenBisModel convert(List<IType> types, List<IPropertyType> typeProperties,
            List<IMetadataEntry> entries)
    {

        Map<String, IType> IdsToTypes =
                types.stream().collect(Collectors.toMap(IType::getId, Function.identity()));

        Map<String, List<String>> typeToInheritanceChain = new HashMap<>();

        Map<String, PropertyType> idsToPropertyTypes = typeProperties.stream()
                .collect(Collectors.toMap(x -> deRdfIdentifier(x.getId()),
                        RdfToModel::getPropertyType));
        Map<PropertyType, List<SampleType>> propertiesToObjects;

        Map<SampleType, List<SemanticAnnotation>> typesToSemanticAnnotations;
        Map<PropertyType, List<SemanticAnnotation>> propertyTypesToSemanticAnnotations;

        Map<SpacePermId, Space> spaces = new HashMap<>();

        Map<ProjectIdentifier, Project> projects = new HashMap<>();

        Map<String, SampleType> codeToSampleType = new HashMap<>();
        Map<String, ExperimentType> codeToExperimentType = new HashMap<>();

        Map<EntityTypePermId, IEntityType> schema = new HashMap<>();
        for (IType typerooni : types)
        {
            if (isProject(typerooni) || isSpace(typerooni))
            {
                System.out.println("Type " + typerooni.getId() + " is space or project, skipping");
                continue;
            }

            // match type
            List<IType> inheritanceChain = getInheritanceChain(typerooni, IdsToTypes);
            typeToInheritanceChain.put(typerooni.getId(),
                    inheritanceChain.stream().map(x -> x.getId()).collect(
                            Collectors.toList()));

            var subclassIdentifiers = typerooni.getSubClassOf();
            var kind = matchKind(inheritanceChain);
            if (kind == EntityKind.SAMPLE)
            {

                SampleTypeFetchOptions sampleTypeFetchOptions = new SampleTypeFetchOptions();
                sampleTypeFetchOptions.withSemanticAnnotations();
                sampleTypeFetchOptions.withPropertyAssignments();

                SampleType sampleType = new SampleType();
                sampleType.setCode(typerooni.getId());
                sampleType.setFetchOptions(sampleTypeFetchOptions);
                sampleType.setPermId(new EntityTypePermId(typerooni.getId(), EntityKind.SAMPLE));

                codeToSampleType.put(sampleType.getCode(), sampleType);

                if (!isOpenBisBaseType(typerooni))
                {
                    schema.put(sampleType.getPermId(), sampleType);
                }
                typerooni.getOntologicalAnnotations().forEach(x -> {
                    SemanticAnnotation semanticAnnotation = new SemanticAnnotation();

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
                experimentType.setCode(typerooni.getId());
                experimentType.setPermId(new EntityTypePermId(typerooni.getId(), kind));

                codeToExperimentType.put(typerooni.getId(), experimentType);

                if (!isOpenBisBaseType(typerooni))
                {
                    schema.put(experimentType.getPermId(), experimentType);
                }
            }

        }

        for (IPropertyType a : typeProperties)
        {
            PropertyType propertyType = new PropertyType();
            PropertyTypeFetchOptions fetchOptions = new PropertyTypeFetchOptions();
            fetchOptions.withSemanticAnnotations();
            propertyType.setFetchOptions(fetchOptions);


            String code = deRdfIdentifier(a.getId());
            propertyType.setPermId(new PropertyTypePermId(code));
            propertyType.setCode(code);
            DataType dataType = matchDataType(a);
            propertyType.setDataType(dataType);

            for (var domainThingy : a.getDomain())
            {
                SampleType sampleType = codeToSampleType.get(domainThingy);
                if (sampleType != null)
                {
                    List<PropertyAssignment> assignments = sampleType.getPropertyAssignments();
                    List<PropertyAssignment> newAssignments = new ArrayList();
                    PropertyAssignment curProperty = new PropertyAssignment();
                    curProperty.setPermId(new PropertyAssignmentPermId(sampleType.getPermId(),
                            propertyType.getPermId()));

                    PropertyAssignment propertyAssignment = new PropertyAssignment();
                    propertyAssignment.setEntityType(sampleType);
                    propertyAssignment.setPropertyType(propertyType);

                    newAssignments.add(curProperty);
                    if (assignments != null)
                    {
                        newAssignments.addAll(assignments);
                    }
                    newAssignments.add(propertyAssignment);
                    sampleType.setPropertyAssignments(newAssignments);
                    a.getOntologicalAnnotations().forEach(x -> {
                        SemanticAnnotation annotation = new SemanticAnnotation();
                        annotation.setPredicateAccessionId(x);
                        annotation.setEntityType(sampleType);
                        annotation.setPropertyType(propertyType);

                        var existingAnnotations = propertyType.getSemanticAnnotations();
                        List<SemanticAnnotation> annotations = new ArrayList<>();
                        if (existingAnnotations != null)
                        {
                            annotations.addAll(existingAnnotations);
                        }
                        annotations.add(annotation);
                        propertyType.setSemanticAnnotations(annotations);
                    });
                }

            }

        }

        Map<ObjectIdentifier, AbstractEntityPropertyHolder> metadata = new HashMap<>();
        for (IMetadataEntry entry : entries)
        {
            AbstractEntityPropertyHolder entity = new Sample();
            ObjectIdentifier objectIdentifier = new SampleIdentifier("lol");

            Optional<EntityKind> entityKind =
                    matchEntityKind(entry, codeToSampleType, typeToInheritanceChain);

            if (entityKind.filter(x -> x == EntityKind.SAMPLE).isPresent())
            {
                Sample sample = new Sample();
                objectIdentifier = new SampleIdentifier(entry.getId());
                Map<String, Serializable> properties = new HashMap<>();
                sample.setType(codeToSampleType.get(entry.getType()));
                entity = sample;
                properties.putAll(entry.getValues());
                for (var a : entry.getValues().entrySet())
                {
                    properties.put(deRdfIdentifier(a.getKey()), a.getValue());

                }

                for (var a : entry.getReferences().entrySet())
                {
                    properties.put(deRdfIdentifier(a.getKey()),
                            a.getValue().stream().collect(Collectors.joining("\n")));
                }
                EntityTypePermId entityTypePermId =
                        new EntityTypePermId(entry.getId(), entityKind.get());
                metadata.put(objectIdentifier, entity);
            }
            if (entry.getType().equals(":Space"))
            {
                Space space = new Space();
                space.setCode(entry.getId());
                spaces.put(new SpacePermId(space.getCode()), space);
            }
            if (entry.getType().equals(":Project"))
            {
                Project project = new Project();
                project.setCode(entry.getId());
                String spaceCode = null;
                if (entry.getValues().containsKey("hasPROJECT"))
                {
                    spaceCode = entry.getValues().get("hasPROJECT").toString();
                }

                projects.put(new ProjectIdentifier(spaceCode, entry.getId()), project);

            }

        }

        //resolve stuff

        List<IEntityType> entityTypes = new ArrayList<>();

        List<AbstractEntityCreation> entities = new ArrayList<>();

        return new OpenBisModel(Map.of(), schema, spaces, projects, metadata, Map.of(), Map.of());
    }

    private static SampleType getSampleType(IType type)
    {
        SampleType sampleType = new SampleType();
        sampleType.setCode(type.getId());
        return sampleType;
    }

    private static PropertyType getPropertyType(IPropertyType typeProperty)
    {
        PropertyType propertyType = new PropertyType();
        propertyType.setCode(typeProperty.getId());
        return propertyType;
    }

    private static Optional<EntityKind> matchEntityKind(IMetadataEntry metfadataEntry,
            Map<String, SampleType> codeToSampleType,
            Map<String, List<String>> typeToInheritanceChain)
    {
        List<String> a =
                typeToInheritanceChain.get(metfadataEntry.getType());
        if (a == null)
        {
            return Optional.empty();
        }

        if (a.stream().anyMatch(":Collection"::equals))
        {
            return Optional.of(EntityKind.EXPERIMENT);
        }
        return Optional.of(EntityKind.SAMPLE);
    }

    private static String deRdfIdentifier(String a)
    {
        Pattern patternBool = Pattern.compile("^is");
        Pattern patternRest = Pattern.compile("^has");

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

        return DataType.VARCHAR;
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
        if (inheritanceChain.stream().anyMatch(x -> x.getId().equals(":Collection")))
        {
            return EntityKind.EXPERIMENT;
        }
        return EntityKind.SAMPLE;

    }

    private static boolean isSpace(IType type)
    {
        return type.getId().equals(":Space");
    }

    private static boolean isProject(IType type)
    {
        return type.getId().equals(":Project");
    }

    private static boolean isOpenBisBaseType(IType type)
    {
        if (isSpace(type))
        {
            return true;
        }
        if (isProject(type))
        {
            return true;
        }
        if (type.getId().equals(":Dataset"))
        {
            return true;
        }
        if (type.getId().equals(":Collection"))
        {
            return true;
        }
        if (type.getId().equals(":Object"))
        {
            return true;
        }
        if (type.getId().equals(":Vocabulary"))
        {
            return true;
        }
        return false;

    }

}
