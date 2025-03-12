package ch.ethz.sis.afsserver.server.archiving;

import java.util.Properties;

import ch.ethz.sis.afsserver.server.common.OpenBISFacade;
import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.systemsx.cisd.common.mail.IMailClient;
import ch.systemsx.cisd.openbis.dss.generic.shared.IArchiverPlugin;
import ch.systemsx.cisd.openbis.dss.generic.shared.IArchiverServiceProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IArchiverTaskScheduler;
import ch.systemsx.cisd.openbis.dss.generic.shared.IConfigProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IDataSetDeleter;
import ch.systemsx.cisd.openbis.dss.generic.shared.IDataSetDirectoryProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IDataSetPathInfoProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IDataSourceProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IHierarchicalContentProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IOpenBISService;
import ch.systemsx.cisd.openbis.dss.generic.shared.IPathInfoDataSourceProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IShareIdManager;

public class ArchiverServiceProvider implements IArchiverServiceProvider
{

    private OpenBISFacade facade;

    public ArchiverServiceProvider(OpenBISFacade facade)
    {
        this.facade = facade;
    }

    @Override public IConfigProvider getConfigProvider()
    {
        return null;
    }

    @Override public IMailClient createEMailClient()
    {
        return null;
    }

    @Override public IHierarchicalContentProvider getHierarchicalContentProvider()
    {
        return null;
    }

    @Override public IDataSetDirectoryProvider getDataSetDirectoryProvider()
    {
        return null;
    }

    @Override public IDataSetPathInfoProvider getDataSetPathInfoProvider()
    {
        return null;
    }

    @Override public IPathInfoDataSourceProvider getPathInfoDataSourceProvider()
    {
        return null;
    }

    @Override public IDataSourceProvider getDataSourceProvider()
    {
        return null;
    }

    @Override public IDataSetDeleter getDataSetDeleter()
    {
        return null;
    }

    @Override public IShareIdManager getShareIdManager()
    {
        return null;
    }

    @Override public IArchiverPlugin getArchiverPlugin()
    {
        return null;
    }

    @Override public IArchiverTaskScheduler getArchiverTaskScheduler()
    {
        return null;
    }

    @Override public Properties getArchiverProperties()
    {
        return null;
    }

    @Override public IOpenBISService getOpenBISService()
    {
        return new OpenBISService(facade);
    }

    @Override public IApplicationServerApi getV3ApplicationService()
    {
        return null;
    }
}
