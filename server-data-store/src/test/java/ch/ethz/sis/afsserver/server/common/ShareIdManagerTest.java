package ch.ethz.sis.afsserver.server.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.ethz.sis.afs.manager.TransactionManager;
import ch.ethz.sis.afsjson.JsonObjectMapper;
import ch.ethz.sis.afsserver.AbstractTest;
import ch.ethz.sis.afsserver.ServerClientEnvironmentFS;
import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameter;
import ch.ethz.sis.afsserver.worker.providers.impl.OpenBISAuthorizationInfoProvider;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.PhysicalData;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.search.DataSetSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.shared.io.IOUtils;
import ch.ethz.sis.shared.log.LogManager;
import ch.ethz.sis.shared.log.Logger;
import ch.ethz.sis.shared.startup.Configuration;
import ch.systemsx.cisd.common.concurrent.MessageChannel;
import ch.systemsx.cisd.openbis.dss.generic.shared.IShareIdManager;

public class ShareIdManagerTest extends AbstractTest
{

    private static final Logger logger = LogManager.getLogger(ShareIdManagerTest.class);

    private static final String SHARE_1 = "1";

    private static final String SHARE_2 = "2";

    private static final String STORAGE_UUID = "test-storage-uuid";

    private static final String DATA_SET_1_CODE = "data-set-1";

    private static final String DATA_SET_2_CODE = "data-set-2";

    private static final String DATA_SET_1_LOCATION =
            STORAGE_UUID + "/" + String.join("/", IOUtils.getShards(DATA_SET_1_CODE)) + "/" + DATA_SET_1_CODE;

    private static final String DATA_SET_2_LOCATION =
            STORAGE_UUID + "/" + String.join("/", IOUtils.getShards(DATA_SET_2_CODE)) + "/" + DATA_SET_2_CODE;

    private Mockery context;

    private IOpenBISFacade openBISFacade;

    @Before
    public void beforeMethod()
    {
        context = new Mockery();
        openBISFacade = context.mock(IOpenBISFacade.class);
    }

    @After
    public void afterMethod()
    {
        context.assertIsSatisfied();
    }

    @Test
    public void testLockTheSameDataSetInTheSameThread() throws Exception
    {
        DataSet mutableSampleDataSet = dataSet(null, sample(true), DATA_SET_1_CODE, SHARE_1, DATA_SET_1_LOCATION);
        testLockDataSetsInTheSameThread(mutableSampleDataSet, mutableSampleDataSet, null);

        DataSet immutableSampleDataSet = dataSet(null, sample(false), DATA_SET_1_CODE, SHARE_1, DATA_SET_1_LOCATION);
        testLockDataSetsInTheSameThread(immutableSampleDataSet, immutableSampleDataSet, null);

        DataSet mutableExperimentDataSet = dataSet(experiment(true), null, DATA_SET_1_CODE, SHARE_1, DATA_SET_1_LOCATION);
        testLockDataSetsInTheSameThread(mutableExperimentDataSet, mutableExperimentDataSet, null);

        DataSet immutableExperimentDataSet = dataSet(experiment(false), null, DATA_SET_1_CODE, SHARE_1, DATA_SET_1_LOCATION);
        testLockDataSetsInTheSameThread(immutableExperimentDataSet, immutableExperimentDataSet, null);
    }

    @Test
    public void testLockTheSameDataSetInDifferentThreads() throws Exception
    {
        // exclusive lock
        DataSet mutableSampleDataSet = dataSet(null, sample(true), DATA_SET_1_CODE, SHARE_1, DATA_SET_1_LOCATION);
        testLockDataSetsInDifferentThreads(mutableSampleDataSet, mutableSampleDataSet, "Locking of data sets: [data-set-1] failed.");

        // shared lock
        DataSet immutableSampleDataSet = dataSet(null, sample(false), DATA_SET_1_CODE, SHARE_1, DATA_SET_1_LOCATION);
        testLockDataSetsInDifferentThreads(immutableSampleDataSet, immutableSampleDataSet, null);

        // exclusive lock
        DataSet mutableExperimentDataSet = dataSet(experiment(true), null, DATA_SET_1_CODE, SHARE_1, DATA_SET_1_LOCATION);
        testLockDataSetsInDifferentThreads(mutableExperimentDataSet, mutableExperimentDataSet, "Locking of data sets: [data-set-1] failed.");

        // shared lock
        DataSet immutableExperimentDataSet = dataSet(experiment(false), null, DATA_SET_1_CODE, SHARE_1, DATA_SET_1_LOCATION);
        testLockDataSetsInDifferentThreads(immutableExperimentDataSet, immutableExperimentDataSet, null);
    }

    @Test
    public void testLockDifferentDataSetsInTheSameThread() throws Exception
    {
        DataSet mutableSampleDataSet1 = dataSet(null, sample(true), DATA_SET_1_CODE, SHARE_1, DATA_SET_1_LOCATION);
        DataSet mutableSampleDataSet2 = dataSet(null, sample(true), DATA_SET_2_CODE, SHARE_1, DATA_SET_2_LOCATION);
        testLockDataSetsInTheSameThread(mutableSampleDataSet1, mutableSampleDataSet2, null);

        DataSet immutableSampleDataSet1 = dataSet(null, sample(false), DATA_SET_1_CODE, SHARE_1, DATA_SET_1_LOCATION);
        DataSet immutableSampleDataSet2 = dataSet(null, sample(false), DATA_SET_2_CODE, SHARE_1, DATA_SET_2_LOCATION);
        testLockDataSetsInTheSameThread(immutableSampleDataSet1, immutableSampleDataSet2, null);

        DataSet mutableExperimentDataSet1 = dataSet(experiment(true), null, DATA_SET_1_CODE, SHARE_1, DATA_SET_1_LOCATION);
        DataSet mutableExperimentDataSet2 = dataSet(experiment(true), null, DATA_SET_2_CODE, SHARE_1, DATA_SET_2_LOCATION);
        testLockDataSetsInTheSameThread(mutableExperimentDataSet1, mutableExperimentDataSet2, null);

        DataSet immutableExperimentDataSet1 = dataSet(experiment(false), null, DATA_SET_1_CODE, SHARE_1, DATA_SET_1_LOCATION);
        DataSet immutableExperimentDataSet2 = dataSet(experiment(false), null, DATA_SET_2_CODE, SHARE_1, DATA_SET_2_LOCATION);
        testLockDataSetsInTheSameThread(immutableExperimentDataSet1, immutableExperimentDataSet2, null);
    }

    @Test
    public void testLockDifferentDataSetsInDifferentThreads() throws Exception
    {
        // exclusive lock
        DataSet mutableSampleDataSet1 = dataSet(null, sample(true), DATA_SET_1_CODE, SHARE_1, DATA_SET_1_LOCATION);
        DataSet mutableSampleDataSet2 = dataSet(null, sample(true), DATA_SET_2_CODE, SHARE_1, DATA_SET_2_LOCATION);
        testLockDataSetsInDifferentThreads(mutableSampleDataSet1, mutableSampleDataSet2, null);

        // shared lock
        DataSet immutableSampleDataSet1 = dataSet(null, sample(false), DATA_SET_1_CODE, SHARE_1, DATA_SET_1_LOCATION);
        DataSet immutableSampleDataSet2 = dataSet(null, sample(false), DATA_SET_2_CODE, SHARE_1, DATA_SET_2_LOCATION);
        testLockDataSetsInDifferentThreads(immutableSampleDataSet1, immutableSampleDataSet2, null);

        // exclusive lock
        DataSet mutableExperimentDataSet1 = dataSet(experiment(true), null, DATA_SET_1_CODE, SHARE_1, DATA_SET_1_LOCATION);
        DataSet mutableExperimentDataSet2 = dataSet(experiment(true), null, DATA_SET_2_CODE, SHARE_1, DATA_SET_2_LOCATION);
        testLockDataSetsInDifferentThreads(mutableExperimentDataSet1, mutableExperimentDataSet2, null);

        // shared lock
        DataSet immutableExperimentDataSet1 = dataSet(experiment(false), null, DATA_SET_1_CODE, SHARE_1, DATA_SET_1_LOCATION);
        DataSet immutableExperimentDataSet2 = dataSet(experiment(false), null, DATA_SET_2_CODE, SHARE_1, DATA_SET_2_LOCATION);
        testLockDataSetsInDifferentThreads(immutableExperimentDataSet1, immutableExperimentDataSet2, null);
    }

    @Test
    public void testLockAndReleaseLock() throws Exception
    {
        DataSet mutableSampleDataSet = dataSet(null, sample(true), DATA_SET_1_CODE, SHARE_1, DATA_SET_1_LOCATION);
        testLockAndReleaseLock(mutableSampleDataSet, false);

        DataSet immutableSampleDataSet = dataSet(null, sample(false), DATA_SET_1_CODE, SHARE_1, DATA_SET_1_LOCATION);
        testLockAndReleaseLock(immutableSampleDataSet, false);

        DataSet mutableExperimentDataSet = dataSet(experiment(true), null, DATA_SET_1_CODE, SHARE_1, DATA_SET_1_LOCATION);
        testLockAndReleaseLock(mutableExperimentDataSet, false);

        DataSet immutableExperimentDataSet = dataSet(experiment(false), null, DATA_SET_1_CODE, SHARE_1, DATA_SET_1_LOCATION);
        testLockAndReleaseLock(immutableExperimentDataSet, false);
    }

    @Test
    public void testLockAndReleaseAllLocks() throws Exception
    {
        DataSet mutableSampleDataSet = dataSet(null, sample(true), DATA_SET_1_CODE, SHARE_1, DATA_SET_1_LOCATION);
        testLockAndReleaseLock(mutableSampleDataSet, true);

        DataSet immutableSampleDataSet = dataSet(null, sample(false), DATA_SET_1_CODE, SHARE_1, DATA_SET_1_LOCATION);
        testLockAndReleaseLock(immutableSampleDataSet, true);

        DataSet mutableExperimentDataSet = dataSet(experiment(true), null, DATA_SET_1_CODE, SHARE_1, DATA_SET_1_LOCATION);
        testLockAndReleaseLock(mutableExperimentDataSet, true);

        DataSet immutableExperimentDataSet = dataSet(experiment(false), null, DATA_SET_1_CODE, SHARE_1, DATA_SET_1_LOCATION);
        testLockAndReleaseLock(immutableExperimentDataSet, true);
    }

    private void testLockDataSetsInTheSameThread(DataSet dataSet1, DataSet dataSet2, String expectedException) throws Exception
    {
        context.checking(new Expectations()
        {
            {
                one(openBISFacade).searchDataSets(with(any(DataSetSearchCriteria.class)), with(any(DataSetFetchOptions.class)));
                will(returnValue(searchResult(dataSet1)));

                one(openBISFacade).searchDataSets(with(any(DataSetSearchCriteria.class)), with(any(DataSetFetchOptions.class)));
                will(returnValue(searchResult(dataSet2)));
            }
        });

        IShareIdManager shareIdManager = shareIdManager();
        shareIdManager.lock(dataSet1.getCode());

        Exception exception = null;
        try
        {
            shareIdManager.lock(dataSet2.getCode());
        } catch (Exception e)
        {
            exception = e;
        }

        assertEquals(expectedException, exception != null ? exception.getMessage() : null);
    }

    private void testLockDataSetsInDifferentThreads(DataSet dataSet1, DataSet dataSet2, String expectedException)
            throws Exception
    {
        context.checking(new Expectations()
        {
            {
                one(openBISFacade).searchDataSets(with(any(DataSetSearchCriteria.class)), with(any(DataSetFetchOptions.class)));
                will(returnValue(searchResult(dataSet1)));

                one(openBISFacade).searchDataSets(with(any(DataSetSearchCriteria.class)), with(any(DataSetFetchOptions.class)));
                will(returnValue(searchResult(dataSet2)));
            }
        });

        IShareIdManager shareIdManager = shareIdManager();
        MessageChannel channel = new MessageChannel();

        AtomicReference<Throwable> exception1 = new AtomicReference<Throwable>(null);
        Thread thread1 = new Thread(() ->
        {
            try
            {
                shareIdManager.lock(dataSet1.getCode());
                channel.send("locked");
            } catch (Throwable e)
            {
                logger.info("Thread 1 exception", e);
                exception1.set(e);
            }
        });

        AtomicReference<Throwable> exception2 = new AtomicReference<Throwable>(null);
        Thread thread2 = new Thread(() ->
        {
            channel.assertNextMessage("locked");
            try
            {
                shareIdManager.lock(dataSet2.getCode());
            } catch (Throwable e)
            {
                logger.info("Thread 2 exception", e);
                exception2.set(e);
            }
        });

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        assertNull(exception1.get());
        assertEquals(expectedException, exception2.get() != null ? exception2.get().getMessage() : null);
    }

    private void testLockAndReleaseLock(DataSet dataSet, boolean releaseAllLocks) throws Exception
    {
        context.checking(new Expectations()
        {
            {
                allowing(openBISFacade).searchDataSets(with(any(DataSetSearchCriteria.class)), with(any(DataSetFetchOptions.class)));
                will(returnValue(searchResult(dataSet)));
            }
        });

        IShareIdManager shareIdManager = shareIdManager();

        MessageChannel channel1 = new MessageChannel();
        MessageChannel channel2 = new MessageChannel();

        AtomicReference<Throwable> exception1 = new AtomicReference<Throwable>(null);
        Thread thread1 = new Thread(() ->
        {
            try
            {
                shareIdManager.lock(dataSet.getCode());
                channel1.send("locked");
                channel2.assertNextMessage("failed");
                if (releaseAllLocks)
                {
                    shareIdManager.releaseLock(dataSet.getCode());
                } else
                {
                    shareIdManager.releaseLocks();
                }
                channel1.send("released");
                channel2.assertNextMessage("locked");
            } catch (Throwable e)
            {
                logger.info("Thread 1 exception", e);
                exception1.set(e);
            }
        });

        AtomicReference<Throwable> exception2 = new AtomicReference<Throwable>(null);
        Thread thread2 = new Thread(() ->
        {
            try
            {
                channel1.assertNextMessage("locked");
                try
                {
                    shareIdManager.lock(dataSet.getCode());
                    fail();
                } catch (Exception e)
                {
                    assertEquals("Locking of data sets: [" + dataSet.getCode() + "] failed.", e.getMessage());
                }
                channel2.send("failed");
                channel1.assertNextMessage("released");
                shareIdManager.lock(dataSet.getCode());
                channel2.send("locked");
            } catch (Throwable e)
            {
                logger.info("Thread 2 exception", e);
                exception2.set(e);
            }
        });

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        assertNull(exception1.get());
        assertNull(exception2.get());
    }

    private ShareIdManager shareIdManager() throws Exception
    {
        Configuration configuration = new Configuration(ServerClientEnvironmentFS.getInstance().getDefaultServerConfiguration().getProperties());
        configuration.setProperty(OpenBISConfiguration.OpenBISParameter.openBISUrl, "test-openbis-url");
        configuration.setProperty(OpenBISConfiguration.OpenBISParameter.openBISTimeout, "1");
        configuration.setProperty(OpenBISConfiguration.OpenBISParameter.openBISUser, "test-user");
        configuration.setProperty(OpenBISConfiguration.OpenBISParameter.openBISPassword, "test-password");
        configuration.setProperty(OpenBISConfiguration.OpenBISParameter.openBISLastSeenDeletionFile, "last-seen-deletion-file");
        configuration.setProperty(OpenBISConfiguration.OpenBISParameter.openBISLastSeenDeletionBatchSize, "1");
        configuration.setProperty(OpenBISConfiguration.OpenBISParameter.openBISLastSeenDeletionIntervalInSeconds, "1");

        JsonObjectMapper jsonObjectMapper = configuration.getInstance(AtomicFileSystemServerParameter.jsonObjectMapperClass);
        String storageRoot = configuration.getStringProperty(AtomicFileSystemServerParameter.storageRoot);
        String writeAheadLogRoot = configuration.getStringProperty(AtomicFileSystemServerParameter.writeAheadLogRoot);

        Path storageRootPath = Paths.get(storageRoot);
        if (!Files.exists(storageRootPath))
        {
            Files.createDirectory(storageRootPath);
        }

        Path share = Paths.get(storageRoot, "1");
        if (!Files.exists(share))
        {
            Files.createDirectory(share);
        }

        OpenBISAuthorizationInfoProvider openBISAuthorizationInfoProvider = new OpenBISAuthorizationInfoProvider();
        openBISAuthorizationInfoProvider.init(configuration);

        TransactionManager transactionManager =
                new TransactionManager(openBISAuthorizationInfoProvider, jsonObjectMapper, writeAheadLogRoot, storageRoot);
        return new ShareIdManager(openBISFacade, transactionManager, storageRoot, 0, 0);
    }

    private Experiment experiment(boolean mutable)
    {
        Experiment experiment = new Experiment();
        if (!mutable)
        {
            experiment.setImmutableDataDate(new Date());
        }
        return experiment;
    }

    private Sample sample(boolean mutable)
    {
        Sample sample = new Sample();
        if (!mutable)
        {
            sample.setImmutableDataDate(new Date());
        }
        return sample;
    }

    private DataSet dataSet(Experiment experiment, Sample sample, String code, String shareId, String location)
    {
        DataSetFetchOptions fetchOptions = new DataSetFetchOptions();
        fetchOptions.withExperiment();
        fetchOptions.withSample();
        fetchOptions.withPhysicalData();

        PhysicalData physicalData = new PhysicalData();
        physicalData.setShareId(shareId);
        physicalData.setLocation(location);

        DataSet dataSet = new DataSet();
        dataSet.setCode(code);
        dataSet.setExperiment(experiment);
        dataSet.setSample(sample);
        dataSet.setPhysicalData(physicalData);
        dataSet.setFetchOptions(fetchOptions);

        return dataSet;
    }

    private SearchResult<DataSet> searchResult(DataSet... dataSets)
    {
        return new SearchResult<>(List.of(dataSets), dataSets.length);
    }

}
