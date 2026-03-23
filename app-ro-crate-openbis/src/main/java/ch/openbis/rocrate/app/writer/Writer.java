package ch.openbis.rocrate.app.writer;

import ch.eth.sis.rocrate.SchemaFacade;
import ch.eth.sis.rocrate.facade.*;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.openbis.rocrate.app.Constants;
import ch.openbis.rocrate.app.writer.mapping.Mapper;
import ch.openbis.rocrate.app.writer.mapping.types.MapResult;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.entities.contextual.ContextualEntity;
import edu.kit.datamanager.ro_crate.entities.data.FileEntity;
import edu.kit.datamanager.ro_crate.writer.FolderWriter;
import edu.kit.datamanager.ro_crate.writer.ZipWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Writer
{

    private static final String TYPE = "@type";

    public void write(OpenBisModel openBisModel, Path outPath) throws IOException, Exception
    {

        SchemaFacade schemaFacade = new SchemaFacade(
                "name", "description", "2024-12-04T07:53:11Z", "licenseIdentifier",
                Map.of("openBIS", "www.openbis.ch")

        );

        addSystemSchema(schemaFacade);
        Mapper mapper = new Mapper();
        MapResult rdfsRepresentation =
                mapper.transform(openBisModel);
        addSchema(schemaFacade, rdfsRepresentation);
        addMetaData(schemaFacade, rdfsRepresentation);
        handleFiles(schemaFacade, rdfsRepresentation);
        RoCrate roCrate = schemaFacade.getCrate();

        if (outPath.toString().toLowerCase().endsWith(".zip"))
        {
            ZipWriter zipWriter = new ZipWriter();
            zipWriter.save(roCrate, outPath.toString());

        } else
        {
            FolderWriter folderRoCrateWriter = new FolderWriter();

            folderRoCrateWriter.save(roCrate, outPath.toString());
        }
    }

    private void handleFiles(SchemaFacade schemaFacade, MapResult mapResult)
    {

        for (MapResult.RoCrateFile file : mapResult.getFiles())
        {
            FileEntity.FileEntityBuilder fileEntityBuilder = new FileEntity.FileEntityBuilder();

            fileEntityBuilder.addType("Dataset");
            fileEntityBuilder.setId(file.id());
            fileEntityBuilder.setLocation(file.path());
            schemaFacade.getCrate().addDataEntity(fileEntityBuilder.build());

        }

    }

    private void addContextEntities(OpenBisModel openBisModel)
    {
        ContextualEntity.ContextualEntityBuilder rdfsProperty =
                new ContextualEntity.ContextualEntityBuilder();

    }


    private void addSystemSchema(ISchemaFacade facade)
    {

        List<IType> fileTypes = new ArrayList<>();
        {
            Type type = new Type();
            type.setId(Constants.PROPERTY_SPACE);
            facade.addType(type);

        }
        {
            Type type = new Type();
            type.setId(Constants.GRAPH_ID_OBJECT);
            fileTypes.add(type);

            facade.addType(type);

        }
        {
            Type type = new Type();
            type.setId(Constants.GRAPH_ID_Collection);
            fileTypes.add(type);
            facade.addType(type);
        }
        {
            Type type = new Type();
            type.setId(Constants.GRAPH_ID_PROJECT);
            facade.addType(type);
        }
        {
            Type type = new Type();
            type.setId(Constants.GRAPH_ID_DATASET);
            facade.addType(type);
        }
        {
            Type type = new Type();
            type.setId(Constants.GRAPH_ID_VOCABULARY);
            facade.addType(type);
        }

        {
            PropertyType propertyType = new PropertyType();
            propertyType.setId(Constants.PROPERTY_ID_FILES);
            {
                Type roCrateFileType = new Type();
                roCrateFileType.setId("Dataset");
                propertyType.addType(roCrateFileType);
                for (IType fileType : fileTypes)
                {
                    Type type = (Type) fileType;
                    type.addProperty(propertyType);
                }

                facade.addPropertyType(propertyType);
                propertyType.setDomainIncludes(fileTypes);
            }
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
