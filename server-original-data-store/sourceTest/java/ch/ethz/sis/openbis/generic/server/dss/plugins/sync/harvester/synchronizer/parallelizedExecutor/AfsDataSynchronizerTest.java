/*
 * Copyright ETH 2026 Zurich, Scientific IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.ethz.sis.openbis.generic.server.dss.plugins.sync.harvester.synchronizer.parallelizedExecutor;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import ch.ethz.sis.afsclient.client.AfsClient;
import ch.ethz.sis.openbis.generic.server.dss.plugins.sync.harvester.synchronizer.IncomingAfsFile;
import ch.systemsx.cisd.common.exceptions.Status;

public class AfsDataSynchronizerTest
{
    private static final String OWNER = "owner";

    private File tempDirectory;

    @BeforeMethod
    public void createTempDirectory() throws Exception
    {
        tempDirectory = Files.createTempDirectory("afs-data-synchronizer-test").toFile();
    }

    @AfterMethod
    public void deleteTempDirectory()
    {
        FileUtils.deleteQuietly(tempDirectory);
    }

    @Test
    public void testMapsLiveSnapshotToLiveFile()
    {
        String path = "/Hello/.afs.snapshots/file.txt/2026_08_25_10_00_00_000";

        assertEquals(AfsDataSynchronizer.livePathForSnapshot(path), "/Hello/file.txt");
    }

    @Test
    public void testMapsTrashedSnapshotToLiveFile()
    {
        String path = "/.afs.trash/Hello/.afs.snapshots/file.txt/2026_08_25_10_00_00_000";

        assertEquals(AfsDataSynchronizer.livePathForSnapshot(path), "/Hello/file.txt");
    }

    @Test
    public void testIdentifiesBackupPaths()
    {
        assertTrue(AfsDataSynchronizer.isBackupPath("/.afs-sync-backup-123"));
        assertTrue(AfsDataSynchronizer.isBackupPath("/.afs-sync-backup-123/entry-0"));
        assertFalse(AfsDataSynchronizer.isBackupPath("/Hello/file.txt"));
    }

    @Test
    public void testRebuildsTrashedSnapshotWithoutTrashedCurrentFile() throws Exception
    {
        InMemoryAfsApi source = new InMemoryAfsApi();
        InMemoryAfsApi harvester = new InMemoryAfsApi();
        IncomingAfsFile snapshot = sourceFile(source,
                "/.afs.trash/Hello/.afs.snapshots/file.txt/2026_01_01_00_00_00_001", "history");
        AfsDataSynchronizer.AfsOwner owner = new AfsDataSynchronizer.AfsOwner(OWNER, Collections.emptyList(), List.of("/Hello"),
                Collections.emptyList(), Collections.emptyList(), List.of(snapshot));

        Status status = synchronizer(source, harvester).execute(List.of(owner));

        assertTrue(status.isOK());
        String snapshotPath = onlyPathBelow(harvester, "/.afs.trash/Hello/.afs.snapshots/file.txt/");
        assertEquals(text(harvester.content(OWNER, snapshotPath)), "history");
        assertNull(harvester.content(OWNER, "/Hello/file.txt"));
        assertNull(harvester.content(OWNER, "/.afs.trash/Hello/file.txt"));
    }

    @Test
    public void testRebuildsSnapshotsOldestFirst() throws Exception
    {
        InMemoryAfsApi source = new InMemoryAfsApi();
        InMemoryAfsApi harvester = new InMemoryAfsApi();
        IncomingAfsFile current = sourceFile(source, "/Hello/file.txt", "current");
        IncomingAfsFile newer = sourceFile(source,
                "/Hello/.afs.snapshots/file.txt/2026_01_01_00_00_00_002", "newer");
        IncomingAfsFile older = sourceFile(source,
                "/Hello/.afs.snapshots/file.txt/2026_01_01_00_00_00_001", "older");
        AfsDataSynchronizer.AfsOwner owner = owner(List.of(current), Collections.emptyList(), List.of(newer, older));

        Status status = synchronizer(source, harvester).execute(List.of(owner));

        assertTrue(status.isOK());
        List<String> snapshotPaths = pathsBelow(harvester, "/Hello/.afs.snapshots/file.txt/");
        assertEquals(snapshotPaths.size(), 2);
        assertEquals(text(harvester.content(OWNER, snapshotPaths.get(0))), "older");
        assertEquals(text(harvester.content(OWNER, snapshotPaths.get(1))), "newer");
        assertEquals(text(harvester.content(OWNER, "/Hello/file.txt")), "current");
    }

    @Test
    public void testRestoresBackupWhenRebuildFails() throws Exception
    {
        InMemoryAfsApi source = new InMemoryAfsApi();
        InMemoryAfsApi harvester = new InMemoryAfsApi();
        IncomingAfsFile desired = sourceFile(source, "/Hello/file.txt", "new");
        IncomingAfsFile unchanged = sourceFile(source, "/Hello/unchanged.txt", "unchanged");
        harvester.addFile(OWNER, "/Hello/file.txt", bytes("old"));
        harvester.addFile(OWNER, unchanged.getPath(), bytes("unchanged"));
        source.clearOperations();
        harvester.clearOperations();
        source.setFailReads(true);

        Status status = synchronizer(source, harvester).execute(
                List.of(owner(List.of(desired, unchanged), Collections.emptyList(), Collections.emptyList())));

        assertTrue(status.isError());
        assertEquals(text(harvester.content(OWNER, "/Hello/file.txt")), "old");
        assertEquals(text(harvester.content(OWNER, unchanged.getPath())), "unchanged");
        assertFalse(harvester.fileMutationPaths().contains(unchanged.getPath()));
        assertFalse(harvester.filePaths(OWNER).stream().anyMatch(AfsDataSynchronizer::isBackupPath));
    }

    @Test
    public void testRestoresChangedFileWhenVerificationFailsAfterUpload() throws Exception
    {
        InMemoryAfsApi source = new InMemoryAfsApi();
        InMemoryAfsApi harvester = new InMemoryAfsApi();
        source.addFile(OWNER, "/Hello/file.txt", bytes("new"));
        IncomingAfsFile desiredWithWrongHash = new IncomingAfsFile("/Hello/file.txt", 3,
                "2026-01-01T00:00:00Z", "wrong-hash");
        IncomingAfsFile unchanged = sourceFile(source, "/Hello/unchanged.txt", "unchanged");
        harvester.addFile(OWNER, "/Hello/file.txt", bytes("old"));
        harvester.addFile(OWNER, unchanged.getPath(), bytes("unchanged"));
        harvester.clearOperations();

        Status status = synchronizer(source, harvester).execute(
                List.of(owner(List.of(desiredWithWrongHash, unchanged), Collections.emptyList(), Collections.emptyList())));

        assertTrue(status.isError());
        assertEquals(text(harvester.content(OWNER, "/Hello/file.txt")), "old");
        assertEquals(text(harvester.content(OWNER, unchanged.getPath())), "unchanged");
        assertFalse(harvester.fileMutationPaths().contains(unchanged.getPath()));
        assertFalse(harvester.filePaths(OWNER).stream().anyMatch(AfsDataSynchronizer::isBackupPath));
    }

    @Test
    public void testBacksUpRootTrashedFileWithSnapshot() throws Exception
    {
        InMemoryAfsApi source = new InMemoryAfsApi();
        InMemoryAfsApi harvester = new InMemoryAfsApi();
        harvester.addFile(OWNER, "/old.txt", bytes("old"));
        harvester.snapshot(OWNER, "/old.txt");
        harvester.delete(OWNER, "/old.txt", true);
        AfsDataSynchronizer.AfsOwner owner = owner(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        Status status = synchronizer(source, harvester).execute(List.of(owner));

        assertTrue(status.isOK());
        assertTrue(harvester.filePaths(OWNER).isEmpty());
    }

    @Test
    public void testReplacesTrashedFileWithoutLosingLiveFileAtSamePath() throws Exception
    {
        InMemoryAfsApi source = new InMemoryAfsApi();
        InMemoryAfsApi harvester = new InMemoryAfsApi();
        IncomingAfsFile live = sourceFile(source, "/Hello/file.txt", "live");
        IncomingAfsFile trashed = sourceFile(source, "/.afs.trash/Hello/file.txt", "new trash");
        harvester.addFile(OWNER, live.getPath(), bytes("old trash"));
        harvester.delete(OWNER, live.getPath(), true);
        harvester.addFile(OWNER, live.getPath(), bytes("live"));

        Status status = synchronizer(source, harvester).execute(
                List.of(owner(List.of(live), List.of(trashed), Collections.emptyList())));

        assertTrue(status.isOK());
        assertEquals(text(harvester.content(OWNER, live.getPath())), "live");
        assertEquals(text(harvester.content(OWNER, trashed.getPath())), "new trash");
    }

    @Test
    public void testCreatesTrashedDirectoryWithoutLosingLiveSubtreeAtSamePath() throws Exception
    {
        InMemoryAfsApi source = new InMemoryAfsApi();
        InMemoryAfsApi harvester = new InMemoryAfsApi();
        IncomingAfsFile live = sourceFile(source, "/Hello/live.txt", "live");
        harvester.addFile(OWNER, live.getPath(), bytes("live"));
        AfsDataSynchronizer.AfsOwner owner = new AfsDataSynchronizer.AfsOwner(OWNER, List.of(live),
                Collections.emptyList(), Collections.emptyList(), List.of("/.afs.trash/Hello"), Collections.emptyList());

        Status status = synchronizer(source, harvester).execute(List.of(owner));

        assertTrue(status.isOK());
        assertEquals(text(harvester.content(OWNER, live.getPath())), "live");
        assertTrue(harvester.directoryPaths(OWNER).contains("/.afs.trash/Hello"));
    }

    @Test
    public void testRetriesListWhenOwnerPathIsBusy()
    {
        InMemoryAfsApi source = new InMemoryAfsApi();
        InMemoryAfsApi harvester = new InMemoryAfsApi();
        harvester.setBusyListFailures(1);
        AfsDataSynchronizer.AfsOwner owner = owner(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        Status status = synchronizer(source, harvester).execute(List.of(owner));

        assertTrue(status.isOK());
    }

    @Test
    public void testSecondSynchronizationHasSameLogicalState() throws Exception
    {
        InMemoryAfsApi source = new InMemoryAfsApi();
        InMemoryAfsApi harvester = new InMemoryAfsApi();
        IncomingAfsFile live = sourceFile(source, "/Hello/file.txt", "live");
        IncomingAfsFile trashed = sourceFile(source, "/.afs.trash/Hello/file.txt", "trashed");
        IncomingAfsFile liveSnapshot = sourceFile(source,
                "/Hello/.afs.snapshots/file.txt/2026_01_01_00_00_00_002", "live-history");
        IncomingAfsFile trashSnapshot = sourceFile(source,
                "/.afs.trash/Hello/.afs.snapshots/file.txt/2026_01_01_00_00_00_001", "trash-history");
        AfsDataSynchronizer.AfsOwner owner = owner(List.of(live), List.of(trashed), List.of(liveSnapshot, trashSnapshot));
        AfsDataSynchronizer synchronizer = synchronizer(source, harvester);

        assertTrue(synchronizer.execute(List.of(owner)).isOK());
        List<String> firstState = logicalState(harvester);
        source.setFailReads(true);
        assertTrue(synchronizer.execute(List.of(owner)).isOK());

        assertEquals(logicalState(harvester), firstState);
    }

    @Test
    public void testReusesMatchingDestinationFileAndDownloadsOnlyMissingFile() throws Exception
    {
        InMemoryAfsApi source = new InMemoryAfsApi();
        InMemoryAfsApi harvester = new InMemoryAfsApi();
        IncomingAfsFile alreadyUploaded = sourceFile(source, "/Hello/already.txt", "already uploaded");
        IncomingAfsFile missing = sourceFile(source, "/Hello/missing.txt", "missing");
        harvester.addFile(OWNER, alreadyUploaded.getPath(), bytes("already uploaded"));
        source.clearOperations();
        harvester.clearOperations();

        Status status = synchronizer(source, harvester).execute(
                List.of(owner(List.of(alreadyUploaded, missing), Collections.emptyList(), Collections.emptyList())));

        assertTrue(status.isOK());
        assertEquals(source.readSources(), Set.of(missing.getPath()));
        assertFalse(harvester.fileMutationPaths().contains(alreadyUploaded.getPath()));
        assertEquals(text(harvester.content(OWNER, alreadyUploaded.getPath())), "already uploaded");
        assertEquals(text(harvester.content(OWNER, missing.getPath())), "missing");
    }

    @Test
    public void testBacksUpAndReplacesOnlyChangedFile() throws Exception
    {
        InMemoryAfsApi source = new InMemoryAfsApi();
        InMemoryAfsApi harvester = new InMemoryAfsApi();
        IncomingAfsFile changed = sourceFile(source, "/Hello/changed.txt", "new");
        IncomingAfsFile unchanged = sourceFile(source, "/Hello/unchanged.txt", "unchanged");
        IncomingAfsFile snapshot = sourceFile(source,
                "/Hello/.afs.snapshots/changed.txt/2026_01_01_00_00_00_001", "history");
        harvester.addFile(OWNER, changed.getPath(), bytes("history"));
        harvester.snapshot(OWNER, changed.getPath());
        harvester.addFile(OWNER, changed.getPath(), bytes("old"));
        harvester.addFile(OWNER, unchanged.getPath(), bytes("unchanged"));
        String existingSnapshotPath = onlyPathBelow(harvester, "/Hello/.afs.snapshots/changed.txt/");
        source.clearOperations();
        harvester.clearOperations();

        Status status = synchronizer(source, harvester).execute(
                List.of(owner(List.of(changed, unchanged), Collections.emptyList(), List.of(snapshot))));

        assertTrue(status.isOK());
        assertEquals(source.readSources(), Set.of(changed.getPath()));
        assertEquals(harvester.copySources(), Set.of(changed.getPath()));
        assertFalse(harvester.fileMutationPaths().contains(unchanged.getPath()));
        assertFalse(harvester.fileMutationPaths().contains(existingSnapshotPath));
        assertEquals(text(harvester.content(OWNER, changed.getPath())), "new");
        assertEquals(text(harvester.content(OWNER, unchanged.getPath())), "unchanged");
        assertEquals(text(harvester.content(OWNER, existingSnapshotPath)), "history");
    }

    @Test
    public void testRebuildsSnapshotHistoryWhenChronologyDiffers() throws Exception
    {
        InMemoryAfsApi source = new InMemoryAfsApi();
        InMemoryAfsApi harvester = new InMemoryAfsApi();
        IncomingAfsFile current = sourceFile(source, "/Hello/file.txt", "current");
        IncomingAfsFile first = sourceFile(source,
                "/Hello/.afs.snapshots/file.txt/2026_01_01_00_00_00_001", "first");
        IncomingAfsFile second = sourceFile(source,
                "/Hello/.afs.snapshots/file.txt/2026_01_01_00_00_00_002", "second");
        harvester.addFile(OWNER, current.getPath(), bytes("second"));
        harvester.snapshot(OWNER, current.getPath());
        harvester.addFile(OWNER, current.getPath(), bytes("first"));
        harvester.snapshot(OWNER, current.getPath());
        harvester.addFile(OWNER, current.getPath(), bytes("current"));

        Status status = synchronizer(source, harvester).execute(
                List.of(owner(List.of(current), Collections.emptyList(), List.of(first, second))));

        assertTrue(status.isOK());
        List<String> snapshotPaths = pathsBelow(harvester, "/Hello/.afs.snapshots/file.txt/");
        assertEquals(text(harvester.content(OWNER, snapshotPaths.get(0))), "first");
        assertEquals(text(harvester.content(OWNER, snapshotPaths.get(1))), "second");
    }

    @Test
    public void testRemovesParentDirectoryWhenItsLastFileIsRemoved() throws Exception
    {
        InMemoryAfsApi source = new InMemoryAfsApi();
        InMemoryAfsApi harvester = new InMemoryAfsApi();
        harvester.addFile(OWNER, "/Hello/removed.txt", bytes("removed"));

        Status status = synchronizer(source, harvester).execute(
                List.of(owner(Collections.emptyList(), Collections.emptyList(), Collections.emptyList())));

        assertTrue(status.isOK());
        assertFalse(harvester.directoryPaths(OWNER).contains("/Hello"));
    }

    @Test
    public void testRemovesEmptySnapshotDirectoryAfterSourceSnapshotIsDeletedAndFileIsMoved() throws Exception
    {
        InMemoryAfsApi source = new InMemoryAfsApi();
        InMemoryAfsApi harvester = new InMemoryAfsApi();
        IncomingAfsFile moved = sourceFile(source, "/Hello/moved.txt", "moved");
        harvester.addFile(OWNER, moved.getPath(), bytes("moved"));
        String staleSnapshotDirectory = "/Hello/.afs.snapshots/original.txt";
        harvester.create(OWNER, staleSnapshotDirectory, true);

        Status status = synchronizer(source, harvester).execute(
                List.of(owner(List.of(moved), Collections.emptyList(), Collections.emptyList())));

        assertTrue(status.isOK());
        assertFalse(harvester.directoryPaths(OWNER).contains(staleSnapshotDirectory));
    }

    @Test
    public void testReusesMatchingSnapshotHistoryAndDownloadsOnlyMissingFile() throws Exception
    {
        InMemoryAfsApi source = new InMemoryAfsApi();
        InMemoryAfsApi harvester = new InMemoryAfsApi();
        IncomingAfsFile current = sourceFile(source, "/Hello/file.txt", "current");
        IncomingAfsFile missing = sourceFile(source, "/Hello/missing.txt", "missing");
        IncomingAfsFile snapshot = sourceFile(source,
                "/Hello/.afs.snapshots/file.txt/2026_01_01_00_00_00_001", "history");
        harvester.addFile(OWNER, current.getPath(), bytes("history"));
        harvester.snapshot(OWNER, current.getPath());
        harvester.addFile(OWNER, current.getPath(), bytes("current"));
        source.clearOperations();
        harvester.clearOperations();

        Status status = synchronizer(source, harvester).execute(
                List.of(owner(List.of(current, missing), Collections.emptyList(), List.of(snapshot))));

        assertTrue(status.isOK());
        assertEquals(source.readSources(), Set.of(missing.getPath()));
        assertFalse(harvester.fileMutationPaths().contains(current.getPath()));
        assertEquals(text(harvester.content(OWNER, current.getPath())), "current");
        assertEquals(text(harvester.content(OWNER, onlyPathBelow(harvester,
                "/Hello/.afs.snapshots/file.txt/"))), "history");
    }

    @Test
    public void testResumesFromRecoveryBackupLeftByInterruptedProcess() throws Exception
    {
        InMemoryAfsApi source = new InMemoryAfsApi();
        InMemoryAfsApi harvester = new InMemoryAfsApi();
        IncomingAfsFile desired = sourceFile(source, "/Hello/file.txt", "uploaded before interruption");
        harvester.addFile(OWNER, "/.afs-sync-backup-interrupted/entry-0/file.txt",
                bytes("uploaded before interruption"));

        Status status = synchronizer(source, harvester).execute(
                List.of(owner(List.of(desired), Collections.emptyList(), Collections.emptyList())));

        assertTrue(status.isOK());
        assertTrue(source.readSources().isEmpty());
        assertEquals(text(harvester.content(OWNER, desired.getPath())), "uploaded before interruption");
        assertFalse(harvester.filePaths(OWNER).stream().anyMatch(AfsDataSynchronizer::isBackupPath));
    }

    private AfsDataSynchronizer synchronizer(InMemoryAfsApi source, InMemoryAfsApi harvester)
    {
        return new AfsDataSynchronizer(client(source), client(harvester), tempDirectory,
                new AfsDataSynchronizationSummary(), false);
    }

    private static AfsClient client(InMemoryAfsApi api)
    {
        AfsClient client = new AfsClient(api, 1024, 30000);
        client.setSessionToken("session");
        return client;
    }

    private static AfsDataSynchronizer.AfsOwner owner(List<IncomingAfsFile> liveFiles,
            List<IncomingAfsFile> trashedFiles, List<IncomingAfsFile> snapshots)
    {
        return new AfsDataSynchronizer.AfsOwner(OWNER, liveFiles, Collections.emptyList(), trashedFiles,
                Collections.emptyList(), snapshots);
    }

    private static IncomingAfsFile sourceFile(InMemoryAfsApi source, String path, String content) throws Exception
    {
        byte[] bytes = bytes(content);
        source.addFile(OWNER, path, bytes);
        return new IncomingAfsFile(path, bytes.length, "2026-01-01T00:00:00Z", source.hash(OWNER, path));
    }

    private static String onlyPathBelow(InMemoryAfsApi api, String prefix)
    {
        List<String> paths = pathsBelow(api, prefix);
        assertEquals(paths.size(), 1);
        return paths.get(0);
    }

    private static List<String> pathsBelow(InMemoryAfsApi api, String prefix)
    {
        List<String> paths = api.filePaths(OWNER).stream().filter(path -> path.startsWith(prefix)).sorted().toList();
        return paths;
    }

    private static List<String> logicalState(InMemoryAfsApi api) throws Exception
    {
        List<String> state = new ArrayList<>();
        for (String path : api.filePaths(OWNER))
        {
            String logicalPath = path;
            if (path.contains("/.afs.snapshots/"))
            {
                logicalPath = path.substring(0, path.lastIndexOf('/') + 1);
            }
            state.add(logicalPath + "=" + api.hash(OWNER, path));
        }
        state.sort(Comparator.naturalOrder());
        return state;
    }

    private static byte[] bytes(String value)
    {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static String text(byte[] value)
    {
        return new String(value, StandardCharsets.UTF_8);
    }
}
