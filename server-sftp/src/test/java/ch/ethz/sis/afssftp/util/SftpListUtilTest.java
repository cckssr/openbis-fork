package ch.ethz.sis.afssftp.util;

import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afssftp.StaticInitializer;
import ch.ethz.sis.afssftp.authentication.User;
import ch.ethz.sis.afssftp.filesystemview.FtpPathLister;
import ch.ethz.sis.afssftp.filesystemview.SftpFileAttributes;
import ch.ethz.sis.afssftp.filesystemview.SftpNode;
import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.create.ExperimentCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.update.ExperimentUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.create.ProjectCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.fetchoptions.ProjectFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.ProjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.create.SampleCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SamplePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.update.SampleUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.create.SpaceCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.fetchoptions.SpaceFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.ISpaceId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.search.SpaceSearchCriteria;
import junit.framework.TestCase;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

import static ch.ethz.sis.afssftp.helpers.TestHelper.createRandomNodeOfType;

public class SftpListUtilTest extends TestCase {

    static {
        StaticInitializer.initialize();
    }

    public void testGetSpaces() {
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        OpenBIS openBIS = Mockito.mock(OpenBIS.class);
        User user = User.builder()
                .username("u5er")
                .sessionToken("53551on")
                .build();
        SftpListUtil sftpListUtil = new SftpListUtil(user, openBISClientUtil);
        Mockito.doReturn(openBIS).when(openBISClientUtil).getOpenBISClient(user);
        Mockito.doReturn(new SearchResult<>(Collections.emptyList(), 0)).when(openBIS).searchSpaces(
                Mockito.any(), Mockito.any()
        );
        sftpListUtil.getSpaces();
        ArgumentCaptor<SpaceSearchCriteria> spaceSearchCriteriaArgumentCaptor = ArgumentCaptor.forClass(SpaceSearchCriteria.class);
        ArgumentCaptor<SpaceFetchOptions> spaceFetchOptionsArgumentCaptor = ArgumentCaptor.forClass(SpaceFetchOptions.class);
        Mockito.verify(openBIS, Mockito.times(1)).searchSpaces(
                spaceSearchCriteriaArgumentCaptor.capture(), spaceFetchOptionsArgumentCaptor.capture()
        );
        assertTrue(spaceSearchCriteriaArgumentCaptor.getValue().getCriteria().isEmpty());
    }

    public void testGetProjects() {
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        OpenBIS openBIS = Mockito.mock(OpenBIS.class);
        User user = User.builder()
                .username("u5er")
                .sessionToken("53551on")
                .build();
        SftpListUtil sftpListUtil = new SftpListUtil(user, openBISClientUtil);
        Mockito.doReturn(openBIS).when(openBISClientUtil).getOpenBISClient(user);
        Mockito.doReturn(Collections.emptyMap()).when(openBIS).getSpaces(
                Mockito.any(), Mockito.any()
        );
        sftpListUtil.getProjects("space-perm-id");
        ArgumentCaptor<List<? extends ISpaceId>> spaceIdListArgumentCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<SpaceFetchOptions> spaceFetchOptionsArgumentCaptor = ArgumentCaptor.forClass(SpaceFetchOptions.class);
        Mockito.verify(openBIS, Mockito.times(1)).getSpaces(
                spaceIdListArgumentCaptor.capture(), spaceFetchOptionsArgumentCaptor.capture()
        );
        assertEquals(new SamplePermId("space-perm-id").getPermId(), ((SpacePermId) (spaceIdListArgumentCaptor.getValue().get(0))).getPermId());
        assertTrue(spaceFetchOptionsArgumentCaptor.getValue().hasProjects());
    }

    public void testGetExperiments() {
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        OpenBIS openBIS = Mockito.mock(OpenBIS.class);
        User user = User.builder()
                .username("u5er")
                .sessionToken("53551on")
                .build();
        SftpListUtil sftpListUtil = new SftpListUtil(user, openBISClientUtil);
        Mockito.doReturn(openBIS).when(openBISClientUtil).getOpenBISClient(user);
        Mockito.doReturn(Collections.emptyMap()).when(openBIS).getProjects(
                Mockito.any(), Mockito.any()
        );
        sftpListUtil.getExperiments("space-code", "project-code");
        ArgumentCaptor<List<? extends ProjectIdentifier>> projectIdListArgumentCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<ProjectFetchOptions> projectFetchOptionsArgumentCaptor = ArgumentCaptor.forClass(ProjectFetchOptions.class);
        Mockito.verify(openBIS, Mockito.times(1)).getProjects(
                projectIdListArgumentCaptor.capture(), projectFetchOptionsArgumentCaptor.capture()
        );
        assertEquals(new ProjectIdentifier("space-code", "project-code").getIdentifier(), projectIdListArgumentCaptor.getValue().get(0).getIdentifier());
        assertTrue(projectFetchOptionsArgumentCaptor.getValue().hasExperiments());
    }

    public void testIsOfTypeFolder() {
        SampleFetchOptions sampleFetchOptions = new SampleFetchOptions();
        sampleFetchOptions.withType();

        Sample sample = new Sample();
        SampleType nonFolderType = new SampleType();
        nonFolderType.setCode("NONFOLDER");
        sample.setType(nonFolderType);
        sample.setFetchOptions(sampleFetchOptions);

        Sample folder = new Sample();
        SampleType folderType = new SampleType();
        folderType.setCode("FOLDER");
        folder.setType(folderType);
        folder.setFetchOptions(sampleFetchOptions);

        assertFalse(SftpListUtil.isOfTypeFolder(sample));
        assertTrue(SftpListUtil.isOfTypeFolder(folder));
    }

    public void testGetSpaceSamples() {
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        OpenBIS openBIS = Mockito.mock(OpenBIS.class);
        User user = User.builder()
                .username("u5er")
                .sessionToken("53551on")
                .build();
        SftpListUtil sftpListUtil = new SftpListUtil(user, openBISClientUtil);
        Mockito.doReturn(openBIS).when(openBISClientUtil).getOpenBISClient(user);
        Mockito.doReturn(new SearchResult<Sample>(Collections.emptyList(), 0)).when(openBIS).searchSamples(
                Mockito.any(), Mockito.any()
        );
        sftpListUtil.getSpaceSamples("space-perm-id");
        ArgumentCaptor<SampleSearchCriteria> sampleSearchCriteriaArgumentCaptor = ArgumentCaptor.forClass(SampleSearchCriteria.class);
        ArgumentCaptor<SampleFetchOptions> sampleFetchOptionsArgumentCaptor = ArgumentCaptor.forClass(SampleFetchOptions.class);
        Mockito.verify(openBIS, Mockito.times(1)).searchSamples(
                sampleSearchCriteriaArgumentCaptor.capture(), sampleFetchOptionsArgumentCaptor.capture()
        );
        assertTrue(sampleSearchCriteriaArgumentCaptor.getValue().getCriteria().toString().contains(
                "[SPACE\n    with attribute 'code' equal to 'space-perm-id'\n, without project]"
        ));
        assertTrue(sampleFetchOptionsArgumentCaptor.getValue().hasProperties());
        assertTrue(sampleFetchOptionsArgumentCaptor.getValue().hasType());
    }

    public void testGetProjectSamples() {
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        OpenBIS openBIS = Mockito.mock(OpenBIS.class);
        User user = User.builder()
                .username("u5er")
                .sessionToken("53551on")
                .build();
        SftpListUtil sftpListUtil = new SftpListUtil(user, openBISClientUtil);
        Mockito.doReturn(openBIS).when(openBISClientUtil).getOpenBISClient(user);
        Mockito.doReturn(new SearchResult<Sample>(Collections.emptyList(), 0)).when(openBIS).searchSamples(
                Mockito.any(), Mockito.any()
        );
        sftpListUtil.getProjectSamples("space-code", "project-code");
        ArgumentCaptor<SampleSearchCriteria> sampleSearchCriteriaArgumentCaptor = ArgumentCaptor.forClass(SampleSearchCriteria.class);
        ArgumentCaptor<SampleFetchOptions> sampleFetchOptionsArgumentCaptor = ArgumentCaptor.forClass(SampleFetchOptions.class);
        Mockito.verify(openBIS, Mockito.times(1)).searchSamples(
                sampleSearchCriteriaArgumentCaptor.capture(), sampleFetchOptionsArgumentCaptor.capture()
        );
        assertTrue(sampleSearchCriteriaArgumentCaptor.getValue().getCriteria().toString().contains(
                "[PROJECT\n    with id '/SPACE-CODE/PROJECT-CODE'\n, without experiment]"
        ));
        assertTrue(sampleFetchOptionsArgumentCaptor.getValue().hasProperties());
        assertTrue(sampleFetchOptionsArgumentCaptor.getValue().hasType());
    }

    public void testGetExperimentSamples() {
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        OpenBIS openBIS = Mockito.mock(OpenBIS.class);
        User user = User.builder()
                .username("u5er")
                .sessionToken("53551on")
                .build();
        SftpListUtil sftpListUtil = new SftpListUtil(user, openBISClientUtil);
        Mockito.doReturn(openBIS).when(openBISClientUtil).getOpenBISClient(user);
        Mockito.doReturn(Collections.emptyMap()).when(openBIS).getProjects(
                Mockito.any(), Mockito.any()
        );
        sftpListUtil.getExperimentSamples("experiment-perm-id");
        ArgumentCaptor<List<? extends ExperimentPermId>> experimentIdListArgumentCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<ExperimentFetchOptions> experimentFetchOptionsArgumentCaptor = ArgumentCaptor.forClass(ExperimentFetchOptions.class);
        Mockito.verify(openBIS, Mockito.times(1)).getExperiments(
                experimentIdListArgumentCaptor.capture(), experimentFetchOptionsArgumentCaptor.capture()
        );
        assertEquals(new ExperimentPermId("experiment-perm-id").getPermId(), experimentIdListArgumentCaptor.getValue().get(0).getPermId());
        assertTrue(experimentFetchOptionsArgumentCaptor.getValue().hasSamples());
        assertTrue(experimentFetchOptionsArgumentCaptor.getValue().withSamples().hasProperties());
        assertTrue(experimentFetchOptionsArgumentCaptor.getValue().withSamples().hasType());
    }

    public void testGetSampleChildren() {
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        OpenBIS openBIS = Mockito.mock(OpenBIS.class);
        User user = User.builder()
                .username("u5er")
                .sessionToken("53551on")
                .build();
        SftpListUtil sftpListUtil = new SftpListUtil(user, openBISClientUtil);
        Mockito.doReturn(openBIS).when(openBISClientUtil).getOpenBISClient(user);
        Mockito.doReturn(Collections.emptyMap()).when(openBIS).getSamples(
                Mockito.any(), Mockito.any()
        );
        sftpListUtil.getSampleChildren("sample-perm-id");
        ArgumentCaptor<List<? extends SamplePermId>> sampleIdListArgumentCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<SampleFetchOptions> sampleFetchOptionsArgumentCaptor = ArgumentCaptor.forClass(SampleFetchOptions.class);
        Mockito.verify(openBIS, Mockito.times(1)).getSamples(
                sampleIdListArgumentCaptor.capture(), sampleFetchOptionsArgumentCaptor.capture()
        );
        assertEquals(new SamplePermId("sample-perm-id").getPermId(), sampleIdListArgumentCaptor.getValue().get(0).getPermId());
        assertTrue(sampleFetchOptionsArgumentCaptor.getValue().hasChildren());
        assertTrue(sampleFetchOptionsArgumentCaptor.getValue().withChildren().hasProperties());
        assertTrue(sampleFetchOptionsArgumentCaptor.getValue().withChildren().hasType());
    }

    public void testGetSampleDatasets() {
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        OpenBIS openBIS = Mockito.mock(OpenBIS.class);
        User user = User.builder()
                .username("u5er")
                .sessionToken("53551on")
                .build();
        SftpListUtil sftpListUtil = new SftpListUtil(user, openBISClientUtil);
        Mockito.doReturn(openBIS).when(openBISClientUtil).getOpenBISClient(user);
        Mockito.doReturn(Collections.emptyMap()).when(openBIS).getSamples(
                Mockito.any(), Mockito.any()
        );
        sftpListUtil.getSampleDatasets("sample-perm-id");
        ArgumentCaptor<List<? extends SamplePermId>> sampleIdListArgumentCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<SampleFetchOptions> sampleFetchOptionsArgumentCaptor = ArgumentCaptor.forClass(SampleFetchOptions.class);
        Mockito.verify(openBIS, Mockito.times(1)).getSamples(
                sampleIdListArgumentCaptor.capture(), sampleFetchOptionsArgumentCaptor.capture()
        );
        assertEquals(new SamplePermId("sample-perm-id").getPermId(), sampleIdListArgumentCaptor.getValue().get(0).getPermId());
        assertTrue(sampleFetchOptionsArgumentCaptor.getValue().hasDataSets());
        assertTrue(sampleFetchOptionsArgumentCaptor.getValue().withDataSets().hasProperties());
    }

    public void testGetExperimentDatasets() {
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        OpenBIS openBIS = Mockito.mock(OpenBIS.class);
        User user = User.builder()
                .username("u5er")
                .sessionToken("53551on")
                .build();
        SftpListUtil sftpListUtil = new SftpListUtil(user, openBISClientUtil);
        Mockito.doReturn(openBIS).when(openBISClientUtil).getOpenBISClient(user);
        Mockito.doReturn(Collections.emptyMap()).when(openBIS).getSamples(
                Mockito.any(), Mockito.any()
        );
        sftpListUtil.getExperimentDatasets("exp-perm-id");
        ArgumentCaptor<List<? extends ExperimentPermId>> experimentIdListArgumentCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<ExperimentFetchOptions> fetchOptionsArgumentCaptor = ArgumentCaptor.forClass(ExperimentFetchOptions.class);
        Mockito.verify(openBIS, Mockito.times(1)).getExperiments(
                experimentIdListArgumentCaptor.capture(), fetchOptionsArgumentCaptor.capture()
        );
        assertEquals(new ExperimentPermId("exp-perm-id").getPermId(), experimentIdListArgumentCaptor.getValue().get(0).getPermId());
        assertTrue(fetchOptionsArgumentCaptor.getValue().hasDataSets());
        assertTrue(fetchOptionsArgumentCaptor.getValue().withDataSets().hasProperties());
    }

    public void testGetAfsEntityPermId() {
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        OpenBIS openBIS = Mockito.mock(OpenBIS.class);
        User user = User.builder()
                .username("u5er")
                .sessionToken("53551on")
                .build();
        SftpListUtil sftpListUtil = new SftpListUtil(user, openBISClientUtil);

        Mockito.doReturn(openBIS).when(openBISClientUtil).getOpenBISClient(user);

        //SAMPLE
        SftpNode sampleNode = createRandomNodeOfType(SftpNode.Type.SAMPLE);
        assertEquals(SftpListUtil.getEntityPermIdFromDisplayName(sampleNode.getIdentifier().get()), sftpListUtil.getAfsEntityPermId(sampleNode));

        //FOLDER
        SftpNode folderNode = createRandomNodeOfType(SftpNode.Type.FOLDER);
        assertEquals(SftpListUtil.getEntityPermIdFromDisplayName(folderNode.getIdentifier().get()), sftpListUtil.getAfsEntityPermId(folderNode));

        //DATASET
        SftpNode datasetNode = createRandomNodeOfType(SftpNode.Type.DATA_SET);
        assertEquals(SftpListUtil.getEntityPermIdFromDisplayName(datasetNode.getIdentifier().get()), sftpListUtil.getAfsEntityPermId(datasetNode));

        //EXPERIMENT
        SftpNode experimentNode = createRandomNodeOfType(SftpNode.Type.EXPERIMENT);
        assertEquals(SftpListUtil.getEntityPermIdFromDisplayName(experimentNode.getIdentifier().get()), sftpListUtil.getAfsEntityPermId(experimentNode));
    }

    public void testListAfsFiles() throws Exception {
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        User user = User.builder()
                .username("u5er")
                .sessionToken("53551on")
                .build();
        OpenBIS.AfsServerFacade afsClientProxyMock = Mockito.mock(
                OpenBIS.AfsServerFacade.class
        );
        Mockito.doReturn(afsClientProxyMock).when(openBISClientUtil).getAfsClient(user);
        SftpListUtil sftpListUtil = new SftpListUtil(user, openBISClientUtil);
        Mockito.doReturn(new File[0]).when(afsClientProxyMock).list("entity-id", "/dir/file", false);
        sftpListUtil.listAfsFiles("entity-id", "/dir/file");
        Mockito.verify(openBISClientUtil, Mockito.times(1)).getAfsClient(user);
        Mockito.verify(afsClientProxyMock, Mockito.times(1)).list("entity-id", "/dir/file", false);
    }

    public void testGetAfsFilePresence() {
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        User user = User.builder()
                .username("u5er")
                .sessionToken("53551on")
                .build();
        OpenBIS.AfsServerFacade afsClientFacadeMock = Mockito.mock(
                OpenBIS.AfsServerFacade.class
        );
        Mockito.doReturn(afsClientFacadeMock).when(openBISClientUtil).getAfsClient(user);
        SftpListUtil sftpListUtil = new SftpListUtil(user, openBISClientUtil);
        File[] listedFiles = {
                new File("entity-id", "/dir/file3", "file3", false, 1004L, Instant.now().atOffset(ZoneOffset.UTC)),
                new File("entity-id", "/dir/subdir", "file3", true, 0L, Instant.now().atOffset(ZoneOffset.UTC)),
                new File("entity-id", "/dir/file", "file", false, 1005L, Instant.now().atOffset(ZoneOffset.UTC)),
                new File("entity-id", "/dir/file2", "file", false, 1006L, Instant.now().atOffset(ZoneOffset.UTC)),
        };

        Mockito.doReturn(listedFiles).when(afsClientFacadeMock).list("entity-id", "/dir", false);

        Optional<File> file = sftpListUtil.getAfsFilePresence("entity-id", "/dir/file");
        assertEquals(Optional.of(listedFiles[2]), file);

        Mockito.verify(afsClientFacadeMock, Mockito.times(1)).list("entity-id", "/dir", false);
        Mockito.verify(openBISClientUtil, Mockito.times(1)).getAfsClient(user);

        Mockito.clearInvocations(afsClientFacadeMock, openBISClientUtil);

        Optional<File> file2 = sftpListUtil.getAfsFilePresence("entity-id", "/dir/file-other");
        assertEquals(Optional.empty(), file2);

        Mockito.verify(afsClientFacadeMock, Mockito.times(1)).list("entity-id", "/dir", false);
        Mockito.verify(openBISClientUtil, Mockito.times(1)).getAfsClient(user);

        Mockito.doThrow(new RuntimeException("NoSuchFileException")).when(afsClientFacadeMock).list("entity-id", "/dir", false);
        Optional<File> file3 = sftpListUtil.getAfsFilePresence("entity-id", "/dir/file");
        assertEquals(Optional.empty(), file3);

        Mockito.doThrow(new RuntimeException()).when(afsClientFacadeMock).list("entity-id", "/dir", false);
        Exception ex = null;
        try {
            sftpListUtil.getAfsFilePresence("entity-id", "/dir/file");
        } catch (Exception e) {
            ex = e;
        }
        assertEquals(RuntimeException.class, ex.getClass());

        Mockito.doReturn(new File[0]).when(afsClientFacadeMock).list("entity-id", "/", false);
        Optional<File> root = sftpListUtil.getAfsFilePresence("entity-id", "/");
        assertEquals("/", root.get().getPath());
    }

    public void testGetDefaultAfsFileAttributes() {
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        User user = User.builder()
                .username("u5er")
                .sessionToken("53551on")
                .build();
        OpenBIS.AfsServerFacade afsClientProxyMock = Mockito.mock(
                OpenBIS.AfsServerFacade.class
        );
        Mockito.doReturn(afsClientProxyMock)
                .when(openBISClientUtil).getAfsClient(user);
        SftpListUtil sftpListUtil = Mockito.spy(new SftpListUtil(user, openBISClientUtil));
        File returnedFile = File.builder()
                .owner("owner")
                .name("file")
                .path("/dir/path")
                .size(5L)
                .directory(false)
                .lastModifiedTime(Instant.now().atOffset(ZoneOffset.UTC))
                .build();
        String entityId = "afs-entity-id";
        String filePath = "/dir/path";
        Mockito.doReturn(Optional.of(returnedFile)).when(sftpListUtil).getAfsFilePresence(entityId, filePath);
        for (boolean mutable : List.of(false, true)) {
            SftpFileAttributes attributes = sftpListUtil.getDefaultAfsFileAttributes(entityId, filePath, mutable).get();
            Mockito.verify(sftpListUtil, Mockito.times(1)).getAfsFilePresence(entityId, filePath);
            assertEquals(returnedFile.getLastModifiedTime().toInstant().toEpochMilli(), attributes.getModifiedTime().toInstant().toEpochMilli());
            assertEquals((boolean) returnedFile.getDirectory(), attributes.isDirectory());
            assertEquals((long) returnedFile.getSize(), attributes.getSize());
            if (mutable) {
                assertEquals(
                    EnumSet.of(
                            PosixFilePermission.OWNER_READ,
                            PosixFilePermission.OWNER_EXECUTE,
                            PosixFilePermission.OWNER_WRITE
                    ), attributes.permissions()
                );
            } else {
                assertEquals(
                    EnumSet.of(
                            PosixFilePermission.OWNER_READ,
                            PosixFilePermission.OWNER_EXECUTE
                    ), attributes.permissions()
                );
            }

            Mockito.clearInvocations(sftpListUtil);
        }
    }

    public void testGetDefaultAbstractDirectoryAttributes() {
        SftpFileAttributes nonWritableAttributes = SftpListUtil.getDefaultAbstractDirectoryAttributes(false, null, null);
        assertTrue(nonWritableAttributes.isDirectory());
        assertFalse(nonWritableAttributes.isSymbolicLink());
        assertFalse(nonWritableAttributes.isRegularFile());
        assertFalse(nonWritableAttributes.isOther());
        assertEquals(EnumSet.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_EXECUTE
        ), nonWritableAttributes.getPermissions());
        assertEquals(0L, nonWritableAttributes.size());
        assertTrue(nonWritableAttributes.getModifiedTime().toInstant().isAfter(Instant.now().minusMillis(60000)));
        assertTrue(nonWritableAttributes.getCreationTime().toInstant().isAfter(Instant.now().minusMillis(60000)));
        assertTrue(nonWritableAttributes.getAccessTime().toInstant().isAfter(Instant.now().minusMillis(60000)));
        assertTrue(nonWritableAttributes.getModifiedTime().toInstant().isBefore(Instant.now().plusMillis(60000)));
        assertTrue(nonWritableAttributes.getCreationTime().toInstant().isBefore(Instant.now().plusMillis(60000)));
        assertTrue(nonWritableAttributes.getAccessTime().toInstant().isBefore(Instant.now().plusMillis(60000)));

        SftpFileAttributes writableAttributes = SftpListUtil.getDefaultAbstractDirectoryAttributes(true, null, null);
        assertTrue(writableAttributes.isDirectory());
        assertFalse(writableAttributes.isSymbolicLink());
        assertFalse(writableAttributes.isRegularFile());
        assertFalse(writableAttributes.isOther());
        assertEquals(EnumSet.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE
        ), writableAttributes.getPermissions());
        assertEquals(0L, writableAttributes.size());
        assertTrue(writableAttributes.getModifiedTime().toInstant().isAfter(Instant.now().minusMillis(60000)));
        assertTrue(writableAttributes.getCreationTime().toInstant().isAfter(Instant.now().minusMillis(60000)));
        assertTrue(writableAttributes.getAccessTime().toInstant().isAfter(Instant.now().minusMillis(60000)));
        assertTrue(writableAttributes.getModifiedTime().toInstant().isBefore(Instant.now().plusMillis(60000)));
        assertTrue(writableAttributes.getCreationTime().toInstant().isBefore(Instant.now().plusMillis(60000)));
        assertTrue(writableAttributes.getAccessTime().toInstant().isBefore(Instant.now().plusMillis(60000)));

        SftpFileAttributes nonWritableAttributesWithExplicitTs = SftpListUtil.getDefaultAbstractDirectoryAttributes(false, 2000L, 3000L);
        assertTrue(nonWritableAttributesWithExplicitTs.isDirectory());
        assertFalse(nonWritableAttributesWithExplicitTs.isSymbolicLink());
        assertFalse(nonWritableAttributesWithExplicitTs.isRegularFile());
        assertFalse(nonWritableAttributesWithExplicitTs.isOther());
        assertEquals(EnumSet.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_EXECUTE
        ), nonWritableAttributesWithExplicitTs.getPermissions());
        assertEquals(0L, nonWritableAttributesWithExplicitTs.size());
        assertEquals(nonWritableAttributesWithExplicitTs.getModifiedTime().toInstant(), Instant.ofEpochMilli(3000));
        assertEquals(nonWritableAttributesWithExplicitTs.getAccessTime().toInstant(), Instant.ofEpochMilli(3000));
        assertEquals(nonWritableAttributesWithExplicitTs.getCreationTime().toInstant(), Instant.ofEpochMilli(2000));

        SftpFileAttributes writableAttributesWithExplicitTs = SftpListUtil.getDefaultAbstractDirectoryAttributes(true, 20000L, 30000L);
        assertTrue(writableAttributesWithExplicitTs.isDirectory());
        assertFalse(writableAttributesWithExplicitTs.isSymbolicLink());
        assertFalse(writableAttributesWithExplicitTs.isRegularFile());
        assertFalse(writableAttributesWithExplicitTs.isOther());
        assertEquals(EnumSet.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE
        ), writableAttributesWithExplicitTs.getPermissions());
        assertEquals(0L, writableAttributesWithExplicitTs.size());
        assertEquals(writableAttributesWithExplicitTs.getModifiedTime().toInstant(), Instant.ofEpochMilli(30000));
        assertEquals(writableAttributesWithExplicitTs.getAccessTime().toInstant(), Instant.ofEpochMilli(30000));
        assertEquals(writableAttributesWithExplicitTs.getCreationTime().toInstant(), Instant.ofEpochMilli(20000));
    }

    public void testGetDisplayName() {
        Space space = new Space();
        space.setCode("space-1");
        assertEquals("space-1", SftpListUtil.getDisplayName(space));

        Project project = new Project();
        project.setCode("project-1");
        assertEquals("project-1", SftpListUtil.getDisplayName(project));

        Experiment experiment = new Experiment();
        ExperimentFetchOptions experimentFetchOptions = new ExperimentFetchOptions();
        experimentFetchOptions.withProperties();
        experiment.setFetchOptions(experimentFetchOptions);
        experiment.setStringProperty("NAME", "Exp NAME");
        experiment.setPermId(new ExperimentPermId("exp-perm-id-1"));
        assertEquals("Exp NAME (EXP-PERM-ID-1)", SftpListUtil.getDisplayName(experiment));

        DataSet dataSet = new DataSet();
        DataSetFetchOptions dataSetFetchOptions = new DataSetFetchOptions();
        dataSetFetchOptions.withProperties();
        dataSet.setFetchOptions(dataSetFetchOptions);
        dataSet.setStringProperty("NAME", "Dataset NAME");
        dataSet.setPermId(new DataSetPermId("dataset-perm-id-1"));
        assertEquals("Dataset NAME (DATASET-PERM-ID-1)", SftpListUtil.getDisplayName(dataSet));

        Sample sample = new Sample();
        SampleFetchOptions sampleFetchOptions = new SampleFetchOptions();
        sampleFetchOptions.withProperties();
        sample.setFetchOptions(sampleFetchOptions);
        sample.setStringProperty("NAME", "Sample NAME");
        sample.setPermId(new SamplePermId("sample-perm-id-1"));
        assertEquals("Sample NAME (SAMPLE-PERM-ID-1)", SftpListUtil.getDisplayName(sample));
    }

    public void testGetSpaceCodeFromDisplayName() {
        assertEquals("space-code-1", SftpListUtil.getSpaceCodeFromDisplayName("space-code-1"));
    }

    public void testGetProjectCodeFromDisplayName() {
        assertEquals("project-code-1", SftpListUtil.getProjectCodeFromDisplayName("project-code-1"));
    }

    public void testGetEntityPermIdFromDisplayName() {
        assertEquals("ENTITY-PERM-ID", SftpListUtil.getEntityPermIdFromDisplayName("naME Surname (ENTITY-PERM-ID)"));
        assertEquals("ENTITY-PERM-ID", SftpListUtil.getEntityPermIdFromDisplayName("naME Surname (  ENTITY-PERM-ID\t)"));
        assertEquals("ENTITY-PERM-ID", SftpListUtil.getEntityPermIdFromDisplayName("(ENTITY-PERM-ID)"));
        assertEquals("ENTITY-PERM-ID", SftpListUtil.getEntityPermIdFromDisplayName("(\tENTITY-PERM-ID  )"));
        assertEquals(null, SftpListUtil.getEntityPermIdFromDisplayName("ENTITY-PERM-ID"));
    }

    public void testGetEntityNameFromDisplayName() {
        assertEquals("naME Surname", SftpListUtil.getEntityNameFromDisplayName("naME Surname (ENTITY-PERM-ID)"));
        assertEquals("naME Surname", SftpListUtil.getEntityNameFromDisplayName("  naME Surname\t (ENTITY-PERM-ID)"));
        assertEquals("naME Surname", SftpListUtil.getEntityNameFromDisplayName("  naME Surname\t (ENTITY-PERM-ID)"));
        assertEquals("naME Su()rname", SftpListUtil.getEntityNameFromDisplayName("  naME Su()rname\t (ENTITY-PERM-ID)"));
        assertEquals(null, SftpListUtil.getEntityNameFromDisplayName("(ENTITY-PERM-ID)"));
        assertEquals(null, SftpListUtil.getEntityNameFromDisplayName("  (ENTITY-PERM-ID)"));
        assertEquals("ENTITY-PERM-ID", SftpListUtil.getEntityNameFromDisplayName("ENTITY-PERM-ID"));
        assertEquals("ENTITY-PERM-ID", SftpListUtil.getEntityNameFromDisplayName(" \tENTITY-PERM-ID "));
    }

    public void testCheckExistence() {
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        User user = User.builder()
                .username("u5er")
                .sessionToken("53551on")
                .build();
        OpenBIS openBISClientMock = Mockito.mock(
                OpenBIS.class
        );
        Mockito.doReturn(openBISClientMock).when(openBISClientUtil).getOpenBISClient(user);
        SftpListUtil sftpListUtil = new SftpListUtil(user, openBISClientUtil);

        for (Long registrationTs : new Long[]{ null, 1000L }) {
            for (Long modificationTs : new Long[]{ null, 100000L }) {
                for (boolean exists: List.of(false, true)) {
                    Space mockSpace = new Space();
                    if (registrationTs != null) { mockSpace.setRegistrationDate(new Date(registrationTs)); }
                    if (modificationTs != null) { mockSpace.setModificationDate(new Date(modificationTs)); }

                    Project mockProject = new Project();
                    if (registrationTs != null) { mockProject.setRegistrationDate(new Date(registrationTs)); }
                    if (modificationTs != null) { mockProject.setModificationDate(new Date(modificationTs)); }

                    Sample mockSample = new Sample();
                    if (registrationTs != null) { mockSample.setRegistrationDate(new Date(registrationTs)); }
                    if (modificationTs != null) { mockSample.setModificationDate(new Date(modificationTs)); }

                    Experiment mockExperiment = new Experiment();
                    if (registrationTs != null) { mockExperiment.setRegistrationDate(new Date(registrationTs)); }
                    if (modificationTs != null) { mockExperiment.setModificationDate(new Date(modificationTs)); }

                    DataSet mockDataSet = new DataSet();
                    if (registrationTs != null) { mockDataSet.setRegistrationDate(new Date(registrationTs)); }
                    if (modificationTs != null) { mockDataSet.setModificationDate(new Date(modificationTs)); }

                    if (exists) {
                        Mockito.doReturn(Collections.singletonMap(new SpacePermId("space_1"), mockSpace)).when(openBISClientMock).getSpaces(Mockito.any(), Mockito.any());
                        Mockito.doReturn(Collections.singletonMap(new ProjectIdentifier("space_1", "project_1"), mockProject)).when(openBISClientMock).getProjects(Mockito.any(), Mockito.any());
                        Mockito.doReturn(Collections.singletonMap(new SamplePermId("sample_1"), mockSample)).when(openBISClientMock).getSamples(Mockito.eq(List.of(new SamplePermId("sample_1"))), Mockito.any());
                        Mockito.doReturn(Collections.singletonMap(new SamplePermId("folder_1"), mockSample)).when(openBISClientMock).getSamples(Mockito.eq(List.of(new SamplePermId("folder_1"))), Mockito.any());
                        Mockito.doReturn(Collections.singletonMap(new ExperimentPermId("experiment_1"), mockExperiment)).when(openBISClientMock).getExperiments(Mockito.any(), Mockito.any());
                        Mockito.doReturn(Collections.singletonMap(new DataSetPermId("dataset_1"), mockDataSet)).when(openBISClientMock).getDataSets(Mockito.any(), Mockito.any());
                    } else {
                        Mockito.doReturn(Collections.emptyMap()).when(openBISClientMock).getSpaces(Mockito.any(), Mockito.any());
                        Mockito.doReturn(Collections.emptyMap()).when(openBISClientMock).getProjects(Mockito.any(), Mockito.any());
                        Mockito.doReturn(Collections.emptyMap()).when(openBISClientMock).getSamples(Mockito.any(), Mockito.any());
                        Mockito.doReturn(Collections.emptyMap()).when(openBISClientMock).getExperiments(Mockito.any(), Mockito.any());
                        Mockito.doReturn(Collections.emptyMap()).when(openBISClientMock).getDataSets(Mockito.any(), Mockito.any());
                    }

                    Mockito.clearInvocations(openBISClientMock);
                    FtpPathLister.EntityDescriptor spaceEntityDescriptor = new FtpPathLister.EntityDescriptor(
                            SftpNode.Type.SPACE,
                            Optional.of("space_1"), Optional.empty(), Optional.empty(), Optional.empty(),
                            Optional.of("space_1"), Optional.empty(), false, null, null
                    );
                    SftpListUtil.EntityBasicInfo spaceBasicInfo = sftpListUtil.checkExistence(spaceEntityDescriptor);
                    assertEquals(exists, spaceBasicInfo.exists());
                    assertEquals(exists ? modificationTs : null, spaceBasicInfo.lastModificationMillis());
                    assertEquals(exists ? registrationTs : null, spaceBasicInfo.registrationMillis());
                    ArgumentCaptor<List> getSpacesArg = ArgumentCaptor.forClass(List.class);
                    Mockito.verify(openBISClientMock, Mockito.times(1)).getSpaces(getSpacesArg.capture(), Mockito.any());
                    assertEquals("SPACE_1", ((SpacePermId) getSpacesArg.getValue().get(0)).getPermId());

                    Mockito.clearInvocations(openBISClientMock);
                    FtpPathLister.EntityDescriptor projectEntityDescriptor = new FtpPathLister.EntityDescriptor(
                            SftpNode.Type.PROJECT,
                            Optional.of("space_1"), Optional.of("project_1"), Optional.empty(), Optional.empty(),
                            Optional.of("/SPACE_1/PROJECT_1"), Optional.empty(), false, null, null
                    );
                    SftpListUtil.EntityBasicInfo projectBasicInfo = sftpListUtil.checkExistence(projectEntityDescriptor);
                    assertEquals(exists, projectBasicInfo.exists());
                    assertEquals(exists ? modificationTs : null, projectBasicInfo.lastModificationMillis());
                    assertEquals(exists ? registrationTs : null, projectBasicInfo.registrationMillis());
                    ArgumentCaptor<List> getProjectsArg = ArgumentCaptor.forClass(List.class);
                    Mockito.verify(openBISClientMock, Mockito.times(1)).getProjects(getProjectsArg.capture(), Mockito.any());
                    assertEquals("/SPACE_1/PROJECT_1", ((ProjectIdentifier) getProjectsArg.getValue().get(0)).getIdentifier());

                    Mockito.clearInvocations(openBISClientMock);
                    FtpPathLister.EntityDescriptor experimentEntityDescriptor = new FtpPathLister.EntityDescriptor(
                            SftpNode.Type.EXPERIMENT,
                            Optional.of("space_1"), Optional.of("project_1"), Optional.of("experiment_1"), Optional.empty(),
                            Optional.of("experiment_1"), Optional.of("experiment_name"), false, null, null
                    );
                    SftpListUtil.EntityBasicInfo experimentBasicInfo = sftpListUtil.checkExistence(experimentEntityDescriptor);
                    assertEquals(exists, experimentBasicInfo.exists());
                    assertEquals(exists ? modificationTs : null, experimentBasicInfo.lastModificationMillis());
                    assertEquals(exists ? registrationTs : null, experimentBasicInfo.registrationMillis());
                    ArgumentCaptor<List> getExperimentsArg = ArgumentCaptor.forClass(List.class);
                    Mockito.verify(openBISClientMock, Mockito.times(1)).getExperiments(getExperimentsArg.capture(), Mockito.any());
                    assertEquals("EXPERIMENT_1", ((ExperimentPermId) getExperimentsArg.getValue().get(0)).getPermId());

                    Mockito.clearInvocations(openBISClientMock);
                    FtpPathLister.EntityDescriptor experimentEntityDescriptorWithoutId = new FtpPathLister.EntityDescriptor(
                            SftpNode.Type.EXPERIMENT,
                            Optional.of("space_1"), Optional.of("project_1"), Optional.of("experiment_1"), Optional.empty(),
                            Optional.empty(), Optional.of("experiment_name"), false, null, null
                    );
                    experimentBasicInfo = sftpListUtil.checkExistence(experimentEntityDescriptorWithoutId);
                    assertFalse(experimentBasicInfo.exists());
                    assertEquals(null, experimentBasicInfo.lastModificationMillis());
                    assertEquals(null, experimentBasicInfo.registrationMillis());
                    Mockito.verify(openBISClientMock, Mockito.times(0)).getExperiments(Mockito.any(), Mockito.any());

                    Mockito.clearInvocations(openBISClientMock);
                    FtpPathLister.EntityDescriptor sampleEntityDescriptor = new FtpPathLister.EntityDescriptor(
                            SftpNode.Type.SAMPLE,
                            Optional.of("space_1"), Optional.of("project_1"), Optional.of("experiment_1"), Optional.empty(),
                            Optional.of("sample_1"), Optional.of("sample_name"), false, null, null
                    );
                    SftpListUtil.EntityBasicInfo sampleBasicInfo = sftpListUtil.checkExistence(sampleEntityDescriptor);
                    assertEquals(exists, sampleBasicInfo.exists());
                    assertEquals(exists ? modificationTs : null, sampleBasicInfo.lastModificationMillis());
                    assertEquals(exists ? registrationTs : null, sampleBasicInfo.registrationMillis());
                    ArgumentCaptor<List> getSamplesArg = ArgumentCaptor.forClass(List.class);
                    Mockito.verify(openBISClientMock, Mockito.times(1)).getSamples(getSamplesArg.capture(), Mockito.any());
                    assertEquals("SAMPLE_1", ((SamplePermId) getSamplesArg.getValue().get(0)).getPermId());

                    Mockito.clearInvocations(openBISClientMock);
                    FtpPathLister.EntityDescriptor sampleEntityDescriptorWithoutId = new FtpPathLister.EntityDescriptor(
                            SftpNode.Type.SAMPLE,
                            Optional.of("space_1"), Optional.of("project_1"), Optional.of("experiment_1"), Optional.empty(),
                            Optional.empty(), Optional.of("sample_name"), false, null, null
                    );
                    sampleBasicInfo = sftpListUtil.checkExistence(sampleEntityDescriptorWithoutId);
                    assertFalse(sampleBasicInfo.exists());
                    assertEquals(null, sampleBasicInfo.lastModificationMillis());
                    assertEquals(null, sampleBasicInfo.registrationMillis());
                    Mockito.verify(openBISClientMock, Mockito.times(0)).getSamples(Mockito.any(), Mockito.any());

                    Mockito.clearInvocations(openBISClientMock);
                    FtpPathLister.EntityDescriptor folderEntityDescriptor = new FtpPathLister.EntityDescriptor(
                            SftpNode.Type.FOLDER,
                            Optional.of("space_1"), Optional.of("project_1"), Optional.of("experiment_1"), Optional.empty(),
                            Optional.of("folder_1"), Optional.of("folder_name"), false, null, null
                    );
                    SftpListUtil.EntityBasicInfo folderBasicInfo = sftpListUtil.checkExistence(folderEntityDescriptor);
                    assertEquals(exists, folderBasicInfo.exists());
                    assertEquals(exists ? modificationTs : null, folderBasicInfo.lastModificationMillis());
                    assertEquals(exists ? registrationTs : null, folderBasicInfo.registrationMillis());
                    ArgumentCaptor<List> getFolderArg = ArgumentCaptor.forClass(List.class);
                    Mockito.verify(openBISClientMock, Mockito.times(1)).getSamples(getFolderArg.capture(), Mockito.any());
                    assertEquals("FOLDER_1", ((SamplePermId) getFolderArg.getValue().get(0)).getPermId());

                    Mockito.clearInvocations(openBISClientMock);
                    FtpPathLister.EntityDescriptor folderEntityDescriptorWithoutId = new FtpPathLister.EntityDescriptor(
                            SftpNode.Type.FOLDER,
                            Optional.of("space_1"), Optional.of("project_1"), Optional.of("experiment_1"), Optional.empty(),
                            Optional.empty(), Optional.of("folder_name"), false, null, null
                    );
                    folderBasicInfo = sftpListUtil.checkExistence(folderEntityDescriptorWithoutId);
                    assertFalse(folderBasicInfo.exists());
                    assertEquals(null, folderBasicInfo.lastModificationMillis());
                    assertEquals(null, folderBasicInfo.registrationMillis());
                    Mockito.verify(openBISClientMock, Mockito.times(0)).getSamples(Mockito.any(), Mockito.any());

                    Mockito.clearInvocations(openBISClientMock);
                    FtpPathLister.EntityDescriptor datasetEntityDescriptor = new FtpPathLister.EntityDescriptor(
                            SftpNode.Type.DATA_SET,
                            Optional.of("space_1"), Optional.of("project_1"), Optional.of("experiment_1"), Optional.of("sample_1"),
                            Optional.of("dataset_1"), Optional.empty(), false, null, null
                    );
                    SftpListUtil.EntityBasicInfo datasetBasicInfo = sftpListUtil.checkExistence(datasetEntityDescriptor);
                    assertEquals(exists, datasetBasicInfo.exists());
                    assertEquals(exists ? modificationTs : null, datasetBasicInfo.lastModificationMillis());
                    assertEquals(exists ? registrationTs : null, datasetBasicInfo.registrationMillis());
                    ArgumentCaptor<List> getDatasetsArg = ArgumentCaptor.forClass(List.class);
                    Mockito.verify(openBISClientMock, Mockito.times(1)).getDataSets(getDatasetsArg.capture(), Mockito.any());
                    assertEquals("DATASET_1", ((DataSetPermId) getDatasetsArg.getValue().get(0)).getPermId());

                    Mockito.clearInvocations(openBISClientMock);
                    FtpPathLister.EntityDescriptor datasetEntityDescriptorWithoutId = new FtpPathLister.EntityDescriptor(
                            SftpNode.Type.DATA_SET,
                            Optional.of("space_1"), Optional.of("project_1"), Optional.of("experiment_1"), Optional.of("sample_1"),
                            Optional.empty(), Optional.empty(), false, null, null
                    );
                    assertFalse(sftpListUtil.checkExistence(datasetEntityDescriptorWithoutId).exists());
                    Mockito.verify(openBISClientMock, Mockito.times(0)).getDataSets(Mockito.any(), Mockito.any());
                }
            }
        }
    }

    public void testCreateSpace() {
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        User user = User.builder()
                .username("u5er")
                .sessionToken("53551on")
                .build();
        OpenBIS openBISClientMock = Mockito.mock(
                OpenBIS.class
        );
        Mockito.doReturn(openBISClientMock).when(openBISClientUtil).getOpenBISClient(user);
        SftpListUtil sftpListUtil = new SftpListUtil(user, openBISClientUtil);
        sftpListUtil.createSpace("space_1");
        ArgumentCaptor<List> createSpacesArg = ArgumentCaptor.forClass(List.class);
        Mockito.verify(openBISClientMock, Mockito.times(1)).createSpaces(
            createSpacesArg.capture()
        );
        assertEquals("SPACE_1", ((SpaceCreation) createSpacesArg.getValue().get(0)).getCode());

        Exception exception = null;
        try {
            sftpListUtil.createSpace("space_1ç");
        } catch (Exception e) {
            exception = e;
        }
        assertEquals(IllegalArgumentException.class, exception.getClass());
    }

    public void testCreateProject() {
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        User user = User.builder()
                .username("u5er")
                .sessionToken("53551on")
                .build();
        OpenBIS openBISClientMock = Mockito.mock(
                OpenBIS.class
        );
        Mockito.doReturn(openBISClientMock).when(openBISClientUtil).getOpenBISClient(user);
        SftpListUtil sftpListUtil = new SftpListUtil(user, openBISClientUtil);
        sftpListUtil.createProject("space_1", "project_1");
        ArgumentCaptor<List> createProjectsArg = ArgumentCaptor.forClass(List.class);
        Mockito.verify(openBISClientMock, Mockito.times(1)).createProjects(
                createProjectsArg.capture()
        );
        assertEquals("SPACE_1", ((ProjectCreation) createProjectsArg.getValue().get(0)).getSpaceId().toString());
        assertEquals("PROJECT_1", ((ProjectCreation) createProjectsArg.getValue().get(0)).getCode());

        Exception exception = null;
        try {
            sftpListUtil.createProject("space_1", "project_code@");
        } catch (Exception e) {
            exception = e;
        }
        assertEquals(IllegalArgumentException.class, exception.getClass());
    }

    public void testCreateExperiment() {
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        User user = User.builder()
                .username("u5er")
                .sessionToken("53551on")
                .build();
        OpenBIS openBISClientMock = Mockito.mock(
                OpenBIS.class
        );
        Mockito.doReturn(openBISClientMock).when(openBISClientUtil).getOpenBISClient(user);
        SftpListUtil sftpListUtil = new SftpListUtil(user, openBISClientUtil);
        sftpListUtil.createExperiment("space_1", "project_1", "experiment name!");
        ArgumentCaptor<List> createExperimentsArg = ArgumentCaptor.forClass(List.class);
        Mockito.verify(openBISClientMock, Mockito.times(1)).createExperiments(
                createExperimentsArg.capture()
        );
        assertEquals("/SPACE_1/PROJECT_1", ((ExperimentCreation) createExperimentsArg.getValue().get(0)).getProjectId().toString());
        assertEquals("EXPERIMENT_NAME", ((ExperimentCreation) createExperimentsArg.getValue().get(0)).getCode());
        assertEquals("experiment name!", ((ExperimentCreation) createExperimentsArg.getValue().get(0)).getStringProperty(SftpListUtil.PROPERTY_NAME));
    }

    public void testCreateSample() {
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        User user = User.builder()
                .username("u5er")
                .sessionToken("53551on")
                .build();
        OpenBIS openBISClientMock = Mockito.mock(
                OpenBIS.class
        );
        Mockito.doReturn(openBISClientMock).when(openBISClientUtil).getOpenBISClient(user);
        SftpListUtil sftpListUtil = new SftpListUtil(user, openBISClientUtil);

        for (boolean folder: List.of(false, true)) {
            for (boolean attachedToExperiment: List.of(false, true)) {
                Mockito.clearInvocations(openBISClientMock);
                sftpListUtil.createSample(
                        "space_1",
                        "project_1",
                        attachedToExperiment ? "experiment_1" : null,
                        attachedToExperiment ? null : "sample_1",
                        "sample! name?",
                        folder
                );
                ArgumentCaptor<List> createSamplesArg = ArgumentCaptor.forClass(List.class);
                Mockito.verify(openBISClientMock, Mockito.times(1)).createSamples(
                        createSamplesArg.capture()
                );
                assertEquals("/SPACE_1/PROJECT_1", ((SampleCreation) createSamplesArg.getValue().get(0)).getProjectId().toString());
                if (attachedToExperiment) {
                    assertEquals("EXPERIMENT_1", ((SampleCreation) createSamplesArg.getValue().get(0)).getExperimentId().toString());
                    assertNull(((SampleCreation) createSamplesArg.getValue().get(0)).getParentIds());
                } else {
                    assertEquals("SAMPLE_1", ((SampleCreation) createSamplesArg.getValue().get(0)).getParentIds().get(0).toString());
                    assertNull(((SampleCreation) createSamplesArg.getValue().get(0)).getExperimentId());
                }
                assertEquals("SAMPLE_NAME", ((SampleCreation) createSamplesArg.getValue().get(0)).getCode());
                assertEquals(
                        folder ? SftpListUtil.FOLDER_SAMPLE_TYPE : SftpListUtil.ENTRY_SAMPLE_TYPE,
                        ((SampleCreation) createSamplesArg.getValue().get(0)).getTypeId().toString()
                );
                assertEquals("sample! name?", ((SampleCreation) createSamplesArg.getValue().get(0)).getStringProperty(SftpListUtil.PROPERTY_NAME));
            }
        }
    }

    public void testDeleteSpace() {
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        User user = User.builder()
                .username("u5er")
                .sessionToken("53551on")
                .build();
        OpenBIS openBISClientMock = Mockito.mock(
                OpenBIS.class
        );
        Mockito.doReturn(openBISClientMock).when(openBISClientUtil).getOpenBISClient(user);
        SftpListUtil sftpListUtil = Mockito.spy(new SftpListUtil(user, openBISClientUtil));
        Mockito.doReturn(Collections.emptyList()).when(sftpListUtil).getSpaceSamples(Mockito.anyString());
        Mockito.doReturn(Collections.emptyList()).when(sftpListUtil).getProjects(Mockito.anyString());
        sftpListUtil.deleteSpace("space_1");
        ArgumentCaptor<List> deleteSpacesArg = ArgumentCaptor.forClass(List.class);
        Mockito.verify(openBISClientMock, Mockito.times(1)).deleteSpaces(
                deleteSpacesArg.capture(), Mockito.any()
        );
        assertEquals("SPACE_1", ((SpacePermId) deleteSpacesArg.getValue().get(0)).getPermId());
    }

    public void testDeleteSpaceFailsIfSpaceNotEmpty() {
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        User user = User.builder()
                .username("u5er")
                .sessionToken("53551on")
                .build();
        OpenBIS openBISClientMock = Mockito.mock(
                OpenBIS.class
        );
        Mockito.doReturn(openBISClientMock).when(openBISClientUtil).getOpenBISClient(user);
        SftpListUtil sftpListUtil = Mockito.spy(new SftpListUtil(user, openBISClientUtil));

        for (boolean someSamples: List.of(false, true)) {
            for (boolean someProjects: List.of(false, true)) {
                if (someSamples || someProjects) {
                    Mockito.doReturn(someSamples ? Collections.singletonList(new Sample()) : Collections.emptyList())
                            .when(sftpListUtil).getSpaceSamples(Mockito.anyString());
                    Mockito.doReturn(someProjects ? Collections.singletonList(new Project()) : Collections.emptyList())
                            .when(sftpListUtil).getProjects(Mockito.anyString());
                    Exception exception = null;
                    try {
                        sftpListUtil.deleteSpace("space_1");
                    } catch (Exception e) {
                        exception = e;
                    }
                    assertNotNull(exception);
                    Mockito.verify(openBISClientMock, Mockito.times(0)).deleteSpaces(
                            Mockito.anyList(), Mockito.any()
                    );
                }
            }
        }
    }

    public void testDeleteProject() {
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        User user = User.builder()
                .username("u5er")
                .sessionToken("53551on")
                .build();
        OpenBIS openBISClientMock = Mockito.mock(
                OpenBIS.class
        );
        Mockito.doReturn(openBISClientMock).when(openBISClientUtil).getOpenBISClient(user);
        SftpListUtil sftpListUtil = Mockito.spy(new SftpListUtil(user, openBISClientUtil));
        Mockito.doReturn(Collections.emptyList()).when(sftpListUtil).getProjectSamples(Mockito.anyString());
        Mockito.doReturn(Collections.emptyList()).when(sftpListUtil).getExperiments(Mockito.anyString());
        sftpListUtil.deleteProject("/SPACE_1/PROJECT_1");
        ArgumentCaptor<List> deleteProjectsArg = ArgumentCaptor.forClass(List.class);
        Mockito.verify(openBISClientMock, Mockito.times(1)).deleteProjects(
                deleteProjectsArg.capture(), Mockito.any()
        );
        assertEquals("/SPACE_1/PROJECT_1", ((ProjectIdentifier) deleteProjectsArg.getValue().get(0)).getIdentifier());
    }

    public void testDeleteProjectFailsIfProjectNotEmpty() {
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        User user = User.builder()
                .username("u5er")
                .sessionToken("53551on")
                .build();
        OpenBIS openBISClientMock = Mockito.mock(
                OpenBIS.class
        );
        Mockito.doReturn(openBISClientMock).when(openBISClientUtil).getOpenBISClient(user);
        SftpListUtil sftpListUtil = Mockito.spy(new SftpListUtil(user, openBISClientUtil));

        for (boolean someSamples: List.of(false, true)) {
            for (boolean someExperiments: List.of(false, true)) {
                if (someSamples || someExperiments) {
                    Mockito.doReturn(someSamples ? Collections.singletonList(new Sample()) : Collections.emptyList())
                            .when(sftpListUtil).getProjectSamples(Mockito.anyString());
                    Mockito.doReturn(someExperiments ? Collections.singletonList(new Experiment()) : Collections.emptyList())
                            .when(sftpListUtil).getExperiments(Mockito.anyString());
                    Exception exception = null;
                    try {
                        sftpListUtil.deleteProject("/SPACE_1/PROJECT_1");
                    } catch (Exception e) {
                        exception = e;
                    }
                    assertNotNull(exception);
                    Mockito.verify(openBISClientMock, Mockito.times(0)).deleteProjects(
                            Mockito.anyList(), Mockito.any()
                    );
                }
            }
        }
    }

    public void testDeleteExperiment() {
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        User user = User.builder()
                .username("u5er")
                .sessionToken("53551on")
                .build();
        OpenBIS openBISClientMock = Mockito.mock(
                OpenBIS.class
        );
        Mockito.doReturn(openBISClientMock).when(openBISClientUtil).getOpenBISClient(user);
        SftpListUtil sftpListUtil = new SftpListUtil(user, openBISClientUtil);
        sftpListUtil.deleteExperiment("experiment_id_1");
        ArgumentCaptor<List> deleteExperimentsArg = ArgumentCaptor.forClass(List.class);
        Mockito.verify(openBISClientMock, Mockito.times(1)).deleteExperiments(
                deleteExperimentsArg.capture(), Mockito.any()
        );
        assertEquals("EXPERIMENT_ID_1", ((ExperimentPermId) deleteExperimentsArg.getValue().get(0)).getPermId());
    }

    public void testDeleteExperimentFailsIfExperimentNotEmpty() {
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        User user = User.builder()
                .username("u5er")
                .sessionToken("53551on")
                .build();
        OpenBIS openBISClientMock = Mockito.mock(
                OpenBIS.class
        );
        Mockito.doReturn(openBISClientMock).when(openBISClientUtil).getOpenBISClient(user);
        SftpListUtil sftpListUtil = Mockito.spy(new SftpListUtil(user, openBISClientUtil));

        for (boolean someSamples: List.of(false, true)) {
            for (boolean someDatasets: List.of(false, true)) {
                if (someSamples || someDatasets) {
                    Mockito.doReturn(someSamples ? Collections.singletonList(new Sample()) : Collections.emptyList())
                            .when(sftpListUtil).getExperimentSamples(Mockito.anyString());
                    Mockito.doReturn(someDatasets ? Collections.singletonList(new DataSet()) : Collections.emptyList())
                            .when(sftpListUtil).getExperimentDatasets(Mockito.anyString());
                    Exception exception = null;
                    try {
                        sftpListUtil.deleteExperiment("experiment_id_1");
                    } catch (Exception e) {
                        exception = e;
                    }
                    assertNotNull(exception);
                    Mockito.verify(openBISClientMock, Mockito.times(0)).deleteExperiments(
                            Mockito.anyList(), Mockito.any()
                    );
                }
            }
        }
    }

    public void testDeleteSample() {
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        User user = User.builder()
                .username("u5er")
                .sessionToken("53551on")
                .build();
        OpenBIS openBISClientMock = Mockito.mock(
                OpenBIS.class
        );
        Mockito.doReturn(openBISClientMock).when(openBISClientUtil).getOpenBISClient(user);
        SftpListUtil sftpListUtil = new SftpListUtil(user, openBISClientUtil);
        sftpListUtil.deleteSample("sample_id_1");
        ArgumentCaptor<List> deleteSamplesArg = ArgumentCaptor.forClass(List.class);
        Mockito.verify(openBISClientMock, Mockito.times(1)).deleteSamples(
                deleteSamplesArg.capture(), Mockito.any()
        );
        assertEquals("SAMPLE_ID_1", ((SamplePermId) deleteSamplesArg.getValue().get(0)).getPermId());
    }

    public void testDeleteSampleFailsIfSampleNotEmpty() {
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        User user = User.builder()
                .username("u5er")
                .sessionToken("53551on")
                .build();
        OpenBIS openBISClientMock = Mockito.mock(
                OpenBIS.class
        );
        Mockito.doReturn(openBISClientMock).when(openBISClientUtil).getOpenBISClient(user);
        SftpListUtil sftpListUtil = Mockito.spy(new SftpListUtil(user, openBISClientUtil));

        for (boolean someSamples: List.of(false, true)) {
            for (boolean someDatasets: List.of(false, true)) {
                if (someSamples || someDatasets) {
                    Mockito.doReturn(someSamples ? Collections.singletonList(new Sample()) : Collections.emptyList())
                            .when(sftpListUtil).getSampleChildren(Mockito.anyString());
                    Mockito.doReturn(someDatasets ? Collections.singletonList(new DataSet()) : Collections.emptyList())
                            .when(sftpListUtil).getSampleDatasets(Mockito.anyString());
                    Exception exception = null;
                    try {
                        sftpListUtil.deleteSample("sample_id_1");
                    } catch (Exception e) {
                        exception = e;
                    }
                    assertNotNull(exception);
                    Mockito.verify(openBISClientMock, Mockito.times(0)).deleteSamples(
                            Mockito.anyList(), Mockito.any()
                    );
                }
            }
        }
    }

    public void testDeleteDataSet() {
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        User user = User.builder()
                .username("u5er")
                .sessionToken("53551on")
                .build();
        OpenBIS openBISClientMock = Mockito.mock(
                OpenBIS.class
        );
        Mockito.doReturn(openBISClientMock).when(openBISClientUtil).getOpenBISClient(user);
        SftpListUtil sftpListUtil = new SftpListUtil(user, openBISClientUtil);
        sftpListUtil.deleteDataSet("dataset_1");
        ArgumentCaptor<List> deleteDatasetsArg = ArgumentCaptor.forClass(List.class);
        Mockito.verify(openBISClientMock, Mockito.times(1)).deleteDataSets(
                deleteDatasetsArg.capture(), Mockito.any()
        );
        assertEquals("DATASET_1", ((DataSetPermId) deleteDatasetsArg.getValue().get(0)).getPermId());
    }

    public void testRenameExperiment() {
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        User user = User.builder()
                .username("u5er")
                .sessionToken("53551on")
                .build();
        OpenBIS openBISClientMock = Mockito.mock(
                OpenBIS.class
        );
        Mockito.doReturn(openBISClientMock).when(openBISClientUtil).getOpenBISClient(user);
        SftpListUtil sftpListUtil = new SftpListUtil(user, openBISClientUtil);
        sftpListUtil.renameExperiment("experiment_id_1", "NEW NaMe");
        ArgumentCaptor<List> updateExperimentsArg = ArgumentCaptor.forClass(List.class);
        Mockito.verify(openBISClientMock, Mockito.times(1)).updateExperiments(
                updateExperimentsArg.capture()
        );
        assertEquals("EXPERIMENT_ID_1", ((ExperimentUpdate) updateExperimentsArg.getValue().get(0)).getExperimentId().toString());
        assertEquals("NEW NaMe", ((ExperimentUpdate) updateExperimentsArg.getValue().get(0)).getStringProperty(SftpListUtil.PROPERTY_NAME));
    }

    public void testRenameSample() {
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        User user = User.builder()
                .username("u5er")
                .sessionToken("53551on")
                .build();
        OpenBIS openBISClientMock = Mockito.mock(
                OpenBIS.class
        );
        Mockito.doReturn(openBISClientMock).when(openBISClientUtil).getOpenBISClient(user);
        SftpListUtil sftpListUtil = new SftpListUtil(user, openBISClientUtil);
        sftpListUtil.renameSample("sample_id_1", "NEW NaMe");
        ArgumentCaptor<List> updateSamplesArg = ArgumentCaptor.forClass(List.class);
        Mockito.verify(openBISClientMock, Mockito.times(1)).updateSamples(
                updateSamplesArg.capture()
        );
        assertEquals("SAMPLE_ID_1", ((SampleUpdate) updateSamplesArg.getValue().get(0)).getSampleId().toString());
        assertEquals("NEW NaMe", ((SampleUpdate) updateSamplesArg.getValue().get(0)).getStringProperty(SftpListUtil.PROPERTY_NAME));
    }

    public void testIsLegalOpenBISCode() {
        assertTrue(SftpListUtil.isLegalOpenBISCode("ASFDaaa325235-._"));
        assertTrue(SftpListUtil.isLegalOpenBISCode("ASFDaaa325235"));
        assertFalse(SftpListUtil.isLegalOpenBISCode("ASFDa!aa325235"));
        assertFalse(SftpListUtil.isLegalOpenBISCode("ASFDa@aa325235"));
        assertFalse(SftpListUtil.isLegalOpenBISCode("ASFDaaa325*235"));
        assertFalse(SftpListUtil.isLegalOpenBISCode("@ASFDaaa325235"));
    }
}