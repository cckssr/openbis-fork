package ch.ethz.sis.openbis.generic.server.asapi.v3.executor.entity;

import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.IOperationContext;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.common.IObjectAuthorizationExecutor;
import ch.systemsx.cisd.openbis.generic.shared.dto.EntityTypePE;

public interface ICreateEntityTypeAuthorizationExecutor<TYPE_PE extends EntityTypePE> extends
        IObjectAuthorizationExecutor
{
    void canCreate(IOperationContext context, TYPE_PE entityTypePE);
}
