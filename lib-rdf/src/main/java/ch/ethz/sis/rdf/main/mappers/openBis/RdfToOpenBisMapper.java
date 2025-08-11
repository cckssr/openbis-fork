package ch.ethz.sis.rdf.main.mappers.openBis;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.entity.AbstractEntityPropertyHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.id.ObjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IEntityType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.fetchoptions.ProjectFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.ProjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.DataType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.fetchoptions.PropertyAssignmentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.fetchoptions.PropertyTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.SemanticAnnotation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.Vocabulary;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.VocabularyTerm;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.fetchoptions.VocabularyFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.id.VocabularyPermId;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.ethz.sis.rdf.main.model.rdf.ModelRDF;
import ch.ethz.sis.rdf.main.model.xlsx.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RdfToOpenBisMapper
{


    public static OpenBisModel convert(ModelRDF modelRDF, String projectIdentifier)
    {
        Map<EntityTypePermId, IEntityType> schema = new LinkedHashMap<>();
        Map<ObjectIdentifier, AbstractEntityPropertyHolder> metadata = new LinkedHashMap<>();
        Map<ProjectIdentifier, Project> projects = new LinkedHashMap<>();
        Map<SpacePermId, Space> spaces = new LinkedHashMap<>();

        List<String> vocabularyOptionList = modelRDF.vocabularyTypeList.stream()
                .flatMap(vocabularyType -> vocabularyType.getOptions().stream())
                .map(VocabularyTypeOption::getDescription)
                .toList();

        Map<VocabularyPermId, Vocabulary> vocabularyMap = new LinkedHashMap<>();

        for (VocabularyType a : modelRDF.vocabularyTypeList)
        {
            Vocabulary vocabulary = new Vocabulary();

            VocabularyFetchOptions fetchOptions = new VocabularyFetchOptions();
            fetchOptions.withTerms();

            vocabulary.setFetchOptions(fetchOptions);
            vocabulary.setTerms(new ArrayList<>());
            vocabulary.setCode(a.code);
            vocabulary.setDescription(a.description);
            for (VocabularyTypeOption option : a.options)
            {
                VocabularyTerm term = new VocabularyTerm();
                term.setCode(option.code);
                term.setDescription(option.description);
                term.setLabel(option.label);
                vocabulary.getTerms().add(term);
            }

            vocabularyMap.put(new VocabularyPermId(vocabulary.getCode()), vocabulary);

        }

        for (SampleType rdfSampleType : modelRDF.sampleTypeList)
        {
            ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType v3SampleType =
                    new ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType();
            v3SampleType.setCode(rdfSampleType.code);
            v3SampleType.setDescription(rdfSampleType.description);

            SampleTypeFetchOptions fetchOptions = new SampleTypeFetchOptions();
            fetchOptions.withPropertyAssignments();
            fetchOptions.withSemanticAnnotations();
            v3SampleType.setFetchOptions(fetchOptions);

            v3SampleType.setPropertyAssignments(new ArrayList<>());
            List<SemanticAnnotation> semanticAnnotations = new ArrayList<>();
            var semanticAnnotation = new SemanticAnnotation();
            semanticAnnotation.setEntityType(v3SampleType);
            semanticAnnotation.setDescriptorOntologyId(modelRDF.ontNamespace);
            semanticAnnotation.setDescriptorOntologyVersion(modelRDF.ontVersion);
            semanticAnnotation.setDescriptorAccessionId(rdfSampleType.ontologyAnnotationId);
            semanticAnnotations.add(semanticAnnotation);
            v3SampleType.setSemanticAnnotations(semanticAnnotations);

            for (SamplePropertyType samplePropertyType : rdfSampleType.properties)
            {

                PropertyTypeFetchOptions propertyTypeFetchOptions = new PropertyTypeFetchOptions();
                propertyTypeFetchOptions.withSampleType();
                propertyTypeFetchOptions.withSemanticAnnotations();
                propertyTypeFetchOptions.withVocabulary();


                PropertyType propertyType = new PropertyType();
                propertyType.setFetchOptions(propertyTypeFetchOptions);
                propertyType.setSemanticAnnotations(new ArrayList<>());
                propertyType.setCode(samplePropertyType.code);
                if (samplePropertyType.vocabularyCode != null)
                {
                    propertyType.setVocabulary(
                            vocabularyMap.get(
                                    new VocabularyPermId(samplePropertyType.vocabularyCode)));
                }
                propertyType.setDataType(DataType.valueOf(samplePropertyType.dataType));
                propertyType.setLabel(samplePropertyType.propertyLabel);
                propertyType.setDescription(samplePropertyType.description
                );

                PropertyAssignment assignment = new PropertyAssignment();
                assignment.setSemanticAnnotations(new ArrayList<>());
                assignment.setMandatory(samplePropertyType.isMandatory == 1);
                boolean isMultiValued =
                        samplePropertyType.isMultiValue || modelRDF.getMultiValueProperties()
                                .contains(
                                        samplePropertyType.code); // In TTL, this can be different on a per class basis, in openBIS only PropertyTypes can be multivalued, not the assignments. So it's a hack.
                propertyType.setMultiValue(isMultiValued);
                List<SemanticAnnotation> propertySemanticAnnotations = new ArrayList<>();
                SemanticAnnotation semanticAnnotation1 = new SemanticAnnotation();
                propertySemanticAnnotations.add(semanticAnnotation1);
                semanticAnnotation1.setPropertyType(propertyType);
                semanticAnnotation1.setPredicateOntologyId(modelRDF.ontNamespace);
                semanticAnnotation1.setPredicateOntologyVersion(modelRDF.ontVersion);
                semanticAnnotation1.setPredicateAccessionId(
                        samplePropertyType.ontologyAnnotationId);
                propertyType.setSemanticAnnotations(propertySemanticAnnotations);
                assignment.setSemanticAnnotations(propertySemanticAnnotations);



                PropertyAssignmentFetchOptions fetchOptions1 = new PropertyAssignmentFetchOptions();
                fetchOptions1.withEntityType();
                fetchOptions1.withPropertyType();
                assignment.setEntityType(v3SampleType);
                assignment.setPropertyType(propertyType);
                fetchOptions1.withSemanticAnnotations();

                assignment.setFetchOptions(fetchOptions1);


                List<PropertyAssignment> assignmentList = new ArrayList<>();
                List<PropertyAssignment> existingList = v3SampleType.getPropertyAssignments();
                if (existingList != null)
                {
                    assignmentList.addAll(existingList);
                }
                assignmentList.add(assignment);

                SemanticAnnotation predicateSemanticAnnotation = new SemanticAnnotation();
                predicateSemanticAnnotation.setPredicateAccessionId(
                        samplePropertyType.ontologyAnnotationId);
                predicateSemanticAnnotation.setPredicateOntologyVersion(modelRDF.ontVersion);
                predicateSemanticAnnotation.setPredicateOntologyId(modelRDF.ontNamespace);
                predicateSemanticAnnotation.setEntityType(v3SampleType);
                predicateSemanticAnnotation.setPropertyType(propertyType);

                v3SampleType.setPropertyAssignments(assignmentList);
            }
            schema.put(new EntityTypePermId(rdfSampleType.code, EntityKind.SAMPLE), v3SampleType);

        }

        Space space = new Space();
        space.setCode(projectIdentifier.split("/")[1]);
        spaces.put(new SpacePermId(space.getCode()), space);

        Project project = new Project();
        project.setCode(projectIdentifier);
        project.setIdentifier(new ProjectIdentifier(projectIdentifier));
        project.setSpace(space);
        {
            ProjectFetchOptions fetchOptions = new ProjectFetchOptions();
            fetchOptions.withSpace();
            project.setFetchOptions(fetchOptions);

        }

        projects.put(project.getIdentifier(), project);



        for (Map.Entry<String, List<SampleObject>> objectEntry : modelRDF.sampleObjectsGroupedByTypeMap.entrySet())
        {

            IEntityType
                    sampleType =
                    schema.get(new EntityTypePermId(objectEntry.getKey(), EntityKind.SAMPLE));

            if (sampleType == null)
            {
                continue;
            }
            for (SampleObject sampleObject : objectEntry.getValue())
            {
                Sample sample = new Sample();

                SampleFetchOptions fetchOptions = new SampleFetchOptions();
                fetchOptions.withProperties();
                fetchOptions.withType();
                fetchOptions.withSpace();
                fetchOptions.withProject();
                sample.setFetchOptions(fetchOptions);
                sample.setSpace(space);
                sample.setProject(project);

                ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType sampleType1 =
                        (ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType) sampleType;

                sample.setType(sampleType1);
                sample.setCode(OpenBisModel.makeOpenBisCodeCompliant(sampleObject.code));
                Map<String, PropertyAssignment>
                        labelToProperty = sampleType1.getPropertyAssignments().stream()
                        .collect(Collectors.toMap(x -> x.getPropertyType().getLabel(), x -> x));

                Map<String, Serializable> proppies = sampleObject.getProperties().stream()
                        .collect(Collectors.toMap(x -> x.getLabel(),
                                x -> convertValue(vocabularyOptionList, x,
                                        labelToProperty.get(x.label).getPropertyType(),
                                        sample.getSpace().getCode(), sample.getProject().getCode()),
                                (existing, replacement) -> existing + "," + replacement));

                if (!proppies.containsKey("Name"))
                {
                    proppies.put("Name", sampleObject.name);
                }

                sample.setProperties(proppies);

                metadata.put(new SampleIdentifier(projectIdentifier, "DEFAULT",
                                sampleObject.code.toUpperCase()),
                        sample);
            }

        }

        Map<String, String> externalToOpenBisIdentifiers = new LinkedHashMap<>();
        return new OpenBisModel(vocabularyMap, schema, spaces, projects, metadata, Map.of(),
                Map.of(), externalToOpenBisIdentifiers);

    }




    private static String convertValue(List<String> vocabularyOptionList,
            SampleObjectProperty sampleObjectProperty, PropertyType propertyType, String space,
            String project)
    {
        return ValueMapper.mapValue(vocabularyOptionList, sampleObjectProperty, propertyType, space,
                project);

    }




}
