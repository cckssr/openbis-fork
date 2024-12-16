package ch.ethz.sis.transaction;

public enum TransactionStatus
{
    NEW,
    BEGIN_STARTED,
    BEGIN_FINISHED,
    PREPARE_STARTED,
    PREPARE_FINISHED,
    COMMIT_STARTED,
    COMMIT_FINISHED,
    ROLLBACK_STARTED,
    ROLLBACK_FINISHED

}
