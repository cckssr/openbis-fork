package ch.ethz.sis.afsserver.server.archiving;

import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameterUtil;
import ch.ethz.sis.shared.startup.Configuration;
import lombok.Getter;

@Getter
public class ArchiverConfiguration
{

    public enum ArchiverParameter
    {
        lockingTimeOut
    }

    private static volatile ArchiverConfiguration instance;

    private static volatile Configuration configuration;

    private final Integer lockingTimeOut;

    public static ArchiverConfiguration getInstance(Configuration configuration)
    {
        if (ArchiverConfiguration.configuration != configuration)
        {
            synchronized (ArchiverConfiguration.class)
            {
                if (ArchiverConfiguration.configuration != configuration)
                {
                    instance = new ArchiverConfiguration(configuration);
                    ArchiverConfiguration.configuration = configuration;
                }
            }
        }

        return instance;
    }

    private ArchiverConfiguration(Configuration configuration)
    {
        lockingTimeOut = AtomicFileSystemServerParameterUtil.getIntegerParameter(configuration,
                ArchiverParameter.lockingTimeOut, true);
    }

}


