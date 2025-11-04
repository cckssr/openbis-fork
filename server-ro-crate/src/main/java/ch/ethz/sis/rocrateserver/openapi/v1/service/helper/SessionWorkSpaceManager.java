package ch.ethz.sis.rocrateserver.openapi.v1.service.helper;

import ch.ethz.sis.rocrateserver.startup.RoCrateServerParameter;
import ch.ethz.sis.rocrateserver.startup.StartupMain;
import org.jboss.logging.Logger;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class SessionWorkSpaceManager
{
    private SessionWorkSpaceManager()
    {
    }

    private static final Logger LOG = Logger.getLogger(SessionWorkSpaceManager.class);


    public static void write(String sessionToken, Path path, InputStream inputStream)
            throws IOException
    {
        Path realPath = getRealPath(sessionToken, path);
        Files.copy(inputStream, realPath, StandardCopyOption.REPLACE_EXISTING);
    }

    public static InputStream read(String sessionToken, Path path) throws IOException
    {
        Path realPath = getRealPath(sessionToken, path);
        return new BufferedInputStream(Files.newInputStream(realPath));

    }

    public static Path getRealPath(String sessionToken, Path path) throws IOException
    {
        String sessionWorkSpace = StartupMain.getConfiguration()
                .getStringProperty(RoCrateServerParameter.sessionWorkSpace);
        LOG.debug("sessionWorkSpace: " + sessionWorkSpace);
        Path userSessionWorkspace = Path.of(sessionWorkSpace, sessionToken);
        LOG.debug("userSessionWorkSpace: " + userSessionWorkspace);

        if (!Files.exists(userSessionWorkspace))
        {
            LOG.debug("Creating userSessionWorkspace: " + userSessionWorkspace);

            Files.createDirectories(userSessionWorkspace);
        }
        if (path == null)
        {
            LOG.debug("Path is null, returning: " + userSessionWorkspace);
            return userSessionWorkspace;
        } else
        {
            LOG.debug("Path is not null, returning: " + userSessionWorkspace.resolve(path));
            return userSessionWorkspace.resolve(path);
        }
    }

    public static void clear(String sessionToken) throws IOException
    {
        Path realPath = getRealPath(sessionToken, null);
        delete(realPath);
    }

    public static void delete(Path sourceAsPath) throws IOException {
        Files.walkFileTree(sourceAsPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

}
