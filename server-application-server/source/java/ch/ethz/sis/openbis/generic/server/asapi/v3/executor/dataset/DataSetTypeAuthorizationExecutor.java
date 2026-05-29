/*
 * Copyright ETH 2008 - 2023 Zürich, Scientific IT Services
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
package ch.ethz.sis.openbis.generic.server.asapi.v3.executor.dataset;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.update.FieldUpdateValue;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.update.DataSetTypeUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.id.IPluginId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.id.PluginPermId;
import ch.systemsx.cisd.common.exceptions.AuthorizationFailureException;
import ch.systemsx.cisd.openbis.generic.shared.dto.DataSetTypePE;
import ch.systemsx.cisd.openbis.generic.shared.dto.EntityTypePE;
import ch.systemsx.cisd.openbis.generic.shared.dto.PersonPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.Session;
import org.springframework.stereotype.Component;

import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.IOperationContext;
import ch.systemsx.cisd.openbis.generic.server.authorization.annotation.Capability;
import ch.systemsx.cisd.openbis.generic.server.authorization.annotation.RolesAllowed;
import ch.systemsx.cisd.openbis.generic.shared.DatabaseCreateOrDeleteModification;
import ch.systemsx.cisd.openbis.generic.shared.DatabaseUpdateModification;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.DatabaseModificationKind.ObjectKind;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.RoleWithHierarchy;

import java.util.Map;

/**
 * @author pkupczyk
 */
@Component
public class DataSetTypeAuthorizationExecutor implements IDataSetTypeAuthorizationExecutor
{

    @Override
    @RolesAllowed({ RoleWithHierarchy.INSTANCE_ADMIN, RoleWithHierarchy.INSTANCE_ETL_SERVER })
    @Capability("CREATE_DATASET_TYPE")
    @DatabaseCreateOrDeleteModification(value = ObjectKind.DATASET_TYPE)
    public void canCreate(IOperationContext context, DataSetTypePE entityTypePE)
    {
    }

    @Override
    @RolesAllowed({ RoleWithHierarchy.PROJECT_OBSERVER, RoleWithHierarchy.SPACE_ETL_SERVER })
    @Capability("GET_DATASET_TYPE")
    public void canGet(IOperationContext context)
    {
    }

    @Override
    @RolesAllowed({ RoleWithHierarchy.PROJECT_OBSERVER, RoleWithHierarchy.SPACE_ETL_SERVER })
    @Capability("SEARCH_DATASET_TYPE")
    public void canSearch(IOperationContext context)
    {
    }

    @Override
    @RolesAllowed({ RoleWithHierarchy.INSTANCE_ADMIN })
    @Capability("UPDATE_DATASET_TYPE")
    @DatabaseUpdateModification(value = ObjectKind.DATASET_TYPE)
    public void canUpdate(IOperationContext context, DataSetTypePE entityType,
            DataSetTypeUpdate update)
    {
        if (entityType.isManagedInternally() && isSystemUser(context.getSession()) == false)
        {
            boolean isModified =
                    isFieldUpdated(update.getDescription(), entityType.getDescription()) ||
                            isFieldUpdated(update.getMainDataSetPath(),
                                    entityType.getMainDataSetPath()) ||
                            isFieldUpdated(update.getMainDataSetPattern(),
                                    entityType.getMainDataSetPattern()) ||
                            isFieldUpdated(update.isDisallowDeletion(),
                                    entityType.isDeletionDisallow());

            if (!isModified && update.getValidationPluginId() != null && update.getValidationPluginId()
                    .isModified())
            {
                IPluginId updatePluginId = update.getValidationPluginId().getValue();
                if (updatePluginId == null)
                {
                    isModified = entityType.getValidationScript() != null;
                } else
                {
                    if (entityType.getValidationScript() == null)
                    {
                        isModified = true;
                    } else
                    {
                        IPluginId permId =
                                new PluginPermId(entityType.getValidationScript().getPermId());
                        isModified = !permId.equals(updatePluginId);
                    }
                }
            }
            if (!isModified && update.getMetaData() != null && update.getMetaData().hasActions())
            {
                if (!update.getMetaData().getRemoved().isEmpty() || !update.getMetaData().getAdded()
                        .isEmpty())
                {
                    isModified = true;
                } else
                {
                    for (Map<String, String> m : update.getMetaData().getSet())
                    {
                        isModified = isModified || !m.equals(entityType.getMetaData());
                    }
                }
            }

            if (isModified)
            {
                throw new AuthorizationFailureException(
                        "Internal entity type fields can be managed only by the system user.");
            }
        }
    }

    @Override
    @RolesAllowed({ RoleWithHierarchy.INSTANCE_ADMIN })
    @Capability("DELETE_DATASET_TYPE")
    @DatabaseCreateOrDeleteModification(value = ObjectKind.DATASET_TYPE)
    public void canDelete(IOperationContext context, EntityTypePE entityTypePE)
    {
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

    private boolean isFieldUpdated(FieldUpdateValue<?> field, Object currentValue)
    {
        if (field != null && field.isModified())
        {
            if (currentValue != null)
            {
                return !currentValue.equals(field.getValue());
            } else
            {
                return field.getValue() != null;
            }
        }
        return false;
    }

}
