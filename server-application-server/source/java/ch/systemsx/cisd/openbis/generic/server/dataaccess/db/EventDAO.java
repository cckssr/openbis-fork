/*
 * Copyright ETH 2009 - 2023 Zürich, Scientific IT Services
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
import ch.systemsx.cisd.openbis.generic.server.dataaccess.IEventDAO;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.db.deletion.EntityHistoryCreator;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.event.DeleteDataSetEventParser;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.DeletedDataSet;
import ch.systemsx.cisd.openbis.generic.shared.dto.EventPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.EventPE.EntityType;
import ch.systemsx.cisd.openbis.generic.shared.dto.EventType;
import ch.ethz.sis.shared.log.classic.impl.Logger;

import org.hibernate.SessionFactory;
import org.springframework.jdbc.support.JdbcAccessor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Data access object for {@link EventPE}.
 *
 * @author Piotr Buczek
 */
public class EventDAO extends AbstractGenericEntityDAO<EventPE> implements IEventDAO
{
    private static final Class<EventPE> ENTITY_CLASS = EventPE.class;

    /**
     * This logger does not output any SQL statement. If you want to do so, you had better set an appropriate debugging level for class
     * {@link JdbcAccessor}. </p>
     */
    private static final Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION,
            EventPE.class);

    public EventDAO(SessionFactory sessionFactory, EntityHistoryCreator historyCreator)
    {
        super(sessionFactory, ENTITY_CLASS, historyCreator);
    }

    @Override
    public EventPE tryFind(String identifier, EntityType entityType, EventType eventType)
    {
        assert identifier != null : "Unspecified identifier.";
        assert entityType != null : "Unspecified entityType.";
        assert eventType != null : "Unspecified eventType.";

        String identifiersInternal = "%" + identifier + "%";

        EventPE unique = currentSession()
                .createQuery(
                        "from EventPE e " +
                                "where e.identifiersInternal like :identifiersInternal " +
                                "and e.entityType = :entityType " +
                                "and e.eventType = :eventType",
                        EventPE.class)
                .setParameter("identifiersInternal", identifiersInternal)
                .setParameter("entityType", entityType)
                .setParameter("eventType", eventType)
                .uniqueResultOptional()
                .orElse(null);

        final EventPE result = tryGetEntity(unique);
        if (operationLog.isDebugEnabled())
        {
            String methodName = MethodUtils.getCurrentMethod().getName();
            operationLog.debug(String.format("%s: '%s'.", methodName, result));
        }
        return result;
    }

    @Override
    public List<DeletedDataSet> listDeletedDataSets(Long lastSeenDeletionEventIdOrNull,
            Date maxDeletionDataOrNull)
    {

        StringBuilder hql = new StringBuilder(
                "from EventPE e where e.eventType = :evt and e.entityType = :ent"
        );
        if (lastSeenDeletionEventIdOrNull != null) {
            hql.append(" and e.id > :lastId");
        }
        if (maxDeletionDataOrNull != null) {
            hql.append(" and e.registrationDate < :maxDate");
        }

        var query = currentSession().createQuery(hql.toString(), EventPE.class)
                .setParameter("evt", EventType.DELETION)
                .setParameter("ent", EntityType.DATASET);

        if (lastSeenDeletionEventIdOrNull != null) {
            query.setParameter("lastId", lastSeenDeletionEventIdOrNull);
        }
        if (maxDeletionDataOrNull != null) {
            query.setParameter("maxDate", maxDeletionDataOrNull);
        }

        List<EventPE> list = query.list();

        if (operationLog.isDebugEnabled())
        {
            String lastDesc =
                    lastSeenDeletionEventIdOrNull == null ? "all" : "id > "
                            + lastSeenDeletionEventIdOrNull;
            operationLog.debug(String.format(
                    "%s(%s): %d data set deletion events(s) have been found.", MethodUtils
                            .getCurrentMethod().getName(), lastDesc, list.size()));
        }

        ArrayList<DeletedDataSet> result = new ArrayList<DeletedDataSet>();
        for (EventPE event : list)
        {
            DeleteDataSetEventParser parser = new DeleteDataSetEventParser(event);
            result.addAll(parser.getDeletedDatasets());
        }
        return result;
    }

    @Override public List<EventPE> listEvents(EventType eventType, EntityType entityTypeOrNull, Date lastSeenTimestampOrNull, Integer limitOrNull)
    {

        StringBuilder hql = new StringBuilder(
                "from " + EventPE.class.getName() + " e where e.eventType = :evt"
        );
        if (entityTypeOrNull != null) {
            hql.append(" and e.entityType = :ent");
        }
        if (lastSeenTimestampOrNull != null) {
            hql.append(" and e.registrationDate > :lastSeen");
        }
        hql.append(" order by e.registrationDate asc, e.id asc");

        int limit = (limitOrNull != null ? limitOrNull : 1);

        var q = currentSession().createQuery(hql.toString(), EventPE.class)
                .setParameter("evt", eventType);
        if (entityTypeOrNull != null) {
            q.setParameter("ent", entityTypeOrNull);
        }
        if (lastSeenTimestampOrNull != null) {
            q.setParameter("lastSeen", lastSeenTimestampOrNull);
        }

        List<EventPE> list = q.setMaxResults(limit).list();

        if (list.size() == limit) {
            Date lastRegistrationDate = list.get(list.size() - 1).getRegistrationDateInternal();

            StringBuilder hqlUpToBoundary = new StringBuilder(
                    "from EventPE e where e.eventType = :evt"
            );
            if (entityTypeOrNull != null) {
                hqlUpToBoundary.append(" and e.entityType = :ent");
            }
            if (lastSeenTimestampOrNull != null) {
                hqlUpToBoundary.append(" and e.registrationDate > :lastSeen");
            }
            hqlUpToBoundary.append(" and e.registrationDate <= :boundaryDate");
            hqlUpToBoundary.append(" order by e.registrationDate asc, e.id asc");

            var q2 = currentSession().createQuery(hqlUpToBoundary.toString(), EventPE.class)
                    .setParameter("evt", eventType)
                    .setParameter("boundaryDate", lastRegistrationDate);

            if (entityTypeOrNull != null) {
                q2.setParameter("ent", entityTypeOrNull);
            }
            if (lastSeenTimestampOrNull != null) {
                q2.setParameter("lastSeen", lastSeenTimestampOrNull);
            }

            // Skip the first 'limit' already returned rows; fetch the remainder up to the boundary
            List<EventPE> remainder = q2.setFirstResult(limit).list();
            if (!remainder.isEmpty()) {
                List<EventPE> full = new ArrayList<>(list.size() + remainder.size());
                full.addAll(list);
                full.addAll(remainder);
                list = full;
            }
        }

        if (operationLog.isDebugEnabled())
        {
            operationLog.debug(String.format(
                    "%s(%s, %s): %d events(s) have been found.", MethodUtils.getCurrentMethod().getName(), eventType, entityTypeOrNull, list.size()));
        }

        return list;
    }
}
