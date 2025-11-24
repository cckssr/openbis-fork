package ch.ethz.sis.rocrateserver.openapi.v1.service.jobs;

public interface IAsyncJob extends Runnable
{
    AsyncJobRegistry.Status getStatus();

    String getMimeType();

    OperationType getOperationType();

    public enum OperationType
    {
        VALIDATE,
        IMPORT,
        EXPORT;

    }

}
