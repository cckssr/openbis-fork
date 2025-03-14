/*
 * Copyright ETH 2014 - 2023 Zürich, Scientific IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.ethz.sis.openbis.generic.server.asapi.v3.executor.deletion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.id.IObjectId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.ISearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.IdSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.deletion.fetchoptions.DeletionFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.deletion.id.DeletionTechId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.deletion.search.DeletedObjectIdSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.deletion.search.DeletionSearchCriteria;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.IOperationContext;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.common.search.AbstractSearchObjectManuallyExecutor;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.common.search.Matcher;
import ch.systemsx.cisd.openbis.generic.server.ComponentNames;
import ch.systemsx.cisd.openbis.generic.server.business.bo.ICommonBusinessObjectFactory;
import ch.systemsx.cisd.openbis.generic.server.business.bo.IDeletionTable;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.IDAOFactory;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.AbstractRegistrationHolder;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.Deletion;
import ch.systemsx.cisd.openbis.generic.shared.dto.DeletedDataPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.DeletionPE;

/**
 * @author pkupczyk
 */
@Component
public class SearchDeletionExecutor implements ISearchDeletionExecutor
{

    @Resource(name = ComponentNames.COMMON_BUSINESS_OBJECT_FACTORY)
    private ICommonBusinessObjectFactory businessObjectFactory;

    @Autowired
    private IDAOFactory daoFactory;

    @Autowired
    private IDeletionAuthorizationExecutor authorizationExecutor;

    @Override
    public List<Deletion> search(IOperationContext context, DeletionSearchCriteria criteria, DeletionFetchOptions fetchOptions)
    {
        authorizationExecutor.canSearch(context);

        if (context == null)
        {
            throw new IllegalArgumentException("Context cannot be null");
        }
        if (fetchOptions == null)
        {
            throw new IllegalArgumentException("Fetch options cannot be null");
        }

        List<Deletion> deletions = null;

        if (criteria.getCriteria() == null || criteria.getCriteria().isEmpty())
        {
            IDeletionTable deletionTable = businessObjectFactory.createDeletionTable(context.getSession());
            deletionTable.load(fetchOptions.hasDeletedObjects());
            deletions = deletionTable.getDeletions();
        } else
        {
            deletions = new SearchDeletions(daoFactory, fetchOptions).search(context, criteria);
        }

        deletions.sort(Comparator.comparing(AbstractRegistrationHolder::getRegistrationDate));

        return deletions;
    }

    private class SearchDeletions extends AbstractSearchObjectManuallyExecutor<DeletionSearchCriteria, Deletion>
    {

        private final DeletionFetchOptions fetchOptions;

        public SearchDeletions(final IDAOFactory daoFactory, final DeletionFetchOptions fetchOptions)
        {
            this.daoFactory = daoFactory;
            this.fetchOptions = fetchOptions;
        }

        @Override
        protected List<Deletion> listAll()
        {
            return new ArrayList<>();
        }

        @Override
        protected Matcher<Deletion> getMatcher(ISearchCriteria criteria)
        {
            if (criteria instanceof DeletedObjectIdSearchCriteria)
            {
                return new DeletedObjectIdMatcher();
            } else if (criteria instanceof IdSearchCriteria<?>)
            {
                return new IdMatcher();
            } else
            {
                throw new IllegalArgumentException("Unknown search criteria: " + criteria.getClass());
            }
        }

        private class IdMatcher extends Matcher<Deletion>
        {

            @Override public List<Deletion> getMatching(final IOperationContext context, final List<Deletion> deletions,
                    final ISearchCriteria criteria)
            {
                Object id = ((IdSearchCriteria<?>) criteria).getId();

                if (id == null)
                {
                    throw new IllegalArgumentException("Id cannot be null");
                } else if (id instanceof DeletionTechId)
                {
                    DeletionTechId techId = (DeletionTechId) id;
                    IDeletionTable deletionTable = businessObjectFactory.createDeletionTable(context.getSession());
                    deletionTable.load(List.of(techId.getTechId()), fetchOptions.hasDeletedObjects());
                    return deletionTable.getDeletions();
                } else
                {
                    throw new IllegalArgumentException("Unknown id: " + id.getClass());
                }
            }
        }

        private class DeletedObjectIdMatcher extends Matcher<Deletion>
        {

            @Override public List<Deletion> getMatching(final IOperationContext context, final List<Deletion> deletions,
                    final ISearchCriteria criteria)
            {
                IObjectId id = ((DeletedObjectIdSearchCriteria) criteria).getId();

                if (id == null)
                {
                    throw new IllegalArgumentException("Deleted object id cannot be null");
                } else if (id instanceof DataSetPermId)
                {
                    List<DeletedDataPE> deletedDataPEs =
                            daoFactory.getDataDAO().tryToFindDeletedDataSetsByCodes(List.of(((DataSetPermId) id).getPermId()));

                    if (deletedDataPEs.isEmpty())
                    {
                        return Collections.emptyList();
                    }

                    DeletionPE deletion = deletedDataPEs.get(0).getDeletion();
                    IDeletionTable deletionTable = businessObjectFactory.createDeletionTable(context.getSession());
                    deletionTable.load(List.of(deletion.getId()), fetchOptions.hasDeletedObjects());
                    return deletionTable.getDeletions();
                } else
                {
                    throw new IllegalArgumentException("Unsupported id: " + id.getClass());
                }
            }
        }

    }

}
