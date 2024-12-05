package ch.ethz.sis.transaction;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.common.logging.LogCategory;
import ch.systemsx.cisd.common.logging.LogFactory;

public class TransactionCoordinator extends AbstractTransactionNode<TransactionCoordinator.Transaction> implements ITransactionCoordinator
{

    private static final Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION, TransactionCoordinator.class);

    private final List<ITransactionParticipant> participants;

    public TransactionCoordinator(final String transactionCoordinatorKey, final String interactiveSessionKey,
            final ISessionTokenProvider sessionTokenProvider, final List<ITransactionParticipant> participants, final ITransactionLog transactionLog,
            int transactionTimeoutInSeconds, int transactionCountLimit)
    {
        super(transactionCoordinatorKey, interactiveSessionKey, sessionTokenProvider, transactionLog, transactionTimeoutInSeconds,
                transactionCountLimit);

        if (participants == null || participants.isEmpty())
        {
            throw new IllegalArgumentException("Participants cannot be null or empty");
        }

        this.participants = participants;
    }

    @Override protected Transaction createTransactionFromLogEntry(final TransactionLogEntry logEntry)
    {
        Transaction transaction = new Transaction(logEntry.getTransactionId(), null);
        transaction.setTransactionStatus(logEntry.getTransactionStatus());
        transaction.setParticipantIds(logEntry.getParticipantIds());
        transaction.setLastAccessedDate(new Date(0));
        return transaction;
    }

    @Override protected TransactionLogEntry createLogEntryFromTransaction(final Transaction transaction)
    {
        TransactionLogEntry entry = new TransactionLogEntry();
        entry.setTransactionId(transaction.getTransactionId());
        entry.setTransactionStatus(transaction.getTransactionStatus());
        entry.setParticipantIds(transaction.getParticipantIds());
        entry.setTwoPhaseTransaction(true);
        entry.setLastAccessedDate(transaction.getLastAccessedDate());
        return entry;
    }

    @Override protected void finishFailedOrAbandonedTransactionViaRollback(final Transaction transaction)
    {
        rollbackTransaction(transaction, null, interactiveSessionKey, true);
    }

    @Override protected void finishFailedOrAbandonedTransactionViaCommit(final Transaction transaction)
    {
        commitPreparedTransaction(transaction, null, interactiveSessionKey, true);
    }

    @Override protected boolean isCoordinator()
    {
        return true;
    }

    @Override public void beginTransaction(final UUID transactionId, final String sessionToken, final String interactiveSessionKey)
    {
        try
        {
            checkTransactionId(transactionId);
            checkSessionToken(sessionToken);
            checkInteractiveSessionKey(interactiveSessionKey);

            Transaction transaction = new Transaction(transactionId, sessionToken);
            registerTransaction(transaction);

            transaction.executeWithLockOrFail(() ->
            {
                try
                {
                    changeTransactionStatus(transaction, TransactionStatus.BEGIN_STARTED);
                    operationLog.info("Begin transaction '" + transactionId + "'.");
                    changeTransactionStatus(transaction, TransactionStatus.BEGIN_FINISHED);
                    return null;
                } catch (Exception beginException)
                {
                    try
                    {
                        transactionLog.deleteTransaction(transactionId);
                        transactionMap.remove(transactionId);
                    } catch (Exception deleteException)
                    {
                        operationLog.warn("Could not delete transaction '" + transactionId + "'.", deleteException);
                    }

                    throw beginException;
                }
            }, true);
        } catch (Exception e)
        {
            throw logException("Begin transaction '" + transactionId + "'failed.", e);
        }
    }

    @Override public <T> T executeOperation(final UUID transactionId, final String sessionToken, final String interactiveSessionKey,
            final String participantId, final String operationName, final Object[] operationArguments)
    {
        try
        {
            checkTransactionId(transactionId);
            checkSessionToken(sessionToken);
            checkInteractiveSessionKey(interactiveSessionKey);
            checkParticipantId(participantId);
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

                operationLog.info("Transaction '" + transactionId + "' execute operation '" + operationName + "' for participant '" + participantId
                        + "' started.");

                for (ITransactionParticipant participant : participants)
                {
                    if (Objects.equals(participant.getParticipantId(), participantId))
                    {
                        /*
                          Begin transaction for participant if this is the first operation executed on that participant.
                        */

                        if (transaction.getParticipantIds() == null || !transaction.getParticipantIds().contains(participantId))
                        {
                            operationLog.info("Begin transaction '" + transactionId + "' for participant '" + participant.getParticipantId() + "'.");

                            try
                            {
                                participant.beginTransaction(transactionId, sessionToken, interactiveSessionKey, transactionCoordinatorKey);

                                Set<String> newParticipantIds = new HashSet<>();
                                if (transaction.getParticipantIds() != null)
                                {
                                    newParticipantIds.addAll(transaction.getParticipantIds());
                                }
                                newParticipantIds.add(participantId);

                                transaction.setParticipantIds(newParticipantIds);
                                changeTransactionStatus(transaction, TransactionStatus.BEGIN_FINISHED);
                            } catch (Exception e)
                            {
                                throw new RuntimeException(
                                        "Begin transaction '" + transactionId + "' failed for participant '" + participant.getParticipantId() + "'.",
                                        e);
                            }
                        }

                        /*
                          An exception thrown by the executed operation does not trigger an automatic rollback.
                          The client has the freedom to decide whether to rollback or keep on working with the current transaction.
                          If the transaction gets abandoned by the client, then it will time out and be automatically rolled back by the coordinator.
                        */

                        T result;

                        try
                        {
                            result =
                                    participant.executeOperation(transactionId, sessionToken, interactiveSessionKey, operationName,
                                            operationArguments);
                        } catch (TransactionOperationException e)
                        {
                            throw new TransactionOperationException(
                                    "Transaction '" + transactionId + "' execute operation '" + operationName + "' for participant '" + participantId
                                            + "' failed with error: " + e.getMessage(), e.getCause());
                        }

                        operationLog.info(
                                "Transaction '" + transactionId + "' execute operation '" + operationName + "' for participant '" + participantId
                                        + "' finished successfully.");

                        return result;
                    }
                }

                return null;
            }, true);
        } catch (Exception e)
        {
            throw logException(
                    "Transaction '" + transactionId + "' execute operation '" + operationName + "' for participant '" + participantId + "' failed.",
                    e);
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
                checkTransactionStatus(transaction, TransactionStatus.BEGIN_FINISHED);

                operationLog.info("Commit transaction '" + transactionId + "' started.");

                prepareTransaction(transaction, sessionToken, interactiveSessionKey);

                try
                {
                    commitPreparedTransaction(transaction, sessionToken, interactiveSessionKey, false);

                    operationLog.info("Commit transaction '" + transactionId + "' finished successfully.");
                } catch (Exception commitException)
                {
                    operationLog.error("Commit transaction '" + transactionId + "' failed. It will be automatically retried by the coordinator.",
                            commitException);

                    /*
                      Do not throw the exception to the client as there is nothing it can do,
                      the commit will be retried automatically by the coordinator
                     */
                }

                return null;
            }, true);
        } catch (Exception e)
        {
            throw logException("Commit transaction '" + transactionId + "' failed.", e);
        }
    }

    private void prepareTransaction(final Transaction transaction, final String sessionToken, final String interactiveSessionKey)
    {
        operationLog.info("Prepare transaction '" + transaction.getTransactionId() + "' started.");

        changeTransactionStatus(transaction, TransactionStatus.PREPARE_STARTED);

        for (ITransactionParticipant participant : participants)
        {
            if (transaction.getParticipantIds() == null || !transaction.getParticipantIds().contains(participant.getParticipantId()))
            {
                operationLog.info(
                        "Skipping prepare of transaction '" + transaction.getTransactionId() + "' for participant '" + participant.getParticipantId()
                                + "' as no operations were executed on that participant.");
                continue;
            }

            try
            {
                operationLog.info(
                        "Prepare transaction '" + transaction.getTransactionId() + "' for participant '" + participant.getParticipantId() + "'.");
                participant.prepareTransaction(transaction.getTransactionId(), sessionToken, interactiveSessionKey, transactionCoordinatorKey);
            } catch (Exception prepareException)
            {
                try
                {
                    rollbackTransaction(transaction, sessionToken, interactiveSessionKey, false);
                } catch (Exception rollbackException)
                {
                    operationLog.warn("Rollback transaction '" + transaction.getTransactionId() + "' failed.", rollbackException);
                }

                throw new RuntimeException(
                        "Prepare transaction '" + transaction.getTransactionId() + "' failed for participant '" + participant.getParticipantId()
                                + "'. The transaction was rolled back.", prepareException);
            }
        }

        changeTransactionStatus(transaction, TransactionStatus.PREPARE_FINISHED);

        operationLog.info("Prepare transaction '" + transaction.getTransactionId() + "' finished successfully.");
    }

    private void commitPreparedTransaction(Transaction transaction, final String sessionToken, final String interactiveSessionKey,
            final boolean recovery)
    {
        operationLog.info("Commit prepared transaction '" + transaction.getTransactionId() + "' started.");

        changeTransactionStatus(transaction, TransactionStatus.COMMIT_STARTED);

        RuntimeException firstException = null;

        for (ITransactionParticipant participant : participants)
        {
            if (transaction.getParticipantIds() == null || !transaction.getParticipantIds().contains(participant.getParticipantId()))
            {
                operationLog.info(
                        "Skipping commit of prepared transaction '" + transaction.getTransactionId() + "' for participant '"
                                + participant.getParticipantId()
                                + "' as no operations were executed on that participant.");
                continue;
            }

            try
            {
                if (recovery)
                {
                    List<UUID> transactions = participant.recoverTransactions(interactiveSessionKey, transactionCoordinatorKey);

                    if (transactions != null && transactions.contains(transaction.getTransactionId()))
                    {
                        operationLog.info(
                                "Commit prepared transaction '" + transaction.getTransactionId() + "' for participant '"
                                        + participant.getParticipantId() + "'.");
                        participant.commitRecoveredTransaction(transaction.getTransactionId(), interactiveSessionKey, transactionCoordinatorKey);
                    } else
                    {
                        operationLog.info(
                                "Skipping commit of prepared transaction '" + transaction.getTransactionId() + "' for participant '"
                                        + participant.getParticipantId()
                                        + "'. The transaction has been already committed at that participant before.");
                    }
                } else
                {
                    operationLog.info(
                            "Commit prepared transaction '" + transaction.getTransactionId() + "' for participant '"
                                    + participant.getParticipantId()
                                    + "'.");
                    participant.commitTransaction(transaction.getTransactionId(), sessionToken, interactiveSessionKey);
                }
            } catch (RuntimeException e)
            {
                if (firstException == null)
                {
                    firstException =
                            new RuntimeException("Commit prepared transaction '" + transaction.getTransactionId() + "' failed for participant '"
                                    + participant.getParticipantId() + "'.", e);
                }
            }
        }

        if (firstException == null)
        {
            changeTransactionStatus(transaction, TransactionStatus.COMMIT_FINISHED);
            operationLog.info("Commit prepared transaction '" + transaction.getTransactionId() + "' finished successfully.");
        } else
        {
            throw firstException;
        }
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
                checkTransactionStatus(transaction, TransactionStatus.BEGIN_STARTED, TransactionStatus.BEGIN_FINISHED,
                        TransactionStatus.PREPARE_STARTED, TransactionStatus.PREPARE_FINISHED, TransactionStatus.ROLLBACK_STARTED,
                        TransactionStatus.ROLLBACK_FINISHED);

                try
                {
                    rollbackTransaction(transaction, sessionToken, interactiveSessionKey, false);
                } catch (Exception rollbackException)
                {
                    operationLog.error("Rollback transaction '" + transactionId + "' failed. It will be automatically retried by the coordinator.",
                            rollbackException);

                    /*
                      Do not throw the exception to the client as there is nothing it can do,
                      the rollback will be retried automatically by the coordinator
                     */
                }

                return null;
            }, true);
        } catch (Exception e)
        {
            throw logException("Rollback transaction '" + transactionId + "' failed.", e);
        }
    }

    private void rollbackTransaction(final Transaction transaction, final String sessionToken, final String interactiveSessionKey,
            final boolean recovery)
    {
        operationLog.info("Rollback transaction '" + transaction.getTransactionId() + "' started.");

        changeTransactionStatus(transaction, TransactionStatus.ROLLBACK_STARTED);

        RuntimeException firstException = null;

        for (ITransactionParticipant participant : participants)
        {
            if (transaction.getParticipantIds() == null || !transaction.getParticipantIds().contains(participant.getParticipantId()))
            {
                operationLog.info(
                        "Skipping rollback of transaction '" + transaction.getTransactionId() + "' for participant '" + participant.getParticipantId()
                                + "' as no operations were executed on that participant.");
                continue;
            }

            try
            {
                operationLog.info(
                        "Rollback transaction '" + transaction.getTransactionId() + "' for participant '" + participant.getParticipantId()
                                + "'.");
                if (recovery)
                {
                    participant.rollbackRecoveredTransaction(transaction.getTransactionId(), interactiveSessionKey, transactionCoordinatorKey);
                } else
                {
                    participant.rollbackTransaction(transaction.getTransactionId(), sessionToken, interactiveSessionKey);
                }
            } catch (RuntimeException e)
            {
                if (firstException == null)
                {
                    firstException = new RuntimeException(
                            "Rollback transaction '" + transaction.getTransactionId() + "' failed for participant '" + participant.getParticipantId()
                                    + "'.", e);
                }
            }
        }

        if (firstException == null)
        {
            changeTransactionStatus(transaction, TransactionStatus.ROLLBACK_FINISHED);
            operationLog.info("Rollback transaction '" + transaction.getTransactionId() + "' finished successfully.");
        } else
        {
            throw firstException;
        }
    }

    private void checkParticipantId(final String participantId)
    {
        if (participantId == null)
        {
            throw new UserFailureException("Participant id cannot be null");
        }

        for (ITransactionParticipant participant : participants)
        {
            if (participantId.equals(participant.getParticipantId()))
            {
                return;
            }
        }

        throw new UserFailureException("Unknown participant with id '" + participantId + "'");
    }

    public static class Transaction extends AbstractTransaction
    {

        private Set<String> participantIds = new HashSet<>();

        public Transaction(final UUID transactionId, final String sessionToken)
        {
            super(transactionId, sessionToken);
        }

        @Override protected <T> T executeAction(final Callable<T> action) throws Exception
        {
            return action.call();
        }

        public void setParticipantIds(final Set<String> participantIds)
        {
            this.participantIds = participantIds;
        }

        public Set<String> getParticipantIds()
        {
            return participantIds;
        }

    }

}
