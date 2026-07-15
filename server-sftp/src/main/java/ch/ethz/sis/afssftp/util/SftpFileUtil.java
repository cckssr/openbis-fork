package ch.ethz.sis.afssftp.util;

import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afssftp.authentication.User;
import ch.ethz.sis.afssftp.filesystemview.AfsFileChannel;
import ch.ethz.sis.openbis.generic.OpenBIS;
import lombok.NonNull;

import java.io.IOException;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.Set;

public class SftpFileUtil {
    private final User user;
    private final OpenBISClientUtil openBISClientUtil;
    private final SftpListUtil sftpListUtil;

    public SftpFileUtil(@NonNull User user) {
        this.user = user;
        this.openBISClientUtil = new OpenBISClientUtil();
        this.sftpListUtil = new SftpListUtil(user);
    }

    //For unit-tests
    SftpFileUtil(
            User user,
            OpenBISClientUtil openBISClientUtil,
            SftpListUtil sftpListUtil
    ) {
        this.user = user;
        this.openBISClientUtil = openBISClientUtil;
        this.sftpListUtil = sftpListUtil;
    }

    public AfsFileChannel createAfsFileChannel(
            @NonNull String entityId,
            @NonNull String afsPath,
            @NonNull User user,
            @NonNull Set<? extends OpenOption> options,
            boolean isAfsEntityMutable
    ) throws IOException {
        boolean readOpenOption = options.contains(StandardOpenOption.READ);
        boolean writeOpenOption = options.contains(StandardOpenOption.WRITE) ||
                options.contains(StandardOpenOption.APPEND) ||
                options.contains(StandardOpenOption.TRUNCATE_EXISTING);

        if (writeOpenOption && !isAfsEntityMutable) {
            throw new UnsupportedOperationException("Cannot write AFS data on data-immutable entity");
        }

        File afsFile = Optional.of(entityId)
                .flatMap( entId -> sftpListUtil.getAfsFilePresence(entId, afsPath))
                .orElse(null);

        OpenBIS.AfsServerFacade afsClient = openBISClientUtil.getAfsClient(user);

        // Create if AFS-file does not exist and CREATE or CREATE_NEW open-options are present
        // (raise exception if file already exists and CREATE_NEW is present)
        boolean justCreated = false;
        if (afsFile != null && options.contains(StandardOpenOption.CREATE_NEW)) {
            throw new IOException(
                    String.format("File already exists (entityId: %s, AFS-path: %s)",
                            entityId, afsPath
                    )
            );
        } else if (afsFile == null && isAfsEntityMutable &&
                (options.contains(StandardOpenOption.CREATE) ||
                        options.contains(StandardOpenOption.CREATE_NEW)
                )
        ) {
            sftpListUtil.tryToCreateAfsFileRootIfNecessary(
                    entityId
            );

            try {
                afsClient.create(entityId, afsPath, false);
            } catch (Exception e) {
                throw new IOException(e);
            }
            justCreated = true;
        }

        boolean afsRegularFile =
                (afsFile != null && !Boolean.TRUE.equals(afsFile.getDirectory()))
                        || justCreated;
        long afsFileSize = justCreated ?
                0L :
                Optional.ofNullable(afsFile).map(File::getSize).orElse(0L);

        if (afsRegularFile) {
            final long initialPosition;
            if (writeOpenOption) {
                if (options.contains(StandardOpenOption.APPEND)) {
                    initialPosition = afsFileSize;
                } else if (options.contains(StandardOpenOption.TRUNCATE_EXISTING)) {
                    try {
                        afsClient.truncate(entityId, afsPath, 0L);
                    } catch (Exception e) {
                        throw new IOException(e);
                    }
                    initialPosition = 0L;
                } else {
                    initialPosition = 0L;
                }
            } else {
                initialPosition = 0L;
            }

            return new AfsFileChannel(
                    entityId,
                    afsPath,
                    user,
                    initialPosition,
                    readOpenOption,
                    writeOpenOption
            );
        } else {
            throw new UnsupportedOperationException("AFS-file does not exist or is not a regular file");
        }
    }

    public void deleteAfsFile(
            @NonNull String entityId,
            @NonNull String afsPath,
            @NonNull User user
    ) throws IOException {
        File afsFile = Optional.of(entityId)
                .flatMap( entId -> sftpListUtil.getAfsFilePresence(entId, afsPath))
                .orElse(null);

        if (afsFile != null) {
            OpenBIS.AfsServerFacade afsClient = openBISClientUtil.getAfsClient(user);

            try {
                if ( !afsClient.delete(entityId, afsPath, true) ) {
                    throw new IOException("Error deleting AFS-file");
                }
            } catch (Exception e) {
                throw new IOException("Error deleting AFS-file", e);
            }
        }
    }

    public void createAfsDirectory(
            @NonNull String entityId,
            @NonNull String afsPath,
            @NonNull User user
    ) throws IOException {
        File afsFile = Optional.of(entityId)
                .flatMap( entId -> sftpListUtil.getAfsFilePresence(entId, afsPath))
                .orElse(null);

        if (afsFile == null) {
            OpenBIS.AfsServerFacade afsClient = openBISClientUtil.getAfsClient(user);

            try {
                if ( !afsClient.create(entityId, afsPath, true) ) {
                    throw new IOException("Error creating AFS-directory");
                }
            } catch (Exception e) {
                throw new IOException("Error creating AFS-directory", e);
            }
        } else {
            throw new IOException("Error creating AFS-directory: file already exists");
        }
    }

    public void copyAfsFile(
            @NonNull String entityIdSrc,
            @NonNull String afsPathSrc,
            @NonNull String entityIdDest,
            @NonNull String afsPathDest,
            @NonNull User user,
            boolean replaceExisting
    ) throws IOException {
        File srcFile = Optional.of(entityIdSrc)
                .flatMap( entId -> sftpListUtil.getAfsFilePresence(entId, afsPathSrc))
                .orElse(null);

        File destFile = Optional.of(entityIdDest)
                .flatMap( entId -> sftpListUtil.getAfsFilePresence(entId, afsPathDest))
                .orElse(null);

        if (srcFile != null) {
            OpenBIS.AfsServerFacade afsClient = openBISClientUtil.getAfsClient(user);

            if (destFile == null || replaceExisting) {
                try {
                    if ( !afsClient.copy(entityIdSrc, afsPathSrc,
                            entityIdDest, afsPathDest) ) {
                        throw new IOException("Error copying AFS-file");
                    }
                } catch (Exception e) {
                    throw new IOException("Error copying AFS-file", e);
                }
            } else {
                throw new IOException("Error copying AFS-file: destination already exists");
            }
        } else {
            throw new IOException("Error copying AFS-file: file does not exist");
        }
    }

    public void moveAfsFile(
            @NonNull String entityIdSrc,
            @NonNull String afsPathSrc,
            @NonNull String entityIdDest,
            @NonNull String afsPathDest,
            @NonNull User user,
            boolean replaceExisting
    ) throws IOException {
        File srcFile = Optional.of(entityIdSrc)
                .flatMap( entId -> sftpListUtil.getAfsFilePresence(entId, afsPathSrc))
                .orElse(null);

        File destFile = Optional.of(entityIdDest)
                .flatMap( entId -> sftpListUtil.getAfsFilePresence(entId, afsPathDest))
                .orElse(null);

        if (srcFile != null) {
            OpenBIS.AfsServerFacade afsClient = openBISClientUtil.getAfsClient(user);
            if (destFile == null) {
                try {
                    if ( !afsClient.move(entityIdSrc, afsPathSrc,
                            entityIdDest, afsPathDest) ) {
                        throw new IOException("Error moving AFS-file");
                    }
                } catch (Exception e) {
                    throw new IOException("Error moving AFS-file", e);
                }
            } else {
                if (replaceExisting) {
                    throw new IOException("Error moving AFS-file: unsupported overwriting already existing destination-file");
                } else {
                    throw new IOException("Error moving AFS-file: already existing destination-file");
                }
            }
        } else {
            throw new IOException("Error moving AFS-file: file does not exist");
        }
    }
}
