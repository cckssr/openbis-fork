package ch.ethz.sis.openbis.systemtests.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import ch.ethz.sis.openbis.afsserver.server.messages.MessagesConsumerMaintenanceTask;
import ch.systemsx.cisd.common.maintenance.IMaintenanceTask;

public class TestMessagesConsumerMaintenanceTask implements IMaintenanceTask
{

    private static final Map<String, MessagesConsumerMaintenanceTask> tasks = new HashMap<>();

    @Override public synchronized void setUp(final String pluginName, final Properties properties)
    {
        MessagesConsumerMaintenanceTask task = new MessagesConsumerMaintenanceTask();
        task.setUp(pluginName, properties);
        tasks.put(pluginName, task);
    }

    @Override public void execute()
    {
        // do not run periodically - only on request with executeOnce
    }

    public static void executeOnce(String pluginName)
    {
        MessagesConsumerMaintenanceTask task = tasks.get(pluginName);

        if (task != null)
        {
            Thread thread = new Thread(task::execute, pluginName);
            try
            {
                thread.start();
                thread.join();
            } catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        } else
        {
            throw new RuntimeException("Plugin with name '" + pluginName + "' not found.");
        }
    }
}
