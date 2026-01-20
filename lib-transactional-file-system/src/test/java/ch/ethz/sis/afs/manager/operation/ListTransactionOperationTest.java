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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.nio.file.NoSuchFileException;
import java.util.UUID;

import org.junit.Test;

import ch.ethz.sis.afs.api.dto.File;
import ch.ethz.sis.shared.io.IOUtils;

public class ListTransactionOperationTest extends AbstractTransactionOperationTest
{

    public static final String DIR_C = "C";

    public static final String FILE_C = "C.txt";

    public static final String DIR_C_PATH = IOUtils.PATH_SEPARATOR + getPath(DIR_C);

    public static final String FILE_C_PATH = IOUtils.PATH_SEPARATOR + getPath(DIR_C, FILE_C);

    @Override
    public void operation() throws Exception
    {
        list(ROOT, false);
    }

    @Test
    public void operation_list_succeed() throws Exception
    {
        begin();
        File[] list = list(ROOT, false);
        assertEquals(2, list.length);
        assertEquals(0, getTransaction().getOperations().size());
    }

    @Test(expected = NoSuchFileException.class)
    public void operation_list_exception() throws Exception
    {
        begin();
        list(ROOT + UUID.randomUUID().toString(), false);
    }

    @Test
    public void operation_list_recursively_succeed() throws Exception
    {
        begin();
        File[] list = list(ROOT, true);
        assertEquals(6, list.length);
        assertEquals(0, getTransaction().getOperations().size());
    }

    @Test
    public void operation_list_directory_after_create_exception() throws Exception
    {
        begin();
        create(DIR_C_PATH, true);
        try
        {
            list(DIR_C_PATH, false);
            fail();
        } catch (Exception e)
        {
            assertError(e, "Path can't be read by: List - After been written: ./target/tests/storage/C");
        }
    }

    @Test
    public void operation_list_directory_after_delete_exception() throws Exception
    {
        begin();
        delete(DIR_B_PATH);
        try
        {
            list(DIR_BC_PATH, true);
            fail();
        } catch (Exception e)
        {
            assertError(e, "Path can't be operated by: List - After been deleted: ./target/tests/storage/B");
        }
    }

    @Test
    public void operation_list_file_after_create_exception() throws Exception
    {
        begin();
        create(FILE_C_PATH, false);
        try
        {
            list(FILE_C_PATH, false);
            fail();
        } catch (Exception e)
        {
            assertError(e, "Path can't be read by: List - After been written: ./target/tests/storage/C/C.txt");
        }
    }

    @Test
    public void operation_list_file_after_write_exception() throws Exception
    {
        begin();
        write(FILE_A_PATH, 0, DATA);
        try
        {
            list(FILE_A_PATH, false);
            fail();
        } catch (Exception e)
        {
            assertError(e, "Path can't be read by: List - After been written: ./target/tests/storage/A/A.txt");
        }
    }

    @Test
    public void operation_list_file_after_delete_exception() throws Exception
    {
        begin();
        delete(FILE_A_PATH);
        try
        {
            list(FILE_A_PATH, false);
            fail();
        } catch (Exception e)
        {
            assertError(e, "Path can't be operated by: List - After been deleted: ./target/tests/storage/A/A.txt");
        }
    }

}
