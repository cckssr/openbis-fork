/*
 * Copyright ETH 2022 - 2023 Zürich, Scientific IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.ethz.sis.afs.manager.operation;

import static ch.ethz.sis.shared.io.IOUtils.getPath;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.Test;

import ch.ethz.sis.afs.AFSEnvironment;
import ch.ethz.sis.afs.api.dto.File;
import ch.ethz.sis.afs.startup.AtomicFileSystemParameter;
import ch.ethz.sis.shared.io.IOUtils;

public class WriteTransactionOperationTest extends AbstractTransactionOperationTest
{

    @Override
    public void operation() throws Exception
    {
        write(FILE_B_PATH, 0, DATA);
    }

    @Test
    public void operation_write_succeed() throws Exception
    {
        begin();
        String realPath = OperationExecutor.getRealPath(getTransaction(), FILE_B_PATH);
        File beforeWrite = IOUtils.getFile(realPath);
        write(FILE_B_PATH, 0, DATA);
        File afterWrite = IOUtils.getFile(realPath);
        assertEquals(1, getTransaction().getOperations().size());
        assertEquals(beforeWrite, afterWrite);
        prepare();
        commit();
        File afterCommit = IOUtils.getFile(realPath);
        assertEquals(DATA.length, (long) afterCommit.getSize());
    }

    @Test(expected = RuntimeException.class)
    public void operation_writeDirectory_exception() throws Exception
    {
        begin();
        write(DIR_B_PATH, 0, DATA);
    }

    @Test
    public void operation_writeDirectoryThatWasDeletedAndReplacedWithFile_succeed() throws Exception
    {
        String baseDir = AFSEnvironment.getDefaultAFSConfig().getStringProperty(AtomicFileSystemParameter.storageRoot);

        Path folder = Path.of(getPath(baseDir, DIR_B_PATH));
        assertTrue(Files.exists(folder));
        assertTrue(Files.isDirectory(folder));

        begin();
        delete(DIR_B_PATH);
        write(DIR_B_PATH, 0, DATA);
        prepare();
        commit();

        Path file = Path.of(getPath(baseDir, DIR_B_PATH));
        assertTrue(Files.exists(file));
        assertFalse(Files.isDirectory(file));
        assertEquals(DATA.length, Files.size(file));
    }

    @Test
    public void operation_writeFileThatWasDeletedAndReplacedWithFolder_fail() throws Exception
    {
        String baseDir = AFSEnvironment.getDefaultAFSConfig().getStringProperty(AtomicFileSystemParameter.storageRoot);

        Path file = Path.of(getPath(baseDir, FILE_B_PATH));
        assertTrue(Files.exists(file));
        assertFalse(Files.isDirectory(file));

        begin();
        delete(FILE_B_PATH);
        create(FILE_B_PATH, true);

        try
        {
            write(FILE_B_PATH, 0, DATA);
            fail();
        } catch (Exception e)
        {
            assertError(e, "Path can't be operated by: Write - ./target/tests/storage/B/B.txt is a directory");
        }
    }

    @Test
    public void operation_writeTwice_succeed() throws Exception
    {
        begin();
        String realPath = OperationExecutor.getRealPath(getTransaction(), FILE_B_PATH);
        File beforeWrite = IOUtils.getFile(realPath);
        write(FILE_B_PATH, 0, DATA);
        write(FILE_B_PATH, DATA.length, DATA);
        File afterWrite = IOUtils.getFile(realPath);
        assertEquals(2, getTransaction().getOperations().size());
        assertEquals(beforeWrite, afterWrite);
        prepare();
        commit();
        File afterCommit = IOUtils.getFile(realPath);
        assertEquals(DATA.length * 2, (long) afterCommit.getSize());

        final byte[] data = IOUtils.readFully(realPath);
        assertTrue(Arrays.equals(data, 0, DATA.length, DATA, 0, DATA.length));
        assertTrue(Arrays.equals(data, DATA.length, 2 * DATA.length, DATA, 0, DATA.length));
    }

    @Test
    public void operation_writeEmpty_succeed() throws Exception
    {
        begin();
        String realPath = OperationExecutor.getRealPath(getTransaction(), FILE_B_PATH);
        File beforeWrite = IOUtils.getFile(realPath);
        byte[] empty = new byte[0];
        write(FILE_B_PATH, 0, empty);
        File afterWrite = IOUtils.getFile(realPath);
        assertEquals(1, getTransaction().getOperations().size());
        assertEquals(beforeWrite, afterWrite);
        prepare();
        commit();
        File afterCommit = IOUtils.getFile(realPath);
        assertEquals(empty.length, (long) afterCommit.getSize());
    }

    @Test
    public void operation_writeOver_succeed() throws Exception
    {
        begin();
        String realPath = OperationExecutor.getRealPath(getTransaction(), FILE_B_PATH);
        File beforeWrite = IOUtils.getFile(realPath);
        write(FILE_B_PATH, 4, DATA);
        File afterWrite = IOUtils.getFile(realPath);
        assertEquals(1, getTransaction().getOperations().size());
        assertEquals(beforeWrite.getPath(), afterWrite.getPath());
        assertEquals(beforeWrite.getName(), afterWrite.getName());
        assertEquals(beforeWrite.getDirectory(), afterWrite.getDirectory());
        assertEquals(beforeWrite.getSize(), afterWrite.getSize());
        prepare();
        commit();
        File afterCommit = IOUtils.getFile(realPath);
        assertEquals(4 + DATA.length, (long) afterCommit.getSize());
    }

    @Test
    public void operation_writeRandomly_succeed() throws Exception
    {
        begin();
        final String realPath = OperationExecutor.getRealPath(getTransaction(), FILE_B_PATH);
        write(FILE_B_PATH, 0, DATA);
        write(0, "12");
        write(2, "34");
        write(1, "B");
        write(3, "D");
        write(4, "56");
        prepare();
        commit();

        final File afterCommit = IOUtils.getFile(realPath);
        final long fileSize = afterCommit.getSize();
        assertEquals(6, fileSize);

        final byte[] dataRead = IOUtils.read(realPath, 0, 6);
        assertArrayEquals("1B3D56".getBytes(), dataRead);
    }

    private void write(final int offset, final String value) throws Exception
    {
        final byte[] data12 = value.getBytes();
        write(FILE_B_PATH, offset, data12);
    }

}
