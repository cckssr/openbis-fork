package ch.openbis.drive.util;

import ch.openbis.drive.conf.Configuration;
import ch.openbis.drive.logging.Logging;
import lombok.NonNull;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
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
        if ( checkDevMode() ) {
            return;
        }

        Configuration configuration = new Configuration();
        if ( !configuration.isManualInstallation() ) {
            switch (OsDetectionUtil.detectOS()) {

                case Linux, Mac -> {
                    ProcessBuilder processBuilder = new ProcessBuilder("nohup", configuration.getAppLauncherPath().toAbsolutePath().toString(), "background-process");
                    processBuilder.redirectOutput(Path.of("/dev/null").toFile());
                    processBuilder.redirectError(Path.of("/dev/null").toFile());
                    processBuilder.environment().putAll(System.getenv());
                    processBuilder.start();
                }

                case Windows -> {
                    ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/K", String.format("start /b \"\" \"%s\" background-process", configuration.getAppLauncherPath().toAbsolutePath()));
                    processBuilder.redirectOutput(Path.of("NUL").toFile());
                    processBuilder.redirectError(Path.of("NUL").toFile());
                    processBuilder.environment().putAll(System.getenv());
                    processBuilder.start();
                }

                case Unknown -> throw new IllegalStateException("Unknown operating-system");
            }
        } else {
            switch (OsDetectionUtil.detectOS()) {

                case Linux, Mac -> {
                    ProcessBuilder processBuilder = new ProcessBuilder("sh", "openbis-drive-service-start.sh");
                    processBuilder.redirectOutput(Path.of("/dev/null").toFile());
                    processBuilder.redirectError(Path.of("/dev/null").toFile());
                    processBuilder.directory(configuration.getManualInstallationAppLaunchDirectory().toFile());
                    processBuilder.environment().putAll(System.getenv());
                    processBuilder.start();
                }

                case Windows -> {
                    ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/K", "openbis-drive-service-start.bat");
                    processBuilder.redirectOutput(Path.of("NUL").toFile());
                    processBuilder.redirectError(Path.of("NUL").toFile());
                    processBuilder.directory(configuration.getManualInstallationAppLaunchDirectory().toFile());
                    processBuilder.environment().putAll(System.getenv());
                    processBuilder.start();
                }

                case Unknown -> throw new IllegalStateException("Unknown operating-system");
            }
        }
    }

    public static void stopServiceBackgroundProcess() throws Exception {
        if ( checkDevMode() ) {
            return;
        }

        Configuration configuration = new Configuration();
        if ( !configuration.isManualInstallation() ) {
            switch (OsDetectionUtil.detectOS()) {
                case Linux, Mac ->
                        Runtime.getRuntime().exec(new String[]{"pkill", "-SIGKILL", "-f", "openbis-drive background-process"});
                case Windows ->
                        Runtime.getRuntime().exec("powershell.exe -command \"$result = Get-WmiObject -Class win32_process -Filter \\\"Name LIKE 'openbis-drive.exe'\\\" | Select ProcessId, CommandLine ; foreach ( $i in $result ) { if ( $i.CommandLine -Match 'background-process' ) { Stop-Process -Force $i.ProcessId ; }}\"");
                case Unknown -> throw new IllegalStateException("Unknown operating-system");
            }
        } else {
            switch (OsDetectionUtil.detectOS()) {
                case Linux, Mac ->
                        Runtime.getRuntime().exec(new String[]{"pkill", "-SIGKILL", "-f", "--", "-cp app-openbis-drive-full\\.jar ch.openbis.drive.DriveAPIService"});
                case Windows ->
                        Runtime.getRuntime().exec("powershell.exe -command \"$result = Get-WmiObject -Class win32_process -Filter \\\"Name LIKE 'javaw.exe'\\\" | Select ProcessId, CommandLine ; foreach ( $i in $result ) { if ( $i.CommandLine -Match '-cp app-openbis-drive-full.jar ch.openbis.drive.DriveAPIService' ) { Stop-Process -Force $i.ProcessId ; }}\"");
                case Unknown -> throw new IllegalStateException("Unknown operating-system");
            }
        }
    }

    public static final String SHUTDOWN_MESSAGE = "shutdown";
    public enum GUISection {
        SYNC_TASKS, SETTINGS, EVENTS, NOTIFICATIONS;

        public String toLabel() {
            return switch (this) {
                case SYNC_TASKS -> "sync_tasks";
                case SETTINGS -> "settings";
                case EVENTS -> "events";
                case NOTIFICATIONS -> "notifications";
            };
        }
    }

    public static boolean tryToStopGraphicalInterface() throws Exception {
        Configuration configuration = new Configuration();
        try ( Socket guiSocket = new Socket() ) {
            guiSocket.connect(
                    new InetSocketAddress("localhost", configuration.getOpenbisDriveGuiPort()),
                    2000
            );

            guiSocket.getOutputStream().write(
                    SHUTDOWN_MESSAGE.getBytes(StandardCharsets.UTF_8));
            return true;
        }
        catch (Exception e) {
            Logging.tryCatchErrorInStaticMethod(OpenBISDriveUtil.class, e);
            return false;
        }
    }

    public static void tryToAwakeOrStartGraphicalInterface(@Nullable GUISection section) {
        try {
            if ( !OpenBISDriveUtil.tryToAwakeGraphicalInterface(section) ) {
                OpenBISDriveUtil.startGraphicalInterface(section);
            }
        } catch (Exception e) {
            Logging.tryCatchErrorInStaticMethod(OpenBISDriveUtil.class, e);
        }
    }

    public static boolean tryToAwakeGraphicalInterface(@Nullable GUISection section) throws Exception {
        Configuration configuration = new Configuration();
        try ( Socket guiSocket = new Socket()) {
            guiSocket.connect(
                    new InetSocketAddress("localhost", configuration.getOpenbisDriveGuiPort()),
                    2000
            );

            guiSocket.getOutputStream().write(
                    Optional.ofNullable(section).map(GUISection::toLabel).orElse("")
                            .getBytes(StandardCharsets.UTF_8));
            return true;
        }
        catch (Exception e) {
            Logging.tryCatchErrorInStaticMethod(OpenBISDriveUtil.class, e);
            return false;
        }
    }

    public static void startGraphicalInterface(@Nullable GUISection section) throws Exception {
        if ( checkDevMode() ) {
            return;
        }

        String launchCommandAddition = section != null ? section.toLabel() : "";

        Configuration configuration = new Configuration();
        if ( !configuration.isManualInstallation() ) {
            switch (OsDetectionUtil.detectOS()) {

                case Linux, Mac -> {
                    ProcessBuilder processBuilder = new ProcessBuilder("nohup", configuration.getAppLauncherPath().toAbsolutePath().toString(), "gui", launchCommandAddition);
                    processBuilder.redirectOutput(Path.of("/dev/null").toFile());
                    processBuilder.redirectError(Path.of("/dev/null").toFile());
                    processBuilder.environment().putAll(System.getenv());
                    processBuilder.start();
                }

                case Windows -> {
                    ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/K",  String.format("start /b \"\" \"%s\" gui %s", configuration.getAppLauncherPath().toAbsolutePath(), launchCommandAddition));
                    processBuilder.redirectOutput(Path.of("NUL").toFile());
                    processBuilder.redirectError(Path.of("NUL").toFile());
                    processBuilder.environment().putAll(System.getenv());
                    processBuilder.start();
                }

                case Unknown -> throw new IllegalStateException("Unknown operating-system");
            }
        } else {
            switch (OsDetectionUtil.detectOS()) {

                case Linux, Mac -> {
                    ProcessBuilder processBuilder = new ProcessBuilder("sh", "openbis-drive-gui.sh", launchCommandAddition);
                    processBuilder.redirectOutput(Path.of("/dev/null").toFile());
                    processBuilder.redirectError(Path.of("/dev/null").toFile());
                    processBuilder.directory(configuration.getManualInstallationAppLaunchDirectory().toFile());
                    processBuilder.environment().putAll(System.getenv());
                    processBuilder.start();
                }

                case Windows -> {
                    ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/K",  String.format("openbis-drive-gui.bat %s", launchCommandAddition));
                    processBuilder.redirectOutput(Path.of("NUL").toFile());
                    processBuilder.redirectError(Path.of("NUL").toFile());
                    processBuilder.directory(configuration.getManualInstallationAppLaunchDirectory().toFile());
                    processBuilder.environment().putAll(System.getenv());
                    processBuilder.start();
                }

                case Unknown -> throw new IllegalStateException("Unknown operating-system");
            }
        }
    }

    public static boolean checkDevMode() {
        return "true".equalsIgnoreCase(System.getenv("OPENBIS_DRIVE_DEV_MODE"));
    }
}
