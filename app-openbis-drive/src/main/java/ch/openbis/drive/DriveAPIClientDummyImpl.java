package ch.openbis.drive;

import ch.openbis.drive.model.*;
import lombok.NonNull;

import java.util.List;

public class DriveAPIClientDummyImpl implements DriveAPI, AutoCloseable {
    public DriveAPIClientDummyImpl() {}

    synchronized public void setSettings(@NonNull Settings settings) {}

    synchronized public @NonNull Settings getSettings() {
        return null;
    }

    synchronized public @NonNull List<@NonNull SyncJob> getSyncJobs() {
        return null;
    }

    synchronized public void addSyncJobs(@NonNull List<@NonNull SyncJob> syncJobs) {}

    synchronized public void removeSyncJobs(@NonNull List<@NonNull SyncJob> syncJobs) {}

    synchronized public void startSyncJobs(@NonNull List<@NonNull SyncJob> syncJobs) {}

    synchronized public void stopSyncJobs(@NonNull List<@NonNull SyncJob> syncJobs) {}

    synchronized public @NonNull List<? extends Event> getEvents(@NonNull Integer limit) {
        return null;
    }

    synchronized public @NonNull  List<Notification> getNotifications(@NonNull Integer limit) {
        return null;
    }

    synchronized public @NonNull List<@NonNull SyncJobLive> getSyncJobsLive() {
        return null;
    }

    @Override
    public void close() {}
}
