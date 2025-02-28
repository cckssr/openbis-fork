package ch.systemsx.cisd.openbis.dss.generic.shared;

import ch.systemsx.cisd.openbis.dss.generic.shared.content.IContentCache;

public interface IServiceProvider
{
    IOpenBISService getOpenBISService();

    IConfigProvider getConfigProvider();

    IShareIdManager getShareIdManager();

    IContentCache getContentCache();

    IDataSetPathInfoProvider getDataSetPathInfoProvider();

    IPathInfoDataSourceProvider getPathInfoDataSourceProvider();

}
