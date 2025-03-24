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
package ch.ethz.sis.openbis.generic.server.asapi.v3.executor.person;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.metadata.IUpdateMetaDataForEntityExecutor;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.pat.IUpdatePersonalAccessTokenExecutor;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.IPersonalAccessTokenDAO;
import ch.systemsx.cisd.openbis.generic.shared.IOpenBisSessionManager;
import ch.systemsx.cisd.openbis.generic.shared.dto.*;
import org.hibernate.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.id.IPersonId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.id.Me;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.id.PersonPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.update.PersonUpdate;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.IOperationContext;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.entity.AbstractUpdateEntityExecutor;
import ch.ethz.sis.openbis.generic.server.asapi.v3.helper.common.CommonUtils;
import ch.ethz.sis.openbis.generic.server.asapi.v3.helper.common.batch.MapBatch;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.openbis.generic.server.business.bo.DataAccessExceptionTranslator;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.IDAOFactory;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.IRoleAssignmentDAO;

/**
 * @author Franz-Josef Elmer
 */
@Component
public class UpdatePersonExecutor
        extends AbstractUpdateEntityExecutor<PersonUpdate, PersonPE, IPersonId, PersonPermId>
        implements IUpdatePersonExecutor
{
    @Autowired
    private IDAOFactory daoFactory;

    @Autowired
    private IPersonalAccessTokenDAO personalAccessTokenDAO;

    @Autowired
    private IOpenBisSessionManager sessionManager;

    @Autowired
    private IPersonAuthorizationExecutor authorizationExecutor;

    @Autowired
    private IMapPersonByIdExecutor mapPersonByIdExecutor;

    @Autowired
    private IUpdateHomeSpaceExecutor updateHomeSpaceExecutor;

    @Autowired
    private IUpdateWebAppSettingsExecutor updateWebAppSettingsExecutor;

    @Autowired
    private IUpdateMetaDataForEntityExecutor<PersonUpdate, PersonPE>
            updateMetaDataForEntityExecutor;

    @Autowired
    private IUpdatePersonalAccessTokenExecutor updatePersonalAccessTokenExecutor;

    @Override
    protected IPersonId getId(PersonUpdate update)
    {
        return update.getUserId();
    }

    @Override
    protected PersonPermId getPermId(PersonPE entity)
    {
        return new PersonPermId(entity.getUserId());
    }

    @Override
    protected void checkData(IOperationContext context, PersonUpdate update)
    {
        IPersonId personId = update.getUserId();
        if (personId == null || personId instanceof Me)
        {
            PersonPE person = context.getSession().tryGetPerson();
            if (person != null)
            {
                update.setUserId(new PersonPermId(person.getUserId()));
            } else
            {
                throw new UserFailureException("Person to be updated not specified.");
            }
        }
    }

    @Override
    protected void checkAccess(IOperationContext context, IPersonId id, PersonPE entity, PersonUpdate update)
    {
        if (entity.isActive() == false)
        {
            authorizationExecutor.canDeactivate(context);
        }
    }

    @Override
    protected void checkBusinessRules(IOperationContext context, IPersonId id, PersonPE entity, PersonUpdate update)
    {
        if(update.getExpiryDate().isModified())
        {
            if (entity.equals(context.getSession().tryGetPerson()))
            {
                throw new UserFailureException("You can not set expiry date to yourself. Ask another instance admin to do that for you.");
            }
            if(entity.isSystemUser() && update.getExpiryDate().getValue() != null)
            {
                throw new UserFailureException("System User must not have expiration date set!");
            }
        }
    }

    @Override
    protected void updateBatch(IOperationContext context, MapBatch<PersonUpdate, PersonPE> batch)
    {
        updateHomeSpaceExecutor.update(context, batch);
        updateWebAppSettingsExecutor.update(context, batch);
        updateMetaDataForEntityExecutor.update(context, batch);
        Set<Entry<PersonUpdate, PersonPE>> entrySet = batch.getObjects().entrySet();
        for (Entry<PersonUpdate, PersonPE> entry : entrySet)
        {
            PersonUpdate personUpdate = entry.getKey();
            PersonPE person = entry.getValue();

            if (personUpdate.isActive() != null && personUpdate.isActive().isModified() && personUpdate.isActive().getValue() != null)
            {
                if (person.isActive() == true && personUpdate.isActive().getValue() == false)
                {
                    deactivate(context, person);
                } else if (person.isActive() == false && personUpdate.isActive().getValue() == true)
                {
                    activate(context, person);
                }
            }
            if(personUpdate.getExpiryDate() != null && personUpdate.getExpiryDate().isModified())
            {
                person.setExpiryDate(personUpdate.getExpiryDate().getValue());
            }
        }
    }

    private void deactivate(IOperationContext context, PersonPE person)
    {
        authorizationExecutor.canDeactivate(context);
        if (person.equals(context.getSession().tryGetPerson()))
        {
            throw new UserFailureException("You can not deactivate yourself. Ask another instance admin to do that for you.");
        }
        person.setActive(false);
    }

    private void activate(IOperationContext context, PersonPE person)
    {
        authorizationExecutor.canActivate(context);
        person.setActive(true);
    }

    @Override
    protected void updateAll(IOperationContext context, MapBatch<PersonUpdate, PersonPE> batch)
    {
    }

    @Override
    protected Map<IPersonId, PersonPE> map(IOperationContext context, Collection<IPersonId> ids)
    {
        return mapPersonByIdExecutor.map(context, ids);
    }

    @Override
    protected List<PersonPE> list(IOperationContext context, Collection<Long> ids)
    {
        return CommonUtils.listPersons(daoFactory, ids);
    }

    @Override
    protected void save(IOperationContext context, List<PersonPE> entities, boolean clearCache)
    {
        for (PersonPE person : entities)
        {
            daoFactory.getPersonDAO().updatePerson(person);
        }
    }

    @Override
    protected void postSaveAction(IOperationContext context, Map<PersonUpdate, PersonPE> updatedMap)
    {
        List<PersonalAccessTokenSession> patSessions = personalAccessTokenDAO.listSessions();
        List<Session> sessions = sessionManager.getSessions();
        boolean updateSessions = false;
        for(Map.Entry<PersonUpdate, PersonPE> entry : updatedMap.entrySet())
        {
            PersonUpdate update = entry.getKey();
            if(update.getExpiryDate() != null && update.getExpiryDate().isModified())
            {
                PersonPE person = entry.getValue();
                for(Session session : sessions)
                {
                    if(person.equals(session.tryGetPerson()) && !person.equals(context.getSession().tryGetPerson()))
                    {
                        updateSessions = true;
                        break;
                    }
                }
                for(PersonalAccessTokenSession patSession : patSessions)
                {
                    if(patSession.getOwnerId().equals(person.getUserId()))
                    {
                        updateSessions = true;
                        break;
                    }
                }
            }
        }
        if(updateSessions)
        {
            sessionManager.updateAllSessions();
        }
    }

    @Override
    protected void handleException(DataAccessException e)
    {
        DataAccessExceptionTranslator.throwException(e, "person", null);
    }

}
