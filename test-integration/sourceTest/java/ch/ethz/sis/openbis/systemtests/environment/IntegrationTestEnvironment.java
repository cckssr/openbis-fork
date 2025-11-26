package ch.ethz.sis.openbis.systemtests.environment;

import java.io.File;
import java.nio.file.Files;
import java.util.Properties;

import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameter;
import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameterUtil;
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
import ch.systemsx.cisd.openbis.generic.shared.util.TestInstanceHostUtils;

public class IntegrationTestEnvironment
{

    private static final Logger log = LogFactory.getLogger(IntegrationTestEnvironment.class);

    private ApplicationServer applicationServer;

    private DataStoreServer dataStoreServer;

    private AfsServer afsServer;

    private RoCrateServer roCrateServer;

    public ApplicationServer createApplicationServer(ApplicationServerConfiguration configuration)
    {
        if (configuration != null && configuration.getServiceProperties() != null)
        {
            configuration.getServiceProperties()
                    .setProperty(TransactionConfiguration.APPLICATION_SERVER_URL_PROPERTY_NAME, TestInstanceHostUtils.getOpenBISProxyUrl());
            configuration.getServiceProperties().setProperty(TransactionConfiguration.AFS_SERVER_URL_PROPERTY_NAME,
                    TestInstanceHostUtils.getAFSProxyUrl() + TestInstanceHostUtils.getAFSPath());
        }

        applicationServer = new ApplicationServer();
        applicationServer.configure(configuration);
        return applicationServer;
    }

    public DataStoreServer createDataStoreServer(DataStoreServerConfiguration configuration)
    {
        if (configuration != null && configuration.getServiceProperties() != null)
        {
            configuration.getServiceProperties().setProperty("server-url", TestInstanceHostUtils.getOpenBISProxyUrl());
            configuration.getServiceProperties().setProperty("port", String.valueOf(TestInstanceHostUtils.getDSSPort()));
            configuration.getServiceProperties().setProperty("download-url", TestInstanceHostUtils.getDSSUrl());
        }

        dataStoreServer = new DataStoreServer();
        dataStoreServer.configure(configuration);
        return dataStoreServer;
    }

    public AfsServer createAfsServer(AfsServerConfiguration configuration)
    {
        if (configuration != null && configuration.getServiceProperties() != null)
        {
            configuration.getServiceProperties()
                    .setProperty(AtomicFileSystemServerParameter.httpServerPort.name(), String.valueOf(TestInstanceHostUtils.getAFSPort()));
            configuration.getServiceProperties()
                    .setProperty(AtomicFileSystemServerParameter.httpServerUri.name(), TestInstanceHostUtils.getAFSPath());
            configuration.getServiceProperties()
                    .setProperty(OpenBISConfiguration.OpenBISParameter.openBISUrl.name(), TestInstanceHostUtils.getOpenBISProxyUrl());
        }

        afsServer = new AfsServer();
        afsServer.configure(configuration);
        return afsServer;
    }

    public RoCrateServer createRoCrateServer(RoCrateServerConfiguration configuration)
    {
        if (configuration != null && configuration.getServiceProperties() != null)
        {
            configuration.getServiceProperties().setProperty("httpServerPort", String.valueOf(TestInstanceHostUtils.getRoCratePort()));
            configuration.getServiceProperties().setProperty("openBISUrl", TestInstanceHostUtils.getOpenBISProxyUrl());
        }

        roCrateServer = new RoCrateServer();
        roCrateServer.configure(configuration);
        return roCrateServer;
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
        Properties properties = applicationServer.getConfiguration().getServiceProperties();

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
        Configuration afsConfiguration = new Configuration(afsServer.getConfiguration().getServiceProperties());
        MessagesDatabaseConfiguration configuration = MessagesDatabaseConfiguration.getInstance(afsConfiguration);
        PostgreSQLDAOFactory factory = new PostgreSQLDAOFactory(configuration.getContext());
        factory.getDatabaseDAO().dropDatabase();
        log.info("Dropped messages database.");
    }

    private void dropPathInfoDatabase()
    {
        Configuration afsConfiguration = new Configuration(afsServer.getConfiguration().getServiceProperties());
        PathInfoDatabaseConfiguration configuration = PathInfoDatabaseConfiguration.getInstance(afsConfiguration);
        PostgreSQLDAOFactory factory = new PostgreSQLDAOFactory(configuration.getContext());
        factory.getDatabaseDAO().dropDatabase();
        log.info("Dropped path info database.");
    }

    private void dropArchiverDatabase()
    {
        Configuration afsConfiguration = new Configuration(afsServer.getConfiguration().getServiceProperties());
        ArchiverDatabaseConfiguration configuration = ArchiverDatabaseConfiguration.getInstance(afsConfiguration);
        PostgreSQLDAOFactory factory = new PostgreSQLDAOFactory(configuration.getContext());
        factory.getDatabaseDAO().dropDatabase();
        log.info("Dropped archiver database.");
    }

    private void cleanupApplicationServerFolders()
    {
        Properties configuration = applicationServer.getConfiguration().getServiceProperties();
        String transactionLogFolder = configuration.getProperty(TransactionConfiguration.TRANSACTION_LOG_FOLDER_PATH_PROPERTY_NAME);
        cleanupFolderSafely(transactionLogFolder);
    }

    private void cleanupAfsServerFolders()
    {
        Configuration configuration = new Configuration(afsServer.getConfiguration().getServiceProperties());

        String writeAheadLogFolder = configuration.getStringProperty(AtomicFileSystemServerParameter.writeAheadLogRoot);
        cleanupFolderSafely(writeAheadLogFolder);

        String storageRoot = configuration.getStringProperty(AtomicFileSystemServerParameter.storageRoot);
        cleanupFolderSafely(storageRoot);

        String storageIncomingShareId = configuration.getStringProperty(AtomicFileSystemServerParameter.storageIncomingShareId);

        new File(storageRoot, storageIncomingShareId).mkdirs();
    }

    private void cleanupDataStoreServerFolders()
    {
        cleanupFolderSafely("targets/storage");
        new File("targets/incoming-default").mkdirs();
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
            Configuration afsConfiguration = new Configuration(afsServer.getConfiguration().getServiceProperties());
            String storageRoot = AtomicFileSystemServerParameterUtil.getStorageRoot(afsConfiguration);
            ch.ethz.sis.shared.io.IOUtils.copy("etc/shares", storageRoot);
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