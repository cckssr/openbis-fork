package ch.ethz.sis.rocrateserver.openapi.v1.service.jobs;

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
import ch.ethz.sis.rocrateserver.startup.Configuration;
import ch.ethz.sis.rocrateserver.startup.RoCrateServerParameter;
import ch.ethz.sis.rocrateserver.startup.StartupMain;
import ch.openbis.rocrate.app.writer.Writer;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.common.mail.EMailAddress;
import ch.systemsx.cisd.common.mail.MailClient;
import ch.systemsx.cisd.common.mail.MailClientParameters;
import io.quarkus.logging.Log;
import jakarta.ws.rs.HttpMethod;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.InputStreamResponseListener;
import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.client.transport.HttpClientTransportOverHTTP;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class ExportJob implements IAsyncJob
{
    private static final Logger LOG = Logger.getLogger(ExportJob.class);

    ExportParams exportParams;

    Path result;

    Exception exception;

    InputStream body;

    OpenBIS openBIS;

    String username;

    private final String email;

    int retry_count = 0;

    private final UUID jobId;

    private static final int MAX_RETRIES = 3;


    public ExportJob(ExportParams exportParams, InputStream body, OpenBIS openBIS, String username)
    {
        this.jobId = UUID.randomUUID();
        this.exportParams = exportParams;
        this.body = body;
        this.openBIS = openBIS;
        if(exportParams.getSendEmail()) {
            this.email = openBIS.getSessionInformation().getPerson().getEmail();
            LOG.debug(String.format("Send email flag is true, configured email: %s", this.email));
        } else {
            this.email = null;
        }
        this.username = username;
    }

    @Override
    public UUID getJobId()
    {
        return this.jobId;
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

    public String getExportType()
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
            LOG.info(String.format("Starting export job: %s", jobId.toString()));
            LOG.info(String.format("Received parameters: %s", exportParams.toString()));
            Map<String, String> identifiers[] = ExportParams.getIdentifiers(exportParams.getInputBodyFormat(), body);
            String[] identifierAnnotations = exportParams.getIdentifierAnnotations();

            List<ExportablePermId> exportablePermIds = new ArrayList<>();
            if(exportParams.getInputBodyFormat() != null &&  exportParams.getInputBodyFormat().equalsIgnoreCase("json")) {
                for(Map<String, String> ids : identifiers) {
                    String kindStr = ids.get("kind");
                    String permId = ids.get("permId");

                    if(kindStr != null){
                        ExportableKind kind = ExportableKind.valueOf(kindStr);
                        exportablePermIds.add(new ExportablePermId(kind, permId));
                    }
                }

            } else {
                // Obtain openBIS Properties annotated with semantic annotations used to hold identifiers
                Set<String> identifierAnnotationPropertyTypeCodes =
                        getIdentifierAnnotationPropertyTypes(openBIS, identifierAnnotations);

                // Search for any openBIS samples holding identifiers on their properties or matching openBIS identifiers or permIds
                SampleSearchCriteria criteria = new SampleSearchCriteria();
                criteria.withOrOperator();

                for (Map<String, String> identifierElement : identifiers)
                {
                    String identifier = identifierElement.get("permId");
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
                    LOG.error("No results for [" + Arrays.stream(identifiers).map(x -> x.get("permId"))
                            .collect(Collectors.joining(", ")) + "]");
                    RoCrateExceptions.throwInstance(RoCrateExceptions.NO_RESULTS_FOUND);
                }

                exportablePermIds =
                        List.of(new ExportablePermId(ExportableKind.SAMPLE,
                                searchResults.getObjects().get(0).getPermId().toString()));
            }


            // Request openBIS export for found samples
            ExportData exportData = new ExportData();
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
                OperationExecution operationExecution;
                try
                {
                    Map<IOperationExecutionId, OperationExecution> operationExecutions =
                            openBIS.getOperationExecutions(List.of(executionId),
                                    ongoingOperationsFetchOptions);
                    operationExecution = operationExecutions.get(executionId);
                } catch (RuntimeException e)
                {

                    retry_count++;
                    if (retry_count >= MAX_RETRIES)
                    {
                        LOG.error("Too many retries", e);
                        this.exception = e;
                        isOperationFinished = true;
                    }
                    continue;
                }

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
                    LOG.info("Download url: " + downloadURL);

                    java.nio.file.Path realPathToExcel = null;
                    java.nio.file.Path downloadPath =
                            downloadOpenBISExport(openBIS, exportParams, downloadURL);

                    final String downloadedFileName = downloadPath.toFile().getName();
                    LOG.info(String.format("Downloaded OpenBIS export file: %s", downloadedFileName));

                    OpenBisModel openBisModel =
                            ExcelReader.convert(ExcelReader.Format.ZIP_EXPORT, downloadPath,
                                    ExcelReader.FileMode.DUMMY);

                    Path resultZipPath =
                            SessionWorkSpaceManager.getRealPath(exportParams.getApiKey(),
                                    Path.of("result-crate.zip"));
                    File zipOut = resultZipPath.toFile();
                    Path roCrateFolderPath = SessionWorkSpaceManager.getRealPath(exportParams.getApiKey(),
                            Path.of("ro-crate-metadata"));

                    LOG.debug(String.format("Converted export to RO-Crate, about to store result in: %s", roCrateFolderPath));
                    Writer writer = new Writer();
                    writer.write(openBisModel, roCrateFolderPath);

                    Path roCrateJsonPath = SessionWorkSpaceManager.getRealPath(exportParams.getApiKey(),
                            Path.of("ro-crate-metadata", "ro-crate-metadata.json"));
                    File roCrateFile = roCrateJsonPath.toFile();

                    LOG.info(String.format("RO-Crate file path: %s", roCrateFile));

                    if (exportParams.getExportMimeType().equalsIgnoreCase(RoCrateService.APPLICATION_LD_JSON))
                    {
                        this.result = roCrateJsonPath;
                    } else if (exportParams.getExportMimeType().equalsIgnoreCase(RoCrateService.APPLICATION_ZIP))
                    {
                        byte[] buffer = new byte[8192];
                        if(downloadedFileName.endsWith(".xlsx")) {
                            try (final ZipArchiveOutputStream zos = new ZipArchiveOutputStream(zipOut)) {
                                addFileToZip(zos, realPathToExcel.toFile(), buffer);
                                addFileToZip(zos, roCrateFile, buffer);
                            }
                        } else if(downloadedFileName.endsWith(".zip")) {
                            File zipIn = downloadPath.toFile();


                            try (ZipFile downloadedZip = new ZipFile(zipIn);
                                ZipArchiveOutputStream zos = new ZipArchiveOutputStream(zipOut))
                            {
                                // Copy existing entries
                                Enumeration<? extends ZipEntry> entries = downloadedZip.entries();
                                while (entries.hasMoreElements()) {
                                    ZipEntry entry = entries.nextElement();

                                    if(!exportParams.isFormatXLSX()) {
                                        String entryName = entry.getName();
                                        if(entryName.startsWith("xlsx/")) {
                                            continue;
                                        }
                                    }

                                    ZipArchiveEntry newEntry = new ZipArchiveEntry(entry.getName());

                                    // Preserve metadata
                                    newEntry.setComment(entry.getComment());
                                    newEntry.setTime(entry.getTime());
                                    newEntry.setMethod(entry.getMethod());
                                    newEntry.setSize(entry.getSize());
                                    newEntry.setCrc(entry.getCrc());
                                    newEntry.setCompressedSize(entry.getCompressedSize());
                                    // Add entry header
                                    zos.putArchiveEntry(newEntry);

                                    // Copy raw data directly from the old zip
                                    try (InputStream raw = downloadedZip.getInputStream(entry))
                                    {
                                        int len;
                                        while ((len = raw.read(buffer)) != -1) {
                                            zos.write(buffer, 0, len);
                                        }
                                    }
                                    zos.closeArchiveEntry();
                                }

                                // add the new file (this will be compressed normally)
                                addFileToZip(zos, roCrateFile, buffer);
                            }
                        }
                        LOG.info(String.format("Export successful for export job: %s", jobId.toString()));
                        this.result = resultZipPath;
                        if(this.email != null && !this.email.isBlank()) {
                            LOG.info(String.format("Preparing to send email to: %s", this.email));
                            sendMailSuccess();
                        } else {
                            LOG.info("No email has been found, skipping email sending.");
                        }
                    } else {
                      throw new UserFailureException(String.format("Unsupported mime type: %s", exportParams.getExportMimeType()));
                    }
                }
                Thread.sleep(2000);
            }

        } catch (Exception e)
        {
            if(this.email != null && !this.email.isBlank()) {
                LOG.info("Export failed, preparing to send email");
                sendMailFailure(e);
            }
            Log.error("Exception during export", e);
            this.exception = e;
        }

    }

    private MailClient createMailClient() {
        Configuration configuration = StartupMain.getConfiguration();
        final String mailFrom = configuration
                .getStringProperty(RoCrateServerParameter.mailFrom);
        final String mailSmtpHost = configuration
                .getStringProperty(RoCrateServerParameter.mailSmtpHost);
        final String mailSmtpUser = configuration
                .getStringProperty(RoCrateServerParameter.mailSmtpUser);
        final String mailSmtpPassword = configuration
                .getStringProperty(RoCrateServerParameter.mailSmtpPassword);

        MailClientParameters mailClientParameters = new MailClientParameters();
        mailClientParameters.setFrom(mailFrom);
        mailClientParameters.setSmtpHost(mailSmtpHost);
        mailClientParameters.setSmtpUser(mailSmtpUser);
        mailClientParameters.setSmtpPassword(mailSmtpPassword);

        return new MailClient(mailClientParameters);
    }

    private void sendMailFailure(Exception exception) {
        try {
            MailClient mailClient = createMailClient();
            EMailAddress recipient = new EMailAddress(this.email);
            final String subject = "openBIS RoCrate Export failed!";
            String content = String.format("Error during export: %s", exception.getMessage());
            Log.info("Sending email to: " + recipient + "\nContent:" + content);
            mailClient.sendEmailMessage(subject, content, null, null, recipient);
        } catch (Exception e)
        {
            Log.error("Failed to send failure message", e);
        }
    }

    private void sendMailSuccess() {
        MailClient mailClient = createMailClient();
        EMailAddress recipient = new EMailAddress(this.email);
        final String subject = "openBIS RoCrate Export Download Ready";

        String roCratePublicUrl = openBIS.getServerInformation().get("server-public-information.ro-crate-server.url");
        String content = roCratePublicUrl + "/download?jobId=" + encode(this.jobId.toString()) + "&apiKey=" + encode(this.exportParams.getApiKey());
        Log.info("Sending email to: " + recipient + "\nContent:" + content);
        mailClient.sendEmailMessage(subject, content, null, null, recipient);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static void addFileToZip(ZipArchiveOutputStream zos, File fileToAdd, byte[] commonBuffer) throws IOException
    {
        ZipArchiveEntry newFileEntry = new ZipArchiveEntry(fileToAdd.getName());
        newFileEntry.setTime(fileToAdd.lastModified());
        zos.putArchiveEntry(newFileEntry);

        try (InputStream raw = new FileInputStream(fileToAdd))
        {
            int len;
            while ((len = raw.read(commonBuffer)) != -1) {
                zos.write(commonBuffer, 0, len);
            }
        }
        zos.closeArchiveEntry();
    }

    private static java.nio.file.Path downloadOpenBISExport(OpenBIS openBIS, ExportParams headers,
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
        LOG.info("Got a response!:");

        // Write openBIS export to disk
        // TODO: Extract this part with previous
        final String filePathSubstring = "filePath=";
        final String fileName = downloadUrl.substring(downloadUrl.indexOf(filePathSubstring) +  filePathSubstring.length());
        java.nio.file.Path pathToExcel = java.nio.file.Path.of(fileName);
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
        String mimeType = exportParams.getExportMimeType();
        if(mimeType.equalsIgnoreCase(RoCrateService.APPLICATION_LD_JSON)) {
            exportOptions.setFormats(Set.of(ExportFormat.XLSX));
            exportOptions.setWithImportCompatibility(true);
        } else {
            exportOptions.setWithImportCompatibility(exportParams.isImportCompatible());
            Set<ExportFormat> formats = new HashSet<>();
            if(exportParams.isFormatPDF()) {
                formats.add(ExportFormat.PDF);
            }
            if(exportParams.isImportDatasetData()) {
                formats.add(ExportFormat.DATA);
            }
            if(exportParams.isImportAfsData()) {
                formats.add(ExportFormat.AFS_DATA);
            }
            //xlsx is required for ro-crate export
            formats.add(ExportFormat.XLSX);
            exportOptions.setFormats(formats);
        }

        exportOptions.setXlsTextFormat(XlsTextFormat.RICH);
        exportOptions.setZipSingleFiles(true);
        exportOptions.setWithReferredTypes(true);

        // Defaults, could be overridden with options
        exportOptions.setWithLevelsAbove(exportParams.isWithLevelsAbove());
        exportOptions.setWithLevelsBelow(exportParams.isWithLevelsBelow());
        exportOptions.setWithObjectsAndDataSetsParents(
                exportParams.isWithObjectsAndDataSetsParents());
        exportOptions.setWithObjectsAndDataSetsOtherSpaces(
                exportParams.isWithObjectsAndDataSetsOtherSpaces());
        exportOptions.setWithObjectsAndDataSetsChildren(
                exportParams.isWithObjectsAndDataSetsChildren());

        return exportOptions;
    }

    public Path getResult()
    {
        return result;
    }
}
