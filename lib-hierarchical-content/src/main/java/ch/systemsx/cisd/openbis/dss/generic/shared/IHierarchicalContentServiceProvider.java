package ch.systemsx.cisd.openbis.dss.generic.shared;

import ch.systemsx.cisd.openbis.dss.generic.shared.api.v1.IDssServiceFactory;
import ch.systemsx.cisd.openbis.dss.generic.shared.content.IContentCache;

public interface IHierarchicalContentServiceProvider
{

    IDssServiceFactory getDssServiceFactory();

    IDataSetPathInfoProvider getDataSetPathInfoProvider();

    IPathInfoDataSourceProvider getPathInfoDataSourceProvider();

    IOpenBISService getOpenBISService();

    IShareIdManager getShareIdManager();

    IConfigProvider getConfigProvider();

    IContentCache getContentCache();
}
