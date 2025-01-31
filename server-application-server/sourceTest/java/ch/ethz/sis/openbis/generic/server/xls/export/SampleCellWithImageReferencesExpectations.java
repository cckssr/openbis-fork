/*
 *  Copyright ETH 2022 - 2024 Zürich, Scientific IT Services
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
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
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SamplePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.SemanticAnnotation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.fetchoptions.SemanticAnnotationFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.search.SemanticAnnotationSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import ch.systemsx.cisd.openbis.generic.server.business.bo.CollectionMatcher;
import org.jmock.Expectations;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

class SampleCellWithImageReferencesExpectations extends Expectations
{

    public SampleCellWithImageReferencesExpectations(final IApplicationServerApi api, final boolean exportReferred)
    {
        if (exportReferred)
        {
            allowing(api).getSamples(with(XLSExportTest.SESSION_TOKEN),
                    with(new CollectionMatcher<>(List.of(new SamplePermId("200001010000000-0001")))),
                    with(any(SampleFetchOptions.class)));
        }
        allowing(api).getSamples(with(XLSExportTest.SESSION_TOKEN), with(new CollectionMatcher<>(
                        List.of(new SamplePermId("200001010000000-0001")))), with(any(SampleFetchOptions.class))
        );

        will(new CustomAction("getting samples")
        {

            @Override
            public Object invoke(final Invocation invocation) throws Throwable
            {
                final SampleFetchOptions fetchOptions = (SampleFetchOptions) invocation.getParameter(2);
                final PropertyAssignment namePropertyAssignment = getNamePropertyAssignment();
                final PropertyAssignment experimentalDescriptionPropertyAssignment = getExperimentalDescriptionPropertyAssignment();
                final PropertyAssignment experimentalGoalsPropertyAssignment = getExperimentalGoalsPropertyAssignment();

                final SampleType sampleType = new SampleType();
                sampleType.setCode("STORAGE");
                sampleType.setManagedInternally(false);
                sampleType.setPermId(new EntityTypePermId("STORAGE", EntityKind.SAMPLE));
                sampleType.setFetchOptions(fetchOptions.withType());
                sampleType.setPropertyAssignments(List.of(namePropertyAssignment, experimentalDescriptionPropertyAssignment,
                        experimentalGoalsPropertyAssignment));

                final Space space = new Space();
                space.setCode("ELN_SETTINGS");
                space.setPermId(new SpacePermId("ELN_SETTINGS"));

                final Project project = new Project();
                project.setCode("STORAGES");
                project.setIdentifier(new ProjectIdentifier("/ELN_SETTINGS/STORAGES"));

                final Experiment experiment = new Experiment();
                experiment.setCode("STORAGES_COLLECTION");
                experiment.setIdentifier(new ExperimentIdentifier("/ELN_SETTINGS/STORAGES/STORAGES_COLLECTION"));

                final Experiment defaultExperiment = new Experiment();
                defaultExperiment.setCode("DEFAULT");
                defaultExperiment.setIdentifier(new ExperimentIdentifier("/DEFAULT/DEFAULT/DEFAULT"));

                final Calendar calendar = Calendar.getInstance();
                calendar.set(2023, Calendar.MARCH, 10, 17, 23, 44);
                final Date registrationDate = calendar.getTime();

                calendar.set(2023, Calendar.MARCH, 11, 17, 23, 44);
                final Date modificationDate = calendar.getTime();

                final Person registrator = new Person();
                registrator.setUserId("system");

                final Person modifier = new Person();
                modifier.setUserId("test");

                final Sample[] samples = new Sample[1];

                samples[0] = new Sample();
                samples[0].setType(sampleType);
                samples[0].setFetchOptions(fetchOptions);
                samples[0].setPermId(new SamplePermId("200001010000000-0001"));
                samples[0].setCode("BENCH");
                samples[0].setIdentifier(new SampleIdentifier(space.getCode(), project.getCode(), null, "BENCH"));
                samples[0].setSpace(space);
                samples[0].setProject(project);
                samples[0].setExperiment(experiment);
                samples[0].setProperty("NAME", "<p>This is some text.</p>"
                        + "<figure class=\"image\">"
                        + "<img src=\"/openbis/openbis/file-service/eln-lims/7b/77/90/7b77903f-e685-4700-974a-5a5d7e109638/"
                        + "7b77903f-e685-4700-974a-5a5d7e109638.jpg\">"
                        + "</figure>"
                        + "<p>Then we have more text between images.</p>"
                        + "<figure class=\"image\">"
                        + "<img src=\"/openbis/openbis/file-service/eln-lims/08/b2/96/08b2968c-1685-4fa8-bef2-f5a80a8210ba/"
                        + "08b2968c-1685-4fa8-bef2-f5a80a8210ba.jpg\">"
                        + "</figure>"
                        + "<p>And some text at the end.</p>");
                samples[0].setProperty("EXPERIMENTAL_STEP.EXPERIMENTAL_DESCRIPTION", "<p>This is some text.</p>"
                        + "<figure class=\"image\">"
                        + "<img src=\"/openbis/openbis/file-service/eln-lims/c1/b2/91/"
                        + "c1b2912a-2ed6-40d6-8d9f-8c3ec2b29c5c/c1b2912a-2ed6-40d6-8d9f-8c3ec2b29c5c.jpg\">"
                        + "</figure>"
                        + "<p>Then we have more text between images.</p>"
                        + "<figure class=\"image\"><img src=\"/openbis/openbis/file-service/eln-lims/46/63/05/"
                        + "466305f0-4842-441f-b21c-777ea82079b4/466305f0-4842-441f-b21c-777ea82079b4.jpg\">"
                        + "</figure>"
                        + "<p>And some text at the end.</p>");
                samples[0].setProperty("EXPERIMENTAL_STEP.EXPERIMENTAL_GOALS", "<p>This is some text.</p><figure class=\"image\">"
                        + "<img src=\"/openbis/openbis/file-service/eln-lims/c0/1b/2e/c01b2e1f-8212-4562-ae8a-9072bf92e687/"
                        + "c01b2e1f-8212-4562-ae8a-9072bf92e687.jpg\">"
                        + "</figure>"
                        + "<p>Then we have more text between images.</p>"
                        + "<figure class=\"image\">"
                        + "<img src=\"/openbis/openbis/file-service/eln-lims/f3/e4/0c/f3e40c2e-109c-4191-bed0-2cf931de185a/"
                        + "f3e40c2e-109c-4191-bed0-2cf931de185a.jpg\">"
                        + "</figure>"
                        + "<p>And some text at the end.</p>");
                samples[0].setRegistrator(registrator);
                samples[0].setModifier(modifier);
                samples[0].setRegistrationDate(registrationDate);
                samples[0].setModificationDate(modificationDate);

                return Arrays.stream(samples).collect(Collectors.toMap(Sample::getPermId, Function.identity(),
                        (sample1, sample2) -> sample2, LinkedHashMap::new));
            }

            private PropertyAssignment getExperimentalDescriptionPropertyAssignment()
            {
                final PropertyType propertyType = new PropertyType();
                propertyType.setCode("EXPERIMENTAL_STEP.EXPERIMENTAL_DESCRIPTION");
                propertyType.setLabel("Experimental description");
                propertyType.setDescription("Experimental description");
                propertyType.setDataType(DataType.MULTILINE_VARCHAR);
                propertyType.setManagedInternally(false);
                propertyType.setMetaData(Map.of("custom_widget", "Word Processor"));

                final PropertyAssignment propertyAssignment = new PropertyAssignment();
                propertyAssignment.setFetchOptions(getPropertyAssignmentFetchOptions());
                propertyAssignment.setPropertyType(propertyType);
                propertyAssignment.setMandatory(false);
                propertyAssignment.setShowInEditView(true);
                propertyAssignment.setSection("General info");

                return propertyAssignment;
            }

            private PropertyAssignment getExperimentalGoalsPropertyAssignment()
            {
                final PropertyType propertyType = new PropertyType();
                propertyType.setCode("EXPERIMENTAL_STEP.EXPERIMENTAL_GOALS");
                propertyType.setLabel("Experimental goals");
                propertyType.setDescription("Experimental goals");
                propertyType.setDataType(DataType.MULTILINE_VARCHAR);
                propertyType.setManagedInternally(false);
                propertyType.setMetaData(Map.of("custom_widget", "")); // No "Word Processor" for this one.

                final PropertyAssignment propertyAssignment = new PropertyAssignment();
                propertyAssignment.setFetchOptions(getPropertyAssignmentFetchOptions());
                propertyAssignment.setPropertyType(propertyType);
                propertyAssignment.setMandatory(false);
                propertyAssignment.setShowInEditView(true);
                propertyAssignment.setSection("General info");

                return propertyAssignment;
            }

            private PropertyAssignment getNamePropertyAssignment()
            {
                final PropertyType propertyType = new PropertyType();
                propertyType.setCode("NAME");
                propertyType.setLabel("Name");
                propertyType.setDescription("Name");
                propertyType.setDataType(DataType.VARCHAR);
                propertyType.setManagedInternally(true);

                final PropertyAssignment propertyAssignment = new PropertyAssignment();
                propertyAssignment.setFetchOptions(getPropertyAssignmentFetchOptions());
                propertyAssignment.setPropertyType(propertyType);
                propertyAssignment.setMandatory(false);
                propertyAssignment.setShowInEditView(true);
                propertyAssignment.setSection("General info");

                return propertyAssignment;
            }

            private PropertyAssignmentFetchOptions getPropertyAssignmentFetchOptions()
            {
                final PropertyAssignmentFetchOptions fetchOptions = new PropertyAssignmentFetchOptions();
                fetchOptions.withPropertyType();
                return fetchOptions;
            }

        });

        allowing(api).searchSemanticAnnotations(with(XLSExportTest.SESSION_TOKEN), with(any(
                        SemanticAnnotationSearchCriteria.class)),
                with(any(SemanticAnnotationFetchOptions.class)));
        SearchResult<SemanticAnnotation> searchResult =
                new SearchResult<>(List.of(), 0);
        will(returnValue(searchResult));


    }

}
