package ch.ethz.sis.openbis.generic.server.asapi.openapi.v1.service;

import ch.ethz.sis.openbis.ros.startup.RoCrateServerParameter;
import ch.ethz.sis.openbis.ros.startup.StartupMain;
import org.apache.commons.io.FileDeleteStrategy;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

public class SessionWorkSpace
{

    private SessionWorkSpace()
    {
    }

    public static void write(String sessionToken, Path path, InputStream inputStream)
            throws IOException
    {
        Path realPath = getRealPath(sessionToken, path);
        Files.copy(inputStream, realPath, StandardCopyOption.REPLACE_EXISTING);
    }

    public static OutputStream read(String sessionToken, Path path) throws IOException
    {
        Path realPath = getRealPath(sessionToken, path);
        return new BufferedOutputStream(Files.newOutputStream(realPath, StandardOpenOption.READ));
    }

    public static Path getRealPath(String sessionToken, Path path) throws IOException
    {
        String sessionWorkSpace = StartupMain.getConfiguration()
                .getStringProperty(RoCrateServerParameter.sessionWorkSpace);
        Path sessionWorkspacePath = Path.of(sessionWorkSpace);
        if (!Files.exists(sessionWorkspacePath))
        {
            Files.createDirectories(sessionWorkspacePath);
        }
        if (path == null)
        {
            return Path.of(sessionWorkSpace, sessionToken);
        } else
        {
            return Path.of(sessionWorkSpace, sessionToken, path.toString());
        }
    }

    public static void clear(String sessionToken) throws IOException
    {
        Path realPath = getRealPath(sessionToken, null);
        File fin = new File(realPath.toString());

        for (File file : fin.listFiles())
        {
            FileDeleteStrategy.FORCE.delete(file);
        }

        Files.delete(realPath);
    }

}
