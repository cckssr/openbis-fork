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

import ch.ethz.sis.shared.log.standard.core.Level;
import ch.ethz.sis.shared.log.standard.core.Priority;

public class LoggingUtils
{

    /**
     * Converts a java.util.logging.Level into a log4j Level.
     *
     * @param julLevel the java.util.logging.Level
     * @return the corresponding log4j Level
     */
    public static Level mapFromJUL(java.util.logging.Level julLevel) {
        if (julLevel == null) {
            return Level.INFO;
        }
        if (julLevel.equals(java.util.logging.Level.OFF)) {
            return Level.OFF;
        }
        if (julLevel.equals(java.util.logging.Level.SEVERE)) {
            return Level.ERROR;
        }
        if (julLevel.equals(java.util.logging.Level.WARNING)) {
            return Level.WARN;
        }
        if (julLevel.equals(java.util.logging.Level.INFO)) {
            return Level.INFO;
        }
        if (julLevel.equals(java.util.logging.Level.FINE)) {
            return Level.DEBUG;
        }
        if (julLevel.equals(java.util.logging.Level.FINER) || julLevel.equals(java.util.logging.Level.FINEST)) {
            return Level.TRACE;
        }
        if (julLevel.equals(java.util.logging.Level.ALL)) {
            return Level.ALL;
        }
        return Level.INFO;
    }


    /**
     * Maps a log level integer value to a corresponding java.util.logging.Level.
     *
     * @param levelInt the integer representation of the log level.
     * @return the corresponding java.util.logging.Level.
     */

    public static java.util.logging.Level mapToJULLevel(Level level) {
        return level == null ? null :  mapToJULLevel(level.toInt());
    }

    public static java.util.logging.Level mapToJULLevel(int levelInt) {
        if (levelInt >= Level.OFF_INT) {
            return java.util.logging.Level.OFF;
        } else if (levelInt >= Level.FATAL_INT) {
            return java.util.logging.Level.SEVERE;
        } else if (levelInt >= Level.ERROR_INT) {
            return java.util.logging.Level.SEVERE;
        } else if (levelInt >= Level.WARN_INT) {
            return java.util.logging.Level.WARNING;
        } else if (levelInt >= Level.INFO_INT) {
            return java.util.logging.Level.INFO;
        } else if (levelInt >= Level.DEBUG_INT) {
            return java.util.logging.Level.FINE;
        } else if (levelInt >= Level.TRACE_INT) {
            return java.util.logging.Level.FINEST;
        } else {
            return java.util.logging.Level.ALL;
        }
    }

    /**
     * Helper method to convert a log4j Level into a java.util.logging.Level.
     *
     * @param level the log4j Level.
     * @return the corresponding java.util.logging.Level.
     */
    public static java.util.logging.Level mapToJUL(Level level) {
        return (level == null) ? java.util.logging.Level.INFO : mapToJULLevel(level.toInt());
    }

    /**
     * Helper method to convert a log4j Priority into a java.util.logging.Level.
     *
     * @param priority the log4j Priority.
     * @return the corresponding java.util.logging.Level.
     */
    private static java.util.logging.Level mapToJULLevel(Priority priority) {
        return (priority == null) ? java.util.logging.Level.INFO : mapToJULLevel(priority.toInt());
    }
}
