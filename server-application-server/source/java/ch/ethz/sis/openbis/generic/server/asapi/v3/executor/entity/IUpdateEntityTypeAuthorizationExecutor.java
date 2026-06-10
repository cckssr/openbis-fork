package ch.ethz.sis.openbis.generic.server.asapi.v3.executor.entity;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.update.IEntityTypeUpdate;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.IOperationContext;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.common.IObjectAuthorizationExecutor;
import ch.systemsx.cisd.openbis.generic.shared.dto.EntityTypePE;

public interface IUpdateEntityTypeAuthorizationExecutor<UPDATE extends IEntityTypeUpdate, TYPE_PE extends EntityTypePE> extends IObjectAuthorizationExecutor
{
    void canUpdate(IOperationContext context, TYPE_PE entityTypePE, UPDATE update);
}
