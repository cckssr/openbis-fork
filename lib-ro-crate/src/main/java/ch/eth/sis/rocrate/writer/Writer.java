package ch.eth.sis.rocrate.writer;

import ch.eth.sis.rocrate.parser.results.ParseResult;
import ch.eth.sis.rocrate.writer.mapping.Mapper;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.entities.data.DataEntity;

import java.nio.file.Path;

public class Writer
{
    public static final String PREFIX_SYSTEM = "openbis-system";

    public static final String PREFIX_SCHEMA = "openbis-schema";

    public static final String PREFIX_METADATA = "openbis-metadata";

    private static final String TYPE = "@type";

    public static final String NAMESPACE_SEPARATOR = ":";

    public static final String SYSTEM_SPACE = PREFIX_SYSTEM + NAMESPACE_SEPARATOR + "Space";

    public static final String SYSTEM_OBJECT = PREFIX_SYSTEM + NAMESPACE_SEPARATOR + "Object";

    public static final String SYSTEM_COLLECTION =
            PREFIX_SYSTEM + NAMESPACE_SEPARATOR + "Collection";

    public static final String SYSTEM_PROJECT = PREFIX_SYSTEM + NAMESPACE_SEPARATOR + "Project";

    public static final String SYSTEM_DATASET = PREFIX_SYSTEM + NAMESPACE_SEPARATOR + "Dataset";

    public static final String SYSTEM_VOCABULARY =
            PREFIX_SYSTEM + NAMESPACE_SEPARATOR + "Vocabulary";

    private final static String RDFS_CLASS = "rdfs:Class";

    private final static String RDFS_PROPERTY = "rdfs:Property";

    public void write(ParseResult parseResult, Path outPath)
    {
        //RoCrate.RoCrateBuilder builder = new RoCrate.RoCrateBuilder("name", "description", "2024-12-04T07:53:11Z", "licenseIdentifier");
        //addSystemSchema(builder);
        Mapper mapper = new Mapper();
        var rdfsRepresentation =
                mapper.transform(parseResult);

    }

    private void addSystemSchema(RoCrate.RoCrateBuilder crateBuilder)
    {

        {
            DataEntity.DataEntityBuilder builder = new DataEntity.DataEntityBuilder();
            builder.setId(SYSTEM_SPACE);
            builder.addProperty(RDFS_CLASS, TYPE);
            crateBuilder.addDataEntity(builder.build());
        }
        {
            DataEntity.DataEntityBuilder builder = new DataEntity.DataEntityBuilder();
            builder.setId(SYSTEM_OBJECT);
            builder.addProperty(RDFS_CLASS, TYPE);
            crateBuilder.addDataEntity(builder.build());
        }
        {
            DataEntity.DataEntityBuilder builder = new DataEntity.DataEntityBuilder();
            builder.setId(SYSTEM_COLLECTION);
            builder.addProperty(RDFS_CLASS, TYPE);
            crateBuilder.addDataEntity(builder.build());
        }
        {
            DataEntity.DataEntityBuilder builder = new DataEntity.DataEntityBuilder();
            builder.setId(SYSTEM_PROJECT);
            builder.addProperty(RDFS_CLASS, TYPE);
            crateBuilder.addDataEntity(builder.build());
        }
        {
            DataEntity.DataEntityBuilder builder = new DataEntity.DataEntityBuilder();
            builder.setId(SYSTEM_DATASET);
            builder.addProperty(RDFS_CLASS, TYPE);
            crateBuilder.addDataEntity(builder.build());
        }
        {
            DataEntity.DataEntityBuilder builder = new DataEntity.DataEntityBuilder();
            builder.setId(SYSTEM_VOCABULARY);
            builder.addProperty(RDFS_CLASS, TYPE);
            crateBuilder.addDataEntity(builder.build());
        }

    }

}
