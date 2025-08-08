package ch.openbis.drive.db;

import ch.openbis.drive.conf.Configuration;
import ch.openbis.drive.model.SyncJob;
import ch.openbis.drive.model.SyncJobEvent;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@RunWith(JUnit4.class)
public class SyncJobEventDAOImpTest {
    private final String localDirectoryRoot = Path.of(
            this.getClass().getClassLoader().getResource("placeholder.txt").getPath()).getParent()
            .toAbsolutePath().toString();

    @Test
    public void createDatabaseIfNotExists() throws Exception {
        if(!Files.exists(Path.of(localDirectoryRoot))) {
            Files.createDirectories(Path.of(localDirectoryRoot));
        }
        SyncJobEventDAOImp syncJobEventDAOImp = new SyncJobEventDAOImp(new Configuration(Path.of(localDirectoryRoot)));
        syncJobEventDAOImp.createDatabaseIfNotExists();
    }

    @Test
    public void insertOrUpdate() throws Exception {
        createDatabaseIfNotExists();

        for(boolean directory : List.of(Boolean.FALSE, Boolean.TRUE)) {
            for(boolean sourceDeleted : List.of(Boolean.FALSE, Boolean.TRUE)) {
                SyncJobEvent testJobEvent = SyncJobEvent.builder()
                        .syncDirection(SyncJobEvent.SyncDirection.UP)
                        .localFile("a.txt")
                        .remoteFile("a.txt")
                        .entityPermId(UUID.randomUUID().toString())
                        .localDirectoryRoot(localDirectoryRoot)
                        .sourceTimestamp(Instant.now().toEpochMilli())
                        .destinationTimestamp(Instant.now().toEpochMilli())
                        .timestamp(Instant.now().toEpochMilli())
                        .sourceDeleted(sourceDeleted)
                        .directory(directory)
                        .build();

                SyncJobEventDAOImp syncJobEventDAOImp = new SyncJobEventDAOImp(new Configuration(Path.of(localDirectoryRoot)));
                syncJobEventDAOImp.insertOrUpdate(testJobEvent);
                SyncJobEvent resultJobEvent = syncJobEventDAOImp.selectByPrimaryKey(testJobEvent.getSyncDirection(), testJobEvent.getLocalFile(), testJobEvent.getRemoteFile());
                Assert.assertEquals(testJobEvent, resultJobEvent);
            }
        }
    }

    @Test
    public void selectByPrimaryKey() throws Exception {
        createDatabaseIfNotExists();

        SyncJobEvent testJobEvent = SyncJobEvent.builder()
                .syncDirection(SyncJobEvent.SyncDirection.UP)
                .localFile("a.txt")
                .remoteFile("a.txt")
                .entityPermId(UUID.randomUUID().toString())
                .localDirectoryRoot(localDirectoryRoot)
                .sourceTimestamp(Instant.now().toEpochMilli())
                .destinationTimestamp(Instant.now().toEpochMilli())
                .timestamp(Instant.now().toEpochMilli())
                .build();

        SyncJobEventDAOImp syncJobEventDAOImp = new SyncJobEventDAOImp(new Configuration(Path.of(localDirectoryRoot)));
        syncJobEventDAOImp.insertOrUpdate(testJobEvent);
        SyncJobEvent resultJobEvent = syncJobEventDAOImp.selectByPrimaryKey(testJobEvent.getSyncDirection(), testJobEvent.getLocalFile(), testJobEvent.getRemoteFile());
        Assert.assertEquals(testJobEvent, resultJobEvent);
    }

    @Test
    public void selectMostRecent() throws Exception {
        createDatabaseIfNotExists();

        List<SyncJobEvent> events = new ArrayList<>();
        long now = System.currentTimeMillis();

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
            events.add(SyncJobEvent.builder().syncDirection(SyncJobEvent.SyncDirection.UP)
                    .sourceTimestamp(System.currentTimeMillis())
                    .destinationTimestamp(null)
                    .timestamp(now += ThreadLocalRandom.current().nextInt(0, 10000))
                    .entityPermId("" + i)
                    .localDirectoryRoot("loc-root")
                    .localFile("loc-incomplete" + i)
                    .remoteFile("rem-incomplete" + i)
                    .build());
            events.add(SyncJobEvent.builder().syncDirection(SyncJobEvent.SyncDirection.DOWN)
                    .sourceTimestamp(System.currentTimeMillis())
                    .destinationTimestamp(null)
                    .timestamp(now += ThreadLocalRandom.current().nextInt(0, 10000))
                    .entityPermId("" + i)
                    .localDirectoryRoot("loc-root2")
                    .localFile("loc" + i)
                    .remoteFile("rem" + i)
                    .sourceDeleted(true)
                    .build());
        }

        SyncJobEventDAOImp syncJobEventDAOImp = new SyncJobEventDAOImp(new Configuration(Path.of(localDirectoryRoot)));
        Collections.shuffle(events);
        syncJobEventDAOImp.clearAll();
        for(SyncJobEvent syncJobEvent: events) {
            syncJobEventDAOImp.insertOrUpdate(syncJobEvent);
        }

        events = events.stream().filter( syncJobEvent ->
                syncJobEvent.getDestinationTimestamp() != null || syncJobEvent.isSourceDeleted())
                .collect(Collectors.toList());
        events.sort(Comparator.comparingLong(SyncJobEvent::getTimestamp).reversed());

        for(int j = 0; j<400; j += 50) {
            List<SyncJobEvent> retrievedEvents = syncJobEventDAOImp.selectMostRecent(j);

            Assert.assertEquals(events.stream().limit(j).toList(), retrievedEvents);
        }
    }


    @Test
    public void pruneOldDeletedByLocalDirectoryRoot() throws Exception {
        createDatabaseIfNotExists();

        List<SyncJobEvent> events = new ArrayList<>();
        long now = System.currentTimeMillis();

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
            events.add(SyncJobEvent.builder().syncDirection(SyncJobEvent.SyncDirection.UP)
                    .sourceTimestamp(System.currentTimeMillis())
                    .destinationTimestamp(null)
                    .timestamp(now += ThreadLocalRandom.current().nextInt(0, 10000))
                    .entityPermId("" + i)
                    .localDirectoryRoot("loc-root")
                    .localFile("loc-incomplete" + i)
                    .remoteFile("rem-incomplete" + i)
                    .build());
            events.add(SyncJobEvent.builder().syncDirection(SyncJobEvent.SyncDirection.DOWN)
                    .sourceTimestamp(System.currentTimeMillis())
                    .destinationTimestamp(null)
                    .timestamp(now += ThreadLocalRandom.current().nextInt(0, 10000))
                    .entityPermId("" + i)
                    .localDirectoryRoot("loc-root2")
                    .localFile("loc" + i)
                    .remoteFile("rem" + i)
                    .sourceDeleted(true)
                    .build());
        }

        SyncJobEventDAOImp syncJobEventDAOImp = new SyncJobEventDAOImp(new Configuration(Path.of(localDirectoryRoot)));
        Collections.shuffle(events);
        syncJobEventDAOImp.clearAll();
        for(SyncJobEvent syncJobEvent: events) {
            syncJobEventDAOImp.insertOrUpdate(syncJobEvent);
        }

        syncJobEventDAOImp.pruneOldDeletedByLocalDirectoryRoot("loc-root", 120);

        events.stream().filter( syncJobEvent -> "loc-root".equals(syncJobEvent.getLocalDirectoryRoot()))
                        .sorted(Comparator.comparing(SyncJobEvent::getTimestamp).reversed())
                        .limit(120)
                .forEach(new Consumer<SyncJobEvent>() {
                    @Override
                    @SneakyThrows
                    public void accept(SyncJobEvent syncJobEvent) {
                        Assert.assertNotNull(syncJobEventDAOImp.selectByPrimaryKey(syncJobEvent.getSyncDirection(), syncJobEvent.getLocalFile(), syncJobEvent.getRemoteFile()));

                    }
                });

        events.stream().filter( syncJobEvent -> "loc-root".equals(syncJobEvent.getLocalDirectoryRoot()))
                        .sorted(Comparator.comparing(SyncJobEvent::getTimestamp).reversed())
                        .skip(120)
                .forEach(new Consumer<SyncJobEvent>() {
                    @Override
                    @SneakyThrows
                    public void accept(SyncJobEvent syncJobEvent) {
                        Assert.assertNull(syncJobEventDAOImp.selectByPrimaryKey(syncJobEvent.getSyncDirection(), syncJobEvent.getLocalFile(), syncJobEvent.getRemoteFile()));
                    }
                });
    }

    @Test
    public void removeByJobKey() throws Exception {
        createDatabaseIfNotExists();

        String uuid = UUID.randomUUID().toString();
        SyncJobEvent testJobEvent = SyncJobEvent.builder()
                .syncDirection(SyncJobEvent.SyncDirection.UP)
                .localFile("a.txt")
                .remoteFile("a.txt")
                .entityPermId(uuid)
                .localDirectoryRoot(localDirectoryRoot)
                .sourceTimestamp(Instant.now().toEpochMilli())
                .destinationTimestamp(Instant.now().toEpochMilli())
                .timestamp(Instant.now().toEpochMilli())
                .build();

        SyncJobEventDAOImp syncJobEventDAOImp = new SyncJobEventDAOImp(new Configuration(Path.of(localDirectoryRoot)));
        syncJobEventDAOImp.insertOrUpdate(testJobEvent);
        SyncJobEvent resultJobEvent = syncJobEventDAOImp.selectByPrimaryKey(testJobEvent.getSyncDirection(), testJobEvent.getLocalFile(), testJobEvent.getRemoteFile());
        Assert.assertEquals(testJobEvent, resultJobEvent);
        syncJobEventDAOImp.removeByLocalDirectoryRoot(localDirectoryRoot);
        Assert.assertEquals(null, syncJobEventDAOImp.selectByPrimaryKey(testJobEvent.getSyncDirection(), testJobEvent.getLocalFile(), testJobEvent.getRemoteFile()));
    }

    @Test
    public void testClearAll() throws Exception {
        createDatabaseIfNotExists();

        List<SyncJobEvent> events = new ArrayList<>();
        long now = System.currentTimeMillis();

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

        SyncJobEventDAOImp syncJobEventDAOImp = new SyncJobEventDAOImp(new Configuration(Path.of(localDirectoryRoot)));
        Collections.shuffle(events);

        syncJobEventDAOImp.clearAll();
        for(SyncJobEvent syncJobEvent: events) {
            syncJobEventDAOImp.insertOrUpdate(syncJobEvent);
        }

        Assert.assertEquals(200, syncJobEventDAOImp.selectMostRecent(10000).size());
        syncJobEventDAOImp.clearAll();
        Assert.assertEquals(0, syncJobEventDAOImp.selectMostRecent(10000).size());
    }
}