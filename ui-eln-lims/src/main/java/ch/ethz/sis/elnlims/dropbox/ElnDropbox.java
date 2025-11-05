package ch.ethz.sis.elnlims.dropbox;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.ethz.sis.afsapi.api.ClientAPI;
import ch.ethz.sis.openbis.generic.foldermonitor.v3.FolderMonitorTask;
import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.IExperimentId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.create.SampleCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.ISampleId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SamplePermId;
import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.ethz.sis.shared.log.classic.impl.Logger;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.common.properties.PropertyUtils;

public class ElnDropbox implements FolderMonitorTask
{

    private static final Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION, ElnDropbox.class);

    public static final String PROPERTY_APPLICATION_SERVER_URL = "application-server-url";

    public static final String PROPERTY_AFS_SERVER_URL = "afs-server-url";

    public static final String PROPERTY_PERSONAL_ACCESS_TOKEN = "personal-access-token";

    public static final String PROPERTY_INTERACTIVE_SESSION_KEY = "interactive-session-key";

    public static final String PROPERTY_DISCARD_FILES_PATTERNS = "discard-files-patterns";

    public static final String PROPERTY_ILLEGAL_FILES_PATTERNS = "illegal-files-patterns";

    private static final String INVALID_FORMAT_ERROR_MESSAGE =
            "Invalid format for the folder name, should follow the pattern <ENTITY_KIND>+<SPACE_CODE>+<PROJECT_CODE>+[<EXPERIMENT_CODE>|<SAMPLE_CODE>]+<OPTIONAL_DATASET_TYPE>+<OPTIONAL_NAME>";

    private static final String FAILED_TO_PARSE_ERROR_MESSAGE = "Failed to parse folder name";

    private static final String FAILED_TO_PARSE_SAMPLE_ERROR_MESSAGE = "Failed to parse sample";

    private static final String FAILED_TO_PARSE_EXPERIMENT_ERROR_MESSAGE = "Failed to parse experiment";

    private static final String FAILED_TO_PARSE_JSON_METADATA_FILE_ERROR_MESSAGE = "Failed to parse JSON metadata file";

    private static final String FOLDER_CONTAINS_NON_DELETABLE_FILES_ERROR_MESSAGE = "Folder contains non-deletable files";

    private static final String SAMPLE_MISSING_ERROR_MESSAGE = "Sample not found";

    private static final String EXPERIMENT_MISSING_ERROR_MESSAGE = "Experiment not found";

    private static final String NAME_PROPERTY_SET_IN_TWO_PLACES_ERROR_MESSAGE =
            "NAME property specified twice, it should just be in either folder name or metadata.json";

    private static final String ILLEGAL_FILES_ERROR_MESSAGE = "Directory contains illegal files";

    private static final String INVALID_PATTERN_ERROR_MESSAGE = "Provided pattern could not be compiled";

    private List<Pattern> discardFilesPatterns;

    private List<Pattern> illegalFilesPatterns;

    private ObjectMapper jsonObjectMapper;

    private OpenBIS openBIS;

    @Override public void configure(final Properties properties)
    {
        String applicationServerUrl = PropertyUtils.getMandatoryProperty(properties, PROPERTY_APPLICATION_SERVER_URL);
        String afsServerUrl = PropertyUtils.getMandatoryProperty(properties, PROPERTY_AFS_SERVER_URL);
        String personalAccessToken = PropertyUtils.getMandatoryProperty(properties, PROPERTY_PERSONAL_ACCESS_TOKEN);
        String interactiveSessionKey = PropertyUtils.getMandatoryProperty(properties, PROPERTY_INTERACTIVE_SESSION_KEY);

        openBIS = new OpenBIS(applicationServerUrl, null, afsServerUrl);
        openBIS.setSessionToken(personalAccessToken);
        openBIS.setInteractiveSessionKey(interactiveSessionKey);

        List<String> discardFilesStringPatterns = PropertyUtils.getList(properties, PROPERTY_DISCARD_FILES_PATTERNS);
        discardFilesPatterns = compilePatterns(discardFilesStringPatterns);

        List<String> illegalFilesStringPatterns = PropertyUtils.getList(properties, PROPERTY_ILLEGAL_FILES_PATTERNS);
        illegalFilesPatterns = compilePatterns(illegalFilesStringPatterns);

        jsonObjectMapper = new ObjectMapper();
    }

    private List<Pattern> compilePatterns(List<String> stringPatterns)
    {
        List<Pattern> patterns = new ArrayList<>();

        for (String stringPattern : stringPatterns)
        {
            try
            {
                patterns.add(Pattern.compile(stringPattern));
            } catch (PatternSyntaxException e)
            {
                throw new UserFailureException(INVALID_PATTERN_ERROR_MESSAGE, e);
            }
        }

        return patterns;
    }

    @Override
    public void process(Path incoming)
    {
        try
        {
            openBIS.beginTransaction();

            String folderName = substringUpToHash(incoming.getFileName().toString());

            deleteFilesMatchingPatterns(incoming, discardFilesPatterns);
            validateIllegalFilesMatchingPatterns(incoming, illegalFilesPatterns);

            if (folderName.startsWith("."))
            {
                return;
            }

            String[] datasetInfo = folderName.split("\\+");
            String entityKind = null;
            Sample sample = null;
            Experiment experiment = null;
            String datasetType = "UNKNOWN";
            String name = null;

            // Parse entity Kind
            if (datasetInfo.length >= 1)
            {
                entityKind = datasetInfo[0];
            } else
            {
                throw new UserFailureException(INVALID_FORMAT_ERROR_MESSAGE + ":" + FAILED_TO_PARSE_ERROR_MESSAGE);
            }

            boolean projectSamplesEnabled = Boolean.parseBoolean(openBIS.getServerInformation().get("project-samples-enabled"));

            // Parse entity Kind Format
            if ("S".equals(entityKind))
            {
                if (datasetInfo.length >= 3)
                {
                    String sampleSpace = datasetInfo[1];
                    String sampleCode = datasetInfo[2];

                    sample = getSample(openBIS, new SampleIdentifier("/" + sampleSpace + "/" + sampleCode));

                    if (sample == null)
                    {
                        throw new UserFailureException(INVALID_FORMAT_ERROR_MESSAGE + ":" + SAMPLE_MISSING_ERROR_MESSAGE);
                    }
                    if (datasetInfo.length >= 4)
                    {
                        datasetType = datasetInfo[3];
                    }
                    if (datasetInfo.length >= 5)
                    {
                        name = datasetInfo[4];
                    }
                    if (datasetInfo.length > 5)
                    {
                        throw new UserFailureException(INVALID_FORMAT_ERROR_MESSAGE + ":" + FAILED_TO_PARSE_SAMPLE_ERROR_MESSAGE);
                    }
                } else
                {
                    throw new UserFailureException(INVALID_FORMAT_ERROR_MESSAGE + ":" + FAILED_TO_PARSE_SAMPLE_ERROR_MESSAGE);
                }
            } else if ("O".equals(entityKind))
            {
                if (datasetInfo.length >= 4 && projectSamplesEnabled)
                {
                    String sampleSpace = datasetInfo[1];
                    String projectCode = datasetInfo[2];
                    String sampleCode = datasetInfo[3];

                    sample = getSample(openBIS, new SampleIdentifier("/" + sampleSpace + "/" + projectCode + "/" + sampleCode));

                    if (sample == null)
                    {
                        throw new UserFailureException(INVALID_FORMAT_ERROR_MESSAGE + ":" + SAMPLE_MISSING_ERROR_MESSAGE);
                    }
                    if (datasetInfo.length >= 5)
                    {
                        datasetType = datasetInfo[4];
                    }
                    if (datasetInfo.length >= 6)
                    {
                        name = datasetInfo[5];
                    }
                    if (datasetInfo.length > 6)
                    {
                        throw new UserFailureException(INVALID_FORMAT_ERROR_MESSAGE + ":" + FAILED_TO_PARSE_SAMPLE_ERROR_MESSAGE);
                    }
                } else
                {
                    throw new UserFailureException(INVALID_FORMAT_ERROR_MESSAGE + ":" + FAILED_TO_PARSE_SAMPLE_ERROR_MESSAGE);
                }

                List<Path> readOnlyFiles = getReadOnlyFiles(incoming);
                if (!readOnlyFiles.isEmpty())
                {
                    throw new UserFailureException(
                            FOLDER_CONTAINS_NON_DELETABLE_FILES_ERROR_MESSAGE + ":" + FAILED_TO_PARSE_SAMPLE_ERROR_MESSAGE + ":\n" + pathListToStr(
                                    readOnlyFiles));
                }
            } else if ("E".equals(entityKind))
            {
                if (datasetInfo.length >= 4)
                {
                    String experimentSpace = datasetInfo[1];
                    String projectCode = datasetInfo[2];
                    String experimentCode = datasetInfo[3];

                    experiment = getExperiment(openBIS, new ExperimentIdentifier("/" + experimentSpace + "/" + projectCode + "/" + experimentCode));

                    if (experiment == null)
                    {
                        throw new UserFailureException(INVALID_FORMAT_ERROR_MESSAGE + ":" + EXPERIMENT_MISSING_ERROR_MESSAGE);
                    }
                    if (datasetInfo.length >= 5)
                    {
                        datasetType = datasetInfo[4];
                    }
                    if (datasetInfo.length >= 6)
                    {
                        name = datasetInfo[5];
                    }
                    if (datasetInfo.length > 6)
                    {
                        throw new UserFailureException(INVALID_FORMAT_ERROR_MESSAGE + ":" + FAILED_TO_PARSE_EXPERIMENT_ERROR_MESSAGE);
                    }
                } else
                {
                    throw new UserFailureException(INVALID_FORMAT_ERROR_MESSAGE + ":" + FAILED_TO_PARSE_EXPERIMENT_ERROR_MESSAGE);
                }

                List<Path> readOnlyFiles = getReadOnlyFiles(incoming);
                if (!readOnlyFiles.isEmpty())
                {
                    throw new UserFailureException(
                            FOLDER_CONTAINS_NON_DELETABLE_FILES_ERROR_MESSAGE + ":" + FAILED_TO_PARSE_EXPERIMENT_ERROR_MESSAGE + ":\n"
                                    + pathListToStr(readOnlyFiles));
                }
            } else
            {
                throw new UserFailureException(INVALID_FORMAT_ERROR_MESSAGE);
            }

            // Create "dataset"
            SampleCreation ownerCreation = new SampleCreation();
            ownerCreation.setTypeId(new EntityTypePermId(datasetType));
            ownerCreation.setAutoGeneratedCode(true);

            // Set name if found
            if (name != null)
            {
                ownerCreation.setProperty("NAME", name);
            }

            // Set sample or experiment
            if (sample != null)
            {
                ownerCreation.setParentIds(List.of(sample.getPermId()));
                ownerCreation.setSpaceId(sample.getSpace().getId());
                if (sample.getProject() != null)
                {
                    ownerCreation.setProjectId(sample.getProject().getPermId());
                }
                if (sample.getExperiment() != null)
                {
                    ownerCreation.setExperimentId(sample.getExperiment().getPermId());
                }
            } else
            {
                ownerCreation.setSpaceId(experiment.getProject().getSpace().getId());
                ownerCreation.setProjectId(experiment.getProject().getPermId());
                ownerCreation.setExperimentId(experiment.getPermId());
            }

            // Move folder to dataset
            int itemsInFolder = 0;
            Path datasetItem = null;

            try (Stream<Path> filesInFolder = Files.list(incoming))
            {
                for (Path item : filesInFolder.collect(Collectors.toList()))
                {
                    String fileName = item.getFileName().toString();
                    if (fileName.equals("metadata.json"))
                    {
                        InputStream metadataStream = new BufferedInputStream(Files.newInputStream(item));
                        Map metadata;
                        try
                        {
                            metadata = jsonObjectMapper.readValue(metadataStream, Map.class);
                        } catch (Exception e)
                        {
                            throw new UserFailureException(FAILED_TO_PARSE_JSON_METADATA_FILE_ERROR_MESSAGE, e);
                        }
                        Map<String, String> properties = (Map<String, String>) metadata.get("properties");

                        for (String propertyKey : properties.keySet())
                        {
                            if (propertyKey.equals("NAME") && name != null)
                            {
                                throw new UserFailureException(NAME_PROPERTY_SET_IN_TWO_PLACES_ERROR_MESSAGE);
                            }

                            String propertyValue = properties.get(propertyKey);

                            if (propertyValue != null)
                            {
                                ownerCreation.setProperty(propertyKey, propertyValue);
                            }
                        }
                    } else
                    {
                        itemsInFolder = itemsInFolder + 1;
                        datasetItem = item;
                    }
                }
            }

            SamplePermId ownerId = openBIS.createSamples(List.of(ownerCreation)).get(0);

            // upload
            if (itemsInFolder > 1)
            {
                openBIS.getAfsServerFacade().upload(incoming, ownerId.getPermId(), Path.of("/default"), ClientAPI.overrideCollisionListener,
                        new ClientAPI.DefaultTransferMonitorLister());
            } else if (datasetItem != null)
            {
                if (Files.isDirectory(datasetItem))
                {
                    openBIS.getAfsServerFacade().upload(datasetItem, ownerId.getPermId(), Path.of("/" + datasetItem.getFileName().toString()),
                            ClientAPI.overrideCollisionListener, new ClientAPI.DefaultTransferMonitorLister());
                } else
                {
                    openBIS.getAfsServerFacade().upload(datasetItem, ownerId.getPermId(), Path.of("/"), ClientAPI.overrideCollisionListener,
                            new ClientAPI.DefaultTransferMonitorLister());
                }
            }

            openBIS.commitTransaction();
        } catch (Exception exception)
        {
            try
            {
                openBIS.rollbackTransaction();
            } catch (Exception rollbackException)
            {
                operationLog.warn("Couldn't rollback transaction", rollbackException);
            }
            throw new UserFailureException(exception.getMessage(), exception);
        }
    }

    private String substringUpToHash(String inputString)
    {
        int hashIndex = inputString.lastIndexOf("#");
        return hashIndex >= 0 ? inputString.substring(0, hashIndex) : inputString;
    }

    private String pathListToStr(List<Path> list)
    {
        StringBuilder str = new StringBuilder();
        list.forEach(item ->
        {
            str.append("\n").append(item);
        });
        return str.toString();
    }

    private Sample getSample(OpenBIS openBIS, ISampleId sampleId)
    {
        SampleFetchOptions fetchOptions = new SampleFetchOptions();
        fetchOptions.withSpace();
        fetchOptions.withProject();
        fetchOptions.withExperiment();
        return openBIS.getSamples(List.of(sampleId), fetchOptions).get(sampleId);
    }

    private Experiment getExperiment(OpenBIS openBIS, IExperimentId experimentId)
    {
        ExperimentFetchOptions fetchOptions = new ExperimentFetchOptions();
        fetchOptions.withProject().withSpace();
        return openBIS.getExperiments(List.of(experimentId), fetchOptions).get(experimentId);
    }

    private void deleteFilesMatchingPatterns(Path file, List<Pattern> patterns) throws IOException
    {
        for (Pattern pattern : patterns)
        {
            if (pattern.matcher(file.getFileName().toString()).find())
            {
                deleteFiles(file);
                return;
            }
        }

        if (Files.isDirectory(file))
        {
            try (Stream<Path> filesInDirectory = Files.list(file))
            {
                filesInDirectory.forEach(fileInDirectory ->
                {
                    try
                    {
                        deleteFilesMatchingPatterns(fileInDirectory, patterns);
                    } catch (IOException e)
                    {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }

    private void deleteFiles(Path file) throws IOException
    {
        Files.walkFileTree(file, new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
            {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void validateIllegalFilesMatchingPatterns(Path file, List<Pattern> patterns) throws IOException
    {
        List<Path> illegalFiles = getIllegalFilesMatchingPatterns(file, patterns);

        if (!illegalFiles.isEmpty())
        {
            StringBuilder message = new StringBuilder(ILLEGAL_FILES_ERROR_MESSAGE + ":\n");

            illegalFiles.forEach(illegalFile ->
            {
                message.append(illegalFile.toString()).append("\n");
            });

            throw new UserFailureException(message.toString());
        }
    }

    private List<Path> getIllegalFilesMatchingPatterns(Path file, List<Pattern> patterns) throws IOException
    {
        List<Path> illegalFiles = new ArrayList<>();

        for (Pattern pattern : patterns)
        {
            if (pattern.matcher(file.getFileName().toString()).find())
            {
                illegalFiles.add(file);
                break;
            }
        }

        if (Files.isDirectory(file))
        {
            try (Stream<Path> filesInDirectory = Files.list(file))
            {
                filesInDirectory.forEach(fileInDirectory ->
                {
                    try
                    {
                        illegalFiles.addAll(getIllegalFilesMatchingPatterns(fileInDirectory, patterns));
                    } catch (IOException e)
                    {
                        throw new RuntimeException(e);
                    }
                });
            }
        }

        return illegalFiles;
    }

    private List<Path> getReadOnlyFiles(Path file) throws IOException
    {
        List<Path> readOnlyFiles = new ArrayList<>();

        Files.walkFileTree(file, new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
                try
                {
                    Files.move(file, file);
                } catch (Exception e)
                {
                    readOnlyFiles.add(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
            {
                try
                {
                    Files.move(dir, dir);
                } catch (Exception e)
                {
                    readOnlyFiles.add(dir);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return readOnlyFiles;
    }

}


