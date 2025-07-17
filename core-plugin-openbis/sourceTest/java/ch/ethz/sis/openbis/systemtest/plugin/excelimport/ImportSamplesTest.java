/*
 * Copyright ETH 2018 - 2023 Zürich, Scientific IT Services
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
package ch.ethz.sis.openbis.systemtest.plugin.excelimport;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.DataType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.create.PropertyAssignmentCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.create.PropertyTypeCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.id.PropertyAssignmentPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.id.PropertyTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.create.SampleTypeCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.create.SemanticAnnotationCreation;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.id.IObjectId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.ISampleId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.generic.server.asapi.v3.IApplicationServerInternalApi;
import ch.systemsx.cisd.common.exceptions.UserFailureException;

import static org.testng.Assert.*;

@ContextConfiguration(locations = "classpath:applicationContext.xml")
@Transactional(transactionManager = "transaction-manager")
@Rollback
public class ImportSamplesTest extends AbstractImportTest
{
    @Autowired
    private IApplicationServerInternalApi v3api;

    private static final String SAMPLES_XLS = "samples/all_in.xls";

    private static final String SAMPLES_SPACE_ELSEWHERE = "samples/space_elsewhere.xls";

    private static final String SAMPLES_SAMPLE_TYPE_ELSWHERE = "samples/sample_type_elsewhere.xls";

    private static final String SAMPLES_SAMPLE_TYPE_ELSWHERE_CYCLIC = "samples/sample_type_elsewhere_cyclic.xls";

    private static final String SAMPLES_SAMPLE_TYPE_ELSWHERE_CYCLIC_WRONG_TYPE = "samples/sample_type_elsewhere_cyclic_wrong_type.xls";

    private static final String SAMPLES_SPACE_PROJECT_EXPERIMENT_ELSEWHERE = "samples/space_project_experiment_elsewhere.xls";

    private static final String SPACE = "samples/space.xls";

    private static final String SAMPLE_TYPE = "samples/sample_type.xls";

    private static final String SAMPLE_TYPE_CYCLIC = "samples/sample_type_cyclic.xls";

    private static final String SAMPLE_TYPE_CYCLIC_FIX_TYPE = "samples/sample_type_cyclic_fix_type.xls";

    private static final String VOCABULARY_TYPE = "samples/vocab_type.xls";

    private static final String CHILD_AS_IDENTIFIER = "samples/child_as_code.xls";

    private static final String CHILD_AS_DOLLARTAG = "samples/child_as_dollartag.xls";

    private static final String PARENT_AS_IDENTIFIER = "samples/parent_as_code.xls";

    private static final String PARENT_AS_DOLLARTAG = "samples/parent_as_dollartag.xls";

    private static final String MANDATORY_FIELD_MISSING = "samples/mandatory_field_missing.xls";

    private static final String NON_MANDATORY_FIELD_MISSING = "samples/non_mandatory_field_missing.xls";

    private static final String AUTO_GENERATED_SAMPLE_LEVEL = "samples/with_auto_generated_code_sample_level.xls";

    private static final String AUTO_GENERATED_SAMPLE_TYPE_LEVEL = "samples/with_auto_generated_code_sampletype_level.xls";

    private static final String GENERAL_ELN_SETTINGS = "samples/general_eln_settings.xlsx";

    private static final String GENERAL_ELN_SETTINGS_UPDATE = "samples/general_eln_settings_update.xlsx";

    private static final String SAMPLE_WITH_SEMANTIC_ANNOTATIONS = "samples/sample_with_semantic_annotations.xls";

    private static String FILES_DIR;

    @BeforeClass
    public void setupClass() throws IOException
    {
        String f = ImportSamplesTest.class.getName().replace(".", "/");
        FILES_DIR = f.substring(0, f.length() - ImportSamplesTest.class.getSimpleName().length()) + "/test_files/";
    }

    @Test
    @DirtiesContext
    public void testSamplesAreCreated() throws IOException
    {
        // the Excel contains internally managed property types which can be only manipulated by the system user
        sessionToken = v3api.loginAsSystem();

        // GIVEN
        final String sessionWorkspaceFile = uploadToAsSessionWorkspace(sessionToken, FilenameUtils.concat(FILES_DIR, SAMPLES_XLS));
        TestUtils.createFrom(v3api, sessionToken, UpdateMode.FAIL_IF_EXISTS, Paths.get(sessionWorkspaceFile));

        // WHEN
        Sample sample = TestUtils.getSample(v3api, sessionToken, "TEST_SPACE", "AAA");

        // THEN
        assertEquals(sample.getCode(), "AAA");
        assertEquals(sample.getProject(), null);
        assertEquals(sample.getExperiment().getCode(), "TEST_EXPERIMENT2");
        assertEquals(sample.getSpace().getCode(), "TEST_SPACE");
    }

    @Test
    @DirtiesContext
    public void testSamplesAreCreatedSecondSample() throws IOException
    {
        // the Excel contains internally managed property types which can be only manipulated by the system user
        sessionToken = v3api.loginAsSystem();

        // GIVEN
        final String sessionWorkspaceFilePath = uploadToAsSessionWorkspace(sessionToken, FilenameUtils.concat(FILES_DIR, SAMPLES_XLS));
        TestUtils.createFrom(v3api, sessionToken, Paths.get(sessionWorkspaceFilePath));
        // WHEN
        Sample sample = TestUtils.getSample(v3api, sessionToken, "TEST_SPACE", "VVV");
        // THEN
        assertEquals(sample.getCode(), "VVV");
        assertEquals(sample.getProject(), null);
        assertEquals(sample.getExperiment().getCode(), "TEST_EXPERIMENT");
        assertEquals(sample.getSpace().getCode(), "TEST_SPACE");
    }

    @Test
    @DirtiesContext
    public void testSamplesAreCreatedThirdSample() throws IOException
    {
        // the Excel contains internally managed property types which can be only manipulated by the system user
        sessionToken = v3api.loginAsSystem();

        // GIVEN
        final String sessionWorkspaceFilePath = uploadToAsSessionWorkspace(sessionToken, FilenameUtils.concat(FILES_DIR, SAMPLES_XLS));
        TestUtils.createFrom(v3api, sessionToken, Paths.get(sessionWorkspaceFilePath));
        // WHEN
        Sample sample = TestUtils.getSample(v3api, sessionToken, "TEST_SPACE", "S1");
        // THEN
        assertEquals(sample.getCode(), "S1");
        assertEquals(sample.getProject(), null);
        assertEquals(sample.getExperiment().getCode(), "TEST_EXPERIMENT");
        assertEquals(sample.getSpace().getCode(), "TEST_SPACE");
    }

    @Test
    @DirtiesContext
    public void testSamplesAreCreatedWhenSpaceOnServer() throws IOException
    {
        // the Excel contains internally managed property types which can be only manipulated by the system user
        String sessionToken = v3api.loginAsSystem();

        // GIVEN
        final String sessionWorkspaceFilePathForSpace = uploadToAsSessionWorkspace(sessionToken, FilenameUtils.concat(FILES_DIR, SPACE));
        TestUtils.createFrom(v3api, sessionToken, Paths.get(sessionWorkspaceFilePathForSpace));
        final String sessionWorkspaceFilePathForSamplesSpaceElsewhere = uploadToAsSessionWorkspace(sessionToken,
                FilenameUtils.concat(FILES_DIR, SAMPLES_SPACE_ELSEWHERE));
        TestUtils.createFrom(v3api, sessionToken, Paths.get(sessionWorkspaceFilePathForSamplesSpaceElsewhere));
        // WHEN
        Sample sample = TestUtils.getSample(v3api, sessionToken, "TEST_SPACE", "VVV");
        // THEN
        assertNotNull(sample);
    }

    @Test
    @DirtiesContext
    public void testSamplesAreCreatedWhenSpaceInSeparateXls() throws IOException
    {
        // the Excel contains internally managed property types which can be only manipulated by the system user
        String sessionToken = v3api.loginAsSystem();

        // GIVEN
        final String sessionWorkspaceFilePathForSpace = uploadToAsSessionWorkspace(sessionToken, FilenameUtils.concat(FILES_DIR, SPACE));
        final String sessionWorkspaceFilePathForSamplesSpaceElsewhere = uploadToAsSessionWorkspace(sessionToken,
                FilenameUtils.concat(FILES_DIR, SAMPLES_SPACE_ELSEWHERE));
        TestUtils.createFrom(v3api, sessionToken, Paths.get(sessionWorkspaceFilePathForSpace),
                Paths.get(sessionWorkspaceFilePathForSamplesSpaceElsewhere));
        // WHEN
        Sample sample = TestUtils.getSample(v3api, sessionToken, "TEST_SPACE", "VVV");
        // THEN
        assertNotNull(sample);
    }

    @Test(expectedExceptions = UserFailureException.class,
            expectedExceptionsMessageRegExp = "(s?).*Entity \\[TEST_SPACE\\] could not be found. "
                    + "Either you forgot to register it or mistyped the identifier.*")
    public void shouldThrowExceptionIfSpaceDoesntExist() throws IOException
    {
        // the Excel contains internally property types which can be only manipulated by the system user
        sessionToken = v3api.loginAsSystem();

        final String sessionWorkspaceFilePathForSamplesSpaceElsewhere = uploadToAsSessionWorkspace(sessionToken,
                FilenameUtils.concat(FILES_DIR, SAMPLES_SPACE_ELSEWHERE));
        TestUtils.createFrom(v3api, sessionToken, Paths.get(sessionWorkspaceFilePathForSamplesSpaceElsewhere));
    }

    @Test
    @DirtiesContext
    public void testSamplesAreCreatedWhenSampleTypeCyclicOnServer() throws IOException
    {
        // the Excel contains internally managed property types which can be only manipulated by the system user
        String sessionToken = v3api.loginAsSystem();

        // GIVEN
        final String sessionWorkspaceFilePathForVocabularyType = uploadToAsSessionWorkspace(sessionToken,
                FilenameUtils.concat(FILES_DIR, VOCABULARY_TYPE));
        final String sessionWorkspaceFilePathForSampleType = uploadToAsSessionWorkspace(sessionToken,
                FilenameUtils.concat(FILES_DIR, SAMPLE_TYPE_CYCLIC));
        TestUtils.createFrom(v3api, sessionToken, Paths.get(sessionWorkspaceFilePathForVocabularyType),
                Paths.get(sessionWorkspaceFilePathForSampleType));

        final String sessionWorkspaceFilePathForSampleTypeElsewhere = uploadToAsSessionWorkspace(sessionToken,
                FilenameUtils.concat(FILES_DIR, SAMPLES_SAMPLE_TYPE_ELSWHERE_CYCLIC));
        List<IObjectId> ids = TestUtils.createFrom(v3api, sessionToken, UpdateMode.UPDATE_IF_EXISTS,
                Paths.get(sessionWorkspaceFilePathForSampleTypeElsewhere));
        List<ISampleId> sampleIds = List.of((SampleIdentifier)ids.get(9), (SampleIdentifier)ids.get(10), (SampleIdentifier)ids.get(11));
        // WHEN
        List<Sample> samples = (List<Sample>) TestUtils.getSamplesById(v3api, sessionToken, sampleIds);
        Set<String> differentCyclicAssignments = new HashSet<>();
        for (Sample sample:samples) {
            differentCyclicAssignments.add((String)sample.getProperty("CYCLIC_SAMPLE_PROPERTY"));
        }
        // THEN
        assertEquals(samples.size(), 3);
        assertEquals(differentCyclicAssignments.size(), 2);
    }

    @Test
    @DirtiesContext
    public void testSamplesAreCreatedWhenSampleTypeCyclicOnServerFixType() throws IOException
    {
        // the Excel contains internally managed property types which can be only manipulated by the system user
        String sessionToken = v3api.loginAsSystem();

        // GIVEN
        final String sessionWorkspaceFilePathForVocabularyType = uploadToAsSessionWorkspace(sessionToken,
                FilenameUtils.concat(FILES_DIR, VOCABULARY_TYPE));
        final String sessionWorkspaceFilePathForSampleType = uploadToAsSessionWorkspace(sessionToken,
                FilenameUtils.concat(FILES_DIR, SAMPLE_TYPE_CYCLIC_FIX_TYPE));

        TestUtils.createFrom(v3api, sessionToken,
                Paths.get(sessionWorkspaceFilePathForVocabularyType),
                Paths.get(sessionWorkspaceFilePathForSampleType));

        final String sessionWorkspaceFilePathForSampleTypeElsewhere = uploadToAsSessionWorkspace(sessionToken,
                FilenameUtils.concat(FILES_DIR, SAMPLES_SAMPLE_TYPE_ELSWHERE_CYCLIC));
        List<IObjectId> ids = TestUtils.createFrom(v3api, sessionToken, UpdateMode.UPDATE_IF_EXISTS,
                Paths.get(sessionWorkspaceFilePathForSampleTypeElsewhere));
        List<ISampleId> sampleIds = List.of((SampleIdentifier)ids.get(9), (SampleIdentifier)ids.get(10), (SampleIdentifier)ids.get(11));
        // WHEN
        List<Sample> samples = TestUtils.getSamplesById(v3api, sessionToken, sampleIds);
        Set<String> differentCyclicAssignments = new HashSet<>();
        for (Sample sample:samples) {
            differentCyclicAssignments.add((String)sample.getProperty("CYCLIC_SAMPLE_PROPERTY"));
        }
        // THEN
        assertEquals(samples.size(), 3);
        assertEquals(differentCyclicAssignments.size(), 2);
    }

    @Test(expectedExceptions = UserFailureException.class,
            expectedExceptionsMessageRegExp = "(?s).*Property CYCLIC_SAMPLE_PROPERTY is not a sample of type ANTIBODY but of type TEST_TYPE.*")
    @DirtiesContext
    public void testSamplesAreCreatedWhenSampleTypeCyclicOnServerFixTypeWrongType() throws IOException
    {
        // the Excel contains internally managed property types which can be only manipulated by the system user
        String sessionToken = v3api.loginAsSystem();

        // GIVEN
        final String sessionWorkspaceFilePathForVocabularyType = uploadToAsSessionWorkspace(sessionToken,
                FilenameUtils.concat(FILES_DIR, VOCABULARY_TYPE));
        final String sessionWorkspaceFilePathForSampleType = uploadToAsSessionWorkspace(sessionToken,
                FilenameUtils.concat(FILES_DIR, SAMPLE_TYPE_CYCLIC_FIX_TYPE));

        TestUtils.createFrom(v3api, sessionToken,
                Paths.get(sessionWorkspaceFilePathForVocabularyType),
                Paths.get(sessionWorkspaceFilePathForSampleType));

        final String sessionWorkspaceFilePathForSampleTypeElsewhere = uploadToAsSessionWorkspace(sessionToken,
                FilenameUtils.concat(FILES_DIR, SAMPLES_SAMPLE_TYPE_ELSWHERE_CYCLIC_WRONG_TYPE));
        TestUtils.createFrom(v3api, sessionToken, UpdateMode.UPDATE_IF_EXISTS, Paths.get(sessionWorkspaceFilePathForSampleTypeElsewhere));
    }

    @Test
    @DirtiesContext
    public void testSamplesAreCreatedWhenSampleTypeOnServer() throws IOException
    {
        // the Excel contains internally managed property types which can be only manipulated by the system user
        String sessionToken = v3api.loginAsSystem();

        // GIVEN
        final String sessionWorkspaceFilePathForVocabularyType = uploadToAsSessionWorkspace(sessionToken,
                FilenameUtils.concat(FILES_DIR, VOCABULARY_TYPE));
        final String sessionWorkspaceFilePathForSampleType = uploadToAsSessionWorkspace(sessionToken,
                FilenameUtils.concat(FILES_DIR, SAMPLE_TYPE));
        TestUtils.createFrom(v3api, sessionToken,
                Paths.get(sessionWorkspaceFilePathForVocabularyType),
                Paths.get(sessionWorkspaceFilePathForSampleType));

        final String sessionWorkspaceFilePathForSampleTypeElsewhere = uploadToAsSessionWorkspace(sessionToken,
                FilenameUtils.concat(FILES_DIR, SAMPLES_SAMPLE_TYPE_ELSWHERE));
        TestUtils.createFrom(v3api, sessionToken, Paths.get(sessionWorkspaceFilePathForSampleTypeElsewhere));
        // WHEN
        Sample sample = TestUtils.getSample(v3api, sessionToken, "TEST_SPACE", "VVV");
        // THEN
        assertNotNull(sample);
    }

    @Test
    @DirtiesContext
    public void testSamplesAreCreatedWhenSampleTypeInSeparateXls() throws IOException
    {
        // the Excel contains internally managed property types which can be only manipulated by the system user
        sessionToken = v3api.loginAsSystem();

        // GIVEN
        final String sessionWorkspaceFilePathForSampleType = uploadToAsSessionWorkspace(sessionToken,
                FilenameUtils.concat(FILES_DIR, SAMPLE_TYPE));
        final String sessionWorkspaceFilePathForSampleTypeElsewhere = uploadToAsSessionWorkspace(sessionToken,
                FilenameUtils.concat(FILES_DIR, SAMPLES_SAMPLE_TYPE_ELSWHERE));
        TestUtils.createFrom(v3api, sessionToken, Paths.get(sessionWorkspaceFilePathForSampleType),
                Paths.get(sessionWorkspaceFilePathForSampleTypeElsewhere));
        // WHEN
        Sample sample = TestUtils.getSample(v3api, sessionToken, "TEST_SPACE", "VVV");
        // THEN
        assertNotNull(sample);
    }

    @Test
    @DirtiesContext
    public void testSamplesChildrenAreAssignedWhenAddressedByIdentifierInXls() throws IOException
    {
        // the Excel contains internally managed property types which can be only manipulated by the system user
        String sessionToken = v3api.loginAsSystem();

        // GIVEN
        final String sessionWorkspaceFile = uploadToAsSessionWorkspace(sessionToken, FilenameUtils.concat(FILES_DIR, CHILD_AS_IDENTIFIER));
        TestUtils.createFrom(v3api, sessionToken, Paths.get(sessionWorkspaceFile));
        // WHEN
        Sample sample = TestUtils.getSample(v3api, sessionToken, "TEST_SPACE", "VVV");
        // THEN
        assertNotNull(sample);
        assertEquals(sample.getChildren().size(), 1);
        assertEquals(sample.getChildren().get(0).getCode(), "AAA");
        assertEquals(sample.getChildren().get(0).getSpace().getCode(), "TEST_SPACE");
        assertEquals(sample.getChildren().get(0).getExperiment().getCode(), "TEST_EXPERIMENT2");
    }

    @Test
    @DirtiesContext
    public void testSamplesParentsAreAssignedWhenAddressedByIdentifierInXls() throws IOException
    {
        // the Excel contains internally managed property types which can be only manipulated by the system user
        String sessionToken = v3api.loginAsSystem();

        // GIVEN
        final String sessionWorkspaceFile = uploadToAsSessionWorkspace(sessionToken, FilenameUtils.concat(FILES_DIR, PARENT_AS_IDENTIFIER));
        TestUtils.createFrom(v3api, sessionToken, Paths.get(sessionWorkspaceFile));
        // WHEN
        Sample sample = TestUtils.getSample(v3api, sessionToken, "TEST_SPACE", "VVV");
        // THEN
        assertNotNull(sample);
        assertEquals(sample.getParents().size(), 1);
        assertEquals(sample.getParents().get(0).getCode(), "AAA");
        assertEquals(sample.getParents().get(0).getSpace().getCode(), "TEST_SPACE");
        assertEquals(sample.getParents().get(0).getExperiment().getCode(), "TEST_EXPERIMENT2");
    }

    @Test
    @DirtiesContext
    public void testSamplesChildrenAreAssignedWhenAddressedByDollartagInXls() throws IOException
    {
        // the Excel contains internally managed property types which can be only manipulated by the system user
        String sessionToken = v3api.loginAsSystem();

        // GIVEN
        final String sessionWorkspaceFile = uploadToAsSessionWorkspace(sessionToken, FilenameUtils.concat(FILES_DIR, CHILD_AS_DOLLARTAG));
        TestUtils.createFrom(v3api, sessionToken, Paths.get(sessionWorkspaceFile));
        // WHEN
        Sample sample = TestUtils.getSample(v3api, sessionToken, "TEST_SPACE", "VVV");
        // THEN
        assertNotNull(sample);
        assertEquals(sample.getChildren().size(), 1);
        assertEquals(sample.getChildren().get(0).getCode(), "AAA");
        assertEquals(sample.getChildren().get(0).getSpace().getCode(), "TEST_SPACE");
        assertEquals(sample.getChildren().get(0).getExperiment().getCode(), "TEST_EXPERIMENT2");
    }

    @Test
    @DirtiesContext
    public void testSamplesParentsAreAssignedWhenAddressedByDollartagInXls() throws IOException
    {
        // the Excel contains internally managed property types which can be only manipulated by the system user
        String sessionToken = v3api.loginAsSystem();

        // GIVEN
        final String sessionWorkspaceFile = uploadToAsSessionWorkspace(sessionToken, FilenameUtils.concat(FILES_DIR, PARENT_AS_DOLLARTAG));
        TestUtils.createFrom(v3api, sessionToken, Paths.get(sessionWorkspaceFile));
        // WHEN
        Sample sample = TestUtils.getSample(v3api, sessionToken, "TEST_SPACE", "VVV");
        // THEN
        assertNotNull(sample);
        assertEquals(sample.getParents().size(), 1);
        assertEquals(sample.getParents().get(0).getCode(), "AAA");
        assertEquals(sample.getParents().get(0).getSpace().getCode(), "TEST_SPACE");
        assertEquals(sample.getParents().get(0).getExperiment().getCode(), "TEST_EXPERIMENT2");
    }

    @Test
    @DirtiesContext
    public void testCreatesSampleWithNonMandatoryFieldsMissing() throws IOException
    {
        // the Excel contains internally managed property types which can be only manipulated by the system user
        sessionToken = v3api.loginAsSystem();

        // GIVEN
        final String sessionWorkspaceFile = uploadToAsSessionWorkspace(sessionToken, FilenameUtils.concat(FILES_DIR, NON_MANDATORY_FIELD_MISSING));
        TestUtils.createFrom(v3api, sessionToken, Paths.get(sessionWorkspaceFile));
        // WHEN
        Sample sample = TestUtils.getSample(v3api, sessionToken, "TEST_SPACE", "AAA");
        // THEN
        assertNotNull(sample);
        assertEquals(sample.getProperties().get("FOR_WHAT"), null);
    }

    @Test
    @DirtiesContext
    public void testCreatesSampleWithAutogeneratedCodeWhenOnPerSampleLevel() throws IOException
    {
        // the Excel contains internally managed property types which can be only manipulated by the system user
        sessionToken = v3api.loginAsSystem();

        // GIVEN
        final String sessionWorkspaceFilePath = uploadToAsSessionWorkspace(sessionToken,
                FilenameUtils.concat(FILES_DIR, AUTO_GENERATED_SAMPLE_LEVEL));
        List<IObjectId> result = TestUtils.createFrom(v3api, sessionToken, UpdateMode.IGNORE_EXISTING, Paths.get(sessionWorkspaceFilePath));
        String permId = result.get(result.size() - 1).toString();

        // WHEN
        Sample sample = TestUtils.getSampleById(v3api, sessionToken, permId);
        // THEN
        assertNotNull(sample.getCode());
        assertEquals(sample.getType().getCode(), "ANTIBODY");
    }

    @Test
    @DirtiesContext
    public void testCreatesSampleWithAutogeneratedCodeWhenOnSampleTypeLevel() throws IOException
    {
        // the Excel contains internally managed property types which can be only manipulated by the system user
        sessionToken = v3api.loginAsSystem();

        // GIVEN
        final String sessionWorkspaceFilePath = uploadToAsSessionWorkspace(sessionToken,
                FilenameUtils.concat(FILES_DIR, AUTO_GENERATED_SAMPLE_TYPE_LEVEL));
        List<IObjectId> result = TestUtils.createFrom(v3api, sessionToken, UpdateMode.IGNORE_EXISTING, Paths.get(sessionWorkspaceFilePath));

        String permId = result.get(result.size() - 1).toString();
        // WHEN
        Sample sample = TestUtils.getSampleById(v3api, sessionToken, permId);
        // THEN
        assertNotNull(sample);
        assertEquals(sample.getType().getCode(), "ANTIBODY");
    }

    @Test
    @DirtiesContext
    public void testSampleIsUpdateByXlsParser() throws IOException
    {
        // the Excel contains internally managed property types which can be only manipulated by the system user
        sessionToken = v3api.loginAsSystem();

        final String sessionWorkspaceFilePath = uploadToAsSessionWorkspace(sessionToken, FilenameUtils.concat(FILES_DIR, GENERAL_ELN_SETTINGS));
        TestUtils.createFrom(v3api, sessionToken, Paths.get(sessionWorkspaceFilePath));

        // test sample before update
        Sample sample = TestUtils.getSample(v3api, sessionToken, "ELN_SETTINGS", "GENERAL_ELN_SETTINGS");
        assertNotNull(sample);
        // properties are empty
        assertEquals(sample.getProperties().size(), 0);

        // test space before update
        Space space = TestUtils.getSpace(v3api, sessionToken, "ELN_SETTINGS");
        assertEquals(space.getDescription(), "ELN Settings");

        // test project before update
        Project project = TestUtils.getProject(v3api, sessionToken, "DEFAULT_PROJECT");
        assertEquals(project.getDescription(), "Default Project");

        // test experiment before update
        Experiment experiment = TestUtils.getExperiment(v3api, sessionToken, "DEFAULT_EXPERIMENT", "DEFAULT_PROJECT", "ELN_SETTINGS");
        assertEquals(experiment.getProperties().size(), 1);
        assertEquals(experiment.getProperties().containsKey("NAME"), true);
        assertEquals(experiment.getProperties().get("NAME"), "Default Experiment");

        final String sessionWorkspaceFilePathForUpdate = uploadToAsSessionWorkspace(sessionToken,
                FilenameUtils.concat(FILES_DIR, GENERAL_ELN_SETTINGS_UPDATE));
        TestUtils.createFrom(v3api, sessionToken, UpdateMode.UPDATE_IF_EXISTS, Paths.get(sessionWorkspaceFilePathForUpdate));

        // test sample after update
        sample = TestUtils.getSample(v3api, sessionToken, "ELN_SETTINGS", "GENERAL_ELN_SETTINGS");
        assertNotNull(sample);
        // properties have been updated
        assertEquals(sample.getProperties().size(), 1);
        assertEquals(sample.getProperties().containsKey("ELN_SETTINGS"), true);
        assertEquals(sample.getProperties().get("ELN_SETTINGS"), "{}");

        // test space after update
        space = TestUtils.getSpace(v3api, sessionToken, "ELN_SETTINGS");
        // test space before update
        assertEquals(space.getDescription(), "ELN Settings Updated");

        // test project after update
        project = TestUtils.getProject(v3api, sessionToken, "DEFAULT_PROJECT");
        assertEquals(project.getDescription(), "Default Project Updated");

        // test experiment after update
        experiment = TestUtils.getExperiment(v3api, sessionToken, "DEFAULT_EXPERIMENT", "DEFAULT_PROJECT", "ELN_SETTINGS");
        assertEquals(experiment.getProperties().size(), 1);
        assertEquals(experiment.getProperties().containsKey("NAME"), true);
        assertEquals(experiment.getProperties().get("NAME"), "Default Experiment Updated");
    }

    @Test(expectedExceptions = UserFailureException.class,
            expectedExceptionsMessageRegExp = "(s?).*Entity \\[TEST_SPACE, /TEST_SPACE/TEST_PROJECT/TEST_EXPERIMENT\\] could not be found. "
                    + "Either you forgot to register it or mistyped the identifier.*")
    public void shouldThrowExceptionIfSamplesSpaceProjectDoesntExist() throws IOException
    {
        // the Excel contains internally property types which can be only manipulated by the system user
        sessionToken = v3api.loginAsSystem();

        final String sessionWorkspaceFilePath = uploadToAsSessionWorkspace(sessionToken,
                FilenameUtils.concat(FILES_DIR, SAMPLES_SPACE_PROJECT_EXPERIMENT_ELSEWHERE));
        TestUtils.createFrom(v3api, sessionToken, Paths.get(sessionWorkspaceFilePath));
    }

    @Test(expectedExceptions = UserFailureException.class,
            expectedExceptionsMessageRegExp = "(s?).*Header 'name' is neither an attribute, property code or property label.*")
    public void shouldThrowExceptionIfMandatoryPropertyIsMissing() throws IOException
    {
        // the Excel contains internally property types which can be only manipulated by the system user
        sessionToken = v3api.loginAsSystem();

        final String sessionWorkspaceFilePath = uploadToAsSessionWorkspace(sessionToken, FilenameUtils.concat(FILES_DIR, MANDATORY_FIELD_MISSING));
        TestUtils.createFrom(v3api, sessionToken, Paths.get(sessionWorkspaceFilePath));
    }

    @Test
    @DirtiesContext
    public void testCreatesSampleWithTypeThatIsFoundBySemanticAnnotation() throws IOException
    {
        // the Excel contains internally managed property types which can be only manipulated by the system user
        sessionToken = v3api.loginAsSystem();

        // Create property types PROPERTY_TYPE_FOR_ASSIGNMENT PROPERTY_TYPE
        final String propertyTypeForAssignmentCode = "PROPERTY_TYPE_FOR_ASSIGNMENT";
        final String propertyTypeCode = "PROPERTY_TYPE";
        PropertyTypeCreation ptCreation = new PropertyTypeCreation();
        ptCreation.setCode(propertyTypeCode);
        ptCreation.setDataType(DataType.VARCHAR);
        ptCreation.setLabel("property type");
        ptCreation.setDescription("some description");

        PropertyTypeCreation ptfaCreation = new PropertyTypeCreation();
        ptfaCreation.setCode(propertyTypeForAssignmentCode);
        ptfaCreation.setDataType(DataType.VARCHAR);
        ptfaCreation.setLabel("property type for assignment");
        ptfaCreation.setDescription("some description");

        v3api.createPropertyTypes(sessionToken, List.of(ptCreation, ptfaCreation));

        // Create sample type ANTIBODY
        final String sampleTypeCode = "ANTIBODY";
        final EntityTypePermId typePermId = new EntityTypePermId(sampleTypeCode, EntityKind.SAMPLE);
        SampleTypeCreation creation = new SampleTypeCreation();
        creation.setCode(sampleTypeCode);
        creation.setAutoGeneratedCode(true);

        PropertyAssignmentCreation assignment = new PropertyAssignmentCreation();
        assignment.setPropertyTypeId(new PropertyTypePermId(propertyTypeForAssignmentCode));
        assignment.setMandatory(false);
        assignment.setSection("General");
        assignment.setShowInEditView(true);

        creation.setPropertyAssignments(List.of(assignment));
        v3api.createSampleTypes(sessionToken, List.of(creation));

        // Create semantic annotations
        //For Sample Type
        SemanticAnnotationCreation annotationSample = new SemanticAnnotationCreation();
        annotationSample.setEntityTypeId(typePermId);
        annotationSample.setPredicateOntologyId("https://en.wikipedia.org/");
        annotationSample.setPredicateOntologyVersion("https://en.wikipedia.org/wiki/Wikipedia:About");
        annotationSample.setPredicateAccessionId("https://en.wikipedia.org/wiki/Antibody");
        annotationSample.setDescriptorOntologyId("https://en.wikipedia.org/");
        annotationSample.setDescriptorOntologyVersion("https://en.wikipedia.org/wiki/Wikipedia:About");
        annotationSample.setDescriptorAccessionId("https://en.wikipedia.org/wiki/Antibody");

        //For Property Type
        SemanticAnnotationCreation annotationProperty = new SemanticAnnotationCreation();
        annotationProperty.setPropertyTypeId(new PropertyTypePermId(propertyTypeCode));
        annotationProperty.setPredicateOntologyId("https://en.wikipedia.org/");
        annotationProperty.setPredicateOntologyVersion("https://en.wikipedia.org/wiki/Wikipedia:About");
        annotationProperty.setPredicateAccessionId("https://en.wikipedia.org/wiki/PropertyType");
        annotationProperty.setDescriptorOntologyId("https://en.wikipedia.org/");
        annotationProperty.setDescriptorOntologyVersion("https://en.wikipedia.org/wiki/Wikipedia:About");
        annotationProperty.setDescriptorAccessionId("https://en.wikipedia.org/wiki/PropertyType");

        //For Property Assignment
        SemanticAnnotationCreation annotationPropertyAssignment = new SemanticAnnotationCreation();
        PropertyAssignmentPermId id = new PropertyAssignmentPermId(typePermId, new PropertyTypePermId(propertyTypeForAssignmentCode));
        annotationPropertyAssignment.setPropertyAssignmentId(id);
        annotationPropertyAssignment.setPredicateOntologyId("https://en.wikipedia.org/");
        annotationPropertyAssignment.setPredicateOntologyVersion("https://en.wikipedia.org/wiki/Wikipedia:About");
        annotationPropertyAssignment.setPredicateAccessionId("https://en.wikipedia.org/wiki/PropertyAssignment");
        annotationPropertyAssignment.setDescriptorOntologyId("https://en.wikipedia.org/");
        annotationPropertyAssignment.setDescriptorOntologyVersion("https://en.wikipedia.org/wiki/Wikipedia:About");
        annotationPropertyAssignment.setDescriptorAccessionId("https://en.wikipedia.org/wiki/PropertyAssignment");

        v3api.createSemanticAnnotations(sessionToken, List.of(annotationSample, annotationProperty, annotationPropertyAssignment));


        // GIVEN
        final String sessionWorkspaceFilePath = uploadToAsSessionWorkspace(sessionToken,
                FilenameUtils.concat(FILES_DIR,
                        SAMPLE_WITH_SEMANTIC_ANNOTATIONS));

        TestUtils.createFrom(v3api, sessionToken, UpdateMode.UPDATE_IF_EXISTS, Paths.get(sessionWorkspaceFilePath));

        // THEN
        SampleType antibodyThatShouldNotExists = TestUtils.getSampleType(v3api, sessionToken, "ANTIBODY_V2");
        assertNull(antibodyThatShouldNotExists);

        SampleType antibody = TestUtils.getSampleType(v3api, sessionToken, "ANTIBODY");
        assertNotNull(antibody);

        List<PropertyAssignment> assignments = antibody.getPropertyAssignments();
        assertEquals(assignments.size(), 3);

        Set<String> codes = assignments.stream().map(x -> x.getPropertyType().getCode()).collect(Collectors.toSet());
        assertEquals(codes, Set.of("NAME", propertyTypeCode, propertyTypeForAssignmentCode));


        // WHEN
        Sample sample = TestUtils.getSample(v3api, sessionToken, "PUBLICATIONS", "PUBCREA22") ;
        // THEN
        assertNotNull(sample);
        assertEquals(sample.getType().getCode(), "ANTIBODY");

        assertEquals(sample.getStringProperty("NAME"), "New Name");
        assertEquals(sample.getStringProperty("PROPERTY_TYPE"), "Some_identifier");
        assertEquals(sample.getStringProperty("PROPERTY_TYPE_FOR_ASSIGNMENT"), "property_type_for_assignment_value");

    }

}
