package ch.systemsx.cisd.openbis.dss.generic.shared;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;

public interface IPathInfoServiceProvider
{
    IApplicationServerApi getV3ApplicationService();

    IDataSetDirectoryProvider getDataSetDirectoryProvider();

    IOpenBISService getOpenBISService();

    IPathInfoDataSourceProvider getPathInfoDataSourceProvider();

    IHierarchicalContentProvider getHierarchicalContentProvider();

    IConfigProvider getConfigProvider();
}
