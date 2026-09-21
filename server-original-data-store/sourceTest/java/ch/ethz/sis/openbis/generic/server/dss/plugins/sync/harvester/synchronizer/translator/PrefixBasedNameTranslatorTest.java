/*
 * Copyright ETH 2026 Zurich, Scientific IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.ethz.sis.openbis.generic.server.dss.plugins.sync.harvester.synchronizer.translator;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class PrefixBasedNameTranslatorTest
{
    private final PrefixBasedNameTranslator translator = new PrefixBasedNameTranslator("SRC");

    @Test
    public void translateOrdinaryCode()
    {
        assertEquals(translator.translate("SAMPLE_TYPE"), "SRC_SAMPLE_TYPE");
        assertEquals(translator.translateBack("SRC_SAMPLE_TYPE"), "SAMPLE_TYPE");
    }

    @Test
    public void translateInternalNamespaceCode()
    {
        assertEquals(translator.translate("$STORAGE"), "$STORAGE");
        assertEquals(translator.translateBack("$STORAGE"), "$STORAGE");
    }

    @DataProvider
    public Object[][] codes()
    {
        return new Object[][] { { "SAMPLE_TYPE" }, { "$STORAGE" } };
    }

    @Test(dataProvider = "codes")
    public void translateBackOfTranslateIsIdentity(String code)
    {
        assertEquals(translator.translateBack(translator.translate(code)), code);
    }
}
