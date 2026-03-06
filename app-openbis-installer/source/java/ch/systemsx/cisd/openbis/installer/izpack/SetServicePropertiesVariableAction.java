package ch.systemsx.cisd.openbis.installer.izpack;

import com.izforge.izpack.api.data.AutomatedInstallData;
import com.izforge.izpack.api.data.PanelActionConfiguration;
import com.izforge.izpack.api.handler.AbstractUIHandler;
import com.izforge.izpack.data.PanelAction;

import java.io.File;
import java.util.Properties;
import java.util.UUID;

import static ch.systemsx.cisd.openbis.installer.izpack.SetTechnologyCheckBoxesAction.ENABLED_TECHNOLOGIES_KEY;

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
        updateProperties(data, isFirstTimeInstallation, installDir);
    }

    void updateProperties(AutomatedInstallData data,
            boolean isFirstTimeInstallation, File installDir)
    {
        File asServicePropertiesFile =
                new File(installDir, Utils.AS_PATH + Utils.SERVICE_PROPERTIES_PATH);
        Properties properties = Utils.tryToGetProperties(asServicePropertiesFile);

        UUID testUUID = UUID.randomUUID();
        System.out.println("||> TEMP LOG:" + isFirstTimeInstallation);
        if(isFirstTimeInstallation) {
            Utils.updateOrAppendProperty(asServicePropertiesFile, "test-dummy-property",
                    "FIRST-" +testUUID);
        } else {
            String property = properties.getProperty("test-dummy-property");
            if(property == null || property.isBlank()) {
                Utils.updateOrAppendProperty(asServicePropertiesFile, "test-dummy-property", "UPDATE-"+testUUID);
            } else {
                Utils.updateOrAppendProperty(asServicePropertiesFile, "test-dummy-property", "UPDATE-"+property);
            }
        }

//        String propertyValue = null;
//        if (properties != null)
//        {
//            String property = properties.getProperty(ENABLED_TECHNOLOGIES_KEY);
//        }
//
//        Utils.updateOrAppendProperty(asServicePropertiesFile, ENABLED_TECHNOLOGIES_KEY,
//                String.join(", ", enabledModules));
    }



}
