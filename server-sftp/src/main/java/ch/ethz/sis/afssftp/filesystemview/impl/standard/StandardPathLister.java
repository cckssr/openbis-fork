package ch.ethz.sis.afssftp.filesystemview.impl.standard;

import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afssftp.authentication.OpenBISUser;
import ch.ethz.sis.afssftp.filesystemview.FtpPathLister;
import ch.ethz.sis.afssftp.filesystemview.OpenBISSftpFileAttributes;
import ch.ethz.sis.afssftp.filesystemview.OpenBISSftpNode;
import ch.ethz.sis.afssftp.filesystemview.OpenBISSftpNodeChain;
import ch.ethz.sis.afssftp.util.OpenBISListUtil;
import lombok.NonNull;

import java.util.*;

public class StandardPathLister implements FtpPathLister {
    private final OpenBISListUtil listUtil;

    public StandardPathLister(OpenBISUser user) {
        this.listUtil = new OpenBISListUtil(user);
    }

    @Override
    public @NonNull List<@NonNull OpenBISSftpNodeChain> list(@NonNull OpenBISSftpNodeChain directory) {
        OpenBISSftpNode lastNode = directory.getLast().orElse(null);
        if ( lastNode != null ) {
            return switch (lastNode.getType()) {
                case ROOT -> listRoot(null, directory);
                case SPACE -> listSpace(lastNode, null, directory);
                case SAMPLE -> listSample(lastNode, null, directory);
                case FOLDER -> listFolder(lastNode, null, directory);
                case DATA_SET -> listDataSet(lastNode, null, directory);
                case PROJECT -> listProject(lastNode, null, directory);
                case EXPERIMENT -> listExperiment(lastNode, null, directory);
                case AFS_FILE -> listFilesInAfsFileNode(lastNode, directory);
                case SUBLEVEL -> {
                    if (directory.size() > 1) {
                        OpenBISSftpNode secondLastNode = directory.nodes().get(directory.nodes().size() - 2);
                        yield  switch (secondLastNode.getType()) {
                            case ROOT -> listRoot(lastNode.getIdentifier().orElseThrow(), directory);
                            case SPACE -> listSpace(secondLastNode, lastNode.getIdentifier().orElseThrow(), directory);
                            case SAMPLE -> listSample(secondLastNode, lastNode.getIdentifier().orElseThrow(), directory);
                            case FOLDER -> listFolder(secondLastNode, lastNode.getIdentifier().orElseThrow(), directory);
                            case DATA_SET -> listDataSet(secondLastNode, lastNode.getIdentifier().orElseThrow(), directory);
                            case PROJECT -> listProject(secondLastNode, lastNode.getIdentifier().orElseThrow(), directory);
                            case EXPERIMENT -> listExperiment(secondLastNode, lastNode.getIdentifier().orElseThrow(), directory);
                            case SUBLEVEL, AFS_FILE -> throw new IllegalArgumentException("Malformed node-chain");
                        };
                    } else {
                        throw new IllegalArgumentException("Malformed node-chain");
                    }
                }
            };
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public OpenBISSftpFileAttributes readAttributes(@NonNull OpenBISSftpNodeChain nodeChain) {
        OpenBISSftpNode lastNode = nodeChain.getLast().orElse(null);
        if ( lastNode != null && lastNode.getType() == OpenBISSftpNode.Type.AFS_FILE ) {
            OpenBISSftpNode afsEntityNode = validateAndGetAfsEntityNodeFromAfsFileChain(nodeChain);

            String spaceCode = nodeChain.lookUpSpaceCode();
            String projectCode = nodeChain.lookUpProjectCode();
            String afsEntityPermId = listUtil.getAfsEntityPermId(afsEntityNode, spaceCode, projectCode);

            return listUtil.getDefaultAfsFileAttributes(
                        afsEntityPermId, lastNode.getJoinedAfsFilePath()
            ).orElse(null);

        } else {
            return OpenBISListUtil.getDefaultAbstractDirectoryAttributes();
        }
    }

    @NonNull List<@NonNull OpenBISSftpNodeChain> listRoot(String sublevel, @NonNull OpenBISSftpNodeChain fullChain) {
        if ( sublevel == null ) {
            return List.of(
                    OpenBISSftpNodeChain.concat(
                            fullChain,
                            OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.SPACE_TYPE_LABEL)
                    )
            );
        } else if ( sublevel.equals( StandardPathTranslator.SPACE_TYPE_LABEL )) {
            List<OpenBISSftpNodeChain> listedSpaces = new ArrayList<>();
            listUtil.getSpaces().iterator().forEachRemaining(
                space -> {
                    listedSpaces.add(OpenBISSftpNodeChain.concat(fullChain,
                            OpenBISSftpNodeChain.fromSpace(space))
                    );
                }
            );

            return listedSpaces;
        } else {
            return Collections.emptyList();
        }
    }

    @NonNull List<@NonNull OpenBISSftpNodeChain> listSpace(@NonNull OpenBISSftpNode spaceNode, String sublevel, @NonNull OpenBISSftpNodeChain fullChain) {
        if ( sublevel == null ) {
            return List.of(
                    OpenBISSftpNodeChain.concat(
                            fullChain,
                            OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.FOLDER_TYPE_LABEL)
                    ),
                    OpenBISSftpNodeChain.concat(
                            fullChain,
                            OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.SAMPLE_TYPE_LABEL)
                    ),
                    OpenBISSftpNodeChain.concat(
                            fullChain,
                            OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.PROJECT_TYPE_LABEL)
                    )
            );
        } else if ( sublevel.equals(StandardPathTranslator.FOLDER_TYPE_LABEL) ) {
            return listSamplesOrFoldersInSpace(spaceNode, fullChain, true);
        } else if ( sublevel.equals(StandardPathTranslator.SAMPLE_TYPE_LABEL) ) {
            return listSamplesOrFoldersInSpace(spaceNode, fullChain, false);
        } else if ( sublevel.equals(StandardPathTranslator.PROJECT_TYPE_LABEL) ) {
            return listProjectsInSpace(spaceNode, fullChain);
        } else {
            return Collections.emptyList();
        }
    }

    @NonNull List<@NonNull OpenBISSftpNodeChain> listProjectsInSpace(@NonNull OpenBISSftpNode spaceNode, @NonNull OpenBISSftpNodeChain fullChain) {
        List<OpenBISSftpNodeChain> listedProjects = new ArrayList<>();
        listUtil.getProjects(spaceNode.getIdentifier().orElseThrow()).iterator().forEachRemaining(
                project -> {
                    listedProjects.add(OpenBISSftpNodeChain.concat(fullChain,
                            OpenBISSftpNodeChain.fromProject(project))
                    );
                }
        );

        return listedProjects;
    }

    @NonNull List<@NonNull OpenBISSftpNodeChain> listSamplesOrFoldersInSpace(@NonNull OpenBISSftpNode spaceNode, @NonNull OpenBISSftpNodeChain fullChain, boolean folders) {
        List<OpenBISSftpNodeChain> listedSamples = new ArrayList<>();
        listUtil.getSpaceSamples(spaceNode.getIdentifier().orElseThrow()).iterator().forEachRemaining(
                sample -> {
                    if (folders == OpenBISListUtil.isOfTypeFolder(sample)) {
                        listedSamples.add(OpenBISSftpNodeChain.concat(fullChain,
                                OpenBISSftpNodeChain.fromSample(sample))
                        );
                    }
                }
        );

        return listedSamples;
    }

    @NonNull List<@NonNull OpenBISSftpNodeChain> listSamplesOrFoldersInProject(@NonNull OpenBISSftpNode projectNode, @NonNull OpenBISSftpNodeChain fullChain, boolean folders) {
        String spaceCode = fullChain.lookUpSpaceCode();

        List<OpenBISSftpNodeChain> listedSamples = new ArrayList<>();
        listUtil.getProjectSamples(
                Objects.requireNonNull(spaceCode), projectNode.getIdentifier().orElseThrow()
        ).iterator().forEachRemaining(
            sample -> {
                if (folders == OpenBISListUtil.isOfTypeFolder(sample)) {
                    listedSamples.add(OpenBISSftpNodeChain.concat(fullChain,
                            OpenBISSftpNodeChain.fromSample(sample))
                    );
                }
            }
        );

        return listedSamples;
    }

    @NonNull List<@NonNull OpenBISSftpNodeChain> listSample(@NonNull OpenBISSftpNode sampleNode, String sublevel, @NonNull OpenBISSftpNodeChain fullChain) {
        if ( sublevel == null ) {
            return List.of(
                    OpenBISSftpNodeChain.concat(
                            fullChain,
                            OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.FOLDER_TYPE_LABEL)
                    ),
                    OpenBISSftpNodeChain.concat(
                            fullChain,
                            OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.SAMPLE_TYPE_LABEL)
                    ),
                    OpenBISSftpNodeChain.concat(
                            fullChain,
                            OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.DATA_SET_TYPE_LABEL)
                    ),
                    OpenBISSftpNodeChain.concat(
                            fullChain,
                            OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.FILE_TYPE_LABEL)
                    )
            );
        } else if ( sublevel.equals(StandardPathTranslator.FOLDER_TYPE_LABEL) ) {
            return listSamplesOrFoldersInSample(sampleNode, fullChain, true);
        } else if ( sublevel.equals(StandardPathTranslator.SAMPLE_TYPE_LABEL) ) {
            return listSamplesOrFoldersInSample(sampleNode, fullChain, false);
        } else if ( sublevel.equals(StandardPathTranslator.DATA_SET_TYPE_LABEL) ) {
            return listDataSetsInSample(sampleNode, fullChain);
        }  else if ( sublevel.equals(StandardPathTranslator.FILE_TYPE_LABEL) ) {
            return listFilesInSampleOrFolder(sampleNode, fullChain);
        } else {
            return Collections.emptyList();
        }
    }

    @NonNull List<@NonNull OpenBISSftpNodeChain> listFolder(@NonNull OpenBISSftpNode folderNode, String sublevel, @NonNull OpenBISSftpNodeChain fullChain) {
        if ( sublevel == null ) {
            return List.of(
                    OpenBISSftpNodeChain.concat(
                            fullChain,
                            OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.FOLDER_TYPE_LABEL)
                    ),
                    OpenBISSftpNodeChain.concat(
                            fullChain,
                            OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.SAMPLE_TYPE_LABEL)
                    ),
                    OpenBISSftpNodeChain.concat(
                            fullChain,
                            OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.FILE_TYPE_LABEL)
                    )
            );
        } else if ( sublevel.equals(StandardPathTranslator.FOLDER_TYPE_LABEL) ) {
            return listSamplesOrFoldersInSample(folderNode, fullChain, true);
        } else if ( sublevel.equals(StandardPathTranslator.SAMPLE_TYPE_LABEL) ) {
            return listSamplesOrFoldersInSample(folderNode, fullChain, false);
        } else if ( sublevel.equals(StandardPathTranslator.FILE_TYPE_LABEL) ) {
            return listFilesInSampleOrFolder(folderNode, fullChain);
        } else {
            return Collections.emptyList();
        }
    }

    @NonNull List<@NonNull OpenBISSftpNodeChain> listSamplesOrFoldersInSample(@NonNull OpenBISSftpNode sampleNode, @NonNull OpenBISSftpNodeChain fullChain, boolean folders) {
        String spaceCode = fullChain.lookUpSpaceCode();
        String projectCode = fullChain.lookUpProjectCode();

        List<OpenBISSftpNodeChain> listedSamples = new ArrayList<>();
        listUtil.getSampleChildren(
                spaceCode, projectCode, sampleNode.getIdentifier().orElseThrow()
        ).iterator().forEachRemaining(
            sample -> {
                if (folders == OpenBISListUtil.isOfTypeFolder(sample)) {
                    listedSamples.add(OpenBISSftpNodeChain.concat(fullChain,
                            OpenBISSftpNodeChain.fromSample(sample))
                    );
                }
            }
        );

        return listedSamples;
    }

    @NonNull List<@NonNull OpenBISSftpNodeChain> listDataSetsInSample(@NonNull OpenBISSftpNode sampleNode, @NonNull OpenBISSftpNodeChain fullChain) {
        String spaceCode = fullChain.lookUpSpaceCode();
        String projectCode = fullChain.lookUpProjectCode();

        List<OpenBISSftpNodeChain> listedDatasets = new ArrayList<>();
        listUtil.getSampleDatasets(
                spaceCode, projectCode, sampleNode.getIdentifier().orElseThrow()
        ).iterator().forEachRemaining(
                dataSet -> {
                    listedDatasets.add(OpenBISSftpNodeChain.concat(fullChain,
                            OpenBISSftpNodeChain.fromDataSet(dataSet))
                    );
                }
        );

        return listedDatasets;
    }

    @NonNull List<@NonNull OpenBISSftpNodeChain> listFilesInSampleOrFolder(@NonNull OpenBISSftpNode sampleNode, @NonNull OpenBISSftpNodeChain fullChain) {
        String spaceCode = fullChain.lookUpSpaceCode();
        String projectCode = fullChain.lookUpProjectCode();
        String samplePermId = listUtil.getAfsEntityPermId(sampleNode, spaceCode, projectCode);

        if (samplePermId != null) {
            List<OpenBISSftpNodeChain> listedAfsFiles = new ArrayList<>();

            File[] files = listUtil.listAfsFiles(samplePermId, "/");

            Arrays.asList(files).iterator().forEachRemaining(
                    file -> {
                        listedAfsFiles.add(OpenBISSftpNodeChain.concat(fullChain,
                                OpenBISSftpNodeChain.fromAfsFilePath(Collections.singletonList(file.getName())))
                        );
                    }
            );

            return listedAfsFiles;
        } else {
            return Collections.emptyList();
        }
    }

    @NonNull List<@NonNull OpenBISSftpNodeChain> listFilesInDataSet(@NonNull OpenBISSftpNode dataSetNode, @NonNull OpenBISSftpNodeChain fullChain) {
        String spaceCode = fullChain.lookUpSpaceCode();
        String projectCode = fullChain.lookUpProjectCode();
        String dataSetPermId = listUtil.getAfsEntityPermId(dataSetNode, spaceCode, projectCode);

        if (dataSetPermId != null) {
            List<OpenBISSftpNodeChain> listedAfsFiles = new ArrayList<>();

            File[] files = listUtil.listAfsFiles(dataSetPermId, "/");

            Arrays.asList(files).iterator().forEachRemaining(
                    file -> {
                        listedAfsFiles.add(OpenBISSftpNodeChain.concat(fullChain,
                                        OpenBISSftpNodeChain.fromAfsFilePath(Collections.singletonList(file.getName())))
                        );
                    }
            );

            return listedAfsFiles;
        } else {
            return Collections.emptyList();
        }
    }

    @NonNull List<@NonNull OpenBISSftpNodeChain> listFilesInExperiment(@NonNull OpenBISSftpNode experimentNode, @NonNull OpenBISSftpNodeChain fullChain) {
        String spaceCode = fullChain.lookUpSpaceCode();
        String projectCode = fullChain.lookUpProjectCode();
        String experimentPermId = listUtil.getAfsEntityPermId(experimentNode, spaceCode, projectCode);

        if (experimentPermId != null) {
            List<OpenBISSftpNodeChain> listedAfsFiles = new ArrayList<>();

            File[] files = listUtil.listAfsFiles(experimentPermId, "/");

            Arrays.asList(files).iterator().forEachRemaining(
                    file -> {
                        listedAfsFiles.add(OpenBISSftpNodeChain.concat(fullChain,
                                        OpenBISSftpNodeChain.fromAfsFilePath(Collections.singletonList(file.getName())))
                        );
                    }
            );

            return listedAfsFiles;
        } else {
            return Collections.emptyList();
        }
    }

    @NonNull List<@NonNull OpenBISSftpNodeChain> listDataSet(@NonNull OpenBISSftpNode dataSetNode, String sublevel, @NonNull OpenBISSftpNodeChain fullChain) {
        if ( sublevel == null ) {
            return List.of(
                    OpenBISSftpNodeChain.concat(
                            fullChain,
                            OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.FILE_TYPE_LABEL)
                    )
            );
        } else if ( sublevel.equals(StandardPathTranslator.FILE_TYPE_LABEL) ) {
            return listFilesInDataSet(dataSetNode, fullChain);
        } else {
            return Collections.emptyList();
        }
    }

    @NonNull List<@NonNull OpenBISSftpNodeChain> listProject(@NonNull OpenBISSftpNode projectNode, String sublevel, @NonNull OpenBISSftpNodeChain fullChain) {
        if ( sublevel == null ) {
            return List.of(
                    OpenBISSftpNodeChain.concat(
                            fullChain,
                            OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.FOLDER_TYPE_LABEL)
                    ),
                    OpenBISSftpNodeChain.concat(
                            fullChain,
                            OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.SAMPLE_TYPE_LABEL)
                    ),
                    OpenBISSftpNodeChain.concat(
                            fullChain,
                            OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.EXPERIMENT_TYPE_LABEL)
                    )
            );
        } else if ( sublevel.equals(StandardPathTranslator.FOLDER_TYPE_LABEL) ) {
            return listSamplesOrFoldersInProject(projectNode, fullChain, true);
        } else if ( sublevel.equals(StandardPathTranslator.SAMPLE_TYPE_LABEL) ) {
            return listSamplesOrFoldersInProject(projectNode, fullChain, false);
        } else if ( sublevel.equals(StandardPathTranslator.EXPERIMENT_TYPE_LABEL) ) {
            return listExperimentsInProject(projectNode, fullChain);
        } else {
            return Collections.emptyList();
        }
    }

    @NonNull List<@NonNull OpenBISSftpNodeChain> listExperimentsInProject(@NonNull OpenBISSftpNode projectNode, @NonNull OpenBISSftpNodeChain fullChain) {
        String spaceCode = fullChain.lookUpSpaceCode();

        List<OpenBISSftpNodeChain> listedExperiments = new ArrayList<>();
        listUtil.getExperiments(
                Objects.requireNonNull(spaceCode), projectNode.getIdentifier().orElseThrow()
        ).iterator().forEachRemaining(
                experiment -> {
                    listedExperiments.add(OpenBISSftpNodeChain.concat(fullChain,
                            OpenBISSftpNodeChain.fromExperiment(experiment))
                    );
                }
        );

        return listedExperiments;
    }

    @NonNull List<@NonNull OpenBISSftpNodeChain> listExperiment(@NonNull OpenBISSftpNode experimentNode, String sublevel, @NonNull OpenBISSftpNodeChain fullChain) {
        if ( sublevel == null ) {
            return List.of(
                    OpenBISSftpNodeChain.concat(
                            fullChain,
                            OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.FOLDER_TYPE_LABEL)
                    ),
                    OpenBISSftpNodeChain.concat(
                            fullChain,
                            OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.SAMPLE_TYPE_LABEL)
                    ),
                    OpenBISSftpNodeChain.concat(
                            fullChain,
                            OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.FILE_TYPE_LABEL)
                    )
            );
        } else if ( sublevel.equals(StandardPathTranslator.FOLDER_TYPE_LABEL) ) {
            return listSamplesOrFoldersInExperiment(experimentNode, fullChain, true);
        } else if ( sublevel.equals(StandardPathTranslator.SAMPLE_TYPE_LABEL) ) {
            return listSamplesOrFoldersInExperiment(experimentNode, fullChain, false);
        } else if ( sublevel.equals(StandardPathTranslator.FILE_TYPE_LABEL) ) {
            return listFilesInExperiment(experimentNode, fullChain);
        } else {
            return Collections.emptyList();
        }
    }

    @NonNull List<@NonNull OpenBISSftpNodeChain> listSamplesOrFoldersInExperiment(@NonNull OpenBISSftpNode experimentNode, @NonNull OpenBISSftpNodeChain fullChain, boolean folders) {
        String spaceCode = fullChain.lookUpSpaceCode();
        String projectCode = fullChain.lookUpProjectCode();

        List<OpenBISSftpNodeChain> listedSamples = new ArrayList<>();
        listUtil.getExperimentSamples(
                Objects.requireNonNull(spaceCode), Objects.requireNonNull(projectCode), experimentNode.getIdentifier().orElseThrow()
        ).iterator().forEachRemaining(
                sample -> {
                    if (folders == OpenBISListUtil.isOfTypeFolder(sample)) {
                        listedSamples.add(OpenBISSftpNodeChain.concat(fullChain,
                                OpenBISSftpNodeChain.fromSample(sample))
                        );
                    }
                }
        );

        return listedSamples;
    }



    @NonNull List<@NonNull OpenBISSftpNodeChain> listFilesInAfsFileNode(@NonNull OpenBISSftpNode afsFileNode, @NonNull OpenBISSftpNodeChain fullChain) {
        OpenBISSftpNodeChain baseChain = new OpenBISSftpNodeChain(
                fullChain.nodes().subList(0, fullChain.nodes().size() - 1)
        );

        OpenBISSftpNode afsEntityNode = validateAndGetAfsEntityNodeFromAfsFileChain(fullChain);

        String spaceCode = fullChain.lookUpSpaceCode();
        String projectCode = fullChain.lookUpProjectCode();
        String afsEntityPermId = listUtil.getAfsEntityPermId(afsEntityNode, spaceCode, projectCode);

        if (afsEntityPermId != null) {
            List<OpenBISSftpNodeChain> listedAfsFiles = new ArrayList<>();

            File[] files = listUtil.listAfsFiles(afsEntityPermId, afsFileNode.getJoinedAfsFilePath());

            Arrays.asList(files).iterator().forEachRemaining(
                    file -> {
                        List<String> afsPathSegments = Arrays.stream(file.getPath().split("/")).filter(
                                segment -> !segment.isEmpty()
                        ).toList();
                        listedAfsFiles.add(
                            OpenBISSftpNodeChain.concat(
                                baseChain,
                                afsFileNode.toBuilder()
                                .afsFilePath(afsPathSegments)
                                .build()
                            )
                        );
                    }
            );

            return listedAfsFiles;
        } else {
            return Collections.emptyList();
        }
    }

    @NonNull OpenBISSftpNode validateAndGetAfsEntityNodeFromAfsFileChain(@NonNull OpenBISSftpNodeChain afsFileChain) {
        if (afsFileChain.size() > 2) {
            OpenBISSftpNode afsFileNode = afsFileChain.getLast().orElseThrow();
            OpenBISSftpNode afsTypeSublevelNode = afsFileChain.get(afsFileChain.size() - 2);
            OpenBISSftpNode afsEntityNode = afsFileChain.get(afsFileChain.size() - 3);
            if (afsFileNode.getType() == OpenBISSftpNode.Type.AFS_FILE &&
                afsTypeSublevelNode.getType() == OpenBISSftpNode.Type.SUBLEVEL &&
                afsTypeSublevelNode.getIdentifier().orElseThrow().equals(StandardPathTranslator.FILE_TYPE_LABEL) &&
                OpenBISListUtil.POSSIBLE_AFS_ENTITY_TYPES.contains(afsEntityNode.getType())
            ) {
                return afsEntityNode;
            } else {
                throw new IllegalArgumentException("Malformed AFS-file node-chain");
            }
        } else {
            throw new IllegalArgumentException("Malformed AFS-file node-chain");
        }
    }
}
