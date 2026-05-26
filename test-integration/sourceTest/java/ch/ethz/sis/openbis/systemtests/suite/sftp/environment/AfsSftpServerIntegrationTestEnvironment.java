package ch.ethz.sis.openbis.systemtests.suite.sftp.environment;

import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameter;
import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.create.DataStoreCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.ProjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.roleassignment.Role;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import ch.ethz.sis.openbis.systemtests.environment.IntegrationTestEnvironment;
import ch.ethz.sis.openbis.systemtests.environment.IntegrationTestFacade;
import lombok.NonNull;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public final class AfsSftpServerIntegrationTestEnvironment
{
    public static final String DEFAULT_SPACE = "DEFAULT";

    public static final String TEST_SPACE_1 = "S1";
    public static final String TEST_SPACE_2 = "S2";

    public static final String TEST_PROJECT_1 = "P1";
    public static final String TEST_PROJECT_2 = "P2";

    public static final String TEST_EXPERIMENT_1 = "E1";
    public static final String TEST_EXPERIMENT_2 = "E2";

    public static final String TEST_SAMPLE_1 = "X1";
    public static final String TEST_SAMPLE_2 = "X2";

    public static final String TEST_FOLDER_1 = "F1";
    public static final String TEST_FOLDER_2 = "F2";

    public static final String TEST_SUBSAMPLE_1 = "Z1";
    public static final String TEST_SUBSAMPLE_2 = "Z2";

    public static final String TEST_DATASET_1 = "D1";
    public static final String TEST_DATASET_2 = "D2";

    public static final String INSTANCE_ADMIN = "admin";

    public static final String DEFAULT_SPACE_ADMIN = "default_space_admin";

    public static final String TEST_SPACE_ADMIN = "test_space_admin";

    public static final String TEST_SPACE_OBSERVER = "test_space_observer";

    private static final ConcurrentHashMap<String, String> permIdsByName = new ConcurrentHashMap<>();

    public static final String PASSWORD = "password";

    public static final String DSS_CODE = "STANDARD";
    public static final String DSS_DOWNLOAD_URL = "http://localhost";
    public static final String DSS_REMOTE_URL = "http://localhost";

    public static IntegrationTestEnvironment environment;

    public static void start()
    {
        if (environment == null)
        {
            environment = new IntegrationTestEnvironment();
            environment.createApplicationServer();
            environment.createAfsServer(IntegrationTestEnvironment.loadProperties(Path.of("etc/suite/sftp/afs/service.properties")));
            environment.createAfsSftpServer();
            environment.start();
            createTestData();
        }
    }

    public static void stop()
    {
        if (environment != null)
        {
            environment.stop();
            environment = null;
        }
    }

    private static void createTestData() {
        permIdsByName.clear();

        OpenBIS openBIS = environment.createOpenBIS();
        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        DataStoreCreation dataStoreCreation = new DataStoreCreation();
        dataStoreCreation.setCode(DSS_CODE);
        dataStoreCreation.setStorageUuid(
                environment.getAfsServer().getServiceProperties().getProperty(
                        AtomicFileSystemServerParameter.storageUuid.name()
                )
        );
        dataStoreCreation.setDownloadUrl(DSS_DOWNLOAD_URL);
        dataStoreCreation.setRemoteUrl(DSS_REMOTE_URL);
        openBIS.createDataStores(Collections.singletonList(dataStoreCreation));

        IntegrationTestFacade facade = new IntegrationTestFacade(environment);
        facade.createUser(openBIS, DEFAULT_SPACE_ADMIN, DEFAULT_SPACE, Role.ADMIN);

        facade.assignPropertyToSampleType(openBIS, "UNKNOWN", "NAME");
        facade.assignPropertyToExperimentType(openBIS, "UNKNOWN", "NAME");
        facade.assignPropertyToDatasetType(openBIS, "UNKNOWN", "NAME");

        for (String space : List.of(TEST_SPACE_1, TEST_SPACE_2)) {
            facade.createSpace(openBIS, space);
            facade.createUser(openBIS, TEST_SPACE_ADMIN + "_for_" + space, space, Role.ADMIN);
            facade.createUser(openBIS, TEST_SPACE_OBSERVER + "_for_" + space, space, Role.OBSERVER);

            for (String spaceSample : List.of(TEST_SAMPLE_1, TEST_SAMPLE_2)) {
                Sample sample = facade.createSampleWithTypeAndNameAndParent(
                        openBIS, new SpacePermId(space), null,
                        spaceSample, "UNKNOWN", spaceSample
                );
                putSampleOrDatasetPermIdByName(space, spaceSample, sample.getPermId().getPermId());
            }
            for (String spaceFolder : List.of(TEST_FOLDER_1, TEST_FOLDER_2)) {
                Sample spaceFolderObj = facade.createSampleWithTypeAndNameAndParent(
                        openBIS, new SpacePermId(space), null,
                        spaceFolder, "FOLDER", spaceFolder
                );
                putSampleOrDatasetPermIdByName(space, spaceFolder, spaceFolderObj.getPermId().getPermId());
                for (String subsample : List.of(TEST_SUBSAMPLE_1, TEST_SUBSAMPLE_2)) {
                    Sample subsampleObj = facade.createSampleWithTypeAndNameAndParent(
                            openBIS, new SpacePermId(space), spaceFolderObj.getPermId(),
                            spaceFolder + "_" + subsample,
                            "UNkNOWN",
                            spaceFolder + "_" + subsample
                    );
                    putSampleOrDatasetPermIdByName(
                            space,
                            spaceFolder + "_" + subsample,
                            subsampleObj.getPermId().getPermId()
                    );
                }
            }

            for (String project : List.of(TEST_PROJECT_1, TEST_PROJECT_2)) {
                Project projectObj = facade.createProject(openBIS, new SpacePermId(space), project);
                for (String projectSample : List.of(TEST_SAMPLE_1, TEST_SAMPLE_2)) {
                    Sample projectSampleObj = facade.createSampleWithTypeAndNameAndParent(
                            openBIS, projectObj.getPermId(), null,
                            project + "_" + projectSample,
                            "UNKNOWN", project + "_" + projectSample
                    );
                    putSampleOrDatasetPermIdByName(
                            space,
                            project + "_" + projectSample,
                            projectSampleObj.getPermId().getPermId()
                    );
                }
                for (String projectFolder : List.of(TEST_FOLDER_1, TEST_FOLDER_2)) {
                    Sample projectFolderObj = facade.createSampleWithTypeAndNameAndParent(
                            openBIS, projectObj.getPermId(), null,
                            project + "_" + projectFolder,
                            "FOLDER", project + "_" + projectFolder
                    );
                    putSampleOrDatasetPermIdByName(
                            space,
                            project + "_" + projectFolder,
                            projectFolderObj.getPermId().getPermId()
                    );
                    for (String projectSubsample : List.of(TEST_SUBSAMPLE_1, TEST_SUBSAMPLE_2)) {
                        Sample projectSubsampleObj = facade.createSampleWithTypeAndNameAndParent(
                                openBIS, projectObj.getPermId(), projectFolderObj.getPermId(),
                                project + "_" + projectFolder + "_" + projectSubsample,
                                "UNkNOWN",
                                project + "_" + projectFolder + "_" + projectSubsample
                        );
                        putSampleOrDatasetPermIdByName(
                                space,
                                project + "_" + projectFolder + "_" + projectSubsample,
                                projectSubsampleObj.getPermId().getPermId()
                        );
                    }
                }

                for (String experiment : List.of(TEST_EXPERIMENT_1, TEST_EXPERIMENT_2)) {
                    Experiment experimentObj = facade.createExperimentWithName(
                            openBIS, new ProjectIdentifier(space, project),
                            experiment, experiment
                    );
                    putExperimentPermIdByName(space, project, experiment, experimentObj.getPermId().getPermId());

                    for (String experimentSample : List.of(TEST_SAMPLE_1, TEST_SAMPLE_2)) {
                        Sample sampleObj = facade.createSampleWithTypeAndNameAndParent(
                                openBIS, experimentObj.getPermId(), null,
                                project + "_" + experiment + "_" + experimentSample,
                                "UNKNOWN", project + "_" + experiment + "_" + experimentSample
                        );
                        putSampleOrDatasetPermIdByName(
                                space,
                                project + "_" + experiment + "_" + experimentSample,
                                sampleObj.getPermId().getPermId()
                        );

                        for (String experimentSampleDataset : List.of(TEST_DATASET_1, TEST_DATASET_2)) {
                            try {
                                DataSet dataSet = facade.createDataSet(openBIS, DSS_CODE,
                                        null, sampleObj.getPermId(),
                                        space + "_" + project + "_" + experiment + "_" + experimentSample + "_" + experimentSampleDataset,
                                        "file_" + space + "_" + project + "_" + experiment + "_" + experimentSample + "_" + experimentSampleDataset,
                                        ("data:" + project + "_" + experiment + "_" + experimentSample + "_" + experimentSampleDataset).getBytes(StandardCharsets.UTF_8),
                                        project + "_" + experiment + "_" + experimentSample + "_" + experimentSampleDataset
                                );
                                putSampleOrDatasetPermIdByName(
                                        space,
                                        project + "_" + experiment + "_" + experimentSample + "_" + experimentSampleDataset,
                                        dataSet.getPermId().getPermId()
                                );
                            } catch (Exception e) { throw new RuntimeException(e); }

                        }
                    }
                    for (String experimentFolder : List.of(TEST_FOLDER_1, TEST_FOLDER_2)) {
                        Sample projectFolderObj = facade.createSampleWithTypeAndNameAndParent(
                                openBIS, experimentObj.getPermId(), null,
                                project + "_" + experiment + "_" + experimentFolder,
                                "FOLDER", project + "_" + experiment + "_" + experimentFolder
                        );
                        putSampleOrDatasetPermIdByName(
                                space,
                                project + "_" + experiment + "_" + experimentFolder,
                                projectFolderObj.getPermId().getPermId()
                        );

                        for (String experimentSubsample : List.of(TEST_SUBSAMPLE_1, TEST_SUBSAMPLE_2)) {
                            Sample experimentSubsampleObj = facade.createSampleWithTypeAndNameAndParent(
                                    openBIS, experimentObj.getPermId(), projectFolderObj.getPermId(),
                                    project + "_" + experiment + "_" + experimentFolder + "_" + experimentSubsample,
                                    "UNkNOWN",
                                    project + "_" + experiment + "_" + experimentFolder + "_" + experimentSubsample
                            );
                            putSampleOrDatasetPermIdByName(
                                    space,
                                    project + "_" + experiment + "_" + experimentFolder + "_" + experimentSubsample,
                                    experimentSubsampleObj.getPermId().getPermId()
                            );
                        }
                    }
                }
            }
        }
    }

    private static void putSampleOrDatasetPermIdByName(@NonNull String testSpace, @NonNull String testName, @NonNull String permId) {
        permIdsByName.put(testSpace + ":" + testName, permId);
    }

    public static String getSampleOrDatasetPermIdByName(@NonNull String testSpace, @NonNull String testName) {
        return permIdsByName.get(testSpace + ":" + testName);
    }

    private static void putExperimentPermIdByName(@NonNull String testSpace, @NonNull String testProject, @NonNull String testName, @NonNull String permId) {
        permIdsByName.put(testSpace + ":" + testProject + ":" + testName, permId);
    }

    public static String getExperimentPermIdByName(@NonNull String testSpace, @NonNull String testProject, @NonNull String testName) {
        return permIdsByName.get(testSpace + ":" + testProject + ":" + testName);
    }
}
