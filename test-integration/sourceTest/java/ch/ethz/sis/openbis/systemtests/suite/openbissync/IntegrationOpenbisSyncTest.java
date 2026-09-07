/*
 * Copyright ETH 2024 Zürich, Scientific IT Services
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
package ch.ethz.sis.openbis.systemtests.suite.openbissync;

import static ch.ethz.sis.openbis.systemtests.suite.openbissync.environment.OpenbisSyncIntegrationTestEnvironment.INSTANCE_ADMIN;
import static ch.ethz.sis.openbis.systemtests.suite.openbissync.environment.OpenbisSyncIntegrationTestEnvironment.NAME_PREFIX;
import static ch.ethz.sis.openbis.systemtests.suite.openbissync.environment.OpenbisSyncIntegrationTestEnvironment.PASSWORD;
import static ch.ethz.sis.openbis.systemtests.suite.openbissync.environment.OpenbisSyncIntegrationTestEnvironment.environment;
import static ch.ethz.sis.openbis.systemtests.suite.openbissync.environment.OpenbisSyncIntegrationTestEnvironment.source;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.search.ExperimentSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportablePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.fetchoptions.ProjectFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.search.ProjectSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.DataType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.create.PropertyAssignmentCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.create.PropertyTypeCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.fetchoptions.PropertyTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.id.PropertyTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.create.SampleCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.create.SampleTypeCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.fetchoptions.SpaceFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import ch.ethz.sis.openbis.generic.server.dss.plugins.sync.harvester.HarvesterMaintenanceTask;
import ch.ethz.sis.openbis.generic.server.dss.plugins.sync.harvester.config.BasicAuthCredentials;
import ch.ethz.sis.openbis.generic.server.dss.plugins.sync.harvester.synchronizer.datasourceconnector.DataSourceConnector;
import ch.ethz.sis.openbis.generic.server.dss.plugins.sync.harvester.synchronizer.datasourceconnector.IDataSourceConnector;
import ch.ethz.sis.openbis.systemtests.environment.IntegrationTestFacade;
import ch.ethz.sis.openbis.systemtests.suite.openbissync.environment.OpenbisSyncIntegrationTestEnvironment;
import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.ethz.sis.shared.log.classic.impl.Logger;
import ch.systemsx.cisd.openbis.generic.shared.util.TestInstanceHostUtils;

/**
 * End-to-end openBIS sync test between two independent instances.
 *
 */
public class IntegrationOpenbisSyncTest
{

    // --- Scenario: a SPACE with a full subtree below it (select the space, get everything) ---

    private static final String SPACE_CODE = "SYNC_SPACE";

    private static final String PROJECT_CODE = "SYNC_PROJECT";

    private static final String EXPERIMENT_CODE = "SYNC_EXPERIMENT";

    /** Sample sitting directly in the space (no project, no experiment). */
    private static final String SPACE_SAMPLE_CODE = "SYNC_SAMPLE_SPACE";

    /** Sample sitting in the project (no experiment). */
    private static final String PROJECT_SAMPLE_CODE = "SYNC_SAMPLE_PROJECT";

    /** Sample sitting in the experiment. */
    private static final String EXPERIMENT_SAMPLE_CODE = "SYNC_SAMPLE_EXPERIMENT";

    // --- Scenario: a top-level PROJECT in its own space ---

    private static final String TOP_PROJECT_SPACE_CODE = "SYNC_TOP_PROJECT_SPACE";

    private static final String TOP_PROJECT_CODE = "SYNC_TOP_PROJECT";

    // --- Scenario: a top-level EXPERIMENT in its own space ---

    private static final String TOP_EXPERIMENT_SPACE_CODE = "SYNC_TOP_EXPERIMENT_SPACE";

    private static final String TOP_EXPERIMENT_PROJECT_CODE = "SYNC_TOP_EXPERIMENT_PROJECT";

    private static final String TOP_EXPERIMENT_CODE = "SYNC_TOP_EXPERIMENT";

    // --- Scenario: two top-level SAMPLEs sharing one space ---

    private static final String TOP_SAMPLE_SPACE_CODE = "SYNC_TOP_SAMPLE_SPACE";

    private static final String TOP_SAMPLE_CODE = "SYNC_TOP_SAMPLE";

    /** Second sample in the same space as {@link #TOP_SAMPLE_CODE}, selected as its own separate root. */
    private static final String TOP_SAMPLE_CODE_2 = "SYNC_TOP_SAMPLE_2";
    private static final String TOP_SAMPLE_CODE_3 = "SYNC_TOP_SAMPLE_3";

    // --- Scenario: entity/property type schema features (BIS-2820 ext.4: multiValue, metaData, unique) ---

    private static final String SCHEMA_FEATURES_SPACE_CODE = "SYNC_SCHEMA_FEATURES_SPACE";

    private static final String SCHEMA_FEATURES_PROPERTY_TYPE_CODE = "SYNC_SCHEMA_FEATURES_PROPERTY";

    private static final String SCHEMA_FEATURES_SAMPLE_TYPE_CODE = "SYNC_SCHEMA_FEATURES_SAMPLE_TYPE";

    private static final String SCHEMA_FEATURES_SAMPLE_CODE = "SYNC_SCHEMA_FEATURES_SAMPLE";

    private static final Map<String, String> SCHEMA_FEATURES_PROPERTY_TYPE_METADATA = Map.of("unit", "nm");

    private static final Map<String, String> SCHEMA_FEATURES_SAMPLE_TYPE_METADATA = Map.of("source", "sync-test");

    // --- Scenario: AFS data attached to a sample and an experiment (BIS-2819 ext.3: AFS sync) ---

    private static final String AFS_SAMPLE_SPACE_CODE = "SYNC_AFS_SAMPLE_SPACE";

    private static final String AFS_SAMPLE_CODE = "SYNC_AFS_SAMPLE";

    private static final String AFS_EXPERIMENT_SPACE_CODE = "SYNC_AFS_EXPERIMENT_SPACE";

    private static final String AFS_EXPERIMENT_PROJECT_CODE = "SYNC_AFS_EXPERIMENT_PROJECT";

    private static final String AFS_EXPERIMENT_CODE = "SYNC_AFS_EXPERIMENT";

    private static final String AFS_SAMPLE_FILE = "sample-file.txt";

    private static final byte[] AFS_SAMPLE_FILE_CONTENT = "sample AFS content".getBytes();

    private static final String AFS_EXPERIMENT_FILE = "experiment-file.txt";

    private static final byte[] AFS_EXPERIMENT_FILE_CONTENT = "experiment AFS content".getBytes();

    private static final String HARVESTER_CONFIG_FILE = "etc/suite/openbis-sync/harvester/dss/harvester-config.txt";

    private static final long SYNC_VERIFICATION_TIMEOUT_MILLIS = 60_000L;

    @BeforeSuite
    public void beforeSuite()
    {
        OpenbisSyncIntegrationTestEnvironment.start();
    }

    @AfterSuite
    public void afterSuite()
    {
        OpenbisSyncIntegrationTestEnvironment.stop();
    }

    @Test
    public void testHarvestAllSelectionsInOneSync() throws Exception
    {
        IntegrationTestFacade facade = new IntegrationTestFacade(environment);
        OpenBIS sourceOpenBIS = sourceLogin();
        OpenBIS harvester = harvesterLogin();

        // Build each scenario on the source and collect its exportable-perm-id tokens for one combined harvest.
        List<String> exportablePermIds = new ArrayList<>();
        exportablePermIds.addAll(createSpaceSubtree(facade, sourceOpenBIS));
        exportablePermIds.addAll(createTopLevelProject(facade, sourceOpenBIS));
        exportablePermIds.addAll(createTopLevelExperiment(facade, sourceOpenBIS));
        exportablePermIds.addAll(createTwoTopLevelSamplesSharingSpace(facade, sourceOpenBIS));

        // Sanity: nothing has been harvested yet.
        assertNull(findHarvestedSpace(harvester, NAME_PREFIX + SPACE_CODE),
                "harvested space must not exist before sync");

        runHarvester(exportablePermIds);

        // Wait until the deepest entity of the largest subtree is in, then assert every scenario.
        facade.waitUntilCondition(
                () -> findHarvestedSample(harvester, NAME_PREFIX + SPACE_CODE, EXPERIMENT_SAMPLE_CODE) != null,
                SYNC_VERIFICATION_TIMEOUT_MILLIS);

        verifySpaceSubtree(harvester);
        verifyTopLevelProject(harvester);
        verifyTopLevelExperiment(harvester);
        verifyTwoTopLevelSamplesSharingSpace(harvester);
    }

    @Test(dependsOnMethods = "testHarvestAllSelectionsInOneSync")
    public void testResourceListWithoutExportablePermIdsContainsNoEntityData() throws Exception
    {
        List<String> locations = fetchResourceListLocations(Collections.emptyList());

        assertTrue(locations.stream().anyMatch(loc -> loc.contains("/MASTER_DATA/")),
                "master data must always be delivered, regardless of the selection: " + locations);
        assertTrue(locations.stream().noneMatch(loc -> loc.matches(".*/(SPACE|PROJECT|EXPERIMENT|SAMPLE|DATA_SET)/.*")),
                "no entity data must be delivered without an 'exportable_perm_id' selection: " + locations);
    }

    @Test
    public void testHarvestEntityAndPropertyTypeSchemaFeatures() throws Exception
    {
        IntegrationTestFacade facade = new IntegrationTestFacade(environment);
        OpenBIS sourceOpenBIS = sourceLogin();
        OpenBIS harvester = harvesterLogin();

        List<String> exportablePermIds = createSchemaFeaturesScenario(facade, sourceOpenBIS);

        assertNull(findHarvestedSpace(harvester, NAME_PREFIX + SCHEMA_FEATURES_SPACE_CODE),
                "harvested schema-features space must not exist before sync");

        runHarvester(exportablePermIds);

        facade.waitUntilCondition(
                () -> findHarvestedSample(harvester, NAME_PREFIX + SCHEMA_FEATURES_SPACE_CODE, SCHEMA_FEATURES_SAMPLE_CODE) != null,
                SYNC_VERIFICATION_TIMEOUT_MILLIS);

        verifySchemaFeatures(harvester);
    }

    @Test
    public void testHarvestAfsDataAttachedToSampleAndExperiment() throws Exception
    {
        IntegrationTestFacade facade = new IntegrationTestFacade(environment);
        OpenBIS sourceOpenBIS = sourceLogin();
        OpenBIS harvester = harvesterLogin();

        Space sampleSpace = facade.createSpace(sourceOpenBIS, AFS_SAMPLE_SPACE_CODE);
        Sample sample = facade.createSample(sourceOpenBIS, sampleSpace.getPermId(), AFS_SAMPLE_CODE);
        sourceOpenBIS.getAfsServerFacade().write(sample.getPermId().getPermId(), AFS_SAMPLE_FILE, 0L, AFS_SAMPLE_FILE_CONTENT);

        Space experimentSpace = facade.createSpace(sourceOpenBIS, AFS_EXPERIMENT_SPACE_CODE);
        Project experimentProject = facade.createProject(sourceOpenBIS, experimentSpace.getPermId(), AFS_EXPERIMENT_PROJECT_CODE);
        Experiment experiment = facade.createExperiment(sourceOpenBIS, experimentProject.getPermId(), AFS_EXPERIMENT_CODE);
        sourceOpenBIS.getAfsServerFacade().write(experiment.getPermId().getPermId(), AFS_EXPERIMENT_FILE, 0L, AFS_EXPERIMENT_FILE_CONTENT);

        List<String> exportablePermIds = List.of(
                "SAMPLE:" + sample.getPermId().getPermId(),
                "EXPERIMENT:" + experiment.getPermId().getPermId());

        runHarvester(exportablePermIds);

        facade.waitUntilCondition(
                () -> findHarvestedSample(harvester, NAME_PREFIX + AFS_SAMPLE_SPACE_CODE, AFS_SAMPLE_CODE) != null,
                SYNC_VERIFICATION_TIMEOUT_MILLIS);

        assertEquals(harvester.getAfsServerFacade().read(sample.getPermId().getPermId(), AFS_SAMPLE_FILE, 0L, AFS_SAMPLE_FILE_CONTENT.length),
                AFS_SAMPLE_FILE_CONTENT, "harvested sample AFS file content");
        assertEquals(harvester.getAfsServerFacade().read(experiment.getPermId().getPermId(), AFS_EXPERIMENT_FILE, 0L, AFS_EXPERIMENT_FILE_CONTENT.length),
                AFS_EXPERIMENT_FILE_CONTENT, "harvested experiment AFS file content");
    }

    // --- Scenario builders: create the source hierarchy, return the exportable-perm-id tokens to harvest ---

    private List<String> createSpaceSubtree(IntegrationTestFacade facade, OpenBIS sourceOpenBIS)
    {
        Space space = facade.createSpace(sourceOpenBIS, SPACE_CODE);
        Project project = facade.createProject(sourceOpenBIS, space.getPermId(), PROJECT_CODE);
        Experiment experiment = facade.createExperiment(sourceOpenBIS, project.getPermId(), EXPERIMENT_CODE);
        facade.createSample(sourceOpenBIS, space.getPermId(), SPACE_SAMPLE_CODE);
        facade.createSample(sourceOpenBIS, project.getPermId(), PROJECT_SAMPLE_CODE);
        facade.createSample(sourceOpenBIS, experiment.getPermId(), EXPERIMENT_SAMPLE_CODE);
        // A space's perm id is its code.
        return List.of("SPACE:" + SPACE_CODE);
    }

    private List<String> createTopLevelProject(IntegrationTestFacade facade, OpenBIS sourceOpenBIS)
    {
        Space space = facade.createSpace(sourceOpenBIS, TOP_PROJECT_SPACE_CODE);
        Project project = facade.createProject(sourceOpenBIS, space.getPermId(), TOP_PROJECT_CODE);
        return List.of("PROJECT:" + project.getPermId().getPermId());
    }

    private List<String> createTopLevelExperiment(IntegrationTestFacade facade, OpenBIS sourceOpenBIS)
    {
        Space space = facade.createSpace(sourceOpenBIS, TOP_EXPERIMENT_SPACE_CODE);
        Project project = facade.createProject(sourceOpenBIS, space.getPermId(), TOP_EXPERIMENT_PROJECT_CODE);
        Experiment experiment = facade.createExperiment(sourceOpenBIS, project.getPermId(), TOP_EXPERIMENT_CODE);
        return List.of("EXPERIMENT:" + experiment.getPermId().getPermId());
    }

    private List<String> createTwoTopLevelSamplesSharingSpace(IntegrationTestFacade facade, OpenBIS sourceOpenBIS)
    {
        Space space = facade.createSpace(sourceOpenBIS, TOP_SAMPLE_SPACE_CODE);
        Sample sample1 = facade.createSample(sourceOpenBIS, space.getPermId(), TOP_SAMPLE_CODE);
        Sample sample2 = facade.createSample(sourceOpenBIS, space.getPermId(), TOP_SAMPLE_CODE_2);
        facade.createSample(sourceOpenBIS, space.getPermId(), TOP_SAMPLE_CODE_3);
        return List.of(
                "SAMPLE:" + sample1.getPermId().getPermId(),
                "SAMPLE:" + sample2.getPermId().getPermId());
    }

    /**
     * Creates a property type with {@code multiValue}/{@code metaData} set, a sample type with its own
     * {@code metaData} that assigns the property type as {@code unique}, and one sample of that type -
     * MasterDataDeliverer only delivers master data actually referenced by a synced entity, so a bare type
     * registration with no instance would never be exported.
     */
    private List<String> createSchemaFeaturesScenario(IntegrationTestFacade facade, OpenBIS sourceOpenBIS)
    {
        PropertyTypeCreation propertyTypeCreation = new PropertyTypeCreation();
        propertyTypeCreation.setCode(SCHEMA_FEATURES_PROPERTY_TYPE_CODE);
        propertyTypeCreation.setLabel("Schema features property");
        propertyTypeCreation.setDescription("Property exercising multiValue + metaData sync");
        propertyTypeCreation.setDataType(DataType.VARCHAR);
        propertyTypeCreation.setMultiValue(true);
        propertyTypeCreation.setMetaData(SCHEMA_FEATURES_PROPERTY_TYPE_METADATA);
        sourceOpenBIS.createPropertyTypes(List.of(propertyTypeCreation));

        PropertyAssignmentCreation assignmentCreation = new PropertyAssignmentCreation();
        assignmentCreation.setPropertyTypeId(new PropertyTypePermId(SCHEMA_FEATURES_PROPERTY_TYPE_CODE));
        assignmentCreation.setUnique(true);

        SampleTypeCreation sampleTypeCreation = new SampleTypeCreation();
        sampleTypeCreation.setCode(SCHEMA_FEATURES_SAMPLE_TYPE_CODE);
        sampleTypeCreation.setMetaData(SCHEMA_FEATURES_SAMPLE_TYPE_METADATA);
        sampleTypeCreation.setPropertyAssignments(List.of(assignmentCreation));
        sourceOpenBIS.createSampleTypes(List.of(sampleTypeCreation));

        Space space = facade.createSpace(sourceOpenBIS, SCHEMA_FEATURES_SPACE_CODE);
        SampleCreation sampleCreation = new SampleCreation();
        sampleCreation.setTypeId(new EntityTypePermId(SCHEMA_FEATURES_SAMPLE_TYPE_CODE));
        sampleCreation.setSpaceId(space.getPermId());
        sampleCreation.setCode(SCHEMA_FEATURES_SAMPLE_CODE);
        sourceOpenBIS.createSamples(List.of(sampleCreation));

        return List.of("SPACE:" + SCHEMA_FEATURES_SPACE_CODE);
    }

    // --- Scenario verifiers: look the harvested copies up through the harvester's V3 API ---

    private void verifySpaceSubtree(OpenBIS harvester)
    {
        String harvestedSpace = NAME_PREFIX + SPACE_CODE;
        assertNotNull(findHarvestedSpace(harvester, harvestedSpace), "harvested SPACE");
        assertNotNull(findHarvestedProject(harvester, harvestedSpace, PROJECT_CODE), "harvested PROJECT");
        assertNotNull(findHarvestedExperiment(harvester, harvestedSpace, PROJECT_CODE, EXPERIMENT_CODE),
                "harvested EXPERIMENT");
        assertSampleAtSpace(harvester, harvestedSpace, SPACE_SAMPLE_CODE);
        assertSampleInProject(harvester, harvestedSpace, PROJECT_CODE, PROJECT_SAMPLE_CODE);
        assertSampleInExperiment(harvester, harvestedSpace, EXPERIMENT_CODE, EXPERIMENT_SAMPLE_CODE);
    }

    private void verifyTopLevelProject(OpenBIS harvester)
    {
        String harvestedSpace = NAME_PREFIX + TOP_PROJECT_SPACE_CODE;
        assertNotNull(findHarvestedSpace(harvester, harvestedSpace), "harvested top-level PROJECT's space");
        assertNotNull(findHarvestedProject(harvester, harvestedSpace, TOP_PROJECT_CODE), "harvested top-level PROJECT");
    }

    private void verifyTopLevelExperiment(OpenBIS harvester)
    {
        String harvestedSpace = NAME_PREFIX + TOP_EXPERIMENT_SPACE_CODE;
        assertNotNull(findHarvestedSpace(harvester, harvestedSpace), "harvested top-level EXPERIMENT's space");
        assertNotNull(findHarvestedProject(harvester, harvestedSpace, TOP_EXPERIMENT_PROJECT_CODE),
                "harvested top-level EXPERIMENT's project");
        assertNotNull(findHarvestedExperiment(harvester, harvestedSpace, TOP_EXPERIMENT_PROJECT_CODE, TOP_EXPERIMENT_CODE),
                "harvested top-level EXPERIMENT");
    }

    private void verifyTwoTopLevelSamplesSharingSpace(OpenBIS harvester)
    {
        String harvestedSpace = NAME_PREFIX + TOP_SAMPLE_SPACE_CODE;
        // The shared space must have been reconstructed exactly once, with both selected samples directly under it.
        assertEquals(findHarvestedSpaces(harvester, harvestedSpace).size(), 1,
                "two same-space sample roots must yield exactly one harvested space");
        assertSampleAtSpace(harvester, harvestedSpace, TOP_SAMPLE_CODE);
        assertSampleAtSpace(harvester, harvestedSpace, TOP_SAMPLE_CODE_2);
        // The unselected third sample must NOT have been dragged in with the pulled-from-above space.
        assertNull(findHarvestedSample(harvester, harvestedSpace, TOP_SAMPLE_CODE_3),
                "the unselected third sample must not be harvested");
    }

    private void verifySchemaFeatures(OpenBIS harvester)
    {
        PropertyTypePermId propertyTypeId = new PropertyTypePermId(NAME_PREFIX + SCHEMA_FEATURES_PROPERTY_TYPE_CODE);
        PropertyType propertyType = harvester.getPropertyTypes(List.of(propertyTypeId), new PropertyTypeFetchOptions()).get(propertyTypeId);
        assertNotNull(propertyType, "harvested schema-features property type");
        assertTrue(propertyType.isMultiValue(), "harvested property type must be multiValue");
        assertEquals(propertyType.getMetaData(), SCHEMA_FEATURES_PROPERTY_TYPE_METADATA, "harvested property type metaData");

        EntityTypePermId sampleTypeId = new EntityTypePermId(NAME_PREFIX + SCHEMA_FEATURES_SAMPLE_TYPE_CODE);
        SampleTypeFetchOptions sampleTypeFetchOptions = new SampleTypeFetchOptions();
        sampleTypeFetchOptions.withPropertyAssignments().withPropertyType();
        SampleType sampleType = harvester.getSampleTypes(List.of(sampleTypeId), sampleTypeFetchOptions).get(sampleTypeId);
        assertNotNull(sampleType, "harvested schema-features sample type");
        assertEquals(sampleType.getMetaData(), SCHEMA_FEATURES_SAMPLE_TYPE_METADATA, "harvested sample type metaData");

        String harvestedPropertyTypeCode = NAME_PREFIX + SCHEMA_FEATURES_PROPERTY_TYPE_CODE;
        PropertyAssignment assignment = sampleType.getPropertyAssignments().stream()
                .filter(a -> a.getPropertyType().getCode().equals(harvestedPropertyTypeCode))
                .findFirst().orElse(null);
        assertNotNull(assignment, "harvested property assignment for " + harvestedPropertyTypeCode);
        assertTrue(assignment.isUnique(), "harvested property assignment must be unique");
    }

    private OpenBIS sourceLogin()
    {
        // The hierarchy is built on the forked SOURCE instance (its own database, offset ports).
        OpenBIS sourceOpenBIS = source.createOpenBIS();
        sourceOpenBIS.login(INSTANCE_ADMIN, PASSWORD);
        return sourceOpenBIS;
    }

    private OpenBIS harvesterLogin()
    {
        // The harvested copies are verified in the in-JVM HARVESTER instance through the V3 API.
        OpenBIS harvester = environment.createOpenBIS();
        harvester.login(INSTANCE_ADMIN, PASSWORD);
        return harvester;
    }

    // --- Verification helpers: look the harvested copies up through the V3 API of the harvester instance ---

    private Space findHarvestedSpace(OpenBIS harvester, String spaceCode)
    {
        SpacePermId id = new SpacePermId(spaceCode);
        return harvester.getSpaces(List.of(id), new SpaceFetchOptions()).get(id);
    }

    private List<Space> findHarvestedSpaces(OpenBIS harvester, String spaceCode)
    {
        ch.ethz.sis.openbis.generic.asapi.v3.dto.space.search.SpaceSearchCriteria criteria =
                new ch.ethz.sis.openbis.generic.asapi.v3.dto.space.search.SpaceSearchCriteria();
        criteria.withCode().thatEquals(spaceCode);
        return harvester.searchSpaces(criteria, new SpaceFetchOptions()).getObjects();
    }

    private Project findHarvestedProject(OpenBIS harvester, String spaceCode, String projectCode)
    {
        ProjectSearchCriteria criteria = new ProjectSearchCriteria();
        criteria.withCode().thatEquals(projectCode);
        criteria.withSpace().withCode().thatEquals(spaceCode);
        ProjectFetchOptions fetchOptions = new ProjectFetchOptions();
        fetchOptions.withSpace();
        List<Project> projects = harvester.searchProjects(criteria, fetchOptions).getObjects();
        return projects.isEmpty() ? null : projects.get(0);
    }

    private Experiment findHarvestedExperiment(OpenBIS harvester, String spaceCode, String projectCode, String experimentCode)
    {
        ExperimentSearchCriteria criteria = new ExperimentSearchCriteria();
        criteria.withCode().thatEquals(experimentCode);
        criteria.withProject().withCode().thatEquals(projectCode);
        criteria.withProject().withSpace().withCode().thatEquals(spaceCode);
        ExperimentFetchOptions fetchOptions = new ExperimentFetchOptions();
        fetchOptions.withProject().withSpace();
        List<Experiment> experiments = harvester.searchExperiments(criteria, fetchOptions).getObjects();
        return experiments.isEmpty() ? null : experiments.get(0);
    }

    private Sample findHarvestedSample(OpenBIS harvester, String spaceCode, String sampleCode)
    {
        SampleSearchCriteria criteria = new SampleSearchCriteria();
        criteria.withCode().thatEquals(sampleCode);
        criteria.withSpace().withCode().thatEquals(spaceCode);
        SampleFetchOptions fetchOptions = new SampleFetchOptions();
        fetchOptions.withSpace();
        fetchOptions.withProject();
        fetchOptions.withExperiment();
        List<Sample> samples = harvester.searchSamples(criteria, fetchOptions).getObjects();
        return samples.isEmpty() ? null : samples.get(0);
    }

    private void assertSampleAtSpace(OpenBIS harvester, String spaceCode, String sampleCode)
    {
        Sample sample = findHarvestedSample(harvester, spaceCode, sampleCode);
        assertNotNull(sample, "harvested space sample " + sampleCode);
        assertNull(sample.getProject(), "space sample " + sampleCode + " must have no project");
        assertNull(sample.getExperiment(), "space sample " + sampleCode + " must have no experiment");
    }

    private void assertSampleInProject(OpenBIS harvester, String spaceCode, String projectCode, String sampleCode)
    {
        Sample sample = findHarvestedSample(harvester, spaceCode, sampleCode);
        assertNotNull(sample, "harvested project sample " + sampleCode);
        assertNotNull(sample.getProject(), "project sample " + sampleCode + " must have a project");
        assertEquals(sample.getProject().getCode(), projectCode, "project sample " + sampleCode + " project");
        assertNull(sample.getExperiment(), "project sample " + sampleCode + " must have no experiment");
    }

    private void assertSampleInExperiment(OpenBIS harvester, String spaceCode, String experimentCode, String sampleCode)
    {
        Sample sample = findHarvestedSample(harvester, spaceCode, sampleCode);
        assertNotNull(sample, "harvested experiment sample " + sampleCode);
        assertNotNull(sample.getExperiment(), "experiment sample " + sampleCode + " must have an experiment");
        assertEquals(sample.getExperiment().getCode(), experimentCode, "experiment sample " + sampleCode + " experiment");
    }

    /**
     * Runs the harvester once, on demand, against the externalized {@link #HARVESTER_CONFIG_FILE}.
     */
    private void runHarvester(List<String> exportablePermIds) throws Exception
    {
        new File("targets/openbis-sync").mkdirs();
        Files.deleteIfExists(new File("targets/openbis-sync/last-sync-timestamp.txt").toPath());

        File generatedConfig = writeConfigWithExportablePermIds(exportablePermIds);

        HarvesterMaintenanceTask<?> task = new HarvesterMaintenanceTask<>();
        Properties properties = new Properties();
        properties.setProperty("harvester-config-file", generatedConfig.getPath());
        task.setUp("openbis-sync-harvester", properties);
        task.execute();
    }

    private File writeConfigWithExportablePermIds(List<String> exportablePermIds) throws IOException
    {
        String permIdsLine = "exportable-perm-ids = " + String.join(", ", exportablePermIds);
        List<String> lines = new ArrayList<>();
        boolean replaced = false;
        for (String line : Files.readAllLines(Path.of(HARVESTER_CONFIG_FILE)))
        {
            if (line.trim().startsWith("exportable-perm-ids"))
            {
                lines.add(permIdsLine);
                replaced = true;
            } else
            {
                lines.add(line);
            }
        }
        if (!replaced)
        {
            lines.add(permIdsLine);
        }
        File generatedConfig = new File("targets/openbis-sync/harvester-config.generated.txt");
        Files.write(generatedConfig.toPath(), lines);
        return generatedConfig;
    }

    private List<String> fetchResourceListLocations(List<ExportablePermId> exportablePermIds) throws Exception
    {
        String resourceListUrl = source.getDSSUrl() + TestInstanceHostUtils.getDSSPath() + "/re-sync";
        BasicAuthCredentials credentials = new BasicAuthCredentials("OAI-PMH", INSTANCE_ADMIN, PASSWORD);
        Logger logger = LogFactory.getLogger(LogCategory.OPERATION, IntegrationOpenbisSyncTest.class);
        IDataSourceConnector connector = new DataSourceConnector(resourceListUrl, credentials, logger);
        Document resourceList = connector.getResourceListAsXMLDoc(exportablePermIds, false,
                false, false, false, false);

        NodeList locs = resourceList.getElementsByTagName("loc");
        List<String> locations = new ArrayList<>();
        for (int i = 0; i < locs.getLength(); i++)
        {
            locations.add(locs.item(i).getTextContent().trim());
        }
        return locations;
    }

}
