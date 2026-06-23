/*
 * Copyright ETH 2013 - 2023 Zürich, Scientific IT Services
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
package ch.systemsx.cisd.openbis.generic.server.dataaccess.dynamic_property.calculator;

import ch.systemsx.cisd.openbis.generic.server.dataaccess.dynamic_property.IDynamicPropertyCalculatorFactory;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.dynamic_property.calculator.api.IDynamicPropertyCalculator;
import ch.systemsx.cisd.openbis.generic.shared.IJythonEvaluatorPool;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.PluginType;
import ch.systemsx.cisd.openbis.generic.shared.dto.EntityTypePropertyTypePE;

/**
 * The class is responsible for getting a dynamic property calculator.
 *
 * @author Pawel Glyzewski
 */
public class DynamicPropertyCalculatorFactory implements IDynamicPropertyCalculatorFactory
{
    private final IJythonEvaluatorPool evaluationRunnerProvider;

    public DynamicPropertyCalculatorFactory(IJythonEvaluatorPool evaluationRunnerProvider)
    {
        this.evaluationRunnerProvider = evaluationRunnerProvider;
    }

    @Override
    /** Returns a calculator for given script (creates a new one if nothing is found in cache). */
    public IDynamicPropertyCalculator getCalculator(EntityTypePropertyTypePE etpt)
    {
        PluginType pluginType = etpt.getScript().getPluginType();
        String script = etpt.getScript().getScript();
        String scriptName = etpt.getScript().getName();
        return getCalculator(pluginType, scriptName, script);
    }

    @Override
    public IDynamicPropertyCalculator getCalculator(PluginType pluginType, String scriptName,
            String script)
    {
        switch (pluginType)
        {
            case JYTHON:
                return JythonDynamicPropertyCalculator.create(script, evaluationRunnerProvider);
        }

        return null;
    }

}
