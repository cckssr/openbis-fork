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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import ch.ethz.sis.afs.manager.TransactionConnection;
import ch.ethz.sis.afsapi.dto.ExceptionReason;
import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afsapi.dto.FreeSpace;
import ch.ethz.sis.afsapi.exception.ThrowableReason;
import ch.ethz.sis.afsclient.client.AfsClient;
import ch.ethz.sis.afsserver.server.Server;
import ch.ethz.sis.shared.io.IOUtils;

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

    protected static final String FILE_B_NAME = "B.txt";

    protected static final String FILE_B = FILE_B_NAME;

    protected static final String FILE_BINARY_FOLDER_NAME = "test-folder";

    protected static final String FILE_BINARY_FOLDER = FILE_BINARY_FOLDER_NAME;

    protected static final String FILE_BINARY_SUBFOLDER_NAME = "test-subfolder";

    protected static final String FILE_BINARY_SUBFOLDER = FILE_BINARY_FOLDER_NAME + "/" + FILE_BINARY_SUBFOLDER_NAME;

    protected static final String FILE_BINARY_NAME = "test.png";

    protected static final String FILE_BINARY = FILE_BINARY_SUBFOLDER + "/" + FILE_BINARY_NAME;

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
        try (final FileInputStream fis = new FileInputStream(file)) {
            binaryData = fis.readAllBytes();
        }
        binarySize = (int) file.length();

        createTestDataFile(owner, FILE_A, DATA);
        createTestDataFile(owner, FILE_BINARY, binaryData);

        afsClient = new AfsClient(
                new URI("http", null, "localhost", httpServerPort, httpServerPath, null, null));
    }

    protected abstract String getTestDataFolder(String owner);

    public void createTestDataFile(String owner, String source, byte[] data) throws Exception {
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
        assertFileEquals(files[0], owner, "/" + FILE_A , FILE_A_NAME, false, (long) DATA.length);
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
        assertFileEquals(files[0], owner, "/" + FILE_A , FILE_A_NAME, false, (long) DATA.length);
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
        }catch(Exception e){
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
    public void resumeRead_getsDataFromTemporaryFile() throws Exception
    {
        login();

        afsClient.resumeRead(owner, FILE_A, Path.of(FILE_B), 0L);

        assertFilesEqual(IOUtils.getPath(testDataRoot, FILE_A), FILE_B);
    }

    private void assertFilesEqual(final String expectedFile, final String actualFile) throws IOException
    {
        byte[] expectedData = IOUtils.readFully(expectedFile);
        byte[] actualData = IOUtils.readFully(actualFile);
        assertArrayEquals(expectedData, actualData);
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
    public void resumeWrite_zeroOffset_createsFile() throws Exception
    {
        login();

        final Boolean result = afsClient.resumeWrite(owner, FILE_B, Path.of(IOUtils.getPath(testDataRoot, FILE_A)), 0L);
        assertTrue(result);

        assertFilesEqual(IOUtils.getPath(testDataRoot, FILE_A), FILE_B);

        Path.of(FILE_B).toFile().delete();
    }

    @Test
    public void resumeWrite_nonZeroOffset_doesNotCreateFile() throws Exception
    {
        login();

        final Long offset = 65L;
        final Boolean result = afsClient.resumeWrite(owner, FILE_B, Path.of(IOUtils.getPath(testDataRoot, FILE_A)), offset);
        assertTrue(result);

        assertFalse(Path.of(FILE_B).toFile().exists());
    }

    @Test
    public void delete_fileIsGone() throws Exception
    {
        login();

        Boolean deleted = afsClient.delete(owner, FILE_A);
        assertTrue(deleted);

        List<ch.ethz.sis.afs.api.dto.File> list = IOUtils.list(testDataRoot, true);
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
    public void move_fileIsRenamed() throws Exception
    {
        login();

        File[] filesBefore = afsClient.list(owner, "", true);
        assertEquals(4, filesBefore.length);

        Arrays.sort(filesBefore, Comparator.comparing(File::getPath));
        assertFileEquals(filesBefore[0], owner, "/" + FILE_A , FILE_A_NAME, false, (long) DATA.length);
        assertFileEquals(filesBefore[1], owner, "/" + FILE_BINARY_FOLDER, FILE_BINARY_FOLDER_NAME, true, null);
        assertFileEquals(filesBefore[2], owner, "/" + FILE_BINARY_SUBFOLDER, FILE_BINARY_SUBFOLDER_NAME, true, null);
        assertFileEquals(filesBefore[3], owner, "/" + FILE_BINARY, FILE_BINARY_NAME, false, (long) binaryData.length);

        Boolean result = afsClient.move(owner, FILE_A, owner, FILE_B);
        assertTrue(result);

        File[] filesAfter = afsClient.list(owner, "", true);
        Arrays.sort(filesAfter, Comparator.comparing(File::getPath));
        assertFileEquals(filesAfter[0], owner, "/" + FILE_B , FILE_B_NAME, false, (long) DATA.length);
        assertFileEquals(filesAfter[1], owner, "/" + FILE_BINARY_FOLDER, FILE_BINARY_FOLDER_NAME, true, null);
        assertFileEquals(filesAfter[2], owner, "/" + FILE_BINARY_SUBFOLDER, FILE_BINARY_SUBFOLDER_NAME, true, null);
        assertFileEquals(filesAfter[3], owner, "/" + FILE_BINARY, FILE_BINARY_NAME, false, (long) binaryData.length);

        byte[] testDataFile = IOUtils.readFully(IOUtils.getPath(testDataRoot, FILE_B));
        assertArrayEquals(DATA, testDataFile);
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

}
