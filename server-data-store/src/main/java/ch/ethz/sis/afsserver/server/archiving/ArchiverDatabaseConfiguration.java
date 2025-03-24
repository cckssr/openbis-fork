package ch.ethz.sis.afsserver.server.archiving;

import java.util.Properties;

import ch.ethz.sis.afsserver.server.common.DatabaseConfiguration;
import ch.ethz.sis.shared.startup.Configuration;
import ch.systemsx.cisd.common.properties.ExtendedProperties;

public class ArchiverDatabaseConfiguration
{

    private static volatile DatabaseConfiguration instance;

    private static volatile Configuration configuration;

    public static DatabaseConfiguration getInstance(Configuration configuration)
    {
        if (ArchiverDatabaseConfiguration.configuration != configuration)
        {
            synchronized (ArchiverDatabaseConfiguration.class)
            {
                if (ArchiverDatabaseConfiguration.configuration != configuration)
                {
                    Properties databaseProperties = ExtendedProperties.getSubset(configuration.getProperties(), "archiverDB.", true);

                    if (!databaseProperties.isEmpty())
                    {
                        instance = new DatabaseConfiguration(new Configuration(databaseProperties));
                    } else
                    {
                        instance = null;
                    }

                    ArchiverDatabaseConfiguration.configuration = configuration;
                }
            }
        }

        return instance;
    }

    private ArchiverDatabaseConfiguration()
    {
    }

}
