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
//import ch.systemsx.cisd.common.logging.BundledTimestampTriggeringEventEvaluator;
//import org.testng.Assert;
//import org.testng.annotations.AfterMethod;
//import org.testng.annotations.BeforeMethod;
//import org.testng.annotations.Test;
//import org.apache.log4j.Level;
//import org.apache.log4j.Logger;
//import org.apache.log4j.spi.LoggingEvent;
//
//public class BundledTimestampTriggeringEventEvaluatorTest {
//
//    private long originalTimePeriod;
//
//    @BeforeMethod
//    public void setUp() {
//        originalTimePeriod = BundledTimestampTriggeringEventEvaluator.timePeriod;
//    }
//
//    @AfterMethod
//    public void tearDown() {
//        BundledTimestampTriggeringEventEvaluator.timePeriod = originalTimePeriod;
//    }
//
//    private LoggingEvent createDummyLoggingEvent() {
//        return new LoggingEvent("dummyFQN", Logger.getLogger("dummyLogger"), Level.INFO, "Test message", null);
//    }
//
//    @Test
//    public void testNoTriggerImmediately() {
//        BundledTimestampTriggeringEventEvaluator.timePeriod = 1000;
//        BundledTimestampTriggeringEventEvaluator evaluator = new BundledTimestampTriggeringEventEvaluator();
//        LoggingEvent event = createDummyLoggingEvent();
//        Assert.assertFalse(evaluator.isTriggeringEvent(event), "Trigger should not occur immediately");
//    }
//
//    @Test
//    public void testTriggerAfterTimePeriod() throws InterruptedException {
//        BundledTimestampTriggeringEventEvaluator.timePeriod = 50;
//        BundledTimestampTriggeringEventEvaluator evaluator = new BundledTimestampTriggeringEventEvaluator();
//        LoggingEvent event = createDummyLoggingEvent();
//        Thread.sleep(60);
//        Assert.assertTrue(evaluator.isTriggeringEvent(event), "Trigger should occur after the time period has passed");
//    }
//
//    @Test
//    public void testResetAfterTrigger() throws InterruptedException {
//        BundledTimestampTriggeringEventEvaluator.timePeriod = 50;
//        BundledTimestampTriggeringEventEvaluator evaluator = new BundledTimestampTriggeringEventEvaluator();
//        LoggingEvent event = createDummyLoggingEvent();
//        Thread.sleep(60);
//        Assert.assertTrue(evaluator.isTriggeringEvent(event), "Trigger should occur after time period");
//        Assert.assertFalse(evaluator.isTriggeringEvent(event), "Subsequent call should not trigger immediately");
//    }
//}