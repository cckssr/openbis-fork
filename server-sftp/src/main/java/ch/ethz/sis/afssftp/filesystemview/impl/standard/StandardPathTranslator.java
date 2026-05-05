package ch.ethz.sis.afssftp.filesystemview.impl.standard;

import ch.ethz.sis.afssftp.filesystemview.FtpPathTranslator;
import ch.ethz.sis.afssftp.filesystemview.SftpNode;
import ch.ethz.sis.afssftp.filesystemview.SftpNodeChain;
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
    public @NonNull List<@NonNull String> toPathSegments(@NonNull SftpNodeChain nodeChain) throws MalformedPathException {
        return nodeChain.nodes().stream().map( StandardPathTranslator::fromFtpNode )
                .flatMap( List::stream ).toList();
    }

    @Override
    public @NonNull SftpNodeChain fromPathSegments(@NonNull List<@NonNull String> pathSegments) throws MalformedPathException {
        List<SftpNode> nodes = new ArrayList<>();

        nodes.add(SftpNodeChain.createRootNode());

        int index = 0;
        while ( index < pathSegments.size() ) {
            SftpNode.Type type = fromTypeLabel(pathSegments.get(index));

            if (index + 1 < pathSegments.size()) {
                switch (type) {
                    case ROOT, SPACE, PROJECT, FOLDER, SAMPLE, EXPERIMENT, DATA_SET -> {
                        nodes.addAll(fromTypeAndIdentifier(type, pathSegments.get(index + 1)));
                        index += 2;
                    }
                    case AFS_FILE -> {
                        nodes.add(SftpNodeChain.createSublevelNode(StandardPathTranslator.FILE_TYPE_LABEL));
                        nodes.add(createAfsFileTypeWithPathTokens(pathSegments.subList(index + 1, pathSegments.size())));
                        index = pathSegments.size();
                    }
                }
            } else {
                nodes.add(
                    SftpNodeChain.createSublevelNode(toTypeLabel(type))
                );
                index++;
            }
        }

        return new SftpNodeChain(nodes);
    }

    @NonNull
    static List<SftpNode> fromTypeAndIdentifier(@NonNull SftpNode.Type type, @NonNull String identifier) {
        return List.of(
                SftpNodeChain.createSublevelNode(toTypeLabel(type)),
                SftpNode.builder()
                    .type(type)
                    .identifier(Optional.of(identifier))
                    .build()
        );
    }

    @NonNull
    static SftpNode createAfsFileTypeWithPathTokens(@NonNull List<@NonNull String> afsPathTokens) {
        return SftpNode.builder()
                .type(SftpNode.Type.AFS_FILE)
                .afsFilePath(afsPathTokens)
                .build();
    }

    static String toTypeLabel(@NonNull SftpNode.Type type) {
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

    static @NonNull SftpNode.Type fromTypeLabel(@NonNull String string) throws MalformedPathException {
        return switch (string) {
            case SPACE_TYPE_LABEL -> SftpNode.Type.SPACE;
            case PROJECT_TYPE_LABEL -> SftpNode.Type.PROJECT;
            case FOLDER_TYPE_LABEL -> SftpNode.Type.FOLDER;
            case SAMPLE_TYPE_LABEL -> SftpNode.Type.SAMPLE;
            case EXPERIMENT_TYPE_LABEL -> SftpNode.Type.EXPERIMENT;
            case DATA_SET_TYPE_LABEL -> SftpNode.Type.DATA_SET;
            case FILE_TYPE_LABEL -> SftpNode.Type.AFS_FILE;
            default -> throw new MalformedPathException("Unknown label");
        };
    }

    static @NonNull List<@NonNull String> fromFtpNode(@NonNull SftpNode node) {
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
