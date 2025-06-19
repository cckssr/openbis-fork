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
//import java.io.File;
//import java.io.FileWriter;
//import java.lang.reflect.Field;
//import java.util.Enumeration;
//import org.apache.log4j.BasicConfigurator;
//import org.apache.log4j.Logger;
//import org.apache.log4j.Level;
//import ch.systemsx.cisd.common.logging.LogInitializer;
//
//public class LogInitializerTest {
//    @BeforeMethod
//    public void setUp() throws Exception {
//        System.clearProperty("log.configuration");
//        resetInitializedFlag();
//        //Logger.getRootLogger().removeAllAppenders();
//    }
//
//    private void resetInitializedFlag() throws Exception {
//        Field field = LogInitializer.class.getDeclaredField("initialized");
//        field.setAccessible(true);
//        field.setBoolean(null, false);
//    }
//
//    private boolean getInitialized() throws Exception {
//        Field field = LogInitializer.class.getDeclaredField("initialized");
//        field.setAccessible(true);
//        return field.getBoolean(null);
//    }
//
//    @Test
//    public void testDefaultInitialization() throws Exception {
//        LogInitializer.init();
//        Assert.assertTrue(getInitialized());
//        //Assert.assertTrue(Logger.getRootLogger().getAllAppenders().hasMoreElements());
//    }
//
//    @Test
//    public void testFileConfigurationInitialization() throws Exception {
//        String xmlConfig = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
//                "<!DOCTYPE log4j:configuration SYSTEM \"log4j.dtd\">\n" +
//                "<log4j:configuration xmlns:log4j=\"http://jakarta.apache.org/log4j/\">\n" +
//                "  <appender name=\"TestAppender\" class=\"org.apache.log4j.ConsoleAppender\">\n" +
//                "    <layout class=\"org.apache.log4j.PatternLayout\">\n" +
//                "      <param name=\"ConversionPattern\" value=\"%p - %m%n\"/>\n" +
//                "    </layout>\n" +
//                "  </appender>\n" +
//                "  <root>\n" +
//                "    <priority value=\"DEBUG\"/>\n" +
//                "    <appender-ref ref=\"TestAppender\"/>\n" +
//                "  </root>\n" +
//                "</log4j:configuration>";
//        File tempFile = File.createTempFile("log", ".xml");
//        try (FileWriter writer = new FileWriter(tempFile)) {
//            writer.write(xmlConfig);
//        }
//        System.setProperty("log.configuration", "file:" + tempFile.getAbsolutePath());
//        LogInitializer.init();
//        Assert.assertTrue(getInitialized());
//        Assert.assertEquals(Logger.getRootLogger().getLevel(), Level.DEBUG);
//        boolean found = false;
////        Enumeration<?> appenders = Logger.getRootLogger().getAllAppenders();
////        while (appenders.hasMoreElements()) {
////            Object appender = appenders.nextElement();
////            if (appender.getClass().getName().contains("ConsoleAppender")) {
////                found = true;
////                break;
////            }
////        }
//        Assert.assertTrue(found);
//        tempFile.delete();
//    }
//
//    @Test
//    public void testMultipleInitializationCalls() throws Exception {
//        LogInitializer.init();
//        boolean first = getInitialized();
//        LogInitializer.init();
//        boolean second = getInitialized();
//        Assert.assertTrue(first && second);
//    }
//}
