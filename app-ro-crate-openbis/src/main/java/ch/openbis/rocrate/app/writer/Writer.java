package ch.openbis.rocrate.app.writer;

import ch.eth.sis.rocrate.SchemaFacade;
import ch.eth.sis.rocrate.facade.*;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.openbis.rocrate.app.writer.mapping.Mapper;
import ch.openbis.rocrate.app.writer.mapping.types.MapResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.entities.contextual.ContextualEntity;
import edu.kit.datamanager.ro_crate.writer.FolderWriter;

import java.nio.file.Path;

public class Writer
{

    private static final String TYPE = "@type";

    public static final String NAMESPACE_SEPARATOR = ":";

    public static final String SYSTEM_SPACE = NAMESPACE_SEPARATOR + "Space";

    public static final String SYSTEM_OBJECT = NAMESPACE_SEPARATOR + "Object";

    public static final String SYSTEM_COLLECTION =
            NAMESPACE_SEPARATOR + "Collection";

    public static final String SYSTEM_PROJECT = NAMESPACE_SEPARATOR + "Project";

    public static final String SYSTEM_DATASET = NAMESPACE_SEPARATOR + "Dataset";

    public static final String SYSTEM_VOCABULARY =
            NAMESPACE_SEPARATOR + "Vocabulary";

    public void write(OpenBisModel openBisModel, Path outPath) throws JsonProcessingException
    {
        RoCrate.RoCrateBuilder builder =
                new RoCrate.RoCrateBuilder("name", "description", "2024-12-04T07:53:11Z",
                        "licenseIdentifier");
        builder.addValuePairToContext("schema",
                "https://www.w3.org/TR/rdf-schema");
        builder.addValuePairToContext("owl",
                "https://www.w3.org/TR/owl-ref");
        builder.addValuePairToContext("openBIS", "_:");

        RoCrate crate = builder.build();
        ISchemaFacade schemaFacade = new SchemaFacade(crate);
        crate.getAllContextualEntities();
        addSystemSchema(schemaFacade);
        Mapper mapper = new Mapper();
        MapResult rdfsRepresentation =
                mapper.transform(openBisModel);
        addSchema(schemaFacade, rdfsRepresentation);
        addMetaData(schemaFacade, rdfsRepresentation);
        RoCrate roCrate = builder.build();
        FolderWriter folderRoCrateWriter = new FolderWriter();
        folderRoCrateWriter.save(roCrate, outPath.toString());

    }

    private void addContextEntities(OpenBisModel openBisModel)
    {
        ContextualEntity.ContextualEntityBuilder rdfsProperty =
                new ContextualEntity.ContextualEntityBuilder();

    }


    private void addSystemSchema(ISchemaFacade facade)
    {

        {
            Type type = new Type();
            type.setId(SYSTEM_SPACE);
            facade.addType(type);

        }
        {
            Type type = new Type();
            type.setId(SYSTEM_OBJECT);
            facade.addType(type);

        }
        {
            Type type = new Type();
            type.setId(SYSTEM_COLLECTION);
            facade.addType(type);
        }
        {
            Type type = new Type();
            type.setId(SYSTEM_PROJECT);
            facade.addType(type);
        }
        {
            Type type = new Type();
            type.setId(SYSTEM_DATASET);
            facade.addType(type);
        }
        {
            Type type = new Type();
            type.setId(SYSTEM_VOCABULARY);
            facade.addType(type);
        }

    }

    private void addSchema(ISchemaFacade facade, MapResult mapResult)
    {
        for (IType type : mapResult.getSchema().getClasses())
        {
            facade.addType(type);

        }
        for (PropertyType rdfsProperty : mapResult.getSchema().getProperties())
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
