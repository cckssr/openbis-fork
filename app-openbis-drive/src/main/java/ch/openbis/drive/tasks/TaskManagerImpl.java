package ch.openbis.drive.tasks;

import ch.openbis.drive.conf.Configuration;
import ch.openbis.drive.db.SyncJobEventDAO;
import ch.openbis.drive.model.SyncJob;
import ch.openbis.drive.notifications.NotificationManager;
import ch.openbis.drive.settings.SettingsManager;
import ch.openbis.drive.util.DirectoryWatch;
import lombok.NonNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class TaskManagerImpl implements TaskManager {
    final ConcurrentHashMap<SyncJob, CloseableSyncTimer> jobTimers = new ConcurrentHashMap<>();
    final ConcurrentHashMap<SyncJob, SyncOperation> syncOperations = new ConcurrentHashMap<>();

    private final @NonNull SyncJobEventDAO syncJobEventDAO;
    private final @NonNull NotificationManager notificationManager;
    private final @NonNull SettingsManager settingsManager;
    private final @NonNull Configuration configuration;

    public TaskManagerImpl(@NonNull SyncJobEventDAO syncJobEventDAO,
                           @NonNull NotificationManager notificationManager,
                           @NonNull SettingsManager settingsManager,
                           @NonNull Configuration configuration) {
        this.syncJobEventDAO = syncJobEventDAO;
        this.notificationManager = notificationManager;
        this.settingsManager = settingsManager;
        this.configuration = configuration;
    }

    public synchronized void clear() {
        for (SyncJob syncJob: jobTimers.keySet()) {
            CloseableSyncTimer timer = jobTimers.get(syncJob);
            timer.cancel();
            SyncOperation syncOperation = syncOperations.remove(syncJob);
            if (syncOperation != null) {
                syncOperation.stop();
            }
        }
        syncOperations.clear();
        jobTimers.clear();
    }

    @Override
    public synchronized void addSyncJobs(@NonNull List<@NonNull SyncJob> syncJobs, int periodSeconds) {
        syncJobs.stream().filter( SyncJob::isEnabled ).forEach( syncJob -> {
            DirectoryWatch directoryWatch = new DirectoryWatch(
                    syncJob.getLocalDirectoryRoot());
            CloseableSyncTimer timer = new CloseableSyncTimer(directoryWatch);

            SyncJobTimeTask syncJobTimeTask = new SyncJobTimeTask(syncJob, timer);

            timer.schedule(syncJobTimeTask, 0L, periodSeconds * 1000L);
            jobTimers.put(syncJob, timer);
        });
    }

    @Override
    public synchronized void removeSyncJobs(@NonNull List<@NonNull SyncJob> syncJobs) {
        List<SyncJob> deleted = new ArrayList<>();
        for (SyncJob syncJob: syncJobs) {
            CloseableSyncTimer timer = jobTimers.get(syncJob);
            if (timer != null ) {
                timer.cancel();
            }
            SyncOperation syncOperation = syncOperations.remove(syncJob);
            if (syncOperation != null) {
                syncOperation.stop();
            }
            deleted.add(syncJob);
        }
        jobTimers.keySet().removeAll(deleted);
    }

    private class SyncJobTimeTask extends TimerTask {
        static final long GRACE_PERIOD_MILLISECONDS = 15000;
        enum TriggeringCause {
            PERIODIC_CHECK,
            FILESYSTEM_EVENT,
            FILESYSTEM_EVENT_CHECK_AFTER_GRACE_PERIOD
        }

        private final SyncJob syncJob;
        private final CloseableSyncTimer timer;
        private final DirectoryWatch directoryWatch;

        public SyncJobTimeTask(SyncJob syncJob, CloseableSyncTimer timer) {
            this.syncJob = syncJob;
            this.timer = timer;
            this.directoryWatch = timer.getDirectoryWatch();
        }

        @Override
        public void run() {
            tryToInsertAndStartNewSyncOperation(TriggeringCause.PERIODIC_CHECK);
        }

        private Void tryToInsertAndStartNewSyncOperation(@NonNull TriggeringCause triggeringCause) {
            System.out.println(String.format("Sync-job %s from %s", syncJob.getLocalDirectoryRoot(), triggeringCause));

            SyncOperation syncTaskOperation;
            try {
                syncTaskOperation = new SyncOperation(syncJob, syncJobEventDAO, notificationManager, configuration, settingsManager.getSettings());
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
            if (syncOperations.putIfAbsent(syncJob, syncTaskOperation) == null) {
                try {
                    directoryWatch.close();

                    if (canStartImmediately(triggeringCause)) {
                        syncTaskOperation.start();
                    } else {
                        rescheduleSyncOperationAfterGracePeriod();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    tryToRestartDirectoryWatch();
                    syncOperations.remove(syncJob);
                }
            }
            return null;
        }

        private boolean canStartImmediately(@NonNull TriggeringCause triggeringCause) {
            return switch (triggeringCause) {
                case PERIODIC_CHECK -> true;
                case FILESYSTEM_EVENT -> false;
                case FILESYSTEM_EVENT_CHECK_AFTER_GRACE_PERIOD ->
                        System.currentTimeMillis() - directoryWatch.getLastEventTs() > GRACE_PERIOD_MILLISECONDS - 1000;
            };
        }

        private Void rescheduleSyncOperationFromFileSystemEvent() {
            if ( !timer.isCancelled() ) {
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        tryToInsertAndStartNewSyncOperation(TriggeringCause.FILESYSTEM_EVENT);
                    }
                }, 0);
            }
            return null;
        }

        private Void rescheduleSyncOperationAfterGracePeriod() {
            if ( !timer.isCancelled() ) {
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        tryToInsertAndStartNewSyncOperation(TriggeringCause.FILESYSTEM_EVENT_CHECK_AFTER_GRACE_PERIOD);
                    }
                }, GRACE_PERIOD_MILLISECONDS);
            }
            return null;
        }

        private void tryToRestartDirectoryWatch() {
            try {
                if (syncJob.getType() == SyncJob.Type.Upload || syncJob.getType() == SyncJob.Type.Bidirectional) {
                    directoryWatch.start(this::rescheduleSyncOperationFromFileSystemEvent);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static class CloseableSyncTimer extends Timer {
        final AtomicBoolean cancelled = new AtomicBoolean(false);
        final DirectoryWatch directoryWatch;

        public CloseableSyncTimer(@NonNull DirectoryWatch directoryWatch) {
            this.directoryWatch = directoryWatch;
        }

        public DirectoryWatch getDirectoryWatch() {
            return directoryWatch;
        }

        @Override
        public void cancel() {
            this.cancelled.set(true);
            try {
                this.directoryWatch.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            super.cancel();
        }

        public boolean isCancelled() {
            return cancelled.get();
        }
    }

    //Through this method, it can be known if a synchronization-task is underway.
    //If SyncTaskOperation is present, progress-monitors can be obtained from it
    public Optional<SyncOperation> getSyncTaskOperation(@NonNull SyncJob syncJob) {
        return Optional.ofNullable(syncOperations.get(syncJob));
    }
}
