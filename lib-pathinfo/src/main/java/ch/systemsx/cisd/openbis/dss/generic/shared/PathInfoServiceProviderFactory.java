package ch.systemsx.cisd.openbis.dss.generic.shared;

public class PathInfoServiceProviderFactory
{

    private static IPathInfoServiceProvider instance;

    public static IPathInfoServiceProvider getInstance()
    {
        return PathInfoServiceProviderFactory.instance;
    }

    public static void setInstance(final IPathInfoServiceProvider instance)
    {
        PathInfoServiceProviderFactory.instance = instance;
    }

}
