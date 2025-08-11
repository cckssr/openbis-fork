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

import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.TypeGroupTechId;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.IOperationContext;
import ch.ethz.sis.openbis.generic.server.asapi.v3.helper.common.AbstractListObjectById;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.ITypeGroupDAO;
import ch.systemsx.cisd.openbis.generic.shared.dto.TypeGroupPE;

import java.util.LinkedList;
import java.util.List;

public class ListTypeGroupByTechId extends AbstractListObjectById<TypeGroupTechId, TypeGroupPE>
{
    private ITypeGroupDAO typeGroupDAO;

    public ListTypeGroupByTechId(ITypeGroupDAO typeGroupDAO)
    {
        this.typeGroupDAO = typeGroupDAO;
    }

    @Override
    public Class<TypeGroupTechId> getIdClass()
    {
        return TypeGroupTechId.class;
    }

    @Override
    public TypeGroupTechId createId(TypeGroupPE typeGroup)
    {
        return new TypeGroupTechId(typeGroup.getId());
    }

    @Override
    public List<TypeGroupPE> listByIds(IOperationContext context, List<TypeGroupTechId> ids)
    {
        List<Long> names = new LinkedList<>();

        for (TypeGroupTechId id : ids)
        {
            names.add(id.getTechId());
        }

        return typeGroupDAO.findByIds(names);
    }
}
