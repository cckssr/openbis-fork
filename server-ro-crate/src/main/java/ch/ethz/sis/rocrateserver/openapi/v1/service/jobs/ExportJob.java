package ch.ethz.sis.rocrateserver.openapi.v1.service.jobs;

import ch.ethz.sis.rocrateserver.openapi.v1.service.params.ExportParams;

import java.nio.file.Path;

public final class ExportJob implements IAsyncJob
{

    ExportParams exportParams;

    Path exportResult;

    Exception exception;

    @Override
    public AsyncJobRegistry.Status getStatus()
    {
        return null;
    }

    @Override
    public String getMimeType()
    {
        return null;
    }

    @Override
    public String getUserId()
    {
        return null;
    }

    @Override
    public Exception getException()
    {
        return null;
    }

    @Override
    public void run()
    {

    }
}
