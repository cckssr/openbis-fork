/*
 *  Copyright ETH 2009 - 2025 Zürich, Scientific IT Services
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
package ch.systemsx.cisd.openbis.generic.server.dataaccess.db;

import ch.systemsx.cisd.openbis.generic.shared.basic.dto.Grantee;
import ch.systemsx.cisd.openbis.generic.shared.dto.*;
import ch.systemsx.cisd.openbis.generic.shared.dto.identifier.ProjectIdentifier;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import ch.systemsx.cisd.openbis.generic.shared.basic.dto.RoleWithHierarchy.RoleCode;

import java.util.Date;

/**
 * Test cases for {@link RoleAssignmentDAO}.
 * 
 * @author Izabela Adamczyk
 */
@Test(groups =
{ "db", "role_assignment" })
public class RoleAssignmentDAOTest extends AbstractDAOTest
{

    private static final String USER_ID = "geralt";

    private static final String AUTH_GROUP_ID = "rivia";

    public void testAddGroupAdminRoleToUser() throws Exception
    {
        String userId = USER_ID;
        PersonPE user = createUserInDB(userId);
        AssertJUnit.assertEquals(0, daoFactory.getPersonDAO().tryFindPersonByUserId(userId)
                .getRoleAssignments().size());
        AssertJUnit.assertEquals(0, daoFactory.getRoleAssignmentDAO().listRoleAssignmentsByPerson(
                user).size());

        SpacePE group = daoFactory.getSpaceDAO().listSpaces().get(0);
        RoleAssignmentPE roleAssignment = new RoleAssignmentPE();
        roleAssignment.setRole(RoleCode.ADMIN);
        roleAssignment.setSpace(group);
        roleAssignment.setRegistrator(getSystemPerson());

        user.addRoleAssignment(roleAssignment);

        daoFactory.getRoleAssignmentDAO().createRoleAssignment(roleAssignment);
        AssertJUnit.assertEquals(1, daoFactory.getRoleAssignmentDAO().listRoleAssignmentsByPerson(
                user).size());
    }

    public void testAddGroupAdminRoleWithExpirationToUser() throws Exception
    {
        String userId = USER_ID;
        PersonPE user = createUserInDB(userId);
        AssertJUnit.assertEquals(0, daoFactory.getPersonDAO().tryFindPersonByUserId(userId)
                .getRoleAssignments().size());
        AssertJUnit.assertEquals(0, daoFactory.getRoleAssignmentDAO().listRoleAssignmentsByPerson(
                user).size());

        SpacePE group = daoFactory.getSpaceDAO().listSpaces().get(0);
        RoleAssignmentPE roleAssignment = new RoleAssignmentPE();
        roleAssignment.setRole(RoleCode.ADMIN);
        roleAssignment.setSpace(group);
        roleAssignment.setRegistrator(getSystemPerson());
        //no expiry date
        user.addRoleAssignment(roleAssignment);

        daoFactory.getRoleAssignmentDAO().createRoleAssignment(roleAssignment);
        AssertJUnit.assertEquals(1, daoFactory.getRoleAssignmentDAO().listRoleAssignmentsByPerson(
                user).size());

        // set expiry date in the past
        roleAssignment.setExpiryDate(new Date(0L));
        daoFactory.getRoleAssignmentDAO().createRoleAssignment(roleAssignment);
        AssertJUnit.assertEquals(0, daoFactory.getRoleAssignmentDAO().listRoleAssignmentsByPerson(
                user).size());

        // set expiry date in the future
        roleAssignment.setExpiryDate(new Date(System.currentTimeMillis() + 1000000L));
        daoFactory.getRoleAssignmentDAO().createRoleAssignment(roleAssignment);
        AssertJUnit.assertEquals(1, daoFactory.getRoleAssignmentDAO().listRoleAssignmentsByPerson(
                user).size());
    }

    public void testListRoleAssignmentsWithExpiration() throws Exception
    {
        String userId = USER_ID;
        PersonPE user = createUserInDB(userId);
        AssertJUnit.assertEquals(0, daoFactory.getPersonDAO().tryFindPersonByUserId(userId)
                .getRoleAssignments().size());
        AssertJUnit.assertEquals(0, daoFactory.getRoleAssignmentDAO().listRoleAssignmentsByPerson(
                user).size());

        int baseNumber = daoFactory.getRoleAssignmentDAO().listRoleAssignments().size();

        SpacePE group = daoFactory.getSpaceDAO().listSpaces().get(0);
        RoleAssignmentPE roleAssignment = new RoleAssignmentPE();
        roleAssignment.setRole(RoleCode.ADMIN);
        roleAssignment.setSpace(group);
        roleAssignment.setRegistrator(getSystemPerson());
        //no expiry date
        user.addRoleAssignment(roleAssignment);

        daoFactory.getRoleAssignmentDAO().createRoleAssignment(roleAssignment);
        AssertJUnit.assertEquals(baseNumber+1, daoFactory.getRoleAssignmentDAO().listRoleAssignments().size());

        // set expiry date in the past
        roleAssignment.setExpiryDate(new Date(0L));
        daoFactory.getRoleAssignmentDAO().createRoleAssignment(roleAssignment);
        AssertJUnit.assertEquals(baseNumber, daoFactory.getRoleAssignmentDAO().listRoleAssignments().size());


        // set expiry date in the future
        roleAssignment.setExpiryDate(new Date(System.currentTimeMillis() + 1000000L));
        daoFactory.getRoleAssignmentDAO().createRoleAssignment(roleAssignment);
        AssertJUnit.assertEquals(baseNumber+1, daoFactory.getRoleAssignmentDAO().listRoleAssignments().size());

    }

    public void testTryFindSpaceRoleAssignmentExpiration() throws Exception
    {
        String userId = USER_ID;
        PersonPE user = createUserInDB(userId);
        AssertJUnit.assertEquals(0, daoFactory.getPersonDAO().tryFindPersonByUserId(userId)
                .getRoleAssignments().size());
        AssertJUnit.assertEquals(0, daoFactory.getRoleAssignmentDAO().listRoleAssignmentsByPerson(
                user).size());


        SpacePE group = daoFactory.getSpaceDAO().listSpaces().get(0);

        RoleAssignmentPE role = daoFactory.getRoleAssignmentDAO().tryFindSpaceRoleAssignment(RoleCode.ADMIN, group.getCode(),
                Grantee.createPerson(user.getUserId()));
        AssertJUnit.assertNull(role);


        RoleAssignmentPE roleAssignment = new RoleAssignmentPE();
        roleAssignment.setRole(RoleCode.ADMIN);
        roleAssignment.setSpace(group);
        roleAssignment.setRegistrator(getSystemPerson());
        //no expiry date
        user.addRoleAssignment(roleAssignment);

        daoFactory.getRoleAssignmentDAO().createRoleAssignment(roleAssignment);
        AssertJUnit.assertNotNull(daoFactory.getRoleAssignmentDAO().tryFindSpaceRoleAssignment(RoleCode.ADMIN, group.getCode(),
                Grantee.createPerson(user.getUserId())));

        // set expiry date in the past
        roleAssignment.setExpiryDate(new Date(0L));
        daoFactory.getRoleAssignmentDAO().createRoleAssignment(roleAssignment);
        AssertJUnit.assertNull(daoFactory.getRoleAssignmentDAO().tryFindSpaceRoleAssignment(RoleCode.ADMIN, group.getCode(),
                Grantee.createPerson(user.getUserId())));

        // set expiry date in the future
        roleAssignment.setExpiryDate(new Date(System.currentTimeMillis() + 1000000L));
        daoFactory.getRoleAssignmentDAO().createRoleAssignment(roleAssignment);
        AssertJUnit.assertNotNull(daoFactory.getRoleAssignmentDAO().tryFindSpaceRoleAssignment(RoleCode.ADMIN, group.getCode(),
                Grantee.createPerson(user.getUserId())));
    }

    public void testTryFindProjectRoleAssignmentExpiration() throws Exception
    {
        String userId = USER_ID;
        PersonPE user = createUserInDB(userId);
        AssertJUnit.assertEquals(0, daoFactory.getPersonDAO().tryFindPersonByUserId(userId)
                .getRoleAssignments().size());
        AssertJUnit.assertEquals(0, daoFactory.getRoleAssignmentDAO().listRoleAssignmentsByPerson(
                user).size());


        ProjectPE project = daoFactory.getProjectDAO().listProjects().get(0);
        ProjectIdentifier identifier = new ProjectIdentifier(project.getSpace().getCode(),
                project.getCode());

        RoleAssignmentPE role = daoFactory.getRoleAssignmentDAO().tryFindProjectRoleAssignment(RoleCode.ADMIN, identifier,
                Grantee.createPerson(user.getUserId()));
        AssertJUnit.assertNull(role);


        RoleAssignmentPE roleAssignment = new RoleAssignmentPE();
        roleAssignment.setRole(RoleCode.ADMIN);
        roleAssignment.setProject(project);
        roleAssignment.setRegistrator(getSystemPerson());
        //no expiry date
        user.addRoleAssignment(roleAssignment);

        daoFactory.getRoleAssignmentDAO().createRoleAssignment(roleAssignment);
        AssertJUnit.assertNotNull(daoFactory.getRoleAssignmentDAO().tryFindProjectRoleAssignment(RoleCode.ADMIN, identifier,
                Grantee.createPerson(user.getUserId())));

        // set expiry date in the past
        roleAssignment.setExpiryDate(new Date(0L));
        daoFactory.getRoleAssignmentDAO().createRoleAssignment(roleAssignment);
        AssertJUnit.assertNull(daoFactory.getRoleAssignmentDAO().tryFindProjectRoleAssignment(RoleCode.ADMIN, identifier,
                Grantee.createPerson(user.getUserId())));

        // set expiry date in the future
        roleAssignment.setExpiryDate(new Date(System.currentTimeMillis() + 1000000L));
        daoFactory.getRoleAssignmentDAO().createRoleAssignment(roleAssignment);
        AssertJUnit.assertNotNull(daoFactory.getRoleAssignmentDAO().tryFindProjectRoleAssignment(RoleCode.ADMIN, identifier,
                Grantee.createPerson(user.getUserId())));
    }

    public void testTryFindInstanceRoleAssignmentExpiration() throws Exception
    {
        String userId = USER_ID;
        PersonPE user = createUserInDB(userId);
        AssertJUnit.assertEquals(0, daoFactory.getPersonDAO().tryFindPersonByUserId(userId)
                .getRoleAssignments().size());
        AssertJUnit.assertEquals(0, daoFactory.getRoleAssignmentDAO().listRoleAssignmentsByPerson(
                user).size());


        RoleAssignmentPE roleAssignment = new RoleAssignmentPE();
        roleAssignment.setRole(RoleCode.ADMIN);
        roleAssignment.setRegistrator(getSystemPerson());
        //no expiry date
        user.addRoleAssignment(roleAssignment);

        daoFactory.getRoleAssignmentDAO().createRoleAssignment(roleAssignment);
        AssertJUnit.assertNotNull(daoFactory.getRoleAssignmentDAO().tryFindInstanceRoleAssignment(RoleCode.ADMIN,
                Grantee.createPerson(user.getUserId())));

        // set expiry date in the past
        roleAssignment.setExpiryDate(new Date(0L));
        daoFactory.getRoleAssignmentDAO().createRoleAssignment(roleAssignment);
        AssertJUnit.assertNull(daoFactory.getRoleAssignmentDAO().tryFindInstanceRoleAssignment(RoleCode.ADMIN,
                Grantee.createPerson(user.getUserId())));

        // set expiry date in the future
        roleAssignment.setExpiryDate(new Date(System.currentTimeMillis() + 1000000L));
        daoFactory.getRoleAssignmentDAO().createRoleAssignment(roleAssignment);
        AssertJUnit.assertNotNull(daoFactory.getRoleAssignmentDAO().tryFindInstanceRoleAssignment(RoleCode.ADMIN,
                Grantee.createPerson(user.getUserId())));
    }

    public void testAddGroupAdminRoleToAuthorizationGroup() throws Exception
    {
        String code = AUTH_GROUP_ID;
        AuthorizationGroupPE authGroup = createAuthGroupInDB(code);
        AssertJUnit.assertEquals(0, daoFactory.getRoleAssignmentDAO()
                .listRoleAssignmentsByAuthorizationGroup(authGroup).size());

        SpacePE group = daoFactory.getSpaceDAO().listSpaces().get(0);
        RoleAssignmentPE roleAssignment = new RoleAssignmentPE();
        roleAssignment.setRole(RoleCode.ADMIN);
        roleAssignment.setSpace(group);
        roleAssignment.setRegistrator(getSystemPerson());

        authGroup.addRoleAssignment(roleAssignment);

        daoFactory.getRoleAssignmentDAO().createRoleAssignment(roleAssignment);
        AssertJUnit.assertEquals(1, daoFactory.getRoleAssignmentDAO()
                .listRoleAssignmentsByAuthorizationGroup(authGroup).size());
    }

    public void testAddGroupAdminRoleWithExpirationToAuthorizationGroup() throws Exception
    {
        String code = AUTH_GROUP_ID;
        AuthorizationGroupPE authGroup = createAuthGroupInDB(code);
        AssertJUnit.assertEquals(0, daoFactory.getRoleAssignmentDAO()
                .listRoleAssignmentsByAuthorizationGroup(authGroup).size());

        SpacePE group = daoFactory.getSpaceDAO().listSpaces().get(0);
        RoleAssignmentPE roleAssignment = new RoleAssignmentPE();
        roleAssignment.setRole(RoleCode.ADMIN);
        roleAssignment.setSpace(group);
        roleAssignment.setRegistrator(getSystemPerson());
        //no expiry date

        authGroup.addRoleAssignment(roleAssignment);

        daoFactory.getRoleAssignmentDAO().createRoleAssignment(roleAssignment);
        AssertJUnit.assertEquals(1, daoFactory.getRoleAssignmentDAO()
                .listRoleAssignmentsByAuthorizationGroup(authGroup).size());

        // set expiry date in the past
        roleAssignment.setExpiryDate(new Date(0L));
        daoFactory.getRoleAssignmentDAO().createRoleAssignment(roleAssignment);
        AssertJUnit.assertEquals(0, daoFactory.getRoleAssignmentDAO()
                .listRoleAssignmentsByAuthorizationGroup(authGroup).size());

        // set expiry date in the future
        roleAssignment.setExpiryDate(new Date(System.currentTimeMillis() + 1000000L));
        AssertJUnit.assertEquals(1, daoFactory.getRoleAssignmentDAO()
                .listRoleAssignmentsByAuthorizationGroup(authGroup).size());
    }

    private AuthorizationGroupPE createAuthGroupInDB(String authGroupCode)
    {
        AuthorizationGroupPE group = new AuthorizationGroupPE();
        group.setCode(authGroupCode);
        group.setDescription("Rivia users");
        group.setRegistrator(getSystemPerson());
        daoFactory.getAuthorizationGroupDAO().create(group);
        return daoFactory.getAuthorizationGroupDAO().tryFindByCode(authGroupCode);
    }

    private PersonPE createUserInDB(String userId)
    {
        PersonPE person = new PersonPE();
        person.setRegistrator(getSystemPerson());
        person.setUserId(userId);
        person.setEmail("geralt@rivia.net");
        person.setFirstName("Geralt");
        person.setLastName("Of Rivia");
        daoFactory.getPersonDAO().createPerson(person);
        return daoFactory.getPersonDAO().tryFindPersonByUserId(userId);
    }

}
