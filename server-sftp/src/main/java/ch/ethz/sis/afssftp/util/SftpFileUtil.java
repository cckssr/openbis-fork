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
            @NonNull Set<? extends OpenOption> options
    ) throws IOException {
        File afsFile = Optional.of(entityId)
                .flatMap( entId -> sftpListUtil.getAfsFilePresence(entId, afsPath))
                .orElse(null);

        OpenBIS.AfsServerFacade afsClient = openBISClientUtil.getAfsClient(user);

        //Create if AFS-file does not exist and CREATE or CREATE_NEW open-options are present
        boolean justCreated = false;
        if (afsFile == null &&
                (options.contains(StandardOpenOption.CREATE) ||
                        options.contains(StandardOpenOption.CREATE_NEW)
                )
        ) {
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
            boolean readOpenOption = options.contains(StandardOpenOption.READ);
            boolean writeOpenOption = options.contains(StandardOpenOption.WRITE);

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
                throw new IOException("Error deleting AFS-file");
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
                throw new IOException("Error creating AFS-directory");
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
                    throw new IOException("Error copying AFS-file");
                }
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
                    throw new IOException("Error moving AFS-file");
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
