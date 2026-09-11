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
            ".global.handlerAliases = DefaultFileHandler, ConsoleHandler\n" +
            "DefaultFileHandler.class = ch.ethz.sis.shared.log.standard.handlers.DailyRollingFileHandler\n" +
            "DefaultFileHandler.maxLogFileSize=1048576\n" +
            "DefaultFileHandler.maxLogRotations=10\n" +
            "DefaultFileHandler.append = true\n" +
            "DefaultFileHandler.level = INFO\n" +
            "DefaultFileHandler.messagePattern = %d %-5p [%t] %c - %m%n\n" +
            "ConsoleHandler.class = ch.ethz.sis.shared.log.standard.handlers.ConsoleHandler\n" +
            "ConsoleHandler.level = INFO\n" +
            "ConsoleHandler.messagePattern = [DRIVE-GUI] %d %-5p [%t] %c - %m%n";

    public static final String CMD_LINE_LOGGER_CONF = "system.property.prefix=openbis-drive-cmd-line.logging.\n" +
            ".global.level=INFO\n" +
            ".global.handlerAliases = DefaultFileHandler\n" +
            "DefaultFileHandler.class = ch.ethz.sis.shared.log.standard.handlers.DailyRollingFileHandler\n" +
            "DefaultFileHandler.maxLogFileSize=1048576\n" +
            "DefaultFileHandler.maxLogRotations=10\n" +
            "DefaultFileHandler.append = true\n" +
            "DefaultFileHandler.level = INFO\n" +
            "DefaultFileHandler.messagePattern = %d %-5p [%t] %c - %m%n";

    public static final String SERVICE_LOGGER_CONF = "system.property.prefix=openbis-drive-service.logging.\n" +
            ".global.level=INFO\n" +
            ".global.handlerAliases = DefaultFileHandler, ConsoleHandler\n" +
            "DefaultFileHandler.class = ch.ethz.sis.shared.log.standard.handlers.DailyRollingFileHandler\n" +
            "DefaultFileHandler.maxLogFileSize=1048576\n" +
            "DefaultFileHandler.maxLogRotations=10\n" +
            "DefaultFileHandler.append = true\n" +
            "DefaultFileHandler.level = INFO\n" +
            "DefaultFileHandler.messagePattern = %d %-5p [%t] %c - %m%n\n" +
            "ConsoleHandler.class = ch.ethz.sis.shared.log.standard.handlers.ConsoleHandler\n" +
            "ConsoleHandler.level = INFO\n" +
            "ConsoleHandler.messagePattern = [DRIVE-SERVICE] %d %-5p [%t] %c - %m%n";

    public static final String TEST_LOGGER_CONF = "system.property.prefix=openbis-drive-test.logging.\n" +
            ".global.level=INFO\n" +
            ".global.handlerAliases = ConsoleHandler\n" +
            "ConsoleHandler.class = ch.ethz.sis.shared.log.standard.handlers.ConsoleHandler\n" +
            "ConsoleHandler.level = INFO\n" +
            "ConsoleHandler.messagePattern = [DRIVE-GUI] %d %-5p [%t] %c - %m%n";

    static void initializeLogging(
            @NonNull String prefix,
            @NonNull String confContent
    ) throws Exception {
        Configuration configuration = new Configuration();
        Optional<Level> propertiesDefinedLevel = configuration.readOpenbisDriveLogLevel();
        Path localAppStateDirectory = configuration.getLocalAppStateDirectory();

        Properties properties = new Properties();
        properties.load(new ByteArrayInputStream(confContent.getBytes(StandardCharsets.UTF_8)));
        properties.put("DefaultFileHandler.logFileName", localAppStateDirectory.resolve(prefix + ".log").toAbsolutePath().toString());
        if (propertiesDefinedLevel.isPresent()) {
            properties.put(".global.level", propertiesDefinedLevel.get().getName());
            properties.put("DefaultFileHandler.level", propertiesDefinedLevel.get().getName());
            properties.put("ConsoleHandler.level", propertiesDefinedLevel.get().getName());
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
