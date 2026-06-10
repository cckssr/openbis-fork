package ch.ethz.sis.afsserver.server;

import java.util.UUID;

import ch.ethz.sis.shared.startup.Configuration;

public interface OperationResultCache
{

    void init(Configuration configuration);

    void setResult(UUID operationId, String operationName, long operationDuration, OperationResult result);

    OperationResult getResult(UUID operationId);

}
