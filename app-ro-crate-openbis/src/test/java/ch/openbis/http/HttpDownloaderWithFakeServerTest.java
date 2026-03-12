package ch.openbis.http;

import ch.openbis.rocrate.app.examples.files.remote.FakeHttpServer;
import com.sun.net.httpserver.HttpExchange;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Path;

public class HttpDownloaderWithFakeServerTest
{

    public static final FakeHttpServer.SpecialHandler
            REDIRECT_HANDLER = new FakeHttpServer.SpecialHandler()
    {
        @Override
        public void handle(HttpExchange exchange) throws IOException
        {
            if (exchange.getRequestURI().toString().contains("toberedirected"))
            {
                exchange.getResponseHeaders().add("Location", "http://localhost:8100/found");
                exchange.sendResponseHeaders(302, 0);
                return;
            }
            if (exchange.getRequestURI().toString().contains("found"))
            {
                byte[] content = "This is the response".getBytes();
                exchange.sendResponseHeaders(200, content.length);
                exchange.getResponseBody().write(content);
                return;
            }

            exchange.sendResponseHeaders(404, 0);

        }
    };

    static FakeHttpServer fakeHttpServer;

    @BeforeClass
    public static void setUp() throws IOException
    {
        fakeHttpServer = FakeHttpServer.build("localhost", 8100);
    }

    @AfterClass
    public static void tearDown()
    {
        fakeHttpServer.stop();
    }

    @Test
    public void testRedirect() throws Exception
    {
        fakeHttpServer.setSpecialHandler(REDIRECT_HANDLER);

        HttpDownloader httpDownloader = new HttpDownloader();
        httpDownloader.add("http://localhost:8100/toberedirected", Path.of("/tmp/outasdf.txt"));

        httpDownloader.error((url, path, exception) -> {
            throw new RuntimeException(exception);
        }).override((url, path) -> {
            return true;
        }).start();

    }

    @Test
    public void testNoRedirect() throws Exception
    {
        fakeHttpServer.setSpecialHandler(REDIRECT_HANDLER);

        HttpDownloader httpDownloader = new HttpDownloader();
        httpDownloader.add("http://localhost:8100/toberedirected", Path.of("/tmp/outasdf.txt"));

        HttpDownloader downloader = httpDownloader.error((url, path, exception) -> {
                    throw new RuntimeException(exception);
                }).override((url, path) -> {
                    return true;
                })
                .redirect(new HttpDownloader.HttpDownloaderRedirectPolicy()
                {
                    @Override
                    public HttpClient.Redirect getRedirect(String target)
                    {
                        return HttpClient.Redirect.NEVER;
                    }
                });
        Assert.assertThrows(RuntimeException.class, () -> downloader.start());

    }


}
