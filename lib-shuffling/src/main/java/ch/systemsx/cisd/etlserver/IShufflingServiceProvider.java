package ch.systemsx.cisd.etlserver;

import ch.systemsx.cisd.openbis.dss.generic.shared.IConfigProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IHierarchicalContentProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IOpenBISService;
import ch.systemsx.cisd.openbis.dss.generic.shared.IShareIdManager;

public interface IShufflingServiceProvider
{
    IOpenBISService getOpenBISService();

    IShareIdManager getShareIdManager();

    IConfigProvider getConfigProvider();

    IHierarchicalContentProvider getHierarchicalContentProvider();
}
