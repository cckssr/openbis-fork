/*
 * Copyright ETH 2009 - 2023 Zürich, Scientific IT Services
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
package ch.systemsx.cisd.openbis.generic.shared.translator;

import ch.systemsx.cisd.openbis.generic.shared.basic.dto.AbstractType;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.LocatorType;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.SampleType;
import ch.systemsx.cisd.openbis.generic.shared.dto.AbstractTypePE;
import ch.systemsx.cisd.openbis.generic.shared.dto.LocatorTypePE;
import ch.systemsx.cisd.openbis.generic.shared.dto.SampleTypePE;

/**
 * Translator of {@link AbstractTypePE} instances.
 * 
 * @author Franz-Josef Elmer
 */
public class TypeTranslator
{
    public static LocatorType translate(LocatorTypePE locatorType)
    {
        LocatorType result = new LocatorType();
        fill(result, locatorType);
        return result;
    }

    public static SampleType translate(SampleTypePE sampleType)
    {
        SampleType result = new SampleType();
        fill(result, sampleType);
        return result;
    }

    private static <T extends AbstractType> T fill(T type, AbstractTypePE typePEOrNull)
    {
        if (typePEOrNull != null)
        {
            type.setCode(typePEOrNull.getCode());
            type.setDescription(typePEOrNull.getDescription());
        }
        return type;
    }

}
