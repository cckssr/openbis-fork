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

package org.apache.log4j;


import ch.systemsx.cisd.common.logging.LoggingUtils;

import java.util.Arrays;
import java.util.logging.Handler;

import static ch.ethz.sis.shared.log.standard.utils.LoggingUtils.mapToJULLevel;
import static ch.systemsx.cisd.common.logging.LoggingUtils.mapFromJUL;
import static ch.systemsx.cisd.common.logging.LoggingUtils.mapToJUL;

/**
 * A drop‐in replacement for log4j's Logger that delegates to java.util.logging.
 */
public class Logger {
    private final java.util.logging.Logger julLogger;


    private static final String LOG4J_TAG = "[Log4j log]";

    private String decorate(Object message) {
        return LOG4J_TAG + "[" + getName() + "] " + String.valueOf(message);
    }

    protected Logger(String name) {
        this.julLogger = java.util.logging.Logger.getLogger(name);
    }

    public static Logger getLogger(String name) { return new Logger(name); }
    public static Logger getLogger(Class<?> clazz) { return getLogger(clazz.getName()); }
    public static Logger getRootLogger() { return getLogger(""); }

    public String getName() { return julLogger.getName(); }

    public void debug(Object message) {
        julLogger.log(java.util.logging.Level.FINE, decorate(message));
    }
    public void debug(Object message, Throwable t) {
        julLogger.log(java.util.logging.Level.FINE, decorate(message), t);
    }

    public void info(Object message) {
        julLogger.log(java.util.logging.Level.INFO, decorate(message));
    }
    public void info(Object message, Throwable t) {
        julLogger.log(java.util.logging.Level.INFO, decorate(message), t);
    }

    public void warn(Object message) {
        julLogger.log(java.util.logging.Level.WARNING, decorate(message));
    }
    public void warn(Object message, Throwable t) {
        julLogger.log(java.util.logging.Level.WARNING, decorate(message), t);
    }

    public void error(Object message) {
        julLogger.log(java.util.logging.Level.SEVERE, decorate(message));
    }
    public void error(Object message, Throwable t) {
        julLogger.log(java.util.logging.Level.SEVERE, decorate(message), t);
    }

    public void fatal(Object message) {
        julLogger.log(java.util.logging.Level.SEVERE, decorate("FATAL: " + String.valueOf(message)));
    }
    public void fatal(Object message, Throwable t) {
        julLogger.log(java.util.logging.Level.SEVERE, decorate("FATAL: " + String.valueOf(message)), t);
    }

    public boolean isDebugEnabled() { return julLogger.isLoggable(java.util.logging.Level.FINE); }
    public boolean isInfoEnabled()  { return julLogger.isLoggable(java.util.logging.Level.INFO); }
    public boolean isErrorEnabled() { return julLogger.isLoggable(java.util.logging.Level.SEVERE); }

    public void log(Priority priority, Object message, Throwable t) {
        java.util.logging.Level julLevel = mapToJULLevel(priority.toInt());
        julLogger.log(julLevel, decorate(message), t);
    }
    public void log(Priority priority, Object message) {
        log(priority, message, null);
    }

    public void setLevel(Level level) { julLogger.setLevel(mapToJUL(level)); }
    public Level getLevel() { return mapFromJUL(julLogger.getLevel()); }

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

    public boolean isEnabledFor(Priority priority) { return julLogger.isLoggable(mapToJULLevel(priority.toInt())); }
    public boolean isTraceEnabled() { return julLogger.isLoggable(java.util.logging.Level.FINEST); }

    public void trace(String message) { julLogger.log(java.util.logging.Level.FINEST, decorate(message)); }


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
