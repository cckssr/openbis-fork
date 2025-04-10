package ch.systemsx.cisd.openbis.dss.generic.shared;

public class ShufflingServiceProviderAdapter implements IShufflingServiceProvider
{
    @Override public IOpenBISService getOpenBISService()
    {
        throw new UnsupportedOperationException();
    }

    @Override public IShareIdManager getShareIdManager()
    {
        throw new UnsupportedOperationException();
    }

    @Override public IConfigProvider getConfigProvider()
    {
        throw new UnsupportedOperationException();
    }

    @Override public IHierarchicalContentProvider getHierarchicalContentProvider()
    {
        throw new UnsupportedOperationException();
    }

    @Override public IChecksumProvider getChecksumProvider()
    {
        throw new UnsupportedOperationException();
    }

    @Override public IIncomingShareIdProvider getIncomingShareIdProvider()
    {
        throw new UnsupportedOperationException();
    }
}
