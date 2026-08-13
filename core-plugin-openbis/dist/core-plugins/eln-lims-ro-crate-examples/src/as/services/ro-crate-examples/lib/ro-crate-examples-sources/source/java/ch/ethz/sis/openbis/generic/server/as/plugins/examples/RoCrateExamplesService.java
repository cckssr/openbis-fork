/*
 *  Copyright ETH 2026 Zürich, Scientific IT Services
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

package ch.ethz.sis.openbis.generic.server.as.plugins.examples;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.ImportResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.data.ImportData;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.data.ImportFormat;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.options.ImportMode;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.options.ImportOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.service.CustomASServiceExecutionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.plugin.service.ICustomASServiceExecutor;
import ch.ethz.sis.openbis.generic.asapi.v3.plugin.service.context.CustomASServiceContext;
import ch.ethz.sis.openbis.generic.server.asapi.v3.IApplicationServerInternalApi;
import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.systemsx.cisd.common.properties.PropertyUtils;
import ch.systemsx.cisd.openbis.generic.server.CommonServiceProvider;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.ethz.sis.shared.log.classic.impl.Logger;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class RoCrateExamplesService implements ICustomASServiceExecutor
{

    private final Properties properties;

    private static final Logger
            operationLog = LogFactory.getLogger(LogCategory.OPERATION, RoCrateExamplesService.class);


    /** Buffer size for the buffer stream for Base64 encoding. Should be a multiple of 3. */
    static final int BUFFER_SIZE = 3 * 1024;

    private final int afsTimeout;


    public RoCrateExamplesService(Properties properties)
    {
        this.properties = properties;
        this.afsTimeout = PropertyUtils.getInt(properties, "afs.timeout", 3600);
    }

    @Override
    public Serializable executeService(CustomASServiceContext context,
            CustomASServiceExecutionOptions options)
    {
        return executeService(context.getSessionToken(), "ro-crate-examples", options.getParameters());
    }

    private Serializable executeService(String sessionToken, String serviceId,
            Map<String, Object> params)
    {
        operationLog.info("Executing service: " + serviceId);
        try
        {
            Object methodParam = params.get("method");
            if(methodParam != null && methodParam instanceof String method)
            {
                if (method.equalsIgnoreCase("isEnabled")) {
                    return (Serializable) Map.of("result", true);
                } else if(method.equalsIgnoreCase("scicat")) {
                    IApplicationServerInternalApi api = CommonServiceProvider.getApplicationServerApi();
                    String token = api.loginAsSystem();

                    String pluginsFolder = CommonServiceProvider.tryToGetProperty("core-plugins-folder");
                    Path dataPath = Path.of(pluginsFolder, "/eln-lims-ro-crate-examples/src/as/example-data", "sci_cat_master_data.zip");

                    CommonServiceProvider.getSessionWorkspaceProvider().write(token, "sci_cat_master_data.zip", new FileInputStream(dataPath.toFile()));
                    ImportData importData = new ImportData(ImportFormat.EXCEL, "sci_cat_master_data.zip");
                    ImportOptions importOptions = new ImportOptions(ImportMode.UPDATE_IF_EXISTS);
                    ImportResult result = api.executeImport(token, importData, importOptions);
                    Map<String, ImportResult> r = new HashMap<>();
                    r.put("result", result);
                    return (Serializable) r;
                }
            }
        } catch (Exception e)
        {
            operationLog.error("Exception during service: " + e.toString());
            return (Serializable) Map.of("error", e);
        }
        return (Serializable) Map.of("result", false);
    }



}
