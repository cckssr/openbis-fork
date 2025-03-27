package ch.ethz.sis.afsserver.server.archiving;

import ch.systemsx.cisd.common.exceptions.NotImplementedException;
import ch.systemsx.cisd.openbis.dss.generic.shared.api.v1.IDssService;
import ch.systemsx.cisd.openbis.dss.generic.shared.api.v1.IDssServiceFactory;

public class DssServiceFactory implements IDssServiceFactory
{
    @Override public IDssService getService(final String baseURL)
    {
        throw new NotImplementedException();
    }
}
