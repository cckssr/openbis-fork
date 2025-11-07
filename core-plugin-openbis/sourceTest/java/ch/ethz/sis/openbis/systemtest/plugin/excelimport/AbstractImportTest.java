/*
 * Copyright ETH 2018 - 2023 Zürich, Scientific IT Services
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
package ch.ethz.sis.openbis.systemtest.plugin.excelimport;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import ch.ethz.sis.openbis.generic.server.asapi.v3.IApplicationServerInternalApi;
import ch.systemsx.cisd.openbis.generic.server.CommonServiceProvider;
import ch.systemsx.cisd.openbis.generic.server.util.TestInitializer;
import ch.systemsx.cisd.openbis.generic.shared.Constants;
import ch.systemsx.cisd.openbis.generic.shared.ISessionWorkspaceProvider;
import ch.systemsx.cisd.openbis.generic.shared.coreplugin.CorePluginsUtils;

public class AbstractImportTest extends AbstractTransactionalTestNGSpringContextTests
{

    private static final String VERSIONING_JSON = "./versioning.json";

    private static final String XLS_VERSIONING_DIR = "xls-import.version-data-file";

    protected static final String SYSTEM_USER = "system";

    protected static final String TEST_USER = "test";

    protected static final String PASSWORD = "password";

    protected static final String VALIDATION_SCRIPT = "full/scripts/valid.py";

    protected static final String DYNAMIC_SCRIPT = "full/scripts/dynamic/dynamic.py";

    @Autowired
    protected IApplicationServerInternalApi v3api;

    protected String sessionToken;

    protected String FILES_DIR;

    @BeforeSuite
    public void setupSuite()
    {
        System.setProperty(XLS_VERSIONING_DIR, VERSIONING_JSON);
        System.setProperty(CorePluginsUtils.CORE_PLUGINS_FOLDER_KEY, "dist/core-plugins");
        System.setProperty(Constants.ENABLED_MODULES_KEY, "xls-import");
        System.setProperty(Constants.PROJECT_SAMPLES_ENABLED_KEY, "false");
        TestInitializer.initEmptyDb();
    }

    @BeforeMethod
    public void beforeTest()
    {
        sessionToken = v3api.login(TEST_USER, PASSWORD);
    }

    @AfterMethod
    public void afterTest()
    {
        File f = new File(VERSIONING_JSON);
        f.delete();
        v3api.logout(sessionToken);
    }

    protected static String uploadToAsSessionWorkspace(final String sessionToken, final String filePath) throws IOException
    {
        final ISessionWorkspaceProvider sessionWorkspaceProvider = CommonServiceProvider.getSessionWorkspaceProvider();
        final String destination = UUID.randomUUID() + "/" + new File(filePath).getName();

        sessionWorkspaceProvider.write(sessionToken, destination, new FileInputStream("sourceTest/java/" + filePath));

        return destination;
    }

    protected static String[] uploadToAsSessionWorkspace(final String sessionToken, final String... filePaths) throws IOException
    {
        final ISessionWorkspaceProvider sessionWorkspaceProvider = CommonServiceProvider.getSessionWorkspaceProvider();
        final UUID uploadId = UUID.randomUUID();

        final String[] destinations = new String[filePaths.length];
        for (int i = 0; i < filePaths.length; i++)
        {
            destinations[i] = uploadId
                    + (filePaths[i].toLowerCase().endsWith(".xls") || filePaths[i].toLowerCase().endsWith(".xlsx") ? "/" : "/scripts/")
                    + new File(filePaths[i]).getName();
            sessionWorkspaceProvider.write(sessionToken, destinations[i], new FileInputStream("sourceTest/java/" + filePaths[i]));
        }

        return destinations;
    }

}
