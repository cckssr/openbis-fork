/*
 * Copyright ETH 2008 - 2023 Zürich, Scientific IT Services
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
package ch.systemsx.cisd.openbis.generic.shared.managed_property;

import ch.systemsx.cisd.openbis.generic.shared.IJythonEvaluatorPool;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.EntityTypePropertyType;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.PluginType;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.Script;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.ScriptType;
import ch.systemsx.cisd.openbis.generic.shared.dto.EntityTypePropertyTypePE;
import ch.systemsx.cisd.openbis.generic.shared.dto.ScriptPE;
import ch.systemsx.cisd.openbis.generic.shared.managed_property.api.IManagedPropertyEvaluator;

/**
 * Factory for creating managed property evaluators. (Could do some caching or other cleverness.)
 * 
 * @author Chandrasekhar Ramakrishnan
 * @author Jakub Straszewski
 * @author Pawel Glyzewski
 */
public class ManagedPropertyEvaluatorFactory implements IManagedPropertyEvaluatorFactory
{
    private final IJythonEvaluatorPool jythonEvaluatorPool;

    public ManagedPropertyEvaluatorFactory(IJythonEvaluatorPool jythonEvaluatorPool)
    {
        this.jythonEvaluatorPool = jythonEvaluatorPool;
    }

    @Override
    public IManagedPropertyEvaluator createManagedPropertyEvaluator(
            EntityTypePropertyTypePE entityTypePropertyTypePE)
    {
        final ScriptPE scriptPE = entityTypePropertyTypePE.getScript();
        assert scriptPE != null && scriptPE.getScriptType() == ScriptType.MANAGED_PROPERTY;

        return getManagedPropertyEvaluator(scriptPE.getPluginType(), scriptPE.getName(),
                scriptPE.getScript());
    }

    @Override
    public IManagedPropertyEvaluator createManagedPropertyEvaluator(
            EntityTypePropertyType<?> entityTypePropertyType)
    {
        Script script = entityTypePropertyType.getScript();

        return getManagedPropertyEvaluator(script.getPluginType(), script.getName(),
                script.getScript());
    }

    private IManagedPropertyEvaluator getManagedPropertyEvaluator(PluginType pluginType,
            String scriptName, String scriptBody)
    {
        switch (pluginType)
        {
            case JYTHON:
                return new JythonManagedPropertyEvaluator(
                        jythonEvaluatorPool.getManagedPropertiesRunner(scriptBody));
        }

        return null;
    }

}
