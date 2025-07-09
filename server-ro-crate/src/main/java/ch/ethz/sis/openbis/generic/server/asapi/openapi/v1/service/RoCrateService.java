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

    private static final File TEMP_DIRECTORY = new File(System.getProperty("java.io.tmpdir"));

    public static boolean isTest = false;

    public static boolean isTest()
    {
        return isTest;
    }

    public static void setTest(boolean isTest)
    {
        RoCrateService.isTest = isTest;
    }

    OpenBIS getOpenBis(String personalAccessToken)
    {
        String openBISUrl =
                StartupMain.getConfiguration().getStringProperty(RoCrateServerParameter.openBISUrl);
        Integer openBISTimeout = StartupMain.getConfiguration()
                .getIntegerProperty(RoCrateServerParameter.openBISTimeout);
        OpenBIS openBIS = new OpenBIS(openBISUrl, openBISTimeout);
        openBIS.setSessionToken(personalAccessToken);

        if (personalAccessToken == null && isTest)
        {
            String openBISUser = StartupMain.getConfiguration()
                    .getStringProperty(RoCrateServerParameter.openBISUser);
            String openBISPassword = StartupMain.getConfiguration()
                    .getStringProperty(RoCrateServerParameter.openBISPassword);
            openBIS.login(openBISUser, openBISPassword);
        }

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
        try
        {
            List<String> result;
            java.nio.file.Path roCratePath = java.nio.file.Path.of("ro-create" + UUID.randomUUID());
            SessionWorkSpace.write(sessionToken, roCratePath, inputStream);
            File tempRoCrate = new File(StartupMain.getConfiguration().getStringProperty(
                    RoCrateServerParameter.sessionWorkSpace) + "/" + UUID.randomUUID().toString());
            tempRoCrate.mkdir();
            File metadataFile = new File(tempRoCrate.getPath() + "/" + "ro-crate-metadata.json");
            try
            {
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(metadataFile));
                bufferedWriter.write(Files.readFile(inputStream));
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
                byte[] importExcel = ExcelWriter.convert(ExcelWriter.Format.EXCEL, conversion);
                OpenBIS openBIS = getOpenBis(null);

                java.nio.file.Path myPath = java.nio.file.Path.of(
                        UUID.randomUUID() + ".xlsx");
                SessionWorkSpace.write(openBIS.getSessionToken(), myPath,
                        new ByteArrayInputStream(importExcel));
                java.nio.file.Path realPath =
                        SessionWorkSpace.getRealPath(openBIS.getSessionToken(), myPath);
                String uploadId = openBIS.uploadToSessionWorkspace(realPath);

                ImportOptions importOptions = new ImportOptions();
                importOptions.setMode(ImportMode.UPDATE_IF_EXISTS);

                ImportData importData = new ImportData();
                importData.setSessionWorkspaceFiles(new String[] { uploadId });
                importData.setFormat(ImportFormat.EXCEL);

                ImportResult importResult = openBIS.executeImport(importData, importOptions);

                result = new ArrayList<>();
                result.add("TRUE");
            } catch (Exception e)
            {
                throw new RuntimeException(e);
            } finally
            {
                metadataFile.delete();
                tempRoCrate.delete();
            }
            return result;
        } catch (Exception e)
        {
            throw new RuntimeException();

        } finally
        {
            SessionWorkSpace.clear(sessionToken);
        }
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("export")
    public OutputStream exportRoCrate(List<String> identifiers) {
        return null;
    }

}
