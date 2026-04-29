package ch.ethz.sis.afssftp.filesystemview.impl.standard;

import ch.ethz.sis.afssftp.filesystemview.FtpPathTranslator;
import ch.ethz.sis.afssftp.filesystemview.SftpNode;
import ch.ethz.sis.afssftp.filesystemview.SftpNodeChain;
import junit.framework.TestCase;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class StandardPathTranslatorTest extends TestCase {
    public void testToPathSegments() throws Exception {
        StandardPathTranslator standardPathTranslator = new StandardPathTranslator();
        assertEquals(Collections.emptyList(), standardPathTranslator.toPathSegments(new SftpNodeChain(
                List.of(
                        SftpNode.builder().type(SftpNode.Type.ROOT).build()
                )
        )));
        assertEquals(List.of("spaces","space_1"), standardPathTranslator.toPathSegments(new SftpNodeChain(
                List.of(
                        SftpNode.builder().type(SftpNode.Type.ROOT).build(),
                        SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                .identifier(Optional.of(StandardPathTranslator.SPACE_TYPE_LABEL)).build(),
                        SftpNode.builder().type(SftpNode.Type.SPACE)
                                .identifier(Optional.of("space_1")).build()
                )
        )));
        assertEquals(List.of("spaces","space_1", "projects", "project_1"),
                standardPathTranslator.toPathSegments(new SftpNodeChain(
                    List.of(
                            SftpNode.builder().type(SftpNode.Type.ROOT).build(),
                            SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.SPACE_TYPE_LABEL)).build(),
                            SftpNode.builder().type(SftpNode.Type.SPACE)
                                    .identifier(Optional.of("space_1")).build(),
                            SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.PROJECT_TYPE_LABEL)).build(),
                            SftpNode.builder().type(SftpNode.Type.PROJECT)
                                    .identifier(Optional.of("project_1")).build()
                    )
        )));
        assertEquals(List.of("spaces","space_1", "projects", "project_1", "experiments", "exp name(experiment_1)"),
                standardPathTranslator.toPathSegments(new SftpNodeChain(
                    List.of(
                            SftpNode.builder().type(SftpNode.Type.ROOT).build(),
                            SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.SPACE_TYPE_LABEL)).build(),
                            SftpNode.builder().type(SftpNode.Type.SPACE)
                                    .identifier(Optional.of("space_1")).build(),
                            SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.PROJECT_TYPE_LABEL)).build(),
                            SftpNode.builder().type(SftpNode.Type.PROJECT)
                                    .identifier(Optional.of("project_1")).build(),
                            SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.EXPERIMENT_TYPE_LABEL)).build(),
                            SftpNode.builder().type(SftpNode.Type.EXPERIMENT)
                                    .identifier(Optional.of("exp name(experiment_1)")).build()
                    )
        )));
        assertEquals(List.of("spaces","space_1", "projects", "project_1", "experiments", "exp name(experiment_1)", "folders", "folder name(folder_1)"),
                standardPathTranslator.toPathSegments(new SftpNodeChain(
                    List.of(
                            SftpNode.builder().type(SftpNode.Type.ROOT).build(),
                            SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.SPACE_TYPE_LABEL)).build(),
                            SftpNode.builder().type(SftpNode.Type.SPACE)
                                    .identifier(Optional.of("space_1")).build(),
                            SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.PROJECT_TYPE_LABEL)).build(),
                            SftpNode.builder().type(SftpNode.Type.PROJECT)
                                    .identifier(Optional.of("project_1")).build(),
                            SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.EXPERIMENT_TYPE_LABEL)).build(),
                            SftpNode.builder().type(SftpNode.Type.EXPERIMENT)
                                    .identifier(Optional.of("exp name(experiment_1)")).build(),
                            SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.FOLDER_TYPE_LABEL)).build(),
                            SftpNode.builder().type(SftpNode.Type.FOLDER)
                                    .identifier(Optional.of("folder name(folder_1)")).build()
                    )
        )));
        assertEquals(List.of("spaces","space_1", "projects", "project_1",
                        "experiments", "exp name(experiment_1)", "folders", "folder name(folder_1)", "samples", "sample name(sample_1)"),
                standardPathTranslator.toPathSegments(new SftpNodeChain(
                    List.of(
                            SftpNode.builder().type(SftpNode.Type.ROOT).build(),
                            SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.SPACE_TYPE_LABEL)).build(),
                            SftpNode.builder().type(SftpNode.Type.SPACE)
                                    .identifier(Optional.of("space_1")).build(),
                            SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.PROJECT_TYPE_LABEL)).build(),
                            SftpNode.builder().type(SftpNode.Type.PROJECT)
                                    .identifier(Optional.of("project_1")).build(),
                            SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.EXPERIMENT_TYPE_LABEL)).build(),
                            SftpNode.builder().type(SftpNode.Type.EXPERIMENT)
                                    .identifier(Optional.of("exp name(experiment_1)")).build(),
                            SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.FOLDER_TYPE_LABEL)).build(),
                            SftpNode.builder().type(SftpNode.Type.FOLDER)
                                    .identifier(Optional.of("folder name(folder_1)")).build(),
                            SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.SAMPLE_TYPE_LABEL)).build(),
                            SftpNode.builder().type(SftpNode.Type.SAMPLE)
                                    .identifier(Optional.of("sample name(sample_1)")).build()
                    )
        )));
        assertEquals(List.of("spaces","space_1", "projects", "project_1",
                        "experiments", "exp name(experiment_1)", "folders", "folder name(folder_1)", "samples", "sample name(sample_1)",
                        "datasets", "dataset name(dataset_1)"),
                standardPathTranslator.toPathSegments(new SftpNodeChain(
                    List.of(
                            SftpNode.builder().type(SftpNode.Type.ROOT).build(),
                            SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.SPACE_TYPE_LABEL)).build(),
                            SftpNode.builder().type(SftpNode.Type.SPACE)
                                    .identifier(Optional.of("space_1")).build(),
                            SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.PROJECT_TYPE_LABEL)).build(),
                            SftpNode.builder().type(SftpNode.Type.PROJECT)
                                    .identifier(Optional.of("project_1")).build(),
                            SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.EXPERIMENT_TYPE_LABEL)).build(),
                            SftpNode.builder().type(SftpNode.Type.EXPERIMENT)
                                    .identifier(Optional.of("exp name(experiment_1)")).build(),
                            SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.FOLDER_TYPE_LABEL)).build(),
                            SftpNode.builder().type(SftpNode.Type.FOLDER)
                                    .identifier(Optional.of("folder name(folder_1)")).build(),
                            SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.SAMPLE_TYPE_LABEL)).build(),
                            SftpNode.builder().type(SftpNode.Type.SAMPLE)
                                    .identifier(Optional.of("sample name(sample_1)")).build(),
                            SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.DATA_SET_TYPE_LABEL)).build(),
                            SftpNode.builder().type(SftpNode.Type.DATA_SET)
                                    .identifier(Optional.of("dataset name(dataset_1)")).build()
                    )
        )));
        assertEquals(List.of("spaces","space_1", "projects", "project_1",
                        "experiments", "exp name(experiment_1)", "folders", "folder name(folder_1)", "samples", "sample name(sample_1)",
                        "datasets", "dataset name(dataset_1)", "files", "dir1", "dir2", "file3"),
                standardPathTranslator.toPathSegments(new SftpNodeChain(
                    List.of(
                            SftpNode.builder().type(SftpNode.Type.ROOT).build(),
                            SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.SPACE_TYPE_LABEL)).build(),
                            SftpNode.builder().type(SftpNode.Type.SPACE)
                                    .identifier(Optional.of("space_1")).build(),
                            SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.PROJECT_TYPE_LABEL)).build(),
                            SftpNode.builder().type(SftpNode.Type.PROJECT)
                                    .identifier(Optional.of("project_1")).build(),
                            SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.EXPERIMENT_TYPE_LABEL)).build(),
                            SftpNode.builder().type(SftpNode.Type.EXPERIMENT)
                                    .identifier(Optional.of("exp name(experiment_1)")).build(),
                            SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.FOLDER_TYPE_LABEL)).build(),
                            SftpNode.builder().type(SftpNode.Type.FOLDER)
                                    .identifier(Optional.of("folder name(folder_1)")).build(),
                            SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.SAMPLE_TYPE_LABEL)).build(),
                            SftpNode.builder().type(SftpNode.Type.SAMPLE)
                                    .identifier(Optional.of("sample name(sample_1)")).build(),
                            SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.DATA_SET_TYPE_LABEL)).build(),
                            SftpNode.builder().type(SftpNode.Type.DATA_SET)
                                    .identifier(Optional.of("dataset name(dataset_1)")).build(),
                            SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.FILE_TYPE_LABEL)).build(),
                            SftpNode.builder().type(SftpNode.Type.AFS_FILE)
                                    .afsFilePath(List.of("dir1", "dir2", "file3")).build()
                    )
        )));

        //More generally: test that each node is correctly translated
        List<SftpNode> nodes = List.of(
                SftpNode.builder().type(SftpNode.Type.ROOT).build(),
                SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                        .identifier(Optional.of(StandardPathTranslator.SPACE_TYPE_LABEL)).build(),
                SftpNode.builder().type(SftpNode.Type.SPACE)
                        .identifier(Optional.of("space_1")).build(),
                SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                        .identifier(Optional.of(StandardPathTranslator.PROJECT_TYPE_LABEL)).build(),
                SftpNode.builder().type(SftpNode.Type.PROJECT)
                        .identifier(Optional.of("project_1")).build(),
                SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                        .identifier(Optional.of(StandardPathTranslator.EXPERIMENT_TYPE_LABEL)).build(),
                SftpNode.builder().type(SftpNode.Type.EXPERIMENT)
                        .identifier(Optional.of("exp name(experiment_1)")).build(),
                SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                        .identifier(Optional.of(StandardPathTranslator.FOLDER_TYPE_LABEL)).build(),
                SftpNode.builder().type(SftpNode.Type.FOLDER)
                        .identifier(Optional.of("folder name(folder_1)")).build(),
                SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                        .identifier(Optional.of(StandardPathTranslator.SAMPLE_TYPE_LABEL)).build(),
                SftpNode.builder().type(SftpNode.Type.SAMPLE)
                        .identifier(Optional.of("sample name(sample_1)")).build(),
                SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                        .identifier(Optional.of(StandardPathTranslator.DATA_SET_TYPE_LABEL)).build(),
                SftpNode.builder().type(SftpNode.Type.DATA_SET)
                        .identifier(Optional.of("dataset name(dataset_1)")).build(),
                SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                        .identifier(Optional.of(StandardPathTranslator.FILE_TYPE_LABEL)).build(),
                SftpNode.builder().type(SftpNode.Type.AFS_FILE)
                        .afsFilePath(List.of("dir1", "dir2", "file3")).build()
        );
        assertEquals(
                nodes.stream().map(StandardPathTranslator::fromFtpNode)
                    .flatMap( List::stream ).toList(),
                standardPathTranslator.toPathSegments(new SftpNodeChain(
                    nodes
        )));
    }

    public void testFromPathSegments() throws Exception {
        StandardPathTranslator standardPathTranslator = new StandardPathTranslator();
        assertEquals(new SftpNodeChain(
                List.of(
                        SftpNode.builder().type(SftpNode.Type.ROOT).build()
                )
        ), standardPathTranslator.fromPathSegments(Collections.emptyList()));
        assertEquals(new SftpNodeChain(
                List.of(
                        SftpNode.builder().type(SftpNode.Type.ROOT).build(),
                        SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                .identifier(Optional.of(StandardPathTranslator.SPACE_TYPE_LABEL)).build(),
                        SftpNode.builder().type(SftpNode.Type.SPACE)
                                .identifier(Optional.of("space_1")).build()
                )
        ), standardPathTranslator.fromPathSegments(List.of("spaces","space_1")));
        assertEquals(new SftpNodeChain(
                        List.of(
                                SftpNode.builder().type(SftpNode.Type.ROOT).build(),
                                SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.SPACE_TYPE_LABEL)).build(),
                                SftpNode.builder().type(SftpNode.Type.SPACE)
                                        .identifier(Optional.of("space_1")).build(),
                                SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.PROJECT_TYPE_LABEL)).build(),
                                SftpNode.builder().type(SftpNode.Type.PROJECT)
                                        .identifier(Optional.of("project_1")).build()
                        )
                ),
                standardPathTranslator.fromPathSegments(List.of("spaces","space_1", "projects", "project_1")));
        assertEquals(new SftpNodeChain(
                        List.of(
                                SftpNode.builder().type(SftpNode.Type.ROOT).build(),
                                SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.SPACE_TYPE_LABEL)).build(),
                                SftpNode.builder().type(SftpNode.Type.SPACE)
                                        .identifier(Optional.of("space_1")).build(),
                                SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.PROJECT_TYPE_LABEL)).build(),
                                SftpNode.builder().type(SftpNode.Type.PROJECT)
                                        .identifier(Optional.of("project_1")).build(),
                                SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.EXPERIMENT_TYPE_LABEL)).build(),
                                SftpNode.builder().type(SftpNode.Type.EXPERIMENT)
                                        .identifier(Optional.of("exp name(experiment_1)")).build()
                        )
                ),
                standardPathTranslator.fromPathSegments(List.of("spaces","space_1", "projects", "project_1", "experiments", "exp name(experiment_1)")));
        assertEquals(new SftpNodeChain(
                        List.of(
                                SftpNode.builder().type(SftpNode.Type.ROOT).build(),
                                SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.SPACE_TYPE_LABEL)).build(),
                                SftpNode.builder().type(SftpNode.Type.SPACE)
                                        .identifier(Optional.of("space_1")).build(),
                                SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.PROJECT_TYPE_LABEL)).build(),
                                SftpNode.builder().type(SftpNode.Type.PROJECT)
                                        .identifier(Optional.of("project_1")).build(),
                                SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.EXPERIMENT_TYPE_LABEL)).build(),
                                SftpNode.builder().type(SftpNode.Type.EXPERIMENT)
                                        .identifier(Optional.of("exp name(experiment_1)")).build(),
                                SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.FOLDER_TYPE_LABEL)).build(),
                                SftpNode.builder().type(SftpNode.Type.FOLDER)
                                        .identifier(Optional.of("folder name(folder_1)")).build()
                        )
                ),
                standardPathTranslator.fromPathSegments(List.of("spaces","space_1", "projects", "project_1", "experiments", "exp name(experiment_1)", "folders", "folder name(folder_1)")));
        assertEquals(new SftpNodeChain(
                        List.of(
                                SftpNode.builder().type(SftpNode.Type.ROOT).build(),
                                SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.SPACE_TYPE_LABEL)).build(),
                                SftpNode.builder().type(SftpNode.Type.SPACE)
                                        .identifier(Optional.of("space_1")).build(),
                                SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.PROJECT_TYPE_LABEL)).build(),
                                SftpNode.builder().type(SftpNode.Type.PROJECT)
                                        .identifier(Optional.of("project_1")).build(),
                                SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.EXPERIMENT_TYPE_LABEL)).build(),
                                SftpNode.builder().type(SftpNode.Type.EXPERIMENT)
                                        .identifier(Optional.of("exp name(experiment_1)")).build(),
                                SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.FOLDER_TYPE_LABEL)).build(),
                                SftpNode.builder().type(SftpNode.Type.FOLDER)
                                        .identifier(Optional.of("folder name(folder_1)")).build(),
                                SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.SAMPLE_TYPE_LABEL)).build(),
                                SftpNode.builder().type(SftpNode.Type.SAMPLE)
                                        .identifier(Optional.of("sample name(sample_1)")).build()
                        )
                ),
                standardPathTranslator.fromPathSegments(List.of("spaces","space_1", "projects", "project_1",
                        "experiments", "exp name(experiment_1)", "folders", "folder name(folder_1)", "samples", "sample name(sample_1)")));
        assertEquals(new SftpNodeChain(
                        List.of(
                                SftpNode.builder().type(SftpNode.Type.ROOT).build(),
                                SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.SPACE_TYPE_LABEL)).build(),
                                SftpNode.builder().type(SftpNode.Type.SPACE)
                                        .identifier(Optional.of("space_1")).build(),
                                SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.PROJECT_TYPE_LABEL)).build(),
                                SftpNode.builder().type(SftpNode.Type.PROJECT)
                                        .identifier(Optional.of("project_1")).build(),
                                SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.EXPERIMENT_TYPE_LABEL)).build(),
                                SftpNode.builder().type(SftpNode.Type.EXPERIMENT)
                                        .identifier(Optional.of("exp name(experiment_1)")).build(),
                                SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.FOLDER_TYPE_LABEL)).build(),
                                SftpNode.builder().type(SftpNode.Type.FOLDER)
                                        .identifier(Optional.of("folder name(folder_1)")).build(),
                                SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.SAMPLE_TYPE_LABEL)).build(),
                                SftpNode.builder().type(SftpNode.Type.SAMPLE)
                                        .identifier(Optional.of("sample name(sample_1)")).build(),
                                SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.DATA_SET_TYPE_LABEL)).build(),
                                SftpNode.builder().type(SftpNode.Type.DATA_SET)
                                        .identifier(Optional.of("dataset name(dataset_1)")).build()
                        )
                ),
                standardPathTranslator.fromPathSegments(List.of("spaces","space_1", "projects", "project_1",
                        "experiments", "exp name(experiment_1)", "folders", "folder name(folder_1)", "samples", "sample name(sample_1)",
                        "datasets", "dataset name(dataset_1)")));
        assertEquals(new SftpNodeChain(
                        List.of(
                                SftpNode.builder().type(SftpNode.Type.ROOT).build(),
                                SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.SPACE_TYPE_LABEL)).build(),
                                SftpNode.builder().type(SftpNode.Type.SPACE)
                                        .identifier(Optional.of("space_1")).build(),
                                SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.PROJECT_TYPE_LABEL)).build(),
                                SftpNode.builder().type(SftpNode.Type.PROJECT)
                                        .identifier(Optional.of("project_1")).build(),
                                SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.EXPERIMENT_TYPE_LABEL)).build(),
                                SftpNode.builder().type(SftpNode.Type.EXPERIMENT)
                                        .identifier(Optional.of("exp name(experiment_1)")).build(),
                                SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.FOLDER_TYPE_LABEL)).build(),
                                SftpNode.builder().type(SftpNode.Type.FOLDER)
                                        .identifier(Optional.of("folder name(folder_1)")).build(),
                                SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.SAMPLE_TYPE_LABEL)).build(),
                                SftpNode.builder().type(SftpNode.Type.SAMPLE)
                                        .identifier(Optional.of("sample name(sample_1)")).build(),
                                SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.DATA_SET_TYPE_LABEL)).build(),
                                SftpNode.builder().type(SftpNode.Type.DATA_SET)
                                        .identifier(Optional.of("dataset name(dataset_1)")).build(),
                                SftpNode.builder().type(SftpNode.Type.SUBLEVEL)
                                        .identifier(Optional.of(StandardPathTranslator.FILE_TYPE_LABEL)).build(),
                                SftpNode.builder().type(SftpNode.Type.AFS_FILE)
                                        .afsFilePath(List.of("dir1", "dir2", "file3")).build()
                        )
                ),
                standardPathTranslator.fromPathSegments(List.of("spaces","space_1", "projects", "project_1",
                        "experiments", "exp name(experiment_1)", "folders", "folder name(folder_1)", "samples", "sample name(sample_1)",
                        "datasets", "dataset name(dataset_1)", "files", "dir1", "dir2", "file3")));
    }

    public void testFromTypeAndIdentifier() {
        List<SftpNode.Type> admittedTypes = List.of(
                SftpNode.Type.SPACE,
                SftpNode.Type.PROJECT,
                SftpNode.Type.EXPERIMENT,
                SftpNode.Type.FOLDER,
                SftpNode.Type.SAMPLE,
                SftpNode.Type.DATA_SET
        );
        String identifier = "fake-id";
        for (SftpNode.Type type : admittedTypes) {
            assertEquals(
                    List.of(
                            SftpNode.builder()
                                    .type(SftpNode.Type.SUBLEVEL)
                                    .identifier(Optional.of(StandardPathTranslator.toTypeLabel(type)))
                                    .build(),
                            SftpNode.builder()
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
        SftpNode afsNode = StandardPathTranslator.createAfsFileTypeWithPathTokens(pathTokens);
        assertEquals(pathTokens, afsNode.getAfsFilePath());
        assertEquals(SftpNode.Type.AFS_FILE, afsNode.getType());
        assertEquals(Optional.empty(), afsNode.getIdentifier());
    }

    public void testToTypeLabel() {
        assertNull(StandardPathTranslator.toTypeLabel(SftpNode.Type.ROOT));
        assertNull(StandardPathTranslator.toTypeLabel(SftpNode.Type.SUBLEVEL));
        assertEquals(StandardPathTranslator.SPACE_TYPE_LABEL, StandardPathTranslator.toTypeLabel(SftpNode.Type.SPACE));
        assertEquals(StandardPathTranslator.PROJECT_TYPE_LABEL, StandardPathTranslator.toTypeLabel(SftpNode.Type.PROJECT));
        assertEquals(StandardPathTranslator.EXPERIMENT_TYPE_LABEL, StandardPathTranslator.toTypeLabel(SftpNode.Type.EXPERIMENT));
        assertEquals(StandardPathTranslator.FOLDER_TYPE_LABEL, StandardPathTranslator.toTypeLabel(SftpNode.Type.FOLDER));
        assertEquals(StandardPathTranslator.SAMPLE_TYPE_LABEL, StandardPathTranslator.toTypeLabel(SftpNode.Type.SAMPLE));
        assertEquals(StandardPathTranslator.DATA_SET_TYPE_LABEL, StandardPathTranslator.toTypeLabel(SftpNode.Type.DATA_SET));
        assertEquals(StandardPathTranslator.FILE_TYPE_LABEL, StandardPathTranslator.toTypeLabel(SftpNode.Type.AFS_FILE));
    }

    public void testFromTypeLabel() throws Exception {
        assertEquals(SftpNode.Type.SPACE, StandardPathTranslator.fromTypeLabel(StandardPathTranslator.SPACE_TYPE_LABEL));
        assertEquals(SftpNode.Type.PROJECT, StandardPathTranslator.fromTypeLabel(StandardPathTranslator.PROJECT_TYPE_LABEL));
        assertEquals(SftpNode.Type.EXPERIMENT, StandardPathTranslator.fromTypeLabel(StandardPathTranslator.EXPERIMENT_TYPE_LABEL));
        assertEquals(SftpNode.Type.FOLDER, StandardPathTranslator.fromTypeLabel(StandardPathTranslator.FOLDER_TYPE_LABEL));
        assertEquals(SftpNode.Type.SAMPLE, StandardPathTranslator.fromTypeLabel(StandardPathTranslator.SAMPLE_TYPE_LABEL));
        assertEquals(SftpNode.Type.DATA_SET, StandardPathTranslator.fromTypeLabel(StandardPathTranslator.DATA_SET_TYPE_LABEL));
        assertEquals(SftpNode.Type.AFS_FILE, StandardPathTranslator.fromTypeLabel(StandardPathTranslator.FILE_TYPE_LABEL));
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
                SftpNode.builder().type(SftpNode.Type.ROOT).build()
        ));
        List<SftpNode.Type> typesWithIdentifier = List.of(
                SftpNode.Type.SPACE,
                SftpNode.Type.PROJECT,
                SftpNode.Type.EXPERIMENT,
                SftpNode.Type.FOLDER,
                SftpNode.Type.SAMPLE,
                SftpNode.Type.DATA_SET
        );
        String identifier = "---fake-identifier---";
        for (SftpNode.Type type : typesWithIdentifier) {
            assertEquals(Collections.singletonList(identifier), StandardPathTranslator.fromFtpNode(
                    SftpNode.builder()
                            .type(type)
                            .identifier(Optional.of(identifier))
                            .build()
            ));
        }
        List<String> afsPathSegments = List.of("dir1", "subdir2", "leaf-file");
        assertEquals(
                afsPathSegments,
                StandardPathTranslator.fromFtpNode(
                    SftpNode.builder()
                            .type(SftpNode.Type.AFS_FILE)
                            .afsFilePath(afsPathSegments)
                            .build()
        ));
    }
}