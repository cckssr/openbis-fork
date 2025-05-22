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

package ch.systemsx.cisd.common.logging;

import java.io.PrintStream;

/**
 * A simple internal logger (inspired by org.apache.log4j.helpers.LogLog).
 */
public final class LogLog {
    // where to write the messages
    private static PrintStream out = System.out;

    // flags
    private static boolean debugEnabled = false;
    private static boolean quietMode   = false;

    // prevent instantiation
    private LogLog() { }

    /**
     * Enable or disable debug-level logging.
     */
    public static void setDebug(boolean enabled) {
        debugEnabled = enabled;
    }

    /**
     * Mute all logging except errors.
     */
    public static void setQuietMode(boolean quiet) {
        quietMode = quiet;
    }

    /**
     * @return true if debug logging is on.
     */
    public static boolean isDebugEnabled() {
        return debugEnabled;
    }

    public static void debug(Object message) {
        if (debugEnabled && !quietMode) {
            out.println("[DEBUG] " + message);
        }
    }

    public static void info(Object message) {
        if (!quietMode) {
            out.println("[INFO]  " + message);
        }
    }

    public static void warn(Object message) {
        if (!quietMode) {
            out.println("[WARN]  " + message);
        }
    }

    public static void error(Object message) {
        out.println("[ERROR] " + message);
    }

    public static void error(Object message, Throwable t) {
        out.println("[ERROR] " + message);
        t.printStackTrace(out);
    }

    /**
     * Redirect all existing and future System.out/System.err to this logger.
     * System.out → INFO, System.err → ERROR
     */
    public static void redirectSystemStreams() {
        System.setOut(new PrintStreamAdapter(Level.INFO));
        System.setErr(new PrintStreamAdapter(Level.ERROR));
    }

    private enum Level { INFO, ERROR }

    /**
     * Adapter that wraps System.out/System.err writes into LogLog calls.
     */
    private static class PrintStreamAdapter extends PrintStream {
        private final Level level;
        private final StringBuilder buffer = new StringBuilder();

        PrintStreamAdapter(Level level) {
            super(System.err); // underlying stream won't actually be used
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
            if (buffer.length() == 0) return;
            String msg = buffer.toString().trim();
            buffer.setLength(0);
            if (level == Level.INFO)  LogLog.info(msg);
            else                       LogLog.error(msg);
        }
    }
}
