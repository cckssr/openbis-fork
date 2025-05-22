/*
 * Copyright ETH 2013 - 2023 Zürich, Scientific IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.systemsx.cisd.openbis.common.log;

import java.util.logging.Filter;
import java.util.logging.LogRecord;

public class DefaultAppenderFilter implements Filter {

    public DefaultAppenderFilter() {
        System.out.println("filter");
    }

    @Override
    public boolean isLoggable(LogRecord record) {
        String loggerName = record.getLoggerName();
        String message = record.getMessage();
        if (message != null) {
            return decide(loggerName, message);
        } else {
            // If the message is not a string, let it pass (neutral).
            return true;
        }
    }

    private boolean decide(String logger, String message) {
        if (((logger.startsWith("ACCESS.") || logger.startsWith("TRACKING."))
                && logger.endsWith("Logger")
                && message.contains("(START)"))
                || logger.equals("org.hibernate.orm.deprecation")) {
            return false;
        } else {
            return true;
        }
    }
}
