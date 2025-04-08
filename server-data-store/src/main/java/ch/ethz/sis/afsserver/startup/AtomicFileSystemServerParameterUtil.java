package ch.ethz.sis.afsserver.startup;

import java.util.Arrays;
import java.util.List;

import ch.ethz.sis.afsjson.JsonObjectMapper;
import ch.ethz.sis.shared.io.IOUtils;
import ch.ethz.sis.shared.startup.Configuration;

public class AtomicFileSystemServerParameterUtil
{

    public static String getStorageRoot(Configuration configuration)
    {
        return getStringParameter(configuration, AtomicFileSystemServerParameter.storageRoot, true);
    }

    public static String getStorageUuid(Configuration configuration)
    {
        return getStringParameter(configuration, AtomicFileSystemServerParameter.storageUuid, true);
    }

    public static Integer getStorageIncomingShareId(Configuration configuration)
    {
        Integer storageIncomingShareId = getIntegerParameter(configuration, AtomicFileSystemServerParameter.storageIncomingShareId, false);
        String storageRoot = getStorageRoot(configuration);
        List<Integer> shares = Arrays.asList(IOUtils.getShares(storageRoot));

        if (storageIncomingShareId == null)
        {
            if (shares.isEmpty())
            {
                throw new RuntimeException(
                        "No shares were found in the storage root '" + storageRoot + "' defined in '" + AtomicFileSystemServerParameter.storageRoot
                                + "' configuration parameter.");
            } else
            {
                storageIncomingShareId = shares.get(0);
            }
        } else if (!shares.contains(storageIncomingShareId))
        {
            throw new RuntimeException("Share '" + storageIncomingShareId + "' defined in '" + AtomicFileSystemServerParameter.storageIncomingShareId
                    + "' configuration parameter does not exist in the storage root '" + storageRoot + "'.");
        }

        return storageIncomingShareId;
    }

    public static Integer getLockingTimeoutInSeconds(final Configuration configuration)
    {
        Integer timeout = getIntegerParameter(configuration, AtomicFileSystemServerParameter.lockingTimeoutInSeconds, false);
        return timeout != null ? timeout : 5;
    }

    public static Integer getLockingWaitingIntervalInMillis(final Configuration configuration)
    {
        Integer interval = getIntegerParameter(configuration, AtomicFileSystemServerParameter.lockingWaitingIntervalInMillis, false);
        return interval != null ? interval : 100;
    }

    public static JsonObjectMapper getJsonObjectMapper(Configuration configuration) throws Exception
    {
        getStringParameter(configuration, AtomicFileSystemServerParameter.jsonObjectMapperClass, true);
        return configuration.getInstance(AtomicFileSystemServerParameter.jsonObjectMapperClass);
    }

    public static String getInteractiveSessionKey(Configuration configuration)
    {
        return getStringParameter(configuration, AtomicFileSystemServerParameter.apiServerInteractiveSessionKey, false);
    }

    public static <E extends Enum<E>, I> I getInstanceParameter(Configuration configuration, E parameter, boolean mandatory)
    {
        String parameterValue = configuration.getStringProperty(parameter);

        if (parameterValue == null || parameterValue.isBlank())
        {
            if (mandatory)
            {
                throw new RuntimeException("Configuration parameter '" + parameter + "' cannot be null or empty.");
            } else
            {
                return null;
            }
        } else
        {
            try
            {
                return configuration.getInstance(parameter);
            } catch (Exception e)
            {
                throw new RuntimeException("Could not create an instance of class specified in configuration parameter '" + parameter + "'.", e);
            }
        }
    }

    public static <E extends Enum<E>> String getStringParameter(Configuration configuration, E parameter, boolean mandatory)
    {
        String parameterValue = configuration.getStringProperty(parameter);

        if (parameterValue == null || parameterValue.isBlank())
        {
            if (mandatory)
            {
                throw new RuntimeException("Configuration parameter '" + parameter + "' cannot be null or empty.");
            } else
            {
                return null;
            }
        } else
        {
            return parameterValue;
        }
    }

    public static <E extends Enum<E>> Integer getIntegerParameter(Configuration configuration, E parameter, boolean mandatory)
    {
        String parameterStringValue = getStringParameter(configuration, parameter, mandatory);

        if (parameterStringValue != null)
        {
            try
            {
                return Integer.parseInt(parameterStringValue);
            } catch (NumberFormatException e)
            {
                throw new RuntimeException("Configuration parameter '" + parameter + "' is not a valid integer.");
            }
        } else
        {
            return null;
        }
    }

}
