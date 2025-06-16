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

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Tests the DailyRollingFileHandler's ability to manage a large number of
 * rotated files, ensuring that the `maxLogRotations` limit is strictly enforced.
 */
public class HighVolumeRotationTest
{
    private static final String DATE_PATTERN = ".yyyy-MM-dd";

    private Path tempDir;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN);

    @BeforeMethod
    public void setUp() throws IOException
    {

        tempDir = Files.createTempDirectory("log-test-high-volume");
    }

    @AfterMethod
    public void tearDown() throws IOException
    {

        if (tempDir != null && Files.exists(tempDir))
        {
            try (Stream<Path> walk = Files.walk(tempDir))
            {
                walk.sorted(java.util.Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        }
    }

    @Test
    public void testRotationKeepsLatestThirtyFilesFromLargeVolume() throws Exception
    {

        final int maxRotations = 30;
        final int filesToCreate = 50; // We will create 50 archives.

        String baseName = "high-volume.log";
        String logFilePath = tempDir.resolve(baseName).toString();
        LocalDate today = LocalDate.now();
        String dateSuffix = today.format(dateFormatter);

        DailyRollingFileHandler handler =
                new DailyRollingFileHandler(logFilePath, 100, true, DATE_PATTERN, maxRotations);
        handler.setFormatter(new SimpleFormatter());

        for (int i = 0; i <= filesToCreate; i++)
        {
            handler.publish(new LogRecord(Level.INFO, "Message number " + i));
            handler.flush(); // Flush to ensure file size is updated immediately.
        }
        handler.close();


        List<Path> archiveFiles = Files.list(tempDir)
                .filter(p -> !p.getFileName().toString().equals(baseName))
                .sorted()
                .collect(Collectors.toList());
        List<String> archiveFileNames = archiveFiles.stream()
                .map(p -> p.getFileName().toString())
                .collect(Collectors.toList());

        assertEquals(archiveFiles.size(), maxRotations,
                String.format("Expected exactly %d archived files, but found %d. Files found: %s",
                        maxRotations, archiveFiles.size(), archiveFileNames));
        assertTrue(Files.exists(tempDir.resolve(baseName)), "The active log file must always exist.");

        for (int i = 1; i <= 20; i++)
        {
            String deletedFileName = baseName + dateSuffix + "_" + i;
            Path deletedPath = tempDir.resolve(deletedFileName);
            assertFalse(Files.exists(deletedPath), "Expected oldest archive " + deletedFileName + " to be deleted, but it was found.");
        }

        for (int i = 21; i <= 50; i++)
        {
            String keptFileName = baseName + dateSuffix + "_" + i;
            Path keptPath = tempDir.resolve(keptFileName);
            assertTrue(Files.exists(keptPath), "Expected newest archive " + keptFileName + " to exist, but it was not found.");
        }
    }
}