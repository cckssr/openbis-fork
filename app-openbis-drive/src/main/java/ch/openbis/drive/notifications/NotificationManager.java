package ch.openbis.drive.notifications;

import ch.openbis.drive.model.Notification;
import ch.openbis.drive.model.SyncJob;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.util.List;

public interface NotificationManager {
    void addNotifications(@NonNull List<@NonNull Notification> notifications);
    List<Notification> getNotifications(@NonNull Integer limit);
    void removeNotifications(@NonNull List<@NonNull Notification> notifications);
    Notification getSpecificNotification(@NonNull Notification notification);
    List<Notification> getConflictNotifications(@NonNull SyncJob syncJob, @NonNull Integer limit);

    void clearNotificationsForSyncJob(@NonNull SyncJob syncJob);
    void clearAllNotifications();
}
