package ch.ethz.sis.openbis.systemtests;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afsserver.server.common.DatabaseConfiguration;
import ch.ethz.sis.afsserver.server.common.OpenBISConfiguration;
import ch.ethz.sis.afsserver.server.pathinfo.PathInfoDatabaseConfiguration;
import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import ch.ethz.sis.openbis.systemtests.common.AbstractIntegrationTest;
import ch.ethz.sis.pathinfo.IPathInfoAutoClosingDAO;
import net.lemnik.eodsql.QueryTool;

public class IntegrationPathInfoTest extends AbstractIntegrationTest
{

    private static final String ENTITY_CODE_PREFIX = "PATH_INFO_TEST_";

    private static final long WAITING_TIME_FOR_PATH_INFO_FEEDING = 2000L;

    private static final long WAITING_TIME_FOR_PATH_INFO_DELETION = 3000L;

    private IPathInfoAutoClosingDAO pathInfoDAO;

    @BeforeMethod
    public void beforeMethod(Method method) throws Exception
    {
        super.beforeMethod(method);

        deleteLastSeenDeletionFile();

        DatabaseConfiguration pathInfoDatabaseConfiguration = PathInfoDatabaseConfiguration.getInstance(getAfsServerConfiguration());
        pathInfoDAO = QueryTool.getQuery(pathInfoDatabaseConfiguration.getDataSource(), IPathInfoAutoClosingDAO.class);
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

        Thread.sleep(WAITING_TIME_FOR_PATH_INFO_FEEDING);

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
        List<File> rootRecursivelyFS = openBIS.getAfsServerFacade().list(sample.getPermId().getPermId(), "", true);
        rootRecursivelyFS.sort(Comparator.comparing(File::getPath));

        assertEquals(rootRecursivelyFS.size(), 4);
        assertFile(rootRecursivelyFS.get(0), sample.getPermId().getPermId(), "/test-file-1", "test-file-1", Boolean.FALSE, 49L);
        assertFile(rootRecursivelyFS.get(1), sample.getPermId().getPermId(), "/test-folder", "test-folder", Boolean.TRUE, null);
        assertFile(rootRecursivelyFS.get(2), sample.getPermId().getPermId(), "/test-folder/test-file-2", "test-file-2", Boolean.FALSE, 49L);
        assertFile(rootRecursivelyFS.get(3), sample.getPermId().getPermId(), "/test-folder/test-file-3_%$.txt", "test-file-3_%$.txt", Boolean.FALSE,
                49L);

        // FS : list "/" non-recursively
        List<File> rootNotRecursiveFS = openBIS.getAfsServerFacade().list(sample.getPermId().getPermId(), "/", false);
        rootNotRecursiveFS.sort(Comparator.comparing(File::getPath));

        assertEquals(2, rootNotRecursiveFS.size());
        assertFile(rootNotRecursiveFS.get(0), sample.getPermId().getPermId(), "/test-file-1", "test-file-1", Boolean.FALSE, 49L);
        assertFile(rootNotRecursiveFS.get(1), sample.getPermId().getPermId(), "/test-folder", "test-folder", Boolean.TRUE, null);

        // FS : list "//" non-recursively
        List<File> rootNotRecursive2FS = openBIS.getAfsServerFacade().list(sample.getPermId().getPermId(), "//", false);
        rootNotRecursive2FS.sort(Comparator.comparing(File::getPath));

        assertEquals(2, rootNotRecursive2FS.size());
        assertFile(rootNotRecursive2FS.get(0), sample.getPermId().getPermId(), "/test-file-1", "test-file-1", Boolean.FALSE, 49L);
        assertFile(rootNotRecursive2FS.get(1), sample.getPermId().getPermId(), "/test-folder", "test-folder", Boolean.TRUE, null);

        // FS : list "test-folder" recursively
        List<File> folderRecursiveFS = openBIS.getAfsServerFacade().list(sample.getPermId().getPermId(), "test-folder", true);
        folderRecursiveFS.sort(Comparator.comparing(File::getPath));

        assertEquals(folderRecursiveFS.size(), 2);
        assertFile(folderRecursiveFS.get(0), sample.getPermId().getPermId(), "/test-folder/test-file-2", "test-file-2", Boolean.FALSE, 49L);
        assertFile(folderRecursiveFS.get(1), sample.getPermId().getPermId(), "/test-folder/test-file-3_%$.txt", "test-file-3_%$.txt", Boolean.FALSE,
                49L);

        // FS : list "/test-folder/" recursively
        List<File> folderRecursive2FS = openBIS.getAfsServerFacade().list(sample.getPermId().getPermId(), "/test-folder/", true);
        folderRecursive2FS.sort(Comparator.comparing(File::getPath));

        assertEquals(folderRecursive2FS.size(), 2);
        assertFile(folderRecursive2FS.get(0), sample.getPermId().getPermId(), "/test-folder/test-file-2", "test-file-2", Boolean.FALSE, 49L);
        assertFile(folderRecursive2FS.get(1), sample.getPermId().getPermId(), "/test-folder/test-file-3_%$.txt", "test-file-3_%$.txt", Boolean.FALSE,
                49L);

        // FS : list "/test-folder" non-recursively
        List<File> folderNotRecursiveFS = openBIS.getAfsServerFacade().list(sample.getPermId().getPermId(), "/test-folder", false);
        folderNotRecursiveFS.sort(Comparator.comparing(File::getPath));

        assertEquals(folderNotRecursiveFS.size(), 2);
        assertFile(folderNotRecursiveFS.get(0), sample.getPermId().getPermId(), "/test-folder/test-file-2", "test-file-2", Boolean.FALSE, 49L);
        assertFile(folderNotRecursiveFS.get(1), sample.getPermId().getPermId(), "/test-folder/test-file-3_%$.txt", "test-file-3_%$.txt",
                Boolean.FALSE, 49L);

        // FS : list "test-folder/test-file-2" recursively
        List<File> fileRecursiveFS = openBIS.getAfsServerFacade().list(sample.getPermId().getPermId(), "test-folder/test-file-2", true);
        assertEquals(fileRecursiveFS.size(), 1);
        assertFile(fileRecursiveFS.get(0), sample.getPermId().getPermId(), "/test-folder/test-file-2", "test-file-2", Boolean.FALSE, 49L);

        // FS : list "/test-folder/test-file-2" non-recursively
        List<File> fileNotRecursiveFS = openBIS.getAfsServerFacade().list(sample.getPermId().getPermId(), "/test-folder/test-file-2", false);
        assertEquals(fileNotRecursiveFS.size(), 1);
        assertFile(fileNotRecursiveFS.get(0), sample.getPermId().getPermId(), "/test-folder/test-file-2", "test-file-2", Boolean.FALSE, 49L);

        // make data immutable
        makeSampleImmutable(openBIS, sample.getPermId());
        Thread.sleep(WAITING_TIME_FOR_PATH_INFO_FEEDING);

        sampleEntryId = pathInfoDAO.tryToGetDataSetId(sample.getPermId().getPermId());
        assertNotNull(sampleEntryId);

        // DB : list "" recursively
        List<File> rootRecursiveDB = openBIS.getAfsServerFacade().list(sample.getPermId().getPermId(), "", true);
        assertFilesEqual(rootRecursivelyFS, rootRecursiveDB);

        // DB : list "/" non-recursively
        List<File> rootNotRecursiveDB = openBIS.getAfsServerFacade().list(sample.getPermId().getPermId(), "/", false);
        assertFilesEqual(rootNotRecursiveFS, rootNotRecursiveDB);

        // DB : list "//" non-recursively
        List<File> rootNotRecursive2DB = openBIS.getAfsServerFacade().list(sample.getPermId().getPermId(), "//", false);
        assertFilesEqual(rootNotRecursive2FS, rootNotRecursive2DB);

        // DB : list "test-folder" recursively
        List<File> folderRecursiveDB = openBIS.getAfsServerFacade().list(sample.getPermId().getPermId(), "test-folder", true);
        assertFilesEqual(folderRecursiveFS, folderRecursiveDB);

        // DB : list "/test-folder/" recursively
        List<File> folderRecursive2DB = openBIS.getAfsServerFacade().list(sample.getPermId().getPermId(), "/test-folder/", true);
        assertFilesEqual(folderRecursive2FS, folderRecursive2DB);

        // DB : list "/test-folder" non-recursively
        List<File> folderNotRecursiveDB = openBIS.getAfsServerFacade().list(sample.getPermId().getPermId(), "/test-folder", false);
        assertFilesEqual(folderNotRecursiveFS, folderNotRecursiveDB);

        // DB : list "test-folder/test-file-2" recursively
        List<File> fileRecursiveDB = openBIS.getAfsServerFacade().list(sample.getPermId().getPermId(), "test-folder/test-file-2", true);
        assertFilesEqual(fileRecursiveFS, fileRecursiveDB);

        // DB : list "/test-folder/test-file-2" non-recursively
        List<File> fileNotRecursiveDB = openBIS.getAfsServerFacade().list(sample.getPermId().getPermId(), "/test-folder/test-file-2", false);
        assertFilesEqual(fileNotRecursiveFS, fileNotRecursiveDB);

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

    public void assertFilesEqual(List<File> files1, List<File> files2) throws Exception
    {
        assertEquals(files1.size(), files2.size());

        files1.sort(Comparator.comparing(File::getPath));
        files2.sort(Comparator.comparing(File::getPath));

        for (int i = 0; i < files1.size(); i++)
        {
            File file1 = files1.get(i);
            File file2 = files2.get(i);
            assertEquals(file1.getOwner(), file2.getOwner());
            assertEquals(file1.getPath(), file2.getPath());
            assertEquals(file1.getName(), file2.getName());
            assertEquals(file1.getDirectory(), file2.getDirectory());
            assertEquals(file1.getSize(), file2.getSize());
            // File system and database dates will differ in precision (we check they are different just to make sure we always
            // hit both the file system and the path info database, but the rest of the dates should be the same). If they are
            // exactly the same it means we didn't hit the path info database.
            assertNotEquals(file1.getLastModifiedTime(), file2.getLastModifiedTime());
            assertEquals(file1.getLastModifiedTime().withNano(0), file2.getLastModifiedTime().withNano(0));
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

}
