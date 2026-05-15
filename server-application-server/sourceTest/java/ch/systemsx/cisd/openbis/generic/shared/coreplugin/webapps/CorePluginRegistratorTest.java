/*
 * Copyright ETH 2014 - 2023 Zürich, Scientific IT Services
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
package ch.systemsx.cisd.openbis.generic.shared.coreplugin.webapps;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import ch.systemsx.cisd.base.tests.AbstractFileSystemTestCase;
import ch.systemsx.cisd.openbis.generic.server.ICommonServerForInternalUse;
import ch.systemsx.cisd.openbis.generic.server.coreplugin.CorePluginRegistrator;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.CorePlugin;
import ch.systemsx.cisd.openbis.generic.shared.coreplugin.ICorePluginResourceLoader;
import ch.systemsx.cisd.openbis.generic.shared.dto.SessionContextDTO;

/**
 * @author Jakub Straszewski
 */
public class CorePluginRegistratorTest extends AbstractFileSystemTestCase
{
    private Mockery context;

    private ICommonServerForInternalUse commonServer;

    private String sessionToken = "SESSION-TOKEN";

    private static final String CORE_PLUGIN_PROPERTIES_FILE_NAME = "core-plugin.properties";

    @BeforeMethod
    public void beforeMethod()
    {
        context = new Mockery();
        commonServer = context.mock(ICommonServerForInternalUse.class);
    }

    @Test
    public void testPluginsRegistration() throws IOException
    {
        File corePluginsFolder = new File(workingDirectory, "core-plugins");
        corePluginsFolder.mkdirs();

        createCorePluginFolder(corePluginsFolder, "dont-test");
        createCorePluginFolder(corePluginsFolder, "test-register-1");
        createCorePluginFolder(corePluginsFolder, "test-register-2");
        createCorePluginFolder(corePluginsFolder, "test-no-master_data");

        CorePluginRegistrator cr = new CorePluginRegistrator();
        cr.setEnabledTechnologies("test-.*");
        cr.setDisabledMasterDataInitialization("test-no-.*");
        cr.setCommonServer(commonServer);
        cr.setPluginsFolderName(corePluginsFolder.getPath());

        expectPluginsRegistered("test-register-1", "test-register-2");

        cr.registerPlugins();

        context.assertIsSatisfied();
    }

    protected void expectPluginsRegistered(final String... plugins)
    {
        context.checking(new Expectations()
            {
                Matcher<CorePlugin> corePlugin(String corePluginName)
                {
                    class CPM extends BaseMatcher<CorePlugin>
                    {
                        private final String name;

                        private CPM(String name)
                        {
                            this.name = name;
                        }

                        @Override
                        public void describeTo(Description description)
                        {

                        }

                        @Override
                        public boolean matches(Object item)
                        {
                            return ((CorePlugin) item).getName().equals(name);
                        }
                    }
                    return new CPM(corePluginName);
                }

                {
                    one(commonServer).tryToAuthenticateAsSystem();

                    SessionContextDTO sessionContext = new SessionContextDTO();
                    sessionContext.setSessionToken(sessionToken);
                    will(returnValue(sessionContext));

                    for (String plugin : plugins)
                    {
                        one(commonServer).registerPlugin(with(sessionToken), with(corePlugin(plugin)),
                                with(any(ICorePluginResourceLoader.class)));
                    }
                }
            });
    }

    private void createCorePluginFolder(File corePluginsFolder, String technologyName) throws IOException
    {
        File technologyDir = new File(corePluginsFolder, technologyName);
        File parentDir = technologyDir;
        for (String dir : Arrays.asList("src", "as"))
        {
            parentDir = new File(parentDir, dir);
        }
        parentDir.mkdirs();

        File corePluginProperties =
                new File(technologyDir, CORE_PLUGIN_PROPERTIES_FILE_NAME);
        ch.systemsx.cisd.common.filesystem.FileUtilities.writeToFile(corePluginProperties,
                "version = 1\n");
        File masterDataScript = new File(parentDir, "initialize-master-data.py");
        masterDataScript.createNewFile();
    }
}
