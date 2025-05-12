package ch.ethz.sis.messages.consumer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import ch.ethz.sis.messages.db.IMessagesDatabase;
import ch.ethz.sis.messages.db.LastSeenMessage;
import ch.ethz.sis.messages.db.Message;
import ch.systemsx.cisd.common.logging.LogCategory;
import ch.systemsx.cisd.common.logging.LogFactory;

public class MessagesConsumer
{

    private static final Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION, MessagesConsumer.class);

    private final String consumerId;

    private final List<IMessageHandler> messageHandlers;

    private final int messageBatchSize;

    private final IMessagesDatabase messagesDatabase;

    public MessagesConsumer(String consumerId, List<IMessageHandler> messageHandlers, int messageBatchSize, IMessagesDatabase messagesDatabase)
    {
        this.consumerId = consumerId;
        this.messageHandlers = messageHandlers;
        this.messageBatchSize = messageBatchSize;
        this.messagesDatabase = messagesDatabase;
    }

    public void consume()
    {
        while (true)
        {
            messagesDatabase.begin();
            LastSeenMessage lastSeenMessage = messagesDatabase.getLastSeenMessagesDAO().getByConsumerId(consumerId);
            messagesDatabase.commit();

            List<Message> messages = loadNextBatch(lastSeenMessage);

            if (messages.isEmpty())
            {
                break;
            }

            consume(lastSeenMessage, messages);
        }
    }

    private List<Message> loadNextBatch(LastSeenMessage lastSeenMessage)
    {
        Set<String> allMessageTypes = new TreeSet<>();

        for (IMessageHandler messageHandler : messageHandlers)
        {
            allMessageTypes.addAll(messageHandler.getSupportedMessageTypes());
        }

        messagesDatabase.begin();
        List<Message> messages = messagesDatabase.getMessagesDAO().listByTypesAndLastSeenId(new ArrayList<>(allMessageTypes),
                lastSeenMessage != null ? lastSeenMessage.getLastSeenMessageId() : null, messageBatchSize);
        messagesDatabase.commit();

        if (messages.isEmpty())
        {
            operationLog.info(
                    "Message consumer '" + consumerId + "' found no new messages with types " + allMessageTypes + ".");

        } else
        {
            List<String> foundMessageTypes = messages.stream().map(Message::getType).collect(Collectors.toList());
            operationLog.info(
                    "Message consumer '" + consumerId + "' found " + messages.size() + " new message(s) with types "
                            + foundMessageTypes + ".");
        }

        return messages;
    }

    private void consume(LastSeenMessage lastSeenMessage, List<Message> messages)
    {
        int successCounter = 0;
        int failureCounter = 0;

        for (Message message : messages)
        {
            IMessageHandler matchingHandler = null;

            for (IMessageHandler messageHandler : messageHandlers)
            {
                Set<String> supportedMessageTypes = messageHandler.getSupportedMessageTypes();

                if (supportedMessageTypes != null && supportedMessageTypes.contains(message.getType()))
                {
                    matchingHandler = messageHandler;
                    break;
                }
            }

            if (matchingHandler != null)
            {
                try
                {
                    matchingHandler.handleMessage(message);
                    operationLog.info("Handled message " + toString(message) + ".");
                    successCounter++;
                } catch (Exception e)
                {
                    operationLog.info("Handling of message " + toString(message) + " has failed.", e);
                    failureCounter++;
                }
            } else
            {
                throw new RuntimeException("Message " + toString(message) + " could not be handled. No handler found for the message type.");
            }

            try
            {
                messagesDatabase.begin();

                if (lastSeenMessage == null)
                {
                    lastSeenMessage = new LastSeenMessage();
                    lastSeenMessage.setConsumerId(consumerId);
                    lastSeenMessage.setLastSeenMessageId(message.getId());
                    messagesDatabase.getLastSeenMessagesDAO().create(lastSeenMessage);
                } else
                {
                    lastSeenMessage.setLastSeenMessageId(message.getId());
                    messagesDatabase.getLastSeenMessagesDAO().update(lastSeenMessage);
                }

                message.setConsumptionTimestamp(new Date());
                messagesDatabase.getMessagesDAO().update(message);

                messagesDatabase.commit();
            } finally
            {
                messagesDatabase.rollback();
            }
        }

        operationLog.info(
                "Message consumer '" + consumerId + "' handled " + messages.size() + " message(s). Successes: " + successCounter + ", failures: "
                        + failureCounter + ".");
    }

    private String toString(Message message)
    {
        return "{id: " + message.getId() + ", type: '" + message.getType() + "', description: '" + message.getDescription() + "'}";
    }

}
