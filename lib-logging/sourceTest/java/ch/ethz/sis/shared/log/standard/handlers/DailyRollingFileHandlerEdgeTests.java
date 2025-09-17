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

import ch.ethz.sis.shared.log.standard.handlers.DailyRollingFileHandler;
import org.testng.annotations.Test;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;

public class DailyRollingFileHandlerEdgeTests {

    @Test
    public void testCreatesParentDirectory() throws Exception {
        Path tempDir = Files.createTempDirectory("log-parent-");
        Path subDir = tempDir.resolve("subdir1/subdir2");
        Path logFile = subDir.resolve("logfile.log");

        assertFalse(Files.exists(subDir));

        DailyRollingFileHandler handler = new DailyRollingFileHandler(
                logFile.toString(), 1000, true, ".yyyy-MM-dd", 1
        );
        handler.setFormatter(new SimpleFormatter());
        handler.publish(new LogRecord(Level.INFO, "Test message"));
        handler.close();

        assertTrue(Files.exists(subDir), "Expected parent directories to be created");
        assertTrue(Files.exists(logFile), "Expected log file to be created");
    }

    @Test
    public void testEncodingUTF16() throws Exception {
        File file = File.createTempFile("utf16log", ".log");
        DailyRollingFileHandler handler = new DailyRollingFileHandler(
                file.getAbsolutePath(), 1000, true, ".yyyy-MM-dd", 1, Charset.forName("UTF-16")
        );
        handler.setFormatter(new SimpleFormatter());
        handler.publish(new LogRecord(Level.INFO, "EncodingTest"));
        handler.close();

        String content = Files.readString(file.toPath(), Charset.forName("UTF-16"));
        assertTrue(content.contains("EncodingTest"), "Expected UTF-16 encoded content");
    }

    @Test
    public void testAppendFalseDeletesFile() throws Exception {
        File file = File.createTempFile("deleteTest", ".log");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("OldContent\n");
        }

        DailyRollingFileHandler handler = new DailyRollingFileHandler(
                file.getAbsolutePath(), 1000, false, ".yyyy-MM-dd", 1
        );
        handler.setFormatter(new SimpleFormatter());
        handler.publish(new LogRecord(Level.INFO, "NewContent"));
        handler.close();

        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        assertFalse(content.contains("OldContent"), "Old content should have been deleted");
        assertTrue(content.contains("NewContent"), "New content should exist");
    }

    @Test(expectedExceptions = IOException.class)
    public void testInvalidPathThrowsException() throws IOException {
        String invalidPath = "/nonexistent/invalidpath/logfile.log";
        new DailyRollingFileHandler(invalidPath, 1000, true, ".yyyy-MM-dd", 1);
    }


    @Test
    public void testFormatterExceptionHandling() throws Exception {
        File file = File.createTempFile("badformatter", ".log");

        DailyRollingFileHandler handler = new DailyRollingFileHandler(
                file.getAbsolutePath(), 1000, true, ".yyyy-MM-dd", 1
        );
        handler.setFormatter(new Formatter() {
            @Override
            public String format(LogRecord record) {
                throw new RuntimeException("Formatter failed");
            }
        });

        // Should not throw, but report error internally
        handler.publish(new LogRecord(Level.INFO, "Will Fail"));
        handler.close();
    }

    @Test
    public void testConcurrentLoggingThreadSafety() throws Exception {
        File file = File.createTempFile("concurrent", ".log");
        DailyRollingFileHandler handler = new DailyRollingFileHandler(
                file.getAbsolutePath(), -1, true, ".yyyy-MM-dd", -1
        );
        handler.setFormatter(new SimpleFormatter());

        ExecutorService executor = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10000; i++) {
            final int index = i;
            executor.submit(() -> {
                handler.publish(new LogRecord(Level.INFO, "Msg" + index));
            });
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        handler.close();

        String content = Files.readString(file.toPath());
        for (int i = 0; i < 10000; i++) {
            assertTrue(content.contains("Msg" + i), "Missing: Msg" + i);
        }
    }

    @Test
    public void testOversizeLogEntry() throws Exception {
        File file = File.createTempFile("oversize", ".log");
        DailyRollingFileHandler handler = new DailyRollingFileHandler(
                file.getAbsolutePath(), 100, true, ".yyyy-MM-dd", -1
        );
        handler.setFormatter(new SimpleFormatter());

        StringBuilder hugeMessage = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            hugeMessage.append("X");
        }
        handler.publish(new LogRecord(Level.INFO, hugeMessage.toString()));
        handler.close();

        assertTrue(Files.readString(file.toPath()).contains("X"), "Huge message should be present");
    }

    @Test
    public void testFlushAndCloseOnEmptyHandler() throws Exception {
        File file = File.createTempFile("empty", ".log");
        DailyRollingFileHandler handler = new DailyRollingFileHandler(
                file.getAbsolutePath(), 1000, true, ".yyyy-MM-dd", 1
        );
        handler.setFormatter(new SimpleFormatter());
        handler.flush();
        handler.close();
        assertTrue(Files.exists(file.toPath()), "File should still exist");
    }

    @Test
    public void testNonNumericSuffixIgnoredInArchiveSorting() throws Exception {
        Path dir = Files.createTempDirectory("nonnumeric-test");
        Path logFile = dir.resolve("myspecial.log");
        Files.createFile(dir.resolve("myspecial.log2024-01-01_backup"));
        Files.createFile(dir.resolve("myspecial.log2024-01-01_1"));

        DailyRollingFileHandler handler = new DailyRollingFileHandler(
                logFile.toString(), 50, true, ".yyyy-MM-dd", 1
        );
        handler.setFormatter(new SimpleFormatter());

        for (int i = 0; i < 10; i++) {
            handler.publish(new LogRecord(Level.INFO, "TestLine" + i));
        }
        handler.flush();
        handler.close();

        // Should not throw or misinterpret non-numeric suffix
        assertTrue(Files.exists(logFile), "Main log file should still exist");
    }

    @Test
    public void testStressWithThousandsOfLogEntries() throws Exception {
        File file = File.createTempFile("stress-log", ".log");
        DailyRollingFileHandler handler = new DailyRollingFileHandler(
                file.getAbsolutePath(), -1, true, ".yyyy-MM-dd", 10
        );
        handler.setFormatter(new SimpleFormatter());

        final int totalLogs = 10000;
        for (int i = 0; i < totalLogs; i++) {
            handler.publish(new LogRecord(Level.INFO, "StressMessage" + i));
        }
        handler.flush();
        handler.close();

        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        for (int i = 0; i < totalLogs; i += 1000) { // Check every 1000th entry for performance
            assertTrue(content.contains("StressMessage" + i), "Expected StressMessage" + i + " in output");
        }
    }
}
