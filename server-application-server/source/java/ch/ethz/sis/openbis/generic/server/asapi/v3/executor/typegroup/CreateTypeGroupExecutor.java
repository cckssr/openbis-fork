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

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.id.IObjectId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.create.TypeGroupCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.TypeGroupId;
import ch.ethz.sis.openbis.generic.server.asapi.v3.context.IProgress;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.IOperationContext;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.entity.AbstractCreateEntityExecutor;
import ch.ethz.sis.openbis.generic.server.asapi.v3.helper.common.batch.CollectionBatch;
import ch.ethz.sis.openbis.generic.server.asapi.v3.helper.common.batch.CollectionBatchProcessor;
import ch.ethz.sis.openbis.generic.server.asapi.v3.helper.common.batch.MapBatch;
import ch.ethz.sis.openbis.generic.server.asapi.v3.helper.entity.progress.CreateProgress;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.openbis.generic.server.business.bo.DataAccessExceptionTranslator;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.IDAOFactory;
import ch.systemsx.cisd.openbis.generic.shared.dto.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

@Component
public class CreateTypeGroupExecutor extends AbstractCreateEntityExecutor<TypeGroupCreation, TypeGroupPE, TypeGroupId>
        implements ICreateTypeGroupExecutor
{

    @Autowired
    private ITypeGroupAuthorizationExecutor authorizationExecutor;

    @Autowired
    private IDAOFactory daoFactory;


    @Override
    protected IObjectId getId(TypeGroupPE entity)
    {
        return new TypeGroupId(entity.getName());
    }

    @Override
    protected void checkData(IOperationContext context, TypeGroupCreation creation)
    {
        if (StringUtils.isEmpty(creation.getName()))
        {
            throw new UserFailureException("Name cannot be empty.");
        }
    }

    @Override
    protected void checkAccess(IOperationContext context)
    {

    }

    @Override
    protected void checkAccess(IOperationContext context, TypeGroupPE entity)
    {
        authorizationExecutor.canCreate(context, entity);
    }

    @Override
    protected List<TypeGroupPE> createEntities(IOperationContext context,
            CollectionBatch<TypeGroupCreation> batch)
    {
        final List<TypeGroupPE> typeGroups = new LinkedList<>();

        new CollectionBatchProcessor<TypeGroupCreation>(context, batch)
        {
            @Override
            public void process(TypeGroupCreation object)
            {
                TypeGroupPE typeGroup = new TypeGroupPE();
                typeGroup.setName(object.getName());
                typeGroup.setManagedInternally(object.isManagedInternally());
                typeGroup.setMetaData(object.getMetaData());
                typeGroup.setRegistrator(context.getSession().tryGetPerson());
                typeGroups.add(typeGroup);
            }

            @Override
            public IProgress createProgress(TypeGroupCreation object, int objectIndex, int totalObjectCount)
            {
                return new CreateProgress(object, objectIndex, totalObjectCount);
            }
        };

        return typeGroups;
    }

    @Override
    protected TypeGroupId createPermId(IOperationContext context, TypeGroupPE entity)
    {
        return new TypeGroupId(entity.getName());
    }

    @Override
    protected void updateBatch(IOperationContext context,
            MapBatch<TypeGroupCreation, TypeGroupPE> batch)
    {

    }

    @Override
    protected void updateAll(IOperationContext context,
            MapBatch<TypeGroupCreation, TypeGroupPE> batch)
    {

    }

//    @Override
//    protected List<TypeGroupPE> list(IOperationContext context, Collection<String> ids)
//    {
//        return daoFactory.getTypeGroupDAO().findByNames(ids);
//    }

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
        DataAccessExceptionTranslator.throwException(e, "type group", null);
    }
}
