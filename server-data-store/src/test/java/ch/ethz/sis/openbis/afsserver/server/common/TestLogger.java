package ch.ethz.sis.openbis.afsserver.server.common;

import ch.ethz.sis.shared.log.standard.utils.LogInitializer;
import ch.ethz.sis.shared.log.standard.utils.LoggingUtils;
import ch.ethz.sis.shared.log.standard.handlers.PatternFormatter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class TestLogger {

    public static String DEFAULT_LOG_LAYOUT_PATTERN = "%-5p %c - %m%n";
    public static String DEFAULT_LOGGER_NAME_REGEX = ".*";

    private static ByteArrayOutputStream recordedLog;
    private static Handler recordingHandler;


    public static void configure() {
        LogInitializer.configureFromFile(new File("logging.properties"));
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        rootLogger.setLevel(Level.ALL);
        // Optionally, you could reconfigure or remove existing handlers here.
    }

    public static void startLogRecording(ch.ethz.sis.shared.log.standard.core.Level level) {
        startLogRecording(level, DEFAULT_LOG_LAYOUT_PATTERN, DEFAULT_LOGGER_NAME_REGEX);
    }

    public static void startLogRecording(ch.ethz.sis.shared.log.standard.core.Level level, String logLayoutPattern, String loggerNameRegex) {
        if (recordingHandler != null) {
            throw new RuntimeException("Test log recording has already been started.");
        }
        recordedLog = new ByteArrayOutputStream();
        // Create our custom handler using a simple formatter and a logger name filter.
        recordingHandler = new TestHandler(recordedLog, new PatternFormatter(logLayoutPattern), new LoggerNameFilter(loggerNameRegex));
        // Set the handler's level (this works as a threshold filter)
        recordingHandler.setLevel(LoggingUtils.mapToJUL(level));

        // Add our handler to the root logger so it receives log events.
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        rootLogger.addHandler(recordingHandler);
    }

    public static void stopLogRecording() {
        if (recordingHandler == null) {
            throw new RuntimeException("Test log recording hasn't been started yet.");
        }
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        rootLogger.removeHandler(recordingHandler);
        recordingHandler.close();
        recordingHandler = null;
        recordedLog = null;
    }

    public static void resetRecordedLog() {
        if (recordingHandler == null) {
            throw new RuntimeException("Test log recording hasn't been started yet.");
        }
        recordedLog.reset();
    }

    public static String getRecordedLog() {
        if (recordingHandler == null) {
            throw new RuntimeException("Test log recording hasn't been started yet.");
        }
        return recordedLog.toString();
    }

    /**
     * A custom Handler that writes log output to a ByteArrayOutputStream.
     */
    private static class TestHandler extends Handler {
        private final ByteArrayOutputStream output;

        public TestHandler(ByteArrayOutputStream output, Formatter formatter, Filter filter) {
            this.output = output;
            setFormatter(formatter);
            setFilter(filter);
        }

        @Override
        public void publish(LogRecord record) {
            if (!isLoggable(record)) {
                return;
            }
            String msg = getFormatter().format(record);
            try {
                output.write(msg.getBytes());
            } catch (IOException e) {
                // Handle the exception as needed.
                e.printStackTrace();
            }
        }

        @Override
        public void flush() {
            try {
                output.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void close() throws SecurityException {
            try {
                output.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * A custom Filter that allows log records only if the logger name matches a given regex.
     */
    private static class LoggerNameFilter implements Filter {
        private final String loggerNameRegex;

        public LoggerNameFilter(String loggerNameRegex) {
            this.loggerNameRegex = loggerNameRegex;
        }

        @Override
        public boolean isLoggable(LogRecord record) {
            String loggerName = record.getLoggerName();
            return loggerName != null && loggerName.matches(loggerNameRegex);
        }
    }

    public static void main(String[] args) {
        // Example usage:
        configure();
        startLogRecording(ch.ethz.sis.shared.log.standard.core.Level.INFO);

        Logger logger = Logger.getLogger(TestLogger.class.getName());
        logger.info("This is a test log message.");

        System.out.println("Recorded log:");
        System.out.println(getRecordedLog());

        stopLogRecording();
    }
}
