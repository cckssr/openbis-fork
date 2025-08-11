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

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.create.CreateObjectsOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.create.CreateObjectsOperationResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.create.CreateTypeGroupAssignmentsOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.create.CreateTypeGroupAssignmentsOperationResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.create.TypeGroupAssignmentCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.TypeGroupAssignmentId;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.IOperationContext;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.common.create.CreateObjectsOperationExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CreateTypeGroupAssignmentsOperationExecutor extends CreateObjectsOperationExecutor<TypeGroupAssignmentCreation, TypeGroupAssignmentId>
        implements ICreateTypeGroupAssignmentsOperationExecutor
{
    @Autowired
    private ICreateTypeGroupAssignmentExecutor executor;


    @Override
    protected Class<? extends CreateObjectsOperation<TypeGroupAssignmentCreation>> getOperationClass()
    {
        return CreateTypeGroupAssignmentsOperation.class;
    }

    @Override
    protected CreateObjectsOperationResult<TypeGroupAssignmentId> doExecute(IOperationContext context,
            CreateObjectsOperation<TypeGroupAssignmentCreation> operation)
    {
        return new CreateTypeGroupAssignmentsOperationResult(executor.create(context, operation.getCreations()));
    }
}
