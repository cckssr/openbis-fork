package ch.ethz.sis.openbis.generic.server.asapi.openapi.v1.service.delegates;

import ch.eth.sis.rocrate.SchemaFacade;
import ch.eth.sis.rocrate.facade.IMetadataEntry;
import ch.eth.sis.rocrate.facade.IPropertyType;
import ch.eth.sis.rocrate.facade.IType;
import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.ImportResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.data.ImportData;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.data.ImportFormat;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.options.ImportMode;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.options.ImportOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.session.SessionInformation;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.ethz.sis.openbis.generic.excel.v3.to.ExcelWriter;
import ch.ethz.sis.openbis.generic.server.asapi.openapi.v1.service.helper.OpeBISProvider;
import ch.ethz.sis.openbis.generic.server.asapi.openapi.v1.service.helper.RoCrateSchemaValidation;
import ch.ethz.sis.openbis.generic.server.asapi.openapi.v1.service.helper.SessionWorkSpace;
import ch.ethz.sis.openbis.generic.server.asapi.openapi.v1.service.params.ValidateParams;
import ch.openbis.rocrate.app.reader.RdfToModel;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.reader.FolderReader;
import edu.kit.datamanager.ro_crate.reader.RoCrateReader;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ImportDelegate
{

    public List<String> import_(
            ValidateParams headers,
            InputStream body)
            throws IOException
    {
        System.out.println("Import Started");
        List<String> result;
        try
        {
            // Writing the crate to the session workspace

            java.nio.file.Path roCrateMetadata = java.nio.file.Path.of("ro-crate-metadata.json");
            SessionWorkSpace.write(headers.getApiKey(), roCrateMetadata, body);
            // Reading ro-crate model
            RoCrateReader roCrateFolderReader = new RoCrateReader(new FolderReader());
            RoCrate crate = roCrateFolderReader.readCrate(SessionWorkSpace.getRealPath(headers.getApiKey(), null).toString());

            SchemaFacade schemaFacade = SchemaFacade.of(crate);
            List<IType> types = schemaFacade.getTypes();
            List<IPropertyType> propertyTypes = schemaFacade.getPropertyTypes();
            List<IMetadataEntry> entryList = new ArrayList<>();
            for (var type : types)
            {
                entryList.addAll(schemaFacade.getEntries(type.getId()));
            }
            // Converting ro-crate model to openBIS model
            OpenBisModel conversion = RdfToModel.convert(types, propertyTypes, entryList, "DEFAULT", "DEFAULT");
            if (!RoCrateSchemaValidation.validate(conversion).isOkay())
            {
                return List.of("no good!");
            }

            byte[] importExcel = ExcelWriter.convert(ExcelWriter.Format.EXCEL, conversion);
            // Sending import request to openBIS
            OpenBIS openBIS = OpeBISProvider.createClient(headers.getApiKey());
            java.nio.file.Path modelAsExcel = java.nio.file.Path.of(
                    UUID.randomUUID() + ".xlsx");
            SessionWorkSpace.write(openBIS.getSessionToken(), modelAsExcel,
                    new ByteArrayInputStream(importExcel));
            java.nio.file.Path realPath =
                    SessionWorkSpace.getRealPath(openBIS.getSessionToken(), modelAsExcel);
            openBIS.uploadToSessionWorkspace(realPath);

            ImportOptions importOptions = new ImportOptions();
            importOptions.setMode(ImportMode.UPDATE_IF_EXISTS);

            ImportData importData = new ImportData();
            importData.setSessionWorkspaceFiles(new String[] { modelAsExcel.toString() });
            importData.setFormat(ImportFormat.EXCEL);
            SessionInformation sessionInformation = openBIS.getSessionInformation();
            System.out.println("Session information: " + sessionInformation);
            ImportResult importResult = openBIS.executeImport(importData, importOptions);
            result = importResult.getObjectIds().stream().map( id -> id.toString()).toList();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new IllegalArgumentException(e);
        } finally
        {
            SessionWorkSpace.clear(headers.getApiKey());
        }
        System.out.println("Import Finished");
        return result;
    }
}
