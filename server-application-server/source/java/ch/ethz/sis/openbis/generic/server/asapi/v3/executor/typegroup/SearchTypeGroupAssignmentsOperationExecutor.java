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

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchObjectsOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchObjectsOperationResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.TypeGroupAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.fetchoptions.TypeGroupAssignmentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.search.*;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.common.search.ISearchObjectExecutor;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.common.search.SearchObjectsPEWithCustomIdOperationExecutor;
import ch.ethz.sis.openbis.generic.server.asapi.v3.translator.ITranslator;
import ch.ethz.sis.openbis.generic.server.asapi.v3.translator.typegroup.ITypeGroupAssignmentTranslator;
import ch.systemsx.cisd.openbis.generic.shared.dto.SampleTypeTypeGroupsId;
import ch.systemsx.cisd.openbis.generic.shared.dto.SampleTypeTypeGroupsPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.SampleTypeTypeGroupsTechId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SearchTypeGroupAssignmentsOperationExecutor
        extends SearchObjectsPEWithCustomIdOperationExecutor<TypeGroupAssignment, SampleTypeTypeGroupsPE, TypeGroupAssignmentSearchCriteria, TypeGroupAssignmentFetchOptions, SampleTypeTypeGroupsTechId>
        implements ISearchTypeGroupAssignmentsOperationExecutor
{

    @Autowired
    private ISearchTypeGroupAssignmentExecutor searchExecutor;

    @Autowired
    private ITypeGroupAssignmentTranslator translator;

    @Override
    protected ISearchObjectExecutor<TypeGroupAssignmentSearchCriteria, SampleTypeTypeGroupsPE> getExecutor()
    {
        return searchExecutor;
    }

    @Override
    protected ITranslator<SampleTypeTypeGroupsTechId, TypeGroupAssignment, TypeGroupAssignmentFetchOptions> getTranslator()
    {
        return translator;
    }

    @Override
    protected SearchObjectsOperationResult<TypeGroupAssignment> getOperationResult(
            SearchResult<TypeGroupAssignment> searchResult)
    {
        return new SearchTypeGroupAssignmentsOperationResult(searchResult);
    }

    @Override
    protected Class<? extends SearchObjectsOperation<TypeGroupAssignmentSearchCriteria, TypeGroupAssignmentFetchOptions>> getOperationClass()
    {
        return SearchTypeGroupAssignmentsOperation.class;
    }

}
