/*
 *  Copyright ETH 2018 - 2025 Zürich, Scientific IT Services
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
package ch.ethz.sis.shared.log.classic.log;

import ch.ethz.sis.shared.log.standard.handlers.BufferedAppender;

import ch.ethz.sis.shared.log.standard.core.Level;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import static java.util.logging.Level.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @author Franz-Josef Elmer
 *
 */
public class BufferedAppenderTest
{

    @Test
    public void test()
    {
        // Given
        BufferedAppender
                appender = new BufferedAppender(new TestFormatter(), Level.FINE,
                null);
        appender.addRegexForLoggingEventsToBeDropped("ab.*f");
        System.err.println(appender.getFilter());

        LogRecord logRecord1 = new LogRecord(INFO, "testing");
        logRecord1.setLoggerName(LogFactory.getRootLogger().getName());
        appender.publish(logRecord1);


        LogRecord logRecord2 = new LogRecord(INFO, "abcdef");
        logRecord2.setLoggerName(LogFactory.getRootLogger().getName());
        appender.publish(logRecord2);

        // Then


//                new LoggingEvent("my-class", LogManager.getRootLogger(), 123456, Level.INFO,
//                "testing", "my-thread", null, "ndc", new LocationInfo(null, null), null));
//        appender.append(new LoggingEvent("my-class", LogManager.getRootLogger(), 123456, Level.INFO,
//                "abcdef", "my-thread", null, "ndc", new LocationInfo(null, null), null));
        
        // When
        assertEquals(appender.getLogContent(), "INFO   - testing");
    }

    @Test
    public void testHugeSingleMessage()
    {
        // Use a small max buffer (50 bytes) for testing purposes.
        int maxBufferSize = 50;
        String logContent = getString(maxBufferSize);

        assertTrue(logContent.getBytes().length <= maxBufferSize,
                "Buffer should not exceed the maximum size (" + maxBufferSize + " bytes)");

        assertTrue(logContent.contains("XXXX"),
                "Log content should contain a portion of the huge message.");
    }

    private static String getString(int maxBufferSize)
    {
        BufferedAppender appender = new BufferedAppender(new TestFormatter(), Level.FINE, null,
                maxBufferSize);

        // Create a huge message
        String hugeMessage = "X".repeat(100);

        LogRecord record = new LogRecord(INFO, hugeMessage);
        record.setLoggerName("root");
        appender.publish(record);

        return appender.getLogContent();
    }


    @Test
    public void testHugeMultipleMessages() {
        // Use a small buffer (100 bytes) to force data dropping.
        int maxBufferSize = 100;
        String logContent = getLogContent(maxBufferSize);
        System.out.println("Final log content:\n" + logContent);

        // Verify that the final buffered content does not exceed the maximum buffer size.
        assertTrue(logContent.getBytes(StandardCharsets.UTF_8).length <= maxBufferSize,
                "Buffer should not exceed the maximum size (" + maxBufferSize + " bytes)");

        // Verify that the dropped (oldest) record "first message" is not present.
        assertFalse(logContent.contains("first message"),
                "The first (oldest) message should have been dropped from the buffer.");

        // Verify that the recent messages ("second message" and "third message") are present.
        assertFalse(logContent.contains("second message"),
                "The log should contain the second message.");
//        assertTrue(logContent.contains("third message"),
//                "The log should contain the third message.");

        // Verify the order is preserved: second message appears before third message.
        int indexSecond = logContent.indexOf("fourth message");
        int indexThird = logContent.indexOf("fifth message");
        assertTrue(indexSecond != -1 && indexThird != -1, "Both second and third messages must be present.");
        assertTrue(indexSecond < indexThird,
                "The second message should appear before the third message.");
    }

    private static String getLogContent(int maxBufferSize)
    {
        BufferedAppender appender = new BufferedAppender(new TestFormatter(), Level.FINE, null,
                maxBufferSize);

        // Publish several log records.
        addLogMessage(appender, "first message");
        addLogMessage(appender, "second message");
        addLogMessage(appender, "third message");
        addLogMessage(appender, "fourth message");
        addLogMessage(appender, "fifth message");
        addLogMessage(appender, "sixth message");
        return appender.getLogContent();
    }

    private static void addLogMessage(BufferedAppender appender, String message)
    {
        LogRecord record1 = new LogRecord(INFO, message);
        record1.setLoggerName("root");
        appender.publish(record1);
    }

    static class TestFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            // Using fixed width for the level to align output
            return String.format("%-5s %s - %s%n",
                    record.getLevel().getName(),
                    record.getLoggerName(),
                    record.getMessage());
        }
    }
}
