package ch.ethz.sis.openbis.generic.server.asapi.openapi.v1.service;

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
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.ethz.sis.openbis.generic.excel.v3.to.ExcelWriter;
import ch.ethz.sis.openbis.ros.startup.RoCrateServerParameter;
import ch.ethz.sis.openbis.ros.startup.StartupMain;
import ch.openbis.rocrate.app.reader.RdfToModel;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.reader.FolderReader;
import edu.kit.datamanager.ro_crate.reader.RoCrateReader;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.testng.reporters.Files;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/openbis/open-api/ro-crate")
public class RoCrateService {

    OpenBIS getOpenBis(String personalAccessToken)
    {
        String openBISUrl =
                StartupMain.getConfiguration().getStringProperty(RoCrateServerParameter.openBISUrl);
        int openBISTimeout = StartupMain.getConfiguration()
                .getIntegerProperty(RoCrateServerParameter.openBISTimeout);
        OpenBIS openBIS = new OpenBIS(openBISUrl, openBISTimeout);
        openBIS.setSessionToken(personalAccessToken);

        return openBIS;

    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("test")
    public String test() {
        OpenBIS openBIS = getOpenBis(null);
        try {
            return openBIS.getSessionToken();
        } catch (Exception ex) {

        } finally {
            openBIS.logout();
        }

        return "error";
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("import")
    public List<String> importRoCrate(InputStream inputStream, Map<String, String> options)
            throws IOException
    {
        String sessionToken = options.get("sessionToken");
        List<String> result;
        try
        {
            // Writing the crate to the session workspace

                java.nio.file.Path roCrateMetadata = java.nio.file.Path.of("ro-crate-metadata.json");
                SessionWorkSpace.write(sessionToken, roCrateMetadata, inputStream);
            // Reading ro-crate model
                RoCrateReader roCrateFolderReader = new RoCrateReader(new FolderReader());
                RoCrate crate = roCrateFolderReader.readCrate(SessionWorkSpace.getRealPath(sessionToken, null).toString());

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
                byte[] importExcel = ExcelWriter.convert(ExcelWriter.Format.EXCEL, conversion);
            // Sending import request to openBIS
                OpenBIS openBIS = getOpenBis(sessionToken);
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

                ImportResult importResult = openBIS.executeImport(importData, importOptions);
            result = importResult.getObjectIds().stream().map( id -> id.toString()).toList();
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException(e);
        } finally
        {
            SessionWorkSpace.clear(sessionToken);
        }
        return result;
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("export")
    public OutputStream exportRoCrate(List<String> identifiers) {
        return null;
    }

}
