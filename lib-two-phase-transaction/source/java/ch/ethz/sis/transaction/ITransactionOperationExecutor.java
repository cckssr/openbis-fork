package ch.ethz.sis.transaction;

public interface ITransactionOperationExecutor
{

    <T> T executeOperation(String sessionToken, String operationName, Object[] operationArguments) throws Exception;

}
