package ch.openbis.drive.util;

import ch.openbis.drive.conf.Configuration;
import lombok.NonNull;

import java.nio.file.Path;

public class OpenBISDriveUtil {
    public static Path getLocalHiddenDirectoryPath(@NonNull Path baseLocalDirectory) {
        return baseLocalDirectory.toAbsolutePath().resolve(Configuration.LOCAL_OPENBIS_HIDDEN_DIRECTORY);
    }

    public static Path getLocalHiddenDirectoryPath(@NonNull String baseLocalDirectory) {
        return getLocalHiddenDirectoryPath(Path.of(baseLocalDirectory));
    }
}
