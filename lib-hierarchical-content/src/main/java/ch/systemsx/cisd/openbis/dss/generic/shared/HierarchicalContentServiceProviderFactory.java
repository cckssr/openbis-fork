package ch.systemsx.cisd.openbis.dss.generic.shared;

public class HierarchicalContentServiceProviderFactory
{

    private static IHierarchicalContentServiceProvider instance;

    public static IHierarchicalContentServiceProvider getInstance()
    {
        return HierarchicalContentServiceProviderFactory.instance;
    }

    public static void setInstance(final IHierarchicalContentServiceProvider instance)
    {
        HierarchicalContentServiceProviderFactory.instance = instance;
    }

}
