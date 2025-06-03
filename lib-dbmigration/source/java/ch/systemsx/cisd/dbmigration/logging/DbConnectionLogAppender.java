/*
 * Copyright ETH 2008 - 2023 Zürich, Scientific IT Services
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
package ch.systemsx.cisd.dbmigration.logging;

import ch.systemsx.cisd.common.logging.ext.DailyRollingFileHandler;
import ch.systemsx.cisd.common.logging.ext.ExtendedLogRecord;

import java.io.IOException;
import java.util.logging.LogRecord;

/**
 * @author pkupczyk
 */
public class DbConnectionLogAppender extends DailyRollingFileHandler
{
    /**
     * Constructs the DailyRollingFileHandler.
     *
     * @param baseFileName The base name (including path) for the log files.
     * @throws IOException If an I/O error occurs while opening the log file.
     */
    public DbConnectionLogAppender(String baseFileName) throws IOException
    {
        super(baseFileName);
    }

    @Override
    public synchronized void publish(LogRecord record)
    {
        ExtendedLogRecord extendedLogRecord = new ExtendedLogRecord(record,
                Thread.currentThread().getName().replaceAll("\\s", "_"));

        super.publish(extendedLogRecord);
    }

    //    @Override
//    public void append(LoggingEvent event)
//    {
//        if (DbConnectionLogConfiguration.getInstance().isDbConnectionsSeparateLogFileEnabled())
//        {
//            super.publish(new LoggingEvent(event.getFQNOfLoggerClass(),
//                    event.getLogger(),
//                    event.getTimeStamp(),
//                    event.getLevel(),
//                    event.getMessage(),
//                    Thread.currentThread().getName().replaceAll("\\s", "_"),
//                    event.getThrowableInformation(),
//                    event.getNDC(),
//                    event.getLocationInformation(),
//                    event.getProperties()));
//        }
//    }
}
