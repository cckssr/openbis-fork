package ch.openbis.drive;

import ch.ethz.sis.afsapi.dto.File;
import ch.openbis.drive.conf.Configuration;
import ch.openbis.drive.db.SyncJobEventDAO;
import ch.openbis.drive.db.SyncJobEventDAOImp;
import ch.openbis.drive.model.*;
import ch.openbis.drive.notifications.NotificationManager;
import ch.openbis.drive.notifications.NotificationManagerSqliteImpl;
import ch.openbis.drive.settings.SettingsManager;
import ch.openbis.drive.tasks.TaskManager;
import ch.openbis.drive.tasks.TaskManagerImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@RunWith(JUnit4.class)
public class DriveAPIServerImplTest {

    private final Configuration configuration;
    private final SyncJobEventDAO syncJobEventDAO;
    private final NotificationManager notificationManager;
    private final SettingsManager settingsManager;
    private final TaskManager taskManager;
    private final DriveAPIServerImpl driveAPIServerImpl;

    public DriveAPIServerImplTest() throws Exception {
        configuration = new Configuration(Path.of(this.getClass().getClassLoader().getResource("placeholder.txt").getPath()).getParent().resolve("drive-api-server-impl-test"));
        Files.createDirectories(configuration.getLocalAppStateDirectory());
        syncJobEventDAO = new SyncJobEventDAOImp(configuration);
        notificationManager = new NotificationManagerSqliteImpl(configuration);
        settingsManager = Mockito.mock(SettingsManager.class);
        taskManager = Mockito.spy(new TaskManagerImpl(syncJobEventDAO, notificationManager, configuration));
        driveAPIServerImpl = Mockito.spy(new DriveAPIServerImpl(settingsManager, notificationManager, taskManager, syncJobEventDAO));
    }

    @Before
    synchronized public void before() {
        Mockito.reset(settingsManager);
        Mockito.reset(taskManager);
    }

    @Test
    synchronized public void setAndGetSettingsTest() {
        Settings settings1 = Settings.defaultSettings();

        SyncJob syncJob1 = new SyncJob(SyncJob.Type.Bidirectional, "url1", "token1", "id1", "/remotedir1", "/localdir1", false);
        SyncJob syncJob2 = new SyncJob(SyncJob.Type.Bidirectional, "url2", "token2", "id2", "/remotedir2", "/localdir2", true);
        Settings settings2 = new Settings(true, "it", 15, new ArrayList<>(List.of(syncJob1, syncJob2)));

        driveAPIServerImpl.setSettings(settings1);
        Mockito.verify(settingsManager, Mockito.times(1)).setSettings(settings1);
        Mockito.verify(taskManager, Mockito.times(1)).clear();
        Mockito.verify(taskManager, Mockito.times(1)).addSyncJobs(settings1.getJobs(), 120);

        Mockito.clearInvocations(settingsManager);
        Mockito.clearInvocations(taskManager);

        driveAPIServerImpl.setSettings(settings2);
        Mockito.verify(settingsManager, Mockito.times(1)).setSettings(settings2);
        Mockito.verify(taskManager, Mockito.times(1)).clear();
        Mockito.verify(taskManager, Mockito.times(1)).addSyncJobs(settings2.getJobs(), 15);

        Mockito.doReturn(settings2).when(settingsManager).getSettings();
        Assert.assertEquals(settings2, driveAPIServerImpl.getSettings());
        Mockito.verify(settingsManager, Mockito.times(1)).getSettings();
    }

    @Test
    synchronized public void getSyncJobsTest() {
        Settings settings1 = Settings.defaultSettings();

        SyncJob syncJob1 = new SyncJob(SyncJob.Type.Bidirectional, "url1", "token1", "id1", "remotedir1", "/localdir1", true);
        SyncJob syncJob2 = new SyncJob(SyncJob.Type.Bidirectional, "url2", "token2", "id2", "remotedir2", "/localdir2", false);
        Settings settings2 = new Settings(true, "it", 15, new ArrayList<>(List.of(syncJob1, syncJob2)));

        Mockito.doReturn(settings1.getJobs()).when(settingsManager).getSyncJobs();
        Assert.assertEquals(Collections.emptyList(), driveAPIServerImpl.getSyncJobs());

        Mockito.doReturn(settings2.getJobs()).when(settingsManager).getSyncJobs();
        Assert.assertEquals(List.of(syncJob1, syncJob2), driveAPIServerImpl.getSyncJobs());
    }

    @Test
    synchronized public void addSyncJobTest() {
        Settings settings = new Settings(true, "it", 15, new ArrayList<>());
        Mockito.doReturn(settings).when(settingsManager).getSettings();

        SyncJob syncJob1 = new SyncJob(SyncJob.Type.Bidirectional, "url1", "token1", "id1", "/remotedir1", "/localdir1", false);
        driveAPIServerImpl.addSyncJobs(List.of(syncJob1));
        Mockito.verify(settingsManager, Mockito.times(1)).addSyncJobs(List.of(syncJob1));
        Mockito.verify(taskManager, Mockito.times(1)).addSyncJobs(List.of(syncJob1), 15);

        Mockito.clearInvocations(settingsManager);
        Mockito.clearInvocations(taskManager);

        SyncJob syncJob2 = new SyncJob(SyncJob.Type.Bidirectional, "url2", "token2", "id2", "remotedir2", "/localdir2", false);
        SyncJob syncJob3 = new SyncJob(SyncJob.Type.Bidirectional, "url3", "token3", "id3", "/remotedir3", "/localdir3", true);
        driveAPIServerImpl.addSyncJobs(List.of(syncJob2, syncJob3));
        Mockito.verify(settingsManager, Mockito.times(1)).addSyncJobs(List.of(syncJob2, syncJob3));
        Mockito.verify(taskManager, Mockito.times(1)).addSyncJobs(List.of(syncJob2, syncJob3), 15);
    }

    @Test(expected = IllegalArgumentException.class)
    synchronized public void addSyncJobWithDuplicatelocalDirTest() {
        DriveAPIServerImpl driveAPIServerWithRealSettingsManager = new DriveAPIServerImpl(new SettingsManager(configuration, syncJobEventDAO, notificationManager), notificationManager, taskManager, syncJobEventDAO);
        driveAPIServerWithRealSettingsManager.setSettings(Settings.defaultSettings());

        SyncJob syncJob1 = new SyncJob(SyncJob.Type.Bidirectional, "url1", "token1", "id1", "/remotedir1", "/localdir1", false);
        driveAPIServerWithRealSettingsManager.addSyncJobs(List.of(syncJob1));

        Assert.assertEquals(List.of(syncJob1), driveAPIServerWithRealSettingsManager.getSettings().getJobs());

        SyncJob syncJob2 = new SyncJob(SyncJob.Type.Bidirectional, "url2", "token2", "id2", "/remotedir2", "/localdir1", true);
        driveAPIServerWithRealSettingsManager.addSyncJobs(List.of(syncJob2));
    }

    @Test
    synchronized public void removeSyncJobTest() {

        SyncJob syncJob1 = new SyncJob(SyncJob.Type.Bidirectional, "url1", "token1", "id1", "/remotedir1", "/localdir1", true);
        SyncJob syncJob2 = new SyncJob(SyncJob.Type.Bidirectional, "url2", "token2", "id2", "/remotedir2", "/localdir2", false);
        SyncJob syncJob3 = new SyncJob(SyncJob.Type.Bidirectional, "url3", "token3", "id3", "/remotedir3", "/localdir3", true);


        driveAPIServerImpl.removeSyncJobs(List.of(syncJob1));
        Mockito.verify(settingsManager, Mockito.times(1)).removeSyncJobs(List.of(syncJob1));
        Mockito.verify(taskManager, Mockito.times(1)).removeSyncJobs(List.of(syncJob1));

        Mockito.clearInvocations(settingsManager);
        Mockito.clearInvocations(taskManager);

        driveAPIServerImpl.removeSyncJobs(List.of(syncJob2, syncJob3));
        Mockito.verify(settingsManager, Mockito.times(1)).removeSyncJobs(List.of(syncJob2, syncJob3));
        Mockito.verify(taskManager, Mockito.times(1)).removeSyncJobs(List.of(syncJob2, syncJob3));
    }

    @Test
    synchronized public void startSyncJobTest() {
        SyncJob syncJob1 = new SyncJob(SyncJob.Type.Bidirectional, "url1", "token1", "id1", "/remotedir1", "/localdir1", true);
        SyncJob syncJob2 = new SyncJob(SyncJob.Type.Bidirectional, "url2", "token2", "id2", "/remotedir2", "/localdir2", false);
        SyncJob syncJob3 = new SyncJob(SyncJob.Type.Bidirectional, "url3", "token3", "id3", "/remotedir3", "/localdir3", false);
        SyncJob syncJob4 = new SyncJob(SyncJob.Type.Bidirectional, "url4", "token1", "id1", "/remotedir1", "/localdir4", true);
        SyncJob syncJob5 = new SyncJob(SyncJob.Type.Bidirectional, "url5", "token2", "id2", "/remotedir2", "/localdir5", false);
        SyncJob syncJob6 = new SyncJob(SyncJob.Type.Bidirectional, "url6", "token3", "id3", "/remotedir3", "/localdir6", true);
        SyncJob syncJob7 = new SyncJob(SyncJob.Type.Bidirectional, "url7", "token3", "id3", "/remotedir3", "/localdir7", false);

        Settings settings = new Settings();
        settings.setLanguage("fr");
        settings.setStartAtLogin(true);
        settings.setSyncInterval(45);
        settings.setJobs(new ArrayList<>(List.of(syncJob1, syncJob2, syncJob3, syncJob4, syncJob5, syncJob6)));

        Mockito.doReturn(settings).when(driveAPIServerImpl).getSettings();

        driveAPIServerImpl.startSyncJobs(List.of(syncJob4, syncJob7, syncJob3, syncJob2));
        Mockito.verify(driveAPIServerImpl, Mockito.times(1)).getSettings();

        Settings newSettings = new Settings();
        newSettings.setLanguage("fr");
        newSettings.setStartAtLogin(true);
        newSettings.setSyncInterval(45);
        syncJob3.setEnabled(true);
        syncJob2.setEnabled(true);
        newSettings.setJobs(new ArrayList<>(List.of(syncJob1, syncJob4, syncJob5, syncJob6, syncJob3, syncJob2)));

        Mockito.verify(driveAPIServerImpl, Mockito.times(1)).setSettings(newSettings);
    }

    @Test
    synchronized public void stopSyncJobTest() {
        SyncJob syncJob1 = new SyncJob(SyncJob.Type.Bidirectional, "url1", "token1", "id1", "/remotedir1", "/localdir1", true);
        SyncJob syncJob2 = new SyncJob(SyncJob.Type.Bidirectional, "url2", "token2", "id2", "/remotedir2", "/localdir2", false);
        SyncJob syncJob3 = new SyncJob(SyncJob.Type.Bidirectional, "url3", "token3", "id3", "/remotedir3", "/localdir3", false);
        SyncJob syncJob4 = new SyncJob(SyncJob.Type.Bidirectional, "url4", "token1", "id1", "/remotedir1", "/localdir4", true);
        SyncJob syncJob5 = new SyncJob(SyncJob.Type.Bidirectional, "url5", "token2", "id2", "/remotedir2", "/localdir5", false);
        SyncJob syncJob6 = new SyncJob(SyncJob.Type.Bidirectional, "url6", "token3", "id3", "/remotedir3", "/localdir6", true);
        SyncJob syncJob7 = new SyncJob(SyncJob.Type.Bidirectional, "url7", "token3", "id3", "/remotedir3", "/localdir7", false);

        Settings settings = new Settings();
        settings.setLanguage("fr");
        settings.setStartAtLogin(true);
        settings.setSyncInterval(45);
        settings.setJobs(new ArrayList<>(List.of(syncJob1, syncJob2, syncJob3, syncJob4, syncJob5, syncJob6)));

        Mockito.doReturn(settings).when(driveAPIServerImpl).getSettings();

        driveAPIServerImpl.stopSyncJobs(List.of(syncJob3, syncJob7, syncJob6, syncJob1));
        Mockito.verify(driveAPIServerImpl, Mockito.times(1)).getSettings();

        Settings newSettings = new Settings();
        newSettings.setLanguage("fr");
        newSettings.setStartAtLogin(true);
        newSettings.setSyncInterval(45);
        syncJob6.setEnabled(false);
        syncJob1.setEnabled(false);
        newSettings.setJobs(new ArrayList<>(List.of(syncJob2, syncJob3, syncJob4, syncJob5, syncJob6, syncJob1)));

        Mockito.verify(driveAPIServerImpl, Mockito.times(1)).setSettings(newSettings);
    }

    @Test
    synchronized public void addAndGetNotificationsTest() {
        driveAPIServerImpl.clearNotifications();

        List<Notification> notifications = new LinkedList<>();
        long now = System.currentTimeMillis();

        for(int i = 0; i<100; i++) {
            notifications.add(new Notification( i % 2 == 0 ? Notification.Type.Conflict : Notification.Type.JobStopped,
                    "/localdir" + i,
                    "/remotedir" + i,
                    "/remotefile" + i,
                    "message" + i,
                    now + i));
        }
        driveAPIServerImpl.addNotifications(notifications);

        Collections.reverse(notifications);
        Assert.assertEquals(notifications, driveAPIServerImpl.getNotifications(100));

        List<Notification> notifications2 = new LinkedList<>();
        for(int i = 100; i<200; i++) {
            notifications2.add(new Notification( i % 2 == 0 ? Notification.Type.Conflict : Notification.Type.JobStopped,
                    "/localdir" + i,
                    "/remotedir" + i,
                    "/remotefile" + i,
                    "message" + i,
                    now + 2000 + i));
        }
        driveAPIServerImpl.addNotifications(notifications2);

        Collections.reverse(notifications2);
        notifications2.addAll(notifications);
        Assert.assertEquals(notifications2, driveAPIServerImpl.getNotifications(200));
    }

    @Test
    synchronized public void removeNotificationsTest() {
        DriveAPIServerImpl openBISSyncClient = new DriveAPIServerImpl(configuration);
        openBISSyncClient.clearNotifications();

        List<Notification> notifications = new LinkedList<>();
        long now = System.currentTimeMillis();

        for(int i = 0; i<1000; i++) {
            notifications.add(new Notification( i % 2 == 0 ? Notification.Type.Conflict : Notification.Type.JobStopped,
                    "/localdir" + i,
                    "/remotedir" + i,
                    "/remotefile" + i,
                    "message" + i,
                    now + i));
        }
        openBISSyncClient.addNotifications(notifications);

        List<Notification> notificationsToBeRemoved = new LinkedList<>();
        for(int i : List.of(5,34,765)) {
            notificationsToBeRemoved.add(new Notification( i % 2 == 0 ? Notification.Type.Conflict : Notification.Type.JobStopped,
                    "/localdir" + i,
                    "/remotedir" + i,
                    "/remotefile" + i,
                    "message" + i,
                    now + i));
        }

        openBISSyncClient.removeNotifications(notificationsToBeRemoved);
        notifications.removeAll(notificationsToBeRemoved);
        Collections.reverse(notifications);

        Assert.assertEquals(notifications, openBISSyncClient.getNotifications(notifications.size()));
    }

    @Test
    synchronized public void testGetEvents() throws Exception {
        DriveAPIServerImpl openBISSyncClient = new DriveAPIServerImpl(configuration);
        openBISSyncClient.syncJobEventDAO.clearAll();

        long now = System.currentTimeMillis();

        List<SyncJobEvent> events = new ArrayList<>();

        for(int i = 0; i<100; i++) {
            events.add(SyncJobEvent.builder().syncDirection(SyncJobEvent.SyncDirection.UP)
                    .sourceTimestamp(System.currentTimeMillis())
                    .destinationTimestamp(System.currentTimeMillis())
                    .timestamp(now += ThreadLocalRandom.current().nextInt(0, 10000))
                    .entityPermId("" + i)
                    .localDirectoryRoot("loc-root")
                    .localFile("loc" + i)
                    .remoteFile("rem" + i)
                    .build());
            events.add(SyncJobEvent.builder().syncDirection(SyncJobEvent.SyncDirection.DOWN)
                    .sourceTimestamp(System.currentTimeMillis())
                    .destinationTimestamp(System.currentTimeMillis())
                    .timestamp(now += ThreadLocalRandom.current().nextInt(0, 10000))
                    .entityPermId("" + i)
                    .localDirectoryRoot("loc-root2")
                    .localFile("loc" + i)
                    .remoteFile("rem" + i)
                    .build());
        }

        Collections.shuffle(events);
        for(SyncJobEvent syncJobEvent: events) {
            openBISSyncClient.syncJobEventDAO.insertOrUpdate(syncJobEvent);
        }

        events.sort(Comparator.comparingLong(SyncJobEvent::getTimestamp).reversed());

        for(int j = 0; j<400; j += 50) {
            List<? extends Event> retrievedEvents = openBISSyncClient.getEvents(j);

            Assert.assertEquals(events.stream().limit(j).toList(), retrievedEvents);
        }
    }

}
