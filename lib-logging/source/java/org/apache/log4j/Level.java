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
/**
 * @deprecated This class is deprecated.
 *             Use {ch.ethz.sis.shared.log.standard} classes instead.
 */

@Deprecated
public class Level extends Priority
{
    public static final int OFF_INT   = Integer.MAX_VALUE;
    public static final int FATAL_INT = 50000;
    public static final int ERROR_INT = 40000;
    public static final int WARN_INT  = 30000;
    public static final int INFO_INT  = 20000;
    public static final int DEBUG_INT = 10000;
    public static final int FINE_INT  = 7500;
    public static final int TRACE_INT = 5000;
    public static final int ALL_INT   = Integer.MIN_VALUE;

    public static final Level OFF   = new Level(OFF_INT, "OFF");
    public static final Level FATAL = new Level(FATAL_INT, "FATAL");
    public static final Level ERROR = new Level(ERROR_INT, "ERROR");
    public static final Level WARN  = new Level(WARN_INT, "WARN");
    public static final Level INFO  = new Level(INFO_INT, "INFO");
    public static final Level DEBUG = new Level(DEBUG_INT, "DEBUG");
    public static final Level FINE  = new Level(FINE_INT, "FINE");
    public static final Level TRACE = new Level(TRACE_INT, "TRACE");
    public static final Level ALL   = new Level(ALL_INT, "ALL");

    // Store the level name internally
    private final String name;

    protected Level(int level, String levelStr) {
        super(level, levelStr);
        this.name = levelStr;
    }

    public boolean isGreaterOrEqual(Priority priority) {
        if (priority == null) {
            // Optionally you can throw an exception, but here we choose to return false
            return false;
        }
        // Assuming that Priority exposes its numeric value via a method toInt()
        return this.toInt() >= priority.toInt();
    }

    public static Level parse(String levelStr) {
        if (levelStr == null) {
            throw new IllegalArgumentException("Level string cannot be null");
        }

        String s = levelStr.trim();

        if ("OFF".equalsIgnoreCase(s)) {
            return OFF;
        } else if ("FATAL".equalsIgnoreCase(s)) {
            return FATAL;
        } else if ("ERROR".equalsIgnoreCase(s)) {
            return ERROR;
        } else if ("WARN".equalsIgnoreCase(s)) {
            return WARN;
        } else if ("INFO".equalsIgnoreCase(s)) {
            return INFO;
        } else if ("DEBUG".equalsIgnoreCase(s)) {
            return DEBUG;
        } else if ("FINE".equalsIgnoreCase(s)) {
            return FINE;
        } else if ("TRACE".equalsIgnoreCase(s)) {
            return TRACE;
        } else if ("ALL".equalsIgnoreCase(s)) {
            return ALL;
        }

        // If the string does not match any level name,
        // try to parse it as an integer.
        try {
            int levelNum = Integer.parseInt(s);
            // If the numeric value matches one of the standard levels, return that.
            if (levelNum == OFF_INT) {
                return OFF;
            } else if (levelNum == FATAL_INT) {
                return FATAL;
            } else if (levelNum == ERROR_INT) {
                return ERROR;
            } else if (levelNum == WARN_INT) {
                return WARN;
            } else if (levelNum == INFO_INT) {
                return INFO;
            } else if (levelNum == DEBUG_INT) {
                return DEBUG;
            } else if (levelNum == FINE_INT) {
                return FINE;
            } else if (levelNum == TRACE_INT) {
                return TRACE;
            } else if (levelNum == ALL_INT) {
                return ALL;
            } else {
                // If the numeric value doesn't match any predefined level,
                // create a new Level instance using the provided value and string.
                return new Level(levelNum, s);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Unknown level: " + levelStr, e);
        }
    }

    /**
     * Returns the human-readable name of this Level.
     *
     * @return the name of the level, e.g. "INFO", "DEBUG", etc.
     */
    public String getName() {
        return this.name;
    }
}
