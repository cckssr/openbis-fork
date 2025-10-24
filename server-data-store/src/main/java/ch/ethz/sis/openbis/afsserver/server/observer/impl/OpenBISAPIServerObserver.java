package ch.ethz.sis.openbis.afsserver.server.observer.impl;

import ch.ethz.sis.afs.manager.TransactionConnection;
import ch.ethz.sis.afsserver.server.Request;
import ch.ethz.sis.afsserver.server.Response;
import ch.ethz.sis.afsserver.server.Worker;
import ch.ethz.sis.afsserver.server.observer.APICall;
import ch.ethz.sis.afsserver.server.observer.APIServerObserver;
import ch.ethz.sis.openbis.afsserver.server.observer.impl.api.CreateDataSetsAPIServerObserver;
import ch.ethz.sis.openbis.afsserver.server.observer.impl.api.UsePathInfoDatabaseAPIServerObserver;
import ch.ethz.sis.shared.startup.Configuration;

public class OpenBISAPIServerObserver implements APIServerObserver<TransactionConnection>
{

    private CreateDataSetsAPIServerObserver createDataSetsObserver;

    private UsePathInfoDatabaseAPIServerObserver pathInfoDatabaseObserver;

    @Override
    public void init(Configuration configuration) throws Exception
    {
        createDataSetsObserver = new CreateDataSetsAPIServerObserver(configuration);
        pathInfoDatabaseObserver = new UsePathInfoDatabaseAPIServerObserver(configuration);
    }

    @Override
    public void beforeAPICall(Worker<TransactionConnection> worker, Request request) throws Exception
    {
        createDataSetsObserver.beforeAPICall(worker, request);
    }

    @Override public Object duringAPICall(Worker<TransactionConnection> worker, APICall apiCall) throws Exception
    {
        return pathInfoDatabaseObserver.duringAPICall(worker, apiCall);
    }

    @Override
    public void afterAPICall(Worker<TransactionConnection> worker, Request request, Response response) throws Exception
    {
        createDataSetsObserver.afterAPICall(worker, request, response);
    }

}
