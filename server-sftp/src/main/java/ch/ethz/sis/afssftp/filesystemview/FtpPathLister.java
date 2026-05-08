package ch.ethz.sis.afssftp.filesystemview;

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

            // This property is required only for types:
            // SPACE, PROJECT, FOLDER, SAMPLE, EXPERIMENT, DATA_SET
            // It represents: space-code for SPACE, concatenation of space-code and project-code for projects,
            // perm-id otherwise
            String identifier,
            // This property is required only for types:
            // FOLDER, SAMPLE, EXPERIMENT, DATA_SET
            // and specifically means: whether the entity is mutable-data
            boolean mutable,

            // These properties are required only for type AFS_FILE
            EntityDescriptor afsEntity,
            String afsPath
    ){}
}
