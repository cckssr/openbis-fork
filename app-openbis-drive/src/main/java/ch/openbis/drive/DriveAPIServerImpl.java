package ch.openbis.drive;

import ch.ethz.sis.shared.log.standard.LogManager;
import ch.ethz.sis.shared.log.standard.Logger;
import ch.openbis.drive.conf.Configuration;
import ch.openbis.drive.db.SyncJobEventDAO;
import ch.openbis.drive.db.SyncJobEventDAOImp;
import ch.openbis.drive.model.*;
import ch.openbis.drive.notifications.NotificationManager;
import ch.openbis.drive.notifications.NotificationManagerSqliteImpl;
import ch.openbis.drive.settings.SettingsManager;
import ch.openbis.drive.tasks.TaskManager;
import ch.openbis.drive.tasks.TaskManagerImpl;
import ch.openbis.drive.util.SystemTrayUtil;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DriveAPIServerImpl implements DriveAPI {
    private final Logger logger = LogManager.getLogger(this.getClass());

    private final SettingsManager settingsManager;
    private final NotificationManager notificationManager;
    private final TaskManager taskManager;
    final SyncJobEventDAO syncJobEventDAO;
    final SystemTrayUtil systemTrayUtil;

    @SneakyThrows
    public DriveAPIServerImpl(Configuration configuration, SystemTrayUtil systemTrayUtil) {
        syncJobEventDAO = new SyncJobEventDAOImp(configuration);
        notificationManager = new NotificationManagerSqliteImpl(configuration);
        settingsManager = new SettingsManager(configuration, syncJobEventDAO, notificationManager);
        this.systemTrayUtil = systemTrayUtil;
        taskManager = new TaskManagerImpl(syncJobEventDAO, notificationManager, settingsManager, configuration, systemTrayUtil);
    }

    public DriveAPIServerImpl(SettingsManager settingsManager,
                              NotificationManager notificationManager,
                              TaskManager taskManager, SyncJobEventDAO syncJobEventDAO,
                              SystemTrayUtil systemTrayUtil) {
        this.settingsManager = settingsManager;
        this.notificationManager = notificationManager;
        this.taskManager = taskManager;
        this.syncJobEventDAO = syncJobEventDAO;
        this.systemTrayUtil = systemTrayUtil;
    }

    synchronized public void start() {
        try {
            Settings settings = settingsManager.getSettings();
            setSettings(Optional.ofNullable(settings).orElse(Settings.defaultSettings()));
        } catch (Error e) {
            logger.catching(e);
            throw e;
        }
    }

    synchronized public void setSettings(@NonNull Settings settings) {
        try {
            settingsManager.setSettings(settings);
            taskManager.clear();
            taskManager.addSyncJobs(settings.getJobs(), settings.getSyncInterval());
        } catch (Error e) {
            logger.catching(e);
            throw e;
        }
    }

    synchronized public @NonNull Settings getSettings() {
        try {
            return settingsManager.getSettings();
        } catch (Error e) {
            logger.catching(e);
            throw e;
        }
    }

    synchronized public @NonNull List<@NonNull SyncJob> getSyncJobs() {
        try {
            return settingsManager.getSyncJobs();
        } catch (Error e) {
            logger.catching(e);
            throw e;
        }
    }

    synchronized public void addSyncJobs(@NonNull List<@NonNull SyncJob> syncJobs) {
        try {
            settingsManager.addSyncJobs(syncJobs);
            taskManager.addSyncJobs(syncJobs, getSettings().getSyncInterval());
        } catch (Error e) {
            logger.catching(e);
            throw e;
        }
    }

    synchronized public void removeSyncJobs(@NonNull List<@NonNull SyncJob> syncJobs) {
        try {
            settingsManager.removeSyncJobs(syncJobs);
            taskManager.removeSyncJobs(syncJobs);
        } catch (Error e) {
            logger.catching(e);
            throw e;
        }
    }

    synchronized public void startSyncJobs(@NonNull List<@NonNull SyncJob> syncJobs) {
        try {
            Settings currentSettings = getSettings();
            ArrayList<@NonNull SyncJob> disabledJobsOnly = syncJobs.stream().filter(syncJob -> currentSettings.getJobs().contains(syncJob) && !syncJob.isEnabled()).collect(Collectors.toCollection(ArrayList::new));
            currentSettings.getJobs().removeAll(disabledJobsOnly);
            for (SyncJob syncJob:disabledJobsOnly) {
                syncJob.setEnabled(true);
            }
            currentSettings.getJobs().addAll(disabledJobsOnly);
            setSettings(currentSettings);
        } catch (Error e) {
            logger.catching(e);
            throw e;
        }

    }

    synchronized public void stopSyncJobs(@NonNull List<@NonNull SyncJob> syncJobs) {
        try {
            Settings currentSettings = getSettings();
            ArrayList<@NonNull SyncJob> enabledJobsOnly = syncJobs.stream().filter(syncJob -> currentSettings.getJobs().contains(syncJob) && syncJob.isEnabled()).collect(Collectors.toCollection(ArrayList::new));
            currentSettings.getJobs().removeAll(enabledJobsOnly);
            for (SyncJob syncJob:enabledJobsOnly) {
                syncJob.setEnabled(false);
            }
            currentSettings.getJobs().addAll(enabledJobsOnly);
            setSettings(currentSettings);
        } catch (Error e) {
            logger.catching(e);
            throw e;
        }

    }

    @SneakyThrows
    synchronized public @NonNull List<? extends Event> getEvents(@NonNull Integer limit) {
        try {
            return syncJobEventDAO.selectMostRecent(limit);
        } catch (Error e) {
            logger.catching(e);
            throw e;
        }
    }

    synchronized public @NonNull List<Notification> getNotifications(@NonNull Integer limit) {
        try {
            return notificationManager.getNotifications(limit);
        } catch (Error e) {
            logger.catching(e);
            throw e;
        }
    }

    synchronized public @NonNull List<@NonNull SyncJobLive> getSyncJobsLive() {
        try {
            return taskManager.getSyncJobsLive();
        } catch (Error e) {
            logger.catching(e);
            throw e;
        }
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
