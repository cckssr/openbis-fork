package ch.ethz.sis.transaction;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.hamcrest.Matcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TransactionCoordinatorTest
{

    public static final UUID TEST_TRANSACTION_ID = UUID.randomUUID();

    public static final UUID TEST_TRANSACTION_ID_2 = UUID.randomUUID();

    public static final UUID TEST_TRANSACTION_ID_3 = UUID.randomUUID();

    public static final UUID TEST_TRANSACTION_ID_4 = UUID.randomUUID();

    public static final String TEST_PARTICIPANT_ID = "participant-id";

    public static final String TEST_PARTICIPANT_ID_2 = "participant-id-2";

    public static final String TEST_PARTICIPANT_ID_3 = "participant-id-3";

    public static final String TEST_PARTICIPANT_ID_4 = "participant-id-4";

    public static final String TEST_SESSION_TOKEN = "test-session-token";

    public static final String TEST_INTERACTIVE_SESSION_KEY = "test-interactive-session-key";

    public static final String TEST_TRANSACTION_COORDINATOR_KEY = "test-transaction-coordinator-key";

    public static final String INVALID_INTERACTIVE_SESSION_KEY = "invalid-interactive-session-key";

    public static final String INVALID_SESSION_TOKEN = "invalid-session-token";

    public static final String UNKNOWN_PARTICIPANT_ID = "unknown-participant-id";

    public static final String TEST_OPERATION_NAME = "test-operation";

    public static final String TEST_OPERATION_NAME_2 = "test-operation-2";

    public static final String TEST_OPERATION_NAME_3 = "test-operation-3";

    public static final Object[] TEST_OPERATION_ARGUMENTS = new Object[] { 1, "abc" };

    public static final String TEST_OPERATION_RESULT = "test-operation-result";

    public static final int TEST_TIMEOUT = 60;

    public static final int TEST_COUNT_LIMIT = 10;

    private Mockery mockery;

    private ITransactionParticipant participant1;

    private ITransactionParticipant participant2;

    private ITransactionParticipant participant3;

    private ITransactionParticipant participant4;

    private ISessionTokenProvider sessionTokenProvider;

    private ITransactionLog transactionLog;

    @BeforeMethod
    protected void beforeMethod()
    {
        mockery = new Mockery();
        participant1 = mockery.mock(ITransactionParticipant.class, "participant1");
        participant2 = mockery.mock(ITransactionParticipant.class, "participant2");
        participant3 = mockery.mock(ITransactionParticipant.class, "participant3");
        participant4 = mockery.mock(ITransactionParticipant.class, "participant4");
        sessionTokenProvider = mockery.mock(ISessionTokenProvider.class);
        transactionLog = mockery.mock(ITransactionLog.class);
    }

    @AfterMethod
    protected void afterMethod()
    {
        mockery.assertIsSatisfied();
    }

    @Test
    public void testBeginTransactionSucceeds()
    {
        TransactionCoordinator coordinator =
                new TransactionCoordinator(TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        List.of(participant1, participant2), transactionLog, TEST_TIMEOUT, TEST_COUNT_LIMIT);

        mockery.checking(new Expectations()
        {
            {
                allowing(participant1).getParticipantId();
                allowing(participant2).getParticipantId();

                allowing(sessionTokenProvider).isValid(TEST_SESSION_TOKEN);
                will(returnValue(true));

                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_STARTED, Set.of())));
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED, Set.of())));
            }
        });

        coordinator.beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY);
    }

    @DataProvider
    protected Object[][] provideInvalidArgumentsForBegin()
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

    @Test(dataProvider = "provideInvalidArgumentsForBegin")
    public void testBeginTransactionWithInvalidArguments(UUID transactionId, String sessionToken, String interactiveSessionKey,
            String expectedException)
    {
        TransactionCoordinator coordinator =
                new TransactionCoordinator(TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        List.of(participant1, participant2), transactionLog, TEST_TIMEOUT, TEST_COUNT_LIMIT);

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
            coordinator.beginTransaction(transactionId, sessionToken, interactiveSessionKey);
            Assert.fail();
        } catch (Throwable t)
        {
            assertEquals(t.getMessage(), expectedException);
        }
    }

    @Test
    public void testExecuteOperationSucceeds()
    {
        TransactionCoordinator coordinator =
                new TransactionCoordinator(TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        List.of(participant1, participant2), transactionLog, TEST_TIMEOUT, TEST_COUNT_LIMIT);

        mockery.checking(new Expectations()
        {
            {
                allowing(participant1).getParticipantId();
                will(returnValue(TEST_PARTICIPANT_ID));
                allowing(participant2).getParticipantId();
                will(returnValue(TEST_PARTICIPANT_ID_2));

                allowing(sessionTokenProvider).isValid(TEST_SESSION_TOKEN);
                will(returnValue(true));

                allowing(sessionTokenProvider).isInstanceAdminOrSystem(TEST_SESSION_TOKEN);
                will(returnValue(false));

                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_STARTED, Set.of())));
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED, Set.of())));

                one(participant1).beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY,
                        TEST_TRANSACTION_COORDINATOR_KEY);
                one(transactionLog).logTransaction(
                        with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED, Set.of(TEST_PARTICIPANT_ID))));
                one(participant1).executeOperation(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_OPERATION_NAME,
                        TEST_OPERATION_ARGUMENTS);
            }
        });

        coordinator.beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY);
        coordinator.executeOperation(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_PARTICIPANT_ID, TEST_OPERATION_NAME,
                TEST_OPERATION_ARGUMENTS);
    }

    @Test
    public void testExecuteOperationFails()
    {
        TransactionCoordinator coordinator =
                new TransactionCoordinator(TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        List.of(participant1, participant2), transactionLog, TEST_TIMEOUT, TEST_COUNT_LIMIT);

        Exception executeOperationException = new RuntimeException();

        mockery.checking(new Expectations()
        {
            {
                allowing(participant1).getParticipantId();
                will(returnValue(TEST_PARTICIPANT_ID));
                allowing(participant2).getParticipantId();
                will(returnValue(TEST_PARTICIPANT_ID_2));

                allowing(sessionTokenProvider).isValid(TEST_SESSION_TOKEN);
                will(returnValue(true));

                allowing(sessionTokenProvider).isInstanceAdminOrSystem(TEST_SESSION_TOKEN);
                will(returnValue(false));

                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_STARTED, Set.of())));
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED, Set.of())));

                one(participant1).beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY,
                        TEST_TRANSACTION_COORDINATOR_KEY);
                one(transactionLog).logTransaction(
                        with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED, Set.of(TEST_PARTICIPANT_ID))));
                one(participant1).executeOperation(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_OPERATION_NAME,
                        TEST_OPERATION_ARGUMENTS);
                will(throwException(executeOperationException));
            }
        });

        coordinator.beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY);

        try
        {
            coordinator.executeOperation(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_PARTICIPANT_ID,
                    TEST_OPERATION_NAME,
                    TEST_OPERATION_ARGUMENTS);
            fail();
        } catch (Exception e)
        {
            assertEquals(e.getMessage(),
                    "Transaction '" + TEST_TRANSACTION_ID + "' execute operation '" + TEST_OPERATION_NAME + "' for participant '"
                            + TEST_PARTICIPANT_ID + "' failed.");
            assertEquals(e.getCause(), executeOperationException);
        }
    }

    @Test
    public void testExecuteOperationFailsAtBegin()
    {
        TransactionCoordinator coordinator =
                new TransactionCoordinator(TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        List.of(participant1, participant2), transactionLog, TEST_TIMEOUT, TEST_COUNT_LIMIT);

        Exception executeOperationException = new RuntimeException();

        mockery.checking(new Expectations()
        {
            {
                allowing(participant1).getParticipantId();
                will(returnValue(TEST_PARTICIPANT_ID));
                allowing(participant2).getParticipantId();
                will(returnValue(TEST_PARTICIPANT_ID_2));

                allowing(sessionTokenProvider).isValid(TEST_SESSION_TOKEN);
                will(returnValue(true));

                allowing(sessionTokenProvider).isInstanceAdminOrSystem(TEST_SESSION_TOKEN);
                will(returnValue(false));

                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_STARTED, Set.of())));
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED, Set.of())));

                one(participant1).beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY,
                        TEST_TRANSACTION_COORDINATOR_KEY);
                will(throwException(executeOperationException));
            }
        });

        coordinator.beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY);

        try
        {
            coordinator.executeOperation(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_PARTICIPANT_ID,
                    TEST_OPERATION_NAME,
                    TEST_OPERATION_ARGUMENTS);
            fail();
        } catch (Exception e)
        {
            assertEquals(e.getMessage(), "Transaction '" + TEST_TRANSACTION_ID + "' execute operation '" + TEST_OPERATION_NAME + "' for participant '"
                    + TEST_PARTICIPANT_ID + "' failed.");
            assertEquals(e.getCause().getMessage(),
                    "Begin transaction '" + TEST_TRANSACTION_ID + "' failed for participant '" + TEST_PARTICIPANT_ID + "'.");
            assertEquals(e.getCause().getCause(), executeOperationException);
        }
    }

    @Test
    public void testExecuteMultipleOperations()
    {
        TransactionCoordinator coordinator =
                new TransactionCoordinator(TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        List.of(participant1, participant2), transactionLog, TEST_TIMEOUT, TEST_COUNT_LIMIT);

        Exception executeOperationException = new RuntimeException();

        mockery.checking(new Expectations()
        {
            {
                allowing(participant1).getParticipantId();
                will(returnValue(TEST_PARTICIPANT_ID));
                allowing(participant2).getParticipantId();
                will(returnValue(TEST_PARTICIPANT_ID_2));

                allowing(sessionTokenProvider).isValid(TEST_SESSION_TOKEN);
                will(returnValue(true));

                allowing(sessionTokenProvider).isInstanceAdminOrSystem(TEST_SESSION_TOKEN);
                will(returnValue(false));

                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_STARTED, Set.of())));
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED, Set.of())));

                one(participant1).beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY,
                        TEST_TRANSACTION_COORDINATOR_KEY);
                one(transactionLog).logTransaction(
                        with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED, Set.of(TEST_PARTICIPANT_ID))));
                one(participant1).executeOperation(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_OPERATION_NAME,
                        TEST_OPERATION_ARGUMENTS);

                one(participant1).executeOperation(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_OPERATION_NAME_2,
                        TEST_OPERATION_ARGUMENTS);
                will(throwException(executeOperationException));

                one(participant1).executeOperation(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_OPERATION_NAME_3,
                        TEST_OPERATION_ARGUMENTS);
                will(returnValue(TEST_OPERATION_RESULT));
            }
        });

        coordinator.beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY);

        coordinator.executeOperation(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_PARTICIPANT_ID,
                TEST_OPERATION_NAME,
                TEST_OPERATION_ARGUMENTS);

        try
        {
            coordinator.executeOperation(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_PARTICIPANT_ID,
                    TEST_OPERATION_NAME_2,
                    TEST_OPERATION_ARGUMENTS);
            fail();
        } catch (Exception e)
        {
            assertEquals(e.getMessage(),
                    "Transaction '" + TEST_TRANSACTION_ID + "' execute operation '" + TEST_OPERATION_NAME_2 + "' for participant '"
                            + TEST_PARTICIPANT_ID + "' failed.");
            assertEquals(e.getCause(), executeOperationException);
        }

        Object result = coordinator.executeOperation(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_PARTICIPANT_ID,
                TEST_OPERATION_NAME_3,
                TEST_OPERATION_ARGUMENTS);

        assertEquals(result, TEST_OPERATION_RESULT);
    }

    @DataProvider
    protected Object[][] provideInvalidArgumentsForExecuteOperations()
    {
        return new Object[][]
                {
                        { null, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_PARTICIPANT_ID, TEST_OPERATION_NAME, TEST_OPERATION_ARGUMENTS,
                                "Transaction id cannot be null" },
                        { TEST_TRANSACTION_ID, null, TEST_INTERACTIVE_SESSION_KEY, TEST_PARTICIPANT_ID, TEST_OPERATION_NAME, TEST_OPERATION_ARGUMENTS,
                                "Session token cannot be null" },
                        { TEST_TRANSACTION_ID, INVALID_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_PARTICIPANT_ID, TEST_OPERATION_NAME,
                                TEST_OPERATION_ARGUMENTS, "Invalid session token" },
                        { TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, null, TEST_PARTICIPANT_ID, TEST_OPERATION_NAME,
                                TEST_OPERATION_ARGUMENTS, "Interactive session key cannot be null" },
                        { TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, INVALID_INTERACTIVE_SESSION_KEY, TEST_PARTICIPANT_ID, TEST_OPERATION_NAME,
                                TEST_OPERATION_ARGUMENTS, "Invalid interactive session key" },
                        { TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, null, TEST_OPERATION_NAME,
                                TEST_OPERATION_ARGUMENTS, "Participant id cannot be null" },
                        { TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, UNKNOWN_PARTICIPANT_ID, TEST_OPERATION_NAME,
                                TEST_OPERATION_ARGUMENTS, "Unknown participant with id '" + UNKNOWN_PARTICIPANT_ID + "'" },
                        { TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_PARTICIPANT_ID, null, TEST_OPERATION_ARGUMENTS,
                                "Operation name cannot be null" },
                        { TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_PARTICIPANT_ID, TEST_OPERATION_NAME, null,
                                "Operation arguments cannot be null" },
                };
    }

    @Test(dataProvider = "provideInvalidArgumentsForExecuteOperations")
    public void testExecuteOperationWithInvalidArguments(UUID transactionId, String sessionToken, String interactiveSessionKeyOrNull,
            String participantId,
            String operationName, Object[] operationArguments, String expectedExceptionMessage)
    {
        TransactionCoordinator coordinator =
                new TransactionCoordinator(TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        List.of(participant1, participant2), transactionLog, TEST_TIMEOUT, TEST_COUNT_LIMIT);

        mockery.checking(new Expectations()
        {
            {
                allowing(participant1).getParticipantId();
                will(returnValue(TEST_PARTICIPANT_ID));
                allowing(participant2).getParticipantId();
                will(returnValue(TEST_PARTICIPANT_ID_2));

                allowing(sessionTokenProvider).isValid(TEST_SESSION_TOKEN);
                will(returnValue(true));

                allowing(sessionTokenProvider).isValid(INVALID_SESSION_TOKEN);
                will(returnValue(false));

                allowing(sessionTokenProvider).isInstanceAdminOrSystem(TEST_SESSION_TOKEN);
                will(returnValue(false));

                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_STARTED, Set.of())));
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED, Set.of())));
            }
        });

        coordinator.beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY);

        try
        {
            coordinator.executeOperation(transactionId, sessionToken, interactiveSessionKeyOrNull, participantId,
                    operationName, operationArguments);
            fail();
        } catch (Exception e)
        {
            assertEquals(e.getMessage(), expectedExceptionMessage);
        }
    }

    @Test
    public void testCommitTransactionSucceeds()
    {
        TransactionCoordinator coordinator =
                new TransactionCoordinator(TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        List.of(participant1, participant2, participant3), transactionLog, TEST_TIMEOUT, TEST_COUNT_LIMIT);

        mockery.checking(new Expectations()
        {
            {
                allowing(participant1).getParticipantId();
                will(returnValue(TEST_PARTICIPANT_ID));
                allowing(participant2).getParticipantId();
                will(returnValue(TEST_PARTICIPANT_ID_2));
                allowing(participant3).getParticipantId();
                will(returnValue(TEST_PARTICIPANT_ID_3));

                allowing(sessionTokenProvider).isValid(TEST_SESSION_TOKEN);
                will(returnValue(true));

                allowing(sessionTokenProvider).isInstanceAdminOrSystem(TEST_SESSION_TOKEN);
                will(returnValue(false));

                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_STARTED, Set.of())));
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED, Set.of())));

                one(participant1).beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY,
                        TEST_TRANSACTION_COORDINATOR_KEY);
                one(transactionLog).logTransaction(
                        with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED, Set.of(TEST_PARTICIPANT_ID))));
                one(participant1).executeOperation(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_OPERATION_NAME,
                        TEST_OPERATION_ARGUMENTS);

                one(participant2).beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY,
                        TEST_TRANSACTION_COORDINATOR_KEY);
                one(transactionLog).logTransaction(
                        with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED, Set.of(TEST_PARTICIPANT_ID, TEST_PARTICIPANT_ID_2))));
                one(participant2).executeOperation(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_OPERATION_NAME_2,
                        TEST_OPERATION_ARGUMENTS);

                one(transactionLog).logTransaction(
                        with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.PREPARE_STARTED, Set.of(TEST_PARTICIPANT_ID, TEST_PARTICIPANT_ID_2))));

                one(participant1).prepareTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY,
                        TEST_TRANSACTION_COORDINATOR_KEY);
                one(participant2).prepareTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY,
                        TEST_TRANSACTION_COORDINATOR_KEY);

                one(transactionLog).logTransaction(
                        with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.PREPARE_FINISHED, Set.of(TEST_PARTICIPANT_ID, TEST_PARTICIPANT_ID_2))));
                one(transactionLog).logTransaction(
                        with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.COMMIT_STARTED, Set.of(TEST_PARTICIPANT_ID, TEST_PARTICIPANT_ID_2))));

                one(participant1).commitTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY);
                one(participant2).commitTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY);

                one(transactionLog).deleteTransaction(TEST_TRANSACTION_ID);
            }
        });

        coordinator.beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY);
        coordinator.executeOperation(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_PARTICIPANT_ID, TEST_OPERATION_NAME,
                TEST_OPERATION_ARGUMENTS);
        coordinator.executeOperation(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_PARTICIPANT_ID_2,
                TEST_OPERATION_NAME_2, TEST_OPERATION_ARGUMENTS);
        coordinator.commitTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY);
    }

    @Test
    public void testCommitTransactionFailsDuringPrepare()
    {
        TransactionCoordinator coordinator =
                new TransactionCoordinator(TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        List.of(participant1, participant2, participant3), transactionLog, TEST_TIMEOUT, TEST_COUNT_LIMIT);

        Exception prepareException = new RuntimeException();
        Exception rollbackException = new RuntimeException();

        mockery.checking(new Expectations()
        {
            {
                allowing(participant1).getParticipantId();
                will(returnValue(TEST_PARTICIPANT_ID));
                allowing(participant2).getParticipantId();
                will(returnValue(TEST_PARTICIPANT_ID_2));
                allowing(participant3).getParticipantId();
                will(returnValue(TEST_PARTICIPANT_ID_3));

                allowing(sessionTokenProvider).isValid(TEST_SESSION_TOKEN);
                will(returnValue(true));

                allowing(sessionTokenProvider).isInstanceAdminOrSystem(TEST_SESSION_TOKEN);
                will(returnValue(false));

                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_STARTED, Set.of())));
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED, Set.of())));

                one(participant1).beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY,
                        TEST_TRANSACTION_COORDINATOR_KEY);
                one(transactionLog).logTransaction(
                        with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED, Set.of(TEST_PARTICIPANT_ID))));
                one(participant1).executeOperation(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_OPERATION_NAME,
                        TEST_OPERATION_ARGUMENTS);

                one(participant2).beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY,
                        TEST_TRANSACTION_COORDINATOR_KEY);
                one(transactionLog).logTransaction(
                        with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED, Set.of(TEST_PARTICIPANT_ID, TEST_PARTICIPANT_ID_2))));
                one(participant2).executeOperation(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_OPERATION_NAME_2,
                        TEST_OPERATION_ARGUMENTS);

                one(transactionLog).logTransaction(
                        with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.PREPARE_STARTED, Set.of(TEST_PARTICIPANT_ID, TEST_PARTICIPANT_ID_2))));

                one(participant1).prepareTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY,
                        TEST_TRANSACTION_COORDINATOR_KEY);
                one(participant2).prepareTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY,
                        TEST_TRANSACTION_COORDINATOR_KEY);
                will(throwException(prepareException));

                one(transactionLog).logTransaction(
                        with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.ROLLBACK_STARTED, Set.of(TEST_PARTICIPANT_ID, TEST_PARTICIPANT_ID_2))));

                one(participant1).rollbackTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY);
                // test that a failing rollback won't prevent other rollbacks from being called
                will(throwException(rollbackException));
                one(participant2).rollbackTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY);
            }
        });

        coordinator.beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY);
        coordinator.executeOperation(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_PARTICIPANT_ID, TEST_OPERATION_NAME,
                TEST_OPERATION_ARGUMENTS);
        coordinator.executeOperation(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_PARTICIPANT_ID_2,
                TEST_OPERATION_NAME_2, TEST_OPERATION_ARGUMENTS);

        try
        {
            coordinator.commitTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY);
            fail();
        } catch (Exception e)
        {
            assertEquals(e.getMessage(), "Commit transaction '" + TEST_TRANSACTION_ID + "' failed.");
            assertEquals(e.getCause().getMessage(),
                    "Prepare transaction '" + TEST_TRANSACTION_ID + "' failed for participant '" + TEST_PARTICIPANT_ID_2
                            + "'. The transaction was rolled back.");
            assertEquals(e.getCause().getCause(), prepareException);
        }
    }

    @Test
    public void testCommitTransactionFailsDuringCommit()
    {
        TransactionCoordinator coordinator =
                new TransactionCoordinator(TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        List.of(participant1, participant2, participant3), transactionLog, TEST_TIMEOUT, TEST_COUNT_LIMIT);

        Exception commitException = new RuntimeException();

        mockery.checking(new Expectations()
        {
            {
                allowing(participant1).getParticipantId();
                will(returnValue(TEST_PARTICIPANT_ID));
                allowing(participant2).getParticipantId();
                will(returnValue(TEST_PARTICIPANT_ID_2));
                allowing(participant3).getParticipantId();
                will(returnValue(TEST_PARTICIPANT_ID_3));

                allowing(sessionTokenProvider).isValid(TEST_SESSION_TOKEN);
                will(returnValue(true));

                allowing(sessionTokenProvider).isInstanceAdminOrSystem(TEST_SESSION_TOKEN);
                will(returnValue(false));

                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_STARTED, Set.of())));
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED, Set.of())));

                one(participant1).beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY,
                        TEST_TRANSACTION_COORDINATOR_KEY);
                one(transactionLog).logTransaction(
                        with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED, Set.of(TEST_PARTICIPANT_ID))));
                one(participant1).executeOperation(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_OPERATION_NAME,
                        TEST_OPERATION_ARGUMENTS);

                one(participant2).beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY,
                        TEST_TRANSACTION_COORDINATOR_KEY);
                one(transactionLog).logTransaction(
                        with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED, Set.of(TEST_PARTICIPANT_ID, TEST_PARTICIPANT_ID_2))));
                one(participant2).executeOperation(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_OPERATION_NAME_2,
                        TEST_OPERATION_ARGUMENTS);

                one(transactionLog).logTransaction(
                        with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.PREPARE_STARTED, Set.of(TEST_PARTICIPANT_ID, TEST_PARTICIPANT_ID_2))));

                one(participant1).prepareTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY,
                        TEST_TRANSACTION_COORDINATOR_KEY);
                one(participant2).prepareTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY,
                        TEST_TRANSACTION_COORDINATOR_KEY);

                one(transactionLog).logTransaction(
                        with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.PREPARE_FINISHED, Set.of(TEST_PARTICIPANT_ID, TEST_PARTICIPANT_ID_2))));
                one(transactionLog).logTransaction(
                        with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.COMMIT_STARTED, Set.of(TEST_PARTICIPANT_ID, TEST_PARTICIPANT_ID_2))));

                // test that a failing commit won't prevent other commits from being called
                one(participant1).commitTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY);
                will(throwException(commitException));
                one(participant2).commitTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY);
            }
        });

        coordinator.beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY);
        coordinator.executeOperation(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_PARTICIPANT_ID, TEST_OPERATION_NAME,
                TEST_OPERATION_ARGUMENTS);
        coordinator.executeOperation(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_PARTICIPANT_ID_2,
                TEST_OPERATION_NAME_2, TEST_OPERATION_ARGUMENTS);
        coordinator.commitTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY);
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
            String expectedException)
    {
        TransactionCoordinator coordinator =
                new TransactionCoordinator(TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        List.of(participant1, participant2), transactionLog, TEST_TIMEOUT, TEST_COUNT_LIMIT);

        mockery.checking(new Expectations()
        {
            {
                allowing(participant1).getParticipantId();
                will(returnValue(TEST_PARTICIPANT_ID));
                allowing(participant2).getParticipantId();
                will(returnValue(TEST_PARTICIPANT_ID_2));

                allowing(sessionTokenProvider).isValid(TEST_SESSION_TOKEN);
                will(returnValue(true));

                allowing(sessionTokenProvider).isValid(INVALID_SESSION_TOKEN);
                will(returnValue(false));

                allowing(sessionTokenProvider).isInstanceAdminOrSystem(TEST_SESSION_TOKEN);
                will(returnValue(false));

                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_STARTED, Set.of())));
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED, Set.of())));
            }
        });

        coordinator.beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY);

        try
        {
            coordinator.commitTransaction(transactionId, sessionToken, interactiveSessionKey);
            Assert.fail();
        } catch (Throwable t)
        {
            assertEquals(t.getMessage(), expectedException);
        }
    }

    @Test
    public void testRollbackTransactionSucceeds()
    {
        TransactionCoordinator coordinator =
                new TransactionCoordinator(TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        List.of(participant1, participant2, participant3), transactionLog, TEST_TIMEOUT, TEST_COUNT_LIMIT);

        mockery.checking(new Expectations()
        {
            {
                allowing(participant1).getParticipantId();
                will(returnValue(TEST_PARTICIPANT_ID));
                allowing(participant2).getParticipantId();
                will(returnValue(TEST_PARTICIPANT_ID_2));
                allowing(participant3).getParticipantId();
                will(returnValue(TEST_PARTICIPANT_ID_3));

                allowing(sessionTokenProvider).isValid(TEST_SESSION_TOKEN);
                will(returnValue(true));

                allowing(sessionTokenProvider).isInstanceAdminOrSystem(TEST_SESSION_TOKEN);
                will(returnValue(false));

                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_STARTED, Set.of())));
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED, Set.of())));

                one(participant1).beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY,
                        TEST_TRANSACTION_COORDINATOR_KEY);
                one(transactionLog).logTransaction(
                        with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED, Set.of(TEST_PARTICIPANT_ID))));
                one(participant1).executeOperation(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_OPERATION_NAME,
                        TEST_OPERATION_ARGUMENTS);

                one(participant2).beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY,
                        TEST_TRANSACTION_COORDINATOR_KEY);
                one(transactionLog).logTransaction(
                        with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED, Set.of(TEST_PARTICIPANT_ID, TEST_PARTICIPANT_ID_2))));
                one(participant2).executeOperation(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_OPERATION_NAME_2,
                        TEST_OPERATION_ARGUMENTS);

                one(transactionLog).logTransaction(
                        with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.ROLLBACK_STARTED, Set.of(TEST_PARTICIPANT_ID, TEST_PARTICIPANT_ID_2))));

                one(participant1).rollbackTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY);
                one(participant2).rollbackTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY);

                one(transactionLog).deleteTransaction(TEST_TRANSACTION_ID);
            }
        });

        coordinator.beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY);
        coordinator.executeOperation(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_PARTICIPANT_ID, TEST_OPERATION_NAME,
                TEST_OPERATION_ARGUMENTS);
        coordinator.executeOperation(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_PARTICIPANT_ID_2,
                TEST_OPERATION_NAME_2, TEST_OPERATION_ARGUMENTS);
        coordinator.rollbackTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY);
    }

    @Test
    public void testRollbackTransactionFails()
    {
        TransactionCoordinator coordinator =
                new TransactionCoordinator(TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        List.of(participant1, participant2, participant3), transactionLog, TEST_TIMEOUT, TEST_COUNT_LIMIT);

        Exception rollbackException = new RuntimeException();

        mockery.checking(new Expectations()
        {
            {
                allowing(participant1).getParticipantId();
                will(returnValue(TEST_PARTICIPANT_ID));
                allowing(participant2).getParticipantId();
                will(returnValue(TEST_PARTICIPANT_ID_2));
                allowing(participant3).getParticipantId();
                will(returnValue(TEST_PARTICIPANT_ID_3));

                allowing(sessionTokenProvider).isValid(TEST_SESSION_TOKEN);
                will(returnValue(true));

                allowing(sessionTokenProvider).isInstanceAdminOrSystem(TEST_SESSION_TOKEN);
                will(returnValue(false));

                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_STARTED, Set.of())));
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED, Set.of())));

                one(participant1).beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY,
                        TEST_TRANSACTION_COORDINATOR_KEY);
                one(transactionLog).logTransaction(
                        with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED, Set.of(TEST_PARTICIPANT_ID))));
                one(participant1).executeOperation(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_OPERATION_NAME,
                        TEST_OPERATION_ARGUMENTS);

                one(participant2).beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY,
                        TEST_TRANSACTION_COORDINATOR_KEY);
                one(transactionLog).logTransaction(
                        with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED, Set.of(TEST_PARTICIPANT_ID, TEST_PARTICIPANT_ID_2))));
                one(participant2).executeOperation(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_OPERATION_NAME_2,
                        TEST_OPERATION_ARGUMENTS);

                one(transactionLog).logTransaction(
                        with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.ROLLBACK_STARTED, Set.of(TEST_PARTICIPANT_ID, TEST_PARTICIPANT_ID_2))));

                // test that a failing rollback won't prevent other rollbacks from being called
                one(participant1).rollbackTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY);
                will(throwException(rollbackException));
                one(participant2).rollbackTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY);
            }
        });

        coordinator.beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY);
        coordinator.executeOperation(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_PARTICIPANT_ID, TEST_OPERATION_NAME,
                TEST_OPERATION_ARGUMENTS);
        coordinator.executeOperation(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY, TEST_PARTICIPANT_ID_2,
                TEST_OPERATION_NAME_2, TEST_OPERATION_ARGUMENTS);
        coordinator.rollbackTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY);
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
                        { TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, INVALID_INTERACTIVE_SESSION_KEY, "Invalid interactive session key" }
                };
    }

    @Test(dataProvider = "provideInvalidArgumentsForRollback")
    public void testRollbackTransactionWithInvalidArguments(UUID transactionId, String sessionToken, String interactiveSessionKey,
            String expectedException)
    {
        TransactionCoordinator coordinator =
                new TransactionCoordinator(TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        List.of(participant1, participant2), transactionLog, TEST_TIMEOUT, TEST_COUNT_LIMIT);

        mockery.checking(new Expectations()
        {
            {
                allowing(participant1).getParticipantId();
                will(returnValue(TEST_PARTICIPANT_ID));
                allowing(participant2).getParticipantId();
                will(returnValue(TEST_PARTICIPANT_ID_2));

                allowing(sessionTokenProvider).isValid(TEST_SESSION_TOKEN);
                will(returnValue(true));

                allowing(sessionTokenProvider).isValid(INVALID_SESSION_TOKEN);
                will(returnValue(false));

                allowing(sessionTokenProvider).isInstanceAdminOrSystem(TEST_SESSION_TOKEN);
                will(returnValue(false));

                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_STARTED, Set.of())));
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.BEGIN_FINISHED, Set.of())));
            }
        });

        coordinator.beginTransaction(TEST_TRANSACTION_ID, TEST_SESSION_TOKEN, TEST_INTERACTIVE_SESSION_KEY);

        try
        {
            coordinator.rollbackTransaction(transactionId, sessionToken, interactiveSessionKey);
            Assert.fail();
        } catch (Throwable t)
        {
            assertEquals(t.getMessage(), expectedException);
        }
    }

    @Test(dataProvider = "provideTestRecoverTransactionWithStatus")
    public void testRecoverTransactionWithStatus(TransactionStatus transactionStatus, boolean throwException)
    {
        TransactionCoordinator coordinator =
                new TransactionCoordinator(TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        List.of(participant1, participant2, participant3, participant4), transactionLog, TEST_TIMEOUT, TEST_COUNT_LIMIT);

        TransactionLogEntry logEntry = new TransactionLogEntry();
        logEntry.setTransactionId(TEST_TRANSACTION_ID);
        logEntry.setTwoPhaseTransaction(true);
        logEntry.setParticipantIds(Set.of(TEST_PARTICIPANT_ID, TEST_PARTICIPANT_ID_2, TEST_PARTICIPANT_ID_3));
        logEntry.setTransactionStatus(transactionStatus);

        Map<UUID, TransactionLogEntry> logEntries = Map.of(logEntry.getTransactionId(), logEntry);

        Exception exception = new RuntimeException();

        mockery.checking(new Expectations()
        {
            {
                allowing(participant1).getParticipantId();
                will(returnValue(TEST_PARTICIPANT_ID));
                allowing(participant2).getParticipantId();
                will(returnValue(TEST_PARTICIPANT_ID_2));
                allowing(participant3).getParticipantId();
                will(returnValue(TEST_PARTICIPANT_ID_3));
                allowing(participant4).getParticipantId();
                will(returnValue(TEST_PARTICIPANT_ID_4));

                one(transactionLog).getTransactions();
                will(returnValue(logEntries));

                switch (transactionStatus)
                {
                    case BEGIN_STARTED:
                    case BEGIN_FINISHED:
                    case PREPARE_STARTED:
                    case ROLLBACK_STARTED:
                        one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.ROLLBACK_STARTED,
                                Set.of(TEST_PARTICIPANT_ID, TEST_PARTICIPANT_ID_2, TEST_PARTICIPANT_ID_3))));
                        one(participant1).rollbackRecoveredTransaction(TEST_TRANSACTION_ID, TEST_INTERACTIVE_SESSION_KEY,
                                TEST_TRANSACTION_COORDINATOR_KEY);

                        if (throwException)
                        {
                            // test that a failing rollback won't prevent other rollbacks from being called
                            will(throwException(exception));
                        }

                        one(participant2).rollbackRecoveredTransaction(TEST_TRANSACTION_ID, TEST_INTERACTIVE_SESSION_KEY,
                                TEST_TRANSACTION_COORDINATOR_KEY);
                        one(participant3).rollbackRecoveredTransaction(TEST_TRANSACTION_ID, TEST_INTERACTIVE_SESSION_KEY,
                                TEST_TRANSACTION_COORDINATOR_KEY);

                        if (!throwException)
                        {
                            one(transactionLog).deleteTransaction(TEST_TRANSACTION_ID);
                        }

                        break;
                    case PREPARE_FINISHED:
                    case COMMIT_STARTED:
                        // only participant 1 and 2 know the transaction
                        one(participant1).recoverTransactions(TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY);
                        will(returnValue(Collections.singletonList(TEST_TRANSACTION_ID)));
                        one(participant2).recoverTransactions(TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY);
                        will(returnValue(Collections.singletonList(TEST_TRANSACTION_ID)));
                        one(participant3).recoverTransactions(TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY);
                        will(returnValue(Collections.emptyList()));

                        one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.COMMIT_STARTED,
                                Set.of(TEST_PARTICIPANT_ID, TEST_PARTICIPANT_ID_2, TEST_PARTICIPANT_ID_3))));

                        one(participant1).commitRecoveredTransaction(TEST_TRANSACTION_ID, TEST_INTERACTIVE_SESSION_KEY,
                                TEST_TRANSACTION_COORDINATOR_KEY);

                        if (throwException)
                        {
                            // test that a failing commit won't prevent other commits from being called
                            will(throwException(exception));
                        }

                        one(participant2).commitRecoveredTransaction(TEST_TRANSACTION_ID, TEST_INTERACTIVE_SESSION_KEY,
                                TEST_TRANSACTION_COORDINATOR_KEY);

                        if (!throwException)
                        {
                            one(transactionLog).deleteTransaction(TEST_TRANSACTION_ID);
                        }
                        break;
                    case NEW:
                    case COMMIT_FINISHED:
                    case ROLLBACK_FINISHED:
                        one(transactionLog).deleteTransaction(TEST_TRANSACTION_ID);
                }
            }
        });

        coordinator.recoverTransactionsFromTransactionLog();
        coordinator.finishFailedOrAbandonedTransactions();
    }

    @Test
    public void testRecoverMultipleTransactions()
    {
        TransactionCoordinator coordinator =
                new TransactionCoordinator(TEST_TRANSACTION_COORDINATOR_KEY, TEST_INTERACTIVE_SESSION_KEY, sessionTokenProvider,
                        List.of(participant1, participant2, participant3), transactionLog, TEST_TIMEOUT, TEST_COUNT_LIMIT);

        TransactionLogEntry logEntry = new TransactionLogEntry();
        logEntry.setTransactionId(TEST_TRANSACTION_ID);
        logEntry.setTwoPhaseTransaction(true);
        logEntry.setParticipantIds(Set.of(TEST_PARTICIPANT_ID, TEST_PARTICIPANT_ID_2));
        logEntry.setTransactionStatus(TransactionStatus.PREPARE_FINISHED);

        TransactionLogEntry logEntry2 = new TransactionLogEntry();
        logEntry2.setTransactionId(TEST_TRANSACTION_ID_2);
        logEntry2.setTwoPhaseTransaction(true);
        logEntry2.setParticipantIds(Set.of(TEST_PARTICIPANT_ID, TEST_PARTICIPANT_ID_2));
        logEntry2.setTransactionStatus(TransactionStatus.COMMIT_STARTED);

        TransactionLogEntry logEntry3 = new TransactionLogEntry();
        logEntry3.setTransactionId(TEST_TRANSACTION_ID_3);
        logEntry3.setTwoPhaseTransaction(true);
        logEntry3.setParticipantIds(Set.of(TEST_PARTICIPANT_ID, TEST_PARTICIPANT_ID_2));
        logEntry3.setTransactionStatus(TransactionStatus.ROLLBACK_STARTED);

        TransactionLogEntry logEntry4 = new TransactionLogEntry();
        logEntry4.setTransactionId(TEST_TRANSACTION_ID_4);
        logEntry4.setTwoPhaseTransaction(true);
        logEntry4.setParticipantIds(Set.of(TEST_PARTICIPANT_ID_3));
        logEntry4.setTransactionStatus(TransactionStatus.ROLLBACK_STARTED);

        Map<UUID, TransactionLogEntry> logEntries =
                Map.of(logEntry.getTransactionId(), logEntry, logEntry2.getTransactionId(), logEntry2, logEntry3.getTransactionId(), logEntry3,
                        logEntry4.getTransactionId(), logEntry4);

        Exception exception = new RuntimeException();

        mockery.checking(new Expectations()
        {
            {
                allowing(participant1).getParticipantId();
                will(returnValue(TEST_PARTICIPANT_ID));
                allowing(participant2).getParticipantId();
                will(returnValue(TEST_PARTICIPANT_ID_2));
                allowing(participant3).getParticipantId();
                will(returnValue(TEST_PARTICIPANT_ID_3));

                one(transactionLog).getTransactions();
                will(returnValue(logEntries));

                // participant 1 (transactions 1, 2); participant 2 (transactions 1, 3); participant 3 (no transactions)
                allowing(participant1).recoverTransactions(TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY);
                will(returnValue(Arrays.asList(TEST_TRANSACTION_ID, TEST_TRANSACTION_ID_2)));
                allowing(participant2).recoverTransactions(TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY);
                will(returnValue(Arrays.asList(TEST_TRANSACTION_ID, TEST_TRANSACTION_ID_3)));
                allowing(participant3).recoverTransactions(TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY);
                will(returnValue(Collections.emptyList()));

                // recover transaction 1 (participant 1 and 2 were involved, they are both waiting for coordinator's decision)
                one(transactionLog).logTransaction(
                        with(logEntry(TEST_TRANSACTION_ID, TransactionStatus.COMMIT_STARTED, Set.of(TEST_PARTICIPANT_ID, TEST_PARTICIPANT_ID_2))));
                one(participant1).commitRecoveredTransaction(TEST_TRANSACTION_ID, TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY);
                will(throwException(exception));
                one(participant2).commitRecoveredTransaction(TEST_TRANSACTION_ID, TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY);

                // recover transaction 2 (participant 1 and 2 were involved, only participant 1 is waiting for coordinator's decision)
                one(transactionLog).logTransaction(
                        with(logEntry(TEST_TRANSACTION_ID_2, TransactionStatus.COMMIT_STARTED, Set.of(TEST_PARTICIPANT_ID, TEST_PARTICIPANT_ID_2))));
                one(participant1).commitRecoveredTransaction(TEST_TRANSACTION_ID_2, TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY);
                one(transactionLog).deleteTransaction(TEST_TRANSACTION_ID_2);

                // recover transaction 3 (participant 1 and 2 were involved)
                one(transactionLog).logTransaction(with(logEntry(TEST_TRANSACTION_ID_3, TransactionStatus.ROLLBACK_STARTED,
                        Set.of(TEST_PARTICIPANT_ID, TEST_PARTICIPANT_ID_2))));
                one(participant1).rollbackRecoveredTransaction(TEST_TRANSACTION_ID_3, TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY);
                one(participant2).rollbackRecoveredTransaction(TEST_TRANSACTION_ID_3, TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY);
                one(transactionLog).deleteTransaction(TEST_TRANSACTION_ID_3);

                // recover transaction 4 (participant 3 was involved)
                one(transactionLog).logTransaction(
                        with(logEntry(TEST_TRANSACTION_ID_4, TransactionStatus.ROLLBACK_STARTED, Set.of(TEST_PARTICIPANT_ID_3))));
                one(participant3).rollbackRecoveredTransaction(TEST_TRANSACTION_ID_4, TEST_INTERACTIVE_SESSION_KEY, TEST_TRANSACTION_COORDINATOR_KEY);
                one(transactionLog).deleteTransaction(TEST_TRANSACTION_ID_4);
            }
        });

        coordinator.recoverTransactionsFromTransactionLog();
        coordinator.finishFailedOrAbandonedTransactions();
    }

    @DataProvider(name = "testRecoverTransactionWithStatus")
    public Object[][] provideTestRecoverTransactionWithStatus()
    {
        List<Object[]> statuses = new ArrayList<>();
        Arrays.stream(TransactionStatus.values()).forEach(s ->
        {
            statuses.add(new Object[] { s, false });
            statuses.add(new Object[] { s, true });
        });
        return statuses.toArray(new Object[0][0]);
    }

    private Matcher<TransactionLogEntry> logEntry(UUID transactionId, TransactionStatus transactionStatus, Set<String> participantIds)
    {
        return new TransactionLogEntryMatcher(transactionId, transactionStatus, participantIds);
    }

}
