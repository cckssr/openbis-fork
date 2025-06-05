package ch.ethz.sis.openbis.systemtests.messages;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import ch.ethz.sis.afsserver.server.messages.MessagesConsumerMaintenanceTask;
import ch.systemsx.cisd.common.maintenance.IMaintenanceTask;

public class TestMessagesConsumerMaintenanceTask implements IMaintenanceTask
{

    private static final List<Configuration> configurations = new ArrayList<>();

    @Override public void setUp(final String pluginName, final Properties properties)
    {
        synchronized (configurations)
        {
            configurations.add(new Configuration(pluginName, properties));
        }
    }

    @Override public void execute()
    {
        // do not run periodically - only on request with executeOnce
    }

    public static void executeOnce(String pluginName)
    {
        for (Configuration configuration : configurations)
        {
            if (Objects.equals(configuration.getPluginName(), pluginName))
            {
                MessagesConsumerMaintenanceTask task = new MessagesConsumerMaintenanceTask();
                task.setUp(configuration.getPluginName(), configuration.getProperties());
                task.execute();
                return;
            }
        }

        throw new RuntimeException("Plugin with name '" + pluginName + "' not found.");
    }

    private static class Configuration
    {
        private final String pluginName;

        private final Properties properties;

        public Configuration(String pluginName, Properties properties)
        {
            this.pluginName = pluginName;
            this.properties = properties;
        }

        public String getPluginName()
        {
            return pluginName;
        }

        public Properties getProperties()
        {
            return properties;
        }
    }
}
