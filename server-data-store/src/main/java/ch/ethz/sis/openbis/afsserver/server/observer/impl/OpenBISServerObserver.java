package ch.ethz.sis.openbis.afsserver.server.observer.impl;

import ch.ethz.sis.afs.manager.TransactionConnection;
import ch.ethz.sis.afsserver.server.APIServer;
import ch.ethz.sis.afsserver.server.observer.ServerObserver;
import ch.ethz.sis.openbis.afsserver.server.observer.impl.server.DeleteDataOfDeletedDataSetsServerObserver;
import ch.ethz.sis.openbis.afsserver.server.observer.impl.server.InitializeOpenBISPluginsServerObserver;
import ch.ethz.sis.shared.startup.Configuration;

public class OpenBISServerObserver implements ServerObserver<TransactionConnection>
{

    private InitializeOpenBISPluginsServerObserver initializeOpenBISPluginsServerObserver;

    private DeleteDataOfDeletedDataSetsServerObserver deleteDataOfDeletedDataSetsServerObserver;

    @Override
    public void init(APIServer<TransactionConnection, ?, ?, ?> apiServer, Configuration configuration) throws Exception
    {
        initializeOpenBISPluginsServerObserver = new InitializeOpenBISPluginsServerObserver();
        initializeOpenBISPluginsServerObserver.init(apiServer, configuration);

        deleteDataOfDeletedDataSetsServerObserver = new DeleteDataOfDeletedDataSetsServerObserver();
        deleteDataOfDeletedDataSetsServerObserver.init(apiServer, configuration);
    }

    @Override
    public void beforeStartup() throws Exception
    {
        initializeOpenBISPluginsServerObserver.beforeStartup();
        deleteDataOfDeletedDataSetsServerObserver.beforeStartup();
    }

    @Override
    public void beforeShutdown() throws Exception
    {
        initializeOpenBISPluginsServerObserver.beforeShutdown();
        deleteDataOfDeletedDataSetsServerObserver.beforeShutdown();
    }

}
