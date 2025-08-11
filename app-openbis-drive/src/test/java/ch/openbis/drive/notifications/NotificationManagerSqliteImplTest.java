package ch.openbis.drive.notifications;

import ch.openbis.drive.conf.Configuration;
import ch.openbis.drive.db.NotificationDAOImpl;
import ch.openbis.drive.model.Notification;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Before;
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
        if(!Files.exists(configuration.getLocalAppDirectory())) {
            Files.createDirectories(configuration.getLocalAppDirectory());
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