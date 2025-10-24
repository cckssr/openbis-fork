package ch.ethz.sis.openbis.systemtests;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;

import ch.ethz.sis.shared.log.standard.core.Level;
import ch.ethz.sis.shared.log.classic.impl.LogManager;
import ch.ethz.sis.shared.log.classic.impl.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import ch.ethz.sis.openbis.afsserver.server.common.TestLogger;
import ch.ethz.sis.foldermonitor.FolderMonitor;
import ch.ethz.sis.foldermonitor.FolderMonitorConfiguration;
import ch.ethz.sis.openbis.generic.foldermonitor.v3.FolderMonitorTask;
import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import ch.ethz.sis.openbis.systemtests.common.AbstractIntegrationTest;
import ch.systemsx.cisd.common.filesystem.FileConstants;
import ch.systemsx.cisd.common.filesystem.FileUtilities;

public class IntegrationFolderMonitor extends AbstractIntegrationTest
{

    private static final String SPACE_CODE = "FOLDER_MONITOR";

    private Path incomingFoldersRoot;

    @BeforeClass
    public void beforeClass() throws Exception
    {
        OpenBIS openBIS = createOpenBIS();
        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        createSpace(openBIS, SPACE_CODE);

        incomingFoldersRoot = Path.of("targets/folder-monitor");

        if (Files.exists(incomingFoldersRoot))
        {
            FileUtilities.deleteRecursively(incomingFoldersRoot.toFile());
            log("Deleted " + incomingFoldersRoot);
        }

        Files.createDirectory(incomingFoldersRoot);
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
    public void testMarkerFileConfiguration() throws Exception
    {
        final String INCOMING_FILE_NAME = "test_marker_file_configuration";
        final String CREATED_SAMPLE_CODE = "TEST_MARKER_FILE_CONFIGURATION_ABC";

        Path incomingWithMarkerFile = Files.createDirectory(incomingFoldersRoot.resolve("incoming-with-marker-file"));

        Properties properties = new Properties();
        properties.load(new BufferedInputStream(new FileInputStream("sourceTest/resource/" + getClass().getSimpleName()
                + "/marker-file.properties")));

        FolderMonitor monitor = new FolderMonitor(new FolderMonitorConfiguration(properties));
        monitor.start();

        OpenBIS openBIS = createOpenBIS();
        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        Sample sample = getSample(openBIS, new SampleIdentifier(SPACE_CODE, null, CREATED_SAMPLE_CODE));
        Assert.assertNull(sample);

        Path incomingFile = Files.createFile(incomingWithMarkerFile.resolve(INCOMING_FILE_NAME));

        Thread.sleep(2000);

        sample = getSample(openBIS, new SampleIdentifier(SPACE_CODE, null, CREATED_SAMPLE_CODE));
        Assert.assertNull(sample);

        Path markerFile = Files.createFile(incomingWithMarkerFile.resolve(FileConstants.IS_FINISHED_PREFIX + INCOMING_FILE_NAME));

        waitUntilCondition(() ->
                {
                    String[] recordedLines = TestLogger.getRecordedLog().split("\n");
                    return Arrays.stream(recordedLines).anyMatch(line -> line.matches(
                            ".*FolderMonitor - After processing of path 'targets/folder-monitor/incoming-with-marker-file/test_marker_file_configuration'.*"));
                },
                5000);

        sample = getSample(openBIS, new SampleIdentifier(SPACE_CODE, null, CREATED_SAMPLE_CODE));
        Assert.assertNotNull(sample);

        Assert.assertFalse(Files.exists(incomingFile));
        Assert.assertFalse(Files.exists(markerFile));

        monitor.stop();
    }

    @Test
    public void testQuietPeriodConfiguration() throws Exception
    {
        final String INCOMING_FILE_NAME = "test_quiet_period_configuration";
        final String CREATED_SAMPLE_CODE = "TEST_QUIET_PERIOD_CONFIGURATION_DEF";

        Path incomingWithQuietPeriod = Files.createDirectory(incomingFoldersRoot.resolve("incoming-with-quiet-period"));

        Properties properties = new Properties();
        properties.load(new BufferedInputStream(new FileInputStream("sourceTest/resource/" + getClass().getSimpleName()
                + "/quiet-period.properties")));

        FolderMonitor monitor = new FolderMonitor(new FolderMonitorConfiguration(properties));
        monitor.start();

        OpenBIS openBIS = createOpenBIS();
        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        Sample sample = getSample(openBIS, new SampleIdentifier(SPACE_CODE, null, CREATED_SAMPLE_CODE));
        Assert.assertNull(sample);

        Path incomingFile = Files.createFile(incomingWithQuietPeriod.resolve(INCOMING_FILE_NAME));

        waitUntilCondition(() ->
                {
                    String[] recordedLines = TestLogger.getRecordedLog().split("\n");
                    return Arrays.stream(recordedLines).anyMatch(line -> line.matches(
                            ".*FolderMonitor - After processing of path 'targets/folder-monitor/incoming-with-quiet-period/test_quiet_period_configuration'.*"));
                },
                5000);

        sample = getSample(openBIS, new SampleIdentifier(SPACE_CODE, null, CREATED_SAMPLE_CODE));
        Assert.assertNotNull(sample);

        Assert.assertFalse(Files.exists(incomingFile));

        monitor.stop();
    }

    @Test
    public void testFailingTask() throws Exception
    {
        final String INCOMING_FAILING_FILE_NAME = "test_failing";
        final String INCOMING_SUCCESS_FILE_NAME = "test_success";

        Path incomingWithFailingTask = Files.createDirectory(incomingFoldersRoot.resolve("incoming-with-failing-task"));

        Properties properties = new Properties();
        properties.load(new BufferedInputStream(new FileInputStream("sourceTest/resource/" + getClass().getSimpleName()
                + "/failing-task.properties")));

        FolderMonitor monitor = new FolderMonitor(new FolderMonitorConfiguration(properties));
        monitor.start();

        Path incomingFailingFile = Files.createFile(incomingWithFailingTask.resolve(INCOMING_FAILING_FILE_NAME));
        Path markerFailingFile = Files.createFile(incomingWithFailingTask.resolve(FileConstants.IS_FINISHED_PREFIX + INCOMING_FAILING_FILE_NAME));

        waitUntilCondition(() ->
                {
                    String[] recordedLines = TestLogger.getRecordedLog().split("\n");
                    return Arrays.stream(recordedLines).anyMatch(line -> line.matches(
                            ".*FolderMonitor - Processing of path 'targets/folder-monitor/incoming-with-failing-task/test_failing' has failed.*"));
                },
                5000);

        Assert.assertTrue(Files.exists(incomingFailingFile));
        Assert.assertTrue(Files.exists(markerFailingFile));

        Path incomingSuccessFile = Files.createFile(incomingWithFailingTask.resolve(INCOMING_SUCCESS_FILE_NAME));
        Path markerSuccessFile = Files.createFile(incomingWithFailingTask.resolve(FileConstants.IS_FINISHED_PREFIX + INCOMING_SUCCESS_FILE_NAME));

        waitUntilCondition(() ->
                {
                    String[] recordedLines = TestLogger.getRecordedLog().split("\n");
                    return Arrays.stream(recordedLines).anyMatch(line -> line.matches(
                            ".*FolderMonitor - After processing of path 'targets/folder-monitor/incoming-with-failing-task/test_success'.*"));
                },
                5000);

        Assert.assertTrue(Files.exists(incomingFailingFile));
        Assert.assertTrue(Files.exists(markerFailingFile));
        Assert.assertFalse(Files.exists(incomingSuccessFile));
        Assert.assertFalse(Files.exists(markerSuccessFile));

        monitor.stop();
    }

    public static class TestTask implements FolderMonitorTask
    {

        private static final Logger logger = LogManager.getLogger(TestTask.class);

        private Properties properties;

        @Override public void configure(final Properties properties)
        {
            this.properties = properties;
            log("Configured with properties: " + properties);
        }

        @Override public void process(final Path incoming)
        {
            log("Started processing path: " + incoming);

            OpenBIS openBIS = createOpenBIS();
            openBIS.login(INSTANCE_ADMIN, PASSWORD);

            String sampleCodeSuffix = properties.getProperty("sample-code-suffix");

            Space space = getSpace(openBIS, new SpacePermId("FOLDER_MONITOR"));
            createSample(openBIS, space.getId(), incoming.getFileName().toString() + sampleCodeSuffix);

            log("Finished processing path: " + incoming);
        }
    }

    public static class TestFailingTask implements FolderMonitorTask
    {

        private static final Logger logger = LogManager.getLogger(TestFailingTask.class);

        @Override public void configure(final Properties properties)
        {
        }

        @Override public void process(final Path incoming)
        {
            log("Started processing path: " + incoming);

            if (incoming.getFileName().toString().contains("fail"))
            {
                throw new RuntimeException("Intentionally failing after a sample is created");
            }
        }
    }

}
