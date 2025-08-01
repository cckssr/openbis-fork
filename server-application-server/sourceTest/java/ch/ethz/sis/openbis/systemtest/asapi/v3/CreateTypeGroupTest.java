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
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.TypeGroupId;
import ch.systemsx.cisd.common.action.IDelegatedAction;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

@Test
public class CreateTypeGroupTest extends AbstractTest
{

    @Test
    public void testCreateSimpleTypeGroup()
    {
        // Given
        String sessionToken = v3api.login(TEST_USER, PASSWORD);

        TypeGroupCreation newGroup = new TypeGroupCreation();
        newGroup.setName("MY_TYPE_GROUP");
        newGroup.setMetaData(Map.of("key", "value"));
        newGroup.setManagedInternally(false);

        // When
        List<TypeGroupId> groups = v3api.createTypeGroups(sessionToken, Arrays.asList(newGroup));

        // Then
        assertEquals(groups.get(0).getPermId(), newGroup.getName());

        TypeGroupFetchOptions fetchOptions = new TypeGroupFetchOptions();
        fetchOptions.withRegistrator();


        TypeGroup group = v3api.getTypeGroups(sessionToken, groups, fetchOptions).get(groups.get(0));
        assertEquals(group.getName(), newGroup.getName());
        assertEquals(group.getRegistrator().getUserId(), TEST_USER);
        Map<String, String> meta = group.getMetaData();

        assertEquals(meta, Map.of("key", "value"));


        v3api.logout(sessionToken);
    }

    @Test
    public void testCreateInternalTypeGroupAsSystem()
    {
        // Given
        String sessionToken = v3api.loginAsSystem();

        TypeGroupCreation newGroup = new TypeGroupCreation();
        newGroup.setName("INTERNAL_TYPE_GROUP");
        newGroup.setMetaData(Map.of("key", "value"));
        newGroup.setManagedInternally(true);

        // When
        List<TypeGroupId> groups = v3api.createTypeGroups(sessionToken, Arrays.asList(newGroup));

        // Then
        assertEquals(groups.get(0).getPermId(), newGroup.getName());

        TypeGroupFetchOptions fetchOptions = new TypeGroupFetchOptions();
        fetchOptions.withRegistrator();


        TypeGroup group = v3api.getTypeGroups(sessionToken, groups, fetchOptions).get(groups.get(0));
        assertEquals(group.getName(), newGroup.getName());
        assertEquals(group.getRegistrator().getUserId(), SYSTEM_USER);
        Map<String, String> meta = group.getMetaData();
        assertEquals(meta, Map.of("key", "value"));
        assertEquals(group.isManagedInternally(), Boolean.TRUE);


        v3api.logout(sessionToken);
    }

    @Test
    public void testCreateWithNameNull()
    {
        assertUserFailureException(new IDelegatedAction()
        {
            @Override
            public void execute()
            {
                TypeGroupCreation creation = new TypeGroupCreation();

                String sessionToken = v3api.login(TEST_USER, PASSWORD);
                v3api.createTypeGroups(sessionToken, Arrays.asList(creation));

            }
        }, "Name cannot be empty");
    }

    @Test
    public void testCreateInternalTypeGroupWithoutRights()
    {
        assertUserFailureException(new IDelegatedAction()
        {
            @Override
            public void execute()
            {
                TypeGroupCreation creation = new TypeGroupCreation();
                creation.setName("MY_TYPE_GROUP");
                creation.setManagedInternally(true);

                String sessionToken = v3api.login(TEST_USER, PASSWORD);
                v3api.createTypeGroups(sessionToken, Arrays.asList(creation));

            }
        }, "Internal type groups can be managed only by the system user.");
    }




}
