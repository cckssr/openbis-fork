package ch.ethz.sis.afsserver.server.pathinfo;

import java.util.Properties;

import ch.ethz.sis.afsserver.server.common.DatabaseConfiguration;
import ch.ethz.sis.shared.startup.Configuration;
import ch.systemsx.cisd.common.properties.ExtendedProperties;

public class PathInfoDatabaseConfiguration
{

    private static volatile DatabaseConfiguration instance;

    private static volatile boolean configured;

    public static DatabaseConfiguration getInstance(Configuration configuration)
    {
        if (!configured)
        {
            synchronized (PathInfoDatabaseConfiguration.class)
            {
                if (!configured)
                {
                    Properties databaseProperties = ExtendedProperties.getSubset(configuration.getProperties(), "pathInfoDB.", true);
                    if (!databaseProperties.isEmpty())
                    {
                        instance = new DatabaseConfiguration(new Configuration(databaseProperties));
                    }
                    configured = true;
                }
            }
        }

        return instance;
    }

    private PathInfoDatabaseConfiguration()
    {
    }

}
