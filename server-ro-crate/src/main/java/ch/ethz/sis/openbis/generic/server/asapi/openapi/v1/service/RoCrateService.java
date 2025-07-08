package ch.ethz.sis.openbis.generic.server.asapi.openapi.v1.service;

import ch.eth.sis.rocrate.SchemaFacade;
import ch.eth.sis.rocrate.facade.IMetadataEntry;
import ch.eth.sis.rocrate.facade.IPropertyType;
import ch.eth.sis.rocrate.facade.IType;
import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
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

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("test")
    public String test() {
        String openBISUrl = StartupMain.getConfiguration().getStringProperty(RoCrateServerParameter.openBISUrl);
        Integer openBISTimeout = StartupMain.getConfiguration().getIntegerProperty(RoCrateServerParameter.openBISTimeout);
        String openBISUser = StartupMain.getConfiguration().getStringProperty(RoCrateServerParameter.openBISUser);
        String openBISPassword = StartupMain.getConfiguration().getStringProperty(RoCrateServerParameter.openBISPassword);
        OpenBIS openBIS = new OpenBIS(openBISUrl, openBISTimeout);
        try {
            return openBIS.login(openBISUser, openBISPassword);
        } catch (Exception ex) {

        } finally {
            openBIS.logout();
        }

        return "error";
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("import")
    public List<String> importRoCrate(InputStream inputStream, Map<String, String> options) {
        List<String> result;
        File tempRoCrate = new File(TEMP_DIRECTORY + "/" + UUID.randomUUID().toString());
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
            OpenBisModel conversion = RdfToModel.convert(types, propertyTypes, entryList, "DEFAULT", "DEFAULT");
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
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("export")
    public OutputStream exportRoCrate(List<String> identifiers) {
        return null;
    }

}
