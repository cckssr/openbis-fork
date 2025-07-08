package ch.ethz.sis.openbis.generic.server.asapi.openapi.v1.service;

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
    public List<String> importRoCrate(InputStream inputStream) {
        return null;
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public OutputStream exportRoCrate(List<String> identifiers) {
        return null;
    }

}
