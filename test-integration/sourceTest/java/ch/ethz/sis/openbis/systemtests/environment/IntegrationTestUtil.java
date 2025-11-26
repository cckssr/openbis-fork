package ch.ethz.sis.openbis.systemtests.environment;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.Properties;

public class IntegrationTestUtil
{

    public static Properties loadProperties(final Path propertiesPath)
    {
        try
        {
            Properties properties = new Properties();
            properties.load(new FileInputStream(propertiesPath.toFile()));
            return properties;
        } catch (Exception e)
        {
            throw new RuntimeException("Loading properties from path: " + propertiesPath + " failed.", e);
        }
    }
}
