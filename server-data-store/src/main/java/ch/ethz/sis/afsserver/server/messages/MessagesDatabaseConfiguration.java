package ch.ethz.sis.afsserver.server.messages;

import java.util.Properties;

import ch.ethz.sis.afsserver.server.common.DatabaseConfiguration;
import ch.ethz.sis.messages.db.MessagesDatabase;
import ch.ethz.sis.shared.startup.Configuration;
import ch.systemsx.cisd.common.properties.ExtendedProperties;

public class MessagesDatabaseConfiguration extends DatabaseConfiguration
{

    private static volatile MessagesDatabaseConfiguration instance;

    private static volatile Configuration configuration;

    private final MessagesDatabaseFacade messagesDatabaseFacade;

    public static MessagesDatabaseConfiguration getInstance(Configuration configuration)
    {
        if (MessagesDatabaseConfiguration.configuration != configuration)
        {
            synchronized (MessagesDatabaseConfiguration.class)
            {
                if (MessagesDatabaseConfiguration.configuration != configuration)
                {
                    Properties databaseProperties = ExtendedProperties.getSubset(configuration.getProperties(), "messagesDB.", true);

                    if (!databaseProperties.isEmpty())
                    {
                        instance = new MessagesDatabaseConfiguration(new Configuration(databaseProperties));
                    } else
                    {
                        instance = null;
                    }

                    MessagesDatabaseConfiguration.configuration = configuration;
                }
            }
        }

        return instance;
    }

    private MessagesDatabaseConfiguration(Configuration configuration)
    {
        super(configuration);

        DatabaseConfiguration databaseConfiguration = MessagesDatabaseConfiguration.getInstance(configuration);

        if (databaseConfiguration != null)
        {
            messagesDatabaseFacade = new MessagesDatabaseFacade(new MessagesDatabase(databaseConfiguration.getDataSource()));
        } else
        {
            throw new RuntimeException("Messages database not configured");
        }
    }

    public MessagesDatabaseFacade getMessagesDatabaseFacade()
    {
        return messagesDatabaseFacade;
    }
}
