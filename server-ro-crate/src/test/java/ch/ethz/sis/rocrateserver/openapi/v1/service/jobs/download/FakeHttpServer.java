package ch.ethz.sis.rocrateserver.openapi.v1.service.jobs.download;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FakeHttpServer
{

    private SpecialHandler specialHandler;

    private final MyHandler handler;

    public FakeHttpServer(MyHandler handler)
    {
        this.handler = handler;
    }

    public interface SpecialHandler
    {

        void handle(HttpExchange exchange) throws IOException;
    }

    public SpecialHandler getSpecialHandler()
    {
        return handler.getSpecialHandler();
    }

    public void setSpecialHandler(
            SpecialHandler specialHandler)
    {
        handler.setSpecialHandler(specialHandler);
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
        MyHandler handler1 = new MyHandler();
        httpServer.createContext("/", handler1);
        httpServer.setExecutor(null); // creates a default executor
        httpServer.start();
        return new FakeHttpServer(handler1);

    }

    static class MyHandler implements HttpHandler
    {
        SpecialHandler specialHandler;

        public SpecialHandler getSpecialHandler()
        {
            return specialHandler;
        }

        public void setSpecialHandler(
                SpecialHandler specialHandler)
        {
            this.specialHandler = specialHandler;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException
        {
            if (this.specialHandler != null)
            {
                specialHandler.handle(exchange);
                return;
            }

            String response = "This is the responserino";
            byte[] content;
            try
            {
                content = MessageDigest.getInstance("SHA-256")
                        .digest(exchange.getRequestURI().toString().getBytes());
            } catch (NoSuchAlgorithmException e)
            {
                throw new RuntimeException(e);
            }
            OutputStream os = exchange.getResponseBody();
            exchange.sendResponseHeaders(200, content.length);

            os.write(content);
            os.close();

        }
    }
}
