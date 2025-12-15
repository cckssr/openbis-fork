package ch.ethz.sis.rocrateserver.openapi.v1.service.jobs;

import java.io.InputStream;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.http.HttpClientTransportOverHTTP;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.operation.IOperationResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.ExportOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.ExportOperationResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportData;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportableKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportablePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.options.ExportFormat;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.options.ExportOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.options.XlsTextFormat;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.operation.AsynchronousOperationExecutionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.operation.AsynchronousOperationExecutionResults;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.operation.OperationExecution;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.operation.OperationExecutionState;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.operation.fetchoptions.OperationExecutionFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.operation.id.IOperationExecutionId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.operation.id.OperationExecutionPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.SemanticAnnotation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.fetchoptions.SemanticAnnotationFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.search.SemanticAnnotationSearchCriteria;
import ch.ethz.sis.openbis.generic.excel.v3.from.ExcelReader;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.ethz.sis.rocrateserver.exception.RoCrateExceptions;
import ch.ethz.sis.rocrateserver.openapi.v1.service.RoCrateService;
import ch.ethz.sis.rocrateserver.openapi.v1.service.helper.SessionWorkSpaceManager;
import ch.ethz.sis.rocrateserver.openapi.v1.service.params.ExportParams;
import ch.ethz.sis.rocrateserver.startup.RoCrateServerParameter;
import ch.ethz.sis.rocrateserver.startup.StartupMain;
import ch.openbis.rocrate.app.writer.Writer;
import io.quarkus.logging.Log;
import jakarta.ws.rs.HttpMethod;

public final class ExportJob implements IAsyncJob
{

    ExportParams exportParams;

    InputStream result;

    Exception exception;

    InputStream body;

    OpenBIS openBIS;

    String username;

    public ExportJob(ExportParams exportParams, InputStream body, OpenBIS openBIS, String username)
    {
        this.exportParams = exportParams;
        this.body = body;
        this.openBIS = openBIS;
        this.username = username;
    }

    @Override
    public AsyncJobRegistry.Status getStatus()
    {
        if (this.exception != null)
        {
            return AsyncJobRegistry.Status.FAILED;
        }
        if (this.result != null)
        {
            return AsyncJobRegistry.Status.COMPLETED;
        }
        return AsyncJobRegistry.Status.RUNNING;
    }

    @Override
    public String getMimeType()
    {
        return exportParams.getExportMimeType();
    }

    @Override
    public String getUserId()
    {
        return username;
    }

    @Override
    public Exception getException()
    {
        return exception;
    }

    @Override
    public void run()
    {

        try
        {
            String[] identifiers = ExportParams.getIdentifiers(body);
            String[] identifierAnnotations = exportParams.getIdentifierAnnotations();

            // Obtain openBIS Properties annotated with semantic annotations used to hold identifiers
            Set<String> identifierAnnotationPropertyTypeCodes =
                    getIdentifierAnnotationPropertyTypes(openBIS, identifierAnnotations);

            // Search for any openBIS samples holding identifiers on their properties or matching openBIS identifiers or permIds
            SampleSearchCriteria criteria = new SampleSearchCriteria();
            criteria.withOrOperator();

            for (String identifier : identifiers)
            {

                // Semantic Annotation Matching to properties
                for (String propertyTypeCode : identifierAnnotationPropertyTypeCodes)
                {
                    criteria.withStringProperty(propertyTypeCode)
                            .withoutWildcards()
                            .thatEquals(identifier);
                }

                // Could it be an openBIS permId ?
                if (identifier.contains("-") && !identifier.contains("/"))
                {
                    criteria.withPermId().withoutWildcards().thatEquals(identifier);
                }

                // Could it be an openBIS identifier ?
                if (identifier.contains("/"))
                {
                    criteria.withIdentifier().withoutWildcards().thatEquals(identifier);
                }

            }

            SearchResult<Sample> searchResults =
                    openBIS.searchSamples(criteria, new SampleFetchOptions());

            if (searchResults.getTotalCount() < 1)
            {
                RoCrateExceptions.throwInstance(RoCrateExceptions.NO_RESULTS_FOUND);
            }

            // Request openBIS export for found samples
            ExportData exportData = new ExportData();
            List<ExportablePermId> exportablePermIds =
                    List.of(new ExportablePermId(ExportableKind.SAMPLE,
                            searchResults.getObjects().get(0).getPermId().toString()));

            exportData.setPermIds(exportablePermIds);
            ExportOptions exportOptions = getExportOptions(exportParams);

            ExportOperation exportOperation = new ExportOperation();
            exportOperation.setExportData(exportData);
            exportOperation.setExportOptions(exportOptions);

            AsynchronousOperationExecutionOptions asynchronousOperationExecutionOptions =
                    new AsynchronousOperationExecutionOptions();

            AsynchronousOperationExecutionResults ongoingOperations =
                    (AsynchronousOperationExecutionResults)
                            openBIS.executeOperations(
                                    List.of(exportOperation),
                                    asynchronousOperationExecutionOptions);

            OperationExecutionFetchOptions ongoingOperationsFetchOptions =
                    new OperationExecutionFetchOptions();
            ongoingOperationsFetchOptions.withDetails();
            ongoingOperationsFetchOptions.withNotification();
            ongoingOperationsFetchOptions.withOwner();
            ongoingOperationsFetchOptions.withSummary();
            ongoingOperationsFetchOptions.withSummary().withError();
            ongoingOperationsFetchOptions.withSummary().withResults();
            ongoingOperationsFetchOptions.withDetails().withResults();

            OperationExecutionPermId executionId = ongoingOperations.getExecutionId();

            boolean isOperationFinished = false;
            while (isOperationFinished == false)
            {
                Map<IOperationExecutionId, OperationExecution> operationExecutions =
                        openBIS.getOperationExecutions(List.of(executionId),
                                ongoingOperationsFetchOptions);
                OperationExecution operationExecution = operationExecutions.get(executionId);

                if (operationExecution.getState() == OperationExecutionState.FAILED)
                {
                    Log.error(operationExecution.getSummary().getError());
                    this.exception =
                            new RuntimeException(operationExecution.getSummary().getError());
                    isOperationFinished = true;

                }

                if (operationExecution.getState() == OperationExecutionState.FINISHED)
                {
                    isOperationFinished = true;
                    IOperationResult iOperationResult =
                            operationExecution.getDetails().getResults().stream().findFirst()
                                    .orElseThrow();
                    ExportOperationResult exportOperationResult =
                            (ExportOperationResult) iOperationResult;
                    String downloadURL = exportOperationResult.getExportResult().getDownloadURL();
                    System.out.println("Download url: " + downloadURL);

                    java.nio.file.Path realPathToExcel =
                            downloadExcel(openBIS, exportParams, downloadURL);

                    // Convert openBIS Excel to Ro-Crate
                    OpenBisModel openBisModel =
                            ExcelReader.convert(ExcelReader.Format.EXCEL, realPathToExcel);

                    Writer writer = new Writer();
                    java.nio.file.Path tempRoCratePath =
                            java.nio.file.Path.of("result-crate" + UUID.randomUUID() + ".zip");
                    java.nio.file.Path realTempRoCratePath =
                            SessionWorkSpaceManager.getRealPath(exportParams.getApiKey(),
                                    tempRoCratePath);
                    writer.write(openBisModel, realTempRoCratePath);

                    if (exportParams.getExportMimeType().equals(RoCrateService.APPLICATION_LD_JSON))
                    {
                        ZipFile zipFile = new ZipFile(realTempRoCratePath.toFile());
                        ZipEntry zipEntry = zipFile.getEntry("ro-crate-metadata.json");
                        this.result = zipFile.getInputStream(zipEntry);

                    }
                }
                Thread.sleep(2000);
            }

            // Download of openBIS export
            // TODO: Extract this download to a separate method and deal with data as streams not as arrays

        } catch (Exception e)
        {

            Log.error("Exception during export", e);
            this.exception = e;
        }

    }

    private static java.nio.file.Path downloadExcel(OpenBIS openBIS, ExportParams headers,
            String downloadUrl) throws Exception
    {
        SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
        sslContextFactory.setTrustAll(true);
        ClientConnector clientConnector = new ClientConnector();
        clientConnector.setSslContextFactory(sslContextFactory);
        clientConnector.setIdleTimeout(Duration.ofMillis(StartupMain.getConfiguration()
                .getIntegerProperty(RoCrateServerParameter.openBISTimeout)));
        HttpClient httpClient = new HttpClient(new HttpClientTransportOverHTTP(clientConnector));
        httpClient.start();

        InputStreamResponseListener listener = new InputStreamResponseListener();

        Request request =
                httpClient.newRequest(
                                downloadUrl)
                        .headers(httpFields -> httpFields.add("sessionToken",
                                openBIS.getSessionToken()));
        request.method(HttpMethod.GET);
        request.send(listener);
        System.out.println("Got a response!:");

        // Write openBIS export to disk
        // TODO: Extract this part with previous
        java.nio.file.Path pathToExcel = java.nio.file.Path.of("metadata.xlsx");
        SessionWorkSpaceManager.write(headers.getApiKey(), pathToExcel, listener.getInputStream());
        java.nio.file.Path realPathToExcel =
                SessionWorkSpaceManager.getRealPath(headers.getApiKey(), pathToExcel);
        return realPathToExcel;

    }

    private Set<String> getIdentifierAnnotationPropertyTypes(
            OpenBIS v3,
            String[] identifierAnnotations)
    {
        SemanticAnnotationSearchCriteria criteria = new SemanticAnnotationSearchCriteria();
        criteria.withOrOperator();
        for (String identifierAnnotation : identifierAnnotations)
        {
            criteria.withPredicateAccessionId().thatEquals(identifierAnnotation);
        }

        SemanticAnnotationFetchOptions options = new SemanticAnnotationFetchOptions();
        options.withPropertyAssignment().withPropertyType();
        options.withPropertyType();

        SearchResult<SemanticAnnotation> apiResults =
                v3.searchSemanticAnnotations(criteria, options);
        Set<String> results = new HashSet<>(apiResults.getTotalCount());
        for (SemanticAnnotation annotation : apiResults.getObjects())
        {
            if (annotation.getPropertyAssignment() != null && annotation.getPropertyAssignment()
                    .getPropertyType() != null)
            {
                results.add(annotation.getPropertyAssignment().getPropertyType().getCode());
            } else if (annotation.getPropertyType() != null)
            {
                results.add(annotation.getPropertyType().getCode());
            }
        }
        return results;
    }

    private static ExportOptions getExportOptions(ExportParams exportParams)
    {
        ExportOptions exportOptions = new ExportOptions();

        // Mandatory, non-optional for ro-crate exports
        exportOptions.setWithImportCompatibility(true);
        exportOptions.setWithReferredTypes(true);
        exportOptions.setXlsTextFormat(XlsTextFormat.RICH);
        exportOptions.setWithLevelsAbove(true);
        exportOptions.setFormats(Set.of(ExportFormat.XLSX));
        exportOptions.setZipSingleFiles(false);

        // Defaults, could be overridden with options
        exportOptions.setWithLevelsBelow(exportParams.isWithLevelsBelow());
        exportOptions.setWithObjectsAndDataSetsParents(
                exportParams.isWithObjectsAndDataSetsParents());
        exportOptions.setWithObjectsAndDataSetsOtherSpaces(
                exportParams.isWithObjectsAndDataSetsOtherSpaces());

        return exportOptions;
    }

    public InputStream getResult()
    {
        return result;
    }
}
