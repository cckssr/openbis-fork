package ch.ethz.sis.afsserver.server.common;

import java.util.Properties;

import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameterUtil;
import ch.ethz.sis.shared.startup.Configuration;
import ch.systemsx.cisd.common.properties.ExtendedProperties;
import ch.systemsx.cisd.common.db.IDatabaseVersionHolder;

public class DatabaseConfiguration
{

    private final Configuration configuration;

    public enum DatabaseParameter
    {
        versionHolderClass,
        databaseEngineCode,
        basicDatabaseName,
        databaseKind,
        scriptFolder
    }

    public static DatabaseConfiguration getPathInfoDBInstance(Configuration configuration)
    {
        Properties databaseProperties = ExtendedProperties.getSubset(configuration.getProperties(), "pathInfoDB.", true);
        return new DatabaseConfiguration(new Configuration(databaseProperties));
    }

    private DatabaseConfiguration(Configuration configuration)
    {
        this.configuration = configuration;
    }

    public String getVersion()
    {
        IDatabaseVersionHolder versionHolder =
                AtomicFileSystemServerParameterUtil.getInstanceParameter(configuration, DatabaseParameter.versionHolderClass, true);
        return versionHolder.getDatabaseVersion();
    }

    public String getDatabaseEngineCode()
    {
        return AtomicFileSystemServerParameterUtil.getStringParameter(configuration, DatabaseParameter.databaseEngineCode, true);
    }

    public String getBasicDatabaseName()
    {
        return AtomicFileSystemServerParameterUtil.getStringParameter(configuration, DatabaseParameter.basicDatabaseName, true);
    }

    public String getDatabaseKind()
    {
        return AtomicFileSystemServerParameterUtil.getStringParameter(configuration, DatabaseParameter.databaseKind, true);
    }

    public String getScriptFolder()
    {
        return AtomicFileSystemServerParameterUtil.getStringParameter(configuration, DatabaseParameter.scriptFolder, true);
    }

}
