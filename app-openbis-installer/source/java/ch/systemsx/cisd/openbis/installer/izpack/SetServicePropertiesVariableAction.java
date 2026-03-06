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

        String property = asProperties.getProperty(asTransactionCoordinatorKeyProperty);
        if(property == null || property.isBlank()) {
            UUID uuid = UUID.randomUUID();
            Utils.updateOrAppendProperty(asServicePropertiesFile, asTransactionCoordinatorKeyProperty, uuid.toString());
            Utils.updateOrAppendProperty(afsServicePropertiesFile, "apiServerTransactionManagerKey", uuid.toString());
        }

        asProperties.getProperty(asTransactionInteractiveSessionKeyProperty);
        if(property == null || property.isBlank()) {
            UUID uuid = UUID.randomUUID();
            Utils.updateOrAppendProperty(asServicePropertiesFile, asTransactionInteractiveSessionKeyProperty, uuid.toString());
            Utils.updateOrAppendProperty(afsServicePropertiesFile, "apiServerInteractiveSessionKey", uuid.toString());
        }

    }


}
