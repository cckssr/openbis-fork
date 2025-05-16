package ch.systemsx.cisd.etlserver;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.systemsx.cisd.openbis.dss.generic.shared.IConfigProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IDataSetDirectoryProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IHierarchicalContentProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IOpenBISService;
import ch.systemsx.cisd.openbis.dss.generic.shared.IPathInfoDataSourceProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IPathInfoServiceProvider;

public class PathInfoServiceProviderAdapter implements IPathInfoServiceProvider
{
    @Override public IApplicationServerApi getV3ApplicationService()
    {
        throw new UnsupportedOperationException();
    }

    @Override public IDataSetDirectoryProvider getDataSetDirectoryProvider()
    {
        throw new UnsupportedOperationException();
    }

    @Override public IOpenBISService getOpenBISService()
    {
        throw new UnsupportedOperationException();
    }

    @Override public IPathInfoDataSourceProvider getPathInfoDataSourceProvider()
    {
        throw new UnsupportedOperationException();
    }

    @Override public IHierarchicalContentProvider getHierarchicalContentProvider()
    {
        throw new UnsupportedOperationException();
    }

    @Override public IConfigProvider getConfigProvider()
    {
        throw new UnsupportedOperationException();
    }
}
