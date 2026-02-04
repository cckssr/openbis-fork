package ch.ethz.sis.transaction;

import java.util.UUID;

public class TransactionId
{
    private static final ThreadLocal<UUID> transactionIds = new ThreadLocal<>();

    public static void setCurrent(UUID transactionId)
    {
        synchronized (TransactionId.class)
        {
            transactionIds.set(transactionId);
        }
    }

    public static UUID getCurrent()
    {
        synchronized (TransactionId.class)
        {
            return transactionIds.get();
        }
    }
}
