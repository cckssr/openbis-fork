package ch.ethz.sis.transaction;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.common.logging.LogCategory;
import ch.systemsx.cisd.common.logging.LogFactory;
import ch.systemsx.cisd.common.time.DateTimeUtils;

public abstract class AbstractTransactionNode<T extends AbstractTransaction>
{

    protected final Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION, getClass());

    protected final String transactionCoordinatorKey;

    protected final String interactiveSessionKey;

    protected final ISessionTokenProvider sessionTokenProvider;

    protected final Map<UUID, T> transactionMap = new ConcurrentHashMap<>();

    protected final ITransactionLog transactionLog;

    protected final int transactionTimeoutInSeconds;

    protected final int transactionCountLimit;

    public AbstractTransactionNode(final String transactionCoordinatorKey, final String interactiveSessionKey,
            final ISessionTokenProvider sessionTokenProvider, final ITransactionLog transactionLog,
            int transactionTimeoutInSeconds, int transactionCountLimit)
    {
        if (transactionCoordinatorKey == null)
        {
            throw new IllegalArgumentException("Transaction coordinator key cannot be null");
        }

        if (interactiveSessionKey == null)
        {
            throw new IllegalArgumentException("Interactive session key cannot be null");
        }

        if (sessionTokenProvider == null)
        {
            throw new IllegalArgumentException("Session token provider cannot be null");
        }

        if (transactionLog == null)
        {
            throw new IllegalArgumentException("Transaction log cannot be null");
        }

        if (transactionTimeoutInSeconds <= 0)
        {
            throw new IllegalArgumentException("Transaction timeout cannot be <= 0");
        }

        if (transactionCountLimit <= 0)
        {
            throw new IllegalArgumentException("Transaction count cannot be <= 0");
        }

        this.transactionCoordinatorKey = transactionCoordinatorKey;
        this.interactiveSessionKey = interactiveSessionKey;
        this.sessionTokenProvider = sessionTokenProvider;
        this.transactionLog = transactionLog;
        this.transactionTimeoutInSeconds = transactionTimeoutInSeconds;
        this.transactionCountLimit = transactionCountLimit;
    }

    protected abstract T createTransactionFromLogEntry(TransactionLogEntry logEntry);

    protected abstract TransactionLogEntry createLogEntryFromTransaction(T transaction);

    protected abstract void finishFailedOrAbandonedTransactionViaCommit(final T transaction) throws Exception;

    protected abstract void finishFailedOrAbandonedTransactionViaRollback(final T transaction) throws Exception;

    protected abstract boolean isCoordinator();

    public void recoverTransactionsFromTransactionLog()
    {
        try
        {
            operationLog.info("Started recovering transactions from transaction log");

            for (TransactionLogEntry logEntry : transactionLog.getTransactions().values())
            {
                if (!logEntry.isTwoPhaseTransaction())
                {
                    operationLog.info(
                            "Nothing to recover for one-phase transaction '" + logEntry.getTransactionId()
                                    + "' found in the transaction log with last status '"
                                    + logEntry.getTransactionStatus() + "'.");
                    transactionLog.deleteTransaction(logEntry.getTransactionId());
                } else if (TransactionStatus.NEW.equals(logEntry.getTransactionStatus())
                        || TransactionStatus.COMMIT_FINISHED.equals(logEntry.getTransactionStatus())
                        || TransactionStatus.ROLLBACK_FINISHED.equals(logEntry.getTransactionStatus()))
                {
                    operationLog.info(
                            "Nothing to recover for two-phase transaction '" + logEntry.getTransactionId()
                                    + "' found in the transaction log with last status '"
                                    + logEntry.getTransactionStatus() + "'.");
                    transactionLog.deleteTransaction(logEntry.getTransactionId());
                } else
                {
                    synchronized (transactionMap)
                    {
                        T existingTransaction = getTransaction(logEntry.getTransactionId());

                        if (existingTransaction == null)
                        {
                            T transaction = createTransactionFromLogEntry(logEntry);
                            registerTransaction(transaction);
                            operationLog.info(
                                    "Recovered transaction '" + transaction.getTransactionId() + "' found in the transaction log with last status '"
                                            + transaction.getTransactionStatus() + "'.");
                        }
                    }
                }
            }

            operationLog.info("Finished recovering transactions from transaction log");
        } catch (Exception e)
        {
            operationLog.error("Recovering transactions from transaction log has failed.", e);
            throw e;
        }
    }

    public void finishFailedOrAbandonedTransactions()
    {
        operationLog.info("Started processing of failed or abandoned transactions");

        for (T transaction : transactionMap.values())
        {
            try
            {
                operationLog.info(
                        "Checking transaction '" + transaction.getTransactionId() + "' with last status '"
                                + transaction.getTransactionStatus() + "'.");

                switch (transaction.getTransactionStatus())
                {
                    case BEGIN_STARTED:
                    case PREPARE_STARTED:
                    case ROLLBACK_STARTED:
                        /*
                          If we are able to lock the transaction with the last state XXX_STARTED,
                          then XXX operation either failed in the middle or was unable to log XXX_FINISHED
                          state at the end. We can roll back the transaction without waiting for timeout.
                        */
                        transaction.executeWithLockOrSkip(() ->
                        {
                            finishFailedOrAbandonedTransactionViaRollback(transaction);
                            return null;
                        }, false);
                        break;
                    case NEW:
                        /*
                          If we are able to lock the transaction with the last state NEW then
                          either we have just created a new transaction and didn't lock it yet
                          or the transaction was unable to log BEGIN_STARTED status and failed.
                          To handle both cases correctly we should roll back after a timeout.
                        */
                    case BEGIN_FINISHED:
                        /*
                          The transaction in BEGIN_FINISHED state should be receiving operation executions.
                          If the operations are not coming then after a timeout we need to roll back.
                        */

                        boolean hasTimedOut =
                                System.currentTimeMillis() - transaction.getLastAccessedDate().getTime() > transactionTimeoutInSeconds * 1000L;

                        if (hasTimedOut)
                        {
                            operationLog.info("Transaction '" + transaction.getTransactionId() + "' has timed out. It was last accessed at '"
                                    + transaction.getLastAccessedDate() + "'.");
                            transaction.executeWithLockOrSkip(() ->
                            {
                                finishFailedOrAbandonedTransactionViaRollback(transaction);
                                return null;
                            }, false);
                        } else
                        {
                            long timeTillTimeoutInMillis = Math.max(0,
                                    transaction.getLastAccessedDate().getTime() + transactionTimeoutInSeconds * 1000L
                                            - System.currentTimeMillis());
                            operationLog.info(
                                    "Transaction '" + transaction.getTransactionId() + "' hasn't timed out yet. It was last accessed at '"
                                            + transaction.getLastAccessedDate() + "'. It will timeout in '" + DateTimeUtils.renderDuration(
                                            timeTillTimeoutInMillis) + "'.");
                        }
                        break;
                    case PREPARE_FINISHED:
                        /*
                            PREPARE_FINISHED state at the coordinator node is an unfinished/failed commit which should be repeated.
                            At a participant node PREPARED_FINISHED state means it is an unfinished transaction which hasn't been yet
                            committed or rolled back (it is waiting for the coordinator's decision).
                         */

                        if (isCoordinator())
                        {
                            transaction.executeWithLockOrSkip(() ->
                            {
                                finishFailedOrAbandonedTransactionViaCommit(transaction);
                                return null;
                            }, false);
                        }
                        break;
                    case COMMIT_STARTED:
                        transaction.executeWithLockOrSkip(() ->
                        {
                            finishFailedOrAbandonedTransactionViaCommit(transaction);
                            return null;
                        }, false);
                        break;
                }
            } catch (Exception e)
            {
                operationLog.warn(
                        "Finishing failed or abandoned transaction '" + transaction.getTransactionId() + "' with last status '"
                                + transaction.getTransactionStatus() + "' has failed.", e);
            }
        }

        operationLog.info("Finished processing of failed or abandoned transactions");
    }

    protected void checkTransactionId(final UUID transactionId)
    {
        if (transactionId == null)
        {
            throw new UserFailureException("Transaction id cannot be null");
        }
    }

    protected void checkSessionToken(final String sessionToken)
    {
        if (sessionToken == null)
        {
            throw new UserFailureException("Session token cannot be null");
        }

        if (!sessionTokenProvider.isValid(sessionToken))
        {
            throw new UserFailureException("Invalid session token");
        }
    }

    protected void checkTransactionCoordinatorKey(final String transactionCoordinatorKey)
    {
        if (transactionCoordinatorKey == null)
        {
            throw new UserFailureException("Transaction coordinator key cannot be null");
        }

        if (!this.transactionCoordinatorKey.equals(transactionCoordinatorKey))
        {
            throw new UserFailureException("Invalid transaction coordinator key");
        }
    }

    protected void checkInteractiveSessionKey(final String interactiveSessionKey)
    {
        if (interactiveSessionKey == null)
        {
            throw new UserFailureException("Interactive session key cannot be null");
        }

        if (!this.interactiveSessionKey.equals(interactiveSessionKey))
        {
            throw new UserFailureException("Invalid interactive session key");
        }
    }

    protected void checkTransactionStatus(final T transaction, final TransactionStatus... expectedStatuses)
    {
        for (final TransactionStatus expectedStatus : expectedStatuses)
        {
            if (transaction.getTransactionStatus() == expectedStatus)
            {
                return;
            }
        }

        throw new UserFailureException(
                "Transaction '" + transaction.getTransactionId() + "' unexpected status '" + transaction.getTransactionStatus()
                        + "'. Expected statuses '"
                        + Arrays.toString(expectedStatuses) + "'.");
    }

    protected void checkTransactionAccess(final T transaction, final String sessionToken)
    {
        if (sessionTokenProvider.isInstanceAdminOrSystem(sessionToken))
        {
            return;
        }

        if (!Objects.equals(transaction.getSessionToken(), sessionToken))
        {
            throw new UserFailureException("Access denied to transaction '" + transaction.getTransactionId() + "'");
        }
    }

    protected void checkOperationName(final String operationName)
    {
        if (operationName == null)
        {
            throw new UserFailureException("Operation name cannot be null");
        }
    }

    protected void checkOperationArguments(final Object[] operationArguments)
    {
        if (operationArguments == null)
        {
            throw new UserFailureException("Operation arguments cannot be null");
        }
    }

    protected void registerTransaction(T newTransaction)
    {
        synchronized (transactionMap)
        {
            T existingTransaction = transactionMap.get(newTransaction.getTransactionId());

            if (existingTransaction == null)
            {
                if (transactionMap.size() < transactionCountLimit)
                {
                    if (newTransaction.getSessionToken() != null)
                    {
                        for (T transaction : transactionMap.values())
                        {
                            if (newTransaction.getSessionToken().equals(transaction.getSessionToken()))
                            {
                                throw new UserFailureException(
                                        "Cannot create more than one transaction for the same session token. Transaction that could not be created: '"
                                                + newTransaction.getTransactionId() + "'. The already existing and still active transaction: '"
                                                + transaction.getTransactionId() + "'.");
                            }
                        }
                    }

                    transactionMap.put(newTransaction.getTransactionId(), newTransaction);
                } else
                {
                    throw new UserFailureException(
                            "Cannot create transaction '" + newTransaction.getTransactionId()
                                    + "' because the transaction count limit has been reached. Number of existing transactions: "
                                    + transactionMap.size());
                }
            } else
            {
                throw new UserFailureException("Transaction '" + newTransaction.getTransactionId() + "' already exists.");
            }
        }
    }

    protected void changeTransactionStatus(final T transaction, final TransactionStatus newTransactionStatus)
    {
        if (TransactionStatus.COMMIT_FINISHED.equals(newTransactionStatus) || TransactionStatus.ROLLBACK_FINISHED.equals(newTransactionStatus))
        {
            transactionLog.deleteTransaction(transaction.getTransactionId());
            transactionMap.remove(transaction.getTransactionId());
            transaction.setTransactionStatus(newTransactionStatus);
        } else
        {
            TransactionStatus oldTransactionStatus = transaction.getTransactionStatus();
            transaction.setTransactionStatus(newTransactionStatus);
            TransactionLogEntry entry = createLogEntryFromTransaction(transaction);
            try
            {
                transactionLog.logTransaction(entry);
            } catch (Exception e)
            {
                transaction.setTransactionStatus(oldTransactionStatus);
                throw e;
            }
        }
    }

    protected T getTransaction(UUID transactionId)
    {
        synchronized (transactionMap)
        {
            return transactionMap.get(transactionId);
        }
    }

    public Map<UUID, T> getTransactionMap()
    {
        return transactionMap;
    }

    protected RuntimeException logException(String message, Exception exception)
    {
        if (exception instanceof UserFailureException || exception instanceof TransactionOperationException)
        {
            operationLog.info(message, exception);
            return (RuntimeException) exception;
        } else
        {
            operationLog.error(message, exception);
            return new RuntimeException(message, exception);
        }
    }

}
