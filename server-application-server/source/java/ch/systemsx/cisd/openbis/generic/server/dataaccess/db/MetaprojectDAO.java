/*
 * Copyright ETH 2012 - 2023 Zürich, Scientific IT Services
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import ch.ethz.sis.shared.log.classic.impl.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.systemsx.cisd.common.reflection.MethodUtils;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.IMetaprojectDAO;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.db.deletion.EntityHistoryCreator;
import ch.systemsx.cisd.openbis.generic.shared.basic.MetaprojectName;
import ch.systemsx.cisd.openbis.generic.shared.dto.IEntityInformationHolderDTO;
import ch.systemsx.cisd.openbis.generic.shared.dto.IEntityInformationWithPropertiesHolder;
import ch.systemsx.cisd.openbis.generic.shared.dto.MetaprojectAssignmentPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.MetaprojectPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.PersonPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.properties.EntityKind;
import org.springframework.orm.hibernate5.SessionFactoryUtils;

import static ch.systemsx.cisd.openbis.generic.server.dataaccess.db.DAOUtils.BATCH_SIZE;

/**
 * @author Pawel Glyzewski
 */
public class MetaprojectDAO extends AbstractGenericEntityDAO<MetaprojectPE> implements
        IMetaprojectDAO
{
    private static final Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION,
            MetaprojectDAO.class);

    private static final Class<MetaprojectPE> ENTITY_CLASS = MetaprojectPE.class;

    public MetaprojectDAO(SessionFactory sessionFactory, EntityHistoryCreator historyCreator)
    {
        super(sessionFactory, ENTITY_CLASS, historyCreator);
    }

    @Override
    public MetaprojectPE tryFindByOwnerAndName(String ownerId, String metaprojectName)
    {
        // Fetch up to 2 so tryFindEntity can warn on duplicates (if your helper does that)
        List<MetaprojectPE> list = currentSession().createQuery(
                                "select m from MetaprojectPE m join m.owner o " +
                                        "where lower(m.name) = :name and o.userId = :ownerId",
                                MetaprojectPE.class)
                        .setParameter("name", metaprojectName.toLowerCase(Locale.ROOT))
                        .setParameter("ownerId", ownerId)
                        .setMaxResults(2)
                        .list();

        final MetaprojectPE entity = tryFindEntity(list, "metaproject");

        if (operationLog.isDebugEnabled())
        {
            String methodName = MethodUtils.getCurrentMethod().getName();
            operationLog.debug(String.format("%s(%s, %s): '%s'.", methodName, ownerId,
                    metaprojectName, entity));
        }
        return entity;
    }

    @Override
    public List<MetaprojectPE> listMetaprojects(PersonPE owner)
    {

        List<MetaprojectPE> list =
                currentSession().createQuery(
                                "select m from MetaprojectPE m " +
                                        "where m.owner = :owner " +
                                        "order by m.name asc",
                                MetaprojectPE.class)
                        .setParameter("owner", owner)
                        .list();

        if (operationLog.isDebugEnabled())
        {
            operationLog.debug(String.format("%s(%s): %d metaproject(s) have been found.",
                    MethodUtils.getCurrentMethod().getName(), owner, list.size()));
        }
        return list;
    }

    @Override
    public void createOrUpdateMetaproject(MetaprojectPE metaproject, PersonPE owner)
    {
        assert metaproject != null : "Missing metaproject.";

        validatePE(metaproject);
        MetaprojectName.validate(metaproject.getName());

        if (metaproject.getOwner() == null)
        {
            metaproject.setOwner(owner);
        }
        metaproject.setPrivate(true);
        doExecute(session -> {
             session.saveOrUpdate(metaproject);
             session.flush();
             return null;
        });
        if (operationLog.isInfoEnabled())
        {
            operationLog.info(String.format("SAVE: metaproject '%s'.", metaproject));
        }
    }

    @Override
    public Collection<MetaprojectPE> listMetaprojectsForEntity(PersonPE owner,
            IEntityInformationHolderDTO entity)
    {

        return doExecute(session -> {
            final String entityKind = entity.getEntityKind().getLabel();
            final String hql =
                    "select distinct m " +
                            "from MetaprojectAssignmentPE a " +
                            "join a.metaproject m " +
                            "where m.owner = :owner and a." + entityKind + " = :entity";

            List<MetaprojectPE> assignments =
                    session.createQuery(hql, MetaprojectPE.class)
                            .setParameter("owner", owner)
                            .setParameter("entity", entity)
                            .list();

            Collection<MetaprojectPE> metaprojects = new HashSet<>(assignments);
            if (operationLog.isDebugEnabled())
            {
                operationLog.debug(String.format("%s(%s, %s): %d metaproject(s) have been found.",
                        MethodUtils.getCurrentMethod().getName(), owner, entity,
                        metaprojects.size()));
            }

            return metaprojects;
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<Long> listMetaprojectEntityIds(Long metaprojectId, EntityKind entityKind)
    {

        final String hql =
                "select e.id " +
                        "from MetaprojectAssignmentPE a " +
                        "join a." + entityKind.getLabel() + " e " +
                        "where a.metaproject.id = :mpId";

        List<Long> idsAsLongs = doExecute(session -> session.createQuery(hql, Long.class)
                        .setParameter("mpId", metaprojectId)
                        .list());

        if (operationLog.isDebugEnabled())
        {
            operationLog.debug(String.format(
                    "%s(%s, %s): %d metaproject entity ids have been found.", MethodUtils
                            .getCurrentMethod().getName(),
                    metaprojectId, entityKind, idsAsLongs
                            .size()));
        }

        return idsAsLongs;
    }

    @Override
    public Collection<MetaprojectAssignmentPE> listMetaprojectAssignmentsForEntities(
            PersonPE owner, Collection<? extends IEntityInformationWithPropertiesHolder> entities,
            EntityKind entityKind)
    {
        if (entities.isEmpty())
        {
            return Collections.emptySet();
        }

        final List<MetaprojectAssignmentPE> assignments = new ArrayList<>();

        InQueryScroller<? extends IEntityInformationWithPropertiesHolder> entitiesScroller = new InQueryScroller<>(entities, 1);
        List<? extends IEntityInformationWithPropertiesHolder> partialEntities = null;

        while ((partialEntities = entitiesScroller.next()) != null)
        {

            final List<? extends IEntityInformationWithPropertiesHolder> finalPartialEntities = partialEntities;
            final String hql =
                    "select a " +
                            "from MetaprojectAssignmentPE a " +
                            "join a.metaproject m " +
                            "where m.owner = :owner " +
                            "and a." + entityKind.getLabel() + " in (:entities)";

            List<MetaprojectAssignmentPE> partialAssignments = doExecute( session ->
                    session.createQuery(hql, MetaprojectAssignmentPE.class)
                            .setParameter("owner", owner)
                            .setParameter("entities", finalPartialEntities)
                            .list());

            assignments.addAll(partialAssignments);
        }

        if (operationLog.isDebugEnabled())
        {
            operationLog.debug(String.format("%s(%s, %s): %d metaproject(s) have been found.",
                    MethodUtils.getCurrentMethod().getName(), owner, entities, assignments.size()));
        }

        return assignments;
    }

    @Override
    public Collection<MetaprojectAssignmentPE> listMetaprojectAssignments(Long metaprojectId,
            EntityKind entityKind)
    {

        final String hql =
                "select a " +
                        "from MetaprojectAssignmentPE a " +
                        "join a.metaproject m " +
                        "where m.id = :mpId " +
                        "and a." + entityKind.getLabel() + " is not null";

        List<MetaprojectAssignmentPE> assignments = doExecute(session -> session.createQuery(hql, MetaprojectAssignmentPE.class)
                        .setParameter("mpId", metaprojectId)
                        .list());

       if (operationLog.isDebugEnabled())
        {
            operationLog.debug(String.format("%s(%s, %s): %d metaproject(s) have been found.",
                    MethodUtils.getCurrentMethod().getName(), metaprojectId, entityKind,
                    assignments.size()));
        }

        return assignments;
    }

    @Override
    public int getMetaprojectAssignmentsCount(Long metaprojectId, EntityKind entityKind)
    {

        final String hql =
                "select count(a) " +
                        "from MetaprojectAssignmentPE a " +
                        "join a.metaproject m " +
                        "where m.id = :mpId " +
                        "and a." + entityKind.getLabel() + " is not null";

        Long count = currentSession().createQuery(hql, Long.class)
                        .setParameter("mpId", metaprojectId)
                        .uniqueResult();

        if (operationLog.isDebugEnabled())
        {
            operationLog.debug(String.format(
                    "%s(%s, %s): %d metaproject assignments have been found.", MethodUtils
                            .getCurrentMethod().getName(),
                    metaprojectId, entityKind, count));
        }

        return count.intValue();
    }

    @Override
    public List<MetaprojectPE> listByIDs(Collection<Long> ids)
    {
        return listByIDsOfName("id", ids);
    }

    private List<MetaprojectPE> listByIDsOfName(String idName, Collection<?> ids)
    {
        if (ids == null || ids.isEmpty())
        {
            return new ArrayList<MetaprojectPE>();
        }
        List<?> allIds = new ArrayList<>(ids);
        List<MetaprojectPE> list = new ArrayList<>(allIds.size());

        for (int i = 0; i < allIds.size(); i += BATCH_SIZE)
        {
            List<?> slice = allIds.subList(i, Math.min(allIds.size(), i + BATCH_SIZE));
            if (slice.isEmpty())
                continue;

            List<MetaprojectPE> batch = doExecute(session -> session.createQuery(
                                            "from " + MetaprojectPE.class.getName() + " e where e." + idName + " in (:ids)",
                                            MetaprojectPE.class
                                    )
                                    .setParameter("ids", slice)
                                    .list());

            list.addAll(batch);
        }
        if (operationLog.isDebugEnabled())
        {
            operationLog.debug(String.format("%d metaproject(s) have been found.", list.size()));
        }
        return list;
    }

}
