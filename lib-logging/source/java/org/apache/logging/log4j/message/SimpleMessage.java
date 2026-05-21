package org.apache.logging.log4j.message;

public class SimpleMessage implements Message
{
    private final String message;

    public SimpleMessage(String message)
    {
        this.message = message;
    }

    @Override
    public String getFormattedMessage()
    {
        return message;
    }

    @Override
    public String toString()
    {
        return getFormattedMessage();
    }
}
