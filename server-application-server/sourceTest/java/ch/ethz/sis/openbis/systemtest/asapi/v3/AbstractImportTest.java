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

package ch.ethz.sis.openbis.systemtest.asapi.v3;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import ch.systemsx.cisd.openbis.generic.server.CommonServiceProvider;
import ch.systemsx.cisd.openbis.generic.shared.ISessionWorkspaceProvider;

public class AbstractImportTest extends AbstractTest
{

    private static final String VERSIONING_JSON = "targets/xls-import-version-info-test.json";

    private static final String XLS_VERSIONING_DIR = "xls-import.version-data-file";

    protected String sessionToken;

    @BeforeSuite
    public void setupSuite()
    {
        System.setProperty("as."+XLS_VERSIONING_DIR, VERSIONING_JSON);
    }

    @BeforeMethod
    public void beforeTest()
    {
        sessionToken = v3api.login(TEST_USER, PASSWORD);
    }

    @AfterMethod
    public void afterTest()
    {
        final File file = new File(VERSIONING_JSON);

        System.out.println("Versioning file: " + file.getAbsolutePath());
        System.out.println("Versioning file exists: " + file.exists());

        file.delete();

        v3api.logout(sessionToken);
    }

    protected static String[] uploadToAsSessionWorkspace(final String sessionToken, final String... relativeFilePaths) throws IOException
    {
        final String[] canonicalFilePaths = getFilePaths(relativeFilePaths);
        final ISessionWorkspaceProvider sessionWorkspaceProvider = CommonServiceProvider.getSessionWorkspaceProvider();
        final String uploadId = UUID.randomUUID().toString();
        final String[] destinations = new String[canonicalFilePaths.length];

        for (int i = 0; i < canonicalFilePaths.length; i++)
        {
            destinations[i] = uploadId + "/" + relativeFilePaths[i];
            sessionWorkspaceProvider.write(sessionToken, destinations[i], new FileInputStream(canonicalFilePaths[i]));
        }

        return destinations;
    }

    private static String[] getFilePaths(final String... fileNames)
    {
        return Arrays.stream(fileNames).map(fileName -> AbstractImportTest.class.getResource("test_files/import/" + fileName).getPath())
                .toArray(String[]::new);
    }

}
