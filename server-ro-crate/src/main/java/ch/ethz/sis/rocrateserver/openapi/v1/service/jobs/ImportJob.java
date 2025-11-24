package ch.ethz.sis.rocrateserver.openapi.v1.service.jobs;

import ch.ethz.sis.rocrateserver.openapi.v1.service.params.ImportParams;
import ch.ethz.sis.rocrateserver.openapi.v1.service.response.ImportResponse;

public final class ImportJob implements IAsyncJob
{
    ImportParams importParams;

    Exception exception;

    ImportResponse importResult;



    // keep reference to result

    @Override
    public AsyncJobRegistry.Status getStatus()
    {
        return null;
    }

    @Override
    public String getMimeType()
    {
        return importParams.getContentType();
    }

    @Override
    public String getUserId()
    {
        return null;
    }

    @Override
    public Exception getException()
    {
        return exception;
    }

    @Override
    public void run()
    {

    }

    public ImportResponse getResult()
    {
        return importResult;
    }
}
