package ch.ethz.sis.openbis.systemtests.environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameter;
import ch.ethz.sis.openbis.afsserver.server.archiving.ArchiverDatabaseConfiguration;
import ch.ethz.sis.openbis.afsserver.server.common.DatabaseConfiguration;
import ch.ethz.sis.openbis.afsserver.server.common.OpenBISConfiguration;
import ch.ethz.sis.openbis.afsserver.server.messages.MessagesDatabaseConfiguration;
import ch.ethz.sis.openbis.afsserver.server.pathinfo.PathInfoDatabaseConfiguration;
import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.server.asapi.v3.TransactionConfiguration;
import ch.ethz.sis.openbis.systemtests.common.TestOpenBISDatabaseVersionHolder;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.ethz.sis.shared.log.classic.impl.Logger;
import ch.ethz.sis.shared.startup.Configuration;
import ch.systemsx.cisd.common.filesystem.FileUtilities;
import ch.systemsx.cisd.common.filesystem.SoftLinkMaker;
import ch.systemsx.cisd.common.properties.ExtendedProperties;
import ch.systemsx.cisd.dbmigration.postgresql.PostgreSQLDAOFactory;
import ch.systemsx.cisd.openbis.dss.generic.shared.utils.DssPropertyParameters;
import ch.systemsx.cisd.openbis.dss.generic.shared.utils.DssPropertyParametersUtil;
import ch.systemsx.cisd.openbis.generic.shared.util.TestInstanceHostUtils;

public class IntegrationTestEnvironment
{

    private static final Logger log = LogFactory.getLogger(IntegrationTestEnvironment.class);

    private ApplicationServer applicationServer;

    private DataStoreServer dataStoreServer;

    private AfsServer afsServer;

    private RoCrateServer roCrateServer;

    private List<Share> shares = new ArrayList<>();

    public ApplicationServer createApplicationServer(Path serviceProperties)
    {
        return createApplicationServer(loadProperties(serviceProperties));
    }

    public ApplicationServer createApplicationServer(Properties serviceProperties)
    {
        if (serviceProperties != null)
        {
            serviceProperties.setProperty(TransactionConfiguration.APPLICATION_SERVER_URL_PROPERTY_NAME, TestInstanceHostUtils.getOpenBISProxyUrl());
            serviceProperties.setProperty(TransactionConfiguration.AFS_SERVER_URL_PROPERTY_NAME,
                    TestInstanceHostUtils.getAFSProxyUrl() + TestInstanceHostUtils.getAFSPath());
        }

        applicationServer = new ApplicationServer();
        applicationServer.configure(serviceProperties);
        return applicationServer;
    }

    public DataStoreServer createDataStoreServer(Path serviceProperties)
    {
        return createDataStoreServer(loadProperties(serviceProperties));
    }

    public DataStoreServer createDataStoreServer(Properties serviceProperties)
    {
        if (serviceProperties != null)
        {
            serviceProperties.setProperty("server-url", TestInstanceHostUtils.getOpenBISProxyUrl());
            serviceProperties.setProperty("port", String.valueOf(TestInstanceHostUtils.getDSSPort()));
            serviceProperties.setProperty("download-url", TestInstanceHostUtils.getDSSUrl());
        }

        dataStoreServer = new DataStoreServer();
        dataStoreServer.configure(serviceProperties);
        return dataStoreServer;
    }

    public AfsServer createAfsServer(Path serviceProperties)
    {
        return createAfsServer(loadProperties(serviceProperties));
    }

    public AfsServer createAfsServer(Properties serviceProperties)
    {
        if (serviceProperties != null)
        {
            serviceProperties.setProperty(AtomicFileSystemServerParameter.httpServerPort.name(), String.valueOf(TestInstanceHostUtils.getAFSPort()));
            serviceProperties.setProperty(AtomicFileSystemServerParameter.httpServerUri.name(), TestInstanceHostUtils.getAFSPath());
            serviceProperties.setProperty(OpenBISConfiguration.OpenBISParameter.openBISUrl.name(), TestInstanceHostUtils.getOpenBISProxyUrl());
        }

        afsServer = new AfsServer();
        afsServer.configure(serviceProperties);
        return afsServer;
    }

    public RoCrateServer createRoCrateServer(Path serviceProperties)
    {
        return createRoCrateServer(loadProperties(serviceProperties));
    }

    public RoCrateServer createRoCrateServer(Properties serviceProperties)
    {
        if (serviceProperties != null)
        {
            serviceProperties.setProperty("httpServerPort", String.valueOf(TestInstanceHostUtils.getRoCratePort()));
            serviceProperties.setProperty("openBISUrl", TestInstanceHostUtils.getOpenBISProxyUrl());
        }

        roCrateServer = new RoCrateServer();
        roCrateServer.configure(serviceProperties);
        return roCrateServer;
    }

    public Share createShare(int shareNumber, Path shareProperties)
    {
        return createShare(shareNumber, loadProperties(shareProperties));
    }

    public Share createShare(int shareNumber, Properties shareProperties)
    {
        Share share = new Share(shareNumber, shareProperties);
        shares.add(share);
        return share;
    }

    public void start()
    {
        dropOpenBISDatabase();
        dropMessagesDatabase();
        dropPathInfoDatabase();
        dropArchiverDatabase();

        cleanupApplicationServerFolders();
        cleanupAfsServerFolders();
        cleanupDataStoreServerFolders();

        configureShares();
        configureELN();

        applicationServer.start();
        dataStoreServer.start();
        afsServer.start();
        roCrateServer.start();
    }

    public void stop()
    {
        roCrateServer.stop();
        afsServer.stop();
        dataStoreServer.stop();
        applicationServer.stop();
    }

    public OpenBIS createOpenBIS()
    {
        return new OpenBIS(TestInstanceHostUtils.getOpenBISUrl() + TestInstanceHostUtils.getOpenBISPath(),
                TestInstanceHostUtils.getDSSUrl() + TestInstanceHostUtils.getDSSPath(),
                TestInstanceHostUtils.getAFSUrl() + TestInstanceHostUtils.getAFSPath());
    }

    private void dropOpenBISDatabase()
    {
        Properties properties = applicationServer.getServiceProperties();

        Properties databaseProperties = ExtendedProperties.getSubset(properties, "database.", true);
        databaseProperties.setProperty(DatabaseConfiguration.NAME, "openbis");
        databaseProperties.setProperty(DatabaseConfiguration.VERSION_HOLDER_CLASS, TestOpenBISDatabaseVersionHolder.class.getName());
        databaseProperties.setProperty(DatabaseConfiguration.SCRIPT_FOLDER, properties.getProperty(DatabaseConfiguration.SCRIPT_FOLDER));

        DatabaseConfiguration configuration = new DatabaseConfiguration(databaseProperties);
        PostgreSQLDAOFactory factory = new PostgreSQLDAOFactory(configuration.getContext());
        factory.getDatabaseDAO().dropDatabase();
        log.info("Dropped openBIS database.");
    }

    private void dropMessagesDatabase()
    {
        Configuration afsConfiguration = new Configuration(afsServer.getServiceProperties());
        MessagesDatabaseConfiguration configuration = MessagesDatabaseConfiguration.getInstance(afsConfiguration);
        PostgreSQLDAOFactory factory = new PostgreSQLDAOFactory(configuration.getContext());
        factory.getDatabaseDAO().dropDatabase();
        log.info("Dropped messages database.");
    }

    private void dropPathInfoDatabase()
    {
        Configuration afsConfiguration = new Configuration(afsServer.getServiceProperties());
        PathInfoDatabaseConfiguration configuration = PathInfoDatabaseConfiguration.getInstance(afsConfiguration);
        PostgreSQLDAOFactory factory = new PostgreSQLDAOFactory(configuration.getContext());
        factory.getDatabaseDAO().dropDatabase();
        log.info("Dropped path info database.");
    }

    private void dropArchiverDatabase()
    {
        Configuration afsConfiguration = new Configuration(afsServer.getServiceProperties());
        ArchiverDatabaseConfiguration configuration = ArchiverDatabaseConfiguration.getInstance(afsConfiguration);
        PostgreSQLDAOFactory factory = new PostgreSQLDAOFactory(configuration.getContext());
        factory.getDatabaseDAO().dropDatabase();
        log.info("Dropped archiver database.");
    }

    private void cleanupApplicationServerFolders()
    {
        Properties configuration = applicationServer.getServiceProperties();
        String transactionLogFolder = configuration.getProperty(TransactionConfiguration.TRANSACTION_LOG_FOLDER_PATH_PROPERTY_NAME);
        cleanupFolderSafely(transactionLogFolder);
    }

    private void cleanupAfsServerFolders()
    {
        Configuration configuration = new Configuration(afsServer.getServiceProperties());

        String writeAheadLogFolder = configuration.getStringProperty(AtomicFileSystemServerParameter.writeAheadLogRoot);
        cleanupFolderSafely(writeAheadLogFolder);

        String storageRoot = configuration.getStringProperty(AtomicFileSystemServerParameter.storageRoot);
        cleanupFolderSafely(storageRoot);

        String storageIncomingShareId = configuration.getStringProperty(AtomicFileSystemServerParameter.storageIncomingShareId);

        new File(storageRoot, storageIncomingShareId).mkdirs();
    }

    private void cleanupDataStoreServerFolders()
    {
        ExtendedProperties properties = DssPropertyParametersUtil.loadProperties(DssPropertyParametersUtil.SERVICE_PROPERTIES_FILE);
        cleanupFolderSafely(properties.getProperty(DssPropertyParameters.STOREROOT_DIR_KEY));
    }

    private void cleanupFolderSafely(String folderPath)
    {
        try
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
                log.info("Deleted folder: " + new File(folderPath).getAbsolutePath());
            }
        } catch (Exception e)
        {
            throw new RuntimeException("Cleaning up folder: " + folderPath + " failed.", e);
        }
    }

    private void configureShares()
    {
        try
        {
            Configuration configuration = new Configuration(afsServer.getServiceProperties());
            String storageRoot = configuration.getStringProperty(AtomicFileSystemServerParameter.storageRoot);

            for (Share share : shares)
            {
                File shareFolder = new File(storageRoot, String.valueOf(share.getShareNumber()));
                shareFolder.mkdirs();
                share.getShareProperties().store(new FileOutputStream(new File(shareFolder, "share.properties")), null);
            }

            log.info("Configured shares.");
        } catch (Exception e)
        {
            throw new RuntimeException("Configuring shares failed.", e);
        }
    }

    private void configureELN()
    {
        SoftLinkMaker.createSymbolicLink(new File("../ui-eln-lims/src/core-plugins/eln-lims"), new File("etc/as/core-plugins/eln-lims"));
        SoftLinkMaker.createSymbolicLink(new File("../ui-eln-lims/src/core-plugins/eln-lims"), new File("etc/dss/core-plugins/eln-lims"));
        SoftLinkMaker.createSymbolicLink(new File("../ui-admin/src/core-plugins/admin"), new File("etc/as/core-plugins/admin"));
        SoftLinkMaker.createSymbolicLink(new File("../ui-admin/src/core-plugins/admin"), new File("etc/dss/core-plugins/admin"));
        log.info("Configured ELN.");
    }

    private static Properties loadProperties(final Path propertiesPath)
    {
        try
        {
            Properties properties = new Properties();
            properties.load(new FileInputStream(propertiesPath.toFile()));
            return properties;
        } catch (Exception e)
        {
            throw new RuntimeException("Loading properties from path: " + propertiesPath + " failed.", e);
        }
    }

    public ApplicationServer getApplicationServer()
    {
        return applicationServer;
    }

    public DataStoreServer getDataStoreServer()
    {
        return dataStoreServer;
    }

    public AfsServer getAfsServer()
    {
        return afsServer;
    }

    public RoCrateServer getRoCrateServer()
    {
        return roCrateServer;
    }

}