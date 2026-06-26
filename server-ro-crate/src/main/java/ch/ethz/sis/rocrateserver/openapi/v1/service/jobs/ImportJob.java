package ch.ethz.sis.rocrateserver.openapi.v1.service.jobs;

import ch.eth.sis.rocrate.SchemaFacade;
import ch.eth.sis.rocrate.facade.IMetadataEntry;
import ch.eth.sis.rocrate.facade.IPropertyType;
import ch.eth.sis.rocrate.facade.IType;
import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.operation.IOperationResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.ImportOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.ImportOperationResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.data.ImportData;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.data.ImportFormat;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.options.ImportMode;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.options.ImportOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.operation.AsynchronousOperationExecutionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.operation.AsynchronousOperationExecutionResults;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.operation.OperationExecution;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.operation.OperationExecutionState;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.operation.fetchoptions.OperationExecutionFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.operation.id.IOperationExecutionId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.operation.id.OperationExecutionPermId;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.ethz.sis.openbis.generic.excel.v3.to.ExcelWriter;
import ch.ethz.sis.rocrateserver.exception.RoCrateExceptions;
import ch.ethz.sis.rocrateserver.openapi.v1.service.RoCrateService;
import ch.ethz.sis.rocrateserver.openapi.v1.service.delegates.ImportDelegate;
import ch.ethz.sis.rocrateserver.openapi.v1.service.helper.SessionWorkSpaceManager;
import ch.ethz.sis.rocrateserver.openapi.v1.service.helper.validation.RoCrateSchemaValidation;
import ch.ethz.sis.rocrateserver.openapi.v1.service.helper.validation.ValidationResult;
import ch.ethz.sis.rocrateserver.openapi.v1.service.jobs.importjob.download.SessionWorkSpacveSaving;
import ch.ethz.sis.rocrateserver.openapi.v1.service.params.ImportParams;
import ch.ethz.sis.shared.log.classic.impl.Logger;
import ch.openbis.rocrate.app.reader.RdfToModel;
import ch.openbis.rocrate.app.reader.externalfile.FileDownloader;
import ch.openbis.rocrate.app.reader.externalfile.IFileDownloader;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.entities.AbstractEntity;
import edu.kit.datamanager.ro_crate.reader.FolderReader;
import edu.kit.datamanager.ro_crate.reader.RoCrateReader;
import io.quarkus.logging.Log;

import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class ImportJob implements IAsyncJob
{

    private static final Logger LOG = Logger.getLogger(ImportJob.class);

    private final UUID jobId;

    ImportParams importParams;

    Exception exception;

    InputStream body;

    OpenBIS openBIS;

    boolean validateOnly;

    ImportDelegate.OpenBisImportResult importResult;

    List<Path> pathsForDeletion;

    String username;

    Instant completionOrFailInstant;

    Clock clock;

    public ImportJob(Clock clock, String username, ImportParams importParams, InputStream body,
            OpenBIS openBIS,
            boolean validateOnly)
    {
        this.jobId =  UUID.randomUUID();
        this.username = username;
        this.importParams = importParams;
        this.body = body;
        this.openBIS = openBIS;
        this.validateOnly = validateOnly;
        this.pathsForDeletion = new ArrayList<>();
        this.clock = clock;
    }


    @Override
    public UUID getJobId()
    {
        return this.jobId;
    }

    // keep reference to result

    @Override
    public AsyncJobRegistry.Status getStatus()
    {
        if (this.importResult != null)
        {
            return AsyncJobRegistry.Status.COMPLETED;
        }
        if (this.exception != null)
        {
            return AsyncJobRegistry.Status.FAILED;
        }

        return AsyncJobRegistry.Status.RUNNING;
    }

    @Override
    public String getMimeType()
    {
        return importParams.getContentType();
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
        LOG.debug(String.format("||> DUPA: %s", jobId.toString()));
        try
        {
            LOG.info(String.format("Starting import job: %s", jobId.toString()));
            LOG.info(String.format("Session token: %s", openBIS.getSessionToken()));
            LOG.info(String.format("Received parameters: %s", importParams.toString()));
            RoCrate crate = null;
            SchemaFacade schemaFacade = null;
            List<IType> types = null;
            List<IPropertyType> propertyTypes = null;
            List<IMetadataEntry> entryList = new ArrayList<>();

            try
            {
                crate = getRoCrate(importParams, body);
                schemaFacade = SchemaFacade.of(crate);
                types = schemaFacade.getTypes();
                propertyTypes = schemaFacade.getPropertyTypes();
                if (types == null || propertyTypes == null)
                {
                    throw new IllegalArgumentException(
                            "Types and/or property types missing from crates");
                }

            } catch (Exception e)
            {
                Log.error("Problem for user " + System.getProperty(
                        "user.name")); //platform independent
                LOG.error("Could not open RO-Crate", e);
                RoCrateExceptions.throwInstance(RoCrateExceptions.MALFORMED_INPUT);

            }
            for (IType type : types)
            {
                entryList.addAll(schemaFacade.getEntries(type.getId()));
            }
            LOG.info(String.format("Computed entry list: %s", entryList));

            Function<URL, URL> mapUrl =
                    Optional.ofNullable(System.getenv().get("RO_CRATE_SERVER_LOCAL_DOWNLOAD_PORT"))
                            .map(Integer::parseInt)
                            .map(FileDownloader::getLocalMapping)
                            .orElse(FileDownloader.getRealMapping());
            IFileDownloader fileDownloader = new FileDownloader(
                    mapUrl, new SessionWorkSpacveSaving(importParams.getApiKey()));
            Map<AbstractEntity, Path> stringPathMap = fileDownloader.handleDownloads(crate);

            // Converting ro-crate model to openBIS model
            LOG.info("Converting ro-crate model to openBIS model");
            RdfToModel.ConversionResult conversion =
                    RdfToModel.convert(types, propertyTypes, entryList, "DEFAULT", "DEFAULT",
                            schemaFacade, stringPathMap);
            OpenBisModel model =
                    conversion.openBisModel();
            ValidationResult validationResult =
                    RoCrateSchemaValidation.validate(conversion);
            LOG.info(String.format("Validation result: %s", validationResult));
            if (!validationResult.isOkay())
            {
                importResult = new ImportDelegate.OpenBisImportResult(List.of(), Map.of(),
                        validationResult);
                return;
            }

            LOG.info("Converting openBIS model to excel.");
            // Convert openbis model to openbis excel format for import
            byte[] importExcel = ExcelWriter.convert(ExcelWriter.Format.ZIP_EXPORT, model);
            java.nio.file.Path modelAsExcel;
            modelAsExcel = Path.of(UUID.randomUUID() + ".zip");
            pathsForDeletion.add(modelAsExcel);
            if (validateOnly)
            {
                importResult = new ImportDelegate.OpenBisImportResult(List.of(), Map.of(),
                        validationResult);
                return;
            }
            if (!validationResult.isOkay())
            {
                RoCrateExceptions.throwInstance(RoCrateExceptions.SCHEMA_VALIDATION_FAILED);
            }

            LOG.info(String.format("Writing excel to session workspace: %s", modelAsExcel));
            // Import
            SessionWorkSpaceManager.write(openBIS.getSessionToken(), modelAsExcel,
                    new ByteArrayInputStream(importExcel));
            java.nio.file.Path realPath =
                    SessionWorkSpaceManager.getRealPath(openBIS.getSessionToken(), modelAsExcel);
            LOG.info(String.format("Excel session workspace path: %s",  realPath));
            pathsForDeletion.add(realPath);

            LOG.info("Uploading excel to OpenBIS workspace.");
            openBIS.uploadToSessionWorkspace(realPath);

            ImportData importData = new ImportData();
            importData.setSessionWorkspaceFiles(new String[] { modelAsExcel.toString() });
            importData.setFormat(ImportFormat.EXCEL);
            ImportOperation importOperation = new ImportOperation();
            AsynchronousOperationExecutionOptions asynchronousOperationExecutionOptions =
                    new AsynchronousOperationExecutionOptions();
            importOperation.setImportOptions(getImportOptions(importParams));
            importOperation.setImportData(importData);

            LOG.info("Executing OpenBIS async import.");
            AsynchronousOperationExecutionResults ongoingOperations =
                    (AsynchronousOperationExecutionResults)
                            openBIS.executeOperations(List.of(importOperation),
                                    asynchronousOperationExecutionOptions);

            OperationExecutionFetchOptions ongoingOperationsFetchOptions =
                    new OperationExecutionFetchOptions();
            ongoingOperationsFetchOptions.withDetails();
            ongoingOperationsFetchOptions.withNotification();
            ongoingOperationsFetchOptions.withOwner();
            ongoingOperationsFetchOptions.withSummary();
            ongoingOperationsFetchOptions.withSummary().withError();
            ongoingOperationsFetchOptions.withDetails().withResults();
            ongoingOperationsFetchOptions.withSummary().withResults();

            OperationExecutionPermId executionId = ongoingOperations.getExecutionId();
            LOG.info(String.format("Import execution id: %s", executionId));

            boolean isOperationFinished = false;
            while (isOperationFinished == false)
            {
                Map<IOperationExecutionId, OperationExecution> operationExecutions =
                        openBIS.getOperationExecutions(List.of(executionId),
                                ongoingOperationsFetchOptions);
                OperationExecution operationExecution = operationExecutions.get(executionId);
                LOG.info(String.format("Import %s status: %s", executionId, operationExecution.getState()));
                isOperationFinished =
                        operationExecution.getState() == OperationExecutionState.FINISHED || operationExecution.getState() == OperationExecutionState.FAILED;
                if (operationExecution.getState() == OperationExecutionState.FAILED)
                {
                    isOperationFinished = true;
                    LOG.error(String.format("OpenBIS import %s failed: %s", executionId, operationExecution.getSummary().getError()));
                    this.exception =
                            new RuntimeException(operationExecution.getSummary().getError());

                }
                Thread.sleep(2000);
            }
            if (this.exception != null)
            {
                return;
            }

            Map<IOperationExecutionId, OperationExecution> operationExecutions =
                    openBIS.getOperationExecutions(List.of(executionId),
                            ongoingOperationsFetchOptions);
            OperationExecution operationExecution = operationExecutions.get(executionId);
            IOperationResult iOperationResult =
                    operationExecution.getDetails().getResults().stream().findFirst()
                            .orElseThrow();
            ImportOperationResult importOperationResult = (ImportOperationResult) iOperationResult;
            LOG.error(String.format("OpenBIS import %s success", executionId));

            this.importResult = new ImportDelegate.OpenBisImportResult(
                    importOperationResult.getImportResult().getObjectIds().stream()
                            .map(id -> id.toString()).toList(),
                    model.getExternalToOpenBisIdentifiers(), validationResult);

        } catch (Exception e)
        {
            LOG.error("Exception during import", e);
            this.exception = e;
        } catch (Error e) {
            Log.error("Error during import", e);
            this.exception = new UserFailureException(e.getMessage());
        } finally
        {
            LOG.info(String.format("Import job finished: %s", jobId.toString()));
            this.completionOrFailInstant = clock.instant();
        }
    }

    public static RoCrate getRoCrate(ImportParams headers, InputStream body) throws IOException
    {
        RoCrate crate = null;
        if (headers.getContentType().contains(RoCrateService.APPLICATION_LD_JSON))
        {
            // Unpack ro-crate
            Path roCrateMetadata = Path.of("ro-crate-metadata.json");
            SessionWorkSpaceManager.write(headers.getApiKey(), roCrateMetadata, body);

            // Reading ro-crate model
            RoCrateReader roCrateFolderReader = new RoCrateReader(new FolderReader());
            String realPath =
                    SessionWorkSpaceManager.getRealPath(headers.getApiKey(), null).toString();
            LOG.debug(String.format("Crate location %s",
                    realPath));

            crate = roCrateFolderReader.readCrate(
                    realPath);
        } else if (headers.getContentType().contains("application/zip"))
        {

            UUID uuid = UUID.randomUUID();
            Path path = Path.of(uuid + ".zip");
            byte[] buffer = new byte[1024];
            LOG.debug("Path: " + path.toString());
            SessionWorkSpaceManager.write(headers.getApiKey(), path, body);
            Path realPath1 = SessionWorkSpaceManager.getRealPath(headers.getApiKey(), path);
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(realPath1.toString())))
            {
                ZipEntry nextEntry = zis.getNextEntry();
                File destDir = new File(SessionWorkSpaceManager.getRealPath(headers.getApiKey(),
                        Path.of(uuid.toString())).toString());

                while (nextEntry != null)
                {
                    File newFile = newFile(destDir, nextEntry);
                    if (nextEntry.isDirectory())
                    {
                        if (!newFile.isDirectory() && !newFile.mkdirs())
                        {
                            throw new IOException("Failed to create directory " + newFile);
                        }
                    } else
                    {
                        // fix for Windows-created archives
                        File parent = newFile.getParentFile();
                        if (!parent.isDirectory() && !parent.mkdirs())
                        {
                            throw new IOException("Failed to create directory " + parent);
                        }

                        // write file content
                        FileOutputStream fos = new FileOutputStream(newFile);
                        int len;
                        while ((len = zis.read(buffer)) > 0)
                        {
                            fos.write(buffer, 0, len);
                        }
                        fos.close();
                    }
                    nextEntry = zis.getNextEntry();

                }

                RoCrateReader roCrateReader = new RoCrateReader(new FolderReader());
                Path realPath = SessionWorkSpaceManager.getRealPath(headers.getApiKey(),
                        Path.of(uuid.toString()));
                LOG.debug(String.format("Crate location %s",
                        realPath));
                if (realPath.toString().startsWith("./"))
                {
                    LOG.error("How did this happen?");
                }

                crate = roCrateReader.readCrate(
                        realPath.toString());
            } catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
        return crate;
    }

    public ImportDelegate.OpenBisImportResult getResult()
    {
        return importResult;
    }

    private static ImportOptions getImportOptions(ImportParams importParams)
    {
        ImportOptions importOptions = new ImportOptions();
        importOptions.setMode(ImportMode.valueOf(importParams.getImportMode()));
        return importOptions;
    }

    // https://www.baeldung.com/java-compress-and-uncompress
    public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException
    {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator))
        {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

    public void delete() throws IOException
    {
        for (Path path : pathsForDeletion)
        {
            SessionWorkSpaceManager.delete(path);
        }

    }

    ;

    public boolean isValidateOnly()
    {
        return validateOnly;
    }

    public Instant getCompletionOrFailInstant()
    {
        return completionOrFailInstant;
    }
}
