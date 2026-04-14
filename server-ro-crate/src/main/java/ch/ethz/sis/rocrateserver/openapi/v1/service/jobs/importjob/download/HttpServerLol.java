package ch.ethz.sis.rocrateserver.openapi.v1.service.jobs.importjob.download;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class HttpServerLol
{
    public static void main(String[] args) throws IOException
    {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("localhost", 3221), 0);
        httpServer.createContext("/", new MyHandler());
        httpServer.setExecutor(null); // creates a default executor
        httpServer.start();
    }

    static class MyHandler implements HttpHandler
    {
        @Override
        public void handle(HttpExchange exchange) throws IOException
        {
            String response = "This is the responserino";
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();

        }
    }
}
