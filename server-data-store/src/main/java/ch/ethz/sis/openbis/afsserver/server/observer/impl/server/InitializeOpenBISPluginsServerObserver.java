package ch.ethz.sis.openbis.afsserver.server.observer.impl.server;

import java.util.List;

import ch.ethz.sis.afs.manager.TransactionConnection;
import ch.ethz.sis.afsserver.server.APIServer;
import ch.ethz.sis.afsserver.server.observer.ServerObserver;
import ch.ethz.sis.openbis.afsserver.server.archiving.ArchiverDatabaseConfiguration;
import ch.ethz.sis.openbis.afsserver.server.archiving.ArchiverServiceProvider;
import ch.ethz.sis.openbis.afsserver.server.common.ApacheCommonsLoggingConfiguration;
import ch.ethz.sis.openbis.afsserver.server.common.DatabaseConfiguration;
import ch.ethz.sis.openbis.afsserver.server.common.HierarchicalContentServiceProvider;
import ch.ethz.sis.openbis.afsserver.server.common.OpenBISConfiguration;
import ch.ethz.sis.openbis.afsserver.server.common.ServiceProvider;
import ch.ethz.sis.openbis.afsserver.server.messages.MessagesDatabaseConfiguration;
import ch.ethz.sis.openbis.afsserver.server.pathinfo.PathInfoDatabaseConfiguration;
import ch.ethz.sis.openbis.afsserver.server.pathinfo.PathInfoServiceProvider;
import ch.ethz.sis.openbis.afsserver.server.shuffling.ShufflingServiceProvider;
import ch.ethz.sis.shared.log.standard.LogManager;
import ch.ethz.sis.shared.log.standard.Logger;
import ch.ethz.sis.shared.startup.Configuration;
import ch.systemsx.cisd.common.maintenance.MaintenancePlugin;
import ch.systemsx.cisd.common.maintenance.MaintenanceTaskParameters;
import ch.systemsx.cisd.common.maintenance.MaintenanceTaskUtils;
import ch.systemsx.cisd.dbmigration.DBMigrationEngine;
import ch.systemsx.cisd.openbis.dss.generic.shared.ArchiverServiceProviderFactory;
import ch.systemsx.cisd.openbis.dss.generic.shared.HierarchicalContentServiceProviderFactory;
import ch.systemsx.cisd.openbis.dss.generic.shared.PathInfoServiceProviderFactory;
import ch.systemsx.cisd.openbis.dss.generic.shared.ServiceProviderFactory;
import ch.systemsx.cisd.openbis.dss.generic.shared.ShufflingServiceProviderFactory;

public class InitializeOpenBISPluginsServerObserver implements ServerObserver<TransactionConnection>
{

    private static final Logger logger = LogManager.getLogger(InitializeOpenBISPluginsServerObserver.class);

    private Configuration configuration;

    private List<MaintenancePlugin> maintenancePlugins;

    @Override public void init(final APIServer<TransactionConnection, ?, ?, ?> apiServer, final Configuration configuration) throws Exception
    {
        this.configuration = configuration;
    }

    @Override public void beforeStartup() throws Exception
    {
        // Make the legacy code that bases on Apache Commons Logging or Log4j use the same logging mechanism as the rest of AFS
        ApacheCommonsLoggingConfiguration.reconfigureToUseAFSLogging();

        // Autoconfigure storage UUID
        OpenBISConfiguration.getInstance(configuration).getStorageUuid();

        // Create messages DB, pathinfo DB and archiving DB
        if (MessagesDatabaseConfiguration.hasInstance(configuration))
        {
            DatabaseConfiguration messagesDatabaseConfiguration = MessagesDatabaseConfiguration.getInstance(configuration);
            DBMigrationEngine.createOrMigrateDatabaseAndGetScriptProvider(messagesDatabaseConfiguration.getContext(),
                    messagesDatabaseConfiguration.getVersion(), null,
                    null);
        }

        if (PathInfoDatabaseConfiguration.hasInstance(configuration))
        {
            DatabaseConfiguration pathInfoDatabaseConfiguration = PathInfoDatabaseConfiguration.getInstance(configuration);
            DBMigrationEngine.createOrMigrateDatabaseAndGetScriptProvider(pathInfoDatabaseConfiguration.getContext(),
                    pathInfoDatabaseConfiguration.getVersion(), null,
                    null);
        }

        if (ArchiverDatabaseConfiguration.hasInstance(configuration))
        {
            DatabaseConfiguration archiverDatabaseConfiguration = ArchiverDatabaseConfiguration.getInstance(configuration);
            DBMigrationEngine.createOrMigrateDatabaseAndGetScriptProvider(archiverDatabaseConfiguration.getContext(),
                    archiverDatabaseConfiguration.getVersion(), null,
                    null);
        }

        // Create objects used by the old DSS code
        ServiceProvider.configure(configuration);
        ServiceProviderFactory.setInstance(ServiceProvider.getInstance());
        PathInfoServiceProviderFactory.setInstance(new PathInfoServiceProvider(ServiceProvider.getInstance()));
        HierarchicalContentServiceProviderFactory.setInstance(new HierarchicalContentServiceProvider(ServiceProvider.getInstance()));
        ShufflingServiceProviderFactory.setInstance(new ShufflingServiceProvider(ServiceProvider.getInstance()));
        ArchiverServiceProviderFactory.setInstance(new ArchiverServiceProvider(ServiceProvider.getInstance()));

        // Create maintenance tasks
        logger.info("Starting maintenance tasks");
        MaintenanceTaskParameters[] maintenanceTaskParameters = MaintenanceTaskUtils.createMaintenancePlugins(configuration.getProperties());
        maintenancePlugins = MaintenanceTaskUtils.startupMaintenancePlugins(maintenanceTaskParameters);
    }

    @Override public void beforeShutdown() throws Exception
    {
        logger.info("Shutting down - maintenance tasks");
        MaintenanceTaskUtils.shutdownMaintenancePlugins(maintenancePlugins);
    }
}
