package ch.systemsx.cisd.openbis.generic.server;

import ch.ethz.sis.shared.log.classic.impl.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.IDAOFactory;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.IDataStoreDAO;
import ch.systemsx.cisd.openbis.generic.shared.Constants;
import ch.systemsx.cisd.openbis.generic.shared.dto.DataSetTypePE;
import ch.systemsx.cisd.openbis.generic.shared.dto.DataStorePE;

@Component
public class AfsRegistrator implements IAfsRegistrator, ApplicationListener<ApplicationEvent>
{

    private final Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION, AfsRegistrator.class);

    private static final String AFS_DATA_SET_TYPE_CODE = "AFS_DATA";

    @Autowired
    private IDAOFactory daoFactory;

    @Autowired IAfsRegistrator self;

    private boolean registered;

    @Override public void onApplicationEvent(final ApplicationEvent event)
    {
        Object source = event.getSource();
        if (source instanceof AbstractApplicationContext)
        {
            AbstractApplicationContext appContext = (AbstractApplicationContext) source;
            if ((event instanceof ContextStartedEvent) || (event instanceof ContextRefreshedEvent))
            {
                // call the bean with transaction support
                self.registerAfs();
            }
        }
    }

    @Transactional
    public void registerAfs()
    {
        synchronized (this)
        {
            if (registered)
            {
                return;
            }
            registered = true;
        }

        IDataStoreDAO dataStoreDAO = daoFactory.getDataStoreDAO();

        DataStorePE existingDataStore = dataStoreDAO.tryToFindDataStoreByCode(Constants.AFS_DATA_STORE_CODE);

        if (existingDataStore == null)
        {
            DataStorePE dataStore = new DataStorePE();
            dataStore.setCode(Constants.AFS_DATA_STORE_CODE);
            dataStore.setDownloadUrl("");
            dataStore.setRemoteUrl("");
            dataStore.setDatabaseInstanceUUID("");
            dataStore.setSessionToken("");
            dataStore.setArchiverConfigured(false);

            operationLog.info("Registering AFS server in the data stores table.");

            dataStoreDAO.createOrUpdateDataStore(dataStore);
        } else
        {
            operationLog.info("AFS server has been already registered in the data stores table before. Nothing to do.");
        }

        DataSetTypePE existingAfsDataSetType = daoFactory.getDataSetTypeDAO().tryToFindDataSetTypeByCode("AFS_DATA");

        if (existingAfsDataSetType == null)
        {
            DataSetTypePE afsDataSetType = new DataSetTypePE();
            afsDataSetType.setCode(AFS_DATA_SET_TYPE_CODE);
            afsDataSetType.setManagedInternally(true);
            daoFactory.getDataSetTypeDAO().persist(afsDataSetType);

            operationLog.info("Registering AFS data set type " + AFS_DATA_SET_TYPE_CODE + ".");
        } else
        {
            operationLog.info("AFS data set type has been already registered before. Nothing to do.");
        }
    }

}
