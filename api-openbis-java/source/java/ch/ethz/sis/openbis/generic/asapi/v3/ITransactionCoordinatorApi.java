package ch.ethz.sis.openbis.generic.asapi.v3;

import ch.ethz.sis.transaction.api.ITransactionCoordinator;
import ch.systemsx.cisd.common.api.IRpcService;

public interface ITransactionCoordinatorApi extends ITransactionCoordinator, IRpcService
{

    String SERVICE_NAME = "transaction-coordinator";

    String SERVICE_URL = "/rmi-" + SERVICE_NAME;

    String JSON_SERVICE_URL = SERVICE_URL + ".json";

    String APPLICATION_SERVER_PARTICIPANT_ID = "application-server";

    String AFS_SERVER_PARTICIPANT_ID = "afs-server";

}
