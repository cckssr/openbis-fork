package ch.ethz.sis.rocrateserver.openapi.v1.service.jobs;

import java.util.UUID;

public sealed interface IAsyncJob extends Runnable permits ExportJob, ValidateJob, ImportJob
{
    AsyncJobRegistry.Status getStatus();

    String getMimeType();

    String getUserId();

    Exception getException();

    UUID getJobId();

}
