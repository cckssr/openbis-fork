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

    // For unit-tests
    VirtualFileSystemProvider(
            @NonNull User user,
            FtpPathTranslator ftpPathTranslator,
            FtpPathLister ftpPathLister,
            SftpListUtil sftpListUtil,
            SftpFileUtil sftpFileUtil
    ) {
        this.logger = LogManager.getLogger(this.getClass());
        this.user = user;
        this.ftpPathTranslator = ftpPathTranslator;
        this.ftpPathLister = ftpPathLister;
        this.sftpListUtil = sftpListUtil;
        this.sftpFileUtil = sftpFileUtil;
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
        FtpPathLister.EntityDescriptor entityDescriptor =
                ftpPathLister.toEntityDescriptor(sftpNodeChain).orElseThrow(UnsupportedOperationException::new);

        if (entityDescriptor.type() == SftpNode.Type.AFS_FILE) {
            String entityId = entityDescriptor.afsEntity().identifier().orElseThrow();
            String afsPath = entityDescriptor.afsPath();
            boolean isAfsEntityDataMutable = entityDescriptor.afsEntity().entityBasicInfo().mutable();

            if ( entityId != null && afsPath != null ) {
                return sftpFileUtil.createAfsFileChannel(
                        entityId,
                        afsPath,
                        user,
                        options,
                        isAfsEntityDataMutable
                );
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
                .stream().map(this::getPathFromNodeChain)
                .filter( item -> {
                    try {
                        return filter == null || filter.accept(item);
                    } catch (Exception e) { throw new RuntimeException(e); }
                }).toList();

        return toDirectoryStream(listedItems);
    }

    @Override
    public void createDirectory(Path path, FileAttribute<?>... fileAttributes) throws IOException {
        SftpNodeChain sftpNodeChain = getNodeChainFromPath(path);
        FtpPathLister.EntityDescriptor entityDescriptor =
                ftpPathLister.toEntityDescriptor(sftpNodeChain).orElseThrow(UnsupportedOperationException::new);

        if (entityDescriptor.type() == SftpNode.Type.AFS_FILE) {
            String entityId = entityDescriptor.afsEntity().identifier().orElseThrow();
            String afsPath = entityDescriptor.afsPath();
            boolean isAfsEntityDataMutable = entityDescriptor.afsEntity().entityBasicInfo().mutable();

            if (isAfsEntityDataMutable) {
                sftpListUtil.tryToCreateAfsFileRootIfNecessary(entityId);

                if ( !"/".equals(afsPath) ) {
                    sftpFileUtil.createAfsDirectory(entityId, afsPath, user);
                }
            } else {
                throw new UnsupportedOperationException("Cannot create AFS-directories in immutable entity");
            }
        } else {
            switch (entityDescriptor.type()) {
                case SPACE -> {
                    String spaceCode = entityDescriptor.identifier().orElseThrow();
                    sftpListUtil.createSpace(spaceCode);
                }
                case PROJECT -> {
                    String spaceCode = entityDescriptor.spaceCode().orElseThrow();
                    String projectCode = entityDescriptor.projectCode().orElseThrow();
                    sftpListUtil.createProject(spaceCode, projectCode);
                }
                case EXPERIMENT -> {
                    String spaceCode = entityDescriptor.spaceCode().orElseThrow();
                    String projectCode = entityDescriptor.projectCode().orElseThrow();
                    String experimentName = entityDescriptor.name().orElseThrow();
                    sftpListUtil.createExperiment(spaceCode, projectCode, experimentName);
                }
                case SAMPLE, FOLDER -> {
                    String spaceCode = entityDescriptor.spaceCode().orElseThrow();
                    String projectCode = entityDescriptor.projectCode().orElse(null);
                    String experimentId = entityDescriptor.experimentId().orElse(null);
                    String parentSampleId = entityDescriptor.parentSampleId().orElse(null);
                    String sampleName = entityDescriptor.name().orElseThrow();
                    sftpListUtil.createSample(
                            spaceCode, projectCode,
                            experimentId, parentSampleId,
                            sampleName,
                            entityDescriptor.type() == SftpNode.Type.FOLDER
                    );
                }
                case DATA_SET -> {
                    throw new UnsupportedOperationException("Creation of datasets through SFTP not supported");
                }
                default -> throw new UnsupportedOperationException("Neither AFS-file nor supported entity type");
            }
        }
    }

    @Override
    public void delete(Path path) throws IOException {
        SftpNodeChain sftpNodeChain = getNodeChainFromPath(path);
        FtpPathLister.EntityDescriptor entityDescriptor =
                ftpPathLister.toEntityDescriptor(sftpNodeChain).orElseThrow(UnsupportedOperationException::new);

        if (entityDescriptor.type() == SftpNode.Type.AFS_FILE) {
            String entityId = entityDescriptor.afsEntity().identifier().orElseThrow();
            String afsPath = entityDescriptor.afsPath();
            boolean isAfsEntityDataMutable = entityDescriptor.afsEntity().entityBasicInfo().mutable();

            if (isAfsEntityDataMutable) {
                if ( entityId != null && afsPath != null ) {
                    sftpFileUtil.deleteAfsFile(entityId, afsPath, user);
                } else {
                    throw new IllegalArgumentException("Missing AFS-file coordinates");
                }
            } else {
                throw new UnsupportedOperationException("Cannot delete AFS-files in immutable entity");
            }
        } else {
            switch (entityDescriptor.type()) {
                case SPACE -> {
                    String spaceCode = entityDescriptor.identifier().orElseThrow();
                    sftpListUtil.deleteSpace(spaceCode);
                }
                case PROJECT -> {
                    String projectId = entityDescriptor.identifier().orElseThrow();
                    sftpListUtil.deleteProject(projectId);
                }
                case EXPERIMENT -> {
                    String experimentPermId = entityDescriptor.identifier().orElseThrow();
                    sftpListUtil.deleteExperiment(experimentPermId);
                }
                case SAMPLE, FOLDER -> {
                    String samplePermId = entityDescriptor.identifier().orElseThrow();
                    sftpListUtil.deleteSample(samplePermId);
                }
                case DATA_SET -> {
                    String dataSetPermId = entityDescriptor.identifier().orElseThrow();
                    sftpListUtil.deleteDataSet(dataSetPermId);
                }
                default -> throw new UnsupportedOperationException("Neither AFS-file nor supported entity type");
            }
        }
    }

    @Override
    public void copy(Path source, Path destination, CopyOption... copyOptions) throws IOException {
        SftpNodeChain sftpNodeChain1 = getNodeChainFromPath(source);
        SftpNodeChain sftpNodeChain2 = getNodeChainFromPath(destination);

        FtpPathLister.EntityDescriptor entityDescriptor1 =
                ftpPathLister.toEntityDescriptor(sftpNodeChain1).orElseThrow(UnsupportedOperationException::new);
        FtpPathLister.EntityDescriptor entityDescriptor2 =
                ftpPathLister.toEntityDescriptor(sftpNodeChain2).orElseThrow(UnsupportedOperationException::new);

        if ( entityDescriptor1.type() == SftpNode.Type.AFS_FILE &&
                entityDescriptor2.type() == SftpNode.Type.AFS_FILE
        ) {
            String entityId1 = entityDescriptor1.afsEntity().identifier().orElseThrow();
            String afsPath1 = entityDescriptor1.afsPath();

            String entityId2 = entityDescriptor2.afsEntity().identifier().orElseThrow();
            String afsPath2 = entityDescriptor2.afsPath();
            boolean isAfsEntity2DataMutable = entityDescriptor2.afsEntity().entityBasicInfo().mutable();

            if (isAfsEntity2DataMutable) {
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
                throw new UnsupportedOperationException("Cannot copy to AFS-files in immutable entity");
            }
        } else {
            throw new UnsupportedOperationException("Not AFS-files");
        }
    }

    @Override
    public void move(Path source, Path destination, CopyOption... copyOptions) throws IOException {
        SftpNodeChain sftpNodeChain1 = getNodeChainFromPath(source);
        SftpNodeChain sftpNodeChain2 = getNodeChainFromPath(destination);

        FtpPathLister.EntityDescriptor entityDescriptor1 =
                ftpPathLister.toEntityDescriptor(sftpNodeChain1).orElseThrow(UnsupportedOperationException::new);
        FtpPathLister.EntityDescriptor entityDescriptor2 =
                ftpPathLister.toEntityDescriptor(sftpNodeChain2).orElseThrow(UnsupportedOperationException::new);

        if ( entityDescriptor1.type() == SftpNode.Type.AFS_FILE &&
                entityDescriptor2.type() == SftpNode.Type.AFS_FILE
        ) {
            String entityId1 = entityDescriptor1.afsEntity().identifier().orElseThrow();
            String afsPath1 = entityDescriptor1.afsPath();
            boolean isAfsEntity1DataMutable = entityDescriptor1.afsEntity().entityBasicInfo().mutable();

            String entityId2 = entityDescriptor2.afsEntity().identifier().orElseThrow();
            String afsPath2 = entityDescriptor2.afsPath();
            boolean isAfsEntity2DataMutable = entityDescriptor2.afsEntity().entityBasicInfo().mutable();

            if (isAfsEntity1DataMutable && isAfsEntity2DataMutable) {
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
                throw new UnsupportedOperationException("Cannot move AFS-files between immutable entities");
            }
        } else {
            // Renaming entities
            if (
                    entityDescriptor1.type() == entityDescriptor2.type() &&
                    entityDescriptor1.identifier().isPresent() &&
                    entityDescriptor1.identifier().equals(entityDescriptor2.identifier())
            ) {
                switch (entityDescriptor1.type()) {
                    case EXPERIMENT -> sftpListUtil.renameExperiment(
                            entityDescriptor1.identifier().get(),
                            entityDescriptor2.name().orElse("")
                    );
                    case SAMPLE, FOLDER -> sftpListUtil.renameSample(
                            entityDescriptor1.identifier().get(),
                            entityDescriptor2.name().orElse("")
                    );
                    case DATA_SET -> throw new UnsupportedOperationException("Unsupported renaming of datasets");
                    default -> throw new UnsupportedOperationException(
                            String.format("Unsupported renaming for entity-type %s", entityDescriptor1.type())
                    );
                }
            } else {
                throw new UnsupportedOperationException("Unsupported moving of entities (only renaming)");
            }
        }
    }

    @Override
    public boolean isSameFile(Path path1, Path path2) throws IOException {
        if (Objects.equals(path1, path2)) {
            return true;
        } else {
            try {
                Optional<FtpPathLister.EntityDescriptor> entity1 =
                        ftpPathLister.toEntityDescriptor(getNodeChainFromPath(path1));
                Optional<FtpPathLister.EntityDescriptor> entity2 =
                        ftpPathLister.toEntityDescriptor(getNodeChainFromPath(path2));

                if (entity1.isPresent() && entity2.isPresent()) {
                    return entity1.get().equals(entity2.get());
                }
            } catch (Exception e) {}
            return false;
        }
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
