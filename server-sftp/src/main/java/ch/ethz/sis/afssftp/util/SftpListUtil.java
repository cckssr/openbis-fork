package ch.ethz.sis.afssftp.util;

import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afsclient.client.AfsClientUploadHelper;
import ch.ethz.sis.afssftp.authentication.User;
import ch.ethz.sis.afssftp.filesystemview.SftpFileAttributes;
import ch.ethz.sis.afssftp.filesystemview.SftpNode;
import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.fetchoptions.ProjectFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.ProjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SamplePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
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

import static ch.ethz.sis.afsclient.client.AfsClientUploadHelper.isPathNotInStoreError;

public class SftpListUtil {
    public static final String FOLDER_SAMPLE_TYPE = "FOLDER";
    public static final Set<SftpNode.Type> POSSIBLE_AFS_ENTITY_TYPES = Set.of(
            SftpNode.Type.FOLDER,
            SftpNode.Type.SAMPLE,
            SftpNode.Type.EXPERIMENT,
            SftpNode.Type.DATA_SET
    );

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
        SamplePermId sampleId = new SamplePermId(samplePermId);

        return Optional.ofNullable(openBIS.getSamples(List.of(sampleId), fetchOptions).get(sampleId))
                .map(Sample::getDataSets).orElse(Collections.emptyList());
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

    public static @NonNull SftpFileAttributes getDefaultAbstractDirectoryAttributes() {
        return SftpFileAttributes.builder()
                .creationTime(FileTime.from(Instant.now()))
                .modifiedTime(FileTime.from(Instant.now()))
                .accessTime(FileTime.from(Instant.now()))
                .directory(true)
                .regularFile(false)
                .size(0)
                .build();
    }

    public static @NonNull String getDisplayName(@NonNull Space space) {
        return space.getCode();
    }

    public static @NonNull String getDisplayName(@NonNull Project project) {
        return project.getCode();
    }

    public static @NonNull String getDisplayName(@NonNull Experiment experiment) {
        String name = experiment.getStringProperty("NAME");
        String permId = experiment.getPermId().getPermId();
        return ((name != null) ? name : "") + "(" + permId + ")";
    }

    public static @NonNull String getDisplayName(@NonNull Sample sample) {
        String name = sample.getStringProperty("NAME");
        String permId = sample.getPermId().getPermId();
        return ((name != null) ? name : "") + "(" + permId + ")";
    }

    public static @NonNull String getDisplayName(@NonNull DataSet dataSet) {
        String name = dataSet.getStringProperty("NAME");
        String permId = dataSet.getPermId().getPermId();
        return ((name != null) ? name : "") + "(" + permId + ")";
    }

    public static @NonNull String getSpaceCodeFromDisplayName(@NonNull String displayName) {
        return displayName;
    }

    public static @NonNull String getProjectCodeFromDisplayName(@NonNull String displayName) {
        return displayName;
    }

    public static @NonNull String getEntityPermIdFromDisplayName(@NonNull String displayName) {
        return Arrays.asList(
                Arrays.asList(
                        displayName.split("\\(")
                ).getLast().split("\\)")
        ).getFirst();
    }
}
