package ch.systemsx.cisd.openbis.dss.generic.shared;

public class ShufflingServiceProvider implements IShufflingServiceProvider
{
    @Override public IOpenBISService getOpenBISService()
    {
        return ServiceProvider.getOpenBISService();
    }

    @Override public IShareIdManager getShareIdManager()
    {
        return ServiceProvider.getShareIdManager();
    }

    @Override public IConfigProvider getConfigProvider()
    {
        return ServiceProvider.getConfigProvider();
    }

    @Override public IHierarchicalContentProvider getHierarchicalContentProvider()
    {
        return ServiceProvider.getHierarchicalContentProvider();
    }

    @Override public IChecksumProvider getChecksumProvider()
    {
        return new HierarchicalContentChecksumProvider(getHierarchicalContentProvider());
    }
}
