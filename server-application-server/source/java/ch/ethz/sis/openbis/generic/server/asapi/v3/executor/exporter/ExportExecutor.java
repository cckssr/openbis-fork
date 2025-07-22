/*
 *  Copyright ETH 2023 Zürich, Scientific IT Services
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package ch.ethz.sis.openbis.generic.server.asapi.v3.executor.exporter;

import static ch.ethz.sis.openbis.generic.server.FileServiceServlet.DEFAULT_REPO_PATH;
import static ch.ethz.sis.openbis.generic.server.FileServiceServlet.REPO_PATH_KEY;
import static ch.ethz.sis.openbis.generic.server.xls.export.ExportableKind.DATASET;
import static ch.ethz.sis.openbis.generic.server.xls.export.ExportableKind.EXPERIMENT;
import static ch.ethz.sis.openbis.generic.server.xls.export.ExportableKind.MASTER_DATA_EXPORTABLE_KINDS;
import static ch.ethz.sis.openbis.generic.server.xls.export.ExportableKind.PROJECT;
import static ch.ethz.sis.openbis.generic.server.xls.export.ExportableKind.SAMPLE;
import static ch.ethz.sis.openbis.generic.server.xls.export.ExportableKind.SPACE;
import static ch.ethz.sis.openbis.generic.server.xls.export.FieldType.ATTRIBUTE;
import static ch.ethz.sis.openbis.generic.server.xls.export.FieldType.PROPERTY;
import static ch.ethz.sis.openbis.generic.server.xls.export.XLSExport.DATA_DIRECTORY;
import static ch.ethz.sis.openbis.generic.server.xls.export.XLSExport.ExportResult;
import static ch.ethz.sis.openbis.generic.server.xls.export.XLSExport.FILE_SERVICE_SUBDIRECTORY;
import static ch.ethz.sis.openbis.generic.server.xls.export.XLSExport.MISCELLANEOUS_DIRECTORY;
import static ch.ethz.sis.openbis.generic.server.xls.export.XLSExport.SCRIPTS_DIRECTORY;
import static ch.ethz.sis.openbis.generic.server.xls.export.XLSExport.TextFormatting;
import static ch.ethz.sis.openbis.generic.server.xls.export.XLSExport.ZIP_EXTENSION;
import static ch.ethz.sis.openbis.generic.server.xls.export.helper.AbstractXLSExportHelper.FIELD_ID_KEY;
import static ch.ethz.sis.openbis.generic.server.xls.export.helper.AbstractXLSExportHelper.FIELD_TYPE_KEY;
import static ch.systemsx.cisd.common.spring.ExposablePropertyPlaceholderConfigurer.PROPERTY_CONFIGURER_BEAN_NAME;
import static ch.systemsx.cisd.openbis.generic.shared.Constants.DOWNLOAD_URL;
import static ch.ethz.sis.openbis.generic.server.asapi.v3.executor.exporter.ExportPropertiesUtils.BUFFER_SIZE;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import javax.annotation.Resource;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.VocabularyTerm;
import ch.ethz.sis.openbis.generic.server.xls.export.*;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Workbook;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.openhtmltopdf.extend.FSSupplier;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.id.ObjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.ICodeHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IDescriptionHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IEntityType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IEntityTypeHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IExperimentHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IIdentifierHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IModificationDateHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IModifierHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IParentChildrenHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IPermIdHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IPropertiesHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IRegistrationDateHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IRegistratorHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.ISampleHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSetKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSetType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.search.DataSetTypeSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.ExperimentType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.search.ExperimentTypeSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.ExportOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.Attribute;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportData;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.IExportableFields;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.SelectedFields;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.options.ExportFormat;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.options.ExportOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.Person;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.DataType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.id.IPropertyTypeId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleTypeSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.generic.asapi.v3.exceptions.NotFetchedException;
import ch.ethz.sis.openbis.generic.dssapi.v3.IDataStoreServerApi;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.DataSetFile;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.download.DataSetFileDownload;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.download.DataSetFileDownloadOptions;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.download.DataSetFileDownloadReader;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.fetchoptions.DataSetFileFetchOptions;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.id.DataSetFilePermId;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.search.DataSetFileSearchCriteria;
import ch.ethz.sis.openbis.generic.server.FileServiceServlet;
import ch.ethz.sis.openbis.generic.server.asapi.v3.IApplicationServerInternalApi;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.IOperationContext;
import ch.ethz.sis.openbis.generic.server.sharedapi.v3.json.ObjectMapperResource;
import ch.ethz.sis.openbis.generic.server.xls.export.helper.AbstractXLSExportHelper;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.common.logging.LogCategory;
import ch.systemsx.cisd.common.logging.LogFactory;
import ch.systemsx.cisd.common.spring.ExposablePropertyPlaceholderConfigurer;
import ch.systemsx.cisd.openbis.generic.server.CommonServiceProvider;
import ch.systemsx.cisd.openbis.generic.shared.ISessionWorkspaceProvider;

@SuppressWarnings("SizeReplaceableByIsEmpty")
@Component
public class ExportExecutor implements IExportExecutor
{

    public static final String EXPORT_FILE_PREFIX = "export";

    public static final String XLSX_DIRECTORY = "xlsx";

    public static final String PDF_DIRECTORY = "hierarchy";

    public static final String SHARED_SAMPLES_DIRECTORY = "(shared)";

    public static final String HTML_EXTENSION = ".html";

    public static final String PDF_EXTENSION = ".pdf";

    public static final String JSON_EXTENSION = ".json";

    public static final String METADATA_FILE_NAME = "metadata" + XLSExport.XLSX_EXTENSION;

    public static final String METADATA_JSON_FILE_NAME = "metadata" + JSON_EXTENSION;

    static final String NAME_PROPERTY_NAME = "NAME";

    private static final String TYPE_EXPORT_FIELD_KEY = "TYPE";

    private static final Map<ExportableKind, IExportFieldsFinder> FIELDS_FINDER_BY_EXPORTABLE_KIND =
            Map.of(ExportableKind.SAMPLE, new SampleExportFieldsFinder(),
                    ExportableKind.EXPERIMENT, new ExperimentExportFieldsFinder(),
                    ExportableKind.DATASET, new DataSetExportFieldsFinder());

    private static final Set<ExportableKind> TYPE_EXPORTABLE_KINDS = EnumSet.of(ExportableKind.SAMPLE_TYPE, ExportableKind.EXPERIMENT_TYPE,
            ExportableKind.DATASET_TYPE, ExportableKind.VOCABULARY_TYPE, ExportableKind.SPACE, ExportableKind.PROJECT);

    private static final String PYTHON_EXTENSION = ".py";

    private static final String KIND_DOCUMENT_PROPERTY_ID = "Kind";

    private static final String TYPE_DOCUMENT_PROPERTY_ID = "Type";

    private static final Logger OPERATION_LOG = LogFactory.getLogger(LogCategory.OPERATION, ExportExecutor.class);

    /** All characters except the ones we consider safe as a directory name. */
    private static final String UNSAFE_CHARACTERS_REGEXP = "[^\\w $!#%'()+,\\-.;=@\\[\\]^{}_~]";


    @Resource(name = ObjectMapperResource.NAME)
    private ObjectMapper objectMapper;

    @Resource(name = ExposablePropertyPlaceholderConfigurer.PROPERTY_CONFIGURER_BEAN_NAME)
    private ExposablePropertyPlaceholderConfigurer configurer;

    private long dataLimit = -1;

    @Override
    public ExportResult doExport(final IOperationContext context, final ExportOperation operation)
    {
        try
        {
            final ExportData exportData = operation.getExportData();
            final ExportOptions exportOptions = operation.getExportOptions();
            final String sessionToken = context.getSession().getSessionToken();

            /*
             * Collect additional permIds in base to options and execute the export without them after
             */
            final ExportData expandedExportData = XLSExportEntityCollector.collectEntities(
                    CommonServiceProvider.getApplicationServerApi(), sessionToken, exportData, exportOptions);

            return doExport(sessionToken, expandedExportData, exportOptions);
        } catch (final IOException e)
        {
            throw UserFailureException.fromTemplate(e, "IO exception exporting.");
        }
    }

    private ExportResult doExport(final String sessionToken, final ExportData exportData, final ExportOptions exportOptions)
            throws IOException
    {
        final IApplicationServerInternalApi applicationServerApi = CommonServiceProvider.getApplicationServerApi();

        final List<ExportablePermId> exportablePermIds = exportData.getPermIds().stream()
                .map(exportablePermIdDto -> new ExportablePermId(
                        ExportableKind.valueOf(exportablePermIdDto.getExportableKind().name()), exportablePermIdDto.getPermId()))
                .collect(Collectors.toList());
        final Set<ExportableKind> exportableKinds = exportablePermIds.stream()
                .map(ExportablePermId::getExportableKind)
                .collect(Collectors.toSet());

        final IExportableFields fields = exportData.getFields();
        final Map<String, Map<String, List<Map<String, String>>>> exportFields;
        if (fields instanceof SelectedFields)
        {
            final SelectedFields selectedFields = (SelectedFields) fields;
            final Set<IPropertyTypeId> properties = new HashSet<>(selectedFields.getProperties());

            exportFields = exportableKinds.stream().flatMap(exportableKind ->
            {
                final IExportFieldsFinder fieldsFinder = FIELDS_FINDER_BY_EXPORTABLE_KIND.get(exportableKind);
                if (fieldsFinder != null)
                {
                    final Map<String, List<Map<String, String>>> selectedFieldMap =
                            fieldsFinder.findExportFields(properties, applicationServerApi, sessionToken, selectedFields);
                    return Stream.of(new AbstractMap.SimpleEntry<>(exportableKind.name(), selectedFieldMap));
                } else if (TYPE_EXPORTABLE_KINDS.contains(exportableKind))
                {
                    final Map<String, List<Map<String, String>>> selectedAttributesMap = findExportAttributes(exportableKind, selectedFields);
                    return Stream.of(new AbstractMap.SimpleEntry<>(TYPE_EXPORT_FIELD_KEY, selectedAttributesMap));
                } else
                {
                    return Stream.empty();
                }
            }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } else
        {
            exportFields = null;
        }

        return doExport(applicationServerApi, sessionToken,
                exportablePermIds, exportFields, TextFormatting.valueOf(exportOptions.getXlsTextFormat().name()), exportOptions.getFormats(),
                exportOptions.isWithReferredTypes(),
                exportOptions.isWithImportCompatibility(),
                exportOptions.isZipSingleFiles()
        );
    }

    private ExportResult doExport(final IApplicationServerApi api,
            final String sessionToken, final List<ExportablePermId> exportablePermIds,
            final Map<String, Map<String, List<Map<String, String>>>> exportFields, final TextFormatting textFormatting,
            final Set<ExportFormat> exportFormats, final boolean exportReferredMasterData,
            final boolean compatibleWithImport, final boolean zipSingleFiles) throws IOException
    {
        final Collection<String> warnings = new ArrayList<>();

        final boolean hasXlsxFormat = exportFormats.contains(ExportFormat.XLSX);
        final boolean hasHtmlFormat = exportFormats.contains(ExportFormat.HTML);
        final boolean hasPdfFormat = exportFormats.contains(ExportFormat.PDF);
        final boolean hasDataFormat = exportFormats.contains(ExportFormat.DATA);

        final ISessionWorkspaceProvider sessionWorkspaceProvider = CommonServiceProvider.getSessionWorkspaceProvider();
        final File sessionWorkspaceDirectory = sessionWorkspaceProvider.getSessionWorkspace(sessionToken).getCanonicalFile();
        final File exportWorkspaceDirectory = new File(sessionWorkspaceDirectory, UUID.randomUUID().toString());
        final Path exportWorkspaceDirectoryPath = exportWorkspaceDirectory.toPath();
        exportWorkspaceDirectory.mkdir();

        if (hasXlsxFormat)
        {
            exportXlsx(api, sessionToken, exportWorkspaceDirectory, exportablePermIds, exportReferredMasterData, exportFields, textFormatting,
                    compatibleWithImport, warnings);
        }

        if (hasHtmlFormat || hasPdfFormat || hasDataFormat)
        {
            final EntitiesVo entitiesVo = new EntitiesVo(sessionToken, exportablePermIds);

            if (hasPdfFormat || hasHtmlFormat)
            {
                final File docDirectory = new File(exportWorkspaceDirectory, PDF_DIRECTORY);
                mkdirs(docDirectory);

                exportSpacesDoc(sessionToken, exportFields, entitiesVo, exportFormats, docDirectory);
                exportProjectsDoc(sessionToken, docDirectory, entitiesVo, exportFields, exportFormats);
                exportExperimentsDoc(sessionToken, docDirectory, entitiesVo, exportFields, exportFormats);
                exportSamplesDoc(sessionToken, docDirectory, entitiesVo, exportFields, exportFormats);
                exportDataSetsDoc(sessionToken, docDirectory, entitiesVo, exportFields, exportFormats);
            }

            if (hasDataFormat)
            {
                exportData(sessionToken, exportWorkspaceDirectory, entitiesVo, compatibleWithImport);
            }
        }

        final File file = getSingleFile(exportWorkspaceDirectoryPath);
        final String exportWorkspaceDirectoryPathString = exportWorkspaceDirectory.getPath();
        final String timestamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS").format(new Date());
        final String zipFileName = String.format("%s.%s%s", EXPORT_FILE_PREFIX, timestamp, ZIP_EXTENSION);

        final ExportResult exportResult;
        if (zipSingleFiles || file == null)
        {
            final File targetZipFile = new File(sessionWorkspaceDirectory, zipFileName);
            if (targetZipFile.exists())
            {
                targetZipFile.delete();
            }

            zipDirectory(exportWorkspaceDirectoryPathString, targetZipFile);
            exportResult = new ExportResult(getDownloadPath(sessionToken, zipFileName), warnings);
        } else
        {
            final Path filePath = file.toPath();
            final String[] nameAndExtension = splitFileName(filePath.getFileName().toString());
            final Path targetFilePath = Files.move(filePath, Path.of(sessionWorkspaceDirectory.getPath(),
                            String.format("%s.%s%s", nameAndExtension[0], timestamp, nameAndExtension[1])),
                    StandardCopyOption.REPLACE_EXISTING);
            final String fileName = targetFilePath.getFileName().toString();

            exportResult = new ExportResult(getDownloadPath(sessionToken, fileName), warnings);
        }

        deleteDirectory(exportWorkspaceDirectoryPathString);

        return exportResult;
    }

    private static String[] splitFileName(final String fileName)
    {
        final int extensionIndex = fileName.lastIndexOf(".");
        if (extensionIndex < 0)
        {
            // No extension found.
            return new String[] {fileName, ""};
        }

        return new String[] {fileName.substring(0, extensionIndex), fileName.substring(extensionIndex)};
    }

    private String getDownloadPath(final String sessionToken, final String fileName)
    {
        final String protocolWithDomain = configurer.getPropertyValue(DOWNLOAD_URL);
        if (protocolWithDomain == null || protocolWithDomain.isBlank())
        {
            throw new UserFailureException(String.format("The property '%s' is not configured for the application server.", DOWNLOAD_URL));
        }

        return String.format("%s/openbis/openbis/download?sessionID=%s&filePath=%s", protocolWithDomain, sessionToken,
                URLEncoder.encode(fileName, StandardCharsets.UTF_8));
    }

    private static void exportXlsx(final IApplicationServerApi api, final String sessionToken, final File exportWorkspaceDirectory,
            final List<ExportablePermId> exportablePermIds, final boolean exportReferredMasterData,
            final Map<String, Map<String, List<Map<String, String>>>> exportFields,
            final TextFormatting textFormatting, final boolean compatibleWithImport, final Collection<String> warnings) throws IOException
    {
        final XLSExport.PrepareWorkbookResult xlsExportResult = XLSExport.prepareWorkbook(api, sessionToken, exportablePermIds,
                exportReferredMasterData, exportFields, textFormatting, compatibleWithImport);

        final File xlsxDirectory = new File(exportWorkspaceDirectory, XLSX_DIRECTORY);
        mkdirs(xlsxDirectory);

        final Map<String, String> xlsExportScripts = xlsExportResult.getScripts();
        if (!xlsExportScripts.isEmpty())
        {
            exportFiles(xlsExportScripts, new File(xlsxDirectory, SCRIPTS_DIRECTORY), fileName -> fileName + PYTHON_EXTENSION);
        }

        final Map<String, String> valueFiles = xlsExportResult.getValueFiles();
        if (!valueFiles.isEmpty())
        {
            exportFiles(valueFiles, new File(xlsxDirectory, DATA_DIRECTORY), Function.identity());
        }

        final Map<String, byte[]> miscellaneousFiles = xlsExportResult.getMiscellaneousFiles();
        if (!miscellaneousFiles.isEmpty())
        {
            exportBinaryFiles(miscellaneousFiles, new File(xlsxDirectory, MISCELLANEOUS_DIRECTORY + '/' + FILE_SERVICE_SUBDIRECTORY),
                    Function.identity());
        }

        try (
                final Workbook wb = xlsExportResult.getWorkbook();
                final BufferedOutputStream bos = new BufferedOutputStream(
                        new FileOutputStream(new File(xlsxDirectory, METADATA_FILE_NAME)), BUFFER_SIZE);
        )
        {
            wb.write(bos);
        }

        warnings.addAll(xlsExportResult.getWarnings());
    }

    private static void exportFiles(final Map<String, String> fileNameToContentsMap, final File directory,
            final Function<String, String> fileNameTransformer) throws IOException
    {
        mkdirs(directory);
        for (final Map.Entry<String, String> fileNameToContentsEntry : fileNameToContentsMap.entrySet())
        {
            final File scriptFile = new File(directory, fileNameTransformer.apply(fileNameToContentsEntry.getKey()));
            try (final BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(scriptFile), BUFFER_SIZE))
            {
                bos.write(fileNameToContentsEntry.getValue().getBytes());
                bos.flush();
            }
        }
    }

    private static void exportBinaryFiles(final Map<String, byte[]> fileNameToContentsMap, final File directory,
            final Function<String, String> fileNameTransformer) throws IOException
    {
        mkdirs(directory);
        for (final Map.Entry<String, byte[]> fileEntry : fileNameToContentsMap.entrySet())
        {
            final File file = new File(directory, fileNameTransformer.apply(fileEntry.getKey()));
            mkdirs(file.getParentFile());
            try (final BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file), BUFFER_SIZE))
            {
                bos.write(fileEntry.getValue());
                bos.flush();
            }
        }
    }

    private void exportData(final String sessionToken, final File exportWorkspaceDirectory, final EntitiesVo entitiesVo,
            final boolean compatibleWithImport) throws IOException
    {
        final Collection<DataSet> dataSets = entitiesVo.getDataSets();
        final long totalSize = dataSets.stream()
                .flatMap(dataSet -> getDataSetFiles(sessionToken, dataSet).stream())
                .mapToLong(DataSetFile::getFileLength).reduce(0L, Long::sum);

        final long totalDataLimit = getDataLimit();
        if (totalSize > totalDataLimit)
        {
            throw UserFailureException.fromTemplate("Total data size %d is larger than the data limit %d.", totalSize, totalDataLimit);
        }

        exportDataSetsData(sessionToken, exportWorkspaceDirectory, dataSets, compatibleWithImport);
    }

    private long getDataLimit()
    {
        if (dataLimit < 0)
        {
            final String dataLimitString = CommonServiceProvider
                    .tryToGetProperty("export.data-limit", "10737418240");

            try
            {
                dataLimit = Long.parseLong(dataLimitString);
            } catch (final NumberFormatException e)
            {
                throw UserFailureException.fromTemplate(e, "Error parsing string '%s' to long.", dataLimitString);
            }
        }

        return dataLimit;
    }

    private static List<DataSetFile> getDataSetFiles(final String sessionToken, final DataSet dataSet)
    {
        final IDataStoreServerApi v3Dss = CommonServiceProvider.getDataStoreServerApi();
        final String dataSetPermId = dataSet.getPermId().getPermId();

        if (dataSet.getKind() != DataSetKind.LINK)
        {
            final DataSetFileSearchCriteria criteria = new DataSetFileSearchCriteria();
            criteria.withDataSet().withPermId().thatEquals(dataSetPermId);

            final SearchResult<DataSetFile> results = v3Dss.searchFiles(sessionToken, criteria, new DataSetFileFetchOptions());

            OPERATION_LOG.info(String.format("Found: %d files", results.getTotalCount()));

            return results.getObjects();
        } else
        {
            OPERATION_LOG.info(String.format("Omitted data export for link dataset with permId: %s", dataSetPermId));
            return List.of();
        }
    }

    private void exportDataSetsData(final String sessionToken, final File exportWorkspaceDirectory,
            final Collection<DataSet> dataSets, final boolean compatibleWithImport) throws IOException
    {
        final IDataStoreServerApi v3Dss = CommonServiceProvider.getDataStoreServerApi();

        for (final DataSet dataSet : dataSets)
        {
            final ICodeHolder codeHolder = getDataSetHolder(dataSet);
            final String code = codeHolder.getCode();
            final String spaceCode = getSpaceCode(codeHolder);
            final String containerCode = getSampleContainerCode(dataSet);
            final String projectCode = getProjectCode(codeHolder);
            final char prefix = codeHolder instanceof Sample ? 'O' : 'E';

            final File parentDataDirectory = compatibleWithImport
                    ? exportWorkspaceDirectory
                    : createDirectoriesForSampleOrExperiment(prefix, new File(exportWorkspaceDirectory, PDF_DIRECTORY), codeHolder);

            final String dataSetPermId = dataSet.getPermId().getPermId();
            final String dataSetCode = dataSet.getCode();
            final String dataSetTypeCode = dataSet.getType().getCode();
            final String dataSetName = getEntityName(dataSet);
            final String dataDirectorySuffix = "#" + UUID.randomUUID();

            final String datasetJson = datasetToJson(dataSet);

            createMetadataJsonFile(parentDataDirectory, prefix, spaceCode, projectCode, containerCode, code,
                    dataSetTypeCode, dataSetCode, dataSetName, dataDirectorySuffix, datasetJson, compatibleWithImport);

            if (dataSet.getKind() != DataSetKind.LINK)
            {
                final DataSetFileSearchCriteria criteria = new DataSetFileSearchCriteria();
                criteria.withDataSet().withPermId().thatEquals(dataSetPermId);

                final SearchResult<DataSetFile> results = v3Dss.searchFiles(sessionToken, criteria, new DataSetFileFetchOptions());

                OPERATION_LOG.info(String.format("Found: %d files", results.getTotalCount()));

                final List<DataSetFile> dataSetFiles = results.getObjects();
                final List<DataSetFilePermId> fileIds = dataSetFiles.stream().map(DataSetFile::getPermId).collect(Collectors.toList());

                final DataSetFileDownloadOptions options = new DataSetFileDownloadOptions();
                options.setRecursive(true);

                try (final InputStream is = v3Dss.downloadFiles(sessionToken, fileIds, options))
                {
                    final DataSetFileDownloadReader reader = new DataSetFileDownloadReader(is);
                    DataSetFileDownload file;
                    while ((file = reader.read()) != null)
                    {
                        createNextDataFile(parentDataDirectory, prefix, spaceCode, projectCode,
                                containerCode, code, dataSetTypeCode, dataSetCode, dataSetName, dataDirectorySuffix, file, compatibleWithImport);
                    }
                }
            } else
            {
                OPERATION_LOG.info(String.format("Omitted data export for link dataset with permId: %s", dataSetPermId));
            }
        }
    }

    private String datasetToJson(final DataSet dataSet) throws JsonProcessingException
    {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        final ObjectNode propertiesNode = objectMapper.createObjectNode();
        propertiesNode.set("properties", objectMapper.valueToTree(dataSet.getProperties()));

        return objectMapper.writeValueAsString(propertiesNode);
    }

    private static File createDirectoriesForSampleOrExperiment(final char prefix, final File documentDirectory, final ICodeHolder codeHolder)
    {
        if (prefix != 'O' && prefix != 'E')
        {
            throw new IllegalArgumentException(String.format("Only 'O' and 'E' can be used as prefix got '%c' instead.", prefix));
        }

        return prefix == 'O'
                ? createDirectoriesForSample(documentDirectory, (Sample) codeHolder)
                : createDirectoriesForExperiment(documentDirectory, (Experiment) codeHolder);
    }

    private static File createMetadataJsonFile(final File parentDataDirectory, final char prefix,
            final String spaceCode, final String projectCode, final String containerCode, final String code, final String dataSetTypeCode,
            final String dataSetCode, final String dataSetName, final String dataDirectorySuffix, final String codeHolderJson,
            final boolean compatibleWithImport) throws IOException
    {
        final File metadataJsonFile;

        if (compatibleWithImport)
        {
            final File dataDirectory = new File(parentDataDirectory, DATA_DIRECTORY + '/');
            mkdirs(dataDirectory);
            metadataJsonFile = new File(dataDirectory,
                    getDataDirectoryName(prefix, spaceCode, projectCode, containerCode, code, dataSetTypeCode,
                            dataDirectorySuffix, METADATA_JSON_FILE_NAME));
        } else
        {
            final File dataDirectory = new File(parentDataDirectory, getFullEntityName(dataSetCode, dataSetName));
            mkdirs(dataDirectory);
            metadataJsonFile = new File(new File(dataDirectory, DATA_DIRECTORY), METADATA_JSON_FILE_NAME);
        }

        final File dataSubdirectory = metadataJsonFile.getParentFile();
        mkdirs(dataSubdirectory);

        try (final OutputStream os = new BufferedOutputStream(new FileOutputStream(metadataJsonFile)))
        {
            writeInChunks(os, codeHolderJson.getBytes(StandardCharsets.UTF_8));
        }

        return metadataJsonFile;
    }

    private void exportSpacesDoc(final String sessionToken, final Map<String, Map<String, List<Map<String, String>>>> exportFields,
            final EntitiesVo entitiesVo, final Set<ExportFormat> exportFormats, final File docDirectory) throws IOException
    {
        createFilesAndDirectoriesForSpacesOfEntities(sessionToken, docDirectory, entitiesVo.getSpaces(), exportFields, exportFormats);
        createFilesAndDirectoriesForSpacesOfEntities(sessionToken, docDirectory, entitiesVo.getProjects(), exportFields, exportFormats);
        createFilesAndDirectoriesForSpacesOfEntities(sessionToken, docDirectory, entitiesVo.getExperiments(), exportFields, exportFormats);
        createFilesAndDirectoriesForSpacesOfEntities(sessionToken, docDirectory, entitiesVo.getSamples(), exportFields, exportFormats);
    }

    private void createFilesAndDirectoriesForSpacesOfEntities(final String sessionToken, final File docDirectory, final Collection<?> entities,
            final Map<String, Map<String, List<Map<String, String>>>> exportFields, final Set<ExportFormat> exportFormats) throws IOException
    {
        final boolean hasHtmlFormat = exportFormats.contains(ExportFormat.HTML);
        final boolean hasPdfFormat = exportFormats.contains(ExportFormat.PDF);

        for (final Object entity : entities)
        {
            if (entity instanceof Space)
            {
                final Space space = (Space) entity;

                final Map<String, List<Map<String, String>>> entityTypeExportFieldsMap = getEntityTypeExportFieldsMap(exportFields, SPACE);
                final String html = getHtml(sessionToken, space, entityTypeExportFieldsMap);
                final byte[] htmlBytes = html.getBytes(StandardCharsets.UTF_8);

                if (hasHtmlFormat)
                {
                    final File htmlFile = createNextDocFile(docDirectory, space.getCode(), null, null, null, null, null, null, null, HTML_EXTENSION);
                    try (final BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(htmlFile), BUFFER_SIZE))
                    {
                        writeInChunks(bos, htmlBytes);
                        bos.flush();
                    }
                }

                if (hasPdfFormat)
                {
                    final File pdfFile = createNextDocFile(docDirectory, space.getCode(), null, null, null, null, null, null, null, PDF_EXTENSION);
                    buildPdf(pdfFile, html);
                }
            } else
            {
                final String spaceCode = getSpaceCode(entity);
                final String directoryName = spaceCode == null && entity instanceof Sample ? SHARED_SAMPLES_DIRECTORY : spaceCode;
                final File space = createNextDocFile(docDirectory, directoryName, null, null, null, null, null, null, null, null);
                mkdirs(space);
            }
        }
    }

    private void exportProjectsDoc(final String sessionToken, final File docDirectory, final EntitiesVo entitiesVo,
            final Map<String, Map<String, List<Map<String, String>>>> exportFields, final Set<ExportFormat> exportFormats) throws IOException
    {
        createFilesAndDirectoriesForProjectsOfEntities(sessionToken, docDirectory, entitiesVo.getProjects(), exportFields, exportFormats);
        createFilesAndDirectoriesForProjectsOfEntities(sessionToken, docDirectory, entitiesVo.getExperiments(), exportFields, exportFormats);
        createFilesAndDirectoriesForProjectsOfEntities(sessionToken, docDirectory, entitiesVo.getSamples(), exportFields, exportFormats);
    }

    private void createFilesAndDirectoriesForProjectsOfEntities(final String sessionToken, final File docDirectory, final Collection<?> entities,
            final Map<String, Map<String, List<Map<String, String>>>> exportFields, final Set<ExportFormat> exportFormats) throws IOException
    {
        for (final Object entity : entities)
        {
            if (entity instanceof Project)
            {
                final Project project = (Project) entity;
                final Map<String, List<Map<String, String>>> entityTypeExportFieldsMap = getEntityTypeExportFieldsMap(exportFields, PROJECT);
                createDocFilesForEntity(sessionToken, docDirectory, entityTypeExportFieldsMap, project,
                        project.getSpace().getCode(), project.getCode(), null, null, null, null, null, null,
                        exportFormats);
            } else
            {
                final String projectCode = getProjectCode(entity);
                if (projectCode != null)
                {
                    final File space = createNextDocFile(docDirectory, getSpaceCode(entity), projectCode, null, null, null, null, null, null, null);
                    mkdirs(space);
                }
            }
        }
    }

    private void exportExperimentsDoc(final String sessionToken, final File docDirectory, final EntitiesVo entitiesVo,
            final Map<String, Map<String, List<Map<String, String>>>> exportFields, final Set<ExportFormat> exportFormats) throws IOException
    {
        createFilesAndDirectoriesForExperimentsOfEntities(sessionToken, docDirectory, entitiesVo.getExperiments(), exportFields, exportFormats);
        createFilesAndDirectoriesForExperimentsOfEntities(sessionToken, docDirectory, entitiesVo.getSamples(), exportFields, exportFormats);
        createFilesAndDirectoriesForExperimentsOfEntities(sessionToken, docDirectory, entitiesVo.getDataSets(), exportFields, exportFormats);
    }

    private void createFilesAndDirectoriesForExperimentsOfEntities(final String sessionToken, final File docDirectory,
            final Collection<?> entities, final Map<String, Map<String, List<Map<String, String>>>> exportFields,
            final Set<ExportFormat> exportFormats) throws IOException
    {
        for (final Object entity : entities)
        {
            if (entity instanceof IExperimentHolder)
            {
                final Experiment experiment = ((IExperimentHolder) entity).getExperiment();
                if (experiment != null)
                {
                    createDirectoriesForExperiment(docDirectory, experiment);
                }
            }

            if (entity instanceof Experiment)
            {
                final Experiment experiment = (Experiment) entity;
                final Project project = experiment.getProject();
                final Map<String, List<Map<String, String>>> entityTypeExportFieldsMap = getEntityTypeExportFieldsMap(exportFields, EXPERIMENT);
                createDocFilesForEntity(sessionToken, docDirectory, entityTypeExportFieldsMap, experiment,
                        project.getSpace().getCode(), project.getCode(), experiment.getCode(), getEntityName(experiment), null, null, null, null,
                        exportFormats);
            }
        }
    }

    private void exportSamplesDoc(final String sessionToken, final File docDirectory, final EntitiesVo entitiesVo,
            final Map<String, Map<String, List<Map<String, String>>>> exportFields, final Set<ExportFormat> exportFormats)
            throws IOException
    {
        createFilesAndDirectoriesForSamplesOfEntities(sessionToken, docDirectory, entitiesVo.getSamples(), exportFields, exportFormats, true);
    }

    private static Map<String, List<Map<String, String>>> getEntityTypeExportFieldsMap(
            final Map<String, Map<String, List<Map<String, String>>>> exportFields, final ExportableKind exportableKind)
    {
        return exportFields == null
                ? null
                : exportFields.get(MASTER_DATA_EXPORTABLE_KINDS.contains(exportableKind) || exportableKind == SPACE || exportableKind == PROJECT
                ? TYPE_EXPORT_FIELD_KEY : exportableKind.toString());
    }

    private void createFilesAndDirectoriesForSamplesOfEntities(final String sessionToken, final File docDirectory,
            final Collection<?> entities, final Map<String, Map<String, List<Map<String, String>>>> exportFields,
            final Set<ExportFormat> exportFormats, final boolean createFiles) throws IOException
    {
        for (final Object entity : entities)
        {
            if (entity instanceof ISampleHolder)
            {
                final Sample sample = ((ISampleHolder) entity).getSample();
                createDirectoriesForSample(docDirectory, sample);
            }

            if (createFiles && entity instanceof Sample)
            {
                final Sample sample = (Sample) entity;
                final Experiment experiment = sample.getExperiment();
                final Sample container = sample.getContainer();

                final String spaceCode = getSpaceCode(sample);
                final String spaceDirectory = spaceCode != null ? spaceCode : SHARED_SAMPLES_DIRECTORY;
                final Map<String, List<Map<String, String>>> entityTypeExportFieldsMap = getEntityTypeExportFieldsMap(exportFields, SAMPLE);

                createDocFilesForEntity(sessionToken, docDirectory, entityTypeExportFieldsMap, sample,
                        spaceDirectory, getProjectCode(sample), experiment != null ? experiment.getCode() : null,
                        experiment != null ? getEntityName(experiment) : null, container != null ? container.getCode() : null, sample.getCode(),
                        getEntityName(sample), null, exportFormats);
            }
        }
    }

    private static File createDirectoriesForSample(final File parentDirectory, final Sample sample)
    {
        final Experiment experiment = sample.getExperiment();
        final Sample container = sample.getContainer();
        final File docFile;

        if (experiment != null)
        {
            final Project project = experiment.getProject();
            docFile = createNextDocFile(parentDirectory, project.getSpace().getCode(), project.getCode(), experiment.getCode(),
                    getEntityName(experiment), container != null ? container.getCode() : null, sample.getCode(), getEntityName(sample), null, null);
        } else
        {
            final Project project = sample.getProject();
            if (project != null)
            {
                docFile = createNextDocFile(parentDirectory, project.getSpace().getCode(), project.getCode(), null,
                        null, container != null ? container.getCode() : null, sample.getCode(), getEntityName(sample), null, null);
            } else
            {
                final Space space = sample.getSpace();
                docFile = createNextDocFile(parentDirectory, space != null ? space.getCode() : SHARED_SAMPLES_DIRECTORY, null, null,
                        null, container != null ? container.getCode() : null, sample.getCode(), getEntityName(sample), null, null);
            }
        }

        mkdirs(docFile);
        return docFile;
    }

    private static File createDirectoriesForExperiment(final File parentDirectory, final Experiment experiment)
    {
        final Project project = experiment.getProject();
        final File docFile = createNextDocFile(parentDirectory, project.getSpace().getCode(), project.getCode(), experiment.getCode(),
                getEntityName(experiment), null, null, null, null, null);
        mkdirs(docFile);
        return docFile;
    }

    private void exportDataSetsDoc(final String sessionToken, final File docDirectory, final EntitiesVo entitiesVo,
            final Map<String, Map<String, List<Map<String, String>>>> exportFields, final Set<ExportFormat> exportFormats) throws IOException
    {
        createFilesAndDirectoriesForDataSetsOfEntities(sessionToken, docDirectory, entitiesVo.getDataSets(), exportFields, exportFormats);
    }

    private void createFilesAndDirectoriesForDataSetsOfEntities(final String sessionToken, final File docDirectory,
            final Collection<?> entities, final Map<String, Map<String, List<Map<String, String>>>> exportFields,
            final Set<ExportFormat> exportFormats) throws IOException
    {
        for (final Object entity : entities)
        {
            if (entity instanceof DataSet)
            {
                final DataSet dataSet = (DataSet) entity;
                final Sample sample = dataSet.getSample();
                final Sample container = sample != null ? sample.getContainer() : null;
                final Experiment experiment = sample != null ? sample.getExperiment() : dataSet.getExperiment();
                final Map<String, List<Map<String, String>>> entityTypeExportFieldsMap = getEntityTypeExportFieldsMap(exportFields, DATASET);

                createDocFilesForEntity(sessionToken, docDirectory, entityTypeExportFieldsMap, dataSet,
                        getSpaceCode(dataSet), getProjectCode(dataSet), experiment != null ? experiment.getCode() : null,
                        experiment != null ? getEntityName(experiment) : null, container != null ? container.getCode() : null,
                        sample != null ? sample.getCode() : null, sample != null ? getEntityName(sample) : null, dataSet.getCode(), exportFormats
                );
            }
        }
    }

    private static String getSpaceCode(final Object entity)
    {
        if (entity instanceof Space)
        {
            return getSpaceCode((Space) entity);
        } else if (entity instanceof Project)
        {
            return getSpaceCode((Project) entity);
        } else if (entity instanceof Experiment)
        {
            return getSpaceCode((Experiment) entity);
        } else if (entity instanceof Sample)
        {
            return getSpaceCode((Sample) entity);
        } else if (entity instanceof DataSet)
        {
            return getSpaceCode((DataSet) entity);
        } else
        {
            throw new IllegalArgumentException();
        }
    }

    private static String getSpaceCode(final Space entity)
    {
        return entity.getCode();
    }

    private static String getSpaceCode(final Project entity)
    {
        return entity.getSpace().getCode();
    }

    private static String getSpaceCode(final Experiment entity)
    {
        return entity.getProject().getSpace().getCode();
    }

    private static String getSpaceCode(final Sample sample)
    {
        final Space space = sample.getSpace();
        if (space != null)
        {
            return sample.getSpace().getCode();
        } else
        {
            final Experiment experiment = sample.getExperiment();
            final Project project = sample.getProject();
            if (experiment != null)
            {
                return experiment.getProject().getSpace().getCode();
            } else if (project != null)
            {
                return project.getSpace().getCode();
            } else
            {
                return null;
            }
        }
    }

    private static String getSpaceCode(final DataSet dataSet)
    {
        final Sample sample = dataSet.getSample();
        return sample != null ? getSpaceCode(sample) : getSpaceCode(dataSet.getExperiment());
    }

    private static String getProjectCode(final Object entity)
    {
        if (entity instanceof Project)
        {
            return getProjectCode((Project) entity);
        } else if (entity instanceof Experiment)
        {
            return getProjectCode((Experiment) entity);
        } else if (entity instanceof Sample)
        {
            return getProjectCode((Sample) entity);
        } else if (entity instanceof DataSet)
        {
            return getProjectCode((DataSet) entity);
        } else
        {
            throw new IllegalArgumentException();
        }
    }

    private static String getProjectCode(final Project project)
    {
        return project.getCode();
    }

    private static String getProjectCode(final Experiment experiment)
    {
        return experiment.getProject().getCode();
    }

    private static String getProjectCode(final Sample sample)
    {
        final Project project = getProjectForSample(sample);
        return project != null ? project.getCode() : null;
    }

    private static String getProjectCode(final DataSet dataSet)
    {
        final Sample sample = dataSet.getSample();
        return sample != null ? getProjectCode(sample) : getProjectCode(dataSet.getExperiment());
    }

    private static Project getProjectForSample(final Sample sample)
    {
        final Experiment experiment = sample.getExperiment();
        if (experiment != null)
        {
            return experiment.getProject();
        } else
        {
            return sample.getProject();
        }
    }

    private static ICodeHolder getDataSetHolder(final DataSet dataSet)
    {
        final Sample sample = dataSet.getSample();
        return sample != null ? sample : dataSet.getExperiment();
    }

    private static String getSampleContainerCode(final DataSet dataSet)
    {
        final Sample sample = dataSet.getSample();
        if (sample != null)
        {
            final Sample container = sample.getContainer();
            return container != null ? container.getCode() : null;
        } else
        {
            return null;
        }
    }

    private static void writeInChunks(final OutputStream os, final byte[] bytes) throws IOException
    {
        final int length = bytes.length;
        for (int pos = 0; pos < length; pos += BUFFER_SIZE)
        {
            os.write(Arrays.copyOfRange(bytes, pos, Math.min(pos + BUFFER_SIZE, length)));
        }
        os.flush();
    }

    private static void writeInChunks(final OutputStream os, final InputStream is) throws IOException
    {
        final byte[] buffer = new byte[BUFFER_SIZE];
        int length;
        while ((length = is.read(buffer)) > 0)
        {
            os.write(buffer, 0, length);
        }
        os.flush();
    }

    private static File createNextDocFile(final File docDirectory, final String spaceCode, final String projectCode, final String experimentCode,
            final String experimentName, final String containerCode, final String sampleCode, final String sampleName, final String dataSetCode,
            final String extension)
    {
        final File docFile = new File(docDirectory, getNextDocDirectoryName(spaceCode, projectCode, experimentCode, experimentName, containerCode,
                sampleCode, sampleName, dataSetCode, extension));
        docFile.getParentFile().mkdirs();
        return docFile;
    }

    private void createDocFilesForEntity(final String sessionToken, final File docDirectory,
            final Map<String, List<Map<String, String>>> entityTypeExportFieldsMap,
            final ICodeHolder entity, final String spaceCode, final String projectCode, final String experimentCode,
            final String experimentName, final String containerCode, final String sampleCode, final String sampleName, final String dataSetCode,
            final Set<ExportFormat> exportFormats) throws IOException
    {
        final boolean hasHtmlFormat = exportFormats.contains(ExportFormat.HTML);
        final boolean hasPdfFormat = exportFormats.contains(ExportFormat.PDF);
        final String html = getHtml(sessionToken, entity, entityTypeExportFieldsMap);
        final byte[] htmlBytes = html.getBytes(StandardCharsets.UTF_8);

        if (hasHtmlFormat)
        {
            final File htmlFile = createNextDocFile(docDirectory, spaceCode, projectCode, experimentCode, experimentName, containerCode, sampleCode,
                    sampleName, dataSetCode, HTML_EXTENSION);
            try (final BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(htmlFile), BUFFER_SIZE))
            {
                writeInChunks(bos, htmlBytes);
                bos.flush();
            }
        }

        if (hasPdfFormat)
        {
            final File pdfFile = createNextDocFile(docDirectory, spaceCode, projectCode, experimentCode, experimentName, containerCode, sampleCode,
                    sampleName, dataSetCode, PDF_EXTENSION);
            buildPdf(pdfFile, html);
        }
    }

    private void createDocFilesForDataSet(final String sessionToken, final File docDirectory,
            final DataSet dataSet, final Set<ExportFormat> exportFormats) throws IOException
    {
        final boolean hasHtmlFormat = exportFormats.contains(ExportFormat.HTML);
        final boolean hasPdfFormat = exportFormats.contains(ExportFormat.PDF);
        final String html = getHtml(sessionToken, dataSet, null);
        final byte[] htmlBytes = html.getBytes(StandardCharsets.UTF_8);

        if (hasHtmlFormat)
        {
            final File htmlFile = new File(docDirectory, dataSet.getCode() + HTML_EXTENSION);
            try (final BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(htmlFile), BUFFER_SIZE))
            {
                writeInChunks(bos, htmlBytes);
                bos.flush();
            }
        }

        if (hasPdfFormat)
        {
            final File pdfFile = new File(docDirectory, dataSet.getCode() + PDF_EXTENSION);
            buildPdf(pdfFile, html);
        }
    }

    private static void buildPdf(final File pdfFile, final String html) throws IOException
    {
        try (final BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(pdfFile), BUFFER_SIZE))
        {
            final PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFont(new FSSupplier<InputStream>()
            {
                @Override
                public InputStream supply()
                {
                    return ExportPDFUtils.class.getResourceAsStream("OpenSans-Regular.ttf");
                }
            }, "OpenSans");
            builder.useFont(new FSSupplier<InputStream>()
            {
                @Override
                public InputStream supply()
                {
                    return ExportPDFUtils.class.getResourceAsStream("NotoSansMath-Regular.ttf");
                }
            }, "NotoSansMath");
            builder.useFont(new FSSupplier<InputStream>()
            {
                @Override
                public InputStream supply()
                {
                    return ExportPDFUtils.class.getResourceAsStream("NotoEmoji-Regular.ttf");
                }
            }, "NotoEmoji");

            String replacedHtml = ExportPDFUtils.addStyleHeader(html);
            replacedHtml = ExportPDFUtils.replaceHSLToHex(replacedHtml, "color", ExportPDFUtils.hslColorPattern);
            replacedHtml = ExportPDFUtils.insertPagePagebreak(replacedHtml, "<h2>Identification Info</h2>");
            Document replacedHtmlDoc = Jsoup.parse(replacedHtml);
            builder.useFastMode().withW3cDocument(new W3CDom().fromJsoup(replacedHtmlDoc), null).toStream(bos).run();
        }
    }

    static String getNextDocDirectoryName(final String spaceCode, final String projectCode, final String experimentCode, final String experimentName,
            final String containerCode, final String sampleCode, final String sampleName, final String dataSetCode, final String extension)
    {
        final StringBuilder entryBuilder = new StringBuilder();

        if (spaceCode == null && (projectCode != null || experimentCode != null || dataSetCode != null || (sampleCode == null && extension != null)))
        {
            throw new IllegalArgumentException();
        } else if (spaceCode != null)
        {
            entryBuilder.append(spaceCode);
        }

        if (projectCode != null)
        {
            entryBuilder.append('/').append(projectCode);
            if (experimentCode != null)
            {
                entryBuilder.append('/');
                addFullEntityName(entryBuilder, null, experimentCode, experimentName);

                if (sampleCode == null)
                {
                    if (dataSetCode != null)
                    {
                        // Experiment data set
                        entryBuilder.append('/').append(dataSetCode);
                    } else if (extension != null)
                    {
                        entryBuilder.append('/');
                        addFullEntityName(entryBuilder, null, experimentCode, experimentName);
                    }
                }
            } else if (sampleCode == null)
            {
                if (dataSetCode != null)
                {
                    throw new IllegalArgumentException();
                }

                if (extension != null)
                {
                    entryBuilder.append('/').append(projectCode);
                }
            }
        } else if (experimentCode != null || (dataSetCode != null && sampleCode == null))
        {
            throw new IllegalArgumentException();
        } else if (experimentCode == null && sampleCode == null && dataSetCode == null && extension != null)
        {
            entryBuilder.append('/').append(spaceCode);
        }

        if (sampleCode != null)
        {
            if (spaceCode != null)
            {
                entryBuilder.append('/');
            }
            addFullEntityName(entryBuilder, containerCode, sampleCode, sampleName);

            if (dataSetCode != null)
            {
                // Sample data set
                entryBuilder.append('/').append(dataSetCode);
            } else if (extension != null)
            {
                entryBuilder.append('/');
                addFullEntityName(entryBuilder, containerCode, sampleCode, sampleName);
            }
        }

        entryBuilder.append(extension != null ? extension : '/');
        return entryBuilder.toString();
    }

    private static void createNextDataFile(final File parentDataDirectory, final char prefix, final String spaceCode, final String projectCode,
            final String containerCode, final String entityCode, final String dataSetTypeCode, final String dataSetCode,
            final String dataSetName, final String dataDirectorySuffix, final DataSetFileDownload dataSetFileDownload,
            final boolean compatibleWithImport) throws IOException
    {
        final DataSetFile dataSetFile = dataSetFileDownload.getDataSetFile();
        final String filePath = fixFilePath(dataSetFile);
        final boolean isDirectory = dataSetFile.isDirectory();

        final File dataSetFsEntry;
        if (compatibleWithImport)
        {
            final File dataDirectory = new File(parentDataDirectory, DATA_DIRECTORY + '/');
            mkdirs(dataDirectory);
            dataSetFsEntry = new File(dataDirectory, getDataDirectoryName(prefix, spaceCode, projectCode, containerCode, entityCode,
                    dataSetTypeCode, dataDirectorySuffix, filePath) + (isDirectory ? "/" : ""));
        } else
        {
            final File datasetDirectory = new File(parentDataDirectory, getFullEntityName(dataSetCode, dataSetName));
            mkdirs(datasetDirectory);
            dataSetFsEntry = new File(new File(datasetDirectory, DATA_DIRECTORY), filePath + (isDirectory ? "/" : ""));
        }

        final File dataSubdirectory = dataSetFsEntry.getParentFile();
        mkdirs(dataSubdirectory);

        if (!isDirectory)
        {
            try (
                    final InputStream is = dataSetFileDownload.getInputStream();
                    final BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(dataSetFsEntry))
            )
            {
                writeInChunks(os, is);
            }
        } else
        {
            mkdirs(dataSetFsEntry);
        }
    }

    /**
     * Replaces "original" with "default" directory name.
     *
     * @param dataSetFile the file object that contains the path information.
     * @return <code>dataSetFile.getPath()</code> as is or with "original" replaced with "default".
     */
    private static String fixFilePath(final DataSetFile dataSetFile)
    {
        final String originalPath = dataSetFile.getPath();
        if (dataSetFile.isDirectory() && Objects.equals(originalPath, "original"))
        {
            return "default";
        } else if (originalPath.startsWith("original/")) {
            return "default" + originalPath.substring("original".length());
        } else {
            return originalPath;
        }
    }

    static String getDataDirectoryName(final char prefix, final String spaceCode, final String projectCode,
            final String containerCode, final String entityCode, final String dataSetTypeCode,
            final String dataDirectorySuffix, final String fileName)
    {
        if (prefix != 'O' && prefix != 'E')
        {
            throw new IllegalArgumentException(String.format("Only 'O' and 'E' can be used as prefix got '%c' instead.", prefix));
        }

        if (containerCode != null && prefix != 'O')
        {
            throw new IllegalArgumentException("Only objects can have containers.");
        }

        final StringBuilder entryBuilder = new StringBuilder(String.valueOf(prefix));

        if (spaceCode != null)
        {
            entryBuilder.append('+').append(spaceCode);
        } else if (prefix == 'E')
        {
            throw new IllegalArgumentException("Space code cannot be null for experiments.");
        } else if (projectCode != null)
        {
            throw new IllegalArgumentException("If space code is null project code should be also null.");
        }

        if (projectCode != null)
        {
            entryBuilder.append('+').append(projectCode);
        } else if (prefix == 'E')
        {
            throw new IllegalArgumentException("Project code cannot be null for experiments.");
        }

        if (entityCode != null)
        {
            entryBuilder.append('+');
            addFullEntityCode(entryBuilder, containerCode, entityCode);
        } else
        {
            throw new IllegalArgumentException("Entity code is mandatory");
        }

        if (dataSetTypeCode != null)
        {
            entryBuilder.append('+').append(dataSetTypeCode);
        } else
        {
            throw new IllegalArgumentException("Data set type code is mandatory");
        }

        if (dataDirectorySuffix != null)
        {
            entryBuilder.append(dataDirectorySuffix);
        }

        if (fileName != null)
        {
            entryBuilder.append('/').append(fileName);
        }

        return entryBuilder.toString();
    }

    private static String getFullEntityName(final String entityCode, final String entityName)
    {
        final StringBuilder stringBuilder = new StringBuilder();
        addFullEntityName(stringBuilder, null, entityCode, entityName);
        return stringBuilder.toString();
    }

    private static void addFullEntityName(final StringBuilder entryBuilder, final String containerCode, final String entityCode,
            final String entityName)
    {
        if (entityName == null || entityName.isEmpty())
        {
            addFullEntityCode(entryBuilder, containerCode, entityCode);
        } else
        {
            entryBuilder.append(entityName).append(" (");
            addFullEntityCode(entryBuilder, containerCode, entityCode);
            entryBuilder.append(")");
        }
    }

    private static void addFullEntityCode(final StringBuilder entryBuilder, final String containerCode, final String entityCode)
    {
        if (containerCode != null)
        {
            entryBuilder.append(containerCode).append('*');
        }

        entryBuilder.append(entityCode);
    }

    private static String getEntityName(final IPropertiesHolder entity)
    {
        try
        {
            return escapeUnsafeCharacters(entity.getStringProperty(NAME_PROPERTY_NAME));
        } catch (final NotFetchedException e)
        {
            return null;
        }
    }

    private static String getBareEntityName(final IPropertiesHolder entity)
    {
        try
        {
            return entity.getStringProperty(NAME_PROPERTY_NAME);
        } catch (final NotFetchedException e)
        {
            return null;
        }
    }

    static String escapeUnsafeCharacters(final String name)
    {
        return name != null ? name.replaceAll(UNSAFE_CHARACTERS_REGEXP, "_") : null;
    }

    private String getHtml(final String sessionToken, final ICodeHolder entityObj,
            final Map<String, List<Map<String, String>>> entityTypeExportFieldsMap) throws IOException
    {
        final IApplicationServerInternalApi v3 = CommonServiceProvider.getApplicationServerApi();
        final DocumentBuilder documentBuilder = new DocumentBuilder();
        final String kindOrType = getKindOrType(entityObj);
        final StringBuilder titleStringBuilder = new StringBuilder();

        if (kindOrType != null)
        {
            titleStringBuilder.append(codeToDisplayName(kindOrType)).append(": ");
        }

        final String bareEntityName = entityObj instanceof IPropertiesHolder ? getBareEntityName((IPropertiesHolder) entityObj) : null;
        if (bareEntityName != null)
        {
            titleStringBuilder.append(bareEntityName);
        } else
        {
            titleStringBuilder.append(entityObj.getCode());
        }

        documentBuilder.addHeader(titleStringBuilder.toString(), 1);

        final IEntityType typeObj = getEntityType(v3, sessionToken, entityObj);

        final List<Map<String, String>> selectedExportFields = getSelectedExportFields(entityObj, entityTypeExportFieldsMap, typeObj);
        final Set<String> selectedExportAttributes = filterFields(selectedExportFields, ATTRIBUTE);
        final Set<String> selectedExportProperties = filterFields(selectedExportFields, PROPERTY);


        boolean isImagingType = ExportImagingUtils.isImagingType(typeObj);

        // Properties
        documentBuilder.addHeader("Properties", 2);
        if (entityObj instanceof IPropertiesHolder && typeObj != null)
        {
            final List<PropertyAssignment> propertyAssignments = typeObj.getPropertyAssignments();
            if (propertyAssignments != null)
            {
                propertyAssignments.sort(Comparator.comparingInt(PropertyAssignment::getOrdinal));

                final Map<String, Serializable> properties = includeSampleProperties((IPropertiesHolder) entityObj);
                boolean firstAssignment = true;
                String currentSection = null;
                for (final PropertyAssignment propertyAssignment : propertyAssignments) {
                    final PropertyType propertyType = propertyAssignment.getPropertyType();

                    if(isImagingType && ExportImagingUtils.isImagingInternalProperty(propertyType.getCode()))
                    {
                        // skip this one
                        continue;
                    }

                    if (!Objects.equals(propertyAssignment.getSection(), currentSection) || firstAssignment) {
                        currentSection = propertyAssignment.getSection();
                        documentBuilder.addHeader(currentSection != null ? currentSection : "", 3);
                        firstAssignment = false;
                    }

                    final String propertyTypeCode = propertyType.getCode();
                    final Object rawPropertyValue = properties.get(propertyTypeCode);

                    if (rawPropertyValue != null && allowsValue(selectedExportProperties, propertyTypeCode))
                    {
                        final String initialPropertyValue = String.valueOf(rawPropertyValue instanceof Sample
                                ? ((Sample) rawPropertyValue).getIdentifier().getIdentifier()
                                : rawPropertyValue);
                        final String propertyValue;

                        if (propertyType.getDataType() == DataType.SAMPLE)
                        {
                            if (rawPropertyValue instanceof Sample[])
                            {
                                propertyValue = Arrays.stream(((Sample[]) rawPropertyValue)).map(sample -> sample.getIdentifier().getIdentifier())
                                        .collect(Collectors.joining(", "));
                            } else if (rawPropertyValue instanceof Sample)
                            {
                                propertyValue = ((Sample) rawPropertyValue).getIdentifier().getIdentifier();
                            } else
                            {
                                throw new IllegalArgumentException("Sample property value is not of type Sample or Sample[].");
                            }
                        } else if (propertyType.getDataType() == DataType.MULTILINE_VARCHAR &&
                                Objects.equals(propertyType.getMetaData().get("custom_widget"), "Word Processor"))
                        {
                            propertyValue = ExportPropertiesUtils.encodeImages(configurer, initialPropertyValue);
                        } else if (propertyType.getDataType() == DataType.XML
                                && Objects.equals(propertyType.getMetaData().get("custom_widget"), "Spreadsheet")
                                && initialPropertyValue.toUpperCase().startsWith(ExportPropertiesUtils.DATA_TAG_START) && initialPropertyValue.toUpperCase()
                                .endsWith(ExportPropertiesUtils.DATA_TAG_END))
                        {
                            final String subString = initialPropertyValue.substring(ExportPropertiesUtils.DATA_TAG_START_LENGTH,
                                    initialPropertyValue.length() - ExportPropertiesUtils.DATA_TAG_END_LENGTH);
                            final String decodedString = new String(Base64.getDecoder().decode(subString), StandardCharsets.UTF_8);
                            final ObjectMapper objectMapper = new ObjectMapper();
                            final JsonNode jsonNode = objectMapper.readTree(decodedString);
                            propertyValue = ExportPDFUtils.convertJsonToHtml(jsonNode);
                        } else if(propertyType.getDataType() == DataType.CONTROLLEDVOCABULARY)
                        {
                            Map<String, String> terms = propertyType.getVocabulary().getTerms().stream().collect(Collectors.toMap(
                                    VocabularyTerm::getCode, x -> x.getLabel() != null ? x.getLabel() : x.getCode()));
                            if(rawPropertyValue.getClass().isArray()) {
                                Serializable[] values = (Serializable[]) rawPropertyValue;
                                StringBuilder builder = new StringBuilder("[");
                                for(Serializable value : values) {
                                    if(builder.length() > 1) {
                                        builder.append(", ");
                                    }
                                    builder.append(terms.get(value.toString().toUpperCase()));
                                }
                                builder.append("]");
                                propertyValue =  builder.toString();
                            } else {
                                propertyValue = terms.get(rawPropertyValue.toString().toUpperCase());
                            }
                        }
                        else
                        {
                            propertyValue = initialPropertyValue;
                        }

                        if (!Objects.equals(propertyValue, "\uFFFD(undefined)"))
                        {
                            documentBuilder.addProperty(propertyType.getLabel(), propertyValue);
                        }
                    }
                }
            }
        }

        // Description

        if (entityObj instanceof IDescriptionHolder && allowsValue(selectedExportAttributes, Attribute.DESCRIPTION.name()))
        {
            final String description = ((IDescriptionHolder) entityObj).getDescription();
            if (description != null && !Objects.equals(description, "\uFFFD(undefined)"))
            {
                documentBuilder.addHeader("Description", 2);
                documentBuilder.addParagraph(ExportPropertiesUtils.encodeImages(configurer, description));
            }
        }

        // Parents / Children

        if (entityObj instanceof IParentChildrenHolder<?>)
        {
            final IParentChildrenHolder<?> parentChildrenHolder = (IParentChildrenHolder<?>) entityObj;
            if (allowsValue(selectedExportAttributes, Attribute.PARENTS.name()))
            {
                documentBuilder.addHeader("Parents", 2);
                final List<?> parents = parentChildrenHolder.getParents();
                for (final Object parent : parents)
                {
                    final String relCodeName = ((ICodeHolder) parent).getCode();
                    final String name = getEntityName((IPropertiesHolder) parent);
                    documentBuilder.addParagraph(relCodeName + (name != null ? " (" + name + ")" : ""));
                }
            }

            if (allowsValue(selectedExportAttributes, Attribute.CHILDREN.name()))
            {
                documentBuilder.addHeader("Children", 2);
                final List<?> children = parentChildrenHolder.getChildren();
                for (final Object child : children)
                {
                    final String relCodeName = ((ICodeHolder) child).getCode();
                    final String name = getEntityName((IPropertiesHolder) child);
                    documentBuilder.addParagraph(relCodeName + (name != null ? " (" + name + ")" : ""));
                }
            }
        }

        // Identification Info

        documentBuilder.addHeader("Identification Info", 2);

        if (entityObj instanceof Experiment)
        {
            documentBuilder.addProperty(KIND_DOCUMENT_PROPERTY_ID, "Experiment");
        } else if (entityObj instanceof Sample)
        {
            documentBuilder.addProperty(KIND_DOCUMENT_PROPERTY_ID, "Sample");
        } else if (entityObj instanceof DataSet)
        {
            documentBuilder.addProperty(KIND_DOCUMENT_PROPERTY_ID, "DataSet");
        }

        documentBuilder.addProperty(entityObj instanceof Project || entityObj instanceof Space
                ? KIND_DOCUMENT_PROPERTY_ID : TYPE_DOCUMENT_PROPERTY_ID,
                kindOrType);

        if (allowsValue(selectedExportAttributes, Attribute.CODE.name()))
        {
            documentBuilder.addProperty("Code", entityObj.getCode());
        }

        if (entityObj instanceof IPermIdHolder && allowsValue(selectedExportAttributes, Attribute.PERM_ID.name()))
        {
            documentBuilder.addProperty("Perm ID", ((IPermIdHolder) entityObj).getPermId().toString());
        }

        if (entityObj instanceof IIdentifierHolder && allowsValue(selectedExportAttributes, Attribute.IDENTIFIER.name()))
        {
            final ObjectIdentifier identifier = ((IIdentifierHolder) entityObj).getIdentifier();
            if (identifier != null)
            {
                documentBuilder.addProperty("Identifier", identifier.getIdentifier());
            }
        }

        // Registration / Modification

        if (entityObj instanceof IRegistratorHolder && allowsValue(selectedExportAttributes, Attribute.REGISTRATOR.name()))
        {
            final Person registrator = ((IRegistratorHolder) entityObj).getRegistrator();
            if (registrator != null)
            {
                documentBuilder.addProperty("Registrator", registrator.getUserId());
            }
        }

        if (entityObj instanceof IRegistrationDateHolder && allowsValue(selectedExportAttributes, Attribute.REGISTRATION_DATE.name()))
        {
            final Date registrationDate = ((IRegistrationDateHolder) entityObj).getRegistrationDate();
            if (registrationDate != null)
            {
                documentBuilder.addProperty("Registration Date", String.valueOf(registrationDate));
            }
        }

        if (entityObj instanceof IModifierHolder && allowsValue(selectedExportAttributes, Attribute.MODIFIER.name()))
        {
            final Person modifier = ((IModifierHolder) entityObj).getModifier();
            if (modifier != null)
            {
                documentBuilder.addProperty("Modifier", modifier.getUserId());
            }
        }

        if (entityObj instanceof IModificationDateHolder && allowsValue(selectedExportAttributes, Attribute.MODIFICATION_DATE.name()))
        {
            final Date modificationDate = ((IModificationDateHolder) entityObj).getModificationDate();
            if (modificationDate != null)
            {
                documentBuilder.addProperty("Modification Date", String.valueOf(modificationDate));
            }
        }

        // Imaging Section
        if(isImagingType)
        {
            ExportImagingUtils.buildImagingData(documentBuilder, typeObj, entityObj, configurer);
        }

        return documentBuilder.getHtml();
    }


    private static IEntityType getEntityType(final IApplicationServerInternalApi v3, final String sessionToken, final ICodeHolder entityObj)
    {
        if (entityObj instanceof Experiment)
        {
            final ExperimentTypeSearchCriteria searchCriteria = new ExperimentTypeSearchCriteria();
            searchCriteria.withCode().thatEquals(((Experiment) entityObj).getType().getCode());
            final ExperimentTypeFetchOptions fetchOptions = new ExperimentTypeFetchOptions();
            fetchOptions.withPropertyAssignments().withPropertyType().withVocabulary().withTerms();
            final SearchResult<ExperimentType> results = v3.searchExperimentTypes(sessionToken, searchCriteria, fetchOptions);
            return results.getObjects().get(0);
        } else if (entityObj instanceof Sample)
        {
            final SampleTypeSearchCriteria searchCriteria = new SampleTypeSearchCriteria();
            searchCriteria.withCode().thatEquals(((Sample) entityObj).getType().getCode());
            final SampleTypeFetchOptions fetchOptions = new SampleTypeFetchOptions();
            fetchOptions.withPropertyAssignments().withPropertyType().withVocabulary().withTerms();
            final SearchResult<SampleType> results = v3.searchSampleTypes(sessionToken, searchCriteria, fetchOptions);
            return results.getObjects().get(0);
        } else if (entityObj instanceof DataSet)
        {
            final DataSetTypeSearchCriteria searchCriteria = new DataSetTypeSearchCriteria();
            searchCriteria.withCode().thatEquals(((DataSet) entityObj).getType().getCode());
            final DataSetTypeFetchOptions fetchOptions = new DataSetTypeFetchOptions();
            fetchOptions.withPropertyAssignments().withPropertyType().withVocabulary().withTerms();
            final SearchResult<DataSetType> results = v3.searchDataSetTypes(sessionToken, searchCriteria, fetchOptions);
            return results.getObjects().get(0);
        } else
        {
            return null;
        }
    }

    private static Set<String> filterFields(final List<Map<String, String>> selectedExportFields, final FieldType fieldType)
    {
        return selectedExportFields != null
                ? selectedExportFields.stream().filter(map -> Objects.equals(map.get(FIELD_TYPE_KEY), fieldType.name()))
                .map(map -> map.get(FIELD_ID_KEY)).collect(Collectors.toSet())
                : null;
    }

    private static List<Map<String, String>> getSelectedExportFields(final ICodeHolder entityObj,
            final Map<String, List<Map<String, String>>> entityTypeExportFieldsMap, final IEntityType typeObj)
    {
        if (entityTypeExportFieldsMap == null || entityTypeExportFieldsMap.isEmpty())
        {
            return null;
        } else if (typeObj != null)
        {
            return entityTypeExportFieldsMap.get(typeObj.getCode());
        } else if (entityObj instanceof Space)
        {
            return entityTypeExportFieldsMap.get(SPACE.name());
        } else if (entityObj instanceof Project)
        {
            return entityTypeExportFieldsMap.get(PROJECT.name());
        } else
        {
            return null;
        }
    }

    private static String getKindOrType(final Object entity)
    {
        if (entity instanceof Project)
        {
            return "Project";
        } else if (entity instanceof Space)
        {
            return "Space";
        } else
        {
            return entity instanceof IEntityTypeHolder ? ((IEntityTypeHolder) entity).getType().getCode() : null;
        }
    }

    private static String codeToDisplayName(final String code)
    {
        return Arrays.stream(code.toLowerCase().split("_")).map(ExportExecutor::capitalizeFirstLetter).collect(Collectors.joining(" "));
    }

    private static String capitalizeFirstLetter(final String str) {
        if (str.isEmpty())
        {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }



    /**
     * Whether the set does not forbid a value.
     *
     * @param set the set to look in
     * @param value the value to be found
     * @return <code>true</code> if set is <code>null</code> or value is in the set
     */
    private boolean allowsValue(final Set<String> set, final String value)
    {
        return set == null || set.contains(value);
    }

    private static Map<String, List<Map<String, String>>> findExportAttributes(final ExportableKind exportableKind, final SelectedFields selectedFields)
    {
        final List<Map<String, String>> attributes = selectedFields.getAttributes().stream()
                .map(attribute -> Map.of(IExportFieldsFinder.TYPE, ATTRIBUTE.name(), IExportFieldsFinder.ID, attribute.name()))
                .collect(Collectors.toList());
        return Map.of(exportableKind.name(), attributes);
    }

    /**
     * Safely tries to create a directory if it does not exist. If it could not be created throws an exception.
     *
     * @param dir the directory to be created.
     */
    private static void mkdirs(final File dir)
    {
        if (!dir.isDirectory())
        {
            final boolean created = dir.mkdirs();
            if (!created)
            {
                throw new RuntimeException(String.format("Cannot create directory '%s'.", dir.getPath()));
            }
        }
    }

    private static void zipDirectory(final String sourceDirectory, final File targetZipFile)
            throws IOException
    {
        final Path sourceDir = Paths.get(sourceDirectory);
        try (final ZipArchiveOutputStream zipOutputStream = new ZipArchiveOutputStream(
                new FileOutputStream(targetZipFile)))
        {
            zipOutputStream.setEncoding(StandardCharsets.ISO_8859_1.toString());
            zipOutputStream.setUseLanguageEncodingFlag(true);
            zipOutputStream.setCreateUnicodeExtraFields(
                    ZipArchiveOutputStream.UnicodeExtraFieldPolicy.ALWAYS);
            zipOutputStream.setMethod(ZipArchiveOutputStream.DEFLATED);
            zipOutputStream.setLevel(5);
            try (final Stream<Path> stream = Files.walk(sourceDir))
            {
                stream.filter(
                                path -> !path.equals(sourceDir) && !path.toFile().equals(targetZipFile))
                        .forEach(path ->
                        {
                            try
                            {
                                final boolean isDirectory = Files.isDirectory(path);
                                final String entryName = sourceDir.relativize(path).toString();
                                final ZipEntry zipEntry =
                                        new ZipEntry(entryName + (isDirectory ? "/" : ""));
                                zipEntry.setMethod(ZipArchiveOutputStream.DEFLATED);
                                if (!isDirectory)
                                {
                                    zipEntry.setSize(Files.size(path));
                                }
                                zipOutputStream.putArchiveEntry(new ZipArchiveEntry(zipEntry));
                                if (!isDirectory)
                                {
                                    Files.copy(path, zipOutputStream);
                                }
                                zipOutputStream.closeArchiveEntry();
                            } catch (IOException e)
                            {
                                throw new RuntimeException(e);
                            }
                        });
            } catch (final IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    public static void deleteDirectory(final String directoryPath) throws IOException {
        final Path path = Paths.get(directoryPath);
        if (Files.exists(path))
        {
            try (final Stream<Path> walkStream = Files.walk(path))
            {
                walkStream
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        }
    }

    /**
     * Finds the only file in the directory structure.
     *
     * @param sourceDirectory the directory to be scanned for the file.
     * @return the only file in the directory structure if it exists and no other files are present, <code>null</code> otherwise.
     * @throws IOException if an IO exception occurs.
     */
    private static File getSingleFile(final Path sourceDirectory) throws IOException
    {
        try (final Stream<Path> stream = Files.walk(sourceDirectory))
        {
            final List<Path> filePaths = stream.filter(path -> path.toFile().isFile()).limit(2).collect(Collectors.toList());
            return filePaths.size() == 1 ? filePaths.get(0).toFile() : null;
        }
    }

    public static boolean isAbsoluteUrl(final String url) {
        try {
            new URL(url);
            return true;
        } catch (final MalformedURLException e) {
            return false;
        }
    }

    private static Map<String, Serializable> includeSampleProperties(final IPropertiesHolder entity)
    {
        final Map<String, Sample[]> sampleProperties;
        if (entity instanceof Sample)
        {
            sampleProperties = ((Sample) entity).getSampleProperties();
        } else if (entity instanceof Experiment)
        {
            sampleProperties = ((Experiment) entity).getSampleProperties();
        } else if (entity instanceof DataSet)
        {
            sampleProperties = ((DataSet) entity).getSampleProperties();
        } else
        {
            sampleProperties = null;
        }

        final Map<String, Serializable> properties = new HashMap<>(entity.getProperties());
        if (sampleProperties != null)
        {
            properties.putAll(sampleProperties);
        }

        return properties;
    }

    private static class EntitiesVo
    {

        private final String sessionToken;

        private final Map<ExportableKind, List<String>> groupedExportablePermIds;

        private Collection<Space> spaces;

        private Collection<Project> projects;

        private Collection<Experiment> experiments;

        private Collection<Sample> samples;

        private Collection<DataSet> dataSets;

        private EntitiesVo(final String sessionToken, final List<ExportablePermId> exportablePermIds)
        {
            this.sessionToken = sessionToken;
            groupedExportablePermIds = getGroupedExportablePermIds(exportablePermIds);
        }

        private static Map<ExportableKind, List<String>> getGroupedExportablePermIds(final List<ExportablePermId> exportablePermIds)
        {
            final Collector<ExportablePermId, List<String>, List<String>> downstreamCollector = Collector.of(ArrayList::new,
                    (stringPermIds, exportablePermId) -> stringPermIds.add(exportablePermId.getPermId().getPermId()),
                    (left, right) ->
                    {
                        left.addAll(right);
                        return left;
                    });

            return exportablePermIds.stream().collect(Collectors.groupingBy(ExportablePermId::getExportableKind, downstreamCollector));
        }

        public Collection<Space> getSpaces()
        {
            if (spaces == null)
            {
                spaces = EntitiesFinder.getSpaces(sessionToken, groupedExportablePermIds.getOrDefault(ExportableKind.SPACE, List.of()));
            }
            return spaces;
        }

        public Collection<Project> getProjects()
        {
            if (projects == null)
            {
                projects = EntitiesFinder.getProjects(sessionToken, groupedExportablePermIds.getOrDefault(ExportableKind.PROJECT, List.of()));
            }

            return projects;
        }

        public Collection<Experiment> getExperiments()
        {
            if (experiments == null)
            {
                experiments = EntitiesFinder.getExperiments(sessionToken, groupedExportablePermIds.getOrDefault(ExportableKind.EXPERIMENT, List.of()));
            }
            return experiments;
        }

        public Collection<Sample> getSamples()
        {
            if (samples == null)
            {
                samples = EntitiesFinder.getSamples(sessionToken, groupedExportablePermIds.getOrDefault(ExportableKind.SAMPLE, List.of()));
            }
            return samples;
        }

        public Collection<DataSet> getDataSets()
        {
            if (dataSets == null)
            {
                dataSets = EntitiesFinder.getDataSets(sessionToken, groupedExportablePermIds.getOrDefault(DATASET, List.of()));
            }
            return dataSets;
        }

    }

}
