package ch.openbis.drive.notifications;

import ch.openbis.drive.conf.Configuration;
import ch.openbis.drive.model.Notification;
import ch.openbis.drive.model.SyncJob;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RunWith(JUnit4.class)
public class NotificationManagerSqliteImplTest {

    Configuration configuration;
    NotificationManager notificationManager;


    public NotificationManagerSqliteImplTest() throws Exception {
        configuration = new Configuration(Path.of(this.getClass().getClassLoader().getResource("placeholder.txt").getPath()).getParent().resolve("notification-manager-sqlite-impl-test"));
        createDatabaseDirectoryIfNotExists();
        notificationManager = new NotificationManagerSqliteImpl(configuration);
    }

    public void createDatabaseDirectoryIfNotExists() throws Exception {
        if(!Files.exists(configuration.getLocalAppStateDirectory())) {
            Files.createDirectories(configuration.getLocalAppStateDirectory());
        }
    }

    @Test
    public void testAddNotifications() {
        notificationManager.clearAllNotifications();

        String localDir = "localDir";
        long now = System.currentTimeMillis();
        List<Notification> notificationList = List.of(
                new Notification(Notification.Type.Conflict, localDir, "localFile", "remoteFile", "MESSAGE__", now+1000),
                new Notification(Notification.Type.Conflict, localDir, "localFile1", "remoteFile1", "MESSAGE__", now-190),
                new Notification(Notification.Type.Conflict, localDir, "localFile2", "remoteFile2", "MESSAGE__", now),
                new Notification(Notification.Type.Conflict, localDir, "localFile3", "remoteFile3", "MESSAGE__", now-32),
                new Notification(Notification.Type.Conflict, localDir, "localFile4", "remoteFile4", "MESSAGE__", now-2),
                new Notification(Notification.Type.Conflict, localDir, "localFile5", "remoteFile5", "MESSAGE__", now-324),
                new Notification(Notification.Type.Conflict, localDir, "localFile6", "remoteFile6", "MESSAGE__", now+1),
                new Notification(Notification.Type.Conflict, localDir, "localFile7", "remoteFile7", "MESSAGE__", now+2),
                new Notification(Notification.Type.JobStopped, localDir, null, null, "MESSAGE__", now+20000),
                new Notification(Notification.Type.JobException, localDir, null, null, "MESSAGE__", now+233333)
        );

        notificationManager.addNotifications(notificationList);

        List<Notification> sortedNotifications = new ArrayList<>(notificationList);
        sortedNotifications.sort(Comparator.comparingLong(Notification::getTimestamp).reversed());

        Assert.assertEquals(sortedNotifications, notificationManager.getNotifications(100));
    }

    @Test
    public void testGetNotifications() {
        notificationManager.clearAllNotifications();

        String localDir = "localDir";
        long now = System.currentTimeMillis();
        List<Notification> notificationList = List.of(
                new Notification(Notification.Type.Conflict, localDir, "localFile", "remoteFile", "MESSAGE__", now+1000),
                new Notification(Notification.Type.Conflict, localDir, "localFile1", "remoteFile1", "MESSAGE__", now-190),
                new Notification(Notification.Type.Conflict, localDir, "localFile2", "remoteFile2", "MESSAGE__", now),
                new Notification(Notification.Type.Conflict, localDir, "localFile3", "remoteFile3", "MESSAGE__", now-32),
                new Notification(Notification.Type.Conflict, localDir, "localFile4", "remoteFile4", "MESSAGE__", now-2),
                new Notification(Notification.Type.Conflict, localDir, "localFile5", "remoteFile5", "MESSAGE__", now-324),
                new Notification(Notification.Type.Conflict, localDir, "localFile6", "remoteFile6", "MESSAGE__", now+1),
                new Notification(Notification.Type.Conflict, localDir, "localFile7", "remoteFile7", "MESSAGE__", now+2),
                new Notification(Notification.Type.JobStopped, localDir, null, null, "MESSAGE__", now+20000),
                new Notification(Notification.Type.JobException, localDir, null, null, "MESSAGE__", now+233333)
        );

        notificationManager.addNotifications(notificationList);

        List<Notification> sortedNotifications = new ArrayList<>(notificationList);
        sortedNotifications.sort(Comparator.comparingLong(Notification::getTimestamp).reversed());

        Assert.assertEquals(sortedNotifications, notificationManager.getNotifications(100));
        Assert.assertEquals(sortedNotifications.stream().limit(4).toList(), notificationManager.getNotifications(4));
        Assert.assertEquals(sortedNotifications.stream().limit(9).toList(), notificationManager.getNotifications(9));

    }

    @Test
    public void testGetConflictNotifications() {
        notificationManager.clearAllNotifications();

        String localDir = "localDir";
        String otherLocalDir = "other-localDir";
        SyncJob syncJob = new SyncJob(SyncJob.Type.Bidirectional, "url1", "token1", "id1", "remotedir1", localDir, false);
        long now = System.currentTimeMillis();
        List<Notification> notificationList = List.of(
                new Notification(Notification.Type.Conflict, localDir, "localFile", "remoteFile", "MESSAGE__", now+1000),
                new Notification(Notification.Type.Conflict, otherLocalDir, "localFile1", "remoteFile1", "MESSAGE__", now-190),
                new Notification(Notification.Type.Conflict, localDir, "localFile2", "remoteFile2", "MESSAGE__", now),
                new Notification(Notification.Type.Conflict, otherLocalDir, "localFile3", "remoteFile3", "MESSAGE__", now-32),
                new Notification(Notification.Type.Conflict, localDir, "localFile4", "remoteFile4", "MESSAGE__", now-2),
                new Notification(Notification.Type.Conflict, otherLocalDir, "localFile5", "remoteFile5", "MESSAGE__", now-324),
                new Notification(Notification.Type.Conflict, localDir, "localFile6", "remoteFile6", "MESSAGE__", now+1),
                new Notification(Notification.Type.Conflict, otherLocalDir, "localFile7", "remoteFile7", "MESSAGE__", now+2),
                new Notification(Notification.Type.JobStopped, localDir, null, null, "MESSAGE__", now+20000),
                new Notification(Notification.Type.JobException, otherLocalDir, null, null, "MESSAGE__", now+233333)
        );

        notificationManager.addNotifications(notificationList);

        List<Notification> expectedNotifications = notificationList.stream().filter(
                notification -> notification.getType() == Notification.Type.Conflict &&
                        notification.getLocalDirectory().equals(localDir)
                ).toList();
        for(int i = 0; i<10; i++) {
            List<Notification> retrievedNotifications = notificationManager.getConflictNotifications(syncJob, i);
            Assert.assertTrue(expectedNotifications.containsAll(retrievedNotifications));
            Assert.assertTrue(retrievedNotifications.size() == Math.min(i, expectedNotifications.size()));
        }
    }

    @Test
    public void testRemoveNotifications() {
        notificationManager.clearAllNotifications();

        String localDir = "localDir";
        long now = System.currentTimeMillis();
        List<Notification> notificationList = List.of(
                new Notification(Notification.Type.Conflict, localDir, "localFile", "remoteFile", "MESSAGE__", now+1000),
                new Notification(Notification.Type.Conflict, localDir, "localFile1", "remoteFile1", "MESSAGE__", now-190),
                new Notification(Notification.Type.Conflict, localDir, "localFile2", "remoteFile2", "MESSAGE__", now),
                new Notification(Notification.Type.Conflict, localDir, "localFile3", "remoteFile3", "MESSAGE__", now-32),
                new Notification(Notification.Type.Conflict, localDir, "localFile4", "remoteFile4", "MESSAGE__", now-2),
                new Notification(Notification.Type.Conflict, localDir, "localFile5", "remoteFile5", "MESSAGE__", now-324),
                new Notification(Notification.Type.Conflict, localDir, "localFile6", "remoteFile6", "MESSAGE__", now+1),
                new Notification(Notification.Type.Conflict, localDir, "localFile7", "remoteFile7", "MESSAGE__", now+2),
                new Notification(Notification.Type.JobStopped, localDir, null, null, "MESSAGE__", now+20000),
                new Notification(Notification.Type.JobException, localDir, null, null, "MESSAGE__", now+233333)
        );

        notificationManager.addNotifications(notificationList);

        List<Notification> sortedNotifications = new ArrayList<>(notificationList);
        sortedNotifications.sort(Comparator.comparingLong(Notification::getTimestamp).reversed());

        notificationManager.removeNotifications(List.of(new Notification(Notification.Type.Conflict, localDir, "localFile3", "remoteFile3", "MESSAGE_CHANGED_", now+100)));

        sortedNotifications.removeIf(entry -> "localFile3".equals(entry.getLocalFile()));

        Assert.assertEquals(sortedNotifications, notificationManager.getNotifications(100));

        notificationManager.removeNotifications(List.of(
                new Notification(Notification.Type.Conflict, localDir, "localFile", "remoteFile", "MESSAGE_CHANGED_", now+100),
                new Notification(Notification.Type.Conflict, localDir, "localFile9", "remoteFile", "MESSAGE_CHANGED_", now+100),
                new Notification(Notification.Type.JobStopped, localDir, null, null, "MESSAGE_CHANGED_", now-100)
                ));

        sortedNotifications.removeIf(entry -> entry.getType() == Notification.Type.JobStopped);
        sortedNotifications.removeIf(entry -> "remoteFile".equals(entry.getRemoteFile()));

        Assert.assertEquals(sortedNotifications, notificationManager.getNotifications(100));
    }

    @Test
    public void testGetSpecificNotification() {
        notificationManager.clearAllNotifications();

        String localDir = "localDir";
        long now = System.currentTimeMillis();
        List<Notification> notificationList = List.of(
                new Notification(Notification.Type.Conflict, localDir, "localFile", "remoteFile", "MESSAGE__", now+1000),
                new Notification(Notification.Type.Conflict, localDir, "localFile1", "remoteFile1", "MESSAGE__", now-190),
                new Notification(Notification.Type.Conflict, localDir, "localFile2", "remoteFile2", "MESSAGE__", now),
                new Notification(Notification.Type.Conflict, localDir, "localFile3", "remoteFile3", "MESSAGE__", now-32),
                new Notification(Notification.Type.Conflict, localDir, "localFile4", "remoteFile4", "MESSAGE__", now-2),
                new Notification(Notification.Type.Conflict, localDir, "localFile5", "remoteFile5", "MESSAGE__", now-324),
                new Notification(Notification.Type.Conflict, localDir, "localFile6", "remoteFile6", "MESSAGE__", now+1),
                new Notification(Notification.Type.Conflict, localDir, "localFile7", "remoteFile7", "MESSAGE__", now+2),
                new Notification(Notification.Type.JobStopped, localDir, null, null, "MESSAGE__", now+20000),
                new Notification(Notification.Type.JobException, localDir, null, null, "MESSAGE__", now+233333)
        );

        notificationManager.addNotifications(notificationList);

        for (Notification notification : notificationList) {
            Assert.assertEquals(notification, notificationManager.getSpecificNotification(notification));
            Assert.assertEquals(notification, notificationManager.getSpecificNotification(notification.toBuilder().message(notification.getMessage() + "CHANGED").build()));
            Assert.assertEquals(notification, notificationManager.getSpecificNotification(notification.toBuilder().timestamp(notification.getTimestamp() + 10000).build()));
            Assert.assertNull(notificationManager.getSpecificNotification(notification.toBuilder().localDirectory(notification.getLocalDirectory() + "CHANGED").build()));
            Assert.assertNull(notificationManager.getSpecificNotification(notification.toBuilder().localFile(notification.getLocalFile() + "CHANGED").build()));
            Assert.assertNull(notificationManager.getSpecificNotification(notification.toBuilder().remoteFile(notification.getRemoteFile() + "CHANGED").build()));
            Assert.assertNull(notificationManager.getSpecificNotification(notification.toBuilder().type(switch (notification.getType()) {
                case JobStopped, JobException -> Notification.Type.Conflict;
                case Conflict -> Notification.Type.JobException;
            }).build()));
        }

    }

    @Test
    public void testClearNotifications() {
        notificationManager.clearAllNotifications();

        String localDir = "localDir";
        String localDir2 = "localDir2";
        long now = System.currentTimeMillis();
        List<Notification> notificationList = List.of(
                new Notification(Notification.Type.Conflict, localDir, "localFile", "remoteFile", "MESSAGE__", now+1000),
                new Notification(Notification.Type.Conflict, localDir2, "localFile1", "remoteFile1", "MESSAGE__", now-190),
                new Notification(Notification.Type.Conflict, localDir, "localFile2", "remoteFile2", "MESSAGE__", now),
                new Notification(Notification.Type.Conflict, localDir2, "localFile3", "remoteFile3", "MESSAGE__", now-32),
                new Notification(Notification.Type.Conflict, localDir, "localFile4", "remoteFile4", "MESSAGE__", now-2),
                new Notification(Notification.Type.Conflict, localDir2, "localFile5", "remoteFile5", "MESSAGE__", now-324),
                new Notification(Notification.Type.Conflict, localDir, "localFile6", "remoteFile6", "MESSAGE__", now+1),
                new Notification(Notification.Type.Conflict, localDir, "localFile7", "remoteFile7", "MESSAGE__", now+2),
                new Notification(Notification.Type.JobStopped, localDir2, null, null, "MESSAGE__", now+20000),
                new Notification(Notification.Type.JobException, localDir, null, null, "MESSAGE__", now+233333)
        );

        notificationManager.addNotifications(notificationList);

        Assert.assertEquals(10, notificationManager.getNotifications(1000).size());

        notificationManager.clearAllNotifications();

        Assert.assertEquals(0, notificationManager.getNotifications(1000).size());
    }
}