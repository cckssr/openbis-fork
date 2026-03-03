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

package ch.ethz.sis.afs.manager.operation;

import static ch.ethz.sis.shared.io.IOUtils.createDirectory;
import static ch.ethz.sis.shared.io.IOUtils.createFile;
import static ch.ethz.sis.shared.io.IOUtils.getPath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

import ch.ethz.sis.afs.AFSEnvironment;
import ch.ethz.sis.afs.api.dto.File;
import ch.ethz.sis.afs.startup.AtomicFileSystemParameter;
import ch.ethz.sis.shared.io.IOUtils;

public class CreateTransactionOperationTest extends AbstractTransactionOperationTest
{

    public static final String DIR_C = "C";

    public static final String FILE_C = "C.txt";

    public static final String FILE_INVALID_C = "D*.txt";

    public static final String DIR_C_PATH = IOUtils.PATH_SEPARATOR + getPath(DIR_C);

    public static final String FILE_C_PATH = IOUtils.PATH_SEPARATOR + getPath(DIR_C, FILE_C);

    public static final String FILE_INVALID_C_PATH = IOUtils.PATH_SEPARATOR + getPath(DIR_C, FILE_INVALID_C);

    @Override
    public void operation() throws Exception
    {
        create(FILE_C_PATH, false);
    }

    @Test
    public void operation_createFile_succeed() throws Exception
    {
        begin();
        final String realPathC = OperationExecutor.getRealPath(getTransaction(), FILE_C_PATH);
        create(FILE_C_PATH, false);
        assertEquals(1, getTransaction().getOperations().size());
        assertFalse(IOUtils.exists(realPathC));
        prepare();
        commit();
        assertTrue(IOUtils.exists(realPathC));

        final File file = IOUtils.getFile(realPathC);
        assertFalse(file.getDirectory());
    }

    @Test
    public void operation_createFileThatExistedBeforeTransaction_fail() throws Exception
    {
        begin();
        try
        {
            create(FILE_A_PATH, false);
            fail();
        } catch (Exception e)
        {
            assertError(e, "Path given to: Create - In store: ./target/tests/storage/A/A.txt");
        }
    }

    @Test
    public void operation_createFileThatExistedBeforeTransactionButWasLaterDeleted_succeed() throws Exception
    {
        begin();
        final String realPathA = OperationExecutor.getRealPath(getTransaction(), FILE_A_PATH);
        delete(FILE_A_PATH, false);
        create(FILE_A_PATH, false);
        prepare();
        commit();
        assertTrue(IOUtils.exists(realPathA));

        final File file = IOUtils.getFile(realPathA);
        assertFalse(file.getDirectory());
    }

    @Test
    public void operation_createFileThatExistedBeforeTransactionAsFolderButWasLaterDeleted_succeed() throws Exception
    {
        String baseDir = AFSEnvironment.getDefaultAFSConfig().getStringProperty(AtomicFileSystemParameter.storageRoot);
        createDirectory(getPath(baseDir, DIR_C));

        final Path folder = Path.of(getPath(baseDir, DIR_C));
        assertTrue(Files.exists(folder));
        assertTrue(Files.isDirectory(folder));

        begin();
        delete(IOUtils.PATH_SEPARATOR + DIR_C, false);
        create(IOUtils.PATH_SEPARATOR + DIR_C, false);
        prepare();
        commit();

        final Path file = Path.of(getPath(baseDir, DIR_C));
        assertTrue(Files.exists(file));
        assertFalse(Files.isDirectory(file));
    }

    @Test
    public void operation_createFileThatWasAlreadyCreatedInTransaction_fail() throws Exception
    {
        begin();
        create(FILE_C_PATH, false);

        try
        {
            create(FILE_C_PATH, false);
            fail();
        } catch (Exception e)
        {
            assertError(e, "Path given to: Create - In store: ./target/tests/storage/C/C.txt");
        }
    }

    @Test
    public void operation_createDirectory_succeed() throws Exception
    {
        begin();
        final String realPathC = OperationExecutor.getRealPath(getTransaction(), DIR_C_PATH);
        create(DIR_C_PATH, true);
        assertEquals(1, getTransaction().getOperations().size());
        assertFalse(IOUtils.exists(realPathC));
        prepare();
        commit();
        assertTrue(IOUtils.exists(realPathC));

        final File file = IOUtils.getFile(realPathC);
        assertTrue(file.getDirectory());
    }

    @Test
    public void operation_createDirectoryThatExistedBeforeTransaction_fail() throws Exception
    {
        begin();
        try
        {
            create(DIR_A_PATH, true);
            fail();
        } catch (Exception e)
        {
            assertError(e, "Path given to: Create - In store: ./target/tests/storage/A");
        }
    }

    @Test
    public void operation_createDirectoryThatExistedBeforeTransactionButWasLaterDeleted_succeed() throws Exception
    {
        begin();
        final String realPathA = OperationExecutor.getRealPath(getTransaction(), DIR_A_PATH);
        delete(DIR_A_PATH, false);
        create(DIR_A_PATH, true);
        prepare();
        commit();
        assertTrue(IOUtils.exists(realPathA));

        final File file = IOUtils.getFile(realPathA);
        assertTrue(file.getDirectory());
    }

    @Test
    public void operation_createDirectoryThatExistedBeforeTransactionAsFileButWasLaterDeleted_succeed() throws Exception
    {
        String baseDir = AFSEnvironment.getDefaultAFSConfig().getStringProperty(AtomicFileSystemParameter.storageRoot);
        createFile(getPath(baseDir, FILE_C));

        final Path file = Path.of(getPath(baseDir, FILE_C));
        assertTrue(Files.exists(file));
        assertFalse(Files.isDirectory(file));

        begin();
        delete(IOUtils.PATH_SEPARATOR + FILE_C, false);
        create(IOUtils.PATH_SEPARATOR + FILE_C, true);
        prepare();
        commit();

        final Path folder = Path.of(getPath(baseDir, FILE_C));
        assertTrue(Files.exists(folder));
        assertTrue(Files.isDirectory(folder));
    }

    @Test
    public void operation_createDirectoryThatWasAlreadyCreatedInTransaction_fail() throws Exception
    {
        begin();
        create(DIR_C_PATH, true);

        try
        {
            create(DIR_C_PATH, true);
            fail();
        } catch (Exception e)
        {
            assertError(e, "Path given to: Create - In store: ./target/tests/storage/C");
        }
    }

    @Test(expected = RuntimeException.class)
    public void operation_createFile_fail() throws Exception
    {
        begin();
        try
        {
            create(FILE_INVALID_C_PATH, false);
        } finally
        {
            rollback();
        }

    }

}
