package ch.ethz.sis.transaction;

import static org.testng.Assert.assertEquals;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class TransactionTestUtil
{

    public static void assertTransactions(Map<UUID, ? extends AbstractTransaction> actualTransactions, TestTransaction... expectedTransactions)
    {
        Map<String, String> actualTransactionsMap = new TreeMap<>();
        Map<String, String> expectedTransactionsMap = new TreeMap<>();

        for (AbstractTransaction actualTransaction : actualTransactions.values())
        {
            actualTransactionsMap.put(actualTransaction.getTransactionId().toString(), actualTransaction.getTransactionStatus().toString());
        }

        for (TestTransaction expectedTransaction : expectedTransactions)
        {
            expectedTransactionsMap.put(expectedTransaction.getTransactionId().toString(), expectedTransaction.getTransactionStatus().toString());
        }

        assertEquals(actualTransactionsMap.toString(), expectedTransactionsMap.toString());
    }

    public static class TestTransaction
    {

        private final UUID transactionId;

        private final TransactionStatus transactionStatus;

        public TestTransaction(final UUID transactionId, final TransactionStatus transactionStatus)
        {
            this.transactionId = transactionId;
            this.transactionStatus = transactionStatus;
        }

        public UUID getTransactionId()
        {
            return transactionId;
        }

        public TransactionStatus getTransactionStatus()
        {
            return transactionStatus;
        }
    }

}
