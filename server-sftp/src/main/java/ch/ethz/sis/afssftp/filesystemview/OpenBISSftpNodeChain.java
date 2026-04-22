package ch.ethz.sis.afssftp.filesystemview;

import ch.ethz.sis.afssftp.util.OpenBISListUtil;
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
public class OpenBISSftpNodeChain {
    private final @NonNull List<@NonNull OpenBISSftpNode> nodes;

    public OpenBISSftpNodeChain(@NonNull List<@NonNull OpenBISSftpNode> nodes) {
        this.nodes = nodes;
    }

    public @NonNull List<@NonNull OpenBISSftpNode> nodes() {
        return nodes;
    }

    public @NonNull Optional<OpenBISSftpNode> getLast() {
        return !nodes.isEmpty() ? Optional.of(nodes.getLast()) : Optional.empty();
    }

    public int size() {
        return nodes.size();
    }

    public OpenBISSftpNode get(int index) {
        return nodes.get(index);
    }

    public static @NonNull OpenBISSftpNodeChain concat(
            @NonNull OpenBISSftpNodeChain chain1,
            @NonNull OpenBISSftpNodeChain chain2) {
        List<OpenBISSftpNode> nodes = new ArrayList<>(chain1.nodes());
        nodes.addAll(chain2.nodes());
        return new OpenBISSftpNodeChain(nodes);
    }

    public static @NonNull OpenBISSftpNodeChain concat(
            @NonNull OpenBISSftpNodeChain chain,
            @NonNull OpenBISSftpNode newNode) {
        List<OpenBISSftpNode> nodes = new ArrayList<>(chain.nodes());
        nodes.add(newNode);
        return new OpenBISSftpNodeChain(nodes);
    }

    public static @NonNull OpenBISSftpNode createRootNode() {
        return new OpenBISSftpNode(
                OpenBISSftpNode.Type.ROOT,
                Optional.empty(),
                Collections.emptyList()
        );
    }

    public static @NonNull OpenBISSftpNodeChain createRoot() {
        return new OpenBISSftpNodeChain(Collections.singletonList(
                createRootNode()
        )
        );
    }

    public static @NonNull OpenBISSftpNode fromSpace(@NonNull Space space) {
        return new OpenBISSftpNode(
                OpenBISSftpNode.Type.SPACE,
                Optional.of(space.getCode()),
                Collections.emptyList()
        );
    }

    public static @NonNull OpenBISSftpNode fromProject(@NonNull Project project) {
        return new OpenBISSftpNode(
                OpenBISSftpNode.Type.PROJECT,
                Optional.of(project.getCode()),
                Collections.emptyList()
        );
    }

    public static @NonNull OpenBISSftpNode fromSample(@NonNull Sample sample) {
        return new OpenBISSftpNode(
                OpenBISListUtil.FOLDER_SAMPLE_TYPE.equals(sample.getType().getCode()) ?
                        OpenBISSftpNode.Type.FOLDER : OpenBISSftpNode.Type.SAMPLE,
                Optional.of(sample.getCode()),
                Collections.emptyList()
        );
    }

    public static @NonNull OpenBISSftpNode fromExperiment(@NonNull Experiment experiment) {
        return new OpenBISSftpNode(
                OpenBISSftpNode.Type.EXPERIMENT,
                Optional.of(experiment.getCode()),
                Collections.emptyList()
        );
    }

    public static @NonNull OpenBISSftpNode fromDataSet(@NonNull DataSet dataSet) {
        return new OpenBISSftpNode(
                OpenBISSftpNode.Type.DATA_SET,
                Optional.of(dataSet.getCode()),
                Collections.emptyList()
        );
    }

    public static @NonNull OpenBISSftpNode fromAfsFilePath(@NonNull List<@NonNull String> tokenizedPath) {
        return new OpenBISSftpNode(
                OpenBISSftpNode.Type.AFS_FILE,
                Optional.empty(),
                tokenizedPath
        );
    }

    public static @NonNull OpenBISSftpNode createSublevelNode(@NonNull String sublevel) {
        return new OpenBISSftpNode(
                OpenBISSftpNode.Type.SUBLEVEL,
                Optional.of(sublevel),
                Collections.emptyList()
        );
    }

    public String lookUpSpaceCode() {
        return this.nodes().stream().filter(node -> node.getType() == OpenBISSftpNode.Type.SPACE)
                .findFirst().flatMap(OpenBISSftpNode::getIdentifier).orElse(null);
    }

    public String lookUpProjectCode() {
        return this.nodes().stream().filter(node -> node.getType() == OpenBISSftpNode.Type.PROJECT)
                .findFirst().flatMap(OpenBISSftpNode::getIdentifier).orElse(null);
    }
}
