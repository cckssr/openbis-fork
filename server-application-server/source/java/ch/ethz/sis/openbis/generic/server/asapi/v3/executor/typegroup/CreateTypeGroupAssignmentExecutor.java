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
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.IEntityTypeId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.create.TypeGroupAssignmentCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.ITypeGroupId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.TypeGroupAssignmentId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.TypeGroupId;
import ch.ethz.sis.openbis.generic.server.asapi.v3.context.IProgress;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.IOperationContext;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.entity.AbstractCreateEntityWithCustomIdExecutor;
import ch.ethz.sis.openbis.generic.server.asapi.v3.helper.common.batch.CollectionBatch;
import ch.ethz.sis.openbis.generic.server.asapi.v3.helper.common.batch.CollectionBatchProcessor;
import ch.ethz.sis.openbis.generic.server.asapi.v3.helper.common.batch.MapBatch;
import ch.ethz.sis.openbis.generic.server.asapi.v3.helper.entity.progress.CreateProgress;
import ch.systemsx.cisd.common.exceptions.AuthorizationFailureException;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.openbis.generic.server.business.bo.DataAccessExceptionTranslator;
import ch.systemsx.cisd.openbis.generic.shared.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CreateTypeGroupAssignmentExecutor extends AbstractCreateEntityWithCustomIdExecutor<TypeGroupAssignmentCreation, SampleTypeTypeGroupsPE, TypeGroupAssignmentId, SampleTypeTypeGroupsTechId>
        implements ICreateTypeGroupAssignmentExecutor
{

    @Autowired
    ISetAssignmentToSampleTypeExecutor setAssignmentToSampleTypeExecutor;

    @Autowired
    ISetAssignmentToTypeGroupExecutor setAssignmentToTypeGroupExecutor;

    @Autowired
    ITypeGroupAssignmentAuthorizationExecutor authorizationExecutor;

    @Override
    protected IObjectId getId(SampleTypeTypeGroupsPE entity)
    {
        IEntityTypeId sampleTypeId = new EntityTypePermId(entity.getSampleType().getPermId(), EntityKind.SAMPLE);
        ITypeGroupId typeGroupId = new TypeGroupId(entity.getTypeGroup().getCode());
        return new TypeGroupAssignmentId(sampleTypeId, typeGroupId);
    }

    @Override
    protected void checkData(IOperationContext context, TypeGroupAssignmentCreation creation)
    {
        if (creation.getSampleTypeId() == null)
        {
            throw new UserFailureException("Sample type needs to be specified.");
        }
        if (creation.getTypeGroupId() == null)
        {
            throw new UserFailureException("Type group needs to be specified.");
        }
    }

    @Override
    protected void checkAccess(IOperationContext context)
    {
    }

    @Override
    protected void checkAccess(IOperationContext context, SampleTypeTypeGroupsPE entity)
    {
        authorizationExecutor.canCreate(context, entity);
        if (!isSystemUser(context.getSession()))
        {
            boolean internalTypeGroup = entity.getTypeGroup().isManagedInternally();
            boolean internalSampleType = entity.getSampleType().isManagedInternally();
            if(entity.isManagedInternally() && !(internalTypeGroup && internalSampleType) ) {
                throw new AuthorizationFailureException(
                        "Internal type group assignments can be managed only by the system user.");
            }
        }
    }

    @Override
    protected List<SampleTypeTypeGroupsPE> createEntities(IOperationContext context, CollectionBatch<TypeGroupAssignmentCreation> batch)
    {
        final List<SampleTypeTypeGroupsPE> typeGroupAssignments = new LinkedList<>();
        new CollectionBatchProcessor<TypeGroupAssignmentCreation>(context, batch)
        {
            @Override
            public void process(TypeGroupAssignmentCreation creation)
            {
                SampleTypeTypeGroupsPE typeGroupAssignment = new SampleTypeTypeGroupsPE();
                typeGroupAssignment.setRegistrator(context.getSession().tryGetCreatorPerson());
                typeGroupAssignment.setManagedInternally(creation.isManagedInternally());
                typeGroupAssignments.add(typeGroupAssignment);
            }

            @Override
            public IProgress createProgress(TypeGroupAssignmentCreation object, int objectIndex, int totalObjectCount)
            {
                return new CreateProgress(object, objectIndex, totalObjectCount);
            }
        };
        return typeGroupAssignments;
    }

    @Override
    protected TypeGroupAssignmentId createPermId(IOperationContext context, SampleTypeTypeGroupsPE entity)
    {
        IEntityTypeId sampleTypeId = new EntityTypePermId(entity.getSampleType().getPermId(), EntityKind.SAMPLE);
        ITypeGroupId typeGroupId = new TypeGroupId(entity.getTypeGroup().getCode());
        return new TypeGroupAssignmentId(sampleTypeId, typeGroupId);
    }

    @Override
    protected void updateBatch(IOperationContext context, MapBatch<TypeGroupAssignmentCreation, SampleTypeTypeGroupsPE> batch)
    {
        setAssignmentToTypeGroupExecutor.set(context, batch);
        setAssignmentToSampleTypeExecutor.set(context, batch);
    }

    @Override
    protected void updateAll(IOperationContext context, MapBatch<TypeGroupAssignmentCreation, SampleTypeTypeGroupsPE> batch)
    {
    }



    @Override
    protected List<SampleTypeTypeGroupsPE> list(IOperationContext context, Collection<SampleTypeTypeGroupsTechId> ids)
    {
        List<SampleTypeTypeGroupsPE> result = new ArrayList<>();
        List<SampleTypeTypeGroupsPE> entities = daoFactory.getTypeGroupAssignmentDAO().listAllEntities();
        for (SampleTypeTypeGroupsPE typeGroupAssignment : entities)
        {
            if (ids.contains(typeGroupAssignment.getId()))
            {
                result.add(typeGroupAssignment);
            }
        }
        return result;
    }

    @Override
    protected void save(IOperationContext context, List<SampleTypeTypeGroupsPE> entities, boolean clearCache)
    {
        for (SampleTypeTypeGroupsPE typeGroupAssignment : entities)
        {
            daoFactory.getTypeGroupAssignmentDAO().createTypeGroupAssignment(typeGroupAssignment);
        }
    }

    @Override
    protected void handleException(DataAccessException e)
    {
        DataAccessExceptionTranslator.throwException(e, "type group assignment", null);
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
