package ch.ethz.sis.rocrateserver.openapi.v1.service;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.session.SessionInformation;
import ch.ethz.sis.rocrateserver.exception.RoCrateExceptions;
import ch.ethz.sis.rocrateserver.openapi.v1.service.delegates.ExportDelegate;
import ch.ethz.sis.rocrateserver.openapi.v1.service.delegates.ImportDelegate;
import ch.ethz.sis.rocrateserver.openapi.v1.service.helper.OpeBISProvider;
import ch.ethz.sis.rocrateserver.openapi.v1.service.helper.SessionWorkSpace;
import ch.ethz.sis.rocrateserver.openapi.v1.service.params.ExportParams;
import ch.ethz.sis.rocrateserver.openapi.v1.service.params.ImportParams;
import ch.ethz.sis.rocrateserver.openapi.v1.service.response.ValidationReport;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.SneakyThrows;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

@Path("/openbis/open-api/ro-crate")
public class RoCrateService {

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
    @SneakyThrows
    public List<String> import_(
            @BeanParam ImportParams headers,
            InputStream body)
    {
        OpenBIS openBIS = null;
        try {
            openBIS = OpeBISProvider.createClient(headers.getApiKey());
            SessionInformation sessionInformation = openBIS.getSessionInformation();
        } catch (Exception ex) {
            RoCrateExceptions.throwInstance(RoCrateExceptions.UNAVAILABLE_API_KEY);
        }

        try {
            return importDelegate.import_(openBIS, headers, body, false);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            SessionWorkSpace.clear(headers.getApiKey());
        }
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes({"application/ld+json", "application/zip"})
    @Path("validate")
    public String validate(
            @BeanParam ImportParams headers,
            InputStream body)
            throws IOException
    {
        OpenBIS openBIS = null;
        try {
            openBIS = OpeBISProvider.createClient(headers.getApiKey());
            SessionInformation sessionInformation = openBIS.getSessionInformation();
        } catch (Exception ex) {
            RoCrateExceptions.throwInstance(RoCrateExceptions.UNAVAILABLE_API_KEY);
        }

        try {
            importDelegate.import_(openBIS, headers, body, true);
            return ValidationReport.serialize(new ValidationReport(true));
        } catch (Exception ex) {
            return ValidationReport.serialize(new ValidationReport(false));
        } finally {
            SessionWorkSpace.clear(headers.getApiKey());
        }
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes("application/json")
    @Path("export")
    public OutputStream export(
            @BeanParam ExportParams headers,
            InputStream body) throws Exception
    {
        OpenBIS openBIS = null;
        try {
            openBIS = OpeBISProvider.createClient(headers.getApiKey());
            SessionInformation sessionInformation = openBIS.getSessionInformation();
        } catch (Exception ex) {
            RoCrateExceptions.throwInstance(RoCrateExceptions.UNAVAILABLE_API_KEY);
        }

        try {
            return exportDelegate.export(openBIS, headers, body);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            SessionWorkSpace.clear(headers.getApiKey());
        }


    }

}
