package ch.systemsx.cisd.openbis.generic.server;

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
import ch.ethz.sis.shared.log.classic.impl.Logger;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.IDAOFactory;
import ch.systemsx.cisd.openbis.generic.shared.Constants;
import ch.systemsx.cisd.openbis.generic.shared.dto.DataSetTypePE;

@Component
public class CoreDataRegistrator implements ICoreDataRegistrator, ApplicationListener<ApplicationEvent>
{

    private final Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION, CoreDataRegistrator.class);

    @Autowired
    private IDAOFactory daoFactory;

    @Autowired ICoreDataRegistrator self;

    private boolean registered;

    @Override public void onApplicationEvent(final ApplicationEvent event)
    {
        Object source = event.getSource();
        if (source instanceof AbstractApplicationContext)
        {
            if ((event instanceof ContextStartedEvent) || (event instanceof ContextRefreshedEvent))
            {
                // call the bean with transaction support
                self.register();
            }
        }
    }

    @Transactional
    public void register()
    {
        synchronized (this)
        {
            if (registered)
            {
                return;
            }
            registered = true;
        }

        DataSetTypePE existingFilesDataSetType = daoFactory.getDataSetTypeDAO().tryToFindDataSetTypeByCode(Constants.FILES_DATA_SET_TYPE_CODE);

        if (existingFilesDataSetType == null)
        {
            DataSetTypePE filesDataSetType = new DataSetTypePE();
            filesDataSetType.setCode(Constants.FILES_DATA_SET_TYPE_CODE);
            filesDataSetType.setManagedInternally(true);
            daoFactory.getDataSetTypeDAO().persist(filesDataSetType);

            operationLog.info("Registering data set type '" + Constants.FILES_DATA_SET_TYPE_CODE + "'.");
        } else
        {
            operationLog.info("'" + Constants.FILES_DATA_SET_TYPE_CODE + "' data set type has been already registered before. Nothing to do.");
        }
    }

}
