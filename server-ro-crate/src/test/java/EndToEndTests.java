import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.rocrateserver.openapi.v1.service.RoCrateService;
import ch.ethz.sis.rocrateserver.openapi.v1.service.params.ExportParams;
import ch.ethz.sis.rocrateserver.openapi.v1.service.response.AsyncJob;
import ch.ethz.sis.rocrateserver.openapi.v1.service.response.result.AsyncResult;
import ch.ethz.sis.rocrateserver.startup.StartupMain;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.Assert;
import org.junit.Ignore;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

// These tests were used during development.
// The important parts were moved into ch.ethz.sis.openbis.systemtests.suite.rocrate.IntegrationRoCrateServerTest
// These tests can be used to test things locally and use the debugger inside the application and ro crate server.
// There is no guarantee that these tests are maintained.
public class EndToEndTests extends AbstractTest
{

    public static final String HEADER_API_KEY = "api-key";

    private static String username = "system";
    private static String password = "changeit";

    @BeforeClass
    public void startQuarkus() throws IOException {
        StartupMain.main(new String[] { "src/main/resources/service.properties" });
    }

    @Test
    public void testTestEcho()
            throws Exception
    {
        getConfiguration();

        given()
                .param("message", "Hello World")
                .when().get("http://localhost:8086/openbis/open-api/ro-crate/test-echo")
                .then()
                .body(is("Hello World"))
                .statusCode(200);
    }

    @Test(enabled = false)
    public void testTestOpenbisConnection()
            throws Exception
    {
        getConfiguration();

        OpenBIS openBIS = new OpenBIS("http://localhost:8888", Integer.MAX_VALUE);
        openBIS.login(username, password);


        given()
                .param(HEADER_API_KEY, openBIS.getSessionToken())
                .when().get("http://localhost:8086/openbis/open-api/ro-crate/test-openbis-connection")
                .then()
                .body(is(username))
                .statusCode(200);
    }

    @Test(enabled = false)
    public void testImport()
            throws Exception
    {
        getConfiguration();

        OpenBIS openBIS = new OpenBIS("http://localhost:8888", Integer.MAX_VALUE);
        openBIS.login(username, password);

        ClassLoader classLoader = getClass().getClassLoader();
        String resourceName = "endtoend/OkayExample.json";
        File file = new File(classLoader.getResource(resourceName).getFile());

        io.restassured.response.Response response = given()
                .header(HEADER_API_KEY, openBIS.getSessionToken())
                .header("Content-Type", RoCrateService.APPLICATION_LD_JSON)
                .body(Files.readAllBytes(Path.of(file.getPath())))
                .when().post("http://localhost:8086/openbis/open-api/ro-crate/import")
                .then()
                .statusCode(Response.Status.ACCEPTED.getStatusCode())
                .extract()
                .response();

        String bodyString = response.getBody().asString();
        ObjectMapper objectMapper = new ObjectMapper();
        AsyncJob asyncJob = objectMapper.readValue(bodyString, AsyncJob.class);

        String jobId = asyncJob.getJobId();
        boolean done = false;
        while (!done)
        {
            io.restassured.response.Response payLoadResponse =
                    given().header(HEADER_API_KEY, openBIS.getSessionToken())
                            .header("jobID", jobId)
                            .when()
                            .get("http://localhost:8086/openbis/open-api/ro-crate/status")
                            .then()
                            .extract()
                            .response();
            String resultString = payLoadResponse.getBody().asString();
            AsyncResult asyncResult = objectMapper.readValue(resultString, AsyncResult.class);

            if (asyncResult.getStatus().equals("COMPLETED"))
            {
                done = true;
            }
            if (asyncResult.getStatus().equals("FAILED"))
            {

                Assert.fail(String.join(",", asyncResult.getErrors()));
            }

            TimeUnit.SECONDS.sleep(3);
        }

        System.out.println("lol");

    }

    @Test(enabled = false)
    public void testImportZip()
            throws Exception
    {
        getConfiguration();

        OpenBIS openBIS = new OpenBIS("http://localhost:8888", Integer.MAX_VALUE);
        openBIS.login(username, password);

        ClassLoader classLoader = getClass().getClassLoader();
        String resourceName = "endtoend/OkayExample.json";
        File file = new File(classLoader.getResource(resourceName).getFile());

        io.restassured.response.Response response = given()
                .header(HEADER_API_KEY, openBIS.getSessionToken())
                .header("Content-Type", RoCrateService.APPLICATION_LD_JSON)
                .body(Files.readAllBytes(Path.of(file.getPath())))
                .when().post("http://localhost:8086/openbis/open-api/ro-crate/validate")
                .then()
                .statusCode(Response.Status.ACCEPTED.getStatusCode())
                .extract()
                .response();

        String bodyString = response.getBody().asString();
        ObjectMapper objectMapper = new ObjectMapper();
        AsyncJob asyncJob = objectMapper.readValue(bodyString, AsyncJob.class);

        String jobId = asyncJob.getJobId();
        boolean done = false;
        while (!done)
        {
            io.restassured.response.Response payLoadResponse =
                    given().header(HEADER_API_KEY, openBIS.getSessionToken())
                            .header("jobID", jobId)
                            .when()
                            .get("http://localhost:8086/openbis/open-api/ro-crate/status")
                            .then()
                            .extract()
                            .response();
            String resultString = payLoadResponse.getBody().asString();
            AsyncResult asyncResult = objectMapper.readValue(resultString, AsyncResult.class);

            if (asyncResult.getStatus().equals("COMPLETED"))
            {
                done = true;
            }
            if (asyncResult.getStatus().equals("FAILED"))
            {

                Assert.fail(String.join(",", asyncResult.getErrors()));
            }

            TimeUnit.SECONDS.sleep(3);
        }

        System.out.println("lol");
    }

    @Test(enabled = false)
    public void testValidate()
            throws Exception
    {
        getConfiguration();

        OpenBIS openBIS = new OpenBIS("http://localhost:8888", Integer.MAX_VALUE);
        openBIS.login(username, password);

        ClassLoader classLoader = getClass().getClassLoader();
        String resourceName = "endtoend/OkayExample.json";
        File file = new File(classLoader.getResource(resourceName).getFile());

        io.restassured.response.Response response = given()
                .header(HEADER_API_KEY, openBIS.getSessionToken())
                .header("Content-Type", RoCrateService.APPLICATION_LD_JSON)
                .body(Files.readAllBytes(Path.of(file.getPath())))
                .when().post("http://localhost:8086/openbis/open-api/ro-crate/validate")
                .then()
                .statusCode(Response.Status.ACCEPTED.getStatusCode())
                .extract()
                .response();

        String bodyString = response.getBody().asString();
        ObjectMapper objectMapper = new ObjectMapper();
        AsyncJob asyncJob = objectMapper.readValue(bodyString, AsyncJob.class);

        String jobId = asyncJob.getJobId();
        boolean done = false;
        while (!done)
        {
            io.restassured.response.Response payLoadResponse =
                    given().header(HEADER_API_KEY, openBIS.getSessionToken())
                            .header("jobID", jobId)
                            .when()
                            .get("http://localhost:8086/openbis/open-api/ro-crate/status")
                            .then()
                            .extract()
                            .response();
            String resultString = payLoadResponse.getBody().asString();
            AsyncResult asyncResult = objectMapper.readValue(resultString, AsyncResult.class);

            if (asyncResult.getStatus().equals("COMPLETED"))
            {
                done = true;
                Assert.assertTrue(asyncResult.getValidationResult().isOkay());

            }
            if (asyncResult.getStatus().equals("FAILED"))
            {
                Assert.fail(asyncResult.getErrors().stream().collect(Collectors.joining("\n")));
            }
            TimeUnit.SECONDS.sleep(3);
        }

    }

    @Test(enabled = false)
    public void testValidateZip()
            throws Exception
    {
        getConfiguration();

        OpenBIS openBIS = new OpenBIS("http://localhost:8888", Integer.MAX_VALUE);
        openBIS.login(username, password);

        ClassLoader classLoader = getClass().getClassLoader();
        String resourceName = "endtoend/OkayExample.zip";
        File file = new File(classLoader.getResource(resourceName).getFile());

        io.restassured.response.Response response = given()
                .header(HEADER_API_KEY, openBIS.getSessionToken())
                .header("Content-Type", RoCrateService.APPLICATION_ZIP)
                .body(Files.readAllBytes(Path.of(file.getPath())))
                .when().post("http://localhost:8086/openbis/open-api/ro-crate/validate")
                .then()
                .statusCode(Response.Status.ACCEPTED.getStatusCode())
                .extract()
                .response();

        String bodyString = response.getBody().asString();
        ObjectMapper objectMapper = new ObjectMapper();
        AsyncJob asyncJob = objectMapper.readValue(bodyString, AsyncJob.class);

        String jobId = asyncJob.getJobId();
        boolean done = false;
        while (!done)
        {
            io.restassured.response.Response payLoadResponse =
                    given().header(HEADER_API_KEY, openBIS.getSessionToken())
                            .header("jobID", jobId)
                            .when()
                            .get("http://localhost:8086/openbis/open-api/ro-crate/status")
                            .then()
                            .extract()
                            .response();
            String resultString = payLoadResponse.getBody().asString();
            AsyncResult asyncResult = objectMapper.readValue(resultString, AsyncResult.class);

            if (asyncResult.getStatus().equals("COMPLETED"))
            {
                done = true;
                Assert.assertTrue(asyncResult.getValidationResult().isOkay());

            }
            TimeUnit.SECONDS.sleep(3);
        }

    }

    @Test(enabled = false)
    public void testValidateUnknown()
            throws Exception
    {
        getConfiguration();

        OpenBIS openBIS = new OpenBIS("http://localhost:8888", Integer.MAX_VALUE);
        openBIS.login(username, password);

        ClassLoader classLoader = getClass().getClassLoader();
        String resourceName = "endtoend/UnknownProperty.json";
        File file = new File(classLoader.getResource(resourceName).getFile());

        io.restassured.response.Response response = given()
                .header(HEADER_API_KEY, openBIS.getSessionToken())
                .header("Content-Type", RoCrateService.APPLICATION_LD_JSON)
                .body(Files.readAllBytes(Path.of(file.getPath())))
                .when().post("http://localhost:8086/openbis/open-api/ro-crate/validate")
                .then()
                .statusCode(Response.Status.ACCEPTED.getStatusCode())
                .extract()
                .response();

        String bodyString = response.getBody().asString();
        ObjectMapper objectMapper = new ObjectMapper();
        AsyncJob asyncJob = objectMapper.readValue(bodyString, AsyncJob.class);

        String jobId = asyncJob.getJobId();
        boolean done = false;
        while (!done)
        {
            io.restassured.response.Response payLoadResponse =
                    given().header(HEADER_API_KEY, openBIS.getSessionToken())
                            .header("jobID", jobId)
                            .when()
                            .get("http://localhost:8086/openbis/open-api/ro-crate/status")
                            .then()
                            .extract()
                            .response();
            String resultString = payLoadResponse.getBody().asString();
            AsyncResult asyncResult = objectMapper.readValue(resultString, AsyncResult.class);

            if (asyncResult.getStatus().equals("COMPLETED"))
            {
                done = true;
                Assert.assertFalse(asyncResult.getValidationResult().isOkay());
                Assert.assertEquals(2,
                        asyncResult.getValidationResult().getEntititesToUndefinedProperties()
                                .size());

            }
            TimeUnit.SECONDS.sleep(3);
        }
    }

    @Test(enabled = false)
    public void testValidateWrong()
            throws Exception
    {
        getConfiguration();

        OpenBIS openBIS = new OpenBIS("http://localhost:8888", Integer.MAX_VALUE);
        openBIS.login(username, password);

        ClassLoader classLoader = getClass().getClassLoader();
        String resourceName = "endtoend/WrongDataType.json";
        File file = new File(classLoader.getResource(resourceName).getFile());

        io.restassured.response.Response response = given()
                .header(HEADER_API_KEY, openBIS.getSessionToken())
                .header("Content-Type", RoCrateService.APPLICATION_LD_JSON)
                .body(Files.readAllBytes(Path.of(file.getPath())))
                .when().post("http://localhost:8086/openbis/open-api/ro-crate/validate")
                .then()
                .statusCode(Response.Status.ACCEPTED.getStatusCode())
                .extract()
                .response();

        String bodyString = response.getBody().asString();
        ObjectMapper objectMapper = new ObjectMapper();
        AsyncJob asyncJob = objectMapper.readValue(bodyString, AsyncJob.class);

        String jobId = asyncJob.getJobId();
        boolean done = false;
        while (!done)
        {
            io.restassured.response.Response payLoadResponse =
                    given().header(HEADER_API_KEY, openBIS.getSessionToken())
                            .header("jobID", jobId)
                            .when()
                            .get("http://localhost:8086/openbis/open-api/ro-crate/status")
                            .then()
                            .extract()
                            .response();
            String resultString = payLoadResponse.getBody().asString();
            AsyncResult asyncResult = objectMapper.readValue(resultString, AsyncResult.class);

            if (asyncResult.getStatus().equals("COMPLETED"))
            {
                done = true;
                Assert.assertFalse(asyncResult.getValidationResult().isOkay());
                Assert.assertEquals(1,
                        asyncResult.getValidationResult().getWrongDataTypes().size());
                Assert.assertTrue(asyncResult.getValidationResult().getWrongDataTypes()
                        .containsKey("SCHEMA_CREATIVEWORK_SCICAT_PUBLISHEDDATA"));
                Assert.assertTrue(asyncResult.getValidationResult().getWrongDataTypes()
                        .get("SCHEMA_CREATIVEWORK_SCICAT_PUBLISHEDDATA").stream()
                        .anyMatch(x -> x.getProperty().contains("NUMBEROFFILES")));

            }
            TimeUnit.SECONDS.sleep(3);
        }
    }

    @Test(enabled = false)
    public void testExportDOI()
            throws Exception
    {
        testExport(RoCrateService.APPLICATION_LD_JSON,
                "[\"https://doi.org/10.1038/s41586-020-3010-5\"]");

    }

    private static void testExport(String exportMimeType, String identifiersJsonString)
            throws JsonProcessingException, InterruptedException
    {
        getConfiguration();

        OpenBIS openBIS = new OpenBIS("http://localhost:8888", Integer.MAX_VALUE);
        openBIS.login(username, password);

        String export_type = exportMimeType;
        io.restassured.response.Response response = given()
                .header(HEADER_API_KEY, openBIS.getSessionToken())
                .header("openbis.with-levels-below", "true")
                .header("Content-Type", "application/json")
                .header(ExportParams.EXPORT_MIME_TYPE_HEADER, export_type)
                .body(identifiersJsonString)
                .when().post("http://localhost:8086/openbis/open-api/ro-crate/export")
                .then()
                .header("Content-Type", "application/json")
                .statusCode(Response.Status.ACCEPTED.getStatusCode())
                .extract()
                .response();

        String bodyString = response.getBody().asString();
        ObjectMapper objectMapper = new ObjectMapper();
        AsyncJob asyncJob = objectMapper.readValue(bodyString, AsyncJob.class);

        String jobId = asyncJob.getJobId();
        boolean done = false;
        while (!done)
        {
            io.restassured.response.Response payLoadResponse =
                    given().header(HEADER_API_KEY, openBIS.getSessionToken())
                            .header("jobID", jobId)
                            .when()
                            .get("http://localhost:8086/openbis/open-api/ro-crate/status")
                            .then()
                            .extract()
                            .response();

            if (payLoadResponse.getContentType().equals(export_type))
            {
                done = true;
                return;
            }

            String resultString = payLoadResponse.getBody().asString();
            try
            {
                AsyncResult asyncResult = objectMapper.readValue(resultString, AsyncResult.class);

            } catch (Exception e)
            {
                System.out.println("lol");
            }
            AsyncResult asyncResult = objectMapper.readValue(resultString, AsyncResult.class);

            if (asyncResult.getStatus().equals("COMPLETED"))
            {
                done = true;
            }
            TimeUnit.SECONDS.sleep(3);
        }
    }

    @Test(enabled = false)
    public void testExportDOIZip()
            throws Exception
    {
        testExport(RoCrateService.APPLICATION_ZIP,
                "[\"https://doi.org/10.1038/s41586-020-3010-5\"]");

    }

    @Test(enabled = false)
    public void testExportIdentifier()
            throws Exception
    {
        testExport(RoCrateService.APPLICATION_LD_JSON,
                "[\"/PUBLICATIONS/PUBLIC_REPOSITORIES/PUB29\"]");

    }

    @Test(enabled = false)
    @Ignore
    // PermIds depend on when the import was done. This can lead to false failure.
    // As long as we don't have a good solution for search in tests, this is disabled.
    public void testExportPermId()
            throws Exception
    {
        // testExport(RoCrateService.APPLICATION_LD_JSON, "[\"20250728111931402-94\"]");

    }

    @Test(enabled = false)
    public void testExportEmptyResults()
            throws Exception
    {
        getConfiguration();

        OpenBIS openBIS = new OpenBIS("http://localhost:8888", Integer.MAX_VALUE);
        openBIS.login(username, password);

        io.restassured.response.Response response = given()
                .header(HEADER_API_KEY, openBIS.getSessionToken())
                .header("openbis.with-levels-below", "true")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header(ExportParams.EXPORT_MIME_TYPE_HEADER, RoCrateService.APPLICATION_LD_JSON)
                .body("[\"NOT-AN-IDENTIFIER\"]")
                .when().post("http://localhost:8086/openbis/open-api/ro-crate/export")
                .then()
                .header("Content-Type", "application/json")
                .statusCode(Response.Status.ACCEPTED.getStatusCode())
                .extract()
                .response();

        String bodyString = response.getBody().asString();
        ObjectMapper objectMapper = new ObjectMapper();
        AsyncJob asyncJob = objectMapper.readValue(bodyString, AsyncJob.class);

        String jobId = asyncJob.getJobId();
        boolean done = false;
        while (!done)
        {
            io.restassured.response.Response payLoadResponse =
                    given().header(HEADER_API_KEY, openBIS.getSessionToken())
                            .header("jobID", jobId)
                            .when()
                            .get("http://localhost:8086/openbis/open-api/ro-crate/status")
                            .then()
                            .extract()
                            .response();

            if (payLoadResponse.getContentType().equals(RoCrateService.APPLICATION_LD_JSON))
            {
                done = true;
                return;
            }

            String resultString = payLoadResponse.getBody().asString();
            AsyncResult asyncResult = objectMapper.readValue(resultString, AsyncResult.class);

            if (asyncResult.getStatus().equals("FAILED"))
            {
                done = true;
                asyncResult.getErrors().stream().anyMatch(x -> x.contains("No results found"));
            }
            TimeUnit.SECONDS.sleep(3);
        }

    }

    // https://github.com/paulscherrerinstitute/rocrate-api/issues/40
    @Test(enabled = false)
    public void testValidateMalformedCrate()
            throws Exception
    {

        getConfiguration();

        OpenBIS openBIS = new OpenBIS("http://localhost:8888", Integer.MAX_VALUE);
        openBIS.login(username, password);

        ClassLoader classLoader = getClass().getClassLoader();
        String resourceName = "endtoend/Malformed.json";
        File file = new File(classLoader.getResource(resourceName).getFile());

        given()
                .header(HEADER_API_KEY, openBIS.getSessionToken())
                .header("Content-Type", RoCrateService.APPLICATION_LD_JSON)
                .header("Accept", "application/json")
                .body(Files.readAllBytes(Path.of(file.getPath())))
                .when().post("http://localhost:8086/openbis/open-api/ro-crate/validate")
                .then()
                .statusCode(400);
    }

    // https://github.com/paulscherrerinstitute/rocrate-api/issues/39
    @Test(enabled = false)
    public void testValidateMalformedCrateZipped()
            throws Exception
    {

        getConfiguration();

        OpenBIS openBIS = new OpenBIS("http://localhost:8888", Integer.MAX_VALUE);
        openBIS.login(username, password);

        ClassLoader classLoader = getClass().getClassLoader();
        String resourceName = "endtoend/Malformed.zip";
        File file = new File(classLoader.getResource(resourceName).getFile());

        given()
                .header(HEADER_API_KEY, openBIS.getSessionToken())
                .header("Content-Type", "application/zip")
                .header("Accept", "application/json")
                .body(Files.readAllBytes(Path.of(file.getPath())))
                .when().post("http://localhost:8086/openbis/open-api/ro-crate/validate")
                .then()
                .statusCode(400);
    }

    // https://github.com/paulscherrerinstitute/rocrate-api/issues/35
    @Test(enabled = false)
    public void testValidateMalformedCrateZippedMissingManifest()
            throws Exception
    {

        getConfiguration();

        OpenBIS openBIS = new OpenBIS("http://localhost:8888", Integer.MAX_VALUE);
        openBIS.login(username, password);

        ClassLoader classLoader = getClass().getClassLoader();
        String resourceName = "endtoend/MissingManifest.zip";
        File file = new File(classLoader.getResource(resourceName).getFile());

        given()
                .header(HEADER_API_KEY, openBIS.getSessionToken())
                .header("Content-Type", "application/zip")
                .header("Accept", "application/json")
                .body(Files.readAllBytes(Path.of(file.getPath())))
                .when().post("http://localhost:8086/openbis/open-api/ro-crate/validate")
                .then()
                .statusCode(400);
    }

    // https://github.com/paulscherrerinstitute/rocrate-api/issues/41

    @Test(enabled = false)
    public void testEmptyPayloadZip()
            throws Exception
    {

        getConfiguration();

        OpenBIS openBIS = new OpenBIS("http://localhost:8888", Integer.MAX_VALUE);
        openBIS.login(username, password);

        ClassLoader classLoader = getClass().getClassLoader();
        String resourceName = "endtoend/empty.zip";
        File file = new File(classLoader.getResource(resourceName).getFile());

        given()
                .header(HEADER_API_KEY, openBIS.getSessionToken())
                .header("Content-Type", "application/zip")
                .header("Accept", "application/json")
                .body(Files.readAllBytes(Path.of(file.getPath())))
                .when().post("http://localhost:8086/openbis/open-api/ro-crate/validate")
                .then()
                .statusCode(400);
    }

    // https://github.com/paulscherrerinstitute/rocrate-api/issues/42

    @Test(enabled = false)
    public void testEmptyPayload()
            throws Exception
    {

        getConfiguration();

        OpenBIS openBIS = new OpenBIS("http://localhost:8888", Integer.MAX_VALUE);
        openBIS.login(username, password);

        ClassLoader classLoader = getClass().getClassLoader();
        String resourceName = "endtoend/empty.json";
        File file = new File(classLoader.getResource(resourceName).getFile());

        given()
                .header(HEADER_API_KEY, openBIS.getSessionToken())
                .header("Content-Type", RoCrateService.APPLICATION_LD_JSON)
                .header("Accept", "application/json")
                .body(Files.readAllBytes(Path.of(file.getPath())))
                .when().post("http://localhost:8086/openbis/open-api/ro-crate/validate")
                .then()
                .statusCode(400);
    }

    // https://github.com/paulscherrerinstitute/rocrate-api/issues/54

    @Test(enabled = false)
    public void testInvalidAcceptHeader()
            throws Exception
    {
        getConfiguration();

        OpenBIS openBIS = new OpenBIS("http://localhost:8888", Integer.MAX_VALUE);
        openBIS.login(username, password);

        ClassLoader classLoader = getClass().getClassLoader();
        String resourceName = "endtoend/OkayExample.json";
        File file = new File(classLoader.getResource(resourceName).getFile());

        String expected = "{\"isValid\":true}";
        given()
                .header(HEADER_API_KEY, openBIS.getSessionToken())
                .header("Content-Type", RoCrateService.APPLICATION_LD_JSON)
                .header("Accept", "application/xml")
                .body(Files.readAllBytes(Path.of(file.getPath())))
                .when().post("http://localhost:8086/openbis/open-api/ro-crate/validate")
                .then()
                .statusCode(406);
    }

    @Test(enabled = false)
    public void testInvalidContentType()
            throws Exception
    {
        getConfiguration();

        OpenBIS openBIS = new OpenBIS("http://localhost:8888", Integer.MAX_VALUE);
        openBIS.login(username, password);

        ClassLoader classLoader = getClass().getClassLoader();
        String resourceName = "endtoend/OkayExample.json";
        File file = new File(classLoader.getResource(resourceName).getFile());

        given()
                .header(HEADER_API_KEY, openBIS.getSessionToken())
                .header("Content-Type", MediaType.APPLICATION_XML_TYPE)
                .header("Accept", "application/json")
                .body(Files.readAllBytes(Path.of(file.getPath())))
                .when().post("http://localhost:8086/openbis/open-api/ro-crate/validate")
                .then()
                .statusCode(415);
    }


}
