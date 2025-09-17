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

package ch.ethz.sis.shared.log.classic.impl;

import ch.ethz.sis.shared.log.standard.core.Level;
import ch.ethz.sis.shared.log.standard.core.Priority;

import java.util.Arrays;
import java.util.logging.Handler;

import static ch.ethz.sis.shared.log.standard.utils.LoggingUtils.mapFromJUL;
import static ch.ethz.sis.shared.log.standard.utils.LoggingUtils.mapToJUL;
import static ch.ethz.sis.shared.log.standard.utils.LoggingUtils.mapToJULLevel;

/**
 * A drop‐in replacement for log4j's Logger that delegates to java.util.logging.
 */
public class Logger {
    private final java.util.logging.Logger julLogger;

    protected Logger(String name)
    {
        this.julLogger = java.util.logging.Logger.getLogger(name);
    }

    /**
     * Returns a Logger for the given name.
     */
    public static Logger getLogger(String name) {
        return new Logger(name);
    }

    public static Logger getLogger(Class<?> clazz) {
        return getLogger(clazz.getName());
    }

    public static Logger getRootLogger() {
        return getLogger("");
    }


    public String getName()
    {
        return julLogger.getName();
    }

    public void debug(Object message) {
        julLogger.log(java.util.logging.Level.FINE, String.valueOf(message));
    }

    public void debug(Object message, Throwable t) {
        julLogger.log(java.util.logging.Level.FINE, String.valueOf(message), t);
    }

    public void info(Object message) {
        julLogger.log(java.util.logging.Level.INFO, String.valueOf(message));
    }

    public void info(Object message, Throwable t) {
        julLogger.log(java.util.logging.Level.INFO, String.valueOf(message), t);
    }

    public void warn(Object message) {
        julLogger.log(java.util.logging.Level.WARNING, String.valueOf(message));
    }

    public void warn(Object message, Throwable t) {
        julLogger.log(java.util.logging.Level.WARNING, String.valueOf(message), t);
    }

    public void error(Object message) {
        julLogger.log(java.util.logging.Level.SEVERE, String.valueOf(message));
    }

    public void error(Object message, Throwable t) {
        julLogger.log(java.util.logging.Level.SEVERE, String.valueOf(message), t);
    }

    public void fatal(Object message) {
        // no direct "fatal" level in JUL, so map it to SEVERE with a "FATAL:" prefix
        julLogger.log(java.util.logging.Level.SEVERE, "FATAL: " + String.valueOf(message));
    }

    public void fatal(Object message, Throwable t) {
        julLogger.log(java.util.logging.Level.SEVERE, "FATAL: " + String.valueOf(message), t);
    }

    public boolean isDebugEnabled() {
        return julLogger.isLoggable(java.util.logging.Level.FINE);
    }

    public boolean isInfoEnabled() {
        return julLogger.isLoggable(java.util.logging.Level.INFO);
    }

    public boolean isErrorEnabled() {
        return julLogger.isLoggable(java.util.logging.Level.SEVERE);
    }

    public void log(Priority priority, Object message, Throwable t) {
        java.util.logging.Level julLevel = mapToJULLevel(priority.toInt());
        julLogger.log(julLevel, String.valueOf(message), t);
    }

    public void log(Priority priority, Object message) {
        log(priority, message, null);
    }

    /**
     * Sets the logging level for this logger.
     *
     * @param level the log4j Level to set.
     */
    public void setLevel(Level level) {
        julLogger.setLevel(mapToJUL(level));
    }
    /**
     * Returns the current log level as a log4j Level.
     */
    public Level getLevel() {
        return mapFromJUL(julLogger.getLevel());
    }


    public Level getEffectiveLevel() {
        java.util.logging.Logger p = this.julLogger;

        // Guard against cycles / self-parent and extreme depth
        java.util.Set<java.util.logging.Logger> seen =
                java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        int hops = 0, MAX_HOPS = 64;

        while (p != null && seen.add(p) && hops++ < MAX_HOPS) {
            java.util.logging.Level lvl = p.getLevel();
            if (lvl != null) {
                return mapFromJUL(lvl);
            }
            java.util.logging.Logger next = p.getParent();
            if (next == p) break;
            p = next;
        }

        // Fallback: use root logger’s level if set, else INFO
        java.util.logging.Level root = java.util.logging.Logger.getLogger("").getLevel();
        return mapFromJUL(root != null ? root : java.util.logging.Level.INFO);
    }

    /**
     * Checks whether logging is enabled for the given log4j Priority.
     */
    public boolean isEnabledFor(Priority priority) {
        return julLogger.isLoggable(mapToJULLevel(priority.toInt()));
    }

    public boolean isTraceEnabled() {
        return julLogger.isLoggable(java.util.logging.Level.FINEST);
    }

    public void trace(String message)
    {
        julLogger.log(java.util.logging.Level.FINEST, String.valueOf(message));
    }

    public void addHandler(Handler handler)
    {
        julLogger.addHandler(handler);
    }

    public void removeHandler(Handler handler)
    {
        julLogger.removeHandler(handler);
    }

    public void removeAllHandlers()
    {
        Arrays.stream(julLogger.getHandlers())
                .forEach(julLogger::removeHandler);
    }

    public Handler getHandler(String name) {
        return Arrays.stream(julLogger.getHandlers())
                .filter(handler -> handler.getClass().getSimpleName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public void catching(Throwable ex) {
        String message = (ex.getMessage() != null) ? ex.getMessage() : "";
        this.log(Level.ERROR, message, ex);
    }

    public Handler[] getHandlers()
    {
        return julLogger.getHandlers();
    }

    public java.util.logging.Logger getJulLogger(){
        return julLogger;
    }
}
