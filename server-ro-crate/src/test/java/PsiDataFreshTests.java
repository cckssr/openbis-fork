import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.rocrateserver.startup.RoCrateServerParameter;
import ch.ethz.sis.rocrateserver.startup.StartupMain;
import jakarta.ws.rs.HttpMethod;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.http.HttpClientTransportOverHTTP;
import org.eclipse.jetty.client.util.BytesRequestContent;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;

public class PsiDataFreshTests extends AbstractTest
{

    private static String username = "system";

    private static String password = "changeit";

    private static final String DOWNLOAD_URL =
            "https://scicat-exporter.development.psi.ch/api/v1/ro-crate/export";

    private static final String IDENTIFIER_BODY = "[\n" +
            "  \"10.16907/4b55cbae-ac98-445a-a15e-1534b2a8b01f\",\n" +
            "  \"10.16907/edfe4ad5-9448-4e38-91a1-0459c8713b9b\"\n" +
            "]";

    @BeforeClass
    public void startQuarkus() throws IOException
    {
        StartupMain.main(new String[] { "src/main/resources/service.properties" });
    }

    @Test
    public void testValidatePsiCrate()
            throws Exception
    {
        getConfiguration();

        OpenBIS openBIS = new OpenBIS("http://localhost:8888", Integer.MAX_VALUE);
        InputStream psiData = fetchPsiData();
        openBIS.login(username, password);

        String expected = "{\"isValid\":true}";
        given()
                .header("api-key", openBIS.getSessionToken())
                .header("Content-Type", "application/ld+json")
                .body(psiData)
                .when().post("http://localhost:8086/openbis/open-api/ro-crate/validate")
                .then()
                .body(allOf(containsString("\"validationErrors\":[]"),
                        containsString("\"isValid\":true")))
                .statusCode(200);
    }

    @Test
    public void testImportFreshPsiCrate()
            throws Exception
    {
        getConfiguration();

        OpenBIS openBIS = new OpenBIS("http://localhost:8888", Integer.MAX_VALUE);
        InputStream psiData = fetchPsiData();
        openBIS.login(username, password);

        String expected = "{\"isValid\":true}";
        given()
                .header("api-key", openBIS.getSessionToken())
                .header("Content-Type", "application/ld+json")
                .body(psiData)
                .when().post("http://localhost:8086/openbis/open-api/ro-crate/validate")
                .then()
                .body(allOf(containsString("\"validationErrors\":[]"),
                        containsString("\"isValid\":true")))
                .statusCode(200);
    }

    InputStream fetchPsiData() throws Exception
    {
        SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
        sslContextFactory.setTrustAll(true);
        ClientConnector clientConnector = new ClientConnector();
        clientConnector.setSslContextFactory(sslContextFactory);
        clientConnector.setIdleTimeout(Duration.ofMillis(StartupMain.getConfiguration()
                .getIntegerProperty(RoCrateServerParameter.openBISTimeout)));
        HttpClient httpClient = new HttpClient(new HttpClientTransportOverHTTP(clientConnector));
        httpClient.start();

        InputStreamResponseListener listener = new InputStreamResponseListener();


        Request request =
                httpClient.newRequest(
                                DOWNLOAD_URL)
                        .headers(httpFields -> httpFields.add("Accept",
                                "application/ld+json"))
                        .body(new BytesRequestContent("application/json", IDENTIFIER_BODY.getBytes(
                                StandardCharsets.UTF_8)));
        request.method(HttpMethod.POST);
        request.send(listener);
        return listener.getInputStream();
    }

}
