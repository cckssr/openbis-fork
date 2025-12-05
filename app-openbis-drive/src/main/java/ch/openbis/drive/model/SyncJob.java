package ch.openbis.drive.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;

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
        return List.of(
            //LINUX HIDDEN FILES WITH "DOT"
            ".*[/\\\\]\\..*",

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
            "/var(/.*)?",

            //WINDOWS SYSTEM
            "([^:]+:)?[/\\\\]Windows([/\\\\].*)?",
            "([^:]+:)?[/\\\\]Program Files([/\\\\].*)?",
            "([^:]+:)?[/\\\\]Program Files \\(x86\\)([/\\\\].*)?",
            "([^:]+:)?[/\\\\]ProgramData([/\\\\].*)?",

            //MAC-OS SYSTEM
            "/Applications(/.*)?",
            "/Library(/.*)?",
            "/System(/.*)?",
            "/Volumes(/.*)?"
        );
    }
}
