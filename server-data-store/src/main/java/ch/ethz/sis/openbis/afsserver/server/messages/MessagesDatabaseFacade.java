package ch.ethz.sis.openbis.afsserver.server.messages;

import java.util.List;

import javax.sql.DataSource;

import ch.ethz.sis.messages.db.Message;
import ch.ethz.sis.messages.db.MessagesDatabase;
import ch.ethz.sis.messages.db.MessagesDatabaseUtil;

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
        MessagesDatabaseUtil.execute(messagesDatabase, () ->
        {
            messagesDatabase.getMessagesDAO().create(message);
            return null;
        });
    }

    @Override public List<Message> listByTypesNotConsumed(final List<String> messageTypes)
    {
        MessagesDatabase messagesDatabase = new MessagesDatabase(dataSource);
        return MessagesDatabaseUtil.execute(messagesDatabase, () -> messagesDatabase.getMessagesDAO().listByTypesNotConsumed(messageTypes));
    }
}
