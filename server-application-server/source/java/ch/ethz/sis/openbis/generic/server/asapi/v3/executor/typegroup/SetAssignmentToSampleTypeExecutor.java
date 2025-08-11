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

import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.IEntityTypeId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.create.TypeGroupAssignmentCreation;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.IOperationContext;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.entity.AbstractSetEntityToOneRelationWithCustomIdExecutor;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.entity.IMapEntityTypeByIdExecutor;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.openbis.generic.shared.dto.*;
import ch.systemsx.cisd.openbis.generic.shared.dto.properties.EntityKind;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SetAssignmentToSampleTypeExecutor extends
        AbstractSetEntityToOneRelationWithCustomIdExecutor<TypeGroupAssignmentCreation, SampleTypeTypeGroupsPE, IEntityTypeId, EntityTypePE, SampleTypeTypeGroupsTechId>
        implements ISetAssignmentToSampleTypeExecutor
{
    @Autowired
    private IMapEntityTypeByIdExecutor mapEntityTypeByIdExecutor;

    @Override
    protected String getRelationName()
    {
        return "typegroupassignment-typegroup";
    }

    @Override
    protected IEntityTypeId getRelatedId(TypeGroupAssignmentCreation creation)
    {
        return creation.getSampleTypeId();
    }

    @Override
    protected Map<IEntityTypeId, EntityTypePE> map(
            IOperationContext context, List<IEntityTypeId> relatedIds)
    {
        return mapEntityTypeByIdExecutor.map(context, EntityKind.SAMPLE, relatedIds);
    }

    @Override
    protected void check(IOperationContext context, SampleTypeTypeGroupsPE entity, IEntityTypeId relatedId, EntityTypePE related)
    {
        if(entity.isManagedInternally() && !related.isManagedInternally())
        {
            throw new UserFailureException("Internal type group assignment can be performed only on internal sample types!");
        }
    }

    @Override
    protected void set(IOperationContext context, SampleTypeTypeGroupsPE entity, EntityTypePE related)
    {
        entity.setSampleType((SampleTypePE) related);
    }
}
