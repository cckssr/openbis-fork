package ch.ethz.sis.transaction;

import java.util.UUID;

public interface ITransactionCoordinator
{

    void beginTransaction(UUID transactionId, String sessionToken, String interactiveSessionKey);

    <T> T executeOperation(UUID transactionId, String sessionToken, String interactiveSessionKey, String participantId, String operationName,
            Object[] operationArguments) throws TransactionOperationException;

    void commitTransaction(UUID transactionId, String sessionToken, String interactiveSessionKey);

    void rollbackTransaction(UUID transactionId, String sessionToken, String interactiveSessionKey);

}
