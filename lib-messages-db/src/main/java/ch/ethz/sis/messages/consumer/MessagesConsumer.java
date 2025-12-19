package ch.ethz.sis.messages.consumer;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import ch.ethz.sis.messages.db.IMessagesDatabase;
import ch.ethz.sis.messages.db.LastSeenMessage;
import ch.ethz.sis.messages.db.Message;
import ch.ethz.sis.messages.db.MessagesDatabaseUtil;
import ch.ethz.sis.messages.process.MessageProcessId;
import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.ethz.sis.shared.log.classic.impl.Logger;

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
        getMessageHandlersByType();
    }

    public void consume()
    {
        final Set<String> allSupportedMessageTypes = getAllSupportedMessageTypes();
        final Map<String, IMessageHandler> messageHandlersByType = getMessageHandlersByType();

        try
        {
            for (IMessageHandler messageHandler : messageHandlers)
            {
                messageHandler.beforeFirstMessage();
            }

            Message newestMessage = MessagesDatabaseUtil.execute(messagesDatabase,
                    () -> messagesDatabase.getMessagesDAO().getNewestByTypes(new ArrayList<>(allSupportedMessageTypes)));

            while (true)
            {
                LastSeenMessage lastSeenMessage =
                        MessagesDatabaseUtil.execute(messagesDatabase, () -> messagesDatabase.getLastSeenMessagesDAO().getByConsumerId(consumerId));

                List<Message> messages =
                        loadNextBatch(allSupportedMessageTypes, lastSeenMessage != null ? lastSeenMessage.getLastSeenMessageId() : null,
                                newestMessage != null ? newestMessage.getId() : null);

                if (messages.isEmpty())
                {
                    break;
                }

                consumeBatch(messages, messageHandlersByType, lastSeenMessage);
            }
        } catch (Exception e)
        {
            throw new RuntimeException("Message consumption has failed. No more messages will be processed.", e);
        } finally
        {
            for (IMessageHandler messageHandler : messageHandlers)
            {
                messageHandler.afterLastMessage();
            }
        }
    }

    private List<Message> loadNextBatch(Set<String> messageTypes, Long minMessageId, Long maxMessageId)
    {
        List<Message> messages = MessagesDatabaseUtil.execute(messagesDatabase, () -> messagesDatabase.getMessagesDAO()
                .listByTypesAndIdRange(new ArrayList<>(messageTypes), minMessageId, maxMessageId, messageBatchSize));

        if (messages.isEmpty())
        {
            operationLog.info("No new messages found with types " + messageTypes + ".");
        } else
        {
            List<String> foundMessageTypes = messages.stream().map(Message::getType).toList();
            operationLog.info(
                    "Found " + messages.size() + " new message(s) with types " + foundMessageTypes + ".");
        }

        return messages;
    }

    private void consumeBatch(List<Message> messages, Map<String, IMessageHandler> messageHandlersByType, LastSeenMessage lastSeenMessage)
    {
        for (Message message : messages)
        {
            IMessageHandler messageHandler = messageHandlersByType.get(message.getType());
            if (messageHandler == null)
            {
                throw new RuntimeException("Message " + toString(message) + " could not be handled. No handler found for the message type.");
            }
            executeMessageHandler(messageHandler, message);
            lastSeenMessage = updateLastSeenMessage(lastSeenMessage, message);
        }

        operationLog.info("Handled " + messages.size() + " message(s).");
    }

    private void executeMessageHandler(IMessageHandler messageHandler, Message message)
    {
        try
        {
            MessageProcessId.setCurrent(message.getProcessId());
            operationLog.info("Started handling message " + toString(message) + ".");
            messageHandler.handleMessage(message);
            operationLog.info("Finished handling message " + toString(message) + ".");
        } catch (Exception e)
        {
            throw new RuntimeException("Handling of message " + toString(message) + " has failed.", e);
        } finally
        {
            MessageProcessId.setCurrent(null);
        }
    }

    private LastSeenMessage updateLastSeenMessage(LastSeenMessage lastSeenMessage, Message message)
    {
        try
        {
            messagesDatabase.begin();

            if (lastSeenMessage == null)
            {
                lastSeenMessage = new LastSeenMessage();
                lastSeenMessage.setConsumerId(consumerId);
                lastSeenMessage.setLastSeenMessageId(message.getId());
                lastSeenMessage.setId(messagesDatabase.getLastSeenMessagesDAO().create(lastSeenMessage));
            } else
            {
                lastSeenMessage.setLastSeenMessageId(message.getId());
                messagesDatabase.getLastSeenMessagesDAO().update(lastSeenMessage);
            }

            message.setConsumptionTimestamp(new Date());
            messagesDatabase.getMessagesDAO().update(message);

            messagesDatabase.commit();
        } catch (Exception e)
        {
            try
            {
                messagesDatabase.rollback();
            } catch (Exception rollbackException)
            {
                operationLog.warn("Rollback failed", rollbackException);
            }

            throw new RuntimeException("Updating last seen message to " + toString(message) + " has failed.", e);
        }

        return lastSeenMessage;
    }

    private Set<String> getAllSupportedMessageTypes()
    {
        Set<String> allMessageTypes = new TreeSet<>();

        for (IMessageHandler messageHandler : messageHandlers)
        {
            allMessageTypes.addAll(messageHandler.getSupportedMessageTypes());
        }

        return allMessageTypes;
    }

    private Map<String, IMessageHandler> getMessageHandlersByType()
    {
        Map<String, IMessageHandler> messageHandlersByType = new HashMap<>();

        for (IMessageHandler messageHandler : messageHandlers)
        {
            Set<String> supportedMessageTypes = messageHandler.getSupportedMessageTypes();

            if (supportedMessageTypes == null || supportedMessageTypes.isEmpty())
            {
                throw new RuntimeException(
                        "Message handler " + messageHandler.getClass().getName() + " is incorrect. It does not support any message types.");
            }

            for (String messageType : supportedMessageTypes)
            {
                if (messageHandlersByType.containsKey(messageType))
                {
                    IMessageHandler existingMessageHandler = messageHandlersByType.get(messageType);
                    throw new RuntimeException(
                            "Message handlers " + existingMessageHandler.getClass().getName() + " and " + messageHandler.getClass().getName()
                                    + " both support the same message type. Configure a separate message consumer for the handlers to tract their last seen messages correctly.");
                } else
                {
                    messageHandlersByType.put(messageType, messageHandler);
                }
            }
        }

        return messageHandlersByType;
    }

    private String toString(Message message)
    {
        return "{id: " + message.getId() + ", type: '" + message.getType() + "', description: '" + message.getDescription() + "'}";
    }

}
