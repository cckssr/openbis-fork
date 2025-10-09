package ch.ethz.sis.messages.consumer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import ch.ethz.sis.shared.log.classic.impl.Logger;

import ch.ethz.sis.messages.db.IMessagesDatabase;
import ch.ethz.sis.messages.db.LastSeenMessage;
import ch.ethz.sis.messages.db.Message;
import ch.ethz.sis.messages.db.MessagesDatabaseUtil;
import ch.ethz.sis.messages.process.MessageProcessId;
import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;

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
        final Set<String> allSupportedMessageTypes = getAllSupportedMessageTypes();

        Message newestMessage = MessagesDatabaseUtil.execute(messagesDatabase,
                () -> messagesDatabase.getMessagesDAO().getNewestByTypes(new ArrayList<>(allSupportedMessageTypes)));

        while (true)
        {

            LastSeenMessage lastSeenMessage =
                    MessagesDatabaseUtil.execute(messagesDatabase, () -> messagesDatabase.getLastSeenMessagesDAO().getByConsumerId(consumerId));

            List<Message> messages = loadNextBatch(allSupportedMessageTypes, lastSeenMessage != null ? lastSeenMessage.getLastSeenMessageId() : null,
                    newestMessage != null ? newestMessage.getId() : null);

            if (messages.isEmpty())
            {
                break;
            }

            consume(lastSeenMessage, messages);
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
            List<String> foundMessageTypes = messages.stream().map(Message::getType).collect(Collectors.toList());
            operationLog.info(
                    "Found " + messages.size() + " new message(s) with types " + foundMessageTypes + ".");
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
                    MessageProcessId.setCurrent(message.getProcessId());
                    operationLog.info("Started handling message " + toString(message) + ".");
                    matchingHandler.handleMessage(message);
                    operationLog.info("Finished handling message " + toString(message) + ".");
                    successCounter++;
                } catch (Exception e)
                {
                    operationLog.info("Handling of message " + toString(message) + " has failed.", e);
                    failureCounter++;
                } finally
                {
                    MessageProcessId.setCurrent(null);
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
                messagesDatabase.rollback();
                throw e;
            }
        }

        operationLog.info("Handled " + messages.size() + " message(s). Successes: " + successCounter + ", failures: " + failureCounter + ".");
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

    private String toString(Message message)
    {
        return "{id: " + message.getId() + ", type: '" + message.getType() + "', description: '" + message.getDescription() + "'}";
    }

}
