/*
 *  Copyright ETH 2025 Zürich, Scientific IT Services
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package ch.ethz.sis.shared.log.standard.utils;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * A simple internal logger.
 * <p>
 * Format: {@code %d %-5p [%t]c - %m%n}
 * <p>
 * This can be used before the logging has been initialized.
 * <p>
 * To override the logging level via system properties:
 *
 * <pre>{@code
 * # Only ERROR messages:
 * -Dloggerdiagnostics.level=ERROR
 *
 * # Show INFO and above:
 * -Dloggerdiagnostics.level=INFO
 *
 * # Show everything including DEBUG:
 * -Dloggerdiagnostics.level=DEBUG
 *
 * # Disable all internal logs:
 * -Dloggerdiagnostics.level=OFF
 * }</pre>
 */

public final class LoggerDiagnostics
{
    private static final PrintStream out = System.out;
    private static final PrintStream err = System.err;

    public enum Level {
        DEBUG, INFO, WARN, ERROR, OFF;

        public boolean allows(Level messageLevel) {
            return this.ordinal() <= messageLevel.ordinal();
        }
    }

    private static final Level CURRENT_LEVEL =
            Optional.ofNullable(System.getProperty("loggerdiagnostics.level"))
                    .map(String::toUpperCase)
                    .map(LoggerDiagnostics.Level::valueOf)
                    .orElse(Level.INFO);

    // yyyy-MM-dd HH:mm:ss,SSS
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS");

    private LoggerDiagnostics() { }


    public static void debug(Object message) {
        if (CURRENT_LEVEL.allows(Level.DEBUG)) {
            out.println(format(Level.DEBUG, String.valueOf(message)));
        }
    }

    public static void info(Object message) {
        if (CURRENT_LEVEL.allows(Level.INFO)) {
            out.println(format(Level.INFO, String.valueOf(message)));
        }
    }

    public static void warn(Object message) {
        if (CURRENT_LEVEL.allows(Level.WARN)) {
            out.println(format(Level.WARN, String.valueOf(message)));
        }
    }

    public static void error(Object message) {
        if (CURRENT_LEVEL.allows(Level.ERROR)) {
            err.println(format(Level.ERROR, String.valueOf(message)));
        }
    }

    public static void error(Object message, Throwable t) {
        if (CURRENT_LEVEL.allows(Level.ERROR)) {
            err.println(format(Level.ERROR, String.valueOf(message)));
            t.printStackTrace(err);
        }
    }

    /**
     * Build the pattern: timestamp level [thread] category - message
     */
    private static String format(Level level, String msg) {
        String timestamp = DATE_FORMAT.format(LocalDateTime.now());
        // pad LEVEL to 5 chars
        String lvl = String.format("%-5s", level.name());
        String thread = Thread.currentThread().getName();
        String category = callerClassName();

        return String.format(
                "%s %-5s [%s] %s - %s",
                timestamp,
                lvl,
                thread,
                category,
                msg
        );
    }

    private static String callerClassName() {
        for (StackTraceElement e : Thread.currentThread().getStackTrace()) {
            String cn = e.getClassName();
            if (!cn.equals(LoggerDiagnostics.class.getName())
                    && !cn.equals(PrintStreamAdapter.class.getName())
                    && !cn.startsWith("java.lang.Thread")) {
                // return simple class name (or e.getClassName() for FQN)
                int dot = cn.lastIndexOf('.');
                return (dot >= 0 ? cn.substring(dot + 1) : cn);
            }
        }
        return "Unknown";
    }

    private static class PrintStreamAdapter extends PrintStream {
        private final Level level;
        private final StringBuilder buffer = new StringBuilder();

        PrintStreamAdapter(Level level) {
            super(System.err); // backing stream is unused
            this.level = level;
        }

        @Override
        public void write(int b) {
            char c = (char)b;
            if (c == '\n') {
                flushBuffer();
            } else {
                buffer.append(c);
                if (buffer.length() > 2048) {
                    flushBuffer();
                }
            }
        }

        @Override
        public void flush() {
            flushBuffer();
        }

        private void flushBuffer() {
            if (buffer.length() == 0) {
                return;
            }
            String msg = buffer.toString().trim();
            buffer.setLength(0);
            LoggerDiagnostics.out.println(LoggerDiagnostics.format(level, msg));
        }
    }
}
