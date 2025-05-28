package ch.ethz.sis.afsserver.server.common;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import ch.ethz.sis.afs.dto.Lock;
import ch.ethz.sis.afs.dto.LockType;
import ch.ethz.sis.afs.manager.TransactionManager;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.search.DataSetSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.search.DataStoreKind;
import ch.systemsx.cisd.openbis.dss.generic.shared.IShareIdManager;
import lombok.Getter;

public class ShareIdManager implements IShareIdManager
{

    private static final String DEFAULT_SHARE_ID = "1";

    private final IOpenBISFacade openBISFacade;

    private final TransactionManager transactionManager;

    private final String storageRoot;

    private final int lockingTimeoutInSeconds;

    private final int lockingWaitingIntervalInMillis;

    private final ThreadLocal<ThreadLocks> threadLocks = new ThreadLocal<>();

    public ShareIdManager(IOpenBISFacade openBISFacade, TransactionManager transactionManager, String storageRoot, int lockingTimeoutInSeconds,
            int lockingWaitingIntervalInMillis)
    {
        this.openBISFacade = openBISFacade;
        this.transactionManager = transactionManager;
        this.storageRoot = storageRoot;
        this.lockingTimeoutInSeconds = lockingTimeoutInSeconds;
        this.lockingWaitingIntervalInMillis = lockingWaitingIntervalInMillis;
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

    @Override public void lock(final String dataSetCode)
    {
        lock(List.of(dataSetCode));
    }

    @Override public void lock(final List<String> dataSetCodes)
    {
        List<DataSet> dataSets = getDataSets(dataSetCodes);

        if (dataSets.size() != dataSetCodes.size())
        {
            List<String> notFoundDataSetCodes = new ArrayList<>(dataSetCodes);
            notFoundDataSetCodes.removeAll(dataSets.stream().map(DataSet::getCode).collect(Collectors.toSet()));
            throw new UnknownDataSetException(notFoundDataSetCodes);
        }

        ThreadLocks threadLocks = getThreadLocks();
        List<DataSetLock> locksToAdd = new ArrayList<>();
        List<DataSetLock> locksToIncrease = new ArrayList<>();

        for (DataSet dataSet : dataSets)
        {
            DataSetLock dataSetLock = threadLocks.getLock(dataSet.getCode());

            if (dataSetLock == null)
            {
                Lock<UUID, String> lock = new Lock<>(threadLocks.getOwnerId(), getResource(dataSet),
                        isMutable(dataSet) ? LockType.HierarchicallyExclusive : LockType.Shared);
                locksToAdd.add(new DataSetLock(dataSet.getCode(), lock, 1));
            } else
            {
                locksToIncrease.add(dataSetLock);
            }
        }

        boolean success = transactionManager.lock(locksToAdd.stream().map(DataSetLock::getLock).collect(Collectors.toList()));

        if (success)
        {
            for (DataSetLock lockToAdd : locksToAdd)
            {
                threadLocks.setLock(lockToAdd.getDataSetCode(), lockToAdd);
            }
            for (DataSetLock lockToIncrease : locksToIncrease)
            {
                lockToIncrease.increaseCounter();
            }
        } else
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
        String dataSetResource = getResource(dataSet);

        while (System.currentTimeMillis() < startMillis + lockingTimeoutInSeconds * 1000L)
        {
            boolean dataSetLocked = false;

            for (Lock<UUID, String> lock : transactionManager.getLocks())
            {
                if (Objects.equals(dataSetResource, lock.getResource()))
                {
                    dataSetLocked = true;
                    break;
                }
            }

            if (dataSetLocked)
            {
                try
                {
                    // Unfortunately, we don't have a mechanism in AFS that would notify
                    // us when all the locks are released, therefore, we need to do the waiting here.
                    Thread.sleep(lockingWaitingIntervalInMillis);
                } catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override public void releaseLock(final String dataSetCode)
    {
        releaseLocks(List.of(dataSetCode));
    }

    @Override public void releaseLocks(final List<String> dataSetCodes)
    {
        ThreadLocks threadLocks = getThreadLocks();
        List<DataSetLock> locksToRemove = new ArrayList<>();
        List<DataSetLock> locksToDecrease = new ArrayList<>();

        for (String dataSetCode : dataSetCodes)
        {
            DataSetLock dataSetLock = threadLocks.getLock(dataSetCode);

            if (dataSetLock != null)
            {
                locksToDecrease.add(dataSetLock);

                if (dataSetLock.getCounter() <= 1)
                {
                    locksToRemove.add(dataSetLock);
                }
            }
        }

        boolean success = transactionManager.unlock(locksToRemove.stream().map(DataSetLock::getLock).collect(Collectors.toList()));

        if (success)
        {
            for (DataSetLock lockToRemove : locksToRemove)
            {
                threadLocks.setLock(lockToRemove.getDataSetCode(), null);
            }
            for (DataSetLock lockToDecrease : locksToDecrease)
            {
                lockToDecrease.decreaseCounter();
            }
        } else
        {
            throw new UnlockingFailedException(dataSetCodes);
        }
    }

    @Override public void releaseLocks()
    {
        ThreadLocks threadLocks = getThreadLocks();
        List<DataSetLock> locksToRemove = threadLocks.getLocks();

        boolean success = transactionManager.unlock(locksToRemove.stream().map(DataSetLock::getLock).collect(Collectors.toList()));

        if (success)
        {
            for (DataSetLock lockToRemove : locksToRemove)
            {
                threadLocks.setLock(lockToRemove.getDataSetCode(), null);
            }
        } else
        {
            throw new UnlockingFailedException(threadLocks.getOwnerId());
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

    private String getResource(DataSet dataSet)
    {
        return Paths.get(storageRoot, dataSet.getPhysicalData().getShareId(), dataSet.getPhysicalData().getLocation()).toString();
    }

    private boolean isMutable(DataSet dataSet)
    {
        return (dataSet.getExperiment() == null || dataSet.getExperiment().getImmutableDataDate() == null) && (dataSet.getSample() == null
                || dataSet.getSample().getImmutableDataDate() == null);
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

    private ThreadLocks getThreadLocks()
    {
        ThreadLocks locks = threadLocks.get();
        if (locks == null)
        {
            locks = new ThreadLocks();
            threadLocks.set(locks);
        }
        return locks;
    }

    private static class ThreadLocks
    {

        @Getter private final UUID ownerId;

        private final Map<String, DataSetLock> dataSetLocks;

        public ThreadLocks()
        {
            ownerId = UUID.randomUUID();
            dataSetLocks = new HashMap<>();
        }

        public DataSetLock getLock(String dataSetCode)
        {
            return dataSetLocks.get(dataSetCode);
        }

        public void setLock(String dataSetCode, DataSetLock dataSetLock)
        {
            dataSetLocks.put(dataSetCode, dataSetLock);
        }

        public List<DataSetLock> getLocks()
        {
            return new ArrayList<>(dataSetLocks.values());
        }
    }

    private static class DataSetLock
    {

        @Getter private final String dataSetCode;

        @Getter private final Lock<UUID, String> lock;

        @Getter private int counter;

        public DataSetLock(String dataSetCode, Lock<UUID, String> lock, int counter)
        {
            this.dataSetCode = dataSetCode;
            this.lock = lock;
            this.counter = counter;
        }

        public void increaseCounter()
        {
            counter++;
        }

        public void decreaseCounter()
        {
            counter--;
        }

    }

}
