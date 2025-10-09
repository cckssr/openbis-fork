package ch.ethz.sis.messages.consumer;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.Test;

import ch.ethz.sis.messages.db.ILastSeenMessagesDAO;
import ch.ethz.sis.messages.db.IMessagesDAO;
import ch.ethz.sis.messages.db.IMessagesDatabase;
import ch.ethz.sis.messages.db.LastSeenMessage;
import ch.ethz.sis.messages.db.Message;

public class MessagesConsumerTest
{

    private static final String CONSUMER_ID = "test-consumer-id";

    private static final String MESSAGE_TYPE_1 = "message-type-1";

    private static final String MESSAGE_TYPE_2 = "message-type-2";

    private static final int BATCH_SIZE = 10;

    @Test
    public void test()
    {
        Mockery context = new Mockery();

        IMessageHandler handler1 = context.mock(IMessageHandler.class, "handler1");
        IMessageHandler handler2 = context.mock(IMessageHandler.class, "handler2");
        IMessagesDatabase database = context.mock(IMessagesDatabase.class);
        IMessagesDAO messagesDAO = context.mock(IMessagesDAO.class);
        ILastSeenMessagesDAO lastSeenMessagesDAO = context.mock(ILastSeenMessagesDAO.class);

        Message message1 = new Message();
        message1.setId(1L);
        message1.setType(MESSAGE_TYPE_1);

        Message message2 = new Message();
        message2.setId(2L);
        message2.setType(MESSAGE_TYPE_2);

        context.checking(new Expectations()
        {
            {
                // setup
                allowing(database).getMessagesDAO();
                will(returnValue(messagesDAO));

                allowing(database).getLastSeenMessagesDAO();
                will(returnValue(lastSeenMessagesDAO));

                allowing(handler1).getSupportedMessageTypes();
                will(returnValue(Set.of(MESSAGE_TYPE_1)));

                allowing(handler2).getSupportedMessageTypes();
                will(returnValue(Set.of(MESSAGE_TYPE_2)));

                // get newest = message2
                one(database).begin();
                one(messagesDAO).getNewestByTypes(List.of(MESSAGE_TYPE_1, MESSAGE_TYPE_2));
                will(returnValue(message2));
                one(database).commit();

                // get last seen = null
                one(database).begin();
                one(lastSeenMessagesDAO).getByConsumerId(CONSUMER_ID);
                will(returnValue(null));
                one(database).commit();

                // get messages between last seen = null and newest = message2
                one(database).begin();
                one(messagesDAO).listByTypesAndIdRange(List.of(MESSAGE_TYPE_1, MESSAGE_TYPE_2), null, message2.getId(), BATCH_SIZE);
                will(returnValue(List.of(message1, message2)));
                one(database).commit();

                // handle message1
                one(handler1).handleMessage(message1);

                // create last seen with message1
                LastSeenMessage lastSeenMessage1 = new LastSeenMessage();
                lastSeenMessage1.setConsumerId(CONSUMER_ID);
                lastSeenMessage1.setLastSeenMessageId(message1.getId());

                one(database).begin();
                one(lastSeenMessagesDAO).create(with(new LastSeenMessageMatcher(lastSeenMessage1)));
                one(messagesDAO).update(with(new MessageMatcher(message1)));
                one(database).commit();

                // handle message2
                one(handler2).handleMessage(message2);

                // update last seen to message2
                LastSeenMessage lastSeenMessage2 = new LastSeenMessage();
                lastSeenMessage2.setConsumerId(CONSUMER_ID);
                lastSeenMessage2.setLastSeenMessageId(message2.getId());

                one(database).begin();
                one(lastSeenMessagesDAO).update(with(new LastSeenMessageMatcher(lastSeenMessage2)));
                one(messagesDAO).update(with(new MessageMatcher(message2)));
                one(database).commit();

                // get last seen = message2
                one(database).begin();
                one(lastSeenMessagesDAO).getByConsumerId(CONSUMER_ID);
                will(returnValue(lastSeenMessage2));
                one(database).commit();

                // get messages between last seen = message2 and newest = message2
                one(database).begin();
                one(messagesDAO).listByTypesAndIdRange(List.of(MESSAGE_TYPE_1, MESSAGE_TYPE_2), message2.getId(), message2.getId(), BATCH_SIZE);
                will(returnValue(List.of()));
                one(database).commit();
            }
        });

        MessagesConsumer consumer = new MessagesConsumer(CONSUMER_ID, List.of(handler1, handler2), BATCH_SIZE, database);
        consumer.consume();

        context.assertIsSatisfied();
    }

    private static class MessageMatcher extends BaseMatcher<Message>
    {

        private final Message message;

        public MessageMatcher(Message message)
        {
            this.message = message;
        }

        @Override
        public boolean matches(Object obj)
        {
            if (obj instanceof Message)
            {
                Message otherMessage = (Message) obj;
                return Objects.equals(message.getId(), otherMessage.getId()) && Objects.equals(message.getType(), otherMessage.getType());
            } else
            {
                return false;
            }
        }

        @Override
        public void describeTo(Description description)
        {
            description.appendText("Message [id: " + message.getId() + ", type:" + message.getType() + "]");
        }
    }

    private static class LastSeenMessageMatcher extends BaseMatcher<LastSeenMessage>
    {

        private final LastSeenMessage lastSeenMessage;

        public LastSeenMessageMatcher(LastSeenMessage lastSeenMessage)
        {
            this.lastSeenMessage = lastSeenMessage;
        }

        @Override
        public boolean matches(Object obj)
        {
            if (obj instanceof LastSeenMessage)
            {
                LastSeenMessage otherLastSeenMessage = (LastSeenMessage) obj;
                return Objects.equals(lastSeenMessage.getLastSeenMessageId(),
                        otherLastSeenMessage.getLastSeenMessageId()) && Objects.equals(lastSeenMessage.getConsumerId(),
                        otherLastSeenMessage.getConsumerId());
            } else
            {
                return false;
            }
        }

        @Override
        public void describeTo(Description description)
        {
            description.appendText(
                    "LastSeenMessage [lastSeenMessageId:" + lastSeenMessage.getLastSeenMessageId() + ", consumerId: "
                            + lastSeenMessage.getConsumerId() + "]");
        }
    }

}
