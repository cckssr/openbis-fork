package ch.ethz.sis.openbis.afsserver.server.common;

import java.util.List;
import java.util.Properties;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ch.ethz.sis.afsserver.AbstractTest;
import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameter;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.DataStore;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.fetchoptions.DataStoreFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.search.DataStoreSearchCriteria;
import ch.ethz.sis.shared.startup.Configuration;
import ch.systemsx.cisd.common.exceptions.ConfigurationFailureException;

public class OpenBISConfigurationTest extends AbstractTest
{

    private static final String OPENBIS_URL = "test-openbis-url";

    private static final String OPENBIS_USER = "test-openbis-user";

    private static final String OPENBIS_PASSWORD = "test-openbis-password";

    private static final Integer OPENBIS_TIMEOUT = 12345;

    private static final String OPENBIS_LAST_SEEN_DELETION_FILE = "test-openbis-last-seen-deletion-file";

    private static final Integer OPENBIS_LAST_SEEN_DELETION_BATCH_SIZE = 100;

    private static final Integer OPENBIS_LAST_SEEN_DELETION_INTERVAL = 5;

    private static final String STORAGE_UUID_1 = "test-storage-uuid-1";

    private static final String STORAGE_UUID_2 = "test-storage-uuid-2";

    private Mockery context;

    @Before
    public void beforeMethod()
    {
        context = new Mockery();
    }

    @After
    public void afterMethod()
    {
        context.assertIsSatisfied();
    }

    @Test
    public void testUuidNotConfiguredAtAll()
    {
        try
        {
            OpenBISConfiguration openBISConfiguration = testStorageUuid(null, List.of());
            openBISConfiguration.getStorageUuid();
            Assert.fail();
        } catch (ConfigurationFailureException e)
        {
            Assert.assertEquals(
                    "Storage UUID configuration is missing. The storage UUID hasn't been set in the AFS service.properties. Moreover, no DSS servers were found to perform automatic storage UUID configuration.",
                    e.getMessage());
        }
    }

    @Test
    public void testUuidConfiguredOnlyAtAFS()
    {
        OpenBISConfiguration openBISConfiguration = testStorageUuid(STORAGE_UUID_1, List.of());
        Assert.assertEquals(STORAGE_UUID_1, openBISConfiguration.getStorageUuid());
    }

    @Test
    public void testUuidConfiguredOnlyAtDSS()
    {
        OpenBISConfiguration openBISConfiguration = testStorageUuid(null, List.of(STORAGE_UUID_1));
        Assert.assertEquals(STORAGE_UUID_1, openBISConfiguration.getStorageUuid());
    }

    @Test
    public void testUuidConfiguredOnlyAtDSSesToTheSameValue()
    {
        OpenBISConfiguration openBISConfiguration = testStorageUuid(null, List.of(STORAGE_UUID_1, STORAGE_UUID_1));
        Assert.assertEquals(STORAGE_UUID_1, openBISConfiguration.getStorageUuid());
    }

    @Test
    public void testUuidConfiguredOnlyAtDSSesToDifferentValues()
    {
        try
        {
            OpenBISConfiguration configuration = testStorageUuid(null, List.of(STORAGE_UUID_1, STORAGE_UUID_2));
            configuration.getStorageUuid();
        } catch (ConfigurationFailureException e)
        {
            Assert.assertEquals(
                    "Storage UUID configuration is missing. The storage UUID hasn't been set in the AFS service.properties. Moreover, an automatic storage UUID configuration is not possible as there are multiple different storage UUID values used among DSS servers ([test-storage-uuid-1, test-storage-uuid-2]).",
                    e.getMessage());
        }
    }

    @Test
    public void testUuidConfiguredAtBothAFSAndDSSToTheSameValue()
    {
        OpenBISConfiguration openBISConfiguration = testStorageUuid(STORAGE_UUID_1, List.of(STORAGE_UUID_1));
        Assert.assertEquals(STORAGE_UUID_1, openBISConfiguration.getStorageUuid());
    }

    @Test
    public void testUuidConfiguredAtBothAFSAndDSSToDifferentValues()
    {
        try
        {
            OpenBISConfiguration openBISConfiguration = testStorageUuid(STORAGE_UUID_1, List.of(STORAGE_UUID_2));
            openBISConfiguration.getStorageUuid();
            Assert.fail();
        } catch (ConfigurationFailureException e)
        {
            Assert.assertEquals(
                    "Storage UUID configuration is incorrect. The storage UUID defined in AFS service.properties (test-storage-uuid-1) is different than the storage UUID(s) found at the DSS server(s) ([test-storage-uuid-2]).",
                    e.getMessage());
        }
    }

    private OpenBISConfiguration testStorageUuid(String afsStorageUuid, List<String> dssStorageUuids)
    {
        IOpenBISFacadeFactory openBISFacadeFactory = context.mock(IOpenBISFacadeFactory.class);
        IOpenBISFacade openBISFacade = context.mock(IOpenBISFacade.class);

        context.checking(new Expectations()
        {
            {
                one(openBISFacadeFactory).createFacade(OPENBIS_URL, OPENBIS_USER, OPENBIS_PASSWORD, OPENBIS_TIMEOUT);
                will(returnValue(openBISFacade));

                List<DataStore> dataStores = dssStorageUuids.stream().map(dssStorageUuid ->
                {
                    DataStore dataStore = new DataStore();
                    dataStore.setStorageUuid(dssStorageUuid);
                    return dataStore;
                }).toList();

                SearchResult<DataStore> dataStoreSearchResult = new SearchResult<>(dataStores, dataStores.size());

                one(openBISFacade).searchDataStores(with(any(DataStoreSearchCriteria.class)), with(any(DataStoreFetchOptions.class)));
                will(returnValue(dataStoreSearchResult));
            }
        });

        Properties properties = new Properties();
        properties.setProperty(OpenBISConfiguration.OpenBISParameter.openBISUrl.name(), OPENBIS_URL);
        properties.setProperty(OpenBISConfiguration.OpenBISParameter.openBISUser.name(), OPENBIS_USER);
        properties.setProperty(OpenBISConfiguration.OpenBISParameter.openBISPassword.name(), OPENBIS_PASSWORD);
        properties.setProperty(OpenBISConfiguration.OpenBISParameter.openBISTimeout.name(), String.valueOf(OPENBIS_TIMEOUT));
        properties.setProperty(OpenBISConfiguration.OpenBISParameter.openBISLastSeenDeletionFile.name(), OPENBIS_LAST_SEEN_DELETION_FILE);
        properties.setProperty(OpenBISConfiguration.OpenBISParameter.openBISLastSeenDeletionBatchSize.name(),
                String.valueOf(OPENBIS_LAST_SEEN_DELETION_BATCH_SIZE));
        properties.setProperty(OpenBISConfiguration.OpenBISParameter.openBISLastSeenDeletionIntervalInSeconds.name(),
                String.valueOf(OPENBIS_LAST_SEEN_DELETION_INTERVAL));

        if (afsStorageUuid != null)
        {
            properties.setProperty(AtomicFileSystemServerParameter.storageUuid.name(), afsStorageUuid);
        }

        Configuration configuration = new Configuration(properties);
        OpenBISConfiguration openBISConfiguration = OpenBISConfiguration.getInstance(configuration, openBISFacadeFactory);

        return openBISConfiguration;
    }

}
