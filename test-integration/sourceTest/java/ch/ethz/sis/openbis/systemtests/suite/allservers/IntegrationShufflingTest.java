package ch.ethz.sis.openbis.systemtests.suite.allservers;

import static ch.ethz.sis.openbis.systemtests.suite.allservers.environment.AllServersIntegrationTestEnvironment.INSTANCE_ADMIN;
import static ch.ethz.sis.openbis.systemtests.suite.allservers.environment.AllServersIntegrationTestEnvironment.PASSWORD;
import static ch.ethz.sis.openbis.systemtests.suite.allservers.environment.AllServersIntegrationTestEnvironment.environment;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import ch.ethz.sis.openbis.afsserver.server.common.TestLogger;
import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.delete.SampleDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.systemtests.environment.IntegrationTestFacade;
import ch.ethz.sis.openbis.systemtests.suite.allservers.environment.AllServersIntegrationTestEnvironment;
import ch.ethz.sis.openbis.systemtests.suite.allservers.environment.TestMessagesConsumerMaintenanceTask;
import ch.ethz.sis.openbis.systemtests.suite.allservers.environment.TestSegmentedStoreShufflingTask;
import ch.ethz.sis.openbis.systemtests.suite.allservers.environment.TestSegmentedStoreShufflingTask.TestChecksumProvider;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.ethz.sis.shared.log.classic.impl.Logger;
import ch.ethz.sis.shared.log.standard.core.Level;
import ch.systemsx.cisd.common.concurrent.MessageChannel;
import ch.systemsx.cisd.common.test.AssertionUtil;

public class IntegrationShufflingTest
{

    private static final Logger log = LogFactory.getLogger(IntegrationShufflingTest.class);

    private static final String EAGER_SHUFFLING_MESSAGES_CONSUMER_TASK = "eagerShufflingMessagesConsumerTask";

    private static final String ENTITY_CODE_PREFIX = "SHUFFLING_TEST_";

    private static final String TEST_FILE_NAME = "test-file.txt";

    private static final String TEST_FILE_CONTENT = "test-content";

    private static final String TEST_FILE_CONTENT_2 = "test-content-2";

    private IntegrationTestFacade facade;

    private Experiment experimentShuffledToShare2;

    private Experiment experimentShuffledToShare3;

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

    @BeforeClass
    public void beforeClass() throws Exception
    {
        IntegrationTestFacade facade = new IntegrationTestFacade(environment);

        OpenBIS openBIS = facade.createOpenBIS();
        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        Space space = facade.createSpace(openBIS, "SHUFFLE");
        Project project = facade.createProject(openBIS, space.getPermId(), "SHUFFLE");

        experimentShuffledToShare2 = facade.createExperiment(openBIS, project.getPermId(), "SHUFFLE_TO_SHARE_2");
        experimentShuffledToShare3 = facade.createExperiment(openBIS, project.getPermId(), "SHUFFLE_TO_SHARE_3");

        log.info("Created experiment " + experimentShuffledToShare2.getIdentifier() + " with perm id " + experimentShuffledToShare2.getPermId());
        log.info("Created experiment " + experimentShuffledToShare3.getIdentifier() + " with perm id " + experimentShuffledToShare3.getPermId());

        openBIS.logout();
    }

    @BeforeMethod
    public void beforeMethod(Method method) throws Exception
    {
        TestLogger.startLogRecording(Level.TRACE, TestLogger.DEFAULT_LOG_LAYOUT_PATTERN, ".*Shuffling.*");
        facade = new IntegrationTestFacade(environment);
    }

    @AfterMethod
    public void afterMethod(Method method) throws Exception
    {
        TestLogger.stopLogRecording();
    }

    @Test
    public void testAFSDataIsShuffledByAFS() throws Exception
    {
        OpenBIS openBIS = facade.createOpenBIS();
        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        // create data at AFS (should be stored in the incoming share i.e. 1)
        Sample sample = facade.createSample(openBIS, experimentShuffledToShare2.getPermId(), ENTITY_CODE_PREFIX + UUID.randomUUID());

        openBIS.getAfsServerFacade()
                .write(sample.getPermId().getPermId(), TEST_FILE_NAME, 0L, TEST_FILE_CONTENT.getBytes());

        facade.assertDataExistsInStoreInShare(sample.getPermId().getPermId(), true, 1);
        facade.assertDataExistsInStoreInShare(sample.getPermId().getPermId(), false, 2);

        TestChecksumProvider checksumProvider = new TestChecksumProvider();
        TestSegmentedStoreShufflingTask.executeOnce(checksumProvider);

        facade.assertDataExistsInStoreInShare(sample.getPermId().getPermId(), true, 2);
        facade.assertDataExistsInStoreInShare(sample.getPermId().getPermId(), false, 1);

        AssertionUtil.assertContainsLines(
                "INFO  OPERATION.EagerShufflingTask - Locked data set " + sample.getPermId().getPermId()
                        + " before shuffling.\n"
                        + "INFO  OPERATION.EagerShufflingTask - Unlocked data set " + sample.getPermId().getPermId()
                        + " after shuffling.\n"
                        + "INFO  OPERATION.EagerShufflingTask - Data set " + sample.getPermId().getPermId()
                        + " successfully moved from share 1 to 2.\n",
                TestLogger.getRecordedLog());
    }

    @Test
    public void testDSSDataIsNotShuffledByAFS() throws Exception
    {
        OpenBIS openBIS = facade.createOpenBIS();
        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        // create data at DSS (should be stored in the incoming share i.e. 1)
        DataSet dataSet =
                facade.createDataSet(openBIS, experimentShuffledToShare2.getPermId(), ENTITY_CODE_PREFIX + UUID.randomUUID(), TEST_FILE_NAME,
                        TEST_FILE_CONTENT.getBytes());

        facade.assertDataExistsInStoreInShare(dataSet.getPermId().getPermId(), true, 1);
        facade.assertDataExistsInStoreInShare(dataSet.getPermId().getPermId(), false, 2);

        TestChecksumProvider checksumProvider = new TestChecksumProvider();
        TestSegmentedStoreShufflingTask.executeOnce(checksumProvider);

        facade.assertDataExistsInStoreInShare(dataSet.getPermId().getPermId(), true, 1);
        facade.assertDataExistsInStoreInShare(dataSet.getPermId().getPermId(), false, 2);
    }

    @Test
    public void testEagerShuffling() throws Exception
    {
        OpenBIS openBIS = facade.createOpenBIS();
        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        // create data at AFS (should be stored in the incoming share i.e. 1)
        Sample sample1 = facade.createSample(openBIS, experimentShuffledToShare2.getPermId(), ENTITY_CODE_PREFIX + UUID.randomUUID());
        Sample sample2 = facade.createSample(openBIS, experimentShuffledToShare2.getPermId(), ENTITY_CODE_PREFIX + UUID.randomUUID());

        openBIS.getAfsServerFacade()
                .write(sample1.getPermId().getPermId(), TEST_FILE_NAME, 0L, TEST_FILE_CONTENT.getBytes());
        openBIS.getAfsServerFacade()
                .write(sample2.getPermId().getPermId(), TEST_FILE_NAME, 0L, TEST_FILE_CONTENT_2.getBytes());

        // shares before
        facade.assertDataExistsInStoreInShare(sample1.getPermId().getPermId(), true, 1);
        facade.assertDataExistsInStoreInShare(sample2.getPermId().getPermId(), true, 1);

        facade.assertDataExistsInStoreInShare(sample1.getPermId().getPermId(), false, 2);
        facade.assertDataExistsInStoreInShare(sample2.getPermId().getPermId(), false, 2);

        // shuffle
        TestMessagesConsumerMaintenanceTask.executeOnce(EAGER_SHUFFLING_MESSAGES_CONSUMER_TASK);

        // shares after
        facade.assertDataExistsInStoreInShare(sample1.getPermId().getPermId(), false, 1);
        facade.assertDataExistsInStoreInShare(sample2.getPermId().getPermId(), false, 1);

        facade.assertDataExistsInStoreInShare(sample1.getPermId().getPermId(), true, 2);
        facade.assertDataExistsInStoreInShare(sample2.getPermId().getPermId(), true, 2);

        AssertionUtil.assertContainsLines(
                "INFO  OPERATION.EagerShufflingMessageHandler - Locked data set " + sample1.getPermId().getPermId()
                        + " before shuffling.\n"
                        + "INFO  OPERATION.EagerShufflingMessageHandler - Unlocked data set " + sample1.getPermId().getPermId()
                        + " after shuffling.\n"
                        + "INFO  OPERATION.EagerShufflingMessageHandler - Data set " + sample1.getPermId().getPermId()
                        + " successfully moved from share 1 to 2.\n" +
                        "INFO  OPERATION.EagerShufflingMessageHandler - Locked data set " + sample2.getPermId().getPermId()
                        + " before shuffling.\n"
                        + "INFO  OPERATION.EagerShufflingMessageHandler - Unlocked data set " + sample2.getPermId().getPermId()
                        + " after shuffling.\n"
                        + "INFO  OPERATION.EagerShufflingMessageHandler - Data set " + sample2.getPermId().getPermId()
                        + " successfully moved from share 1 to 2.\n",
                TestLogger.getRecordedLog());
    }

    @Test
    public void testEagerShufflingWithDataSetNotFound() throws Exception
    {
        OpenBIS openBIS = facade.createOpenBIS();
        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        // create data at AFS (should be stored in the incoming share i.e. 1)
        Sample sample1 = facade.createSample(openBIS, experimentShuffledToShare2.getPermId(), ENTITY_CODE_PREFIX + UUID.randomUUID());
        Sample sample2 = facade.createSample(openBIS, experimentShuffledToShare2.getPermId(), ENTITY_CODE_PREFIX + UUID.randomUUID());

        openBIS.getAfsServerFacade()
                .write(sample1.getPermId().getPermId(), TEST_FILE_NAME, 0L, TEST_FILE_CONTENT.getBytes());
        openBIS.getAfsServerFacade()
                .write(sample2.getPermId().getPermId(), TEST_FILE_NAME, 0L, TEST_FILE_CONTENT_2.getBytes());

        // shares before
        facade.assertDataExistsInStoreInShare(sample1.getPermId().getPermId(), true, 1);
        facade.assertDataExistsInStoreInShare(sample2.getPermId().getPermId(), true, 1);

        facade.assertDataExistsInStoreInShare(sample1.getPermId().getPermId(), false, 2);
        facade.assertDataExistsInStoreInShare(sample2.getPermId().getPermId(), false, 2);

        // delete sample1 and therefore also AFS data set
        SampleDeletionOptions deletionOptions = new SampleDeletionOptions();
        deletionOptions.setReason("test shuffling when deleted");
        openBIS.deleteSamples(List.of(sample1.getPermId()), deletionOptions);

        Sample sample1AfterDeletion = facade.getSample(openBIS, sample1.getPermId());
        assertNull(sample1AfterDeletion);

        // shuffle
        TestMessagesConsumerMaintenanceTask.executeOnce(EAGER_SHUFFLING_MESSAGES_CONSUMER_TASK);

        // shares after
        facade.assertDataExistsInStoreInShare(sample1.getPermId().getPermId(), true, 1);
        facade.assertDataExistsInStoreInShare(sample2.getPermId().getPermId(), false, 1);

        facade.assertDataExistsInStoreInShare(sample1.getPermId().getPermId(), false, 2);
        facade.assertDataExistsInStoreInShare(sample2.getPermId().getPermId(), true, 2);

        AssertionUtil.assertContainsLines(
                "INFO  OPERATION.EagerShufflingMessageHandler - Could not find any of the data sets to be shuffled: [" + sample1.getPermId()
                        .getPermId() + "]. Nothing will be shuffled.\n" +
                        "INFO  OPERATION.EagerShufflingMessageHandler - Locked data set " + sample2.getPermId().getPermId()
                        + " before shuffling.\n"
                        + "INFO  OPERATION.EagerShufflingMessageHandler - Unlocked data set " + sample2.getPermId().getPermId()
                        + " after shuffling.\n"
                        + "INFO  OPERATION.EagerShufflingMessageHandler - Data set " + sample2.getPermId().getPermId()
                        + " successfully moved from share 1 to 2.\n",
                TestLogger.getRecordedLog());
    }

    @Test
    public void testEagerShufflingWithDataSetInArchive() throws Exception
    {
        OpenBIS openBIS = facade.createOpenBIS();
        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        // create data at AFS (should be stored in the incoming share i.e. 1)
        Sample sample1 = facade.createSample(openBIS, experimentShuffledToShare2.getPermId(), ENTITY_CODE_PREFIX + UUID.randomUUID());
        Sample sample2 = facade.createSample(openBIS, experimentShuffledToShare2.getPermId(), ENTITY_CODE_PREFIX + UUID.randomUUID());

        openBIS.getAfsServerFacade()
                .write(sample1.getPermId().getPermId(), TEST_FILE_NAME, 0L, TEST_FILE_CONTENT.getBytes());
        openBIS.getAfsServerFacade()
                .write(sample2.getPermId().getPermId(), TEST_FILE_NAME, 0L, TEST_FILE_CONTENT_2.getBytes());

        // shares before
        facade.assertDataExistsInStoreInShare(sample1.getPermId().getPermId(), true, 1);
        facade.assertDataExistsInStoreInShare(sample2.getPermId().getPermId(), true, 1);

        facade.assertDataExistsInStoreInShare(sample1.getPermId().getPermId(), false, 2);
        facade.assertDataExistsInStoreInShare(sample2.getPermId().getPermId(), false, 2);

        // archive sample1
        IntegrationArchivingTest.archive(environment, openBIS, null, sample1.getPermId());

        // shuffle
        TestMessagesConsumerMaintenanceTask.executeOnce(EAGER_SHUFFLING_MESSAGES_CONSUMER_TASK);

        // shares after
        facade.assertDataExistsInStoreInShare(sample1.getPermId().getPermId(), true, 1);
        facade.assertDataExistsInStoreInShare(sample2.getPermId().getPermId(), false, 1);

        facade.assertDataExistsInStoreInShare(sample1.getPermId().getPermId(), false, 2);
        facade.assertDataExistsInStoreInShare(sample2.getPermId().getPermId(), true, 2);

        AssertionUtil.assertContainsLines(
                "WARN  OPERATION.EagerShufflingMessageHandler - Data set " + sample1.getPermId().getPermId()
                        + " couldn't be shuffled because its archiving status is ARCHIVED\n" +
                        "INFO  OPERATION.EagerShufflingMessageHandler - Locked data set " + sample2.getPermId().getPermId()
                        + " before shuffling.\n"
                        + "INFO  OPERATION.EagerShufflingMessageHandler - Unlocked data set " + sample2.getPermId().getPermId()
                        + " after shuffling.\n"
                        + "INFO  OPERATION.EagerShufflingMessageHandler - Data set " + sample2.getPermId().getPermId()
                        + " successfully moved from share 1 to 2.\n",
                TestLogger.getRecordedLog());
    }

    @Test
    public void testDataIsLockedDuringShuffling() throws Exception
    {
        OpenBIS openBIS = facade.createOpenBIS();
        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        Sample sample = facade.createSample(openBIS, experimentShuffledToShare2.getPermId(), ENTITY_CODE_PREFIX + UUID.randomUUID());

        openBIS.getAfsServerFacade()
                .write(sample.getPermId().getPermId(), TEST_FILE_NAME, 0L, TEST_FILE_CONTENT.getBytes());

        facade.assertDataExistsInStoreInShare(sample.getPermId().getPermId(), true, 1);

        MessageChannel toShuffling = new MessageChannel(5000);
        MessageChannel fromShuffling = new MessageChannel(5000);

        Thread shufflingThread = new Thread(() ->
        {
            TestChecksumProvider checksumProvider = new TestChecksumProvider()
            {
                @Override public long getChecksum(final String dataSetCode, final String relativePath) throws IOException
                {
                    if (dataSetCode.equals(sample.getPermId().getPermId()))
                    {
                        fromShuffling.send("beforeChecksum");
                        toShuffling.assertNextMessage("afterRead");
                    }

                    return super.getChecksum(dataSetCode, relativePath);
                }
            };
            TestSegmentedStoreShufflingTask.executeOnce(checksumProvider);
        });

        shufflingThread.start();
        fromShuffling.assertNextMessage("beforeChecksum");

        try
        {
            byte[] content = openBIS.getAfsServerFacade()
                    .read(sample.getPermId().getPermId(), TEST_FILE_NAME, 0L, TEST_FILE_CONTENT.getBytes().length);
            assertEquals(new String(content), TEST_FILE_CONTENT);
            fail();
        } catch (Exception e)
        {
            assertTrue(e.getMessage().contains(TEST_FILE_NAME + " is currently being used"), e.getMessage());
        }

        toShuffling.send("afterRead");
        shufflingThread.join();

        facade.assertDataExistsInStoreInShare(sample.getPermId().getPermId(), true, 2);

        AssertionUtil.assertContainsLines(
                "INFO  OPERATION.EagerShufflingTask - Locked data set " + sample.getPermId().getPermId()
                        + " before shuffling.\n"
                        + "INFO  OPERATION.EagerShufflingTask - Unlocked data set " + sample.getPermId().getPermId()
                        + " after shuffling.\n"
                        + "INFO  OPERATION.EagerShufflingTask - Data set " + sample.getPermId().getPermId()
                        + " successfully moved from share 1 to 2.\n",
                TestLogger.getRecordedLog());
    }

    @Test
    public void testFailedShufflingWithExceptionIsCleanedUp() throws Exception
    {
        OpenBIS openBIS = facade.createOpenBIS();
        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        Sample sample = facade.createSample(openBIS, experimentShuffledToShare2.getPermId(), ENTITY_CODE_PREFIX + UUID.randomUUID());

        openBIS.getAfsServerFacade()
                .write(sample.getPermId().getPermId(), TEST_FILE_NAME, 0L, TEST_FILE_CONTENT.getBytes());

        facade.assertDataExistsInStoreInShare(sample.getPermId().getPermId(), true, 1);
        facade.assertDataExistsInStoreInShare(sample.getPermId().getPermId(), false, 2);

        TestChecksumProvider checksumProvider = new TestChecksumProvider()
        {
            @Override public long getChecksum(final String dataSetCode, final String relativePath) throws IOException
            {
                if (dataSetCode.equals(sample.getPermId().getPermId()))
                {
                    throw new RuntimeException("Test checksum exception");
                }

                return super.getChecksum(dataSetCode, relativePath);
            }
        };
        TestSegmentedStoreShufflingTask.executeOnce(checksumProvider);

        facade.assertDataExistsInStoreInShare(sample.getPermId().getPermId(), true, 1);
        facade.assertDataExistsInStoreInShare(sample.getPermId().getPermId(), false, 2);

        AssertionUtil.assertContainsLines(
                "INFO  OPERATION.EagerShufflingTask - Locked data set " + sample.getPermId().getPermId()
                        + " before shuffling.\n"
                        + "INFO  OPERATION.EagerShufflingTask - Unlocked data set " + sample.getPermId().getPermId()
                        + " after shuffling.\n"
                        + "ERROR OPERATION.EagerShufflingTask - Couldn't move data set " + sample.getPermId().getPermId() + " to share 2.\n"
                        + "INFO  OPERATION.SimpleShuffling - Await for data set " + sample.getPermId().getPermId()
                        + " to be unlocked.\n",
                TestLogger.getRecordedLog());

        AssertionUtil.assertDoesNotContainLines(
                "INFO  OPERATION.EagerShufflingTask - Data set " + sample.getPermId().getPermId()
                        + " successfully moved from share 1 to 2.\n", TestLogger.getRecordedLog());
    }

    @Test
    public void testFailedShufflingWithIncorrectChecksumIsCleanedUp() throws Exception
    {
        OpenBIS openBIS = facade.createOpenBIS();
        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        Sample sample = facade.createSample(openBIS, experimentShuffledToShare2.getPermId(), ENTITY_CODE_PREFIX + UUID.randomUUID());

        openBIS.getAfsServerFacade()
                .write(sample.getPermId().getPermId(), TEST_FILE_NAME, 0L, TEST_FILE_CONTENT.getBytes());

        facade.assertDataExistsInStoreInShare(sample.getPermId().getPermId(), true, 1);
        facade.assertDataExistsInStoreInShare(sample.getPermId().getPermId(), false, 2);

        TestChecksumProvider checksumProvider = new TestChecksumProvider()
        {
            @Override public long getChecksum(final String dataSetCode, final String relativePath) throws IOException
            {
                if (dataSetCode.equals(sample.getPermId().getPermId()))
                {
                    // +1 to make the checksum incorrect
                    return super.getChecksum(dataSetCode, relativePath) + 1;
                }

                return super.getChecksum(dataSetCode, relativePath);
            }
        };
        TestSegmentedStoreShufflingTask.executeOnce(checksumProvider);

        facade.assertDataExistsInStoreInShare(sample.getPermId().getPermId(), true, 1);
        facade.assertDataExistsInStoreInShare(sample.getPermId().getPermId(), false, 2);

        AssertionUtil.assertContainsLines(
                "INFO  OPERATION.EagerShufflingTask - Locked data set " + sample.getPermId().getPermId()
                        + " before shuffling.\n"
                        + "INFO  OPERATION.EagerShufflingTask - Unlocked data set " + sample.getPermId().getPermId()
                        + " after shuffling.\n"
                        + "ERROR OPERATION.EagerShufflingTask - Couldn't move data set " + sample.getPermId().getPermId() + " to share 2.\n"
                        + "INFO  OPERATION.SimpleShuffling - Await for data set " + sample.getPermId().getPermId()
                        + " to be unlocked.\n",
                TestLogger.getRecordedLog());

        AssertionUtil.assertDoesNotContainLines(
                "INFO  OPERATION.EagerShufflingTask - Data set " + sample.getPermId().getPermId()
                        + " successfully moved from share 1 to 2.\n", TestLogger.getRecordedLog());
    }

}
