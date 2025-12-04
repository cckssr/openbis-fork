package ch.ethz.sis.openbis.systemtests.environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameter;
import ch.ethz.sis.openbis.afsserver.server.archiving.ArchiverDatabaseConfiguration;
import ch.ethz.sis.openbis.afsserver.server.common.DatabaseConfiguration;
import ch.ethz.sis.openbis.afsserver.server.common.OpenBISConfiguration;
import ch.ethz.sis.openbis.afsserver.server.messages.MessagesDatabaseConfiguration;
import ch.ethz.sis.openbis.afsserver.server.pathinfo.PathInfoDatabaseConfiguration;
import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.id.PersonPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.roleassignment.Role;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.roleassignment.create.RoleAssignmentCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.update.SampleUpdate;
import ch.ethz.sis.openbis.generic.server.asapi.v3.TransactionConfiguration;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.ethz.sis.shared.log.classic.impl.Logger;
import ch.ethz.sis.shared.startup.Configuration;
import ch.systemsx.cisd.common.filesystem.FileUtilities;
import ch.systemsx.cisd.common.filesystem.SoftLinkMaker;
import ch.systemsx.cisd.common.properties.ExtendedProperties;
import ch.systemsx.cisd.common.reflection.BeanUtils;
import ch.systemsx.cisd.dbmigration.DBUtilities;
import ch.systemsx.cisd.dbmigration.DatabaseConfigurationContext;
import ch.systemsx.cisd.dbmigration.SQLUtils;
import ch.systemsx.cisd.openbis.dss.generic.shared.utils.DssPropertyParameters;
import ch.systemsx.cisd.openbis.generic.shared.util.TestInstanceHostUtils;

public class IntegrationTestEnvironment
{

    private static final Logger log = LogFactory.getLogger(IntegrationTestEnvironment.class);

    private static final String SYSTEM_USER = "system";

    private static final String INSTANCE_ADMIN_USER = "admin";

    private static final String PASSWORD = "password";

    private ApplicationServer applicationServer;

    private DataStoreServer dataStoreServer;

    private AfsServer afsServer;

    private RoCrateServer roCrateServer;

    private List<Share> shares = new ArrayList<>();

    public IntegrationTestEnvironment()
    {
        createShares(Map.of(
                1, loadProperties(Path.of("etc/default/shares/1/share.properties")),
                2, loadProperties(Path.of("etc/default/shares/2/share.properties")),
                3, loadProperties(Path.of("etc/default/shares/3/share.properties")),
                4, loadProperties(Path.of("etc/default/shares/4/share.properties"))
        ));
    }

    public ApplicationServer createApplicationServer()
    {
        return createApplicationServer(loadProperties(Path.of("etc/default/as/service.properties")));
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

    public DataStoreServer createDataStoreServer()
    {
        return createDataStoreServer(loadProperties(Path.of("etc/default/dss/service.properties")));
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

    public AfsServer createAfsServer()
    {
        return createAfsServer(loadProperties(Path.of("etc/default/afs/service.properties")));
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

    public RoCrateServer createRoCrateServer()
    {
        return createRoCrateServer(loadProperties(Path.of("etc/default/ro-crate/service.properties")));
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

    public void createShares(Map<Integer, Properties> shares)
    {
        List<Share> newShares = new ArrayList<>();
        for (Map.Entry<Integer, Properties> entry : shares.entrySet())
        {
            Share share = new Share(entry.getKey(), entry.getValue());
            newShares.add(share);
        }
        this.shares = newShares;
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

        if (applicationServer != null)
        {
            applicationServer.start();
        }
        if (dataStoreServer != null)
        {
            dataStoreServer.start();
        }
        if (afsServer != null)
        {
            afsServer.start();
        }
        if (roCrateServer != null)
        {
            roCrateServer.start();
        }

        configureSystemUser();
        configureELNSettings();
    }

    public void stop()
    {
        if (roCrateServer != null)
        {
            roCrateServer.stop();
        }
        if (afsServer != null)
        {
            afsServer.stop();
        }
        if (dataStoreServer != null)
        {
            dataStoreServer.stop();
        }
        if (applicationServer != null)
        {
            applicationServer.stop();
        }
    }

    public OpenBIS createOpenBIS()
    {
        return createOpenBIS(30000);
    }

    public OpenBIS createOpenBIS(int timeout)
    {
        return new OpenBIS(TestInstanceHostUtils.getOpenBISUrl() + TestInstanceHostUtils.getOpenBISPath(),
                TestInstanceHostUtils.getDSSUrl() + TestInstanceHostUtils.getDSSPath(),
                TestInstanceHostUtils.getAFSUrl() + TestInstanceHostUtils.getAFSPath(), timeout);
    }

    private void dropOpenBISDatabase()
    {
        if (applicationServer != null)
        {
            Properties properties = applicationServer.getServiceProperties();

            Properties databaseProperties = ExtendedProperties.getSubset(properties, "database.", true);

            databaseProperties.setProperty(DatabaseConfiguration.NAME, "openbis");
            databaseProperties.setProperty(DatabaseConfiguration.VERSION_HOLDER_CLASS,
                    ApplicationServer.OpenBISDatabaseVersionHolder.class.getName());
            databaseProperties.setProperty(DatabaseConfiguration.SCRIPT_FOLDER, properties.getProperty(DatabaseConfiguration.SCRIPT_FOLDER));

            DatabaseConfiguration configuration = new DatabaseConfiguration(databaseProperties);
            dropDatabase(configuration.getContext());
            log.info("Dropped openBIS database.");
        }
    }

    private void dropMessagesDatabase()
    {
        if (applicationServer != null)
        {
            Properties properties = applicationServer.getServiceProperties();

            Properties databaseProperties = ExtendedProperties.getSubset(properties, "messages-database.", true);

            databaseProperties.setProperty(DatabaseConfiguration.NAME, "messages");
            databaseProperties.setProperty(DatabaseConfiguration.VERSION_HOLDER_CLASS,
                    ApplicationServer.MessagesDatabaseVersionHolder.class.getName());
            databaseProperties.setProperty(DatabaseConfiguration.SCRIPT_FOLDER, databaseProperties.getProperty(DatabaseConfiguration.SCRIPT_FOLDER));

            DatabaseConfiguration configuration = new DatabaseConfiguration(databaseProperties);
            dropDatabase(configuration.getContext());
            log.info("Dropped messages database configured at AS.");
        }
        if (afsServer != null)
        {
            Configuration afsConfiguration = new Configuration(afsServer.getServiceProperties());
            MessagesDatabaseConfiguration configuration = MessagesDatabaseConfiguration.getInstance(afsConfiguration);
            if (configuration != null)
            {
                dropDatabase(configuration.getContext());
                log.info("Dropped messages database configured at AFS.");
            }
        }
    }

    private void dropPathInfoDatabase()
    {
        if (dataStoreServer != null)
        {
            Properties properties = ExtendedProperties.getSubset(dataStoreServer.getServiceProperties(), "path-info-db.", true);
            if (!properties.isEmpty())
            {
                DatabaseConfigurationContext context =
                        BeanUtils.createBean(DatabaseConfigurationContext.class, properties);
                dropDatabase(context);
                log.info("Dropped path info database configured at DSS.");
            }
        }
        if (afsServer != null)
        {
            Configuration afsConfiguration = new Configuration(afsServer.getServiceProperties());
            PathInfoDatabaseConfiguration configuration = PathInfoDatabaseConfiguration.getInstance(afsConfiguration);
            if (configuration != null)
            {
                dropDatabase(configuration.getContext());
                log.info("Dropped path info database configured at AFS.");
            }
        }
    }

    private void dropArchiverDatabase()
    {
        if (dataStoreServer != null)
        {
            Properties properties = ExtendedProperties.getSubset(dataStoreServer.getServiceProperties(), "multi-dataset-archiver-db.", true);
            if (!properties.isEmpty())
            {
                DatabaseConfigurationContext context =
                        BeanUtils.createBean(DatabaseConfigurationContext.class, properties);
                dropDatabase(context);
                log.info("Dropped archiver database configured at DSS.");
            }
        }
        if (afsServer != null)
        {
            Configuration afsConfiguration = new Configuration(afsServer.getServiceProperties());
            ArchiverDatabaseConfiguration configuration = ArchiverDatabaseConfiguration.getInstance(afsConfiguration);
            if (configuration != null)
            {
                dropDatabase(configuration.getContext());
                log.info("Dropped archiver database configured at AFS.");
            }
        }
    }

    private void dropDatabase(DatabaseConfigurationContext context)
    {
        try
        {
            SQLUtils.execute(context.getAdminDataSource(), "drop database " + context.getDatabaseName(), new SQLUtils.NoParametersSetter());
        } catch (SQLException e)
        {
            if (!DBUtilities.isDBNotExistException(e))
            {
                throw new RuntimeException(e);
            }
        }
    }

    private void cleanupApplicationServerFolders()
    {
        if (applicationServer != null)
        {
            Properties configuration = applicationServer.getServiceProperties();
            String transactionLogFolder = configuration.getProperty(TransactionConfiguration.TRANSACTION_LOG_FOLDER_PATH_PROPERTY_NAME);
            cleanupFolderSafely(transactionLogFolder);
        }
    }

    private void cleanupAfsServerFolders()
    {
        if (afsServer != null)
        {
            Configuration configuration = new Configuration(afsServer.getServiceProperties());

            String writeAheadLogFolder = configuration.getStringProperty(AtomicFileSystemServerParameter.writeAheadLogRoot);
            cleanupFolderSafely(writeAheadLogFolder);

            String storageRoot = configuration.getStringProperty(AtomicFileSystemServerParameter.storageRoot);
            cleanupFolderSafely(storageRoot);

            String storageIncomingShareId = configuration.getStringProperty(AtomicFileSystemServerParameter.storageIncomingShareId);

            new File(storageRoot, storageIncomingShareId).mkdirs();
        }
    }

    private void cleanupDataStoreServerFolders()
    {
        if (dataStoreServer != null)
        {
            ExtendedProperties properties = ExtendedProperties.createWith(dataStoreServer.getServiceProperties());
            cleanupFolderSafely(properties.getProperty(DssPropertyParameters.STOREROOT_DIR_KEY));
        }
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

    private void configureSystemUser()
    {
        if (applicationServer != null)
        {
            OpenBIS openBIS = createOpenBIS();
            openBIS.login(INSTANCE_ADMIN_USER, PASSWORD);

            RoleAssignmentCreation roleCreation = new RoleAssignmentCreation();
            roleCreation.setUserId(new PersonPermId(SYSTEM_USER));
            roleCreation.setRole(Role.ADMIN);

            openBIS.createRoleAssignments(List.of(roleCreation));

            log.info("Configured system user.");
        }
    }

    private void configureShares()
    {
        if (dataStoreServer != null || afsServer != null)
        {
            Path dataStoreStoreRoot = null;
            Path afsStoreRoot = null;

            if (dataStoreServer != null)
            {
                dataStoreStoreRoot = Path.of(ExtendedProperties.createWith(dataStoreServer.getServiceProperties()).getProperty("storeroot-dir"));
            }

            if (afsServer != null)
            {
                Configuration configuration = new Configuration(afsServer.getServiceProperties());
                afsStoreRoot = Path.of(configuration.getStringProperty(AtomicFileSystemServerParameter.storageRoot));
            }

            if (dataStoreStoreRoot != null && afsStoreRoot != null && !Objects.equals(dataStoreStoreRoot.toAbsolutePath().normalize().toString(),
                    afsStoreRoot.toAbsolutePath().normalize().toString()))
            {
                throw new RuntimeException(
                        "Store roots of DSS and AFS are inconsistent. DSS uses: " + dataStoreStoreRoot + " while AFS uses: " + afsStoreRoot
                                + ". Both should use the same store root.");
            }

            try
            {
                Path storeRoot = dataStoreStoreRoot != null ? dataStoreStoreRoot : afsStoreRoot;

                for (Share share : shares)
                {
                    File shareFolder = new File(storeRoot.toFile(), String.valueOf(share.getShareNumber()));
                    shareFolder.mkdirs();
                    share.getShareProperties().store(new FileOutputStream(new File(shareFolder, "share.properties")), null);
                }

                log.info("Configured shares.");
            } catch (Exception e)
            {
                throw new RuntimeException("Configuring shares failed.", e);
            }
        }
    }

    private void configureELN()
    {
        if (applicationServer != null)
        {
            SoftLinkMaker.createSymbolicLink(new File("../ui-eln-lims/src/core-plugins/eln-lims"), new File("etc/default/as/core-plugins/eln-lims"));
            SoftLinkMaker.createSymbolicLink(new File("../ui-admin/src/core-plugins/admin"), new File("etc/default/as/core-plugins/admin"));
            log.info("Configured ELN for AS.");
        }
        if (dataStoreServer != null)
        {
            SoftLinkMaker.createSymbolicLink(new File("../ui-eln-lims/src/core-plugins/eln-lims"), new File("etc/default/dss/core-plugins/eln-lims"));
            SoftLinkMaker.createSymbolicLink(new File("../ui-admin/src/core-plugins/admin"), new File("etc/default/dss/core-plugins/admin"));
            log.info("Configured ELN for DSS.");
        }
    }

    private void configureELNSettings()
    {
        if (applicationServer != null)
        {
            OpenBIS openBIS = createOpenBIS();
            openBIS.login(INSTANCE_ADMIN_USER, PASSWORD);

            SampleUpdate elnSettingsUpdate = new SampleUpdate();
            elnSettingsUpdate.setSampleId(new SampleIdentifier("/ELN_SETTINGS/GENERAL_ELN_SETTINGS"));
            elnSettingsUpdate.setProperties(Map.of("ELN_SETTINGS", FileUtilities.loadToString(new File("etc/default/as/eln-settings.json"))));

            openBIS.updateSamples(List.of(elnSettingsUpdate));
            log.info("Configured ELN settings.");
        }
    }

    public static Properties loadProperties(final Path propertiesPath)
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