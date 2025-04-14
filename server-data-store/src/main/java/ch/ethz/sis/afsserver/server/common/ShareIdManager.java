package ch.ethz.sis.afsserver.server.common;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import ch.ethz.sis.afs.dto.Lock;
import ch.ethz.sis.afs.dto.LockType;
import ch.ethz.sis.afs.manager.TransactionManager;
import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameter;
import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameterUtil;
import ch.ethz.sis.afsserver.worker.ConnectionFactory;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.search.DataSetSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.search.DataStoreKind;
import ch.ethz.sis.shared.startup.Configuration;
import ch.systemsx.cisd.openbis.dss.generic.shared.IShareIdManager;

public class ShareIdManager implements IShareIdManager
{

    private static final String DEFAULT_SHARE_ID = "1";

    private final OpenBISFacade openBISFacade;

    private final TransactionManager transactionManager;

    private final String storageRoot;

    private final int lockingTimeoutInSeconds;

    private final int lockingWaitingIntervalInMillis;

    public ShareIdManager(Configuration configuration, OpenBISFacade openBISFacade)
    {
        this.openBISFacade = openBISFacade;
        this.storageRoot = AtomicFileSystemServerParameterUtil.getStorageRoot(configuration);
        this.lockingTimeoutInSeconds = AtomicFileSystemServerParameterUtil.getLockingTimeoutInSeconds(configuration);
        this.lockingWaitingIntervalInMillis = AtomicFileSystemServerParameterUtil.getLockingWaitingIntervalInMillis(configuration);

        Object connectionFactoryObject;

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
    }

    @Override public boolean isKnown(final String dataSetCode)
    {
        return getDataSet(dataSetCode) != null;
    }

    @Override public String getShareId(final String dataSetCode)
    {
        DataSet dataSet = getDataSet(dataSetCode);

        if (dataSet == null)
        {
            throw new UnknownDataSetException(dataSetCode);
        }

        if (dataSet.getPhysicalData() != null)
        {
            return dataSet.getPhysicalData().getShareId();
        } else
        {
            return DEFAULT_SHARE_ID;
        }
    }

    @Override public void setShareId(final String dataSetCode, final String shareId)
    {
        // do nothing
    }

    @Override public void lock(final UUID ownerId, final String dataSetCode)
    {
        lock(ownerId, List.of(dataSetCode));
    }

    @Override public void lock(final UUID ownerId, final List<String> dataSetCodes)
    {
        List<DataSet> dataSets = getDataSets(dataSetCodes);

        if (dataSets.size() != dataSetCodes.size())
        {
            List<String> notFoundDataSetCodes = new ArrayList<>(dataSetCodes);
            notFoundDataSetCodes.removeAll(dataSets.stream().map(DataSet::getCode).collect(Collectors.toSet()));
            throw new UnknownDataSetException(notFoundDataSetCodes);
        }

        List<DataSet> mutableDataSets = filterDataSetsByMutability(dataSets, true);
        List<DataSet> immutableDataSets = filterDataSetsByMutability(dataSets, false);

        List<Lock<UUID, String>> locks = new ArrayList<>();
        locks.addAll(createLocks(ownerId, mutableDataSets, LockType.HierarchicallyExclusive));
        locks.addAll(createLocks(ownerId, immutableDataSets, LockType.Shared));

        boolean success = transactionManager.lock(locks);

        if (!success)
        {
            throw new LockingFailedException(dataSetCodes);
        }
    }

    @Override public void await(final String dataSetCode)
    {
        DataSet dataSet = getDataSet(dataSetCode);

        if (dataSet == null)
        {
            throw new UnknownDataSetException(dataSetCode);
        }

        long startMillis = System.currentTimeMillis();

        while (System.currentTimeMillis() < startMillis + lockingTimeoutInSeconds * 1000L)
        {
            List<Lock<UUID, String>> locks = filterLocksByDataSet(transactionManager.getLocks(), dataSet);

            if (!locks.isEmpty())
            {
                // Unfortunately, we don't have a mechanism in AFS what would notify
                // us when all the locks are released, therefore we need to do the waiting here.

                try
                {
                    Thread.sleep(lockingWaitingIntervalInMillis);
                } catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override public void releaseLock(final UUID ownerId, final String dataSetCode)
    {
        List<Lock<UUID, String>> locks = filterLocksByOwnerId(transactionManager.getLocks(), ownerId);
        List<Lock<UUID, String>> locksToRelease = new ArrayList<>();

        for (Lock<UUID, String> lock : locks)
        {
            if (lock.getResource().contains(dataSetCode)) // TODO improve the condition
            {
                locksToRelease.add(lock);
            }
        }

        boolean success = transactionManager.unlock(locksToRelease);

        if (!success)
        {
            throw new UnlockingFailedException(List.of(dataSetCode));
        }
    }

    @Override public void releaseLocks(final UUID ownerId)
    {
        List<Lock<UUID, String>> locksToRelease = filterLocksByOwnerId(transactionManager.getLocks(), ownerId);

        boolean success = transactionManager.unlock(locksToRelease);

        if (!success)
        {
            throw new UnlockingFailedException(ownerId);
        }
    }

    @Override public void cleanupLocks()
    {
        // nothing to do here
    }

    private List<DataSet> getDataSets(List<String> dataSetCodes)
    {
        DataSetSearchCriteria criteria = new DataSetSearchCriteria();
        criteria.withDataStore().withKind().thatIn(DataStoreKind.AFS);
        criteria.withCodes().thatIn(dataSetCodes);

        DataSetFetchOptions fetchOptions = new DataSetFetchOptions();
        fetchOptions.withExperiment();
        fetchOptions.withSample();
        fetchOptions.withPhysicalData();

        return openBISFacade.searchDataSets(criteria, fetchOptions).getObjects();
    }

    private DataSet getDataSet(String dataSetCode)
    {
        List<DataSet> dataSets = getDataSets(List.of(dataSetCode));

        if (dataSets.isEmpty())
        {
            return null;
        } else
        {
            return dataSets.get(0);
        }
    }

    private List<DataSet> filterDataSetsByMutability(List<DataSet> dataSets, boolean mutable)
    {
        return dataSets.stream().filter(dataSet ->
        {
            Date experimentImmutableDate = dataSet.getExperiment() != null ? dataSet.getExperiment().getImmutableDataDate() : null;
            Date sampleImmutableDate = dataSet.getSample() != null ? dataSet.getSample().getImmutableDataDate() : null;

            return (experimentImmutableDate == null && sampleImmutableDate == null) == mutable;
        }).collect(Collectors.toList());
    }

    private List<Lock<UUID, String>> createLocks(UUID owner, List<DataSet> dataSets, LockType lockType)
    {
        List<Lock<UUID, String>> locks = new ArrayList<>();

        for (DataSet dataSet : dataSets)
        {
            String resource = Paths.get(storageRoot, dataSet.getPhysicalData().getShareId(), dataSet.getPhysicalData().getLocation()).toString();
            locks.add(new Lock<>(owner, resource, lockType));
        }

        return locks;
    }

    private List<Lock<UUID, String>> filterLocksByOwnerId(List<Lock<UUID, String>> locks, UUID ownerId)
    {
        return locks.stream().filter(lock -> Objects.equals(ownerId, lock.getOwner())).collect(Collectors.toList());
    }

    private List<Lock<UUID, String>> filterLocksByDataSet(List<Lock<UUID, String>> locks, DataSet dataSet)
    {
        Lock<UUID, String> dataSetLock = createLocks(null, List.of(dataSet), null).get(0);
        return locks.stream().filter(lock -> Objects.equals(dataSetLock.getResource(), lock.getResource())).collect(Collectors.toList());
    }

    private static class UnknownDataSetException extends IllegalArgumentException
    {
        public UnknownDataSetException(List<String> dataSetCodes)
        {
            super("Unknown data set: " + dataSetCodes);
        }

        public UnknownDataSetException(String dataSetCode)
        {
            super("Unknown data set: " + dataSetCode);
        }
    }

    private static class LockingFailedException extends RuntimeException
    {
        public LockingFailedException(List<String> dataSetCodes)
        {
            super("Locking of data sets: " + dataSetCodes + " failed.");
        }
    }

    private static class UnlockingFailedException extends RuntimeException
    {
        public UnlockingFailedException(UUID ownerId)
        {
            super("Unlocking resources locked by owner: " + ownerId + " failed.");
        }

        public UnlockingFailedException(List<String> dataSetCodes)
        {
            super("Unlocking of data sets: " + dataSetCodes + " failed.");
        }
    }

}
