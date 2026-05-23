package ch.openbis.drive.conf;

import ch.ethz.sis.afsclient.client.AfsClient;
import ch.openbis.drive.util.OpenBISDriveUtil;
import ch.openbis.drive.util.OsDetectionUtil;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

public class Configuration {
    public static final String LOCAL_OPENBIS_HIDDEN_DIRECTORY = "openbis-drive";
    public static final String LOCAL_OPENBIS_STATE_DIRECTORY = "state";
    public static final String LOCAL_OPENBIS_LAUNCH_SCRIPTS_DIRECTORY = "launch-scripts";
    public static final String LOCAL_APPLICATION_DIRECTORY_ENV_KEY = "OPENBIS_DRIVE_DIR";
    public static final int OPENBIS_DRIVE_DEFAULT_PORT = 65342;
    public static final String OPENBIS_DRIVE_PORT_ENV_KEY = "OPENBIS_DRIVE_PORT";
    public static final String OPENBIS_DRIVE_MANUAL_INSTALLATION_PROPERTY = "ch.openbis.drive.manualInstallation";
    public static final String OPENBIS_DRIVE_PROPERTIES_FILE = "openbis-drive.properties";

    @NonNull
    private final Path localAppDirectory;
    private final int openbisDrivePort;

    public Configuration() throws IOException {
        this(System.getenv());
    }

    public Configuration(@NonNull Map<String, String> env) throws IOException {
        String localAppDirFromEnv = env.get(LOCAL_APPLICATION_DIRECTORY_ENV_KEY);
        if(localAppDirFromEnv != null && !localAppDirFromEnv.isEmpty()) {
            localAppDirectory = Path.of(localAppDirFromEnv).toAbsolutePath().normalize();
        } else {
            localAppDirectory = OpenBISDriveUtil.getLocalHiddenDirectoryPath(Path.of(System.getProperty("user.home")), OsDetectionUtil.detectOS());
        }

        createHiddenAppDirectories();

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

    private void createHiddenAppDirectories() throws IOException {
        Files.createDirectories(localAppDirectory);
        Files.createDirectories(localAppDirectory.resolve(LOCAL_OPENBIS_STATE_DIRECTORY));
        if ( isManualInstallation() ) {
            Files.createDirectories(localAppDirectory.resolve(LOCAL_OPENBIS_LAUNCH_SCRIPTS_DIRECTORY));
        }
    }

    public void readOpenbisDriveProperties() throws IOException {
        if (Files.exists(localAppDirectory.resolve(OPENBIS_DRIVE_PROPERTIES_FILE))) {
            Properties properties = new Properties();
            properties.load(new FileInputStream(localAppDirectory.resolve(OPENBIS_DRIVE_PROPERTIES_FILE).toFile()));
            if (properties.getProperty(AfsClient.NO_TLS_CERT_CHECK_SYSTEM_PROPERTY) != null) {
                setSystemProperty(AfsClient.NO_TLS_CERT_CHECK_SYSTEM_PROPERTY, properties.getProperty(AfsClient.NO_TLS_CERT_CHECK_SYSTEM_PROPERTY));
                setSystemProperty("jdk.internal.httpclient.disableHostnameVerification", "true");
            }
        }
    }

    void setSystemProperty(@NonNull String key, @NonNull String value) {
        System.setProperty(key, value);
    }

    public Path getLocalAppStateDirectory() {
        return localAppDirectory.resolve(LOCAL_OPENBIS_STATE_DIRECTORY);
    }

    public Path getAppLauncherPath() {
        return switch (OsDetectionUtil.detectOS()) {
            case Linux -> getAppLauncherPathForLinux();
            case Windows -> getAppLauncherPathForWindows();
            case Mac -> getAppLauncherPathForMac();
            case Unknown -> throw new IllegalStateException("Unknown OS");
        };
    }

    Path getAppLauncherPathForLinux() {
        return Path.of("/","opt", "openbis-drive", "bin", "openbis-drive");
    }

    Path getAppLauncherPathForWindows() {
        return Path.of("C:\\", "Program Files", "openbis-drive", "openbis-drive.exe");
    }

    Path getAppLauncherPathForMac() {
        return Path.of("/","Applications", "openbis-drive.app", "Contents", "MacOS", "openbis-drive");
    }

    public Path getManualInstallationAppLaunchDirectory() {
        return localAppDirectory.resolve(LOCAL_OPENBIS_LAUNCH_SCRIPTS_DIRECTORY);
    }

    public int getOpenbisDrivePort() {
        return openbisDrivePort;
    }

    public boolean isManualInstallation() {
        return  "true".equalsIgnoreCase(System.getProperty(OPENBIS_DRIVE_MANUAL_INSTALLATION_PROPERTY));
    }
}
