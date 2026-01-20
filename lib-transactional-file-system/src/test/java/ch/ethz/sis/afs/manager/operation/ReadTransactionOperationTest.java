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
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;

import ch.ethz.sis.shared.io.IOUtils;

public class ReadTransactionOperationTest extends AbstractTransactionOperationTest
{

    public static final String DIR_C = "C";

    public static final String FILE_C = "C.txt";

    public static final String DIR_C_PATH = IOUtils.PATH_SEPARATOR + getPath(DIR_C);

    public static final String FILE_C_PATH = IOUtils.PATH_SEPARATOR + getPath(DIR_C, FILE_C);

    @Override
    public void operation() throws Exception
    {
        read(FILE_A_PATH, 0, DATA.length);
    }

    @Test
    public void operation_read_succeed() throws Exception
    {
        begin();
        byte[] data = read(FILE_A_PATH, 0, DATA.length);
        assertArrayEquals(data, DATA);
        assertEquals(0, getTransaction().getOperations().size());
    }

    @Test(expected = RuntimeException.class)
    public void operation_readDirectory_exception() throws Exception
    {
        begin();
        byte[] data = read(DIR_A_PATH, 0, DATA.length);
    }

    @Test
    public void operation_read0_succeed() throws Exception
    {
        begin();
        byte[] empty = new byte[0];
        byte[] data = read(FILE_A_PATH, 0, empty.length);
        assertArrayEquals(data, empty);
        assertEquals(0, getTransaction().getOperations().size());
    }

    @Test
    public void operation_readEmpty_succeed() throws Exception
    {
        begin();
        byte[] empty = new byte[0];
        byte[] data = read(FILE_B_PATH, 0, empty.length);
        assertArrayEquals(data, empty);
        assertEquals(0, getTransaction().getOperations().size());
    }

    @Test(expected = IOException.class)
    public void operation_readOver_exception() throws Exception
    {
        begin();
        read(FILE_B_PATH, 0, 1);
    }

    @Test
    public void operation_read_after_create_exception() throws Exception
    {
        begin();
        create(FILE_C_PATH, false);
        try
        {
            read(FILE_C_PATH, 0, DATA.length);
            fail();
        } catch (Exception e)
        {
            assertError(e, "Path can't be read by: Read - After been written: ./target/tests/storage/C/C.txt");
        }
    }

    @Test
    public void operation_read_after_write_exception() throws Exception
    {
        begin();
        write(FILE_A_PATH, 0, DATA);
        try
        {
            read(FILE_A_PATH, 0, DATA.length);
            fail();
        } catch (Exception e)
        {
            assertError(e, "Path can't be read by: Read - After been written: ./target/tests/storage/A/A.txt");
        }
    }

    @Test
    public void operation_read_after_delete_exception() throws Exception
    {
        begin();
        delete(FILE_A_PATH);
        try
        {
            read(FILE_A_PATH, 0, DATA.length);
            fail();
        } catch (Exception e)
        {
            assertError(e, "Path can't be operated by: Read - After been deleted: ./target/tests/storage/A/A.txt");
        }
    }
}
