package ch.ethz.sis.rocrateserver.openapi.v1.service.jobs;

public interface IAsyncJob extends Runnable
{
    AsyncJobRegistry.Status getStatus();

    String getMimeType();

    OperationType getOperationType();

    String getUserId();

    public enum OperationType
    {
        VALIDATE,
        IMPORT,
        EXPORT;

    }

}
