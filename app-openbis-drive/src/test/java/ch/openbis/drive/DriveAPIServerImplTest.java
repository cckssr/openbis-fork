package ch.openbis.drive;

import ch.openbis.drive.conf.Configuration;
import ch.openbis.drive.model.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@RunWith(JUnit4.class)
public class DriveAPIServerImplTest {

    private final Configuration configuration;

    public DriveAPIServerImplTest() {
        configuration = new Configuration(Path.of(this.getClass().getClassLoader().getResource("placeholder.txt").getPath()).getParent());
    }


    @Test
    synchronized public void setAndGetSettingsTest() {
        DriveAPIServerImpl openBISSyncClient = new DriveAPIServerImpl(configuration);
        Settings settings1 = Settings.defaultSettings();

        SyncJob syncJob1 = new SyncJob(SyncJob.Type.Bidirectional, "url1", "token1", "id1", "/remotedir1", "/localdir1", false);
        SyncJob syncJob2 = new SyncJob(SyncJob.Type.Bidirectional, "url2", "token2", "id2", "/remotedir2", "/localdir2", true);
        Settings settings2 = new Settings(true, "it", 15, new ArrayList<>(List.of(syncJob1, syncJob2)));

        openBISSyncClient.setSettings(settings1);
        Assert.assertEquals(settings1, openBISSyncClient.getSettings());

        openBISSyncClient.setSettings(settings2);
        Assert.assertEquals(settings2, openBISSyncClient.getSettings());
    }

    @Test
    synchronized public void getSyncJobsTest() {
        DriveAPIServerImpl openBISSyncClient = new DriveAPIServerImpl(configuration);

        Settings settings1 = Settings.defaultSettings();

        SyncJob syncJob1 = new SyncJob(SyncJob.Type.Bidirectional, "url1", "token1", "id1", "remotedir1", "/localdir1", true);
        SyncJob syncJob2 = new SyncJob(SyncJob.Type.Bidirectional, "url2", "token2", "id2", "remotedir2", "/localdir2", false);
        Settings settings2 = new Settings(true, "it", 15, new ArrayList<>(List.of(syncJob1, syncJob2)));

        openBISSyncClient.setSettings(settings1);
        Assert.assertEquals(Collections.emptyList(), openBISSyncClient.getSyncJobs());

        openBISSyncClient.setSettings(settings2);
        Assert.assertEquals(List.of(syncJob1, syncJob2), openBISSyncClient.getSyncJobs());
    }

    @Test(expected = IllegalArgumentException.class)
    synchronized public void setInvalidSettingsForOverlappinglocalDirTest() {
        DriveAPIServerImpl openBISSyncClient = new DriveAPIServerImpl(configuration);

        SyncJob syncJob1 = new SyncJob(SyncJob.Type.Bidirectional, "url1", "token1", "id1", "/remotedir1", "/localdir1", true);
        SyncJob syncJob2 = new SyncJob(SyncJob.Type.Bidirectional, "url2", "token2", "id2", "/remotedir2", "/localdir1/subdir", false);
        Settings settings = new Settings(true, "it", 15, new ArrayList<>(List.of(syncJob1, syncJob2)));

        openBISSyncClient.setSettings(settings);
    }

    @Test
    synchronized public void addSyncJobTest() {
        DriveAPIServerImpl openBISSyncClient = new DriveAPIServerImpl(configuration);

        openBISSyncClient.setSettings(Settings.defaultSettings());

        SyncJob syncJob1 = new SyncJob(SyncJob.Type.Bidirectional, "url1", "token1", "id1", "/remotedir1", "/localdir1", false);
        openBISSyncClient.addSyncJobs(List.of(syncJob1));

        Assert.assertEquals(List.of(syncJob1), openBISSyncClient.getSettings().getJobs());

        SyncJob syncJob2 = new SyncJob(SyncJob.Type.Bidirectional, "url2", "token2", "id2", "/remotedir2", "/localdir2", true);
        openBISSyncClient.addSyncJobs(List.of(syncJob2));

        Assert.assertEquals(List.of(syncJob1, syncJob2), openBISSyncClient.getSettings().getJobs());

        SyncJob syncJob3 = new SyncJob(SyncJob.Type.Bidirectional, "url3", "token3", "id3", "/remotedir3", "/localdir3", true);
        openBISSyncClient.addSyncJobs(List.of(syncJob3));

        Assert.assertEquals(List.of(syncJob1, syncJob2, syncJob3), openBISSyncClient.getSettings().getJobs());
    }

    @Test
    synchronized public void addSyncJobsTest() {
        DriveAPIServerImpl openBISSyncClient = new DriveAPIServerImpl(configuration);

        openBISSyncClient.setSettings(Settings.defaultSettings());

        SyncJob syncJob1 = new SyncJob(SyncJob.Type.Bidirectional, "url1", "token1", "id1", "/remotedir1", "/localdir1", true);
        openBISSyncClient.addSyncJobs(Collections.singletonList(syncJob1));

        Assert.assertEquals(List.of(syncJob1), openBISSyncClient.getSettings().getJobs());

        SyncJob syncJob2 = new SyncJob(SyncJob.Type.Bidirectional, "url2", "token2", "id2", "/remotedir2", "/localdir2", true);
        SyncJob syncJob3 = new SyncJob(SyncJob.Type.Bidirectional, "url3", "token3", "id3", "/remotedir3", "/localdir3", false);
        openBISSyncClient.addSyncJobs(List.of(syncJob2, syncJob3));

        Assert.assertEquals(List.of(syncJob1, syncJob2, syncJob3), openBISSyncClient.getSettings().getJobs());
    }

    @Test(expected = IllegalArgumentException.class)
    synchronized public void addSyncJobWithDuplicatelocalDirTest() {
        DriveAPIServerImpl openBISSyncClient = new DriveAPIServerImpl(configuration);

        openBISSyncClient.setSettings(Settings.defaultSettings());

        SyncJob syncJob1 = new SyncJob(SyncJob.Type.Bidirectional, "url1", "token1", "id1", "/remotedir1", "/localdir1", false);
        openBISSyncClient.addSyncJobs(List.of(syncJob1));

        Assert.assertEquals(List.of(syncJob1), openBISSyncClient.getSettings().getJobs());

        SyncJob syncJob2 = new SyncJob(SyncJob.Type.Bidirectional, "url2", "token2", "id2", "/remotedir2", "/localdir1", true);
        openBISSyncClient.addSyncJobs(List.of(syncJob2));
    }

    @Test
    synchronized public void removeSyncJobTest() {
        DriveAPIServerImpl openBISSyncClient = new DriveAPIServerImpl(configuration);

        openBISSyncClient.setSettings(Settings.defaultSettings());

        SyncJob syncJob1 = new SyncJob(SyncJob.Type.Bidirectional, "url1", "token1", "id1", "/remotedir1", "/localdir1", true);
        openBISSyncClient.addSyncJobs(List.of(syncJob1));
        SyncJob syncJob2 = new SyncJob(SyncJob.Type.Bidirectional, "url2", "token2", "id2", "/remotedir2", "/localdir2", false);
        openBISSyncClient.addSyncJobs(List.of(syncJob2));
        SyncJob syncJob3 = new SyncJob(SyncJob.Type.Bidirectional, "url3", "token3", "id3", "/remotedir3", "/localdir3", true);
        openBISSyncClient.addSyncJobs(List.of(syncJob3));

        Assert.assertEquals(List.of(syncJob1, syncJob2, syncJob3), openBISSyncClient.getSettings().getJobs());

        openBISSyncClient.removeSyncJobs(List.of(new SyncJob(SyncJob.Type.Bidirectional, "url2", "token2", "id2", "/remotedir2", "/localdir2", false)));
        Assert.assertEquals(List.of(syncJob1, syncJob3), openBISSyncClient.getSettings().getJobs());
        openBISSyncClient.removeSyncJobs(List.of(new SyncJob(SyncJob.Type.Bidirectional, "url1", "token1", "id1", "/remotedir1", "/localdir1", true)));
        Assert.assertEquals(List.of(syncJob3), openBISSyncClient.getSettings().getJobs());
        openBISSyncClient.removeSyncJobs(List.of(new SyncJob(SyncJob.Type.Bidirectional, "url3", "token3", "id3", "/remotedir3", "/localdir3", true)));
        Assert.assertEquals(Collections.emptyList(), openBISSyncClient.getSettings().getJobs());
    }

    @Test
    synchronized public void removeSyncJobsTest() {
        DriveAPIServerImpl openBISSyncClient = new DriveAPIServerImpl(configuration);

        openBISSyncClient.setSettings(Settings.defaultSettings());

        SyncJob syncJob1 = new SyncJob(SyncJob.Type.Bidirectional, "url1", "token1", "id1", "/remotedir1", "/localdir1", false);
        SyncJob syncJob2 = new SyncJob(SyncJob.Type.Bidirectional, "url2", "token2", "id2", "/remotedir2", "/localdir2", true);
        SyncJob syncJob3 = new SyncJob(SyncJob.Type.Bidirectional, "url3", "token3", "id3", "/remotedir3", "/localdir3", true);
        openBISSyncClient.addSyncJobs(List.of(syncJob1, syncJob2, syncJob3));

        Assert.assertEquals(List.of(syncJob1, syncJob2, syncJob3), openBISSyncClient.getSettings().getJobs());

        openBISSyncClient.removeSyncJobs(Collections.singletonList(new SyncJob(SyncJob.Type.Bidirectional, "url2", "token2", "id2", "/remotedir2", "/localdir2", true)));
        Assert.assertEquals(List.of(syncJob1, syncJob3), openBISSyncClient.getSettings().getJobs());

        openBISSyncClient.removeSyncJobs(
                List.of(new SyncJob(SyncJob.Type.Bidirectional, "url1", "token1", "id1", "/remotedir1", "/localdir1", false),
                        new SyncJob(SyncJob.Type.Bidirectional, "url3", "token3", "id3", "/remotedir3", "/localdir3", true)));
        Assert.assertEquals(Collections.emptyList(), openBISSyncClient.getSettings().getJobs());
    }

    @Test
    synchronized public void addAndGetNotificationsTest() {
        DriveAPIServerImpl openBISSyncClient = new DriveAPIServerImpl(configuration);
        openBISSyncClient.clearNotifications();

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
        openBISSyncClient.addNotifications(notifications);

        Collections.reverse(notifications);
        Assert.assertEquals(notifications, openBISSyncClient.getNotifications(100));

        List<Notification> notifications2 = new LinkedList<>();
        for(int i = 100; i<200; i++) {
            notifications2.add(new Notification( i % 2 == 0 ? Notification.Type.Conflict : Notification.Type.JobStopped,
                    "/localdir" + i,
                    "/remotedir" + i,
                    "/remotefile" + i,
                    "message" + i,
                    now + 2000 + i));
        }
        openBISSyncClient.addNotifications(notifications2);

        Collections.reverse(notifications2);
        notifications2.addAll(notifications);
        Assert.assertEquals(notifications2, openBISSyncClient.getNotifications(200));
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
