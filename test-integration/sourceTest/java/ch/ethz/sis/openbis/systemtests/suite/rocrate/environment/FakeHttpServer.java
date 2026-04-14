package ch.ethz.sis.openbis.systemtests.suite.rocrate.environment;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.SneakyThrows;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FakeHttpServer
{
    private ExecutorService service;

    private HttpServer server;

    public FakeHttpServer(ExecutorService service, HttpServer server)
    {
        this.service = service;
        this.server = server;
    }

    public static void main(String[] args) throws IOException
    {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("localhost", 3221), 0);
        httpServer.createContext("/", new MyHandler());
        httpServer.setExecutor(null); // creates a default executor
        httpServer.start();
    }

    public static FakeHttpServer build(String host, int port) throws IOException
    {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(host, port), 0);
        httpServer.createContext("/", new MyHandler());
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        httpServer.setExecutor(executorService); // creates a default executor
        httpServer.start();
        return new FakeHttpServer(executorService, httpServer);

    }

    public void stop() throws InterruptedException
    {
        this.server.stop(1);
        this.service.shutdownNow();
        this.service.awaitTermination(1000L, TimeUnit.MILLISECONDS);

    }

    static class MyHandler implements HttpHandler
    {
        @SneakyThrows
        @Override
        public void handle(HttpExchange exchange) throws IOException
        {
            byte[] content = exchange.getRequestURI().toString().getBytes();
            OutputStream os = exchange.getResponseBody();
            exchange.sendResponseHeaders(200, content.length);
            os.write(content);
            os.close();

        }
    }
}
