package ch.ethz.sis.afssftp.util;

import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afssftp.authentication.User;
import ch.ethz.sis.afssftp.filesystemview.SftpFileAttributes;
import ch.ethz.sis.afssftp.filesystemview.SftpNode;
import ch.ethz.sis.afssftp.startup.AfsSftpServerParameter;
import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.fetchoptions.ProjectFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.ProjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SamplePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.fetchoptions.SpaceFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.ISpaceId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.search.SpaceSearchCriteria;
import ch.ethz.sis.shared.log.standard.LogFactory;
import ch.ethz.sis.shared.log.standard.LogFactoryFactory;
import ch.ethz.sis.shared.log.standard.LogManager;
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
        initLogFactory();
    }

    private static void initLogFactory() {
        try {
            LogFactoryFactory logFactoryFactory = new LogFactoryFactory();
            LogFactory logFactory = logFactoryFactory.create("ch.ethz.sis.shared.log.standard.impl.StandardLogFactory");
            logFactory.configure("logging.properties");
            LogManager.setLogFactory(logFactory);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testGetSpaces() {
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        OpenBIS openBIS = Mockito.mock(OpenBIS.class);
        User user = User.builder()
                .username("u5er")
                .password("pWd")
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
                .password("pWd")
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
                .password("pWd")
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
                .password("pWd")
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
                .password("pWd")
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
                .password("pWd")
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
                .password("pWd")
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
                .password("pWd")
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

    public void testGetAfsEntityPermId() {
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        OpenBIS openBIS = Mockito.mock(OpenBIS.class);
        User user = User.builder()
                .username("u5er")
                .password("pWd")
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
                .password("pWd")
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
                .password("pWd")
                .sessionToken("53551on")
                .build();
        OpenBIS.AfsServerFacade afsClientProxyMock = Mockito.mock(
                OpenBIS.AfsServerFacade.class
        );
        Mockito.doReturn(afsClientProxyMock).when(openBISClientUtil).getAfsClient(user);
        SftpListUtil sftpListUtil = new SftpListUtil(user, openBISClientUtil);
        try {
            sftpListUtil.getAfsFilePresence("entity-id", "/dir/file");
        } catch (Exception e) {}
        Mockito.verify(openBISClientUtil, Mockito.times(1)).getAfsClient(user);
    }

    public void testGetDefaultAfsFileAttributes() {
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        User user = User.builder()
                .username("u5er")
                .password("pWd")
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
        SftpFileAttributes attributes = SftpListUtil.getDefaultAbstractDirectoryAttributes();
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
        assertEquals("Exp NAME(EXP-PERM-ID-1)", SftpListUtil.getDisplayName(experiment));

        DataSet dataSet = new DataSet();
        DataSetFetchOptions dataSetFetchOptions = new DataSetFetchOptions();
        dataSetFetchOptions.withProperties();
        dataSet.setFetchOptions(dataSetFetchOptions);
        dataSet.setStringProperty("NAME", "Dataset NAME");
        dataSet.setPermId(new DataSetPermId("dataset-perm-id-1"));
        assertEquals("Dataset NAME(DATASET-PERM-ID-1)", SftpListUtil.getDisplayName(dataSet));

        Sample sample = new Sample();
        SampleFetchOptions sampleFetchOptions = new SampleFetchOptions();
        sampleFetchOptions.withProperties();
        sample.setFetchOptions(sampleFetchOptions);
        sample.setStringProperty("NAME", "Sample NAME");
        sample.setPermId(new SamplePermId("sample-perm-id-1"));
        assertEquals("Sample NAME(SAMPLE-PERM-ID-1)", SftpListUtil.getDisplayName(sample));
    }

    public void testGetSpaceCodeFromDisplayName() {
        assertEquals("space-code-1", SftpListUtil.getSpaceCodeFromDisplayName("space-code-1"));
    }

    public void testGetProjectCodeFromDisplayName() {
        assertEquals("project-code-1", SftpListUtil.getProjectCodeFromDisplayName("project-code-1"));
    }

    public void testGetEntityPermIdFromDisplayName() {
        assertEquals("ENTITY-PERM-ID", SftpListUtil.getEntityPermIdFromDisplayName("naME Surname(ENTITY-PERM-ID)"));
        assertEquals("ENTITY-PERM-ID", SftpListUtil.getEntityPermIdFromDisplayName("(ENTITY-PERM-ID)"));
        assertEquals("ENTITY-PERM-ID", SftpListUtil.getEntityPermIdFromDisplayName("ENTITY-PERM-ID"));
    }
}