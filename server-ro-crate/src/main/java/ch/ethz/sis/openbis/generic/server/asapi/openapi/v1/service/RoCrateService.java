package ch.ethz.sis.openbis.generic.server.asapi.openapi.v1.service;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.server.asapi.openapi.v1.service.delegates.ExportDelegate;
import ch.ethz.sis.openbis.generic.server.asapi.openapi.v1.service.delegates.ImportDelegate;
import ch.ethz.sis.openbis.generic.server.asapi.openapi.v1.service.delegates.ValidateDelegate;
import ch.ethz.sis.openbis.generic.server.asapi.openapi.v1.service.helper.OpeBISProvider;
import ch.ethz.sis.openbis.generic.server.asapi.openapi.v1.service.params.ExportParams;
import ch.ethz.sis.openbis.generic.server.asapi.openapi.v1.service.params.ValidateParams;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

@Path("/openbis/open-api/ro-crate")
public class RoCrateService {

    @Inject
    ValidateDelegate validateDelegate;

    @Inject
    ImportDelegate importDelegate;

    @Inject
    ExportDelegate exportDelegate;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("test-echo")
    public String testEcho(@QueryParam(value = "message") String message) {
        return message;
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("test-openbis-connection")
    public String testOpenbisConnection(@QueryParam(value = "api-key") String apiKey) {
        OpenBIS openBIS = OpeBISProvider.createClient(apiKey);
        try {
            return openBIS.getSessionInformation().getUserName();
        } finally {
            openBIS.logout();
        }
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes({"application/ld+json", "application/zip"})
    @Path("import")
    public List<String> import_(
            @BeanParam ValidateParams headers,
            InputStream body)
            throws IOException
    {
        return importDelegate.import_(headers, body);
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes({"application/ld+json", "application/zip"})
    @Path("validate")
    public List<String> validate(
            @BeanParam ValidateParams headers,
            InputStream body)
            throws IOException
    {
        return validateDelegate.validate(headers, body);
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes("application/json")
    @Path("export")
    public OutputStream export(
            @BeanParam ExportParams headers,
            InputStream body) throws Exception
    {
        return exportDelegate.export(headers, body);
    }

}
