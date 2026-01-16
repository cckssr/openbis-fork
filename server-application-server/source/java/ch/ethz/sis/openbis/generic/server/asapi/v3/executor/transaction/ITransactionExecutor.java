package ch.ethz.sis.openbis.generic.server.asapi.v3.executor.transaction;


public interface ITransactionExecutor
{
    /***
     * Helper method for executing logic in a separate transaction. Create a new transaction,
     * and suspend the current transaction if one exists. New transaction is independent and isolated
     * from parent transaction.
     * @param runnable custom logic
     * @throws Exception
     */
    void executeInSeparateTransaction(Runnable runnable) throws Exception;
}
