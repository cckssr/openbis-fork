package ch.ethz.sis.afsclient.client;

import lombok.NonNull;

import java.nio.file.Path;

public class TemporaryPathUtil {
    public static final String OPENBIS_TMP_SUFFIX = ".openbis-drive-part";

    static Path getTwinTemporaryPath(@NonNull Path originalPath) {
        Path fileName = originalPath.getFileName();
        Path parent = originalPath.getParent();

        if (fileName != null && parent != null) {
            return parent.resolve(Path.of(fileName + OPENBIS_TMP_SUFFIX));
        } else {
            throw new IllegalArgumentException("originalPath can't be the root");
        }
    }

    static boolean isTwinTemporaryPath(@NonNull Path path) {
        return path.toString().endsWith(OPENBIS_TMP_SUFFIX);
    }
}
