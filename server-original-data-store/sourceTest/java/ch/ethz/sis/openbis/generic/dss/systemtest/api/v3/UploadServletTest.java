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
package ch.ethz.sis.openbis.generic.dss.systemtest.api.v3;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.MultiPartRequestContent;
import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.client.StringRequestContent;

import org.eclipse.jetty.http.HttpCookieStore;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.MultiPart;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.systemsx.cisd.common.http.JettyHttpClientFactory;
import ch.systemsx.cisd.openbis.datastoreserver.systemtests.SystemTestCase;
import ch.systemsx.cisd.openbis.generic.client.web.server.UploadedFilesBean;
import ch.systemsx.cisd.openbis.generic.shared.util.TestInstanceHostUtils;

/**
 * @author pkupczyk
 */
public class UploadServletTest extends SystemTestCase
{

    private static final String SERVICE_URL = TestInstanceHostUtils.getOpenBISUrl() + "/openbis/upload";

    private static final String PARAM_SESSION_ID = "sessionID";

    private static final String PARAM_SESSION_KEY_PREFIX = "sessionKey_";

    private static final String PARAM_SESSION_KEYS_NUMBER = "sessionKeysNumber";

    private static final String USER = "test";

    private static final String PASSWORD = "password";

    private static final String FALSE_TRUE_PROVIDER = "false-true-provider";

    private IApplicationServerApi as;

    @BeforeClass
    private void beforeClass() throws Exception
    {
        as = applicationContext.getBean(IApplicationServerApi.class);
    }

    @BeforeMethod
    private void beforeMethod() throws Exception
    {
        HttpClient client = JettyHttpClientFactory.getHttpClient();
        HttpCookieStore store = client.getHttpCookieStore();
        store.clear();              // Jetty 12 equivalent of removeAll()
        cleanOSTempFolder();
    }

    @AfterMethod
    private void afterMethod() throws Exception
    {
        assertOSTempFolderFiles();
    }

    @Test(dataProvider = FALSE_TRUE_PROVIDER)
    public void testUploadSingleFile(boolean withSessionTokenParam) throws Exception
    {
        String sessionToken = as.login(USER, PASSWORD);

        if (false == withSessionTokenParam)
        {
            initHttpSession(sessionToken);
        }

        cleanSessionWorkspace(sessionToken);

        MultiPartRequestContent multipart = new MultiPartRequestContent();
        multipart.addPart(new MultiPart.ContentSourcePart(
                "testFieldName",
                "testFileName",
                HttpFields.EMPTY,
                new StringRequestContent("testContent")
        ));
        multipart.close();

        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(SERVICE_URL).method(HttpMethod.POST);
        if (withSessionTokenParam)
        {
            request.param(PARAM_SESSION_ID, sessionToken);
        }
        request.param(PARAM_SESSION_KEYS_NUMBER, "1");
        request.param(PARAM_SESSION_KEY_PREFIX + "0", "testFieldName");
        request.body(multipart);

        request.send();

        assertSessionWorkspaceFiles(sessionToken, "testContent");
    }

    @Test(dataProvider = FALSE_TRUE_PROVIDER)
    public void testUploadMultipleFilesUnderOneSessionKey(boolean withSessionTokenParam) throws Exception
    {
        String sessionToken = as.login(USER, PASSWORD);

        if (false == withSessionTokenParam)
        {
            initHttpSession(sessionToken);
        }

        cleanSessionWorkspace(sessionToken);

        MultiPartRequestContent multipart = new MultiPartRequestContent();
        multipart.addPart(new MultiPart.ContentSourcePart(
                "testFieldName1", "testFileName1", HttpFields.EMPTY,
                new StringRequestContent("testContent1")
        ));
        multipart.addPart(new MultiPart.ContentSourcePart(
                "testFieldName2", "testFileName2", HttpFields.EMPTY,
                new StringRequestContent("testContent2")
        ));
        multipart.close();

        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(SERVICE_URL).method(HttpMethod.POST);
        if (withSessionTokenParam)
        {
            request.param(PARAM_SESSION_ID, sessionToken);
        }
        request.param(PARAM_SESSION_KEYS_NUMBER, "1");
        request.param(PARAM_SESSION_KEY_PREFIX + "0", "testFieldName");
        request.body(multipart);

        request.send();

        assertSessionWorkspaceFiles(sessionToken, "testContent1", "testContent2");
    }

    @Test(dataProvider = FALSE_TRUE_PROVIDER)
    public void testUploadMultipleFilesUnderMultipleSessionKeys(boolean withSessionTokenParam) throws Exception
    {
        String sessionToken = as.login(USER, PASSWORD);

        if (false == withSessionTokenParam)
        {
            initHttpSession(sessionToken);
        }

        cleanSessionWorkspace(sessionToken);

        MultiPartRequestContent multipart = new MultiPartRequestContent();
        multipart.addPart(new MultiPart.ContentSourcePart(
                "testFieldName1", "testFileName1", HttpFields.EMPTY,
                new StringRequestContent("testContent1")
        ));
        multipart.addPart(new MultiPart.ContentSourcePart(
                "testFieldName2", "testFileName2", HttpFields.EMPTY,
                new StringRequestContent("testContent2")
        ));
        multipart.close();

        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(SERVICE_URL).method(HttpMethod.POST);
        if (withSessionTokenParam)
        {
            request.param(PARAM_SESSION_ID, sessionToken);
        }
        request.param(PARAM_SESSION_KEYS_NUMBER, "2");
        request.param(PARAM_SESSION_KEY_PREFIX + "0", "testFieldName1");
        request.param(PARAM_SESSION_KEY_PREFIX + "1", "testFieldName2");
        request.body(multipart);

        request.send();

        assertSessionWorkspaceFiles(sessionToken, "testContent1", "testContent2");
    }

    @Test
    public void testUploadWithHttpSessionValidAndWithoutSessionTokenParam() throws Exception
    {
        String sessionToken = as.login(USER, PASSWORD);
        initHttpSession(sessionToken);
        cleanSessionWorkspace(sessionToken);
        upload(null, "testContent");
        assertSessionWorkspaceFiles(sessionToken, "testContent");
    }

    @Test
    public void testUploadWithHttpSessionValidAndWithSessionTokenParamInvalid() throws Exception
    {
        String sessionToken1 = as.login(USER, PASSWORD);

        initHttpSession(sessionToken1);

        String sessionToken2 = "admin-invalidtoken";

        cleanSessionWorkspace(sessionToken1);
        cleanSessionWorkspace(sessionToken2);

        ContentResponse response = upload(sessionToken2, "testContent");

        assertEquals("<message type=\"error\">Session token '" + sessionToken2 + "' is invalid: user is not logged in.</message>",
                response.getContentAsString());

        assertSessionWorkspaceFiles(sessionToken1);
        assertSessionWorkspaceFiles(sessionToken2);
    }

    @Test
    public void testUploadWithHttpSessionValidAndWithSessionTokenParamValid() throws Exception
    {
        String sessionToken1 = as.login(USER, PASSWORD);

        initHttpSession(sessionToken1);

        String sessionToken2 = as.login(USER, PASSWORD);

        cleanSessionWorkspace(sessionToken1);
        cleanSessionWorkspace(sessionToken2);

        upload(sessionToken2, "testContent");

        assertSessionWorkspaceFiles(sessionToken1);
        assertSessionWorkspaceFiles(sessionToken2, "testContent");
    }

    @Test
    public void testUploadWithHttpSessionInvalidAndWithoutSessionTokenParam() throws Exception
    {
        String sessionToken = as.login(USER, PASSWORD);

        initHttpSession(sessionToken);

        // invalidate the session
        as.logout(sessionToken);

        cleanSessionWorkspace(sessionToken);

        ContentResponse response = upload(null, "testContent");

        assertEquals("<message type=\"error\">Session token '" + sessionToken + "' is invalid: user is not logged in.</message>",
                response.getContentAsString());

        assertSessionWorkspaceFiles(sessionToken);
    }

    @Test
    public void testUploadWithHttpSessionInvalidAndWithSessionTokenParamValid() throws Exception
    {
        String sessionToken1 = as.login(USER, PASSWORD);

        initHttpSession(sessionToken1);

        // invalidate the session
        as.logout(sessionToken1);

        String sessionToken2 = as.login(USER, PASSWORD);

        cleanSessionWorkspace(sessionToken1);
        cleanSessionWorkspace(sessionToken2);

        upload(sessionToken2, "testContent");

        assertSessionWorkspaceFiles(sessionToken1);
        assertSessionWorkspaceFiles(sessionToken2, "testContent");
    }

    @Test
    public void testUploadWithHttpSessionInvalidAndWithSessionTokenParamInvalid() throws Exception
    {
        String sessionToken1 = as.login(USER, PASSWORD);

        initHttpSession(sessionToken1);

        // invalidate the session
        as.logout(sessionToken1);

        String sessionToken2 = "admin-invalidtoken";

        cleanSessionWorkspace(sessionToken1);
        cleanSessionWorkspace(sessionToken2);

        ContentResponse response = upload(sessionToken2, "testContent");

        assertEquals("<message type=\"error\">Session token '" + sessionToken2 + "' is invalid: user is not logged in.</message>",
                response.getContentAsString());

        assertSessionWorkspaceFiles(sessionToken1);
        assertSessionWorkspaceFiles(sessionToken2);
    }

    @Test
    public void testUploadWithoutHttpSessionAndWithoutSessionTokenParam() throws Exception
    {
        ContentResponse response = upload(null, "testContent");
        assertEquals("<message type=\"error\">Pre-existing session required but none found</message>", response.getContentAsString());
    }

    @Test
    public void testUploadWithoutHttpSessionAndWithSessionTokenParamValid() throws Exception
    {
        String sessionToken = as.login(USER, PASSWORD);
        cleanSessionWorkspace(sessionToken);
        upload(sessionToken, "testContent");
        assertSessionWorkspaceFiles(sessionToken, "testContent");
    }

    @Test
    public void testUploadWithoutHttpSessionAndWithSessionTokenParamInvalid() throws Exception
    {
        String sessionToken = "admin-invalidtoken";

        cleanSessionWorkspace(sessionToken);

        ContentResponse response = upload(sessionToken, "testContent");

        assertEquals("<message type=\"error\">Session token '" + sessionToken + "' is invalid: user is not logged in.</message>",
                response.getContentAsString());

        assertSessionWorkspaceFiles(sessionToken);
    }

    @Test
    public void testUploadWithoutSessionKeysNumberParam() throws Exception
    {
        String sessionToken = as.login(USER, PASSWORD);

        MultiPartRequestContent multipart = new MultiPartRequestContent();
        multipart.addPart(new MultiPart.ContentSourcePart(
                "testFieldName",                    // field name
                "testFileName",                     // filename
                HttpFields.EMPTY,                   // headers
                new StringRequestContent("testContent")  // content (text/plain by default)
        ));
        multipart.close();

        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(SERVICE_URL).method(HttpMethod.POST);
        request.param(PARAM_SESSION_ID, sessionToken);
        request.param(PARAM_SESSION_KEY_PREFIX + "0", "testFieldName");
        request.body(multipart);

        ContentResponse response = request.send();
        assertEquals("<message type=\"error\">No form field 'sessionKeysNumber' could be found in the transmitted form.</message>",
                response.getContentAsString());
    }

    @Test
    public void testUploadWithIncorrectSessionKeysNumberParamFormat() throws Exception
    {
        String sessionToken = as.login(USER, PASSWORD);

        MultiPartRequestContent multipart = new MultiPartRequestContent();
        multipart.addPart(new MultiPart.ContentSourcePart(
                "testFieldName",
                "testFileName",
                HttpFields.EMPTY,
                new StringRequestContent("testContent")
        ));
        multipart.close();

        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(SERVICE_URL).method(HttpMethod.POST);
        request.param(PARAM_SESSION_ID, sessionToken);
        request.param(PARAM_SESSION_KEYS_NUMBER, "thisShouldBeANumber");
        request.param(PARAM_SESSION_KEY_PREFIX + "0", "testFieldName");
        request.body(multipart);

        ContentResponse response = request.send();
        assertEquals("<message type=\"error\">For input string: \"thisShouldBeANumber\"</message>",
                response.getContentAsString());
    }

    @Test
    public void testUploadWithoutSessionKeyPrefixParam() throws Exception
    {
        String sessionToken = as.login(USER, PASSWORD);

        MultiPartRequestContent multipart = new MultiPartRequestContent();
        multipart.addPart(new MultiPart.ContentSourcePart(
                "testFieldName",
                "testFileName",
                HttpFields.EMPTY,
                new StringRequestContent("testContent")
        ));
        multipart.close();

        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(SERVICE_URL).method(HttpMethod.POST);
        request.param(PARAM_SESSION_ID, sessionToken);
        request.param(PARAM_SESSION_KEYS_NUMBER, "1");
        request.body(multipart);

        ContentResponse response = request.send();

        assertEquals("<message type=\"error\">No field '" + PARAM_SESSION_KEY_PREFIX + "0' could be found in the transmitted form.</message>",
                response.getContentAsString());
    }

    @Test
    public void testUploadWithTooFewSessionKeyPrefixParams() throws Exception
    {
        String sessionToken = as.login(USER, PASSWORD);

        MultiPartRequestContent multipart = new MultiPartRequestContent();
        multipart.addPart(new MultiPart.ContentSourcePart(
                "testFieldName",
                "testFileName",
                HttpFields.EMPTY,
                new StringRequestContent("testContent")
        ));
        multipart.close();

        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(SERVICE_URL).method(HttpMethod.POST);
        request.param(PARAM_SESSION_ID, sessionToken);
        request.param(PARAM_SESSION_KEYS_NUMBER, "2");
        request.param(PARAM_SESSION_KEY_PREFIX + "0", "testFieldName");
        request.body(multipart);

        ContentResponse response = request.send();

        assertEquals("<message type=\"error\">No field '" + PARAM_SESSION_KEY_PREFIX + "1' could be found in the transmitted form.</message>",
                response.getContentAsString());
    }

    @Test
    public void testUploadWithLogout() throws Exception
    {
        String sessionToken = as.login(USER, PASSWORD);

        upload(sessionToken, "testContent");

        assertSessionWorkspaceFiles(sessionToken, "testContent");

        as.logout(sessionToken);

        assertSessionWorkspaceFiles(sessionToken);
    }

    private File getSessionWorkspace(String sessionToken) throws Exception
    {
        File sessionWorkspaceRootDir = new File("targets/sessionWorkspace");
        return new File(sessionWorkspaceRootDir, sessionToken);
    }

    private void cleanSessionWorkspace(String sessionToken) throws Exception
    {
        File sessionWorkspace = getSessionWorkspace(sessionToken);

        if (sessionWorkspace.exists())
        {
            FileUtils.deleteQuietly(sessionWorkspace);
        }

        assertSessionWorkspaceFiles(sessionToken);
    }

    private void assertSessionWorkspaceFiles(String sessionToken, String... fileContents) throws Exception
    {
        assertFiles(getSessionWorkspace(sessionToken).listFiles(), fileContents);
    }

    private void initHttpSession(String sessionToken) throws Exception
    {
        // upload a dummy file to initialize HTTP session
        MultiPartRequestContent multipart = new MultiPartRequestContent();
        multipart.addPart(new MultiPart.ContentSourcePart(
                "initHttpSession",              // field name
                "initHttpSession",              // filename
                HttpFields.EMPTY,               // headers (none)
                new StringRequestContent("initHttpSession") // content (defaults to text/plain)
        ));
        multipart.close();

        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(SERVICE_URL).method(HttpMethod.POST);
        request.param(PARAM_SESSION_ID, sessionToken);
        request.param(PARAM_SESSION_KEYS_NUMBER, "1");
        request.param(PARAM_SESSION_KEY_PREFIX + "0", "initHttpSession");

        request.body(multipart);
        request.send();
    }

    private ContentResponse upload(String sessionToken, String fileContent) throws Exception
    {
        MultiPartRequestContent multipart = new MultiPartRequestContent();
        multipart.addPart(new MultiPart.ContentSourcePart(
                "testFieldName",
                "testFileName",
                HttpFields.EMPTY,
                new StringRequestContent(fileContent)
        ));
        multipart.close();

        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(SERVICE_URL).method(HttpMethod.POST);
        if (sessionToken != null)
        {
            request.param(PARAM_SESSION_ID, sessionToken);
        }
        request.param(PARAM_SESSION_KEYS_NUMBER, "1");
        request.param(PARAM_SESSION_KEY_PREFIX + "0", "testFieldName");

        request.body(multipart);
        return request.send();
    }

    private File getOSTempFolder() throws Exception
    {
        return new File(System.getProperty("java.io.tmpdir"));
    }

    private void cleanOSTempFolder() throws Exception
    {
        File tempFolder = getOSTempFolder();

        if (tempFolder.exists())
        {
            File[] files = tempFolder.listFiles(new OSTempFolderFileFilter());

            for (File file : files)
            {
                FileUtils.deleteQuietly(file);
            }
        }

        assertOSTempFolderFiles();
    }

    private void assertOSTempFolderFiles(String... fileContents) throws Exception
    {
        assertFiles(getOSTempFolder().listFiles(new OSTempFolderFileFilter()), fileContents);
    }

    private void assertFiles(File[] files, String... fileContents) throws Exception
    {
        List<String> expectedContents = new ArrayList<String>(Arrays.asList(fileContents));
        List<String> actualContents = new ArrayList<String>();

        if (files != null)
        {
            for (File file : files)
            {
                actualContents.add(FileUtils.readFileToString(file));
            }
        }

        expectedContents.sort(String.CASE_INSENSITIVE_ORDER);
        actualContents.sort(String.CASE_INSENSITIVE_ORDER);

        assertEquals(expectedContents, actualContents);
    }

    private static class OSTempFolderFileFilter implements FileFilter
    {

        @Override
        public boolean accept(File file)
        {
            return file.getName().startsWith(UploadedFilesBean.class.getSimpleName());
        }

    }

    @DataProvider(name = FALSE_TRUE_PROVIDER)
    public static Object[][] provideFalseTrue()
    {
        return new Object[][] { { false }, { true } };
    }

}
