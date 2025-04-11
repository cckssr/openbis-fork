package ch.systemsx.cisd.openbis.dss.generic.shared;

import javax.sql.DataSource;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.systemsx.cisd.etlserver.IPathInfoServiceProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.utils.PathInfoDataSourceProvider;

public class PathInfoServiceProvider implements IPathInfoServiceProvider
{
    @Override public IApplicationServerApi getV3ApplicationService()
    {
        return ServiceProvider.getV3ApplicationService();
    }

    @Override public IDataSetDirectoryProvider getDataSetDirectoryProvider()
    {
        return ServiceProvider.getDataStoreService().getDataSetDirectoryProvider();
    }

    @Override public IOpenBISService getOpenBISService()
    {
        return ServiceProvider.getOpenBISService();
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

    @Override public IHierarchicalContentProvider getHierarchicalContentProvider()
    {
        return ServiceProvider.getHierarchicalContentProvider();
    }

    @Override public IConfigProvider getConfigProvider()
    {
        return ServiceProvider.getConfigProvider();
    }
}
