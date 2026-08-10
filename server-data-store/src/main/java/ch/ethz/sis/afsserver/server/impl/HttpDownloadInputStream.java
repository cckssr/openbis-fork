package ch.ethz.sis.afsserver.server.impl;

import ch.ethz.sis.afs.api.TransactionConnectionInformation;
import ch.ethz.sis.afsserver.server.APIServer;
import ch.ethz.sis.afsserver.server.Request;
import ch.ethz.sis.afsserver.server.Response;
import ch.ethz.sis.afsserver.server.Worker;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

class HttpDownloadInputStream<CONNECTION extends TransactionConnectionInformation, API> extends InputStream
{
    final APIServer<CONNECTION, Request, Response, API> server;
    final Map<String, Object> parsedParameters;
    final String method;
    Worker<CONNECTION> connectionWorker;
    boolean errorFound = false;

    public  HttpDownloadInputStream(
            APIServer<CONNECTION, Request, Response, API> server,
            final String method,
            final Map<String, Object> parsedParameters) throws Exception {
        this.server = server;
        this.parsedParameters = parsedParameters;
        this.method = method;
        connectionWorker = server.checkOut();
    }

    @Override
    public int read() throws IOException
    {
        String owner = parsedParameters.get("owner").toString();
        String source = parsedParameters.get("source").toString();
        //final Response response = server.processOperation(apiRequest, apiResponseBuilder, performanceAuditor);

        return 0;
    }

    @Override
    public void close() throws IOException
    {
        if (connectionWorker != null)
        {
            server.checkIn(errorFound, connectionWorker);
            connectionWorker = null;
        }
        super.close();
    }
}
