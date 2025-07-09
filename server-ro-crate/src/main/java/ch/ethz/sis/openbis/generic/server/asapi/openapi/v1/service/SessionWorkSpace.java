package ch.ethz.sis.openbis.generic.server.asapi.openapi.v1.service;

import ch.ethz.sis.openbis.ros.startup.RoCrateServerParameter;
import ch.ethz.sis.openbis.ros.startup.StartupMain;
import org.apache.commons.io.FileDeleteStrategy;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

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
        Path userSessionWorkspace = Path.of(sessionWorkSpace, sessionToken);
        if (!Files.exists(userSessionWorkspace))
        {
            Files.createDirectories(userSessionWorkspace);
        }
        if (path == null)
        {
            return userSessionWorkspace;
        } else
        {
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
