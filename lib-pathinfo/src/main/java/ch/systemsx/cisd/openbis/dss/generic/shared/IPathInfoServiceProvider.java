package ch.systemsx.cisd.openbis.dss.generic.shared;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.systemsx.cisd.openbis.common.io.hierarchical_content.IHierarchicalContentNodeFilter;

public interface IPathInfoServiceProvider
{
    IApplicationServerApi getV3ApplicationService();

    IDataSetDirectoryProvider getDataSetDirectoryProvider();

    IOpenBISService getOpenBISService();

    IPathInfoDataSourceProvider getPathInfoDataSourceProvider();

    IHierarchicalContentProvider getHierarchicalContentProvider();

    IHierarchicalContentNodeFilter getHierarchicalContentNodeFilter();

    IConfigProvider getConfigProvider();
}
