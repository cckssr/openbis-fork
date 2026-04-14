/*
 *  Copyright ETH 2023 Zürich, Scientific IT Services
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package ch.ethz.sis.afsserver.client;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ch.ethz.sis.afs.manager.TransactionConnection;
import ch.ethz.sis.afs.manager.operation.DeleteOperationExecutor;
import ch.ethz.sis.afsapi.api.ClientAPI;
import ch.ethz.sis.afsapi.dto.Chunk;
import ch.ethz.sis.afsapi.dto.ExceptionReason;
import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afsapi.dto.FreeSpace;
import ch.ethz.sis.afsapi.exception.ThrowableReason;
import ch.ethz.sis.afsclient.client.AfsClient;
import ch.ethz.sis.afsclient.client.AfsClientUploadHelper;
import ch.ethz.sis.afsclient.client.TemporaryPathUtil;
import ch.ethz.sis.afsserver.server.Server;
import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameter;
import ch.ethz.sis.shared.io.IOUtils;
import ch.ethz.sis.shared.startup.Configuration;
import lombok.NonNull;

public abstract class BaseApiClientTest
{
    protected static Server<TransactionConnection, ?> afsServer;

    protected static AfsClient afsClient;

    protected static int httpServerPort;

    protected static String httpServerPath;

    protected static String storageRoot;

    protected static String storageUuid;

    protected static final String FILE_A = "A.txt";

    protected static final String FILE_A_NAME = FILE_A;

    protected static final byte[] DATA = "ABCD".getBytes();

    protected static final byte[] DATA_2 = "ABCDE".getBytes();

    protected static final byte[] DATA_3 = "ABCDEF".getBytes();

    protected static final String FILE_B_NAME = "B.txt";

    protected static final String FILE_B = FILE_B_NAME;

    protected static final String FILE_C_NAME = "C.txt";

    protected static final String FILE_C = FILE_C_NAME;

    protected static final String FILE_BINARY_FOLDER_NAME = "test-folder";

    protected static final String FILE_BINARY_FOLDER = FILE_BINARY_FOLDER_NAME;

    protected static final String FILE_BINARY_SUBFOLDER_NAME = "test-subfolder";

    protected static final String FILE_BINARY_SUBFOLDER = FILE_BINARY_FOLDER_NAME + "/" + FILE_BINARY_SUBFOLDER_NAME;

    protected static final String FILE_BINARY_NAME = "test.png";

    protected static final String FILE_BINARY = FILE_BINARY_SUBFOLDER + "/" + FILE_BINARY_NAME;

    public static final String TEST_RESOURCE_DIRECTORY = "ch/ethz/sis/afsserver/";

    public static final String DOWNLOAD_TEST_RESOURCE_DIRECTORY_NAME = "downloadtest";

    public static final String DOWNLOAD_TEST_RESOURCE_DIRECTORY = TEST_RESOURCE_DIRECTORY + "/" + DOWNLOAD_TEST_RESOURCE_DIRECTORY_NAME;

    public static final String UPLOAD_TEST_RESOURCE_DIRECTORY_NAME = "uploadtest";

    public static final String UPLOAD_TEST_RESOURCE_DIRECTORY = TEST_RESOURCE_DIRECTORY + "/" + UPLOAD_TEST_RESOURCE_DIRECTORY_NAME;

    public static final String TRASH_FOLDER_NAME = ".afs.trash";

    public static final String SNAPSHOTS_FOLDER_NAME = ".afs.snapshots";

    protected static String owner = UUID.randomUUID().toString();

    protected int binarySize = -1;

    protected byte[] binaryData = null;

    protected String testDataRoot;

    @AfterClass
    public static void classTearDown() throws Exception
    {
        afsServer.shutdown(true);
    }

    @Before
    public void setUp() throws Exception
    {
        testDataRoot = IOUtils.getPath(storageRoot, getTestDataFolder(owner));

        final URL resource = getClass().getClassLoader().getResource("ch/ethz/sis/afsserver/client/test.png");
        final java.io.File file = new java.io.File(resource.toURI());
        try (final FileInputStream fis = new FileInputStream(file))
        {
            binaryData = fis.readAllBytes();
        }
        binarySize = (int) file.length();

        createTestDataFile(owner, FILE_A, DATA);
        createTestDataFile(owner, FILE_BINARY, binaryData);

        String testResourceDirectory = TEST_RESOURCE_DIRECTORY;
        Path resourceDirectoryPath = Path.of(getClass().getClassLoader().getResource(testResourceDirectory).getPath());
        IOUtils.createDirectories(resourceDirectoryPath.toAbsolutePath() + "/" + DOWNLOAD_TEST_RESOURCE_DIRECTORY_NAME);
        IOUtils.createDirectories(resourceDirectoryPath.toAbsolutePath() + "/" + UPLOAD_TEST_RESOURCE_DIRECTORY_NAME);

        afsClient = new AfsClient(
                new URI("http", null, "localhost", httpServerPort, httpServerPath, null, null));
    }

    protected abstract String getTestDataFolder(String owner);

    public void createTestDataFile(String owner, String source, byte[] data) throws Exception
    {
        String testDataRoot = IOUtils.getPath(storageRoot, getTestDataFolder(owner));
        String testDataFile = IOUtils.getPath(testDataRoot, source);
        IOUtils.createDirectories(new java.io.File(testDataFile).getParent());
        IOUtils.createFile(testDataFile);
        IOUtils.write(testDataFile, 0, data);
    }

    @After
    public void deleteTestData() throws IOException
    {
        IOUtils.delete(storageRoot);
    }

    @Test
    public void truncate_file() throws Exception
    {
        login();

        byte[] dataBefore = IOUtils.readFully(IOUtils.getPath(testDataRoot, FILE_A));
        assertArrayEquals(DATA, dataBefore);

        afsClient.truncate(owner, FILE_A, (long) (DATA.length / 2));

        byte[] dataAfterFirstTruncate = IOUtils.readFully(IOUtils.getPath(testDataRoot, FILE_A));
        assertArrayEquals(Arrays.copyOf(DATA, (DATA.length / 2)), dataAfterFirstTruncate);

        afsClient.truncate(owner, FILE_A, 0L);

        byte[] dataAfterSecondTruncate = IOUtils.readFully(IOUtils.getPath(testDataRoot, FILE_A));
        assertArrayEquals(new byte[] {}, dataAfterSecondTruncate);
    }

    @Test
    public void truncate_folder() throws Exception
    {
        login();

        try
        {
            afsClient.truncate(owner, FILE_BINARY_FOLDER, 0L);
            fail();
        } catch (Exception e)
        {
            assertTrue(e.getMessage(),
                    e.getMessage().contains(
                            "Path can't be operated by: Truncate - " + testDataRoot + "/" + FILE_BINARY_FOLDER
                                    + " is not a regular file"));

        }
    }

    @Test
    public void truncate_withNegativeSizeParameterFails() throws Exception
    {
        login();

        try
        {
            afsClient.truncate(owner, FILE_A, -1L);
            fail();
        } catch (Exception e)
        {
            assertTrue(e.getMessage(), e.getMessage().contains("Parameter of operation Truncate is invalid. Size cannot be < 0"));
        }
    }

    @Test
    public void truncate_withSizeParameterGreaterThanFileSizeDoesNothing() throws Exception
    {
        login();

        byte[] dataBefore = IOUtils.readFully(IOUtils.getPath(testDataRoot, FILE_A));
        assertArrayEquals(DATA, dataBefore);

        afsClient.truncate(owner, FILE_A, (long) DATA.length + 1);

        byte[] dataAfter = IOUtils.readFully(IOUtils.getPath(testDataRoot, FILE_A));
        assertArrayEquals(DATA, dataAfter);
    }

    @Test
    public void truncate_inTrashDirectoryIsNotAllowed() throws Exception
    {
        login();

        try
        {
            afsClient.truncate(owner, TRASH_FOLDER_NAME + "/" + FILE_A, 0L);
            fail();
        } catch (Exception e)
        {
            assertTrue(e.getMessage(),
                    e.getMessage().contains(
                            "Path can't be operated by: Truncate - " + testDataRoot + "/" + TRASH_FOLDER_NAME + "/" + FILE_A
                                    + " is in trash directory"));
        }
    }

    @Test
    public void truncate_inSnapshotsDirectoryIsNotAllowed() throws Exception
    {
        login();

        try
        {
            afsClient.truncate(owner, SNAPSHOTS_FOLDER_NAME + "/" + FILE_A, 0L);
            fail();
        } catch (Exception e)
        {
            assertTrue(e.getMessage(),
                    e.getMessage().contains("Path can't be operated by: Truncate - " + testDataRoot + "/" + SNAPSHOTS_FOLDER_NAME + "/" + FILE_A
                            + " is in snapshots directory"));
        }
    }

    @Test
    public void snapshot_file() throws Exception
    {
        login();

        File[] beforeSnapshot = listFilesFromAFS(afsClient, owner, "/");
        assertEquals("""
                /A.txt, FILE, 4
                /test-folder, FOLDER, null
                /test-folder/test-subfolder, FOLDER, null
                /test-folder/test-subfolder/test.png, FILE, 19951
                """, printFiles(beforeSnapshot));

        Boolean snapshot = afsClient.snapshot(owner, FILE_A);
        assertTrue(snapshot);

        File[] afterSnapshot = listFilesFromAFS(afsClient, owner, "/");
        assertEquals("""
                /.afs.snapshots, FOLDER, null
                /.afs.snapshots/A.txt, FOLDER, null
                /.afs.snapshots/A.txt/<SNAPSHOT>, FILE, 4
                /A.txt, FILE, 4
                /test-folder, FOLDER, null
                /test-folder/test-subfolder, FOLDER, null
                /test-folder/test-subfolder/test.png, FILE, 19951
                """, replaceSnapshots(printFiles(afterSnapshot)));

        afsClient.write(owner, FILE_A, 0L, DATA_2);

        File[] afterUpdate = listFilesFromAFS(afsClient, owner, "/");
        assertEquals("""
                /.afs.snapshots, FOLDER, null
                /.afs.snapshots/A.txt, FOLDER, null
                /.afs.snapshots/A.txt/<SNAPSHOT>, FILE, 4
                /A.txt, FILE, 5
                /test-folder, FOLDER, null
                /test-folder/test-subfolder, FOLDER, null
                /test-folder/test-subfolder/test.png, FILE, 19951
                """, replaceSnapshots(printFiles(afterUpdate)));

        Boolean snapshot2 = afsClient.snapshot(owner, FILE_A);
        assertTrue(snapshot2);

        File[] afterSnapshot2 = listFilesFromAFS(afsClient, owner, "/");
        assertEquals("""
                /.afs.snapshots, FOLDER, null
                /.afs.snapshots/A.txt, FOLDER, null
                /.afs.snapshots/A.txt/<SNAPSHOT>, FILE, 4
                /.afs.snapshots/A.txt/<SNAPSHOT>, FILE, 5
                /A.txt, FILE, 5
                /test-folder, FOLDER, null
                /test-folder/test-subfolder, FOLDER, null
                /test-folder/test-subfolder/test.png, FILE, 19951
                """, replaceSnapshots(printFiles(afterSnapshot2)));
    }

    @Test
    public void snapshot_folder() throws Exception
    {
        login();

        try
        {
            afsClient.snapshot(owner, FILE_BINARY_FOLDER);
            fail();
        } catch (Exception e)
        {
            assertTrue(e.getMessage(),
                    e.getMessage()
                            .contains("Path can't be operated by: Snapshot - " + testDataRoot + "/" + FILE_BINARY_FOLDER + " is not a regular file"));
        }
    }

    @Test
    public void snapshot_inTrashDirectoryIsNotAllowed() throws Exception
    {
        login();

        try
        {
            afsClient.snapshot(owner, TRASH_FOLDER_NAME + "/" + FILE_A);
            fail();
        } catch (Exception e)
        {
            assertTrue(e.getMessage(),
                    e.getMessage().contains(
                            "Path can't be operated by: Snapshot - " + testDataRoot + "/" + TRASH_FOLDER_NAME + "/" + FILE_A
                                    + " is in trash directory"));
        }
    }

    @Test
    public void snapshot_inSnapshotsDirectoryIsNotAllowed() throws Exception
    {
        login();

        try
        {
            afsClient.snapshot(owner, SNAPSHOTS_FOLDER_NAME + "/" + FILE_A);
            fail();
        } catch (Exception e)
        {
            assertTrue(e.getMessage(),
                    e.getMessage().contains("Path can't be operated by: Snapshot - " + testDataRoot + "/" + SNAPSHOTS_FOLDER_NAME + "/" + FILE_A
                            + " is in snapshots directory"));
        }
    }

    @Test
    public void delete_fileToTrash() throws Exception
    {
        login();

        // snapshot "A.txt" file, update it and snapshot again
        afsClient.snapshot(owner, FILE_A);
        afsClient.write(owner, FILE_A, 0L, DATA_2);
        afsClient.snapshot(owner, FILE_A);

        // hash "A.txt" file
        afsClient.hash(owner, FILE_A);

        // snapshot "test.png" file
        afsClient.snapshot(owner, FILE_BINARY);

        // preview "test.png" file
        afsClient.preview(owner, FILE_BINARY);

        File[] beforeFirstDeletionAFS = listFilesFromAFS(afsClient, owner, "/");
        assertEquals("""
                /.afs.snapshots, FOLDER, null
                /.afs.snapshots/A.txt, FOLDER, null
                /.afs.snapshots/A.txt/<SNAPSHOT>, FILE, 4
                /.afs.snapshots/A.txt/<SNAPSHOT>, FILE, 5
                /A.txt, FILE, 5
                /test-folder, FOLDER, null
                /test-folder/test-subfolder, FOLDER, null
                /test-folder/test-subfolder/.afs.snapshots, FOLDER, null
                /test-folder/test-subfolder/.afs.snapshots/test.png, FOLDER, null
                /test-folder/test-subfolder/.afs.snapshots/test.png/<SNAPSHOT>, FILE, 19951
                /test-folder/test-subfolder/test.png, FILE, 19951
                """, replaceSnapshots(printFiles(beforeFirstDeletionAFS)));

        ch.ethz.sis.afs.api.dto.File[] beforeFirstDeletionFS = listFilesFromFS(testDataRoot);
        assertEquals("""
                /.afs, FOLDER, null
                /.afs.snapshots, FOLDER, null
                /.afs.snapshots/A.txt, FOLDER, null
                /.afs.snapshots/A.txt/<SNAPSHOT>, FILE, 4
                /.afs.snapshots/A.txt/<SNAPSHOT>, FILE, 5
                /.afs/A.txt-hash.md5, FILE, 32
                /A.txt, FILE, 5
                /test-folder, FOLDER, null
                /test-folder/test-subfolder, FOLDER, null
                /test-folder/test-subfolder/.afs, FOLDER, null
                /test-folder/test-subfolder/.afs.snapshots, FOLDER, null
                /test-folder/test-subfolder/.afs.snapshots/test.png, FOLDER, null
                /test-folder/test-subfolder/.afs.snapshots/test.png/<SNAPSHOT>, FILE, 19951
                /test-folder/test-subfolder/.afs/test.png-preview.jpg, FILE, 8607
                /test-folder/test-subfolder/test.png, FILE, 19951
                """, replaceSnapshots(printFiles(beforeFirstDeletionFS)));

        // trash "A.txt" file
        Boolean firstDeletion = afsClient.delete(owner, FILE_A, true);
        assertTrue(firstDeletion);

        File[] afterFirstDeletionAFS = listFilesFromAFS(afsClient, owner, "/");
        assertEquals("""
                /.afs.trash, FOLDER, null
                /.afs.trash/.afs.snapshots, FOLDER, null
                /.afs.trash/.afs.snapshots/A.txt, FOLDER, null
                /.afs.trash/.afs.snapshots/A.txt/<SNAPSHOT>, FILE, 4
                /.afs.trash/.afs.snapshots/A.txt/<SNAPSHOT>, FILE, 5
                /.afs.trash/A.txt, FILE, 5
                /test-folder, FOLDER, null
                /test-folder/test-subfolder, FOLDER, null
                /test-folder/test-subfolder/.afs.snapshots, FOLDER, null
                /test-folder/test-subfolder/.afs.snapshots/test.png, FOLDER, null
                /test-folder/test-subfolder/.afs.snapshots/test.png/<SNAPSHOT>, FILE, 19951
                /test-folder/test-subfolder/test.png, FILE, 19951
                """, replaceSnapshots(printFiles(afterFirstDeletionAFS)));

        ch.ethz.sis.afs.api.dto.File[] afterFirstDeletionFS = listFilesFromFS(testDataRoot);
        assertEquals("""
                /.afs, FOLDER, null
                /.afs.trash, FOLDER, null
                /.afs.trash/.afs, FOLDER, null
                /.afs.trash/.afs.snapshots, FOLDER, null
                /.afs.trash/.afs.snapshots/A.txt, FOLDER, null
                /.afs.trash/.afs.snapshots/A.txt/<SNAPSHOT>, FILE, 4
                /.afs.trash/.afs.snapshots/A.txt/<SNAPSHOT>, FILE, 5
                /.afs.trash/.afs/A.txt-hash.md5, FILE, 32
                /.afs.trash/A.txt, FILE, 5
                /test-folder, FOLDER, null
                /test-folder/test-subfolder, FOLDER, null
                /test-folder/test-subfolder/.afs, FOLDER, null
                /test-folder/test-subfolder/.afs.snapshots, FOLDER, null
                /test-folder/test-subfolder/.afs.snapshots/test.png, FOLDER, null
                /test-folder/test-subfolder/.afs.snapshots/test.png/<SNAPSHOT>, FILE, 19951
                /test-folder/test-subfolder/.afs/test.png-preview.jpg, FILE, 8607
                /test-folder/test-subfolder/test.png, FILE, 19951
                """, replaceSnapshots(printFiles(afterFirstDeletionFS)));

        // create a new "A.txt" file with different data
        createTestDataFile(owner, FILE_A, DATA_3);

        FileTime firstDeletedFileALastModifiedTime = Files.getLastModifiedTime(Path.of(testDataRoot, TRASH_FOLDER_NAME, FILE_A_NAME));
        String firstDeletedFileALastModifiedTimeFormatted =
                firstDeletedFileALastModifiedTime.toInstant().atZone(ZoneId.systemDefault()).format(DeleteOperationExecutor.TIMESTAMP_SUFFIX_FORMAT);

        // trash the new "A.txt" file
        Boolean secondDeletion = afsClient.delete(owner, FILE_A, true);
        assertTrue(secondDeletion);

        // check the new "A.txt" file is in the trash and the previously trashed "A.txt" file and its hash got renamed
        File[] afterSecondDeletionAFS = listFilesFromAFS(afsClient, owner, "/");
        assertEquals("""
                        /.afs.trash, FOLDER, null
                        /.afs.trash/.afs.snapshots, FOLDER, null
                        /.afs.trash/.afs.snapshots/A.txt#%s, FOLDER, null
                        /.afs.trash/.afs.snapshots/A.txt#%s/<SNAPSHOT>, FILE, 4
                        /.afs.trash/.afs.snapshots/A.txt#%s/<SNAPSHOT>, FILE, 5
                        /.afs.trash/A.txt, FILE, 6
                        /.afs.trash/A.txt#%s, FILE, 5
                        /test-folder, FOLDER, null
                        /test-folder/test-subfolder, FOLDER, null
                        /test-folder/test-subfolder/.afs.snapshots, FOLDER, null
                        /test-folder/test-subfolder/.afs.snapshots/test.png, FOLDER, null
                        /test-folder/test-subfolder/.afs.snapshots/test.png/<SNAPSHOT>, FILE, 19951
                        /test-folder/test-subfolder/test.png, FILE, 19951
                        """.formatted(firstDeletedFileALastModifiedTimeFormatted, firstDeletedFileALastModifiedTimeFormatted,
                        firstDeletedFileALastModifiedTimeFormatted, firstDeletedFileALastModifiedTimeFormatted),
                replaceSnapshots(printFiles(afterSecondDeletionAFS)));

        ch.ethz.sis.afs.api.dto.File[] afterSecondDeletionFS = listFilesFromFS(testDataRoot);
        assertEquals("""
                        /.afs, FOLDER, null
                        /.afs.trash, FOLDER, null
                        /.afs.trash/.afs, FOLDER, null
                        /.afs.trash/.afs.snapshots, FOLDER, null
                        /.afs.trash/.afs.snapshots/A.txt#%s, FOLDER, null
                        /.afs.trash/.afs.snapshots/A.txt#%s/<SNAPSHOT>, FILE, 4
                        /.afs.trash/.afs.snapshots/A.txt#%s/<SNAPSHOT>, FILE, 5
                        /.afs.trash/.afs/A.txt#%s-hash.md5, FILE, 32
                        /.afs.trash/A.txt, FILE, 6
                        /.afs.trash/A.txt#%s, FILE, 5
                        /test-folder, FOLDER, null
                        /test-folder/test-subfolder, FOLDER, null
                        /test-folder/test-subfolder/.afs, FOLDER, null
                        /test-folder/test-subfolder/.afs.snapshots, FOLDER, null
                        /test-folder/test-subfolder/.afs.snapshots/test.png, FOLDER, null
                        /test-folder/test-subfolder/.afs.snapshots/test.png/<SNAPSHOT>, FILE, 19951
                        /test-folder/test-subfolder/.afs/test.png-preview.jpg, FILE, 8607
                        /test-folder/test-subfolder/test.png, FILE, 19951
                        """.formatted(firstDeletedFileALastModifiedTimeFormatted, firstDeletedFileALastModifiedTimeFormatted,
                        firstDeletedFileALastModifiedTimeFormatted, firstDeletedFileALastModifiedTimeFormatted, firstDeletedFileALastModifiedTimeFormatted),
                replaceSnapshots(printFiles(afterSecondDeletionFS)));

        // delete the first deleted "A.txt" file from trash (i.e. delete permanently)
        Boolean permanentDeletion =
                afsClient.delete(owner, TRASH_FOLDER_NAME + "/" + FILE_A + "#" + firstDeletedFileALastModifiedTimeFormatted, true);
        assertTrue(permanentDeletion);

        File[] afterPermanentDeletionAFS = listFilesFromAFS(afsClient, owner, "/");
        assertEquals("""
                        /.afs.trash, FOLDER, null
                        /.afs.trash/A.txt, FILE, 6
                        /test-folder, FOLDER, null
                        /test-folder/test-subfolder, FOLDER, null
                        /test-folder/test-subfolder/.afs.snapshots, FOLDER, null
                        /test-folder/test-subfolder/.afs.snapshots/test.png, FOLDER, null
                        /test-folder/test-subfolder/.afs.snapshots/test.png/<SNAPSHOT>, FILE, 19951
                        /test-folder/test-subfolder/test.png, FILE, 19951
                        """,
                replaceSnapshots(printFiles(afterPermanentDeletionAFS)));

        ch.ethz.sis.afs.api.dto.File[] afterPermanentDeletionFS = listFilesFromFS(testDataRoot);
        assertEquals("""
                        /.afs, FOLDER, null
                        /.afs.trash, FOLDER, null
                        /.afs.trash/.afs, FOLDER, null
                        /.afs.trash/A.txt, FILE, 6
                        /test-folder, FOLDER, null
                        /test-folder/test-subfolder, FOLDER, null
                        /test-folder/test-subfolder/.afs, FOLDER, null
                        /test-folder/test-subfolder/.afs.snapshots, FOLDER, null
                        /test-folder/test-subfolder/.afs.snapshots/test.png, FOLDER, null
                        /test-folder/test-subfolder/.afs.snapshots/test.png/<SNAPSHOT>, FILE, 19951
                        /test-folder/test-subfolder/.afs/test.png-preview.jpg, FILE, 8607
                        /test-folder/test-subfolder/test.png, FILE, 19951
                        """,
                replaceSnapshots(printFiles(afterPermanentDeletionFS)));

        // delete the whole trash
        Boolean wholeTrashDeletion = afsClient.delete(owner, TRASH_FOLDER_NAME, true);
        assertTrue(wholeTrashDeletion);

        File[] afterWholeTrashDeletionAFS = listFilesFromAFS(afsClient, owner, "/");
        assertEquals("""
                /test-folder, FOLDER, null
                /test-folder/test-subfolder, FOLDER, null
                /test-folder/test-subfolder/.afs.snapshots, FOLDER, null
                /test-folder/test-subfolder/.afs.snapshots/test.png, FOLDER, null
                /test-folder/test-subfolder/.afs.snapshots/test.png/<SNAPSHOT>, FILE, 19951
                /test-folder/test-subfolder/test.png, FILE, 19951
                """, replaceSnapshots(printFiles(afterWholeTrashDeletionAFS)));

        ch.ethz.sis.afs.api.dto.File[] afterWholeTrashDeletionFS = listFilesFromFS(testDataRoot);
        assertEquals("""
                /.afs, FOLDER, null
                /test-folder, FOLDER, null
                /test-folder/test-subfolder, FOLDER, null
                /test-folder/test-subfolder/.afs, FOLDER, null
                /test-folder/test-subfolder/.afs.snapshots, FOLDER, null
                /test-folder/test-subfolder/.afs.snapshots/test.png, FOLDER, null
                /test-folder/test-subfolder/.afs.snapshots/test.png/<SNAPSHOT>, FILE, 19951
                /test-folder/test-subfolder/.afs/test.png-preview.jpg, FILE, 8607
                /test-folder/test-subfolder/test.png, FILE, 19951
                """, replaceSnapshots(printFiles(afterWholeTrashDeletionFS)));
    }

    @Test
    public void delete_fileToTrashThenFolderWithTheSameName() throws Exception
    {
        login();

        String testOwner = UUID.randomUUID().toString();

        // create "A.txt" file
        createTestDataFile(testOwner, FILE_A, DATA);

        File[] beforeFileDeletionAFS = listFilesFromAFS(afsClient, testOwner, "/");
        assertEquals("""
                /A.txt, FILE, 4
                """, printFiles(beforeFileDeletionAFS));

        // trash "A.txt" file
        Boolean fileDeletion = afsClient.delete(testOwner, FILE_A, true);
        assertTrue(fileDeletion);

        File[] afterFileDeletionAFS = listFilesFromAFS(afsClient, testOwner, "/");
        assertEquals("""
                /.afs.trash, FOLDER, null
                /.afs.trash/A.txt, FILE, 4
                """, printFiles(afterFileDeletionAFS));

        // create "A.txt" folder with "A.txt" file inside
        createTestDataFile(testOwner, FILE_A + "/" + FILE_A_NAME, DATA_2);

        File[] beforeFolderDeletionAFS = listFilesFromAFS(afsClient, testOwner, "/");
        assertEquals("""
                /.afs.trash, FOLDER, null
                /.afs.trash/A.txt, FILE, 4
                /A.txt, FOLDER, null
                /A.txt/A.txt, FILE, 5
                """, printFiles(beforeFolderDeletionAFS));

        FileTime deletedFileALastModifiedTime =
                Files.getLastModifiedTime(Path.of(storageRoot, getTestDataFolder(testOwner), TRASH_FOLDER_NAME, FILE_A));
        String deletedFileALastModifiedTimeFormatted =
                deletedFileALastModifiedTime.toInstant().atZone(ZoneId.systemDefault()).format(DeleteOperationExecutor.TIMESTAMP_SUFFIX_FORMAT);

        // trash "A.txt" folder
        Boolean folderDeletion = afsClient.delete(testOwner, FILE_A, true);
        assertTrue(folderDeletion);

        // check "A.txt" folder is in the trash and the previously trashed "A.txt" file gets renamed
        File[] afterFolderDeletionAFS = listFilesFromAFS(afsClient, testOwner, "/");
        assertEquals("""
                /.afs.trash, FOLDER, null
                /.afs.trash/A.txt, FOLDER, null
                /.afs.trash/A.txt#%s, FILE, 4
                /.afs.trash/A.txt/A.txt, FILE, 5
                """.formatted(deletedFileALastModifiedTimeFormatted), printFiles(afterFolderDeletionAFS));
    }

    @Test
    public void delete_folderToTrash() throws Exception
    {
        login();

        // snapshot "test.png" file
        afsClient.snapshot(owner, FILE_BINARY);

        File[] beforeFirstDeletionAFS = listFilesFromAFS(afsClient, owner, "/");
        assertEquals("""
                /A.txt, FILE, 4
                /test-folder, FOLDER, null
                /test-folder/test-subfolder, FOLDER, null
                /test-folder/test-subfolder/.afs.snapshots, FOLDER, null
                /test-folder/test-subfolder/.afs.snapshots/test.png, FOLDER, null
                /test-folder/test-subfolder/.afs.snapshots/test.png/<SNAPSHOT>, FILE, 19951
                /test-folder/test-subfolder/test.png, FILE, 19951
                """, replaceSnapshots(printFiles(beforeFirstDeletionAFS)));

        // trash "test-subfolder" folder with "test.png" file inside
        Boolean firstDeletion = afsClient.delete(owner, FILE_BINARY_SUBFOLDER, true);
        assertTrue(firstDeletion);

        File[] afterFirstDeletionAFS = listFilesFromAFS(afsClient, owner, "/");
        assertEquals("""
                /.afs.trash, FOLDER, null
                /.afs.trash/test-folder, FOLDER, null
                /.afs.trash/test-folder/test-subfolder, FOLDER, null
                /.afs.trash/test-folder/test-subfolder/.afs.snapshots, FOLDER, null
                /.afs.trash/test-folder/test-subfolder/.afs.snapshots/test.png, FOLDER, null
                /.afs.trash/test-folder/test-subfolder/.afs.snapshots/test.png/<SNAPSHOT>, FILE, 19951
                /.afs.trash/test-folder/test-subfolder/test.png, FILE, 19951
                /A.txt, FILE, 4
                /test-folder, FOLDER, null
                """, replaceSnapshots(printFiles(afterFirstDeletionAFS)));

        // create a new "test-subfolder" folder with a new file "test.png" inside
        createTestDataFile(owner, FILE_BINARY, DATA_2);

        // snapshot new "test.png" file
        afsClient.snapshot(owner, FILE_BINARY);

        File[] beforeSecondDeletionAFS = listFilesFromAFS(afsClient, owner, "/");
        assertEquals("""
                /.afs.trash, FOLDER, null
                /.afs.trash/test-folder, FOLDER, null
                /.afs.trash/test-folder/test-subfolder, FOLDER, null
                /.afs.trash/test-folder/test-subfolder/.afs.snapshots, FOLDER, null
                /.afs.trash/test-folder/test-subfolder/.afs.snapshots/test.png, FOLDER, null
                /.afs.trash/test-folder/test-subfolder/.afs.snapshots/test.png/<SNAPSHOT>, FILE, 19951
                /.afs.trash/test-folder/test-subfolder/test.png, FILE, 19951
                /A.txt, FILE, 4
                /test-folder, FOLDER, null
                /test-folder/test-subfolder, FOLDER, null
                /test-folder/test-subfolder/.afs.snapshots, FOLDER, null
                /test-folder/test-subfolder/.afs.snapshots/test.png, FOLDER, null
                /test-folder/test-subfolder/.afs.snapshots/test.png/<SNAPSHOT>, FILE, 5
                /test-folder/test-subfolder/test.png, FILE, 5
                """, replaceSnapshots(printFiles(beforeSecondDeletionAFS)));

        FileTime firstDeletedBinaryFileLastModifiedTime = Files.getLastModifiedTime(Path.of(testDataRoot, TRASH_FOLDER_NAME, FILE_BINARY));
        String firstDeletedFileALastModifiedTimeFormatted =
                firstDeletedBinaryFileLastModifiedTime.toInstant().atZone(ZoneId.systemDefault())
                        .format(DeleteOperationExecutor.TIMESTAMP_SUFFIX_FORMAT);

        // trash the new "test-subfolder" folder (the deleted folders are merged and the previously deleted "test.png" is renamed)
        Boolean secondDeletion = afsClient.delete(owner, FILE_BINARY_SUBFOLDER, true);
        assertTrue(secondDeletion);

        File[] afterSecondDeletionAFS = listFilesFromAFS(afsClient, owner, "/");
        assertEquals("""
                        /.afs.trash, FOLDER, null
                        /.afs.trash/test-folder, FOLDER, null
                        /.afs.trash/test-folder/test-subfolder, FOLDER, null
                        /.afs.trash/test-folder/test-subfolder/.afs.snapshots, FOLDER, null
                        /.afs.trash/test-folder/test-subfolder/.afs.snapshots/test.png, FOLDER, null
                        /.afs.trash/test-folder/test-subfolder/.afs.snapshots/test.png#%s, FOLDER, null
                        /.afs.trash/test-folder/test-subfolder/.afs.snapshots/test.png#%s/<SNAPSHOT>, FILE, 19951
                        /.afs.trash/test-folder/test-subfolder/.afs.snapshots/test.png/<SNAPSHOT>, FILE, 5
                        /.afs.trash/test-folder/test-subfolder/test.png, FILE, 5
                        /.afs.trash/test-folder/test-subfolder/test.png#%s, FILE, 19951
                        /A.txt, FILE, 4
                        /test-folder, FOLDER, null
                        """.formatted(firstDeletedFileALastModifiedTimeFormatted, firstDeletedFileALastModifiedTimeFormatted,
                        firstDeletedFileALastModifiedTimeFormatted),
                replaceSnapshots(printFiles(afterSecondDeletionAFS)));

        // delete "test-subfolder" folder from trash (i.e. delete permanently)
        Boolean permanentDeletion = afsClient.delete(owner, TRASH_FOLDER_NAME + "/" + FILE_BINARY_SUBFOLDER, true);
        assertTrue(permanentDeletion);

        File[] afterPermanentDeletionAFS = listFilesFromAFS(afsClient, owner, "/");
        assertEquals("""
                /.afs.trash, FOLDER, null
                /.afs.trash/test-folder, FOLDER, null
                /A.txt, FILE, 4
                /test-folder, FOLDER, null
                """, printFiles(afterPermanentDeletionAFS));

        // delete the whole trash
        Boolean wholeTrashDeletion = afsClient.delete(owner, TRASH_FOLDER_NAME, true);
        assertTrue(wholeTrashDeletion);

        File[] afterWholeTrashDeletionAFS = listFilesFromAFS(afsClient, owner, "/");
        assertEquals("""
                /A.txt, FILE, 4
                /test-folder, FOLDER, null
                """, printFiles(afterWholeTrashDeletionAFS));
    }

    @Test
    public void delete_folderToTrashThenFileWithTheSameName() throws Exception
    {
        login();

        String testOwner = UUID.randomUUID().toString();

        // create "A.txt" folder with "A.txt" file inside
        createTestDataFile(testOwner, FILE_A + "/" + FILE_A, DATA);

        File[] beforeFolderDeletionAFS = listFilesFromAFS(afsClient, testOwner, "/");
        assertEquals("""
                /A.txt, FOLDER, null
                /A.txt/A.txt, FILE, 4
                """, printFiles(beforeFolderDeletionAFS));

        // trash "A.txt" folder
        Boolean folderDeletion = afsClient.delete(testOwner, FILE_A, true);
        assertTrue(folderDeletion);

        File[] afterFolderDeletionAFS = listFilesFromAFS(afsClient, testOwner, "/");
        assertEquals("""
                /.afs.trash, FOLDER, null
                /.afs.trash/A.txt, FOLDER, null
                /.afs.trash/A.txt/A.txt, FILE, 4
                """, printFiles(afterFolderDeletionAFS));

        // create "A.txt" file
        createTestDataFile(testOwner, FILE_A, DATA_2);

        File[] beforeFileDeletionAFS = listFilesFromAFS(afsClient, testOwner, "/");
        assertEquals("""
                /.afs.trash, FOLDER, null
                /.afs.trash/A.txt, FOLDER, null
                /.afs.trash/A.txt/A.txt, FILE, 4
                /A.txt, FILE, 5
                """, printFiles(beforeFileDeletionAFS));

        FileTime deletedFileALastModifiedTime =
                Files.getLastModifiedTime(Path.of(storageRoot, getTestDataFolder(testOwner), TRASH_FOLDER_NAME, FILE_A));
        String deletedFileALastModifiedTimeFormatted =
                deletedFileALastModifiedTime.toInstant().atZone(ZoneId.systemDefault()).format(DeleteOperationExecutor.TIMESTAMP_SUFFIX_FORMAT);

        // trash "A.txt" file
        Boolean fileDeletion = afsClient.delete(testOwner, FILE_A, true);
        assertTrue(fileDeletion);

        // check "A.txt" file is in the trash and the previously trashed "A.txt" folder gets renamed
        File[] afterFileDeletionAFS = listFilesFromAFS(afsClient, testOwner, "/");
        assertEquals("""
                /.afs.trash, FOLDER, null
                /.afs.trash/A.txt, FILE, 5
                /.afs.trash/A.txt#%s, FOLDER, null
                /.afs.trash/A.txt#%s/A.txt, FILE, 4
                """.formatted(deletedFileALastModifiedTimeFormatted, deletedFileALastModifiedTimeFormatted), printFiles(afterFileDeletionAFS));
    }

    @Test
    public void login_sessionTokenIsNotNull() throws Exception
    {
        final String token = login();
        assertNotNull(token);
    }

    @Test
    public void isSessionValid_throwsException() throws Exception
    {
        try
        {
            afsClient.isSessionValid();
            fail();
        } catch (IllegalStateException e)
        {
            assertThat(e.getMessage(), containsString("No session information detected!"));
        }
    }

    @Test
    public void isSessionValid_returnsTrue() throws Exception
    {
        login();

        final Boolean isValid = afsClient.isSessionValid();
        assertTrue(isValid);
    }

    @Test
    public void logout_withoutLogin_throwsException() throws Exception
    {
        try
        {
            afsClient.logout();
            fail();
        } catch (IllegalStateException e)
        {
            assertThat(e.getMessage(), containsString("No session information detected!"));
        }
    }

    @Test
    public void logout_withLogin_returnsTrue() throws Exception
    {
        login();

        final Boolean result = afsClient.logout();

        assertTrue(result);
    }

    @Test
    public void list_rootRecursive() throws Exception
    {
        login();

        File[] files = afsClient.list(owner, "", Boolean.TRUE);
        assertEquals(4, files.length);

        Arrays.sort(files, Comparator.comparing(File::getPath));
        assertFileEquals(files[0], owner, "/" + FILE_A, FILE_A_NAME, false, (long) DATA.length);
        assertFileEquals(files[1], owner, "/" + FILE_BINARY_FOLDER, FILE_BINARY_FOLDER_NAME, true, null);
        assertFileEquals(files[2], owner, "/" + FILE_BINARY_SUBFOLDER, FILE_BINARY_SUBFOLDER_NAME, true, null);
        assertFileEquals(files[3], owner, "/" + FILE_BINARY, FILE_BINARY_NAME, false, (long) binaryData.length);
    }

    @Test
    public void list_rootNonRecursive() throws Exception
    {
        login();

        File[] files = afsClient.list(owner, "", Boolean.FALSE);
        assertEquals(2, files.length);

        Arrays.sort(files, Comparator.comparing(File::getPath));
        assertFileEquals(files[0], owner, "/" + FILE_A, FILE_A_NAME, false, (long) DATA.length);
        assertFileEquals(files[1], owner, "/" + FILE_BINARY_FOLDER, FILE_BINARY_FOLDER_NAME, true, null);
    }

    @Test
    public void list_folderRecursive() throws Exception
    {
        login();

        File[] files = afsClient.list(owner, FILE_BINARY_FOLDER, Boolean.TRUE);
        assertEquals(2, files.length);

        Arrays.sort(files, Comparator.comparing(File::getPath));
        assertFileEquals(files[0], owner, "/" + FILE_BINARY_SUBFOLDER, FILE_BINARY_SUBFOLDER_NAME, true, null);
        assertFileEquals(files[1], owner, "/" + FILE_BINARY, FILE_BINARY_NAME, false, (long) binaryData.length);
    }

    @Test
    public void list_folderNonRecursive() throws Exception
    {
        login();

        File[] files = afsClient.list(owner, FILE_BINARY_FOLDER, Boolean.FALSE);
        assertEquals(1, files.length);

        assertFileEquals(files[0], owner, "/" + FILE_BINARY_SUBFOLDER, FILE_BINARY_SUBFOLDER_NAME, true, null);
    }

    @Test
    public void list_withLeadingSlash() throws Exception
    {
        login();

        File[] files = afsClient.list(owner, "/" + FILE_BINARY, Boolean.FALSE);
        assertEquals(1, files.length);
        assertFileEquals(files[0], owner, "/" + FILE_BINARY, FILE_BINARY_NAME, false, (long) binaryData.length);

    }

    @Test
    public void list_withoutLeadingSlash() throws Exception
    {
        login();

        File[] files = afsClient.list(owner, FILE_BINARY, Boolean.FALSE);
        assertEquals(1, files.length);
        assertFileEquals(files[0], owner, "/" + FILE_BINARY, FILE_BINARY_NAME, false, (long) binaryData.length);
    }

    @Test
    public void list_withRelativePath() throws Exception
    {
        login();

        try
        {
            afsClient.list(owner, "/../" + FILE_BINARY, Boolean.FALSE);
        } catch (Exception e)
        {
            ThrowableReason reason = (ThrowableReason) e.getCause();
            String message = ((ExceptionReason) reason.getReason()).getMessage();
            assertTrue(message.contains(
                    "Path given to: List - can't contain '/../'"));
        }
    }

    @Test
    public void free_returnsValue() throws Exception
    {
        login();

        final FreeSpace space = afsClient.free(owner, "");
        assertTrue(space.getFree() >= 0);
        assertTrue(space.getTotal() > 0);
        assertTrue(space.getFree() <= space.getTotal());
    }

    @Test
    public void read_getsDataFromTemporaryFile() throws Exception
    {
        login();

        byte[] bytes = afsClient.read(owner, FILE_A, 0L, DATA.length);
        assertArrayEquals(DATA, bytes);
    }

    @Test
    public void read_binaryFile() throws Exception
    {
        login();

        byte[] bytes = afsClient.read(owner, FILE_BINARY, 0L, binarySize);
        assertArrayEquals(binaryData, bytes);
    }

    @Test
    public void create_inTrashDirectoryIsNotAllowed() throws Exception
    {
        login();

        try
        {
            afsClient.create(owner, TRASH_FOLDER_NAME + "/" + FILE_A, false);
            fail();
        } catch (Exception e)
        {
            assertTrue(e.getMessage(),
                    e.getMessage().contains(
                            "Path can't be operated by: Create - " + testDataRoot + "/" + TRASH_FOLDER_NAME + "/" + FILE_A
                                    + " is in trash directory"));
        }
    }

    @Test
    public void create_inSnapshotsDirectoryIsNotAllowed() throws Exception
    {
        login();

        try
        {
            afsClient.create(owner, SNAPSHOTS_FOLDER_NAME + "/" + FILE_A, false);
            fail();
        } catch (Exception e)
        {
            assertTrue(e.getMessage(),
                    e.getMessage().contains("Path can't be operated by: Create - " + testDataRoot + "/" + SNAPSHOTS_FOLDER_NAME + "/" + FILE_A
                            + " is in snapshots directory"));
        }
    }

    @Test
    public void write_zeroOffset_createsFile() throws Exception
    {
        login();

        Boolean result = afsClient.write(owner, FILE_B, 0L, DATA);
        assertTrue(result);

        byte[] testDataFile = IOUtils.readFully(IOUtils.getPath(testDataRoot, FILE_B));
        assertArrayEquals(DATA, testDataFile);
    }

    @Test
    public void write_nonZeroOffset_createsFile() throws Exception
    {
        login();

        Long offset = 65L;
        Boolean result = afsClient.write(owner, FILE_B, offset, DATA);
        assertTrue(result);

        byte[] testDataFile = IOUtils.readFully(IOUtils.getPath(testDataRoot, FILE_A));
        assertArrayEquals(DATA, testDataFile);
    }

    @Test
    public void write_toTrashDirectoryIsNotAllowed() throws Exception
    {
        login();

        try
        {
            afsClient.write(owner, TRASH_FOLDER_NAME + "/" + FILE_A, 0L, DATA);
            fail();
        } catch (Exception e)
        {
            assertTrue(e.getMessage(),
                    e.getMessage().contains(
                            "Path can't be operated by: Write - " + testDataRoot + "/" + TRASH_FOLDER_NAME + "/" + FILE_A
                                    + " is in trash directory"));
        }
    }

    @Test
    public void write_toSnapshotsDirectoryIsNotAllowed() throws Exception
    {
        login();

        try
        {
            afsClient.write(owner, SNAPSHOTS_FOLDER_NAME + "/" + FILE_A, 0L, DATA);
            fail();
        } catch (Exception e)
        {
            assertTrue(e.getMessage(),
                    e.getMessage().contains("Path can't be operated by: Write - " + testDataRoot + "/" + SNAPSHOTS_FOLDER_NAME + "/" + FILE_A
                            + " is in snapshots directory"));
        }
    }

    @Test
    public void delete_fileIsGone() throws Exception
    {
        login();

        Boolean deleted = afsClient.delete(owner, FILE_A, false);
        assertTrue(deleted);

        List<ch.ethz.sis.afs.api.dto.File> list =
                IOUtils.list(testDataRoot, true).stream().filter(file -> !IOUtils.isAfsHiddenFile(file.getPath())).toList();
        assertEquals(3, list.size());
    }

    @Test
    public void copy_newFileIsCreated() throws Exception
    {
        login();

        Boolean result = afsClient.copy(owner, FILE_A, owner, FILE_B);
        assertTrue(result);

        byte[] testDataFile = IOUtils.readFully(IOUtils.getPath(testDataRoot, FILE_B));
        assertArrayEquals(DATA, testDataFile);
    }

    @Test
    public void copy_copiesContentOfSourceToTargetAndKeepsSnapshotsOfTargetFileIfExist() throws Exception
    {
        login();

        afsClient.snapshot(owner, FILE_A);

        afsClient.write(owner, FILE_B, 0L, DATA_2);
        afsClient.snapshot(owner, FILE_B);

        File[] filesBefore = listFilesFromAFS(afsClient, owner, "/");
        assertEquals("""
                /.afs.snapshots, FOLDER, null
                /.afs.snapshots/A.txt, FOLDER, null
                /.afs.snapshots/A.txt/<SNAPSHOT>, FILE, 4
                /.afs.snapshots/B.txt, FOLDER, null
                /.afs.snapshots/B.txt/<SNAPSHOT>, FILE, 5
                /A.txt, FILE, 4
                /B.txt, FILE, 5
                /test-folder, FOLDER, null
                /test-folder/test-subfolder, FOLDER, null
                /test-folder/test-subfolder/test.png, FILE, 19951
                """, replaceSnapshots(printFiles(filesBefore)));

        Boolean result = afsClient.copy(owner, FILE_A, owner, FILE_B);
        assertTrue(result);

        Boolean result2 = afsClient.copy(owner, FILE_A, owner, FILE_C);
        assertTrue(result2);

        File[] filesAfter = listFilesFromAFS(afsClient, owner, "/");
        assertEquals("""
                /.afs.snapshots, FOLDER, null
                /.afs.snapshots/A.txt, FOLDER, null
                /.afs.snapshots/A.txt/<SNAPSHOT>, FILE, 4
                /.afs.snapshots/B.txt, FOLDER, null
                /.afs.snapshots/B.txt/<SNAPSHOT>, FILE, 5
                /A.txt, FILE, 4
                /B.txt, FILE, 4
                /C.txt, FILE, 4
                /test-folder, FOLDER, null
                /test-folder/test-subfolder, FOLDER, null
                /test-folder/test-subfolder/test.png, FILE, 19951
                """, replaceSnapshots(printFiles(filesAfter)));

        byte[] testDataFile = IOUtils.readFully(IOUtils.getPath(testDataRoot, FILE_B));
        assertArrayEquals(DATA, testDataFile);
    }

    @Test
    public void copy_toTrashDirectoryIsNotAllowed() throws Exception
    {
        login();

        try
        {
            afsClient.copy(owner, FILE_A, owner, TRASH_FOLDER_NAME + "/" + FILE_A);
            fail();
        } catch (Exception e)
        {
            assertTrue(e.getMessage(),
                    e.getMessage().contains(
                            "Path can't be operated by: Copy - " + testDataRoot + "/" + TRASH_FOLDER_NAME + "/" + FILE_A
                                    + " is in trash directory"));
        }
    }

    @Test
    public void copy_fromTrashDirectoryIsAllowed() throws Exception
    {
        login();

        afsClient.delete(owner, FILE_A, true);
        afsClient.copy(owner, TRASH_FOLDER_NAME + "/" + FILE_A, owner, FILE_A);
    }

    @Test
    public void copy_toSnapshotsDirectoryIsNotAllowed() throws Exception
    {
        login();

        try
        {
            afsClient.copy(owner, FILE_A, owner, SNAPSHOTS_FOLDER_NAME + "/" + FILE_A);
            fail();
        } catch (Exception e)
        {
            assertTrue(e.getMessage(),
                    e.getMessage().contains("Path can't be operated by: Copy - " + testDataRoot + "/" + SNAPSHOTS_FOLDER_NAME + "/" + FILE_A
                            + " is in snapshots directory"));
        }
    }

    @Test
    public void copy_fromSnapshotsDirectoryIsAllowed() throws Exception
    {
        login();

        afsClient.snapshot(owner, FILE_A);
        afsClient.copy(owner, SNAPSHOTS_FOLDER_NAME + "/" + FILE_A, owner, FILE_A + ".copy");
    }

    @Test
    public void move_file() throws Exception
    {
        login();

        afsClient.snapshot(owner, FILE_A);

        File[] filesBefore = listFilesFromAFS(afsClient, owner, "/");
        assertEquals("""
                /.afs.snapshots, FOLDER, null
                /.afs.snapshots/A.txt, FOLDER, null
                /.afs.snapshots/A.txt/<SNAPSHOT>, FILE, 4
                /A.txt, FILE, 4
                /test-folder, FOLDER, null
                /test-folder/test-subfolder, FOLDER, null
                /test-folder/test-subfolder/test.png, FILE, 19951
                """, replaceSnapshots(printFiles(filesBefore)));

        Boolean result = afsClient.move(owner, FILE_A, owner, FILE_B);
        assertTrue(result);

        File[] filesAfter = listFilesFromAFS(afsClient, owner, "/");
        assertEquals("""
                /.afs.snapshots, FOLDER, null
                /.afs.snapshots/B.txt, FOLDER, null
                /.afs.snapshots/B.txt/<SNAPSHOT>, FILE, 4
                /B.txt, FILE, 4
                /test-folder, FOLDER, null
                /test-folder/test-subfolder, FOLDER, null
                /test-folder/test-subfolder/test.png, FILE, 19951
                """, replaceSnapshots(printFiles(filesAfter)));

        byte[] testDataFile = IOUtils.readFully(IOUtils.getPath(testDataRoot, FILE_B));
        assertArrayEquals(DATA, testDataFile);
    }

    @Test
    public void move_toTrashDirectoryIsNotAllowed() throws Exception
    {
        login();

        try
        {
            afsClient.move(owner, FILE_A, owner, TRASH_FOLDER_NAME + "/" + FILE_A);
            fail();
        } catch (Exception e)
        {
            assertTrue(e.getMessage(),
                    e.getMessage().contains(
                            "Path can't be operated by: Move - " + testDataRoot + "/" + TRASH_FOLDER_NAME + "/" + FILE_A
                                    + " is in trash directory"));
        }
    }

    @Test
    public void move_fromTrashDirectoryIsAllowed() throws Exception
    {
        login();

        afsClient.delete(owner, FILE_A, true);
        afsClient.move(owner, TRASH_FOLDER_NAME + "/" + FILE_A, owner, FILE_A);
    }

    @Test
    public void move_toSnapshotsDirectoryIsNotAllowed() throws Exception
    {
        login();

        try
        {
            afsClient.move(owner, FILE_A, owner, SNAPSHOTS_FOLDER_NAME + "/" + FILE_A);
            fail();
        } catch (Exception e)
        {
            assertTrue(e.getMessage(),
                    e.getMessage().contains("Path can't be operated by: Move - " + testDataRoot + "/" + SNAPSHOTS_FOLDER_NAME + "/" + FILE_A
                            + " is in snapshots directory"));
        }
    }

    @Test
    public void move_fromSnapshotsDirectoryIsAllowed() throws Exception
    {
        login();

        afsClient.snapshot(owner, FILE_A);
        afsClient.move(owner, SNAPSHOTS_FOLDER_NAME + "/" + FILE_A, owner, FILE_A + ".copy");
    }

    @Test
    public void download_successfully_from_dir_to_dir() throws Exception
    {
        login();

        String serverMainDirectory = "/tobedownloaded";
        afsClient.create(owner, serverMainDirectory, true);
        for (int i = 0; i < 5; i++)
        {
            String testFileName = serverMainDirectory + String.format("/test%s.txt", i);
            afsClient.create(owner, testFileName, false);
            byte[] testFileContent = String.format("TEST_FILE_CONTENT_%s", i).getBytes(StandardCharsets.UTF_8);
            afsClient.write(new ch.ethz.sis.afsapi.dto.Chunk[] {
                    new ch.ethz.sis.afsapi.dto.Chunk(owner, testFileName, 0L, testFileContent.length, testFileContent) });
        }
        for (int i = 0; i < 3; i++)
        {
            String subDirName = serverMainDirectory + String.format("/subdir%s", i);
            afsClient.create(owner, subDirName, true);
            for (int j = 0; j < 3; j++)
            {
                String subsubDirName = subDirName + String.format("/subsubdir%s_%s", i, j);
                afsClient.create(owner, subsubDirName, true);
                for (int k = 0; k < 5; k++)
                {
                    String testFileName = subsubDirName + String.format("/test_%s_%s_%s.txt", i, j, k);
                    if (k == 2)
                    {
                        testFileName = testFileName + TemporaryPathUtil.OPENBIS_TMP_SUFFIX;
                    }
                    afsClient.create(owner, testFileName, false);
                    byte[] testFileContent;
                    if (k != 4)
                    {
                        testFileContent = String.format("TEST_FILE_CONTENT_%s_%s_%s", i, j, k).getBytes(StandardCharsets.UTF_8);
                    } else
                    {
                        testFileContent = new byte[0];
                    }
                    afsClient.write(new ch.ethz.sis.afsapi.dto.Chunk[] {
                            new ch.ethz.sis.afsapi.dto.Chunk(owner, testFileName, 0L, testFileContent.length, testFileContent) });
                }
            }
        }

        int numberOfBigFiles = 3;
        byte[][] bigFileSha256s = new byte[numberOfBigFiles][];
        for (int i = 0; i < numberOfBigFiles; i++)
        {
            String subDirName = serverMainDirectory + String.format("/subdirwithbigfile%s", i);
            afsClient.create(owner, subDirName, true);
            String testFileName = subDirName + String.format("/bigfiletest_%s.txt", i);
            afsClient.create(owner, testFileName, false);
            int maxSize = 100000;
            long j = 0;
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            while (j + getMaxUsableChunkSize() < maxSize)
            {
                byte[] testFileContent = new byte[getMaxUsableChunkSize()];
                Arrays.fill(testFileContent, (byte) j);
                afsClient.write(new ch.ethz.sis.afsapi.dto.Chunk[] {
                        new ch.ethz.sis.afsapi.dto.Chunk(owner, testFileName, j, testFileContent.length, testFileContent) });
                messageDigest.update(testFileContent);
                j += getMaxUsableChunkSize();
            }
            bigFileSha256s[i] = messageDigest.digest();
        }

        Path resourceDirectoryPath = Path.of(getClass().getClassLoader().getResource(DOWNLOAD_TEST_RESOURCE_DIRECTORY).getPath());
        IOUtils.list(resourceDirectoryPath.toString(), true).forEach(file ->
        {
            try
            {
                IOUtils.delete(file.getPath());
            } catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });

        afsClient.download(owner, Path.of("/"), resourceDirectoryPath, ClientAPI.overrideCollisionListener,
                new ClientAPI.DefaultTransferMonitorLister());

        for (int i = 0; i < 5; i++)
        {
            String testFileName = serverMainDirectory + String.format("/test%s.txt", i);
            byte[] testFileContent = String.format("TEST_FILE_CONTENT_%s", i).getBytes(StandardCharsets.UTF_8);
            Path filePath = Path.of(getClass().getClassLoader().getResource(DOWNLOAD_TEST_RESOURCE_DIRECTORY + testFileName).getPath());
            assertArrayEquals(testFileContent, Files.readAllBytes(filePath));
        }
        for (int i = 0; i < 3; i++)
        {
            String subDirName = serverMainDirectory + String.format("/subdir%s", i);
            for (int j = 0; j < 3; j++)
            {
                String subsubDirName = subDirName + String.format("/subsubdir%s_%s", i, j);
                for (int k = 0; k < 5; k++)
                {
                    String testFileName = subsubDirName + String.format("/test_%s_%s_%s.txt", i, j, k);
                    if (k == 2)
                    {
                        testFileName = testFileName + TemporaryPathUtil.OPENBIS_TMP_SUFFIX;
                    }
                    byte[] testFileContent;
                    if (k != 4)
                    {
                        testFileContent = String.format("TEST_FILE_CONTENT_%s_%s_%s", i, j, k).getBytes(StandardCharsets.UTF_8);
                    } else
                    {
                        testFileContent = new byte[0];
                    }
                    if (k != 2)
                    {
                        Path filePath = Path.of(getClass().getClassLoader().getResource(DOWNLOAD_TEST_RESOURCE_DIRECTORY + testFileName).getPath());
                        assertArrayEquals(testFileContent, Files.readAllBytes(filePath));
                    } else
                    {
                        Assert.assertNull(getClass().getClassLoader().getResource(DOWNLOAD_TEST_RESOURCE_DIRECTORY + testFileName));
                    }
                }
            }
        }
        for (int i = 0; i < numberOfBigFiles; i++)
        {
            String subDirName = serverMainDirectory + String.format("/subdirwithbigfile%s", i);
            String testFileName = subDirName + String.format("/bigfiletest_%s.txt", i);
            Path filePath = Path.of(getClass().getClassLoader().getResource(DOWNLOAD_TEST_RESOURCE_DIRECTORY + testFileName).getPath());
            assertArrayEquals(bigFileSha256s[i], DigestUtils.sha256(new FileInputStream(filePath.toFile())));
        }
    }

    @Test
    public void download_successfully_from_dir_to_dir_with_relative_path() throws Exception
    {
        login();

        String serverMainDirectory = "/tobedownloaded";
        afsClient.create(owner, serverMainDirectory, true);
        for (int i = 0; i < 5; i++)
        {
            String testFileName = serverMainDirectory + String.format("/test%s.txt", i);
            afsClient.create(owner, testFileName, false);
            byte[] testFileContent = String.format("TEST_FILE_CONTENT_%s", i).getBytes(StandardCharsets.UTF_8);
            afsClient.write(new ch.ethz.sis.afsapi.dto.Chunk[] {
                    new ch.ethz.sis.afsapi.dto.Chunk(owner, testFileName, 0L, testFileContent.length, testFileContent) });
        }
        for (int i = 0; i < 3; i++)
        {
            String subDirName = serverMainDirectory + String.format("/subdir%s", i);
            afsClient.create(owner, subDirName, true);
            for (int j = 0; j < 3; j++)
            {
                String subsubDirName = subDirName + String.format("/subsubdir%s_%s", i, j);
                afsClient.create(owner, subsubDirName, true);
                for (int k = 0; k < 5; k++)
                {
                    String testFileName = subsubDirName + String.format("/test_%s_%s_%s.txt", i, j, k);
                    if (k == 2)
                    {
                        testFileName = testFileName + TemporaryPathUtil.OPENBIS_TMP_SUFFIX;
                    }
                    afsClient.create(owner, testFileName, false);
                    byte[] testFileContent;
                    if (k != 4)
                    {
                        testFileContent = String.format("TEST_FILE_CONTENT_%s_%s_%s", i, j, k).getBytes(StandardCharsets.UTF_8);
                    } else
                    {
                        testFileContent = new byte[0];
                    }
                    afsClient.write(new ch.ethz.sis.afsapi.dto.Chunk[] {
                            new ch.ethz.sis.afsapi.dto.Chunk(owner, testFileName, 0L, testFileContent.length, testFileContent) });
                }
            }
        }

        int numberOfBigFiles = 3;
        byte[][] bigFileSha256s = new byte[numberOfBigFiles][];
        for (int i = 0; i < numberOfBigFiles; i++)
        {
            String subDirName = serverMainDirectory + String.format("/subdirwithbigfile%s", i);
            afsClient.create(owner, subDirName, true);
            String testFileName = subDirName + String.format("/bigfiletest_%s.txt", i);
            afsClient.create(owner, testFileName, false);
            int maxSize = 100000;
            long j = 0;
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            while (j + getMaxUsableChunkSize() < maxSize)
            {
                byte[] testFileContent = new byte[getMaxUsableChunkSize()];
                Arrays.fill(testFileContent, (byte) j);
                afsClient.write(new ch.ethz.sis.afsapi.dto.Chunk[] {
                        new ch.ethz.sis.afsapi.dto.Chunk(owner, testFileName, j, testFileContent.length, testFileContent) });
                messageDigest.update(testFileContent);
                j += getMaxUsableChunkSize();
            }
            bigFileSha256s[i] = messageDigest.digest();
        }

        Path resourceDirectoryPath = Path.of(getClass().getClassLoader().getResource(DOWNLOAD_TEST_RESOURCE_DIRECTORY).getPath());
        IOUtils.list(resourceDirectoryPath.toString(), true).forEach(file ->
        {
            try
            {
                IOUtils.delete(file.getPath());
            } catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });

        afsClient.download(owner, Path.of("/"), Path.of(new java.io.File(".").getCanonicalPath()).relativize(resourceDirectoryPath),
                ClientAPI.overrideCollisionListener, new ClientAPI.DefaultTransferMonitorLister());

        for (int i = 0; i < 5; i++)
        {
            String testFileName = serverMainDirectory + String.format("/test%s.txt", i);
            byte[] testFileContent = String.format("TEST_FILE_CONTENT_%s", i).getBytes(StandardCharsets.UTF_8);
            Path filePath = Path.of(getClass().getClassLoader().getResource(DOWNLOAD_TEST_RESOURCE_DIRECTORY + testFileName).getPath());
            assertArrayEquals(testFileContent, Files.readAllBytes(filePath));
        }
        for (int i = 0; i < 3; i++)
        {
            String subDirName = serverMainDirectory + String.format("/subdir%s", i);
            for (int j = 0; j < 3; j++)
            {
                String subsubDirName = subDirName + String.format("/subsubdir%s_%s", i, j);
                for (int k = 0; k < 5; k++)
                {
                    String testFileName = subsubDirName + String.format("/test_%s_%s_%s.txt", i, j, k);
                    if (k == 2)
                    {
                        testFileName = testFileName + TemporaryPathUtil.OPENBIS_TMP_SUFFIX;
                    }
                    byte[] testFileContent;
                    if (k != 4)
                    {
                        testFileContent = String.format("TEST_FILE_CONTENT_%s_%s_%s", i, j, k).getBytes(StandardCharsets.UTF_8);
                    } else
                    {
                        testFileContent = new byte[0];
                    }
                    if (k != 2)
                    {
                        Path filePath = Path.of(getClass().getClassLoader().getResource(DOWNLOAD_TEST_RESOURCE_DIRECTORY + testFileName).getPath());
                        assertArrayEquals(testFileContent, Files.readAllBytes(filePath));
                    } else
                    {
                        Assert.assertNull(getClass().getClassLoader().getResource(DOWNLOAD_TEST_RESOURCE_DIRECTORY + testFileName));
                    }
                }
            }
        }
        for (int i = 0; i < numberOfBigFiles; i++)
        {
            String subDirName = serverMainDirectory + String.format("/subdirwithbigfile%s", i);
            String testFileName = subDirName + String.format("/bigfiletest_%s.txt", i);
            Path filePath = Path.of(getClass().getClassLoader().getResource(DOWNLOAD_TEST_RESOURCE_DIRECTORY + testFileName).getPath());
            assertArrayEquals(bigFileSha256s[i], DigestUtils.sha256(new FileInputStream(filePath.toFile())));
        }
    }

    @Test
    public void download_successfully_from_dir_to_dir_skipping_subdir() throws Exception
    {
        login();

        String serverMainDirectory = "/tobedownloaded";
        afsClient.create(owner, serverMainDirectory, true);
        for (int i = 0; i < 5; i++)
        {
            String testFileName = serverMainDirectory + String.format("/test%s.txt", i);
            afsClient.create(owner, testFileName, false);
            byte[] testFileContent = String.format("TEST_FILE_CONTENT_%s", i).getBytes(StandardCharsets.UTF_8);
            afsClient.write(new ch.ethz.sis.afsapi.dto.Chunk[] {
                    new ch.ethz.sis.afsapi.dto.Chunk(owner, testFileName, 0L, testFileContent.length, testFileContent) });
        }
        for (int i = 0; i < 3; i++)
        {
            String subDirName = serverMainDirectory + String.format("/subdir%s", i);
            afsClient.create(owner, subDirName, true);
            for (int j = 0; j < 3; j++)
            {
                String subsubDirName = subDirName + String.format("/subsubdir%s_%s", i, j);
                afsClient.create(owner, subsubDirName, true);
                for (int k = 0; k < 5; k++)
                {
                    String testFileName = subsubDirName + String.format("/test_%s_%s_%s.txt", i, j, k);
                    afsClient.create(owner, testFileName, false);
                    byte[] testFileContent = String.format("TEST_FILE_CONTENT_%s_%s_%s", i, j, k).getBytes(StandardCharsets.UTF_8);
                    afsClient.write(new ch.ethz.sis.afsapi.dto.Chunk[] {
                            new ch.ethz.sis.afsapi.dto.Chunk(owner, testFileName, 0L, testFileContent.length, testFileContent) });
                }
            }
        }

        Path resourceDirectoryPath = Path.of(getClass().getClassLoader().getResource(DOWNLOAD_TEST_RESOURCE_DIRECTORY).getPath());
        IOUtils.list(resourceDirectoryPath.toString(), true).forEach(file ->
        {
            try
            {
                IOUtils.delete(file.getPath());
            } catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });

        afsClient.download(owner, Path.of("/"), resourceDirectoryPath, new ClientAPI.FileCollisionListener()
        {
            @Override
            public ClientAPI.CollisionAction precheck(@NonNull Path sourcePath, @NonNull Path destinationPath, boolean collision)
            {
                if (destinationPath.toAbsolutePath().startsWith(resourceDirectoryPath.toAbsolutePath().resolve("subdir2")))
                {
                    return ClientAPI.CollisionAction.Skip;
                } else
                {
                    return ClientAPI.CollisionAction.Override;
                }
            }
        }, new ClientAPI.DefaultTransferMonitorLister());

        for (int i = 0; i < 5; i++)
        {
            String testFileName = serverMainDirectory + String.format("/test%s.txt", i);
            byte[] testFileContent = String.format("TEST_FILE_CONTENT_%s", i).getBytes(StandardCharsets.UTF_8);
            Path filePath = Path.of(getClass().getClassLoader().getResource(DOWNLOAD_TEST_RESOURCE_DIRECTORY + testFileName).getPath());
            assertArrayEquals(testFileContent, Files.readAllBytes(filePath));
        }
        for (int i = 0; i < 3; i++)
        {
            String subDirName = serverMainDirectory + String.format("/subdir%s", i);
            if (i != 2)
            {
                Assert.assertTrue(
                        Files.isDirectory(Path.of(getClass().getClassLoader().getResource(DOWNLOAD_TEST_RESOURCE_DIRECTORY + subDirName).getPath())));
            } else
            {
                Assert.assertTrue(
                        Files.exists(Path.of(getClass().getClassLoader().getResource(DOWNLOAD_TEST_RESOURCE_DIRECTORY + subDirName).getPath())));
            }
            for (int j = 0; j < 3; j++)
            {
                String subsubDirName = subDirName + String.format("/subsubdir%s_%s", i, j);
                for (int k = 0; k < 5; k++)
                {
                    String testFileName = subsubDirName + String.format("/test_%s_%s_%s.txt", i, j, k);
                    if (i != 2)
                    {
                        byte[] testFileContent = String.format("TEST_FILE_CONTENT_%s_%s_%s", i, j, k).getBytes(StandardCharsets.UTF_8);
                        Path filePath = Path.of(getClass().getClassLoader().getResource(DOWNLOAD_TEST_RESOURCE_DIRECTORY + testFileName).getPath());
                        assertArrayEquals(testFileContent, Files.readAllBytes(filePath));
                    } else
                    {
                        Assert.assertFalse(IOUtils.exists(testFileName));
                    }
                }
            }
        }
    }

    @Test
    public void download_successfully_overwriting_files() throws Exception
    {
        login();

        String serverMainDirectory = "/tobedownloaded";
        afsClient.create(owner, serverMainDirectory, true);
        for (int i = 0; i < 5; i++)
        {
            String testFileName = serverMainDirectory + String.format("/test%s.txt", i);
            afsClient.create(owner, testFileName, false);
            byte[] testFileContent = String.format("TEST_FILE_CONTENT_%s", i).getBytes(StandardCharsets.UTF_8);
            afsClient.write(new ch.ethz.sis.afsapi.dto.Chunk[] {
                    new ch.ethz.sis.afsapi.dto.Chunk(owner, testFileName, 0L, testFileContent.length, testFileContent) });
        }
        for (int i = 0; i < 3; i++)
        {
            String subDirName = serverMainDirectory + String.format("/subdir%s", i);
            afsClient.create(owner, subDirName, true);
            for (int j = 0; j < 3; j++)
            {
                String subsubDirName = subDirName + String.format("/subsubdir%s_%s", i, j);
                afsClient.create(owner, subsubDirName, true);
                for (int k = 0; k < 5; k++)
                {
                    String testFileName = subsubDirName + String.format("/test_%s_%s_%s.txt", i, j, k);
                    afsClient.create(owner, testFileName, false);
                    byte[] testFileContent;
                    if (k != 4)
                    {
                        testFileContent = String.format("TEST_FILE_CONTENT_%s_%s_%s", i, j, k).getBytes(StandardCharsets.UTF_8);
                    } else
                    {
                        testFileContent = new byte[0];
                    }
                    afsClient.write(new ch.ethz.sis.afsapi.dto.Chunk[] {
                            new ch.ethz.sis.afsapi.dto.Chunk(owner, testFileName, 0L, testFileContent.length, testFileContent) });
                }
            }
        }

        Path resourceDirectoryPath = Path.of(getClass().getClassLoader().getResource(DOWNLOAD_TEST_RESOURCE_DIRECTORY).getPath());
        IOUtils.list(resourceDirectoryPath.toString(), true).forEach(file ->
        {
            try
            {
                IOUtils.delete(file.getPath());
            } catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });

        for (int i = 0; i < 5; i++)
        {
            String testFileName = String.format("/test%s.txt", i);
            byte[] testFileContentToBeOverwritten = String.format("TEST_FILE_CONTENT_TO_BE_OVERWRITTEN_%s", i).getBytes(StandardCharsets.UTF_8);
            IOUtils.createFile(resourceDirectoryPath.toAbsolutePath() + testFileName);
            Path filePath = Path.of(getClass().getClassLoader().getResource(DOWNLOAD_TEST_RESOURCE_DIRECTORY + testFileName).getPath());
            IOUtils.write(filePath.toString(), 0, testFileContentToBeOverwritten);
            assertArrayEquals(testFileContentToBeOverwritten, Files.readAllBytes(filePath));
        }
        for (int i = 0; i < 3; i++)
        {
            String subDirName = String.format("/subdir%s", i);
            IOUtils.createDirectory(resourceDirectoryPath.toAbsolutePath() + subDirName);
            for (int j = 0; j < 3; j++)
            {
                String subsubDirName = subDirName + String.format("/subsubdir%s_%s", i, j);
                IOUtils.createDirectory(resourceDirectoryPath.toAbsolutePath() + subsubDirName);
                for (int k = 0; k < 5; k++)
                {
                    String testFileName = subsubDirName + String.format("/test_%s_%s_%s.txt", i, j, k);
                    byte[] testFileContentToBeOverwritten =
                            String.format("TEST_FILE_CONTENT_TO_BE_OVERWRITTEN_%s_%s_%s", i, j, k).getBytes(StandardCharsets.UTF_8);
                    IOUtils.createFile(resourceDirectoryPath.toAbsolutePath() + testFileName);
                    Path filePath = Path.of(getClass().getClassLoader().getResource(DOWNLOAD_TEST_RESOURCE_DIRECTORY + testFileName).getPath());
                    IOUtils.write(filePath.toString(), 0, testFileContentToBeOverwritten);
                    assertArrayEquals(testFileContentToBeOverwritten, Files.readAllBytes(filePath));
                }
            }
        }

        afsClient.download(owner, Path.of("/"), resourceDirectoryPath, ClientAPI.overrideCollisionListener,
                new ClientAPI.DefaultTransferMonitorLister());

        for (int i = 0; i < 5; i++)
        {
            String testFileName = serverMainDirectory + String.format("/test%s.txt", i);
            byte[] testFileContent = String.format("TEST_FILE_CONTENT_%s", i).getBytes(StandardCharsets.UTF_8);
            Path filePath = Path.of(getClass().getClassLoader().getResource(DOWNLOAD_TEST_RESOURCE_DIRECTORY + testFileName).getPath());
            assertArrayEquals(testFileContent, Files.readAllBytes(filePath));
        }
        for (int i = 0; i < 3; i++)
        {
            String subDirName = serverMainDirectory + String.format("/subdir%s", i);
            for (int j = 0; j < 3; j++)
            {
                String subsubDirName = subDirName + String.format("/subsubdir%s_%s", i, j);
                for (int k = 0; k < 5; k++)
                {
                    String testFileName = subsubDirName + String.format("/test_%s_%s_%s.txt", i, j, k);
                    byte[] testFileContent;
                    if (k != 4)
                    {
                        testFileContent = String.format("TEST_FILE_CONTENT_%s_%s_%s", i, j, k).getBytes(StandardCharsets.UTF_8);
                    } else
                    {
                        testFileContent = new byte[0];
                    }
                    Path filePath = Path.of(getClass().getClassLoader().getResource(DOWNLOAD_TEST_RESOURCE_DIRECTORY + testFileName).getPath());
                    assertArrayEquals(testFileContent, Files.readAllBytes(filePath));
                }
            }
        }
    }

    @Test
    public void download_successfully_from_regular_file_to_dir() throws Exception
    {
        login();

        String serverMainDirectory = "/tobedownloaded";
        afsClient.create(owner, serverMainDirectory, true);

        String testFileName = "/test1.txt";
        String testFilePath = serverMainDirectory + testFileName;
        afsClient.create(owner, testFilePath, false);
        byte[] testFileContent = "TEST_FILE_CONTENT_1".getBytes(StandardCharsets.UTF_8);
        afsClient.write(new ch.ethz.sis.afsapi.dto.Chunk[] {
                new ch.ethz.sis.afsapi.dto.Chunk(owner, testFilePath, 0L, testFileContent.length, testFileContent) });

        Path resourceDirectoryPath = Path.of(getClass().getClassLoader().getResource(DOWNLOAD_TEST_RESOURCE_DIRECTORY).getPath());
        IOUtils.list(resourceDirectoryPath.toString(), true).forEach(file ->
        {
            try
            {
                IOUtils.delete(file.getPath());
            } catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });

        afsClient.download(owner, Path.of(testFilePath), resourceDirectoryPath, ClientAPI.overrideCollisionListener,
                new ClientAPI.DefaultTransferMonitorLister());

        Path filePath = Path.of(getClass().getClassLoader().getResource(DOWNLOAD_TEST_RESOURCE_DIRECTORY + testFileName).getPath());
        assertArrayEquals(testFileContent, Files.readAllBytes(filePath));
    }

    @Test
    public void download_successfully_from_regular_file_to_regular_file() throws Exception
    {
        login();

        String serverMainDirectory = "/tobedownloaded";
        afsClient.create(owner, serverMainDirectory, true);

        String testFileName = "/test1.txt";
        String testFilePath = serverMainDirectory + testFileName;
        afsClient.create(owner, testFilePath, false);
        byte[] testFileContent = "TEST_FILE_CONTENT_1".getBytes(StandardCharsets.UTF_8);
        afsClient.write(new ch.ethz.sis.afsapi.dto.Chunk[] {
                new ch.ethz.sis.afsapi.dto.Chunk(owner, testFilePath, 0L, testFileContent.length, testFileContent) });

        Path resourceDirectoryPath = Path.of(getClass().getClassLoader().getResource(DOWNLOAD_TEST_RESOURCE_DIRECTORY).getPath());
        IOUtils.list(resourceDirectoryPath.toString(), true).forEach(file ->
        {
            try
            {
                IOUtils.delete(file.getPath());
            } catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });

        String localTestFileName = "/test2.txt";
        IOUtils.createFile(resourceDirectoryPath + localTestFileName);
        byte[] contentToBeOverwritten = "CONTENT_TO_BE_OVERWRITTEN".getBytes(StandardCharsets.UTF_8);
        IOUtils.write(resourceDirectoryPath + localTestFileName, 0L, contentToBeOverwritten);
        assertArrayEquals(contentToBeOverwritten, Files.readAllBytes(Path.of(resourceDirectoryPath + localTestFileName)));

        afsClient.download(owner, Path.of(testFilePath), Path.of(resourceDirectoryPath.toAbsolutePath() + localTestFileName),
                ClientAPI.overrideCollisionListener, new ClientAPI.DefaultTransferMonitorLister());

        Path filePath = Path.of(getClass().getClassLoader().getResource(DOWNLOAD_TEST_RESOURCE_DIRECTORY + localTestFileName).getPath());
        assertArrayEquals(testFileContent, Files.readAllBytes(filePath));
    }

    @Test(expected = IllegalArgumentException.class)
    public void download_with_failure_from_dir_to_regular_file() throws Exception
    {
        login();

        String serverMainDirectory = "/tobedownloaded";
        afsClient.create(owner, serverMainDirectory, true);

        String testDirName = "/test1";
        String testDirPath = serverMainDirectory + testDirName;
        afsClient.create(owner, testDirPath, true);

        Path resourceDirectoryPath = Path.of(getClass().getClassLoader().getResource(DOWNLOAD_TEST_RESOURCE_DIRECTORY).getPath());
        IOUtils.list(resourceDirectoryPath.toString(), true).forEach(file ->
        {
            try
            {
                IOUtils.delete(file.getPath());
            } catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });

        String localTestFileName = "/test2.txt";
        IOUtils.createFile(resourceDirectoryPath + localTestFileName);
        byte[] contentToBeOverwritten = "CONTENT_TO_BE_OVERWRITTEN".getBytes(StandardCharsets.UTF_8);
        IOUtils.write(resourceDirectoryPath + localTestFileName, 0L, contentToBeOverwritten);
        assertArrayEquals(contentToBeOverwritten, Files.readAllBytes(Path.of(resourceDirectoryPath + localTestFileName)));

        afsClient.download(owner, Path.of(testDirPath), Path.of(resourceDirectoryPath.toAbsolutePath() + localTestFileName),
                ClientAPI.overrideCollisionListener, new ClientAPI.DefaultTransferMonitorLister());
    }

    @Test(expected = IllegalArgumentException.class)
    public void download_with_failure_source_path_not_absolute() throws Exception
    {
        login();

        String serverMainDirectory = "/tobedownloaded";
        afsClient.create(owner, serverMainDirectory, true);

        Path resourceDirectoryPath = Path.of(getClass().getClassLoader().getResource(DOWNLOAD_TEST_RESOURCE_DIRECTORY).getPath());

        afsClient.download(owner, Path.of("non_absolute_path"), resourceDirectoryPath.toAbsolutePath(), ClientAPI.overrideCollisionListener,
                new ClientAPI.DefaultTransferMonitorLister());
    }

    @Test(expected = IllegalArgumentException.class)
    public void download_with_failure_source_path_not_found() throws Exception
    {
        login();

        String serverMainDirectory = "/tobedownloaded";
        afsClient.create(owner, serverMainDirectory, true);

        Path resourceDirectoryPath = Path.of(getClass().getClassLoader().getResource(DOWNLOAD_TEST_RESOURCE_DIRECTORY).getPath());

        afsClient.download(owner, Path.of("/non_existing_server_file"), resourceDirectoryPath.toAbsolutePath(), ClientAPI.overrideCollisionListener,
                new ClientAPI.DefaultTransferMonitorLister());
    }

    @Test(expected = IllegalArgumentException.class)
    public void download_with_failure_destination_path_not_found() throws Exception
    {
        login();

        String serverMainDirectory = "/tobedownloaded";
        afsClient.create(owner, serverMainDirectory, true);

        String testFileName = "/test1.txt";
        String testFilePath = serverMainDirectory + testFileName;
        afsClient.create(owner, testFilePath, false);
        byte[] testFileContent = "TEST_FILE_CONTENT_1".getBytes(StandardCharsets.UTF_8);
        afsClient.write(new ch.ethz.sis.afsapi.dto.Chunk[] {
                new ch.ethz.sis.afsapi.dto.Chunk(owner, testFilePath, 0L, testFileContent.length, testFileContent) });

        String downloadTestResourceDirectory = DOWNLOAD_TEST_RESOURCE_DIRECTORY;
        Path resourceDirectoryPath = Path.of(getClass().getClassLoader().getResource(downloadTestResourceDirectory).getPath());

        afsClient.download(owner, Path.of(testFilePath), resourceDirectoryPath.resolve("NON_EXISTING_LOCAL_FILE"),
                ClientAPI.overrideCollisionListener, new ClientAPI.DefaultTransferMonitorLister());
    }

    @Test
    public void download_with_failure_from_dir_to_dir_for_concurrent_modification() throws Exception
    {
        login();

        String serverMainDirectory = "/tobedownloaded";
        afsClient.create(owner, serverMainDirectory, true);
        for (int i = 0; i < 5; i++)
        {
            String testFileName = serverMainDirectory + String.format("/test%s.txt", i);
            afsClient.create(owner, testFileName, false);
            byte[] testFileContent = String.format("TEST_FILE_CONTENT_%s", i).getBytes(StandardCharsets.UTF_8);
            afsClient.write(new ch.ethz.sis.afsapi.dto.Chunk[] {
                    new ch.ethz.sis.afsapi.dto.Chunk(owner, testFileName, 0L, testFileContent.length, testFileContent) });
        }

        Path resourceDirectoryPath = Path.of(getClass().getClassLoader().getResource(DOWNLOAD_TEST_RESOURCE_DIRECTORY).getPath());
        IOUtils.list(resourceDirectoryPath.toString(), true).forEach(file ->
        {
            try
            {
                IOUtils.delete(file.getPath());
            } catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });

        ClientAPI.DefaultTransferMonitorLister sneakyContentChangingMonitor = new ClientAPI.DefaultTransferMonitorLister()
        {
            static byte[] CHANGED_FILE_CONTENT = "CHANGED_FILE_CONTENT".getBytes(StandardCharsets.UTF_8);

            @Override
            public synchronized void start(Path from, Path to, long total)
            {
                super.start(from, to, total);
                try
                {
                    afsClient.write(new Chunk[] {
                            new Chunk(owner, from.toAbsolutePath().toString(), 0L, CHANGED_FILE_CONTENT.length, CHANGED_FILE_CONTENT) });
                } catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
        };
        boolean result =
                afsClient.download(owner, Path.of("/"), resourceDirectoryPath, ClientAPI.overrideCollisionListener, sneakyContentChangingMonitor);
        Assert.assertFalse(result);
        Assert.assertTrue(sneakyContentChangingMonitor.getException() instanceof IllegalStateException);
        Assert.assertEquals("Incomplete download", sneakyContentChangingMonitor.getException().getMessage());
    }

    @Test
    public void upload_successfully_from_dir_to_dir() throws Exception
    {
        login();

        Path resourceDirectoryPath = Path.of(getClass().getClassLoader().getResource(UPLOAD_TEST_RESOURCE_DIRECTORY).getPath());
        IOUtils.list(resourceDirectoryPath.toString(), true).forEach(file ->
        {
            try
            {
                IOUtils.delete(file.getPath());
            } catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });

        for (int i = 0; i < 5; i++)
        {
            String testFileName = String.format("/test%s.txt", i);
            byte[] testFileContent = String.format("TEST_FILE_CONTENT_%s", i).getBytes(StandardCharsets.UTF_8);
            Path filePath = Path.of(resourceDirectoryPath.toAbsolutePath() + testFileName);
            IOUtils.createFile(filePath.toAbsolutePath().toString());
            IOUtils.write(filePath.toAbsolutePath().toString(), 0, testFileContent);
            assertArrayEquals(testFileContent, Files.readAllBytes(filePath));
        }
        for (int i = 0; i < 3; i++)
        {
            String subDirName = String.format("/subdir%s", i);
            IOUtils.createDirectory(resourceDirectoryPath.toAbsolutePath() + subDirName);
            for (int j = 0; j < 3; j++)
            {
                String subsubDirName = subDirName + String.format("/subsubdir%s_%s", i, j);
                IOUtils.createDirectory(resourceDirectoryPath.toAbsolutePath() + subsubDirName);
                for (int k = 0; k < 5; k++)
                {
                    String testFileName = subsubDirName + String.format("/test_%s_%s_%s.txt", i, j, k);
                    if (k == 2)
                    {
                        testFileName = testFileName + TemporaryPathUtil.OPENBIS_TMP_SUFFIX;
                    }
                    byte[] testFileContent;
                    if (k != 4)
                    {
                        testFileContent = String.format("TEST_FILE_CONTENT_%s_%s_%s", i, j, k).getBytes(StandardCharsets.UTF_8);
                    } else
                    {
                        testFileContent = new byte[0];
                    }
                    Path filePath = Path.of(resourceDirectoryPath.toAbsolutePath() + testFileName);
                    IOUtils.createFile(filePath.toAbsolutePath().toString());
                    IOUtils.write(filePath.toAbsolutePath().toString(), 0, testFileContent);
                    assertArrayEquals(testFileContent, Files.readAllBytes(filePath));
                }
            }
        }

        int numberOfBigFiles = 3;
        byte[][] bigFileSha256s = new byte[numberOfBigFiles][];
        for (int i = 0; i < numberOfBigFiles; i++)
        {
            String subDirName = String.format("/subdirwithbigfile%s", i);
            IOUtils.createDirectory(resourceDirectoryPath.toAbsolutePath() + subDirName);
            String testFileName = subDirName + String.format("/bigfiletest_%s.txt", i);
            Path filePath = Path.of(resourceDirectoryPath.toAbsolutePath() + testFileName);
            IOUtils.createFile(filePath.toAbsolutePath().toString());
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            for (int j = 0; j < 100; j++)
            {
                byte[] content = new byte[1000];
                Arrays.fill(content, (byte) j);
                IOUtils.write(filePath.toAbsolutePath().toString(), j * 1000, content);
                sha256.update(content);
            }
            bigFileSha256s[i] = sha256.digest();
        }

        String serverUploadDirectory = "/uploads";
        if (AfsClientUploadHelper.getServerFilePresence(afsClient, owner, serverUploadDirectory).isPresent())
        {
            afsClient.delete(owner, serverUploadDirectory, false);
        }
        afsClient.create(owner, serverUploadDirectory, true);

        afsClient.upload(resourceDirectoryPath, owner, Path.of(serverUploadDirectory), ClientAPI.overrideCollisionListener,
                new ClientAPI.DefaultTransferMonitorLister());

        for (int i = 0; i < 5; i++)
        {
            String testFileName = String.format("/test%s.txt", i);
            byte[] testFileContent = String.format("TEST_FILE_CONTENT_%s", i).getBytes(StandardCharsets.UTF_8);
            Chunk[] readChunk =
                    afsClient.read(new Chunk[] { new Chunk(owner, serverUploadDirectory + testFileName, 0L, testFileContent.length, new byte[0]) });
            assertArrayEquals(testFileContent, readChunk[0].getData());
        }
        for (int i = 0; i < 3; i++)
        {
            String subDirName = String.format("/subdir%s", i);
            for (int j = 0; j < 3; j++)
            {
                String subsubDirName = subDirName + String.format("/subsubdir%s_%s", i, j);
                for (int k = 0; k < 5; k++)
                {
                    if (k == 2)
                    {
                        String testFileName = subsubDirName + String.format("/test_%s_%s_%s.txt", i, j, k) + TemporaryPathUtil.OPENBIS_TMP_SUFFIX;
                        Assert.assertTrue(
                                AfsClientUploadHelper.getServerFilePresence(afsClient, owner, serverUploadDirectory + testFileName).isEmpty());
                    } else if (k == 4)
                    {
                        String testFileName = subsubDirName + String.format("/test_%s_%s_%s.txt", i, j, k);
                        byte[] testFileContent = new byte[0];
                        Chunk[] readChunk = afsClient.read(
                                new Chunk[] { new Chunk(owner, serverUploadDirectory + testFileName, 0L, testFileContent.length, new byte[0]) });
                        assertArrayEquals(testFileContent, readChunk[0].getData());
                    } else
                    {
                        String testFileName = subsubDirName + String.format("/test_%s_%s_%s.txt", i, j, k);
                        byte[] testFileContent = String.format("TEST_FILE_CONTENT_%s_%s_%s", i, j, k).getBytes(StandardCharsets.UTF_8);
                        Chunk[] readChunk = afsClient.read(
                                new Chunk[] { new Chunk(owner, serverUploadDirectory + testFileName, 0L, testFileContent.length, new byte[0]) });
                        assertArrayEquals(testFileContent, readChunk[0].getData());
                    }
                }
            }
        }

        for (int i = 0; i < numberOfBigFiles; i++)
        {
            String subDirName = String.format("/subdirwithbigfile%s", i);
            String testFileName = subDirName + String.format("/bigfiletest_%s.txt", i);
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            Chunk[] readChunk;
            int j = 0;
            do
            {
                int offset = j * getMaxUsableChunkSize();
                readChunk = afsClient.read(new Chunk[] {
                        new Chunk(owner, serverUploadDirectory + testFileName, (long) offset, Integer.min(getMaxUsableChunkSize(), 100000 - offset),
                                new byte[0]) });
                sha256.update(readChunk[0].getData());
                j++;
            } while (readChunk[0].getOffset() + readChunk[0].getData().length < 100000);
            assertArrayEquals(bigFileSha256s[i], sha256.digest());
        }
    }

    @Test
    public void upload_successfully_from_dir_to_dir_with_relative_path() throws Exception
    {
        login();

        Path resourceDirectoryPath = Path.of(getClass().getClassLoader().getResource(UPLOAD_TEST_RESOURCE_DIRECTORY).getPath());
        IOUtils.list(resourceDirectoryPath.toString(), true).forEach(file ->
        {
            try
            {
                IOUtils.delete(file.getPath());
            } catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });

        for (int i = 0; i < 5; i++)
        {
            String testFileName = String.format("/test%s.txt", i);
            byte[] testFileContent = String.format("TEST_FILE_CONTENT_%s", i).getBytes(StandardCharsets.UTF_8);
            Path filePath = Path.of(resourceDirectoryPath.toAbsolutePath() + testFileName);
            IOUtils.createFile(filePath.toAbsolutePath().toString());
            IOUtils.write(filePath.toAbsolutePath().toString(), 0, testFileContent);
            assertArrayEquals(testFileContent, Files.readAllBytes(filePath));
        }
        for (int i = 0; i < 3; i++)
        {
            String subDirName = String.format("/subdir%s", i);
            IOUtils.createDirectory(resourceDirectoryPath.toAbsolutePath() + subDirName);
            for (int j = 0; j < 3; j++)
            {
                String subsubDirName = subDirName + String.format("/subsubdir%s_%s", i, j);
                IOUtils.createDirectory(resourceDirectoryPath.toAbsolutePath() + subsubDirName);
                for (int k = 0; k < 5; k++)
                {
                    String testFileName = subsubDirName + String.format("/test_%s_%s_%s.txt", i, j, k);
                    if (k == 2)
                    {
                        testFileName = testFileName + TemporaryPathUtil.OPENBIS_TMP_SUFFIX;
                    }
                    byte[] testFileContent;
                    if (k != 4)
                    {
                        testFileContent = String.format("TEST_FILE_CONTENT_%s_%s_%s", i, j, k).getBytes(StandardCharsets.UTF_8);
                    } else
                    {
                        testFileContent = new byte[0];
                    }
                    Path filePath = Path.of(resourceDirectoryPath.toAbsolutePath() + testFileName);
                    IOUtils.createFile(filePath.toAbsolutePath().toString());
                    IOUtils.write(filePath.toAbsolutePath().toString(), 0, testFileContent);
                    assertArrayEquals(testFileContent, Files.readAllBytes(filePath));
                }
            }
        }

        int numberOfBigFiles = 3;
        byte[][] bigFileSha256s = new byte[numberOfBigFiles][];
        for (int i = 0; i < numberOfBigFiles; i++)
        {
            String subDirName = String.format("/subdirwithbigfile%s", i);
            IOUtils.createDirectory(resourceDirectoryPath.toAbsolutePath() + subDirName);
            String testFileName = subDirName + String.format("/bigfiletest_%s.txt", i);
            Path filePath = Path.of(resourceDirectoryPath.toAbsolutePath() + testFileName);
            IOUtils.createFile(filePath.toAbsolutePath().toString());
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            for (int j = 0; j < 100; j++)
            {
                byte[] content = new byte[1000];
                Arrays.fill(content, (byte) j);
                IOUtils.write(filePath.toAbsolutePath().toString(), j * 1000, content);
                sha256.update(content);
            }
            bigFileSha256s[i] = sha256.digest();
        }

        String serverUploadDirectory = "/uploads";
        if (AfsClientUploadHelper.getServerFilePresence(afsClient, owner, serverUploadDirectory).isPresent())
        {
            afsClient.delete(owner, serverUploadDirectory, false);
        }
        afsClient.create(owner, serverUploadDirectory, true);

        afsClient.upload(Path.of(new java.io.File(".").getCanonicalPath()).relativize(resourceDirectoryPath), owner, Path.of(serverUploadDirectory),
                ClientAPI.overrideCollisionListener, new ClientAPI.DefaultTransferMonitorLister());

        for (int i = 0; i < 5; i++)
        {
            String testFileName = String.format("/test%s.txt", i);
            byte[] testFileContent = String.format("TEST_FILE_CONTENT_%s", i).getBytes(StandardCharsets.UTF_8);
            Chunk[] readChunk =
                    afsClient.read(new Chunk[] { new Chunk(owner, serverUploadDirectory + testFileName, 0L, testFileContent.length, new byte[0]) });
            assertArrayEquals(testFileContent, readChunk[0].getData());
        }
        for (int i = 0; i < 3; i++)
        {
            String subDirName = String.format("/subdir%s", i);
            for (int j = 0; j < 3; j++)
            {
                String subsubDirName = subDirName + String.format("/subsubdir%s_%s", i, j);
                for (int k = 0; k < 5; k++)
                {
                    if (k == 2)
                    {
                        String testFileName = subsubDirName + String.format("/test_%s_%s_%s.txt", i, j, k) + TemporaryPathUtil.OPENBIS_TMP_SUFFIX;
                        Assert.assertTrue(
                                AfsClientUploadHelper.getServerFilePresence(afsClient, owner, serverUploadDirectory + testFileName).isEmpty());
                    } else if (k == 4)
                    {
                        String testFileName = subsubDirName + String.format("/test_%s_%s_%s.txt", i, j, k);
                        byte[] testFileContent = new byte[0];
                        Chunk[] readChunk = afsClient.read(
                                new Chunk[] { new Chunk(owner, serverUploadDirectory + testFileName, 0L, testFileContent.length, new byte[0]) });
                        assertArrayEquals(testFileContent, readChunk[0].getData());
                    } else
                    {
                        String testFileName = subsubDirName + String.format("/test_%s_%s_%s.txt", i, j, k);
                        byte[] testFileContent = String.format("TEST_FILE_CONTENT_%s_%s_%s", i, j, k).getBytes(StandardCharsets.UTF_8);
                        Chunk[] readChunk = afsClient.read(
                                new Chunk[] { new Chunk(owner, serverUploadDirectory + testFileName, 0L, testFileContent.length, new byte[0]) });
                        assertArrayEquals(testFileContent, readChunk[0].getData());
                    }
                }
            }
        }

        for (int i = 0; i < numberOfBigFiles; i++)
        {
            String subDirName = String.format("/subdirwithbigfile%s", i);
            String testFileName = subDirName + String.format("/bigfiletest_%s.txt", i);
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            Chunk[] readChunk;
            int j = 0;
            do
            {
                int offset = j * getMaxUsableChunkSize();
                readChunk = afsClient.read(new Chunk[] {
                        new Chunk(owner, serverUploadDirectory + testFileName, (long) offset, Integer.min(getMaxUsableChunkSize(), 100000 - offset),
                                new byte[0]) });
                sha256.update(readChunk[0].getData());
                j++;
            } while (readChunk[0].getOffset() + readChunk[0].getData().length < 100000);
            assertArrayEquals(bigFileSha256s[i], sha256.digest());
        }
    }

    @Test
    public void upload_successfully_from_dir_to_dir_skipping_subdir() throws Exception
    {
        login();

        Path resourceDirectoryPath = Path.of(getClass().getClassLoader().getResource(UPLOAD_TEST_RESOURCE_DIRECTORY).getPath());
        IOUtils.list(resourceDirectoryPath.toString(), true).forEach(file ->
        {
            try
            {
                IOUtils.delete(file.getPath());
            } catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });

        for (int i = 0; i < 5; i++)
        {
            String testFileName = String.format("/test%s.txt", i);
            byte[] testFileContent = String.format("TEST_FILE_CONTENT_%s", i).getBytes(StandardCharsets.UTF_8);
            Path filePath = Path.of(resourceDirectoryPath.toAbsolutePath() + testFileName);
            IOUtils.createFile(filePath.toAbsolutePath().toString());
            IOUtils.write(filePath.toAbsolutePath().toString(), 0, testFileContent);
            assertArrayEquals(testFileContent, Files.readAllBytes(filePath));
        }
        for (int i = 0; i < 3; i++)
        {
            String subDirName = String.format("/subdir%s", i);
            IOUtils.createDirectory(resourceDirectoryPath.toAbsolutePath() + subDirName);
            for (int j = 0; j < 3; j++)
            {
                String subsubDirName = subDirName + String.format("/subsubdir%s_%s", i, j);
                IOUtils.createDirectory(resourceDirectoryPath.toAbsolutePath() + subsubDirName);
                for (int k = 0; k < 5; k++)
                {
                    String testFileName = subsubDirName + String.format("/test_%s_%s_%s.txt", i, j, k);
                    byte[] testFileContent = String.format("TEST_FILE_CONTENT_%s_%s_%s", i, j, k).getBytes(StandardCharsets.UTF_8);
                    Path filePath = Path.of(resourceDirectoryPath.toAbsolutePath() + testFileName);
                    IOUtils.createFile(filePath.toAbsolutePath().toString());
                    IOUtils.write(filePath.toAbsolutePath().toString(), 0, testFileContent);
                    assertArrayEquals(testFileContent, Files.readAllBytes(filePath));
                }
            }
        }

        String serverUploadDirectory = "/uploads";
        if (AfsClientUploadHelper.getServerFilePresence(afsClient, owner, serverUploadDirectory).isPresent())
        {
            afsClient.delete(owner, serverUploadDirectory, false);
        }
        afsClient.create(owner, serverUploadDirectory, true);

        afsClient.upload(resourceDirectoryPath, owner, Path.of(serverUploadDirectory), new ClientAPI.FileCollisionListener()
        {
            @Override
            public ClientAPI.CollisionAction precheck(@NonNull Path sourcePath, @NonNull Path destinationPath, boolean collision)
            {
                if (sourcePath.toAbsolutePath().startsWith(resourceDirectoryPath.toAbsolutePath().resolve("subdir2")))
                {
                    return ClientAPI.CollisionAction.Skip;
                } else
                {
                    return ClientAPI.CollisionAction.Override;
                }
            }
        }, new ClientAPI.DefaultTransferMonitorLister());

        for (int i = 0; i < 5; i++)
        {
            String testFileName = String.format("/test%s.txt", i);
            byte[] testFileContent = String.format("TEST_FILE_CONTENT_%s", i).getBytes(StandardCharsets.UTF_8);
            Chunk[] readChunk =
                    afsClient.read(new Chunk[] { new Chunk(owner, serverUploadDirectory + testFileName, 0L, testFileContent.length, new byte[0]) });
            assertArrayEquals(testFileContent, readChunk[0].getData());
        }
        for (int i = 0; i < 3; i++)
        {
            String subDirName = String.format("/subdir%s", i);
            Optional<File> checkedDir = AfsClientUploadHelper.getServerFilePresence(afsClient, owner, serverUploadDirectory + subDirName);
            if (i != 2)
            {
                Assert.assertEquals(true, checkedDir.get().getDirectory());
            } else
            {
                Assert.assertEquals(Optional.empty(), checkedDir);
            }
            for (int j = 0; j < 3; j++)
            {
                String subsubDirName = subDirName + String.format("/subsubdir%s_%s", i, j);
                for (int k = 0; k < 5; k++)
                {
                    String testFileName = subsubDirName + String.format("/test_%s_%s_%s.txt", i, j, k);
                    if (i != 2)
                    {
                        byte[] testFileContent = String.format("TEST_FILE_CONTENT_%s_%s_%s", i, j, k).getBytes(StandardCharsets.UTF_8);
                        Chunk[] readChunk = afsClient.read(
                                new Chunk[] { new Chunk(owner, serverUploadDirectory + testFileName, 0L, testFileContent.length, new byte[0]) });
                        assertArrayEquals(testFileContent, readChunk[0].getData());
                    } else
                    {
                        Optional<File> absentFile =
                                AfsClientUploadHelper.getServerFilePresence(afsClient, owner, serverUploadDirectory + testFileName);
                        Assert.assertEquals(Optional.empty(), absentFile);
                    }
                }
            }
        }
    }

    @Test
    public void upload_successfully_overwriting_files() throws Exception
    {
        login();

        String serverUploadDirectory = "/uploads";
        if (AfsClientUploadHelper.getServerFilePresence(afsClient, owner, serverUploadDirectory).isPresent())
        {
            afsClient.delete(owner, serverUploadDirectory, false);
        }
        afsClient.create(owner, serverUploadDirectory, true);

        for (int i = 0; i < 3; i++)
        {
            String subDirName = serverUploadDirectory + String.format("/subdir%s", i);
            afsClient.create(owner, subDirName, true);
            for (int j = 0; j < 3; j++)
            {
                String subsubDirName = subDirName + String.format("/subsubdir%s_%s", i, j);
                afsClient.create(owner, subsubDirName, true);
                for (int k = 0; k < 5; k++)
                {
                    String testFileName = subsubDirName + String.format("/test_%s_%s_%s.txt", i, j, k);
                    afsClient.create(owner, testFileName, false);
                    byte[] testFileContent = String.format("TEST_FILE_CONTENT_TO_BE_OVERWRITTEN_%s_%s_%s", i, j, k).getBytes(StandardCharsets.UTF_8);
                    afsClient.write(new ch.ethz.sis.afsapi.dto.Chunk[] {
                            new ch.ethz.sis.afsapi.dto.Chunk(owner, testFileName, 0L, testFileContent.length, testFileContent) });
                }
            }
        }

        for (int i = 0; i < 3; i++)
        {
            String subDirName = String.format("/subdir%s", i);
            for (int j = 0; j < 3; j++)
            {
                String subsubDirName = subDirName + String.format("/subsubdir%s_%s", i, j);
                for (int k = 0; k < 5; k++)
                {
                    String testFileName = subsubDirName + String.format("/test_%s_%s_%s.txt", i, j, k);
                    byte[] testFileContent = String.format("TEST_FILE_CONTENT_TO_BE_OVERWRITTEN_%s_%s_%s", i, j, k).getBytes(StandardCharsets.UTF_8);
                    Chunk[] readChunk = afsClient.read(
                            new Chunk[] { new Chunk(owner, serverUploadDirectory + testFileName, 0L, testFileContent.length, new byte[0]) });
                    assertArrayEquals(testFileContent, readChunk[0].getData());
                }
            }
        }

        Path resourceDirectoryPath = Path.of(getClass().getClassLoader().getResource(UPLOAD_TEST_RESOURCE_DIRECTORY).getPath());
        IOUtils.list(resourceDirectoryPath.toString(), true).forEach(file ->
        {
            try
            {
                IOUtils.delete(file.getPath());
            } catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });

        for (int i = 0; i < 3; i++)
        {
            String subDirName = String.format("/subdir%s", i);
            IOUtils.createDirectory(resourceDirectoryPath.toAbsolutePath() + subDirName);
            for (int j = 0; j < 3; j++)
            {
                String subsubDirName = subDirName + String.format("/subsubdir%s_%s", i, j);
                IOUtils.createDirectory(resourceDirectoryPath.toAbsolutePath() + subsubDirName);
                for (int k = 0; k < 5; k++)
                {
                    String testFileName = subsubDirName + String.format("/test_%s_%s_%s.txt", i, j, k);
                    byte[] testFileContent;
                    if (k != 4)
                    {
                        testFileContent = String.format("TEST_FILE_CONTENT_%s_%s_%s", i, j, k).getBytes(StandardCharsets.UTF_8);
                    } else
                    {
                        testFileContent = new byte[0];
                    }
                    Path filePath = Path.of(resourceDirectoryPath.toAbsolutePath() + testFileName);
                    IOUtils.createFile(filePath.toAbsolutePath().toString());
                    IOUtils.write(filePath.toAbsolutePath().toString(), 0, testFileContent);
                    assertArrayEquals(testFileContent, Files.readAllBytes(filePath));
                }
            }
        }

        afsClient.upload(resourceDirectoryPath, owner, Path.of(serverUploadDirectory), ClientAPI.overrideCollisionListener,
                new ClientAPI.DefaultTransferMonitorLister());

        for (int i = 0; i < 3; i++)
        {
            String subDirName = String.format("/subdir%s", i);
            for (int j = 0; j < 3; j++)
            {
                String subsubDirName = subDirName + String.format("/subsubdir%s_%s", i, j);
                for (int k = 0; k < 5; k++)
                {
                    String testFileName = subsubDirName + String.format("/test_%s_%s_%s.txt", i, j, k);
                    byte[] testFileContent;
                    if (k != 4)
                    {
                        testFileContent = String.format("TEST_FILE_CONTENT_%s_%s_%s", i, j, k).getBytes(StandardCharsets.UTF_8);
                    } else
                    {
                        testFileContent = new byte[0];
                    }
                    Chunk[] readChunk = afsClient.read(
                            new Chunk[] { new Chunk(owner, serverUploadDirectory + testFileName, 0L, testFileContent.length, new byte[0]) });
                    assertArrayEquals(testFileContent, readChunk[0].getData());
                }
            }
        }
    }

    @Test
    public void upload_successfully_from_regular_file_to_dir() throws Exception
    {
        login();

        Path resourceDirectoryPath = Path.of(getClass().getClassLoader().getResource(UPLOAD_TEST_RESOURCE_DIRECTORY).getPath());
        IOUtils.list(resourceDirectoryPath.toString(), true).forEach(file ->
        {
            try
            {
                IOUtils.delete(file.getPath());
            } catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });

        String testFileName = String.format("/test%s.txt", 1);
        byte[] testFileContent = String.format("TEST_FILE_CONTENT_%s", 1).getBytes(StandardCharsets.UTF_8);
        Path filePath = Path.of(resourceDirectoryPath.toAbsolutePath() + testFileName);
        IOUtils.createFile(filePath.toAbsolutePath().toString());
        IOUtils.write(filePath.toAbsolutePath().toString(), 0, testFileContent);
        assertArrayEquals(testFileContent, Files.readAllBytes(filePath));

        String serverUploadDirectory = "/uploads";
        if (AfsClientUploadHelper.getServerFilePresence(afsClient, owner, serverUploadDirectory).isPresent())
        {
            afsClient.delete(owner, serverUploadDirectory, false);
        }
        afsClient.create(owner, serverUploadDirectory, true);

        afsClient.upload(filePath.toAbsolutePath(), owner, Path.of(serverUploadDirectory), ClientAPI.overrideCollisionListener,
                new ClientAPI.DefaultTransferMonitorLister());

        Chunk[] readChunk =
                afsClient.read(new Chunk[] { new Chunk(owner, serverUploadDirectory + testFileName, 0L, testFileContent.length, new byte[0]) });
        assertArrayEquals(testFileContent, readChunk[0].getData());
    }

    @Test
    public void upload_successfully_from_regular_file_to_regular_file() throws Exception
    {
        login();

        String serverUploadDirectory = "/uploads";
        if (AfsClientUploadHelper.getServerFilePresence(afsClient, owner, serverUploadDirectory).isPresent())
        {
            afsClient.delete(owner, serverUploadDirectory, false);
        }
        afsClient.create(owner, serverUploadDirectory, true);

        String testFileName = serverUploadDirectory + String.format("/test_%s.txt", 1);
        afsClient.create(owner, testFileName, false);
        byte[] testFileContentToBeOverwritten = String.format("TEST_FILE_CONTENT_TO_BE_OVERWRITTEN_%s", 1).getBytes(StandardCharsets.UTF_8);
        afsClient.write(new ch.ethz.sis.afsapi.dto.Chunk[] {
                new ch.ethz.sis.afsapi.dto.Chunk(owner, testFileName, 0L, testFileContentToBeOverwritten.length, testFileContentToBeOverwritten) });

        Chunk[] readChunk = afsClient.read(new Chunk[] { new Chunk(owner, testFileName, 0L, testFileContentToBeOverwritten.length, new byte[0]) });
        assertArrayEquals(testFileContentToBeOverwritten, readChunk[0].getData());

        Path resourceDirectoryPath = Path.of(getClass().getClassLoader().getResource(UPLOAD_TEST_RESOURCE_DIRECTORY).getPath());
        IOUtils.list(resourceDirectoryPath.toString(), true).forEach(file ->
        {
            try
            {
                IOUtils.delete(file.getPath());
            } catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });

        String newTestFileName = String.format("/test%s.txt", 2);
        byte[] newTestFileContent = String.format("TEST_FILE_CONTENT_%s", 2).getBytes(StandardCharsets.UTF_8);
        Path filePath = Path.of(resourceDirectoryPath.toAbsolutePath() + newTestFileName);
        IOUtils.createFile(filePath.toAbsolutePath().toString());
        IOUtils.write(filePath.toAbsolutePath().toString(), 0, newTestFileContent);
        assertArrayEquals(newTestFileContent, Files.readAllBytes(filePath));

        afsClient.upload(filePath.toAbsolutePath(), owner, Path.of(testFileName), ClientAPI.overrideCollisionListener,
                new ClientAPI.DefaultTransferMonitorLister());

        Chunk[] newReadChunk = afsClient.read(new Chunk[] { new Chunk(owner, testFileName, 0L, newTestFileContent.length, new byte[0]) });
        assertArrayEquals(newTestFileContent, newReadChunk[0].getData());
    }

    @Test(expected = IllegalArgumentException.class)
    public void upload_with_failure_from_dir_to_regular_file() throws Exception
    {
        login();

        String serverUploadDirectory = "/uploads";
        if (AfsClientUploadHelper.getServerFilePresence(afsClient, owner, serverUploadDirectory).isPresent())
        {
            afsClient.delete(owner, serverUploadDirectory, false);
        }
        afsClient.create(owner, serverUploadDirectory, true);

        String testFileName = serverUploadDirectory + String.format("/test_%s.txt", 1);
        afsClient.create(owner, testFileName, false);
        byte[] testFileContentToBeOverwritten = String.format("TEST_FILE_CONTENT_TO_BE_OVERWRITTEN_%s", 1).getBytes(StandardCharsets.UTF_8);
        afsClient.write(new ch.ethz.sis.afsapi.dto.Chunk[] {
                new ch.ethz.sis.afsapi.dto.Chunk(owner, testFileName, 0L, testFileContentToBeOverwritten.length, testFileContentToBeOverwritten) });

        Chunk[] readChunk = afsClient.read(new Chunk[] { new Chunk(owner, testFileName, 0L, testFileContentToBeOverwritten.length, new byte[0]) });
        assertArrayEquals(testFileContentToBeOverwritten, readChunk[0].getData());

        Path resourceDirectoryPath = Path.of(getClass().getClassLoader().getResource(UPLOAD_TEST_RESOURCE_DIRECTORY).getPath());
        IOUtils.list(resourceDirectoryPath.toString(), true).forEach(file ->
        {
            try
            {
                IOUtils.delete(file.getPath());
            } catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });

        afsClient.upload(resourceDirectoryPath, owner, Path.of(testFileName), ClientAPI.overrideCollisionListener,
                new ClientAPI.DefaultTransferMonitorLister());
    }

    @Test(expected = IllegalArgumentException.class)
    public void upload_with_failure_destination_path_not_absolute() throws Exception
    {
        login();

        Path resourceDirectoryPath = Path.of(getClass().getClassLoader().getResource(UPLOAD_TEST_RESOURCE_DIRECTORY).getPath());

        afsClient.upload(resourceDirectoryPath, owner, Path.of("RELATIVE_PATH.txt"), ClientAPI.overrideCollisionListener,
                new ClientAPI.DefaultTransferMonitorLister());
    }

    @Test(expected = IllegalArgumentException.class)
    public void upload_with_failure_destination_path_not_found() throws Exception
    {
        login();

        String serverUploadDirectory = "/uploads";
        if (AfsClientUploadHelper.getServerFilePresence(afsClient, owner, serverUploadDirectory).isPresent())
        {
            afsClient.delete(owner, serverUploadDirectory, false);
        }
        afsClient.create(owner, serverUploadDirectory, true);

        Path resourceDirectoryPath = Path.of(getClass().getClassLoader().getResource(UPLOAD_TEST_RESOURCE_DIRECTORY).getPath());
        IOUtils.list(resourceDirectoryPath.toString(), true).forEach(file ->
        {
            try
            {
                IOUtils.delete(file.getPath());
            } catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });

        String newTestFileName = String.format("/test%s.txt", 2);
        byte[] newTestFileContent = String.format("TEST_FILE_CONTENT_%s", 2).getBytes(StandardCharsets.UTF_8);
        Path filePath = Path.of(resourceDirectoryPath.toAbsolutePath() + newTestFileName);
        IOUtils.createFile(filePath.toAbsolutePath().toString());
        IOUtils.write(filePath.toAbsolutePath().toString(), 0, newTestFileContent);

        afsClient.upload(filePath.toAbsolutePath(), owner, Path.of("/NON-EXISTING_FILE"), ClientAPI.overrideCollisionListener,
                new ClientAPI.DefaultTransferMonitorLister());
    }

    @Test(expected = IllegalArgumentException.class)
    public void upload_with_failure_source_path_not_found() throws Exception
    {
        login();

        String serverUploadDirectory = "/uploads";
        if (AfsClientUploadHelper.getServerFilePresence(afsClient, owner, serverUploadDirectory).isPresent())
        {
            afsClient.delete(owner, serverUploadDirectory, false);
        }
        afsClient.create(owner, serverUploadDirectory, true);

        Path resourceDirectoryPath = Path.of(getClass().getClassLoader().getResource(UPLOAD_TEST_RESOURCE_DIRECTORY).getPath());

        afsClient.upload(Path.of("NON-EXISTING_LOCAL_PATH"), owner, Path.of("/uploads"), ClientAPI.overrideCollisionListener,
                new ClientAPI.DefaultTransferMonitorLister());
    }

    @Test
    public void upload_with_failure_from_dir_to_dir_for_concurrent_modifications() throws Exception
    {
        login();

        Path resourceDirectoryPath = Path.of(getClass().getClassLoader().getResource(UPLOAD_TEST_RESOURCE_DIRECTORY).getPath());
        IOUtils.list(resourceDirectoryPath.toString(), true).forEach(file ->
        {
            try
            {
                IOUtils.delete(file.getPath());
            } catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });

        for (int i = 0; i < 5; i++)
        {
            String testFileName = String.format("/test%s.txt", i);
            byte[] testFileContent = String.format("TEST_FILE_CONTENT_%s", i).getBytes(StandardCharsets.UTF_8);
            Path filePath = Path.of(resourceDirectoryPath.toAbsolutePath() + testFileName);
            IOUtils.createFile(filePath.toAbsolutePath().toString());
            IOUtils.write(filePath.toAbsolutePath().toString(), 0, testFileContent);
            assertArrayEquals(testFileContent, Files.readAllBytes(filePath));
        }

        String serverUploadDirectory = "/uploads";
        if (AfsClientUploadHelper.getServerFilePresence(afsClient, owner, serverUploadDirectory).isPresent())
        {
            afsClient.delete(owner, serverUploadDirectory, false);
        }
        afsClient.create(owner, serverUploadDirectory, true);

        ClientAPI.DefaultTransferMonitorLister sneakyContentChangingMonitor = new ClientAPI.DefaultTransferMonitorLister()
        {
            @Override
            public synchronized void start(Path from, Path to, long total)
            {
                super.start(from, to, total);
                try
                {
                    IOUtils.write(from.toAbsolutePath().toString(), 0, "CHANGED_FILE_CONTENT".getBytes(StandardCharsets.UTF_8));
                } catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
        };
        boolean result = afsClient.upload(resourceDirectoryPath, owner, Path.of(serverUploadDirectory), ClientAPI.overrideCollisionListener,
                sneakyContentChangingMonitor);
        Assert.assertFalse(result);
        Assert.assertTrue(sneakyContentChangingMonitor.getException() instanceof IllegalStateException);
        Assert.assertEquals("Incomplete upload", sneakyContentChangingMonitor.getException().getMessage());
    }

    @Test
    public void upload_successfully_with_file_names_starting_with_dot() throws Exception
    {
        login();

        Path resourceDirectoryPath = Path.of(getClass().getClassLoader().getResource(UPLOAD_TEST_RESOURCE_DIRECTORY).getPath());
        IOUtils.list(resourceDirectoryPath.toString(), true).forEach(file ->
        {
            try
            {
                IOUtils.delete(file.getPath());
            } catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });

        for (int i = 0; i < 5; i++)
        {
            String testFileName = String.format("/.test%s.txt", i);
            byte[] testFileContent = String.format("TEST_FILE_CONTENT_%s", i).getBytes(StandardCharsets.UTF_8);
            Path filePath = Path.of(resourceDirectoryPath.toAbsolutePath() + testFileName);
            IOUtils.createFile(filePath.toAbsolutePath().toString());
            IOUtils.write(filePath.toAbsolutePath().toString(), 0, testFileContent);
            assertArrayEquals(testFileContent, Files.readAllBytes(filePath));
        }
        for (int i = 0; i < 3; i++)
        {
            String subDirName = String.format("/.subdir%s", i);
            IOUtils.createDirectory(resourceDirectoryPath.toAbsolutePath() + subDirName);
            for (int j = 0; j < 3; j++)
            {
                String subsubDirName = subDirName + String.format("/.subsubdir%s_%s", i, j);
                IOUtils.createDirectory(resourceDirectoryPath.toAbsolutePath() + subsubDirName);
                for (int k = 0; k < 5; k++)
                {
                    String testFileName = subsubDirName + String.format("/.test_%s_%s_%s.txt", i, j, k);
                    if (k == 2)
                    {
                        testFileName = testFileName + TemporaryPathUtil.OPENBIS_TMP_SUFFIX;
                    }
                    byte[] testFileContent;
                    if (k != 4)
                    {
                        testFileContent = String.format("TEST_FILE_CONTENT_%s_%s_%s", i, j, k).getBytes(StandardCharsets.UTF_8);
                    } else
                    {
                        testFileContent = new byte[0];
                    }
                    Path filePath = Path.of(resourceDirectoryPath.toAbsolutePath() + testFileName);
                    IOUtils.createFile(filePath.toAbsolutePath().toString());
                    IOUtils.write(filePath.toAbsolutePath().toString(), 0, testFileContent);
                    assertArrayEquals(testFileContent, Files.readAllBytes(filePath));
                }
            }
        }

        int numberOfBigFiles = 3;
        byte[][] bigFileSha256s = new byte[numberOfBigFiles][];
        for (int i = 0; i < numberOfBigFiles; i++)
        {
            String subDirName = String.format("/.subdirwithbigfile%s", i);
            IOUtils.createDirectory(resourceDirectoryPath.toAbsolutePath() + subDirName);
            String testFileName = subDirName + String.format("/.bigfiletest_%s.txt", i);
            Path filePath = Path.of(resourceDirectoryPath.toAbsolutePath() + testFileName);
            IOUtils.createFile(filePath.toAbsolutePath().toString());
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            for (int j = 0; j < 100; j++)
            {
                byte[] content = new byte[1000];
                Arrays.fill(content, (byte) j);
                IOUtils.write(filePath.toAbsolutePath().toString(), j * 1000, content);
                sha256.update(content);
            }
            bigFileSha256s[i] = sha256.digest();
        }

        String serverUploadDirectory = "/uploads";
        if (AfsClientUploadHelper.getServerFilePresence(afsClient, owner, serverUploadDirectory).isPresent())
        {
            afsClient.delete(owner, serverUploadDirectory, false);
        }
        afsClient.create(owner, serverUploadDirectory, true);

        afsClient.upload(resourceDirectoryPath, owner, Path.of(serverUploadDirectory), ClientAPI.overrideCollisionListener,
                new ClientAPI.DefaultTransferMonitorLister());

        for (int i = 0; i < 5; i++)
        {
            String testFileName = String.format("/.test%s.txt", i);
            byte[] testFileContent = String.format("TEST_FILE_CONTENT_%s", i).getBytes(StandardCharsets.UTF_8);
            Chunk[] readChunk =
                    afsClient.read(new Chunk[] { new Chunk(owner, serverUploadDirectory + testFileName, 0L, testFileContent.length, new byte[0]) });
            assertArrayEquals(testFileContent, readChunk[0].getData());
        }
        for (int i = 0; i < 3; i++)
        {
            String subDirName = String.format("/.subdir%s", i);
            for (int j = 0; j < 3; j++)
            {
                String subsubDirName = subDirName + String.format("/.subsubdir%s_%s", i, j);
                for (int k = 0; k < 5; k++)
                {
                    if (k == 2)
                    {
                        String testFileName = subsubDirName + String.format("/.test_%s_%s_%s.txt", i, j, k) + TemporaryPathUtil.OPENBIS_TMP_SUFFIX;
                        Assert.assertTrue(
                                AfsClientUploadHelper.getServerFilePresence(afsClient, owner, serverUploadDirectory + testFileName).isEmpty());
                    } else if (k == 4)
                    {
                        String testFileName = subsubDirName + String.format("/.test_%s_%s_%s.txt", i, j, k);
                        byte[] testFileContent = new byte[0];
                        Chunk[] readChunk = afsClient.read(
                                new Chunk[] { new Chunk(owner, serverUploadDirectory + testFileName, 0L, testFileContent.length, new byte[0]) });
                        assertArrayEquals(testFileContent, readChunk[0].getData());
                    } else
                    {
                        String testFileName = subsubDirName + String.format("/.test_%s_%s_%s.txt", i, j, k);
                        byte[] testFileContent = String.format("TEST_FILE_CONTENT_%s_%s_%s", i, j, k).getBytes(StandardCharsets.UTF_8);
                        Chunk[] readChunk = afsClient.read(
                                new Chunk[] { new Chunk(owner, serverUploadDirectory + testFileName, 0L, testFileContent.length, new byte[0]) });
                        assertArrayEquals(testFileContent, readChunk[0].getData());
                    }
                }
            }
        }

        for (int i = 0; i < numberOfBigFiles; i++)
        {
            String subDirName = String.format("/.subdirwithbigfile%s", i);
            String testFileName = subDirName + String.format("/.bigfiletest_%s.txt", i);
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            Chunk[] readChunk;
            int j = 0;
            do
            {
                int offset = j * getMaxUsableChunkSize();
                readChunk = afsClient.read(new Chunk[] {
                        new Chunk(owner, serverUploadDirectory + testFileName, (long) offset, Integer.min(getMaxUsableChunkSize(), 100000 - offset),
                                new byte[0]) });
                sha256.update(readChunk[0].getData());
                j++;
            } while (readChunk[0].getOffset() + readChunk[0].getData().length < 100000);
            assertArrayEquals(bigFileSha256s[i], sha256.digest());
        }
    }

    @Test
    public void test_successful_checksums() throws Exception
    {
        login();

        List<String> serverComputedChecksums = new ArrayList<>();
        String serverMainDirectory = "/tobedownloaded";
        afsClient.create(owner, serverMainDirectory, true);
        for (int i = 0; i < 5; i++)
        {
            String testFileName = serverMainDirectory + String.format("/test%s.txt", i);
            afsClient.create(owner, testFileName, false);
            byte[] testFileContent = String.format("TEST_FILE_CONTENT_%s", i).getBytes(StandardCharsets.UTF_8);
            afsClient.write(new ch.ethz.sis.afsapi.dto.Chunk[] {
                    new ch.ethz.sis.afsapi.dto.Chunk(owner, testFileName, 0L, testFileContent.length, testFileContent) });
            serverComputedChecksums.add(afsClient.hash(owner, testFileName));
        }
        for (int i = 0; i < 3; i++)
        {
            String subDirName = serverMainDirectory + String.format("/subdir%s", i);
            afsClient.create(owner, subDirName, true);
            for (int j = 0; j < 3; j++)
            {
                String subsubDirName = subDirName + String.format("/subsubdir%s_%s", i, j);
                afsClient.create(owner, subsubDirName, true);
                for (int k = 0; k < 5; k++)
                {
                    String testFileName = subsubDirName + String.format("/test_%s_%s_%s.txt", i, j, k);
                    afsClient.create(owner, testFileName, false);
                    byte[] testFileContent;
                    if (k != 4)
                    {
                        testFileContent = String.format("TEST_FILE_CONTENT_%s_%s_%s", i, j, k).getBytes(StandardCharsets.UTF_8);
                    } else
                    {
                        testFileContent = new byte[0];
                    }
                    afsClient.write(new ch.ethz.sis.afsapi.dto.Chunk[] {
                            new ch.ethz.sis.afsapi.dto.Chunk(owner, testFileName, 0L, testFileContent.length, testFileContent) });
                    serverComputedChecksums.add(afsClient.hash(owner, testFileName));
                }
            }
        }

        int numberOfBigFiles = 3;
        byte[][] bigFileSha256s = new byte[numberOfBigFiles][];
        for (int i = 0; i < numberOfBigFiles; i++)
        {
            String subDirName = serverMainDirectory + String.format("/subdirwithbigfile%s", i);
            afsClient.create(owner, subDirName, true);
            String testFileName = subDirName + String.format("/bigfiletest_%s.txt", i);
            afsClient.create(owner, testFileName, false);
            int maxSize = 100000;
            long j = 0;
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            while (j + getMaxUsableChunkSize() < maxSize)
            {
                byte[] testFileContent = new byte[getMaxUsableChunkSize()];
                Arrays.fill(testFileContent, (byte) j);
                afsClient.write(new ch.ethz.sis.afsapi.dto.Chunk[] {
                        new ch.ethz.sis.afsapi.dto.Chunk(owner, testFileName, j, testFileContent.length, testFileContent) });
                messageDigest.update(testFileContent);
                j += getMaxUsableChunkSize();
            }
            serverComputedChecksums.add(afsClient.hash(owner, testFileName));
            bigFileSha256s[i] = messageDigest.digest();
        }

        Path resourceDirectoryPath = Path.of(getClass().getClassLoader().getResource(DOWNLOAD_TEST_RESOURCE_DIRECTORY).getPath());
        IOUtils.list(resourceDirectoryPath.toString(), true).forEach(file ->
        {
            try
            {
                IOUtils.delete(file.getPath());
            } catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });

        afsClient.download(owner, Path.of("/"), resourceDirectoryPath, ClientAPI.overrideCollisionListener,
                new ClientAPI.DefaultTransferMonitorLister());

        List<String> locallyComputedChecksums = new ArrayList<>();
        List<String> serverRecomputedChecksums = new ArrayList<>();
        for (int i = 0; i < 5; i++)
        {
            String testFileName = serverMainDirectory + String.format("/test%s.txt", i);
            Path filePath = Path.of(getClass().getClassLoader().getResource(DOWNLOAD_TEST_RESOURCE_DIRECTORY + testFileName).getPath());
            locallyComputedChecksums.add(DigestUtils.md5Hex(new FileInputStream(filePath.toFile())));
            serverRecomputedChecksums.add(afsClient.hash(owner, testFileName));
        }
        for (int i = 0; i < 3; i++)
        {
            String subDirName = serverMainDirectory + String.format("/subdir%s", i);
            for (int j = 0; j < 3; j++)
            {
                String subsubDirName = subDirName + String.format("/subsubdir%s_%s", i, j);
                for (int k = 0; k < 5; k++)
                {
                    String testFileName = subsubDirName + String.format("/test_%s_%s_%s.txt", i, j, k);
                    Path filePath = Path.of(getClass().getClassLoader().getResource(DOWNLOAD_TEST_RESOURCE_DIRECTORY + testFileName).getPath());
                    locallyComputedChecksums.add(DigestUtils.md5Hex(new FileInputStream(filePath.toFile())));
                    serverRecomputedChecksums.add(afsClient.hash(owner, testFileName));
                }
            }
        }
        for (int i = 0; i < numberOfBigFiles; i++)
        {
            String subDirName = serverMainDirectory + String.format("/subdirwithbigfile%s", i);
            String testFileName = subDirName + String.format("/bigfiletest_%s.txt", i);
            Path filePath = Path.of(getClass().getClassLoader().getResource(DOWNLOAD_TEST_RESOURCE_DIRECTORY + testFileName).getPath());
            locallyComputedChecksums.add(DigestUtils.md5Hex(new FileInputStream(filePath.toFile())));
            serverRecomputedChecksums.add(afsClient.hash(owner, testFileName));
        }

        Assert.assertEquals(locallyComputedChecksums, serverComputedChecksums);
        Assert.assertEquals(serverComputedChecksums, serverRecomputedChecksums);

        List<String> serverComputedNewChecksums = new ArrayList<>();
        for (int i = 0; i < 5; i++)
        {
            String testFileName = serverMainDirectory + String.format("/test%s.txt", i);
            byte[] testFileContent = String.format("new_TEST_FILE_CONTENT_%s", i).getBytes(StandardCharsets.UTF_8);
            afsClient.write(new ch.ethz.sis.afsapi.dto.Chunk[] {
                    new ch.ethz.sis.afsapi.dto.Chunk(owner, testFileName, 0L, testFileContent.length, testFileContent) });
            serverComputedNewChecksums.add(afsClient.hash(owner, testFileName));
        }
        for (int i = 0; i < 3; i++)
        {
            String subDirName = serverMainDirectory + String.format("/subdir%s", i);
            for (int j = 0; j < 3; j++)
            {
                String subsubDirName = subDirName + String.format("/subsubdir%s_%s", i, j);
                for (int k = 0; k < 5; k++)
                {
                    String testFileName = subsubDirName + String.format("/test_%s_%s_%s.txt", i, j, k);
                    byte[] testFileContent;
                    if (k != 4)
                    {
                        testFileContent = String.format("new_TEST_FILE_CONTENT_%s_%s_%s", i, j, k).getBytes(StandardCharsets.UTF_8);
                    } else
                    {
                        testFileContent = new byte[0];
                    }
                    afsClient.write(new ch.ethz.sis.afsapi.dto.Chunk[] {
                            new ch.ethz.sis.afsapi.dto.Chunk(owner, testFileName, 0L, testFileContent.length, testFileContent) });
                    serverComputedNewChecksums.add(afsClient.hash(owner, testFileName));
                }
            }
        }

        for (int i = 0; i < numberOfBigFiles; i++)
        {
            String subDirName = serverMainDirectory + String.format("/subdirwithbigfile%s", i);
            String testFileName = subDirName + String.format("/bigfiletest_%s.txt", i);
            int maxSize = 100000;
            long j = 0;
            while (j + getMaxUsableChunkSize() < maxSize)
            {
                byte[] testFileContent = new byte[getMaxUsableChunkSize()];
                Arrays.fill(testFileContent, (byte) (j + 1));
                afsClient.write(new ch.ethz.sis.afsapi.dto.Chunk[] {
                        new ch.ethz.sis.afsapi.dto.Chunk(owner, testFileName, j, testFileContent.length, testFileContent) });
                j += getMaxUsableChunkSize();
            }
            serverComputedNewChecksums.add(afsClient.hash(owner, testFileName));
        }

        afsClient.download(owner, Path.of("/"), resourceDirectoryPath, ClientAPI.overrideCollisionListener,
                new ClientAPI.DefaultTransferMonitorLister());

        List<String> locallyComputedNewChecksums = new ArrayList<>();
        List<String> serverRecomputedNewChecksums = new ArrayList<>();
        for (int i = 0; i < 5; i++)
        {
            String testFileName = serverMainDirectory + String.format("/test%s.txt", i);
            Path filePath = Path.of(getClass().getClassLoader().getResource(DOWNLOAD_TEST_RESOURCE_DIRECTORY + testFileName).getPath());
            locallyComputedNewChecksums.add(DigestUtils.md5Hex(new FileInputStream(filePath.toFile())));
            serverRecomputedNewChecksums.add(afsClient.hash(owner, testFileName));
        }
        for (int i = 0; i < 3; i++)
        {
            String subDirName = serverMainDirectory + String.format("/subdir%s", i);
            for (int j = 0; j < 3; j++)
            {
                String subsubDirName = subDirName + String.format("/subsubdir%s_%s", i, j);
                for (int k = 0; k < 5; k++)
                {
                    String testFileName = subsubDirName + String.format("/test_%s_%s_%s.txt", i, j, k);
                    Path filePath = Path.of(getClass().getClassLoader().getResource(DOWNLOAD_TEST_RESOURCE_DIRECTORY + testFileName).getPath());
                    locallyComputedNewChecksums.add(DigestUtils.md5Hex(new FileInputStream(filePath.toFile())));
                    serverRecomputedNewChecksums.add(afsClient.hash(owner, testFileName));
                }
            }
        }
        for (int i = 0; i < numberOfBigFiles; i++)
        {
            String subDirName = serverMainDirectory + String.format("/subdirwithbigfile%s", i);
            String testFileName = subDirName + String.format("/bigfiletest_%s.txt", i);
            Path filePath = Path.of(getClass().getClassLoader().getResource(DOWNLOAD_TEST_RESOURCE_DIRECTORY + testFileName).getPath());
            locallyComputedNewChecksums.add(DigestUtils.md5Hex(new FileInputStream(filePath.toFile())));
            serverRecomputedNewChecksums.add(afsClient.hash(owner, testFileName));
        }

        Assert.assertFalse(locallyComputedChecksums.equals(locallyComputedNewChecksums));
        Assert.assertEquals(locallyComputedNewChecksums, serverComputedNewChecksums);
        Assert.assertEquals(serverComputedNewChecksums, serverRecomputedNewChecksums);
    }

    @Test(expected = IllegalArgumentException.class)
    public void hash_with_failure_source_path_directory() throws Exception
    {
        login();

        String serverMainDirectory = "/tobedownloaded";
        afsClient.create(owner, serverMainDirectory, true);

        afsClient.hash(owner, serverMainDirectory);
    }

    @Test(expected = IllegalArgumentException.class)
    public void hash_with_failure_source_path_non_found() throws Exception
    {
        login();

        String serverMainDirectory = "/tobedownloaded";
        afsClient.create(owner, serverMainDirectory, true);

        afsClient.hash(owner, serverMainDirectory + "/non-existent-file");
    }

    @Test()
    public void preview_empty_for_non_image_file_extensions() throws Exception
    {
        login();

        String serverMainDirectory = "/tobedownloaded";
        afsClient.create(owner, serverMainDirectory, true);
        afsClient.create(owner, serverMainDirectory + "/example.txt", false);
        afsClient.write(owner, serverMainDirectory + "/example.txt", 0L, new byte[] { 'C', 'I', 'A', 'O', 1 });
        afsClient.create(owner, serverMainDirectory + "/example.json", false);
        afsClient.write(owner, serverMainDirectory + "/example.json", 0L, new byte[] { 'C', 'I', 'A', 'O', 2 });
        afsClient.create(owner, serverMainDirectory + "/example.csv", false);
        afsClient.write(owner, serverMainDirectory + "/example.csv", 0L, new byte[] { 'C', 'I', 'A', 'O', 3 });
        afsClient.create(owner, serverMainDirectory + "/example", false);
        afsClient.write(owner, serverMainDirectory + "/example", 0L, new byte[] { 'C', 'I', 'A', 'O', 4 });

        Assert.assertArrayEquals(new byte[0], afsClient.preview(owner, serverMainDirectory + "/example.txt"));
        Assert.assertArrayEquals(new byte[0], afsClient.preview(owner, serverMainDirectory + "/example.json"));
        Assert.assertArrayEquals(new byte[0], afsClient.preview(owner, serverMainDirectory + "/example.csv"));
        Assert.assertArrayEquals(new byte[0], afsClient.preview(owner, serverMainDirectory + "/example"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void preview_with_failure_source_path_directory() throws Exception
    {
        login();

        String serverMainDirectory = "/tobedownloaded";
        afsClient.create(owner, serverMainDirectory, true);

        afsClient.preview(owner, serverMainDirectory);
    }

    @Test(expected = IllegalArgumentException.class)
    public void preview_with_failure_source_path_non_found() throws Exception
    {
        login();

        String serverMainDirectory = "/tobedownloaded";
        afsClient.create(owner, serverMainDirectory, true);

        afsClient.preview(owner, serverMainDirectory + "/non-existent-file");
    }

    @Test()
    public void preview_success_source_path_image() throws Exception
    {
        for (String extension : List.of("tiff", "gif", "jpg", "jpeg", "png", "bmp"))
        {
            final URL resource = getClass().getClassLoader().getResource("ch/ethz/sis/afsserver/client/image_example." + extension);
            final java.io.File file = new java.io.File(resource.toURI());

            BufferedImage originalImage = ImageIO.read(file);
            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();
            long originalFileSize = Files.size(file.toPath());
            double originalRatio = ((double) originalWidth) / ((double) originalHeight);
            String originalContentType = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(Files.readAllBytes(file.toPath())));

            login();

            String serverMainDirectory = "/tobedownloaded";
            String imageServerFileName = serverMainDirectory + "/image_example." + extension;
            try
            {
                afsClient.create(owner, serverMainDirectory, true);
            } catch (Exception e)
            {
            }
            afsClient.upload(Path.of(resource.getPath()), owner, Path.of(serverMainDirectory), ClientAPI.overrideCollisionListener,
                    new ClientAPI.DefaultTransferMonitorLister());

            byte[] previewBytes = afsClient.preview(owner, imageServerFileName);
            BufferedImage previewImage = ImageIO.read(new ByteArrayInputStream(previewBytes));
            int previewWidth = previewImage.getWidth();
            int previewHeight = previewImage.getHeight();
            long previewFileSize = previewBytes.length;
            double previewRatio = ((double) previewWidth) / ((double) previewHeight);
            String previewContentType = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(previewBytes));

            Assert.assertEquals(extension.equals("bmp") ? null : ("image/" + (extension.equals("jpg") ? "jpeg" : extension)), originalContentType);
            Assert.assertEquals("image/jpeg", previewContentType);
            Assert.assertTrue(previewFileSize < originalFileSize);
            Assert.assertTrue(previewWidth <= originalWidth);
            Assert.assertTrue(previewHeight <= originalHeight);
            Assert.assertEquals(previewRatio, originalRatio, 0.01);

            byte[] repeatedPreviewBytes = afsClient.preview(owner, imageServerFileName);
            Assert.assertArrayEquals(previewBytes, repeatedPreviewBytes);
        }
    }

    @Test()
    public void preview_changes_when_source_content_is_modified() throws Exception
    {
        final URL resource = getClass().getClassLoader().getResource("ch/ethz/sis/afsserver/client/image_example.jpeg");
        final java.io.File file = new java.io.File(resource.toURI());

        BufferedImage originalImage = ImageIO.read(file);

        //Resize original image
        Image scaledImage = originalImage.getScaledInstance(10, 10, Image.SCALE_SMOOTH);
        BufferedImage resizedImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.drawImage(scaledImage, 0, 0, null);
        g2d.dispose();
        // Compress to JPEG with lower quality
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        writer.setOutput(ios);
        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed())
        {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.5f); // Quality: 0.0 (lowest) to 1.0 (highest)
        }
        writer.write(null, new IIOImage(resizedImage, null, null), param);
        writer.dispose();
        ios.close();

        byte[] changedOriginalImage = baos.toByteArray();
        String newName = "changed_example_image.jpeg";
        Path changedImagePath = file.toPath().getParent().resolve(newName);
        Files.write(changedImagePath, changedOriginalImage, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);

        login();

        String serverMainDirectory = "/tobedownloaded";
        String imageServerFileName = serverMainDirectory + "/image_example.jpeg";
        try
        {
            afsClient.create(owner, serverMainDirectory, true);
        } catch (Exception e)
        {
        }
        afsClient.upload(Path.of(resource.getPath()), owner, Path.of(serverMainDirectory), ClientAPI.overrideCollisionListener,
                new ClientAPI.DefaultTransferMonitorLister());

        byte[] previewBytes = afsClient.preview(owner, imageServerFileName);

        afsClient.upload(changedImagePath, owner, Path.of(serverMainDirectory).resolve("image_example.jpeg"), ClientAPI.overrideCollisionListener,
                new ClientAPI.DefaultTransferMonitorLister());

        byte[] newPreviewBytes = afsClient.preview(owner, imageServerFileName);

        Assert.assertFalse(Arrays.equals(previewBytes, newPreviewBytes));
    }

    @Test()
    public void preview_from_image_with_big_dimensions() throws Exception
    {
        final URL resource = getClass().getClassLoader().getResource("ch/ethz/sis/afsserver/client/image_example.jpeg");
        final java.io.File file = new java.io.File(resource.toURI());
        BufferedImage initialImageToBeStretched = ImageIO.read(file);

        //Resize original image
        int originalWidth = 3000;
        int originalHeight = 2500;
        Image scaledImage = initialImageToBeStretched.getScaledInstance(originalWidth, originalHeight, Image.SCALE_SMOOTH);
        BufferedImage resizedImage = new BufferedImage(originalWidth, originalHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.drawImage(scaledImage, 0, 0, null);
        g2d.dispose();
        // Compress to JPEG with lower quality
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        writer.setOutput(ios);
        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed())
        {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.5f); // Quality: 0.0 (lowest) to 1.0 (highest)
        }
        writer.write(null, new IIOImage(resizedImage, null, null), param);
        writer.dispose();
        ios.close();

        byte[] changedOriginalImage = baos.toByteArray();
        String newName = "stretched_image.jpeg";
        Path changedImagePath = file.toPath().getParent().resolve(newName);
        Files.write(changedImagePath, changedOriginalImage, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
        long originalFileSize = Files.size(changedImagePath);
        double originalRatio = ((double) originalWidth) / ((double) originalHeight);

        login();

        String serverMainDirectory = "/tobedownloaded";
        String imageServerFileName = serverMainDirectory + "/" + newName;
        try
        {
            afsClient.create(owner, serverMainDirectory, true);
        } catch (Exception e)
        {
        }
        afsClient.upload(changedImagePath, owner, Path.of(serverMainDirectory), ClientAPI.overrideCollisionListener,
                new ClientAPI.DefaultTransferMonitorLister());

        byte[] previewBytes = afsClient.preview(owner, imageServerFileName);

        BufferedImage previewImage = ImageIO.read(new ByteArrayInputStream(previewBytes));
        int previewWidth = previewImage.getWidth();
        int previewHeight = previewImage.getHeight();
        long previewFileSize = previewBytes.length;
        double previewRatio = ((double) previewWidth) / ((double) previewHeight);

        Assert.assertTrue(previewFileSize < originalFileSize);
        Assert.assertTrue(previewWidth <= 1980);
        Assert.assertTrue(previewHeight <= 1980);
        Assert.assertEquals(previewRatio, originalRatio, 0.01);
    }

    @Test()
    public void preview_from_image_file_with_wrong_content() throws Exception
    {
        byte[] wrongContent = "NOT_JPEG".getBytes(StandardCharsets.UTF_8);
        String newName = "wrong_image.jpeg";
        final URL resource = getClass().getClassLoader().getResource("ch/ethz/sis/afsserver/client/");
        final java.io.File file = new java.io.File(resource.toURI());
        Path wrongImagePath = file.toPath().resolve(newName);
        Files.write(wrongImagePath, wrongContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

        login();

        String serverMainDirectory = "/tobedownloaded";
        String imageServerFileName = serverMainDirectory + "/" + newName;
        try
        {
            afsClient.create(owner, serverMainDirectory, true);
        } catch (Exception e)
        {
        }
        afsClient.upload(wrongImagePath, owner, Path.of(serverMainDirectory), ClientAPI.overrideCollisionListener,
                new ClientAPI.DefaultTransferMonitorLister());

        byte[] previewBytes = afsClient.preview(owner, imageServerFileName);

        Assert.assertArrayEquals(new byte[0], previewBytes);
    }

    protected String login() throws Exception
    {
        return afsClient.login("test", "test");
    }

    protected void assertFileEquals(File actualFile, String expectedOwner, String expectedPath, String expectedName, Boolean expectedDirectory,
            Long expectedSize)
    {
        assertEquals(expectedOwner, actualFile.getOwner());
        assertEquals(expectedPath, actualFile.getPath());
        assertEquals(expectedName, actualFile.getName());
        assertEquals(expectedDirectory, actualFile.getDirectory());
        assertEquals(expectedSize, actualFile.getSize());
    }

    protected String printFiles(File[] files)
    {
        StringBuilder result = new StringBuilder();
        for (File actualFile : files)
        {
            result.append(actualFile.getPath());
            result.append(", ");
            result.append(actualFile.getDirectory() ? "FOLDER" : "FILE");
            result.append(", ");
            result.append(actualFile.getSize());
            result.append("\n");
        }
        return result.toString();
    }

    protected String printFiles(ch.ethz.sis.afs.api.dto.File[] files)
    {
        StringBuilder result = new StringBuilder();
        for (ch.ethz.sis.afs.api.dto.File actualFile : files)
        {
            result.append(actualFile.getPath());
            result.append(", ");
            result.append(actualFile.getDirectory() ? "FOLDER" : "FILE");
            result.append(", ");
            result.append(actualFile.getSize());
            result.append("\n");
        }
        return result.toString();
    }

    private String replaceSnapshots(String str)
    {
        return str.replaceAll("(.*\\.afs\\.snapshots/[^/]+/)\\d{4}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{3}", "$1<SNAPSHOT>");
    }

    protected abstract Configuration getServerConfiguration();

    protected int getMaxUsableChunkSize()
    {
        Configuration serverConfiguration = getServerConfiguration();
        if (serverConfiguration != null)
        {
            int httpMaxContentLength = serverConfiguration.getIntegerProperty(AtomicFileSystemServerParameter.httpMaxContentLength);
            int maxReadSize = serverConfiguration.getIntegerProperty(AtomicFileSystemServerParameter.maxReadSizeInBytes);

            return Integer.min(maxReadSize, httpMaxContentLength / 3 * 2);

        } else
        {
            return 0;
        }
    }

    private File[] listFilesFromAFS(AfsClient client, String owner, String source) throws Exception
    {
        File[] files = client.list(owner, source, true);
        Arrays.sort(files, Comparator.comparing(File::getPath));
        return files;
    }

    private ch.ethz.sis.afs.api.dto.File[] listFilesFromFS(String source) throws Exception
    {
        return IOUtils.list(source, true).stream()
                .sorted(Comparator.comparing(ch.ethz.sis.afs.api.dto.File::getPath))
                .map(file ->
                {
                    Path relativePath = Path.of(source).relativize(Path.of(file.getPath()));
                    return new ch.ethz.sis.afs.api.dto.File("/" + relativePath, file.getName(), file.getDirectory(), file.getSize(),
                            file.getLastModifiedTime(), file.getCreationTime(), file.getLastAccessTime());
                })
                .toArray(ch.ethz.sis.afs.api.dto.File[]::new);
    }

}
