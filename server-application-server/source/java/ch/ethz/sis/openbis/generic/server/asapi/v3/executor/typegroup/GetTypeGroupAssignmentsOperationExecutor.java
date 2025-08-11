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

package ch.ethz.sis.openbis.generic.server.asapi.v3.executor.typegroup;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.get.GetObjectsOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.get.GetObjectsOperationResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.TypeGroupAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.fetchoptions.TypeGroupAssignmentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.get.GetTypeGroupAssignmentsOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.get.GetTypeGroupAssignmentsOperationResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.ITypeGroupAssignmentId;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.IOperationContext;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.common.get.AbstractGetObjectsOperationExecutor;
import ch.ethz.sis.openbis.generic.server.asapi.v3.translator.ITranslator;
import ch.ethz.sis.openbis.generic.server.asapi.v3.translator.TranslationContext;
import ch.ethz.sis.openbis.generic.server.asapi.v3.translator.typegroup.ITypeGroupAssignmentTranslator;
//import ch.systemsx.cisd.openbis.generic.shared.dto.SampleTypeTypeGroupsId;
import ch.systemsx.cisd.openbis.generic.shared.dto.SampleTypeTypeGroupsPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.SampleTypeTypeGroupsTechId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GetTypeGroupAssignmentsOperationExecutor
        extends AbstractGetObjectsOperationExecutor<ITypeGroupAssignmentId, SampleTypeTypeGroupsTechId, TypeGroupAssignment, TypeGroupAssignmentFetchOptions>
        implements IGetTypeGroupAssignmentsOperationExecutor
{
    @Autowired
    private IMapTypeGroupByIdExecutor mapExecutor;

    @Autowired
    private IMapSampleTypeTypeGroupsByIdExecutor mapTypeGroupAssignmentsByIdExecutor;

    @Autowired
    private ITypeGroupAssignmentTranslator translator;

    @Override
    protected Class<? extends GetObjectsOperation<ITypeGroupAssignmentId, TypeGroupAssignmentFetchOptions>> getOperationClass()
    {
        return GetTypeGroupAssignmentsOperation.class;
    }


    @Override
    protected Map<ITypeGroupAssignmentId, SampleTypeTypeGroupsTechId> map(IOperationContext context,
            List<? extends ITypeGroupAssignmentId> ids,
            TypeGroupAssignmentFetchOptions fetchOptions)
    {
        Map<ITypeGroupAssignmentId, SampleTypeTypeGroupsPE> map = mapTypeGroupAssignmentsByIdExecutor.map(context, ids);
        Map<ITypeGroupAssignmentId, SampleTypeTypeGroupsTechId> result = new HashMap<>();
        for(Map.Entry<ITypeGroupAssignmentId, SampleTypeTypeGroupsPE> entry : map.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getId());
        }
        return result;
    }

    @Override
    protected Map<SampleTypeTypeGroupsTechId, TypeGroupAssignment> translate(TranslationContext context,
            Collection<SampleTypeTypeGroupsTechId> objects,
            TypeGroupAssignmentFetchOptions fetchOptions)
    {
        return getTranslator().translate(context, objects, fetchOptions);
    }

    protected ITranslator<SampleTypeTypeGroupsTechId, TypeGroupAssignment, TypeGroupAssignmentFetchOptions> getTranslator()
    {
        return translator;
    }

    @Override
    protected GetObjectsOperationResult<ITypeGroupAssignmentId, TypeGroupAssignment> getOperationResult(
            Map<ITypeGroupAssignmentId, TypeGroupAssignment> objectMap)
    {
        return new GetTypeGroupAssignmentsOperationResult(objectMap);
    }
}
