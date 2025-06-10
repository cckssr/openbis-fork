package ch.ethz.sis.openbis.systemtests.common;

import java.util.Properties;

import ch.systemsx.cisd.common.maintenance.IMaintenanceTask;
import ch.systemsx.cisd.etlserver.path.PathInfoDatabaseFeedingTask;

public class TestPathInfoDatabaseFeedingTask implements IMaintenanceTask
{

    private static String pluginName;

    private static PathInfoDatabaseFeedingTask task;

    @Override public synchronized void setUp(final String pluginName, final Properties properties)
    {
        TestPathInfoDatabaseFeedingTask.pluginName = pluginName;
        task = new PathInfoDatabaseFeedingTask();
        task.setUp(pluginName, properties);
    }

    @Override public void execute()
    {
        // do not run periodically - only on request with executeOnce
    }

    public static void executeOnce()
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
    }
}
