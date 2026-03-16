/*
 * Copyright ETH 2011 - 2023 Zürich, Scientific IT Services
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import ch.ethz.sis.shared.log.classic.impl.Logger;

import jakarta.persistence.Query;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;

//import org.hibernate.query.MutationQuery;
//import org.hibernate.query.NativeQuery;
//import org.hibernate.query.SelectionQuery;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.support.JdbcAccessor;
import org.springframework.orm.hibernate5.HibernateCallback;
import org.springframework.orm.hibernate5.HibernateTemplate;

import ch.systemsx.cisd.common.collection.SimpleComparator;
import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.DynamicPropertyEvaluationOperation;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.IDeletionDAO;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.IDynamicPropertyEvaluationScheduler;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.PersistencyResources;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.db.deletion.EntityHistoryCreator;
import ch.systemsx.cisd.openbis.generic.shared.basic.TechId;
import ch.systemsx.cisd.openbis.generic.shared.dto.DataSetRelationshipPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.DeletionPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.IDeletablePE;
import ch.systemsx.cisd.openbis.generic.shared.dto.MetaprojectAssignmentPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.PersonPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.SampleRelationshipPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.TableNames;
import ch.systemsx.cisd.openbis.generic.shared.dto.properties.EntityKind;
import ch.systemsx.cisd.openbis.generic.shared.util.HibernateUtils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.lemnik.eodsql.QueryTool;

import static ch.systemsx.cisd.openbis.generic.server.dataaccess.db.DAOUtils.BATCH_SIZE;

/**
 * <i>Data Access Object</i> implementation for {@link IDeletionDAO}.
 * 
 * @author Christian Ribeaud
 */
final class DeletionDAO extends AbstractGenericEntityDAO<DeletionPE> implements IDeletionDAO
{
    private static final SimpleComparator<DeletionPE, Long> DELETION_COMPARATOR = new SimpleComparator<DeletionPE, Long>()
        {
            @Override
            public Long evaluate(DeletionPE deletion)
            {
                return deletion.getId();
            }
        };

    private static final String ID = "id";

    private static final String DELETION_ID = "deletion.id";

    private static final String CONTAINER_ID = "containerId";

    private static final String ORIGINAL_DELETION = "originalDeletion";

    /**
     * This logger does not output any SQL statement. If you want to do so, you had better set an appropriate debugging level for class
     * {@link JdbcAccessor}.
     * </p>
     */
    private static final Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION,
            DeletionDAO.class);

    private final PersistencyResources persistencyResources;

    private final IDeletionQuery deletionQuery;

    DeletionDAO(final SessionFactory sessionFactory,
            final PersistencyResources persistencyResources, EntityHistoryCreator historyCreator)
    {
        super(sessionFactory, DeletionPE.class, historyCreator);

        this.persistencyResources = persistencyResources;
        deletionQuery = QueryTool.getManagedQuery(IDeletionQuery.class);
    }

    //
    // IDeletionDAO
    //

    @Override
    public final void create(final DeletionPE deletion) throws DataAccessException
    {
        assert deletion != null : "Unspecified deletion";
        validatePE(deletion);

        doExecute(session -> {
            session.save(deletion);
            session.flush();
            return null;
            });
        if (operationLog.isInfoEnabled())
        {
            operationLog.info(String.format("ADD: deletion '%s'.", deletion));
        }
    }

    @Override
    public void revert(DeletionPE deletion, PersonPE modifier) throws DataAccessException
    {
        operationLog.info(String.format("REVERT: deletion %s.", deletion));
        for (EntityKind entityKind : EntityKind.values())
        {
            // NOTE: material deletion are always permanent and therefore can't be reverted
            revertDeletionOfEntities(deletion, entityKind, modifier);
        }
        super.flush();
        super.clear();
        super.delete(deletion);
    }

    @SuppressWarnings("unused")
    private void revertDeletionOfEntitiesOld(final DeletionPE deletion, final EntityKind entityKind)
    {
        assert deletion != null : "Unspecified deletion";
        assert entityKind != null : "Unspecified entity kind";

        List<TechId> ids =
                findTrashedEntityIds(Collections.singletonList(TechId.create(deletion)), entityKind);

        int updatedRows = doExecute(session -> {
                    String query =
                            String.format("UPDATE VERSIONED %s SET deletion = NULL WHERE deletion = ?",
                                    entityKind.getDeletedEntityClass().getSimpleName());
                    //        int updatedRows = hibernateTemplate.bulkUpdate(query, deletion);
                    Query q = session.createNativeQuery(query);
                    q.setParameter("deletion", deletion);

                    int upRows = q.executeUpdate();
                    session.flush();
                    session.clear();
                    return upRows;
                });
        scheduleDynamicPropertiesEvaluationByIds(TechId.asLongs(ids), entityKind);

        operationLog.info(String.format("%s %s(s) reverted", updatedRows, entityKind.name()));
    }

    private void revertDeletionOfEntities(final DeletionPE deletion, final EntityKind entityKind,
            final PersonPE modifier)
    {
        assert deletion != null : "Unspecified deletion";
        assert entityKind != null : "Unspecified entity kind";
        assert modifier != null : "Unspecified modifier";

        List<TechId> ids =
                findTrashedEntityIds(Collections.singletonList(TechId.create(deletion)), entityKind);

        int updatedRows = (Integer) executeStatelessAction(new StatelessHibernateCallback()
            {

                @Override
                public Object doInStatelessSession(StatelessSession session)
                {

                    String query =
                            String.format(
                                    "UPDATE %s SET modification_timestamp = now(), "
                                            + "del_id = NULL, orig_del = NULL, pers_id_modifier = :modifierId "
                                            + "WHERE del_id = :deletionId",
                                    entityKind.getAllTableName());
                    final Query sqlQuery = session.createNativeQuery(query);
                    sqlQuery.setParameter("deletionId", HibernateUtils.getId(deletion));
                    sqlQuery.setParameter("modifierId", HibernateUtils.getId(modifier));
                    return sqlQuery.executeUpdate();
                }

            });

        switch (entityKind)
        {
            case SAMPLE:
                revertDeletionOfRelationships(deletion, TableNames.SAMPLE_RELATIONSHIPS_ALL_TABLE);
                break;
            case DATA_SET:
                revertDeletionOfRelationships(deletion, TableNames.DATA_SET_RELATIONSHIPS_ALL_TABLE);
                break;
            case EXPERIMENT:
                break;
        }

        revertDeletionOfRelationships(deletion, TableNames.METAPROJECT_ASSIGNMENTS_ALL_TABLE);

        scheduleDynamicPropertiesEvaluationByIds(TechId.asLongs(ids), entityKind);

        operationLog.info(String.format("%s %s(s) reverted", updatedRows, entityKind.name()));
    }

    private void revertDeletionOfRelationships(final DeletionPE deletion, final String tableName)
    {
        assert deletion != null : "Unspecified deletion";

        int updatedRows = (Integer) executeStatelessAction(new StatelessHibernateCallback()
            {

                @Override
                public Object doInStatelessSession(StatelessSession session)
                {
                    String query =
                            String.format("UPDATE %s SET del_id = NULL WHERE del_id = :deletionId",
                                    tableName);
                    final Query sqlQuery = session.createNativeQuery(query);
                    sqlQuery.setParameter("deletionId", HibernateUtils.getId(deletion));
                    return sqlQuery.executeUpdate();
                }
            });

        operationLog.info(String.format("%s %s(s) reverted", updatedRows, tableName));
    }

    @Override
    public List<TechId> findTrashedSampleIds(final List<TechId> deletionIds)
    {
        return findTrashedEntityIds(deletionIds, EntityKind.SAMPLE);
    }

    @Override
    public List<TechId> findTrashedNonComponentSampleIds(final List<TechId> deletionIds)
    {
        return findTrashedEntityIds(deletionIds, EntityKind.SAMPLE,
                (root, cb) -> cb.isNull(path(root, CONTAINER_ID)));
    }

    @Override
    public List<TechId> findTrashedComponentSampleIds(final List<TechId> deletionIds)
    {
        return findTrashedEntityIds(deletionIds, EntityKind.SAMPLE,
                (root, cb) -> cb.isNotNull(path(root, CONTAINER_ID)));
    }

    @Override
    public List<TechId> findTrashedExperimentIds(final List<TechId> deletionIds)
    {
        return findTrashedEntityIds(deletionIds, EntityKind.EXPERIMENT);
    }

    @Override
    public List<String> findTrashedDataSetCodes(final List<TechId> deletionIds)
    {
        if (deletionIds.isEmpty())
        {
            return Collections.emptyList();
        }
        final List<Long> longIds = TechId.asLongs(deletionIds);
        List<Long> ids = new java.util.ArrayList<>(longIds);
        List<String> results = new java.util.ArrayList<>(ids.size());
        final Class<?> deletedEntityClass = EntityKind.DATA_SET.getDeletedEntityClass();

        for (int i = 0; i < ids.size(); i += BATCH_SIZE) {
            List<Long> slice = ids.subList(i, Math.min(ids.size(), i + BATCH_SIZE));
            if (slice.isEmpty()) continue;

            List<String> batch = doExecute(session -> session.createQuery(
                                            "select d.code from " + deletedEntityClass.getName() + " d " +
                                                    "where d." + DELETION_ID + " in (:ids)",
                                            String.class
                                    )
                                    .setParameter("ids", slice)
                                    .list());

            results.addAll(batch);
        }

        operationLog.info(String.format("found %s trashed %s(s)", results.size(),
                EntityKind.DATA_SET.name()));
        return results;
    }

    private List<TechId> findTrashedEntityIds(final List<TechId> deletionIds,
            final EntityKind entityKind, final Pred... extra)
    {
        if (deletionIds.isEmpty())
        {
            return Collections.emptyList();
        }
        final List<Long> longIds = TechId.asLongs(deletionIds);
        final Class<?> deletedEntityClass = entityKind.getDeletedEntityClass();


        List<Long> results = doExecute(session -> {
            var cb = session.getCriteriaBuilder();
            List<Long> out = new ArrayList<>(longIds.size());

            for (int i = 0; i < longIds.size(); i += BATCH_SIZE) {
                List<Long> slice = longIds.subList(i, Math.min(longIds.size(), i + BATCH_SIZE));

                var cq = cb.createQuery(Long.class);
                var root = cq.from(deletedEntityClass);

                var predicates = new ArrayList<Predicate>();
                predicates.add(path(root, DELETION_ID).in(slice));

                if (extra != null) {
                    for (Pred p : extra) {
                        predicates.add(p.build(root, cb));
                    }
                }

                cq.select(root.get("id")).where(predicates.toArray(new Predicate[0]));

                out.addAll(session.createQuery(cq).getResultList());
            }
            return out;
        });


        operationLog
                .info(String.format("found %s trashed %s(s)", results.size(), entityKind.name()));
        return transformNumbers2TechIdList(results);
    }

    private static Path<?> path(Path<?> root, String dotPath) {
        Path<?> p = root;
        for (String part : dotPath.split("\\.")) {
            p = p.get(part);
        }
        return p;
    }

    @Override
    public int trash(EntityKind entityKind, List<TechId> entityIds, DeletionPE deletion)
            throws DataAccessException
    {
        return trash(entityKind, entityIds, deletion, false);
    }

    @Override
    public int trash(final EntityKind entityKind, final List<TechId> entityIds,
            final DeletionPE deletion, final boolean isOriginalDeletion) throws DataAccessException
    {
        if (entityIds.isEmpty())
        {
            return 0;
        }

//                    MutationQuery query = session
        int updatedRows = trashEntities(entityKind, entityIds, deletion, isOriginalDeletion);

        switch (entityKind)
        {
            case SAMPLE:
                trashSampleRelationships(entityIds, deletion);
                break;
            case DATA_SET:
                trashDataSetRelationships(entityIds, deletion);
                break;
            case EXPERIMENT:
                break;
        }

        trashMetaprojectAssignments(entityIds, entityKind, deletion);

        if (operationLog.isInfoEnabled())
        {
            operationLog.info(String.format("trashing %d %ss", updatedRows, entityKind.getLabel()));
        }
        currentSession().flush();

        List<Long> ids = TechId.asLongs(entityIds);
        scheduleRemoveFromFullTextIndex(ids, entityKind);

        return updatedRows;
    }

    private int trashEntities(final EntityKind entityKind, final List<TechId> entityIds,
            final DeletionPE deletion, final boolean isOriginalDeletion)
    {
        if (entityKind.getAllTableName() != null)
        {
            return trashEntitiesWithNativeUpdate(entityKind, entityIds, deletion, isOriginalDeletion);
        } else
        {
            return trashEntitiesWithHql(entityKind, entityIds, deletion, isOriginalDeletion);
        }
    }

    private int trashEntitiesWithHql(final EntityKind entityKind, final List<TechId> entityIds,
            final DeletionPE deletion, final boolean isOriginalDeletion)
    {
        String hql = "UPDATE " + entityKind.getEntityClass().getSimpleName() + " c " +
                "SET c.deletion = :delRef, " +
                "    c.originalDeletion = " + (isOriginalDeletion ? ":delId" : "NULL") + " " +
                "WHERE c.deletion IS NULL AND c.id IN (:ids)";

        Query mutationQuery = currentSession().createQuery(hql)
                .setParameter("delRef", deletion)
                .setParameter("ids", TechId.asLongs(entityIds));

        if (isOriginalDeletion)
        {
            mutationQuery.setParameter("delId", deletion.getId());
        }

        return mutationQuery.executeUpdate();
    }

    private int trashEntitiesWithNativeUpdate(final EntityKind entityKind, final List<TechId> entityIds,
            final DeletionPE deletion, final boolean isOriginalDeletion)
    {
        final List<Long> ids = TechId.asLongs(entityIds);
        final Long deletionId = HibernateUtils.getId(deletion);
        final Long modifierId = HibernateUtils.getId(deletion.getRegistrator());

        Session currentSession = currentSession();
        currentSession.flush();

        Integer updatedRows = (Integer) executeStatelessAction(new StatelessHibernateCallback()
            {
                @Override
                public Object doInStatelessSession(StatelessSession session) throws HibernateException
                {
                    StringBuilder sql = new StringBuilder();
                    sql.append("UPDATE ").append(entityKind.getAllTableName())
                            .append(" SET del_id = :delId,")
                            .append(" modification_timestamp = now(),")
                            .append(" pers_id_modifier = :modifierId,");

                    if (isOriginalDeletion)
                    {
                        sql.append(" orig_del = :origDel");
                    } else
                    {
                        sql.append(" orig_del = NULL");
                    }

                    sql.append(" WHERE del_id IS NULL AND id IN (:ids)");

                    var query = session.createNativeQuery(sql.toString());
                    query.setParameter("delId", deletionId);
                    query.setParameter("modifierId", modifierId);
                    if (isOriginalDeletion)
                    {
                        query.setParameter("origDel", deletionId);
                    }
                    query.setParameterList("ids", ids);

                    return query.executeUpdate();
                }
            });

        // ensure the current session does not hold stale entity states after the stateless update
        currentSession().clear();

        return updatedRows;
    }

    private int trashSampleRelationships(final List<TechId> samplesIds, final DeletionPE deletion)
            throws DataAccessException
    {
        if (samplesIds.isEmpty())
        {
            return 0;
        }
                    // NOTE: 'VERSIONED' makes modification time modified too
        int updatedRows = doExecute(session -> session
                    .createQuery(
                            "UPDATE "
                                    + SampleRelationshipPE.class.getSimpleName()
                                    + " SET deletion = :deletion, author = :author"
                                    + " WHERE deletion IS NULL"
                                    + " AND (parentSample.id IN (:ids) OR childSample.id in (:ids))")
                    .setParameter("deletion", deletion)
                    .setParameter("author", deletion.getRegistrator())
                    .setParameterList("ids", TechId.asLongs(samplesIds)).executeUpdate());
        if (operationLog.isInfoEnabled())
        {
            operationLog.info(String
                    .format("trashing %d %ss", updatedRows, "sample relationships."));
        }
        currentSession().flush();

        return updatedRows;
    }

    private int trashMetaprojectAssignments(final List<TechId> entityIds,
            final EntityKind entityKind, final DeletionPE deletion) throws DataAccessException
    {
        if (entityIds.isEmpty())
        {
            return 0;
        }
        return  doExecute(session -> {
            int uRows = session.createQuery(
                            "UPDATE " + MetaprojectAssignmentPE.class.getSimpleName()
                                    + " SET deletion = :deletion"
                                    + " WHERE deletion IS NULL" + " AND "
                                    + entityKind.getLabel() + ".id IN (:ids)")
                    .setParameter("deletion", deletion)
                    .setParameterList("ids", TechId.asLongs(entityIds)).executeUpdate();
            if (operationLog.isInfoEnabled())
            {
                operationLog.info(
                        String.format("trashing %d %ss", uRows, entityKind.getLabel()
                                + " metaproject assignments."));
            }
            session.flush();
            return uRows;
        });
    }

    private int trashDataSetRelationships(final List<TechId> dataSetIds, final DeletionPE deletion)
            throws DataAccessException
    {
        if (dataSetIds.isEmpty())
        {
            return 0;
        }
        return doExecute(session -> {
            int updatedRows = session.createQuery(
                            "UPDATE "
                                    + DataSetRelationshipPE.class.getSimpleName()
                                    + " SET deletion = :deletion, author = :author"
                                    + " WHERE deletion IS NULL"
                                    + " AND (parentDataSet.id IN (:ids) OR childDataSet.id in (:ids))")
                    .setParameter("deletion", deletion)
                    .setParameter("author", deletion.getRegistrator())
                    .setParameterList("ids", TechId.asLongs(dataSetIds)).executeUpdate();

            if (operationLog.isInfoEnabled())
            {
                operationLog.info(String.format("trashing %d %ss", updatedRows,
                        "data set relationships."));
            }
            session.flush();

            return updatedRows;
        });
    }

    @Override
    public List<DeletionPE> findAllById(List<Long> ids)
    {
        if (ids.isEmpty())
        {
            return Collections.emptyList();
        }

        List<Long> all = new ArrayList<>(ids);
        List<DeletionPE> result = new ArrayList<>(all.size());

        for (int i = 0; i < all.size(); i += BATCH_SIZE) {
            List<Long> slice = all.subList(i, Math.min(all.size(), i + BATCH_SIZE));
            if (slice.isEmpty()) continue;

            List<DeletionPE> batch =  doExecute(session -> session.createQuery(
                                            "from " + DeletionPE.class.getName() + " d where d.id in (:ids)",
                                            DeletionPE.class
                                    )
                                    .setParameter("ids", slice)
                                    .list());

            result.addAll(batch);
        }

        if (operationLog.isDebugEnabled())
        {
            operationLog.debug(String.format("%s deletions has been found", result.size()));
        }

        return result;
    }

    protected IDynamicPropertyEvaluationScheduler getIndexUpdateScheduler()
    {
        return persistencyResources.getDynamicPropertyEvaluationScheduler();
    }

    protected IDynamicPropertyEvaluationScheduler getDynamicPropertyEvaluatorScheduler()
    {
        return persistencyResources.getDynamicPropertyEvaluationScheduler();
    }

    protected void scheduleRemoveFromFullTextIndex(List<Long> ids, EntityKind entityKind)
    {
        getIndexUpdateScheduler().scheduleUpdate(DynamicPropertyEvaluationOperation.delete(entityKind.getEntityClass(), ids));
    }

    protected void scheduleDynamicPropertiesEvaluationByIds(List<Long> ids, EntityKind entityKind)
    {
        scheduleDynamicPropertiesEvaluationForIds(getDynamicPropertyEvaluatorScheduler(),
                entityKind.getEntityClass(), ids);
    }

    @Override
    public List<TechId> findTrashedDataSetIds(List<TechId> deletionIds)
    {
        return findTrashedEntityIds(deletionIds, EntityKind.DATA_SET);
    }

    @Override
    public List<? extends IDeletablePE> listDeletedEntities(EntityKind entityKind,
            List<TechId> entityIds)
    {
        if (entityIds.isEmpty())
        {
            return Collections.emptyList();
        }

        List<Long> ids = TechId.asLongs(entityIds);
        final Class<?> deletedClass = entityKind.getDeletedEntityClass();

        List<? extends IDeletablePE>  out = new ArrayList<>(ids.size());

        for (int i = 0; i < ids.size(); i += BATCH_SIZE) {
            List<Long> slice = ids.subList(i, Math.min(ids.size(), i + BATCH_SIZE));
            if (slice.isEmpty()) continue;

            List batch = doExecute(session -> session.createQuery(
                                            "from " + deletedClass.getName() + " d where d.id in (:ids)", deletedClass
                                    )
                                    .setParameter("ids", slice)
                                    .list());

            out.addAll(batch);
        }
        return out;
    }

    @Override
    public List<TechId> listDeletedEntitiesForType(EntityKind entityKind, TechId entityTypeId)
    {
        String typeId = null;
        switch (entityKind)
        {
            case EXPERIMENT:
                typeId = "experimentType.id";
                break;

            case SAMPLE:
                typeId = "sampleType.id";
                break;

            case DATA_SET:
                typeId = "dataSetType.id";
                break;

            default:
                // entities of these types cannot be in the trash can
                return Collections.emptyList();
        }

        final Class<?> deletedEntityClass = entityKind.getDeletedEntityClass();
        final String hql = "select d.id from " + deletedEntityClass.getName() + " d "
                + "where d." + typeId + " = :typeId";

        List<Long> result = doExecute(session -> session.createQuery(hql, Long.class)
                        .setParameter("typeId", entityTypeId.getId())
                        .getResultList());

        return TechId.createList(result);
    }

    @Override
    public List<TechId> findOriginalTrashedDataSetIds(List<TechId> deletionIds)
    {
        return findTrashedEntityIds(deletionIds, EntityKind.DATA_SET,
                (root, cb) -> cb.isNotNull(path(root, ORIGINAL_DELETION)));
    }

    @Override
    public List<TechId> findOriginalTrashedExperimentIds(List<TechId> deletionIds)
    {
        return findTrashedEntityIds(deletionIds, EntityKind.EXPERIMENT,
                (root, cb) -> cb.isNotNull(path(root, ORIGINAL_DELETION)));
    }

    @Override
    public List<TechId> findOriginalTrashedSampleIds(List<TechId> deletionIds)
    {
        return findTrashedEntityIds(deletionIds, EntityKind.SAMPLE,
                (root, cb) -> cb.isNotNull(path(root, ORIGINAL_DELETION)));
    }

    @Override
    public List<TechId> listAllDependentDeletions(List<TechId> deletionIds)
    {
        LongSet ids = new LongOpenHashSet();
        for (TechId techId : deletionIds)
        {
            if (techId != null)
            {
                ids.add(techId.getId());
            }
        }
        LongSet newIds = getCompleteSet(ids);
        newIds.removeAll(ids);
        List<Long> dependentIds = new ArrayList<>(newIds);
        Collections.sort(dependentIds);
        return TechId.createList(dependentIds);
    }

    private LongSet getCompleteSet(LongSet ids)
    {
        for (;;)
        {
            LongSet newIds = new LongOpenHashSet();
            newIds.addAll(deletionQuery.getSampleDeletionsOfExperimentDeletions(ids));
            newIds.addAll(deletionQuery.getDataSetDeletionsOfExperimentDeletions(ids));
            newIds.addAll(deletionQuery.getSampleDeletionsOfContainerDeletions(ids));
            newIds.addAll(deletionQuery.getDataSetDeletionsOfSampleDeletions(ids));
            if (newIds.size() == ids.size())
            {
                return newIds;
            }
            ids = newIds;
        }
    }
}
