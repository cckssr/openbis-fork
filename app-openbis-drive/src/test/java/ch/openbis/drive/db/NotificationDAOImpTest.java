package ch.openbis.drive.db;

import ch.openbis.drive.conf.Configuration;
import ch.openbis.drive.model.Notification;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@RunWith(JUnit4.class)
public class NotificationDAOImpTest {
    private final String localDirectoryRoot = Path.of(
            this.getClass().getClassLoader().getResource("placeholder.txt").getPath()).getParent()
            .resolve("notification-dao-impl-test")
            .toAbsolutePath().toString();

    private final Configuration configuration = new Configuration(Path.of(localDirectoryRoot));

    @Before
    synchronized public void clearDatabase() throws Exception {
        createDatabaseDirectoryIfNotExists();
        Files.deleteIfExists(Path.of(localDirectoryRoot, NotificationDAOImpl.DB_FILE_NAME));
    }

    public void createDatabaseDirectoryIfNotExists() throws Exception {
        if(!Files.exists(Path.of(localDirectoryRoot))) {
            Files.createDirectories(Path.of(localDirectoryRoot));
        }
    }

    @Test
    synchronized public void insertOrUpdate() throws Exception {

        NotificationDAOImpl notificationDAOImpl = new NotificationDAOImpl(configuration);

        for (Notification.Type type : Notification.Type.values()) {
            Notification notification = Notification.builder()
                    .type(type)
                    .localDirectory("localDir")
                    .localFile(type == Notification.Type.Conflict ? "localFile" : null)
                    .remoteFile(type == Notification.Type.Conflict ? "remoteFile" : null)
                    .message("MESSAGE__")
                    .timestamp(System.currentTimeMillis())
                    .build();

            notificationDAOImpl.insertOrUpdate(notification);
            Notification readNotification = notificationDAOImpl.selectByPrimaryKey(notification.getType(), notification.getLocalDirectory(), notification.getLocalFile(), notification.getRemoteFile());

            Assert.assertEquals(notification, readNotification);
        }

    }

    @Test
    synchronized public void selectByTypeAndLocalDirAndFiles() throws Exception {

        NotificationDAOImpl notificationDAOImpl = new NotificationDAOImpl(configuration);

        for (Notification.Type type : Notification.Type.values()) {
            Notification notification = Notification.builder()
                    .type(type)
                    .localDirectory("localDir")
                    .localFile(type == Notification.Type.Conflict ? "localFile" : null)
                    .remoteFile(type == Notification.Type.Conflict ? "remoteFile" : null)
                    .message("MESSAGE__")
                    .timestamp(System.currentTimeMillis())
                    .build();

            notificationDAOImpl.insertOrUpdate(notification);
            Notification readNotification = notificationDAOImpl.selectByPrimaryKey(notification.getType(), notification.getLocalDirectory(), notification.getLocalFile(), notification.getRemoteFile());

            Assert.assertEquals(notification, readNotification);
        }
    }

    @Test
    synchronized public void removeByTypeAndLocalDirAndFiles() throws Exception {

        NotificationDAOImpl notificationDAOImpl = new NotificationDAOImpl(configuration);

        for (Notification.Type type : Notification.Type.values()) {
            Notification notification = Notification.builder()
                    .type(type)
                    .localDirectory("localDir")
                    .localFile(type == Notification.Type.Conflict ? "localFile" : null)
                    .remoteFile(type == Notification.Type.Conflict ? "remoteFile" : null)
                    .message("MESSAGE__")
                    .timestamp(System.currentTimeMillis())
                    .build();

            notificationDAOImpl.insertOrUpdate(notification);
        }

        Assert.assertEquals(3, notificationDAOImpl.selectLast(100).size());

        notificationDAOImpl.removeByPrimaryKey(Notification.Type.JobStopped, "localDir", null, null);
        Assert.assertEquals(2, notificationDAOImpl.selectLast(100).size());
        Assert.assertEquals(Set.of(Notification.Type.Conflict, Notification.Type.JobException), notificationDAOImpl.selectLast(100).stream().map(Notification::getType).collect(Collectors.toSet()));

        notificationDAOImpl.removeByPrimaryKey(Notification.Type.Conflict, "localDir", "localFile", "remoteFile");
        Assert.assertEquals(1, notificationDAOImpl.selectLast(100).size());
        Assert.assertEquals(Set.of(Notification.Type.JobException), notificationDAOImpl.selectLast(100).stream().map(Notification::getType).collect(Collectors.toSet()));

        notificationDAOImpl.removeByPrimaryKey(Notification.Type.JobException, "localDir", null, null);
        Assert.assertEquals(0, notificationDAOImpl.selectLast(100).size());
        Assert.assertEquals(Collections.emptySet(), notificationDAOImpl.selectLast(100).stream().map(Notification::getType).collect(Collectors.toSet()));
    }

    @Test
    synchronized public void selectLast() throws Exception {

        NotificationDAOImpl notificationDAOImpl = new NotificationDAOImpl(configuration);

        LinkedList<Notification> expectedNotifications = new LinkedList<>();

        for(int limit=0; limit<100; limit = limit + 10) {

            for(int j = 0; j<30; j++) {
                for (Notification.Type type : Notification.Type.values()) {
                    Notification notification = Notification.builder()
                            .type(type)
                            .localDirectory("localDir")
                            .localFile("localFile" + j)
                            .remoteFile("remoteFile" + j)
                            .message("MESSAGE__")
                            .timestamp(System.currentTimeMillis())
                            .build();

                    notificationDAOImpl.insertOrUpdate(notification);
                    Notification readNotification = notificationDAOImpl.selectByPrimaryKey(notification.getType(), notification.getLocalDirectory(), notification.getLocalFile(), notification.getRemoteFile());

                    Assert.assertEquals(notification, readNotification);

                    expectedNotifications.addFirst(notification);
                }

                List<Notification> retrievedNotifications = notificationDAOImpl.selectLast(limit);
                Assert.assertEquals(expectedNotifications.stream().limit(limit).toList(), retrievedNotifications);
            }
        }
    }

    @Test
    synchronized public void removeByLocalDir() throws Exception {

        NotificationDAOImpl notificationDAOImpl = new NotificationDAOImpl(configuration);

        for(int i = 0; i<10; i++) {
            for (Notification.Type type : Notification.Type.values()) {
                Notification notification = Notification.builder()
                        .type(type)
                        .localDirectory("localDir" + i)
                        .localFile(type == Notification.Type.Conflict ? "localFile" : null)
                        .remoteFile(type == Notification.Type.Conflict ? "remoteFile" : null)
                        .message("MESSAGE__")
                        .timestamp(System.currentTimeMillis())
                        .build();

                notificationDAOImpl.insertOrUpdate(notification);
            }
        }

        Assert.assertEquals(30, notificationDAOImpl.selectLast(100).size());

        notificationDAOImpl.removeByLocalDirectory("localDir3");

        Assert.assertEquals(27, notificationDAOImpl.selectLast(100).size());

        notificationDAOImpl.removeByLocalDirectory("localDir7");

        Assert.assertEquals(24, notificationDAOImpl.selectLast(100).size());

        Assert.assertEquals(Set.of("localDir0","localDir1", "localDir2","localDir4","localDir5","localDir6","localDir8","localDir9"),
                notificationDAOImpl.selectLast(100).stream().map(Notification::getLocalDirectory).collect(Collectors.toSet()));
    }

    @Test
    synchronized public void testRemoveOldEntriesByType() throws Exception {

        NotificationDAOImpl notificationDAOImpl = new NotificationDAOImpl(configuration);
        notificationDAOImpl.clearAll();


        String localDir = "localDir";
        String localDir2 = "localDir2";
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

        for(int i = 0; i<10; i++) {
            for (Notification.Type type : Notification.Type.values()) {
                Notification notification = Notification.builder()
                        .type(type)
                        .localDirectory("localDir" + i)
                        .localFile(type == Notification.Type.Conflict ? "localFile" : null)
                        .remoteFile(type == Notification.Type.Conflict ? "remoteFile" : null)
                        .message("MESSAGE__")
                        .timestamp(System.currentTimeMillis())
                        .build();

                notificationDAOImpl.insertOrUpdate(notification);
            }
        }

        notificationDAOImpl.clearAll();
        Assert.assertEquals(0, notificationDAOImpl.selectLast(10000).size());
    }
}