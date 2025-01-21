package ch.ethz.sis.openbis.systemtests.shuffling;

import java.io.IOException;
import java.util.Properties;

import ch.ethz.sis.afsserver.server.common.ServiceProvider;
import ch.ethz.sis.afsserver.server.maintenance.IMaintenanceTask;
import ch.ethz.sis.afsserver.server.shuffling.DataSetMover;
import ch.ethz.sis.afsserver.server.shuffling.IChecksumProvider;
import ch.ethz.sis.afsserver.server.shuffling.IncomingShareIdProvider;
import ch.ethz.sis.afsserver.server.shuffling.SegmentedStoreShufflingTask;
import ch.ethz.sis.afsserver.server.shuffling.SimpleChecksumProvider;
import ch.systemsx.cisd.common.filesystem.SimpleFreeSpaceProvider;

public class TestSegmentedStoreShufflingTask implements IMaintenanceTask
{

    private static String pluginName;

    private static Properties properties;

    @Override public void setUp(final String pluginName, final Properties properties)
    {
        TestSegmentedStoreShufflingTask.pluginName = pluginName;
        TestSegmentedStoreShufflingTask.properties = properties;
    }

    @Override public void execute()
    {
        // do not run periodically - only on request with executeOnce
    }

    public static void executeOnce(IChecksumProvider checksumProvider)
    {
        SegmentedStoreShufflingTask segmentedStoreShufflingTask =
                new SegmentedStoreShufflingTask(IncomingShareIdProvider.getIdsOfIncomingShares(), ServiceProvider.getOpenBISService(),
                        new SimpleFreeSpaceProvider(), new DataSetMover(ServiceProvider.getOpenBISService(), ServiceProvider.getLockManager()),
                        checksumProvider, ServiceProvider.getConfigProvider());
        segmentedStoreShufflingTask.setUp(pluginName, properties);
        segmentedStoreShufflingTask.execute();
    }

    public static class TestChecksumProvider implements IChecksumProvider
    {

        private final TestChecksumAction action;

        public TestChecksumProvider()
        {
            action = (dataSetCode, relativePath) -> new SimpleChecksumProvider().getChecksum(dataSetCode, relativePath);
        }

        public TestChecksumProvider(TestChecksumAction action)
        {
            this.action = action;
        }

        @Override public long getChecksum(final String dataSetCode, final String relativePath) throws IOException
        {
            return action.execute(dataSetCode, relativePath);
        }

    }

    public interface TestChecksumAction
    {
        long execute(String dataSetCode, String relativePath) throws IOException;
    }

}
