/*
 * Copyright ETH 2022 - 2023 Zürich, Scientific IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.ethz.sis.afsserver.server;

import java.util.List;

import ch.ethz.sis.afsjson.jackson.JacksonObjectMapper;
import ch.ethz.sis.afsserver.http.HttpServer;
import ch.ethz.sis.afsserver.http.HttpServerHandler;
import ch.ethz.sis.afsserver.server.common.ApacheCommonsLoggingConfiguration;
import ch.ethz.sis.afsserver.server.common.ApacheLog4j1Configuration;
import ch.ethz.sis.afsserver.server.common.DatabaseConfiguration;
import ch.ethz.sis.afsserver.server.impl.ApiServerAdapter;
import ch.ethz.sis.afsserver.server.impl.HttpDownloadAdapter;
import ch.ethz.sis.afsserver.server.maintenance.MaintenancePlugin;
import ch.ethz.sis.afsserver.server.maintenance.MaintenanceTaskParameters;
import ch.ethz.sis.afsserver.server.maintenance.MaintenanceTaskUtils;
import ch.ethz.sis.afsserver.server.observer.APIServerObserver;
import ch.ethz.sis.afsserver.server.observer.ServerObserver;
import ch.ethz.sis.afsserver.server.observer.impl.DummyServerObserver;
import ch.ethz.sis.afsserver.server.shuffling.IncomingShareIdProvider;
import ch.ethz.sis.afsserver.server.shuffling.ServiceProvider;
import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameter;
import ch.ethz.sis.shared.log.LogFactory;
import ch.ethz.sis.shared.log.LogFactoryFactory;
import ch.ethz.sis.shared.log.LogManager;
import ch.ethz.sis.shared.log.Logger;
import ch.ethz.sis.shared.pool.Factory;
import ch.ethz.sis.shared.pool.Pool;
import ch.ethz.sis.shared.startup.Configuration;
import ch.systemsx.cisd.dbmigration.DBMigrationEngine;
import ch.systemsx.cisd.dbmigration.DatabaseConfigurationContext;

public final class Server<CONNECTION, API>
{

    private Logger logger;

    private Pool<Configuration, CONNECTION> connectionsPool;

    private Pool<Configuration, Worker<CONNECTION>> workersPool;

    private APIServer apiServer;

    private JacksonObjectMapper jsonObjectMapper;

    private ApiServerAdapter<CONNECTION, API> apiServerAdapter;

    private HttpDownloadAdapter<CONNECTION, API> httpDownloadAdapter;

    private HttpServer httpServer;

    private boolean shutdown;

    private ServerObserver<CONNECTION> observer;

    private List<MaintenancePlugin> maintenancePlugins;

    public Server(Configuration configuration) throws Exception
    {
        //1. Load logging plugin, Initializing LogManager
        shutdown = false;

        LogFactoryFactory logFactoryFactory = new LogFactoryFactory();
        LogFactory logFactory = logFactoryFactory.create(configuration.getStringProperty(AtomicFileSystemServerParameter.logFactoryClass));
        logFactory.configure(configuration.getStringProperty(AtomicFileSystemServerParameter.logConfigFile));
        LogManager.setLogFactory(logFactory);
        logger = LogManager.getLogger(Server.class);

        // Make the legacy code that bases on Apache Commons Logging or Log4j use the same logging mechanism as the rest of AFS
        ApacheCommonsLoggingConfiguration.reconfigureToUseAFSLogging();
        ApacheLog4j1Configuration.reconfigureToUseAFSLogging();

        logger.info("=== Server Bootstrap ===");
        logger.info("Running with java.version: " + System.getProperty("java.version"));

        // 2 Create pathinfo DB
        DatabaseConfiguration pathInfoDatabaseConfiguration = DatabaseConfiguration.getPathInfoDBInstance(configuration);
        DatabaseConfigurationContext pathInfoDatabaseContext = new DatabaseConfigurationContext();
        pathInfoDatabaseContext.setDatabaseEngineCode(pathInfoDatabaseConfiguration.getDatabaseEngineCode());
        pathInfoDatabaseContext.setBasicDatabaseName(pathInfoDatabaseConfiguration.getBasicDatabaseName());
        pathInfoDatabaseContext.setDatabaseKind(pathInfoDatabaseConfiguration.getDatabaseKind());
        pathInfoDatabaseContext.setScriptFolder(pathInfoDatabaseConfiguration.getScriptFolder());
        DBMigrationEngine.createOrMigrateDatabaseAndGetScriptProvider(pathInfoDatabaseContext, pathInfoDatabaseConfiguration.getVersion(), null,
                null);

        // 2.1 Load DB plugin
        logger.info("Creating Connection Factory");
        Factory<Configuration, Configuration, CONNECTION> connectionFactory =
                configuration.getSharableInstance(AtomicFileSystemServerParameter.connectionFactoryClass);
        connectionFactory.init(configuration);

        logger.info("Creating Workers Factory");
        Factory<Configuration, Configuration, Worker<CONNECTION>> workerFactory =
                configuration.getSharableInstance(AtomicFileSystemServerParameter.workerFactoryClass);
        workerFactory.init(configuration);

        // 2.2 Creating workers pool
        logger.info("Creating server workers");
        int poolSize = configuration.getIntegerProperty(AtomicFileSystemServerParameter.poolSize);

        connectionsPool = new Pool<>(poolSize, configuration, connectionFactory);
        workersPool = new Pool<>(poolSize, configuration, workerFactory);

        // 2.3 Init API Server observer
        APIServerObserver<CONNECTION> apiServerObserver = configuration.getInstance(AtomicFileSystemServerParameter.apiServerObserver);
        if (apiServerObserver == null)
        {
            apiServerObserver = new DummyServerObserver<>();
        }
        apiServerObserver.init(configuration);

        // 2.4 Creating API Server
        logger.info("Creating API server");
        Class<?> publicApiInterface = configuration.getInterfaceClass(AtomicFileSystemServerParameter.publicApiInterface);
        String interactiveSessionKey = configuration.getStringProperty(AtomicFileSystemServerParameter.apiServerInteractiveSessionKey);
        String transactionManagerKey = configuration.getStringProperty(AtomicFileSystemServerParameter.apiServerTransactionManagerKey);
        int apiServerWorkerTimeout = configuration.getIntegerProperty(AtomicFileSystemServerParameter.apiServerWorkerTimeout);
        apiServer =
                new APIServer(connectionsPool, workersPool, publicApiInterface, interactiveSessionKey, transactionManagerKey, apiServerWorkerTimeout,
                        apiServerObserver);

        // 2.5 Creating JSON RPC Service
        logger.info("Creating API Server adaptor");
        jsonObjectMapper = configuration.getSharableInstance(AtomicFileSystemServerParameter.jsonObjectMapperClass);
        apiServerAdapter = new ApiServerAdapter(apiServer, jsonObjectMapper);

        // 2.6 Creating Download Service
        logger.info("Creating Download Server adaptor");
        httpDownloadAdapter = new HttpDownloadAdapter<>(apiServer, jsonObjectMapper);

        // 2.7 Creating HTTP Service
        int httpServerPort = configuration.getIntegerProperty(AtomicFileSystemServerParameter.httpServerPort);
        int maxContentLength = configuration.getIntegerProperty(AtomicFileSystemServerParameter.httpMaxContentLength);
        logger.info("Starting HTTP Service on port " + httpServerPort + " with maxContentLength " + maxContentLength);
        httpServer = configuration.getSharableInstance(AtomicFileSystemServerParameter.httpServerClass);
        String httpServerUri = configuration.getStringProperty(AtomicFileSystemServerParameter.httpServerUri);
        httpServer.start(httpServerPort, maxContentLength, httpServerUri, new HttpServerHandler[] { apiServerAdapter });

        // 2.8 Create objects used by the old DSS code
        ServiceProvider.configure(configuration);
        IncomingShareIdProvider.configure(configuration);

        // 2.9 Create maintenance tasks
        logger.info("Starting maintenance tasks");
        MaintenanceTaskParameters[] maintenanceTaskParameters = MaintenanceTaskUtils.createMaintenancePlugins(configuration.getProperties());
        maintenancePlugins = MaintenanceTaskUtils.startupMaintenancePlugins(maintenanceTaskParameters);

        // 2.10 Init observer
        observer = configuration.getInstance(AtomicFileSystemServerParameter.serverObserver);
        if (observer == null)
        {
            observer = new DummyServerObserver<>();
        }
        observer.init(apiServer, configuration);
        observer.beforeStartup();

        // 3 Startup
        logger.info("=== Server ready ===");
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            public void run()
            {
                try
                {
                    shutdown(true);
                } catch (Exception e)
                {
                    logger.catching(e);
                }
            }
        });
    }

    public void shutdown(boolean gracefully) throws Exception
    {
        if (!shutdown)
        {
            observer.beforeShutdown();
            shutdown = true;
            logger.info("Shutting down - maintenance tasks");
            MaintenanceTaskUtils.shutdownMaintenancePlugins(maintenancePlugins);
            logger.info("Shutting down - http server");
            httpServer.shutdown(gracefully);
            logger.info("Shutting down - api server");
            apiServer.shutdown();
            logger.info("Shutting down - waiting for api server workers to finish");
            if (gracefully)
            {
                while (apiServer.hasWorkersInUse())
                {
                    Thread.sleep(100);
                }
            }
            logger.info("Shutting down - connection pool");
            connectionsPool.shutdown();
        }
    }
}
