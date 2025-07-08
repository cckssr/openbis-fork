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

import ch.ethz.sis.openbis.generic.asapi.v3.typegroup.id.ITypeGroupAssignmentId;
import ch.ethz.sis.openbis.generic.asapi.v3.typegroup.id.ITypeGroupId;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.IOperationContext;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.common.get.AbstractMapObjectByIdExecutor;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.space.ISpaceAuthorizationExecutor;
import ch.ethz.sis.openbis.generic.server.asapi.v3.helper.common.IListObjectById;
import ch.ethz.sis.openbis.generic.server.asapi.v3.helper.typegroup.ListTypeGroupAssignmentById;
import ch.ethz.sis.openbis.generic.server.asapi.v3.helper.typegroup.ListTypeGroupByName;
import ch.ethz.sis.openbis.generic.server.asapi.v3.helper.typegroup.ListTypeGroupByTechId;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.IDAOFactory;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.ITypeGroupAssignmentDAO;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.ITypeGroupDAO;
import ch.systemsx.cisd.openbis.generic.shared.dto.SampleTypeTypeGroupsPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.TypeGroupPE;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MapSampleTypeTypeGroupsByIdExecutor extends AbstractMapObjectByIdExecutor<ITypeGroupAssignmentId, SampleTypeTypeGroupsPE>
        implements IMapSampleTypeTypeGroupsByIdExecutor
{
    private ITypeGroupAssignmentDAO typeGroupAssignmentDAO;

//    @Autowired
//    private ISpaceAuthorizationExecutor authorizationExecutor;

    @Override
    protected void checkAccess(IOperationContext context)
    {
//        authorizationExecutor.canGet(context);
    }

    @Override
    protected void addListers(IOperationContext context, List<IListObjectById<? extends ITypeGroupAssignmentId, SampleTypeTypeGroupsPE>> listers)
    {
        listers.add(new ListTypeGroupAssignmentById(typeGroupAssignmentDAO));
    }

    @Autowired
    private void setDAOFactory(IDAOFactory daoFactory)
    {
        typeGroupAssignmentDAO = daoFactory.getTypeGroupAssignmentDAO();
    }
}
