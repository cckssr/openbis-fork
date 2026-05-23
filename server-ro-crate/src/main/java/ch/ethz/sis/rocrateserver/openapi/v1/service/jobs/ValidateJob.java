package ch.ethz.sis.rocrateserver.openapi.v1.service.jobs;

import ch.ethz.sis.rocrateserver.openapi.v1.service.helper.validation.ValidationResult;
import ch.ethz.sis.rocrateserver.openapi.v1.service.params.ImportParams;

public final class ValidateJob implements IAsyncJob
{

    ImportParams importParams;

    ValidationResult result;

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

    public ValidationResult getResult()
    {
        return result;
    }

}
