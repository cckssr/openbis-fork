package ch.systemsx.cisd.common.logging.ext;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.annotations.AfterMethod;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
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
        DailyRollingFileHandler handler = new DailyRollingFileHandler(base, 100, true, DATE_PATTERN, -1, Charset.defaultCharset());
        handler.setFormatter(new SimpleFormatter());

        // Write enough entries to trigger three rotations
        for (int i = 0; i < 100; i++) {
            handler.publish(new LogRecord(Level.INFO, "Line" + i));
        }
        handler.flush();

        // After writes: expect files testlog + dateSuffix .1, .2, .3
        Path rotated1 = tempDir.resolve("testlog" + dateSuffix + "_1");
        Path rotated2 = tempDir.resolve("testlog" + dateSuffix + "_2");
        Path rotated3 = tempDir.resolve("testlog" + dateSuffix + "_3");
        Path active = tempDir.resolve("testlog");

       // 1) Check that “rotated1” actually exists; if not, list directory contents
        boolean exists1 = Files.exists(rotated1);
        String dirListing1 = Arrays.toString(rotated1.getParent().toFile().list());
        assertTrue(
                exists1,
                String.format(
                        "Expected rotated file <%s> to exist, but it does not.%n" +
                                "Actual files in %s: %s",
                        rotated1.getFileName(),
                        rotated1.getParent().toAbsolutePath(),
                        dirListing1
                )
        );

        // 2) Check that “rotated2” exists
        boolean exists2 = Files.exists(rotated2);
        String dirListing2 = Arrays.toString(rotated2.getParent().toFile().list());
        assertTrue(
                exists2,
                String.format(
                        "Expected rotated file <%s> to exist, but it does not.%n" +
                                "Actual files in %s: %s",
                        rotated2.getFileName(),
                        rotated2.getParent().toAbsolutePath(),
                        dirListing2
                )
        );

        // 3) Check that “rotated3” exists
        boolean exists3 = Files.exists(rotated3);
        String dirListing3 = Arrays.toString(rotated3.getParent().toFile().list());
        assertTrue(
                exists3,
                String.format(
                        "Expected rotated file <%s> to exist, but it does not.%n" +
                                "Actual files in %s: %s",
                        rotated3.getFileName(),
                        rotated3.getParent().toAbsolutePath(),
                        dirListing3
                )
        );

        // 4) Check that “active” still exists at base name
        boolean existsActive = Files.exists(active);
        String dirListingActive = Arrays.toString(active.getParent().toFile().list());
        assertTrue(
                existsActive,
                String.format(
                        "Expected active log file <%s> to exist at base name, but it does not.%n" +
                                "Actual files in %s: %s",
                        active.getFileName(),
                        active.getParent().toAbsolutePath(),
                        dirListingActive
                )
        );

        // --- Now verify that each file actually contains the substring “Line” ---

        String c1 = exists1 ? Files.readString(rotated1) : "";
        assertTrue(
                c1.contains("Line"),
                String.format(
                        "Expected <%s> to contain “Line”, but its contents were:%n---%n%s%n---",
                        rotated1.getFileName(),
                        c1.isEmpty() ? "(file does not exist or is empty)" : c1
                )
        );

        String c2 = exists2 ? Files.readString(rotated2) : "";
        assertTrue(
                c2.contains("Line"),
                String.format(
                        "Expected <%s> to contain “Line”, but its contents were:%n---%n%s%n---",
                        rotated2.getFileName(),
                        c2.isEmpty() ? "(file does not exist or is empty)" : c2
                )
        );

        String c3 = exists3 ? Files.readString(rotated3) : "";
        assertTrue(
                c3.contains("Line"),
                String.format(
                        "Expected <%s> to contain “Line”, but its contents were:%n---%n%s%n---",
                        rotated3.getFileName(),
                        c3.isEmpty() ? "(file does not exist or is empty)" : c3
                )
        );

        String cActive = existsActive ? Files.readString(active) : "";
        assertTrue(
                cActive.contains("Line"),
                String.format(
                        "Expected active file <%s> to contain “Line”, but its contents were:%n---%n%s%n---",
                        active.getFileName(),
                        cActive.isEmpty() ? "(file does not exist or is empty)" : cActive
                )
        );

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
                handler = new DailyRollingFileHandler(base, 1000, false, DATE_PATTERN, -1);
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

    @Test
    public void testMaxLogRotationsZero() throws Exception {
        LocalDate today = LocalDate.now();
        String dateSuffix = today.format(DateTimeFormatter.ofPattern(DATE_PATTERN));

        String base = tempDir.resolve("zerolog").toString();
        // maxLogRotations = 0 => no archives kept
        DailyRollingFileHandler handler = new DailyRollingFileHandler(base, 50, true, DATE_PATTERN, 0);
        handler.setFormatter(new SimpleFormatter());

        // Write entries to trigger multiple size rotations
        for (int i = 0; i < 20; i++) {
            handler.publish(new LogRecord(Level.INFO, "Msg" + i));
        }
        handler.flush();
        handler.close();

        File parent = tempDir.toFile();
        String prefix = "zerolog" + dateSuffix + "_";
        File[] archives = parent.listFiles((d, name) -> name.startsWith(prefix));
        assertTrue(archives != null && archives.length == 0,
                "Expected no archived files when maxLogRotations=0, but found: " + Arrays.toString(parent.list()));

        // Active file should still exist
        Path active = tempDir.resolve("zerolog");
        assertTrue(Files.exists(active), "Active log file should exist");
    }

    @Test
    public void testMaxLogRotationsUnlimited() throws Exception {
        LocalDate today = LocalDate.now();
        String dateSuffix = today.format(DateTimeFormatter.ofPattern(DATE_PATTERN));

        String base = tempDir.resolve("unlimited").toString();
        // maxLogRotations = -1 => unlimited archives
        DailyRollingFileHandler handler = new DailyRollingFileHandler(base, 50, true, DATE_PATTERN, -1);
        handler.setFormatter(new SimpleFormatter());

        // Trigger multiple rotations
        for (int i = 0; i < 20; i++) {
            handler.publish(new LogRecord(Level.INFO, "UnlimitedMsg" + i));
        }
        handler.flush();
        handler.close();

        File parent = tempDir.toFile();
        String prefix = "unlimited" + dateSuffix + "_";
        File[] archives = parent.listFiles((d, name) -> name.startsWith(prefix));
        assertTrue(archives != null && archives.length > 10,
                "Expected more than 2 archived files when maxLogRotations=-1 (unlimited), but found: " + Arrays.toString(parent.list()));

        // Active file still exists
        Path active = tempDir.resolve("unlimited");
        assertTrue(Files.exists(active), "Active log file should exist");
    }

    @Test
    public void testDailyRotationMultipleDays() throws Exception {
        // Simulate 3 consecutive days of logging
        LocalDate baseDate = LocalDate.now().minusDays(2);
        String base = tempDir.resolve("multiday").toString();
        DailyRollingFileHandler handler = new DailyRollingFileHandler(base, Integer.MAX_VALUE, true, DATE_PATTERN, -1);
        handler.setFormatter(new SimpleFormatter());

        // Write a log for each simulated day
        for (int i = 0; i < 3; i++) {
            LocalDate simulatedDate = baseDate.plusDays(i);
            // Set the handler's currentDate to the simulated previous day to force rollover
            if (i > 0) {
                Field dateField = DailyRollingFileHandler.class.getDeclaredField("currentDate");
                dateField.setAccessible(true);
                dateField.set(handler, simulatedDate.minusDays(1));
            }
            // Publish a record for this day
            String message = "DayMsg" + i;
            handler.publish(new LogRecord(Level.INFO, message));
            handler.flush();
        }
        handler.close();

        // Verify that archives exist for the first two days, and active for the last day
        for (int i = 0; i < 2; i++) {
            LocalDate date = baseDate.plusDays(i);
            String suffix = date.format(DateTimeFormatter.ofPattern(DATE_PATTERN));
            Path archived = tempDir.resolve("multiday" + suffix);
            assertTrue(Files.exists(archived), "Expected archive for date=" + date + " to exist");
            String content = Files.readString(archived);
            assertTrue(content.contains("DayMsg" + i),
                    "Archive " + archived.getFileName() + " should contain DayMsg" + i);
        }
        // Active file should contain the message for the third day
        Path active = tempDir.resolve("multiday");
        assertTrue(Files.exists(active), "Active file for last day should exist");
        String activeContent = Files.readString(active);
        assertTrue(activeContent.contains("DayMsg2"), "Active log should contain DayMsg2");
    }


    @Test
    public void testDailyRetentionSevenDays() throws Exception {
        LocalDate today = LocalDate.now();
        // simulate 10 days ago as starting point
        LocalDate startDate = today.minusDays(9);
        String base = tempDir.resolve("sevenday").toString();
        DailyRollingFileHandler handler = new DailyRollingFileHandler(base, Integer.MAX_VALUE, true, DATE_PATTERN, 7);
        handler.setFormatter(new SimpleFormatter());

        Field dateField = DailyRollingFileHandler.class.getDeclaredField("currentDate");
        dateField.setAccessible(true);
        // initialize to day before startDate
        dateField.set(handler, startDate.minusDays(1));

        // simulate 10 consecutive days of logging
        for (int i = 0; i < 10; i++) {
            LocalDate simDate = startDate.plusDays(i);
            // force rollover by setting to previous day
            dateField.set(handler, simDate.minusDays(1));
            handler.publish(new LogRecord(Level.INFO, "Day" + i));
            handler.flush();
        }
        handler.close();

        File parent = tempDir.toFile();
        // archives are named sevenday.<date>
        File[] archives = parent.listFiles((d, name) -> name.startsWith("sevenday."));
        assertTrue(archives != null && archives.length == 7,
                "Expected exactly 7 archived files when maxLogRotations=7, but found: " + Arrays.toString(parent.list()));

        // ensure active file exists
        Path active = tempDir.resolve("sevenday");
        assertTrue(Files.exists(active), "Active log file should exist");
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
