package ch.openbis.drive.util;

import ch.openbis.drive.conf.Configuration;
import lombok.NonNull;

import java.nio.file.Path;

public class OpenBISDriveUtil {
    public static Path getLocalHiddenDirectoryPath(@NonNull Path baseLocalDirectory, @NonNull OsDetectionUtil.OS operatingSystem) {
        Path path = switch (operatingSystem) {
            case Linux -> baseLocalDirectory.resolve(".local").resolve("state").resolve(Configuration.LOCAL_OPENBIS_HIDDEN_DIRECTORY);
            case Windows -> baseLocalDirectory.resolve("AppData").resolve("Local").resolve(Configuration.LOCAL_OPENBIS_HIDDEN_DIRECTORY);
            case Mac -> baseLocalDirectory.resolve("Library").resolve("Application Support").resolve(Configuration.LOCAL_OPENBIS_HIDDEN_DIRECTORY);
            case Unknown -> throw new IllegalArgumentException("Unknown operating system");
        };
        return path.toAbsolutePath().normalize();
    }

    public static Path getLocalHiddenDirectoryPath(@NonNull String baseLocalDirectory, @NonNull OsDetectionUtil.OS operatingSystem) {
        return getLocalHiddenDirectoryPath(Path.of(baseLocalDirectory), operatingSystem);
    }
}
