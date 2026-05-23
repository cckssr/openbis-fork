package ch.ethz.sis.openbis.systemtests.environment;

import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.eclipse.jetty.ee10.proxy.ProxyServlet;
import org.eclipse.jetty.http.UriCompliance;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.remoting.rmi.CodebaseAwareObjectInputStream;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.ethz.sis.shared.log.classic.impl.Logger;
import ch.systemsx.cisd.common.db.IDatabaseVersionHolder;
import ch.systemsx.cisd.openbis.generic.shared.util.TestInstanceHostUtils;

public class ApplicationServer
{

    private static final Logger log = LogFactory.getLogger(ApplicationServer.class);

    private Properties serviceProperties;

    private org.eclipse.jetty.server.Server applicationServer;

    private GenericWebApplicationContext applicationContext;

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
            throw new RuntimeException("Service properties cannot be null");
        }

        startProxy();
        startServer();
    }

    private void startServer()
    {
        try
        {
            log.info("Starting application server.");

            Properties properties = serviceProperties;
            for (Object key : properties.keySet())
            {
                Object value = properties.get(key);
                System.setProperty(String.valueOf(key), String.valueOf(value));
            }

            System.setProperty("org.eclipse.jetty.util.UrlEncoded.allowEncodedSlash", "true");

            org.eclipse.jetty.server.Server server = new org.eclipse.jetty.server.Server();
            HttpConfiguration httpConfig = new HttpConfiguration();
            httpConfig.setUriCompliance(UriCompliance.UNSAFE);
            ServerConnector connector =
                    new ServerConnector(server, new HttpConnectionFactory(httpConfig));
            connector.setPort(TestInstanceHostUtils.getOpenBISPort());
            server.addConnector(connector);
            DispatcherServlet dispatcherServlet = new DispatcherServlet()
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected WebApplicationContext findWebApplicationContext()
                {
//                    XmlBeanFactory beanFactory =
//                            new XmlBeanFactory(new FileSystemResource("../server-application-server/resource/server/spring-servlet.xml"));
//                    applicationContext = new GenericWebApplicationContext(beanFactory);
//                    GenericWebApplicationContext wac = new GenericWebApplicationContext();
                    applicationContext = new GenericWebApplicationContext();
                    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(applicationContext);
                    reader.loadBeanDefinitions(new FileSystemResource(
                            "../server-application-server/resource/server/spring-servlet.xml"));

                    applicationContext.setParent(new ClassPathXmlApplicationContext("classpath:applicationContext.xml"));
                    applicationContext.refresh();
                    return applicationContext;
                }
            };
            ServletContextHandler servletContext =
                    new ServletContextHandler(ServletContextHandler.SESSIONS);

            servletContext.setContextPath("/");
            server.setHandler(servletContext);

            ServletHolder servlet = new ServletHolder(dispatcherServlet);
            servlet.getRegistration().setMultipartConfig(
                    new MultipartConfigElement(System.getProperty("java.io.tmpdir"))
            );
            servletContext.addServlet(servlet, "/*");

            server.start();

            applicationServer = server;

            log.info("Started application server.");

        } catch (Exception e)
        {
            log.error("Starting application server failed.", e);
            throw new RuntimeException(e);
        }
    }

    private void startProxy()
    {
        try
        {
            log.info("Starting application server proxy.");

            org.eclipse.jetty.server.Server server = new org.eclipse.jetty.server.Server();
            HttpConfiguration httpConfig = new HttpConfiguration();
            httpConfig.setUriCompliance(UriCompliance.UNSAFE);
            ServerConnector connector =
                    new ServerConnector(server, new HttpConnectionFactory(httpConfig));
            connector.setPort(TestInstanceHostUtils.getOpenBISProxyPort());
            server.addConnector(connector);
            ProxyServlet proxyServlet = new ProxyServlet.Transparent()
            {
                @Override protected void service(final HttpServletRequest request, final HttpServletResponse response)
                {
                    try
                    {
                        ProxyRequest proxyRequest = new ProxyRequest(request);

                        if (Objects.equals(request.getContentType(), "application/octet-stream")
                                    || Objects.equals(request.getContentType(),"application/x-java-serialized-object"))
                        {
                            CodebaseAwareObjectInputStream objectInputStream =
                                    new CodebaseAwareObjectInputStream(proxyRequest.getInputStream(), getClass().getClassLoader(), true);
                            RemoteInvocation remoteInvocation = (RemoteInvocation) objectInputStream.readObject();

                            log.info(
                                    "[AS PROXY] url: " + proxyRequest.getRequestURL() + ", method: " + remoteInvocation.getMethodName()
                                            + ", parameters: "
                                            + Arrays.toString(
                                            remoteInvocation.getArguments()));

                            if (proxyInterceptor != null)
                            {
                                proxyInterceptor.invoke(remoteInvocation.getMethodName(), () ->
                                {
                                    super.service(proxyRequest, response);
                                    return null;
                                });
                            } else
                            {
                                super.service(proxyRequest, response);
                            }
                        } else
                        {
                            super.service(proxyRequest, response);
                        }
                    } catch (Exception e)
                    {
                        log.info("[AS PROXY] failed");
                        throw new RuntimeException(e);
                    }
                }
            };
            ServletHolder proxyServletHolder = new ServletHolder(proxyServlet);
            proxyServletHolder.setInitParameter("proxyTo", TestInstanceHostUtils.getOpenBISUrl() + "/");
            ServletContextHandler servletContext =
                    new ServletContextHandler(ServletContextHandler.SESSIONS);
            servletContext.setContextPath("/");
            server.setHandler(servletContext);

            servletContext.addServlet(proxyServletHolder, "/*");
            server.start();

            proxyServer = server;

            log.info("Started application server proxy.");
        } catch (Exception e)
        {
            log.error("Starting application server proxy failed.", e);
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
            applicationContext.close();
            ((ClassPathXmlApplicationContext) applicationContext.getParent()).close();
            applicationServer.stop();
            log.info("Stopped application server.");
        } catch (Exception e)
        {
            log.error("Stopping application server failed.", e);
            throw new RuntimeException(e);
        }
    }

    private void stopProxy()
    {
        try
        {
            proxyServer.stop();
            log.info("Stopped application server proxy.");
        } catch (Exception e)
        {
            log.error("Stopping application server proxy failed.", e);
            throw new RuntimeException(e);
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

    public GenericWebApplicationContext getApplicationContext()
    {
        return applicationContext;
    }

    public static class OpenBISDatabaseVersionHolder implements IDatabaseVersionHolder
    {

        @Override public String getDatabaseVersion()
        {
            return ch.systemsx.cisd.openbis.generic.server.dataaccess.db.DatabaseVersionHolder.getDatabaseVersion();
        }
    }

    public static class MessagesDatabaseVersionHolder implements IDatabaseVersionHolder
    {

        @Override public String getDatabaseVersion()
        {
            return ch.ethz.sis.messages.db.MessagesDatabaseVersionHolder.getDatabaseVersionStatic();
        }
    }

}
