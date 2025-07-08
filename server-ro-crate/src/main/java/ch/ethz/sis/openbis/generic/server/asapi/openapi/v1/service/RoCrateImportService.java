package ch.ethz.sis.openbis.generic.server.asapi.openapi.v1.service;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.ros.startup.RoCrateServerParameter;
import ch.ethz.sis.openbis.ros.startup.StartupMain;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.POST;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

@Path("/openbis/open-api/ro-crate")
public class RoCrateImportService {

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
    public List<String> importRoCrate(InputStream inputStream) {
        return null;
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("export")
    public OutputStream exportRoCrate(List<String> identifiers) {
        return null;
    }

}
