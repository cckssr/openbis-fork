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
//import org.apache.log4j.MDC;
//import ch.systemsx.cisd.common.logging.LoggingContextHandler;
//import java.util.function.Supplier;
//
//public class LoggingContextHandlerTest {
//    private LoggingContextHandler handler;
//
//    @BeforeMethod
//    public void setUp() {
//        MDC.remove("contextInfo");
//        Supplier<String> supplier = () -> "127.0.0.1";
//        handler = new LoggingContextHandler(supplier);
//    }
//
//    @AfterMethod
//    public void tearDown() {
//        MDC.remove("contextInfo");
//    }
//
//    @Test
//    public void testSetMDCWithExistingContext() {
//        String contextID = "userSession";
//        String context = "user=testUser";
//        handler.addContext(contextID, context);
//        handler.setMDC(contextID);
//        String mdcValue = (String) MDC.get("contextInfo");
//        String expected = String.format(" {%s, ip=%s}", context, "127.0.0.1");
//        Assert.assertEquals(mdcValue, expected);
//    }
//
//    @Test
//    public void testSetMDCWithNonExistingContext() {
//        String contextID = "nonExistent";
//        handler.setMDC(contextID);
//        String mdcValue = (String) MDC.get("contextInfo");
//        String expected = String.format(" {UNKNOWN_TOKEN='%s', ip=%s}", contextID, "127.0.0.1");
//        Assert.assertEquals(mdcValue, expected);
//    }
//
//    @Test
//    public void testDestroyContext() {
//        String contextID = "temp";
//        String context = "tempContext";
//        handler.addContext(contextID, context);
//        handler.destroyContext(contextID);
//        handler.setMDC(contextID);
//        String mdcValue = (String) MDC.get("contextInfo");
//        String expected = String.format(" {UNKNOWN_TOKEN='%s', ip=%s}", contextID, "127.0.0.1");
//        Assert.assertEquals(mdcValue, expected);
//    }
//}
