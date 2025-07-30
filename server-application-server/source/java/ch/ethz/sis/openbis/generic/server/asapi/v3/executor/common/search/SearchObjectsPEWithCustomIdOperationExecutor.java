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

package ch.ethz.sis.openbis.generic.server.asapi.v3.executor.common.search;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.fetchoptions.FetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.AbstractSearchCriteria;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.IOperationContext;
import ch.ethz.sis.openbis.generic.server.asapi.v3.search.planner.ILocalSearchManager;
import ch.ethz.sis.openbis.generic.server.asapi.v3.translator.ITranslator;
import ch.ethz.sis.openbis.generic.server.asapi.v3.translator.TranslationContext;
import ch.systemsx.cisd.openbis.generic.shared.basic.ICustomIdHolder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class SearchObjectsPEWithCustomIdOperationExecutor<OBJECT, OBJECT_PE extends ICustomIdHolder<OBJECT_PE_ID>, CRITERIA extends AbstractSearchCriteria, FETCH_OPTIONS extends FetchOptions<OBJECT>, OBJECT_PE_ID extends Serializable>
        extends AbstractSearchObjectsOperationExecutor<OBJECT, OBJECT_PE_ID, CRITERIA, FETCH_OPTIONS>
{
    protected abstract ISearchObjectExecutor<CRITERIA, OBJECT_PE> getExecutor();

    protected abstract ITranslator<OBJECT_PE_ID, OBJECT, FETCH_OPTIONS> getTranslator();

    @Override
    protected List<OBJECT_PE_ID> doSearch(IOperationContext context, CRITERIA criteria, FETCH_OPTIONS fetchOptions)
    {
        List<OBJECT_PE> objectPEs = getExecutor().search(context, criteria);
        List<OBJECT_PE_ID> ids = new ArrayList<>();

        for (OBJECT_PE objectPE : objectPEs)
        {
            ids.add(objectPE.getId());
        }

        return ids;
    }

    @Override
    protected Map<OBJECT_PE_ID, OBJECT> doTranslate(TranslationContext translationContext,
            Collection<OBJECT_PE_ID> ids, FETCH_OPTIONS fetchOptions)
    {
        return getTranslator().translate(translationContext, ids, fetchOptions);
    }


    @Override
    protected ILocalSearchManager<CRITERIA, OBJECT, OBJECT_PE_ID> getSearchManager() {
        throw new RuntimeException("This method is not implemented yet.");
    }
}
