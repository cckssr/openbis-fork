/*
 * Copyright ETH 2017 - 2023 Zürich, Scientific IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.ethz.sis.openbis.generic.server.asapi.v3.executor.entity;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import ch.systemsx.cisd.common.exceptions.AuthorizationFailureException;
import ch.systemsx.cisd.openbis.generic.shared.dto.ExperimentTypePE;
import ch.systemsx.cisd.openbis.generic.shared.dto.PersonPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.delete.DataSetTypeDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.deletion.AbstractObjectDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.IEntityTypeId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.delete.ExperimentTypeDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.delete.SampleTypeDeletionOptions;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.IOperationContext;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.dataset.IDataSetTypeAuthorizationExecutor;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.experiment.IExperimentTypeAuthorizationExecutor;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.sample.ISampleTypeAuthorizationExecutor;
import ch.systemsx.cisd.openbis.generic.server.business.bo.IEntityTypeBO;
import ch.systemsx.cisd.openbis.generic.shared.dto.EntityTypePE;
import ch.systemsx.cisd.openbis.generic.shared.dto.properties.EntityKind;

/**
 * @author Franz-Josef Elmer
 */
@Component
public class DeleteEntityTypeExecutor extends AbstractDeleteEntityExecutor<Void, IEntityTypeId, EntityTypePE, AbstractObjectDeletionOptions<?>>
        implements IDeleteEntityTypeExecutor
{
    @Autowired
    private IMapEntityTypeByIdExecutor mapEntityTypeByIdExecutor;

    @Autowired
    private IDataSetTypeAuthorizationExecutor dataSetTypeAuthorizationExecutor;

    @Autowired
    private IExperimentTypeAuthorizationExecutor experimentTypeAuthorizationExecutor;

    @Autowired
    private ISampleTypeAuthorizationExecutor sampleTypeAuthorizationExecutor;

    @Override
    protected Map<IEntityTypeId, EntityTypePE> map(IOperationContext context, List<? extends IEntityTypeId> entityTypeIds,
            AbstractObjectDeletionOptions<?> deletionOptions)
    {
        EntityKind entityKind;

        if (deletionOptions instanceof ExperimentTypeDeletionOptions)
        {
            entityKind = EntityKind.EXPERIMENT;
        } else if (deletionOptions instanceof SampleTypeDeletionOptions)
        {
            entityKind = EntityKind.SAMPLE;
        } else if (deletionOptions instanceof DataSetTypeDeletionOptions)
        {
            entityKind = EntityKind.DATA_SET;
        } else
        {
            throw new IllegalArgumentException("Unsupported deletion options: " + (deletionOptions != null ? deletionOptions.getClass() : null));
        }

        return mapEntityTypeByIdExecutor.map(context, entityKind, entityTypeIds);
    }

    @Override
    protected void checkAccess(IOperationContext context, IEntityTypeId entityId, EntityTypePE entity)
    {
        checkEntityType(context.getSession(), entity);
        switch (entity.getEntityKind())
        {
            case DATA_SET:
                dataSetTypeAuthorizationExecutor.canDelete(context, entity);
                break;
            case EXPERIMENT:
                experimentTypeAuthorizationExecutor.canDelete(context, entity);
                break;
            case SAMPLE:
                sampleTypeAuthorizationExecutor.canDelete(context, entity);
                break;
        }
    }

    @Override
    protected void updateModificationDateAndModifier(IOperationContext context, EntityTypePE entity)
    {
    }

    @Override
    protected Void delete(IOperationContext context, Collection<EntityTypePE> entities, AbstractObjectDeletionOptions<?> deletionOptions)
    {
        for (EntityTypePE entityType : entities)
        {
            IEntityTypeBO bo = businessObjectFactory.createEntityTypeBO(context.getSession());
            bo.load(entityType.getEntityKind(), entityType.getCode());
            bo.delete();
        }
        return null;
    }

    private void checkEntityType(Session session, EntityTypePE entity)
    {
        if(entity.isManagedInternally() && isSystemUser(session) == false)
        {
            throw new AuthorizationFailureException("Internal entity types can be managed only by the system user.");
        }
    }

    private boolean isSystemUser(Session session)
    {
        PersonPE user = session.tryGetPerson();

        if (user == null)
        {
            throw new AuthorizationFailureException("Could not check access because the current session does not have any user assigned.");
        } else
        {
            return user.isSystemUser();
        }
    }

}
