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

package ch.ethz.sis.shared.log.classic.configtest;

import ch.ethz.sis.shared.log.standard.utils.LogInitializer;
import ch.ethz.sis.shared.log.standard.handlers.DailyRollingFileHandler;
import ch.ethz.sis.shared.log.standard.handlers.PatternFormatter;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class LoggingHanldersConfigTest
{

    @BeforeMethod
    public void setUp() throws IOException {
        Path tempDir = Path.of("logs");
        if (Files.notExists(tempDir)) {
            Files.createDirectory(tempDir);
        }
        LogInitializer.configureFromFile(new File(
                "/ch/ethz/sis/shared/log/classic/configtest/testlogging.properties"));

    }


    /**
     * All handlers that have a messagePattern property configured
     * should be using PatternFormatter underneath.
     */
    @Test
    public void testAllHandlersUsePatternFormatter() {
        // logger names to check: root ("") plus the named ones
        String[] loggers = { "", "AUTH", "TRACKING", "ACCESS" };


        for (String loggerName : loggers) {
            Handler[] handlers = Logger.getLogger(loggerName).getHandlers();

            Assert.assertTrue(handlers.length > 0,
                    "Expected at least one handler on logger '" + loggerName + "'");
            for (Handler h : handlers) {
                Formatter fmt = h.getFormatter();
                Assert.assertNotNull(fmt,
                        "Formatter must not be null on handler " + h + " of logger " + loggerName);
                Assert.assertEquals(fmt.getClass(), PatternFormatter.class,
                        "Handler " + h + " on logger '" + loggerName +
                                "' should use PatternFormatter, but was " + fmt.getClass().getSimpleName());
            }
        }
    }

    @Test
    public void testHandlersAreAttached() {
        // root
        Handler[] rootHandlers = Logger.getLogger("").getHandlers();
        boolean hasDefault = Arrays.stream(rootHandlers)
                .anyMatch(h -> h instanceof DailyRollingFileHandler
                        && ((DailyRollingFileHandler)h).getLogFileName().contains("openbis.log"));
        boolean hasConsole = Arrays.stream(rootHandlers)
                .anyMatch(h -> h instanceof ConsoleHandler);
        Assert.assertTrue(hasDefault, "DefaultFileHandler should be on root logger");
        Assert.assertTrue(hasConsole, "ConsoleHandler should be on root logger");

        // AUTH
        Handler[] authHandlers = Logger.getLogger("AUTH").getHandlers();
        Assert.assertEquals(authHandlers.length, 2, "AUTH should have 2 handlers");
        Assert.assertTrue(Arrays.stream(authHandlers)
                        .anyMatch(h -> h instanceof DailyRollingFileHandler
                                && ((DailyRollingFileHandler)h).getLogFileName().contains("openbis_auth.log")),
                "AuthFileHandler missing on AUTH logger");
        Assert.assertTrue(Arrays.stream(authHandlers)
                        .anyMatch(h -> h instanceof DailyRollingFileHandler
                                && ((DailyRollingFileHandler)h).getLogFileName().contains("openbis_usage.log")),
                "UsageFileHandler missing on AUTH logger");

        // TRACKING
        Handler[] trackHandlers = Logger.getLogger("TRACKING").getHandlers();
        Assert.assertEquals(trackHandlers.length, 1, "TRACKING should have exactly 1 handler");
        Assert.assertTrue(trackHandlers[0] instanceof DailyRollingFileHandler
                        && ((DailyRollingFileHandler)trackHandlers[0]).getLogFileName().contains("openbis_usage.log"),
                "UsageFileHandler missing on TRACKING logger");

        // ACCESS
        Handler[] accessHandlers = Logger.getLogger("ACCESS").getHandlers();
        Assert.assertEquals(accessHandlers.length, 1, "ACCESS should have exactly 1 handler");
        Assert.assertTrue(accessHandlers[0] instanceof DailyRollingFileHandler
                        && ((DailyRollingFileHandler)accessHandlers[0]).getLogFileName().contains("openbis_usage.log"),
                "UsageFileHandler missing on ACCESS logger");
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
