package ch.systemsx.cisd.etlserver;

public class ShufflingServiceProviderFactory
{

    private static IShufflingServiceProvider instance;

    public static IShufflingServiceProvider getInstance()
    {
        return ShufflingServiceProviderFactory.instance;
    }

    public static void setInstance(final IShufflingServiceProvider instance)
    {
        ShufflingServiceProviderFactory.instance = instance;
    }

}
