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
//import ch.systemsx.cisd.common.logging.LogLevel;
//import ch.systemsx.cisd.common.logging.ConditionalNotificationLogger;
//
//public class ConditionalNotificationLoggerTest {
//    private TestAppender normalAppender;
//    private TestAppender notificationAppender;
//    private Logger normalLogger;
//    private Logger notificationLogger;
//
//    @BeforeMethod
//    public void setUp() {
//        normalAppender = new TestAppender();
//        notificationAppender = new TestAppender();
//        normalLogger = Logger.getLogger("normalLogger");
//        normalLogger.removeAllAppenders();
//        normalLogger.addAppender(normalAppender);
//        notificationLogger = Logger.getLogger("notificationLogger");
//        notificationLogger.removeAllAppenders();
//        notificationLogger.addAppender(notificationAppender);
//        notificationLogger.setLevel(Level.INFO);
//    }
//
//    @AfterMethod
//    public void tearDown() {
//        normalLogger.removeAllAppenders();
//        notificationLogger.removeAllAppenders();
//    }
//
//    public static class TestAppender extends AppenderSkeleton {
//        private List<LoggingEvent> events = new ArrayList<>();
//        @Override
//        protected void append(LoggingEvent event) {
//            events.add(event);
//        }
//        public List<LoggingEvent> getEvents() {
//            return events;
//        }
//        @Override
//        public void close() {}
//        @Override
//        public boolean requiresLayout() {
//            return false;
//        }
//    }
//
//    @Test
//    public void testErrorBelowThreshold() {
//        int threshold = 2;
//        ConditionalNotificationLogger logger = new ConditionalNotificationLogger(normalLogger, notificationLogger, threshold);
//        logger.log(LogLevel.ERROR, "Error1");
//        Assert.assertEquals(normalAppender.getEvents().size(), 1);
//        Assert.assertEquals(notificationAppender.getEvents().size(), 0);
//    }
//
//    @Test
//    public void testNotificationTriggered() {
//        int threshold = 2;
//        ConditionalNotificationLogger logger = new ConditionalNotificationLogger(normalLogger, notificationLogger, threshold);
//        logger.log(LogLevel.ERROR, "Error1");
//        logger.log(LogLevel.ERROR, "Error2");
//        logger.log(LogLevel.ERROR, "Error3");
//        logger.log(LogLevel.ERROR, "Error4");
//        Assert.assertEquals(normalAppender.getEvents().size(), 2);
//        Assert.assertEquals(notificationAppender.getEvents().size(), 1);
//        LoggingEvent notifEvent = notificationAppender.getEvents().get(0);
//        Assert.assertEquals(notifEvent.getRenderedMessage(), "Error3");
//    }
//
//    @Test
//    public void testResetSendsInfo() {
//        int threshold = 1;
//        ConditionalNotificationLogger logger = new ConditionalNotificationLogger(normalLogger, notificationLogger, threshold);
//        logger.log(LogLevel.ERROR, "Error1");
//        logger.log(LogLevel.ERROR, "Error2");
//        Assert.assertEquals(notificationAppender.getEvents().size(), 1);
//        notificationAppender.getEvents().clear();
//        logger.reset("ResetInfo");
//        Assert.assertEquals(notificationAppender.getEvents().size(), 1);
//        LoggingEvent infoEvent = notificationAppender.getEvents().get(0);
//        Assert.assertEquals(infoEvent.getLevel(), Level.INFO);
//        Assert.assertEquals(infoEvent.getRenderedMessage(), "ResetInfo");
//        logger.log(LogLevel.ERROR, "Error3");
//        Assert.assertEquals(notificationAppender.getEvents().size(), 1);
//    }
//
//    @Test
//    public void testNonErrorLogGoesToNormal() {
//        int threshold = 1;
//        ConditionalNotificationLogger logger = new ConditionalNotificationLogger(normalLogger, notificationLogger, threshold);
//        logger.log(LogLevel.INFO, "InfoMessage");
//        Assert.assertEquals(normalAppender.getEvents().size(), 1);
//        Assert.assertEquals(notificationAppender.getEvents().size(), 0);
//    }
//}
