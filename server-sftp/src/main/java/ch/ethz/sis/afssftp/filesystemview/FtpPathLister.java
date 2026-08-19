package ch.ethz.sis.afssftp.filesystemview;

import ch.ethz.sis.afssftp.util.SftpListUtil;
import lombok.NonNull;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface FtpPathLister {
    @NonNull List<@NonNull SftpNodeChain> list(@NonNull SftpNodeChain directory) throws IOException;
    SftpFileAttributes readAttributes(@NonNull SftpNodeChain nodeChain) throws IOException;
    Optional<EntityDescriptor> toEntityDescriptor(@NonNull SftpNodeChain nodeChain) throws IOException;

    record EntityDescriptor(
            @NonNull SftpNode.Type type,

            // This property can be present for:
            // SPACE, PROJECT, FOLDER, SAMPLE, EXPERIMENT, DATA_SET
            // It represents: the code of the space in which the entity is located (if any)
            Optional<String> spaceCode,

            // This property can be present for:
            // PROJECT, FOLDER, SAMPLE, EXPERIMENT, DATA_SET
            // It represents: the code of the project in which the entity is located (if any)
            Optional<String> projectCode,

            // This property can be present for:
            // FOLDER, SAMPLE, DATA_SET
            // It represents: the perm-id of the experiment in which the entity is located (if any)
            Optional<String> experimentId,

            // This property can be present for:
            // FOLDER, SAMPLE, DATA_SET
            // It represents: the perm-id of a sample to which this entity is attached (if any)
            Optional<String> parentSampleId,

            // This property is required only for types:
            // SPACE, PROJECT, FOLDER, SAMPLE, EXPERIMENT, DATA_SET
            // It represents: space-code for SPACE, concatenation of space-code and project-code for projects,
            // perm-id otherwise
            // It will be empty in paths input for the creation of:
            // FOLDER, SAMPLE, EXPERIMENT, DATA_SET;
            // otherwise it must be present
            Optional<String> identifier,

            // This property can be present for types:
            // FOLDER, SAMPLE, EXPERIMENT, DATA_SET
            // It represents: the NAME-property of the entity, as it is parsed from the path.
            // This might or might not correspond to the real NAME-property of the entity:
            // in cases like entity creation or renaming, it represents the desired new name
            Optional<String> name,

            // These properties are required only for type AFS_FILE
            EntityDescriptor afsEntity,
            String afsPath,

            // This property is required (and makes sense) only for types:
            // SPACE, PROJECT, FOLDER, SAMPLE, EXPERIMENT, DATA_SET
            SftpListUtil.EntityBasicInfo entityBasicInfo
    ){}
}
