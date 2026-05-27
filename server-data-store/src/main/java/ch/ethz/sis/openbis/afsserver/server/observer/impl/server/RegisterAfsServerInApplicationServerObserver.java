package ch.ethz.sis.openbis.afsserver.server.observer.impl.server;

import ch.ethz.sis.afs.manager.TransactionConnection;
import ch.ethz.sis.afsserver.server.APIServer;
import ch.ethz.sis.afsserver.server.observer.ServerObserver;
import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameter;
import ch.ethz.sis.openbis.afsserver.server.common.IOpenBISFacade;
import ch.ethz.sis.openbis.afsserver.server.common.OpenBISConfiguration;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.create.DataStoreCreation;
import ch.ethz.sis.shared.log.standard.LogManager;
import ch.ethz.sis.shared.log.standard.Logger;
import ch.ethz.sis.shared.startup.Configuration;
import ch.systemsx.cisd.openbis.generic.shared.Constants;

public class RegisterAfsServerInApplicationServerObserver implements ServerObserver<TransactionConnection>
{

    private static final Logger logger = LogManager.getLogger(RegisterAfsServerInApplicationServerObserver.class);

    private Configuration configuration;

    @Override public void init(final APIServer<TransactionConnection, ?, ?, ?> apiServer, final Configuration configuration) throws Exception
    {
        this.configuration = configuration;
    }

    @Override public void beforeStartup() throws Exception
    {
        OpenBISConfiguration openBISConfiguration = OpenBISConfiguration.getInstance(configuration);
        IOpenBISFacade openBISFacade = openBISConfiguration.getOpenBISFacade();

        DataStoreCreation creation = new DataStoreCreation();
        creation.setCode(Constants.AFS_DATA_STORE_CODE);
        creation.setDownloadUrl(configuration.getStringProperty(AtomicFileSystemServerParameter.httpServerPublicUrl));
        creation.setStorageUuid(openBISConfiguration.getStorageUuid());

        openBISFacade.createDataStore(creation);

        logger.info("Registered AFS server as data store in AS server.");
    }

    @Override public void beforeShutdown() throws Exception
    {

    }
}
