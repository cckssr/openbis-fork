package ch.ethz.sis.openbis.generic.server.asapi.openapi.v1.service.delegates;

import ch.eth.sis.rocrate.SchemaFacade;
import ch.eth.sis.rocrate.facade.IMetadataEntry;
import ch.eth.sis.rocrate.facade.IPropertyType;
import ch.eth.sis.rocrate.facade.IType;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.ethz.sis.openbis.generic.server.asapi.openapi.v1.service.RoCrateSchemaValidation;
import ch.ethz.sis.openbis.generic.server.asapi.openapi.v1.service.SessionWorkSpace;
import ch.ethz.sis.openbis.generic.server.asapi.openapi.v1.service.params.ValidateParams;
import ch.openbis.rocrate.app.reader.RdfToModel;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.reader.FolderReader;
import edu.kit.datamanager.ro_crate.reader.RoCrateReader;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class ValidateDelegate
{

    public List<String> validate(InputStream inputStream, ValidateParams validateParams)
            throws IOException
    {
        List<String> result = new ArrayList<>();
        try
        {
            // Writing the crate to the session workspace

            java.nio.file.Path roCrateMetadata = java.nio.file.Path.of("ro-crate-metadata.json");
            SessionWorkSpace.write(validateParams.getApiKey(), roCrateMetadata, inputStream);
            // Reading ro-crate model
            RoCrateReader roCrateFolderReader = new RoCrateReader(new FolderReader());
            RoCrate crate = roCrateFolderReader.readCrate(
                    SessionWorkSpace.getRealPath(validateParams.getApiKey(), null).toString());

            SchemaFacade schemaFacade = SchemaFacade.of(crate);
            List<IType> types = schemaFacade.getTypes();
            List<IPropertyType> propertyTypes = schemaFacade.getPropertyTypes();
            List<IMetadataEntry> entryList = new ArrayList<>();
            for (var type : types)
            {
                entryList.addAll(schemaFacade.getEntries(type.getId()));
            }
            // Converting ro-crate model to openBIS model
            OpenBisModel conversion =
                    RdfToModel.convert(types, propertyTypes, entryList, "DEFAULT", "DEFAULT");
            if (!RoCrateSchemaValidation.validate(conversion).isOkay())
            {
                return List.of("no good!");
            }
        } catch (Exception e)
        {
            throw new IllegalArgumentException(e);
        } finally
        {
            SessionWorkSpace.clear(validateParams.getApiKey());
        }
        return result;

    }
}
