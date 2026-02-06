package ch.ethz.sis.openbis.generic.server.xls.importer.helper;

import ch.ethz.sis.afsapi.api.ClientAPI;
import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afsclient.client.AfsClient;
import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportModes;
import ch.systemsx.cisd.common.exceptions.UserFailureException;

import java.nio.file.Path;

final class AfsClientImportProxy
{


    private final AfsClient client;
    private final IApplicationServerApi v3;
    private final String sessionToken;

    private final ClientAPI.FileCollisionListener overrideCollisionListener = new ClientAPI.FileCollisionListener() {
        @Override
        public ClientAPI.CollisionAction precheck(Path sourcePath,Path destinationPath, boolean collision) {
            return ClientAPI.CollisionAction.Override;
        }
    };



    private AfsClientImportProxy(String sessionToken, IApplicationServerApi api, AfsClient client) {
        this.sessionToken = sessionToken;
        this.v3 = api;
        this.client = client;
    }


    public static AfsClientImportProxy getAfsClient(String sessionToken, IApplicationServerApi api, AfsClient client) {
        return new AfsClientImportProxy(sessionToken, api, client);
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
            return v3.isSessionActive(sessionToken);
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

}
