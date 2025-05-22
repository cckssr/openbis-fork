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
//import org.apache.log4j.AppenderSkeleton;
//import org.apache.log4j.spi.LoggingEvent;
//import java.util.ArrayList;
//import java.util.List;
//import ch.systemsx.cisd.common.logging.LogUtils;
//
//public class LogUtilsTest {
//    private Logger logger;
//    private TestAppender appender;
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
//        public void close() {}
//        @Override
//        public boolean requiresLayout() {
//            return false;
//        }
//    }
//
//    @BeforeMethod
//    public void setUp() {
//        logger = Logger.getLogger("TestLogger");
//        //logger.removeAllAppenders();
//        appender = new TestAppender();
//        //logger.addAppender(appender);
//    }
//
//    @AfterMethod
//    public void tearDown() {
//        //logger.removeAppender(appender);
//    }
//
//    @Test(expectedExceptions = AssertionError.class)
//    public void testLogErrorWithFailingAssertionThrowsAssertion() {
//        LogUtils.logErrorWithFailingAssertion(logger, "Error occurred");
//    }
//
//    @Test
//    public void testLogErrorWithFailingAssertionLogsError() {
//        try {
//            LogUtils.logErrorWithFailingAssertion(logger, "Error occurred");
//        } catch (AssertionError e) {
//        }
//        Assert.assertFalse(appender.getEvents().isEmpty());
//        Assert.assertEquals(appender.getEvents().get(0).getRenderedMessage(), "Error occurred");
//    }
//
//    @Test
//    public void testRemoveEmbeddedStackTrace() {
//        String input = "An error happened\n\tat com.example.MyClass.method(MyClass.java:10)\nCaused by: NullPointerException\n\tat com.example.MyClass.otherMethod(MyClass.java:20)";
//        String expected = "An error happened\nCaused by: NullPointerException";
//        String result = LogUtils.removeEmbeddedStackTrace(input);
//        Assert.assertEquals(result, expected);
//    }
//}
