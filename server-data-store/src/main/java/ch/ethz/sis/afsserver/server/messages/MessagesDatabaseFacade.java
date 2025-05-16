package ch.ethz.sis.afsserver.server.messages;

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

}
