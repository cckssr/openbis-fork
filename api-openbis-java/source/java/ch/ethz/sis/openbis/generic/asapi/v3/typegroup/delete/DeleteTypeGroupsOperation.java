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

package ch.ethz.sis.openbis.generic.asapi.v3.typegroup.delete;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.delete.DeleteObjectsOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.typegroup.id.ITypeGroupId;
import ch.systemsx.cisd.base.annotation.JsonObject;

import java.util.List;

@JsonObject("as.dto.typegroup.delete.DeleteTypeGroupsOperation")
public class DeleteTypeGroupsOperation extends DeleteObjectsOperation<ITypeGroupId, TypeGroupDeletionOptions>
{
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    public DeleteTypeGroupsOperation() {
    }

    public DeleteTypeGroupsOperation(
            List<? extends ITypeGroupId> typeGroupsIds, TypeGroupDeletionOptions options) {
        super(typeGroupsIds, options);
    }

}
