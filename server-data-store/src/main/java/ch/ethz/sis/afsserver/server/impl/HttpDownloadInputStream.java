package ch.ethz.sis.afsserver.server.impl;

import ch.ethz.sis.afs.api.OperationsAPI;
import ch.ethz.sis.afs.api.TransactionConnectionInformation;
import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afsserver.server.APIServer;
import ch.ethz.sis.afsserver.server.Request;
import ch.ethz.sis.afsserver.server.Response;
import ch.ethz.sis.afsserver.server.Worker;
import lombok.SneakyThrows;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/*
 This class implements the methods used by  io.netty.handler.stream.ChunkedStream
 */
class HttpDownloadInputStream<CONNECTION extends TransactionConnectionInformation, API> extends InputStream
{
    final APIServer<CONNECTION, Request, Response, API> server;
    final Map<String, Object> parsedParameters;
    final String method;
    Worker<CONNECTION> connectionWorker;
    boolean errorFound = false;

    // Variables to support constantly reading forward
    File[] files;
    int filesIndex;
    int currentOffset;

    public  HttpDownloadInputStream(
            APIServer<CONNECTION, Request, Response, API> server,
            final String method,
            final Map<String, Object> parsedParameters) throws Exception {
        this.server = server;
        this.parsedParameters = parsedParameters;
        this.method = method;
        connectionWorker = server.checkOut();
    }

    /*
         @NotNull byte[] b, @Range int off, @Range int len
         */
    @SneakyThrows
    @Override
    public int read(byte[] buffer, int off, int len) throws IOException
    {
//        String owner = parsedParameters.get("owner").toString();
//        String source = parsedParameters.get("source").toString();
//        Worker worker = null;
//        boolean errorFound = false;
//        try
//        {
//            worker = server.checkOut();
//            OperationsAPI connection = (OperationsAPI) worker.getConnection();
//            connection.read()
//        } catch (Exception ex) {
//            errorFound = true;
//        } finally
//        {
//            server.checkIn(errorFound, worker);
//        }
        return -1;
    }

    @SneakyThrows
    @Override
    public int read(byte[] buffer) throws IOException
    {
        return -1;
    }

    @Override
    public int read() throws IOException
    {
        return -1;
    }

    @SneakyThrows
    @Override
    public int available() throws IOException
    {
        return -1;
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
