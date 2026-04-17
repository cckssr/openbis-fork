package ch.ethz.sis.afs.manager.operation;

import static org.junit.Assert.fail;

import org.junit.Test;

public class SnapshotTransactionOperationTest extends AbstractTransactionOperationTest
{

    @Override
    public void operation() throws Exception
    {
        snapshot(FILE_A_PATH);
    }

    @Test
    public void snapshotFileIsAllowed() throws Exception
    {
        begin();
        snapshot(FILE_A_PATH);
        prepare();
        commit();
    }

    @Test
    public void snapshotDirectoryIsNotAllowed() throws Exception
    {
        begin();

        try
        {
            snapshot(DIR_A_PATH);
            fail();
        } catch (Exception e)
        {
            assertError(e, "Path can't be operated by: Snapshot - A is not a regular file");
        }
    }

}
