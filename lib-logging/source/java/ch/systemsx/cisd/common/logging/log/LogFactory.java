/*
 *  Copyright ETH 2007 - 2025 Zürich, Scientific IT Services
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
package ch.systemsx.cisd.common.logging.log;

import java.util.logging.Logger;

/**
 * This class is used to create loggers (using <code>java.util.logging</code>).
 * 
 * The logger name is constructed based on a {@link LogCategory} and an optional Class.
 * For admin logs, only the category name is used.
 * 
 * @author Bernd Rinn
 */
public final class LogFactory
{
    private LogFactory()
    {
        // Cannot be instantiated.
    }

    /**
     * Returns the logger name for the given {@link LogCategory} and {@link Class}.
     * It will contain the name of the category, followed by the simple name of the class.
     *
     * @param category the logging category.
     * @param clazz the class for which the logger is created.
     * @return the combined logger name.
     */
    public static String getLoggerName(LogCategory category, Class<?> clazz)
    {
        return category.name() + "." + clazz.getSimpleName();
    }

    /**
     * Returns the logger name for the given {@link LogCategory}. This method is intended for admin logs.
     * It will contain only the name of the category.
     *
     * @param category the logging category.
     * @return the logger name.
     * @throws IllegalArgumentException if the category is not an admin log.
     */
    public static String getLoggerName(LogCategory category)
    {
        if (!category.isAdminLog())
        {
            throw new IllegalArgumentException("Only admin logs are allowed here, but we got " + category + ".");
        }
        return category.name();
    }

    /**
     * Returns the logger for the given {@link LogCategory} and {@link Class}.
     * The name of the logger will contain the category name followed by the class’s simple name.
     *
     * @param category the logging category.
     * @param clazz the class for which the logger is created.
     * @return the logger instance.
     */
    public static Logger getLogger(LogCategory category, Class<?> clazz)
    {
        return Logger.getLogger(getLoggerName(category, clazz));
    }

    /**
     * Returns the logger for the given {@link LogCategory}. This method is intended for admin logs.
     * The logger name will be the category name.
     *
     * @param category the logging category.
     * @return the logger instance.
     */
    public static Logger getLogger(LogCategory category)
    {
        return Logger.getLogger(getLoggerName(category));
    }

    /**
     * Returns the global logger
     *
     * @return the logger instance.
     */
    public static Logger getRootLogger()
    {
        return Logger.getLogger("root");
    }
}
