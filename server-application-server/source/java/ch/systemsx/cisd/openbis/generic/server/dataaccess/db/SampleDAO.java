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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import ch.ethz.sis.shared.log.classic.impl.Logger;

import org.hibernate.Session;

import org.hibernate.query.NativeQuery;

import org.hibernate.query.Query;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.support.JdbcAccessor;

import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.ISampleDAO;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.PersistencyResources;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.db.deletion.EntityHistoryCreator;
import ch.systemsx.cisd.openbis.generic.shared.basic.CodeConverter;
import ch.systemsx.cisd.openbis.generic.shared.basic.TechId;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.AttachmentHolderKind;
import ch.systemsx.cisd.openbis.generic.shared.dto.ColumnNames;
import ch.systemsx.cisd.openbis.generic.shared.dto.DataPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.DeletionPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.EventPE.EntityType;
import ch.systemsx.cisd.openbis.generic.shared.dto.PersonPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.ProjectPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.SamplePE;
import ch.systemsx.cisd.openbis.generic.shared.dto.SamplePropertyPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.SpacePE;
import ch.systemsx.cisd.openbis.generic.shared.dto.TableNames;
import ch.systemsx.cisd.openbis.generic.shared.dto.identifier.SampleIdentifier;

import static ch.systemsx.cisd.openbis.generic.server.dataaccess.db.DAOUtils.BATCH_SIZE;

/**
 * Implementation of {@link ISampleDAO} for databases.
 * 
 * @author Tomasz Pylak
 */
public class SampleDAO extends AbstractGenericEntityWithPropertiesDAO<SamplePE> implements
        ISampleDAO
{
    private final static Class<SamplePE> ENTITY_CLASS = SamplePE.class;

    /**
     * This logger does not output any SQL statement. If you want to do so, you had better set an appropriate debugging level for class
     * {@link JdbcAccessor}.
     * </p>
     */
    private static final Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION,
            SampleDAO.class);

    SampleDAO(final PersistencyResources persistencyResources, EntityHistoryCreator historyCreator)
    {
        super(persistencyResources, SamplePE.class, historyCreator);
    }

    // LockSampleModificationsInterceptor automatically obtains lock
    private SamplePE internalCreateOrUpdateSample(final SamplePE sample, final PersonPE modifier,
            final Session session, final boolean doLog)
    {
        validatePE(sample);
        sample.setCode(CodeConverter.tryToDatabase(sample.getCode()));
        if (sample.getModificationDate() == null)
        {
            sample.setModificationDate(getTransactionTimeStamp());
        }
        lockEntity(sample.getExperiment());
        lockEntity(sample.getContainer());
        lockEntities(sample.getParents());
        session.saveOrUpdate(sample);
        if (doLog && operationLog.isInfoEnabled())
        {
            operationLog.info(String.format("ADD: sample '%s'.", sample));
        }
        return sample;
    }

    //
    // ISampleDAO
    //

    @Override
    public final void createOrUpdateSample(final SamplePE sample, final PersonPE modifier)
            throws DataAccessException
    {
        assert sample != null : "Unspecified sample";

        try
        {

            SamplePE persistedSample = doExecute(session ->
                                internalCreateOrUpdateSample(sample, modifier, session, true));

            // need to deal with exception thrown by trigger checking code uniqueness
            flushWithSqlExceptionHandling();
            scheduleDynamicPropertiesEvaluation(Collections.singletonList(persistedSample));
            scheduleDynamicPropertiesEvaluation(getDynamicPropertyEvaluatorScheduler(),
                    DataPE.class, new ArrayList<DataPE>(persistedSample.getDatasets()));
        } catch (DataAccessException e)
        {
            SampleDataAccessExceptionTranslator.translateAndThrow(e);
        }
    }

    @Override
    public final List<SamplePE> listSamplesByGeneratedFrom(final SamplePE sample)
    {
        return sample.getGenerated();
    }

    @Override
    public final List<SamplePE> listSamplesBySpaceAndProperty(final String propertyCode,
            final String propertyValue, final SpacePE space) throws DataAccessException
    {
        assert space != null : "Unspecified space.";
        assert propertyCode != null : "Unspecified property code";
        assert propertyValue != null : "Unspecified property value";

        String queryFormat =
                "from " + SamplePropertyPE.class.getSimpleName()
                        + " where %s = ?1 and entity.space = ?2 "
                        + " and entityTypePropertyType.propertyTypeInternal.simpleCode = ?3"
                        + " and entityTypePropertyType.propertyTypeInternal.managedInternally = ?4";
        List<SamplePE> entities =
                listByPropertyValue(queryFormat, propertyCode, propertyValue, space);
        if (operationLog.isDebugEnabled())
        {
            operationLog.debug(String.format(
                    "%d samples have been found for space '%s' and property '%s' equal to '%s'.",
                    entities.size(), space, propertyCode, propertyValue));
        }
        return entities;
    }

    private List<SamplePE> listByPropertyValue(String queryFormat, String propertyCode,
            String propertyValue, SpacePE parent)
    {
        String simplePropertyCode = CodeConverter.tryToDatabase(propertyCode);
        boolean isInternalNamespace = CodeConverter.isInternalNamespace(propertyCode);
        Object[] arguments =
                toArray(propertyValue, parent, simplePropertyCode, isInternalNamespace);

        String queryPropertySimpleValue = String.format(queryFormat, "value");
        List<SamplePropertyPE> properties1 =
                find(SamplePropertyPE.class, queryPropertySimpleValue, arguments);

        String queryPropertyVocabularyTerm = String.format(queryFormat, "vocabularyTerm.simpleCode");
        List<SamplePropertyPE> properties2 =
                find(SamplePropertyPE.class, queryPropertyVocabularyTerm, arguments);

        properties1.addAll(properties2);
        List<SamplePE> entities = extractEntities(properties1);
        return entities;
    }

    private static List<SamplePE> extractEntities(List<SamplePropertyPE> properties)
    {
        List<SamplePE> samples = new ArrayList<SamplePE>();
        for (SamplePropertyPE prop : properties)
        {
            samples.add(prop.getEntity());
        }
        return samples;
    }

    @Override
    public SamplePE tryToFindByPermID(String permID) throws DataAccessException
    {
        assert permID != null : "Unspecified permanent ID.";
        SamplePE sample = currentSession()
                .createQuery(
                        "select distinct s " +
                                "from " + ENTITY_CLASS.getName() + " s " +
                                "join fetch s.sampleType st " +
                                "left join fetch st.sampleTypePropertyTypesInternal " +
                                "where s.permId = :permId",
                        ENTITY_CLASS
                )
                .setParameter("permId", permID)
                .uniqueResultOptional()
                .orElse(null);

        if (operationLog.isDebugEnabled())
        {
            operationLog.debug(String.format("Following sample '%s' has been found for "
                    + "permanent ID '%s'.", sample, permID));
        }
        return sample;
    }

    @Override
    public final SamplePE tryFindByCodeAndDatabaseInstance(final String sampleCode)
    {
        assert sampleCode != null : "Unspecified sample code.";

        SampleCodeParts parts = SampleCodeParts.from(sampleCode);
        List<String> baseConditions = new ArrayList<String>();
        baseConditions.add("s.space is null");

        SamplePE sample = findSample(parts, null, baseConditions, ContainerRestriction.MATCH_FROM_CODE);
        if (sample == null && parts.hasContainer() == false)
        {
            sample = findSample(parts, null, baseConditions, ContainerRestriction.REQUIRE_PRESENT);
        }
        if (operationLog.isDebugEnabled())
        {
            operationLog.debug(String
                    .format("Following sample '%s' has been found for "
                            + "code '%s'.", sample, sampleCode));
        }
        return sample;
    }

    @Override
    public final List<SamplePE> listByCodesAndDatabaseInstance(final List<String> sampleCodes,
            final String containerCodeOrNull)
    {
        assert sampleCodes != null : "Unspecified sample codes.";

        List<String> baseConditions = new ArrayList<String>();
        baseConditions.add("s.space is null");
        return listByCodes(sampleCodes, containerCodeOrNull, null, baseConditions);
    }

    @Override
    public SamplePE tryfindByCodeAndProject(String sampleCode, ProjectPE project)
    {
        assert sampleCode != null : "Unspecified sample code.";
        assert project != null : "Unspecified project.";

        SampleCodeParts parts = SampleCodeParts.from(sampleCode);
        List<String> baseConditions = new ArrayList<String>();
        baseConditions.add("s.projectInternal = :project");

        SamplePE sample = findSample(parts, query -> query.setParameter("project", project),
                baseConditions, ContainerRestriction.MATCH_FROM_CODE);
        if (sample == null && parts.hasContainer() == false)
        {
            sample = findSample(parts, query -> query.setParameter("project", project),
                    baseConditions, ContainerRestriction.REQUIRE_PRESENT);
        }
        if (operationLog.isDebugEnabled())
        {
            operationLog.debug(String.format(
                    "Following sample '%s' has been found for code '%s' and project '%s'.", sample,
                    sampleCode, project));
        }
        return sample;
    }

    @Override
    public final SamplePE tryFindByCodeAndSpace(final String sampleCode, final SpacePE space)
    {
        return tryFindByCodeAndSpace(sampleCode, space, false);
    }
    
    @Override
    public final SamplePE tryFindByCodeAndSpace(final String sampleCode, final SpacePE space, boolean ignoringProject)
    {
        assert sampleCode != null : "Unspecified sample code.";
        assert space != null : "Unspecified space.";

        SampleCodeParts parts = SampleCodeParts.from(sampleCode);
        List<String> baseConditions = new ArrayList<String>();
        baseConditions.add("s.space = :space");
        if (ignoringProject == false)
        {
            baseConditions.add("s.projectInternal is null");
        }

        SamplePE sample = findSample(parts, query -> query.setParameter("space", space),
                baseConditions, ContainerRestriction.MATCH_FROM_CODE);
        if (sample == null && parts.hasContainer() == false)
        {
            sample = findSample(parts, query -> query.setParameter("space", space), baseConditions,
                    ContainerRestriction.REQUIRE_PRESENT);
        }
        if (operationLog.isDebugEnabled())
        {
            operationLog.debug(String.format(
                    "Following sample '%s' has been found for code '%s' and space '%s'.", sample,
                    sampleCode, space));
        }
        return sample;
    }

    @Override
    public final List<SamplePE> listByCodesAndSpace(final List<String> sampleCodes,
            final String containerCodeOrNull, final SpacePE space)
    {
        assert sampleCodes != null : "Unspecified sample codes.";
        assert space != null : "Unspecified space.";

        List<String> baseConditions = new ArrayList<String>();
        baseConditions.add("s.space = :space");
        baseConditions.add("s.projectInternal is null");

        return listByCodes(sampleCodes, containerCodeOrNull,
                query -> query.setParameter("space", space), baseConditions);
    }

    @Override
    public List<SamplePE> listByCodesAndProject(List<String> sampleCodes, String containerCodeOrNull, ProjectPE project)
    {
        assert sampleCodes != null : "Unspecified sample codes.";
        assert project != null : "Unspecified project.";

        List<String> baseConditions = new ArrayList<String>();
        baseConditions.add("s.projectInternal = :project");

        return listByCodes(sampleCodes, containerCodeOrNull,
                query -> query.setParameter("project", project), baseConditions);
    }

    private List<SamplePE> listByCodes(List<String> sampleCodes, String containerCodeOrNull,
            Consumer<Query<SamplePE>> parameterBinder, List<String> baseConditions)
    {
        if (sampleCodes == null || sampleCodes.isEmpty())
        {
            return new ArrayList<SamplePE>();
        }

        List<String> convertedCodes = new ArrayList<String>(sampleCodes.size());
        for (String sampleCode : sampleCodes)
        {
            convertedCodes.add(CodeConverter.tryToDatabase(sampleCode));
        }

        List<String> conditions = new ArrayList<String>();
        if (baseConditions != null)
        {
            for (String condition : baseConditions)
            {
                if (condition != null && condition.isEmpty() == false)
                {
                    conditions.add(condition);
                }
            }
        }
        conditions.add("s.code in (:codes)");
        if (containerCodeOrNull != null)
        {
            conditions.add("s.container.code = :containerCode");
        } else
        {
            conditions.add("s.container is null");
        }

        Query<SamplePE> query = createSampleSelectionQuery(conditions, true);
        query.setParameterList("codes", convertedCodes);
        if (containerCodeOrNull != null)
        {
            query.setParameter("containerCode", CodeConverter.tryToDatabase(containerCodeOrNull));
        }
        if (parameterBinder != null)
        {
            parameterBinder.accept(query);
        }

        List<SamplePE> result = query.getResultList();
        if (operationLog.isDebugEnabled())
        {
            operationLog.debug(String.format("%s samples has been found", result.size()));
        }
        return result;
    }

    private SamplePE findSample(SampleCodeParts parts,
            Consumer<Query<SamplePE>> parameterBinder,
            List<String> baseConditions, ContainerRestriction containerRestriction)
    {
        List<String> conditions = new ArrayList<String>();
        if (baseConditions != null)
        {
            for (String condition : baseConditions)
            {
                if (condition != null && condition.isEmpty() == false)
                {
                    conditions.add(condition);
                }
            }
        }
        conditions.add("s.code = :code");

        switch (containerRestriction)
        {
            case MATCH_FROM_CODE:
                if (parts.hasContainer())
                {
                    conditions.add("s.container.code = :containerCode");
                } else
                {
                    conditions.add("s.container is null");
                }
                break;
            case REQUIRE_PRESENT:
                conditions.add("s.container is not null");
                break;
            case REQUIRE_ABSENT:
                conditions.add("s.container is null");
                break;
            default:
                break;
        }

        Query<SamplePE> query = createSampleSelectionQuery(conditions, false);
        query.setParameter("code", parts.dbSubCode);
        if (containerRestriction == ContainerRestriction.MATCH_FROM_CODE && parts.hasContainer())
        {
            query.setParameter("containerCode", parts.dbContainerCode);
        }
        if (parameterBinder != null)
        {
            parameterBinder.accept(query);
        }

        return query.uniqueResultOptional().orElse(null);
    }

    private Query<SamplePE> createSampleSelectionQuery(List<String> conditions, boolean fetchProperties)
    {
        StringBuilder hql = new StringBuilder();
        hql.append("select distinct s from ").append(ENTITY_CLASS.getName()).append(" s ")
                .append("join fetch s.sampleType st ")
                .append("left join fetch st.sampleTypePropertyTypesInternal ");
        if (fetchProperties)
        {
            hql.append("left join fetch s.sampleProperties sp ");
        }
        if (conditions != null && conditions.isEmpty() == false)
        {
            hql.append("where ").append(String.join(" and ", conditions)).append(' ');
        }
        return currentSession().createQuery(hql.toString(), ENTITY_CLASS);
    }

    private enum ContainerRestriction
    {
        MATCH_FROM_CODE,
        REQUIRE_PRESENT,
        REQUIRE_ABSENT
    }

    private static final class SampleCodeParts
    {
        private final String dbSubCode;
        private final String dbContainerCode;
        private final boolean hasContainer;

        private SampleCodeParts(String dbSubCode, String dbContainerCode, boolean hasContainer)
        {
            this.dbSubCode = dbSubCode;
            this.dbContainerCode = dbContainerCode;
            this.hasContainer = hasContainer;
        }

        static SampleCodeParts from(String sampleCode)
        {
            String[] sampleCodeTokens = sampleCode
                    .split(SampleIdentifier.CONTAINED_SAMPLE_CODE_SEPARARTOR_STRING);
            String subCodeRaw = sampleCodeTokens.length > 1 ? sampleCodeTokens[1] : sampleCode;
            String containerRaw = sampleCodeTokens.length > 1 ? sampleCodeTokens[0] : null;

            String dbSubCode = CodeConverter.tryToDatabase(subCodeRaw);
            String dbContainerCode = containerRaw == null ? null
                    : CodeConverter.tryToDatabase(containerRaw);

            return new SampleCodeParts(dbSubCode, dbContainerCode, containerRaw != null);
        }

        boolean hasContainer()
        {
            return hasContainer;
        }
    }

    @Override
    public final void createOrUpdateSamples(final List<SamplePE> samples, final PersonPE modifier,
            boolean clearCache)
            throws DataAccessException
    {
        assert samples != null && samples.size() > 0 : "Unspecified or empty samples.";

        try
        {
            doExecute(session -> {

                for (final SamplePE samplePE : samples)
                {
                    internalCreateOrUpdateSample(samplePE, modifier, session, false);
                }
                if (operationLog.isInfoEnabled())
                {
                    operationLog.info(String.format("ADD: %d samples.", samples.size()));
                }

                return null;
            });
            // need to deal with exception thrown by trigger checking code uniqueness
            flushWithSqlExceptionHandling();
            scheduleDynamicPropertiesEvaluation(samples);

            if (clearCache)
            {
                currentSession().clear();
            }
        } catch (DataAccessException e)
        {
            SampleDataAccessExceptionTranslator.translateAndThrow(e);
        }
    }

    @Override
    public final void updateSample(final SamplePE sample, final PersonPE modifier)
            throws DataAccessException
    {
        assert sample != null : "Unspecified sample";

        try
        {
            sample.setModifier(modifier);
            validatePE(sample);

            // need to deal with exception thrown by trigger checking code uniqueness
            flushWithSqlExceptionHandling();
            scheduleDynamicPropertiesEvaluation(Collections.singletonList(sample));

            if (operationLog.isInfoEnabled())
            {
                operationLog.info("UPDATE: sample '" + sample + "'.");
            }
        } catch (DataAccessException e)
        {
            SampleDataAccessExceptionTranslator.translateAndThrow(e);
        }
    }

    @Override
    public List<SamplePE> listByPermID(Collection<String> values)
    {
        return listByIDsOfName("permId", values);
    }

    @Override
    public List<SamplePE> listByIDs(Collection<Long> ids)
    {
        return listByIDsOfName("id", ids);
    }

    private List<SamplePE> listByIDsOfName(String idName, Collection<?> values)
    {
        if (values == null || values.isEmpty())
        {
            return new ArrayList<SamplePE>();
        }
        List<?> allValues = new ArrayList<>(values);
        List<SamplePE> list = new ArrayList<>(allValues.size());

        for (int i = 0; i < allValues.size(); i += BATCH_SIZE)
        {
            List<?> slice = allValues.subList(i, Math.min(allValues.size(), i + BATCH_SIZE));
            if (slice.isEmpty())
            {
                continue;
            }

// Hibernate 6, use this one
        String hql =
                "select  s " +
                        "from SamplePE s " +
                        "where s." + idName + " in (:ids)"
                        ;

        List<SamplePE> batch = doExecute(session ->
                session.createQuery(hql, SamplePE.class)
                        .setParameterList("ids", slice)
                        .getResultList());

            list.addAll(batch);
        }

        if (operationLog.isDebugEnabled())
        {
            operationLog.debug(String.format("%d sample(s) have been found.", list.size()));
        }
        return list;
    }

    @Override
    public void delete(final List<TechId> sampleIds, final PersonPE registrator, final String reason)
            throws DataAccessException
    {
        // NOTE: we use SAMPLES_ALL_TABLE, not DELETED_SAMPLES_VIEW because we still want to be
        // able to directly delete samples without going to trash (trash may be disabled)
        final String samplesTable = TableNames.SAMPLES_ALL_TABLE;

        final String sqlSelectPermIds = SQLBuilder.createSelectPermIdsSQL(samplesTable);
        final String sqlDeleteProperties =
                SQLBuilder.createDeletePropertiesSQL(TableNames.SAMPLE_PROPERTIES_TABLE,
                        ColumnNames.SAMPLE_COLUMN);
        final String sqlDeleteAttachments =
                SQLBuilder.createDeleteAttachmentsSQL(ColumnNames.SAMPLE_COLUMN);
        final String sqlDeleteSamples = SQLBuilder.createDeleteEnitiesSQL(samplesTable);
        final String sqlInsertEvent = SQLBuilder.createInsertEventSQL();
        final String sqlSelectPropertyHistory = createQueryPropertyHistorySQL();
        final String sqlSelectRelationshipHistory = createQueryRelationshipHistorySQL();
        final String sqlSelectAttributes = createQueryAttributesSQL();

        executePermanentDeleteAction(EntityType.SAMPLE, sampleIds, registrator, reason,
                sqlSelectPermIds, sqlDeleteProperties,
                sqlDeleteAttachments, sqlDeleteSamples, sqlInsertEvent, sqlSelectPropertyHistory,
                sqlSelectRelationshipHistory, sqlSelectAttributes, null, AttachmentHolderKind.SAMPLE);
        currentSession().clear();
    }

    private static String createQueryPropertyHistorySQL()
    {
        return "("
                + "SELECT s.perm_id, pt.code, coalesce(h.value, h.vocabulary_term) as value, "
                + "p.user_id, h.valid_from_timestamp, h.valid_until_timestamp "
                + "FROM samples_all s, sample_properties_history h, sample_type_property_types stpt, property_types pt, persons p "
                + "WHERE h.samp_id " + SQLBuilder.inEntityIds() + " AND "
                + "s.id = h.samp_id AND "
                + "h.stpt_id = stpt.id AND "
                + "stpt.prty_id = pt.id AND "
                + "pers_id_author = p.id "
                + ") UNION ("
                + "SELECT s.perm_id, pt.code, coalesce(value, "
                + "(SELECT (t.code || ' [' || v.code || ']') "
                + "FROM controlled_vocabulary_terms as t JOIN controlled_vocabularies as v ON t.covo_id = v.id "
                + "WHERE t.id = pr.cvte_id)) as value, "
                + "author.user_id, pr.modification_timestamp, null "
                + "FROM samples_all s, sample_properties pr, sample_type_property_types stpt, property_types pt, persons author "
                + "WHERE pr.samp_id " + SQLBuilder.inEntityIds() + " AND "
                + "s.id = pr.samp_id AND "
                + "pr.stpt_id = stpt.id AND "
                + "stpt.prty_id = pt.id AND "
                + "pr.pers_id_author = author.id "
                + ") "
                + " ORDER BY 1, valid_from_timestamp";
    }

    private static String createQueryRelationshipHistorySQL()
    {
        return "SELECT s.perm_id, h.relation_type, h.entity_perm_id, " + ENTITY_TYPE + ", "
                + "p.user_id, h.valid_from_timestamp, h.valid_until_timestamp "
                + "FROM samples_all s, sample_relationships_history h, persons p "
                + "WHERE s.id = h.main_samp_id AND "
                + "h.main_samp_id " + SQLBuilder.inEntityIds() + " AND "
                + "h.pers_id_author = p.id "
                + "ORDER BY 1, valid_from_timestamp";
    }

    private static final String ENTITY_TYPE = "case "
            + "when h.space_id is not null then 'SPACE' "
            + "when h.samp_id is not null then 'SAMPLE' "
            + "when h.proj_id is not null then 'PROJECT' "
            + "when h.expe_id is not null then 'EXPERIMENT' "
            + "else 'UNKNOWN' end as entity_type";

    private static String createQueryAttributesSQL()
    {
        return "SELECT s.id, s.perm_id, s.code, t.code as entity_type, "
                + "s.registration_timestamp, r.user_id as registrator "
                + "FROM samples_all s "
                + "JOIN sample_types t on s.saty_id = t.id "
                + "JOIN persons r on s.pers_id_registerer = r.id "
                + "WHERE s.id " + SQLBuilder.inEntityIds();
    }

    @Override
    public void deletePermanently(final DeletionPE deletion, final PersonPE registrator)
    {
         doExecute( session -> delete(deletion, registrator, session));
    }

    private Object delete(DeletionPE deletion, PersonPE registrator, Session session)
    {
        String permIdQuery = "SELECT id, perm_id FROM samples_all WHERE del_id = :id";

        String properties =
                "DELETE FROM sample_properties WHERE samp_id IN ("
                        + "SELECT id FROM samples_all WHERE del_id = :id)";

        String attachmentContentIdQuery =
                "SELECT exac_id FROM attachments WHERE samp_id IN (SELECT id FROM samples_all WHERE del_id = :id)";

        String attachments =
                "DELETE FROM attachments WHERE samp_id IN ("
                        + "SELECT id FROM samples_all WHERE del_id = :id)";

        String attachmentContents =
                "DELETE FROM attachment_contents WHERE id IN (:ids)";

        String samples =
                "DELETE FROM samples_all WHERE del_id = :id";

        String event =
                "INSERT INTO events (id, event_type, description, reason, pers_id_registerer, entity_type, identifiers, content) "
                        + "VALUES (nextval('EVENT_ID_SEQ'), 'DELETION', :description, :reason, :registerer, 'SAMPLE', :identifiers, :content)";


        NativeQuery<?> getPermIds = session.createNativeQuery(permIdQuery);
        getPermIds.setParameter("id", deletion.getId());

        StringBuffer permIdList = new StringBuffer();
        List<Long> entityIdsToDelete = new ArrayList<>();
        for (Object rowObj : getPermIds.getResultList())
        {
            Object[] result = (Object[]) rowObj;
            permIdList.append(", ");
            permIdList.append((String) result[1]);
            Number idVal = (Number) result[0];
            entityIdsToDelete.add(idVal != null ? idVal.longValue() : null);
        }

        if (permIdList.length() == 0)
        {
            return null;
        }

        String permIds = permIdList.substring(2);

        InQueryScroller<Long> entityIdsToDeleteScroller = new InQueryScroller<>(entityIdsToDelete, 16384 /*
                                                                                                          * createQueryPropertyHistorySQL
                                                                                                          * uses the parameters twice
                                                                                                          */);
        List<Long> partialEntityIdsToDelete = null;
        String content = "";
        while ((partialEntityIdsToDelete = entityIdsToDeleteScroller.next()) != null)
        {
            if (content.length() > 0)
            {
                content += ", ";
            }
            content += historyCreator.apply(session, partialEntityIdsToDelete, createQueryPropertyHistorySQL(),
                    createQueryRelationshipHistorySQL(), createQueryAttributesSQL(), null,
                    AttachmentHolderKind.SAMPLE, registrator);
        }

        Query deleteProperties = session.createNativeQuery(properties);
        deleteProperties.setParameter("id", deletion.getId());
        deleteProperties.executeUpdate();

        NativeQuery<Long> getAttachmentContentIds =
                session.createNativeQuery(attachmentContentIdQuery);
        getAttachmentContentIds.addScalar("exac_id", StandardBasicTypes.LONG);
        getAttachmentContentIds.setParameter("id", deletion.getId());
        List<Long> attachmentContentIdList = getAttachmentContentIds.getResultList();

        Query deleteAttachments = session.createNativeQuery(attachments);
        deleteAttachments.setParameter("id", deletion.getId());
        deleteAttachments.executeUpdate();

        // if (attachmentContentIdList.size() > 0)
        // {
        // MutationQuery deleteAttachmentContents =
        // session.createNativeMutationQuery(attachmentContents);
        // deleteAttachmentContents.setParameterList("ids", attachmentContentIdList);
        // deleteAttachmentContents.executeUpdate();
        // }
        //
        Query deleteSamples = session.createNativeQuery(samples);
        deleteSamples.setParameter("id", deletion.getId());
        deleteSamples.executeUpdate();

        Query insertEvent = session.createNativeQuery(event);
        insertEvent.setParameter("description", permIds);
        insertEvent.setParameter("reason", deletion.getReason());
        insertEvent.setParameter("registerer", registrator.getId());
        insertEvent.setParameter("identifiers", permIds);
        insertEvent.setParameter("content", content);
        insertEvent.executeUpdate();

        return null;
    }

    @Override
    public Set<TechId> listSampleIdsByChildrenIds(final Collection<TechId> children,
            final TechId relationship)
    {
        final String query =
                "select sample_id_parent from " + TableNames.SAMPLE_RELATIONSHIPS_VIEW
                        + " where sample_id_child in (:ids) and relationship_id = :r ";

        InQuery<Long, ? extends Number> inQuery = new InQuery<>();
        Map<String, Object> fixParams = new HashMap<String, Object>();
        fixParams.put("r", relationship.getId());

        final List<Long> longIds = TechId.asLongs(children);
        final List<? extends Number> results = doExecute(session ->  inQuery.withBatch(session, query, "ids", longIds, fixParams));
        Set<TechId> result = transformNumbers2TechIdSet(results);
        if (operationLog.isDebugEnabled())
        {
            operationLog.debug(String.format("found %d sample parents for given children",
                    results.size()));
        }
        return result;
    }

    @Override
    public Map<Long, Set<Long>> mapSampleIdsByChildrenIds(final Collection<Long> children, final Long relationship)
    {
        final String query = "select sample_id_child, sample_id_parent from " + TableNames.SAMPLE_RELATIONSHIPS_VIEW
                + " where relationship_id = :relationship and sample_id_child in (:children)";

        InQuery<Long, Object[]> inQuery = new InQuery<>();
        Map<String, Object> fixParams = new HashMap<String, Object>();
        fixParams.put("relationship", relationship);

        List<Object[]> results = inQuery.withBatch(currentSession(), query, "children", new ArrayList<>(children), fixParams);

        Map<Long, Set<Long>> childIdToParentIdsMap = new HashMap<>();

        for (Object[] result : results)
        {
            Number childId = (Number) result[0];
            Number parentId = (Number) result[1];

            Set<Long> parentIds = childIdToParentIdsMap.get(childId.longValue());
            if (parentIds == null)
            {
                parentIds = new HashSet<Long>();
                childIdToParentIdsMap.put(childId.longValue(), parentIds);
            }

            parentIds.add(parentId.longValue());
        }

        return childIdToParentIdsMap;
    }

    @Override
    public Set<TechId> listSampleIdsByParentIds(Collection<TechId> parentIds)
    {
        return listChildrenIds(parentIds, TableNames.SAMPLE_RELATIONSHIPS_VIEW);
    }

    @Override
    public Set<TechId> listChildrenForTrashedSamples(Collection<TechId> parentIds)
    {
        return listChildrenIds(parentIds, TableNames.SAMPLE_RELATIONSHIPS_ALL_TABLE);
    }

    private Set<TechId> listChildrenIds(final Collection<TechId> parents, String tableName)
    {
        final String query = "SELECT sample_id_child FROM " + tableName + " WHERE sample_id_parent IN (:ids)";


        final List<Long> longIds = TechId.asLongs(parents);
        InQuery<Long, Number> inQuery = new InQuery<>();
        List<Number> results = doExecute(session ->  inQuery.withBatch(session, query, "ids", longIds, null));
        Set<TechId> result = transformNumbers2TechIdSet(results);
        if (operationLog.isDebugEnabled())
        {
            operationLog.info(String.format("found %d sample children for given parents",
                    results.size()));
        }
        return result;
    }

    @Override
    public List<TechId> listSampleIdsByContainerIds(final Collection<TechId> containers)
    {
        final List<Long> longIds = TechId.asLongs(containers);
        return listSampleIdsByColumn("container.id", longIds, "sample components for given containers");
    }

    @Override
    public List<TechId> listSampleIdsBySampleTypeIds(Collection<TechId> sampleTypeIds)
    {
        final List<Long> longIds = TechId.asLongs(sampleTypeIds);
        return listSampleIdsByColumn("sampleType.id", longIds, "samples for given sample types");
    }

    @Override
    public List<TechId> listSampleIdsByExperimentIds(final Collection<TechId> experiments)
    {
        final List<Long> longIds = TechId.asLongs(experiments);
        return listSampleIdsByColumn("experimentInternal.id", longIds, "samples for given experiments");
    }

    @Override
    public List<TechId> listSampleIdsByProjectIds(final Collection<TechId> projects)
    {
        final List<Long> longIds = TechId.asLongs(projects);
        return listSampleIdsByColumn("projectInternal.id", longIds, "samples for given projects");
    }

    private List<TechId> listSampleIdsByColumn(String columnName, final List<Long> longIds, String message)
    {
        List<?> allValues = new ArrayList<>(longIds);
        List<Long> results = new ArrayList<>(allValues.size());

        for (int i = 0; i < allValues.size(); i += BATCH_SIZE)
        {
            List<?> slice = allValues.subList(i, Math.min(allValues.size(), i + BATCH_SIZE));
            if (slice.isEmpty()){
                continue;
            }
            List<Long> batch = doExecute(session -> session.createQuery(
                                    "select e.id from " + SamplePE.class.getName() +
                                            " e where e." + columnName + " in (:ids)",
                                    Long.class
                            )
                            .setParameterList("ids", slice)
                            .getResultList());

            results.addAll(batch);
        }

        operationLog.info(String.format("found %s " + message, results.size()));
        return transformNumbers2TechIdList(results);
    }

    @Override
    public void setSampleContainer(final Long sampleId, final Long containerId)
    {
        doExecute(session -> {
            Query q = session.createNativeQuery(
                    "update samples set samp_id_part_of = :containerId where id = :sampleId");
            q.setParameter("containerId", containerId);
            q.setParameter("sampleId", sampleId);
            q.executeUpdate();
            return null;
        });

    }

    @Override
    public void setSampleContained(final Long sampleId, final Collection<Long> containedIds)
    {
        doExecute(session -> {
            Query clearQuery = session.createNativeQuery(
                    "update samples set samp_id_part_of = null where id not in :containedIds and samp_id_part_of = :containerId");
            clearQuery.setParameter("containerId", sampleId);
            clearQuery.setParameterList("containedIds", containedIds);
            clearQuery.executeUpdate();

            addSampleContained(sampleId, containedIds);
            return null;
        });

    }

    @Override
    public void addSampleContained(final Long sampleId, final Collection<Long> containedIds)
    {
        doExecute(session -> {
            Query setQuery = session.createNativeQuery(
                    "update samples set samp_id_part_of = :containerId where id in :containedIds");
            setQuery.setParameter("containerId", sampleId);
            setQuery.setParameterList("containedIds", containedIds);
            setQuery.executeUpdate();
           return null;
        });
    }

    @Override
    public void removeSampleContained(final Long sampleId, final Collection<Long> containedIds)
    {

        doExecute(session -> {
            Query clearQuery = session.createNativeQuery(
                    "update samples set samp_id_part_of = null where id in :containedIds and samp_id_part_of = :containerId");
            clearQuery.setParameter("containerId", sampleId);
            clearQuery.setParameterList("containedIds", containedIds);
            clearQuery.executeUpdate();
            return null;
        });

    }

    @Override
    public void setSampleRelationshipChildren(final Long sampleId, final Collection<Long> childrenIds, final Long relationshipId,
            final PersonPE author)
    {
        doExecute(session -> {
            Query q = session.createNativeQuery(
                    "delete from sample_relationships where sample_id_child not in :childrenIds and sample_id_parent = :parentId and relationship_id = :relationshipId");
            q.setParameterList("childrenIds", childrenIds);
            q.setParameter("parentId", sampleId);
            q.setParameter("relationshipId", relationshipId);

            q.executeUpdate();

            addSampleRelationshipChildren(sampleId, childrenIds, relationshipId, author);
            return null;
        });
    }

    @Override
    public void addSampleRelationshipChildren(final Long sampleId, final Collection<Long> childrenIds, final Long relationshipId,
            final PersonPE author)
    {
        doExecute(session -> {
            for (Long relatedSampleId : childrenIds)
            {
                Query q = session.createNativeQuery(
                        "insert into sample_relationships (id, sample_id_parent, sample_id_child, relationship_id, pers_id_author, registration_timestamp, modification_timestamp) "
                                + "select nextval('sample_relationship_id_seq'),  :parentId, :childId, :relationshipId, :authorId, now(), now() where not exists "
                                + "(select 1 from sample_relationships where sample_id_parent = :parentId and sample_id_child = :childId and relationship_id = :relationshipId)");
                q.setParameter("parentId", sampleId);
                q.setParameter("childId", relatedSampleId);
                q.setParameter("relationshipId", relationshipId);
                q.setParameter("authorId", author.getId());

                q.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public void removeSampleRelationshipChildren(final Long sampleId, final Collection<Long> childrenIds, final Long relationshipId,
            final PersonPE author)
    {
        doExecute(session -> {
            Query q = session.createNativeQuery(
                    "delete from sample_relationships where sample_id_parent = :parentId and sample_id_child in :childrenIds and relationship_id = :relationshipId");
            q.setParameter("parentId", sampleId);
            q.setParameterList("childrenIds", childrenIds);
            q.setParameter("relationshipId", relationshipId);

            q.executeUpdate();
            return null;
        });
    }

    @Override
    public void setSampleRelationshipParents(final Long sampleId, final Collection<Long> parentsIds, final Long relationshipId, final PersonPE author)
    {
        doExecute(session -> {
            Query q = session.createNativeQuery(
                    "delete from sample_relationships where sample_id_parent not in :parentIds and sample_id_child = :childId and relationship_id = :relationshipId");
            q.setParameterList("parentIds", parentsIds);
            q.setParameter("childId", sampleId);
            q.setParameter("relationshipId", relationshipId);

            q.executeUpdate();

            addSampleRelationshipParents(sampleId, parentsIds, relationshipId, author);
            return null;
        });
    }

    @Override
    public void addSampleRelationshipParents(final Long sampleId, final Collection<Long> parentsIds, final Long relationshipId, final PersonPE author)
    {
        doExecute(session -> {
            for (Long parentId : parentsIds)
            {
                Query q = session.createNativeQuery(
                        "insert into sample_relationships (id, sample_id_parent, sample_id_child, relationship_id, pers_id_author, registration_timestamp, modification_timestamp) "
                                + "select nextval('sample_relationship_id_seq'),  :parentId, :childId, :relationshipId, :authorId, now(), now() where not exists "
                                + "(select 1 from sample_relationships where sample_id_parent = :parentId and sample_id_child = :childId and relationship_id = :relationshipId)");
                q.setParameter("parentId", parentId);
                q.setParameter("childId", sampleId);
                q.setParameter("relationshipId", relationshipId);
                q.setParameter("authorId", author.getId());

                q.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public void removeSampleRelationshipParents(final Long sampleId, final Collection<Long> parentsIds, final Long relationshipId,
            final PersonPE author)
    {
        doExecute(session -> {
            Query q = session.createNativeQuery(
                    "delete from sample_relationships where sample_id_parent in :parentIds and sample_id_child = :childId and relationship_id = :relationshipId");
            q.setParameterList("parentIds", parentsIds);
            q.setParameter("childId", sampleId);
            q.setParameter("relationshipId", relationshipId);

            q.executeUpdate();
            return null;
        });
    }

    @Override
    public SamplePE tryGetByIdWithTypePropertyTypesAndExperiment(TechId sampleId)
    {
        return (SamplePE) currentSession()
                .createQuery(
                        "select s from SamplePE s " +
                                "left join fetch s.sampleType st " +
                                "left join fetch st.sampleTypePropertyTypesInternal " +
                                "left join fetch s.experimentInternal " +
                                "where s.id = :id"
                )
                .setParameter("id", sampleId.getId())
                .uniqueResult();
    }


    @Override
    Logger getLogger()
    {
        return operationLog;
    }

}
