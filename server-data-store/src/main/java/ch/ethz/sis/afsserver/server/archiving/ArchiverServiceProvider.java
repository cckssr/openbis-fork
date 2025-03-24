package ch.ethz.sis.afsserver.server.archiving;

import java.util.Properties;

import javax.sql.DataSource;

import ch.ethz.sis.afsserver.server.common.OpenBISConfiguration;
import ch.ethz.sis.afsserver.server.common.OpenBISFacade;
import ch.ethz.sis.afsserver.server.pathinfo.PathInfoDatabaseConfiguration;
import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.shared.startup.Configuration;
import ch.systemsx.cisd.common.mail.IMailClient;
import ch.systemsx.cisd.common.server.ISessionTokenProvider;
import ch.systemsx.cisd.common.spring.HttpInvokerUtils;
import ch.systemsx.cisd.openbis.dss.generic.server.DatabaseBasedDataSetPathInfoProvider;
import ch.systemsx.cisd.openbis.dss.generic.server.plugins.standard.archiver.IMultiDataSetArchiverDataSourceProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.DataSetDirectoryProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.HierarchicalContentProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IArchiverPlugin;
import ch.systemsx.cisd.openbis.dss.generic.shared.IArchiverServiceProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IArchiverTaskScheduler;
import ch.systemsx.cisd.openbis.dss.generic.shared.IConfigProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IDataSetDeleter;
import ch.systemsx.cisd.openbis.dss.generic.shared.IDataSetDirectoryProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IDataSetPathInfoProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IHierarchicalContentProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IOpenBISService;
import ch.systemsx.cisd.openbis.dss.generic.shared.IPathInfoDataSourceProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IShareIdManager;
import ch.systemsx.cisd.openbis.dss.generic.shared.content.ContentCache;
import ch.systemsx.cisd.openbis.dss.generic.shared.content.IContentCache;

public class ArchiverServiceProvider implements IArchiverServiceProvider
{

    private final Configuration configuration;

    private final OpenBISFacade openBISFacade;

    public ArchiverServiceProvider(Configuration configuration, OpenBISFacade openBISFacade)
    {
        this.configuration = configuration;
        this.openBISFacade = openBISFacade;
    }

    @Override public IConfigProvider getConfigProvider()
    {
        return new ConfigProvider(configuration);
    }

    @Override public IMailClient createEMailClient()
    {
        return null;
    }

    @Override public IHierarchicalContentProvider getHierarchicalContentProvider()
    {
        // TODO properties for content cache
        IContentCache contentCache = ContentCache.create(new Properties());
        ISessionTokenProvider sessionTokenProvider = new ISessionTokenProvider()
        {
            @Override public String getSessionToken()
            {
                return "";
            }
        };
        // TODO properties for content provider
        Properties properties = new Properties();

        return new HierarchicalContentProvider(getOpenBISService(), getShareIdManager(), getConfigProvider(), contentCache, sessionTokenProvider,
                properties);
    }

    @Override public IDataSetDirectoryProvider getDataSetDirectoryProvider()
    {
        return new DataSetDirectoryProvider(getConfigProvider().getStoreRoot(), getShareIdManager());
    }

    @Override public IDataSetPathInfoProvider getDataSetPathInfoProvider()
    {
        DataSource dataSource = PathInfoDatabaseConfiguration.getInstance(configuration).getDataSource();
        return new DatabaseBasedDataSetPathInfoProvider(dataSource);
    }

    @Override public IPathInfoDataSourceProvider getPathInfoDataSourceProvider()
    {
        return null;
    }

    @Override public IMultiDataSetArchiverDataSourceProvider getMultiDataSetArchiverDataSourceProvider()
    {
        return null;
    }

    @Override public IDataSetDeleter getDataSetDeleter()
    {
        return null;
    }

    @Override public IShareIdManager getShareIdManager()
    {
        return new ShareIdManager(configuration, openBISFacade);
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
        return new OpenBISService(openBISFacade);
    }

    @Override public IApplicationServerApi getV3ApplicationService()
    {
        OpenBISConfiguration openBISConfig = OpenBISConfiguration.getInstance(configuration);
        return HttpInvokerUtils.createServiceStub(IApplicationServerApi.class,
                openBISConfig.getOpenBISUrl() + "/openbis/openbis" + IApplicationServerApi.SERVICE_URL,
                openBISConfig.getOpenBISTimeout());
    }
}
