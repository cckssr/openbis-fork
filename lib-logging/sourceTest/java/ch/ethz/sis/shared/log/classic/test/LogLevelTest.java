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
import ch.ethz.sis.shared.log.classic.core.LogLevel;

public class LogLevelTest {
    @Test
    public void testEnumValues() {
        LogLevel[] levels = LogLevel.values();
        Assert.assertEquals(levels.length, 7);
        Assert.assertEquals(levels[0], LogLevel.UNDEFINED);
        Assert.assertEquals(levels[1], LogLevel.OFF);
        Assert.assertEquals(levels[2], LogLevel.TRACE);
        Assert.assertEquals(levels[3], LogLevel.DEBUG);
        Assert.assertEquals(levels[4], LogLevel.INFO);
        Assert.assertEquals(levels[5], LogLevel.WARN);
        Assert.assertEquals(levels[6], LogLevel.ERROR);
    }

    @Test
    public void testEnumNames() {
        Assert.assertEquals(LogLevel.UNDEFINED.name(), "UNDEFINED");
        Assert.assertEquals(LogLevel.OFF.name(), "OFF");
        Assert.assertEquals(LogLevel.TRACE.name(), "TRACE");
        Assert.assertEquals(LogLevel.DEBUG.name(), "DEBUG");
        Assert.assertEquals(LogLevel.INFO.name(), "INFO");
        Assert.assertEquals(LogLevel.WARN.name(), "WARN");
        Assert.assertEquals(LogLevel.ERROR.name(), "ERROR");
    }
}
