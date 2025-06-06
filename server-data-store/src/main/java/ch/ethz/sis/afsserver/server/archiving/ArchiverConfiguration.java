package ch.ethz.sis.afsserver.server.archiving;

import java.io.File;
import java.util.Properties;

import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameterUtil;
import ch.ethz.sis.shared.startup.Configuration;
import ch.systemsx.cisd.base.exceptions.CheckedExceptionTunnel;
import ch.systemsx.cisd.common.exceptions.ConfigurationFailureException;
import ch.systemsx.cisd.common.properties.ExtendedProperties;
import ch.systemsx.cisd.common.properties.PropertyUtils;
import ch.systemsx.cisd.common.reflection.ClassUtils;
import ch.systemsx.cisd.openbis.dss.generic.shared.IArchiverPlugin;
import lombok.Getter;

@Getter
public class ArchiverConfiguration
{

    public enum ArchiverParameter
    {
        lockingTimeOutInSeconds,
        lockingWaitingIntervalInMillis
    }

    private static volatile ArchiverConfiguration instance;

    private static volatile Configuration configuration;

    private final IArchiverPlugin archiverPlugin;

    private final Properties properties;

    public static ArchiverConfiguration getInstance(Configuration configuration)
    {
        if (ArchiverConfiguration.configuration != configuration)
        {
            synchronized (ArchiverConfiguration.class)
            {
                if (ArchiverConfiguration.configuration != configuration)
                {
                    Properties archiverProperties = ExtendedProperties.getSubset(configuration.getProperties(), "archiver.", true);

                    if (!archiverProperties.isEmpty())
                    {
                        String storeRoot = AtomicFileSystemServerParameterUtil.getStorageRoot(configuration);
                        instance = new ArchiverConfiguration(archiverProperties, storeRoot);
                    } else
                    {
                        instance = null;
                    }

                    ArchiverConfiguration.configuration = configuration;
                }
            }
        }

        return instance;
    }

    private ArchiverConfiguration(Properties properties, String storeRoot)
    {
        String className = PropertyUtils.getMandatoryProperty(properties, "class");

        try
        {
            archiverPlugin = ClassUtils.create(IArchiverPlugin.class, className, properties,
                    new File(storeRoot));
        } catch (ConfigurationFailureException ex)
        {
            throw ex;
        } catch (Exception ex)
        {
            throw new ConfigurationFailureException("Cannot find the archiver class '" + className
                    + "'", CheckedExceptionTunnel.unwrapIfNecessary(ex));
        }

        this.properties = properties;
    }

}


