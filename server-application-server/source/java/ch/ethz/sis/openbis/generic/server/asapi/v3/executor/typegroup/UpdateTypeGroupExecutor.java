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

import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.ITypeGroupId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.TypeGroupId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.update.TypeGroupUpdate;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.IOperationContext;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.entity.AbstractUpdateEntityExecutor;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.metadata.IUpdateMetaDataForEntityExecutor;
import ch.ethz.sis.openbis.generic.server.asapi.v3.helper.common.batch.MapBatch;
import ch.systemsx.cisd.common.exceptions.AuthorizationFailureException;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.openbis.generic.server.business.bo.DataAccessExceptionTranslator;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.IDAOFactory;
import ch.systemsx.cisd.openbis.generic.shared.dto.*;
import ch.systemsx.cisd.openbis.generic.shared.util.RelationshipUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class UpdateTypeGroupExecutor extends AbstractUpdateEntityExecutor<TypeGroupUpdate, TypeGroupPE, ITypeGroupId, TypeGroupId>
        implements IUpdateTypeGroupExecutor
{
    @Autowired
    private IDAOFactory daoFactory;

    @Autowired
    private IMapTypeGroupByNameExecutor mapTypeGroupByName;

    @Autowired
    private ITypeGroupAuthorizationExecutor authorizationExecutor;

    @Autowired
    private IUpdateMetaDataForEntityExecutor<TypeGroupUpdate, TypeGroupPE> updateMetaDataExecutor;


    @Override
    protected ITypeGroupId getId(TypeGroupUpdate update)
    {
        return update.getTypeGroupId();
    }

    @Override
    protected TypeGroupId getPermId(TypeGroupPE entity)
    {
        return new TypeGroupId(entity.getCode());
    }

    @Override
    protected void checkData(IOperationContext context, TypeGroupUpdate update)
    {
        if(update.getTypeGroupId() == null) {
            throw new UserFailureException("Type Group id can not be null");
        }
        if (update.getCode() != null && update.getCode().isModified())
        {
            if(update.getCode().getValue() == null || update.getCode().getValue().isBlank()) {
                throw new UserFailureException("Type Group code cannot be empty.");
            }
        }
    }

    @Override
    protected void checkAccess(IOperationContext context, ITypeGroupId id, TypeGroupPE entity,
            TypeGroupUpdate update)
    {
        authorizationExecutor.canUpdate(context, update, entity);
    }

    @Override
    protected void checkBusinessRules(IOperationContext context, ITypeGroupId id, TypeGroupPE entity, TypeGroupUpdate update) {
        if(entity.isManagedInternally() && update.getManagedInternally().isModified() && update.getManagedInternally().getValue() == false) {
            for(SampleTypeTypeGroupsPE assignments : entity.getTypeGroupAssignments()) {
                if(assignments.isManagedInternally()) {
                    throw new UserFailureException("Type Group can not be made non-internal if it has internal assignments!");
                }
            }
        }
    }

    @Override
    protected void updateBatch(IOperationContext context,
            MapBatch<TypeGroupUpdate, TypeGroupPE> batch)
    {
        for (Map.Entry<TypeGroupUpdate, TypeGroupPE> entry : batch.getObjects().entrySet())
        {
            TypeGroupUpdate update = entry.getKey();
            TypeGroupPE typeGroup = entry.getValue();

            if (update.getCode() != null && update.getCode().isModified())
            {
                typeGroup.setCode(update.getCode().getValue());
            }
            if(update.getManagedInternally() != null && update.getManagedInternally().isModified()) {
                typeGroup.setManagedInternally(update.getManagedInternally().getValue());
            }
        }

        updateMetaDataExecutor.update(context, batch);

        PersonPE person = context.getSession().tryGetPerson();
        Date timeStamp = daoFactory.getTransactionTimestamp();

        for (TypeGroupPE entity : batch.getObjects().values())
        {
            RelationshipUtils.updateModificationDateAndModifier(entity, person, timeStamp);
        }
    }

    @Override
    protected void updateAll(IOperationContext context,
            MapBatch<TypeGroupUpdate, TypeGroupPE> batch)
    {
        // nothing to do
    }

    @Override
    protected Map<ITypeGroupId, TypeGroupPE> map(IOperationContext context,
            Collection<ITypeGroupId> ids)
    {
        return mapTypeGroupByName.map(context, ids);
    }

    @Override
    protected List<TypeGroupPE> list(IOperationContext context, Collection<Long> ids)
    {
        return daoFactory.getTypeGroupDAO().findByIds(ids);
    }

    @Override
    protected void save(IOperationContext context, List<TypeGroupPE> entities, boolean clearCache)
    {
        for (TypeGroupPE entity : entities)
        {
            daoFactory.getTypeGroupDAO().createOrUpdate(entity);
        }
    }

    @Override
    protected void handleException(DataAccessException e)
    {
        DataAccessExceptionTranslator.throwException(e, "space", null);
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
