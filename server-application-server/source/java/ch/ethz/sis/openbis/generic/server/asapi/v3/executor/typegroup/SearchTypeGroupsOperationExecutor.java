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
import ch.ethz.sis.openbis.generic.asapi.v3.typegroup.TypeGroup;
import ch.ethz.sis.openbis.generic.asapi.v3.typegroup.fetchoptions.TypeGroupFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.typegroup.search.SearchTypeGroupsOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.typegroup.search.SearchTypeGroupsOperationResult;
import ch.ethz.sis.openbis.generic.asapi.v3.typegroup.search.TypeGroupSearchCriteria;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.IOperationContext;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.common.search.ISearchObjectExecutor;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.common.search.SearchObjectsPEOperationExecutor;
import ch.ethz.sis.openbis.generic.server.asapi.v3.search.planner.ILocalSearchManager;
import ch.ethz.sis.openbis.generic.server.asapi.v3.search.planner.TypeGroupSearchManager;
import ch.ethz.sis.openbis.generic.server.asapi.v3.translator.ITranslator;
import ch.ethz.sis.openbis.generic.server.asapi.v3.translator.typegroup.ITypeGroupTranslator;
import ch.systemsx.cisd.openbis.generic.shared.dto.TypeGroupPE;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SearchTypeGroupsOperationExecutor extends SearchObjectsPEOperationExecutor<TypeGroup, TypeGroupPE, TypeGroupSearchCriteria, TypeGroupFetchOptions>
        implements ISearchTypeGroupsOperationExecutor
{
    @Autowired
    private ISearchTypeGroupExecutor searchExecutor;

    @Autowired
    private ITypeGroupTranslator translator;

    @Autowired
    private TypeGroupSearchManager searchManager;

    @Override
    protected Class<? extends SearchObjectsOperation<TypeGroupSearchCriteria, TypeGroupFetchOptions>> getOperationClass()
    {
        return SearchTypeGroupsOperation.class;
    }

    @Override
    protected ISearchObjectExecutor<TypeGroupSearchCriteria, TypeGroupPE> getExecutor()
    {
        return searchExecutor;
    }

    @Override
    protected ITranslator<Long, TypeGroup, TypeGroupFetchOptions> getTranslator()
    {
        return translator;
    }

    @Override
    protected SearchObjectsOperationResult<TypeGroup> getOperationResult(
            SearchResult<TypeGroup> searchResult)
    {
        return new SearchTypeGroupsOperationResult(searchResult);
    }

    @Override
    protected ILocalSearchManager<TypeGroupSearchCriteria, TypeGroup, Long> getSearchManager() {
        return searchManager;
    }

    @Override
    protected SearchObjectsOperationResult<TypeGroup> doExecute(final IOperationContext context,
            final SearchObjectsOperation<TypeGroupSearchCriteria, TypeGroupFetchOptions> operation)
    {
        return executeDirectSQLSearch(context, operation);
    }
}
