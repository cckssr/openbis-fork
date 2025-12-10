package ch.ethz.sis.rocrateserver.openapi.v1.service;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.session.SessionInformation;
import ch.ethz.sis.rocrateserver.exception.RoCrateExceptions;
import ch.ethz.sis.rocrateserver.openapi.v1.service.delegates.ExportDelegate;
import ch.ethz.sis.rocrateserver.openapi.v1.service.delegates.ImportDelegate;
import ch.ethz.sis.rocrateserver.openapi.v1.service.helper.OpeBISFactory;
import ch.ethz.sis.rocrateserver.openapi.v1.service.helper.validation.ValidationErrorMapping;
import ch.ethz.sis.rocrateserver.openapi.v1.service.params.ExportParams;
import ch.ethz.sis.rocrateserver.openapi.v1.service.params.ImportParams;
import ch.ethz.sis.rocrateserver.openapi.v1.service.response.ErrorResponse;
import ch.ethz.sis.rocrateserver.openapi.v1.service.response.Validation.ValidationReport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.SneakyThrows;
import org.jboss.logging.Logger;
import org.jboss.resteasy.specimpl.ResponseBuilderImpl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;


@Path("/openbis/open-api/ro-crate")
public class RoCrateService {

    private static final Logger LOG = Logger.getLogger(RoCrateService.class);


    public static final String APPLICATION_LD_JSON = "application/ld+json";

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
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes({ APPLICATION_LD_JSON, "application/zip" })
    @Path("import")
    @SneakyThrows
    public Response import_(
            @BeanParam ImportParams headers,
            InputStream body) throws JsonProcessingException
    {
        OpenBIS openBIS = null;
        try {
            openBIS = OpeBISFactory.createOpenBIS(headers.getApiKey());
            SessionInformation sessionInformation = openBIS.getSessionInformation();
        } catch (Exception ex) {
            RoCrateExceptions.throwInstance(RoCrateExceptions.UNAVAILABLE_API_KEY);
        }

        try {
            Map<String, String> externalToOpenBisIdentifiers =
                    importDelegate.import_(openBIS, headers, body, false)
                            .getExternalToOpenBisIdentifiers();
            ObjectMapper objectMapper = new ObjectMapper();
            String serialized = objectMapper.writeValueAsString(externalToOpenBisIdentifiers);
            return Response.ok(serialized).build();
        } catch (WebApplicationException ex)
        {
            ErrorResponse errorResponse = new ErrorResponse();
            ObjectMapper objectMapper = new ObjectMapper();
            Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
            responseBuilder.status(ex.getResponse().getStatus());
            responseBuilder.entity(objectMapper.writeValueAsString(errorResponse));
            return responseBuilder.build();

        } catch (Exception ex)
        {
            LOG.error("There was an error", ex);
            throw new RuntimeException(ex);
        } finally {
            //SessionWorkSpaceManager.clear(headers.getApiKey());
        }
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes({ APPLICATION_LD_JSON, "application/zip" })
    @Path("validate")
    public Object validate(
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
            return ValidationReport.serialize(
                    new ValidationReport(openBisImportResult.getValidationResult().isOkay(),
                            ValidationErrorMapping.mapErrors(
                                    openBisImportResult.getValidationResult()),
                            openBisImportResult.getValidationResult().getFoundIdentifiers()));
        } catch (WebApplicationException ex)
        {
            LOG.error("There was an error", ex);

            ErrorResponse errorResponse = new ErrorResponse();
            ObjectMapper objectMapper = new ObjectMapper();
            Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
            responseBuilder.status(ex.getResponse().getStatus());
            responseBuilder.entity(objectMapper.writeValueAsString(errorResponse));
            return responseBuilder.build();
        } catch (Exception ex)
        {
            LOG.error("There was an error", ex);
            throw new RuntimeException(ex);

        } finally
        {
            //SessionWorkSpaceManager.clear(headers.getApiKey());
        }
    }

    @POST
    @Produces({ APPLICATION_LD_JSON, "application/zip" })
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("export")
    public Response export(
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
            InputStream inputStream = exportDelegate.export(openBIS, headers, body);
            return Response.ok(inputStream.readAllBytes())
                    .type(headers.getAccept()).build();
        } catch (WebApplicationException ex)
        {
            ErrorResponse errorResponse = new ErrorResponse();
            ObjectMapper objectMapper = new ObjectMapper();
            Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
            responseBuilder.status(ex.getResponse().getStatus());
            responseBuilder.entity(objectMapper.writeValueAsString(errorResponse));
            return responseBuilder.build();

        } catch (Exception ex)
        {
            Log.error(ex);
            throw new RuntimeException(ex);
        } finally {
            //SessionWorkSpaceManager.clear(headers.getApiKey());
        }


    }

}
