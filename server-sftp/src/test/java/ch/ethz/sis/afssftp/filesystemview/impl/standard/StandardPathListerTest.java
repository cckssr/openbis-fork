package ch.ethz.sis.afssftp.filesystemview.impl.standard;

import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afssftp.filesystemview.OpenBISSftpFileAttributes;
import ch.ethz.sis.afssftp.filesystemview.OpenBISSftpNode;
import ch.ethz.sis.afssftp.filesystemview.OpenBISSftpNodeChain;
import ch.ethz.sis.afssftp.util.OpenBISListUtil;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import junit.framework.TestCase;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class StandardPathListerTest extends TestCase {
    private static final OpenBISSftpNodeChain exampleBaseChain = new OpenBISSftpNodeChain(List.of(
            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.ROOT).build(),
            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                    .identifier(Optional.of(StandardPathTranslator.SPACE_TYPE_LABEL))
                    .build(),
            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SPACE)
                    .identifier(Optional.of("space_1")).build()
    ));

    public void testListWithRoot() {
        OpenBISListUtil listUtil = Mockito.mock(OpenBISListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));


        OpenBISSftpNode lastNode = OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.ROOT).build();
        OpenBISSftpNodeChain directory = OpenBISSftpNodeChain.concat(exampleBaseChain, lastNode);
        List<OpenBISSftpNodeChain> exampleListResult = List.of(
                new OpenBISSftpNodeChain(List.of(
                        OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.ROOT).build(),
                        OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                .identifier(Optional.of(StandardPathTranslator.SPACE_TYPE_LABEL))
                                .build()
                )));
        Mockito.doReturn(exampleListResult).when(standardPathLister).listRoot(null, directory);
        List<OpenBISSftpNodeChain> listResult = standardPathLister.list(directory);
        Mockito.verify(standardPathLister, Mockito.times(1)).listRoot(null, directory);
        assertEquals(exampleListResult, listResult);
    }

    public void testListWithSpace() {
        OpenBISListUtil listUtil = Mockito.mock(OpenBISListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));


        OpenBISSftpNode lastNode = OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SPACE)
                .identifier(Optional.of("space_1")).build();
        OpenBISSftpNodeChain directory = OpenBISSftpNodeChain.concat(exampleBaseChain, lastNode);
        List<OpenBISSftpNodeChain> exampleListResult = List.of(
                new OpenBISSftpNodeChain(List.of(
                        OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.ROOT).build(),
                        OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                .identifier(Optional.of(StandardPathTranslator.SPACE_TYPE_LABEL))
                                .build(),
                        OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SPACE)
                                .identifier(Optional.of("space_1"))
                                .build(),
                        OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                .identifier(Optional.of(StandardPathTranslator.PROJECT_TYPE_LABEL))
                                .build()
                )));
        Mockito.doReturn(exampleListResult).when(standardPathLister).listSpace(lastNode, null, directory);
        List<OpenBISSftpNodeChain> listResult = standardPathLister.list(directory);
        Mockito.verify(standardPathLister, Mockito.times(1)).listSpace(lastNode, null, directory);
        assertEquals(exampleListResult, listResult);
    }

    public void testListWithSample() {
        OpenBISListUtil listUtil = Mockito.mock(OpenBISListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));


        OpenBISSftpNode lastNode = OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SAMPLE)
                .identifier(Optional.of("sample_1")).build();
        OpenBISSftpNodeChain directory = OpenBISSftpNodeChain.concat(exampleBaseChain, lastNode);
        List<OpenBISSftpNodeChain> exampleListResult = List.of(
                new OpenBISSftpNodeChain(List.of(
                        OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.ROOT).build(),
                        OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                .identifier(Optional.of(StandardPathTranslator.SAMPLE_TYPE_LABEL))
                                .build(),
                        OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SAMPLE)
                                .identifier(Optional.of("sample_1"))
                                .build(),
                        OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                .identifier(Optional.of(StandardPathTranslator.DATA_SET_TYPE_LABEL))
                                .build()
                )));
        Mockito.doReturn(exampleListResult).when(standardPathLister).listSample(lastNode, null, directory);
        List<OpenBISSftpNodeChain> listResult = standardPathLister.list(directory);
        Mockito.verify(standardPathLister, Mockito.times(1)).listSample(lastNode, null, directory);
        assertEquals(exampleListResult, listResult);
    }

    public void testListWithFolder() {
        OpenBISListUtil listUtil = Mockito.mock(OpenBISListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));


        OpenBISSftpNode lastNode = OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.FOLDER)
                .identifier(Optional.of("folder_1")).build();
        OpenBISSftpNodeChain directory = OpenBISSftpNodeChain.concat(exampleBaseChain, lastNode);
        List<OpenBISSftpNodeChain> exampleListResult = List.of(
                new OpenBISSftpNodeChain(List.of(
                        OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.ROOT).build(),
                        OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                .identifier(Optional.of(StandardPathTranslator.FOLDER_TYPE_LABEL))
                                .build(),
                        OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.FOLDER)
                                .identifier(Optional.of("folder_1"))
                                .build(),
                        OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                .identifier(Optional.of(StandardPathTranslator.SAMPLE_TYPE_LABEL))
                                .build()
                )));
        Mockito.doReturn(exampleListResult).when(standardPathLister).listFolder(lastNode, null, directory);
        List<OpenBISSftpNodeChain> listResult = standardPathLister.list(directory);
        Mockito.verify(standardPathLister, Mockito.times(1)).listFolder(lastNode, null, directory);
        assertEquals(exampleListResult, listResult);
    }

    public void testListWithDataset() {
        OpenBISListUtil listUtil = Mockito.mock(OpenBISListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));


        OpenBISSftpNode lastNode = OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.DATA_SET)
                .identifier(Optional.of("dataset_1")).build();
        OpenBISSftpNodeChain directory = OpenBISSftpNodeChain.concat(exampleBaseChain, lastNode);
        List<OpenBISSftpNodeChain> exampleListResult = List.of(
                new OpenBISSftpNodeChain(List.of(
                        OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.ROOT).build(),
                        OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                .identifier(Optional.of(StandardPathTranslator.DATA_SET_TYPE_LABEL))
                                .build(),
                        OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.DATA_SET)
                                .identifier(Optional.of("dataset_1"))
                                .build(),
                        OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                .identifier(Optional.of(StandardPathTranslator.FILE_TYPE_LABEL))
                                .build()
                )));
        Mockito.doReturn(exampleListResult).when(standardPathLister).listDataSet(lastNode, null, directory);
        List<OpenBISSftpNodeChain> listResult = standardPathLister.list(directory);
        Mockito.verify(standardPathLister, Mockito.times(1)).listDataSet(lastNode, null, directory);
        assertEquals(exampleListResult, listResult);
    }

    public void testListWithProject() {
        OpenBISListUtil listUtil = Mockito.mock(OpenBISListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));


        OpenBISSftpNode lastNode = OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.PROJECT)
                .identifier(Optional.of("project_1")).build();
        OpenBISSftpNodeChain directory = OpenBISSftpNodeChain.concat(exampleBaseChain, lastNode);
        List<OpenBISSftpNodeChain> exampleListResult = List.of(
                new OpenBISSftpNodeChain(List.of(
                        OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.ROOT).build(),
                        OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                .identifier(Optional.of(StandardPathTranslator.PROJECT_TYPE_LABEL))
                                .build(),
                        OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.PROJECT)
                                .identifier(Optional.of("project_1"))
                                .build(),
                        OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                .identifier(Optional.of(StandardPathTranslator.EXPERIMENT_TYPE_LABEL))
                                .build()
                )));
        Mockito.doReturn(exampleListResult).when(standardPathLister).listProject(lastNode, null, directory);
        List<OpenBISSftpNodeChain> listResult = standardPathLister.list(directory);
        Mockito.verify(standardPathLister, Mockito.times(1)).listProject(lastNode, null, directory);
        assertEquals(exampleListResult, listResult);
    }

    public void testListWithExperiment() {
        OpenBISListUtil listUtil = Mockito.mock(OpenBISListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));


        OpenBISSftpNode lastNode = OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.EXPERIMENT)
                .identifier(Optional.of("experiment_1")).build();
        OpenBISSftpNodeChain directory = OpenBISSftpNodeChain.concat(exampleBaseChain, lastNode);
        List<OpenBISSftpNodeChain> exampleListResult = List.of(
                new OpenBISSftpNodeChain(List.of(
                        OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.ROOT).build(),
                        OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                .identifier(Optional.of(StandardPathTranslator.EXPERIMENT_TYPE_LABEL))
                                .build(),
                        OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.EXPERIMENT)
                                .identifier(Optional.of("experiment_1"))
                                .build(),
                        OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                .identifier(Optional.of(StandardPathTranslator.SAMPLE_TYPE_LABEL))
                                .build()
                )));
        Mockito.doReturn(exampleListResult).when(standardPathLister).listExperiment(lastNode, null, directory);
        List<OpenBISSftpNodeChain> listResult = standardPathLister.list(directory);
        Mockito.verify(standardPathLister, Mockito.times(1)).listExperiment(lastNode, null, directory);
        assertEquals(exampleListResult, listResult);
    }

    public void testListWithAfsFile() {
        OpenBISListUtil listUtil = Mockito.mock(OpenBISListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));


        OpenBISSftpNode lastNode = OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.AFS_FILE)
                .afsFilePath(List.of("dir1")).build();
        OpenBISSftpNodeChain directory = OpenBISSftpNodeChain.concat(exampleBaseChain, lastNode);
        List<OpenBISSftpNodeChain> exampleListResult = List.of(
                new OpenBISSftpNodeChain(List.of(
                        OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.ROOT).build(),
                        OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                .identifier(Optional.of(StandardPathTranslator.FILE_TYPE_LABEL))
                                .build(),
                        OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.AFS_FILE)
                                .afsFilePath(List.of("dir1", "file2")).build()
                )));
        Mockito.doReturn(exampleListResult).when(standardPathLister).listFilesInAfsFileNode(lastNode, directory);
        List<OpenBISSftpNodeChain> listResult = standardPathLister.list(directory);
        Mockito.verify(standardPathLister, Mockito.times(1)).listFilesInAfsFileNode(lastNode, directory);
        assertEquals(exampleListResult, listResult);
    }


    public void testListWithSublevel() {
        OpenBISListUtil listUtil = Mockito.mock(OpenBISListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));


        OpenBISSftpNode lastNode = OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                .identifier(Optional.of("sublevel-label")).build();
        OpenBISSftpNode secondLastNode;
        OpenBISSftpNodeChain directory;
        List<OpenBISSftpNodeChain> listResult;
        Exception exception;
        List<OpenBISSftpNodeChain> exampleListResult = List.of(
                new OpenBISSftpNodeChain(List.of(
                        OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.ROOT).build(),
                        OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                .identifier(Optional.of(StandardPathTranslator.FILE_TYPE_LABEL))
                                .build(),
                        OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.AFS_FILE)
                                .afsFilePath(List.of("dir1", "file2")).build()
                )));

        //Sublevel of ROOT
        secondLastNode = OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.ROOT).build();
        directory = OpenBISSftpNodeChain.concat(
                exampleBaseChain,
                new OpenBISSftpNodeChain(List.of(secondLastNode, lastNode))
        );
        Mockito.doReturn(exampleListResult).when(standardPathLister).listRoot("sublevel-label", directory);
        listResult = standardPathLister.list(directory);
        Mockito.verify(standardPathLister, Mockito.times(1)).listRoot("sublevel-label", directory);
        assertEquals(exampleListResult, listResult);
        Mockito.clearInvocations(standardPathLister);

        //Sublevel of SPACE
        secondLastNode = OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SPACE)
                .identifier(Optional.of("space_1")).build();
        directory = OpenBISSftpNodeChain.concat(
                exampleBaseChain,
                new OpenBISSftpNodeChain(List.of(secondLastNode, lastNode))
        );
        Mockito.doReturn(exampleListResult).when(standardPathLister).listSpace(secondLastNode, "sublevel-label", directory);
        listResult = standardPathLister.list(directory);
        Mockito.verify(standardPathLister, Mockito.times(1)).listSpace(secondLastNode, "sublevel-label", directory);
        assertEquals(exampleListResult, listResult);
        Mockito.clearInvocations(standardPathLister);

        //Sublevel of PROJECT
        secondLastNode = OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.PROJECT)
                .identifier(Optional.of("project_1")).build();
        directory = OpenBISSftpNodeChain.concat(
                exampleBaseChain,
                new OpenBISSftpNodeChain(List.of(secondLastNode, lastNode))
        );
        Mockito.doReturn(exampleListResult).when(standardPathLister).listProject(secondLastNode, "sublevel-label", directory);
        listResult = standardPathLister.list(directory);
        Mockito.verify(standardPathLister, Mockito.times(1)).listProject(secondLastNode, "sublevel-label", directory);
        assertEquals(exampleListResult, listResult);
        Mockito.clearInvocations(standardPathLister);

        //Sublevel of EXPERIMENT
        secondLastNode = OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.EXPERIMENT)
                .identifier(Optional.of("experiment_1")).build();
        directory = OpenBISSftpNodeChain.concat(
                exampleBaseChain,
                new OpenBISSftpNodeChain(List.of(secondLastNode, lastNode))
        );
        Mockito.doReturn(exampleListResult).when(standardPathLister).listExperiment(secondLastNode, "sublevel-label", directory);
        listResult = standardPathLister.list(directory);
        Mockito.verify(standardPathLister, Mockito.times(1)).listExperiment(secondLastNode, "sublevel-label", directory);
        assertEquals(exampleListResult, listResult);
        Mockito.clearInvocations(standardPathLister);

        //Sublevel of FOLDER
        secondLastNode = OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.FOLDER)
                .identifier(Optional.of("folder_1")).build();
        directory = OpenBISSftpNodeChain.concat(
                exampleBaseChain,
                new OpenBISSftpNodeChain(List.of(secondLastNode, lastNode))
        );
        Mockito.doReturn(exampleListResult).when(standardPathLister).listFolder(secondLastNode, "sublevel-label", directory);
        listResult = standardPathLister.list(directory);
        Mockito.verify(standardPathLister, Mockito.times(1)).listFolder(secondLastNode, "sublevel-label", directory);
        assertEquals(exampleListResult, listResult);
        Mockito.clearInvocations(standardPathLister);

        //Sublevel of SAMPLE
        secondLastNode = OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SAMPLE)
                .identifier(Optional.of("sample_1")).build();
        directory = OpenBISSftpNodeChain.concat(
                exampleBaseChain,
                new OpenBISSftpNodeChain(List.of(secondLastNode, lastNode))
        );
        Mockito.doReturn(exampleListResult).when(standardPathLister).listSample(secondLastNode, "sublevel-label", directory);
        listResult = standardPathLister.list(directory);
        Mockito.verify(standardPathLister, Mockito.times(1)).listSample(secondLastNode, "sublevel-label", directory);
        assertEquals(exampleListResult, listResult);
        Mockito.clearInvocations(standardPathLister);

        //Sublevel of DATASET
        secondLastNode = OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.DATA_SET)
                .identifier(Optional.of("dataset_1")).build();
        directory = OpenBISSftpNodeChain.concat(
                exampleBaseChain,
                new OpenBISSftpNodeChain(List.of(secondLastNode, lastNode))
        );
        Mockito.doReturn(exampleListResult).when(standardPathLister).listDataSet(secondLastNode, "sublevel-label", directory);
        listResult = standardPathLister.list(directory);
        Mockito.verify(standardPathLister, Mockito.times(1)).listDataSet(secondLastNode, "sublevel-label", directory);
        assertEquals(exampleListResult, listResult);
        Mockito.clearInvocations(standardPathLister);

        //Sublevel of SUBLEVEL: not allowed
        secondLastNode = OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                .identifier(Optional.of("other")).build();
        directory = OpenBISSftpNodeChain.concat(
                exampleBaseChain,
                new OpenBISSftpNodeChain(List.of(secondLastNode, lastNode))
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
        secondLastNode = OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.AFS_FILE)
                .afsFilePath(List.of("dir1", "file2")).build();
        directory = OpenBISSftpNodeChain.concat(
                exampleBaseChain,
                new OpenBISSftpNodeChain(List.of(secondLastNode, lastNode))
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
        directory = new OpenBISSftpNodeChain(Collections.singletonList(lastNode));
        exception = null;
        try {
            standardPathLister.list(directory);
        } catch (Exception e) {
            exception = e;
        }
        assertEquals(IllegalArgumentException.class, exception.getClass());
        Mockito.clearInvocations(standardPathLister);
    }

    public void testReadAttributes() {
        OpenBISListUtil listUtil = Mockito.mock(OpenBISListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));

        List<OpenBISSftpNode.Type> abstractDirectoryTypes = List.of(
                OpenBISSftpNode.Type.ROOT,
                OpenBISSftpNode.Type.SPACE,
                OpenBISSftpNode.Type.PROJECT,
                OpenBISSftpNode.Type.EXPERIMENT,
                OpenBISSftpNode.Type.FOLDER,
                OpenBISSftpNode.Type.SAMPLE,
                OpenBISSftpNode.Type.DATA_SET,
                OpenBISSftpNode.Type.SUBLEVEL
        );

        for (OpenBISSftpNode.Type type : abstractDirectoryTypes) {
            OpenBISSftpNodeChain chain = OpenBISSftpNodeChain.concat(
                    exampleBaseChain,
                    OpenBISSftpNode.builder()
                        .type(type)
                        .identifier(Optional.of("id-fake"))
                        .build()
            );

            OpenBISSftpFileAttributes readAttributes = standardPathLister.readAttributes(chain);
            assertTrue(readAttributes.isDirectory());
            assertFalse(readAttributes.isRegularFile());
            assertFalse(readAttributes.isSymbolicLink());
            assertFalse(readAttributes.isOther());
            assertEquals(
                    OpenBISListUtil.getDefaultAbstractDirectoryAttributes().getPermissions(),
                    readAttributes.getPermissions()
            );
        }

        OpenBISSftpNodeChain chain = Mockito.spy(OpenBISSftpNodeChain.concat(
                exampleBaseChain,
                OpenBISSftpNode.builder()
                        .type(OpenBISSftpNode.Type.AFS_FILE)
                        .afsFilePath(List.of("dir-1", "dir-2", "file-3"))
                        .build()
        ));
        OpenBISSftpNode afsEntityNode = OpenBISSftpNode.builder()
                .type(OpenBISSftpNode.Type.DATA_SET)
                .identifier(Optional.of("fake-id"))
                .build();

        Mockito.doReturn(afsEntityNode).when(standardPathLister)
                .validateAndGetAfsEntityNodeFromAfsFileChain(chain);
        Mockito.doReturn("space-1").when(chain).lookUpSpaceCode();
        Mockito.doReturn("project-3").when(chain).lookUpProjectCode();
        String permId = "12345-12345";
        Mockito.doReturn(permId).when(listUtil).getAfsEntityPermId(
                afsEntityNode, "space-1", "project-3"
        );
        OpenBISSftpFileAttributes sampleAttributes = OpenBISListUtil.getDefaultAbstractDirectoryAttributes();
        Mockito.doReturn(Optional.of(sampleAttributes)).when(listUtil).getDefaultAfsFileAttributes(
                permId, "/dir-1/dir-2/file-3"
        );

        assertEquals(sampleAttributes, standardPathLister.readAttributes(chain));
        Mockito.verify(standardPathLister, Mockito.times(1))
                .validateAndGetAfsEntityNodeFromAfsFileChain(chain);
        Mockito.verify(chain, Mockito.times(1)).lookUpSpaceCode();
        Mockito.verify(chain, Mockito.times(1)).lookUpProjectCode();
        Mockito.verify(listUtil, Mockito.times(1)).getAfsEntityPermId(
                afsEntityNode, "space-1", "project-3"
        );
        Mockito.verify(listUtil, Mockito.times(1)).getDefaultAfsFileAttributes(
                permId, "/dir-1/dir-2/file-3"
        );
    }

    public void testListRoot() {
        OpenBISListUtil listUtil = Mockito.mock(OpenBISListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));

        assertEquals(List.of(
                OpenBISSftpNodeChain.concat(
                        exampleBaseChain,
                        OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.SPACE_TYPE_LABEL)
                )), standardPathLister.listRoot(null, exampleBaseChain));

        standardPathLister.listRoot(StandardPathTranslator.SPACE_TYPE_LABEL, exampleBaseChain);
        Mockito.verify(listUtil, Mockito.times(1)).getSpaces();
    }

    public void testListSpace() {
        OpenBISListUtil listUtil = Mockito.mock(OpenBISListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));

        OpenBISSftpNode spaceNode = OpenBISSftpNode.builder()
                .type(OpenBISSftpNode.Type.SPACE)
                .identifier(Optional.of("space-1")).build();

        assertEquals(List.of(
                OpenBISSftpNodeChain.concat(
                        exampleBaseChain,
                        OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.FOLDER_TYPE_LABEL)
                ),
                OpenBISSftpNodeChain.concat(
                        exampleBaseChain,
                        OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.SAMPLE_TYPE_LABEL)
                ),
                OpenBISSftpNodeChain.concat(
                        exampleBaseChain,
                        OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.PROJECT_TYPE_LABEL)
                )), standardPathLister.listSpace(spaceNode, null, exampleBaseChain));

        standardPathLister.listSpace(spaceNode, StandardPathTranslator.FOLDER_TYPE_LABEL, exampleBaseChain);
        Mockito.verify(standardPathLister, Mockito.times(1)).listSamplesOrFoldersInSpace(
                spaceNode, exampleBaseChain, true
        );
        Mockito.clearInvocations(standardPathLister);

        standardPathLister.listSpace(spaceNode, StandardPathTranslator.SAMPLE_TYPE_LABEL, exampleBaseChain);
        Mockito.verify(standardPathLister, Mockito.times(1)).listSamplesOrFoldersInSpace(
                spaceNode, exampleBaseChain, false
        );
        Mockito.clearInvocations(standardPathLister);

        standardPathLister.listSpace(spaceNode, StandardPathTranslator.PROJECT_TYPE_LABEL, exampleBaseChain);
        Mockito.verify(standardPathLister, Mockito.times(1)).listProjectsInSpace(
                spaceNode, exampleBaseChain
        );
        Mockito.clearInvocations(standardPathLister);
    }

    public void testListProjectsInSpace() {
        OpenBISListUtil listUtil = Mockito.mock(OpenBISListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));

        OpenBISSftpNode spaceNode = OpenBISSftpNode.builder()
                .type(OpenBISSftpNode.Type.SPACE)
                .identifier(Optional.of("space-1")).build();

        standardPathLister.listProjectsInSpace(spaceNode, exampleBaseChain);
        Mockito.verify(listUtil, Mockito.times(1)).getProjects(
                spaceNode.getIdentifier().get()
        );
    }

    public void testListSamplesOrFoldersInSpace() {
        OpenBISListUtil listUtil = Mockito.mock(OpenBISListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));

        OpenBISSftpNode spaceNode = OpenBISSftpNode.builder()
                .type(OpenBISSftpNode.Type.SPACE)
                .identifier(Optional.of("space-1")).build();

        SampleFetchOptions fetchOptions = new SampleFetchOptions();
        fetchOptions.withType();

        Sample sample1 = new Sample();
        sample1.setFetchOptions(fetchOptions);
        sample1.setCode("SAMPLE-1");
        SampleType sampleType1 = new SampleType();
        sampleType1.setCode("NONFOLDER");
        sample1.setType(sampleType1);

        Sample sample2 = new Sample();
        sample2.setFetchOptions(fetchOptions);
        sample2.setCode("FOLDER-2");
        SampleType sampleType2 = new SampleType();
        sampleType2.setCode("FOLDER");
        sample2.setType(sampleType2);

        List<Sample> returnedSamples = List.of(
                sample1,
                sample2
        );
        Mockito.doReturn(returnedSamples).when(listUtil)
                        .getSpaceSamples("space-1");

        List<OpenBISSftpNodeChain> openBISSftpNodeChainList;

        openBISSftpNodeChainList =
                standardPathLister.listSamplesOrFoldersInSpace(spaceNode, exampleBaseChain, true);
        Mockito.verify(listUtil, Mockito.times(1)).getSpaceSamples(
                spaceNode.getIdentifier().get()
        );
        assertEquals(1, openBISSftpNodeChainList.size());
        assertEquals(
                OpenBISSftpNode.Type.FOLDER,
                openBISSftpNodeChainList.getLast().getLast()
                        .get().getType());
        assertEquals(
                "FOLDER-2",
                openBISSftpNodeChainList.getLast().getLast()
                        .get().getIdentifier().get());
        Mockito.clearInvocations(listUtil);

        openBISSftpNodeChainList =
                standardPathLister.listSamplesOrFoldersInSpace(spaceNode, exampleBaseChain, false);
        Mockito.verify(listUtil, Mockito.times(1)).getSpaceSamples(
                spaceNode.getIdentifier().get()
        );
        assertEquals(1, openBISSftpNodeChainList.size());
        assertEquals(
                OpenBISSftpNode.Type.SAMPLE,
                openBISSftpNodeChainList.getLast().getLast()
                        .get().getType());
        assertEquals(
                "SAMPLE-1",
                openBISSftpNodeChainList.getLast().getLast()
                        .get().getIdentifier().get());
        Mockito.clearInvocations(listUtil);
    }

    public void testListSamplesOrFoldersInProject() {
        OpenBISListUtil listUtil = Mockito.mock(OpenBISListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));
        OpenBISSftpNodeChain baseChain = Mockito.spy(exampleBaseChain);
        Mockito.doReturn("space-1").when(baseChain).lookUpSpaceCode();

        OpenBISSftpNode projectNode = OpenBISSftpNode.builder()
                .type(OpenBISSftpNode.Type.PROJECT)
                .identifier(Optional.of("project-1")).build();

        SampleFetchOptions fetchOptions = new SampleFetchOptions();
        fetchOptions.withType();

        Sample sample1 = new Sample();
        sample1.setFetchOptions(fetchOptions);
        sample1.setCode("SAMPLE-1");
        SampleType sampleType1 = new SampleType();
        sampleType1.setCode("NONFOLDER");
        sample1.setType(sampleType1);

        Sample sample2 = new Sample();
        sample2.setFetchOptions(fetchOptions);
        sample2.setCode("FOLDER-2");
        SampleType sampleType2 = new SampleType();
        sampleType2.setCode("FOLDER");
        sample2.setType(sampleType2);

        List<Sample> returnedSamples = List.of(
                sample1,
                sample2
        );
        Mockito.doReturn(returnedSamples).when(listUtil)
                .getProjectSamples("space-1", "project-1");

        List<OpenBISSftpNodeChain> openBISSftpNodeChainList;

        openBISSftpNodeChainList =
                standardPathLister.listSamplesOrFoldersInProject(projectNode, baseChain, true);
        Mockito.verify(listUtil, Mockito.times(1)).getProjectSamples("space-1", "project-1");
        assertEquals(1, openBISSftpNodeChainList.size());
        assertEquals(
                OpenBISSftpNode.Type.FOLDER,
                openBISSftpNodeChainList.getLast().getLast()
                        .get().getType());
        assertEquals(
                "FOLDER-2",
                openBISSftpNodeChainList.getLast().getLast()
                        .get().getIdentifier().get());
        Mockito.clearInvocations(listUtil);

        openBISSftpNodeChainList =
                standardPathLister.listSamplesOrFoldersInProject(projectNode, baseChain, false);
        Mockito.verify(listUtil, Mockito.times(1)).getProjectSamples("space-1", "project-1");
        assertEquals(1, openBISSftpNodeChainList.size());
        assertEquals(
                OpenBISSftpNode.Type.SAMPLE,
                openBISSftpNodeChainList.getLast().getLast()
                        .get().getType());
        assertEquals(
                "SAMPLE-1",
                openBISSftpNodeChainList.getLast().getLast()
                        .get().getIdentifier().get());
        Mockito.clearInvocations(listUtil);
    }

    public void testListSample() {
        OpenBISListUtil listUtil = Mockito.mock(OpenBISListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));

        OpenBISSftpNode sampleNode = OpenBISSftpNode.builder()
                .type(OpenBISSftpNode.Type.SAMPLE)
                .identifier(Optional.of("sample-1")).build();

        assertEquals(List.of(
                OpenBISSftpNodeChain.concat(
                        exampleBaseChain,
                        OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.FOLDER_TYPE_LABEL)
                ),
                OpenBISSftpNodeChain.concat(
                        exampleBaseChain,
                        OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.SAMPLE_TYPE_LABEL)
                ),
                OpenBISSftpNodeChain.concat(
                        exampleBaseChain,
                        OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.DATA_SET_TYPE_LABEL)
                ),
                OpenBISSftpNodeChain.concat(
                        exampleBaseChain,
                        OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.FILE_TYPE_LABEL)
                )), standardPathLister.listSample(sampleNode, null, exampleBaseChain));

        standardPathLister.listSample(sampleNode, StandardPathTranslator.FOLDER_TYPE_LABEL, exampleBaseChain);
        Mockito.verify(standardPathLister, Mockito.times(1)).listSamplesOrFoldersInSample(
                sampleNode, exampleBaseChain, true
        );
        Mockito.clearInvocations(standardPathLister);

        standardPathLister.listSample(sampleNode, StandardPathTranslator.SAMPLE_TYPE_LABEL, exampleBaseChain);
        Mockito.verify(standardPathLister, Mockito.times(1)).listSamplesOrFoldersInSample(
                sampleNode, exampleBaseChain, false
        );
        Mockito.clearInvocations(standardPathLister);

        standardPathLister.listSample(sampleNode, StandardPathTranslator.DATA_SET_TYPE_LABEL, exampleBaseChain);
        Mockito.verify(standardPathLister, Mockito.times(1)).listDataSetsInSample(
                sampleNode, exampleBaseChain
        );

        standardPathLister.listSample(sampleNode, StandardPathTranslator.FILE_TYPE_LABEL, exampleBaseChain);
        Mockito.verify(standardPathLister, Mockito.times(1)).listFilesInSampleOrFolder(
                sampleNode, exampleBaseChain
        );
        Mockito.clearInvocations(standardPathLister);
    }

    public void testListFolder() {
        OpenBISListUtil listUtil = Mockito.mock(OpenBISListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));

        OpenBISSftpNode folderNode = OpenBISSftpNode.builder()
                .type(OpenBISSftpNode.Type.FOLDER)
                .identifier(Optional.of("folder-1")).build();

        assertEquals(List.of(
                OpenBISSftpNodeChain.concat(
                        exampleBaseChain,
                        OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.FOLDER_TYPE_LABEL)
                ),
                OpenBISSftpNodeChain.concat(
                        exampleBaseChain,
                        OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.SAMPLE_TYPE_LABEL)
                ),
                OpenBISSftpNodeChain.concat(
                        exampleBaseChain,
                        OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.FILE_TYPE_LABEL)
                )), standardPathLister.listFolder(folderNode, null, exampleBaseChain));

        standardPathLister.listFolder(folderNode, StandardPathTranslator.FOLDER_TYPE_LABEL, exampleBaseChain);
        Mockito.verify(standardPathLister, Mockito.times(1)).listSamplesOrFoldersInSample(
                folderNode, exampleBaseChain, true
        );
        Mockito.clearInvocations(standardPathLister);

        standardPathLister.listFolder(folderNode, StandardPathTranslator.SAMPLE_TYPE_LABEL, exampleBaseChain);
        Mockito.verify(standardPathLister, Mockito.times(1)).listSamplesOrFoldersInSample(
                folderNode, exampleBaseChain, false
        );
        Mockito.clearInvocations(standardPathLister);

        standardPathLister.listFolder(folderNode, StandardPathTranslator.FILE_TYPE_LABEL, exampleBaseChain);
        Mockito.verify(standardPathLister, Mockito.times(1)).listFilesInSampleOrFolder(
                folderNode, exampleBaseChain
        );
        Mockito.clearInvocations(standardPathLister);
    }

    public void testListSamplesOrFoldersInSample() {
        OpenBISListUtil listUtil = Mockito.mock(OpenBISListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));
        OpenBISSftpNodeChain baseChain = Mockito.spy(exampleBaseChain);
        Mockito.doReturn("space-1").when(baseChain).lookUpSpaceCode();
        Mockito.doReturn("project-1").when(baseChain).lookUpProjectCode();

        OpenBISSftpNode sampleNode = OpenBISSftpNode.builder()
                .type(OpenBISSftpNode.Type.SAMPLE)
                .identifier(Optional.of("sample-1")).build();

        SampleFetchOptions fetchOptions = new SampleFetchOptions();
        fetchOptions.withType();

        Sample sample1 = new Sample();
        sample1.setFetchOptions(fetchOptions);
        sample1.setCode("SAMPLE-1");
        SampleType sampleType1 = new SampleType();
        sampleType1.setCode("NONFOLDER");
        sample1.setType(sampleType1);

        Sample sample2 = new Sample();
        sample2.setFetchOptions(fetchOptions);
        sample2.setCode("FOLDER-2");
        SampleType sampleType2 = new SampleType();
        sampleType2.setCode("FOLDER");
        sample2.setType(sampleType2);

        List<Sample> returnedSamples = List.of(
                sample1,
                sample2
        );
        Mockito.doReturn(returnedSamples).when(listUtil)
                .getSampleChildren("space-1", "project-1", "sample-1");

        List<OpenBISSftpNodeChain> openBISSftpNodeChainList;

        openBISSftpNodeChainList =
                standardPathLister.listSamplesOrFoldersInSample(sampleNode, baseChain, true);
        Mockito.verify(listUtil, Mockito.times(1)).getSampleChildren("space-1", "project-1", "sample-1");
        assertEquals(1, openBISSftpNodeChainList.size());
        assertEquals(
                OpenBISSftpNode.Type.FOLDER,
                openBISSftpNodeChainList.getLast().getLast()
                        .get().getType());
        assertEquals(
                "FOLDER-2",
                openBISSftpNodeChainList.getLast().getLast()
                        .get().getIdentifier().get());
        Mockito.clearInvocations(listUtil);

        openBISSftpNodeChainList =
                standardPathLister.listSamplesOrFoldersInSample(sampleNode, baseChain, false);
        Mockito.verify(listUtil, Mockito.times(1)).getSampleChildren("space-1", "project-1", "sample-1");
        assertEquals(1, openBISSftpNodeChainList.size());
        assertEquals(
                OpenBISSftpNode.Type.SAMPLE,
                openBISSftpNodeChainList.getLast().getLast()
                        .get().getType());
        assertEquals(
                "SAMPLE-1",
                openBISSftpNodeChainList.getLast().getLast()
                        .get().getIdentifier().get());
        Mockito.clearInvocations(listUtil);
    }

    public void testListDataSetsInSample() {
        OpenBISListUtil listUtil = Mockito.mock(OpenBISListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));
        OpenBISSftpNodeChain baseChain = Mockito.spy(exampleBaseChain);
        Mockito.doReturn("space-1").when(baseChain).lookUpSpaceCode();
        Mockito.doReturn("project-1").when(baseChain).lookUpProjectCode();

        OpenBISSftpNode sampleNode = OpenBISSftpNode.builder()
                .type(OpenBISSftpNode.Type.SAMPLE)
                .identifier(Optional.of("sample-1")).build();

        standardPathLister.listDataSetsInSample(sampleNode, baseChain);
        Mockito.verify(listUtil, Mockito.times(1)).getSampleDatasets("space-1", "project-1", "sample-1");
    }

    public void testListFilesInSampleOrFolder() {
        OpenBISListUtil listUtil = Mockito.mock(OpenBISListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));
        OpenBISSftpNodeChain baseChain = Mockito.spy(exampleBaseChain);
        Mockito.doReturn("space-1").when(baseChain).lookUpSpaceCode();
        Mockito.doReturn("project-1").when(baseChain).lookUpProjectCode();

        OpenBISSftpNode sampleNode = OpenBISSftpNode.builder()
                .type(OpenBISSftpNode.Type.SAMPLE)
                .identifier(Optional.of("sample-1")).build();

        Mockito.doReturn("afs-perm-id-1").when(listUtil)
                        .getAfsEntityPermId(sampleNode, "space-1", "project-1");
        Mockito.doReturn(new File[0]).when(listUtil).listAfsFiles(Mockito.anyString(), Mockito.anyString());

        standardPathLister.listFilesInSampleOrFolder(sampleNode, baseChain);
        Mockito.verify(listUtil, Mockito.times(1)).listAfsFiles("afs-perm-id-1", "/");
    }

    public void testListFilesInDataSet() {
        OpenBISListUtil listUtil = Mockito.mock(OpenBISListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));
        OpenBISSftpNodeChain baseChain = Mockito.spy(exampleBaseChain);
        Mockito.doReturn("space-1").when(baseChain).lookUpSpaceCode();
        Mockito.doReturn("project-1").when(baseChain).lookUpProjectCode();

        OpenBISSftpNode datasetNode = OpenBISSftpNode.builder()
                .type(OpenBISSftpNode.Type.DATA_SET)
                .identifier(Optional.of("sample-1")).build();

        Mockito.doReturn("afs-perm-id-1").when(listUtil)
                .getAfsEntityPermId(datasetNode, "space-1", "project-1");
        Mockito.doReturn(new File[0]).when(listUtil).listAfsFiles(Mockito.anyString(), Mockito.anyString());

        standardPathLister.listFilesInDataSet(datasetNode, baseChain);
        Mockito.verify(listUtil, Mockito.times(1)).listAfsFiles("afs-perm-id-1", "/");
    }

    public void testListFilesInExperiment() {
        OpenBISListUtil listUtil = Mockito.mock(OpenBISListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));
        OpenBISSftpNodeChain baseChain = Mockito.spy(exampleBaseChain);
        Mockito.doReturn("space-1").when(baseChain).lookUpSpaceCode();
        Mockito.doReturn("project-1").when(baseChain).lookUpProjectCode();

        OpenBISSftpNode experimentNode = OpenBISSftpNode.builder()
                .type(OpenBISSftpNode.Type.EXPERIMENT)
                .identifier(Optional.of("experiment-1")).build();

        Mockito.doReturn("afs-perm-id-1").when(listUtil)
                .getAfsEntityPermId(experimentNode, "space-1", "project-1");
        Mockito.doReturn(new File[0]).when(listUtil).listAfsFiles(Mockito.anyString(), Mockito.anyString());

        standardPathLister.listFilesInExperiment(experimentNode, baseChain);
        Mockito.verify(listUtil, Mockito.times(1)).listAfsFiles("afs-perm-id-1", "/");
    }

    public void testListDataSet() {
        OpenBISListUtil listUtil = Mockito.mock(OpenBISListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));

        OpenBISSftpNode datasetNode = OpenBISSftpNode.builder()
                .type(OpenBISSftpNode.Type.DATA_SET)
                .identifier(Optional.of("dataset-1")).build();

        assertEquals(List.of(
                OpenBISSftpNodeChain.concat(
                        exampleBaseChain,
                        OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.FILE_TYPE_LABEL)
                )
        ), standardPathLister.listDataSet(datasetNode, null, exampleBaseChain));

        standardPathLister.listDataSet(datasetNode, StandardPathTranslator.FILE_TYPE_LABEL, exampleBaseChain);
        Mockito.verify(standardPathLister, Mockito.times(1)).listFilesInDataSet(
                datasetNode, exampleBaseChain
        );
    }

    public void testListProject() {
        OpenBISListUtil listUtil = Mockito.mock(OpenBISListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));

        OpenBISSftpNode projectNode = OpenBISSftpNode.builder()
                .type(OpenBISSftpNode.Type.PROJECT)
                .identifier(Optional.of("project-1")).build();

        assertEquals(List.of(
                OpenBISSftpNodeChain.concat(
                        exampleBaseChain,
                        OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.FOLDER_TYPE_LABEL)
                ),
                OpenBISSftpNodeChain.concat(
                        exampleBaseChain,
                        OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.SAMPLE_TYPE_LABEL)
                ),
                OpenBISSftpNodeChain.concat(
                        exampleBaseChain,
                        OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.EXPERIMENT_TYPE_LABEL)
                )), standardPathLister.listProject(projectNode, null, exampleBaseChain));

        standardPathLister.listProject(projectNode, StandardPathTranslator.FOLDER_TYPE_LABEL, exampleBaseChain);
        Mockito.verify(standardPathLister, Mockito.times(1)).listSamplesOrFoldersInProject(
                projectNode, exampleBaseChain, true
        );
        Mockito.clearInvocations(standardPathLister);

        standardPathLister.listProject(projectNode, StandardPathTranslator.SAMPLE_TYPE_LABEL, exampleBaseChain);
        Mockito.verify(standardPathLister, Mockito.times(1)).listSamplesOrFoldersInProject(
                projectNode, exampleBaseChain, false
        );
        Mockito.clearInvocations(standardPathLister);

        standardPathLister.listProject(projectNode, StandardPathTranslator.EXPERIMENT_TYPE_LABEL, exampleBaseChain);
        Mockito.verify(standardPathLister, Mockito.times(1)).listExperimentsInProject(
                projectNode, exampleBaseChain
        );
        Mockito.clearInvocations(standardPathLister);
    }

    public void testListExperimentsInProject() {
        OpenBISListUtil listUtil = Mockito.mock(OpenBISListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));
        OpenBISSftpNodeChain baseChain = Mockito.spy(exampleBaseChain);
        Mockito.doReturn("space-1").when(baseChain).lookUpSpaceCode();

        OpenBISSftpNode projectNode = OpenBISSftpNode.builder()
                .type(OpenBISSftpNode.Type.PROJECT)
                .identifier(Optional.of("project-1")).build();

        standardPathLister.listExperimentsInProject(projectNode, baseChain);
        Mockito.verify(listUtil, Mockito.times(1)).getExperiments(
                "space-1", "project-1"
        );
    }

    public void testListExperiment() {
        OpenBISListUtil listUtil = Mockito.mock(OpenBISListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));
        OpenBISSftpNodeChain baseChain = Mockito.spy(exampleBaseChain);
        Mockito.doReturn("space-1").when(baseChain).lookUpSpaceCode();
        Mockito.doReturn("project-1").when(baseChain).lookUpProjectCode();

        OpenBISSftpNode experimentNode = OpenBISSftpNode.builder()
                .type(OpenBISSftpNode.Type.EXPERIMENT)
                .identifier(Optional.of("experiment-1")).build();

        assertEquals(List.of(
                OpenBISSftpNodeChain.concat(
                        baseChain,
                        OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.FOLDER_TYPE_LABEL)
                ),
                OpenBISSftpNodeChain.concat(
                        baseChain,
                        OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.SAMPLE_TYPE_LABEL)
                ),
                OpenBISSftpNodeChain.concat(
                        baseChain,
                        OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.FILE_TYPE_LABEL)
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

        standardPathLister.listExperiment(experimentNode, StandardPathTranslator.FILE_TYPE_LABEL, baseChain);
        Mockito.verify(standardPathLister, Mockito.times(1)).listFilesInExperiment(
                experimentNode, baseChain
        );
        Mockito.clearInvocations(standardPathLister);
    }

    public void testListSamplesOrFoldersInExperiment() {
        OpenBISListUtil listUtil = Mockito.mock(OpenBISListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));
        OpenBISSftpNodeChain baseChain = Mockito.spy(exampleBaseChain);
        Mockito.doReturn("space-1").when(baseChain).lookUpSpaceCode();
        Mockito.doReturn("project-1").when(baseChain).lookUpProjectCode();

        OpenBISSftpNode experimentNode = OpenBISSftpNode.builder()
                .type(OpenBISSftpNode.Type.EXPERIMENT)
                .identifier(Optional.of("experiment-1")).build();

        SampleFetchOptions fetchOptions = new SampleFetchOptions();
        fetchOptions.withType();

        Sample sample1 = new Sample();
        sample1.setFetchOptions(fetchOptions);
        sample1.setCode("SAMPLE-1");
        SampleType sampleType1 = new SampleType();
        sampleType1.setCode("NONFOLDER");
        sample1.setType(sampleType1);

        Sample sample2 = new Sample();
        sample2.setFetchOptions(fetchOptions);
        sample2.setCode("FOLDER-2");
        SampleType sampleType2 = new SampleType();
        sampleType2.setCode("FOLDER");
        sample2.setType(sampleType2);

        List<Sample> returnedSamples = List.of(
                sample1,
                sample2
        );
        Mockito.doReturn(returnedSamples).when(listUtil)
                .getExperimentSamples("space-1", "project-1", "experiment-1");

        List<OpenBISSftpNodeChain> openBISSftpNodeChainList;

        openBISSftpNodeChainList =
                standardPathLister.listSamplesOrFoldersInExperiment(experimentNode, baseChain, true);
        Mockito.verify(listUtil, Mockito.times(1)).getExperimentSamples("space-1", "project-1", "experiment-1");
        assertEquals(1, openBISSftpNodeChainList.size());
        assertEquals(
                OpenBISSftpNode.Type.FOLDER,
                openBISSftpNodeChainList.getLast().getLast()
                        .get().getType());
        assertEquals(
                "FOLDER-2",
                openBISSftpNodeChainList.getLast().getLast()
                        .get().getIdentifier().get());
        Mockito.clearInvocations(listUtil);

        openBISSftpNodeChainList =
                standardPathLister.listSamplesOrFoldersInExperiment(experimentNode, baseChain, false);
        Mockito.verify(listUtil, Mockito.times(1)).getExperimentSamples("space-1", "project-1", "experiment-1");
        assertEquals(1, openBISSftpNodeChainList.size());
        assertEquals(
                OpenBISSftpNode.Type.SAMPLE,
                openBISSftpNodeChainList.getLast().getLast()
                        .get().getType());
        assertEquals(
                "SAMPLE-1",
                openBISSftpNodeChainList.getLast().getLast()
                        .get().getIdentifier().get());
        Mockito.clearInvocations(listUtil);
    }

    public void testListFilesInAfsFileNode() {
        OpenBISListUtil listUtil = Mockito.mock(OpenBISListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));
        OpenBISSftpNodeChain baseChain = Mockito.spy(exampleBaseChain);
        Mockito.doReturn("space-1").when(baseChain).lookUpSpaceCode();
        Mockito.doReturn("project-1").when(baseChain).lookUpProjectCode();

        OpenBISSftpNode afsEntityNode = OpenBISSftpNode.builder()
                .type(OpenBISSftpNode.Type.SAMPLE)
                .identifier(Optional.of("sample-1")).build();

        Mockito.doReturn(afsEntityNode).when(standardPathLister)
                .validateAndGetAfsEntityNodeFromAfsFileChain(baseChain);
        Mockito.doReturn("afs-perm-id-1").when(listUtil)
                .getAfsEntityPermId(afsEntityNode, "space-1", "project-1");

        OpenBISSftpNode afsFileNode = OpenBISSftpNode.builder()
                .type(OpenBISSftpNode.Type.AFS_FILE)
                .afsFilePath(List.of("dir-1", "dir-2", "file-12")).build();


        Mockito.doReturn(new File[0]).when(listUtil).listAfsFiles(Mockito.anyString(), Mockito.anyString());

        standardPathLister.listFilesInAfsFileNode(afsFileNode, baseChain);
        Mockito.verify(listUtil, Mockito.times(1)).listAfsFiles("afs-perm-id-1", "/dir-1/dir-2/file-12");
    }

    public void testValidateAndGetAfsEntityNodeFromAfsFileChain() {
        OpenBISListUtil listUtil = Mockito.mock(OpenBISListUtil.class);
        StandardPathLister standardPathLister = Mockito.spy(new StandardPathLister(listUtil));

        OpenBISSftpNodeChain shortChain = new OpenBISSftpNodeChain(
                List.of(
                        OpenBISSftpNode.builder()
                                .type(OpenBISSftpNode.Type.SUBLEVEL)
                                .identifier(Optional.of(StandardPathTranslator.FILE_TYPE_LABEL))
                                .build(),
                        OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.AFS_FILE)
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

        for (OpenBISSftpNode.Type notAdmittedTypeForAfsEntity : List.of(
                OpenBISSftpNode.Type.ROOT,
                OpenBISSftpNode.Type.SPACE,
                OpenBISSftpNode.Type.PROJECT,
                OpenBISSftpNode.Type.AFS_FILE,
                OpenBISSftpNode.Type.SUBLEVEL
        )) {
            OpenBISSftpNodeChain noAfsEntity = new OpenBISSftpNodeChain(
                    List.of(
                            OpenBISSftpNode.builder()
                                    .type(notAdmittedTypeForAfsEntity)
                                    .identifier(Optional.of("id-fake"))
                                    .build(),
                            OpenBISSftpNode.builder()
                                    .type(OpenBISSftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.FILE_TYPE_LABEL))
                                    .build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.AFS_FILE)
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

        for (OpenBISSftpNode.Type admittedTypeForAfsEntity : List.of(
                OpenBISSftpNode.Type.SAMPLE,
                OpenBISSftpNode.Type.FOLDER,
                OpenBISSftpNode.Type.EXPERIMENT,
                OpenBISSftpNode.Type.DATA_SET
        )) {
            OpenBISSftpNodeChain goodAfsEntity = new OpenBISSftpNodeChain(
                    List.of(
                            OpenBISSftpNode.builder()
                                    .type(admittedTypeForAfsEntity)
                                    .identifier(Optional.of("id-fake"))
                                    .build(),
                            OpenBISSftpNode.builder()
                                    .type(OpenBISSftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.FILE_TYPE_LABEL))
                                    .build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.AFS_FILE)
                                    .afsFilePath(List.of("dir1", "dir2", "file"))
                                    .build()
                    )
            );

            assertEquals(
                OpenBISSftpNode.builder()
                    .type(admittedTypeForAfsEntity)
                    .identifier(Optional.of("id-fake"))
                    .build(),
                standardPathLister.validateAndGetAfsEntityNodeFromAfsFileChain(goodAfsEntity)
            );
        }
    }
}