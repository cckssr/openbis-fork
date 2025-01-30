package ch.openbis.rocrate.app.parser;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.entity.AbstractEntity;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.id.ObjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IEntityType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.ProjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.openbis.rocrate.app.parser.helper.*;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.openbis.rocrate.app.parser.results.ParseResult;
import ch.openbis.rocrate.app.parser.stuff.ImportTypes;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ExcelConversionParser
{

    private static final String PATH_SEPARATOR = "/";

    private static final String XLSX_FOLDER_NAME = "xlsx" + PATH_SEPARATOR;

    private static final String SCRIPTS_FOLDER_NAME = "scripts" + PATH_SEPARATOR;

    private static final String DATA_FOLDER_NAME = "data" + PATH_SEPARATOR;

    private static final String MISCELLANEOUS_FOLDER_NAME = "miscellaneous" + PATH_SEPARATOR;

    private static final String FILE_SERVICES_FOLDER_NAME =
            MISCELLANEOUS_FOLDER_NAME + "file-service" + PATH_SEPARATOR;

    private static final String XLS_EXTENSION = "." + "xls";

    private static final String XLSX_EXTENSION = "." + "xlsx";

    private static final int XLSX_DOCUMENT_LIMIT = 536870912; // 512 MB

    private static final int EMBEDDED_DOCUMENT_LIMIT = 16777216; // 16 MB

    private final String[] sessionWorkspaceFiles;

    private final VocabularyHelper vocabularyHelper;

    private final VocabularyTermHelper vocabularyTermHelper;

    private final DataSetTypeHelper dataSetTypeHelper;

    private final PropertyHelper propertyHelper;

    private final PropertyAssignmentHelper propertyAssignmentHelper;

    private final CollectionTypeHelper collectionTypeHelper;

    private final ObjectTypeHelper objectTypeHelper;

    private final SpaceHelper spaceHelper;

    private final ObjectHelper objectHelper;

    private final ProjectHelper projectHelper;

    private final CollectionHelper collectionHelper;

    private final SemanticAnnotationHelper semanticAnnotationHelper;

    private final Map<String, String> importValues;

    private final byte[][] xls;

    private static final String ZIP_EXTENSION = "." + "zip";

    public ExcelConversionParser(
            String[] sessionWorkspaceFiles) throws IOException
    {
        this.vocabularyHelper = new VocabularyHelper();
        this.vocabularyTermHelper = new VocabularyTermHelper(vocabularyHelper);
        this.dataSetTypeHelper = new DataSetTypeHelper();
        this.propertyHelper = new PropertyHelper(vocabularyHelper);
        this.collectionTypeHelper = new CollectionTypeHelper();
        this.objectTypeHelper = new ObjectTypeHelper();
        this.spaceHelper = new SpaceHelper();
        this.projectHelper = new ProjectHelper(spaceHelper);
        this.collectionHelper =
                new CollectionHelper(collectionTypeHelper, projectHelper);
        this.objectHelper =
                new ObjectHelper(spaceHelper, projectHelper, collectionTypeHelper,
                        objectTypeHelper, collectionHelper);

        this.propertyAssignmentHelper =
                new PropertyAssignmentHelper(dataSetTypeHelper, collectionTypeHelper,
                        objectTypeHelper, this.propertyHelper);

        this.sessionWorkspaceFiles = sessionWorkspaceFiles;
        this.semanticAnnotationHelper = new SemanticAnnotationHelper();

        // File Parsing
        Map<String, String> scripts = new HashMap<>();
        byte[][] xls = new byte[sessionWorkspaceFiles.length][];
        this.importValues = new HashMap<>();

        for (int i = 0; i < sessionWorkspaceFiles.length; i++)
        {
            InputStream read = Files.newInputStream(new File(sessionWorkspaceFiles[i]).toPath());
            if (sessionWorkspaceFiles[i].toLowerCase().endsWith(ZIP_EXTENSION))
            {
                try (final ZipInputStream zip = new ZipInputStream(read))
                {
                    ZipEntry entry;
                    while ((entry = zip.getNextEntry()) != null)
                    {
                        final String entryName = entry.getName().startsWith(XLSX_FOLDER_NAME) ?
                                entry.getName().substring(XLSX_FOLDER_NAME.length())
                                :
                                entry.getName();
                        if (entry.isDirectory())
                        {
                            if (!entryName.isEmpty() &&
                                    !SCRIPTS_FOLDER_NAME.equals(entryName) &&
                                    !DATA_FOLDER_NAME.equals(entryName) &&
                                    !entryName.startsWith(MISCELLANEOUS_FOLDER_NAME))
                            {
                                throw UserFailureException.fromTemplate(
                                        "Illegal directory '%s' is found inside the imported file.",
                                        entryName);
                            }
                        } else
                        {
                            if (!entryName.contains(PATH_SEPARATOR) && (entryName.endsWith(
                                    XLS_EXTENSION) || entryName.endsWith(XLSX_EXTENSION)))
                            {
                                validateEntrySize(entry.getSize(), XLSX_DOCUMENT_LIMIT);
                                if (xls[i] == null)
                                {
                                    xls[i] = zip.readAllBytes();
                                } else
                                {
                                    throw UserFailureException.fromTemplate(
                                            "More than one XLS file found in the root of the imported ZIP file.");
                                }
                            } else if (entryName.startsWith(SCRIPTS_FOLDER_NAME))
                            {
                                validateEntrySize(entry.getSize(), EMBEDDED_DOCUMENT_LIMIT);
                                scripts.put(entryName.substring(SCRIPTS_FOLDER_NAME.length()),
                                        new String(zip.readAllBytes()));
                            } else if (entryName.startsWith(DATA_FOLDER_NAME))
                            {
                                validateEntrySize(entry.getSize(), EMBEDDED_DOCUMENT_LIMIT);
                                this.importValues.put(
                                        entryName.substring(DATA_FOLDER_NAME.length()),
                                        new String(zip.readAllBytes()));
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

                final File canonicalFile = new File(sessionWorkspaceFiles[i]);
                validateEntrySize(canonicalFile.length(), XLSX_DOCUMENT_LIMIT);
                xls[i] = Files.readAllBytes(canonicalFile.toPath());

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
                            scripts.put(file.getName(),
                                    Files.readString(file.toPath(), StandardCharsets.UTF_8));
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
                            //this.importValues.put(file.getName(), Files.readString(file.toPath(), StandardCharsets.UTF_8));
                        }
                    }
                }
            }
        }

        this.xls = xls;
    }

    private static void validateEntrySize(final long size, final int limit)
    {
        if (size > limit)
        {
            throw UserFailureException.fromTemplate("Document limit exceeded: %d.", size);
        }
    }

    public ParseResult start() throws IOException
    {

        for (byte[] xl : this.xls)
        {
            List<List<List<String>>> lines = ExcelParser.parseExcel(xl, importValues);
            int pageNumber = 0;
            int lineNumber;

            while (pageNumber < lines.size())
            {
                lineNumber = 0;
                List<List<String>> page = lines.get(pageNumber);
                int pageEnd = getPageEnd(page);
                while (lineNumber < pageEnd)
                {
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
                            vocabularyHelper.importBlock(page, pageNumber, lineNumber,
                                    lineNumber + 2);
                            if (lineNumber + 2 != blockEnd)
                            {
                                vocabularyTermHelper.importBlock(page, pageNumber, lineNumber,
                                        blockEnd);
                            }
                            break;
                        case SAMPLE_TYPE:
                            objectTypeHelper.importBlock(page, pageNumber, lineNumber,
                                    lineNumber + 2);
                            if (lineNumber + 2 != blockEnd)
                            {
                                propertyHelper.importBlock(page, pageNumber, lineNumber + 2,
                                        blockEnd);
                                propertyAssignmentHelper.importBlock(page, pageNumber, lineNumber,
                                        blockEnd, ImportTypes.SAMPLE_TYPE);
                                semanticAnnotationHelper.importBlockForEntityTypeProperty(page,
                                        pageNumber, lineNumber, blockEnd,
                                        ImportTypes.SAMPLE_TYPE);

                            }
                            break;
                        case EXPERIMENT_TYPE:
                            collectionTypeHelper.importBlock(page, pageNumber, lineNumber,
                                    lineNumber + 2);
                            if (lineNumber + 2 != blockEnd)
                            {
                                propertyHelper.importBlock(page, pageNumber, lineNumber + 2,
                                        blockEnd);
                                propertyAssignmentHelper.importBlock(page, pageNumber, lineNumber,
                                        blockEnd, ImportTypes.EXPERIMENT_TYPE);
                                semanticAnnotationHelper.importBlockForEntityTypeProperty(page,
                                        pageNumber, lineNumber, blockEnd,
                                        ImportTypes.EXPERIMENT_TYPE);
                            }
                            break;
                        case DATASET_TYPE:
                            dataSetTypeHelper.importBlock(page, pageNumber, lineNumber,
                                    lineNumber + 2);

                            if (lineNumber + 2 != blockEnd)
                            {
                                propertyHelper.importBlock(page, pageNumber, lineNumber + 2,
                                        blockEnd);
                                propertyAssignmentHelper.importBlock(page, pageNumber, lineNumber,
                                        blockEnd, ImportTypes.DATASET_TYPE);
                                semanticAnnotationHelper.importBlockForEntityType(page, pageNumber,
                                        lineNumber, lineNumber + 2, ImportTypes.DATASET_TYPE);

                            }

                            break;
                        case SPACE:
                            spaceHelper.importBlock(page, pageNumber, lineNumber, blockEnd);
                            break;
                        case PROJECT:
                            projectHelper.importBlock(page, pageNumber, lineNumber, blockEnd);
                            break;
                        case EXPERIMENT:
                            collectionHelper.importBlock(page, pageNumber, lineNumber, blockEnd);
                            break;
                        case SAMPLE:
                            objectHelper.importBlock(page, pageNumber, lineNumber, blockEnd);
                            break;

                        case PROPERTY_TYPE:
                            semanticAnnotationHelper.importBlockForPropertyType(page, pageNumber,
                                    lineNumber, blockEnd);
                            break;
                        default:
                            throw new UserFailureException("Unknown type: " + blockType);
                    }
                    lineNumber = blockEnd + 1;
                }
                pageNumber++;
            }
        }

        importZipData();
        Map<EntityTypePermId, IEntityType> entityTypeMap = dataSetTypeHelper.getResult();
        Map<EntityTypePermId, IEntityType> collectionTypeHelperResult =
                collectionTypeHelper.getResult();
        Map<EntityTypePermId, IEntityType> objectTypreResult = objectTypeHelper.getResult();
        Map<EntityTypePermId, IEntityType> dataSetResult = dataSetTypeHelper.getResult();

        Map<EntityTypePermId, IEntityType> schema = new HashMap<>();
        schema.putAll(entityTypeMap);
        schema.putAll(collectionTypeHelperResult);
        schema.putAll(objectTypreResult);
        schema.putAll(dataSetResult);

        objectHelper.resolveReferences();
        Map<ObjectIdentifier, AbstractEntity> metadata = new HashMap<>();

        Map<ObjectIdentifier, AbstractEntity> objectResult = objectHelper.getResult();
        Map<ObjectIdentifier, AbstractEntity> collectionResult = collectionHelper.getResult();

        metadata.putAll(objectResult);
        metadata.putAll(collectionResult);
        Map<ProjectIdentifier, Project> projectResult = projectHelper.getResult();
        Map<String, Space> spaceResult = spaceHelper.getResult();
        SemanticAnnotationHelper.SemanticAnnotationByKind semanticAnnotationByKind =
                semanticAnnotationHelper.getResult();

        return new ParseResult(schema, metadata, projectResult, spaceResult,
                semanticAnnotationByKind);
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
                        throw new UserFailureException(
                                "Content found after a double blank row that should mark the end of a page.");
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
        for (String sessionWorkspaceFile : this.sessionWorkspaceFiles)
        {
            if (sessionWorkspaceFile.toLowerCase().endsWith(ZIP_EXTENSION))
            {
                InputStream read = Files.newInputStream(new File(sessionWorkspaceFile).toPath());

                try (final ZipInputStream zip = new ZipInputStream(read))
                {
                    ZipEntry entry;
                    while ((entry = zip.getNextEntry()) != null)
                    {
                        final String filePath = entry.getName().startsWith(XLSX_FOLDER_NAME) ?
                                entry.getName().substring(XLSX_FOLDER_NAME.length())
                                :
                                entry.getName();
                        if (!entry.isDirectory() && filePath.startsWith(FILE_SERVICES_FOLDER_NAME))
                        {
                            String fileServicePath = PATH_SEPARATOR + filePath.substring(
                                    FILE_SERVICES_FOLDER_NAME.length());
                            try (final OutputStream outputStream = newOutputStream(
                                    fileServicePath))
                            {
                                zip.transferTo(outputStream);
                            }
                        }
                    }
                }
            }
        }
    }

    public static OutputStream newOutputStream(String dst) throws IOException
    {
        final Path filePathAsPath = Path.of(dst);
        Files.createDirectories(filePathAsPath.getParent());
        return Files.newOutputStream(filePathAsPath);
    }

}
