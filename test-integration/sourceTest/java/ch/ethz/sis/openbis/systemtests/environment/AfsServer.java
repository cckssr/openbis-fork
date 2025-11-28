package ch.ethz.sis.openbis.systemtests.environment;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.proxy.ProxyServlet;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import ch.ethz.sis.afs.manager.TransactionConnection;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.ethz.sis.shared.log.classic.impl.Logger;
import ch.ethz.sis.shared.startup.Configuration;
import ch.systemsx.cisd.openbis.generic.shared.util.TestInstanceHostUtils;

public class AfsServer
{

    private static final Logger log = LogFactory.getLogger(AfsServer.class);

    private Properties serviceProperties;

    private ch.ethz.sis.afsserver.server.Server<TransactionConnection, Object> server;

    private org.eclipse.jetty.server.Server proxyServer;

    private ProxyInterceptor proxyInterceptor;

    public void configure(final Properties serviceProperties)
    {
        if (serviceProperties == null)
        {
            throw new RuntimeException("Service properties cannot be null");
        }
        this.serviceProperties = serviceProperties;
    }

    public void start()
    {
        if (serviceProperties == null)
        {
            throw new RuntimeException("Afs server hasn't been configured.");
        }

        startProxy();
        startServer();
    }

    private void startServer()
    {
        try
        {
            log.info("Starting afs server.");
            server = new ch.ethz.sis.afsserver.server.Server<>(new Configuration(serviceProperties));
            log.info("Started afs server.");
        } catch (Exception e)
        {
            log.error("Starting afs server failed.", e);
            throw new RuntimeException(e);
        }
    }

    private void startProxy()
    {
        try
        {
            log.info("Starting afs server proxy.");
            org.eclipse.jetty.server.Server server = new org.eclipse.jetty.server.Server();
            HttpConfiguration httpConfig = new HttpConfiguration();
            ServerConnector connector =
                    new ServerConnector(server, new HttpConnectionFactory(httpConfig));
            connector.setPort(TestInstanceHostUtils.getAFSProxyPort());
            server.addConnector(connector);
            ProxyServlet proxyServlet = new ProxyServlet.Transparent()
            {
                @Override protected void service(final HttpServletRequest request, final HttpServletResponse response)
                {
                    try
                    {
                        ProxyRequest proxyRequest = new ProxyRequest(request);

                        Map<String, String> parameters = new HashMap<>();

                        Iterator<String> parametersInQueryStringIterator = proxyRequest.getParameterNames().asIterator();
                        while (parametersInQueryStringIterator.hasNext())
                        {
                            String name = parametersInQueryStringIterator.next();
                            parameters.put(name, proxyRequest.getParameter(name));
                        }

                        String parametersInBodyString = IOUtils.toString(proxyRequest.getInputStream());
                        Map<String, String> parametersInBody = parseUrlQuery(parametersInBodyString);

                        parameters.putAll(parametersInBody);

                        log.info(
                                "[AFS PROXY] url: " + proxyRequest.getRequestURL() + ", method: " + parameters.get("method") + ", parameters: "
                                        + parameters);

                        if (proxyInterceptor != null)
                        {
                            proxyInterceptor.invoke(parameters.get("method"), () ->
                            {
                                super.service(proxyRequest, response);
                                return null;
                            });
                        } else
                        {
                            super.service(proxyRequest, response);
                        }
                    } catch (Exception e)
                    {
                        log.info("[AFS PROXY] failed");
                        throw new RuntimeException(e);
                    }
                }
            };
            ServletHolder proxyServletHolder = new ServletHolder(proxyServlet);
            proxyServletHolder.setInitParameter("proxyTo", TestInstanceHostUtils.getAFSUrl());
            ServletContextHandler servletContext =
                    new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS);
            servletContext.addServlet(proxyServletHolder, "/*");
            server.start();

            proxyServer = server;
            log.info("Started afs server proxy.");
        } catch (Exception e)
        {
            log.error("Starting afs server proxy failed.", e);
            throw new RuntimeException(e);
        }
    }

    public void stop()
    {
        stopServer();
        stopProxy();
    }

    private void stopServer()
    {
        try
        {
            server.shutdown(false);
            log.info("Stopped afs server.");
        } catch (Exception e)
        {
            log.error("Stopping afs server failed.", e);
            throw new RuntimeException(e);
        }
    }

    private void stopProxy()
    {
        try
        {
            proxyServer.stop();
            log.info("Stopped afs server proxy.");
        } catch (Exception e)
        {
            log.error("Stopping afs server proxy failed.", e);
            throw new RuntimeException(e);
        }
    }

    private static Map<String, String> parseUrlQuery(String url) throws Exception
    {
        try
        {
            Map<String, String> parameters = new HashMap<>();
            String[] namesAndValues = url.split("&");
            for (String nameAndValue : namesAndValues)
            {
                int index = nameAndValue.indexOf("=");
                String name = nameAndValue.substring(0, index);
                String value = nameAndValue.substring(index + 1);
                parameters.put(URLDecoder.decode(name, StandardCharsets.UTF_8), URLDecoder.decode(value, StandardCharsets.UTF_8));
            }
            return parameters;
        } catch (Exception e)
        {
            return Collections.emptyMap();
        }
    }

    public void setProxyInterceptor(final ProxyInterceptor proxyInterceptor)
    {
        this.proxyInterceptor = proxyInterceptor;
    }

    public Properties getServiceProperties()
    {
        return serviceProperties;
    }
}
