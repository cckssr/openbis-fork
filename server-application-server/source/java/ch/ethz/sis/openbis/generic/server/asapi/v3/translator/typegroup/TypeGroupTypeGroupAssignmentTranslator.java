/*
 *  Copyright ETH 2025 Zürich, Scientific IT Services
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package ch.ethz.sis.openbis.generic.server.asapi.v3.translator.typegroup;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.TypeGroupAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.fetchoptions.TypeGroupAssignmentFetchOptions;
import ch.ethz.sis.openbis.generic.server.asapi.v3.translator.TranslationContext;
import ch.ethz.sis.openbis.generic.server.asapi.v3.translator.common.ObjectRelationRecord;
import ch.ethz.sis.openbis.generic.server.asapi.v3.translator.common.ObjectToManyRelationTranslator;
import ch.systemsx.cisd.openbis.generic.shared.dto.SampleTypeTypeGroupsTechId;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.lemnik.eodsql.QueryTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TypeGroupTypeGroupAssignmentTranslator extends ObjectToManyRelationTranslator<TypeGroupAssignment, TypeGroupAssignmentFetchOptions>
        implements ITypeGroupTypeGroupAssignmentTranslator
{
    @Autowired
    private ITypeGroupAssignmentTranslator typeGroupAssignmentTranslator;


    @Override
    protected List<ObjectRelationRecord> loadRecords(LongOpenHashSet typeGroupIds)
    {
        TypeGroupQuery query = QueryTool.getManagedQuery(TypeGroupQuery.class);
        return query.getTypeGroupAssignmentIds(typeGroupIds);
    }

    @Override
    protected Object getObjectsRelations(TranslationContext context, Collection<Long> objectIds, TypeGroupAssignmentFetchOptions relatedFetchOptions)
    {
        List<ObjectRelationRecord> records = loadRecords(new LongOpenHashSet(objectIds));

        Collection<Long> relatedIds = new HashSet<Long>();
        List<SampleTypeTypeGroupsTechId> assignmentIds = new ArrayList<>();

        for (ObjectRelationRecord record : records)
        {
            if (record.relatedId != null)
            {
                relatedIds.add(record.relatedId);
                SampleTypeTypeGroupsTechId id = new SampleTypeTypeGroupsTechId(record.relatedId, record.objectId);
                assignmentIds.add(id);
            }
        }

        Map<SampleTypeTypeGroupsTechId, TypeGroupAssignment> relatedIdToRelated = translateRelatedNew(context, assignmentIds, relatedFetchOptions);
        Map<Long, Collection<TypeGroupAssignment>> objectIdToRelatedMap = new HashMap<Long, Collection<TypeGroupAssignment>>();

        for(SampleTypeTypeGroupsTechId id : assignmentIds)
        {
            Long typeGroupId = id.getTypeGroupTechId();
            Collection<TypeGroupAssignment> relatedCollection = objectIdToRelatedMap.get(typeGroupId);

            if (relatedCollection == null)
            {
                relatedCollection = createCollection();
                objectIdToRelatedMap.put(typeGroupId, relatedCollection);
            }

            TypeGroupAssignment relatedObject = relatedIdToRelated.get(id);
            if (relatedObject != null)
            {
                relatedCollection.add(relatedObject);
            }

        }

        for (Long objectId : objectIds)
        {
            if (false == objectIdToRelatedMap.containsKey(objectId))
            {
                objectIdToRelatedMap.put(objectId, createCollection());
            }
        }

        return objectIdToRelatedMap;
    }

    @Override
    protected Map<Long, TypeGroupAssignment> translateRelated(TranslationContext context,
            Collection<Long> typeGroupIds, TypeGroupAssignmentFetchOptions annotationFetchOptions)
    {
        throw new RuntimeException("Not implemented!");
    }

    private Map<SampleTypeTypeGroupsTechId, TypeGroupAssignment> translateRelatedNew(TranslationContext context,
            Collection<SampleTypeTypeGroupsTechId> typeGroupIds, TypeGroupAssignmentFetchOptions annotationFetchOptions)
    {
        return  typeGroupAssignmentTranslator.translate(context, typeGroupIds, annotationFetchOptions);
    }

    @Override
    protected Collection<TypeGroupAssignment> createCollection()
    {
        return new ArrayList<>();
    }
}
