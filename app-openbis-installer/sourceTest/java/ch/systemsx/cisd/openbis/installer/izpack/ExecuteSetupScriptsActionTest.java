/*
 * Copyright ETH 2012 - 2023 Zürich, Scientific IT Services
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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import ch.systemsx.cisd.base.tests.AbstractFileSystemTestCase;
import ch.systemsx.cisd.common.filesystem.FileUtilities;

/**
 * @author Franz-Josef Elmer
 */
public class ExecuteSetupScriptsActionTest extends AbstractFileSystemTestCase
{
    private File dssServicePropertiesFile;

    private ExecuteSetupScriptsAction action;

    private File myKeystoreFile;

    private File keystoreFileDSS;

    @BeforeMethod
    public void setUpFiles() throws IOException
    {
        dssServicePropertiesFile =
                new File(workingDirectory, Utils.DSS_PATH + Utils.SERVICE_PROPERTIES_PATH);
        FileUtils.copyFile(
                new File("../core-plugin-openbis/dist/etc/service.properties/"),
                dssServicePropertiesFile);

        keystoreFileDSS = new File(workingDirectory, Utils.DSS_PATH + Utils.KEYSTORE_PATH);
        myKeystoreFile = new File(workingDirectory, "my-keystore");
        FileUtils.writeStringToFile(myKeystoreFile, "my-keys");
        action = new ExecuteSetupScriptsAction();
    }

    @Test
    public void testInstallKeyStoreWithUndefinedKeyStoreFileName() throws Exception
    {
        action.installKeyStore("", workingDirectory);

        assertEquals(false, keystoreFileDSS.exists());
    }

    @Test
    public void testInstallKeyStoreWithKeyStoreFileName() throws Exception
    {
        action.installKeyStore(myKeystoreFile.getPath(), workingDirectory);

        assertEquals(true, keystoreFileDSS.exists());
        assertEquals("my-keys", FileUtils.readFileToString(keystoreFileDSS));
    }

    @Test
    public void testEnablePathinfoDBNotDefinedInServiceProperties() throws Exception
    {
        FileUtilities.writeToFile(dssServicePropertiesFile, "");

        action.enablePathinfoDB(workingDirectory);

        Properties properties = loadProperties(dssServicePropertiesFile);
        assertEquals("", properties.getProperty(ExecuteSetupScriptsAction.DATA_SOURCES_KEY));
        assertEquals("",
                properties.getProperty(ExecuteSetupScriptsAction.POST_REGISTRATION_TASKS_KEY));
        assertEquals("",
                properties.getProperty(ExecuteSetupScriptsAction.MAINTENANCE_PLUGINS_KEY));
        assertEquals("", properties.getProperty(ExecuteSetupScriptsAction.PROCESSING_PLUGINS_KEY));
    }

    @Test
    public void testEnableAlreadyEnabledPathinfoDB() throws Exception
    {
        Properties properties = loadProperties(dssServicePropertiesFile);
        assertEquals(ExecuteSetupScriptsAction.PATHINFO_DB_DATA_SOURCE,
                properties.getProperty(ExecuteSetupScriptsAction.DATA_SOURCES_KEY).trim());
        assertEquals(ExecuteSetupScriptsAction.PATHINFO_DB_FEEDING_TASK,
                properties.getProperty(ExecuteSetupScriptsAction.POST_REGISTRATION_TASKS_KEY));
        assertEquals("post-registration, " + ExecuteSetupScriptsAction.PATHINFO_DB_DELETION_TASK,
                properties.getProperty(ExecuteSetupScriptsAction.MAINTENANCE_PLUGINS_KEY));

        action.enablePathinfoDB(workingDirectory);

        properties = loadProperties(dssServicePropertiesFile);
        assertEquals(ExecuteSetupScriptsAction.PATHINFO_DB_DATA_SOURCE,
                properties.getProperty(ExecuteSetupScriptsAction.DATA_SOURCES_KEY).trim());
        assertEquals(ExecuteSetupScriptsAction.PATHINFO_DB_FEEDING_TASK,
                properties.getProperty(ExecuteSetupScriptsAction.POST_REGISTRATION_TASKS_KEY));
        assertEquals("post-registration, " + ExecuteSetupScriptsAction.PATHINFO_DB_DELETION_TASK,
                properties.getProperty(ExecuteSetupScriptsAction.MAINTENANCE_PLUGINS_KEY));
    }

    public Properties loadProperties(File propertiesFile) throws Exception
    {
        Properties properties = new Properties();
        FileReader fileReader = null;
        try
        {
            fileReader = new FileReader(propertiesFile);
            properties.load(fileReader);
        } finally
        {
            IOUtils.closeQuietly(fileReader);
        }
        return properties;
    }
}
