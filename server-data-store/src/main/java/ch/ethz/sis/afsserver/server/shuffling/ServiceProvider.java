package ch.ethz.sis.afsserver.server.shuffling;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ch.ethz.sis.afs.dto.Lock;
import ch.ethz.sis.afs.dto.LockType;
import ch.ethz.sis.afs.manager.TransactionManager;
import ch.ethz.sis.afsserver.server.common.OpenBISConfiguration;
import ch.ethz.sis.afsserver.server.common.OpenBISFacade;
import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameter;
import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameterUtil;
import ch.ethz.sis.afsserver.worker.ConnectionFactory;
import ch.ethz.sis.shared.startup.Configuration;

public class ServiceProvider
{
    private static volatile boolean initialized = false;

    private static Configuration configuration;

    private static IEncapsulatedOpenBISService openBISService;

    private static ILockManager lockManager;

    private static IConfigProvider configProvider;

    public static void configure(Configuration configuration)
    {
        ServiceProvider.configuration = configuration;
        ServiceProvider.initialized = false;
    }

    public static OpenBISFacade getOpenBISFacade()
    {
        return OpenBISConfiguration.getInstance(configuration).getOpenBISFacade();
    }

    public static IEncapsulatedOpenBISService getOpenBISService()
    {
        initialize();
        return openBISService;
    }

    public static ILockManager getLockManager()
    {
        initialize();
        return lockManager;
    }

    public static IConfigProvider getConfigProvider()
    {
        initialize();
        return configProvider;
    }

    public static Configuration getConfiguration()
    {
        return configuration;
    }

    private static void initialize()
    {
        // initialize lazily only to verify configuration properties if they are really needed

        if (!initialized)
        {
            synchronized (ServiceProvider.class)
            {
                if (!initialized)
                {
                    if (configuration == null)
                    {
                        throw new RuntimeException("Cannot initialize with null configuration");
                    }

                    ServiceProvider.openBISService =
                            new EncapsulatedOpenBISService(OpenBISConfiguration.getInstance(configuration).getOpenBISFacade());
                    ServiceProvider.lockManager = createLockManager();
                    ServiceProvider.configProvider = new ConfigProvider(configuration);
                    initialized = true;
                }
            }
        }
    }

    private static ILockManager createLockManager()
    {
        Object connectionFactoryObject;

        try
        {
            connectionFactoryObject = configuration.getSharableInstance(AtomicFileSystemServerParameter.connectionFactoryClass);
        } catch (Exception e)
        {
            throw new RuntimeException("Could not get instance of connection factory", e);
        }

        if (connectionFactoryObject instanceof ConnectionFactory)
        {
            ConnectionFactory connectionFactory = (ConnectionFactory) connectionFactoryObject;
            TransactionManager transactionManager = connectionFactory.getTransactionManager();
            return new ILockManager()
            {

                @Override public boolean lock(final UUID owner, final List<SimpleDataSetInformationDTO> dataSets, final LockType lockType)
                {
                    return transactionManager.lock(convert(owner, dataSets, lockType));
                }

                @Override public boolean unlock(final UUID owner, final List<SimpleDataSetInformationDTO> dataSets, final LockType lockType)
                {
                    return transactionManager.unlock(convert(owner, dataSets, lockType));
                }

                private List<Lock<UUID, String>> convert(UUID owner, List<SimpleDataSetInformationDTO> dataSets, LockType lockType)
                {
                    List<Lock<UUID, String>> locks = new ArrayList<>();

                    String storageRoot = AtomicFileSystemServerParameterUtil.getStorageRoot(configuration);

                    for (SimpleDataSetInformationDTO dataSet : dataSets)
                    {
                        String resource = Paths.get(storageRoot, dataSet.getDataSetShareId(), dataSet.getDataSetLocation()).toString();
                        locks.add(new Lock<>(owner, resource, lockType));
                    }

                    return locks;
                }
            };
        } else
        {
            throw new RuntimeException("Unsupported connection factory class " + connectionFactoryObject.getClass()
                    + ". Cannot extract instance of transaction manager from it.");
        }
    }

}
