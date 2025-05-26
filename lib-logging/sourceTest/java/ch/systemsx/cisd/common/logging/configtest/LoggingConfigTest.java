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

import ch.systemsx.cisd.common.logging.LogInitializer;
import ch.systemsx.cisd.common.logging.ext.DailyRollingFileHandler;
import ch.systemsx.cisd.common.logging.ext.PatternFormatter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.Assert;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
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
        Logger auth     = Logger.getLogger("AUTH");
        Logger tracking = Logger.getLogger("TRACKING");
        Logger access   = Logger.getLogger("ACCESS");

        root.info("root-hello");
        auth.info("auth-hello");
        tracking.info("track-hello");
        access.info("access-hello");

        // flush to ensure write-through
        for (Handler h : LogManager.getLogManager().getLogger("" ).getHandlers()) {
            h.flush();
        }

        // verify files exist
        Path defaultLog = Path.of("logs/openbis_log.txt");
        Path authLog    = Path.of("logs/openbis_auth_log.txt");
        Path usageLog   = Path.of("logs/openbis_usage_log.txt");

        Assert.assertTrue(Files.exists(defaultLog), "Default log not created");
        Assert.assertTrue(Files.exists(authLog),    "Auth log not created");
        Assert.assertTrue(Files.exists(usageLog),   "Usage log not created");

        // verify content
        String defContent   = Files.readString(defaultLog);
        String authContent  = Files.readString(authLog);
        String usageContent = Files.readString(usageLog);

        Assert.assertTrue(defContent.contains("root-hello"),   "root message missing in default log");
        Assert.assertTrue(authContent.contains("auth-hello"),  "auth message missing in auth log");
        Assert.assertTrue(usageContent.contains("track-hello"),  "tracking message missing in usage log");
        Assert.assertTrue(usageContent.contains("access-hello"), "access message missing in usage log");
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
                        && ((DailyRollingFileHandler)h).getLogFileNamePattern().contains("openbis_log.txt"));
        boolean hasConsole = Arrays.stream(rootHandlers)
                .anyMatch(h -> h instanceof ConsoleHandler);
        Assert.assertTrue(hasDefault, "DefaultFileHandler should be on root logger");
        Assert.assertTrue(hasConsole, "ConsoleHandler should be on root logger");

        // AUTH
        Handler[] authHandlers = Logger.getLogger("AUTH").getHandlers();
        Assert.assertEquals(authHandlers.length, 2, "AUTH should have 2 handlers");
        Assert.assertTrue(Arrays.stream(authHandlers)
                        .anyMatch(h -> h instanceof DailyRollingFileHandler
                                && ((DailyRollingFileHandler)h).getLogFileNamePattern().contains("openbis_auth_log.txt")),
                "AuthFileHandler missing on AUTH logger");
        Assert.assertTrue(Arrays.stream(authHandlers)
                        .anyMatch(h -> h instanceof DailyRollingFileHandler
                                && ((DailyRollingFileHandler)h).getLogFileNamePattern().contains("openbis_usage_log.txt")),
                "UsageFileHandler missing on AUTH logger");

        // TRACKING
        Handler[] trackHandlers = Logger.getLogger("TRACKING").getHandlers();
        Assert.assertEquals(trackHandlers.length, 1, "TRACKING should have exactly 1 handler");
        Assert.assertTrue(trackHandlers[0] instanceof DailyRollingFileHandler
                        && ((DailyRollingFileHandler)trackHandlers[0]).getLogFileNamePattern().contains("openbis_usage_log.txt"),
                "UsageFileHandler missing on TRACKING logger");

        // ACCESS
        Handler[] accessHandlers = Logger.getLogger("ACCESS").getHandlers();
        Assert.assertEquals(accessHandlers.length, 1, "ACCESS should have exactly 1 handler");
        Assert.assertTrue(accessHandlers[0] instanceof DailyRollingFileHandler
                        && ((DailyRollingFileHandler)accessHandlers[0]).getLogFileNamePattern().contains("openbis_usage_log.txt"),
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
