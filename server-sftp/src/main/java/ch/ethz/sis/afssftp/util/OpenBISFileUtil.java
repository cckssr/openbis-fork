package ch.ethz.sis.afssftp.util;

import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afssftp.authentication.OpenBISUser;
import ch.ethz.sis.afssftp.filesystemview.AfsFileChannel;
import lombok.NonNull;

import java.io.IOException;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.Set;

public class OpenBISFileUtil {
    private final OpenBISUser user;
    private final OpenBISClientUtil openBISClientUtil;
    private final OpenBISListUtil openBISListUtil;

    public OpenBISFileUtil(@NonNull OpenBISUser user) {
        this.user = user;
        this.openBISClientUtil = new OpenBISClientUtil();
        this.openBISListUtil = new OpenBISListUtil(user);
    }

    //For unit-tests
    OpenBISFileUtil(
            OpenBISUser user,
            OpenBISClientUtil openBISClientUtil,
            OpenBISListUtil openBISListUtil
    ) {
        this.user = user;
        this.openBISClientUtil = openBISClientUtil;
        this.openBISListUtil = openBISListUtil;
    }

    public AfsFileChannel createAfsFileChannel(
            @NonNull String entityId,
            @NonNull String afsPath,
            @NonNull OpenBISUser openBISUser,
            @NonNull Set<? extends OpenOption> options
    ) throws IOException {
        File afsFile = Optional.of(entityId)
                .flatMap( entId -> openBISListUtil.getAfsFilePresence(entId, afsPath))
                .orElse(null);

        AfsClientProxy afsClient = openBISClientUtil.getAfsClient(openBISUser);

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
                    openBISUser,
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
            @NonNull OpenBISUser openBISUser
    ) throws IOException {
        File afsFile = Optional.of(entityId)
                .flatMap( entId -> openBISListUtil.getAfsFilePresence(entId, afsPath))
                .orElse(null);

        if (afsFile != null) {
            AfsClientProxy afsClient = openBISClientUtil.getAfsClient(openBISUser);

            try {
                if ( !afsClient.delete(entityId, afsPath, true) ) {
                    throw new IOException("Error deleting AFS-file");
                }
            } catch (Exception e) {
                throw new IOException("Error deleting AFS-file");
            }
        }
    }
}
