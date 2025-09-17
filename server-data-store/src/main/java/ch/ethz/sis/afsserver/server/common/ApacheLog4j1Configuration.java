package ch.ethz.sis.afsserver.server.common;



import ch.ethz.sis.shared.log.classic.impl.LogManager;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ApacheLog4j1Configuration
{

    /**
     * Removes default JUL handlers and installs a custom handler
     * that redirects all JUL log messages to the AFS logging system.
     */
    public static void reconfigureToUseAFSLogging() {
        java.util.logging.Logger rootLogger = LogManager.getRootLogger().getJulLogger();

        List<Handler> handlers = Arrays.stream(rootLogger.getHandlers()).collect(Collectors.toList());
        for (Handler handler : handlers) {
            rootLogger.removeHandler(handler);
        }
        rootLogger.addHandler(new AfsLogHandler());

        handlers.forEach(rootLogger::addHandler);

        rootLogger.setLevel(Level.ALL); // Capture everything and let AFS filter

        System.out.println("java.util.logging reconfigured to use AFS Logging.");
    }

    /**
     * A custom java.util.logging.Handler that forwards log records
     * to the ch.ethz.sis.shared.log.Logger system.
     */
    private static class AfsLogHandler extends Handler {

        private static final ThreadLocal<Boolean> loggingRecursionGuard =
                ThreadLocal.withInitial(() -> Boolean.FALSE);

        @Override
        public void publish(LogRecord record) {

            if (loggingRecursionGuard.get()) {
                return;
            }

            if (!isLoggable(record)) {
                return;
            }

            loggingRecursionGuard.set(true);

            ch.ethz.sis.shared.log.standard.Logger
                    afsLogger = ch.ethz.sis.shared.log.standard.LogManager.getLogger(record.getLoggerName());


            String message = record.getMessage(); // Consider using a Formatter if needed, but raw message is often fine
            Throwable throwable = record.getThrown();

            int levelValue = record.getLevel().intValue();

            try {
                if (levelValue <= Level.FINE.intValue()) {
                    if (afsLogger.isTraceEnabled()) { // Check if enabled before formatting/logging
                        // Mimic original traceAccess behavior
                        if (throwable != null) {
                            afsLogger.traceAccess(message, throwable);
                        } else {
                            afsLogger.traceAccess(message);
                        }
                    }
                } else if (levelValue <= Level.INFO.intValue()) {
                    if (afsLogger.isInfoEnabled()) {
                        if (throwable != null) {
                            afsLogger.info(message, throwable);
                        } else {
                            afsLogger.info(message);
                        }
                    }
                } else {
                    if (afsLogger.isErrorEnabled()) {
                        if (throwable != null) {
                            afsLogger.catching(new RuntimeException(message, throwable));
                        } else {
                            afsLogger.catching(new RuntimeException(message));
                        }
                    }
                }
            } catch (Exception e) {
                // Log errors during logging attempt to standard error
                System.err.println("Error logging message via AfsLogHandler:");
                System.err.println("  Message: " + message);
                System.err.println("  Level: " + record.getLevel());
                System.err.println("  Logger: " + record.getLoggerName());
                if (throwable != null) {
                    throwable.printStackTrace(System.err);
                }
                e.printStackTrace(System.err);
            } finally
            {
                loggingRecursionGuard.remove();
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {

        }
    }

    public static void main(String[] args) {
        // Configure the redirection *before* any logging occurs
        reconfigureToUseAFSLogging();

        // Now, any code using standard java.util.logging will be redirected
        java.util.logging.Logger julLogger = Logger.getLogger("com.example.MyClass");

        julLogger.finest("This is a finest message (-> traceAccess)"); // Might not show if AFS trace isn't enabled
        julLogger.fine("This is a fine message (-> traceAccess)");   // Might not show if AFS trace isn't enabled
        julLogger.info("This is an info message (-> info)");
        julLogger.warning("This is a warning message (-> catching)");
        julLogger.severe("This is a severe message with exception (-> catching)");

        try {
            int x = 1 / 0;
        } catch (Exception e) {
            julLogger.log(Level.SEVERE, "An arithmetic error occurred", e); // (-> catching)
        }

        // You might need to ensure the AFS logging system has time to flush
        // depending on its implementation (e.g., if it uses async appenders).
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}

        System.out.println("Finished logging.");
    }

}
