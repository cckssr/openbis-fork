package ch.openbis.drive;

import ch.openbis.drive.conf.Configuration;
import ch.openbis.drive.db.SyncJobEventDAO;
import ch.openbis.drive.db.SyncJobEventDAOImp;
import ch.openbis.drive.model.*;
import ch.openbis.drive.notifications.NotificationManager;
import ch.openbis.drive.notifications.NotificationManagerSqliteImpl;
import ch.openbis.drive.settings.SettingsManager;
import ch.openbis.drive.tasks.TaskManager;
import ch.openbis.drive.tasks.TaskManagerImpl;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DriveAPIServerImpl implements DriveAPI {
    private final SettingsManager settingsManager;
    private final NotificationManager notificationManager;
    private final TaskManager taskManager;
    final SyncJobEventDAO syncJobEventDAO;

    @SneakyThrows
    public DriveAPIServerImpl(Configuration configuration) {
        syncJobEventDAO = new SyncJobEventDAOImp(configuration);
        notificationManager = new NotificationManagerSqliteImpl(configuration);
        settingsManager = new SettingsManager(configuration, syncJobEventDAO, notificationManager);
        taskManager = new TaskManagerImpl(syncJobEventDAO, notificationManager);
    }

    synchronized public void setSettings(@NonNull Settings settings) {
        settingsManager.setSettings(settings);
        taskManager.clear();
        taskManager.addSyncJobs(settings.getJobs(), settings.getSyncInterval());
    }

    synchronized public @NonNull Settings getSettings() {
        return settingsManager.getSettings();
    }

    synchronized public @NonNull List<@NonNull SyncJob> getSyncJobs() {
        return settingsManager.getSyncJobs();
    }

    synchronized public void addSyncJobs(@NonNull List<@NonNull SyncJob> syncJobs) {
        settingsManager.addSyncJobs(syncJobs);
        for (SyncJob syncJob:syncJobs) {
            if (syncJob.isEnabled()) {
                taskManager.addSyncJobs(syncJobs, getSettings().getSyncInterval());
            }
        }
    }

    synchronized public void removeSyncJobs(@NonNull List<@NonNull SyncJob> syncJobs) {
        settingsManager.removeSyncJobs(syncJobs);
        for (SyncJob syncJob:syncJobs) {
            if (syncJob.isEnabled()) {
                taskManager.removeSyncJobs(syncJobs);
            }
        }
    }

    synchronized public void startSyncJobs(@NonNull List<@NonNull SyncJob> syncJobs) {
        ArrayList<@NonNull SyncJob> disabledJobsOnly = syncJobs.stream().filter(syncJob -> !syncJob.isEnabled()).collect(Collectors.toCollection(ArrayList::new));
        removeSyncJobs(disabledJobsOnly);
        for (SyncJob syncJob:disabledJobsOnly) {
            syncJob.setEnabled(true);
        }
        addSyncJobs(disabledJobsOnly);
    }

    synchronized public void stopSyncJobs(@NonNull List<@NonNull SyncJob> syncJobs) {
        ArrayList<@NonNull SyncJob> enabledJobsOnly = syncJobs.stream().filter(SyncJob::isEnabled).collect(Collectors.toCollection(ArrayList::new));
        removeSyncJobs(enabledJobsOnly);
        for (SyncJob syncJob:enabledJobsOnly) {
            syncJob.setEnabled(false);
        }
        addSyncJobs(enabledJobsOnly);
    }

    @SneakyThrows
    synchronized public @NonNull List<? extends Event> getEvents(@NonNull Integer limit) {
        return syncJobEventDAO.selectMostRecent(limit);
    }

    synchronized public @NonNull List<Notification> getNotifications(@NonNull Integer limit) {
        return notificationManager.getNotifications(limit);
    }

    // NON-PUBLIC METHODS

    synchronized void addNotifications(@NonNull List<@NonNull Notification> notifications) {
        notificationManager.addNotifications(notifications);
    }

    synchronized void removeNotifications(@NonNull List<@NonNull Notification> notifications) {
        notificationManager.removeNotifications(notifications);
    }

    synchronized void clearNotifications() {
        notificationManager.clearAllNotifications();
    }
}
