package ch.openbis.drive.util;

import ch.openbis.drive.conf.Configuration;
import lombok.NonNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.stream.Stream;

public class StartAtLoginUtil {
    synchronized public static void prepareUserStartUpProfile(@NonNull Configuration configuration) throws Exception {
        OsDetectionUtil.OS os = OsDetectionUtil.detectOS();
        if (os == OsDetectionUtil.OS.Linux) {

            Path loginProfileFile = Path.of(System.getProperty("user.home")).resolve(".profile");

            if(!Files.exists(loginProfileFile)) {
                Files.createFile(loginProfileFile);
            }

            if(Files.isRegularFile(loginProfileFile) && Files.size(loginProfileFile) < 10000000) {
                byte[] loginProfileFileContent = Files.readAllBytes(loginProfileFile);
                String loginProfileFileContentUTF8 = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(loginProfileFileContent)).toString();
                String startUpProfileAddition = String.format(LINUX_USER_LOGIN_PROFILE_ADDITION, configuration.getLocalAppStateDirectory().resolve(Path.of("start-at-login", "*")));
                if (!loginProfileFileContentUTF8.contains(startUpProfileAddition)) {
                    Files.copy(loginProfileFile, loginProfileFile.getParent().resolve(".profile.openbis-drive-bak"), StandardCopyOption.REPLACE_EXISTING);
                    Files.write(loginProfileFile, startUpProfileAddition.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
                }
            } else {
                throw new UnsupportedOperationException("User .profile not manageable");
            }

        }
    }

    synchronized public static void removeStartAtLogin(@NonNull Configuration configuration) throws Exception {
        switch (OsDetectionUtil.detectOS()) {
            case Linux -> {
                Path startAtLoginDir = configuration.getLocalAppStateDirectory().resolve("start-at-login");
                if(Files.isDirectory(startAtLoginDir)) {
                    try (Stream<Path> pathStream = Files.list(startAtLoginDir)){
                        pathStream.forEach(path -> {
                            if(Files.isRegularFile(path)) {
                                try {
                                    Files.delete(path);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    throw new RuntimeException(e);
                                }
                            }
                        });
                    }
                }
            }
            case Windows -> {
                Path userMenuStartupDir = Path.of(System.getProperty("user.home"))
                        .resolve(Path.of("AppData", "Roaming", "Microsoft", "Windows", "Start Menu", "Programs", "Startup"));

                Path startupScriptCopy = userMenuStartupDir.resolve("openbis-drive-service-start.bat");
                Files.deleteIfExists(startupScriptCopy);
            }
            case Mac -> {
                Path startAtLoginDir = Path.of(System.getProperty("user.home"))
                        .resolve(Path.of("Library", "LaunchAgents"));

                if(Files.isDirectory(startAtLoginDir)) {
                    try (Stream<Path> pathStream = Files.list(startAtLoginDir)){
                        pathStream.forEach(path -> {
                            if(path.getFileName().toString().equals(MAC_OS_START_AT_LOGIN_PLIST_FILE_NAME) && Files.isRegularFile(path)) {
                                try {
                                    Files.delete(path);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    throw new RuntimeException(e);
                                }
                            }
                        });
                    }
                }
            }
            case Unknown -> {
                throw new IllegalStateException("Unable to detect a known operating-system");
            }
        }
    }

    synchronized public static void addStartAtLogin(@NonNull Configuration configuration) throws Exception {
        switch (OsDetectionUtil.detectOS()) {
            case Linux -> {
                prepareUserStartUpProfile(configuration);

                Path startAtLoginDir = configuration.getLocalAppStateDirectory().resolve("start-at-login");
                if(!Files.exists(startAtLoginDir)) {
                    Files.createDirectories(startAtLoginDir);
                }
                if(Files.isDirectory(startAtLoginDir)) {
                    try (Stream<Path> pathStream = Files.list(startAtLoginDir)) {
                        if (pathStream.noneMatch(path -> path.getFileName().equals(Path.of(LINUX_START_AT_LOGIN_SCRIPT_NAME)))) {
                            Path startAtLoginScript = startAtLoginDir.resolve(LINUX_START_AT_LOGIN_SCRIPT_NAME);
                            Path launchScriptsDir = configuration.getLocalAppLaunchDirectory();
                            Files.createFile(startAtLoginScript);
                            Files.write(startAtLoginScript, String.format(LINUX_START_AT_LOGIN_SCRIPT, launchScriptsDir.toAbsolutePath()).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                        }
                    }
                } else {
                    throw new IllegalStateException(String.format("%s must be a directory", startAtLoginDir));
                }
            }
            case Windows -> {
                Path userMenuStartupDir = Path.of(System.getProperty("user.home"))
                        .resolve(Path.of("AppData", "Roaming", "Microsoft", "Windows", "Start Menu", "Programs", "Startup"));
                Files.createDirectories(userMenuStartupDir);

                Path startupScriptCopy = userMenuStartupDir.resolve("openbis-drive-service-start.bat");
                if(!Files.exists(startupScriptCopy)) {
                    Path launchScriptsDir = configuration.getLocalAppLaunchDirectory();
                    Files.copy(launchScriptsDir.resolve("openbis-drive-service-start.bat"), startupScriptCopy, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            case Mac -> {
                Path startAtLoginDir = Path.of(System.getProperty("user.home"))
                        .resolve(Path.of("Library", "LaunchAgents"));
                Files.createDirectories(startAtLoginDir);

                if(Files.isDirectory(startAtLoginDir)) {
                    try (Stream<Path> pathStream = Files.list(startAtLoginDir)) {
                        if (pathStream.noneMatch(path -> path.getFileName().equals(Path.of(MAC_OS_START_AT_LOGIN_PLIST_FILE_NAME)))) {
                            Path startAtLoginScript = startAtLoginDir.resolve(MAC_OS_START_AT_LOGIN_PLIST_FILE_NAME);
                            Path launchScriptsDir = configuration.getLocalAppLaunchDirectory();
                            Files.write(startAtLoginScript, String.format(MAC_OS_START_AT_LOGIN_PLIST_FILE, launchScriptsDir.toAbsolutePath()).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                        }
                    }
                } else {
                    throw new IllegalStateException(String.format("%s must be a directory", startAtLoginDir));
                }
            }
            case Unknown -> {
                throw new IllegalStateException("Unable to detect a known operating-system");
            }
        }

    }

    public static String LINUX_USER_LOGIN_PROFILE_ADDITION = "\n# Added by openBIS-Drive\nfor script in %s ; do\nsh \"$script\"\ndone\n";

    public static String LINUX_START_AT_LOGIN_SCRIPT = "cd %s\nsh openbis-drive-service-start.sh";
    public static String LINUX_START_AT_LOGIN_SCRIPT_NAME = "start-at-login.sh";

    public static String MAC_OS_START_AT_LOGIN_PLIST_FILE_NAME = "ch.openbis.drive.loginscript.plist";
    public static String MAC_OS_START_AT_LOGIN_PLIST_FILE = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE plist PUBLIC "-//Apple Computer//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            <plist version="1.0">
            <dict>
               <key>Label</key>
               <string>ch.openbis.drive.loginscript</string>
               <key>ProgramArguments</key>
               <array>
                   <string>%s/mac-os-launchd/openbis-drive-service-start-for-launchd.sh</string>
               </array>
               <key>RunAtLoad</key>
               <true/>
               <key>KeepAlive</key>
               <true/>
            </dict>
            </plist>""";
}
