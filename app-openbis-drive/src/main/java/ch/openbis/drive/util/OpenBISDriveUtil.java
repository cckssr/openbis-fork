package ch.openbis.drive.util;

import ch.openbis.drive.conf.Configuration;
import lombok.NonNull;

import java.nio.file.Path;
import java.util.Optional;

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

    public static void startServiceBackgroundProcess() throws Exception {
        switch (OsDetectionUtil.detectOS()) {

            case Linux, Mac -> Runtime.getRuntime().exec(new String[]{"sh", "openbis-drive-service-start.sh"}, new String[]{
                            String.format("OPENBIS_DRIVE_DIR=%s", Optional.ofNullable(System.getenv("OPENBIS_DRIVE_DIR")).orElse("")),
                            String.format("OPENBIS_DRIVE_PORT=%s", Optional.ofNullable(System.getenv("OPENBIS_DRIVE_PORT")).orElse("")),
                            String.format("PATH=%s", Optional.ofNullable(System.getenv("PATH")).orElse("")),
                            String.format("JAVA_HOME=%s", Optional.ofNullable(System.getenv("JAVA_HOME")).orElse("")),
                    },
                    new Configuration().getLocalAppLaunchDirectory().toFile());

            case Windows -> Runtime.getRuntime().exec(new String[]{"cmd.exe", "/K",  "openbis-drive-service-start.bat"}, new String[]{
                            String.format("OPENBIS_DRIVE_DIR=%s", Optional.ofNullable(System.getenv("OPENBIS_DRIVE_DIR")).orElse("")),
                            String.format("PATH=%s", Optional.ofNullable(System.getenv("PATH")).orElse("")),
                            String.format("JAVA_HOME=%s", Optional.ofNullable(System.getenv("JAVA_HOME")).orElse("")),
                            String.format("USERPROFILE=%s", Optional.ofNullable(System.getenv("USERPROFILE")).orElse("")),
                    },
                    new Configuration().getLocalAppLaunchDirectory().toFile());

            case Unknown -> throw new IllegalStateException("Unknown operating-system");
        }
    }

    public static void stopServiceBackgroundProcess() throws Exception {
        switch (OsDetectionUtil.detectOS()) {
            case Linux, Mac -> Runtime.getRuntime().exec(new String[]{"pkill", "-SIGKILL", "-f", "java -cp app-openbis-drive-full\\.jar ch.openbis.drive.DriveAPIService"});
            case Windows -> Runtime.getRuntime().exec("powershell.exe -command \"$result = Get-WmiObject -Class win32_process -Filter \\\"Name LIKE 'javaw.exe'\\\" | Select ProcessId, CommandLine ; foreach ( $i in $result ) { if ( $i.CommandLine -Match '-cp app-openbis-drive-full.jar ch.openbis.drive.DriveAPIService' ) { Stop-Process -Force $i.ProcessId ; }}\"");
            case Unknown -> throw new IllegalStateException("Unknown operating-system");
        }
    }
}
