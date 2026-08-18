package ch.openbis.drive.tasks;

import ch.openbis.drive.DriveTestCase;
import ch.openbis.drive.conf.Configuration;
import ch.openbis.drive.db.SyncJobEventDAO;
import ch.openbis.drive.model.SyncJob;
import ch.openbis.drive.notifications.NotificationManager;
import ch.openbis.drive.settings.SettingsManager;
import ch.openbis.drive.util.SystemTrayUtil;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@RunWith(JUnit4.class)
public class TaskManagerImplTest extends DriveTestCase {

    private final SyncJobEventDAO syncJobEventDAO = Mockito.mock(SyncJobEventDAO.class);
    private final NotificationManager notificationManager = Mockito.mock(NotificationManager.class);
    private final SettingsManager settingsManager = Mockito.mock(SettingsManager.class);
    private final Configuration configuration = new Configuration(Path.of("/fake-local-app-directory"));
    private final SystemTrayUtil systemTrayUtil = Mockito.mock(SystemTrayUtil.class);
    private final TaskManagerImpl taskManagerImpl = new TaskManagerImpl(syncJobEventDAO, notificationManager, settingsManager, configuration, systemTrayUtil);


    @Test
    public void testClear() {
        List<SyncJob> syncJobList = List.of(
                new SyncJob(SyncJob.Type.Bidirectional, "url", "token", "uuid", "title", "/remotedir1", "/localdir1", true),
                new SyncJob(SyncJob.Type.Bidirectional, "url", "token", "uuid", "title", "/remotedir2", "/localdir2", false),
                new SyncJob(SyncJob.Type.Bidirectional, "url", "token", "uuid", "title", "/remotedir3", "/localdir3", true)
        );

        taskManagerImpl.addSyncJobs(syncJobList, 1);

        Assert.assertEquals(
                Set.of(new SyncJob(SyncJob.Type.Bidirectional, "url", "token", "uuid", "title", "/remotedir1", "/localdir1", true),
                        new SyncJob(SyncJob.Type.Bidirectional, "url", "token", "uuid", "title", "/remotedir3", "/localdir3", true)),
                taskManagerImpl.jobTimers.keySet());

        taskManagerImpl.clear();

        Assert.assertEquals(
                Collections.emptySet(),
                taskManagerImpl.jobTimers.keySet());
    }

    @Test
    public void testAddSyncJobs() {
        List<SyncJob> syncJobList = List.of(
                new SyncJob(SyncJob.Type.Bidirectional, "url", "token", "uuid", "title", "/remotedir1", "/localdir1", true),
                new SyncJob(SyncJob.Type.Bidirectional, "url", "token", "uuid", "title", "/remotedir2", "/localdir2", false),
                new SyncJob(SyncJob.Type.Bidirectional, "url", "token", "uuid", "title", "/remotedir3", "/localdir3", true)
        );

        taskManagerImpl.addSyncJobs(syncJobList, 1);

        Assert.assertEquals(
                Set.of(new SyncJob(SyncJob.Type.Bidirectional, "url", "token", "uuid", "title", "/remotedir1", "/localdir1", true),
                new SyncJob(SyncJob.Type.Bidirectional, "url", "token", "uuid", "title", "/remotedir3", "/localdir3", true)),
                taskManagerImpl.jobTimers.keySet());
    }

    @Test
    public void testRemoveSyncJobs() {
        List<SyncJob> syncJobList = List.of(
                new SyncJob(SyncJob.Type.Bidirectional, "url", "token", "uuid", "title", "/remotedir1", "/localdir1", true),
                new SyncJob(SyncJob.Type.Bidirectional, "url", "token", "uuid", "title", "/remotedir2", "/localdir2", false),
                new SyncJob(SyncJob.Type.Bidirectional, "url", "token", "uuid", "title", "/remotedir3", "/localdir3", true)
        );

        taskManagerImpl.addSyncJobs(syncJobList, 1);

        Assert.assertEquals(
                Set.of(new SyncJob(SyncJob.Type.Bidirectional, "url", "token", "uuid", "title", "/remotedir1", "/localdir1", true),
                        new SyncJob(SyncJob.Type.Bidirectional, "url", "token", "uuid", "title", "/remotedir3", "/localdir3", true)),
                taskManagerImpl.jobTimers.keySet());

        taskManagerImpl.removeSyncJobs(List.of(new SyncJob(SyncJob.Type.Bidirectional, "url", "token", "uuid", "title", "/remotedir1", "/localdir1", true)));

        Assert.assertEquals(
                Set.of(new SyncJob(SyncJob.Type.Bidirectional, "url", "token", "uuid", "title", "/remotedir3", "/localdir3", true)),
                taskManagerImpl.jobTimers.keySet());
    }
}