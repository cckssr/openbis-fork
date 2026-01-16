package ch.ethz.sis.openbis.generic.server.xls.importer.helper;

import ch.ethz.sis.afsapi.api.ClientAPI;
import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afsclient.client.AfsClient;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportModes;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.openbis.generic.server.CommonServiceProvider;

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
//            if(interactiveSessionKey == null || interactiveSessionKey.isBlank()) {
//                throw new IllegalStateException("Interactive Session Key is not configured!");
//            }
            final int timeout = Integer.parseInt(timeoutStr);
            AfsClient client = getAfsClient(sessionToken, url, timeout, interactiveSessionKey);

            return new AfsClientImportProxy(client);
        } else {
            return new AfsClientImportProxy(null);
        }
    }

    private static AfsClient getAfsClient(String sessionToken, String afsServerUrl, int timeoutInSeconds, String interactiveSessionKey)
    {
        AfsClient afsClient = new AfsClient(URI.create(afsServerUrl), timeoutInSeconds * 1000);
        afsClient.setSessionToken(sessionToken);
//        afsClient.setInteractiveSessionKey(interactiveSessionKey);
        return afsClient;
    }

    File[] listFilesBase(String permId, String source, boolean recursively) throws Exception
    {
        return client.list(permId, source, recursively);
    }

    public File[] listFiles(String permId, boolean recursively) {
        try {
            return listFilesBase(permId, "", recursively);
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

    public void createDirectory(String permId, String path) {
        createDirectory(permId, path, false);
    }

    public void createDirectory(String permId, String path, boolean failIfExist) {
        try
        {
            client.create(permId, path, true);
        } catch (Exception e)
        {
            if(!failIfExist && e.toString().contains("NoSuchFileException")) {
                return;
            }
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

    public Boolean uploadFiles(Path sourcePath, String permId, Path destinationPath, ImportModes importModes) throws Exception
    {
        ClientAPI.FileCollisionListener collisionListener = new ClientAPI.FileCollisionListener() {
            public ClientAPI.CollisionAction precheck(Path sourcePath, Path destinationPath, boolean collision) {
                if (sourcePath == null) {
                    throw new IllegalArgumentException("sourcePath is marked non-null but is null");
                } else if (destinationPath == null) {
                    throw new IllegalArgumentException("destinationPath is marked non-null but is null");
                } else {
                    switch (importModes) {
                        case FAIL_IF_EXISTS -> {
                            if(collision) {
                                throw new UserFailureException(String.format("File '%s' is already present in '%s'", destinationPath, permId));
                            }
                        }
                        case IGNORE_EXISTING -> {
                            return ClientAPI.CollisionAction.Skip;
                        }
                        case UPDATE_IF_EXISTS -> {
                            return ClientAPI.CollisionAction.Override;
                        }
                    }

                    return ClientAPI.CollisionAction.Override;
                }
            }
        };

       return client.upload(sourcePath, permId, destinationPath, collisionListener, new ClientAPI.DefaultTransferMonitorLister());
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
