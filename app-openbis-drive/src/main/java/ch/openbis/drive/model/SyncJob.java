package ch.openbis.drive.model;

import ch.openbis.drive.util.OsDetectionUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

@Data
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class SyncJob {
    public enum Type { Bidirectional, Upload, Download }

    @NonNull private Type type;
    @NonNull private String openBisUrl;
    @NonNull private String openBisPersonalAccessToken;
    @NonNull private String entityPermId;

    @NonNull private String remoteDirectoryRoot;
    @NonNull private String localDirectoryRoot;

    private boolean enabled;

    private boolean skipHiddenFiles = true;
    @NonNull private ArrayList<String> hiddenPathPatterns = new ArrayList<>(getDefaultHiddenPathPatterns());

    public SyncJob(@NonNull Type type, @NonNull String openBisUrl, @NonNull String openBisPersonalAccessToken, @NonNull String entityPermId, @NonNull String remoteDirectoryRoot, @NonNull String localDirectoryRoot, boolean enabled) {
        this.enabled = enabled;
        this.localDirectoryRoot = localDirectoryRoot;
        this.remoteDirectoryRoot = remoteDirectoryRoot;
        this.entityPermId = entityPermId;
        this.openBisPersonalAccessToken = openBisPersonalAccessToken;
        this.openBisUrl = openBisUrl;
        this.type = type;
    }

    static public List<String> getDefaultHiddenPathPatterns() {
        return Stream.concat(
                getDefaultHiddenPathPatternsForAnyPlatform().stream(),
                getDefaultHiddenPathPatternsForCurrentPlatform(OsDetectionUtil.detectOS()).stream()
        ).toList();
    }

    static public List<String> getDefaultHiddenPathPatternsForAnyPlatform() {
        return List.of(
                //HIDDEN FILES WITH "DOT"
                ".*[/\\\\]\\..*",
                ".*'.*",
                ".*~.*",
                ".*\\$.*",
                ".*%.*"
        );
    }

    static public List<String> getDefaultHiddenPathPatternsForCurrentPlatform(@NonNull OsDetectionUtil.OS operatingSystem) {
        return switch (operatingSystem) {
            case Linux -> getDefaultHiddenPathPatternsForLinux();
            case Windows -> getDefaultHiddenPathPatternsForWindows();
            case Mac -> getDefaultHiddenPathPatternsForMacOS();
            case Unknown -> Collections.emptyList();
        };
    }

    static public List<String> getDefaultHiddenPathPatternsForLinux() {
        return List.of(
                //LINUX SYSTEM
                "/bin(/.*)?",
                "/boot(/.*)?",
                "/dev(/.*)?",
                "/etc(/.*)?",
                "/lib(/.*)?",
                "/media(/.*)?",
                "/mnt(/.*)?",
                "/opt(/.*)?",
                "/proc(/.*)?",
                "/root(/.*)?",
                "/run(/.*)?",
                "/sbin(/.*)?",
                "/snap(/.*)?",
                "/srv(/.*)?",
                "/sys(/.*)?",
                "/tmp(/.*)?",
                "/usr(/.*)?",
                "/var(/.*)?"
        );
    }

    static public List<String> getDefaultHiddenPathPatternsForWindows() {
        return List.of(
                //WINDOWS DB FILES
                ".*[/\\\\]desktop\\.ini",
                ".*[/\\\\]IconCache\\.db",
                ".*[/\\\\]thumbs\\.db",

                //WINDOWS SYSTEM
                "([^:]+:)?[/\\\\]Windows([/\\\\].*)?",
                "([^:]+:)?[/\\\\]Program Files([/\\\\].*)?",
                "([^:]+:)?[/\\\\]Program Files \\(x86\\)([/\\\\].*)?",
                "([^:]+:)?[/\\\\]ProgramData([/\\\\].*)?"
        );
    }

    static public List<String> getDefaultHiddenPathPatternsForMacOS() {
        return List.of(
                //MAC-OS SYSTEM
                "/Applications(/.*)?",
                "/Library(/.*)?",
                "/System(/.*)?",
                "/Volumes(/.*)?"
        );
    }
}
