package ch.ethz.sis.openbis.generic.excel.v3.from;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.entity.AbstractEntityPropertyHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.id.ObjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IEntityType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.ProjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.SemanticAnnotation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import ch.ethz.sis.openbis.generic.excel.v3.from.enums.ImportTypes;
import ch.ethz.sis.openbis.generic.excel.v3.from.enums.ScriptTypes;
import ch.ethz.sis.openbis.generic.excel.v3.from.helper.*;
import ch.ethz.sis.openbis.generic.excel.v3.from.utils.ExcelParser;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.systemsx.cisd.common.exceptions.UserFailureException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ExcelReader
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

    private final Path[] sessionWorkspaceFiles;

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

    private final ScriptHelper scriptHelper;

    private final Map<String, String> importValues;

    private final byte[][] xls;

    private static final String ZIP_EXTENSION = "." + "zip";

    private Map<String, List<Path>> miscellaneous = new LinkedHashMap<>();

    public static enum Format { ZIP_EXPORT, EXCEL }

    public static OpenBisModel convert(Format inputFormat, Path inputFile) throws IOException {
            if (inputFormat != Format.EXCEL && inputFormat != Format.ZIP_EXPORT) {
                throw new IllegalArgumentException("Argument with name inputFormat and value: " + inputFormat +  " not supported.");
            }
            ExcelReader excelReader = new ExcelReader(new Path[]{ inputFile });
            return excelReader.start();
    }

    private ExcelReader(
            Path[] sessionWorkspaceFiles) throws IOException
    {
        this.vocabularyHelper = new VocabularyHelper();
        this.vocabularyTermHelper = new VocabularyTermHelper(vocabularyHelper);
        this.dataSetTypeHelper = new DataSetTypeHelper();
        this.collectionTypeHelper = new CollectionTypeHelper();
        this.objectTypeHelper = new ObjectTypeHelper();
        this.spaceHelper = new SpaceHelper();
        this.projectHelper = new ProjectHelper(spaceHelper);
        this.collectionHelper =
                new CollectionHelper(collectionTypeHelper, projectHelper);
        this.objectHelper =
                new ObjectHelper(spaceHelper, projectHelper, collectionTypeHelper,
                        objectTypeHelper, collectionHelper);
        this.propertyHelper = new PropertyHelper(vocabularyHelper, objectTypeHelper);

        this.propertyAssignmentHelper =
                new PropertyAssignmentHelper(dataSetTypeHelper, collectionTypeHelper,
                        objectTypeHelper, this.propertyHelper);
        Map<String, String> scripts = new HashMap<>();

        this.scriptHelper = new ScriptHelper(scripts);

        this.sessionWorkspaceFiles = sessionWorkspaceFiles;
        this.semanticAnnotationHelper = new SemanticAnnotationHelper();

        // File Parsing
        byte[][] xls = new byte[sessionWorkspaceFiles.length][];
        this.importValues = new HashMap<>();

        for (int i = 0; i < sessionWorkspaceFiles.length; i++)
        {
            InputStream read = Files.newInputStream(sessionWorkspaceFiles[i]);
            if (sessionWorkspaceFiles[i].toString().endsWith(ZIP_EXTENSION))
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

                final File canonicalFile = sessionWorkspaceFiles[i].toFile();
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
                            this.importValues.put(file.getName(),
                                    Files.readString(file.toPath(), StandardCharsets.UTF_8));
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

    private OpenBisModel start() throws IOException
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
                            scriptHelper.importBlock(page, pageNumber, lineNumber, lineNumber + 2,
                                    ScriptTypes.VALIDATION_SCRIPT);

                            objectTypeHelper.importBlock(page, pageNumber, lineNumber,
                                    lineNumber + 2);
                            semanticAnnotationHelper.importBlockForEntityType(page,
                                    pageNumber, lineNumber, lineNumber + 2,
                                    ImportTypes.SAMPLE_TYPE);

                            if (lineNumber + 2 != blockEnd)
                            {
                                scriptHelper.importBlock(page, pageNumber, lineNumber + 2, blockEnd,
                                        ScriptTypes.DYNAMIC_SCRIPT);

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
                            scriptHelper.importBlock(page, pageNumber, lineNumber, lineNumber + 2,
                                    ScriptTypes.VALIDATION_SCRIPT);

                            collectionTypeHelper.importBlock(page, pageNumber, lineNumber,
                                    lineNumber + 2);
                            if (lineNumber + 2 != blockEnd)
                            {
                                scriptHelper.importBlock(page, pageNumber, lineNumber + 2, blockEnd,
                                        ScriptTypes.DYNAMIC_SCRIPT);

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
                            scriptHelper.importBlock(page, pageNumber, lineNumber, lineNumber + 2,
                                    ScriptTypes.VALIDATION_SCRIPT);

                            dataSetTypeHelper.importBlock(page, pageNumber, lineNumber,
                                    lineNumber + 2);

                            if (lineNumber + 2 != blockEnd)
                            {
                                scriptHelper.importBlock(page, pageNumber, lineNumber + 2, blockEnd,
                                        ScriptTypes.DYNAMIC_SCRIPT);

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
        propertyHelper.resolveReferences();
        Map<ObjectIdentifier, AbstractEntityPropertyHolder> metadata = new HashMap<>();

        Map<ObjectIdentifier, AbstractEntityPropertyHolder> objectResult = objectHelper.getResult();
        Map<ObjectIdentifier, AbstractEntityPropertyHolder> collectionResult = collectionHelper.getResult();

        metadata.putAll(objectResult);
        metadata.putAll(collectionResult);
        Map<ProjectIdentifier, Project> projectResult = projectHelper.getResult();
        Map<SpacePermId, Space> spaceResult = spaceHelper.getResult();
        SemanticAnnotationByKind semanticAnnotationByKind =
                semanticAnnotationHelper.getResult();
        setSemanticAnnotations(schema, semanticAnnotationByKind);

        return new OpenBisModel(Map.of(), schema, spaceResult, projectResult, metadata,
                scriptHelper.getResults(), miscellaneous);
    }

    private void setSemanticAnnotations(Map<EntityTypePermId, IEntityType> schema,
            SemanticAnnotationByKind semanticAnnotationByKind)
    {

        for (Map.Entry<EntityTypePermId, IEntityType> entry : schema.entrySet())
        {
            if (entry.getKey().getEntityKind() != EntityKind.SAMPLE)
            {
                continue;
            }

            List<SemanticAnnotation> semanticAnnotations = Optional.ofNullable(
                            semanticAnnotationByKind.getEntityTypeAnnotations()
                                    .get(entry.getKey().getPermId()))
                    .map(x -> x.stream()
                            .collect(Collectors.toList()))
                    .orElse(List.of());
            SampleType sampleType = (SampleType) entry.getValue();
            sampleType.setSemanticAnnotations(semanticAnnotations);
            for (var property : sampleType.getPropertyAssignments())
            {
                List<SemanticAnnotation> propertySemanticAnnotations = Optional.ofNullable(
                                semanticAnnotationByKind
                                        .getEntityPropertyTypeAnnotations()
                                        .get(property.getPropertyType().getCode()))
                        .map(x -> x.stream()
                                .collect(Collectors.toList()))
                        .orElse(List.of());
                property.getPropertyType().setSemanticAnnotations(propertySemanticAnnotations);
            }
        }

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
        for (Path sessionWorkspaceFile : this.sessionWorkspaceFiles)
        {
            if (sessionWorkspaceFile.toString().endsWith(ZIP_EXTENSION))
            {
                InputStream read = Files.newInputStream(sessionWorkspaceFile);

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
                            List<Path> paths =
                                    miscellaneous.getOrDefault("FILE_SERVICES_FOLDER_NAME",
                                            new ArrayList<>());
                            paths.add(
                                    Path.of(filePath.replaceFirst(FILE_SERVICES_FOLDER_NAME, "")));
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

    private static OutputStream newOutputStream(String dst) throws IOException
    {
        final Path filePathAsPath = Path.of(dst);
        Files.createDirectories(filePathAsPath.getParent());
        return Files.newOutputStream(filePathAsPath);
    }

}
