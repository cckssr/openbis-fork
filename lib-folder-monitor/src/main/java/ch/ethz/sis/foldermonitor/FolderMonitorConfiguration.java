package ch.ethz.sis.foldermonitor;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;

import ch.systemsx.cisd.common.exceptions.ConfigurationFailureException;
import ch.systemsx.cisd.common.properties.PropertyParametersUtil;
import ch.systemsx.cisd.common.properties.PropertyUtils;
import ch.systemsx.cisd.common.time.DateTimeUtils;
import lombok.Getter;

public class FolderMonitorConfiguration
{

    public static final String PROPERTY_FOLDER = "folder";

    public static final String PROPERTY_MODE = "mode";

    public static final String PROPERTY_CHECKING_INTERVAL = "checking-interval";

    public static final String PROPERTY_QUIET_PERIOD = "quiet-period";

    public static final String PROPERTY_TASK = "task";

    public static final String PROPERTY_CLASS = "class";

    @Getter
    private final Path folder;

    @Getter
    private final FolderMonitorMode mode;

    @Getter
    private final Long checkingInterval;

    @Getter
    private final Long quietPeriod;

    @Getter
    private final Class<?> taskClass;

    @Getter
    private final Properties taskProperties;

    public FolderMonitorConfiguration(Properties properties)
    {
        String folderString = PropertyUtils.getMandatoryProperty(properties, PROPERTY_FOLDER);

        folder = Path.of(folderString);

        String modeString = PropertyUtils.getMandatoryProperty(properties, PROPERTY_MODE);

        try
        {
            mode = FolderMonitorMode.valueOf(modeString);
        } catch (Exception e)
        {
            throw new ConfigurationFailureException(
                    "Mode '" + modeString + "' is invalid. Supported values: " + Arrays.toString(FolderMonitorMode.values()), e);
        }

        String checkingIntervalString = PropertyUtils.getMandatoryProperty(properties, PROPERTY_CHECKING_INTERVAL);

        try
        {
            checkingInterval = DateTimeUtils.parseDurationToMillis(checkingIntervalString);
        } catch (Exception e)
        {
            throw new ConfigurationFailureException("Checking interval '" + checkingIntervalString + "' is invalid", e);
        }

        if (FolderMonitorMode.QUIET_PERIOD.equals(mode))
        {
            String quietPeriodString = PropertyUtils.getMandatoryProperty(properties, PROPERTY_QUIET_PERIOD);

            try
            {
                quietPeriod = DateTimeUtils.parseDurationToMillis(quietPeriodString);
            } catch (Exception e)
            {
                throw new ConfigurationFailureException("Quiet period '" + quietPeriodString + "' is invalid", e);
            }
        } else
        {
            quietPeriod = null;
        }

        String taskClassName = PropertyUtils.getMandatoryProperty(properties, PROPERTY_TASK + "." + PROPERTY_CLASS);

        try
        {
            taskClass = Class.forName(taskClassName);
        } catch (Exception e)
        {
            throw new ConfigurationFailureException("Task class '" + taskClassName + "' not found", e);
        }

        if (!FolderMonitorTask.class.isAssignableFrom(taskClass))
        {
            throw new ConfigurationFailureException(
                    "Task class '" + taskClassName + "' does not implement '" + FolderMonitorTask.class.getName() + "' interface");
        }

        taskProperties = PropertyParametersUtil.extractSingleSectionProperties(properties, PROPERTY_TASK, false).getProperties();
    }

}
