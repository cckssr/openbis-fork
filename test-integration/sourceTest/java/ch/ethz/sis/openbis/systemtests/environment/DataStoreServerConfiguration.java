package ch.ethz.sis.openbis.systemtests.environment;

import java.nio.file.Path;
import java.util.Properties;

public class DataStoreServerConfiguration
{

    private Properties serviceProperties;

    public void loadServiceProperties(Path servicePropertiesPath)
    {
        serviceProperties = IntegrationTestUtil.loadProperties(servicePropertiesPath);
    }

    public void setServiceProperties(final Properties serviceProperties)
    {
        this.serviceProperties = serviceProperties;
    }

    public Properties getServiceProperties()
    {
        return serviceProperties;
    }

}
