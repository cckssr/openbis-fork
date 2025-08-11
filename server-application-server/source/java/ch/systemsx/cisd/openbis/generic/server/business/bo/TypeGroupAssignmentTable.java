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

package ch.systemsx.cisd.openbis.generic.server.business.bo;

import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.openbis.generic.server.business.IRelationshipService;
import ch.systemsx.cisd.openbis.generic.server.business.bo.util.DataSetTypeWithoutExperimentChecker;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.IDAOFactory;
import ch.systemsx.cisd.openbis.generic.shared.dto.SampleTypeTypeGroupsPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.SampleTypeTypeGroupsTechId;
import ch.systemsx.cisd.openbis.generic.shared.dto.Session;
import ch.systemsx.cisd.openbis.generic.shared.managed_property.IManagedPropertyEvaluatorFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataRetrievalFailureException;

import java.util.List;

public class TypeGroupAssignmentTable extends AbstractBusinessObject implements ITypeGroupAssignmentTable
{
    private SampleTypeTypeGroupsPE typeGroupAssignmentPE;

    public TypeGroupAssignmentTable(final IDAOFactory daoFactory, final Session session,
            IManagedPropertyEvaluatorFactory managedPropertyEvaluatorFactory,
            DataSetTypeWithoutExperimentChecker dataSetTypeChecker,
            IRelationshipService relationshipService)
    {
        super(daoFactory, session, managedPropertyEvaluatorFactory, dataSetTypeChecker, relationshipService);
    }

    @Override
    public void deleteById(SampleTypeTypeGroupsTechId typeGroupAssignmentTechId)
    {
        loadByTechId(typeGroupAssignmentTechId);
        try
        {
            getTypeGroupAssignmentDAO().delete(typeGroupAssignmentPE);
        } catch (final DataAccessException ex)
        {

            throwException(ex, String.format("type group '%s'", typeGroupAssignmentPE.getId()));
        }
    }

    public void loadByTechId(SampleTypeTypeGroupsTechId typeGroupAssignmentId)
    {
        try
        {
            typeGroupAssignmentPE = getTypeGroupAssignmentDAO().findByTechId(List.of(typeGroupAssignmentId)).get(0);
        } catch (DataRetrievalFailureException exception)
        {
            throw new UserFailureException(String.format(
                    "Type group assignment with ID '%s' does not exist.", typeGroupAssignmentId));
        }
    }

    @Override
    public void save() throws UserFailureException
    {

    }
}
