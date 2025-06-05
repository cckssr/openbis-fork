package ch.ethz.sis.openbis.systemtests;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import ch.ethz.sis.afsserver.server.common.TestLogger;
import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.ArchivingStatus;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.systemtests.common.AbstractIntegrationTest;
import ch.ethz.sis.openbis.systemtests.common.TestMessagesConsumerMaintenanceTask;
import ch.ethz.sis.openbis.systemtests.common.TestPathInfoDatabaseFeedingTask;
import ch.ethz.sis.shared.io.IOUtils;
import ch.systemsx.cisd.base.utilities.OSUtilities;
import ch.systemsx.cisd.common.logging.LogCategory;
import ch.systemsx.cisd.common.logging.LogFactory;
import ch.systemsx.cisd.common.process.ProcessExecutionHelper;
import ch.systemsx.cisd.common.process.ProcessResult;
import ch.systemsx.cisd.common.test.AssertionUtil;
import ch.systemsx.cisd.openbis.dss.generic.server.plugins.standard.archiver.MultiDataSetFileOperationsManager;
import ch.systemsx.cisd.openbis.dss.generic.shared.ArchiverServiceProviderFactory;

public class IntegrationArchivingTest extends AbstractIntegrationTest
{

    private static final Logger log = LogFactory.getLogger(LogCategory.OPERATION, IntegrationArchivingTest.class);

    private static final String COMMON_MESSAGES_CONSUMER_TASK = "commonMessagesConsumerTask";

    private static final String ARCHIVING_MESSAGES_CONSUMER_TASK = "archivingMessagesConsumerTask";

    private static final String FINALIZE_ARCHIVING_MESSAGES_CONSUMER_TASK = "finalizeArchivingMessagesConsumerTask";

    private static final String ENTITY_CODE_PREFIX = "ARCHIVING_TEST_";

    private static final String TEST_FILE_NAME = "test-file.txt";

    private static final String TEST_FILE_CONTENT = "test-content";

    private Space space;

    private File finalDestination;

    private File stagingDestination;

    private File replicatedDestination;

    @BeforeClass public void beforeClass() throws Exception
    {
        Properties archiverProperties = ArchiverServiceProviderFactory.getInstance().getArchiverProperties();

        finalDestination = new File(archiverProperties.getProperty(MultiDataSetFileOperationsManager.FINAL_DESTINATION_KEY));
        stagingDestination = new File(archiverProperties.getProperty(MultiDataSetFileOperationsManager.STAGING_DESTINATION_KEY));
        replicatedDestination = new File(archiverProperties.getProperty(MultiDataSetFileOperationsManager.REPLICATED_DESTINATION_KEY));

        IOUtils.delete(finalDestination.getPath());
        IOUtils.delete(stagingDestination.getPath());
        IOUtils.delete(replicatedDestination.getPath());

        IOUtils.createDirectories(finalDestination.getPath());
        IOUtils.createDirectories(stagingDestination.getPath());
        IOUtils.createDirectories(replicatedDestination.getPath());

        OpenBIS openBIS = createOpenBIS();
        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        space = createSpace(openBIS, "ARCHIVING");

        openBIS.logout();
    }

    @BeforeMethod
    public void beforeMethod(Method method) throws Exception
    {
        super.beforeMethod(method);
        TestLogger.startLogRecording(Level.TRACE, TestLogger.DEFAULT_LOG_LAYOUT_PATTERN, ".*");
    }

    @AfterMethod
    public void afterMethod(Method method) throws Exception
    {
        super.afterMethod(method);
        TestLogger.stopLogRecording();
    }

    @Test
    public void testSuccessfulArchiving() throws Exception
    {
        OpenBIS openBIS = createOpenBIS();

        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        // create a sample
        Sample sample = createSample(openBIS, space.getPermId(), ENTITY_CODE_PREFIX + UUID.randomUUID());

        // upload a file
        openBIS.getAfsServerFacade().write(sample.getPermId().getPermId(), TEST_FILE_NAME, 0L, TEST_FILE_CONTENT.getBytes());

        // check data set is AVAILABLE
        DataSet afsDataSet = getAfsDataSet(openBIS, sample.getPermId().getPermId());
        assertEquals(afsDataSet.getPhysicalData().getStatus(), ArchivingStatus.AVAILABLE);

        // create archiving message
        ArchiverServiceProviderFactory.getInstance().getOpenBISService().archiveDataSets(List.of(sample.getPermId().getPermId()), true, Map.of());

        // consume archiving message
        TestMessagesConsumerMaintenanceTask.executeOnce(ARCHIVING_MESSAGES_CONSUMER_TASK);

        // check data set is still AVAILABLE (it shouldn't have been archived as it is still mutable)
        afsDataSet = getAfsDataSet(openBIS, sample.getPermId().getPermId());
        assertEquals(afsDataSet.getPhysicalData().getStatus(), ArchivingStatus.AVAILABLE);

        // make sample immutable
        makeSampleImmutable(openBIS, sample.getPermId());

        // populate path info db (only immutable data added considered)
        TestPathInfoDatabaseFeedingTask.executeOnce();

        // create archiving message again
        ArchiverServiceProviderFactory.getInstance().getOpenBISService().archiveDataSets(List.of(sample.getPermId().getPermId()), true, Map.of());

        // consume archiving message
        TestMessagesConsumerMaintenanceTask.executeOnce(ARCHIVING_MESSAGES_CONSUMER_TASK);

        // check data set is now ARCHIVE_PENDING
        afsDataSet = getAfsDataSet(openBIS, sample.getPermId().getPermId());
        assertEquals(afsDataSet.getPhysicalData().getStatus(), ArchivingStatus.ARCHIVE_PENDING);

        // replicate from final-destination to replica-destination
        replicateDataSetArchive(sample.getPermId().getPermId());

        // consume finalize archiving message
        TestMessagesConsumerMaintenanceTask.executeOnce(FINALIZE_ARCHIVING_MESSAGES_CONSUMER_TASK);

        // check data set is ARCHIVED
        afsDataSet = getAfsDataSet(openBIS, sample.getPermId().getPermId());
        assertEquals(afsDataSet.getPhysicalData().getStatus(), ArchivingStatus.ARCHIVED);

        // check data set can still be read
        byte[] fileContent = openBIS.getAfsServerFacade().read(sample.getPermId().getPermId(), TEST_FILE_NAME, 0L, TEST_FILE_CONTENT.length());
        assertEquals(fileContent, TEST_FILE_CONTENT.getBytes());

        // consume delete data set message
        TestMessagesConsumerMaintenanceTask.executeOnce(COMMON_MESSAGES_CONSUMER_TASK);

        // wait until data set is deleted
        TestLogger.waitUntilCondition(
                recordedLog -> Arrays.stream(recordedLog)
                        .anyMatch(line -> line.matches(".*Data set " + sample.getPermId().getPermId() + " at .* has been successfully deleted.*")),
                5000);

        try
        {
            // check data set can no longer be read
            openBIS.getAfsServerFacade().read(sample.getPermId().getPermId(), TEST_FILE_NAME, 0L, TEST_FILE_CONTENT.length());
            fail();
        } catch (Exception e)
        {
            AssertionUtil.assertContains("NoSuchFileException", e.getMessage());
        }
    }

    public DataSet getAfsDataSet(OpenBIS openBIS, String dataSetCode)
    {
        DataSetPermId dataSetId = new DataSetPermId(dataSetCode);
        DataSetFetchOptions fetchOptions = new DataSetFetchOptions();
        fetchOptions.withPhysicalData();
        return openBIS.getDataSets(List.of(dataSetId), fetchOptions).get(dataSetId);
    }

    public void replicateDataSetArchive(String dataSetCode)
    {
        File[] finalDestinationFiles = finalDestination.listFiles();

        if (finalDestinationFiles == null || finalDestinationFiles.length == 0)
        {
            throw new RuntimeException("Replication failed. The final destination folder is empty. ");
        }

        List<File> dataSetArchives =
                Arrays.stream(finalDestinationFiles).filter(file -> file.getName().endsWith(".tar") && file.getName().contains(dataSetCode))
                        .sorted(Comparator.comparing(File::lastModified).reversed()).toList();

        if (dataSetArchives.isEmpty())
        {
            throw new RuntimeException(
                    "Replication failed. No archives for data set " + dataSetCode + " were found in the final destination folder.");
        }

        File newestDataSetArchive = dataSetArchives.iterator().next();
        File dataSetArchiveReplica = new File(replicatedDestination, newestDataSetArchive.getName());

        try
        {
            IOUtils.copy(newestDataSetArchive.getPath(), dataSetArchiveReplica.getPath());
        } catch (IOException e)
        {
            throw new RuntimeException(
                    "Replication failed. Could not copy file " + newestDataSetArchive.getAbsolutePath() + " to "
                            + dataSetArchiveReplica.getAbsolutePath(), e);
        }

        File shell = OSUtilities.findExecutable("sh");

        if (shell == null)
        {
            throw new RuntimeException("Replication failed. Shell command necessary for setting T flag was not found.");
        }

        List<String> setTFlagCommand = Arrays.asList(shell.getAbsolutePath(), "-c", "chmod +t " + dataSetArchiveReplica.getAbsoluteFile());
        ProcessResult setTFlagResult = ProcessExecutionHelper.run(setTFlagCommand, log, log);

        if (setTFlagResult.isOK())
        {
            log("Successfully replicated file " + newestDataSetArchive.getAbsolutePath() + " to " + dataSetArchiveReplica.getAbsolutePath() + ".");
        } else
        {
            throw new RuntimeException("Replication failed. Could not set the T flag on file " + dataSetArchiveReplica.getAbsolutePath());
        }
    }

}
