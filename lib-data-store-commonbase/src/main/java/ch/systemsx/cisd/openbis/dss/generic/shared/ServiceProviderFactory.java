package ch.systemsx.cisd.openbis.dss.generic.shared;

public class ServiceProviderFactory
{

    private static IServiceProvider instance;

    public static IServiceProvider getInstance()
    {
        return ServiceProviderFactory.instance;
    }

    public static void setInstance(final IServiceProvider instance)
    {
        ServiceProviderFactory.instance = instance;
    }
}
