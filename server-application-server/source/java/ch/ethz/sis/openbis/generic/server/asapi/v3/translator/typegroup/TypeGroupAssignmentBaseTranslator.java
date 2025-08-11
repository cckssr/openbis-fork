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

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.fetchoptions.EmptyFetchOptions;
import ch.ethz.sis.openbis.generic.server.asapi.v3.translator.AbstractCachingTranslator;
import ch.ethz.sis.openbis.generic.server.asapi.v3.translator.TranslationContext;
import ch.ethz.sis.openbis.generic.server.asapi.v3.translator.common.ObjectHolder;
import ch.systemsx.cisd.openbis.generic.shared.dto.SampleTypeTypeGroupsTechId;
import net.lemnik.eodsql.QueryTool;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TypeGroupAssignmentBaseTranslator
        extends AbstractCachingTranslator<SampleTypeTypeGroupsTechId, ObjectHolder<TypeGroupAssignmentBaseRecord>, EmptyFetchOptions>
        implements ITypeGroupAssignmentBaseTranslator
{
    @Override
    protected ObjectHolder<TypeGroupAssignmentBaseRecord> createObject(TranslationContext context,
            SampleTypeTypeGroupsTechId input, EmptyFetchOptions fetchOptions)
    {
        return new ObjectHolder<TypeGroupAssignmentBaseRecord>();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void updateObject(TranslationContext context, SampleTypeTypeGroupsTechId input,
            ObjectHolder<TypeGroupAssignmentBaseRecord> output, Object relations,
            EmptyFetchOptions fetchOptions)
    {
        Map<SampleTypeTypeGroupsTechId, TypeGroupAssignmentBaseRecord> recordMap = (Map<SampleTypeTypeGroupsTechId, TypeGroupAssignmentBaseRecord>) relations;
        TypeGroupAssignmentBaseRecord record = recordMap.get(input);
        output.setObject(record);
    }


    @Override
    protected Object getObjectsRelations(TranslationContext context, Collection<SampleTypeTypeGroupsTechId> ids, EmptyFetchOptions fetchOptions)
    {
        Map<SampleTypeTypeGroupsTechId, TypeGroupAssignmentBaseRecord> recordMap = new HashMap<>();
        for(SampleTypeTypeGroupsTechId id : ids) {
            List<TypeGroupAssignmentBaseRecord> records = loadRecords(id);
            for (TypeGroupAssignmentBaseRecord record : records)
            {
                recordMap.put(id, record);
            }
        }

        return recordMap;
    }

    protected List<TypeGroupAssignmentBaseRecord> loadRecords(SampleTypeTypeGroupsTechId objectId)
    {
        TypeGroupQuery query = QueryTool.getManagedQuery(TypeGroupQuery.class);
        return query.getTypeGroupAssignment(objectId.getTypeGroupTechId(), objectId.getSampleTypeTechId());
    }

}
