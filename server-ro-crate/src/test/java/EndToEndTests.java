import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.rocrateserver.startup.StartupMain;
import org.junit.Ignore;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class EndToEndTests extends AbstractTest
{

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

    @Test
    public void testTestOpenbisConnection()
            throws Exception
    {
        getConfiguration();

        OpenBIS openBIS = new OpenBIS("http://localhost:8888", Integer.MAX_VALUE);
        openBIS.login(username, password);

        given()
                .param("api-key", openBIS.getSessionToken())
                .when().get("http://localhost:8086/openbis/open-api/ro-crate/test-openbis-connection")
                .then()
                .body(is(username))
                .statusCode(200);
    }

    @Test
    public void testImport()
            throws Exception
    {
        getConfiguration();

        OpenBIS openBIS = new OpenBIS("http://localhost:8888", Integer.MAX_VALUE);
        openBIS.login(username, password);

        ClassLoader classLoader = getClass().getClassLoader();
        String resourceName = "endtoend/OkayExample.json";
        File file = new File(classLoader.getResource(resourceName).getFile());

        given()
                .header("api-key", openBIS.getSessionToken())
                .header("Content-Type", "application/ld+json")
                .body(Files.readAllBytes(Path.of(file.getPath())))
                .when().post("http://localhost:8086/openbis/open-api/ro-crate/import")
                .then()
                .statusCode(200);
    }

    @Test
    public void testValidate()
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
                .header("api-key", openBIS.getSessionToken())
                .header("Content-Type", "application/ld+json")
                .header("Accept", "application/json")
                .body(Files.readAllBytes(Path.of(file.getPath())))
                .when().post("http://localhost:8086/openbis/open-api/ro-crate/validate")
                .then()
                .body(allOf(containsString("\"validationErrors\":[]"),
                        containsString("\"isValid\":true")))
                .statusCode(200);
    }

    @Test
    public void testValidateUnknown()
            throws Exception
    {
        getConfiguration();

        OpenBIS openBIS = new OpenBIS("http://localhost:8888", Integer.MAX_VALUE);
        openBIS.login(username, password);

        ClassLoader classLoader = getClass().getClassLoader();
        String resourceName = "endtoend/UnknownProperty.json";
        File file = new File(classLoader.getResource(resourceName).getFile());

        given()
                .header("api-key", openBIS.getSessionToken())
                .header("Content-Type", "application/ld+json")
                .header("Accept", "application/zip")
                .body(Files.readAllBytes(Path.of(file.getPath())))
                .when().post("http://localhost:8086/openbis/open-api/ro-crate/validate")
                .then()
                .body(allOf(containsString("\"validationErrors\":[{"),
                        containsString("\"isValid\":false"),
                        containsString("\"property\":\"wrong\""),
                        containsString("\"message\":\"Property not in schema\"")))
                .statusCode(200);
    }

    @Test
    public void testValidateWrong()
            throws Exception
    {
        getConfiguration();

        OpenBIS openBIS = new OpenBIS("http://localhost:8888", Integer.MAX_VALUE);
        openBIS.login(username, password);

        ClassLoader classLoader = getClass().getClassLoader();
        String resourceName = "endtoend/WrongDataType.json";
        File file = new File(classLoader.getResource(resourceName).getFile());

        String expected = "{\"isValid\":true}";
        given()
                .header("api-key", openBIS.getSessionToken())
                .header("Content-Type", "application/ld+json")
                .header("Accept", "application/json")
                .body(Files.readAllBytes(Path.of(file.getPath())))
                .when().post("http://localhost:8086/openbis/open-api/ro-crate/validate")
                .then()
                .body(allOf(containsString("\"validationErrors\":[{"),
                        containsString("\"isValid\":false"), containsString("NUMBEROFFILES")))
                .statusCode(200);
    }


    @Test
    public void testExportDOI()
            throws Exception
    {
        getConfiguration();

        OpenBIS openBIS = new OpenBIS("http://localhost:8888", Integer.MAX_VALUE);
        openBIS.login(username, password);

        given()
                .header("api-key", openBIS.getSessionToken())
                .header("openbis.with-Levels-below", "true")
                .header("Content-Type", "application/json")
                .header("Accept", "application/ld+json")
                .body("[\"https://doi.org/10.1038/s41586-020-3010-5\"]")
                .when().post("http://localhost:8086/openbis/open-api/ro-crate/export")
                .then()
                .statusCode(200);
    }

    @Test
    public void testExportIdentifier()
            throws Exception
    {
        getConfiguration();

        OpenBIS openBIS = new OpenBIS("http://localhost:8888", Integer.MAX_VALUE);
        openBIS.login(username, password);

        given()
                .header("api-key", openBIS.getSessionToken())
                .header("openbis.with-Levels-below", "true")
                .header("Content-Type", "application/json")
                .header("Accept", "application/ld+json")
                .body("[\"/PUBLICATIONS/PUBLIC_REPOSITORIES/PUB29\"]")
                .when().post("http://localhost:8086/openbis/open-api/ro-crate/export")
                .then()
                .statusCode(200);
    }

    @Test
    @Ignore
    // PermIds depend on when the import was done. This can lead to false failure.
    // As long as we don't have a good solution for search in tests, this is disabled.
    public void testExportPermId()
            throws Exception
    {
        getConfiguration();

        OpenBIS openBIS = new OpenBIS("http://localhost:8888", Integer.MAX_VALUE);
        openBIS.login(username, password);

        given()
                .header("api-key", openBIS.getSessionToken())
                .header("openbis.with-Levels-below", "true")
                .header("Content-Type", "application/json")
                .body("[\"20250728111931402-94\"]")
                .when().post("http://localhost:8086/openbis/open-api/ro-crate/export")
                .then()
                .statusCode(200);
    }

    @Test
    public void testExportEmptyResults()
            throws Exception
    {
        getConfiguration();

        OpenBIS openBIS = new OpenBIS("http://localhost:8888", Integer.MAX_VALUE);
        openBIS.login(username, password);

        given()
                .header("api-key", openBIS.getSessionToken())
                .header("openbis.with-Levels-below", "true")
                .header("Content-Type", "application/json")
                .body("[\"NOT-AN-IDENTIFIER\"]")
                .when().post("http://localhost:8086/openbis/open-api/ro-crate/export")
                .then()
                .statusCode(404);
    }
}
