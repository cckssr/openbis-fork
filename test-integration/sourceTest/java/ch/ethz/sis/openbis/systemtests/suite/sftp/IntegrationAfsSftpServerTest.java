package ch.ethz.sis.openbis.systemtests.suite.sftp;

import ch.ethz.sis.openbis.systemtests.suite.sftp.environment.AfsSftpServerIntegrationTestEnvironment;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static ch.ethz.sis.openbis.systemtests.suite.sftp.environment.AfsSftpServerIntegrationTestEnvironment.*;
import static org.testng.Assert.*;

public class IntegrationAfsSftpServerTest
{

    private static final String SFTP_HOST = "localhost";
    private static final int SFTP_PORT = 2223;
    private static final long SFTP_TIMEOUT = 5000;
    private static final String USER = "admin";

    @BeforeSuite
    public void beforeSuite()
    {
        AfsSftpServerIntegrationTestEnvironment.start();
    }

    @AfterSuite
    public void afterSuite()
    {
        AfsSftpServerIntegrationTestEnvironment.stop();
    }

    @Test(priority = 1)
    public void testList() throws Exception
    {
        try (SshClient client = SshClient.setUpDefaultClient())
        {
            client.start();

            try (ClientSession session = client.connect(USER, SFTP_HOST, SFTP_PORT)
                    .verify(SFTP_TIMEOUT)
                    .getSession())
            {
                session.addPasswordIdentity(AfsSftpServerIntegrationTestEnvironment.PASSWORD);
                session.auth().verify(SFTP_TIMEOUT);

                try (SftpClient sftp = SftpClientFactory.instance().createSftpClient(session)) {

                    for (String space : List.of(TEST_SPACE_1, TEST_SPACE_2)) {
                        assertEquals(
                                listRemoteDirectory(sftp, "/"),
                                Set.of(".", "spaces")
                        );

                        assertEquals(
                                listRemoteDirectory(sftp,"/spaces/" + space),
                                Set.of(".", "..", "projects", "folders", "samples")
                        );

                        assertEquals(
                                listRemoteDirectory(sftp,"/spaces/" + space + "/samples"),
                                Set.of(".", "..",
                                        String.format("%s (%s)", TEST_SAMPLE_1, getSampleOrDatasetPermIdByName(space, TEST_SAMPLE_1)),
                                        String.format("%s (%s)", TEST_SAMPLE_2, getSampleOrDatasetPermIdByName(space, TEST_SAMPLE_2)),
                                        String.format("%s (%s)", TEST_FOLDER_1 + "_" + TEST_SUBSAMPLE_1, getSampleOrDatasetPermIdByName(space, TEST_FOLDER_1 + "_" + TEST_SUBSAMPLE_1)),
                                        String.format("%s (%s)", TEST_FOLDER_1 + "_" + TEST_SUBSAMPLE_2, getSampleOrDatasetPermIdByName(space, TEST_FOLDER_1 + "_" + TEST_SUBSAMPLE_2)),
                                        String.format("%s (%s)", TEST_FOLDER_2 + "_" + TEST_SUBSAMPLE_1, getSampleOrDatasetPermIdByName(space, TEST_FOLDER_2 + "_" + TEST_SUBSAMPLE_1)),
                                        String.format("%s (%s)", TEST_FOLDER_2 + "_" + TEST_SUBSAMPLE_2, getSampleOrDatasetPermIdByName(space, TEST_FOLDER_2 + "_" + TEST_SUBSAMPLE_2))
                                )
                        );

                        for (String spaceSample : List.of(TEST_SAMPLE_1, TEST_SAMPLE_2)) {
                            String spaceSampleDisplayName = String.format("%s (%s)", spaceSample, getSampleOrDatasetPermIdByName(space, spaceSample));

                            assertEquals(
                                    listRemoteDirectory(sftp,"/spaces/" + space + "/samples/" + spaceSampleDisplayName),
                                    Set.of(".", "..", "folders", "samples", "datasets", "files")
                            );

                            assertEquals(
                                    listRemoteDirectory(sftp,"/spaces/" + space + "/samples/" + spaceSampleDisplayName + "/folders"),
                                    Set.of(".", "..")
                            );

                            assertEquals(
                                    listRemoteDirectory(sftp,"/spaces/" + space + "/samples/" + spaceSampleDisplayName + "/samples"),
                                    Set.of(".", "..")
                            );

                            assertEquals(
                                    listRemoteDirectory(sftp,"/spaces/" + space + "/samples/" + spaceSampleDisplayName + "/datasets"),
                                    Set.of(".", "..")
                            );

                            assertEquals(
                                    listRemoteDirectory(sftp,"/spaces/" + space + "/samples/" + spaceSampleDisplayName + "/files"),
                                    Set.of(".", "..")
                            );
                        }

                        assertEquals(
                                listRemoteDirectory(sftp,"/spaces/" + space + "/folders"),
                                Set.of(".", "..",
                                        String.format("%s (%s)", TEST_FOLDER_1, getSampleOrDatasetPermIdByName(space, TEST_FOLDER_1)),
                                        String.format("%s (%s)", TEST_FOLDER_2, getSampleOrDatasetPermIdByName(space, TEST_FOLDER_2))
                                )
                        );

                        for (String spaceFolder : List.of(TEST_FOLDER_1, TEST_FOLDER_2)) {
                            String spaceFolderDisplayName = String.format("%s (%s)", spaceFolder, getSampleOrDatasetPermIdByName(space, spaceFolder));

                            assertEquals(
                                    listRemoteDirectory(sftp,"/spaces/" + space + "/folders/" + spaceFolderDisplayName),
                                    Set.of(".", "..", "folders", "samples", "datasets", "files")
                            );

                            assertEquals(
                                    listRemoteDirectory(sftp,"/spaces/" + space + "/folders/" + spaceFolderDisplayName + "/folders"),
                                    Set.of(".", "..")
                            );

                            assertEquals(
                                    listRemoteDirectory(sftp,"/spaces/" + space + "/folders/" + spaceFolderDisplayName + "/samples"),
                                    Set.of(".", "..",
                                            String.format("%s (%s)", spaceFolder + "_" + TEST_SUBSAMPLE_1, getSampleOrDatasetPermIdByName(space, spaceFolder + "_" + TEST_SUBSAMPLE_1)),
                                            String.format("%s (%s)", spaceFolder + "_" + TEST_SUBSAMPLE_2, getSampleOrDatasetPermIdByName(space, spaceFolder + "_" + TEST_SUBSAMPLE_2))
                                    )
                            );

                            assertEquals(
                                    listRemoteDirectory(sftp,"/spaces/" + space + "/folders/" + spaceFolderDisplayName + "/datasets"),
                                    Set.of(".", "..")
                            );

                            assertEquals(
                                    listRemoteDirectory(sftp,"/spaces/" + space + "/folders/" + spaceFolderDisplayName + "/files"),
                                    Set.of(".", "..")
                            );

                            for (String subsample : List.of(TEST_SUBSAMPLE_1, TEST_SUBSAMPLE_2)) {
                                String spaceSubsampleDisplayName = String.format("%s (%s)", spaceFolder + "_" + subsample, getSampleOrDatasetPermIdByName(space, spaceFolder + "_" + subsample));

                                assertEquals(
                                        listRemoteDirectory(sftp,"/spaces/" + space + "/folders/" + spaceFolder + "/samples/" + spaceSubsampleDisplayName),
                                        Set.of(".", "..", "folders", "samples", "datasets", "files")
                                );

                                assertEquals(
                                        listRemoteDirectory(sftp,"/spaces/" + space + "/folders/" + spaceFolder + "/samples/" + spaceSubsampleDisplayName + "/folders"),
                                        Set.of(".", "..")
                                );

                                assertEquals(
                                        listRemoteDirectory(sftp,"/spaces/" + space + "/folders/" + spaceFolder + "/samples/" + spaceSubsampleDisplayName + "/samples"),
                                        Set.of(".", "..")
                                );

                                assertEquals(
                                        listRemoteDirectory(sftp,"/spaces/" + space + "/folders/" + spaceFolder + "/samples/" + spaceSubsampleDisplayName + "/datasets"),
                                        Set.of(".", "..")
                                );

                                assertEquals(
                                        listRemoteDirectory(sftp,"/spaces/" + space + "/folders/" + spaceFolder + "/samples/" + spaceSubsampleDisplayName + "/files"),
                                        Set.of(".", "..")
                                );
                            }
                        }

                        for (String project : List.of(TEST_PROJECT_1, TEST_PROJECT_2)) {
                            assertEquals(
                                    listRemoteDirectory(sftp,"/spaces/" + space + "/projects/" + project),
                                    Set.of(".", "..", "experiments", "folders", "samples")
                            );

                            assertEquals(
                                    listRemoteDirectory(sftp,"/spaces/" + space + "/projects/" + project + "/experiments"),
                                    Set.of(".", "..",
                                            String.format("%s (%s)", TEST_EXPERIMENT_1, getExperimentPermIdByName(space, project, TEST_EXPERIMENT_1)),
                                            String.format("%s (%s)", TEST_EXPERIMENT_2, getExperimentPermIdByName(space, project, TEST_EXPERIMENT_2))
                                    )
                            );

                            assertEquals(
                                    listRemoteDirectory(sftp,"/spaces/" + space + "/projects/" + project + "/samples"),
                                    Set.of(".", "..",
                                            String.format("%s (%s)", project + "_" + TEST_SAMPLE_1, getSampleOrDatasetPermIdByName(space, project + "_" + TEST_SAMPLE_1)),
                                            String.format("%s (%s)", project + "_" + TEST_SAMPLE_2, getSampleOrDatasetPermIdByName(space, project + "_" + TEST_SAMPLE_2)),
                                            String.format("%s (%s)", project + "_" + TEST_FOLDER_1 + "_" + TEST_SUBSAMPLE_1, getSampleOrDatasetPermIdByName(space, project + "_" + TEST_FOLDER_1 + "_" + TEST_SUBSAMPLE_1)),
                                            String.format("%s (%s)", project + "_" + TEST_FOLDER_1 + "_" + TEST_SUBSAMPLE_2, getSampleOrDatasetPermIdByName(space, project + "_" + TEST_FOLDER_1 + "_" + TEST_SUBSAMPLE_2)),
                                            String.format("%s (%s)", project + "_" + TEST_FOLDER_2 + "_" + TEST_SUBSAMPLE_1, getSampleOrDatasetPermIdByName(space, project + "_" + TEST_FOLDER_2 + "_" + TEST_SUBSAMPLE_1)),
                                            String.format("%s (%s)", project + "_" + TEST_FOLDER_2 + "_" + TEST_SUBSAMPLE_2, getSampleOrDatasetPermIdByName(space, project + "_" + TEST_FOLDER_2 + "_" + TEST_SUBSAMPLE_2))
                                    )
                            );

                            assertEquals(
                                    listRemoteDirectory(sftp,"/spaces/" + space + "/projects/" + project + "/folders"),
                                    Set.of(".", "..",
                                            String.format("%s (%s)", project + "_" + TEST_FOLDER_1, getSampleOrDatasetPermIdByName(space, project + "_" + TEST_FOLDER_1)),
                                            String.format("%s (%s)", project + "_" + TEST_FOLDER_2, getSampleOrDatasetPermIdByName(space, project + "_" + TEST_FOLDER_2))
                                    )
                            );
                            for (String projectSample : List.of(TEST_SAMPLE_1, TEST_SAMPLE_2)) {
                                String projectSampleDisplayName = String.format("%s (%s)", project + "_" + projectSample, getSampleOrDatasetPermIdByName(space, project + "_" + projectSample));

                                assertEquals(
                                        listRemoteDirectory(sftp,"/spaces/" + space + "/projects/" + project + "/samples/" + projectSampleDisplayName),
                                        Set.of(".", "..", "folders", "samples", "datasets", "files")
                                );

                                assertEquals(
                                        listRemoteDirectory(sftp,"/spaces/" + space + "/projects/" + project + "/samples/" + projectSampleDisplayName + "/folders"),
                                        Set.of(".", "..")
                                );

                                assertEquals(
                                        listRemoteDirectory(sftp,"/spaces/" + space + "/projects/" + project + "/samples/" + projectSampleDisplayName + "/samples"),
                                        Set.of(".", "..")
                                );

                                assertEquals(
                                        listRemoteDirectory(sftp,"/spaces/" + space + "/projects/" + project + "/samples/" + projectSampleDisplayName + "/datasets"),
                                        Set.of(".", "..")
                                );

                                assertEquals(
                                        listRemoteDirectory(sftp,"/spaces/" + space + "/projects/" + project + "/samples/" + projectSampleDisplayName + "/files"),
                                        Set.of(".", "..")
                                );
                            }
                            for (String projectFolder : List.of(TEST_FOLDER_1, TEST_FOLDER_2)) {
                                String projectFolderDisplayName = String.format("%s (%s)", project + "_" + projectFolder, getSampleOrDatasetPermIdByName(space, project + "_" + projectFolder));

                                assertEquals(
                                        listRemoteDirectory(sftp,"/spaces/" + space + "/projects/" + project + "/folders/" + projectFolderDisplayName),
                                        Set.of(".", "..", "folders", "samples", "datasets", "files")
                                );

                                assertEquals(
                                        listRemoteDirectory(sftp,"/spaces/" + space + "/projects/" + project + "/folders/" + projectFolderDisplayName + "/folders"),
                                        Set.of(".", "..")
                                );

                                assertEquals(
                                        listRemoteDirectory(sftp,"/spaces/" + space + "/projects/" + project + "/folders/" + projectFolderDisplayName + "/samples"),
                                        Set.of(".", "..",
                                                String.format("%s (%s)", project + "_" + projectFolder + "_" + TEST_SUBSAMPLE_1, getSampleOrDatasetPermIdByName(space, project + "_" + projectFolder + "_" + TEST_SUBSAMPLE_1)),
                                                String.format("%s (%s)", project + "_" + projectFolder + "_" + TEST_SUBSAMPLE_2, getSampleOrDatasetPermIdByName(space, project + "_" + projectFolder + "_" + TEST_SUBSAMPLE_2))
                                        )
                                );

                                assertEquals(
                                        listRemoteDirectory(sftp,"/spaces/" + space + "/projects/" + project + "/folders/" + projectFolderDisplayName + "/datasets"),
                                        Set.of(".", "..")
                                );

                                assertEquals(
                                        listRemoteDirectory(sftp,"/spaces/" + space + "/projects/" + project + "/folders/" + projectFolderDisplayName + "/files"),
                                        Set.of(".", "..")
                                );
                                for (String projectSubsample : List.of(TEST_SUBSAMPLE_1, TEST_SUBSAMPLE_2)) {
                                    String projectSubsampleDisplayName = String.format("%s (%s)", project + "_" + projectFolder + "_" + projectSubsample,
                                            getSampleOrDatasetPermIdByName(space, project + "_" + projectFolder + "_" + projectSubsample));

                                    assertEquals(
                                            listRemoteDirectory(sftp,"/spaces/" + space + "/projects/" + project + "/folders/" + projectSubsampleDisplayName + "/samples/" + projectSubsampleDisplayName),
                                            Set.of(".", "..", "folders", "samples", "datasets", "files")
                                    );

                                    assertEquals(
                                            listRemoteDirectory(sftp,"/spaces/" + space + "/projects/" + project + "/folders/" + projectSubsampleDisplayName + "/samples/" + projectSubsampleDisplayName + "/folders"),
                                            Set.of(".", "..")
                                    );

                                    assertEquals(
                                            listRemoteDirectory(sftp,"/spaces/" + space + "/projects/" + project + "/folders/" + projectSubsampleDisplayName + "/samples/" + projectSubsampleDisplayName + "/samples"),
                                            Set.of(".", "..")
                                    );

                                    assertEquals(
                                            listRemoteDirectory(sftp,"/spaces/" + space + "/projects/" + project + "/folders/" + projectSubsampleDisplayName + "/samples/" + projectSubsampleDisplayName + "/datasets"),
                                            Set.of(".", "..")
                                    );

                                    assertEquals(
                                            listRemoteDirectory(sftp, "/spaces/" + space + "/projects/" + project + "/folders/" + projectSubsampleDisplayName + "/samples/" + projectSubsampleDisplayName + "/files"),
                                            Set.of(".", "..")
                                    );
                                }
                            }

                            for (String experiment : List.of(TEST_EXPERIMENT_1, TEST_EXPERIMENT_2)) {
                                String experimentDisplayName = String.format("%s (%s)", experiment, getExperimentPermIdByName(space, project, experiment));
                                assertEquals(
                                        listRemoteDirectory(sftp,"/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName),
                                        Set.of(".", "..", "folders", "samples", "datasets", "files")
                                );

                                assertEquals(
                                        listRemoteDirectory(sftp,"/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/samples"),
                                        Set.of(".", "..",
                                                String.format("%s (%s)", project + "_" + experiment + "_" + TEST_SAMPLE_1,
                                                        getSampleOrDatasetPermIdByName(space, project + "_" + experiment + "_" + TEST_SAMPLE_1)),
                                                String.format("%s (%s)", project + "_" + experiment + "_" + TEST_SAMPLE_2,
                                                        getSampleOrDatasetPermIdByName(space, project + "_" + experiment + "_" + TEST_SAMPLE_2)),
                                                String.format("%s (%s)", project + "_" + experiment + "_" + TEST_FOLDER_1 + "_" + TEST_SUBSAMPLE_1,
                                                        getSampleOrDatasetPermIdByName(space, project + "_" + experiment + "_" + TEST_FOLDER_1 + "_" + TEST_SUBSAMPLE_1)),
                                                String.format("%s (%s)", project + "_" + experiment + "_" + TEST_FOLDER_1 + "_" + TEST_SUBSAMPLE_2,
                                                        getSampleOrDatasetPermIdByName(space, project + "_" + experiment + "_" + TEST_FOLDER_1 + "_" + TEST_SUBSAMPLE_2)),
                                                String.format("%s (%s)", project + "_" + experiment + "_" + TEST_FOLDER_2 + "_" + TEST_SUBSAMPLE_1,
                                                        getSampleOrDatasetPermIdByName(space, project + "_" + experiment + "_" + TEST_FOLDER_2 + "_" + TEST_SUBSAMPLE_1)),
                                                String.format("%s (%s)", project + "_" + experiment + "_" + TEST_FOLDER_2 + "_" + TEST_SUBSAMPLE_2,
                                                        getSampleOrDatasetPermIdByName(space, project + "_" + experiment + "_" + TEST_FOLDER_2 + "_" + TEST_SUBSAMPLE_2))
                                        )
                                );

                                assertEquals(
                                        listRemoteDirectory(sftp,"/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/folders"),
                                        Set.of(".", "..",
                                                String.format("%s (%s)", project + "_" + experiment + "_" + TEST_FOLDER_1,
                                                        getSampleOrDatasetPermIdByName(space, project + "_" + experiment + "_" + TEST_FOLDER_1)),
                                                String.format("%s (%s)", project + "_" + experiment + "_" + TEST_FOLDER_2,
                                                        getSampleOrDatasetPermIdByName(space, project + "_" + experiment + "_" + TEST_FOLDER_2))
                                        )
                                );

                                assertEquals(
                                        listRemoteDirectory(sftp,"/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/datasets"),
                                        Set.of(".", "..",
                                                String.format("%s (%s)", project + "_" + experiment + "_" + TEST_SAMPLE_1 + "_" + TEST_DATASET_1,
                                                        getSampleOrDatasetPermIdByName(space, project + "_" + experiment + "_" + TEST_SAMPLE_1 + "_" + TEST_DATASET_1)),
                                                String.format("%s (%s)", project + "_" + experiment + "_" + TEST_SAMPLE_1 + "_" + TEST_DATASET_2,
                                                        getSampleOrDatasetPermIdByName(space, project + "_" + experiment + "_" + TEST_SAMPLE_1 + "_" + TEST_DATASET_2)),
                                                String.format("%s (%s)", project + "_" + experiment + "_" + TEST_SAMPLE_2 + "_" + TEST_DATASET_1,
                                                        getSampleOrDatasetPermIdByName(space, project + "_" + experiment + "_" + TEST_SAMPLE_2 + "_" + TEST_DATASET_1)),
                                                String.format("%s (%s)", project + "_" + experiment + "_" + TEST_SAMPLE_2 + "_" + TEST_DATASET_2,
                                                        getSampleOrDatasetPermIdByName(space, project + "_" + experiment + "_" + TEST_SAMPLE_2 + "_" + TEST_DATASET_2))
                                        )
                                );

                                assertEquals(
                                        listRemoteDirectory(sftp,"/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/files"),
                                        Set.of(".", "..")
                                );

                                for (String experimentSample : List.of(TEST_SAMPLE_1, TEST_SAMPLE_2)) {
                                    String experimentSampleDisplayName = String.format("%s (%s)", project + "_" + experiment + "_" + experimentSample,
                                            getSampleOrDatasetPermIdByName(space, project + "_" + experiment + "_" + experimentSample));

                                    assertEquals(
                                            listRemoteDirectory(sftp,"/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/samples/" + experimentSampleDisplayName),
                                            Set.of(".", "..", "folders", "samples", "datasets", "files")
                                    );

                                    assertEquals(
                                            listRemoteDirectory(sftp,"/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/samples/" + experimentSampleDisplayName + "/folders"),
                                            Set.of(".", "..")
                                    );

                                    assertEquals(
                                            listRemoteDirectory(sftp,"/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/samples/" + experimentSampleDisplayName + "/samples"),
                                            Set.of(".", "..")
                                    );

                                    assertEquals(
                                            listRemoteDirectory(sftp,"/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/samples/" + experimentSampleDisplayName + "/datasets"),
                                            Set.of(".", "..",
                                                    String.format("%s (%s)", project + "_" + experiment + "_" + experimentSample + "_" + TEST_DATASET_1,
                                                            getSampleOrDatasetPermIdByName(space, project + "_" + experiment + "_" + experimentSample + "_" + TEST_DATASET_1)),
                                                    String.format("%s (%s)", project + "_" + experiment + "_" + experimentSample + "_" + TEST_DATASET_2,
                                                            getSampleOrDatasetPermIdByName(space, project + "_" + experiment + "_" + experimentSample + "_" + TEST_DATASET_2))
                                            )
                                    );

                                    assertEquals(
                                            listRemoteDirectory(sftp,"/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/samples/" + experimentSampleDisplayName + "/files"),
                                            Set.of(".", "..")
                                    );
                                    for (String experimentSampleDataset : List.of(TEST_DATASET_1, TEST_DATASET_2)) {
                                        String datasetDisplayName = String.format("%s (%s)", project + "_" + experiment + "_" + experimentSample + "_" + experimentSampleDataset,
                                                getSampleOrDatasetPermIdByName(space, project + "_" + experiment + "_" + experimentSample + "_" + experimentSampleDataset));

                                        assertEquals(
                                                listRemoteDirectory(sftp,"/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/samples/" + experimentSampleDisplayName + "/datasets/" + datasetDisplayName),
                                                Set.of(".", "..", "files")
                                        );
                                        assertEquals(
                                                listRemoteDirectory(sftp,"/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/samples/" + experimentSampleDisplayName + "/datasets/" + datasetDisplayName + "/files"),
                                                Set.of(".", "..",
                                                        "file_" + space + "_" + project + "_" + experiment + "_" + experimentSample + "_" + experimentSampleDataset
                                                )
                                        );
                                        assertEquals(
                                                readTestFile(sftp,
                                                        "/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/samples/" + experimentSampleDisplayName + "/datasets/" + datasetDisplayName + "/files/" +
                                                        "file_" + space + "_" + project + "_" + experiment + "_" + experimentSample + "_" + experimentSampleDataset,
                                                        0L, 1024
                                                ),
                                                ("data:" + project + "_" + experiment + "_" + experimentSample + "_" + experimentSampleDataset).getBytes(StandardCharsets.UTF_8)
                                        );
                                    }
                                }
                                for (String experimentFolder : List.of(TEST_FOLDER_1, TEST_FOLDER_2)) {
                                    String experimentFolderDisplayName = String.format("%s (%s)", project + "_" + experiment + "_" + experimentFolder,
                                            getSampleOrDatasetPermIdByName(space, project + "_" + experiment + "_" + experimentFolder));

                                    assertEquals(
                                            listRemoteDirectory(sftp,"/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/folders/" + experimentFolderDisplayName),
                                            Set.of(".", "..", "folders", "samples", "datasets", "files")
                                    );

                                    assertEquals(
                                            listRemoteDirectory(sftp,"/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/folders/" + experimentFolderDisplayName + "/folders"),
                                            Set.of(".", "..")
                                    );

                                    assertEquals(
                                            listRemoteDirectory(sftp,"/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/folders/" + experimentFolderDisplayName + "/samples"),
                                            Set.of(".", "..",
                                                    String.format("%s (%s)", project + "_" + experiment + "_" + experimentFolder + "_" + TEST_SUBSAMPLE_1,
                                                            getSampleOrDatasetPermIdByName(space, project + "_" + experiment + "_" + experimentFolder + "_" + TEST_SUBSAMPLE_1)),
                                                    String.format("%s (%s)", project + "_" + experiment + "_" + experimentFolder + "_" + TEST_SUBSAMPLE_2,
                                                            getSampleOrDatasetPermIdByName(space, project + "_" + experiment + "_" + experimentFolder + "_" + TEST_SUBSAMPLE_2))
                                            )
                                    );

                                    assertEquals(
                                            listRemoteDirectory(sftp,"/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/folders/" + experimentFolderDisplayName + "/datasets"),
                                            Set.of(".", "..")
                                    );

                                    assertEquals(
                                            listRemoteDirectory(sftp,"/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/folders/" + experimentFolderDisplayName + "/files"),
                                            Set.of(".", "..")
                                    );
                                    for (String experimentSubsample : List.of(TEST_SUBSAMPLE_1, TEST_SUBSAMPLE_2)) {
                                        String experimentSubsampleDisplayName = String.format("%s (%s)", project + "_" + experiment + "_" + experimentFolder + "_" + experimentSubsample,
                                                getSampleOrDatasetPermIdByName(space, project + "_" + experiment + "_" + experimentFolder + "_" + experimentSubsample));

                                        assertEquals(
                                                listRemoteDirectory(sftp,"/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/folders/" + experimentFolderDisplayName + "/samples/" + experimentSubsampleDisplayName),
                                                Set.of(".", "..", "folders", "samples", "datasets", "files")
                                        );

                                        assertEquals(
                                                listRemoteDirectory(sftp,"/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/folders/" + experimentFolderDisplayName + "/samples/" + experimentSubsampleDisplayName + "/folders"),
                                                Set.of(".", "..")
                                        );

                                        assertEquals(
                                                listRemoteDirectory(sftp,"/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/folders/" + experimentFolderDisplayName + "/samples/" + experimentSubsampleDisplayName + "/samples"),
                                                Set.of(".", "..")
                                        );

                                        assertEquals(
                                                listRemoteDirectory(sftp,"/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/folders/" + experimentFolderDisplayName + "/samples/" + experimentSubsampleDisplayName + "/datasets"),
                                                Set.of(".", "..")
                                        );

                                        assertEquals(
                                                listRemoteDirectory(sftp,"/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/folders/" + experimentFolderDisplayName + "/samples/" + experimentSubsampleDisplayName + "/files"),
                                                Set.of(".", "..")
                                        );
                                    }
                                }
                            }
                        }
                    }
                }
            } finally
            {
                client.stop();
            }
        }
    }

    @Test(priority = 2)
    public void testAfsUploadToImmutableDatasetFails() throws Exception {
        try (SshClient client = SshClient.setUpDefaultClient()) {
            client.start();

            try (ClientSession session = client.connect(USER, SFTP_HOST, SFTP_PORT)
                    .verify(SFTP_TIMEOUT)
                    .getSession()) {
                session.addPasswordIdentity(AfsSftpServerIntegrationTestEnvironment.PASSWORD);
                session.auth().verify(SFTP_TIMEOUT);

                Exception exception = null;
                try (SftpClient sftp = SftpClientFactory.instance().createSftpClient(session)) {
                    String datasetFilesRootDirectory = String.format("/spaces/S1/projects/P1/experiments/E1 (%s)/samples/X1 (%s)/datasets/D1 (%s)/files",
                            getExperimentPermIdByName("S1", "P1", "E1"),
                            getSampleOrDatasetPermIdByName("S1", "P1_E1_X1"),
                            getSampleOrDatasetPermIdByName("S1", "P1_E1_X1_D1"));
                    Set<String> currentEntries = listRemoteDirectory(sftp, datasetFilesRootDirectory);

                    assertTrue(currentEntries.containsAll(List.of(".", "..", "file_S1_P1_E1_X1_D1")));
                    assertFalse(currentEntries.contains("newfile.txt"));

                    uploadTestFile(sftp,
                            datasetFilesRootDirectory + "/newfile.txt",
                            0L, "HELLO".getBytes(StandardCharsets.UTF_8)
                    );
                } catch (Exception e) {
                    exception = e;
                }
                assertNotNull(exception);
            }
        }
    }

    @Test(priority = 2)
    public void testAfsUploadToMutableSample() throws Exception {
        try (SshClient client = SshClient.setUpDefaultClient()) {
            client.start();

            try (ClientSession session = client.connect(USER, SFTP_HOST, SFTP_PORT)
                    .verify(SFTP_TIMEOUT)
                    .getSession()) {
                session.addPasswordIdentity(AfsSftpServerIntegrationTestEnvironment.PASSWORD);
                session.auth().verify(SFTP_TIMEOUT);

                try (SftpClient sftp = SftpClientFactory.instance().createSftpClient(session)) {
                    String sampleFilesRootDirectory = String.format("/spaces/S1/projects/P1/experiments/E1 (%s)/samples/X1 (%s)/files",
                            getExperimentPermIdByName("S1", "P1", "E1"),
                            getSampleOrDatasetPermIdByName("S1", "P1_E1_X1"));

                    byte[] content = "HELLO hello H3770".getBytes(StandardCharsets.UTF_8);
                    uploadTestFile(sftp,
                            sampleFilesRootDirectory + "/newfile.txt",
                            0L, content
                    );
                    assertEquals(
                            readTestFile(
                                    sftp,
                                    sampleFilesRootDirectory + "/newfile.txt",
                                    0L, 1024
                            ),
                            content
                    );
                }
            }
        }
    }

    @Test(priority = 3)
    public void testCreateAfsDirInImmutableDatasetFails() throws Exception {
        try (SshClient client = SshClient.setUpDefaultClient()) {
            client.start();

            try (ClientSession session = client.connect(USER, SFTP_HOST, SFTP_PORT)
                    .verify(SFTP_TIMEOUT)
                    .getSession()) {
                session.addPasswordIdentity(AfsSftpServerIntegrationTestEnvironment.PASSWORD);
                session.auth().verify(SFTP_TIMEOUT);

                Exception exception = null;
                try (SftpClient sftp = SftpClientFactory.instance().createSftpClient(session)) {
                    String datasetFilesRootDirectory = String.format("/spaces/S1/projects/P1/experiments/E1 (%s)/samples/X1 (%s)/datasets/D2 (%s)/files",
                            getExperimentPermIdByName("S1", "P1", "E1"),
                            getSampleOrDatasetPermIdByName("S1", "P1_E1_X1"),
                            getSampleOrDatasetPermIdByName("S1", "P1_E1_X1_D2"));
                    Set<String> currentEntries = listRemoteDirectory(sftp, datasetFilesRootDirectory);

                    assertTrue(currentEntries.containsAll(List.of(".", "..")));
                    assertFalse(currentEntries.contains("newdir"));

                    createRemoteDirectory(sftp,
                            datasetFilesRootDirectory + "/newdir"
                    );
                } catch (Exception e) {
                    exception = e;
                }
                assertNotNull(exception);
            }
        }
    }

    @Test(priority = 3)
    public void testCreateAfsDirInMutableSample() throws Exception {
        try (SshClient client = SshClient.setUpDefaultClient()) {
            client.start();

            try (ClientSession session = client.connect(USER, SFTP_HOST, SFTP_PORT)
                    .verify(SFTP_TIMEOUT)
                    .getSession()) {
                session.addPasswordIdentity(AfsSftpServerIntegrationTestEnvironment.PASSWORD);
                session.auth().verify(SFTP_TIMEOUT);

                try (SftpClient sftp = SftpClientFactory.instance().createSftpClient(session)) {
                    String sampleFilesRootDirectory = String.format("/spaces/S1/projects/P1/experiments/E1 (%s)/samples/X2 (%s)/files",
                            getExperimentPermIdByName("S1", "P1", "E1"),
                            getSampleOrDatasetPermIdByName("S1", "P1_E1_X2"));
                    Set<String> currentEntries = listRemoteDirectory(sftp, sampleFilesRootDirectory);

                    assertTrue(currentEntries.containsAll(List.of(".", "..")));
                    assertFalse(currentEntries.contains("newdir"));

                    createRemoteDirectory(sftp,
                            sampleFilesRootDirectory + "/newdir"
                    );

                    Set<String> newEntries = listRemoteDirectory(sftp, sampleFilesRootDirectory);
                    assertTrue(newEntries.containsAll(Set.of(".", "..", "newdir")));
                }
            }
        }
    }

    @Test(priority = 4)
    public void testAfsDeleteInImmutableDatasetFails() throws Exception {
        try (SshClient client = SshClient.setUpDefaultClient()) {
            client.start();

            try (ClientSession session = client.connect(USER, SFTP_HOST, SFTP_PORT)
                    .verify(SFTP_TIMEOUT)
                    .getSession()) {
                session.addPasswordIdentity(AfsSftpServerIntegrationTestEnvironment.PASSWORD);
                session.auth().verify(SFTP_TIMEOUT);

                Exception exception = null;
                try (SftpClient sftp = SftpClientFactory.instance().createSftpClient(session)) {
                    String datasetFilesRootDirectory = String.format("/spaces/S1/projects/P1/experiments/E1 (%s)/samples/X1 (%s)/datasets/D2 (%s)/files",
                            getExperimentPermIdByName("S1", "P1", "E1"),
                            getSampleOrDatasetPermIdByName("S1", "P1_E1_X1"),
                            getSampleOrDatasetPermIdByName("S1", "P1_E1_X1_D2"));
                    Set<String> currentEntries = listRemoteDirectory(sftp, datasetFilesRootDirectory);

                    assertTrue(currentEntries.containsAll(List.of(".", "..", "file_S1_P1_E1_X1_D2")));
                    assertFalse(currentEntries.contains("newdir"));

                    deleteTestFile(sftp,
                            "/spaces/S1/projects/P1/experiments/E1 (%s)/samples/X1 (%s)/datasets/D2 (%s)/files/file_S1_P1_E1_X1_D2"
                    );
                } catch (Exception e) {
                    exception = e;
                }
                assertNotNull(exception);
            }
        }
    }

    @Test(priority = 4)
    public void testAfsDeleteInMutableSample() throws Exception {
        try (SshClient client = SshClient.setUpDefaultClient()) {
            client.start();

            try (ClientSession session = client.connect(USER, SFTP_HOST, SFTP_PORT)
                    .verify(SFTP_TIMEOUT)
                    .getSession()) {
                session.addPasswordIdentity(AfsSftpServerIntegrationTestEnvironment.PASSWORD);
                session.auth().verify(SFTP_TIMEOUT);

                try (SftpClient sftp = SftpClientFactory.instance().createSftpClient(session)) {
                    String sampleFilesRootDirectory = String.format("/spaces/S1/projects/P1/experiments/E2 (%s)/samples/X1 (%s)/files",
                            getExperimentPermIdByName("S1", "P1", "E2"),
                            getSampleOrDatasetPermIdByName("S1", "P1_E2_X1"));
                    Set<String> currentEntries = listRemoteDirectory(sftp, sampleFilesRootDirectory);

                    byte[] content = "HELLO hello H3770".getBytes(StandardCharsets.UTF_8);

                    assertTrue(currentEntries.containsAll(List.of(".", "..")));
                    assertFalse(currentEntries.contains("newfile.txt"));

                    uploadTestFile(sftp,
                            sampleFilesRootDirectory + "/newfile.txt",
                            0L, content
                    );
                    assertEquals(
                            readTestFile(
                                    sftp,
                                    sampleFilesRootDirectory + "/newfile.txt",
                                    0L, 1024
                            ),
                            content
                    );

                    Set<String> newEntries = listRemoteDirectory(sftp, sampleFilesRootDirectory);
                    assertTrue(newEntries.containsAll(Set.of(".", "..", "newfile.txt")));

                    deleteTestFile(sftp, sampleFilesRootDirectory + "/newfile.txt");
                    newEntries = listRemoteDirectory(sftp, sampleFilesRootDirectory);
                    assertFalse(newEntries.contains("newfile.txt"));
                }
            }
        }
    }

    @Test(priority = 5)
    public void testAfsRenameInImmutableDatasetFails() throws Exception {
        try (SshClient client = SshClient.setUpDefaultClient()) {
            client.start();

            try (ClientSession session = client.connect(USER, SFTP_HOST, SFTP_PORT)
                    .verify(SFTP_TIMEOUT)
                    .getSession()) {
                session.addPasswordIdentity(AfsSftpServerIntegrationTestEnvironment.PASSWORD);
                session.auth().verify(SFTP_TIMEOUT);

                Exception exception = null;
                try (SftpClient sftp = SftpClientFactory.instance().createSftpClient(session)) {
                    String datasetFilesRootDirectory = String.format("/spaces/S1/projects/P1/experiments/E1 (%s)/samples/X1 (%s)/datasets/D2 (%s)/files",
                            getExperimentPermIdByName("S1", "P1", "E1"),
                            getSampleOrDatasetPermIdByName("S1", "P1_E1_X1"),
                            getSampleOrDatasetPermIdByName("S1", "P1_E1_X1_D2"));
                    Set<String> currentEntries = listRemoteDirectory(sftp, datasetFilesRootDirectory);

                    assertTrue(currentEntries.containsAll(List.of(".", "..", "file_S1_P1_E1_X1_D2")));
                    assertFalse(currentEntries.contains("newdir"));

                    renameTestFile(sftp,
                            "/spaces/S1/projects/P1/experiments/E1 (%s)/samples/X1 (%s)/datasets/D2 (%s)/files/file_S1_P1_E1_X1_D2",
                            "/spaces/S1/projects/P1/experiments/E1 (%s)/samples/X1 (%s)/datasets/D2 (%s)/files/renamed_file_S1_P1_E1_X1_D2"

                    );
                } catch (Exception e) {
                    exception = e;
                }
                assertNotNull(exception);
            }
        }
    }

    @Test(priority = 5)
    public void testAfsRenameInMutableSample() throws Exception {
        try (SshClient client = SshClient.setUpDefaultClient()) {
            client.start();

            try (ClientSession session = client.connect(USER, SFTP_HOST, SFTP_PORT)
                    .verify(SFTP_TIMEOUT)
                    .getSession()) {
                session.addPasswordIdentity(AfsSftpServerIntegrationTestEnvironment.PASSWORD);
                session.auth().verify(SFTP_TIMEOUT);

                try (SftpClient sftp = SftpClientFactory.instance().createSftpClient(session)) {
                    String sampleFilesRootDirectory = String.format("/spaces/S1/projects/P1/experiments/E2 (%s)/samples/X1 (%s)/files",
                            getExperimentPermIdByName("S1", "P1", "E2"),
                            getSampleOrDatasetPermIdByName("S1", "P1_E2_X1"));
                    Set<String> currentEntries = listRemoteDirectory(sftp, sampleFilesRootDirectory);

                    byte[] content = "HELLO hello H3770".getBytes(StandardCharsets.UTF_8);

                    assertTrue(currentEntries.containsAll(List.of(".", "..")));
                    assertFalse(currentEntries.contains("newfile.txt"));

                    uploadTestFile(sftp,
                            sampleFilesRootDirectory + "/newfile.txt",
                            0L, content
                    );
                    assertEquals(
                            readTestFile(
                                    sftp,
                                    sampleFilesRootDirectory + "/newfile.txt",
                                    0L, 1024
                            ),
                            content
                    );

                    Set<String> newEntries = listRemoteDirectory(sftp, sampleFilesRootDirectory);
                    assertTrue(newEntries.containsAll(Set.of(".", "..", "newfile.txt")));

                    renameTestFile(sftp,
                            sampleFilesRootDirectory + "/newfile.txt",
                            sampleFilesRootDirectory + "/renamed_newfile.txt"
                            );
                    newEntries = listRemoteDirectory(sftp, sampleFilesRootDirectory);
                    assertFalse(newEntries.contains("newfile.txt"));
                    assertTrue(newEntries.contains("renamed_newfile.txt"));
                }
            }
        }
    }

    private static Set<String> listRemoteDirectory(SftpClient sftp, String dirPath) throws Exception {
        try (SftpClient.CloseableHandle fileHandle = sftp.openDir(dirPath))
        {
            return sftp.readDir(fileHandle)
                    .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet());
        }
    }

    private static byte[] readTestFile(SftpClient sftp, String filePath, long offset, int maximum) throws Exception
    {
        try (SftpClient.CloseableHandle fileHandle = sftp.open(filePath, SftpClient.OpenMode.Read))
        {
            byte[] bytes = new byte[maximum];
            int readBytes = sftp.read(fileHandle, offset, bytes);
            return Arrays.copyOfRange(bytes, 0, readBytes);
        }
    }

    private static void uploadTestFile(SftpClient sftp, String filePath, long offset, byte[] content) throws Exception
    {
        try (SftpClient.CloseableHandle fileHandle = sftp.open(filePath, SftpClient.OpenMode.Write, SftpClient.OpenMode.Create))
        {
            sftp.write(fileHandle, offset, content);
        }
    }

    private static void deleteTestFile(SftpClient sftp, String filePath) throws Exception {
        sftp.remove(filePath);
    }

    private static void createRemoteDirectory(SftpClient sftp, String filePath) throws Exception {
        sftp.mkdir(filePath);
    }

    private static void renameTestFile(SftpClient sftp, String filePath, String newFilePath) throws Exception {
        sftp.rename(filePath, newFilePath);
    }
}
