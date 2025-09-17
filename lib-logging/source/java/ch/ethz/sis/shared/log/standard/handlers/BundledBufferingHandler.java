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

package ch.ethz.sis.shared.log.standard.handlers;

import ch.ethz.sis.shared.log.classic.event.BundledTimestampTriggeringEventEvaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Formatter;
import java.util.logging.SimpleFormatter;

public class BundledBufferingHandler extends Handler {
    private final BundledTimestampTriggeringEventEvaluator evaluator = new BundledTimestampTriggeringEventEvaluator();
    private final List<LogRecord> buffer = new ArrayList<>();

    // You can set your own formatter; here we use the SimpleFormatter for demonstration.
    public BundledBufferingHandler() {
        setFormatter(new SimpleFormatter());
    }

    @Override
    public synchronized void publish(LogRecord record) {
        // Buffer the log record
        buffer.add(record);
        
        // Check if it's time to trigger (flush)
        if (evaluator.isTriggeringEvent(record)) {
            flushBuffer();
        }
    }

    private void flushBuffer() {
        // Process the buffered log records.
        // For example, you might send an email with all the buffered messages.
        // Here, we'll just print them to the console.
        Formatter formatter = getFormatter();
        for (LogRecord record : buffer) {
            System.out.print(formatter.format(record));
        }
        buffer.clear();
    }

    @Override
    public void flush() {
        flushBuffer();
    }

    @Override
    public void close() throws SecurityException {
        flushBuffer();
    }
}
