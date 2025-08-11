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

package ch.ethz.sis.openbis.systemtest.asapi.v3;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.TypeGroup;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.TypeGroupAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.fetchoptions.TypeGroupAssignmentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.fetchoptions.TypeGroupFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.TypeGroupAssignmentId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.TypeGroupId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.search.TypeGroupAssignmentSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.search.TypeGroupSearchCriteria;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

public class SearchTypeGroupTest extends AbstractTest
{
    @Test
    public void testSearchWithEmptyCriteria()
    {
        testSearchTypeGroup(TEST_USER, new TypeGroupSearchCriteria(), "TEST-TYPE-GROUP", "TYPE-GROUP-INTERNAL", "TEST-TYPE-GROUP-MODIFIED");
    }

    @Test
    public void testSearchWithNameContaining()
    {
        TypeGroupSearchCriteria criteria = new TypeGroupSearchCriteria();
        criteria.withCode().thatContains("TEST-TYPE");
        testSearchTypeGroup(TEST_USER, criteria, "TEST-TYPE-GROUP", "TEST-TYPE-GROUP-MODIFIED");
    }

    @Test
    public void testSearchWithIdContaining()
    {
        TypeGroupSearchCriteria criteria = new TypeGroupSearchCriteria();
        criteria.withId().thatEquals(new TypeGroupId("TYPE-GROUP-INTERNAL"));
        testSearchTypeGroup(TEST_USER, criteria, "TYPE-GROUP-INTERNAL");
    }

    @Test
    public void testSearchWithIdSetToNonexistentPermId()
    {
        TypeGroupSearchCriteria criteria = new TypeGroupSearchCriteria();
        criteria.withId().thatEquals(new TypeGroupId("IDONTEXIST"));
        testSearchTypeGroup(TEST_USER, criteria);
    }

    @Test
    public void testSearchAssignmentsWithEmptyCriteria()
    {
        List<TypeGroupAssignment> results = searchTypeGroupAssignments(TEST_USER, new TypeGroupAssignmentSearchCriteria());
        assertEquals(results.size(), 3);
    }

    @Test
    public void testSearchAssignmentsWithId()
    {
        TypeGroupAssignmentSearchCriteria criteria = new TypeGroupAssignmentSearchCriteria();
        criteria.withId().thatEquals(new TypeGroupAssignmentId(new EntityTypePermId("MASTER_PLATE", EntityKind.SAMPLE), new TypeGroupId("TYPE-GROUP-INTERNAL")));
        List<TypeGroupAssignment> results = searchTypeGroupAssignments(TEST_USER, criteria);
        assertEquals(results.size(), 1);
    }

    @Test
    public void testSearchAssignmentsWithSampleType()
    {
        TypeGroupAssignmentSearchCriteria criteria = new TypeGroupAssignmentSearchCriteria();
        criteria.withSampleType().withCode().thatEquals("MASTER_PLATE");
        List<TypeGroupAssignment> results = searchTypeGroupAssignments(TEST_USER, criteria);
        assertEquals(results.size(), 2);
    }

    @Test
    public void testSearchAssignmentsWithTypeGroup()
    {
        TypeGroupAssignmentSearchCriteria criteria = new TypeGroupAssignmentSearchCriteria();
        criteria.withTypeGroup().withCode().thatEquals("TYPE-GROUP-INTERNAL");
        List<TypeGroupAssignment> results = searchTypeGroupAssignments(TEST_USER, criteria);
        assertEquals(results.size(), 2);
    }

    private void testSearchTypeGroup(String user, TypeGroupSearchCriteria criteria, String... expectedPermIds)
    {
        String sessionToken = v3api.login(user, PASSWORD);

        SearchResult<TypeGroup> searchResult =
                v3api.searchTypeGroups(sessionToken, criteria, new TypeGroupFetchOptions());
        List<TypeGroup> tags = searchResult.getObjects();

        assertTypeGroups(tags, expectedPermIds);
        v3api.logout(sessionToken);
    }

    private List<TypeGroupAssignment> searchTypeGroupAssignments(String user, TypeGroupAssignmentSearchCriteria criteria)
    {
        String sessionToken = v3api.login(user, PASSWORD);

        TypeGroupAssignmentFetchOptions fetchOptions = new TypeGroupAssignmentFetchOptions();
        fetchOptions.withSampleType();
        fetchOptions.withTypeGroup();

        SearchResult<TypeGroupAssignment> searchResult =
                v3api.searchTypeGroupAssignments(sessionToken, criteria, fetchOptions);
        List<TypeGroupAssignment> assignments = searchResult.getObjects();

        v3api.logout(sessionToken);
        return assignments;
    }



}
