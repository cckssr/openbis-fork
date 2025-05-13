package ch.systemsx.cisd.openbis.dss.generic.shared;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.systemsx.cisd.common.mail.IMailClient;
import ch.systemsx.cisd.openbis.dss.generic.shared.dto.DataSetCodesWithStatus;
import ch.systemsx.cisd.openbis.dss.generic.shared.utils.PathInfoDataSourceProvider;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.DataSetArchivingStatus;
import ch.systemsx.cisd.openbis.generic.shared.dto.DatasetDescription;

public class ArchiverServiceProvider implements IArchiverServiceProvider
{
    @Override public IConfigProvider getConfigProvider()
    {
        return ServiceProvider.getConfigProvider();
    }

    @Override public IMailClient createEMailClient()
    {
        return ServiceProvider.getDataStoreService().createEMailClient();
    }

    @Override public IHierarchicalContentProvider getHierarchicalContentProvider()
    {
        return ServiceProvider.getHierarchicalContentProvider();
    }

    @Override public IDataSetDirectoryProvider getDataSetDirectoryProvider()
    {
        return ServiceProvider.getDataStoreService().getDataSetDirectoryProvider();
    }

    @Override public IDataSetPathInfoProvider getDataSetPathInfoProvider()
    {
        return ServiceProvider.getDataSetPathInfoProvider();
    }

    @Override public IPathInfoDataSourceProvider getPathInfoDataSourceProvider()
    {
        return new IPathInfoDataSourceProvider()
        {

            @Override public DataSource getDataSource()
            {
                return PathInfoDataSourceProvider.getDataSource();
            }

            @Override public boolean isDataSourceDefined()
            {
                return PathInfoDataSourceProvider.isDataSourceDefined();
            }
        };
    }

    @Override public IArchiverDataSourceProvider getArchiverDataSourceProvider()
    {
        return new IArchiverDataSourceProvider()
        {
            @Override public DataSource getDataSource()
            {
                return ServiceProvider.getDataSourceProvider().getDataSource("multi-dataset-archiver-db");
            }
        };
    }

    @Override public IDataSetDeleter getDataSetDeleter()
    {
        return ServiceProvider.getDataStoreService().getDataSetDeleter();
    }

    @Override public IDataSetStatusUpdater getDataSetStatusUpdater()
    {
        return new IDataSetStatusUpdater()
        {
            @Override
            public void scheduleUpdate(List<String> codes, DataSetArchivingStatus status,
                    Boolean present)
            {
                QueueingDataSetStatusUpdaterService.update(new DataSetCodesWithStatus(
                        codes, status, present));
            }
        };
    }

    @Override public IShareIdManager getShareIdManager()
    {
        return ServiceProvider.getShareIdManager();
    }

    @Override public IArchiverPlugin getArchiverPlugin()
    {
        return ServiceProvider.getDataStoreService().getArchiverPlugin();
    }

    @Override public IArchiverTaskScheduler getArchiverTaskScheduler()
    {
        return new IArchiverTaskScheduler()
        {
            @Override public void scheduleTask(final String taskKey, final IArchiverTask task, final Map<String, String> parameterBindings,
                    final List<DatasetDescription> datasets, final String userId, final String userEmailOrNull, final String userSessionToken)
            {
                ServiceProvider.getDataStoreService()
                        .scheduleTask(taskKey, new ArchiverProcessingPluginTask(task), parameterBindings, datasets, userId, userEmailOrNull,
                                userSessionToken);
            }
        };
    }

    @Override public Properties getArchiverProperties()
    {
        return ServiceProvider.getDataStoreService().getArchiverProperties();
    }

    @Override public IOpenBISService getOpenBISService()
    {
        return ServiceProvider.getOpenBISService();
    }

    @Override public IApplicationServerApi getV3ApplicationService()
    {
        return ServiceProvider.getV3ApplicationService();
    }

    @Override public IIncomingShareIdProvider getIncomingShareIdProvider()
    {
        return ServiceProvider.getIncomingShareIdProvider();
    }
}
