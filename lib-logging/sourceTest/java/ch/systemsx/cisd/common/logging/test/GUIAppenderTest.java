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
//import javax.swing.JTextArea;
//import ch.systemsx.cisd.common.logging.GUIAppender;
//
//public class GUIAppenderTest {
//    private GUIAppender appender;
//
//    @BeforeMethod
//    public void setUp() {
//        appender = new GUIAppender(Level.DEBUG, "%m%n");
//    }
//
//    @AfterMethod
//    public void tearDown() {
//        Logger.getRootLogger().removeAppender(appender);
//    }
//
//    @Test
//    public void testLogMessageAppended() {
//        Logger logger = Logger.getLogger("TestLogger");
//        logger.info("Test message");
//        JTextArea area = appender.getTextArea();
//        String text = area.getText();
//        Assert.assertTrue(text.contains("Test message"));
//    }
//}
