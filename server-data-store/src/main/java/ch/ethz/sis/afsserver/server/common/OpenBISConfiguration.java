package ch.ethz.sis.afsserver.server.common;

import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameterUtil;
import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.shared.startup.Configuration;
import lombok.Getter;

@Getter
public class OpenBISConfiguration
{

    public enum OpenBISParameter
    {
        openBISUrl,
        openBISTimeout,
        openBISUser,
        openBISPassword,
        openBISLastSeenDeletionFile,
        openBISLastSeenDeletionBatchSize,
        openBISLastSeenDeletionIntervalInSeconds
    }

    private static volatile OpenBISConfiguration instance;

    private static volatile Configuration configuration;

    private final String openBISUrl;

    private final Integer openBISTimeout;

    private final String openBISUser;

    private final String openBISPassword;

    private final String openBISLastSeenDeletionFile;

    private final Integer openBISLastSeenDeletionBatchSize;

    private final Integer openBISLastSeenDeletionIntervalInSeconds;

    private final IOpenBISFacade openBISFacade;

    public static OpenBISConfiguration getInstance(Configuration configuration)
    {
        if (OpenBISConfiguration.configuration != configuration)
        {
            synchronized (OpenBISConfiguration.class)
            {
                if (OpenBISConfiguration.configuration != configuration)
                {
                    instance = new OpenBISConfiguration(configuration);
                    OpenBISConfiguration.configuration = configuration;
                }
            }
        }

        return instance;
    }

    private OpenBISConfiguration(Configuration configuration)
    {
        openBISUrl = AtomicFileSystemServerParameterUtil.getStringParameter(configuration, OpenBISParameter.openBISUrl, true);
        openBISTimeout = AtomicFileSystemServerParameterUtil.getIntegerParameter(configuration, OpenBISParameter.openBISTimeout, true);
        openBISUser = AtomicFileSystemServerParameterUtil.getStringParameter(configuration, OpenBISParameter.openBISUser, true);
        openBISPassword = AtomicFileSystemServerParameterUtil.getStringParameter(configuration, OpenBISParameter.openBISPassword, true);
        openBISLastSeenDeletionFile =
                AtomicFileSystemServerParameterUtil.getStringParameter(configuration, OpenBISParameter.openBISLastSeenDeletionFile, true);
        openBISLastSeenDeletionBatchSize =
                AtomicFileSystemServerParameterUtil.getIntegerParameter(configuration, OpenBISParameter.openBISLastSeenDeletionBatchSize, true);
        openBISLastSeenDeletionIntervalInSeconds = AtomicFileSystemServerParameterUtil.getIntegerParameter(configuration,
                OpenBISParameter.openBISLastSeenDeletionIntervalInSeconds, true);
        openBISFacade = new OpenBISFacade(openBISUrl, openBISUser, openBISPassword, openBISTimeout);
    }

    public OpenBIS getOpenBIS()
    {
        return new OpenBIS(openBISUrl, openBISTimeout);
    }

}
