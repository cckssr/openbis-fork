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

package ch.ethz.sis.openbis.generic.server.asapi.v3.helper.typegroup;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.IEntityTypeId;
import ch.ethz.sis.openbis.generic.asapi.v3.typegroup.id.ITypeGroupId;
import ch.ethz.sis.openbis.generic.asapi.v3.typegroup.id.TypeGroupAssignmentId;
import ch.ethz.sis.openbis.generic.asapi.v3.typegroup.id.TypeGroupId;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.IOperationContext;
import ch.ethz.sis.openbis.generic.server.asapi.v3.helper.common.AbstractListObjectById;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.ITypeGroupAssignmentDAO;
import ch.systemsx.cisd.openbis.generic.shared.dto.SampleTypeTypeGroupsPE;

import java.util.List;

public class ListTypeGroupAssignmentById extends AbstractListObjectById<TypeGroupAssignmentId, SampleTypeTypeGroupsPE>
{
    private ITypeGroupAssignmentDAO typeGroupAssignmentDAO;

    public ListTypeGroupAssignmentById(ITypeGroupAssignmentDAO typeGroupAssignmentDAO)
    {
        this.typeGroupAssignmentDAO = typeGroupAssignmentDAO;
    }

    @Override
    public Class<TypeGroupAssignmentId> getIdClass()
    {
        return TypeGroupAssignmentId.class;
    }

    @Override
    public TypeGroupAssignmentId createId(SampleTypeTypeGroupsPE typeGroupAssignment)
    {
        IEntityTypeId sampleTypeId = new EntityTypePermId(typeGroupAssignment.getSampleType().getPermId(), EntityKind.SAMPLE);
        ITypeGroupId typeGroupId = new TypeGroupId(typeGroupAssignment.getTypeGroup().getName());
        return new TypeGroupAssignmentId(sampleTypeId,  typeGroupId);
    }

    @Override
    public List<SampleTypeTypeGroupsPE> listByIds(IOperationContext context, List<TypeGroupAssignmentId> ids)
    {
        return typeGroupAssignmentDAO.findByIds(ids);
    }
}
