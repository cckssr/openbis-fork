package ch.ethz.sis.rocrateserver.openapi.v1.service;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.session.SessionInformation;
import ch.ethz.sis.rocrateserver.exception.RoCrateExceptions;
import ch.ethz.sis.rocrateserver.openapi.v1.service.delegates.ExportDelegate;
import ch.ethz.sis.rocrateserver.openapi.v1.service.delegates.ImportDelegate;
import ch.ethz.sis.rocrateserver.openapi.v1.service.helper.OpeBISFactory;
import ch.ethz.sis.rocrateserver.openapi.v1.service.helper.SessionWorkSpaceCleanupTimer;
import ch.ethz.sis.rocrateserver.openapi.v1.service.helper.validation.ValidationResult;
import ch.ethz.sis.rocrateserver.openapi.v1.service.jobs.*;
import ch.ethz.sis.rocrateserver.openapi.v1.service.params.DownloadParams;
import ch.ethz.sis.rocrateserver.openapi.v1.service.params.ExportParams;
import ch.ethz.sis.rocrateserver.openapi.v1.service.params.ImportParams;
import ch.ethz.sis.rocrateserver.openapi.v1.service.params.ResultParams;
import ch.ethz.sis.rocrateserver.openapi.v1.service.response.AsyncJob;
import ch.ethz.sis.rocrateserver.openapi.v1.service.response.ErrorResponse;
import ch.ethz.sis.rocrateserver.openapi.v1.service.response.ImportResponse;
import ch.ethz.sis.rocrateserver.openapi.v1.service.response.Validation.ValidationReport;
import ch.ethz.sis.rocrateserver.openapi.v1.service.response.result.AsyncResult;
import ch.ethz.sis.rocrateserver.openapi.v1.service.response.result.StatusResponse;
import ch.ethz.sis.rocrateserver.startup.StartupMain;
import ch.ethz.sis.shared.log.classic.impl.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.SneakyThrows;
import org.jboss.resteasy.specimpl.ResponseBuilderImpl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
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

    Timer sessionWorkSpaceCleanupTimer =
            SessionWorkSpaceCleanupTimer.getTimer(Clock.systemUTC(), StartupMain.getConfiguration(),
                    asyncJobRegistry);


    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("test-echo")
    public String testEcho(@QueryParam(value = "message") String message)
    {
        LOG.info(String.format("Received echo message '%s'", message));
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
            responseBuilder.type(MediaType.APPLICATION_JSON_TYPE);
            String o = objectMapper.writeValueAsString(new ErrorResponse(ex.getMessage()));
            responseBuilder.entity(o);
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
            responseBuilder.type(MediaType.APPLICATION_JSON_TYPE);
            String o = objectMapper.writeValueAsString(new ErrorResponse(ex.getMessage()));
            responseBuilder.entity(o);
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
        if (headers.getExportMimeType() == null || !ExportDelegate.ACCEPTABLE_MIME_TYPES.contains(
                headers.getExportMimeType()))
        {

            String message =
                    "The Export header is not in the range of supported options. Please use one of " + ExportDelegate.ACCEPTABLE_MIME_TYPES.stream()
                            .collect(
                                    Collectors.joining(", "));
            ErrorResponse errorResponse = new ErrorResponse(message);
            ObjectMapper objectMapper = new ObjectMapper();
            Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
            responseBuilder.status(Response.Status.BAD_REQUEST);
            responseBuilder.entity(objectMapper.writeValueAsString(errorResponse));
            return responseBuilder.build();
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
            responseBuilder.type(MediaType.APPLICATION_JSON_TYPE);
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

        try
        {
            List<AsyncResult> asyncStatuses =
                    asyncJobRegistry.pollAll(openBIS.getSessionInformation().getUserName()).stream()
                            .map(x -> mapAsyncResult(x)).toList();
            StatusResponse statusResponse = new StatusResponse(asyncStatuses);
            ObjectMapper objectMapper = new ObjectMapper();

            ResponseBuilderImpl responseBuilder = new ResponseBuilderImpl();
            responseBuilder.status(Response.Status.OK);
            responseBuilder.type(MediaType.APPLICATION_JSON_TYPE);
            responseBuilder.entity(objectMapper.writeValueAsString(statusResponse));
            return responseBuilder.build();
        } catch (Exception e)
        {
            Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
            responseBuilder.entity(
                    e.getMessage() + "\n" + Arrays.stream(e.getStackTrace()).map(x -> x.toString())
                            .collect(
                                    Collectors.joining("\n")));
            return responseBuilder.build();

        }

    }

    private AsyncResult mapAsyncResult(AsyncJobRegistry.AsyncStatus status)
    {
        String statusVal = status.getStatus().toString();
        List<String> errors = status.getJob().getException() != null ?
                List.of(status.getJob().getException().toString()) :
                List.of();

        AsyncResult asyncResult = new AsyncResult(statusVal, errors, null, status.getJobId());

        IAsyncJob job = status.getJob();
        if (status.getStatus() == AsyncJobRegistry.Status.COMPLETED && status.getJob() instanceof ImportJob)
        {

            if (((ImportJob) job).isValidateOnly())
            {
                ValidationResult validationResult =
                        ((ImportJob) job).getResult().getValidationResult();
                asyncResult.setValidationResult(
                        ValidationReport.from(validationResult));

            } else
            {

                ImportDelegate.OpenBisImportResult openBisImportResult =
                        ((ImportJob) job).getResult();

                ImportResponse importResponse = new ImportResponse(
                        openBisImportResult.getExternalToOpenBisIdentifiers());

                asyncResult.setImportResponse(importResponse);

            }

        }

        return asyncResult;
    }

    @GET
    @Path("status/{jobId}")
    public Response statusDetail(
            @BeanParam ResultParams headers,
            @PathParam("jobId") String jobId,
            InputStream body) throws Exception
    {

        OpenBIS openBIS = null;
        SessionInformation sessionInformation = null;
        if (jobId == null)
        {
            ErrorResponse errorResponse = new ErrorResponse("Header jobID is missing");
            return new ResponseBuilderImpl().entity(
                            objectMapper.writeValueAsString(errorResponse))
                    .status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build();
        }
        if (headers.getApiKey() == null)
        {
            ErrorResponse errorResponse = new ErrorResponse("Header api-key is missing");
            return new ResponseBuilderImpl().entity(
                            objectMapper.writeValueAsString(errorResponse))
                    .status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build();
        }

        try
        {
            openBIS = OpeBISFactory.createOpenBIS(headers.getApiKey());
            sessionInformation = openBIS.getSessionInformation();
        } catch (Exception ex)
        {
            RoCrateExceptions.throwInstance(RoCrateExceptions.UNAVAILABLE_API_KEY);
        }
        AsyncJobRegistry.AsyncStatus status =
                asyncJobRegistry.poll(sessionInformation.getUserName(), jobId);
        AsyncResult result = null;
        int statusCode = 0;
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();

        if (status.getStatus() == AsyncJobRegistry.Status.COMPLETED)
        {
            IAsyncJob job = status.getJob();
            if (job instanceof ExportJob)
            {
                result = new AsyncResult(status.getStatus().toString(), List.of(), null, jobId);
                java.nio.file.Path path = ((ExportJob) job).getResult();
                AsyncResult asyncResult = new AsyncResult();
                asyncResult.setStatus(status.getStatus().toString());
                responseBuilder.entity(objectMapper.writeValueAsString(asyncResult));
                return responseBuilder.build();
            }
            if (job instanceof ValidateJob)
            {
                responseBuilder.status(Response.Status.OK);
                responseBuilder.type(MediaType.APPLICATION_JSON);
                ValidationResult result1 = ((ValidateJob) job).getResult();
                AsyncResult asyncResult =
                        new AsyncResult(status.getStatus().toString(), List.of(), null, jobId);
                asyncResult.setValidationResult(ValidationReport.from(result1));
                responseBuilder.entity(objectMapper.writeValueAsString(asyncResult));
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
                            new AsyncResult(status.getStatus().toString(), List.of(), null,
                                    jobId
                            );
                    ValidationResult validationResult =
                            ((ImportJob) job).getResult().getValidationResult();
                    asyncResult.setValidationResult(
                            ValidationReport.from(validationResult));
                    responseBuilder.entity(objectMapper.writeValueAsString(asyncResult));

                } else
                {

                    ImportDelegate.OpenBisImportResult openBisImportResult =
                            ((ImportJob) job).getResult();

                    ImportResponse importResponse = new ImportResponse(
                            openBisImportResult.getExternalToOpenBisIdentifiers());

                    AsyncResult asyncResult =
                            new AsyncResult(status.getStatus().toString(), List.of(),
                                    ((ImportJob) job).getResult().getValidationResult(), jobId);
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
                                    Collectors.joining(","))), null, jobId);
            statusCode = Response.Status.OK.getStatusCode();

        } else
        {

            result = new AsyncResult(status.getStatus().toString(), List.of(), null, jobId);
            statusCode = Response.Status.OK.getStatusCode();
        }
        responseBuilder = Response.ok(objectMapper.writeValueAsString(result));
        responseBuilder.status(statusCode);

        return responseBuilder.build();
    }

    @GET
    @Path("download")
    public Response download(
            @BeanParam DownloadParams headers,
            @QueryParam(value = "jobId") String jobIdParam,
            @QueryParam(value = "apiKey") String apiKeyParam,
            InputStream body) throws Exception
    {
        String jobId = jobIdParam;
        String apiKey = apiKeyParam;
        if(headers.getApiKey() != null && !headers.getApiKey().isBlank()) {
            apiKey = headers.getApiKey();
        }

        if(headers.getJobId() != null && !headers.getJobId().isBlank()) {
            jobId = headers.getJobId();
        }
        return downloadResponse(jobId, apiKey);
    }

    private Response downloadResponse(String jobId, String apiKey) throws Exception
    {
        OpenBIS openBIS = null;
        SessionInformation sessionInformation = null;
        if (jobId == null || jobId.isBlank())
        {
            ErrorResponse errorResponse = new ErrorResponse("Header jobID is missing");
            return new ResponseBuilderImpl().entity(objectMapper.writeValueAsString(errorResponse))
                    .status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build();
        }
        if (apiKey == null || apiKey.isBlank())
        {
            ErrorResponse errorResponse = new ErrorResponse("Header api-key is missing");
            return new ResponseBuilderImpl().entity(objectMapper.writeValueAsString(errorResponse))
                    .status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build();
        }


        try
        {
            openBIS = OpeBISFactory.createOpenBIS(apiKey);
            sessionInformation = openBIS.getSessionInformation();
        } catch (Exception ex)
        {
            RoCrateExceptions.throwInstance(RoCrateExceptions.UNAVAILABLE_API_KEY);
        }
        AsyncJobRegistry.AsyncStatus status =
                asyncJobRegistry.poll(sessionInformation.getUserName(), jobId);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();

        if (status == null)
        {
            responseBuilder.status(Response.Status.NOT_FOUND);
            return responseBuilder.build();

        }

        AsyncResult result = null;
        int statusCode = 0;

        if (status.getStatus() == AsyncJobRegistry.Status.COMPLETED)
        {
            IAsyncJob job = status.getJob();
            if (job instanceof ExportJob)
            {
                java.nio.file.Path path = ((ExportJob) job).getResult();

                String fileName = path.getFileName().toString();

                responseBuilder = Response.ok(Files.readAllBytes(path),
                        ((ExportJob) job).getExportType());
                responseBuilder.header("Content-Disposition",  "attachment; filename=\""+ fileName +"\"");
                responseBuilder.type(((ExportJob) job).getExportType());

                return responseBuilder.build();
            } else
            {
                ErrorResponse errorResponse =
                        new ErrorResponse("Unsupported async job type, only export supported here");
                ObjectMapper objectMapper = new ObjectMapper();
                responseBuilder = new ResponseBuilderImpl();
                responseBuilder.status(Response.Status.BAD_REQUEST);
                responseBuilder.type(MediaType.APPLICATION_JSON_TYPE);
            }

        } else if (status.getStatus() == AsyncJobRegistry.Status.FAILED)
        {
            result = new AsyncResult(status.getStatus().toString(),
                    List.of(status.getJob().getException().getMessage() + "\n" + Arrays.stream(
                                    status.getJob().getException().getStackTrace()).toList().stream()
                            .map(x -> x.toString()).collect(
                                    Collectors.joining(","))), null, jobId);
            statusCode = Response.Status.OK.getStatusCode();

        } else
        {

            responseBuilder.status(Response.Status.CONFLICT);
            return responseBuilder.build();
        }
        responseBuilder = Response.ok(objectMapper.writeValueAsString(result));
        responseBuilder.status(statusCode);

        return responseBuilder.build();
    }
}
