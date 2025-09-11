package ch.ethz.sis.foldermonitor;

import java.nio.file.Path;
import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.Test;

import ch.systemsx.cisd.common.exceptions.ConfigurationFailureException;

public class FolderMonitorConfigurationTest
{

    @Test
    public void testMarkerFileConfiguration()
    {
        Properties properties = createCorrectMarkerFileProperties();
        FolderMonitorConfiguration configuration = new FolderMonitorConfiguration(properties);
        Assert.assertEquals(configuration.getFolder(), Path.of("test-folder"));
        Assert.assertEquals(configuration.getMode(), FolderMonitorMode.MARKER_FILE);
        Assert.assertEquals(configuration.getCheckingInterval(), Long.valueOf(1000));
        Assert.assertEquals(configuration.getTaskClass(), CorrectTask.class);

        Properties taskProperties = configuration.getTaskProperties();
        Assert.assertEquals(taskProperties.size(), 3);
        Assert.assertEquals(taskProperties.get(FolderMonitorConfiguration.PROPERTY_CLASS), CorrectTask.class.getName());
        Assert.assertEquals(taskProperties.get("a"), "1");
        Assert.assertEquals(taskProperties.get("b"), "abc");
    }

    @Test
    public void testQuietPeriodConfiguration()
    {
        Properties properties = createCorrectQuietPeriodProperties();
        FolderMonitorConfiguration configuration = new FolderMonitorConfiguration(properties);
        Assert.assertEquals(configuration.getFolder(), Path.of("test-folder"));
        Assert.assertEquals(configuration.getMode(), FolderMonitorMode.QUIET_PERIOD);
        Assert.assertEquals(configuration.getCheckingInterval(), Long.valueOf(10));
        Assert.assertEquals(configuration.getQuietPeriod(), Long.valueOf(60000));
        Assert.assertEquals(configuration.getTaskClass(), CorrectTask.class);

        Properties taskProperties = configuration.getTaskProperties();
        Assert.assertEquals(taskProperties.size(), 3);
        Assert.assertEquals(taskProperties.get(FolderMonitorConfiguration.PROPERTY_CLASS), CorrectTask.class.getName());
        Assert.assertEquals(taskProperties.get("a"), "1");
        Assert.assertEquals(taskProperties.get("b"), "abc");
    }

    @Test
    public void testMissingFolder()
    {
        Properties properties = createCorrectMarkerFileProperties();
        properties.remove(FolderMonitorConfiguration.PROPERTY_FOLDER);
        try
        {
            new FolderMonitorConfiguration(properties);
            Assert.fail();
        } catch (ConfigurationFailureException e)
        {
            Assert.assertEquals(e.getMessage(),
                    "Given key 'folder' not found in properties '[checking-interval, task.class, task.b, task.a, mode]'");
        }
    }

    @Test
    public void testMissingMode()
    {
        Properties properties = createCorrectMarkerFileProperties();
        properties.remove(FolderMonitorConfiguration.PROPERTY_MODE);
        try
        {
            new FolderMonitorConfiguration(properties);
            Assert.fail();
        } catch (ConfigurationFailureException e)
        {
            Assert.assertEquals(e.getMessage(),
                    "Given key 'mode' not found in properties '[checking-interval, task.class, folder, task.b, task.a]'");
        }
    }

    @Test
    public void testMissingCheckingInterval()
    {
        Properties properties = createCorrectQuietPeriodProperties();
        properties.remove(FolderMonitorConfiguration.PROPERTY_CHECKING_INTERVAL);
        try
        {
            new FolderMonitorConfiguration(properties);
            Assert.fail();
        } catch (ConfigurationFailureException e)
        {
            Assert.assertEquals(e.getMessage(),
                    "Given key 'checking-interval' not found in properties '[task.class, folder, quiet-period, task.b, task.a, mode]'");
        }
    }

    @Test
    public void testMissingQuietPeriod()
    {
        Properties properties = createCorrectQuietPeriodProperties();
        properties.remove(FolderMonitorConfiguration.PROPERTY_QUIET_PERIOD);
        try
        {
            new FolderMonitorConfiguration(properties);
            Assert.fail();
        } catch (ConfigurationFailureException e)
        {
            Assert.assertEquals(e.getMessage(),
                    "Given key 'quiet-period' not found in properties '[checking-interval, task.class, folder, task.b, task.a, mode]'");
        }
    }

    @Test
    public void testMissingTaskClass()
    {
        Properties properties = createCorrectQuietPeriodProperties();
        properties.remove(FolderMonitorConfiguration.PROPERTY_TASK + "." + FolderMonitorConfiguration.PROPERTY_CLASS);
        try
        {
            new FolderMonitorConfiguration(properties);
            Assert.fail();
        } catch (ConfigurationFailureException e)
        {
            Assert.assertEquals(e.getMessage(),
                    "Given key 'task.class' not found in properties '[checking-interval, folder, quiet-period, task.b, task.a, mode]'");
        }
    }

    @Test
    public void testIncorrectMode()
    {
        Properties properties = createCorrectMarkerFileProperties();
        properties.setProperty(FolderMonitorConfiguration.PROPERTY_MODE, "incorrect mode");
        try
        {
            new FolderMonitorConfiguration(properties);
            Assert.fail();
        } catch (ConfigurationFailureException e)
        {
            Assert.assertEquals(e.getMessage(), "Mode 'incorrect mode' is invalid. Supported values: [QUIET_PERIOD, MARKER_FILE]");
        }
    }

    @Test
    public void testIncorrectCheckingInterval()
    {
        Properties properties = createCorrectMarkerFileProperties();
        properties.setProperty(FolderMonitorConfiguration.PROPERTY_CHECKING_INTERVAL, "incorrect checking interval");
        try
        {
            new FolderMonitorConfiguration(properties);
            Assert.fail();
        } catch (ConfigurationFailureException e)
        {
            Assert.assertEquals(e.getMessage(), "Checking interval 'incorrect checking interval' is invalid");
        }
    }

    @Test
    public void testIncorrectQuietPeriod()
    {
        Properties properties = createCorrectQuietPeriodProperties();
        properties.setProperty(FolderMonitorConfiguration.PROPERTY_QUIET_PERIOD, "incorrect quiet period");
        try
        {
            new FolderMonitorConfiguration(properties);
            Assert.fail();
        } catch (ConfigurationFailureException e)
        {
            Assert.assertEquals(e.getMessage(), "Quiet period 'incorrect quiet period' is invalid");
        }
    }

    @Test
    public void testIncorrectTaskClass()
    {
        Properties properties = createCorrectMarkerFileProperties();
        properties.setProperty(FolderMonitorConfiguration.PROPERTY_TASK + "." + FolderMonitorConfiguration.PROPERTY_CLASS,
                IncorrectTask.class.getName());
        try
        {
            new FolderMonitorConfiguration(properties);
            Assert.fail();
        } catch (ConfigurationFailureException e)
        {
            Assert.assertEquals(e.getMessage(),
                    "Task class 'ch.ethz.sis.foldermonitor.FolderMonitorConfigurationTest$IncorrectTask' does not implement 'ch.ethz.sis.foldermonitor.FolderMonitorTask' interface");
        }
    }

    public static class CorrectTask implements FolderMonitorTask
    {

        @Override public void configure(final Properties properties)
        {

        }

        @Override public void process(final Path incoming)
        {

        }
    }

    public static class IncorrectTask
    {

    }

    public Properties createCorrectMarkerFileProperties()
    {
        Properties properties = new Properties();
        properties.setProperty(FolderMonitorConfiguration.PROPERTY_FOLDER, "test-folder");
        properties.setProperty(FolderMonitorConfiguration.PROPERTY_MODE, "MARKER_FILE");
        properties.setProperty(FolderMonitorConfiguration.PROPERTY_CHECKING_INTERVAL, "1s");
        properties.setProperty(FolderMonitorConfiguration.PROPERTY_TASK + "." + FolderMonitorConfiguration.PROPERTY_CLASS,
                CorrectTask.class.getName());
        properties.setProperty(FolderMonitorConfiguration.PROPERTY_TASK + ".a", "1");
        properties.setProperty(FolderMonitorConfiguration.PROPERTY_TASK + ".b", "abc");
        return properties;
    }

    private Properties createCorrectQuietPeriodProperties()
    {
        Properties properties = new Properties();
        properties.setProperty(FolderMonitorConfiguration.PROPERTY_FOLDER, "test-folder");
        properties.setProperty(FolderMonitorConfiguration.PROPERTY_MODE, "QUIET_PERIOD");
        properties.setProperty(FolderMonitorConfiguration.PROPERTY_CHECKING_INTERVAL, "10ms");
        properties.setProperty(FolderMonitorConfiguration.PROPERTY_QUIET_PERIOD, "1min");
        properties.setProperty(FolderMonitorConfiguration.PROPERTY_TASK + "." + FolderMonitorConfiguration.PROPERTY_CLASS,
                CorrectTask.class.getName());
        properties.setProperty(FolderMonitorConfiguration.PROPERTY_TASK + ".a", "1");
        properties.setProperty(FolderMonitorConfiguration.PROPERTY_TASK + ".b", "abc");
        return properties;
    }

}
