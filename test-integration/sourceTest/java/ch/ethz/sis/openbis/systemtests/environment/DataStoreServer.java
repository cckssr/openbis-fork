package ch.ethz.sis.openbis.systemtests.environment;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;

import ch.ethz.sis.afs.manager.TransactionConnection;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.ethz.sis.shared.log.classic.impl.Logger;
import ch.systemsx.cisd.common.filesystem.QueueingPathRemoverService;
import ch.systemsx.cisd.etlserver.ETLDaemon;
import ch.systemsx.cisd.openbis.dss.generic.shared.ArchiverServiceProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.ArchiverServiceProviderFactory;
import ch.systemsx.cisd.openbis.dss.generic.shared.HierarchicalContentServiceProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.HierarchicalContentServiceProviderFactory;
import ch.systemsx.cisd.openbis.dss.generic.shared.ServiceProviderFactory;
import ch.systemsx.cisd.openbis.dss.generic.shared.ServiceProviderImpl;
import ch.systemsx.cisd.openbis.dss.generic.shared.ShufflingServiceProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.ShufflingServiceProviderFactory;

public class DataStoreServer implements Server<DataStoreServerConfiguration>
{

    private static final Logger log = LogFactory.getLogger(DataStoreServer.class);

    private DataStoreServerConfiguration configuration;

    private ch.ethz.sis.afsserver.server.Server<TransactionConnection, Object> server;

    @Override public void configure(final DataStoreServerConfiguration configuration)
    {
        this.configuration = configuration;
    }

    @Override public void start()
    {
        if (configuration == null)
        {
            throw new RuntimeException("Data store server hasn't been configured.");
        }

        try
        {
            log.info("Starting data store server.");
            Properties serviceProperties = configuration.getServiceProperties();
            serviceProperties.store(new FileOutputStream(new File("etc/service.properties")),
                    "This file has been generated. DSS has service.properties location hardcoded, without this file it won't start up");

            ServiceProviderFactory.setInstance(new ServiceProviderImpl());
            ArchiverServiceProviderFactory.setInstance(new ArchiverServiceProvider());
            ShufflingServiceProviderFactory.setInstance(new ShufflingServiceProvider());
            HierarchicalContentServiceProviderFactory.setInstance(new HierarchicalContentServiceProvider());
            QueueingPathRemoverService.start(new File(serviceProperties.getProperty("root-dir")), ETLDaemon.shredderQueueFile);
            ch.systemsx.cisd.openbis.dss.generic.server.DataStoreServer.main(new String[0]);
            log.info("Started data store server.");
        } catch (Exception e)
        {
            log.error("Starting data store server failed.", e);
            throw new RuntimeException(e);
        }
    }

    @Override public void stop()
    {
        try
        {
            ch.systemsx.cisd.openbis.dss.generic.server.DataStoreServer.stop();
            log.info("Stopped data store server.");
        } catch (Exception e)
        {
            log.error("Stopping data store server failed.", e);
            throw new RuntimeException(e);
        }
    }

    @Override public DataStoreServerConfiguration getConfiguration()
    {
        return configuration;
    }

    @Override public StringBuffer getLogs()
    {
        return null;
    }
}
