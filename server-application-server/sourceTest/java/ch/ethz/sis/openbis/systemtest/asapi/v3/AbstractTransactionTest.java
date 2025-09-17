package ch.ethz.sis.openbis.systemtest.asapi.v3;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import ch.ethz.sis.shared.log.classic.impl.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.ICodeHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IIdentifierHolder;
import ch.ethz.sis.openbis.generic.server.asapi.v3.TransactionConfiguration;
import ch.ethz.sis.openbis.generic.server.asapi.v3.TransactionCoordinatorApi;
import ch.ethz.sis.openbis.generic.server.asapi.v3.TransactionParticipantApi;
import ch.ethz.sis.transaction.AbstractTransaction;
import ch.ethz.sis.transaction.IDatabaseTransactionProvider;
import ch.ethz.sis.transaction.ISessionTokenProvider;
import ch.ethz.sis.transaction.ITransactionLog;
import ch.ethz.sis.transaction.ITransactionOperationExecutor;
import ch.ethz.sis.transaction.TransactionCoordinator;
import ch.ethz.sis.transaction.TransactionParticipant;
import ch.ethz.sis.transaction.TransactionStatus;
import ch.ethz.sis.transaction.api.ITransactionParticipant;
import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.systemsx.cisd.dbmigration.DatabaseConfigurationContext;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.IDAOFactory;
import ch.systemsx.cisd.openbis.generic.shared.IOpenBisSessionManager;

public class AbstractTransactionTest extends AbstractTest
{

    private static final Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION, AbstractTransactionTest.class);

    public static final String TEST_COORDINATOR_KEY = "system-test-transaction-coordinator-key";

    public static final String TEST_INTERACTIVE_SESSION_KEY = "system-test-interactive-session-key";

    public static final String TEST_PARTICIPANT_1_ID = "system-test-participant-1";

    public static final String TEST_PARTICIPANT_2_ID = "system-test-participant-2";

    public static final String TRANSACTION_LOG_ROOT_FOLDER = "targets/system-test-transaction-logs";

    public static final String TRANSACTION_LOG_COORDINATOR_FOLDER = "system-test-coordinator";

    public static final String TRANSACTION_LOG_PARTICIPANT_1_FOLDER = "system-test-participant-1";

    public static final String TRANSACTION_LOG_PARTICIPANT_2_FOLDER = "system-test-participant-2";

    public static final String OPERATION_CREATE_SPACES = "createSpaces";

    public static final String OPERATION_CREATE_PROJECTS = "createProjects";

    public static final String OPERATION_SEARCH_SPACES = "searchSpaces";

    public static final String OPERATION_SEARCH_PROJECTS = "searchProjects";

    public static final String CODE_PREFIX = "TRANSACTION_TEST_";

    @Autowired
    public PlatformTransactionManager transactionManager;

    @Autowired
    public IDAOFactory daoFactory;

    @Autowired
    public DatabaseConfigurationContext databaseContext;

    @Autowired
    public IOpenBisSessionManager sessionManager;

    public TestTransactionCoordinatorApi createCoordinator(TransactionConfiguration configuration, List<ITransactionParticipant> participants,
            String logFolderName)
    {
        TestTransactionCoordinatorApi coordinatorApi = new TestTransactionCoordinatorApi(configuration, participants, logFolderName);
        coordinatorApi.init();
        return coordinatorApi;
    }

    public TestTransactionParticipantApi createParticipant(TransactionConfiguration configuration, String participantId, String logFolderName)
    {
        TestTransactionParticipantApi participantApi = new TestTransactionParticipantApi(configuration, participantId, logFolderName);
        participantApi.init();
        return participantApi;
    }

    public TransactionConfiguration createConfiguration(final boolean enabled, final int transactionTimeoutInSeconds, final int transactionCountLimit)
    {
        return new TransactionConfiguration()
        {
            @Override public boolean isEnabled()
            {
                return enabled;
            }

            public int getTransactionTimeoutInSeconds()
            {
                return transactionTimeoutInSeconds;
            }

            public int getFinishTransactionsIntervalInSeconds()
            {
                return 3600;
            }

            public String getInteractiveSessionKey()
            {
                return TEST_INTERACTIVE_SESSION_KEY;
            }

            public String getCoordinatorKey()
            {
                return TEST_COORDINATOR_KEY;
            }

            public String getTransactionLogFolderPath()
            {
                return TRANSACTION_LOG_ROOT_FOLDER;
            }

            public int getTransactionCountLimit()
            {
                return transactionCountLimit;
            }

            public String getApplicationServerUrl()
            {
                return null;
            }

            public int getApplicationServerTimeoutInSeconds()
            {
                return -1;
            }

            public String getAfsServerUrl()
            {
                return null;
            }

            public int getAfsServerTimeoutInSeconds()
            {
                return -1;
            }
        };
    }

    public void rollbackPreparedDatabaseTransactions() throws Exception
    {
        try (Connection connection = databaseContext.getDataSource().getConnection(); Statement statement = connection.createStatement())
        {
            List<String> preparedTransactionIds = new ArrayList<>();

            ResultSet preparedTransactions = statement.executeQuery("SELECT gid FROM pg_prepared_xacts");
            while (preparedTransactions.next())
            {
                preparedTransactionIds.add(preparedTransactions.getString(1));
            }

            for (String preparedTransactionId : preparedTransactionIds)
            {
                statement.execute("ROLLBACK PREPARED '" + preparedTransactionId + "'");
            }
        }
    }

    public void deleteCreatedSpacesAndProjects() throws Exception
    {
        try (Connection connection = databaseContext.getDataSource().getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("DELETE FROM projects WHERE code LIKE '" + CODE_PREFIX + "%'");
            statement.execute("DELETE FROM spaces WHERE code LIKE '" + CODE_PREFIX + "%'");
        }
    }

    public class TestTransactionCoordinatorApi extends TransactionCoordinatorApi
    {

        private final List<ITransactionParticipant> testParticipants;

        public TestTransactionCoordinatorApi(final TransactionConfiguration transactionConfiguration,
                final List<ITransactionParticipant> participants, final String logFolderName)
        {
            super(transactionConfiguration, v3api, sessionManager, logFolderName);
            this.testParticipants = participants;
        }

        @Override protected TransactionCoordinator createCoordinator(final String transactionCoordinatorKey,
                final String interactiveSessionKey, final ISessionTokenProvider sessionTokenProvider,
                final List<ITransactionParticipant> participants, final ITransactionLog transactionLog, final int transactionTimeoutInSeconds,
                final int transactionCountLimit)
        {
            return new TransactionCoordinator(transactionCoordinatorKey,
                    interactiveSessionKey, sessionTokenProvider, this.testParticipants,
                    transactionLog, transactionTimeoutInSeconds,
                    transactionCountLimit);
        }

        public void close()
        {
            String sessionToken = v3api.loginAsSystem();

            for (TransactionCoordinator.Transaction transaction : getTransactionMap().values())
            {
                try
                {
                    if (TransactionStatus.COMMIT_STARTED.equals(transaction.getTransactionStatus()))
                    {
                        commitTransaction(transaction.getTransactionId(), sessionToken, TEST_INTERACTIVE_SESSION_KEY);
                    } else
                    {
                        rollbackTransaction(transaction.getTransactionId(), sessionToken, TEST_INTERACTIVE_SESSION_KEY);
                    }
                } catch (Exception e)
                {
                    operationLog.warn("Could not close transaction '" + transaction.getTransactionId() + "'.", e);
                }
            }
        }

    }

    public class TestTransactionParticipantApi extends TransactionParticipantApi
    {

        // Map the original transaction id coming from the coordinator to a unique transaction id for each participant,
        // this way we can have multiple participants preparing the transaction on the same test database.

        private final Map<UUID, UUID> originalToInternalId = new HashMap<>();

        private final Map<UUID, UUID> internalToOriginalId = new HashMap<>();

        private boolean mapTransactions = false;

        private TestDatabaseTransactionProvider testDatabaseTransactionProvider;

        public TestTransactionParticipantApi(final TransactionConfiguration transactionConfiguration, final String participantId,
                final String logFolderName)
        {
            super(transactionConfiguration, transactionManager, daoFactory, databaseContext, v3api, sessionManager, participantId, logFolderName);
        }

        @Override protected TransactionParticipant createParticipant(final String participantId, final String transactionCoordinatorKey,
                final String interactiveSessionKey, final ISessionTokenProvider sessionTokenProvider,
                final IDatabaseTransactionProvider databaseTransactionProvider, final ITransactionOperationExecutor operationExecutor,
                final ITransactionLog transactionLog, final int transactionTimeoutInSeconds, final int transactionCountLimit)
        {
            this.testDatabaseTransactionProvider = new TestDatabaseTransactionProvider(databaseTransactionProvider);
            return new TransactionParticipant(
                    participantId,
                    transactionCoordinatorKey,
                    interactiveSessionKey,
                    sessionTokenProvider,
                    this.testDatabaseTransactionProvider,
                    operationExecutor,
                    transactionLog,
                    transactionTimeoutInSeconds,
                    transactionCountLimit
            );
        }

        public TestDatabaseTransactionProvider getDatabaseTransactionProvider()
        {
            return this.testDatabaseTransactionProvider;
        }

        @Override public void beginTransaction(final UUID transactionId, final String sessionToken, final String interactiveSessionKey,
                final String transactionCoordinatorKey)
        {
            super.beginTransaction(mapOriginalToInternalId(transactionId), sessionToken, interactiveSessionKey,
                    transactionCoordinatorKey);
        }

        @Override public <T> T executeOperation(final UUID transactionId, final String sessionToken, final String interactiveSessionKey,
                final String operationName, final Object[] operationArguments)
        {
            return super.executeOperation(mapOriginalToInternalId(transactionId), sessionToken, interactiveSessionKey, operationName,
                    operationArguments);
        }

        @Override public void prepareTransaction(final UUID transactionId, final String sessionToken, final String interactiveSessionKey,
                final String transactionCoordinatorKey)
        {
            super.prepareTransaction(mapOriginalToInternalId(transactionId), sessionToken, interactiveSessionKey, transactionCoordinatorKey);
        }

        @Override public void commitTransaction(final UUID transactionId, final String sessionToken, final String interactiveSessionKey)
        {
            super.commitTransaction(mapOriginalToInternalId(transactionId), sessionToken, interactiveSessionKey);
        }

        @Override public void commitRecoveredTransaction(final UUID transactionId, final String interactiveSessionKey,
                final String transactionCoordinatorKey)
        {
            super.commitRecoveredTransaction(mapOriginalToInternalId(transactionId), interactiveSessionKey, transactionCoordinatorKey);
        }

        @Override public void rollbackTransaction(final UUID transactionId, final String sessionToken, final String interactiveSessionKey)
        {
            super.rollbackTransaction(mapOriginalToInternalId(transactionId), sessionToken, interactiveSessionKey);
        }

        @Override public void rollbackRecoveredTransaction(final UUID transactionId, final String interactiveSessionKey,
                final String transactionCoordinatorKey)
        {
            super.rollbackRecoveredTransaction(mapOriginalToInternalId(transactionId), interactiveSessionKey, transactionCoordinatorKey);
        }

        @Override public List<UUID> recoverTransactions(final String interactiveSessionKey, final String transactionCoordinatorKey)
        {
            List<UUID> transactionIds = new ArrayList<>();

            for (UUID internalTransactionId : super.recoverTransactions(interactiveSessionKey, transactionCoordinatorKey))
            {
                transactionIds.add(mapInternalToOriginalId(internalTransactionId));
            }

            return transactionIds;
        }

        public void recoverTransactionsFromTransactionLog()
        {
            super.recoverTransactionsFromTransactionLog();
        }

        public void finishFailedOrAbandonedTransactions()
        {
            super.finishFailedOrAbandonedTransactions();
        }

        public Map<UUID, TransactionParticipant.Transaction> getTransactionMap()
        {
            Map<UUID, TransactionParticipant.Transaction> transactionMap = new HashMap<>();

            for (Map.Entry<UUID, ? extends AbstractTransaction> entry : super.getTransactionMap().entrySet())
            {
                TransactionParticipant.Transaction internalTransaction = (TransactionParticipant.Transaction) entry.getValue();
                TransactionParticipant.Transaction originalTransaction =
                        new TransactionParticipant.Transaction(mapInternalToOriginalId(internalTransaction.getTransactionId()),
                                internalTransaction.getSessionToken());
                originalTransaction.setTransactionStatus(internalTransaction.getTransactionStatus());
                originalTransaction.setLastAccessedDate(internalTransaction.getLastAccessedDate());
                transactionMap.put(originalTransaction.getTransactionId(), originalTransaction);
            }

            return transactionMap;
        }

        public void setTestTransactionMapping(Map<UUID, UUID> coordinatorIdToParticipantIdMap)
        {
            for (Map.Entry<UUID, UUID> entry : coordinatorIdToParticipantIdMap.entrySet())
            {
                originalToInternalId.put(entry.getKey(), entry.getValue());
                internalToOriginalId.put(entry.getValue(), entry.getKey());
            }

            mapTransactions = true;
        }

        public Map<UUID, UUID> getTestTransactionMapping()
        {
            return originalToInternalId;
        }

        private UUID mapInternalToOriginalId(UUID internalId)
        {
            if (mapTransactions)
            {
                return internalToOriginalId.get(internalId);
            } else
            {
                return internalId;
            }
        }

        private UUID mapOriginalToInternalId(UUID originalId)
        {
            if (mapTransactions)
            {
                return originalToInternalId.get(originalId);
            } else
            {
                return originalId;
            }
        }

        public void close()
        {
            String sessionToken = v3api.loginAsSystem();

            getDatabaseTransactionProvider().setRollbackAction(null);
            getDatabaseTransactionProvider().setCommitAction(null);

            for (TransactionParticipant.Transaction transaction : getTransactionMap().values())
            {
                try
                {
                    if (TransactionStatus.COMMIT_STARTED.equals(transaction.getTransactionStatus()))
                    {
                        commitTransaction(transaction.getTransactionId(), sessionToken, TEST_INTERACTIVE_SESSION_KEY);
                    } else
                    {
                        rollbackTransaction(transaction.getTransactionId(), sessionToken, TEST_INTERACTIVE_SESSION_KEY);
                    }
                } catch (Exception e)
                {
                    operationLog.warn("Could not close transaction '" + transaction.getTransactionId() + "'.", e);
                }
            }
        }
    }

    public static class TestDatabaseTransactionProvider implements IDatabaseTransactionProvider
    {

        private final IDatabaseTransactionProvider databaseTransactionProvider;

        private Runnable beginAction;

        private Runnable prepareAction;

        private Runnable commitAction;

        private Runnable rollbackAction;

        public TestDatabaseTransactionProvider(IDatabaseTransactionProvider databaseTransactionProvider)
        {
            this.databaseTransactionProvider = databaseTransactionProvider;
        }

        @Override public Object beginTransaction(final UUID transactionId) throws Exception
        {
            if (beginAction != null)
            {
                beginAction.run();
            }
            return databaseTransactionProvider.beginTransaction(transactionId);
        }

        @Override public void prepareTransaction(final UUID transactionId, final Object transaction) throws Exception
        {
            if (prepareAction != null)
            {
                prepareAction.run();
            }
            databaseTransactionProvider.prepareTransaction(transactionId, transaction);
        }

        @Override public void rollbackTransaction(final UUID transactionId, final Object transaction, final boolean isTwoPhaseTransaction)
                throws Exception
        {
            if (rollbackAction != null)
            {
                rollbackAction.run();
            }
            databaseTransactionProvider.rollbackTransaction(transactionId, transaction, isTwoPhaseTransaction);
        }

        @Override public void commitTransaction(final UUID transactionId, final Object transaction, final boolean isTwoPhaseTransaction)
                throws Exception
        {
            if (commitAction != null)
            {
                commitAction.run();
            }
            databaseTransactionProvider.commitTransaction(transactionId, transaction, isTwoPhaseTransaction);
        }

        public void setBeginAction(final Runnable beginAction)
        {
            this.beginAction = beginAction;
        }

        public void setPrepareAction(final Runnable prepareAction)
        {
            this.prepareAction = prepareAction;
        }

        public void setCommitAction(final Runnable commitAction)
        {
            this.commitAction = commitAction;
        }

        public void setRollbackAction(final Runnable rollbackAction)
        {
            this.rollbackAction = rollbackAction;
        }

    }

    public static void assertTransactionsDisabled(Runnable action)
    {
        try
        {
            action.run();
            fail();
        } catch (Exception e)
        {
            assertEquals(e.getMessage(), "Transactions are disabled in service.properties file.");
        }
    }

    public static Set<String> codes(Collection<? extends ICodeHolder> objectsWithCodes)
    {
        return objectsWithCodes.stream().map(ICodeHolder::getCode).collect(Collectors.toSet());
    }

    public static Set<String> identifiers(Collection<? extends IIdentifierHolder> objectsWithIdentifiers)
    {
        return objectsWithIdentifiers.stream().map(o -> o.getIdentifier().getIdentifier()).collect(Collectors.toSet());
    }

    public static <T> Set<T> difference(Set<T> s1, Set<T> s2)
    {
        Set<T> temp = new HashSet<>(s1);
        temp.removeAll(s2);
        return temp;
    }

}
