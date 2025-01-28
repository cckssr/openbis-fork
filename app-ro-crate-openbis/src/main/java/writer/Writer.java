package writer;

import ch.eth.sis.rocrate.SchemaFacade;
import ch.eth.sis.rocrate.facade.ISchemaFacade;
import ch.eth.sis.rocrate.facade.MetadataEntry;
import ch.eth.sis.rocrate.facade.RdfsClass;
import ch.eth.sis.rocrate.facade.TypeProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.writer.FolderWriter;
import parser.results.ParseResult;
import writer.mapping.Mapper;
import writer.mapping.types.MapResult;

import java.nio.file.Path;

public class Writer
{

    private static final String TYPE = "@type";

    public static final String NAMESPACE_SEPARATOR = ":";

    public static final String SYSTEM_SPACE = ":" + NAMESPACE_SEPARATOR + "Space";

    public static final String SYSTEM_OBJECT = ":" + NAMESPACE_SEPARATOR + "Object";

    public static final String SYSTEM_COLLECTION =
            ":" + NAMESPACE_SEPARATOR + "Collection";

    public static final String SYSTEM_PROJECT = ":" + NAMESPACE_SEPARATOR + "Project";

    public static final String SYSTEM_DATASET = ":" + NAMESPACE_SEPARATOR + "Dataset";

    public static final String SYSTEM_VOCABULARY =
            ":" + NAMESPACE_SEPARATOR + "Vocabulary";

    public void write(ParseResult parseResult, Path outPath) throws JsonProcessingException
    {
        RoCrate.RoCrateBuilder builder =
                new RoCrate.RoCrateBuilder("name", "description", "2024-12-04T07:53:11Z",
                        "licenseIdentifier");
        ISchemaFacade schemaFacade = new SchemaFacade(builder.build());
        addSystemSchema(schemaFacade);
        Mapper mapper = new Mapper();
        MapResult rdfsRepresentation =
                mapper.transform(parseResult);
        addSchema(schemaFacade, rdfsRepresentation);
        addMetaData(schemaFacade, rdfsRepresentation);
        RoCrate roCrate = builder.build();
        FolderWriter folderRoCrateWriter = new FolderWriter();
        folderRoCrateWriter.save(roCrate, outPath.toString());

    }

    private void addSystemSchema(ISchemaFacade facade)
    {

        {
            RdfsClass rdfsClass = new RdfsClass();
            rdfsClass.setId(SYSTEM_SPACE);
            facade.addType(rdfsClass);

        }
        {
            RdfsClass rdfsClass = new RdfsClass();
            rdfsClass.setId(SYSTEM_OBJECT);
            facade.addType(rdfsClass);

        }
        {
            RdfsClass rdfsClass = new RdfsClass();
            rdfsClass.setId(SYSTEM_COLLECTION);
            facade.addType(rdfsClass);
        }
        {
            RdfsClass rdfsClass = new RdfsClass();
            rdfsClass.setId(SYSTEM_PROJECT);
            facade.addType(rdfsClass);
        }
        {
            RdfsClass rdfsClass = new RdfsClass();
            rdfsClass.setId(SYSTEM_DATASET);
            facade.addType(rdfsClass);
        }
        {
            RdfsClass rdfsClass = new RdfsClass();
            rdfsClass.setId(SYSTEM_VOCABULARY);
            facade.addType(rdfsClass);
        }

    }

    private void addSchema(ISchemaFacade facade, MapResult mapResult)
    {
        for (RdfsClass rdfsClass : mapResult.getSchema().getClasses())
        {
            facade.addType(rdfsClass);

        }
        for (TypeProperty rdfsProperty : mapResult.getSchema().getProperties())
        {
            facade.addPropertyType(rdfsProperty);

        }

    }

    private void addMetaData(ISchemaFacade facade, MapResult mapResult)
    {
        for (MetadataEntry metaDataEntry : mapResult.getMetaDataEntries())
        {
            facade.addEntry(metaDataEntry);

        }

    }

}
