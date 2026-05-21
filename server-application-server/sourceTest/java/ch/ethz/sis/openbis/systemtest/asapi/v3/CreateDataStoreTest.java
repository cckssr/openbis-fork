/*
 * Copyright ETH 2015 - 2023 Zürich, Scientific IT Services
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

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.testng.annotations.Test;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.DataStore;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.create.DataStoreCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.fetchoptions.DataStoreFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.id.DataStorePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.search.DataStoreSearchCriteria;
import ch.systemsx.cisd.common.action.IDelegatedAction;

/**
 * @author pkupczyk
 */
public class CreateDataStoreTest extends AbstractTest
{

    @Test
    public void testCreateWithCodeNull()
    {
        final String sessionToken = v3api.login(TEST_USER, PASSWORD);

        final DataStoreCreation dataStore = new DataStoreCreation();

        assertUserFailureException(new IDelegatedAction()
        {
            @Override
            public void execute()
            {
                v3api.createDataStores(sessionToken, Arrays.asList(dataStore));
            }
        }, "Code cannot be empty");
    }

    @Test
    public void testCreateWithCodeExisting()
    {
        final String sessionToken = v3api.login(TEST_USER, PASSWORD);

        DataStoreCreation creation = new DataStoreCreation();
        creation.setCode("TEST_" + UUID.randomUUID().toString().toUpperCase());
        creation.setDownloadUrl("download url");
        creation.setRemoteUrl("remote url");
        creation.setStorageUuid("storage uuid");
        DataStorePermId dataStoreId = v3api.createDataStores(sessionToken, List.of(creation)).getFirst();

        DataStoreCreation creation2 = new DataStoreCreation();
        creation2.setCode(creation.getCode());
        creation2.setDownloadUrl("download url 2");
        creation2.setRemoteUrl("remote url 2");
        creation2.setStorageUuid("storage uuid 2");
        DataStorePermId dataStoreId2 = v3api.createDataStores(sessionToken, List.of(creation2)).getFirst();

        assertEquals(dataStoreId, dataStoreId2);

        DataStore dataStore2 = getDataStore(sessionToken, dataStoreId2);
        assertEquals(dataStore2.getCode(), creation.getCode());
        assertEquals(dataStore2.getDownloadUrl(), creation2.getDownloadUrl());
        assertEquals(dataStore2.getRemoteUrl(), creation2.getRemoteUrl());
        assertEquals(dataStore2.getStorageUuid(), creation2.getStorageUuid());
    }

    @Test
    public void testCreateWithInstanceAdmin()
    {
        testCreateWithUser(TEST_USER);
    }

    @Test
    public void testCreateWithSpaceAdmin()
    {
        assertUnauthorizedObjectAccessException(() -> testCreateWithUser(TEST_GROUP_ADMIN), new DataStorePermId("TEST_DATA_STORE"));
    }

    @Test
    public void testCreateWithETLServer()
    {
        testCreateWithUser(TEST_INSTANCE_ETLSERVER);
    }

    @Test
    public void testLogging()
    {
        String sessionToken = v3api.login(TEST_USER, PASSWORD);

        DataStoreCreation creation = new DataStoreCreation();
        creation.setCode("LOG_TEST_1");

        DataStoreCreation creation2 = new DataStoreCreation();
        creation2.setCode("LOG_TEST_2");

        v3api.createDataStores(sessionToken, Arrays.asList(creation, creation2));

        assertAccessLog("create-data-stores  NEW_DATA_STORES('[DataStoreCreation[code=LOG_TEST_1], DataStoreCreation[code=LOG_TEST_2]]')");
    }

    private void testCreateWithUser(String userId)
    {
        final String sessionToken = v3api.login(userId, PASSWORD);

        DataStoreCreation creation = new DataStoreCreation();
        creation.setCode("TEST_DATA_STORE");
        creation.setDownloadUrl("test download url");
        creation.setRemoteUrl("test remote url");
        creation.setStorageUuid("test storage uuid");

        List<DataStorePermId> permIds = v3api.createDataStores(sessionToken, List.of(creation));

        DataStoreSearchCriteria criteria = new DataStoreSearchCriteria();
        criteria.withId().thatEquals(permIds.getFirst());
        SearchResult<DataStore> searchResult = v3api.searchDataStores(sessionToken, criteria, new DataStoreFetchOptions());

        assertEquals(searchResult.getObjects().size(), 1);

        DataStore dataStore = searchResult.getObjects().getFirst();
        assertEquals(dataStore.getCode(), creation.getCode());
        assertEquals(dataStore.getDownloadUrl(), creation.getDownloadUrl());
        assertEquals(dataStore.getRemoteUrl(), creation.getRemoteUrl());
        assertEquals(dataStore.getStorageUuid(), creation.getStorageUuid());
    }

    private DataStore getDataStore(String sessionToken, DataStorePermId dataStoreId)
    {
        DataStoreSearchCriteria criteria = new DataStoreSearchCriteria();
        criteria.withId().thatEquals(dataStoreId);
        SearchResult<DataStore> searchResult = v3api.searchDataStores(sessionToken, criteria, new DataStoreFetchOptions());
        return searchResult.getObjects().isEmpty() ? null : searchResult.getObjects().getFirst();
    }

}
