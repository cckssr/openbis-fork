package ch.openbis.drive.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class SyncJobLive {
    @NonNull private final String localDirectory;

    private final boolean uploading;
    private final boolean downloading;

    private final long totalUpload;
    private final long totalDownload;

    private final long currentUpload;
    private final long currentDownload;
}
