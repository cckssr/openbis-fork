package ch.ethz.sis.afsserver.server.messages;

import ch.ethz.sis.messages.db.Message;
import ch.ethz.sis.messages.db.MessagesDatabase;

public class MessagesDatabaseFacade implements IMessagesDatabaseFacade
{

    private final MessagesDatabase messagesDatabase;

    public MessagesDatabaseFacade(MessagesDatabase messagesDatabase)
    {
        this.messagesDatabase = messagesDatabase;
    }

    @Override public void create(Message message)
    {
        messagesDatabase.begin();
        messagesDatabase.getMessagesDAO().create(message);
        messagesDatabase.commit();
    }

    public MessagesDatabase getMessagesDatabase()
    {
        return messagesDatabase;
    }
}
