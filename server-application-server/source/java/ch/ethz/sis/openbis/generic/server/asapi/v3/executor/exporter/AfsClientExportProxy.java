package ch.ethz.sis.openbis.generic.server.asapi.v3.executor.exporter;

import ch.ethz.sis.afsapi.api.ClientAPI;
import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afsclient.client.AfsClient;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.openbis.generic.server.CommonServiceProvider;

import java.net.URI;
import java.nio.file.Path;

final class AfsClientExportProxy
{

    public static final String AFS_SERVER_URL_PROPERTY_NAME = "server-public-information.afs-server.url";

    public static final String AFS_SERVER_TIMEOUT_PROPERTY_NAME = "server-public-information.afs-server.timeout";

    public static final String AFS_SERVER_TIMEOUT_DEFAULT = "3600";

    private final AfsClient client;

    private final ClientAPI.FileCollisionListener overrideCollisionListener = new ClientAPI.FileCollisionListener() {
        @Override
        public ClientAPI.CollisionAction precheck(Path sourcePath,Path destinationPath, boolean collision) {
            return ClientAPI.CollisionAction.Override;
        }
    };

    private AfsClientExportProxy(AfsClient client) {
        this.client = client;
    }


    public static AfsClientExportProxy getAfsClient(String sessionToken) {
        String url = CommonServiceProvider.tryToGetProperty(AFS_SERVER_URL_PROPERTY_NAME);
        if(url != null && !url.isBlank()) {
            String timeoutStr = CommonServiceProvider.tryToGetProperty(AFS_SERVER_TIMEOUT_PROPERTY_NAME, AFS_SERVER_TIMEOUT_DEFAULT);
            final int timeout = Integer.parseInt(timeoutStr);
            AfsClient client = getAfsClient(sessionToken, url, timeout);
            AfsClientExportProxy result = new AfsClientExportProxy(client);
            return result;
        } else {
            throw new UserFailureException("AFS is not configured!");
        }
    }

    private static AfsClient getAfsClient(String sessionToken, String afsServerUrl, int timeoutInSeconds)
    {
        AfsClient afsClient = new AfsClient(URI.create(afsServerUrl), timeoutInSeconds * 1000);
        afsClient.setSessionToken(sessionToken);
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

    public byte[] downloadFileChunk(String permId, String path, long offset, int limit) {
        try
        {
            return client.read(permId, path, offset, limit);
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public boolean isSessionValid() {
        try {
            return client.isSessionValid();
        } catch (Exception e)
        {
            return false;
        }
    }


}
