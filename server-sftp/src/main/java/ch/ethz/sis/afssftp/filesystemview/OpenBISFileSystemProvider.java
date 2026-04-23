package ch.ethz.sis.afssftp.filesystemview;

import ch.ethz.sis.afssftp.authentication.OpenBISUser;
import ch.ethz.sis.afssftp.filesystemview.impl.standard.StandardPathLister;
import ch.ethz.sis.afssftp.filesystemview.impl.standard.StandardPathTranslator;
import ch.ethz.sis.afssftp.util.OpenBISFileUtil;
import ch.ethz.sis.afssftp.util.OpenBISListUtil;
import ch.ethz.sis.shared.log.standard.LogManager;
import ch.ethz.sis.shared.log.standard.Logger;
import jakarta.annotation.Nonnull;
import lombok.NonNull;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.nio.file.spi.FileSystemProvider;
import java.util.*;

public class OpenBISFileSystemProvider extends FileSystemProvider {
    private final Logger logger;
    
    @NonNull private final OpenBISUser openBISUser;
    private OpenBISFileSystem createdFileSystem;
    private final FtpPathTranslator ftpPathTranslator;
    private final FtpPathLister ftpPathLister;
    private final OpenBISListUtil openBISListUtil;
    private final OpenBISFileUtil openBISFileUtil;

    public OpenBISFileSystemProvider(
            @NonNull OpenBISUser openBISUser
    ) {
        this.logger = LogManager.getLogger(this.getClass());
        this.openBISUser = openBISUser;
        this.ftpPathTranslator = new StandardPathTranslator();
        this.ftpPathLister = new StandardPathLister(openBISUser);
        this.openBISListUtil = new OpenBISListUtil(openBISUser);
        this.openBISFileUtil = new OpenBISFileUtil(openBISUser);
    }

    void acceptCreatedFileSystem(@NonNull OpenBISFileSystem createdFileSystem) {
        this.createdFileSystem = createdFileSystem;
    }

    @Override
    public String getScheme() {
        return "openbis";
    }

    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> map) throws IOException {
        //Returning null is fine: method is not used
        return null;
    }

    @Override
    public FileSystem getFileSystem(URI uri) {
        //Returning null is fine: method is not used
        return null;
    }

    @Override
    public Path getPath(URI uri) {
        //Returning null is fine: method is not used
        return null;
    }

    @Override
    public FileChannel newFileChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        OpenBISSftpNodeChain openBISSftpNodeChain = getNodeChainFromPath(path);
        if (openBISSftpNodeChain.getLast()
                .map( node -> node.getType() == OpenBISSftpNode.Type.AFS_FILE)
                .orElse(false)
        ) {
            String entityId = openBISListUtil.getAfsEntityPermId(
                    openBISSftpNodeChain.get(openBISSftpNodeChain.size() - 3),
                    openBISSftpNodeChain.lookUpSpaceCode(),
                    openBISSftpNodeChain.lookUpProjectCode()
            );
            String afsPath = openBISSftpNodeChain.getLast().get().getJoinedAfsFilePath();

            if ( entityId != null && afsPath != null ) {
                return openBISFileUtil.createAfsFileChannel(entityId, afsPath, openBISUser, options);
            } else {
                throw new IllegalArgumentException("Missing AFS-file coordinates");
            }
        } else {
            throw new UnsupportedOperationException("Not AFS-file");
        }
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> set, FileAttribute<?>... fileAttributes) throws IOException {
        return newFileChannel(path, set, fileAttributes);
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path path, DirectoryStream.Filter<? super Path> filter) throws IOException {
        OpenBISSftpNodeChain openBISSftpNodeChain = getNodeChainFromPath(path);

        List<Path> listedItems = ftpPathLister.list(openBISSftpNodeChain)
                .stream().map(this::getPathFromNodeChain).toList();

        return toDirectoryStream(listedItems);
    }

    @Override
    public void createDirectory(Path path, FileAttribute<?>... fileAttributes) throws IOException {
        /*TODO decode path into AS or AFS object and try to create it as a directory, if applicable:
            this should be possible only for real AFS directories
        */
    }

    @Override
    public void delete(Path path) throws IOException {
        //TODO decode path into AS or AFS object and try to delete it, if applicable
    }

    @Override
    public void copy(Path path, Path path1, CopyOption... copyOptions) throws IOException {
        //TODO decode paths into AS or AFS objects (or object positions) and try to copy the former onto the latter, if applicable
    }

    @Override
    public void move(Path path, Path path1, CopyOption... copyOptions) throws IOException {
        //TODO decode paths into AS or AFS objects (or object positions) and try to move the former onto the latter, if applicable
    }

    @Override
    public boolean isSameFile(Path path, Path path1) throws IOException {
        /* TODO There are cases of different paths pointing to the same item
            in AS or AFS servers: implement a way to detect this
        */
        return Objects.equals(path, path1);
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        //Objects and files exposed by AS and AFS are never considered hidden
        //(hidden items are not exposed at all)
        return false;
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        //Returning null is fine: method is not used
        return null;
    }

    @Override
    public void checkAccess(Path path, AccessMode... accessModes) throws IOException {
        //Doing nothing here: authorization checks are performed on AS and AFS servers
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> aClass, LinkOption... linkOptions) {
        //Returning null is fine: method is not used
        return null;
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> aClass, LinkOption... linkOptions) throws IOException {
        OpenBISSftpNodeChain openBISSftpNodeChain = getNodeChainFromPath(path);
        return aClass.cast(ftpPathLister.readAttributes(openBISSftpNodeChain));
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String s, LinkOption... linkOptions) throws IOException {
        OpenBISSftpNodeChain openBISSftpNodeChain = getNodeChainFromPath(path);
        OpenBISSftpFileAttributes attributes = ftpPathLister.readAttributes(openBISSftpNodeChain);
        if ( attributes != null ) {
            return Map.of(
                    "isRegularFile", attributes.isRegularFile(),
                    "isDirectory",  attributes.isDirectory(),
                    "isSymbolicLink", attributes.isSymbolicLink(),
                    "permissions", attributes.permissions(),
                    "size", attributes.getSize(),
                    "lastModifiedTime", attributes.getModifiedTime(),
                    "lastAccessTime", attributes.getAccessTime(),
                    "owner", attributes.owner(),
                    "group", attributes.group()
            );
        } else {
            return Collections.emptyMap();
        }
    }

    @Override
    public void setAttribute(Path path, String s, Object o, LinkOption... linkOptions) throws IOException {
        throw new UnsupportedOperationException("Setting attributes unsupported");
    }
    
    /////// UTILITY METHOD SECTION ///////

    @Nonnull OpenBISSftpNodeChain getNodeChainFromPath(Path path) {
        List<String> pathSegments = new ArrayList<>();
        path.forEach(item -> pathSegments.add(item.toString()));
        OpenBISSftpNodeChain openBISSftpNodeChain;
        try {
            openBISSftpNodeChain = ftpPathTranslator.fromPathSegments(pathSegments);
        } catch (FtpPathTranslator.MalformedPathException e) {
            logger.throwing(e);
            throw new RuntimeException(e);
        }
        return openBISSftpNodeChain;
    }


    @Nonnull Path getPathFromNodeChain(OpenBISSftpNodeChain item) {
        try {
            return new OpenBISSftpPath(createdFileSystem, "/", ftpPathTranslator.toPathSegments(item));
        } catch (FtpPathTranslator.MalformedPathException e) {
            logger.throwing(e);
            throw new RuntimeException(e);
        }
    }

    @Nonnull
    static DirectoryStream<Path> toDirectoryStream(List<Path> listedItems) {
        return new DirectoryStream<Path>() {
            @Override
            public @NonNull Iterator<Path> iterator() {
                return listedItems.iterator();
            }

            @Override
            public void close() throws IOException {}
        };
    }
}
