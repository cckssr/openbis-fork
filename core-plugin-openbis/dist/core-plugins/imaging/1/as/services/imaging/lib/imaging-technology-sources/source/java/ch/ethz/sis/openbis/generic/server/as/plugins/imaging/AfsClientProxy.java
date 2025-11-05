package ch.ethz.sis.openbis.generic.server.as.plugins.imaging;

import ch.ethz.sis.afsapi.api.ClientAPI;
import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afsclient.client.AfsClient;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.common.spring.ExposablePropertyPlaceholderConfigurer;
import ch.systemsx.cisd.openbis.dss.generic.shared.ServiceProvider;
import ch.systemsx.cisd.openbis.generic.server.CommonServiceProvider;

import java.net.URI;
import java.nio.file.Path;
import java.util.UUID;

final class AfsClientProxy
{
    public static final String AFS_SERVER_URL_PROPERTY_NAME = "server-public-information.afs-server.url";

    public static final String AFS_SERVER_TIMEOUT_PROPERTY_NAME = "server-public-information.afs-server.timeout";

    public static final String AFS_SERVER_TIMEOUT_DEFAULT = "3600";

    private UUID transactionId;

    private final AfsClient client;

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

    public static AfsClientProxy getAfsClient(String sessionToken) {


        String url = CommonServiceProvider.tryToGetProperty(AFS_SERVER_URL_PROPERTY_NAME);
        if(url != null && !url.isBlank()) {
            String timeoutStr = CommonServiceProvider.tryToGetProperty(AFS_SERVER_TIMEOUT_PROPERTY_NAME, AFS_SERVER_TIMEOUT_DEFAULT);
//            String interactiveSessionKey = CommonServiceProvider.tryToGetProperty(INTERACTIVE_SESSION_KEY_PROPERTY_NAME);
//            if(interactiveSessionKey == null || interactiveSessionKey.isBlank()) {
//                throw new IllegalStateException("Interactive Session Key is not configured!");
//            }
            final int timeout = Integer.parseInt(timeoutStr);
//            AfsClient client = getAfsClient(sessionToken, url, timeout, interactiveSessionKey);
            AfsClient client = getAfsClient(sessionToken, url, timeout);

            return new AfsClientProxy(client);
        } else {
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
            // TODO files not found vs regular exception
            int a = 1;
            return new File[0];
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
