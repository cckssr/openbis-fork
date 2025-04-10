package ch.ethz.sis.openbis.systemtests.shuffling;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;

import ch.systemsx.cisd.common.maintenance.IMaintenanceTask;
import ch.systemsx.cisd.etlserver.plugins.SegmentedStoreShufflingTask;
import ch.systemsx.cisd.openbis.dss.generic.shared.IChecksumProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IConfigProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IHierarchicalContentProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IIncomingShareIdProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IOpenBISService;
import ch.systemsx.cisd.openbis.dss.generic.shared.IShareIdManager;
import ch.systemsx.cisd.openbis.dss.generic.shared.IShufflingServiceProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.ShufflingServiceProviderAdapter;
import ch.systemsx.cisd.openbis.dss.generic.shared.ShufflingServiceProviderFactory;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.AbstractExternalData;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.PhysicalDataSet;

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
        IShufflingServiceProvider originalProvider = ShufflingServiceProviderFactory.getInstance();

        ShufflingServiceProviderFactory.setInstance(new ShufflingServiceProviderAdapter()
        {
            @Override public IOpenBISService getOpenBISService()
            {
                return originalProvider.getOpenBISService();
            }

            @Override public IShareIdManager getShareIdManager()
            {
                return originalProvider.getShareIdManager();
            }

            @Override public IConfigProvider getConfigProvider()
            {
                return originalProvider.getConfigProvider();
            }

            @Override public IHierarchicalContentProvider getHierarchicalContentProvider()
            {
                return originalProvider.getHierarchicalContentProvider();
            }

            @Override public IIncomingShareIdProvider getIncomingShareIdProvider()
            {
                return originalProvider.getIncomingShareIdProvider();
            }

            @Override public IChecksumProvider getChecksumProvider()
            {
                return checksumProvider;
            }
        });

        SegmentedStoreShufflingTask segmentedStoreShufflingTask = new SegmentedStoreShufflingTask();
        segmentedStoreShufflingTask.setUp(pluginName, properties);
        segmentedStoreShufflingTask.execute();

        ShufflingServiceProviderFactory.setInstance(originalProvider);
    }

    public static class TestChecksumProvider implements IChecksumProvider
    {

        @Override public long getChecksum(final String dataSetCode, final String relativePath) throws IOException
        {
            AbstractExternalData dataSet = ShufflingServiceProviderFactory.getInstance().getOpenBISService().tryGetDataSet(dataSetCode);

            if (dataSet instanceof PhysicalDataSet)
            {
                PhysicalDataSet physicalDataSet = (PhysicalDataSet) dataSet;
                File storeRoot = ShufflingServiceProviderFactory.getInstance().getConfigProvider().getStoreRoot();
                File shareFolder = new File(storeRoot, physicalDataSet.getDataSetShareId());
                File dataSetFolder = new File(shareFolder, physicalDataSet.getDataSetLocation());
                return (int) FileUtils.checksumCRC32(new File(dataSetFolder, relativePath));
            } else
            {
                return -1;
            }
        }

    }

}
