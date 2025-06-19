package ch.ethz.sis.openbis.generic.asapi.v3;

import ch.ethz.sis.transaction.api.ITransactionParticipant;
import ch.systemsx.cisd.common.api.IRpcService;

public interface ITransactionParticipantApi extends ITransactionParticipant, IRpcService
{

    String SERVICE_NAME = "transaction-participant";

    String SERVICE_URL = "/rmi-" + SERVICE_NAME;

    String JSON_SERVICE_URL = SERVICE_URL + ".json";

}