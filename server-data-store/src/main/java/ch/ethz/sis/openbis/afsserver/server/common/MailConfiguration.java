package ch.ethz.sis.openbis.afsserver.server.common;

import java.util.Properties;

import ch.ethz.sis.shared.startup.Configuration;
import ch.systemsx.cisd.common.properties.ExtendedProperties;
import ch.systemsx.cisd.common.properties.PropertyUtils;
import lombok.Getter;

@Getter
public class MailConfiguration
{

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
                        instance = new MailConfiguration(mailProperties);
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

    private MailConfiguration(Properties properties)
    {
        this.from = PropertyUtils.getProperty(properties, "from");
        this.smtpHost = PropertyUtils.getProperty(properties, "smtp.host");
        this.smtpUser = PropertyUtils.getProperty(properties, "smtp.user");
        this.smtpPassword = PropertyUtils.getProperty(properties, "smtp.password");
    }

}
