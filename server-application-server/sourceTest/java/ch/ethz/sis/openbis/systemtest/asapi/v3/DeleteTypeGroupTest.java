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
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.create.TypeGroupCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.delete.TypeGroupDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.fetchoptions.TypeGroupFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.ITypeGroupId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.TypeGroupId;
import ch.systemsx.cisd.common.action.IDelegatedAction;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class DeleteTypeGroupTest extends AbstractDeletionTest
{

    @Test
    public void testDeleteEmptyList()
    {
        String sessionToken = v3api.login(TEST_USER, PASSWORD);

        TypeGroupDeletionOptions options = new TypeGroupDeletionOptions();
        options.setReason("It is just a test");

        v3api.deleteTypeGroups(sessionToken, new ArrayList<>(), options);
    }

    @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = ".*Entity ids cannot be null.*")
    public void testDeleteWithNullTypeGroupIds()
    {
        TypeGroupDeletionOptions options = new TypeGroupDeletionOptions();
        options.setReason("It is just a test");

        deleteTypeGroup(TEST_USER, null, options);
    }

    @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = ".*Deletion options cannot be null.*")
    public void testDeleteWithNullOptions()
    {
        TypeGroupCreation creation = new TypeGroupCreation();
        creation.setName("TYPE_GROUP_TO_DELETE");

        TypeGroup before = createTypeGroup(TEST_USER, creation);

        deleteTypeGroup(TEST_USER, before.getId(), null);
    }

    @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = ".*Deletion reason cannot be null.*")
    public void testDeleteWithNullReason()
    {
        TypeGroupCreation creation = new TypeGroupCreation();
        creation.setName("TYPE_GROUP_TO_DELETE");

        TypeGroup before = createTypeGroup(TEST_USER, creation);

        TypeGroupDeletionOptions options = new TypeGroupDeletionOptions();

        deleteTypeGroup(TEST_USER, before.getId(), options);
    }

    @Test
    public void testDeleteWithEmptyTypeGroup()
    {
        TypeGroupCreation creation = new TypeGroupCreation();
        creation.setName("TYPE_GROUP_TO_DELETE");

        TypeGroup before = createTypeGroup(TEST_USER, creation);

        TypeGroupDeletionOptions options = new TypeGroupDeletionOptions();
        options.setReason("It is just a test");

        TypeGroup after = deleteTypeGroup(TEST_USER, before.getId(), options);
        assertNull(after);
    }

    @Test
    public void testDeleteInternalTypeGroupAsSystem()
    {
        TypeGroupCreation creation = new TypeGroupCreation();
        creation.setName("TYPE_GROUP_TO_DELETE_INTERNAL");
        creation.setManagedInternally(true);

        TypeGroup before = createTypeGroup(SYSTEM_USER, creation);

        TypeGroupDeletionOptions options = new TypeGroupDeletionOptions();
        options.setReason("It is just a test");

        TypeGroup after = deleteTypeGroup(SYSTEM_USER, before.getId(), options);
        assertNull(after);
    }

    @Test
    public void testDeleteInternalTypeGroupWithoutRights()
    {
        TypeGroupCreation creation = new TypeGroupCreation();
        creation.setName("TYPE_GROUP_TO_DELETE_INTERNAL_2");
        creation.setManagedInternally(true);

        TypeGroup before = createTypeGroup(SYSTEM_USER, creation);

        TypeGroupDeletionOptions options = new TypeGroupDeletionOptions();
        options.setReason("It is just a test");

        assertUnauthorizedObjectAccessException(new IDelegatedAction()
        {
            @Override
            public void execute()
            {
                deleteTypeGroup(TEST_USER, before.getId(), options);
            }
        }, before.getId());
    }


    private TypeGroup createTypeGroup(String user, TypeGroupCreation creation)
    {
        String sessionToken = SYSTEM_USER.equals(user) ? v3api.loginAsSystem() : v3api.login(user, PASSWORD);

        List<TypeGroupId> permIds = v3api.createTypeGroups(sessionToken, Arrays.asList(creation));

        Map<ITypeGroupId, TypeGroup> map = v3api.getTypeGroups(sessionToken, permIds, new TypeGroupFetchOptions());
        assertEquals(map.size(), 1);

        return map.get(permIds.get(0));
    }

    private TypeGroup deleteTypeGroup(String user, ITypeGroupId id, TypeGroupDeletionOptions options)
    {
        String sessionToken = SYSTEM_USER.equals(user) ? v3api.loginAsSystem() : v3api.login(user, PASSWORD);

        List<ITypeGroupId> ids = null;

        if (id != null)
        {
            ids = Arrays.asList(id);
        }

        v3api.deleteTypeGroups(sessionToken, ids, options);

        Map<ITypeGroupId, TypeGroup> map = v3api.getTypeGroups(sessionToken, ids, new TypeGroupFetchOptions());

        return map.get(id);
    }

}
