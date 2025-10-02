package ch.openbis.drive.conf;

import ch.openbis.drive.util.OpenBISDriveUtil;
import lombok.NonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class Configuration {
    public static final String LOCAL_OPENBIS_HIDDEN_DIRECTORY = ".openbis-drive";
    public static final String LOCAL_APPLICATION_DIRECTORY_ENV_KEY = "OPENBIS_DRIVE_DIR";
    public static final int OPENBIS_DRIVE_DEFAULT_PORT = 65342;
    public static final String OPENBIS_DRIVE_PORT_ENV_KEY = "OPENBIS_DRIVE_PORT";

    @NonNull
    private final Path localAppDirectory;
    private final int openbisDrivePort;

    public Configuration() throws IOException {
        this(System.getenv());
    }

    public Configuration(@NonNull Map<String, String> env) throws IOException {
        String localAppDirFromEnv = env.get(LOCAL_APPLICATION_DIRECTORY_ENV_KEY);
        if(localAppDirFromEnv != null && !localAppDirFromEnv.isEmpty()) {
            localAppDirectory = Path.of(localAppDirFromEnv);
        } else {
            localAppDirectory = OpenBISDriveUtil.getLocalHiddenDirectoryPath(Path.of(System.getProperty("user.home")));
        }

        if(Files.exists(localAppDirectory)) {
            if (!Files.isDirectory(localAppDirectory)) {
                throw new IllegalStateException(String.format("Local application directory path does not point to a directory: %s", localAppDirectory));
            }
        } else {
            Files.createDirectory(localAppDirectory);
        }

        String openbisPortFromEnv = env.get(OPENBIS_DRIVE_PORT_ENV_KEY);
        if(openbisPortFromEnv != null && !openbisPortFromEnv.isEmpty()) {
            openbisDrivePort = Integer.parseInt(openbisPortFromEnv);
        } else {
            openbisDrivePort = OPENBIS_DRIVE_DEFAULT_PORT;
        }
    }

    public Configuration(@NonNull Path localAppDirectory) {
        this.localAppDirectory = localAppDirectory;
        this.openbisDrivePort = OPENBIS_DRIVE_DEFAULT_PORT;
    }

    public Configuration(@NonNull Path localAppDirectory, int openbisDrivePort) {
        this.localAppDirectory = localAppDirectory;
        this.openbisDrivePort = openbisDrivePort;
    }

    public Path getLocalAppDirectory() {
        return localAppDirectory;
    }

    public int getOpenbisDrivePort() {
        return openbisDrivePort;
    }
}
