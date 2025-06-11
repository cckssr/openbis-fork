package ch.ethz.sis.openbis.systemtests;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.UUID;

import org.apache.log4j.Level;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afsserver.server.common.DatabaseConfiguration;
import ch.ethz.sis.afsserver.server.common.OpenBISConfiguration;
import ch.ethz.sis.afsserver.server.common.TestLogger;
import ch.ethz.sis.afsserver.server.pathinfo.PathInfoDatabaseConfiguration;
import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import ch.ethz.sis.openbis.systemtests.common.AbstractIntegrationTest;
import ch.ethz.sis.openbis.systemtests.common.TestPathInfoDatabaseFeedingTask;
import ch.ethz.sis.pathinfo.IPathInfoAutoClosingDAO;
import net.lemnik.eodsql.QueryTool;

public class IntegrationPathInfoTest extends AbstractIntegrationTest
{

    private static final String ENTITY_CODE_PREFIX = "PATH_INFO_TEST_";

    private static final long WAITING_TIME_FOR_PATH_INFO_DELETION = 3000L;

    private IPathInfoAutoClosingDAO pathInfoDAO;

    @BeforeMethod
    public void beforeMethod(Method method) throws Exception
    {
        super.beforeMethod(method);

        TestLogger.startLogRecording(Level.INFO, TestLogger.DEFAULT_LOG_LAYOUT_PATTERN, ".*");

        deleteLastSeenDeletionFile();

        DatabaseConfiguration pathInfoDatabaseConfiguration = PathInfoDatabaseConfiguration.getInstance(getAfsServerConfiguration());
        pathInfoDAO = QueryTool.getQuery(pathInfoDatabaseConfiguration.getDataSource(), IPathInfoAutoClosingDAO.class);
    }

    @AfterMethod
    public void afterMethod(Method method) throws Exception
    {
        super.afterMethod(method);
        TestLogger.stopLogRecording();
    }

    @Test
    public void testPathInfoDBEntriesGetCreatedAndDeleted() throws Exception
    {
        OpenBIS openBIS = createOpenBIS();
        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        SpacePermId spaceId = new SpacePermId(DEFAULT_SPACE);
        Project project = createProject(openBIS, spaceId, ENTITY_CODE_PREFIX + UUID.randomUUID());

        // create both mutable and immutable experiments and samples
        Experiment mutableExperiment = createExperiment(openBIS, project.getPermId(), ENTITY_CODE_PREFIX + UUID.randomUUID());
        Experiment immutableExperiment = createExperiment(openBIS, project.getPermId(), ENTITY_CODE_PREFIX + UUID.randomUUID());
        Sample mutableSample = createSample(openBIS, spaceId, ENTITY_CODE_PREFIX + UUID.randomUUID());
        Sample immutableSample = createSample(openBIS, spaceId, ENTITY_CODE_PREFIX + UUID.randomUUID());

        Long mutableExperimentEntryId = pathInfoDAO.tryToGetDataSetId(mutableExperiment.getPermId().getPermId());
        Long immutableExperimentEntryId = pathInfoDAO.tryToGetDataSetId(immutableExperiment.getPermId().getPermId());
        Long mutableSampleEntryId = pathInfoDAO.tryToGetDataSetId(mutableSample.getPermId().getPermId());
        Long immutableSampleEntryId = pathInfoDAO.tryToGetDataSetId(immutableSample.getPermId().getPermId());

        assertNull(mutableExperimentEntryId);
        assertNull(immutableExperimentEntryId);
        assertNull(mutableSampleEntryId);
        assertNull(immutableSampleEntryId);

        String testFile = "test-file-" + UUID.randomUUID();
        String testData = "test-content-" + UUID.randomUUID();

        // write data to the experiments and samples
        openBIS.getAfsServerFacade().write(mutableExperiment.getPermId().getPermId(), testFile, 0L, testData.getBytes());
        openBIS.getAfsServerFacade().write(immutableExperiment.getPermId().getPermId(), testFile, 0L, testData.getBytes());
        openBIS.getAfsServerFacade().write(mutableSample.getPermId().getPermId(), testFile, 0L, testData.getBytes());
        openBIS.getAfsServerFacade().write(immutableSample.getPermId().getPermId(), testFile, 0L, testData.getBytes());

        // make chosen data immutable
        makeExperimentImmutable(openBIS, immutableExperiment.getPermId());
        makeSampleImmutable(openBIS, immutableSample.getPermId());

        TestPathInfoDatabaseFeedingTask.executeOnce();

        // verify that path info entries got created for the immutable data
        mutableExperimentEntryId = pathInfoDAO.tryToGetDataSetId(mutableExperiment.getPermId().getPermId());
        immutableExperimentEntryId = pathInfoDAO.tryToGetDataSetId(immutableExperiment.getPermId().getPermId());
        mutableSampleEntryId = pathInfoDAO.tryToGetDataSetId(mutableSample.getPermId().getPermId());
        immutableSampleEntryId = pathInfoDAO.tryToGetDataSetId(immutableSample.getPermId().getPermId());

        assertNull(mutableExperimentEntryId);
        assertNotNull(immutableExperimentEntryId);
        assertNull(mutableSampleEntryId);
        assertNotNull(immutableSampleEntryId);

        // delete the experiments and samples
        deleteExperiment(openBIS, mutableExperiment.getPermId());
        deleteExperiment(openBIS, immutableExperiment.getPermId());
        deleteSample(openBIS, mutableSample.getPermId());
        deleteSample(openBIS, immutableSample.getPermId());

        Thread.sleep(WAITING_TIME_FOR_PATH_INFO_DELETION);

        // verify that path info entries got deleted
        mutableExperimentEntryId = pathInfoDAO.tryToGetDataSetId(mutableExperiment.getPermId().getPermId());
        immutableExperimentEntryId = pathInfoDAO.tryToGetDataSetId(immutableExperiment.getPermId().getPermId());
        mutableSampleEntryId = pathInfoDAO.tryToGetDataSetId(mutableSample.getPermId().getPermId());
        immutableSampleEntryId = pathInfoDAO.tryToGetDataSetId(immutableSample.getPermId().getPermId());

        assertNull(mutableExperimentEntryId);
        assertNull(immutableExperimentEntryId);
        assertNull(mutableSampleEntryId);
        assertNull(immutableSampleEntryId);
    }

    @Test
    public void testAFSListMethodReturnsTheSameResultsWithAndWithoutPathInfoDBEntries() throws Exception
    {
        final String FOUND_IN_PATH_INFO_DB = "found in the path info database";

        OpenBIS openBIS = createOpenBIS();
        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        SpacePermId spaceId = new SpacePermId(DEFAULT_SPACE);
        Sample sample = createSample(openBIS, spaceId, ENTITY_CODE_PREFIX + UUID.randomUUID());

        Long sampleEntryId = pathInfoDAO.tryToGetDataSetId(sample.getPermId().getPermId());
        assertNull(sampleEntryId);

        String testData1 = "test-content-" + UUID.randomUUID();
        String testData2 = "test-content-" + UUID.randomUUID();
        String testData3 = "test-content-" + UUID.randomUUID();

        // create files and folders
        openBIS.getAfsServerFacade().create(sample.getPermId().getPermId(), "test-folder", true);
        openBIS.getAfsServerFacade().write(sample.getPermId().getPermId(), "test-file-1", 0L, testData1.getBytes());
        openBIS.getAfsServerFacade().write(sample.getPermId().getPermId(), "test-folder/test-file-2", 0L, testData2.getBytes());
        openBIS.getAfsServerFacade().write(sample.getPermId().getPermId(), "test-folder/test-file-3_%$.txt", 0L, testData3.getBytes());

        // FS : list "" recursively
        File[] rootRecursivelyFS = openBIS.getAfsServerFacade().list(sample.getPermId().getPermId(), "", true);
        Arrays.sort(rootRecursivelyFS, Comparator.comparing(File::getPath));

        assertEquals(rootRecursivelyFS.length, 4);
        assertFile(rootRecursivelyFS[0], sample.getPermId().getPermId(), "/test-file-1", "test-file-1", Boolean.FALSE, 49L);
        assertFile(rootRecursivelyFS[1], sample.getPermId().getPermId(), "/test-folder", "test-folder", Boolean.TRUE, null);
        assertFile(rootRecursivelyFS[2], sample.getPermId().getPermId(), "/test-folder/test-file-2", "test-file-2", Boolean.FALSE, 49L);
        assertFile(rootRecursivelyFS[3], sample.getPermId().getPermId(), "/test-folder/test-file-3_%$.txt", "test-file-3_%$.txt", Boolean.FALSE,
                49L);

        // FS : list "/" non-recursively
        File[] rootNotRecursiveFS = openBIS.getAfsServerFacade().list(sample.getPermId().getPermId(), "/", false);
        Arrays.sort(rootNotRecursiveFS, Comparator.comparing(File::getPath));

        assertEquals(2, rootNotRecursiveFS.length);
        assertFile(rootNotRecursiveFS[0], sample.getPermId().getPermId(), "/test-file-1", "test-file-1", Boolean.FALSE, 49L);
        assertFile(rootNotRecursiveFS[1], sample.getPermId().getPermId(), "/test-folder", "test-folder", Boolean.TRUE, null);

        // FS : list "//" non-recursively
        File[] rootNotRecursive2FS = openBIS.getAfsServerFacade().list(sample.getPermId().getPermId(), "//", false);
        Arrays.sort(rootNotRecursive2FS, Comparator.comparing(File::getPath));

        assertEquals(2, rootNotRecursive2FS.length);
        assertFile(rootNotRecursive2FS[0], sample.getPermId().getPermId(), "/test-file-1", "test-file-1", Boolean.FALSE, 49L);
        assertFile(rootNotRecursive2FS[1], sample.getPermId().getPermId(), "/test-folder", "test-folder", Boolean.TRUE, null);

        // FS : list "test-folder" recursively
        File[] folderRecursiveFS = openBIS.getAfsServerFacade().list(sample.getPermId().getPermId(), "test-folder", true);
        Arrays.sort(folderRecursiveFS, Comparator.comparing(File::getPath));

        assertEquals(folderRecursiveFS.length, 2);
        assertFile(folderRecursiveFS[0], sample.getPermId().getPermId(), "/test-folder/test-file-2", "test-file-2", Boolean.FALSE, 49L);
        assertFile(folderRecursiveFS[1], sample.getPermId().getPermId(), "/test-folder/test-file-3_%$.txt", "test-file-3_%$.txt", Boolean.FALSE,
                49L);

        // FS : list "/test-folder/" recursively
        File[] folderRecursive2FS = openBIS.getAfsServerFacade().list(sample.getPermId().getPermId(), "/test-folder/", true);
        Arrays.sort(folderRecursive2FS, Comparator.comparing(File::getPath));

        assertEquals(folderRecursive2FS.length, 2);
        assertFile(folderRecursive2FS[0], sample.getPermId().getPermId(), "/test-folder/test-file-2", "test-file-2", Boolean.FALSE, 49L);
        assertFile(folderRecursive2FS[1], sample.getPermId().getPermId(), "/test-folder/test-file-3_%$.txt", "test-file-3_%$.txt", Boolean.FALSE,
                49L);

        // FS : list "/test-folder" non-recursively
        File[] folderNotRecursiveFS = openBIS.getAfsServerFacade().list(sample.getPermId().getPermId(), "/test-folder", false);
        Arrays.sort(folderNotRecursiveFS, Comparator.comparing(File::getPath));

        assertEquals(folderNotRecursiveFS.length, 2);
        assertFile(folderNotRecursiveFS[0], sample.getPermId().getPermId(), "/test-folder/test-file-2", "test-file-2", Boolean.FALSE, 49L);
        assertFile(folderNotRecursiveFS[1], sample.getPermId().getPermId(), "/test-folder/test-file-3_%$.txt", "test-file-3_%$.txt",
                Boolean.FALSE, 49L);

        // FS : list "test-folder/test-file-2" recursively
        File[] fileRecursiveFS = openBIS.getAfsServerFacade().list(sample.getPermId().getPermId(), "test-folder/test-file-2", true);
        assertEquals(fileRecursiveFS.length, 1);
        assertFile(fileRecursiveFS[0], sample.getPermId().getPermId(), "/test-folder/test-file-2", "test-file-2", Boolean.FALSE, 49L);

        // FS : list "/test-folder/test-file-2" non-recursively
        File[] fileNotRecursiveFS = openBIS.getAfsServerFacade().list(sample.getPermId().getPermId(), "/test-folder/test-file-2", false);
        assertEquals(fileNotRecursiveFS.length, 1);
        assertFile(fileNotRecursiveFS[0], sample.getPermId().getPermId(), "/test-folder/test-file-2", "test-file-2", Boolean.FALSE, 49L);

        // make data immutable
        makeSampleImmutable(openBIS, sample.getPermId());
        TestPathInfoDatabaseFeedingTask.executeOnce();

        sampleEntryId = pathInfoDAO.tryToGetDataSetId(sample.getPermId().getPermId());
        assertNotNull(sampleEntryId);

        assertContains(TestLogger.getRecordedLog(), FOUND_IN_PATH_INFO_DB, 0);

        // DB : list "" recursively
        File[] rootRecursiveDB = openBIS.getAfsServerFacade().list(sample.getPermId().getPermId(), "", true);
        assertFilesEqual(rootRecursivelyFS, rootRecursiveDB);
        assertContains(TestLogger.getRecordedLog(), FOUND_IN_PATH_INFO_DB, 1);

        // DB : list "/" non-recursively
        File[] rootNotRecursiveDB = openBIS.getAfsServerFacade().list(sample.getPermId().getPermId(), "/", false);
        assertFilesEqual(rootNotRecursiveFS, rootNotRecursiveDB);
        assertContains(TestLogger.getRecordedLog(), FOUND_IN_PATH_INFO_DB, 2);

        // DB : list "//" non-recursively
        File[] rootNotRecursive2DB = openBIS.getAfsServerFacade().list(sample.getPermId().getPermId(), "//", false);
        assertFilesEqual(rootNotRecursive2FS, rootNotRecursive2DB);
        assertContains(TestLogger.getRecordedLog(), FOUND_IN_PATH_INFO_DB, 3);

        // DB : list "test-folder" recursively
        File[] folderRecursiveDB = openBIS.getAfsServerFacade().list(sample.getPermId().getPermId(), "test-folder", true);
        assertFilesEqual(folderRecursiveFS, folderRecursiveDB);
        assertContains(TestLogger.getRecordedLog(), FOUND_IN_PATH_INFO_DB, 4);

        // DB : list "/test-folder/" recursively
        File[] folderRecursive2DB = openBIS.getAfsServerFacade().list(sample.getPermId().getPermId(), "/test-folder/", true);
        assertFilesEqual(folderRecursive2FS, folderRecursive2DB);
        assertContains(TestLogger.getRecordedLog(), FOUND_IN_PATH_INFO_DB, 5);

        // DB : list "/test-folder" non-recursively
        File[] folderNotRecursiveDB = openBIS.getAfsServerFacade().list(sample.getPermId().getPermId(), "/test-folder", false);
        assertFilesEqual(folderNotRecursiveFS, folderNotRecursiveDB);
        assertContains(TestLogger.getRecordedLog(), FOUND_IN_PATH_INFO_DB, 6);

        // DB : list "test-folder/test-file-2" recursively
        File[] fileRecursiveDB = openBIS.getAfsServerFacade().list(sample.getPermId().getPermId(), "test-folder/test-file-2", true);
        assertFilesEqual(fileRecursiveFS, fileRecursiveDB);
        assertContains(TestLogger.getRecordedLog(), FOUND_IN_PATH_INFO_DB, 7);

        // DB : list "/test-folder/test-file-2" non-recursively
        File[] fileNotRecursiveDB = openBIS.getAfsServerFacade().list(sample.getPermId().getPermId(), "/test-folder/test-file-2", false);
        assertFilesEqual(fileNotRecursiveFS, fileNotRecursiveDB);
        assertContains(TestLogger.getRecordedLog(), FOUND_IN_PATH_INFO_DB, 8);

        // delete data
        deleteSample(openBIS, sample.getPermId());
        Thread.sleep(WAITING_TIME_FOR_PATH_INFO_DELETION);

        sampleEntryId = pathInfoDAO.tryToGetDataSetId(sample.getPermId().getPermId());
        assertNull(sampleEntryId);
    }

    private void deleteLastSeenDeletionFile() throws Exception
    {
        String lastSeenDeletionFile = OpenBISConfiguration.getInstance(getAfsServerConfiguration()).getOpenBISLastSeenDeletionFile();
        Files.deleteIfExists(Path.of(lastSeenDeletionFile));
    }

    public void assertFilesEqual(File[] files1, File[] files2) throws Exception
    {
        assertEquals(files1.length, files2.length);

        Arrays.sort(files1, Comparator.comparing(File::getPath));
        Arrays.sort(files2, Comparator.comparing(File::getPath));

        for (int i = 0; i < files1.length; i++)
        {
            File file1 = files1[i];
            File file2 = files2[i];
            assertEquals(file1.getOwner(), file2.getOwner());
            assertEquals(file1.getPath(), file2.getPath());
            assertEquals(file1.getName(), file2.getName());
            assertEquals(file1.getDirectory(), file2.getDirectory());
            assertEquals(file1.getSize(), file2.getSize());
            assertEquals(file1.getLastModifiedTime(), file2.getLastModifiedTime());
        }
    }

    public void assertFile(File file, String expectedOwner, String expectedPath, String expectedName, Boolean expectedDirectory, Long expectedSize)
    {
        assertEquals(file.getOwner(), expectedOwner);
        assertEquals(file.getPath(), expectedPath);
        assertEquals(file.getName(), expectedName);
        assertEquals(file.getDirectory(), expectedDirectory);
        assertEquals(file.getSize(), expectedSize);
        assertNotNull(file.getLastModifiedTime());
    }

    public void assertContains(String str, String substring, int count)
    {
        int counter = 0;
        int indexOf = 0;

        while (true)
        {
            indexOf = str.indexOf(substring, indexOf);

            if (indexOf == -1)
            {
                break;
            } else
            {
                indexOf += substring.length();
                counter++;
            }
        }

        assertEquals(counter, count);
    }

}
