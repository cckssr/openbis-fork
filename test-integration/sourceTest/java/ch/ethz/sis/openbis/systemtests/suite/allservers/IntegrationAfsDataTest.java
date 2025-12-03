package ch.ethz.sis.openbis.systemtests.suite.allservers;

import static ch.ethz.sis.openbis.systemtests.suite.allservers.environment.AllServersIntegrationTestEnvironment.DEFAULT_SPACE;
import static ch.ethz.sis.openbis.systemtests.suite.allservers.environment.AllServersIntegrationTestEnvironment.DEFAULT_SPACE_ADMIN;
import static ch.ethz.sis.openbis.systemtests.suite.allservers.environment.AllServersIntegrationTestEnvironment.INSTANCE_ADMIN;
import static ch.ethz.sis.openbis.systemtests.suite.allservers.environment.AllServersIntegrationTestEnvironment.PASSWORD;
import static ch.ethz.sis.openbis.systemtests.suite.allservers.environment.AllServersIntegrationTestEnvironment.TEST_INTERACTIVE_SESSION_KEY;
import static ch.ethz.sis.openbis.systemtests.suite.allservers.environment.AllServersIntegrationTestEnvironment.TEST_SPACE;
import static ch.ethz.sis.openbis.systemtests.suite.allservers.environment.AllServersIntegrationTestEnvironment.TEST_SPACE_ADMIN;
import static ch.ethz.sis.openbis.systemtests.suite.allservers.environment.AllServersIntegrationTestEnvironment.TEST_SPACE_OBSERVER;
import static ch.ethz.sis.openbis.systemtests.suite.allservers.environment.AllServersIntegrationTestEnvironment.environment;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.io.IOUtils;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import ch.ethz.sis.openbis.afsserver.server.common.OpenBISConfiguration;
import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.search.DataSetSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.search.DataStoreKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.deletion.id.IDeletionId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.delete.ExperimentDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.update.ExperimentUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.delete.SampleDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.update.SampleUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.DataSetFile;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.download.DataSetFileDownload;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.download.DataSetFileDownloadOptions;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.download.DataSetFileDownloadReader;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.fetchoptions.DataSetFileFetchOptions;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.search.DataSetFileSearchCriteria;
import ch.ethz.sis.openbis.systemtests.environment.IntegrationTestFacade;
import ch.ethz.sis.openbis.systemtests.suite.allservers.environment.AllServersIntegrationTestEnvironment;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.ethz.sis.shared.log.classic.impl.Logger;
import ch.ethz.sis.shared.startup.Configuration;

public class IntegrationAfsDataTest
{

    private static final Logger log = LogFactory.getLogger(IntegrationAfsDataTest.class);

    private static final String ENTITY_CODE_PREFIX = "AFS_DATA_TEST_";

    private static final long WAITING_TIME_FOR_ASYNC_TASKS = 5000L;

    private IntegrationTestFacade facade;

    @BeforeSuite
    public void beforeSuite()
    {
        AllServersIntegrationTestEnvironment.start();
    }

    @AfterSuite
    public void afterSuite()
    {
        AllServersIntegrationTestEnvironment.stop();
    }

    @BeforeMethod
    public void beforeMethod() throws Exception
    {
        facade = new IntegrationTestFacade(environment);
        deleteLastSeenDeletionFile();
    }

    @Test
    public void testCreateAfsDataWithoutTransaction() throws Exception
    {
        OpenBIS openBIS = facade.createOpenBIS();
        openBIS.setInteractiveSessionKey(TEST_INTERACTIVE_SESSION_KEY);

        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        // create sample at AS
        Sample sample = facade.createSample(openBIS, new SpacePermId(DEFAULT_SPACE), ENTITY_CODE_PREFIX + UUID.randomUUID());

        facade.assertSampleExistsAtAS(sample.getPermId().getPermId(), true);
        facade.assertAFSDataSetExistsAtAS(sample.getPermId().getPermId(), false);
        facade.assertDataExistsInStoreInShare(sample.getPermId().getPermId(), false, null);

        // create data at AFS
        openBIS.getAfsServerFacade().write(sample.getPermId().getPermId(), "test-file.txt", 0L, "test-content".getBytes());

        facade.assertSampleExistsAtAS(sample.getPermId().getPermId(), true);
        facade.assertAFSDataSetExistsAtAS(sample.getPermId().getPermId(), true);
        facade.assertDataExistsInStoreInShare(sample.getPermId().getPermId(), true, 1);

        openBIS.getAfsServerFacade().write(sample.getPermId().getPermId(), "test-file-2.txt", 0L, "test-content-2".getBytes());

        facade.assertSampleExistsAtAS(sample.getPermId().getPermId(), true);
        facade.assertAFSDataSetExistsAtAS(sample.getPermId().getPermId(), true);
        facade.assertDataExistsInStoreInShare(sample.getPermId().getPermId(), true, 1);
    }

    @Test
    public void testCreateAfsDataIn1PCTransaction() throws Exception
    {
        OpenBIS openBIS = facade.createOpenBIS();
        openBIS.setInteractiveSessionKey(TEST_INTERACTIVE_SESSION_KEY);

        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        // create sample at AS
        Sample sample = facade.createSample(openBIS, new SpacePermId(DEFAULT_SPACE), ENTITY_CODE_PREFIX + UUID.randomUUID());

        // BEGIN
        openBIS.beginTransaction();

        facade.assertSampleExistsAtAS(sample.getPermId().getPermId(), true);
        facade.assertAFSDataSetExistsAtAS(sample.getPermId().getPermId(), false);
        facade.assertDataExistsInStoreInShare(sample.getPermId().getPermId(), false, null);

        // create data at AFS
        openBIS.getAfsServerFacade().write(sample.getPermId().getPermId(), "test-file.txt", 0L, "test-content".getBytes());
        openBIS.getAfsServerFacade().write(sample.getPermId().getPermId(), "test-file-2.txt", 0L, "test-content-2".getBytes());

        facade.assertSampleExistsAtAS(sample.getPermId().getPermId(), true);
        facade.assertAFSDataSetExistsAtAS(sample.getPermId().getPermId(), false);
        facade.assertDataExistsInStoreInShare(sample.getPermId().getPermId(), false, null);

        // COMMIT
        openBIS.commitTransaction();

        facade.assertSampleExistsAtAS(sample.getPermId().getPermId(), true);
        facade.assertAFSDataSetExistsAtAS(sample.getPermId().getPermId(), true);
        facade.assertDataExistsInStoreInShare(sample.getPermId().getPermId(), true, 1);
    }

    @Test
    public void testCreateAfsDataIn2PCTransaction() throws Exception
    {
        OpenBIS openBIS = facade.createOpenBIS();
        openBIS.setInteractiveSessionKey(TEST_INTERACTIVE_SESSION_KEY);

        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        // BEGIN
        openBIS.beginTransaction();

        // create sample at AS
        Sample sample = facade.createSample(openBIS, new SpacePermId(DEFAULT_SPACE), ENTITY_CODE_PREFIX + UUID.randomUUID());

        facade.assertSampleExistsAtAS(sample.getPermId().getPermId(), false);
        facade.assertAFSDataSetExistsAtAS(sample.getPermId().getPermId(), false);
        facade.assertDataExistsInStoreInShare(sample.getPermId().getPermId(), false, null);

        // create data at AFS
        openBIS.getAfsServerFacade().write(sample.getPermId().getPermId(), "test-file.txt", 0L, "test-content".getBytes());
        openBIS.getAfsServerFacade().write(sample.getPermId().getPermId(), "test-file-2.txt", 0L, "test-content-2".getBytes());

        facade.assertSampleExistsAtAS(sample.getPermId().getPermId(), false);
        facade.assertAFSDataSetExistsAtAS(sample.getPermId().getPermId(), false);
        facade.assertDataExistsInStoreInShare(sample.getPermId().getPermId(), false, null);

        // COMMIT
        openBIS.commitTransaction();

        facade.assertSampleExistsAtAS(sample.getPermId().getPermId(), true);
        facade.assertAFSDataSetExistsAtAS(sample.getPermId().getPermId(), true);
        facade.assertDataExistsInStoreInShare(sample.getPermId().getPermId(), true, 1);
    }

    @Test
    public void testAccessToReadDataFromSample() throws Exception
    {
        // TEST space users should have READ access, DEFAULT space user should not
        List<List<Object>> testCases =
                List.of(List.of(TEST_SPACE_ADMIN, true), List.of(TEST_SPACE_OBSERVER, true), List.of(DEFAULT_SPACE_ADMIN, false));

        for (List<Object> testCase : testCases)
        {
            log.info("Test case: " + testCase);

            String userId = (String) testCase.get(0);
            boolean userHasAccess = (boolean) testCase.get(1);

            String testFile = "test-file-" + UUID.randomUUID();
            String testData = "test-content-" + UUID.randomUUID();

            OpenBIS openBISInstanceAdmin = facade.createOpenBIS();
            openBISInstanceAdmin.login(INSTANCE_ADMIN, PASSWORD);

            // create sample and data in TEST space with instance admin user
            Sample sample = facade.createSample(openBISInstanceAdmin, new SpacePermId(TEST_SPACE), ENTITY_CODE_PREFIX + UUID.randomUUID());
            openBISInstanceAdmin.getAfsServerFacade().write(sample.getPermId().getPermId(), testFile, 0L, testData.getBytes());

            facade.assertSampleExistsAtAS(sample.getPermId().getPermId(), true);
            facade.assertAFSDataSetExistsAtAS(sample.getPermId().getPermId(), true);
            facade.assertDataExistsInStoreInShare(sample.getPermId().getPermId(), true, 1);

            OpenBIS openBIS = facade.createOpenBIS();
            openBIS.login(userId, PASSWORD);

            try
            {
                byte[] readData =
                        openBIS.getAfsServerFacade().read(sample.getPermId().getPermId(), testFile, 0L, testData.length());

                if (userHasAccess)
                {
                    assertEquals(readData, testData.getBytes());
                } else
                {
                    fail();
                }
            } catch (Exception e)
            {
                if (userHasAccess)
                {
                    fail();
                } else
                {
                    assertTrue(e.getMessage().contains("don't have rights [Read] over " + sample.getPermId().getPermId()));
                }
            }

            facade.assertSampleExistsAtAS(sample.getPermId().getPermId(), true);
            facade.assertAFSDataSetExistsAtAS(sample.getPermId().getPermId(), true);
            facade.assertDataExistsInStoreInShare(sample.getPermId().getPermId(), true, 1);
        }
    }

    @Test
    public void testAccessToWriteDataToSample() throws Exception
    {
        // TEST space admin user should have WRITE access to mutable sample, TEST space observer and DEFAULT space user should not
        List<List<Object>> testCases =
                List.of(List.of(TEST_SPACE_ADMIN, true, true), List.of(TEST_SPACE_ADMIN, false, false), List.of(TEST_SPACE_OBSERVER, true, false),
                        List.of(TEST_SPACE_OBSERVER, false, false), List.of(DEFAULT_SPACE_ADMIN, true, false),
                        List.of(DEFAULT_SPACE_ADMIN, false, false));

        for (List<Object> testCase : testCases)
        {
            log.info("Test case: " + testCase);

            String userId = (String) testCase.get(0);
            boolean mutableData = (boolean) testCase.get(1);
            boolean expectedAccess = (boolean) testCase.get(2);

            String testFile = "test-file-" + UUID.randomUUID();
            String testData = "test-content-" + UUID.randomUUID();

            OpenBIS openBISInstanceAdmin = facade.createOpenBIS();
            openBISInstanceAdmin.login(INSTANCE_ADMIN, PASSWORD);

            // create sample in TEST space with instance admin user
            Sample sample = facade.createSample(openBISInstanceAdmin, new SpacePermId(TEST_SPACE), ENTITY_CODE_PREFIX + UUID.randomUUID());

            if (!mutableData)
            {
                SampleUpdate sampleUpdate = new SampleUpdate();
                sampleUpdate.setSampleId(sample.getPermId());
                sampleUpdate.makeDataImmutable();
                openBISInstanceAdmin.updateSamples(List.of(sampleUpdate));
            }

            facade.assertSampleExistsAtAS(sample.getPermId().getPermId(), true);
            facade.assertAFSDataSetExistsAtAS(sample.getPermId().getPermId(), false);
            facade.assertDataExistsInStoreInShare(sample.getPermId().getPermId(), false, null);

            OpenBIS openBIS = facade.createOpenBIS();
            openBIS.login(userId, PASSWORD);

            try
            {
                openBIS.getAfsServerFacade().write(sample.getPermId().getPermId(), testFile, 0L, testData.getBytes());
                byte[] readData = openBIS.getAfsServerFacade().read(sample.getPermId().getPermId(), testFile, 0L, testData.length());

                if (expectedAccess)
                {
                    assertEquals(readData, testData.getBytes());
                    facade.assertSampleExistsAtAS(sample.getPermId().getPermId(), true);
                    facade.assertAFSDataSetExistsAtAS(sample.getPermId().getPermId(), true);
                    facade.assertDataExistsInStoreInShare(sample.getPermId().getPermId(), true, 1);
                } else
                {
                    fail();
                }
            } catch (Exception e)
            {
                if (expectedAccess)
                {
                    fail();
                } else
                {
                    assertTrue(e.getMessage().contains("don't have rights [Write] over " + sample.getPermId().getPermId()));
                    facade.assertSampleExistsAtAS(sample.getPermId().getPermId(), true);
                    facade.assertAFSDataSetExistsAtAS(sample.getPermId().getPermId(), false);
                    facade.assertDataExistsInStoreInShare(sample.getPermId().getPermId(), false, null);
                }
            }
        }
    }

    @Test
    public void testAccessToReadDataFromExperiment() throws Exception
    {
        // TEST space users should have READ access, DEFAULT space user should not
        List<List<Object>> testCases =
                List.of(List.of(TEST_SPACE_ADMIN, true), List.of(TEST_SPACE_OBSERVER, true), List.of(DEFAULT_SPACE_ADMIN, false));

        for (List<Object> testCase : testCases)
        {
            log.info("Test case: " + testCase);

            String userId = (String) testCase.get(0);
            boolean userHasAccess = (boolean) testCase.get(1);

            String testFile = "test-file-" + UUID.randomUUID();
            String testData = "test-content-" + UUID.randomUUID();

            OpenBIS openBISInstanceAdmin = facade.createOpenBIS();
            openBISInstanceAdmin.login(INSTANCE_ADMIN, PASSWORD);

            // create experiment and data in TEST space with instance admin user
            Project project = facade.createProject(openBISInstanceAdmin, new SpacePermId(TEST_SPACE), ENTITY_CODE_PREFIX + UUID.randomUUID());
            Experiment experiment = facade.createExperiment(openBISInstanceAdmin, project.getPermId(), ENTITY_CODE_PREFIX + UUID.randomUUID());
            openBISInstanceAdmin.getAfsServerFacade().write(experiment.getPermId().getPermId(), testFile, 0L, testData.getBytes());

            facade.assertExperimentExistsAtAS(experiment.getPermId().getPermId(), true);
            facade.assertAFSDataSetExistsAtAS(experiment.getPermId().getPermId(), true);
            facade.assertDataExistsInStoreInShare(experiment.getPermId().getPermId(), true, 1);

            OpenBIS openBIS = facade.createOpenBIS();
            openBIS.login(userId, PASSWORD);

            try
            {
                byte[] readData =
                        openBIS.getAfsServerFacade().read(experiment.getPermId().getPermId(), testFile, 0L, testData.length());

                if (userHasAccess)
                {
                    assertEquals(readData, testData.getBytes());
                } else
                {
                    fail();
                }
            } catch (Exception e)
            {
                if (userHasAccess)
                {
                    fail();
                } else
                {
                    assertTrue(e.getMessage().contains("don't have rights [Read] over " + experiment.getPermId().getPermId()));
                }
            }

            facade.assertExperimentExistsAtAS(experiment.getPermId().getPermId(), true);
            facade.assertAFSDataSetExistsAtAS(experiment.getPermId().getPermId(), true);
            facade.assertDataExistsInStoreInShare(experiment.getPermId().getPermId(), true, 1);
        }
    }

    @Test
    public void testAccessToWriteDataToExperiment() throws Exception
    {
        // TEST space admin user should have WRITE access to mutable experiment, TEST space observer and DEFAULT space user should not
        List<List<Object>> testCases =
                List.of(List.of(TEST_SPACE_ADMIN, true, true), List.of(TEST_SPACE_ADMIN, false, false), List.of(TEST_SPACE_OBSERVER, true, false),
                        List.of(TEST_SPACE_OBSERVER, false, false), List.of(DEFAULT_SPACE_ADMIN, true, false),
                        List.of(DEFAULT_SPACE_ADMIN, false, false));

        for (List<Object> testCase : testCases)
        {
            log.info("Test case: " + testCase);

            String userId = (String) testCase.get(0);
            boolean mutableData = (boolean) testCase.get(1);
            boolean expectedAccess = (boolean) testCase.get(2);

            String testFile = "test-file-" + UUID.randomUUID();
            String testData = "test-content-" + UUID.randomUUID();

            OpenBIS openBISInstanceAdmin = facade.createOpenBIS();
            openBISInstanceAdmin.login(INSTANCE_ADMIN, PASSWORD);

            // create experiment and in TEST space with instance admin user
            Project project = facade.createProject(openBISInstanceAdmin, new SpacePermId(TEST_SPACE), ENTITY_CODE_PREFIX + UUID.randomUUID());
            Experiment experiment = facade.createExperiment(openBISInstanceAdmin, project.getPermId(), ENTITY_CODE_PREFIX + UUID.randomUUID());

            if (!mutableData)
            {
                ExperimentUpdate experimentUpdate = new ExperimentUpdate();
                experimentUpdate.setExperimentId(experiment.getPermId());
                experimentUpdate.makeDataImmutable();
                openBISInstanceAdmin.updateExperiments(List.of(experimentUpdate));
            }

            facade.assertExperimentExistsAtAS(experiment.getPermId().getPermId(), true);
            facade.assertAFSDataSetExistsAtAS(experiment.getPermId().getPermId(), false);
            facade.assertDataExistsInStoreInShare(experiment.getPermId().getPermId(), false, null);

            OpenBIS openBIS = facade.createOpenBIS();
            openBIS.login(userId, PASSWORD);

            try
            {
                openBIS.getAfsServerFacade().write(experiment.getPermId().getPermId(), testFile, 0L, testData.getBytes());
                byte[] readData = openBIS.getAfsServerFacade().read(experiment.getPermId().getPermId(), testFile, 0L, testData.length());

                if (expectedAccess)
                {
                    assertEquals(readData, testData.getBytes());
                    facade.assertExperimentExistsAtAS(experiment.getPermId().getPermId(), true);
                    facade.assertAFSDataSetExistsAtAS(experiment.getPermId().getPermId(), true);
                    facade.assertDataExistsInStoreInShare(experiment.getPermId().getPermId(), true, 1);
                } else
                {
                    fail();
                }
            } catch (Exception e)
            {
                if (expectedAccess)
                {
                    fail();
                } else
                {
                    assertTrue(e.getMessage().contains("don't have rights [Write] over " + experiment.getPermId().getPermId()));
                    facade.assertExperimentExistsAtAS(experiment.getPermId().getPermId(), true);
                    facade.assertAFSDataSetExistsAtAS(experiment.getPermId().getPermId(), false);
                    facade.assertDataExistsInStoreInShare(experiment.getPermId().getPermId(), false, null);
                }
            }
        }
    }

    @Test
    public void testAccessToReadDataFromDSSDataSet() throws Exception
    {
        // TEST space users should have READ access, DEFAULT space user should not
        List<List<Object>> testCases =
                List.of(List.of(TEST_SPACE_ADMIN, true), List.of(TEST_SPACE_OBSERVER, true), List.of(DEFAULT_SPACE_ADMIN, false));

        for (List<Object> testCase : testCases)
        {
            log.info("Test case: " + testCase);

            String userId = (String) testCase.get(0);
            boolean userHasAccess = (boolean) testCase.get(1);

            String testFile = "test-file-" + UUID.randomUUID();
            String testData = "test-content-" + UUID.randomUUID();

            OpenBIS openBISInstanceAdmin = facade.createOpenBIS();
            openBISInstanceAdmin.login(INSTANCE_ADMIN, PASSWORD);

            // create experiment and data in TEST space with instance admin user
            Project project = facade.createProject(openBISInstanceAdmin, new SpacePermId(TEST_SPACE), ENTITY_CODE_PREFIX + UUID.randomUUID());
            Experiment experiment = facade.createExperiment(openBISInstanceAdmin, project.getPermId(), ENTITY_CODE_PREFIX + UUID.randomUUID());
            DataSet dataSet = facade.createDataSet(openBISInstanceAdmin, experiment.getPermId(), ENTITY_CODE_PREFIX + UUID.randomUUID(), testFile,
                    testData.getBytes());

            facade.assertDSSDataSetExistsAtAS(dataSet.getPermId().getPermId(), true);
            facade.assertAFSDataSetExistsAtAS(dataSet.getPermId().getPermId(), false);
            facade.assertDataExistsInStoreInShare(dataSet.getPermId().getPermId(), true, 1);

            OpenBIS openBIS = facade.createOpenBIS();
            openBIS.login(userId, PASSWORD);

            try
            {
                byte[] readData =
                        openBIS.getAfsServerFacade().read(dataSet.getPermId().getPermId(), testFile, 0L, testData.length());

                if (userHasAccess)
                {
                    assertEquals(readData, testData.getBytes());
                } else
                {
                    fail();
                }
            } catch (Exception e)
            {
                if (userHasAccess)
                {
                    fail();
                } else
                {
                    assertTrue(e.getMessage().contains("don't have rights [Read] over " + dataSet.getPermId().getPermId()));
                }
            }

            facade.assertDSSDataSetExistsAtAS(dataSet.getPermId().getPermId(), true);
            facade.assertAFSDataSetExistsAtAS(dataSet.getPermId().getPermId(), false);
            facade.assertDataExistsInStoreInShare(dataSet.getPermId().getPermId(), true, 1);
        }
    }

    @Test
    public void testAccessToWriteDataToDSSDataSet() throws Exception
    {
        // nobody should have WRITE access to old DSS dataset
        List<String> userIds = List.of(INSTANCE_ADMIN, TEST_SPACE_ADMIN, TEST_SPACE_OBSERVER);

        for (String userId : userIds)
        {
            log.info("Test case: " + userId);

            String testFile = "test-file-" + UUID.randomUUID();
            String testData = "test-content-" + UUID.randomUUID();

            OpenBIS openBISInstanceAdmin = facade.createOpenBIS();
            openBISInstanceAdmin.login(INSTANCE_ADMIN, PASSWORD);

            // create dataset with instance admin user
            Project project = facade.createProject(openBISInstanceAdmin, new SpacePermId(TEST_SPACE), ENTITY_CODE_PREFIX + UUID.randomUUID());
            Experiment experiment = facade.createExperiment(openBISInstanceAdmin, project.getPermId(), ENTITY_CODE_PREFIX + UUID.randomUUID());
            DataSet dataSet = facade.createDataSet(openBISInstanceAdmin, experiment.getPermId(), ENTITY_CODE_PREFIX + UUID.randomUUID(), testFile,
                    testData.getBytes());

            facade.assertDSSDataSetExistsAtAS(dataSet.getPermId().getPermId(), true);
            facade.assertAFSDataSetExistsAtAS(dataSet.getPermId().getPermId(), false);
            facade.assertDataExistsInStoreInShare(dataSet.getPermId().getPermId(), true, 1);

            OpenBIS openBIS = facade.createOpenBIS();
            openBIS.login(userId, PASSWORD);

            try
            {
                openBIS.getAfsServerFacade().write(dataSet.getPermId().getPermId(), testFile, 0L, testData.getBytes());
                fail();
            } catch (Exception e)
            {
                assertTrue(e.getMessage().contains("don't have rights [Write] over " + dataSet.getPermId().getPermId()));
            }

            facade.assertDSSDataSetExistsAtAS(dataSet.getPermId().getPermId(), true);
            facade.assertAFSDataSetExistsAtAS(dataSet.getPermId().getPermId(), false);
            facade.assertDataExistsInStoreInShare(dataSet.getPermId().getPermId(), true, 1);
        }
    }

    @Test
    public void testDeleteExperimentWithAfsDataSet() throws Exception
    {
        OpenBIS openBIS = facade.createOpenBIS();

        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        Project project = facade.createProject(openBIS, new SpacePermId(DEFAULT_SPACE), ENTITY_CODE_PREFIX + UUID.randomUUID());
        Experiment experiment = facade.createExperiment(openBIS, project.getPermId(), ENTITY_CODE_PREFIX + UUID.randomUUID());

        facade.assertExperimentExistsAtAS(experiment.getPermId().getPermId(), true);
        facade.assertAFSDataSetExistsAtAS(experiment.getPermId().getPermId(), false);
        facade.assertDataExistsInStoreInShare(experiment.getPermId().getPermId(), false, null);

        openBIS.getAfsServerFacade().write(experiment.getPermId().getPermId(), "test-file.txt", 0L, "test-content".getBytes());
        openBIS.getAfsServerFacade().write(experiment.getPermId().getPermId(), "test-file-2.txt", 0L, "test-content-2".getBytes());

        ExperimentDeletionOptions options = new ExperimentDeletionOptions();
        options.setReason("It is just a test");

        facade.assertExperimentExistsAtAS(experiment.getPermId().getPermId(), true);
        facade.assertAFSDataSetExistsAtAS(experiment.getPermId().getPermId(), true);
        facade.assertDataExistsInStoreInShare(experiment.getPermId().getPermId(), true, 1);

        IDeletionId deletionId = openBIS.deleteExperiments(List.of(experiment.getPermId()), options);
        openBIS.confirmDeletions(List.of(deletionId));

        facade.assertExperimentExistsAtAS(experiment.getPermId().getPermId(), false);
        facade.assertAFSDataSetExistsAtAS(experiment.getPermId().getPermId(), false);
        // we need to wait for both AS events-search-task and AFS serverObserver
        Thread.sleep(WAITING_TIME_FOR_ASYNC_TASKS);
        facade.assertDataExistsInStoreInShare(experiment.getPermId().getPermId(), false, null);
    }

    @Test
    public void testDeleteSampleWithAfsDataSet() throws Exception
    {
        OpenBIS openBIS = facade.createOpenBIS();

        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        Sample sample = facade.createSample(openBIS, new SpacePermId(DEFAULT_SPACE), ENTITY_CODE_PREFIX + UUID.randomUUID());

        facade.assertSampleExistsAtAS(sample.getPermId().getPermId(), true);
        facade.assertAFSDataSetExistsAtAS(sample.getPermId().getPermId(), false);
        facade.assertDataExistsInStoreInShare(sample.getPermId().getPermId(), false, null);

        openBIS.getAfsServerFacade().write(sample.getPermId().getPermId(), "test-file.txt", 0L, "test-content".getBytes());
        openBIS.getAfsServerFacade().write(sample.getPermId().getPermId(), "test-file-2.txt", 0L, "test-content-2".getBytes());

        SampleDeletionOptions options = new SampleDeletionOptions();
        options.setReason("It is just a test");

        facade.assertSampleExistsAtAS(sample.getPermId().getPermId(), true);
        facade.assertAFSDataSetExistsAtAS(sample.getPermId().getPermId(), true);
        facade.assertDataExistsInStoreInShare(sample.getPermId().getPermId(), true, 1);

        IDeletionId deletionId = openBIS.deleteSamples(List.of(sample.getPermId()), options);
        openBIS.confirmDeletions(List.of(deletionId));

        facade.assertSampleExistsAtAS(sample.getPermId().getPermId(), false);
        facade.assertAFSDataSetExistsAtAS(sample.getPermId().getPermId(), false);
        // we need to wait for both AS events-search-task and AFS serverObserver
        Thread.sleep(WAITING_TIME_FOR_ASYNC_TASKS);
        facade.assertDataExistsInStoreInShare(sample.getPermId().getPermId(), false, null);
    }

    @Test
    public void testSearchAfsDataSetFilesViaDataStoreServerWithDefaultStoreKind() throws Exception
    {
        OpenBIS openBIS = facade.createOpenBIS();

        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        Sample sample = facade.createSample(openBIS, new SpacePermId(DEFAULT_SPACE), ENTITY_CODE_PREFIX + UUID.randomUUID());
        openBIS.getAfsServerFacade().write(sample.getPermId().getPermId(), "test-file.txt", 0L, "test-content".getBytes());
        openBIS.getAfsServerFacade().write(sample.getPermId().getPermId(), "test-file-2.txt", 0L, "test-content-2".getBytes());

        DataSetFileSearchCriteria criteria = new DataSetFileSearchCriteria();
        DataSetSearchCriteria dataSetCriteria = criteria.withDataSet();
        dataSetCriteria.withCode().thatEquals(sample.getPermId().getPermId());

        SearchResult<DataSetFile> result = openBIS.getDataStoreFacade().searchFiles(criteria, new DataSetFileFetchOptions());
        assertEquals(result.getObjects().size(), 0);
    }

    @Test
    public void testSearchAfsDataSetFilesViaDataStoreServerWithDssStoreKind() throws Exception
    {
        OpenBIS openBIS = facade.createOpenBIS();

        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        Sample sample = facade.createSample(openBIS, new SpacePermId(DEFAULT_SPACE), ENTITY_CODE_PREFIX + UUID.randomUUID());
        openBIS.getAfsServerFacade().write(sample.getPermId().getPermId(), "test-file.txt", 0L, "test-content".getBytes());
        openBIS.getAfsServerFacade().write(sample.getPermId().getPermId(), "test-file-2.txt", 0L, "test-content-2".getBytes());

        DataSetFileSearchCriteria criteria = new DataSetFileSearchCriteria();
        DataSetSearchCriteria dataSetCriteria = criteria.withDataSet();
        dataSetCriteria.withDataStore().withKind().thatIn(DataStoreKind.DSS);
        dataSetCriteria.withCode().thatEquals(sample.getPermId().getPermId());

        SearchResult<DataSetFile> result = openBIS.getDataStoreFacade().searchFiles(criteria, new DataSetFileFetchOptions());
        assertEquals(result.getObjects().size(), 0);
    }

    @Test
    public void testSearchAfsDataSetFilesViaDataStoreServerWithAfsStoreKind() throws Exception
    {
        OpenBIS openBIS = facade.createOpenBIS();

        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        Sample sample = facade.createSample(openBIS, new SpacePermId(DEFAULT_SPACE), ENTITY_CODE_PREFIX + UUID.randomUUID());
        openBIS.getAfsServerFacade().write(sample.getPermId().getPermId(), "test-file.txt", 0L, "test-content".getBytes());
        openBIS.getAfsServerFacade().write(sample.getPermId().getPermId(), "test-file-2.txt", 0L, "test-content-2".getBytes());

        DataSetFileSearchCriteria criteria = new DataSetFileSearchCriteria();
        DataSetSearchCriteria dataSetCriteria = criteria.withDataSet();
        dataSetCriteria.withDataStore().withKind().thatIn(DataStoreKind.AFS);
        dataSetCriteria.withCode().thatEquals(sample.getPermId().getPermId());

        SearchResult<DataSetFile> result = openBIS.getDataStoreFacade().searchFiles(criteria, new DataSetFileFetchOptions());
        assertEquals(result.getObjects().size(), 3);
        assertEquals(result.getObjects().get(0).getPath(), "");
        assertEquals(result.getObjects().get(1).getPath(), "test-file-2.txt");
        assertEquals(result.getObjects().get(2).getPath(), "test-file.txt");
    }

    @Test
    public void testDownloadAfsDataSetFilesViaDataStoreServer() throws Exception
    {
        OpenBIS openBIS = facade.createOpenBIS();

        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        Sample sample = facade.createSample(openBIS, new SpacePermId(DEFAULT_SPACE), ENTITY_CODE_PREFIX + UUID.randomUUID());
        openBIS.getAfsServerFacade().write(sample.getPermId().getPermId(), "test-file.txt", 0L, "test-content".getBytes());
        openBIS.getAfsServerFacade().write(sample.getPermId().getPermId(), "test-file-2.txt", 0L, "test-content-2".getBytes());

        DataSetFileSearchCriteria criteria = new DataSetFileSearchCriteria();
        DataSetSearchCriteria dataSetCriteria = criteria.withDataSet();
        dataSetCriteria.withDataStore().withKind().thatIn(DataStoreKind.AFS);
        dataSetCriteria.withCode().thatEquals(sample.getPermId().getPermId());

        SearchResult<DataSetFile> result = openBIS.getDataStoreFacade().searchFiles(criteria, new DataSetFileFetchOptions());

        InputStream stream =
                openBIS.getDataStoreFacade().downloadFiles(result.getObjects().stream().map(DataSetFile::getPermId).collect(Collectors.toList()),
                        new DataSetFileDownloadOptions());
        DataSetFileDownloadReader reader = new DataSetFileDownloadReader(stream);
        DataSetFileDownload file;

        Map<String, String> downloadedFiles = new HashedMap<>();

        while ((file = reader.read()) != null)
        {
            if (file.getDataSetFile().isDirectory())
            {
                downloadedFiles.put(file.getDataSetFile().getPath(), "");
            } else
            {
                downloadedFiles.put(file.getDataSetFile().getPath(), IOUtils.toString(file.getInputStream()));
            }
        }

        assertEquals(downloadedFiles.size(), 3);
        assertEquals(downloadedFiles.get(""), "");
        assertEquals(downloadedFiles.get("test-file.txt"), "test-content");
        assertEquals(downloadedFiles.get("test-file-2.txt"), "test-content-2");
    }

    private void deleteLastSeenDeletionFile() throws Exception
    {
        Configuration configuration = new Configuration(environment.getAfsServer().getServiceProperties());
        String lastSeenDeletionFile = OpenBISConfiguration.getInstance(configuration).getOpenBISLastSeenDeletionFile();
        Files.deleteIfExists(Path.of(lastSeenDeletionFile));
    }

}
