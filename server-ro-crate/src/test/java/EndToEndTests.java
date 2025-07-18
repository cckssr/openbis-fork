import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.ros.startup.Configuration;
import ch.ethz.sis.openbis.ros.startup.RoCrateServerParameter;
import ch.ethz.sis.openbis.ros.startup.StartupMain;
import io.quarkus.runtime.Quarkus;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.http.HttpClientTransportOverHTTP;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;


public class EndToEndTests extends AbstractTest
{

    @BeforeEach
    public void startQuarkus() {
        Quarkus.run(StartupMain.class, new String[]{"./src/main/resources/service.properties"});
    }

    @Test
    public void importRoCrateOpenAPITest()
            throws Exception
    {
        getConfiguration();

        OpenBIS openBIS = new OpenBIS("http://localhost:8888", Integer.MAX_VALUE);
        openBIS.login("system",
                "changeit");

//        given()
//                .header("sessionToken", openBIS.getSessionToken())
//                .body(RoCrateServiceTest.roCrateMetadataJsonExample1.getBytes())
//                .when().post("http://localhost:8085/openbis/open-api/ro-crate/import")
//                .then()
//                .statusCode(200);

        Configuration configuration = getConfiguration();

        String apiMethod = "import";
        int port = configuration.getIntegerProperty(RoCrateServerParameter.httpServerPort);
        SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
        sslContextFactory.setTrustAll(true);
        ClientConnector clientConnector = new ClientConnector();
        clientConnector.setSslContextFactory(sslContextFactory);
        int timeout = configuration.getIntegerProperty(RoCrateServerParameter.openBISTimeout);
        clientConnector.setIdleTimeout(Duration.ofMillis(timeout));
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
        Assert.assertEquals(200, response.getStatus());
    }

    @Test
    public void validateRoCrateOpenAPITest()
            throws Exception
    {
        getConfiguration();

        OpenBIS openBIS = new OpenBIS("http://localhost:8888", Integer.MAX_VALUE);
        openBIS.login("system",
                "changeit");

        //        given()
        //                .header("sessionToken", openBIS.getSessionToken())
        //                .body(RoCrateServiceTest.roCrateMetadataJsonExample1.getBytes())
        //                .when().post("http://localhost:8085/openbis/open-api/ro-crate/import")
        //                .then()
        //                .statusCode(200);

        Configuration configuration = getConfiguration();

        String apiMethod = "validate";
        int port = configuration.getIntegerProperty(RoCrateServerParameter.httpServerPort);
        SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
        sslContextFactory.setTrustAll(true);
        ClientConnector clientConnector = new ClientConnector();
        clientConnector.setSslContextFactory(sslContextFactory);
        int timeout = configuration.getIntegerProperty(RoCrateServerParameter.openBISTimeout);
        clientConnector.setIdleTimeout(Duration.ofMillis(timeout));
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
        Assert.assertEquals(200, response.getStatus());
    }
}
