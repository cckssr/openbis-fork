package ch.ethz.sis.transaction;

public class TransactionOperationException extends RuntimeException
{

    // Needed for ExceptionUtil to avoid masquerading
    public TransactionOperationException(String message)
    {
        super(message);
    }

    public TransactionOperationException(String message, Throwable cause)
    {
        super(message, cause);
    }

}
