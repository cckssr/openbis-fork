package ch.ethz.sis.afsserver.server.archiving;

import java.util.Properties;

import ch.ethz.sis.afsserver.server.common.DatabaseConfiguration;
import ch.ethz.sis.shared.startup.Configuration;
import ch.systemsx.cisd.common.properties.ExtendedProperties;

public class ArchiverDatabaseConfiguration extends DatabaseConfiguration
{

    private static volatile ArchiverDatabaseConfiguration instance;

    private static volatile Configuration configuration;

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

    public static ArchiverDatabaseConfiguration getInstance(Configuration configuration)
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
                        instance = new ArchiverDatabaseConfiguration(new Configuration(databaseProperties));
                    } else
                    {
                        throw new DatabaseNotConfiguredException();
                    }

                    ArchiverDatabaseConfiguration.configuration = configuration;
                }
            }
        }

        return instance;
    }

    private ArchiverDatabaseConfiguration(Configuration configuration)
    {
        super(configuration);
    }

}
