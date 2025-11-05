package ch.ethz.sis.openbis.afsserver.server.common;

import ch.systemsx.cisd.openbis.common.io.hierarchical_content.IHierarchicalContentNodeFilter;
import ch.systemsx.cisd.openbis.dss.generic.shared.IConfigProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IDataSetPathInfoProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IHierarchicalContentServiceProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IOpenBISService;
import ch.systemsx.cisd.openbis.dss.generic.shared.IPathInfoDataSourceProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IShareIdManager;
import ch.systemsx.cisd.openbis.dss.generic.shared.api.v1.IDssServiceFactory;
import ch.systemsx.cisd.openbis.dss.generic.shared.content.IContentCache;

public class HierarchicalContentServiceProvider implements IHierarchicalContentServiceProvider
{

    private final ServiceProvider serviceProvider;

    public HierarchicalContentServiceProvider(ServiceProvider serviceProvider)
    {
        this.serviceProvider = serviceProvider;
    }

    @Override public IDssServiceFactory getDssServiceFactory()
    {
        return serviceProvider.getDssServiceFactory();
    }

    @Override public IDataSetPathInfoProvider getDataSetPathInfoProvider()
    {
        return serviceProvider.getDataSetPathInfoProvider();
    }

    @Override public IPathInfoDataSourceProvider getPathInfoDataSourceProvider()
    {
        return serviceProvider.getPathInfoDataSourceProvider();
    }

    @Override public IHierarchicalContentNodeFilter getHierarchicalContentNodeFilter()
    {
        return serviceProvider.getHierarchicalContentNodeFilter();
    }

    @Override public IOpenBISService getOpenBISService()
    {
        return serviceProvider.getOpenBISService();
    }

    @Override public IShareIdManager getShareIdManager()
    {
        return serviceProvider.getShareIdManager();
    }

    @Override public IConfigProvider getConfigProvider()
    {
        return serviceProvider.getConfigProvider();
    }

    @Override public IContentCache getContentCache()
    {
        return serviceProvider.getContentCache();
    }
}
