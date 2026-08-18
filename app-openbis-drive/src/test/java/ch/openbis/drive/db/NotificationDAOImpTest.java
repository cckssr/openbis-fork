package ch.openbis.drive.db;

import ch.openbis.drive.DriveTestCase;
import ch.openbis.drive.conf.Configuration;
import ch.openbis.drive.model.Notification;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@RunWith(JUnit4.class)
public class NotificationDAOImpTest extends DriveTestCase {
    private final String localDirectoryRoot = Path.of(
            this.getClass().getClassLoader().getResource("placeholder.txt").getPath()).getParent()
            .resolve("notification-dao-impl-test")
            .toAbsolutePath().toString();

    private final Configuration configuration = new Configuration(Path.of(localDirectoryRoot));

    @Before
    synchronized public void clearDatabase() throws Exception {
        createDatabaseDirectoryIfNotExists();
        Files.deleteIfExists(configuration.getLocalAppStateDirectory().resolve(NotificationDAOImpl.DB_FILE_NAME));
    }

    public void createDatabaseDirectoryIfNotExists() throws Exception {
        Files.createDirectories(configuration.getLocalAppStateDirectory());
    }

    @Test
    synchronized public void insertOrUpdate() throws Exception {

        NotificationDAOImpl notificationDAOImpl = new NotificationDAOImpl(configuration);

        long counter = 0;
        for (Notification.Type type : Notification.Type.values()) {
            Notification notification = Notification.builder()
                    .type(type)
                    .localDirectory("001_localDir")
                    .localFile(type == Notification.Type.Conflict ? "localFile" : null)
                    .remoteFile(type == Notification.Type.Conflict ? "remoteFile" : null)
                    .message("MESSAGE__")
                    .timestamp(System.currentTimeMillis() + counter)
                    .build();

            notificationDAOImpl.insertOrUpdate(notification);
            Notification readNotification = notificationDAOImpl.selectByPrimaryKey(notification.getType(), notification.getLocalDirectory(), notification.getLocalFile(), notification.getRemoteFile());

            Assert.assertEquals(notification, readNotification);
            counter++;
        }

    }

    @Test
    synchronized public void selectByTypeAndLocalDirAndFiles() throws Exception {

        NotificationDAOImpl notificationDAOImpl = new NotificationDAOImpl(configuration);

        long counter = 0;
        for (Notification.Type type : Notification.Type.values()) {
            Notification notification = Notification.builder()
                    .type(type)
                    .localDirectory("002_localDir")
                    .localFile(type == Notification.Type.Conflict ? "localFile" : null)
                    .remoteFile(type == Notification.Type.Conflict ? "remoteFile" : null)
                    .message("MESSAGE__")
                    .timestamp(System.currentTimeMillis() + counter)
                    .build();

            notificationDAOImpl.insertOrUpdate(notification);
            Notification readNotification = notificationDAOImpl.selectByPrimaryKey(notification.getType(), notification.getLocalDirectory(), notification.getLocalFile(), notification.getRemoteFile());

            Assert.assertEquals(notification, readNotification);
            counter++;
        }
    }

    @Test
    synchronized public void removeByTypeAndLocalDirAndFiles() throws Exception {

        NotificationDAOImpl notificationDAOImpl = new NotificationDAOImpl(configuration);

        long counter = 0;
        for (Notification.Type type : Notification.Type.values()) {
            Notification notification = Notification.builder()
                    .type(type)
                    .localDirectory("003_localDir")
                    .localFile(type == Notification.Type.Conflict ? "localFile" : null)
                    .remoteFile(type == Notification.Type.Conflict ? "remoteFile" : null)
                    .message("MESSAGE__")
                    .timestamp(System.currentTimeMillis() + counter)
                    .build();

            notificationDAOImpl.insertOrUpdate(notification);
            counter++;
        }

        Assert.assertEquals(3, notificationDAOImpl.selectLast(100).size());

        notificationDAOImpl.removeByPrimaryKey(Notification.Type.JobStopped, "003_localDir", null, null);
        Assert.assertEquals(2, notificationDAOImpl.selectLast(100).size());
        Assert.assertEquals(Set.of(Notification.Type.Conflict, Notification.Type.JobException), notificationDAOImpl.selectLast(100).stream().map(Notification::getType).collect(Collectors.toSet()));

        notificationDAOImpl.removeByPrimaryKey(Notification.Type.Conflict, "003_localDir", "localFile", "remoteFile");
        Assert.assertEquals(1, notificationDAOImpl.selectLast(100).size());
        Assert.assertEquals(Set.of(Notification.Type.JobException), notificationDAOImpl.selectLast(100).stream().map(Notification::getType).collect(Collectors.toSet()));

        notificationDAOImpl.removeByPrimaryKey(Notification.Type.JobException, "003_localDir", null, null);
        Assert.assertEquals(0, notificationDAOImpl.selectLast(100).size());
        Assert.assertEquals(Collections.emptySet(), notificationDAOImpl.selectLast(100).stream().map(Notification::getType).collect(Collectors.toSet()));
    }

    @Test
    synchronized public void selectLast() throws Exception {

        NotificationDAOImpl notificationDAOImpl = new NotificationDAOImpl(configuration);

        LinkedList<Notification> expectedNotifications = new LinkedList<>();

        long counter = 0;
        for(int limit=0; limit<100; limit = limit + 10) {

            for(int j = 0; j<30; j++) {
                for (Notification.Type type : Notification.Type.values()) {
                    Notification notification = Notification.builder()
                            .type(type)
                            .localDirectory("004_localDir")
                            .localFile("localFile" + j)
                            .remoteFile("remoteFile" + j)
                            .message("MESSAGE__")
                            .timestamp(System.currentTimeMillis() + counter)
                            .build();

                    notificationDAOImpl.insertOrUpdate(notification);
                    Notification readNotification = notificationDAOImpl.selectByPrimaryKey(notification.getType(), notification.getLocalDirectory(), notification.getLocalFile(), notification.getRemoteFile());

                    Assert.assertEquals(notification, readNotification);

                    expectedNotifications.addFirst(notification);
                    counter++;
                }

                List<Notification> retrievedNotifications = notificationDAOImpl.selectLast(limit);
                Assert.assertEquals(expectedNotifications.stream().limit(limit).toList(), retrievedNotifications);
            }
        }
    }

    @Test
    synchronized public void selectByLocalDirAndType() throws Exception {

        NotificationDAOImpl notificationDAOImpl = new NotificationDAOImpl(configuration);

        LinkedList<Notification> expectedNotifications = new LinkedList<>();

        long counter = 0;
        for(int j = 0; j<30; j++) {
            for (Notification.Type type : Notification.Type.values()) {
                Notification notification = Notification.builder()
                        .type(type)
                        .localDirectory("005_localDir" + (j % 3))
                        .localFile("localFile" + j)
                        .remoteFile("remoteFile" + j)
                        .message("MESSAGE__")
                        .timestamp(System.currentTimeMillis() + counter)
                        .build();

                notificationDAOImpl.insertOrUpdate(notification);
                Notification readNotification = notificationDAOImpl.selectByPrimaryKey(notification.getType(), notification.getLocalDirectory(), notification.getLocalFile(), notification.getRemoteFile());

                Assert.assertEquals(notification, readNotification);

                if (readNotification.getLocalDirectory().equals("005_localDir1") && readNotification.getType() == Notification.Type.Conflict) {
                    expectedNotifications.addFirst(notification);
                }
                counter++;
            }
        }

        for(int i=0; i<20; i++) {
            List<Notification> retrievedNotifications = notificationDAOImpl.selectByLocalDirectoryAndType("005_localDir1", Notification.Type.Conflict, i);
            Assert.assertTrue(expectedNotifications.containsAll(retrievedNotifications));
            Assert.assertTrue(retrievedNotifications.size() == Math.min(i, expectedNotifications.size()));
        }
    }

    @Test
    synchronized public void removeByLocalDir() throws Exception {

        NotificationDAOImpl notificationDAOImpl = new NotificationDAOImpl(configuration);

        long counter = 0;
        for(int i = 0; i<10; i++) {
            for (Notification.Type type : Notification.Type.values()) {
                Notification notification = Notification.builder()
                        .type(type)
                        .localDirectory("006_localDir" + i)
                        .localFile(type == Notification.Type.Conflict ? "localFile" : null)
                        .remoteFile(type == Notification.Type.Conflict ? "remoteFile" : null)
                        .message("MESSAGE__")
                        .timestamp(System.currentTimeMillis() + counter)
                        .build();

                notificationDAOImpl.insertOrUpdate(notification);
                counter++;
            }
        }

        Assert.assertEquals(30, notificationDAOImpl.selectLast(100).size());

        notificationDAOImpl.removeByLocalDirectory("006_localDir3");

        Assert.assertEquals(27, notificationDAOImpl.selectLast(100).size());

        notificationDAOImpl.removeByLocalDirectory("006_localDir7");

        Assert.assertEquals(24, notificationDAOImpl.selectLast(100).size());

        Assert.assertEquals(Set.of("006_localDir0","006_localDir1", "006_localDir2","006_localDir4","006_localDir5","006_localDir6","006_localDir8","006_localDir9"),
                notificationDAOImpl.selectLast(100).stream().map(Notification::getLocalDirectory).collect(Collectors.toSet()));
    }

    @Test
    synchronized public void testRemoveOldEntriesByType() throws Exception {

        NotificationDAOImpl notificationDAOImpl = new NotificationDAOImpl(configuration);
        notificationDAOImpl.clearAll();


        String localDir = "007_localDir";
        String localDir2 = "007_localDir2";
        long now = System.currentTimeMillis();
        List<Notification> notificationList = List.of(
                new Notification(Notification.Type.JobStopped, localDir, null, null, "MESSAGE__", now+1000),
                new Notification(Notification.Type.JobStopped, localDir2, null, null, "MESSAGE__", now-190),
                new Notification(Notification.Type.JobStopped, localDir, null, null, "MESSAGE__", now),
                new Notification(Notification.Type.JobStopped, localDir2, null, null, "MESSAGE__", now-32),
                new Notification(Notification.Type.JobStopped, localDir, null, null, "MESSAGE__", now-2),
                new Notification(Notification.Type.JobStopped, localDir2, null, null, "MESSAGE__", now-324),
                new Notification(Notification.Type.JobStopped, localDir, null, null, "MESSAGE__", now+1),
                new Notification(Notification.Type.JobStopped, localDir, null, null, "MESSAGE__", now+2),
                new Notification(Notification.Type.JobStopped, localDir2, null, null, "MESSAGE__", now+20000),
                new Notification(Notification.Type.JobStopped, localDir, null, null, "MESSAGE__", now+233333)
        );

        for(Notification notification : notificationList) {
            notificationDAOImpl.insertOrUpdate(notification);
        }

        Assert.assertEquals(10, notificationDAOImpl.selectLast(1000).size());

        notificationDAOImpl.removeOldEntriesByLocalDirectoryAndType(localDir, Notification.Type.JobStopped, 3);

        Assert.assertEquals(7, notificationDAOImpl.selectLast(1000).size());

        Assert.assertEquals(List.of(
                new Notification(Notification.Type.JobStopped, localDir, null, null, "MESSAGE__", now+1000),
                new Notification(Notification.Type.JobStopped, localDir2, null, null, "MESSAGE__", now-190),
                new Notification(Notification.Type.JobStopped, localDir2, null, null, "MESSAGE__", now-32),
                new Notification(Notification.Type.JobStopped, localDir2, null, null, "MESSAGE__", now-324),
                new Notification(Notification.Type.JobStopped, localDir, null, null, "MESSAGE__", now+2),
                new Notification(Notification.Type.JobStopped, localDir2, null, null, "MESSAGE__", now+20000),
                new Notification(Notification.Type.JobStopped, localDir, null, null, "MESSAGE__", now+233333)
        ).stream().sorted(Comparator.comparing(Notification::getTimestamp).reversed()).toList(), notificationDAOImpl.selectLast(100));
    }

    @Test
    synchronized public void clearAll() throws Exception {

        NotificationDAOImpl notificationDAOImpl = new NotificationDAOImpl(configuration);

        long counter = 0;
        for(int i = 0; i<10; i++) {
            for (Notification.Type type : Notification.Type.values()) {
                Notification notification = Notification.builder()
                        .type(type)
                        .localDirectory("008_localDir" + i)
                        .localFile(type == Notification.Type.Conflict ? "localFile" : null)
                        .remoteFile(type == Notification.Type.Conflict ? "remoteFile" : null)
                        .message("MESSAGE__")
                        .timestamp(System.currentTimeMillis() + counter)
                        .build();

                notificationDAOImpl.insertOrUpdate(notification);
                counter++;
            }
        }

        notificationDAOImpl.clearAll();
        Assert.assertEquals(0, notificationDAOImpl.selectLast(10000).size());
    }
}