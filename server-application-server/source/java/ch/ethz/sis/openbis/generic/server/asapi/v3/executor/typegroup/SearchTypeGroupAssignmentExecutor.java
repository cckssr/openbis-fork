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

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.AbstractSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.ISearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleTypeSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.search.TypeGroupAssignmentSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.search.TypeGroupSearchCriteria;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.IOperationContext;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.common.search.AbstractSearchObjectManuallyExecutor;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.common.search.ISearchObjectExecutor;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.common.search.Matcher;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.sample.ISearchSampleTypeExecutor;
import ch.systemsx.cisd.openbis.generic.shared.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class SearchTypeGroupAssignmentExecutor extends AbstractSearchObjectManuallyExecutor<TypeGroupAssignmentSearchCriteria, SampleTypeTypeGroupsPE>
        implements ISearchTypeGroupAssignmentExecutor
{

    @Autowired
    private ITypeGroupAssignmentAuthorizationExecutor authorizationExecutor;

    @Autowired
    private ISearchTypeGroupExecutor searchTypeGroupExecutor;

    @Autowired
    private ISearchSampleTypeExecutor searchSampleTypeExecutor;

    @Override
    public List<SampleTypeTypeGroupsPE> search(IOperationContext context, TypeGroupAssignmentSearchCriteria criteria)
    {
        authorizationExecutor.canSearch(context);
        return super.search(context, criteria);
    }

    @Override
    protected List<SampleTypeTypeGroupsPE> listAll()
    {
        return daoFactory.getTypeGroupAssignmentDAO().listAllEntities();
    }

    @Override
    protected Matcher<SampleTypeTypeGroupsPE> getMatcher(ISearchCriteria criteria)
    {
        if (criteria instanceof TypeGroupSearchCriteria)
        {
            return new TypeGroupMatcher();
        } else if(criteria instanceof SampleTypeSearchCriteria) {
            return new SampleTypeMatcher();
        } else
        {
            throw new IllegalArgumentException("Unknown search criteria: " + criteria.getClass());
        }
    }

    private class TypeGroupMatcher extends EntityMatcher<TypeGroupSearchCriteria, SampleTypeTypeGroupsPE, TypeGroupPE>
    {
        public TypeGroupMatcher()
        {
            super(searchTypeGroupExecutor);
        }

        @Override
        public TypeGroupPE getSubObject(SampleTypeTypeGroupsPE object)
        {
            return object.getTypeGroup();
        }
    }

    private class SampleTypeMatcher extends EntityMatcher<SampleTypeSearchCriteria, SampleTypeTypeGroupsPE, SampleTypePE>
    {
        public SampleTypeMatcher()
        {
            super(searchSampleTypeExecutor);
        }

        @Override
        public SampleTypePE getSubObject(SampleTypeTypeGroupsPE object)
        {
            return object.getSampleType();
        }
    }

    private abstract static class EntityMatcher<CRITERIA extends AbstractSearchCriteria, OBJECT, SUBOBJECT> extends Matcher<OBJECT>
    {
        private final ISearchObjectExecutor<CRITERIA, SUBOBJECT> searchExecutor;

        protected EntityMatcher(ISearchObjectExecutor<CRITERIA, SUBOBJECT> searchExecutor)
        {
            this.searchExecutor = searchExecutor;
        }

        @Override
        public List<OBJECT> getMatching(IOperationContext context, List<OBJECT> objects, ISearchCriteria criteria)
        {
            @SuppressWarnings("unchecked")
            List<SUBOBJECT> list = searchExecutor.search(context, (CRITERIA) criteria);
            Set<SUBOBJECT> set = new HashSet<>(list);

            List<OBJECT> matches = new ArrayList<>();
            for (OBJECT object : objects)
            {
                if (set.contains(getSubObject(object)))
                {
                    matches.add(object);
                }
            }

            return matches;
        }

        public abstract SUBOBJECT getSubObject(OBJECT object);

    }
}
