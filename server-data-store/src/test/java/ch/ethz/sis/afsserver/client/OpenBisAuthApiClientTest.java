/*
 *  Copyright ETH 2023 Zürich, Scientific IT Services
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package ch.ethz.sis.afsserver.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.ethz.sis.afsapi.dto.ExceptionReason;
import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afsapi.exception.ThrowableReason;
import ch.ethz.sis.afsserver.server.Server;
import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameter;
import ch.ethz.sis.openbis.generic.asapi.v3.ITransactionCoordinatorApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.PhysicalData;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.create.DataSetCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.id.DataStorePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.rights.Right;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.rights.Rights;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SamplePermId;
import ch.ethz.sis.shared.io.IOUtils;
import ch.ethz.sis.shared.startup.Configuration;
import ch.systemsx.cisd.common.exceptions.UserFailureException;

public class OpenBisAuthApiClientTest extends BaseApiClientTest
{

    /**
     * Match this value with openBISUrl from properties file
     */
    private static final int OPENBIS_DUMMY_SERVER_PORT = 8084;

    private static final String OPENBIS_DUMMY_SERVER_PATH = "/";

    private static final String TRANSACTION_MANAGER_KEY = "5678";

    private static final String INTERACTIVE_SESSION_KEY = "1234";

    private static final String SHARE_1 = "1";

    private static final String SHARE_2 = "2";

    private static final String SHARE_3 = "3";

    private static final String TEST_FILE_1 = "test-file-1";

    private static final String TEST_FILE_2 = "test-folder/test-file-2";

    private static final String TEST_FILE_3 = "test-folder-2/test-file-3";

    private static final String TEST_CONTENT_A = "test-content-a";

    private static final String TEST_CONTENT_AB = "test-content-ab";

    private static final String TEST_CONTENT_ABC = "test-content-abc";

    private static int authenticationProxyCacheIdleTimeout;

    private static int authorizationProxyCacheIdleTimeout;

    private DummyOpenBisServer dummyOpenBisServer;

    // used for creating test data for the super class tests
    @Override protected String getTestDataFolder(final String owner)
    {
        return getTestDataFolder(SHARE_1, owner);
    }

    public String getTestDataFolder(final String shareId, String owner)
    {
        return shareId + "/" + storageUuid + "/" + String.join("/", IOUtils.getShards(owner.toUpperCase())) + "/"
                + owner.toUpperCase();
    }

    public void createTestDataFile(String shareId, String owner, String source, byte[] data) throws Exception
    {
        String testDataRoot = IOUtils.getPath(storageRoot, getTestDataFolder(shareId, owner));
        String testDataFile = IOUtils.getPath(testDataRoot, source);
        IOUtils.createDirectories(new java.io.File(testDataFile).getParent());
        IOUtils.createFile(testDataFile);
        IOUtils.write(testDataFile, 0, data);
    }

    @BeforeClass
    public static void classSetUp() throws Exception
    {
        final Configuration configuration =
                new Configuration(List.of(AtomicFileSystemServerParameter.class),
                        "src/test/resources/test-server-with-auth-config.properties");
        httpServerPort =
                configuration.getIntegerProperty(AtomicFileSystemServerParameter.httpServerPort);
        httpServerPath =
                configuration.getStringProperty(AtomicFileSystemServerParameter.httpServerUri);
        storageRoot = configuration.getStringProperty(AtomicFileSystemServerParameter.storageRoot);
        storageUuid = configuration.getStringProperty(AtomicFileSystemServerParameter.storageUuid);

        authenticationProxyCacheIdleTimeout = configuration.getIntegerProperty(AtomicFileSystemServerParameter.authenticationProxyCacheIdleTimeout);
        authorizationProxyCacheIdleTimeout = configuration.getIntegerProperty(AtomicFileSystemServerParameter.authorizationProxyCacheIdleTimeout);

        // create 3 shares for AFS to have something to loop over in search for experiment/sample location
        IOUtils.createDirectories(storageRoot + "/" + SHARE_1);
        IOUtils.createDirectories(storageRoot + "/" + SHARE_2);
        IOUtils.createDirectories(storageRoot + "/" + SHARE_3);

        afsServer = new Server<>(configuration);
    }

    @Before
    public void setUpDummyOpenBis() throws Exception
    {
        dummyOpenBisServer =
                new DummyOpenBisServer(OPENBIS_DUMMY_SERVER_PORT, OPENBIS_DUMMY_SERVER_PATH);
        dummyOpenBisServer.setOperationExecutor(getDefaultOperationExecutor());
        dummyOpenBisServer.start();
    }

    @After
    public void tearDownDummyOpenBis()
    {
        dummyOpenBisServer.stop();
    }

    @Test
    public void list_callFailsDueToMissingPermissions() throws Exception
    {
        login();

        Thread.sleep(authenticationProxyCacheIdleTimeout+1000);

        dummyOpenBisServer.setOperationExecutor((url, methodName, methodArguments) ->
        {
            switch (methodName)
            {
                case "getExperiments":
                case "getSamples":
                case "getDataSets":
                    return Map.of();
            }

            return getDefaultOperationExecutor().executeOperation(url, methodName, methodArguments);
        });

        try
        {
            afsClient.list(owner, "", Boolean.TRUE);
            fail();
        } catch (Exception e)
        {
            ThrowableReason reason = (ThrowableReason) e.getCause();
            String message = ((ExceptionReason) reason.getReason()).getMessage();
            assertTrue(message.matches(
                    "(?s).*Session .* don't have rights \\[Read\\] over .*to perform the operation List(?s).*"));
        }
    }

    @Test
    public void list_failsDueToExpiredSession() throws Exception
    {
        login();

        Thread.sleep(authenticationProxyCacheIdleTimeout+1000);

        dummyOpenBisServer.setOperationExecutor((url, methodName, methodArguments) ->
        {
            switch (methodName)
            {
                case "isSessionActive":
                    return false;
            }

            throw new UnsupportedOperationException(methodName, methodArguments);
        });

        try
        {
            afsClient.list(owner, "", Boolean.TRUE);
            fail();
        } catch (Exception e)
        {
            ThrowableReason reason = (ThrowableReason) e.getCause();
            String message = ((ExceptionReason) reason.getReason()).getMessage();
            assertTrue(message.matches("(?s).*Session .* doesn't exist(?s).*"));
        }
    }

    @Test
    public void list_withExperimentOwner() throws Exception
    {
        login();

        String testExperiment = UUID.randomUUID().toString();

        // create data for the owner in both share 2 and 3 (the first found should be returned)
        createTestDataFile(SHARE_2, testExperiment, TEST_FILE_1, TEST_CONTENT_A.getBytes());
        createTestDataFile(SHARE_2, testExperiment, TEST_FILE_2, TEST_CONTENT_AB.getBytes());
        createTestDataFile(SHARE_3, testExperiment, TEST_FILE_3, TEST_CONTENT_ABC.getBytes());

        dummyOpenBisServer.setOperationExecutor((url, methodName, methodArguments) ->
        {
            switch (methodName)
            {
                // only the default implementation of "getExperiments" will return results
                case "getSamples":
                case "getDataSets":
                    return Map.of();
            }

            return getDefaultOperationExecutor().executeOperation(url, methodName, methodArguments);
        });

        List<File> files = afsClient.list(testExperiment, "", Boolean.TRUE);
        assertEquals(3, files.size());

        files = sortFiles(files);
        assertFileEquals(files.get(0), testExperiment, "/test-file-1", "test-file-1", false, (long) TEST_CONTENT_A.getBytes().length);
        assertFileEquals(files.get(1), testExperiment, "/test-folder", "test-folder", true, null);
        assertFileEquals(files.get(2), testExperiment, "/test-folder/test-file-2", "test-file-2", false,
                (long) TEST_CONTENT_AB.getBytes().length);
    }

    @Test
    public void list_withSampleOwner() throws Exception
    {
        login();

        String testSample = UUID.randomUUID().toString();

        // create data for the owner in both share 2 and 3 (the first found should be returned)
        createTestDataFile(SHARE_2, testSample, TEST_FILE_1, TEST_CONTENT_A.getBytes());
        createTestDataFile(SHARE_2, testSample, TEST_FILE_2, TEST_CONTENT_AB.getBytes());
        createTestDataFile(SHARE_3, testSample, TEST_FILE_3, TEST_CONTENT_ABC.getBytes());

        dummyOpenBisServer.setOperationExecutor((url, methodName, methodArguments) ->
        {
            switch (methodName)
            {
                // only the default implementation of "getSamples" will return results
                case "getExperiments":
                case "getDataSets":
                    return Map.of();
            }

            return getDefaultOperationExecutor().executeOperation(url, methodName, methodArguments);
        });

        List<File> files = afsClient.list(testSample, "", Boolean.TRUE);
        assertEquals(3, files.size());

        files = sortFiles(files);
        assertFileEquals(files.get(0), testSample, "/test-file-1", "test-file-1", false, (long) TEST_CONTENT_A.getBytes().length);
        assertFileEquals(files.get(1), testSample, "/test-folder", "test-folder", true, null);
        assertFileEquals(files.get(2), testSample, "/test-folder/test-file-2", "test-file-2", false,
                (long) TEST_CONTENT_AB.getBytes().length);
    }

    @Test
    public void list_withDataSetOwner() throws Exception
    {
        login();

        String testDataSet = UUID.randomUUID().toString();

        // create data for the owner in both share 2 and 3 (the share returned by V3 API getDataSets call is used)
        createTestDataFile(SHARE_2, testDataSet, TEST_FILE_1, TEST_CONTENT_A.getBytes());
        createTestDataFile(SHARE_2, testDataSet, TEST_FILE_2, TEST_CONTENT_AB.getBytes());
        createTestDataFile(SHARE_3, testDataSet, TEST_FILE_3, TEST_CONTENT_ABC.getBytes());

        dummyOpenBisServer.setOperationExecutor((url, methodName, methodArguments) ->
        {
            switch (methodName)
            {
                // only the default implementation of "getDataSets" will return results
                case "getExperiments":
                case "getSamples":
                    return Map.of();
            }

            return getDefaultOperationExecutor().executeOperation(url, methodName, methodArguments);
        });

        List<File> files = afsClient.list(testDataSet, "", Boolean.TRUE);
        assertEquals(2, files.size());

        files = sortFiles(files);
        assertFileEquals(files.get(0), testDataSet, "/test-folder-2", "test-folder-2", true, null);
        assertFileEquals(files.get(1), testDataSet, "/test-folder-2/test-file-3", "test-file-3", false, (long) TEST_CONTENT_ABC.getBytes().length);
    }

    @Test
    public void free_callFailsDueToMissingPermissions() throws Exception
    {
        login();

        Thread.sleep(authenticationProxyCacheIdleTimeout+1000);

        dummyOpenBisServer.setOperationExecutor((url, methodName, methodArguments) ->
        {
            switch (methodName)
            {
                case "getExperiments":
                case "getSamples":
                case "getDataSets":
                    return Map.of();
            }

            return getDefaultOperationExecutor().executeOperation(url, methodName, methodArguments);
        });

        try
        {
            afsClient.free(owner, "");
            fail();
        } catch (Exception e)
        {
            ThrowableReason reason = (ThrowableReason) e.getCause();
            String message = ((ExceptionReason) reason.getReason()).getMessage();
            assertTrue(message.matches(
                    "(?s).*Session .* don't have rights \\[Read\\] over .*to perform the operation Free(?s).*"));
        }
    }

    @Test
    public void free_failsDueToExpiredSession() throws Exception
    {
        login();

        Thread.sleep(authenticationProxyCacheIdleTimeout+1000);

        dummyOpenBisServer.setOperationExecutor((url, methodName, methodArguments) ->
        {
            switch (methodName)
            {
                case "isSessionActive":
                    return false;
            }

            throw new UnsupportedOperationException(methodName, methodArguments);
        });

        try
        {
            afsClient.free(owner, "");
            fail();
        } catch (Exception e)
        {
            ThrowableReason reason = (ThrowableReason) e.getCause();
            String message = ((ExceptionReason) reason.getReason()).getMessage();
            assertTrue(message.matches("(?s).*Session .* doesn't exist(?s).*"));
        }
    }

    @Test
    public void write_failsDueToMissingPermission_noFileCreated() throws Exception
    {
        login();

        Thread.sleep(authorizationProxyCacheIdleTimeout+1000);

        dummyOpenBisServer.setOperationExecutor((url, methodName, methodArguments) ->
        {
            switch (methodName)
            {
                case "getRights":
                    Object param = ((List<?>) methodArguments[1]).get(0);
                    return Map.of(param, new Rights(Set.of()));
            }

            return getDefaultOperationExecutor().executeOperation(url, methodName, methodArguments);
        });

        try
        {
            afsClient.write(owner, FILE_B, 0L, DATA);
            fail();
        } catch (Exception e)
        {
            ThrowableReason reason = (ThrowableReason) e.getCause();
            String message = ((ExceptionReason) reason.getReason()).getMessage();
            assertTrue(message.matches(
                    "(?s).*Session .* don't have rights \\[Write\\] over .* to perform the operation Write(?s).*"));
        }

        assertFalse(IOUtils.exists(IOUtils.getPath(testDataRoot, FILE_B)));
    }

    @Test
    public void write_withDataSetOwner_failsDueToMissingPermission_noFileCreated() throws Exception
    {
        login();

        Thread.sleep(authorizationProxyCacheIdleTimeout+1000);

        dummyOpenBisServer.setOperationExecutor((url, methodName, methodArguments) ->
        {
            switch (methodName)
            {
                case "getExperiments":
                case "getSamples":
                    // only the default implementation of "getDataSets" will return results
                    return Map.of();
                case "getRights":
                    Object param = ((List<?>) methodArguments[1]).get(0);
                    // these are rights to manipulate data set metadata, still the data set files should be immutable
                    return Map.of(param, new Rights(Set.of(Right.CREATE, Right.UPDATE, Right.DELETE)));
            }

            return getDefaultOperationExecutor().executeOperation(url, methodName, methodArguments);
        });

        try
        {
            afsClient.write(owner, FILE_B, 0L, DATA);
            fail();
        } catch (Exception e)
        {
            ThrowableReason reason = (ThrowableReason) e.getCause();
            String message = ((ExceptionReason) reason.getReason()).getMessage();
            assertTrue(message.matches(
                    "(?s).*Session .* don't have rights \\[Write\\] over .* to perform the operation Write(?s).*"));
        }

        assertFalse(IOUtils.exists(IOUtils.getPath(testDataRoot, FILE_B)));
    }

    @Test
    public void write_createsDataSetInASThatDoesNotExist() throws Exception
    {
        login();

        String ownerWithoutFiles = UUID.randomUUID().toString();

        List<DataSetCreation> dataSetCreations = new ArrayList<>();

        dummyOpenBisServer.setOperationExecutor((url, methodName, methodArguments) ->
        {
            switch (methodName)
            {
                case "createDataSets":
                    dataSetCreations.addAll((List<DataSetCreation>) methodArguments[1]);
                    return List.of(new DataSetPermId(ownerWithoutFiles));
            }

            return getDefaultOperationExecutor().executeOperation(url, methodName, methodArguments);
        });

        afsClient.write(ownerWithoutFiles, FILE_A, 0L, DATA);

        // dataset gets created on first write
        assertEquals(1, dataSetCreations.size());
        assertEquals(ownerWithoutFiles, dataSetCreations.get(0).getCode());
        assertEquals(new DataStorePermId("AFS"), dataSetCreations.get(0).getDataStoreId());

        afsClient.write(ownerWithoutFiles, FILE_B, 0L, DATA);

        // no more datasets get created on subsequent writes
        assertEquals(1, dataSetCreations.size());
    }

    @Test
    public void write_createsDataSetInASThatAlreadyExists() throws Exception
    {
        login();

        String ownerWithoutFiles = UUID.randomUUID().toString();

        dummyOpenBisServer.setOperationExecutor((url, methodName, methodArguments) ->
        {
            switch (methodName)
            {
                case "createDataSets":
                    throw new DataSetAlreadyExistsException();
            }

            return getDefaultOperationExecutor().executeOperation(url, methodName, methodArguments);
        });

        // even though AS throws an exception this does not fail
        afsClient.write(ownerWithoutFiles, FILE_A, 0L, DATA);
    }

    @Test
    public void write_createsDataSetInAS_1PCTransaction_commitWithASSuccess() throws Exception
    {
        write_createsDataSetInAS_1PCTransaction(false, false);
    }

    @Test
    public void write_createsDataSetInAS_1PCTransaction_commitWithASFailure() throws Exception
    {
        write_createsDataSetInAS_1PCTransaction(false, true);
    }

    @Test
    public void write_createsDataSetInAS_1PCTransaction_rollbackWithASSuccess() throws Exception
    {
        write_createsDataSetInAS_1PCTransaction(true, false);
    }

    @Test
    public void write_createsDataSetInAS_1PCTransaction_rollbackWithASFailure() throws Exception
    {
        write_createsDataSetInAS_1PCTransaction(true, true);
    }

    private void write_createsDataSetInAS_1PCTransaction(boolean rollbackTransaction, boolean dataSetCreationFails) throws Exception
    {
        login();

        String ownerWithoutFiles = UUID.randomUUID().toString();
        Path ownerFolderPath = Path.of(IOUtils.getPath(storageRoot, getTestDataFolder(ownerWithoutFiles.toUpperCase())));

        MutableObject<Boolean> dataSetCreationFailsMutable = new MutableObject<>(dataSetCreationFails);
        List<DataSetCreation> dataSetCreations = new ArrayList<>();

        dummyOpenBisServer.setOperationExecutor((url, methodName, methodArguments) ->
        {
            switch (methodName)
            {
                case "createDataSets":
                    if (dataSetCreationFailsMutable.getValue())
                    {
                        throw new RuntimeException();
                    } else if (!dataSetCreations.isEmpty())
                    {
                        throw new DataSetAlreadyExistsException();
                    } else
                    {
                        dataSetCreations.addAll((List<DataSetCreation>) methodArguments[1]);
                        return List.of(new DataSetPermId(ownerWithoutFiles));
                    }
            }

            return getDefaultOperationExecutor().executeOperation(url, methodName, methodArguments);
        });

        UUID transactionId = UUID.randomUUID();
        afsClient.setInteractiveSessionKey(INTERACTIVE_SESSION_KEY);

        afsClient.begin(transactionId);
        assertEquals(0, dataSetCreations.size());

        afsClient.write(ownerWithoutFiles, FILE_A, 0L, DATA);
        assertEquals(0, dataSetCreations.size());

        if (rollbackTransaction)
        {
            afsClient.rollback();

            // no datasets get created on rollback
            assertEquals(0, dataSetCreations.size());

            // no files were stored
            assertFalse(Files.exists(ownerFolderPath));
        } else
        {
            if (dataSetCreationFails)
            {
                try
                {
                    afsClient.commit();
                    fail();
                } catch (Exception e)
                {
                    // no datasets get created on commit if AS fails
                    assertEquals(0, dataSetCreations.size());

                    // no files were stored
                    assertFalse(Files.exists(ownerFolderPath));
                }
            } else
            {
                afsClient.commit();

                // dataset gets created on commit (if the commit itself would fail then the dataset unfortunately would stay)
                assertEquals(1, dataSetCreations.size());
                assertEquals(ownerWithoutFiles.toUpperCase(), dataSetCreations.get(0).getCode());
                assertEquals(new DataStorePermId("AFS"), dataSetCreations.get(0).getDataStoreId());

                // files were stored
                assertTrue(Files.exists(ownerFolderPath));
            }
        }

        // make AS data set creation succeed
        dataSetCreationFailsMutable.setValue(false);

        UUID transactionId2 = UUID.randomUUID();

        afsClient.begin(transactionId2);
        afsClient.write(ownerWithoutFiles, FILE_B, 0L, DATA);
        afsClient.commit();

        // no more than 1 dataset gets created
        assertEquals(1, dataSetCreations.size());
        assertTrue(Files.exists(ownerFolderPath));
    }

    @Test
    public void write_createsDataSetInAS_2PCTransaction_commitWithASSuccess() throws Exception
    {
        write_createsDataSetInAS_2PCTransaction(false, false);
    }

    @Test
    public void write_createsDataSetInAS_2PCTransaction_commitWithASFailure() throws Exception
    {
        write_createsDataSetInAS_2PCTransaction(false, true);
    }

    @Test
    public void write_createsDataSetInAS_2PCTransaction_rollbackWithASSuccess() throws Exception
    {
        write_createsDataSetInAS_2PCTransaction(true, false);
    }

    @Test
    public void write_createsDataSetInAS_2PCTransaction_rollbackWithASFailure() throws Exception
    {
        write_createsDataSetInAS_2PCTransaction(true, true);
    }

    private void write_createsDataSetInAS_2PCTransaction(boolean rollbackTransaction, boolean dataSetCreationFails) throws Exception
    {
        login();

        String ownerWithoutFiles = UUID.randomUUID().toString();
        Path ownerFolderPath = Path.of(IOUtils.getPath(storageRoot, getTestDataFolder(ownerWithoutFiles.toUpperCase())));

        MutableObject<Boolean> dataSetCreationFailsMutable = new MutableObject<>(dataSetCreationFails);
        List<DataSetCreation> dataSetCreations = new ArrayList<>();

        dummyOpenBisServer.setOperationExecutor((url, methodName, methodArguments) ->
        {
            if (url.endsWith(ITransactionCoordinatorApi.SERVICE_URL))
            {
                if (methodName.equals("executeOperation"))
                {
                    String operationName = (String) methodArguments[4];
                    Object[] operationArguments = (Object[]) methodArguments[5];

                    if (operationName.equals("createDataSets"))
                    {
                        if (dataSetCreationFailsMutable.getValue())
                        {
                            throw new RuntimeException();
                        } else if (!dataSetCreations.isEmpty())
                        {
                            throw new DataSetAlreadyExistsException();
                        } else
                        {
                            dataSetCreations.addAll((List<DataSetCreation>) operationArguments[1]);
                            return List.of(new DataSetPermId(ownerWithoutFiles));
                        }
                    }

                    return getDefaultOperationExecutor().executeOperation(url, operationName, operationArguments);
                }

                throw new UnsupportedOperationException(methodName, methodArguments);
            } else
            {
                return getDefaultOperationExecutor().executeOperation(url, methodName, methodArguments);
            }
        });

        UUID transactionId = UUID.randomUUID();
        afsClient.setTransactionManagerKey(TRANSACTION_MANAGER_KEY);
        afsClient.setInteractiveSessionKey(INTERACTIVE_SESSION_KEY);

        afsClient.begin(transactionId);
        assertEquals(0, dataSetCreations.size());

        try
        {
            afsClient.write(ownerWithoutFiles, FILE_A, 0L, DATA);

            if (dataSetCreationFails)
            {
                fail();
            } else
            {
                assertEquals(1, dataSetCreations.size());
                assertEquals(ownerWithoutFiles.toUpperCase(), dataSetCreations.get(0).getCode().toUpperCase());
                assertEquals(new DataStorePermId("AFS"), dataSetCreations.get(0).getDataStoreId());
                assertFalse(Files.exists(ownerFolderPath));
            }
        } catch (Exception e)
        {
            if (dataSetCreationFails)
            {
                assertEquals(0, dataSetCreations.size());
                assertFalse(Files.exists(ownerFolderPath));
            } else
            {
                throw e;
            }
        }

        afsClient.prepare();

        if (rollbackTransaction)
        {
            afsClient.rollback();
            assertFalse(Files.exists(ownerFolderPath));
        } else
        {
            afsClient.commit();

            if (dataSetCreationFails)
            {
                assertFalse(Files.exists(ownerFolderPath));
            } else
            {
                assertTrue(Files.exists(ownerFolderPath));
            }
        }

        // make AS data set creation succeed
        dataSetCreationFailsMutable.setValue(false);

        UUID transactionId2 = UUID.randomUUID();

        afsClient.begin(transactionId2);
        afsClient.write(ownerWithoutFiles, FILE_B, 0L, DATA);
        afsClient.prepare();
        afsClient.commit();

        // no more than 1 dataset gets created
        assertEquals(1, dataSetCreations.size());
        assertTrue(Files.exists(ownerFolderPath));
    }

    @Test
    public void move_failsDueToMissingPermissions() throws Exception
    {
        login();

        Thread.sleep(authorizationProxyCacheIdleTimeout+1000);

        dummyOpenBisServer.setOperationExecutor((url, methodName, methodArguments) ->
        {
            switch (methodName)
            {
                case "getRights":
                    Object param = ((List<?>) methodArguments[1]).get(0);
                    return Map.of(param, new Rights(Set.of()));
            }

            return getDefaultOperationExecutor().executeOperation(url, methodName, methodArguments);
        });

        try
        {
            afsClient.move(owner, FILE_A, owner, FILE_B);
            fail();
        } catch (Exception e)
        {
            ThrowableReason reason = (ThrowableReason) e.getCause();
            String message = ((ExceptionReason) reason.getReason()).getMessage();
            assertTrue(message.matches(
                    "(?s).*Session .* don't have rights \\[(Write|Read), (Write|Read)\\] over .* to perform the operation Move(?s).*"));
        }
        assertFalse(IOUtils.exists(IOUtils.getPath(testDataRoot, FILE_B)));
    }

    private DummyOpenBisServer.OperationExecutor getDefaultOperationExecutor()
    {
        return (url, methodName, methodArguments) ->
        {
            switch (methodName)
            {
                case "login":
                    return "test-login-token";
                case "logout":
                    return null;
                case "isSessionActive":
                    return true;
                case "getSamples":
                    Object sampleId = ((List<?>) methodArguments[1]).get(0);

                    Sample sample = new Sample();
                    sample.setPermId((SamplePermId) sampleId);

                    return Map.of(sampleId, sample);
                case "getExperiments":
                    Object experimentId = ((List<?>) methodArguments[1]).get(0);

                    Experiment experiment = new Experiment();
                    experiment.setPermId((ExperimentPermId) experimentId);

                    return Map.of(experimentId, experiment);
                case "getDataSets":
                    Object dataSetId = ((List<?>) methodArguments[1]).get(0);

                    DataSet dataSet = new DataSet();
                    dataSet.setPermId((DataSetPermId) dataSetId);

                    PhysicalData physicalData = new PhysicalData();
                    physicalData.setShareId(SHARE_3);
                    physicalData.setLocation(
                            storageUuid + "/" + String.join("/", IOUtils.getShards(dataSet.getPermId().toString())) + "/" + dataSet.getPermId()
                                    .toString());
                    dataSet.setPhysicalData(physicalData);

                    DataSetFetchOptions fo = new DataSetFetchOptions();
                    fo.withPhysicalData();
                    dataSet.setFetchOptions(fo);

                    return Map.of(dataSetId, dataSet);
                case "getRights":
                    Object param = ((List<?>) methodArguments[1]).get(0);
                    return Map.of(param, new Rights(Set.of(Right.UPDATE)));
                case "searchEvents":
                    return new SearchResult<>(Collections.emptyList(), 0);
                case "getSessionInformation":
                    return null;
            }

            throw new UnsupportedOperationException(methodName, methodArguments);
        };
    }

    private static class UnsupportedOperationException extends RuntimeException
    {
        public UnsupportedOperationException(String methodName, Object[] methodArguments)
        {
            super("Method: '" + methodName + ", arguments: '" + Arrays.toString(methodArguments) + "'");
        }
    }

    private static class DataSetAlreadyExistsException extends UserFailureException
    {
        public DataSetAlreadyExistsException()
        {
            super("DataSet already exists in the database and needs to be unique");
        }
    }

}
