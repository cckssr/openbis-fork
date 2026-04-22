package ch.ethz.sis.afssftp.filesystemview.impl.standard;

import ch.ethz.sis.afssftp.filesystemview.FtpPathTranslator;
import ch.ethz.sis.afssftp.filesystemview.OpenBISSftpNode;
import ch.ethz.sis.afssftp.filesystemview.OpenBISSftpNodeChain;
import lombok.NonNull;

import java.util.*;

public class StandardPathTranslator implements FtpPathTranslator {
    public static final String SPACE_TYPE_LABEL = "spaces";
    public static final String PROJECT_TYPE_LABEL = "projects";
    public static final String SAMPLE_TYPE_LABEL = "samples";
    public static final String FOLDER_TYPE_LABEL = "folders";
    public static final String EXPERIMENT_TYPE_LABEL = "experiments";
    public static final String DATA_SET_TYPE_LABEL = "datasets";
    public static final String FILE_TYPE_LABEL = "files";

    @Override
    public @NonNull List<@NonNull String> toPathSegments(@NonNull OpenBISSftpNodeChain nodeChain) throws MalformedPathException {
        return nodeChain.nodes().stream().map( StandardPathTranslator::fromFtpNode )
                .flatMap( List::stream ).toList();
    }

    @Override
    public @NonNull OpenBISSftpNodeChain fromPathSegments(@NonNull List<@NonNull String> pathSegments) throws MalformedPathException {
        List<OpenBISSftpNode> nodes = new ArrayList<>();

        nodes.add(OpenBISSftpNodeChain.createRootNode());

        int index = 0;
        while ( index < pathSegments.size() ) {
            OpenBISSftpNode.Type type = fromTypeLabel(pathSegments.get(index));

            if (index + 1 < pathSegments.size()) {
                switch (type) {
                    case ROOT, SPACE, PROJECT, FOLDER, SAMPLE, EXPERIMENT, DATA_SET -> {
                        nodes.addAll(fromTypeAndIdentifier(type, pathSegments.get(index + 1)));
                        index += 2;
                    }
                    case AFS_FILE -> {
                        nodes.add(OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.FILE_TYPE_LABEL));
                        nodes.add(createAfsFileTypeWithPathTokens(pathSegments.subList(index + 1, pathSegments.size())));
                        index = pathSegments.size();
                    }
                }
            } else {
                nodes.add(
                    OpenBISSftpNodeChain.createSublevelNode(toTypeLabel(type))
                );
                index++;
            }
        }

        return new OpenBISSftpNodeChain(nodes);
    }

    @NonNull
    static List<OpenBISSftpNode> fromTypeAndIdentifier(@NonNull OpenBISSftpNode.Type type, @NonNull String identifier) {
        return List.of(
                OpenBISSftpNodeChain.createSublevelNode(toTypeLabel(type)),
                OpenBISSftpNode.builder()
                    .type(type)
                    .identifier(Optional.of(identifier))
                    .build()
        );
    }

    @NonNull
    static OpenBISSftpNode createAfsFileTypeWithPathTokens(@NonNull List<@NonNull String> afsPathTokens) {
        return OpenBISSftpNode.builder()
                .type(OpenBISSftpNode.Type.AFS_FILE)
                .afsFilePath(afsPathTokens)
                .build();
    }

    static String toTypeLabel(@NonNull OpenBISSftpNode.Type type) {
        return switch (type) {
            case ROOT, SUBLEVEL -> null;
            case SPACE -> SPACE_TYPE_LABEL;
            case PROJECT -> PROJECT_TYPE_LABEL;
            case FOLDER -> FOLDER_TYPE_LABEL;
            case SAMPLE -> SAMPLE_TYPE_LABEL;
            case EXPERIMENT -> EXPERIMENT_TYPE_LABEL;
            case DATA_SET -> DATA_SET_TYPE_LABEL;
            case AFS_FILE -> FILE_TYPE_LABEL;
        };
    }

    static @NonNull OpenBISSftpNode.Type fromTypeLabel(@NonNull String string) throws MalformedPathException {
        return switch (string) {
            case SPACE_TYPE_LABEL -> OpenBISSftpNode.Type.SPACE;
            case PROJECT_TYPE_LABEL -> OpenBISSftpNode.Type.PROJECT;
            case FOLDER_TYPE_LABEL -> OpenBISSftpNode.Type.FOLDER;
            case SAMPLE_TYPE_LABEL -> OpenBISSftpNode.Type.SAMPLE;
            case EXPERIMENT_TYPE_LABEL -> OpenBISSftpNode.Type.EXPERIMENT;
            case DATA_SET_TYPE_LABEL -> OpenBISSftpNode.Type.DATA_SET;
            case FILE_TYPE_LABEL -> OpenBISSftpNode.Type.AFS_FILE;
            default -> throw new MalformedPathException("Unknown label");
        };
    }

    static @NonNull List<@NonNull String> fromFtpNode(@NonNull OpenBISSftpNode node) {
        List<String> ret = new LinkedList<>();
        switch (node.getType()) {
            case ROOT -> {
                // The root-node adds no segment to the path-representation
            }
            case SPACE, PROJECT, FOLDER, SAMPLE, EXPERIMENT, DATA_SET, SUBLEVEL -> {
                node.getIdentifier().ifPresent( ret::add );
            }
            case AFS_FILE -> {
                ret.addAll( node.getAfsFilePath() );
            }
        }
        return ret;
    }
}
