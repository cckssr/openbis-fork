package ch.openbis.drive.db;

import ch.openbis.drive.DriveTestCase;
import ch.openbis.drive.conf.Configuration;
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
public class SyncJobEventDAOImpTest extends DriveTestCase {
    private final String localDirectoryRoot = Path.of(
            this.getClass().getClassLoader().getResource("placeholder.txt").getPath()).getParent()
            .resolve("sync-job-event-dao-impl-test")
            .toAbsolutePath().toString();

    @Test
    public void createDatabaseIfNotExists() throws Exception {
        Configuration configuration = new Configuration(Path.of(localDirectoryRoot));
        Files.createDirectories(configuration.getLocalAppStateDirectory());
        SyncJobEventDAOImp syncJobEventDAOImp = new SyncJobEventDAOImp(configuration);
        syncJobEventDAOImp.createDatabaseIfNotExists();
    }

    @Test
    public void insertOrUpdate() throws Exception {
        createDatabaseIfNotExists();

        for(boolean directory : List.of(Boolean.FALSE, Boolean.TRUE)) {
            for(boolean sourceDeleted : List.of(Boolean.FALSE, Boolean.TRUE)) {
                SyncJobEvent testJobEvent = SyncJobEvent.builder()
                        .syncDirection(SyncJobEvent.SyncDirection.UP)
                        .localFile("001_a.txt")
                        .remoteFile("001_a.txt")
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
                .localFile("002_a.txt")
                .remoteFile("002_a.txt")
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
                    .timestamp(now += UNIQUE_RANDOM_LONG_INTEGERS.get(3 * i))
                    .entityPermId("" + i)
                    .localDirectoryRoot("003_loc-root")
                    .localFile("loc" + i)
                    .remoteFile("rem" + i)
                    .build());
            events.add(SyncJobEvent.builder().syncDirection(SyncJobEvent.SyncDirection.UP)
                    .sourceTimestamp(System.currentTimeMillis())
                    .destinationTimestamp(null)
                    .timestamp(now += UNIQUE_RANDOM_LONG_INTEGERS.get(3 * i + 1))
                    .entityPermId("" + i)
                    .localDirectoryRoot("003_loc-root")
                    .localFile("loc-incomplete" + i)
                    .remoteFile("rem-incomplete" + i)
                    .build());
            events.add(SyncJobEvent.builder().syncDirection(SyncJobEvent.SyncDirection.DOWN)
                    .sourceTimestamp(System.currentTimeMillis())
                    .destinationTimestamp(null)
                    .timestamp(now += UNIQUE_RANDOM_LONG_INTEGERS.get(3 * i + 2))
                    .entityPermId("" + i)
                    .localDirectoryRoot("003_loc-root2")
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
                    .timestamp(now += UNIQUE_RANDOM_LONG_INTEGERS.get(3 * i))
                    .entityPermId("" + i)
                    .localDirectoryRoot("004_loc-root")
                    .localFile("loc" + i)
                    .remoteFile("rem" + i)
                    .build());
            events.add(SyncJobEvent.builder().syncDirection(SyncJobEvent.SyncDirection.UP)
                    .sourceTimestamp(System.currentTimeMillis())
                    .destinationTimestamp(null)
                    .timestamp(now += UNIQUE_RANDOM_LONG_INTEGERS.get(3 * i + 1))
                    .entityPermId("" + i)
                    .localDirectoryRoot("004_loc-root")
                    .localFile("loc-incomplete" + i)
                    .remoteFile("rem-incomplete" + i)
                    .build());
            events.add(SyncJobEvent.builder().syncDirection(SyncJobEvent.SyncDirection.DOWN)
                    .sourceTimestamp(System.currentTimeMillis())
                    .destinationTimestamp(null)
                    .timestamp(now += UNIQUE_RANDOM_LONG_INTEGERS.get(3 * i + 2))
                    .entityPermId("" + i)
                    .localDirectoryRoot("004_loc-root2")
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

        syncJobEventDAOImp.pruneOldDeletedByLocalDirectoryRoot("004_loc-root", 120);

        events.stream().filter( syncJobEvent -> "004_loc-root".equals(syncJobEvent.getLocalDirectoryRoot()))
                        .sorted(Comparator.comparing(SyncJobEvent::getTimestamp).reversed())
                        .limit(120)
                .forEach(new Consumer<SyncJobEvent>() {
                    @Override
                    @SneakyThrows
                    public void accept(SyncJobEvent syncJobEvent) {
                        Assert.assertNotNull(syncJobEventDAOImp.selectByPrimaryKey(syncJobEvent.getSyncDirection(), syncJobEvent.getLocalFile(), syncJobEvent.getRemoteFile()));

                    }
                });

        events.stream().filter( syncJobEvent -> "004_loc-root".equals(syncJobEvent.getLocalDirectoryRoot()))
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
                .localFile("005_a.txt")
                .remoteFile("005_a.txt")
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
                    .timestamp(now += UNIQUE_RANDOM_LONG_INTEGERS.get(2 * i))
                    .entityPermId("" + i)
                    .localDirectoryRoot("006_loc-root")
                    .localFile("loc" + i)
                    .remoteFile("rem" + i)
                    .build());
            events.add(SyncJobEvent.builder().syncDirection(SyncJobEvent.SyncDirection.DOWN)
                    .sourceTimestamp(System.currentTimeMillis())
                    .destinationTimestamp(System.currentTimeMillis())
                    .timestamp(now += UNIQUE_RANDOM_LONG_INTEGERS.get(2 * i + 1))
                    .entityPermId("" + i)
                    .localDirectoryRoot("006_loc-root2")
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

    public static final HashSet<Long> RANDOM_LONG_INTEGERS_SET = new HashSet<>();
    public static final List<Long> UNIQUE_RANDOM_LONG_INTEGERS;
    static {
        while ( RANDOM_LONG_INTEGERS_SET.size() < 1000 ) {
            RANDOM_LONG_INTEGERS_SET.add(ThreadLocalRandom.current().nextLong(0, 1000000));
        }
        UNIQUE_RANDOM_LONG_INTEGERS = RANDOM_LONG_INTEGERS_SET.stream().toList();
    }
}