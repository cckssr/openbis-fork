package ch.ethz.sis.rdf.main;

public class Config
{

    private static Config INSTANCE;

    private final boolean removeDanglingReferences;

    public Config(boolean removeDanglingReferences)
    {
        this.removeDanglingReferences = removeDanglingReferences;
    }

    public static void setConfig(boolean removeDanglingReferences)
    {
        INSTANCE = new Config(removeDanglingReferences);

    }

    public static Config getINSTANCE()
    {
        return INSTANCE;
    }

    public boolean removeDanglingReferences()
    {
        return removeDanglingReferences;
    }
}
