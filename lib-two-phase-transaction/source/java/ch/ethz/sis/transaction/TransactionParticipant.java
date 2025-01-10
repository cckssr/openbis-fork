package ch.ethz.sis.transaction;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.common.logging.LogCategory;
import ch.systemsx.cisd.common.logging.LogFactory;

public class TransactionParticipant extends AbstractTransactionNode<TransactionParticipant.Transaction> implements ITransactionParticipant
{

    private static final Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION, TransactionParticipant.class);

    private final String participantId;

    private final ITransactionOperationExecutor operationExecutor;

    private IDatabaseTransactionProvider databaseTransactionProvider;

    public TransactionParticipant(String participantId, String transactionCoordinatorKey, String interactiveSessionKey,
            ISessionTokenProvider sessionTokenProvider, IDatabaseTransactionProvider databaseTransactionProvider,
            ITransactionOperationExecutor operationExecutor, ITransactionLog transactionLog, int transactionTimeoutInSeconds,
            int transactionCountLimit)
    {
        super(transactionCoordinatorKey, interactiveSessionKey, sessionTokenProvider, transactionLog, transactionTimeoutInSeconds,
                transactionCountLimit);

        if (participantId == null)
        {
            throw new IllegalArgumentException("Participant id cannot be null");
        }

        if (databaseTransactionProvider == null)
        {
            throw new IllegalArgumentException("Database transaction provider cannot be null");
        }

        if (operationExecutor == null)
        {
            throw new IllegalArgumentException("Operation executor cannot be null");
        }

        this.participantId = participantId;
        this.databaseTransactionProvider = databaseTransactionProvider;
        this.operationExecutor = operationExecutor;
    }

    @Override public String getParticipantId()
    {
        return participantId;
    }

    @Override protected Transaction createTransactionFromLogEntry(final TransactionLogEntry logEntry)
    {
        Transaction transaction = new Transaction(logEntry.getTransactionId(), null);
        transaction.setTransactionStatus(logEntry.getTransactionStatus());
        transaction.setTwoPhaseTransaction(logEntry.isTwoPhaseTransaction());
        transaction.setLastAccessedDate(new Date(0));
        return transaction;
    }

    @Override protected TransactionLogEntry createLogEntryFromTransaction(final Transaction transaction)
    {
        TransactionLogEntry entry = new TransactionLogEntry();
        entry.setTransactionId(transaction.getTransactionId());
        entry.setTransactionStatus(transaction.getTransactionStatus());
        entry.setTwoPhaseTransaction(transaction.isTwoPhaseTransaction());
        entry.setLastAccessedDate(transaction.getLastAccessedDate());
        return entry;
    }

    @Override protected void finishFailedOrAbandonedTransactionViaCommit(final Transaction transaction) throws Exception
    {
        if (TransactionStatus.COMMIT_STARTED.equals(transaction.getTransactionStatus()))
        {
            commitTransaction(transaction);
        }
    }

    @Override protected void finishFailedOrAbandonedTransactionViaRollback(final Transaction transaction) throws Exception
    {
        rollbackTransaction(transaction);
    }

    @Override protected boolean isCoordinator()
    {
        return false;
    }

    @Override public void beginTransaction(final UUID transactionId, final String sessionToken, final String interactiveSessionKey,
            final String transactionCoordinatorKey)
    {
        try
        {
            checkTransactionId(transactionId);
            checkSessionToken(sessionToken);
            checkInteractiveSessionKey(interactiveSessionKey);

            if (transactionCoordinatorKey != null)
            {
                checkTransactionCoordinatorKey(transactionCoordinatorKey);
            }

            Transaction transaction = new Transaction(transactionId, sessionToken);
            transaction.setTwoPhaseTransaction(transactionCoordinatorKey != null);
            registerTransaction(transaction);

            transaction.executeWithLockOrFail(() ->
            {
                try
                {
                    operationLog.info("Begin transaction '" + transactionId + "' started.");

                    changeTransactionStatus(transaction, TransactionStatus.BEGIN_STARTED);

                    Object databaseTransaction = databaseTransactionProvider.beginTransaction(transactionId);
                    transaction.setDatabaseTransaction(databaseTransaction);

                    changeTransactionStatus(transaction, TransactionStatus.BEGIN_FINISHED);

                    operationLog.info("Begin transaction '" + transactionId + "' finished successfully.");

                    return null;
                } catch (Exception beginException)
                {
                    try
                    {
                        rollbackTransaction(transaction);
                    } catch (Exception rollbackException)
                    {
                        operationLog.warn("Transaction '" + transaction.getTransactionId() + "' rollback failed.", rollbackException);
                    }

                    throw beginException;
                }
            }, true);
        } catch (Exception e)
        {
            throw logException("Begin transaction '" + transactionId + "' failed.", e);
        }
    }

    @Override public <T> T executeOperation(final UUID transactionId, final String sessionToken, final String interactiveSessionKey,
            final String operationName, final Object[] operationArguments)
    {
        try
        {
            checkTransactionId(transactionId);
            checkSessionToken(sessionToken);
            checkInteractiveSessionKey(interactiveSessionKey);
            checkOperationName(operationName);
            checkOperationArguments(operationArguments);

            Transaction transaction = getTransaction(transactionId);

            if (transaction == null)
            {
                throw new UserFailureException("Transaction '" + transactionId + "' does not exist.");
            }

            return transaction.executeWithoutLock(() ->
            {
                checkTransactionAccess(transaction, sessionToken);
                checkTransactionStatus(transaction, TransactionStatus.BEGIN_FINISHED);

                operationLog.info("Transaction '" + transactionId + "' execute operation '" + operationName + "' started.");

                T result;

                try
                {

                    result = operationExecutor.executeOperation(sessionToken, operationName, operationArguments);
                } catch (Exception operationException)
                {
                    throw new TransactionOperationException(operationException.getMessage(), operationException);
                }

                operationLog.info("Transaction '" + transactionId + "' execute operation '" + operationName + "' finished successfully.");
                return result;

            }, true);
        } catch (Exception e)
        {
            throw logException("Transaction '" + transactionId + "' execute operation '" + operationName + "' failed.", e);
        }
    }

    @Override public void prepareTransaction(final UUID transactionId, final String sessionToken, final String interactiveSessionKey,
            final String transactionCoordinatorKey)
    {
        try
        {
            checkTransactionId(transactionId);
            checkSessionToken(sessionToken);
            checkInteractiveSessionKey(interactiveSessionKey);
            checkTransactionCoordinatorKey(transactionCoordinatorKey);

            Transaction transaction = getTransaction(transactionId);

            if (transaction == null)
            {
                throw new UserFailureException("Transaction '" + transactionId + "' does not exist.");
            }

            transaction.executeWithLockOrFail(() ->
            {
                checkTransactionAccess(transaction, sessionToken);
                checkTransactionStatus(transaction, TransactionStatus.BEGIN_FINISHED);

                if (!transaction.isTwoPhaseTransaction())
                {
                    throw new UserFailureException("Transaction '" + transactionId
                            + "' was started without transaction coordinator key, therefore calling prepare is not allowed.");
                }

                operationLog.info("Prepare transaction '" + transactionId + "' started.");

                changeTransactionStatus(transaction, TransactionStatus.PREPARE_STARTED);
                databaseTransactionProvider.prepareTransaction(transactionId, transaction.getDatabaseTransaction());
                changeTransactionStatus(transaction, TransactionStatus.PREPARE_FINISHED);

                operationLog.info("Prepare transaction '" + transactionId + "' finished successfully.");

                return null;
            }, true);
        } catch (Exception e)
        {
            throw logException("Prepare transaction '" + transactionId + "' failed.", e);
        }
    }

    @Override public List<UUID> recoverTransactions(final String interactiveSessionKey, final String transactionCoordinatorKey)
    {
        try
        {
            checkInteractiveSessionKey(interactiveSessionKey);
            checkTransactionCoordinatorKey(transactionCoordinatorKey);

            operationLog.info("Started recovering transactions (triggered by the coordinator)");

            recoverTransactionsFromTransactionLog();

            List<UUID> preparedTransactions = new ArrayList<>();

            for (Transaction transaction : transactionMap.values())
            {
                if (transaction.isTwoPhaseTransaction())
                {
                    if (TransactionStatus.PREPARE_FINISHED.equals(transaction.getTransactionStatus()))
                    {
                        preparedTransactions.add(transaction.getTransactionId());
                    } else if (TransactionStatus.COMMIT_STARTED.equals(transaction.getTransactionStatus()))
                    {
                        transaction.executeWithLockOrSkip(() -> preparedTransactions.add(transaction.getTransactionId()), false);
                    }
                }
            }

            operationLog.info("Finished recovering transactions (triggered by the coordinator)");

            return preparedTransactions;
        } catch (Exception e)
        {
            throw logException("Recover transactions failed.", e);
        }
    }

    @Override public void commitTransaction(final UUID transactionId, final String sessionToken, final String interactiveSessionKey)
    {
        try
        {
            checkTransactionId(transactionId);
            checkSessionToken(sessionToken);
            checkInteractiveSessionKey(interactiveSessionKey);

            Transaction transaction = getTransaction(transactionId);

            if (transaction == null)
            {
                throw new UserFailureException("Transaction '" + transactionId + "' does not exist.");
            }

            transaction.executeWithLockOrFail(() ->
            {
                checkTransactionAccess(transaction, sessionToken);
                commitTransaction(transaction);
                return null;
            }, true);
        } catch (Exception e)
        {
            throw logException("Commit transaction '" + transactionId + "' failed.", e);
        }
    }

    @Override public void commitRecoveredTransaction(final UUID transactionId, final String interactiveSessionKey,
            final String transactionCoordinatorKey)
    {
        try
        {
            checkTransactionId(transactionId);
            checkInteractiveSessionKey(interactiveSessionKey);
            checkTransactionCoordinatorKey(transactionCoordinatorKey);

            Transaction transaction = getTransaction(transactionId);

            if (transaction == null)
            {
                throw new UserFailureException("Transaction '" + transactionId + "' does not exist.");
            }

            transaction.executeWithLockOrWait(() ->
            {
                commitTransaction(transaction);
                return null;
            }, transactionTimeoutInSeconds, true);
        } catch (Exception e)
        {
            throw logException("Commit transaction '" + transactionId + "' failed.", e);
        }
    }

    private void commitTransaction(Transaction transaction) throws Exception
    {
        if (transaction.isTwoPhaseTransaction())
        {
            checkTransactionStatus(transaction, TransactionStatus.PREPARE_FINISHED, TransactionStatus.COMMIT_STARTED);
        } else
        {
            checkTransactionStatus(transaction, TransactionStatus.BEGIN_FINISHED);
        }

        operationLog.info("Commit transaction '" + transaction.getTransactionId() + "' started.");

        changeTransactionStatus(transaction, TransactionStatus.COMMIT_STARTED);

        try
        {
            databaseTransactionProvider.commitTransaction(transaction.getTransactionId(), transaction.getDatabaseTransaction(),
                    transaction.isTwoPhaseTransaction());
        } catch (Exception commitException)
        {
            if (!transaction.isTwoPhaseTransaction())
            {
                try
                {
                    rollbackTransaction(transaction);
                } catch (Exception rollbackException)
                {
                    operationLog.warn("Transaction '" + transaction.getTransactionId() + "' rollback failed.", rollbackException);
                }
            }

            throw commitException;
        }

        changeTransactionStatus(transaction, TransactionStatus.COMMIT_FINISHED);

        transaction.close();
        transactionMap.remove(transaction.getTransactionId());

        operationLog.info("Commit transaction '" + transaction.getTransactionId() + "' finished successfully.");
    }

    @Override public void rollbackTransaction(final UUID transactionId, final String sessionToken, final String interactiveSessionKey)
    {
        try
        {
            checkTransactionId(transactionId);
            checkSessionToken(sessionToken);
            checkInteractiveSessionKey(interactiveSessionKey);

            Transaction transaction = getTransaction(transactionId);

            if (transaction == null)
            {
                return;
            }

            transaction.executeWithLockOrFail(() ->
            {
                checkTransactionAccess(transaction, sessionToken);
                rollbackTransaction(transaction);
                return null;
            }, true);
        } catch (Exception e)
        {
            throw logException("Rollback transaction '" + transactionId + "' failed.", e);
        }
    }

    @Override public void rollbackRecoveredTransaction(final UUID transactionId, final String interactiveSessionKey,
            final String transactionCoordinatorKey)
    {
        try
        {
            checkTransactionId(transactionId);
            checkInteractiveSessionKey(interactiveSessionKey);
            checkTransactionCoordinatorKey(transactionCoordinatorKey);

            Transaction transaction = getTransaction(transactionId);

            if (transaction == null)
            {
                return;
            }

            transaction.executeWithLockOrWait(() ->
            {
                rollbackTransaction(transaction);
                return null;
            }, transactionTimeoutInSeconds, true);
        } catch (Exception e)
        {
            throw logException("Rollback transaction '" + transactionId + "' failed.", e);
        }
    }

    private void rollbackTransaction(Transaction transaction) throws Exception
    {
        if (TransactionStatus.ROLLBACK_FINISHED.equals(transaction.getTransactionStatus()))
        {
            operationLog.info("Transaction '" + transaction.getTransactionId() + "' has been already rolled back before.");
            return;
        }

        if (transaction.isTwoPhaseTransaction())
        {
            checkTransactionStatus(transaction, TransactionStatus.NEW, TransactionStatus.BEGIN_STARTED, TransactionStatus.BEGIN_FINISHED,
                    TransactionStatus.PREPARE_STARTED, TransactionStatus.PREPARE_FINISHED, TransactionStatus.ROLLBACK_STARTED);
        } else
        {
            checkTransactionStatus(transaction, TransactionStatus.NEW, TransactionStatus.BEGIN_STARTED,
                    TransactionStatus.BEGIN_FINISHED, TransactionStatus.COMMIT_STARTED, TransactionStatus.ROLLBACK_STARTED);
        }

        operationLog.info("Rollback transaction '" + transaction.getTransactionId() + "' started.");

        changeTransactionStatus(transaction, TransactionStatus.ROLLBACK_STARTED);

        if (transaction.getDatabaseTransaction() != null)
        {
            databaseTransactionProvider.rollbackTransaction(transaction.getTransactionId(), transaction.getDatabaseTransaction(),
                    transaction.isTwoPhaseTransaction());
        }

        changeTransactionStatus(transaction, TransactionStatus.ROLLBACK_FINISHED);

        transaction.close();
        transactionMap.remove(transaction.getTransactionId());

        operationLog.info("Rollback transaction '" + transaction.getTransactionId() + "' finished successfully.");
    }

    public static class Transaction extends AbstractTransaction
    {

        private final ExecutorService executor =
                Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "transaction-" + getTransactionId()));

        private Object databaseTransaction;

        private boolean isTwoPhaseTransaction;

        public Transaction(final UUID transactionId, final String sessionToken)
        {
            super(transactionId, sessionToken);
        }

        @Override protected <T> T executeAction(final Callable<T> action) throws Exception
        {
            if (executor.isShutdown())
            {
                operationLog.info("Cannot execute a new action on transaction '" + getTransactionId() + "' as it has been already closed.");
                return null;
            }

            try
            {
                Future<T> future = executor.submit(action);
                return future.get();
            } catch (ExecutionException e)
            {
                Throwable originalException = e.getCause();
                if (originalException instanceof Exception)
                {
                    throw (Exception) originalException;
                } else
                {
                    throw new RuntimeException(originalException);
                }
            }
        }

        public Object getDatabaseTransaction()
        {
            return databaseTransaction;
        }

        public void setDatabaseTransaction(final Object databaseTransaction)
        {
            this.databaseTransaction = databaseTransaction;
        }

        public boolean isTwoPhaseTransaction()
        {
            return isTwoPhaseTransaction;
        }

        public void setTwoPhaseTransaction(final boolean twoPhaseTransaction)
        {
            isTwoPhaseTransaction = twoPhaseTransaction;
        }

        public void close()
        {
            try
            {
                executor.shutdown();
            } catch (Exception e)
            {
                operationLog.warn("Transaction '" + getTransactionId() + "' close failed.", e);
            }
        }
    }

    public IDatabaseTransactionProvider getDatabaseTransactionProvider()
    {
        return databaseTransactionProvider;
    }

    public void setDatabaseTransactionProvider(final IDatabaseTransactionProvider databaseTransactionProvider)
    {
        this.databaseTransactionProvider = databaseTransactionProvider;
    }

}
