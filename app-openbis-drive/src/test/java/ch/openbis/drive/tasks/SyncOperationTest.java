package ch.openbis.drive.tasks;

import ch.ethz.sis.afsapi.api.ClientAPI;
import ch.ethz.sis.afsapi.dto.File;
import ch.openbis.drive.conf.Configuration;
import ch.openbis.drive.db.SyncJobEventDAO;
import ch.openbis.drive.db.SyncJobEventDAOImp;
import ch.openbis.drive.model.Event;
import ch.openbis.drive.model.Notification;
import ch.openbis.drive.model.SyncJob;
import ch.openbis.drive.model.SyncJobEvent;
import ch.openbis.drive.notifications.NotificationManager;
import junit.framework.TestCase;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;


import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static ch.ethz.sis.afsclient.client.AfsClientUploadHelper.toServerPathString;

@RunWith(JUnit4.class)
public class SyncOperationTest extends TestCase {

    private final Configuration configuration = new Configuration(Path.of("/fake-local-app-directory"));

    @Test
    public void testPublicContructor() throws Exception {
        String localDirPath = Path.of(this.getClass().getClassLoader().getResource("placeholder.txt").getPath()).getParent().toString();
        SyncJob syncJob = new SyncJob(SyncJob.Type.Bidirectional, "url", "uuid", "token", "/remotedir1", localDirPath, true);
        NotificationManager notificationManager = Mockito.mock(NotificationManager.class);
        SyncJobEventDAO syncJobEventDAO = Mockito.mock(SyncJobEventDAOImp.class);
        SyncOperation syncOperation = new SyncOperation(syncJob, syncJobEventDAO, notificationManager, configuration);

        Assert.assertEquals(syncJobEventDAO, syncOperation.syncJobEventDAO);
        Assert.assertEquals(syncOperation.afsClientProxy.afsClient, syncOperation.getAfsClient());
        Assert.assertEquals(URI.create(syncJob.getOpenBisUrl()), syncOperation.getAfsClient().getServerUri());
        Assert.assertEquals(SyncOperation.MAX_READ_SIZE_BYTES, syncOperation.getAfsClient().getMaxReadSizeInBytes());
        Assert.assertEquals("uuid", syncOperation.getAfsClient().getSessionToken());
        Assert.assertEquals(configuration.getLocalAppStateDirectory(), syncOperation.localOpenBisHiddenStateDirectory);
        Assert.assertEquals(ClientAPI.DefaultTransferMonitorLister.class, syncOperation.uploadMonitor.getClass());
        Assert.assertEquals(ClientAPI.DefaultTransferMonitorLister.class, syncOperation.downloadMonitor.getClass());
        Assert.assertEquals(notificationManager, syncOperation.notificationManager);
    }

    @Test
    public void testStart() throws Exception {
        SyncJobEventDAO syncJobEventDAO = Mockito.mock(SyncJobEventDAO.class);
        SyncOperation.AfsClientProxy afsClient = Mockito.mock(SyncOperation.AfsClientProxy.class);
        ClientAPI.TransferMonitorListener uploadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        ClientAPI.TransferMonitorListener downloadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        NotificationManager notificationManager = Mockito.mock(NotificationManager.class);

        SyncJob syncJob1 = new SyncJob(SyncJob.Type.Upload, "url", "uuid", "token", "/remotedir1", "/localdir1", true);
        SyncOperation syncOperation1 = Mockito.spy(new SyncOperation(syncJob1, afsClient, uploadMonitor, downloadMonitor, syncJobEventDAO, Path.of("/hidden-dir"), notificationManager));
        syncOperation1.start();

        Mockito.verify(syncOperation1, Mockito.times(1)).upload();
        Mockito.verify(syncOperation1, Mockito.times(1)).pruneOldDeletedSyncEvents();

        SyncJob syncJob2 = new SyncJob(SyncJob.Type.Download, "url", "uuid", "token", "/remotedir1", "/localdir1", true);

        SyncOperation syncOperation2 = Mockito.spy(new SyncOperation(syncJob2, afsClient, uploadMonitor, downloadMonitor, syncJobEventDAO, Path.of("/hidden-dir"), notificationManager));
        syncOperation2.start();

        Mockito.verify(syncOperation2, Mockito.times(1)).download();
        Mockito.verify(syncOperation2, Mockito.times(1)).pruneOldDeletedSyncEvents();

        SyncJob syncJob3 = new SyncJob(SyncJob.Type.Bidirectional, "url", "uuid", "token", "/remotedir1", "/localdir1", true);

        SyncOperation syncOperation3 = Mockito.spy(new SyncOperation(syncJob3, afsClient, uploadMonitor, downloadMonitor, syncJobEventDAO, Path.of("/hidden-dir"), notificationManager));
        syncOperation3.start();

        Mockito.verify(syncOperation3, Mockito.times(1)).synchronize();
        Mockito.verify(syncOperation3, Mockito.times(1)).pruneOldDeletedSyncEvents();
        Mockito.verify(syncOperation3, Mockito.times(1)).clearStaleConflictNotifications();
    }

    @Test
    public void testStop() {
        SyncJobEventDAO syncJobEventDAO = Mockito.mock(SyncJobEventDAO.class);
        SyncOperation.AfsClientProxy afsClient = Mockito.mock(SyncOperation.AfsClientProxy.class);
        ClientAPI.TransferMonitorListener uploadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        ClientAPI.TransferMonitorListener downloadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        NotificationManager notificationManager = Mockito.mock(NotificationManager.class);

        SyncJob syncJob1 = new SyncJob(SyncJob.Type.Upload, "url", "uuid", "token", "/remotedir1", "/localdir1", true);
        SyncOperation syncOperation1 = Mockito.spy(new SyncOperation(syncJob1, afsClient, uploadMonitor, downloadMonitor, syncJobEventDAO, Path.of("/hidden-dir"), notificationManager));
        syncOperation1.stop();

        Mockito.verify(syncOperation1.uploadMonitor, Mockito.times(1)).stop();
        Mockito.verify(syncOperation1.downloadMonitor, Mockito.times(1)).stop();
        Mockito.verify(syncOperation1, Mockito.times(1)).raiseJobStoppedNotification();


        uploadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        downloadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        SyncJob syncJob2 = new SyncJob(SyncJob.Type.Download, "url", "uuid", "token", "/remotedir1", "/localdir1", true);
        notificationManager = Mockito.mock(NotificationManager.class);

        SyncOperation syncOperation2 = Mockito.spy(new SyncOperation(syncJob2, afsClient, uploadMonitor, downloadMonitor, syncJobEventDAO, Path.of("/hidden-dir"), notificationManager));
        syncOperation2.stop();

        Mockito.verify(syncOperation2.uploadMonitor, Mockito.times(1)).stop();
        Mockito.verify(syncOperation2.downloadMonitor, Mockito.times(1)).stop();
        Mockito.verify(syncOperation1, Mockito.times(1)).raiseJobStoppedNotification();

        uploadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        downloadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        SyncJob syncJob3 = new SyncJob(SyncJob.Type.Bidirectional, "url", "uuid", "token", "/remotedir1", "/localdir1", true);
        notificationManager = Mockito.mock(NotificationManager.class);

        SyncOperation syncOperation3 = Mockito.spy(new SyncOperation(syncJob3, afsClient, uploadMonitor, downloadMonitor, syncJobEventDAO, Path.of("/hidden-dir"), notificationManager));
        syncOperation3.stop();

        Mockito.verify(syncOperation3.uploadMonitor, Mockito.times(1)).stop();
        Mockito.verify(syncOperation3.downloadMonitor, Mockito.times(1)).stop();
        Mockito.verify(syncOperation1, Mockito.times(1)).raiseJobStoppedNotification();
    }

    @Test
    public void testGetTransferMonitor() {
        SyncJobEventDAO syncJobEventDAO = Mockito.mock(SyncJobEventDAO.class);
        SyncOperation.AfsClientProxy afsClient = Mockito.mock(SyncOperation.AfsClientProxy.class);
        ClientAPI.TransferMonitorListener uploadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        ClientAPI.TransferMonitorListener downloadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        NotificationManager notificationManager = Mockito.mock(NotificationManager.class);

        SyncJob syncJob1 = new SyncJob(SyncJob.Type.Upload, "url", "uuid", "token", "/remotedir1", "/localdir1", true);
        SyncOperation syncOperation1 = Mockito.spy(new SyncOperation(syncJob1, afsClient, uploadMonitor, downloadMonitor, syncJobEventDAO, Path.of("/hidden-dir"), notificationManager));
        Assert.assertEquals(uploadMonitor, syncOperation1.getTransferMonitor());


        uploadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        downloadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        SyncJob syncJob2 = new SyncJob(SyncJob.Type.Download, "url", "uuid", "token", "/remotedir1", "/localdir1", true);
        notificationManager = Mockito.mock(NotificationManager.class);

        SyncOperation syncOperation2 = Mockito.spy(new SyncOperation(syncJob2, afsClient, uploadMonitor, downloadMonitor, syncJobEventDAO, Path.of("/hidden-dir"), notificationManager));
        Assert.assertEquals(downloadMonitor, syncOperation2.getTransferMonitor());

        uploadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        downloadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        SyncJob syncJob3 = new SyncJob(SyncJob.Type.Bidirectional, "url", "uuid", "token", "/remotedir1", "/localdir1", true);
        notificationManager = Mockito.mock(NotificationManager.class);

        SyncOperation syncOperation3 = Mockito.spy(new SyncOperation(syncJob3, afsClient, uploadMonitor, downloadMonitor, syncJobEventDAO, Path.of("/hidden-dir"), notificationManager));
        Assert.assertEquals(uploadMonitor, syncOperation3.getTransferMonitor());
        Mockito.when(uploadMonitor.isFinished()).thenReturn(true);
        Assert.assertEquals(downloadMonitor, syncOperation3.getTransferMonitor());
    }

    @Test
    public void testCheckSyncStateOnCollision() {

        Instant now = Instant.now();
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_DEST_MODIFIED_LATER, SyncOperation.checkSyncState(null, now, now, SyncJobEvent.SyncDirection.UP, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_DEST_MODIFIED_LATER, SyncOperation.checkSyncState(null, now, now.plusMillis(1000), SyncJobEvent.SyncDirection.UP, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_SRC_MODIFIED_LATER, SyncOperation.checkSyncState(null, now.plusMillis(1000),now, SyncJobEvent.SyncDirection.UP, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE, SyncOperation.checkSyncState(null, null, Instant.now(), SyncJobEvent.SyncDirection.UP, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE, SyncOperation.checkSyncState(null, Instant.now(), null, SyncJobEvent.SyncDirection.UP, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE, SyncOperation.checkSyncState(null, null, null, SyncJobEvent.SyncDirection.UP, true));

        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_DEST_MODIFIED_LATER, SyncOperation.checkSyncState(null, now, now, SyncJobEvent.SyncDirection.DOWN, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_DEST_MODIFIED_LATER, SyncOperation.checkSyncState(null, now, now.plusMillis(1000), SyncJobEvent.SyncDirection.DOWN, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_SRC_MODIFIED_LATER, SyncOperation.checkSyncState(null, now.plusMillis(1000),now, SyncJobEvent.SyncDirection.DOWN, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE, SyncOperation.checkSyncState(null, null, Instant.now(), SyncJobEvent.SyncDirection.DOWN, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE, SyncOperation.checkSyncState(null, Instant.now(), null, SyncJobEvent.SyncDirection.DOWN, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE, SyncOperation.checkSyncState(null, null, null, SyncJobEvent.SyncDirection.DOWN, true));

        SyncJobEvent incompleteSyncJobEvent = SyncJobEvent.builder().syncDirection(SyncJobEvent.SyncDirection.UP).localFile("/loc").remoteFile("/rem").entityPermId("uuid").localDirectoryRoot("/locRoot")
                .destinationTimestamp(null).sourceTimestamp(now.toEpochMilli()).timestamp(System.currentTimeMillis()).timestamp(System.currentTimeMillis()).build();

        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_DEST_MODIFIED_LATER, SyncOperation.checkSyncState(incompleteSyncJobEvent, now, now, SyncJobEvent.SyncDirection.UP, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_DEST_MODIFIED_LATER, SyncOperation.checkSyncState(incompleteSyncJobEvent, now, now.plusMillis(1000), SyncJobEvent.SyncDirection.UP, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_SRC_MODIFIED_LATER, SyncOperation.checkSyncState(incompleteSyncJobEvent, now, now.minusMillis(1000), SyncJobEvent.SyncDirection.UP, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE, SyncOperation.checkSyncState(incompleteSyncJobEvent, now, null, SyncJobEvent.SyncDirection.UP, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE, SyncOperation.checkSyncState(incompleteSyncJobEvent, null, now, SyncJobEvent.SyncDirection.UP, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE, SyncOperation.checkSyncState(incompleteSyncJobEvent, null, null, SyncJobEvent.SyncDirection.UP, true));

        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_DEST_MODIFIED_LATER, SyncOperation.checkSyncState(incompleteSyncJobEvent, now, now, SyncJobEvent.SyncDirection.DOWN, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_DEST_MODIFIED_LATER, SyncOperation.checkSyncState(incompleteSyncJobEvent, now, now.plusMillis(1000), SyncJobEvent.SyncDirection.DOWN, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_SRC_MODIFIED_LATER, SyncOperation.checkSyncState(incompleteSyncJobEvent, now, now.minusMillis(1000), SyncJobEvent.SyncDirection.DOWN, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE, SyncOperation.checkSyncState(incompleteSyncJobEvent, now, null, SyncJobEvent.SyncDirection.DOWN, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE, SyncOperation.checkSyncState(incompleteSyncJobEvent, null, now, SyncJobEvent.SyncDirection.DOWN, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE, SyncOperation.checkSyncState(incompleteSyncJobEvent, null, null, SyncJobEvent.SyncDirection.DOWN, true));

        SyncJobEvent completedUpSyncJobEvent = SyncJobEvent.builder().syncDirection(SyncJobEvent.SyncDirection.UP).localFile("/loc").remoteFile("/rem").entityPermId("uuid").localDirectoryRoot("/locRoot")
                .destinationTimestamp(now.plusMillis(500).toEpochMilli()).sourceTimestamp(now.toEpochMilli()).timestamp(System.currentTimeMillis()).build();

        Assert.assertEquals(SyncOperation.SyncCheckResult.NONE_MODIFIED, SyncOperation.checkSyncState(completedUpSyncJobEvent, now, now, SyncJobEvent.SyncDirection.UP, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.NONE_MODIFIED, SyncOperation.checkSyncState(completedUpSyncJobEvent, now.minusMillis(10), now.plusMillis(450), SyncJobEvent.SyncDirection.UP, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.DESTINATION_MODIFIED, SyncOperation.checkSyncState(completedUpSyncJobEvent, now, now.plusMillis(1000), SyncJobEvent.SyncDirection.UP, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.DESTINATION_MODIFIED, SyncOperation.checkSyncState(completedUpSyncJobEvent, now.minusMillis(10), now.plusMillis(1000), SyncJobEvent.SyncDirection.UP, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SOURCE_MODIFIED, SyncOperation.checkSyncState(completedUpSyncJobEvent, now.plusMillis(500), now.plusMillis(500), SyncJobEvent.SyncDirection.UP, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SOURCE_MODIFIED, SyncOperation.checkSyncState(completedUpSyncJobEvent, now.plusMillis(500), now.plusMillis(450), SyncJobEvent.SyncDirection.UP, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE, SyncOperation.checkSyncState(completedUpSyncJobEvent, now, null, SyncJobEvent.SyncDirection.UP, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE, SyncOperation.checkSyncState(completedUpSyncJobEvent, null, now, SyncJobEvent.SyncDirection.UP, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE, SyncOperation.checkSyncState(completedUpSyncJobEvent, null, null, SyncJobEvent.SyncDirection.UP, true));

        Assert.assertEquals(SyncOperation.SyncCheckResult.NONE_MODIFIED, SyncOperation.checkSyncState(completedUpSyncJobEvent, now, now, SyncJobEvent.SyncDirection.DOWN, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.NONE_MODIFIED, SyncOperation.checkSyncState(completedUpSyncJobEvent, now.plusMillis(450), now.minusMillis(10), SyncJobEvent.SyncDirection.DOWN, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SOURCE_MODIFIED, SyncOperation.checkSyncState(completedUpSyncJobEvent, now.plusMillis(1000), now, SyncJobEvent.SyncDirection.DOWN, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SOURCE_MODIFIED, SyncOperation.checkSyncState(completedUpSyncJobEvent, now.plusMillis(1000), now.minusMillis(10), SyncJobEvent.SyncDirection.DOWN, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.DESTINATION_MODIFIED, SyncOperation.checkSyncState(completedUpSyncJobEvent, now.plusMillis(500), now.plusMillis(500), SyncJobEvent.SyncDirection.DOWN, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.DESTINATION_MODIFIED, SyncOperation.checkSyncState(completedUpSyncJobEvent, now.plusMillis(450), now.plusMillis(500), SyncJobEvent.SyncDirection.DOWN, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE, SyncOperation.checkSyncState(completedUpSyncJobEvent, now, null, SyncJobEvent.SyncDirection.DOWN, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE, SyncOperation.checkSyncState(completedUpSyncJobEvent, null, now, SyncJobEvent.SyncDirection.DOWN, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE, SyncOperation.checkSyncState(completedUpSyncJobEvent, null, null, SyncJobEvent.SyncDirection.DOWN, true));

        SyncJobEvent completedDownSyncJobEvent = SyncJobEvent.builder().syncDirection(SyncJobEvent.SyncDirection.DOWN).localFile("/loc").remoteFile("/rem").entityPermId("uuid").localDirectoryRoot("/locRoot")
                .destinationTimestamp(now.plusMillis(500).toEpochMilli()).sourceTimestamp(now.toEpochMilli()).timestamp(System.currentTimeMillis()).build();

        Assert.assertEquals(SyncOperation.SyncCheckResult.NONE_MODIFIED, SyncOperation.checkSyncState(completedDownSyncJobEvent, now, now, SyncJobEvent.SyncDirection.DOWN, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.NONE_MODIFIED, SyncOperation.checkSyncState(completedDownSyncJobEvent, now.minusMillis(10), now.plusMillis(450), SyncJobEvent.SyncDirection.DOWN, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.DESTINATION_MODIFIED, SyncOperation.checkSyncState(completedDownSyncJobEvent, now, now.plusMillis(1000), SyncJobEvent.SyncDirection.DOWN, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.DESTINATION_MODIFIED, SyncOperation.checkSyncState(completedDownSyncJobEvent, now.minusMillis(10), now.plusMillis(1000), SyncJobEvent.SyncDirection.DOWN, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SOURCE_MODIFIED, SyncOperation.checkSyncState(completedDownSyncJobEvent, now.plusMillis(500), now.plusMillis(500), SyncJobEvent.SyncDirection.DOWN, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SOURCE_MODIFIED, SyncOperation.checkSyncState(completedDownSyncJobEvent, now.plusMillis(500), now.plusMillis(450), SyncJobEvent.SyncDirection.DOWN, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE, SyncOperation.checkSyncState(completedDownSyncJobEvent, now, null, SyncJobEvent.SyncDirection.DOWN, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE, SyncOperation.checkSyncState(completedDownSyncJobEvent, null, now, SyncJobEvent.SyncDirection.DOWN, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE, SyncOperation.checkSyncState(completedDownSyncJobEvent, null, null, SyncJobEvent.SyncDirection.DOWN, true));

        Assert.assertEquals(SyncOperation.SyncCheckResult.NONE_MODIFIED, SyncOperation.checkSyncState(completedDownSyncJobEvent, now, now, SyncJobEvent.SyncDirection.UP, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.NONE_MODIFIED, SyncOperation.checkSyncState(completedDownSyncJobEvent, now.plusMillis(450), now.minusMillis(10), SyncJobEvent.SyncDirection.UP, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SOURCE_MODIFIED, SyncOperation.checkSyncState(completedDownSyncJobEvent, now.plusMillis(1000), now, SyncJobEvent.SyncDirection.UP, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SOURCE_MODIFIED, SyncOperation.checkSyncState(completedDownSyncJobEvent, now.plusMillis(1000), now.minusMillis(10), SyncJobEvent.SyncDirection.UP, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.DESTINATION_MODIFIED, SyncOperation.checkSyncState(completedDownSyncJobEvent, now.plusMillis(500), now.plusMillis(500), SyncJobEvent.SyncDirection.UP, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.DESTINATION_MODIFIED, SyncOperation.checkSyncState(completedDownSyncJobEvent, now.plusMillis(450), now.plusMillis(500), SyncJobEvent.SyncDirection.UP, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE, SyncOperation.checkSyncState(completedDownSyncJobEvent, now, null, SyncJobEvent.SyncDirection.UP, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE, SyncOperation.checkSyncState(completedDownSyncJobEvent, null, now, SyncJobEvent.SyncDirection.UP, true));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE, SyncOperation.checkSyncState(completedDownSyncJobEvent, null, null, SyncJobEvent.SyncDirection.UP, true));
    }
    
    @Test
    public void testCheckSyncStateOnNoCollision() {

        Instant now = Instant.now();
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_DEST_MODIFIED_LATER, SyncOperation.checkSyncState(null, now, now, SyncJobEvent.SyncDirection.UP, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_DEST_MODIFIED_LATER, SyncOperation.checkSyncState(null, now, now.plusMillis(1000), SyncJobEvent.SyncDirection.UP, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_SRC_MODIFIED_LATER, SyncOperation.checkSyncState(null, now.plusMillis(1000),now, SyncJobEvent.SyncDirection.UP, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_NO_DEST, SyncOperation.checkSyncState(null, null, Instant.now(), SyncJobEvent.SyncDirection.UP, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_NO_DEST, SyncOperation.checkSyncState(null, Instant.now(), null, SyncJobEvent.SyncDirection.UP, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_NO_DEST, SyncOperation.checkSyncState(null, null, null, SyncJobEvent.SyncDirection.UP, false));

        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_DEST_MODIFIED_LATER, SyncOperation.checkSyncState(null, now, now, SyncJobEvent.SyncDirection.DOWN, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_DEST_MODIFIED_LATER, SyncOperation.checkSyncState(null, now, now.plusMillis(1000), SyncJobEvent.SyncDirection.DOWN, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_SRC_MODIFIED_LATER, SyncOperation.checkSyncState(null, now.plusMillis(1000),now, SyncJobEvent.SyncDirection.DOWN, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_NO_DEST, SyncOperation.checkSyncState(null, null, Instant.now(), SyncJobEvent.SyncDirection.DOWN, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_NO_DEST, SyncOperation.checkSyncState(null, Instant.now(), null, SyncJobEvent.SyncDirection.DOWN, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_NO_DEST, SyncOperation.checkSyncState(null, null, null, SyncJobEvent.SyncDirection.DOWN, false));

        SyncJobEvent incompleteSyncJobEvent = SyncJobEvent.builder().syncDirection(SyncJobEvent.SyncDirection.UP).localFile("/loc").remoteFile("/rem").entityPermId("uuid").localDirectoryRoot("/locRoot")
                .destinationTimestamp(null).sourceTimestamp(now.toEpochMilli()).timestamp(System.currentTimeMillis()).build();

        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_DEST_MODIFIED_LATER, SyncOperation.checkSyncState(incompleteSyncJobEvent, now, now, SyncJobEvent.SyncDirection.UP, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_DEST_MODIFIED_LATER, SyncOperation.checkSyncState(incompleteSyncJobEvent, now, now.plusMillis(1000), SyncJobEvent.SyncDirection.UP, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_SRC_MODIFIED_LATER, SyncOperation.checkSyncState(incompleteSyncJobEvent, now, now.minusMillis(1000), SyncJobEvent.SyncDirection.UP, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_NO_DEST, SyncOperation.checkSyncState(incompleteSyncJobEvent, now, null, SyncJobEvent.SyncDirection.UP, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_NO_DEST, SyncOperation.checkSyncState(incompleteSyncJobEvent, null, now, SyncJobEvent.SyncDirection.UP, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_NO_DEST, SyncOperation.checkSyncState(incompleteSyncJobEvent, null, null, SyncJobEvent.SyncDirection.UP, false));

        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_DEST_MODIFIED_LATER, SyncOperation.checkSyncState(incompleteSyncJobEvent, now, now, SyncJobEvent.SyncDirection.DOWN, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_DEST_MODIFIED_LATER, SyncOperation.checkSyncState(incompleteSyncJobEvent, now, now.plusMillis(1000), SyncJobEvent.SyncDirection.DOWN, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_SRC_MODIFIED_LATER, SyncOperation.checkSyncState(incompleteSyncJobEvent, now, now.minusMillis(1000), SyncJobEvent.SyncDirection.DOWN, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_NO_DEST, SyncOperation.checkSyncState(incompleteSyncJobEvent, now, null, SyncJobEvent.SyncDirection.DOWN, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_NO_DEST, SyncOperation.checkSyncState(incompleteSyncJobEvent, null, now, SyncJobEvent.SyncDirection.DOWN, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_NO_DEST, SyncOperation.checkSyncState(incompleteSyncJobEvent, null, null, SyncJobEvent.SyncDirection.DOWN, false));

        SyncJobEvent completedUpSyncJobEvent = SyncJobEvent.builder().syncDirection(SyncJobEvent.SyncDirection.UP).localFile("/loc").remoteFile("/rem").entityPermId("uuid").localDirectoryRoot("/locRoot")
                .destinationTimestamp(now.plusMillis(500).toEpochMilli()).sourceTimestamp(now.toEpochMilli()).timestamp(System.currentTimeMillis()).build();

        Assert.assertEquals(SyncOperation.SyncCheckResult.DESTINATION_DELETED, SyncOperation.checkSyncState(completedUpSyncJobEvent, now, now, SyncJobEvent.SyncDirection.UP, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.DESTINATION_DELETED, SyncOperation.checkSyncState(completedUpSyncJobEvent, now.minusMillis(10), now.plusMillis(450), SyncJobEvent.SyncDirection.UP, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.DESTINATION_DELETED, SyncOperation.checkSyncState(completedUpSyncJobEvent, now, now.plusMillis(1000), SyncJobEvent.SyncDirection.UP, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.DESTINATION_DELETED, SyncOperation.checkSyncState(completedUpSyncJobEvent, now.minusMillis(10), now.plusMillis(1000), SyncJobEvent.SyncDirection.UP, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.BOTH_MODIFIED_DEST_DELETED, SyncOperation.checkSyncState(completedUpSyncJobEvent, now.plusMillis(500), now.plusMillis(500), SyncJobEvent.SyncDirection.UP, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.BOTH_MODIFIED_DEST_DELETED, SyncOperation.checkSyncState(completedUpSyncJobEvent, now.plusMillis(500), now.plusMillis(450), SyncJobEvent.SyncDirection.UP, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.DESTINATION_DELETED, SyncOperation.checkSyncState(completedUpSyncJobEvent, now, null, SyncJobEvent.SyncDirection.UP, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_NO_DEST, SyncOperation.checkSyncState(completedUpSyncJobEvent, null, now, SyncJobEvent.SyncDirection.UP, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_NO_DEST, SyncOperation.checkSyncState(completedUpSyncJobEvent, null, null, SyncJobEvent.SyncDirection.UP, false));

        Assert.assertEquals(SyncOperation.SyncCheckResult.DESTINATION_DELETED, SyncOperation.checkSyncState(completedUpSyncJobEvent, now, now, SyncJobEvent.SyncDirection.DOWN, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.DESTINATION_DELETED, SyncOperation.checkSyncState(completedUpSyncJobEvent, now.plusMillis(450), now.minusMillis(10), SyncJobEvent.SyncDirection.DOWN, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.BOTH_MODIFIED_DEST_DELETED, SyncOperation.checkSyncState(completedUpSyncJobEvent, now.plusMillis(1000), now, SyncJobEvent.SyncDirection.DOWN, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.BOTH_MODIFIED_DEST_DELETED, SyncOperation.checkSyncState(completedUpSyncJobEvent, now.plusMillis(1000), now.minusMillis(10), SyncJobEvent.SyncDirection.DOWN, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.DESTINATION_DELETED, SyncOperation.checkSyncState(completedUpSyncJobEvent, now.plusMillis(500), now.plusMillis(500), SyncJobEvent.SyncDirection.DOWN, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.DESTINATION_DELETED, SyncOperation.checkSyncState(completedUpSyncJobEvent, now.plusMillis(450), now.plusMillis(500), SyncJobEvent.SyncDirection.DOWN, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.DESTINATION_DELETED, SyncOperation.checkSyncState(completedUpSyncJobEvent, now, null, SyncJobEvent.SyncDirection.DOWN, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_NO_DEST, SyncOperation.checkSyncState(completedUpSyncJobEvent, null, now, SyncJobEvent.SyncDirection.DOWN, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_NO_DEST, SyncOperation.checkSyncState(completedUpSyncJobEvent, null, null, SyncJobEvent.SyncDirection.DOWN, false));

        SyncJobEvent completedDownSyncJobEvent = SyncJobEvent.builder().syncDirection(SyncJobEvent.SyncDirection.DOWN).localFile("/loc").remoteFile("/rem").entityPermId("uuid").localDirectoryRoot("/locRoot")
                .destinationTimestamp(now.plusMillis(500).toEpochMilli()).sourceTimestamp(now.toEpochMilli()).timestamp(System.currentTimeMillis()).build();

        Assert.assertEquals(SyncOperation.SyncCheckResult.DESTINATION_DELETED, SyncOperation.checkSyncState(completedDownSyncJobEvent, now, now, SyncJobEvent.SyncDirection.DOWN, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.DESTINATION_DELETED, SyncOperation.checkSyncState(completedDownSyncJobEvent, now.minusMillis(10), now.plusMillis(450), SyncJobEvent.SyncDirection.DOWN, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.DESTINATION_DELETED, SyncOperation.checkSyncState(completedDownSyncJobEvent, now, now.plusMillis(1000), SyncJobEvent.SyncDirection.DOWN, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.DESTINATION_DELETED, SyncOperation.checkSyncState(completedDownSyncJobEvent, now.minusMillis(10), now.plusMillis(1000), SyncJobEvent.SyncDirection.DOWN, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.BOTH_MODIFIED_DEST_DELETED, SyncOperation.checkSyncState(completedDownSyncJobEvent, now.plusMillis(500), now.plusMillis(500), SyncJobEvent.SyncDirection.DOWN, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.BOTH_MODIFIED_DEST_DELETED, SyncOperation.checkSyncState(completedDownSyncJobEvent, now.plusMillis(500), now.plusMillis(450), SyncJobEvent.SyncDirection.DOWN, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.DESTINATION_DELETED, SyncOperation.checkSyncState(completedDownSyncJobEvent, now, null, SyncJobEvent.SyncDirection.DOWN, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_NO_DEST, SyncOperation.checkSyncState(completedDownSyncJobEvent, null, now, SyncJobEvent.SyncDirection.DOWN, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_NO_DEST, SyncOperation.checkSyncState(completedDownSyncJobEvent, null, null, SyncJobEvent.SyncDirection.DOWN, false));

        Assert.assertEquals(SyncOperation.SyncCheckResult.DESTINATION_DELETED, SyncOperation.checkSyncState(completedDownSyncJobEvent, now, now, SyncJobEvent.SyncDirection.UP, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.DESTINATION_DELETED, SyncOperation.checkSyncState(completedDownSyncJobEvent, now.plusMillis(450), now.minusMillis(10), SyncJobEvent.SyncDirection.UP, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.BOTH_MODIFIED_DEST_DELETED, SyncOperation.checkSyncState(completedDownSyncJobEvent, now.plusMillis(1000), now, SyncJobEvent.SyncDirection.UP, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.BOTH_MODIFIED_DEST_DELETED, SyncOperation.checkSyncState(completedDownSyncJobEvent, now.plusMillis(1000), now.minusMillis(10), SyncJobEvent.SyncDirection.UP, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.DESTINATION_DELETED, SyncOperation.checkSyncState(completedDownSyncJobEvent, now.plusMillis(500), now.plusMillis(500), SyncJobEvent.SyncDirection.UP, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.DESTINATION_DELETED, SyncOperation.checkSyncState(completedDownSyncJobEvent, now.plusMillis(450), now.plusMillis(500), SyncJobEvent.SyncDirection.UP, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.DESTINATION_DELETED, SyncOperation.checkSyncState(completedDownSyncJobEvent, now, null, SyncJobEvent.SyncDirection.UP, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_NO_DEST, SyncOperation.checkSyncState(completedDownSyncJobEvent, null, now, SyncJobEvent.SyncDirection.UP, false));
        Assert.assertEquals(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_NO_DEST, SyncOperation.checkSyncState(completedDownSyncJobEvent, null, null, SyncJobEvent.SyncDirection.UP, false));
    }
    
    @Test
    public void testGetSyncActionForSyncResult() {
        
        Assert.assertEquals(SyncOperation.SyncCheckAction.PROCEED, SyncOperation.getSyncActionForSyncResult(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE, SyncJob.Type.Upload));
        Assert.assertEquals(SyncOperation.SyncCheckAction.PROCEED, SyncOperation.getSyncActionForSyncResult(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_NO_DEST, SyncJob.Type.Upload));
        Assert.assertEquals(SyncOperation.SyncCheckAction.RAISE_VERSION_CONFLICT, SyncOperation.getSyncActionForSyncResult(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_DEST_MODIFIED_LATER, SyncJob.Type.Upload));
        Assert.assertEquals(SyncOperation.SyncCheckAction.PROCEED, SyncOperation.getSyncActionForSyncResult(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_SRC_MODIFIED_LATER, SyncJob.Type.Upload));
        Assert.assertEquals(SyncOperation.SyncCheckAction.PROCEED, SyncOperation.getSyncActionForSyncResult(SyncOperation.SyncCheckResult.DESTINATION_DELETED, SyncJob.Type.Upload));
        Assert.assertEquals(SyncOperation.SyncCheckAction.RAISE_VERSION_CONFLICT, SyncOperation.getSyncActionForSyncResult(SyncOperation.SyncCheckResult.DESTINATION_MODIFIED, SyncJob.Type.Upload));
        Assert.assertEquals(SyncOperation.SyncCheckAction.PROCEED, SyncOperation.getSyncActionForSyncResult(SyncOperation.SyncCheckResult.SOURCE_MODIFIED, SyncJob.Type.Upload));
        Assert.assertEquals(SyncOperation.SyncCheckAction.RAISE_VERSION_CONFLICT, SyncOperation.getSyncActionForSyncResult(SyncOperation.SyncCheckResult.BOTH_MODIFIED, SyncJob.Type.Upload));
        Assert.assertEquals(SyncOperation.SyncCheckAction.PROCEED, SyncOperation.getSyncActionForSyncResult(SyncOperation.SyncCheckResult.BOTH_MODIFIED_DEST_DELETED, SyncJob.Type.Upload));
        Assert.assertEquals(SyncOperation.SyncCheckAction.SKIP, SyncOperation.getSyncActionForSyncResult(SyncOperation.SyncCheckResult.NONE_MODIFIED, SyncJob.Type.Upload));

        Assert.assertEquals(SyncOperation.SyncCheckAction.PROCEED, SyncOperation.getSyncActionForSyncResult(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE, SyncJob.Type.Download));
        Assert.assertEquals(SyncOperation.SyncCheckAction.PROCEED, SyncOperation.getSyncActionForSyncResult(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_NO_DEST, SyncJob.Type.Download));
        Assert.assertEquals(SyncOperation.SyncCheckAction.RAISE_VERSION_CONFLICT, SyncOperation.getSyncActionForSyncResult(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_DEST_MODIFIED_LATER, SyncJob.Type.Download));
        Assert.assertEquals(SyncOperation.SyncCheckAction.PROCEED, SyncOperation.getSyncActionForSyncResult(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_SRC_MODIFIED_LATER, SyncJob.Type.Download));
        Assert.assertEquals(SyncOperation.SyncCheckAction.PROCEED, SyncOperation.getSyncActionForSyncResult(SyncOperation.SyncCheckResult.DESTINATION_DELETED, SyncJob.Type.Download));
        Assert.assertEquals(SyncOperation.SyncCheckAction.RAISE_VERSION_CONFLICT, SyncOperation.getSyncActionForSyncResult(SyncOperation.SyncCheckResult.DESTINATION_MODIFIED, SyncJob.Type.Download));
        Assert.assertEquals(SyncOperation.SyncCheckAction.PROCEED, SyncOperation.getSyncActionForSyncResult(SyncOperation.SyncCheckResult.SOURCE_MODIFIED, SyncJob.Type.Download));
        Assert.assertEquals(SyncOperation.SyncCheckAction.RAISE_VERSION_CONFLICT, SyncOperation.getSyncActionForSyncResult(SyncOperation.SyncCheckResult.BOTH_MODIFIED, SyncJob.Type.Download));
        Assert.assertEquals(SyncOperation.SyncCheckAction.PROCEED, SyncOperation.getSyncActionForSyncResult(SyncOperation.SyncCheckResult.BOTH_MODIFIED_DEST_DELETED, SyncJob.Type.Download));
        Assert.assertEquals(SyncOperation.SyncCheckAction.SKIP, SyncOperation.getSyncActionForSyncResult(SyncOperation.SyncCheckResult.NONE_MODIFIED, SyncJob.Type.Download));

        Assert.assertEquals(SyncOperation.SyncCheckAction.PROCEED, SyncOperation.getSyncActionForSyncResult(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE, SyncJob.Type.Bidirectional));
        Assert.assertEquals(SyncOperation.SyncCheckAction.PROCEED, SyncOperation.getSyncActionForSyncResult(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_NO_DEST, SyncJob.Type.Bidirectional));
        Assert.assertEquals(SyncOperation.SyncCheckAction.SKIP, SyncOperation.getSyncActionForSyncResult(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_DEST_MODIFIED_LATER, SyncJob.Type.Bidirectional));
        Assert.assertEquals(SyncOperation.SyncCheckAction.PROCEED, SyncOperation.getSyncActionForSyncResult(SyncOperation.SyncCheckResult.SYNC_STATE_INCOMPLETE_SRC_MODIFIED_LATER, SyncJob.Type.Bidirectional));
        Assert.assertEquals(SyncOperation.SyncCheckAction.DELETE_SOURCE, SyncOperation.getSyncActionForSyncResult(SyncOperation.SyncCheckResult.DESTINATION_DELETED, SyncJob.Type.Bidirectional));
        Assert.assertEquals(SyncOperation.SyncCheckAction.SKIP, SyncOperation.getSyncActionForSyncResult(SyncOperation.SyncCheckResult.DESTINATION_MODIFIED, SyncJob.Type.Bidirectional));
        Assert.assertEquals(SyncOperation.SyncCheckAction.PROCEED, SyncOperation.getSyncActionForSyncResult(SyncOperation.SyncCheckResult.SOURCE_MODIFIED, SyncJob.Type.Bidirectional));
        Assert.assertEquals(SyncOperation.SyncCheckAction.RAISE_VERSION_CONFLICT, SyncOperation.getSyncActionForSyncResult(SyncOperation.SyncCheckResult.BOTH_MODIFIED, SyncJob.Type.Bidirectional));
        Assert.assertEquals(SyncOperation.SyncCheckAction.PROCEED, SyncOperation.getSyncActionForSyncResult(SyncOperation.SyncCheckResult.BOTH_MODIFIED_DEST_DELETED, SyncJob.Type.Bidirectional));
        Assert.assertEquals(SyncOperation.SyncCheckAction.SKIP, SyncOperation.getSyncActionForSyncResult(SyncOperation.SyncCheckResult.NONE_MODIFIED, SyncJob.Type.Bidirectional));
    }

    @Test
    public void testGetSourceInfo() throws Exception {
        SyncJobEventDAO syncJobEventDAO = Mockito.mock(SyncJobEventDAO.class);
        SyncOperation.AfsClientProxy afsClient = Mockito.mock(SyncOperation.AfsClientProxy.class);
        ClientAPI.TransferMonitorListener uploadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        ClientAPI.TransferMonitorListener downloadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        NotificationManager notificationManager = Mockito.mock(NotificationManager.class);

        SyncJob syncJob1 = new SyncJob(SyncJob.Type.Upload, "url", "uuid", "token", "/remotedir1", "/localdir1", true);
        SyncOperation syncOperation = Mockito.spy(new SyncOperation(syncJob1, afsClient, uploadMonitor, downloadMonitor, syncJobEventDAO, Path.of("/hidden-dir"), notificationManager));

        Path sourcePath = Path.of("source");
        Optional<SyncOperation.FileInfo> info1 = Optional.of(new SyncOperation.FileInfo(false, Instant.now()));
        Optional<SyncOperation.FileInfo> info2 = Optional.of(new SyncOperation.FileInfo(true, Instant.now()));
        Mockito.doReturn(info1).when(syncOperation).getLocalFileInfo(sourcePath);
        Mockito.doReturn(info2).when(syncOperation).getRemoteFileInfo(sourcePath);
        Assert.assertEquals(info1, syncOperation.getSourceInfo(SyncJobEvent.SyncDirection.UP, sourcePath));
        Assert.assertEquals(info2, syncOperation.getSourceInfo(SyncJobEvent.SyncDirection.DOWN, sourcePath));
        Mockito.verify(syncOperation, Mockito.times(1)).getLocalFileInfo(sourcePath);
        Mockito.verify(syncOperation, Mockito.times(1)).getRemoteFileInfo(sourcePath);
    }

    @Test
    public void testGetDestinationInfo() throws Exception {
        SyncJobEventDAO syncJobEventDAO = Mockito.mock(SyncJobEventDAO.class);
        SyncOperation.AfsClientProxy afsClient = Mockito.mock(SyncOperation.AfsClientProxy.class);
        ClientAPI.TransferMonitorListener uploadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        ClientAPI.TransferMonitorListener downloadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        NotificationManager notificationManager = Mockito.mock(NotificationManager.class);

        SyncJob syncJob1 = new SyncJob(SyncJob.Type.Upload, "url", "uuid", "token", "/remotedir1", "/localdir1", true);
        SyncOperation syncOperation = Mockito.spy(new SyncOperation(syncJob1, afsClient, uploadMonitor, downloadMonitor, syncJobEventDAO, Path.of("/hidden-dir"), notificationManager));

        Path sourcePath = Path.of("source");
        Optional<SyncOperation.FileInfo> info1 = Optional.of(new SyncOperation.FileInfo(false, Instant.now()));
        Optional<SyncOperation.FileInfo> info2 = Optional.of(new SyncOperation.FileInfo(true, Instant.now()));
        Mockito.doReturn(info1).when(syncOperation).getLocalFileInfo(sourcePath);
        Mockito.doReturn(info2).when(syncOperation).getRemoteFileInfo(sourcePath);
        Assert.assertEquals(info2, syncOperation.getDestinationInfo(SyncJobEvent.SyncDirection.UP, sourcePath));
        Assert.assertEquals(info1, syncOperation.getDestinationInfo(SyncJobEvent.SyncDirection.DOWN, sourcePath));
        Mockito.verify(syncOperation, Mockito.times(1)).getLocalFileInfo(sourcePath);
        Mockito.verify(syncOperation, Mockito.times(1)).getRemoteFileInfo(sourcePath);
    }

    @Test
    public void testGetLocalFileInfo() throws Exception {
        SyncJobEventDAO syncJobEventDAO = Mockito.mock(SyncJobEventDAO.class);
        SyncOperation.AfsClientProxy afsClient = Mockito.mock(SyncOperation.AfsClientProxy.class);
        ClientAPI.TransferMonitorListener uploadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        ClientAPI.TransferMonitorListener downloadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        NotificationManager notificationManager = Mockito.mock(NotificationManager.class);

        SyncJob syncJob1 = new SyncJob(SyncJob.Type.Upload, "url", "uuid", "token", "/remotedir1", "/localdir1", true);
        SyncOperation syncOperation = Mockito.spy(new SyncOperation(syncJob1, afsClient, uploadMonitor, downloadMonitor, syncJobEventDAO, Path.of("/hidden-dir"), notificationManager));

        String localDirPath = Path.of(this.getClass().getClassLoader().getResource("placeholder.txt").getPath()).getParent().toString();
        Path localFile = Path.of(localDirPath, UUID.randomUUID().toString());

        Files.createFile(localFile);
        Instant localLastModified = Files.getLastModifiedTime(localFile).toInstant();

        Assert.assertEquals(localLastModified, syncOperation.getLocalFileInfo(localFile).get().getLastModifiedDate());
        Assert.assertEquals(false,  syncOperation.getLocalFileInfo(localFile).get().isDirectory());
        Files.delete(localFile);
        Assert.assertEquals(Optional.empty(),  syncOperation.getLocalFileInfo(localFile));

        Path localSubDir = Path.of(localDirPath, UUID.randomUUID().toString());

        Files.createDirectory(localSubDir);
        Instant localSubdirLastModified = Files.getLastModifiedTime(localSubDir).toInstant();

        Assert.assertEquals(localSubdirLastModified,  syncOperation.getLocalFileInfo(localSubDir).get().getLastModifiedDate());
        Assert.assertEquals(true,  syncOperation.getLocalFileInfo(localSubDir).get().isDirectory());
        Files.delete(localSubDir);

    }

    @Test
    public void testGetRemoteFileInfo() throws Exception {
        SyncJobEventDAO syncJobEventDAO = Mockito.mock(SyncJobEventDAO.class);
        SyncOperation.AfsClientProxy afsClient = Mockito.mock(SyncOperation.AfsClientProxy.class);
        ClientAPI.TransferMonitorListener uploadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        ClientAPI.TransferMonitorListener downloadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        NotificationManager notificationManager = Mockito.mock(NotificationManager.class);

        SyncJob syncJob1 = new SyncJob(SyncJob.Type.Upload, "url", "uuid", "token", "/remotedir1", "/localdir1", true);
        SyncOperation syncOperation = Mockito.spy(new SyncOperation(syncJob1, afsClient, uploadMonitor, downloadMonitor, syncJobEventDAO, Path.of("/hidden-dir"), notificationManager));

        Path remotePath = Path.of("/remoteroot/path");
        Instant remoteLastModified = Instant.now();
        File file = new File("owner", "path", "name", false, 1000L, OffsetDateTime.ofInstant(remoteLastModified, ZoneOffset.UTC));
        Mockito.doReturn(Optional.of(file)).when(syncOperation).getRemoteFilePresence(remotePath);
        Assert.assertEquals(remoteLastModified, syncOperation.getRemoteFileInfo(remotePath).get().getLastModifiedDate());
        Assert.assertEquals(false, syncOperation.getRemoteFileInfo(remotePath).get().isDirectory());

        File dir = new File("owner", "/remoteroot/path", "name", true, 1000L, null);
        Mockito.doReturn(Optional.of(dir)).when(syncOperation).getRemoteFilePresence(remotePath);
        Mockito.doReturn(new File[] { new File("owner", "/remoteroot/path", "name", true, 1000L, OffsetDateTime.ofInstant(remoteLastModified, ZoneOffset.UTC)) } ).when(syncOperation).getRemoteFileList(Path.of("/remoteroot"));
        Assert.assertEquals(remoteLastModified, syncOperation.getRemoteFileInfo(remotePath).get().getLastModifiedDate());
        Assert.assertEquals(true, syncOperation.getRemoteFileInfo(remotePath).get().isDirectory());

        Mockito.doReturn(Optional.empty()).when(syncOperation).getRemoteFilePresence(remotePath);
        Assert.assertEquals(Optional.empty(), syncOperation.getRemoteFileInfo(remotePath));
    }

    @Test
    public void testGetSyncJobEvent() throws Exception {
        SyncOperation.AfsClientProxy afsClient = Mockito.mock(SyncOperation.AfsClientProxy.class);
        ClientAPI.TransferMonitorListener uploadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        ClientAPI.TransferMonitorListener downloadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        NotificationManager notificationManager = Mockito.mock(NotificationManager.class);

        SyncJobEventDAO syncJobEventDAO1 = Mockito.mock(SyncJobEventDAO.class);
        SyncJob syncJob1 = new SyncJob(SyncJob.Type.Upload, "url", "uuid", "token", "/remotedir1", "/localdir1", true);
        SyncOperation syncOperation1 = Mockito.spy(new SyncOperation(syncJob1, afsClient, uploadMonitor, downloadMonitor, syncJobEventDAO1, Path.of("/hidden-dir"), notificationManager));

        SyncJobEventDAO syncJobEventDAO2 = Mockito.mock(SyncJobEventDAO.class);
        SyncJob syncJob2 = new SyncJob(SyncJob.Type.Upload, "url", "uuid", "token", "/remotedir1", "/localdir1", true);
        SyncOperation syncOperation2 = Mockito.spy(new SyncOperation(syncJob2, afsClient, uploadMonitor, downloadMonitor, syncJobEventDAO2, Path.of("/hidden-dir"), notificationManager));

        SyncJobEventDAO syncJobEventDAO3 = Mockito.mock(SyncJobEventDAO.class);
        SyncJob syncJob3 = new SyncJob(SyncJob.Type.Download, "url", "uuid", "token", "/remotedir1", "/localdir1", true);
        SyncOperation syncOperation3 = Mockito.spy(new SyncOperation(syncJob3, afsClient, uploadMonitor, downloadMonitor, syncJobEventDAO3, Path.of("/hidden-dir"), notificationManager));

        SyncJobEventDAO syncJobEventDAO4 = Mockito.mock(SyncJobEventDAO.class);
        SyncJob syncJob4 = new SyncJob(SyncJob.Type.Download, "url", "uuid", "token", "/remotedir1", "/localdir1", true);
        SyncOperation syncOperation4 = Mockito.spy(new SyncOperation(syncJob4, afsClient, uploadMonitor, downloadMonitor, syncJobEventDAO4, Path.of("/hidden-dir"), notificationManager));

        SyncJobEventDAO syncJobEventDAO5 = Mockito.mock(SyncJobEventDAO.class);
        SyncJob syncJob5 = new SyncJob(SyncJob.Type.Bidirectional, "url", "uuid", "token", "/remotedir1", "/localdir1", true);
        SyncOperation syncOperation5 = Mockito.spy(new SyncOperation(syncJob5, afsClient, uploadMonitor, downloadMonitor, syncJobEventDAO5, Path.of("/hidden-dir"), notificationManager));

        SyncJobEventDAO syncJobEventDAO6 = Mockito.mock(SyncJobEventDAO.class);
        SyncJob syncJob6 = new SyncJob(SyncJob.Type.Bidirectional, "url", "uuid", "token", "/remotedir1", "/localdir1", true);
        SyncOperation syncOperation6 = Mockito.spy(new SyncOperation(syncJob6, afsClient, uploadMonitor, downloadMonitor, syncJobEventDAO6, Path.of("/hidden-dir"), notificationManager));

        Path sourcePath = Path.of("local");
        Path destinationPath = Path.of("remote");

        syncOperation1.getSyncJobEvent(SyncJobEvent.SyncDirection.UP, sourcePath, destinationPath);
        Mockito.verify(syncJobEventDAO1, Mockito.times(1)).selectByPrimaryKey(SyncJobEvent.SyncDirection.UP, sourcePath.toAbsolutePath().toString(), toServerPathString(destinationPath));
        Mockito.verify(syncJobEventDAO1, Mockito.times(0)).selectByPrimaryKey(Mockito.eq(SyncJobEvent.SyncDirection.DOWN), Mockito.anyString(), Mockito.anyString());

        try {
            syncOperation2.getSyncJobEvent(SyncJobEvent.SyncDirection.DOWN, sourcePath, destinationPath);
        } catch (Exception e) {
            Assert.assertEquals(IllegalStateException.class, e.getClass());
            Assert.assertEquals("Incompatible SyncDirection and JobType",e.getMessage());
        }

        try {
            syncOperation3.getSyncJobEvent(SyncJobEvent.SyncDirection.UP, sourcePath, destinationPath);
        } catch (Exception e) {
            Assert.assertEquals(IllegalStateException.class, e.getClass());
            Assert.assertEquals("Incompatible SyncDirection and JobType",e.getMessage());
        }

        syncOperation4.getSyncJobEvent(SyncJobEvent.SyncDirection.DOWN, sourcePath, destinationPath);
        Mockito.verify(syncJobEventDAO4, Mockito.times(1)).selectByPrimaryKey(SyncJobEvent.SyncDirection.DOWN, destinationPath.toAbsolutePath().toString(), toServerPathString(sourcePath));
        Mockito.verify(syncJobEventDAO4, Mockito.times(0)).selectByPrimaryKey(Mockito.eq(SyncJobEvent.SyncDirection.UP), Mockito.anyString(), Mockito.anyString());

        syncOperation5.getSyncJobEvent(SyncJobEvent.SyncDirection.UP, sourcePath, destinationPath);
        Mockito.verify(syncJobEventDAO5, Mockito.times(1)).selectByPrimaryKey(SyncJobEvent.SyncDirection.UP, sourcePath.toAbsolutePath().toString(), toServerPathString(destinationPath));
        Mockito.verify(syncJobEventDAO5, Mockito.times(1)).selectByPrimaryKey(SyncJobEvent.SyncDirection.DOWN, sourcePath.toAbsolutePath().toString(), toServerPathString(destinationPath));

        syncOperation6.getSyncJobEvent(SyncJobEvent.SyncDirection.DOWN, sourcePath, destinationPath);
        Mockito.verify(syncJobEventDAO6, Mockito.times(1)).selectByPrimaryKey(SyncJobEvent.SyncDirection.UP, destinationPath.toAbsolutePath().toString(), toServerPathString(sourcePath));
        Mockito.verify(syncJobEventDAO6, Mockito.times(1)).selectByPrimaryKey(SyncJobEvent.SyncDirection.DOWN, destinationPath.toAbsolutePath().toString(), toServerPathString(sourcePath));
    }


    @Test
    public void testInsertOrUpdateNewSyncEntry() throws Exception {
        SyncOperation.AfsClientProxy afsClient = Mockito.mock(SyncOperation.AfsClientProxy.class);
        ClientAPI.TransferMonitorListener uploadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        ClientAPI.TransferMonitorListener downloadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        NotificationManager notificationManager = Mockito.mock(NotificationManager.class);

        SyncJobEventDAO syncJobEventDAO1 = Mockito.mock(SyncJobEventDAO.class);
        SyncJob syncJob1 = new SyncJob(SyncJob.Type.Upload, "url", "uuid", "token", "/remotedir1", "/localdir1", true);
        SyncOperation syncOperation1 = Mockito.spy(new SyncOperation(syncJob1, afsClient, uploadMonitor, downloadMonitor, syncJobEventDAO1, Path.of("/hidden-dir"), notificationManager));

        Path sourcePath = Path.of("source");
        Path destinationPath = Path.of("destination");
        Instant now = Instant.now();
        syncOperation1.insertNewSyncEntry(SyncJobEvent.SyncDirection.UP, sourcePath, destinationPath, now);
        syncOperation1.insertNewSyncEntry(SyncJobEvent.SyncDirection.DOWN, sourcePath, destinationPath, now);
        ArgumentCaptor<SyncJobEvent> syncJobEventArgumentCaptor = ArgumentCaptor.forClass(SyncJobEvent.class);
        Mockito.verify(syncJobEventDAO1, Mockito.times(2)).insertOrUpdate(syncJobEventArgumentCaptor.capture());

        Assert.assertEquals(
                SyncJobEvent.builder().syncDirection(SyncJobEvent.SyncDirection.UP).localFile(sourcePath.toAbsolutePath().toString()).remoteFile(toServerPathString(destinationPath))
                        .entityPermId(syncJob1.getEntityPermId()).localDirectoryRoot(syncJob1.getLocalDirectoryRoot()).sourceTimestamp(now.toEpochMilli()).timestamp(0L).build(), syncJobEventArgumentCaptor.getAllValues().get(0).toBuilder().timestamp(0L).build());
        Assert.assertEquals(
                SyncJobEvent.builder().syncDirection(SyncJobEvent.SyncDirection.DOWN).localFile(destinationPath.toAbsolutePath().toString()).remoteFile(toServerPathString(sourcePath))
                        .entityPermId(syncJob1.getEntityPermId()).localDirectoryRoot(syncJob1.getLocalDirectoryRoot()).sourceTimestamp(now.toEpochMilli()).timestamp(0L).build(), syncJobEventArgumentCaptor.getAllValues().get(1).toBuilder().timestamp(0L).build());

        Assert.assertTrue(
                syncJobEventArgumentCaptor.getAllValues().get(0).getTimestamp() >= now.toEpochMilli() && syncJobEventArgumentCaptor.getAllValues().get(0).getTimestamp() <= Instant.now().toEpochMilli());

        Assert.assertTrue(
                syncJobEventArgumentCaptor.getAllValues().get(0).getTimestamp() >= now.toEpochMilli() && syncJobEventArgumentCaptor.getAllValues().get(1).getTimestamp() <= Instant.now().toEpochMilli());

    }


    @Test
    public void testInsertNewSyncEntryForDirectory() throws Exception {
        SyncOperation.AfsClientProxy afsClient = Mockito.mock(SyncOperation.AfsClientProxy.class);
        ClientAPI.TransferMonitorListener uploadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        ClientAPI.TransferMonitorListener downloadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        NotificationManager notificationManager = Mockito.mock(NotificationManager.class);

        SyncJobEventDAO syncJobEventDAO1 = Mockito.mock(SyncJobEventDAO.class);
        SyncJob syncJob1 = new SyncJob(SyncJob.Type.Upload, "url", "uuid", "token", "/remotedir1", "/localdir1", true);
        SyncOperation syncOperation1 = Mockito.spy(new SyncOperation(syncJob1, afsClient, uploadMonitor, downloadMonitor, syncJobEventDAO1, Path.of("/hidden-dir"), notificationManager));

        Path sourcePath = Path.of("source");
        Path destinationPath = Path.of("destination");
        Instant now = Instant.now();
        syncOperation1.insertNewCompletedSyncEntry(SyncJobEvent.SyncDirection.UP, sourcePath, destinationPath, now, true);
        syncOperation1.insertNewCompletedSyncEntry(SyncJobEvent.SyncDirection.DOWN, sourcePath, destinationPath, now, true);
        ArgumentCaptor<SyncJobEvent> syncJobEventArgumentCaptor = ArgumentCaptor.forClass(SyncJobEvent.class);
        Mockito.verify(syncJobEventDAO1, Mockito.times(2)).insertOrUpdate(syncJobEventArgumentCaptor.capture());

        Assert.assertEquals(
                SyncJobEvent.builder().syncDirection(SyncJobEvent.SyncDirection.UP).localFile(sourcePath.toAbsolutePath().toString()).remoteFile(toServerPathString(destinationPath))
                        .entityPermId(syncJob1.getEntityPermId()).localDirectoryRoot(syncJob1.getLocalDirectoryRoot()).directory(true).sourceTimestamp(now.toEpochMilli()).destinationTimestamp(0L).timestamp(0L).build(), syncJobEventArgumentCaptor.getAllValues().get(0).toBuilder().destinationTimestamp(0L).timestamp(0L).build());
        Assert.assertEquals(
                SyncJobEvent.builder().syncDirection(SyncJobEvent.SyncDirection.DOWN).localFile(destinationPath.toAbsolutePath().toString()).remoteFile(toServerPathString(sourcePath))
                        .entityPermId(syncJob1.getEntityPermId()).localDirectoryRoot(syncJob1.getLocalDirectoryRoot()).directory(true).sourceTimestamp(now.toEpochMilli()).destinationTimestamp(0L).timestamp(0L).build(), syncJobEventArgumentCaptor.getAllValues().get(1).toBuilder().destinationTimestamp(0L).timestamp(0L).build());

        Assert.assertTrue(
                syncJobEventArgumentCaptor.getAllValues().get(0).getTimestamp() >= now.toEpochMilli() && syncJobEventArgumentCaptor.getAllValues().get(0).getTimestamp() <= Instant.now().toEpochMilli());

        Assert.assertTrue(
                syncJobEventArgumentCaptor.getAllValues().get(0).getTimestamp() >= now.toEpochMilli() && syncJobEventArgumentCaptor.getAllValues().get(1).getTimestamp() <= Instant.now().toEpochMilli());

        Assert.assertTrue(
                syncJobEventArgumentCaptor.getAllValues().get(0).getTimestamp() >= now.toEpochMilli() && syncJobEventArgumentCaptor.getAllValues().get(0).getDestinationTimestamp() <= Instant.now().toEpochMilli());

        Assert.assertTrue(
                syncJobEventArgumentCaptor.getAllValues().get(0).getTimestamp() >= now.toEpochMilli() && syncJobEventArgumentCaptor.getAllValues().get(1).getDestinationTimestamp() <= Instant.now().toEpochMilli());
    }

    @Test
    public void testInsertNewSyncEntryForSourceDeleted() throws Exception {
        for(boolean directory : List.of(Boolean.FALSE, Boolean.TRUE)) {
            SyncOperation.AfsClientProxy afsClient = Mockito.mock(SyncOperation.AfsClientProxy.class);
            ClientAPI.TransferMonitorListener uploadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
            ClientAPI.TransferMonitorListener downloadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
            NotificationManager notificationManager = Mockito.mock(NotificationManager.class);

            SyncJobEventDAO syncJobEventDAO1 = Mockito.mock(SyncJobEventDAO.class);
            SyncJob syncJob1 = new SyncJob(SyncJob.Type.Upload, "url", "uuid", "token", "/remotedir1", "/localdir1", true);
            SyncOperation syncOperation1 = Mockito.spy(new SyncOperation(syncJob1, afsClient, uploadMonitor, downloadMonitor, syncJobEventDAO1, Path.of("/hidden-dir"), notificationManager));

            Path sourcePath = Path.of("source");
            Path destinationPath = Path.of("destination");
            Instant now = Instant.now();
            syncOperation1.insertNewSyncEntryForSourceDeleted(SyncJobEvent.SyncDirection.UP, sourcePath, destinationPath, directory);
            syncOperation1.insertNewSyncEntryForSourceDeleted(SyncJobEvent.SyncDirection.DOWN, sourcePath, destinationPath, directory);
            ArgumentCaptor<SyncJobEvent> syncJobEventArgumentCaptor = ArgumentCaptor.forClass(SyncJobEvent.class);
            Mockito.verify(syncJobEventDAO1, Mockito.times(2)).insertOrUpdate(syncJobEventArgumentCaptor.capture());

            Assert.assertEquals(
                    SyncJobEvent.builder().syncDirection(SyncJobEvent.SyncDirection.UP).localFile(sourcePath.toAbsolutePath().toString()).remoteFile(toServerPathString(destinationPath))
                            .entityPermId(syncJob1.getEntityPermId()).localDirectoryRoot(syncJob1.getLocalDirectoryRoot()).sourceDeleted(true).directory(directory).sourceTimestamp(0L).destinationTimestamp(null).timestamp(0L).build(), syncJobEventArgumentCaptor.getAllValues().get(0).toBuilder().sourceTimestamp(0L).timestamp(0L).build());
            Assert.assertEquals(
                    SyncJobEvent.builder().syncDirection(SyncJobEvent.SyncDirection.DOWN).localFile(destinationPath.toAbsolutePath().toString()).remoteFile(toServerPathString(sourcePath))
                            .entityPermId(syncJob1.getEntityPermId()).localDirectoryRoot(syncJob1.getLocalDirectoryRoot()).sourceDeleted(true).directory(directory).sourceTimestamp(0L).destinationTimestamp(null).timestamp(0L).build(), syncJobEventArgumentCaptor.getAllValues().get(1).toBuilder().sourceTimestamp(0L).timestamp(0L).build());

            Assert.assertTrue(
                    syncJobEventArgumentCaptor.getAllValues().get(0).getTimestamp() >= now.toEpochMilli() && syncJobEventArgumentCaptor.getAllValues().get(0).getTimestamp() <= Instant.now().toEpochMilli());

            Assert.assertTrue(
                    syncJobEventArgumentCaptor.getAllValues().get(0).getTimestamp() >= now.toEpochMilli() && syncJobEventArgumentCaptor.getAllValues().get(1).getTimestamp() <= Instant.now().toEpochMilli());

            Assert.assertTrue(
                    syncJobEventArgumentCaptor.getAllValues().get(0).getSourceTimestamp() >= now.toEpochMilli() && syncJobEventArgumentCaptor.getAllValues().get(0).getSourceTimestamp() <= Instant.now().toEpochMilli());

            Assert.assertTrue(
                    syncJobEventArgumentCaptor.getAllValues().get(0).getSourceTimestamp() >= now.toEpochMilli() && syncJobEventArgumentCaptor.getAllValues().get(1).getSourceTimestamp() <= Instant.now().toEpochMilli());

        }
    }

    @Test
    public void testSkipLocalHiddenAppDirPrecheck() {
        SyncOperation.AfsClientProxy afsClient = Mockito.mock(SyncOperation.AfsClientProxy.class);
        ClientAPI.TransferMonitorListener uploadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        ClientAPI.TransferMonitorListener downloadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        NotificationManager notificationManager = Mockito.mock(NotificationManager.class);

        SyncJobEventDAO syncJobEventDAO1 = Mockito.mock(SyncJobEventDAO.class);
        SyncJob syncJob1 = new SyncJob(SyncJob.Type.Upload, "url", "uuid", "token", "/remotedir1", "/localdir1", true);
        SyncOperation syncOperation1 = Mockito.spy(new SyncOperation(syncJob1, afsClient, uploadMonitor, downloadMonitor, syncJobEventDAO1, Path.of("/hidden-dir"), notificationManager));

        Assert.assertTrue(syncOperation1.skipAppPrivateFilesPrecheck(SyncJobEvent.SyncDirection.UP, Path.of("/hidden-dir"), Path.of("/remote")));
        Assert.assertTrue(syncOperation1.skipAppPrivateFilesPrecheck(SyncJobEvent.SyncDirection.DOWN, Path.of("/remote"), Path.of("/hidden-dir")));
        Assert.assertTrue(syncOperation1.skipAppPrivateFilesPrecheck(SyncJobEvent.SyncDirection.UP, Path.of("/hidden-dir/subfile"), Path.of("/remote")));
        Assert.assertTrue(syncOperation1.skipAppPrivateFilesPrecheck(SyncJobEvent.SyncDirection.DOWN, Path.of("/remote"), Path.of("/hidden-dir/subfile")));
        Assert.assertTrue(syncOperation1.skipAppPrivateFilesPrecheck(SyncJobEvent.SyncDirection.UP, Path.of("/hidden-dir/subfile/subfile2"), Path.of("/remote")));
        Assert.assertTrue(syncOperation1.skipAppPrivateFilesPrecheck(SyncJobEvent.SyncDirection.DOWN, Path.of("/remote"), Path.of("/hidden-dir/subfile/subfile2")));

        Assert.assertFalse(syncOperation1.skipAppPrivateFilesPrecheck(SyncJobEvent.SyncDirection.DOWN, Path.of("/hidden-dir"), Path.of("/remote")));
        Assert.assertFalse(syncOperation1.skipAppPrivateFilesPrecheck(SyncJobEvent.SyncDirection.UP, Path.of("/remote"), Path.of("/hidden-dir")));
        Assert.assertFalse(syncOperation1.skipAppPrivateFilesPrecheck(SyncJobEvent.SyncDirection.DOWN, Path.of("/hidden-dir/subfile"), Path.of("/remote")));
        Assert.assertFalse(syncOperation1.skipAppPrivateFilesPrecheck(SyncJobEvent.SyncDirection.UP, Path.of("/remote"), Path.of("/hidden-dir/subfile")));
        Assert.assertFalse(syncOperation1.skipAppPrivateFilesPrecheck(SyncJobEvent.SyncDirection.DOWN, Path.of("/hidden-dir/subfile/subfile2"), Path.of("/remote")));
        Assert.assertFalse(syncOperation1.skipAppPrivateFilesPrecheck(SyncJobEvent.SyncDirection.UP, Path.of("/remote"), Path.of("/hidden-dir/subfile/subfile2")));

        Assert.assertFalse(syncOperation1.skipAppPrivateFilesPrecheck(SyncJobEvent.SyncDirection.UP, Path.of("/good-dir"), Path.of("/remote")));
        Assert.assertFalse(syncOperation1.skipAppPrivateFilesPrecheck(SyncJobEvent.SyncDirection.DOWN, Path.of("/remote"), Path.of("/good-dir")));
        Assert.assertFalse(syncOperation1.skipAppPrivateFilesPrecheck(SyncJobEvent.SyncDirection.UP, Path.of("/good-dir/subfile"), Path.of("/remote")));
        Assert.assertFalse(syncOperation1.skipAppPrivateFilesPrecheck(SyncJobEvent.SyncDirection.DOWN, Path.of("/remote"), Path.of("/good-dir/subfile")));
        Assert.assertFalse(syncOperation1.skipAppPrivateFilesPrecheck(SyncJobEvent.SyncDirection.UP, Path.of("/good-dir/subfile/subfile2"), Path.of("/remote")));
        Assert.assertFalse(syncOperation1.skipAppPrivateFilesPrecheck(SyncJobEvent.SyncDirection.DOWN, Path.of("/remote"), Path.of("/good-dir/subfile/subfile2")));

    }

    @Test
    public void testSkipAppPrivateConflictFilesPrecheck() {
        SyncOperation.AfsClientProxy afsClient = Mockito.mock(SyncOperation.AfsClientProxy.class);
        ClientAPI.TransferMonitorListener uploadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        ClientAPI.TransferMonitorListener downloadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        NotificationManager notificationManager = Mockito.mock(NotificationManager.class);

        SyncJobEventDAO syncJobEventDAO1 = Mockito.mock(SyncJobEventDAO.class);
        SyncJob syncJob1 = new SyncJob(SyncJob.Type.Upload, "url", "uuid", "token", "/remotedir1", "/localdir1", true);
        SyncOperation syncOperation1 = Mockito.spy(new SyncOperation(syncJob1, afsClient, uploadMonitor, downloadMonitor, syncJobEventDAO1, Path.of("/hidden-dir"), notificationManager));

        Assert.assertTrue(syncOperation1.skipAppPrivateFilesPrecheck(SyncJobEvent.SyncDirection.UP, Path.of("/local/test.openbis-conflict"), Path.of("/remote/test.openbis-conflict")));
        Assert.assertTrue(syncOperation1.skipAppPrivateFilesPrecheck(SyncJobEvent.SyncDirection.UP, Path.of("/local/test.openbis-conflict"), Path.of("/remote/test")));
        Assert.assertTrue(syncOperation1.skipAppPrivateFilesPrecheck(SyncJobEvent.SyncDirection.UP, Path.of("/local/test"), Path.of("/remote/test.openbis-conflict")));
        Assert.assertFalse(syncOperation1.skipAppPrivateFilesPrecheck(SyncJobEvent.SyncDirection.UP, Path.of("/local/test"), Path.of("/remote/test")));

        Assert.assertTrue(syncOperation1.skipAppPrivateFilesPrecheck(SyncJobEvent.SyncDirection.DOWN, Path.of("/local/test.openbis-conflict"), Path.of("/remote/test.openbis-conflict")));
        Assert.assertTrue(syncOperation1.skipAppPrivateFilesPrecheck(SyncJobEvent.SyncDirection.DOWN, Path.of("/local/test.openbis-conflict"), Path.of("/remote/test")));
        Assert.assertTrue(syncOperation1.skipAppPrivateFilesPrecheck(SyncJobEvent.SyncDirection.DOWN, Path.of("/local/test"), Path.of("/remote/test.openbis-conflict")));
        Assert.assertFalse(syncOperation1.skipAppPrivateFilesPrecheck(SyncJobEvent.SyncDirection.DOWN, Path.of("/local/test"), Path.of("/remote/test")));
    }

    @Test
    public void testPickMoreRecentCompletedFileSyncState() {
        Instant now = Instant.now();

        SyncJobEvent incompleteSyncJobEvent = SyncJobEvent.builder().syncDirection(SyncJobEvent.SyncDirection.UP).localFile("loc").remoteFile("rem")
                .entityPermId("ownerId").localDirectoryRoot("locRoot").sourceTimestamp(Instant.now().toEpochMilli()).timestamp(now.toEpochMilli()).destinationTimestamp(null).build();

        SyncJobEvent syncJobEventWithSourceDeleted = SyncJobEvent.builder().syncDirection(SyncJobEvent.SyncDirection.UP).localFile("loc").remoteFile("rem")
                .entityPermId("ownerId").localDirectoryRoot("locRoot").sourceTimestamp(Instant.now().toEpochMilli()).timestamp(now.plusMillis(500).toEpochMilli()).destinationTimestamp(null).sourceDeleted(true).build();

        SyncJobEvent completeSyncJobEvent1 = SyncJobEvent.builder().syncDirection(SyncJobEvent.SyncDirection.UP).localFile("loc").remoteFile("rem")
                .entityPermId("ownerId").localDirectoryRoot("locRoot").sourceTimestamp(Instant.now().toEpochMilli()).destinationTimestamp(now.toEpochMilli()).timestamp(now.toEpochMilli()).build();
        SyncJobEvent completeSyncJobEvent2 = SyncJobEvent.builder().syncDirection(SyncJobEvent.SyncDirection.UP).localFile("loc").remoteFile("rem")
                .entityPermId("ownerId").localDirectoryRoot("locRoot").sourceTimestamp(Instant.now().toEpochMilli()).destinationTimestamp(now.plusMillis(1000).toEpochMilli()).timestamp(now.plusMillis(1000).toEpochMilli()).build();

        Assert.assertEquals(null, SyncOperation.pickMoreRecentCompletedFileSyncState(null, null));
        Assert.assertEquals(null, SyncOperation.pickMoreRecentCompletedFileSyncState(incompleteSyncJobEvent, null));
        Assert.assertEquals(null, SyncOperation.pickMoreRecentCompletedFileSyncState(null, incompleteSyncJobEvent));
        Assert.assertEquals(null, SyncOperation.pickMoreRecentCompletedFileSyncState(incompleteSyncJobEvent, incompleteSyncJobEvent));

        Assert.assertEquals(syncJobEventWithSourceDeleted, SyncOperation.pickMoreRecentCompletedFileSyncState(null, syncJobEventWithSourceDeleted));
        Assert.assertEquals(syncJobEventWithSourceDeleted, SyncOperation.pickMoreRecentCompletedFileSyncState(syncJobEventWithSourceDeleted, null));

        Assert.assertEquals(syncJobEventWithSourceDeleted, SyncOperation.pickMoreRecentCompletedFileSyncState(incompleteSyncJobEvent, syncJobEventWithSourceDeleted));
        Assert.assertEquals(syncJobEventWithSourceDeleted, SyncOperation.pickMoreRecentCompletedFileSyncState(syncJobEventWithSourceDeleted, incompleteSyncJobEvent));

        Assert.assertEquals(syncJobEventWithSourceDeleted, SyncOperation.pickMoreRecentCompletedFileSyncState(completeSyncJobEvent1, syncJobEventWithSourceDeleted));
        Assert.assertEquals(syncJobEventWithSourceDeleted, SyncOperation.pickMoreRecentCompletedFileSyncState(syncJobEventWithSourceDeleted, completeSyncJobEvent1));

        Assert.assertEquals(completeSyncJobEvent2, SyncOperation.pickMoreRecentCompletedFileSyncState(completeSyncJobEvent2, syncJobEventWithSourceDeleted));
        Assert.assertEquals(completeSyncJobEvent2, SyncOperation.pickMoreRecentCompletedFileSyncState(syncJobEventWithSourceDeleted, completeSyncJobEvent2));

        Assert.assertEquals(completeSyncJobEvent2, SyncOperation.pickMoreRecentCompletedFileSyncState(incompleteSyncJobEvent, completeSyncJobEvent2));
        Assert.assertEquals(completeSyncJobEvent1, SyncOperation.pickMoreRecentCompletedFileSyncState(completeSyncJobEvent1, incompleteSyncJobEvent));
        Assert.assertEquals(completeSyncJobEvent2, SyncOperation.pickMoreRecentCompletedFileSyncState(completeSyncJobEvent1, completeSyncJobEvent2));
        Assert.assertEquals(completeSyncJobEvent2, SyncOperation.pickMoreRecentCompletedFileSyncState(completeSyncJobEvent2, completeSyncJobEvent1));
    }

    @Test
    public void testSyncTransferredFileListenerUploadWithoutDbUpdate() throws Exception {
        SyncOperation.AfsClientProxy afsClient = Mockito.mock(SyncOperation.AfsClientProxy.class);
        ClientAPI.TransferMonitorListener uploadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        ClientAPI.TransferMonitorListener downloadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        NotificationManager notificationManager = Mockito.mock(NotificationManager.class);

        SyncJobEventDAO syncJobEventDAO1 = Mockito.mock(SyncJobEventDAO.class);
        SyncJob syncJob1 = new SyncJob(SyncJob.Type.Bidirectional, "url", "uuid", "token", "/remotedir1", "/localdir1", true);
        SyncOperation syncOperation1 = Mockito.spy(new SyncOperation(syncJob1, afsClient, uploadMonitor, downloadMonitor, syncJobEventDAO1, Path.of("/hidden-dir"), notificationManager));

        SyncOperation.SyncTaskFileTransferredListener syncTaskFileDownloadListener = syncOperation1.new SyncTaskFileTransferredListener(SyncJobEvent.SyncDirection.UP);

        Path sourcePath = Path.of("source");
        Path destinationPath = Path.of("destination");

        Mockito.doReturn(null).when(syncJobEventDAO1).selectByPrimaryKey(Mockito.any(), Mockito.anyString(), Mockito.anyString());
        syncTaskFileDownloadListener.transferred(sourcePath, destinationPath);
        ArgumentCaptor<SyncJobEvent> syncJobEventArgumentCaptor = ArgumentCaptor.forClass(SyncJobEvent.class);
        Mockito.verify(syncJobEventDAO1, Mockito.times(1)).selectByPrimaryKey(Mockito.any(), Mockito.anyString(), Mockito.anyString());
        Mockito.verify(syncJobEventDAO1, Mockito.times(0)).insertOrUpdate(Mockito.any());
        Mockito.verify(syncOperation1, Mockito.times(1)).removePossibleConflictNotification(sourcePath, destinationPath, SyncJobEvent.SyncDirection.UP);
    }

    @Test
    public void testSyncTransferredFileListenerUploadWithDbUpdate() throws Exception {
        SyncOperation.AfsClientProxy afsClient = Mockito.mock(SyncOperation.AfsClientProxy.class);
        ClientAPI.TransferMonitorListener uploadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        ClientAPI.TransferMonitorListener downloadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        NotificationManager notificationManager = Mockito.mock(NotificationManager.class);

        SyncJobEventDAO syncJobEventDAO1 = Mockito.mock(SyncJobEventDAO.class);
        SyncJob syncJob1 = new SyncJob(SyncJob.Type.Bidirectional, "url", "uuid", "token", "/remotedir1", "/localdir1", true);
        SyncOperation syncOperation1 = Mockito.spy(new SyncOperation(syncJob1, afsClient, uploadMonitor, downloadMonitor, syncJobEventDAO1, Path.of("/hidden-dir"), notificationManager));

        SyncOperation.SyncTaskFileTransferredListener syncTaskFileDownloadListener = syncOperation1.new SyncTaskFileTransferredListener(SyncJobEvent.SyncDirection.UP);

        Path sourcePath = Path.of("source");
        Path destinationPath = Path.of("destination");

        SyncJobEvent selectedSyncEntry = SyncJobEvent.builder().syncDirection(SyncJobEvent.SyncDirection.UP).localFile(sourcePath.toAbsolutePath().toString()).remoteFile(toServerPathString(destinationPath))
                .entityPermId(syncJob1.getEntityPermId()).localDirectoryRoot(syncJob1.getLocalDirectoryRoot()).sourceTimestamp(Instant.now().toEpochMilli()).timestamp(System.currentTimeMillis()).build();
        Mockito.doReturn(selectedSyncEntry).when(syncJobEventDAO1).selectByPrimaryKey(Mockito.any(), Mockito.anyString(), Mockito.anyString());
        Instant destinationLastModified = Instant.now();
        Mockito.doReturn(Optional.of(new SyncOperation.FileInfo(false, destinationLastModified))).when(syncOperation1).getRemoteFileInfo(destinationPath);
        syncTaskFileDownloadListener.transferred(sourcePath, destinationPath);
        ArgumentCaptor<SyncJobEvent> updatedSyncJobEventArgumentCaptor = ArgumentCaptor.forClass(SyncJobEvent.class);
        Mockito.verify(syncJobEventDAO1, Mockito.times(1)).selectByPrimaryKey(SyncJobEvent.SyncDirection.UP, sourcePath.toAbsolutePath().toString(), toServerPathString(destinationPath));
        Mockito.verify(syncJobEventDAO1, Mockito.times(1)).insertOrUpdate(updatedSyncJobEventArgumentCaptor.capture());

        Assert.assertEquals(destinationLastModified.toEpochMilli(), (long) updatedSyncJobEventArgumentCaptor.getValue().getDestinationTimestamp());
        Assert.assertEquals(selectedSyncEntry.toBuilder().timestamp(0L).build(), updatedSyncJobEventArgumentCaptor.getValue().toBuilder().timestamp(0L).destinationTimestamp(null).build());
        Assert.assertTrue(updatedSyncJobEventArgumentCaptor.getValue().getTimestamp() >= destinationLastModified.toEpochMilli() && updatedSyncJobEventArgumentCaptor.getValue().getTimestamp() <= Instant.now().toEpochMilli());
        Mockito.verify(syncOperation1, Mockito.times(1)).removePossibleConflictNotification(sourcePath, destinationPath, SyncJobEvent.SyncDirection.UP);
    }

    @Test
    public void testSyncTransferredFileListenerDownload() throws Exception {
        SyncOperation.AfsClientProxy afsClient = Mockito.mock(SyncOperation.AfsClientProxy.class);
        ClientAPI.TransferMonitorListener uploadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        ClientAPI.TransferMonitorListener downloadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        NotificationManager notificationManager = Mockito.mock(NotificationManager.class);

        SyncJobEventDAO syncJobEventDAO1 = Mockito.mock(SyncJobEventDAO.class);
        SyncJob syncJob1 = new SyncJob(SyncJob.Type.Bidirectional, "url", "uuid", "token", "/remotedir1", "/localdir1", true);
        SyncOperation syncOperation1 = Mockito.spy(new SyncOperation(syncJob1, afsClient, uploadMonitor, downloadMonitor, syncJobEventDAO1, Path.of("/hidden-dir"), notificationManager));

        SyncOperation.SyncTaskFileTransferredListener syncTaskFileDownloadListener = syncOperation1.new SyncTaskFileTransferredListener(SyncJobEvent.SyncDirection.DOWN);

        Path sourcePath = Path.of("source");
        Path destinationPath = Path.of("destination");

        SyncJobEvent selectedSyncEntry = SyncJobEvent.builder().syncDirection(SyncJobEvent.SyncDirection.UP).localFile(sourcePath.toAbsolutePath().toString()).remoteFile(toServerPathString(destinationPath))
                .entityPermId(syncJob1.getEntityPermId()).localDirectoryRoot(syncJob1.getLocalDirectoryRoot()).sourceTimestamp(Instant.now().toEpochMilli()).timestamp(System.currentTimeMillis()).build();
        Mockito.doReturn(selectedSyncEntry).when(syncJobEventDAO1).selectByPrimaryKey(Mockito.any(), Mockito.anyString(), Mockito.anyString());
        Instant destinationLastModified = Instant.now();
        Mockito.doReturn(Optional.of(new SyncOperation.FileInfo(false, destinationLastModified))).when(syncOperation1).getLocalFileInfo(destinationPath);
        syncTaskFileDownloadListener.transferred(sourcePath, destinationPath);
        Mockito.verify(syncJobEventDAO1, Mockito.times(1)).selectByPrimaryKey(SyncJobEvent.SyncDirection.DOWN, destinationPath.toAbsolutePath().toString(), toServerPathString(sourcePath));
        ArgumentCaptor<SyncJobEvent> updatedSyncJobEventArgumentCaptor = ArgumentCaptor.forClass(SyncJobEvent.class);
        Mockito.verify(syncJobEventDAO1, Mockito.times(1)).insertOrUpdate(updatedSyncJobEventArgumentCaptor.capture());
        Assert.assertEquals(destinationLastModified.toEpochMilli(), (long) updatedSyncJobEventArgumentCaptor.getValue().getDestinationTimestamp());
        Assert.assertEquals(selectedSyncEntry.toBuilder().timestamp(0L).build(), updatedSyncJobEventArgumentCaptor.getValue().toBuilder().timestamp(0L).destinationTimestamp(null).build());
        Assert.assertTrue(updatedSyncJobEventArgumentCaptor.getValue().getTimestamp() >= destinationLastModified.toEpochMilli() && updatedSyncJobEventArgumentCaptor.getValue().getTimestamp() <= Instant.now().toEpochMilli());
        Mockito.verify(syncOperation1, Mockito.times(1)).removePossibleConflictNotification(sourcePath, destinationPath, SyncJobEvent.SyncDirection.DOWN);
    }

    @Test
    public void testFileSyncCollisionListenerDeleteSourceFile() throws Exception {
        for (SyncJobEvent.SyncDirection syncDirection : SyncJobEvent.SyncDirection.values()) {
            SyncOperation.AfsClientProxy afsClient = Mockito.mock(SyncOperation.AfsClientProxy.class);
            ClientAPI.TransferMonitorListener uploadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
            ClientAPI.TransferMonitorListener downloadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
            NotificationManager notificationManager = Mockito.mock(NotificationManager.class);

            SyncJobEventDAO syncJobEventDAO1 = Mockito.mock(SyncJobEventDAO.class);
            SyncJob syncJob1 = new SyncJob(SyncJob.Type.Bidirectional, "url", "uuid", "token", "/remotedir1", "/localdir1", true);
            SyncOperation syncOperation1 = Mockito.spy(new SyncOperation(syncJob1, afsClient, uploadMonitor, downloadMonitor, syncJobEventDAO1, Path.of("/hidden-dir"), notificationManager));

            SyncOperation.FileSyncCollisionListener fileSyncCollisionListener = syncOperation1.new FileSyncCollisionListener(syncDirection);

            if (syncDirection == SyncJobEvent.SyncDirection.UP) {

                String localDirPath = Path.of(this.getClass().getClassLoader().getResource("placeholder.txt").getPath()).getParent().toString();
                Path localFile = Path.of(localDirPath, UUID.randomUUID().toString());
                Files.createFile(localFile);
                fileSyncCollisionListener.deleteSourceFile(localFile);
                Assert.assertFalse(Files.exists(localFile));

            } else if (syncDirection == SyncJobEvent.SyncDirection.DOWN) {

                Path remotePath = Path.of("remote");
                Mockito.doNothing().when(syncOperation1).deleteRemoteFile(remotePath);
                fileSyncCollisionListener.deleteSourceFile(remotePath);
                Mockito.verify(syncOperation1, Mockito.times(1)).deleteRemoteFile(remotePath);
            }
        }
    }

    @Test
    public void testFileSyncCollisionListenerIsSourceDirEmpty() throws Exception {
        for (SyncJobEvent.SyncDirection syncDirection : SyncJobEvent.SyncDirection.values()) {
            SyncOperation.AfsClientProxy afsClient = Mockito.mock(SyncOperation.AfsClientProxy.class);
            ClientAPI.TransferMonitorListener uploadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
            ClientAPI.TransferMonitorListener downloadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
            NotificationManager notificationManager = Mockito.mock(NotificationManager.class);

            SyncJobEventDAO syncJobEventDAO1 = Mockito.mock(SyncJobEventDAO.class);
            SyncJob syncJob1 = new SyncJob(SyncJob.Type.Bidirectional, "url", "uuid", "token", "/remotedir1", "/localdir1", true);
            SyncOperation syncOperation1 = Mockito.spy(new SyncOperation(syncJob1, afsClient, uploadMonitor, downloadMonitor, syncJobEventDAO1, Path.of("/hidden-dir"), notificationManager));

            SyncOperation.FileSyncCollisionListener fileSyncCollisionListener = syncOperation1.new FileSyncCollisionListener(syncDirection);

            if (syncDirection == SyncJobEvent.SyncDirection.UP) {

                String localDirPath = Path.of(this.getClass().getClassLoader().getResource("placeholder.txt").getPath()).getParent().toString();
                Path localDir = Path.of(localDirPath, UUID.randomUUID().toString());
                Files.createDirectory(localDir);
                Files.createFile(localDir.resolve("newfile"));
                Assert.assertFalse(fileSyncCollisionListener.isSourceDirEmpty(localDir));
                Files.deleteIfExists(localDir.resolve("newfile"));
                Assert.assertTrue(fileSyncCollisionListener.isSourceDirEmpty(localDir));

            } else if (syncDirection == SyncJobEvent.SyncDirection.DOWN) {

                Path remotePath = Path.of("remote");
                Mockito.doReturn(new File[1]).when(syncOperation1).getRemoteFileList(remotePath);
                Assert.assertFalse(fileSyncCollisionListener.isSourceDirEmpty(remotePath));
                Mockito.doReturn(new File[0]).when(syncOperation1).getRemoteFileList(remotePath);
                Assert.assertTrue(fileSyncCollisionListener.isSourceDirEmpty(remotePath));
            }
        }
    }

    @Test
    public void testFileSyncCollisionListenerSkippingLocalAppDir() throws Exception {
        for (SyncJob.Type syncJobType : SyncJob.Type.values()) {
            for (SyncJobEvent.SyncDirection syncDirection : SyncJobEvent.SyncDirection.values()) {
                SyncOperation.AfsClientProxy afsClient = Mockito.mock(SyncOperation.AfsClientProxy.class);
                ClientAPI.TransferMonitorListener uploadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
                ClientAPI.TransferMonitorListener downloadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
                NotificationManager notificationManager = Mockito.mock(NotificationManager.class);

                SyncJobEventDAO syncJobEventDAO1 = Mockito.mock(SyncJobEventDAO.class);
                SyncJob syncJob1 = new SyncJob(syncJobType, "url", "uuid", "token", "/remotedir1", "/localdir1", true);
                SyncOperation syncOperation1 = Mockito.spy(new SyncOperation(syncJob1, afsClient, uploadMonitor, downloadMonitor, syncJobEventDAO1, Path.of("/hidden-dir"), notificationManager));

                SyncOperation.FileSyncCollisionListener fileSyncCollisionListener = Mockito.spy(syncOperation1.new FileSyncCollisionListener(syncDirection));

                Path sourcePath = Path.of("source");
                Path destinationPath = Path.of("destination");

                Mockito.doReturn(true).when(syncOperation1).skipAppPrivateFilesPrecheck(syncDirection, sourcePath, destinationPath);

                ClientAPI.CollisionAction collisionAction = fileSyncCollisionListener.precheck(sourcePath, destinationPath, false);

                Mockito.verify(fileSyncCollisionListener, Mockito.times(0)).handleSyncResult(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
                Mockito.verify(syncOperation1, Mockito.times(0)).handleFileVersionConflict(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

                Assert.assertEquals(ClientAPI.CollisionAction.Skip, collisionAction);
                Mockito.verify(syncOperation1, Mockito.times(0)).insertNewSyncEntry(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
                Mockito.verify(syncOperation1, Mockito.times(0)).insertNewCompletedSyncEntry(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyBoolean());
            }
        }
    }

    @Test
    public void testFileSyncCollisionListenerMissingOrIncompleteSourceInfo() throws Exception {
        for (SyncJob.Type syncJobType : SyncJob.Type.values()) {
            for (SyncJobEvent.SyncDirection syncDirection : SyncJobEvent.SyncDirection.values()) {
                for(Optional<?> sourceInfo : List.of(Optional.empty(), Optional.of(new SyncOperation.FileInfo(false, null)))) {
                    SyncOperation.AfsClientProxy afsClient = Mockito.mock(SyncOperation.AfsClientProxy.class);
                    ClientAPI.TransferMonitorListener uploadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
                    ClientAPI.TransferMonitorListener downloadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
                    NotificationManager notificationManager = Mockito.mock(NotificationManager.class);

                    SyncJobEventDAO syncJobEventDAO1 = Mockito.mock(SyncJobEventDAO.class);
                    SyncJob syncJob1 = new SyncJob(syncJobType, "url", "uuid", "token", "/remotedir1", "/localdir1", true);
                    SyncOperation syncOperation1 = Mockito.spy(new SyncOperation(syncJob1, afsClient, uploadMonitor, downloadMonitor, syncJobEventDAO1, Path.of("/hidden-dir"), notificationManager));

                    SyncOperation.FileSyncCollisionListener fileSyncCollisionListener = Mockito.spy(syncOperation1.new FileSyncCollisionListener(syncDirection));

                    Path sourcePath = Path.of("source");
                    Path destinationPath = Path.of("destination");

                    Instant now = Instant.now();
                    Instant later = now.plusMillis(100);
                    Mockito.doReturn(sourceInfo).when(syncOperation1).getSourceInfo(syncDirection, sourcePath);
                    Mockito.doReturn(Optional.of(new SyncOperation.FileInfo(false, later))).when(syncOperation1).getDestinationInfo(syncDirection, destinationPath);

                    ClientAPI.CollisionAction collisionAction = fileSyncCollisionListener.precheck(sourcePath, destinationPath, false);

                    Mockito.verify(fileSyncCollisionListener, Mockito.times(0)).handleSyncResult(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
                    Mockito.verify(syncOperation1, Mockito.times(0)).handleFileVersionConflict(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());


                    Assert.assertEquals(ClientAPI.CollisionAction.Skip, collisionAction);
                    Mockito.verify(syncOperation1, Mockito.times(0)).insertNewSyncEntry(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
                    Mockito.verify(syncOperation1, Mockito.times(0)).insertNewCompletedSyncEntry(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyBoolean());
                }
            }
        }
    }

    @Test
    public void testFileSyncCollisionListenerHandlingSyncResult() throws Exception {
        for (SyncJob.Type syncJobType : SyncJob.Type.values()) {
            for (SyncJobEvent.SyncDirection syncDirection : SyncJobEvent.SyncDirection.values()) {
                for(ClientAPI.CollisionAction expectedCollisionAction : ClientAPI.CollisionAction.values()) {
                    for(boolean collision : List.of(Boolean.FALSE, Boolean.TRUE)) {
                        for(boolean isSourcePathDir : List.of(Boolean.FALSE, Boolean.TRUE)) {
                            SyncOperation.AfsClientProxy afsClient = Mockito.mock(SyncOperation.AfsClientProxy.class);
                            ClientAPI.TransferMonitorListener uploadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
                            ClientAPI.TransferMonitorListener downloadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
                            NotificationManager notificationManager = Mockito.mock(NotificationManager.class);

                            SyncJobEventDAO syncJobEventDAO1 = Mockito.mock(SyncJobEventDAO.class);
                            SyncJob syncJob1 = new SyncJob(syncJobType, "url", "uuid", "token", "/remotedir1", "/localdir1", true);


                            Path sourcePath = Path.of("source");
                            Path destinationPath = Path.of("destination");

                            for(SyncJobEvent fakeSyncEntry : getDifferentFakeSyncJobEntries(syncDirection, sourcePath, destinationPath, syncJob1)) {
                                SyncOperation syncOperation1 = Mockito.spy(new SyncOperation(syncJob1, afsClient, uploadMonitor, downloadMonitor, syncJobEventDAO1, Path.of("/hidden-dir"), notificationManager));
                                SyncOperation.FileSyncCollisionListener fileSyncCollisionListener = Mockito.spy(syncOperation1.new FileSyncCollisionListener(syncDirection));

                                Mockito.doReturn(false).when(syncOperation1).skipAppPrivateFilesPrecheck(syncDirection, sourcePath, destinationPath);

                                Instant now = Instant.now();
                                Instant later = now.plusMillis(100);
                                Optional<SyncOperation.FileInfo> sourceInfo = Optional.of(new SyncOperation.FileInfo(isSourcePathDir, now));
                                Mockito.doReturn(sourceInfo).when(syncOperation1).getSourceInfo(syncDirection, sourcePath);
                                Mockito.doReturn(Optional.of(new SyncOperation.FileInfo(false, later))).when(syncOperation1).getDestinationInfo(syncDirection, destinationPath);

                                Mockito.doReturn(fakeSyncEntry).when(syncOperation1).getSyncJobEvent(syncDirection, sourcePath, destinationPath);

                                ArgumentCaptor<Instant> instantArgumentCaptor = ArgumentCaptor.forClass(Instant.class);
                                SyncOperation.SyncCheckResult expectedSyncCheckResult = SyncOperation.checkSyncState(fakeSyncEntry, now, collision ? later : null, syncDirection, collision);
                                Mockito.doReturn(expectedCollisionAction).when(fileSyncCollisionListener)
                                        .handleSyncResult(sourcePath, destinationPath, expectedSyncCheckResult, sourceInfo.get(), fakeSyncEntry);

                                ClientAPI.CollisionAction collisionAction = fileSyncCollisionListener.precheck(sourcePath, destinationPath, collision);

                                Mockito.verify(syncOperation1, Mockito.times(collision ? 1 : 0)).getDestinationInfo(Mockito.any(), Mockito.any());
                                Mockito.verify(fileSyncCollisionListener, Mockito.times(1)).handleSyncResult(sourcePath, destinationPath, expectedSyncCheckResult, sourceInfo.get(), fakeSyncEntry);
                                Mockito.verify(syncOperation1, Mockito.times(0)).handleFileVersionConflict(sourcePath, destinationPath, syncDirection, fakeSyncEntry);
                                Assert.assertEquals(expectedCollisionAction, collisionAction);

                                if(expectedCollisionAction != ClientAPI.CollisionAction.Override) {
                                    Mockito.verify(syncOperation1, Mockito.times(0)).insertNewSyncEntry(syncDirection, sourcePath, destinationPath, now);
                                    Mockito.verify(syncOperation1, Mockito.times(0)).insertNewSyncEntry(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
                                }

                                if(expectedCollisionAction == ClientAPI.CollisionAction.Override && !isSourcePathDir) {
                                    Mockito.verify(syncOperation1, Mockito.times(1)).insertNewSyncEntry(syncDirection, sourcePath, destinationPath, now);
                                    Mockito.verify(syncOperation1, Mockito.times(0)).insertNewCompletedSyncEntry(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyBoolean());
                                }

                                if(expectedCollisionAction == ClientAPI.CollisionAction.Override && isSourcePathDir) {
                                    if (!collision || fakeSyncEntry == null || fakeSyncEntry.getDestinationTimestamp() == null) {
                                        Mockito.verify(syncOperation1, Mockito.times(1)).insertNewCompletedSyncEntry(Mockito.eq(syncDirection), Mockito.eq(sourcePath), Mockito.eq(destinationPath), Mockito.eq(now), Mockito.anyBoolean());
                                        Mockito.verify(syncOperation1, Mockito.times(0)).insertNewSyncEntry(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
                                    } else {
                                        Mockito.verify(syncOperation1, Mockito.times(0)).insertNewSyncEntry(syncDirection, sourcePath, destinationPath, now);
                                        Mockito.verify(syncOperation1, Mockito.times(0)).insertNewSyncEntry(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    public void testFileSyncCollisionListenerHandleSyncResult() throws Exception {
        for (SyncJob.Type syncJobType : SyncJob.Type.values()) {
            for (SyncJobEvent.SyncDirection syncDirection : SyncJobEvent.SyncDirection.values()) {
                for (SyncOperation.SyncCheckResult syncCheckResult : SyncOperation.SyncCheckResult.values()) {
                    for(boolean isSourcePathDir : List.of(Boolean.FALSE, Boolean.TRUE)) {
                        for(boolean isSourceDirEmpty : List.of(Boolean.FALSE, Boolean.TRUE)) {
                            SyncOperation.AfsClientProxy afsClient = Mockito.mock(SyncOperation.AfsClientProxy.class);
                            ClientAPI.TransferMonitorListener uploadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
                            ClientAPI.TransferMonitorListener downloadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
                            NotificationManager notificationManager = Mockito.mock(NotificationManager.class);

                            SyncJobEventDAO syncJobEventDAO1 = Mockito.mock(SyncJobEventDAO.class);
                            SyncJob syncJob1 = new SyncJob(syncJobType, "url", "uuid", "token", "/remotedir1", "/localdir1", true);
                            SyncOperation syncOperation1 = Mockito.spy(new SyncOperation(syncJob1, afsClient, uploadMonitor, downloadMonitor, syncJobEventDAO1, Path.of("/hidden-dir"), notificationManager));

                            SyncOperation.FileSyncCollisionListener fileSyncCollisionListener = Mockito.spy(syncOperation1.new FileSyncCollisionListener(syncDirection));

                            Path sourcePath = Path.of("source");
                            Path destinationPath = Path.of("destination");


                            Instant now = Instant.now();
                            SyncOperation.FileInfo sourceInfo = new SyncOperation.FileInfo(isSourcePathDir, now);

                            SyncJobEvent fakeSyncEntry = SyncJobEvent.builder().syncDirection(syncDirection).localFile(destinationPath.toAbsolutePath().toString()).remoteFile(sourcePath.toAbsolutePath().toString())
                                    .entityPermId(syncJob1.getEntityPermId()).localDirectoryRoot(syncJob1.getLocalDirectoryRoot()).sourceTimestamp(System.currentTimeMillis()).timestamp(System.currentTimeMillis()).build();

                            Mockito.doReturn(isSourceDirEmpty).when(fileSyncCollisionListener).isSourceDirEmpty(sourcePath);
                            Mockito.doNothing().when(fileSyncCollisionListener).deleteSourceFile(sourcePath);
                            Mockito.doReturn(ClientAPI.CollisionAction.Skip).when(syncOperation1).handleFileVersionConflict(sourcePath, destinationPath, syncDirection, fakeSyncEntry);
                            ClientAPI.CollisionAction collisionAction = fileSyncCollisionListener.handleSyncResult(sourcePath, destinationPath, syncCheckResult, sourceInfo, fakeSyncEntry);

                            SyncOperation.SyncCheckAction syncCheckAction = SyncOperation.getSyncActionForSyncResult(syncCheckResult, syncJobType);

                            if(syncCheckAction == SyncOperation.SyncCheckAction.SKIP) {
                                Mockito.verify(fileSyncCollisionListener, Mockito.times(0)).deleteSourceFile(Mockito.any());
                                Mockito.verify(syncOperation1, Mockito.times(0)).handleFileVersionConflict(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
                                Mockito.verify(syncOperation1, Mockito.times(0)).insertNewSyncEntryForSourceDeleted(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyBoolean());
                                Assert.assertEquals(ClientAPI.CollisionAction.Skip, collisionAction);
                            }
                            if(syncCheckAction == SyncOperation.SyncCheckAction.PROCEED) {
                                Mockito.verify(fileSyncCollisionListener, Mockito.times(0)).deleteSourceFile(Mockito.any());
                                Mockito.verify(syncOperation1, Mockito.times(0)).handleFileVersionConflict(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
                                Mockito.verify(syncOperation1, Mockito.times(0)).insertNewSyncEntryForSourceDeleted(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyBoolean());
                                Assert.assertEquals(ClientAPI.CollisionAction.Override, collisionAction);
                            }
                            if(syncCheckAction == SyncOperation.SyncCheckAction.RAISE_VERSION_CONFLICT) {
                                if(!isSourcePathDir) {
                                    Mockito.verify(fileSyncCollisionListener, Mockito.times(0)).deleteSourceFile(Mockito.any());
                                    Mockito.verify(syncOperation1, Mockito.times(1)).handleFileVersionConflict(sourcePath, destinationPath, syncDirection, fakeSyncEntry);
                                    Assert.assertEquals(ClientAPI.CollisionAction.Skip, collisionAction);
                                } else {
                                    Mockito.verify(fileSyncCollisionListener, Mockito.times(0)).deleteSourceFile(Mockito.any());
                                    Mockito.verify(syncOperation1, Mockito.times(0)).handleFileVersionConflict(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
                                    Assert.assertEquals(ClientAPI.CollisionAction.Override, collisionAction);
                                }
                                Mockito.verify(syncOperation1, Mockito.times(0)).insertNewSyncEntryForSourceDeleted(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyBoolean());
                            }
                            if(syncCheckAction == SyncOperation.SyncCheckAction.DELETE_SOURCE) {
                                if(!isSourcePathDir || isSourceDirEmpty) {
                                    Mockito.verify(fileSyncCollisionListener, Mockito.times(1)).deleteSourceFile(sourcePath);
                                    Mockito.verify(syncOperation1, Mockito.times(1)).insertNewSyncEntryForSourceDeleted(syncDirection, sourcePath, destinationPath, isSourcePathDir);
                                } else {
                                    Mockito.verify(fileSyncCollisionListener, Mockito.times(0)).deleteSourceFile(Mockito.any());
                                    Mockito.verify(syncOperation1, Mockito.times(0)).insertNewSyncEntryForSourceDeleted(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyBoolean());
                                }
                                Mockito.verify(syncOperation1, Mockito.times(0)).handleFileVersionConflict(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
                                Assert.assertEquals(ClientAPI.CollisionAction.Skip, collisionAction);
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    public void testHandleFileVersionConflict_resolvedConflict() throws Exception {
        SyncOperation.AfsClientProxy afsClient = Mockito.mock(SyncOperation.AfsClientProxy.class);
        ClientAPI.TransferMonitorListener uploadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        ClientAPI.TransferMonitorListener downloadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        NotificationManager notificationManager = Mockito.mock(NotificationManager.class);

        SyncJobEventDAO syncJobEventDAO1 = Mockito.mock(SyncJobEventDAO.class);
        SyncJob syncJob1 = new SyncJob(SyncJob.Type.Bidirectional, "url", "uuid", "token", "/remotedir1", "/localdir1", true);
        SyncOperation syncOperation1 = Mockito.spy(new SyncOperation(syncJob1, afsClient, uploadMonitor, downloadMonitor, syncJobEventDAO1, Path.of("/hidden-dir"), notificationManager));

        for( SyncJobEvent.SyncDirection syncDirection : List.of(Event.SyncDirection.UP, Event.SyncDirection.DOWN)) {
            Path source = Path.of(this.getClass().getClassLoader().getResource("placeholder.txt").getPath()).getParent().resolve("source");
            Path destination = Path.of(this.getClass().getClassLoader().getResource("placeholder.txt").getPath()).getParent().resolve("destination");

            Path localFile = syncDirection == SyncJobEvent.SyncDirection.UP ? source : destination;
            Path remoteFile = syncDirection == SyncJobEvent.SyncDirection.UP ? destination : source;

            Path conflictLocalFile = Path.of(localFile.toAbsolutePath() + SyncOperation.CONFLICT_FILE_SUFFIX);
            Files.write(localFile, new byte[0], StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            FileTime localFileLastModified = Files.getLastModifiedTime(localFile);

            for(boolean remoteFileInfoIsPresent : List.of(false, true)) {
                for(boolean uploadResult : List.of(false, true)) {
                    for(boolean transferMonitorException : List.of(false, true)) {
                        Mockito.clearInvocations(syncOperation1);
                        Mockito.clearInvocations(afsClient);
                        Mockito.clearInvocations(notificationManager);
                        Mockito.clearInvocations(syncJobEventDAO1);
                        Files.deleteIfExists(conflictLocalFile);
                        Notification alreadyPresentNotification = new Notification(
                                Notification.Type.Conflict,
                                syncJob1.getLocalDirectoryRoot(),
                                localFile.toString(),
                                remoteFile.toString(),
                                "FILE VERSION CONFLICT",
                                Instant.now().toEpochMilli()
                        );
                        Mockito.doReturn(alreadyPresentNotification).when(notificationManager).getSpecificNotification(Mockito.any());
                        Instant remoteLastModifiedDate = Instant.now().minusSeconds(1000000);
                        Mockito.doReturn( remoteFileInfoIsPresent ? Optional.of(new SyncOperation.FileInfo(false,
                                remoteLastModifiedDate)) : Optional.empty()
                        ).when(syncOperation1).getRemoteFileInfo(remoteFile);
                        Mockito.doAnswer((invocationOnMock)->{
                            ClientAPI.TransferMonitorListener transferMonitorListener = invocationOnMock.getArgument(4, ClientAPI.TransferMonitorListener.class);
                            //call start and add on transferMonitorListener to cause a 'transferred' event
                            transferMonitorListener.start(localFile, remoteFile, 10);
                            transferMonitorListener.add(localFile, remoteFile, 10, true);
                            if(transferMonitorException) {
                                transferMonitorListener.failed(new RuntimeException("EXCEPTIONAL RESULT"));
                            }
                            return uploadResult;
                        }).when(afsClient).upload(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());

                        SyncJobEvent fakeSyncEntry = SyncJobEvent.builder().syncDirection(syncDirection).localFile(localFile.toString()).remoteFile(remoteFile.toString())
                                .entityPermId(syncJob1.getEntityPermId()).localDirectoryRoot(syncJob1.getLocalDirectoryRoot()).sourceTimestamp(System.currentTimeMillis()).timestamp(System.currentTimeMillis()).build();

                        Exception exception = null;
                        try {
                            syncOperation1.handleFileVersionConflict(source, destination, syncDirection, fakeSyncEntry);
                        } catch (Exception e) {
                            exception = e;
                        }
                        Mockito.verify(afsClient, Mockito.times(1)).delete(syncJob1.getEntityPermId(), remoteFile.toString());

                        Mockito.verify(afsClient, Mockito.times(1)).upload(
                                Mockito.eq(localFile),
                                Mockito.eq(syncJob1.getEntityPermId()),
                                Mockito.eq(remoteFile), Mockito.any(), Mockito.any());

                        if(remoteFileInfoIsPresent) {
                            SyncJobEvent expectedSyncJobEvent = SyncJobEvent.builder()
                                    .syncDirection(Event.SyncDirection.UP)
                                    .localDirectoryRoot(syncJob1.getLocalDirectoryRoot())
                                    .entityPermId(syncJob1.getEntityPermId())
                                    .localFile(localFile.toAbsolutePath().toString())
                                    .remoteFile(remoteFile.toAbsolutePath().toString())
                                    .sourceTimestamp(localFileLastModified.toMillis())
                                    .destinationTimestamp(remoteLastModifiedDate.toEpochMilli())
                                    .timestamp(Instant.now().toEpochMilli())
                                    .directory(false)
                                    .sourceDeleted(false).build();
                            ArgumentCaptor<SyncJobEvent> syncJobEventArgumentCaptor = ArgumentCaptor.forClass(SyncJobEvent.class);
                            Mockito.verify(syncJobEventDAO1, Mockito.times(1)).insertOrUpdate(syncJobEventArgumentCaptor.capture());
                            Assert.assertEquals(
                                    expectedSyncJobEvent.toBuilder().timestamp(syncJobEventArgumentCaptor.getValue().getTimestamp()).build(),
                                    syncJobEventArgumentCaptor.getValue());
                        }

                        if(uploadResult && !transferMonitorException && remoteFileInfoIsPresent) {
                            Mockito.verify(syncOperation1, Mockito.times(1)).removePossibleConflictNotification(source, destination, syncDirection);
                            Assert.assertNull(exception);
                        } else {
                            Mockito.verify(syncOperation1, Mockito.times(0)).removePossibleConflictNotification(Mockito.any(), Mockito.any(), Mockito.any());
                            Assert.assertNotNull(exception);
                        }
                    }
                }
            }
        }
    }

    @Test
    public void testHandleFileVersionConflict_raiseConflictNotification() throws Exception {
        SyncOperation.AfsClientProxy afsClient = Mockito.mock(SyncOperation.AfsClientProxy.class);
        ClientAPI.TransferMonitorListener uploadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        ClientAPI.TransferMonitorListener downloadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        NotificationManager notificationManager = Mockito.mock(NotificationManager.class);

        SyncJobEventDAO syncJobEventDAO1 = Mockito.mock(SyncJobEventDAO.class);
        SyncJob syncJob1 = new SyncJob(SyncJob.Type.Bidirectional, "url", "uuid", "token", "/remotedir1", "/localdir1", true);
        SyncOperation syncOperation1 = Mockito.spy(new SyncOperation(syncJob1, afsClient, uploadMonitor, downloadMonitor, syncJobEventDAO1, Path.of("/hidden-dir"), notificationManager));

        for( SyncJobEvent.SyncDirection syncDirection : List.of(Event.SyncDirection.UP, Event.SyncDirection.DOWN)) {
            Path source = Path.of(this.getClass().getClassLoader().getResource("placeholder.txt").getPath()).getParent().resolve("source");
            Path destination = Path.of(this.getClass().getClassLoader().getResource("placeholder.txt").getPath()).getParent().resolve("destination");

            Path localFile = syncDirection == SyncJobEvent.SyncDirection.UP ? source : destination;
            Path remoteFile = syncDirection == SyncJobEvent.SyncDirection.UP ? destination : source;

            for (boolean conflictFileAlreadyPresent : List.of(false, true)) {
                Path conflictLocalFile = Path.of(localFile.toAbsolutePath() + SyncOperation.CONFLICT_FILE_SUFFIX);
                for(boolean remoteFileInfoIsPresent : List.of(false, true)) {

                    for(boolean remoteLaterModifiedThanLocalConflictFile : List.of(false, true)) {

                        boolean doDownload = !conflictFileAlreadyPresent || (remoteFileInfoIsPresent && remoteLaterModifiedThanLocalConflictFile);

                        for(boolean downloadResult : List.of(false, true)) {
                            for(boolean transferMonitorException : List.of(false, true)) {
                                if(conflictFileAlreadyPresent) {
                                    Files.write(conflictLocalFile, new byte[0], StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
                                } else {
                                    Files.deleteIfExists(conflictLocalFile);
                                }
                                Mockito.clearInvocations(syncOperation1);
                                Mockito.clearInvocations(afsClient);
                                if (remoteFileInfoIsPresent) {
                                    Mockito.doReturn(Optional.of(new SyncOperation.FileInfo(false,
                                            remoteLaterModifiedThanLocalConflictFile ? Instant.now().plusSeconds(1000) : Instant.now().minusSeconds(1000000)
                                    ))).when(syncOperation1).getRemoteFileInfo(remoteFile);
                                } else {
                                    Mockito.doReturn(Optional.empty()).when(syncOperation1).getRemoteFileInfo(remoteFile);
                                }
                                Mockito.doAnswer((invocationOnMock)->{
                                    ClientAPI.TransferMonitorListener transferMonitorListener = invocationOnMock.getArgument(4, ClientAPI.TransferMonitorListener.class);
                                    if(transferMonitorException) {
                                        transferMonitorListener.failed(new RuntimeException("EXCEPTIONAL RESULT"));
                                    }
                                    return downloadResult;
                                }).when(afsClient).download(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

                                SyncJobEvent fakeSyncEntry = SyncJobEvent.builder().syncDirection(syncDirection).localFile(localFile.toString()).remoteFile(remoteFile.toString())
                                        .entityPermId(syncJob1.getEntityPermId()).localDirectoryRoot(syncJob1.getLocalDirectoryRoot()).sourceTimestamp(System.currentTimeMillis()).timestamp(System.currentTimeMillis()).build();

                                Exception exception = null;
                                try {
                                    syncOperation1.handleFileVersionConflict(source, destination, syncDirection, fakeSyncEntry);
                                } catch (Exception e) {
                                    exception = e;
                                }
                                if(doDownload) {
                                    if(downloadResult && !transferMonitorException) {
                                        Assert.assertNull(exception);
                                        Mockito.verify(afsClient, Mockito.times(1)).download(Mockito.eq(syncJob1.getEntityPermId()), Mockito.eq(remoteFile), Mockito.eq(conflictLocalFile), Mockito.any(), Mockito.any());
                                        Mockito.verify(afsClient, Mockito.times(0)).upload(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
                                        Mockito.verify(syncOperation1, Mockito.times(1)).raiseConflictNotification(source, destination, syncDirection, fakeSyncEntry);
                                    } else {
                                        Assert.assertNotNull(exception);
                                        Mockito.verify(afsClient, Mockito.times(1)).download(Mockito.eq(syncJob1.getEntityPermId()), Mockito.eq(remoteFile), Mockito.eq(conflictLocalFile), Mockito.any(), Mockito.any());
                                        Mockito.verify(afsClient, Mockito.times(0)).upload(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
                                        Mockito.verify(syncOperation1, Mockito.times(0)).raiseConflictNotification(source, destination, syncDirection, fakeSyncEntry);
                                    }
                                } else {
                                    Assert.assertNull(exception);
                                    Mockito.verify(afsClient, Mockito.times(0)).download(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
                                    Mockito.verify(afsClient, Mockito.times(0)).upload(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
                                    Mockito.verify(syncOperation1, Mockito.times(1)).raiseConflictNotification(source, destination, syncDirection, fakeSyncEntry);
                                }

                            }
                        }
                    }

                }
            }
        }
    }

    @Test
    public void testRaiseConflictNotification() {
        SyncOperation.AfsClientProxy afsClient = Mockito.mock(SyncOperation.AfsClientProxy.class);
        ClientAPI.TransferMonitorListener uploadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        ClientAPI.TransferMonitorListener downloadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        NotificationManager notificationManager = Mockito.mock(NotificationManager.class);

        SyncJobEventDAO syncJobEventDAO1 = Mockito.mock(SyncJobEventDAO.class);
        SyncJob syncJob1 = new SyncJob(SyncJob.Type.Bidirectional, "url", "uuid", "token", "/remotedir1", "/localdir1", true);
        SyncOperation syncOperation1 = Mockito.spy(new SyncOperation(syncJob1, afsClient, uploadMonitor, downloadMonitor, syncJobEventDAO1, Path.of("/hidden-dir"), notificationManager));

        Path source = Path.of("source");
        Path destination = Path.of("destination");
        SyncJobEvent fakeSyncEntry = SyncJobEvent.builder().syncDirection(SyncJobEvent.SyncDirection.UP).localFile(source.toAbsolutePath().toString()).remoteFile(destination.toAbsolutePath().toString())
                .entityPermId(syncJob1.getEntityPermId()).localDirectoryRoot(syncJob1.getLocalDirectoryRoot()).sourceTimestamp(System.currentTimeMillis()).timestamp(System.currentTimeMillis()).build();
        Instant now = Instant.now();

        syncOperation1.raiseConflictNotification(source, destination, SyncJobEvent.SyncDirection.UP, fakeSyncEntry);

        ArgumentCaptor<List<Notification>> notificationArgumentCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(syncOperation1.notificationManager, Mockito.times(1)).addNotifications(notificationArgumentCaptor.capture());
        Assert.assertEquals(1, notificationArgumentCaptor.getValue().size());
        Assert.assertEquals(Notification.Type.Conflict, notificationArgumentCaptor.getValue().get(0).getType());
        Assert.assertEquals(syncJob1.getLocalDirectoryRoot(), notificationArgumentCaptor.getValue().get(0).getLocalDirectory());
        Assert.assertEquals(source.toString(), notificationArgumentCaptor.getValue().get(0).getLocalFile());
        Assert.assertEquals(toServerPathString(destination), notificationArgumentCaptor.getValue().get(0).getRemoteFile());
        Assert.assertTrue(notificationArgumentCaptor.getValue().get(0).getMessage().contains("FILE VERSION CONFLICT"));
        Assert.assertTrue(now.toEpochMilli() <= notificationArgumentCaptor.getValue().get(0).getTimestamp());
        Assert.assertTrue(Instant.now().toEpochMilli() >= notificationArgumentCaptor.getValue().get(0).getTimestamp());
    }


    @Test
    public void testRemoveConflictNotification() {
        SyncOperation.AfsClientProxy afsClient = Mockito.mock(SyncOperation.AfsClientProxy.class);
        ClientAPI.TransferMonitorListener uploadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        ClientAPI.TransferMonitorListener downloadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        NotificationManager notificationManager = Mockito.mock(NotificationManager.class);

        SyncJobEventDAO syncJobEventDAO1 = Mockito.mock(SyncJobEventDAO.class);
        SyncJob syncJob1 = new SyncJob(SyncJob.Type.Bidirectional, "url", "uuid", "token", "/remotedir1", "/localdir1", true);
        SyncOperation syncOperation1 = Mockito.spy(new SyncOperation(syncJob1, afsClient, uploadMonitor, downloadMonitor, syncJobEventDAO1, Path.of("/hidden-dir"), notificationManager));

        Path source = Path.of("source");
        Path destination = Path.of("destination");
        SyncJobEvent fakeSyncEntry = SyncJobEvent.builder().syncDirection(SyncJobEvent.SyncDirection.UP).localFile(source.toAbsolutePath().toString()).remoteFile(toServerPathString(destination))
                .entityPermId(syncJob1.getEntityPermId()).localDirectoryRoot(syncJob1.getLocalDirectoryRoot()).sourceTimestamp(System.currentTimeMillis()).timestamp(System.currentTimeMillis()).build();
        Instant now = Instant.now();

        syncOperation1.removePossibleConflictNotification(source, destination, SyncJobEvent.SyncDirection.UP);

        ArgumentCaptor<List<Notification>> notificationArgumentCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(syncOperation1.notificationManager, Mockito.times(1)).removeNotifications(notificationArgumentCaptor.capture());
        Assert.assertEquals(1, notificationArgumentCaptor.getValue().size());
        Assert.assertEquals(Notification.Type.Conflict, notificationArgumentCaptor.getValue().get(0).getType());
        Assert.assertEquals(syncJob1.getLocalDirectoryRoot(), notificationArgumentCaptor.getValue().get(0).getLocalDirectory());
        Assert.assertEquals(source.toString(), notificationArgumentCaptor.getValue().get(0).getLocalFile());
        Assert.assertEquals(toServerPathString(destination), notificationArgumentCaptor.getValue().get(0).getRemoteFile());
        Assert.assertEquals("FILE VERSION CONFLICT", notificationArgumentCaptor.getValue().get(0).getMessage());
        Assert.assertTrue(now.toEpochMilli() <= notificationArgumentCaptor.getValue().get(0).getTimestamp());
        Assert.assertTrue(Instant.now().toEpochMilli() >= notificationArgumentCaptor.getValue().get(0).getTimestamp());
    }

    @Test
    public void testRaiseJobStoppedNotification() {
        SyncOperation.AfsClientProxy afsClient = Mockito.mock(SyncOperation.AfsClientProxy.class);
        ClientAPI.TransferMonitorListener uploadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        ClientAPI.TransferMonitorListener downloadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        NotificationManager notificationManager = Mockito.mock(NotificationManager.class);

        SyncJobEventDAO syncJobEventDAO1 = Mockito.mock(SyncJobEventDAO.class);
        SyncJob syncJob1 = new SyncJob(SyncJob.Type.Bidirectional, "url", "uuid", "token", "/remotedir1", "/localdir1", true);
        SyncOperation syncOperation1 = Mockito.spy(new SyncOperation(syncJob1, afsClient, uploadMonitor, downloadMonitor, syncJobEventDAO1, Path.of("/hidden-dir"), notificationManager));

        Path source = Path.of("source");
        Path destination = Path.of("destination");
        SyncJobEvent fakeSyncEntry = SyncJobEvent.builder().syncDirection(SyncJobEvent.SyncDirection.UP).localFile(source.toAbsolutePath().toString()).remoteFile(destination.toAbsolutePath().toString())
                .entityPermId(syncJob1.getEntityPermId()).localDirectoryRoot(syncJob1.getLocalDirectoryRoot()).sourceTimestamp(System.currentTimeMillis()).timestamp(System.currentTimeMillis()).build();
        Instant now = Instant.now();

        syncOperation1.raiseJobStoppedNotification();

        ArgumentCaptor<List<Notification>> notificationArgumentCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(syncOperation1.notificationManager, Mockito.times(1)).addNotifications(notificationArgumentCaptor.capture());
        Assert.assertEquals(1, notificationArgumentCaptor.getValue().size());
        Assert.assertEquals(Notification.Type.JobStopped, notificationArgumentCaptor.getValue().get(0).getType());
        Assert.assertEquals(syncJob1.getLocalDirectoryRoot(), notificationArgumentCaptor.getValue().get(0).getLocalDirectory());
        Assert.assertEquals(null, notificationArgumentCaptor.getValue().get(0).getLocalFile());
        Assert.assertEquals(null, notificationArgumentCaptor.getValue().get(0).getRemoteFile());
        Assert.assertEquals("JOB STOPPED", notificationArgumentCaptor.getValue().get(0).getMessage());
        Assert.assertTrue(now.toEpochMilli() <= notificationArgumentCaptor.getValue().get(0).getTimestamp());
        Assert.assertTrue(Instant.now().toEpochMilli() >= notificationArgumentCaptor.getValue().get(0).getTimestamp());
    }

    @Test
    public void testRaiseJobExceptionNotification() {
        SyncOperation.AfsClientProxy afsClient = Mockito.mock(SyncOperation.AfsClientProxy.class);
        ClientAPI.TransferMonitorListener uploadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        ClientAPI.TransferMonitorListener downloadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        NotificationManager notificationManager = Mockito.mock(NotificationManager.class);

        SyncJobEventDAO syncJobEventDAO1 = Mockito.mock(SyncJobEventDAO.class);
        SyncJob syncJob1 = new SyncJob(SyncJob.Type.Bidirectional, "url", "uuid", "token", "/remotedir1", "/localdir1", true);
        SyncOperation syncOperation1 = Mockito.spy(new SyncOperation(syncJob1, afsClient, uploadMonitor, downloadMonitor, syncJobEventDAO1, Path.of("/hidden-dir"), notificationManager));

        Path source = Path.of("source");
        Path destination = Path.of("destination");
        SyncJobEvent fakeSyncEntry = SyncJobEvent.builder().syncDirection(SyncJobEvent.SyncDirection.UP).localFile(source.toAbsolutePath().toString()).remoteFile(destination.toAbsolutePath().toString())
                .entityPermId(syncJob1.getEntityPermId()).localDirectoryRoot(syncJob1.getLocalDirectoryRoot()).sourceTimestamp(System.currentTimeMillis()).timestamp(System.currentTimeMillis()).build();
        Instant now = Instant.now();

        Exception exception = new RuntimeException("!!!UNEXPECTED EXCEPTION!!!");
        syncOperation1.raiseJobExceptionNotification(exception);

        ArgumentCaptor<List<Notification>> notificationArgumentCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(syncOperation1.notificationManager, Mockito.times(1)).addNotifications(notificationArgumentCaptor.capture());
        Assert.assertEquals(1, notificationArgumentCaptor.getValue().size());
        Assert.assertEquals(Notification.Type.JobException, notificationArgumentCaptor.getValue().get(0).getType());
        Assert.assertEquals(syncJob1.getLocalDirectoryRoot(), notificationArgumentCaptor.getValue().get(0).getLocalDirectory());
        Assert.assertEquals(null, notificationArgumentCaptor.getValue().get(0).getLocalFile());
        Assert.assertEquals(null, notificationArgumentCaptor.getValue().get(0).getRemoteFile());
        Assert.assertEquals("java.lang.RuntimeException exception with message: !!!UNEXPECTED EXCEPTION!!!", notificationArgumentCaptor.getValue().get(0).getMessage());
        Assert.assertTrue(now.toEpochMilli() <= notificationArgumentCaptor.getValue().get(0).getTimestamp());
        Assert.assertTrue(Instant.now().toEpochMilli() >= notificationArgumentCaptor.getValue().get(0).getTimestamp());
    }

    @Test
    @SneakyThrows
    public void testClearStaleConflictNotifications() {
        Path localDirPath = Path.of(this.getClass().getClassLoader().getResource("placeholder.txt").getPath()).getParent().toAbsolutePath();
        Files.deleteIfExists(localDirPath.resolve("conflict1"));
        Files.deleteIfExists(localDirPath.resolve("conflict2"));
        Files.createFile(localDirPath.resolve("conflict1"));
        Files.createFile(localDirPath.resolve("conflict2"));

        SyncOperation.AfsClientProxy afsClient = Mockito.mock(SyncOperation.AfsClientProxy.class);
        ClientAPI.TransferMonitorListener uploadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        ClientAPI.TransferMonitorListener downloadMonitor = Mockito.mock(ClientAPI.TransferMonitorListener.class);
        NotificationManager notificationManager = Mockito.mock(NotificationManager.class);

        SyncJobEventDAO syncJobEventDAO1 = Mockito.mock(SyncJobEventDAO.class);
        SyncJob syncJob1 = new SyncJob(SyncJob.Type.Bidirectional, "url", "uuid", "token", "/remotedir1", "/localdir1", true);
        SyncOperation syncOperation1 = Mockito.spy(new SyncOperation(syncJob1, afsClient, uploadMonitor, downloadMonitor, syncJobEventDAO1, Path.of("/hidden-dir"), notificationManager));

        List<Notification> conflictNotifications = List.of(
                Notification.builder().type(Notification.Type.Conflict).localDirectory(localDirPath.toString())
                        .localFile(localDirPath.resolve("conflict1").toString()).remoteFile("/remote1")
                        .timestamp(System.currentTimeMillis()).message("CONFLICT").build(),
                Notification.builder().type(Notification.Type.Conflict).localDirectory(localDirPath.toString())
                        .localFile(localDirPath.resolve("conflict2").toString()).remoteFile("/remote2")
                        .timestamp(System.currentTimeMillis()).message("CONFLICT").build(),
                Notification.builder().type(Notification.Type.Conflict).localDirectory(localDirPath.toString())
                        .localFile(localDirPath.resolve("conflict3").toString()).remoteFile("/remote3")
                        .timestamp(System.currentTimeMillis()).message("CONFLICT").build()
        );
        Mockito.doReturn(conflictNotifications).when(notificationManager).getConflictNotifications(syncJob1, 100);

        syncOperation1.clearStaleConflictNotifications();

        ArgumentCaptor<List<Notification>> removedNotificationArgCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(notificationManager, Mockito.times(1)).removeNotifications(removedNotificationArgCaptor.capture());
        assertEquals(1, removedNotificationArgCaptor.getValue().size());
        assertEquals(conflictNotifications.get(2), removedNotificationArgCaptor.getValue().get(0));
    }

    private static SyncJobEvent[] getDifferentFakeSyncJobEntries(SyncJobEvent.SyncDirection syncDirection, Path sourcePath, Path destinationPath, SyncJob syncJob) {
        return new SyncJobEvent[]{

                SyncJobEvent.builder().syncDirection(syncDirection).localFile(destinationPath.toAbsolutePath().toString()).remoteFile(sourcePath.toAbsolutePath().toString())
                        .entityPermId(syncJob.getEntityPermId()).localDirectoryRoot(syncJob.getLocalDirectoryRoot()).sourceTimestamp(Instant.now().toEpochMilli()).timestamp(System.currentTimeMillis()).build(),

                SyncJobEvent.builder().syncDirection(syncDirection).localFile(destinationPath.toAbsolutePath().toString()).remoteFile(sourcePath.toAbsolutePath().toString())
                        .entityPermId(syncJob.getEntityPermId()).localDirectoryRoot(syncJob.getLocalDirectoryRoot()).sourceTimestamp(Instant.now().toEpochMilli()).timestamp(System.currentTimeMillis()).destinationTimestamp(Instant.now().toEpochMilli()).build(),

                null
        };
    }
}