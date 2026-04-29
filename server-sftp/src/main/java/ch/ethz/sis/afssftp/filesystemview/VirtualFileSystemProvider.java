package ch.ethz.sis.afssftp.filesystemview;

import ch.ethz.sis.afssftp.authentication.User;
import ch.ethz.sis.afssftp.filesystemview.impl.standard.StandardPathLister;
import ch.ethz.sis.afssftp.filesystemview.impl.standard.StandardPathTranslator;
import ch.ethz.sis.afssftp.util.SftpFileUtil;
import ch.ethz.sis.afssftp.util.SftpListUtil;
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

public class VirtualFileSystemProvider extends FileSystemProvider {
    private final Logger logger;
    
    @NonNull private final User user;
    private VirtualFileSystem createdFileSystem;
    private final FtpPathTranslator ftpPathTranslator;
    private final FtpPathLister ftpPathLister;
    private final SftpListUtil sftpListUtil;
    private final SftpFileUtil sftpFileUtil;

    public VirtualFileSystemProvider(
            @NonNull User user
    ) {
        this.logger = LogManager.getLogger(this.getClass());
        this.user = user;
        this.ftpPathTranslator = new StandardPathTranslator();
        this.ftpPathLister = new StandardPathLister(user);
        this.sftpListUtil = new SftpListUtil(user);
        this.sftpFileUtil = new SftpFileUtil(user);
    }

    void acceptCreatedFileSystem(@NonNull VirtualFileSystem createdFileSystem) {
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
        SftpNodeChain sftpNodeChain = getNodeChainFromPath(path);
        if (sftpNodeChain.getLast()
                .map( node -> node.getType() == SftpNode.Type.AFS_FILE)
                .orElse(false)
        ) {
            String entityId = sftpListUtil.getAfsEntityPermId(
                    sftpNodeChain.get(sftpNodeChain.size() - 3)
            );
            String afsPath = sftpNodeChain.getLast().get().getJoinedAfsFilePath();

            if ( entityId != null && afsPath != null ) {
                return sftpFileUtil.createAfsFileChannel(entityId, afsPath, user, options);
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
        SftpNodeChain sftpNodeChain = getNodeChainFromPath(path);

        List<Path> listedItems = ftpPathLister.list(sftpNodeChain)
                .stream().map(this::getPathFromNodeChain).toList();

        return toDirectoryStream(listedItems);
    }

    @Override
    public void createDirectory(Path path, FileAttribute<?>... fileAttributes) throws IOException {
        SftpNodeChain sftpNodeChain = getNodeChainFromPath(path);
        if (sftpNodeChain.getLast()
                .map( node -> node.getType() == SftpNode.Type.AFS_FILE)
                .orElse(false)
        ) {
            String entityId = sftpListUtil.getAfsEntityPermId(
                    sftpNodeChain.get(sftpNodeChain.size() - 3)
            );
            String afsPath = sftpNodeChain.getLast().get().getJoinedAfsFilePath();

            if ( entityId != null && afsPath != null ) {
                sftpFileUtil.createAfsDirectory(entityId, afsPath, user);
            } else {
                throw new IllegalArgumentException("Missing AFS-file coordinates");
            }
        } else {
            throw new UnsupportedOperationException("Not AFS-file");
        }
    }

    @Override
    public void delete(Path path) throws IOException {
        SftpNodeChain sftpNodeChain = getNodeChainFromPath(path);
        if (sftpNodeChain.getLast()
                .map( node -> node.getType() == SftpNode.Type.AFS_FILE)
                .orElse(false)
        ) {
            String entityId = sftpListUtil.getAfsEntityPermId(
                    sftpNodeChain.get(sftpNodeChain.size() - 3)
            );
            String afsPath = sftpNodeChain.getLast().get().getJoinedAfsFilePath();

            if ( entityId != null && afsPath != null ) {
                sftpFileUtil.deleteAfsFile(entityId, afsPath, user);
            } else {
                throw new IllegalArgumentException("Missing AFS-file coordinates");
            }
        } else {
            throw new UnsupportedOperationException("Not AFS-file");
        }
    }

    @Override
    public void copy(Path source, Path destination, CopyOption... copyOptions) throws IOException {
        SftpNodeChain sftpNodeChain1 = getNodeChainFromPath(source);
        SftpNodeChain sftpNodeChain2 = getNodeChainFromPath(destination);
        if (sftpNodeChain1.getLast()
                .map( node -> node.getType() == SftpNode.Type.AFS_FILE)
                .orElse(false) &&
            sftpNodeChain2.getLast()
                .map( node -> node.getType() == SftpNode.Type.AFS_FILE)
                .orElse(false)
        ) {
            String entityId1 = sftpListUtil.getAfsEntityPermId(
                    sftpNodeChain1.get(sftpNodeChain1.size() - 3)
            );
            String afsPath1 = sftpNodeChain1.getLast().get().getJoinedAfsFilePath();

            String entityId2 = sftpListUtil.getAfsEntityPermId(
                    sftpNodeChain2.get(sftpNodeChain2.size() - 3)
            );
            String afsPath2 = sftpNodeChain2.getLast().get().getJoinedAfsFilePath();

            if ( entityId1 != null && afsPath1 != null && entityId2 != null && afsPath2 != null ) {
                sftpFileUtil.copyAfsFile(
                        entityId1, afsPath1,
                        entityId2, afsPath2,
                        user,
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
        SftpNodeChain sftpNodeChain1 = getNodeChainFromPath(source);
        SftpNodeChain sftpNodeChain2 = getNodeChainFromPath(destination);
        if (sftpNodeChain1.getLast()
                .map( node -> node.getType() == SftpNode.Type.AFS_FILE)
                .orElse(false) &&
            sftpNodeChain2.getLast()
                        .map( node -> node.getType() == SftpNode.Type.AFS_FILE)
                        .orElse(false)
        ) {
            String entityId1 = sftpListUtil.getAfsEntityPermId(
                    sftpNodeChain1.get(sftpNodeChain1.size() - 3)
            );
            String afsPath1 = sftpNodeChain1.getLast().get().getJoinedAfsFilePath();

            String entityId2 = sftpListUtil.getAfsEntityPermId(
                    sftpNodeChain2.get(sftpNodeChain2.size() - 3)
            );
            String afsPath2 = sftpNodeChain2.getLast().get().getJoinedAfsFilePath();

            if ( entityId1 != null && afsPath1 != null && entityId2 != null && afsPath2 != null ) {
                sftpFileUtil.moveAfsFile(
                        entityId1, afsPath1,
                        entityId2, afsPath2,
                        user,
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
        SftpNodeChain sftpNodeChain = getNodeChainFromPath(path);
        return aClass.cast(ftpPathLister.readAttributes(sftpNodeChain));
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String s, LinkOption... linkOptions) throws IOException {
        SftpNodeChain sftpNodeChain = getNodeChainFromPath(path);
        SftpFileAttributes attributes = ftpPathLister.readAttributes(sftpNodeChain);
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

    @Nonnull
    SftpNodeChain getNodeChainFromPath(Path path) {
        List<String> pathSegments = new ArrayList<>();
        path.forEach(item -> pathSegments.add(item.toString()));
        SftpNodeChain sftpNodeChain;
        try {
            sftpNodeChain = ftpPathTranslator.fromPathSegments(pathSegments);
        } catch (FtpPathTranslator.MalformedPathException e) {
            logger.throwing(e);
            throw new RuntimeException(e);
        }
        return sftpNodeChain;
    }


    @Nonnull Path getPathFromNodeChain(SftpNodeChain item) {
        try {
            return new SftpPath(createdFileSystem, "/", ftpPathTranslator.toPathSegments(item));
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
