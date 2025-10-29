package ch.ethz.sis.openbis.generic.server.xls.importer.helper;

import ch.ethz.sis.afsapi.api.ClientAPI;
import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afsclient.client.AfsClient;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.openbis.generic.server.CommonServiceProvider;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.UUID;

final class AfsClientImportProxy
{

    public static final String AFS_SERVER_URL_PROPERTY_NAME = "server-public-information.afs-server.url";

    public static final String AFS_SERVER_TIMEOUT_PROPERTY_NAME = "server-public-information.afs-server.timeout";

    public static final String INTERACTIVE_SESSION_KEY_PROPERTY_NAME = "api.v3.transaction.interactive-session-key";

    public static final String AFS_SERVER_TIMEOUT_DEFAULT = "3600";

    private UUID transactionId;

    private final AfsClient client;

    private final ClientAPI.FileCollisionListener overrideCollisionListener = new ClientAPI.FileCollisionListener() {
        @Override
        public ClientAPI.CollisionAction precheck(Path sourcePath,Path destinationPath, boolean collision) {
            return ClientAPI.CollisionAction.Override;
        }
    };

    private AfsClientImportProxy(AfsClient client) {
        this.client = client;
    }


    public static AfsClientImportProxy getAfsClient(String sessionToken) {
        String url = CommonServiceProvider.tryToGetProperty(AFS_SERVER_URL_PROPERTY_NAME);
        if(url != null && !url.isBlank()) {
            String timeoutStr = CommonServiceProvider.tryToGetProperty(AFS_SERVER_TIMEOUT_PROPERTY_NAME, AFS_SERVER_TIMEOUT_DEFAULT);
            String interactiveSessionKey = CommonServiceProvider.tryToGetProperty(INTERACTIVE_SESSION_KEY_PROPERTY_NAME);
            if(interactiveSessionKey == null || interactiveSessionKey.isBlank()) {
                throw new IllegalStateException("Interactive Session Key is not configured!");
            }
            final int timeout = Integer.parseInt(timeoutStr);
            AfsClient client = getAfsClient(sessionToken, url, timeout, interactiveSessionKey);

            AfsClientImportProxy result = new AfsClientImportProxy(client);
            return result;
        } else {
            throw new UserFailureException("AFS is not configured!");
        }
    }

    private static AfsClient getAfsClient(String sessionToken, String afsServerUrl, int timeoutInSeconds, String interactiveSessionKey)
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
        try {
            return client.isSessionValid();
        } catch (Exception e)
        {
            return false;
        }
    }

    public void createDirectory(String permId, String path) {
        try
        {
            client.create(permId, path, true);
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public void remove(String permId, String path) {
        try
        {
            client.delete(permId, path);
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public void move(String permId, String path, String pathTo) {
        try {
            client.move(permId, path, permId, pathTo);
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public void upload(String permId, String name, long size, InputStream stream) {
        final int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int bytesRead = 0;
        try
        {
            int count = 0;
            while ((bytesRead = stream.read(buffer, 0, bufferSize)) != -1)
            {
                long offset = bufferSize*count;
                if(bytesRead < bufferSize) {
                    byte[] tmp = new byte[bytesRead];
                    System.arraycopy(buffer, 0, tmp, 0, bytesRead);
                    client.write(permId, name, offset, tmp);
                } else {
                    client.write(permId, name, offset, buffer);
                }
                count++;
            }

        } catch (IOException e)
        {
            throw new RuntimeException(e);
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }

    }

    public void begin() {
        if(transactionId != null) {
            return;
        }
        UUID transactionId = UUID.randomUUID();
        try
        {
            client.begin(transactionId);
            this.transactionId = transactionId;
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public void commit() {
        if(transactionId == null) {
            return;
        }
        try
        {
            client.commit();
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }

    }

    public void rollback() {
        if(transactionId == null) {
            return;
        }
        try
        {
            client.rollback();
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }

    }


}
