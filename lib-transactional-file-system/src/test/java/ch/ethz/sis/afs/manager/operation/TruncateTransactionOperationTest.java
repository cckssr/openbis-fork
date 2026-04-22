package ch.ethz.sis.afs.manager.operation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import ch.ethz.sis.afs.api.dto.File;
import ch.ethz.sis.shared.io.IOUtils;

public class TruncateTransactionOperationTest extends AbstractTransactionOperationTest
{

    @Override
    public void operation() throws Exception
    {
        truncate(FILE_A_PATH, 0);
    }

    @Test
    public void truncateFileIsAllowed() throws Exception
    {
        begin();

        String realPath = OperationExecutor.getRealPath(getTransaction(), FILE_A_PATH);

        File beforeTruncate = IOUtils.getFile(realPath);
        assertEquals(Long.valueOf(DATA.length), beforeTruncate.getSize());

        truncate(FILE_A_PATH, 0);

        File afterTruncate = IOUtils.getFile(realPath);
        assertEquals(Long.valueOf(DATA.length), afterTruncate.getSize());

        prepare();
        commit();

        File afterCommit = IOUtils.getFile(realPath);
        assertEquals(Long.valueOf(0), afterCommit.getSize());
    }

    @Test
    public void truncateDirectoryIsNotAllowed() throws Exception
    {
        begin();

        try
        {
            truncate(DIR_A_PATH, 0);
            fail();
        } catch (Exception e)
        {
            assertError(e, "Path can't be operated by: Truncate - A is not a regular file");
        }
    }

    @Test
    public void truncateWithNegativeSizeParameterFails() throws Exception
    {
        begin();

        try
        {
            truncate(FILE_A_PATH, -1);
            fail();
        } catch (Exception e)
        {
            assertError(e, "Parameter of operation Truncate is invalid. Size cannot be < 0");
        }
    }

    @Test
    public void truncateWithSizeParameterGreaterThanFileSizeDoesNothing() throws Exception
    {
        begin();

        String realPath = OperationExecutor.getRealPath(getTransaction(), FILE_A_PATH);
        File before = IOUtils.getFile(realPath);

        truncate(FILE_A_PATH, before.getSize() + 1);

        prepare();
        commit();

        File after = IOUtils.getFile(realPath);
        assertEquals(before.getSize(), after.getSize());
    }
}
