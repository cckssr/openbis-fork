package ch.ethz.sis.afsserver.server.pathinfo;

import ch.ethz.sis.afsserver.server.common.ServiceProvider;
import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.systemsx.cisd.etlserver.IPathInfoServiceProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IConfigProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IDataSetDirectoryProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IHierarchicalContentProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IOpenBISService;
import ch.systemsx.cisd.openbis.dss.generic.shared.IPathInfoDataSourceProvider;

public class PathInfoServiceProvider implements IPathInfoServiceProvider
{

    private final ServiceProvider serviceProvider;

    public PathInfoServiceProvider(ServiceProvider serviceProvider)
    {
        this.serviceProvider = serviceProvider;
    }

    @Override public IApplicationServerApi getV3ApplicationService()
    {
        return serviceProvider.getV3ApplicationService();
    }

    @Override public IDataSetDirectoryProvider getDataSetDirectoryProvider()
    {
        return serviceProvider.getDataSetDirectoryProvider();
    }

    @Override public IOpenBISService getOpenBISService()
    {
        return serviceProvider.getOpenBISService();
    }

    @Override public IPathInfoDataSourceProvider getPathInfoDataSourceProvider()
    {
        return serviceProvider.getPathInfoDataSourceProvider();
    }

    @Override public IHierarchicalContentProvider getHierarchicalContentProvider()
    {
        return serviceProvider.getHierarchicalContentProvider();
    }

    @Override public IConfigProvider getConfigProvider()
    {
        return serviceProvider.getConfigProvider();
    }
}
