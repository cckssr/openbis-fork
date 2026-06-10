/*
 * Copyright ETH 2022 - 2023 Zürich, Scientific IT Services
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
package ch.ethz.sis.openbis.generic.server.xls.importer.helper;

import java.io.File;
import java.util.List;
import java.util.Map;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.Plugin;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.PluginType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.create.PluginCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.fetchoptions.PluginFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.id.PluginPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.update.PluginUpdate;
import ch.ethz.sis.openbis.generic.server.xls.importer.ImportOptions;
import ch.ethz.sis.openbis.generic.server.xls.importer.delay.DelayedExecutionDecorator;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportModes;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportTypes;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ScriptTypes;
import ch.ethz.sis.openbis.generic.server.xls.importer.utils.ImportUtils;

import static ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.PluginType.DYNAMIC_PROPERTY;
import static ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.PluginType.ENTITY_VALIDATION;
import static ch.ethz.sis.openbis.generic.server.xls.importer.enums.ScriptTypes.DYNAMIC_SCRIPT;
import static ch.ethz.sis.openbis.generic.server.xls.importer.enums.ScriptTypes.VALIDATION_SCRIPT;

public class ScriptImportHelper extends BasicImportHelper
{
    private static final String OWNER_CODE = "Code";

    private ScriptTypes scriptType = null;

    private final Map<String, String> scripts;

    private final DelayedExecutionDecorator delayedExecutor;

    public ScriptImportHelper(DelayedExecutionDecorator delayedExecutor, ImportModes mode, ImportOptions options, Map<String, String> scripts)
    {
        super(mode, options);
        this.scripts = scripts;
        this.delayedExecutor = delayedExecutor;
    }

    @Override protected ImportTypes getTypeName()
    {
        return ImportTypes.SCRIPT;
    }

    private PluginPermId getScriptId(Map<String, Integer> header, List<String> values)
    {
        String ownerCode = getValueByColumnName(header, values, OWNER_CODE);
        String script = getValueByColumnName(header, values, scriptType.getColumnName());
        return ImportUtils.getScriptId(ownerCode, script, null);
    }

    @Override protected boolean isObjectExist(Map<String, Integer> header, List<String> values)
    {
        PluginPermId script = getScriptId(header, values);

        if (script != null)
        {
            return delayedExecutor.getPlugin(script, new PluginFetchOptions()) != null;
        }

        return false;
    }

    @Override protected void createObject(Map<String, Integer> header, List<String> values, int page, int line)
    {
        String scriptPath = getValueByColumnName(header, values, scriptType.getColumnName());

        if (scriptPath != null && !scriptPath.isEmpty())
        {
            String script = this.scripts.get(new File(scriptPath).getName());
            if (script != null)
            {
                PluginCreation creation = new PluginCreation();
                creation.setName(getScriptId(header, values).getPermId());
                creation.setScript(script);
                switch (scriptType)
                {
                    case VALIDATION_SCRIPT:
                        creation.setPluginType(ENTITY_VALIDATION);
                        break;
                    case DYNAMIC_SCRIPT:
                        creation.setPluginType(DYNAMIC_PROPERTY);
                        break;
                }
                delayedExecutor.createPlugin(creation);
            }
        }
    }

    @Override protected void updateObject(Map<String, Integer> header, List<String> values, int page, int line)
    {
        String scriptPath = getValueByColumnName(header, values, scriptType.getColumnName());
        if (scriptPath != null && !scriptPath.isEmpty())
        {
            String script = this.scripts.get(new File(scriptPath).getName());
            if (script != null)
            {
                PluginUpdate update = new PluginUpdate();
                update.setPluginId(getScriptId(header, values));
                update.setScript(script);
                delayedExecutor.updatePlugin(update);
            }
        }
    }

    @Override protected void validateHeader(Map<String, Integer> header)
    {
        checkKeyExistence(header, OWNER_CODE);
        checkKeyExistence(header, scriptType.getColumnName());
    }

    public void importBlock(List<List<String>> page, int pageIndex, int start, int end, ScriptTypes scriptType)
    {
        this.scriptType = scriptType;
        super.importBlock(page, pageIndex, start, end);
    }

    @Override public void importBlock(List<List<String>> page, int pageIndex, int start, int end)
    {
        throw new IllegalStateException("This method should have never been called.");
    }
}
