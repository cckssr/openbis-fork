package ch.systemsx.cisd.common.logging;

import ch.systemsx.cisd.common.logging.ext.NamedFileHandler;
import ch.systemsx.cisd.common.logging.ext.PatternFormatter;
import org.apache.log4j.Logger;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.Assert;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

/**
 * TestNG test for NamedFileHandler to verify that the log file is created
 * and contains the expected log message.
 */
public class NameFileHandlerTest {

    private static final String LOG_FILE = "test.log";

    @BeforeMethod
    public void deletePreexistingLog() {
        cleanup();
    }

    @AfterMethod
    public void removeTestLog() {
        cleanup();
    }

    public void cleanup() {
        // Remove existing log file before each test
        File file = new File(LOG_FILE);
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    public void testLogFileCreatedAndContainsMessage() throws IOException {
        // Configure logger
        Logger logger = Logger.getLogger(
                LogFactory.getLoggerName(LogCategory.OPERATION, NameFileHandlerTest.class)
                        + ".DS1"
        );
        NamedFileHandler handler = new NamedFileHandler("name", LOG_FILE, true);
        handler.setFormatter(new PatternFormatter("%d %-5p %m%n"));
        logger.addHandler(handler);
        logger.getJulLogger().setUseParentHandlers(false);

        // Log a test message
        String message = "hello";
        logger.info(message);

        // Assert log file was created
        File file = new File(LOG_FILE);
        Assert.assertTrue(file.exists(), "Log file should have been created.");

        // Read file content and assert it contains the message
        String content = new String(
                Files.readAllBytes(file.toPath()),
                StandardCharsets.UTF_8
        );
        Assert.assertTrue(
                content.contains(message),
                "Log file should contain the logged message."
        );
    }
}
