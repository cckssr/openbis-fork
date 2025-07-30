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

import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.delete.TypeGroupDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.ITypeGroupId;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.IOperationContext;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.entity.AbstractDeleteEntityExecutor;
import ch.systemsx.cisd.openbis.generic.server.business.bo.ITypeGroupBO;
import ch.systemsx.cisd.openbis.generic.shared.basic.TechId;
import ch.systemsx.cisd.openbis.generic.shared.dto.TypeGroupPE;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
public class DeleteTypeGroupExecutor extends AbstractDeleteEntityExecutor<Void, ITypeGroupId, TypeGroupPE, TypeGroupDeletionOptions>
        implements IDeleteTypeGroupExecutor
{
    @Autowired
    private IMapTypeGroupByNameExecutor mapTypeGroupByIdExecutor;

    @Autowired
    private ITypeGroupAuthorizationExecutor authorizationExecutor;

    @Override
    protected Map<ITypeGroupId, TypeGroupPE> map(
            IOperationContext context, List<? extends ITypeGroupId> entityIds, TypeGroupDeletionOptions deletionOptions)
    {
        return mapTypeGroupByIdExecutor.map(context, entityIds);
    }

    @Override
    protected void checkAccess(IOperationContext context, ITypeGroupId entityId, TypeGroupPE entity)
    {
        authorizationExecutor.canDelete(context, entityId, entity);
    }

    @Override
    protected void updateModificationDateAndModifier(IOperationContext context, TypeGroupPE entity)
    {
        // nothing to do
    }

    @Override
    protected Void delete(IOperationContext context, Collection<TypeGroupPE> typeGroups, TypeGroupDeletionOptions deletionOptions)
    {
        ITypeGroupBO typeGroupBO = businessObjectFactory.createTypeGroupBO(context.getSession());
        for (TypeGroupPE typeGroup : typeGroups)
        {
            typeGroupBO.deleteByTechId(new TechId(typeGroup.getId()));
        }
        return null;
    }
}
