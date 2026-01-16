/*
 *  Copyright ETH 2023 Zürich, Scientific IT Services
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

package ch.ethz.sis.openbis.generic.server.asapi.v3.executor.importer;

import java.io.IOException;

import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.transaction.ITransactionExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.ImportOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.ImportResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.data.ImportData;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.options.ImportOptions;
import ch.ethz.sis.openbis.generic.server.asapi.v3.IApplicationServerInternalApi;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.IOperationContext;
import ch.ethz.sis.openbis.generic.server.xls.importer.XLSImport;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportModes;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.openbis.generic.server.CommonServiceProvider;

@Component
public class ImportExecutor implements IImportExecutor
{

    @Autowired
    private ITransactionExecutor transactionExecutor;


    @Override
    public ImportResult doImport(final IOperationContext context, final ImportOperation operation)
    {
        final ImportData importData = operation.getImportData();

        final IApplicationServerInternalApi applicationServerApi = CommonServiceProvider.getApplicationServerApi();
        final ImportOptions importOptions = operation.getImportOptions();

        final ch.ethz.sis.openbis.generic.server.xls.importer.ImportOptions importerImportOptions =
                new ch.ethz.sis.openbis.generic.server.xls.importer.ImportOptions();

        final boolean projectSamplesEnabled = Boolean.parseBoolean(applicationServerApi.getServerInformation(context.getSession().getSessionToken())
                .get("project-samples-enabled"));
        importerImportOptions.setAllowProjectSamples(projectSamplesEnabled);

        try
        {
            final XLSImport xlsImport = new XLSImport(context.getSession().getSessionToken(), applicationServerApi,
                    ImportModes.valueOf(importOptions.getMode().name()), importerImportOptions, importData.getSessionWorkspaceFiles(), false);
            ImportResult result = new ImportResult();

            transactionExecutor.executeInSeparateTransaction(() -> importMetaData(xlsImport, result));
            xlsImport.importZipAfsData();

            return result;
        } catch (final IOException e)
        {
            throw UserFailureException.fromTemplate(e, "IO exception importing.");
        } catch (final Exception e)
        {
            throw UserFailureException.fromTemplate(e,"Exception importing data: %s", e.getMessage());
        }
    }

    private void importMetaData(final XLSImport xlsImport, ImportResult result)
    {
        try
        {
            result.setObjectIds(xlsImport.start());
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

}
