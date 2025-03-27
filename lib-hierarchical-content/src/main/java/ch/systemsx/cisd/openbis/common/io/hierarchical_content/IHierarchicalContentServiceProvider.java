package ch.systemsx.cisd.openbis.common.io.hierarchical_content;

import ch.systemsx.cisd.openbis.dss.generic.shared.IConfigProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IDataSetPathInfoProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IOpenBISService;
import ch.systemsx.cisd.openbis.dss.generic.shared.IPathInfoDataSourceProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IShareIdManager;
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
