package ch.ethz.sis.messages.db;

public class LastSeenMessage
{

    private Long id;

    private Long lastSeenMessageId;

    private String consumerId;

    public Long getId()
    {
        return id;
    }

    public void setId(final Long id)
    {
        this.id = id;
    }

    public Long getLastSeenMessageId()
    {
        return lastSeenMessageId;
    }

    public void setLastSeenMessageId(final Long lastSeenMessageId)
    {
        this.lastSeenMessageId = lastSeenMessageId;
    }

    public String getConsumerId()
    {
        return consumerId;
    }

    public void setConsumerId(final String consumerId)
    {
        this.consumerId = consumerId;
    }
}
