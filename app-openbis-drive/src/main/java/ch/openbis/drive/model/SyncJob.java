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
    public enum IgnoredFilesMode { GlobalDefault, SpecificList, None }
    public enum EntityType { Sample, Experiment, Dataset }

    @NonNull private Type type;
    @NonNull private String openBisUrl;
    @NonNull private String openBisPersonalAccessToken;
    @NonNull private String entityPermId;
    private EntityType entityType;
    private boolean entityImmutable;
    @NonNull private String title;

    @NonNull private String remoteDirectoryRoot;
    @NonNull private String localDirectoryRoot;

    private boolean enabled;

    private IgnoredFilesMode ignoreFiles = IgnoredFilesMode.GlobalDefault;
    @NonNull private ArrayList<String> ignoredPathPatterns = new ArrayList<>();

    public SyncJob(@NonNull Type type, @NonNull String openBisUrl, @NonNull String openBisPersonalAccessToken, @NonNull String entityPermId, @NonNull String title, @NonNull String remoteDirectoryRoot, @NonNull String localDirectoryRoot, boolean enabled) {
        this.enabled = enabled;
        this.localDirectoryRoot = localDirectoryRoot;
        this.remoteDirectoryRoot = remoteDirectoryRoot;
        this.entityPermId = entityPermId;
        this.title = title;
        this.openBisPersonalAccessToken = openBisPersonalAccessToken;
        this.openBisUrl = openBisUrl;
        this.type = type;
    }

    static public List<String> getDefaultIgnoredPathPatterns() {
        return Stream.concat(
                getDefaultIgnoredPathPatternsForAnyPlatform().stream(),
                getDefaultIgnoredPathPatternsForCurrentPlatform(OsDetectionUtil.detectOS()).stream()
        ).toList();
    }

    static public List<String> getDefaultIgnoredPathPatternsForAnyPlatform() {
        return List.of(
                //HIDDEN FILES WITH "DOT"
                "**/.**",
                "**'**",
                "**~**",
                "**$**",
                "**%**"
        );
    }

    static public List<String> getDefaultIgnoredPathPatternsForCurrentPlatform(@NonNull OsDetectionUtil.OS operatingSystem) {
        return switch (operatingSystem) {
            case Linux -> getDefaultIgnoredPathPatternsForLinux();
            case Windows -> getDefaultIgnoredPathPatternsForWindows();
            case Mac -> getDefaultIgnoredPathPatternsForMacOS();
            case Unknown -> Collections.emptyList();
        };
    }

    static public List<String> getDefaultIgnoredPathPatternsForLinux() {
        return Collections.emptyList();
    }

    static public List<String> getDefaultIgnoredPathPatternsForWindows() {
        return List.of(
                //WINDOWS DB FILES
                "**/desktop.ini",
                "**/Desktop.ini",
                "**/IconCache.db",
                "**/thumbs.db",
                "**/Thumbs.db"
        );
    }

    static public List<String> getDefaultIgnoredPathPatternsForMacOS() {
        return List.of(
                //MAC desktop store
                "**/.DS_Store",
                "**/._.DS_Store",

                //MAC time machine markers
                "**/.com.apple.timemachine.supported-*"
        );
    }
}
