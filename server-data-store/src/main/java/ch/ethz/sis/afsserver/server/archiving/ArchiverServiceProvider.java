package ch.ethz.sis.afsserver.server.archiving;

import java.util.Properties;

import ch.ethz.sis.afsserver.server.common.ServiceProvider;
import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.systemsx.cisd.common.mail.IMailClient;
import ch.systemsx.cisd.openbis.dss.generic.shared.IArchiverDataSourceProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IArchiverPlugin;
import ch.systemsx.cisd.openbis.dss.generic.shared.IArchiverServiceProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IArchiverTaskScheduler;
import ch.systemsx.cisd.openbis.dss.generic.shared.IConfigProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IDataSetDeleter;
import ch.systemsx.cisd.openbis.dss.generic.shared.IDataSetDirectoryProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IDataSetPathInfoProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IHierarchicalContentProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IIncomingShareIdProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IOpenBISService;
import ch.systemsx.cisd.openbis.dss.generic.shared.IPathInfoDataSourceProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IShareIdManager;

public class ArchiverServiceProvider implements IArchiverServiceProvider
{

    private final ServiceProvider serviceProvider;

    public ArchiverServiceProvider(ServiceProvider serviceProvider)
    {
        this.serviceProvider = serviceProvider;
    }

    @Override public IConfigProvider getConfigProvider()
    {
        return serviceProvider.getConfigProvider();
    }

    @Override public IMailClient createEMailClient()
    {
        return serviceProvider.createEMailClient();
    }

    @Override public IHierarchicalContentProvider getHierarchicalContentProvider()
    {
        return serviceProvider.getHierarchicalContentProvider();
    }

    @Override public IDataSetDirectoryProvider getDataSetDirectoryProvider()
    {
        return serviceProvider.getDataSetDirectoryProvider();
    }

    @Override public IDataSetPathInfoProvider getDataSetPathInfoProvider()
    {
        return serviceProvider.getDataSetPathInfoProvider();
    }

    @Override public IPathInfoDataSourceProvider getPathInfoDataSourceProvider()
    {
        return serviceProvider.getPathInfoDataSourceProvider();
    }

    @Override public IArchiverDataSourceProvider getArchiverDataSourceProvider()
    {
        return serviceProvider.getArchiverDataSourceProvider();
    }

    @Override public IDataSetDeleter getDataSetDeleter()
    {
        return serviceProvider.getDataSetDeleter();
    }

    @Override public IShareIdManager getShareIdManager()
    {
        return serviceProvider.getShareIdManager();
    }

    @Override public IArchiverPlugin getArchiverPlugin()
    {
        return serviceProvider.getArchiverPlugin();
    }

    @Override public IArchiverTaskScheduler getArchiverTaskScheduler()
    {
        return serviceProvider.getArchiverTaskScheduler();
    }

    @Override public Properties getArchiverProperties()
    {
        return serviceProvider.getArchiverProperties();
    }

    @Override public IOpenBISService getOpenBISService()
    {
        return serviceProvider.getOpenBISService();
    }

    @Override public IApplicationServerApi getV3ApplicationService()
    {
        return serviceProvider.getV3ApplicationService();
    }

    @Override public IIncomingShareIdProvider getIncomingShareIdProvider()
    {
        return serviceProvider.getIncomingShareIdProvider();
    }
}
