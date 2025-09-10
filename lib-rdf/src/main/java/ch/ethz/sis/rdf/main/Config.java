package ch.ethz.sis.rdf.main;

public class Config
{

    private static Config INSTANCE;

    private final boolean removeDanglingReferences;

    private final boolean writeSchema;

    private final boolean enforceSingleValues;

    public Config(boolean removeDanglingReferences, boolean writeSchema,
            boolean enforceSingleValues)
    {
        this.removeDanglingReferences = removeDanglingReferences;
        this.writeSchema = writeSchema;
        this.enforceSingleValues = enforceSingleValues;
    }

    public boolean isRemoveDanglingReferences()
    {
        return removeDanglingReferences;
    }

    public boolean isEnforceSingleValues()
    {
        return enforceSingleValues;
    }

    public boolean isWriteSchema()
    {
        return writeSchema;
    }

    public static void setConfig(boolean removeDanglingReferences, boolean writeSchema,
            boolean enforceSingleValues)
    {
        INSTANCE = new Config(removeDanglingReferences, writeSchema, enforceSingleValues);

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
