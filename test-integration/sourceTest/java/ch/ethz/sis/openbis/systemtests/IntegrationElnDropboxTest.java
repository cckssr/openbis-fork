package ch.ethz.sis.openbis.systemtests;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.elnlims.dropbox.ElnDropbox;
import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.ISampleId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.systemtests.common.AbstractIntegrationTest;

public class IntegrationElnDropboxTest extends AbstractIntegrationTest
{

    private Space space;

    private Project project;

    private Experiment experiment;

    @BeforeClass public void beforeClass() throws Exception
    {
        OpenBIS openBIS = createOpenBIS();
        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        space = createSpace(openBIS, "ELN_DROPBOX_SPACE");
        project = createProject(openBIS, space.getPermId(), "ELN_DROPBOX_PROJECT");
        experiment = createExperiment(openBIS, project.getPermId(), "ELN_DROPBOX_EXPERIMENT");

        openBIS.logout();
    }

    @Test
    public void testRegisterFile()
    {
        OpenBIS openBIS = createOpenBIS();
        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        Sample sample = createSample(openBIS, space.getPermId(), "TEST_REGISTER_FILE");

        List<Sample> before = searchSampleChildren(openBIS, sample.getPermId());
        Assert.assertEquals(before.size(), 0);

        executeDropbox(new Properties(), Path.of("sourceTest/resource/" + getClass().getSimpleName()
                + "/testRegisterFile/S+ELN_DROPBOX_SPACE+TEST_REGISTER_FILE"));

        List<Sample> after = searchSampleChildren(openBIS, sample.getPermId());
        Assert.assertEquals(after.size(), 1);

        Sample owner = after.get(0);

        File[] files = openBIS.getAfsServerFacade().list(owner.getPermId().getPermId(), "", true);
        Arrays.sort(files, Comparator.comparing(File::getPath));
        Assert.assertEquals(files.length, 1);

        assertFile(openBIS, files[0], "test-file.txt", "/test-file.txt", 12, "test-content");
    }

    @Test
    public void testRegisterFileAndFolder()
    {
        OpenBIS openBIS = createOpenBIS();
        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        Sample sample = createSample(openBIS, space.getPermId(), "TEST_REGISTER_FILE_AND_FOLDER");

        List<Sample> before = searchSampleChildren(openBIS, sample.getPermId());
        Assert.assertEquals(before.size(), 0);

        executeDropbox(new Properties(), Path.of("sourceTest/resource/" + getClass().getSimpleName()
                + "/testRegisterFileAndFolder/S+ELN_DROPBOX_SPACE+TEST_REGISTER_FILE_AND_FOLDER"));

        List<Sample> after = searchSampleChildren(openBIS, sample.getPermId());
        Assert.assertEquals(after.size(), 1);

        Sample owner = after.get(0);

        File[] files = openBIS.getAfsServerFacade().list(owner.getPermId().getPermId(), "", true);
        Arrays.sort(files, Comparator.comparing(File::getPath));
        Assert.assertEquals(files.length, 4);

        assertDirectory(files[0], "default", "/default");
        assertFile(openBIS, files[1], "test-file.txt", "/default/test-file.txt", 12, "test-content");
        assertDirectory(files[2], "test-folder", "/default/test-folder");
        assertFile(openBIS, files[3], "test-file-in-folder.txt", "/default/test-folder/test-file-in-folder.txt", 22, "test-content-in-folder");
    }

    @Test
    public void testRegisterFolder()
    {
        OpenBIS openBIS = createOpenBIS();
        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        Sample sample = createSample(openBIS, space.getPermId(), "TEST_REGISTER_FOLDER");

        List<Sample> before = searchSampleChildren(openBIS, sample.getPermId());
        Assert.assertEquals(before.size(), 0);

        executeDropbox(new Properties(), Path.of("sourceTest/resource/" + getClass().getSimpleName()
                + "/testRegisterFolder/S+ELN_DROPBOX_SPACE+TEST_REGISTER_FOLDER"));

        List<Sample> after = searchSampleChildren(openBIS, sample.getPermId());
        Assert.assertEquals(after.size(), 1);

        Sample owner = after.get(0);

        File[] files = openBIS.getAfsServerFacade().list(owner.getPermId().getPermId(), "", true);
        Arrays.sort(files, Comparator.comparing(File::getPath));
        Assert.assertEquals(files.length, 2);

        assertDirectory(files[0], "test-folder", "/test-folder");
        assertFile(openBIS, files[1], "test-file-in-folder.txt", "/test-folder/test-file-in-folder.txt", 22, "test-content-in-folder");
    }

    private static void executeDropbox(Properties properties, Path incoming)
    {
        OpenBIS openBIS = createOpenBIS();
        openBIS.login(INSTANCE_ADMIN, PASSWORD);
        openBIS.setInteractiveSessionKey(TEST_INTERACTIVE_SESSION_KEY);

        ElnDropbox elnDropbox = new ElnDropbox();
        elnDropbox.configure(properties);
        elnDropbox.process(openBIS, incoming);
    }

    private static List<Sample> searchSampleChildren(OpenBIS openBIS, ISampleId parentId)
    {
        SampleSearchCriteria criteria = new SampleSearchCriteria();
        criteria.withParents().withId().thatEquals(parentId);
        return openBIS.searchSamples(criteria, new SampleFetchOptions()).getObjects();
    }

    private static void assertDirectory(File file, String expectedName, String expectedPath)
    {
        Assert.assertEquals(file.getName(), expectedName);
        Assert.assertEquals(file.getPath(), expectedPath);
        Assert.assertNull(file.getSize());
        Assert.assertEquals(file.getDirectory(), Boolean.TRUE);
    }

    private static void assertFile(OpenBIS openBIS, File file, String expectedName, String expectedPath, long expectedSize, String expectedContent)
    {
        Assert.assertEquals(file.getName(), expectedName);
        Assert.assertEquals(file.getPath(), expectedPath);
        Assert.assertEquals(file.getSize(), Long.valueOf(expectedSize));
        Assert.assertEquals(file.getDirectory(), Boolean.FALSE);

        byte[] content = openBIS.getAfsServerFacade().read(file.getOwner(), file.getPath(), 0L, file.getSize().intValue());
        Assert.assertEquals(new String(content), expectedContent);
    }
}
