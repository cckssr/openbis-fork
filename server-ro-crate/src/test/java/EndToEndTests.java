import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.ros.startup.Configuration;
import ch.ethz.sis.openbis.ros.startup.RoCrateServerParameter;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.http.HttpClientTransportOverHTTP;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class EndToEndTests extends AbstractTest
{

    @Test
    public void importRoCrateOpenAPITest()
            throws Exception
    {
        getConfiguration();

        OpenBIS openBIS = new OpenBIS("http://localhost:8888", Integer.MAX_VALUE);
        openBIS.login("system",
                "changeit");

        given()
                .header("sessionToken", openBIS.getSessionToken())
                .body(RoCrateServiceTest.roCrateMetadataJsonExample1.getBytes())
                .when().post("/openbis/open-api/ro-crate/import")
                .then()
                .statusCode(200)
                .body(is("hello"));

        Configuration configuration = getConfiguration();

        String apiMethod = "import";
        int port = getConfiguration().getIntegerProperty(RoCrateServerParameter.httpServerPort);
        SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
        sslContextFactory.setTrustAll(true);
        ClientConnector clientConnector = new ClientConnector();
        clientConnector.setSslContextFactory(sslContextFactory);
        HttpClient httpClient = new HttpClient(new HttpClientTransportOverHTTP(clientConnector));
        httpClient.start();

        Request request =
                httpClient.newRequest(
                                "http://localhost:" + port + "/openbis/open-api/ro-crate/" + apiMethod)
                        .headers(httpFields -> httpFields.add("sessionToken",
                                openBIS.getSessionToken()));
        request.method(HttpMethod.POST);
        request.content(
                new BytesContentProvider(RoCrateServiceTest.roCrateMetadataJsonExample1.getBytes()),
                "application/ld+json");
        ContentResponse response = request.send();
        System.out.println(response);
        System.out.println(response.getContentAsString());
    }
}
