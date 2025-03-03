package ch.systemsx.cisd.openbis.dss.generic.shared;

public class ArchiverServiceProviderFactory
{

    private static IArchiverServiceProvider instance;

    public static IArchiverServiceProvider getInstance()
    {
        return ArchiverServiceProviderFactory.instance;
    }

    public static void setInstance(final IArchiverServiceProvider instance)
    {
        ArchiverServiceProviderFactory.instance = instance;
    }

}
