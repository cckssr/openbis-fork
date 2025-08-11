package ch.openbis.drive.tasks;

import ch.openbis.drive.model.SyncJob;
import lombok.NonNull;

import java.util.List;

public interface TaskManager {
    void clear();

    /*
        These are used not only to add/remove, but also to start/stop by the client
        Because of this they cannot decide if the task has been deleted from the settings
        They should not be clearing the internal database
     */

    void addSyncJobs(@NonNull List<@NonNull SyncJob> syncJobs, int periodSeconds);
    void removeSyncJobs(@NonNull List<@NonNull SyncJob> syncJobs);
}
