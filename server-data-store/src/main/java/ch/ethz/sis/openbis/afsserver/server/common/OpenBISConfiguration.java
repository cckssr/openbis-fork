package ch.ethz.sis.openbis.afsserver.server.common;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameterUtil;
import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.DataStore;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.fetchoptions.DataStoreFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.search.DataStoreKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.search.DataStoreSearchCriteria;
import ch.ethz.sis.shared.log.standard.LogManager;
import ch.ethz.sis.shared.log.standard.Logger;
import ch.ethz.sis.shared.startup.Configuration;
import ch.systemsx.cisd.common.exceptions.ConfigurationFailureException;
import lombok.Getter;

@Getter
public class OpenBISConfiguration
{

    public enum OpenBISParameter
    {
        openBISUrl,
        openBISTimeout,
        openBISUser,
        openBISPassword,
        openBISLastSeenDeletionFile,
        openBISLastSeenDeletionBatchSize,
        openBISLastSeenDeletionIntervalInSeconds
    }

    private static volatile OpenBISConfiguration instance;

    private static volatile Configuration configuration;

    private final String openBISUrl;

    private final Integer openBISTimeout;

    private final String openBISUser;

    private final String openBISPassword;

    private final String openBISLastSeenDeletionFile;

    private final Integer openBISLastSeenDeletionBatchSize;

    private final Integer openBISLastSeenDeletionIntervalInSeconds;

    private final IOpenBISFacade openBISFacade;

    private String storageUuid;

    public static OpenBISConfiguration getInstance(Configuration configuration)
    {
        return getInstance(configuration, new IOpenBISFacadeFactory()
        {
            @Override public IOpenBISFacade createFacade(String openBISUrl, String openBISUser, String openBISPassword,
                    Integer openBISTimeout)
            {
                return new OpenBISFacade(openBISUrl, openBISUser, openBISPassword, openBISTimeout);
            }
        });
    }

    static OpenBISConfiguration getInstance(Configuration configuration, IOpenBISFacadeFactory facadeFactory)
    {
        if (OpenBISConfiguration.configuration != configuration)
        {
            synchronized (OpenBISConfiguration.class)
            {
                if (OpenBISConfiguration.configuration != configuration)
                {
                    instance = new OpenBISConfiguration(configuration, facadeFactory);
                    OpenBISConfiguration.configuration = configuration;
                }
            }
        }

        return instance;
    }

    private OpenBISConfiguration(Configuration configuration, IOpenBISFacadeFactory facadeFactory)
    {
        openBISUrl = AtomicFileSystemServerParameterUtil.getStringParameter(configuration, OpenBISParameter.openBISUrl, true);
        openBISTimeout = AtomicFileSystemServerParameterUtil.getIntegerParameter(configuration, OpenBISParameter.openBISTimeout, true);
        openBISUser = AtomicFileSystemServerParameterUtil.getStringParameter(configuration, OpenBISParameter.openBISUser, true);
        openBISPassword = AtomicFileSystemServerParameterUtil.getStringParameter(configuration, OpenBISParameter.openBISPassword, true);
        openBISLastSeenDeletionFile =
                AtomicFileSystemServerParameterUtil.getStringParameter(configuration, OpenBISParameter.openBISLastSeenDeletionFile, true);
        openBISLastSeenDeletionBatchSize =
                AtomicFileSystemServerParameterUtil.getIntegerParameter(configuration, OpenBISParameter.openBISLastSeenDeletionBatchSize, true);
        openBISLastSeenDeletionIntervalInSeconds = AtomicFileSystemServerParameterUtil.getIntegerParameter(configuration,
                OpenBISParameter.openBISLastSeenDeletionIntervalInSeconds, true);
        openBISFacade = facadeFactory.createFacade(openBISUrl, openBISUser, openBISPassword, openBISTimeout);

    }

    public synchronized String getStorageUuid()
    {
        if (storageUuid != null)
        {
            return storageUuid;
        }

        Logger logger = LogManager.getLogger(OpenBISConfiguration.class);

        DataStoreSearchCriteria dssCriteria = new DataStoreSearchCriteria();
        dssCriteria.withKind().thatIn(DataStoreKind.DSS);
        List<DataStore> dssServers = openBISFacade.searchDataStores(dssCriteria, new DataStoreFetchOptions()).getObjects();
        Set<String> dssStorageUuids =
                dssServers.stream().map(DataStore::getStorageUuid).filter(uuid -> uuid != null && !uuid.isBlank()).collect(Collectors.toSet());
        String afsStorageUuid = AtomicFileSystemServerParameterUtil.getStorageUuid(configuration);

        if (afsStorageUuid != null && !afsStorageUuid.isBlank())
        {
            if (dssStorageUuids.isEmpty() || dssStorageUuids.contains(afsStorageUuid))
            {
                storageUuid = afsStorageUuid;
                logger.info("Storage UUID: " + storageUuid);
            } else
            {
                throw new ConfigurationFailureException(
                        "Storage UUID configuration is incorrect. The storage UUID defined in AFS service.properties (" + afsStorageUuid
                                + ") is different than the storage UUID(s) found at the DSS server(s) (" + dssStorageUuids + ").");
            }
        } else
        {
            if (dssStorageUuids.isEmpty())
            {
                throw new ConfigurationFailureException(
                        "Storage UUID configuration is missing. The storage UUID hasn't been set in the AFS service.properties. Moreover, no DSS servers were found to perform automatic storage UUID configuration.");
            } else if (dssStorageUuids.size() == 1)
            {
                storageUuid = dssStorageUuids.iterator().next();
                logger.info("Storage UUID autoconfigured to value used by the DSS: " + storageUuid);
            } else
            {
                throw new ConfigurationFailureException(
                        "Storage UUID configuration is missing. The storage UUID hasn't been set in the AFS service.properties. Moreover, an automatic storage UUID configuration is not possible as there are multiple different storage UUID values used among DSS servers ("
                                + dssStorageUuids + ").");
            }
        }

        return storageUuid;
    }

    public OpenBIS getOpenBIS()
    {
        return new OpenBIS(openBISUrl, openBISTimeout);
    }

}
