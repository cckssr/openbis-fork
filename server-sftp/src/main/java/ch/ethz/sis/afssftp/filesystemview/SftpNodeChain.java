package ch.ethz.sis.afssftp.filesystemview;

import ch.ethz.sis.afssftp.util.SftpListUtil;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ToString
@EqualsAndHashCode
public class SftpNodeChain {
    private final @NonNull List<@NonNull SftpNode> nodes;

    public SftpNodeChain(@NonNull List<@NonNull SftpNode> nodes) {
        this.nodes = nodes;
    }

    public @NonNull List<@NonNull SftpNode> nodes() {
        return nodes;
    }

    public @NonNull Optional<SftpNode> getLast() {
        return !nodes.isEmpty() ? Optional.of(nodes.getLast()) : Optional.empty();
    }

    public int size() {
        return nodes.size();
    }

    public SftpNode get(int index) {
        return nodes.get(index);
    }

    public static @NonNull SftpNodeChain concat(
            @NonNull SftpNodeChain chain1,
            @NonNull SftpNodeChain chain2) {
        List<SftpNode> nodes = new ArrayList<>(chain1.nodes());
        nodes.addAll(chain2.nodes());
        return new SftpNodeChain(nodes);
    }

    public static @NonNull SftpNodeChain concat(
            @NonNull SftpNodeChain chain,
            @NonNull SftpNode newNode) {
        List<SftpNode> nodes = new ArrayList<>(chain.nodes());
        nodes.add(newNode);
        return new SftpNodeChain(nodes);
    }

    public static @NonNull SftpNode createRootNode() {
        return new SftpNode(
                SftpNode.Type.ROOT,
                Optional.empty(),
                Collections.emptyList()
        );
    }

    public static @NonNull SftpNodeChain createRoot() {
        return new SftpNodeChain(Collections.singletonList(
                createRootNode()
        )
        );
    }

    public static @NonNull SftpNode fromSpace(@NonNull Space space) {
        return new SftpNode(
                SftpNode.Type.SPACE,
                Optional.of(SftpListUtil.getDisplayName(space)),
                Collections.emptyList()
        );
    }

    public static @NonNull SftpNode fromProject(@NonNull Project project) {
        return new SftpNode(
                SftpNode.Type.PROJECT,
                Optional.of(SftpListUtil.getDisplayName(project)),
                Collections.emptyList()
        );
    }

    public static @NonNull SftpNode fromSample(@NonNull Sample sample) {
        return new SftpNode(
                SftpListUtil.FOLDER_SAMPLE_TYPE.equals(sample.getType().getCode()) ?
                        SftpNode.Type.FOLDER : SftpNode.Type.SAMPLE,
                Optional.of(SftpListUtil.getDisplayName(sample)),
                Collections.emptyList()
        );
    }

    public static @NonNull SftpNode fromExperiment(@NonNull Experiment experiment) {
        return new SftpNode(
                SftpNode.Type.EXPERIMENT,
                Optional.of(SftpListUtil.getDisplayName(experiment)),
                Collections.emptyList()
        );
    }

    public static @NonNull SftpNode fromDataSet(@NonNull DataSet dataSet) {
        return new SftpNode(
                SftpNode.Type.DATA_SET,
                Optional.of(SftpListUtil.getDisplayName(dataSet)),
                Collections.emptyList()
        );
    }

    public static @NonNull SftpNode fromAfsFilePath(@NonNull List<@NonNull String> tokenizedPath) {
        return new SftpNode(
                SftpNode.Type.AFS_FILE,
                Optional.empty(),
                tokenizedPath
        );
    }

    public static @NonNull SftpNode createSublevelNode(@NonNull String sublevel) {
        return new SftpNode(
                SftpNode.Type.SUBLEVEL,
                Optional.of(sublevel),
                Collections.emptyList()
        );
    }

    public String lookUpSpaceCode() {
        return this.nodes().stream().filter(node -> node.getType() == SftpNode.Type.SPACE)
                .findFirst().flatMap(SftpNode::getIdentifier)
                .map(SftpListUtil::getSpaceCodeFromDisplayName).orElse(null);
    }

    public String lookUpProjectCode() {
        return this.nodes().stream().filter(node -> node.getType() == SftpNode.Type.PROJECT)
                .findFirst().flatMap(SftpNode::getIdentifier)
                .map(SftpListUtil::getProjectCodeFromDisplayName).orElse(null);
    }
}
