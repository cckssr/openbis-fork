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
package ch.systemsx.cisd.openbis.generic.server.dataaccess.db;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.StatelessSession;
import org.hibernate.query.Query;
import org.springframework.dao.DataAccessException;

import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.ethz.sis.shared.log.classic.impl.Logger;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.IDynamicPropertyEvaluationScheduler;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.IEntityPropertyTypeDAO;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.PersistencyResources;
import ch.systemsx.cisd.openbis.generic.shared.dto.ColumnNames;
import ch.systemsx.cisd.openbis.generic.shared.dto.EntityPropertyPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.EntityPropertyWithSampleDataTypePE;
import ch.systemsx.cisd.openbis.generic.shared.dto.EntityTypePE;
import ch.systemsx.cisd.openbis.generic.shared.dto.EntityTypePropertyTypePE;
import ch.systemsx.cisd.openbis.generic.shared.dto.ExternalDataPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.IEntityInformationWithPropertiesHolder;
import ch.systemsx.cisd.openbis.generic.shared.dto.PropertyTypePE;
import ch.systemsx.cisd.openbis.generic.shared.dto.SequenceNames;
import ch.systemsx.cisd.openbis.generic.shared.dto.TableNames;
import ch.systemsx.cisd.openbis.generic.shared.dto.VocabularyPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.VocabularyTermWithStats;
import ch.systemsx.cisd.openbis.generic.shared.dto.properties.EntityKind;
import jakarta.persistence.TemporalType;

/**
 * The unique {@link IEntityPropertyTypeDAO} implementation.
 *
 * @author Christian Ribeaud
 * @author Tomasz Pylak
 * @author Izabela Adamczyk
 */
final class EntityPropertyTypeDAO extends AbstractDAO implements IEntityPropertyTypeDAO
{

    private static final Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION,
            EntityPropertyTypeDAO.class);

    private final EntityKind entityKind;

    private final IDynamicPropertyEvaluationScheduler dynamicPropertyEvaluationScheduler;

    public EntityPropertyTypeDAO(final EntityKind entityKind,
            final PersistencyResources persistencyResources)
    {
        super(persistencyResources.getSessionFactory());
        this.entityKind = entityKind;
        this.dynamicPropertyEvaluationScheduler =
                persistencyResources.getDynamicPropertyEvaluationScheduler();
    }

    private final <T extends EntityTypePropertyTypePE> Class<T> getEntityTypePropertyTypeAssignmentClass()
    {
        return entityKind.getEntityTypePropertyTypeAssignmentClass();
    }

    //
    // IEntityPropertyTypeDAO
    //

    @Override
    public List<EntityTypePropertyTypePE> listEntityPropertyTypes() throws DataAccessException
    {
        return cast(loadAll(getEntityTypePropertyTypeAssignmentClass()));
    }

    @Override
    public final List<EntityTypePropertyTypePE> listEntityPropertyTypes(
            final EntityTypePE entityType) throws DataAccessException
    {
        assert entityType != null : "Unspecified EntityType";

        final List<EntityTypePropertyTypePE> assignments =
                cast(find(EntityTypePropertyTypePE.class,
                        String.format("from %s etpt where etpt.entityTypeInternal = ?1",
                                getEntityTypePropertyTypeAssignmentClass().getSimpleName()),
                        toArray(entityType)));
        if (operationLog.isDebugEnabled())
        {
            operationLog.debug(String.format(
                    "%d assignments have been found for entity type '%s'.", assignments.size(),
                    entityType));
        }
        return assignments;
    }

    @Override
    public List<String> listPropertyTypeCodes() throws DataAccessException
    {
        final List<EntityTypePropertyTypePE> assignments =
                cast(loadAll(getEntityTypePropertyTypeAssignmentClass()));
        Set<String> propertyTypeCodes = new HashSet<String>();

        for (EntityTypePropertyTypePE assignment : assignments)
        {
            propertyTypeCodes.add(assignment.getPropertyType().getCode());
        }

        if (operationLog.isDebugEnabled())
        {
            operationLog.debug(String.format(
                    "%d property types have been found for entity kind '%s'.",
                    propertyTypeCodes.size(), entityKind));
        }
        return new ArrayList<String>(propertyTypeCodes);
    }

    @Override
    public EntityTypePropertyTypePE tryFindAssignment(EntityTypePE entityType,
            PropertyTypePE propertyType)
    {
        assert entityType != null : "Unspecified entity type.";
        assert propertyType != null : "Unspecified property type.";

        final Class<? extends EntityTypePropertyTypePE> cls = getEntityTypePropertyTypeAssignmentClass();

        return currentSession()
                .createQuery(
                        "from " + cls.getName() + " e " +
                                "where e.propertyTypeInternal = :pt and e.entityTypeInternal = :et",
                        cls)
                .setParameter("pt", propertyType)
                .setParameter("et", entityType)
                .uniqueResultOptional()
                .orElse(null);
    }

    @Override
    public final void createEntityPropertyTypeAssignment(
            final EntityTypePropertyTypePE entityPropertyTypeAssignement)
            throws DataAccessException
    {
        assert entityPropertyTypeAssignement != null : "Unspecified EntityTypePropertyType";
        validatePE(entityPropertyTypeAssignement);

        doExecute(session ->
        {
            session.save(entityPropertyTypeAssignement);
            session.flush();
            return null;
        });

        if (operationLog.isInfoEnabled())
        {
            operationLog.info("ADD: assignment of property '"
                    + entityPropertyTypeAssignement.getPropertyType().getCode()
                    + "' with entity type '"
                    + entityPropertyTypeAssignement.getEntityType().getCode() + "'.");
        }
    }

    @Override
    public List<Long> listEntityIds(final EntityTypePE entityType) throws DataAccessException
    {
        assert entityType != null : "Unspecified entity type.";

        //        Hibernate 6 changes
        final Class<?> entityClass = entityKind.getEntityClass();
        final String typeField = entityKind.getEntityTypeFieldName(); // e.g. "sampleTypeInternal"

        // Select the identifier using the portable HQL/JPQL id(...) selector
        final String hql = "select e.id from " + entityClass.getName() + " e " +
                "where e." + typeField + " = :type";

        final List<Long> list = doExecute(session -> session
                .createQuery(hql, Long.class)
                .setParameter("type", entityType)
                .getResultList());

        if (operationLog.isInfoEnabled())
        {
            operationLog.info(String.format("LIST: found %s ids of entities of type '%s'.",
                    list.size(), entityType));
        }
        return list;
    }

    @Override
    public void scheduleDynamicPropertiesEvaluation(final EntityTypePropertyTypePE assignment)
            throws DataAccessException
    {
        assert assignment != null : "Unspecified assignment.";
        if (assignment.isDynamic()) // sanity check
        {
            List<Long> entityIds = listEntityIds(assignment);
            scheduleDynamicPropertiesEvaluation(entityIds);
        }
    }

    private List<Long> listEntityIds(final EntityTypePropertyTypePE assignment)
            throws DataAccessException
    {
        assert assignment != null : "Unspecified assignment.";

        String query = null;

        switch (entityKind)
        {
            case SAMPLE:
                query =
                        String.format("SELECT DISTINCT sample.id "
                                + "FROM SamplePE sample, SampleTypePropertyTypePE stpt "
                                + "WHERE sample.sampleType = stpt.entityTypeInternal AND stpt = ?1");
                break;
            case DATA_SET:
                query =
                        String.format("SELECT DISTINCT data.id "
                                + "FROM DataPE data, DataSetTypePropertyTypePE dtpt "
                                + "WHERE data.dataSetType = dtpt.entityTypeInternal AND dtpt = ?1");
                break;
            case MATERIAL:
                query =
                        String.format("SELECT DISTINCT material.id "
                                + "FROM MaterialPE material, MaterialTypePropertyTypePE mtpt "
                                + "WHERE material.materialType = mtpt.entityTypeInternal AND mtpt = ?1");
                break;
            case EXPERIMENT:
                query =
                        String.format("SELECT DISTINCT experiment.id "
                                + "FROM ExperimentPE experiment, ExperimentTypePropertyTypePE etpt "
                                + "WHERE experiment.experimentType = etpt.entityTypeInternal AND etpt = ?1");
                break;
            default:
                throw new IllegalArgumentException(entityKind.toString());
        }

        final List<Long> list = cast(find(Long.class, query, toArray(assignment)));

        if (operationLog.isInfoEnabled())
        {
            operationLog.info(String.format(
                    "LIST: found %s ids of entities of type '%s' assigned to property '%s'.",
                    list.size(), assignment.getEntityType(), assignment.getPropertyType()));
        }
        return list;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void updateEntityModificationTimestamps(final List<Long> entityIds)
    {
        assert entityIds != null : "Null entityId list.";

        if (entityIds.isEmpty())
        {
            return;
        }

        doExecute(session ->
        {
            String entityTableName = null;

            switch (entityKind)
            {
                case SAMPLE:
                    entityTableName = TableNames.SAMPLES_ALL_TABLE;
                    break;
                case DATA_SET:
                    entityTableName = TableNames.DATA_ALL_TABLE;
                    break;
                case MATERIAL:
                    entityTableName = TableNames.MATERIALS_TABLE;
                    break;
                case EXPERIMENT:
                    entityTableName = TableNames.EXPERIMENTS_VIEW;
                    break;
                default:
                    throw new IllegalArgumentException(entityKind.toString());
            }
            InQueryScroller<Long> updateQueryScroller = new InQueryScroller(entityIds, 1);
            List<Long> partialEntityId;
            while ((partialEntityId = updateQueryScroller.next()) != null)
            {
                Query updateQuery = session
                        .createNativeQuery("update " + entityTableName + " set modification_timestamp = :timestamp where id in :entityIds ");
                updateQuery.setParameter("timestamp", getTransactionTimeStamp(), TemporalType.TIMESTAMP);
                updateQuery.setParameterList("entityIds", partialEntityId);
                updateQuery.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public ScrollableResults listPropertyValues(String entityTypeCode, String propertyTypeCode) throws DataAccessException
    {
        final String queryString = String.format("SELECT pv.value FROM %s pa join %s pv "
                        + " ON pa.id = pv.entityTypePropertyType.id "
                        + " WHERE pa.propertyTypeInternal.simpleCode = :propertyTypeCode "
                        + " AND pa.entityTypeInternal.simpleCode = :entityTypeCode ",
                entityKind
                        .getEntityTypePropertyTypeAssignmentClass().getSimpleName(),
                entityKind.getEntityPropertyClass().getSimpleName()
        );

        Query query = currentSession().createQuery(queryString)
                .setReadOnly(true)
                .setCacheable(false)
                .setFetchSize(1000);

        query.setParameter("propertyTypeCode", propertyTypeCode);
        query.setParameter("entityTypeCode", entityTypeCode);

        ScrollableResults results = query.scroll(ScrollMode.FORWARD_ONLY);
        return results;
    }

    @Override
    public List<Long> listIdsOfEntitiesWithoutPropertyValue(
            final EntityTypePropertyTypePE assignment) throws DataAccessException
    {
        assert assignment != null : "Unspecified assignment.";

        String query =
                String.format(
                        "SELECT e.id FROM %s e WHERE e.%s = ?1 AND e not in (SELECT p.entity FROM %s p WHERE p.entityTypePropertyType = ?2)",
                        entityKind.getEntityClass().getSimpleName(), entityKind
                                .getEntityTypeFieldName(),
                        entityKind.getEntityPropertyClass()
                                .getSimpleName());
        final List<Long> list =
                find(Long.class, query,
                        toArray(assignment.getEntityType(), assignment));

        if (operationLog.isInfoEnabled())
        {
            operationLog.info(String.format(
                    "LIST: found %s ids of entities of type '%s' assigned to property '%s'.",
                    list.size(), assignment.getEntityType(), assignment.getPropertyType()));
        }
        return list;
    }

    @Override
    public void createProperties(final EntityPropertyPE property, final List<Long> entityIds)
    {
        assert property != null : "Given property data can not be null.";

        final Long etptId = property.getEntityTypePropertyType().getId();
        final Long registratorId = property.getRegistrator().getId();

        final EntityKindPropertyTableNames propertyTableNames =
                getEntityKindPropertyTableNames(entityKind);
        final String tableName = propertyTableNames.getPropertiesTable();
        final String sequenceName = propertyTableNames.getPropertiesSequence();
        final String entityColumn = propertyTableNames.getEntityColumn();
        final String propertyTypeColumn = propertyTableNames.getPropertyTypeColumn();

        final String valueColumn;
        final Serializable valueObject;
        String valuePlaceHolder = ":value";

        if (property.getVocabularyTerm() != null)
        {
            valueColumn = "cvte_id";
            valueObject = property.getVocabularyTerm().getId();
        } else if (property.getMaterialValue() != null)
        {
            valueColumn = "mate_prop_id";
            valueObject = property.getMaterialValue().getId();
        } else if (property.getJsonValue() != null)
        {
            valueColumn = "json_value";
            valueObject = property.getJsonValue();
            valuePlaceHolder = "CAST(:value AS jsonb)";
        } else if (property instanceof final EntityPropertyWithSampleDataTypePE sampleProperty && sampleProperty.getSampleValue() != null)
        {
            valueColumn = "samp_prop_id";
            valueObject = sampleProperty.getSampleValue().getId();
        } else
        {
            assert property.getValue() != null;
            valueColumn = "value";
            valueObject = property.getValue();
        }

        final String sql =
                String.format(
                        "INSERT INTO %s (id, pers_id_registerer, pers_id_author, %s, %s, %s) "
                                + "VALUES (nextval('%s'), :registratorId, :registratorId, :entityId, :etptId, " + valuePlaceHolder + ")",
                        tableName, entityColumn, propertyTypeColumn, valueColumn, sequenceName);

        // inserts are performed using stateless session for better memory management
        executeStatelessAction(new StatelessHibernateCallback()
        {
            @Override
            public Object doInStatelessSession(StatelessSession session)
            {
                final Query sqlQuery = session.createNativeQuery(sql);
                sqlQuery.setParameter("registratorId", registratorId);
                sqlQuery.setParameter("etptId", etptId);
                // TODO check how to handle null values
                sqlQuery.setParameter("value", valueObject);
                int counter = 0;
                for (Long entityId : entityIds)
                {
                    sqlQuery.setParameter("entityId", entityId);
                    sqlQuery.executeUpdate();
                    if (operationLog.isDebugEnabled())
                    {
                        operationLog.debug(String.format(
                                "Created property '%s' for %s with id %s", property,
                                entityKind.getLabel(), entityId));
                    }
                    if (++counter % 1000 == 0)
                    {
                        operationLog.info(String.format(
                                "%d %s properties have been created...", counter,
                                entityKind.getLabel()));
                        if (operationLog.isDebugEnabled())
                        {
                            operationLog.debug(getMemoryUsageMessage());
                        }
                    }
                }
                return null;
            }
        });

        if (operationLog.isInfoEnabled())
        {
            operationLog.info(String.format("Created %s %s properties : %s", entityIds.size(),
                    entityKind.getLabel(), property));
        }

        scheduleDynamicPropertiesEvaluation(entityIds);
    }

    private String getMemoryUsageMessage()
    {
        Runtime runtime = Runtime.getRuntime();
        long mb = 1024l * 1024l;
        long totalMemory = runtime.totalMemory() / mb;
        long freeMemory = runtime.freeMemory() / mb;
        long maxMemory = runtime.maxMemory() / mb;
        return "MEMORY (in MB): free:" + freeMemory + " total:" + totalMemory + " max:" + maxMemory;
    }

    @Override
    public void fillTermUsageStatistics(List<VocabularyTermWithStats> termsWithStats,
            VocabularyPE vocabulary)
    {
        assert termsWithStats != null : "Unspecified terms.";
        assert vocabulary != null : "Unspecified vocabulary.";
        assert termsWithStats.size() == vocabulary.getTerms().size() : "Sizes of terms to be filled and vocabulary terms don't match.";

        Map<Long, VocabularyTermWithStats> termsById =
                new HashMap<>(termsWithStats.size());
        for (VocabularyTermWithStats termWithStats : termsWithStats)
        {
            Long id = termWithStats.getTerm().getId();
            termsById.put(id, termWithStats);
        }

        Class<?> epClass = entityKind.getEntityPropertyClass();

        List<Object[]> results = doExecute(session -> session.createQuery(
                        "select count(ep), t.id " +
                                "from " + epClass.getName() + " ep " +
                                "join ep.vocabularyTerm t " +
                                "where t.vocabularyInternal = :vocab " +
                                "group by t.id",
                        Object[].class
                )
                .setParameter("vocab", vocabulary)
                .getResultList());

        assert results != null;
        for (Object[] result : results)
        {
            Integer numberOfUsages = ((Number) result[0]).intValue();
            Long termId = (Long) result[1];
            termsById.get(termId).registerUsage(entityKind, numberOfUsages);
        }
    }

    @Override
    public List<EntityPropertyPE> listPropertiesByVocabularyTerm(long vocabularyTermId)
    {
        // we have to fetch props.entity, because hibernate search has some problems with reindexing
        // otherwise
        String query =
                String.format(
                        "from %s props join fetch props.entity where props.vocabularyTerm.id = ?1",
                        entityKind.getEntityPropertyClass().getSimpleName());
        //
        List<EntityPropertyPE> properties =
                find(EntityPropertyPE.class, query, toArray(vocabularyTermId));
        if (operationLog.isDebugEnabled())
        {
            operationLog.debug(String.format("Term '%s' is used in %d properties of kind %s.",
                    vocabularyTermId, properties.size(), entityKind));
        }
        return properties;
    }

    @Override
    public void updateProperties(List<EntityPropertyPE> properties)
    {
        for (EntityPropertyPE entityProperty : properties)
        {
            doExecute(session -> session.save(entityProperty));
        }
        doExecute(session ->
        {
            session.flush();
            return null;
        });
        if (operationLog.isInfoEnabled())
        {
            operationLog.info("UPDATE: " + properties.size() + " of kind " + entityKind
                    + " updated.");
        }
    }

    @Override
    public void increaseOrdinals(EntityTypePE entityType, Long fromOrdinal, int increment)
    {
        assert entityType != null : "Unspecified entity type.";
        assert fromOrdinal != null : "Unspecified ordinal.";

        String hql =
                "update " + entityKind.getEntityTypePropertyTypeAssignmentClass().getSimpleName() + " etpt " +
                        "set etpt.ordinal = etpt.ordinal + :inc " +
                        "where etpt.entityTypeInternal = :etype and etpt.ordinal >= :fromOrd";

        int updatedRows =
                doExecute(session ->
                {
                    int upRows = session.createQuery(hql)
                            .setParameter("inc",
                                    (long) increment)   // or Integer if ordinal is an int
                            .setParameter("etype", entityType)
                            .setParameter("fromOrd", fromOrdinal)
                            .executeUpdate();
                    session.flush();
                    return upRows;
                });

        if (operationLog.isInfoEnabled())
        {
            operationLog.debug(String.format(
                    "%d etpt(s) updated for entity type '%s' with ordinal increased by %d.",
                    updatedRows, entityType.getCode(), increment));
        }
    }

    @Override
    public Long getMaxOrdinal(EntityTypePE entityType)
    {
        assert entityType != null : "Unspecified entity type.";

        String query =
                String.format("select max(etpt.ordinal) from %s etpt "
                                + "WHERE etpt.entityTypeInternal = ?1",
                        entityKind
                                .getEntityTypePropertyTypeAssignmentClass().getSimpleName());

        List<Long> resultList = find(Long.class, query, entityType);
        Long maxOrdinal = resultList.get(0);
        return maxOrdinal == null ? 0L : maxOrdinal;
    }

    @Override
    public final void validateAndSaveUpdatedEntity(EntityTypePropertyTypePE entity)
    {
        assert entity != null : "entity is null";

        validatePE(entity);
        flush();
    }

    @Override
    public int countAssignmentValues(String entityTypeCode, String propertyTypeCode)
    {
        assert entityTypeCode != null : "Unspecified entity type.";
        assert propertyTypeCode != null : "Unspecified property type.";

        String query =
                String.format("SELECT count(pv.id) FROM %s pa join %s pv "
                                + " ON pa.id = pv.entityTypePropertyType.id "
                                + " WHERE pa.propertyTypeInternal.simpleCode = ?1 "
                                + " AND pa.entityTypeInternal.code = ?2",
                        entityKind
                                .getEntityTypePropertyTypeAssignmentClass().getSimpleName(),
                        entityKind.getEntityPropertyClass().getSimpleName()
                );

        return find(Long.class, query,
                toArray(propertyTypeCode, entityTypeCode)).get(0).intValue();
    }

    @Override
    public void delete(EntityTypePropertyTypePE assignment)
    {

        List<Long> entityIds = listEntityIds(assignment);

        String hql = "delete from " + entityKind.getEntityPropertyClass().getSimpleName()
                + " ep where ep.entityTypePropertyType = :assignment";

        int affected =
                doExecute(session ->
                {
                    int updated = session
                            .createQuery(hql)
                            .setParameter("assignment", assignment)
                            .executeUpdate();
                    session.flush();
                    session.clear();

                    session.createQuery(
                                    "delete from " + assignment.getClass().getName() + " a where a.id = :id")
                            .setParameter("id", assignment.getId())
                            .executeUpdate();

                    session.flush();
                    return updated;
                });

        updateEntityModificationTimestamps(entityIds);
        scheduleDynamicPropertiesEvaluation(entityIds);

        if (operationLog.isInfoEnabled())
        {
            operationLog.info("DELETE: assignment between " + entityKind + " of type "
                    + assignment.getEntityType().getCode() + " and property type "
                    + assignment.getPropertyType().getCode());
        }
    }

    // helpers

    private void scheduleDynamicPropertiesEvaluation(List<Long> entityIds)
    {
        scheduleDynamicPropertiesEvaluationForIds(dynamicPropertyEvaluationScheduler,
                getIndexedEntityClass(entityKind), entityIds);
    }

    private static <T extends IEntityInformationWithPropertiesHolder> Class<T> getIndexedEntityClass(
            EntityKind entityKind)
    {
        switch (entityKind)
        {
            case DATA_SET:
                return cast(ExternalDataPE.class);
            default:
                return entityKind.getEntityClass();
        }
    }

    private static EntityKindPropertyTableNames getEntityKindPropertyTableNames(
            EntityKind entityKind)
    {
        switch (entityKind)
        {
            case DATA_SET:
                return new EntityKindPropertyTableNames(TableNames.DATA_SET_PROPERTIES_TABLE,
                        SequenceNames.DATA_SET_PROPERTY_SEQUENCE,
                        ColumnNames.DATA_SET_TYPE_PROPERTY_TYPE_COLUMN, ColumnNames.DATA_SET_COLUMN);
            case EXPERIMENT:
                return new EntityKindPropertyTableNames(TableNames.EXPERIMENT_PROPERTIES_TABLE,
                        SequenceNames.EXPERIMENT_PROPERTY_SEQUENCE,
                        ColumnNames.EXPERIMENT_TYPE_PROPERTY_TYPE_COLUMN,
                        ColumnNames.EXPERIMENT_COLUMN);
            case MATERIAL:
                return new EntityKindPropertyTableNames(TableNames.MATERIAL_PROPERTIES_TABLE,
                        SequenceNames.MATERIAL_PROPERTY_SEQUENCE,
                        ColumnNames.MATERIAL_TYPE_PROPERTY_TYPE_COLUMN, ColumnNames.MATERIAL_COLUMN);
            case SAMPLE:
                return new EntityKindPropertyTableNames(TableNames.SAMPLE_PROPERTIES_TABLE,
                        SequenceNames.SAMPLE_PROPERTY_SEQUENCE,
                        ColumnNames.SAMPLE_TYPE_PROPERTY_TYPE_COLUMN, ColumnNames.SAMPLE_COLUMN);
        }
        return null; // can't happen
    }

    private static class EntityKindPropertyTableNames
    {

        private final String propertiesTable;

        private final String propertiesSequence;

        private final String propertyTypeColumn;

        private final String entityColumn;

        public EntityKindPropertyTableNames(String propertiesTable, String propertiesSequence,
                String propertyTypeColumn, String entityColumn)
        {
            this.propertiesTable = propertiesTable;
            this.propertiesSequence = propertiesSequence;
            this.propertyTypeColumn = propertyTypeColumn;
            this.entityColumn = entityColumn;
        }

        public String getPropertiesTable()
        {
            return propertiesTable;
        }

        public String getPropertiesSequence()
        {
            return propertiesSequence;
        }

        public String getPropertyTypeColumn()
        {
            return propertyTypeColumn;
        }

        public String getEntityColumn()
        {
            return entityColumn;
        }

    }

}
