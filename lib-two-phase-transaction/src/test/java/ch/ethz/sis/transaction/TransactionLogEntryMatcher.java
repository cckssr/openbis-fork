package ch.ethz.sis.transaction;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

public class TransactionLogEntryMatcher extends BaseMatcher<TransactionLogEntry>
{
    private final UUID transactionId;

    private final TransactionStatus transactionStatus;

    private final Set<String> participantIds;

    public TransactionLogEntryMatcher(UUID transactionId, TransactionStatus transactionStatus, Set<String> participantIds){
        this.transactionId = transactionId;
        this.transactionStatus = transactionStatus;
        this.participantIds = participantIds;
    }

    @Override public boolean matches(final Object o)
    {
        if (o instanceof TransactionLogEntry)
        {
            TransactionLogEntry entry = (TransactionLogEntry) o;
            return Objects.equals(entry.getTransactionId(), transactionId) && Objects.equals(entry.getTransactionStatus(), transactionStatus) && Objects.equals(entry.getParticipantIds(), participantIds);
        }
        return false;
    }

    @Override public void describeTo(final Description description)
    {
        description.appendText(transactionId + ", " + transactionStatus + ", " + participantIds);
    }
}