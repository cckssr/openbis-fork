package ch.openbis.drive;

import ch.openbis.drive.model.Event;
import ch.openbis.drive.model.Notification;
import ch.openbis.drive.model.Settings;
import ch.openbis.drive.model.SyncJob;
import lombok.NonNull;

import java.util.List;

public interface DriveAPI {
    void setSettings(@NonNull Settings settings);
    @NonNull Settings getSettings();

    @NonNull List<SyncJob> getSyncJobs();
    void addSyncJobs(@NonNull List<SyncJob> syncJobs);
    void removeSyncJobs(@NonNull List<SyncJob> syncJobs);
    void startSyncJobs(@NonNull List<SyncJob> syncJobs);
    void stopSyncJobs(@NonNull List<SyncJob> syncJobs);

    @NonNull List<Notification> getNotifications(@NonNull Integer limit);
    @NonNull List<Event> getEvents(@NonNull Integer limit);
}
