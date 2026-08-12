package ch.openbis.drive.logging;

import ch.ethz.sis.shared.log.standard.LogFactory;
import ch.ethz.sis.shared.log.standard.LogManager;
import ch.ethz.sis.shared.log.standard.impl.StandardLogFactory;
import ch.openbis.drive.conf.Configuration;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;

public class Logging {
    public static final String GUI_LOGGER_CONF = "system.property.prefix=openbis-drive-gui.logging.\n" +
            ".global.level=INFO\n" +
            ".global.handlerAliases = myFileHandler, myConsoleHandler\n" +
            "myFileHandler.class = ch.ethz.sis.shared.log.standard.handlers.DailyRollingFileHandler\n" +
            "myFileHandler.maxLogFileSize=1048576\n" +
            "myFileHandler.maxLogRotations=10\n" +
            "myFileHandler.append = true\n" +
            "myFileHandler.level = INFO\n" +
            "myFileHandler.messagePattern = %d %-5p [%t] %c - %m%n\n" +
            "myConsoleHandler.class = ch.ethz.sis.shared.log.standard.handlers.ConsoleHandler\n" +
            "myConsoleHandler.level = INFO\n" +
            "myConsoleHandler.messagePattern = [DRIVE-GUI] %d %-5p [%t] %c - %m%n";

    public static final String CMD_LINE_LOGGER_CONF = "system.property.prefix=openbis-drive-cmd-line.logging.\n" +
            ".global.level=INFO\n" +
            ".global.handlerAliases = myFileHandler\n" +
            "myFileHandler.class = ch.ethz.sis.shared.log.standard.handlers.DailyRollingFileHandler\n" +
            "myFileHandler.maxLogFileSize=1048576\n" +
            "myFileHandler.maxLogRotations=10\n" +
            "myFileHandler.append = true\n" +
            "myFileHandler.level = INFO\n" +
            "myFileHandler.messagePattern = %d %-5p [%t] %c - %m%n";

    public static final String SERVICE_LOGGER_CONF = "system.property.prefix=openbis-drive-service.logging.\n" +
            ".global.level=INFO\n" +
            ".global.handlerAliases = myFileHandler, myConsoleHandler\n" +
            "myFileHandler.class = ch.ethz.sis.shared.log.standard.handlers.DailyRollingFileHandler\n" +
            "myFileHandler.maxLogFileSize=1048576\n" +
            "myFileHandler.maxLogRotations=10\n" +
            "myFileHandler.append = true\n" +
            "myFileHandler.level = INFO\n" +
            "myFileHandler.messagePattern = %d %-5p [%t] %c - %m%n\n" +
            "myConsoleHandler.class = ch.ethz.sis.shared.log.standard.handlers.ConsoleHandler\n" +
            "myConsoleHandler.level = INFO\n" +
            "myConsoleHandler.messagePattern = [DRIVE-SERVICE] %d %-5p [%t] %c - %m%n";

    public static final String TEST_LOGGER_CONF = "system.property.prefix=openbis-drive-test.logging.\n" +
            ".global.level=INFO\n" +
            ".global.handlerAliases = myConsoleHandler\n" +
            "myConsoleHandler.class = ch.ethz.sis.shared.log.standard.handlers.ConsoleHandler\n" +
            "myConsoleHandler.level = INFO\n" +
            "myConsoleHandler.messagePattern = [DRIVE-GUI] %d %-5p [%t] %c - %m%n";

    static void initializeLogging(
            @NonNull String prefix,
            @NonNull String confContent
    ) throws Exception {
        Configuration configuration = new Configuration();
        Optional<Level> propertiesDefinedLevel = configuration.readOpenbisDriveLogLevel();
        Path localAppStateDirectory = configuration.getLocalAppStateDirectory();

        Properties properties = new Properties();
        properties.load(new ByteArrayInputStream(confContent.getBytes(StandardCharsets.UTF_8)));
        properties.put("myFileHandler.logFileName", localAppStateDirectory.resolve(prefix + ".log").toAbsolutePath().toString());
        if (propertiesDefinedLevel.isPresent()) {
            properties.put(".global.level", propertiesDefinedLevel.get().getName());
            properties.put("myFileHandler.level", propertiesDefinedLevel.get().getName());
            properties.put("myConsoleHandler.level", propertiesDefinedLevel.get().getName());
        }

        Path logConfFile = Files.createTempFile(prefix + "-logging", ".properties");
        properties.store(new FileOutputStream(logConfFile.toFile()), "");
        logConfFile.toFile().deleteOnExit();

        LogFactory logFactory = new StandardLogFactory();
        logFactory.configure(
                logConfFile.toAbsolutePath().toString()
        );
        LogManager.setLogFactory(logFactory);
    }

    public static void initializeBackgroundServiceLogging() throws Exception {
        initializeLogging(
                "openbis-drive-service",
                SERVICE_LOGGER_CONF
        );
    }

    public static void initializeCommandLineLogging() throws Exception {
        System.setProperty("loggerdiagnostics.level", "OFF");
        initializeLogging(
                "openbis-drive-cmd-line",
                CMD_LINE_LOGGER_CONF
        );
    }

    public static void initializeGraphicalInterfaceLogging() throws Exception {
        initializeLogging(
                "openbis-drive-gui",
                GUI_LOGGER_CONF
        );
    }

    @SneakyThrows
    public static void initializeTestLogging() {
        initializeLogging(
                "openbis-drive-test",
                TEST_LOGGER_CONF
        );
    }

    public static void tryLogInfoInStaticMethod(@NonNull Class<?> clazz, @NonNull String message) {
        try {
            LogManager.getLogger(clazz).info(message);
        } catch (Exception ignored) {}
    }

    public static void tryCatchErrorInStaticMethod(@NonNull Class<?> clazz, @NonNull Throwable error) {
        try {
            LogManager.getLogger(clazz).catching(error);
        } catch (Exception ignored) {
            error.printStackTrace();
        }
    }
}
