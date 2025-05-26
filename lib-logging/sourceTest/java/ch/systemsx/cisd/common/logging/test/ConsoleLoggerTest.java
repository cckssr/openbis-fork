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

package ch.systemsx.cisd.common.logging.test;

import org.testng.Assert;
import org.testng.annotations.Test;
import ch.systemsx.cisd.common.logging.ConsoleLogger;
import ch.systemsx.cisd.common.logging.LogLevel;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class ConsoleLoggerTest {
    @Test
    public void testLogWithoutThrowable() {
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(outContent);
        ConsoleLogger logger = new ConsoleLogger(ps);
        logger.log(LogLevel.INFO, "Test message");
        String expected = LogLevel.INFO.toString() + ": " + "Test message" + System.lineSeparator();
        Assert.assertEquals(outContent.toString(), expected);
    }

    @Test
    public void testLogWithThrowable() {
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(outContent);
        ConsoleLogger logger = new ConsoleLogger(ps);
        Exception ex = new Exception("Test exception");
        logger.log(LogLevel.ERROR, "Error occurred", ex);
        String output = outContent.toString();
        String expectedPrefix = LogLevel.ERROR.toString() + ": " + "Error occurred" + System.lineSeparator();
        Assert.assertTrue(output.startsWith(expectedPrefix));
        Assert.assertTrue(output.contains("java.lang.Exception: Test exception"));
    }
}
