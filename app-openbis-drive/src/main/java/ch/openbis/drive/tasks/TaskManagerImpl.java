package ch.openbis.drive.tasks;

import ch.openbis.drive.conf.Configuration;
import ch.openbis.drive.db.SyncJobEventDAO;
import ch.openbis.drive.model.SyncJob;
import ch.openbis.drive.notifications.NotificationManager;
import lombok.NonNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TaskManagerImpl implements TaskManager {
    final ConcurrentHashMap<SyncJob, Timer> jobTimers = new ConcurrentHashMap<>();
    final ConcurrentHashMap<SyncJob, SyncOperation> syncOperations = new ConcurrentHashMap<>();

    private final @NonNull SyncJobEventDAO syncJobEventDAO;
    private final @NonNull NotificationManager notificationManager;
    private final @NonNull Configuration configuration;

    public TaskManagerImpl(@NonNull SyncJobEventDAO syncJobEventDAO, @NonNull NotificationManager notificationManager, @NonNull Configuration configuration) {
        this.syncJobEventDAO = syncJobEventDAO;
        this.notificationManager = notificationManager;
        this.configuration = configuration;
    }

    public synchronized void clear() {
        for (SyncJob syncJob: jobTimers.keySet()) {
            Timer timer = jobTimers.get(syncJob);
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
            Timer timer = new Timer();
            SyncJobTimeTask syncJobTimeTask = new SyncJobTimeTask(syncJob);
            timer.schedule(syncJobTimeTask, 0L, periodSeconds * 1000L);
            jobTimers.put(syncJob, timer);
        });
    }

    @Override
    public synchronized void removeSyncJobs(@NonNull List<@NonNull SyncJob> syncJobs) {
        List<SyncJob> deleted = new ArrayList<>();
        for (SyncJob syncJob: syncJobs) {
            Timer timer = jobTimers.get(syncJob);
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
        private final SyncJob syncJob;

        public SyncJobTimeTask(SyncJob syncJob) {
            this.syncJob = syncJob;
        }

        @Override
        public void run() {

            try {

                System.out.println(String.format("Sync-job %s", syncJob.getLocalDirectoryRoot()));
                SyncOperation syncTaskOperation = new SyncOperation(syncJob, syncJobEventDAO, notificationManager, configuration);
                syncOperations.put(syncJob, syncTaskOperation);
                syncTaskOperation.start();

            } catch (Exception e) {
                //TODO better logging system?
                e.printStackTrace();
            } finally {
                syncOperations.remove(syncJob);
            }

        }
    }

    //Through this method, it can be known if a synchronization-task is underway.
    //If SyncTaskOperation is present, progress-monitors can be obtained from it
    public Optional<SyncOperation> getSyncTaskOperation(@NonNull SyncJob syncJob) {
        return Optional.ofNullable(syncOperations.get(syncJob));
    }
}
