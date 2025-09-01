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
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.IExperimentId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.id.PropertyTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.ISampleId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.systemtests.common.AbstractIntegrationTest;
import ch.systemsx.cisd.common.exceptions.UserFailureException;

public class IntegrationElnDropboxTest extends AbstractIntegrationTest
{

    private Space space;

    private Project project;

    private SampleType sampleType;

    @BeforeClass public void beforeClass() throws Exception
    {
        OpenBIS openBIS = createOpenBIS();
        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        space = createSpace(openBIS, "ELN_DROPBOX_SPACE");
        project = createProject(openBIS, space.getPermId(), "ELN_DROPBOX_PROJECT");
        sampleType = createSampleType(openBIS, "ELN_DROPBOX_SAMPLE_TYPE", List.of(new PropertyTypePermId("NAME")));

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

        assertFile(openBIS, files[0], "test-file.txt", "/test-file.txt", "test-content");
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
        assertFile(openBIS, files[1], "test-file.txt", "/default/test-file.txt", "test-content");
        assertDirectory(files[2], "test-folder", "/default/test-folder");
        assertFile(openBIS, files[3], "test-file-in-folder.txt", "/default/test-folder/test-file-in-folder.txt", "test-content-in-folder");
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
        assertFile(openBIS, files[1], "test-file-in-folder.txt", "/test-folder/test-file-in-folder.txt", "test-content-in-folder");
    }

    @Test
    public void testRegisterInSpaceSample()
    {
        OpenBIS openBIS = createOpenBIS();
        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        Sample spaceSample = createSample(openBIS, space.getPermId(), "TEST_REGISTER_IN_SPACE_SAMPLE");

        List<Sample> before = searchSampleChildren(openBIS, spaceSample.getPermId());
        Assert.assertEquals(before.size(), 0);

        executeDropbox(new Properties(), Path.of("sourceTest/resource/" + getClass().getSimpleName()
                + "/testRegisterInSpaceSample/S+ELN_DROPBOX_SPACE+TEST_REGISTER_IN_SPACE_SAMPLE+ELN_DROPBOX_SAMPLE_TYPE+ELN_DROPBOX_SAMPLE_NAME"));

        List<Sample> after = searchSampleChildren(openBIS, spaceSample.getPermId());
        Assert.assertEquals(after.size(), 1);

        Sample owner = after.get(0);
        Assert.assertEquals(owner.getType().getCode(), sampleType.getCode());
        Assert.assertEquals(owner.getSpace().getCode(), "ELN_DROPBOX_SPACE");
        Assert.assertNull(owner.getProject());
        Assert.assertNull(owner.getExperiment());
        Assert.assertEquals(owner.getProperties().get("NAME"), "ELN_DROPBOX_SAMPLE_NAME");

        File[] files = openBIS.getAfsServerFacade().list(owner.getPermId().getPermId(), "", true);
        Arrays.sort(files, Comparator.comparing(File::getPath));
        Assert.assertEquals(files.length, 1);

        assertFile(openBIS, files[0], "test-in-space-sample.txt", "/test-in-space-sample.txt", "test-in-space-sample-content");
    }

    @Test
    public void testRegisterInNonExistentSpaceSample()
    {
        OpenBIS openBIS = createOpenBIS();
        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        try
        {
            executeDropbox(new Properties(), Path.of("sourceTest/resource/" + getClass().getSimpleName()
                    + "/testRegisterInNonExistentSpaceSample/S+ELN_DROPBOX_SPACE+TEST_REGISTER_IN_NON_EXISTENT_SPACE_SAMPLE"));
            Assert.fail();
        } catch (UserFailureException e)
        {
            Assert.assertEquals(e.getMessage(),
                    "Invalid format for the folder name, should follow the pattern <ENTITY_KIND>+<SPACE_CODE>+<PROJECT_CODE>+[<EXPERIMENT_CODE>|<SAMPLE_CODE>]+<OPTIONAL_DATASET_TYPE>+<OPTIONAL_NAME>:Sample not found");
        }
    }

    @Test
    public void testRegisterInProjectSample()
    {
        OpenBIS openBIS = createOpenBIS();
        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        Sample projectSample = createSample(openBIS, project.getPermId(), "TEST_REGISTER_IN_PROJECT_SAMPLE");

        List<Sample> before = searchSampleChildren(openBIS, projectSample.getPermId());
        Assert.assertEquals(before.size(), 0);

        executeDropbox(new Properties(), Path.of("sourceTest/resource/" + getClass().getSimpleName()
                + "/testRegisterInProjectSample/O+ELN_DROPBOX_SPACE+ELN_DROPBOX_PROJECT+TEST_REGISTER_IN_PROJECT_SAMPLE+ELN_DROPBOX_SAMPLE_TYPE+ELN_DROPBOX_SAMPLE_NAME"));

        List<Sample> after = searchSampleChildren(openBIS, projectSample.getPermId());
        Assert.assertEquals(after.size(), 1);

        Sample owner = after.get(0);
        Assert.assertEquals(owner.getType().getCode(), sampleType.getCode());
        Assert.assertEquals(owner.getSpace().getCode(), "ELN_DROPBOX_SPACE");
        Assert.assertEquals(owner.getProject().getCode(), "ELN_DROPBOX_PROJECT");
        Assert.assertNull(owner.getExperiment());
        Assert.assertEquals(owner.getProperties().get("NAME"), "ELN_DROPBOX_SAMPLE_NAME");

        File[] files = openBIS.getAfsServerFacade().list(owner.getPermId().getPermId(), "", true);
        Arrays.sort(files, Comparator.comparing(File::getPath));
        Assert.assertEquals(files.length, 1);

        assertFile(openBIS, files[0], "test-in-project-sample.txt", "/test-in-project-sample.txt", "test-in-project-sample-content");
    }

    @Test
    public void testRegisterInNonExistentProjectSample()
    {
        OpenBIS openBIS = createOpenBIS();
        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        try
        {
            executeDropbox(new Properties(), Path.of("sourceTest/resource/" + getClass().getSimpleName()
                    + "/testRegisterInNonExistentProjectSample/O+ELN_DROPBOX_SPACE+ELN_DROPBOX_PROJECT+TEST_REGISTER_IN_NON_EXISTENT_PROJECT_SAMPLE"));
            Assert.fail();
        } catch (UserFailureException e)
        {
            Assert.assertEquals(e.getMessage(),
                    "Invalid format for the folder name, should follow the pattern <ENTITY_KIND>+<SPACE_CODE>+<PROJECT_CODE>+[<EXPERIMENT_CODE>|<SAMPLE_CODE>]+<OPTIONAL_DATASET_TYPE>+<OPTIONAL_NAME>:Sample not found");
        }
    }

    @Test
    public void testRegisterInExperiment()
    {
        OpenBIS openBIS = createOpenBIS();
        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        Experiment experiment = createExperiment(openBIS, project.getPermId(), "TEST_REGISTER_IN_EXPERIMENT");

        List<Sample> before = searchExperimentSamples(openBIS, experiment.getPermId());
        Assert.assertEquals(before.size(), 0);

        executeDropbox(new Properties(), Path.of("sourceTest/resource/" + getClass().getSimpleName()
                + "/testRegisterInExperiment/E+ELN_DROPBOX_SPACE+ELN_DROPBOX_PROJECT+TEST_REGISTER_IN_EXPERIMENT+ELN_DROPBOX_SAMPLE_TYPE+ELN_DROPBOX_SAMPLE_NAME"));

        List<Sample> after = searchExperimentSamples(openBIS, experiment.getPermId());
        Assert.assertEquals(after.size(), 1);

        Sample owner = after.get(0);
        Assert.assertEquals(owner.getType().getCode(), sampleType.getCode());
        Assert.assertEquals(owner.getSpace().getCode(), "ELN_DROPBOX_SPACE");
        Assert.assertEquals(owner.getProject().getCode(), "ELN_DROPBOX_PROJECT");
        Assert.assertEquals(owner.getExperiment().getCode(), "TEST_REGISTER_IN_EXPERIMENT");
        Assert.assertEquals(owner.getProperties().get("NAME"), "ELN_DROPBOX_SAMPLE_NAME");

        File[] files = openBIS.getAfsServerFacade().list(owner.getPermId().getPermId(), "", true);
        Arrays.sort(files, Comparator.comparing(File::getPath));
        Assert.assertEquals(files.length, 1);

        assertFile(openBIS, files[0], "test-in-experiment.txt", "/test-in-experiment.txt", "test-in-experiment-content");
    }

    @Test
    public void testRegisterInNonExistentExperiment()
    {
        OpenBIS openBIS = createOpenBIS();
        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        try
        {
            executeDropbox(new Properties(), Path.of("sourceTest/resource/" + getClass().getSimpleName()
                    + "/testRegisterInNonExistentExperiment/E+ELN_DROPBOX_SPACE+ELN_DROPBOX_PROJECT+TEST_REGISTER_IN_NON_EXISTENT_EXPERIMENT"));
            Assert.fail();
        } catch (UserFailureException e)
        {
            Assert.assertEquals(e.getMessage(),
                    "Invalid format for the folder name, should follow the pattern <ENTITY_KIND>+<SPACE_CODE>+<PROJECT_CODE>+[<EXPERIMENT_CODE>|<SAMPLE_CODE>]+<OPTIONAL_DATASET_TYPE>+<OPTIONAL_NAME>:Experiment not found");
        }
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

    private static List<Sample> searchExperimentSamples(OpenBIS openBIS, IExperimentId experimentId)
    {
        SampleSearchCriteria criteria = new SampleSearchCriteria();
        criteria.withExperiment().withId().thatEquals(experimentId);
        SampleFetchOptions fetchOptions = new SampleFetchOptions();
        fetchOptions.withSpace();
        fetchOptions.withProject();
        fetchOptions.withExperiment();
        fetchOptions.withType();
        fetchOptions.withProperties();
        return openBIS.searchSamples(criteria, fetchOptions).getObjects();
    }

    private static List<Sample> searchSampleChildren(OpenBIS openBIS, ISampleId parentId)
    {
        SampleSearchCriteria criteria = new SampleSearchCriteria();
        criteria.withParents().withId().thatEquals(parentId);
        SampleFetchOptions fetchOptions = new SampleFetchOptions();
        fetchOptions.withSpace();
        fetchOptions.withProject();
        fetchOptions.withExperiment();
        fetchOptions.withType();
        fetchOptions.withProperties();
        return openBIS.searchSamples(criteria, fetchOptions).getObjects();
    }

    private static void assertDirectory(File file, String expectedName, String expectedPath)
    {
        Assert.assertEquals(file.getName(), expectedName);
        Assert.assertEquals(file.getPath(), expectedPath);
        Assert.assertNull(file.getSize());
        Assert.assertEquals(file.getDirectory(), Boolean.TRUE);
    }

    private static void assertFile(OpenBIS openBIS, File file, String expectedName, String expectedPath, String expectedContent)
    {
        Assert.assertEquals(file.getName(), expectedName);
        Assert.assertEquals(file.getPath(), expectedPath);
        Assert.assertEquals(file.getSize(), Long.valueOf(expectedContent.length()));
        Assert.assertEquals(file.getDirectory(), Boolean.FALSE);

        byte[] content = openBIS.getAfsServerFacade().read(file.getOwner(), file.getPath(), 0L, file.getSize().intValue());
        Assert.assertEquals(new String(content), expectedContent);
    }
}
