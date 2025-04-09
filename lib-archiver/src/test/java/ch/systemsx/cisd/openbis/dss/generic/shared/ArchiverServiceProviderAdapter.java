package ch.systemsx.cisd.openbis.dss.generic.shared;

import java.util.Properties;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.systemsx.cisd.common.mail.IMailClient;

public class ArchiverServiceProviderAdapter implements IArchiverServiceProvider
{
    @Override public IConfigProvider getConfigProvider()
    {
        throw new UnsupportedOperationException();
    }

    @Override public IMailClient createEMailClient()
    {
        throw new UnsupportedOperationException();
    }

    @Override public IHierarchicalContentProvider getHierarchicalContentProvider()
    {
        throw new UnsupportedOperationException();
    }

    @Override public IDataSetDirectoryProvider getDataSetDirectoryProvider()
    {
        throw new UnsupportedOperationException();
    }

    @Override public IDataSetPathInfoProvider getDataSetPathInfoProvider()
    {
        throw new UnsupportedOperationException();
    }

    @Override public IPathInfoDataSourceProvider getPathInfoDataSourceProvider()
    {
        throw new UnsupportedOperationException();
    }

    @Override public IArchiverDataSourceProvider getArchiverDataSourceProvider()
    {
        return null;
    }

    @Override public IDataSetDeleter getDataSetDeleter()
    {
        throw new UnsupportedOperationException();
    }

    @Override public IShareIdManager getShareIdManager()
    {
        throw new UnsupportedOperationException();
    }

    @Override public IArchiverPlugin getArchiverPlugin()
    {
        throw new UnsupportedOperationException();
    }

    @Override public IArchiverTaskScheduler getArchiverTaskScheduler()
    {
        throw new UnsupportedOperationException();
    }

    @Override public Properties getArchiverProperties()
    {
        throw new UnsupportedOperationException();
    }

    @Override public IOpenBISService getOpenBISService()
    {
        throw new UnsupportedOperationException();
    }

    @Override public IApplicationServerApi getV3ApplicationService()
    {
        throw new UnsupportedOperationException();
    }

    @Override public IIncomingShareIdProvider getIncomingShareIdProvider()
    {
        throw new UnsupportedOperationException();
    }
}
