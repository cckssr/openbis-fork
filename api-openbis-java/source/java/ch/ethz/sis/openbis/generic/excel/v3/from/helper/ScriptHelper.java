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
package ch.ethz.sis.openbis.generic.excel.v3.from.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.Plugin;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.id.PluginPermId;
import ch.ethz.sis.openbis.generic.excel.v3.from.enums.ImportTypes;
import ch.ethz.sis.openbis.generic.excel.v3.from.enums.ScriptTypes;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.PluginType.DYNAMIC_PROPERTY;
import static ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.PluginType.ENTITY_VALIDATION;

public class ScriptHelper extends BasicImportHelper
{
    private static final String OWNER_CODE = "Code";

    private ScriptTypes scriptType = null;

    private final Map<String, String> scripts;

    private final Map<PluginPermId, Plugin> accumulator;

    public ScriptHelper(Map<String, String> scripts)
    {
        this.scripts = scripts;
        this.accumulator = new LinkedHashMap<>();
    }

    protected ImportTypes getTypeName()
    {
        return ImportTypes.SCRIPT;
    }

    private PluginPermId getScriptId(Map<String, Integer> header, List<String> values)
    {
        String script = getValueByColumnName(header, values, scriptType.getColumnName());
        return getScriptId(header, values);
    }

    @Override
    protected boolean isObjectExist(Map<String, Integer> header, List<String> values)
    {

        return false;
    }

    @Override
    protected void createObject(Map<String, Integer> header, List<String> values, int page,
            int line)
    {
        String scriptPath = getValueByColumnName(header, values, scriptType.getColumnName());

        if (scriptPath != null && !scriptPath.isEmpty())
        {
            String script = this.scripts.get(new File(scriptPath).getName());
            if (script != null)
            {
                Plugin plugin = new Plugin();
                plugin.setName(getScriptId(header, values).getPermId());
                plugin.setPermId(getScriptId(header, values));
                plugin.setScript(script);

                switch (scriptType)
                {
                    case VALIDATION_SCRIPT:
                        plugin.setPluginType(ENTITY_VALIDATION);
                        break;
                    case DYNAMIC_SCRIPT:
                        plugin.setPluginType(DYNAMIC_PROPERTY);
                        break;
                }
                accumulator.put(plugin.getPermId(), plugin);
            }
        }
    }

    @Override
    protected void updateObject(Map<String, Integer> header, List<String> values, int page,
            int line)
    {
    }

    @Override
    protected void validateHeader(Map<String, Integer> header)
    {
        checkKeyExistence(header, OWNER_CODE);
        checkKeyExistence(header, scriptType.getColumnName());
    }

    public void importBlock(List<List<String>> page, int pageIndex, int start, int end,
            ScriptTypes scriptType)
    {
        this.scriptType = scriptType;
        super.importBlock(page, pageIndex, start, end);
    }

    @Override
    public void importBlock(List<List<String>> page, int pageIndex, int start, int end)
    {
        throw new IllegalStateException("This method should have never been called.");
    }

    public Map<PluginPermId, Plugin> getResults()
    {
        return accumulator;
    }
}
