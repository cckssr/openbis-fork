package ch.ethz.sis.openbis.afsserver.server.common;

import java.util.Properties;

import javax.sql.DataSource;

import ch.systemsx.cisd.common.db.IDatabaseVersionHolder;
import ch.systemsx.cisd.common.properties.PropertyUtils;
import ch.systemsx.cisd.common.reflection.ClassUtils;
import ch.systemsx.cisd.dbmigration.DatabaseConfigurationContext;

public class DatabaseConfiguration
{

    public static final String NAME = "name";

    public static final String KIND = "kind";

    public static final String ENGINE = "engine";

    public static final String VERSION_HOLDER_CLASS = "version-holder-class";

    public static final String SCRIPT_FOLDER = "script-folder";

    public static final String SCRIPT_SINGLE_STEP_MODE = "script-single-step-mode";

    public static final String URL_HOST_PART = "url-host-part";

    public static final String CREATE_FROM_SCRATCH = "create-from-scratch";

    public static final String OWNER = "owner";

    public static final String OWNER_PASSWORD = "owner-password";

    public static final String ADMIN_USER = "admin-user";

    public static final String ADMIN_PASSWORD = "admin-password";

    public static final String MAX_WAIT_FOR_CONNECTION = "max-wait-for-connection";

    public static final String MAX_ACTIVE_CONNECTIONS = "max-active-connections";

    public static final String MAX_IDLE_CONNECTIONS = "max-idle-connections";

    public static final String ACTIVE_CONNECTIONS_LOG_INTERVAL = "active-connections-log-interval";

    private final String version;

    private final DatabaseConfigurationContext context;

    public DatabaseConfiguration(Properties properties)
    {
        // mandatory
        String name = PropertyUtils.getMandatoryProperty(properties, NAME);
        String kind = PropertyUtils.getMandatoryProperty(properties, KIND);
        String engine = PropertyUtils.getMandatoryProperty(properties, ENGINE);
        String versionHolderClass = PropertyUtils.getMandatoryProperty(properties, VERSION_HOLDER_CLASS);
        String scriptFolder = PropertyUtils.getMandatoryProperty(properties, SCRIPT_FOLDER);

        // optional
        String scriptSingleStepMode = PropertyUtils.getProperty(properties, SCRIPT_SINGLE_STEP_MODE);
        String urlHostPart = PropertyUtils.getProperty(properties, URL_HOST_PART);
        String createFromScratch = PropertyUtils.getProperty(properties, CREATE_FROM_SCRATCH);
        String owner = PropertyUtils.getProperty(properties, OWNER);
        String ownerPassword = PropertyUtils.getProperty(properties, OWNER_PASSWORD);
        String adminUser = PropertyUtils.getProperty(properties, ADMIN_USER);
        String adminPassword = PropertyUtils.getProperty(properties, ADMIN_PASSWORD);
        String maxWaitForConnection = PropertyUtils.getProperty(properties, MAX_WAIT_FOR_CONNECTION);
        String maxActiveConnections = PropertyUtils.getProperty(properties, MAX_ACTIVE_CONNECTIONS);
        String maxIdleConnections = PropertyUtils.getProperty(properties, MAX_IDLE_CONNECTIONS);
        String activeConnectionsLogInterval = PropertyUtils.getProperty(properties, ACTIVE_CONNECTIONS_LOG_INTERVAL);

        DatabaseConfigurationContext context = new DatabaseConfigurationContext();
        context.setDatabaseEngineCode(engine);
        context.setBasicDatabaseName(name);
        context.setDatabaseKind(kind);
        context.setScriptFolder(scriptFolder);
        context.setScriptSingleStepModeProp(scriptSingleStepMode);
        context.setUrlHostPart(urlHostPart);
        context.setCreateFromScratchProp(createFromScratch);
        context.setOwner(owner);
        context.setPassword(ownerPassword);
        context.setAdminUser(adminUser);
        context.setAdminPassword(adminPassword);
        context.setMaxWaitForConnectionProp(maxWaitForConnection);
        context.setMaxActiveConnectionsProp(maxActiveConnections);
        context.setMaxIdleConnectionsProp(maxIdleConnections);
        context.setActiveConnectionsLogIntervalProp(activeConnectionsLogInterval);

        IDatabaseVersionHolder versionHolder = null;
        try
        {
            versionHolder = (IDatabaseVersionHolder) ClassUtils.createInstance(Class.forName(versionHolderClass));
        } catch (ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }

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

    public static class DatabaseNotConfiguredException extends RuntimeException
    {

    }

}
