package ch.ethz.sis.transaction;

import java.util.Date;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionLogEntry
{

    private UUID transactionId;

    private TransactionStatus transactionStatus;

    private Boolean isTwoPhaseTransaction;

    private Set<String> participantIds;

    private Date lastAccessedDate;

    public UUID getTransactionId()
    {
        return transactionId;
    }

    public void setTransactionId(final UUID transactionId)
    {
        this.transactionId = transactionId;
    }

    public TransactionStatus getTransactionStatus()
    {
        return transactionStatus;
    }

    public void setTransactionStatus(final TransactionStatus transactionStatus)
    {
        this.transactionStatus = transactionStatus;
    }

    public Boolean isTwoPhaseTransaction()
    {
        return Boolean.TRUE.equals(isTwoPhaseTransaction);
    }

    public void setTwoPhaseTransaction(final Boolean twoPhaseTransaction)
    {
        isTwoPhaseTransaction = twoPhaseTransaction;
    }

    public Set<String> getParticipantIds()
    {
        return participantIds;
    }

    public void setParticipantIds(final Set<String> participantIds)
    {
        this.participantIds = participantIds;
    }

    public Date getLastAccessedDate()
    {
        return lastAccessedDate;
    }

    public void setLastAccessedDate(final Date lastAccessedDate)
    {
        this.lastAccessedDate = lastAccessedDate;
    }

    @Override public boolean equals(final Object o)
    {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override public int hashCode()
    {
        return Objects.hash(getTransactionId());
    }

    @Override public String toString()
    {
        return ToStringBuilder.reflectionToString(this);
    }
}
