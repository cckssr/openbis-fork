package ch.ethz.sis.afssftp.filesystemview.impl.standard;

import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afssftp.filesystemview.FtpPathLister;
import ch.ethz.sis.afssftp.filesystemview.SftpFileAttributes;
import ch.ethz.sis.afssftp.filesystemview.SftpNode;
import ch.ethz.sis.afssftp.filesystemview.SftpNodeChain;
import ch.ethz.sis.afssftp.helpers.TestHelper;
import ch.ethz.sis.afssftp.util.SftpListUtil;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.ProjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SamplePermId;
import junit.framework.TestCase;
import org.mockito.Mockito;

import java.nio.file.NoSuchFileException;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class StandardPathListerTest extends TestCase {
    private static final SftpNodeChain exampleBaseChainUpToSpace= new SftpNodeChain(List.of(
            SftpNode.builder().type(SftpNode.Type.ROOT).build(),
            SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                    .identifier(Optional.of(StandardPathTranslator.SPACE_TYPE_LABEL))
                    .build(),
            SftpNode.builder().type(SftpNode.Type.SPACE)
                    .identifier(Optional.of("space_1")).build()
    ));

    private static final SftpNodeChain exampleBaseChainUpToProject = new SftpNodeChain(List.of(
            SftpNode.builder().type(SftpNode.Type.ROOT).build(),
            SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                    .identifier(Optional.of(StandardPathTranslator.SPACE_TYPE_LABEL))
                    .build(),
            SftpNode.builder().type(SftpNode.Type.SPACE)
                    .identifier(Optional.of("space_1")).build(),
            SftpNode.builder().type(SftpNode.Type.SUBLEVEL).identifier(Optional.of(StandardPathTranslator.PROJECT_TYPE_LABEL)).build(),
            SftpNode.builder().type(SftpNode.Type.PROJECT).identifier(Optional.of("project_1")).build()
    ));

    public void testListWithRoot() throws Exception {
        SftpListUtil listUtil = Mockito.mock(SftpListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));


        SftpNode lastNode = SftpNode.builder().type(SftpNode.Type.ROOT).build();
        SftpNodeChain directory = SftpNodeChain.concat(exampleBaseChainUpToProject, lastNode);
        List<SftpNodeChain> exampleListResult = List.of(
                new SftpNodeChain(List.of(
                        SftpNode.builder().type(SftpNode.Type.ROOT).build(),
                        SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                .identifier(Optional.of(StandardPathTranslator.SPACE_TYPE_LABEL))
                                .build()
                )));
        Mockito.doReturn(exampleListResult).when(standardPathLister).listRoot(null, directory);
        List<SftpNodeChain> listResult = standardPathLister.list(directory);
        Mockito.verify(standardPathLister, Mockito.times(1)).listRoot(null, directory);
        assertEquals(exampleListResult, listResult);
    }

    public void testListWithSpace() throws Exception {
        SftpListUtil listUtil = Mockito.mock(SftpListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));


        SftpNode lastNode = SftpNode.builder().type(SftpNode.Type.SPACE)
                .identifier(Optional.of("space_1")).build();
        SftpNodeChain directory = SftpNodeChain.concat(exampleBaseChainUpToProject, lastNode);
        List<SftpNodeChain> exampleListResult = List.of(
                new SftpNodeChain(List.of(
                        SftpNode.builder().type(SftpNode.Type.ROOT).build(),
                        SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                .identifier(Optional.of(StandardPathTranslator.SPACE_TYPE_LABEL))
                                .build(),
                        SftpNode.builder().type(SftpNode.Type.SPACE)
                                .identifier(Optional.of("space_1"))
                                .build(),
                        SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                .identifier(Optional.of(StandardPathTranslator.PROJECT_TYPE_LABEL))
                                .build()
                )));
        Mockito.doReturn(exampleListResult).when(standardPathLister).listSpace(lastNode, null, directory);
        List<SftpNodeChain> listResult = standardPathLister.list(directory);
        Mockito.verify(standardPathLister, Mockito.times(1)).listSpace(lastNode, null, directory);
        assertEquals(exampleListResult, listResult);
    }

    public void testListWithSample() throws Exception {
        SftpListUtil listUtil = Mockito.mock(SftpListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));


        SftpNode lastNode = SftpNode.builder().type(SftpNode.Type.SAMPLE)
                .identifier(Optional.of("sample_1")).build();
        SftpNodeChain directory = SftpNodeChain.concat(exampleBaseChainUpToProject, lastNode);
        List<SftpNodeChain> exampleListResult = List.of(
                new SftpNodeChain(List.of(
                        SftpNode.builder().type(SftpNode.Type.ROOT).build(),
                        SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                .identifier(Optional.of(StandardPathTranslator.SAMPLE_TYPE_LABEL))
                                .build(),
                        SftpNode.builder().type(SftpNode.Type.SAMPLE)
                                .identifier(Optional.of("sample_1"))
                                .build(),
                        SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                .identifier(Optional.of(StandardPathTranslator.DATA_SET_TYPE_LABEL))
                                .build()
                )));
        Mockito.doReturn(exampleListResult).when(standardPathLister).listSample(lastNode, null, directory);
        List<SftpNodeChain> listResult = standardPathLister.list(directory);
        Mockito.verify(standardPathLister, Mockito.times(1)).listSample(lastNode, null, directory);
        assertEquals(exampleListResult, listResult);
    }

    public void testListWithFolder() throws Exception {
        SftpListUtil listUtil = Mockito.mock(SftpListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));


        SftpNode lastNode = SftpNode.builder().type(SftpNode.Type.FOLDER)
                .identifier(Optional.of("folder_1")).build();
        SftpNodeChain directory = SftpNodeChain.concat(exampleBaseChainUpToProject, lastNode);
        List<SftpNodeChain> exampleListResult = List.of(
                new SftpNodeChain(List.of(
                        SftpNode.builder().type(SftpNode.Type.ROOT).build(),
                        SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                .identifier(Optional.of(StandardPathTranslator.FOLDER_TYPE_LABEL))
                                .build(),
                        SftpNode.builder().type(SftpNode.Type.FOLDER)
                                .identifier(Optional.of("folder_1"))
                                .build(),
                        SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                .identifier(Optional.of(StandardPathTranslator.SAMPLE_TYPE_LABEL))
                                .build()
                )));
        Mockito.doReturn(exampleListResult).when(standardPathLister).listFolder(lastNode, null, directory);
        List<SftpNodeChain> listResult = standardPathLister.list(directory);
        Mockito.verify(standardPathLister, Mockito.times(1)).listFolder(lastNode, null, directory);
        assertEquals(exampleListResult, listResult);
    }

    public void testListWithDataset() throws Exception {
        SftpListUtil listUtil = Mockito.mock(SftpListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));


        SftpNode lastNode = SftpNode.builder().type(SftpNode.Type.DATA_SET)
                .identifier(Optional.of("dataset_1")).build();
        SftpNodeChain directory = SftpNodeChain.concat(exampleBaseChainUpToProject, lastNode);
        List<SftpNodeChain> exampleListResult = List.of(
                new SftpNodeChain(List.of(
                        SftpNode.builder().type(SftpNode.Type.ROOT).build(),
                        SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                .identifier(Optional.of(StandardPathTranslator.DATA_SET_TYPE_LABEL))
                                .build(),
                        SftpNode.builder().type(SftpNode.Type.DATA_SET)
                                .identifier(Optional.of("dataset_1"))
                                .build(),
                        SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                .identifier(Optional.of(StandardPathTranslator.FILE_TYPE_LABEL))
                                .build()
                )));
        Mockito.doReturn(exampleListResult).when(standardPathLister).listDataSet(lastNode, null, directory);
        List<SftpNodeChain> listResult = standardPathLister.list(directory);
        Mockito.verify(standardPathLister, Mockito.times(1)).listDataSet(lastNode, null, directory);
        assertEquals(exampleListResult, listResult);
    }

    public void testListWithProject() throws Exception {
        SftpListUtil listUtil = Mockito.mock(SftpListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));


        SftpNode lastNode = SftpNode.builder().type(SftpNode.Type.PROJECT)
                .identifier(Optional.of("project_1")).build();
        SftpNodeChain directory = SftpNodeChain.concat(exampleBaseChainUpToProject, lastNode);
        List<SftpNodeChain> exampleListResult = List.of(
                new SftpNodeChain(List.of(
                        SftpNode.builder().type(SftpNode.Type.ROOT).build(),
                        SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                .identifier(Optional.of(StandardPathTranslator.PROJECT_TYPE_LABEL))
                                .build(),
                        SftpNode.builder().type(SftpNode.Type.PROJECT)
                                .identifier(Optional.of("project_1"))
                                .build(),
                        SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                .identifier(Optional.of(StandardPathTranslator.EXPERIMENT_TYPE_LABEL))
                                .build()
                )));
        Mockito.doReturn(exampleListResult).when(standardPathLister).listProject(lastNode, null, directory);
        List<SftpNodeChain> listResult = standardPathLister.list(directory);
        Mockito.verify(standardPathLister, Mockito.times(1)).listProject(lastNode, null, directory);
        assertEquals(exampleListResult, listResult);
    }

    public void testListWithExperiment() throws Exception {
        SftpListUtil listUtil = Mockito.mock(SftpListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));


        SftpNode lastNode = SftpNode.builder().type(SftpNode.Type.EXPERIMENT)
                .identifier(Optional.of("experiment_1")).build();
        SftpNodeChain directory = SftpNodeChain.concat(exampleBaseChainUpToProject, lastNode);
        List<SftpNodeChain> exampleListResult = List.of(
                new SftpNodeChain(List.of(
                        SftpNode.builder().type(SftpNode.Type.ROOT).build(),
                        SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                .identifier(Optional.of(StandardPathTranslator.EXPERIMENT_TYPE_LABEL))
                                .build(),
                        SftpNode.builder().type(SftpNode.Type.EXPERIMENT)
                                .identifier(Optional.of("experiment_1"))
                                .build(),
                        SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                .identifier(Optional.of(StandardPathTranslator.SAMPLE_TYPE_LABEL))
                                .build()
                )));
        Mockito.doReturn(exampleListResult).when(standardPathLister).listExperiment(lastNode, null, directory);
        List<SftpNodeChain> listResult = standardPathLister.list(directory);
        Mockito.verify(standardPathLister, Mockito.times(1)).listExperiment(lastNode, null, directory);
        assertEquals(exampleListResult, listResult);
    }

    public void testListWithAfsFile() throws Exception {
        SftpListUtil listUtil = Mockito.mock(SftpListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));


        SftpNode lastNode = SftpNode.builder().type(SftpNode.Type.AFS_FILE)
                .afsFilePath(List.of("dir1")).build();
        SftpNodeChain directory = SftpNodeChain.concat(exampleBaseChainUpToProject, lastNode);
        List<SftpNodeChain> exampleListResult = List.of(
                new SftpNodeChain(List.of(
                        SftpNode.builder().type(SftpNode.Type.ROOT).build(),
                        SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                .identifier(Optional.of(StandardPathTranslator.FILE_TYPE_LABEL))
                                .build(),
                        SftpNode.builder().type(SftpNode.Type.AFS_FILE)
                                .afsFilePath(List.of("dir1", "file2")).build()
                )));
        Mockito.doReturn(exampleListResult).when(standardPathLister).listFilesInAfsFileNode(lastNode, directory);
        List<SftpNodeChain> listResult = standardPathLister.list(directory);
        Mockito.verify(standardPathLister, Mockito.times(1)).listFilesInAfsFileNode(lastNode, directory);
        assertEquals(exampleListResult, listResult);
    }


    public void testListWithSublevel() throws Exception {
        SftpListUtil listUtil = Mockito.mock(SftpListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));


        SftpNode lastNode = SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                .identifier(Optional.of("sublevel-label")).build();
        SftpNode secondLastNode;
        SftpNodeChain directory;
        List<SftpNodeChain> listResult;
        Exception exception;
        List<SftpNodeChain> exampleListResult = List.of(
                new SftpNodeChain(List.of(
                        SftpNode.builder().type(SftpNode.Type.ROOT).build(),
                        SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                .identifier(Optional.of(StandardPathTranslator.FILE_TYPE_LABEL))
                                .build(),
                        SftpNode.builder().type(SftpNode.Type.AFS_FILE)
                                .afsFilePath(List.of("dir1", "file2")).build()
                )));

        //Sublevel of ROOT
        secondLastNode = SftpNode.builder().type(SftpNode.Type.ROOT).build();
        directory = SftpNodeChain.concat(
                exampleBaseChainUpToProject,
                new SftpNodeChain(List.of(secondLastNode, lastNode))
        );
        Mockito.doReturn(exampleListResult).when(standardPathLister).listRoot("sublevel-label", directory);
        listResult = standardPathLister.list(directory);
        Mockito.verify(standardPathLister, Mockito.times(1)).listRoot("sublevel-label", directory);
        assertEquals(exampleListResult, listResult);
        Mockito.clearInvocations(standardPathLister);

        //Sublevel of SPACE
        secondLastNode = SftpNode.builder().type(SftpNode.Type.SPACE)
                .identifier(Optional.of("space_1")).build();
        directory = SftpNodeChain.concat(
                exampleBaseChainUpToProject,
                new SftpNodeChain(List.of(secondLastNode, lastNode))
        );
        Mockito.doReturn(exampleListResult).when(standardPathLister).listSpace(secondLastNode, "sublevel-label", directory);
        listResult = standardPathLister.list(directory);
        Mockito.verify(standardPathLister, Mockito.times(1)).listSpace(secondLastNode, "sublevel-label", directory);
        assertEquals(exampleListResult, listResult);
        Mockito.clearInvocations(standardPathLister);

        //Sublevel of PROJECT
        secondLastNode = SftpNode.builder().type(SftpNode.Type.PROJECT)
                .identifier(Optional.of("project_1")).build();
        directory = SftpNodeChain.concat(
                exampleBaseChainUpToProject,
                new SftpNodeChain(List.of(secondLastNode, lastNode))
        );
        Mockito.doReturn(exampleListResult).when(standardPathLister).listProject(secondLastNode, "sublevel-label", directory);
        listResult = standardPathLister.list(directory);
        Mockito.verify(standardPathLister, Mockito.times(1)).listProject(secondLastNode, "sublevel-label", directory);
        assertEquals(exampleListResult, listResult);
        Mockito.clearInvocations(standardPathLister);

        //Sublevel of EXPERIMENT
        secondLastNode = SftpNode.builder().type(SftpNode.Type.EXPERIMENT)
                .identifier(Optional.of("experiment_1")).build();
        directory = SftpNodeChain.concat(
                exampleBaseChainUpToProject,
                new SftpNodeChain(List.of(secondLastNode, lastNode))
        );
        Mockito.doReturn(exampleListResult).when(standardPathLister).listExperiment(secondLastNode, "sublevel-label", directory);
        listResult = standardPathLister.list(directory);
        Mockito.verify(standardPathLister, Mockito.times(1)).listExperiment(secondLastNode, "sublevel-label", directory);
        assertEquals(exampleListResult, listResult);
        Mockito.clearInvocations(standardPathLister);

        //Sublevel of FOLDER
        secondLastNode = SftpNode.builder().type(SftpNode.Type.FOLDER)
                .identifier(Optional.of("folder_1")).build();
        directory = SftpNodeChain.concat(
                exampleBaseChainUpToProject,
                new SftpNodeChain(List.of(secondLastNode, lastNode))
        );
        Mockito.doReturn(exampleListResult).when(standardPathLister).listFolder(secondLastNode, "sublevel-label", directory);
        listResult = standardPathLister.list(directory);
        Mockito.verify(standardPathLister, Mockito.times(1)).listFolder(secondLastNode, "sublevel-label", directory);
        assertEquals(exampleListResult, listResult);
        Mockito.clearInvocations(standardPathLister);

        //Sublevel of SAMPLE
        secondLastNode = SftpNode.builder().type(SftpNode.Type.SAMPLE)
                .identifier(Optional.of("sample_1")).build();
        directory = SftpNodeChain.concat(
                exampleBaseChainUpToProject,
                new SftpNodeChain(List.of(secondLastNode, lastNode))
        );
        Mockito.doReturn(exampleListResult).when(standardPathLister).listSample(secondLastNode, "sublevel-label", directory);
        listResult = standardPathLister.list(directory);
        Mockito.verify(standardPathLister, Mockito.times(1)).listSample(secondLastNode, "sublevel-label", directory);
        assertEquals(exampleListResult, listResult);
        Mockito.clearInvocations(standardPathLister);

        //Sublevel of DATASET
        secondLastNode = SftpNode.builder().type(SftpNode.Type.DATA_SET)
                .identifier(Optional.of("dataset_1")).build();
        directory = SftpNodeChain.concat(
                exampleBaseChainUpToProject,
                new SftpNodeChain(List.of(secondLastNode, lastNode))
        );
        Mockito.doReturn(exampleListResult).when(standardPathLister).listDataSet(secondLastNode, "sublevel-label", directory);
        listResult = standardPathLister.list(directory);
        Mockito.verify(standardPathLister, Mockito.times(1)).listDataSet(secondLastNode, "sublevel-label", directory);
        assertEquals(exampleListResult, listResult);
        Mockito.clearInvocations(standardPathLister);

        //Sublevel of SUBLEVEL: not allowed
        secondLastNode = SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                .identifier(Optional.of("other")).build();
        directory = SftpNodeChain.concat(
                exampleBaseChainUpToProject,
                new SftpNodeChain(List.of(secondLastNode, lastNode))
        );
        exception = null;
        try {
            standardPathLister.list(directory);
        } catch (Exception e) {
            exception = e;
        }
        assertEquals(IllegalArgumentException.class, exception.getClass());
        Mockito.clearInvocations(standardPathLister);

        //Sublevel of AFS_FILE: not allowed
        secondLastNode = SftpNode.builder().type(SftpNode.Type.AFS_FILE)
                .afsFilePath(List.of("dir1", "file2")).build();
        directory = SftpNodeChain.concat(
                exampleBaseChainUpToProject,
                new SftpNodeChain(List.of(secondLastNode, lastNode))
        );
        exception = null;
        try {
            standardPathLister.list(directory);
        } catch (Exception e) {
            exception = e;
        }
        assertEquals(IllegalArgumentException.class, exception.getClass());
        Mockito.clearInvocations(standardPathLister);

        //Sublevel of nothing: not allowed
        directory = new SftpNodeChain(Collections.singletonList(lastNode));
        exception = null;
        try {
            standardPathLister.list(directory);
        } catch (Exception e) {
            exception = e;
        }
        assertEquals(IllegalArgumentException.class, exception.getClass());
        Mockito.clearInvocations(standardPathLister);
    }

    public void testToEntityDescriptor() throws Exception {
        SftpListUtil listUtil = Mockito.mock(SftpListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));
        SftpListUtil.EntityBasicInfo mockBasicInfo = new SftpListUtil.EntityBasicInfo(
                true,
                15235235L,
                14143132L,
                true
        );
        Mockito.doReturn(mockBasicInfo).when(listUtil).checkExistence(
                Mockito.any(),
                Mockito.any()
        );

        // Empty case
        SftpNodeChain emptyChain = new SftpNodeChain(Collections.emptyList());
        assertTrue(standardPathLister.toEntityDescriptor(emptyChain).isEmpty());

        // ROOT case
        SftpNodeChain rootChain = new SftpNodeChain(Collections.singletonList(TestHelper.createRandomNodeOfType(SftpNode.Type.ROOT)));
        assertTrue(standardPathLister.toEntityDescriptor(rootChain).isEmpty());

        // SPACE case
        SftpNodeChain spaceChain = exampleBaseChainUpToSpace;
        FtpPathLister.EntityDescriptor spaceEntityDescriptor = standardPathLister.toEntityDescriptor(spaceChain).get();
        assertEquals(SftpNode.Type.SPACE, spaceEntityDescriptor.type());
        assertEquals(spaceChain.getLast().get().getIdentifier(), spaceEntityDescriptor.identifier());
        assertEquals(spaceChain.getLast().get().getIdentifier(), spaceEntityDescriptor.spaceCode());
        assertEquals(Optional.empty(), spaceEntityDescriptor.projectCode());
        assertEquals(Optional.empty(), spaceEntityDescriptor.experimentId());
        assertEquals(Optional.empty(), spaceEntityDescriptor.parentSampleId());
        assertEquals(Optional.empty(), spaceEntityDescriptor.name());
        assertEquals(mockBasicInfo, spaceEntityDescriptor.entityBasicInfo());

        // PROJECT case
        SftpNodeChain projectChain =exampleBaseChainUpToProject;
        FtpPathLister.EntityDescriptor projectEntityDescriptor = standardPathLister.toEntityDescriptor(projectChain).get();
        assertEquals(SftpNode.Type.PROJECT, projectEntityDescriptor.type());
        assertEquals(
                new ProjectIdentifier(
                        projectChain.lookUpSpaceCode(),
                        projectChain.getLast().get().getIdentifier().get()
                ).getIdentifier(),
                projectEntityDescriptor.identifier().get());
        assertEquals(Optional.of("space_1"), projectEntityDescriptor.spaceCode());
        assertEquals(projectChain.getLast().get().getIdentifier(), projectEntityDescriptor.projectCode());
        assertEquals(Optional.empty(), projectEntityDescriptor.experimentId());
        assertEquals(Optional.empty(), projectEntityDescriptor.parentSampleId());
        assertEquals(Optional.empty(), projectEntityDescriptor.name());
        assertEquals(mockBasicInfo, projectEntityDescriptor.entityBasicInfo());

        // EXPERIMENT case and AFS files under that
        SftpNodeChain experimentChain = SftpNodeChain.concat(exampleBaseChainUpToProject,
                new SftpNodeChain(List.of(
                    SftpNodeChain.createSublevelNode(StandardPathTranslator.EXPERIMENT_TYPE_LABEL),
                    TestHelper.createRandomNodeOfType(SftpNode.Type.EXPERIMENT).toBuilder()
                                .identifier(Optional.of("fake_name (fake-perm-id)"))
                                .build()
                )
            )
        );

        FtpPathLister.EntityDescriptor entityDescriptor = standardPathLister.toEntityDescriptor(experimentChain).get();
        assertEquals(SftpNode.Type.EXPERIMENT, entityDescriptor.type());
        assertEquals(
                "fake-perm-id",
                entityDescriptor.identifier().get());
        assertEquals(
                "fake_name",
                entityDescriptor.name().get());
        assertEquals(
                "space_1",
                entityDescriptor.spaceCode().get());
        assertEquals(
                "project_1",
                entityDescriptor.projectCode().get());
        assertEquals(mockBasicInfo, spaceEntityDescriptor.entityBasicInfo());

        SftpNodeChain afsEntityChain = SftpNodeChain.concat(experimentChain,
                new SftpNodeChain(
                        List.of(
                                TestHelper.createRandomNodeOfType(SftpNode.Type.SUBLEVEL).toBuilder()
                                        .identifier(Optional.of(StandardPathTranslator.FILE_TYPE_LABEL))
                                        .build(),
                                TestHelper.createRandomNodeOfType(SftpNode.Type.AFS_FILE).toBuilder()
                                        .afsFilePath(List.of("dir0", "dir1", "file2.txt"))
                                        .build()
                        )
                )
        );

        FtpPathLister.EntityDescriptor afsEntityDescriptor = standardPathLister.toEntityDescriptor(afsEntityChain).get();
        assertEquals(SftpNode.Type.AFS_FILE, afsEntityDescriptor.type());
        assertEquals("/dir0/dir1/file2.txt", afsEntityDescriptor.afsPath());
        assertEquals(SftpNode.Type.EXPERIMENT, afsEntityDescriptor.afsEntity().type());
        assertEquals(
                "fake-perm-id",
                afsEntityDescriptor.afsEntity().identifier().get());
        assertEquals(mockBasicInfo, spaceEntityDescriptor.entityBasicInfo());

        // FOLDER, SAMPLE cases and AFS files under them
        for (SftpNode.Type type : List.of(
                SftpNode.Type.FOLDER,
                SftpNode.Type.SAMPLE,
                SftpNode.Type.DATA_SET)
        ) {
            for (boolean withProject : List.of(false, true)) {
                for (boolean withExperiment : List.of(false, true)) {
                    for (boolean withParentSample : List.of(false, true)) {
                        SftpNodeChain baseChain;

                        if (withExperiment) {
                            baseChain = exampleBaseChainUpToProject;
                            baseChain = SftpNodeChain.concat(
                                    baseChain,
                                    new SftpNodeChain(List.of(
                                            SftpNodeChain.createSublevelNode(StandardPathTranslator.EXPERIMENT_TYPE_LABEL),
                                            TestHelper.createRandomNodeOfType(SftpNode.Type.EXPERIMENT).toBuilder().identifier(Optional.of("exp_name (experiment_1)")).build()
                                    ))
                            );
                        } else if (withProject) {
                            baseChain = exampleBaseChainUpToProject;
                        } else {
                            baseChain = exampleBaseChainUpToSpace;
                        }

                        if (withParentSample) {
                            baseChain = SftpNodeChain.concat(
                                    baseChain,
                                    new SftpNodeChain(List.of(
                                            SftpNodeChain.createSublevelNode(StandardPathTranslator.SAMPLE_TYPE_LABEL),
                                            TestHelper.createRandomNodeOfType(SftpNode.Type.SAMPLE).toBuilder().identifier(Optional.of("parent_sample_name (parent_sample_1)")).build()
                                    ))
                            );
                        }

                        SftpNodeChain entityChain = SftpNodeChain.concat(baseChain,
                                new SftpNodeChain(List.of(
                                        SftpNodeChain.createSublevelNode(
                                                type == SftpNode.Type.SAMPLE ?
                                                        StandardPathTranslator.SAMPLE_TYPE_LABEL : StandardPathTranslator.FOLDER_TYPE_LABEL),
                                        TestHelper.createRandomNodeOfType(type).toBuilder()
                                                .identifier(Optional.of("fake_name (fake-perm-id)"))
                                                .build()
                                )));

                        FtpPathLister.EntityDescriptor entityDescriptor1 = standardPathLister.toEntityDescriptor(entityChain).get();
                        assertEquals(type, entityDescriptor1.type());
                        assertEquals(
                                "fake-perm-id",
                                entityDescriptor1.identifier().get());
                        assertEquals(
                                "fake_name",
                                entityDescriptor1.name().get());
                        assertEquals(
                                "space_1",
                                entityDescriptor1.spaceCode().get());
                        assertEquals(
                                withProject || withExperiment ? "project_1" : null,
                                entityDescriptor1.projectCode().orElse(null));
                        assertEquals(
                                withExperiment ? "experiment_1" : null,
                                entityDescriptor1.experimentId().orElse(null));
                        assertEquals(
                                withParentSample ? "parent_sample_1" : null,
                                entityDescriptor1.parentSampleId().orElse(null));
                        assertEquals(mockBasicInfo, spaceEntityDescriptor.entityBasicInfo());

                        SftpNodeChain afsEntityChain1 = SftpNodeChain.concat(entityChain,
                                new SftpNodeChain(
                                        List.of(
                                                TestHelper.createRandomNodeOfType(SftpNode.Type.SUBLEVEL).toBuilder()
                                                        .identifier(Optional.of(StandardPathTranslator.FILE_TYPE_LABEL))
                                                        .build(),
                                                TestHelper.createRandomNodeOfType(SftpNode.Type.AFS_FILE).toBuilder()
                                                        .afsFilePath(List.of("dir0", "dir1", "file2.txt"))
                                                        .build()
                                        )
                                )
                        );

                        FtpPathLister.EntityDescriptor afsEntityDescriptor1 = standardPathLister.toEntityDescriptor(afsEntityChain1).get();
                        assertEquals(SftpNode.Type.AFS_FILE, afsEntityDescriptor1.type());
                        assertEquals("/dir0/dir1/file2.txt", afsEntityDescriptor1.afsPath());
                        assertEquals(type, afsEntityDescriptor1.afsEntity().type());
                        assertEquals(
                                "fake-perm-id",
                                afsEntityDescriptor1.afsEntity().identifier().get());
                        assertEquals(mockBasicInfo, spaceEntityDescriptor.entityBasicInfo());
                    }
                }
            }
        }

        // DATA_SET case and AFS files under that
        for (boolean withExperiment : List.of(false, true)) {
            for (boolean withParentSample : List.of(false, true)) {
                SftpNodeChain baseChain;

                if (withExperiment) {
                    baseChain = exampleBaseChainUpToProject;
                    baseChain = SftpNodeChain.concat(
                            baseChain,
                            new SftpNodeChain(List.of(
                                    SftpNodeChain.createSublevelNode(StandardPathTranslator.EXPERIMENT_TYPE_LABEL),
                                    TestHelper.createRandomNodeOfType(SftpNode.Type.EXPERIMENT).toBuilder().identifier(Optional.of("exp_name (experiment_1)")).build()
                            ))
                    );
                } else {
                    baseChain = exampleBaseChainUpToSpace;
                }

                if (withParentSample || !withExperiment) {
                    baseChain = SftpNodeChain.concat(
                            baseChain,
                            new SftpNodeChain(List.of(
                                    SftpNodeChain.createSublevelNode(StandardPathTranslator.SAMPLE_TYPE_LABEL),
                                    TestHelper.createRandomNodeOfType(SftpNode.Type.SAMPLE).toBuilder().identifier(Optional.of("parent_sample_name (parent_sample_1)")).build()
                            ))
                    );
                }

                SftpNodeChain entityChain = SftpNodeChain.concat(baseChain,
                        new SftpNodeChain(List.of(
                                SftpNodeChain.createSublevelNode(StandardPathTranslator.DATA_SET_TYPE_LABEL),
                                TestHelper.createRandomNodeOfType(SftpNode.Type.DATA_SET).toBuilder()
                                        .identifier(Optional.of("fake_name (fake-perm-id)"))
                                        .build()
                        )));

                FtpPathLister.EntityDescriptor entityDescriptor2 = standardPathLister.toEntityDescriptor(entityChain).get();
                assertEquals(SftpNode.Type.DATA_SET, entityDescriptor2.type());
                assertEquals(
                        "fake-perm-id",
                        entityDescriptor2.identifier().get());
                assertEquals(
                        "fake_name",
                        entityDescriptor2.name().get());
                assertEquals(
                        "space_1",
                        entityDescriptor2.spaceCode().get());
                assertEquals(
                        withExperiment ? "project_1" : null,
                        entityDescriptor2.projectCode().orElse(null));
                assertEquals(
                        withExperiment ? "experiment_1" : null,
                        entityDescriptor2.experimentId().orElse(null));
                assertEquals(
                        withParentSample || !withExperiment ? "parent_sample_1" : null,
                        entityDescriptor2.parentSampleId().orElse(null));
                assertEquals(mockBasicInfo, spaceEntityDescriptor.entityBasicInfo());

                SftpNodeChain afsEntityChain2 = SftpNodeChain.concat(entityChain,
                        new SftpNodeChain(
                                List.of(
                                        TestHelper.createRandomNodeOfType(SftpNode.Type.SUBLEVEL).toBuilder()
                                                .identifier(Optional.of(StandardPathTranslator.FILE_TYPE_LABEL))
                                                .build(),
                                        TestHelper.createRandomNodeOfType(SftpNode.Type.AFS_FILE).toBuilder()
                                                .afsFilePath(List.of("dir0", "dir1", "file2.txt"))
                                                .build()
                                )
                        )
                );

                FtpPathLister.EntityDescriptor afsEntityDescriptor2 = standardPathLister.toEntityDescriptor(afsEntityChain2).get();
                assertEquals(SftpNode.Type.AFS_FILE, afsEntityDescriptor2.type());
                assertEquals("/dir0/dir1/file2.txt", afsEntityDescriptor2.afsPath());
                assertEquals(SftpNode.Type.DATA_SET, afsEntityDescriptor2.afsEntity().type());
                assertEquals(
                        "fake-perm-id",
                        afsEntityDescriptor2.afsEntity().identifier().get());
                assertEquals(mockBasicInfo, spaceEntityDescriptor.entityBasicInfo());
            }
        }

        // SUBLEVEL not "files" under EXPERIMENT, FOLDER, SAMPLE, DATA_SET cases
        for (SftpNode.Type type : List.of(
                SftpNode.Type.EXPERIMENT,
                SftpNode.Type.FOLDER,
                SftpNode.Type.SAMPLE,
                SftpNode.Type.DATA_SET)
        ) {
            SftpNodeChain entityChain = SftpNodeChain.concat(exampleBaseChainUpToProject,
                    new SftpNodeChain( List.of(
                            TestHelper.createRandomNodeOfType(type).toBuilder()
                                .identifier(Optional.of("fake_name (fake-perm-id)"))
                                .build(),
                            TestHelper.createRandomNodeOfType(SftpNode.Type.SUBLEVEL)
                        )
                    )
            );

            assertEquals(Optional.empty(), standardPathLister.toEntityDescriptor(entityChain));
        }

        // SUBLEVEL "files" under EXPERIMENT, FOLDER, SAMPLE, DATA_SET cases
        for (SftpNode.Type type : List.of(
                SftpNode.Type.EXPERIMENT,
                SftpNode.Type.FOLDER,
                SftpNode.Type.SAMPLE,
                SftpNode.Type.DATA_SET)
        ) {
            SftpNodeChain entityChain = SftpNodeChain.concat(exampleBaseChainUpToProject,
                    new SftpNodeChain(List.of(
                            TestHelper.createRandomNodeOfType(type).toBuilder()
                                    .identifier(Optional.of("fake_name (fake-perm-id)"))
                                    .build(),
                            TestHelper.createRandomNodeOfType(SftpNode.Type.SUBLEVEL).toBuilder()
                                    .identifier(Optional.of(StandardPathTranslator.FILE_TYPE_LABEL))
                                    .build()
                    )
                    )
            );

            FtpPathLister.EntityDescriptor entityDescriptor3 = standardPathLister.toEntityDescriptor(entityChain).get();
            assertEquals(SftpNode.Type.AFS_FILE, entityDescriptor3.type());
            assertEquals("/", entityDescriptor3.afsPath());
            assertEquals(type, entityDescriptor3.afsEntity().type());
            assertEquals(
                    "fake-perm-id",
                    entityDescriptor3.afsEntity().identifier().get());
            assertEquals(mockBasicInfo, spaceEntityDescriptor.entityBasicInfo());
        }
    }

    public void testReadAttributes() throws Exception {
        SftpListUtil listUtil = Mockito.mock(SftpListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));

        // Abstract directory type ROOT
            SftpNodeChain rootChain = SftpNodeChain.createRoot();
            SftpFileAttributes readAttributesForRoot = standardPathLister.readAttributes(rootChain);
            assertTrue(readAttributesForRoot.isDirectory());
            assertFalse(readAttributesForRoot.isRegularFile());
            assertFalse(readAttributesForRoot.isSymbolicLink());
            assertFalse(readAttributesForRoot.isOther());
            assertEquals(
                    SftpListUtil.getDefaultAbstractDirectoryAttributes(false, null, null).getPermissions(),
                    readAttributesForRoot.getPermissions()
            );

        // Abstract directory type SUBLEVEL
        for (boolean parentEntityExists : List.of(false, true)) {
            Mockito.reset(listUtil);
            Mockito.doReturn( new SftpListUtil.EntityBasicInfo(parentEntityExists, 4000L, 50000L, false) )
                    .when(listUtil).checkExistence(Mockito.any(), Mockito.eq(Optional.of(
                                new ProjectIdentifier(
                                        exampleBaseChainUpToProject.lookUpSpaceCode(),
                                        exampleBaseChainUpToProject.lookUpProjectCode()
                                ).toString()
                            )
                        )
                    );
            SftpNodeChain sublevelTypeChain = SftpNodeChain.concat(
                    exampleBaseChainUpToProject,
                    SftpNode.builder()
                            .type(SftpNode.Type.SUBLEVEL)
                            .identifier(Optional.of("id-fake"))
                            .build()
            );

            SftpFileAttributes readAttributes = null;
            Exception exception = null;
            try {
                readAttributes = standardPathLister.readAttributes(sublevelTypeChain);
            } catch (Exception e) {
                exception = e;
            }
            if (parentEntityExists) {
                assertNull(exception);
                assertTrue(readAttributes.isDirectory());
                assertFalse(readAttributes.isRegularFile());
                assertFalse(readAttributes.isSymbolicLink());
                assertFalse(readAttributes.isOther());
                assertEquals(
                        SftpListUtil.getDefaultAbstractDirectoryAttributes(true, null, null).getPermissions(),
                        readAttributes.getPermissions()
                );
                assertEquals(FileTime.fromMillis(4000), readAttributes.getCreationTime());
                assertEquals(FileTime.fromMillis(50000), readAttributes.getModifiedTime());
                assertEquals(FileTime.fromMillis(50000), readAttributes.getAccessTime());
            } else {
                assertNull(readAttributes);
                assertEquals(NoSuchFileException.class, exception.getClass());
            }
        }

        // Abstract directory types: entities
        List<SftpNode.Type> abstractDirectoryTypesForEntities = List.of(
                SftpNode.Type.SPACE,
                SftpNode.Type.PROJECT,
                SftpNode.Type.EXPERIMENT,
                SftpNode.Type.FOLDER,
                SftpNode.Type.SAMPLE,
                SftpNode.Type.DATA_SET
        );
        for (SftpNode.Type type : abstractDirectoryTypesForEntities) {
            for (boolean exists: List.of(false, true)) {
                for (boolean mutable: List.of(false, true)) {
                    SftpNodeChain chain = SftpNodeChain.concat(
                            exampleBaseChainUpToProject,
                            SftpNode.builder()
                                    .type(type)
                                    .identifier(
                                            Optional.of(type == SftpNode.Type.SPACE || type == SftpNode.Type.PROJECT ?
                                                "id-fake" : "(id-fake)")
                                    )
                                    .build()
                    );
                    Mockito.reset(listUtil);
                    Mockito.doReturn(new SftpListUtil.EntityBasicInfo(exists, 4000L, 50000L, mutable))
                            .when(listUtil).checkExistence(
                                    Mockito.eq(type),
                                    Mockito.argThat(
                                            argument -> argument.isPresent() && argument.get().toLowerCase().endsWith("id-fake")
                                    )
                            );

                    SftpFileAttributes readAttributes = null;
                    Exception exception = null;
                    try {
                        readAttributes = standardPathLister.readAttributes(chain);
                    } catch (Exception e) {
                        exception = e;
                    }

                    if (exists) {
                        assertNull(exception);
                        assertTrue(readAttributes.isDirectory());
                        assertFalse(readAttributes.isRegularFile());
                        assertFalse(readAttributes.isSymbolicLink());
                        assertFalse(readAttributes.isOther());
                        assertEquals(
                                SftpListUtil.getDefaultAbstractDirectoryAttributes(false, null, null).getPermissions(),
                                readAttributes.getPermissions()
                        );
                        assertEquals(FileTime.fromMillis(4000), readAttributes.getCreationTime());
                        assertEquals(FileTime.fromMillis(50000), readAttributes.getModifiedTime());
                        assertEquals(FileTime.fromMillis(50000), readAttributes.getAccessTime());
                    } else {
                        assertNull(readAttributes);
                        assertEquals(NoSuchFileException.class, exception.getClass());
                    }
                }
            }
        }

        // AFS cases
        // Non-root
        SftpNodeChain chain1 = Mockito.spy(SftpNodeChain.concat(
                exampleBaseChainUpToProject,
                SftpNode.builder()
                        .type(SftpNode.Type.AFS_FILE)
                        .afsFilePath(List.of("dir-1", "dir-2", "file-3"))
                        .build()
        ));
        for (boolean afsEntityExists: List.of(false, true)) {
            for (boolean mutable : List.of(false, true)) {
                String afsFilePath = "/dir-1/dir-2/file-3";
                for (SftpFileAttributes sampleAttributes : new SftpFileAttributes[]{
                        SftpListUtil.getDefaultAbstractDirectoryAttributes(false, null, null)
                        , null
                }) {
                    String permId = "12345-12345";
                    FtpPathLister.EntityDescriptor entityDescriptor = new FtpPathLister.EntityDescriptor(
                            SftpNode.Type.AFS_FILE,
                            Optional.empty(),
                            Optional.empty(),
                            Optional.empty(),
                            Optional.empty(),
                            Optional.empty(),
                            Optional.empty(),
                            new FtpPathLister.EntityDescriptor(SftpNode.Type.SAMPLE,
                                    Optional.of("space_1"),
                                    Optional.empty(),
                                    Optional.empty(),
                                    Optional.empty(),
                                    Optional.of(permId),
                                    Optional.empty(),
                                    null,
                                    null,
                                    new SftpListUtil.EntityBasicInfo(afsEntityExists, 4000L, 50000L, mutable)),
                            afsFilePath,
                            null
                    );
                    Mockito.doReturn(Optional.of(entityDescriptor)).when(standardPathLister).toEntityDescriptor(chain1);
                    Mockito.reset(listUtil);
                    Mockito.doReturn(Optional.ofNullable(sampleAttributes)).when(listUtil).getDefaultAfsFileAttributes(
                            permId, afsFilePath, mutable
                    );

                    Exception exception = null;
                    SftpFileAttributes readAttributes = null;
                    try {
                        readAttributes = standardPathLister.readAttributes(chain1);
                    } catch (Exception e) {
                        exception = e;
                    }

                    if (afsEntityExists && sampleAttributes != null) {
                        assertEquals(sampleAttributes, readAttributes);
                    } else {
                        assertTrue(exception instanceof NoSuchFileException);
                    }

                    Mockito.verify(standardPathLister, Mockito.times(1)).toEntityDescriptor(chain1);
                    Mockito.verify(listUtil, Mockito.times(afsEntityExists ? 1 : 0)).getDefaultAfsFileAttributes(
                            permId, afsFilePath, mutable
                    );

                    Mockito.clearInvocations(standardPathLister, listUtil);
                }
            }
        }

        // AFS cases
        // Root
        SftpNodeChain chain2 = Mockito.spy(SftpNodeChain.concat(
                exampleBaseChainUpToProject,
                SftpNode.builder()
                        .type(SftpNode.Type.SUBLEVEL)
                        .identifier(Optional.of(StandardPathTranslator.FILE_TYPE_LABEL))
                        .build()
        ));
        for (boolean afsEntityExists: List.of(false, true)) {
            for (boolean mutable : List.of(false, true)) {
                String afsFilePath = "/";
                SftpListUtil.EntityBasicInfo entityBasicInfo = new SftpListUtil.EntityBasicInfo(afsEntityExists, null, null, mutable);
                String permId = "12345-12345";
                FtpPathLister.EntityDescriptor entityDescriptor = new FtpPathLister.EntityDescriptor(
                        SftpNode.Type.AFS_FILE,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        new FtpPathLister.EntityDescriptor(SftpNode.Type.SAMPLE,
                                Optional.of("space_1"),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.of(permId),
                                Optional.empty(),
                                null,
                                null,
                                entityBasicInfo
                        ),
                        afsFilePath,
                        null
                );
                Mockito.doReturn(Optional.of(entityDescriptor)).when(standardPathLister).toEntityDescriptor(chain2);

                Exception exception = null;
                SftpFileAttributes readAttributes = null;
                try {
                    readAttributes = standardPathLister.readAttributes(chain2);
                } catch (Exception e) {
                    exception = e;
                }

                if (entityBasicInfo.exists()) {
                    assertEquals(SftpListUtil.getDefaultAbstractDirectoryAttributes(
                            mutable,
                            entityBasicInfo.registrationMillis(),
                            entityBasicInfo.lastModificationMillis()
                    ), readAttributes);
                } else {
                    assertTrue(exception instanceof NoSuchFileException);
                }

                Mockito.verify(listUtil, Mockito.times(mutable ? 1 : 0)).tryToCreateAfsFileRootIfNecessary(
                        permId
                );

                Mockito.verify(standardPathLister, Mockito.times(1)).toEntityDescriptor(chain2);
                Mockito.verify(listUtil, Mockito.times(0)).getDefaultAfsFileAttributes(
                        permId, afsFilePath, mutable
                );

                Mockito.clearInvocations(standardPathLister, listUtil);
            }
        }
    }

    public void testListRoot() {
        SftpListUtil listUtil = Mockito.mock(SftpListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));

        assertEquals(List.of(
                SftpNodeChain.concat(
                        exampleBaseChainUpToProject,
                        SftpNodeChain.createSublevelNode(StandardPathTranslator.SPACE_TYPE_LABEL)
                )), standardPathLister.listRoot(null, exampleBaseChainUpToProject));

        standardPathLister.listRoot(StandardPathTranslator.SPACE_TYPE_LABEL, exampleBaseChainUpToProject);
        Mockito.verify(listUtil, Mockito.times(1)).getSpaces();
    }

    public void testListSpace() {
        SftpListUtil listUtil = Mockito.mock(SftpListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));

        SftpNode spaceNode = SftpNode.builder()
                .type(SftpNode.Type.SPACE)
                .identifier(Optional.of("space-1")).build();

        assertEquals(List.of(
                SftpNodeChain.concat(
                        exampleBaseChainUpToProject,
                        SftpNodeChain.createSublevelNode(StandardPathTranslator.FOLDER_TYPE_LABEL)
                ),
                SftpNodeChain.concat(
                        exampleBaseChainUpToProject,
                        SftpNodeChain.createSublevelNode(StandardPathTranslator.SAMPLE_TYPE_LABEL)
                ),
                SftpNodeChain.concat(
                        exampleBaseChainUpToProject,
                        SftpNodeChain.createSublevelNode(StandardPathTranslator.PROJECT_TYPE_LABEL)
                )), standardPathLister.listSpace(spaceNode, null, exampleBaseChainUpToProject));

        standardPathLister.listSpace(spaceNode, StandardPathTranslator.FOLDER_TYPE_LABEL, exampleBaseChainUpToProject);
        Mockito.verify(standardPathLister, Mockito.times(1)).listSamplesOrFoldersInSpace(
                spaceNode, exampleBaseChainUpToProject, true
        );
        Mockito.clearInvocations(standardPathLister);

        standardPathLister.listSpace(spaceNode, StandardPathTranslator.SAMPLE_TYPE_LABEL, exampleBaseChainUpToProject);
        Mockito.verify(standardPathLister, Mockito.times(1)).listSamplesOrFoldersInSpace(
                spaceNode, exampleBaseChainUpToProject, false
        );
        Mockito.clearInvocations(standardPathLister);

        standardPathLister.listSpace(spaceNode, StandardPathTranslator.PROJECT_TYPE_LABEL, exampleBaseChainUpToProject);
        Mockito.verify(standardPathLister, Mockito.times(1)).listProjectsInSpace(
                spaceNode, exampleBaseChainUpToProject
        );
        Mockito.clearInvocations(standardPathLister);
    }

    public void testListProjectsInSpace() {
        SftpListUtil listUtil = Mockito.mock(SftpListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));

        SftpNode spaceNode = SftpNode.builder()
                .type(SftpNode.Type.SPACE)
                .identifier(Optional.of("space-1")).build();

        standardPathLister.listProjectsInSpace(spaceNode, exampleBaseChainUpToProject);
        Mockito.verify(listUtil, Mockito.times(1)).getProjects(
                spaceNode.getIdentifier().get()
        );
    }

    public void testListSamplesOrFoldersInSpace() {
        SftpListUtil listUtil = Mockito.mock(SftpListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));

        SftpNode spaceNode = SftpNode.builder()
                .type(SftpNode.Type.SPACE)
                .identifier(Optional.of("space-1")).build();

        SampleFetchOptions fetchOptions = new SampleFetchOptions();
        fetchOptions.withType();
        fetchOptions.withProperties();

        Sample sample1 = new Sample();
        sample1.setFetchOptions(fetchOptions);
        sample1.setPermId(new SamplePermId("SAMPLE-1"));
        sample1.setStringProperty("NAME", "SaMpleName");
        SampleType sampleType1 = new SampleType();
        sampleType1.setCode("NONFOLDER");
        sample1.setType(sampleType1);

        Sample sample2 = new Sample();
        sample2.setFetchOptions(fetchOptions);
        sample2.setPermId(new SamplePermId("FOLDER-2"));
        sample2.setStringProperty("NAME", "folderNAME");
        SampleType sampleType2 = new SampleType();
        sampleType2.setCode("FOLDER");
        sample2.setType(sampleType2);

        List<Sample> returnedSamples = List.of(
                sample1,
                sample2
        );
        Mockito.doReturn(returnedSamples).when(listUtil)
                        .getSpaceSamples("space-1");

        List<SftpNodeChain> sftpNodeChainList;

        sftpNodeChainList =
                standardPathLister.listSamplesOrFoldersInSpace(spaceNode, exampleBaseChainUpToProject, true);
        Mockito.verify(listUtil, Mockito.times(1)).getSpaceSamples(
                spaceNode.getIdentifier().get()
        );
        assertEquals(1, sftpNodeChainList.size());
        assertEquals(
                SftpNode.Type.FOLDER,
                sftpNodeChainList.getLast().getLast()
                        .get().getType());
        assertEquals(
                "folderNAME (FOLDER-2)",
                sftpNodeChainList.getLast().getLast()
                        .get().getIdentifier().get());
        Mockito.clearInvocations(listUtil);

        sftpNodeChainList =
                standardPathLister.listSamplesOrFoldersInSpace(spaceNode, exampleBaseChainUpToProject, false);
        Mockito.verify(listUtil, Mockito.times(1)).getSpaceSamples(
                spaceNode.getIdentifier().get()
        );
        assertEquals(1, sftpNodeChainList.size());
        assertEquals(
                SftpNode.Type.SAMPLE,
                sftpNodeChainList.getLast().getLast()
                        .get().getType());
        assertEquals(
                "SaMpleName (SAMPLE-1)",
                sftpNodeChainList.getLast().getLast()
                        .get().getIdentifier().get());
        Mockito.clearInvocations(listUtil);
    }

    public void testListSamplesOrFoldersInProject() {
        SftpListUtil listUtil = Mockito.mock(SftpListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));
        SftpNodeChain baseChain = Mockito.spy(exampleBaseChainUpToProject);
        Mockito.doReturn("space-1").when(baseChain).lookUpSpaceCode();

        SftpNode projectNode = SftpNode.builder()
                .type(SftpNode.Type.PROJECT)
                .identifier(Optional.of("project-1")).build();

        SampleFetchOptions fetchOptions = new SampleFetchOptions();
        fetchOptions.withType();
        fetchOptions.withProperties();

        Sample sample1 = new Sample();
        sample1.setFetchOptions(fetchOptions);
        sample1.setPermId(new SamplePermId("SAMPLE-1"));
        sample1.setStringProperty("NAME", "SaMpleName");
        SampleType sampleType1 = new SampleType();
        sampleType1.setCode("NONFOLDER");
        sample1.setType(sampleType1);

        Sample sample2 = new Sample();
        sample2.setFetchOptions(fetchOptions);
        sample2.setPermId(new SamplePermId("FOLDER-2"));
        sample2.setStringProperty("NAME", "folderNAME");
        SampleType sampleType2 = new SampleType();
        sampleType2.setCode("FOLDER");
        sample2.setType(sampleType2);

        List<Sample> returnedSamples = List.of(
                sample1,
                sample2
        );
        Mockito.doReturn(returnedSamples).when(listUtil)
                .getProjectSamples("space-1", "project-1");

        List<SftpNodeChain> sftpNodeChainList;

        sftpNodeChainList =
                standardPathLister.listSamplesOrFoldersInProject(projectNode, baseChain, true);
        Mockito.verify(listUtil, Mockito.times(1)).getProjectSamples("space-1", "project-1");
        assertEquals(1, sftpNodeChainList.size());
        assertEquals(
                SftpNode.Type.FOLDER,
                sftpNodeChainList.getLast().getLast()
                        .get().getType());
        assertEquals(
                "folderNAME (FOLDER-2)",
                sftpNodeChainList.getLast().getLast()
                        .get().getIdentifier().get());
        Mockito.clearInvocations(listUtil);

        sftpNodeChainList =
                standardPathLister.listSamplesOrFoldersInProject(projectNode, baseChain, false);
        Mockito.verify(listUtil, Mockito.times(1)).getProjectSamples("space-1", "project-1");
        assertEquals(1, sftpNodeChainList.size());
        assertEquals(
                SftpNode.Type.SAMPLE,
                sftpNodeChainList.getLast().getLast()
                        .get().getType());
        assertEquals(
                "SaMpleName (SAMPLE-1)",
                sftpNodeChainList.getLast().getLast()
                        .get().getIdentifier().get());
        Mockito.clearInvocations(listUtil);
    }

    public void testListSample() {
        SftpListUtil listUtil = Mockito.mock(SftpListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));

        SftpNode sampleNode = SftpNode.builder()
                .type(SftpNode.Type.SAMPLE)
                .identifier(Optional.of("(sample-1)")).build();

        assertEquals(List.of(
                SftpNodeChain.concat(
                        exampleBaseChainUpToProject,
                        SftpNodeChain.createSublevelNode(StandardPathTranslator.FOLDER_TYPE_LABEL)
                ),
                SftpNodeChain.concat(
                        exampleBaseChainUpToProject,
                        SftpNodeChain.createSublevelNode(StandardPathTranslator.SAMPLE_TYPE_LABEL)
                ),
                SftpNodeChain.concat(
                        exampleBaseChainUpToProject,
                        SftpNodeChain.createSublevelNode(StandardPathTranslator.DATA_SET_TYPE_LABEL)
                ),
                SftpNodeChain.concat(
                        exampleBaseChainUpToProject,
                        SftpNodeChain.createSublevelNode(StandardPathTranslator.FILE_TYPE_LABEL)
                )), standardPathLister.listSample(sampleNode, null, exampleBaseChainUpToProject));

        standardPathLister.listSample(sampleNode, StandardPathTranslator.FOLDER_TYPE_LABEL, exampleBaseChainUpToProject);
        Mockito.verify(standardPathLister, Mockito.times(1)).listSamplesOrFoldersInSample(
                sampleNode, exampleBaseChainUpToProject, true
        );
        Mockito.clearInvocations(standardPathLister);

        standardPathLister.listSample(sampleNode, StandardPathTranslator.SAMPLE_TYPE_LABEL, exampleBaseChainUpToProject);
        Mockito.verify(standardPathLister, Mockito.times(1)).listSamplesOrFoldersInSample(
                sampleNode, exampleBaseChainUpToProject, false
        );
        Mockito.clearInvocations(standardPathLister);

        standardPathLister.listSample(sampleNode, StandardPathTranslator.DATA_SET_TYPE_LABEL, exampleBaseChainUpToProject);
        Mockito.verify(standardPathLister, Mockito.times(1)).listDataSetsInSample(
                sampleNode, exampleBaseChainUpToProject
        );

        standardPathLister.listSample(sampleNode, StandardPathTranslator.FILE_TYPE_LABEL, exampleBaseChainUpToProject);
        Mockito.verify(standardPathLister, Mockito.times(1)).listFilesInSampleOrFolder(
                sampleNode, exampleBaseChainUpToProject
        );
        Mockito.clearInvocations(standardPathLister);
    }

    public void testListFolder() {
        SftpListUtil listUtil = Mockito.mock(SftpListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));

        SftpNode folderNode = SftpNode.builder()
                .type(SftpNode.Type.FOLDER)
                .identifier(Optional.of("(folder-1)")).build();

        assertEquals(List.of(
                SftpNodeChain.concat(
                        exampleBaseChainUpToProject,
                        SftpNodeChain.createSublevelNode(StandardPathTranslator.FOLDER_TYPE_LABEL)
                ),
                SftpNodeChain.concat(
                        exampleBaseChainUpToProject,
                        SftpNodeChain.createSublevelNode(StandardPathTranslator.SAMPLE_TYPE_LABEL)
                ),
                SftpNodeChain.concat(
                        exampleBaseChainUpToProject,
                        SftpNodeChain.createSublevelNode(StandardPathTranslator.DATA_SET_TYPE_LABEL)
                ),
                SftpNodeChain.concat(
                        exampleBaseChainUpToProject,
                        SftpNodeChain.createSublevelNode(StandardPathTranslator.FILE_TYPE_LABEL)
                )), standardPathLister.listFolder(folderNode, null, exampleBaseChainUpToProject));

        standardPathLister.listFolder(folderNode, StandardPathTranslator.FOLDER_TYPE_LABEL, exampleBaseChainUpToProject);
        Mockito.verify(standardPathLister, Mockito.times(1)).listSamplesOrFoldersInSample(
                folderNode, exampleBaseChainUpToProject, true
        );
        Mockito.clearInvocations(standardPathLister);

        standardPathLister.listFolder(folderNode, StandardPathTranslator.SAMPLE_TYPE_LABEL, exampleBaseChainUpToProject);
        Mockito.verify(standardPathLister, Mockito.times(1)).listSamplesOrFoldersInSample(
                folderNode, exampleBaseChainUpToProject, false
        );
        Mockito.clearInvocations(standardPathLister);

        standardPathLister.listFolder(folderNode, StandardPathTranslator.DATA_SET_TYPE_LABEL, exampleBaseChainUpToProject);
        Mockito.verify(standardPathLister, Mockito.times(1)).listDataSetsInSample(
                folderNode, exampleBaseChainUpToProject
        );
        Mockito.clearInvocations(standardPathLister);

        standardPathLister.listFolder(folderNode, StandardPathTranslator.FILE_TYPE_LABEL, exampleBaseChainUpToProject);
        Mockito.verify(standardPathLister, Mockito.times(1)).listFilesInSampleOrFolder(
                folderNode, exampleBaseChainUpToProject
        );
        Mockito.clearInvocations(standardPathLister);
    }

    public void testListSamplesOrFoldersInSample() {
        SftpListUtil listUtil = Mockito.mock(SftpListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));
        SftpNodeChain baseChain = Mockito.spy(exampleBaseChainUpToProject);

        SftpNode sampleNode = SftpNode.builder()
                .type(SftpNode.Type.SAMPLE)
                .identifier(Optional.of("SAMPLE NAME1 (sample-perm-id-1)")).build();

        SampleFetchOptions fetchOptions = new SampleFetchOptions();
        fetchOptions.withType();
        fetchOptions.withProperties();

        Sample sample1 = new Sample();
        sample1.setFetchOptions(fetchOptions);
        sample1.setPermId(new SamplePermId("SAMPLE-1"));
        sample1.setStringProperty("NAME", "SaMpleName");
        SampleType sampleType1 = new SampleType();
        sampleType1.setCode("NONFOLDER");
        sample1.setType(sampleType1);

        Sample sample2 = new Sample();
        sample2.setFetchOptions(fetchOptions);
        sample2.setPermId(new SamplePermId("FOLDER-2"));
        sample2.setStringProperty("NAME", "folderNAME");
        SampleType sampleType2 = new SampleType();
        sampleType2.setCode("FOLDER");
        sample2.setType(sampleType2);

        List<Sample> returnedSamples = List.of(
                sample1,
                sample2
        );
        Mockito.doReturn(returnedSamples).when(listUtil)
                .getSampleChildren("sample-perm-id-1");

        List<SftpNodeChain> sftpNodeChainList;

        sftpNodeChainList =
                standardPathLister.listSamplesOrFoldersInSample(sampleNode, baseChain, true);
        Mockito.verify(listUtil, Mockito.times(1)).getSampleChildren("sample-perm-id-1");
        assertEquals(1, sftpNodeChainList.size());
        assertEquals(
                SftpNode.Type.FOLDER,
                sftpNodeChainList.getLast().getLast()
                        .get().getType());
        assertEquals(
                "folderNAME (FOLDER-2)",
                sftpNodeChainList.getLast().getLast()
                        .get().getIdentifier().get());
        Mockito.clearInvocations(listUtil);

        sftpNodeChainList =
                standardPathLister.listSamplesOrFoldersInSample(sampleNode, baseChain, false);
        Mockito.verify(listUtil, Mockito.times(1)).getSampleChildren("sample-perm-id-1");
        assertEquals(1, sftpNodeChainList.size());
        assertEquals(
                SftpNode.Type.SAMPLE,
                sftpNodeChainList.getLast().getLast()
                        .get().getType());
        assertEquals(
                "SaMpleName (SAMPLE-1)",
                sftpNodeChainList.getLast().getLast()
                        .get().getIdentifier().get());
        Mockito.clearInvocations(listUtil);
    }

    public void testListDataSetsInSample() {
        SftpListUtil listUtil = Mockito.mock(SftpListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));
        SftpNodeChain baseChain = Mockito.spy(exampleBaseChainUpToProject);

        SftpNode sampleNode = SftpNode.builder()
                .type(SftpNode.Type.SAMPLE)
                .identifier(Optional.of("Sample name (sample-perm-id-1)")).build();

        standardPathLister.listDataSetsInSample(sampleNode, baseChain);
        Mockito.verify(listUtil, Mockito.times(1)).getSampleDatasets("sample-perm-id-1");
    }

    public void testListDataSetsInExperiment() {
        SftpListUtil listUtil = Mockito.mock(SftpListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));
        SftpNodeChain baseChain = Mockito.spy(exampleBaseChainUpToProject);

        SftpNode experimentNode = SftpNode.builder()
                .type(SftpNode.Type.EXPERIMENT)
                .identifier(Optional.of("Experiment name (exp-perm-id-1)")).build();

        standardPathLister.listDataSetsInExperiment(experimentNode, baseChain);
        Mockito.verify(listUtil, Mockito.times(1)).getExperimentDatasets("exp-perm-id-1");
    }

    public void testListFilesInSampleOrFolder() {
        SftpListUtil listUtil = Mockito.mock(SftpListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));
        SftpNodeChain baseChain = Mockito.spy(exampleBaseChainUpToProject);

        SftpNode sampleNode = SftpNode.builder()
                .type(SftpNode.Type.SAMPLE)
                .identifier(Optional.of("Sample name (sample-perm-id-1)")).build();

        Mockito.doReturn("afs-perm-id-1").when(listUtil)
                        .getAfsEntityPermId(sampleNode);
        Mockito.doReturn(new File[0]).when(listUtil).listAfsFiles(Mockito.anyString(), Mockito.anyString());

        standardPathLister.listFilesInSampleOrFolder(sampleNode, baseChain);
        Mockito.verify(listUtil, Mockito.times(1)).listAfsFiles("afs-perm-id-1", "/");
    }

    public void testListFilesInDataSet() {
        SftpListUtil listUtil = Mockito.mock(SftpListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));
        SftpNodeChain baseChain = Mockito.spy(exampleBaseChainUpToProject);

        SftpNode datasetNode = SftpNode.builder()
                .type(SftpNode.Type.DATA_SET)
                .identifier(Optional.of("dataset-perm-id-1")).build();

        Mockito.doReturn("afs-perm-id-1").when(listUtil)
                .getAfsEntityPermId(datasetNode);
        Mockito.doReturn(new File[0]).when(listUtil).listAfsFiles(Mockito.anyString(), Mockito.anyString());

        standardPathLister.listFilesInDataSet(datasetNode, baseChain);
        Mockito.verify(listUtil, Mockito.times(1)).listAfsFiles("afs-perm-id-1", "/");
    }

    public void testListFilesInExperiment() {
        SftpListUtil listUtil = Mockito.mock(SftpListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));
        SftpNodeChain baseChain = Mockito.spy(exampleBaseChainUpToProject);

        SftpNode experimentNode = SftpNode.builder()
                .type(SftpNode.Type.EXPERIMENT)
                .identifier(Optional.of("Experiment name (experiment-1)")).build();

        Mockito.doReturn("afs-perm-id-1").when(listUtil)
                .getAfsEntityPermId(experimentNode);
        Mockito.doReturn(new File[0]).when(listUtil).listAfsFiles(Mockito.anyString(), Mockito.anyString());

        standardPathLister.listFilesInExperiment(experimentNode, baseChain);
        Mockito.verify(listUtil, Mockito.times(1)).listAfsFiles("afs-perm-id-1", "/");
    }

    public void testListDataSet() {
        SftpListUtil listUtil = Mockito.mock(SftpListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));

        SftpNode datasetNode = SftpNode.builder()
                .type(SftpNode.Type.DATA_SET)
                .identifier(Optional.of("Dataset name (dataset-1)")).build();

        assertEquals(List.of(
                SftpNodeChain.concat(
                        exampleBaseChainUpToProject,
                        SftpNodeChain.createSublevelNode(StandardPathTranslator.FILE_TYPE_LABEL)
                )
        ), standardPathLister.listDataSet(datasetNode, null, exampleBaseChainUpToProject));

        standardPathLister.listDataSet(datasetNode, StandardPathTranslator.FILE_TYPE_LABEL, exampleBaseChainUpToProject);
        Mockito.verify(standardPathLister, Mockito.times(1)).listFilesInDataSet(
                datasetNode, exampleBaseChainUpToProject
        );
    }

    public void testListProject() {
        SftpListUtil listUtil = Mockito.mock(SftpListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));

        SftpNode projectNode = SftpNode.builder()
                .type(SftpNode.Type.PROJECT)
                .identifier(Optional.of("project-1")).build();

        assertEquals(List.of(
                SftpNodeChain.concat(
                        exampleBaseChainUpToProject,
                        SftpNodeChain.createSublevelNode(StandardPathTranslator.FOLDER_TYPE_LABEL)
                ),
                SftpNodeChain.concat(
                        exampleBaseChainUpToProject,
                        SftpNodeChain.createSublevelNode(StandardPathTranslator.SAMPLE_TYPE_LABEL)
                ),
                SftpNodeChain.concat(
                        exampleBaseChainUpToProject,
                        SftpNodeChain.createSublevelNode(StandardPathTranslator.EXPERIMENT_TYPE_LABEL)
                )), standardPathLister.listProject(projectNode, null, exampleBaseChainUpToProject));

        standardPathLister.listProject(projectNode, StandardPathTranslator.FOLDER_TYPE_LABEL, exampleBaseChainUpToProject);
        Mockito.verify(standardPathLister, Mockito.times(1)).listSamplesOrFoldersInProject(
                projectNode, exampleBaseChainUpToProject, true
        );
        Mockito.clearInvocations(standardPathLister);

        standardPathLister.listProject(projectNode, StandardPathTranslator.SAMPLE_TYPE_LABEL, exampleBaseChainUpToProject);
        Mockito.verify(standardPathLister, Mockito.times(1)).listSamplesOrFoldersInProject(
                projectNode, exampleBaseChainUpToProject, false
        );
        Mockito.clearInvocations(standardPathLister);

        standardPathLister.listProject(projectNode, StandardPathTranslator.EXPERIMENT_TYPE_LABEL, exampleBaseChainUpToProject);
        Mockito.verify(standardPathLister, Mockito.times(1)).listExperimentsInProject(
                projectNode, exampleBaseChainUpToProject
        );
        Mockito.clearInvocations(standardPathLister);
    }

    public void testListExperimentsInProject() {
        SftpListUtil listUtil = Mockito.mock(SftpListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));
        SftpNodeChain baseChain = Mockito.spy(exampleBaseChainUpToProject);
        Mockito.doReturn("space-1").when(baseChain).lookUpSpaceCode();

        SftpNode projectNode = SftpNode.builder()
                .type(SftpNode.Type.PROJECT)
                .identifier(Optional.of("project-1")).build();

        standardPathLister.listExperimentsInProject(projectNode, baseChain);
        Mockito.verify(listUtil, Mockito.times(1)).getExperiments(
                "space-1", "project-1"
        );
    }

    public void testListExperiment() {
        SftpListUtil listUtil = Mockito.mock(SftpListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));
        SftpNodeChain baseChain = Mockito.spy(exampleBaseChainUpToProject);

        SftpNode experimentNode = SftpNode.builder()
                .type(SftpNode.Type.EXPERIMENT)
                .identifier(Optional.of("Experiment name (experiment-1)")).build();

        assertEquals(List.of(
                SftpNodeChain.concat(
                        baseChain,
                        SftpNodeChain.createSublevelNode(StandardPathTranslator.FOLDER_TYPE_LABEL)
                ),
                SftpNodeChain.concat(
                        baseChain,
                        SftpNodeChain.createSublevelNode(StandardPathTranslator.SAMPLE_TYPE_LABEL)
                ),
                SftpNodeChain.concat(
                        baseChain,
                        SftpNodeChain.createSublevelNode(StandardPathTranslator.DATA_SET_TYPE_LABEL)
                ),
                SftpNodeChain.concat(
                        baseChain,
                        SftpNodeChain.createSublevelNode(StandardPathTranslator.FILE_TYPE_LABEL)
                )), standardPathLister.listExperiment(experimentNode, null, baseChain));

        standardPathLister.listExperiment(experimentNode, StandardPathTranslator.FOLDER_TYPE_LABEL, baseChain);
        Mockito.verify(standardPathLister, Mockito.times(1)).listSamplesOrFoldersInExperiment(
                experimentNode, baseChain, true
        );
        Mockito.clearInvocations(standardPathLister);

        standardPathLister.listExperiment(experimentNode, StandardPathTranslator.SAMPLE_TYPE_LABEL, baseChain);
        Mockito.verify(standardPathLister, Mockito.times(1)).listSamplesOrFoldersInExperiment(
                experimentNode, baseChain, false
        );
        Mockito.clearInvocations(standardPathLister);

        standardPathLister.listExperiment(experimentNode, StandardPathTranslator.DATA_SET_TYPE_LABEL, baseChain);
        Mockito.verify(standardPathLister, Mockito.times(1)).listDataSetsInExperiment(
                experimentNode, baseChain
        );
        Mockito.clearInvocations(standardPathLister);

        standardPathLister.listExperiment(experimentNode, StandardPathTranslator.FILE_TYPE_LABEL, baseChain);
        Mockito.verify(standardPathLister, Mockito.times(1)).listFilesInExperiment(
                experimentNode, baseChain
        );
        Mockito.clearInvocations(standardPathLister);
    }

    public void testListSamplesOrFoldersInExperiment() {
        SftpListUtil listUtil = Mockito.mock(SftpListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));
        SftpNodeChain baseChain = Mockito.spy(exampleBaseChainUpToProject);

        SftpNode experimentNode = SftpNode.builder()
                .type(SftpNode.Type.EXPERIMENT)
                .identifier(Optional.of("exp NAME (experiment-perm-id-1)")).build();

        SampleFetchOptions fetchOptions = new SampleFetchOptions();
        fetchOptions.withType();
        fetchOptions.withProperties();

        Sample sample1 = new Sample();
        sample1.setFetchOptions(fetchOptions);
        sample1.setPermId(new SamplePermId("SAMPLE-1"));
        sample1.setStringProperty("NAME", "SaMpleName");
        SampleType sampleType1 = new SampleType();
        sampleType1.setCode("NONFOLDER");
        sample1.setType(sampleType1);

        Sample sample2 = new Sample();
        sample2.setFetchOptions(fetchOptions);
        sample2.setPermId(new SamplePermId("FOLDER-2"));
        sample2.setStringProperty("NAME", "folderNAME");
        SampleType sampleType2 = new SampleType();
        sampleType2.setCode("FOLDER");
        sample2.setType(sampleType2);

        List<Sample> returnedSamples = List.of(
                sample1,
                sample2
        );
        Mockito.doReturn(returnedSamples).when(listUtil)
                .getExperimentSamples("experiment-perm-id-1");

        List<SftpNodeChain> sftpNodeChainList;

        sftpNodeChainList =
                standardPathLister.listSamplesOrFoldersInExperiment(experimentNode, baseChain, true);
        Mockito.verify(listUtil, Mockito.times(1)).getExperimentSamples("experiment-perm-id-1");
        assertEquals(1, sftpNodeChainList.size());
        assertEquals(
                SftpNode.Type.FOLDER,
                sftpNodeChainList.getLast().getLast()
                        .get().getType());
        assertEquals(
                "folderNAME (FOLDER-2)",
                sftpNodeChainList.getLast().getLast()
                        .get().getIdentifier().get());
        Mockito.clearInvocations(listUtil);

        sftpNodeChainList =
                standardPathLister.listSamplesOrFoldersInExperiment(experimentNode, baseChain, false);
        Mockito.verify(listUtil, Mockito.times(1)).getExperimentSamples("experiment-perm-id-1");
        assertEquals(1, sftpNodeChainList.size());
        assertEquals(
                SftpNode.Type.SAMPLE,
                sftpNodeChainList.getLast().getLast()
                        .get().getType());
        assertEquals(
                "SaMpleName (SAMPLE-1)",
                sftpNodeChainList.getLast().getLast()
                        .get().getIdentifier().get());
        Mockito.clearInvocations(listUtil);
    }

    public void testListFilesInAfsFileNode() {
        SftpListUtil listUtil = Mockito.mock(SftpListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));
        SftpNodeChain baseChain = Mockito.spy(exampleBaseChainUpToProject);

        SftpNode afsEntityNode = SftpNode.builder()
                .type(SftpNode.Type.SAMPLE)
                .identifier(Optional.of("Sample name (sample-1)")).build();

        Mockito.doReturn(afsEntityNode).when(standardPathLister)
                .validateAndGetAfsEntityNodeFromAfsFileChain(baseChain);
        Mockito.doReturn("afs-perm-id-1").when(listUtil)
                .getAfsEntityPermId(afsEntityNode);

        SftpNode afsFileNode = SftpNode.builder()
                .type(SftpNode.Type.AFS_FILE)
                .afsFilePath(List.of("dir-1", "dir-2", "file-12")).build();


        Mockito.doReturn(new File[0]).when(listUtil).listAfsFiles(Mockito.anyString(), Mockito.anyString());

        standardPathLister.listFilesInAfsFileNode(afsFileNode, baseChain);
        Mockito.verify(listUtil, Mockito.times(1)).listAfsFiles("afs-perm-id-1", "/dir-1/dir-2/file-12");
    }

    public void testValidateAndGetAfsEntityNodeFromAfsFileChain() {
        SftpListUtil listUtil = Mockito.mock(SftpListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));

        SftpNodeChain shortChain = new SftpNodeChain(
                List.of(
                        SftpNode.builder()
                                .type(SftpNode.Type.SUBLEVEL)
                                .identifier(Optional.of(StandardPathTranslator.FILE_TYPE_LABEL))
                                .build(),
                        SftpNode.builder().type(SftpNode.Type.AFS_FILE)
                                .afsFilePath(List.of("dir1", "dir2", "file"))
                                .build()
                )
        );
        Exception shortChainException = null;
        try {
            standardPathLister.validateAndGetAfsEntityNodeFromAfsFileChain(shortChain);
        } catch (Exception e) {
            shortChainException = e;
        }
        assertEquals(IllegalArgumentException.class, shortChainException.getClass());

        for (SftpNode.Type notAdmittedTypeForAfsEntity : List.of(
                SftpNode.Type.ROOT,
                SftpNode.Type.SPACE,
                SftpNode.Type.PROJECT,
                SftpNode.Type.AFS_FILE,
                SftpNode.Type.SUBLEVEL
        )) {
            SftpNodeChain noAfsEntity = new SftpNodeChain(
                    List.of(
                            SftpNode.builder()
                                    .type(notAdmittedTypeForAfsEntity)
                                    .identifier(Optional.of("id-fake"))
                                    .build(),
                            SftpNode.builder()
                                    .type(SftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.FILE_TYPE_LABEL))
                                    .build(),
                            SftpNode.builder().type(SftpNode.Type.AFS_FILE)
                                    .afsFilePath(List.of("dir1", "dir2", "file"))
                                    .build()
                    )
            );

            Exception noAfsEntityException = null;
            try {
                standardPathLister.validateAndGetAfsEntityNodeFromAfsFileChain(noAfsEntity);
            } catch (Exception e) {
                noAfsEntityException = e;
            }
            assertEquals(IllegalArgumentException.class, noAfsEntityException.getClass());
        }

        for (SftpNode.Type admittedTypeForAfsEntity : List.of(
                SftpNode.Type.SAMPLE,
                SftpNode.Type.FOLDER,
                SftpNode.Type.EXPERIMENT,
                SftpNode.Type.DATA_SET
        )) {
            SftpNodeChain goodAfsEntity = new SftpNodeChain(
                    List.of(
                            SftpNode.builder()
                                    .type(admittedTypeForAfsEntity)
                                    .identifier(Optional.of("id-fake"))
                                    .build(),
                            SftpNode.builder()
                                    .type(SftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.FILE_TYPE_LABEL))
                                    .build(),
                            SftpNode.builder().type(SftpNode.Type.AFS_FILE)
                                    .afsFilePath(List.of("dir1", "dir2", "file"))
                                    .build()
                    )
            );

            assertEquals(
                SftpNode.builder()
                    .type(admittedTypeForAfsEntity)
                    .identifier(Optional.of("id-fake"))
                    .build(),
                standardPathLister.validateAndGetAfsEntityNodeFromAfsFileChain(goodAfsEntity)
            );
        }

        for (SftpNode.Type notAdmittedTypeForAfsEntity : List.of(
                SftpNode.Type.ROOT,
                SftpNode.Type.SPACE,
                SftpNode.Type.PROJECT,
                SftpNode.Type.AFS_FILE,
                SftpNode.Type.SUBLEVEL
        )) {
            SftpNodeChain noAfsEntity = new SftpNodeChain(
                    List.of(
                            SftpNode.builder()
                                    .type(notAdmittedTypeForAfsEntity)
                                    .identifier(Optional.of("id-fake"))
                                    .build(),
                            SftpNode.builder()
                                    .type(SftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.FILE_TYPE_LABEL))
                                    .build()
                    )
            );

            Exception noAfsEntityException = null;
            try {
                standardPathLister.validateAndGetAfsEntityNodeFromAfsFileChain(noAfsEntity);
            } catch (Exception e) {
                noAfsEntityException = e;
            }
            assertEquals(IllegalArgumentException.class, noAfsEntityException.getClass());
        }

        for (SftpNode.Type admittedTypeForAfsEntity : List.of(
                SftpNode.Type.SAMPLE,
                SftpNode.Type.FOLDER,
                SftpNode.Type.EXPERIMENT,
                SftpNode.Type.DATA_SET
        )) {
            SftpNodeChain goodAfsEntity = new SftpNodeChain(
                    List.of(
                            SftpNode.builder()
                                    .type(admittedTypeForAfsEntity)
                                    .identifier(Optional.of("id-fake"))
                                    .build(),
                            SftpNode.builder()
                                    .type(SftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.FILE_TYPE_LABEL))
                                    .build()
                    )
            );

            assertEquals(
                    SftpNode.builder()
                            .type(admittedTypeForAfsEntity)
                            .identifier(Optional.of("id-fake"))
                            .build(),
                    standardPathLister.validateAndGetAfsEntityNodeFromAfsFileChain(goodAfsEntity)
            );
        }
    }

    public void testValidateAndGetAfsFilePathFromAfsFileChain() {
        SftpListUtil listUtil = Mockito.mock(SftpListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));

        SftpNodeChain shortChain = new SftpNodeChain(
                List.of(
                        SftpNode.builder()
                                .type(SftpNode.Type.SUBLEVEL)
                                .identifier(Optional.of(StandardPathTranslator.FILE_TYPE_LABEL))
                                .build(),
                        SftpNode.builder().type(SftpNode.Type.AFS_FILE)
                                .afsFilePath(List.of("dir1", "dir2", "file"))
                                .build()
                )
        );
        Exception shortChainException = null;
        try {
            standardPathLister.validateAndGetAfsFilePathFromAfsFileChain(shortChain);
        } catch (Exception e) {
            shortChainException = e;
        }
        assertEquals(IllegalArgumentException.class, shortChainException.getClass());

        for (SftpNode.Type notAdmittedTypeForAfsEntity : List.of(
                SftpNode.Type.ROOT,
                SftpNode.Type.SPACE,
                SftpNode.Type.PROJECT,
                SftpNode.Type.AFS_FILE,
                SftpNode.Type.SUBLEVEL
        )) {
            SftpNodeChain noAfsEntity = new SftpNodeChain(
                    List.of(
                            SftpNode.builder()
                                    .type(notAdmittedTypeForAfsEntity)
                                    .identifier(Optional.of("id-fake"))
                                    .build(),
                            SftpNode.builder()
                                    .type(SftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.FILE_TYPE_LABEL))
                                    .build(),
                            SftpNode.builder().type(SftpNode.Type.AFS_FILE)
                                    .afsFilePath(List.of("dir1", "dir2", "file"))
                                    .build()
                    )
            );

            Exception noAfsEntityException = null;
            try {
                standardPathLister.validateAndGetAfsFilePathFromAfsFileChain(noAfsEntity);
            } catch (Exception e) {
                noAfsEntityException = e;
            }
            assertEquals(IllegalArgumentException.class, noAfsEntityException.getClass());
        }

        for (SftpNode.Type admittedTypeForAfsEntity : List.of(
                SftpNode.Type.SAMPLE,
                SftpNode.Type.FOLDER,
                SftpNode.Type.EXPERIMENT,
                SftpNode.Type.DATA_SET
        )) {
            SftpNodeChain goodAfsEntity = new SftpNodeChain(
                    List.of(
                            SftpNode.builder()
                                    .type(admittedTypeForAfsEntity)
                                    .identifier(Optional.of("id-fake"))
                                    .build(),
                            SftpNode.builder()
                                    .type(SftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.FILE_TYPE_LABEL))
                                    .build(),
                            SftpNode.builder().type(SftpNode.Type.AFS_FILE)
                                    .afsFilePath(List.of("dir1", "dir2", "file"))
                                    .build()
                    )
            );

            assertEquals(
                    "/dir1/dir2/file",
                    standardPathLister.validateAndGetAfsFilePathFromAfsFileChain(goodAfsEntity)
            );
        }

        for (SftpNode.Type notAdmittedTypeForAfsEntity : List.of(
                SftpNode.Type.ROOT,
                SftpNode.Type.SPACE,
                SftpNode.Type.PROJECT,
                SftpNode.Type.AFS_FILE,
                SftpNode.Type.SUBLEVEL
        )) {
            SftpNodeChain noAfsEntity = new SftpNodeChain(
                    List.of(
                            SftpNode.builder()
                                    .type(notAdmittedTypeForAfsEntity)
                                    .identifier(Optional.of("id-fake"))
                                    .build(),
                            SftpNode.builder()
                                    .type(SftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.FILE_TYPE_LABEL))
                                    .build()
                    )
            );

            Exception noAfsEntityException = null;
            try {
                standardPathLister.validateAndGetAfsFilePathFromAfsFileChain(noAfsEntity);
            } catch (Exception e) {
                noAfsEntityException = e;
            }
            assertEquals(IllegalArgumentException.class, noAfsEntityException.getClass());
        }

        for (SftpNode.Type admittedTypeForAfsEntity : List.of(
                SftpNode.Type.SAMPLE,
                SftpNode.Type.FOLDER,
                SftpNode.Type.EXPERIMENT,
                SftpNode.Type.DATA_SET
        )) {
            SftpNodeChain goodAfsEntity = new SftpNodeChain(
                    List.of(
                            SftpNode.builder()
                                    .type(admittedTypeForAfsEntity)
                                    .identifier(Optional.of("id-fake"))
                                    .build(),
                            SftpNode.builder()
                                    .type(SftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.FILE_TYPE_LABEL))
                                    .build()
                    )
            );

            assertEquals(
                    "/",
                    standardPathLister.validateAndGetAfsFilePathFromAfsFileChain(goodAfsEntity)
            );
        }
    }

    public void testPointsToAfsFile() {
        SftpListUtil listUtil = Mockito.mock(SftpListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));

        assertFalse(standardPathLister.pointsToAfsFile(new SftpNodeChain(
                Collections.emptyList()
        )));
        assertFalse(standardPathLister.pointsToAfsFile(new SftpNodeChain(
                List.of(
                        TestHelper.createRandomNode()
                )
        )));
        assertTrue(standardPathLister.pointsToAfsFile(new SftpNodeChain(
                List.of(
                        TestHelper.createRandomNode(),
                        TestHelper.createRandomNodeOfType(SftpNode.Type.AFS_FILE)
                )
        )));
        assertTrue(standardPathLister.pointsToAfsFile(new SftpNodeChain(
                List.of(
                        TestHelper.createRandomNode(),
                        TestHelper.createRandomNodeOfType(SftpNode.Type.SUBLEVEL)
                                .toBuilder()
                                .identifier(Optional.of(StandardPathTranslator.FILE_TYPE_LABEL))
                                .build()
                )
        )));
        assertFalse(standardPathLister.pointsToAfsFile(new SftpNodeChain(
                List.of(
                        TestHelper.createRandomNode(),
                        TestHelper.createRandomNodeOfType(SftpNode.Type.SUBLEVEL)
                                .toBuilder()
                                .identifier(Optional.of("other"))
                                .build()
                )
        )));
        for (SftpNode.Type otherType : List.of(
                SftpNode.Type.ROOT,
                SftpNode.Type.PROJECT,
                SftpNode.Type.EXPERIMENT,
                SftpNode.Type.SAMPLE,
                SftpNode.Type.FOLDER,
                SftpNode.Type.DATA_SET
            )
        ) {
            assertFalse(standardPathLister.pointsToAfsFile(new SftpNodeChain(
                    List.of(
                            TestHelper.createRandomNode(),
                            TestHelper.createRandomNodeOfType(otherType)
                    )
            )));
        }
    }
}