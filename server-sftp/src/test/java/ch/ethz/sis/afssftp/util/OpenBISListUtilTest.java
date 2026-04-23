package ch.ethz.sis.afssftp.util;

import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afsclient.client.AfsClient;
import ch.ethz.sis.afssftp.authentication.OpenBISUser;
import ch.ethz.sis.afssftp.filesystemview.OpenBISSftpFileAttributes;
import ch.ethz.sis.afssftp.filesystemview.OpenBISSftpNode;
import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.fetchoptions.ProjectFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.ProjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SamplePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.fetchoptions.SpaceFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.ISpaceId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.search.SpaceSearchCriteria;
import junit.framework.TestCase;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.net.URI;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

import static ch.ethz.sis.afssftp.helpers.TestHelper.createRandomNodeOfType;

public class OpenBISListUtilTest extends TestCase {

    public void testGetSpaces() {
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        OpenBIS openBIS = Mockito.mock(OpenBIS.class);
        OpenBISUser openBISUser = OpenBISUser.builder()
                .username("u5er")
                .password("pWd")
                .sessionToken("53551on")
                .build();
        OpenBISListUtil openBISListUtil = new OpenBISListUtil(openBISUser, openBISClientUtil);
        Mockito.doReturn(openBIS).when(openBISClientUtil).getOpenBISClient(openBISUser);
        Mockito.doReturn(new SearchResult<>(Collections.emptyList(), 0)).when(openBIS).searchSpaces(
                Mockito.any(), Mockito.any()
        );
        openBISListUtil.getSpaces();
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
        OpenBISUser openBISUser = OpenBISUser.builder()
                .username("u5er")
                .password("pWd")
                .sessionToken("53551on")
                .build();
        OpenBISListUtil openBISListUtil = new OpenBISListUtil(openBISUser, openBISClientUtil);
        Mockito.doReturn(openBIS).when(openBISClientUtil).getOpenBISClient(openBISUser);
        Mockito.doReturn(Collections.emptyMap()).when(openBIS).getSpaces(
                Mockito.any(), Mockito.any()
        );
        openBISListUtil.getProjects("space-perm-id");
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
        OpenBISUser openBISUser = OpenBISUser.builder()
                .username("u5er")
                .password("pWd")
                .sessionToken("53551on")
                .build();
        OpenBISListUtil openBISListUtil = new OpenBISListUtil(openBISUser, openBISClientUtil);
        Mockito.doReturn(openBIS).when(openBISClientUtil).getOpenBISClient(openBISUser);
        Mockito.doReturn(Collections.emptyMap()).when(openBIS).getProjects(
                Mockito.any(), Mockito.any()
        );
        openBISListUtil.getExperiments("space-code", "project-code");
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

        assertFalse(OpenBISListUtil.isOfTypeFolder(sample));
        assertTrue(OpenBISListUtil.isOfTypeFolder(folder));
    }

    public void testGetSpaceSamples() {
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        OpenBIS openBIS = Mockito.mock(OpenBIS.class);
        OpenBISUser openBISUser = OpenBISUser.builder()
                .username("u5er")
                .password("pWd")
                .sessionToken("53551on")
                .build();
        OpenBISListUtil openBISListUtil = new OpenBISListUtil(openBISUser, openBISClientUtil);
        Mockito.doReturn(openBIS).when(openBISClientUtil).getOpenBISClient(openBISUser);
        Mockito.doReturn(Collections.emptyMap()).when(openBIS).getSpaces(
                Mockito.any(), Mockito.any()
        );
        openBISListUtil.getSpaceSamples("space-perm-id");
        ArgumentCaptor<List<? extends ISpaceId>> spaceIdListArgumentCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<SpaceFetchOptions> spaceFetchOptionsArgumentCaptor = ArgumentCaptor.forClass(SpaceFetchOptions.class);
        Mockito.verify(openBIS, Mockito.times(1)).getSpaces(
                spaceIdListArgumentCaptor.capture(), spaceFetchOptionsArgumentCaptor.capture()
        );
        assertEquals(new SamplePermId("space-perm-id").getPermId(), ((SpacePermId) (spaceIdListArgumentCaptor.getValue().get(0))).getPermId());
        assertTrue(spaceFetchOptionsArgumentCaptor.getValue().hasSamples());
        assertTrue(spaceFetchOptionsArgumentCaptor.getValue().withSamples().hasProject());
        assertTrue(spaceFetchOptionsArgumentCaptor.getValue().withSamples().hasExperiment());
        assertTrue(spaceFetchOptionsArgumentCaptor.getValue().withSamples().hasParents());
    }

    public void testGetProjectSamples() {
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        OpenBIS openBIS = Mockito.mock(OpenBIS.class);
        OpenBISUser openBISUser = OpenBISUser.builder()
                .username("u5er")
                .password("pWd")
                .sessionToken("53551on")
                .build();
        OpenBISListUtil openBISListUtil = new OpenBISListUtil(openBISUser, openBISClientUtil);
        Mockito.doReturn(openBIS).when(openBISClientUtil).getOpenBISClient(openBISUser);
        Mockito.doReturn(Collections.emptyMap()).when(openBIS).getProjects(
                Mockito.any(), Mockito.any()
        );
        openBISListUtil.getProjectSamples("space-code", "project-code");
        ArgumentCaptor<List<? extends ProjectIdentifier>> projectIdListArgumentCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<ProjectFetchOptions> projectFetchOptionsArgumentCaptor = ArgumentCaptor.forClass(ProjectFetchOptions.class);
        Mockito.verify(openBIS, Mockito.times(1)).getProjects(
                projectIdListArgumentCaptor.capture(), projectFetchOptionsArgumentCaptor.capture()
        );
        assertEquals(new ProjectIdentifier("space-code", "project-code").getIdentifier(), projectIdListArgumentCaptor.getValue().get(0).getIdentifier());
        assertTrue(projectFetchOptionsArgumentCaptor.getValue().hasSamples());
        assertTrue(projectFetchOptionsArgumentCaptor.getValue().withSamples().hasExperiment());
        assertTrue(projectFetchOptionsArgumentCaptor.getValue().withSamples().hasParents());
    }

    public void testGetExperimentSamples() {
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        OpenBIS openBIS = Mockito.mock(OpenBIS.class);
        OpenBISUser openBISUser = OpenBISUser.builder()
                .username("u5er")
                .password("pWd")
                .sessionToken("53551on")
                .build();
        OpenBISListUtil openBISListUtil = new OpenBISListUtil(openBISUser, openBISClientUtil);
        Mockito.doReturn(openBIS).when(openBISClientUtil).getOpenBISClient(openBISUser);
        Mockito.doReturn(Collections.emptyMap()).when(openBIS).getProjects(
                Mockito.any(), Mockito.any()
        );
        openBISListUtil.getExperimentSamples("space-code", "project-code", "experiment-code");
        ArgumentCaptor<List<? extends ExperimentIdentifier>> experimentIdListArgumentCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<ExperimentFetchOptions> experimentFetchOptionsArgumentCaptor = ArgumentCaptor.forClass(ExperimentFetchOptions.class);
        Mockito.verify(openBIS, Mockito.times(1)).getExperiments(
                experimentIdListArgumentCaptor.capture(), experimentFetchOptionsArgumentCaptor.capture()
        );
        assertEquals(new ExperimentIdentifier("space-code", "project-code", "experiment-code").getIdentifier(), experimentIdListArgumentCaptor.getValue().get(0).getIdentifier());
        assertTrue(experimentFetchOptionsArgumentCaptor.getValue().hasSamples());
        assertTrue(experimentFetchOptionsArgumentCaptor.getValue().withSamples().hasParents());
    }

    public void testGetSampleChildren() {
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        OpenBIS openBIS = Mockito.mock(OpenBIS.class);
        OpenBISUser openBISUser = OpenBISUser.builder()
                .username("u5er")
                .password("pWd")
                .sessionToken("53551on")
                .build();
        OpenBISListUtil openBISListUtil = new OpenBISListUtil(openBISUser, openBISClientUtil);
        Mockito.doReturn(openBIS).when(openBISClientUtil).getOpenBISClient(openBISUser);
        Mockito.doReturn(Collections.emptyMap()).when(openBIS).getSamples(
                Mockito.any(), Mockito.any()
        );
        openBISListUtil.getSampleChildren("space-code", "project-code", "sample-code");
        ArgumentCaptor<List<? extends SampleIdentifier>> sampleIdListArgumentCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<SampleFetchOptions> sampleFetchOptionsArgumentCaptor = ArgumentCaptor.forClass(SampleFetchOptions.class);
        Mockito.verify(openBIS, Mockito.times(1)).getSamples(
                sampleIdListArgumentCaptor.capture(), sampleFetchOptionsArgumentCaptor.capture()
        );
        assertEquals(new SampleIdentifier("space-code", "project-code", null, "sample-code").getIdentifier(), sampleIdListArgumentCaptor.getValue().get(0).getIdentifier());
        assertTrue(sampleFetchOptionsArgumentCaptor.getValue().hasChildren());
    }

    public void testGetSampleDatasets() {
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        OpenBIS openBIS = Mockito.mock(OpenBIS.class);
        OpenBISUser openBISUser = OpenBISUser.builder()
                .username("u5er")
                .password("pWd")
                .sessionToken("53551on")
                .build();
        OpenBISListUtil openBISListUtil = new OpenBISListUtil(openBISUser, openBISClientUtil);
        Mockito.doReturn(openBIS).when(openBISClientUtil).getOpenBISClient(openBISUser);
        Mockito.doReturn(Collections.emptyMap()).when(openBIS).getSamples(
                Mockito.any(), Mockito.any()
        );
        openBISListUtil.getSampleDatasets("space-code", "project-code", "sample-code");
        ArgumentCaptor<List<? extends SampleIdentifier>> sampleIdListArgumentCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<SampleFetchOptions> sampleFetchOptionsArgumentCaptor = ArgumentCaptor.forClass(SampleFetchOptions.class);
        Mockito.verify(openBIS, Mockito.times(1)).getSamples(
                sampleIdListArgumentCaptor.capture(), sampleFetchOptionsArgumentCaptor.capture()
        );
        assertEquals(new SampleIdentifier("space-code", "project-code", null, "sample-code").getIdentifier(), sampleIdListArgumentCaptor.getValue().get(0).getIdentifier());
        assertTrue(sampleFetchOptionsArgumentCaptor.getValue().hasDataSets());
    }

    public void testGetAfsEntityPermId() {
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        OpenBIS openBIS = Mockito.mock(OpenBIS.class);
        OpenBISUser openBISUser = OpenBISUser.builder()
                .username("u5er")
                .password("pWd")
                .sessionToken("53551on")
                .build();
        OpenBISListUtil openBISListUtil = new OpenBISListUtil(openBISUser, openBISClientUtil);
        String spaceCode = "space-code";
        String projectCode = "project-code";

        Mockito.doReturn(openBIS).when(openBISClientUtil).getOpenBISClient(openBISUser);

        //SAMPLE
        OpenBISSftpNode sampleNode = createRandomNodeOfType(OpenBISSftpNode.Type.SAMPLE);
        Mockito.doReturn(Collections.emptyMap()).when(openBIS).getSamples(
                Mockito.any(), Mockito.any()
        );
        openBISListUtil.getAfsEntityPermId(sampleNode, spaceCode, projectCode);
        ArgumentCaptor<List<? extends SampleIdentifier>> sampleIdListArgumentCaptor1 = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<SampleFetchOptions> sampleFetchOptionsArgumentCaptor1 = ArgumentCaptor.forClass(SampleFetchOptions.class);
        Mockito.verify(openBIS, Mockito.times(1)).getSamples(
                sampleIdListArgumentCaptor1.capture(), sampleFetchOptionsArgumentCaptor1.capture()
        );
        assertEquals(new SampleIdentifier(spaceCode, projectCode, null, sampleNode.getIdentifier().get()).getIdentifier(), sampleIdListArgumentCaptor1.getValue().get(0).getIdentifier());
        Mockito.clearInvocations(openBIS);

        //FOLDER
        OpenBISSftpNode folderNode = createRandomNodeOfType(OpenBISSftpNode.Type.FOLDER);
        Mockito.doReturn(Collections.emptyMap()).when(openBIS).getSamples(
                Mockito.any(), Mockito.any()
        );
        openBISListUtil.getAfsEntityPermId(folderNode, spaceCode, projectCode);
        ArgumentCaptor<List<? extends SampleIdentifier>> sampleIdListArgumentCaptor2 = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<SampleFetchOptions> sampleFetchOptionsArgumentCaptor2 = ArgumentCaptor.forClass(SampleFetchOptions.class);
        Mockito.verify(openBIS, Mockito.times(1)).getSamples(
                sampleIdListArgumentCaptor2.capture(), sampleFetchOptionsArgumentCaptor2.capture()
        );
        assertEquals(new SampleIdentifier(spaceCode, projectCode, null, folderNode.getIdentifier().get()).getIdentifier(), sampleIdListArgumentCaptor2.getValue().get(0).getIdentifier());
        Mockito.clearInvocations(openBIS);

        //DATASET
        OpenBISSftpNode datasetNode = createRandomNodeOfType(OpenBISSftpNode.Type.DATA_SET);
        Mockito.doReturn(Collections.emptyMap()).when(openBIS).getDataSets(
                Mockito.any(), Mockito.any()
        );
        openBISListUtil.getAfsEntityPermId(datasetNode, spaceCode, projectCode);
        ArgumentCaptor<List<? extends DataSetPermId>> datasetIdArgumentCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<DataSetFetchOptions> datasetFetchOptionsArgumentCaptor = ArgumentCaptor.forClass(DataSetFetchOptions.class);
        Mockito.verify(openBIS, Mockito.times(1)).getDataSets(
                datasetIdArgumentCaptor.capture(), datasetFetchOptionsArgumentCaptor.capture()
        );
        assertEquals(new DataSetPermId(datasetNode.getIdentifier().get()).getPermId(), datasetIdArgumentCaptor.getValue().get(0).getPermId());
        Mockito.clearInvocations(openBIS);

        //EXPERIMENT
        OpenBISSftpNode experimentNode = createRandomNodeOfType(OpenBISSftpNode.Type.EXPERIMENT);
        Mockito.doReturn(Collections.emptyMap()).when(openBIS).getDataSets(
                Mockito.any(), Mockito.any()
        );
        openBISListUtil.getAfsEntityPermId(experimentNode, spaceCode, projectCode);
        ArgumentCaptor<List<? extends ExperimentIdentifier>> experimentIdArgumentCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<ExperimentFetchOptions> experimentFetchOptionsArgumentCaptor = ArgumentCaptor.forClass(ExperimentFetchOptions.class);
        Mockito.verify(openBIS, Mockito.times(1)).getExperiments(
                experimentIdArgumentCaptor.capture(), experimentFetchOptionsArgumentCaptor.capture()
        );
        assertEquals(new ExperimentIdentifier(spaceCode, projectCode, experimentNode.getIdentifier().get()).getIdentifier(), experimentIdArgumentCaptor.getValue().get(0).getIdentifier());
        Mockito.clearInvocations(openBIS);
    }

    public void testListAfsFiles() throws Exception {
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        OpenBISUser openBISUser = OpenBISUser.builder()
                .username("u5er")
                .password("pWd")
                .sessionToken("53551on")
                .build();
        AfsClientProxy afsClientProxyMock = Mockito.spy(
                new AfsClientProxy(new AfsClient(URI.create("http://example.com:8080/afs-server")))
        );
        Mockito.doReturn(afsClientProxyMock).when(openBISClientUtil).getAfsClient(openBISUser);
        OpenBISListUtil openBISListUtil = new OpenBISListUtil(openBISUser, openBISClientUtil);
        Mockito.doReturn(new File[0]).when(afsClientProxyMock).list("entity-id", "/dir/file", false);
        openBISListUtil.listAfsFiles("entity-id", "/dir/file");
        Mockito.verify(openBISClientUtil, Mockito.times(1)).getAfsClient(openBISUser);
        Mockito.verify(afsClientProxyMock, Mockito.times(1)).list("entity-id", "/dir/file", false);
    }

    public void testGetAfsFilePresence() {
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        OpenBISUser openBISUser = OpenBISUser.builder()
                .username("u5er")
                .password("pWd")
                .sessionToken("53551on")
                .build();
        AfsClientProxy afsClientProxyMock = Mockito.spy(
                new AfsClientProxy(new AfsClient(URI.create("http://example.com:8080/afs-server")))
        );
        Mockito.doReturn(afsClientProxyMock).when(openBISClientUtil).getAfsClient(openBISUser);
        OpenBISListUtil openBISListUtil = new OpenBISListUtil(openBISUser, openBISClientUtil);
        try {
            openBISListUtil.getAfsFilePresence("entity-id", "/dir/file");
        } catch (Exception e) {}
        Mockito.verify(openBISClientUtil, Mockito.times(1)).getAfsClient(openBISUser);
    }

    public void testGetDefaultAfsFileAttributes() {
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        OpenBISUser openBISUser = OpenBISUser.builder()
                .username("u5er")
                .password("pWd")
                .sessionToken("53551on")
                .build();
        Mockito.doReturn(new AfsClientProxy(new AfsClient(URI.create("http://example.com:8080/afs-server"))))
                .when(openBISClientUtil).getAfsClient(openBISUser);
        OpenBISListUtil openBISListUtil = Mockito.spy(new OpenBISListUtil(openBISUser, openBISClientUtil));
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
        Mockito.doReturn(Optional.of(returnedFile)).when(openBISListUtil).getAfsFilePresence(entityId, filePath);
        OpenBISSftpFileAttributes attributes = openBISListUtil.getDefaultAfsFileAttributes(entityId, filePath).get();
        Mockito.verify(openBISListUtil, Mockito.times(1)).getAfsFilePresence(entityId, filePath);
        assertEquals(returnedFile.getLastModifiedTime().toInstant().toEpochMilli(), attributes.getModifiedTime().toInstant().toEpochMilli());
        assertEquals((boolean) returnedFile.getDirectory(), attributes.isDirectory());
        assertEquals((long) returnedFile.getSize(), attributes.getSize());
    }

    public void testGetDefaultAbstractDirectoryAttributes() {
        OpenBISSftpFileAttributes attributes = OpenBISListUtil.getDefaultAbstractDirectoryAttributes();
        assertTrue(attributes.isDirectory());
        assertFalse(attributes.isSymbolicLink());
        assertFalse(attributes.isRegularFile());
        assertFalse(attributes.isOther());
        assertEquals(EnumSet.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_EXECUTE
        ), attributes.getPermissions());
        assertEquals(0L, attributes.size());
        assertTrue(attributes.getModifiedTime().toInstant().isAfter(Instant.now().minusMillis(60000)));
        assertTrue(attributes.getCreationTime().toInstant().isAfter(Instant.now().minusMillis(60000)));
        assertTrue(attributes.getAccessTime().toInstant().isAfter(Instant.now().minusMillis(60000)));
        assertTrue(attributes.getModifiedTime().toInstant().isBefore(Instant.now().plusMillis(60000)));
        assertTrue(attributes.getCreationTime().toInstant().isBefore(Instant.now().plusMillis(60000)));
        assertTrue(attributes.getAccessTime().toInstant().isBefore(Instant.now().plusMillis(60000)));
    }
}