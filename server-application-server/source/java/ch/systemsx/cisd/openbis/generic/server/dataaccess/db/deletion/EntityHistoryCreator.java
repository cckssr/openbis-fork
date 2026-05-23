/*
 * Copyright ETH 2015 - 2023 Zürich, Scientific IT Services
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
package ch.systemsx.cisd.openbis.generic.server.dataaccess.db.deletion;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.hibernate.query.NativeQuery;
import org.hibernate.SharedSessionContract;
import org.hibernate.transform.ResultTransformer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import ch.systemsx.cisd.common.collection.SimpleComparator;
import ch.systemsx.cisd.common.time.DateFormatThreadLocal;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.IAttachmentDAO;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.IDAOFactory;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.AttachmentHolderKind;
import ch.systemsx.cisd.openbis.generic.shared.dto.AttachmentHolderPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.AttachmentPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.PersonPE;

public class EntityHistoryCreator
{
    private static final String ENTITY_IDS = "entityIds";

    private static final Set<String> NON_ATTRIBUTE_COLUMNS = new HashSet<>(Arrays.asList("ID", "PERM_ID"));

    private boolean enabled = false;

    private IDAOFactory daoFactory;

    public void setDaoFactory(IDAOFactory daoFactory)
    {
        this.daoFactory = daoFactory;
    }

    public void setEnabled(String s)
    {
        if (s != null && s.length() > 0 && s.equalsIgnoreCase("false"))
        {
            enabled = false;
        } else
        {
            enabled = true;
        }
    }

    public String apply(SharedSessionContract session, List<Long> entityIdsToDelete,
            String propertyHistoryQuery, String relationshipHistoryQuery, String attributesQuery,
            List<? extends AttachmentHolderPE> attachmentHolders, AttachmentHolderKind attachmentHolderKind,
            PersonPE registrator)
    {
        if (!enabled)
        {
            return "";
        }
        Map<String, List<? extends EntityModification>> histories = new HashMap<String, List<? extends EntityModification>>();

        if (attributesQuery != null)
        {
            List<AttributeEntry> attributeEntries =
                    selectAttributeEntries(session.createNativeQuery(attributesQuery), entityIdsToDelete);
            addToHistories(histories, attributeEntries);
        }

        List<PropertyHistoryEntry> propertyHistory =
                selectHistoryPropertyEntries(session.createNativeQuery(propertyHistoryQuery), entityIdsToDelete);
        addToHistories(histories, propertyHistory);

        if (relationshipHistoryQuery != null)
        {
            List<RelationshipHistoryEntry> relationshipHistory =
                    selectRelationshipHistoryEntries(session.createNativeQuery(relationshipHistoryQuery),
                            entityIdsToDelete);
            addToHistories(histories, relationshipHistory);
        }
        daoFactory.getSessionFactory().getCurrentSession().flush();
        daoFactory.getSessionFactory().getCurrentSession().clear();

        if (attachmentHolders != null)
        {
            List<RelationshipHistoryEntry> deletedAttachments = deleteAttachments(session, registrator, attachmentHolders);
            addToHistories(histories, deletedAttachments);
        }
        if (attachmentHolderKind != null)
        {
            List<RelationshipHistoryEntry> deletedAttachments = deleteAttachments(session, registrator, attachmentHolderKind, entityIdsToDelete);
            addToHistories(histories, deletedAttachments);
        }

        return jsonize(histories);
    }

    public String jsonize(Map<String, List<? extends EntityModification>> modifications)
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        ObjectWriter ow = new ObjectMapper().setDateFormat(dateFormat).writer().withDefaultPrettyPrinter();
        String content;
        try
        {
            content = ow.writeValueAsString(modifications);
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        return content;
    }

    private List<RelationshipHistoryEntry> deleteAttachments(SharedSessionContract session,
            PersonPE registrator, AttachmentHolderKind attachmentHolderKind, List<Long> holderIds)
    {
        NativeQuery sqlQuery = session.createNativeQuery(createSelectHolderStatement(attachmentHolderKind));
        List<AttachmentHolder> attachmentHolders = new ArrayList<>();
        Map<Long, AttachmentHolder> attachmentHoldersById = new HashMap<>();
        List<Map<String, Object>> rows = getRows(sqlQuery, holderIds);
        for (Map<String, Object> row : rows)
        {
            AttachmentHolder attachmentHolder = new AttachmentHolder();
            attachmentHolder.id = ((Number) row.get("ID")).longValue();
            attachmentHolder.permId = (String) row.get("PERM_ID");
            attachmentHolders.add(attachmentHolder);
            attachmentHoldersById.put(attachmentHolder.id, attachmentHolder);
        }
        Map<String, AttachmentEntry> attachmentEntries = new TreeMap<>();
        NativeQuery insertQuery = session.createNativeQuery("INSERT INTO events (id, event_type, description, reason, pers_id_registerer, "
                + "entity_type, identifiers, content, exac_id) "
                + "VALUES (nextval('EVENT_ID_SEQ'), 'DELETION', :description, '', :registerer, "
                + "'ATTACHMENT', :identifiers, :content, :attachment)");
        sqlQuery = session.createNativeQuery(createSelectAttachmentsStatement(attachmentHolderKind));
        List<Map<String, Object>> rowList = getRows(sqlQuery, holderIds);
        for (Map<String, Object> row : rowList)
        {
            long holderId = ((Number) row.get("HOLDER_ID")).longValue();
            AttachmentHolder holder = attachmentHoldersById.get(holderId);
            String holderName = attachmentHolderKind.name().toLowerCase();
            String holderIdentifier = holder.permId;
            String fileName = (String) row.get("FILE_NAME");
            int version = ((Number) row.get("VERSION")).intValue();
            String identifier = String.format("%s/%s/%s(%s)", holderName, holderIdentifier, fileName, version);
            insertQuery.setParameter("description", identifier);
            insertQuery.setParameter("registerer", registrator.getId());
            insertQuery.setParameter("identifiers", identifier);
            insertQuery.setParameter("attachment", ((Number) row.get("EXAC_ID")).longValue());
            Map<String, List<? extends EntityModification>> modifications = new HashMap<String, List<? extends EntityModification>>();
            AttachmentEntry attachmentEntry = new AttachmentEntry();
            attachmentEntry.fileName = fileName;
            attachmentEntry.version = version;
            attachmentEntry.description = (String) row.get("DESCRIPTION");
            attachmentEntry.title = (String) row.get("TITLE");
            attachmentEntry.relationType = "OWNED";
            attachmentEntry.entityType = attachmentHolderKind.toString();
            attachmentEntry.relatedEntity = holder.permId;
            attachmentEntry.validFrom = toDate(row.get("REGISTRATION_TIMESTAMP"));
            attachmentEntry.userId = (String) row.get("USER_ID");
            modifications.put(identifier, Arrays.asList(attachmentEntry));
            attachmentEntries.put(identifier, attachmentEntry);
            insertQuery.setParameter("content", jsonize(modifications));
            insertQuery.executeUpdate();
        }
        List<RelationshipHistoryEntry> result = new ArrayList<>();
        Set<Entry<String, AttachmentEntry>> entrySet = attachmentEntries.entrySet();
        for (Entry<String, AttachmentEntry> entry : entrySet)
        {
            RelationshipHistoryEntry relationshipHistoryEntry = new RelationshipHistoryEntry();
            AttachmentEntry attachmentEntry = entry.getValue();
            relationshipHistoryEntry.userId = attachmentEntry.userId;
            relationshipHistoryEntry.entityType = "ATTACHMENT";
            relationshipHistoryEntry.permId = attachmentEntry.relatedEntity;
            relationshipHistoryEntry.relatedEntity = entry.getKey();
            relationshipHistoryEntry.relationType = "OWNER";
            relationshipHistoryEntry.validFrom = entry.getValue().validFrom;
            result.add(relationshipHistoryEntry);
        }
        return result;
    }

    private String createSelectHolderStatement(AttachmentHolderKind holderKind)
    {
        String tableName = "";
        switch (holderKind)
        {
            case PROJECT:
                tableName = "projects";
                break;
            case EXPERIMENT:
                tableName = "experiments_all";
                break;
            case SAMPLE:
                tableName = "samples_all";
                break;
        }
        return "SELECT id, perm_id from " + tableName + " WHERE id in (:" + ENTITY_IDS + ")";
    }

    private String createSelectAttachmentsStatement(AttachmentHolderKind attachmentHolderKind)
    {
        String holderColumn = "";
        switch (attachmentHolderKind)
        {
            case PROJECT:
                holderColumn = "proj_id";
                break;
            case EXPERIMENT:
                holderColumn = "expe_id";
                break;
            case SAMPLE:
                holderColumn = "samp_id";
                break;
        }
        return "SELECT " + holderColumn + " as holder_id, file_name, "
                + "version, title, description, a.registration_timestamp, r.user_id, exac_id "
                + "FROM attachments a join persons r on a.pers_id_registerer = r.id "
                + "WHERE " + holderColumn + " in (:" + ENTITY_IDS + ")";
    }

    private List<RelationshipHistoryEntry> deleteAttachments(SharedSessionContract session,
            PersonPE registrator, List<? extends AttachmentHolderPE> attachmentHolders)
    {
        List<RelationshipHistoryEntry> relationshipHistoryEntries = new ArrayList<>();
        IAttachmentDAO attachmentDAO = daoFactory.getAttachmentDAO();
        for (AttachmentHolderPE holder : attachmentHolders)
        {
            List<AttachmentPE> attachments = attachmentDAO.listAttachments(holder);
            holder.setInternalAttachments(new HashSet<>(attachments));
            List<String> fileNames = new ArrayList<>();
            for (AttachmentPE attachment : attachments)
            {
                fileNames.add(attachment.getFileName());
            }
            Map<String, AttachmentEntry> deletedAttachments = attachmentDAO.deleteAttachments(holder, "", fileNames, registrator);
            Set<Entry<String, AttachmentEntry>> entrySet = deletedAttachments.entrySet();
            for (Entry<String, AttachmentEntry> entry : entrySet)
            {
                RelationshipHistoryEntry relationshipHistoryEntry = new RelationshipHistoryEntry();
                relationshipHistoryEntry.userId = entry.getValue().userId;
                relationshipHistoryEntry.entityType = "ATTACHMENT";
                relationshipHistoryEntry.permId = holder.getPermId();
                relationshipHistoryEntry.relatedEntity = entry.getKey();
                relationshipHistoryEntry.relationType = "OWNER";
                relationshipHistoryEntry.validFrom = entry.getValue().validFrom;
                relationshipHistoryEntries.add(relationshipHistoryEntry);
            }
        }
        return relationshipHistoryEntries;
    }

    private <T> List<AttributeEntry> selectAttributeEntries(NativeQuery<T> sqlQuery, List<Long> entityIdsToDelete)
    {
        List<Map<String, Object>> rows = getRows(sqlQuery, entityIdsToDelete);

        Map<Long, EntityAttributes> result = new HashMap<>();
        for (Map<String, Object> row : rows)
        {
            Number idValue = (Number) row.get("ID");
            Long id = idValue != null ? idValue.longValue() : null;
            String permId = (String) row.get("PERM_ID");
            EntityAttributes attributes = new EntityAttributes(permId);
            for (Entry<String, Object> entry : row.entrySet())
            {
                String key = entry.getKey();
                if (NON_ATTRIBUTE_COLUMNS.contains(key))
                {
                    continue;
                }
                attributes.addAttribute(key, render(entry.getValue()));

            }
            result.put(id, attributes);
        }

        List<AttributeEntry> list = new ArrayList<>();
        for (EntityAttributes entityAttributes : result.values())
        {
            Map<String, String> map = entityAttributes.getAttributes();
            for (Entry<String, String> entry : map.entrySet())
            {
                AttributeEntry historyEntry = new AttributeEntry();
                historyEntry.permId = entityAttributes.getPermId();
                historyEntry.attributeName = entry.getKey();
                historyEntry.value = entry.getValue();
                list.add(historyEntry);
            }
        }
        return list;
    }

    private String render(Object value)
    {
        if (value == null)
        {
            return null;
        }

        Date dateValue = tryToDate(value);
        if (dateValue != null)
        {
            return DateFormatThreadLocal.DATE_FORMAT.get().format(dateValue);
        }

        return value.toString();
    }

    private void addToHistories(Map<String, List<? extends EntityModification>> histories,
            List<? extends EntityModification> modifications)
    {
        for (EntityModification entry : modifications)
        {
            String permId = entry.permId;
            List<? extends EntityModification> current = histories.get(permId);
            if (current == null)
            {
                current = new ArrayList<>();
            }
            histories.put(permId, addModification(current, entry));
        }
    }

    private List<EntityModification> addModification(List<? extends EntityModification> current, EntityModification modification)
    {
        List<EntityModification> list = new ArrayList<>(current);
        list.add(modification);
        Collections.sort(list, new SimpleComparator<EntityModification, Date>()
            {
                @Override
                public Date evaluate(EntityModification item)
                {
                    return item.validFrom == null ? new Date(0) : item.validFrom;
                }
            });
        return Collections.unmodifiableList(list);
    }

    private <T> List<PropertyHistoryEntry> selectHistoryPropertyEntries(
            final NativeQuery<T> selectPropertyHistory, final List<Long> entityIds)
    {
        selectPropertyHistory.setParameterList(ENTITY_IDS, entityIds);
        selectPropertyHistory.setResultTransformer(new ResultTransformer()
            {
                private static final long serialVersionUID = 1L;

                @Override
                public Object transformTuple(Object[] values, String[] aliases)
                {
                    PropertyHistoryEntry entry = new PropertyHistoryEntry();
                    int i = 0;
                    entry.permId = (String) values[i++];
                    entry.propertyCode = (String) values[i++];
                    entry.value = (String) values[i++];
                    entry.userId = (String) values[i++];
                    entry.validFrom = toDate(values[i++]);
                    entry.validUntil = toDate( values[i++]);
                    return entry;
                }

                @SuppressWarnings("rawtypes")
                @Override
                public List transformList(List list)
                {
                    return list;
                }
            });
        return cast(selectPropertyHistory.list());
    }

    private <T> List<RelationshipHistoryEntry> selectRelationshipHistoryEntries(final NativeQuery<T> selectRelationshipHistory,
            final List<Long> entityIds)
    {
        selectRelationshipHistory.setParameterList(ENTITY_IDS, entityIds);
        selectRelationshipHistory.setResultTransformer(new ResultTransformer()
            {
                private static final long serialVersionUID = 1L;

                @Override
                public Object transformTuple(Object[] values, String[] aliases)
                {
                    RelationshipHistoryEntry entry = new RelationshipHistoryEntry();
                    int i = 0;
                    entry.permId = (String) values[i++];
                    entry.relationType = (String) values[i++];
                    entry.relatedEntity = (String) values[i++];
                    entry.entityType = (String) values[i++];
                    entry.userId = (String) values[i++];
                    entry.validFrom = toDate(values[i++]);
                    entry.validUntil = toDate(values[i++]);
                    return entry;
                }

                @SuppressWarnings("rawtypes")
                @Override
                public List transformList(List list)
                {
                    return list;
                }
            });
        return cast(selectRelationshipHistory.list());
    }

    private static Date toDate(Object o) {
        if (o == null) return null;
        if (o instanceof java.util.Date d) return d;
        if (o instanceof java.sql.Timestamp ts) return new java.util.Date(ts.getTime());
        if (o instanceof Instant i) return java.util.Date.from(i);
        if (o instanceof OffsetDateTime odt) return java.util.Date.from(odt.toInstant());
        if (o instanceof ZonedDateTime zdt) return java.util.Date.from(zdt.toInstant());
        if (o instanceof LocalDateTime ldt) return java.util.Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
        if (o instanceof LocalDate ld) return java.util.Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
        throw new IllegalArgumentException("Unsupported time type: " + o.getClass());
    }

    private static Date tryToDate(Object value)
    {
        try
        {
            return toDate(value);
        } catch (IllegalArgumentException ex)
        {
            return null;
        }
    }

    private <T> List<Map<String, Object>> getRows(NativeQuery<T> sqlQuery, List<Long> entityIdsToDelete)
    {
        sqlQuery.setParameterList(ENTITY_IDS, entityIdsToDelete);
        sqlQuery.setResultTransformer(new ResultTransformer()
            {
                private static final long serialVersionUID = 1L;

                @Override
                public Object transformTuple(Object[] values, String[] aliases)
                {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 0; i < values.length; i++)
                    {
                        row.put(aliases[i].toUpperCase(), values[i]);
                    }
                    return row;
                }

                @SuppressWarnings("rawtypes")
                @Override
                public List transformList(List list)
                {
                    return list;
                }
            });
        return cast(sqlQuery.list());
    }

    @SuppressWarnings("unchecked")
    protected final <T> List<T> cast(final List<?> list)
    {
        return (List<T>) list;
    }

    private static class EntityAttributes
    {
        private final String permId;

        private final Map<String, String> attributes = new HashMap<String, String>();

        public EntityAttributes(String permId)
        {
            this.permId = permId;
        }

        public void addAttribute(String attributeName, String value)
        {
            attributes.put(attributeName, value);
        }

        public String getPermId()
        {
            return permId;
        }

        public Map<String, String> getAttributes()
        {
            return attributes;
        }

    }

    private static class AttachmentHolder
    {
        long id;

        String permId;
    }

}
