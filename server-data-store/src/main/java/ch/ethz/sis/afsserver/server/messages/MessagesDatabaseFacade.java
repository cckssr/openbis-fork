package ch.ethz.sis.afsserver.server.messages;

import ch.ethz.sis.afsserver.server.common.DatabaseConfiguration;
import ch.ethz.sis.messages.db.Message;
import ch.ethz.sis.messages.db.MessagesDatabase;
import ch.ethz.sis.shared.startup.Configuration;

public class MessagesDatabaseFacade
{

    private final MessagesDatabase messagesDatabase;

    public MessagesDatabaseFacade(Configuration configuration)
    {
        final DatabaseConfiguration messagesDatabaseConfiguration = MessagesDatabaseConfiguration.getInstance(configuration);

        if (messagesDatabaseConfiguration != null)
        {
            messagesDatabase = new MessagesDatabase(messagesDatabaseConfiguration.getDataSource());
        } else
        {
            throw new RuntimeException("Messages database not configured");
        }
    }

    public void create(Message message)
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
