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
import java.util.UUID;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.DataStore;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.fetchoptions.DataStoreFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.search.DataStoreKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.search.DataStoreSearchCriteria;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.transaction.ITransactionExecutor;
import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.ethz.sis.shared.log.classic.impl.Logger;
import ch.ethz.sis.transaction.TransactionId;
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

    private static final Logger
            operationLog = LogFactory.getLogger(LogCategory.OPERATION, ImportExecutor.class);


    @Override
    public ImportResult doImport(final IOperationContext context, final ImportOperation operation)
    {
        final ImportData importData = operation.getImportData();

        final IApplicationServerInternalApi applicationServerApi = CommonServiceProvider.getApplicationServerApi();

        String transactionEnabled = CommonServiceProvider.tryToGetProperty("api.v3.transaction.enabled");
        String interactiveSessionKey = CommonServiceProvider.tryToGetProperty("api.v3.transaction.interactive-session-key");

        String afsUrl = CommonServiceProvider.tryToGetProperty("server-public-information.afs-server.url");
        String asUrl = CommonServiceProvider.tryToGetProperty("api.v3.transaction.participant.application-server.url");

        DataStoreSearchCriteria searchCriteria = new DataStoreSearchCriteria();
        searchCriteria.withKind().thatIn(DataStoreKind.DSS);
        SearchResult<DataStore> stores = applicationServerApi.searchDataStores(context.getSession().getSessionToken(), searchCriteria, new DataStoreFetchOptions());
        String dssUrl = null;
        if(stores.getTotalCount() > 0) {
            dssUrl = stores.getObjects().get(0).getRemoteUrl() + "/datastore_server";
        }

        OpenBIS openBIS = new OpenBIS(asUrl, dssUrl, afsUrl, 30000);

        openBIS.setSessionToken(context.getSession().getSessionToken());
        openBIS.setInteractiveSessionKey(interactiveSessionKey);


        final ImportOptions importOptions = operation.getImportOptions();

        final ch.ethz.sis.openbis.generic.server.xls.importer.ImportOptions importerImportOptions =
                new ch.ethz.sis.openbis.generic.server.xls.importer.ImportOptions();

        final boolean projectSamplesEnabled = Boolean.parseBoolean(applicationServerApi.getServerInformation(context.getSession().getSessionToken())
                .get("project-samples-enabled"));
        importerImportOptions.setAllowProjectSamples(projectSamplesEnabled);

        try
        {
            XLSImport xlsImport = new XLSImport(context.getSession().getSessionToken(), openBIS,
                    ImportModes.valueOf(importOptions.getMode().name()), importerImportOptions, importData.getSessionWorkspaceFiles(), false);
            ImportResult result = new ImportResult();
            if(xlsImport.importContainsAfsData())
            {
                operationLog.info("Importing metadata and data");
                if (transactionEnabled != null && !transactionEnabled.equalsIgnoreCase("true"))
                {
                    operationLog.info("Transactions are not enabled");
                    //transactions disabled
                    transactionExecutor.executeInSeparateTransaction(
                            () -> importMetaData(xlsImport, result));
                    xlsImport.importZipAfsData();
                } else
                {
                    UUID transactionId = TransactionId.getCurrent();
                    if (transactionId == null)
                    {
                        operationLog.info("No Two-Phase transaction id found");
                        try
                        {
                            openBIS.beginTransaction();
                            importMetaData(xlsImport, result);
                            xlsImport.importZipAfsData();

                            openBIS.commitTransaction();
                        } catch (Exception e)
                        {
                            openBIS.rollbackTransaction();
                            throw e;
                        }
                    } else
                    {
                        throw UserFailureException.fromTemplate(
                                "Import in Two-Phase transactions is not supported!");
                    }
                }
            } else {
                result.setObjectIds(xlsImport.start());
            }

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
