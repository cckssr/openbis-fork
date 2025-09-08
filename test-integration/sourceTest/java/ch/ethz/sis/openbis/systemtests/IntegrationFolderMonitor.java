package ch.ethz.sis.openbis.systemtests;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import ch.ethz.sis.foldermonitor.FolderMonitor;
import ch.ethz.sis.foldermonitor.FolderMonitorConfiguration;
import ch.ethz.sis.foldermonitor.FolderMonitorTask;
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

    private Path incomingWithMarkerFile;

    private Path incomingWithQuietPeriod;

    @BeforeClass
    public void beforeClass()
    {
        OpenBIS openBIS = createOpenBIS();
        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        createSpace(openBIS, SPACE_CODE);
    }

    @BeforeTest
    public void beforeTest() throws Exception
    {
        Path folder = Path.of("targets/folder-monitor");

        if (Files.exists(folder))
        {
            FileUtilities.deleteRecursively(folder.toFile());
            log("Deleted " + folder);
        }

        Files.createDirectory(folder);
        incomingWithMarkerFile = Files.createDirectory(folder.resolve("incoming-with-marker-file"));
        incomingWithQuietPeriod = Files.createDirectory(folder.resolve("incoming-with-quiet-period"));
    }

    @Test
    public void testMarkerFileConfiguration() throws Exception
    {
        final String INCOMING_FILE = "test_marker_file_configuration";
        final String CREATED_SAMPLE = "TEST_MARKER_FILE_CONFIGURATION_ABC";

        Properties properties = new Properties();
        properties.load(new BufferedInputStream(new FileInputStream("sourceTest/resource/" + getClass().getSimpleName()
                + "/marker-file.properties")));

        FolderMonitor monitor = new FolderMonitor(new FolderMonitorConfiguration(properties));
        monitor.start();

        OpenBIS openBIS = createOpenBIS();
        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        Sample sample = getSample(openBIS, new SampleIdentifier(SPACE_CODE, null, CREATED_SAMPLE));
        Assert.assertNull(sample);

        Files.createFile(incomingWithMarkerFile.resolve(INCOMING_FILE));

        Thread.sleep(2000);

        sample = getSample(openBIS, new SampleIdentifier(SPACE_CODE, null, CREATED_SAMPLE));
        Assert.assertNull(sample);

        Files.createFile(incomingWithMarkerFile.resolve(FileConstants.IS_FINISHED_PREFIX + INCOMING_FILE));

        Thread.sleep(2000);

        sample = getSample(openBIS, new SampleIdentifier(SPACE_CODE, null, CREATED_SAMPLE));
        Assert.assertNotNull(sample);

        monitor.stop();
    }

    @Test
    public void testQuietPeriodConfiguration() throws Exception
    {
        final String INCOMING_FILE = "test_quiet_period_configuration";
        final String CREATED_SAMPLE = "TEST_QUIET_PERIOD_CONFIGURATION_DEF";

        Properties properties = new Properties();
        properties.load(new BufferedInputStream(new FileInputStream("sourceTest/resource/" + getClass().getSimpleName()
                + "/quiet-period.properties")));

        FolderMonitor monitor = new FolderMonitor(new FolderMonitorConfiguration(properties));
        monitor.start();

        OpenBIS openBIS = createOpenBIS();
        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        Sample sample = getSample(openBIS, new SampleIdentifier(SPACE_CODE, null, CREATED_SAMPLE));
        Assert.assertNull(sample);

        Files.createFile(incomingWithQuietPeriod.resolve(INCOMING_FILE));

        Thread.sleep(2000);

        sample = getSample(openBIS, new SampleIdentifier(SPACE_CODE, null, CREATED_SAMPLE));
        Assert.assertNotNull(sample);

        monitor.stop();
    }

    public static class TestFolderMonitorTask implements FolderMonitorTask
    {

        private static final Logger logger = LogManager.getLogger(TestFolderMonitorTask.class);

        private Properties properties;

        @Override public void configure(final Properties properties)
        {
            this.properties = properties;
            logger.info("Configured with properties: " + properties);
        }

        @Override public void process(final Path incoming)
        {
            logger.info("Started processing path: " + incoming);

            OpenBIS openBIS = createOpenBIS();
            openBIS.login(INSTANCE_ADMIN, PASSWORD);

            String sampleCodeSuffix = properties.getProperty("sample-code-suffix");

            Space space = getSpace(openBIS, new SpacePermId("FOLDER_MONITOR"));
            createSample(openBIS, space.getId(), incoming.getFileName().toString() + sampleCodeSuffix);

            logger.info("Finished processing path: " + incoming);
        }

    }

}
