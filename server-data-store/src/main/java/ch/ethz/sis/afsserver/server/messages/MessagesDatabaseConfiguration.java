package ch.ethz.sis.afsserver.server.messages;

import java.util.Properties;

import ch.ethz.sis.afsserver.server.common.DatabaseConfiguration;
import ch.ethz.sis.shared.startup.Configuration;
import ch.systemsx.cisd.common.properties.ExtendedProperties;

public class MessagesDatabaseConfiguration extends DatabaseConfiguration
{

    private static volatile MessagesDatabaseConfiguration instance;

    private static volatile Configuration configuration;

    private final MessagesDatabaseFacade messagesDatabaseFacade;

    public static boolean hasInstance(Configuration configuration)
    {
        try
        {
            getInstance(configuration);
            return true;
        } catch (DatabaseNotConfiguredException e)
        {
            return false;
        }
    }

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
                        throw new DatabaseNotConfiguredException();
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
        messagesDatabaseFacade = new MessagesDatabaseFacade(getDataSource());
    }

    public MessagesDatabaseFacade getMessagesDatabaseFacade()
    {
        return messagesDatabaseFacade;
    }

}
