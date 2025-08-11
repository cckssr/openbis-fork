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

import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.TypeGroup;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.TypeGroupAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.fetchoptions.TypeGroupAssignmentFetchOptions;
import ch.ethz.sis.openbis.generic.server.asapi.v3.translator.AbstractCachingTranslator;
import ch.ethz.sis.openbis.generic.server.asapi.v3.translator.TranslationContext;
import ch.ethz.sis.openbis.generic.server.asapi.v3.translator.TranslationResults;
import ch.ethz.sis.openbis.generic.server.asapi.v3.translator.sample.ISampleTypeTranslator;
import ch.systemsx.cisd.openbis.generic.shared.dto.SampleTypeTypeGroupsTechId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class TypeGroupAssignmentTranslator
        extends AbstractCachingTranslator<SampleTypeTypeGroupsTechId, TypeGroupAssignment, TypeGroupAssignmentFetchOptions>
        implements ITypeGroupAssignmentTranslator
{

    @Autowired
    private ITypeGroupAssignmentBaseTranslator baseTranslator;

    @Autowired
    private ITypeGroupAssignmentRegistratorTranslator registratorTranslator;

    @Autowired
    private ISampleTypeTranslator sampleTypeTranslator;

    @Autowired
    private  ITypeGroupTranslator typeGroupTranslator;


    @Override
    protected Object getObjectsRelations(TranslationContext context, Collection<SampleTypeTypeGroupsTechId> typeGroupAssignmentIds, TypeGroupAssignmentFetchOptions fetchOptions)
    {
        TranslationResults relations = new TranslationResults();

        relations.put(ITypeGroupAssignmentBaseTranslator.class, baseTranslator.translate(context, typeGroupAssignmentIds, null));

        if (fetchOptions.hasRegistrator())
        {
            relations.put(ITypeGroupAssignmentRegistratorTranslator.class, registratorTranslator.translate(context, typeGroupAssignmentIds, fetchOptions.withRegistrator()));
        }

        return relations;
    }

    @Override
    protected TypeGroupAssignment createObject(TranslationContext context,
            SampleTypeTypeGroupsTechId input, TypeGroupAssignmentFetchOptions fetchOptions)
    {
        TypeGroupAssignment typeGroupAssignment = new TypeGroupAssignment();
        typeGroupAssignment.setFetchOptions(fetchOptions);
        return typeGroupAssignment;
    }

    @Override
    protected void updateObject(TranslationContext context, SampleTypeTypeGroupsTechId input,
            TypeGroupAssignment result, Object objectRelations,
            TypeGroupAssignmentFetchOptions fetchOptions)
    {
        TranslationResults relations = (TranslationResults) objectRelations;
        TypeGroupAssignmentBaseRecord baseRecord = relations.get(ITypeGroupAssignmentBaseTranslator.class, input);

        result.setManagedInternally(baseRecord.managedInternally);
        result.setRegistrationDate(baseRecord.registrationDate);

        if(fetchOptions.hasTypeGroup()) {
            TypeGroup typeGroup = typeGroupTranslator.translate(context, input.getTypeGroupTechId(), fetchOptions.withTypeGroup());
            result.setTypeGroup(typeGroup);
            result.getFetchOptions().withTypeGroupUsing(fetchOptions.withTypeGroup());
        }

        if(fetchOptions.hasSampleType()) {
            SampleType sampleType = sampleTypeTranslator.translate(context, input.getSampleTypeTechId(),
                    fetchOptions.withSampleType());
            result.setSampleType(sampleType);
            result.getFetchOptions().withSampleTypeUsing(fetchOptions.withSampleType());
        }

        if (fetchOptions.hasRegistrator())
        {
            result.setRegistrator(relations.get(ITypeGroupAssignmentRegistratorTranslator.class, input));
            result.getFetchOptions().withRegistratorUsing(fetchOptions.withRegistrator());
        }

    }

}
