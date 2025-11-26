package ch.ethz.sis.openbis.systemtests.environment;

import java.util.Arrays;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.proxy.ProxyServlet;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.remoting.rmi.CodebaseAwareObjectInputStream;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.ethz.sis.shared.log.classic.impl.Logger;
import ch.systemsx.cisd.openbis.generic.shared.util.TestInstanceHostUtils;

public class ApplicationServer implements Server<ApplicationServerConfiguration>
{

    private static final Logger log = LogFactory.getLogger(ApplicationServer.class);

    private ApplicationServerConfiguration configuration;

    private org.eclipse.jetty.server.Server applicationServer;

    private GenericWebApplicationContext applicationContext;

    private org.eclipse.jetty.server.Server proxyServer;

    private ServerProxyInterceptor proxyInterceptor;

    @Override public void configure(final ApplicationServerConfiguration configuration)
    {
        if (configuration == null)
        {
            throw new RuntimeException("Configuration cannot be null");
        }
        if (configuration.getServiceProperties() == null)
        {
            throw new RuntimeException("Service properties cannot be null");
        }
        this.configuration = configuration;
    }

    @Override public void start()
    {
        if (configuration == null)
        {
            throw new RuntimeException("Application server hasn't been configured.");
        }

        startProxy();
        startServer();
    }

    private void startServer()
    {
        try
        {
            log.info("Starting application server.");

            Properties properties = configuration.getServiceProperties();
            for (Object key : properties.keySet())
            {
                Object value = properties.get(key);
                System.setProperty(String.valueOf(key), String.valueOf(value));
            }

            org.eclipse.jetty.server.Server server = new org.eclipse.jetty.server.Server();
            HttpConfiguration httpConfig = new HttpConfiguration();
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
                    XmlBeanFactory beanFactory =
                            new XmlBeanFactory(new FileSystemResource("../server-application-server/resource/server/spring-servlet.xml"));
                    applicationContext = new GenericWebApplicationContext(beanFactory);
                    applicationContext.setParent(new ClassPathXmlApplicationContext("classpath:applicationContext.xml"));
                    applicationContext.refresh();
                    return applicationContext;
                }
            };
            ServletContextHandler servletContext =
                    new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS);
            servletContext.addServlet(new ServletHolder(dispatcherServlet), "/*");

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
                        ServerProxyRequest proxyRequest = new ServerProxyRequest(request);

                        CodebaseAwareObjectInputStream objectInputStream =
                                new CodebaseAwareObjectInputStream(proxyRequest.getInputStream(), getClass().getClassLoader(), true);
                        RemoteInvocation remoteInvocation = (RemoteInvocation) objectInputStream.readObject();

                        log.info(
                                "[AS PROXY] url: " + proxyRequest.getRequestURL() + ", method: " + remoteInvocation.getMethodName() + ", parameters: "
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
                    new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS);
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

    @Override public void stop()
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

    public void setProxyInterceptor(final ServerProxyInterceptor proxyInterceptor)
    {
        this.proxyInterceptor = proxyInterceptor;
    }

    @Override public ApplicationServerConfiguration getConfiguration()
    {
        return configuration;
    }

    public GenericWebApplicationContext getApplicationContext()
    {
        return applicationContext;
    }

    @Override public StringBuffer getLogs()
    {
        return null;
    }

}
