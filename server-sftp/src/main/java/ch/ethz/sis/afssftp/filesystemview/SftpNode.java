package ch.ethz.sis.afssftp.filesystemview;

import lombok.Builder;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Value
@Builder(toBuilder = true)
@ToString
public class SftpNode {
    public enum Type {
        ROOT,   // Root of the AS hierarchy
        SPACE,  // Space entity in AS
        PROJECT,    // Project entity in AS
        FOLDER, // Folder (special type of sample entity) in AS
        SAMPLE, // Sample entity (not of type "FOLDER") in AS
        EXPERIMENT, // Experiment entity in AS
        DATA_SET,   // Dataset entity AS

        SUBLEVEL,   /* This node-type can be used in a node-chain
                        to represent the grouping of some entities:
                        for example, grouping of child-entities of a certain kind */

        AFS_FILE, /* This node-type can appear at the end of a node-chain
                    to represent AFS files attached to AS entities
                    represented by previous nodes in the chain */
    }

    /**
     * The type of AS or AFS entity this node points to
     */
    @NonNull Type type;

    /**
     * Identifier of the AS entity:
     * - for type ROOT: empty
     * - for types SPACE, PROJECT, FOLDER, SAMPLE, EXPERIMENT, DATA_SET: something from which their "code" or "permId" can be obtained
     *      -> Examples from standard implementations: "SPACE-CODE", "Sample name($SAMPLE_PERM_ID)"
     * - for type SUBLEVEL: the "label" of this sublevel
     * - for AFS_FILE: not applicable, so empty
     */
    @Builder.Default
    @NonNull Optional<String> identifier = Optional.empty();

    /**
     * For type AFS_FILE:
     * the tokenized AFS file path.
     * For example: "/main_data/collection1/data.csv" would be:
     * [ "main_data" , "collection1", "data.csv" ]
     * <p>
     * Otherwise, empty list
     */
    @Builder.Default
    @NonNull List<String> afsFilePath = Collections.emptyList();

    public String getJoinedAfsFilePath() {
        return  "/" + String.join("/", getAfsFilePath());
    }
}
