package ch.ethz.sis.afsserver.server.archiving;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import ch.ethz.sis.afsserver.server.common.DatabaseConfiguration;
import ch.ethz.sis.afsserver.server.common.OpenBISConfiguration;
import ch.ethz.sis.afsserver.server.common.OpenBISFacade;
import ch.ethz.sis.afsserver.server.pathinfo.PathInfoDatabaseConfiguration;
import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.shared.startup.Configuration;
import ch.systemsx.cisd.common.mail.IMailClient;
import ch.systemsx.cisd.common.server.ISessionTokenProvider;
import ch.systemsx.cisd.common.spring.HttpInvokerUtils;
import ch.systemsx.cisd.openbis.dss.generic.server.DatabaseBasedDataSetPathInfoProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.DataSetDirectoryProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.HierarchicalContentProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IArchiverDataSourceProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IArchiverPlugin;
import ch.systemsx.cisd.openbis.dss.generic.shared.IArchiverServiceProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IArchiverTask;
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
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.IDatasetLocation;
import ch.systemsx.cisd.openbis.dss.generic.server.DeletionCommand;
import ch.systemsx.cisd.openbis.generic.shared.dto.DatasetDescription;

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
        final ArchiverConfiguration archiverConfiguration = ArchiverConfiguration.getInstance(configuration);

        IContentCache contentCache = ContentCache.create(archiverConfiguration.getProperties());
        ISessionTokenProvider sessionTokenProvider = new ISessionTokenProvider()
        {
            @Override public String getSessionToken()
            {
                return getOpenBISService().getSessionToken();
            }
        };

        return new HierarchicalContentProvider(getOpenBISService(), getShareIdManager(), getConfigProvider(), contentCache, sessionTokenProvider,
                archiverConfiguration.getProperties());
    }

    @Override public IDataSetDirectoryProvider getDataSetDirectoryProvider()
    {
        return new DataSetDirectoryProvider(getConfigProvider().getStoreRoot(), getShareIdManager());
    }

    @Override public IDataSetPathInfoProvider getDataSetPathInfoProvider()
    {
        final DatabaseConfiguration pathInfoDatabaseConfiguration = PathInfoDatabaseConfiguration.getInstance(configuration);

        if (pathInfoDatabaseConfiguration != null)
        {
            return new DatabaseBasedDataSetPathInfoProvider(pathInfoDatabaseConfiguration.getDataSource());
        } else
        {
            throw new RuntimeException("Path info database not configured");
        }
    }

    @Override public IPathInfoDataSourceProvider getPathInfoDataSourceProvider()
    {
        final DatabaseConfiguration pathInfoDatabaseConfiguration = PathInfoDatabaseConfiguration.getInstance(configuration);

        return new IPathInfoDataSourceProvider()
        {
            @Override public DataSource getDataSource()
            {
                return pathInfoDatabaseConfiguration.getDataSource();
            }

            @Override public boolean isDataSourceDefined()
            {
                return pathInfoDatabaseConfiguration != null;
            }
        };
    }

    @Override public IArchiverDataSourceProvider getArchiverDataSourceProvider()
    {
        final DatabaseConfiguration archiverDatabaseConfiguration = ArchiverDatabaseConfiguration.getInstance(configuration);

        if (archiverDatabaseConfiguration != null)
        {
            return new IArchiverDataSourceProvider()
            {
                @Override public DataSource getDataSource()
                {
                    return archiverDatabaseConfiguration.getDataSource();
                }
            };
        } else
        {
            throw new RuntimeException("Archiver database not configured");
        }
    }

    @Override public IDataSetDeleter getDataSetDeleter()
    {
        return new IDataSetDeleter()
        {
            @Override public void scheduleDeletionOfDataSets(final List<? extends IDatasetLocation> dataSets, final int maxNumberOfRetries,
                    final long waitingTimeBetweenRetries)
            {
                new DeletionCommand(dataSets, maxNumberOfRetries, waitingTimeBetweenRetries).execute(getHierarchicalContentProvider(),
                        getDataSetDirectoryProvider());
            }
        };
    }

    @Override public IShareIdManager getShareIdManager()
    {
        return new ShareIdManager(configuration, openBISFacade);
    }

    @Override public IArchiverPlugin getArchiverPlugin()
    {
        ArchiverConfiguration archiverConfiguration = ArchiverConfiguration.getInstance(configuration);
        return archiverConfiguration.getArchiverPlugin();
    }

    @Override public IArchiverTaskScheduler getArchiverTaskScheduler()
    {
        return new IArchiverTaskScheduler()
        {
            @Override public void scheduleTask(final String taskKey, final IArchiverTask task, final Map<String, String> parameterBindings,
                    final List<DatasetDescription> datasets, final String userId, final String userEmailOrNull, final String userSessionToken)
            {
                task.process(datasets, parameterBindings);
            }
        };
    }

    @Override public Properties getArchiverProperties()
    {
        ArchiverConfiguration archiverConfiguration = ArchiverConfiguration.getInstance(configuration);
        return archiverConfiguration.getProperties();
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
