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

package ch.systemsx.cisd.common.logging.configtest;

import ch.systemsx.cisd.common.logging.LogCategory;
import ch.systemsx.cisd.common.logging.LogFactory;
import ch.systemsx.cisd.common.logging.LogInitializer;
import ch.systemsx.cisd.common.logging.ext.DailyRollingFileHandler;
import ch.systemsx.cisd.common.logging.ext.PatternFormatter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.Assert;

import java.io.*;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.*;
import java.util.stream.Stream;

public class LoggingConfigTest {

    @BeforeMethod
    public void setUp() throws IOException {
        Path tempDir = Path.of("logs");
        if (Files.notExists(tempDir)) {
            Files.createDirectory(tempDir);
        }
        LogInitializer.configureFromFile(new File("/ch/systemsx/cisd/common/logging/configtest/testlogging.properties"));

    }

    @Test
    public void testAllFilesAreCreatedAndContainMessages() throws IOException {
        // fire log events
        Logger root     = Logger.getLogger("");
//        Logger auth     = Logger.getLogger("AUTH");
//        Logger tracking = Logger.getLogger("TRACKING");
//        Logger access   = Logger.getLogger("ACCESS");

        final org.apache.log4j.Logger
                auth = LogFactory.getLogger(LogCategory.AUTH,
                LoggingConfigTest.class);

        final org.apache.log4j.Logger tracking = LogFactory.getLogger(LogCategory.TRACKING,
                LoggingConfigTest.class);

        final org.apache.log4j.Logger access = LogFactory.getLogger(LogCategory.ACCESS,
                LoggingConfigTest.class);

        root.info("root-hello");
        auth.info("auth-hello");
        tracking.info("track-hello");
        access.info("access-hello");

        // flush to ensure write-through
        for (Handler h : LogManager.getLogManager().getLogger("" ).getHandlers()) {
            h.flush();
        }

        // verify files exist
        Path defaultLog = Path.of("logs/openbis.log");
        Path authLog    = Path.of("logs/openbis_auth.log");
        Path usageLog   = Path.of("logs/openbis_usage.log");
        Path parentDir = defaultLog.getParent();
        String dirListing = Arrays.toString(parentDir.toFile().list());

        boolean existsDefault = Files.exists(defaultLog);
        Assert.assertTrue(
                existsDefault,
                String.format(
                        "Expected default log <%s> to exist, but it does not.%n" +
                                "Directory listing (%s): %s",
                        defaultLog.getFileName(),
                        defaultLog.getParent().toAbsolutePath(),
                        dirListing
                )
        );

        boolean existsAuth = Files.exists(authLog);
        Assert.assertTrue(
                existsAuth,
                String.format(
                        "Expected auth log <%s> to exist, but it does not.%n" +
                                "Directory listing (%s): %s",
                        authLog.getFileName(),
                        parentDir.toAbsolutePath(),
                        dirListing
                )
        );

        boolean existsUsage = Files.exists(usageLog);
        Assert.assertTrue(
                existsUsage,
                String.format(
                        "Expected usage log <%s> to exist, but it does not.%n" +
                                "Directory listing (%s): %s",
                        usageLog.getFileName(),
                        parentDir.toAbsolutePath(),
                        dirListing
                )
        );

        // 2) Verify each file’s contents, and if a substring is missing, show the entire content
        String defContent   = Files.readString(defaultLog);
        String authContent  = Files.readString(authLog);
        String usageContent = Files.readString(usageLog);

        Assert.assertTrue(
                defContent.contains("root-hello"),
                String.format(
                        "Expected default log <%s> to contain \"root-hello\", but its contents were:%n---%n%s%n---",
                        defaultLog.getFileName(),
                        defContent.isEmpty() ? "(file is empty)" : defContent
                )
        );

        Assert.assertTrue(
                authContent.contains("auth-hello"),
                String.format(
                        "Expected auth log <%s> to contain \"auth-hello\", but its contents were:%n---%n%s%n---",
                        authLog.getFileName(),
                        authContent.isEmpty() ? "(file is empty)" : authContent
                )
        );

        Assert.assertTrue(
                usageContent.contains("track-hello"),
                String.format(
                        "Expected usage log <%s> to contain \"track-hello\", but its contents were:%n---%n%s%n---",
                        usageLog.getFileName(),
                        usageContent.isEmpty() ? "(file is empty)" : usageContent
                )
        );

        Assert.assertTrue(
                usageContent.contains("access-hello"),
                String.format(
                        "Expected usage log <%s> to contain \"access-hello\", but its contents were:%n---%n%s%n---",
                        usageLog.getFileName(),
                        usageContent
                )
        );

    }

    @AfterMethod
    public void tearDown() throws IOException {
        Path logsDir = Paths.get("logs");
        if (Files.exists(logsDir)) {
            // Walk the file tree, delete children before parents
            try (Stream<Path> paths = Files.walk(logsDir)) {
                paths
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                // log and swallow or rethrow as needed
                                System.err.println("Failed to delete " + path + ": " + e.getMessage());
                            }
                        });
            }
        }
    }
}
