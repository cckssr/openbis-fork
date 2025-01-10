package ch.ethz.sis.transaction;

import java.util.Map;
import java.util.UUID;

public interface ITransactionLog
{

    void logTransaction(final TransactionLogEntry transaction);

    void deleteTransaction(UUID transactionId);

    Map<UUID, TransactionLogEntry> getTransactions();

}
