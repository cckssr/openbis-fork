package ch.ethz.sis.messages.consumer;

import java.util.Set;

import ch.ethz.sis.messages.db.Message;

public interface IMessageHandler
{
    Set<String> getSupportedMessageTypes();

    default void beforeFirstMessage()
    {
    }

    void handleMessage(Message message);

    default void afterLastMessage()
    {
    }

}
