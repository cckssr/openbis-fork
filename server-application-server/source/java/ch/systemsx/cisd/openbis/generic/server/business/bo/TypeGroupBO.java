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
import ch.systemsx.cisd.openbis.generic.shared.basic.TechId;
import ch.systemsx.cisd.openbis.generic.shared.dto.Session;
import ch.systemsx.cisd.openbis.generic.shared.dto.TypeGroupPE;
import ch.systemsx.cisd.openbis.generic.shared.managed_property.IManagedPropertyEvaluatorFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataRetrievalFailureException;

public class TypeGroupBO extends AbstractBusinessObject implements ITypeGroupBO
{

    private TypeGroupPE typeGroupPE;

    public TypeGroupBO(final IDAOFactory daoFactory, final Session session,
            IManagedPropertyEvaluatorFactory managedPropertyEvaluatorFactory,
            DataSetTypeWithoutExperimentChecker dataSetTypeChecker,
            IRelationshipService relationshipService)
    {
        super(daoFactory, session, managedPropertyEvaluatorFactory, dataSetTypeChecker, relationshipService);
    }

    @Override
    public void deleteByTechId(TechId typeGroupId)
    {
        loadByTechId(typeGroupId);
        try
        {
            getTypeGroupDAO().delete(typeGroupPE);
        } catch (final DataAccessException ex)
        {
            throwException(ex, String.format("type group '%s'", typeGroupPE.getName()));
        }
    }

    @Override
    public void loadByTechId(TechId techId)
    {
        try
        {
            typeGroupPE = getTypeGroupDAO().getById(techId);
        } catch (DataRetrievalFailureException exception)
        {
            throw new UserFailureException(String.format(
                    "Type group with ID '%s' does not exist.", techId));
        }
    }

    @Override
    public void save() throws UserFailureException
    {

    }
}
