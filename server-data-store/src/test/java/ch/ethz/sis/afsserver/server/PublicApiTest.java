/*
 * Copyright ETH 2022 - 2023 Zürich, Scientific IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.ethz.sis.afsserver.server;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.ethz.sis.afs.manager.TransactionConnection;
import ch.ethz.sis.afs.manager.TrashRootProvider;
import ch.ethz.sis.afsapi.api.PublicAPI;
import ch.ethz.sis.afsapi.dto.Chunk;
import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afsapi.dto.FreeSpace;
import ch.ethz.sis.afsclient.client.ChunkEncoderDecoder;
import ch.ethz.sis.afsserver.AbstractTest;
import ch.ethz.sis.afsserver.ServerClientEnvironmentFS;
import ch.ethz.sis.afsserver.server.impl.OperationResultCache;
import ch.ethz.sis.afsserver.server.observer.impl.DummyServerObserver;
import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameter;
import ch.ethz.sis.afsserver.worker.ConnectionFactory;
import ch.ethz.sis.afsserver.worker.WorkerFactory;
import ch.ethz.sis.shared.io.IOUtils;
import ch.ethz.sis.shared.log.classic.impl.Logger;
import ch.ethz.sis.shared.pool.Pool;
import ch.ethz.sis.shared.startup.Configuration;

public abstract class PublicApiTest extends AbstractTest
{

    public static final Logger logger = Logger.getLogger(PublicApiTest.class);

    public abstract PublicAPI getPublicAPI(APIServer apiServer, String interactiveSessionKey, String transactionManagerKey, String sessionToken)
            throws Exception;

    public PublicAPI getPublicAPI() throws Exception
    {
        return getPublicAPI(getAPIServer(), null, null, UUID.randomUUID().toString());
    }

    public PublicAPI getPublicAPI(String interactiveSessionKey, String transactionManagerKey) throws Exception
    {
        return getPublicAPI(getAPIServer(), interactiveSessionKey, transactionManagerKey, UUID.randomUUID().toString());
    }

    public static final String ROOT = IOUtils.PATH_SEPARATOR_AS_STRING;

    public static final String FILE_A = "A.txt";

    public static final byte[] DATA = "ABCD".getBytes();

    public static final String FILE_B = "B.txt";

    public String owner = UUID.randomUUID().toString();

    @Before
    public void createTestData() throws IOException
    {
        deleteTestData();

        String storageRoot = ServerClientEnvironmentFS.getInstance()
                .getDefaultServerConfiguration()
                .getStringProperty(AtomicFileSystemServerParameter.storageRoot);
        String testDataRoot = IOUtils.getPath(storageRoot, owner.toString());
        IOUtils.createDirectories(testDataRoot);
        String testDataFile = IOUtils.getPath(testDataRoot, FILE_A);
        IOUtils.createFile(testDataFile);
        IOUtils.write(testDataFile, 0, DATA);
    }

    @After
    public void deleteTestData() throws IOException
    {
        String storageRoot = ServerClientEnvironmentFS.getInstance()
                .getDefaultServerConfiguration()
                .getStringProperty(AtomicFileSystemServerParameter.storageRoot);
        if (IOUtils.exists(storageRoot))
        {
            IOUtils.delete(storageRoot);
        }
        String writeAheadLogRoot = ServerClientEnvironmentFS.getInstance()
                .getDefaultServerConfiguration()
                .getStringProperty(AtomicFileSystemServerParameter.writeAheadLogRoot);
        if (IOUtils.exists(writeAheadLogRoot))
        {
            IOUtils.delete(writeAheadLogRoot);
        }
    }

    @Test
    public void list() throws Exception
    {
        File[] list = getPublicAPI().list(owner, ROOT, Boolean.TRUE);
        assertEquals(1, list.length);
        assertEquals(FILE_A, list[0].getName());
    }

    @Test
    public void read() throws Exception
    {
        Chunk[] chunks = getPublicAPI().read(new Chunk[] { new Chunk(owner, FILE_A, 0L, DATA.length, ChunkEncoderDecoder.EMPTY_ARRAY) });
        assertArrayEquals(DATA, chunks[0].getData());
    }

    @Test(expected = RuntimeException.class)
    public void read_big_failure() throws Exception
    {
        Chunk[] chunks = getPublicAPI().read(new Chunk[] { new Chunk(owner, FILE_A, 0L, Integer.MAX_VALUE, ChunkEncoderDecoder.EMPTY_ARRAY) });
        assertArrayEquals(DATA, chunks[0].getData());
    }

    @Test
    public void write() throws Exception
    {
        getPublicAPI().write(new Chunk[] { new Chunk(owner, FILE_B, 0L, DATA.length, DATA) });
        Chunk[] chunks = getPublicAPI().read(new Chunk[] { new Chunk(owner, FILE_B, 0L, DATA.length, ChunkEncoderDecoder.EMPTY_ARRAY) });
        assertArrayEquals(DATA, chunks[0].getData());
    }

    @Test
    public void delete() throws Exception
    {
        Boolean deleted = getPublicAPI().delete(owner, FILE_A, false);
        assertTrue(deleted);
        File[] list = getPublicAPI().list(owner, ROOT, Boolean.TRUE);
        assertEquals(0, list.length);
    }

    @Test
    public void copy() throws Exception
    {
        getPublicAPI().copy(owner, FILE_A, owner, FILE_B);
        Chunk[] chunks = getPublicAPI().read(new Chunk[] { new Chunk(owner, FILE_B, 0L, DATA.length, ChunkEncoderDecoder.EMPTY_ARRAY) });
        assertArrayEquals(DATA, chunks[0].getData());
    }

    @Test
    public void move() throws Exception
    {
        getPublicAPI().move(owner, FILE_A, owner, FILE_B);
        File[] list = getPublicAPI().list(owner, ROOT, Boolean.TRUE);
        assertEquals(1, list.length);
        assertEquals(FILE_B, list[0].getName());
    }

    @Test
    public void create_directory() throws Exception
    {
        getPublicAPI().create(owner, FILE_B, Boolean.TRUE);

        final File[] list = getPublicAPI().list(owner, ROOT, Boolean.TRUE);
        assertEquals(2, list.length);

        final File[] matchedFiles = Arrays.stream(list).filter(file -> file.getName().equals(FILE_B)).toArray(File[]::new);
        assertEquals(1, matchedFiles.length);
        assertTrue(matchedFiles[0].getDirectory());
    }

    @Test
    public void create_file() throws Exception
    {
        getPublicAPI().create(owner, FILE_B, Boolean.FALSE);

        final File[] list = getPublicAPI().list(owner, ROOT, Boolean.TRUE);

        final File[] matchedFiles = Arrays.stream(list).filter(file -> file.getName().equals(FILE_B)).toArray(File[]::new);
        assertEquals(1, matchedFiles.length);
        assertFalse(matchedFiles[0].getDirectory());

        Chunk[] chunks = getPublicAPI().read(new Chunk[] { new Chunk(owner, FILE_B, 0L, 0, ChunkEncoderDecoder.EMPTY_ARRAY) });
        assertEquals(0, chunks[0].getData().length);
    }

    @Test
    public void free() throws Exception
    {
        final FreeSpace space = getPublicAPI().free(owner, ROOT);
        assertTrue(space.getFree() >= 0);
        assertTrue(space.getTotal() > 0);
        assertTrue(space.getFree() <= space.getTotal());
    }

    @Test
    public void operation_state_begin_succeed() throws Exception
    {
        UUID sessionToken = UUID.randomUUID();
        PublicAPI publicAPI = getPublicAPI("1234", null);
        publicAPI.begin(sessionToken);
    }

    @Test
    public void operation_state_prepare_succeed() throws Exception
    {
        UUID sessionToken = UUID.randomUUID();
        PublicAPI publicAPI = getPublicAPI("1234", "5678");
        publicAPI.begin(sessionToken);
        publicAPI.prepare();
    }

    @Test
    public void operation_state_rollback_succeed() throws Exception
    {
        UUID sessionToken = UUID.randomUUID();
        PublicAPI publicAPI = getPublicAPI("1234", "5678");
        publicAPI.begin(sessionToken);
        publicAPI.prepare();
        publicAPI.rollback();
    }

    @Test
    public void operation_state_commit_succeed() throws Exception
    {
        UUID sessionToken = UUID.randomUUID();
        PublicAPI publicAPI = getPublicAPI("1234", null);
        publicAPI.begin(sessionToken);
        publicAPI.commit();
    }

    @Test
    public void operation_state_commitPrepared_succeed() throws Exception
    {
        UUID sessionToken = UUID.randomUUID();
        PublicAPI publicAPI = getPublicAPI("1234", "5678");
        publicAPI.begin(sessionToken);
        publicAPI.prepare();
        publicAPI.commit();
    }

    @Test
    public void operation_state_commit_reuse_succeed() throws Exception
    {
        UUID sessionToken = UUID.randomUUID();
        PublicAPI publicAPI = getPublicAPI("1234", "5678");
        publicAPI.begin(sessionToken);
        publicAPI.prepare();
        publicAPI.commit();
        publicAPI.begin(sessionToken);
    }

    @Test
    public void operation_state_rollback_reuse_succeed() throws Exception
    {
        UUID sessionToken = UUID.randomUUID();
        PublicAPI publicAPI = getPublicAPI("1234", "5678");
        publicAPI.begin(sessionToken);
        publicAPI.prepare();
        publicAPI.rollback();
        publicAPI.begin(sessionToken);
    }

    @Test(expected = RuntimeException.class)
    public void operation_state_begin_reuse_fails() throws Exception
    {
        PublicAPI publicAPI = getPublicAPI("1234", null);
        publicAPI.begin(UUID.randomUUID());
        publicAPI.begin(UUID.randomUUID());
    }

    @Test
    public void operation_state_prepare_reuse_succeed() throws Exception
    {
        UUID sessionToken = UUID.randomUUID();
        PublicAPI publicAPI = getPublicAPI("1234", "5678");
        publicAPI.begin(sessionToken);
        publicAPI.prepare();
        publicAPI.begin(sessionToken);
    }

    @Test
    public void non_interactive_sessions_without_session_token_should_work() throws Exception
    {
        APIServer apiServer = getAPIServer();

        run_in_multiple_threads(threadIndex ->
        {
            PublicAPI publicAPI = getPublicAPI(apiServer, null, null, null);
            String sessionToken = publicAPI.login("user", "password");
            assertNotNull(sessionToken);
        });
    }

    @Test
    public void non_interactive_sessions_with_different_session_tokens_should_work() throws Exception
    {
        non_interactive_sessions_with_session_tokens(() -> UUID.randomUUID().toString());
    }

    @Test
    public void non_interactive_sessions_with_the_same_session_token_should_work() throws Exception
    {
        String sessionToken = UUID.randomUUID().toString();
        non_interactive_sessions_with_session_tokens(() -> sessionToken);
    }

    private void non_interactive_sessions_with_session_tokens(Supplier<String> sessionTokenSupplier) throws Exception
    {
        APIServer apiServer = getAPIServer();

        run_in_multiple_threads(threadIndex ->
        {
            String sessionToken = sessionTokenSupplier.get();

            PublicAPI publicAPI = getPublicAPI(apiServer, null, null, sessionToken);
            byte[] data = ("file-content-" + threadIndex).getBytes(StandardCharsets.UTF_8);
            publicAPI.write(new Chunk[] { new Chunk(owner, "file_" + threadIndex, 0L, data.length, data) });
            logger.info("Thread " + threadIndex + " wrote chunk");
            Chunk[] readData =
                    publicAPI.read(new Chunk[] { new Chunk(owner, "file_" + threadIndex, 0L, data.length, ChunkEncoderDecoder.EMPTY_ARRAY) });
            logger.info("Thread " + threadIndex + " read chunk");

            assertEquals(1, readData.length);
            assertArrayEquals(data, readData[0].getData());
        });
    }

    @Test
    public void interactive_session_without_session_token_should_fail() throws Exception
    {
        APIServer apiServer = getAPIServer();
        PublicAPI publicAPI = getPublicAPI(apiServer, "1234", null, null);

        try
        {
            publicAPI.begin(UUID.randomUUID());
            fail();
        } catch (Exception e)
        {
            assertTrue(e.getMessage(), e.getMessage().contains("Session null doesn't exist"));
        }
    }

    @Test
    public void interactive_sessions_with_different_session_tokens_should_work() throws Exception
    {
        APIServer apiServer = getAPIServer();

        run_in_multiple_threads(threadIndex ->
        {
            String sessionToken = UUID.randomUUID().toString();

            PublicAPI publicAPI = getPublicAPI(apiServer, "1234", null, sessionToken);

            apiServer.runIdleWorkerCleanupTask();

            publicAPI.begin(UUID.randomUUID());
            byte[] data = ("file-content-" + threadIndex).getBytes(StandardCharsets.UTF_8);
            publicAPI.write(new Chunk[] { new Chunk(owner, "file_" + threadIndex, 0L, data.length, data) });
            logger.info("Thread " + threadIndex + " wrote first chunk");

            apiServer.runIdleWorkerCleanupTask();

            publicAPI.write(new Chunk[] { new Chunk(owner, "file_" + threadIndex, (long) data.length, data.length, data) });
            logger.info("Thread " + threadIndex + " wrote second chunk");
            publicAPI.commit();

            apiServer.runIdleWorkerCleanupTask();

            PublicAPI publicAPI2 = getPublicAPI(apiServer, null, null, sessionToken);
            Chunk[] readData = publicAPI2.read(new Chunk[] {
                    new Chunk(owner, "file_" + threadIndex, 0L, data.length, ChunkEncoderDecoder.EMPTY_ARRAY) });
            logger.info("Thread " + threadIndex + " read first chunk");

            apiServer.runIdleWorkerCleanupTask();

            Chunk[] readData2 = publicAPI2.read(new Chunk[] {
                    new Chunk(owner, "file_" + threadIndex, (long) data.length, data.length, ChunkEncoderDecoder.EMPTY_ARRAY) });
            logger.info("Thread " + threadIndex + " read second chunk");

            apiServer.runIdleWorkerCleanupTask();

            assertEquals(1, readData.length);
            assertEquals(1, readData2.length);
            assertArrayEquals(data, readData[0].getData());
            assertArrayEquals(data, readData2[0].getData());
        });
    }

    @Test
    public void interactive_sessions_with_the_same_session_token_should_fail()
    {
        String sessionToken = UUID.randomUUID().toString();

        try
        {
            APIServer apiServer = getAPIServer();

            run_in_multiple_threads(threadIndex ->
            {
                PublicAPI publicAPI = getPublicAPI(apiServer, "1234", null, sessionToken);
                UUID transactionId = UUID.randomUUID();
                // The same session token shared across different threads tries to begin multiple transactions. Depending on the timing,
                // it may either fail with one error or another.
                publicAPI.begin(transactionId);
            });
            fail();
        } catch (Exception e)
        {
            assertTrue(e.getMessage(),
                    e.getMessage().contains(
                            "Session '" + sessionToken + "' is already being used by another concurrent operation within the same transaction.")
                            || e.getMessage().contains("Session '" + sessionToken + "' is already being used by another transaction"));
        }
    }

    @Test
    public void interactive_session_trying_to_begin_two_transactions_should_fail() throws Exception
    {
        String sessionToken = UUID.randomUUID().toString();

        UUID transactionId1 = UUID.randomUUID();
        UUID transactionId2 = UUID.randomUUID();

        try
        {
            APIServer apiServer = getAPIServer();
            PublicAPI publicAPI = getPublicAPI(apiServer, "1234", null, sessionToken);
            publicAPI.begin(transactionId1);
            // Starting another transaction for the same session token is not allowed
            publicAPI.begin(transactionId2);
            fail();
        } catch (Exception e)
        {
            assertTrue(e.getMessage(), e.getMessage()
                    .contains("Session '" + sessionToken + "' is already being used by another transaction (" + transactionId1 + ")."));
        }
    }

    @Test
    public void interactive_session_recovery_should_work() throws Exception
    {
        APIServer apiServer = getAPIServer();

        run_in_multiple_threads(threadIndex ->
        {
            UUID transactionId = UUID.randomUUID();

            apiServer.runIdleWorkerCleanupTask();

            // Begin a transaction and leave it unfinished
            PublicAPI transactionPublicAPI = getPublicAPI(apiServer, "1234", null, UUID.randomUUID().toString());
            transactionPublicAPI.begin(transactionId);
            byte[] data = ("file-content-" + threadIndex).getBytes(StandardCharsets.UTF_8);
            transactionPublicAPI.write(new Chunk[] { new Chunk(owner, "file_" + threadIndex, 0L, data.length, data) });
            logger.info("Thread " + threadIndex + " wrote the chunk");

            apiServer.runIdleWorkerCleanupTask();

            // Recover and commit the transaction using a different session token (in real life it will be system user)
            PublicAPI recoveryPublicAPI = getPublicAPI(apiServer, "1234", "5678", UUID.randomUUID().toString());
            recoveryPublicAPI.begin(transactionId);
            recoveryPublicAPI.commit();
            logger.info("Thread " + threadIndex + " commited");

            apiServer.runIdleWorkerCleanupTask();

            // Check the changes can be read after commit outside the transaction
            PublicAPI checkPublicAPI = getPublicAPI(apiServer, null, null, UUID.randomUUID().toString());
            Chunk[] readData = checkPublicAPI.read(new Chunk[] {
                    new Chunk(owner, "file_" + threadIndex, 0L, data.length, ChunkEncoderDecoder.EMPTY_ARRAY) });
            logger.info("Thread " + threadIndex + " read the chunk");

            apiServer.runIdleWorkerCleanupTask();

            assertEquals(1, readData.length);
            assertArrayEquals(data, readData[0].getData());
        });
    }

    private <T> void run_in_multiple_threads(ThreadCallable threadCallable) throws Exception
    {
        final int numberOfThreads = 100;
        final CyclicBarrier barrier = new CyclicBarrier(numberOfThreads);
        final List<Future<T>> futures = new ArrayList<>();

        try (ExecutorService threadPool = Executors.newFixedThreadPool(numberOfThreads))
        {
            for (int i = 0; i < numberOfThreads; i++)
            {
                final int threadIndex = i;
                futures.add(threadPool.submit(() ->
                {
                    barrier.await();
                    logger.info("Thread " + threadIndex + " crossed the barrier");

                    for(int j = 0 ; j < 100; j++)
                    {
                        threadCallable.call(threadIndex);
                    }
                    return null;
                }));
            }
        }

        for (Future<T> future : futures)
        {
            future.get();
        }
    }

    private interface ThreadCallable
    {
        void call(int threadIndex) throws Exception;
    }

    protected APIServer getAPIServer() throws Exception
    {
        Configuration configuration = ServerClientEnvironmentFS.getInstance().getDefaultServerConfiguration();

        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.init(configuration);

        WorkerFactory workerFactory = new WorkerFactory();
        int poolSize = configuration.getIntegerProperty(AtomicFileSystemServerParameter.poolSize);

        Pool<Configuration, TransactionConnection> connectionsPool = new Pool<>(poolSize, configuration, connectionFactory);
        Pool<Configuration, Worker> workersPool = new Pool<>(poolSize, configuration, workerFactory);

        OperationResultCache operationResultCache = new OperationResultCache();
        operationResultCache.init(configuration);

        String interactiveSessionKey = configuration.getStringProperty(AtomicFileSystemServerParameter.apiServerInteractiveSessionKey);
        String transactionManagerKey = configuration.getStringProperty(AtomicFileSystemServerParameter.apiServerTransactionManagerKey);
        int apiServerWorkerTimeout = configuration.getIntegerProperty(AtomicFileSystemServerParameter.apiServerWorkerTimeout);
        TrashRootProvider trashRootProvider = configuration.getSharableInstance(AtomicFileSystemServerParameter.trashRootProviderClass);
        trashRootProvider.init(configuration);

        DummyServerObserver observer = new DummyServerObserver();
        observer.init(configuration);
        APIServer apiServer =
                new APIServer(connectionsPool, workersPool, operationResultCache, PublicAPI.class, interactiveSessionKey, transactionManagerKey,
                        apiServerWorkerTimeout, observer);
        observer.init(apiServer, configuration);
        return apiServer;
    }

}