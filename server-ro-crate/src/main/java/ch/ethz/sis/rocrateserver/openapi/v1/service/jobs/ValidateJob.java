package ch.ethz.sis.rocrateserver.openapi.v1.service.jobs;

import ch.ethz.sis.rocrateserver.openapi.v1.service.helper.validation.ValidationResult;
import ch.ethz.sis.rocrateserver.openapi.v1.service.params.ImportParams;

import java.util.UUID;

public final class ValidateJob implements IAsyncJob
{
    private final UUID jobId;

    ImportParams importParams;

    ValidationResult result;

    Exception exception;

    public ValidateJob() {
        this.jobId = UUID.randomUUID();
    }

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

    @Override
    public UUID getJobId()
    {
        return this.jobId;
    }

    public ValidationResult getResult()
    {
        return result;
    }

}
