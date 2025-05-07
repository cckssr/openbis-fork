package ch.ethz.sis.messagesdb;

import java.util.Date;

public class Message
{

    private Long id;

    private String type;

    private String description;

    private String metaData;

    private String processId;

    private Date creationTimestamp;

    public Long getId()
    {
        return id;
    }

    public void setId(final Long id)
    {
        this.id = id;
    }

    public String getType()
    {
        return type;
    }

    public void setType(final String type)
    {
        this.type = type;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(final String description)
    {
        this.description = description;
    }

    public String getMetaData()
    {
        return metaData;
    }

    public void setMetaData(final String metaData)
    {
        this.metaData = metaData;
    }

    public String getProcessId()
    {
        return processId;
    }

    public void setProcessId(final String processId)
    {
        this.processId = processId;
    }

    public Date getCreationTimestamp()
    {
        return creationTimestamp;
    }

    public void setCreationTimestamp(final Date creationTimestamp)
    {
        this.creationTimestamp = creationTimestamp;
    }
}
