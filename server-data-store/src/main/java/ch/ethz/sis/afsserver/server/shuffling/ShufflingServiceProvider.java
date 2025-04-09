package ch.ethz.sis.afsserver.server.shuffling;

import ch.ethz.sis.afsserver.server.common.ServiceProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.HierarchicalContentChecksumProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IChecksumProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IConfigProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IHierarchicalContentProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IIncomingShareIdProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IOpenBISService;
import ch.systemsx.cisd.openbis.dss.generic.shared.IShareIdManager;
import ch.systemsx.cisd.openbis.dss.generic.shared.IShufflingServiceProvider;

public class ShufflingServiceProvider implements IShufflingServiceProvider
{
    private final ServiceProvider serviceProvider;

    public ShufflingServiceProvider(final ServiceProvider serviceProvider)
    {
        this.serviceProvider = serviceProvider;
    }

    @Override public IOpenBISService getOpenBISService()
    {
        return serviceProvider.getOpenBISService();
    }

    @Override public IShareIdManager getShareIdManager()
    {
        return serviceProvider.getShareIdManager();
    }

    @Override public IConfigProvider getConfigProvider()
    {
        return serviceProvider.getConfigProvider();
    }

    @Override public IHierarchicalContentProvider getHierarchicalContentProvider()
    {
        return serviceProvider.getHierarchicalContentProvider();
    }

    @Override public IChecksumProvider getChecksumProvider()
    {
        return new HierarchicalContentChecksumProvider(getHierarchicalContentProvider());
    }

    @Override public IIncomingShareIdProvider getIncomingShareIdProvider()
    {
        return serviceProvider.getIncomingShareIdProvider();
    }

}
