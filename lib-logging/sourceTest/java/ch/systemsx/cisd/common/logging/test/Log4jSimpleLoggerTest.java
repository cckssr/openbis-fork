///*
// *  Copyright ETH 2025 Zürich, Scientific IT Services
// *
// *  Licensed under the Apache License, Version 2.0 (the "License");
// *  you may not use this file except in compliance with the License.
// *  You may obtain a copy of the License at
// *
// *       http://www.apache.org/licenses/LICENSE-2.0
// *
// *  Unless required by applicable law or agreed to in writing, software
// *  distributed under the License is distributed on an "AS IS" BASIS,
// *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// *  See the License for the specific language governing permissions and
// *  limitations under the License.
// *
// */
//
//package ch.systemsx.cisd.common.logging.test;
//
//import org.testng.Assert;
//import org.testng.annotations.Test;
//import org.testng.annotations.BeforeMethod;
//import org.testng.annotations.AfterMethod;
//import org.apache.log4j.Logger;
//import org.apache.log4j.Level;
//import org.apache.log4j.spi.LoggingEvent;
//import org.apache.log4j.AppenderSkeleton;
//import java.util.ArrayList;
//import java.util.List;
//import ch.systemsx.cisd.common.logging.Log4jSimpleLogger;
//import ch.systemsx.cisd.common.logging.LogLevel;
//import org.apache.log4j.Priority;
//
//public class Log4jSimpleLoggerTest {
//    private Logger testLogger;
//    private TestAppender appender;
//
//    @BeforeMethod
//    public void setUp() {
//        testLogger = Logger.getLogger("TestLogger");
//        //testLogger.removeAllAppenders();
//        //appender = new TestAppender();
//        //testLogger.addAppender(appender);
//        testLogger.setLevel(Level.ALL);
//    }
//
//    @AfterMethod
//    public void tearDown() {
//        //testLogger.removeAppender(appender);
//    }
//
//    private static class TestAppender extends AppenderSkeleton {
//        private final List<LoggingEvent> events = new ArrayList<>();
//        @Override
//        protected void append(LoggingEvent event) {
//            events.add(event);
//        }
//        public List<LoggingEvent> getEvents() {
//            return events;
//        }
//        @Override
//        public void close() {
//        }
//        @Override
//        public boolean requiresLayout() {
//            return false;
//        }
//    }
//
//    @Test
//    public void testLogWithoutOverride() {
//        Log4jSimpleLogger logger = new Log4jSimpleLogger(testLogger);
//        logger.log(LogLevel.INFO, "Info message");
//        List<LoggingEvent> events = appender.getEvents();
//        Assert.assertEquals(events.size(), 1);
//        LoggingEvent event = events.get(0);
//        Assert.assertEquals(event.getLevel(), Level.INFO);
//        Assert.assertEquals(event.getRenderedMessage(), "Info message");
//    }
//
//    @Test
//    public void testLogWithOverride() {
//        Log4jSimpleLogger logger = new Log4jSimpleLogger(testLogger, Level.ERROR);
//        logger.log(LogLevel.DEBUG, "Debug message");
//        List<LoggingEvent> events = appender.getEvents();
//        Assert.assertEquals(events.size(), 1);
//        LoggingEvent event = events.get(0);
//        Assert.assertEquals(event.getLevel(), Level.ERROR);
//        Assert.assertEquals(event.getRenderedMessage(), "Debug message");
//    }
//
//    @Test
//    public void testLogWithThrowable() {
//        Log4jSimpleLogger logger = new Log4jSimpleLogger(testLogger);
//        Exception ex = new Exception("Test exception");
//        logger.log(LogLevel.WARN, "Warning message", ex);
//        List<LoggingEvent> events = appender.getEvents();
//        Assert.assertEquals(events.size(), 1);
//        LoggingEvent event = events.get(0);
//        Assert.assertEquals(event.getLevel(), Level.WARN);
//        Assert.assertEquals(event.getRenderedMessage(), "Warning message");
//        Assert.assertNotNull(event.getThrowableInformation());
//        String[] throwableStrRep = event.getThrowableStrRep();
//        boolean found = false;
//        for (String line : throwableStrRep) {
//            if (line.contains("Test exception")) {
//                found = true;
//                break;
//            }
//        }
//        Assert.assertTrue(found);
//    }
//}
