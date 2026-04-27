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
                openBISFileUtil.createAfsDirectory(entityId, afsPath, openBISUser);
            } else {
                throw new IllegalArgumentException("Missing AFS-file coordinates");
            }
        } else {
            throw new UnsupportedOperationException("Not AFS-file");
        }
    }

    @Override
    public void delete(Path path) throws IOException {
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
                openBISFileUtil.deleteAfsFile(entityId, afsPath, openBISUser);
            } else {
                throw new IllegalArgumentException("Missing AFS-file coordinates");
            }
        } else {
            throw new UnsupportedOperationException("Not AFS-file");
        }
    }

    @Override
    public void copy(Path source, Path destination, CopyOption... copyOptions) throws IOException {
        OpenBISSftpNodeChain openBISSftpNodeChain1 = getNodeChainFromPath(source);
        OpenBISSftpNodeChain openBISSftpNodeChain2 = getNodeChainFromPath(destination);
        if (openBISSftpNodeChain1.getLast()
                .map( node -> node.getType() == OpenBISSftpNode.Type.AFS_FILE)
                .orElse(false) &&
            openBISSftpNodeChain2.getLast()
                .map( node -> node.getType() == OpenBISSftpNode.Type.AFS_FILE)
                .orElse(false)
        ) {
            String entityId1 = openBISListUtil.getAfsEntityPermId(
                    openBISSftpNodeChain1.get(openBISSftpNodeChain1.size() - 3),
                    openBISSftpNodeChain1.lookUpSpaceCode(),
                    openBISSftpNodeChain1.lookUpProjectCode()
            );
            String afsPath1 = openBISSftpNodeChain1.getLast().get().getJoinedAfsFilePath();

            String entityId2 = openBISListUtil.getAfsEntityPermId(
                    openBISSftpNodeChain2.get(openBISSftpNodeChain2.size() - 3),
                    openBISSftpNodeChain2.lookUpSpaceCode(),
                    openBISSftpNodeChain2.lookUpProjectCode()
            );
            String afsPath2 = openBISSftpNodeChain2.getLast().get().getJoinedAfsFilePath();

            if ( entityId1 != null && afsPath1 != null && entityId2 != null && afsPath2 != null ) {
                openBISFileUtil.copyAfsFile(
                        entityId1, afsPath1,
                        entityId2, afsPath2,
                        openBISUser,
                        Arrays.asList(copyOptions).contains(StandardCopyOption.REPLACE_EXISTING));
            } else {
                throw new IllegalArgumentException("Missing AFS-file coordinates");
            }
        } else {
            throw new UnsupportedOperationException("Not AFS-files");
        }
    }

    @Override
    public void move(Path source, Path destination, CopyOption... copyOptions) throws IOException {
        OpenBISSftpNodeChain openBISSftpNodeChain1 = getNodeChainFromPath(source);
        OpenBISSftpNodeChain openBISSftpNodeChain2 = getNodeChainFromPath(destination);
        if (openBISSftpNodeChain1.getLast()
                .map( node -> node.getType() == OpenBISSftpNode.Type.AFS_FILE)
                .orElse(false) &&
            openBISSftpNodeChain2.getLast()
                        .map( node -> node.getType() == OpenBISSftpNode.Type.AFS_FILE)
                        .orElse(false)
        ) {
            String entityId1 = openBISListUtil.getAfsEntityPermId(
                    openBISSftpNodeChain1.get(openBISSftpNodeChain1.size() - 3),
                    openBISSftpNodeChain1.lookUpSpaceCode(),
                    openBISSftpNodeChain1.lookUpProjectCode()
            );
            String afsPath1 = openBISSftpNodeChain1.getLast().get().getJoinedAfsFilePath();

            String entityId2 = openBISListUtil.getAfsEntityPermId(
                    openBISSftpNodeChain2.get(openBISSftpNodeChain2.size() - 3),
                    openBISSftpNodeChain2.lookUpSpaceCode(),
                    openBISSftpNodeChain2.lookUpProjectCode()
            );
            String afsPath2 = openBISSftpNodeChain2.getLast().get().getJoinedAfsFilePath();

            if ( entityId1 != null && afsPath1 != null && entityId2 != null && afsPath2 != null ) {
                openBISFileUtil.moveAfsFile(
                        entityId1, afsPath1,
                        entityId2, afsPath2,
                        openBISUser,
                        Arrays.asList(copyOptions).contains(StandardCopyOption.REPLACE_EXISTING));
            } else {
                throw new IllegalArgumentException("Missing AFS-file coordinates");
            }
        } else {
            throw new UnsupportedOperationException("Not AFS-files");
        }
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
