package ch.ethz.sis.rdf.main;

public class Config
{

    private static Config INSTANCE;

    private final boolean removeDanglingReferences;

    private Config(boolean removeDanglingReferences)
    {
        this.removeDanglingReferences = removeDanglingReferences;
    }

    public static void setConfig(boolean removeDanglingReferences)
    {
        if (INSTANCE != null)
        {
            throw new IllegalStateException("Config has already been set, something's wrong!");
        }

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
