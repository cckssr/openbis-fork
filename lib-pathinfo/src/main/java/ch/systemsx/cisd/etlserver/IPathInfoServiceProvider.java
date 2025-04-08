package ch.systemsx.cisd.etlserver;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.systemsx.cisd.openbis.dss.generic.shared.IConfigProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IDataSetDirectoryProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IHierarchicalContentProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IOpenBISService;
import ch.systemsx.cisd.openbis.dss.generic.shared.IPathInfoDataSourceProvider;

public interface IPathInfoServiceProvider
{
    IApplicationServerApi getV3ApplicationService();

    IDataSetDirectoryProvider getDataSetDirectoryProvider();

    IOpenBISService getOpenBISService();

    IPathInfoDataSourceProvider getPathInfoDataSourceProvider();

    IHierarchicalContentProvider getHierarchicalContentProvider();

    IConfigProvider getConfigProvider();
}
