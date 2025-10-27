package ch.openbis.drive.tasks;

import ch.ethz.sis.afsapi.api.ClientAPI;
import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afsclient.client.AfsClient;
import ch.ethz.sis.afsclient.client.AfsClientDownloadHelper;
import ch.ethz.sis.afsclient.client.AfsClientUploadHelper;
import ch.openbis.drive.conf.Configuration;
import ch.openbis.drive.db.SyncJobEventDAO;

import ch.openbis.drive.model.Event;
import ch.openbis.drive.model.Notification;
import ch.openbis.drive.model.SyncJob;
import ch.openbis.drive.model.SyncJobEvent;
import ch.openbis.drive.notifications.NotificationManager;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Stream;

import static ch.ethz.sis.afsclient.client.AfsClientUploadHelper.toServerPathString;

public class SyncOperation {
    static final int MAX_READ_SIZE_BYTES = 10485760;
    static final int AFS_CLIENT_TIMEOUT = 10000;
    static final String CONFLICT_FILE_SUFFIX = ".openbis-conflict";

    private final @NonNull SyncJob syncJob;

    final @NonNull AfsClientProxy afsClientProxy;
    final @NonNull ClientAPI.TransferMonitorListener uploadMonitor;
    final @NonNull ClientAPI.TransferMonitorListener downloadMonitor;

    final @NonNull SyncJobEventDAO syncJobEventDAO;
    final Path localOpenBisHiddenStateDirectory;

    final @NonNull NotificationManager notificationManager;


    public SyncOperation(@NonNull SyncJob syncJob,
                         @NonNull SyncJobEventDAO syncJobEventDAO,
                         @NonNull NotificationManager notificationManager,
                         @NonNull Configuration configuration) throws SQLException, IOException {
        AfsClient afsClient = new AfsClient(URI.create(syncJob.getOpenBisUrl()), MAX_READ_SIZE_BYTES, AFS_CLIENT_TIMEOUT);
        afsClient.setSessionToken(syncJob.getOpenBisPersonalAccessToken());

        ClientAPI.DefaultTransferMonitorLister uploadMonitor = new ClientAPI.DefaultTransferMonitorLister();
        ClientAPI.DefaultTransferMonitorLister downloadMonitor = new ClientAPI.DefaultTransferMonitorLister();
        uploadMonitor.addFileTransferredListener(new SyncTaskFileTransferredListener(SyncJobEvent.SyncDirection.UP));
        downloadMonitor.addFileTransferredListener(new SyncTaskFileTransferredListener(SyncJobEvent.SyncDirection.DOWN));

        this.syncJob = syncJob;
        this.afsClientProxy = new AfsClientProxy(afsClient);
        this.uploadMonitor = uploadMonitor;
        this.downloadMonitor = downloadMonitor;
        this.syncJobEventDAO = syncJobEventDAO;
        this.localOpenBisHiddenStateDirectory = configuration.getLocalAppStateDirectory();
        this.notificationManager = notificationManager;
    }

    SyncOperation(@NonNull SyncJob syncJob,
                  @NonNull AfsClientProxy afsClient,
                  @NonNull ClientAPI.TransferMonitorListener uploadMonitor,
                  @NonNull ClientAPI.TransferMonitorListener downloadMonitor,
                  @NonNull SyncJobEventDAO syncJobEventDAO,
                  @NonNull Path localOpenBisHiddenStateDirectory,
                  @NonNull NotificationManager notificationManager
    ) {
        this.syncJob = syncJob;
        this.afsClientProxy = afsClient;
        this.uploadMonitor = uploadMonitor;
        this.downloadMonitor = downloadMonitor;
        this.syncJobEventDAO = syncJobEventDAO;
        this.localOpenBisHiddenStateDirectory = localOpenBisHiddenStateDirectory;
        this.notificationManager = notificationManager;
    }

    public synchronized void upload() throws Exception {
        afsClientProxy.upload(Path.of(syncJob.getLocalDirectoryRoot()), syncJob.getEntityPermId(), Path.of(syncJob.getRemoteDirectoryRoot()),
                new FileSyncCollisionListener(SyncJobEvent.SyncDirection.UP), uploadMonitor);
    }

    public synchronized void download() throws Exception {
        afsClientProxy.download(syncJob.getEntityPermId(), Path.of(syncJob.getRemoteDirectoryRoot()), Path.of(syncJob.getLocalDirectoryRoot()),
                new FileSyncCollisionListener(SyncJobEvent.SyncDirection.DOWN), downloadMonitor);
    }

    public synchronized void synchronize() throws Exception {
        afsClientProxy.upload(Path.of(syncJob.getLocalDirectoryRoot()), syncJob.getEntityPermId(), Path.of(syncJob.getRemoteDirectoryRoot()),
                new FileSyncCollisionListener(SyncJobEvent.SyncDirection.UP), uploadMonitor);
        afsClientProxy.download(syncJob.getEntityPermId(), Path.of(syncJob.getRemoteDirectoryRoot()), Path.of(syncJob.getLocalDirectoryRoot()),
                new FileSyncCollisionListener(SyncJobEvent.SyncDirection.DOWN), downloadMonitor);
    }

    @SneakyThrows
    public void start() {
        try {
            switch (syncJob.getType()) {
                case Upload -> upload();
                case Download -> download();
                case Bidirectional -> synchronize();
            }
        } catch (Exception e) {
            raiseJobExceptionNotification(e);
            throw e;
        } finally {
            pruneOldDeletedSyncEvents();
            clearStaleConflictNotifications();
        }
    }

    public void stop() {
        uploadMonitor.stop();
        downloadMonitor.stop();
        raiseJobStoppedNotification();
    }

    public ClientAPI.TransferMonitorListener getTransferMonitor() {
        switch (syncJob.getType()) {
            case Upload:
                return uploadMonitor;
            case Download:
                return downloadMonitor;
            default: //Bidirectional
                if (!uploadMonitor.isFinished()) {
                    return uploadMonitor;
                } else {
                    return downloadMonitor;
                }
        }
    }

    public class FileSyncCollisionListener implements ClientAPI.FileCollisionListener {
        final @NonNull SyncJobEvent.SyncDirection syncDirection;

        public FileSyncCollisionListener(@NonNull SyncJobEvent.SyncDirection syncDirection) {
            this.syncDirection = syncDirection;
        }

        @Override
        @SneakyThrows
        public ClientAPI.CollisionAction precheck(@NonNull Path sourcePath, @NonNull Path destinationPath, boolean collision) {
            ClientAPI.CollisionAction collisionAction;
            Optional<FileInfo> sourceInfoOpt;
            SyncJobEvent syncJobEvent = null;

            if( skipAppPrivateFilesPrecheck(syncDirection, sourcePath, destinationPath) ) {
                sourceInfoOpt = Optional.empty();
                collisionAction = ClientAPI.CollisionAction.Skip;
            } else {
                sourceInfoOpt = getSourceInfo(syncDirection, sourcePath);

                if (sourceInfoOpt.isEmpty() || sourceInfoOpt.get().getLastModifiedDate() == null) {
                    //Insufficient information
                    collisionAction = ClientAPI.CollisionAction.Skip;

                } else {
                    FileInfo sourceInfo = sourceInfoOpt.get();
                    syncJobEvent = getSyncJobEvent(syncDirection, sourcePath, destinationPath);
                    Instant destinationLastModified = collision ? getDestinationInfo(syncDirection, destinationPath).map(FileInfo::getLastModifiedDate).orElse(null) : null;
                    SyncCheckResult syncCheckResult = checkSyncState(
                            syncJobEvent,
                            sourceInfo.getLastModifiedDate(), destinationLastModified,
                            syncDirection, collision);

                    collisionAction = handleSyncResult(sourcePath, destinationPath, syncCheckResult, sourceInfo, syncJobEvent);
                }
            }

            if(collisionAction == ClientAPI.CollisionAction.Override ) {
                if(!sourceInfoOpt.get().isDirectory()) {
                    insertNewSyncEntry(syncDirection, sourcePath, destinationPath, sourceInfoOpt.get().getLastModifiedDate());
                } else if (!collision || syncJobEvent == null || syncJobEvent.getDestinationTimestamp() == null) {
                    insertNewCompletedSyncEntry(syncDirection, sourcePath, destinationPath, sourceInfoOpt.get().getLastModifiedDate(), true);
                }
            }

            return collisionAction;
        }

        ClientAPI.CollisionAction handleSyncResult(Path sourcePath, Path destinationPath, SyncCheckResult syncCheckResult, FileInfo sourceInfo, SyncJobEvent syncJobEvent) throws Exception {

            SyncCheckAction syncCheckAction = getSyncActionForSyncResult(syncCheckResult, syncJob.getType());

            return switch (syncCheckAction) {
                case SKIP -> ClientAPI.CollisionAction.Skip;
                case PROCEED -> ClientAPI.CollisionAction.Override;
                case DELETE_SOURCE -> {
                    if (!sourceInfo.isDirectory() || isSourceDirEmpty(sourcePath)) {
                        deleteSourceFile(sourcePath);
                        insertNewSyncEntryForSourceDeleted(syncDirection, sourcePath, destinationPath, sourceInfo.isDirectory());
                    }
                    yield ClientAPI.CollisionAction.Skip;
                }
                case RAISE_VERSION_CONFLICT -> {
                    if (!sourceInfo.isDirectory()) {
                        yield handleFileVersionConflict(sourcePath, destinationPath, syncDirection, syncJobEvent);
                    } else {
                        yield ClientAPI.CollisionAction.Override;
                    }
                }
            };
        }

        void deleteSourceFile(Path sourcePath) throws Exception {
            if (syncDirection == SyncJobEvent.SyncDirection.UP) {
                Files.deleteIfExists(sourcePath);
            } else {
                deleteRemoteFile(sourcePath);
            }
        }

        boolean isSourceDirEmpty(Path sourcePath) throws Exception {
            return switch (syncDirection) {
                case UP -> {
                    try (Stream<Path> list = Files.list(sourcePath)) { yield list.findAny().isEmpty(); }
                }
                case DOWN ->
                        getRemoteFileList(sourcePath).length == 0;
            };
        }
    }

    public class SyncTaskFileTransferredListener implements ClientAPI.FileTransferredListener {
        private final @NonNull SyncJobEvent.SyncDirection syncDirection;

        public SyncTaskFileTransferredListener(@NonNull SyncJobEvent.SyncDirection syncDirection) {
            this.syncDirection = syncDirection;
        }

        @Override
        @SneakyThrows
        public void transferred(@NonNull Path source, @NonNull Path destination) {
            Path localFile = switch (syncDirection) {
                case UP -> source;
                case DOWN -> destination;
            };

            Path remoteFile = switch (syncDirection) {
                case UP -> destination;
                case DOWN -> source;
            };

            SyncJobEvent syncJobEvent = syncJobEventDAO.selectByPrimaryKey(syncDirection, localFile.toAbsolutePath().toString(), toServerPathString(remoteFile));

            if (syncJobEvent != null) {
                Instant completion;

                if (syncDirection == SyncJobEvent.SyncDirection.UP) {
                    completion = getRemoteFileInfo(destination).map(FileInfo::getLastModifiedDate)
                            .orElseThrow(() -> new IllegalStateException("Error retrieving remote destination last-modification date"));
                } else {
                    completion = getLocalFileInfo(destination).map(FileInfo::getLastModifiedDate)
                            .orElseThrow(() -> new IllegalStateException("Error retrieving local destination last-modification date"));
                }

                if (completion != null) {
                    SyncJobEvent updatedSyncJobEvent = syncJobEvent.toBuilder().destinationTimestamp(completion.toEpochMilli()).timestamp(Instant.now().toEpochMilli()).build();
                    syncJobEventDAO.insertOrUpdate(updatedSyncJobEvent);
                }
            }

            removePossibleConflictNotification(source, destination, syncDirection);
        }
    }

    public enum SyncCheckResult {
        SYNC_STATE_INCOMPLETE,
        SYNC_STATE_INCOMPLETE_SRC_MODIFIED_LATER,
        SYNC_STATE_INCOMPLETE_DEST_MODIFIED_LATER,
        SYNC_STATE_INCOMPLETE_NO_DEST,

        NONE_MODIFIED,

        SOURCE_MODIFIED,

        DESTINATION_MODIFIED,
        DESTINATION_DELETED,

        BOTH_MODIFIED,
        BOTH_MODIFIED_DEST_DELETED
    }

    public enum SyncCheckAction {
        PROCEED,
        SKIP,
        DELETE_SOURCE,
        RAISE_VERSION_CONFLICT
    }


    @NonNull static SyncCheckResult checkSyncState(SyncJobEvent syncJobEvent, Instant sourceLastModified, Instant destinationLastModified,
                                                   @NonNull SyncJobEvent.SyncDirection syncDirection, boolean collision) {
        if(syncJobEvent == null || syncJobEvent.getDestinationTimestamp() == null ||
                sourceLastModified == null ||
                (collision && destinationLastModified == null)) { // Missing synchronization information

            if (sourceLastModified != null && destinationLastModified != null) {
                if(sourceLastModified.isAfter(destinationLastModified)) {
                    return SyncCheckResult.SYNC_STATE_INCOMPLETE_SRC_MODIFIED_LATER;
                } else {
                    return SyncCheckResult.SYNC_STATE_INCOMPLETE_DEST_MODIFIED_LATER;
                }
            } else if (!collision) {
                return SyncCheckResult.SYNC_STATE_INCOMPLETE_NO_DEST;
            } else {
                return SyncCheckResult.SYNC_STATE_INCOMPLETE;
            }
        } else {
            boolean sourceModified = sourceLastModified.toEpochMilli() >
                    (syncJobEvent.getSyncDirection() == syncDirection ? syncJobEvent.getSourceTimestamp() : syncJobEvent.getDestinationTimestamp());
            boolean destinationModified = !collision || (destinationLastModified.toEpochMilli() >
                    (syncJobEvent.getSyncDirection() == syncDirection ? syncJobEvent.getDestinationTimestamp() : syncJobEvent.getSourceTimestamp()));

            if (sourceModified) {
                if (destinationModified) {
                    return collision ? SyncCheckResult.BOTH_MODIFIED : SyncCheckResult.BOTH_MODIFIED_DEST_DELETED;
                } else {
                    return SyncCheckResult.SOURCE_MODIFIED;
                }
            } else {
                if (destinationModified) {
                    return collision ? SyncCheckResult.DESTINATION_MODIFIED : SyncCheckResult.DESTINATION_DELETED;
                } else {
                    return SyncCheckResult.NONE_MODIFIED;
                }
            }
        }
    }

    static @NonNull SyncCheckAction getSyncActionForSyncResult(@NonNull SyncCheckResult syncCheckResult, @NonNull SyncJob.Type type) {
        return switch (type) {
            case Upload, Download ->
                switch (syncCheckResult) {

                    case SYNC_STATE_INCOMPLETE, SYNC_STATE_INCOMPLETE_SRC_MODIFIED_LATER,
                         SYNC_STATE_INCOMPLETE_NO_DEST, SOURCE_MODIFIED, DESTINATION_DELETED,
                         BOTH_MODIFIED_DEST_DELETED -> SyncCheckAction.PROCEED;

                    case BOTH_MODIFIED, DESTINATION_MODIFIED, SYNC_STATE_INCOMPLETE_DEST_MODIFIED_LATER ->
                            SyncCheckAction.RAISE_VERSION_CONFLICT;

                    case NONE_MODIFIED -> SyncCheckAction.SKIP;
                };

            case Bidirectional ->
                    switch (syncCheckResult) {

                    case SYNC_STATE_INCOMPLETE, SYNC_STATE_INCOMPLETE_SRC_MODIFIED_LATER, SOURCE_MODIFIED,
                         SYNC_STATE_INCOMPLETE_NO_DEST, BOTH_MODIFIED_DEST_DELETED ->
                            SyncCheckAction.PROCEED;
                    case SYNC_STATE_INCOMPLETE_DEST_MODIFIED_LATER, NONE_MODIFIED, DESTINATION_MODIFIED ->
                            SyncCheckAction.SKIP;
                    case DESTINATION_DELETED -> SyncCheckAction.DELETE_SOURCE;
                    case BOTH_MODIFIED -> SyncCheckAction.RAISE_VERSION_CONFLICT;
                };
        };
    }

    @Value
    static class FileInfo {
        boolean directory;
        Instant lastModifiedDate;
    }

    Optional<FileInfo> getSourceInfo(@NonNull SyncJobEvent.SyncDirection syncDirection, @NonNull Path sourcePath) throws Exception {
        return switch (syncDirection) {
            case UP -> getLocalFileInfo(sourcePath);
            case DOWN -> getRemoteFileInfo(sourcePath);
        };
    }

    Optional<FileInfo> getDestinationInfo(@NonNull SyncJobEvent.SyncDirection syncDirection, @NonNull Path destinationPath) throws Exception {
        return switch (syncDirection) {
            case UP -> getRemoteFileInfo(destinationPath);
            case DOWN -> getLocalFileInfo(destinationPath);
        };
    }

    Optional<FileInfo> getLocalFileInfo(@NonNull Path localPath) throws Exception {
        if (Files.exists(localPath)) {
            return Optional.of(new FileInfo(Files.isDirectory(localPath), Files.getLastModifiedTime(localPath).toInstant()));
        } else {
            return Optional.empty();
        }
    }

    Optional<FileInfo> getRemoteFileInfo(@NonNull Path remotePath) throws Exception {
        Optional<File> remoteFileCheck = getRemoteFilePresence(remotePath);
        if (remoteFileCheck.isPresent()) {
            File remoteFile = remoteFileCheck.get();
            if(!remoteFile.getDirectory()) {
                return Optional.of(new FileInfo(false, remoteFile.getLastModifiedTime().toInstant()));
            } else {
                Path parentPath = Path.of(remoteFile.getPath()).getParent();
                if (parentPath != null) {
                    Instant lastModified = Arrays.stream(getRemoteFileList(parentPath))
                            .filter(file -> file.getPath().equals(toServerPathString(remotePath)))
                            .findFirst().map(File::getLastModifiedTime).map(OffsetDateTime::toInstant).orElse(null);

                    return Optional.of(new FileInfo(true, lastModified));

                } else {
                    return Optional.of(new FileInfo(true, null));
                }
            }
        } else {
            return Optional.empty();
        }
    }

    Optional<File> getRemoteFilePresence(@NonNull Path remotePath) throws Exception {
        return AfsClientUploadHelper.getServerFilePresence(getAfsClient(), syncJob.getEntityPermId(), toServerPathString(remotePath));
    }

    File[] getRemoteFileList(@NonNull Path remotePath) throws Exception {
        return getAfsClient().list(syncJob.getEntityPermId(), toServerPathString(remotePath), false);
    }

    void deleteRemoteFile(@NonNull Path remotePath) throws Exception {
        try {
            getAfsClient().delete(syncJob.getEntityPermId(), toServerPathString(remotePath));
        } catch (Exception e) {
            if ( !AfsClientUploadHelper.isPathNotInStoreError(e) ) {
                throw e;
            }
        }
    }

    SyncJobEvent getSyncJobEvent(@NonNull SyncJobEvent.SyncDirection syncDirection, @NonNull Path source, @NonNull Path destination) throws SQLException, IOException {
        if ((syncDirection == SyncJobEvent.SyncDirection.UP && syncJob.getType() == SyncJob.Type.Download) || (syncDirection == SyncJobEvent.SyncDirection.DOWN && syncJob.getType() == SyncJob.Type.Upload)) {
            throw new IllegalStateException("Incompatible SyncDirection and JobType");
        }

        Path localFile = switch (syncDirection) {
            case UP -> source;
            case DOWN -> destination;
        };

        Path remoteFile = switch (syncDirection) {
            case UP -> destination;
            case DOWN -> source;
        };

        if (syncJob.getType() == SyncJob.Type.Bidirectional) {

            SyncJobEvent uploadSyncJobEvent = syncJobEventDAO.selectByPrimaryKey(
                    SyncJobEvent.SyncDirection.UP, localFile.toAbsolutePath().toString(), toServerPathString(remoteFile));

            SyncJobEvent downloadSyncJobEvent = syncJobEventDAO.selectByPrimaryKey(
                    SyncJobEvent.SyncDirection.DOWN, localFile.toAbsolutePath().toString(), toServerPathString(remoteFile));

            return pickMoreRecentCompletedFileSyncState(uploadSyncJobEvent, downloadSyncJobEvent);
        } else {
            return syncJobEventDAO.selectByPrimaryKey(
                    syncDirection, localFile.toAbsolutePath().toString(), toServerPathString(remoteFile));
        }
    }

    void insertNewSyncEntry(SyncJobEvent.SyncDirection syncDirection, Path source, Path destination, Instant sourceLastModified) throws SQLException, IOException {
        Path localFile = switch (syncDirection) {
            case UP -> source;
            case DOWN -> destination;
        };

        Path remoteFile = switch (syncDirection) {
            case UP -> destination;
            case DOWN -> source;
        };

        SyncJobEvent newFileSyncEntry = SyncJobEvent.builder()
                .syncDirection(syncDirection).localFile(localFile.toAbsolutePath().toString()).remoteFile(toServerPathString(remoteFile))
                .entityPermId(syncJob.getEntityPermId()).localDirectoryRoot(syncJob.getLocalDirectoryRoot())
                .sourceTimestamp(sourceLastModified.toEpochMilli()).destinationTimestamp(null)
                .timestamp(System.currentTimeMillis()).build();

        syncJobEventDAO.insertOrUpdate(newFileSyncEntry);
    }

    void insertNewCompletedSyncEntry(SyncJobEvent.SyncDirection syncDirection, Path source, Path destination, @NonNull Instant sourceLastModified, boolean directory) throws SQLException, IOException {
        Path localFile = switch (syncDirection) {
            case UP -> source;
            case DOWN -> destination;
        };

        Path remoteFile = switch (syncDirection) {
            case UP -> destination;
            case DOWN -> source;
        };

        Instant now = Instant.now();
        SyncJobEvent newFileSyncEntry = SyncJobEvent.builder()
                .syncDirection(syncDirection).localFile(localFile.toAbsolutePath().toString()).remoteFile(toServerPathString(remoteFile))
                .entityPermId(syncJob.getEntityPermId()).localDirectoryRoot(syncJob.getLocalDirectoryRoot())
                .sourceTimestamp(sourceLastModified.toEpochMilli()).destinationTimestamp(now.toEpochMilli()).timestamp(now.toEpochMilli())
                .directory(directory)
                .build();

        syncJobEventDAO.insertOrUpdate(newFileSyncEntry);
    }

    void insertNewSyncEntryForSourceDeleted(SyncJobEvent.SyncDirection syncDirection, Path source, Path destination, boolean directory) throws SQLException, IOException {
        Path localFile = switch (syncDirection) {
            case UP -> source;
            case DOWN -> destination;
        };

        Path remoteFile = switch (syncDirection) {
            case UP -> destination;
            case DOWN -> source;
        };

        Instant now = Instant.now();
        SyncJobEvent newFileSyncEntry = SyncJobEvent.builder()
                .syncDirection(syncDirection).localFile(localFile.toAbsolutePath().toString()).remoteFile(toServerPathString(remoteFile))
                .entityPermId(syncJob.getEntityPermId()).localDirectoryRoot(syncJob.getLocalDirectoryRoot())
                .sourceTimestamp(now.toEpochMilli()).destinationTimestamp(null).timestamp(now.toEpochMilli())
                .directory(directory).sourceDeleted(true)
                .build();

        syncJobEventDAO.insertOrUpdate(newFileSyncEntry);
    }

    boolean skipAppPrivateFilesPrecheck(@NonNull SyncJobEvent.SyncDirection syncDirection, @NonNull Path source, @NonNull Path destination) {
        Path localPath = switch (syncDirection) {
            case UP -> source;
            case DOWN -> destination;
        };

        return localPath.toAbsolutePath().normalize().startsWith(localOpenBisHiddenStateDirectory) ||
                source.getFileName().toString().endsWith(CONFLICT_FILE_SUFFIX) ||
                destination.getFileName().toString().endsWith(CONFLICT_FILE_SUFFIX);
    }

    static SyncJobEvent pickMoreRecentCompletedFileSyncState(SyncJobEvent entry1, SyncJobEvent entry2) {
        return Stream.of(entry1, entry2).filter(Objects::nonNull)
                .filter(entry -> entry.getDestinationTimestamp() != null || entry.isSourceDeleted())
                .max(Comparator.comparing(SyncJobEvent::getTimestamp)).orElse(null);
    }

    @SneakyThrows
    ClientAPI.CollisionAction handleFileVersionConflict(@NonNull Path source, @NonNull Path destination, @NonNull SyncJobEvent.SyncDirection syncDirection, @NonNull SyncJobEvent syncJobEvent) {
        Path localFile = syncDirection == SyncJobEvent.SyncDirection.UP ? source : destination;
        Path remoteFile = syncDirection == SyncJobEvent.SyncDirection.UP ? destination : source;
        Notification alreadyPresentConflictNotification = notificationManager.getSpecificNotification(new Notification(
                Notification.Type.Conflict,
                syncJob.getLocalDirectoryRoot(),
                localFile.toString(),
                toServerPathString(remoteFile),
                "FILE VERSION CONFLICT",
                Instant.now().toEpochMilli()
        ));

        Path localSuffixedConflictFile = Path.of(localFile + CONFLICT_FILE_SUFFIX).toAbsolutePath();

        //Check if conflict notification is present and .openbis-conflict file has been deleted, that means: conflict resolution has been performed
        if (alreadyPresentConflictNotification != null && !Files.exists(localSuffixedConflictFile)) {
            afsClientProxy.delete(syncJob.getEntityPermId(), toServerPathString(remoteFile));
            FileTime localLastModification = Files.getLastModifiedTime(localFile);
            ClientAPI.DefaultTransferMonitorLister transferMonitorListener = new ClientAPI.DefaultTransferMonitorLister();
            transferMonitorListener.addFileTransferredListener(new ClientAPI.FileTransferredListener() {
                @Override
                @SneakyThrows
                public void transferred(@NonNull Path sourcePath, @NonNull Path destinationPath) {
                    if(sourcePath.equals(localFile) && destinationPath.equals(remoteFile)) {
                        SyncJobEvent updatedSyncJobEvent = SyncJobEvent.builder()
                                .syncDirection(Event.SyncDirection.UP)
                                .localDirectoryRoot(syncJob.getLocalDirectoryRoot())
                                .entityPermId(syncJob.getEntityPermId())
                                .localFile(localFile.toAbsolutePath().toString())
                                .remoteFile(toServerPathString(remoteFile))
                                .sourceTimestamp(localLastModification.toMillis())
                                .destinationTimestamp(getRemoteFileInfo(remoteFile).map(FileInfo::getLastModifiedDate)
                                        .map(Instant::toEpochMilli)
                                        .orElseThrow(() -> new IllegalStateException("Error retrieving remote destination last-modification date")))
                                .timestamp(Instant.now().toEpochMilli())
                                .directory(false)
                                .sourceDeleted(false).build();
                        syncJobEventDAO.insertOrUpdate(updatedSyncJobEvent);
                    }
                }
            });
            boolean uploadResult = afsClientProxy.upload(localFile, syncJob.getEntityPermId(), remoteFile, ClientAPI.overrideCollisionListener, transferMonitorListener);

            if (uploadResult && transferMonitorListener.getException() == null) {
                removePossibleConflictNotification(source, destination, syncDirection);
            } else {
                throw new RuntimeException(String.format("Upload of conflict resolution failed for local file: %s", localFile));
            }
        } else {
            boolean doDownloadRemoteVersion = false;
            boolean localConflictFileExists = Files.exists(localSuffixedConflictFile);
            if (localConflictFileExists) {
                Optional<FileInfo> remoteFileInfo = getRemoteFileInfo(remoteFile);
                if(remoteFileInfo.isPresent()) {
                    BasicFileAttributes attr = Files.readAttributes(localSuffixedConflictFile, BasicFileAttributes.class);
                    FileTime fileTime = attr.creationTime();
                    if (remoteFileInfo.get().getLastModifiedDate().toEpochMilli() >
                            fileTime.toMillis()) {
                        doDownloadRemoteVersion = true;
                    }
                }
            } else {
                doDownloadRemoteVersion = true;
            }

            if(doDownloadRemoteVersion) {
                ClientAPI.DefaultTransferMonitorLister transferMonitorListener = new ClientAPI.DefaultTransferMonitorLister();
                if(!localConflictFileExists) {
                    Files.createFile(localSuffixedConflictFile);
                }
                boolean downloadResult = afsClientProxy.download(syncJob.getEntityPermId(), remoteFile, localSuffixedConflictFile, ClientAPI.overrideCollisionListener, transferMonitorListener);
                if(!downloadResult || transferMonitorListener.getException() != null) {
                    throw new RuntimeException(String.format("Download of conflicting remote content failed: %s", remoteFile));
                }
            }

            raiseConflictNotification(source, destination, syncDirection, syncJobEvent);
        }

        return ClientAPI.CollisionAction.Skip;
    }

    void raiseConflictNotification(@NonNull Path source, @NonNull Path destination, @NonNull SyncJobEvent.SyncDirection syncDirection, @NonNull SyncJobEvent syncJobEvent) {
        String localFile = syncDirection == SyncJobEvent.SyncDirection.UP ? source.toString() : destination.toString();
        String remoteFile = syncDirection == SyncJobEvent.SyncDirection.UP ? toServerPathString(destination) : toServerPathString(source);

        Notification notification = new Notification(
                Notification.Type.Conflict,
                syncJob.getLocalDirectoryRoot(),
                localFile,
                remoteFile,
                String.format("FILE VERSION CONFLICT: check remote version in %s.openbis-conflict, make due changes in %s if necessary, then delete %s.openbis-conflict to mark resolution", localFile, localFile, localFile),
                Instant.now().toEpochMilli()
        );

        notificationManager.addNotifications(Collections.singletonList(notification));
    }

    void removePossibleConflictNotification(@NonNull Path source, @NonNull Path destination, @NonNull SyncJobEvent.SyncDirection syncDirection) {
        String localFile = syncDirection == SyncJobEvent.SyncDirection.UP ? source.toString() : destination.toString();
        String remoteFile = syncDirection == SyncJobEvent.SyncDirection.UP ? toServerPathString(destination) : toServerPathString(source);

        Notification notification = new Notification(
                Notification.Type.Conflict,
                syncJob.getLocalDirectoryRoot(),
                localFile,
                remoteFile,
                "FILE VERSION CONFLICT",
                Instant.now().toEpochMilli()
        );

        notificationManager.removeNotifications(Collections.singletonList(notification));
    }

    void clearStaleConflictNotifications() {
        List<Notification> conflictNotifications = notificationManager.getConflictNotifications(syncJob, 100);

        for (Notification notification: conflictNotifications) {
            boolean removeNotification = false;
            Path localFile = null;
            try {
                localFile = Path.of(notification.getLocalFile());
            } catch (Exception e) {
                removeNotification = true;
            }

            if(localFile != null && !Files.exists(localFile)) {
                removeNotification = true;
            }

            if(removeNotification) {
                notificationManager.removeNotifications(Collections.singletonList(notification));
            }
        }
    }

    void raiseJobStoppedNotification() {
       Notification notification = new Notification(
                Notification.Type.JobStopped,
                syncJob.getLocalDirectoryRoot(),
                null,
                null,
                "JOB STOPPED",
                Instant.now().toEpochMilli()
        );

        notificationManager.addNotifications(Collections.singletonList(notification));
    }

    void raiseJobExceptionNotification(Exception e) {
        Notification notification = new Notification(
                Notification.Type.JobException,
                syncJob.getLocalDirectoryRoot(),
                null,
                null,
                e.getClass().getCanonicalName() + " exception with message: " + e.getMessage(),
                Instant.now().toEpochMilli()
        );

        notificationManager.addNotifications(Collections.singletonList(notification));
    }

    synchronized void pruneOldDeletedSyncEvents() throws Exception {
        syncJobEventDAO.pruneOldDeletedByLocalDirectoryRoot(syncJob.getLocalDirectoryRoot(), 1000);
    }


    AfsClient getAfsClient() {
        return afsClientProxy.afsClient;
    }

    static class AfsClientProxy {
        final AfsClient afsClient;

        public AfsClientProxy(@NonNull AfsClient afsClient) {
            this.afsClient = afsClient;
        }

        @NonNull public Boolean upload(@NonNull Path sourcePath, @NonNull String destinationOwner, @NonNull Path destinationPath, @NonNull ClientAPI.FileCollisionListener fileCollisionListener, @NonNull ClientAPI.TransferMonitorListener transferMonitorListener) throws Exception {
            return AfsClientUploadHelper.upload(afsClient, sourcePath, destinationOwner, destinationPath, fileCollisionListener, transferMonitorListener, false);
        }

        @NonNull public Boolean download(@NonNull String sourceOwner, @NonNull Path sourcePath, @NonNull Path destinationPath, @NonNull ClientAPI.FileCollisionListener fileCollisionListener, @NonNull ClientAPI.TransferMonitorListener transferMonitorListener) throws Exception{
            return AfsClientDownloadHelper.download(afsClient, sourceOwner, sourcePath, destinationPath, fileCollisionListener, transferMonitorListener);
        }

        public void delete(@NonNull String sourceOwner, @NonNull String sourcePath) throws Exception {
            afsClient.delete(sourceOwner, sourcePath);
        }
    }
}

