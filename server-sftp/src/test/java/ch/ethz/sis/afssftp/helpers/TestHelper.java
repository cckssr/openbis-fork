package ch.ethz.sis.afssftp.helpers;

import ch.ethz.sis.afssftp.filesystemview.OpenBISSftpNode;
import lombok.NonNull;

import java.util.*;

public class TestHelper {
    public static OpenBISSftpNode createRandomNode() {
        Random random = new Random();
        OpenBISSftpNode.Type randomType = OpenBISSftpNode.Type.values()[
                random.nextInt(OpenBISSftpNode.Type.values().length)
                ];
        return createRandomNodeOfType(randomType);
    }


    public static OpenBISSftpNode createRandomNodeOfType(@NonNull OpenBISSftpNode.Type type) {
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
        return OpenBISSftpNode.builder()
                .type(type)
                .identifier(Optional.ofNullable(identifier))
                .afsFilePath(afsPathSegments)
                .build();
    }
}
