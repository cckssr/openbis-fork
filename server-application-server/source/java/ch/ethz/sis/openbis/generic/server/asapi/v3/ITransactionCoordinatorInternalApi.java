package ch.ethz.sis.openbis.generic.server.asapi.v3;

import java.util.Map;
import java.util.UUID;

import ch.ethz.sis.transaction.TransactionCoordinator;

public interface ITransactionCoordinatorInternalApi
{

    Map<UUID, TransactionCoordinator.Transaction> getTransactionMap();

}
