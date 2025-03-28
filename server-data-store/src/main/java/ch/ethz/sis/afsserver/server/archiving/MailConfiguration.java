package ch.ethz.sis.afsserver.server.archiving;

import java.util.Properties;

import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameterUtil;
import ch.ethz.sis.shared.startup.Configuration;
import ch.systemsx.cisd.common.properties.ExtendedProperties;
import lombok.Getter;

@Getter
public class MailConfiguration
{

    public enum MailParameter
    {
        from,
        smtpHost,
        smtpUser,
        smtpPassword
    }

    private static volatile MailConfiguration instance;

    private static volatile Configuration configuration;

    private final String from;

    private final String smtpHost;

    private final String smtpUser;

    private final String smtpPassword;

    public static MailConfiguration getInstance(Configuration configuration)
    {
        if (MailConfiguration.configuration != configuration)
        {
            synchronized (MailConfiguration.class)
            {
                if (MailConfiguration.configuration != configuration)
                {
                    Properties mailProperties = ExtendedProperties.getSubset(configuration.getProperties(), "mail.", true);

                    if (!mailProperties.isEmpty())
                    {
                        instance = new MailConfiguration(new Configuration(mailProperties));
                    } else
                    {
                        instance = null;
                    }

                    MailConfiguration.configuration = configuration;
                }
            }
        }

        return instance;
    }

    private MailConfiguration(Configuration configuration)
    {
        this.from = AtomicFileSystemServerParameterUtil.getStringParameter(configuration,
                MailConfiguration.MailParameter.from, false);
        this.smtpHost = AtomicFileSystemServerParameterUtil.getStringParameter(configuration,
                MailParameter.smtpHost, false);
        this.smtpUser = AtomicFileSystemServerParameterUtil.getStringParameter(configuration,
                MailParameter.smtpUser, false);
        this.smtpPassword = AtomicFileSystemServerParameterUtil.getStringParameter(configuration,
                MailParameter.smtpPassword, false);
    }

}
