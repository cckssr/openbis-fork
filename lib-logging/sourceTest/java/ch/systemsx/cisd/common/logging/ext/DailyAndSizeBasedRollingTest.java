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

package ch.systemsx.cisd.common.logging.ext;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Minimal test that verifies:
 *  – size‑rotated archives of a previous day are removed when a daily rollover occurs
 *    and <code>maxLogRotations = 0</code>.
 */
public class DailyAndSizeBasedRollingTest
{

    private static final String DATE_PATTERN = ".yyyy-MM-dd";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private Path tempDir;

    @BeforeMethod
    public void setUp() throws IOException {
        tempDir = Path.of("log-deltest");
        if (Files.notExists(tempDir)) {
            Files.createDirectory(tempDir);
        }
    }

    @AfterMethod
    public void tearDown() throws IOException {
        Files.walk(tempDir)
             .sorted(java.util.Comparator.reverseOrder())
             .map(Path::toFile)
             .forEach(File::delete);
    }

    @Test
    public void testOldSizeArchivesRemovedOnDailyRollover() throws Exception {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        String yesterdaySuffix = "." + yesterday.format(DATE_FMT);

        String baseName = "dailyclean";
        Path   basePath = tempDir.resolve(baseName);

        // ---- step 1: create fake size‑rotated logs for yesterday ----
        for (int i = 1; i <= 3; i++) {
            Path p = tempDir.resolve(baseName + yesterdaySuffix + "_" + i);
            Files.writeString(p, "old", StandardCharsets.UTF_8);
        }
        // make sure they exist
        File[] pre = tempDir.toFile().listFiles((d, n) -> n.startsWith(baseName + yesterdaySuffix + "_"));
        assertEquals(pre == null ? 0 : pre.length, 3, "Pre‑condition: expected 3 size archives for yesterday");

        // ---- step 2: create handler (cap = 0 → everything deleted) and force its date to yesterday ----
        DailyRollingFileHandler handler = new DailyRollingFileHandler(basePath.toString(), Integer.MAX_VALUE, true, DATE_PATTERN, 0);
        handler.setFormatter(new SimpleFormatter());
        handler.setCurrentDate(yesterday);

        // ---- step 3: publish one record today, triggering daily rollover ----
        handler.publish(new LogRecord(Level.INFO, "today"));
        handler.flush();
        handler.close();

        // ---- step 4: assert that NO yesterday size archives remain ----
        File[] post = tempDir.toFile().listFiles((d, n) -> n.startsWith(baseName + yesterdaySuffix + "_"));
        assertTrue(post == null || post.length == 0,
                   "Expected yesterday's size archives to be deleted, but found: " + Arrays.toString(tempDir.toFile().list()));

        // and active file for today exists & contains message
        assertTrue(Files.exists(basePath), "Active log file should exist after rollover");
        assertTrue(Files.readString(basePath).contains("today"));
    }
}
