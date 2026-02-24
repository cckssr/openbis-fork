package ch.ethz.sis.openbis.systemtests.suite.rocrate;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.data.ImportData;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.data.ImportFormat;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.options.ImportMode;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.options.ImportOptions;
import ch.ethz.sis.openbis.systemtests.suite.rocrate.environment.RoCrateServerIntegrationTestEnvironment;
import ch.systemsx.cisd.common.http.JettyHttpClientFactory;
import ch.systemsx.cisd.openbis.generic.shared.util.TestInstanceHostUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.client.BytesRequestContent;
import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.http.HttpMethod;
import org.junit.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static ch.ethz.sis.openbis.systemtests.suite.rocrate.environment.RoCrateServerIntegrationTestEnvironment.environment;
import static org.testng.Assert.*;

public class IntegrationRoCrateServerTest
{

    private static final int TIMEOUT = 4 * 60 * 1000;

    private static String username = "system";

    private static String password = "changeit";

    public static final String HEADER_API_KEY = "api-key";

    private static ObjectMapper objectMapper = new ObjectMapper();

    @BeforeSuite
    public void beforeSuite()
    {
        RoCrateServerIntegrationTestEnvironment.start();

        Path file =
                Path.of("sourceTest/resource/" + getClass().getSimpleName() + "/metadata_setup.xlsx");

        OpenBIS openBIS = environment.createOpenBIS();
        openBIS.login(username, password);
        String uploadedFile = openBIS.uploadToSessionWorkspace(file);

        ImportData importData = new ImportData();
        importData.setFormat(ImportFormat.EXCEL);
        importData.setSessionWorkspaceFiles(new String[] { file.getFileName().toString() });
        ImportOptions importOptions = new ImportOptions();
        importOptions.setMode(ImportMode.UPDATE_IF_EXISTS);
        openBIS.executeImport(importData, importOptions);
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
        OpenBIS openBIS = environment.createOpenBIS();
        openBIS.login(username, password);

        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(TestInstanceHostUtils.getRoCrateUrl() + "/openbis/open-api/ro-crate/test-openbis-connection");
        request.param("api-key", openBIS.getSessionToken());

        ContentResponse response = request.send();
        assertEquals(response.getStatus(), 200);
        assertEquals(response.getContentAsString(), username);
    }

    @Test(enabled = true, timeOut = TIMEOUT)
    public void testImport() throws Exception
    {
        OpenBIS openBIS = environment.createOpenBIS(TIMEOUT);
        openBIS.login(username, password);

        Path file = Path.of("sourceTest/resource/" + getClass().getSimpleName() + "/OkayExample.json");

        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(TestInstanceHostUtils.getRoCrateUrl() + "/openbis/open-api/ro-crate/import");
        request.method(HttpMethod.POST);
        request.headers(headers -> {
                    headers.add("api-key", openBIS.getSessionToken());
                    headers.add("Content-Type", "application/ld+json");
                });
        request.body(new BytesRequestContent(Files.readAllBytes(file)));
        request.idleTimeout(TIMEOUT, TimeUnit.MILLISECONDS);

        ContentResponse response = request.send();
        LinkedHashMap asyncJob =
                objectMapper.readValue(response.getContentAsString(), LinkedHashMap.class);
        String jobId = asyncJob.get("jobId").toString();

        assertEquals(response.getStatus(), 202);

        boolean done = false;
        while (!done)
        {
            Request pollRequest = client.newRequest(
                    TestInstanceHostUtils.getRoCrateUrl() + "/openbis/open-api/ro-crate/status");
            pollRequest.method(HttpMethod.GET);
            pollRequest.headers(headers -> {
                headers.add("api-key", openBIS.getSessionToken());
                headers.add("jobId", jobId);
            });
            pollRequest.idleTimeout(TIMEOUT, TimeUnit.MILLISECONDS);
            ContentResponse pollResponse = pollRequest.send();
            LinkedHashMap asyncResult =
                    objectMapper.readValue(pollResponse.getContentAsString(), LinkedHashMap.class);

            if (asyncResult.get("status").equals("COMPLETED"))
            {
                done = true;
            }

            if (asyncResult.get("status").equals("FAILED"))
            {
                List<String> errors = (List<String>) asyncResult.get("errors");
                Assert.fail(errors.stream().collect(Collectors.joining(",")));
                done = true;
            }

            Thread.sleep(2000);
        }
    }

    @Test(enabled = false) // This takes over 30 seconds, should be converted to async implementation
    public void testImportZip()
            throws Exception
    {
        OpenBIS openBIS = environment.createOpenBIS();
        openBIS.login(username, password);

        Path file = Path.of("sourceTest/resource/" + getClass().getSimpleName() + "/OkayExample.zip");

        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(TestInstanceHostUtils.getRoCrateUrl() + "/openbis/open-api/ro-crate/import");
        request.method(HttpMethod.POST);
        request.headers(headers -> {
                    headers.add("api-key", openBIS.getSessionToken());
                    headers.add("Content-Type", "application/zip");
                });
        request.body(new BytesRequestContent(Files.readAllBytes(file)));

        ContentResponse response = request.send();
        LinkedHashMap asyncJob =
                objectMapper.readValue(response.getContentAsString(), LinkedHashMap.class);
        String jobId = asyncJob.get("jobId").toString();
        assertEquals(response.getStatus(), 202);

        boolean done = false;
        while (!done)
        {
            Request pollRequest = client.newRequest(
                    TestInstanceHostUtils.getRoCrateUrl() + "/openbis/open-api/ro-crate/status");
            pollRequest.method(HttpMethod.GET);
            pollRequest.headers(headers -> {
                headers.add("api-key", openBIS.getSessionToken());
                headers.add("jobId", jobId);
            });
            pollRequest.idleTimeout(TIMEOUT, TimeUnit.MILLISECONDS);
            ContentResponse pollResponse = pollRequest.send();
            LinkedHashMap asyncResult =
                    objectMapper.readValue(pollResponse.getContentAsString(), LinkedHashMap.class);

            if (asyncResult.get("status").equals("COMPLETED"))
            {
                done = true;
            }

            if (asyncResult.get("status").equals("FAILED"))
            {
                List<String> errors = (List<String>) asyncResult.get("errors");
                Assert.fail(errors.stream().collect(Collectors.joining(",")));
                done = true;
            }

            Thread.sleep(2000);
        }


    }

    @Test
    public void testValidate()
            throws Exception
    {
        OpenBIS openBIS = environment.createOpenBIS();
        openBIS.login(username, password);

        Consumer<LinkedHashMap<String, Object>> assertions = x ->
        {
            LinkedHashMap<String, Object> validationResult =
                    (LinkedHashMap<String, Object>) x.get("validationResult");

            boolean isValid = (boolean) (validationResult.get("isValid"));
            assertTrue(isValid);
        };

        testValidateAstract("OkayExample.json", "application/ld+json", assertions);

    }

    @Test
    public void testValidateZip()
            throws Exception
    {
        OpenBIS openBIS = environment.createOpenBIS();
        openBIS.login(username, password);

        Consumer<LinkedHashMap<String, Object>> assertions = x ->
        {
            LinkedHashMap<String, Object> validationResult =
                    (LinkedHashMap<String, Object>) x.get("validationResult");

            boolean isValid = (boolean) (validationResult.get("isValid"));
            assertTrue(isValid);
        };

        testValidateAstract("OkayExample.zip", "application/zip", assertions);
    }

    @Test
    public void testValidateUnknown()
            throws Exception
    {
        OpenBIS openBIS = environment.createOpenBIS();
        openBIS.login(username, password);

        Consumer<LinkedHashMap<String, Object>> assertions = x ->
        {
            LinkedHashMap<String, Object> validationResult =
                    (LinkedHashMap<String, Object>) x.get("validationResult");
            LinkedHashMap<String, Object> entitiesToUndefinedProperties =
                    (LinkedHashMap<String, Object>) validationResult.get(
                            "entititesToUndefinedProperties");
            assertEquals(2, entitiesToUndefinedProperties.size());

            boolean isValid = (boolean) (validationResult.get("isValid"));
            assertFalse(isValid);

        };

        testValidateAstract("UnknownProperty.json", "application/ld+json", assertions);

    }

    @Test
    public void testValidateWrong()
            throws Exception
    {
        Consumer<LinkedHashMap<String, Object>> validationStuff = x ->
        {
            LinkedHashMap<String, Object> validationResult =
                    (LinkedHashMap<String, Object>) x.get("validationResult");
            LinkedHashMap<String, Object> wrongDataTypes =
                    (LinkedHashMap<String, Object>) validationResult.get(
                            "wrongDataTypes");
            assertEquals(1, wrongDataTypes.size());

            boolean isValid = (boolean) (validationResult.get("isValid"));
            assertFalse(isValid);

        };

        testValidateAstract("WrongDataType.json", "application/ld+json", validationStuff);
    }

    @Test(enabled = true, timeOut = TIMEOUT)
    // This test depends on some data which should be created before the test runs
    public void testExportDOI()
            throws Exception
    {

        String payload = "[\"https://doi.org/10.1038/s41586-020-3010-5\"]";
        String mimeType = "application/ld+json";
        testExport(mimeType, payload, x -> testStatus(x, "COMPLETED"),
                x -> testStatus(x, "FAILED"), IntegrationRoCrateServerTest::checkDownload);
    }

    private static boolean testStatus(ContentResponse contentResponse, String asyncStatus)
    {
        try
        {
            LinkedHashMap asyncJob =
                    objectMapper.readValue(contentResponse.getContentAsString(),
                            LinkedHashMap.class);
            return asyncJob.get("status").toString().equals(asyncStatus);
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test(enabled = true, timeOut = TIMEOUT)
    // This test depends on some data which should be created before the test runs
    public void testExportDOIZip()
            throws Exception
    {
        String payload = "[\"https://doi.org/10.1038/s41586-020-3010-5\"]";
        String mimeType = "application/zip";
        testExport(mimeType, payload, x -> testStatus(x, "COMPLETED"),
                x -> testStatus(x, "FAILED"), IntegrationRoCrateServerTest::checkDownload);
    }

    @Test(enabled = true, timeOut = TIMEOUT)
    // This depends on some data which should be created before the test runs
    public void testExportIdentifier()
            throws Exception
    {
        String payload = "[\"/PUBLICATIONS/PUBLIC_REPOSITORIES/PUB29\"]";
        String mimeType = "application/ld+json";
        testExport(mimeType, payload, x -> testStatus(x, "COMPLETED"),
                x -> testStatus(x, "FAILED"), IntegrationRoCrateServerTest::checkDownload);
    }

    @Test(enabled = false, timeOut = TIMEOUT)
    // PermIds depend on when the import was done. This can lead to false failure.
    // As long as we don't have a good solution for search in tests, this is disabled.
    public void testExportPermId()
            throws Exception
    {
        String payload = "[\"/PUBLICATIONS/PUBLIC_REPOSITORIES/PUB29\"";
        String mimeType = "application/ld+json";
        testExport(mimeType, payload, x -> testStatus(x, "COMPLETED"),
                x -> testStatus(x, "COMPLETED"), IntegrationRoCrateServerTest::checkDownload);

    }

    @Test(timeOut = TIMEOUT)
    public void testExportEmptyResults()
            throws Exception
    {

        testExport("application/ld+json", "[\"DOES-NOT-EXIST\"]", x -> testStatus(x, "FAILED"),
                x -> testStatus(x, "COMPLETED"), x -> {
                });
    }

    @Test(dataProvider = "acceptableMimeTypes")
    public void testAcceptableExportMimeTypes(String acceptableExportMimeType,
            int expectedStatusCode)
            throws Exception
    {
        String payload = "[\"/PUBLICATIONS/PUBLIC_REPOSITORIES/PUB29\"]";

        OpenBIS openBIS = environment.createOpenBIS(TIMEOUT);
        openBIS.login(username, password);

        String export_type = acceptableExportMimeType;
        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(
                TestInstanceHostUtils.getRoCrateUrl() + "/openbis/open-api/ro-crate/export");
        request.method(HttpMethod.POST);
        request.headers(headers -> {
            headers.add("api-key", openBIS.getSessionToken());
            headers.add("Content-Type", "application/json");
            headers.add("Export", export_type);
        });
        request.body(new BytesRequestContent(payload.getBytes()));
        request.idleTimeout(TIMEOUT, TimeUnit.MILLISECONDS);

        ContentResponse response = request.send();
        assertEquals(response.getStatus(), expectedStatusCode);
    }

    @Test
    public void testMissingExportMimeType()
            throws Exception
    {
        String payload = "[\"/PUBLICATIONS/PUBLIC_REPOSITORIES/PUB29\"]";

        OpenBIS openBIS = environment.createOpenBIS(TIMEOUT);
        openBIS.login(username, password);

        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(
                TestInstanceHostUtils.getRoCrateUrl() + "/openbis/open-api/ro-crate/export");
        request.method(HttpMethod.POST);
        request.headers(headers -> {
            headers.add("api-key", openBIS.getSessionToken());
            headers.add("Content-Type", "application/json");
        });
        request.body(new BytesRequestContent(payload.getBytes()));
        request.idleTimeout(TIMEOUT, TimeUnit.MILLISECONDS);

        ContentResponse response = request.send();
        assertEquals(response.getStatus(), 400);
        String contentAsString = response.getContentAsString();
        assertTrue(contentAsString.contains(
                "The Export header is not in the range of supported options. Please use one of"));
        assertTrue(contentAsString.contains("application/ld+json"));
        assertTrue(contentAsString.contains("application/zip"));

    }




    // https://github.com/paulscherrerinstitute/rocrate-api/issues/40
    @Test
    public void testValidateMalformedCrate()
            throws Exception
    {
        OpenBIS openBIS = environment.createOpenBIS();
        openBIS.login(username, password);

        Path file = Path.of("sourceTest/resource/" + getClass().getSimpleName() + "/Malformed.json");

        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(TestInstanceHostUtils.getRoCrateUrl() + "/openbis/open-api/ro-crate/validate");
        request.method(HttpMethod.POST);
        request.headers(headers -> {
            headers.add("api-key", openBIS.getSessionToken());
            headers.add("Content-Type", "application/ld+json");
            headers.add("Accept", "application/json");
        });
        request.body(new BytesRequestContent(Files.readAllBytes(file)));

        ContentResponse response = request.send();
        System.out.println(response.getStatus());
        System.out.println(response.getMediaType());
        System.out.println(response.getContentAsString());

        assertEquals(response.getStatus(), 400);
        assertEquals(response.getMediaType(), "application/json");
        LinkedHashMap<String, Object> res =
                objectMapper.readValue(response.getContentAsString(), LinkedHashMap.class);
        objectMapper.readValue(response.getContentAsString(), LinkedHashMap.class);
        assertTrue(res.containsKey("message"));

    }

    @Test
    public void testImportMalformedCrate()
            throws Exception
    {
        OpenBIS openBIS = environment.createOpenBIS();
        openBIS.login(username, password);

        Path file =
                Path.of("sourceTest/resource/" + getClass().getSimpleName() + "/Malformed.json");

        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(
                TestInstanceHostUtils.getRoCrateUrl() + "/openbis/open-api/ro-crate/import");
        request.method(HttpMethod.POST);
        request.headers(headers -> {
            headers.add("api-key", openBIS.getSessionToken());
            headers.add("Content-Type", "application/ld+json");
            headers.add("Accept", "application/json");
        });
        request.body(new BytesRequestContent(Files.readAllBytes(file)));

        ContentResponse response = request.send();
        System.out.println(response.getStatus());
        System.out.println(response.getMediaType());
        System.out.println(response.getContentAsString());

        assertEquals(response.getStatus(), 400);
        assertEquals(response.getMediaType(), "application/json");
        LinkedHashMap<String, Object> res =
                objectMapper.readValue(response.getContentAsString(), LinkedHashMap.class);
        assertTrue(res.containsKey("message"));
    }

    // https://github.com/paulscherrerinstitute/rocrate-api/issues/39
    @Test
    public void testValidateMalformedCrateZipped()
            throws Exception
    {
        OpenBIS openBIS = environment.createOpenBIS();
        openBIS.login(username, password);

        Path file = Path.of("sourceTest/resource/" + getClass().getSimpleName() + "/Malformed.zip");

        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(TestInstanceHostUtils.getRoCrateUrl() + "/openbis/open-api/ro-crate/validate");
        request.method(HttpMethod.POST);
        request.headers(headers -> {
            headers.add("api-key", openBIS.getSessionToken());
            headers.add("Content-Type", "application/zip");
            headers.add("Accept", "application/json");
        });
        request.body(new BytesRequestContent(Files.readAllBytes(file)));

        ContentResponse response = request.send();
        assertEquals(response.getStatus(), 400);
    }

    // https://github.com/paulscherrerinstitute/rocrate-api/issues/35
    @Test(enabled = true) // This uses MissingManifest.zip file which does not exist
    public void testValidateMalformedCrateZippedMissingManifest()
            throws Exception
    {
        OpenBIS openBIS = environment.createOpenBIS();
        openBIS.login(username, password);

        Path file = Path.of("sourceTest/resource/" + getClass().getSimpleName() + "/MissingManifest.zip");

        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(TestInstanceHostUtils.getRoCrateUrl() + "/openbis/open-api/ro-crate/validate");
        request.method(HttpMethod.POST);
        request.headers(headers -> {
            headers.add("api-key", openBIS.getSessionToken());
            headers.add("Content-Type", "application/zip");
            headers.add("Accept", "application/json");
        });
        request.body(new BytesRequestContent(Files.readAllBytes(file)));

        ContentResponse response = request.send();
        assertEquals(response.getStatus(), 400);
    }

    // https://github.com/paulscherrerinstitute/rocrate-api/issues/41

    @Test
    public void testEmptyPayloadZip()
            throws Exception
    {
        OpenBIS openBIS = environment.createOpenBIS();
        openBIS.login(username, password);

        Path file = Path.of("sourceTest/resource/" + getClass().getSimpleName() + "/empty.zip");

        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(TestInstanceHostUtils.getRoCrateUrl() + "/openbis/open-api/ro-crate/validate");
        request.method(HttpMethod.POST);
        request.headers(headers -> {
            headers.add("api-key", openBIS.getSessionToken());
            headers.add("Content-Type", "application/zip");
            headers.add("Accept", "application/json");
        });
        request.body(new BytesRequestContent(Files.readAllBytes(file)));

        ContentResponse response = request.send();
        assertEquals(response.getStatus(), 400);
    }

    // https://github.com/paulscherrerinstitute/rocrate-api/issues/42

    @Test
    public void testEmptyPayload()
            throws Exception
    {
        OpenBIS openBIS = environment.createOpenBIS();
        openBIS.login(username, password);

        Path file = Path.of("sourceTest/resource/" + getClass().getSimpleName() + "/empty.json");

        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(TestInstanceHostUtils.getRoCrateUrl() + "/openbis/open-api/ro-crate/validate");
        request.method(HttpMethod.POST);
        request.headers(headers -> {
            headers.add("api-key", openBIS.getSessionToken());
            headers.add("Content-Type", "application/ld+json");
            headers.add("Accept", "application/json");
        });
        request.body(new BytesRequestContent(Files.readAllBytes(file)));

        ContentResponse response = request.send();
        assertEquals(response.getStatus(), 400);
    }

    // https://github.com/paulscherrerinstitute/rocrate-api/issues/54

    @Test
    public void testInvalidAcceptHeader()
            throws Exception
    {
        OpenBIS openBIS = environment.createOpenBIS();
        openBIS.login(username, password);

        Path file = Path.of("sourceTest/resource/" + getClass().getSimpleName() + "/OkayExample.json");

        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(TestInstanceHostUtils.getRoCrateUrl() + "/openbis/open-api/ro-crate/validate");
        request.method(HttpMethod.POST);
        request.headers(headers -> {
            headers.add("api-key", openBIS.getSessionToken());
            headers.add("Content-Type", "application/ld+json");
            headers.add("Accept", "application/xml");
        });
        request.body(new BytesRequestContent(Files.readAllBytes(file)));

        ContentResponse response = request.send();
        assertEquals(response.getStatus(), 406);
    }

    @Test
    public void testInvalidContentType()
            throws Exception
    {
        OpenBIS openBIS = environment.createOpenBIS();
        openBIS.login(username, password);

        Path file = Path.of("sourceTest/resource/" + getClass().getSimpleName() + "/OkayExample.json");

        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(TestInstanceHostUtils.getRoCrateUrl() + "/openbis/open-api/ro-crate/validate");
        request.method(HttpMethod.POST);
        request.headers(headers -> {
            headers.add("api-key", openBIS.getSessionToken());
            headers.add("Content-Type", "application/xml");
            headers.add("Accept", "application/json");
        });
        request.body(new BytesRequestContent(Files.readAllBytes(file)));

        ContentResponse response = request.send();
        assertEquals(response.getStatus(), 415);
    }

    public static void checkDownload(ExportCallerParams exportCallerParams)
    {
        Request pollRequest = exportCallerParams.client.newRequest(
                TestInstanceHostUtils.getRoCrateUrl() + "/openbis/open-api/ro-crate/status");
        pollRequest.method(HttpMethod.GET);
        pollRequest.headers(headers -> {
            headers.add("api-key", exportCallerParams.apiKey);
            headers.add("jobId", exportCallerParams.jobId);
        });

    }

    record ExportCallerParams(String apiKey, String jobId, String mimeType, HttpClient client)
    {
    }

    private static void testExport(String exportMimeType, String identifiersJsonString,
            Predicate<ContentResponse> successCheck, Predicate<ContentResponse> failCheck,
            Consumer<ExportCallerParams> afterCompletionCheck)
            throws IOException, InterruptedException, ExecutionException, TimeoutException
    {
        OpenBIS openBIS = environment.createOpenBIS(TIMEOUT);
        openBIS.login(username, password);

        String export_type = exportMimeType;
        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(
                TestInstanceHostUtils.getRoCrateUrl() + "/openbis/open-api/ro-crate/export");
        request.method(HttpMethod.POST);
        request.headers(headers -> {
            headers.add("api-key", openBIS.getSessionToken());
            headers.add("Content-Type", "application/json");
            headers.add("Export", export_type);
            headers.add("openbis.with-levels-above", "true");
            headers.add("openbis.import-compatible", "true");
        });
        request.body(new BytesRequestContent(identifiersJsonString.getBytes()));
        request.idleTimeout(TIMEOUT, TimeUnit.MILLISECONDS);

        ContentResponse response = request.send();
        LinkedHashMap asyncJob =
                objectMapper.readValue(response.getContentAsString(), LinkedHashMap.class);
        String jobId = asyncJob.get("jobId").toString();

        assertEquals(response.getStatus(), 202);

        boolean done = false;
        while (!done)
        {
            Request pollRequest = client.newRequest(
                    TestInstanceHostUtils.getRoCrateUrl() + "/openbis/open-api/ro-crate/status");
            pollRequest.method(HttpMethod.GET);
            pollRequest.headers(headers -> {
                headers.add("api-key", openBIS.getSessionToken());
                headers.add("jobId", jobId);
            });
            pollRequest.idleTimeout(TIMEOUT, TimeUnit.MILLISECONDS);
            ContentResponse pollResponse = pollRequest.send();
            if (successCheck.test(pollResponse))
            {
                done = true;
                continue;
            }


            LinkedHashMap asyncResult =
                    objectMapper.readValue(pollResponse.getContentAsString(), LinkedHashMap.class);

            if (failCheck.test(pollResponse))
            {
                List<String> errors = (List<String>) asyncResult.get("errors");
                Assert.fail(errors.stream().collect(Collectors.joining("")));
            }

            Thread.sleep(2000);
        }

        afterCompletionCheck.accept(
                new ExportCallerParams(openBIS.getSessionToken(), jobId, exportMimeType, client));

    }

    private void testValidateAstract(String fileName, String mimeType,
            Consumer<LinkedHashMap<String, Object>> validationStuff)
            throws IOException, InterruptedException, ExecutionException, TimeoutException
    {
        String successState = "COMPLETED";
        String failState = "FAILED";

        OpenBIS openBIS = environment.createOpenBIS();
        openBIS.login(username, password);

        Path file = Path.of("sourceTest/resource/" + getClass().getSimpleName() + "/" + fileName);

        HttpClient client = JettyHttpClientFactory.getHttpClient();
        Request request = client.newRequest(
                TestInstanceHostUtils.getRoCrateUrl() + "/openbis/open-api/ro-crate/validate");
        request.method(HttpMethod.POST);
        request.headers(headers -> {
            headers.add("api-key", openBIS.getSessionToken());
            headers.add("Content-Type", mimeType);
            headers.add("Accept", "application/json");
        });

        request.body(new BytesRequestContent(Files.readAllBytes(file)));

        ContentResponse response = request.send();
        LinkedHashMap asyncJob =
                objectMapper.readValue(response.getContentAsString(), LinkedHashMap.class);
        String jobId = asyncJob.get("jobId").toString();

        assertEquals(response.getStatus(), 202);

        boolean done = false;
        while (!done)
        {
            Request pollRequest = client.newRequest(
                    TestInstanceHostUtils.getRoCrateUrl() + "/openbis/open-api/ro-crate/status");
            pollRequest.method(HttpMethod.GET);
            pollRequest.headers(headers -> {
                headers.add("api-key", openBIS.getSessionToken());
                headers.add("jobId", jobId);
            });
            ContentResponse pollResponse = pollRequest.send();
            LinkedHashMap<String, Object> asyncResult =
                    objectMapper.readValue(pollResponse.getContentAsString(), LinkedHashMap.class);

            if (asyncResult.get("status").equals(successState))
            {
                done = true;
                validationStuff.accept(asyncResult);
            }

            if (asyncResult.get("status").equals(failState))
            {
                List<String> errors = (List<String>) asyncResult.get("errors");
                Assert.fail(errors.stream().collect(Collectors.joining("")));
            }
            Thread.sleep(2000);

        }
    }

    @DataProvider(name = "acceptableMimeTypes")
    public static Object[][] acceptableMimeTypeProvider()
    {
        return new Object[][] { { "application/ld+json", 202 }, { "application/zip", 202 },
                { "application/xml", 400 }, { "metlapiltetzotzontzin/hmeephmeep", 400 } };

    }

}
