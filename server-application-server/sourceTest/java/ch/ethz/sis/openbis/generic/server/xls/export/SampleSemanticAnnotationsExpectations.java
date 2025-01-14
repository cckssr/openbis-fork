/*
 * Copyright ETH 2022 - 2023 Zürich, Scientific IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.ethz.sis.openbis.generic.server.xls.export;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.Person;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.ProjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.DataType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.fetchoptions.PropertyAssignmentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.fetchoptions.PropertyTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SamplePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.SemanticAnnotation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.fetchoptions.SemanticAnnotationFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.search.SemanticAnnotationSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import ch.ethz.sis.openbis.systemtest.asapi.v3.ExportTest;
import ch.systemsx.cisd.openbis.generic.server.business.bo.CollectionMatcher;
import org.jmock.Expectations;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

class SampleSemanticAnnotationsExpectations extends Expectations
{

    public static final String PERM_ID = "200001010000000-0001";

    public SampleSemanticAnnotationsExpectations(final IApplicationServerApi api,
            final boolean exportReferred)
    {

        if (exportReferred)
        {
            allowing(api).getSamples(with(XLSExportTest.SESSION_TOKEN),
                    with(new CollectionMatcher<>(List.of(new SamplePermId(PERM_ID)))),
                    with(any(SampleFetchOptions.class)));
        }
        allowing(api).getSampleTypes(with(XLSExportTest.SESSION_TOKEN),
                with(new CollectionMatcher<>(
                        List.of(new EntityTypePermId(PERM_ID, EntityKind.SAMPLE)))), with(any(
                        SampleTypeFetchOptions.class)));

        will(new CustomAction("getting sampleType")
        {

            @Override
            public Object invoke(final Invocation invocation) throws Throwable
            {
                final SampleTypeFetchOptions fetchOptions =
                        (SampleTypeFetchOptions) invocation.getParameter(2);
                fetchOptions.withPropertyAssignments();
                final PropertyAssignment namePropertyAssignment = getNamePropertyAssignment();

                final Space space = new Space();
                space.setCode("ELN_SETTINGS");
                space.setPermId(new SpacePermId("ELN_SETTINGS"));

                final Project project = new Project();
                project.setCode("STORAGES");
                project.setIdentifier(new ProjectIdentifier("/ELN_SETTINGS/STORAGES"));

                final Experiment experiment = new Experiment();
                experiment.setCode("STORAGES_COLLECTION");
                experiment.setIdentifier(
                        new ExperimentIdentifier("/ELN_SETTINGS/STORAGES/STORAGES_COLLECTION"));

                final Experiment defaultExperiment = new Experiment();
                defaultExperiment.setCode("DEFAULT");
                defaultExperiment.setIdentifier(
                        new ExperimentIdentifier("/DEFAULT/DEFAULT/DEFAULT"));

                final Calendar calendar = Calendar.getInstance();
                calendar.set(2023, Calendar.MARCH, 10, 17, 23, 44);
                final Date registrationDate = calendar.getTime();

                calendar.set(2023, Calendar.MARCH, 11, 17, 23, 44);
                final Date modificationDate = calendar.getTime();

                final Person registrator = new Person();
                registrator.setUserId("system");

                final Person modifier = new Person();
                modifier.setUserId("test");

                final SampleType[] sampleTypes = new SampleType[1];

                sampleTypes[0] = getSampleType(fetchOptions, namePropertyAssignment);
                return Arrays.stream(sampleTypes)
                        .collect(Collectors.toMap(SampleType::getPermId, Function.identity(),
                                (sample1, sample2) -> sample2, LinkedHashMap::new));
            }

            private PropertyAssignment getBoxesCountPropertyAssignment()
            {
                final PropertyType propertyType = new PropertyType();
                propertyType.setCode("STORAGE.BOX_NUM");
                propertyType.setLabel("Number of Boxes");
                propertyType.setDescription("Number of Boxes");
                propertyType.setDataType(DataType.INTEGER);
                propertyType.setManagedInternally(true);

                final PropertyAssignment propertyAssignment = new PropertyAssignment();
                propertyAssignment.setFetchOptions(getPropertyAssignmentFetchOptions());
                propertyAssignment.setPropertyType(propertyType);
                propertyAssignment.setMandatory(false);
                propertyAssignment.setShowInEditView(true);
                propertyAssignment.setSection("General info");

                return propertyAssignment;
            }

        });
        allowing(api).searchSemanticAnnotations(with(XLSExportTest.SESSION_TOKEN), with(any(
                        SemanticAnnotationSearchCriteria.class)),
                with(any(SemanticAnnotationFetchOptions.class)));

        SemanticAnnotation semanticAnnotation = new SemanticAnnotation();

        List<SemanticAnnotation> semanticAnnotations = List.of(semanticAnnotation);
        SemanticAnnotationFetchOptions semanticAnnotationFetchOptions =
                new SemanticAnnotationFetchOptions();
        semanticAnnotationFetchOptions.withPropertyAssignment();
        semanticAnnotationFetchOptions.withEntityType();
        semanticAnnotation.setFetchOptions(semanticAnnotationFetchOptions);
        semanticAnnotation.setPropertyAssignment(getNamePropertyAssignment());
        semanticAnnotation.setDescriptorOntologyId("ontologyid");
        semanticAnnotation.setDescriptorAccessionId("accessiond");
        semanticAnnotation.setDescriptorOntologyVersion("ontologyversion");
        semanticAnnotation.setPredicateAccessionId("accesionid");
        semanticAnnotation.setPredicateOntologyVersion("ontologyversion");
        semanticAnnotation.setPredicateOntologyId("ontologyid");

        SampleType sampleType = new SampleType();
        sampleType.setCode(PERM_ID);
        semanticAnnotation.setEntityType(sampleType);
        semanticAnnotation.setPropertyAssignment(getNamePropertyAssignment());

        SearchResult<SemanticAnnotation> searchResult =
                new SearchResult<>(semanticAnnotations, semanticAnnotations.size());
        will(returnValue(searchResult));

    }

    private static SampleType getSampleType(SampleTypeFetchOptions fetchOptions,
            PropertyAssignment namePropertyAssignment)
    {
        SampleType type = new SampleType();

        {
            type.setCode("SAMPLE");
            type.setFetchOptions(fetchOptions);
            type.setPermId(new EntityTypePermId(PERM_ID, EntityKind.SAMPLE));
            type.setPropertyAssignments(List.of(namePropertyAssignment));
            type.setDescription("description");
            type.setManagedInternally(false);
            type.setAutoGeneratedCode(false);
            type.setValidationPlugin(null);
            type.setGeneratedCodePrefix(null);
            Calendar myCal = Calendar.getInstance();
            myCal.set(Calendar.YEAR, 2025);
            myCal.set(Calendar.MONTH, 1);
            myCal.set(Calendar.DAY_OF_MONTH, 12);
            Date theDate = myCal.getTime();
            LocalDateTime dateTime = LocalDateTime.parse("2018-05-05T11:50:55");
            var instant = dateTime.toInstant(ZoneOffset.UTC);
            type.setModificationDate(Date.from(instant));
            type.setSubcodeUnique(false);

        }
        return type;
    }

    private static String getResourceFileContent(final String filePath)
    {
        try (final InputStream exampleTextInputStream = ExportTest.class.getClassLoader()
                .getResourceAsStream(filePath))
        {
            Objects.requireNonNull(exampleTextInputStream);
            return new String(exampleTextInputStream.readAllBytes());
        } catch (final IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private PropertyAssignment getNamePropertyAssignment()
    {
        final PropertyType propertyType = new PropertyType();
        propertyType.setCode("NAME");
        propertyType.setLabel("Name");
        propertyType.setDescription("Name");
        propertyType.setDataType(DataType.VARCHAR);
        propertyType.setManagedInternally(true);

        PropertyTypeFetchOptions propertyTypeFetchOptions = new PropertyTypeFetchOptions();

        propertyTypeFetchOptions.withVocabulary();
        propertyTypeFetchOptions.withSemanticAnnotations();

        propertyType.setFetchOptions(propertyTypeFetchOptions);

        final PropertyAssignment propertyAssignment = new PropertyAssignment();
        propertyAssignment.setFetchOptions(getPropertyAssignmentFetchOptions());
        propertyAssignment.setPropertyType(propertyType);
        propertyAssignment.setMandatory(false);
        propertyAssignment.setShowInEditView(true);
        propertyAssignment.setSection("General info");
        SampleType sampleType = new SampleType();
        sampleType.setCode("SAMPLE");
        propertyAssignment.setEntityType(sampleType);

        return propertyAssignment;
    }

    private PropertyAssignmentFetchOptions getPropertyAssignmentFetchOptions()
    {
        final PropertyAssignmentFetchOptions fetchOptions = new PropertyAssignmentFetchOptions();
        fetchOptions.withPropertyType();
        fetchOptions.withPlugin();
        fetchOptions.withEntityType();

        return fetchOptions;
    }

}
