package ch.systemsx.cisd.common.logging.ext;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.annotations.AfterMethod;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import static org.testng.Assert.assertTrue;

/**
 * TestNG tests for CustomFileHandler: size-based rotation and daily rolling.
 */
public class DailyRollingFileHandlerTest
{

    // Pattern for the date-only part; suffix will be "." + formatted date.
    private static final String DATE_PATTERN = ".yyyy-MM-dd";

    private Path tempDir;

    @BeforeMethod
    public void setUp() throws IOException {
        tempDir = Path.of("log-test");
        if (Files.notExists(tempDir)) {
            Files.createDirectory(tempDir);
        }
    }

    @Test
    public void testSizeBasedRotationMultipleIndices() throws Exception {
        // Using small limit so rotation occurs quickly
        LocalDate today = LocalDate.now();
        String dateSuffix = today.format(DateTimeFormatter.ofPattern(DATE_PATTERN));
        String base = tempDir.resolve("testlog").toString();
        DailyRollingFileHandler handler = new DailyRollingFileHandler(base, 100, true, DATE_PATTERN);
        handler.setFormatter(new SimpleFormatter());

        // Write enough entries to trigger three rotations
        for (int i = 0; i < 100; i++) {
            handler.publish(new LogRecord(Level.INFO, "Line" + i));
        }
        handler.flush();

        // After writes: expect files testlog + dateSuffix .1, .2, .3
        Path rotated1 = tempDir.resolve("testlog" + dateSuffix + ".1");
        Path rotated2 = tempDir.resolve("testlog" + dateSuffix + ".2");
        Path rotated3 = tempDir.resolve("testlog" + dateSuffix + ".3");
        Path active = tempDir.resolve("testlog");

        assertTrue(Files.exists(rotated1), "Expected rotated file .1");
        assertTrue(Files.exists(rotated2), "Expected rotated file .2");
        assertTrue(Files.exists(rotated3), "Expected rotated file .3");
        assertTrue(Files.exists(active), "Active log file should exist at base name");

        // Ensure each file contains some lines
        assertTrue(Files.readString(rotated1).contains("Line"), ".1 should contain lines");
        assertTrue(Files.readString(rotated2).contains("Line"), ".2 should contain lines");
        assertTrue(Files.readString(rotated3).contains("Line"), ".3 should contain lines");
        assertTrue(Files.readString(active).contains("Line"), "Active should contain lines");

        handler.close();
    }

    @Test
    public void testDailyRollover() throws Exception {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        String todaySuffix = today.format(DateTimeFormatter.ofPattern(DATE_PATTERN));
        String yesterdaySuffix = yesterday.format(DateTimeFormatter.ofPattern(DATE_PATTERN));

        String base = tempDir.resolve("dailystream").toString();
        DailyRollingFileHandler
                handler = new DailyRollingFileHandler(base, 1000, false, DATE_PATTERN);
        handler.setFormatter(new SimpleFormatter());

        // Write one entry for "yesterday"
        handler.publish(new LogRecord(Level.INFO, "OldDayMessage"));
        handler.flush();
        // Now simulate a date rollover by setting currentDate to yesterday
        Field dateField = DailyRollingFileHandler.class.getDeclaredField("currentDate");
        dateField.setAccessible(true);
        dateField.set(handler, yesterday);

        // Publish for "today" — this should rotate the active file to base + yesterdaySuffix
        handler.publish(new LogRecord(Level.INFO, "NewDayMessage"));
        handler.flush();

        // The renamed file should carry yesterday's suffix, and active should be fresh
        Path fileY = tempDir.resolve("dailystream" + yesterdaySuffix);
        Path fileT = tempDir.resolve("dailystream");

        assertTrue(Files.exists(fileY), "Log file for yesterday should exist");
        assertTrue(Files.exists(fileT), "Active log file for today should exist");

        String contentY = Files.readString(fileY);
        String contentT = Files.readString(fileT);
        assertTrue(contentY.contains("OldDayMessage"), "Yesterday's log should contain OldDayMessage");
        assertTrue(contentT.contains("NewDayMessage"), "Today's log should contain NewDayMessage");

        handler.close();
    }

    @AfterMethod
    public void tearDown() throws IOException {
        // Clean up all files and directories created during test
        Files.walk(tempDir)
                .sorted(java.util.Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }
}
