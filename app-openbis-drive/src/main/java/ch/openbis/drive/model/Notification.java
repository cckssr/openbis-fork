package ch.openbis.drive.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.Instant;

@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class Notification {
    public enum Type { Conflict, JobStopped, JobException }

    // Primary key
    @NonNull private final Type type;
    @NonNull private final String localDirectory;
    private final String localFile;
    private final String remoteFile;
    //

    @NonNull private final String message;
    @NonNull private final Long timestamp;
}
