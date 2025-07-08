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

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.delete.DeleteObjectsOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.delete.DeleteObjectsOperationResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.delete.DeleteSpacesOperationResult;
import ch.ethz.sis.openbis.generic.asapi.v3.typegroup.delete.DeleteTypeGroupAssignmentsOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.typegroup.delete.TypeGroupAssignmentDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.typegroup.id.ITypeGroupAssignmentId;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.IOperationContext;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.common.delete.DeleteObjectsOperationExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DeleteTypeGroupAssignmentsOperationExecutor extends DeleteObjectsOperationExecutor<ITypeGroupAssignmentId, TypeGroupAssignmentDeletionOptions>
        implements IDeleteTypeGroupAssignmentsOperationExecutor
{
    @Autowired
    private IDeleteTypeGroupAssignmentExecutor executor;

    @Override
    protected Class<? extends DeleteObjectsOperation<ITypeGroupAssignmentId, TypeGroupAssignmentDeletionOptions>> getOperationClass()
    {
        return DeleteTypeGroupAssignmentsOperation.class;
    }

    @Override
    protected DeleteObjectsOperationResult doExecute(IOperationContext context, DeleteObjectsOperation<ITypeGroupAssignmentId, TypeGroupAssignmentDeletionOptions> operation)
    {
        executor.delete(context, operation.getObjectIds(), operation.getOptions());
        return new DeleteSpacesOperationResult();
    }
}
