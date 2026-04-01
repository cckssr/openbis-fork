/*
 * Copyright ETH 2026 Zürich, Scientific IT Services
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
package ch.systemsx.cisd.openbis.installer.izpack;

import com.izforge.izpack.api.data.AutomatedInstallData;
import com.izforge.izpack.api.data.PanelActionConfiguration;
import com.izforge.izpack.api.handler.AbstractUIHandler;
import com.izforge.izpack.data.PanelAction;

import java.io.File;
import java.util.Properties;
import java.util.UUID;


public class SetServicePropertiesVariableAction implements PanelAction
{

    @Override
    public void initialize(PanelActionConfiguration panelActionConfiguration)
    {

    }

    @Override
    public void executeAction(AutomatedInstallData data, AbstractUIHandler abstractUIHandler)
    {
        boolean isFirstTimeInstallation = GlobalInstallationContext.isFirstTimeInstallation;
        File installDir = GlobalInstallationContext.installDir;

        initialize2PT(data, isFirstTimeInstallation, installDir);
        initializeImaging(data, isFirstTimeInstallation, installDir);
    }

    /**
     * Initialize 2PT (Two-Phase Transactions) properties
     * @param data
     * @param isFirstTimeInstallation
     * @param installDir
     */
    void initialize2PT(AutomatedInstallData data,
            boolean isFirstTimeInstallation, File installDir) {
        final String asTransactionCoordinatorKeyProperty = "api.v3.transaction.coordinator-key";
        final String asTransactionInteractiveSessionKeyProperty = "api.v3.transaction.interactive-session-key";


        File asServicePropertiesFile =
                new File(installDir, Utils.AS_PATH + Utils.SERVICE_PROPERTIES_PATH);
        Properties asProperties = Utils.tryToGetProperties(asServicePropertiesFile);

        File afsServicePropertiesFile =
                new File(installDir, Utils.AFS_PATH + Utils.SERVICE_PROPERTIES_PATH);

        String transactionCoordinatorKey = asProperties.getProperty(asTransactionCoordinatorKeyProperty);
        if(transactionCoordinatorKey == null || transactionCoordinatorKey.isBlank()) {
            UUID uuid = UUID.randomUUID();
            Utils.updateOrAppendProperty(asServicePropertiesFile, asTransactionCoordinatorKeyProperty, uuid.toString());
            Utils.updateOrAppendProperty(afsServicePropertiesFile, "apiServerTransactionManagerKey", uuid.toString());
        }

        String transactionInteractiveSessionKey = asProperties.getProperty(asTransactionInteractiveSessionKeyProperty);
        if(transactionInteractiveSessionKey == null || transactionInteractiveSessionKey.isBlank()) {
            UUID uuid = UUID.randomUUID();
            Utils.updateOrAppendProperty(asServicePropertiesFile, asTransactionInteractiveSessionKeyProperty, uuid.toString());
            Utils.updateOrAppendProperty(afsServicePropertiesFile, "apiServerInteractiveSessionKey", uuid.toString());
        }

        if(!isFirstTimeInstallation) {
            //Flow during upgrade if transactions were not setup
            final String transactionsEnabledProperty = "api.v3.transaction.enabled";
            String transactionsEnabled = asProperties.getProperty(transactionsEnabledProperty);
            if(transactionsEnabled == null || transactionsEnabled.isBlank()) {
                Utils.updateOrAppendProperty(asServicePropertiesFile, transactionsEnabledProperty, "true");
            }

            final String afsUrlProperty = "api.v3.transaction.participant.afs-server.url";
            String afsUrl = asProperties.getProperty(afsUrlProperty);
            if(afsUrl == null || afsUrl.isBlank()) {
                Utils.updateOrAppendProperty(asServicePropertiesFile, afsUrlProperty, "http://localhost:8085/afs-server");
            }

            final String logPathProperty = "api.v3.transaction.transaction-log-folder-path";
            String logFolderPath = asProperties.getProperty(logPathProperty);
            if(logFolderPath == null || logFolderPath.isBlank()) {
                Utils.updateOrAppendProperty(asServicePropertiesFile, logPathProperty, "logs/transaction-logs");
            }

            final String asParticipantUrlProperty = "api.v3.transaction.participant.application-server.url";
            String asParticipantUrl = asProperties.getProperty(asParticipantUrlProperty);
            if(asParticipantUrl == null || asParticipantUrl.isBlank()) {
                Utils.updateOrAppendProperty(asServicePropertiesFile, asParticipantUrlProperty, "https://localhost:8443");
            }
        }

    }

    void initializeImaging(AutomatedInstallData data,
            boolean isFirstTimeInstallation, File installDir) {
        if(isFirstTimeInstallation) {
            for (String technology : GlobalInstallationContext.TECHNOLOGIES) {
                String lowerCasedTechnology = technology.toLowerCase();
                String technologyFlag = data.getVariable(technology);
                if (lowerCasedTechnology.equalsIgnoreCase("imaging") && Boolean.TRUE.toString().equalsIgnoreCase(technologyFlag)) {
                    File asServicePropertiesFile =
                            new File(installDir, Utils.AS_PATH + Utils.SERVICE_PROPERTIES_PATH);
                    if(asServicePropertiesFile.exists()) {
                        String dataDir = GlobalInstallationContext.getDataDir(data);
                        Utils.updateOrAppendProperty(asServicePropertiesFile, "imaging.as.services.imaging.storageRoot.dss", dataDir + "/store");
                        Utils.updateOrAppendProperty(asServicePropertiesFile, "imaging.as.services.imaging.storageRoot.afs", dataDir + "/store");
                    }
                    break;
                }
            }
        }
    }


}
