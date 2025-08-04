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

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.*;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.search.TypeGroupNameSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.search.TypeGroupSearchCriteria;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.IOperationContext;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.common.search.*;
import ch.systemsx.cisd.openbis.generic.shared.dto.TypeGroupPE;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SearchTypeGroupExecutor extends AbstractSearchObjectManuallyExecutor<TypeGroupSearchCriteria, TypeGroupPE>
        implements ISearchTypeGroupExecutor
{
    @Autowired
    private ITypeGroupAuthorizationExecutor authorizationExecutor;

    @Override
    public List<TypeGroupPE> search(IOperationContext context, TypeGroupSearchCriteria criteria)
    {
        authorizationExecutor.canSearch(context);
        return super.search(context, criteria);
    }

    @Override
    protected List<TypeGroupPE> listAll()
    {
        return daoFactory.getTypeGroupDAO().listAllEntities();
    }

    @Override
    protected Matcher<TypeGroupPE> getMatcher(ISearchCriteria criteria)
    {
        if (criteria instanceof NameSearchCriteria || criteria instanceof TypeGroupNameSearchCriteria)
        {
            return new NameMatcher<>();
        }  else
        {
            throw new IllegalArgumentException("Unknown search criteria: " + criteria.getClass());
        }
    }

}
