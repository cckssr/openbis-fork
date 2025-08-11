package ch.openbis.drive;

import ch.openbis.drive.model.Event;
import ch.openbis.drive.model.Notification;
import ch.openbis.drive.model.Settings;
import ch.openbis.drive.model.SyncJob;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public interface DriveAPI {
    void setSettings(@NonNull Settings settings);
    @NonNull Settings getSettings();

    @NonNull List<@NonNull SyncJob> getSyncJobs();
    void addSyncJobs(List<@NonNull SyncJob> syncJobs);
    void removeSyncJobs(@NonNull List<@NonNull SyncJob> syncJobs);
    void startSyncJobs(@NonNull List<@NonNull SyncJob> syncJobs);
    void stopSyncJobs(@NonNull List<@NonNull SyncJob> syncJobs);

    @NonNull List<? extends Event> getEvents(@NonNull Integer limit);
    @NonNull List<Notification> getNotifications(@NonNull Integer limit);
}
