package ch.systemsx.cisd.openbis.dss.generic.shared;

import ch.systemsx.cisd.openbis.common.io.hierarchical_content.IHierarchicalContentNodeFilter;
import ch.systemsx.cisd.openbis.dss.generic.shared.api.v1.IDssServiceFactory;
import ch.systemsx.cisd.openbis.dss.generic.shared.content.IContentCache;

public interface IHierarchicalContentServiceProvider
{

    IDssServiceFactory getDssServiceFactory();

    IDataSetPathInfoProvider getDataSetPathInfoProvider();

    IPathInfoDataSourceProvider getPathInfoDataSourceProvider();

    IHierarchicalContentNodeFilter getHierarchicalContentNodeFilter();

    IOpenBISService getOpenBISService();

    IShareIdManager getShareIdManager();

    IConfigProvider getConfigProvider();

    IContentCache getContentCache();
}
