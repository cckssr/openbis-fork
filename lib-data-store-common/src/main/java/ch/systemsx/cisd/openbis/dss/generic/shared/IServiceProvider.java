package ch.systemsx.cisd.openbis.dss.generic.shared;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.systemsx.cisd.openbis.dss.generic.shared.api.v1.IDssServiceFactory;
import ch.systemsx.cisd.openbis.dss.generic.shared.content.IContentCache;

public interface IServiceProvider
{
    IOpenBISService getOpenBISService();

    IConfigProvider getConfigProvider();

    IShareIdManager getShareIdManager();

    IContentCache getContentCache();

    IDataSetDirectoryProvider getDataSetDirectoryProvider();

    IDataSetPathInfoProvider getDataSetPathInfoProvider();

    IDataSetDeleter getDataSetDeleter();

    IPathInfoDataSourceProvider getPathInfoDataSourceProvider();

    IDssServiceFactory getDssServiceFactory();

    IApplicationServerApi getV3ApplicationService();
}
