package ch.ethz.sis.afssftp.util;

import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afsclient.client.AfsClientUploadHelper;
import ch.ethz.sis.afssftp.authentication.OpenBISUser;
import ch.ethz.sis.afssftp.filesystemview.OpenBISSftpFileAttributes;
import ch.ethz.sis.afssftp.filesystemview.OpenBISSftpNode;
import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.id.ObjectPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.fetchoptions.ProjectFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.ProjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.fetchoptions.SpaceFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.search.SpaceSearchCriteria;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static ch.ethz.sis.afsclient.client.AfsClientUploadHelper.isPathNotInStoreError;

public class OpenBISListUtil {
    public static final String FOLDER_SAMPLE_TYPE = "FOLDER";
    public static final Set<OpenBISSftpNode.Type> POSSIBLE_AFS_ENTITY_TYPES = Set.of(
            OpenBISSftpNode.Type.FOLDER,
            OpenBISSftpNode.Type.SAMPLE,
            OpenBISSftpNode.Type.EXPERIMENT,
            OpenBISSftpNode.Type.DATA_SET
    );

    private final OpenBISUser user;
    private final OpenBISClientUtil openBISClientUtil;

    public OpenBISListUtil(@NonNull OpenBISUser user) {
        this.user = user;
        this.openBISClientUtil = new OpenBISClientUtil();
    }

    //For unit-tests
    OpenBISListUtil(
            OpenBISUser user,
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
     * @return samples directly attached to space-entity
     */
    public @NonNull List<Sample> getSpaceSamples(@NonNull String spacePermId) {
        OpenBIS openBIS = openBISClientUtil.getOpenBISClient(user);

        SpaceFetchOptions fetchOptions = new SpaceFetchOptions();
        fetchOptions.withSamples().withType();
        fetchOptions.withSamples().withProject();
        fetchOptions.withSamples().withExperiment();
        fetchOptions.withSamples().withParents();
        SpacePermId spaceId = new SpacePermId(spacePermId);

        return Optional.ofNullable(
                    openBIS.getSpaces(List.of(spaceId), fetchOptions).get(spaceId)
                )
                .map(Space::getSamples)
                .map(samples -> samples.stream()
                        .filter(sample -> sample.getProject() == null)
                        .filter(sample -> sample.getExperiment() == null)
                        .filter(sample -> sample.getParents().isEmpty())
                        .toList()
                ).orElse(Collections.emptyList());
    }

    /***
     * @param spaceCode Space-entity code
     *
     * @param projectCode Project-entity code
     * @return samples directly attached to project-entity
     */
    public @NonNull List<Sample> getProjectSamples(@NonNull String spaceCode, @NonNull String projectCode) {
        OpenBIS openBIS = openBISClientUtil.getOpenBISClient(user);

        ProjectFetchOptions fetchOptions = new ProjectFetchOptions();
        fetchOptions.withSamples().withType();
        fetchOptions.withSamples().withExperiment();
        fetchOptions.withSamples().withParents();
        ProjectIdentifier projectId = new ProjectIdentifier(spaceCode, projectCode);

        return Optional.ofNullable(openBIS.getProjects(List.of(projectId), fetchOptions).get(projectId))
                .map(Project::getSamples)
                .map(samples -> samples.stream()
                        .filter(sample -> sample.getExperiment() == null)
                        .filter(sample -> sample.getParents().isEmpty())
                        .toList()
                ).orElse(Collections.emptyList());
    }

    /***
     * @param spaceCode Space-entity code
     *
     * @param projectCode Project-entity code
     * @param experimentCode Experiment-entity code
     * @return samples directly attached to experiment-entity
     */
    public @NonNull List<Sample> getExperimentSamples(
            @NonNull String spaceCode, @NonNull String projectCode, @NonNull String experimentCode) {
        OpenBIS openBIS = openBISClientUtil.getOpenBISClient(user);

        ExperimentFetchOptions fetchOptions = new ExperimentFetchOptions();
        fetchOptions.withSamples().withType();
        fetchOptions.withSamples().withParents();
        ExperimentIdentifier experimentId = new ExperimentIdentifier(spaceCode, projectCode, experimentCode);

        return Optional.ofNullable(openBIS.getExperiments(List.of(experimentId), fetchOptions).get(experimentId))
                .map(Experiment::getSamples)
                .map(samples -> samples.stream()
                        .filter(sample -> sample.getParents().isEmpty())
                        .toList()
                ).orElse(Collections.emptyList());
    }

    /***
     * @param spaceCode Space-entity code
     *
     * @param projectCode Project-entity code
     * @param sampleCode Sample-entity code
     * @return samples directly attached to sample-entity
     */
    public @NonNull List<Sample> getSampleChildren(String spaceCode, String projectCode, @NonNull String sampleCode) {
        OpenBIS openBIS = openBISClientUtil.getOpenBISClient(user);

        SampleFetchOptions fetchOptions = new SampleFetchOptions();
        fetchOptions.withChildren().withType();
        SampleIdentifier sampleId = new SampleIdentifier(spaceCode, projectCode, null, sampleCode);

        return Optional.ofNullable(openBIS.getSamples(List.of(sampleId), fetchOptions).get(sampleId))
                .map(Sample::getChildren).orElse(Collections.emptyList());
    }

    /***
     * @param spaceCode Space-entity code
     *
     * @param projectCode Project-entity code
     * @param sampleCode Sample-entity code
     * @return datasets directly attached to sample-entity
     */
    public @NonNull List<DataSet> getSampleDatasets(String spaceCode, String projectCode, @NonNull String sampleCode) {
        OpenBIS openBIS = openBISClientUtil.getOpenBISClient(user);

        SampleFetchOptions fetchOptions = new SampleFetchOptions();
        fetchOptions.withDataSets();
        SampleIdentifier sampleId = new SampleIdentifier(spaceCode, projectCode, null, sampleCode);

        return Optional.ofNullable(openBIS.getSamples(List.of(sampleId), fetchOptions).get(sampleId))
                .map(Sample::getDataSets).orElse(Collections.emptyList());
    }

    public String getAfsEntityPermId(@NonNull OpenBISSftpNode afsEntityNode, String spaceCode, String projectCode) {
        return switch (afsEntityNode.getType()) {
            case SAMPLE, FOLDER -> {
                OpenBIS openBIS = openBISClientUtil.getOpenBISClient(user);

                SampleIdentifier sampleId = new SampleIdentifier(
                        spaceCode, projectCode, null, afsEntityNode.getIdentifier().orElseThrow()
                );
                yield  Optional.ofNullable(
                        openBIS.getSamples(List.of(sampleId), new SampleFetchOptions()).get(sampleId)
                    ).map(Sample::getPermId).map(ObjectPermId::getPermId)
                    .orElse(null);
            }
            case DATA_SET -> {
                OpenBIS openBIS = openBISClientUtil.getOpenBISClient(user);

                DataSetPermId dataSetPermId = new DataSetPermId(
                        afsEntityNode.getIdentifier().orElseThrow()
                );

                yield  Optional.ofNullable(
                        openBIS.getDataSets(List.of(dataSetPermId), new DataSetFetchOptions()).get(dataSetPermId)
                    ).map(DataSet::getPermId).map(ObjectPermId::getPermId)
                    .orElse(null);
            }
            case EXPERIMENT -> {
                OpenBIS openBIS = openBISClientUtil.getOpenBISClient(user);

                ExperimentIdentifier experimentId = new ExperimentIdentifier(
                        spaceCode, projectCode,
                        afsEntityNode.getIdentifier().orElseThrow()
                );

                yield  Optional.ofNullable(
                        openBIS.getExperiments(List.of(experimentId), new ExperimentFetchOptions()).get(experimentId)
                    ).map(Experiment::getPermId).map(ObjectPermId::getPermId)
                    .orElse(null);
            }
            default -> null;
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
                openBISClientUtil.getAfsClient(user).getInnerClient(),
                afsEntityId,
                absoluteAfsFilePath
        );
    }

    @SneakyThrows
    public Optional<OpenBISSftpFileAttributes> getDefaultAfsFileAttributes(@NonNull String afsEntityId, @NonNull String absoluteAfsFilePath) {
        return getAfsFilePresence(afsEntityId, absoluteAfsFilePath).map(
                file -> {
                    FileTime lastModified = file.getLastModifiedTime() != null ?
                            FileTime.from(file.getLastModifiedTime().toInstant()) :
                            FileTime.from(Instant.now());

                    return OpenBISSftpFileAttributes.builder()
                            .creationTime(lastModified)
                            .modifiedTime(lastModified)
                            .accessTime(lastModified)
                            .directory(Boolean.TRUE.equals(file.getDirectory()))
                            .regularFile(!Boolean.TRUE.equals(file.getDirectory()))
                            .size(file.getSize() != null ? file.getSize() : 0)
                            .build();
                }
        );
    }

    public static @NonNull OpenBISSftpFileAttributes getDefaultAbstractDirectoryAttributes() {
        return OpenBISSftpFileAttributes.builder()
                .creationTime(FileTime.from(Instant.now()))
                .modifiedTime(FileTime.from(Instant.now()))
                .accessTime(FileTime.from(Instant.now()))
                .directory(true)
                .regularFile(false)
                .size(0)
                .build();
    }
}
