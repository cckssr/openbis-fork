package ch.ethz.sis.rocrateserver.openapi.v1.service;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.session.SessionInformation;
import ch.ethz.sis.rocrateserver.exception.RoCrateExceptions;
import ch.ethz.sis.rocrateserver.openapi.v1.service.delegates.ExportDelegate;
import ch.ethz.sis.rocrateserver.openapi.v1.service.delegates.ImportDelegate;
import ch.ethz.sis.rocrateserver.openapi.v1.service.helper.OpeBISFactory;
import ch.ethz.sis.rocrateserver.openapi.v1.service.jobs.*;
import ch.ethz.sis.rocrateserver.openapi.v1.service.params.ExportParams;
import ch.ethz.sis.rocrateserver.openapi.v1.service.params.ImportParams;
import ch.ethz.sis.rocrateserver.openapi.v1.service.params.ResultParams;
import ch.ethz.sis.rocrateserver.openapi.v1.service.response.AsyncJob;
import ch.ethz.sis.rocrateserver.openapi.v1.service.response.ErrorResponse;
import ch.ethz.sis.rocrateserver.openapi.v1.service.response.ImportResponse;
import ch.ethz.sis.rocrateserver.openapi.v1.service.response.result.AsyncResult;
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Path("/openbis/open-api/ro-crate")
public class RoCrateService
{

    private static final Logger LOG = Logger.getLogger(RoCrateService.class);

    public static final String APPLICATION_LD_JSON = "application/ld+json";

    public static final String APPLICATION_ZIP = "application/zip";

    @Inject
    ImportDelegate importDelegate;

    @Inject
    ExportDelegate exportDelegate;

    AsyncJobRegistry asyncJobRegistry = new AsyncJobRegistry();

    ObjectMapper objectMapper = new ObjectMapper();

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("test-echo")
    public String testEcho(@QueryParam(value = "message") String message)
    {
        return message;
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("test-openbis-connection")
    public String testOpenbisConnection(@QueryParam(value = "api-key") String apiKey)
    {
        OpenBIS openBIS = OpeBISFactory.createOpenBIS(apiKey);
        try
        {
            return openBIS.getSessionInformation().getUserName();
        } finally
        {
            openBIS.logout();
        }
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes({ APPLICATION_LD_JSON, APPLICATION_ZIP })
    @Path("import")
    @SneakyThrows
    public Response import_(
            @BeanParam ImportParams headers,
            InputStream body) throws JsonProcessingException
    {
        OpenBIS openBIS = null;
        try
        {
            openBIS = OpeBISFactory.createOpenBIS(headers.getApiKey());
            SessionInformation sessionInformation = openBIS.getSessionInformation();
        } catch (Exception ex)
        {
            RoCrateExceptions.throwInstance(RoCrateExceptions.UNAVAILABLE_API_KEY);
        }

        try
        {
            AsyncJob asyncJob =
                    importDelegate.import_(asyncJobRegistry, openBIS, headers, body, false);
            ObjectMapper objectMapper = new ObjectMapper();
            String serialized = objectMapper.writeValueAsString(asyncJob);
            return Response.accepted(serialized).build();
        } catch (WebApplicationException ex)
        {
            ErrorResponse errorResponse = new ErrorResponse();
            ObjectMapper objectMapper = new ObjectMapper();
            Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
            responseBuilder.status(ex.getResponse().getStatus());
            responseBuilder.entity(ex.getMessage());
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
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes({ APPLICATION_LD_JSON, APPLICATION_ZIP })
    @Path("validate")
    public Response validate(
            @BeanParam ImportParams headers,
            InputStream body)
            throws IOException
    {
        OpenBIS openBIS = null;
        try
        {
            openBIS = OpeBISFactory.createOpenBIS(headers.getApiKey());
            SessionInformation sessionInformation = openBIS.getSessionInformation();
        } catch (Exception ex)
        {
            RoCrateExceptions.throwInstance(RoCrateExceptions.UNAVAILABLE_API_KEY);
        }

        try
        {
            AsyncJob asyncResult =
                    importDelegate.import_(asyncJobRegistry, openBIS, headers, body, true);
            ObjectMapper objectMapper = new ObjectMapper();
            return Response.accepted().type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(objectMapper.writeValueAsString(asyncResult)).build();
        } catch (WebApplicationException ex)
        {
            LOG.error("There was an error", ex);

            ErrorResponse errorResponse = new ErrorResponse();
            ObjectMapper objectMapper = new ObjectMapper();
            Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
            responseBuilder.status(ex.getResponse().getStatus());
            responseBuilder.entity(ex.getMessage());
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
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("export")
    public Response export(
            @BeanParam ExportParams headers,
            InputStream body) throws Exception
    {
        OpenBIS openBIS = null;
        try
        {
            openBIS = OpeBISFactory.createOpenBIS(headers.getApiKey());
            SessionInformation sessionInformation = openBIS.getSessionInformation();
        } catch (Exception ex)
        {
            RoCrateExceptions.throwInstance(RoCrateExceptions.UNAVAILABLE_API_KEY);
        }

        try
        {
            AsyncJob job = exportDelegate.export(asyncJobRegistry, openBIS, headers, body);
            ObjectMapper objectMapper = new ObjectMapper();
            return Response.accepted(objectMapper.writeValueAsString(job))
                    .type(MediaType.APPLICATION_JSON_TYPE).build();
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
        } finally
        {
            //SessionWorkSpaceManager.clear(headers.getApiKey());
        }

    }

    @GET
    @Path("status")
    public Response status(
            @BeanParam ResultParams headers,
            InputStream body) throws Exception
    {
        OpenBIS openBIS = null;
        SessionInformation sessionInformation = null;
        try
        {
            openBIS = OpeBISFactory.createOpenBIS(headers.getApiKey());
            sessionInformation = openBIS.getSessionInformation();
        } catch (Exception ex)
        {
            RoCrateExceptions.throwInstance(RoCrateExceptions.UNAVAILABLE_API_KEY);
        }
        AsyncJobRegistry.AsyncStatus status =
                asyncJobRegistry.poll(sessionInformation.getUserName(), headers.getJobId());
        AsyncResult result = null;
        int statusCode = 0;
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();

        if (status.getStatus() == AsyncJobRegistry.Status.COMPLETED)
        {
            IAsyncJob job = status.getJob();
            if (job instanceof ExportJob)
            {
                result = new AsyncResult(status.getStatus().toString(), List.of(), null);
                responseBuilder = Response.ok(objectMapper.writeValueAsString(result));
                return responseBuilder.build();
            }
            if (job instanceof ValidateJob)
            {
                responseBuilder.status(Response.Status.OK);
                responseBuilder.type(MediaType.APPLICATION_JSON);
                responseBuilder.entity(((ValidateJob) job).getResult());
                return responseBuilder.build();
            }
            if (job instanceof ImportJob)
            {
                responseBuilder.type(MediaType.APPLICATION_JSON);
                responseBuilder.status(Response.Status.OK);

                if (((ImportJob) job).isValidateOnly())
                {
                    responseBuilder.status(Response.Status.OK);
                    AsyncResult asyncResult =
                            new AsyncResult(status.getStatus().toString(), List.of(), null
                            );
                    asyncResult.setValidationResult(
                            ((ImportJob) job).getResult().getValidationResult());
                    responseBuilder.entity(objectMapper.writeValueAsString(asyncResult));

                } else
                {

                    ImportDelegate.OpenBisImportResult openBisImportResult =
                            ((ImportJob) job).getResult();

                    ImportResponse importResponse = new ImportResponse(
                            openBisImportResult.getExternalToOpenBisIdentifiers());

                    AsyncResult asyncResult = new AsyncResult(status.toString(), List.of(),
                            ((ImportJob) job).getResult().getValidationResult());
                    asyncResult.setImportResponse(importResponse);
                    responseBuilder.entity(objectMapper.writeValueAsString(asyncResult));

                }

                return responseBuilder.build();

            }

        } else if (status.getStatus() == AsyncJobRegistry.Status.FAILED)
        {
            result = new AsyncResult(status.getStatus().toString(),
                    List.of(status.getJob().getException().getMessage() + "\n" + Arrays.stream(
                                    status.getJob().getException().getStackTrace()).toList().stream()
                            .map(x -> x.toString()).collect(
                                    Collectors.joining(","))), null);
            statusCode = Response.Status.OK.getStatusCode();

        } else
        {

            result = new AsyncResult(status.getStatus().toString(), List.of(), null);
            statusCode = Response.Status.OK.getStatusCode();
        }
        responseBuilder = Response.ok(objectMapper.writeValueAsString(result));
        responseBuilder.status(statusCode);

        return responseBuilder.build();

    }

}
