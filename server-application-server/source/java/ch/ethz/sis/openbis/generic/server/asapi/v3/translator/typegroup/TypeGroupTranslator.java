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

import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.TypeGroup;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.TypeGroupAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.fetchoptions.TypeGroupFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.TypeGroupId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.TypeGroupTechId;
import ch.ethz.sis.openbis.generic.server.asapi.v3.helper.common.CommonUtils;
import ch.ethz.sis.openbis.generic.server.asapi.v3.translator.AbstractCachingTranslator;
import ch.ethz.sis.openbis.generic.server.asapi.v3.translator.TranslationContext;
import ch.ethz.sis.openbis.generic.server.asapi.v3.translator.TranslationResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
public class TypeGroupTranslator extends AbstractCachingTranslator<Long, TypeGroup, TypeGroupFetchOptions>
        implements ITypeGroupTranslator
{

    @Autowired
    private ITypeGroupBaseTranslator baseTranslator;

    @Autowired
    private ITypeGroupRegistratorTranslator registratorTranslator;

    @Autowired
    private ITypeGroupModifierTranslator modifierTranslator;

    @Autowired
    private ITypeGroupTypeGroupAssignmentTranslator typeGroupAssignmentTranslator;

    @Override
    protected TypeGroup createObject(TranslationContext context, Long input,
            TypeGroupFetchOptions fetchOptions)
    {
        TypeGroup typeGroup = new TypeGroup();
        typeGroup.setFetchOptions(fetchOptions);
        return typeGroup;
    }

    @Override
    protected Object getObjectsRelations(TranslationContext context, Collection<Long> typeGroupIds, TypeGroupFetchOptions fetchOptions)
    {
        TranslationResults relations = new TranslationResults();

        relations.put(ITypeGroupBaseTranslator.class, baseTranslator.translate(context, typeGroupIds, null));


        if (fetchOptions.hasRegistrator())
        {
            relations.put(ITypeGroupRegistratorTranslator.class, registratorTranslator.translate(context, typeGroupIds, fetchOptions.withRegistrator()));
        }

        if (fetchOptions.hasModifier())
        {
            relations.put(ITypeGroupModifierTranslator.class, modifierTranslator.translate(context, typeGroupIds, fetchOptions.withModifier()));
        }

        if(fetchOptions.hasTypeGroupAssignments())
        {
            relations.put(ITypeGroupTypeGroupAssignmentTranslator.class,
                    typeGroupAssignmentTranslator.translate(context, typeGroupIds, fetchOptions.withTypeGroupAssignments()));
        }

        return relations;
    }

    @Override
    protected void updateObject(TranslationContext context, Long typeGroupId, TypeGroup result,
            Object objectRelations, TypeGroupFetchOptions fetchOptions)
    {
        TranslationResults relations = (TranslationResults) objectRelations;
        TypeGroupBaseRecord baseRecord = relations.get(ITypeGroupBaseTranslator.class, typeGroupId);

        result.setId(new TypeGroupId(baseRecord.name));
        result.setName(baseRecord.name);
        result.setManagedInternally(baseRecord.managedInternally);
        result.setMetaData(CommonUtils.asMap(baseRecord.metaData));
        result.setModificationDate(baseRecord.modificationDate);
        result.setRegistrationDate(baseRecord.registrationDate);

        if (fetchOptions.hasRegistrator())
        {
            result.setRegistrator(relations.get(ITypeGroupRegistratorTranslator.class, typeGroupId));
            result.getFetchOptions().withRegistratorUsing(fetchOptions.withRegistrator());
        }

        if (fetchOptions.hasModifier())
        {
            result.setModifier(relations.get(ITypeGroupModifierTranslator.class, typeGroupId));
            result.getFetchOptions().withModifierUsing(fetchOptions.withModifier());
        }

        if(fetchOptions.hasTypeGroupAssignments())
        {
            result.setTypeGroupAssignments((List<TypeGroupAssignment>) relations.get(
                    ITypeGroupTypeGroupAssignmentTranslator.class, typeGroupId));
            result.getFetchOptions().withTypeGroupAssignmentsUsing(fetchOptions.withTypeGroupAssignments());
        }
    }
}
