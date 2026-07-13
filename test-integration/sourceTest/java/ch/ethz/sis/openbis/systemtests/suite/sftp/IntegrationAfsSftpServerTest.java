package ch.ethz.sis.openbis.systemtests.suite.sftp;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.fetchoptions.ProjectFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.ProjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SamplePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.fetchoptions.SpaceFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
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
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
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
                                                        "file_ % = # ~ $ " + space + "_" + project + "_" + experiment + "_" + experimentSample + "_" + experimentSampleDataset
                                                )
                                        );
                                        assertEquals(
                                                readTestFile(sftp,
                                                        "/spaces/" + space + "/projects/" + project + "/experiments/" + experimentDisplayName + "/samples/" + experimentSampleDisplayName + "/datasets/" + datasetDisplayName + "/files/" +
                                                        "file_ % = # ~ $ " + space + "_" + project + "_" + experiment + "_" + experimentSample + "_" + experimentSampleDataset,
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

                    assertTrue(currentEntries.containsAll(List.of(".", "..", "file_ % = # ~ $ S1_P1_E1_X1_D1")));
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

                    byte[] biggerContent = new byte[40000000];
                    ThreadLocalRandom.current().nextBytes(biggerContent);

                     uploadTestFile(sftp,
                            sampleFilesRootDirectory + "/newfile2.txt",
                            0L, biggerContent
                    );
                    assertEquals(
                            readTestFile(
                                    sftp,
                                    sampleFilesRootDirectory + "/newfile2.txt",
                                    0L, 42000000
                            ),
                            biggerContent
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

                    assertTrue(currentEntries.containsAll(List.of(".", "..", "file_ % = # ~ $ S1_P1_E1_X1_D2")));
                    assertFalse(currentEntries.contains("newdir"));

                    deleteTestFile(sftp,
                            String.format("/spaces/S1/projects/P1/experiments/E1 (%s)/samples/X1 (%s)/datasets/D2 (%s)/files/file_ %% = # ~ $ S1_P1_E1_X1_D2",
                                    getExperimentPermIdByName("S1", "P1", "E1"),
                                    getSampleOrDatasetPermIdByName("S1", "P1_E1_X1"),
                                    getSampleOrDatasetPermIdByName("S1", "P1_E1_X1_D2"))
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

                    assertTrue(currentEntries.containsAll(List.of(".", "..", "file_ % = # ~ $ S1_P1_E1_X1_D2")));
                    assertFalse(currentEntries.contains("newdir"));

                    renameTestFile(sftp,
                            "/spaces/S1/projects/P1/experiments/E1 (%s)/samples/X1 (%s)/datasets/D2 (%s)/files/file_ % = # ~ $ S1_P1_E1_X1_D2",
                            "/spaces/S1/projects/P1/experiments/E1 (%s)/samples/X1 (%s)/datasets/D2 (%s)/files/renamed_file_ % = # ~ $ S1_P1_E1_X1_D2"

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

    @Test(priority = 6)
    public void testEntityDirectoriesCreationAndDeletion() throws Exception {
        OpenBIS openBIS = environment.createOpenBIS();
        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        try (SshClient client = SshClient.setUpDefaultClient()) {
            client.start();

            try (ClientSession session = client.connect(USER, SFTP_HOST, SFTP_PORT)
                    .verify(SFTP_TIMEOUT)
                    .getSession()) {
                session.addPasswordIdentity(AfsSftpServerIntegrationTestEnvironment.PASSWORD);
                session.auth().verify(SFTP_TIMEOUT);

                try (SftpClient sftp = SftpClientFactory.instance().createSftpClient(session)) {
                    // Create and delete space
                    assertFalse(listRemoteDirectory(sftp, "/spaces").contains("CREATED_SPACE_1"));
                    sftp.mkdir("/spaces/CREATED_SPACE_1");
                    assertTrue(listRemoteDirectory(sftp, "/spaces").contains("CREATED_SPACE_1"));
                    assertNotNull(openBIS.getSpaces(
                        List.of(new SpacePermId("CREATED_SPACE_1")
                        ), new SpaceFetchOptions()
                    ).get(new SpacePermId("CREATED_SPACE_1")));
                    sftp.rmdir("/spaces/CREATED_SPACE_1");
                    assertFalse(listRemoteDirectory(sftp, "/spaces").contains("CREATED_SPACE_1"));
                    assertNull(openBIS.getSpaces(
                            List.of(new SpacePermId("CREATED_SPACE_1")
                            ), new SpaceFetchOptions()
                    ).get(new SpacePermId("CREATED_SPACE_1")));

                    // Create and delete project
                    sftp.mkdir("/spaces/CREATED_SPACE");
                    assertFalse(listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/projects").contains("CREATED_PROJECT_1"));
                    sftp.mkdir("/spaces/CREATED_SPACE/projects/CREATED_PROJECT_1");
                    assertTrue(listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/projects").contains("CREATED_PROJECT_1"));
                    assertNotNull(openBIS.getProjects(
                            List.of(new ProjectIdentifier("CREATED_SPACE", "CREATED_PROJECT_1")
                            ), new ProjectFetchOptions()
                    ).get(new ProjectIdentifier("CREATED_SPACE", "CREATED_PROJECT_1")));
                    sftp.rmdir("/spaces/CREATED_SPACE/projects/CREATED_PROJECT_1");
                    assertFalse(listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/projects").contains("CREATED_PROJECT_1"));
                    assertNull(openBIS.getProjects(
                            List.of(new ProjectIdentifier("CREATED_SPACE", "CREATED_PROJECT_1")
                            ), new ProjectFetchOptions()
                    ).get(new ProjectIdentifier("CREATED_SPACE", "CREATED_PROJECT_1")));

                    // Create and delete experiment
                    sftp.mkdir("/spaces/CREATED_SPACE/projects/CREATED_PROJECT");
                    assertFalse(listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments")
                            .stream().anyMatch( entry -> entry.startsWith("CREATED_EXP_1") ));
                    sftp.mkdir("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/CREATED_EXP_1");
                    assertTrue(listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments")
                            .stream().anyMatch( entry -> entry.startsWith("CREATED_EXP_1") ));
                    String createdExp1PermId = listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments")
                            .stream().filter( entry -> entry.startsWith("CREATED_EXP_1") ).findFirst().get()
                            .split("\\(")[1].split("\\)")[0].trim();
                    assertNotNull(openBIS.getExperiments(
                            List.of(new ExperimentPermId(createdExp1PermId)
                            ), new ExperimentFetchOptions()
                    ).get(new ExperimentPermId(createdExp1PermId)));
                    sftp.rmdir(String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/(%s)", createdExp1PermId));
                    assertFalse(listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments")
                            .stream().anyMatch( entry -> entry.startsWith("CREATED_EXP_1") ));
                    assertNull(openBIS.getExperiments(
                            List.of(new ExperimentPermId(createdExp1PermId)
                            ), new ExperimentFetchOptions()
                    ).get(new ExperimentPermId(createdExp1PermId)));

                    // Create and delete samples
                    sftp.mkdir("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/CREATED_EXP");
                    String createdExpPermId = listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments")
                            .stream().filter( entry -> entry.startsWith("CREATED_EXP") ).findFirst().get()
                            .split("\\(")[1].split("\\)")[0].trim();

                    assertFalse(listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/samples")
                            .stream().anyMatch( entry -> entry.startsWith("CREATED_SMP") ));
                    assertFalse(listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/projects/CREATED_PROJECT/samples")
                            .stream().anyMatch( entry -> entry.startsWith("CREATED_SMP") ));
                    assertFalse(listRemoteDirectory(sftp, String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/(%s)/samples", createdExpPermId))
                            .stream().anyMatch( entry -> entry.startsWith("CREATED_SMP") ));

                    sftp.mkdir("/spaces/CREATED_SPACE/samples/CREATED_SMP_1");
                    sftp.mkdir("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/samples/CREATED_SMP_2");
                    sftp.mkdir(String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/(%s)/samples/CREATED_SMP_3", createdExpPermId));

                    assertTrue(listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/samples")
                            .stream().anyMatch( entry -> entry.startsWith("CREATED_SMP_1") ));
                    assertTrue(listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/projects/CREATED_PROJECT/samples")
                            .stream().anyMatch( entry -> entry.startsWith("CREATED_SMP_2") ));
                    assertTrue(listRemoteDirectory(sftp, String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/(%s)/samples", createdExpPermId))
                            .stream().anyMatch( entry -> entry.startsWith("CREATED_SMP_3") ));

                    String createdSmp1PermId = listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/samples")
                            .stream().filter( entry -> entry.startsWith("CREATED_SMP_1") ).findFirst().get()
                            .split("\\(")[1].split("\\)")[0].trim();
                    String createdSmp2PermId = listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/projects/CREATED_PROJECT/samples")
                            .stream().filter( entry -> entry.startsWith("CREATED_SMP_2") ).findFirst().get()
                            .split("\\(")[1].split("\\)")[0].trim();
                    String createdSmp3PermId = listRemoteDirectory(sftp, String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/(%s)/samples", createdExpPermId))
                            .stream().filter( entry -> entry.startsWith("CREATED_SMP_3") ).findFirst().get()
                            .split("\\(")[1].split("\\)")[0].trim();

                    {
                        //SUB-CASE: sample under sample
                        assertFalse(listRemoteDirectory(sftp, String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/(%s)/samples/(%s)/samples", createdExpPermId, createdSmp3PermId))
                                .stream().anyMatch( entry -> entry.startsWith("CREATED_SMP") ));

                        sftp.mkdir(String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/(%s)/samples/(%s)/samples/CREATED_SMP_4",
                                createdExpPermId, createdSmp3PermId));

                        assertTrue(listRemoteDirectory(sftp, String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/(%s)/samples/(%s)/samples", createdExpPermId, createdSmp3PermId))
                                .stream().anyMatch( entry -> entry.startsWith("CREATED_SMP_4") ));

                        String createdSmp4PermId = listRemoteDirectory(sftp, String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/(%s)/samples/(%s)/samples", createdExpPermId, createdSmp3PermId))
                                .stream().filter( entry -> entry.startsWith("CREATED_SMP_4") ).findFirst().get()
                                .split("\\(")[1].split("\\)")[0].trim();

                        sftp.rmdir(String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/(%s)/samples/(%s)/samples/CREATED_SMP_4(%s)",
                                createdExpPermId, createdSmp3PermId, createdSmp4PermId));

                        assertFalse(listRemoteDirectory(sftp, String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/(%s)/samples/(%s)/samples", createdExpPermId, createdSmp3PermId))
                                .stream().anyMatch( entry -> entry.startsWith("CREATED_SMP") ));
                    }

                    assertNotNull(openBIS.getSamples(
                            List.of(new SamplePermId(createdSmp1PermId)
                            ), new SampleFetchOptions()
                    ).get(new SamplePermId(createdSmp1PermId)));
                    assertNotNull(openBIS.getSamples(
                            List.of(new SamplePermId(createdSmp2PermId)
                            ), new SampleFetchOptions()
                    ).get(new SamplePermId(createdSmp2PermId)));
                    assertNotNull(openBIS.getSamples(
                            List.of(new SamplePermId(createdSmp3PermId)
                            ), new SampleFetchOptions()
                    ).get(new SamplePermId(createdSmp3PermId)));

                    sftp.rmdir(String.format("/spaces/CREATED_SPACE/samples/(%s)", createdSmp1PermId));
                    sftp.rmdir(String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/samples/(%s)", createdSmp2PermId));
                    sftp.rmdir(String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/(%s)/samples/(%s)", createdExpPermId, createdSmp3PermId));

                    assertFalse(listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/samples")
                            .stream().anyMatch( entry -> entry.startsWith("CREATED_SMP_1") ));
                    assertFalse(listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/projects/CREATED_PROJECT/samples")
                            .stream().anyMatch( entry -> entry.startsWith("CREATED_SMP_2") ));
                    assertFalse(listRemoteDirectory(sftp, String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/(%s)/samples", createdExpPermId))
                            .stream().anyMatch( entry -> entry.startsWith("CREATED_SMP_3") ));

                    assertNull(openBIS.getSamples(
                            List.of(new SamplePermId(createdSmp1PermId)
                            ), new SampleFetchOptions()
                    ).get(new SamplePermId(createdSmp1PermId)));
                    assertNull(openBIS.getSamples(
                            List.of(new SamplePermId(createdSmp2PermId)
                            ), new SampleFetchOptions()
                    ).get(new SamplePermId(createdSmp2PermId)));
                    assertNull(openBIS.getSamples(
                            List.of(new SamplePermId(createdSmp3PermId)
                            ), new SampleFetchOptions()
                    ).get(new SamplePermId(createdSmp3PermId)));

                    // Create and delete folders (folder-type samples)
                    assertFalse(listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/folders")
                            .stream().anyMatch( entry -> entry.startsWith("CREATED_FLD") ));
                    assertFalse(listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/projects/CREATED_PROJECT/folders")
                            .stream().anyMatch( entry -> entry.startsWith("CREATED_FLD") ));
                    assertFalse(listRemoteDirectory(sftp, String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/(%s)/folders", createdExpPermId))
                            .stream().anyMatch( entry -> entry.startsWith("CREATED_FLD") ));

                    sftp.mkdir("/spaces/CREATED_SPACE/folders/CREATED_FLD_1");
                    sftp.mkdir("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/folders/CREATED_FLD_2");
                    sftp.mkdir(String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/(%s)/folders/CREATED_FLD_3", createdExpPermId));

                    assertTrue(listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/folders")
                            .stream().anyMatch( entry -> entry.startsWith("CREATED_FLD_1") ));
                    assertTrue(listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/projects/CREATED_PROJECT/folders")
                            .stream().anyMatch( entry -> entry.startsWith("CREATED_FLD_2") ));
                    assertTrue(listRemoteDirectory(sftp, String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/(%s)/folders", createdExpPermId))
                            .stream().anyMatch( entry -> entry.startsWith("CREATED_FLD_3") ));

                    String createdFld1PermId = listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/folders")
                            .stream().filter( entry -> entry.startsWith("CREATED_FLD_1") ).findFirst().get()
                            .split("\\(")[1].split("\\)")[0].trim();
                    String createdFld2PermId = listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/projects/CREATED_PROJECT/folders")
                            .stream().filter( entry -> entry.startsWith("CREATED_FLD_2") ).findFirst().get()
                            .split("\\(")[1].split("\\)")[0].trim();
                    String createdFld3PermId = listRemoteDirectory(sftp, String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/(%s)/folders", createdExpPermId))
                            .stream().filter( entry -> entry.startsWith("CREATED_FLD_3") ).findFirst().get()
                            .split("\\(")[1].split("\\)")[0].trim();

                    {
                        //SUB-CASE: sample under folder
                        assertFalse(listRemoteDirectory(sftp, String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/(%s)/folders/(%s)/samples", createdExpPermId, createdFld3PermId))
                                .stream().anyMatch( entry -> entry.startsWith("CREATED_FLD") ));

                        sftp.mkdir(String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/(%s)/folders/(%s)/samples/CREATED_FLD_4",
                                createdExpPermId, createdFld3PermId));

                        assertTrue(listRemoteDirectory(sftp, String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/(%s)/folders/(%s)/samples", createdExpPermId, createdFld3PermId))
                                .stream().anyMatch( entry -> entry.startsWith("CREATED_FLD_4") ));

                        String createdFld4PermId = listRemoteDirectory(sftp, String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/(%s)/folders/(%s)/samples", createdExpPermId, createdFld3PermId))
                                .stream().filter( entry -> entry.startsWith("CREATED_FLD_4") ).findFirst().get()
                                .split("\\(")[1].split("\\)")[0].trim();

                        sftp.rmdir(String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/(%s)/folders/(%s)/samples/CREATED_FLD_4(%s)",
                                createdExpPermId, createdFld3PermId, createdFld4PermId));

                        assertFalse(listRemoteDirectory(sftp, String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/(%s)/folders/(%s)/samples", createdExpPermId, createdFld3PermId))
                                .stream().anyMatch( entry -> entry.startsWith("CREATED_FLD") ));
                    }

                    assertNotNull(openBIS.getSamples(
                            List.of(new SamplePermId(createdFld1PermId)
                            ), new SampleFetchOptions()
                    ).get(new SamplePermId(createdFld1PermId)));
                    assertNotNull(openBIS.getSamples(
                            List.of(new SamplePermId(createdFld2PermId)
                            ), new SampleFetchOptions()
                    ).get(new SamplePermId(createdFld2PermId)));
                    assertNotNull(openBIS.getSamples(
                            List.of(new SamplePermId(createdFld3PermId)
                            ), new SampleFetchOptions()
                    ).get(new SamplePermId(createdFld3PermId)));

                    sftp.rmdir(String.format("/spaces/CREATED_SPACE/folders/(%s)", createdFld1PermId));
                    sftp.rmdir(String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/folders/(%s)", createdFld2PermId));
                    sftp.rmdir(String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/(%s)/folders/(%s)", createdExpPermId, createdFld3PermId));

                    assertFalse(listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/folders")
                            .stream().anyMatch( entry -> entry.startsWith("CREATED_FLD_1") ));
                    assertFalse(listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/projects/CREATED_PROJECT/folders")
                            .stream().anyMatch( entry -> entry.startsWith("CREATED_FLD_2") ));
                    assertFalse(listRemoteDirectory(sftp, String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/(%s)/folders", createdExpPermId))
                            .stream().anyMatch( entry -> entry.startsWith("CREATED_FLD_3") ));

                    assertNull(openBIS.getSamples(
                            List.of(new SamplePermId(createdFld1PermId)
                            ), new SampleFetchOptions()
                    ).get(new SamplePermId(createdFld1PermId)));
                    assertNull(openBIS.getSamples(
                            List.of(new SamplePermId(createdFld2PermId)
                            ), new SampleFetchOptions()
                    ).get(new SamplePermId(createdFld2PermId)));
                    assertNull(openBIS.getSamples(
                            List.of(new SamplePermId(createdFld3PermId)
                            ), new SampleFetchOptions()
                    ).get(new SamplePermId(createdFld3PermId)));

                    //Delete dataset
                    String datasetDirectory = String.format("/spaces/S1/projects/P1/experiments/E1 (%s)/samples/X1 (%s)/datasets/D2 (%s)",
                            getExperimentPermIdByName("S1", "P1", "E1"),
                            getSampleOrDatasetPermIdByName("S1", "P1_E1_X1"),
                            getSampleOrDatasetPermIdByName("S1", "P1_E1_X1_D2"));

                    assertTrue(listRemoteDirectory(sftp, datasetDirectory).size() > 2);
                    sftp.rmdir(datasetDirectory);
                    assertTrue(listRemoteDirectory(sftp, String.format("/spaces/S1/projects/P1/experiments/E1 (%s)/samples/X1 (%s)/datasets",
                            getExperimentPermIdByName("S1", "P1", "E1"),
                            getSampleOrDatasetPermIdByName("S1", "P1_E1_X1"))).stream().noneMatch(
                                    entry -> entry.startsWith("D2")
                    ));
                }
            }
        }
    }

    @Test(priority = 7)
    public void testEntityDirectoriesRenaming() throws Exception {
        OpenBIS openBIS = environment.createOpenBIS();
        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        try (SshClient client = SshClient.setUpDefaultClient()) {
            client.start();

            try (ClientSession session = client.connect(USER, SFTP_HOST, SFTP_PORT)
                    .verify(SFTP_TIMEOUT)
                    .getSession()) {
                session.addPasswordIdentity(AfsSftpServerIntegrationTestEnvironment.PASSWORD);
                session.auth().verify(SFTP_TIMEOUT);

                try (SftpClient sftp = SftpClientFactory.instance().createSftpClient(session)) {
                    try {
                        sftp.mkdir("/spaces/CREATED_SPACE");
                        sftp.mkdir("/spaces/CREATED_SPACE/projects/CREATED_PROJECT");
                    } catch (Exception e) {}

                    // Create and rename experiment
                    assertFalse(listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments")
                            .stream().anyMatch( entry -> entry.startsWith("CREATED_EXP_2") ));
                    sftp.mkdir("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/CREATED_EXP_2");
                    assertTrue(listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments")
                            .stream().anyMatch( entry -> entry.startsWith("CREATED_EXP_2") ));
                    String createdExp2PermId = listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments")
                            .stream().filter( entry -> entry.startsWith("CREATED_EXP_2") ).findFirst().get()
                            .split("\\(")[1].split("\\)")[0].trim();

                    sftp.rename(
                            String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/(%s)", createdExp2PermId),
                            String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/renamed CREATED_EXP_2 (%s)", createdExp2PermId)
                    );
                    assertFalse(listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments")
                            .stream().anyMatch( entry -> entry.startsWith("CREATED_EXP_2") ));
                    assertTrue(listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments")
                            .stream().anyMatch( entry -> entry.startsWith("renamed CREATED_EXP_2") ));

                    // Create and rename samples
                    String createdExpPermId = listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments")
                            .stream().filter( entry -> entry.startsWith("renamed CREATED_EXP_2") ).findFirst().get()
                            .split("\\(")[1].split("\\)")[0].trim();

                    assertFalse(listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/samples")
                            .stream().anyMatch( entry -> entry.startsWith("CREATED_SMP") ));
                    assertFalse(listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/projects/CREATED_PROJECT/samples")
                            .stream().anyMatch( entry -> entry.startsWith("CREATED_SMP") ));
                    assertFalse(listRemoteDirectory(sftp, String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/(%s)/samples", createdExpPermId))
                            .stream().anyMatch( entry -> entry.startsWith("CREATED_SMP") ));

                    sftp.mkdir("/spaces/CREATED_SPACE/samples/CREATED_SMP_b_1");
                    sftp.mkdir("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/samples/CREATED_SMP_b_2");
                    sftp.mkdir(String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/(%s)/samples/CREATED_SMP_b_3", createdExpPermId));

                    assertTrue(listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/samples")
                            .stream().anyMatch( entry -> entry.startsWith("CREATED_SMP_b_1") ));
                    assertTrue(listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/projects/CREATED_PROJECT/samples")
                            .stream().anyMatch( entry -> entry.startsWith("CREATED_SMP_b_2") ));
                    assertTrue(listRemoteDirectory(sftp, String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/(%s)/samples", createdExpPermId))
                            .stream().anyMatch( entry -> entry.startsWith("CREATED_SMP_b_3") ));

                    String createdSmp1PermId = listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/samples")
                            .stream().filter( entry -> entry.startsWith("CREATED_SMP_b_1") ).findFirst().get()
                            .split("\\(")[1].split("\\)")[0].trim();
                    String createdSmp2PermId = listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/projects/CREATED_PROJECT/samples")
                            .stream().filter( entry -> entry.startsWith("CREATED_SMP_b_2") ).findFirst().get()
                            .split("\\(")[1].split("\\)")[0].trim();
                    String createdSmp3PermId = listRemoteDirectory(sftp, String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/(%s)/samples", createdExpPermId))
                            .stream().filter( entry -> entry.startsWith("CREATED_SMP_b_3") ).findFirst().get()
                            .split("\\(")[1].split("\\)")[0].trim();

                    sftp.rename(String.format("/spaces/CREATED_SPACE/samples/(%s)", createdSmp1PermId),
                            String.format("/spaces/CREATED_SPACE/samples/renamed CREATED_SMP_b_1 (%s)", createdSmp1PermId));
                    sftp.rename(String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/samples/(%s)", createdSmp2PermId),
                            String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/samples/renamed CREATED_SMP_b_2 (%s)", createdSmp2PermId));
                    sftp.rename(String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/(%s)/samples/(%s)", createdExpPermId, createdSmp3PermId),
                            String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/(%s)/samples/renamed CREATED_SMP_b_3 (%s)", createdExpPermId, createdSmp3PermId));

                    assertFalse(listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/samples")
                            .stream().anyMatch( entry -> entry.startsWith("CREATED_SMP_b_1") ));
                    assertFalse(listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/projects/CREATED_PROJECT/samples")
                            .stream().anyMatch( entry -> entry.startsWith("CREATED_SMP_b_2") ));
                    assertFalse(listRemoteDirectory(sftp, String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/(%s)/samples", createdExpPermId))
                            .stream().anyMatch( entry -> entry.startsWith("CREATED_SMP_b_3") ));

                    assertTrue(listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/samples")
                            .stream().anyMatch( entry -> entry.startsWith("renamed CREATED_SMP_b_1") ));
                    assertTrue(listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/projects/CREATED_PROJECT/samples")
                            .stream().anyMatch( entry -> entry.startsWith("renamed CREATED_SMP_b_2") ));
                    assertTrue(listRemoteDirectory(sftp, String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/(%s)/samples", createdExpPermId))
                            .stream().anyMatch( entry -> entry.startsWith("renamed CREATED_SMP_b_3") ));


                    // Create and delete folders (folder-type samples)
                    assertFalse(listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/folders")
                            .stream().anyMatch( entry -> entry.startsWith("CREATED_FLD") ));
                    assertFalse(listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/projects/CREATED_PROJECT/folders")
                            .stream().anyMatch( entry -> entry.startsWith("CREATED_FLD") ));
                    assertFalse(listRemoteDirectory(sftp, String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/(%s)/folders", createdExpPermId))
                            .stream().anyMatch( entry -> entry.startsWith("CREATED_FLD") ));

                    sftp.mkdir("/spaces/CREATED_SPACE/folders/CREATED_FLD_b_1");
                    sftp.mkdir("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/folders/CREATED_FLD_b_2");
                    sftp.mkdir(String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/(%s)/folders/CREATED_FLD_b_3", createdExpPermId));

                    assertTrue(listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/folders")
                            .stream().anyMatch( entry -> entry.startsWith("CREATED_FLD_b_1") ));
                    assertTrue(listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/projects/CREATED_PROJECT/folders")
                            .stream().anyMatch( entry -> entry.startsWith("CREATED_FLD_b_2") ));
                    assertTrue(listRemoteDirectory(sftp, String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/(%s)/folders", createdExpPermId))
                            .stream().anyMatch( entry -> entry.startsWith("CREATED_FLD_b_3") ));

                    String createdFld1PermId = listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/folders")
                            .stream().filter( entry -> entry.startsWith("CREATED_FLD_b_1") ).findFirst().get()
                            .split("\\(")[1].split("\\)")[0].trim();
                    String createdFld2PermId = listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/projects/CREATED_PROJECT/folders")
                            .stream().filter( entry -> entry.startsWith("CREATED_FLD_b_2") ).findFirst().get()
                            .split("\\(")[1].split("\\)")[0].trim();
                    String createdFld3PermId = listRemoteDirectory(sftp, String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/(%s)/folders", createdExpPermId))
                            .stream().filter( entry -> entry.startsWith("CREATED_FLD_b_3") ).findFirst().get()
                            .split("\\(")[1].split("\\)")[0].trim();

                    sftp.rename(String.format("/spaces/CREATED_SPACE/folders/(%s)", createdFld1PermId),
                            String.format("/spaces/CREATED_SPACE/folders/renamed CREATED_FLD_b_1 (%s)", createdFld1PermId));
                    sftp.rename(String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/folders/(%s)", createdFld2PermId),
                            String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/folders/renamed CREATED_FLD_b_2 (%s)", createdFld2PermId));
                    sftp.rename(String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/(%s)/folders/(%s)", createdExpPermId, createdFld3PermId),
                            String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/(%s)/folders/renamed CREATED_FLD_b_3 (%s)", createdExpPermId, createdFld3PermId));

                    assertFalse(listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/folders")
                            .stream().anyMatch( entry -> entry.startsWith("CREATED_FLD_b_1") ));
                    assertFalse(listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/projects/CREATED_PROJECT/folders")
                            .stream().anyMatch( entry -> entry.startsWith("CREATED_FLD_b_2") ));
                    assertFalse(listRemoteDirectory(sftp, String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/(%s)/folders", createdExpPermId))
                            .stream().anyMatch( entry -> entry.startsWith("CREATED_FLD_b_3") ));

                    assertTrue(listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/folders")
                            .stream().anyMatch( entry -> entry.startsWith("renamed CREATED_FLD_b_1") ));
                    assertTrue(listRemoteDirectory(sftp, "/spaces/CREATED_SPACE/projects/CREATED_PROJECT/folders")
                            .stream().anyMatch( entry -> entry.startsWith("renamed CREATED_FLD_b_2") ));
                    assertTrue(listRemoteDirectory(sftp, String.format("/spaces/CREATED_SPACE/projects/CREATED_PROJECT/experiments/(%s)/folders", createdExpPermId))
                            .stream().anyMatch( entry -> entry.startsWith("renamed CREATED_FLD_b_3") ));
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

            int index = 0;
            while (index < maximum) {
                int chunkSize = Integer.min(
                        16500,
                        (int) Long.min(Integer.MAX_VALUE, maximum - index)
                );
                byte[] chunk = new byte[chunkSize];
                int readBytes = sftp.read(fileHandle, offset + index, chunk);

                if (readBytes > -1) {
                    System.arraycopy(chunk, 0, bytes, index, readBytes);
                    index = index + readBytes;
                } else {
                    break;
                }
            }
            return Arrays.copyOfRange(bytes, 0, index);
        }
    }

    private static void uploadTestFile(SftpClient sftp, String filePath, long offset, byte[] content) throws Exception
    {
        try (SftpClient.CloseableHandle fileHandle = sftp.open(filePath, SftpClient.OpenMode.Write, SftpClient.OpenMode.Create))
        {
            int index = 0;
            while (index < offset + content.length) {
                int chunkSize = Integer.min(
                        16500,
                        (int) Long.min(Integer.MAX_VALUE, offset + content.length - index)
                );

                sftp.write(fileHandle, offset + index, Arrays.copyOfRange(content, index, index + chunkSize));
                index = index + chunkSize;
            }
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
