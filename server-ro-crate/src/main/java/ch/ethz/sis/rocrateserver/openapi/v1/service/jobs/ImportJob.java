package ch.ethz.sis.rocrateserver.openapi.v1.service.jobs;

import ch.ethz.sis.rocrateserver.openapi.v1.service.params.ImportParams;

public class ImportJob implements IAsyncJob
{
    ImportParams importParams;

    Exception exception;

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
    public OperationType getOperationType()
    {
        return OperationType.IMPORT;
    }

    @Override
    public void run()
    {

    }
}
