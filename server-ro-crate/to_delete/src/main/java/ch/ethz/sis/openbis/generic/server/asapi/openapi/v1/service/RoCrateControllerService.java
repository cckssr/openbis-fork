package ch.ethz.sis.openbis.generic.server.asapi.openapi.v1.service;

import ch.eth.sis.rocrate.SchemaFacade;
import ch.eth.sis.rocrate.facade.IMetadataEntry;
import ch.eth.sis.rocrate.facade.IPropertyType;
import ch.eth.sis.rocrate.facade.IType;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.openbis.rocrate.app.reader.RdfToModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.reader.FolderReader;
import edu.kit.datamanager.ro_crate.reader.RoCrateReader;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class RoCrateControllerService
{

    public enum RoCrateValidationResult
    {
        OK,
        NO_ENTITY_FOUND,
        VALIDATION_ERROR,
        PROPERTY_ERROR,
        MISSING_DATA_ERROR,

    }

    private static final File TEMP_DIRECTORY = new File(System.getProperty("java.io.tmpdir"));

    public boolean validate(String jsonLd)
    {

        File tempRoCrate = new File(TEMP_DIRECTORY + "/" + UUID.randomUUID().toString());
        tempRoCrate.mkdir();
        File metadataFile = new File(tempRoCrate.getPath() + "/" + "ro-crate-metadata.json");

        try
        {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(metadataFile));
            bufferedWriter.write(jsonLd);
            bufferedWriter.flush();
            bufferedWriter.close();

            RoCrateReader roCrateFolderReader = new RoCrateReader(new FolderReader());
            RoCrate crate = roCrateFolderReader.readCrate(tempRoCrate.getPath());

            SchemaFacade schemaFacade = SchemaFacade.of(crate);
            List<IType> types = schemaFacade.getTypes();
            List<IPropertyType> propertyTypes = schemaFacade.getPropertyTypes();
            List<IMetadataEntry> entryList = new ArrayList<>();
            for (var type : types)
            {
                entryList.addAll(schemaFacade.getEntries(type.getId()));

            }
            OpenBisModel conversion =
                    RdfToModel.convert(types, propertyTypes, entryList, "DEFAULT", "DEFAULT");
            RoCrateValidate.validate(conversion);

            System.out.println("lol");

        } catch (JsonProcessingException e)
        {
            throw new RuntimeException(e);
        } catch (FileNotFoundException e)
        {
            throw new RuntimeException(e);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        } finally
        {
            metadataFile.delete();
            tempRoCrate.delete();
        }

        return false;
    }

}
