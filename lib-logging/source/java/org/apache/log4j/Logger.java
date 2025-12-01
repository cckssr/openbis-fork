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
public class Logger  extends ch.ethz.sis.shared.log.classic.impl.Logger
{


    private static final String LOG4J_TAG = "[Log4j log]";

    private String decorate(Object message) {
        return LOG4J_TAG + "[" + getName() + "] " + String.valueOf(message);
    }

    protected Logger(String name) {
        super(name);
    }

    public static Logger getLogger(String name) { return new Logger(name); }
    public static Logger getLogger(Class<?> clazz) { return getLogger(clazz.getName()); }
    public static Logger getRootLogger() { return getLogger(""); }

    public void debug(Object message) {
        super.debug(decorate(message));
    }
    public void debug(Object message, Throwable t) {
        super.debug(decorate(message), t);
    }

    public void info(Object message) {
        super.info(decorate(message));
    }
    public void info(Object message, Throwable t) {
        super.info(decorate(message), t);
    }

    public void warn(Object message) {
        super.warn(decorate(message));
    }
    public void warn(Object message, Throwable t) {
        super.warn(decorate(message), t);
    }

    public void error(Object message) {
        super.error(decorate(message));
    }
    public void error(Object message, Throwable t) {
        super.error(decorate(message), t);
    }

    public void fatal(Object message) {
        super.fatal(decorate("FATAL: " + String.valueOf(message)));
    }
    public void fatal(Object message, Throwable t) {
        super.fatal( decorate("FATAL: " + String.valueOf(message)), t);
    }

}
