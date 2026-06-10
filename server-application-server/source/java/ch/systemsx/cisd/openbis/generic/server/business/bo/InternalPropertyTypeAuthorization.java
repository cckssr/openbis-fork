/*
 * Copyright ETH 2020 - 2023 Zürich, Scientific IT Services
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
package ch.systemsx.cisd.openbis.generic.server.business.bo;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.update.PropertyTypeUpdate;
import ch.systemsx.cisd.common.exceptions.AuthorizationFailureException;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.IPropertyTypeUpdates;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.NewETPTAssignment;
import ch.systemsx.cisd.openbis.generic.shared.dto.*;

public class InternalPropertyTypeAuthorization
{

    public void canCreatePropertyType(Session session, PropertyTypePE propertyType)
    {
        checkPropertyType(session, propertyType);
    }

    public void canUpdatePropertyType(Session session, PropertyTypePE propertyType, PropertyTypeUpdate update)
    {
        checkPropertyType(session, propertyType);
        // internal flag change check
        if((!propertyType.isManagedInternally() && update.getManagedInternally().isModified() && update.getManagedInternally().getValue() == true) ||
           ( propertyType.isManagedInternally() && update.getManagedInternally().isModified() && update.getManagedInternally().getValue() == false)) {
            if(isSystemUser(session) == false) {
                throw new AuthorizationFailureException(
                        "Property types can be made internal only by the system user.");
            }
        }
    }

    public void canUpdatePropertyType(Session session, PropertyTypePE propertyType, IPropertyTypeUpdates update)
    {
        checkPropertyType(session, propertyType);
        // internal flag change check
        if((!propertyType.isManagedInternally() && update.isManagedInternally() == true) ||
           (propertyType.isManagedInternally()  && update.isManagedInternally() == false)) {
            if(isSystemUser(session) == false) {
                throw new AuthorizationFailureException(
                        "Property types can be made internal only by the system user.");
            }
        }
    }

    public void canDeletePropertyType(Session session, PropertyTypePE propertyType)
    {
        checkPropertyType(session, propertyType);
    }

    public void canCreatePropertyAssignment(Session session, EntityTypePE entityType, PropertyTypePE propertyType, EntityTypePropertyTypePE propertyAssignment)
    {
        if(propertyAssignment.isManagedInternallyNamespace())
        {
            if(propertyType.isManagedInternally())
            {
                if(isSystemUser(session) == false)
                {
                    throw new AuthorizationFailureException(
                            "Internal property assignments can be managed only by the system user.");
                }
            } else {
                throw new UserFailureException(
                        "Internal property assignments can be added only to internal property types.");
            }
        } else if(entityType.isManagedInternally() && propertyAssignment.isMandatory() && isSystemUser(session) == false) {
            throw new AuthorizationFailureException(
                    "Mandatory property assignments for internal types can be managed only by the system user.");
        }
    }

    public void canUpdateInternallyManagedFlag(Session session, EntityTypePropertyTypePE propertyAssignment, NewETPTAssignment assignmentUpdates) {
        if((!propertyAssignment.isManagedInternallyNamespace() && assignmentUpdates.isManagedInternally())
                || (propertyAssignment.isManagedInternallyNamespace() && !assignmentUpdates.isManagedInternally())) {
            if(isSystemUser(session) == false) {
                throw new AuthorizationFailureException("Internal property assignment flag can be managed only by the system user.");
            }
        }
    }

    public void canUpdatePropertyAssignment(Session session, EntityTypePE entityType, PropertyTypePE propertyType, EntityTypePropertyTypePE propertyAssignment)
    {
        checkPropertyAssignment(session, entityType, propertyType, propertyAssignment);
    }

    public void canDeletePropertyAssignment(Session session, EntityTypePE entityType, PropertyTypePE propertyType, EntityTypePropertyTypePE propertyAssignment)
    {
        checkPropertyAssignment(session, entityType, propertyType, propertyAssignment);
    }

    private void checkPropertyType(Session session, PropertyTypePE propertyType)
    {
        if (propertyType.isManagedInternally() && isSystemUser(session) == false)
        {
            throw new AuthorizationFailureException("Internal property types can be managed only by the system user.");
        }
    }

    private void checkPropertyAssignment(Session session, EntityTypePE entityType, PropertyTypePE propertyType, EntityTypePropertyTypePE propertyAssignment)
    {
        boolean isNotASystemUser = isSystemUser(session) == false;
        if (propertyAssignment.isManagedInternallyNamespace() && isNotASystemUser)
        {
            throw new AuthorizationFailureException(
                    "Internal property assignments created by the system user for internal property types can be managed only by the system user.");
        }
        if (entityType.isManagedInternally() && propertyAssignment.isMandatory() && isNotASystemUser)
        {
            throw new AuthorizationFailureException(
                    "Mandatory property assignments for internal types can be can be managed only by the system user.");
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
