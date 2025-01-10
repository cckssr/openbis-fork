package ch.ethz.sis.transaction;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;
import static org.testng.AssertJUnit.assertNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import org.apache.commons.lang3.mutable.MutableObject;
import org.hamcrest.Matcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TransactionParticipantTest
{

    public static final String TEST_PARTICIPANT_ID = "test-participant-id";

    public static final UUID TEST_TRANSACTION_ID = UUID.randomUUID();

    public static final UUID TEST_TRANSACTION_ID_2 = UUID.randomUUID();

    public static final Object TEST_TRANSACTION = new Object();

    public static final Object TEST_TRANSACTION_2 = new Object();

    public static final String TEST_TRANSACTION_COORDINATOR_KEY = "test-transaction-coordinator-key";

    public static final String TEST_INTERACTIVE_SESSION_KEY = "test-interactive-session-key";

    public static final String TEST_SESSION_TOKEN = "test-session-token";

    public static final String TEST_SESSION_TOKEN_2 = "test-session-token-2";

    public static final String INVALID_TRANSACTION_COORDINATOR_KEY = "invalid-transaction-coordinator-key";

    public static final String INVALID_INTERACTIVE_SESSION_KEY = "invalid-interactive-session-key";

    public static final String INVALID_SESSION_TOKEN = "invalid-session-token";

    public static final String TEST_OPERATION_NAME = "test-operation";

    public static final String TEST_OPERATION_NAME_2 = "test-operation-2";

    public static final Object[] TEST_OPERATION_ARGUMENTS = new Object[] { "test-argument", 1 };

    public static final Object[] TEST_OPERATION_ARGUMENTS_2 = new Object[] { "test-argument-2", 2 };

    public static final String TEST_OPERATION_RESULT = "test-result";

    public static final String TEST_OPERATION_RESULT_2 = "test-result-2";

    public static final RuntimeException TEST_UNCHECKED_EXCEPTION = new RuntimeException("Test unchecked exception");

    public static final Exception TEST_CHECKED_EXCEPTION = new Exception("Test checked exception");

    public static final int TEST_TRANSACTION_TIMEOUT = 60;

    public static final int TEST_THREAD_COUNT_LIMIT = 5;

    private Mockery mockery;

    private IDatabaseTransactionProvider databaseTransactionProvider;

    private ISessionTokenProvider sessionTokenProvider;

    private ITransactionOperationExecutor transactionOperationExecutor;

    private ITransactionLog transactionLog;

    @BeforeMethod
    protected void beforeMethod()
    {
        mockery = new Mockery();
        databaseTransactionProvider = mockery.mock(IDatabaseTransactionProvider.class);
        sessionTokenProvider = mockery.mock(ISessionTokenProvider.class);
        transactionOperationExecutor = mockery.mock(ITransactionOperationExecutor.class);
        transactionLog = mockery.mock(ITransactionLog.class);
    }

    @AfterMethod
    protected void afterMethod()
    {
        mockery.assertIsSatisfied();
    }

    @Test(dataProvider = "provideKeys")
    public void testDifferentTransactionsAreExecutedInSeparateThreads(String interactiveSessionKey, String transactionCoordinatorKey) throws Throwable
    {
        TransactionParticipant participant =
                new TransactionParticipant(TEST_PARTICIPANT_ID, TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        databaseTransactionProvider, transactionOperationExecutor, transactionLog, TEST_TRANSACTION_TIMEOUT, TEST_THREAD_COUNT_LIMIT);

        MutableObject<String> transaction1BeginThreadName = new MutableObject<>();
        MutableObject<String> transaction1PrepareThreadName = new MutableObject<>();
        MutableObject<String> transaction1CommitThreadName = new MutableObject<>();

        MutableObject<String> transaction2BeginThreadName = new MutableObject<>();
        MutableObject<String> transaction2RollbackThreadName = new MutableObject<>();

        mockery.checking(new Expectations()
        {
            {
                allowing(sessionTokenProvider).isValid(TEST_SESSION_TOKEN);
                will(returnValue(true));
                allowing(sessionTokenProvider).isValid(TEST_SESSION_TOKEN_2);
                will(returnValue(true));

                allowing(sessionTokenProvider).isInstanceAdminOrSystem(TEST_SESSION_TOKEN);
                will(returnValue(false));
                allowing(sessionTokenProvider).isInstanceAdminOrSystem(TEST_SESSION_TOKEN_2);
                will(returnValue(false));

                // begin 1
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_STARTED)));
                one(databaseTransactionProvider).beginTransaction(with(TEST_TRANSACTION_ID));
                will(new CustomAction("beginTransaction")
                {
                    @Override public Object invoke(final Invocation invocation)
                    {
                        transaction1BeginThreadName.setValue(Thread.currentThread().getName());
                        return TEST_TRANSACTION;
                    }
                });
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED)));

                if (transactionCoordinatorKey != null)
                {
                    // prepare 1
                    one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.PREPARE_STARTED)));
                    one(databaseTransactionProvider).prepareTransaction(with(TEST_TRANSACTION_ID), with(TEST_TRANSACTION));
                    will(new CustomAction("prepareTransaction")
                    {
                        @Override public Object invoke(final Invocation invocation)
                        {
                            transaction1PrepareThreadName.setValue(Thread.currentThread().getName());
                            return null;
                        }
                    });
                    one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.PREPARE_FINISHED)));
                }

                // execute 1
                one(transactionOperationExecutor).executeOperation(TEST_SESSION_TOKEN, TEST_OPERATION_NAME, TEST_OPERATION_ARGUMENTS);
                will(new CustomAction("executeOperation")
                {
                    @Override public Object invoke(final Invocation invocation)
                    {
                        return Thread.currentThread().getName();
                    }
                });

                // commit 1
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.COMMIT_STARTED)));
                one(databaseTransactionProvider).commitTransaction(with(TEST_TRANSACTION_ID), with(TEST_TRANSACTION),
                        with(transactionCoordinatorKey != null));
                will(new CustomAction("commitTransaction")
                {
                    @Override public Object invoke(final Invocation invocation)
                    {
                        transaction1CommitThreadName.setValue(Thread.currentThread().getName());
                        return null;
                    }
                });
                one(transactionLog).deleteTransaction(TEST_TRANSACTION_ID);

                // begin 2
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID_2, TransactionStatus.BEGIN_STARTED)));
                one(databaseTransactionProvider).beginTransaction(with(TEST_TRANSACTION_ID_2));
                will(new CustomAction("beginTransaction")
                {
                    @Override public Object invoke(final Invocation invocation)
                    {
                        transaction2BeginThreadName.setValue(Thread.currentThread().getName());
                        return TEST_TRANSACTION_2;
                    }
                });
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID_2, TransactionStatus.BEGIN_FINISHED)));

                // execute 2
                one(transactionOperationExecutor).executeOperation(TEST_SESSION_TOKEN_2, TEST_OPERATION_NAME_2, TEST_OPERATION_ARGUMENTS_2);
                will(new CustomAction("executeOperation")
                {
                    @Override public Object invoke(final Invocation invocation)
                    {
                        return Thread.currentThread().getName();
                    }
                });

                // rollback 2
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID_2, TransactionStatus.ROLLBACK_STARTED)));
                one(databaseTransactionProvider).rollbackTransaction(with(TEST_TRANSACTION_ID_2), with(TEST_TRANSACTION_2),
                        with(transactionCoordinatorKey != null));
                will(new CustomAction("rollbackTransaction")
                {
                    @Override public Object invoke(final Invocation invocation)
                    {
                        transaction2RollbackThreadName.setValue(Thread.currentThread().getName());
                        return null;
                    }
                });
                one(transactionLog).deleteTransaction(TEST_TRANSACTION_ID_2);
            }
        });

        // begin 1
        participant.beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, interactiveSessionKey, transactionCoordinatorKey);
        // begin 2
        participant.beginTransaction(TEST_TRANSACTION_ID_2, TEST_SESSION_TOKEN_2, interactiveSessionKey, transactionCoordinatorKey);
        // execute 1
        String transaction1OperationThreadName =
                participant.executeOperation(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, interactiveSessionKey, TEST_OPERATION_NAME,
                        TEST_OPERATION_ARGUMENTS);
        // execute 2
        String transaction2OperationThreadName =
                participant.executeOperation(TEST_TRANSACTION_ID_2, TEST_SESSION_TOKEN_2, interactiveSessionKey, TEST_OPERATION_NAME_2,
                        TEST_OPERATION_ARGUMENTS_2);

        if (transactionCoordinatorKey != null)
        {
            // prepare 1
            participant.prepareTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, interactiveSessionKey, transactionCoordinatorKey);
        }

        // commit 1
        participant.commitTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, interactiveSessionKey);
        // rollback 2
        participant.rollbackTransaction(TEST_TRANSACTION_ID_2, TEST_SESSION_TOKEN_2, interactiveSessionKey);

        Set<String> transaction1ThreadNames = new HashSet<>(
                List.of(transaction1BeginThreadName.getValue(), transaction1OperationThreadName, transaction1CommitThreadName.getValue()));

        if (transactionCoordinatorKey != null)
        {
            transaction1ThreadNames.add(transaction1PrepareThreadName.getValue());
        }

        Set<String> transaction2ThreadNames = new HashSet<>(
                List.of(transaction2BeginThreadName.getValue(), transaction2OperationThreadName, transaction2RollbackThreadName.getValue()));

        assertEquals(transaction1ThreadNames.size(), 1);
        assertEquals(transaction2ThreadNames.size(), 1);

        assertFalse(transaction1ThreadNames.contains(Thread.currentThread().getName()));
        assertFalse(transaction2ThreadNames.contains(Thread.currentThread().getName()));
        assertFalse(transaction1ThreadNames.removeAll(transaction2ThreadNames));
    }

    @DataProvider
    protected Object[][] provideInvalidArgumentsForBegin()
    {
        return new Object[][]
                {
                        { null, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY, "Transaction id cannot be null" },
                        { TEST_TRANSACTION_ID, null, TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY, "Session token cannot be null" },
                        { TEST_TRANSACTION_ID, INVALID_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY,
                                "Invalid session token" },
                        { TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, null, TEST_TRANSACTION_COORDINATOR_KEY, "Interactive session key cannot be null" },
                        { TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, INVALID_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY,
                                "Invalid interactive session key" },
                        { TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, INVALID_TRANSACTION_COORDINATOR_KEY,
                                "Invalid transaction coordinator key" },
                };
    }

    @Test(dataProvider = "provideInvalidArgumentsForBegin")
    public void testBeginTransactionWithInvalidArguments(UUID transactionId, String sessionToken, String interactiveSessionKey,
            String transactionCoordinatorKey, String expectedException)
    {
        TransactionParticipant participant =
                new TransactionParticipant(TEST_PARTICIPANT_ID, TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        databaseTransactionProvider, transactionOperationExecutor, transactionLog, TEST_TRANSACTION_TIMEOUT, TEST_THREAD_COUNT_LIMIT);

        mockery.checking(new Expectations()
        {
            {
                allowing(sessionTokenProvider).isValid(TEST_SESSION_TOKEN);
                will(returnValue(true));

                allowing(sessionTokenProvider).isValid(INVALID_SESSION_TOKEN);
                will(returnValue(false));
            }
        });

        try
        {
            // begin (fails)
            participant.beginTransaction(transactionId, sessionToken, interactiveSessionKey, transactionCoordinatorKey);
            fail();
        } catch (Throwable t)
        {
            assertEquals(t.getMessage(), expectedException);
            assertNull(participant.getTransaction(TEST_TRANSACTION_ID));
        }
    }

    @DataProvider
    protected Object[][] provideKeysAndExceptionsForBegin()
    {
        return new Object[][]
                {
                        { TEST_INTERACTIVE_SESSION_KEY, null, TEST_UNCHECKED_EXCEPTION },
                        { TEST_INTERACTIVE_SESSION_KEY, null, TEST_CHECKED_EXCEPTION },
                        { TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY, TEST_UNCHECKED_EXCEPTION },
                        { TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY, TEST_CHECKED_EXCEPTION },
                };
    }

    @Test(dataProvider = "provideKeysAndExceptionsForBegin")
    public void testBeginTransactionFails(String interactiveSessionKey, String transactionCoordinatorKey, Throwable throwable) throws Throwable
    {
        TransactionParticipant participant =
                new TransactionParticipant(TEST_PARTICIPANT_ID, TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        databaseTransactionProvider, transactionOperationExecutor, transactionLog, TEST_TRANSACTION_TIMEOUT, TEST_THREAD_COUNT_LIMIT);

        mockery.checking(new Expectations()
        {
            {
                allowing(sessionTokenProvider).isValid(TEST_SESSION_TOKEN);
                will(returnValue(true));

                allowing(sessionTokenProvider).isInstanceAdminOrSystem(TEST_SESSION_TOKEN);
                will(returnValue(false));

                // begin (fails)
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_STARTED)));
                one(databaseTransactionProvider).beginTransaction(with(TEST_TRANSACTION_ID));
                will(throwException(throwable));

                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.ROLLBACK_STARTED)));
                one(transactionLog).deleteTransaction(TEST_TRANSACTION_ID);
            }
        });

        try
        {
            // begin (fails)
            participant.beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, interactiveSessionKey, transactionCoordinatorKey);
            fail();
        } catch (Throwable t)
        {
            assertEquals(t.getMessage(), "Begin transaction '" + TEST_TRANSACTION_ID + "' failed.");
            assertEquals(t.getCause(), throwable);
            assertNull(participant.getTransaction(TEST_TRANSACTION_ID));
        }
    }

    @DataProvider
    protected Object[][] provideInvalidArgumentsForExecuteOperations()
    {
        return new Object[][]
                {
                        { null, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_OPERATION_NAME, TEST_OPERATION_ARGUMENTS,
                                "Transaction id cannot be null" },
                        { TEST_TRANSACTION_ID, null, TEST_INTERACTIVE_SESSION_KEY, TEST_OPERATION_NAME, TEST_OPERATION_ARGUMENTS,
                                "Session token cannot be null" },
                        { TEST_TRANSACTION_ID, INVALID_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_OPERATION_NAME,
                                TEST_OPERATION_ARGUMENTS, "Invalid session token" },
                        { TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, null, TEST_OPERATION_NAME,
                                TEST_OPERATION_ARGUMENTS, "Interactive session key cannot be null" },
                        { TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, INVALID_INTERACTIVE_SESSION_KEY, TEST_OPERATION_NAME,
                                TEST_OPERATION_ARGUMENTS, "Invalid interactive session key" },
                        { TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, null, TEST_OPERATION_ARGUMENTS,
                                "Operation name cannot be null" },
                        { TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_OPERATION_NAME, null,
                                "Operation arguments cannot be null" },
                };
    }

    @Test(dataProvider = "provideInvalidArgumentsForExecuteOperations")
    public void testExecuteOperationWithInvalidArguments(UUID transactionId, String sessionToken, String interactiveSessionKeyOrNull,
            String operationName, Object[] operationArguments, String expectedExceptionMessage)
            throws Throwable
    {
        TransactionParticipant participant =
                new TransactionParticipant(TEST_PARTICIPANT_ID, TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        databaseTransactionProvider, transactionOperationExecutor, transactionLog, TEST_TRANSACTION_TIMEOUT, TEST_THREAD_COUNT_LIMIT);

        mockery.checking(new Expectations()
        {
            {
                allowing(sessionTokenProvider).isValid(TEST_SESSION_TOKEN);
                will(returnValue(true));

                allowing(sessionTokenProvider).isValid(INVALID_SESSION_TOKEN);
                will(returnValue(false));

                // begin
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_STARTED)));
                one(databaseTransactionProvider).beginTransaction(with(TEST_TRANSACTION_ID));
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED)));
            }
        });

        // begin
        participant.beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY);

        try
        {
            // execute (fails)
            participant.executeOperation(transactionId, sessionToken, interactiveSessionKeyOrNull, operationName,
                    operationArguments);
            fail();
        } catch (Throwable t)
        {
            assertEquals(t.getMessage(), expectedExceptionMessage);
            assertNotNull(participant.getTransaction(TEST_TRANSACTION_ID));
        }
    }

    @DataProvider
    protected Object[][] provideKeysAndExceptionsForExecuteOperations()
    {
        return new Object[][]
                {
                        { TEST_INTERACTIVE_SESSION_KEY, null, TEST_UNCHECKED_EXCEPTION },
                        { TEST_INTERACTIVE_SESSION_KEY, null, TEST_CHECKED_EXCEPTION },
                        { TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY, TEST_UNCHECKED_EXCEPTION },
                        { TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY, TEST_CHECKED_EXCEPTION },
                };
    }

    @Test(dataProvider = "provideKeysAndExceptionsForExecuteOperations")
    public void testExecuteOperationFails(String interactiveSessionKey, String transactionCoordinatorKey, Throwable throwable) throws Throwable
    {
        TransactionParticipant participant =
                new TransactionParticipant(TEST_PARTICIPANT_ID, TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        databaseTransactionProvider, transactionOperationExecutor, transactionLog, TEST_TRANSACTION_TIMEOUT, TEST_THREAD_COUNT_LIMIT);

        mockery.checking(new Expectations()
        {
            {
                allowing(sessionTokenProvider).isValid(TEST_SESSION_TOKEN);
                will(returnValue(true));

                allowing(sessionTokenProvider).isInstanceAdminOrSystem(TEST_SESSION_TOKEN);
                will(returnValue(false));

                // begin
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_STARTED)));
                one(databaseTransactionProvider).beginTransaction(with(TEST_TRANSACTION_ID));
                will(returnValue(TEST_TRANSACTION));
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED)));

                // execute
                one(transactionOperationExecutor).executeOperation(TEST_SESSION_TOKEN, TEST_OPERATION_NAME, TEST_OPERATION_ARGUMENTS);
                will(throwException(throwable));

                // rollback
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.ROLLBACK_STARTED)));
                one(databaseTransactionProvider).rollbackTransaction(with(TEST_TRANSACTION_ID), with(TEST_TRANSACTION),
                        with(transactionCoordinatorKey != null));
                one(transactionLog).deleteTransaction(TEST_TRANSACTION_ID);
            }
        });

        assertNull(participant.getTransaction(TEST_TRANSACTION_ID));
        // begin
        participant.beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, interactiveSessionKey, transactionCoordinatorKey);
        assertNotNull(participant.getTransaction(TEST_TRANSACTION_ID));

        try
        {
            // execute (fails)
            participant.executeOperation(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, interactiveSessionKey, TEST_OPERATION_NAME,
                    TEST_OPERATION_ARGUMENTS);
            fail();
        } catch (TransactionOperationException e)
        {
            assertEquals(e.getCause(), throwable);
            assertNotNull(participant.getTransaction(TEST_TRANSACTION_ID));
            // rollback
            participant.rollbackTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, interactiveSessionKey);
            assertNull(participant.getTransaction(TEST_TRANSACTION_ID));
        }
    }

    @Test(dataProvider = "provideKeysAndExceptionsForExecuteOperations")
    public void testExecuteOperationFailsButGetsRetriedAndSucceeds(String interactiveSessionKey, String transactionCoordinatorKey,
            Throwable throwable) throws Throwable
    {
        TransactionParticipant participant =
                new TransactionParticipant(TEST_PARTICIPANT_ID, TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        databaseTransactionProvider, transactionOperationExecutor, transactionLog, TEST_TRANSACTION_TIMEOUT, TEST_THREAD_COUNT_LIMIT);

        mockery.checking(new Expectations()
        {
            {
                allowing(sessionTokenProvider).isValid(TEST_SESSION_TOKEN);
                will(returnValue(true));

                allowing(sessionTokenProvider).isInstanceAdminOrSystem(TEST_SESSION_TOKEN);
                will(returnValue(false));

                // begin
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_STARTED)));
                one(databaseTransactionProvider).beginTransaction(with(TEST_TRANSACTION_ID));
                will(returnValue(TEST_TRANSACTION));
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED)));

                // execute (fails)
                one(transactionOperationExecutor).executeOperation(TEST_SESSION_TOKEN, TEST_OPERATION_NAME, TEST_OPERATION_ARGUMENTS);
                will(throwException(throwable));

                // execute
                one(transactionOperationExecutor).executeOperation(TEST_SESSION_TOKEN, TEST_OPERATION_NAME, TEST_OPERATION_ARGUMENTS);
                will(returnValue("OK"));

                if (transactionCoordinatorKey != null)
                {
                    // prepare
                    one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.PREPARE_STARTED)));
                    one(databaseTransactionProvider).prepareTransaction(with(TEST_TRANSACTION_ID), with(TEST_TRANSACTION));
                    one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.PREPARE_FINISHED)));
                }

                // commit
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.COMMIT_STARTED)));
                one(databaseTransactionProvider).commitTransaction(with(TEST_TRANSACTION_ID), with(TEST_TRANSACTION),
                        with(transactionCoordinatorKey != null));
                one(transactionLog).deleteTransaction(TEST_TRANSACTION_ID);
            }
        });

        assertNull(participant.getTransaction(TEST_TRANSACTION_ID));
        // begin
        participant.beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, interactiveSessionKey, transactionCoordinatorKey);
        assertNotNull(participant.getTransaction(TEST_TRANSACTION_ID));

        try
        {
            // execute (fails)
            participant.executeOperation(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, interactiveSessionKey, TEST_OPERATION_NAME,
                    TEST_OPERATION_ARGUMENTS);
            fail();
        } catch (Throwable e)
        {
            assertEquals(e.getCause(), throwable);
            assertNotNull(participant.getTransaction(TEST_TRANSACTION_ID));

            // execute
            Object result =
                    participant.executeOperation(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, interactiveSessionKey, TEST_OPERATION_NAME,
                            TEST_OPERATION_ARGUMENTS);
            assertEquals(result, "OK");

            if (transactionCoordinatorKey != null)
            {
                // prepare
                participant.prepareTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, interactiveSessionKey, transactionCoordinatorKey);
                assertNotNull(participant.getTransaction(TEST_TRANSACTION_ID));
            }

            // commit
            participant.commitTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, interactiveSessionKey);
            assertNull(participant.getTransaction(TEST_TRANSACTION_ID));
        }
    }

    @DataProvider
    protected Object[][] provideInvalidArgumentsForPrepare()
    {
        return new Object[][]
                {
                        { null, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY, "Transaction id cannot be null" },
                        { TEST_TRANSACTION_ID, null, TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY, "Session token cannot be null" },
                        { TEST_TRANSACTION_ID, INVALID_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY,
                                "Invalid session token" },
                        { TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, null, TEST_TRANSACTION_COORDINATOR_KEY, "Interactive session key cannot be null" },
                        { TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, INVALID_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY,
                                "Invalid interactive session key" },
                        { TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, null, "Transaction coordinator key cannot be null" },
                        { TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, INVALID_TRANSACTION_COORDINATOR_KEY,
                                "Invalid transaction coordinator key" }
                };
    }

    @Test(dataProvider = "provideInvalidArgumentsForPrepare")
    public void testPrepareTransactionWithInvalidArguments(UUID transactionId, String sessionToken, String interactiveSessionKey,
            String transactionCoordinatorKeyOrNull,
            String expectedExceptionMessage) throws Throwable
    {
        TransactionParticipant participant =
                new TransactionParticipant(TEST_PARTICIPANT_ID, TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        databaseTransactionProvider, transactionOperationExecutor, transactionLog, TEST_TRANSACTION_TIMEOUT, TEST_THREAD_COUNT_LIMIT);

        mockery.checking(new Expectations()
        {
            {
                allowing(sessionTokenProvider).isValid(TEST_SESSION_TOKEN);
                will(returnValue(true));

                allowing(sessionTokenProvider).isValid(INVALID_SESSION_TOKEN);
                will(returnValue(false));

                // begin
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_STARTED)));
                one(databaseTransactionProvider).beginTransaction(with(TEST_TRANSACTION_ID));
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED)));
            }
        });

        // begin
        participant.beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY);

        try
        {
            // prepare (fails)
            participant.prepareTransaction(transactionId, sessionToken, interactiveSessionKey, transactionCoordinatorKeyOrNull);
            fail();
        } catch (Throwable t)
        {
            assertEquals(t.getMessage(), expectedExceptionMessage);
            assertNotNull(participant.getTransaction(TEST_TRANSACTION_ID));
        }
    }

    @DataProvider
    protected Object[][] provideKeysAndExceptionsForPrepare()
    {
        return new Object[][]
                {
                        { TEST_UNCHECKED_EXCEPTION },
                        { TEST_CHECKED_EXCEPTION },
                };
    }

    @Test(dataProvider = "provideKeysAndExceptionsForPrepare")
    public void testPrepareTransactionFails(Throwable throwable) throws Throwable
    {
        TransactionParticipant participant =
                new TransactionParticipant(TEST_PARTICIPANT_ID, TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        databaseTransactionProvider, transactionOperationExecutor, transactionLog, TEST_TRANSACTION_TIMEOUT, TEST_THREAD_COUNT_LIMIT);

        mockery.checking(new Expectations()
        {
            {
                allowing(sessionTokenProvider).isValid(TEST_SESSION_TOKEN);
                will(returnValue(true));

                allowing(sessionTokenProvider).isInstanceAdminOrSystem(TEST_SESSION_TOKEN);
                will(returnValue(false));

                // begin
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_STARTED)));
                one(databaseTransactionProvider).beginTransaction(with(TEST_TRANSACTION_ID));
                will(returnValue(TEST_TRANSACTION));
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED)));

                // execute
                one(transactionOperationExecutor).executeOperation(TEST_SESSION_TOKEN, TEST_OPERATION_NAME, TEST_OPERATION_ARGUMENTS);

                // prepare (fails)
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.PREPARE_STARTED)));
                one(databaseTransactionProvider).prepareTransaction(with(TEST_TRANSACTION_ID), with(TEST_TRANSACTION));
                will(throwException(throwable));

                // rollback
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.ROLLBACK_STARTED)));
                one(databaseTransactionProvider).rollbackTransaction(with(TEST_TRANSACTION_ID), with(TEST_TRANSACTION), with(true));
                one(transactionLog).deleteTransaction(TEST_TRANSACTION_ID);
            }
        });

        assertNull(participant.getTransaction(TEST_TRANSACTION_ID));
        // begin
        participant.beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY);
        assertNotNull(participant.getTransaction(TEST_TRANSACTION_ID));
        // execute
        participant.executeOperation(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_OPERATION_NAME,
                TEST_OPERATION_ARGUMENTS);

        try
        {
            // prepare (fails)
            participant.prepareTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY);
            fail();
        } catch (Throwable t)
        {
            assertEquals(t.getMessage(), "Prepare transaction '" + TEST_TRANSACTION_ID + "' failed.");
            assertEquals(t.getCause(), throwable);
            assertNotNull(participant.getTransaction(TEST_TRANSACTION_ID));

            // rollback
            participant.rollbackTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY);
            assertNull(participant.getTransaction(TEST_TRANSACTION_ID));
        }
    }

    @DataProvider
    protected Object[][] provideInvalidArgumentsForCommit()
    {
        return new Object[][]
                {
                        { null, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, "Transaction id cannot be null" },
                        { TEST_TRANSACTION_ID, null, TEST_INTERACTIVE_SESSION_KEY, "Session token cannot be null" },
                        { TEST_TRANSACTION_ID, INVALID_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, "Invalid session token" },
                        { TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, null, "Interactive session key cannot be null" },
                        { TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, INVALID_INTERACTIVE_SESSION_KEY, "Invalid interactive session key" }
                };
    }

    @Test(dataProvider = "provideInvalidArgumentsForCommit")
    public void testCommitTransactionWithInvalidArguments(UUID transactionId, String sessionToken, String interactiveSessionKey,
            String expectedException) throws Throwable
    {
        doTestCommitTransactionWithInvalidArguments(participant ->
        {
            participant.commitTransaction(transactionId, sessionToken, interactiveSessionKey);
            return null;
        }, expectedException);
    }

    @DataProvider
    protected Object[][] provideInvalidArgumentsForCommitRecovery()
    {
        return new Object[][]
                {
                        { null, TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY, "Transaction id cannot be null" },
                        { TEST_TRANSACTION_ID, null, TEST_TRANSACTION_COORDINATOR_KEY, "Interactive session key cannot be null" },
                        { TEST_TRANSACTION_ID, TEST_INTERACTIVE_SESSION_KEY, null, "Transaction coordinator key cannot be null" },
                        { TEST_TRANSACTION_ID, INVALID_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY, "Invalid interactive session key" },
                        { TEST_TRANSACTION_ID, TEST_INTERACTIVE_SESSION_KEY, INVALID_TRANSACTION_COORDINATOR_KEY,
                                "Invalid transaction coordinator key" },
                };
    }

    @Test(dataProvider = "provideInvalidArgumentsForCommitRecovery")
    public void testCommitTransactionRecoveryWithInvalidArguments(UUID transactionId, String interactiveSessionKey, String transactionCoordinatorKey,
            String expectedException) throws Throwable
    {
        doTestCommitTransactionWithInvalidArguments(participant ->
        {
            participant.commitRecoveredTransaction(transactionId, interactiveSessionKey, transactionCoordinatorKey);
            return null;
        }, expectedException);
    }

    private void doTestCommitTransactionWithInvalidArguments(Function<ITransactionParticipant, Void> commitTransaction,
            String expectedExceptionMessage) throws Throwable
    {
        TransactionParticipant participant =
                new TransactionParticipant(TEST_PARTICIPANT_ID, TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        databaseTransactionProvider, transactionOperationExecutor, transactionLog, TEST_TRANSACTION_TIMEOUT, TEST_THREAD_COUNT_LIMIT);

        mockery.checking(new Expectations()
        {
            {
                allowing(sessionTokenProvider).isValid(TEST_SESSION_TOKEN);
                will(returnValue(true));

                allowing(sessionTokenProvider).isValid(INVALID_SESSION_TOKEN);
                will(returnValue(false));

                // begin
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_STARTED)));
                one(databaseTransactionProvider).beginTransaction(with(TEST_TRANSACTION_ID));
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED)));
            }
        });

        // begin
        participant.beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY);

        try
        {
            commitTransaction.apply(participant);
            fail();
        } catch (Throwable t)
        {
            assertEquals(t.getMessage(), expectedExceptionMessage);
        }
    }

    @DataProvider
    protected Object[][] provideKeysAndExceptionsForCommit()
    {
        return new Object[][]
                {
                        { TEST_INTERACTIVE_SESSION_KEY, null, TEST_UNCHECKED_EXCEPTION },
                        { TEST_INTERACTIVE_SESSION_KEY, null, TEST_CHECKED_EXCEPTION },
                        { TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY, TEST_UNCHECKED_EXCEPTION },
                        { TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY, TEST_CHECKED_EXCEPTION },
                };
    }

    @Test(dataProvider = "provideKeysAndExceptionsForCommit")
    public void testCommitTransactionFails(String interactiveSessionKey, String transactionCoordinatorKey, Throwable throwable) throws Throwable
    {
        doTestCommitTransactionFails(interactiveSessionKey, transactionCoordinatorKey, throwable, participant ->
        {
            participant.commitTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY);
            return null;
        });
    }

    @Test(dataProvider = "provideKeysAndExceptionsForCommit")
    public void testCommitTransactionRecoveryFails(String interactiveSessionKey, String transactionCoordinatorKey, Throwable throwable)
            throws Throwable
    {
        doTestCommitTransactionFails(interactiveSessionKey, transactionCoordinatorKey, throwable, participant ->
        {
            participant.commitRecoveredTransaction(TEST_TRANSACTION_ID, TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY);
            return null;
        });
    }

    private void doTestCommitTransactionFails(String interactiveSessionKey, String transactionCoordinatorKey, Throwable throwable,
            Function<ITransactionParticipant, Void> commitTransaction) throws Throwable
    {
        TransactionParticipant participant =
                new TransactionParticipant(TEST_PARTICIPANT_ID, TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        databaseTransactionProvider, transactionOperationExecutor, transactionLog, TEST_TRANSACTION_TIMEOUT, TEST_THREAD_COUNT_LIMIT);

        mockery.checking(new Expectations()
        {
            {
                allowing(sessionTokenProvider).isValid(TEST_SESSION_TOKEN);
                will(returnValue(true));

                allowing(sessionTokenProvider).isInstanceAdminOrSystem(TEST_SESSION_TOKEN);
                will(returnValue(false));

                // begin
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_STARTED)));
                one(databaseTransactionProvider).beginTransaction(with(TEST_TRANSACTION_ID));
                will(returnValue(TEST_TRANSACTION));
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED)));

                // execute
                one(transactionOperationExecutor).executeOperation(TEST_SESSION_TOKEN, TEST_OPERATION_NAME, TEST_OPERATION_ARGUMENTS);

                if (transactionCoordinatorKey != null)
                {
                    // prepare
                    one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.PREPARE_STARTED)));
                    one(databaseTransactionProvider).prepareTransaction(with(TEST_TRANSACTION_ID), with(TEST_TRANSACTION));
                    one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.PREPARE_FINISHED)));
                }

                // commit (fails)
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.COMMIT_STARTED)));
                one(databaseTransactionProvider).commitTransaction(with(TEST_TRANSACTION_ID), with(TEST_TRANSACTION),
                        with(transactionCoordinatorKey != null));
                will(throwException(throwable));

                if (transactionCoordinatorKey == null)
                {
                    // rollback
                    one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.ROLLBACK_STARTED)));
                    one(databaseTransactionProvider).rollbackTransaction(with(TEST_TRANSACTION_ID), with(TEST_TRANSACTION), with(false));
                    one(transactionLog).deleteTransaction(TEST_TRANSACTION_ID);
                }
            }
        });

        assertNull(participant.getTransaction(TEST_TRANSACTION_ID));
        // begin
        participant.beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, interactiveSessionKey, transactionCoordinatorKey);
        assertNotNull(participant.getTransaction(TEST_TRANSACTION_ID));
        // execute
        participant.executeOperation(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, interactiveSessionKey, TEST_OPERATION_NAME,
                TEST_OPERATION_ARGUMENTS);
        assertNotNull(participant.getTransaction(TEST_TRANSACTION_ID));

        if (transactionCoordinatorKey != null)
        {
            // prepare
            participant.prepareTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, interactiveSessionKey, transactionCoordinatorKey);
            assertNotNull(participant.getTransaction(TEST_TRANSACTION_ID));
        }

        try
        {
            // commit (fails)
            commitTransaction.apply(participant);
            fail();
        } catch (Throwable t)
        {
            assertEquals(t.getMessage(), "Commit transaction '" + TEST_TRANSACTION_ID + "' failed.");
            assertEquals(t.getCause(), throwable);

            if (transactionCoordinatorKey == null)
            {
                assertNull(participant.getTransaction(TEST_TRANSACTION_ID));
            } else
            {
                assertNotNull(participant.getTransaction(TEST_TRANSACTION_ID));
            }
        }
    }

    @DataProvider
    protected Object[][] provideInvalidArgumentsForRollback()
    {
        return new Object[][]
                {
                        { null, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, "Transaction id cannot be null" },
                        { TEST_TRANSACTION_ID, null, TEST_INTERACTIVE_SESSION_KEY, "Session token cannot be null" },
                        { TEST_TRANSACTION_ID, INVALID_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, "Invalid session token" },
                        { TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, null, "Interactive session key cannot be null" },
                        { TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, INVALID_INTERACTIVE_SESSION_KEY, "Invalid interactive session key" },
                };
    }

    @Test(dataProvider = "provideInvalidArgumentsForRollback")
    public void testRollbackTransactionWithInvalidArguments(UUID transactionId, String sessionToken, String interactiveSessionKeyOrNull,
            String expectedException) throws Throwable
    {
        doTestRollbackTransactionWithInvalidArguments(participant ->
        {
            participant.rollbackTransaction(transactionId, sessionToken, interactiveSessionKeyOrNull);
            return null;
        }, expectedException);
    }

    @DataProvider
    protected Object[][] provideInvalidArgumentsForRollbackRecovery()
    {
        return new Object[][]
                {
                        { null, TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY, "Transaction id cannot be null" },
                        { TEST_TRANSACTION_ID, null, TEST_TRANSACTION_COORDINATOR_KEY, "Interactive session key cannot be null" },
                        { TEST_TRANSACTION_ID, TEST_INTERACTIVE_SESSION_KEY, null, "Transaction coordinator key cannot be null" },
                        { TEST_TRANSACTION_ID, INVALID_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY, "Invalid interactive session key" },
                        { TEST_TRANSACTION_ID, TEST_INTERACTIVE_SESSION_KEY, INVALID_TRANSACTION_COORDINATOR_KEY,
                                "Invalid transaction coordinator key" },
                };
    }

    @Test(dataProvider = "provideInvalidArgumentsForRollbackRecovery")
    public void testRollbackTransactionRecoveryWithInvalidArguments(UUID transactionId, String interactiveSessionKeyOrNull,
            String transactionCoordinatorKeyOrNull,
            String expectedException) throws Throwable
    {
        doTestRollbackTransactionWithInvalidArguments(participant ->
        {
            participant.rollbackRecoveredTransaction(transactionId, interactiveSessionKeyOrNull, transactionCoordinatorKeyOrNull);
            return null;
        }, expectedException);
    }

    private void doTestRollbackTransactionWithInvalidArguments(Function<ITransactionParticipant, Void> rollbackTransaction,
            String expectedExceptionMessage)
            throws Throwable
    {
        TransactionParticipant participant =
                new TransactionParticipant(TEST_PARTICIPANT_ID, TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        databaseTransactionProvider, transactionOperationExecutor, transactionLog, TEST_TRANSACTION_TIMEOUT, TEST_THREAD_COUNT_LIMIT);

        mockery.checking(new Expectations()
        {
            {
                allowing(sessionTokenProvider).isValid(TEST_SESSION_TOKEN);
                will(returnValue(true));

                allowing(sessionTokenProvider).isValid(INVALID_SESSION_TOKEN);
                will(returnValue(false));

                // begin
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_STARTED)));
                one(databaseTransactionProvider).beginTransaction(with(TEST_TRANSACTION_ID));
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED)));
            }
        });

        // begin
        participant.beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY);

        try
        {
            // rollback (fails)
            rollbackTransaction.apply(participant);
            fail();
        } catch (Throwable t)
        {
            assertEquals(t.getMessage(), expectedExceptionMessage);
            assertNotNull(participant.getTransaction(TEST_TRANSACTION_ID));
        }
    }

    @DataProvider
    protected Object[][] provideKeysAndExceptionsForRollback()
    {
        return new Object[][]
                {
                        { TEST_INTERACTIVE_SESSION_KEY, null, TEST_UNCHECKED_EXCEPTION },
                        { TEST_INTERACTIVE_SESSION_KEY, null, TEST_CHECKED_EXCEPTION },
                        { TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY, TEST_UNCHECKED_EXCEPTION },
                        { TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY, TEST_CHECKED_EXCEPTION },
                };
    }

    @Test(dataProvider = "provideKeysAndExceptionsForRollback")
    public void testRollbackTransactionFails(String interactiveSessionKey, String transactionCoordinatorKey, Throwable throwable) throws Throwable
    {
        doTestRollbackTransactionFails(interactiveSessionKey, transactionCoordinatorKey, throwable, participant ->
        {
            participant.rollbackTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY);
            return null;
        });
    }

    @Test(dataProvider = "provideKeysAndExceptionsForRollback")
    public void testRollbackTransactionRecoveryFails(String interactiveSessionKey, String transactionCoordinatorKey, Throwable throwable)
            throws Throwable
    {
        doTestRollbackTransactionFails(interactiveSessionKey, transactionCoordinatorKey, throwable, participant ->
        {
            participant.rollbackRecoveredTransaction(TEST_TRANSACTION_ID, TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY);
            return null;
        });
    }

    private void doTestRollbackTransactionFails(String interactiveSessionKey, String transactionCoordinatorKey, Throwable throwable,
            Function<ITransactionParticipant, Void> rollbackTransaction) throws Throwable
    {
        TransactionParticipant participant =
                new TransactionParticipant(TEST_PARTICIPANT_ID, TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        databaseTransactionProvider, transactionOperationExecutor, transactionLog, TEST_TRANSACTION_TIMEOUT, TEST_THREAD_COUNT_LIMIT);

        mockery.checking(new Expectations()
        {
            {
                allowing(sessionTokenProvider).isValid(TEST_SESSION_TOKEN);
                will(returnValue(true));

                allowing(sessionTokenProvider).isInstanceAdminOrSystem(TEST_SESSION_TOKEN);
                will(returnValue(false));

                // begin
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_STARTED)));
                one(databaseTransactionProvider).beginTransaction(with(TEST_TRANSACTION_ID));
                will(returnValue(TEST_TRANSACTION));
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED)));

                // execute
                one(transactionOperationExecutor).executeOperation(TEST_SESSION_TOKEN, TEST_OPERATION_NAME, TEST_OPERATION_ARGUMENTS);

                // rollback (fails)
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.ROLLBACK_STARTED)));
                one(databaseTransactionProvider).rollbackTransaction(with(TEST_TRANSACTION_ID), with(TEST_TRANSACTION),
                        with(transactionCoordinatorKey != null));
                will(throwException(throwable));
            }
        });

        assertNull(participant.getTransaction(TEST_TRANSACTION_ID));
        // begin
        participant.beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, interactiveSessionKey, transactionCoordinatorKey);
        assertNotNull(participant.getTransaction(TEST_TRANSACTION_ID));
        // execute
        participant.executeOperation(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, interactiveSessionKey, TEST_OPERATION_NAME, TEST_OPERATION_ARGUMENTS);
        assertNotNull(participant.getTransaction(TEST_TRANSACTION_ID));

        try
        {
            // rollback (fails)
            rollbackTransaction.apply(participant);
            fail();
        } catch (Throwable t)
        {
            assertEquals(t.getMessage(), "Rollback transaction '" + TEST_TRANSACTION_ID + "' failed.");
            assertEquals(t.getCause(), throwable);
            assertNotNull(participant.getTransaction(TEST_TRANSACTION_ID));
        }
    }

    @Test(dataProvider = "provideKeys")
    public void testNewTransactionCanBeStarted(String interactiveSessionKey, String transactionCoordinatorKey) throws Throwable
    {
        TransactionParticipant participant =
                new TransactionParticipant(TEST_PARTICIPANT_ID, TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        databaseTransactionProvider, transactionOperationExecutor, transactionLog, TEST_TRANSACTION_TIMEOUT, TEST_THREAD_COUNT_LIMIT);

        mockery.checking(new Expectations()
        {
            {
                allowing(sessionTokenProvider).isValid(TEST_SESSION_TOKEN);
                will(returnValue(true));

                // begin
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_STARTED)));
                one(databaseTransactionProvider).beginTransaction(with(TEST_TRANSACTION_ID));
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED)));
            }
        });

        // begin
        participant.beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, interactiveSessionKey, transactionCoordinatorKey);
    }

    @Test
    public void testNewTransactionCannotExecuteOperations()
    {
        TransactionParticipant participant =
                new TransactionParticipant(TEST_PARTICIPANT_ID, TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        databaseTransactionProvider, transactionOperationExecutor, transactionLog, TEST_TRANSACTION_TIMEOUT, TEST_THREAD_COUNT_LIMIT);

        mockery.checking(new Expectations()
        {
            {
                allowing(sessionTokenProvider).isValid(TEST_SESSION_TOKEN);
                will(returnValue(true));
            }
        });

        try
        {
            participant.executeOperation(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_OPERATION_NAME,
                    TEST_OPERATION_ARGUMENTS);
            fail();
        } catch (Exception e)
        {
            assertEquals(e.getMessage(),
                    "Transaction '" + TEST_TRANSACTION_ID + "' does not exist.");
        }
    }

    @Test
    public void testNewTransactionCannotBePrepared()
    {
        TransactionParticipant participant =
                new TransactionParticipant(TEST_PARTICIPANT_ID, TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        databaseTransactionProvider, transactionOperationExecutor, transactionLog, TEST_TRANSACTION_TIMEOUT, TEST_THREAD_COUNT_LIMIT);

        mockery.checking(new Expectations()
        {
            {
                allowing(sessionTokenProvider).isValid(TEST_SESSION_TOKEN);
                will(returnValue(true));
            }
        });

        try
        {
            participant.prepareTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY,
                    TEST_TRANSACTION_COORDINATOR_KEY);
            fail();
        } catch (Exception e)
        {
            assertEquals(e.getMessage(),
                    "Transaction '" + TEST_TRANSACTION_ID + "' does not exist.");
        }
    }

    @Test
    public void testNewTransactionCannotBeCommitted()
    {
        TransactionParticipant participant =
                new TransactionParticipant(TEST_PARTICIPANT_ID, TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        databaseTransactionProvider, transactionOperationExecutor, transactionLog, TEST_TRANSACTION_TIMEOUT, TEST_THREAD_COUNT_LIMIT);

        mockery.checking(new Expectations()
        {
            {
                allowing(sessionTokenProvider).isValid(TEST_SESSION_TOKEN);
                will(returnValue(true));
            }
        });

        try
        {
            participant.commitTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY);
        } catch (Exception e)
        {
            assertEquals(e.getMessage(),
                    "Transaction '" + TEST_TRANSACTION_ID + "' does not exist.");
        }
    }

    @Test
    public void testNewTransactionCanBeRolledBack()
    {
        TransactionParticipant participant =
                new TransactionParticipant(TEST_PARTICIPANT_ID, TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        databaseTransactionProvider, transactionOperationExecutor, transactionLog, TEST_TRANSACTION_TIMEOUT, TEST_THREAD_COUNT_LIMIT);

        mockery.checking(new Expectations()
        {
            {
                allowing(sessionTokenProvider).isValid(TEST_SESSION_TOKEN);
                will(returnValue(true));
            }
        });

        // the call is possible and does nothing (used in recovery process)
        participant.rollbackTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY);
    }

    @Test(dataProvider = "provideKeys")
    public void testStartedTransactionCannotBeStarted(String interactiveSessionKey, String transactionCoordinatorKey) throws Throwable
    {
        TransactionParticipant participant =
                new TransactionParticipant(TEST_PARTICIPANT_ID, TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        databaseTransactionProvider, transactionOperationExecutor, transactionLog, TEST_TRANSACTION_TIMEOUT, TEST_THREAD_COUNT_LIMIT);

        mockery.checking(new Expectations()
        {
            {
                allowing(sessionTokenProvider).isValid(TEST_SESSION_TOKEN);
                will(returnValue(true));

                // begin
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_STARTED)));
                one(databaseTransactionProvider).beginTransaction(with(TEST_TRANSACTION_ID));
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED)));
            }
        });

        // begin
        participant.beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, interactiveSessionKey, transactionCoordinatorKey);

        try
        {
            // repeated begin (fails)
            participant.beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, interactiveSessionKey, transactionCoordinatorKey);
            fail();
        } catch (Exception e)
        {
            assertEquals(e.getMessage(),
                    "Transaction '" + TEST_TRANSACTION_ID + "' already exists.");
        }
    }

    @Test(dataProvider = "provideKeys")
    public void testStartedTransactionCanExecuteOperations(String interactiveSessionKey, String transactionCoordinatorKey) throws Throwable
    {
        TransactionParticipant participant =
                new TransactionParticipant(TEST_PARTICIPANT_ID, TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        databaseTransactionProvider, transactionOperationExecutor, transactionLog, TEST_TRANSACTION_TIMEOUT, TEST_THREAD_COUNT_LIMIT);

        mockery.checking(new Expectations()
        {
            {
                allowing(sessionTokenProvider).isValid(TEST_SESSION_TOKEN);
                will(returnValue(true));

                allowing(sessionTokenProvider).isInstanceAdminOrSystem(TEST_SESSION_TOKEN);
                will(returnValue(false));

                // begin
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_STARTED)));
                one(databaseTransactionProvider).beginTransaction(with(TEST_TRANSACTION_ID));
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED)));

                // execute 1
                one(transactionOperationExecutor).executeOperation(TEST_SESSION_TOKEN, TEST_OPERATION_NAME, TEST_OPERATION_ARGUMENTS);
                will(returnValue(TEST_OPERATION_RESULT));

                // execute 2
                one(transactionOperationExecutor).executeOperation(TEST_SESSION_TOKEN, TEST_OPERATION_NAME_2, TEST_OPERATION_ARGUMENTS_2);
                will(returnValue(TEST_OPERATION_RESULT_2));
            }
        });

        // begin
        participant.beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, interactiveSessionKey, transactionCoordinatorKey);

        // execute 1
        Object result = participant.executeOperation(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, interactiveSessionKey, TEST_OPERATION_NAME,
                TEST_OPERATION_ARGUMENTS);
        assertEquals(result, TEST_OPERATION_RESULT);

        // execute 2
        Object result2 =
                participant.executeOperation(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, interactiveSessionKey, TEST_OPERATION_NAME_2,
                        TEST_OPERATION_ARGUMENTS_2);
        assertEquals(result2, TEST_OPERATION_RESULT_2);
    }

    @Test(dataProvider = "provideKeys")
    public void testStartedTransactionCanPrepare(String interactiveSessionKey, String transactionCoordinatorKey) throws Throwable
    {
        TransactionParticipant participant =
                new TransactionParticipant(TEST_PARTICIPANT_ID, TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        databaseTransactionProvider, transactionOperationExecutor, transactionLog, TEST_TRANSACTION_TIMEOUT, TEST_THREAD_COUNT_LIMIT);

        mockery.checking(new Expectations()
        {
            {
                Object transaction = new Object();

                allowing(sessionTokenProvider).isValid(TEST_SESSION_TOKEN);
                will(returnValue(true));

                allowing(sessionTokenProvider).isInstanceAdminOrSystem(TEST_SESSION_TOKEN);
                will(returnValue(false));

                // begin
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_STARTED)));
                one(databaseTransactionProvider).beginTransaction(with(TEST_TRANSACTION_ID));
                will(returnValue(transaction));
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED)));

                if (transactionCoordinatorKey != null)
                {
                    // prepare
                    one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.PREPARE_STARTED)));
                    one(databaseTransactionProvider).prepareTransaction(with(TEST_TRANSACTION_ID), with(transaction));
                    one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.PREPARE_FINISHED)));
                }
            }
        });

        // begin
        participant.beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, interactiveSessionKey, transactionCoordinatorKey);
        try
        {
            // prepare
            participant.prepareTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, interactiveSessionKey, TEST_TRANSACTION_COORDINATOR_KEY);

            if (transactionCoordinatorKey == null)
            {
                fail();
            }
        } catch (Exception e)
        {
            if (transactionCoordinatorKey != null)
            {
                fail();
            }
            assertEquals(e.getMessage(), "Transaction '" + TEST_TRANSACTION_ID
                    + "' was started without transaction coordinator key, therefore calling prepare is not allowed.");
        }
    }

    @Test(dataProvider = "provideKeys")
    public void testStartedTransactionCanBeRolledBack(String interactiveSessionKey, String transactionCoordinatorKey) throws Throwable
    {
        TransactionParticipant participant =
                new TransactionParticipant(TEST_PARTICIPANT_ID, TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        databaseTransactionProvider, transactionOperationExecutor, transactionLog, TEST_TRANSACTION_TIMEOUT, TEST_THREAD_COUNT_LIMIT);

        mockery.checking(new Expectations()
        {
            {
                Object transaction = new Object();

                allowing(sessionTokenProvider).isValid(TEST_SESSION_TOKEN);
                will(returnValue(true));

                allowing(sessionTokenProvider).isInstanceAdminOrSystem(TEST_SESSION_TOKEN);
                will(returnValue(false));

                // begin
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_STARTED)));
                one(databaseTransactionProvider).beginTransaction(with(TEST_TRANSACTION_ID));
                will(returnValue(transaction));
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED)));

                // rollback
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.ROLLBACK_STARTED)));
                one(databaseTransactionProvider).rollbackTransaction(with(TEST_TRANSACTION_ID), with(transaction),
                        with(transactionCoordinatorKey != null));
                one(transactionLog).deleteTransaction(TEST_TRANSACTION_ID);
            }
        });

        // begin
        participant.beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, interactiveSessionKey, transactionCoordinatorKey);
        // rollback
        participant.rollbackTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, interactiveSessionKey);
    }

    @Test(dataProvider = "provideKeys")
    public void testStartedTransactionCanBeCommitted(String interactiveSessionKey, String transactionCoordinatorKey) throws Throwable
    {
        TransactionParticipant participant =
                new TransactionParticipant(TEST_PARTICIPANT_ID, TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        databaseTransactionProvider, transactionOperationExecutor, transactionLog, TEST_TRANSACTION_TIMEOUT, TEST_THREAD_COUNT_LIMIT);

        mockery.checking(new Expectations()
        {
            {
                Object transaction = new Object();

                allowing(sessionTokenProvider).isValid(TEST_SESSION_TOKEN);
                will(returnValue(true));

                allowing(sessionTokenProvider).isInstanceAdminOrSystem(TEST_SESSION_TOKEN);
                will(returnValue(false));

                // begin
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_STARTED)));
                one(databaseTransactionProvider).beginTransaction(with(TEST_TRANSACTION_ID));
                will(returnValue(transaction));
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED)));

                if (transactionCoordinatorKey == null)
                {
                    // commit
                    one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.COMMIT_STARTED)));
                    one(databaseTransactionProvider).commitTransaction(with(TEST_TRANSACTION_ID), with(transaction), with(false));
                    one(transactionLog).deleteTransaction(TEST_TRANSACTION_ID);
                }
            }
        });

        // begin
        participant.beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, interactiveSessionKey, transactionCoordinatorKey);

        try
        {
            // commit
            participant.commitTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, interactiveSessionKey);

            if (transactionCoordinatorKey != null)
            {
                fail();
            }
        } catch (Exception e)
        {
            if (transactionCoordinatorKey == null)
            {
                fail();
            } else
            {
                assertEquals(e.getMessage(), "Transaction '" + TEST_TRANSACTION_ID
                        + "' unexpected status 'BEGIN_FINISHED'. Expected statuses '[PREPARE_FINISHED, COMMIT_STARTED]'.");
            }
        }
    }

    @Test
    public void testPreparedTransactionCannotBeStarted() throws Throwable
    {
        TransactionParticipant participant =
                new TransactionParticipant(TEST_PARTICIPANT_ID, TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        databaseTransactionProvider, transactionOperationExecutor, transactionLog, TEST_TRANSACTION_TIMEOUT, TEST_THREAD_COUNT_LIMIT);

        mockery.checking(new Expectations()
        {
            {
                Object transaction = new Object();

                allowing(sessionTokenProvider).isValid(TEST_SESSION_TOKEN);
                will(returnValue(true));

                allowing(sessionTokenProvider).isInstanceAdminOrSystem(TEST_SESSION_TOKEN);
                will(returnValue(false));

                // begin
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_STARTED)));
                one(databaseTransactionProvider).beginTransaction(with(TEST_TRANSACTION_ID));
                will(returnValue(transaction));
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED)));

                // prepare
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.PREPARE_STARTED)));
                one(databaseTransactionProvider).prepareTransaction(with(TEST_TRANSACTION_ID), with(transaction));
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.PREPARE_FINISHED)));
            }
        });

        // begin
        participant.beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY);
        // prepare
        participant.prepareTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY);
        try
        {
            // repeated begin (fails)
            participant.beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY);
            fail();
        } catch (Exception e)
        {
            assertEquals(e.getMessage(),
                    "Transaction '" + TEST_TRANSACTION_ID + "' already exists.");
        }
    }

    @Test
    public void testPreparedTransactionCannotBePrepared() throws Throwable
    {
        TransactionParticipant participant =
                new TransactionParticipant(TEST_PARTICIPANT_ID, TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        databaseTransactionProvider, transactionOperationExecutor, transactionLog, TEST_TRANSACTION_TIMEOUT, TEST_THREAD_COUNT_LIMIT);

        mockery.checking(new Expectations()
        {
            {
                Object transaction = new Object();

                allowing(sessionTokenProvider).isValid(TEST_SESSION_TOKEN);
                will(returnValue(true));

                allowing(sessionTokenProvider).isInstanceAdminOrSystem(TEST_SESSION_TOKEN);
                will(returnValue(false));

                // begin
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_STARTED)));
                one(databaseTransactionProvider).beginTransaction(with(TEST_TRANSACTION_ID));
                will(returnValue(transaction));
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED)));

                // prepare
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.PREPARE_STARTED)));
                one(databaseTransactionProvider).prepareTransaction(with(TEST_TRANSACTION_ID), with(transaction));
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.PREPARE_FINISHED)));
            }
        });

        // begin
        participant.beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY);
        // prepare
        participant.prepareTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY);
        try
        {
            // repeated prepare (fails)
            participant.prepareTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY);
            fail();
        } catch (Exception e)
        {
            assertEquals(e.getMessage(),
                    "Transaction '" + TEST_TRANSACTION_ID
                            + "' unexpected status 'PREPARE_FINISHED'. Expected statuses '[BEGIN_FINISHED]'.");
        }
    }

    @Test
    public void testPreparedTransactionCannotExecuteOperations() throws Throwable
    {
        TransactionParticipant participant =
                new TransactionParticipant(TEST_PARTICIPANT_ID, TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        databaseTransactionProvider, transactionOperationExecutor, transactionLog, TEST_TRANSACTION_TIMEOUT, TEST_THREAD_COUNT_LIMIT);

        mockery.checking(new Expectations()
        {
            {
                Object transaction = new Object();

                allowing(sessionTokenProvider).isValid(TEST_SESSION_TOKEN);
                will(returnValue(true));

                allowing(sessionTokenProvider).isInstanceAdminOrSystem(TEST_SESSION_TOKEN);
                will(returnValue(false));

                // begin
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_STARTED)));
                one(databaseTransactionProvider).beginTransaction(with(TEST_TRANSACTION_ID));
                will(returnValue(transaction));
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED)));

                // prepare
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.PREPARE_STARTED)));
                one(databaseTransactionProvider).prepareTransaction(with(TEST_TRANSACTION_ID), with(transaction));
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.PREPARE_FINISHED)));
            }
        });

        // begin
        participant.beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY);
        // prepare
        participant.prepareTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY);
        try
        {
            // execute (fails)
            participant.executeOperation(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_OPERATION_NAME,
                    TEST_OPERATION_ARGUMENTS);
            fail();
        } catch (Exception e)
        {
            assertEquals(e.getMessage(),
                    "Transaction '" + TEST_TRANSACTION_ID
                            + "' unexpected status 'PREPARE_FINISHED'. Expected statuses '[BEGIN_FINISHED]'.");
        }
    }

    @Test
    public void testPreparedTransactionCanBeRolledBack() throws Throwable
    {
        TransactionParticipant participant =
                new TransactionParticipant(TEST_PARTICIPANT_ID, TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        databaseTransactionProvider, transactionOperationExecutor, transactionLog, TEST_TRANSACTION_TIMEOUT, TEST_THREAD_COUNT_LIMIT);

        mockery.checking(new Expectations()
        {
            {
                Object transaction = new Object();

                allowing(sessionTokenProvider).isValid(TEST_SESSION_TOKEN);
                will(returnValue(true));

                allowing(sessionTokenProvider).isInstanceAdminOrSystem(TEST_SESSION_TOKEN);
                will(returnValue(false));

                // begin
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_STARTED)));
                one(databaseTransactionProvider).beginTransaction(with(TEST_TRANSACTION_ID));
                will(returnValue(transaction));
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED)));

                // prepare
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.PREPARE_STARTED)));
                one(databaseTransactionProvider).prepareTransaction(with(TEST_TRANSACTION_ID), with(transaction));
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.PREPARE_FINISHED)));

                // rollback
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.ROLLBACK_STARTED)));
                one(databaseTransactionProvider).rollbackTransaction(with(TEST_TRANSACTION_ID), with(transaction), with(true));
                one(transactionLog).deleteTransaction(TEST_TRANSACTION_ID);
            }
        });

        // begin
        participant.beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY);
        // prepare
        participant.prepareTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY);
        // rollback
        participant.rollbackTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY);
    }

    @Test
    public void testPreparedTransactionCanCommit() throws Throwable
    {
        TransactionParticipant participant =
                new TransactionParticipant(TEST_PARTICIPANT_ID, TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        databaseTransactionProvider, transactionOperationExecutor, transactionLog, TEST_TRANSACTION_TIMEOUT, TEST_THREAD_COUNT_LIMIT);

        mockery.checking(new Expectations()
        {
            {
                Object transaction = new Object();

                allowing(sessionTokenProvider).isValid(TEST_SESSION_TOKEN);
                will(returnValue(true));

                allowing(sessionTokenProvider).isInstanceAdminOrSystem(TEST_SESSION_TOKEN);
                will(returnValue(false));

                // begin
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_STARTED)));
                one(databaseTransactionProvider).beginTransaction(with(TEST_TRANSACTION_ID));
                will(returnValue(transaction));
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED)));

                // prepare
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.PREPARE_STARTED)));
                one(databaseTransactionProvider).prepareTransaction(with(TEST_TRANSACTION_ID), with(transaction));
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.PREPARE_FINISHED)));

                // commit
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.COMMIT_STARTED)));
                one(databaseTransactionProvider).commitTransaction(with(TEST_TRANSACTION_ID), with(transaction), with(true));
                one(transactionLog).deleteTransaction(TEST_TRANSACTION_ID);
            }
        });

        // begin
        participant.beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY);
        // prepare
        participant.prepareTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY);
        // commit
        participant.commitTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY);
    }

    @Test(dataProvider = "provideKeys")
    public void testCommittedTransactionIsForgotten(String interactiveSessionKey, String transactionCoordinatorKey) throws Throwable
    {
        TransactionParticipant participant =
                new TransactionParticipant(TEST_PARTICIPANT_ID, TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        databaseTransactionProvider, transactionOperationExecutor, transactionLog, TEST_TRANSACTION_TIMEOUT, TEST_THREAD_COUNT_LIMIT);

        mockery.checking(new Expectations()
        {
            {
                Object transaction = new Object();

                allowing(sessionTokenProvider).isValid(TEST_SESSION_TOKEN);
                will(returnValue(true));

                allowing(sessionTokenProvider).isInstanceAdminOrSystem(TEST_SESSION_TOKEN);
                will(returnValue(false));

                // begin
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_STARTED)));
                one(databaseTransactionProvider).beginTransaction(with(TEST_TRANSACTION_ID));
                will(returnValue(transaction));
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED)));

                if (transactionCoordinatorKey != null)
                {
                    // prepare
                    one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.PREPARE_STARTED)));
                    one(databaseTransactionProvider).prepareTransaction(with(TEST_TRANSACTION_ID), with(transaction));
                    one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.PREPARE_FINISHED)));
                }

                // commit
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.COMMIT_STARTED)));
                one(databaseTransactionProvider).commitTransaction(with(TEST_TRANSACTION_ID), with(transaction),
                        with(transactionCoordinatorKey != null));
                one(transactionLog).deleteTransaction(TEST_TRANSACTION_ID);

                // another begin
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_STARTED)));
                one(databaseTransactionProvider).beginTransaction(with(TEST_TRANSACTION_ID));
                will(returnValue(transaction));
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED)));
            }
        });

        assertNull(participant.getTransaction(TEST_TRANSACTION_ID));
        // begin
        participant.beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, interactiveSessionKey, transactionCoordinatorKey);
        assertNotNull(participant.getTransaction(TEST_TRANSACTION_ID));

        if (transactionCoordinatorKey != null)
        {
            // prepare
            participant.prepareTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, interactiveSessionKey, transactionCoordinatorKey);
            assertNotNull(participant.getTransaction(TEST_TRANSACTION_ID));
        }

        // commit
        participant.commitTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, interactiveSessionKey);
        assertNull(participant.getTransaction(TEST_TRANSACTION_ID));

        // another begin - this is treated as a new transaction as the previous transaction with the same id has been already committed and therefore forgotten
        participant.beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, interactiveSessionKey, transactionCoordinatorKey);
        assertNotNull(participant.getTransaction(TEST_TRANSACTION_ID));
    }

    @DataProvider
    protected Object[][] provideKeys()
    {
        return new Object[][]
                {
                        // interactive flow
                        { TEST_INTERACTIVE_SESSION_KEY, null },
                        { TEST_INTERACTIVE_SESSION_KEY, null },
                        { TEST_INTERACTIVE_SESSION_KEY, null },
                        // coordinator flow
                        { TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY },
                        { TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY },
                        { TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY },
                };
    }

    private Matcher<TransactionLogEntry> logEntry(UUID transactionId, TransactionStatus transactionStatus)
    {
        return new TransactionLogEntryMatcher(transactionId, transactionStatus, null);
    }

}

