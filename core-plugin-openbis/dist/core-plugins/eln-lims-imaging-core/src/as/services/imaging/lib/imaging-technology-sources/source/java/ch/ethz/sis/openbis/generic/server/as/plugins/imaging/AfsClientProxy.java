package ch.ethz.sis.openbis.generic.server.as.plugins.imaging;

import java.net.URI;
import java.nio.file.Path;
import java.util.UUID;

import ch.ethz.sis.afsapi.api.ClientAPI;
import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afsclient.client.AfsClient;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.DataStore;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.fetchoptions.DataStoreFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.search.DataStoreKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.search.DataStoreSearchCriteria;
import ch.ethz.sis.openbis.generic.server.asapi.v3.IApplicationServerInternalApi;
import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.ethz.sis.shared.log.classic.impl.Logger;
import ch.systemsx.cisd.common.spring.ExposablePropertyPlaceholderConfigurer;
import ch.systemsx.cisd.openbis.dss.generic.shared.ServiceProvider;
import ch.systemsx.cisd.openbis.generic.server.CommonServiceProvider;

final class AfsClientProxy
{
    private UUID transactionId;

    private final AfsClient client;

    private static final Logger
            operationLog = LogFactory.getLogger(LogCategory.OPERATION, AfsClientProxy.class);

    private final ClientAPI.FileCollisionListener overrideCollisionListener = new ClientAPI.FileCollisionListener() {
        @Override
        public ClientAPI.CollisionAction precheck(Path sourcePath,Path destinationPath, boolean collision) {
            return ClientAPI.CollisionAction.Override;
        }
    };

    private AfsClientProxy(AfsClient client) {
        this.client = client;
    }

    private static String tryToGetProperty(String propertyName, String defaultValue) {
        return ((ExposablePropertyPlaceholderConfigurer) ServiceProvider.getApplicationContext()
                .getBean("propertyConfigurer"))
                .getPropertyValue(propertyName, defaultValue);
    }

    public static AfsClientProxy getAfsClient(String sessionToken, int timeout) {
        IApplicationServerInternalApi applicationServerApi = CommonServiceProvider.getApplicationServerApi();

        DataStoreSearchCriteria dataStoreSearchCriteria = new DataStoreSearchCriteria();
        dataStoreSearchCriteria.withKind().thatIn(DataStoreKind.AFS);

        SearchResult<DataStore> dataStoreSearchResult =
                applicationServerApi.searchDataStores(sessionToken, dataStoreSearchCriteria, new DataStoreFetchOptions());
        String url = dataStoreSearchResult.getObjects().isEmpty() ? null : dataStoreSearchResult.getObjects().getFirst().getDownloadUrl();

        if(url != null && !url.isBlank()) {
            operationLog.info("Resolved AFS server URL: " + url);
//            String interactiveSessionKey = CommonServiceProvider.tryToGetProperty(INTERACTIVE_SESSION_KEY_PROPERTY_NAME);
//            if(interactiveSessionKey == null || interactiveSessionKey.isBlank()) {
//                throw new IllegalStateException("Interactive Session Key is not configured!");
//            }
//            AfsClient client = getAfsClient(sessionToken, url, timeout, interactiveSessionKey);
            AfsClient client = getAfsClient(sessionToken, url, timeout);

            return new AfsClientProxy(client);
        } else {
            operationLog.info("Could not resolve AFS server URL");
            return new AfsClientProxy(null);
        }
    }

    private static AfsClient getAfsClient(String sessionToken, String afsServerUrl, int timeoutInSeconds)
    {
        AfsClient afsClient = new AfsClient(URI.create(afsServerUrl), timeoutInSeconds * 1000);
        afsClient.setSessionToken(sessionToken);
        //        afsClient.setInteractiveSessionKey(interactiveSessionKey);
        return afsClient;
    }


    public File[] listFiles(String permId) {
        try {
            return client.list(permId, "", true);
        } catch (Exception e)
        {
            if(e.toString().contains("NoSuchFileException")) {
                return new File[0];
            }
            throw new RuntimeException(e);
        }
    }


    public boolean isSessionValid() {
        if(client == null) {
            return false;
        }
        try {
            return client.isSessionValid();
        } catch (Exception e)
        {
            return false;
        }
    }

    public byte[] downloadFileChunk(String permId, String path, long offset, int limit) {
        try
        {
            return client.read(permId, path, offset, limit);
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public void downloadEntityFiles(String permId, Path destination) {
        try
        {
            client.download(permId, Path.of("/"), destination,
                    overrideCollisionListener,
                    new ClientAPI.DefaultTransferMonitorLister());
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }

    }
}
