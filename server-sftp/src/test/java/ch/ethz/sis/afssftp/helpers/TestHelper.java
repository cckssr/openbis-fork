package ch.ethz.sis.afssftp.helpers;

import ch.ethz.sis.afssftp.filesystemview.SftpNode;
import lombok.NonNull;

import java.util.*;

public class TestHelper {
    public static SftpNode createRandomNode() {
        Random random = new Random();
        SftpNode.Type randomType = SftpNode.Type.values()[
                random.nextInt(SftpNode.Type.values().length)
                ];
        return createRandomNodeOfType(randomType);
    }


    public static SftpNode createRandomNodeOfType(@NonNull SftpNode.Type type) {
        Random random = new Random();
        String identifier = switch (type) {
            case ROOT, AFS_FILE -> null;
            case SPACE, PROJECT, EXPERIMENT, SAMPLE, FOLDER, DATA_SET, SUBLEVEL -> UUID.randomUUID().toString();
        };
        List<String> afsPathSegments = switch (type) {
            case AFS_FILE -> {
                int pathLength = random.nextInt(5);
                List<String> segments = new ArrayList<>();
                for (int i=0; i<pathLength; i++) {
                    segments.add("dir" + random.nextInt(100));
                }
                yield segments;
            }
            case ROOT, SPACE, PROJECT, EXPERIMENT, SAMPLE, FOLDER, DATA_SET, SUBLEVEL -> Collections.emptyList();
        };
        return SftpNode.builder()
                .type(type)
                .identifier(Optional.ofNullable(identifier))
                .afsFilePath(afsPathSegments)
                .build();
    }
}
