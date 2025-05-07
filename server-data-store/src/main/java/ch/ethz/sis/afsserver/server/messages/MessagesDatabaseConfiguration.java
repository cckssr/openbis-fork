package ch.ethz.sis.afsserver.server.messages;

import java.util.Properties;

import ch.ethz.sis.afsserver.server.common.DatabaseConfiguration;
import ch.ethz.sis.shared.startup.Configuration;
import ch.systemsx.cisd.common.properties.ExtendedProperties;

public class MessagesDatabaseConfiguration
{

    private static volatile DatabaseConfiguration instance;

    private static volatile Configuration configuration;

    public static DatabaseConfiguration getInstance(Configuration configuration)
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
                        instance = new DatabaseConfiguration(new Configuration(databaseProperties));
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

    private MessagesDatabaseConfiguration()
    {
    }

}
