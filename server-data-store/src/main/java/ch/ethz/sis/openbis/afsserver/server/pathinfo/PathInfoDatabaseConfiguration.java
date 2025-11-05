package ch.ethz.sis.openbis.afsserver.server.pathinfo;

import java.util.Properties;

import ch.ethz.sis.openbis.afsserver.server.common.DatabaseConfiguration;
import ch.ethz.sis.shared.startup.Configuration;
import ch.systemsx.cisd.common.properties.ExtendedProperties;

public class PathInfoDatabaseConfiguration extends DatabaseConfiguration
{

    private static volatile PathInfoDatabaseConfiguration instance;

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

    public static PathInfoDatabaseConfiguration getInstance(Configuration configuration)
    {
        if (PathInfoDatabaseConfiguration.configuration != configuration)
        {
            synchronized (PathInfoDatabaseConfiguration.class)
            {
                if (PathInfoDatabaseConfiguration.configuration != configuration)
                {
                    Properties databaseProperties = ExtendedProperties.getSubset(configuration.getProperties(), "pathInfoDB.", true);

                    if (!databaseProperties.isEmpty())
                    {
                        instance = new PathInfoDatabaseConfiguration(databaseProperties);
                    } else
                    {
                        throw new DatabaseNotConfiguredException();
                    }

                    PathInfoDatabaseConfiguration.configuration = configuration;
                }
            }
        }

        return instance;
    }

    private PathInfoDatabaseConfiguration(Properties properties)
    {
        super(properties);
    }

}
