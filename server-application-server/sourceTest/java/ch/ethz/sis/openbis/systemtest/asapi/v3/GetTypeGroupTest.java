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

import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.TypeGroup;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.TypeGroupAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.fetchoptions.TypeGroupAssignmentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.fetchoptions.TypeGroupFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.ITypeGroupId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.TypeGroupId;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import static org.testng.Assert.*;

public class GetTypeGroupTest extends AbstractTest
{
    @Test
    public void testGetByName()
    {
        String sessionToken = v3api.login(TEST_USER, PASSWORD);

        TypeGroupId permId1 = new TypeGroupId("TYPE-GROUP-INTERNAL");
        TypeGroupId permId2 = new TypeGroupId( "TEST-TYPE-GROUP");
        TypeGroupId permId3 = new TypeGroupId( "TEST-TYPE-GROUP-MODIFIED");

        Map<ITypeGroupId, TypeGroup> map =
                v3api.getTypeGroups(sessionToken, Arrays.asList(permId1, permId2, permId3),
                        new TypeGroupFetchOptions());

        assertEquals(map.size(), 3);

        Iterator<TypeGroup> iter = map.values().iterator();
        assertEquals(iter.next().getId(), permId1);
        assertEquals(iter.next().getId(), permId2);
        assertEquals(iter.next().getId(), permId3);

        assertEquals(map.get(permId1).getId(), permId1);
        assertEquals(map.get(permId2).getId(), permId2);
        assertEquals(map.get(permId3).getId(), permId3);

        v3api.logout(sessionToken);
    }

    @Test
    public void testGetWithRegistrator()
    {
        String sessionToken = v3api.login(TEST_USER, PASSWORD);

        TypeGroupId permId = new TypeGroupId( "TEST-TYPE-GROUP");

        TypeGroupFetchOptions fetchOptions = new TypeGroupFetchOptions();
        fetchOptions.withRegistrator();

        TypeGroup typeGroup =
                v3api.getTypeGroups(sessionToken, Arrays.asList(permId),
                        fetchOptions).get(permId);

        assertNotNull(typeGroup.getRegistrator());
        assertEquals(typeGroup.getRegistrator().getUserId(), SYSTEM_USER);

        v3api.logout(sessionToken);
    }

    @Test
    public void testGetWithModifier()
    {
        String sessionToken = v3api.login(TEST_USER, PASSWORD);

        TypeGroupId permId = new TypeGroupId( "TEST-TYPE-GROUP-MODIFIED");

        TypeGroupFetchOptions fetchOptions = new TypeGroupFetchOptions();
        fetchOptions.withModifier();

        TypeGroup typeGroup =
                v3api.getTypeGroups(sessionToken, Arrays.asList(permId),
                        fetchOptions).get(permId);

        assertNotNull(typeGroup.getModifier());
        assertEquals(typeGroup.getModifier().getUserId(), TEST_USER);

        v3api.logout(sessionToken);
    }

    @Test
    public void testGetInternalTypeGroup()
    {
        String sessionToken = v3api.login(TEST_USER, PASSWORD);

        TypeGroupId permId = new TypeGroupId( "TYPE-GROUP-INTERNAL");

        TypeGroupFetchOptions fetchOptions = new TypeGroupFetchOptions();

        TypeGroup typeGroup =
                v3api.getTypeGroups(sessionToken, Arrays.asList(permId),
                        fetchOptions).get(permId);

        assertTrue(typeGroup.isManagedInternally());

        v3api.logout(sessionToken);
    }

    @Test
    public void testGetWithAssignmentsBasic()
    {
        String sessionToken = v3api.login(TEST_USER, PASSWORD);

        TypeGroupId permId = new TypeGroupId( "TYPE-GROUP-INTERNAL");

        TypeGroupFetchOptions fetchOptions = new TypeGroupFetchOptions();
        fetchOptions.withTypeGroupAssignments();

        TypeGroup typeGroup =
                v3api.getTypeGroups(sessionToken, Arrays.asList(permId),
                        fetchOptions).get(permId);

        assertEquals(typeGroup.getTypeGroupAssignments().size(), 2);
        TypeGroupAssignment assignment = typeGroup.getTypeGroupAssignments().get(0);
        assertNull(assignment.getTypeGroup());
        assertNull(assignment.getSampleType());

        assignment = typeGroup.getTypeGroupAssignments().get(1);
        assertNull(assignment.getTypeGroup());
        assertNull(assignment.getSampleType());
        assertTrue(assignment.isManagedInternally());

        v3api.logout(sessionToken);
    }

    @Test
    public void testGetWithAssignments()
    {
        String sessionToken = v3api.login(TEST_USER, PASSWORD);

        TypeGroupId permId = new TypeGroupId( "TYPE-GROUP-INTERNAL");

        TypeGroupFetchOptions fetchOptions = new TypeGroupFetchOptions();
        TypeGroupAssignmentFetchOptions assignmentFetchOptions = fetchOptions.withTypeGroupAssignments();
        assignmentFetchOptions.withTypeGroup();
        assignmentFetchOptions.withSampleType();

        TypeGroup typeGroup =
                v3api.getTypeGroups(sessionToken, Arrays.asList(permId),
                        fetchOptions).get(permId);

        assertEquals(typeGroup.getTypeGroupAssignments().size(), 2);
        TypeGroupAssignment assignment = typeGroup.getTypeGroupAssignments().get(0);
        assertEquals(assignment.getTypeGroup().getCode(), "TYPE-GROUP-INTERNAL");
        assertEquals(assignment.getSampleType().getCode(), "MASTER_PLATE");

        assignment = typeGroup.getTypeGroupAssignments().get(1);
        assertEquals(assignment.getTypeGroup().getCode(), "TYPE-GROUP-INTERNAL");
        assertEquals(assignment.getSampleType().getCode(), "INTERNAL_TEST");
        assertTrue(assignment.isManagedInternally());

        v3api.logout(sessionToken);
    }

    @Test
    public void testGetByIdsNonexistent()
    {
        String sessionToken = v3api.login(TEST_USER, PASSWORD);

        TypeGroupId permId1 = new TypeGroupId("IDONTEXIST");
        TypeGroupId permId2 = new TypeGroupId( "TEST-TYPE-GROUP");

        Map<ITypeGroupId, TypeGroup> map =
                v3api.getTypeGroups(sessionToken, Arrays.asList(permId1, permId2),
                        new TypeGroupFetchOptions());

        assertEquals(map.size(), 1);

        Iterator<TypeGroup> iter = map.values().iterator();
        assertEquals(iter.next().getId(), permId2);
        assertEquals(map.get(permId2).getId(), permId2);

        v3api.logout(sessionToken);
    }

    @Test
    public void testGetByIdsDuplicated()
    {
        String sessionToken = v3api.login(TEST_USER, PASSWORD);

        TypeGroupId permId1 = new TypeGroupId("TEST-TYPE-GROUP");
        TypeGroupId permId2 = new TypeGroupId( "TEST-TYPE-GROUP");

        Map<ITypeGroupId, TypeGroup> map =
                v3api.getTypeGroups(sessionToken, Arrays.asList(permId1, permId2),
                        new TypeGroupFetchOptions());

        assertEquals(map.size(), 1);

        assertEquals(map.get(permId1).getId(), permId1);

        v3api.logout(sessionToken);
    }
}
