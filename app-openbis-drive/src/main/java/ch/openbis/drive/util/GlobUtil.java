package ch.openbis.drive.util;

import ch.openbis.drive.logging.Logging;
import lombok.NonNull;

import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;

public class GlobUtil {
    public static List<PathMatcher> compileIgnoredPathGlob(@NonNull String glob) throws Exception {
        List<PathMatcher> pathMatchers = new ArrayList<>();
        String trimmed = glob.trim();

        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }

        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        pathMatchers.add(FileSystems.getDefault().getPathMatcher("glob:" + trimmed));

        try {
            pathMatchers.add(FileSystems.getDefault().getPathMatcher("glob:" + trimmed + "/**"));
        } catch (Exception e) {
            Logging.tryCatchErrorInStaticMethod(GlobUtil.class, e);
        }

        if (trimmed.startsWith("**/")) {
            try {
                pathMatchers.add(FileSystems.getDefault().getPathMatcher("glob:" + trimmed.substring(3)));
                pathMatchers.add(FileSystems.getDefault().getPathMatcher("glob:" + trimmed.substring(3) + "/**"));
            } catch (Exception e) {
                Logging.tryCatchErrorInStaticMethod(GlobUtil.class, e);
            }
        }

        return pathMatchers;
    }
}
