package ch.ethz.sis.openbis.systemtests.suite.sftp;

import ch.ethz.sis.openbis.systemtests.suite.sftp.environment.AfsSftpServerIntegrationTestEnvironment;
import org.apache.commons.io.IOUtils;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static ch.ethz.sis.openbis.systemtests.suite.sftp.environment.AfsSftpServerIntegrationTestEnvironment.*;
import static org.testng.Assert.assertEquals;

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

    @Test
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
                                sftp.readDir(sftp.openDir("/"))
                                .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                Set.of(".", "spaces")
                        );

                        assertEquals(
                                sftp.readDir(sftp.openDir("/spaces/" + space ))
                                        .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                Set.of(".", "..", "projects", "folders", "samples")
                        );

                        assertEquals(
                                sftp.readDir(sftp.openDir("/spaces/" + space + "/samples"))
                                        .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
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
                                    sftp.readDir(sftp.openDir("/spaces/" + space + "/samples/" + spaceSampleDisplayName))
                                            .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                    Set.of(".", "..", "folders", "samples", "datasets", "files")
                            );

                            assertEquals(
                                    sftp.readDir(sftp.openDir("/spaces/" + space + "/samples/" + spaceSampleDisplayName + "/folders"))
                                            .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                    Set.of(".", "..")
                            );

                            assertEquals(
                                    sftp.readDir(sftp.openDir("/spaces/" + space + "/samples/" + spaceSampleDisplayName + "/samples"))
                                            .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                    Set.of(".", "..")
                            );

                            assertEquals(
                                    sftp.readDir(sftp.openDir("/spaces/" + space + "/samples/" + spaceSampleDisplayName + "/datasets"))
                                            .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                    Set.of(".", "..")
                            );

                            assertEquals(
                                    sftp.readDir(sftp.openDir("/spaces/" + space + "/samples/" + spaceSampleDisplayName + "/files"))
                                            .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                    Set.of(".", "..")
                            );
                        }

                        assertEquals(
                                sftp.readDir(sftp.openDir("/spaces/" + space + "/folders"))
                                        .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                Set.of(".", "..",
                                        String.format("%s (%s)", TEST_FOLDER_1, getSampleOrDatasetPermIdByName(space, TEST_FOLDER_1)),
                                        String.format("%s (%s)", TEST_FOLDER_2, getSampleOrDatasetPermIdByName(space, TEST_FOLDER_2))
                                )
                        );

                        for (String spaceFolder : List.of(TEST_FOLDER_1, TEST_FOLDER_2)) {
                            String spaceFolderDisplayName = String.format("%s (%s)", spaceFolder, getSampleOrDatasetPermIdByName(space, spaceFolder));

                            assertEquals(
                                    sftp.readDir(sftp.openDir("/spaces/" + space + "/folders/" + spaceFolderDisplayName))
                                            .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                    Set.of(".", "..", "folders", "samples", "datasets", "files")
                            );

                            assertEquals(
                                    sftp.readDir(sftp.openDir("/spaces/" + space + "/folders/" + spaceFolderDisplayName + "/folders"))
                                            .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                    Set.of(".", "..")
                            );

                            assertEquals(
                                    sftp.readDir(sftp.openDir("/spaces/" + space + "/folders/" + spaceFolderDisplayName + "/samples"))
                                            .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                    Set.of(".", "..",
                                            String.format("%s (%s)", spaceFolder + "_" + TEST_SUBSAMPLE_1, getSampleOrDatasetPermIdByName(space, spaceFolder + "_" + TEST_SUBSAMPLE_1)),
                                            String.format("%s (%s)", spaceFolder + "_" + TEST_SUBSAMPLE_2, getSampleOrDatasetPermIdByName(space, spaceFolder + "_" + TEST_SUBSAMPLE_2))
                                    )
                            );

                            assertEquals(
                                    sftp.readDir(sftp.openDir("/spaces/" + space + "/folders/" + spaceFolderDisplayName + "/datasets"))
                                            .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                    Set.of(".", "..")
                            );

                            assertEquals(
                                    sftp.readDir(sftp.openDir("/spaces/" + space + "/folders/" + spaceFolderDisplayName + "/files"))
                                            .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                    Set.of(".", "..")
                            );

                            for (String subsample : List.of(TEST_SUBSAMPLE_1, TEST_SUBSAMPLE_2)) {
                                String spaceSubsampleDisplayName = String.format("%s (%s)", spaceFolder + "_" + subsample, getSampleOrDatasetPermIdByName(space, spaceFolder + "_" + subsample));

                                assertEquals(
                                        sftp.readDir(sftp.openDir("/spaces/" + space + "/folders/" + spaceFolder + "/samples/" + spaceSubsampleDisplayName))
                                                .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                        Set.of(".", "..", "folders", "samples", "datasets", "files")
                                );

                                assertEquals(
                                        sftp.readDir(sftp.openDir("/spaces/" + space + "/folders/" + spaceFolder + "/samples/" + spaceSubsampleDisplayName + "/folders"))
                                                .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                        Set.of(".", "..")
                                );

                                assertEquals(
                                        sftp.readDir(sftp.openDir("/spaces/" + space + "/folders/" + spaceFolder + "/samples/" + spaceSubsampleDisplayName + "/samples"))
                                                .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                        Set.of(".", "..")
                                );

                                assertEquals(
                                        sftp.readDir(sftp.openDir("/spaces/" + space + "/folders/" + spaceFolder + "/samples/" + spaceSubsampleDisplayName + "/datasets"))
                                                .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                        Set.of(".", "..")
                                );

                                assertEquals(
                                        sftp.readDir(sftp.openDir("/spaces/" + space + "/folders/" + spaceFolder + "/samples/" + spaceSubsampleDisplayName + "/files"))
                                                .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                        Set.of(".", "..")
                                );
                            }
                        }

                        for (String project : List.of(TEST_PROJECT_1, TEST_PROJECT_2)) {
                            assertEquals(
                                    sftp.readDir(sftp.openDir("/spaces/" + space + "/projects/" + project))
                                            .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                    Set.of(".", "..", "experiments", "folders", "samples")
                            );

                            assertEquals(
                                    sftp.readDir(sftp.openDir("/spaces/" + space + "/projects/" + project + "/experiments"))
                                            .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                    Set.of(".", "..",
                                            String.format("%s (%s)", TEST_EXPERIMENT_1, getExperimentPermIdByName(space, project, TEST_EXPERIMENT_1)),
                                            String.format("%s (%s)", TEST_EXPERIMENT_2, getExperimentPermIdByName(space, project, TEST_EXPERIMENT_2))
                                    )
                            );

                            assertEquals(
                                    sftp.readDir(sftp.openDir("/spaces/" + space + "/projects/" + project + "/samples"))
                                            .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
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
                                    sftp.readDir(sftp.openDir("/spaces/" + space + "/projects/" + project + "/folders"))
                                            .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                    Set.of(".", "..",
                                            String.format("%s (%s)", project + "_" + TEST_FOLDER_1, getSampleOrDatasetPermIdByName(space, project + "_" + TEST_FOLDER_1)),
                                            String.format("%s (%s)", project + "_" + TEST_FOLDER_2, getSampleOrDatasetPermIdByName(space, project + "_" + TEST_FOLDER_2))
                                    )
                            );
                            for (String projectSample : List.of(TEST_SAMPLE_1, TEST_SAMPLE_2)) {
                                String projectSampleDisplayName = String.format("%s (%s)", project + "_" + projectSample, getSampleOrDatasetPermIdByName(space, project + "_" + projectSample));

                                assertEquals(
                                        sftp.readDir(sftp.openDir("/spaces/" + space + "/projects/" + project + "/samples/" + projectSampleDisplayName))
                                                .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                        Set.of(".", "..", "folders", "samples", "datasets", "files")
                                );

                                assertEquals(
                                        sftp.readDir(sftp.openDir("/spaces/" + space + "/projects/" + project + "/samples/" + projectSampleDisplayName + "/folders"))
                                                .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                        Set.of(".", "..")
                                );

                                assertEquals(
                                        sftp.readDir(sftp.openDir("/spaces/" + space + "/projects/" + project + "/samples/" + projectSampleDisplayName + "/samples"))
                                                .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                        Set.of(".", "..")
                                );

                                assertEquals(
                                        sftp.readDir(sftp.openDir("/spaces/" + space + "/projects/" + project + "/samples/" + projectSampleDisplayName + "/datasets"))
                                                .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                        Set.of(".", "..")
                                );

                                assertEquals(
                                        sftp.readDir(sftp.openDir("/spaces/" + space + "/projects/" + project + "/samples/" + projectSampleDisplayName + "/files"))
                                                .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                        Set.of(".", "..")
                                );
                            }
                            for (String projectFolder : List.of(TEST_FOLDER_1, TEST_FOLDER_2)) {
                                String projectFolderDisplayName = String.format("%s (%s)", project + "_" + projectFolder, getSampleOrDatasetPermIdByName(space, project + "_" + projectFolder));

                                assertEquals(
                                        sftp.readDir(sftp.openDir("/spaces/" + space + "/projects/" + project + "/folders/" + projectFolderDisplayName))
                                                .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                        Set.of(".", "..", "folders", "samples", "datasets", "files")
                                );

                                assertEquals(
                                        sftp.readDir(sftp.openDir("/spaces/" + space + "/projects/" + project + "/folders/" + projectFolderDisplayName + "/folders"))
                                                .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                        Set.of(".", "..")
                                );

                                assertEquals(
                                        sftp.readDir(sftp.openDir("/spaces/" + space + "/projects/" + project + "/folders/" + projectFolderDisplayName + "/samples"))
                                                .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                        Set.of(".", "..",
                                                String.format("%s (%s)", project + "_" + projectFolder + "_" + TEST_SUBSAMPLE_1, getSampleOrDatasetPermIdByName(space, project + "_" + projectFolder + "_" + TEST_SUBSAMPLE_1)),
                                                String.format("%s (%s)", project + "_" + projectFolder + "_" + TEST_SUBSAMPLE_2, getSampleOrDatasetPermIdByName(space, project + "_" + projectFolder + "_" + TEST_SUBSAMPLE_2))
                                        )
                                );

                                assertEquals(
                                        sftp.readDir(sftp.openDir("/spaces/" + space + "/projects/" + project + "/folders/" + projectFolderDisplayName + "/datasets"))
                                                .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                        Set.of(".", "..")
                                );

                                assertEquals(
                                        sftp.readDir(sftp.openDir("/spaces/" + space + "/projects/" + project + "/folders/" + projectFolderDisplayName + "/files"))
                                                .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                        Set.of(".", "..")
                                );
                                for (String projectSubsample : List.of(TEST_SUBSAMPLE_1, TEST_SUBSAMPLE_2)) {
                                    String projectSubsampleDisplayName = String.format("%s (%s)", project + "_" + projectFolder + "_" + projectSubsample,
                                            getSampleOrDatasetPermIdByName(space, project + "_" + projectFolder + "_" + projectSubsample));

                                    assertEquals(
                                            sftp.readDir(sftp.openDir("/spaces/" + space + "/projects/" + project + "/folders/" + projectSubsampleDisplayName + "/samples/" + projectSubsampleDisplayName))
                                                    .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                            Set.of(".", "..", "folders", "samples", "datasets", "files")
                                    );

                                    assertEquals(
                                            sftp.readDir(sftp.openDir("/spaces/" + space + "/projects/" + project + "/folders/" + projectSubsampleDisplayName + "/samples/" + projectSubsampleDisplayName + "/folders"))
                                                    .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                            Set.of(".", "..")
                                    );

                                    assertEquals(
                                            sftp.readDir(sftp.openDir("/spaces/" + space + "/projects/" + project + "/folders/" + projectSubsampleDisplayName + "/samples/" + projectSubsampleDisplayName + "/samples"))
                                                    .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                            Set.of(".", "..")
                                    );

                                    assertEquals(
                                            sftp.readDir(sftp.openDir("/spaces/" + space + "/projects/" + project + "/folders/" + projectSubsampleDisplayName + "/samples/" + projectSubsampleDisplayName + "/datasets"))
                                                    .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                            Set.of(".", "..")
                                    );

                                    assertEquals(
                                            sftp.readDir(sftp.openDir("/spaces/" + space + "/projects/" + project + "/folders/" + projectSubsampleDisplayName + "/samples/" + projectSubsampleDisplayName + "/files"))
                                                    .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                            Set.of(".", "..")
                                    );
                                }
                            }

                            for (String experiment : List.of(TEST_EXPERIMENT_1, TEST_EXPERIMENT_2)) {
                                String experimentDisplayName = String.format("%s (%s)", experiment, getExperimentPermIdByName(space, project, experiment));
                                assertEquals(
                                        sftp.readDir(sftp.openDir("/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName))
                                                .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                        Set.of(".", "..", "folders", "samples", "datasets", "files")
                                );

                                assertEquals(
                                        sftp.readDir(sftp.openDir("/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/samples"))
                                                .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
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
                                        sftp.readDir(sftp.openDir("/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/folders"))
                                                .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                        Set.of(".", "..",
                                                String.format("%s (%s)", project + "_" + experiment + "_" + TEST_FOLDER_1,
                                                        getSampleOrDatasetPermIdByName(space, project + "_" + experiment + "_" + TEST_FOLDER_1)),
                                                String.format("%s (%s)", project + "_" + experiment + "_" + TEST_FOLDER_2,
                                                        getSampleOrDatasetPermIdByName(space, project + "_" + experiment + "_" + TEST_FOLDER_2))
                                        )
                                );

                                assertEquals(
                                        sftp.readDir(sftp.openDir("/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/datasets"))
                                                .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
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
                                        sftp.readDir(sftp.openDir("/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/files"))
                                                .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                        Set.of(".", "..")
                                );

                                for (String experimentSample : List.of(TEST_SAMPLE_1, TEST_SAMPLE_2)) {
                                    String experimentSampleDisplayName = String.format("%s (%s)", project + "_" + experiment + "_" + experimentSample,
                                            getSampleOrDatasetPermIdByName(space, project + "_" + experiment + "_" + experimentSample));

                                    assertEquals(
                                            sftp.readDir(sftp.openDir("/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/samples/" + experimentSampleDisplayName))
                                                    .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                            Set.of(".", "..", "folders", "samples", "datasets", "files")
                                    );

                                    assertEquals(
                                            sftp.readDir(sftp.openDir("/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/samples/" + experimentSampleDisplayName + "/folders"))
                                                    .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                            Set.of(".", "..")
                                    );

                                    assertEquals(
                                            sftp.readDir(sftp.openDir("/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/samples/" + experimentSampleDisplayName + "/samples"))
                                                    .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                            Set.of(".", "..")
                                    );

                                    assertEquals(
                                            sftp.readDir(sftp.openDir("/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/samples/" + experimentSampleDisplayName + "/datasets"))
                                                    .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                            Set.of(".", "..",
                                                    String.format("%s (%s)", project + "_" + experiment + "_" + experimentSample + "_" + TEST_DATASET_1,
                                                            getSampleOrDatasetPermIdByName(space, project + "_" + experiment + "_" + experimentSample + "_" + TEST_DATASET_1)),
                                                    String.format("%s (%s)", project + "_" + experiment + "_" + experimentSample + "_" + TEST_DATASET_2,
                                                            getSampleOrDatasetPermIdByName(space, project + "_" + experiment + "_" + experimentSample + "_" + TEST_DATASET_2))
                                            )
                                    );

                                    assertEquals(
                                            sftp.readDir(sftp.openDir("/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/samples/" + experimentSampleDisplayName + "/files"))
                                                    .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                            Set.of(".", "..")
                                    );
                                    for (String experimentSampleDataset : List.of(TEST_DATASET_1, TEST_DATASET_2)) {
                                        String datasetDisplayName = String.format("%s (%s)", project + "_" + experiment + "_" + experimentSample + "_" + experimentSampleDataset,
                                                getSampleOrDatasetPermIdByName(space, project + "_" + experiment + "_" + experimentSample + "_" + experimentSampleDataset));

                                        assertEquals(
                                                sftp.readDir(sftp.openDir("/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/samples/" + experimentSampleDisplayName + "/datasets/" + datasetDisplayName))
                                                        .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                                Set.of(".", "..", "files")
                                        );
                                        assertEquals(
                                                sftp.readDir(sftp.openDir("/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/samples/" + experimentSampleDisplayName + "/datasets/" + datasetDisplayName + "/files"))
                                                        .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                                Set.of(".", "..",
                                                        "file_" + space + "_" + project + "_" + experiment + "_" + experimentSample + "_" + experimentSampleDataset
                                                )
                                        );
                                    }
                                }
                                for (String experimentFolder : List.of(TEST_FOLDER_1, TEST_FOLDER_2)) {
                                    String experimentFolderDisplayName = String.format("%s (%s)", project + "_" + experiment + "_" + experimentFolder,
                                            getSampleOrDatasetPermIdByName(space, project + "_" + experiment + "_" + experimentFolder));

                                    assertEquals(
                                            sftp.readDir(sftp.openDir("/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/folders/" + experimentFolderDisplayName))
                                                    .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                            Set.of(".", "..", "folders", "samples", "datasets", "files")
                                    );

                                    assertEquals(
                                            sftp.readDir(sftp.openDir("/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/folders/" + experimentFolderDisplayName + "/folders"))
                                                    .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                            Set.of(".", "..")
                                    );

                                    assertEquals(
                                            sftp.readDir(sftp.openDir("/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/folders/" + experimentFolderDisplayName + "/samples"))
                                                    .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                            Set.of(".", "..",
                                                    String.format("%s (%s)", project + "_" + experiment + "_" + experimentFolder + "_" + TEST_SUBSAMPLE_1,
                                                            getSampleOrDatasetPermIdByName(space, project + "_" + experiment + "_" + experimentFolder + "_" + TEST_SUBSAMPLE_1)),
                                                    String.format("%s (%s)", project + "_" + experiment + "_" + experimentFolder + "_" + TEST_SUBSAMPLE_2,
                                                            getSampleOrDatasetPermIdByName(space, project + "_" + experiment + "_" + experimentFolder + "_" + TEST_SUBSAMPLE_2))
                                            )
                                    );

                                    assertEquals(
                                            sftp.readDir(sftp.openDir("/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/folders/" + experimentFolderDisplayName + "/datasets"))
                                                    .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                            Set.of(".", "..")
                                    );

                                    assertEquals(
                                            sftp.readDir(sftp.openDir("/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/folders/" + experimentFolderDisplayName + "/files"))
                                                    .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                            Set.of(".", "..")
                                    );
                                    for (String experimentSubsample : List.of(TEST_SUBSAMPLE_1, TEST_SUBSAMPLE_2)) {
                                        String experimentSubsampleDisplayName = String.format("%s (%s)", project + "_" + experiment + "_" + experimentFolder + "_" + experimentSubsample,
                                                getSampleOrDatasetPermIdByName(space, project + "_" + experiment + "_" + experimentFolder + "_" + experimentSubsample));

                                        assertEquals(
                                                sftp.readDir(sftp.openDir("/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/folders/" + experimentFolderDisplayName + "/samples/" + experimentSubsampleDisplayName))
                                                        .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                                Set.of(".", "..", "folders", "samples", "datasets", "files")
                                        );

                                        assertEquals(
                                                sftp.readDir(sftp.openDir("/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/folders/" + experimentFolderDisplayName + "/samples/" + experimentSubsampleDisplayName + "/folders"))
                                                        .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                                Set.of(".", "..")
                                        );

                                        assertEquals(
                                                sftp.readDir(sftp.openDir("/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/folders/" + experimentFolderDisplayName + "/samples/" + experimentSubsampleDisplayName + "/samples"))
                                                        .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                                Set.of(".", "..")
                                        );

                                        assertEquals(
                                                sftp.readDir(sftp.openDir("/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/folders/" + experimentFolderDisplayName + "/samples/" + experimentSubsampleDisplayName + "/datasets"))
                                                        .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
                                                Set.of(".", "..")
                                        );

                                        assertEquals(
                                                sftp.readDir(sftp.openDir("/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/folders/" + experimentFolderDisplayName + "/samples/" + experimentSubsampleDisplayName + "/files"))
                                                        .stream().map(SftpClient.DirEntry::getFilename).collect(Collectors.toSet()),
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

    private byte[] readFile(SftpClient sftp, String filePath) throws Exception
    {
        try (InputStream inputStream = sftp.read(filePath))
        {
            return IOUtils.toByteArray(inputStream);
        }
    }
}
