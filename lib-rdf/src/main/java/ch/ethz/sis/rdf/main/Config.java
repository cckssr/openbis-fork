package ch.ethz.sis.rdf.main;

public class Config
{

    private static Config INSTANCE;

    private final boolean removeDanglingReferences;

    private final boolean writeSchema;

    public Config(boolean removeDanglingReferences, boolean writeSchema)
    {
        this.removeDanglingReferences = removeDanglingReferences;
        this.writeSchema = writeSchema;
    }

    public boolean isWriteSchema()
    {
        return writeSchema;
    }

    public static void setConfig(boolean removeDanglingReferences, boolean writeSchema)
    {
        INSTANCE = new Config(removeDanglingReferences, writeSchema);

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
