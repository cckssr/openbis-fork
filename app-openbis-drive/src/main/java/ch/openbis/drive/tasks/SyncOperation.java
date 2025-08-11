package ch.openbis.drive.tasks;

import ch.ethz.sis.afsapi.api.ClientAPI;
import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afsclient.client.AfsClient;
import ch.ethz.sis.afsclient.client.AfsClientDownloadHelper;
import ch.ethz.sis.afsclient.client.AfsClientUploadHelper;
import ch.openbis.drive.db.SyncJobEventDAO;

import ch.openbis.drive.model.Notification;
import ch.openbis.drive.model.SyncJob;
import ch.openbis.drive.model.SyncJobEvent;
import ch.openbis.drive.notifications.NotificationManager;
import ch.openbis.drive.util.OpenBISDriveUtil;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Stream;

public class SyncOperation {
    static final int MAX_READ_SIZE_BYTES = 10485760;
    static final int AFS_CLIENT_TIMEOUT = 10000;

    private final @NonNull SyncJob syncJob;

    final @NonNull AfsClientProxy afsClientProxy;
    final @NonNull ClientAPI.TransferMonitorListener uploadMonitor;
    final @NonNull ClientAPI.TransferMonitorListener downloadMonitor;

    final @NonNull SyncJobEventDAO syncJobEventDAO;
    final Path localOpenBisHiddenDirectory;

    final @NonNull NotificationManager notificationManager;


    public SyncOperation(@NonNull SyncJob syncJob, @NonNull SyncJobEventDAO syncJobEventDAO, @NonNull NotificationManager notificationManager) throws SQLException, IOException {
        AfsClient afsClient = new AfsClient(URI.create(syncJob.getOpenBisUrl()), MAX_READ_SIZE_BYTES, AFS_CLIENT_TIMEOUT);
        afsClient.setSessionToken(syncJob.getOpenBisPersonalAccessToken());

        ClientAPI.DefaultTransferMonitorLister uploadMonitor = new ClientAPI.DefaultTransferMonitorLister();
        ClientAPI.DefaultTransferMonitorLister downloadMonitor = new ClientAPI.DefaultTransferMonitorLister();
        uploadMonitor.addFileTransferredListener(new SyncTaskFileTransferredListener(SyncJobEvent.SyncDirection.UP));
        downloadMonitor.addFileTransferredListener(new SyncTaskFileTransferredListener(SyncJobEvent.SyncDirection.DOWN));

        Path localOpenBisHiddenDirectory = OpenBISDriveUtil.getLocalHiddenDirectoryPath(syncJob.getLocalDirectoryRoot());

        this.syncJob = syncJob;
        this.afsClientProxy = new AfsClientProxy(afsClient);
        this.uploadMonitor = uploadMonitor;
        this.downloadMonitor = downloadMonitor;
        this.syncJobEventDAO = syncJobEventDAO;
        this.localOpenBisHiddenDirectory = localOpenBisHiddenDirectory;
        this.notificationManager = notificationManager;
    }

    SyncOperation(@NonNull SyncJob syncJob,
                  @NonNull AfsClientProxy afsClient,
                  @NonNull ClientAPI.TransferMonitorListener uploadMonitor,
                  @NonNull ClientAPI.TransferMonitorListener downloadMonitor,
                  @NonNull SyncJobEventDAO syncJobEventDAO,
                  @NonNull Path localOpenBisHiddenDirectory,
                  @NonNull NotificationManager notificationManager
    ) {
        this.syncJob = syncJob;
        this.afsClientProxy = afsClient;
        this.uploadMonitor = uploadMonitor;
        this.downloadMonitor = downloadMonitor;
        this.syncJobEventDAO = syncJobEventDAO;
        this.localOpenBisHiddenDirectory = localOpenBisHiddenDirectory;
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

            if( skipLocalHiddenAppDirPrecheck(syncDirection, sourcePath, destinationPath) ) {
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

            SyncJobEvent syncJobEvent = syncJobEventDAO.selectByPrimaryKey(syncDirection, localFile.toAbsolutePath().toString(), remoteFile.toAbsolutePath().toString());

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
                            .filter(file -> file.getPath().equals(remotePath.toAbsolutePath().toString()))
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
        return AfsClientUploadHelper.getServerFilePresence(getAfsClient(), syncJob.getEntityPermId(), remotePath.toAbsolutePath().toString());
    }

    File[] getRemoteFileList(@NonNull Path remotePath) throws Exception {
        return getAfsClient().list(syncJob.getEntityPermId(), remotePath.toAbsolutePath().toString(), false);
    }

    void deleteRemoteFile(@NonNull Path remotePath) throws Exception {
        try {
            getAfsClient().delete(syncJob.getEntityPermId(), remotePath.toAbsolutePath().toString());
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
                    SyncJobEvent.SyncDirection.UP, localFile.toAbsolutePath().toString(), remoteFile.toAbsolutePath().toString());

            SyncJobEvent downloadSyncJobEvent = syncJobEventDAO.selectByPrimaryKey(
                    SyncJobEvent.SyncDirection.DOWN, localFile.toAbsolutePath().toString(), remoteFile.toAbsolutePath().toString());

            return pickMoreRecentCompletedFileSyncState(uploadSyncJobEvent, downloadSyncJobEvent);
        } else {
            return syncJobEventDAO.selectByPrimaryKey(
                    syncDirection, localFile.toAbsolutePath().toString(), remoteFile.toAbsolutePath().toString());
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
                .syncDirection(syncDirection).localFile(localFile.toAbsolutePath().toString()).remoteFile(remoteFile.toAbsolutePath().toString())
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
                .syncDirection(syncDirection).localFile(localFile.toAbsolutePath().toString()).remoteFile(remoteFile.toAbsolutePath().toString())
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
                .syncDirection(syncDirection).localFile(localFile.toAbsolutePath().toString()).remoteFile(remoteFile.toAbsolutePath().toString())
                .entityPermId(syncJob.getEntityPermId()).localDirectoryRoot(syncJob.getLocalDirectoryRoot())
                .sourceTimestamp(now.toEpochMilli()).destinationTimestamp(null).timestamp(now.toEpochMilli())
                .directory(directory).sourceDeleted(true)
                .build();

        syncJobEventDAO.insertOrUpdate(newFileSyncEntry);
    }

    boolean skipLocalHiddenAppDirPrecheck(@NonNull SyncJobEvent.SyncDirection syncDirection, @NonNull Path source, @NonNull Path destination) {
        Path localPath = switch (syncDirection) {
            case UP -> source;
            case DOWN -> destination;
        };

        return localPath.toAbsolutePath().startsWith(localOpenBisHiddenDirectory);
    }

    static SyncJobEvent pickMoreRecentCompletedFileSyncState(SyncJobEvent entry1, SyncJobEvent entry2) {
        return Stream.of(entry1, entry2).filter(Objects::nonNull)
                .filter(entry -> entry.getDestinationTimestamp() != null || entry.isSourceDeleted())
                .max(Comparator.comparing(SyncJobEvent::getTimestamp)).orElse(null);
    }

    ClientAPI.CollisionAction handleFileVersionConflict(@NonNull Path source, @NonNull Path destination, @NonNull SyncJobEvent.SyncDirection syncDirection, @NonNull SyncJobEvent syncJobEvent) {
        raiseConflictNotification(source, destination, syncDirection, syncJobEvent);
        return ClientAPI.CollisionAction.Skip;
    }

    void raiseConflictNotification(@NonNull Path source, @NonNull Path destination, @NonNull SyncJobEvent.SyncDirection syncDirection, @NonNull SyncJobEvent syncJobEvent) {
        String localFile = syncDirection == SyncJobEvent.SyncDirection.UP ? source.toString() : destination.toString();
        String remoteFile = syncDirection == SyncJobEvent.SyncDirection.UP ? destination.toString() : source.toString();

        Notification notification = new Notification(
                Notification.Type.Conflict,
                syncJob.getLocalDirectoryRoot(),
                localFile,
                remoteFile,
                "FILE VERSION CONFLICT",
                Instant.now().toEpochMilli()
        );

        notificationManager.addNotifications(Collections.singletonList(notification));
    }

    void removePossibleConflictNotification(@NonNull Path source, @NonNull Path destination, @NonNull SyncJobEvent.SyncDirection syncDirection) {
        String localFile = syncDirection == SyncJobEvent.SyncDirection.UP ? source.toString() : destination.toString();
        String remoteFile = syncDirection == SyncJobEvent.SyncDirection.UP ? destination.toString() : source.toString();

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
            return AfsClientUploadHelper.upload(afsClient, sourcePath, destinationOwner, destinationPath, fileCollisionListener, transferMonitorListener);
        }

        @NonNull public Boolean download(@NonNull String sourceOwner, @NonNull Path sourcePath, @NonNull Path destinationPath, @NonNull ClientAPI.FileCollisionListener fileCollisionListener, @NonNull ClientAPI.TransferMonitorListener transferMonitorListener) throws Exception{
            return AfsClientDownloadHelper.download(afsClient, sourceOwner, sourcePath, destinationPath, fileCollisionListener, transferMonitorListener);
        }
    }
}

