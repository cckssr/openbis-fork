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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FileUtils;

import ch.ethz.sis.afsapi.api.ClientAPI;
import ch.ethz.sis.afsclient.client.AfsClient;
import ch.ethz.sis.afsclient.client.AfsClientUploadHelper;
import ch.ethz.sis.openbis.generic.server.dss.plugins.sync.harvester.synchronizer.IncomingAfsFile;
import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.ethz.sis.shared.log.classic.impl.Logger;
import ch.systemsx.cisd.common.concurrent.ITaskExecutor;
import ch.systemsx.cisd.common.exceptions.Status;

/** Synchronizes changed logical file units while journaling only the destination state that will be replaced. */
public class AfsDataSynchronizer implements ITaskExecutor<List<AfsDataSynchronizer.AfsOwner>>
{
    private static final Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION, AfsDataSynchronizer.class);

    private static final String TRASH_PATH = "/.afs.trash";

    private static final String SNAPSHOTS_DIRECTORY = ".afs.snapshots";

    private static final String BACKUP_PATH_PREFIX = "/.afs-sync-backup-";

    private static final int PATH_BUSY_RETRIES = 5;

    private static final long PATH_BUSY_RETRY_DELAY_MILLIS = 1000;

    private final AfsClient sourceAfsClient;

    private final AfsClient harvesterAfsClient;

    private final File tempDirBase;

    private final AfsDataSynchronizationSummary summary;

    private final boolean dryRun;

    public AfsDataSynchronizer(AfsClient sourceAfsClient, AfsClient harvesterAfsClient, File tempDirBase,
            AfsDataSynchronizationSummary summary, boolean dryRun)
    {
        this.sourceAfsClient = sourceAfsClient;
        this.harvesterAfsClient = harvesterAfsClient;
        this.tempDirBase = tempDirBase;
        this.summary = summary;
        this.dryRun = dryRun;
    }

    @Override
    public Status execute(List<AfsOwner> owners)
    {
        for (AfsOwner owner : owners)
        {
            try
            {
                synchronize(owner);
            } catch (Exception e)
            {
                operationLog.error("AFS data synchronization failed for owner " + owner.permId(), e);
                return Status.createError("AFS data synchronization failed for owner " + owner.permId() + ": " + e.getMessage());
            }
        }
        return Status.OK;
    }

    private void synchronize(AfsOwner ownerDataSource) throws Exception
    {
        // ID used to address this owner's data on both AFS clients
        String ownerPermId = ownerDataSource.permId();
        // Recursive listing of everything currently on the harvester side, used only to decide "add" vs "update" for the summary count
        ch.ethz.sis.afsapi.dto.File[] existingEntries = list(ownerPermId, "/", true);

        if (dryRun)
        {
            // Dry run: just tally this owner as added/updated without touching any data
            recordResult(ownerDataSource, existingEntries.length > 0);
            // Skip all actual synchronization work
            return;
        }

        // Create the owner's root directory on the harvester if it doesn't exist yet
        ensureOwnerRootExists(ownerPermId);
        // Snapshot the harvester's current live/trashed/snapshot files and directories into an AfsOwner
        DestinationState harvesterState = captureHarvesterState(ownerPermId);
        // expected units from data source meta data
        // A "unit" here is one logical file identified by its canonical live path, bundling that
        // path's live hash, trash hash, and snapshot history together (see FileUnitState/fileUnits())
        Map<String, FileUnitState> sourceUnits = fileUnits(ownerDataSource);
        // Same per-live-path bundling, but for what the harvester currently has
        Map<String, FileUnitState> harvesterUnits = fileUnits(harvesterState.owner());

        // state that exists on harvester and has changed on source
        // Live paths whose live-file hash differs between source and harvester
        Set<String> affectedLiveFiles = affectedLiveFiles(sourceUnits, harvesterUnits);
        // Live paths whose trashed-file hash differs between source and harvester
        Set<String> affectedTrashedFiles = affectedTrashedFiles(sourceUnits, harvesterUnits);
        // Live paths whose snapshot history differs between source and harvester
        Set<String> affectedSnapshotUnits = affectedSnapshotUnits(sourceUnits, harvesterUnits);


        // Live paths that can't be patched piecemeal (e.g. trash and a coexisting live file/snapshot history
        // both changing) and so need their entire live+trash+snapshot bundle torn down and rebuilt together
        Set<String> fullRebuildUnits = fullRebuildUnits(sourceUnits, harvesterUnits, affectedTrashedFiles,
                affectedSnapshotUnits);
        // Union of every live path whose live, trash, or snapshot state changed — i.e. every affected unit
        Set<String> affectedFileUnits = new HashSet<>(affectedLiveFiles);
        // Fold in trash diffs
        affectedFileUnits.addAll(affectedTrashedFiles);
        // Fold in snapshot diffs
        affectedFileUnits.addAll(affectedSnapshotUnits);
        // Directories (live or trashed) that exist on only one side
        Set<String> affectedDirectories = affectedDirectories(ownerDataSource, harvesterState.owner());
        if (affectedFileUnits.isEmpty() && affectedDirectories.isEmpty())
        {
            // Stale empty snapshot containers (e.g. left behind by a source rename) are invisible to the
            // delta above, since they hold no files; remove them even though no other transfer is needed.
            removeUnexpectedEmptyDirectories(ownerDataSource);
            // Delete any stale backup folders left over from a previous run
            cleanupRecoveryBackups(ownerPermId);
            // Log that nothing needed to change
            operationLog.info("AFS data is already synchronized for owner " + ownerPermId + "; skipping transfer");
            // Nothing more to do for this owner
            return;
        }

        // Unique path under the owner to stage a rollback journal for this run
        String backupPath = BACKUP_PATH_PREFIX + UUID.randomUUID();
        // Just the source-side files/dirs that actually need to be written
        AfsOwner expectedDelta = delta(ownerDataSource, affectedLiveFiles, affectedTrashedFiles, fullRebuildUnits,
                affectedDirectories);
        // Just the harvester-side files/dirs that will be overwritten, kept so they can be restored on failure
        AfsOwner rollbackDelta = delta(harvesterState.owner(), affectedLiveFiles, affectedTrashedFiles,
                fullRebuildUnits, affectedDirectories);
        // Tracks whether the pre-mutation backup finished, to decide if rollback is safe
        boolean backupComplete = false;
        // Tracks whether any destructive change has begun, to decide if rollback is needed at all
        boolean mutationStarted = false;
        // Hash -> already-available path, so identical content doesn't get re-uploaded/re-copied
        Map<String, String> reusableFilesByHash = new HashMap<>();

        // Journal only state that this synchronization will mutate.
        try
        {
            // Create the backup directory on the harvester
            harvesterAfsClient.create(ownerPermId, backupPath, true);
            // Copy every file that's about to be overwritten into the backup path, recording their hashes
            backupFiles(rollbackDelta, backupPath, reusableFilesByHash);
            // Mark backup as done so a failure past this point knows rollback data exists
            backupComplete = true;
            // Also index any pre-existing backup files on the harvester (e.g. from a previous interrupted run) as reusable content
            reusableFilesByHash.putAll(reusableBackupFilesByHash(ownerPermId));
            // From here on, the harvester's live state is about to be modified
            mutationStarted = true;
            // Delete the harvester-side paths that are changing, per the rules for full-rebuild vs targeted units
            clearDelta(ownerPermId, sourceUnits, affectedLiveFiles, affectedTrashedFiles, fullRebuildUnits,
                    affectedDirectories);
            // Recreate trash, trashed dirs, snapshots, live files and live dirs to match the source
            rebuild(expectedDelta, reusableFilesByHash, backupPath);
            // Clean up any leftover empty directories not expected by the source
            removeUnexpectedEmptyDirectories(ownerDataSource);
            // Re-list the harvester and confirm it now matches the source exactly
            verify(ownerDataSource);
            // Verification passed, so the rollback backup is no longer needed
            harvesterAfsClient.delete(ownerPermId, backupPath, false);
            // Also remove any other stale backup folders found lying around
            cleanupRecoveryBackups(ownerPermId);
            // Tally this owner as added or updated in the summary
            recordResult(ownerDataSource, existingEntries.length > 0);
        } catch (Exception e)
        {
            // Only attempt rollback if there's a complete backup and a mutation actually began
            if (backupComplete && mutationStarted)
            {
                try
                {
                    // Undo the partial change by clearing the paths again, this time relative to the original harvester state
                    clearDelta(ownerPermId, harvesterUnits, affectedLiveFiles, affectedTrashedFiles, fullRebuildUnits,
                            affectedDirectories);
                    // Restore the original harvester content from the backup
                    rebuild(rollbackDelta, reusableFilesByHash, backupPath);
                    // Clean up empty directories relative to the restored (original) state
                    removeUnexpectedEmptyDirectories(harvesterState.owner());
                    // Confirm the harvester now matches its pre-sync state again
                    verify(harvesterState.owner());
                    // Rollback succeeded, so the backup can be discarded
                    harvesterAfsClient.delete(ownerPermId, backupPath, false);
                } catch (Exception restoreException)
                {
                    // Attach the rollback failure to the original error so both are visible
                    e.addSuppressed(restoreException);
                    // Surface a clear error: manual recovery is needed, and the backup path is preserved on purpose
                    throw new Exception("AFS delta update and rollback failed; recovery data remains at " + backupPath, e);
                }
            } else
            {
                // No real mutation happened yet, so just discard the (empty or partial) backup directory
                deleteIfPresent(ownerPermId, backupPath);
            }
            // Propagate the original failure to the caller regardless of rollback outcome
            throw e;
        }
    }

    private DestinationState captureHarvesterState(String ownerPermId) throws Exception
    {
        List<IncomingAfsFile> liveFiles = new ArrayList<>();
        List<IncomingAfsFile> trashedFiles = new ArrayList<>();
        List<IncomingAfsFile> snapshots = new ArrayList<>();
        Set<String> directories = new HashSet<>();
        Set<String> filePaths = new HashSet<>();

        for (ch.ethz.sis.afsapi.dto.File entry : list(ownerPermId, "/", true))
        {
            String path = entry.getPath();
            if (isBackupPath(path))
            {
                continue;
            }
            if (Boolean.TRUE.equals(entry.getDirectory()))
            {
                directories.add(path);
                continue;
            }

            filePaths.add(path);
            IncomingAfsFile file = new IncomingAfsFile(path, entry.getSize(), entry.getLastModifiedTime().toString(),
                    harvesterAfsClient.hash(ownerPermId, path));
            if (isInSnapshots(path))
            {
                snapshots.add(file);
            } else if (isInTrash(path))
            {
                trashedFiles.add(file);
            } else
            {
                liveFiles.add(file);
            }
        }

        Set<String> emptyDirectories = emptyDirectories(directories, filePaths);
        List<String> liveDirectories = emptyDirectories.stream().filter(path -> isInTrash(path) == false).toList();
        List<String> trashedDirectories = emptyDirectories.stream().filter(AfsDataSynchronizer::isInTrash).toList();
        return new DestinationState(new AfsOwner(ownerPermId, liveFiles, liveDirectories, trashedFiles,
                trashedDirectories, snapshots));
    }

    private Set<String> affectedLiveFiles(Map<String, FileUnitState> expected, Map<String, FileUnitState> actual)
    {
        return affectedComponents(expected, actual, Component.LIVE);
    }

    private Set<String> affectedTrashedFiles(Map<String, FileUnitState> expected, Map<String, FileUnitState> actual)
    {
        return affectedComponents(expected, actual, Component.TRASHED);
    }

    private Set<String> affectedSnapshotUnits(Map<String, FileUnitState> expected, Map<String, FileUnitState> actual)
    {
        return affectedComponents(expected, actual, Component.SNAPSHOTS);
    }

    private Set<String> affectedComponents(Map<String, FileUnitState> expected, Map<String, FileUnitState> actual,
            Component component)
    {
        Set<String> paths = new HashSet<>(expected.keySet());
        paths.addAll(actual.keySet());
        paths.removeIf(path -> Objects.equals(component.value(expected.getOrDefault(path, FileUnitState.EMPTY)),
                component.value(actual.getOrDefault(path, FileUnitState.EMPTY))));
        return paths;
    }

    private Set<String> fullRebuildUnits(Map<String, FileUnitState> expected, Map<String, FileUnitState> actual,
            Set<String> affectedTrashedFiles, Set<String> affectedSnapshotUnits)
    {
        Set<String> fullRebuildUnits = new HashSet<>(affectedSnapshotUnits);
        for (String path : affectedTrashedFiles)
        {
            FileUnitState expectedUnit = expected.getOrDefault(path, FileUnitState.EMPTY);
            FileUnitState actualUnit = actual.getOrDefault(path, FileUnitState.EMPTY);
            if (expectedUnit.liveHash != null || actualUnit.liveHash != null || expectedUnit.snapshots.isEmpty() == false
                    || actualUnit.snapshots.isEmpty() == false)
            {
                // AFS needs the live path to replace trash, so coexisting live state and snapshots form one unit.
                fullRebuildUnits.add(path);
            }
        }
        return fullRebuildUnits;
    }

    private Set<String> affectedDirectories(AfsOwner expected, AfsOwner actual)
    {
        Set<String> expectedDirectories = allDirectories(expected);
        Set<String> actualDirectories = allDirectories(actual);
        Set<String> affected = new HashSet<>(expectedDirectories);
        affected.addAll(actualDirectories);
        affected.removeIf(path -> expectedDirectories.contains(path) == actualDirectories.contains(path));
        return affected;
    }

    private AfsOwner delta(AfsOwner owner, Set<String> affectedLiveFiles, Set<String> affectedTrashedFiles,
            Set<String> fullRebuildUnits, Set<String> affectedDirectories)
    {
        List<IncomingAfsFile> liveFiles = owner.afsFiles().stream()
                .filter(file -> affectedLiveFiles.contains(file.getPath()) || fullRebuildUnits.contains(file.getPath()))
                .toList();
        List<IncomingAfsFile> trashedFiles = owner.trashedAfsFiles().stream()
                .filter(file -> affectedTrashedFiles.contains(livePathForTrashedPath(file.getPath()))
                        || fullRebuildUnits.contains(livePathForTrashedPath(file.getPath())))
                .toList();
        List<IncomingAfsFile> snapshots = owner.afsFileSnapshots().stream()
                .filter(file -> fullRebuildUnits.contains(livePathForSnapshot(file.getPath()))).toList();
        List<String> liveDirectories = owner.afsDirectories().stream().filter(affectedDirectories::contains).toList();
        List<String> trashedDirectories = owner.trashedAfsDirectories().stream()
                .filter(affectedDirectories::contains).toList();
        return new AfsOwner(owner.permId(), liveFiles, liveDirectories, trashedFiles, trashedDirectories, snapshots);
    }

    private void backupFiles(AfsOwner rollbackDelta, String backupPath, Map<String, String> reusableFilesByHash) throws Exception
    {
        List<IncomingAfsFile> files = new ArrayList<>();
        files.addAll(rollbackDelta.afsFiles());
        files.addAll(rollbackDelta.trashedAfsFiles());
        files.addAll(rollbackDelta.afsFileSnapshots());
        int backupNumber = 0;
        for (IncomingAfsFile file : files)
        {
            if (reusableFilesByHash.containsKey(file.getHash()))
            {
                continue;
            }
            String backedUpPath = backupPath + "/file-" + backupNumber++;
            harvesterAfsClient.copy(rollbackDelta.permId(), file.getPath(), rollbackDelta.permId(), backedUpPath);
            reusableFilesByHash.put(file.getHash(), backedUpPath);
        }
    }

    private void clearDelta(String ownerPermId, Map<String, FileUnitState> expectedUnits, Set<String> affectedLiveFiles,
            Set<String> affectedTrashedFiles, Set<String> fullRebuildUnits, Set<String> affectedDirectories) throws Exception
    {
        for (String livePath : fullRebuildUnits)
        {
            deleteIfPresent(ownerPermId, livePath);
            deleteIfPresent(ownerPermId, TRASH_PATH + livePath);
            deleteIfPresent(ownerPermId, snapshotsDirectoryFor(livePath));
            deleteIfPresent(ownerPermId, snapshotsDirectoryFor(TRASH_PATH + livePath));
        }
        for (String livePath : affectedLiveFiles)
        {
            if (fullRebuildUnits.contains(livePath) == false
                    && expectedUnits.getOrDefault(livePath, FileUnitState.EMPTY).liveHash == null)
            {
                deleteIfPresent(ownerPermId, livePath);
            }
        }
        for (String livePath : affectedTrashedFiles)
        {
            if (fullRebuildUnits.contains(livePath) == false)
            {
                deleteIfPresent(ownerPermId, TRASH_PATH + livePath);
            }
        }
        List<String> directories = affectedDirectories.stream()
                .sorted(Comparator.comparingInt(String::length).reversed()).toList();
        for (String path : directories)
        {
            deleteIfPresent(ownerPermId, path);
        }
    }

    private static Map<String, FileUnitState> fileUnits(AfsOwner owner)
    {
        Map<String, FileUnitState> units = new HashMap<>();

        for (IncomingAfsFile file : owner.afsFiles())
        {
            FileUnitState state = units.computeIfAbsent(
                    file.getPath(),
                    ignored -> new FileUnitState());

            state.liveHash = file.getHash();
        }

        for (IncomingAfsFile file : owner.trashedAfsFiles())
        {
            String livePath = livePathForTrashedPath(file.getPath());

            FileUnitState state = units.computeIfAbsent(
                    livePath,
                    ignored -> new FileUnitState());

            state.trashedHash = file.getHash();
        }

        List<IncomingAfsFile> snapshots = owner.afsFileSnapshots().stream()
                .sorted(Comparator.comparing((IncomingAfsFile file) -> snapshotName(file.getPath()))
                        .thenComparing(IncomingAfsFile::getPath))
                .toList();

        for (IncomingAfsFile file : snapshots)
        {
            String livePath = livePathForSnapshot(file.getPath());
            SnapshotVersion version = new SnapshotVersion(
                    isInTrash(file.getPath()),
                    file.getHash());

            FileUnitState state = units.computeIfAbsent(
                    livePath,
                    ignored -> new FileUnitState());

            state.snapshots.add(version);
        }

        return units;
    }

    private static Set<String> allDirectories(AfsOwner owner)
    {
        Set<String> directories = new HashSet<>(owner.afsDirectories());
        directories.addAll(owner.trashedAfsDirectories());
        return directories;
    }

    private void removeUnexpectedEmptyDirectories(AfsOwner expected) throws Exception
    {
        Set<String> directories = new HashSet<>();
        Set<String> filePaths = new HashSet<>();
        for (ch.ethz.sis.afsapi.dto.File entry : list(expected.permId(), "/", true))
        {
            if (isBackupPath(entry.getPath()))
            {
                continue;
            }
            if (Boolean.TRUE.equals(entry.getDirectory()))
            {
                directories.add(entry.getPath());
            } else
            {
                filePaths.add(entry.getPath());
            }
        }

        Set<String> unexpected = emptyDirectories(directories, filePaths);
        unexpected.removeAll(allDirectories(expected));
        unexpected.addAll(unexpectedSnapshotContainers(directories, filePaths, expected));
        for (String path : unexpected.stream().sorted(Comparator.comparingInt(String::length).reversed()).toList())
        {
            deleteIfPresent(expected.permId(), path);
        }
    }

    private Set<String> unexpectedSnapshotContainers(Set<String> directories, Set<String> filePaths, AfsOwner expected)
    {
        // A snapshot container (".../.afs.snapshots/<name>") is implied structure, not explicit directory
        // state, so it never surfaces as an "empty directory" above; but one left with no snapshot files
        // under it - e.g. after the source file was renamed - still has to be cleaned up explicitly.
        Set<String> expectedSnapshotUnits = new HashSet<>();
        for (IncomingAfsFile snapshot : expected.afsFileSnapshots())
        {
            expectedSnapshotUnits.add(livePathForSnapshot(snapshot.getPath()));
        }

        Set<String> unexpected = new HashSet<>();
        for (String directory : directories)
        {
            if (isSnapshotsContainerDirectory(directory) && hasDescendantFile(directory, filePaths) == false
                    && expectedSnapshotUnits.contains(livePathForSnapshotContainer(directory)) == false)
            {
                unexpected.add(directory);
            }
        }
        return unexpected;
    }

    private void cleanupRecoveryBackups(String ownerPermId)
    {
        try
        {
            for (ch.ethz.sis.afsapi.dto.File entry : list(ownerPermId, "/", false))
            {
                if (isBackupPath(entry.getPath()))
                {
                    try
                    {
                        harvesterAfsClient.delete(ownerPermId, entry.getPath(), false);
                    } catch (Exception e)
                    {
                        operationLog.warn("Could not remove stale AFS recovery backup " + entry.getPath() + " for owner "
                                + ownerPermId + ": " + e.getMessage());
                    }
                }
            }
        } catch (Exception e)
        {
            operationLog.warn("Could not list stale AFS recovery backups for owner " + ownerPermId + ": " + e.getMessage());
        }
    }

    private Map<String, String> reusableBackupFilesByHash(String ownerPermId) throws Exception
    {
        Map<String, String> reusableFilesByHash = new HashMap<>();
        for (ch.ethz.sis.afsapi.dto.File entry : list(ownerPermId, "/", true))
        {
            if (isBackupPath(entry.getPath()) && Boolean.TRUE.equals(entry.getDirectory()) == false)
            {
                String hash = harvesterAfsClient.hash(ownerPermId, entry.getPath());
                reusableFilesByHash.putIfAbsent(hash, entry.getPath());
            }
        }
        return reusableFilesByHash;
    }

    private void rebuild(AfsOwner owner, Map<String, String> reusableFilesByHash, String backupPath) throws Exception
    {
        // Build trash first so later trash moves cannot remove live history.
        rebuildTrashedFiles(owner, reusableFilesByHash);
        rebuildTrashedDirectories(owner, backupPath);
        rebuildSnapshots(owner, reusableFilesByHash);
        rebuildLiveFiles(owner, reusableFilesByHash);
        rebuildLiveDirectories(owner);
    }

    private void rebuildSnapshots(AfsOwner owner, Map<String, String> reusableFilesByHash) throws Exception
    {
        // Live and trashed snapshots share one logical file history.
        Map<String, List<IncomingAfsFile>> snapshotsByLivePath = new HashMap<>();

        for (IncomingAfsFile snapshot : owner.afsFileSnapshots())
        {
            String livePath = livePathForSnapshot(snapshot.getPath());
            snapshotsByLivePath.computeIfAbsent(livePath, ignored -> new ArrayList<>()).add(snapshot);
        }

        Map<String, IncomingAfsFile> liveFilesByPath = filesByPath(owner.afsFiles());
        for (Map.Entry<String, List<IncomingAfsFile>> entry : snapshotsByLivePath.entrySet())
        {
            IncomingAfsFile currentFile = liveFilesByPath.get(entry.getKey());
            rebuildSnapshotHistory(owner.permId(), entry.getKey(), entry.getValue(), currentFile, reusableFilesByHash);
        }
    }

    private void rebuildSnapshotHistory(String ownerPermId, String livePath, List<IncomingAfsFile> snapshots,
            IncomingAfsFile currentFile, Map<String, String> reusableFilesByHash) throws Exception
    {
        // Snapshot names preserve source chronology.
        snapshots.sort(Comparator.comparing(snapshot -> snapshotName(snapshot.getPath())));
        deleteIfPresent(ownerPermId, livePath);

        boolean hasLiveSnapshot = false;
        for (IncomingAfsFile snapshot : snapshots)
        {
            // AFS snapshots require a temporary live file.
            replaceStagingFile(ownerPermId, snapshot, livePath, reusableFilesByHash);
            String generatedSnapshot = createSnapshot(ownerPermId, livePath);
            if (isInTrash(snapshot.getPath()))
            {
                // Match the source snapshot placement.
                harvesterAfsClient.delete(ownerPermId, generatedSnapshot, true);
            } else
            {
                hasLiveSnapshot = true;
            }
        }

        if (currentFile != null)
        {
            // Restore the current file after its history.
            replaceStagingFile(ownerPermId, currentFile, livePath, reusableFilesByHash);
        } else if (hasLiveSnapshot)
        {
            throw new IllegalStateException("Live snapshot has no current file: " + livePath);
        } else
        {
            // Trash-only history must not leave a live file.
            deleteIfPresent(ownerPermId, livePath);
        }
    }

    private String createSnapshot(String ownerPermId, String livePath) throws Exception
    {
        // Generated names differ, so identify the new path by set difference.
        Set<String> snapshotsBefore = snapshotPaths(ownerPermId, livePath);
        harvesterAfsClient.snapshot(ownerPermId, livePath);

        Set<String> snapshotsAfter = snapshotPaths(ownerPermId, livePath);
        snapshotsAfter.removeAll(snapshotsBefore);
        if (snapshotsAfter.size() != 1)
        {
            throw new IllegalStateException("Could not identify generated snapshot for " + livePath);
        }
        return snapshotsAfter.iterator().next();
    }

    private void replaceStagingFile(String ownerPermId, IncomingAfsFile file, String livePath,
            Map<String, String> reusableFilesByHash) throws Exception
    {
        Set<String> snapshotsBefore = snapshotPaths(ownerPermId, livePath);
        restoreOrTransferFile(ownerPermId, file, livePath, reusableFilesByHash);

        // Upload overrides can create snapshots that are not in the source.
        Set<String> automaticSnapshots = snapshotPaths(ownerPermId, livePath);
        automaticSnapshots.removeAll(snapshotsBefore);
        for (String automaticSnapshot : automaticSnapshots)
        {
            harvesterAfsClient.delete(ownerPermId, automaticSnapshot, false);
        }
    }

    private void rebuildTrashedFiles(AfsOwner owner, Map<String, String> reusableFilesByHash) throws Exception
    {
        for (IncomingAfsFile file : owner.trashedAfsFiles())
        {
            // AFS only creates trash entries from live paths.
            String livePath = livePathForTrashedPath(file.getPath());
            deleteIfPresent(owner.permId(), livePath);
            restoreOrTransferFile(owner.permId(), file, livePath, reusableFilesByHash);
            harvesterAfsClient.delete(owner.permId(), livePath, true);
        }
    }

    private void rebuildTrashedDirectories(AfsOwner owner, String backupPath) throws Exception
    {
        for (String directory : owner.trashedAfsDirectories())
        {
            // AFS only creates trash entries from live paths.
            String livePath = livePathForTrashedPath(directory);
            String stagingPath = backupPath + "/staging-directory-" + UUID.randomUUID();
            boolean livePathStaged = false;
            if (AfsClientUploadHelper.getServerFilePresence(harvesterAfsClient, owner.permId(), livePath).isPresent())
            {
                // Trash creation needs this path; retain any unrelated live subtree server-side while it is used.
                harvesterAfsClient.move(owner.permId(), livePath, owner.permId(), stagingPath);
                livePathStaged = true;
            }
            try
            {
                transferDirectory(owner.permId(), livePath);
                harvesterAfsClient.delete(owner.permId(), livePath, true);
            } finally
            {
                if (livePathStaged)
                {
                    deleteIfPresent(owner.permId(), livePath);
                    harvesterAfsClient.move(owner.permId(), stagingPath, owner.permId(), livePath);
                }
            }
        }
    }

    private void rebuildLiveFiles(AfsOwner owner, Map<String, String> reusableFilesByHash) throws Exception
    {
        for (IncomingAfsFile file : owner.afsFiles())
        {
            String path = file.getPath();
            if (AfsClientUploadHelper.getServerFilePresence(harvesterAfsClient, owner.permId(), path).isPresent())
            {
                // Snapshot rebuilding may already have restored the desired current file.
                if (file.getHash().equals(harvesterAfsClient.hash(owner.permId(), path)))
                {
                    continue;
                }
                replaceStagingFile(owner.permId(), file, path, reusableFilesByHash);
                continue;
            }
            restoreOrTransferFile(owner.permId(), file, path, reusableFilesByHash);
        }
    }

    private void restoreOrTransferFile(String ownerPermId, IncomingAfsFile file, String destinationPath,
            Map<String, String> reusableFilesByHash) throws Exception
    {
        String reusablePath = reusableFilesByHash.get(file.getHash());
        if (reusablePath != null)
        {
            operationLog.info("Reusing AFS file " + reusablePath + " for " + destinationPath + " of owner " + ownerPermId);
            harvesterAfsClient.copy(ownerPermId, reusablePath, ownerPermId, destinationPath);
        } else
        {
            transferFile(ownerPermId, Paths.get(file.getPath()), Paths.get(destinationPath));
        }
    }

    private void rebuildLiveDirectories(AfsOwner owner) throws Exception
    {
        for (String directory : owner.afsDirectories())
        {
            transferDirectory(owner.permId(), directory);
        }
    }

    private void verify(AfsOwner owner) throws Exception
    {
        // Compare paths and hashes; generated timestamps may differ.
        Map<String, String> expectedFiles = new HashMap<>();
        for (IncomingAfsFile file : owner.afsFiles())
        {
            expectedFiles.put(file.getPath(), file.getHash());
        }
        for (IncomingAfsFile file : owner.trashedAfsFiles())
        {
            expectedFiles.put(file.getPath(), file.getHash());
        }

        Map<String, List<SnapshotVersion>> expectedSnapshots = snapshotHistories(owner.afsFileSnapshots());
        Map<String, String> actualFiles = new HashMap<>();
        List<ch.ethz.sis.afsapi.dto.File> actualSnapshotFiles = new ArrayList<>();
        Set<String> actualDirectories = new HashSet<>();
        Set<String> actualFilePaths = new HashSet<>();

        for (ch.ethz.sis.afsapi.dto.File entry : list(owner.permId(), "/", true))
        {
            String path = entry.getPath();
            if (isBackupPath(path))
            {
                // Recovery data is outside the mirrored state.
                continue;
            }
            if (Boolean.TRUE.equals(entry.getDirectory()))
            {
                actualDirectories.add(path);
            } else if (isInSnapshots(path))
            {
                actualFilePaths.add(path);
                actualSnapshotFiles.add(entry);
            } else
            {
                actualFilePaths.add(path);
                actualFiles.put(path, harvesterAfsClient.hash(owner.permId(), path));
            }
        }

        if (expectedFiles.equals(actualFiles) == false)
        {
            throw new IllegalStateException("Current AFS files do not match the datasource");
        }
        Map<String, List<SnapshotVersion>> actualSnapshots = new HashMap<>();
        actualSnapshotFiles.sort(Comparator
                .comparing((ch.ethz.sis.afsapi.dto.File file) -> snapshotName(file.getPath()))
                .thenComparing(ch.ethz.sis.afsapi.dto.File::getPath));
        for (ch.ethz.sis.afsapi.dto.File snapshot : actualSnapshotFiles)
        {
            String path = snapshot.getPath();
            SnapshotVersion version = new SnapshotVersion(isInTrash(path), harvesterAfsClient.hash(owner.permId(), path));
            actualSnapshots.computeIfAbsent(livePathForSnapshot(path), ignored -> new ArrayList<>()).add(version);
        }
        if (expectedSnapshots.equals(actualSnapshots) == false)
        {
            throw new IllegalStateException("AFS snapshots do not match the datasource");
        }
        Set<String> expectedEmptyDirectories = new HashSet<>(owner.afsDirectories());
        expectedEmptyDirectories.addAll(owner.trashedAfsDirectories());
        Set<String> actualEmptyDirectories = emptyDirectories(actualDirectories, actualFilePaths);
        if (expectedEmptyDirectories.equals(actualEmptyDirectories) == false)
        {
            throw new IllegalStateException("AFS directories do not match the datasource");
        }
    }

    private Set<String> emptyDirectories(Set<String> directories, Set<String> filePaths)
    {
        Set<String> emptyDirectories = new HashSet<>();
        for (String directory : directories)
        {
            if (directory.equals(TRASH_PATH) || isInSnapshots(directory) || hasDescendantFile(directory, filePaths))
            {
                // Internal and implied directories are not explicit state.
                continue;
            }
            emptyDirectories.add(directory);
        }
        return emptyDirectories;
    }

    private boolean hasDescendantFile(String directory, Set<String> filePaths)
    {
        String prefix = directory.endsWith("/") ? directory : directory + "/";
        for (String filePath : filePaths)
        {
            if (filePath.startsWith(prefix))
            {
                return true;
            }
        }
        return false;
    }

    private static Map<String, List<SnapshotVersion>> snapshotHistories(List<IncomingAfsFile> snapshots)
    {
        Map<String, List<SnapshotVersion>> histories = new HashMap<>();
        List<IncomingAfsFile> orderedSnapshots = snapshots.stream()
                .sorted(Comparator.comparing((IncomingAfsFile file) -> snapshotName(file.getPath()))
                        .thenComparing(IncomingAfsFile::getPath))
                .toList();
        for (IncomingAfsFile snapshot : orderedSnapshots)
        {
            String path = snapshot.getPath();
            SnapshotVersion version = new SnapshotVersion(isInTrash(path), snapshot.getHash());
            histories.computeIfAbsent(livePathForSnapshot(path), ignored -> new ArrayList<>()).add(version);
        }
        return histories;
    }

    private Set<String> snapshotPaths(String ownerPermId, String livePath) throws Exception
    {
        Set<String> paths = new HashSet<>();
        for (ch.ethz.sis.afsapi.dto.File entry : list(ownerPermId, snapshotsDirectoryFor(livePath), false))
        {
            if (Boolean.TRUE.equals(entry.getDirectory()) == false)
            {
                paths.add(entry.getPath());
            }
        }
        return paths;
    }

    private void deleteIfPresent(String ownerPermId, String path) throws Exception
    {
        if (AfsClientUploadHelper.getServerFilePresence(harvesterAfsClient, ownerPermId, path).isPresent())
        {
            harvesterAfsClient.delete(ownerPermId, path, false);
        }
    }

    private ch.ethz.sis.afsapi.dto.File[] list(String ownerPermId, String path, boolean recursive) throws Exception
    {
        int retries = 0;
        while (true)
        {
            try
            {
                return harvesterAfsClient.list(ownerPermId, path, recursive);
            } catch (Exception e)
            {
                if (AfsClientUploadHelper.isPathNotInStoreError(e))
                {
                    // Missing internal paths are equivalent to empty lists.
                    return new ch.ethz.sis.afsapi.dto.File[0];
                }
                if (isPathBusyError(e) == false || retries >= PATH_BUSY_RETRIES)
                {
                    throw e;
                }

                // A concurrent AFS operation may hold the owner briefly.
                retries++;
                operationLog.warn("AFS path is busy for owner " + ownerPermId + "; retrying list " + retries + "/"
                        + PATH_BUSY_RETRIES);
                Thread.sleep(PATH_BUSY_RETRY_DELAY_MILLIS);
            }
        }
    }

    private static boolean isPathBusyError(Exception e)
    {
        // The client exposes server error 10011 only in the message.
        String message = e.getMessage();
        return message != null && message.contains("exceptionCode") && message.contains("10011")
                && message.contains("currently being used");
    }

    private void ensureOwnerRootExists(String ownerPermId) throws Exception
    {
        if (AfsClientUploadHelper.getServerFilePresence(harvesterAfsClient, ownerPermId, "/").isEmpty())
        {
            harvesterAfsClient.create(ownerPermId, "/", true);
        }
    }

    private void recordResult(AfsOwner owner, boolean update)
    {
        int count = owner.afsFiles().size()
                + owner.trashedAfsFiles().size()
                + owner.afsDirectories().size()
                + owner.trashedAfsDirectories().size()
                + owner.afsFileSnapshots().size();
        if (update)
        {
            summary.updatedCount.addAndGet(count);
        } else
        {
            summary.addedCount.addAndGet(count);
        }
    }

    private static Map<String, IncomingAfsFile> filesByPath(List<IncomingAfsFile> files)
    {
        Map<String, IncomingAfsFile> filesByPath = new HashMap<>();
        for (IncomingAfsFile file : files)
        {
            filesByPath.put(file.getPath(), file);
        }
        return filesByPath;
    }

    private static String snapshotsDirectoryFor(String livePath)
    {
        int lastSlash = livePath.lastIndexOf('/');
        String parentPath = livePath.substring(0, lastSlash);
        String fileName = livePath.substring(lastSlash + 1);
        return parentPath + "/" + SNAPSHOTS_DIRECTORY + "/" + fileName;
    }

    static String livePathForSnapshot(String snapshotPath)
    {
        String marker = "/" + SNAPSHOTS_DIRECTORY + "/";
        int markerIndex = snapshotPath.indexOf(marker);
        if (markerIndex < 0)
        {
            throw new IllegalArgumentException("Invalid snapshot path: " + snapshotPath);
        }

        String parentPath = snapshotPath.substring(0, markerIndex);
        String remainingPath = snapshotPath.substring(markerIndex + marker.length());
        int nextSlash = remainingPath.indexOf('/');
        if (nextSlash < 0)
        {
            throw new IllegalArgumentException("Invalid snapshot path: " + snapshotPath);
        }

        String livePath = parentPath + "/" + remainingPath.substring(0, nextSlash);
        return isInTrash(livePath) ? livePathForTrashedPath(livePath) : livePath;
    }

    private static boolean isSnapshotsContainerDirectory(String path)
    {
        String marker = "/" + SNAPSHOTS_DIRECTORY + "/";
        int markerIndex = path.indexOf(marker);
        if (markerIndex < 0)
        {
            return false;
        }

        String remainingPath = path.substring(markerIndex + marker.length());
        return remainingPath.isEmpty() == false && remainingPath.contains("/") == false;
    }

    private static String livePathForSnapshotContainer(String containerPath)
    {
        String marker = "/" + SNAPSHOTS_DIRECTORY + "/";
        int markerIndex = containerPath.indexOf(marker);
        String parentPath = containerPath.substring(0, markerIndex);
        String fileName = containerPath.substring(markerIndex + marker.length());
        String livePath = parentPath + "/" + fileName;
        return isInTrash(livePath) ? livePathForTrashedPath(livePath) : livePath;
    }

    private static String livePathForTrashedPath(String trashedPath)
    {
        return trashedPath.substring(TRASH_PATH.length());
    }

    private static String snapshotName(String snapshotPath)
    {
        return snapshotPath.substring(snapshotPath.lastIndexOf('/') + 1);
    }

    static boolean isBackupPath(String path)
    {
        return path.startsWith(BACKUP_PATH_PREFIX);
    }

    private static boolean isInTrash(String path)
    {
        return path.equals(TRASH_PATH) || path.startsWith(TRASH_PATH + "/");
    }

    private static boolean isInSnapshots(String path)
    {
        return path.contains("/" + SNAPSHOTS_DIRECTORY + "/") || path.endsWith("/" + SNAPSHOTS_DIRECTORY);
    }

    private void transferDirectory(String ownerPermId, String directoryPath) throws Exception
    {
        // Upload an empty local tree to create the remote directory.
        File tempDir = new File(tempDirBase, "afs-" + UUID.randomUUID());
        tempDir.mkdirs();
        try
        {
            Path relativePath = Paths.get("/").relativize(Paths.get(directoryPath));
            if (relativePath.getNameCount() > 0)
            {
                Files.createDirectories(tempDir.toPath().resolve(relativePath));
            }
            harvesterAfsClient.upload(tempDir.toPath(), ownerPermId, Paths.get("/"), ClientAPI.overrideCollisionListener,
                    new ClientAPI.DefaultTransferMonitorLister());
        } finally
        {
            FileUtils.deleteQuietly(tempDir);
        }
    }

    private void transferFile(String ownerPermId, Path sourcePath, Path destinationPath) throws Exception
    {
        // A local staging tree lets the clients mirror different paths.
        File tempDir = new File(tempDirBase, "afs-" + UUID.randomUUID());
        tempDir.mkdirs();
        try
        {
            sourceAfsClient.download(ownerPermId, sourcePath, tempDir.toPath(), ClientAPI.overrideCollisionListener,
                    new ClientAPI.DefaultTransferMonitorLister());

            Path downloadedFile = tempDir.toPath().resolve(sourcePath.getFileName());
            Path mirroredFile = tempDir.toPath().resolve(Paths.get("/").relativize(destinationPath));
            if (mirroredFile.equals(downloadedFile) == false)
            {
                Files.createDirectories(mirroredFile.getParent());
                Files.move(downloadedFile, mirroredFile);
            }

            harvesterAfsClient.upload(tempDir.toPath(), ownerPermId, Paths.get("/"), ClientAPI.overrideCollisionListener,
                    new ClientAPI.DefaultTransferMonitorLister());
        } finally
        {
            FileUtils.deleteQuietly(tempDir);
        }
    }

    private enum Component
    {
        LIVE,
        TRASHED,
        SNAPSHOTS;

        private Object value(FileUnitState state)
        {
            return switch (this)
            {
                case LIVE -> state.liveHash;
                case TRASHED -> state.trashedHash;
                case SNAPSHOTS -> state.snapshots;
            };
        }
    }

    private record SnapshotVersion(boolean trashed, String hash)
    {
    }

    private record DestinationState(AfsOwner owner)
    {
    }

    private static final class FileUnitState
    {
        private static final FileUnitState EMPTY = new FileUnitState();

        private String liveHash;

        private String trashedHash;

        private final List<SnapshotVersion> snapshots = new ArrayList<>();

        @Override
        public boolean equals(Object object)
        {
            if (this == object)
            {
                return true;
            }
            if ((object instanceof FileUnitState) == false)
            {
                return false;
            }
            FileUnitState other = (FileUnitState) object;
            return Objects.equals(liveHash, other.liveHash) && Objects.equals(trashedHash, other.trashedHash)
                    && snapshots.equals(other.snapshots);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(liveHash, trashedHash, snapshots);
        }
    }

    public record AfsOwner(String permId, List<IncomingAfsFile> afsFiles,
                           List<String> afsDirectories, List<IncomingAfsFile> trashedAfsFiles,
                           List<String> trashedAfsDirectories,
                           List<IncomingAfsFile> afsFileSnapshots)
        {
        }
}
