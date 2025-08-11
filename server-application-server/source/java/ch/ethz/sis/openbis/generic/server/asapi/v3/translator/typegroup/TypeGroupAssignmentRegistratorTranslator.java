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

import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.Person;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.fetchoptions.PersonFetchOptions;
import ch.ethz.sis.openbis.generic.server.asapi.v3.translator.AbstractCachingTranslator;
import ch.ethz.sis.openbis.generic.server.asapi.v3.translator.TranslationContext;
import ch.ethz.sis.openbis.generic.server.asapi.v3.translator.common.ObjectHolder;
import ch.ethz.sis.openbis.generic.server.asapi.v3.translator.common.ObjectRelationRecord;
import ch.ethz.sis.openbis.generic.server.asapi.v3.translator.person.IPersonTranslator;
import ch.systemsx.cisd.openbis.generic.shared.dto.SampleTypeTypeGroupsTechId;
import net.lemnik.eodsql.QueryTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TypeGroupAssignmentRegistratorTranslator
        extends AbstractCachingTranslator<SampleTypeTypeGroupsTechId, ObjectHolder<Person>, PersonFetchOptions>
        implements ITypeGroupAssignmentRegistratorTranslator
{
    @Autowired
    private IPersonTranslator personTranslator;

    protected List<ObjectRelationRecord> loadRecords(SampleTypeTypeGroupsTechId id)
    {
        TypeGroupQuery query = QueryTool.getManagedQuery(TypeGroupQuery.class);
        return query.getAssignmentRegistratorId(id.getTypeGroupTechId(), id.getSampleTypeTechId());
    }

    @Override
    protected Object getObjectsRelations(TranslationContext context, Collection<SampleTypeTypeGroupsTechId> objectIds, PersonFetchOptions relatedFetchOptions)
    {
        Map<SampleTypeTypeGroupsTechId, Long> recordMap = new HashMap<>();
        Collection<Long> relatedIds = new HashSet<>();
        for(SampleTypeTypeGroupsTechId id : objectIds) {
            List<ObjectRelationRecord> records = loadRecords(id);
            for (ObjectRelationRecord record : records)
            {
                if (record.relatedId != null)
                {
                    relatedIds.add(record.relatedId);
                }
                recordMap.put(id, record.relatedId);
            }
        }


        Map<Long, Person> relatedIdToRelated = translateRelated(context, relatedIds, relatedFetchOptions);

        Map<SampleTypeTypeGroupsTechId, Person> objectIdToRelatedMap = new HashMap<>();
        for (SampleTypeTypeGroupsTechId record : recordMap.keySet())
        {
            objectIdToRelatedMap.put(record, relatedIdToRelated.get(recordMap.get(record)));
        }

        return objectIdToRelatedMap;
    }

    protected Map<Long, Person> translateRelated(TranslationContext context, Collection<Long> relatedIds, PersonFetchOptions relatedFetchOptions)
    {
        return personTranslator.translate(context, relatedIds, relatedFetchOptions);
    }

    @Override
    protected ObjectHolder<Person> createObject(TranslationContext context,
            SampleTypeTypeGroupsTechId input, PersonFetchOptions fetchOptions)
    {
        return new ObjectHolder<Person>();
    }

    @Override
    protected void updateObject(TranslationContext context, SampleTypeTypeGroupsTechId input,
            ObjectHolder<Person> output, Object relations, PersonFetchOptions fetchOptions)
    {
        Map<SampleTypeTypeGroupsTechId, Person> recordMap = (Map<SampleTypeTypeGroupsTechId, Person>) relations;
        Person record = recordMap.get(input);
        output.setObject(record);
    }
}
