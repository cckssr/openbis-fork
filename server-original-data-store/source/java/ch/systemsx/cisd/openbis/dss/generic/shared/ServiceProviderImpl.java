package ch.systemsx.cisd.openbis.dss.generic.shared;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;

public class ServiceProviderImpl implements IServiceProvider
{
    @Override public IOpenBISService getOpenBISService()
    {
        return ServiceProvider.getOpenBISService();
    }

    @Override public IConfigProvider getConfigProvider()
    {
        return ServiceProvider.getConfigProvider();
    }

    @Override public IShareIdManager getShareIdManager()
    {
        return ServiceProvider.getShareIdManager();
    }

    @Override public IDataSetDirectoryProvider getDataSetDirectoryProvider()
    {
        return ServiceProvider.getDataStoreService().getDataSetDirectoryProvider();
    }

    @Override public IDataSetDeleter getDataSetDeleter()
    {
        return ServiceProvider.getDataStoreService().getDataSetDeleter();
    }

    @Override public IApplicationServerApi getV3ApplicationService()
    {
        return ServiceProvider.getV3ApplicationService();
    }

    @Override public IIncomingShareIdProvider getIncomingShareIdProvider()
    {
        return ServiceProvider.getIncomingShareIdProvider();
    }
}
