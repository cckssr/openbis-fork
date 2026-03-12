package ch.ethz.sis.rocrateserver.openapi.v1.service.jobs.importjob.download;

import ch.ethz.sis.rocrateserver.openapi.v1.service.helper.SessionWorkSpaceManager;
import ch.openbis.rocrate.app.reader.externalfile.IFileDownloadStrategy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

public class SessionWorkSpacveSaving implements IFileDownloadStrategy
{

    String sessionToken;

    public SessionWorkSpacveSaving(String sessionToken)
    {
        this.sessionToken = sessionToken;
    }

    @Override
    public Path getPath(String fileName) throws IOException
    {
        Path path = Path.of(UUID.randomUUID().toString());
        SessionWorkSpaceManager.write(sessionToken, path, new ByteArrayInputStream(new byte[] {}));
        return SessionWorkSpaceManager.getRealPath(sessionToken, path);
    }
}
