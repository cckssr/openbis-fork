/*
 * Copyright ETH 2010 - 2023 Zürich, Scientific IT Services
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
package ch.ethz.sis.openbis.systemtests.common;

import static org.testng.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.sql.DataSource;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameterUtil;
import ch.ethz.sis.openbis.afsserver.server.common.OpenBISConfiguration;
import ch.ethz.sis.openbis.afsserver.server.common.TestLogger;
import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSetKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.create.DataSetCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.create.PhysicalDataCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.FileFormatTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.ProprietaryStorageFormatPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.RelativeLocationLocatorTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.id.DataStorePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.deletion.id.IDeletionId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.IEntityTypeId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.create.ExperimentCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.delete.ExperimentDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.IExperimentId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.update.ExperimentUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.Person;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.create.PersonCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.fetchoptions.PersonFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.id.PersonPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.create.ProjectCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.fetchoptions.ProjectFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.IProjectId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.ProjectPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.create.PropertyAssignmentCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.create.PropertyTypeCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.fetchoptions.PropertyTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.id.IPropertyTypeId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.id.PropertyTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.roleassignment.Role;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.roleassignment.create.RoleAssignmentCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.create.SampleCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.create.SampleTypeCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.delete.SampleDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.ISampleId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SamplePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.update.SampleUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.create.SpaceCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.fetchoptions.SpaceFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.ISpaceId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import ch.ethz.sis.openbis.systemtests.environment.AfsServerConfiguration;
import ch.ethz.sis.openbis.systemtests.environment.ApplicationServerConfiguration;
import ch.ethz.sis.openbis.systemtests.environment.DataStoreServerConfiguration;
import ch.ethz.sis.openbis.systemtests.environment.IntegrationTestEnvironment;
import ch.ethz.sis.openbis.systemtests.environment.RoCrateServerConfiguration;
import ch.ethz.sis.openbis.systemtests.environment.ServerProxyInterceptor;
import ch.ethz.sis.shared.startup.Configuration;
import ch.systemsx.cisd.common.filesystem.FileUtilities;

/**
 * @author pkupczyk
 */
public class AbstractIntegrationTest
{
    public static final String TEST_INTERACTIVE_SESSION_KEY = "integration-test-interactive-session-key";

    public static final String TEST_DATA_STORE_CODE = "STANDARD";

    public static final String DEFAULT_SPACE = "DEFAULT";

    public static final String TEST_SPACE = "TEST";

    public static final String INSTANCE_ADMIN = "admin";

    public static final String DEFAULT_SPACE_ADMIN = "default_space_admin";

    public static final String TEST_SPACE_ADMIN = "test_space_admin";

    public static final String TEST_SPACE_OBSERVER = "test_space_observer";

    public static final String PASSWORD = "password";

    public static IntegrationTestEnvironment environment;

    @BeforeSuite
    public void beforeSuite() throws Exception
    {
        initLogging();

        IntegrationTestEnvironment environment = new IntegrationTestEnvironment();

        ApplicationServerConfiguration applicationServerConfiguration = new ApplicationServerConfiguration();
        applicationServerConfiguration.loadServiceProperties(Path.of("etc/as/service.properties"));
        environment.createApplicationServer(applicationServerConfiguration);

        DataStoreServerConfiguration dataStoreServerConfiguration = new DataStoreServerConfiguration();
        dataStoreServerConfiguration.loadServiceProperties(Path.of("etc/dss/service.properties"));
        environment.createDataStoreServer(dataStoreServerConfiguration);

        AfsServerConfiguration afsServerConfiguration = new AfsServerConfiguration();
        afsServerConfiguration.loadServiceProperties(Path.of("etc/afs/service.properties"));
        environment.createAfsServer(afsServerConfiguration);

        RoCrateServerConfiguration roCrateServerConfiguration = new RoCrateServerConfiguration();
        roCrateServerConfiguration.loadServiceProperties(Path.of("etc/ro-crate/service.properties"));
        environment.createRoCrateServer(roCrateServerConfiguration);

        environment.start();

        AbstractIntegrationTest.environment = environment;

        createApplicationServerData();

        TestLogger.configure();
    }

    @AfterSuite
    public void afterSuite() throws Exception
    {
        environment.stop();
    }

    @BeforeMethod
    public void beforeMethod(Method method) throws Exception
    {
        log("\n>>>>>>>>>>>>>>>>\nBEFORE " + method.getDeclaringClass().getName() + "." + method.getName() + "\n>>>>>>>>>>>>>>>>\n");
        environment.getApplicationServer().setProxyInterceptor(null);
        environment.getAfsServer().setProxyInterceptor(null);
    }

    @AfterMethod
    public void afterMethod(Method method) throws Exception
    {
        log("\n<<<<<<<<<<<<<<<<\nAFTER  " + method.getDeclaringClass().getName() + "." + method.getName() + "\n<<<<<<<<<<<<<<<<\n");
    }

    private void initLogging()
    {
        System.setProperty("log.configuration", "etc/as/logging.properties");
    }

    public void restartApplicationServer() throws Exception
    {
        log("Restarting application server.");
        environment.getApplicationServer().stop();
        environment.getApplicationServer().start();
    }

    public void restartAfsServer() throws Exception
    {
        log("Restarting afs server.");
        environment.getAfsServer().stop();
        environment.getAfsServer().start();
    }

    public static void setApplicationServerProxyInterceptor(
            final ServerProxyInterceptor applicationServerProxyInterceptor)
    {
        environment.getApplicationServer().setProxyInterceptor(applicationServerProxyInterceptor);
    }

    public static void setAfsServerProxyInterceptor(final ServerProxyInterceptor afsServerProxyInterceptor)
    {
        environment.getAfsServer().setProxyInterceptor(afsServerProxyInterceptor);
    }

    private static void createApplicationServerData() throws Exception
    {
        Configuration configuration = new Configuration(environment.getAfsServer().getConfiguration().getServiceProperties());

        OpenBIS openBIS = createOpenBIS();
        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        createSpace(openBIS, TEST_SPACE);
        createUser(openBIS, TEST_SPACE_ADMIN, TEST_SPACE, Role.ADMIN);
        createUser(openBIS, TEST_SPACE_OBSERVER, TEST_SPACE, Role.OBSERVER);

        createUser(openBIS, DEFAULT_SPACE_ADMIN, DEFAULT_SPACE, Role.ADMIN);

        SampleUpdate elnSettingsUpdate = new SampleUpdate();
        elnSettingsUpdate.setSampleId(new SampleIdentifier("/ELN_SETTINGS/GENERAL_ELN_SETTINGS"));
        elnSettingsUpdate.setProperties(Map.of("ELN_SETTINGS", FileUtilities.loadToString(new File("etc/as/eln-settings.json"))));

        openBIS.updateSamples(List.of(elnSettingsUpdate));
    }

    public static OpenBIS createOpenBIS()
    {
        return environment.createOpenBIS();
    }

    public static SampleType createSampleType(OpenBIS openBIS, String sampleTypeCode, List<IPropertyTypeId> propertyTypeIds)
    {
        SampleTypeCreation sampleTypeCreation = new SampleTypeCreation();
        sampleTypeCreation.setCode(sampleTypeCode);
        if (propertyTypeIds != null)
        {
            List<PropertyAssignmentCreation> assignmentCreations = new ArrayList<>();
            for (IPropertyTypeId propertyTypeId : propertyTypeIds)
            {
                PropertyAssignmentCreation assignmentCreation = new PropertyAssignmentCreation();
                assignmentCreation.setPropertyTypeId(propertyTypeId);
                assignmentCreations.add(assignmentCreation);
            }
            sampleTypeCreation.setPropertyAssignments(assignmentCreations);
        }
        List<EntityTypePermId> sampleTypeIds = openBIS.createSampleTypes(List.of(sampleTypeCreation));
        SampleType sampleType = getSampleType(openBIS, sampleTypeIds.get(0));
        log("Created sample type " + sampleType.getCode());
        return sampleType;
    }

    public static PropertyType createPropertyType(OpenBIS openBIS, String propertyTypeCode)
    {
        PropertyTypeCreation propertyTypeCreation = new PropertyTypeCreation();
        propertyTypeCreation.setCode(propertyTypeCode);
        List<PropertyTypePermId> propertyTypeIds = openBIS.createPropertyTypes(List.of(propertyTypeCreation));
        PropertyType propertyType = getPropertyType(openBIS, propertyTypeIds.get(0));
        log("Created property type " + propertyType.getCode());
        return propertyType;
    }

    public static Space createSpace(OpenBIS openBIS, String spaceCode)
    {
        SpaceCreation spaceCreation = new SpaceCreation();
        spaceCreation.setCode(spaceCode);
        List<SpacePermId> spaceIds = openBIS.createSpaces(List.of(spaceCreation));
        Space space = getSpace(openBIS, spaceIds.get(0));
        log("Created space " + space.getCode());
        return space;
    }

    public static SampleType getSampleType(OpenBIS openBIS, IEntityTypeId sampleTypeId)
    {
        return openBIS.getSampleTypes(List.of(sampleTypeId), new SampleTypeFetchOptions()).get(sampleTypeId);
    }

    public static PropertyType getPropertyType(OpenBIS openBIS, IPropertyTypeId propertyTypeId)
    {
        return openBIS.getPropertyTypes(List.of(propertyTypeId), new PropertyTypeFetchOptions()).get(propertyTypeId);
    }

    public static Space getSpace(OpenBIS openBIS, ISpaceId spaceId)
    {
        return openBIS.getSpaces(List.of(spaceId), new SpaceFetchOptions()).get(spaceId);
    }

    public static Project createProject(OpenBIS openBIS, ISpaceId spaceId, String projectCode)
    {
        ProjectCreation projectCreation = new ProjectCreation();
        projectCreation.setSpaceId(spaceId);
        projectCreation.setCode(projectCode);
        List<ProjectPermId> projectIds = openBIS.createProjects(List.of(projectCreation));
        Project project = openBIS.getProjects(projectIds, new ProjectFetchOptions()).get(projectIds.get(0));
        log("Created project " + project.getIdentifier() + " (" + project.getPermId().getPermId() + ")");
        return project;
    }

    public static Experiment createExperiment(OpenBIS openBIS, IProjectId projectId, String experimentCode)
    {
        ExperimentCreation experimentCreation = new ExperimentCreation();
        experimentCreation.setTypeId(new EntityTypePermId("UNKNOWN"));
        experimentCreation.setProjectId(projectId);
        experimentCreation.setCode(experimentCode);
        List<ExperimentPermId> experimentIds = openBIS.createExperiments(List.of(experimentCreation));
        Experiment experiment = openBIS.getExperiments(experimentIds, new ExperimentFetchOptions()).get(experimentIds.get(0));
        log("Created experiment " + experiment.getIdentifier() + " (" + experiment.getPermId().getPermId() + ")");
        return experiment;
    }

    public static void makeExperimentImmutable(OpenBIS openBIS, IExperimentId experimentId)
    {
        ExperimentUpdate update = new ExperimentUpdate();
        update.setExperimentId(experimentId);
        update.makeDataImmutable();
        openBIS.updateExperiments(List.of(update));
    }

    public static void deleteExperiment(OpenBIS openBIS, IExperimentId experimentId)
    {
        ExperimentDeletionOptions options = new ExperimentDeletionOptions();
        options.setReason("test");
        IDeletionId deletionId = openBIS.deleteExperiments(List.of(experimentId), options);
        openBIS.confirmDeletions(List.of(deletionId));
        log("Deleted experiment " + experimentId);
    }

    public static Sample createSample(OpenBIS openBIS, ISpaceId spaceId, String sampleCode)
    {
        SampleCreation sampleCreation = new SampleCreation();
        sampleCreation.setTypeId(new EntityTypePermId("UNKNOWN"));
        sampleCreation.setSpaceId(spaceId);
        sampleCreation.setCode(sampleCode);
        List<SamplePermId> sampleIds = openBIS.createSamples(List.of(sampleCreation));
        Sample sample = getSample(openBIS, sampleIds.get(0));
        log("Created sample " + sample.getIdentifier() + " (" + sample.getPermId().getPermId() + ")");
        return sample;
    }

    public static Sample createSample(OpenBIS openBIS, IProjectId projectId, String sampleCode)
    {
        ProjectFetchOptions projectFetchOptions = new ProjectFetchOptions();
        projectFetchOptions.withSpace();

        Project project = openBIS.getProjects(List.of(projectId), projectFetchOptions).get(projectId);
        if (project == null)
        {
            throw new RuntimeException("Project with id " + projectId + " hasn't been found.");
        }

        SampleCreation sampleCreation = new SampleCreation();
        sampleCreation.setTypeId(new EntityTypePermId("UNKNOWN"));
        sampleCreation.setSpaceId(project.getSpace().getPermId());
        sampleCreation.setProjectId(project.getPermId());
        sampleCreation.setCode(sampleCode);
        List<SamplePermId> sampleIds = openBIS.createSamples(List.of(sampleCreation));
        Sample sample = getSample(openBIS, sampleIds.get(0));
        log("Created sample " + sample.getIdentifier() + " (" + sample.getPermId().getPermId() + ")");
        return sample;
    }

    public static Sample createSample(OpenBIS openBIS, IExperimentId experimentId, String sampleCode)
    {
        ExperimentFetchOptions experimentFetchOptions = new ExperimentFetchOptions();
        experimentFetchOptions.withProject().withSpace();

        Experiment experiment = openBIS.getExperiments(List.of(experimentId), experimentFetchOptions).get(experimentId);
        if (experiment == null)
        {
            throw new RuntimeException("Experiment with id " + experimentId + " hasn't been found.");
        }

        SampleCreation sampleCreation = new SampleCreation();
        sampleCreation.setTypeId(new EntityTypePermId("UNKNOWN"));
        sampleCreation.setSpaceId(experiment.getProject().getSpace().getPermId());
        sampleCreation.setExperimentId(experiment.getPermId());
        sampleCreation.setCode(sampleCode);
        List<SamplePermId> sampleIds = openBIS.createSamples(List.of(sampleCreation));
        Sample sample = getSample(openBIS, sampleIds.get(0));
        log("Created sample " + sample.getIdentifier() + " (" + sample.getPermId().getPermId() + ")");
        return sample;
    }

    public static void makeSampleImmutable(OpenBIS openBIS, ISampleId sampleId)
    {
        SampleUpdate update = new SampleUpdate();
        update.setSampleId(sampleId);
        update.makeDataImmutable();
        openBIS.updateSamples(List.of(update));
    }

    public static void deleteSample(OpenBIS openBIS, ISampleId sampleId)
    {
        SampleDeletionOptions options = new SampleDeletionOptions();
        options.setReason("test");
        IDeletionId deletionId = openBIS.deleteSamples(List.of(sampleId), options);
        openBIS.confirmDeletions(List.of(deletionId));
        log("Deleted sample " + sampleId);
    }

    public static Sample getSample(OpenBIS openBIS, ISampleId sampleId)
    {
        return openBIS.getSamples(List.of(sampleId), new SampleFetchOptions()).get(sampleId);
    }

    public static DataSet createDataSet(OpenBIS openBIS, IExperimentId experimentId, String dataSetCode, String testFile, byte[] testData)
            throws IOException
    {
        Configuration afsServerConfiguration = new Configuration(environment.getAfsServer().getConfiguration().getServiceProperties());
        String storageRoot = AtomicFileSystemServerParameterUtil.getStorageRoot(afsServerConfiguration);
        String storageUuid = OpenBISConfiguration.getInstance(afsServerConfiguration).getStorageUuid();
        Integer shareId = AtomicFileSystemServerParameterUtil.getStorageIncomingShareId(afsServerConfiguration);

        List<String> dataSetFolderLocation = new ArrayList<>();
        dataSetFolderLocation.add(storageUuid);
        dataSetFolderLocation.addAll(Arrays.asList(ch.ethz.sis.shared.io.IOUtils.getShards(dataSetCode.toUpperCase())));
        dataSetFolderLocation.add(dataSetCode.toUpperCase());

        File dataSetFolder = new File(new File(storageRoot, String.valueOf(shareId)), String.join(File.separator, dataSetFolderLocation));

        Files.createDirectories(dataSetFolder.toPath());
        Path testFilePath = Files.createFile(Path.of(dataSetFolder.getPath(), testFile));
        ch.ethz.sis.shared.io.IOUtils.write(testFilePath.toFile().getAbsolutePath(), 0L, testData);

        PhysicalDataCreation physicalCreation = new PhysicalDataCreation();
        physicalCreation.setShareId(shareId.toString());
        physicalCreation.setLocation(String.join(File.separator, dataSetFolderLocation));
        physicalCreation.setFileFormatTypeId(new FileFormatTypePermId("PROPRIETARY"));
        physicalCreation.setLocatorTypeId(new RelativeLocationLocatorTypePermId());
        physicalCreation.setStorageFormatId(new ProprietaryStorageFormatPermId());
        physicalCreation.setH5arFolders(false);
        physicalCreation.setH5Folders(false);

        DataSetCreation dataSetCreation = new DataSetCreation();
        dataSetCreation.setDataStoreId(new DataStorePermId(TEST_DATA_STORE_CODE));
        dataSetCreation.setDataSetKind(DataSetKind.PHYSICAL);
        dataSetCreation.setTypeId(new EntityTypePermId("UNKNOWN"));
        dataSetCreation.setExperimentId(experimentId);
        dataSetCreation.setCode(dataSetCode);
        dataSetCreation.setPhysicalData(physicalCreation);

        List<DataSetPermId> dataSetIds = openBIS.createDataSetsAS(List.of(dataSetCreation));
        DataSet dataSet = openBIS.getDataSets(dataSetIds, new DataSetFetchOptions()).get(dataSetIds.get(0));

        log("Created dataSet " + dataSet.getPermId());
        return dataSet;
    }

    public static Person createUser(OpenBIS openBIS, String userId, String spaceCode, Role spaceRole)
    {
        PersonCreation personCreation = new PersonCreation();
        personCreation.setUserId(userId);
        PersonPermId personId = openBIS.createPersons(List.of(personCreation)).get(0);

        RoleAssignmentCreation roleCreation = new RoleAssignmentCreation();
        roleCreation.setUserId(personId);
        if (spaceCode != null)
        {
            roleCreation.setSpaceId(new SpacePermId(spaceCode));
        }
        roleCreation.setRole(spaceRole);
        openBIS.createRoleAssignments(List.of(roleCreation));

        Person person = openBIS.getPersons(List.of(personId), new PersonFetchOptions()).get(personId);
        log("Created user " + person.getUserId());
        return person;
    }

    public static void log(String message)
    {
        System.out.println("[TEST] " + message);
    }

    public void assertExperimentExistsAtAS(String experimentPermId, boolean exists) throws Exception
    {
        try (Connection connection = environment.getApplicationServer().getApplicationContext().getBean(DataSource.class).getConnection();
                Statement statement = connection.createStatement())
        {
            ResultSet resultSet = statement.executeQuery("SELECT count(*) FROM experiments_all WHERE perm_id = '" + experimentPermId + "'");
            resultSet.next();
            assertEquals(resultSet.getInt(1), exists ? 1 : 0);
        }
    }

    public void assertSampleExistsAtAS(String samplePermId, boolean exists) throws Exception
    {
        try (Connection connection = environment.getApplicationServer().getApplicationContext().getBean(DataSource.class).getConnection();
                Statement statement = connection.createStatement())
        {
            ResultSet resultSet = statement.executeQuery("SELECT count(*) FROM samples_all WHERE perm_id = '" + samplePermId + "'");
            resultSet.next();
            assertEquals(resultSet.getInt(1), exists ? 1 : 0);
        }
    }

    public void assertDSSDataSetExistsAtAS(String dataSetPermId, boolean exists) throws Exception
    {
        try (Connection connection = environment.getApplicationServer().getApplicationContext().getBean(DataSource.class).getConnection();
                Statement statement = connection.createStatement())
        {
            ResultSet resultSet = statement.executeQuery("SELECT count(*) FROM data_all WHERE afs_data = 'f' AND code = '" + dataSetPermId + "'");
            resultSet.next();
            assertEquals(resultSet.getInt(1), exists ? 1 : 0);
        }
    }

    public void assertAFSDataSetExistsAtAS(String dataSetPermId, boolean exists) throws Exception
    {
        try (Connection connection = environment.getApplicationServer().getApplicationContext().getBean(DataSource.class).getConnection();
                Statement statement = connection.createStatement())
        {
            ResultSet resultSet = statement.executeQuery("SELECT count(*) FROM data_all WHERE afs_data = 't' AND code = '" + dataSetPermId + "'");
            resultSet.next();
            assertEquals(resultSet.getInt(1), exists ? 1 : 0);
        }
    }

    public void assertDataExistsInStoreInShare(String dataSetPermId, boolean exists, Integer shareId) throws Exception
    {
        Configuration afsServerConfiguration = new Configuration(environment.getAfsServer().getConfiguration().getServiceProperties());
        String storageRoot = AtomicFileSystemServerParameterUtil.getStorageRoot(afsServerConfiguration);
        String storageUuid = OpenBISConfiguration.getInstance(afsServerConfiguration).getStorageUuid();

        List<String> dataSetFolderLocation = new ArrayList<>();
        dataSetFolderLocation.add(storageUuid);
        dataSetFolderLocation.addAll(Arrays.asList(ch.ethz.sis.shared.io.IOUtils.getShards(dataSetPermId.toUpperCase())));
        dataSetFolderLocation.add(dataSetPermId.toUpperCase());

        Path dataSetFolder = Paths.get(storageRoot, String.valueOf(shareId), String.join(File.separator, dataSetFolderLocation));
        assertEquals(Files.exists(dataSetFolder), exists);

        try (Connection connection = environment.getApplicationServer().getApplicationContext().getBean(DataSource.class).getConnection();
                Statement statement = connection.createStatement())
        {
            ResultSet resultSet = statement.executeQuery(
                    "SELECT location, share_id FROM data_all d LEFT OUTER JOIN external_data ed on d.id = ed.id WHERE d.code = '"
                            + dataSetPermId.toUpperCase() + "'");
            resultSet.next();

            if (exists)
            {
                String locationInDB = resultSet.getString(1);
                String shareInDB = resultSet.getString(2);

                assertEquals(shareInDB, String.valueOf(shareId));
                assertEquals(locationInDB, String.join(File.separator, dataSetFolderLocation));
            }
        }
    }

    public static void waitUntilCondition(Supplier<Boolean> condition, long timeout)
    {
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() < startTime + timeout)
        {
            if (condition.get())
            {
                return;
            } else
            {
                try
                {
                    Thread.sleep(timeout / 10);
                } catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
        }

        throw new RuntimeException("Timed out waiting for " + timeout + " ms.");
    }

}
