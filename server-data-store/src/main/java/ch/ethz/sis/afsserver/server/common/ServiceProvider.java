package ch.ethz.sis.afsserver.server.common;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.sql.DataSource;

import ch.ethz.sis.afs.manager.TransactionManager;
import ch.ethz.sis.afsjson.JsonObjectMapper;
import ch.ethz.sis.afsserver.server.archiving.ArchiverConfiguration;
import ch.ethz.sis.afsserver.server.archiving.ArchiverDatabaseConfiguration;
import ch.ethz.sis.afsserver.server.archiving.IArchiverContextFactory;
import ch.ethz.sis.afsserver.server.archiving.messages.FinalizeDataSetArchivingMessage;
import ch.ethz.sis.afsserver.server.archiving.messages.UpdateDataSetArchivingStatusMessage;
import ch.ethz.sis.afsserver.server.messages.DeleteDataSetFromStoreMessage;
import ch.ethz.sis.afsserver.server.messages.DeleteFileMessage;
import ch.ethz.sis.afsserver.server.messages.MessagesDatabaseConfiguration;
import ch.ethz.sis.afsserver.server.messages.MessagesDatabaseFacade;
import ch.ethz.sis.afsserver.server.pathinfo.PathInfoDatabaseConfiguration;
import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameter;
import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameterUtil;
import ch.ethz.sis.afsserver.worker.ConnectionFactory;
import ch.ethz.sis.messages.process.MessageProcessId;
import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.shared.startup.Configuration;
import ch.systemsx.cisd.common.exceptions.NotImplementedException;
import ch.systemsx.cisd.common.mail.IMailClient;
import ch.systemsx.cisd.common.mail.MailClient;
import ch.systemsx.cisd.common.mail.MailClientParameters;
import ch.systemsx.cisd.common.properties.PropertyUtils;
import ch.systemsx.cisd.common.server.ISessionTokenProvider;
import ch.systemsx.cisd.common.spring.HttpInvokerUtils;
import ch.systemsx.cisd.common.time.DateTimeUtils;
import ch.systemsx.cisd.openbis.dss.generic.server.DatabaseBasedDataSetPathInfoProvider;
import ch.systemsx.cisd.openbis.dss.generic.server.plugins.standard.archiver.AbstractMultiDataSetArchiveCleaner;
import ch.systemsx.cisd.openbis.dss.generic.server.plugins.standard.archiver.FileDeleter;
import ch.systemsx.cisd.openbis.dss.generic.server.plugins.standard.archiver.IMultiDataSetArchiveCleaner;
import ch.systemsx.cisd.openbis.dss.generic.server.plugins.standard.archiver.MultiDataSetArchivingFinalizer;
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
import ch.systemsx.cisd.openbis.dss.generic.shared.IDataSetStatusUpdater;
import ch.systemsx.cisd.openbis.dss.generic.shared.IHierarchicalContentProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IIncomingShareIdProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IOpenBISService;
import ch.systemsx.cisd.openbis.dss.generic.shared.IPathInfoDataSourceProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IServiceProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IShareIdManager;
import ch.systemsx.cisd.openbis.dss.generic.shared.IncomingShareIdProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.api.v1.IDssService;
import ch.systemsx.cisd.openbis.dss.generic.shared.api.v1.IDssServiceFactory;
import ch.systemsx.cisd.openbis.dss.generic.shared.content.ContentCache;
import ch.systemsx.cisd.openbis.dss.generic.shared.content.IContentCache;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.DataSetArchivingStatus;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.IDatasetLocation;
import ch.systemsx.cisd.openbis.generic.shared.dto.DatasetDescription;
import lombok.Getter;

public class ServiceProvider implements IServiceProvider
{

    @Getter
    private static ServiceProvider instance;

    @Getter
    private final Configuration configuration;

    private IConfigProvider configProvider;

    private IMailClient mailClient;

    private IHierarchicalContentProvider hierarchicalContentProvider;

    private IDataSetDirectoryProvider dataSetDirectoryProvider;

    private IDataSetPathInfoProvider dataSetPathInfoProvider;

    private IPathInfoDataSourceProvider pathInfoDataSourceProvider;

    private IArchiverDataSourceProvider archiverDataSourceProvider;

    private IDataSetDeleter dataSetDeleter;

    private IDataSetStatusUpdater dataSetStatusUpdater;

    private IShareIdManager shareIdManager;

    private IArchiverPlugin archiverPlugin;

    private IArchiverContextFactory archiverContextFactory;

    private IArchiverTaskScheduler archiverTaskScheduler;

    private Properties archiverProperties;

    private IOpenBISService openBISService;

    private IApplicationServerApi applicationServerApi;

    private IContentCache contentCache;

    private IDssServiceFactory dssServiceFactory;

    private IIncomingShareIdProvider incomingShareIdProvider;

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
            dataSetPathInfoProvider =
                    new DatabaseBasedDataSetPathInfoProvider(PathInfoDatabaseConfiguration.getInstance(configuration)::getDataSource);
        }

        return dataSetPathInfoProvider;
    }

    public synchronized IPathInfoDataSourceProvider getPathInfoDataSourceProvider()
    {
        if (pathInfoDataSourceProvider == null)
        {
            pathInfoDataSourceProvider = new IPathInfoDataSourceProvider()
            {
                public DataSource getDataSource()
                {
                    return PathInfoDatabaseConfiguration.getInstance(configuration).getDataSource();
                }

                public boolean isDataSourceDefined()
                {
                    return PathInfoDatabaseConfiguration.hasInstance(configuration);
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
                    if (dataSets.isEmpty())
                    {
                        return;
                    }
                    MessagesDatabaseFacade facade = MessagesDatabaseConfiguration.getInstance(configuration).getMessagesDatabaseFacade();
                    JsonObjectMapper objectMapper = AtomicFileSystemServerParameterUtil.getJsonObjectMapper(configuration);
                    facade.create(new DeleteDataSetFromStoreMessage(MessageProcessId.getCurrentOrGenerateNew(), dataSets, maxNumberOfRetries,
                            waitingTimeBetweenRetries).serialize(objectMapper));
                }
            };
        }

        return dataSetDeleter;
    }

    public synchronized IDataSetStatusUpdater getDataSetStatusUpdater()
    {
        if (dataSetStatusUpdater == null)
        {
            dataSetStatusUpdater = new IDataSetStatusUpdater()
            {
                @Override public void scheduleUpdate(final List<String> dataSetCodes, final DataSetArchivingStatus status,
                        final Boolean presentInArchive)
                {
                    if (dataSetCodes.isEmpty())
                    {
                        return;
                    }
                    MessagesDatabaseFacade facade = MessagesDatabaseConfiguration.getInstance(configuration).getMessagesDatabaseFacade();
                    JsonObjectMapper objectMapper = AtomicFileSystemServerParameterUtil.getJsonObjectMapper(configuration);
                    facade.create(new UpdateDataSetArchivingStatusMessage(MessageProcessId.getCurrentOrGenerateNew(), dataSetCodes, status,
                            presentInArchive).serialize(objectMapper));
                }
            };
        }

        return dataSetStatusUpdater;
    }

    public IMultiDataSetArchiveCleaner getDataSetArchiveCleaner(final Properties properties)
    {
        return new AbstractMultiDataSetArchiveCleaner(properties)
        {
            @Override protected void deleteAsync(final File file)
            {
                final long timeout =
                        DateTimeUtils.getDurationInMillis(properties, FileDeleter.DELETION_TIME_OUT_KEY, FileDeleter.DEFAULT_DELETION_TIME_OUT);
                final String emailOrNull = PropertyUtils.getProperty(properties, FileDeleter.EMAIL_ADDRESS_KEY);

                String emailTemplate = null;
                String emailSubject = null;
                String emailFromAddressOrNull = null;

                if (emailOrNull != null)
                {
                    emailTemplate = PropertyUtils.getMandatoryProperty(properties, FileDeleter.EMAIL_TEMPLATE_KEY);
                    emailSubject = PropertyUtils.getMandatoryProperty(properties, FileDeleter.EMAIL_SUBJECT_KEY);
                    emailFromAddressOrNull = properties.getProperty(FileDeleter.EMAIL_FROM_ADDRESS_KEY);
                }

                MessagesDatabaseFacade facade = MessagesDatabaseConfiguration.getInstance(configuration).getMessagesDatabaseFacade();
                JsonObjectMapper objectMapper = AtomicFileSystemServerParameterUtil.getJsonObjectMapper(configuration);
                facade.create(new DeleteFileMessage(MessageProcessId.getCurrentOrGenerateNew(), file, new Date(), timeout,
                        emailOrNull, emailTemplate, emailSubject, emailFromAddressOrNull).serialize(
                        objectMapper));
            }
        };
    }

    public synchronized IShareIdManager getShareIdManager()
    {
        if (shareIdManager == null)
        {
            final IOpenBISFacade openBISFacade = OpenBISConfiguration.getInstance(configuration).getOpenBISFacade();
            final String storageRoot = AtomicFileSystemServerParameterUtil.getStorageRoot(configuration);
            final int lockingTimeoutInSeconds = AtomicFileSystemServerParameterUtil.getLockingTimeoutInSeconds(configuration);
            final int lockingWaitingIntervalInMillis = AtomicFileSystemServerParameterUtil.getLockingWaitingIntervalInMillis(configuration);

            Object connectionFactoryObject;
            TransactionManager transactionManager;

            try
            {
                connectionFactoryObject = configuration.getSharableInstance(AtomicFileSystemServerParameter.connectionFactoryClass);
            } catch (Exception e)
            {
                throw new RuntimeException("Could not get instance of connection factory", e);
            }

            if (connectionFactoryObject == null)
            {
                throw new RuntimeException("Connection factory was null");
            } else if (connectionFactoryObject instanceof ConnectionFactory)
            {
                ConnectionFactory connectionFactory = (ConnectionFactory) connectionFactoryObject;
                transactionManager = connectionFactory.getTransactionManager();
            } else
            {
                throw new RuntimeException("Unsupported connection factory class " + connectionFactoryObject.getClass()
                        + ". Cannot extract instance of transaction manager from it.");
            }

            shareIdManager =
                    new ShareIdManager(openBISFacade, transactionManager, storageRoot, lockingTimeoutInSeconds, lockingWaitingIntervalInMillis);
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
                        final List<DatasetDescription> dataSets, final String userId, final String userEmailOrNull, final String userSessionToken)
                {
                    if (dataSets.isEmpty())
                    {
                        return;
                    }

                    if (task instanceof MultiDataSetArchivingFinalizer)
                    {
                        MessagesDatabaseFacade facade = MessagesDatabaseConfiguration.getInstance(configuration).getMessagesDatabaseFacade();
                        JsonObjectMapper objectMapper = AtomicFileSystemServerParameterUtil.getJsonObjectMapper(configuration);
                        facade.create(
                                new FinalizeDataSetArchivingMessage(MessageProcessId.getCurrentOrGenerateNew(), (MultiDataSetArchivingFinalizer) task,
                                        parameterBindings, dataSets).serialize(objectMapper));
                    } else
                    {
                        throw new IllegalArgumentException("Unsupported task: " + task.getClass());
                    }
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
            MessagesDatabaseConfiguration messagesDatabaseConfiguration = MessagesDatabaseConfiguration.getInstance(configuration);
            JsonObjectMapper jsonObjectMapper = AtomicFileSystemServerParameterUtil.getJsonObjectMapper(configuration);
            openBISService = new OpenBISService(openBISConfiguration.getOpenBISFacade(), messagesDatabaseConfiguration.getMessagesDatabaseFacade(),
                    jsonObjectMapper);
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

    public synchronized IIncomingShareIdProvider getIncomingShareIdProvider()
    {
        if (incomingShareIdProvider == null)
        {
            IncomingShareIdProvider provider = new IncomingShareIdProvider();
            provider.add(Set.of(AtomicFileSystemServerParameterUtil.getStorageIncomingShareId(configuration).toString()));
            incomingShareIdProvider = provider;
        }

        return incomingShareIdProvider;
    }

}
