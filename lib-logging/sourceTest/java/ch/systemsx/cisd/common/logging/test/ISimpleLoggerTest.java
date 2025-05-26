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

package ch.systemsx.cisd.common.logging.test;

import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.ArrayList;
import java.util.List;
import ch.systemsx.cisd.common.logging.ISimpleLogger;
import ch.systemsx.cisd.common.logging.LogLevel;

public class ISimpleLoggerTest {
    private static class LogEntry {
        LogLevel level;
        String message;
        String throwableMessage;
        LogEntry(LogLevel level, String message, String throwableMessage) {
            this.level = level;
            this.message = message;
            this.throwableMessage = throwableMessage;
        }
    }

    private static class DummySimpleLogger implements ISimpleLogger {
        List<LogEntry> entries = new ArrayList<>();
        @Override
        public void log(LogLevel level, String message) {
            entries.add(new LogEntry(level, message, null));
        }
        @Override
        public void log(LogLevel level, String message, Throwable throwableOrNull) {
            String throwableMessage = throwableOrNull == null ? null : throwableOrNull.getMessage();
            entries.add(new LogEntry(level, message, throwableMessage));
        }
    }

    @Test
    public void testLogWithoutThrowable() {
        DummySimpleLogger logger = new DummySimpleLogger();
        logger.log(LogLevel.INFO, "Info message");
        Assert.assertEquals(logger.entries.size(), 1);
        LogEntry entry = logger.entries.get(0);
        Assert.assertEquals(entry.level, LogLevel.INFO);
        Assert.assertEquals(entry.message, "Info message");
        Assert.assertNull(entry.throwableMessage);
    }

    @Test
    public void testLogWithThrowable() {
        DummySimpleLogger logger = new DummySimpleLogger();
        Exception ex = new Exception("Error occurred");
        logger.log(LogLevel.ERROR, "Error message", ex);
        Assert.assertEquals(logger.entries.size(), 1);
        LogEntry entry = logger.entries.get(0);
        Assert.assertEquals(entry.level, LogLevel.ERROR);
        Assert.assertEquals(entry.message, "Error message");
        Assert.assertEquals(entry.throwableMessage, "Error occurred");
    }
}
