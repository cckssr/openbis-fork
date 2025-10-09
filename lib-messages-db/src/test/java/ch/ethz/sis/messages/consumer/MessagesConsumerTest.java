package ch.ethz.sis.messages.consumer;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import ch.ethz.sis.messages.db.ILastSeenMessagesDAO;
import ch.ethz.sis.messages.db.IMessagesDAO;
import ch.ethz.sis.messages.db.IMessagesDatabase;
import ch.ethz.sis.messages.db.LastSeenMessage;
import ch.ethz.sis.messages.db.Message;
import ch.ethz.sis.shared.log.classic.utils.LogRecordingUtils;
import ch.ethz.sis.shared.log.standard.core.Level;
import ch.ethz.sis.shared.log.standard.handlers.BufferedAppender;
import ch.systemsx.cisd.common.test.AssertionUtil;

public class MessagesConsumerTest
{

    private static final String CONSUMER_ID = "test-consumer-id";

    private static final String MESSAGE_TYPE_1 = "message-type-1";

    private static final String MESSAGE_TYPE_2 = "message-type-2";

    private static final int BATCH_SIZE = 10;

    private BufferedAppender logRecorder;

    private Mockery context;

    private IMessageHandler handler1;

    private IMessageHandler handler2;

    private IMessagesDatabase database;

    private IMessagesDAO messagesDAO;

    private ILastSeenMessagesDAO lastSeenMessagesDAO;

    @BeforeMethod
    public void beforeTest()
    {
        logRecorder = LogRecordingUtils.createRecorder("%-5p %c - %m%n", Level.DEBUG);
        context = new Mockery();
        handler1 = context.mock(IMessageHandler.class, "handler1");
        handler2 = context.mock(IMessageHandler.class, "handler2");
        database = context.mock(IMessagesDatabase.class);
        messagesDAO = context.mock(IMessagesDAO.class);
        lastSeenMessagesDAO = context.mock(ILastSeenMessagesDAO.class);
        context.checking(new Expectations()
        {{
            allowing(database).getMessagesDAO();
            will(returnValue(messagesDAO));

            allowing(database).getLastSeenMessagesDAO();
            will(returnValue(lastSeenMessagesDAO));

            allowing(handler1).getSupportedMessageTypes();
            will(returnValue(Set.of(MESSAGE_TYPE_1)));

            allowing(handler2).getSupportedMessageTypes();
            will(returnValue(Set.of(MESSAGE_TYPE_2)));
        }});
    }

    @AfterMethod
    public void afterTest()
    {
        logRecorder.reset();
        context.assertIsSatisfied();
    }

    @Test
    public void testConsumeTwoSuccessfulMessages()
    {
        Message message1 = message(1L, MESSAGE_TYPE_1);
        Message message2 = message(2L, MESSAGE_TYPE_2);

        context.checking(new Expectations()
        {
            {
                // get newest = message2
                expectGetNewestMessagesByTypes(List.of(MESSAGE_TYPE_1, MESSAGE_TYPE_2), message2);

                // get last seen = null
                expectGetLastSeenMessage(null);

                // get messages between [null, message2] returns [message1, message2]
                expectListMessagesByTypesAndIdRange(List.of(MESSAGE_TYPE_1, MESSAGE_TYPE_2), null, message2.getId(), BATCH_SIZE,
                        List.of(message1, message2));

                // handle message1
                one(handler1).handleMessage(message1);

                // create last seen = message1
                expectCreateLastSeenMessage(message1);

                // handle message2
                one(handler2).handleMessage(message2);

                // update last seen = message2
                expectUpdateLastSeenMessage(message2);

                // get last seen = message2
                expectGetLastSeenMessage(message2);

                // get messages between [message2, message2] returns []
                expectListMessagesByTypesAndIdRange(List.of(MESSAGE_TYPE_1, MESSAGE_TYPE_2), message2.getId(), message2.getId(), BATCH_SIZE,
                        List.of());
            }
        });

        MessagesConsumer consumer = new MessagesConsumer(CONSUMER_ID, List.of(handler1, handler2), BATCH_SIZE, database);
        consumer.consume();

        AssertionUtil.assertContainsLines(
                "INFO  OPERATION.MessagesConsumer - Found 2 new message(s) with types [message-type-1, message-type-2].\n"
                        + "INFO  OPERATION.MessagesConsumer - Started handling message {id: 1, type: 'message-type-1', description: 'null'}.\n"
                        + "INFO  OPERATION.MessagesConsumer - Finished handling message {id: 1, type: 'message-type-1', description: 'null'}.\n"
                        + "INFO  OPERATION.MessagesConsumer - Started handling message {id: 2, type: 'message-type-2', description: 'null'}.\n"
                        + "INFO  OPERATION.MessagesConsumer - Finished handling message {id: 2, type: 'message-type-2', description: 'null'}.\n"
                        + "INFO  OPERATION.MessagesConsumer - Handled 2 message(s). Successes: 2, failures: 0.\n"
                        + "INFO  OPERATION.MessagesConsumer - No new messages found with types [message-type-1, message-type-2].",
                logRecorder.getLogContent());
    }

    @Test
    public void testConsumeOneFailingOneSuccessfulMessage()
    {
        Message message1 = message(1L, MESSAGE_TYPE_1);
        Message message2 = message(2L, MESSAGE_TYPE_2);

        context.checking(new Expectations()
        {
            {
                // get newest = message2
                expectGetNewestMessagesByTypes(List.of(MESSAGE_TYPE_1, MESSAGE_TYPE_2), message2);

                // get last seen = null
                expectGetLastSeenMessage(null);

                // get messages between [null, message2] returns [message1, message2]
                expectListMessagesByTypesAndIdRange(List.of(MESSAGE_TYPE_1, MESSAGE_TYPE_2), null, message2.getId(), BATCH_SIZE,
                        List.of(message1, message2));

                // handle message1 fails with exception
                one(handler1).handleMessage(message1);
                RuntimeException testException = new RuntimeException();
                will(throwException(testException));

                // create last seen = message1
                expectCreateLastSeenMessage(message1);

                // handle message2
                one(handler2).handleMessage(message2);

                // update last seen = message2
                expectUpdateLastSeenMessage(message2);

                // get last seen = message2
                expectGetLastSeenMessage(message2);

                // get messages between [message2, message2] returns []
                expectListMessagesByTypesAndIdRange(List.of(MESSAGE_TYPE_1, MESSAGE_TYPE_2), message2.getId(), message2.getId(), BATCH_SIZE,
                        List.of());
            }
        });

        MessagesConsumer consumer = new MessagesConsumer(CONSUMER_ID, List.of(handler1, handler2), BATCH_SIZE, database);
        consumer.consume();

        AssertionUtil.assertContainsLines(
                "INFO  OPERATION.MessagesConsumer - Found 2 new message(s) with types [message-type-1, message-type-2].\n"
                        + "INFO  OPERATION.MessagesConsumer - Started handling message {id: 1, type: 'message-type-1', description: 'null'}.\n"
                        + "INFO  OPERATION.MessagesConsumer - Handling of message {id: 1, type: 'message-type-1', description: 'null'} has failed.\n"
                        + "INFO  OPERATION.MessagesConsumer - Started handling message {id: 2, type: 'message-type-2', description: 'null'}.\n"
                        + "INFO  OPERATION.MessagesConsumer - Finished handling message {id: 2, type: 'message-type-2', description: 'null'}.\n"
                        + "INFO  OPERATION.MessagesConsumer - Handled 2 message(s). Successes: 1, failures: 1.\n"
                        + "INFO  OPERATION.MessagesConsumer - No new messages found with types [message-type-1, message-type-2].",
                logRecorder.getLogContent());
    }

    @Test
    public void testConsumeTwoBatches()
    {
        Message message1 = message(1L, MESSAGE_TYPE_1);
        Message message2 = message(2L, MESSAGE_TYPE_2);
        Message message3 = message(3L, MESSAGE_TYPE_1);

        context.checking(new Expectations()
        {
            {
                // get newest = message2
                expectGetNewestMessagesByTypes(List.of(MESSAGE_TYPE_1, MESSAGE_TYPE_2), message3);

                // get last seen = null
                expectGetLastSeenMessage(null);

                // get messages between [null, message3] returns [message1, message2] with batch size = 2
                expectListMessagesByTypesAndIdRange(List.of(MESSAGE_TYPE_1, MESSAGE_TYPE_2), null, message3.getId(), 2,
                        List.of(message1, message2));

                // handle message1
                one(handler1).handleMessage(message1);

                // create last seen = message1
                expectCreateLastSeenMessage(message1);

                // handle message2
                one(handler2).handleMessage(message2);

                // update last seen = message2
                expectUpdateLastSeenMessage(message2);

                // get last seen = message2
                expectGetLastSeenMessage(message2);

                // get messages between [message2, message3] returns [message3]
                expectListMessagesByTypesAndIdRange(List.of(MESSAGE_TYPE_1, MESSAGE_TYPE_2), message2.getId(), message3.getId(), 2,
                        List.of(message3));

                // handle message3
                one(handler1).handleMessage(message3);

                // update last seen = message3
                expectUpdateLastSeenMessage(message3);

                // get last seen = message3
                expectGetLastSeenMessage(message3);

                // get messages between [message3, message3] returns []
                expectListMessagesByTypesAndIdRange(List.of(MESSAGE_TYPE_1, MESSAGE_TYPE_2), message3.getId(), message3.getId(), 2,
                        List.of());
            }
        });

        MessagesConsumer consumer = new MessagesConsumer(CONSUMER_ID, List.of(handler1, handler2), 2, database);
        consumer.consume();

        AssertionUtil.assertContainsLines(
                "INFO  OPERATION.MessagesConsumer - Found 2 new message(s) with types [message-type-1, message-type-2].\n"
                        + "INFO  OPERATION.MessagesConsumer - Started handling message {id: 1, type: 'message-type-1', description: 'null'}.\n"
                        + "INFO  OPERATION.MessagesConsumer - Finished handling message {id: 1, type: 'message-type-1', description: 'null'}.\n"
                        + "INFO  OPERATION.MessagesConsumer - Started handling message {id: 2, type: 'message-type-2', description: 'null'}.\n"
                        + "INFO  OPERATION.MessagesConsumer - Finished handling message {id: 2, type: 'message-type-2', description: 'null'}.\n"
                        + "INFO  OPERATION.MessagesConsumer - Handled 2 message(s). Successes: 2, failures: 0.\n"
                        + "INFO  OPERATION.MessagesConsumer - Found 1 new message(s) with types [message-type-1].\n"
                        + "INFO  OPERATION.MessagesConsumer - Started handling message {id: 3, type: 'message-type-1', description: 'null'}.\n"
                        + "INFO  OPERATION.MessagesConsumer - Finished handling message {id: 3, type: 'message-type-1', description: 'null'}.\n"
                        + "INFO  OPERATION.MessagesConsumer - Handled 1 message(s). Successes: 1, failures: 0.\n"
                        + "INFO  OPERATION.MessagesConsumer - No new messages found with types [message-type-1, message-type-2].",
                logRecorder.getLogContent());
    }

    private Message message(Long id, String type)
    {
        Message message = new Message();
        message.setId(id);
        message.setType(type);
        return message;
    }

    private void expectGetNewestMessagesByTypes(List<String> messageTypes, Message result)
    {
        context.checking(new Expectations()
        {{
            one(database).begin();
            one(messagesDAO).getNewestByTypes(messageTypes);
            will(returnValue(result));
            one(database).commit();
        }});
    }

    private void expectGetLastSeenMessage(Message message)
    {
        context.checking(new Expectations()
        {
            {
                LastSeenMessage lastSeenMessage = null;

                if (message != null)
                {
                    lastSeenMessage = new LastSeenMessage();
                    lastSeenMessage.setConsumerId(CONSUMER_ID);
                    lastSeenMessage.setLastSeenMessageId(message.getId());
                }

                one(database).begin();
                one(lastSeenMessagesDAO).getByConsumerId(CONSUMER_ID);
                will(returnValue(lastSeenMessage));
                one(database).commit();
            }
        });
    }

    private void expectListMessagesByTypesAndIdRange(List<String> messageTypes, Long minMessageId, Long maxMessageId, int batchSize,
            List<Message> result)
    {
        context.checking(new Expectations()
        {
            {
                one(database).begin();
                one(messagesDAO).listByTypesAndIdRange(messageTypes, minMessageId, maxMessageId, batchSize);
                will(returnValue(result));
                one(database).commit();
            }
        });
    }

    private void expectCreateLastSeenMessage(Message message)
    {
        context.checking(new Expectations()
        {
            {
                LastSeenMessage lastSeenMessage = new LastSeenMessage();
                lastSeenMessage.setConsumerId(CONSUMER_ID);
                lastSeenMessage.setLastSeenMessageId(message.getId());

                one(database).begin();
                one(lastSeenMessagesDAO).create(with(new LastSeenMessageMatcher(lastSeenMessage)));
                one(messagesDAO).update(with(new MessageMatcher(message)));
                one(database).commit();
            }
        });
    }

    private void expectUpdateLastSeenMessage(Message message)
    {
        context.checking(new Expectations()
        {
            {
                LastSeenMessage lastSeenMessage = new LastSeenMessage();
                lastSeenMessage.setConsumerId(CONSUMER_ID);
                lastSeenMessage.setLastSeenMessageId(message.getId());

                one(database).begin();
                one(lastSeenMessagesDAO).update(with(new LastSeenMessageMatcher(lastSeenMessage)));
                one(messagesDAO).update(with(new MessageMatcher(message)));
                one(database).commit();
            }
        });
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
