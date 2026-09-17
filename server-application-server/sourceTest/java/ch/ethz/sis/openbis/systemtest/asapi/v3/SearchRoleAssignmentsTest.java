/*
 * Copyright ETH 2017 - 2023 Zürich, Scientific IT Services
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
package ch.ethz.sis.openbis.systemtest.asapi.v3;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.testng.annotations.Test;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.authorizationgroup.AuthorizationGroup;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.authorizationgroup.create.AuthorizationGroupCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.authorizationgroup.id.AuthorizationGroupPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.create.PersonCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.id.PersonPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.create.ProjectCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.ProjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.roleassignment.Role;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.roleassignment.RoleAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.roleassignment.RoleLevel;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.roleassignment.create.RoleAssignmentCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.roleassignment.fetchoptions.RoleAssignmentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.roleassignment.id.RoleAssignmentTechId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.roleassignment.search.RoleAssignmentSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.create.SpaceCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;

/**
 * @author Franz-Josef Elmer
 */
public class SearchRoleAssignmentsTest extends AbstractTest
{
    @Test
    public void testSearchForAll()
    {
        // Given
        String sessionToken = v3api.login(TEST_USER, PASSWORD);
        RoleAssignmentSearchCriteria searchCriteria = new RoleAssignmentSearchCriteria();
        RoleAssignmentFetchOptions fetchOptions = new RoleAssignmentFetchOptions();
        fetchOptions.withUser();
        fetchOptions.withAuthorizationGroup();
        fetchOptions.withSpace();
        fetchOptions.withProject();

        // When
        List<RoleAssignment> assignments = v3api.searchRoleAssignments(sessionToken, searchCriteria, fetchOptions).getObjects();

        // Then
        assertMinimumNumbersOfRoleAssignments(assignments, 29);
    }

    @Test
    public void testSearchForAllProjects()
    {
        // Given
        String sessionToken = v3api.login(TEST_USER, PASSWORD);
        RoleAssignmentSearchCriteria searchCriteria = new RoleAssignmentSearchCriteria();
        searchCriteria.withProject();
        RoleAssignmentFetchOptions fetchOptions = new RoleAssignmentFetchOptions();
        fetchOptions.withUser();
        fetchOptions.withAuthorizationGroup();
        fetchOptions.withSpace();
        fetchOptions.withProject();

        // When
        List<RoleAssignment> assignments = v3api.searchRoleAssignments(sessionToken, searchCriteria, fetchOptions).getObjects();

        // Then
        assertMinimumNumbersOfRoleAssignments(assignments, 5);
    }

    @Test
    public void testSearchForAllSpace()
    {
        // Given
        String sessionToken = v3api.login(TEST_USER, PASSWORD);
        RoleAssignmentSearchCriteria searchCriteria = new RoleAssignmentSearchCriteria();
        searchCriteria.withSpace();
        RoleAssignmentFetchOptions fetchOptions = new RoleAssignmentFetchOptions();
        fetchOptions.withUser();
        fetchOptions.withAuthorizationGroup();
        fetchOptions.withSpace();
        fetchOptions.withProject();

        // When
        List<RoleAssignment> assignments = v3api.searchRoleAssignments(sessionToken, searchCriteria, fetchOptions).getObjects();

        // Then
        assertMinimumNumbersOfRoleAssignments(assignments, 19);
    }

    @Test
    public void testSearchForAllAuthorizationGroups()
    {
        // Given
        String sessionToken = v3api.login(TEST_USER, PASSWORD);
        RoleAssignmentSearchCriteria searchCriteria = new RoleAssignmentSearchCriteria();
        searchCriteria.withAuthorizationGroup();
        RoleAssignmentFetchOptions fetchOptions = new RoleAssignmentFetchOptions();
        fetchOptions.withUser();
        fetchOptions.withAuthorizationGroup();
        fetchOptions.withSpace();
        fetchOptions.withProject();

        // When
        List<RoleAssignment> assignments = v3api.searchRoleAssignments(sessionToken, searchCriteria, fetchOptions).getObjects();

        // Then
        assertMinimumNumbersOfRoleAssignments(assignments, 2);
    }

    @Test
    public void testSearchForAllUsers()
    {
        // Given
        String sessionToken = v3api.login(TEST_USER, PASSWORD);
        RoleAssignmentSearchCriteria searchCriteria = new RoleAssignmentSearchCriteria();
        searchCriteria.withUser();
        RoleAssignmentFetchOptions fetchOptions = new RoleAssignmentFetchOptions();
        fetchOptions.withUser();
        fetchOptions.withAuthorizationGroup();
        fetchOptions.withSpace();
        fetchOptions.withProject();

        // When
        List<RoleAssignment> assignments = v3api.searchRoleAssignments(sessionToken, searchCriteria, fetchOptions).getObjects();

        // Then
        assertMinimumNumbersOfRoleAssignments(assignments, 27);
    }

    @Test
    public void testSearchForAUser()
    {
        // Given
        String sessionToken = v3api.login(TEST_USER, PASSWORD);
        RoleAssignmentSearchCriteria searchCriteria = new RoleAssignmentSearchCriteria();
        searchCriteria.withUser().withUserId().thatEquals("test");
        RoleAssignmentFetchOptions fetchOptions = new RoleAssignmentFetchOptions();
        fetchOptions.withUser();
        fetchOptions.withAuthorizationGroup();
        fetchOptions.withSpace();
        fetchOptions.withProject();

        // When
        List<RoleAssignment> assignments = v3api.searchRoleAssignments(sessionToken, searchCriteria, fetchOptions).getObjects();

        // Then
        assertRoleAssignments(assignments, "ADMIN INSTANCE for user test\n"
                + "ADMIN SPACE[CISD] for user test\n"
                + "ADMIN SPACE[TESTGROUP] for user test\n"
                + "ETL_SERVER INSTANCE for user test\n"
                + "ETL_SERVER SPACE[CISD] for user test\n");
    }

    @Test
    public void testSearchForAUserAlsoViaGroups()
    {
        // Given
        String sessionToken = v3api.login(TEST_USER, PASSWORD);
        RoleAssignmentSearchCriteria searchCriteria = new RoleAssignmentSearchCriteria();
        searchCriteria.withOrOperator();
        searchCriteria.withUser().withUserId().thatContains("p_");
        searchCriteria.withAuthorizationGroup().withUser().withUserId().thatContains("p_");
        RoleAssignmentFetchOptions fetchOptions = new RoleAssignmentFetchOptions();
        fetchOptions.withUser();
        fetchOptions.withAuthorizationGroup();
        fetchOptions.withSpace();
        fetchOptions.withProject();

        // When
        List<RoleAssignment> assignments = v3api.searchRoleAssignments(sessionToken, searchCriteria, fetchOptions).getObjects();

        // Then
        assertRoleAssignments(assignments, "ADMIN SPACE[TESTGROUP] for group AGROUP\n"
                + "ETL_SERVER SPACE[TESTGROUP] for user test_group_etl_server\n"
                + "USER PROJECT[/CISD/DEFAULT] for group AGROUP\n");
    }

    @Test
    public void testSearchForSomeUsers()
    {
        // Given
        String sessionToken = v3api.login(TEST_USER, PASSWORD);
        RoleAssignmentSearchCriteria searchCriteria = new RoleAssignmentSearchCriteria();
        searchCriteria.withUser().withUserIds().thatIn(Arrays.asList("test", "homeless"));
        RoleAssignmentFetchOptions fetchOptions = new RoleAssignmentFetchOptions();
        fetchOptions.withUser();
        fetchOptions.withAuthorizationGroup();
        fetchOptions.withSpace();
        fetchOptions.withProject();

        // When
        List<RoleAssignment> assignments = v3api.searchRoleAssignments(sessionToken, searchCriteria, fetchOptions).getObjects();

        // Then
        assertRoleAssignments(assignments, "ADMIN INSTANCE for user test\n"
                + "ADMIN SPACE[CISD] for user test\n"
                + "ADMIN SPACE[TESTGROUP] for user homeless\n"
                + "ADMIN SPACE[TESTGROUP] for user test\n"
                + "ETL_SERVER INSTANCE for user test\n"
                + "ETL_SERVER SPACE[CISD] for user test\n"
                + "POWER_USER SPACE[CISD] for user homeless\n");
    }

    @Test
    public void testSearchForAUserAndASpace()
    {
        // Given
        String sessionToken = v3api.login(TEST_USER, PASSWORD);
        RoleAssignmentSearchCriteria searchCriteria = new RoleAssignmentSearchCriteria();
        searchCriteria.withUser().withUserId().thatEquals("test");
        searchCriteria.withAndOperator();
        searchCriteria.withSpace().withCode().thatStartsWith("C");
        RoleAssignmentFetchOptions fetchOptions = new RoleAssignmentFetchOptions();
        fetchOptions.withUser();
        fetchOptions.withAuthorizationGroup();
        fetchOptions.withSpace();
        fetchOptions.withProject();

        // When
        List<RoleAssignment> assignments = v3api.searchRoleAssignments(sessionToken, searchCriteria, fetchOptions).getObjects();

        // Then
        assertRoleAssignments(assignments, "ADMIN SPACE[CISD] for user test\n"
                + "ETL_SERVER SPACE[CISD] for user test\n");
    }

    @Test
    public void testSearchForARoleAssignmentId()
    {
        // Given
        String sessionToken = v3api.login(TEST_USER, PASSWORD);
        RoleAssignmentSearchCriteria searchCriteria = new RoleAssignmentSearchCriteria();
        searchCriteria.withId().thatEquals(new RoleAssignmentTechId(2L));
        RoleAssignmentFetchOptions fetchOptions = new RoleAssignmentFetchOptions();
        fetchOptions.withUser();
        fetchOptions.withAuthorizationGroup();
        fetchOptions.withSpace();
        fetchOptions.withProject();

        // When
        List<RoleAssignment> assignments = v3api.searchRoleAssignments(sessionToken, searchCriteria, fetchOptions).getObjects();

        // Then
        assertRoleAssignments(assignments, "ADMIN SPACE[CISD] for user test\n");
    }

    @Test
    public void testSearchForNonAdminUsers()
    {
        String instanceAdminToken = v3api.login(TEST_USER, PASSWORD);

        // spaces
        SpaceCreation spaceCreation = new SpaceCreation();
        spaceCreation.setCode("SPACE_" + UUID.randomUUID());

        SpacePermId spacePermId = v3api.createSpaces(instanceAdminToken, List.of(spaceCreation)).getFirst();

        // projects
        ProjectCreation projectCreation = new ProjectCreation();
        projectCreation.setSpaceId(new SpacePermId(spaceCreation.getCode()));
        projectCreation.setCode("PROJECT_" + UUID.randomUUID());

        ProjectIdentifier projectIdentifier = new ProjectIdentifier(spaceCreation.getCode(), projectCreation.getCode());

        v3api.createProjects(instanceAdminToken, List.of(projectCreation));

        // space admin
        PersonCreation spaceAdminCreation = new PersonCreation();
        spaceAdminCreation.setUserId("space_admin_" + UUID.randomUUID());

        // space user
        PersonCreation spaceUserCreation = new PersonCreation();
        spaceUserCreation.setUserId("space_user_" + UUID.randomUUID());

        // project admin
        PersonCreation projectAdminCreation = new PersonCreation();
        projectAdminCreation.setUserId("project_admin_" + UUID.randomUUID() + "_pa_on");

        // project user
        PersonCreation projectUserCreation = new PersonCreation();
        projectUserCreation.setUserId("project_user_" + UUID.randomUUID() + "_pa_on");

        v3api.createPersons(instanceAdminToken, List.of(spaceAdminCreation, spaceUserCreation, projectAdminCreation, projectUserCreation));

        // space admin via group assignment
        AuthorizationGroupCreation spaceAdminGroupCreation = new AuthorizationGroupCreation();
        spaceAdminGroupCreation.setCode("space_admin_group_" + UUID.randomUUID());
        spaceAdminGroupCreation.setUserIds(List.of(new PersonPermId(spaceAdminCreation.getUserId())));

        final AuthorizationGroupPermId spaceAdminGroupId =
                v3api.createAuthorizationGroups(instanceAdminToken, List.of(spaceAdminGroupCreation)).getFirst();

        RoleAssignmentCreation spaceAdminGroupRole = new RoleAssignmentCreation();
        spaceAdminGroupRole.setSpaceId(new SpacePermId(spaceCreation.getCode()));
        spaceAdminGroupRole.setAuthorizationGroupId(spaceAdminGroupId);
        spaceAdminGroupRole.setRole(Role.ADMIN);

        v3api.createRoleAssignments(instanceAdminToken, List.of(spaceAdminGroupRole));

        // space user via direct assignment
        RoleAssignmentCreation spaceUserRole = new RoleAssignmentCreation();
        spaceUserRole.setSpaceId(new SpacePermId(spaceCreation.getCode()));
        spaceUserRole.setUserId(new PersonPermId(spaceUserCreation.getUserId()));
        spaceUserRole.setRole(Role.USER);

        v3api.createRoleAssignments(instanceAdminToken, List.of(spaceUserRole));

        // project admin via group assignment
        AuthorizationGroupCreation projectAdminGroupCreation = new AuthorizationGroupCreation();
        projectAdminGroupCreation.setCode("project_admin_group_" + UUID.randomUUID());
        projectAdminGroupCreation.setUserIds(List.of(new PersonPermId(projectAdminCreation.getUserId())));

        final AuthorizationGroupPermId projectAdminGroupId =
                v3api.createAuthorizationGroups(instanceAdminToken, List.of(projectAdminGroupCreation)).getFirst();

        RoleAssignmentCreation projectAdminGroupRole = new RoleAssignmentCreation();
        projectAdminGroupRole.setProjectId(new ProjectIdentifier(spaceCreation.getCode(), projectCreation.getCode()));
        projectAdminGroupRole.setAuthorizationGroupId(projectAdminGroupId);
        projectAdminGroupRole.setRole(Role.ADMIN);

        v3api.createRoleAssignments(instanceAdminToken, List.of(projectAdminGroupRole));

        // project user via direct assignment
        RoleAssignmentCreation projectUserRole = new RoleAssignmentCreation();
        projectUserRole.setProjectId(new ProjectIdentifier(spaceCreation.getCode(), projectCreation.getCode()));
        projectUserRole.setUserId(new PersonPermId(projectUserCreation.getUserId()));
        projectUserRole.setRole(Role.USER);

        v3api.createRoleAssignments(instanceAdminToken, List.of(projectUserRole));

        RoleAssignmentSearchCriteria criteria = new RoleAssignmentSearchCriteria();
        RoleAssignmentFetchOptions fetchOptions = new RoleAssignmentFetchOptions();
        fetchOptions.withSpace();
        fetchOptions.withProject();
        fetchOptions.withUser();
        fetchOptions.withAuthorizationGroup();

        // check space admin roles
        String spaceAdminSessionToken = v3api.login(spaceAdminCreation.getUserId(), PASSWORD);
        SearchResult<RoleAssignment> spaceAdminResult = v3api.searchRoleAssignments(spaceAdminSessionToken, criteria, fetchOptions);
        assertRoleAssignments(spaceAdminResult.getObjects(),
                "ADMIN PROJECT[" + projectIdentifier + "] for group " + projectAdminGroupId + "\n"
                        + "ADMIN SPACE[" + spacePermId + "] for group " + spaceAdminGroupId + "\n"
                        + "USER PROJECT[" + projectIdentifier + "] for user " + projectUserCreation.getUserId() + "\n"
                        + "USER SPACE[" + spacePermId + "] for user " + spaceUserCreation.getUserId() + "\n");

        // check space user roles
        String spaceUserSessionToken = v3api.login(spaceUserCreation.getUserId(), PASSWORD);
        SearchResult<RoleAssignment> spaceUserResult = v3api.searchRoleAssignments(spaceUserSessionToken, criteria, fetchOptions);
        assertRoleAssignments(spaceUserResult.getObjects(), "USER SPACE[" + spacePermId + "] for user " + spaceUserCreation.getUserId() + "\n");

        // check project admin roles
        String projectAdminSessionToken = v3api.login(projectAdminCreation.getUserId(), PASSWORD);
        SearchResult<RoleAssignment> projectAdminResult = v3api.searchRoleAssignments(projectAdminSessionToken, criteria, fetchOptions);
        assertRoleAssignments(projectAdminResult.getObjects(),
                "ADMIN PROJECT[" + projectIdentifier + "] for group " + projectAdminGroupId + "\n"
                        + "USER PROJECT[" + projectIdentifier + "] for user " + projectUserCreation.getUserId() + "\n");

        // check project user roles
        String projectUserSessionToken = v3api.login(projectUserCreation.getUserId(), PASSWORD);
        SearchResult<RoleAssignment> projectUserResult = v3api.searchRoleAssignments(projectUserSessionToken, criteria, fetchOptions);
        assertRoleAssignments(projectUserResult.getObjects(),
                "USER PROJECT[" + projectIdentifier + "] for user " + projectUserCreation.getUserId() + "\n");
    }

    @Test
    public void testLogging()
    {
        String sessionToken = v3api.login(TEST_USER, PASSWORD);

        RoleAssignmentSearchCriteria c = new RoleAssignmentSearchCriteria();
        c.withUser().withUserId().thatEquals("test");

        RoleAssignmentFetchOptions fo = new RoleAssignmentFetchOptions();
        fo.withUser();
        fo.withAuthorizationGroup();

        v3api.searchRoleAssignments(sessionToken, c, fo);

        assertAccessLog(
                "search-role-assignments  SEARCH_CRITERIA:\n'ROLE_ASSIGNMENT\n    with person:\n        with attribute 'userId' equal to 'test'\n'\nFETCH_OPTIONS:\n'RoleAssignment\n    with User\n    with AuthorizationGroup\n'");
    }

    private void assertMinimumNumbersOfRoleAssignments(List<RoleAssignment> assignments, int minSize)
    {
        assertTrue(assignments.size() >= minSize, "Expecting at least " + minSize
                + " but got only the following " + assignments.size() + ": " + render(assignments));
    }

    private void assertRoleAssignments(List<RoleAssignment> assignments, String expectedAssignmentsAsString)
    {
        List<String> assignmentsAsStrings = render(assignments);
        StringBuilder builder = new StringBuilder();
        for (String line : assignmentsAsStrings)
        {
            builder.append(line).append("\n");
        }
        assertEquals(builder.toString(), expectedAssignmentsAsString);
    }

    private List<String> render(List<RoleAssignment> assignments)
    {
        List<String> assignmentsAsStrings = new ArrayList<>();
        for (RoleAssignment roleAssignment : assignments)
        {
            String asString = asString(roleAssignment);
            assignmentsAsStrings.add(asString);
        }
        Collections.sort(assignmentsAsStrings);
        return assignmentsAsStrings;
    }

    private String asString(RoleAssignment assignment)
    {
        StringBuilder builder = new StringBuilder();
        RoleLevel roleLevel = assignment.getRoleLevel();
        builder.append(assignment.getRole()).append(" ").append(roleLevel);
        switch (roleLevel)
        {
            case SPACE:
                builder.append("[").append(assignment.getSpace().getCode()).append("]");
                break;
            case PROJECT:
                builder.append("[").append(assignment.getProject().getIdentifier()).append("]");
                break;
            case INSTANCE:
                break;
        }
        builder.append(" for ");
        AuthorizationGroup authorizationGroup = assignment.getAuthorizationGroup();
        if (authorizationGroup != null)
        {
            builder.append("group ").append(authorizationGroup.getCode());
        } else
        {
            builder.append("user ").append(assignment.getUser().getUserId());
        }
        return builder.toString();
    }
}
