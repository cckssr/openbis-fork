package ch.ethz.sis.afsserver.server.messages;

import java.util.List;

import javax.sql.DataSource;

import ch.ethz.sis.messages.db.Message;
import ch.ethz.sis.messages.db.MessagesDatabase;

public class MessagesDatabaseFacade implements IMessagesDatabaseFacade
{

    private final DataSource dataSource;

    public MessagesDatabaseFacade(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    @Override public void create(Message message)
    {
        MessagesDatabase messagesDatabase = new MessagesDatabase(dataSource);
        messagesDatabase.begin();
        messagesDatabase.getMessagesDAO().create(message);
        messagesDatabase.commit();
    }

    @Override public List<Message> listByTypesNotConsumed(final List<String> messageTypes)
    {
        MessagesDatabase messagesDatabase = new MessagesDatabase(dataSource);
        messagesDatabase.begin();
        List<Message> messages = messagesDatabase.getMessagesDAO().listByTypesNotConsumed(messageTypes);
        messagesDatabase.commit();
        return messages;
    }
}
