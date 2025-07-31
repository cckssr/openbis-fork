package ch.ethz.sis.rocrateserver.openapi.v1.service;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.session.SessionInformation;
import ch.ethz.sis.rocrateserver.exception.RoCrateExceptions;
import ch.ethz.sis.rocrateserver.openapi.v1.service.delegates.ExportDelegate;
import ch.ethz.sis.rocrateserver.openapi.v1.service.delegates.ImportDelegate;
import ch.ethz.sis.rocrateserver.openapi.v1.service.helper.OpeBISFactory;
import ch.ethz.sis.rocrateserver.openapi.v1.service.helper.SessionWorkSpaceManager;
import ch.ethz.sis.rocrateserver.openapi.v1.service.helper.ValidationErrorMapping;
import ch.ethz.sis.rocrateserver.openapi.v1.service.params.ExportParams;
import ch.ethz.sis.rocrateserver.openapi.v1.service.params.ImportParams;
import ch.ethz.sis.rocrateserver.openapi.v1.service.response.Validation.ValidationReport;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.SneakyThrows;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

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
        OpenBIS openBIS = OpeBISFactory.createOpenBIS(apiKey);
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
    public Map<String, String> import_(
            @BeanParam ImportParams headers,
            InputStream body)
    {
        OpenBIS openBIS = null;
        try {
            openBIS = OpeBISFactory.createOpenBIS(headers.getApiKey());
            SessionInformation sessionInformation = openBIS.getSessionInformation();
        } catch (Exception ex) {
            RoCrateExceptions.throwInstance(RoCrateExceptions.UNAVAILABLE_API_KEY);
        }

        try {
            return importDelegate.import_(openBIS, headers, body, false)
                    .getExternalToOpenBisIdentifiers();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            SessionWorkSpaceManager.clear(headers.getApiKey());
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
            openBIS = OpeBISFactory.createOpenBIS(headers.getApiKey());
            SessionInformation sessionInformation = openBIS.getSessionInformation();
        } catch (Exception ex) {
            RoCrateExceptions.throwInstance(RoCrateExceptions.UNAVAILABLE_API_KEY);
        }

        try {
            ImportDelegate.OpenBisImportResult openBisImportResult =
                    importDelegate.import_(openBIS, headers, body, true);
            return ValidationReport.serialize(new ValidationReport(true,
                    ValidationErrorMapping.mapErrors(openBisImportResult.getValidationResult())));
        } catch (Exception ex) {
            return ValidationReport.serialize(new ValidationReport(false, List.of()));
        } finally {
            SessionWorkSpaceManager.clear(headers.getApiKey());
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
            openBIS = OpeBISFactory.createOpenBIS(headers.getApiKey());
            SessionInformation sessionInformation = openBIS.getSessionInformation();
        } catch (Exception ex) {
            RoCrateExceptions.throwInstance(RoCrateExceptions.UNAVAILABLE_API_KEY);
        }

        try {
            return exportDelegate.export(openBIS, headers, body);
        } catch (WebApplicationException e)
        {
            throw e;
        } catch (Exception ex)
        {


            throw new RuntimeException(ex);
        } finally {
            SessionWorkSpaceManager.clear(headers.getApiKey());
        }


    }

}
