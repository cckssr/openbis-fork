/*
 * Copyright ETH 2010 - 2023 Zürich, Scientific IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.ethz.sis.openbis.systemtests.common;

import static org.testng.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.proxy.ProxyServlet;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
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
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import ch.ethz.sis.afs.manager.TransactionConnection;
import ch.ethz.sis.afsserver.server.archiving.ArchiverDatabaseConfiguration;
import ch.ethz.sis.afsserver.server.common.DatabaseConfiguration;
import ch.ethz.sis.afsserver.server.common.OpenBISConfiguration;
import ch.ethz.sis.afsserver.server.common.TestLogger;
import ch.ethz.sis.afsserver.server.messages.MessagesDatabaseConfiguration;
import ch.ethz.sis.afsserver.server.pathinfo.PathInfoDatabaseConfiguration;
import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameter;
import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameterUtil;
import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSetKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.create.DataSetCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.create.PhysicalDataCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.FileFormatTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.ProprietaryStorageFormatPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.RelativeLocationLocatorTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.id.DataStorePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.deletion.id.IDeletionId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.create.ExperimentCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.delete.ExperimentDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.IExperimentId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.update.ExperimentUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.Person;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.create.PersonCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.fetchoptions.PersonFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.id.PersonPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.create.ProjectCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.fetchoptions.ProjectFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.IProjectId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.ProjectPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.roleassignment.Role;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.roleassignment.create.RoleAssignmentCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.create.SampleCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.delete.SampleDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.ISampleId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SamplePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.update.SampleUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.create.SpaceCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.fetchoptions.SpaceFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.ISpaceId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import ch.ethz.sis.openbis.generic.server.asapi.v3.TransactionConfiguration;
import ch.ethz.sis.shared.startup.Configuration;
import ch.systemsx.cisd.common.filesystem.FileUtilities;
import ch.systemsx.cisd.common.filesystem.QueueingPathRemoverService;
import ch.systemsx.cisd.common.filesystem.SoftLinkMaker;
import ch.systemsx.cisd.common.properties.ExtendedProperties;
import ch.systemsx.cisd.dbmigration.postgresql.PostgreSQLDAOFactory;
import ch.systemsx.cisd.etlserver.ETLDaemon;
import ch.systemsx.cisd.openbis.dss.generic.server.DataStoreServer;
import ch.systemsx.cisd.openbis.dss.generic.shared.ArchiverServiceProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.ArchiverServiceProviderFactory;
import ch.systemsx.cisd.openbis.dss.generic.shared.HierarchicalContentServiceProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.HierarchicalContentServiceProviderFactory;
import ch.systemsx.cisd.openbis.dss.generic.shared.ServiceProviderFactory;
import ch.systemsx.cisd.openbis.dss.generic.shared.ServiceProviderImpl;
import ch.systemsx.cisd.openbis.dss.generic.shared.ShufflingServiceProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.ShufflingServiceProviderFactory;
import ch.systemsx.cisd.openbis.generic.shared.util.TestInstanceHostUtils;

/**
 * @author pkupczyk
 */
public abstract class AbstractIntegrationTest
{
    public static final String TEST_INTERACTIVE_SESSION_KEY = "integration-test-interactive-session-key";

    public static final String TEST_DATA_STORE_CODE = "STANDARD";

    public static final String DEFAULT_SPACE = "DEFAULT";

    public static final String TEST_SPACE = "TEST";

    public static final String INSTANCE_ADMIN = "admin";

    public static final String DEFAULT_SPACE_ADMIN = "default_space_admin";

    public static final String TEST_SPACE_ADMIN = "test_space_admin";

    public static final String TEST_SPACE_OBSERVER = "test_space_observer";

    public static final String PASSWORD = "password";

    private static Server applicationServerProxy;

    private static ProxyInterceptor applicationServerProxyInterceptor;

    private static Server applicationServer;

    protected static GenericWebApplicationContext applicationServerSpringContext;

    private static Server afsServerProxy;

    private static ProxyInterceptor afsServerProxyInterceptor;

    private static ch.ethz.sis.afsserver.server.Server<TransactionConnection, Object> afsServer;

    @BeforeSuite
    public void beforeSuite() throws Exception
    {
        initLogging();

        dropOpenBISDatabase();
        dropMessagesDatabase();
        dropPathInfoDatabase();
        dropArchiverDatabase();

        cleanupApplicationServerFolders();
        cleanupAfsServerFolders();
        cleanupDataStoreServerFolders();

        configureShares();
        configureELN();

        startApplicationServer();
        startApplicationServerProxy();
        createApplicationServerData();
        startDataStoreServer();
        startAfsServer();
        startAfsServerProxy();

        TestLogger.configure();
    }

    @AfterSuite
    public void afterSuite() throws Exception
    {
        shutdownAfsServer();
        shutdownAfsServerProxy();
        shutdownDataStoreServer();
        shutdownApplicationServer();
        shutdownApplicationServerProxy();
    }

    @BeforeMethod
    public void beforeMethod(Method method) throws Exception
    {
        log("\n>>>>>>>>>>>>>>>>\nBEFORE " + method.getDeclaringClass().getName() + "." + method.getName() + "\n>>>>>>>>>>>>>>>>\n");
        setApplicationServerProxyInterceptor(null);
        setAfsServerProxyInterceptor(null);
    }

    @AfterMethod
    public void afterMethod(Method method) throws Exception
    {
        log("\n<<<<<<<<<<<<<<<<\nAFTER  " + method.getDeclaringClass().getName() + "." + method.getName() + "\n<<<<<<<<<<<<<<<<\n");
    }

    private void initLogging()
    {
        System.setProperty("log4j.configuration", "etc/as/log4j1.xml");
        System.setProperty("log4j.configurationFile", "etc/as/log4j1.xml");
    }

    private void dropOpenBISDatabase() throws Exception
    {
        Properties properties = getApplicationServerConfiguration();

        Properties databaseProperties = ExtendedProperties.getSubset(properties, "database.", true);
        databaseProperties.setProperty(DatabaseConfiguration.NAME, "openbis");
        databaseProperties.setProperty(DatabaseConfiguration.VERSION_HOLDER_CLASS, TestOpenBISDatabaseVersionHolder.class.getName());
        databaseProperties.setProperty(DatabaseConfiguration.SCRIPT_FOLDER, properties.getProperty(DatabaseConfiguration.SCRIPT_FOLDER));

        DatabaseConfiguration configuration = new DatabaseConfiguration(databaseProperties);
        PostgreSQLDAOFactory factory = new PostgreSQLDAOFactory(configuration.getContext());
        factory.getDatabaseDAO().dropDatabase();
        log("Dropped openBIS database.");
    }

    private void dropMessagesDatabase()
    {
        MessagesDatabaseConfiguration configuration = MessagesDatabaseConfiguration.getInstance(getAfsServerConfiguration());
        PostgreSQLDAOFactory factory = new PostgreSQLDAOFactory(configuration.getContext());
        factory.getDatabaseDAO().dropDatabase();
        log("Dropped messages database.");
    }

    private void dropPathInfoDatabase()
    {
        PathInfoDatabaseConfiguration configuration = PathInfoDatabaseConfiguration.getInstance(getAfsServerConfiguration());
        PostgreSQLDAOFactory factory = new PostgreSQLDAOFactory(configuration.getContext());
        factory.getDatabaseDAO().dropDatabase();
        log("Dropped path info database.");
    }

    private void dropArchiverDatabase()
    {
        ArchiverDatabaseConfiguration configuration = ArchiverDatabaseConfiguration.getInstance(getAfsServerConfiguration());
        PostgreSQLDAOFactory factory = new PostgreSQLDAOFactory(configuration.getContext());
        factory.getDatabaseDAO().dropDatabase();
        log("Dropped archiver database.");
    }

    private void cleanupApplicationServerFolders() throws Exception
    {
        Properties configuration = getApplicationServerConfiguration();

        String transactionLogFolder = configuration.getProperty(TransactionConfiguration.TRANSACTION_LOG_FOLDER_PATH_PROPERTY_NAME);
        cleanupFolderSafely(transactionLogFolder);
    }

    private void cleanupAfsServerFolders() throws Exception
    {
        Configuration configuration = getAfsServerConfiguration();

        String writeAheadLogFolder = configuration.getStringProperty(AtomicFileSystemServerParameter.writeAheadLogRoot);
        cleanupFolderSafely(writeAheadLogFolder);

        String storageRoot = configuration.getStringProperty(AtomicFileSystemServerParameter.storageRoot);
        cleanupFolderSafely(storageRoot);

        String storageIncomingShareId = configuration.getStringProperty(AtomicFileSystemServerParameter.storageIncomingShareId);

        new File(storageRoot, storageIncomingShareId).mkdirs();
    }

    private void cleanupDataStoreServerFolders() throws Exception
    {
        cleanupFolderSafely("targets/storage");
        new File("targets/incoming-default").mkdirs();
    }

    private void cleanupFolderSafely(String folderPath) throws Exception
    {
        if (!new File(folderPath).exists())
        {
            return;
        }

        File safetyRoot = new File("../").getCanonicalFile();
        File folderParent = new File(folderPath).getCanonicalFile();

        while (folderParent != null && !Files.isSameFile(safetyRoot.toPath(), folderParent.toPath()))
        {
            folderParent = folderParent.getParentFile();
        }

        if (folderParent == null)
        {
            throw new RuntimeException(
                    "Folder " + new File(folderPath).getAbsolutePath() + " is outside of " + safetyRoot.getAbsolutePath()
                            + " therefore cannot be safely deleted.");
        } else
        {
            FileUtilities.deleteRecursively(new File(folderPath));
            log("Deleted folder: " + new File(folderPath).getAbsolutePath());
        }
    }

    private void configureShares() throws Exception
    {
        Configuration configuration = getAfsServerConfiguration();
        String storageRoot = AtomicFileSystemServerParameterUtil.getStorageRoot(configuration);
        ch.ethz.sis.shared.io.IOUtils.copy("etc/shares", storageRoot);
        log("Configured shares.");
    }

    private void configureELN() throws Exception
    {
        SoftLinkMaker.createSymbolicLink(new File("../ui-eln-lims/src/core-plugins/eln-lims"), new File("etc/as/core-plugins/eln-lims"));
        SoftLinkMaker.createSymbolicLink(new File("../ui-eln-lims/src/core-plugins/eln-lims"), new File("etc/dss/core-plugins/eln-lims"));
        SoftLinkMaker.createSymbolicLink(new File("../ui-admin/src/core-plugins/admin"), new File("etc/as/core-plugins/admin"));
        SoftLinkMaker.createSymbolicLink(new File("../ui-admin/src/core-plugins/admin"), new File("etc/dss/core-plugins/admin"));
        log("Configured ELN.");
    }

    private void startApplicationServer() throws Exception
    {
        log("Starting application server.");
        Properties configuration = getApplicationServerConfiguration();

        for (Object key : configuration.keySet())
        {
            Object value = configuration.get(key);
            System.setProperty(String.valueOf(key), String.valueOf(value));
        }

        Server server = new Server();
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
                applicationServerSpringContext = new GenericWebApplicationContext(beanFactory);
                applicationServerSpringContext.setParent(new ClassPathXmlApplicationContext("classpath:applicationContext.xml"));
                applicationServerSpringContext.refresh();
                return applicationServerSpringContext;
            }
        };
        ServletContextHandler servletContext =
                new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS);
        servletContext.addServlet(new ServletHolder(dispatcherServlet), "/*");
        server.start();

        AbstractIntegrationTest.applicationServer = server;
        log("Started application server.");
    }

    private void startApplicationServerProxy() throws Exception
    {
        log("Starting application server proxy.");
        Server server = new Server();
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
                    ProxyRequest proxyRequest = new ProxyRequest(request);

                    CodebaseAwareObjectInputStream objectInputStream =
                            new CodebaseAwareObjectInputStream(proxyRequest.getInputStream(), getClass().getClassLoader(), true);
                    RemoteInvocation remoteInvocation = (RemoteInvocation) objectInputStream.readObject();

                    System.out.println(
                            "[AS PROXY] url: " + proxyRequest.getRequestURL() + ", method: " + remoteInvocation.getMethodName() + ", parameters: "
                                    + Arrays.toString(
                                    remoteInvocation.getArguments()));

                    if (applicationServerProxyInterceptor != null)
                    {
                        applicationServerProxyInterceptor.invoke(remoteInvocation.getMethodName(), () ->
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
                    System.out.println("[AS PROXY] failed");
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

        AbstractIntegrationTest.applicationServerProxy = server;
        log("Started application server proxy.");
    }

    private void createApplicationServerData() throws Exception
    {
        Configuration configuration = getAfsServerConfiguration();

        OpenBIS openBIS = createOpenBIS();
        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        String afsServerUser = OpenBISConfiguration.getInstance(configuration).getOpenBISUser();
        createUser(openBIS, afsServerUser, null, Role.ETL_SERVER);

        createSpace(openBIS, TEST_SPACE);
        createUser(openBIS, TEST_SPACE_ADMIN, TEST_SPACE, Role.ADMIN);
        createUser(openBIS, TEST_SPACE_OBSERVER, TEST_SPACE, Role.OBSERVER);

        createUser(openBIS, DEFAULT_SPACE_ADMIN, DEFAULT_SPACE, Role.ADMIN);

        SampleUpdate elnSettingsUpdate = new SampleUpdate();
        elnSettingsUpdate.setSampleId(new SampleIdentifier("/ELN_SETTINGS/GENERAL_ELN_SETTINGS"));
        elnSettingsUpdate.setProperties(Map.of("ELN_SETTINGS", FileUtilities.loadToString(new File("etc/as/eln-settings.json"))));

        openBIS.updateSamples(List.of(elnSettingsUpdate));
    }

    private void startDataStoreServer() throws Exception
    {
        log("Starting data store server.");
        Properties configuration = getDataStoreServerConfiguration();
        ServiceProviderFactory.setInstance(new ServiceProviderImpl());
        ArchiverServiceProviderFactory.setInstance(new ArchiverServiceProvider());
        ShufflingServiceProviderFactory.setInstance(new ShufflingServiceProvider());
        HierarchicalContentServiceProviderFactory.setInstance(new HierarchicalContentServiceProvider());
        QueueingPathRemoverService.start(new File(configuration.getProperty("root-dir")), ETLDaemon.shredderQueueFile);
        DataStoreServer.main(new String[0]);
        log("Started data store server.");
    }

    private void startAfsServer() throws Exception
    {
        log("Starting afs server.");
        Configuration configuration = getAfsServerConfiguration();
        AbstractIntegrationTest.afsServer = new ch.ethz.sis.afsserver.server.Server<>(configuration);
        log("Started afs server.");
    }

    private void startAfsServerProxy() throws Exception
    {
        log("Starting afs server proxy.");
        Server server = new Server();
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

                    System.out.println(
                            "[AFS PROXY] url: " + proxyRequest.getRequestURL() + ", method: " + parameters.get("method") + ", parameters: "
                                    + parameters);

                    if (afsServerProxyInterceptor != null)
                    {
                        afsServerProxyInterceptor.invoke(parameters.get("method"), () ->
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
                    System.out.println("[AFS PROXY] failed");
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

        AbstractIntegrationTest.afsServerProxy = server;
        log("Started afs server proxy.");
    }

    private void shutdownApplicationServer() throws Exception
    {
        applicationServerSpringContext.close();
        ((ClassPathXmlApplicationContext) applicationServerSpringContext.getParent()).close();
        applicationServer.stop();
        log("Shut down application server.");
    }

    private void shutdownApplicationServerProxy()
    {
        applicationServerProxy.setStopAtShutdown(true);
        log("Shut down application server proxy.");
    }

    private void shutdownDataStoreServer()
    {
        DataStoreServer.stop();
        log("Shut down data store server.");
    }

    private void shutdownAfsServer() throws Exception
    {
        afsServer.shutdown(false);
        log("Shut down afs server.");
    }

    private void shutdownAfsServerProxy()
    {
        afsServerProxy.setStopAtShutdown(true);
        log("Shut down afs server proxy.");
    }

    public void restartApplicationServer() throws Exception
    {
        log("Restarting application server.");
        shutdownApplicationServer();
        startApplicationServer();
    }

    public void restartAfsServer() throws Exception
    {
        log("Restarting afs server.");
        shutdownAfsServer();
        startAfsServer();
    }

    public static Properties getApplicationServerConfiguration() throws Exception
    {
        Properties configuration = new Properties();
        configuration.load(new FileInputStream("etc/as/service.properties"));
        configuration.setProperty(TransactionConfiguration.APPLICATION_SERVER_URL_PROPERTY_NAME, TestInstanceHostUtils.getOpenBISProxyUrl());
        configuration.setProperty(TransactionConfiguration.AFS_SERVER_URL_PROPERTY_NAME,
                TestInstanceHostUtils.getAFSProxyUrl() + TestInstanceHostUtils.getAFSPath());
        return configuration;
    }

    public static Properties getDataStoreServerConfiguration() throws Exception
    {
        Properties configuration = new Properties();
        configuration.load(new FileInputStream("etc/dss/service.properties"));
        configuration.setProperty("server-url", TestInstanceHostUtils.getOpenBISProxyUrl());
        configuration.setProperty("port", String.valueOf(TestInstanceHostUtils.getDSSPort()));
        configuration.setProperty("download-url", TestInstanceHostUtils.getDSSUrl());
        configuration.store(new FileOutputStream(new File("etc/service.properties")),
                "This file has been generated. DSS has service.properties location hardcoded, without this file it won't start up");
        return configuration;
    }

    public static Configuration getAfsServerConfiguration()
    {
        Configuration configuration = new Configuration(List.of(AtomicFileSystemServerParameter.class),
                "etc/afs/service.properties");
        configuration.setProperty(AtomicFileSystemServerParameter.httpServerPort, String.valueOf(TestInstanceHostUtils.getAFSPort()));
        configuration.setProperty(AtomicFileSystemServerParameter.httpServerUri, TestInstanceHostUtils.getAFSPath());
        configuration.setProperty(OpenBISConfiguration.OpenBISParameter.openBISUrl, TestInstanceHostUtils.getOpenBISProxyUrl());
        return configuration;
    }

    public static void setApplicationServerProxyInterceptor(
            final ProxyInterceptor applicationServerProxyInterceptor)
    {
        AbstractIntegrationTest.applicationServerProxyInterceptor = applicationServerProxyInterceptor;
    }

    public static void setAfsServerProxyInterceptor(final ProxyInterceptor afsServerProxyInterceptor)
    {
        AbstractIntegrationTest.afsServerProxyInterceptor = afsServerProxyInterceptor;
    }

    public interface ProxyInterceptor
    {
        void invoke(String method, Callable<Void> defaultAction) throws Exception;
    }

    private static class ProxyRequest extends HttpServletRequestWrapper
    {
        private boolean read;

        private byte[] bytes;

        public ProxyRequest(final HttpServletRequest request)
        {
            super(request);
        }

        @Override public ServletInputStream getInputStream() throws IOException
        {
            if (!read)
            {
                bytes = IOUtils.toByteArray(super.getInputStream());
                read = true;
            }
            return new ServletInputStream()
            {
                private final ByteArrayInputStream bytesStream = new ByteArrayInputStream(bytes);

                @Override public int read()
                {
                    return bytesStream.read();
                }

                @Override public boolean isReady()
                {
                    return true;
                }

                @Override public boolean isFinished()
                {
                    return bytesStream.available() == 0;
                }

                @Override public void setReadListener(final ReadListener readListener)
                {
                }

            };
        }
    }

    public static OpenBIS createOpenBIS()
    {
        return new OpenBIS(TestInstanceHostUtils.getOpenBISUrl() + TestInstanceHostUtils.getOpenBISPath(),
                TestInstanceHostUtils.getDSSUrl() + TestInstanceHostUtils.getDSSPath(),
                TestInstanceHostUtils.getAFSUrl() + TestInstanceHostUtils.getAFSPath());
    }

    public static Space createSpace(OpenBIS openBIS, String spaceCode)
    {
        SpaceCreation spaceCreation = new SpaceCreation();
        spaceCreation.setCode(spaceCode);
        List<SpacePermId> spaceIds = openBIS.createSpaces(List.of(spaceCreation));
        Space space = getSpace(openBIS, spaceIds.get(0));
        log("Created space " + space.getCode());
        return space;
    }

    public static Space getSpace(OpenBIS openBIS, ISpaceId spaceId)
    {
        return openBIS.getSpaces(List.of(spaceId), new SpaceFetchOptions()).get(spaceId);
    }

    public static Project createProject(OpenBIS openBIS, ISpaceId spaceId, String projectCode)
    {
        ProjectCreation projectCreation = new ProjectCreation();
        projectCreation.setSpaceId(spaceId);
        projectCreation.setCode(projectCode);
        List<ProjectPermId> projectIds = openBIS.createProjects(List.of(projectCreation));
        Project project = openBIS.getProjects(projectIds, new ProjectFetchOptions()).get(projectIds.get(0));
        log("Created project " + project.getIdentifier() + " (" + project.getPermId().getPermId() + ")");
        return project;
    }

    public static Experiment createExperiment(OpenBIS openBIS, IProjectId projectId, String experimentCode)
    {
        ExperimentCreation experimentCreation = new ExperimentCreation();
        experimentCreation.setTypeId(new EntityTypePermId("UNKNOWN"));
        experimentCreation.setProjectId(projectId);
        experimentCreation.setCode(experimentCode);
        List<ExperimentPermId> experimentIds = openBIS.createExperiments(List.of(experimentCreation));
        Experiment experiment = openBIS.getExperiments(experimentIds, new ExperimentFetchOptions()).get(experimentIds.get(0));
        log("Created experiment " + experiment.getIdentifier() + " (" + experiment.getPermId().getPermId() + ")");
        return experiment;
    }

    public static void makeExperimentImmutable(OpenBIS openBIS, IExperimentId experimentId)
    {
        ExperimentUpdate update = new ExperimentUpdate();
        update.setExperimentId(experimentId);
        update.makeDataImmutable();
        openBIS.updateExperiments(List.of(update));
    }

    public static void deleteExperiment(OpenBIS openBIS, IExperimentId experimentId)
    {
        ExperimentDeletionOptions options = new ExperimentDeletionOptions();
        options.setReason("test");
        IDeletionId deletionId = openBIS.deleteExperiments(List.of(experimentId), options);
        openBIS.confirmDeletions(List.of(deletionId));
        log("Deleted experiment " + experimentId);
    }

    public static Sample createSample(OpenBIS openBIS, ISpaceId spaceId, String sampleCode)
    {
        SampleCreation sampleCreation = new SampleCreation();
        sampleCreation.setTypeId(new EntityTypePermId("UNKNOWN"));
        sampleCreation.setSpaceId(spaceId);
        sampleCreation.setCode(sampleCode);
        List<SamplePermId> sampleIds = openBIS.createSamples(List.of(sampleCreation));
        Sample sample = getSample(openBIS, sampleIds.get(0));
        log("Created sample " + sample.getIdentifier() + " (" + sample.getPermId().getPermId() + ")");
        return sample;
    }

    public static Sample createSample(OpenBIS openBIS, IExperimentId experimentId, String sampleCode)
    {
        ExperimentFetchOptions experimentFetchOptions = new ExperimentFetchOptions();
        experimentFetchOptions.withProject().withSpace();

        Experiment experiment = openBIS.getExperiments(List.of(experimentId), experimentFetchOptions).get(experimentId);
        if (experiment == null)
        {
            throw new RuntimeException("Experiment with id " + experimentId + " hasn't been found.");
        }

        SampleCreation sampleCreation = new SampleCreation();
        sampleCreation.setTypeId(new EntityTypePermId("UNKNOWN"));
        sampleCreation.setSpaceId(experiment.getProject().getSpace().getPermId());
        sampleCreation.setExperimentId(experiment.getPermId());
        sampleCreation.setCode(sampleCode);
        List<SamplePermId> sampleIds = openBIS.createSamples(List.of(sampleCreation));
        Sample sample = getSample(openBIS, sampleIds.get(0));
        log("Created sample " + sample.getIdentifier() + " (" + sample.getPermId().getPermId() + ")");
        return sample;
    }

    public static void makeSampleImmutable(OpenBIS openBIS, ISampleId sampleId)
    {
        SampleUpdate update = new SampleUpdate();
        update.setSampleId(sampleId);
        update.makeDataImmutable();
        openBIS.updateSamples(List.of(update));
    }

    public static void deleteSample(OpenBIS openBIS, ISampleId sampleId)
    {
        SampleDeletionOptions options = new SampleDeletionOptions();
        options.setReason("test");
        IDeletionId deletionId = openBIS.deleteSamples(List.of(sampleId), options);
        openBIS.confirmDeletions(List.of(deletionId));
        log("Deleted sample " + sampleId);
    }

    public static Sample getSample(OpenBIS openBIS, ISampleId sampleId)
    {
        return openBIS.getSamples(List.of(sampleId), new SampleFetchOptions()).get(sampleId);
    }

    public static DataSet createDataSet(OpenBIS openBIS, IExperimentId experimentId, String dataSetCode, String testFile, byte[] testData)
            throws IOException
    {
        Configuration afsServerConfiguration = getAfsServerConfiguration();
        String storageRoot = AtomicFileSystemServerParameterUtil.getStorageRoot(afsServerConfiguration);
        String storageUuid = AtomicFileSystemServerParameterUtil.getStorageUuid(afsServerConfiguration);
        Integer shareId = AtomicFileSystemServerParameterUtil.getStorageIncomingShareId(afsServerConfiguration);

        List<String> dataSetFolderLocation = new ArrayList<>();
        dataSetFolderLocation.add(storageUuid);
        dataSetFolderLocation.addAll(Arrays.asList(ch.ethz.sis.shared.io.IOUtils.getShards(dataSetCode.toUpperCase())));
        dataSetFolderLocation.add(dataSetCode.toUpperCase());

        File dataSetFolder = new File(new File(storageRoot, String.valueOf(shareId)), String.join(File.separator, dataSetFolderLocation));

        Files.createDirectories(dataSetFolder.toPath());
        Path testFilePath = Files.createFile(Path.of(dataSetFolder.getPath(), testFile));
        ch.ethz.sis.shared.io.IOUtils.write(testFilePath.toFile().getAbsolutePath(), 0L, testData);

        PhysicalDataCreation physicalCreation = new PhysicalDataCreation();
        physicalCreation.setShareId(shareId.toString());
        physicalCreation.setLocation(String.join(File.separator, dataSetFolderLocation));
        physicalCreation.setFileFormatTypeId(new FileFormatTypePermId("PROPRIETARY"));
        physicalCreation.setLocatorTypeId(new RelativeLocationLocatorTypePermId());
        physicalCreation.setStorageFormatId(new ProprietaryStorageFormatPermId());
        physicalCreation.setH5arFolders(false);
        physicalCreation.setH5Folders(false);

        DataSetCreation dataSetCreation = new DataSetCreation();
        dataSetCreation.setDataStoreId(new DataStorePermId(TEST_DATA_STORE_CODE));
        dataSetCreation.setDataSetKind(DataSetKind.PHYSICAL);
        dataSetCreation.setTypeId(new EntityTypePermId("UNKNOWN"));
        dataSetCreation.setExperimentId(experimentId);
        dataSetCreation.setCode(dataSetCode);
        dataSetCreation.setPhysicalData(physicalCreation);

        List<DataSetPermId> dataSetIds = openBIS.createDataSetsAS(List.of(dataSetCreation));
        DataSet dataSet = openBIS.getDataSets(dataSetIds, new DataSetFetchOptions()).get(dataSetIds.get(0));

        log("Created dataSet " + dataSet.getPermId());
        return dataSet;
    }

    public static Person createUser(OpenBIS openBIS, String userId, String spaceCode, Role spaceRole)
    {
        PersonCreation personCreation = new PersonCreation();
        personCreation.setUserId(userId);
        PersonPermId personId = openBIS.createPersons(List.of(personCreation)).get(0);

        RoleAssignmentCreation roleCreation = new RoleAssignmentCreation();
        roleCreation.setUserId(personId);
        if (spaceCode != null)
        {
            roleCreation.setSpaceId(new SpacePermId(spaceCode));
        }
        roleCreation.setRole(spaceRole);
        openBIS.createRoleAssignments(List.of(roleCreation));

        Person person = openBIS.getPersons(List.of(personId), new PersonFetchOptions()).get(personId);
        log("Created user " + person.getUserId());
        return person;
    }

    public static void log(String message)
    {
        System.out.println("[TEST] " + message);
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

    public void assertExperimentExistsAtAS(String experimentPermId, boolean exists) throws Exception
    {
        try (Connection connection = applicationServerSpringContext.getBean(DataSource.class).getConnection();
                Statement statement = connection.createStatement())
        {
            ResultSet resultSet = statement.executeQuery("SELECT count(*) FROM experiments_all WHERE perm_id = '" + experimentPermId + "'");
            resultSet.next();
            assertEquals(resultSet.getInt(1), exists ? 1 : 0);
        }
    }

    public void assertSampleExistsAtAS(String samplePermId, boolean exists) throws Exception
    {
        try (Connection connection = applicationServerSpringContext.getBean(DataSource.class).getConnection();
                Statement statement = connection.createStatement())
        {
            ResultSet resultSet = statement.executeQuery("SELECT count(*) FROM samples_all WHERE perm_id = '" + samplePermId + "'");
            resultSet.next();
            assertEquals(resultSet.getInt(1), exists ? 1 : 0);
        }
    }

    public void assertDSSDataSetExistsAtAS(String dataSetPermId, boolean exists) throws Exception
    {
        try (Connection connection = applicationServerSpringContext.getBean(DataSource.class).getConnection();
                Statement statement = connection.createStatement())
        {
            ResultSet resultSet = statement.executeQuery("SELECT count(*) FROM data_all WHERE afs_data = 'f' AND code = '" + dataSetPermId + "'");
            resultSet.next();
            assertEquals(resultSet.getInt(1), exists ? 1 : 0);
        }
    }

    public void assertAFSDataSetExistsAtAS(String dataSetPermId, boolean exists) throws Exception
    {
        try (Connection connection = applicationServerSpringContext.getBean(DataSource.class).getConnection();
                Statement statement = connection.createStatement())
        {
            ResultSet resultSet = statement.executeQuery("SELECT count(*) FROM data_all WHERE afs_data = 't' AND code = '" + dataSetPermId + "'");
            resultSet.next();
            assertEquals(resultSet.getInt(1), exists ? 1 : 0);
        }
    }

    public void assertDataExistsInStoreInShare(String dataSetPermId, boolean exists, Integer shareId) throws Exception
    {
        Configuration afsServerConfiguration = getAfsServerConfiguration();
        String storageRoot = AtomicFileSystemServerParameterUtil.getStorageRoot(afsServerConfiguration);
        String storageUuid = AtomicFileSystemServerParameterUtil.getStorageUuid(afsServerConfiguration);

        List<String> dataSetFolderLocation = new ArrayList<>();
        dataSetFolderLocation.add(storageUuid);
        dataSetFolderLocation.addAll(Arrays.asList(ch.ethz.sis.shared.io.IOUtils.getShards(dataSetPermId.toUpperCase())));
        dataSetFolderLocation.add(dataSetPermId.toUpperCase());

        Path dataSetFolder = Paths.get(storageRoot, String.valueOf(shareId), String.join(File.separator, dataSetFolderLocation));
        assertEquals(Files.exists(dataSetFolder), exists);

        try (Connection connection = applicationServerSpringContext.getBean(DataSource.class).getConnection();
                Statement statement = connection.createStatement())
        {
            ResultSet resultSet = statement.executeQuery(
                    "SELECT location, share_id FROM data_all d LEFT OUTER JOIN external_data ed on d.id = ed.id WHERE d.code = '"
                            + dataSetPermId.toUpperCase() + "'");
            resultSet.next();

            if (exists)
            {
                String locationInDB = resultSet.getString(1);
                String shareInDB = resultSet.getString(2);

                assertEquals(shareInDB, String.valueOf(shareId));
                assertEquals(locationInDB, String.join(File.separator, dataSetFolderLocation));
            }
        }
    }

}
