package ch.openbis.drive.protobuf.converters;

import ch.openbis.drive.model.Event;
import ch.openbis.drive.model.Notification;
import ch.openbis.drive.model.Settings;
import ch.openbis.drive.model.SyncJob;
import ch.openbis.drive.protobuf.DriveApiService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class ProtobufConversionUtilTest {

    @Test
    public void fromProtobufSettings() {
        DriveApiService.Settings protobufSettings = DriveApiService.Settings.newBuilder().setLanguage("fr").setStartAtLogin(true).setSyncIntervalSeconds(5436)
                .setJobs(DriveApiService.SyncJobs.newBuilder().addSyncJobs(
                        DriveApiService.SyncJob.newBuilder()
                                .setEnabled(true)
                                .setLocalDirectoryRoot("/loc-dir")
                                .setOpenBisUrl("http://UrL")
                                .setEntityPermId("uiop-4321")
                                .setOpenBisPersonalAccessToken("tkn-TKN")
                                .setRemoteDirectoryRoot("/remDIR")
                                .setType(DriveApiService.SyncJob.Type.BIDIRECTIONAL)
                                .build()
                ).addSyncJobs(
                        DriveApiService.SyncJob.newBuilder()
                                .setEnabled(false)
                                .setLocalDirectoryRoot("/loc-dir2")
                                .setOpenBisUrl("http://UrL3")
                                .setEntityPermId("uiop-43215")
                                .setOpenBisPersonalAccessToken("tkn-TKN2")
                                .setRemoteDirectoryRoot("/remDIR3")
                                .setType(DriveApiService.SyncJob.Type.DOWNLOAD)
                                .build()
                ).build()).build();

        Settings settings = ProtobufConversionUtil.fromProtobufSettings(protobufSettings);

        Assert.assertEquals("fr", settings.getLanguage());
        Assert.assertEquals(true, settings.isStartAtLogin());
        Assert.assertEquals(5436, settings.getSyncInterval());
        Assert.assertEquals(ProtobufConversionUtil.fromProtobufSyncJobs(DriveApiService.SyncJobs.newBuilder().addSyncJobs(
                DriveApiService.SyncJob.newBuilder()
                        .setEnabled(true)
                        .setLocalDirectoryRoot("/loc-dir")
                        .setOpenBisUrl("http://UrL")
                        .setEntityPermId("uiop-4321")
                        .setOpenBisPersonalAccessToken("tkn-TKN")
                        .setRemoteDirectoryRoot("/remDIR")
                        .setType(DriveApiService.SyncJob.Type.BIDIRECTIONAL)
                        .build()
        ).addSyncJobs(
                DriveApiService.SyncJob.newBuilder()
                        .setEnabled(false)
                        .setLocalDirectoryRoot("/loc-dir2")
                        .setOpenBisUrl("http://UrL3")
                        .setEntityPermId("uiop-43215")
                        .setOpenBisPersonalAccessToken("tkn-TKN2")
                        .setRemoteDirectoryRoot("/remDIR3")
                        .setType(DriveApiService.SyncJob.Type.DOWNLOAD)
                        .build()
        ).build()), settings.getJobs());
    }

    @Test
    public void toProtobufSettings() {
        Settings settings = new Settings(true, "it", 63, new ArrayList<>(List.of(
                new SyncJob(SyncJob.Type.Upload, "http://loc", "tkntkn", "1234-abcd", "/remDIR", "/LOCdir", true)
        )));

        DriveApiService.Settings protobufSettings = ProtobufConversionUtil.toProtobufSettings(settings);

        Assert.assertEquals("it", protobufSettings.getLanguage());
        Assert.assertEquals(true, protobufSettings.getStartAtLogin());
        Assert.assertEquals(63, protobufSettings.getSyncIntervalSeconds());
        Assert.assertEquals(ProtobufConversionUtil.toProtobufSyncJobs(List.of(
                        new SyncJob(SyncJob.Type.Upload, "http://loc", "tkntkn", "1234-abcd", "/remDIR", "/LOCdir", true)
                )
        ), protobufSettings.getJobs());
    }

    @Test
    public void fromProtobufSyncJobs() {

        DriveApiService.SyncJobs protobufSyncJobs = DriveApiService.SyncJobs.newBuilder().addSyncJobs(
                DriveApiService.SyncJob.newBuilder()
                        .setEnabled(true)
                        .setLocalDirectoryRoot("/loc-dir")
                        .setOpenBisUrl("http://UrL")
                        .setEntityPermId("uiop-4321")
                        .setOpenBisPersonalAccessToken("tkn-TKN")
                        .setRemoteDirectoryRoot("/remDIR")
                        .setType(DriveApiService.SyncJob.Type.BIDIRECTIONAL)
                        .build()
        ).addSyncJobs(
                DriveApiService.SyncJob.newBuilder()
                        .setEnabled(false)
                        .setLocalDirectoryRoot("/loc-dir2")
                        .setOpenBisUrl("http://UrL3")
                        .setEntityPermId("uiop-43215")
                        .setOpenBisPersonalAccessToken("tkn-TKN2")
                        .setRemoteDirectoryRoot("/remDIR3")
                        .setType(DriveApiService.SyncJob.Type.DOWNLOAD)
                        .build()
        ).build();

        List<SyncJob> syncJobs = ProtobufConversionUtil.fromProtobufSyncJobs(protobufSyncJobs);

        Assert.assertEquals(2, syncJobs.size());

        Assert.assertEquals(true, syncJobs.get(0).isEnabled());
        Assert.assertEquals("/loc-dir", syncJobs.get(0).getLocalDirectoryRoot());
        Assert.assertEquals("http://UrL", syncJobs.get(0).getOpenBisUrl());
        Assert.assertEquals("uiop-4321", syncJobs.get(0).getEntityPermId());
        Assert.assertEquals("tkn-TKN", syncJobs.get(0).getOpenBisPersonalAccessToken());
        Assert.assertEquals("/remDIR", syncJobs.get(0).getRemoteDirectoryRoot());
        Assert.assertEquals(SyncJob.Type.Bidirectional, syncJobs.get(0).getType());

        Assert.assertEquals(false, syncJobs.get(1).isEnabled());
        Assert.assertEquals("/loc-dir2", syncJobs.get(1).getLocalDirectoryRoot());
        Assert.assertEquals("http://UrL3", syncJobs.get(1).getOpenBisUrl());
        Assert.assertEquals("uiop-43215", syncJobs.get(1).getEntityPermId());
        Assert.assertEquals("tkn-TKN2", syncJobs.get(1).getOpenBisPersonalAccessToken());
        Assert.assertEquals("/remDIR3", syncJobs.get(1).getRemoteDirectoryRoot());
        Assert.assertEquals(SyncJob.Type.Download, syncJobs.get(1).getType());

    }

    @Test
    public void toProtobufSyncJobs() {
        List<SyncJob> syncJobs = List.of(
                new SyncJob(SyncJob.Type.Upload, "http://loc", "tkntkn", "1234-abcd", "/remDIR", "/LOCdir", true),
                new SyncJob(SyncJob.Type.Bidirectional, "http://loc2", "tkntkn2", "1234-abcd2", "/remDIR3", "/LOCdir3", false)
        );

        DriveApiService.SyncJobs protobufSyncJobs = ProtobufConversionUtil.toProtobufSyncJobs(syncJobs);

        Assert.assertEquals(2, protobufSyncJobs.getSyncJobsCount());

        Assert.assertEquals(true, protobufSyncJobs.getSyncJobs(0).getEnabled());
        Assert.assertEquals("/LOCdir", protobufSyncJobs.getSyncJobs(0).getLocalDirectoryRoot());
        Assert.assertEquals("http://loc", protobufSyncJobs.getSyncJobs(0).getOpenBisUrl());
        Assert.assertEquals("1234-abcd", protobufSyncJobs.getSyncJobs(0).getEntityPermId());
        Assert.assertEquals("tkntkn", protobufSyncJobs.getSyncJobs(0).getOpenBisPersonalAccessToken());
        Assert.assertEquals("/remDIR", protobufSyncJobs.getSyncJobs(0).getRemoteDirectoryRoot());
        Assert.assertEquals(DriveApiService.SyncJob.Type.UPLOAD, protobufSyncJobs.getSyncJobs(0).getType());

        Assert.assertEquals(false, protobufSyncJobs.getSyncJobs(1).getEnabled());
        Assert.assertEquals("/LOCdir3", protobufSyncJobs.getSyncJobs(1).getLocalDirectoryRoot());
        Assert.assertEquals("http://loc2", protobufSyncJobs.getSyncJobs(1).getOpenBisUrl());
        Assert.assertEquals("1234-abcd2", protobufSyncJobs.getSyncJobs(1).getEntityPermId());
        Assert.assertEquals("tkntkn2", protobufSyncJobs.getSyncJobs(1).getOpenBisPersonalAccessToken());
        Assert.assertEquals("/remDIR3", protobufSyncJobs.getSyncJobs(1).getRemoteDirectoryRoot());
        Assert.assertEquals(DriveApiService.SyncJob.Type.BIDIRECTIONAL, protobufSyncJobs.getSyncJobs(1).getType());
    }

    @Test
    public void fromProtobufNotifications() {
        DriveApiService.Notifications protobufNotifications = DriveApiService.Notifications.newBuilder()
                .addNotifications(
                        DriveApiService.Notification.newBuilder().setType(DriveApiService.Notification.Type.CONFLICT).setLocalDirectory("/dir").setLocalFile("/loc4").setRemoteFile("/rem").setMessage("mEsSaGe1").setTimestamp(324234L).build()
                )
                .addNotifications(
                        DriveApiService.Notification.newBuilder().setType(DriveApiService.Notification.Type.JOB_EXCEPTION).setLocalDirectory("/dir2").setLocalFile("/loc5").setRemoteFile("/rem").setMessage("mEsSaGe2").setTimestamp(67543L).build()
                )
                .build();

        List<Notification> notifications = ProtobufConversionUtil.fromProtobufNotifications(protobufNotifications);

        Assert.assertEquals(2, notifications.size());

        Assert.assertEquals(Notification.Type.Conflict, notifications.get(0).getType());
        Assert.assertEquals("/dir", notifications.get(0).getLocalDirectory());
        Assert.assertEquals("/loc4", notifications.get(0).getLocalFile());
        Assert.assertEquals("/rem", notifications.get(0).getRemoteFile());
        Assert.assertEquals("mEsSaGe1", notifications.get(0).getMessage());
        Assert.assertEquals((Long) 324234L, notifications.get(0).getTimestamp());

        Assert.assertEquals(Notification.Type.JobException, notifications.get(1).getType());
        Assert.assertEquals("/dir2", notifications.get(1).getLocalDirectory());
        Assert.assertEquals("/loc5", notifications.get(1).getLocalFile());
        Assert.assertEquals("/rem", notifications.get(1).getRemoteFile());
        Assert.assertEquals("mEsSaGe2", notifications.get(1).getMessage());
        Assert.assertEquals((Long) 67543L, notifications.get(1).getTimestamp());

    }

    @Test
    public void toProtobufNotifications() {

        List<Notification> notifications = List.of(Notification.builder().type(Notification.Type.Conflict).localDirectory("/dir").localFile("/loc4").remoteFile("/rem").message("mEsSaGe1").timestamp(324234L).build(),
                Notification.builder().type(Notification.Type.JobException).localDirectory("/dir2").localFile("/loc5").remoteFile("/rem").message("mEsSaGe2").timestamp(67543L).build(),
                Notification.builder().type(Notification.Type.JobStopped).localDirectory("/dir6").localFile("/loc7").remoteFile("/rem").message("mEsSaGe3").timestamp(8543L).build());

        DriveApiService.Notifications protobufNotifications = ProtobufConversionUtil.toProtobufNotifications(notifications);

        Assert.assertEquals(3, protobufNotifications.getNotificationsCount());

        Assert.assertEquals(DriveApiService.Notification.Type.CONFLICT, protobufNotifications.getNotifications(0).getType());
        Assert.assertEquals("/dir", protobufNotifications.getNotifications(0).getLocalDirectory());
        Assert.assertEquals("/loc4", protobufNotifications.getNotifications(0).getLocalFile());
        Assert.assertEquals("/rem", protobufNotifications.getNotifications(0).getRemoteFile());
        Assert.assertEquals("mEsSaGe1", protobufNotifications.getNotifications(0).getMessage());
        Assert.assertEquals(324234L, protobufNotifications.getNotifications(0).getTimestamp());

        Assert.assertEquals(DriveApiService.Notification.Type.JOB_EXCEPTION, protobufNotifications.getNotifications(1).getType());
        Assert.assertEquals("/dir2", protobufNotifications.getNotifications(1).getLocalDirectory());
        Assert.assertEquals("/loc5", protobufNotifications.getNotifications(1).getLocalFile());
        Assert.assertEquals("/rem", protobufNotifications.getNotifications(1).getRemoteFile());
        Assert.assertEquals("mEsSaGe2", protobufNotifications.getNotifications(1).getMessage());
        Assert.assertEquals(67543L, protobufNotifications.getNotifications(1).getTimestamp());

        Assert.assertEquals(DriveApiService.Notification.Type.JOB_STOPPED, protobufNotifications.getNotifications(2).getType());
        Assert.assertEquals("/dir6", protobufNotifications.getNotifications(2).getLocalDirectory());
        Assert.assertEquals("/loc7", protobufNotifications.getNotifications(2).getLocalFile());
        Assert.assertEquals("/rem", protobufNotifications.getNotifications(2).getRemoteFile());
        Assert.assertEquals("mEsSaGe3", protobufNotifications.getNotifications(2).getMessage());
        Assert.assertEquals(8543L, protobufNotifications.getNotifications(2).getTimestamp());
    }

    @Test
    public void fromProtobufEvents() {
        DriveApiService.Events protobufEvents = DriveApiService.Events.newBuilder()
                .addEvents(DriveApiService.Event.newBuilder().setSyncDirection(DriveApiService.Event.SyncDirection.UP)
                        .setLocalFile("/loc").setDirectory(true).setSourceDeleted(false).setRemoteFile("/rem").setTimestamp(432523L).build())
                .addEvents(DriveApiService.Event.newBuilder().setSyncDirection(DriveApiService.Event.SyncDirection.DOWN)
                        .setLocalFile("/loc1").setDirectory(true).setSourceDeleted(true).setRemoteFile("/rem1").setTimestamp(75934L).build())
                .addEvents(DriveApiService.Event.newBuilder().setSyncDirection(DriveApiService.Event.SyncDirection.DOWN)
                        .setLocalFile("/loc2").setDirectory(false).setSourceDeleted(true).setRemoteFile("/rem3").setTimestamp(75938L).build())
                .build();

        List<? extends Event> events = ProtobufConversionUtil.fromProtobufEvents(protobufEvents);

        Assert.assertEquals(3, events.size());

        Assert.assertEquals(Event.SyncDirection.UP, events.get(0).getSyncDirection());
        Assert.assertEquals("/loc", events.get(0).getLocalFile());
        Assert.assertEquals(true, events.get(0).isDirectory());
        Assert.assertEquals(false, events.get(0).isSourceDeleted());
        Assert.assertEquals("/rem", events.get(0).getRemoteFile());
        Assert.assertEquals((Long) 432523L, events.get(0).getTimestamp());

        Assert.assertEquals(Event.SyncDirection.DOWN, events.get(1).getSyncDirection());
        Assert.assertEquals("/loc1", events.get(1).getLocalFile());
        Assert.assertEquals(true, events.get(1).isDirectory());
        Assert.assertEquals(true, events.get(1).isSourceDeleted());
        Assert.assertEquals("/rem1", events.get(1).getRemoteFile());
        Assert.assertEquals((Long) 75934L, events.get(1).getTimestamp());

        Assert.assertEquals(Event.SyncDirection.DOWN, events.get(2).getSyncDirection());
        Assert.assertEquals("/loc2", events.get(2).getLocalFile());
        Assert.assertEquals(false, events.get(2).isDirectory());
        Assert.assertEquals(true, events.get(2).isSourceDeleted());
        Assert.assertEquals("/rem3", events.get(2).getRemoteFile());
        Assert.assertEquals((Long) 75938L, events.get(2).getTimestamp());
    }

    @Test
    public void toProtobufEvents() {
        List<? extends Event> events = List.of(
                new EventClientDto(DriveApiService.Event.newBuilder().setSyncDirection(DriveApiService.Event.SyncDirection.UP)
                        .setLocalFile("/loc").setDirectory(true).setSourceDeleted(false).setRemoteFile("/rem").setTimestamp(432523L).build()),
                new EventClientDto(DriveApiService.Event.newBuilder().setSyncDirection(DriveApiService.Event.SyncDirection.DOWN)
                        .setLocalFile("/loc1").setDirectory(true).setSourceDeleted(true).setRemoteFile("/rem1").setTimestamp(75934L).build())
        );

        DriveApiService.Events protobufEvents = ProtobufConversionUtil.toProtobufEvents(events);

        Assert.assertEquals(2, protobufEvents.getEventsCount());

        Assert.assertEquals(DriveApiService.Event.SyncDirection.UP, protobufEvents.getEvents(0).getSyncDirection());
        Assert.assertEquals("/loc", protobufEvents.getEvents(0).getLocalFile());
        Assert.assertEquals(true, protobufEvents.getEvents(0).getDirectory());
        Assert.assertEquals(false, protobufEvents.getEvents(0).getSourceDeleted());
        Assert.assertEquals("/rem", protobufEvents.getEvents(0).getRemoteFile());
        Assert.assertEquals(432523L, protobufEvents.getEvents(0).getTimestamp());

        Assert.assertEquals(DriveApiService.Event.SyncDirection.DOWN, protobufEvents.getEvents(1).getSyncDirection());
        Assert.assertEquals("/loc1", protobufEvents.getEvents(1).getLocalFile());
        Assert.assertEquals(true, protobufEvents.getEvents(1).getDirectory());
        Assert.assertEquals(true, protobufEvents.getEvents(1).getSourceDeleted());
        Assert.assertEquals("/rem1", protobufEvents.getEvents(1).getRemoteFile());
        Assert.assertEquals(75934L, protobufEvents.getEvents(1).getTimestamp());
    }

    @Test
    public void fromProtobufSyncDirectionEnum() {
        Assert.assertEquals(Event.SyncDirection.UP, ProtobufConversionUtil.fromProtobufSyncDirectionEnum(DriveApiService.Event.SyncDirection.UP));
        Assert.assertEquals(Event.SyncDirection.DOWN, ProtobufConversionUtil.fromProtobufSyncDirectionEnum(DriveApiService.Event.SyncDirection.DOWN));
    }

    @Test
    public void toProtobufSyncDirectionEnum() {
        Assert.assertEquals(DriveApiService.Event.SyncDirection.UP, ProtobufConversionUtil.toProtobufSyncDirectionEnum(Event.SyncDirection.UP));
        Assert.assertEquals(DriveApiService.Event.SyncDirection.DOWN, ProtobufConversionUtil.toProtobufSyncDirectionEnum(Event.SyncDirection.DOWN));

    }

    @Test
    public void fromProtobufNotificationTypeEnum() {
        Assert.assertEquals(Notification.Type.Conflict, ProtobufConversionUtil.fromProtobufNotificationTypeEnum(DriveApiService.Notification.Type.CONFLICT));
        Assert.assertEquals(Notification.Type.JobStopped, ProtobufConversionUtil.fromProtobufNotificationTypeEnum(DriveApiService.Notification.Type.JOB_STOPPED));
        Assert.assertEquals(Notification.Type.JobException, ProtobufConversionUtil.fromProtobufNotificationTypeEnum(DriveApiService.Notification.Type.JOB_EXCEPTION));
    }

    @Test
    public void toProtobufNotificationTypeEnum() {
        Assert.assertEquals(DriveApiService.Notification.Type.CONFLICT, ProtobufConversionUtil.toProtobufNotificationTypeEnum(Notification.Type.Conflict));
        Assert.assertEquals(DriveApiService.Notification.Type.JOB_STOPPED, ProtobufConversionUtil.toProtobufNotificationTypeEnum(Notification.Type.JobStopped));
        Assert.assertEquals(DriveApiService.Notification.Type.JOB_EXCEPTION, ProtobufConversionUtil.toProtobufNotificationTypeEnum(Notification.Type.JobException));
    }

    @Test
    public void fromProtobuftoSyncJobTypeEnum() {
        Assert.assertEquals(SyncJob.Type.Bidirectional, ProtobufConversionUtil.fromProtobuftoSyncJobTypeEnum(DriveApiService.SyncJob.Type.BIDIRECTIONAL));
        Assert.assertEquals(SyncJob.Type.Upload, ProtobufConversionUtil.fromProtobuftoSyncJobTypeEnum(DriveApiService.SyncJob.Type.UPLOAD));
        Assert.assertEquals(SyncJob.Type.Download, ProtobufConversionUtil.fromProtobuftoSyncJobTypeEnum(DriveApiService.SyncJob.Type.DOWNLOAD));
    }

    @Test
    public void toProtobufSyncJobTypeEnum() {
        Assert.assertEquals(DriveApiService.SyncJob.Type.BIDIRECTIONAL, ProtobufConversionUtil.toProtobufSyncJobTypeEnum(SyncJob.Type.Bidirectional));
        Assert.assertEquals(DriveApiService.SyncJob.Type.UPLOAD, ProtobufConversionUtil.toProtobufSyncJobTypeEnum(SyncJob.Type.Upload));
        Assert.assertEquals(DriveApiService.SyncJob.Type.DOWNLOAD, ProtobufConversionUtil.toProtobufSyncJobTypeEnum(SyncJob.Type.Download));
    }
}