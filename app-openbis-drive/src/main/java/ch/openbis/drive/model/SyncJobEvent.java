package ch.openbis.drive.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Value
@Builder(toBuilder = true)
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class SyncJobEvent implements Event {

    /*
     * These 3 properties are the key of the table
     */
    @NonNull private final SyncDirection syncDirection;
    @NonNull private final String localFile;
    @NonNull private final String remoteFile;

    private final boolean directory;
    private final boolean sourceDeleted;

    /*
     * These 2 properties identify the job, so we can clear all data belonging to a job when this is deleted
     */
    @NonNull private final String entityPermId;
    @NonNull private final String localDirectoryRoot;

    /*
     * These 2 properties track the changes on the files
     */
    @NonNull private final Long sourceTimestamp;
    private final Long destinationTimestamp;

    @NonNull private final Long timestamp;
}
