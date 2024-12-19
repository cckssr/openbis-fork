package ch.ethz.sis.afsserver.server.common;

import javax.sql.DataSource;

import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameterUtil;
import ch.ethz.sis.shared.startup.Configuration;
import ch.systemsx.cisd.common.db.IDatabaseVersionHolder;
import ch.systemsx.cisd.dbmigration.DatabaseConfigurationContext;

public class DatabaseConfiguration
{

    public enum DatabaseParameter
    {
        versionHolderClass,
        databaseEngineCode,
        basicDatabaseName,
        databaseKind,
        scriptFolder
    }

    private final String version;

    private final DatabaseConfigurationContext context;

    public DatabaseConfiguration(Configuration configuration)
    {
        IDatabaseVersionHolder versionHolder =
                AtomicFileSystemServerParameterUtil.getInstanceParameter(configuration, DatabaseParameter.versionHolderClass, true);
        String databaseEngineCode = AtomicFileSystemServerParameterUtil.getStringParameter(configuration, DatabaseParameter.databaseEngineCode, true);
        String basicDatabaseName = AtomicFileSystemServerParameterUtil.getStringParameter(configuration, DatabaseParameter.basicDatabaseName, true);
        String databaseKind = AtomicFileSystemServerParameterUtil.getStringParameter(configuration, DatabaseParameter.databaseKind, true);
        String scriptFolder = AtomicFileSystemServerParameterUtil.getStringParameter(configuration, DatabaseParameter.scriptFolder, true);

        DatabaseConfigurationContext context = new DatabaseConfigurationContext();
        context.setDatabaseEngineCode(databaseEngineCode);
        context.setBasicDatabaseName(basicDatabaseName);
        context.setDatabaseKind(databaseKind);
        context.setScriptFolder(scriptFolder);

        this.version = versionHolder.getDatabaseVersion();
        this.context = context;
    }

    public String getVersion()
    {
        return version;
    }

    public DatabaseConfigurationContext getContext()
    {
        return context;
    }

    public DataSource getDataSource()
    {
        return getContext().getDataSource();
    }

}
