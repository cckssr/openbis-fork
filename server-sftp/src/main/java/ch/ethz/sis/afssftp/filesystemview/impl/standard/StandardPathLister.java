package ch.ethz.sis.afssftp.filesystemview.impl.standard;

import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afssftp.authentication.User;
import ch.ethz.sis.afssftp.filesystemview.*;
import ch.ethz.sis.afssftp.util.SftpListUtil;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.ProjectIdentifier;
import lombok.NonNull;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.*;

public class StandardPathLister implements FtpPathLister {
    private final SftpListUtil listUtil;

    public StandardPathLister(@NonNull User user) {
        this.listUtil = new SftpListUtil(user);
    }

    //For unit-tests
    StandardPathLister(SftpListUtil listUtil) {
        this.listUtil = listUtil;
    }

    @Override
    public @NonNull List<@NonNull SftpNodeChain> list(@NonNull SftpNodeChain directory) throws IOException {
        SftpNode lastNode = directory.getLast().orElse(null);
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
                        SftpNode secondLastNode = directory.nodes().get(directory.nodes().size() - 2);
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
    public SftpFileAttributes readAttributes(@NonNull SftpNodeChain nodeChain)
            throws NoSuchFileException {
        if ( pointsToAfsFile(nodeChain) ) {
            Optional<EntityDescriptor> entityDescriptorOpt = toEntityDescriptor(nodeChain);
            if (entityDescriptorOpt.isPresent() && entityDescriptorOpt.get().type() == SftpNode.Type.AFS_FILE) {
                EntityDescriptor entityDescriptor = entityDescriptorOpt.get();

                String afsFilePath = entityDescriptor.afsPath();
                boolean isAfsEntityDataMutable = entityDescriptor.afsEntity().mutable();
                String afsEntityPermId = entityDescriptor.afsEntity().identifier().orElseThrow();

                if ("/".equals(afsFilePath) && isAfsEntityDataMutable) {
                    listUtil.tryToCreateAfsFileRootIfNecessary(
                            afsEntityPermId
                    );
                }

                Optional<SftpFileAttributes> attributes =  listUtil.getDefaultAfsFileAttributes(
                        afsEntityPermId, afsFilePath, isAfsEntityDataMutable
                );

                if (attributes.isPresent()) {
                    return attributes.get();
                } else {
                    if ("/".equals(afsFilePath) && !isAfsEntityDataMutable) {
                        SftpListUtil.EntityBasicInfo afsEntityBasicInfo = listUtil.checkExistence(entityDescriptor.afsEntity());
                        if (afsEntityBasicInfo.exists()) {
                            return SftpListUtil.getDefaultAbstractDirectoryAttributes(
                                    false,
                                    afsEntityBasicInfo.registrationMillis(),
                                    afsEntityBasicInfo.lastModificationMillis()
                            );
                        } else {
                            throw new NoSuchFileException(
                                    String.format("Entity of type : %s and identifier : %s",
                                            entityDescriptor.afsEntity().type(),
                                            afsEntityPermId
                                    )
                            );
                        }
                    } else {
                        throw new NoSuchFileException("AFS entity perm-id : " + afsEntityPermId + " AFS file-path : " + afsFilePath);
                    }
                }

            } else {
                throw new IllegalStateException("Entity descriptor should be of type AFS_FILE");
            }
        } else {
            SftpNode lastNode = nodeChain.getLast().orElse(null);

            if (lastNode != null) {
                return switch (lastNode.getType()) {
                    case ROOT -> SftpListUtil.getDefaultAbstractDirectoryAttributes(false, null, null);
                    case SUBLEVEL -> {
                        Optional<EntityDescriptor> parentEntityDescriptorOpt = toEntityDescriptor(nodeChain.toParent());
                        if (parentEntityDescriptorOpt.isPresent()) {
                            EntityDescriptor parentEntityDescriptor = parentEntityDescriptorOpt.get();
                            SftpListUtil.EntityBasicInfo entityBasicInfo = listUtil.checkExistence(parentEntityDescriptor);
                            if (entityBasicInfo.exists()) {
                                yield  SftpListUtil.getDefaultAbstractDirectoryAttributes(
                                        true,
                                        entityBasicInfo.registrationMillis(),
                                        entityBasicInfo.lastModificationMillis()
                                );
                            } else {
                                throw new NoSuchFileException(
                                        String.format("Entity of type : %s and identifier : %s",
                                                parentEntityDescriptor.type(),
                                                parentEntityDescriptor.identifier()
                                        )
                                );
                            }
                        } else {
                            yield SftpListUtil.getDefaultAbstractDirectoryAttributes(false, null, null);
                        }
                    }
                    default -> {
                        Optional<EntityDescriptor> entityDescriptorOpt = toEntityDescriptor(nodeChain);
                        if (entityDescriptorOpt.isPresent()) {
                            EntityDescriptor entityDescriptor = entityDescriptorOpt.get();
                            SftpListUtil.EntityBasicInfo entityBasicInfo = listUtil.checkExistence(entityDescriptor);
                            if (entityBasicInfo.exists()) {
                                yield  SftpListUtil.getDefaultAbstractDirectoryAttributes(
                                        false,
                                        entityBasicInfo.registrationMillis(),
                                        entityBasicInfo.lastModificationMillis()
                                );
                            } else {
                                throw new NoSuchFileException(
                                    String.format("Entity of type : %s and identifier : %s",
                                        entityDescriptor.type(),
                                        entityDescriptor.identifier()
                                    )
                                );
                            }
                        } else {
                            throw new IllegalStateException("Missing entity descriptor");
                        }
                    }
                };
            } else {
                return SftpListUtil.getDefaultAbstractDirectoryAttributes(false, null, null);
            }
        }
    }

    @Override
    public Optional<EntityDescriptor> toEntityDescriptor(@NonNull SftpNodeChain nodeChain) {
        if (nodeChain.size() > 0) {
            SftpNode lastNode = nodeChain.getLast().orElseThrow();
            SftpNode.Type lastNodeType = lastNode.getType();
            return switch (lastNodeType) {
                case ROOT -> Optional.empty();
                case SPACE -> Optional.of(new EntityDescriptor(
                        SftpNode.Type.SPACE,
                        Optional.of(lastNode.getIdentifier().orElseThrow()),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(lastNode.getIdentifier().orElseThrow()),
                        Optional.empty(),
                        false,
                        null,
                        null
                ));
                case PROJECT -> Optional.of(new EntityDescriptor(
                        SftpNode.Type.PROJECT,
                        Optional.of(nodeChain.lookUpSpaceCode()),
                        Optional.of(lastNode.getIdentifier().orElseThrow()),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(new ProjectIdentifier(
                                Optional.of(nodeChain.lookUpSpaceCode()).get(),
                                lastNode.getIdentifier().orElseThrow()
                        ).getIdentifier()),
                        Optional.empty(),
                        false,
                        null,
                        null
                ));
                case EXPERIMENT, FOLDER, SAMPLE, DATA_SET -> {
                    String entityPermId = SftpListUtil.getEntityPermIdFromDisplayName(
                            lastNode.getIdentifier().orElseThrow()
                    );
                    String nameProperty = SftpListUtil.getEntityNameFromDisplayName(
                            lastNode.getIdentifier().orElseThrow()
                    );
                    yield Optional.of(new EntityDescriptor(
                        lastNodeType,
                        Optional.of(nodeChain.lookUpSpaceCode()),
                        lastNodeType == SftpNode.Type.EXPERIMENT ?
                                Optional.of(nodeChain.lookUpProjectCode()) :
                                Optional.ofNullable(nodeChain.lookUpProjectCode()),
                        lastNodeType == SftpNode.Type.EXPERIMENT ?
                                Optional.ofNullable(entityPermId) :
                                Optional.ofNullable(nodeChain.lookUpExperimentPermId()),
                        switch (lastNodeType) {
                            case DATA_SET -> Optional.ofNullable(nodeChain.lookUpParentSamplePermId());
                            case SAMPLE, FOLDER -> Optional.ofNullable(nodeChain.toParent().lookUpParentSamplePermId());
                            default -> Optional.empty();
                        },
                        Optional.ofNullable(entityPermId),
                        Optional.ofNullable(nameProperty),
                            entityPermId == null ||
                                    listUtil.isAfsEntityMutable(entityPermId, lastNodeType),
                        null, null
                    ));
                }
                case SUBLEVEL, AFS_FILE -> {
                    if ( pointsToAfsFile(nodeChain) ) {
                        SftpNode afsEntityNode = validateAndGetAfsEntityNodeFromAfsFileChain(nodeChain);
                        String afsFilePath = validateAndGetAfsFilePathFromAfsFileChain(nodeChain);
                        String entityPermId = Objects.requireNonNull(SftpListUtil.getEntityPermIdFromDisplayName(
                                afsEntityNode.getIdentifier().orElseThrow()
                        ));
                        yield Optional.of(new EntityDescriptor(
                                SftpNode.Type.AFS_FILE,
                                Optional.ofNullable(nodeChain.lookUpSpaceCode()),
                                Optional.ofNullable(nodeChain.lookUpProjectCode()),
                                afsEntityNode.getType() == SftpNode.Type.EXPERIMENT ?
                                        Optional.of(entityPermId) :
                                        Optional.ofNullable(nodeChain.lookUpExperimentPermId()),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(), false,
                                new EntityDescriptor(
                                    afsEntityNode.getType(),
                                    Optional.ofNullable(nodeChain.lookUpSpaceCode()),
                                    Optional.ofNullable(nodeChain.lookUpProjectCode()),
                                    afsEntityNode.getType() == SftpNode.Type.EXPERIMENT ?
                                            Optional.of(entityPermId) :
                                            Optional.ofNullable(nodeChain.lookUpExperimentPermId()),
                                    Optional.empty(),
                                    Optional.of(entityPermId),
                                    Optional.ofNullable(SftpListUtil.getEntityNameFromDisplayName(
                                            afsEntityNode.getIdentifier().orElseThrow()
                                    )),
                                    listUtil.isAfsEntityMutable(entityPermId, afsEntityNode.getType()),
                                    null, null
                                ),
                                afsFilePath
                        ));
                    } else {
                        yield Optional.empty();
                    }
                }
            };
        } else {
            return Optional.empty();
        }
    }

    @NonNull List<@NonNull SftpNodeChain> listRoot(String sublevel, @NonNull SftpNodeChain fullChain) {
        if ( sublevel == null ) {
            return List.of(
                    SftpNodeChain.concat(
                            fullChain,
                            SftpNodeChain.createSublevelNode(StandardPathTranslator.SPACE_TYPE_LABEL)
                    )
            );
        } else if ( sublevel.equals( StandardPathTranslator.SPACE_TYPE_LABEL )) {
            List<SftpNodeChain> listedSpaces = new ArrayList<>();
            listUtil.getSpaces().iterator().forEachRemaining(
                space -> {
                    listedSpaces.add(SftpNodeChain.concat(fullChain,
                            SftpNodeChain.fromSpace(space))
                    );
                }
            );

            return listedSpaces;
        } else {
            return Collections.emptyList();
        }
    }

    @NonNull List<@NonNull SftpNodeChain> listSpace(@NonNull SftpNode spaceNode, String sublevel, @NonNull SftpNodeChain fullChain) {
        if ( sublevel == null ) {
            return List.of(
                    SftpNodeChain.concat(
                            fullChain,
                            SftpNodeChain.createSublevelNode(StandardPathTranslator.FOLDER_TYPE_LABEL)
                    ),
                    SftpNodeChain.concat(
                            fullChain,
                            SftpNodeChain.createSublevelNode(StandardPathTranslator.SAMPLE_TYPE_LABEL)
                    ),
                    SftpNodeChain.concat(
                            fullChain,
                            SftpNodeChain.createSublevelNode(StandardPathTranslator.PROJECT_TYPE_LABEL)
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

    @NonNull List<@NonNull SftpNodeChain> listProjectsInSpace(@NonNull SftpNode spaceNode, @NonNull SftpNodeChain fullChain) {
        List<SftpNodeChain> listedProjects = new ArrayList<>();
        listUtil.getProjects(
                spaceNode.getIdentifier()
                        .map(SftpListUtil::getSpaceCodeFromDisplayName).orElseThrow()
        ).iterator().forEachRemaining(
                project -> {
                    listedProjects.add(SftpNodeChain.concat(fullChain,
                            SftpNodeChain.fromProject(project))
                    );
                }
        );

        return listedProjects;
    }

    @NonNull List<@NonNull SftpNodeChain> listSamplesOrFoldersInSpace(@NonNull SftpNode spaceNode, @NonNull SftpNodeChain fullChain, boolean folders) {
        List<SftpNodeChain> listedSamples = new ArrayList<>();
        listUtil.getSpaceSamples(
                spaceNode.getIdentifier()
                        .map(SftpListUtil::getSpaceCodeFromDisplayName).orElseThrow()
        ).iterator().forEachRemaining(
                sample -> {
                    if (folders == SftpListUtil.isOfTypeFolder(sample)) {
                        listedSamples.add(SftpNodeChain.concat(fullChain,
                                SftpNodeChain.fromSample(sample))
                        );
                    }
                }
        );

        return listedSamples;
    }

    @NonNull List<@NonNull SftpNodeChain> listSamplesOrFoldersInProject(@NonNull SftpNode projectNode, @NonNull SftpNodeChain fullChain, boolean folders) {
        String spaceCode = fullChain.lookUpSpaceCode();

        List<SftpNodeChain> listedSamples = new ArrayList<>();
        listUtil.getProjectSamples(
                Objects.requireNonNull(spaceCode),
                projectNode.getIdentifier().map(SftpListUtil::getProjectCodeFromDisplayName).orElseThrow()
        ).iterator().forEachRemaining(
            sample -> {
                //TODO: decide if folders have to be displayed differently from other sample-types
                if (folders == SftpListUtil.isOfTypeFolder(sample)) {
                    listedSamples.add(SftpNodeChain.concat(fullChain,
                            SftpNodeChain.fromSample(sample))
                    );
                }
            }
        );

        return listedSamples;
    }

    @NonNull List<@NonNull SftpNodeChain> listSample(@NonNull SftpNode sampleNode, String sublevel, @NonNull SftpNodeChain fullChain) {
        if ( sublevel == null ) {
            return List.of(
                    SftpNodeChain.concat(
                            fullChain,
                            SftpNodeChain.createSublevelNode(StandardPathTranslator.FOLDER_TYPE_LABEL)
                    ),
                    SftpNodeChain.concat(
                            fullChain,
                            SftpNodeChain.createSublevelNode(StandardPathTranslator.SAMPLE_TYPE_LABEL)
                    ),
                    SftpNodeChain.concat(
                            fullChain,
                            SftpNodeChain.createSublevelNode(StandardPathTranslator.DATA_SET_TYPE_LABEL)
                    ),
                    SftpNodeChain.concat(
                            fullChain,
                            SftpNodeChain.createSublevelNode(StandardPathTranslator.FILE_TYPE_LABEL)
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

    @NonNull List<@NonNull SftpNodeChain> listFolder(@NonNull SftpNode folderNode, String sublevel, @NonNull SftpNodeChain fullChain) {
        if ( sublevel == null ) {
            return List.of(
                    SftpNodeChain.concat(
                            fullChain,
                            SftpNodeChain.createSublevelNode(StandardPathTranslator.FOLDER_TYPE_LABEL)
                    ),
                    SftpNodeChain.concat(
                            fullChain,
                            SftpNodeChain.createSublevelNode(StandardPathTranslator.SAMPLE_TYPE_LABEL)
                    ),
                    SftpNodeChain.concat(
                            fullChain,
                            SftpNodeChain.createSublevelNode(StandardPathTranslator.DATA_SET_TYPE_LABEL)
                    ),
                    SftpNodeChain.concat(
                            fullChain,
                            SftpNodeChain.createSublevelNode(StandardPathTranslator.FILE_TYPE_LABEL)
                    )
            );
        } else if ( sublevel.equals(StandardPathTranslator.FOLDER_TYPE_LABEL) ) {
            return listSamplesOrFoldersInSample(folderNode, fullChain, true);
        } else if ( sublevel.equals(StandardPathTranslator.SAMPLE_TYPE_LABEL) ) {
            return listSamplesOrFoldersInSample(folderNode, fullChain, false);
        } else if ( sublevel.equals(StandardPathTranslator.DATA_SET_TYPE_LABEL) ) {
            return listDataSetsInSample(folderNode, fullChain);
        } else if ( sublevel.equals(StandardPathTranslator.FILE_TYPE_LABEL) ) {
            return listFilesInSampleOrFolder(folderNode, fullChain);
        } else {
            return Collections.emptyList();
        }
    }

    @NonNull List<@NonNull SftpNodeChain> listSamplesOrFoldersInSample(@NonNull SftpNode sampleNode, @NonNull SftpNodeChain fullChain, boolean folders) {
        List<SftpNodeChain> listedSamples = new ArrayList<>();
        listUtil.getSampleChildren(
                sampleNode.getIdentifier().map(SftpListUtil::getEntityPermIdFromDisplayName).orElseThrow()
        ).iterator().forEachRemaining(
            sample -> {
                //TODO: decide if folders have to be displayed differently from other sample-types
                if (folders == SftpListUtil.isOfTypeFolder(sample)) {
                    listedSamples.add(SftpNodeChain.concat(fullChain,
                            SftpNodeChain.fromSample(sample))
                    );
                }
            }
        );

        return listedSamples;
    }

    @NonNull List<@NonNull SftpNodeChain> listDataSetsInSample(@NonNull SftpNode sampleNode, @NonNull SftpNodeChain fullChain) {
        List<SftpNodeChain> listedDatasets = new ArrayList<>();
        listUtil.getSampleDatasets(
                sampleNode.getIdentifier().map(SftpListUtil::getEntityPermIdFromDisplayName).orElseThrow()
        ).iterator().forEachRemaining(
                dataSet -> {
                    listedDatasets.add(SftpNodeChain.concat(fullChain,
                            SftpNodeChain.fromDataSet(dataSet))
                    );
                }
        );

        return listedDatasets;
    }

    @NonNull List<@NonNull SftpNodeChain> listDataSetsInExperiment(@NonNull SftpNode experimentNode, @NonNull SftpNodeChain fullChain) {
        List<SftpNodeChain> listedDatasets = new ArrayList<>();
        listUtil.getExperimentDatasets(
                experimentNode.getIdentifier().map(SftpListUtil::getEntityPermIdFromDisplayName).orElseThrow()
        ).iterator().forEachRemaining(
                dataSet -> {
                    listedDatasets.add(SftpNodeChain.concat(fullChain,
                            SftpNodeChain.fromDataSet(dataSet))
                    );
                }
        );

        return listedDatasets;
    }

    @NonNull List<@NonNull SftpNodeChain> listFilesInSampleOrFolder(@NonNull SftpNode sampleNode, @NonNull SftpNodeChain fullChain) {
        String samplePermId = listUtil.getAfsEntityPermId(sampleNode);

        if (samplePermId != null) {
            List<SftpNodeChain> listedAfsFiles = new ArrayList<>();

            File[] files = listUtil.listAfsFiles(samplePermId, "/");

            Arrays.asList(files).iterator().forEachRemaining(
                    file -> {
                        listedAfsFiles.add(SftpNodeChain.concat(fullChain,
                                SftpNodeChain.fromAfsFilePath(Collections.singletonList(file.getName())))
                        );
                    }
            );

            return listedAfsFiles;
        } else {
            return Collections.emptyList();
        }
    }

    @NonNull List<@NonNull SftpNodeChain> listFilesInDataSet(@NonNull SftpNode dataSetNode, @NonNull SftpNodeChain fullChain) {
        String dataSetPermId = listUtil.getAfsEntityPermId(dataSetNode);

        if (dataSetPermId != null) {
            List<SftpNodeChain> listedAfsFiles = new ArrayList<>();

            File[] files = listUtil.listAfsFiles(dataSetPermId, "/");

            Arrays.asList(files).iterator().forEachRemaining(
                    file -> {
                        listedAfsFiles.add(SftpNodeChain.concat(fullChain,
                                        SftpNodeChain.fromAfsFilePath(Collections.singletonList(file.getName())))
                        );
                    }
            );

            return listedAfsFiles;
        } else {
            return Collections.emptyList();
        }
    }

    @NonNull List<@NonNull SftpNodeChain> listFilesInExperiment(@NonNull SftpNode experimentNode, @NonNull SftpNodeChain fullChain) {
        String experimentPermId = listUtil.getAfsEntityPermId(experimentNode);

        if (experimentPermId != null) {
            List<SftpNodeChain> listedAfsFiles = new ArrayList<>();

            File[] files = listUtil.listAfsFiles(experimentPermId, "/");

            Arrays.asList(files).iterator().forEachRemaining(
                    file -> {
                        listedAfsFiles.add(SftpNodeChain.concat(fullChain,
                                        SftpNodeChain.fromAfsFilePath(Collections.singletonList(file.getName())))
                        );
                    }
            );

            return listedAfsFiles;
        } else {
            return Collections.emptyList();
        }
    }

    @NonNull List<@NonNull SftpNodeChain> listDataSet(@NonNull SftpNode dataSetNode, String sublevel, @NonNull SftpNodeChain fullChain) {
        if ( sublevel == null ) {
            return List.of(
                    SftpNodeChain.concat(
                            fullChain,
                            SftpNodeChain.createSublevelNode(StandardPathTranslator.FILE_TYPE_LABEL)
                    )
            );
        } else if ( sublevel.equals(StandardPathTranslator.FILE_TYPE_LABEL) ) {
            return listFilesInDataSet(dataSetNode, fullChain);
        } else {
            return Collections.emptyList();
        }
    }

    @NonNull List<@NonNull SftpNodeChain> listProject(@NonNull SftpNode projectNode, String sublevel, @NonNull SftpNodeChain fullChain) {
        if ( sublevel == null ) {
            return List.of(
                    SftpNodeChain.concat(
                            fullChain,
                            SftpNodeChain.createSublevelNode(StandardPathTranslator.FOLDER_TYPE_LABEL)
                    ),
                    SftpNodeChain.concat(
                            fullChain,
                            SftpNodeChain.createSublevelNode(StandardPathTranslator.SAMPLE_TYPE_LABEL)
                    ),
                    SftpNodeChain.concat(
                            fullChain,
                            SftpNodeChain.createSublevelNode(StandardPathTranslator.EXPERIMENT_TYPE_LABEL)
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

    @NonNull List<@NonNull SftpNodeChain> listExperimentsInProject(@NonNull SftpNode projectNode, @NonNull SftpNodeChain fullChain) {
        String spaceCode = fullChain.lookUpSpaceCode();

        List<SftpNodeChain> listedExperiments = new ArrayList<>();
        listUtil.getExperiments(
                Objects.requireNonNull(spaceCode),
                projectNode.getIdentifier().map(SftpListUtil::getProjectCodeFromDisplayName).orElseThrow()
        ).iterator().forEachRemaining(
                experiment -> {
                    listedExperiments.add(SftpNodeChain.concat(fullChain,
                            SftpNodeChain.fromExperiment(experiment))
                    );
                }
        );

        return listedExperiments;
    }

    @NonNull List<@NonNull SftpNodeChain> listExperiment(@NonNull SftpNode experimentNode, String sublevel, @NonNull SftpNodeChain fullChain) {
        if ( sublevel == null ) {
            return List.of(
                    SftpNodeChain.concat(
                            fullChain,
                            SftpNodeChain.createSublevelNode(StandardPathTranslator.FOLDER_TYPE_LABEL)
                    ),
                    SftpNodeChain.concat(
                            fullChain,
                            SftpNodeChain.createSublevelNode(StandardPathTranslator.SAMPLE_TYPE_LABEL)
                    ),
                    SftpNodeChain.concat(
                            fullChain,
                            SftpNodeChain.createSublevelNode(StandardPathTranslator.DATA_SET_TYPE_LABEL)
                    ),
                    SftpNodeChain.concat(
                            fullChain,
                            SftpNodeChain.createSublevelNode(StandardPathTranslator.FILE_TYPE_LABEL)
                    )
            );
        } else if ( sublevel.equals(StandardPathTranslator.FOLDER_TYPE_LABEL) ) {
            return listSamplesOrFoldersInExperiment(experimentNode, fullChain, true);
        } else if ( sublevel.equals(StandardPathTranslator.SAMPLE_TYPE_LABEL) ) {
            return listSamplesOrFoldersInExperiment(experimentNode, fullChain, false);
        } else if ( sublevel.equals(StandardPathTranslator.DATA_SET_TYPE_LABEL) ) {
            return listDataSetsInExperiment(experimentNode, fullChain);
        } else if ( sublevel.equals(StandardPathTranslator.FILE_TYPE_LABEL) ) {
            return listFilesInExperiment(experimentNode, fullChain);
        } else {
            return Collections.emptyList();
        }
    }

    @NonNull List<@NonNull SftpNodeChain> listSamplesOrFoldersInExperiment(@NonNull SftpNode experimentNode, @NonNull SftpNodeChain fullChain, boolean folders) {
        List<SftpNodeChain> listedSamples = new ArrayList<>();
        listUtil.getExperimentSamples(
                experimentNode.getIdentifier().map(SftpListUtil::getEntityPermIdFromDisplayName).orElseThrow()
        ).iterator().forEachRemaining(
                sample -> {
                    //TODO: decide if folders have to be displayed differently from other sample-types
                    if (folders == SftpListUtil.isOfTypeFolder(sample)) {
                        listedSamples.add(SftpNodeChain.concat(fullChain,
                                SftpNodeChain.fromSample(sample))
                        );
                    }
                }
        );

        return listedSamples;
    }



    @NonNull List<@NonNull SftpNodeChain> listFilesInAfsFileNode(@NonNull SftpNode afsFileNode, @NonNull SftpNodeChain fullChain) {
        SftpNodeChain baseChain = new SftpNodeChain(
                fullChain.nodes().subList(0, fullChain.nodes().size() - 1)
        );

        SftpNode afsEntityNode = validateAndGetAfsEntityNodeFromAfsFileChain(fullChain);
        String afsEntityPermId = listUtil.getAfsEntityPermId(afsEntityNode);

        if (afsEntityPermId != null) {
            List<SftpNodeChain> listedAfsFiles = new ArrayList<>();

            File[] files = listUtil.listAfsFiles(afsEntityPermId, afsFileNode.getJoinedAfsFilePath());

            Arrays.asList(files).iterator().forEachRemaining(
                    file -> {
                        List<String> afsPathSegments = Arrays.stream(file.getPath().split("/")).filter(
                                segment -> !segment.isEmpty()
                        ).toList();
                        listedAfsFiles.add(
                            SftpNodeChain.concat(
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

    @NonNull
    SftpNode validateAndGetAfsEntityNodeFromAfsFileChain(@NonNull SftpNodeChain afsFileChain) {
        if (afsFileChain.size() > 1) {
            SftpNode lastNode = afsFileChain.getLast().orElseThrow();

            if (lastNode.getType() == SftpNode.Type.AFS_FILE && afsFileChain.size() > 2) {
                SftpNode afsTypeSublevelNode = afsFileChain.get(afsFileChain.size() - 2);
                SftpNode afsEntityNode = afsFileChain.get(afsFileChain.size() - 3);
                if (afsTypeSublevelNode.getType() == SftpNode.Type.SUBLEVEL &&
                        afsTypeSublevelNode.getIdentifier().orElseThrow().equals(StandardPathTranslator.FILE_TYPE_LABEL) &&
                        SftpListUtil.POSSIBLE_AFS_ENTITY_TYPES.contains(afsEntityNode.getType())
                ) {
                    return afsEntityNode;
                } else {
                    throw new IllegalArgumentException("Malformed AFS-file node-chain");
                }
            } else if (lastNode.getType() == SftpNode.Type.SUBLEVEL &&
                    lastNode.getIdentifier().orElseThrow().equals(StandardPathTranslator.FILE_TYPE_LABEL) &&
                    afsFileChain.size() > 1) {
                SftpNode afsEntityNode = afsFileChain.get(afsFileChain.size() - 2);
                if (SftpListUtil.POSSIBLE_AFS_ENTITY_TYPES.contains(afsEntityNode.getType())) {
                    return afsEntityNode;
                } else {
                    throw new IllegalArgumentException("Malformed AFS-file node-chain");
                }
            } else {
                throw new IllegalArgumentException("Malformed AFS-file node-chain");
            }
        } else {
            throw new IllegalArgumentException("Malformed AFS-file node-chain");
        }
    }

    @NonNull
    String validateAndGetAfsFilePathFromAfsFileChain(@NonNull SftpNodeChain afsFileChain) {
        if (afsFileChain.size() > 1) {
            SftpNode lastNode = afsFileChain.getLast().orElseThrow();

            if (lastNode.getType() == SftpNode.Type.AFS_FILE && afsFileChain.size() > 2) {
                SftpNode afsTypeSublevelNode = afsFileChain.get(afsFileChain.size() - 2);
                SftpNode afsEntityNode = afsFileChain.get(afsFileChain.size() - 3);
                if (afsTypeSublevelNode.getType() == SftpNode.Type.SUBLEVEL &&
                        afsTypeSublevelNode.getIdentifier().orElseThrow().equals(StandardPathTranslator.FILE_TYPE_LABEL) &&
                        SftpListUtil.POSSIBLE_AFS_ENTITY_TYPES.contains(afsEntityNode.getType())
                ) {
                    return lastNode.getJoinedAfsFilePath();
                } else {
                    throw new IllegalArgumentException("Malformed AFS-file node-chain");
                }
            } else if (lastNode.getType() == SftpNode.Type.SUBLEVEL &&
                    lastNode.getIdentifier().orElseThrow().equals(StandardPathTranslator.FILE_TYPE_LABEL) &&
                    afsFileChain.size() > 1) {
                SftpNode afsEntityNode = afsFileChain.get(afsFileChain.size() - 2);
                if (SftpListUtil.POSSIBLE_AFS_ENTITY_TYPES.contains(afsEntityNode.getType())) {
                    return "/";
                } else {
                    throw new IllegalArgumentException("Malformed AFS-file node-chain");
                }
            } else {
                throw new IllegalArgumentException("Malformed AFS-file node-chain");
            }
        } else {
            throw new IllegalArgumentException("Malformed AFS-file node-chain");
        }
    }

    boolean pointsToAfsFile(@NonNull SftpNodeChain afsFileChain) {
        return afsFileChain.size() > 1 && afsFileChain.getLast().map(
            lastNode -> lastNode.getType() == SftpNode.Type.AFS_FILE ||
                    (
                        lastNode.getType() == SftpNode.Type.SUBLEVEL &&
                                lastNode.getIdentifier().orElseThrow().equals(StandardPathTranslator.FILE_TYPE_LABEL)
                    )
        ).orElse(false);
    }
}
