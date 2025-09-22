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

import ch.ethz.sis.shared.log.standard.handlers.DailyRollingFileHandler;
import ch.ethz.sis.shared.log.standard.handlers.ext.ExtendedLogRecord;

import java.io.IOException;
import java.util.logging.LogRecord;

/**
 * @author pkupczyk
 */
public class DbConnectionLogHandler extends DailyRollingFileHandler
{
    /**
     * Constructs the DailyRollingFileHandler.
     *
     * @param baseFileName The base name (including path) for the log files.
     * @throws IOException If an I/O error occurs while opening the log file.
     */
    public DbConnectionLogHandler(String baseFileName) throws IOException
    {
        super(baseFileName);
    }

    public DbConnectionLogHandler(String logFileName,
            int maxLogFileSize,
            boolean append,
            int maxLogRotations) throws IOException {
        super(logFileName, maxLogFileSize, append, maxLogRotations);
    }

    @Override
    public synchronized void publish(LogRecord record)
    {
        if (DbConnectionLogConfiguration.getInstance().isDbConnectionsSeparateLogFileEnabled())
        {
            ExtendedLogRecord extendedLogRecord = new ExtendedLogRecord(record,
                    Thread.currentThread().getName().replaceAll("\\s", "_"));

            super.publish(extendedLogRecord);
        }
    }

}
