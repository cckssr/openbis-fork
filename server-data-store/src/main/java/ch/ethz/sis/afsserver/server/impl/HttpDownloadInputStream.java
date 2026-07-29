package ch.ethz.sis.afsserver.server.impl;

import ch.ethz.sis.afs.api.TransactionConnectionInformation;
import ch.ethz.sis.afsserver.server.APIServer;
import ch.ethz.sis.afsserver.server.Request;
import ch.ethz.sis.afsserver.server.Response;
import ch.ethz.sis.afsserver.server.Worker;

import java.io.IOException;
import java.io.InputStream;

class HttpDownloadInputStream<CONNECTION extends TransactionConnectionInformation, API> extends InputStream
{
    final APIServer<CONNECTION, Request, Response, API> server;
    Worker<CONNECTION> connectionWorker;
    boolean errorFound = false;

    public  HttpDownloadInputStream(
            APIServer<CONNECTION, Request, Response, API> server) throws Exception {
        this.server = server;
        connectionWorker = server.checkOut();
    }

    @Override
    public int read() throws IOException
    {
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
