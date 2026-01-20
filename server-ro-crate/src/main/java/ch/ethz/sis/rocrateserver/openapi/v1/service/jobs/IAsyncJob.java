package ch.ethz.sis.rocrateserver.openapi.v1.service.jobs;

public sealed interface IAsyncJob extends Runnable permits ExportJob, ValidateJob, ImportJob
{
    AsyncJobRegistry.Status getStatus();

    String getMimeType();

    String getUserId();

    Exception getException();

}
