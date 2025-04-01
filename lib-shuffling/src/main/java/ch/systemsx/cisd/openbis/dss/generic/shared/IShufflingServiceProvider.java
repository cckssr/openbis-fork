package ch.systemsx.cisd.openbis.dss.generic.shared;

public interface IShufflingServiceProvider
{
    IOpenBISService getOpenBISService();

    IShareIdManager getShareIdManager();

    IConfigProvider getConfigProvider();

    IHierarchicalContentProvider getHierarchicalContentProvider();
}
