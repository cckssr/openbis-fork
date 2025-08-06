/*
 * Copyright ETH 2022 - 2023 Zürich, Scientific IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.ethz.sis.openbis.generic.server.xls.importer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import ch.ethz.sis.openbis.generic.server.xls.importer.helper.*;
import ch.ethz.sis.openbis.generic.server.xls.importer.helper.semanticannotation.SemanticAnnotationHelper;
import org.apache.log4j.Logger;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.id.IObjectId;
import ch.ethz.sis.openbis.generic.server.xls.importer.delay.DelayedExecutionDecorator;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportModes;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportTypes;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ScriptTypes;
import ch.ethz.sis.openbis.generic.server.xls.importer.handler.ExcelParser;
import ch.ethz.sis.openbis.generic.server.xls.importer.handler.VersionInfoHandler;
import ch.ethz.sis.openbis.generic.server.xls.importer.utils.DatabaseConsistencyChecker;
import ch.ethz.sis.openbis.generic.server.xls.importer.utils.FileServerUtils;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.common.logging.LogCategory;
import ch.systemsx.cisd.common.logging.LogFactory;
import ch.systemsx.cisd.openbis.generic.server.CommonServiceProvider;
import ch.systemsx.cisd.openbis.generic.shared.ISessionWorkspaceProvider;

public class XLSImport
{
    private static final String PATH_SEPARATOR = "/";

    private static final String XLSX_FOLDER_NAME = "xlsx" + PATH_SEPARATOR;

    private static final String SCRIPTS_FOLDER_NAME = "scripts" + PATH_SEPARATOR;

    private static final String DATA_FOLDER_NAME = "data" + PATH_SEPARATOR;

    private static final String MISCELLANEOUS_FOLDER_NAME = "miscellaneous" + PATH_SEPARATOR;

    private static final String FILE_SERVICES_FOLDER_NAME = MISCELLANEOUS_FOLDER_NAME + "file-service" + PATH_SEPARATOR;

    private static final String XLS_EXTENSION = "." + "xls";

    private static final String XLSX_EXTENSION = "." + "xlsx";

    private static final int XLSX_DOCUMENT_LIMIT = 536870912; // 512 MB

    private static final int EMBEDDED_DOCUMENT_LIMIT = 16777216; // 16 MB

    private static final Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION, XLSImport.class);

    private final String sessionToken;

    private final IApplicationServerApi api;

    private final DelayedExecutionDecorator delayedExecutor;

    private final ImportOptions options;

    private final Map<String, Integer> beforeVersions;

    private final Map<String, Integer> afterVersions;

    private final VocabularyImportHelper vocabularyHelper;

    private final VocabularyTermImportHelper vocabularyTermHelper;

    private final SampleTypeImportHelper sampleTypeHelper;

    private final ExperimentTypeImportHelper experimentTypeHelper;

    private final DatasetTypeImportHelper datasetTypeHelper;

    private final SpaceImportHelper spaceHelper;

    private final ProjectImportHelper projectHelper;

    private final ExperimentImportHelper experimentHelper;

    private final SampleImportHelper sampleHelper;

    private final PropertyTypeImportHelper propertyHelper;

    private final PropertyAssignmentImportHelper propertyAssignmentHelper;

    private final SemanticAnnotationImportHelper semanticAnnotationImportHelper;

    private final TypeGroupImportHelper typeGroupImportHelper;

    private final DatabaseConsistencyChecker dbChecker;

    private final boolean shouldCheckVersionsOnDatabase;

    private final Map<String, String> importValues;

    private final ScriptImportHelper scriptHelper;

    private final String[] sessionWorkspaceFiles;

    private final byte[][] xls;

    private static final String ZIP_EXTENSION = "." + "zip";

    public XLSImport(String sessionToken,
            IApplicationServerApi api,
            ImportModes mode,
            ImportOptions options,
            String[] sessionWorkspaceFiles,
            boolean shouldCheckVersionsOnDatabase) throws IOException
    {
        this.sessionToken = sessionToken;
        this.api = api;
        this.options = options;
        this.beforeVersions = Collections.unmodifiableMap(VersionInfoHandler.loadAllVersions(options));
        this.afterVersions = VersionInfoHandler.loadAllVersions(options);
        this.dbChecker = new DatabaseConsistencyChecker(this.sessionToken, this.api, this.afterVersions);
        this.delayedExecutor = new DelayedExecutionDecorator(this.sessionToken, this.api);

        SemanticAnnotationHelper annotationCache = new SemanticAnnotationHelper(delayedExecutor);

        this.vocabularyHelper = new VocabularyImportHelper(this.delayedExecutor, mode, options, afterVersions);
        this.vocabularyTermHelper = new VocabularyTermImportHelper(this.delayedExecutor, mode, options, afterVersions);
        this.sampleTypeHelper = new SampleTypeImportHelper(this.delayedExecutor, mode, options, afterVersions, annotationCache);
        this.experimentTypeHelper = new ExperimentTypeImportHelper(this.delayedExecutor, mode, options, afterVersions, annotationCache);
        this.datasetTypeHelper = new DatasetTypeImportHelper(this.delayedExecutor, mode, options, afterVersions, annotationCache);
        this.spaceHelper = new SpaceImportHelper(this.delayedExecutor, mode, options);
        this.projectHelper = new ProjectImportHelper(this.delayedExecutor, mode, options);
        this.experimentHelper = new ExperimentImportHelper(this.delayedExecutor, mode, options, annotationCache);
        this.sampleHelper = new SampleImportHelper(this.delayedExecutor, mode, options, annotationCache);
        this.propertyHelper = new PropertyTypeImportHelper(this.delayedExecutor, mode, options, afterVersions, annotationCache);
        this.propertyAssignmentHelper = new PropertyAssignmentImportHelper(this.delayedExecutor, mode, options, beforeVersions, annotationCache);
        this.semanticAnnotationImportHelper = new SemanticAnnotationImportHelper(this.delayedExecutor, mode, options, annotationCache);
        this.typeGroupImportHelper = new TypeGroupImportHelper(this.delayedExecutor, mode, options);
        this.shouldCheckVersionsOnDatabase = shouldCheckVersionsOnDatabase;

        // File Parsing
        this.sessionWorkspaceFiles = sessionWorkspaceFiles;
        final ISessionWorkspaceProvider sessionWorkspaceProvider = CommonServiceProvider.getSessionWorkspaceProvider();

        final Map<String, String> scripts = new HashMap<>();
        byte[][] xls = new byte[sessionWorkspaceFiles.length][];
        this.importValues = new HashMap<>();

        for (int i = 0; i < sessionWorkspaceFiles.length; i++)
        {
            InputStream read = sessionWorkspaceProvider.read(sessionToken, sessionWorkspaceFiles[i]);
            if (sessionWorkspaceFiles[i].toLowerCase().endsWith(ZIP_EXTENSION))
            {
                try (final ZipInputStream zip = new ZipInputStream(read))
                {
                    ZipEntry entry;
                    while ((entry = zip.getNextEntry()) != null)
                    {
                        final String entryName = entry.getName().startsWith(XLSX_FOLDER_NAME) ? entry.getName().substring(XLSX_FOLDER_NAME.length())
                                : entry.getName();
                        if (entry.isDirectory())
                        {
                            if (!entryName.isEmpty() &&
                                    !SCRIPTS_FOLDER_NAME.equals(entryName) &&
                                    !DATA_FOLDER_NAME.equals(entryName) &&
                                    !entryName.startsWith(MISCELLANEOUS_FOLDER_NAME))
                            {
                                throw UserFailureException.fromTemplate("Illegal directory '%s' is found inside the imported file.", entryName);
                            }
                        } else
                        {
                            if (!entryName.contains(PATH_SEPARATOR) && (entryName.endsWith(XLS_EXTENSION) || entryName.endsWith(XLSX_EXTENSION)))
                            {
                                validateEntrySize(entry.getSize(), XLSX_DOCUMENT_LIMIT);
                                if (xls[i] == null)
                                {
                                    xls[i] = zip.readAllBytes();
                                } else
                                {
                                    throw UserFailureException.fromTemplate("More than one XLS file found in the root of the imported ZIP file.");
                                }
                            } else if (entryName.startsWith(SCRIPTS_FOLDER_NAME))
                            {
                                validateEntrySize(entry.getSize(), EMBEDDED_DOCUMENT_LIMIT);
                                scripts.put(entryName.substring(SCRIPTS_FOLDER_NAME.length()), new String(zip.readAllBytes()));
                            } else if (entryName.startsWith(DATA_FOLDER_NAME))
                            {
                                validateEntrySize(entry.getSize(), EMBEDDED_DOCUMENT_LIMIT);
                                this.importValues.put(entryName.substring(DATA_FOLDER_NAME.length()), new String(zip.readAllBytes()));
                            } else if (!entryName.startsWith(MISCELLANEOUS_FOLDER_NAME))
                            {
                                throw UserFailureException.fromTemplate(
                                        "Entry '%s' is not allowed. Only one root XLS file is allowed and files inside the '%s' or '%s' folder",
                                        entry.getName(), SCRIPTS_FOLDER_NAME, DATA_FOLDER_NAME);
                            }
                        }
                    }
                }
            } else
            {
                final File canonicalFile = sessionWorkspaceProvider.getCanonicalFile(sessionToken, sessionWorkspaceFiles[i]);
                validateEntrySize(canonicalFile.length(), XLSX_DOCUMENT_LIMIT);
                xls[i] = sessionWorkspaceProvider.readAllBytes(sessionToken, sessionWorkspaceFiles[i]);

                // Script folder support
                final String scriptsFolderName = canonicalFile.getParent()
                        + PATH_SEPARATOR + SCRIPTS_FOLDER_NAME;

                final File scriptsFolder = new File(scriptsFolderName);
                if (scriptsFolder.exists() && scriptsFolder.isDirectory())
                {
                    final File[] files = scriptsFolder.listFiles();
                    if (files != null)
                    {
                        for (final File file : files)
                        {
                            scripts.put(file.getName(), Files.readString(file.toPath(), StandardCharsets.UTF_8));
                        }
                    }
                }

                // Data folder support
                final String dataFolderName = canonicalFile.getParent()
                        + PATH_SEPARATOR + DATA_FOLDER_NAME;

                final File dataFolder = new File(dataFolderName);
                if (dataFolder.exists() && dataFolder.isDirectory())
                {
                    final File[] files = dataFolder.listFiles();
                    if (files != null)
                    {
                        for (final File file : files)
                        {
                            this.importValues.put(file.getName(), Files.readString(file.toPath(), StandardCharsets.UTF_8));
                        }
                    }
                }
            }
        }

        this.xls = xls;
        this.scriptHelper = new ScriptImportHelper(this.delayedExecutor, mode, options, scripts);
    }

    private static void validateEntrySize(final long size, final int limit)
    {
        if (size > limit)
        {
            throw UserFailureException.fromTemplate("Document limit exceeded: %d.", size);
        }
    }

    public List<IObjectId> start() throws IOException
    {
        if (shouldCheckVersionsOnDatabase)
        {
            this.dbChecker.checkVersionsOnDataBase();
        }

        for (int i = 0; i < this.xls.length; i++)
        {
            List<List<List<String>>> lines = ExcelParser.parseExcel(this.xls[i], importValues);
            int pageNumber = 0;
            int lineNumber;

            while (pageNumber < lines.size())
            {
                lineNumber = 0;
                List<List<String>> page = lines.get(pageNumber);
                int pageEnd = getPageEnd(page);
                while (lineNumber < pageEnd)
                {
                    operationLog.debug("XLSImport - Page: " + pageNumber + " Line: " + lineNumber);
                    int blockEnd = getBlockEnd(page, lineNumber);
                    ImportTypes blockType;
                    try
                    {
                        blockType = ImportTypes.valueOf(page.get(lineNumber).get(0));
                    } catch (Exception e)
                    {
                        throw new UserFailureException(
                                "Exception at page " + (pageNumber + 1) + " and line " + (lineNumber + 1) + " with message: " + e.getMessage());
                    }
                    lineNumber++;

                    switch (blockType)
                    {
                        case VOCABULARY_TYPE:
                            vocabularyHelper.importBlock(page, pageNumber, lineNumber, lineNumber + 2);
                            if (lineNumber + 2 != blockEnd)
                            {
                                vocabularyTermHelper.importBlock(page, pageNumber, lineNumber, blockEnd);
                            }
                            break;
                        case SAMPLE_TYPE:
                            // parse and create scripts
                            scriptHelper.importBlock(page, pageNumber, lineNumber, lineNumber + 2, ScriptTypes.VALIDATION_SCRIPT);
                            // parse and create sample type
                            sampleTypeHelper.importBlock(page, pageNumber, lineNumber, lineNumber + 2);
                            semanticAnnotationImportHelper.importBlockForEntityType(page, pageNumber, lineNumber, lineNumber + 2,
                                    ImportTypes.SAMPLE_TYPE);
                            // parse and assignment properties
                            if (lineNumber + 2 != blockEnd)
                            {
                                scriptHelper.importBlock(page, pageNumber, lineNumber + 2, blockEnd, ScriptTypes.DYNAMIC_SCRIPT);
                                propertyHelper.importBlock(page, pageNumber, lineNumber + 2, blockEnd, ImportTypes.SAMPLE_TYPE);
                                propertyAssignmentHelper.importBlock(page, pageNumber, lineNumber, blockEnd, ImportTypes.SAMPLE_TYPE);
                                semanticAnnotationImportHelper.importBlockForPropertyAssignment(page, pageNumber, lineNumber, blockEnd,
                                        ImportTypes.SAMPLE_TYPE);
                            }
                            break;
                        case EXPERIMENT_TYPE:
                            // parse and create scripts
                            scriptHelper.importBlock(page, pageNumber, lineNumber, lineNumber + 2, ScriptTypes.VALIDATION_SCRIPT);
                            // parse and create experiment type
                            experimentTypeHelper.importBlock(page, pageNumber, lineNumber, lineNumber + 2);
                            semanticAnnotationImportHelper.importBlockForEntityType(page, pageNumber, lineNumber, lineNumber + 2,
                                    ImportTypes.EXPERIMENT_TYPE);
                            // parse and assignment properties
                            if (lineNumber + 2 != blockEnd)
                            {
                                scriptHelper.importBlock(page, pageNumber, lineNumber + 2, blockEnd, ScriptTypes.DYNAMIC_SCRIPT);
                                propertyHelper.importBlock(page, pageNumber, lineNumber + 2, blockEnd, ImportTypes.EXPERIMENT_TYPE);
                                propertyAssignmentHelper.importBlock(page, pageNumber, lineNumber, blockEnd, ImportTypes.EXPERIMENT_TYPE);
                                semanticAnnotationImportHelper.importBlockForPropertyAssignment(page, pageNumber, lineNumber, blockEnd,
                                        ImportTypes.EXPERIMENT_TYPE);
                            }
                            break;
                        case DATASET_TYPE:
                            // parse and create scripts
                            scriptHelper.importBlock(page, pageNumber, lineNumber, lineNumber + 2, ScriptTypes.VALIDATION_SCRIPT);
                            // parse and create dataset type
                            datasetTypeHelper.importBlock(page, pageNumber, lineNumber, lineNumber + 2);
                            semanticAnnotationImportHelper.importBlockForEntityType(page, pageNumber, lineNumber, lineNumber + 2,
                                    ImportTypes.DATASET_TYPE);
                            // parse and assignment properties
                            if (lineNumber + 2 != blockEnd)
                            {
                                scriptHelper.importBlock(page, pageNumber, lineNumber + 2, blockEnd, ScriptTypes.DYNAMIC_SCRIPT);
                                propertyHelper.importBlock(page, pageNumber, lineNumber + 2, blockEnd, ImportTypes.DATASET_TYPE);
                                propertyAssignmentHelper.importBlock(page, pageNumber, lineNumber, blockEnd, ImportTypes.DATASET_TYPE);
                                semanticAnnotationImportHelper.importBlockForPropertyAssignment(page, pageNumber, lineNumber, blockEnd,
                                        ImportTypes.DATASET_TYPE);
                            }
                            break;
                        case SPACE:
                            spaceHelper.importBlock(page, pageNumber, lineNumber, blockEnd);
                            break;
                        case PROJECT:
                            projectHelper.importBlock(page, pageNumber, lineNumber, blockEnd);
                            break;
                        case EXPERIMENT:
                            experimentHelper.importBlock(page, pageNumber, lineNumber, blockEnd);
                            break;
                        case SAMPLE:
                            sampleHelper.importBlock(page, pageNumber, lineNumber, blockEnd);
                            break;
                        case PROPERTY_TYPE:
                            propertyHelper.importBlock(page, pageNumber, lineNumber, blockEnd);
                            semanticAnnotationImportHelper.importBlockForPropertyType(page, pageNumber, lineNumber, blockEnd);
                            break;
                        case TYPE_GROUP:
                            typeGroupImportHelper.importBlock(page, pageNumber, lineNumber, blockEnd);
                            break;
                        default:
                            throw new UserFailureException("Unknown type: " + blockType);
                    }
                    lineNumber = blockEnd + 1;
                }
                pageNumber++;
            }
        }

        this.delayedExecutor.hasFinished();
        VersionInfoHandler.writeAllVersions(options, afterVersions);
        importZipData();
        return new ArrayList<>(this.delayedExecutor.getIds());
    }

    private int getPageEnd(List<List<String>> page)
    {
        int pageEnd = page.size();

        boolean prevLineIsEmpty = false;

        for (int i = 0; i < page.size(); ++i)
        {
            boolean curLineIsEmpty = isLineEmpty(page.get(i));

            if (prevLineIsEmpty && curLineIsEmpty)
            {
                pageEnd = i - 2;

                for (int bankIndex = pageEnd + 1; bankIndex < page.size(); bankIndex++)
                {
                    if (!isLineEmpty(page.get(bankIndex)))
                    {
                        throw new UserFailureException("Content found after a double blank row that should mark the end of a page.");
                    }
                }

                break;
            }
            prevLineIsEmpty = curLineIsEmpty;
        }

        return pageEnd;
    }

    private int getBlockEnd(List<List<String>> page, int start)
    {
        for (int i = start; i < page.size(); ++i)
        {
            if (isLineEmpty(page.get(i)))
            {
                return i;
            }
        }
        return page.size();
    }

    private boolean isLineEmpty(List<String> line)
    {
        for (String cell : line)
        {
            if (cell != null && !cell.trim().isEmpty())
            {
                return false;
            }
        }
        return true;
    }


    private void importZipData() throws IOException
    {
        for (int i = 0; i < this.sessionWorkspaceFiles.length; i++)
        {
            if (this.sessionWorkspaceFiles[i].toLowerCase().endsWith(ZIP_EXTENSION))
            {
                final ISessionWorkspaceProvider sessionWorkspaceProvider = CommonServiceProvider.getSessionWorkspaceProvider();
                InputStream read = sessionWorkspaceProvider.read(this.sessionToken, this.sessionWorkspaceFiles[i]);

                try (final ZipInputStream zip = new ZipInputStream(read))
                {
                    ZipEntry entry;
                    while ((entry = zip.getNextEntry()) != null)
                    {
                        final String filePath = entry.getName().startsWith(XLSX_FOLDER_NAME) ? entry.getName().substring(XLSX_FOLDER_NAME.length())
                                : entry.getName();
                        if (!entry.isDirectory() && filePath.startsWith(FILE_SERVICES_FOLDER_NAME))
                        {
                            String fileServicePath = PATH_SEPARATOR + filePath.substring(FILE_SERVICES_FOLDER_NAME.length());
                            try (final OutputStream outputStream = FileServerUtils.newOutputStream(fileServicePath))
                            {
                                zip.transferTo(outputStream);
                            }
                        }
                    }
                }
            }
        }
    }
}