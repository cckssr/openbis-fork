package ch.ethz.sis.afssftp.filesystemview.impl.standard;

import ch.ethz.sis.afssftp.filesystemview.FtpPathTranslator;
import ch.ethz.sis.afssftp.filesystemview.OpenBISSftpNode;
import ch.ethz.sis.afssftp.filesystemview.OpenBISSftpNodeChain;
import junit.framework.TestCase;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class StandardPathTranslatorTest extends TestCase {
    public void testToPathSegments() throws Exception {
        StandardPathTranslator standardPathTranslator = new StandardPathTranslator();
        assertEquals(Collections.emptyList(), standardPathTranslator.toPathSegments(new OpenBISSftpNodeChain(
                List.of(
                        OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.ROOT).build()
                )
        )));
        assertEquals(List.of("spaces","space_1"), standardPathTranslator.toPathSegments(new OpenBISSftpNodeChain(
                List.of(
                        OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.ROOT).build(),
                        OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                .identifier(Optional.of(StandardPathTranslator.SPACE_TYPE_LABEL)).build(),
                        OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SPACE)
                                .identifier(Optional.of("space_1")).build()
                )
        )));
        assertEquals(List.of("spaces","space_1", "projects", "project_1"),
                standardPathTranslator.toPathSegments(new OpenBISSftpNodeChain(
                    List.of(
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.ROOT).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.SPACE_TYPE_LABEL)).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SPACE)
                                    .identifier(Optional.of("space_1")).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.PROJECT_TYPE_LABEL)).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.PROJECT)
                                    .identifier(Optional.of("project_1")).build()
                    )
        )));
        assertEquals(List.of("spaces","space_1", "projects", "project_1", "experiments", "experiment_1"),
                standardPathTranslator.toPathSegments(new OpenBISSftpNodeChain(
                    List.of(
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.ROOT).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.SPACE_TYPE_LABEL)).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SPACE)
                                    .identifier(Optional.of("space_1")).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.PROJECT_TYPE_LABEL)).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.PROJECT)
                                    .identifier(Optional.of("project_1")).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.EXPERIMENT_TYPE_LABEL)).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.EXPERIMENT)
                                    .identifier(Optional.of("experiment_1")).build()
                    )
        )));
        assertEquals(List.of("spaces","space_1", "projects", "project_1", "experiments", "experiment_1", "folders", "folder_1"),
                standardPathTranslator.toPathSegments(new OpenBISSftpNodeChain(
                    List.of(
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.ROOT).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.SPACE_TYPE_LABEL)).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SPACE)
                                    .identifier(Optional.of("space_1")).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.PROJECT_TYPE_LABEL)).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.PROJECT)
                                    .identifier(Optional.of("project_1")).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.EXPERIMENT_TYPE_LABEL)).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.EXPERIMENT)
                                    .identifier(Optional.of("experiment_1")).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.FOLDER_TYPE_LABEL)).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.FOLDER)
                                    .identifier(Optional.of("folder_1")).build()
                    )
        )));
        assertEquals(List.of("spaces","space_1", "projects", "project_1",
                        "experiments", "experiment_1", "folders", "folder_1", "samples", "sample_1"),
                standardPathTranslator.toPathSegments(new OpenBISSftpNodeChain(
                    List.of(
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.ROOT).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.SPACE_TYPE_LABEL)).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SPACE)
                                    .identifier(Optional.of("space_1")).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.PROJECT_TYPE_LABEL)).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.PROJECT)
                                    .identifier(Optional.of("project_1")).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.EXPERIMENT_TYPE_LABEL)).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.EXPERIMENT)
                                    .identifier(Optional.of("experiment_1")).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.FOLDER_TYPE_LABEL)).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.FOLDER)
                                    .identifier(Optional.of("folder_1")).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.SAMPLE_TYPE_LABEL)).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SAMPLE)
                                    .identifier(Optional.of("sample_1")).build()
                    )
        )));
        assertEquals(List.of("spaces","space_1", "projects", "project_1",
                        "experiments", "experiment_1", "folders", "folder_1", "samples", "sample_1",
                        "datasets", "dataset_1"),
                standardPathTranslator.toPathSegments(new OpenBISSftpNodeChain(
                    List.of(
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.ROOT).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.SPACE_TYPE_LABEL)).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SPACE)
                                    .identifier(Optional.of("space_1")).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.PROJECT_TYPE_LABEL)).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.PROJECT)
                                    .identifier(Optional.of("project_1")).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.EXPERIMENT_TYPE_LABEL)).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.EXPERIMENT)
                                    .identifier(Optional.of("experiment_1")).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.FOLDER_TYPE_LABEL)).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.FOLDER)
                                    .identifier(Optional.of("folder_1")).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.SAMPLE_TYPE_LABEL)).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SAMPLE)
                                    .identifier(Optional.of("sample_1")).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.DATA_SET_TYPE_LABEL)).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.DATA_SET)
                                    .identifier(Optional.of("dataset_1")).build()
                    )
        )));
        assertEquals(List.of("spaces","space_1", "projects", "project_1",
                        "experiments", "experiment_1", "folders", "folder_1", "samples", "sample_1",
                        "datasets", "dataset_1", "files", "dir1", "dir2", "file3"),
                standardPathTranslator.toPathSegments(new OpenBISSftpNodeChain(
                    List.of(
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.ROOT).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.SPACE_TYPE_LABEL)).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SPACE)
                                    .identifier(Optional.of("space_1")).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.PROJECT_TYPE_LABEL)).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.PROJECT)
                                    .identifier(Optional.of("project_1")).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.EXPERIMENT_TYPE_LABEL)).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.EXPERIMENT)
                                    .identifier(Optional.of("experiment_1")).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.FOLDER_TYPE_LABEL)).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.FOLDER)
                                    .identifier(Optional.of("folder_1")).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.SAMPLE_TYPE_LABEL)).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SAMPLE)
                                    .identifier(Optional.of("sample_1")).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.DATA_SET_TYPE_LABEL)).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.DATA_SET)
                                    .identifier(Optional.of("dataset_1")).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.FILE_TYPE_LABEL)).build(),
                            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.AFS_FILE)
                                    .afsFilePath(List.of("dir1", "dir2", "file3")).build()
                    )
        )));

        //More generally: test that each node is correctly translated
        List<OpenBISSftpNode> nodes = List.of(
                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.ROOT).build(),
                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                        .identifier(Optional.of(StandardPathTranslator.SPACE_TYPE_LABEL)).build(),
                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SPACE)
                        .identifier(Optional.of("space_1")).build(),
                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                        .identifier(Optional.of(StandardPathTranslator.PROJECT_TYPE_LABEL)).build(),
                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.PROJECT)
                        .identifier(Optional.of("project_1")).build(),
                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                        .identifier(Optional.of(StandardPathTranslator.EXPERIMENT_TYPE_LABEL)).build(),
                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.EXPERIMENT)
                        .identifier(Optional.of("experiment_1")).build(),
                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                        .identifier(Optional.of(StandardPathTranslator.FOLDER_TYPE_LABEL)).build(),
                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.FOLDER)
                        .identifier(Optional.of("folder_1")).build(),
                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                        .identifier(Optional.of(StandardPathTranslator.SAMPLE_TYPE_LABEL)).build(),
                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SAMPLE)
                        .identifier(Optional.of("sample_1")).build(),
                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                        .identifier(Optional.of(StandardPathTranslator.DATA_SET_TYPE_LABEL)).build(),
                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.DATA_SET)
                        .identifier(Optional.of("dataset_1")).build(),
                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                        .identifier(Optional.of(StandardPathTranslator.FILE_TYPE_LABEL)).build(),
                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.AFS_FILE)
                        .afsFilePath(List.of("dir1", "dir2", "file3")).build()
        );
        assertEquals(
                nodes.stream().map(StandardPathTranslator::fromFtpNode)
                    .flatMap( List::stream ).toList(),
                standardPathTranslator.toPathSegments(new OpenBISSftpNodeChain(
                    nodes
        )));
    }

    public void testFromPathSegments() throws Exception {
        StandardPathTranslator standardPathTranslator = new StandardPathTranslator();
        assertEquals(new OpenBISSftpNodeChain(
                List.of(
                        OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.ROOT).build()
                )
        ), standardPathTranslator.fromPathSegments(Collections.emptyList()));
        assertEquals(new OpenBISSftpNodeChain(
                List.of(
                        OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.ROOT).build(),
                        OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                .identifier(Optional.of(StandardPathTranslator.SPACE_TYPE_LABEL)).build(),
                        OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SPACE)
                                .identifier(Optional.of("space_1")).build()
                )
        ), standardPathTranslator.fromPathSegments(List.of("spaces","space_1")));
        assertEquals(new OpenBISSftpNodeChain(
                        List.of(
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.ROOT).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.SPACE_TYPE_LABEL)).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SPACE)
                                        .identifier(Optional.of("space_1")).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.PROJECT_TYPE_LABEL)).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.PROJECT)
                                        .identifier(Optional.of("project_1")).build()
                        )
                ),
                standardPathTranslator.fromPathSegments(List.of("spaces","space_1", "projects", "project_1")));
        assertEquals(new OpenBISSftpNodeChain(
                        List.of(
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.ROOT).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.SPACE_TYPE_LABEL)).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SPACE)
                                        .identifier(Optional.of("space_1")).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.PROJECT_TYPE_LABEL)).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.PROJECT)
                                        .identifier(Optional.of("project_1")).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.EXPERIMENT_TYPE_LABEL)).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.EXPERIMENT)
                                        .identifier(Optional.of("experiment_1")).build()
                        )
                ),
                standardPathTranslator.fromPathSegments(List.of("spaces","space_1", "projects", "project_1", "experiments", "experiment_1")));
        assertEquals(new OpenBISSftpNodeChain(
                        List.of(
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.ROOT).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.SPACE_TYPE_LABEL)).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SPACE)
                                        .identifier(Optional.of("space_1")).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.PROJECT_TYPE_LABEL)).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.PROJECT)
                                        .identifier(Optional.of("project_1")).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.EXPERIMENT_TYPE_LABEL)).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.EXPERIMENT)
                                        .identifier(Optional.of("experiment_1")).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.FOLDER_TYPE_LABEL)).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.FOLDER)
                                        .identifier(Optional.of("folder_1")).build()
                        )
                ),
                standardPathTranslator.fromPathSegments(List.of("spaces","space_1", "projects", "project_1", "experiments", "experiment_1", "folders", "folder_1")));
        assertEquals(new OpenBISSftpNodeChain(
                        List.of(
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.ROOT).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.SPACE_TYPE_LABEL)).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SPACE)
                                        .identifier(Optional.of("space_1")).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.PROJECT_TYPE_LABEL)).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.PROJECT)
                                        .identifier(Optional.of("project_1")).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.EXPERIMENT_TYPE_LABEL)).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.EXPERIMENT)
                                        .identifier(Optional.of("experiment_1")).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.FOLDER_TYPE_LABEL)).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.FOLDER)
                                        .identifier(Optional.of("folder_1")).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.SAMPLE_TYPE_LABEL)).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SAMPLE)
                                        .identifier(Optional.of("sample_1")).build()
                        )
                ),
                standardPathTranslator.fromPathSegments(List.of("spaces","space_1", "projects", "project_1",
                        "experiments", "experiment_1", "folders", "folder_1", "samples", "sample_1")));
        assertEquals(new OpenBISSftpNodeChain(
                        List.of(
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.ROOT).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.SPACE_TYPE_LABEL)).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SPACE)
                                        .identifier(Optional.of("space_1")).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.PROJECT_TYPE_LABEL)).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.PROJECT)
                                        .identifier(Optional.of("project_1")).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.EXPERIMENT_TYPE_LABEL)).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.EXPERIMENT)
                                        .identifier(Optional.of("experiment_1")).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.FOLDER_TYPE_LABEL)).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.FOLDER)
                                        .identifier(Optional.of("folder_1")).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.SAMPLE_TYPE_LABEL)).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SAMPLE)
                                        .identifier(Optional.of("sample_1")).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.DATA_SET_TYPE_LABEL)).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.DATA_SET)
                                        .identifier(Optional.of("dataset_1")).build()
                        )
                ),
                standardPathTranslator.fromPathSegments(List.of("spaces","space_1", "projects", "project_1",
                        "experiments", "experiment_1", "folders", "folder_1", "samples", "sample_1",
                        "datasets", "dataset_1")));
        assertEquals(new OpenBISSftpNodeChain(
                        List.of(
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.ROOT).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.SPACE_TYPE_LABEL)).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SPACE)
                                        .identifier(Optional.of("space_1")).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.PROJECT_TYPE_LABEL)).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.PROJECT)
                                        .identifier(Optional.of("project_1")).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.EXPERIMENT_TYPE_LABEL)).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.EXPERIMENT)
                                        .identifier(Optional.of("experiment_1")).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.FOLDER_TYPE_LABEL)).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.FOLDER)
                                        .identifier(Optional.of("folder_1")).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.SAMPLE_TYPE_LABEL)).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SAMPLE)
                                        .identifier(Optional.of("sample_1")).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.DATA_SET_TYPE_LABEL)).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.DATA_SET)
                                        .identifier(Optional.of("dataset_1")).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.FILE_TYPE_LABEL)).build(),
                                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.AFS_FILE)
                                        .afsFilePath(List.of("dir1", "dir2", "file3")).build()
                        )
                ),
                standardPathTranslator.fromPathSegments(List.of("spaces","space_1", "projects", "project_1",
                        "experiments", "experiment_1", "folders", "folder_1", "samples", "sample_1",
                        "datasets", "dataset_1", "files", "dir1", "dir2", "file3")));
    }

    public void testFromTypeAndIdentifier() {
        List<OpenBISSftpNode.Type> admittedTypes = List.of(
                OpenBISSftpNode.Type.SPACE,
                OpenBISSftpNode.Type.PROJECT,
                OpenBISSftpNode.Type.EXPERIMENT,
                OpenBISSftpNode.Type.FOLDER,
                OpenBISSftpNode.Type.SAMPLE,
                OpenBISSftpNode.Type.DATA_SET
        );
        String identifier = "fake-id";
        for (OpenBISSftpNode.Type type : admittedTypes) {
            assertEquals(
                    List.of(
                            OpenBISSftpNode.builder()
                                    .type(OpenBISSftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.toTypeLabel(type)))
                                    .build(),
                            OpenBISSftpNode.builder()
                                    .type(type)
                                    .identifier(Optional.of(identifier))
                                    .build()
                    ),
                    StandardPathTranslator.fromTypeAndIdentifier(type, identifier)
            );
        }
    }

    public void testCreateAfsFileTypeWithPathTokens() {
        List<String> pathTokens = List.of("abc", "cfd", "dir1");
        OpenBISSftpNode afsNode = StandardPathTranslator.createAfsFileTypeWithPathTokens(pathTokens);
        assertEquals(pathTokens, afsNode.getAfsFilePath());
        assertEquals(OpenBISSftpNode.Type.AFS_FILE, afsNode.getType());
        assertEquals(Optional.empty(), afsNode.getIdentifier());
    }

    public void testToTypeLabel() {
        assertNull(StandardPathTranslator.toTypeLabel(OpenBISSftpNode.Type.ROOT));
        assertNull(StandardPathTranslator.toTypeLabel(OpenBISSftpNode.Type.SUBLEVEL));
        assertEquals(StandardPathTranslator.SPACE_TYPE_LABEL, StandardPathTranslator.toTypeLabel(OpenBISSftpNode.Type.SPACE));
        assertEquals(StandardPathTranslator.PROJECT_TYPE_LABEL, StandardPathTranslator.toTypeLabel(OpenBISSftpNode.Type.PROJECT));
        assertEquals(StandardPathTranslator.EXPERIMENT_TYPE_LABEL, StandardPathTranslator.toTypeLabel(OpenBISSftpNode.Type.EXPERIMENT));
        assertEquals(StandardPathTranslator.FOLDER_TYPE_LABEL, StandardPathTranslator.toTypeLabel(OpenBISSftpNode.Type.FOLDER));
        assertEquals(StandardPathTranslator.SAMPLE_TYPE_LABEL, StandardPathTranslator.toTypeLabel(OpenBISSftpNode.Type.SAMPLE));
        assertEquals(StandardPathTranslator.DATA_SET_TYPE_LABEL, StandardPathTranslator.toTypeLabel(OpenBISSftpNode.Type.DATA_SET));
        assertEquals(StandardPathTranslator.FILE_TYPE_LABEL, StandardPathTranslator.toTypeLabel(OpenBISSftpNode.Type.AFS_FILE));
    }

    public void testFromTypeLabel() throws Exception {
        assertEquals(OpenBISSftpNode.Type.SPACE, StandardPathTranslator.fromTypeLabel(StandardPathTranslator.SPACE_TYPE_LABEL));
        assertEquals(OpenBISSftpNode.Type.PROJECT, StandardPathTranslator.fromTypeLabel(StandardPathTranslator.PROJECT_TYPE_LABEL));
        assertEquals(OpenBISSftpNode.Type.EXPERIMENT, StandardPathTranslator.fromTypeLabel(StandardPathTranslator.EXPERIMENT_TYPE_LABEL));
        assertEquals(OpenBISSftpNode.Type.FOLDER, StandardPathTranslator.fromTypeLabel(StandardPathTranslator.FOLDER_TYPE_LABEL));
        assertEquals(OpenBISSftpNode.Type.SAMPLE, StandardPathTranslator.fromTypeLabel(StandardPathTranslator.SAMPLE_TYPE_LABEL));
        assertEquals(OpenBISSftpNode.Type.DATA_SET, StandardPathTranslator.fromTypeLabel(StandardPathTranslator.DATA_SET_TYPE_LABEL));
        assertEquals(OpenBISSftpNode.Type.AFS_FILE, StandardPathTranslator.fromTypeLabel(StandardPathTranslator.FILE_TYPE_LABEL));
        Exception exception = null;
        try {
            StandardPathTranslator.fromTypeLabel("other");
        } catch (Exception e) {
            exception = e;
        }
        assertEquals(FtpPathTranslator.MalformedPathException.class, exception.getClass());
    }

    public void testFromFtpNode() {
        assertEquals(Collections.emptyList(), StandardPathTranslator.fromFtpNode(
                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.ROOT).build()
        ));
        List<OpenBISSftpNode.Type> typesWithIdentifier = List.of(
                OpenBISSftpNode.Type.SPACE,
                OpenBISSftpNode.Type.PROJECT,
                OpenBISSftpNode.Type.EXPERIMENT,
                OpenBISSftpNode.Type.FOLDER,
                OpenBISSftpNode.Type.SAMPLE,
                OpenBISSftpNode.Type.DATA_SET
        );
        String identifier = "---fake-identifier---";
        for (OpenBISSftpNode.Type type : typesWithIdentifier) {
            assertEquals(Collections.singletonList(identifier), StandardPathTranslator.fromFtpNode(
                    OpenBISSftpNode.builder()
                            .type(type)
                            .identifier(Optional.of(identifier))
                            .build()
            ));
        }
        List<String> afsPathSegments = List.of("dir1", "subdir2", "leaf-file");
        assertEquals(
                afsPathSegments,
                StandardPathTranslator.fromFtpNode(
                    OpenBISSftpNode.builder()
                            .type(OpenBISSftpNode.Type.AFS_FILE)
                            .afsFilePath(afsPathSegments)
                            .build()
        ));
    }
}