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
//import org.apache.log4j.AppenderSkeleton;
//import org.apache.log4j.Level;
//import org.apache.log4j.Logger;
//import org.apache.log4j.spi.LoggingEvent;
//import org.testng.Assert;
//import org.testng.annotations.AfterMethod;
//import org.testng.annotations.BeforeMethod;
//import org.testng.annotations.Test;
//import java.lang.reflect.Proxy;
//import java.util.ArrayList;
//import java.util.List;
//import ch.systemsx.cisd.common.logging.LogAnnotation;
//import ch.systemsx.cisd.common.logging.LogCategory;
//import ch.systemsx.cisd.common.logging.LogFactory;
//import ch.systemsx.cisd.common.logging.LogInvocationHandler;
//import ch.systemsx.cisd.common.logging.LogLevel;
//
//public class LogInvocationHandlerTest {
//
//    private static class TestAppender extends AppenderSkeleton {
//        List<LoggingEvent> events = new ArrayList<>();
//        @Override
//        protected void append(LoggingEvent event) {
//            events.add(event);
//        }
//        @Override
//        public void close() {}
//        @Override
//        public boolean requiresLayout() {
//            return false;
//        }
//    }
//
//    public interface TestService {
//        String sayHello(String name);
//        @LogAnnotation(logCategory = LogCategory.AUTH, logLevel = LogLevel.ERROR)
//        String sayGoodbye(String name);
//        void errorProne();
//    }
//
//    public static class TestServiceImpl implements TestService {
//        public String sayHello(String name) {
//            return "Hello " + name;
//        }
//        public String sayGoodbye(String name) {
//            return "Goodbye " + name;
//        }
//        public void errorProne() {
//            throw new RuntimeException("Test exception");
//        }
//    }
//
//    private TestAppender testAppender;
//    private Logger opLogger;
//    private Logger authLogger;
//
//    @BeforeMethod
//    public void setUp() {
//        testAppender = new TestAppender();
//        opLogger = Logger.getLogger(LogFactory.getLoggerName(LogCategory.OPERATION, TestService.class));
//        //opLogger.addAppender(testAppender);
//        authLogger = Logger.getLogger(LogFactory.getLoggerName(LogCategory.AUTH, TestService.class));
//        //authLogger.addAppender(testAppender);
//    }
//
//    @AfterMethod
//    public void tearDown() {
//        //opLogger.removeAppender(testAppender);
//        //authLogger.removeAppender(testAppender);
//    }
//
//    @Test
//    public void testSuccessfulInvocationWithoutAnnotationOnlyIfAnnotatedFalse() {
//        TestServiceImpl target = new TestServiceImpl();
//        LogInvocationHandler handler = new LogInvocationHandler(target, "dummy", Level.INFO, TestService.class, false);
//        TestService proxy = (TestService) Proxy.newProxyInstance(TestService.class.getClassLoader(), new Class[] { TestService.class }, handler);
//        String result = proxy.sayHello("World");
//        Assert.assertEquals(result, "Hello World");
//        Assert.assertFalse(testAppender.events.isEmpty());
//        String message = testAppender.events.get(0).getRenderedMessage();
//        Assert.assertTrue(message.contains("Successful invocation of dummy.sayHello(World) took"));
//    }
//
//    @Test
//    public void testInvocationWithoutAnnotationOnlyIfAnnotatedTrue() {
//        TestServiceImpl target = new TestServiceImpl();
//        LogInvocationHandler handler = new LogInvocationHandler(target, "dummy", Level.INFO, TestService.class, true);
//        TestService proxy = (TestService) Proxy.newProxyInstance(TestService.class.getClassLoader(), new Class[] { TestService.class }, handler);
//        String result = proxy.sayHello("World");
//        Assert.assertEquals(result, "Hello World");
//        Assert.assertTrue(testAppender.events.isEmpty());
//    }
//
//    @Test
//    public void testAnnotatedInvocationWithOnlyIfAnnotatedTrue() {
//        TestServiceImpl target = new TestServiceImpl();
//        LogInvocationHandler handler = new LogInvocationHandler(target, "dummy", Level.INFO, TestService.class, true);
//        TestService proxy = (TestService) Proxy.newProxyInstance(TestService.class.getClassLoader(), new Class[] { TestService.class }, handler);
//        String result = proxy.sayGoodbye("Alice");
//        Assert.assertEquals(result, "Goodbye Alice");
//        Assert.assertFalse(testAppender.events.isEmpty());
//        String message = testAppender.events.get(0).getRenderedMessage();
//        Assert.assertTrue(message.contains("Successful invocation of dummy.sayGoodbye(Alice) took"));
//    }
//
//    @Test
//    public void testInvocationWithException() {
//        TestServiceImpl target = new TestServiceImpl();
//        LogInvocationHandler handler = new LogInvocationHandler(target, "dummy", Level.INFO, TestService.class, false);
//        TestService proxy = (TestService) Proxy.newProxyInstance(TestService.class.getClassLoader(), new Class[] { TestService.class }, handler);
//        try {
//            proxy.errorProne();
//            Assert.fail("Expected exception not thrown");
//        } catch (RuntimeException e) {
//            Assert.assertEquals(e.getMessage(), "Test exception");
//        }
//        Assert.assertFalse(testAppender.events.isEmpty());
//        String message = testAppender.events.get(0).getRenderedMessage();
//        Assert.assertTrue(message.contains("Failed invocation of dummy.errorProne() took"));
//    }
//}
