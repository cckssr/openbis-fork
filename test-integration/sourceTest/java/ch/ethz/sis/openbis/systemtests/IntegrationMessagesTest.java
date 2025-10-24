package ch.ethz.sis.openbis.systemtests;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import ch.ethz.sis.openbis.afsserver.server.common.DatabaseConfiguration;
import ch.ethz.sis.openbis.afsserver.server.messages.MessagesDatabaseConfiguration;
import ch.ethz.sis.messages.consumer.IMessageHandler;
import ch.ethz.sis.messages.consumer.MessagesConsumer;
import ch.ethz.sis.messages.db.Message;
import ch.ethz.sis.messages.db.MessagesDatabase;
import ch.ethz.sis.openbis.systemtests.common.AbstractIntegrationTest;

public class IntegrationMessagesTest extends AbstractIntegrationTest
{

    private static final String CONSUMER_ID = "integration-messages-test-consumer-id";

    private static final String MESSAGE_TYPE_1 = "integration-messages-test-type-1";

    private static final String MESSAGE_TYPE_2 = "integration-messages-test-type-2";

    private MessagesDatabase messagesDatabase;

    @BeforeMethod
    public void beforeMethod(Method method) throws Exception
    {
        super.beforeMethod(method);
        DatabaseConfiguration messagesDatabaseConfiguration = MessagesDatabaseConfiguration.getInstance(getAfsServerConfiguration());
        messagesDatabase = new MessagesDatabase(messagesDatabaseConfiguration.getDataSource());
    }

    @AfterMethod
    public void afterMethod(Method method) throws Exception
    {
        super.afterMethod(method);
    }

    @Test
    public void testConsumeMessages()
    {
        MessageHandler handler1 = new MessageHandler(Set.of(MESSAGE_TYPE_1), false);
        MessageHandler handler2 = new MessageHandler(Set.of(MESSAGE_TYPE_2), true);

        MessagesConsumer consumer = new MessagesConsumer(CONSUMER_ID, List.of(handler1, handler2), 1, messagesDatabase);

        messagesDatabase.begin();

        // create 3 messages
        Message message1 = new Message();
        message1.setType(MESSAGE_TYPE_1);
        message1.setDescription(UUID.randomUUID().toString());
        messagesDatabase.getMessagesDAO().create(message1);

        Message message2 = new Message();
        message2.setType(MESSAGE_TYPE_2);
        message2.setDescription(UUID.randomUUID().toString());
        messagesDatabase.getMessagesDAO().create(message2);

        Message message3 = new Message();
        message3.setType(MESSAGE_TYPE_1);
        message3.setDescription(UUID.randomUUID().toString());
        messagesDatabase.getMessagesDAO().create(message3);

        // check there are 3 unconsumed messages
        List<Message> beforeMessages = messagesDatabase.getMessagesDAO().listByTypesNotConsumed(List.of(MESSAGE_TYPE_1, MESSAGE_TYPE_2));
        Assert.assertEquals(beforeMessages.size(), 3);
        messagesDatabase.commit();

        // consume
        consumer.consume();

        // check there are 0 unconsumed messages
        messagesDatabase.begin();
        List<Message> afterMessages = messagesDatabase.getMessagesDAO().listByTypesNotConsumed(List.of(MESSAGE_TYPE_1, MESSAGE_TYPE_2));
        Assert.assertEquals(afterMessages.size(), 0);
        messagesDatabase.commit();
    }

    @Test
    public void testConsumeMessagesThatProduceNewMessages()
    {
        Message messageCreatedByHandler = new Message();
        messageCreatedByHandler.setType(MESSAGE_TYPE_1);
        messageCreatedByHandler.setDescription(UUID.randomUUID().toString());

        MessageHandler handler1 = new MessageHandler(Set.of(MESSAGE_TYPE_1), messageCreatedByHandler);
        MessageHandler handler2 = new MessageHandler(Set.of(MESSAGE_TYPE_2), false);

        MessagesConsumer consumer = new MessagesConsumer(CONSUMER_ID, List.of(handler1, handler2), 1, messagesDatabase);

        messagesDatabase.begin();

        // create 2 messages
        Message message1 = new Message();
        message1.setType(MESSAGE_TYPE_1);
        message1.setDescription(UUID.randomUUID().toString());
        messagesDatabase.getMessagesDAO().create(message1);

        Message message2 = new Message();
        message2.setType(MESSAGE_TYPE_2);
        message2.setDescription(UUID.randomUUID().toString());
        messagesDatabase.getMessagesDAO().create(message2);

        // check there are 2 unconsumed messages
        List<Message> beforeMessages = messagesDatabase.getMessagesDAO().listByTypesNotConsumed(List.of(MESSAGE_TYPE_1, MESSAGE_TYPE_2));
        Assert.assertEquals(beforeMessages.size(), 2);
        messagesDatabase.commit();

        // consume
        consumer.consume();

        // check there is 1 unconsumed message (the one created by handler)
        messagesDatabase.begin();
        List<Message> afterMessages = messagesDatabase.getMessagesDAO().listByTypesNotConsumed(List.of(MESSAGE_TYPE_1, MESSAGE_TYPE_2));
        Assert.assertEquals(afterMessages.size(), 1);
        Assert.assertEquals(afterMessages.get(0).getDescription(), messageCreatedByHandler.getDescription());
        messagesDatabase.commit();
    }

    private class MessageHandler implements IMessageHandler
    {

        private final Set<String> supportedMessageTypes;

        private boolean fail;

        private Message messageToCreate;

        public MessageHandler(Set<String> supportedMessageTypes, boolean fail)
        {
            this.supportedMessageTypes = supportedMessageTypes;
            this.fail = fail;
        }

        public MessageHandler(Set<String> supportedMessageTypes, Message messageToCreate)
        {
            this.supportedMessageTypes = supportedMessageTypes;
            this.messageToCreate = messageToCreate;
        }

        @Override public Set<String> getSupportedMessageTypes()
        {
            return supportedMessageTypes;
        }

        @Override public void handleMessage(final Message message)
        {
            if (fail)
            {
                throw new RuntimeException("Intentional failure of handling message: " + message.getId());
            }

            if (messageToCreate != null)
            {
                messagesDatabase.begin();
                Long messageId = messagesDatabase.getMessagesDAO().create(messageToCreate);
                log("Created message: " + messageId + " while handling message: " + message.getId());
                messagesDatabase.commit();
            }
        }
    }

}
