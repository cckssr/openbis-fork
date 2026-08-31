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
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.common.properties.PropertyUtils;
import ch.systemsx.cisd.openbis.generic.server.CommonServiceProvider;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.ethz.sis.shared.log.classic.impl.Logger;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

import static ch.systemsx.cisd.openbis.generic.shared.Constants.DOWNLOAD_URL;

public class RoCrateExamplesService implements ICustomASServiceExecutor
{

    private final Properties properties;

    private static final Logger
            operationLog = LogFactory.getLogger(LogCategory.OPERATION, RoCrateExamplesService.class);

    private static final List<String> TYPE_NAMES = List.of("scicat", "logbook", "publication");

    private static final Map<String, String> TYPE_TO_NAME_MAP = Map.of("scicat", "sci_cat_master_data.zip",
            "logbook", "logbook-demo-crate.2026-08-24-09-21-49-055.zip",
            "publication", "publication-demo-crate.2026-08-24-08-53-33-997.zip");


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

    private Serializable runImportForFile(String sessionToken, String fileName) throws IOException
    {
        IApplicationServerInternalApi api = CommonServiceProvider.getApplicationServerApi();
        String token = api.loginAsSystem();

        Path exampleDataFolderPath = Path.of(this.properties.get("lib-folder").toString(), "../../../example-data");
        Path dataPath = Path.of(exampleDataFolderPath.toString(), fileName);
        operationLog.info(String.format("Importing data: %s", dataPath));
        CommonServiceProvider.getSessionWorkspaceProvider().write(token, fileName, new FileInputStream(dataPath.toFile()));
        ImportData importData = new ImportData(ImportFormat.EXCEL, fileName);
        ImportOptions importOptions = new ImportOptions(ImportMode.UPDATE_IF_EXISTS);
        ImportResult result = api.executeImport(token, importData, importOptions);
        operationLog.info(String.format("Import result: %s", result));
        Map<String, Serializable> r = new HashMap<>();
        r.put("result", result);
        api.logout(token);
        return (Serializable) r;
    }

    private Serializable getData(String sessionToken, Map<String, Object> params) throws IOException
    {
        IApplicationServerInternalApi api = CommonServiceProvider.getApplicationServerApi();

        String typeParam = (String) params.get("type");
        String fileName = TYPE_TO_NAME_MAP.get(typeParam);

        Path exampleDataFolderPath = Path.of(this.properties.get("lib-folder").toString(), "../../../example-data");
        Path dataPath = Path.of(exampleDataFolderPath.toString(), fileName);
        operationLog.info(String.format("Moving data for import: %s", dataPath));
        CommonServiceProvider.getSessionWorkspaceProvider().write(sessionToken, fileName, new FileInputStream(dataPath.toFile()));
        Map<String, Serializable> r = new HashMap<>();
        r.put("result", fileName);
        return (Serializable) r;
    }

    private String getDownloadPath(final String sessionToken, Map<String, Object> params)
            throws IOException
    {
        final String protocolWithDomain = CommonServiceProvider.tryToGetProperty(DOWNLOAD_URL);
        if (protocolWithDomain == null || protocolWithDomain.isBlank())
        {
            throw new UserFailureException(String.format("The property '%s' is not configured for the application server.", DOWNLOAD_URL));
        }

        String typeParam = (String) params.get("type");
        String fileName = TYPE_TO_NAME_MAP.get(typeParam);
        String downloadUrl = null;
        if(fileName != null) {
            Path exampleDataFolderPath = Path.of(this.properties.get("lib-folder").toString(), "../../../example-data");
            Path dataPath = Path.of(exampleDataFolderPath.toString(), fileName);
            CommonServiceProvider.getSessionWorkspaceProvider().write(sessionToken, fileName, new FileInputStream(dataPath.toFile()));

            downloadUrl = String.format("%s/openbis/openbis/download?sessionID=%s&filePath=%s", protocolWithDomain, sessionToken,
                    URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        }
        return downloadUrl;
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
                } else if(method.equalsIgnoreCase("download")) {
                    return getDownloadPath(sessionToken, params);
                } else if(method.equalsIgnoreCase("scicat")) {
                    return runImportForFile(sessionToken, TYPE_TO_NAME_MAP.get(method));
                } else if(method.equalsIgnoreCase("getData")) {
                    return getData(sessionToken, params);
                } else {
                    throw new UnsupportedOperationException(String.format("No such method:'%s'", method));
                }
            }
        } catch (Exception e)
        {
            operationLog.error("Exception during service: " + e);
            return (Serializable) Map.of("error", e);
        }
        return (Serializable) Map.of("result", false);
    }



}
