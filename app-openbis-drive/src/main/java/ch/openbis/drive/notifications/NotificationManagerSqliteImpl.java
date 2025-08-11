package ch.openbis.drive.notifications;

import ch.openbis.drive.conf.Configuration;
import ch.openbis.drive.db.NotificationDAO;
import ch.openbis.drive.db.NotificationDAOImpl;
import ch.openbis.drive.model.Notification;
import ch.openbis.drive.model.SyncJob;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.util.List;

public class NotificationManagerSqliteImpl implements NotificationManager {
    private final NotificationDAO notificationDAO;

    public NotificationManagerSqliteImpl(Configuration configuration) throws Exception {
        notificationDAO = new NotificationDAOImpl(configuration);
    }

    @Override
    @SneakyThrows
    synchronized public void addNotifications(@NonNull List<@NonNull Notification> notifications) {
        for(Notification notification : notifications) {
            notificationDAO.insertOrUpdate(notification);
            if (notification.getType() != Notification.Type.Conflict) {
                notificationDAO.removeOldEntriesByLocalDirectoryAndType(notification.getLocalDirectory(), notification.getType(), 10);
            }
        }
    }

    @Override
    @SneakyThrows
    synchronized public List<Notification> getNotifications(@NonNull Integer limit) {
        return notificationDAO.selectLast(limit);
    }

    @Override
    @SneakyThrows
    synchronized public void removeNotifications(@NonNull List<@NonNull Notification> notifications) {
        for(Notification notification : notifications) {
            notificationDAO.removeByPrimaryKey(notification.getType(), notification.getLocalDirectory(), notification.getLocalFile(), notification.getRemoteFile());
        }
    }

    @Override
    @SneakyThrows
    synchronized public void clearNotificationsForSyncJob(@NonNull SyncJob syncJob) {
        notificationDAO.removeByLocalDirectory(syncJob.getLocalDirectoryRoot());
    }

    @SneakyThrows
    synchronized public void clearAllNotifications() {
        notificationDAO.clearAll();
    }
}
