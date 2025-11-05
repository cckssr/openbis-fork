/*
 *  Copyright ETH 2024 Zürich, Scientific IT Services
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

package ch.ethz.sis.openbis.generic.server.as.plugins.imaging.adaptor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class ImagingDataSetPythonAdaptor extends ImagingDataSetAbstractPythonAdaptor
{
    private static final String SCRIPT_PROPERTY = "script-path";
    private static final String PYTHON3_PROPERTY = "python3-path";


    public ImagingDataSetPythonAdaptor(Properties properties)
    {
        String scriptProperty = properties.getProperty(SCRIPT_PROPERTY, "");
        if (scriptProperty.trim().isEmpty())
        {
            throw new IllegalArgumentException(
                    "There is no script path property called '" + SCRIPT_PROPERTY + "' defined for this adaptor!");
        }
        Path script = Paths.get(scriptProperty);
        if (!Files.exists(script))
        {
            throw new IllegalArgumentException("Script file " + script + " does not exists!");
        }
        this.scriptPath = script.toString();
        this.pythonPath = properties.getProperty(PYTHON3_PROPERTY, "python3");
    }
}
