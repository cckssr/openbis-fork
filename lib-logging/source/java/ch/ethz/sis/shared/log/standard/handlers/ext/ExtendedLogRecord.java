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

package ch.ethz.sis.shared.log.standard.handlers.ext;

import java.util.logging.Level;
import java.util.logging.LogRecord;

public class ExtendedLogRecord extends LogRecord {
    private final String threadName;

    public ExtendedLogRecord(Level level, String msg) {
        super(level, msg);
        // Capture the thread name at the time of creation
        this.threadName = Thread.currentThread().getName();
    }

    /**
     * Constructs an ExtendedLogRecord by copying all values from the given LogRecord.
     * The thread name is captured at the time of creation.
     *
     * @param record the LogRecord whose values are to be copied
     */
    public ExtendedLogRecord(LogRecord record, String threadName) {
        // Initialize using the level and message from the provided record.
        super(record.getLevel(), record.getMessage());

        // Copy additional properties from the given record.
        setLoggerName(record.getLoggerName());
        setMillis(record.getMillis());
        setSequenceNumber(record.getSequenceNumber());
        setSourceClassName(record.getSourceClassName());
        setSourceMethodName(record.getSourceMethodName());
        setParameters(record.getParameters());
        setResourceBundle(record.getResourceBundle());
        setResourceBundleName(record.getResourceBundleName());
        setThreadID(record.getThreadID());
        setThrown(record.getThrown());

        // Capture the thread name at creation time.
        this.threadName = threadName;
    }

    public String getThreadName() {
        return threadName;
    }
}
