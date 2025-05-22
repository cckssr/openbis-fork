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
import java.lang.reflect.Method;
import ch.systemsx.cisd.common.logging.LogAnnotation;
import ch.systemsx.cisd.common.logging.LogCategory;
import ch.systemsx.cisd.common.logging.LogLevel;

public class LogAnnotationTest {
    static class Dummy {
        @LogAnnotation
        public void defaultMethod() {}

        @LogAnnotation(logCategory = LogCategory.AUTH, logLevel = LogLevel.ERROR)
        public void customMethod() {}
    }

    @Test
    public void testDefaultAnnotation() throws Exception {
        Method method = Dummy.class.getMethod("defaultMethod");
        LogAnnotation annotation = method.getAnnotation(LogAnnotation.class);
        Assert.assertNotNull(annotation);
        Assert.assertEquals(annotation.logCategory(), LogCategory.OPERATION);
        Assert.assertEquals(annotation.logLevel(), LogLevel.UNDEFINED);
    }

    @Test
    public void testCustomAnnotation() throws Exception {
        Method method = Dummy.class.getMethod("customMethod");
        LogAnnotation annotation = method.getAnnotation(LogAnnotation.class);
        Assert.assertNotNull(annotation);
        Assert.assertEquals(annotation.logCategory(), LogCategory.AUTH);
        Assert.assertEquals(annotation.logLevel(), LogLevel.ERROR);
    }
}
