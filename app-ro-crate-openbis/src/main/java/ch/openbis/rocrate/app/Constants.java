package ch.openbis.rocrate.app;

import java.util.Set;

public class Constants
{
    public static final String GRAPH_ID_SPACE = "openBIS:Space";

    public static final String GRAPH_ID_PROJECT = "openBIS:Project";

    public static final String GRAPH_ID_Collection = "openBIS:Collection";

    public static final String GRAPH_ID_DATASET = "openBIS:Dataset";

    public static final String GRAPH_ID_OBJECT = "openBIS:Object";

    public static final String GRAPH_ID_VOCABULARY = "openBIS:Vocabulary";


    public static final String PROPERTY_SPACE = "openBIS:hasSPACE";

    public static final String PROPERTY_PROJECT = "openBIS:hasPROJECT";

    public static final String PROPERTY_COLLECTION = "openBIS:hasCOLLECTION";

    public static final String PROPERTY_ID_FILES = "schema:hasPart";

    public static final String PROPERTY_ID_PARENTS = "openBIS:parents";

    public static final String EQUIVALENCE_PARENT = "https://schema.org/isBasedOn";

    public final static Set<String> FILE_TYPES = Set.of("File", "MediaObject");



}
