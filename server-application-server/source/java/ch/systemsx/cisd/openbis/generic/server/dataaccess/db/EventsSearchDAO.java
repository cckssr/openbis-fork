/*
 * Copyright ETH 2021 - 2023 Zürich, Scientific IT Services
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
package ch.systemsx.cisd.openbis.generic.server.dataaccess.db;

import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.systemsx.cisd.common.reflection.MethodUtils;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.IEventsSearchDAO;
import ch.systemsx.cisd.openbis.generic.shared.dto.EventPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.EventType;
import ch.systemsx.cisd.openbis.generic.shared.dto.EventsSearchPE;
import ch.ethz.sis.shared.log.classic.impl.Logger;
import org.hibernate.SessionFactory;


import java.util.Date;

/**
 * Data access object for {@link EventsSearchPE}.
 *
 * @author pkupczyk
 */
public class EventsSearchDAO extends AbstractGenericEntityDAO<EventsSearchPE> implements IEventsSearchDAO
{
    private static final Class<EventsSearchPE> ENTITY_CLASS = EventsSearchPE.class;

    private static final Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION, EventsSearchPE.class);

    public EventsSearchDAO(SessionFactory sessionFactory)
    {
        super(sessionFactory, ENTITY_CLASS, null);
    }

    @Override public void createOrUpdate(EventsSearchPE eventsSearchPE)
    {
        doExecute(session -> {
            session.saveOrUpdate(eventsSearchPE);
            return null;
        });
    }

    @Override public Date getLastTimestamp(EventType eventType, EventPE.EntityType entityType)
    {
        Date lastTimestamp = doExecute(session->
                session.createQuery(
                        "select max(e.registrationTimestamp) " +
                                "from EventsSearchPE e " +
                                "where e.eventType = :evt and e.entityType = :ent",
                        Date.class)
                .setParameter("evt", eventType)
                .setParameter("ent", entityType)
                .uniqueResultOptional()
                .orElse(null));

        if (operationLog.isDebugEnabled())
        {
            operationLog.debug(String.format(
                    "%s(%s, %s): last timestamp = %s.", MethodUtils.getCurrentMethod().getName(), eventType, entityType, lastTimestamp));
        }

        return lastTimestamp;
    }
}
