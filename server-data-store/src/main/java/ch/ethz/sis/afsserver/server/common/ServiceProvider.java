package ch.ethz.sis.afsserver.server.common;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import ch.ethz.sis.afsserver.server.archiving.ArchiverConfiguration;
import ch.ethz.sis.afsserver.server.archiving.ArchiverDatabaseConfiguration;
import ch.ethz.sis.afsserver.server.archiving.IArchiverContextFactory;
import ch.ethz.sis.afsserver.server.pathinfo.PathInfoDatabaseConfiguration;
import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.shared.startup.Configuration;
import ch.systemsx.cisd.common.exceptions.NotImplementedException;
import ch.systemsx.cisd.common.mail.IMailClient;
import ch.systemsx.cisd.common.mail.MailClient;
import ch.systemsx.cisd.common.mail.MailClientParameters;
import ch.systemsx.cisd.common.server.ISessionTokenProvider;
import ch.systemsx.cisd.common.spring.HttpInvokerUtils;
import ch.systemsx.cisd.openbis.dss.generic.server.DatabaseBasedDataSetPathInfoProvider;
import ch.systemsx.cisd.openbis.dss.generic.server.DeletionCommand;
import ch.systemsx.cisd.openbis.dss.generic.shared.ArchiverTaskContext;
import ch.systemsx.cisd.openbis.dss.generic.shared.DataSetDirectoryProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.HierarchicalContentProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IArchiverDataSourceProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IArchiverPlugin;
import ch.systemsx.cisd.openbis.dss.generic.shared.IArchiverTask;
import ch.systemsx.cisd.openbis.dss.generic.shared.IArchiverTaskScheduler;
import ch.systemsx.cisd.openbis.dss.generic.shared.IConfigProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IDataSetDeleter;
import ch.systemsx.cisd.openbis.dss.generic.shared.IDataSetDirectoryProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IDataSetPathInfoProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IHierarchicalContentProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IOpenBISService;
import ch.systemsx.cisd.openbis.dss.generic.shared.IPathInfoDataSourceProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IServiceProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IShareIdManager;
import ch.systemsx.cisd.openbis.dss.generic.shared.api.v1.IDssService;
import ch.systemsx.cisd.openbis.dss.generic.shared.api.v1.IDssServiceFactory;
import ch.systemsx.cisd.openbis.dss.generic.shared.content.ContentCache;
import ch.systemsx.cisd.openbis.dss.generic.shared.content.IContentCache;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.IDatasetLocation;
import ch.systemsx.cisd.openbis.generic.shared.dto.DatasetDescription;
import lombok.Getter;

public class ServiceProvider implements IServiceProvider
{

    @Getter
    private static ServiceProvider instance;

    private final Configuration configuration;

    private IConfigProvider configProvider;

    private IMailClient mailClient;

    private IHierarchicalContentProvider hierarchicalContentProvider;

    private IDataSetDirectoryProvider dataSetDirectoryProvider;

    private IDataSetPathInfoProvider dataSetPathInfoProvider;

    private IPathInfoDataSourceProvider pathInfoDataSourceProvider;

    private IArchiverDataSourceProvider archiverDataSourceProvider;

    private IDataSetDeleter dataSetDeleter;

    private IShareIdManager shareIdManager;

    private IArchiverPlugin archiverPlugin;

    private IArchiverContextFactory archiverContextFactory;

    private IArchiverTaskScheduler archiverTaskScheduler;

    private Properties archiverProperties;

    private IOpenBISService openBISService;

    private IApplicationServerApi applicationServerApi;

    private IContentCache contentCache;

    private IDssServiceFactory dssServiceFactory;

    public static void configure(Configuration configuration)
    {
        ServiceProvider.instance = new ServiceProvider(configuration);
    }

    private ServiceProvider(Configuration configuration)
    {
        this.configuration = configuration;
    }

    public synchronized IConfigProvider getConfigProvider()
    {
        if (configProvider == null)
        {
            configProvider = new ConfigProvider(configuration);
        }

        return configProvider;
    }

    public synchronized IMailClient createEMailClient()
    {
        if (mailClient == null)
        {
            MailConfiguration mailConfiguration = MailConfiguration.getInstance(configuration);

            MailClientParameters parameters = new MailClientParameters();
            parameters.setFrom(mailConfiguration.getFrom());
            parameters.setSmtpHost(mailConfiguration.getSmtpHost());
            parameters.setSmtpUser(mailConfiguration.getSmtpUser());
            parameters.setSmtpPassword(mailConfiguration.getSmtpPassword());
            mailClient = new MailClient(parameters);
        }

        return mailClient;
    }

    public synchronized IHierarchicalContentProvider getHierarchicalContentProvider()
    {
        if (hierarchicalContentProvider == null)
        {
            ISessionTokenProvider sessionTokenProvider = new ISessionTokenProvider()
            {
                public String getSessionToken()
                {
                    return getOpenBISService().getSessionToken();
                }
            };

            hierarchicalContentProvider =
                    new HierarchicalContentProvider(getOpenBISService(), getShareIdManager(), getConfigProvider(), getContentCache(),
                            sessionTokenProvider, configuration.getProperties());
        }

        return hierarchicalContentProvider;
    }

    public synchronized IDataSetDirectoryProvider getDataSetDirectoryProvider()
    {
        if (dataSetDirectoryProvider == null)
        {
            dataSetDirectoryProvider = new DataSetDirectoryProvider(getConfigProvider().getStoreRoot(), getShareIdManager());
        }

        return dataSetDirectoryProvider;
    }

    public synchronized IDataSetPathInfoProvider getDataSetPathInfoProvider()
    {
        if (dataSetPathInfoProvider == null)
        {
            final DatabaseConfiguration pathInfoDatabaseConfiguration = PathInfoDatabaseConfiguration.getInstance(configuration);

            if (pathInfoDatabaseConfiguration != null)
            {
                dataSetPathInfoProvider = new DatabaseBasedDataSetPathInfoProvider(pathInfoDatabaseConfiguration::getDataSource);
            } else
            {
                throw new RuntimeException("Path info database not configured");
            }
        }

        return dataSetPathInfoProvider;
    }

    public synchronized IPathInfoDataSourceProvider getPathInfoDataSourceProvider()
    {
        if (pathInfoDataSourceProvider == null)
        {
            final DatabaseConfiguration pathInfoDatabaseConfiguration = PathInfoDatabaseConfiguration.getInstance(configuration);

            pathInfoDataSourceProvider = new IPathInfoDataSourceProvider()
            {
                public DataSource getDataSource()
                {
                    return pathInfoDatabaseConfiguration.getDataSource();
                }

                public boolean isDataSourceDefined()
                {
                    return pathInfoDatabaseConfiguration != null;
                }
            };
        }

        return pathInfoDataSourceProvider;
    }

    public synchronized IArchiverDataSourceProvider getArchiverDataSourceProvider()
    {
        if (archiverDataSourceProvider == null)
        {
            final DatabaseConfiguration archiverDatabaseConfiguration = ArchiverDatabaseConfiguration.getInstance(configuration);

            if (archiverDatabaseConfiguration != null)
            {
                archiverDataSourceProvider = new IArchiverDataSourceProvider()
                {
                    public DataSource getDataSource()
                    {
                        return archiverDatabaseConfiguration.getDataSource();
                    }
                };
            } else
            {
                throw new RuntimeException("Archiver database not configured");
            }
        }

        return archiverDataSourceProvider;
    }

    public synchronized IDataSetDeleter getDataSetDeleter()
    {
        if (dataSetDeleter == null)
        {
            dataSetDeleter = new IDataSetDeleter()
            {
                public void scheduleDeletionOfDataSets(final List<? extends IDatasetLocation> dataSets, final int maxNumberOfRetries,
                        final long waitingTimeBetweenRetries)
                {
                    new DeletionCommand(dataSets, maxNumberOfRetries, waitingTimeBetweenRetries).execute(getHierarchicalContentProvider(),
                            getDataSetDirectoryProvider());
                }
            };
        }

        return dataSetDeleter;
    }

    public synchronized IShareIdManager getShareIdManager()
    {
        if (shareIdManager == null)
        {
            OpenBISConfiguration openBISConfiguration = OpenBISConfiguration.getInstance(configuration);
            shareIdManager = new ShareIdManager(configuration, openBISConfiguration.getOpenBISFacade());
        }

        return shareIdManager;
    }

    public synchronized IArchiverPlugin getArchiverPlugin()
    {
        if (archiverPlugin == null)
        {
            ArchiverConfiguration archiverConfiguration = ArchiverConfiguration.getInstance(configuration);
            if (archiverConfiguration != null)
            {
                archiverPlugin = archiverConfiguration.getArchiverPlugin();
            }
        }

        return archiverPlugin;
    }

    public synchronized IArchiverTaskScheduler getArchiverTaskScheduler()
    {
        if (archiverTaskScheduler == null)
        {
            archiverTaskScheduler = new IArchiverTaskScheduler()
            {
                public void scheduleTask(final String taskKey, final IArchiverTask task, final Map<String, String> parameterBindings,
                        final List<DatasetDescription> datasets, final String userId, final String userEmailOrNull, final String userSessionToken)
                {
                    task.process(datasets, parameterBindings);
                }
            };
        }

        return archiverTaskScheduler;
    }

    public synchronized IArchiverContextFactory getArchiverContextFactory()
    {
        if (archiverContextFactory == null)
        {
            archiverContextFactory = new IArchiverContextFactory()
            {
                @Override public ArchiverTaskContext createContext()
                {
                    return new ArchiverTaskContext(getDataSetDirectoryProvider(), getHierarchicalContentProvider());
                }
            };
        }

        return archiverContextFactory;
    }

    public synchronized Properties getArchiverProperties()
    {
        if (archiverProperties == null)
        {
            ArchiverConfiguration archiverConfiguration = ArchiverConfiguration.getInstance(configuration);
            if (archiverConfiguration != null)
            {
                archiverProperties = archiverConfiguration.getProperties();
            }
        }

        return archiverProperties;
    }

    public synchronized IOpenBISService getOpenBISService()
    {
        if (openBISService == null)
        {
            OpenBISConfiguration openBISConfiguration = OpenBISConfiguration.getInstance(configuration);
            openBISService = new OpenBISService(openBISConfiguration.getOpenBISFacade(), getArchiverPlugin(), getArchiverContextFactory());
        }

        return openBISService;
    }

    public synchronized IApplicationServerApi getV3ApplicationService()
    {
        if (applicationServerApi == null)
        {
            OpenBISConfiguration openBISConfig = OpenBISConfiguration.getInstance(configuration);
            applicationServerApi = HttpInvokerUtils.createServiceStub(IApplicationServerApi.class,
                    openBISConfig.getOpenBISUrl() + "/openbis/openbis" + IApplicationServerApi.SERVICE_URL,
                    openBISConfig.getOpenBISTimeout());
        }

        return applicationServerApi;
    }

    public synchronized IDssServiceFactory getDssServiceFactory()
    {
        if (dssServiceFactory == null)
        {
            dssServiceFactory = new IDssServiceFactory()
            {
                @Override public IDssService getService(final String baseURL)
                {
                    // used only by RemoteHierarchicalContentNode which is not needed at AFS
                    throw new NotImplementedException();
                }
            };
        }

        return dssServiceFactory;
    }

    public synchronized IContentCache getContentCache()
    {
        if (contentCache == null)
        {
            contentCache = ContentCache.create(configuration.getProperties());
        }

        return contentCache;
    }
}
