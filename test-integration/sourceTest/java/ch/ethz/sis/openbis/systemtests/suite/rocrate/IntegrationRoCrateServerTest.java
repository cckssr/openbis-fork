package ch.ethz.sis.openbis.systemtests.suite.rocrate;

import static ch.ethz.sis.openbis.systemtests.suite.rocrate.environment.RoCrateServerIntegrationTestEnvironment.environment;
import static org.testng.Assert.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.BytesRequestContent;
import org.eclipse.jetty.http.HttpMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.systemtests.suite.rocrate.environment.RoCrateServerIntegrationTestEnvironment;
import ch.systemsx.cisd.common.http.JettyHttpClientFactory;
import ch.systemsx.cisd.common.test.AssertionUtil;
import ch.systemsx.cisd.openbis.generic.shared.util.TestInstanceHostUtils;

public class IntegrationRoCrateServerTest
{

    private static int TIMEOUT = 5 * 60 * 1000;

    private static String username = "system";

    private static String password = "changeit";

    @BeforeSuite
    public void beforeSuite()
    {
        RoCrateServerIntegrationTestEnvironment.start();
    }

    @AfterSuite
    public void afterSuite()
    {
        RoCrateServerIntegrationTestEnvironment.stop();
    }

    @Test
    public void testTestEcho() throws Exception
    {
        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(TestInstanceHostUtils.getRoCrateUrl() + "/openbis/open-api/ro-crate/test-echo");
        request.param("message", "Hello World");

        ContentResponse response = request.send();
        assertEquals(response.getStatus(), 200);
        assertEquals(response.getContentAsString(), "Hello World");
    }

    @Test
    public void testTestOpenBISConnection() throws Exception
    {
        OpenBIS openBIS = environment.createOpenBIS(TIMEOUT);
        openBIS.login(username, password);

        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(TestInstanceHostUtils.getRoCrateUrl() + "/openbis/open-api/ro-crate/test-openbis-connection");
        request.param("api-key", openBIS.getSessionToken());

        ContentResponse response = request.send();
        assertEquals(response.getStatus(), 200);
        assertEquals(response.getContentAsString(), username);
    }

    @Test(enabled = false) // This takes over 30 seconds, should be converted to async implementation
    public void testImport() throws Exception
    {
        OpenBIS openBIS = environment.createOpenBIS(TIMEOUT);
        openBIS.login(username, password);

        Path file = Path.of("sourceTest/resource/" + getClass().getSimpleName() + "/OkayExample.json");

        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(TestInstanceHostUtils.getRoCrateUrl() + "/openbis/open-api/ro-crate/import");
        request.method(HttpMethod.POST);
        request.header("api-key", openBIS.getSessionToken());
        request.header("Content-Type", "application/ld+json");
        request.body(new BytesRequestContent(Files.readAllBytes(file)));

        ContentResponse response = request.send();
        assertEquals(response.getStatus(), 200);
    }

    @Test(enabled = false) // This takes over 30 seconds, should be converted to async implementation
    public void testImportZip()
            throws Exception
    {
        OpenBIS openBIS = environment.createOpenBIS(TIMEOUT);
        openBIS.login(username, password);

        Path file = Path.of("sourceTest/resource/" + getClass().getSimpleName() + "/OkayExample.zip");

        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(TestInstanceHostUtils.getRoCrateUrl() + "/openbis/open-api/ro-crate/import");
        request.method(HttpMethod.POST);
        request.header("api-key", openBIS.getSessionToken());
        request.header("Content-Type", "application/zip");
        request.body(new BytesRequestContent(Files.readAllBytes(file)));

        ContentResponse response = request.send();
        assertEquals(response.getStatus(), 200);
    }

    @Test
    public void testValidate()
            throws Exception
    {
        OpenBIS openBIS = environment.createOpenBIS(TIMEOUT);
        openBIS.login(username, password);

        Path file = Path.of("sourceTest/resource/" + getClass().getSimpleName() + "/OkayExample.json");

        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(TestInstanceHostUtils.getRoCrateUrl() + "/openbis/open-api/ro-crate/validate");
        request.method(HttpMethod.POST);
        request.header("api-key", openBIS.getSessionToken());
        request.header("Content-Type", "application/ld+json");
        request.header("Accept", "application/json");
        request.body(new BytesRequestContent(Files.readAllBytes(file)));

        ContentResponse response = request.send();
        assertEquals(response.getStatus(), 200);
        AssertionUtil.assertContains("\"isValid\":true", response.getContentAsString());
    }

    @Test
    public void testValidateZip()
            throws Exception
    {
        OpenBIS openBIS = environment.createOpenBIS(TIMEOUT);
        openBIS.login(username, password);

        Path file = Path.of("sourceTest/resource/" + getClass().getSimpleName() + "/OkayExample.zip");

        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(TestInstanceHostUtils.getRoCrateUrl() + "/openbis/open-api/ro-crate/validate");
        request.method(HttpMethod.POST);
        request.header("api-key", openBIS.getSessionToken());
        request.header("Content-Type", "application/zip");
        request.header("Accept", "application/json");
        request.body(new BytesRequestContent(Files.readAllBytes(file)));

        ContentResponse response = request.send();
        assertEquals(response.getStatus(), 200);
        AssertionUtil.assertContains("\"validationErrors\":[]", response.getContentAsString());
        AssertionUtil.assertContains("\"isValid\":true", response.getContentAsString());
    }

    @Test
    public void testValidateUnknown()
            throws Exception
    {
        OpenBIS openBIS = environment.createOpenBIS(TIMEOUT);
        openBIS.login(username, password);

        Path file = Path.of("sourceTest/resource/" + getClass().getSimpleName() + "/UnknownProperty.json");

        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(TestInstanceHostUtils.getRoCrateUrl() + "/openbis/open-api/ro-crate/validate");
        request.method(HttpMethod.POST);
        request.header("api-key", openBIS.getSessionToken());
        request.header("Content-Type", "application/ld+json");
        request.header("Accept", "application/json");
        request.body(new BytesRequestContent(Files.readAllBytes(file)));

        ContentResponse response = request.send();
        assertEquals(response.getStatus(), 200);
        AssertionUtil.assertContains("\"validationErrors\":[{", response.getContentAsString());
        AssertionUtil.assertContains("\"isValid\":false", response.getContentAsString());
        AssertionUtil.assertContains("\"property\":\"wrong\"", response.getContentAsString());
        AssertionUtil.assertContains("\"message\":\"Property not in schema\"", response.getContentAsString());
    }

    @Test
    public void testValidateWrong()
            throws Exception
    {
        OpenBIS openBIS = environment.createOpenBIS(TIMEOUT);
        openBIS.login(username, password);

        Path file = Path.of("sourceTest/resource/" + getClass().getSimpleName() + "/WrongDataType.json");

        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(TestInstanceHostUtils.getRoCrateUrl() + "/openbis/open-api/ro-crate/validate");
        request.method(HttpMethod.POST);
        request.header("api-key", openBIS.getSessionToken());
        request.header("Content-Type", "application/ld+json");
        request.header("Accept", "application/json");
        request.body(new BytesRequestContent(Files.readAllBytes(file)));

        ContentResponse response = request.send();
        assertEquals(response.getStatus(), 200);
        AssertionUtil.assertContains("\"validationErrors\":[{", response.getContentAsString());
        AssertionUtil.assertContains("\"isValid\":false", response.getContentAsString());
        AssertionUtil.assertContains("NUMBEROFFILES", response.getContentAsString());
    }

    @Test(enabled = false)// This test depends on some data which should be created before the test runs
    public void testExportDOI()
            throws Exception
    {
        OpenBIS openBIS = environment.createOpenBIS(TIMEOUT);
        openBIS.login(username, password);

        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(TestInstanceHostUtils.getRoCrateUrl() + "/openbis/open-api/ro-crate/export");
        request.method(HttpMethod.POST);
        request.header("api-key", openBIS.getSessionToken());
        request.header("openbis.with-Levels-below", "true");
        request.header("Content-Type", "application/json");
        request.header("Accept", "application/ld+json");
        request.body(new BytesRequestContent("[\"https://doi.org/10.1038/s41586-020-3010-5\"]".getBytes()));

        ContentResponse response = request.send();
        assertEquals(response.getStatus(), 200);
        assertEquals(response.getHeaders().get("Content-Type"), "application/ld+json");
        AssertionUtil.assertContains("@context", response.getContentAsString());
    }

    @Test(enabled = false) // This test depends on some data which should be created before the test runs
    public void testExportDOIZip()
            throws Exception
    {
        OpenBIS openBIS = environment.createOpenBIS(TIMEOUT);
        openBIS.login(username, password);

        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(TestInstanceHostUtils.getRoCrateUrl() + "/openbis/open-api/ro-crate/export");
        request.method(HttpMethod.POST);
        request.header("api-key", openBIS.getSessionToken());
        request.header("openbis.with-Levels-below", "true");
        request.header("Content-Type", "application/json");
        request.header("Accept", "application/zip");
        request.body(new BytesRequestContent("[\"https://doi.org/10.1038/s41586-020-3010-5\"]".getBytes()));

        ContentResponse response = request.send();
        assertEquals(response.getStatus(), 200);
        assertEquals(response.getHeaders().get("Content-Type"), "application/zip");
    }

    @Test(enabled = false) // This depends on some data which should be created before the test runs
    public void testExportIdentifier()
            throws Exception
    {
        OpenBIS openBIS = environment.createOpenBIS(TIMEOUT);
        openBIS.login(username, password);

        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(TestInstanceHostUtils.getRoCrateUrl() + "/openbis/open-api/ro-crate/export");
        request.method(HttpMethod.POST);
        request.header("api-key", openBIS.getSessionToken());
        request.header("openbis.with-Levels-below", "true");
        request.header("Content-Type", "application/json");
        request.header("Accept", "application/ld+json");
        request.body(new BytesRequestContent("[\"/PUBLICATIONS/PUBLIC_REPOSITORIES/PUB29\"]".getBytes()));

        ContentResponse response = request.send();
        assertEquals(response.getStatus(), 200);
    }

    @Test(enabled = false)
    // PermIds depend on when the import was done. This can lead to false failure.
    // As long as we don't have a good solution for search in tests, this is disabled.
    public void testExportPermId()
            throws Exception
    {
        OpenBIS openBIS = environment.createOpenBIS(TIMEOUT);
        openBIS.login(username, password);

        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(TestInstanceHostUtils.getRoCrateUrl() + "/openbis/open-api/ro-crate/export");
        request.method(HttpMethod.POST);
        request.header("api-key", openBIS.getSessionToken());
        request.header("openbis.with-Levels-below", "true");
        request.header("Content-Type", "application/json");
        request.body(new BytesRequestContent("[\"20250728111931402-94\"]".getBytes()));

        ContentResponse response = request.send();
        assertEquals(response.getStatus(), 200);
    }

    @Test
    public void testExportEmptyResults()
            throws Exception
    {
        OpenBIS openBIS = environment.createOpenBIS(TIMEOUT);
        openBIS.login(username, password);

        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(TestInstanceHostUtils.getRoCrateUrl() + "/openbis/open-api/ro-crate/export");
        request.method(HttpMethod.POST);
        request.header("api-key", openBIS.getSessionToken());
        request.header("openbis.with-Levels-below", "true");
        request.header("Content-Type", "application/json");
        request.body(new BytesRequestContent("[\"NOT-AN-IDENTIFIER\"]".getBytes()));

        ContentResponse response = request.send();
        assertEquals(response.getStatus(), 404);
    }

    // https://github.com/paulscherrerinstitute/rocrate-api/issues/40
    @Test
    public void testValidateMalformedCrate()
            throws Exception
    {
        OpenBIS openBIS = environment.createOpenBIS(TIMEOUT);
        openBIS.login(username, password);

        Path file = Path.of("sourceTest/resource/" + getClass().getSimpleName() + "/Malformed.json");

        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(TestInstanceHostUtils.getRoCrateUrl() + "/openbis/open-api/ro-crate/validate");
        request.method(HttpMethod.POST);
        request.header("api-key", openBIS.getSessionToken());
        request.header("Content-Type", "application/ld+json");
        request.header("Accept", "application/json");
        request.body(new BytesRequestContent(Files.readAllBytes(file)));

        ContentResponse response = request.send();
        assertEquals(response.getStatus(), 400);
    }

    // https://github.com/paulscherrerinstitute/rocrate-api/issues/39
    @Test
    public void testValidateMalformedCrateZipped()
            throws Exception
    {
        OpenBIS openBIS = environment.createOpenBIS(TIMEOUT);
        openBIS.login(username, password);

        Path file = Path.of("sourceTest/resource/" + getClass().getSimpleName() + "/Malformed.zip");

        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(TestInstanceHostUtils.getRoCrateUrl() + "/openbis/open-api/ro-crate/validate");
        request.method(HttpMethod.POST);
        request.header("api-key", openBIS.getSessionToken());
        request.header("Content-Type", "application/zip");
        request.header("Accept", "application/json");
        request.body(new BytesRequestContent(Files.readAllBytes(file)));

        ContentResponse response = request.send();
        assertEquals(response.getStatus(), 400);
    }

    // https://github.com/paulscherrerinstitute/rocrate-api/issues/35
    @Test(enabled = false) // This uses MissingManifest.zip file which does not exist
    public void testValidateMalformedCrateZippedMissingManifest()
            throws Exception
    {
        OpenBIS openBIS = environment.createOpenBIS(TIMEOUT);
        openBIS.login(username, password);

        Path file = Path.of("sourceTest/resource/" + getClass().getSimpleName() + "/MissingManifest.zip");

        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(TestInstanceHostUtils.getRoCrateUrl() + "/openbis/open-api/ro-crate/validate");
        request.method(HttpMethod.POST);
        request.header("api-key", openBIS.getSessionToken());
        request.header("Content-Type", "application/zip");
        request.header("Accept", "application/json");
        request.body(new BytesRequestContent(Files.readAllBytes(file)));

        ContentResponse response = request.send();
        assertEquals(response.getStatus(), 400);
    }

    // https://github.com/paulscherrerinstitute/rocrate-api/issues/41

    @Test
    public void testEmptyPayloadZip()
            throws Exception
    {
        OpenBIS openBIS = environment.createOpenBIS(TIMEOUT);
        openBIS.login(username, password);

        Path file = Path.of("sourceTest/resource/" + getClass().getSimpleName() + "/empty.zip");

        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(TestInstanceHostUtils.getRoCrateUrl() + "/openbis/open-api/ro-crate/validate");
        request.method(HttpMethod.POST);
        request.header("api-key", openBIS.getSessionToken());
        request.header("Content-Type", "application/zip");
        request.header("Accept", "application/json");
        request.body(new BytesRequestContent(Files.readAllBytes(file)));

        ContentResponse response = request.send();
        assertEquals(response.getStatus(), 400);
    }

    // https://github.com/paulscherrerinstitute/rocrate-api/issues/42

    @Test
    public void testEmptyPayload()
            throws Exception
    {
        OpenBIS openBIS = environment.createOpenBIS(TIMEOUT);
        openBIS.login(username, password);

        Path file = Path.of("sourceTest/resource/" + getClass().getSimpleName() + "/empty.json");

        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(TestInstanceHostUtils.getRoCrateUrl() + "/openbis/open-api/ro-crate/validate");
        request.method(HttpMethod.POST);
        request.header("api-key", openBIS.getSessionToken());
        request.header("Content-Type", "application/ld+json");
        request.header("Accept", "application/json");
        request.body(new BytesRequestContent(Files.readAllBytes(file)));

        ContentResponse response = request.send();
        assertEquals(response.getStatus(), 400);
    }

    // https://github.com/paulscherrerinstitute/rocrate-api/issues/54

    @Test
    public void testInvalidAcceptHeader()
            throws Exception
    {
        OpenBIS openBIS = environment.createOpenBIS(TIMEOUT);
        openBIS.login(username, password);

        Path file = Path.of("sourceTest/resource/" + getClass().getSimpleName() + "/OkayExample.json");

        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(TestInstanceHostUtils.getRoCrateUrl() + "/openbis/open-api/ro-crate/validate");
        request.method(HttpMethod.POST);
        request.header("api-key", openBIS.getSessionToken());
        request.header("Content-Type", "application/ld+json");
        request.header("Accept", "application/xml");
        request.body(new BytesRequestContent(Files.readAllBytes(file)));

        ContentResponse response = request.send();
        assertEquals(response.getStatus(), 406);
    }

    @Test
    public void testInvalidContentType()
            throws Exception
    {
        OpenBIS openBIS = environment.createOpenBIS(TIMEOUT);
        openBIS.login(username, password);

        Path file = Path.of("sourceTest/resource/" + getClass().getSimpleName() + "/OkayExample.json");

        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(TestInstanceHostUtils.getRoCrateUrl() + "/openbis/open-api/ro-crate/validate");
        request.method(HttpMethod.POST);
        request.header("api-key", openBIS.getSessionToken());
        request.header("Content-Type", "application/xml");
        request.header("Accept", "application/json");
        request.body(new BytesRequestContent(Files.readAllBytes(file)));

        ContentResponse response = request.send();
        assertEquals(response.getStatus(), 415);
    }

}
