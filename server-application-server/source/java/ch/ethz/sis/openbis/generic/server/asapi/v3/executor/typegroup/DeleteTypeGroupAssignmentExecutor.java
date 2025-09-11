/*
 *  Copyright ETH 2025 Zürich, Scientific IT Services
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package ch.ethz.sis.openbis.generic.server.asapi.v3.executor.typegroup;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.delete.TypeGroupAssignmentDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.ITypeGroupAssignmentId;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.IOperationContext;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.entity.AbstractDeleteEntityExecutor;
import ch.systemsx.cisd.common.exceptions.AuthorizationFailureException;
import ch.systemsx.cisd.openbis.generic.server.business.bo.ITypeGroupAssignmentTable;
import ch.systemsx.cisd.openbis.generic.shared.dto.PersonPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.SampleTypeTypeGroupsPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
public class DeleteTypeGroupAssignmentExecutor extends AbstractDeleteEntityExecutor<Void, ITypeGroupAssignmentId, SampleTypeTypeGroupsPE, TypeGroupAssignmentDeletionOptions>
        implements IDeleteTypeGroupAssignmentExecutor
{
    @Autowired
    private IMapSampleTypeTypeGroupsByIdExecutor mapTypeGroupAssignmentsByIdExecutor;

    @Autowired
    private ITypeGroupAssignmentAuthorizationExecutor authorizationExecutor;

    @Override
    protected Map<ITypeGroupAssignmentId, SampleTypeTypeGroupsPE> map(
            IOperationContext context, List<? extends ITypeGroupAssignmentId> entityIds, TypeGroupAssignmentDeletionOptions deletionOptions)
    {
        return mapTypeGroupAssignmentsByIdExecutor.map(context, entityIds);
    }

    @Override
    protected void checkAccess(IOperationContext context, ITypeGroupAssignmentId entityId, SampleTypeTypeGroupsPE entity)
    {
        if(entity.isManagedInternally() && isSystemUser(context.getSession()) == false)
        {
            throw new AuthorizationFailureException("Internal type group assignments can be managed only by the system user.");
        }
        authorizationExecutor.canDelete(context, entityId, entity);
    }

    @Override
    protected void updateModificationDateAndModifier(IOperationContext context, SampleTypeTypeGroupsPE entity)
    {
        // nothing to do
    }

    @Override
    protected Void delete(IOperationContext context, Collection<SampleTypeTypeGroupsPE> typeGroupAssignments, TypeGroupAssignmentDeletionOptions deletionOptions)
    {
        ITypeGroupAssignmentTable assignmentTable = businessObjectFactory.createTypeGroupAssignmentTable(context.getSession());
        for (SampleTypeTypeGroupsPE typeGroupAssignment : typeGroupAssignments)
        {
            assignmentTable.deleteById(typeGroupAssignment.getId());
        }
        return null;
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
