package ch.ethz.sis.afsserver.server;

public interface OperationResult
{

    Object getResult();

    Throwable getException();

}
