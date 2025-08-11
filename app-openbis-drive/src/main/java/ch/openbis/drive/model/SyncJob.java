package ch.openbis.drive.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

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
}
