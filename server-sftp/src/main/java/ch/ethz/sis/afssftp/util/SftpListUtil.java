package ch.ethz.sis.afssftp.util;

import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afsclient.client.AfsClientUploadHelper;
import ch.ethz.sis.afssftp.authentication.User;
import ch.ethz.sis.afssftp.filesystemview.FtpPathLister;
import ch.ethz.sis.afssftp.filesystemview.SftpFileAttributes;
import ch.ethz.sis.afssftp.filesystemview.SftpNode;
import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.delete.DataSetDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.create.ExperimentCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.delete.ExperimentDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.update.ExperimentUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.create.ProjectCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.delete.ProjectDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.fetchoptions.ProjectFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.ProjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.create.SampleCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.delete.SampleDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SamplePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.update.SampleUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.create.SpaceCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.delete.SpaceDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.fetchoptions.SpaceFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.search.SpaceSearchCriteria;
import ch.ethz.sis.shared.log.standard.LogManager;
import ch.ethz.sis.shared.log.standard.Logger;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

import static ch.ethz.sis.afsclient.client.AfsClientUploadHelper.isPathNotInStoreError;

public class SftpListUtil {
    public static final String FOLDER_SAMPLE_TYPE = "FOLDER";
    public static final String ENTRY_SAMPLE_TYPE = "ENTRY";
    public static final String DATASET_ENTITY_TYPE_AFS_DATA = "AFS_DATA";
    public static final Set<SftpNode.Type> POSSIBLE_AFS_ENTITY_TYPES = Set.of(
            SftpNode.Type.FOLDER,
            SftpNode.Type.SAMPLE,
            SftpNode.Type.EXPERIMENT,
            SftpNode.Type.DATA_SET
    );
    public static final String PROPERTY_NAME = "NAME";
    public static final String EXPERIMENT_TYPE_COLLECTION = "COLLECTION";

    private final Logger logger = LogManager.getLogger(this.getClass());

    private final User user;
    private final OpenBISClientUtil openBISClientUtil;

    public SftpListUtil(@NonNull User user) {
        this.user = user;
        this.openBISClientUtil = new OpenBISClientUtil();
    }

    //For unit-tests
    SftpListUtil(
            User user,
            OpenBISClientUtil openBISClientUtil) {
        this.user = user;
        this.openBISClientUtil = openBISClientUtil;
    }

    public @NonNull List<Space> getSpaces() {
        OpenBIS openBIS = openBISClientUtil.getOpenBISClient(user);

        SearchResult<Space> spaces = openBIS.searchSpaces(new SpaceSearchCriteria(), new SpaceFetchOptions());

        return spaces.getObjects();
    }

    public @NonNull List<Project> getProjects(@NonNull String spacePermId) {
        OpenBIS openBIS = openBISClientUtil.getOpenBISClient(user);

        SpaceFetchOptions fetchOptions = new SpaceFetchOptions();
        fetchOptions.withProjects();
        SpacePermId spaceId = new SpacePermId(spacePermId);

        return Optional.ofNullable(openBIS.getSpaces(List.of(spaceId), fetchOptions).get(spaceId))
                .map(Space::getProjects).orElse(Collections.emptyList());
    }

    public @NonNull List<Experiment> getExperiments(@NonNull String spaceCode, @NonNull String projectCode) {
        OpenBIS openBIS = openBISClientUtil.getOpenBISClient(user);

        ProjectFetchOptions fetchOptions = new ProjectFetchOptions();
        fetchOptions.withExperiments();
        fetchOptions.withExperiments().withProperties();
        ProjectIdentifier projectId = new ProjectIdentifier(spaceCode, projectCode);

        return Optional.ofNullable(openBIS.getProjects(List.of(projectId), fetchOptions).get(projectId))
                .map(Project::getExperiments).orElse(Collections.emptyList());
    }

    public static boolean isOfTypeFolder(Sample sample) {
        return sample.getType().getCode().equals(FOLDER_SAMPLE_TYPE);
    }

    /***
     * @param spacePermId Space-entity code
     *
     * @return samples attached to space-entity
     */
    public @NonNull List<Sample> getSpaceSamples(@NonNull String spacePermId) {
        OpenBIS openBIS = openBISClientUtil.getOpenBISClient(user);

        SampleFetchOptions fetchOptions = new SampleFetchOptions();
        fetchOptions.withProperties();
        fetchOptions.withType();

        SampleSearchCriteria criteria = new SampleSearchCriteria();
        criteria.withSpace().withCode().thatEquals(spacePermId);
        criteria.withoutProject();

        List<Sample> objects = openBIS.searchSamples(criteria, fetchOptions).getObjects();

        return Optional.ofNullable(
                        objects
                )
                .map(samples -> samples.stream()
                        .toList()
                ).orElse(Collections.emptyList());
    }

    /***
     * @param spaceCode Space-entity code
     *
     * @param projectCode Project-entity code
     * @return samples attached to project-entity
     */
    public @NonNull List<Sample> getProjectSamples(@NonNull String spaceCode, @NonNull String projectCode) {
        OpenBIS openBIS = openBISClientUtil.getOpenBISClient(user);
        ProjectIdentifier projectId = new ProjectIdentifier(spaceCode, projectCode);

        SampleFetchOptions fetchOptions = new SampleFetchOptions();
        fetchOptions.withProperties();
        fetchOptions.withType();

        SampleSearchCriteria criteria = new SampleSearchCriteria();
        criteria.withProject().withId().thatEquals(projectId);
        criteria.withoutExperiment();

        List<Sample> objects = openBIS.searchSamples(criteria, fetchOptions).getObjects();

        return Optional.ofNullable(objects)
                .map(samples -> samples.stream()
                        .toList()
                ).orElse(Collections.emptyList());
    }

    /***
     * @param experimentPermId Experiment perm-id
     *
     * @return samples attached to experiment-entity
     */
    public @NonNull List<Sample> getExperimentSamples(
            @NonNull String experimentPermId) {
        OpenBIS openBIS = openBISClientUtil.getOpenBISClient(user);

        ExperimentFetchOptions fetchOptions = new ExperimentFetchOptions();
        fetchOptions.withSamples().withProperties();
        fetchOptions.withSamples().withType();

        ExperimentPermId experimentId = new ExperimentPermId(experimentPermId);

        return Optional.ofNullable(openBIS.getExperiments(List.of(experimentId), fetchOptions).get(experimentId))
                .map(Experiment::getSamples)
                .map(samples -> samples.stream()
                        .toList()
                ).orElse(Collections.emptyList());
    }

    /***
     * @param samplePermId Sample-entity perm-id
     * @return samples attached to sample-entity
     */
    public @NonNull List<Sample> getSampleChildren(@NonNull String samplePermId) {
        OpenBIS openBIS = openBISClientUtil.getOpenBISClient(user);

        SampleFetchOptions fetchOptions = new SampleFetchOptions();
        fetchOptions.withChildren().withProperties();
        fetchOptions.withChildren().withType();
        SamplePermId sampleId = new SamplePermId(samplePermId);

        return Optional.ofNullable(openBIS.getSamples(List.of(sampleId), fetchOptions).get(sampleId))
                .map(Sample::getChildren).orElse(Collections.emptyList());
    }

    /***
     * @param samplePermId Sample perm-id
     * @return datasets attached to sample-entity
     */
    public @NonNull List<DataSet> getSampleDatasets(@NonNull String samplePermId) {
        OpenBIS openBIS = openBISClientUtil.getOpenBISClient(user);

        SampleFetchOptions fetchOptions = new SampleFetchOptions();
        fetchOptions.withDataSets();
        fetchOptions.withDataSets().withProperties();
        fetchOptions.withDataSets().withType();
        SamplePermId sampleId = new SamplePermId(samplePermId);

        return Optional.ofNullable(openBIS.getSamples(List.of(sampleId), fetchOptions).get(sampleId))
                .map(Sample::getDataSets).orElse(Collections.emptyList());
    }

    /***
     * @param experimentPermId Experiment perm-id
     * @return datasets attached to experiment-entity
     */
    public @NonNull List<DataSet> getExperimentDatasets(@NonNull String experimentPermId) {
        OpenBIS openBIS = openBISClientUtil.getOpenBISClient(user);

        ExperimentFetchOptions fetchOptions = new ExperimentFetchOptions();
        fetchOptions.withDataSets();
        fetchOptions.withDataSets().withProperties();
        ExperimentPermId experimentId = new ExperimentPermId(experimentPermId);

        return Optional.ofNullable(openBIS.getExperiments(List.of(experimentId), fetchOptions).get(experimentId))
                .map(Experiment::getDataSets).orElse(Collections.emptyList());
    }

    public String getAfsEntityPermId(@NonNull SftpNode afsEntityNode) {
        return switch (afsEntityNode.getType()) {
            case SAMPLE, FOLDER, DATA_SET, EXPERIMENT ->
                    afsEntityNode.getIdentifier()
                            .map(SftpListUtil::getEntityPermIdFromDisplayName).orElseThrow();
            default -> null;
        };
    }

    public boolean isAfsEntityMutable(@NonNull String entityPermId, @NonNull SftpNode.Type type) {
        return switch (type) {
            case SAMPLE, FOLDER -> {
                OpenBIS openBIS = openBISClientUtil.getOpenBISClient(user);

                SamplePermId sampleId = new SamplePermId(entityPermId);
                yield  Optional.ofNullable(
                                openBIS.getSamples(List.of(sampleId), new SampleFetchOptions()).get(sampleId)
                        ).map(sample -> !sample.isImmutableData()).orElse(false);
            }
            case EXPERIMENT -> {
                OpenBIS openBIS = openBISClientUtil.getOpenBISClient(user);

                ExperimentPermId experimentId = new ExperimentPermId(entityPermId);
                yield  Optional.ofNullable(
                                openBIS.getExperiments(List.of(experimentId), new ExperimentFetchOptions()).get(experimentId)
                        ).map(exp -> !exp.isImmutableData()).orElse(false);
            }
            case DATA_SET -> false;
            default -> false;
        };
    }

    public @NonNull File[] listAfsFiles(@NonNull String afsEntityId, @NonNull String absoluteAfsFilePath) {
        try {
            return openBISClientUtil.getAfsClient(user).list(afsEntityId, absoluteAfsFilePath, false);
        } catch (Exception e) {
            if (isPathNotInStoreError(e)) {
                return new File[0];
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    @SneakyThrows
    public Optional<File> getAfsFilePresence(@NonNull String afsEntityId, @NonNull String absoluteAfsFilePath) {
        return AfsClientUploadHelper.getServerFilePresence(
                openBISClientUtil.getAfsClient(user),
                afsEntityId,
                absoluteAfsFilePath
        );
    }

    @SneakyThrows
    public Optional<SftpFileAttributes> getDefaultAfsFileAttributes(
            @NonNull String afsEntityId,
            @NonNull String absoluteAfsFilePath,
            boolean mutable) {
        return getAfsFilePresence(afsEntityId, absoluteAfsFilePath).map(
                file -> {
                    FileTime lastModified = file.getLastModifiedTime() != null ?
                            FileTime.from(file.getLastModifiedTime().toInstant()) :
                            FileTime.from(Instant.now());

                    return SftpFileAttributes.builder()
                            .creationTime(lastModified)
                            .modifiedTime(lastModified)
                            .accessTime(lastModified)
                            .directory(Boolean.TRUE.equals(file.getDirectory()))
                            .regularFile(!Boolean.TRUE.equals(file.getDirectory()))
                            .size(file.getSize() != null ? file.getSize() : 0)
                            .permissions( mutable ?
                                EnumSet.of(
                                    PosixFilePermission.OWNER_READ,
                                    PosixFilePermission.OWNER_WRITE,
                                    PosixFilePermission.OWNER_EXECUTE
                                ) : EnumSet.of(
                                    PosixFilePermission.OWNER_READ,
                                    PosixFilePermission.OWNER_EXECUTE
                                )
                            ).build();
                }
        );
    }

    public void createAfsFileRootIfNecessary(@NonNull String afsEntityId) {
        if (getAfsFilePresence(afsEntityId, "/").isEmpty()) {
            openBISClientUtil.getAfsClient(user).create(afsEntityId, "/", true);
        }
    }

    public void tryToCreateAfsFileRootIfNecessary(
            @NonNull String afsEntityId) {
        try {
            createAfsFileRootIfNecessary(afsEntityId);
        } catch (Exception e) {
            logger.catching(e);
        }
    }

    public static @NonNull SftpFileAttributes getDefaultAbstractDirectoryAttributes(
            boolean writable
    ) {
        return SftpFileAttributes.builder()
                .creationTime(FileTime.from(Instant.now()))
                .modifiedTime(FileTime.from(Instant.now()))
                .accessTime(FileTime.from(Instant.now()))
                .directory(true)
                .regularFile(false)
                .size(0)
                .permissions( writable ?
                    EnumSet.of(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OWNER_EXECUTE
                    ) : EnumSet.of(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_EXECUTE
                    )
                )
                .build();
    }

    public static @NonNull String getDisplayName(@NonNull Space space) {
        return space.getCode();
    }

    public static @NonNull String getDisplayName(@NonNull Project project) {
        return project.getCode();
    }

    public static @NonNull String getDisplayName(@NonNull Experiment experiment) {
        String name = experiment.getStringProperty(PROPERTY_NAME);
        String permId = experiment.getPermId().getPermId();
        return ((name != null) ? (name + " ") : "") + "(" + permId + ")";
    }

    public static @NonNull String getDisplayName(@NonNull Sample sample) {
        String name = sample.getStringProperty(PROPERTY_NAME);
        String permId = sample.getPermId().getPermId();
        return ((name != null) ? (name + " ") : "") + "(" + permId + ")";
    }

    public static @NonNull String getDisplayName(@NonNull DataSet dataSet) {
        String name = dataSet.getStringProperty(PROPERTY_NAME);
        String permId = dataSet.getPermId().getPermId();
        return ((name != null) ? (name + " ") : "") + "(" + permId + ")";
    }

    public static @NonNull String getSpaceCodeFromDisplayName(@NonNull String displayName) {
        return displayName.trim();
    }

    public static @NonNull String getProjectCodeFromDisplayName(@NonNull String displayName) {
        return displayName.trim();
    }

    public static String getEntityPermIdFromDisplayName(@NonNull String displayName) {
        String[] splitByOpeningParenthesis = displayName.split("\\(");
        if (splitByOpeningParenthesis.length > 1) {
            String[] splitByClosingParenthesis = splitByOpeningParenthesis[splitByOpeningParenthesis.length - 1].split("\\)");
            if (splitByClosingParenthesis.length > 0) {
                return splitByClosingParenthesis[0].trim();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public static String getEntityNameFromDisplayName(@NonNull String displayName) {
        int lastOpenedParenthesis = displayName.lastIndexOf("(");
        return Optional.of(
            displayName.substring(
                    0,
                    lastOpenedParenthesis > -1 ?
                            lastOpenedParenthesis : displayName.length()
            )
        ).map(String::trim).filter(str -> !str.isEmpty()).orElse(null);
    }

    public boolean checkExistence(@NonNull FtpPathLister.EntityDescriptor entityDescriptor) {
        return switch (entityDescriptor.type()) {
            case SPACE ->
                    !openBISClientUtil.getOpenBISClient(user).getSpaces(
                            Collections.singletonList(
                                    new SpacePermId(entityDescriptor.identifier().orElseThrow())
                            ),
                            new SpaceFetchOptions()
                    ).isEmpty();
            case PROJECT ->
                    !openBISClientUtil.getOpenBISClient(user).getProjects(
                            Collections.singletonList(
                                    new ProjectIdentifier(
                                            entityDescriptor.identifier().orElseThrow()
                                    )
                            ),
                            new ProjectFetchOptions()
                    ).isEmpty();
            case EXPERIMENT -> entityDescriptor.identifier().isPresent() &&
                    !openBISClientUtil.getOpenBISClient(user).getExperiments(
                            Collections.singletonList(
                                    new ExperimentPermId(entityDescriptor.identifier().orElseThrow())
                            ),
                            new ExperimentFetchOptions()
                    ).isEmpty();
            case SAMPLE, FOLDER -> entityDescriptor.identifier().isPresent() &&
                    !openBISClientUtil.getOpenBISClient(user).getSamples(
                            Collections.singletonList(
                                    new SamplePermId(entityDescriptor.identifier().orElseThrow())
                            ),
                            new SampleFetchOptions()
                    ).isEmpty();
            case DATA_SET -> entityDescriptor.identifier().isPresent() &&
                    !openBISClientUtil.getOpenBISClient(user).getDataSets(
                            Collections.singletonList(
                                    new DataSetPermId(entityDescriptor.identifier().orElseThrow())
                            ),
                            new DataSetFetchOptions()
                    ).isEmpty();
            default -> false;
        };
    }

    // Entity manipulation section ////

    public void createSpace(@NonNull String spaceCode) {
        spaceCode = spaceCode.trim().replace(" ", "_").toUpperCase();
        if (!isLegalOpenBISCode(spaceCode)) {
            throw new IllegalArgumentException("Illegal code");
        }
        SpaceCreation spaceCreation = new SpaceCreation();
        spaceCreation.setCode(spaceCode);
        openBISClientUtil.getOpenBISClient(user).createSpaces(
                Collections.singletonList(
                        spaceCreation
                )
        );
    }

    public void deleteSpace(@NonNull String spaceCode) {
        SpaceDeletionOptions spaceDeletionOptions = new SpaceDeletionOptions();
        spaceDeletionOptions.setReason("Deleted through SFTP");
        openBISClientUtil.getOpenBISClient(user).deleteSpaces(
                Collections.singletonList(
                        new SpacePermId(spaceCode)
                ),
                spaceDeletionOptions
        );
    }

    public void createProject(@NonNull String spaceCode, @NonNull String projectCode) {
        spaceCode = spaceCode.trim().replace(" ", "_").toUpperCase();
        projectCode = projectCode.trim().replace(" ", "_").toUpperCase();
        if (!isLegalOpenBISCode(spaceCode) || !isLegalOpenBISCode(projectCode)) {
            throw new IllegalArgumentException("Illegal code");
        }
        ProjectCreation projectCreation = new ProjectCreation();
        projectCreation.setCode(projectCode);
        projectCreation.setSpaceId(new SpacePermId(spaceCode));
        openBISClientUtil.getOpenBISClient(user).createProjects(
                Collections.singletonList(
                        projectCreation
                )
        );
    }

    public void deleteProject(@NonNull String projectId) {
        ProjectDeletionOptions projectDeletionOptions = new ProjectDeletionOptions();
        projectDeletionOptions.setReason("Deleted through SFTP");
        openBISClientUtil.getOpenBISClient(user).deleteProjects(
                Collections.singletonList(
                        new ProjectIdentifier(projectId)
                ),
                projectDeletionOptions
        );
    }

    public void createExperiment(
            @NonNull String spaceCode,
            @NonNull String projectCode,
            @NonNull String experimentName) {
        ExperimentCreation experimentCreation = new ExperimentCreation();
        experimentCreation.setProjectId(new ProjectIdentifier(spaceCode, projectCode));
        experimentCreation.setCode(experimentName.replace(" ", "_").replaceAll("[^a-zA-Z0-9_\\-.]", "").toUpperCase());
        experimentCreation.setProperty(SftpListUtil.PROPERTY_NAME, experimentName);
        experimentCreation.setTypeId(new EntityTypePermId(EXPERIMENT_TYPE_COLLECTION));
        openBISClientUtil.getOpenBISClient(user).createExperiments(
                Collections.singletonList(
                        experimentCreation
                )
        );
    }

    public void deleteExperiment(@NonNull String experimentPermId) {
        ExperimentDeletionOptions experimentDeletionOptions = new ExperimentDeletionOptions();
        experimentDeletionOptions.setReason("Deleted through SFTP");
        openBISClientUtil.getOpenBISClient(user).deleteExperiments(
                Collections.singletonList(
                        new ExperimentPermId(experimentPermId)
                ),
                experimentDeletionOptions
        );
    }

    public void renameExperiment(@NonNull String experimentPermId, @NonNull String newName) {
        ExperimentUpdate experimentUpdate = new ExperimentUpdate();
        experimentUpdate.setExperimentId(new ExperimentPermId(experimentPermId));
        experimentUpdate.setProperty(PROPERTY_NAME, newName);
        openBISClientUtil.getOpenBISClient(user).updateExperiments(
                Collections.singletonList(
                        experimentUpdate
                )
        );
    }

    public void createSample(
            @NonNull String spaceCode,
            String projectCode,
            String experimentId,
            String parentSampleId,
            @NonNull String sampleName,
            boolean folderType) {
        SampleCreation sampleCreation = new SampleCreation();
        sampleCreation.setSpaceId(new SpacePermId(spaceCode));
        if (projectCode != null) {
            sampleCreation.setProjectId(new ProjectIdentifier(spaceCode, projectCode));
        }
        if (experimentId != null) {
            sampleCreation.setExperimentId(new ExperimentPermId(experimentId));
        }
        if (parentSampleId != null) {
            sampleCreation.setParentIds(Collections.singletonList(new SamplePermId(parentSampleId)));
        }
        sampleCreation.setCode(sampleName.replace(" ", "_").replaceAll("[^a-zA-Z0-9_\\-.]", "").toUpperCase());
        sampleCreation.setProperty(SftpListUtil.PROPERTY_NAME, sampleName);

        if (folderType) {
            sampleCreation.setTypeId(new EntityTypePermId(FOLDER_SAMPLE_TYPE));
        } else {
            sampleCreation.setTypeId(new EntityTypePermId(ENTRY_SAMPLE_TYPE));
        }
        openBISClientUtil.getOpenBISClient(user).createSamples(
                Collections.singletonList(
                        sampleCreation
                )
        );
    }

    public void deleteSample(@NonNull String samplePermId) {
        SampleDeletionOptions sampleDeletionOptions = new SampleDeletionOptions();
        sampleDeletionOptions.setReason("Deleted through SFTP");
        openBISClientUtil.getOpenBISClient(user).deleteSamples(
                Collections.singletonList(
                        new SamplePermId(samplePermId)
                ),
                sampleDeletionOptions
        );
    }

    public void renameSample(@NonNull String samplePermId, @NonNull String newName) {
        SampleUpdate sampleUpdate = new SampleUpdate();
        sampleUpdate.setSampleId(new SamplePermId(samplePermId));
        sampleUpdate.setProperty(PROPERTY_NAME, newName);
        openBISClientUtil.getOpenBISClient(user).updateSamples(
                Collections.singletonList(
                        sampleUpdate
                )
        );
    }

    public void deleteDataSet(@NonNull String dataSetPermId) {
        DataSetDeletionOptions dataSetDeletionOptions = new DataSetDeletionOptions();
        dataSetDeletionOptions.setReason("Deleted through SFTP");
        openBISClientUtil.getOpenBISClient(user).deleteDataSets(
                Collections.singletonList(
                        new DataSetPermId(dataSetPermId)
                ),
                dataSetDeletionOptions
        );
    }

    public static final Pattern ENTITY_CODE_LEGAL_PATTERN =  Pattern.compile("^[a-zA-Z0-9_\\-.]+$");
    public static boolean isLegalOpenBISCode(@NonNull String code) {
        return ENTITY_CODE_LEGAL_PATTERN.asMatchPredicate().test(code);
    }
}
