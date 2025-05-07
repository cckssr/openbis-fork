package ch.ethz.sis.afsserver.server.messages;

import ch.ethz.sis.afsserver.server.common.DatabaseConfiguration;
import ch.ethz.sis.messagesdb.Message;
import ch.ethz.sis.messagesdb.MessagesDAO;
import ch.ethz.sis.shared.startup.Configuration;

public class MessagesDatabaseFacade
{

    private final MessagesDAO messagesDatabaseDAO;

    public MessagesDatabaseFacade(Configuration configuration)
    {
        final DatabaseConfiguration messagesDatabaseConfiguration = MessagesDatabaseConfiguration.getInstance(configuration);

        if (messagesDatabaseConfiguration != null)
        {
            messagesDatabaseDAO = new MessagesDAO(messagesDatabaseConfiguration.getDataSource());
        } else
        {
            throw new RuntimeException("Messages database not configured");
        }
    }

    public void create(Message message)
    {
        messagesDatabaseDAO.create(message);
    }
}
