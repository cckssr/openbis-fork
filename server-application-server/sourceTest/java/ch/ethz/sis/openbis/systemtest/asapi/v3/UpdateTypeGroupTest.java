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
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.fetchoptions.TypeGroupFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.ITypeGroupId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.TypeGroupId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.update.TypeGroupUpdate;
import ch.systemsx.cisd.common.action.IDelegatedAction;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Map;

import static org.testng.Assert.assertEquals;

public class UpdateTypeGroupTest extends AbstractTest
{
    @Test(expectedExceptions = UserFailureException.class, expectedExceptionsMessageRegExp = "(?s).*Type group name cannot be null.*")
    public void testUpdateWithTypeGroupIdNull()
    {
        TypeGroupUpdate update = new TypeGroupUpdate();
        updateTypeGroup(TEST_USER, update);
    }

    @Test
    public void testUpdateWithTypeGroupIdNonexistent()
    {
        final ITypeGroupId id = new TypeGroupId("IDONTEXIST");
        assertObjectNotFoundException(new IDelegatedAction()
        {
            @Override
            public void execute()
            {
                TypeGroupUpdate update = new TypeGroupUpdate();
                update.setTypeGroupId(id);
                update.setName("SOME_NEW_NAME");
                updateTypeGroup(TEST_USER, update);
            }
        }, id);
    }

    @Test
    public void testUpdateWithTagUnauthorized()
    {
        final TypeGroupId id = new TypeGroupId( "TYPE-GROUP-INTERNAL");
        assertUnauthorizedObjectAccessException(new IDelegatedAction()
        {
            @Override
            public void execute()
            {
                TypeGroupUpdate update = new TypeGroupUpdate();
                update.setTypeGroupId(id);
                update.setName("SOME_NEW_NAME");
                updateTypeGroup(TEST_SPACE_USER, update);
            }
        }, id);
    }

    @Test
    public void testUpdateWithName()
    {
        String sessionToken = v3api.login(TEST_USER, PASSWORD);

        TypeGroupCreation newGroup = new TypeGroupCreation();
        newGroup.setName("MY_TYPE_GROUP_FOR_UPDATE");
        newGroup.setMetaData(Map.of("key", "value"));
        newGroup.setManagedInternally(false);

        TypeGroupId group = v3api.createTypeGroups(sessionToken, Arrays.asList(newGroup)).get(0);

        TypeGroup before = getTypeGroup(TEST_USER, group);
        assertEquals(before.getName(), "MY_TYPE_GROUP_FOR_UPDATE");

        TypeGroupUpdate update = new TypeGroupUpdate();
        update.setTypeGroupId(before.getId());
        update.setName("MY_TYPE_GROUP_FOR_UPDATE_RENAMED");

        TypeGroup after = updateTypeGroup(TEST_USER, update);

        assertEquals(after.getName(), update.getName().getValue());
    }

    private TypeGroup getTypeGroup(String user, ITypeGroupId id)
    {
        TypeGroupFetchOptions fetchOptions = new TypeGroupFetchOptions();
        fetchOptions.withRegistrator();
        fetchOptions.withModifier();
        fetchOptions.withTypeGroupAssignments();

        String sessionToken = SYSTEM_USER.equals(user) ? v3api.loginAsSystem() :  v3api.login(user, PASSWORD);
        Map<ITypeGroupId, TypeGroup> tags = v3api.getTypeGroups(sessionToken, Arrays.asList(id), fetchOptions);
        return tags.get(id);
    }

    private TypeGroup updateTypeGroup(String user, TypeGroupUpdate update)
    {
        String sessionToken = SYSTEM_USER.equals(user) ? v3api.loginAsSystem() :  v3api.login(user, PASSWORD);

        v3api.updateTypeGroups(sessionToken, Arrays.asList(update));

        return getTypeGroup(user, new TypeGroupId(update.getName().getValue()));
    }
}
