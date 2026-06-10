package ch.ethz.sis.openbis.generic.server.asapi.v3.executor.entity;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.update.IEntityTypeUpdate;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.IOperationContext;
import ch.systemsx.cisd.common.exceptions.AuthorizationFailureException;
import ch.systemsx.cisd.openbis.generic.server.authorization.annotation.Capability;
import ch.systemsx.cisd.openbis.generic.server.authorization.annotation.RolesAllowed;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.RoleWithHierarchy;
import ch.systemsx.cisd.openbis.generic.shared.dto.EntityTypePE;
import ch.systemsx.cisd.openbis.generic.shared.dto.PersonPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.Session;
import org.springframework.stereotype.Component;

@Component
public class UpdateEntityTypeAuthorizationExecutor<UPDATE extends IEntityTypeUpdate, TYPE_PE extends EntityTypePE>
        implements IUpdateEntityTypeAuthorizationExecutor<UPDATE, TYPE_PE>
{

    @Override
    @RolesAllowed({ RoleWithHierarchy.INSTANCE_ADMIN })
    @Capability("UPDATE_ENTITY_TYPE")
    public void canUpdate(IOperationContext context, TYPE_PE entityTypePE, UPDATE update)
    {
        if(!entityTypePE.isManagedInternally() &&
                update.getManagedInternally().isModified() &&
                update.getManagedInternally().getValue() == true &&
                isSystemUser(context.getSession()) == false) {
            throw new AuthorizationFailureException("Entity type can be made internal only by the system user.");
        }
    }

    private boolean isSystemUser(Session session)
    {
        PersonPE user = session.tryGetPerson();

        if (user == null)
        {
            throw new AuthorizationFailureException(
                    "Could not check access because the current session does not have any user assigned.");
        } else
        {
            return user.isSystemUser();
        }
    }
}
