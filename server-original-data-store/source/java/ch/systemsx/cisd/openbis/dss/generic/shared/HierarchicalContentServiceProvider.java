package ch.systemsx.cisd.openbis.dss.generic.shared;

import javax.sql.DataSource;

import ch.systemsx.cisd.openbis.common.io.hierarchical_content.IHierarchicalContentServiceProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.api.v1.IDssService;
import ch.systemsx.cisd.openbis.dss.generic.shared.api.v1.IDssServiceFactory;
import ch.systemsx.cisd.openbis.dss.generic.shared.content.IContentCache;
import ch.systemsx.cisd.openbis.dss.generic.shared.utils.PathInfoDataSourceProvider;

public class HierarchicalContentServiceProvider implements IHierarchicalContentServiceProvider
{
    @Override public IDssServiceFactory getDssServiceFactory()
    {
        return new IDssServiceFactory()
        {
            @Override public IDssService getService(final String baseURL)
            {
                return (IDssService) ServiceProvider.getDssServiceRpcGeneric().getService();
            }
        };
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

    @Override public IOpenBISService getOpenBISService()
    {
        return ServiceProvider.getOpenBISService();
    }

    @Override public IShareIdManager getShareIdManager()
    {
        return ServiceProvider.getShareIdManager();
    }

    @Override public IConfigProvider getConfigProvider()
    {
        return ServiceProvider.getConfigProvider();
    }

    @Override public IContentCache getContentCache()
    {
        return ServiceProvider.getContentCache();
    }
}
