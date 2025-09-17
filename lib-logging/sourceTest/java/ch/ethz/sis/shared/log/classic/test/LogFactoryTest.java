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

package ch.ethz.sis.shared.log.classic.test;

import org.testng.Assert;
import org.testng.annotations.Test;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.Logger;

public class LogFactoryTest {
    static class Dummy {}

    @Test
    public void testGetLoggerNameWithCategoryAndClass() {
        String expected = LogCategory.OPERATION.name() + "." + Dummy.class.getSimpleName();
        String result = LogFactory.getLoggerName(LogCategory.OPERATION, Dummy.class);
        Assert.assertEquals(result, expected);
    }

    @Test
    public void testGetLoggerNameWithAdminCategory() {
        String expected = LogCategory.AUTH.name();
        String result = LogFactory.getLoggerName(LogCategory.AUTH);
        Assert.assertEquals(result, expected);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testGetLoggerNameWithNonAdminCategory() {
        LogFactory.getLoggerName(LogCategory.OPERATION);
    }

    @Test
    public void testGetLoggerWithCategoryAndClass() {
        String expected = LogCategory.MACHINE.name() + "." + Dummy.class.getSimpleName();
        Logger logger = LogFactory.getLogger(LogCategory.MACHINE, Dummy.class);
        Assert.assertEquals(logger.getName(), expected);
    }

    @Test
    public void testGetLoggerWithAdminCategory() {
        String expected = LogCategory.TRACKING.name();
        Logger logger = LogFactory.getLogger(LogCategory.TRACKING);
        Assert.assertEquals(logger.getName(), expected);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testGetLoggerWithNonAdminCategory() {
        LogFactory.getLogger(LogCategory.NOTIFY);
    }
}
