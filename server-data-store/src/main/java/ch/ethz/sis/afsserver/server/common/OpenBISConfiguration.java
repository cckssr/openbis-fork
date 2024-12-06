package ch.ethz.sis.afsserver.server.common;

import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameterUtil;
import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.shared.startup.Configuration;

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

    private final Configuration configuration;

    public static OpenBISConfiguration getInstance(Configuration configuration)
    {
        return new OpenBISConfiguration(configuration);
    }

    private OpenBISConfiguration(Configuration configuration)
    {
        this.configuration = configuration;
    }

    public OpenBIS getOpenBIS()
    {
        return new OpenBIS(getOpenBISUrl(), getOpenBISTimeout());
    }

    public OpenBISFacade getOpenBISFacade()
    {
        return new OpenBISFacade(getOpenBISUrl(), getOpenBISUser(), getOpenBISPassword(),
                getOpenBISTimeout());
    }

    public String getOpenBISUrl()
    {
        return AtomicFileSystemServerParameterUtil.getStringParameter(configuration, OpenBISParameter.openBISUrl, true);
    }

    public Integer getOpenBISTimeout()
    {
        return AtomicFileSystemServerParameterUtil.getIntegerParameter(configuration, OpenBISParameter.openBISTimeout, true);
    }

    public String getOpenBISUser()
    {
        return AtomicFileSystemServerParameterUtil.getStringParameter(configuration, OpenBISParameter.openBISUser, true);
    }

    public String getOpenBISPassword()
    {
        return AtomicFileSystemServerParameterUtil.getStringParameter(configuration, OpenBISParameter.openBISPassword, true);
    }

    public String getOpenBISLastSeenDeletionFile()
    {
        return AtomicFileSystemServerParameterUtil.getStringParameter(configuration, OpenBISParameter.openBISLastSeenDeletionFile,
                true);
    }

    public Integer getOpenBISLastSeenDeletionBatchSize()
    {
        return AtomicFileSystemServerParameterUtil.getIntegerParameter(configuration, OpenBISParameter.openBISLastSeenDeletionBatchSize, true);
    }

    public Integer getOpenBISLastSeenDeletionIntervalInSeconds()
    {
        return AtomicFileSystemServerParameterUtil.getIntegerParameter(configuration,
                OpenBISParameter.openBISLastSeenDeletionIntervalInSeconds, true);
    }

}
