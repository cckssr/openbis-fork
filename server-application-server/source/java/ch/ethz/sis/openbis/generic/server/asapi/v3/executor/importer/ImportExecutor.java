/*
 *  Copyright ETH 2023-2026 Zürich, Scientific IT Services
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.UUID;

import ch.ethz.sis.afsapi.api.PublicAPI;
import ch.ethz.sis.afsclient.client.AfsClient;
import ch.ethz.sis.openbis.generic.asapi.v3.ITransactionCoordinatorApi;
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
    private static final Logger
            operationLog = LogFactory.getLogger(LogCategory.OPERATION, ImportExecutor.class);

    public static final String TWO_PHASE_TRANSACTIONS_ENABLED_PROPERTY_NAME = "api.v3.transaction.enabled";
    public static final String TWO_PHASE_TRANSACTIONS_SESSION_KEY_PROPERTY_NAME = "api.v3.transaction.interactive-session-key";
    public static final String AFS_SERVER_URL_PROPERTY_NAME = "api.v3.transaction.participant.afs-server.url";
    public static final String AFS_SERVER_TIMEOUT_PROPERTY_NAME = "api.v3.transaction.participant.afs-server.timeout";
    private static final String DEFAULT_AFS_CLIENT_TIMEOUT = "30000";

    @Autowired
    private ITransactionExecutor transactionExecutor;

    private UUID transactionId;

    @Override
    public ImportResult doImport(final IOperationContext context, final ImportOperation operation)
    {
        final String sessionToken = context.getSession().getSessionToken();
        final ImportData importData = operation.getImportData();
        final ImportOptions importOptions = operation.getImportOptions();

        final ch.ethz.sis.openbis.generic.server.xls.importer.ImportOptions importerImportOptions =
                new ch.ethz.sis.openbis.generic.server.xls.importer.ImportOptions();

        final boolean projectSamplesEnabled = Boolean.parseBoolean(CommonServiceProvider.getApplicationServerApi().getServerInformation(sessionToken)
                .get("project-samples-enabled"));
        importerImportOptions.setAllowProjectSamples(projectSamplesEnabled);
        importerImportOptions.setExperimentsByType(importOptions.getExperimentsByType());
        importerImportOptions.setSpacesByType(importOptions.getSpacesByType());

        return executeImport(sessionToken, ImportModes.valueOf(importOptions.getMode().name()), importerImportOptions, importData.getSessionWorkspaceFiles());
    }

    //needs to be public for xls-import jython script
    public ImportResult executeImport(final String sessionToken, ImportModes importModes, final ch.ethz.sis.openbis.generic.server.xls.importer.ImportOptions importerImportOptions, String[] files) {
        transactionId = TransactionId.getCurrent();
        final String transactionEnabledProperty = CommonServiceProvider.tryToGetProperty(TWO_PHASE_TRANSACTIONS_ENABLED_PROPERTY_NAME);
        boolean transactionEnabled = true;
        if (transactionEnabledProperty == null || !transactionEnabledProperty.trim().equalsIgnoreCase("true")) {
            transactionId = null;
            transactionEnabled = false;
        }
        final String interactiveSessionKey = CommonServiceProvider.tryToGetProperty(TWO_PHASE_TRANSACTIONS_SESSION_KEY_PROPERTY_NAME);
        final String afsUrl = CommonServiceProvider.tryToGetProperty(AFS_SERVER_URL_PROPERTY_NAME);

        final ITransactionCoordinatorApi transactionCoordinatorApi = CommonServiceProvider.getTransactionCoordinatorApi();

        final IApplicationServerInternalApi applicationServerApi = createTransactionalProxy(ITransactionCoordinatorApi.APPLICATION_SERVER_PARTICIPANT_ID, IApplicationServerInternalApi.class,
                CommonServiceProvider.getApplicationServerApi(), transactionCoordinatorApi, sessionToken, interactiveSessionKey);

        AfsClient afsClient = null;
        if(afsUrl != null && !afsUrl.isEmpty()) {
            final String afsTimeoutProperty = CommonServiceProvider.tryToGetProperty(AFS_SERVER_TIMEOUT_PROPERTY_NAME, DEFAULT_AFS_CLIENT_TIMEOUT);
            final int timeout = Integer.parseInt(afsTimeoutProperty);

            afsClient = new AfsClient(URI.create(afsUrl), AfsClient.DEFAULT_PACKAGE_SIZE_IN_BYTES,
                    timeout);
            afsClient.setSessionToken(sessionToken);

            afsClient = new AfsClient(createTransactionalProxy(ITransactionCoordinatorApi.AFS_SERVER_PARTICIPANT_ID, PublicAPI.class,
                    afsClient, transactionCoordinatorApi, sessionToken, interactiveSessionKey), AfsClient.DEFAULT_PACKAGE_SIZE_IN_BYTES,
                    timeout);
            afsClient.setSessionToken(sessionToken);
            afsClient.setInteractiveSessionKey(interactiveSessionKey);
        } else {
            operationLog.info("AFS url configuration not found.");
        }

        try
        {

            final XLSImport xlsImport = new XLSImport(sessionToken, applicationServerApi,
                    importModes, importerImportOptions, files, false, afsClient);

            ImportResult result = new ImportResult();
            if(xlsImport.importContainsAfsData())
            {
                operationLog.info("Importing metadata and data");
                if(afsClient == null) {
                    operationLog.info("AFS configuration not found!");
                    throw new UserFailureException("AFS configuration not found!");
                }

                if (!transactionEnabled)
                {
                    operationLog.info("Two-Phase transactions are not enabled. Executing in separate transactions mode");
                    transactionExecutor.executeInSeparateTransaction(
                            () -> importMetaData(xlsImport, result));
                    xlsImport.importZipAfsData();
                } else
                {
                    if (transactionId == null)
                    {
                        operationLog.info("No existing transaction id found");
                        transactionId = UUID.randomUUID();
                        try
                        {
                            transactionCoordinatorApi.beginTransaction(transactionId, sessionToken, interactiveSessionKey);
                            importMetaData(xlsImport, result);
                            xlsImport.importZipAfsData();
                            transactionCoordinatorApi.commitTransaction(transactionId, sessionToken, interactiveSessionKey);
                        } catch (Exception e)
                        {
                            transactionCoordinatorApi.rollbackTransaction(transactionId, sessionToken, interactiveSessionKey);
                            throw e;
                        }
                    } else
                    {
                        throw UserFailureException.fromTemplate(
                                "Import in Two-Phase transactions is not supported!");
                    }
                }
            } else {
                operationLog.info("No data detected. Importing metadata only.");
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

    private <T> T createTransactionalProxy(String transactionParticipantId, Class<T> serviceInterface, T service,
            ITransactionCoordinatorApi transactionCoordinatorApi, String sessionToken, String interactiveSessionKey)
    {
        return (T) Proxy.newProxyInstance(serviceInterface.getClassLoader(), new Class[] { serviceInterface },
                (proxy, method, args) ->
                {
                    if (transactionId != null)
                    {
                        return transactionCoordinatorApi.executeOperation(transactionId, sessionToken, interactiveSessionKey,
                                transactionParticipantId, method.getName(), args);
                    } else
                    {
                        try
                        {
                            return method.invoke(service, args);
                        } catch (InvocationTargetException e)
                        {
                            throw e.getTargetException();
                        }
                    }
                });
    }

}
