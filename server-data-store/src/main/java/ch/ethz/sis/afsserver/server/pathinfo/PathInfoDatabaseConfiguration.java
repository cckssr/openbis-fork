package ch.ethz.sis.afsserver.server.pathinfo;

import java.util.Properties;

import ch.ethz.sis.afsserver.server.common.DatabaseConfiguration;
import ch.ethz.sis.shared.startup.Configuration;
import ch.systemsx.cisd.common.properties.ExtendedProperties;

public class PathInfoDatabaseConfiguration
{

    private static volatile DatabaseConfiguration instance;

    public static DatabaseConfiguration getInstance(Configuration configuration)
    {
        if (instance == null)
        {
            synchronized (PathInfoDatabaseConfiguration.class)
            {
                if (instance == null)
                {
                    Properties databaseProperties = ExtendedProperties.getSubset(configuration.getProperties(), "pathInfoDB.", true);
                    instance = new DatabaseConfiguration(new Configuration(databaseProperties));
                }
            }
        }

        return instance;
    }

    private PathInfoDatabaseConfiguration()
    {
    }

}
