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

import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.create.SampleTypeCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.TypeGroup;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.TypeGroupAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.create.TypeGroupAssignmentCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.create.TypeGroupCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.fetchoptions.TypeGroupAssignmentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.fetchoptions.TypeGroupFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.ITypeGroupAssignmentId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.TypeGroupAssignmentId;
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
        newGroup.setCode("MY_TYPE_GROUP");
        newGroup.setMetaData(Map.of("key", "value"));
        newGroup.setManagedInternally(false);

        // When
        List<TypeGroupId> groups = v3api.createTypeGroups(sessionToken, Arrays.asList(newGroup));

        // Then
        assertEquals(groups.get(0).getPermId(), newGroup.getCode());

        TypeGroupFetchOptions fetchOptions = new TypeGroupFetchOptions();
        fetchOptions.withRegistrator();


        TypeGroup group = v3api.getTypeGroups(sessionToken, groups, fetchOptions).get(groups.get(0));
        assertEquals(group.getCode(), newGroup.getCode());
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
        newGroup.setCode("INTERNAL_TYPE_GROUP");
        newGroup.setMetaData(Map.of("key", "value"));
        newGroup.setManagedInternally(true);

        // When
        List<TypeGroupId> groups = v3api.createTypeGroups(sessionToken, Arrays.asList(newGroup));

        // Then
        assertEquals(groups.get(0).getPermId(), newGroup.getCode());

        TypeGroupFetchOptions fetchOptions = new TypeGroupFetchOptions();
        fetchOptions.withRegistrator();


        TypeGroup group = v3api.getTypeGroups(sessionToken, groups, fetchOptions).get(groups.get(0));
        assertEquals(group.getCode(), newGroup.getCode());
        assertEquals(group.getRegistrator().getUserId(), SYSTEM_USER);
        Map<String, String> meta = group.getMetaData();
        assertEquals(meta, Map.of("key", "value"));
        assertEquals(group.isManagedInternally(), Boolean.TRUE);


        v3api.logout(sessionToken);
    }

    @Test
    public void testCreateWithCodeNull()
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
        }, "Code cannot be empty");
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
                creation.setCode("MY_TYPE_GROUP");
                creation.setManagedInternally(true);

                String sessionToken = v3api.login(TEST_USER, PASSWORD);
                v3api.createTypeGroups(sessionToken, Arrays.asList(creation));

            }
        }, "Internal type groups can be managed only by the system user.");
    }

    @Test
    public void testCreateTypeGroupAssignment()
    {
        // Given
        String sessionToken = v3api.login(TEST_USER, PASSWORD);

        TypeGroupCreation newGroup = new TypeGroupCreation();
        newGroup.setCode("MY_TYPE_GROUP_WITH_ASSIGNMENT");
        newGroup.setMetaData(Map.of("key", "value"));
        newGroup.setManagedInternally(false);

        List<TypeGroupId> groups = v3api.createTypeGroups(sessionToken, Arrays.asList(newGroup));

        // Then
        assertEquals(groups.get(0).getPermId(), newGroup.getCode());

        SampleTypeCreation newType = new SampleTypeCreation();
        newType.setCode("SAMPLE_TYPE_WITH_TYPEGROUP");
        newType.setDescription("test");
        newType.setGeneratedCodePrefix("TEST-");
        List<EntityTypePermId> typePermIds = v3api.createSampleTypes(sessionToken, Arrays.asList(newType));

        assertEquals(typePermIds.size(), 1);
        
        TypeGroupAssignmentCreation creation = new TypeGroupAssignmentCreation();
        creation.setTypeGroupId(groups.get(0));
        creation.setSampleTypeId(typePermIds.get(0));

        List<TypeGroupAssignmentId> ids = v3api.createTypeGroupAssignments(sessionToken, Arrays.asList(creation));
        
        //Check if type group assignment is fetched from type group
        TypeGroupFetchOptions fetchOptions = new TypeGroupFetchOptions();
        TypeGroupAssignmentFetchOptions options = fetchOptions.withTypeGroupAssignments();
        options.withSampleType();
        options.withTypeGroup();

        
        TypeGroup group = v3api.getTypeGroups(sessionToken, groups, fetchOptions).get(groups.get(0));
        assertEquals(group.getCode(), newGroup.getCode());
        assertEquals(group.getTypeGroupAssignments().size(), 1);
        TypeGroupAssignment assignment = group.getTypeGroupAssignments().get(0);
        
        assertEquals(assignment.getSampleType().getCode(), newType.getCode());
        assertEquals(assignment.getTypeGroup().getCode(), newGroup.getCode());

        //Check if type group assignment is fetched from sample type
        SampleTypeFetchOptions sampleTypeFetchOptions = new SampleTypeFetchOptions();
        sampleTypeFetchOptions.withTypeGroupAssignmentsUsing(options);
        SampleType sampleType = v3api.getSampleTypes(sessionToken, typePermIds, sampleTypeFetchOptions).get(typePermIds.get(0));
        assertEquals(sampleType.getCode(), newType.getCode());
        assertEquals(sampleType.getTypeGroupAssignments().size(), 1);
        assignment = sampleType.getTypeGroupAssignments().get(0);
        assertEquals(assignment.getSampleType().getCode(), newType.getCode());
        assertEquals(assignment.getTypeGroup().getCode(), newGroup.getCode());

        //Check if type group assignment is fetched from dedicated method
        Map<ITypeGroupAssignmentId, TypeGroupAssignment> assignmentMap = v3api.getTypeGroupAssignments(sessionToken, ids, options);
        assignment = assignmentMap.get(ids.get(0));
        assertEquals(assignment.getSampleType().getCode(), newType.getCode());
        assertEquals(assignment.getTypeGroup().getCode(), newGroup.getCode());


        v3api.logout(sessionToken);
    }




}
